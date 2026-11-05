# Librarian Trade Finder Continued

**A fork of [Librarian Trade Finder](https://github.com/Greeenman999/LibrarianTradeFinder) by Greenman999.**

This fork is maintained and ported to Minecraft Java Edition **26.3** by **Mr_Lan123**. It is not the original project.

**Modification notice:** this fork was modified by Mr_Lan123 on **2026-10-30**. See [FORK_NOTICE.md](FORK_NOTICE.md) for the upstream revision and attribution.

Original project: [GitHub](https://github.com/Greeenman999/LibrarianTradeFinder) · [Modrinth](https://modrinth.com/mod/librarian-trade-finder)

## Features retained from the original mod

- Search for selected enchanted-book trades from a librarian villager.
- Select a lectern and librarian automatically or manually.
- Configure target enchantments, minimum level and maximum emerald price.
- Start or stop search using keybindings or `/tradefinder` commands.
- Optional legit mode, slow mode, axe durability protection and return-to-villager option.
- English, Russian, German and Simplified Chinese translations.

## Minecraft 26.3 port notes

The port updates the project to the 26.3 toolchain and relevant client APIs:

- Uses SDL-backed vanilla `InputConstants` key codes instead of GLFW constants.
- Uses the `Gui` screen API (`minecraft.gui.setScreen` / `minecraft.gui.screen`).
- Uses the 26.3 HUD location for overlay messages.
- Replaces the removed `ServerboundSwingPacket` with `ServerboundPunchPacket` and the 26.3 swing-animation API.
- Replaces the removed `AxeItem` class with the data-driven `#minecraft:axes` item tag (`ItemStack.typeHolder().is(ItemTags.AXES)`) for axe durability protection.
- Registers the key category with `KeyMapping.Category.register`, as required by the 26.3 key-mapping API.
- Notifies Minecraft's text-input manager when custom EditBox focus changes, as required by the SDL input backend.
- Updates Gradle, Loom, Fabric Loader and Fabric API dependencies for 26.3.
- Renders screen widgets through the public `Screen.children()` API instead of accessing the private `Screen.renderables` field.
- Uses `Vec3.atCenterOf(BlockPos)` because `BlockPos.getCenter()` is not present in the 26.3 mappings used by this project.

## Build in IntelliJ IDEA

1. Install a **64-bit JDK 25**.
2. Open this project folder (the folder containing `settings.gradle`) in IntelliJ IDEA.
3. Set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM** to JDK 25.
4. Allow Gradle to sync. The wrapper downloads Gradle 9.8.2 automatically.
5. Run the Gradle task **Tasks → build → build**, or run `gradlew.bat build` in the project root.
6. The mod jar is written to `build/libs/`. Minecraft 26.3 uses the non-obfuscated Loom pipeline, so the build does not run the old `remapJar` task.

## Version/toolchain

- Minecraft: `26.3`
- Java: `25`
- Fabric Loader: `0.20.1`
- Fabric Loom: `1.19-SNAPSHOT`
- Fabric API: `0.165.0+26.3`
- Mod Menu (optional compile-only API): `21.0.0`
- Gradle Wrapper: `9.8.2`

## Credits and license

- Original mod author: **Greenman999**.
- Fork author / maintainer: **Mr_Lan123**.
- Original repository and original work remain credited above.
- This fork retains the original **GNU General Public License v3.0 (GPL-3.0)**. See `LICENSE`.

This repository is a modified fork and must not be presented as the original Librarian Trade Finder.
