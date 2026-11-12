# Slot Machine (Fabric mod, Minecraft 26.3)

A gambling-themed test mod: craftable components assemble into a two-block-tall
slot machine with a chest-style GUI.

## Status: work in progress

Working:
- All crafting recipes (gear, slot reel, slot handle, copper wire, top/bottom
  halves, final assembly)
- The slot machine block: places two-tall, breaks fully from either half
- Right-click opens the Slot Machine GUI (payment slot, locked reel slots, Spin button)
- Shift-right-click opens a separate Luck Slot GUI

Also working (untested in-game): spin RNG, reel animation, payouts, heart removal, /op jackpot.

Not implemented yet:
- No charm items or luck effects — the Luck Slot GUI is just a bare inventory
- No custom "gambling" recipe-book category (recipes currently use vanilla's
  `misc` category since custom categories need client-side mixin work)

## Building

This project uses the Fabric Loom Gradle plugin. The Gradle wrapper isn't
checked in yet — open the project in IntelliJ IDEA (it will offer to generate
the wrapper) or run `gradle wrapper` locally if you have Gradle installed,
then commit the generated `gradlew`, `gradlew.bat`, and `gradle/wrapper/`
files.

Requires JDK 25 (Minecraft 26.3's runtime).

```
./gradlew runClient
```
