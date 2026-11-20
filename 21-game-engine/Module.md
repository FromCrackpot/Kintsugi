# Module game-engine

The game engine and SDK umbrella. Games depend on `game-engine` alone and reach every library type
through it: the CGA algebra (`cga`), the scene graph (`scene-graph`), the Compose renderer
(`compose`), the SVG persistence layer (`svg`), the audio output (`audio`) and the `.bfxr` sound
format (`bfxr`).

One entry composable, `Game`, owns the machinery every game would otherwise hand-roll: a
semi-fixed-timestep loop, input sampled in CGA units, sound output, and the asset-load boundary.
The game supplies a world factory that builds its `World`, the immediate-mode state frame. A world
holds the game's nodes in staged properties, advances one step at a time, and projects itself into
the scene drawn every frame.

# Package fromcrackpot.kintsugi.gameengine

The engine surface, in five parts:

- `Game`, the entry composable running the loop.
- The world frame every game's state builds on. `World` holds `staged` properties. Its `scene`
  projection draws them in staging order unless overridden, and `root`, what the engine draws, is
  the scene plus what the engine draws over it. `step` runs the per-step `update` hook.
- The per-step context. `Step` carries the step's duration and its `Input` sample, and is the
  context of every update. `restart` is the step-gated call that ends a step, and the world it runs
  in, through its `Restart` signal.
- Sound. `Sound` holds samples compared by content. An update plays one with `play`, and the world
  lists the step's sounds in `played`.
- The load boundary, `AssetScope`. Every world, the first and each replacement, is built from what
  it loads and from programmatic shapes. It loads SVG assets as one typed leaf or as every leaf,
  and `.bfxr` sounds, rendered at load.

## Sound

A world never touches the audio output. `play` records the sound on the world, and once a frame's
steps are done, `Game` plays what they recorded.

| Rule | Behavior |
|---|---|
| One upload per sound | Equal sounds share one clip for the `Game`'s life, so a world rebuilt on `restart` costs no new upload. |
| One start per frame | Equal sounds played within one frame start once. |
| Voices ring out | A pause, an interruption or a `restart` lets playing sounds finish. Only `Game` leaving the composition stops them. |
| A restart still sounds | What the restarting step played before `restart` plays too. |
| No output, no sound | When the platform's audio output can't open or fails, the game runs on silently. |
| A browser waits for a press | In a browser, sounds played before the page's first pointer or key press are silent. |
| Headless | `drive` and direct `step` calls play nothing. `played` shows what each step played. |

# Package fromcrackpot.kintsugi.gameengine.entity

The entity tier. A game entity is an immutable value subclassing its node, so it substitutes into
every operation on that node and its shape.

- `PolygonEntity` is a polygon-node subclass. Every entity class meets its `rebuild` obligation,
  which carries its facts through the shape-preserving operations.
- `GroupEntity` is its group-node analog, with typed children and a typed `rebuild` obligation. A
  factory of the same name builds a fact-free group.
- `Entity` names the position a `PolygonEntity` reads as `x` and `y`.

The subpackages hold what acts on entities: the traits they opt into, the kinetics a game applies
at once, and the kinematics the engine runs over time.

# Package fromcrackpot.kintsugi.gameengine.entity.traits

The trait tier: interfaces naming the recurring facts an entity carries beside its geometry. An
entity opts into a trait by implementing it.

- A passive trait, like `Inertial` (a velocity), gets no behavior from the engine. A kinetics
  demands the traits it reads and writes through its operator's generic bounds.
- `Explosive` (how many fragments an entity bursts into) comes with its verb, `explode`. It splits
  the entity into fragments that the world runs and draws over its scene until the last one is gone.
- A kinematic trait (`Kinematic`, and `Tilting`, a tilt target and rate) names its law as its
  `law`. The engine advances every staged entity carrying one, once per step. A class with two
  kinematic traits overrides `law` to choose.

# Package fromcrackpot.kintsugi.gameengine.entity.kinetics

The kinetics tier: a versor acts on shapes, and a kinetics acts on entities, at once.

- A kinetics is a stateless value applied to one entity with `*`: `entity *= bounce(penetration)`.
  It performs a universal instant response every game would otherwise hand-roll.
- It demands the entity's traits through its operator's generic bounds, so a missing trait is a
  compile error.
- Each kinetics is built by a lowercase-verb factory, and the moved entity comes back as its own
  class through its `copy` hook.

"Kinetics" is invariable: *a kinetics*, like *a series*. `Bounce` is the first resident, resolving
a `Penetration` as a perfectly elastic reflection.

# Package fromcrackpot.kintsugi.gameengine.entity.kinematics

The kinematics tier: a kinetics changes an entity at once, and a kinematics moves it over time. A
kinematics is a law (`Kinematics`) that the engine applies once per step, before the game's update,
to every staged node whose kinematic trait names it as its `law`. The game only sets the trait's
facts.

A law's contract:

- it hands back a node it leaves at rest as itself, so a world at rest draws nothing new;
- it keeps the node's class;
- it is exact however a second splits into steps.

"Kinematics" is invariable: *a kinematics*. Two residents:

- `TiltServo`, the law behind `Tilting`: a rate-limited turn about the centroid toward the target.
  On arrival, the target resets to zero and the entity turns back to rest within the same step.
- `Blast`, the law of an explosion, which the world runs on its own. The fragments' distances from
  the burst's origin grow by a spread that starts fast and settles at `1 + burstRate / drag`. Each
  fragment's size decays as `exp(−travel / erodeLength − age / erodeTime)` until it falls to
  `minSize` and is dropped. So the farther fragments end smaller, and every fragment, one on the
  origin included, is gone in finite time.

# Package fromcrackpot.kintsugi.gameengine.input

The input a step reads: sampled state, never events.

- `Input` is one step's snapshot of every active `Pointer`, in CGA units, and the `Keys`. Its press
  and release edges are latched between frames and seen by a frame's first step only.
- `FollowInputX` is the stateful pointer-or-keys follow helper, one instance per follower.
