package com.errolmc.inventorysorter.client.input;

import com.errolmc.inventorysorter.client.config.SorterConfig;
import com.errolmc.inventorysorter.client.mixin.AbstractContainerScreenAccessor;
import com.errolmc.inventorysorter.client.sort.ClickQueue;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * Shift + left-drag: every slot the mouse passes over is shift-clicked, so its items move to the other inventory.
 *
 * <p>Vanilla shift-clicks the slot the drag starts on. The drag then only takes slots from the same inventory as that
 * slot, so items that have just arrived in the other inventory are not sent straight back.
 */
public final class ShiftDrag {
	/** Distance in GUI pixels between the points checked along the mouse's path; slots are 18 pixels apart. */
	private static final double STEP = 2;

	private static @Nullable Screen screen;
	private static @Nullable Container source;
	private static final Set<Slot> VISITED = Collections.newSetFromMap(new IdentityHashMap<>());
	private static double lastX;
	private static double lastY;

	private ShiftDrag() {
	}

	/** Hooks the mouse events of a newly initialised screen. */
	public static void register(Screen newScreen) {
		if (!(newScreen instanceof AbstractContainerScreen<?> containerScreen) || newScreen instanceof CreativeModeInventoryScreen) {
			return;
		}
		ScreenMouseEvents.afterMouseClick(newScreen).register((s, event, consumed) -> {
			start(containerScreen, event);
			return consumed;
		});
		ScreenMouseEvents.afterMouseDrag(newScreen).register((s, event, dx, dy, consumed) -> {
			drag(containerScreen, event);
			return consumed;
		});
		ScreenMouseEvents.afterMouseRelease(newScreen).register((s, event, consumed) -> {
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				stop();
			}
			return consumed;
		});
	}

	private static void start(AbstractContainerScreen<?> containerScreen, MouseButtonEvent event) {
		stop();
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !SorterConfig.get().shiftDrag) {
			return;
		}
		Slot slot = slotAt(containerScreen, event.x(), event.y());
		// The cursor is still empty after vanilla's own shift-click; with an item on it, this is a vanilla drag instead.
		if (slot == null || !Minecraft.getInstance().hasShiftDown() || !containerScreen.getMenu().getCarried().isEmpty()) {
			return;
		}
		screen = containerScreen;
		source = slot.container;
		VISITED.add(slot);
		lastX = event.x();
		lastY = event.y();
	}

	private static void drag(AbstractContainerScreen<?> containerScreen, MouseButtonEvent event) {
		if (screen != containerScreen || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		boolean canMove = minecraft.hasShiftDown() && player != null && minecraft.gameMode != null
			&& containerScreen.getMenu().getCarried().isEmpty() && !ClickQueue.isBusy();
		if (canMove) {
			// Check points along the path since the last event, so a quick flick of the mouse doesn't skip slots.
			double dx = event.x() - lastX;
			double dy = event.y() - lastY;
			int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy) / STEP));
			for (int i = 1; i <= steps; i++) {
				Slot slot = slotAt(containerScreen, lastX + dx * i / steps, lastY + dy * i / steps);
				if (slot != null && slot.container == source && VISITED.add(slot) && movable(slot, player)) {
					minecraft.gameMode.handleContainerInput(containerScreen.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE, player);
				}
			}
		}
		lastX = event.x();
		lastY = event.y();
	}

	private static void stop() {
		screen = null;
		source = null;
		VISITED.clear();
	}

	/** Output slots (crafting results, furnace output...) are left alone: dragging over them would craft or take everything. */
	private static boolean movable(Slot slot, Player player) {
		return slot.hasItem() && slot.mayPickup(player) && slot.mayPlace(slot.getItem());
	}

	/** The slot under a point, with the same one-pixel margin vanilla uses for hovering. */
	private static @Nullable Slot slotAt(AbstractContainerScreen<?> containerScreen, double x, double y) {
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) containerScreen;
		double px = x - accessor.inventorySorter$leftPos();
		double py = y - accessor.inventorySorter$topPos();
		for (Slot slot : containerScreen.getMenu().slots) {
			if (slot.isActive() && px >= slot.x - 1 && px < slot.x + 17 && py >= slot.y - 1 && py < slot.y + 17) {
				return slot;
			}
		}
		return null;
	}
}
