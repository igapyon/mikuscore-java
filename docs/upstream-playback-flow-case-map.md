# Pinned upstream playback-flow case map

This map covers `tests/unit/playback-flow.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. WebAudio context creation,
oscillator scheduling, progress callbacks, and UI state updates are browser
execution and excluded. The runtime-independent compaction and measure-timeline
algorithms are included.

| Upstream case | Java regression / owner | Status |
| --- | --- | --- |
| uses `webkitAudioContext` fallback | WebAudio browser execution | excluded |
| fails gracefully when Web Audio API is unavailable | WebAudio browser execution | excluded |
| applies tempo map when scheduling oscillator start times | WebAudio browser execution | excluded |
| extends note release while pedal is active | WebAudio browser execution | excluded |
| reports tick progress while scheduling | WebAudio browser execution | excluded |
| compacts dense schedules by per-onset and total budget | `MidiIoTest#compactsDensePlaybackSchedules` | done evidence |
| uses compacted schedules before creating oscillators | WebAudio browser execution; compaction owner is `MidiIoTest#compactsDensePlaybackSchedules` | excluded |
| preserves small same-onset chords during compaction | `MidiIoTest#keepsProtectedSmallPlaybackOnsetsDuringCompaction` | done evidence |
| prioritizes outer voices and non-octave duplicates in dense onset | `MidiIoTest#prioritizesOuterVoicesAndUniquePitchesInDensePlaybackOnsets` | done evidence |
| builds pickup-aware measure timeline | `MidiIoTest#buildsPinnedPlaybackMeasureTimelineWithCrossPartPickupDetection` | done evidence |
| maps selected playback location when starting mid-score | Browser UI/playback orchestration | excluded |

`src/ts/playback.ts` only re-exports MIDI APIs and remains covered by the MIDI
mapping. No additional runtime-independent implementation is owned by that
file.
