package computer.brads.flowchat.forge;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.OnJoinServerEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Mod(modid = FlowChatForge.MOD_ID, name = "FlowChat", version = "2.1.2", acceptedMinecraftVersions = "[1.7.10]")
public class FlowChatForge {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("FlowChat initialized (Forge 1.7.10 client)");
        Path configDir = new File(Minecraft.getMinecraft().mcDataDir, "config").toPath();
        config = new FlowChatConfig(configDir);
        config.load();
        whenLastWorldTick = System.currentTimeMillis();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (config == null || config.isDisabled()) return;
        try {
            String text = event.message.getUnformattedText();
            MessageProcessor.Result result = processor.process(text, config.getIncomingRules(), serverIp);
            if (result == null) return;
            if (result.cancelled) { event.setCanceled(true); return; }
            if (!result.processedText.equals(result.originalText))
                event.message = new ChatComponentText(ForgeTextHelper.formatColors(result.processedText));
            if (result.playSound) ForgeTextHelper.playSound(result.soundId);
        } catch (Exception e) {
            LOGGER.error("FlowChat error processing chat", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            whenLastWorldTick = System.currentTimeMillis();
            if (mc.func_147104_D() != null) {
                serverIp = mc.func_147104_D().serverIP;
            }
        }
    }

    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (config == null || config.isDisabled()) return;
        config.load(); // reload config on join

        // Resolve server IP from the connection
        final String joinedIp;
        if (event.isLocal) {
            joinedIp = "singleplayer";
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            joinedIp = (mc.func_147104_D() != null) ? mc.func_147104_D().serverIP : "unknown";
        }
        serverIp = joinedIp;

        List<OnJoinServerEntry> entries = config.getOnJoinServer();
        if (entries.isEmpty()) return;

        LOGGER.info("Processing {} onJoinServer entries for {}", entries.size(), joinedIp);

        for (final OnJoinServerEntry entry : entries) {
            if (!entry.matchesServer(joinedIp)) continue;

            final List<String> commands = entry.getCommands();
            int delaySec = entry.getDelay();

            if (delaySec <= 0) {
                // Execute immediately on next client tick
                scheduleCommands(commands, 0);
            } else {
                scheduleCommands(commands, delaySec * 1000L);
            }
        }
    }

    /**
     * Schedule commands to be sent after a delay (in ms).
     * Commands are sent on the main Minecraft thread via addScheduledTask equivalent.
     */
    private void scheduleCommands(final List<String> commands, long delayMs) {
        if (delayMs <= 0) {
            // Send on next available tick
            for (String cmd : commands) {
                sendCommand(cmd);
            }
        } else {
            // Use a timer to delay, then send on the MC thread
            new Timer("FlowChat-OnJoin", true).schedule(new TimerTask() {
                @Override
                public void run() {
                    for (String cmd : commands) {
                        sendCommand(cmd);
                    }
                }
            }, delayMs);
        }
    }

    private void sendCommand(String command) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                LOGGER.info("onJoinServer: sending '{}'", command);
                mc.thePlayer.sendChatMessage(command);
            } else {
                LOGGER.warn("onJoinServer: player null, can't send '{}'", command);
            }
        } catch (Exception e) {
            LOGGER.error("onJoinServer: error sending command", e);
        }
    }
}
