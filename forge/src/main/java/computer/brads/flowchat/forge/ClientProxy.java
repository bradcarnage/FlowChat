package computer.brads.flowchat.forge;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.OnJoinServerEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SideOnly(Side.CLIENT)
public class ClientProxy {
    private final Logger logger;
    private FlowChatConfig config;
    private final MessageProcessor processor = new MessageProcessor();
    private long whenLastWorldTick;
    private String serverIp = "unknown";

    public ClientProxy(Logger logger) {
        this.logger = logger;
    }

    public void init() {
        logger.info("FlowChat initialized (Forge 1.7.10 client)");
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
            logger.error("FlowChat error processing chat", e);
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

        logger.info("Processing {} onJoinServer entries for {}", entries.size(), joinedIp);

        for (final OnJoinServerEntry entry : entries) {
            if (!entry.matchesServer(joinedIp)) continue;

            final List<String> commands = entry.getCommands();
            int delaySec = entry.getDelay();

            if (delaySec <= 0) {
                scheduleCommands(commands, 0);
            } else {
                scheduleCommands(commands, delaySec * 1000L);
            }
        }
    }

    private void scheduleCommands(final List<String> commands, long delayMs) {
        if (delayMs <= 0) {
            for (String cmd : commands) {
                sendCommand(cmd);
            }
        } else {
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
                logger.info("onJoinServer: sending '{}'", command);
                mc.thePlayer.sendChatMessage(command);
            } else {
                logger.warn("onJoinServer: player null, can't send '{}'", command);
            }
        } catch (Exception e) {
            logger.error("onJoinServer: error sending command", e);
        }
    }
}
