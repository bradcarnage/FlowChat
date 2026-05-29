package computer.brads.chatflow;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SettingsManager {
    static Path configpath;

    static {
        // Use Fabric's config directory
        configpath = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir()
                .resolve("flowchat.json");
    }

    private static void initSettingsFile() throws IOException {
        // Try migrating from legacy locations
        Path legacyRules = Path.of("flowchat/rules.json");
        Path legacyConfig = Path.of("config/flowchat.json");
        Path legacyProperties = Path.of("config/flowchat.properties");

        if (Files.exists(legacyConfig) && !Files.exists(configpath)) {
            Files.move(legacyConfig, configpath);
            FlowChat.LOGGER.info("Migrated legacy config from config/flowchat.json");
        } else if (Files.exists(legacyRules) && !Files.exists(configpath)) {
            Files.move(legacyRules, configpath);
            FlowChat.LOGGER.info("Migrated legacy config from flowchat/rules.json");
        } else {
            FlowChat.LOGGER.info("Creating new config at {}", configpath);
            Files.writeString(configpath, "{\n  \"incoming\": [],\n  \"outgoing\": []\n}\n");
        }

        // Clean up legacy files
        try {
            Files.deleteIfExists(legacyRules);
            Path legacyDir = Path.of("flowchat");
            if (Files.exists(legacyDir) && Files.isDirectory(legacyDir)) {
                Files.deleteIfExists(legacyDir);
            }
        } catch (IOException ignored) {}
        try { Files.deleteIfExists(legacyProperties); } catch (IOException ignored) {}
    }

    public static boolean loadFilterRules() {
        try (Reader reader = Files.newBufferedReader(configpath, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            FlowChat.filter_rules = parsed.getAsJsonObject();
            FlowChat.LOGGER.debug("Loaded config from {}", configpath.getFileName());
            return true;
        } catch (Exception ex) {
            try {
                initSettingsFile();
                try (Reader reader = Files.newBufferedReader(configpath, StandardCharsets.UTF_8)) {
                    FlowChat.filter_rules = JsonParser.parseReader(reader).getAsJsonObject();
                    FlowChat.LOGGER.info("Loaded config after migration/creation");
                }
            } catch (IOException e) {
                FlowChat.LOGGER.error("Could not read config file {}", configpath.getFileName(), e);
            }
            return false;
        }
    }
}
