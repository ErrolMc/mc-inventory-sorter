# Inventory Sorter

Client-only Fabric mod for Minecraft 26.3: sort / sort-in-columns / sort-in-rows / move-all buttons in the title row of storage panels and the player inventory. Built for the user's MultiMC instance at `E:\MultiMC\instances\26.3` (mods in `.minecraft\mods`).

## Commands

- `.\gradlew.bat build`: compile, run unit tests, jar in `build\libs` (ship `inventory-sorter-<version>.jar`, not `-sources`)
- `.\gradlew.bat runClientGameTest`: real client in a test world; opens chest, double chest, barrel, shulker box, ender chest and the inventory, clicks the buttons and presses the sort key with real input, and checks the server-side contents. Screenshots in `build\run\clientGameTest\screenshots`. Opens a game window; the user wants it muted, which `src/gametest/.../MuteAudio` does (at startup and again at test start, since the runner resets options before each test).
- `.\gradlew.bat runClient`: dev client (Mod Menu and Cloth Config are on the dev runtime classpath)
- `.\gradlew.bat genSources`: decompiled Minecraft sources, needed for editor navigation and for checking 26.x names
- `.\tools\check-deps.ps1 -ModsDir 'E:\MultiMC\instances\26.3\.minecraft\mods' -Staged build\libs\inventory-sorter-<version>.jar` (PowerShell 7): checks the jar's dependencies against the pack; must print `PROBLEMS: 0`

## Gotchas

- Needs JDK 25. `JAVA_HOME` on this machine is JDK 17; `gradle/gradle-daemon-jvm.properties` makes the wrapper run Gradle on JDK 25 (`C:\Program Files\Java\jdk-25.0.5.0.2`) anyway, and `java.toolchain` compiles with 25.
- 26.x is unobfuscated and uses Mojang names, with renames versus 1.21: `GuiGraphicsExtractor` (was GuiGraphics), `extractRenderState`/`extractContents` (was render), `ContainerInput` (was ClickType), `MultiPlayerGameMode.handleContainerInput`, `Identifier` (was ResourceLocation), `minecraft.gui.screen()`. Mouse buttons are SDL numbers: `InputConstants.MOUSE_BUTTON_LEFT = 1`, `RIGHT = 3`; container click button 0 is still primary. Check names in the genSources output instead of guessing.
- Loom plugin is `net.fabricmc.fabric-loom` (no remapping): plain `implementation`/`compileOnly`/`localRuntime`, no `modImplementation`.
- IDE is VS Code, not IntelliJ. `.vscode/launch.json` is hand-written: F5 runs `gradlew runClient --debug-jvm` and attaches on port 5005. Don't run `gradlew vscode`; it overwrites that file with a launcher that uses VS Code's own build output (skips Gradle resource processing).
- Sort key (R, `input/SortKey`): handled by `mixin/AbstractContainerScreenMixin` at the `keyInventory` check in `AbstractContainerScreen.keyPressed`, so focused text fields win, like vanilla's E. In the user's pack REI's recipe key is also R and REI (via Architectury's keyboard mixin) sees keys first. `KeyMapping` takes SDL scancodes (`InputConstants.KEY_R = 21`); there is no `Type.KEYSYM`, use the 3-arg constructor.
- Shift-drag (`input/ShiftDrag`) uses Fabric's per-screen `ScreenMouseEvents`, registered in `AFTER_INIT` (Fabric resets them on every init). Vanilla shift-clicks the first slot itself; the drag only takes slots from that slot's container. Drag events carry the modifiers from the press, so live Shift comes from `Minecraft.hasShiftDown()`. Fabric's `TestInput` sends mouse buttons with no modifiers, so the game test presses through `MouseHandlerAccessor.invokeOnButton` with modifier 1 (left Shift) to get a real shift-click.
- Bundles (and any item overriding `overrideStackedOnOther`/`overrideOtherStackedOnMe`) act on clicks, so the planner never swaps them on the cursor; see `SortPlanner`.

## Layout

- `src/main`: `sort/SortPlanner` is pure Java (no Minecraft): turns a slot grid into a list of vanilla PICKUP clicks. Unit tests in `src/test` replay the clicks with an independent copy of vanilla's click rules.
- `src/client`: Minecraft side. `gui/ButtonGroups` adds buttons on `ScreenEvents.AFTER_INIT`; `sort/Section` finds the container and player slot grids; `sort/Sorter` builds the planner input and sort order; `sort/ClickQueue` sends clicks (optionally spread over ticks); `input/SortKey` is the R key binding and its action/target dispatch; `input/ShiftDrag` moves slots dragged over with Shift held; `config/` holds the Gson config, the Mod Menu entrypoint and the Cloth Config screen (only loaded when Cloth Config is present); `mixin/AbstractContainerScreenAccessor` reads panel geometry.
- `src/gametest`: the client game test.

## Working with the user

Terse; wants things done and recommended, not surveyed. Ask before committing, pushing, or touching the MultiMC `mods` folder. The user wants Inventive Inventory removed from the pack when this mod is installed.
