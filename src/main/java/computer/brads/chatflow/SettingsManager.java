package computer.brads.chatflow;

import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsManager {
    static Path configpath = Paths.get("config/flowchat.json");

    private static void initSettingsFile() throws IOException {
        Path temp = null;
        try {
            temp = Files.move(Paths.get("flowchat/rules.json"), configpath);
        } catch (IOException ignored) {}
        if(temp != null)
        {
            System.out.println("Moved legacy config file to proper directory. ("+configpath.getFileName()+")");
        }
        else
        {
            System.out.println("Could find a config file; creating new one.");
            BufferedWriter bw = Files.newBufferedWriter(configpath);
            bw.write("{}");
            bw.newLine();
            bw.close();
        }
        try {
            Files.delete(Paths.get("flowchat"));
            System.out.println("Deleted legacy \"flowchat\" folder (No longer in use)");
        } catch (IOException ignored) {}
        if(new File("config/flowchat.properties").delete()) {
            System.out.println("Deleted legacy \"config/flowchat.properties\" (No longer in use)");
        }
    }

    public static boolean loadFilterRules() {
        try (Reader reader = Files.newBufferedReader(configpath, StandardCharsets.UTF_8)) {
            FlowChat.filter_rules = new JsonParser().parse(reader).getAsJsonObject();
            System.out.println("read json rule file "+configpath.getFileName()+" successfully");
            return true;
        } catch (Exception ex) {
            try {
                initSettingsFile();
                Reader reader = Files.newBufferedReader(configpath, StandardCharsets.UTF_8);
                FlowChat.filter_rules = new JsonParser().parse(reader).getAsJsonObject();
                System.out.println("read json rule file "+configpath.getFileName()+" successfully (After Migration/Creation)");
            } catch (IOException e) {
                System.out.println("could not read json rule file "+configpath.getFileName());
                e.printStackTrace();
            }
            return false;
        }
    }
}