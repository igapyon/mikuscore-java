# Upstream Follow-up Log

This document records upstream-following questions, parity gaps, and conversion constraints found during `miku-score-java` straight conversion.

## Open Items

### CLI diagnostics parity

- Status: open, recorded 2026-08-06
- Area: `scripts/mikuscore-cli.mjs`, `MikuscoreCli`
- Upstream reference: `--diagnostics text|json` and the structured error
  diagnostics produced by the Node.js CLI.
- Note: Java currently keeps primary output on stdout and failures on stderr
  with exit codes `0` / `1` / `2`, but does not accept a diagnostics-format
  option or emit the upstream JSON diagnostics envelope. Preserve that
  difference explicitly in help and mapping documents until a bounded,
  testable Java parity slice is selected.

### `change_duration` remaining timing parity

- Status: open, narrowed 2026-05-09
- Area: `core/ScoreCore.ts`, `core/timeIndex.ts`, `core/xmlUtils.ts`
- Upstream reference: `change_duration` dispatch path in `ScoreCore.ts`
- Note: Java migrates payload/target validation, `<duration>` mutation, simple notation metadata sync, triplet duration rejection without tuplet context, overfull validation, following/preceding rest consumption for duration expansion, and trailing rest fill for shortened durations. Remaining parity work is deeper timing-warning behavior around complex voice lanes.

### `insert_note_after` timing parity

- Status: open, narrowed 2026-05-09
- Area: `core/ScoreCore.ts`, `core/timeIndex.ts`
- Upstream reference: `insert_note_after` dispatch path in `ScoreCore.ts`
- Note: Java currently migrates payload/anchor validation, adjacent pitched-note insertion, overfull validation, same-lane / backup-forward boundary checks, and the `MEASURE_UNDERFULL` warning subset for `validate-command`. Remaining parity work is preserving underfull warnings through a structured successful apply result if the Java apply API grows beyond XML-only success output.

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
- 2026-05-14 note: The current Verovio JavaScript toolkit is an Emscripten WebAssembly / JS build of the C++ Verovio engine, not hand-written JavaScript that can be straight-converted to Java. The native Verovio Java binding uses JNI, which is outside the desired Java 1.8 pure-Java direction.
- 2026-05-14 scale check: a shallow Verovio clone showed roughly 312K lines under `src`, roughly 357K lines for `src` + `include/vrv` + `tools`, and still roughly 156K lines after excluding bundled / large support areas such as Humdrum, pugi, MIDI, JSON, and `iohumdrum`. This scale is the practical background for keeping Java-side `render svg` unsupported in this repository.

## Closed Items

### `core/accidentalSpelling.ts` helper parity

- Status: closed for current Java helper slice
- Area: `core/accidentalSpelling.ts`
- Upstream reference: `midiToPitch`, `keySignatureAlterForStep`, `accidentalTextFromAlter`, `resolveAccidentalTextForPitch`
- Note: Java now has `AccidentalSpelling` with the upstream helper behavior covered by focused JUnit tests. `MusicXmlState` uses the migrated accidental text mapping for pitch edits and inserted notes.

### `core/staffClefPolicy.ts` helper parity

- Status: closed for current Java helper slice
- Area: `core/staffClefPolicy.ts`
- Upstream reference: `shouldUseGrandStaffByRange`, `chooseSingleClefByKeys`, `pickStaffByPitchWithHysteresis`, `pickStaffForClusterWithHysteresis`
- Note: Java now has `StaffClefPolicy` with the upstream thresholds and helper behavior covered by focused JUnit tests. `AbcIo` and `MusicXmlState` delegate their already-migrated clef/staff decisions to the shared helper.

### `change_to_pitch` grand-staff staff assignment

- Status: closed for current Java basic command slice
- Area: `core/ScoreCore.ts`, `core/staffClefPolicy.ts`
- Upstream reference: `autoAssignGrandStaffByPitch` and `pickStaffByPitchWithHysteresis`
- Note: Java now updates `<staff>` after `change_to_pitch` when the edited note is in an inherited two-staff G/F grand-staff context. The staff choice follows the upstream hysteresis thresholds.

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
