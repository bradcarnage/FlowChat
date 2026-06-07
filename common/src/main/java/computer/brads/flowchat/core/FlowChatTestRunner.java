package computer.brads.flowchat.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

/**
 * Self-test runner for FlowChat. Runs in-process tests that validate
 * core logic without external dependencies. Platform code extends
 * with platform-specific tests (sound playback, toast rendering, etc.)
 */
public class FlowChatTestRunner {

    public static class TestResult {
        public final int number;
        public final String name;
        public final boolean passed;
        public final String error;

        public TestResult(int number, String name, boolean passed, String error) {
            this.number = number;
            this.name = name;
            this.passed = passed;
            this.error = error;
        }
    }

    /**
     * Run all common (pure-logic) tests.
     * @return list of test results
     */
    public static List<TestResult> runCommonTests() {
        List<TestResult> results = new ArrayList<>();
        int n = 0;

        // 1. Config parse
        n++;
        try {
            FlowChatConfig config = new FlowChatConfig(java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime()));
            boolean loaded = config.load();
            results.add(new TestResult(n, "Config parse (default creation)", loaded, loaded ? null : "load() returned false"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Config parse (default creation)", false, e.toString()));
        }

        // 2. Field aliases — canonical names
        n++;
        try {
            JsonObject json = JsonParser.parseString(
                "{\"pattern\": \"hello\", \"replacement\": \"world\", \"server\": \".*test.*\", " +
                "\"toast\": true, \"sound\": \"bell\", \"respond\": \"gg\", \"notifyStyle\": \"toast\"}"
            ).getAsJsonObject();
            FlowChatRule rule = new FlowChatRule(json);
            boolean ok = "hello".equals(rule.search) && "world".equals(rule.replacement) &&
                    ".*test.*".equals(rule.serverSearch) && rule.toast &&
                    "toast".equals(rule.notifyStyle) && rule.playSound &&
                    "minecraft:block.note_block.bell".equals(rule.soundId) &&
                    rule.respondMsg != null && "gg".equals(rule.respondMsg.getAsString());
            results.add(new TestResult(n, "Field aliases (canonical)", ok, ok ? null : "Field resolution mismatch"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Field aliases (canonical)", false, e.toString()));
        }

        // 3. Legacy field names
        n++;
        try {
            JsonObject json = JsonParser.parseString(
                "{\"msgsearch\": \"hi\", \"msgreplacement\": \"bye\", \"serversearch\": \".*legacy.*\", " +
                "\"toastMe\": true, \"playSound\": true, \"soundName\": \"click\", \"respondMsg\": \"ty\"}"
            ).getAsJsonObject();
            FlowChatRule rule = new FlowChatRule(json);
            boolean ok = "hi".equals(rule.search) && "bye".equals(rule.replacement) &&
                    ".*legacy.*".equals(rule.serverSearch) && rule.toast && rule.playSound &&
                    "minecraft:ui.button.click".equals(rule.soundId) &&
                    rule.respondMsg != null && "ty".equals(rule.respondMsg.getAsString());
            results.add(new TestResult(n, "Legacy field names", ok, ok ? null : "Legacy field resolution mismatch"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Legacy field names", false, e.toString()));
        }

        // 4. Text replacement
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"hello\", \"replacement\": \"world\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("hello there", Collections.singletonList(rule), "test");
            boolean ok = "world there".equals(r.processedText);
            results.add(new TestResult(n, "Text replacement", ok, ok ? null : "Got: " + r.processedText));
        } catch (Exception e) {
            results.add(new TestResult(n, "Text replacement", false, e.toString()));
        }

        // 5. Capture group backreference
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"<(\\\\w+)>\", \"replacement\": \"[$1]\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("<Steve> hi", Collections.singletonList(rule), "test");
            boolean ok = "[Steve] hi".equals(r.processedText);
            results.add(new TestResult(n, "Capture group backreference", ok, ok ? null : "Got: " + r.processedText));
        } catch (Exception e) {
            results.add(new TestResult(n, "Capture group backreference", false, e.toString()));
        }

        // 6. No match — no modification
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"xyz\", \"replacement\": \"abc\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("hello", Collections.singletonList(rule), "test");
            boolean ok = !r.wasModified() && "hello".equals(r.processedText);
            results.add(new TestResult(n, "No match — no modification", ok, ok ? null : "wasModified=" + r.wasModified()));
        } catch (Exception e) {
            results.add(new TestResult(n, "No match — no modification", false, e.toString()));
        }

        // 7. Section sign stripping (default behavior)
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"Green Blue\", \"replacement\": \"MATCHED\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("\u00a7aGreen \u00a7bBlue", Collections.singletonList(rule), "test");
            boolean ok = "MATCHED".equals(r.processedText);
            results.add(new TestResult(n, "Section sign stripping", ok, ok ? null : "Got: " + r.processedText));
        } catch (Exception e) {
            results.add(new TestResult(n, "Section sign stripping", false, e.toString()));
        }

        // 8. Toast flag → cancelled
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"toast\": true}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test msg", Collections.singletonList(rule), "test");
            boolean ok = r.cancelled && r.toast;
            results.add(new TestResult(n, "Toast flag → cancelled", ok, ok ? null : "cancelled=" + r.cancelled + " toast=" + r.toast));
        } catch (Exception e) {
            results.add(new TestResult(n, "Toast flag → cancelled", false, e.toString()));
        }

