package com.errolmc.inventorysorter.sort;

/** How sorted stacks are laid out over a container grid. */
public enum Layout {
	/** Fill row by row, one stack after another. */
	PLAIN,
	/** Fill row by row, starting each item type on a new row when there is room. */
	ROWS,
	/** Fill column by column, starting each item type on a new column when there is room. */
	COLUMNS
}
