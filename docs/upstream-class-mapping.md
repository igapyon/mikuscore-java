# Upstream Class Mapping

This document maps upstream `mikuscore` files to Java class groups.

The goal is traceability, not Java-first redesign.

## Mapping Status

| Upstream file | Java class / package | Status | Notes |
| --- | --- | --- | --- |
| `core/ScoreCore.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Basic command catalog migrated: `change_to_pitch`, `change_duration` with triplet context guard and rest consume/fill timing subset, simple `insert_note_after`, simple `delete_note`, `split_note` with overfull timing revalidation subset, and `ui_noop` |
| `core/commands.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlCommandValidation`, `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Basic command node-id/no-op behavior migrated |
| `core/interfaces.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlCommandValidation` | partial | Command validation result and diagnostics subset |
| `core/validators.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Basic command payload / target validation, overfull subset, and structural boundary subset |
| `core/timeIndex.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | Measure capacity / occupied-time subset for overfull validation, triplet context detection, rest consumption on duration expansion, underfull rest fill after shortening, and split occupied-time projection |
| `src/ts/beam-common.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlIo`, `jp.igapyon.mikuscore.abc.AbcIo` | partial | Implicit beam assignment subset used by `applyImplicitBeamsToMusicXmlText`; ABC measure beam XML assignment subset used by `buildAbcBeamXmlByNoteIndex` |
| `core/accidentalSpelling.ts` | pending | not started | Pitch spelling helper |
| `core/staffClefPolicy.ts` | pending | not started | Staff / clef policy |
| `core/xmlUtils.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | `setPitch`, `setDurationValue`, `createNoteElement`, `createRestElement`, `replaceWithRestNote`, chord-head delete promotion, clone-based split, and simple duration notation helpers |
| `src/ts/musicxml-io.ts` | `jp.igapyon.mikuscore.musicxml.MusicXmlIo`, `jp.igapyon.mikuscore.musicxml.MusicXmlState` | partial | MusicXML DOM parse / serialize, pretty print, imported-text normalization subset for part-list / part id / tuplet notation / final barline, and explicit implicit beam pass; state commands still use local DOM helpers |
| `src/ts/mxl-io.ts` | `jp.igapyon.mikuscore.musicxml.MxlIo` | partial | MXL container extraction / encoding subset |
| `src/ts/zip-io.ts` | `jp.igapyon.mikuscore.musicxml.MxlIo` | partial | ZIP text extraction by extension, root-entry listing, and MXL zip encoding subset |
| `src/ts/abc-io.ts` | `jp.igapyon.mikuscore.abc.AbcIo` | partial | Fraction, ABC length token parse/format, pitch, accidental, key, tempo unit, abcjs wrapper, measure duration estimate, ABC key fifths, `%@mks` meta directive, import line processor, body text entry, voice directive tail, header parsing, voice measure meta, MusicXML export XML, part measure render context, rendered measure misc XML, rendered part measure XML, part list/body XML integration, score-partwise document wrapper, MusicXML export context, measure note XML core, note lyric/time-modification XML, note leading direction XML, measure beam XML, note notations decoration XML, body import voice stores helper, body lyric application, body field state update, body barline processing, non-playable body entry dispatch, simple body token dispatch, bracket / grace / fallback body dispatch, pending note state helper, ABC chord harmony XML helper, MusicXML to ABC harmony / lyric helper, MusicXML to ABC DOM utility helper, MusicXML to ABC lane definition helper, MusicXML to ABC meta line helper, MusicXML to ABC measure meta helper, MusicXML to ABC measure state helper, MusicXML to ABC direction token helper, MusicXML to ABC note lane / timing helper, MusicXML to ABC note ornament helper, MusicXML to ABC pitch token helper, MusicXML to ABC note ornament prefix helper, MusicXML to ABC note articulation prefix helper, MusicXML to ABC note technical prefix helper, first `parseForMusicXml` / `musicXmlFromAbc` integration, initial `musicXmlToAbc` integration, MusicXML to ABC harmony / direction / lyric public integration, MusicXML to ABC grace / tie / slur public integration, MusicXML to ABC tuplet / time-modification public integration, MusicXML to ABC note notation / fermata prefix public integration, MusicXML to ABC trill accidental metadata public integration, MusicXML to ABC measure / diagnostic metadata public integration, MusicXML to ABC repeat / ending barline public integration, ABC quoted-string import to harmony / words direction handoff, initial ABC golden fixture roundtrip, ABC body tuplet timing, ABC body grace group import, ABC overlay import integration, basic / standard-shorthand / prefixed ABC body decoration pending state, ABC body navigation / wedge / dynamics decoration import, ABC richer decoration aliases, ABC overfull compatibility diagnostics, ABC body repeat / ending metadata, ABC body tie handoff, ABC chord tie handoff, missing voice measure rest fallback, grace-note occupancy exclusion, slur warning, and ABC body broken rhythm / slur handoff slices migrated; broader fixture-based parity expansion remains pending |
| `src/ts/abc-layout.ts` | `jp.igapyon.mikuscore.abc.AbcIo` | partial | Grouped staff voice detection and grouped-staff measure note XML helper migrated for ABC MusicXML export composition |
| `src/ts/abc-lexer.ts` | `jp.igapyon.mikuscore.abc.AbcLexer` | partial | Low-level ABC length, accidental, and note lexer helpers migrated |
| `src/ts/abc-parser.ts` | `jp.igapyon.mikuscore.abc.AbcParser` | partial | Playable-event, field/repeat/barline, span/decoration, broken-rhythm/shorthand/tie/slur, body dispatcher, and grace group helper slices migrated; first ABC I/O integration uses playable-event and barline parsing |
| `src/ts/musescore-io.ts` | pending | not started | MuseScore I/O |
| `src/ts/midi-io.ts` | pending | not started | MIDI I/O |
| `src/ts/mei-io.ts` | pending | not started | MEI I/O |
| `src/ts/lilypond-io.ts` | pending | not started | LilyPond I/O |
| `src/ts/vsqx-io.ts` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints are intentionally excluded from the initial Java target |
| `src/ts/cli-api.ts` | `jp.igapyon.mikuscore.coreapi.CoreApi`, `jp.igapyon.mikuscore.musicxml.MusicXmlState`, `jp.igapyon.mikuscore.musicxml.MxlIo` | partial | `summarizeMusicXmlState`, `inspectMusicXmlMeasure`, `validateMusicXmlCommand`, `applyMusicXmlCommand`, `diffMusicXmlState`, and the first MusicXML/MXL CLI file I/O bridge migrated |
| `scripts/mikuscore-cli.mjs` | `jp.igapyon.mikuscore.cli.MikuscoreCli` | partial | `--help`, `--version`, first `convert --from musicxml --to musicxml` slice, first `convert --from abc --to musicxml` slice, `state summarize`, `state inspect-measure`, `state validate-command`, `state apply-command`, and `state diff` |

## Out of Scope Initially

| Upstream area | Java handling |
| --- | --- |
| Web UI source | Out of Java initial conversion scope |
| Browser DOM flow | Out of Java initial conversion scope |
| Browser download / preview flow | Review only when it exposes product semantics |
