# Module audio

Sound output in clip mode: samples are rendered ahead of time into a `Pcm`, uploaded once to a `PcmSink` as a
`Clip`, and every play hands the platform's own player a new `Voice`. No code of this module runs on an audio thread.

# Package fromcrackpot.kintsugi.audio

`PcmSink()` opens the platform's output, `load` turns a `Pcm` into a `Clip`, and `Clip.play` returns a `Voice` to
stop. A sink plays mono PCM at `PcmSink.SAMPLE_RATE`, 44,100 Hz, and leaves conversion to the device rate to the
platform.

## The platform players

| Target | A clip | A play | Voice limit |
|--------|--------|--------|-------------|
| JVM | the samples, kept for a software mixer | a mixer voice; one 16-bit `SourceDataLine` plays the mix | none |
| Android | the samples, kept for a software mixer | a mixer voice; one streaming float `AudioTrack` plays the mix and pauses after 10 s of silence | none |
| iOS | an `AVAudioPCMBuffer` | a pooled `AVAudioPlayerNode` on the engine's main mixer, in the ambient session category | 32 per sink |
| Browser | an `AudioBuffer` | a fresh `AudioBufferSourceNode` through a `GainNode` | none |

Past a limit, the oldest voice is cut to make room for the new one.

## Browsers: the first gesture

A browser keeps audio suspended until the page's first user gesture. The sink resumes it on the first pointer or key
press anywhere in the page; plays before that are silent, never queued.
