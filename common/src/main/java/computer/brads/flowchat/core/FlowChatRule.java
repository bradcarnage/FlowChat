package computer.brads.flowchat.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.regex.Pattern;

public class FlowChatRule {
    public final String search;
    public final String replacement;
    public final Pattern pattern;
    public final String serverSearch;
    public final boolean toastMe;
    public final boolean playSound;
    public final String soundName;
    public final boolean localOnly;
    public final JsonElement respondMsg;
    public final JsonObject valueStack;

    public FlowChatRule(JsonObject json) {
        this.search = json.has("search") ? json.get("search").getAsString()
                : json.has("msgsearch") ? json.get("msgsearch").getAsString() : "";
        this.replacement = json.has("replacement") ? json.get("replacement").getAsString()
                : json.has("msgreplacement") ? json.get("msgreplacement").getAsString() : "$0";
        this.pattern = Pattern.compile(this.search);
        this.serverSearch = json.has("serversearch") ? json.get("serversearch").getAsString() : null;
        this.toastMe = json.has("toastMe") && json.get("toastMe").getAsBoolean();
        this.playSound = json.has("playSound") && json.get("playSound").getAsBoolean();
        this.soundName = json.has("soundName") ? json.get("soundName").getAsString() : null;
        this.localOnly = json.has("localOnly") && json.get("localOnly").getAsBoolean();
        this.respondMsg = json.has("respondMsg") ? json.get("respondMsg") : null;
        this.valueStack = json.has("valuestack") ? json.get("valuestack").getAsJsonObject() : null;
    }

    public boolean matchesServer(String serverIp) {
        if (serverSearch == null) return true;
        try { return serverIp.matches(serverSearch); }
        catch (Exception e) { return false; }
    }
}
