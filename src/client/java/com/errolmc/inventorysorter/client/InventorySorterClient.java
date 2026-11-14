package com.errolmc.inventorysorter.client;

import com.errolmc.inventorysorter.client.config.SorterConfig;
import com.errolmc.inventorysorter.client.gui.ButtonGroups;
import com.errolmc.inventorysorter.client.input.ShiftDrag;
import com.errolmc.inventorysorter.client.input.SortKey;
import com.errolmc.inventorysorter.client.sort.ClickQueue;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class InventorySorterClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SorterConfig.load();
		SortKey.register();
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			ButtonGroups.addTo(screen);
			ShiftDrag.register(screen);
		});
		ClientTickEvents.END_CLIENT_TICK.register(ClickQueue::tick);
	}
}
