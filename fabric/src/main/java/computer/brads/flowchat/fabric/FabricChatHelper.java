package computer.brads.flowchat.fabric;

import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.SoundResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class FabricChatHelper {

    public static void sendChat(String message) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null) return;
        var connection = client.getConnection();
        if (connection == null) return;
        if (message.startsWith("/")) {
            connection.sendCommand(message.substring(1));
        } else {
            connection.sendChat(message);
        }
    }

    public static void showActionBar(String message) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.sendOverlayMessage(Component.literal(message));
    }

    public static void showLocalMessage(String message) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.sendSystemMessage(Component.literal(message));
    }

    public static void playNotificationSound(String soundName) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        String resolved = SoundResolver.resolve(soundName);
        if (resolved == null) return;
        SoundEvent sound;
        try {
            sound = SoundEvent.createVariableRangeEvent(Identifier.parse(resolved));
        } catch (Exception e) {
            return;
        }
        if (sound != null) player.playSound(sound, 1.0f, 1.0f);
    }

    public static void showToast(String message) {
        var client = Minecraft.getInstance();
        if (client == null) return;
        SystemToast.add(client.getToastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("FlowChat"),
                Component.literal(message));
    }

    public static void showAdvancement(String message) {
        var client = Minecraft.getInstance();
        if (client == null) return;
        SystemToast toast = new SystemToast(
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("\u00a76\u00a7l\u2605 FlowChat"),
                Component.literal(message));
        client.getToastManager().addToast(toast);
    }

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
        var client = Minecraft.getInstance();
        String username = client.player != null ? client.player.getName().getString() : null;
        String serverName = "Singleplayer";
        var entry = client.getCurrentServer();
        if (entry != null) serverName = entry.name;
        return MessageProcessor.replaceTags(input, FlowChatFabric.serverIp, username, serverName);
    }
}
