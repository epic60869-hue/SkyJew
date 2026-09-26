# Third-party notices

## NopoMod emoji assets

SkyBalls's integrated chat-emoji renderer uses the emoji sprite set and emoji-name data from NopoMod.

Upstream project: https://github.com/NopoTheGamer/NopoMod
Pinned source commit: 3243c18910c1b281f6585cd4657b349df4c60d50

The Gradle build fetches those upstream assets into the generated resource directory rather than copying the binary sprites into this repository. The upstream project's LICENSE applies to those assets/source materials.

The SkyBalls feature implementation itself is a separate Java implementation and does not copy NopoMod's Kotlin feature architecture.

The pet display's overflow-level calculation from the tab list (adding the XP of the capped levels to the "+N XP" overflow line and recomputing the level on the legendary curve, and the progress line format) follows NopoMod's `features/pets/PetDisplay.kt` and `OverflowPetLevels.kt`, licensed under the GNU Lesser General Public License v2.1.

## Skyblocker

SkyBalls's /sj custom item and armor customization (the `com.epic60869.skyballs.custom` package and its mixins) is ported from Skyblocker:

- https://github.com/SkyblockerMod/Skyblocker
- Version: v6.10.4+26.2
- Relevant source: src/main/java/de/hysky/skyblocker/skyblock/item/custom/, the GUI utilities it uses under src/main/java/de/hysky/skyblocker/utils/, and the DataComponentHolder, DyedItemColor, ItemStack and EquipmentLayerRenderer mixins
- Assets: the customization screen sprites under assets/skyballs/textures/gui/ and the related en_us translations

Skyblocker is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).

Each ported source file keeps a Skyblocker attribution line. SkyBalls replaces Skyblocker's config, NEU repository and scheduler dependencies with its own small implementations.

SkyBalls's Item Price Tooltip follows Skyblocker's AvgBinTooltip, LBinTooltip and NpcPriceTooltip (src/main/java/de/hysky/skyblocker/skyblock/item/tooltip/adders/) and reads auction prices from the same API (hysky.de). The container menu mixin that notifies slot listeners (used by the Chronomatron solver) follows Skyblocker's AbstractContainerMenuMixin.

## CommandKeys

SkyBalls's /sj keys GUI and macro system is a port of TerminalMC/CommandKeys (the `com.epic60869.skyballs.commandkeys` package, its mixins, the `skyballs_commandkeys` assets and translations, and the MultiLineEditBox entry in `skyballs.classtweaker`).

- https://github.com/TerminalMC/CommandKeys
- Branch: mc26.2
- License: Apache License 2.0 (a copy is included at assets/skyballs_commandkeys/LICENSE.txt)

Changes made for SkyBalls: repackaged, mod ID changed to `skyballs_commandkeys`, the command moved to /sj keys, the multi-platform service loader replaced with a Fabric implementation, and a one-time import of SkyBalls's previous command key macros added. Each modified source file keeps its original license header with a note of these changes.

SkyBalls's rarity item backgrounds, calendar date calculator and commission HUD style are ported from Skyblocker (see above).

## Skyblocker dungeon and experimentation features

SkyBalls's dungeon map, puzzle solvers, secret waypoints, room detection, terminal and device solvers, and experimentation table solvers (the `com.epic60869.skyballs.sb` package and the `assets/skyballs/dungeons` room data) are ported from Skyblocker v6.10.4+26.2 (LGPL-3.0), keeping Skyblocker's source structure. Skyblocker's config, location, scheduler and rendering classes are replaced by small SkyBalls stand-ins, and its custom world renderer is replaced by one built on Minecraft's gizmos.

## SkyHanni repository data

Several SkyBalls features use chat and item patterns, slayer XP and spawn costs, and the sea creature list published in SkyHanni's data repository (https://github.com/hannibal002/SkyHanni-REPO, MIT License). The sea creature list is downloaded at runtime; the patterns and slayer values are included in SkyBalls's source.

## SkyHanni GUI Position Editor

SkyBalls's /sj gui position editor is an adaptation of the SkyHanni GUI position editor interaction model, including draggable HUD boxes, hover information, keyboard movement, and scroll-wheel scaling.

- https://github.com/hannibal002/SkyHanni
- Relevant source: src/main/java/at/hannibal2/skyhanni/config/core/config/gui/GuiPositionEditor.kt and src/main/java/at/hannibal2/skyhanni/data/GuiEditManager.kt
- License: GNU Lesser General Public License v2.1 (LGPL-2.1)

SkyBalls's implementation is independently adapted to SkyBalls's own Java/Fabric 26.x HUD system and does not add SkyHanni as a runtime dependency.

## SkyHanni Current Chat Display

SkyBalls's current chat display (`SkyBallsCurrentChat.java`) follows SkyHanni's CurrentChatDisplay (src/main/java/at/hannibal2/skyhanni/features/chat/CurrentChatDisplay.kt): the channel-change message patterns and the channel names and colours. SkyHanni is licensed under the GNU Lesser General Public License v2.1 (LGPL-2.1).

## SkyOcean

SkyBalls's search-keybind and recipe-command workflows are adapted from the corresponding SkyOcean features:

