# Upstream Follow-up Log

This document records upstream-following questions, parity gaps, and conversion constraints found during `miku-score-java` straight conversion.

## Open Items

None. The original local reference assets are absent from the pinned upstream
checkout, and their observable MIDI, MEI, and MuseScore predicates are covered
by versioned compact behavior-equivalent Java fixtures.

VSQX, Verovio/SVG, browser UI/WebAudio, and Node/Web packaging are explicit
scope exclusions, documented in `remaining-migration-items.md`.

## Closed Items

### MuseScore public conversion bridge

- Status: closed for all versioned public source, unit, sample, integration,
  and round-trip evidence on 2026-08-10
- Area: upstream `src/ts/musescore-io.ts` / `src/ts/cli-api.ts`, Java
  `MuseScoreIo` / `CoreApi` / `MikuscoreCli`
- Note: Java now exposes public MusicXML DOM to MSCX and MSCX to MusicXML
  conversion rather than emitting an empty score or reading just the first
  voice. The bridge carries score metadata, parts, staffs, measures, voices,
  pitched chords, rests, and key/time/clef basics, including `.mscz` I/O.
  The 93 public cases and every public source export are individually mapped;
  CFFP covers the shared cross-format semantics. The separately named
  local-reference predicates are complete through the non-unit map's compact
  equivalents.

### Core state lifecycle bridge

- Status: closed for the lifecycle slice on 2026-08-09
- Area: upstream `core/ScoreCore.ts`, Java `core.ScoreCore` / `MusicXmlState`
- Note: Java now provides a stateful `load` / `dispatch` / `save` facade with
  clean-original versus dirty-serialized save modes, dirty state, deterministic
  note IDs, and debug serialization. It delegates all command semantics to
  `MusicXmlState`, so API and CLI validation agree. `editableVoice` is
  normalized at construction, rejects non-configured voices before mutation,
  and scopes save-time overfull timing validation to the configured voice.
  Stable node identity is retained through pitch/duration edits, insert,
  split, delete-to-rest, and chord-head deletion; internal markers are
  stripped from debug and saved XML. The remaining core work is limited to
  command-result and validator/XML utility edge behavior.

### Voice-lane timing parity for state edits

- Status: closed for the current Java core scope on 2026-08-09
- Area: `core/ScoreCore.ts`, `core/timeIndex.ts`, `core/validators.ts`, and
  Java `MusicXmlState` / `CoreApi` / `MikuscoreCli`
- Upstream reference: `change_duration`, `insert_note_after`, and
  `split_note` dispatch paths in `ScoreCore.ts`
- Note: Java now preserves successful `MEASURE_UNDERFULL` warnings through
  `applyMusicXmlCommandWithWarnings`, `CoreApi.CliResult`, text diagnostics,
  and JSON diagnostics. Shortening a note where a backup/forward boundary
  prevents gap fill emits the same underfull warning. Split records the lane
  timing before mutation and rejects a changed occupied duration afterward;
  the adjacent split immediately before `<backup>` remains allowed. Focused
  `MusicXmlStateTest`, `CoreApiTest`, and `MikuscoreCliTest` regressions cover
  these paths.

### Canonical generated-name parity

- Status: closed for the current Java core scope on 2026-08-09
- Area: `src/ts/download-flow.ts`, `src/ts/abc-io.ts`, `src/ts/mei-io.ts`,
  `src/ts/musescore-io.ts` and their Java counterparts
- Note: user-facing fallback titles and generated download filename stems now
  use the renamed canonical product name `miku-score`. The MIDI
  `app=mikuscore` metadata identifier remains intentionally unchanged for
  compatibility.

### CLI diagnostics parity

- Status: closed for the current Java CLI slice on 2026-08-09
- Area: `scripts/miku-score-cli.mjs`, `MikuscoreCli`
- Upstream reference: `--diagnostics text|json` and the structured diagnostics
  envelope produced by the Node.js CLI.
- Note: Java accepts `--diagnostics text|json`. JSON mode preserves the
  primary stdout or `--out` result and emits a version 1 diagnostics envelope
  to stderr, including command, I/O, outcome, error type, and error code.
  Focused Java CLI tests cover successful and usage-error envelopes.

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

### Verovio-backed SVG rendering

- Status: closed as excluded from Java parity scope on 2026-08-09
- Area: `render svg`, `src/ts/verovio-out.ts`
- Note: Upstream SVG rendering depends on `window.verovio`, browser DOM
  serialization, and the Verovio JavaScript/WebAssembly runtime. No pure-Java
  renderer or external-runtime strategy is selected, and SVG rendering is not
  a Java parity goal. The CLI may retain its explicit unsupported response.

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
