package computer.brads.flowchat.core;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OnJoinServerConfigTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FlowChatConfig loadConfig(String json) throws Exception {
        File configDir = tempFolder.newFolder("config");
        Path configFile = configDir.toPath().resolve("flowchat.json");
        Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
        FlowChatConfig config = new FlowChatConfig(configDir.toPath());
        assertTrue("Config should load successfully", config.load());
        return config;
    }

    @Test
    public void testOnJoinServerParsing() throws Exception {
        String json = "{\n" +
            "  \"incoming\": [],\n" +
            "  \"outgoing\": [],\n" +
            "  \"onJoinServer\": [\n" +
            "    {\n" +
            "      \"commands\": [\"/spawn\"],\n" +
            "      \"delay\": 3,\n" +
            "      \"description\": \"Go to spawn on join\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        FlowChatConfig config = loadConfig(json);
        List<OnJoinServerEntry> entries = config.getOnJoinServer();
        assertEquals(1, entries.size());
        assertEquals("/spawn", entries.get(0).getCommands().get(0));
        assertEquals(3, entries.get(0).getDelay());
        assertEquals("Go to spawn on join", entries.get(0).getDescription());
    }

    @Test
    public void testMultipleOnJoinEntries() throws Exception {
        String json = "{\n" +
            "  \"incoming\": [],\n" +
            "  \"outgoing\": [],\n" +
            "  \"onJoinServer\": [\n" +
            "    {\"commands\": [\"/spawn\"], \"description\": \"all servers\"},\n" +
            "    {\"commands\": [\"/kit pvp\"], \"server\": \"pvp\\\\.example\\\\.com\", \"delay\": 5}\n" +
            "  ]\n" +
            "}";
        FlowChatConfig config = loadConfig(json);
        List<OnJoinServerEntry> entries = config.getOnJoinServer();
        assertEquals(2, entries.size());

        // First entry matches all servers
        assertTrue(entries.get(0).matchesServer("anything"));
        assertEquals("/spawn", entries.get(0).getCommands().get(0));

        // Second entry only matches pvp server
        assertTrue(entries.get(1).matchesServer("pvp.example.com"));
        assertFalse(entries.get(1).matchesServer("creative.example.com"));
        assertEquals(5, entries.get(1).getDelay());
    }

    @Test
    public void testNoOnJoinServer() throws Exception {
        String json = "{\"incoming\": [], \"outgoing\": []}";
        FlowChatConfig config = loadConfig(json);
        assertTrue(config.getOnJoinServer().isEmpty());
    }

    @Test
    public void testEmptyOnJoinServer() throws Exception {
        String json = "{\"incoming\": [], \"outgoing\": [], \"onJoinServer\": []}";
        FlowChatConfig config = loadConfig(json);
        assertTrue(config.getOnJoinServer().isEmpty());
    }

    @Test
    public void testMalformedEntrySkipped() throws Exception {
        String json = "{\n" +
            "  \"incoming\": [],\n" +
            "  \"outgoing\": [],\n" +
            "  \"onJoinServer\": [\n" +
            "    {\"commands\": [\"/spawn\"]},\n" +
            "    \"not an object\",\n" +
            "    {\"commands\": [\"/home\"]}\n" +
            "  ]\n" +
            "}";
        FlowChatConfig config = loadConfig(json);
        // Malformed entry should be skipped, 2 valid ones remain
        assertEquals(2, config.getOnJoinServer().size());
    }

    @Test
    public void testOnJoinWithServerFilterAndCommands() throws Exception {
        String json = "{\n" +
            "  \"incoming\": [],\n" +
            "  \"outgoing\": [],\n" +
            "  \"onJoinServer\": [\n" +
            "    {\n" +
            "      \"commands\": [\"/spawn\", \"/kit starter\", \"/msg admin I'm here\"],\n" +
            "      \"server\": \"play\\\\.myserver\",\n" +
            "      \"delay\": 2,\n" +
            "      \"description\": \"Setup commands for myserver\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        FlowChatConfig config = loadConfig(json);
        OnJoinServerEntry entry = config.getOnJoinServer().get(0);
        assertEquals(3, entry.getCommands().size());
        assertEquals("/spawn", entry.getCommands().get(0));
        assertEquals("/kit starter", entry.getCommands().get(1));
        assertEquals("/msg admin I'm here", entry.getCommands().get(2));
        assertTrue(entry.matchesServer("play.myserver.com"));
        assertFalse(entry.matchesServer("other.server.com"));
    }
}
