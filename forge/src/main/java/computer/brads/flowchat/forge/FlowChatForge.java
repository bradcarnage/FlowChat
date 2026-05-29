package computer.brads.flowchat.forge;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = "flowchat", name = "FlowChat", version = "2.1.0", clientSideOnly = true)
public class FlowChatForge {
    public static final Logger LOGGER = LogManager.getLogger("flowchat");
    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("FlowChat initialized (Forge 1.12.2 client)");
        File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        config = new FlowChatConfig(configDir.toPath());
        config.load();
        whenLastWorldTick = System.currentTimeMillis();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (config == null || config.isDisabled()) return;
        try {
            String text = event.getMessage().getUnformattedText();
            MessageProcessor.Result result = processor.process(text, config.getIncomingRules(), serverIp);
            if (result == null) return;
            if (result.cancelled) { event.setCanceled(true); return; }
            if (!result.processedText.equals(result.originalText))
                event.setMessage(new TextComponentString(ForgeTextHelper.formatColors(result.processedText)));
            if (result.playSound) ForgeTextHelper.playSound(result.soundId);
        } catch (Exception e) { LOGGER.error("Error processing chat", e); }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (config == null || config.isDisabled() || Minecraft.getMinecraft().world == null) return;
        long now = System.currentTimeMillis();
        if (whenLastWorldTick < now - 1000) { config.load(); }
        whenLastWorldTick = now;
    }
}
