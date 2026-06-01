package computer.brads.flowchat.core;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;

/**
 * Tests for @ tag selector resolution in MessageProcessor.
 * Covers standalone tags, item tags (bare/prefixed), all unresolved behaviors,
 * slot addressing, armor slots, and edge cases from TAG_SELECTORS_SPEC.md.
 */
public class TagSelectorTest {

    private TagSettings defaultSettings;
    private ItemData diamond_sword;
    private ItemData golden_apple;
    private ItemData iron_helmet;
    private ItemData empty;

    @Before
    public void setUp() {
        defaultSettings = TagSettings.DEFAULT;
        diamond_sword = new ItemData("Diamond Sword", "minecraft:diamond_sword", 1,
                1500, 1561, true,
                Arrays.asList("Sharpness V", "Unbreaking III"),
                Arrays.asList("A legendary blade"),
                Arrays.asList("+7 Attack Damage"));
        golden_apple = new ItemData("Golden Apple", "minecraft:golden_apple", 16,
                0, 0, false,
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
        iron_helmet = new ItemData("Iron Helmet", "minecraft:iron_helmet", 1,
                150, 165, true,
                Arrays.asList("Protection IV"),
                Collections.<String>emptyList(), Collections.<String>emptyList());
        empty = ItemData.EMPTY;
    }

    private TagContext makeCtx(ItemData mainHand) {
        return makeCtx(mainHand, null, null, null);
    }

    private TagContext makeCtx(ItemData mainHand, ItemData offhand, ItemData head, Map<UUID, PlayerActivityTracker> players) {
        ItemData[] slots = new ItemData[41];
        slots[0] = mainHand; // selected slot = 0
        if (offhand != null) slots[40] = offhand;
        if (head != null) slots[39] = head;
        return new TagContext("Steve", "550e8400-e29b-41d4-a716-446655440000",
                "mc.hypixel.net", "Hypixel",
                100.5, 64.0, -200.3, "overworld",
                0, slots,
                players != null ? players : Collections.<UUID, PlayerActivityTracker>emptyMap(),
                defaultSettings);
    }

    private TagContext makeCtxWithSettings(TagSettings settings) {
        ItemData[] slots = new ItemData[41];
        slots[0] = diamond_sword;
        return new TagContext("Steve", "550e8400-e29b-41d4-a716-446655440000",
                "mc.hypixel.net", "Hypixel",
                100.5, 64.0, -200.3, "overworld",
                0, slots,
                Collections.<UUID, PlayerActivityTracker>emptyMap(),
                settings);
    }

    // ========== Standalone Tags ==========

    @Test
    public void tagSelf_resolvesUsername() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Hello @s!", ctx);
        assertEquals("Hello Steve!", r.text);
        assertFalse(r.cancelled);
    }

