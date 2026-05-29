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
        public String notifyStyle; // "actionbar", "toast", "advancement"
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

    /**
     * Process a message through rules.
     *
     * @param message    plain text of the message
     * @param rules      list of rules to apply
     * @param serverIp   server identifier for server-filtering
     * @param username   current player's username (for {username} tag)
     * @param serverName server display name (for {servername} tag)
     * @return processing result
     */
    public Result process(String message, List<FlowChatRule> rules, String serverIp,
                          String username, String serverName) {
        return process(message, rules, serverIp, username, serverName, null);
    }

    /**
     * Process a message through rules, with optional raw JSON for matchJson rules.
     *
     * @param message    plain text of the message
     * @param rules      list of rules to apply
     * @param serverIp   server identifier
     * @param username   current player's username
     * @param serverName server display name
     * @param rawJson    raw JSON component string (for Feature #6 matchJson rules), may be null
     * @return processing result
     */
    public Result process(String message, List<FlowChatRule> rules, String serverIp,
                          String username, String serverName, String rawJson) {
        Result result = new Result(message);

        // Default text: strip § codes and normalize whitespace
        String strippedMsg = message
                .replaceAll("\\r", "\\\\r")
                .replaceAll("\\n", "\\\\n")
                .replaceAll("\u00a7\\w", "");

        // Feature #3: color-aware version preserves § codes
        String colorMsg = message
                .replaceAll("\\r", "\\\\r")
                .replaceAll("\\n", "\\\\n");

        boolean anyMatch = false;

        for (FlowChatRule rule : rules) {
            if (!rule.matchesServer(serverIp)) continue;

            // Choose which text to match against based on rule flags
            String matchText;
            if (rule.matchJson) {
                if (rawJson == null) continue; // matchJson requires JSON; skip if unavailable
                // Feature #6: match against raw JSON component text
                matchText = rawJson;
            } else if (rule.colorAware) {
                // Feature #3: match against text WITH color codes preserved
                matchText = colorMsg;
            } else {
                // Default: match against stripped text (legacy behavior)
                matchText = strippedMsg;
            }

            Matcher matcher = rule.pattern.matcher(matchText);
            if (!matcher.find()) continue;

            anyMatch = true;

            if (rule.respondMsg != null && autoResponseDepth < MAX_AUTO_RESPONSE_DEPTH) {
                autoResponseDepth++;
                try {
                    collectAutoResponses(result, rule, matchText, serverIp, username, serverName);
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
                replStr = handleValueStacking(rule, matcher, matchText, replStr);
            }
            replStr = replaceTags(replStr, serverIp, username, serverName);

            // Apply replacement to the appropriate text streams
            if (rule.matchJson && rawJson != null) {
                // For JSON matching, replacement goes into stripped text for display
                strippedMsg = matchText.replaceAll(rule.search, replStr);
            } else if (rule.colorAware) {
                colorMsg = colorMsg.replaceAll(rule.search, replStr);
                strippedMsg = colorMsg.replaceAll("\u00a7\\w", "");
            } else {
                strippedMsg = strippedMsg.replaceAll(rule.search, replStr);
            }
        }

        if (!anyMatch) return result;

        // Use color-aware text if any colorAware rule matched, else stripped
        result.processedText = strippedMsg;
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

    /**
     * Strip § color codes from text.
     * Useful for extracting plain text from formatted messages.
     */
    public static String stripColors(String text) {
        if (text == null) return null;
        return text.replaceAll("\u00a7\\w", "");
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
