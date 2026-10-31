package com.errolmc.inventorysorter.gametest;

import com.errolmc.inventorysorter.client.config.ModMenuIntegration;
import com.errolmc.inventorysorter.client.config.SortMode;
import com.errolmc.inventorysorter.client.config.SorterConfig;
import com.errolmc.inventorysorter.client.gui.SortButton;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Opens real containers in a test world, clicks the buttons with the mouse and checks the server-side result.
 * Screenshots land in build/run/clientGameTest/screenshots.
 */
public class InventorySorterClientGameTest implements FabricClientGameTest {
	private static final int CONTAINER_SECTION = 0;
	private static final int PLAYER_SECTION = 1;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode survival @a");
			BlockPos base = world.getServer().computeOnServer(server -> player(server).blockPosition()).offset(2, 0, 0);

			singleChest(context, world, base);
			doubleChest(context, world, base.offset(0, 0, 3));
			barrel(context, world, base.offset(0, 0, -3));
			shulkerBox(context, world, base.offset(3, 0, 0));
			enderChest(context, world);
			playerInventory(context, world);
		}
	}

	// ------------------------------------------------------------------ scenarios

	private void singleChest(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
		setBlock(world, pos, "minecraft:chest");
		fill(world, pos, Map.of(
			0, "minecraft:dirt 10",
			3, "minecraft:stone 30",
			5, "minecraft:dirt 20",
			8, "minecraft:diamond_sword",
			12, "minecraft:stone 50",
			14, "minecraft:bundle",
			20, "minecraft:apple 5",
			22, "minecraft:oak_log 64",
			26, "minecraft:stone 60"));
		open(context, world, server -> level(server).getBlockState(pos).getMenuProvider(level(server), pos), ContainerScreen.class);
		List<ItemStack> before = containerItems(world);

		List<ButtonInfo> buttons = buttons(context);
		check(buttons.size() == 8, "chest should have 4 container + 4 player buttons, has " + buttons.size());
		hover(context, button(buttons, CONTAINER_SECTION, SortButton.Icon.ROWS));
		context.takeScreenshot("chest-hover-rows");

		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		List<ItemStack> sorted = containerItems(world);
		checkSameItems(before, sorted, "single chest sort");
		checkPlainSorted(sorted, "single chest sort");
		for (ItemStack stack : sorted) {
			if (stack.is(net.minecraft.world.item.Items.BUNDLE)) {
				check(stack.get(DataComponents.BUNDLE_CONTENTS).isEmpty(), "bundle picked up items while sorting");
			}
		}
		context.takeScreenshot("chest-sorted");

		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.COLUMNS));
		List<ItemStack> columns = containerItems(world);
		checkSameItems(before, columns, "single chest columns");
		checkLinesStartFresh(columns, 9, 3, false, "single chest columns");
		context.takeScreenshot("chest-columns");

		// Move everything down into the player inventory and back up again. Shift-click fills the hotbar first, and
		// the player-side Move All leaves the hotbar alone, so fill the hotbar with something else beforehand.
		for (int i = 0; i < 9; i++) {
			world.getServer().runCommand("item replace entity @a hotbar." + i + " with minecraft:feather");
		}
		context.waitTicks(2);
		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.MOVE_DOWN));
		check(containerItems(world).stream().allMatch(ItemStack::isEmpty), "move all to inventory left items in the chest");
		checkSameItems(before, playerMain(world), "move all to inventory");
		click(context, world, button(buttons, PLAYER_SECTION, SortButton.Icon.MOVE_UP));
		checkSameItems(before, containerItems(world), "move all back to the chest");
		check(playerMain(world).stream().allMatch(ItemStack::isEmpty), "move all to container left items in the inventory");
		close(context, world);
	}

	private void doubleChest(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
		world.getServer().runCommand("setblock " + xyz(pos) + " minecraft:chest[facing=north,type=left]");
		world.getServer().runCommand("setblock " + xyz(pos.east()) + " minecraft:chest[facing=north,type=right]");
		Map<Integer, String> items = new HashMap<>();
		items.put(1, "minecraft:cobblestone 64");
		items.put(4, "minecraft:cobblestone 12");
		items.put(9, "minecraft:torch 40");
		items.put(13, "minecraft:iron_ingot 20");
		items.put(17, "minecraft:cobblestone 64");
		items.put(25, "minecraft:torch 30");
		fill(world, pos, items);
		fill(world, pos.east(), Map.of(
			2, "minecraft:iron_ingot 7",
			6, "minecraft:bread 16",
			11, "minecraft:oak_planks 64",
			19, "minecraft:bread 3"));
		open(context, world, server -> level(server).getBlockState(pos).getMenuProvider(level(server), pos), ContainerScreen.class);
		check(containerItems(world).size() == 54, "double chest should have 54 slots");
		List<ItemStack> before = containerItems(world);

		List<ButtonInfo> buttons = buttons(context);
		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.ROWS));
		List<ItemStack> rows = containerItems(world);
		checkSameItems(before, rows, "double chest rows");
		checkLinesStartFresh(rows, 9, 6, true, "double chest rows");
		hover(context, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		context.takeScreenshot("double-chest-rows");
		close(context, world);
	}

	private void barrel(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
		setBlock(world, pos, "minecraft:barrel");
		fill(world, pos, Map.of(4, "minecraft:sand 33", 7, "minecraft:gravel 5", 18, "minecraft:sand 40"));
		open(context, world, server -> level(server).getBlockState(pos).getMenuProvider(level(server), pos), ContainerScreen.class);
		List<ItemStack> before = containerItems(world);
		List<ButtonInfo> buttons = buttons(context);
		check(buttons.size() == 8, "barrel should have 8 buttons, has " + buttons.size());
		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		checkSameItems(before, containerItems(world), "barrel sort");
		checkPlainSorted(containerItems(world), "barrel sort");
		close(context, world);
	}

	private void shulkerBox(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
		setBlock(world, pos, "minecraft:shulker_box");
		fill(world, pos, Map.of(2, "minecraft:redstone 10", 10, "minecraft:glass 20", 16, "minecraft:redstone 50"));
		open(context, world, server -> level(server).getBlockState(pos).getMenuProvider(level(server), pos), ShulkerBoxScreen.class);
		List<ItemStack> before = containerItems(world);
		List<ButtonInfo> buttons = buttons(context);
		check(buttons.size() == 8, "shulker box should have 8 buttons, has " + buttons.size());
		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		checkSameItems(before, containerItems(world), "shulker box sort");
		checkPlainSorted(containerItems(world), "shulker box sort");
		context.takeScreenshot("shulker-box-sorted");
		close(context, world);
	}

	private void enderChest(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getEnderChestInventory().setItem(5, new ItemStack(net.minecraft.world.item.Items.EMERALD, 9));
			player.getEnderChestInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.COAL, 12));
		});
		open(context, world, server -> new SimpleMenuProvider(
			(id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
			Component.translatable("container.enderchest")), ContainerScreen.class);
		List<ItemStack> before = containerItems(world);
		List<ButtonInfo> buttons = buttons(context);
		check(buttons.size() == 8, "ender chest should have 8 buttons, has " + buttons.size());
		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		checkSameItems(before, containerItems(world), "ender chest sort");
		checkPlainSorted(containerItems(world), "ender chest sort");
		close(context, world);
	}

	private void playerInventory(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("clear @a");
		// inventory.0 to inventory.26 are the 27 main slots above the hotbar.
		world.getServer().runCommand("item replace entity @a inventory.3 with minecraft:arrow 20");
		world.getServer().runCommand("item replace entity @a inventory.9 with minecraft:bone 5");
		world.getServer().runCommand("item replace entity @a inventory.15 with minecraft:arrow 50");
		world.getServer().runCommand("item replace entity @a inventory.26 with minecraft:string 7");
		world.getServer().runCommand("item replace entity @a hotbar.4 with minecraft:torch 3");
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(5);

		List<ButtonInfo> buttons = buttons(context);
		check(buttons.size() == 3, "survival inventory should have 3 sort buttons, has " + buttons.size());
		List<ItemStack> before = playerMain(world);
		hover(context, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		context.takeScreenshot("player-inventory-hover-sort");
		click(context, world, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		List<ItemStack> after = playerMain(world);
		checkSameItems(before, after, "player inventory sort");
		checkPlainSorted(after, "player inventory sort");
		ItemStack hotbar = world.getServer().computeOnServer(server -> player(server).getInventory().getItem(4).copy());
		check(hotbar.is(net.minecraft.world.item.Items.TORCH) && hotbar.getCount() == 3, "hotbar was touched");

		// Right-click cycles the sort mode.
		SortMode mode = SorterConfig.get().sortMode;
		hover(context, button(buttons, CONTAINER_SECTION, SortButton.Icon.SORT));
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
		context.waitTicks(2);
		check(SorterConfig.get().sortMode == mode.next(), "right-click did not change the sort mode");
		SorterConfig.get().sortMode = mode;
		SorterConfig.save();
		context.setScreen(() -> null);

		// The Mod Menu "Configure" screen (Cloth Config).
		context.setScreen(() -> new ModMenuIntegration().getModConfigScreenFactory().create(null));
		context.waitTicks(5);
		context.takeScreenshot("config-screen");
		context.setScreen(() -> null);
	}

	// ------------------------------------------------------------------ checks

	private static void checkSameItems(List<ItemStack> before, List<ItemStack> after, String what) {
		check(totals(before).equals(totals(after)), what + ": items changed " + totals(before) + " -> " + totals(after));
	}

	/** Non-empty stacks come first, in registry order, full stacks of an item before its partial stack. */
	private static void checkPlainSorted(List<ItemStack> stacks, String what) {
		boolean seenEmpty = false;
		int lastId = -1;
		Map<String, Boolean> seenPartial = new HashMap<>();
		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) {
				seenEmpty = true;
				continue;
			}
			check(!seenEmpty, what + ": gap before " + stack);
			int id = BuiltInRegistries.ITEM.getId(stack.getItem());
			check(id >= lastId, what + ": out of order at " + stack);
			lastId = id;
			String key = key(stack);
			check(!seenPartial.getOrDefault(key, false), what + ": two partial stacks or full after partial of " + key);
			if (stack.getCount() < stack.getMaxStackSize()) {
				seenPartial.put(key, true);
			}
		}
	}

	/** Each item type starts at the beginning of a column (or row) when there is room for that. */
	private static void checkLinesStartFresh(List<ItemStack> stacks, int width, int height, boolean byRows, String what) {
		String previous = null;
		int lines = byRows ? height : width;
		int lineLength = byRows ? width : height;
		for (int line = 0; line < lines; line++) {
			for (int offset = 0; offset < lineLength; offset++) {
				ItemStack stack = stacks.get(byRows ? line * width + offset : offset * width + line);
				if (stack.isEmpty()) {
					continue;
				}
				String key = key(stack);
				if (!key.equals(previous)) {
					check(offset == 0, what + ": " + key + " does not start a new line");
				}
				previous = key;
			}
		}
	}

	private static Map<String, Integer> totals(List<ItemStack> stacks) {
		Map<String, Integer> totals = new HashMap<>();
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				totals.merge(key(stack), stack.getCount(), Integer::sum);
			}
		}
		return totals;
	}

	private static String key(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()) + stack.getComponentsPatch().toString();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	// ------------------------------------------------------------------ world helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static ServerLevel level(MinecraftServer server) {
		return player(server).level();
	}

	private static String xyz(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static void setBlock(TestSingleplayerContext world, BlockPos pos, String block) {
		world.getServer().runCommand("setblock " + xyz(pos) + " " + block);
	}

	private static void fill(TestSingleplayerContext world, BlockPos pos, Map<Integer, String> items) {
		items.forEach((slot, item) -> world.getServer().runCommand("item replace block " + xyz(pos) + " container." + slot + " with " + item));
	}

	private static void open(ClientGameTestContext context, TestSingleplayerContext world, Function<MinecraftServer, MenuProvider> provider,
							 Class<? extends Screen> screen) {
		world.getServer().runOnServer(server -> player(server).openMenu(provider.apply(server)));
		context.waitForScreen(screen);
		context.waitTicks(5);
	}

	private static void close(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(minecraft -> minecraft.player.closeContainer());
		world.getConnection().waitForServerboundPackets();
		context.waitTicks(2);
	}

	/** The server's view of the open menu's container slots. */
	private static List<ItemStack> containerItems(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			List<ItemStack> items = new ArrayList<>();
			for (Slot slot : player(server).containerMenu.slots) {
				if (!(slot.container instanceof Inventory)) {
					items.add(slot.getItem().copy());
				}
			}
			return items;
		});
	}

	/** The server's view of the player's 27 main inventory slots. */
	private static List<ItemStack> playerMain(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			List<ItemStack> items = new ArrayList<>();
			for (int i = 9; i < 36; i++) {
				items.add(player(server).getInventory().getItem(i).copy());
			}
			return items;
		});
	}

	// ------------------------------------------------------------------ screen helpers

	private record ButtonInfo(SortButton.Icon icon, int x, int y, int section) {
	}

	/** The sort buttons on screen; section 0 is the top row of buttons, section 1 the one below it. */
	private static List<ButtonInfo> buttons(ClientGameTestContext context) {
		List<ButtonInfo> found = context.computeOnClient(minecraft -> {
			List<ButtonInfo> list = new ArrayList<>();
			for (AbstractWidget widget : Screens.getWidgets(minecraft.gui.screen())) {
				if (widget instanceof SortButton button) {
					list.add(new ButtonInfo(button.icon(), button.getX(), button.getY(), 0));
				}
			}
			return list;
		});
		int topY = found.stream().mapToInt(ButtonInfo::y).min().orElse(0);
		List<ButtonInfo> result = new ArrayList<>();
		for (ButtonInfo b : found) {
			result.add(new ButtonInfo(b.icon(), b.x(), b.y(), b.y() == topY ? CONTAINER_SECTION : PLAYER_SECTION));
		}
		result.sort(Comparator.comparingInt(ButtonInfo::section).thenComparingInt(ButtonInfo::x));
		return result;
	}

	private static ButtonInfo button(List<ButtonInfo> buttons, int section, SortButton.Icon icon) {
		return buttons.stream()
			.filter(b -> b.section() == section && b.icon() == icon)
			.findFirst()
			.orElseThrow(() -> new AssertionError("no " + icon + " button in section " + section + ": " + buttons));
	}

	private static void hover(ClientGameTestContext context, ButtonInfo button) {
		double[] scale = context.computeOnClient(minecraft -> {
			Window window = minecraft.getWindow();
			return new double[] {(double) window.getScreenWidth() / window.getGuiScaledWidth(), (double) window.getScreenHeight() / window.getGuiScaledHeight()};
		});
		context.getInput().setCursorPos((button.x() + SortButton.SIZE / 2.0) * scale[0], (button.y() + SortButton.SIZE / 2.0) * scale[1]);
		context.waitTicks(3);
	}

	private static void click(ClientGameTestContext context, TestSingleplayerContext world, ButtonInfo button) {
		hover(context, button);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
		world.getConnection().waitForServerboundPackets();
		context.waitTicks(2);
	}
}
