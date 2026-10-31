package com.errolmc.inventorysorter.client.config;

import net.minecraft.network.chat.Component;

/** What the sort key (R by default) does. */
public enum HotkeyAction {
	SORT,
	SORT_COLUMNS,
	SORT_ROWS,
	/** Move everything to the other side of the menu; only in storage screens. */
	MOVE_ALL;

	public Component displayName() {
		return Component.translatable("inventory-sorter.hotkey_action." + name().toLowerCase());
	}
}
