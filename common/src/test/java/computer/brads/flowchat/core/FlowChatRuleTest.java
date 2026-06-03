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

    // === Additional alias coverage ===

    @Test
    public void searchAliasOrder_patternFirst() {
        // pattern > search > msgsearch
        FlowChatRule r = makeRule("{\"pattern\": \"A\", \"search\": \"B\", \"msgsearch\": \"C\"}");
        assertEquals("A", r.search);
    }

    @Test
    public void searchAliasOrder_searchSecond() {
        FlowChatRule r = makeRule("{\"search\": \"B\", \"msgsearch\": \"C\"}");
        assertEquals("B", r.search);
    }

    @Test
    public void replacementCanonicalOverLegacy() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"replacement\": \"canon\", \"msgreplacement\": \"legacy\"}");
        assertEquals("canon", r.replacement);
    }

    @Test
    public void serverCanonicalOverLegacy() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"server\": \"canon\", \"serversearch\": \"legacy\"}");
        assertEquals("canon", r.serverSearch);
    }

    @Test
    public void toastCanonicalOverLegacy() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"toast\": false, \"toastMe\": true}");
        assertFalse(r.toast); // canonical wins
    }

    @Test
    public void respondCanonicalOverLegacy() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"respond\": \"canon\", \"respondMsg\": \"legacy\"}");
        assertEquals("canon", r.respondMsg.getAsString());
    }

    // === Sound edge cases ===

    @Test
    public void soundAsJsonObject_disablesSound() {
        // sound as non-primitive JSON object → playSound=false
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": {\"name\": \"bell\"}}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void soundAsJsonArray_disablesSound() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": [\"bell\"]}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void soundAsNull_disablesSound() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": null}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void legacySoundName_withoutPlaySound() {
        // soundName present but playSound missing → playSound defaults false
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"soundName\": \"bell\"}");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void legacyPlaySoundTrue_withoutSoundName() {
        // playSound=true, no soundName → default sound
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"playSound\": true}");
        assertTrue(r.playSound);
        assertEquals("minecraft:entity.experience_orb.pickup", r.soundId);
    }

    @Test
    public void soundFieldTakesPrecedenceOverLegacy() {
        // "sound" canonical should override playSound+soundName
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"sound\": \"bell\", \"playSound\": false, \"soundName\": \"click\"}");
        assertTrue(r.playSound);
        assertEquals("minecraft:block.note_block.bell", r.soundId);
    }

    // === matchesServer edge cases ===

    @Test
    public void matchesServer_emptyString() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"server\": \".*\"}");
        assertTrue(r.matchesServer(""));
    }

    @Test
    public void matchesServer_exactMatch() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"server\": \"mc.hypixel.net\"}");
        assertTrue(r.matchesServer("mc.hypixel.net"));
        assertFalse(r.matchesServer("mc.hypixel.net:25565")); // no partial
    }

    @Test
    public void matchesServer_invalidRegex_returnsFalse() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"server\": \"[invalid\"}");
        assertFalse(r.matchesServer("anything"));
    }

    @Test
    public void matchesServer_caseMatters() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"server\": \".*Hypixel.*\"}");
        assertFalse(r.matchesServer("mc.hypixel.net")); // case mismatch
        assertTrue(r.matchesServer("mc.Hypixel.net"));
    }

    // === Empty/missing pattern ===

    @Test
    public void emptyPattern_neverMatches() {
        FlowChatRule r = makeRule("{\"pattern\": \"\"}");
        // Empty pattern compiles to (?!) which never matches
        assertFalse(r.pattern.matcher("anything").find());
        assertFalse(r.pattern.matcher("").find());
    }

    @Test
    public void noPatternField_defaultsEmpty() {
        FlowChatRule r = makeRule("{}");
        assertEquals("", r.search);
        // Pattern (?!) never matches
        assertFalse(r.pattern.matcher("test").find());
    }

    // === Unicode in patterns ===

    @Test
    public void unicodePattern() {
        FlowChatRule r = makeRule("{\"pattern\": \"こんにちは\", \"replacement\": \"hello\"}");
        assertEquals("こんにちは", r.search);
        assertTrue(r.pattern.matcher("こんにちは世界").find());
    }

    @Test
    public void emojiPattern() {
        FlowChatRule r = makeRule("{\"pattern\": \"\\\\u2764\", \"replacement\": \"love\"}");
        assertTrue(r.pattern.matcher("I ❤ you").find());
    }

    // === ValueStack parsing ===

    @Test
    public void valueStackNull_whenMissing() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\"}");
        assertNull(r.valueStack);
    }

    @Test
    public void valueStackWithIgnoreDiffs() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"valuestack\": {\"stack_values\": [1], \"expire_after\": 10, \"ignore_diffs\": [2]}}");
        assertNotNull(r.valueStack);
        assertTrue(r.valueStack.has("ignore_diffs"));
        assertEquals(1, r.valueStack.getAsJsonArray("ignore_diffs").size());
    }

    @Test
    public void valueStackWithSeparator() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"valuestack\": {\"stack_values\": [1], \"seperate_float_with\": \",\"}}");
        assertNotNull(r.valueStack);
        assertEquals(",", r.valueStack.get("seperate_float_with").getAsString());
    }

    // === respondMsg variants ===

    @Test
    public void respondMsgNull_whenMissing() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\"}");
        assertNull(r.respondMsg);
    }

    @Test
    public void respondMsgSingleString() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"respond\": \"hello\"}");
        assertTrue(r.respondMsg.isJsonPrimitive());
        assertEquals("hello", r.respondMsg.getAsString());
    }

    @Test
    public void respondMsgEmptyArray() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"respond\": []}");
        assertNotNull(r.respondMsg);
        assertTrue(r.respondMsg.isJsonArray());
        assertEquals(0, r.respondMsg.getAsJsonArray().size());
    }

    // === Defaults when fields absent ===

    @Test
    public void allDefaultsOnMinimalRule() {
        FlowChatRule r = makeRule("{\"pattern\": \"test\"}");
        assertEquals("test", r.search);
        assertEquals("$0", r.replacement);
        assertNull(r.serverSearch);
        assertFalse(r.toast);
        assertEquals("actionbar", r.notifyStyle);
        assertFalse(r.playSound);
        assertNull(r.soundId);
        assertNull(r.respondMsg);
        assertNull(r.valueStack);
        assertFalse(r.colorAware);
        assertFalse(r.matchJson);
    }

    // === Regex compilation ===

    @Test
    public void regexWithCaptureGroups() {
        FlowChatRule r = makeRule("{\"pattern\": \"<(\\\\w+)> (.*)\"}");
        assertTrue(r.pattern.matcher("<Steve> hello world").find());
    }

    @Test
    public void regexWithSpecialChars() {
        FlowChatRule r = makeRule("{\"pattern\": \"\\\\[Server\\\\] \\\\d+ players\"}");
        assertTrue(r.pattern.matcher("[Server] 42 players online").find());
    }

    @Test
    public void colorAwareFalseDefault() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"colorAware\": false}");
        assertFalse(r.colorAware);
    }

    @Test
    public void matchJsonFalseDefault() {
        FlowChatRule r = makeRule("{\"pattern\": \"x\", \"matchJson\": false}");
        assertFalse(r.matchJson);
    }
}
