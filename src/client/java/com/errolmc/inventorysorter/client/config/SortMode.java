package com.errolmc.inventorysorter.client.config;

import net.minecraft.network.chat.Component;

/** What the sort buttons order items by. */
public enum SortMode {
	/** Registry order, which keeps similar items (stone, wood, ores, tools...) together. */
	ITEM_TYPE,
	/** Display name, A to Z. */
	NAME,
	/** Item id ({@code minecraft:acacia_boat}...), A to Z. */
	ITEM_ID,
	/** Most items first. */
	COUNT,
	/** Grouped by the mod that adds the item, then item type. */
	MOD;

	public Component displayName() {
		return Component.translatable("inventory-sorter.sort_mode." + name().toLowerCase());
	}

	public SortMode next() {
		SortMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}
}
