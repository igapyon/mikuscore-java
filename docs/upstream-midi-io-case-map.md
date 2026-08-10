---
title: MIDI I/O pinned case map
status: unit-and-golden-complete
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# MIDI I/O pinned case map

The renamed Node upstream at `v0.6.1` contains 70 `it` cases in
`tests/unit/midi-io.spec.ts`. Its separate
`tests/unit/midi-roundtrip-golden.spec.ts` generates three baseline fixture
cases and defines two explicit semantic-golden cases, for five runtime cases
in total. All 75 public unit/golden cases are exercised by `MidiIoTest`.

The golden bridge intentionally mirrors the Node execution conditions:
playback extraction and event collection use 128 TPQ, while the non-raw
MidiWriterJS-compatible output keeps its default 480-TPQ header. This
distinction is required for the triplet fixture to reproduce the pinned Node
comparison envelope.

| Pinned upstream cases | Java evidence | Status |
| --- | --- | --- |
| `midi-io.spec.ts` lines 228-769 (15 cases): full/implicit measure timeline, grace modes, tie fallback, slur/retrigger boundaries, and mixed underfull timeline | `keepsFullNonImplicitMeasureLengthForPlaybackTimeline`; grace/tie/slur regression group; `keepsTimelineStableForUnderfullImplicitRegularUnderfullSequence` | done |
| lines 802-1051 (8 cases): metric accents, tempo and pedal collection, drum mapping, and simple MIDI import | metric-accent, tempo/pedal, drum, and `convertsMidiToMusicXmlUsingImportedSkeletonFacade` regressions in `MidiIoTest` | done |
| lines 1065-1522 (23 cases): import staccato/beam/retrigger/polyphony/track and drum split, quantize/pickup/key/spelling/staff policies | direct public `convertMidiToMusicXml` regressions from `doesNotInferStaccatoFromDetachedMidiImportNotes` through `doesNotEmitFullRestOnlyInactiveVoiceInMeasureThatAlreadyHasNotesOnMidiImport` | done |
| lines 1540-1848 (16 cases): import metadata, warnings/options, FF59/FF58 export, pickup prelude, and raw text/track-name metadata | MIDI import metadata/diagnostic regressions; `collectsMidiKeySignatureEventsForFf59ExportLikeUpstreamRegression`; `collectsMidiTimeSignatureEventsForFf58ExportLikeUpstreamRegression`; raw metadata regressions | done |
| lines 1875-2078 (8 cases): triplet/articulation playback timing, duplicate FF58 suppression, MKS/standard metadata precedence, and Viola clef policy | `keepsStableTripletEighthTimingInMusicXmlPlaybackExtraction`; notation/time-signature/metadata/Viola regressions in `MidiIoTest` | done |
| `midi-roundtrip-golden.spec.ts` generated cases for `base.musicxml`, `interleaved_voices.musicxml`, and `roundtrip_piano_tempo.musicxml` | `roundtripsGoldenFixturesThroughMidiKeepingKeyMeterTempoBaseline` | done |
| `midi-roundtrip-golden.spec.ts`: `roundtrip_moonlight_m13_m16_like.musicxml` practical event multiset ceiling | `keepsPracticalNoteTimingAndPitchCloseForMoonlightGoldenFragment` | done |
| `midi-roundtrip-golden.spec.ts`: `roundtrip_triplet_m1_m4_like.musicxml` onset/pitch and duration-ratio envelope | `keepsOnsetAndPitchCloseForTripletGoldenFragmentWithDurationRatioTolerance` | done |

The complete source/export map is
[upstream-midi-io-source-map.md](upstream-midi-io-source-map.md). The only
remaining MIDI-specific non-unit cases are local-fixture spot checks recorded
separately in [upstream-midi-nonunit-case-map.md](upstream-midi-nonunit-case-map.md).
