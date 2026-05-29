package computer.brads.flowchat.neoforge;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Mod("flowchat")
public class FlowChatNeoForge {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static long whenLastCmdSent;
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";

    public FlowChatNeoForge() {
        if (FMLLoader.getDist() != Dist.CLIENT) return;

        LOGGER.info("FlowChat initialized (NeoForge client)");
        config = new FlowChatConfig(FMLPaths.CONFIGDIR.get());
        config.load();

        whenLastCmdSent = Instant.now().toEpochMilli();
        whenLastWorldTick = Instant.now().toEpochMilli();

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (config == null || config.isDisabled()) return;

        try {
            String text = event.getMessage().getString();
            MessageProcessor.Result result = processor.process(text, config.getIncomingRules(), serverIp);
            if (result == null) return;

            if (result.cancelled) {
                event.setCanceled(true);
                return;
            }

            if (!result.processedText.equals(result.originalText)) {
                event.setMessage(Component.literal(NeoForgeTextHelper.formatColors(result.processedText)));
            }

            if (result.playSound) {
                NeoForgeTextHelper.playSound(result.soundId);
            }

            if (result.toast) {
                NeoForgeTextHelper.showToast(result.processedText);
            }

            for (String response : result.autoResponses) {
                Minecraft.getInstance().player.connection.sendChat(response);
                whenLastCmdSent = Instant.now().toEpochMilli();
            }
        } catch (Exception e) {
            LOGGER.error("Error processing chat", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (config == null || config.isDisabled()) return;
        if (Minecraft.getInstance().level == null) return;

        long now = Instant.now().toEpochMilli();
        if (whenLastWorldTick < now - 1000) {
            serverIp = "singleplayer";
            try {
                var entry = Minecraft.getInstance().getCurrentServer();
                if (entry != null) serverIp = entry.ip;
            } catch (Exception ignored) {}
            config.load();
        }
        whenLastWorldTick = now;
    }
}
