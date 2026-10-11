# Module compose

The Compose canvas renderer: `Kanvas`, a composable drawing a scene graph onto a
transparent Compose canvas — every shape in CGA model space, uniformly fit-scaled and clipped
to the scene's bounds — plus the pure conversion boundary between model space and canvas px,
the Compose bridge for the shared paint vocabulary, and a draw-call tracing entry validating
draw output without pixel rasterization.

# Package fromcrackpot.kintsugi.compose

The composable and the conversion boundary. `Kanvas` draws the scene; `cgaToCanvas` /
`canvasToCga` convert between CGA model space (centered, Y-up) and canvas px (top-left-origin,
Y-down) — the module's single coordinate boundary; `fitScale` is the one draw-scale formula the
draw pass and pointer-input conversion share; and `worldBorder` derives the circle infinite
lines are clipped against.

# Package fromcrackpot.kintsugi.compose.style

The Compose bridge for the scene graph's paint vocabulary: `ShapeColor` to Compose `Color` and
back — channel-exact towards Compose, sRGB-converted and 8-bit-rounded back.

# Package fromcrackpot.kintsugi.compose.tracing

Draw-call tracing: `canvasToTrace` runs styled shapes through the production draw pass and
returns each recorded Compose canvas call as one normalized text line — geometry and style
assertions with no pixel rasterization.
