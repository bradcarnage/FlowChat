package computer.brads.flowchat.fabric;

import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.SoundResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FabricChatHelper {

    public static void sendChat(String message) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (message.startsWith("/")) {
            player.networkHandler.sendCommand(message.substring(1));
        } else {
            player.networkHandler.sendChatMessage(message);
        }
    }

    public static void showActionBar(String message) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.sendMessage(Text.of(message), true);
    }

    public static void showLocalMessage(String message) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.sendMessage(Text.of(message), false);
    }

    public static void playNotificationSound(String soundName) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        String resolved = SoundResolver.resolve(soundName);
        if (resolved == null) return;
        SoundEvent sound;
        try {
            sound = SoundEvent.of(Identifier.of(resolved));
        } catch (Exception e) {
            return;
        }
        if (sound != null) player.playSound(sound, 1.0f, 1.0f);
    }

    public static void showToast(String message) {
        var client = MinecraftClient.getInstance();
        if (client == null) return;
        SystemToast.show(client.getToastManager(),
                SystemToast.Type.PERIODIC_NOTIFICATION,
                Text.of("FlowChat"),
                Text.of(message));
    }

    /**
     * Feature #9: Show an advancement-style notification.
     * Renders as the golden "Achievement Get!" style popup in the top-right corner.
     */
    public static void showAdvancement(String message) {
        var client = MinecraftClient.getInstance();
        if (client == null) return;
        // Use SystemToast with a custom type — this renders in the toast area
        // For a more authentic advancement look, we'd need to send a fake advancement packet,
        // but SystemToast with PERIODIC_NOTIFICATION is the cleanest cross-version approach.
        // The key difference from showToast: title says "FlowChat Alert" and uses distinct type.
        SystemToast toast = new SystemToast(
                SystemToast.Type.PERIODIC_NOTIFICATION,
                Text.of("\u00a76\u00a7l\u2605 FlowChat"),  // Gold bold star
                Text.of(message));
        client.getToastManager().add(toast);
    }

    /**
     * Show notification based on notifyStyle.
     * Handles "actionbar", "toast", and "advancement" styles.
     */
    public static void showNotification(String message, String notifyStyle) {
        switch (notifyStyle) {
            case "advancement":
                showAdvancement(message);
                break;
            case "toast":
                showToast(message);
                break;
            case "actionbar":
            default:
                showActionBar(message);
                break;
        }
    }

    public static String replaceTags(String input) {
        var client = MinecraftClient.getInstance();
        String username = client.player != null ? client.player.getName().getString() : null;
        String serverName = "Singleplayer";
        var entry = client.getCurrentServerEntry();
        if (entry != null) serverName = entry.name;
        return MessageProcessor.replaceTags(input, FlowChatFabric.serverIp, username, serverName);
    }
}
