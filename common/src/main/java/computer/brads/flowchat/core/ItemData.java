package computer.brads.flowchat.core;

import java.util.Collections;
import java.util.List;

/**
 * Platform-agnostic item data carrier.
 * Constructed by platform code (Fabric/Forge/NeoForge) from MC ItemStack.
 */
public class ItemData {
    public static final ItemData EMPTY = new ItemData();

    public final String displayName;
    public final String namespacedId;
    public final int count;
    public final int durability;
    public final int maxDurability;
    public final boolean hasDurability;
    public final List<String> enchantments;
    public final List<String> lore;
    public final List<String> attributes;
    public final boolean isEmpty;

    /** Empty/air slot */
    private ItemData() {
        this.displayName = "";
        this.namespacedId = "";
        this.count = 0;
        this.durability = 0;
        this.maxDurability = 0;
        this.hasDurability = false;
        this.enchantments = Collections.emptyList();
        this.lore = Collections.emptyList();
        this.attributes = Collections.emptyList();
        this.isEmpty = true;
    }

    public ItemData(String displayName, String namespacedId, int count,
                    int durability, int maxDurability, boolean hasDurability,
                    List<String> enchantments, List<String> lore, List<String> attributes) {
        this.displayName = displayName != null ? displayName : "";
        this.namespacedId = namespacedId != null ? namespacedId : "";
        this.count = count;
        this.durability = durability;
        this.maxDurability = maxDurability;
        this.hasDurability = hasDurability;
        this.enchantments = enchantments != null ? enchantments : Collections.emptyList();
        this.lore = lore != null ? lore : Collections.emptyList();
        this.attributes = attributes != null ? attributes : Collections.emptyList();
        this.isEmpty = false;
    }
}