        // 9. Sound resolve (named)
        n++;
        try {
            String id = SoundResolver.resolve("bell");
            boolean ok = "minecraft:block.note_block.bell".equals(id);
            results.add(new TestResult(n, "Sound resolve (named: bell)", ok, ok ? null : "Got: " + id));
        } catch (Exception e) {
            results.add(new TestResult(n, "Sound resolve (named: bell)", false, e.toString()));
        }

        // 10. Sound resolve (arbitrary)
        n++;
        try {
            String id = SoundResolver.resolve("minecraft:entity.pig.ambient");
            boolean ok = "minecraft:entity.pig.ambient".equals(id);
            results.add(new TestResult(n, "Sound resolve (arbitrary)", ok, ok ? null : "Got: " + id));
        } catch (Exception e) {
            results.add(new TestResult(n, "Sound resolve (arbitrary)", false, e.toString()));
        }

        // 11. Sound resolve (null → default)
        n++;
        try {
            String id = SoundResolver.resolve(null);
            boolean ok = "minecraft:entity.experience_orb.pickup".equals(id);
            results.add(new TestResult(n, "Sound resolve (null → default)", ok, ok ? null : "Got: " + id));
        } catch (Exception e) {
            results.add(new TestResult(n, "Sound resolve (null → default)", false, e.toString()));
        }

        // 12. Sound resolve (none → silent)
        n++;
        try {
            String id = SoundResolver.resolve("none");
            boolean ok = id == null;
            results.add(new TestResult(n, "Sound resolve (none → null)", ok, ok ? null : "Got: " + id));
        } catch (Exception e) {
            results.add(new TestResult(n, "Sound resolve (none → null)", false, e.toString()));
        }

        // 13. Sound resolve (no namespace → auto-prefix)
        n++;
        try {
            String id = SoundResolver.resolve("entity.cow.ambient");
            boolean ok = "minecraft:entity.cow.ambient".equals(id);
            results.add(new TestResult(n, "Sound resolve (auto-prefix)", ok, ok ? null : "Got: " + id));
        } catch (Exception e) {
            results.add(new TestResult(n, "Sound resolve (auto-prefix)", false, e.toString()));
        }

        // 14. All 8 named aliases resolve
        n++;
        try {
            Map<String, String> expected = new LinkedHashMap<>();
            expected.put("ding", "minecraft:entity.experience_orb.pickup");
            expected.put("orb", "minecraft:entity.experience_orb.pickup");
            expected.put("levelup", "minecraft:entity.player.levelup");
            expected.put("level", "minecraft:entity.player.levelup");
            expected.put("anvil", "minecraft:block.anvil.land");
            expected.put("note", "minecraft:block.note_block.bell");
            expected.put("bell", "minecraft:block.note_block.bell");
            expected.put("click", "minecraft:ui.button.click");
            expected.put("pop", "minecraft:entity.item.pickup");
            StringBuilder failures = new StringBuilder();
            for (Map.Entry<String, String> e : expected.entrySet()) {
                String got = SoundResolver.resolve(e.getKey());
                if (!e.getValue().equals(got)) {
                    failures.append(e.getKey()).append("→").append(got).append(" ");
                }
            }
            boolean ok = failures.length() == 0;
            results.add(new TestResult(n, "All named sound aliases", ok, ok ? null : failures.toString()));
        } catch (Exception e) {
            results.add(new TestResult(n, "All named sound aliases", false, e.toString()));
        }

