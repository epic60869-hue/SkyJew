# Third-party notices

## NopoMod emoji assets

SkyJew's integrated chat-emoji renderer uses the emoji sprite set and emoji-name data from NopoMod.

Upstream project: https://github.com/NopoTheGamer/NopoMod
Pinned source commit: 3243c18910c1b281f6585cd4657b349df4c60d50

The Gradle build fetches those upstream assets into the generated resource directory rather than copying the binary sprites into this repository. The upstream project's LICENSE applies to those assets/source materials.

The SkyJew feature implementation itself is a separate Java implementation and does not copy NopoMod's Kotlin feature architecture.

## Skyblocker

SkyJew's /sj custom GUI is an adaptation of the Skyblocker customization-screen design and source structure from:

- https://github.com/SkyblockerMod/Skyblocker
- Relevant source: src/main/java/de/hysky/skyblocker/skyblock/item/custom/screen/

Skyblocker is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).

The SkyJew adaptation keeps the Skyblocker attribution in the relevant source files. This notice is provided alongside the adapted code.

## CommandKeys

SkyJew's /sj keys GUI and command-key workflow are adapted from TerminalMC/CommandKeys.

- https://github.com/TerminalMC/CommandKeys
- Branch used as reference: mc26.1
- License: Apache License 2.0

The SkyJew implementation is adapted to SkyJew's own configuration/backend rather than adding CommandKeys as a runtime dependency.

## SkyHanni GUI Position Editor

SkyJew's /sj gui position editor is an adaptation of the SkyHanni GUI position editor interaction model, including draggable HUD boxes, hover information, keyboard movement, and scroll-wheel scaling.

- https://github.com/hannibal002/SkyHanni
- Relevant source: src/main/java/at/hannibal2/skyhanni/config/core/config/gui/GuiPositionEditor.kt and src/main/java/at/hannibal2/skyhanni/data/GuiEditManager.kt
- License: GNU Lesser General Public License v2.1 (LGPL-2.1)

SkyJew's implementation is independently adapted to SkyJew's own Java/Fabric 26.x HUD system and does not add SkyHanni as a runtime dependency.

## SkyOcean

SkyJew's search-keybind and recipe-command workflows are adapted from the corresponding SkyOcean features:

- https://github.com/meowdding/SkyOcean
- Item search: src/main/kotlin/me/owdding/skyocean/features/item/search/ItemSearch.kt
- Recipe command: src/main/kotlin/me/owdding/skyocean/commands/CraftHelperCommand.kt
- Recipe autocomplete: src/main/kotlin/me/owdding/skyocean/utils/suggestions/RecipeNameSuggestionProvider.kt

SkyJew uses an independent Java implementation and does not add SkyOcean as a runtime dependency. SkyOcean's repository is licensed under its published project terms.
