package computer.brads.flowchat.core;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class OnJoinServerEntryTest {

    private OnJoinServerEntry parse(String json) {
        return new OnJoinServerEntry(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    public void testBasicEntry() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"], \"description\": \"go to spawn\"}");
        assertEquals(1, entry.getCommands().size());
        assertEquals("/spawn", entry.getCommands().get(0));
        assertEquals("go to spawn", entry.getDescription());
        assertEquals(0, entry.getDelay());
        assertNull(entry.getServerFilter());
    }

    @Test
    public void testMultipleCommands() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\", \"/kit starter\", \"/msg admin hello\"]}");
        assertEquals(3, entry.getCommands().size());
        assertEquals("/spawn", entry.getCommands().get(0));
        assertEquals("/kit starter", entry.getCommands().get(1));
        assertEquals("/msg admin hello", entry.getCommands().get(2));
    }

    @Test
    public void testDelay() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"], \"delay\": 5}");
        assertEquals(5, entry.getDelay());
    }

    @Test
    public void testServerFilterMatch() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"], \"server\": \"hypixel\"}");
        assertTrue(entry.matchesServer("mc.hypixel.net"));
        assertFalse(entry.matchesServer("play.cubecraft.net"));
        assertEquals("hypixel", entry.getServerFilter());
    }

    @Test
    public void testServerFilterRegex() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"], \"server\": \".*\\\\.hypixel\\\\.net\"}");
        assertTrue(entry.matchesServer("mc.hypixel.net"));
        assertTrue(entry.matchesServer("play.hypixel.net"));
        assertFalse(entry.matchesServer("hypixel.com"));
    }

    @Test
    public void testNoServerFilterMatchesAll() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"]}");
        assertTrue(entry.matchesServer("any.server.com"));
        assertTrue(entry.matchesServer("singleplayer"));
    }

    @Test
    public void testNullServerIp() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"], \"server\": \"hypixel\"}");
        assertFalse(entry.matchesServer(null));
    }

    @Test
    public void testNoServerFilterNullIp() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"]}");
        assertTrue(entry.matchesServer(null)); // no filter = match all
    }

    @Test
    public void testEmptyCommands() {
        OnJoinServerEntry entry = parse("{\"commands\": []}");
        assertTrue(entry.getCommands().isEmpty());
    }

    @Test
    public void testMissingCommandsKey() {
        OnJoinServerEntry entry = parse("{}");
        assertTrue(entry.getCommands().isEmpty());
        assertEquals(0, entry.getDelay());
        assertNull(entry.getServerFilter());
        assertNull(entry.getDescription());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCommandsListIsImmutable() {
        OnJoinServerEntry entry = parse("{\"commands\": [\"/spawn\"]}");
        entry.getCommands().add("/hack"); // should throw
    }
}
