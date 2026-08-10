# Pinned upstream musicxml-output case map

This map covers `tests/unit/musicxml-output.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. Both metadata policy and
diagnostic summary helpers are runtime independent.

| Upstream case | Java regression | Status |
| --- | --- | --- |
| returns original text unchanged when all metadata families are kept | `MusicXmlOutputTest#retainsOriginalTextWhenAllMetadataFamiliesAreKept` | done evidence |
| removes selected mks families and prunes empty containers | `MusicXmlOutputTest#removesSelectedMetadataFamiliesAndPrunesEmptyContainers` | done evidence |
| keeps invalid input unchanged | `MusicXmlOutputTest#retainsInvalidMetadataInputUnchanged` | done evidence |
| summarizes existing ABC warning categories and skips the count field | `MusicXmlOutputTest#summarizesExistingAbcDiagnosticWarningCategories` | done evidence |
| returns an empty summary for invalid MusicXML | `MusicXmlOutputTest#returnsEmptyDiagnosticSummaryForInvalidMusicXml` | done evidence |

`MusicXmlOutput` keeps the source's strict `mks:` field prefix selection,
lowercased metadata-family policy, empty `miscellaneous`/`attributes`
pruning, and the two recognized diagnostic categories.
