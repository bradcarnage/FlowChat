package computer.brads.chatflow;

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
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class FlowChat implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static JsonObject filter_rules;
    public static String last_cmd_sent;
    public static Long when_last_cmd_sent;
    public static Long when_last_worldtick;
    public static String server_ip;
    public static Boolean still_in_void = false;
    public static HashMap<String, SVCP> stacked_value_cacher = new HashMap<>();

    public static class SVCP {
        public HashMap<Integer, Double> stacked_values = new HashMap<Integer,Double>() {{}};
        public int expire_after_epoch;
        public int iter_count = 0;
        public SVCP(int expire_sec) {
            System.out.println("INIT THE THING");
            this.expire_after_epoch = (int) ((Instant.now().toEpochMilli()/1000)+expire_sec);
        }
    }

    public static boolean disabled;
    @Override
    public void onInitializeClient() {
        LOGGER.info("FlowChat " + FabricLoader.getInstance().getModContainer("flowchat").get().getMetadata().getVersion() + " Initialized");
        SettingsManager.loadFilterRules();
        when_last_cmd_sent = Instant.now().toEpochMilli();
        when_last_worldtick = Instant.now().toEpochMilli();
        ClientTickEvents.START_WORLD_TICK.register(client -> {
            long epochMilli = Instant.now().toEpochMilli();
            try {
                if (when_last_worldtick < epochMilli-1000) {
                    server_ip = "singleplayer";
                    try { server_ip = MinecraftClient.getInstance().getCurrentServerEntry().address; } catch (Exception ignored) { }
                    System.out.println("WorldTicks stopped for a second; Fetched server IP: "+server_ip);
                    SettingsManager.loadFilterRules();
                }
                when_last_worldtick = epochMilli;
            } catch (Exception ignored) {}
            try {
                if (FlowChat.filter_rules.has("antiAFK")) {
                    JsonObject jobj = FlowChat.filter_rules.get("antiAFK").getAsJsonObject();
                    if (!jobj.has("serversearch") || server_ip.matches(jobj.get("serversearch").getAsString())) {
                        if (jobj.has("afterSeconds") && jobj.has("command")) {
                            if (FlowChat.when_last_cmd_sent+(jobj.get("afterSeconds").getAsLong()*1000) < epochMilli) {
                                System.out.println("Sending antiAFK message.");
                                MinecraftClient.getInstance().player.sendChatMessage(jobj.get("command").getAsString());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            try {
                if (FlowChat.filter_rules.has("voidFall")) {
                    JsonObject jobj = FlowChat.filter_rules.get("voidFall").getAsJsonObject();
                    if (!jobj.has("serversearch") || server_ip.matches(jobj.get("serversearch").getAsString())) {
                        if (jobj.has("command")) {
                            double ylevel = -20;
                            if (jobj.has("yLevel")) { ylevel = jobj.get("yLevel").getAsDouble(); }
                            if (ylevel >= MinecraftClient.getInstance().player.getY()) {
//                                run command only once, set a still_in_void flag that needs to get unset first.
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
            } catch (Exception ignored) {}
        });
    }
}
