# Third-party emoji assets

TastyFish's integrated chat-emoji renderer uses the emoji sprite set and emoji-name data from NopoMod.

Upstream project: https://github.com/NopoTheGamer/NopoMod
Pinned source commit: 3243c18910c1b281f6585cd4657b349df4c60d50

The Gradle build fetches those upstream assets into the generated resource directory rather than copying the binary sprites into this repository. The upstream project's LICENSE applies to those assets/source materials.

The TastyFish feature implementation itself is a separate Java implementation and does not copy NopoMod's Kotlin feature architecture.