- https://github.com/meowdding/SkyOcean
- Item search: src/main/kotlin/me/owdding/skyocean/features/item/search/ItemSearch.kt
- Recipe command: src/main/kotlin/me/owdding/skyocean/commands/CraftHelperCommand.kt
- Recipe autocomplete: src/main/kotlin/me/owdding/skyocean/utils/suggestions/RecipeNameSuggestionProvider.kt

SkyBalls's /sj recipe craft helper is a Java port of SkyOcean's craft helper (src/main/kotlin/me/owdding/skyocean/features/recipe/crafthelper/ and commands/CraftHelperCommand.kt): the recipe tree with leftover carry-over, the have/need evaluation, and the tree display. SkyOcean's code is licensed under the MIT License (SkyOcean License v1, section 1); SkyBalls reads recipes from the NEU repository instead of SkyOcean's repo library.

Copyright notice for the ported code: Copyright (c) meowdding / SkyOcean contributors. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Stella

SkyBalls's dungeon secret routes (`features/dungeons/DungeonRoutes.java`) follow Stella's secret-route system: the route file format (steps of waypoints and a path line), the waypoint types and labels, the room coordinate scheme (the blue terracotta roof corner as the origin, with the corner that holds it giving the rotation), step advancing on secret pickup, and automatic recording.

- https://github.com/Eclipse-5214/stella
- Relevant source: src/main/kotlin/co/stellarskys/stella/features/secrets/SecretRoutes.kt, features/secrets/utils/routes/ (RouteRegistry.kt, RoutePlayer.kt, RouteRecorder.kt, WaypointType.kt) and api/dungeons/map/Room.kt
- License: GNU Lesser General Public License v3.0 (LGPL-3.0)

Stella's default routes ("generated by Stella, recorded by Luckkytigger") are not bundled with SkyBalls. They are downloaded at runtime from Stella's route server (https://ether.stellarskys.co/routes/default.json) and cached in the SkyBalls config folder.

## Odin

Several SkyBalls dungeon features follow Odin's implementations:

- https://github.com/odtheking/Odin (main branch, Minecraft 26.1.2)
- Relevant source: `utils/skyblock/SplitsManager.kt`, `features/impl/skyblock/Splits.kt`, `utils/PersonalBest.kt`, `features/impl/dungeon/InvincibilityTimer.kt`, `LeapMenu.kt`, `BloodCamp.kt` (including its Watcher and blood mob head textures, mob skull data by DocilElm), `DoorHighlight.kt`, `PositionalMessages.kt`, the puzzle solvers in `puzzlesolvers/` (Ice Fill, Boulder, Creeper Beams, Three Weirdos, Quiz, Teleport Maze, Water Board and Blaze) with their data files `ice-fill-floors.json`, `boulder-solutions.json`, `creeper-beams-solutions.json`, `quiz-answers.json` and `water-solutions.json` (copied to `assets/skyballs/puzzles/`), and `render/PlayerSize.kt`

Ported to Java in `features/dungeons/DungeonFeatures.java` (splits, split PBs, mask timers), `LeapMenu.java`, `BloodCamp.java`, `BloodCampSkulls.java`, `DoorHighlight.java`, `PositionalMessages.java`, `OdinPuzzleSolvers.java`, `OdinTerminals.java` (terminal solvers from `features/impl/boss/TerminalSolver.kt` and `utils/skyblock/dungeon/terminals/`), `OdinDevices.java` (`SimonSays.kt`, `ArrowAlign.kt`, `ArrowsDevice.kt`), `mixin/SkyBallsPlayerSizeMixin.java` and `mixin/SkyBallsToggleSprintMixin.java` (`AutoSprint.kt` / `LocalPlayerMixin.java`). Each file names Odin in its class comment.

Odin is licensed under the BSD 3-Clause License:

```
BSD 3-Clause License

Copyright (c) 2025, odtheking

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## NoammAddons

SkyBalls's dungeon map ("NoammAddons (Legit)" style) and dungeon score calculator are ported from NoammAddons:

- https://github.com/Noamm9/NoammAddons (26.2 branch)
- Relevant source: `features/impl/dungeon/map/MapRenderer.kt`, `MapConfig.kt`, `utils/dungeons/map/handlers/HotbarMapColorParser.kt`, `MapUpdater.kt`, `ScoreCalculation.kt`, `utils/dungeons/map/utils/MapUtils.kt`, `utils/dungeons/map/core/`, `features/impl/dungeon/ScoreCalculator.kt`, and the checkmark and marker textures in `textures/gui/dungeonmap/` (copied to `assets/skyballs/textures/gui/dungeonmap/`)

Ported to Java in `features/dungeons/NoammMap.java` and `features/dungeons/ScoreCalculator.java`. NoammAddons is dedicated to the public domain under CC0 1.0 Universal.

## Skysoft Tooltip Scroll

SkyBalls's tooltip scroll (`features/misc/ScrollableTooltips.java`, `SkyBallsTooltipMixin`, `SkyBallsTooltipScrollMixin`) is a Java port of Skysoft's Tooltip Scroll (src/main/kotlin/com/skysoft/gui/tooltip/TooltipViewport.kt and TooltipPanSession.kt, com/skysoft/config/TooltipScrollConfig.kt, and the tooltip positioner / mouse scroll mixins): the pan session with smooth movement, the keyboard and mouse wheel controls, and the settings.

- License: GNU Lesser General Public License v3.0 (LGPL-3.0)
