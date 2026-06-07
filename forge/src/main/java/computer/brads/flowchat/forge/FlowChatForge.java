package computer.brads.flowchat.forge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = FlowChatForge.MOD_ID, name = "FlowChat", version = "2.1.2", acceptedMinecraftVersions = "[1.7.10]")
public class FlowChatForge {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            new ClientProxy(LOGGER).init();
        } else {
            LOGGER.info("FlowChat: client-side mod, skipping server init");
        }
    }
}
