package computer.brads.flowchat.core;

import com.google.gson.JsonObject;

/**
 * Parsed tagSettings from config. Controls tag resolution behavior.
 */
public class TagSettings {
    public static final TagSettings DEFAULT = new TagSettings();

    public final String multiPlayerSeparator;
    public final String durabilityFormat;   // "current/max", "current", "percent", "max"
    public final String coordinateFormat;   // "x y z", "x, y, z", "x y z [dim]"
    public final String unresolvedBehavior; // "cancel", "passthrough", "fallback", "strip"
    public final String unresolvedFallback;

    private TagSettings() {
        this.multiPlayerSeparator = ", ";
        this.durabilityFormat = "current/max";
        this.coordinateFormat = "x y z";
        this.unresolvedBehavior = "cancel";
        this.unresolvedFallback = "";
    }

    public TagSettings(JsonObject json) {
        this.multiPlayerSeparator = getString(json, "multiPlayerSeparator", ", ");
        this.durabilityFormat = getString(json, "durabilityFormat", "current/max");
        this.coordinateFormat = getString(json, "coordinateFormat", "x y z");
        this.unresolvedBehavior = getString(json, "unresolvedBehavior", "cancel");
        this.unresolvedFallback = getString(json, "unresolvedFallback", "");
    }

    private static String getString(JsonObject json, String key, String def) {
        if (json != null && json.has(key)) {
            try { return json.get(key).getAsString(); }
            catch (Exception e) { /* fall through */ }
        }
        return def;
    }
}
