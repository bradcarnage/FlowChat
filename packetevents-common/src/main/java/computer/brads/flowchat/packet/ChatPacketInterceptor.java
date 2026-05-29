package computer.brads.flowchat.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.StaticSound;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisguisedChat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.SoundResolver;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Universal chat packet interceptor using PacketEvents.
 * Works identically on Spigot, BungeeCord, and Velocity.
 * Uses PE's built-in AdventureSerializer — no external adventure deps needed.
 */
public class ChatPacketInterceptor extends PacketListenerAbstract {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowchat");

    private final FlowChatConfig config;
    private final MessageProcessor processor;
    private final String serverIdentifier;

    public ChatPacketInterceptor(FlowChatConfig config, String serverIdentifier) {
        super(PacketListenerPriority.NORMAL);
        this.config = config;
        this.processor = new MessageProcessor();
        this.serverIdentifier = serverIdentifier;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (config.isDisabled()) return;
        try {
            if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
                handleSystemChat(event);
            } else if (event.getPacketType() == PacketType.Play.Server.DISGUISED_CHAT) {
                handleDisguisedChat(event);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing outbound packet", e);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (config.isDisabled()) return;
        try {
            if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
                handleOutgoingChat(event);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing inbound chat packet", e);
        }
    }

    private void handleSystemChat(PacketSendEvent event) {
        WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
        if (wrapper.isOverlay()) return;

        Component message = wrapper.getMessage();
        if (message == null) return;

        // Use PE's AdventureSerializer for text extraction and JSON serialization
        String plainText = AdventureSerializer.asVanilla(message);
        if (plainText == null || plainText.isEmpty()) return;

        // Feature #6: JSON for matchJson rules
        String rawJson = null;
        try { rawJson = AdventureSerializer.toJson(message); } catch (Exception ignored) {}

        MessageProcessor.Result result = processor.process(
                plainText, config.getIncomingRules(), serverIdentifier, null, null, rawJson);
        if (!result.wasModified()) return;

        String colored = MessageProcessor.formatColors(result.processedText);

        if (result.toast) {
            wrapper.setMessage(Component.text(colored));
            wrapper.setOverlay(true);
            if (result.playSound) sendSoundPacket(event, result.soundId);
            return;
        }

        if (result.cancelled) {
            event.setCancelled(true);
            return;
        }

        if (result.playSound) sendSoundPacket(event, result.soundId);

        if (!plainText.equals(result.processedText)) {
            wrapper.setMessage(Component.text(colored));
            LOGGER.debug("Modified outbound: {} -> {}", plainText, colored);
        }

        for (String response : result.autoResponses) {
            sendAutoResponse(event, response);
        }
    }

    private void handleDisguisedChat(PacketSendEvent event) {
        WrapperPlayServerDisguisedChat wrapper = new WrapperPlayServerDisguisedChat(event);
        Component message = wrapper.getMessage();
        if (message == null) return;

        String plainText = AdventureSerializer.asVanilla(message);
        if (plainText == null || plainText.isEmpty()) return;

        String rawJson = null;
        try { rawJson = AdventureSerializer.toJson(message); } catch (Exception ignored) {}

        MessageProcessor.Result result = processor.process(
                plainText, config.getIncomingRules(), serverIdentifier, null, null, rawJson);
        if (!result.wasModified()) return;

        String colored = MessageProcessor.formatColors(result.processedText);

        if (result.toast) {
            event.setCancelled(true);
            WrapperPlayServerSystemChatMessage overlay = new WrapperPlayServerSystemChatMessage(
                    true, Component.text(colored));
            event.getUser().sendPacket(overlay);
            if (result.playSound) sendSoundPacket(event, result.soundId);
            return;
        }

        if (result.cancelled) {
            event.setCancelled(true);
            return;
        }

        if (result.playSound) sendSoundPacket(event, result.soundId);

        if (!plainText.equals(result.processedText)) {
            wrapper.setMessage(Component.text(colored));
        }

        for (String response : result.autoResponses) {
            sendAutoResponse(event, response);
        }
    }

    private void handleOutgoingChat(PacketReceiveEvent event) {
        List<computer.brads.flowchat.core.FlowChatRule> outgoingRules = config.getOutgoingRules();
        if (outgoingRules.isEmpty()) return;

        WrapperPlayClientChatMessage wrapper = new WrapperPlayClientChatMessage(event);
        String message = wrapper.getMessage();
        if (message == null || message.isEmpty()) return;

        MessageProcessor.Result result = processor.process(message, outgoingRules, serverIdentifier);
        if (!result.wasModified()) return;

        if (result.cancelled || result.toast) {
            event.setCancelled(true);
            if (result.toast) {
                String colored = MessageProcessor.formatColors(result.processedText);
                WrapperPlayServerSystemChatMessage notify = new WrapperPlayServerSystemChatMessage(
                        true, Component.text(colored));
                event.getUser().sendPacket(notify);
            }
            return;
        }

        if (!message.equals(result.processedText)) {
            wrapper.setMessage(MessageProcessor.formatColors(result.processedText));
            LOGGER.debug("Modified outgoing: {} -> {}", message, result.processedText);
        }
    }

    private void sendSoundPacket(PacketSendEvent event, String soundId) {
        if (soundId == null) return;
        try {
            String[] parts = soundId.split(":", 2);
            String namespace = parts.length > 1 ? parts[0] : "minecraft";
            String path = parts.length > 1 ? parts[1] : parts[0];
            ResourceLocation loc = new ResourceLocation(namespace, path);
            StaticSound sound = new StaticSound(loc, null);
            com.github.retrooper.packetevents.util.Vector3i pos =
                    new com.github.retrooper.packetevents.util.Vector3i(0, 0, 0);
            WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(
                    sound, SoundCategory.MASTER, pos, 1.0f, 1.0f);
            event.getUser().sendPacket(packet);
        } catch (Exception e) {
            LOGGER.debug("Failed to send sound packet for {}: {}", soundId, e.getMessage());
        }
    }

    private void sendAutoResponse(PacketSendEvent event, String message) {
        try {
            WrapperPlayServerSystemChatMessage chatMsg = new WrapperPlayServerSystemChatMessage(
                    false, Component.text(message));
            event.getUser().sendPacket(chatMsg);
            LOGGER.debug("Sent auto-response: {}", message);
        } catch (Exception e) {
            LOGGER.debug("Failed to send auto-response: {}", e.getMessage());
        }
    }
}
