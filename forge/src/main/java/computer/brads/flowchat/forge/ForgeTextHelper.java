package computer.brads.flowchat.forge;

import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.core.SoundResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ForgeTextHelper {

    public static String formatColors(String text) {
        return MessageProcessor.formatColors(text);
    }

    public static void playSound(String soundId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String resolved = SoundResolver.resolve(soundId);
        if (resolved == null) return; // silent

        try {
            ResourceLocation loc = ResourceLocation.parse(resolved);
            SoundEvent sound = SoundEvent.createVariableRangeEvent(loc);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f, 1.0f));
        } catch (Exception e) {
            // Fallback: ignore invalid sound identifiers
        }
    }

    public static void showToast(String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToastManager().addToast(SystemToast.multiline(
                mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("FlowChat"),
                Component.literal(message)
        ));
    }

    public static void showActionBar(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.displayClientMessage(Component.literal(message), true);
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
