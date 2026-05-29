package computer.brads.flowchat.forge;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Mod("flowchat")
public class FlowChatForge {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static String lastCmdSent;
    public static long whenLastCmdSent;
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";
    public static boolean stillInVoid = false;

    public FlowChatForge() {
        if (FMLLoader.getDist() != Dist.CLIENT) return;

        LOGGER.info("FlowChat initialized (Forge client)");
        config = new FlowChatConfig(FMLPaths.CONFIGDIR.get());
        config.load();

        whenLastCmdSent = Instant.now().toEpochMilli();
        whenLastWorldTick = Instant.now().toEpochMilli();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (config == null || config.isDisabled()) return;

        try {
            String text = event.getMessage().getString();

            // Resolve username and serverName for 5-arg process()
            Minecraft mc = Minecraft.getInstance();
            String username = mc.player != null ? mc.player.getName().getString() : null;
            String serverName = "Singleplayer";
            var entry = mc.getCurrentServer();
            if (entry != null) serverName = entry.name;

            MessageProcessor.Result result = processor.process(text, config.getIncomingRules(), serverIp, username, serverName);
            if (result == null) return;

            if (result.cancelled) {
                event.setCanceled(true);
                return;
            }

            if (!result.processedText.equals(result.originalText)) {
                event.setMessage(Component.literal(MessageProcessor.formatColors(result.processedText)));
            }

            if (result.playSound) {
                ForgeTextHelper.playSound(result.soundId);
            }

            if (result.toast) {
                // Use notifyStyle to decide how to notify
                String style = result.notifyStyle != null ? result.notifyStyle : "actionbar";
                switch (style) {
                    case "toast" -> ForgeTextHelper.showToast(result.processedText);
                    case "actionbar" -> ForgeTextHelper.showActionBar(result.processedText);
                    default -> ForgeTextHelper.showActionBar(result.processedText);
                }
            }

            for (String response : result.autoResponses) {
                ForgeTextHelper.sendChat(response);
                whenLastCmdSent = Instant.now().toEpochMilli();
            }
        } catch (Exception e) {
            LOGGER.error("Error processing chat", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
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

        // Anti-AFK
        try {
            var afk = config.getAntiAfk();
            if (afk != null && (!afk.has("serversearch") || serverIp.matches(afk.get("serversearch").getAsString()))) {
                if (afk.has("afterSeconds") && afk.has("command")) {
                    if (whenLastCmdSent + (afk.get("afterSeconds").getAsLong() * 1000) < now) {
                        ForgeTextHelper.sendChat(afk.get("command").getAsString());
                        whenLastCmdSent = now;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Void fall
        try {
            var vf = config.getVoidFall();
            if (vf != null && (!vf.has("serversearch") || serverIp.matches(vf.get("serversearch").getAsString()))) {
                if (vf.has("command")) {
                    double yLevel = vf.has("yLevel") ? vf.get("yLevel").getAsDouble() : -20;
                    var player = Minecraft.getInstance().player;
                    if (player != null && yLevel >= player.getY()) {
                        if (!stillInVoid) {
                            stillInVoid = true;
                            ForgeTextHelper.sendChat(vf.get("command").getAsString());
                        }
                    } else {
                        stillInVoid = false;
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
