package computer.brads.flowchat.fabric;

import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.SoundResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
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

    public static String replaceTags(String input) {
        var client = MinecraftClient.getInstance();
        String username = client.player != null ? client.player.getName().getString() : null;
        String serverName = "Singleplayer";
        var entry = client.getCurrentServerEntry();
        if (entry != null) serverName = entry.name;
        return MessageProcessor.replaceTags(input, FlowChatFabric.serverIp, username, serverName);
    }
}
