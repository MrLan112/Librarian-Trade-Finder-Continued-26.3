# Build fixes — 26.3 patch release

This patch release addresses the three compile errors reported while building `2.6.0+26.3`:

1. `ControlUi` no longer accesses the `Screen.renderables` field. It walks the public `Screen.children()` list and renders children that implement `Renderable`.
2. `TradeFinder` now calculates the lectern center with `Vec3.atCenterOf(blockPos)` rather than the removed/unavailable `BlockPos.getCenter()` call. The center is calculated once before iterating nearby entities.
3. The obsolete `Screen.renderables` access-widener entry was removed; the access widener remains for `AbstractSelectionList.Entry`.

The project version is now `2.6.1+26.3`.

The previous compiler output also contained an unchecked-operation note for Brigadier's generic `Holder` argument extraction. It is a warning, not a compilation error, and is not the cause of the failed build.
