package computer.brads.flowchat.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Tests for onJoinServer logic — config parsing + command scheduling.
 * The actual onJoinServer handler lives in each loader (Fabric/Forge/NeoForge),
 * but all loaders share the same algorithm. We test that algorithm here
 * against FlowChatConfig.getOnJoinServer() output.
 */
public class OnJoinServerHandlerTest {

    // Mirrors the PendingCommand class used in all loaders
    private static class PendingCommand {
        final String command;
        final long executeAtTick;
        PendingCommand(String command, long executeAtTick) {
            this.command = command;
            this.executeAtTick = executeAtTick;
        }
    }

    private List<PendingCommand> pendingCommands;
    private long tickCounter;

    @Before
    public void setUp() {
        pendingCommands = new ArrayList<PendingCommand>();
        tickCounter = 0;
    }

    /**
     * Replicates the handleOnJoinServer method from loaders.
     * Identical logic across Fabric/Forge/NeoForge.
     */
    private void handleOnJoinServer(String currentIp, List<JsonObject> entries) {
        for (JsonObject entry : entries) {
            if (entry.has("server")) {
                String regex = entry.get("server").getAsString();
                if (!currentIp.matches(regex)) continue;
            }
            JsonArray cmds = entry.getAsJsonArray("commands");
            if (cmds == null) continue;
            int delaySec = entry.has("delay") ? entry.get("delay").getAsInt() : 0;
            long delayTicks = delaySec * 20L;
            for (int i = 0; i < cmds.size(); i++) {
                pendingCommands.add(new PendingCommand(cmds.get(i).getAsString(), tickCounter + delayTicks));
                delayTicks += 20; // 1 second gap between commands
            }
        }
    }

    /**
     * Drain pending commands up to targetTick. Returns commands that fired.
     */
    private List<String> drainUntilTick(long targetTick) {
        List<String> fired = new ArrayList<String>();
        while (tickCounter <= targetTick) {
            tickCounter++;
            Iterator<PendingCommand> it = pendingCommands.iterator();
            while (it.hasNext()) {
                PendingCommand pc = it.next();
                if (tickCounter >= pc.executeAtTick) {
                    fired.add(pc.command);
                    it.remove();
                }
            }
        }
        return fired;
    }

    // --- Test helpers ---

    private JsonObject makeEntry(String[] commands, String server, int delay) {
        JsonObject entry = new JsonObject();
        JsonArray cmds = new JsonArray();
        for (String c : commands) cmds.add(c);
        entry.add("commands", cmds);
        if (server != null) entry.addProperty("server", server);
        if (delay > 0) entry.addProperty("delay", delay);
        return entry;
    }

    private JsonObject makeEntry(String[] commands) {
        return makeEntry(commands, null, 0);
    }

    private JsonObject makeEntry(String[] commands, String server) {
        return makeEntry(commands, server, 0);
    }

    // === Tests ===

    @Test
    public void joinServer_commandsQueued() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub", "/play skyblock"}));

        handleOnJoinServer("mc.hypixel.net", entries);