    @Test
    public void tagSelfUuid_resolvesPlayerUuid() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("UUID: @su", ctx);
        assertEquals("UUID: 550e8400-e29b-41d4-a716-446655440000", r.text);
    }

    @Test
    public void tagIp_resolvesServerIp() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Server: @ip", ctx);
        assertEquals("Server: mc.hypixel.net", r.text);
    }

    @Test
    public void tagTime_resolvesCurrentTime() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Time: @t", ctx);
        assertTrue(r.text.matches("Time: \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void tagCoords_defaultFormat() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("At @c", ctx);
        assertEquals("At 101 64 -200", r.text); // rounds
    }

    @Test
    public void tagCoords_commaFormat() {
        TagSettings settings = new TagSettings(com.google.gson.JsonParser.parseString(
                "{\"coordinateFormat\": \"x, y, z\"}").getAsJsonObject());
        TagContext ctx = makeCtxWithSettings(settings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("At @c", ctx);
        assertEquals("At 101, 64, -200", r.text);
    }

    @Test
    public void tagCoords_dimFormat() {
        TagSettings settings = new TagSettings(com.google.gson.JsonParser.parseString(
                "{\"coordinateFormat\": \"x y z [dim]\"}").getAsJsonObject());
        TagContext ctx = makeCtxWithSettings(settings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("At @c", ctx);
        assertEquals("At 101 64 -200 [overworld]", r.text);
    }

    // ========== Player List Tags ==========

    @Test
    public void tagPlayerList_noPlayers() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Players: @l online: @a", ctx);
        assertEquals("Players: 0 online: ", r.text);
    }

    @Test
    public void tagPlayerList_withRealPlayers() {
        long now = System.currentTimeMillis();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        PlayerActivityTracker p1 = new PlayerActivityTracker(id1, "Alex");
        PlayerActivityTracker p2 = new PlayerActivityTracker(id2, "Notch");
        // Make p1 real (5+ movements), p2 bot (< threshold)
        for (int i = 0; i < 6; i++) p1.recordMovement(now - i * 1000);
        p2.recordMovement(now - 1000); // only 1 movement

        Map<UUID, PlayerActivityTracker> players = new LinkedHashMap<UUID, PlayerActivityTracker>();
        players.put(id1, p1);
        players.put(id2, p2);

        TagContext ctx = makeCtx(empty, null, null, players);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Count: @l Names: @a", ctx);
        assertEquals("Count: 1 Names: Alex", r.text);
    }

    @Test
    public void tagPlayerUuids_au() {
        long now = System.currentTimeMillis();
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        PlayerActivityTracker p1 = new PlayerActivityTracker(id1, "Alex");
        for (int i = 0; i < 6; i++) p1.recordMovement(now - i * 1000);

        Map<UUID, PlayerActivityTracker> players = new LinkedHashMap<UUID, PlayerActivityTracker>();
        players.put(id1, p1);

        TagContext ctx = makeCtx(empty, null, null, players);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("UUIDs: @au", ctx);
        assertEquals("UUIDs: 00000000-0000-0000-0000-000000000001", r.text);
    }

    // ========== Main Hand Item Tags (bare suffix) ==========

    @Test
    public void tagItemName_bare() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Holding @i", ctx);
        assertEquals("Holding Diamond Sword", r.text);
    }

    @Test
    public void tagItemNameAlias_in() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Holding @in", ctx);
        assertEquals("Holding Diamond Sword", r.text);
    }

    @Test
    public void tagItemCount_ic() {
        TagContext ctx = makeCtx(golden_apple);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Count: @ic", ctx);
        assertEquals("Count: 16", r.text);
    }

    @Test
    public void tagItemId_id() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("ID: @id", ctx);
        assertEquals("ID: minecraft:diamond_sword", r.text);
    }

    @Test
    public void tagItemDetails_d() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Details: @d", ctx);
        assertEquals("Details: Diamond Sword [Sharpness V, Unbreaking III] (A legendary blade) {+7 Attack Damage}", r.text);
    }

    @Test
    public void tagItemDetails_ie() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Info: @ie", ctx);
        assertEquals("Info: Diamond Sword [Sharpness V, Unbreaking III] (A legendary blade) {+7 Attack Damage}", r.text);
    }

    @Test
    public void tagDurability_du() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Dur: @du", ctx);
        assertEquals("Dur: 1500/1561", r.text);
    }

    @Test
    public void tagDurability_dur() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Dur: @dur", ctx);
        assertEquals("Dur: 1500/1561", r.text);
    }

    @Test
    public void tagDurability_idu() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Dur: @idu", ctx);
        assertEquals("Dur: 1500/1561", r.text);
    }

    @Test
    public void tagDurability_percentFormat() {
        TagSettings settings = new TagSettings(com.google.gson.JsonParser.parseString(
                "{\"durabilityFormat\": \"percent\"}").getAsJsonObject());
        TagContext ctx = makeCtxWithSettings(settings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Dur: @du", ctx);
        assertEquals("Dur: 96%", r.text); // 1500*100/1561 = 96
    }

    @Test
    public void tagDurability_noDurItem() {
        TagContext ctx = makeCtx(golden_apple);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Dur: @du", ctx);
        assertEquals("Dur: ", r.text); // no durability → empty
    }

    @Test
    public void tagEmptySlot_returnsEmpty() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Item: @i end", ctx);
        assertEquals("Item:  end", r.text); // empty slot → ""
    }

    // ========== Offhand Prefix ==========

    @Test
    public void tagOffhand_oi() {
        TagContext ctx = makeCtx(empty, golden_apple, null, null);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Offhand: @oi", ctx);
        assertEquals("Offhand: Golden Apple", r.text);
    }

    @Test
    public void tagOffhand_oic() {
        TagContext ctx = makeCtx(empty, golden_apple, null, null);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Count: @oic", ctx);
        assertEquals("Count: 16", r.text);
    }

    @Test
    public void tagOffhand_oid() {
        TagContext ctx = makeCtx(empty, golden_apple, null, null);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("ID: @oid", ctx);
        assertEquals("ID: minecraft:golden_apple", r.text);
    }

    // ========== Armor Prefix ==========

    @Test
    public void tagArmor_hi() {
        TagContext ctx = makeCtx(empty, null, iron_helmet, null);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Head: @hi", ctx);
        assertEquals("Head: Iron Helmet", r.text);
    }

    @Test
    public void tagArmor_hie() {
        TagContext ctx = makeCtx(empty, null, iron_helmet, null);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Info: @hie", ctx);
        assertEquals("Info: Iron Helmet [Protection IV]", r.text);
    }

    @Test
    public void tagArmor_hdu() {
        TagContext ctx = makeCtx(empty, null, iron_helmet, null);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Dur: @hdu", ctx);
        assertEquals("Dur: 150/165", r.text);
    }

    // ========== Numeric Slot Prefix ==========

    @Test
    public void tagSlot0_item() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Slot0: @0i", ctx);
        assertEquals("Slot0: Diamond Sword", r.text);
    }

    @Test
    public void tagSlot23_item() {
        ItemData[] slots = new ItemData[41];
        slots[23] = golden_apple;
        TagContext ctx = new TagContext("Steve", "uuid", "ip", "srv",
                0, 0, 0, "overworld", 0, slots,
                Collections.<UUID, PlayerActivityTracker>emptyMap(), defaultSettings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Slot23: @23i", ctx);
        assertEquals("Slot23: Golden Apple", r.text);
    }

    @Test
    public void tagSlot23_count() {
        ItemData[] slots = new ItemData[41];
        slots[23] = golden_apple;
        TagContext ctx = new TagContext("Steve", "uuid", "ip", "srv",
                0, 0, 0, "overworld", 0, slots,
                Collections.<UUID, PlayerActivityTracker>emptyMap(), defaultSettings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Count: @23ic", ctx);
        assertEquals("Count: 16", r.text);
    }

    @Test
    public void tagSlot_outOfRange() {
        TagContext ctx = makeCtx(empty);
        // Slot 99 — out of range, should be unresolved
        // Default behavior is cancel
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Item: @99i", ctx);
        assertTrue(r.cancelled);
    }

    @Test
    public void tagSlot35_valid() {
        ItemData[] slots = new ItemData[41];
        slots[35] = golden_apple;
        TagContext ctx = new TagContext("Steve", "uuid", "ip", "srv",
                0, 0, 0, "overworld", 0, slots,
                Collections.<UUID, PlayerActivityTracker>emptyMap(), defaultSettings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Last: @35i", ctx);
        assertEquals("Last: Golden Apple", r.text);
    }

    // ========== Unresolved Behavior ==========

    @Test
    public void unresolved_cancel() {
        // Default behavior is "cancel"
        TagContext ctx = makeCtx(empty);
        // @xyz is not a valid tag
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Test @xyz end", ctx);
        assertTrue(r.cancelled);
    }

    @Test
    public void unresolved_passthrough() {
        TagSettings settings = new TagSettings(com.google.gson.JsonParser.parseString(
                "{\"unresolvedBehavior\": \"passthrough\"}").getAsJsonObject());
        TagContext ctx = makeCtxWithSettings(settings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Test @xyz end", ctx);
        assertFalse(r.cancelled);
        assertEquals("Test @xyz end", r.text);
    }

    @Test
    public void unresolved_strip() {
        TagSettings settings = new TagSettings(com.google.gson.JsonParser.parseString(
                "{\"unresolvedBehavior\": \"strip\"}").getAsJsonObject());
        TagContext ctx = makeCtxWithSettings(settings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Test @xyz end", ctx);
        assertFalse(r.cancelled);
        assertEquals("Test  end", r.text);
    }

    @Test
    public void unresolved_fallback() {
        TagSettings settings = new TagSettings(com.google.gson.JsonParser.parseString(
                "{\"unresolvedBehavior\": \"fallback\", \"unresolvedFallback\": \"?\"}").getAsJsonObject());
        TagContext ctx = makeCtxWithSettings(settings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Test @xyz end", ctx);
        assertFalse(r.cancelled);
        assertEquals("Test ? end", r.text);
    }

    // ========== Mixed Tags ==========

    @Test
    public void multipleTags_inOneLine() {
        TagContext ctx = makeCtx(diamond_sword, golden_apple, iron_helmet, null);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors(
                "@s is at @c holding @i with @oi in offhand", ctx);
        assertEquals("Steve is at 101 64 -200 holding Diamond Sword with Golden Apple in offhand", r.text);
    }

    @Test
    public void adjacentTags() {
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("@s@i", ctx);
        assertEquals("SteveDiamond Sword", r.text);
    }

    // ========== No @ in string ==========

    @Test
    public void noAtSign_passthrough() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Hello world", ctx);
        assertEquals("Hello world", r.text);
        assertFalse(r.cancelled);
    }

    @Test
    public void nullInput_passthrough() {
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors(null, ctx);
        assertNull(r.text);
        assertFalse(r.cancelled);
    }

    // ========== Integration: process() with TagContext ==========

    @Test
    public void processWithTagContext_resolvesInReplacement() {
        MessageProcessor processor = new MessageProcessor();
        FlowChatRule rule = new FlowChatRule(com.google.gson.JsonParser.parseString(
                "{\"pattern\": \"test\", \"replacement\": \"@s is holding @i\"}").getAsJsonObject());
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule),
                "mc.test.com", "Steve", "TestServer", null, ctx);
        assertEquals("Steve is holding Diamond Sword", r.processedText);
    }

    @Test
    public void processWithTagContext_cancelOnUnresolved() {
        MessageProcessor processor = new MessageProcessor();
        FlowChatRule rule = new FlowChatRule(com.google.gson.JsonParser.parseString(
                "{\"pattern\": \"test\", \"replacement\": \"@s has @xyz item\"}").getAsJsonObject());
        TagContext ctx = makeCtx(diamond_sword);
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule),
                "mc.test.com", "Steve", "TestServer", null, ctx);
        assertTrue(r.cancelled);
    }

    @Test
    public void processWithoutTagContext_noResolution() {
        MessageProcessor processor = new MessageProcessor();
        FlowChatRule rule = new FlowChatRule(com.google.gson.JsonParser.parseString(
                "{\"pattern\": \"test\", \"replacement\": \"@s is here\"}").getAsJsonObject());
        // No TagContext (null) — @ tags should remain unresolved
        MessageProcessor.Result r = processor.process("test", Collections.singletonList(rule),
                "mc.test.com", "Steve", "TestServer", null, null);
        assertEquals("@s is here", r.processedText);
    }

    // ========== Standalone tag collision avoidance ==========

    @Test
    public void tagC_isCoords_notChestArmor() {
        // @c should resolve to coordinates, NOT chest armor item
        // because 'c' is in STANDALONE_TAGS and checked first
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Pos: @c", ctx);
        assertEquals("Pos: 101 64 -200", r.text);
    }

    @Test
    public void tagL_isPlayerCount_notLegsArmor() {
        // @l should resolve to real player count, NOT legs armor
        TagContext ctx = makeCtx(empty);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Count: @l", ctx);
        assertEquals("Count: 0", r.text);
    }

    @Test
    public void tagCI_isChestItem() {
        // @ci = chest armor item name — 'c' prefix + 'i' suffix
        ItemData[] slots = new ItemData[41];
        ItemData chestplate = new ItemData("Diamond Chestplate", "minecraft:diamond_chestplate", 1,
                500, 528, true, Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList());
        slots[38] = chestplate; // chest slot
        TagContext ctx = new TagContext("Steve", "uuid", "ip", "srv",
                0, 0, 0, "overworld", 0, slots,
                Collections.<UUID, PlayerActivityTracker>emptyMap(), defaultSettings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Chest: @ci", ctx);
        assertEquals("Chest: Diamond Chestplate", r.text);
    }

    @Test
    public void tagLI_isLegsItem() {
        // @li = legs armor item name
        ItemData[] slots = new ItemData[41];
        ItemData leggings = new ItemData("Iron Leggings", "minecraft:iron_leggings", 1,
                200, 225, true, Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList());
        slots[37] = leggings;
        TagContext ctx = new TagContext("Steve", "uuid", "ip", "srv",
                0, 0, 0, "overworld", 0, slots,
                Collections.<UUID, PlayerActivityTracker>emptyMap(), defaultSettings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Legs: @li", ctx);
        assertEquals("Legs: Iron Leggings", r.text);
    }

    // ========== Item details with no enchants/lore/attrs ==========

    @Test
    public void tagDetails_plainItem() {
        TagContext ctx = makeCtx(golden_apple);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Info: @d", ctx);
        assertEquals("Info: Golden Apple", r.text); // no enchants/lore/attrs
    }

    // ========== Multi-player separator ==========

    @Test
    public void multiPlayerSeparator_custom() {
        long now = System.currentTimeMillis();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        PlayerActivityTracker p1 = new PlayerActivityTracker(id1, "Alex");
        PlayerActivityTracker p2 = new PlayerActivityTracker(id2, "Notch");
        for (int i = 0; i < 6; i++) {
            p1.recordMovement(now - i * 1000);
            p2.recordMovement(now - i * 1000);
        }

        Map<UUID, PlayerActivityTracker> players = new LinkedHashMap<UUID, PlayerActivityTracker>();
        players.put(id1, p1);
        players.put(id2, p2);

        TagSettings settings = new TagSettings(com.google.gson.JsonParser.parseString(
                "{\"multiPlayerSeparator\": \" | \"}").getAsJsonObject());
        ItemData[] slots = new ItemData[41];
        TagContext ctx = new TagContext("Steve", "uuid", "ip", "srv",
                0, 0, 0, "overworld", 0, slots, players, settings);
        MessageProcessor.ResolveResult r = MessageProcessor.resolveTagSelectors("Players: @a", ctx);
        assertEquals("Players: Alex | Notch", r.text);
    }
}
