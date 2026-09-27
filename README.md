# Nether Portal Helper

A client-side Fabric mod that calculates the matching Nether and Overworld coordinates and provides an optional compass for a locked portal target.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.5 or later
- Fabric API 0.161.0 or later for Minecraft 26.2
- Java 25 or later

## Controls

- **P:** Lock or unlock the calculated portal target and navigate to it.
- **B:** Open the saved portal bookmarks for the current world or server.
- **F6:** Show or hide the HUD. Both key bindings can be changed in Minecraft's Controls settings.

Bookmarks store a named position in the Overworld or Nether, or the current calculated target. They remain separate for each single-player world and multiplayer server. Select a saved bookmark and choose **Navigate** to use it with the compass.

## Build

Run `./gradlew build` on Linux or macOS, or `gradlew.bat build` on Windows. The distributable JAR is written to `build/libs`.

See [CHANGELOG.md](CHANGELOG.md) for release notes.
