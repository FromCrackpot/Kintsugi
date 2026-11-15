# Module bfxr

The `.bfxr` sound format of bfxr2: a JSON document naming a synth, its version and its parameter values, rendered to
`Pcm` by ports of bfxr2's own synths. Documents saved by bfxr2's editor load unchanged; what bfxr2 would silently
ignore or clamp is reported instead.

# Package fromcrackpot.kintsugi.bfxr

`parseBfxr` checks a document and returns a `BfxrSound`, whose `render` produces mono samples at 44,100 Hz matching
bfxr2's renderer. `BfxrSynth` lists the 23 synths and the parameters each accepts, pinned to one bfxr2 release.
Footsteppr's samples can reach ±4, as in bfxr2; a sink clips them at output.

## The document

| Key | Required | Meaning |
|-----|----------|---------|
| `synth_type` | yes | a `BfxrSynth.id`, case-sensitive |
| `version` | yes | that synth's `BfxrSynth.version` |
| `params` | yes | an object of parameter values; an omitted parameter takes its default |
| `renderSeed` | no | 0 to 1, default 0.5: the seed of the noise the classic Bfxr and Footsteppr synths draw |
| `file_name` | no | the editor's name for the sound; ignored |

```json
{
  "synth_type": "Whooshr",
  "version": "1.0.0",
  "params": { "duration": 0.3, "size": 0.45, "whistle": 0.3, "movement": 0.9 }
}
```

## Parameter shapes

| `BfxrParam` | JSON value | Accepted |
|-------------|------------|----------|
| `Range` | a number | from `min` to `max`, bounds included |
| `Choice` | a number | one of `options` |
| `Transition` | `{"start": …, "end": …, "curve": …}` | both ends from `min` to `max`, a curve out of `curves`; an omitted field takes the default's |
| `Text` | a string | at most `maxLength` UTF-16 units |

## Jinglr's phrase

Jinglr's `phrase` is a JSON array of notes, written as a string inside the document:
`"phrase": "[{\"degree\":0,\"beats\":0.5},{\"degree\":4,\"beats\":1}]"`. It holds 1 to 12 notes, played in order.

| Note key | Accepted | Omitted |
|----------|----------|---------|
| `degree` | an integer from −7 to 14, a step of the scale from the key's root; `null` for a rest | 0 |
| `beats` | a multiple of 0.25 from 0.25 to 2 | 0.5 |

## Mixr's sources

Mixr's `sources` is a JSON array of at most two sounds of other synths, written as a string inside the document, layered
by `balance`. A slot is `null` when empty.

| Source key | Required | Accepted |
|------------|----------|----------|
| `synth` | yes | a `BfxrSynth.id` other than Mixr |
| `params` | no | that synth's parameters, checked as in a document of its own; an omitted parameter takes its default |
| `renderSeed` | no | 0 to 1: the seed of the noise its synth draws; Mixr's own `seed` when omitted |
| `name` | no | the editor's name for the source, at most 60 UTF-16 units |
| `generator`, `selectedGenerator` | no | the editor's preset choice, any string; ignored |

## Stricter than bfxr2

bfxr2 renders any document it can read: it ignores unknown keys, clamps out-of-range numbers, swaps a value of the
wrong type for the default, cuts long texts and remaps retired options. `parseBfxr` and the `BfxrSound` constructor
report each of these as a problem instead. All the problems come at once in one `BfxrFormatException`, with a
did-you-mean hint for a misspelt name, so a mistake never turns silently into a different sound.

- Jinglr's phrase is checked note by note in the same way. bfxr2 turns a melody it can't read into its default one.
- Mixr's sources are checked the same way too. bfxr2 drops a source it can't read, keeps only the first two, and cuts
  a long name.

A classic Bfxr sound whose pitch starts below its own `min_frequency_relative_to_starting_frequency` mutes before its
first sample and renders nothing. bfxr2 can't play such a sound either. It is rejected too, as a Mixr source as well,
once every other problem is fixed.
