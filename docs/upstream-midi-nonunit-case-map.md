---
title: MIDI non-unit case map
status: done
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# MIDI non-unit case map

| Upstream evidence | Java-equivalent evidence | Status |
| --- | --- | --- |
| `tests/spot/local-musicxml-to-midi-moonlight.spot.spec.ts` — semantic reference-MIDI comparison | `MidiIoTest#keepsCompactMoonlightEquivalentPracticalAcrossAllPinnedQuantizationGrids` uses the versioned `roundtrip_moonlight_m13_m16_like.musicxml` score fragment, writer-compatible export, MIDI import, and the upstream practical pitch/onset/duration-ratio comparison at `1/8`, `1/16`, and `1/32`. | done |
| same file — focused first-eight-measure practical-diff comparison | `MidiIoTest#keepsPracticalNoteTimingAndPitchCloseForMoonlightGoldenFragment` provides the focused semantic multiset guard; the all-grid equivalent above covers the source test's quantization loop. | done |

The original Moonlight MusicXML/MIDI pair is deliberately untracked upstream
(`tests/local-data` contains only `.gitkeep`). Its exact bytes therefore cannot
be mirrored from the pinned revision. The checked-in compact fixture is an
original, behavior-equivalent replacement: it locks the observable MIDI
conversion contract without redistributing the unavailable source assets.
The four `src/samples/midi/sample*.mid` files are source samples, not an
additional executable MIDI parity case family.
