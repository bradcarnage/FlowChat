package computer.brads.flowchat.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class NeoForgeTextHelper {

    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }

    public static void playSound(String soundName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0f, 1.0f));
    }

    public static void showToast(String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(SystemToast.multiline(
                mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("FlowChat"),
                Component.literal(message)
        ));
    }
}
