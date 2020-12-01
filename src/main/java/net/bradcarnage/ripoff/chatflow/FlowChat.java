package net.bradcarnage.ripoff.chatflow;

import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

@Environment(EnvType.CLIENT)
public class FlowChat implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static JsonObject filter_rules;
    public static String last_cmd_sent;
    public static Long when_last_cmd_sent;
    public static String server_ip;
    public static Boolean still_in_void = false;

    public static boolean disabled;
    @Override
    public void onInitializeClient() {
        LOGGER.info("FlowChat " + FabricLoader.getInstance().getModContainer("flowchat").get().getMetadata().getVersion() + " Initialized");
        SettingsManager.initSettingsClient();
        FlowChat.when_last_cmd_sent = Instant.now().toEpochMilli();

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            try {
//                System.out.println("Player Respawned");
                server_ip = "singleplayer";
                try { server_ip = MinecraftClient.getInstance().getCurrentServerEntry().address; } catch (Exception ignored) { }
                System.out.println("Fetched server IP: "+server_ip);
            } catch (Exception ignored) {};
        });
        ClientTickEvents.START_WORLD_TICK.register(client -> {
            try {
                if (FlowChat.filter_rules.has("antiAFK")) {
                    JsonObject jobj = FlowChat.filter_rules.get("antiAFK").getAsJsonObject();
                    if (!jobj.has("serversearch") || server_ip.matches(jobj.get("serversearch").getAsString())) {
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
                }
            } catch (Exception ignored) {};
            try {
                if (FlowChat.filter_rules.has("voidFall")) {
                    JsonObject jobj = FlowChat.filter_rules.get("voidFall").getAsJsonObject();
                    if (!jobj.has("serversearch") || server_ip.matches(jobj.get("serversearch").getAsString())) {
                        if (jobj.has("command")) {
                            double ylevel = -20;
                            if (jobj.has("yLevel")) { ylevel = jobj.get("yLevel").getAsDouble(); }
                            if (ylevel >= MinecraftClient.getInstance().player.getY()) {
                                if (!still_in_void) {
                                    still_in_void = true;
                                    String command = jobj.get("command").getAsString();
                                    System.out.println("Sending inVoid message: "+ command);
                                    MinecraftClient.getInstance().player.sendChatMessage(command);
                                }
                            } else {
                                still_in_void = false;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {};
        });
    }
}
