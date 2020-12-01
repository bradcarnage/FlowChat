package net.bradcarnage.ripoff.chatflow;

import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
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

        ClientTickEvents.START_WORLD_TICK.register(client -> {
            try {
                if (FlowChat.filter_rules.has("antiAFK")) {
                    JsonObject jobj = FlowChat.filter_rules.get("antiAFK").getAsJsonObject();
                    if (jobj.has("afterSeconds") && jobj.has("command")) {
//                        System.out.println(FlowChat.when_last_cmd_sent+(jobj.get("afterSeconds").getAsLong()*1000));
//                        System.out.println("vs");
//                        System.out.println(Instant.now().toEpochMilli());
                        if (FlowChat.when_last_cmd_sent+(jobj.get("afterSeconds").getAsLong()*1000) < Instant.now().toEpochMilli()) {
                            System.out.println("Sending antiAFK message.");
                            MinecraftClient.getInstance().player.sendChatMessage(jobj.get("command").getAsString());
                        }
                    }
                }
            } catch (Exception ignored) {};
        });
    }
}
