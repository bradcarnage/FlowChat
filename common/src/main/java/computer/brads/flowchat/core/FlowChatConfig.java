package computer.brads.flowchat.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowChatConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowchat");

    private final Path configPath;
    private JsonObject rawConfig;
    private List<FlowChatRule> incomingRules = Collections.emptyList();
    private List<FlowChatRule> outgoingRules = Collections.emptyList();
    private JsonObject antiAfk;
    private JsonObject voidFall;
    private List<JsonObject> onJoinServer = Collections.emptyList();
    private TagSettings tagSettings = TagSettings.DEFAULT;
    private boolean disabled = false;

    public FlowChatConfig(Path configDir) {
        this.configPath = configDir.resolve("flowchat.json");
    }

    public boolean load() {
        try {
            if (!Files.exists(configPath)) { createDefault(); }
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                rawConfig = JsonParser.parseReader(reader).getAsJsonObject();
            }
            parseRules();
            LOGGER.debug("Loaded FlowChat config from {}", configPath);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load config from {}", configPath, e);
            return false;
        }
    }

    private void createDefault() throws IOException {
        Files.createDirectories(configPath.getParent());
        Files.write(configPath, "{\n  \"incoming\": [],\n  \"outgoing\": []\n}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        LOGGER.info("Created default config at {}", configPath);
    }

    private void parseRules() {
        incomingRules = parseRuleArray("incoming");
        outgoingRules = parseRuleArray("outgoing");
        antiAfk = rawConfig.has("antiAFK") ? rawConfig.getAsJsonObject("antiAFK") : null;
        voidFall = rawConfig.has("voidFall") ? rawConfig.getAsJsonObject("voidFall") : null;
        tagSettings = rawConfig.has("tagSettings") ? new TagSettings(rawConfig.getAsJsonObject("tagSettings")) : TagSettings.DEFAULT;

        // onJoinServer — array of JsonObjects
        if (rawConfig.has("onJoinServer") && rawConfig.get("onJoinServer").isJsonArray()) {
            List<JsonObject> entries = new ArrayList<>();
            for (JsonElement elem : rawConfig.getAsJsonArray("onJoinServer")) {
                try { entries.add(elem.getAsJsonObject()); }
                catch (Exception e) { LOGGER.warn("Skipping malformed onJoinServer entry: {}", e.getMessage()); }
            }
            onJoinServer = entries;
        } else {
            onJoinServer = Collections.emptyList();
        }
    }

    private List<FlowChatRule> parseRuleArray(String key) {
        if (rawConfig == null || !rawConfig.has(key)) return Collections.emptyList();
        List<FlowChatRule> rules = new ArrayList<>();
        for (JsonElement elem : rawConfig.getAsJsonArray(key)) {
            try { rules.add(new FlowChatRule(elem.getAsJsonObject())); }
            catch (Exception e) { LOGGER.warn("Skipping malformed rule in {}: {}", key, e.getMessage()); }
        }
        return rules;
    }

    public List<FlowChatRule> getIncomingRules() { return incomingRules; }
    public List<FlowChatRule> getOutgoingRules() { return outgoingRules; }
    public JsonObject getAntiAfk() { return antiAfk; }
    public JsonObject getVoidFall() { return voidFall; }
    public List<JsonObject> getOnJoinServer() { return onJoinServer; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Path getConfigPath() { return configPath; }
    public TagSettings getTagSettings() { return tagSettings; }
}
