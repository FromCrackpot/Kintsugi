# Kintsugi

Source-available, free for non-commercial use under the [Kintsugi Engine Community License](./LICENSE).

> **Experimental** — expect breaking changes before 1.0.

A cross-platform 2D vector game engine powered by Conformal Geometric Algebra (CGA), built with Kotlin Multiplatform (KMP/CMP).
Scenes are authored in code or SVG, carried by a renderer-neutral scene graph, and either rendered through Compose Canvas on desktop (JVM) and web (WASM) or serialized back to SVG.

```
code ─┐                  ┌─ Compose Canvas (desktop JVM, web WASM)
      ├─ scene graph ────┤
SVG  ─┘                  └─ SVG
```

## Modules

| Folder | Gradle ID | Description |
|--------|-----------|-------------|
| [`01-cga`](./01-cga) | `:cga` | 2D CGA engine — primitives, operators, transforms |
| [`02-scene-graph`](./02-scene-graph) | `:scene-graph` | Renderer-neutral scene graph |
| [`11-compose`](./11-compose) | `:compose` | Compose Canvas renderer |
| [`12-svg`](./12-svg) | `:svg` | SVG ↔ scene-graph codec |
| [`21-game-engine`](./21-game-engine) | `:game-engine` | Game engine — loop, input, assets |
| [`90-scratchpad`](./90-scratchpad) | `:scratchpad` | Scratch scenes for demos and experiments |
| [`91-brickit`](./91-brickit) | `:brickit` | Brick'It, a breakout-like game |

## Getting Started

The demo apps run from a clone of this repository and need JDK 25 or newer:

```bash
git clone https://github.com/FromCrackpot/Kintsugi.git
cd Kintsugi
```

```bash
./gradlew :brickit:run                             # Brick'It, desktop (JVM)
./gradlew :brickit:wasmJsBrowserDevelopmentRun     # Brick'It, web (WASM)
./gradlew :scratchpad:run                          # Scratchpad, desktop (JVM)
./gradlew :scratchpad:wasmJsBrowserDevelopmentRun  # Scratchpad, web (WASM)
```

To build your own game on the engine, see [Usage](#usage).

## Usage

The libraries are published to `https://repo.fromcrackpot.com`. The JVM artifacts need JDK 25 or newer.
The `content` filter makes Gradle ask this repository only for Kintsugi artifacts:

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.fromcrackpot.com") {
        content { includeGroup("com.fromcrackpot") }
    }
}

dependencies {
    implementation("com.fromcrackpot:kintsugi-game-engine:0.1.2")
}
```

## Math Resources

Kintsugi's geometry follows [Eric Lengyel](https://x.com/EricLengyel)'s formulation of geometric algebra: his notation, operator names and sign conventions.

[**Projective Geometric Algebra Illuminated**](https://projectivegeometricalgebra.org/) (2024) is the best way into CGA, and assumes only basic linear algebra.

The rest of his work (wikis, a C++ library, Mathematica packages) is gathered at [projectivegeometricalgebra.org](https://projectivegeometricalgebra.org/).

<a href="https://projectivegeometricalgebra.org/"><img src="https://projectivegeometricalgebra.org/pgai.jpg" alt="Projective Geometric Algebra Illuminated, front cover" height="260"></a>

