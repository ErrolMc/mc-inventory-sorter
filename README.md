# Inventory Sorter

A client-side Fabric mod for Minecraft 26.3 that puts small sort buttons in the title row of every storage panel: chests, double chests, barrels, ender chests, shulker boxes, dispensers, droppers, hoppers and your own inventory.

| Button | What it does |
|---|---|
| Sort | Merges partial stacks and sorts the panel |
| Sort in Columns | Same, but fills top to bottom and each item type starts a new column |
| Sort in Rows | Same, but each item type starts a new row |
| Move All | On the container: everything into your inventory. On your inventory: everything into the container |

When there are more item types than columns or rows, a type only starts a new one where there's spare room. Columns still fill top to bottom, but in a completely full chest Sort in Rows can only come out the same as Sort.

Right-click any sort button to cycle the sort mode. Hover a button to see the current mode.

Hold **Shift** and drag with the left mouse button to move every stack you pass over to the other inventory, as if you had shift-clicked each one. It only takes items from the side the drag started on, so items don't bounce straight back when the drag crosses into the other panel. Crafting outputs and other result slots are skipped.

Press **R** in any inventory to sort the panel under the mouse. What R does (Sort, Sort in Columns, Sort in Rows or Move All) and which panel it acts on are options; the key itself is under Options → Controls → Key Binds → Inventory Sorter. Like vanilla's own inventory keys, it does nothing while you're typing in a text box.

REI's "show recipe" key is also R by default. REI gets key presses first, so with both on R, pressing R over an item may open REI's recipe page instead of sorting. Rebind one of the two if that gets in the way.

Sorting uses ordinary inventory clicks, so it works on vanilla and Paper servers without anything installed server-side. Bundles are never clicked onto other items, so they don't swallow anything while sorting.

## Options

Open them with **Mod Menu → Inventory Sorter → Configure** (needs Cloth Config), or edit `config/inventory-sorter.json`.

- **Sort by**: Item Type (keeps similar items together), Name, Item ID, Count (biggest piles first), Mod
- **Sort key does**: Sort, Sort in Columns, Sort in Rows or Move All
- **Sort key acts on**: the panel under the mouse, always the container, or always your inventory
- **Shift-drag to move items**: on by default
- **Reverse order**: for example Name Z to A
- **Include hotbar**: lets sorting and Move All on your inventory touch the hotbar (off by default)
- **Clicks per tick**: Instant by default. Lower it if a server's anti-cheat complains about fast clicking
- **Which buttons to show**, whether your inventory gets buttons, and whether crafting tables, furnaces and other vanilla screens show them too
- **Button offset X / Y**: nudges the buttons if a resource pack or another mod puts something in the way

## Install

1. Build it (below) or take `inventory-sorter-<version>.jar` from `build/libs` (not the `-sources` jar).
2. Put it in the instance's `mods` folder next to Fabric API. Mod Menu and Cloth Config are optional and only needed for the settings screen.
3. Restart the game.

Requires Minecraft 26.3, Fabric Loader 0.20.1+, Fabric API and Java 25.

## Development (VS Code)

1. Install JDK 25 (this machine has it at `C:\Program Files\Java\jdk-25.0.5.0.2`; `.vscode/settings.json` points there).
2. Open the folder in VS Code and install the recommended extensions when asked (Extension Pack for Java, Gradle for Java). The Java extension imports the Gradle project on its own; the first import downloads Minecraft and takes a few minutes.
3. Run **Terminal → Run Task → Decompile Minecraft sources** once, then **Java: Clean Java Language Server Workspace** from the command palette. After that, Go to Definition and hover docs work for Minecraft and Fabric API classes.
4. Press **F5** (launch config "Minecraft Client") to start a dev client through Gradle with the debugger attached, so breakpoints work. Edits to method bodies can usually be hot-swapped into the running game (save, then the debugger's hot code replace); anything else needs a restart.

Gradle tasks, from VS Code's task menu or a terminal:

```
.\gradlew.bat build               # compile, unit tests, jar in build\libs
.\gradlew.bat runClient           # dev client without the debugger
.\gradlew.bat runClientGameTest   # in-game test (muted): opens containers, clicks the buttons, presses R, shift-drags, checks the results
.\gradlew.bat genSources          # decompiled Minecraft sources for the editor
```

The Gradle wrapper picks JDK 25 by itself (`gradle/gradle-daemon-jvm.properties`), even though `JAVA_HOME` on this machine is JDK 17.

## Licence

MIT
