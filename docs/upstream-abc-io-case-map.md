---
title: ABC I/O pinned case map
status: complete
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# ABC I/O pinned case map

This map completes the included ABC I/O test inventory for the renamed Node
upstream at `v0.6.1`. The pinned suite contains 408 `it` cases in
`tests/unit/abc-io.spec.ts`, one standalone inline-voice case, and four
golden fixtures. There are no ABC integration, property, corpus, or slow test
files in that revision.

The line ranges below are deliberately pinned to the recorded revision. Each
range covers every `it` invocation in that contiguous source range; a Java
test may exercise several upstream spelling/alias cases as one regression.

| Pinned upstream cases | Java evidence | Status |
| --- | --- | --- |
| `abc-io.spec.ts` lines 16-546 (22 cases): public facade roundtrip, positive voice values, import options, length syntax, grand/same-staff lanes, tempo, clefs, voice properties, reflow, and diagnostics | `roundtripsBundledAbcGoldenFixturesThroughMusicXmlToAbc`; `abcRoundtripKeepsGrandStaffLanesValidAndVoiceNumbersPositive`; `musicXmlFromAbcHonorsPublicMetadataAndPrettyPrintOptions`; `parsesSlashLengthShorthandIncludingDoubleSlashIntoMusicXml`; `parsesNumeratorSlashShorthandInNotesChordsAndGraceGroups`; tempo/clef/voice/reflow regressions in `AbcIoTest` | done |
| lines 564-819 (13 cases): inline/continued fields, quoted annotation and harmony parsing, rehearsal, and shorthand decorations | `abcImportSupportsInlineKeyMeterLengthAndTempoFields`; continued-field, quoted-harmony, rehearsal, and standard-decoration regressions in `AbcIoTest` | done |
| lines 849-1457 (19 cases): MusicXML export title/composer/key/meter defaults, leading direction/harmony ordering, and harmony/rehearsal roundtrips | `musicXmlToAbcUsesPublicHeaderFallbacksAndOmitsMissingComposer`; `convertsMusicXmlToAbcHarmonyDirectionAndLyrics`; `musicXmlToAbcExports*Harmony*`; `musicXmlToAbcExportsRehearsalDirectionAsDecorationAndRoundtrips` | done |
| lines 1490-1901 (14 cases): lyrics, inline voices, grouped score layouts, grouped metadata, and editorial/courtesy accidentals | lyric, inline-voice, `abcImportMapsScoreGrouped*`, `abcImportPreservesMixedScoreGroupedAndUngroupedOrdering`, `abcImportDeDuplicatesRepeatedIdsInsideScoreGroups`, and accidental regressions in `AbcIoTest` | done |
| lines 1927-2544 (32 cases): user decorations, unsupported-body diagnostics, pickups, malformed note content, and overlays | `abcImportSupportsUserDefined*`; `abcImportWarns*`; `abcImportMapsOverlaySyntaxIntoSyntheticOverlayVoices`; `abcImportKeepsLaterMeasureOverlayNotesAfterPlainMeasures` | done |
| lines 2567-4677 (117 cases): import decorations, aliases, notation ordering, technical marks, directions, dynamics, and beams | `abcImportParses*`; `abcImportAccepts*`; `abcImportRoundtrips*`; `parsesAbcStandardShorthandDecorationsIntoMusicXml`; `parsesAbcPrefixedDecorationsAndAccidentalAnnotationsIntoMusicXml`; beam regressions in `AbcIoTest` | done |
| lines 4725-5440 (22 cases): missing-voice rests, grace occupancy, diagnostics, slurs/ties, and grace/trill/turn export beginnings | `usesMeterSizedEmptyRestsForMissingAbcVoiceMeasures`; `doesNotTreatAbcGraceNotesAsMeasureOccupancy`; `exportsMusicXmlDiagnosticMiscFieldsAsAbcMksDiagLines`; slur/tie and grace/turn regressions in `AbcIoTest` | done |
| lines 5496-7106 (47 cases): tremolo/glissando/slide, trill, phrase/articulation, wedge, and dynamics export/alias roundtrips | `musicXmlToAbcExportsTremoloGlissandoAndSlideVariantsAndRoundtrips`; `musicXmlToAbcExportsLongTrillStartAndStopDecorations`; articulation, wedge, and dynamics regressions in `AbcIoTest` | done |
| lines 7125-8860 (58 cases): bowing/technical/fingering/string/pluck/open/snap/harmonic/thumb/mordent/arpeggio aliases and export roundtrips | bowing, technical, fingering/string/pluck, state-technical, mordent/arpeggio, and ornament regression groups in `AbcIoTest` | done |
| lines 8884-10987 (64 cases): navigation/dynamics/stopped/tie/slur/accidental/key/C-clef/metadata/tuplet/repeat and voice-less-grace export/import cases | navigation/dynamics/stopped/tie/slur/accidental/key/metadata/tuplet/repeat/voice-less-grace regressions in `AbcIoTest` | done |
| `abc-inline-voice-switch.spec.ts` (1 case) | `abcImportKeepsStandaloneInlineVoiceLineActiveForFollowingBodyLines` | done |
| `abc-roundtrip-golden.spec.ts` (4 fixtures: `base`, `with_backup_safe`, `interleaved_voices`, `roundtrip_piano_tempo`) | `roundtripsBundledAbcGoldenFixturesThroughMusicXmlToAbc`, which includes all four pinned fixtures plus 18 additional focused Java fixtures | done |

The direct source/export mapping remains in
[upstream-abc-io-source-map.md](upstream-abc-io-source-map.md). Lexer and
parser have separate complete maps. Together these maps close the included
ABC scope; no browser/UI, VSQX, Verovio/SVG, or Node distribution work is
being counted here.
