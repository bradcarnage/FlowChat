package computer.brads.flowchat.neoforge;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Mod("flowchat")
public class FlowChatNeoForge {
    public static final String MOD_ID = "flowchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static FlowChatConfig config;
    public static MessageProcessor processor = new MessageProcessor();
    public static long whenLastCmdSent;
    public static long whenLastWorldTick;
    public static String serverIp = "unknown";
    public static boolean stillInVoid = false;
    private static String previousServerIp = "";
    private static long tickCounter = 0;
    private static record PendingCommand(String command, long executeAtTick) {}
    private static final List<PendingCommand> pendingCommands = new ArrayList<>();

    public FlowChatNeoForge() {
        if (FMLLoader.getDist() != Dist.CLIENT) return;

        LOGGER.info("FlowChat initialized (NeoForge client)");
        config = new FlowChatConfig(FMLPaths.CONFIGDIR.get());
        config.load();

        whenLastCmdSent = Instant.now().toEpochMilli();
        whenLastWorldTick = Instant.now().toEpochMilli();

        NeoForge.EVENT_BUS.register(this);
    }

    private String getUsername() {
        try {
            var player = Minecraft.getInstance().player;
            if (player != null) return player.getName().getString();
        } catch (Exception ignored) {}
        return null;
    }

    private String getServerName() {
        try {
            var entry = Minecraft.getInstance().getCurrentServer();
            if (entry != null) return entry.name;
        } catch (Exception ignored) {}
        return "Singleplayer";
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (config == null || config.isDisabled()) return;

        try {
            String text = event.getMessage().getString();
            String username = getUsername();
            String serverName = getServerName();
            MessageProcessor.Result result = processor.process(text, config.getIncomingRules(), serverIp, username, serverName);
            if (result == null) return;

            if (result.cancelled) {
                event.setCanceled(true);
                return;
            }

            if (!result.processedText.equals(result.originalText)) {
                event.setMessage(Component.literal(NeoForgeTextHelper.formatColors(result.processedText)));
            }

            if (result.playSound) {
                NeoForgeTextHelper.playSound(result.soundId);
            }

            if (result.toast) {
                String notifyStyle = result.notifyStyle != null ? result.notifyStyle : "toast";
                switch (notifyStyle) {
                    case "actionbar" -> NeoForgeTextHelper.showActionBar(result.processedText);
                    default -> NeoForgeTextHelper.showToast(result.processedText);
                }
            }

            for (String response : result.autoResponses) {
                NeoForgeTextHelper.sendChat(response);
                whenLastCmdSent = Instant.now().toEpochMilli();
            }
        } catch (Exception e) {
            LOGGER.error("Error processing chat", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        if (config == null || config.isDisabled()) return;
        if (Minecraft.getInstance().level == null) return;

        long now = Instant.now().toEpochMilli();
        if (whenLastWorldTick < now - 1000) {
            serverIp = "singleplayer";
            try {
                var entry = Minecraft.getInstance().getCurrentServer();
                if (entry != null) serverIp = entry.ip;
            } catch (Exception ignored) {}
            config.load();
        }
        whenLastWorldTick = now;

        // Drain pending onJoinServer commands
        tickCounter++;
        if (!pendingCommands.isEmpty()) {
            pendingCommands.removeIf(pc -> {
                if (tickCounter >= pc.executeAtTick()) {
                    try { NeoForgeTextHelper.sendChat(pc.command()); whenLastCmdSent = Instant.now().toEpochMilli(); } catch (Exception ignored) {}
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
                        NeoForgeTextHelper.sendChat(afk.get("command").getAsString());
                        whenLastCmdSent = now;
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
                    var player = Minecraft.getInstance().player;
                    if (player != null && yLevel >= player.getY()) {
                        if (!stillInVoid) {
                            stillInVoid = true;
                            NeoForgeTextHelper.sendChat(vf.get("command").getAsString());
                        }
                    } else {
                        stillInVoid = false;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void handleOnJoinServer(String currentIp) {
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
                    delayTicks += 20;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("onJoinServer error: {}", e.getMessage());
        }
    }
}
