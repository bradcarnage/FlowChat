package computer.brads.flowchat.forge;

public class ForgeTextHelper {
    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }
    public static void playSound(String soundName) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.playSound(net.minecraft.init.SoundEvents.BLOCK_NOTE_HARP, 1.0f, 1.0f);
            }
        } catch (Exception ignored) {}
    }
}
