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
import java.util.regex.Pattern;

public class MessageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowchat");
    private static final int MAX_AUTO_RESPONSE_DEPTH = 1;

    // Tag selector regex: @ followed by optional slot prefix + tag suffix
    // Matches: @s, @su, @ip, @a, @au, @l, @lu, @p, @pu, @c, @t, @i, @in, @ic, @id, @d, @ie, @du, @dur, @idu
    // And prefixed: @oi, @hi, @ci, @li, @bi, @0i, @23ic, etc.
    private static final Pattern TAG_PATTERN = Pattern.compile("@([a-z0-9]+)");

    // Valid item suffixes (longest first for greedy matching)
    private static final String[] ITEM_SUFFIXES = {"idu", "dur", "du", "ie", "in", "ic", "id", "i", "d"};

    // Standalone tags that match exactly (no prefix parsing)
    private static final Set<String> STANDALONE_TAGS = new HashSet<String>(Arrays.asList(
            "s", "su", "ip", "a", "au", "l", "lu", "p", "pu", "c", "t"
    ));

    // Armor prefix chars
    private static final String ARMOR_PREFIXES = "hclb";

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
        return process(message, rules, serverIp, null, null, null, null);
    }

    /**
     * Process a message through rules.
     */
    public Result process(String message, List<FlowChatRule> rules, String serverIp,
                          String username, String serverName) {
        return process(message, rules, serverIp, username, serverName, null, null);
    }

    /**
     * Process a message through rules, with optional raw JSON for matchJson rules.
     */
    public Result process(String message, List<FlowChatRule> rules, String serverIp,
                          String username, String serverName, String rawJson) {
        return process(message, rules, serverIp, username, serverName, rawJson, null);
    }

    /**
     * Process a message through rules, with optional TagContext for @ tag resolution.
     *
     * @param message    plain text of the message
     * @param rules      list of rules to apply
     * @param serverIp   server identifier
     * @param username   current player's username
     * @param serverName server display name
     * @param rawJson    raw JSON component string (for Feature #6 matchJson rules), may be null
     * @param tagContext client-side data for @ tag resolution, may be null (server-side)
     * @return processing result
     */
    public Result process(String message, List<FlowChatRule> rules, String serverIp,
                          String username, String serverName, String rawJson, TagContext tagContext) {
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

            // Resolve @ tag selectors if TagContext available
            if (tagContext != null) {
                ResolveResult tagResult = resolveTagSelectors(replStr, tagContext);
                if (tagResult.cancelled) {
                    result.cancelled = true;
                    return result;
                }
                replStr = tagResult.text;
            }

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

    // --- Tag Selector Resolution ---

    /**
     * Result of resolving @ tag selectors in a string.
     */
    public static class ResolveResult {
        public final String text;
        public final boolean cancelled;

        ResolveResult(String text, boolean cancelled) {
            this.text = text;
            this.cancelled = cancelled;
        }
    }

    /**
     * Resolve all @ tag selectors in input using TagContext.
     * Returns ResolveResult with resolved text, or cancelled=true if unresolvedBehavior="cancel"
     * and any tag couldn't be resolved.
     */
    public static ResolveResult resolveTagSelectors(String input, TagContext ctx) {
        if (input == null || !input.contains("@")) return new ResolveResult(input, false);

        TagSettings settings = ctx.settings;
        StringBuffer sb = new StringBuffer();
        Matcher m = TAG_PATTERN.matcher(input);
        boolean hasUnresolved = false;

        while (m.find()) {
            String tagBody = m.group(1); // everything after @
            String resolved = resolveTag(tagBody, ctx);

            if (resolved == null) {
                // Unresolved tag
                hasUnresolved = true;
                if ("cancel".equals(settings.unresolvedBehavior)) {
                    return new ResolveResult(input, true);
                } else if ("passthrough".equals(settings.unresolvedBehavior)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0))); // leave as-is
                } else if ("fallback".equals(settings.unresolvedBehavior)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(settings.unresolvedFallback));
                } else if ("strip".equals(settings.unresolvedBehavior)) {
                    m.appendReplacement(sb, "");
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                }
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(resolved));
            }
        }
        m.appendTail(sb);
        return new ResolveResult(sb.toString(), false);
    }

    /**
     * Resolve a single tag body (after @). Returns null if unresolved.
     */
    static String resolveTag(String tagBody, TagContext ctx) {
        // Step 1: Check standalone tags first (exact match)
        if (STANDALONE_TAGS.contains(tagBody)) {
            return resolveStandaloneTag(tagBody, ctx);
        }

        // Step 2: Check for bare item tags (no prefix, main hand)
        for (String suffix : ITEM_SUFFIXES) {
            if (tagBody.equals(suffix)) {
                return resolveItemTag(suffix, ctx.getMainHand(), ctx.settings);
            }
        }

        // Step 3: Prefixed item tags
        if (tagBody.length() >= 2) {
            char first = tagBody.charAt(0);

            // Offhand prefix 'o'
            if (first == 'o') {
                String rest = tagBody.substring(1);
                for (String suffix : ITEM_SUFFIXES) {
                    if (rest.equals(suffix)) {
                        return resolveItemTag(suffix, ctx.getOffhand(), ctx.settings);
                    }
                }
            }

            // Armor prefixes h/c/l/b — ONLY valid with item suffix
            if (ARMOR_PREFIXES.indexOf(first) >= 0) {
                String rest = tagBody.substring(1);
                for (String suffix : ITEM_SUFFIXES) {
                    if (rest.equals(suffix)) {
                        return resolveItemTag(suffix, ctx.getArmor(first), ctx.settings);
                    }
                }
                // Bare armor prefix (e.g. @h, @b) — unresolved
                if (tagBody.length() == 1) return null;
            }

            // Numeric slot prefix (greedy: consume all leading digits)
            if (first >= '0' && first <= '9') {
                // Find longest leading numeric prefix
                int numEnd = 1;
                while (numEnd < tagBody.length() && tagBody.charAt(numEnd) >= '0' && tagBody.charAt(numEnd) <= '9') {
                    numEnd++;
                }
                String numStr = tagBody.substring(0, numEnd);
                String rest = tagBody.substring(numEnd);

                for (String suffix : ITEM_SUFFIXES) {
                    if (rest.equals(suffix)) {
                        int slot;
                        try { slot = Integer.parseInt(numStr); }
                        catch (NumberFormatException e) { return null; }

                        // Valid slots: 0-35 (hotbar + inventory)
                        if (slot < 0 || slot > 35) return null; // out of range = unresolved
                        return resolveItemTag(suffix, ctx.getSlot(slot), ctx.settings);
                    }
                }
            }
        }

        // No match — unresolved
        return null;
    }

    private static String resolveStandaloneTag(String tag, TagContext ctx) {
        long now = System.currentTimeMillis();
        switch (tag) {
            case "s":  return ctx.username != null ? ctx.username : null;
            case "su": return ctx.playerUuid != null ? ctx.playerUuid : null;
            case "ip": return ctx.serverIp != null ? ctx.serverIp : null;
            case "t":  return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            case "c":  return formatCoordinates(ctx);
            case "l":  return String.valueOf(countRealPlayers(ctx, now));
            case "a":  return joinPlayerNames(ctx, now);
            case "au": return joinPlayerUuids(ctx, now);
            case "lu": return joinPlayerUuids(ctx, now); // same as @au
            case "p":  return nearestPlayerName(ctx, now);
            case "pu": return nearestPlayerUuid(ctx, now);
            default:   return null;
        }
    }

    private static String resolveItemTag(String suffix, ItemData item, TagSettings settings) {
        if (item == null || item.isEmpty) return "";

        switch (suffix) {
            case "i":
            case "in":
                return item.displayName;
            case "ic":
                return String.valueOf(item.count);
            case "id":
                return item.namespacedId;
            case "d":
            case "ie":
                return formatItemDetails(item);
            case "du":
            case "dur":
            case "idu":
                return formatDurability(item, settings);
            default:
                return null;
        }
    }

    private static String formatItemDetails(ItemData item) {
        StringBuilder sb = new StringBuilder(item.displayName);
        if (!item.enchantments.isEmpty()) {
            sb.append(" [");
            for (int i = 0; i < item.enchantments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(item.enchantments.get(i));
            }
            sb.append("]");
        }
        if (!item.lore.isEmpty()) {
            sb.append(" (");
            for (int i = 0; i < item.lore.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(item.lore.get(i));
            }
            sb.append(")");
        }
        if (!item.attributes.isEmpty()) {
            sb.append(" {");
            for (int i = 0; i < item.attributes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(item.attributes.get(i));
            }
            sb.append("}");
        }
        return sb.toString();
    }

    private static String formatDurability(ItemData item, TagSettings settings) {
        if (!item.hasDurability) return "";
        String fmt = settings.durabilityFormat;
        if ("current/max".equals(fmt)) return item.durability + "/" + item.maxDurability;
        if ("current".equals(fmt)) return String.valueOf(item.durability);
        if ("max".equals(fmt)) return String.valueOf(item.maxDurability);
        if ("percent".equals(fmt)) {
            if (item.maxDurability == 0) return "0%";
            return (item.durability * 100 / item.maxDurability) + "%";
        }
        return item.durability + "/" + item.maxDurability; // default
    }

    private static String formatCoordinates(TagContext ctx) {
        int x = (int) Math.round(ctx.playerX);
        int y = (int) Math.round(ctx.playerY);
        int z = (int) Math.round(ctx.playerZ);
        String fmt = ctx.settings.coordinateFormat;
        if ("x, y, z".equals(fmt)) return x + ", " + y + ", " + z;
        if ("x y z [dim]".equals(fmt)) return x + " " + y + " " + z + " [" + ctx.dimension + "]";
        return x + " " + y + " " + z; // default "x y z"
    }

    private static int countRealPlayers(TagContext ctx, long now) {
        int count = 0;
        for (PlayerActivityTracker t : ctx.getNearbyPlayers().values()) {
            if (t.isRealPlayer(now)) count++;
        }
        return count;
    }

    private static String joinPlayerNames(TagContext ctx, long now) {
        List<String> names = new ArrayList<String>();
        for (PlayerActivityTracker t : ctx.getNearbyPlayers().values()) {
            if (t.isRealPlayer(now)) names.add(t.name);
        }
        return joinStrings(names, ctx.settings.multiPlayerSeparator);
    }

    private static String joinPlayerUuids(TagContext ctx, long now) {
        List<String> uuids = new ArrayList<String>();
        for (PlayerActivityTracker t : ctx.getNearbyPlayers().values()) {
            if (t.isRealPlayer(now)) uuids.add(t.uuid.toString());
        }
        return joinStrings(uuids, ctx.settings.multiPlayerSeparator);
    }

    private static PlayerActivityTracker nearestPlayer(TagContext ctx, long now) {
        PlayerActivityTracker nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (PlayerActivityTracker t : ctx.getNearbyPlayers().values()) {
            if (!t.isRealPlayer(now)) continue;
            // Distance calc requires position data — use 0,0,0 as default for tracked players
            // (platform code should set actual positions; for now nearest = first real)
            if (nearest == null) {
                nearest = t;
            }
        }
        return nearest;
    }

    private static String nearestPlayerName(TagContext ctx, long now) {
        PlayerActivityTracker p = nearestPlayer(ctx, now);
        return p != null ? p.name : null;
    }

    private static String nearestPlayerUuid(TagContext ctx, long now) {
        PlayerActivityTracker p = nearestPlayer(ctx, now);
        return p != null ? p.uuid.toString() : null;
    }

    private static String joinStrings(List<String> items, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(items.get(i));
        }
        return sb.toString();
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
