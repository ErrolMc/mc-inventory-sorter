package com.errolmc.inventorysorter.client.config;

import com.errolmc.inventorysorter.client.input.SortKey;
import java.util.Arrays;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The Cloth Config settings screen. Only loaded when Cloth Config is installed. */
final class ConfigScreen {
	private ConfigScreen() {
	}

	static Screen create(Screen parent) {
		SorterConfig config = SorterConfig.get();
		SorterConfig defaults = new SorterConfig();

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("inventory-sorter.config.title"))
			.setSavingRunnable(SorterConfig::save);
		ConfigEntryBuilder entries = builder.entryBuilder();

		ConfigCategory sorting = builder.getOrCreateCategory(Component.translatable("inventory-sorter.config.category.sorting"));
		sorting.addEntry(entries.startEnumSelector(text("sort_mode"), SortMode.class, config.sortMode)
			.setDefaultValue(defaults.sortMode)
			.setEnumNameProvider(mode -> ((SortMode) mode).displayName())
			.setTooltip(tooltip("sort_mode"))
			.setSaveConsumer(value -> config.sortMode = value)
			.build());
		sorting.addEntry(entries.startBooleanToggle(text("reverse_order"), config.reverseOrder)
			.setDefaultValue(defaults.reverseOrder)
			.setTooltip(tooltip("reverse_order"))
			.setSaveConsumer(value -> config.reverseOrder = value)
			.build());
		sorting.addEntry(entries.startBooleanToggle(text("include_hotbar"), config.includeHotbar)
			.setDefaultValue(defaults.includeHotbar)
			.setTooltip(tooltip("include_hotbar"))
			.setSaveConsumer(value -> config.includeHotbar = value)
			.build());
		sorting.addEntry(entries.startIntSlider(text("clicks_per_tick"), config.clicksPerTick, 0, 64)
			.setDefaultValue(defaults.clicksPerTick)
			.setTextGetter(value -> value == 0
				? Component.translatable("inventory-sorter.config.clicks_per_tick.instant")
				: Component.literal(String.valueOf(value)))
			.setTooltip(tooltip("clicks_per_tick"))
			.setSaveConsumer(value -> config.clicksPerTick = value)
			.build());

		ConfigCategory hotkey = builder.getOrCreateCategory(Component.translatable("inventory-sorter.config.category.hotkey"));
		hotkey.addEntry(entries.startTextDescription(Component.translatable("inventory-sorter.config.hotkey_info", SortKey.boundKeyName())).build());
		hotkey.addEntry(entries.startEnumSelector(text("hotkey_action"), HotkeyAction.class, config.hotkeyAction)
			.setDefaultValue(defaults.hotkeyAction)
			.setEnumNameProvider(action -> ((HotkeyAction) action).displayName())
			.setTooltip(tooltip("hotkey_action"))
			.setSaveConsumer(value -> config.hotkeyAction = value)
			.build());
		hotkey.addEntry(entries.startEnumSelector(text("hotkey_target"), HotkeyTarget.class, config.hotkeyTarget)
			.setDefaultValue(defaults.hotkeyTarget)
			.setEnumNameProvider(target -> ((HotkeyTarget) target).displayName())
			.setTooltip(tooltip("hotkey_target"))
			.setSaveConsumer(value -> config.hotkeyTarget = value)
			.build());

		ConfigCategory buttons = builder.getOrCreateCategory(Component.translatable("inventory-sorter.config.category.buttons"));
		buttons.addEntry(entries.startBooleanToggle(text("show_sort_button"), config.showSortButton)
			.setDefaultValue(defaults.showSortButton)
			.setSaveConsumer(value -> config.showSortButton = value)
			.build());
		buttons.addEntry(entries.startBooleanToggle(text("show_columns_button"), config.showColumnsButton)
			.setDefaultValue(defaults.showColumnsButton)
			.setSaveConsumer(value -> config.showColumnsButton = value)
			.build());
		buttons.addEntry(entries.startBooleanToggle(text("show_rows_button"), config.showRowsButton)
			.setDefaultValue(defaults.showRowsButton)
			.setSaveConsumer(value -> config.showRowsButton = value)
			.build());
		buttons.addEntry(entries.startBooleanToggle(text("show_move_all_button"), config.showMoveAllButton)
			.setDefaultValue(defaults.showMoveAllButton)
			.setSaveConsumer(value -> config.showMoveAllButton = value)
			.build());
		buttons.addEntry(entries.startBooleanToggle(text("player_inventory_buttons"), config.playerInventoryButtons)
			.setDefaultValue(defaults.playerInventoryButtons)
			.setTooltip(tooltip("player_inventory_buttons"))
			.setSaveConsumer(value -> config.playerInventoryButtons = value)
			.build());
		buttons.addEntry(entries.startBooleanToggle(text("player_buttons_in_other_screens"), config.playerButtonsInOtherScreens)
			.setDefaultValue(defaults.playerButtonsInOtherScreens)
			.setTooltip(tooltip("player_buttons_in_other_screens"))
			.setSaveConsumer(value -> config.playerButtonsInOtherScreens = value)
			.build());
		buttons.addEntry(entries.startIntSlider(text("button_offset_x"), config.buttonOffsetX, -100, 100)
			.setDefaultValue(defaults.buttonOffsetX)
			.setSaveConsumer(value -> config.buttonOffsetX = value)
			.build());
		buttons.addEntry(entries.startIntSlider(text("button_offset_y"), config.buttonOffsetY, -100, 100)
			.setDefaultValue(defaults.buttonOffsetY)
			.setSaveConsumer(value -> config.buttonOffsetY = value)
			.build());

		return builder.build();
	}

	private static Component text(String key) {
		return Component.translatable("inventory-sorter.config." + key);
	}

	/** Cloth Config does not wrap tooltips, so the translations put a newline where each line should end. */
	private static Component[] tooltip(String key) {
		String text = Component.translatable("inventory-sorter.config." + key + ".tooltip").getString();
		return Arrays.stream(text.split("\n")).map(Component::literal).toArray(Component[]::new);
	}
}
