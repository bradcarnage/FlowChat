package computer.brads.flowchat.fabric;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class FlowChatFabric implements ClientModInitializer {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static String lastCmdSent;
    public static long whenLastCmdSent;
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";
    public static boolean stillInVoid = false;
    private static String previousServerIp = "";
    private static long tickCounter = 0;
    private static record PendingCommand(String command, long executeAtTick) {}
    private static final List<PendingCommand> pendingCommands = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        LOGGER.info("FlowChat {} initialized (Fabric client)", version);

        config = new FlowChatConfig(FabricLoader.getInstance().getConfigDir());
        config.load();

        whenLastCmdSent = Instant.now().toEpochMilli();
        whenLastWorldTick = Instant.now().toEpochMilli();

        ClientTickEvents.START_WORLD_TICK.register(world -> {
            long now = Instant.now().toEpochMilli();
            try {
                if (whenLastWorldTick < now - 1000) {
                    serverIp = "singleplayer";
                    try {
                        var entry = MinecraftClient.getInstance().getCurrentServerEntry();
                        if (entry != null) serverIp = entry.address;
                    } catch (Exception ignored) {}
                    config.load();
                }
                whenLastWorldTick = now;
            } catch (Exception ignored) {}

            if (config.isDisabled()) return;

            // Drain pending onJoinServer commands
            tickCounter++;
            if (!pendingCommands.isEmpty()) {
                pendingCommands.removeIf(pc -> {
                    if (tickCounter >= pc.executeAtTick()) {
                        try { FabricChatHelper.sendChat(pc.command()); } catch (Exception ignored) {}
                        return true;
                    }
                    return false;
                });
            }

            // Detect server join
            if (!serverIp.equals(previousServerIp)) {
                previousServerIp = serverIp;
                handleOnJoinServer(serverIp);
            }

            // Anti-AFK
            try {
                var afk = config.getAntiAfk();
                if (afk != null && (!afk.has("serversearch") || serverIp.matches(afk.get("serversearch").getAsString()))) {
                    if (afk.has("afterSeconds") && afk.has("command")) {
                        if (whenLastCmdSent + (afk.get("afterSeconds").getAsLong() * 1000) < now) {
                            FabricChatHelper.sendChat(afk.get("command").getAsString());
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Void fall
            try {
                var vf = config.getVoidFall();
                if (vf != null && (!vf.has("serversearch") || serverIp.matches(vf.get("serversearch").getAsString()))) {
                    if (vf.has("command")) {
                        double yLevel = vf.has("yLevel") ? vf.get("yLevel").getAsDouble() : -20;
                        var player = MinecraftClient.getInstance().player;
                        if (player != null && yLevel >= player.getY()) {
                            if (!stillInVoid) { stillInVoid = true; FabricChatHelper.sendChat(vf.get("command").getAsString()); }
                        } else { stillInVoid = false; }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private static void handleOnJoinServer(String currentIp) {
        try {
            for (JsonObject entry : config.getOnJoinServer()) {
                if (entry.has("server")) {
                    String regex = entry.get("server").getAsString();
                    if (!currentIp.matches(regex)) continue;
                }
                JsonArray cmds = entry.getAsJsonArray("commands");
                if (cmds == null) continue;
                int delaySec = entry.has("delay") ? entry.get("delay").getAsInt() : 0;
                long delayTicks = delaySec * 20L;
                for (JsonElement cmd : cmds) {
                    pendingCommands.add(new PendingCommand(cmd.getAsString(), tickCounter + delayTicks));
                    delayTicks += 20; // 1 second gap between commands
                }
            }
        } catch (Exception e) {
            LOGGER.warn("onJoinServer error: {}", e.getMessage());
        }
    }
}
