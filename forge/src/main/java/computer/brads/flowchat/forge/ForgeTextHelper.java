package computer.brads.flowchat.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class ForgeTextHelper {
    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }
    public static void playSound(String soundName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0f, 1.0f));
    }
}
