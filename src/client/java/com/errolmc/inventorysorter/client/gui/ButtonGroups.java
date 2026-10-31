package com.errolmc.inventorysorter.client.gui;

import com.errolmc.inventorysorter.client.config.SorterConfig;
import com.errolmc.inventorysorter.client.mixin.AbstractContainerScreenAccessor;
import com.errolmc.inventorysorter.client.sort.Section;
import com.errolmc.inventorysorter.client.sort.Sorter;
import com.errolmc.inventorysorter.sort.Layout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Adds a row of buttons to the title row of each inventory section of a container screen. */
public final class ButtonGroups {
	/** Space between buttons. */
	private static final int PITCH = SortButton.SIZE + 2;
	/** Distance from the panel's right edge to the last button, lining it up with the slot grid. */
	private static final int RIGHT_MARGIN = 7;

	private ButtonGroups() {
	}

	public static void addTo(Screen screen) {
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || screen instanceof CreativeModeInventoryScreen) {
			return;
		}
		SorterConfig config = SorterConfig.get();
		AbstractContainerMenu menu = containerScreen.getMenu();
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) containerScreen;
		boolean storage = Section.isStorageMenu(menu);

		if (storage) {
			Section container = Section.container(menu);
			if (container != null) {
				add(screen, menu, container, panel, () -> panel.inventorySorter$titleLabelY(), true);
			}
		}

		boolean showPlayer = config.playerInventoryButtons
			&& (storage || screen instanceof InventoryScreen || (config.playerButtonsInOtherScreens && isPlainVanillaScreen(screen)));
		if (showPlayer) {
			Section player = Section.player(menu, config.includeHotbar);
			if (player != null) {
				add(screen, menu, player, panel, () -> panel.inventorySorter$inventoryLabelY(), storage);
			}
		}
	}

	/**
	 * Vanilla screens (crafting table, furnace, anvil...) keep the usual "Inventory" label row free. Modded screens and
	 * the beacon may put something there, so they are left alone.
	 */
	private static boolean isPlainVanillaScreen(Screen screen) {
		return screen.getClass().getName().startsWith("net.minecraft.") && !(screen instanceof BeaconScreen);
	}

	private static void add(Screen screen, AbstractContainerMenu menu, Section section, AbstractContainerScreenAccessor panel,
							IntSupplier labelY, boolean withMoveAll) {
		SorterConfig config = SorterConfig.get();
		boolean toContainer = section.kind() == Section.Kind.PLAYER;

		List<Spec> specs = new ArrayList<>();
		if (config.showSortButton) {
			specs.add(sortSpec(SortButton.Icon.SORT, "sort", Layout.PLAIN, menu, section));
		}
		if (config.showColumnsButton) {
			specs.add(sortSpec(SortButton.Icon.COLUMNS, "sort_columns", Layout.COLUMNS, menu, section));
		}
		if (config.showRowsButton) {
			specs.add(sortSpec(SortButton.Icon.ROWS, "sort_rows", Layout.ROWS, menu, section));
		}
		if (withMoveAll && config.showMoveAllButton) {
			Component tooltip = Component.translatable(toContainer ? "inventory-sorter.button.move_to_container" : "inventory-sorter.button.move_to_inventory");
			specs.add(new Spec(toContainer ? SortButton.Icon.MOVE_UP : SortButton.Icon.MOVE_DOWN,
				() -> Sorter.moveAll(menu, section), null, () -> tooltip));
		}

		// Right-aligned in the label row; positions are re-read every frame because the recipe book moves the panel.
		for (int i = 0; i < specs.size(); i++) {
			Spec spec = specs.get(i);
			int fromRight = specs.size() - i;
			IntSupplier x = () -> panel.inventorySorter$leftPos() + panel.inventorySorter$imageWidth() - RIGHT_MARGIN
				- fromRight * PITCH + 2 + SorterConfig.get().buttonOffsetX;
			IntSupplier y = () -> panel.inventorySorter$topPos() + labelY.getAsInt() - 1 + SorterConfig.get().buttonOffsetY;
			Screens.getWidgets(screen).add(new SortButton(spec.icon(), x, y, spec.onLeftClick(), spec.onRightClick(), spec.tooltip()));
		}
	}

	private static Spec sortSpec(SortButton.Icon icon, String key, Layout layout, AbstractContainerMenu menu, Section section) {
		Supplier<Component> tooltip = () -> {
			SorterConfig config = SorterConfig.get();
			Component mode = config.sortMode.displayName();
			if (config.reverseOrder) {
				mode = Component.translatable("inventory-sorter.sort_mode.reversed", mode);
			}
			return Component.translatable("inventory-sorter.button." + key)
				.append("\n")
				.append(Component.translatable("inventory-sorter.button.mode", mode).withStyle(ChatFormatting.GRAY))
				.append("\n")
				.append(Component.translatable("inventory-sorter.button.change_mode").withStyle(ChatFormatting.DARK_GRAY));
		};
		Runnable cycleMode = () -> {
			SorterConfig config = SorterConfig.get();
			config.sortMode = config.sortMode.next();
			SorterConfig.save();
		};
		return new Spec(icon, () -> Sorter.sort(menu, section, layout), cycleMode, tooltip);
	}

	private record Spec(SortButton.Icon icon, Runnable onLeftClick, Runnable onRightClick, Supplier<Component> tooltip) {
	}
}