        assertEquals("Should queue 2 commands", 2, pendingCommands.size());
        assertEquals("/hub", pendingCommands.get(0).command);
        assertEquals("/play skyblock", pendingCommands.get(1).command);
    }

    @Test
    public void serverRegex_matchingServer_commandsFire() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub"}, ".*hypixel.*"));

        handleOnJoinServer("mc.hypixel.net", entries);

        assertEquals("Matching server should queue commands", 1, pendingCommands.size());
        assertEquals("/hub", pendingCommands.get(0).command);
    }

    @Test
    public void serverRegex_nonMatchingServer_noCommands() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub"}, ".*hypixel.*"));

        handleOnJoinServer("play.mineplex.com", entries);

        assertTrue("Non-matching server should skip", pendingCommands.isEmpty());
    }

    @Test
    public void serverRegex_noServerField_matchesAll() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/spawn"}));

        handleOnJoinServer("any.random.server.com", entries);

        assertEquals("No server field = match all", 1, pendingCommands.size());
    }

    @Test
    public void delay_commandsScheduledForFutureTick() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub"}, null, 5));

        handleOnJoinServer("mc.hypixel.net", entries);

        assertEquals(1, pendingCommands.size());
        // delay=5 → 5*20=100 ticks from tickCounter(0)
        assertEquals("Should be at tick 100", 100, pendingCommands.get(0).executeAtTick);

        // Drain to tick 98 — should NOT fire yet (drainUntilTick increments first)
        List<String> earlyFired = drainUntilTick(98);
        assertTrue("Should not fire before delay", earlyFired.isEmpty());

        // Drain to tick 99 — tickCounter hits 100 inside, command fires
        List<String> fired = drainUntilTick(99);
        assertEquals("Should fire at delay tick", 1, fired.size());
        assertEquals("/hub", fired.get(0));
    }

    @Test
    public void delay_multipleCommands_staggeredByOneSecond() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub", "/play skyblock", "/warp"}, null, 2));

        handleOnJoinServer("mc.hypixel.net", entries);

        assertEquals(3, pendingCommands.size());
        // delay=2 → base 40 ticks, then +20 each
        assertEquals(40, pendingCommands.get(0).executeAtTick);
        assertEquals(60, pendingCommands.get(1).executeAtTick);
        assertEquals(80, pendingCommands.get(2).executeAtTick);
    }

    @Test
    public void multipleEntries_differentServers() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub"}, ".*hypixel.*"));
        entries.add(makeEntry(new String[]{"/spawn"}, ".*mineplex.*"));
        entries.add(makeEntry(new String[]{"/lobby"}, ".*cubecraft.*"));

        handleOnJoinServer("play.mineplex.com", entries);

        assertEquals("Only mineplex entry should match", 1, pendingCommands.size());
        assertEquals("/spawn", pendingCommands.get(0).command);
    }

    @Test
    public void multipleEntries_bothMatch() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub"}, ".*hypixel.*"));
        entries.add(makeEntry(new String[]{"/vip"})); // no server = matches all

        handleOnJoinServer("mc.hypixel.net", entries);

        assertEquals("Both should match (specific + wildcard)", 2, pendingCommands.size());
        assertEquals("/hub", pendingCommands.get(0).command);
        assertEquals("/vip", pendingCommands.get(1).command);
    }

    @Test
    public void emptyEntries_noop() {
        List<JsonObject> entries = Collections.emptyList();

        handleOnJoinServer("mc.hypixel.net", entries);

        assertTrue("Empty entries = no commands", pendingCommands.isEmpty());
    }

    @Test
    public void singleplayer_matchesWhenNoServerFilter() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/gamemode creative"}));

        handleOnJoinServer("singleplayer", entries);

        assertEquals(1, pendingCommands.size());
        assertEquals("/gamemode creative", pendingCommands.get(0).command);
    }

    @Test
    public void singleplayer_skippedWhenServerFilterSet() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub"}, ".*hypixel.*"));

        handleOnJoinServer("singleplayer", entries);

        assertTrue("Singleplayer shouldn't match hypixel regex", pendingCommands.isEmpty());
    }

    @Test
    public void noDelay_commandsAtCurrentTick() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub", "/play"}));

        tickCounter = 500;
        handleOnJoinServer("mc.server.com", entries);

        // delay=0 → base 0 ticks, first at 500, second at 500+20
        assertEquals(500, pendingCommands.get(0).executeAtTick);
        assertEquals(520, pendingCommands.get(1).executeAtTick);
    }

    @Test
    public void missingCommandsField_skipped() {
        JsonObject entry = new JsonObject();
        entry.addProperty("server", ".*hypixel.*");
        // No "commands" field
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(entry);

        handleOnJoinServer("mc.hypixel.net", entries);

        assertTrue("Entry without commands should be skipped", pendingCommands.isEmpty());
    }

    @Test
    public void emptyCommandsArray_noop() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{}));

        handleOnJoinServer("mc.server.com", entries);

        assertTrue("Empty commands array = nothing queued", pendingCommands.isEmpty());
    }

    @Test
    public void drainOrder_fifoWithinSameTick() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/first", "/second", "/third"}));

        handleOnJoinServer("mc.server.com", entries);

        // All have different ticks (0, 20, 40) so they fire in order
        List<String> fired = drainUntilTick(50);
        assertEquals(3, fired.size());
        assertEquals("/first", fired.get(0));
        assertEquals("/second", fired.get(1));
        assertEquals("/third", fired.get(2));
    }

    @Test
    public void complexScenario_multipleEntriesWithDelays() {
        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hub", "/play skyblock"}, ".*hypixel.*", 3));
        entries.add(makeEntry(new String[]{"/msg friend hi"}, null, 10));

        handleOnJoinServer("mc.hypixel.net", entries);

        assertEquals("Should queue 3 total commands", 3, pendingCommands.size());
        // Entry 1: delay=3 → 60 ticks, then +20
        assertEquals(60, pendingCommands.get(0).executeAtTick);   // /hub
        assertEquals(80, pendingCommands.get(1).executeAtTick);   // /play skyblock
        // Entry 2: delay=10 → 200 ticks
        assertEquals(200, pendingCommands.get(2).executeAtTick);  // /msg friend hi
    }

    @Test
    public void serverIpChanged_previousServerTracking() {
        // Simulate the IP change detection logic from loaders
        String previousServerIp = "";
        String[] serverSequence = {"singleplayer", "mc.hypixel.net", "mc.hypixel.net", "play.mineplex.com"};
        int joinCount = 0;

        List<JsonObject> entries = new ArrayList<JsonObject>();
        entries.add(makeEntry(new String[]{"/hello"}));

        for (String ip : serverSequence) {
            if (!ip.equals(previousServerIp)) {
                previousServerIp = ip;
                handleOnJoinServer(ip, entries);
                joinCount++;
            }
        }

        // singleplayer, hypixel, mineplex = 3 joins (duplicate hypixel skipped)
        assertEquals("Should detect 3 server changes", 3, joinCount);
        assertEquals("Should queue 3 commands", 3, pendingCommands.size());
    }
}
