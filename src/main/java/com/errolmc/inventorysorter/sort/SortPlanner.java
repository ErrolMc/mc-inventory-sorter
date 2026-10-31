package com.errolmc.inventorysorter.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plans a sort as a list of left clicks (vanilla PICKUP clicks) on container slots, so sorting works on any server.
 *
 * <p>The planner works on a model of the slots and replays every click on it with vanilla's rules:
 * an empty cursor picks up the whole stack, a stack on the cursor is placed into an empty slot, merged into a slot
 * holding the same item, or swapped with a slot holding a different item.
 *
 * <p>Some stacks (bundles) override clicks when another stack is on the cursor or in the slot. Those are only ever
 * picked up into an empty cursor or placed into an empty slot. If the container has no empty slot to do that with,
 * they stay where they are and everything else is sorted around them.
 */
public final class SortPlanner {
	private final Cell[] cells;
	private final int width;
	private final int height;
	private final int[] maxStack;
	private final boolean[] special;
	private final boolean[] locked;
	private final List<Integer> clicks = new ArrayList<>();
	private Cell cursor = Cell.EMPTY;

	/**
	 * @param current  the slots in row-major order ({@code width * height} cells)
	 * @param maxStack max stack size per type id
	 * @param special  per type id: true when the stack must not be clicked onto, or have something clicked onto it
	 * @param locked   per position: true when the slot must not be touched
	 */
	private SortPlanner(Cell[] current, int width, int height, int[] maxStack, boolean[] special, boolean[] locked) {
		if (current.length != width * height) {
			throw new IllegalArgumentException("expected " + width * height + " cells, got " + current.length);
		}
		this.cells = current.clone();
		this.width = width;
		this.height = height;
		this.maxStack = maxStack;
		this.special = special;
		this.locked = locked == null ? new boolean[current.length] : locked.clone();
	}

	public static Result plan(Cell[] current, int width, int height, int[] maxStack, boolean[] special, boolean[] locked, Layout layout) {
		return new SortPlanner(current, width, height, maxStack, special, locked).run(layout);
	}

	/**
	 * @param clicks positions to left-click, in order
	 * @param target the expected slot contents afterwards
	 */
	public record Result(List<Integer> clicks, Cell[] target) {
	}

	private Result run(Layout layout) {
		boolean hasEmpty = false;
		for (int i = 0; i < cells.length; i++) {
			if (!locked[i] && cells[i].isEmpty()) {
				hasEmpty = true;
				break;
			}
		}
		if (!hasEmpty) {
			// Without an empty slot, special stacks cannot be moved safely, so leave them in place.
			for (int i = 0; i < cells.length; i++) {
				if (!cells[i].isEmpty() && special[cells[i].type()]) {
					locked[i] = true;
				}
			}
		}

		mergePartialStacks();
		Cell[] target = buildTarget(layout);
		applyTarget(target);
		return new Result(List.copyOf(clicks), target);
	}

	// ------------------------------------------------------------------ phase 1: merge partial stacks

	private void mergePartialStacks() {
		for (int i = 0; i < cells.length; i++) {
			if (locked[i] || cells[i].isEmpty() || special[cells[i].type()]) {
				continue;
			}
			int type = cells[i].type();
			for (int j = i + 1; j < cells.length && cells[i].count() < max(type); j++) {
				if (locked[j] || cells[j].type() != type || cells[j].isEmpty()) {
					continue;
				}
				click(j);
				click(i);
				if (!cursor.isEmpty()) {
					click(j);
				}
			}
		}
	}

	// ------------------------------------------------------------------ phase 2: work out where everything goes

	private Cell[] buildTarget(Layout layout) {
		int types = maxStack.length;
		long[] totals = new long[types];
		List<List<Cell>> specialStacks = new ArrayList<>();
		for (int t = 0; t < types; t++) {
			specialStacks.add(new ArrayList<>());
		}
		for (int i = 0; i < cells.length; i++) {
			if (!locked[i] && !cells[i].isEmpty()) {
				int t = cells[i].type();
				totals[t] += cells[i].count();
				specialStacks.get(t).add(cells[i]);
			}
		}

		// One group of stacks per type, in type order: full stacks first, then the remainder.
		// Special stacks were not merged, so they keep their own counts.
		List<List<Cell>> groups = new ArrayList<>();
		for (int t = 0; t < types; t++) {
			if (totals[t] == 0) {
				continue;
			}
			List<Cell> group = new ArrayList<>();
			if (special[t]) {
				group.addAll(specialStacks.get(t));
				group.sort((x, y) -> Integer.compare(y.count(), x.count()));
			} else {
				int max = max(t);
				long left = totals[t];
				while (left > 0) {
					int n = (int) Math.min(left, max);
					group.add(new Cell(t, n));
					left -= n;
				}
			}
			groups.add(group);
		}

		Cell[] target = null;
		if (layout != Layout.PLAIN) {
			boolean byRows = layout == Layout.ROWS;
			target = place(groups, byRows, Grouping.STRICT);
			if (target == null) {
				target = place(groups, byRows, Grouping.RELAXED);
			}
		}
		if (target == null) {
			target = place(groups, true, Grouping.NONE);
		}
		if (target == null) {
			throw new IllegalStateException("stacks do not fit after merging");
		}
		return target;
	}

