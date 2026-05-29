package computer.brads.flowchat.forge;

public class ForgeTextHelper {
    public static String formatColors(String text) {
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }
    public static void playSound(String soundName) {
        // Sound notification not supported on Forge 1.14.4 due to mapping differences
        // Chat processing still works without sound
    }
}
