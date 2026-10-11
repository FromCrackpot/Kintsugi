# kintsugi-svg

SVG asset pipeline for Kintsugi: parses SVG into a `:scene-graph` scene graph and serializes scene graphs back to SVG, targeting SVG 1.1 with CGA semantics carried in a `k:*` sidecar namespace (the `/v1` format). 
Pure data layer — no Compose, no rendering. 
See `docs/CGA-SVG-MAP.md` for the format's mapping table and examples.