        // 15. Auto-response (single)
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"respond\": \"gg\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test", Collections.singletonList(rule), "test");
            boolean ok = r.autoResponses.size() == 1 && "gg".equals(r.autoResponses.get(0));
            results.add(new TestResult(n, "Auto-response (single)", ok, ok ? null : "Got: " + r.autoResponses));
        } catch (Exception e) {
            results.add(new TestResult(n, "Auto-response (single)", false, e.toString()));
        }

        // 16. Auto-response (array) — Feature #7 verification
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"respond\": [\"msg1\", \"msg2\"]}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test", Collections.singletonList(rule), "test");
            boolean ok = r.autoResponses.size() == 2 && "msg1".equals(r.autoResponses.get(0)) && "msg2".equals(r.autoResponses.get(1));
            results.add(new TestResult(n, "Auto-response (array)", ok, ok ? null : "Got: " + r.autoResponses));
        } catch (Exception e) {
            results.add(new TestResult(n, "Auto-response (array)", false, e.toString()));
        }

        // 17. Server filter — match
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"X\", \"server\": \".*hypixel.*\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test", Collections.singletonList(rule), "mc.hypixel.net");
            boolean ok = r.wasModified();
            results.add(new TestResult(n, "Server filter — match", ok, ok ? null : "Rule did not fire"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Server filter — match", false, e.toString()));
        }

        // 18. Server filter — no match
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"replacement\": \"X\", \"server\": \".*hypixel.*\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test", Collections.singletonList(rule), "play.cubecraft.net");
            boolean ok = !r.wasModified();
            results.add(new TestResult(n, "Server filter — no match", ok, ok ? null : "Rule fired when it shouldn't"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Server filter — no match", false, e.toString()));
        }

        // 19. Tag {time}
        n++;
        try {
            String result = MessageProcessor.replaceTags("Time is {time}", "test", null, null);
            boolean ok = result.matches("Time is \\d{2}:\\d{2}:\\d{2}");
            results.add(new TestResult(n, "Tag {time}", ok, ok ? null : "Got: " + result));
        } catch (Exception e) {
            results.add(new TestResult(n, "Tag {time}", false, e.toString()));
        }

        // 20. Tag {serverip}
        n++;
        try {
            String result = MessageProcessor.replaceTags("IP: {serverip}", "mc.test.com", null, null);
            boolean ok = "IP: mc.test.com".equals(result);
            results.add(new TestResult(n, "Tag {serverip}", ok, ok ? null : "Got: " + result));
        } catch (Exception e) {
            results.add(new TestResult(n, "Tag {serverip}", false, e.toString()));
        }

        // 21. Tag {username}
        n++;
        try {
            String result = MessageProcessor.replaceTags("Hi {username}", "test", "Steve", null);
            boolean ok = "Hi Steve".equals(result);
            results.add(new TestResult(n, "Tag {username}", ok, ok ? null : "Got: " + result));
        } catch (Exception e) {
            results.add(new TestResult(n, "Tag {username}", false, e.toString()));
        }

        // 22. Tag {servername}
        n++;
        try {
            String result = MessageProcessor.replaceTags("On {servername}", "test", null, "Hypixel");
            boolean ok = "On Hypixel".equals(result);
            results.add(new TestResult(n, "Tag {servername}", ok, ok ? null : "Got: " + result));
        } catch (Exception e) {
            results.add(new TestResult(n, "Tag {servername}", false, e.toString()));
        }

        // 23. Color formatting
        n++;
        try {
            String result = MessageProcessor.formatColors("&aGreen &bBlue &lBold");
            boolean ok = "\u00a7aGreen \u00a7bBlue \u00a7lBold".equals(result);
            results.add(new TestResult(n, "Color formatting (& → §)", ok, ok ? null : "Got: " + result));
        } catch (Exception e) {
            results.add(new TestResult(n, "Color formatting (& → §)", false, e.toString()));
        }

        // 24. Multiple rules — cumulative
        n++;
        try {
            FlowChatRule rule1 = makeRule("{\"pattern\": \"hello\", \"replacement\": \"hi\"}");
            FlowChatRule rule2 = makeRule("{\"pattern\": \"hi\", \"replacement\": \"hey\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("hello", Arrays.asList(rule1, rule2), "test");
            boolean ok = "hey".equals(r.processedText);
            results.add(new TestResult(n, "Multiple rules — cumulative", ok, ok ? null : "Got: " + r.processedText));
        } catch (Exception e) {
            results.add(new TestResult(n, "Multiple rules — cumulative", false, e.toString()));
        }

        // 25. First sound wins
        n++;
        try {
            FlowChatRule rule1 = makeRule("{\"pattern\": \"test\", \"sound\": \"bell\"}");
            FlowChatRule rule2 = makeRule("{\"pattern\": \"test\", \"sound\": \"click\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test", Arrays.asList(rule1, rule2), "test");
            boolean ok = r.playSound && "minecraft:block.note_block.bell".equals(r.soundId);
            results.add(new TestResult(n, "First sound wins", ok, ok ? null : "Got: " + r.soundId));
        } catch (Exception e) {
            results.add(new TestResult(n, "First sound wins", false, e.toString()));
        }

        // 26. notifyStyle propagation
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"toast\": true, \"notifyStyle\": \"toast\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test", Collections.singletonList(rule), "test");
            boolean ok = r.toast && "toast".equals(r.notifyStyle) && r.cancelled;
            results.add(new TestResult(n, "notifyStyle propagation", ok, ok ? null : "style=" + r.notifyStyle + " toast=" + r.toast));
        } catch (Exception e) {
            results.add(new TestResult(n, "notifyStyle propagation", false, e.toString()));
        }

        // 27. Disabled toggle
        n++;
        try {
            FlowChatConfig config = new FlowChatConfig(java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime()));
            config.load();
            config.setDisabled(true);
            boolean ok = config.isDisabled();
            config.setDisabled(false);
            ok = ok && !config.isDisabled();
            results.add(new TestResult(n, "Disabled toggle", ok, ok ? null : "Toggle failed"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Disabled toggle", false, e.toString()));
        }

        // === NEW FEATURE TESTS ===

        // 28. Feature #3: colorAware — regex matches § codes
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"\\u00a7aGreen\", \"replacement\": \"FOUND_COLOR\", \"colorAware\": true}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("\u00a7aGreen text", Collections.singletonList(rule), "test");
            boolean ok = r.wasModified() && r.processedText.contains("FOUND_COLOR");
            results.add(new TestResult(n, "Feature #3: colorAware regex", ok, ok ? null : "Got: " + r.processedText));
        } catch (Exception e) {
            results.add(new TestResult(n, "Feature #3: colorAware regex", false, e.toString()));
        }

        // 29. Feature #3: colorAware=false (default) — § stripped before match
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"\\u00a7aGreen\", \"replacement\": \"FOUND_COLOR\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("\u00a7aGreen text", Collections.singletonList(rule), "test");
            boolean ok = !r.wasModified(); // Should NOT match because § is stripped
            results.add(new TestResult(n, "Feature #3: default strips colors", ok, ok ? null : "Matched when it shouldn't"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Feature #3: default strips colors", false, e.toString()));
        }

        // 30. Feature #6: matchJson — matches against raw JSON
        n++;
        try {
            // Simulate a JSON component like {"text":"Hello","color":"red"}
            String rawJson = "{\"text\":\"Hello\",\"color\":\"red\"}";
            FlowChatRule rule = makeRule("{\"pattern\": \"\\\"color\\\":\\\"red\\\"\", \"replacement\": \"JSON_MATCHED\", \"matchJson\": true}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("Hello", Collections.singletonList(rule), "test", null, null, rawJson);
            boolean ok = r.wasModified() && r.processedText.contains("JSON_MATCHED");
            results.add(new TestResult(n, "Feature #6: matchJson", ok, ok ? null : "Got: " + r.processedText));
        } catch (Exception e) {
            results.add(new TestResult(n, "Feature #6: matchJson", false, e.toString()));
        }

        // 31. Feature #6: matchJson without rawJson falls back to plain text
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"hello\", \"replacement\": \"PLAIN_MATCH\", \"matchJson\": true}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("hello world", Collections.singletonList(rule), "test", null, null, null);
            boolean ok = !r.wasModified(); // matchJson=true but no JSON provided → no match
            results.add(new TestResult(n, "Feature #6: matchJson without JSON", ok, ok ? null : "Unexpectedly matched"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Feature #6: matchJson without JSON", false, e.toString()));
        }

        // 32. Feature #9: advancement notifyStyle
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"test\", \"toast\": true, \"notifyStyle\": \"advancement\"}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("test", Collections.singletonList(rule), "test");
            boolean ok = r.toast && "advancement".equals(r.notifyStyle) && r.cancelled;
            results.add(new TestResult(n, "Feature #9: advancement notifyStyle", ok, ok ? null : "style=" + r.notifyStyle));
        } catch (Exception e) {
            results.add(new TestResult(n, "Feature #9: advancement notifyStyle", false, e.toString()));
        }

        // 33. Feature #3: colorAware with replacement preserving colors
        n++;
        try {
            FlowChatRule rule = makeRule("{\"pattern\": \"\\u00a7a(\\\\w+)\", \"replacement\": \"\\u00a7b$1\", \"colorAware\": true}");
            MessageProcessor proc = new MessageProcessor();
            MessageProcessor.Result r = proc.process("\u00a7aGreen", Collections.singletonList(rule), "test");
            boolean ok = r.wasModified();
            results.add(new TestResult(n, "Feature #3: colorAware replacement", ok, ok ? null : "No match"));
        } catch (Exception e) {
            results.add(new TestResult(n, "Feature #3: colorAware replacement", false, e.toString()));
        }

        // 34. stripColors utility
        n++;
        try {
            String result = MessageProcessor.stripColors("\u00a7aGreen \u00a7bBlue");
            boolean ok = "Green Blue".equals(result);
            results.add(new TestResult(n, "stripColors utility", ok, ok ? null : "Got: " + result));
        } catch (Exception e) {
            results.add(new TestResult(n, "stripColors utility", false, e.toString()));
        }

        // 35. onJoinServer config parsing
        n++;
        try {
            java.nio.file.Path tmpDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime());
            FlowChatConfig ojsConfig = new FlowChatConfig(tmpDir);
            java.nio.file.Files.createDirectories(tmpDir);
            java.nio.file.Files.write(tmpDir.resolve("flowchat.json"),
                "{\"incoming\":[],\"onJoinServer\":[{\"commands\":[\"/hub\",\"/play skyblock\"],\"server\":\".*hypixel.*\",\"delay\":5,\"description\":\"Hypixel auto-join\"}]}".getBytes());
            ojsConfig.load();
            List<JsonObject> entries = ojsConfig.getOnJoinServer();
            boolean ok = entries.size() == 1 && entries.get(0).has("commands") && entries.get(0).has("server");
            results.add(new TestResult(n, "onJoinServer config parsing", ok, ok ? null : "Size: " + entries.size()));
        } catch (Exception e) {
            results.add(new TestResult(n, "onJoinServer config parsing", false, e.toString()));
        }

        // 36. onJoinServer server filter regex matching
        n++;
        try {
            java.nio.file.Path tmpDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime());
            FlowChatConfig ojsConfig = new FlowChatConfig(tmpDir);
            java.nio.file.Files.createDirectories(tmpDir);
            java.nio.file.Files.write(tmpDir.resolve("flowchat.json"),
                "{\"incoming\":[],\"onJoinServer\":[{\"commands\":[\"/cmd\"],\"server\":\".*hypixel.*\"}]}".getBytes());
            ojsConfig.load();
            JsonObject entry = ojsConfig.getOnJoinServer().get(0);
            String regex = entry.get("server").getAsString();
            boolean match = "mc.hypixel.net".matches(regex);
            boolean noMatch = !"play.cubecraft.net".matches(regex);
            boolean ok = match && noMatch;
            results.add(new TestResult(n, "onJoinServer server filter", ok, ok ? null : "match=" + match + " noMatch=" + noMatch));
        } catch (Exception e) {
            results.add(new TestResult(n, "onJoinServer server filter", false, e.toString()));
        }

        // 37. onJoinServer commands array
        n++;
        try {
            java.nio.file.Path tmpDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime());
            FlowChatConfig ojsConfig = new FlowChatConfig(tmpDir);
            java.nio.file.Files.createDirectories(tmpDir);
            java.nio.file.Files.write(tmpDir.resolve("flowchat.json"),
                "{\"incoming\":[],\"onJoinServer\":[{\"commands\":[\"/hub\",\"/play skyblock\",\"/warp\"]}]}".getBytes());
            ojsConfig.load();
            JsonObject entry = ojsConfig.getOnJoinServer().get(0);
            JsonArray cmds = entry.getAsJsonArray("commands");
            boolean ok = cmds.size() == 3 && "/hub".equals(cmds.get(0).getAsString()) && "/warp".equals(cmds.get(2).getAsString());
            results.add(new TestResult(n, "onJoinServer commands array", ok, ok ? null : "Got " + cmds));
        } catch (Exception e) {
            results.add(new TestResult(n, "onJoinServer commands array", false, e.toString()));
        }

        // 38. onJoinServer delay defaults to 0
        n++;
        try {
            java.nio.file.Path tmpDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime());
            FlowChatConfig ojsConfig = new FlowChatConfig(tmpDir);
            java.nio.file.Files.createDirectories(tmpDir);
            java.nio.file.Files.write(tmpDir.resolve("flowchat.json"),
                "{\"incoming\":[],\"onJoinServer\":[{\"commands\":[\"/cmd\"]},{\"commands\":[\"/cmd2\"],\"delay\":10}]}".getBytes());
            ojsConfig.load();
            List<JsonObject> entries = ojsConfig.getOnJoinServer();
            boolean noDelay = !entries.get(0).has("delay");
            int defaultDelay = entries.get(0).has("delay") ? entries.get(0).get("delay").getAsInt() : 0;
            int explicitDelay = entries.get(1).get("delay").getAsInt();
            boolean ok = defaultDelay == 0 && explicitDelay == 10;
            results.add(new TestResult(n, "onJoinServer delay defaults", ok, ok ? null : "default=" + defaultDelay + " explicit=" + explicitDelay));
        } catch (Exception e) {
            results.add(new TestResult(n, "onJoinServer delay defaults", false, e.toString()));
        }

        // 39. onJoinServer empty array
        n++;
        try {
            java.nio.file.Path tmpDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime());
            FlowChatConfig ojsConfig = new FlowChatConfig(tmpDir);
            java.nio.file.Files.createDirectories(tmpDir);
            java.nio.file.Files.write(tmpDir.resolve("flowchat.json"),
                "{\"incoming\":[],\"onJoinServer\":[]}".getBytes());
            ojsConfig.load();
            boolean ok = ojsConfig.getOnJoinServer().isEmpty();
            results.add(new TestResult(n, "onJoinServer empty array", ok, ok ? null : "Not empty"));
        } catch (Exception e) {
            results.add(new TestResult(n, "onJoinServer empty array", false, e.toString()));
        }

        // 40. onJoinServer missing field
        n++;
        try {
            java.nio.file.Path tmpDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "flowchat-test-" + System.nanoTime());
            FlowChatConfig ojsConfig = new FlowChatConfig(tmpDir);
            java.nio.file.Files.createDirectories(tmpDir);
            java.nio.file.Files.write(tmpDir.resolve("flowchat.json"),
                "{\"incoming\":[]}".getBytes());
            ojsConfig.load();
            boolean ok = ojsConfig.getOnJoinServer().isEmpty();
            results.add(new TestResult(n, "onJoinServer missing field", ok, ok ? null : "Not empty: " + ojsConfig.getOnJoinServer().size()));
        } catch (Exception e) {
            results.add(new TestResult(n, "onJoinServer missing field", false, e.toString()));
        }

        return results;
    }

    // --- Helper ---

    private static FlowChatRule makeRule(String json) {
        return new FlowChatRule(JsonParser.parseString(json).getAsJsonObject());
    }
}
