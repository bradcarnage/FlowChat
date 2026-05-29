package computer.brads.chatflow;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Helper for sending chat messages and commands.
 * Abstracts the sendChatMessage/sendCommand split that happened in 1.19.1.
 * In 1.21.x, chat and commands are separate methods.
 */
public class ChatHelper {

    /**
     * Send a chat message or command. Automatically routes commands (starting with /)
     * through sendCommand and regular messages through sendChatMessage.
     */
    public static void sendChat(String message) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;

        if (message.startsWith("/")) {
            // In 1.19.1+, commands go through sendCommand (without the leading /)
            player.networkHandler.sendCommand(message.substring(1));
        } else {
            player.networkHandler.sendChatMessage(message);
        }
    }

    /**
     * Display a message on the action bar (toast).
     */
    public static void showActionBar(String message) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.sendMessage(Text.of(message), true);
    }

    /**
     * Display a message in the player's chat HUD (local only, not sent to server).
     */
    public static void showLocalMessage(String message) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.sendMessage(Text.of(message), false);
    }

    /**
     * Play a notification sound. Supports sound names from config.
     * Default: ENTITY_EXPERIENCE_ORB_PICKUP (the classic "ding")
     */
    public static void playNotificationSound(String soundName) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;

        SoundEvent sound = resolveSound(soundName);
        if (sound != null) {
            player.playSound(sound, 1.0f, 1.0f);
        }
    }

    /**
     * Resolve a sound name string to a SoundEvent.
     * Accepts: "ding", "levelup", "anvil", "note", "click",
     * or full identifiers like "minecraft:entity.experience_orb.pickup"
     */
    private static SoundEvent resolveSound(String name) {
        if (name == null || name.isEmpty()) {
            return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        }

        return switch (name.toLowerCase()) {
            case "ding", "orb" -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "levelup", "level" -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            case "anvil" -> SoundEvents.BLOCK_ANVIL_LAND;
            case "note", "bell" -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case "click" -> SoundEvents.UI_BUTTON_CLICK.value();
            case "pop" -> SoundEvents.ENTITY_ITEM_PICKUP;
            case "none", "silent" -> null;
            default -> {
                // Try resolving as a full identifier
                try {
                    Identifier id = Identifier.of(name);
                    yield SoundEvent.of(id);
                } catch (Exception e) {
                    FlowChat.LOGGER.warn("Unknown sound '{}', using default", name);
                    yield SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                }
            }
        };
    }

    /**
     * Replace tag variables in a string.
     * Supported tags: {username}, {servername}, {serverip}, {time}
     */
    public static String replaceTags(String input) {
        if (input == null || !input.contains("{")) return input;

        var client = MinecraftClient.getInstance();
        String result = input;

        if (result.contains("{username}") && client.player != null) {
            result = result.replace("{username}", client.player.getName().getString());
        }
        if (result.contains("{serverip}")) {
            result = result.replace("{serverip}", FlowChat.server_ip);
        }
        if (result.contains("{servername}")) {
            String serverName = "Unknown";
            var entry = client.getCurrentServerEntry();
            if (entry != null) {
                serverName = entry.name;
            } else {
                serverName = "Singleplayer";
            }
            result = result.replace("{servername}", serverName);
        }
        if (result.contains("{time}")) {
            result = result.replace("{time}",
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        }

        return result;
    }
}
