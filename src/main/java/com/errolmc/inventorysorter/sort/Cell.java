package com.errolmc.inventorysorter.sort;

/**
 * One slot in the planner's model of a container.
 *
 * @param type  item type id, or {@link #EMPTY_TYPE}; ids are assigned in sort order, so type 0 sorts first
 * @param count stack size (0 when empty)
 */
public record Cell(int type, int count) {
	public static final int EMPTY_TYPE = -1;
	public static final Cell EMPTY = new Cell(EMPTY_TYPE, 0);

	public boolean isEmpty() {
		return type == EMPTY_TYPE || count <= 0;
	}
}
