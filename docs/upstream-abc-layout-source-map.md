# Pinned upstream ABC layout source map

This source-level map covers `src/ts/abc-layout.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. Its four exports are
runtime independent and included in the Java parity scope.

| Upstream export | Java implementation | Java regression | Status |
| --- | --- | --- | --- |
| `parseAbcScoreLayout` | `AbcIo#parseAbcScoreLayout` | `AbcIoTest#parsesAndBuildsAbcScoreLayoutsWithFallbackVoices` | done evidence |
| `buildAbcParsedPartsFromLayout` | `AbcIo#buildAbcParsedPartsFromLayout` | `AbcIoTest#parsesAndBuildsAbcScoreLayoutsWithFallbackVoices`, `AbcIoTest#abcImportRetainsUnknownScoreLayoutVoiceAsFallbackStaff` | done evidence |
| `hasAbcGroupedStaffVoices` | `AbcIo#hasAbcGroupedStaffVoices` | `AbcIoTest#buildsGroupedStaffMeasuresAndPartBodies` | done evidence |
| `buildAbcGroupedStaffMeasureNotesXml` | `AbcIo#buildAbcGroupedStaffMeasureNotesXml` | `AbcIoTest#buildsGroupedStaffMeasuresAndPartBodies` | done evidence |

`musicXmlFromAbc` now uses the public layout helpers directly. This preserves
the source's group ordering, duplicate and invalid ID handling, declared-voice
fallback order, default `"1"` layout, grouped part names, and the fallback
staff created when a score directive names a voice with no normalized data.
