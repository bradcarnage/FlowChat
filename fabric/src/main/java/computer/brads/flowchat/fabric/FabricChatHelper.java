package computer.brads.flowchat.fabric;

import computer.brads.flowchat.core.MessageProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FabricChatHelper {
    public static void sendChat(String message) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (message.startsWith("/")) { player.networkHandler.sendCommand(message.substring(1)); }
        else { player.networkHandler.sendChatMessage(message); }
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
        SoundEvent sound = resolveSound(soundName);
        if (sound != null) player.playSound(sound, 1.0f, 1.0f);
    }
    public static String replaceTags(String input) {
        var client = MinecraftClient.getInstance();
        String username = client.player != null ? client.player.getName().getString() : null;
        String serverName = "Singleplayer";
        var entry = client.getCurrentServerEntry();
        if (entry != null) serverName = entry.name;
        return MessageProcessor.replaceTags(input, FlowChatFabric.serverIp, username, serverName);
    }
    private static SoundEvent resolveSound(String name) {
        if (name == null || name.isEmpty()) return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        return switch (name.toLowerCase()) {
            case "ding", "orb" -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "levelup", "level" -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            case "anvil" -> SoundEvents.BLOCK_ANVIL_LAND;
            case "note", "bell" -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case "click" -> SoundEvents.UI_BUTTON_CLICK.value();
            case "pop" -> SoundEvents.ENTITY_ITEM_PICKUP;
            case "none", "silent" -> null;
            default -> {
                try { yield SoundEvent.of(new Identifier(name)); }
                catch (Exception e) { yield SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP; }
            }
        };
    }
}
