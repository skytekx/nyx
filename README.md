# Nyx

A mod about effects of the moon. Commission for Drakallen

## 1.20.1 port (branch `1.20.1`)

This branch is a **Minecraft 1.20.1 (Forge)** port of Nyx, maintained by SkyTekx.
The original mod and concept are by **Ellpeck** (https://github.com/Ellpeck/Nyx),
and this port keeps the original MIT `LICENSE` and copyright notice intact.

The original `master` branch targets Minecraft 1.12.2 and is a large content mod
(lunar events, meteors, lunar water, tools, enchantments). This port focuses on
Nyx's **client-side night atmosphere**, reimplemented for the modern 1.20.1
client render pipeline:

- **Darker nights, governed by the moon phase.** A Mixin into
  `ClientLevel#getSkyDarken(float)` scales the night sky brightness down. A full
  moon keeps the night relatively bright; a new moon turns it pitch black,
  mirroring how the original tied its effects to the moon-phase factor.
- **Moon-tinted night sky/fog.** A `ViewportEvent.ComputeFogColor` handler
  darkens and tints the fog and horizon to match, deeper on darker moon phases
  (this echoes Ellpeck's original sky-tint handler, but drives it off the moon
  phase rather than scripted lunar events).
- **Configurable** via `ForgeConfigSpec` (client config `nyx-client.toml`):
  darkness strength, moon-phase influence, minimum brightness, fog darkening,
  and the night tint colour/strength.

It is a **client-side only** mod (`displayTest=IGNORE_SERVER_VERSION`); the
dedicated server neither needs nor loads it.

### Building

```
export JAVA_HOME=/path/to/jdk-17
./gradlew build -x test --no-daemon
```

Forge `1.20.1-47.4.x`, Parchment `2023.09.03-1.20.1`, JDK 17. The built jar
lands in `build/libs/`.
