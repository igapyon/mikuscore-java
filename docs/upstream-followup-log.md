# Upstream Follow-up Log

This document records upstream-following questions, parity gaps, and conversion constraints found during `mikuscore-java` straight conversion.

## Open Items

### `change_duration` remaining timing parity

- Status: open, narrowed 2026-05-09
- Area: `core/ScoreCore.ts`, `core/timeIndex.ts`, `core/xmlUtils.ts`
- Upstream reference: `change_duration` dispatch path in `ScoreCore.ts`
- Note: Java migrates payload/target validation, `<duration>` mutation, simple notation metadata sync, triplet duration rejection without tuplet context, overfull validation, following/preceding rest consumption for duration expansion, and trailing rest fill for shortened durations. Remaining parity work is deeper timing-warning behavior around complex voice lanes.

### `insert_note_after` timing parity

- Status: open
- Area: `core/ScoreCore.ts`, `core/timeIndex.ts`
- Upstream reference: `insert_note_after` dispatch path in `ScoreCore.ts`
- Note: Java currently migrates payload/anchor validation, adjacent pitched-note insertion, overfull validation, and same-lane / backup-forward boundary checks. Underfull timing warnings still require later `timeIndex` work.

### `split_note` timing parity

- Status: open, narrowed 2026-05-09
- Area: `core/ScoreCore.ts`, `core/timeIndex.ts`
- Upstream reference: `split_note` dispatch path in `ScoreCore.ts`
- Note: Java currently migrates even-duration validation, clone-based adjacent split, forward-boundary rejection, and the overfull timing revalidation subset that rejects an already-overfull edited lane. Remaining parity work is the deeper post-split lane-timing invariant / restore behavior around complex backup-forward layouts.

### SVG render runtime

- Status: open
- Area: `render svg`
- Upstream reference: `src/ts/verovio-out.ts`, browser/runtime-related render flow
- Note: Upstream `renderMusicXmlDomToSvg` depends on `window.verovio`, `verovio.js` runtime initialization, and browser DOM serialization. Java direct conversion is therefore not part of the current initial slice. Keep Java `render svg` unsupported until a Java-compatible renderer runtime or explicit external-runtime strategy is chosen.

## Closed Items

### ABC trill accidental metadata roundtrip

- Status: closed for current Java ABC conversion slice
- Area: `src/ts/abc-io.ts`
- Upstream reference: `MusicXML->ABC stores trill accidental-mark in mikuscore comment and restores it`
- Note: Java now emits `%@mks trill ... upper=...` from MusicXML `ornaments/accidental-mark` and applies the meta hint back to the imported ABC playable event during MusicXML export.

### VSQX conversion bridge

- Status: closed for initial Java conversion scope
- Area: `src/ts/vsqx-io.ts`
- Note: VSQX conversion is intentionally excluded from the initial Java straight-conversion target because the upstream path depends on a bridge / dependency shape that is not a direct Java conversion slice.

### `delete_note` structural parity

- Status: closed for current Java basic command slice
- Area: `core/ScoreCore.ts`, `core/xmlUtils.ts`
- Upstream reference: `delete_note` dispatch path and `replaceWithRestNote`
- Note: Java migrates non-rest, non-chord target validation, same-duration rest replacement, backup/forward boundary rejection, and chord-head promotion of the next chord tone.

### `musicxml-io` normalization parity

- Status: closed for current Java MusicXML I/O slice
- Area: `src/ts/musicxml-io.ts`, `src/ts/beam-common.ts`
- Upstream reference: `normalizeImportedMusicXmlText`, `applyImplicitBeamsToMusicXmlText`
- Note: Java migrates parse / serialize / pretty-print shape, part-list / part-id normalization, tuplet notation enrichment, final right barline normalization, and the explicit implicit-beam pass subset covered by upstream unit-test intent.
