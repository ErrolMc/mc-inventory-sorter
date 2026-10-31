package com.errolmc.inventorysorter.client.gui;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

/** A small bevelled square with a pixel icon, placed in a panel's title row. */
public class SortButton extends AbstractButton {
	public static final int SIZE = 10;

	private static final int BODY = 0xFF6F6F6F;
	private static final int LIGHT_EDGE = 0xFF9A9A9A;
	private static final int DARK_EDGE = 0xFF3F3F3F;
	private static final int BODY_HOVER = 0xFF6A78C8;
	private static final int LIGHT_EDGE_HOVER = 0xFFA3AEF0;
	private static final int DARK_EDGE_HOVER = 0xFF3B4385;
	private static final int ICON = 0xFFDDDDDD;
	private static final int ICON_HOVER = 0xFFFFFFFF;

	private final Icon icon;
	private final IntSupplier x;
	private final IntSupplier y;
	private final Runnable onLeftClick;
	private final Runnable onRightClick;
	private final Supplier<Component> tooltip;
	private Component lastTooltip;

	/**
	 * @param x            screen x, read every frame so the button follows the panel (the recipe book moves it)
	 * @param onRightClick may be null
	 * @param tooltip      read every frame, so it can show the current setting
	 */
	public SortButton(Icon icon, IntSupplier x, IntSupplier y, Runnable onLeftClick, Runnable onRightClick, Supplier<Component> tooltip) {
		super(x.getAsInt(), y.getAsInt(), SIZE, SIZE, tooltip.get());
		this.icon = icon;
		this.x = x;
		this.y = y;
		this.onLeftClick = onLeftClick;
		this.onRightClick = onRightClick;
		this.tooltip = tooltip;
		updateTooltip();
	}

	public Icon icon() {
		return icon;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		onLeftClick.run();
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && onRightClick != null) {
			onRightClick.run();
		} else {
			onLeftClick.run();
		}
	}

	@Override
	protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
		return buttonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT
			|| (onRightClick != null && buttonInfo.button() == InputConstants.MOUSE_BUTTON_RIGHT);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		setPosition(x.getAsInt(), y.getAsInt());
		updateTooltip();

		// Not isHoveredOrFocused(): a clicked button keeps focus and would stay highlighted.
		boolean hovered = isHovered();
		int x0 = getX();
		int y0 = getY();
		int x1 = x0 + SIZE;
		int y1 = y0 + SIZE;
		graphics.fill(x0, y0, x1, y1, hovered ? BODY_HOVER : BODY);
		graphics.fill(x0, y0, x1 - 1, y0 + 1, hovered ? LIGHT_EDGE_HOVER : LIGHT_EDGE);
		graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, hovered ? LIGHT_EDGE_HOVER : LIGHT_EDGE);
		graphics.fill(x0 + 1, y1 - 1, x1, y1, hovered ? DARK_EDGE_HOVER : DARK_EDGE);
		graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, hovered ? DARK_EDGE_HOVER : DARK_EDGE);
		icon.draw(graphics, x0 + 2, y0 + 2, hovered ? ICON_HOVER : ICON);
	}

	private void updateTooltip() {
		Component current = tooltip.get();
		if (!current.equals(lastTooltip)) {
			lastTooltip = current;
			setMessage(current);
			setTooltip(Tooltip.create(current));
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}

	/** 6x6 pixel glyphs, drawn as runs of filled pixels. */
	public enum Icon {
		SORT(
			"######",
			"....#.",
			"...#..",
			"..#...",
			".#....",
			"######"),
		COLUMNS(
			".#..#.",
			".#..#.",
			".#..#.",
			".#..#.",
			".#..#.",
			".#..#."),
		ROWS(
			"......",
			"######",
			"......",
			"......",
			"######",
			"......"),
		/** Arrow up onto a tray: from the player inventory into the container above it. */
		MOVE_UP(
			"######",
			"......",
			"..##..",
			".####.",
			"..##..",
			"..##.."),
		/** Arrow down onto a tray: from the container into the player inventory below it. */
		MOVE_DOWN(
			"..##..",
			"..##..",
			".####.",
			"..##..",
			"......",
			"######");

		private final String[] rows;

		Icon(String... rows) {
			this.rows = rows;
		}

		void draw(GuiGraphicsExtractor graphics, int x, int y, int color) {
			for (int row = 0; row < rows.length; row++) {
				String line = rows[row];
				int col = 0;
				while (col < line.length()) {
					if (line.charAt(col) != '#') {
						col++;
						continue;
					}
					int start = col;
					while (col < line.length() && line.charAt(col) == '#') {
						col++;
					}
					graphics.fill(x + start, y + row, x + col, y + row + 1, color);
				}
			}
		}
	}
}
