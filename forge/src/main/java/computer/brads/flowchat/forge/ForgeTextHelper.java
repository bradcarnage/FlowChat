package computer.brads.flowchat.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class ForgeTextHelper {

    public static String formatColors(String text) {
        // Convert & color codes to section symbol
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }

    public static void playSound(String soundName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SoundEvent sound;
        switch (soundName != null ? soundName.toLowerCase() : "bell") {
            case "note", "bell" -> sound = SoundEvents.NOTE_BLOCK_BELL.value();
            case "click" -> sound = SoundEvents.UI_BUTTON_CLICK.value();
            default -> sound = SoundEvents.NOTE_BLOCK_BELL.value();
        }

        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f, 1.0f));
    }

    public static void showToast(String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(SystemToast.multiline(
                mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("FlowChat"),
                Component.literal(message)
        ));
    }

    public static void sendChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (message.startsWith("/")) {
                mc.player.connection.sendCommand(message.substring(1));
            } else {
                mc.player.connection.sendChat(message);
            }
        }
    }
}
