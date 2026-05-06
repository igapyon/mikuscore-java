# Upstream Class Mapping

This document maps upstream `mikuscore` files to Java class groups.

The goal is traceability, not Java-first redesign.

## Mapping Status

| Upstream file | Java class / package | Status | Notes |
| --- | --- | --- | --- |
| `core/ScoreCore.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Basic command catalog migrated: `change_to_pitch`, simple `change_duration`, simple `insert_note_after`, simple `delete_note`, simple `split_note`, and `ui_noop` |
| `core/commands.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlCommandValidation`, `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Basic command node-id/no-op behavior migrated |
| `core/interfaces.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlCommandValidation` | partial | Command validation result and diagnostics subset |
| `core/validators.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Basic command payload / target validation, overfull subset, and structural boundary subset |
| `core/timeIndex.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Measure capacity / occupied-time subset for overfull validation |
| `src/ts/beam-common.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlIo` | partial | Implicit beam assignment subset used by `applyImplicitBeamsToMusicXmlText` |
| `core/accidentalSpelling.ts` | pending | not started | Pitch spelling helper |
| `core/staffClefPolicy.ts` | pending | not started | Staff / clef policy |
| `core/xmlUtils.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | `setPitch`, `setDurationValue`, `createNoteElement`, `replaceWithRestNote`, chord-head delete promotion, clone-based split, and simple duration notation helpers |
| `src/ts/musicxml-io.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlIo`, `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | MusicXML DOM parse / serialize, pretty print, imported-text normalization subset for part-list / part id / tuplet notation / final barline, and explicit implicit beam pass; state commands still use local DOM helpers |
| `src/ts/mxl-io.ts` | `jp.igapyon.mikuscore.musicxml.MxlIo` | partial | MXL container extraction / encoding subset |
| `src/ts/zip-io.ts` | `jp.igapyon.mikuscore.musicxml.MxlIo` | partial | ZIP text extraction by extension, root-entry listing, and MXL zip encoding subset |
| `src/ts/abc-io.ts` | `jp.igapyon.mikuscore.abc.AbcIo` | partial | Fraction, ABC length token parse/format, pitch, accidental, key, tempo unit, abcjs wrapper, measure duration estimate, ABC key fifths, `%@mks` meta directive, import line processor, body text entry, voice directive tail, header parsing, and voice measure meta helper slices migrated; body import and MusicXML export integration remain pending |
| `src/ts/abc-lexer.ts` | `jp.igapyon.mikuscore.abc.AbcLexer` | partial | Low-level ABC length, accidental, and note lexer helpers migrated |
| `src/ts/abc-parser.ts` | `jp.igapyon.mikuscore.abc.AbcParser` | partial | Playable-event, field/repeat/barline, span/decoration, broken-rhythm/shorthand/tie/slur, body dispatcher, and grace group helper slices migrated; ABC I/O integration remains pending |
| `src/ts/musescore-io.ts` | pending | not started | MuseScore I/O |
| `src/ts/midi-io.ts` | pending | not started | MIDI I/O |
| `src/ts/mei-io.ts` | pending | not started | MEI I/O |
| `src/ts/lilypond-io.ts` | pending | not started | LilyPond I/O |
| `src/ts/vsqx-io.ts` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints are intentionally excluded from the initial Java target |
| `src/ts/cli-api.ts` | `jp.igapyon.mikuscore.coreapi.CoreApi`, `jp.igapyon.mikuscore.musicxml.MusicXmlState`, `jp.igapyon.mikuscore.musicxml.MxlIo` | partial | `summarizeMusicXmlState`, `inspectMusicXmlMeasure`, `validateMusicXmlCommand`, `applyMusicXmlCommand`, `diffMusicXmlState`, and the first MusicXML/MXL CLI file I/O bridge migrated |
| `scripts/mikuscore-cli.mjs` | `jp.igapyon.mikuscore.cli.MikuscoreCli` | partial | `--help`, `--version`, first `convert --from musicxml --to musicxml` slice, `state summarize`, `state inspect-measure`, `state validate-command`, `state apply-command`, and `state diff` |

## Out of Scope Initially

| Upstream area | Java handling |
| --- | --- |
| Web UI source | Out of Java initial conversion scope |
| Browser DOM flow | Out of Java initial conversion scope |
| Browser download / preview flow | Review only when it exposes product semantics |
