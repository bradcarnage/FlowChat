package computer.brads.flowchat.core;

import com.google.gson.JsonParser;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MessageProcessorTest {

    private MessageProcessor processor;

    @Before
    public void setUp() {
        processor = new MessageProcessor();
    }

    private FlowChatRule makeRule(String json) {
        return new FlowChatRule(JsonParser.parseString(json).getAsJsonObject());
    }

    // --- Basic processing ---

    @Test
    public void simpleReplace() {
        FlowChatRule rule = makeRule("{\"pattern\": \"hello\", \"replacement\": \"world\"}");
        MessageProcessor.Result r = processor.process("hello there", Collections.singletonList(rule), "test");
        assertEquals("world there", r.processedText);
        assertTrue(r.wasModified());
    }

    @Test
    public void noMatch() {
        FlowChatRule rule = makeRule("{\"pattern\": \"xyz\", \"replacement\": \"abc\"}");
        MessageProcessor.Result r = processor.process("hello", Collections.singletonList(rule), "test");
        assertEquals("hello", r.processedText);
        assertFalse(r.wasModified());
    }

    @Test
    public void captureGroupBackref() {
        FlowChatRule rule = makeRule("{\"pattern\": \"<(\\\\w+)>\", \"replacement\": \"[$1]\"}");
        MessageProcessor.Result r = processor.process("<Steve> hi", Collections.singletonList(rule), "test");
        assertEquals("[Steve] hi", r.processedText);
    }

    @Test
    public void sectionSignStripping() {
        FlowChatRule rule = makeRule("{\"pattern\": \"Green Blue\", \"replacement\": \"MATCHED\"}");
        MessageProcessor.Result r = processor.process("\u00a7aGreen \u00a7bBlue", Collections.singletonList(rule), "test");
        assertEquals("MATCHED", r.processedText);
    }

    @Test
    public void multipleRulesCumulative() {
        FlowChatRule rule1 = makeRule("{\"pattern\": \"hello\", \"replacement\": \"hi\"}");
        FlowChatRule rule2 = makeRule("{\"pattern\": \"hi\", \"replacement\": \"hey\"}");
        MessageProcessor.Result r = processor.process("hello", Arrays.asList(rule1, rule2), "test");
        assertEquals("hey", r.processedText);
    }

    // --- Toast / notification ---

    @Test
    public void toastCancelsMessage() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"toast\": true}");
        MessageProcessor.Result r = processor.process("test msg", Collections.singletonList(rule), "test");
        assertTrue(r.toast);
        assertTrue(r.cancelled);
    }

    @Test
    public void notifyStylePropagates() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"toast\": true, \"notifyStyle\": \"toast\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "test");
        assertEquals("toast", r.notifyStyle);
    }

    @Test
    public void notifyStyleDefaultsToActionbar() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"toast\": true}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "test");
        assertEquals("actionbar", r.notifyStyle);
    }

    // --- Sound ---

    @Test
    public void soundPropagates() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"sound\": \"bell\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "test");
        assertTrue(r.playSound);
        assertEquals("minecraft:block.note_block.bell", r.soundId);
    }

    @Test
    public void firstSoundWins() {
        FlowChatRule rule1 = makeRule("{\"pattern\": \"test\", \"sound\": \"bell\"}");
        FlowChatRule rule2 = makeRule("{\"pattern\": \"test\", \"sound\": \"click\"}");
        MessageProcessor.Result r = processor.process("test", Arrays.asList(rule1, rule2), "test");
        assertEquals("minecraft:block.note_block.bell", r.soundId);
    }

    // --- Auto-response ---

    @Test
    public void autoResponseSingle() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"respond\": \"gg\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "test");
        assertEquals(1, r.autoResponses.size());
        assertEquals("gg", r.autoResponses.get(0));
    }

    @Test
    public void autoResponseArray() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"respond\": [\"msg1\", \"msg2\"]}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "test");
        assertEquals(2, r.autoResponses.size());
        assertEquals("msg1", r.autoResponses.get(0));
        assertEquals("msg2", r.autoResponses.get(1));
    }

    // --- Server filter ---

    @Test
    public void serverFilterMatch() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"X\", \"server\": \".*hypixel.*\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "mc.hypixel.net");
        assertTrue(r.wasModified());
    }

    @Test
    public void serverFilterNoMatch() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"X\", \"server\": \".*hypixel.*\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "play.cubecraft.net");
        assertFalse(r.wasModified());
    }

    @Test
    public void serverFilterNullMatchesAll() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"X\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "any.server.com");
        assertTrue(r.wasModified());
    }

    // --- Tag replacement ---

    @Test
    public void tagTime() {
        String result = MessageProcessor.replaceTags("{time}", "test", null, null);
        assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void tagServerIp() {
        String result = MessageProcessor.replaceTags("{serverip}", "mc.test.com", null, null);
        assertEquals("mc.test.com", result);
    }

    @Test
    public void tagUsername() {
        String result = MessageProcessor.replaceTags("Hi {username}", "test", "Steve", null);
        assertEquals("Hi Steve", result);
    }

    @Test
    public void tagServerName() {
        String result = MessageProcessor.replaceTags("On {servername}", "test", null, "Hypixel");
        assertEquals("On Hypixel", result);
    }

    @Test
    public void tagUsernamePassedToProcess() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"Hi {username}\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "srv", "Steve", "MyServer");
        assertEquals("Hi Steve", r.processedText);
    }

    @Test
    public void tagServerNamePassedToProcess() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"On {servername}\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "srv", null, "MyServer");
        assertEquals("On MyServer", r.processedText);
    }

    // --- Color formatting ---

    @Test
    public void formatColors() {
        assertEquals("\u00a7aGreen", MessageProcessor.formatColors("&aGreen"));
        assertEquals("\u00a7l\u00a7oBoldItalic", MessageProcessor.formatColors("&l&oBoldItalic"));
        assertEquals("No codes", MessageProcessor.formatColors("No codes"));
        assertNull(MessageProcessor.formatColors(null));
    }

    // --- Value stacking ---

    @Test
    public void valueStackAccumulates() {
        FlowChatRule rule = makeRule(
            "{\"pattern\": \"Earned (\\\\d+) coins\", \"replacement\": \"Total: $^1 coins (x$^i)\", " +
            "\"valuestack\": {\"stack_values\": [1], \"expire_after\": 10}}"
        );
        MessageProcessor proc = new MessageProcessor();
        proc.process("Earned 10 coins", Collections.singletonList(rule), "test");
        MessageProcessor.Result r2 = proc.process("Earned 5 coins", Collections.singletonList(rule), "test");
        // Should contain accumulated value (15)
        assertTrue(r2.processedText.contains("15"));
    }

    // --- Empty rules ---

    @Test
    public void emptyRuleList() {
        MessageProcessor.Result r = processor.process("hello", Collections.emptyList(), "test");
        assertEquals("hello", r.processedText);
        assertFalse(r.wasModified());
    }

    // === Feature #3: colorAware ===

    @Test
    public void colorAwareMatchesSectionSign() {
        FlowChatRule rule = makeRule("{\"pattern\": \"\u00a7a\\\\w+\", \"replacement\": \"FOUND\", \"colorAware\": true}");
        MessageProcessor.Result r = processor.process("\u00a7aGreen text", Collections.singletonList(rule), "test");
        assertTrue(r.wasModified());
        assertTrue(r.processedText.contains("FOUND"));
    }

    @Test
    public void colorAwareFalseStripsColors() {
        FlowChatRule rule = makeRule("{\"pattern\": \"\u00a7a\\\\w+\", \"replacement\": \"FOUND\"}");
        MessageProcessor.Result r = processor.process("\u00a7aGreen text", Collections.singletonList(rule), "test");
        assertFalse(r.wasModified()); // § stripped → pattern can't match
    }

    // === Feature #6: matchJson ===

    @Test
    public void matchJsonMatchesRawJson() {
        String rawJson = "{\"text\":\"Hello\",\"color\":\"red\"}";
        FlowChatRule rule = makeRule("{\"pattern\": \"\\\"color\\\":\\\"red\\\"\", \"replacement\": \"FOUND\", \"matchJson\": true}");
        MessageProcessor.Result r = processor.process("Hello", Collections.singletonList(rule), "test", null, null, rawJson);
        assertTrue(r.wasModified());
    }

    @Test
    public void matchJsonSkipsWhenNoJson() {
        FlowChatRule rule = makeRule("{\"pattern\": \"hello\", \"replacement\": \"X\", \"matchJson\": true}");
        MessageProcessor.Result r = processor.process("hello", Collections.singletonList(rule), "test", null, null, null);
        assertFalse(r.wasModified()); // matchJson=true but no JSON → skip
    }

    // === Feature #9: advancement notifyStyle ===

    @Test
    public void advancementNotifyStyle() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"toast\": true, \"notifyStyle\": \"advancement\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), "test");
        assertTrue(r.toast);
        assertTrue(r.cancelled);
        assertEquals("advancement", r.notifyStyle);
    }

    // === stripColors utility ===

    @Test
    public void stripColorsUtility() {
        assertEquals("Green Blue", MessageProcessor.stripColors("§aGreen §bBlue"));
        assertEquals("plain", MessageProcessor.stripColors("plain"));
        assertNull(MessageProcessor.stripColors(null));
    }

    // === Additional edge cases ===

    @Test
    public void replacementWithMultipleGroups() {
        FlowChatRule rule = makeRule("{\"pattern\": \"(\\\\w+) killed (\\\\w+)\", \"replacement\": \"$2 was slain by $1\"}");
        MessageProcessor.Result r = processor.process("Steve killed Alex", Collections.singletonList(rule), "test");
        assertEquals("Alex was slain by Steve", r.processedText);
    }

    @Test
    public void processNullServer() {
        FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"X\"}");
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule), null);
        assertTrue(r.wasModified());
    }

    @Test
    public void serverFilterSkipsNonMatching() {
        FlowChatRule rule1 = makeRule("{\"pattern\": \"test\", \"replacement\": \"A\", \"server\": \"hypixel\"}");
        FlowChatRule rule2 = makeRule("{\"pattern\": \"test\", \"replacement\": \"B\"}");
        MessageProcessor.Result r = processor.process("test", Arrays.asList(rule1, rule2), "cubecraft");
        assertEquals("B", r.processedText); // rule1 skipped, rule2 applied
    }

    @Test
    public void toastWithReplacement() {
        FlowChatRule rule = makeRule("{\"pattern\": \"(\\\\w+) joined\", \"toast\": true, \"replacement\": \"$1 is here!\"}");
        MessageProcessor.Result r = processor.process("Steve joined the game", Collections.singletonList(rule), "test");
        assertTrue(r.toast);
        assertTrue(r.cancelled);
        assertEquals("Steve is here! the game", r.processedText);
    }

    @Test
    public void multipleAutoResponses() {
        FlowChatRule rule1 = makeRule("{\"pattern\": \"test\", \"respond\": \"r1\"}");
        FlowChatRule rule2 = makeRule("{\"pattern\": \"test\", \"respond\": [\"r2\", \"r3\"]}");
        MessageProcessor.Result r = processor.process("test", Arrays.asList(rule1, rule2), "test");
        assertEquals(3, r.autoResponses.size());
    }

    @Test
    public void soundNoMatchDoesNotPlaySound() {
        FlowChatRule rule = makeRule("{\"pattern\": \"xyz\", \"sound\": \"bell\"}");
        MessageProcessor.Result r = processor.process("hello", Collections.singletonList(rule), "test");
        assertFalse(r.playSound);
        assertNull(r.soundId);
    }

    @Test
    public void emptyPattern_neverMatches() {
        FlowChatRule rule = makeRule("{\"pattern\": \"\", \"replacement\": \"X\"}");
        MessageProcessor.Result r = processor.process("anything", Collections.singletonList(rule), "test");
        assertFalse(r.wasModified());
    }

    @Test
    public void tagUnknown_leftAsIs() {
        String result = MessageProcessor.replaceTags("{nonexistenttag}", "test", null, null);
        assertEquals("{nonexistenttag}", result);
    }

    @Test
    public void tagMultipleInSameString() {
        String result = MessageProcessor.replaceTags("{serverip} on {servername}", "mc.test.com", null, "TestServer");
        assertEquals("mc.test.com on TestServer", result);
    }

    @Test
    public void formatColorsAllCodes() {
        assertEquals("§0Black", MessageProcessor.formatColors("&0Black"));
        assertEquals("§fWhite", MessageProcessor.formatColors("&fWhite"));
        assertEquals("§kObfusc", MessageProcessor.formatColors("&kObfusc"));
        assertEquals("§rReset", MessageProcessor.formatColors("&rReset"));
    }

    @Test
    public void formatColorsNoDoubleConvert() {
        // Already has § - ampersand code elsewhere
        assertEquals("§a§bBlue", MessageProcessor.formatColors("§a&bBlue"));
    }

    @Test
    public void colorAwarePreservesColorsInMatch() {
        // colorAware: pattern sees the §codes in the text
        FlowChatRule rule = makeRule("{\"pattern\": \"§aHello\", \"replacement\": \"MATCHED\", \"colorAware\": true}");
        MessageProcessor.Result r = processor.process("§aHello World", Collections.singletonList(rule), "test");
        assertTrue(r.wasModified());
        assertEquals("MATCHED World", r.processedText);
    }

    @Test
    public void matchJsonWithComplexJson() {
        String rawJson = "{\"text\":\"\",\"extra\":[{\"text\":\"Steve\",\"color\":\"gold\"},{\"text\":\" says hi\"}]}";
        FlowChatRule rule = makeRule("{\"pattern\": \"gold\", \"replacement\": \"GOLD_USER\", \"matchJson\": true}");
        MessageProcessor.Result r = processor.process("Steve says hi", Collections.singletonList(rule), "test", null, null, rawJson);
        assertTrue(r.wasModified());
    }

    @Test
    public void matchJsonFalseUsesPlainText() {
        String rawJson = "{\"text\":\"hidden\",\"color\":\"red\"}";
        FlowChatRule rule = makeRule("{\"pattern\": \"visible\", \"replacement\": \"X\", \"matchJson\": false}");
        MessageProcessor.Result r = processor.process("visible", Collections.singletonList(rule), "test", null, null, rawJson);
        assertTrue(r.wasModified());
    }

    @Test
    public void valueStackFirstProcessNoAccumulation() {
        FlowChatRule rule = makeRule(
            "{\"pattern\": \"Got (\\\\d+) gold\", \"replacement\": \"Total: $^1 gold (x$^i)\", " +
            "\"valuestack\": {\"stack_values\": [1], \"expire_after\": 10}}"
        );
        MessageProcessor proc = new MessageProcessor();
        MessageProcessor.Result r = proc.process("Got 50 gold", Collections.singletonList(rule), "test");
        assertTrue(r.processedText.contains("50"));
        assertTrue(r.processedText.contains("x1")); // first occurrence
    }

    @Test
    public void valueStackTripleAccumulation() {
        FlowChatRule rule = makeRule(
            "{\"pattern\": \"Earned (\\\\d+) coins\", \"replacement\": \"Total: $^1 coins (x$^i)\", " +
            "\"valuestack\": {\"stack_values\": [1], \"expire_after\": 10}}"
        );
        MessageProcessor proc = new MessageProcessor();
        proc.process("Earned 10 coins", Collections.singletonList(rule), "test");
        proc.process("Earned 20 coins", Collections.singletonList(rule), "test");
        MessageProcessor.Result r3 = proc.process("Earned 30 coins", Collections.singletonList(rule), "test");
        assertTrue(r3.processedText.contains("60")); // 10+20+30
        assertTrue(r3.processedText.contains("x3")); // third
    }

    @Test
    public void unicodeInMessage() {
        FlowChatRule rule = makeRule("{\"pattern\": \"こんにちは\", \"replacement\": \"Hello\"}");
        MessageProcessor.Result r = processor.process("こんにちは世界", Collections.singletonList(rule), "test");
        assertEquals("Hello世界", r.processedText);
    }

    @Test
    public void regexSpecialCharsInMessage() {
        FlowChatRule rule = makeRule("{\"pattern\": \"\\\\[Server\\\\]\", \"replacement\": \"[SRV]\"}");
        MessageProcessor.Result r = processor.process("[Server] Hello", Collections.singletonList(rule), "test");
        assertEquals("[SRV] Hello", r.processedText);
    }

    @Test
    public void tagInAutoResponse() {
        FlowChatRule rule = makeRule("{\"pattern\": \"ping\", \"respond\": \"pong from {serverip}\"}");
        MessageProcessor.Result r = processor.process("ping", Collections.singletonList(rule), "mc.test.com");
        assertEquals(1, r.autoResponses.size());
        assertEquals("pong from mc.test.com", r.autoResponses.get(0));
    }

    @Test
    public void resultFieldsDefaultCorrectly() {
        MessageProcessor.Result r = processor.process("test", Collections.emptyList(), "srv");
        assertFalse(r.toast);
        assertFalse(r.cancelled);
        assertFalse(r.playSound);
        assertNull(r.soundId);
        assertEquals("actionbar", r.notifyStyle); // default is actionbar
        assertTrue(r.autoResponses.isEmpty());
    }
}
