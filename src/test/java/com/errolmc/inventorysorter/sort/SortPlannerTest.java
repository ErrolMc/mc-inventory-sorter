package com.errolmc.inventorysorter.sort;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SortPlannerTest {
	private static final int W = 9;
	private static final int H = 3;

	@Test
	void plainSortMergesAndOrders() {
		// types: 0 = stone (64), 1 = dirt (64), 2 = sword (1)
		int[] max = {64, 64, 1};
		boolean[] special = new boolean[3];
		Cell[] cells = empty(W * H);
		cells[0] = new Cell(1, 10);
		cells[4] = new Cell(0, 60);
		cells[5] = new Cell(2, 1);
		cells[8] = new Cell(0, 30);
		cells[20] = new Cell(1, 5);
		cells[26] = new Cell(2, 1);

		Cell[] after = run(cells, max, special, null, Layout.PLAIN);

		assertEquals(new Cell(0, 64), after[0]);
		assertEquals(new Cell(0, 26), after[1]);
		assertEquals(new Cell(1, 15), after[2]);
		assertEquals(new Cell(2, 1), after[3]);
		assertEquals(new Cell(2, 1), after[4]);
		for (int i = 5; i < after.length; i++) {
			assertTrue(after[i].isEmpty(), "slot " + i);
		}
	}

	@Test
	void rowsStartEachTypeOnANewRow() {
		int[] max = {64, 64, 64};
		Cell[] cells = empty(W * H);
		cells[3] = new Cell(2, 64);
		cells[7] = new Cell(0, 64);
		cells[12] = new Cell(1, 64);
		cells[13] = new Cell(0, 64);

		Cell[] after = run(cells, max, new boolean[3], null, Layout.ROWS);

		assertEquals(new Cell(0, 64), after[0]);
		assertEquals(new Cell(0, 64), after[1]);
		assertEquals(new Cell(1, 64), after[9]);
		assertEquals(new Cell(2, 64), after[18]);
	}

	@Test
	void columnsStartEachTypeOnANewColumn() {
		int[] max = {64, 64};
		Cell[] cells = empty(W * H);
		for (int i = 0; i < 4; i++) {
			cells[10 + i] = new Cell(0, 64);
		}
		cells[25] = new Cell(1, 64);

		Cell[] after = run(cells, max, new boolean[2], null, Layout.COLUMNS);

		// Type 0 fills column 0 (positions 0, 9, 18) then the top of column 1; type 1 starts column 2.
		assertEquals(new Cell(0, 64), after[0]);
		assertEquals(new Cell(0, 64), after[9]);
		assertEquals(new Cell(0, 64), after[18]);
		assertEquals(new Cell(0, 64), after[1]);
		assertEquals(new Cell(1, 64), after[2]);
	}

	@Test
	void groupingFallsBackWhenItDoesNotFit() {
		// Four types in three rows cannot each get a row; relaxed grouping packs them instead.
		int[] max = {64, 64, 64, 64};
		Cell[] cells = empty(W * H);
		for (int t = 0; t < 4; t++) {
			cells[t * 2] = new Cell(t, 64);
		}

		Cell[] after = run(cells, max, new boolean[4], null, Layout.ROWS);

		assertEquals(new Cell(0, 64), after[0]);
		assertEquals(new Cell(1, 64), after[1]);
		assertEquals(new Cell(2, 64), after[2]);
		assertEquals(new Cell(3, 64), after[3]);
	}

	@Test
	void columnsStillRunTopToBottomInAFullContainer() {
		// 3x3, completely full, four types: no room for every type to get a column, so they run on down the columns.
		int[] max = {64, 64, 64, 64};
		Cell[] cells = {
			new Cell(3, 64), new Cell(1, 64), new Cell(0, 64),
			new Cell(2, 64), new Cell(3, 64), new Cell(1, 64),
			new Cell(3, 64), new Cell(0, 64), new Cell(2, 64),
		};

		SortPlanner.Result result = SortPlanner.plan(cells, 3, 3, max, new boolean[4], null, Layout.COLUMNS);
		Cell[] after = replay(cells, result.clicks(), max, new boolean[4]);

		// Columns: 0 0 1 | 1 2 2 | 3 3 3, shown here row by row.
		assertArrayEquals(new Cell[] {
			new Cell(0, 64), new Cell(1, 64), new Cell(3, 64),
			new Cell(0, 64), new Cell(2, 64), new Cell(3, 64),
			new Cell(1, 64), new Cell(2, 64), new Cell(3, 64),
		}, after);
	}

	@Test
	void rowsUseSpareRoomToAvoidWrapping() {
		// 4x3 with two spare slots: type 1 moves to its own row, type 2 has to wrap because there is no room left.
		int[] max = {64, 64, 64, 64};
		Cell[] cells = empty(12);
		int[] types = {2, 0, 3, 2, 1, 0, 2, 1, 0, 2};
		for (int i = 0; i < types.length; i++) {
			cells[i] = new Cell(types[i], 64);
		}

		SortPlanner.Result result = SortPlanner.plan(cells, 4, 3, max, new boolean[4], null, Layout.ROWS);
		Cell[] after = replay(cells, result.clicks(), max, new boolean[4]);

		Cell e = Cell.EMPTY;
		Cell a = new Cell(0, 64);
		Cell b = new Cell(1, 64);
		Cell c = new Cell(2, 64);
		Cell d = new Cell(3, 64);
		assertArrayEquals(new Cell[] {
			a, a, a, e,
			b, b, c, c,
			c, c, d, e,
		}, after);
	}

	@Test
	void specialStacksAreNeverSwappedOnTheCursor() {
		// Type 1 is a bundle: the planner itself throws if a click would put something onto it.
		int[] max = {64, 1, 64};
		boolean[] special = {false, true, false};
		Cell[] cells = empty(W * H);
		cells[0] = new Cell(2, 64);
		cells[1] = new Cell(1, 1);
		cells[2] = new Cell(0, 64);

		Cell[] after = run(cells, max, special, null, Layout.PLAIN);

		assertEquals(new Cell(0, 64), after[0]);
		assertEquals(new Cell(1, 1), after[1]);
		assertEquals(new Cell(2, 64), after[2]);
	}

	@Test
	void specialStacksStayPutWhenThereIsNoEmptySlot() {
		int[] max = {64, 1, 64};
		boolean[] special = {false, true, false};
		Cell[] cells = new Cell[4];
		cells[0] = new Cell(1, 1);
		cells[1] = new Cell(2, 64);
		cells[2] = new Cell(0, 64);
		cells[3] = new Cell(2, 64);

		SortPlanner.Result result = SortPlanner.plan(cells, 4, 1, max, special, null, Layout.PLAIN);
		Cell[] after = replay(cells, result.clicks(), max, special);

		assertEquals(new Cell(1, 1), after[0]);
		assertEquals(new Cell(0, 64), after[1]);
		assertEquals(new Cell(2, 64), after[2]);
		assertEquals(new Cell(2, 64), after[3]);
	}

	@Test
	void lockedSlotsAreUntouched() {
		int[] max = {64, 64};
		boolean[] locked = new boolean[W * H];
		locked[0] = true;
		Cell[] cells = empty(W * H);
		cells[0] = new Cell(1, 3);
		cells[5] = new Cell(0, 64);

		SortPlanner.Result result = SortPlanner.plan(cells, W, H, max, new boolean[2], locked, Layout.PLAIN);
		for (int click : result.clicks()) {
			assertTrue(click != 0, "clicked the locked slot");
		}
		Cell[] after = replay(cells, result.clicks(), max, new boolean[2]);
		assertEquals(new Cell(1, 3), after[0]);
		assertEquals(new Cell(0, 64), after[1]);
	}

	@Test
	void alreadySortedNeedsNoClicks() {
		int[] max = {64, 64};
		Cell[] cells = empty(W * H);
		cells[0] = new Cell(0, 64);
		cells[1] = new Cell(1, 12);

		SortPlanner.Result result = SortPlanner.plan(cells, W, H, max, new boolean[2], null, Layout.PLAIN);
		assertEquals(List.of(), result.clicks());
	}

	@Test
	void randomContainersEndUpExactlyAtTheTarget() {
		Random random = new Random(1234);
		for (int round = 0; round < 2000; round++) {
			int width = 1 + random.nextInt(9);
			int height = 1 + random.nextInt(6);
			int types = 1 + random.nextInt(8);
			int[] max = new int[types];
			boolean[] special = new boolean[types];
			for (int t = 0; t < types; t++) {
				max[t] = switch (random.nextInt(3)) {
					case 0 -> 1;
					case 1 -> 16;
					default -> 64;
				};
				special[t] = max[t] == 1 && random.nextInt(3) == 0;
			}
			Cell[] cells = empty(width * height);
			boolean[] locked = new boolean[cells.length];
			for (int i = 0; i < cells.length; i++) {
				if (random.nextInt(3) > 0) {
					int t = random.nextInt(types);
					cells[i] = new Cell(t, 1 + random.nextInt(max[t]));
				}
				locked[i] = random.nextInt(20) == 0;
			}
			for (Layout layout : Layout.values()) {
				SortPlanner.Result result = SortPlanner.plan(cells, width, height, max, special, locked, layout);
				Cell[] after = replay(cells, result.clicks(), max, special);
				assertArrayEquals(result.target(), after, () -> "round " + Arrays.toString(cells) + " " + layout);
				assertEquals(totals(cells, types), totals(after, types));
				for (int i = 0; i < cells.length; i++) {
					if (locked[i]) {
						assertEquals(cells[i], after[i]);
					}
				}
				assertInOrder(after, width, height, special, locked, layout);
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	private static Cell[] run(Cell[] cells, int[] max, boolean[] special, boolean[] locked, Layout layout) {
		SortPlanner.Result result = SortPlanner.plan(cells, W, H, max, special, locked, layout);
		Cell[] after = replay(cells, result.clicks(), max, special);
		assertArrayEquals(result.target(), after);
		return after;
	}

	/**
	 * Read in the layout's direction (down the columns for {@link Layout#COLUMNS}, along the rows otherwise), the types
	 * never go backwards. Locked slots and special stacks are skipped, since special stacks can be pinned in place.
	 */
	private static void assertInOrder(Cell[] after, int width, int height, boolean[] special, boolean[] locked, Layout layout) {
		int last = -1;
		for (int n = 0; n < after.length; n++) {
			int pos = layout == Layout.COLUMNS ? (n % height) * width + n / height : n;
			Cell cell = after[pos];
			if (locked[pos] || cell.isEmpty() || special[cell.type()]) {
				continue;
			}
			int type = cell.type();
			assertTrue(type >= last, () -> layout + " out of order at " + pos + ": " + Arrays.toString(after));
			last = type;
		}
	}

	private static Cell[] empty(int n) {
		Cell[] cells = new Cell[n];
		Arrays.fill(cells, Cell.EMPTY);
		return cells;
	}

	private static List<Long> totals(Cell[] cells, int types) {
		Long[] totals = new Long[types];
		Arrays.fill(totals, 0L);
		for (Cell cell : cells) {
			if (!cell.isEmpty()) {
				totals[cell.type()] += cell.count();
			}
		}
		return List.of(totals);
	}

	/**
	 * An independent copy of vanilla's left-click rules, so the test does not just trust the planner's own model.
	 * Fails on any click that would trigger a bundle's special behaviour.
	 */
	private static Cell[] replay(Cell[] start, List<Integer> clicks, int[] max, boolean[] special) {
		Cell[] cells = start.clone();
		Cell cursor = Cell.EMPTY;
		for (int pos : clicks) {
			Cell slot = cells[pos];
			if (!cursor.isEmpty() && !slot.isEmpty() && (special[cursor.type()] || special[slot.type()])) {
				throw new AssertionError("click on " + pos + " would trigger a bundle");
			}
			if (cursor.isEmpty()) {
				cursor = slot;
				cells[pos] = Cell.EMPTY;
			} else if (slot.isEmpty()) {
				cells[pos] = cursor;
				cursor = Cell.EMPTY;
			} else if (slot.type() == cursor.type()) {
				int moved = Math.min(cursor.count(), Math.max(0, max[slot.type()] - slot.count()));
				cells[pos] = new Cell(slot.type(), slot.count() + moved);
				cursor = cursor.count() == moved ? Cell.EMPTY : new Cell(cursor.type(), cursor.count() - moved);
			} else {
				cells[pos] = cursor;
				cursor = slot;
			}
		}
		assertTrue(cursor.isEmpty(), "cursor left holding " + cursor);
		return cells;
	}
}
