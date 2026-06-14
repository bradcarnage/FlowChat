package computer.brads.flowchat.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import computer.brads.flowchat.core.SoundResolver;

public class ForgeTextHelper {
    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }
    public static void playSound(String soundName) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // Resolve aliases via shared SoundResolver first
                String resolved = SoundResolver.resolve(soundName);
                if (resolved == null) return; // "none"/"silent" — skip
                SoundEvent sound = resolveSoundEvent(resolved);
                mc.getSoundManager().play(SimpleSound.forUI(sound, 1.0f));
            }
        } catch (Exception ignored) {}
    }
    private static SoundEvent resolveSoundEvent(String soundName) {
        // Strip minecraft: prefix if present
        String id = soundName.startsWith("minecraft:") ? soundName.substring(10) : soundName;
        // Map common resolved IDs to SoundEvents constants
        switch (id) {
            case "entity.experience_orb.pickup": return SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "entity.player.levelup": return SoundEvents.PLAYER_LEVELUP;
            case "block.anvil.land": return SoundEvents.ANVIL_LAND;
            case "block.note_block.bell": return SoundEvents.NOTE_BLOCK_BELL;
            case "ui.button.click": return SoundEvents.UI_BUTTON_CLICK;
            case "entity.item.pickup": return SoundEvents.ITEM_PICKUP;
            default:
                // Arbitrary sound ID — create dynamic SoundEvent
                return new SoundEvent(new ResourceLocation(soundName.contains(":") ? soundName : "minecraft:" + soundName));
        }
    }
}