	private enum Grouping {
		/** Every type starts a new line. */
		STRICT,
		/** A type starts a new line only if it does not fit in what is left of the current line. */
		RELAXED,
		NONE
	}

	/** Returns the target slots, or null if the groups do not fit with this grouping. */
	private Cell[] place(List<List<Cell>> groups, boolean byRows, Grouping grouping) {
		int lines = byRows ? height : width;
		int lineLength = byRows ? width : height;

		Cell[] target = new Cell[cells.length];
		for (int i = 0; i < cells.length; i++) {
			target[i] = locked[i] ? cells[i] : Cell.EMPTY;
		}

		int line = 0;
		int offset = 0;
		boolean lineUsed = false;
		for (List<Cell> group : groups) {
			if (lineUsed && grouping != Grouping.NONE) {
				boolean newLine = grouping == Grouping.STRICT || freeFrom(byRows, line, offset, lineLength) < group.size();
				if (newLine) {
					line++;
					offset = 0;
					lineUsed = false;
				}
			}
			for (Cell stack : group) {
				int pos;
				while (true) {
					if (offset >= lineLength) {
						line++;
						offset = 0;
					}
					if (line >= lines) {
						return null;
					}
					pos = position(byRows, line, offset);
					offset++;
					if (!locked[pos]) {
						break;
					}
				}
				target[pos] = stack;
				lineUsed = true;
			}
		}
		return target;
	}

	private int freeFrom(boolean byRows, int line, int offset, int lineLength) {
		int free = 0;
		for (int o = offset; o < lineLength; o++) {
			if (!locked[position(byRows, line, o)]) {
				free++;
			}
		}
		return free;
	}

	private int position(boolean byRows, int line, int offset) {
		return byRows ? line * width + offset : offset * width + line;
	}

	// ------------------------------------------------------------------ phase 3: move stacks into place

	private void applyTarget(Cell[] target) {
		for (int k = 0; k < cells.length; k++) {
			if (locked[k] || cells[k].equals(target[k])) {
				continue;
			}
			if (target[k].isEmpty()) {
				// Something is here that belongs later; move it to an empty slot that still needs filling.
				int j = findUnfixed(k, target, Cell.EMPTY, false);
				click(k);
				click(j);
				continue;
			}
			int j = findUnfixed(k, target, target[k], true);
			exchange(k, j);
		}
	}

	/** Finds an unfixed position after {@code k} whose content equals {@code wanted} (or, for empty, that is empty and needs filling). */
	private int findUnfixed(int k, Cell[] target, Cell wanted, boolean exact) {
		for (int j = k + 1; j < cells.length; j++) {
			if (locked[j] || cells[j].equals(target[j])) {
				continue;
			}
			if (exact ? cells[j].equals(wanted) : cells[j].isEmpty()) {
				return j;
			}
		}
		throw new IllegalStateException("no slot holds " + wanted + " for position " + k);
	}

	/** Puts the stack at {@code j} into {@code k}, and whatever was in {@code k} into {@code j}. */
	private void exchange(int k, int j) {
		Cell a = cells[k];
		Cell b = cells[j];
		if (a.isEmpty()) {
			click(j);
			click(k);
			return;
		}
		if (!special[a.type()] && !special[b.type()]) {
			if (a.type() == b.type() && a.count() > b.count()) {
				// Same item, different counts: one of the two is full, so a merge leaves the counts swapped.
				// Pick up the full one first, otherwise the click onto the full slot does nothing.
				click(k);
				click(j);
				click(k);
			} else {
				click(j);
				click(k);
				click(j);
			}
			return;
		}
		// A special stack is involved, so go through an empty slot instead of swapping on the cursor.
		int e = findEmpty();
		click(k);
		click(e);
		click(j);
		click(k);
		click(e);
		click(j);
	}

	private int findEmpty() {
		for (int i = 0; i < cells.length; i++) {
			if (!locked[i] && cells[i].isEmpty()) {
				return i;
			}
		}
		throw new IllegalStateException("no empty slot to move a special stack through");
	}

	// ------------------------------------------------------------------ click model

	private int max(int type) {
		return Math.max(1, maxStack[type]);
	}

	/** Applies a vanilla left click on {@code pos} to the model and records it. */
	private void click(int pos) {
		Cell slot = cells[pos];
		if (cursor.isEmpty()) {
			if (slot.isEmpty()) {
				throw new IllegalStateException("click on empty slot " + pos + " with empty cursor");
			}
			cursor = slot;
			cells[pos] = Cell.EMPTY;
		} else if (slot.isEmpty()) {
			cells[pos] = cursor;
			cursor = Cell.EMPTY;
		} else if (slot.type() == cursor.type()) {
			int room = Math.max(0, max(slot.type()) - slot.count());
			int moved = Math.min(room, cursor.count());
			cells[pos] = new Cell(slot.type(), slot.count() + moved);
			int left = cursor.count() - moved;
			cursor = left == 0 ? Cell.EMPTY : new Cell(cursor.type(), left);
		} else {
			if (special[slot.type()] || special[cursor.type()]) {
				throw new IllegalStateException("planned an unsafe click on " + pos);
			}
			cells[pos] = cursor;
			cursor = slot;
		}
		clicks.add(pos);
	}

	@Override
	public String toString() {
		return Arrays.toString(cells) + " cursor=" + cursor;
	}
}
