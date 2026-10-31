package com.errolmc.inventorysorter.client.mixin;

import com.errolmc.inventorysorter.client.input.SortKey;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Handles the sort key where vanilla handles the inventory key: after focused widgets (text fields) had their turn. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected @Nullable Slot hoveredSlot;

	@Inject(
		method = "keyPressed",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyInventory:Lnet/minecraft/client/KeyMapping;", opcode = Opcodes.GETFIELD, ordinal = 0),
		cancellable = true
	)
	private void inventorySorter$sortKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (SortKey.handle((AbstractContainerScreen<?>) (Object) this, hoveredSlot, event)) {
			cir.setReturnValue(true);
		}
	}
}
