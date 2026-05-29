package computer.brads.flowchat.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisguisedChat;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Universal chat packet interceptor using PacketEvents.
 * Works identically on Spigot, BungeeCord, and Velocity.
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

    private void handleSystemChat(PacketSendEvent event) {
        WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
        if (wrapper.isOverlay()) return;

        Component message = wrapper.getMessage();
        if (message == null) return;

        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        if (plainText.isEmpty()) return;

        MessageProcessor.Result result = processor.process(plainText, config.getIncomingRules(), serverIdentifier);
        if (!result.wasModified()) return;

        if (result.toastMe) {
            wrapper.setMessage(Component.text(result.processedText));
            wrapper.setOverlay(true);
            return;
        }

        if (result.cancelled) {
            event.setCancelled(true);
            return;
        }

        if (!plainText.equals(result.processedText)) {
            wrapper.setMessage(Component.text(result.processedText));
            LOGGER.debug("Modified outbound: {} -> {}", plainText, result.processedText);
        }
    }

    private void handleDisguisedChat(PacketSendEvent event) {
        WrapperPlayServerDisguisedChat wrapper = new WrapperPlayServerDisguisedChat(event);
        Component message = wrapper.getMessage();
        if (message == null) return;

        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        if (plainText.isEmpty()) return;

        MessageProcessor.Result result = processor.process(plainText, config.getIncomingRules(), serverIdentifier);
        if (!result.wasModified()) return;

        if (result.cancelled) { event.setCancelled(true); return; }

        if (!plainText.equals(result.processedText)) {
            wrapper.setMessage(Component.text(result.processedText));
        }
    }
}
