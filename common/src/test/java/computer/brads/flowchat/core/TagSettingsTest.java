package computer.brads.flowchat.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for TagSettings parsing and defaults.
 */
public class TagSettingsTest {

    private TagSettings parse(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return new TagSettings(obj);
    }

    // --- DEFAULT instance ---

    @Test
    public void defaultDurabilityFormat() {
        assertEquals("current/max", TagSettings.DEFAULT.durabilityFormat);
    }

    @Test
    public void defaultCoordinateFormat() {
        assertNotNull(TagSettings.DEFAULT.coordinateFormat);
    }

    @Test
    public void defaultMultiPlayerSeparator() {
        assertNotNull(TagSettings.DEFAULT.multiPlayerSeparator);
    }

    @Test
    public void defaultUnresolvedBehavior() {
        assertNotNull(TagSettings.DEFAULT.unresolvedBehavior);
    }

    // --- Custom parsing ---

    @Test
    public void customDurabilityFormat() {
        TagSettings ts = parse("{\"durabilityFormat\": \"percent\"}");
        assertEquals("percent", ts.durabilityFormat);
    }

    @Test
    public void customCoordinateFormat() {
        TagSettings ts = parse("{\"coordinateFormat\": \"(%d, %d, %d)\"}");
        assertEquals("(%d, %d, %d)", ts.coordinateFormat);
    }

    @Test
    public void customUnresolvedBehavior() {
        TagSettings ts = parse("{\"unresolvedBehavior\": \"cancel\"}");
        assertEquals("cancel", ts.unresolvedBehavior);
    }

    @Test
    public void customUnresolvedFallback() {
        TagSettings ts = parse("{\"unresolvedBehavior\": \"fallback\", \"unresolvedFallback\": \"N/A\"}");
        assertEquals("fallback", ts.unresolvedBehavior);
        assertEquals("N/A", ts.unresolvedFallback);
    }

    @Test
    public void missingFieldsFallToDefaults() {
        TagSettings ts = parse("{}");
        assertEquals(TagSettings.DEFAULT.durabilityFormat, ts.durabilityFormat);
        assertEquals(TagSettings.DEFAULT.coordinateFormat, ts.coordinateFormat);
        assertEquals(TagSettings.DEFAULT.unresolvedBehavior, ts.unresolvedBehavior);
    }

    @Test
    public void partialFieldsMixed() {
        TagSettings ts = parse("{\"durabilityFormat\": \"percent\"}");
        assertEquals("percent", ts.durabilityFormat);
        assertEquals(TagSettings.DEFAULT.coordinateFormat, ts.coordinateFormat);
    }

    @Test
    public void customMultiPlayerSeparator() {
        TagSettings ts = parse("{\"multiPlayerSeparator\": \" & \"}");
        assertEquals(" & ", ts.multiPlayerSeparator);
    }

    // --- Durability format values ---

    @Test
    public void durabilityFormatAbsolute() {
        TagSettings ts = parse("{\"durabilityFormat\": \"current\"}");
        assertEquals("current", ts.durabilityFormat);
    }

    @Test
    public void durabilityFormatMax() {
        TagSettings ts = parse("{\"durabilityFormat\": \"max\"}");
        assertEquals("max", ts.durabilityFormat);
    }

    // --- Unresolved behavior values ---

    @Test
    public void unresolvedPassthrough() {
        TagSettings ts = parse("{\"unresolvedBehavior\": \"passthrough\"}");
        assertEquals("passthrough", ts.unresolvedBehavior);
    }

    @Test
    public void unresolvedStrip() {
        TagSettings ts = parse("{\"unresolvedBehavior\": \"strip\"}");
        assertEquals("strip", ts.unresolvedBehavior);
    }
}
