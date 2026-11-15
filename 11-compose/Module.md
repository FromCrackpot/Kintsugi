# Module compose

The Compose canvas renderer. `Scene` is a composable that draws a scene graph onto a transparent
Compose canvas: every shape in CGA model space, uniformly fit-scaled and clipped to the scene's
bounds. The module also holds the pure conversion boundary between model space and canvas px, the
Compose bridge for the shared paint vocabulary, and a draw-call tracing entry that validates draw
output without pixel rasterization.

# Package fromcrackpot.kintsugi.compose

The composable and the conversion boundary.

- `Scene` draws the scene.
- `cgaToCanvas` / `canvasToCga` convert between CGA model space (centered, Y-up) and canvas px
  (top-left origin, Y-down). They are the module's single coordinate boundary.
- `fitScale` is the one draw-scale formula that the draw pass and pointer-input conversion share.
- `worldBorder` derives the circle that infinite lines are clipped against.

# Package fromcrackpot.kintsugi.compose.style

The Compose bridge for the scene graph's paint vocabulary: `ShapeColor` to Compose `Color` and
back. The conversion is channel-exact towards Compose, and sRGB-converted and rounded to 8 bits on
the way back.

# Package fromcrackpot.kintsugi.compose.tracing

Draw-call tracing: `canvasToTrace` runs styled shapes through the production draw pass and returns
each recorded Compose canvas call as one normalized text line. It supports geometry and style
assertions with no pixel rasterization.
