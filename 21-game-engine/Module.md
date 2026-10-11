# Module game-engine

The game engine and SDK umbrella: games depend on `game-engine` alone and reach every library type —
the CGA algebra (`cga`), the scene graph (`scene-graph`), the Compose renderer (`compose`), the SVG
persistence layer (`svg`) — through it. One entry composable, `Game`, owns the machinery every
game would otherwise hand-roll: a semi-fixed-timestep loop, input sampled in CGA units, and the
asset-load boundary. The game supplies a byte reader and a world recipe building its `World` —
the immediate-mode state frame that owns its entities as plain properties, advances through its
per-step update hook, and projects itself into the scene drawn every frame.

# Package fromcrackpot.kintsugi.gameengine

The engine surface in five parts: `Game`, the entry composable running the loop; the entity tier
(`PolygonEntity`, a polygon-node subclass whose `copy` obligation every entity class discharges
to carry its facts through the shape-preserving operations; `GroupEntity`, its group-node analog
with typed children and a typed `copy` obligation); the world frame
(`World` with its `scene` draw-order tree projection, the `root` the engine draws, and the
per-step `update` hook) every game's state builds on, with
`FollowInputX` as the stateful pointer-or-keys follow helper; the per-step context and snapshots
(`Step` carrying the step's duration and its `Input`/`Pointer`/`Keys` sample, established as the
context of every update, and `restart`, the step-gated call ending a step and the world it runs
in through its `Restart` signal); and the load boundary (`AssetScope`) building every world —
the first and each replacement — from SVG assets and programmatic shapes.

# Package fromcrackpot.kintsugi.gameengine.traits

The trait tier: interfaces naming the recurring facts an entity carries beside its geometry
(`Kinetic` — a velocity). An entity opts into a trait by implementing it; the engine attaches no
behavior of its own, and a dynamics demands the traits it reads and writes through its operator's
generic bounds.

# Package fromcrackpot.kintsugi.gameengine.dynamics

The dynamics tier: a versor acts on shapes, a dynamics acts on entities. A dynamics is a stateless
value applied to one entity with `*` — `entity *= bounce(penetration)` — performing a universal
response every game would otherwise hand-roll, and demanding the entity's traits through its
operator's generic bounds, so a missing trait is a compile error. Each dynamics is built by a
lowercase-verb factory, and the moved entity comes back as its own class through its `copy` hook.
`Bounce` is the first resident, resolving a `Penetration` as a perfectly elastic reflection.
