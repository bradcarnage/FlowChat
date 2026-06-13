package computer.brads.flowchat.fabric;

import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.SoundResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FabricChatHelper {
    public static void sendChat(String message) {
        if (MinecraftClient.getInstance().player == null) return;
        MinecraftClient.getInstance().player.sendChatMessage(message);
    }
    public static void showActionBar(String message) {
        if (MinecraftClient.getInstance().player == null) return;
        MinecraftClient.getInstance().player.sendMessage(Text.of(message), true);
    }
    public static void showLocalMessage(String message) {
        if (MinecraftClient.getInstance().player == null) return;
        MinecraftClient.getInstance().player.sendMessage(Text.of(message), false);
    }
    public static void playNotificationSound(String soundName) {
        if (MinecraftClient.getInstance().player == null) return;
        SoundEvent sound = resolveSound(soundName);
        if (sound != null) MinecraftClient.getInstance().player.playSound(sound, 1.0f, 1.0f);
    }
    public static String replaceTags(String input) {
        MinecraftClient client = MinecraftClient.getInstance();
        String username = client.player != null ? client.player.getName().getString() : null;
        String serverName = "Singleplayer";
        if (client.getCurrentServerEntry() != null) serverName = client.getCurrentServerEntry().name;
        return MessageProcessor.replaceTags(input, FlowChatFabric.serverIp, username, serverName);
    }
    private static SoundEvent resolveSound(String name) {
        // Use SoundResolver for consistent alias handling across all platforms
        String resolved = SoundResolver.resolve(name);
        if (resolved == null) return null; // silent
        // Strip minecraft: prefix if present
        String id = resolved.startsWith("minecraft:") ? resolved.substring(10) : resolved;
        // Map common resolved IDs to SoundEvents constants
        switch (id) {
            case "entity.experience_orb.pickup": return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "entity.player.levelup": return SoundEvents.ENTITY_PLAYER_LEVELUP;
            case "block.anvil.land": return SoundEvents.BLOCK_ANVIL_LAND;
            case "block.note_block.bell": return SoundEvents.BLOCK_NOTE_BLOCK_BELL;
            case "ui.button.click": return SoundEvents.UI_BUTTON_CLICK;
            case "entity.item.pickup": return SoundEvents.ENTITY_ITEM_PICKUP;
            default:
                // Arbitrary sound ID — create dynamic SoundEvent
                return new SoundEvent(new Identifier(resolved));
        }
    }
}
