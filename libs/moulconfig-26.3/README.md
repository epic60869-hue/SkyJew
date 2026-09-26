# MoulConfig for Minecraft 26.3

MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig, LGPL-3.0) has no Minecraft 26.3 build yet, so SkyJew rebuilds its
version-specific "platform" layer for 26.3 and repackages it with the unchanged 4.7.2 common classes
(`libs/maven/org/notenoughupdates/moulconfig/modern-26.3/4.7.2-skyjew/`).

`src/` holds the platform sources: MoulConfig's `modern/templates` (v4 branch) preprocessed for Minecraft 26.3, with these changes:

- Blaze3D classes moved to `com.mojang.renderpearl.api` (`RenderPipeline`, `FilterMode`, `GpuSampler`, `GpuTextureView`).
- 26.3 uses SDL instead of GLFW: `isMouseButtonDown` reads `MouseHandler`, `isKeyDown` no longer takes a window, mouse buttons are
  translated from SDL numbering back to the GLFW numbering MoulConfig uses, and `KeyEvent.scancode()` is now `keycode()`.
- `KEY_LSUPER`/`KEY_RSUPER` are `KEY_LGUI`/`KEY_RGUI`; `GuiGraphicsExtractor.tooltip` takes an extra boolean.
- `moulconfig.accesswidener` uses the new class names.

Swap back to an official `modern-26.3` MoulConfig build in `build.gradle` once one is published.
