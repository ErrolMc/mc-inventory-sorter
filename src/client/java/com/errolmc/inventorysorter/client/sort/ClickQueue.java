package com.errolmc.inventorysorter.client.sort;

import com.errolmc.inventorysorter.client.config.SorterConfig;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.jspecify.annotations.Nullable;

/** Sends planned slot clicks to the server, all at once or a few per tick. */
public final class ClickQueue {
	private record Click(int slotIndex, ContainerInput input) {
	}

	private static final ArrayDeque<Click> PENDING = new ArrayDeque<>();
	private static int containerId = -1;

	private ClickQueue() {
	}

	public static boolean isBusy() {
		return !PENDING.isEmpty();
	}

	/** Queues clicks for {@code menu} (slot indices into {@code menu.slots}) and sends as many as the config allows now. */
	public static void submit(AbstractContainerMenu menu, List<Integer> slotIndices, ContainerInput input) {
		if (isBusy() || slotIndices.isEmpty()) {
			return;
		}
		containerId = menu.containerId;
		for (int slotIndex : slotIndices) {
			PENDING.add(new Click(slotIndex, input));
		}
		tick(Minecraft.getInstance());
	}

	public static void tick(Minecraft minecraft) {
		if (PENDING.isEmpty()) {
			return;
		}
		AbstractContainerMenu menu = currentMenu(minecraft);
		if (menu == null) {
			PENDING.clear();
			return;
		}
		int budget = SorterConfig.get().clicksPerTick;
		int sent = 0;
		while (!PENDING.isEmpty() && (budget <= 0 || sent < budget)) {
			Click click = PENDING.poll();
			minecraft.gameMode.handleContainerInput(menu.containerId, click.slotIndex(), 0, click.input(), minecraft.player);
			sent++;
		}
	}

	/** The open menu, if it is still the one the clicks were planned for. */
	private static @Nullable AbstractContainerMenu currentMenu(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.gameMode == null) {
			return null;
		}
		AbstractContainerMenu menu = minecraft.player.containerMenu;
		return menu.containerId == containerId ? menu : null;
	}
}
