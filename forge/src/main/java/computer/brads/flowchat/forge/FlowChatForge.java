package computer.brads.flowchat.forge;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Mod(modid = FlowChatForge.MOD_ID, name = "FlowChat", version = "2.1.0", acceptedMinecraftVersions = "[1.8.9]")
public class FlowChatForge {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("FlowChat initialized (Forge 1.8.9 client)");
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
            if (result.playSound) ForgeTextHelper.playSound(result.soundName);
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
            if (mc.getCurrentServerData() != null) {
                serverIp = mc.getCurrentServerData().serverIP;
            }
        }
    }
}
