# Cross-format feature-parity (CFFP) case map

This document maps `tests/unit/cffp-series.spec.ts` from the renamed Node
upstream revision `a8adc1998237f7b371cae75728afec7dd1795977`.

## Source and execution boundary

The exact pinned upstream case definitions are retained as the test resource
[`upstream-cffp-series.spec.ts`](../src/test/resources/upstream-cffp-series.spec.ts).
`CffpSeriesTest` parses its 86 case definitions instead of maintaining a
second hand-copied fixture list, then executes every included bridge:

| Upstream bridge | Java evidence | Status |
| --- | --- | --- |
| MusicXML → ABC → MusicXML | `CffpSeriesTest` via `CoreApi` | done |
| MusicXML → MEI → MusicXML | `CffpSeriesTest` via `CoreApi` | done |
| MusicXML → LilyPond → MusicXML | `CffpSeriesTest` via `CoreApi` | done |
| MusicXML → MuseScore → MusicXML | `CffpSeriesTest` via `CoreApi` | done |
| MusicXML → MIDI → MusicXML | `CffpSeriesTest` via the public `MidiIo` event/writer/import bridge | done |
| MusicXML → VSQX → MusicXML | — | excluded — VSQX is outside the agreed scope |

For all 84 cases that request a pitched fact, the test checks the Node suite's
first-pitch step/octave, zero onset, and per-format duration policy. The two
unpitched/rest-only cases still execute all five included bridges and require a
successful MusicXML result. The MIDI bridge uses the CFFP 128-TPQ playback
and `1/16` import-quantization convention, including the non-raw
MidiWriterJS-compatible serializer.

## Exact upstream feature predicates

Nineteen CFFP cases declare feature preservation for one or more formats.
Their 44 included format assertions are checked by explicit Java DOM
predicates—not merely tag presence—covering the exact source conditions:

| Case group | Preserved semantics | Status |
| --- | --- | --- |
| `TRILL`, `SEGNO-CODA`, `OCTSHIFT` | ornament/navigation/direction markers and attributes | done |
| `ACCIDENTAL`, `ACCIDENTAL-RESET` | natural/sharp spelling, measure-boundary reset, and pitch alteration | done |
| `MULTIVOICE-BACKUP`, `PERCUSSION-VOICE-LAYER`, `GRANDSTAFF-MAPPING` | note voice and staff lane identities | done |
| `PICKUP-IMPLICIT`, `TIME-CHANGE` | implicit pickup metadata and 4/4 → 3/4 change | done |
| `REPEAT-ENDING`, `TEMPO-MAP` | forward/backward repeat directions and 120/90 tempo map | done |
| `SLUR`, `TIE`, `NOTE-TIES-CROSS-MEASURE` | start/stop pair placement, including adjacent measures | done |
| `STACCATO`, `ACCENT`, `GRACE`, `TUPLET` | notation and time-modification markers | done |

The test intentionally fails if a later upstream CFFP case gains a
`preserveByFormat: true` predicate without an explicit Java semantic mapping.

## Verification

- `mvn -q -Dtest=CffpSeriesTest test`
- `./node_modules/.bin/vitest run tests/unit/cffp-series.spec.ts` in the
  pinned upstream checkout: **86 passed** on 2026-08-10.

All included CFFP cases are complete. VSQX remains an explicit exclusion.
