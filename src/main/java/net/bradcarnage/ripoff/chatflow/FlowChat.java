package net.bradcarnage.ripoff.chatflow;

import com.google.gson.JsonArray;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class FlowChat implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static JsonArray filter_rules;

    public static boolean disabled;
    @Override
    public void onInitializeClient() {
        LOGGER.info("FlowChat " + FabricLoader.getInstance().getModContainer("flowchat").get().getMetadata().getVersion() + " Initialized");
        SettingsManager.initSettingsClient();
    }
}
