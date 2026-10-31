package com.errolmc.inventorysorter.client.config;

import net.minecraft.network.chat.Component;

/** Which panel the sort key acts on. */
public enum HotkeyTarget {
	/** The panel under the mouse; the container when the mouse is not over a slot. */
	UNDER_MOUSE,
	/** Always the container (your inventory in screens without one). */
	CONTAINER,
	/** Always your inventory. */
	PLAYER;

	public Component displayName() {
		return Component.translatable("inventory-sorter.hotkey_target." + name().toLowerCase());
	}
}
