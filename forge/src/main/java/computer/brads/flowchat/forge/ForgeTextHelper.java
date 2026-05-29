package computer.brads.flowchat.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.SoundEvents;

public class ForgeTextHelper {
    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }
    public static void playSound(String soundName) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        } catch (Exception ignored) {}
    }
}
