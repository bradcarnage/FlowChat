package computer.brads.flowchat.core;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for FlowChatConfig: loading, parsing, default creation, malformed input handling.
 */
public class FlowChatConfigTest {

    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("flowchat-test");
    }

    @After
    public void tearDown() throws IOException {
        // Clean up temp files
        Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (IOException e) { /* ignore */ } });
    }

    private void writeConfig(String json) throws IOException {
        Files.writeString(tempDir.resolve("flowchat.json"), json);
    }

    // --- Default creation ---

    @Test
    public void createDefault_whenNoFile() {
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertTrue(Files.exists(tempDir.resolve("flowchat.json")));
        assertTrue(config.getIncomingRules().isEmpty());
        assertTrue(config.getOutgoingRules().isEmpty());
    }

    @Test
    public void createDefault_createsParentDirs() throws IOException {
        Path nested = tempDir.resolve("a/b/c");
        FlowChatConfig config = new FlowChatConfig(nested);
        assertTrue(config.load());
        assertTrue(Files.exists(nested.resolve("flowchat.json")));
    }

    // --- Basic parsing ---

    @Test
    public void parsesIncomingAndOutgoing() throws IOException {
        writeConfig("{\"incoming\": [{\"pattern\": \"hello\"}], \"outgoing\": [{\"pattern\": \"bye\"}, {\"pattern\": \"later\"}]}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertEquals(1, config.getIncomingRules().size());
        assertEquals(2, config.getOutgoingRules().size());
        assertEquals("hello", config.getIncomingRules().get(0).search);
        assertEquals("bye", config.getOutgoingRules().get(0).search);
    }

    @Test
    public void parsesAntiAfk() throws IOException {
        writeConfig("{\"incoming\": [], \"outgoing\": [], \"antiAFK\": {\"enabled\": true, \"interval\": 30}}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        JsonObject afk = config.getAntiAfk();
        assertNotNull(afk);
        assertTrue(afk.get("enabled").getAsBoolean());
        assertEquals(30, afk.get("interval").getAsInt());
    }

    @Test
    public void parsesVoidFall() throws IOException {
        writeConfig("{\"incoming\": [], \"outgoing\": [], \"voidFall\": {\"enabled\": false}}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        JsonObject vf = config.getVoidFall();
        assertNotNull(vf);
        assertFalse(vf.get("enabled").getAsBoolean());
    }

    @Test
    public void parsesTagSettings() throws IOException {
        writeConfig("{\"incoming\": [], \"outgoing\": [], \"tagSettings\": {\"durabilityFormat\": \"percent\", \"coordinateFormat\": \"x, y, z\"}}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        TagSettings ts = config.getTagSettings();
        assertEquals("percent", ts.durabilityFormat);
        assertEquals("x, y, z", ts.coordinateFormat);
    }

    @Test
    public void tagSettingsDefaultsWhenMissing() throws IOException {
        writeConfig("{\"incoming\": [], \"outgoing\": []}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertSame(TagSettings.DEFAULT, config.getTagSettings());
    }

    // --- Missing keys ---

    @Test
    public void missingIncomingKey() throws IOException {
        writeConfig("{\"outgoing\": [{\"pattern\": \"test\"}]}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertTrue(config.getIncomingRules().isEmpty());
        assertEquals(1, config.getOutgoingRules().size());
    }

    @Test
    public void missingOutgoingKey() throws IOException {
        writeConfig("{\"incoming\": [{\"pattern\": \"test\"}]}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertEquals(1, config.getIncomingRules().size());
        assertTrue(config.getOutgoingRules().isEmpty());
    }

    @Test
    public void emptyJsonObject() throws IOException {
        writeConfig("{}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertTrue(config.getIncomingRules().isEmpty());
        assertTrue(config.getOutgoingRules().isEmpty());
        assertNull(config.getAntiAfk());
        assertNull(config.getVoidFall());
    }

    // --- Malformed input ---

    @Test
    public void malformedJson_returnsFalse() throws IOException {
        writeConfig("{ this is not valid json");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertFalse(config.load());
    }

    @Test
    public void malformedRule_skippedGracefully() throws IOException {
        // Second rule has non-object element, third is valid
        writeConfig("{\"incoming\": [{\"pattern\": \"good\"}, \"not-an-object\", {\"pattern\": \"also-good\"}], \"outgoing\": []}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertEquals(2, config.getIncomingRules().size());
        assertEquals("good", config.getIncomingRules().get(0).search);
        assertEquals("also-good", config.getIncomingRules().get(1).search);
    }

    @Test
    public void emptyArrays() throws IOException {
        writeConfig("{\"incoming\": [], \"outgoing\": []}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertTrue(config.getIncomingRules().isEmpty());
        assertTrue(config.getOutgoingRules().isEmpty());
    }

    // --- Disabled toggle ---

    @Test
    public void disabledDefault() {
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertFalse(config.isDisabled());
    }

    @Test
    public void disabledToggle() {
        FlowChatConfig config = new FlowChatConfig(tempDir);
        config.setDisabled(true);
        assertTrue(config.isDisabled());
        config.setDisabled(false);
        assertFalse(config.isDisabled());
    }

    // --- Config path ---

    @Test
    public void configPathResolvesCorrectly() {
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertEquals(tempDir.resolve("flowchat.json"), config.getConfigPath());
    }

    // --- Reload behavior ---

    @Test
    public void reloadPicksUpChanges() throws IOException {
        writeConfig("{\"incoming\": [{\"pattern\": \"first\"}], \"outgoing\": []}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        assertEquals(1, config.getIncomingRules().size());
        assertEquals("first", config.getIncomingRules().get(0).search);

        // Overwrite and reload
        writeConfig("{\"incoming\": [{\"pattern\": \"second\"}, {\"pattern\": \"third\"}], \"outgoing\": []}");
        assertTrue(config.load());
        assertEquals(2, config.getIncomingRules().size());
        assertEquals("second", config.getIncomingRules().get(0).search);
    }

    // --- Full rule parsing through config ---

    @Test
    public void fullRuleWithAllFields() throws IOException {
        writeConfig("{\"incoming\": [{\"pattern\": \"hello (\\\\w+)\", \"replacement\": \"Hi $1!\", " +
                "\"server\": \".*hypixel.*\", \"toast\": true, \"notifyStyle\": \"advancement\", " +
                "\"sound\": \"bell\", \"respond\": [\"gg\", \"wp\"], \"colorAware\": true, \"matchJson\": false}], " +
                "\"outgoing\": []}");
        FlowChatConfig config = new FlowChatConfig(tempDir);
        assertTrue(config.load());
        FlowChatRule r = config.getIncomingRules().get(0);
        assertEquals("hello (\\w+)", r.search);
        assertEquals("Hi $1!", r.replacement);
        assertEquals(".*hypixel.*", r.serverSearch);
        assertTrue(r.toast);
        assertEquals("advancement", r.notifyStyle);
        assertTrue(r.playSound);
        assertEquals("minecraft:block.note_block.bell", r.soundId);
        assertTrue(r.respondMsg.isJsonArray());
        assertEquals(2, r.respondMsg.getAsJsonArray().size());
        assertTrue(r.colorAware);
        assertFalse(r.matchJson);
    }
}
