package computer.brads.flowchat.core;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side data carrier passed to MessageProcessor for tag resolution.
 * Constructed by platform code (Fabric/Forge/NeoForge) each time tags need resolving.
 * Server plugins pass null TagContext — tags remain unresolved.
 */
public class TagContext {
    public final String username;
    public final String playerUuid;
    public final String serverIp;
    public final String serverName;
    public final double playerX, playerY, playerZ;
    public final String dimension;   // "overworld", "the_nether", "the_end"
    public final int selectedSlot;   // 0-8 hotbar selection
    public final TagSettings settings;

    // Inventory: slots 0-35 (hotbar 0-8, inventory 9-35), armor 36-39 (feet/legs/chest/head), offhand 40
    private final ItemData[] inventorySlots;
    private final Map<UUID, PlayerActivityTracker> nearbyPlayers;

    public TagContext(String username, String playerUuid, String serverIp, String serverName,
                      double playerX, double playerY, double playerZ, String dimension,
                      int selectedSlot, ItemData[] inventorySlots,
                      Map<UUID, PlayerActivityTracker> nearbyPlayers, TagSettings settings) {
        this.username = username;
        this.playerUuid = playerUuid;
        this.serverIp = serverIp;
        this.serverName = serverName;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.dimension = dimension != null ? dimension : "overworld";
        this.selectedSlot = selectedSlot;
        this.inventorySlots = inventorySlots != null ? inventorySlots : new ItemData[41];
        this.nearbyPlayers = nearbyPlayers != null ? nearbyPlayers : Collections.<UUID, PlayerActivityTracker>emptyMap();
        this.settings = settings != null ? settings : TagSettings.DEFAULT;
    }

    /** Get item at inventory slot index (0-40). Returns ItemData.EMPTY for out-of-range or null. */
    public ItemData getSlot(int slot) {
        if (slot < 0 || slot >= inventorySlots.length || inventorySlots[slot] == null)
            return ItemData.EMPTY;
        return inventorySlots[slot];
    }

    /** Get main hand item (selected hotbar slot). */
    public ItemData getMainHand() {
        return getSlot(selectedSlot);
    }

    /** Get offhand item (slot 40). */
    public ItemData getOffhand() {
        return getSlot(40);
    }

    /** Get armor slot: h=head(39), c=chest(38), l=legs(37), b=boots(36). */
    public ItemData getArmor(char prefix) {
        switch (prefix) {
            case 'h': return getSlot(39);
            case 'c': return getSlot(38);
            case 'l': return getSlot(37);
            case 'b': return getSlot(36);
            default: return ItemData.EMPTY;
        }
    }

    public Map<UUID, PlayerActivityTracker> getNearbyPlayers() {
        return nearbyPlayers;
    }
}
