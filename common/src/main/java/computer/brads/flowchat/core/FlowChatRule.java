package computer.brads.flowchat.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.regex.Pattern;

public class FlowChatRule {
    // Canonical fields
    public final String search;
    public final String replacement;
    public final Pattern pattern;
    public final String serverSearch;
    public final boolean toast;
    public final String notifyStyle; // "actionbar", "toast", "advancement"
    public final boolean playSound;
    public final String soundId; // Resolved via SoundResolver
    public final JsonElement respondMsg;
    public final JsonObject valueStack;

    // Feature #3: Color-aware regex — when true, don't strip §codes before matching
    public final boolean colorAware;

    // Feature #6: JSON component matching — when true, match against raw JSON text
    public final boolean matchJson;

    public FlowChatRule(JsonObject json) {
        // Pattern/search — canonical: "pattern", aliases: "search", "msgsearch"
        this.search = getString(json, "pattern", "search", "msgsearch");
        this.pattern = Pattern.compile(this.search.isEmpty() ? "(?!)" : this.search);

        // Replacement — canonical: "replacement", alias: "msgreplacement"
        String repl = getStringOrNull(json, "replacement", "msgreplacement");
        this.replacement = repl != null ? repl : "$0";

        // Server filter — canonical: "server", alias: "serversearch"
        this.serverSearch = getStringOrNull(json, "server", "serversearch");

        // Toast — canonical: "toast", alias: "toastMe"
        this.toast = getBool(json, "toast", "toastMe");

        // Notification style — canonical: "notifyStyle" (no aliases)
        // Values: "actionbar" (default), "toast" (system toast), "advancement" (achievement popup)
        this.notifyStyle = json.has("notifyStyle") ? json.get("notifyStyle").getAsString() : "actionbar";

        // Sound — unified field. Canonical: "sound", aliases: "soundName" (string), "playSound" (boolean)
        if (json.has("sound")) {
            JsonElement soundElem = json.get("sound");
            if (soundElem.isJsonPrimitive()) {
                if (soundElem.getAsJsonPrimitive().isBoolean()) {
                    this.playSound = soundElem.getAsBoolean();
                    this.soundId = this.playSound ? SoundResolver.resolve(null) : null;
                } else {
                    String soundStr = soundElem.getAsString();
                    this.playSound = SoundResolver.shouldPlay(soundStr);
                    this.soundId = SoundResolver.resolve(soundStr);
                }
            } else {
                this.playSound = false;
                this.soundId = null;
            }
        } else if (json.has("playSound") || json.has("soundName")) {
            this.playSound = json.has("playSound") && json.get("playSound").getAsBoolean();
            String legacyName = json.has("soundName") ? json.get("soundName").getAsString() : null;
            this.soundId = this.playSound ? SoundResolver.resolve(legacyName) : null;
        } else {
            this.playSound = false;
            this.soundId = null;
        }

        // Auto-response — canonical: "respond", alias: "respondMsg"
        if (json.has("respond")) {
            this.respondMsg = json.get("respond");
        } else if (json.has("respondMsg")) {
            this.respondMsg = json.get("respondMsg");
        } else {
            this.respondMsg = null;
        }

        // Value stacking — no aliases
        this.valueStack = json.has("valuestack") ? json.get("valuestack").getAsJsonObject() : null;

        // Feature #3: Color-aware regex
        this.colorAware = getBool(json, "colorAware");

        // Feature #6: JSON component matching
        this.matchJson = getBool(json, "matchJson");
    }

    public boolean matchesServer(String serverIp) {
        if (serverSearch == null) return true;
        try { return serverIp.matches(serverSearch); }
        catch (Exception e) { return false; }
    }

    // --- Field resolution helpers ---

    private static String getString(JsonObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key)) {
                try { return json.get(key).getAsString(); }
                catch (Exception e) { /* skip */ }
            }
        }
        return "";
    }

    private static String getStringOrNull(JsonObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key)) {
                try { return json.get(key).getAsString(); }
                catch (Exception e) { /* skip */ }
            }
        }
        return null;
    }

    private static boolean getBool(JsonObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key)) {
                try { return json.get(key).getAsBoolean(); }
                catch (Exception e) { /* skip */ }
            }
        }
        return false;
    }
}
