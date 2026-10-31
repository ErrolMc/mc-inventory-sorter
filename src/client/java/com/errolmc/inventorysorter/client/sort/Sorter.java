package com.errolmc.inventorysorter.client.sort;

import com.errolmc.inventorysorter.client.config.SortMode;
import com.errolmc.inventorysorter.client.config.SorterConfig;
import com.errolmc.inventorysorter.sort.Cell;
import com.errolmc.inventorysorter.sort.Layout;
import com.errolmc.inventorysorter.sort.SortPlanner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Turns a button press into slot clicks. */
public final class Sorter {
	private static final Logger LOGGER = LoggerFactory.getLogger("inventory-sorter");
	private static final Map<Class<?>, Boolean> OVERRIDES_CLICKS = new ConcurrentHashMap<>();

	private Sorter() {
	}

	public static void sort(AbstractContainerMenu menu, Section section, Layout layout) {
		Minecraft minecraft = Minecraft.getInstance();
		if (ClickQueue.isBusy() || !menu.getCarried().isEmpty() || minecraft.player == null) {
			return;
		}
		Player player = minecraft.player;
		List<Slot> slots = section.slots();
		int n = slots.size();

		// Group the stacks into item types (same item, same components), then number the types in sort order.
		List<ItemStack> kinds = new ArrayList<>();
		List<Long> totals = new ArrayList<>();
		int[] kindOf = new int[n];
		for (int i = 0; i < n; i++) {
			ItemStack stack = slots.get(i).getItem();
			kindOf[i] = -1;
			if (stack.isEmpty()) {
				continue;
			}
			int kind = indexOfKind(kinds, stack);
			if (kind < 0) {
				kind = kinds.size();
				kinds.add(stack);
				totals.add(0L);
			}
			kindOf[i] = kind;
			totals.set(kind, totals.get(kind) + stack.getCount());
		}

		Integer[] order = new Integer[kinds.size()];
		for (int k = 0; k < order.length; k++) {
			order[k] = k;
		}
		Comparator<Integer> byKind = comparator(SorterConfig.get().sortMode, kinds, totals);
		if (SorterConfig.get().reverseOrder) {
			byKind = byKind.reversed();
		}
		java.util.Arrays.sort(order, byKind);
		int[] typeOfKind = new int[kinds.size()];
		for (int t = 0; t < order.length; t++) {
			typeOfKind[order[t]] = t;
		}

		Cell[] cells = new Cell[n];
		int[] maxStack = new int[kinds.size()];
		boolean[] special = new boolean[kinds.size()];
		boolean[] locked = new boolean[n];
		for (int i = 0; i < n; i++) {
			Slot slot = slots.get(i);
			ItemStack stack = slot.getItem();
			if (kindOf[i] < 0) {
				cells[i] = Cell.EMPTY;
				locked[i] = !slot.isActive();
				continue;
			}
			int type = typeOfKind[kindOf[i]];
			cells[i] = new Cell(type, stack.getCount());
			int max = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
			maxStack[type] = Math.max(maxStack[type], Math.max(max, stack.getCount()));
			special[type] |= isSpecial(stack);
			locked[i] = !slot.isActive() || !slot.mayPickup(player) || !slot.mayPlace(stack);
		}

		List<Integer> clicks;
		try {
			clicks = SortPlanner.plan(cells, section.width(), section.height(), maxStack, special, locked, layout).clicks();
		} catch (IllegalStateException e) {
			LOGGER.error("Could not plan a sort of {} slots", n, e);
			return;
		}
		List<Integer> slotIndices = new ArrayList<>(clicks.size());
		for (int position : clicks) {
			slotIndices.add(slots.get(position).index);
		}
		ClickQueue.submit(menu, slotIndices, ContainerInput.PICKUP);
	}

	/** Shift-clicks every stack in the section, which moves it to the other side of the menu. */
	public static void moveAll(AbstractContainerMenu menu, Section section) {
		if (ClickQueue.isBusy() || !menu.getCarried().isEmpty()) {
			return;
		}
		List<Integer> slotIndices = new ArrayList<>();
		for (Slot slot : section.slots()) {
			if (slot.hasItem()) {
				slotIndices.add(slot.index);
			}
		}
		ClickQueue.submit(menu, slotIndices, ContainerInput.QUICK_MOVE);
	}

	private static int indexOfKind(List<ItemStack> kinds, ItemStack stack) {
		for (int k = 0; k < kinds.size(); k++) {
			if (ItemStack.isSameItemSameComponents(kinds.get(k), stack)) {
				return k;
			}
		}
		return -1;
	}

	private static Comparator<Integer> comparator(SortMode mode, List<ItemStack> kinds, List<Long> totals) {
		Comparator<Integer> registryOrder = Comparator.comparingInt(k -> BuiltInRegistries.ITEM.getId(kinds.get(k).getItem()));
		Comparator<Integer> name = Comparator.comparing(k -> kinds.get(k).getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
		Comparator<Integer> id = Comparator.comparing(k -> BuiltInRegistries.ITEM.getKey(kinds.get(k).getItem()).toString());
		Comparator<Integer> mod = Comparator.comparing(k -> BuiltInRegistries.ITEM.getKey(kinds.get(k).getItem()).getNamespace());
		// Same item with different components (enchanted books, potions...) still needs a stable order.
		Comparator<Integer> components = Comparator.comparing(k -> kinds.get(k).getComponentsPatch().toString());

		Comparator<Integer> primary = switch (mode) {
			case ITEM_TYPE -> registryOrder.thenComparing(name);
			case NAME -> name.thenComparing(registryOrder);
			case ITEM_ID -> id;
			case COUNT -> Comparator.<Integer>comparingLong(k -> -totals.get(k)).thenComparing(registryOrder);
			case MOD -> mod.thenComparing(registryOrder);
		};
		return primary.thenComparing(components);
	}

	/**
	 * Bundles (and modded items like them) do something else when a stack is clicked onto them, or they are clicked
	 * onto a stack, so the planner must not swap them on the cursor.
	 */
	private static boolean isSpecial(ItemStack stack) {
		return stack.has(DataComponents.BUNDLE_CONTENTS) || OVERRIDES_CLICKS.computeIfAbsent(stack.getItem().getClass(), Sorter::overridesClicks);
	}

	private static boolean overridesClicks(Class<?> itemClass) {
		try {
			Class<?> a = itemClass.getMethod("overrideStackedOnOther", ItemStack.class, Slot.class, ClickAction.class, Player.class).getDeclaringClass();
			Class<?> b = itemClass.getMethod("overrideOtherStackedOnMe", ItemStack.class, ItemStack.class, Slot.class, ClickAction.class, Player.class, SlotAccess.class).getDeclaringClass();
			return a != Item.class || b != Item.class;
		} catch (NoSuchMethodException e) {
			return true;
		}
	}
}
