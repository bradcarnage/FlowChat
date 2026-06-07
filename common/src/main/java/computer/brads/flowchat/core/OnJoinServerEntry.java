package computer.brads.flowchat.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a single onJoinServer config entry.
 * Runs a set of commands when the player joins a server matching the optional server regex.
 */
public class OnJoinServerEntry {
    private final List<String> commands;
    private final String serverFilter;
    private final Pattern serverPattern;
    private final int delay; // seconds
    private final String description;

    public OnJoinServerEntry(JsonObject json) {
        List<String> cmds = new ArrayList<String>();
        if (json.has("commands") && json.get("commands").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("commands");
            for (JsonElement e : arr) {
                cmds.add(e.getAsString());
            }
        }
        this.commands = Collections.unmodifiableList(cmds);

        this.serverFilter = json.has("server") ? json.get("server").getAsString() : null;
        this.serverPattern = serverFilter != null ? Pattern.compile(serverFilter) : null;

        this.delay = json.has("delay") ? json.get("delay").getAsInt() : 0;

        this.description = json.has("description") ? json.get("description").getAsString() : null;
    }

    public List<String> getCommands() { return commands; }
    public String getServerFilter() { return serverFilter; }
    public int getDelay() { return delay; }
    public String getDescription() { return description; }

    /**
     * Check if this entry matches the given server IP/address.
     */
    public boolean matchesServer(String serverIp) {
        if (serverPattern == null) return true;
        if (serverIp == null) return false;
        return serverPattern.matcher(serverIp).find();
    }
}
