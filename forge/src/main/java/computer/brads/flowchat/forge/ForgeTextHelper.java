package computer.brads.flowchat.forge;

import net.minecraft.client.Minecraft;

public class ForgeTextHelper {
    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "§$1");
    }
    public static void playSound(String soundName) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.getSoundHandler().play(net.minecraft.client.audio.SimpleSound.master(
                    net.minecraft.util.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        } catch (Exception ignored) {}
    }
}
