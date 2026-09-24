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
