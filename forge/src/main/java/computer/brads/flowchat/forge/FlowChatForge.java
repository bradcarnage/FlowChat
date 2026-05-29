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
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("flowchat")
public class FlowChatForge {
    public static final Logger LOGGER = LogManager.getLogger("flowchat");
    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";

    public FlowChatForge() {
        if (FMLLoader.getDist() != Dist.CLIENT) return;
        LOGGER.info("FlowChat initialized (Forge client)");
        config = new FlowChatConfig(FMLPaths.CONFIGDIR.get());
        config.load();
        whenLastWorldTick = System.currentTimeMillis();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (config == null || config.isDisabled()) return;
        try {
            String text = event.getMessage().getString();
            MessageProcessor.Result result = processor.process(text, config.getIncomingRules(), serverIp);
            if (result == null) return;
            if (result.cancelled) { event.setCanceled(true); return; }
            if (!result.processedText.equals(result.originalText))
                event.setMessage(new StringTextComponent(ForgeTextHelper.formatColors(result.processedText)));
            if (result.playSound) ForgeTextHelper.playSound(result.soundName);
        } catch (Exception e) { LOGGER.error("Error processing chat", e); }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (config == null || config.isDisabled() || Minecraft.getInstance().level == null) return;
        long now = System.currentTimeMillis();
        if (whenLastWorldTick < now - 1000) { config.load(); }
        whenLastWorldTick = now;
    }
}
