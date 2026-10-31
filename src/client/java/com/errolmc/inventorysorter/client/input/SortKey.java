package com.errolmc.inventorysorter.client.input;

import com.errolmc.inventorysorter.client.config.SorterConfig;
import com.errolmc.inventorysorter.client.sort.Section;
import com.errolmc.inventorysorter.client.sort.Sorter;
import com.errolmc.inventorysorter.sort.Layout;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/** The sort key (R by default, rebindable under Controls); what it does is set in the config. */
public final class SortKey {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("inventory-sorter", "main"));

	public static final KeyMapping KEY = new KeyMapping("key.inventory-sorter.sort", InputConstants.KEY_R, CATEGORY);

	private SortKey() {
	}

	public static void register() {
		KeyMappingHelper.registerKeyMapping(KEY);
	}

	/**
	 * Called from a container screen's key handling at the point where vanilla checks its own inventory key, so a
	 * focused text field gets the key first.
	 *
	 * @return true if the key was ours and something was done
	 */
	public static boolean handle(AbstractContainerScreen<?> screen, @Nullable Slot hoveredSlot, KeyEvent event) {
		if (!KEY.matches(event) || screen instanceof CreativeModeInventoryScreen) {
			return false;
		}
		SorterConfig config = SorterConfig.get();
		AbstractContainerMenu menu = screen.getMenu();
		Section container = Section.container(menu);
		Section player = Section.player(menu, config.includeHotbar);

		Section target = switch (config.hotkeyTarget) {
			case CONTAINER -> container != null ? container : player;
			case PLAYER -> player;
			case UNDER_MOUSE -> {
				if (hoveredSlot == null) {
					yield container != null ? container : player;
				}
				if (hoveredSlot.container instanceof Inventory) {
					yield player;
				}
				// Over the container, or over some other slot (furnace fuel...) that has no section.
				yield container != null && container.slots().contains(hoveredSlot) ? container : null;
			}
		};
		if (target == null) {
			return false;
		}

		switch (config.hotkeyAction) {
			case SORT -> Sorter.sort(menu, target, Layout.PLAIN);
			case SORT_COLUMNS -> Sorter.sort(menu, target, Layout.COLUMNS);
			case SORT_ROWS -> Sorter.sort(menu, target, Layout.ROWS);
			case MOVE_ALL -> {
				if (container == null) {
					return false;
				}
				Sorter.moveAll(menu, target);
			}
		}
		return true;
	}

	/** The key's current binding, for example "R". */
	public static Component boundKeyName() {
		return KEY.getTranslatedKeyMessage();
	}
}
