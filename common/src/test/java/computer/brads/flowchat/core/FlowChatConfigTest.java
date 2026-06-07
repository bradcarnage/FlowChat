package computer.brads.flowchat.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JUnit tests for FlowChatConfig, including onJoinServer parsing.
 */
public class FlowChatConfigTest {

    private Path tempDir;
    private FlowChatConfig config;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("flowchat-test");
    }

    @After
    public void tearDown() {
        // Clean up temp files
        try {
            File dir = tempDir.toFile();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            dir.delete();
        } catch (Exception ignored) {}
    }

    private void writeConfig(String json) throws IOException {
        Files.createDirectories(tempDir);
        Files.write(tempDir.resolve("flowchat.json"), json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testDefaultConfigCreation() {
        config = new FlowChatConfig(tempDir);
        config.load();
        assertNotNull("Config should exist after load", config);
        assertNotNull("onJoinServer should not be null", config.getOnJoinServer());
        assertTrue("Default onJoinServer should be empty", config.getOnJoinServer().isEmpty());
    }

    @Test
    public void testOnJoinServerParsing() throws IOException {
        writeConfig("{\n\"onJoinServer\": [\n{\n\"commands\": [\"/hub\", \"/play skyblock\"],\n\"server\": \".*hypixel.*\",\n\"delay\": 5,\n\"description\": \"Auto-join Hypixel Skyblock\"\n}\n]\n}");
        config = new FlowChatConfig(tempDir);
        config.load();

        assertEquals("Should have 1 onJoinServer entry", 1, config.getOnJoinServer().size());
        JsonObject entry = config.getOnJoinServer().get(0);
        assertEquals(".*hypixel.*", entry.get("server").getAsString());
        assertEquals(5, entry.get("delay").getAsInt());

        JsonArray cmds = entry.getAsJsonArray("commands");
        assertEquals(2, cmds.size());
        assertEquals("/hub", cmds.get(0).getAsString());
        assertEquals("/play skyblock", cmds.get(1).getAsString());
    }

    @Test
    public void testOnJoinServerNoServerFilter() throws IOException {
        writeConfig("{\n\"onJoinServer\": [\n{\n\"commands\": [\"/spawn\"]\n}\n]\n}");
        config = new FlowChatConfig(tempDir);
        config.load();

        assertEquals(1, config.getOnJoinServer().size());
        JsonObject entry = config.getOnJoinServer().get(0);
        assertFalse("Should not have server field", entry.has("server"));
        assertFalse("Should not have delay field", entry.has("delay"));
        assertEquals(1, entry.getAsJsonArray("commands").size());
    }

    @Test
    public void testOnJoinServerMultipleEntries() throws IOException {
        writeConfig("{\n\"onJoinServer\": [\n{ \"commands\": [\"/hub\"], \"server\": \".*hypixel.*\" },\n{ \"commands\": [\"/spawn\"], \"delay\": 3 },\n{ \"commands\": [\"/msg friend hi\"], \"server\": \".*mineplex.*\", \"delay\": 10 }\n]\n}");
        config = new FlowChatConfig(tempDir);
        config.load();

        assertEquals("Should have 3 entries", 3, config.getOnJoinServer().size());
        assertEquals(".*hypixel.*", config.getOnJoinServer().get(0).get("server").getAsString());
        assertEquals(3, config.getOnJoinServer().get(1).get("delay").getAsInt());
        assertEquals(".*mineplex.*", config.getOnJoinServer().get(2).get("server").getAsString());
    }

    @Test
    public void testOnJoinServerMalformedSkipped() throws IOException {
        writeConfig("{\n\"onJoinServer\": [\n{ \"commands\": [\"/valid\"] },\n\"not_an_object\",\n{ \"commands\": [\"/also_valid\"] }\n]\n}");
        config = new FlowChatConfig(tempDir);
        config.load();

        // Malformed entry (string instead of object) should be skipped
        assertEquals("Should have 2 valid entries (malformed skipped)", 2, config.getOnJoinServer().size());
    }

    @Test
    public void testOnJoinServerEmptyArray() throws IOException {
        writeConfig("{\n\"onJoinServer\": []\n}");
        config = new FlowChatConfig(tempDir);
        config.load();

        assertNotNull(config.getOnJoinServer());
        assertTrue("Empty array should produce empty list", config.getOnJoinServer().isEmpty());
    }

    @Test
    public void testOnJoinServerCoexistsWithOtherRules() throws IOException {
        writeConfig("{\n\"antiAFK\": {\n\"afterSeconds\": 250,\n\"command\": \"/ping\"\n},\n\"incoming\": [\n{ \"search\": \"hello\", \"replacement\": \"world\" }\n],\n\"onJoinServer\": [\n{ \"commands\": [\"/hub\"] }\n]\n}");
        config = new FlowChatConfig(tempDir);
        config.load();

        assertNotNull("antiAFK should parse", config.getAntiAfk());
        assertFalse("incoming should have entries", config.getIncomingRules().isEmpty());
        assertEquals("onJoinServer should have 1 entry", 1, config.getOnJoinServer().size());
    }
}
