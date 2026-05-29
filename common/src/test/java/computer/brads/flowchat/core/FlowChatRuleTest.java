package computer.brads.flowchat.core;

import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlowChatRuleTest {

    private FlowChatRule makeRule(String json) {
        return new FlowChatRule(JsonParser.parseString(json).getAsJsonObject());
    }

    // --- Field alias resolution ---

    @Test
    public void canonicalFieldNames() {
        FlowChatRule r = makeRule("{\"pattern\": \"test\", \"replacement\": \"x\", \"server\": \".*\", \"toast\": true, \"respond\": \"y\"}");
        assertEquals("test", r.search);
        assertEquals("x", r.replacement);
        assertEquals(".*", r.serverSearch);
        assertTrue(r.toast);
        assertNotNull(r.respondMsg);
    }

    @Test
    public void legacySearchFields() {
        FlowChatRule r1 = makeRule("{\"search\": \"a\"}");
        assertEquals("a", r1.search);

        FlowChatRule r2 = makeRule("{\"msgsearch\": \"b\"}");
        assertEquals("b", r2.search);
    }

    @Test
    public void canonicalTakesPrecedenceOverLegacy() {
        FlowChatRule r = makeRule("{\"pattern\": \"canonical\", \"search\": \"legacy\"}");
        assertEquals("canonical", r.search);
    }

    @Test
    public void legacyReplacementField() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"msgreplacement\": \"y\"}");
        assertEquals("y", r.replacement);
    }

    @Test
    public void defaultReplacement() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\"}");
        assertEquals("$0", r.replacement);
    }

    @Test
    public void legacyServerSearch() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"serversearch\": \".*test.*\"}");
        assertEquals(".*test.*", r.serverSearch);
    }

    @Test
    public void legacyToastMe() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"toastMe\": true}");
        assertTrue(r.toast);
    }

    @Test
    public void legacyRespondMsg() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"respondMsg\": \"hi\"}");
        assertNotNull(r.respondMsg);
        assertEquals("hi", r.respondMsg.getAsString());
    }

    // --- Sound field unification ---

    @Test
    public void soundAsString() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": \"bell\"}");
        assertTrue(r.playSound);
        assertEquals("minecraft:block.note_block.bell", r.soundId);
    }

    @Test
    public void soundAsTrue() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": true}");
        assertTrue(r.playSound);
        assertEquals("minecraft:entity.experience_orb.pickup", r.soundId);
    }

    @Test
    public void soundAsFalse() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": false}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void soundAsNone() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": \"none\"}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void soundAsArbitrary() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": \"minecraft:entity.pig.ambient\"}");
        assertTrue(r.playSound);
        assertEquals("minecraft:entity.pig.ambient", r.soundId);
    }

    @Test
    public void legacySoundFields() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"playSound\": true, \"soundName\": \"click\"}");
        assertTrue(r.playSound);
        assertEquals("minecraft:ui.button.click", r.soundId);
    }

    @Test
    public void legacyPlaySoundFalse() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"playSound\": false, \"soundName\": \"bell\"}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void noSoundFields() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\"}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    // --- notifyStyle ---

    @Test
    public void notifyStyleDefault() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"toast\": true}");
        assertEquals("actionbar", r.notifyStyle);
    }

    @Test
    public void notifyStyleExplicit() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"toast\": true, \"notifyStyle\": \"toast\"}");
        assertEquals("toast", r.notifyStyle);
    }

    // --- Server matching ---

    @Test
    public void matchesServerNull() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\"}");
        assertTrue(r.matchesServer("anything"));
    }

    @Test
    public void matchesServerMatch() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"server\": \".*hypixel.*\"}");
        assertTrue(r.matchesServer("mc.hypixel.net"));
    }

    @Test
    public void matchesServerNoMatch() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"server\": \".*hypixel.*\"}");
        assertFalse(r.matchesServer("play.cubecraft.net"));
    }

    // --- localOnly removed ---

    @Test
    public void localOnlyFieldIgnored() {
        // Should not crash, field is silently ignored
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"localOnly\": true}");
        assertEquals("x", r.search);
    }

    // --- Value stack ---

    @Test
    public void valueStackParsed() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"valuestack\": {\"stack_values\": [1], \"expire_after\": 5}}");
        assertNotNull(r.valueStack);
        assertTrue(r.valueStack.has("stack_values"));
    }

    // --- Respond array ---

    @Test
    public void respondArray() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"respond\": [\"a\", \"b\"]}");
        assertNotNull(r.respondMsg);
        assertTrue(r.respondMsg.isJsonArray());
        assertEquals(2, r.respondMsg.getAsJsonArray().size());
    }

    // --- Feature #3: colorAware ---

    @Test
    public void colorAwareDefault() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\"}");
        assertFalse(r.colorAware);
    }

    @Test
    public void colorAwareTrue() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"colorAware\": true}");
        assertTrue(r.colorAware);
    }

    // --- Feature #6: matchJson ---

    @Test
    public void matchJsonDefault() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\"}");
        assertFalse(r.matchJson);
    }

    @Test
    public void matchJsonTrue() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"matchJson\": true}");
        assertTrue(r.matchJson);
    }

    // --- Feature #9: advancement notifyStyle ---

    @Test
    public void notifyStyleAdvancement() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"toast\": true, \"notifyStyle\": \"advancement\"}");
        assertEquals("advancement", r.notifyStyle);
        assertTrue(r.toast);
    }
}
