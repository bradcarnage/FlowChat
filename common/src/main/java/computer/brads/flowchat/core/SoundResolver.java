package computer.brads.flowchat.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified sound resolution for all platforms.
 * Resolves named aliases to full Minecraft sound identifiers,
 * and passes through arbitrary identifiers unchanged.
 */
public class SoundResolver {
    private static final String DEFAULT_SOUND = "minecraft:entity.experience_orb.pickup";
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        ALIASES.put("ding", "minecraft:entity.experience_orb.pickup");
        ALIASES.put("orb", "minecraft:entity.experience_orb.pickup");
        ALIASES.put("levelup", "minecraft:entity.player.levelup");
        ALIASES.put("level", "minecraft:entity.player.levelup");
        ALIASES.put("anvil", "minecraft:block.anvil.land");
        ALIASES.put("note", "minecraft:block.note_block.bell");
        ALIASES.put("bell", "minecraft:block.note_block.bell");
        ALIASES.put("click", "minecraft:ui.button.click");
        ALIASES.put("pop", "minecraft:entity.item.pickup");
    }

    /**
     * Resolve a sound field value to a full Minecraft sound identifier.
     *
     * @param value the raw sound field value — named alias, full identifier, "none"/"silent", null, or empty
     * @return full Minecraft sound identifier (e.g. "minecraft:block.note_block.bell"),
     *         or null if the sound should be silent
     */
    public static String resolve(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_SOUND;
        }

        String lower = value.toLowerCase();

        if ("none".equals(lower) || "silent".equals(lower)) {
            return null;
        }

        String alias = ALIASES.get(lower);
        if (alias != null) {
            return alias;
        }

        // Treat as arbitrary Minecraft sound identifier
        // Add minecraft: prefix if no namespace specified
        if (!value.contains(":")) {
            return "minecraft:" + value;
        }
        return value;
    }

    /**
     * Check if a sound value indicates sound should be played.
     * Handles boolean-like values from the unified sound field.
     *
     * @param value raw value from config — could be string, "true"/"false", etc.
     * @return true if sound should play
     */
    public static boolean shouldPlay(String value) {
        if (value == null) return false;
        if ("false".equalsIgnoreCase(value)) return false;
        if ("none".equalsIgnoreCase(value) || "silent".equalsIgnoreCase(value)) return false;
        return true;
    }

    /**
     * @return the default sound identifier used when no sound name is specified
     */
    public static String getDefault() {
        return DEFAULT_SOUND;
    }
}
