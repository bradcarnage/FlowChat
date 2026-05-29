package computer.brads.chatflow;

import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class FlowChat implements ClientModInitializer {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static JsonObject filter_rules;
    public static String last_cmd_sent;
    public static long when_last_cmd_sent;
    public static long when_last_worldtick;
    public static String server_ip = "unknown";
    public static boolean still_in_void = false;
    public static boolean disabled = false;
    public static HashMap<String, SVCP> stacked_value_cacher = new HashMap<>();

    public static class SVCP {
        public HashMap<Integer, Double> stacked_values = new HashMap<>();
        public int expire_after_epoch;
        public int iter_count = 0;

        public SVCP(int expire_sec) {
            LOGGER.debug("Creating new value stacker with {}s expiry", expire_sec);
            this.expire_after_epoch = (int) ((Instant.now().toEpochMilli() / 1000) + expire_sec);
        }
    }

    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        LOGGER.info("FlowChat {} initialized", version);

        SettingsManager.loadFilterRules();
        when_last_cmd_sent = Instant.now().toEpochMilli();
        when_last_worldtick = Instant.now().toEpochMilli();

        ClientTickEvents.START_WORLD_TICK.register(world -> {
            long epochMilli = Instant.now().toEpochMilli();
            try {
                if (when_last_worldtick < epochMilli - 1000) {
                    server_ip = "singleplayer";
                    try {
                        var entry = MinecraftClient.getInstance().getCurrentServerEntry();
                        if (entry != null) {
                            server_ip = entry.address;
                        }
                    } catch (Exception ignored) {}
                    LOGGER.debug("WorldTicks paused >1s; server IP: {}", server_ip);
                    SettingsManager.loadFilterRules();
                }
                when_last_worldtick = epochMilli;
            } catch (Exception ignored) {}

            if (filter_rules == null || disabled) return;

            // Anti-AFK
            try {
                if (filter_rules.has("antiAFK")) {
                    JsonObject jobj = filter_rules.get("antiAFK").getAsJsonObject();
                    if (!jobj.has("serversearch") || server_ip.matches(jobj.get("serversearch").getAsString())) {
                        if (jobj.has("afterSeconds") && jobj.has("command")) {
                            if (when_last_cmd_sent + (jobj.get("afterSeconds").getAsLong() * 1000) < epochMilli) {
                                LOGGER.debug("Sending antiAFK message");
                                ChatHelper.sendChat(jobj.get("command").getAsString());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Void fall protection
            try {
                if (filter_rules.has("voidFall")) {
                    JsonObject jobj = filter_rules.get("voidFall").getAsJsonObject();
                    if (!jobj.has("serversearch") || server_ip.matches(jobj.get("serversearch").getAsString())) {
                        if (jobj.has("command")) {
                            double ylevel = jobj.has("yLevel") ? jobj.get("yLevel").getAsDouble() : -20;
                            var player = MinecraftClient.getInstance().player;
                            if (player != null && ylevel >= player.getY()) {
                                if (!still_in_void) {
                                    still_in_void = true;
                                    String command = jobj.get("command").getAsString();
                                    LOGGER.debug("Sending voidFall command: {}", command);
                                    ChatHelper.sendChat(command);
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
