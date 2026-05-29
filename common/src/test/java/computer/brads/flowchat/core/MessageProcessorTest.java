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
}
