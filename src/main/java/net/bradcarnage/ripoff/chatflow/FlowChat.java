package net.bradcarnage.ripoff.chatflow;

import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

@Environment(EnvType.CLIENT)
public class FlowChat implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static JsonObject filter_rules;
    public static String last_cmd_sent;
    public static Long when_last_cmd_sent;

    public static boolean disabled;
    @Override
    public void onInitializeClient() {
        LOGGER.info("FlowChat " + FabricLoader.getInstance().getModContainer("flowchat").get().getMetadata().getVersion() + " Initialized");
        SettingsManager.initSettingsClient();
        FlowChat.when_last_cmd_sent = Instant.now().toEpochMilli();
    }
}
