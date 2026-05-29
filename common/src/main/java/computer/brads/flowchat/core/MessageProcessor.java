package computer.brads.flowchat.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;

public class MessageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowchat");
    private static final int MAX_AUTO_RESPONSE_DEPTH = 1;

    private final Map<String, ValueStackCache> stackCaches = new HashMap<>();
    private int autoResponseDepth = 0;

    public static class Result {
        public final String originalText;
        public String processedText;
        public boolean cancelled;
        public boolean toast;
        public String notifyStyle;
        public boolean playSound;
        public String soundId;
        public final List<String> autoResponses = new ArrayList<>();

        public Result(String text) {
            this.originalText = text;
            this.processedText = text;
            this.notifyStyle = "actionbar";
        }

        public boolean wasModified() {
            return !originalText.equals(processedText) || cancelled || toast;
        }
    }

    public Result process(String message, List<FlowChatRule> rules, String serverIp) {
        return process(message, rules, serverIp, null, null);
    }

    public Result process(String message, List<FlowChatRule> rules, String serverIp,
                          String username, String serverName) {
        Result result = new Result(message);
        String msg = message
                .replaceAll("\\r", "\\\\r")
                .replaceAll("\\n", "\\\\n")
                .replaceAll("\u00a7\\w", "");

        boolean anyMatch = false;

        for (FlowChatRule rule : rules) {
            if (!rule.matchesServer(serverIp)) continue;

            Matcher matcher = rule.pattern.matcher(msg);
            if (!matcher.find()) continue;

            anyMatch = true;

            if (rule.respondMsg != null && autoResponseDepth < MAX_AUTO_RESPONSE_DEPTH) {
                autoResponseDepth++;
                try {
                    collectAutoResponses(result, rule, msg, serverIp, username, serverName);
                } finally {
                    autoResponseDepth--;
                }
            }

            if (!result.toast && rule.toast) {
                result.toast = true;
                result.notifyStyle = rule.notifyStyle;
            }

            if (!result.playSound && rule.playSound) {
                result.playSound = true;
                result.soundId = rule.soundId;
            }

            String replStr = rule.replacement;
            if (rule.valueStack != null) {
                replStr = handleValueStacking(rule, matcher, msg, replStr);
            }
            replStr = replaceTags(replStr, serverIp, username, serverName);
            msg = msg.replaceAll(rule.search, replStr);
        }

        if (!anyMatch) return result;

        result.processedText = msg;
        if (result.toast) result.cancelled = true;

        return result;
    }

    public static String replaceTags(String input, String serverIp, String username, String serverName) {
        if (input == null || !input.contains("{")) return input;
        String result = input;
        if (result.contains("{username}") && username != null)
            result = result.replace("{username}", username);
        if (result.contains("{serverip}") && serverIp != null)
            result = result.replace("{serverip}", serverIp);
        if (result.contains("{servername}") && serverName != null)
            result = result.replace("{servername}", serverName);
        if (result.contains("{time}"))
            result = result.replace("{time}", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return result;
    }

    /**
     * Apply & color code to § conversion.
     * Shared across all platforms — call from platform code after processing.
     */
    public static String formatColors(String text) {
        if (text == null) return null;
        return text.replaceAll("&([0-9a-fk-or])", "\u00a7$1");
    }

    private void collectAutoResponses(Result result, FlowChatRule rule, String msg,
                                       String serverIp, String username, String serverName) {
        try {
            if (rule.respondMsg.isJsonArray()) {
                for (JsonElement elem : rule.respondMsg.getAsJsonArray()) {
                    result.autoResponses.add(msg.replaceAll(rule.search,
                            replaceTags(elem.getAsString(), serverIp, username, serverName)));
                }
            } else {
                result.autoResponses.add(msg.replaceAll(rule.search,
                        replaceTags(rule.respondMsg.getAsString(), serverIp, username, serverName)));
            }
        } catch (Exception e) { LOGGER.error("Error collecting auto-responses", e); }
    }

    private String handleValueStacking(FlowChatRule rule, Matcher pmatch, String msg, String replstr) {
        try {
            JsonObject vstack = rule.valueStack;
            String separator = vstack.has("seperate_float_with") ? vstack.get("seperate_float_with").getAsString() : ".";
            if (!vstack.has("stack_values")) return replstr;

            String stackerrepl = rule.replacement;
            if (vstack.has("ignore_diffs")) {
                for (JsonElement repl : vstack.get("ignore_diffs").getAsJsonArray())
                    stackerrepl = stackerrepl.replaceAll("\\$" + repl.getAsInt(), "");
            }
            stackerrepl = stackerrepl.replaceAll("\\$\\^i", "");
            for (JsonElement repl : vstack.get("stack_values").getAsJsonArray()) {
                stackerrepl = stackerrepl.replaceAll("\\$" + repl.getAsInt(), "");
                stackerrepl = stackerrepl.replaceAll("\\$\\^" + repl.getAsInt(), "");
            }

            String stackermatcher = msg.replaceAll(rule.search, stackerrepl);
            int expireSec = vstack.has("expire_after") ? vstack.get("expire_after").getAsInt() : 4;
            stackCaches.putIfAbsent(stackermatcher, new ValueStackCache(expireSec));
            ValueStackCache cache = stackCaches.get(stackermatcher);
            int now = (int) (Instant.now().toEpochMilli() / 1000);

            for (JsonElement repl : vstack.get("stack_values").getAsJsonArray()) {
                int idx = repl.getAsInt();
                String raw = pmatch.group(idx).replace(separator, ".").replaceAll("[^\\d.]", "");
                double val = Double.parseDouble(raw);
                if (cache.values.containsKey(idx) && cache.expireEpoch > now) val += cache.values.get(idx);
                cache.values.put(idx, val);
                replstr = replstr.replaceAll("\\$\\^" + idx, NumberFormat.getInstance().format(val));
            }

            if (cache.expireEpoch > now) cache.iterCount++;
            else cache.iterCount = 1;
            replstr = replstr.replaceAll("\\$\\^i", String.valueOf(cache.iterCount));
            cache.expireEpoch = now + expireSec;
        } catch (Exception e) { LOGGER.debug("Value stacking error", e); }
        return replstr;
    }

    private static class ValueStackCache {
        Map<Integer, Double> values = new HashMap<>();
        int expireEpoch;
        int iterCount = 0;
        ValueStackCache(int expireSec) {
            this.expireEpoch = (int) (Instant.now().toEpochMilli() / 1000) + expireSec;
        }
    }
}
