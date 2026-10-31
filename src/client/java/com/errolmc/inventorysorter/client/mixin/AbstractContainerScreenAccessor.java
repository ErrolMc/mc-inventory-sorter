package com.errolmc.inventorysorter.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read access to the panel geometry, which the buttons are anchored to. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int inventorySorter$leftPos();

	@Accessor("topPos")
	int inventorySorter$topPos();

	@Accessor("imageWidth")
	int inventorySorter$imageWidth();

	@Accessor("titleLabelY")
	int inventorySorter$titleLabelY();

	@Accessor("inventoryLabelY")
	int inventorySorter$inventoryLabelY();
}
