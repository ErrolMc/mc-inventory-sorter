package com.errolmc.inventorysorter.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Settings, stored as {@code config/inventory-sorter.json}. */
public final class SorterConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("inventory-sorter");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("inventory-sorter.json");

	private static SorterConfig instance = new SorterConfig();

	public SortMode sortMode = SortMode.ITEM_TYPE;
	public boolean reverseOrder = false;

	public boolean showSortButton = true;
	public boolean showColumnsButton = true;
	public boolean showRowsButton = true;
	public boolean showMoveAllButton = true;

	/** Buttons on the player inventory panel (chests, shulker boxes, the survival inventory...). */
	public boolean playerInventoryButtons = true;
	/** Player inventory buttons in other screens too (crafting table, furnace...). */
	public boolean playerButtonsInOtherScreens = true;
	/** Sorting and move-all also take the hotbar. */
	public boolean includeHotbar = false;

	/** What the sort key (R by default, rebindable in Controls) does, and to which panel. */
	public HotkeyAction hotkeyAction = HotkeyAction.SORT;
	public HotkeyTarget hotkeyTarget = HotkeyTarget.UNDER_MOUSE;

	/** 0 sends every click at once; otherwise at most this many clicks per tick, for servers that limit clicks. */
	public int clicksPerTick = 0;

	/** Nudges every button group, for resource packs or mods that move things around. */
	public int buttonOffsetX = 0;
	public int buttonOffsetY = 0;

	public static SorterConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			instance = new SorterConfig();
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			SorterConfig loaded = GSON.fromJson(reader, SorterConfig.class);
			instance = loaded != null ? loaded.sanitized() : new SorterConfig();
		} catch (IOException | JsonParseException e) {
			LOGGER.error("Could not read {}, using defaults", FILE, e);
			instance = new SorterConfig();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Could not write {}", FILE, e);
		}
	}

	private SorterConfig sanitized() {
		if (sortMode == null) {
			sortMode = SortMode.ITEM_TYPE;
		}
		if (hotkeyAction == null) {
			hotkeyAction = HotkeyAction.SORT;
		}
		if (hotkeyTarget == null) {
			hotkeyTarget = HotkeyTarget.UNDER_MOUSE;
		}
		clicksPerTick = Math.max(0, clicksPerTick);
		return this;
	}
}
