package computer.brads.flowchat.fabric;

import computer.brads.flowchat.core.MessageProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class FabricChatHelper {
    public static void sendChat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.player.sendChatMessage(message);
    }
    public static void showActionBar(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.player.sendMessage(Text.of(message), true);
    }
    public static void showLocalMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.player.sendMessage(Text.of(message), false);
    }
    public static void playNotificationSound(String soundName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        SoundEvent sound = resolveSound(soundName);
        if (sound != null) client.player.playSound(sound, 1.0f, 1.0f);
    }
    public static String replaceTags(String input) {
        MinecraftClient client = MinecraftClient.getInstance();
        String username = client.player != null ? client.player.getName().getString() : null;
        String serverName = "Singleplayer";
        if (client.getCurrentServerEntry() != null) serverName = client.getCurrentServerEntry().name;
        return MessageProcessor.replaceTags(input, FlowChatFabric.serverIp, username, serverName);
    }
    private static SoundEvent resolveSound(String name) {
        if (name == null || name.isEmpty()) return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        String lower = name.toLowerCase();
        if (lower.equals("ding") || lower.equals("orb")) return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        if (lower.equals("levelup") || lower.equals("level")) return SoundEvents.ENTITY_PLAYER_LEVELUP;
        if (lower.equals("anvil")) return SoundEvents.BLOCK_ANVIL_LAND;
        if (lower.equals("note") || lower.equals("bell")) return SoundEvents.BLOCK_NOTE_BLOCK_BELL;
        if (lower.equals("click")) return SoundEvents.UI_BUTTON_CLICK;
        if (lower.equals("pop")) return SoundEvents.ENTITY_ITEM_PICKUP;
        if (lower.equals("none") || lower.equals("silent")) return null;
        return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
    }
}
