package net.bradcarnage.ripoff.chatflow;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static net.bradcarnage.ripoff.chatflow.SettingsManager.loadFilterRules;

public class SettingsManager {
    public static File settingsFile = new File("config/flowchat.properties");

    public static final String[][] settingsClient = {
            {"filterChat", "true"},
            {"regexRuleFile", "rules.json"}
    };

    private static void initSettingsFile(String[][] settings) {
        // Init settings file if it doesn't exist
        if (!settingsFile.exists()) {
            File configDir = new File("config/");
            if (!configDir.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                configDir.mkdir();
            }
            try {
                boolean fileCreated = settingsFile.createNewFile();

                if (fileCreated) {
                    Properties prop = new Properties();
                    for (String[] setting : settings) {
                        prop.put(setting[0], setting[1]);
                    }

                    BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile));
                    prop.store(writer, null);
                    writer.flush();
                    writer.close();
                } else {
                    throw new IOException();
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not create settings file for FlowChat!");
            }
        } else { // If the file does exist, make sure that it has all the settings and generate the ones that don't exist
            try {
                Properties prop = new Properties();
                prop.load(new BufferedReader(new FileReader(settingsFile)));
                for (String[] setting : settings) {
                    if (prop.getProperty(setting[0]) == null) {
                        prop.put(setting[0], setting[1]);
                    }
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile));
                prop.store(writer, null);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not create settings file for FlowChat!");
            }
        }
        loadFilterRules();
    }

    /**
     * Initialize the settings file on startup.
     */
    public static void initSettingsClient() {
        initSettingsFile(settingsClient);
    }

    /**
     * @param setting Name of the setting to load.
     * @return The setting value.
     */
    public static String loadSetting(String setting) {
        BufferedReader reader;
        Properties prop = new Properties();

        try {
            reader = new BufferedReader(new FileReader(settingsFile));
            prop.load(reader);
            reader.close();

            return prop.getProperty(setting);
        } catch (IOException e) {
            throw new RuntimeException("Can't read settings for FlowChat!");
        }
    }

    public static boolean loadFilterRules() {
        Path path = Paths.get("flowchat/"+SettingsManager.loadSetting("regexRuleFile"));
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            FlowChat.filter_rules = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element: FlowChat.filter_rules) {
                JsonObject jobj = element.getAsJsonObject();
                System.out.println(jobj.get("search").getAsString());
                System.out.println(jobj.get("replacement").getAsString());
                System.out.println(jobj.get("toastMe").getAsBoolean());
            }
            System.out.println("read json rule file "+path.getFileName()+" successfully");
            return true;
        } catch (Exception ex) {
            System.out.println("could not read json rule file"+path.getFileName());
            ex.printStackTrace();
            return false;
        }
    }



    public static boolean loadBooleanSetting(String setting) {
        return Boolean.parseBoolean(loadSetting(setting));
    }

    /**
     * @param key      Name of setting to write to.
     * @param setpoint What to set the setting to.
     */
    public static void writeSetting(String key, String setpoint) {
        Properties prop = new Properties();
        BufferedReader reader;
        BufferedWriter writer;

        try {
            reader = new BufferedReader(new FileReader(settingsFile));
            prop.load(reader);
            reader.close();

            prop.setProperty(key, setpoint);

            writer = new BufferedWriter(new FileWriter(settingsFile));
            prop.store(writer, null);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("Can't write setting...");
        }
    }
}