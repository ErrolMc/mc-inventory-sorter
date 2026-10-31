package com.errolmc.inventorysorter.client.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * A rectangular block of slots in a menu that gets its own buttons: the storage container, or the player's inventory.
 *
 * @param slots the slots in row-major order ({@code width * height} of them)
 */
public record Section(Kind kind, List<Slot> slots, int width, int height) {
	public enum Kind {
		CONTAINER,
		PLAYER
	}

	private static final int PLAYER_MAIN_START = 9;
	private static final int PLAYER_MAIN_END = 36;
	private static final int HOTBAR_SIZE = 9;

	/** True for the menus whose non-player slots are plain storage: chests, barrels, ender chests, shulker boxes... */
	public static boolean isStorageMenu(AbstractContainerMenu menu) {
		return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof DispenserMenu || menu instanceof HopperMenu;
	}

	/** The storage slots of a storage menu, or null if the menu is not one or its slots are not a grid. */
	public static @Nullable Section container(AbstractContainerMenu menu) {
		if (!isStorageMenu(menu)) {
			return null;
		}
		List<Slot> slots = new ArrayList<>();
		for (Slot slot : menu.slots) {
			if (!(slot.container instanceof Inventory)) {
				slots.add(slot);
			}
		}
		return grid(Kind.CONTAINER, slots);
	}

	/** The player's main inventory (plus the hotbar as a fourth row when asked), or null if the menu does not show it. */
	public static @Nullable Section player(AbstractContainerMenu menu, boolean includeHotbar) {
		Slot[] main = new Slot[PLAYER_MAIN_END - PLAYER_MAIN_START];
		Slot[] hotbar = new Slot[HOTBAR_SIZE];
		for (Slot slot : menu.slots) {
			if (!(slot.container instanceof Inventory)) {
				continue;
			}
			int index = slot.getContainerSlot();
			if (index >= PLAYER_MAIN_START && index < PLAYER_MAIN_END) {
				main[index - PLAYER_MAIN_START] = slot;
			} else if (index >= 0 && index < HOTBAR_SIZE) {
				hotbar[index] = slot;
			}
		}
		List<Slot> slots = new ArrayList<>();
		for (Slot slot : main) {
			if (slot == null) {
				return null;
			}
			slots.add(slot);
		}
		int rows = main.length / HOTBAR_SIZE;
		if (includeHotbar && allPresent(hotbar)) {
			slots.addAll(List.of(hotbar));
			rows++;
		}
		return new Section(Kind.PLAYER, slots, HOTBAR_SIZE, rows);
	}

	private static boolean allPresent(Slot[] slots) {
		for (Slot slot : slots) {
			if (slot == null) {
				return false;
			}
		}
		return true;
	}

	/** Orders slots by their on-screen position and checks they form a full grid. */
	private static @Nullable Section grid(Kind kind, List<Slot> slots) {
		if (slots.isEmpty()) {
			return null;
		}
		TreeSet<Integer> xs = new TreeSet<>();
		TreeSet<Integer> ys = new TreeSet<>();
		for (Slot slot : slots) {
			xs.add(slot.x);
			ys.add(slot.y);
		}
		if (xs.size() * ys.size() != slots.size()) {
			return null;
		}
		List<Slot> ordered = new ArrayList<>(slots);
		ordered.sort(Comparator.<Slot>comparingInt(s -> s.y).thenComparingInt(s -> s.x));
		return new Section(kind, List.copyOf(ordered), xs.size(), ys.size());
	}
}
