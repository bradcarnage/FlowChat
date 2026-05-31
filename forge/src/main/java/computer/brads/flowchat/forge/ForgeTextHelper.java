package computer.brads.flowchat.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;

public class ForgeTextHelper {
    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }
    public static void playSound(String soundName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        SoundEvent sound;
        switch (soundName != null ? soundName.toLowerCase() : "bell") {
            case "note": case "bell": sound = SoundEvents.NOTE_BLOCK_BELL.get(); break;
            case "click": sound = SoundEvents.UI_BUTTON_CLICK.get(); break;
            default: sound = SoundEvents.NOTE_BLOCK_BELL.get(); break;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f, 1.0f));
    }
}
