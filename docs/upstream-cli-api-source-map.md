# Pinned upstream CLI API source map

This map inventories every runtime-independent public export in
`src/ts/cli-api.ts` at upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`. A facade row marked **done
evidence** verifies routing and its `CliResult` contract; conversion-depth
semantics remain owned by the corresponding format item in the parity matrix.

| Upstream export | Java owner and evidence | Status |
| --- | --- | --- |
| `CliResult` | `CoreApi.CliResult`; text/byte output, warning list, diagnostic list, and compatible first-diagnostic accessor are covered by `CoreApiTest#retainsThePinnedMidiImportDiagnosticsArray` | done evidence |
| `decodeCliMusicXmlInput` | `CoreApi.decodeCliMusicXmlInput`; `CoreApiTest#decodesMusicXmlTextInputAsCliResult`, `#decodesMxlInputAsCliResult` | done evidence |
| `decodeCliMuseScoreInput` | `CoreApi.decodeCliMuseScoreInput`; `CoreApiTest#decodesMuseScoreZipInputAsCliResult` | done evidence |
| `encodeCliMusicXmlOutput` | `CoreApi.encodeCliMusicXmlOutput`; `CoreApiTest#encodesMxlOutputAsCliResult` | done evidence |
| `encodeCliMuseScoreOutput` | `CoreApi.encodeCliMuseScoreOutput`; `CoreApiTest#encodesMuseScoreZipOutputAsCliResult` | done evidence |
| `importAbcToMusicXml` | `CoreApi.importAbcToMusicXml`; `CoreApiTest#importsAbcToMusicXmlAsCliResult`, `#reportsThePinnedCliApiInvalidAbcDiagnostic` | done facade evidence |
| `exportMusicXmlToAbc` | `CoreApi.exportMusicXmlToAbc`; `CoreApiTest#exportsMusicXmlToAbcAsCliResult`, `#rejectsInvalidMusicXmlWithThePinnedCliApiDiagnosticForEveryExportFacade` | done facade evidence |
| `importMidiToMusicXml` | `CoreApi.importMidiToMusicXml`; `CoreApiTest#importsMidiToMusicXmlAsCliResult`, `#retainsThePinnedMidiImportDiagnosticsArray` | done facade evidence |
| `exportMusicXmlToMidi` | `CoreApi.exportMusicXmlToMidi`; `CoreApiTest#exportsMusicXmlToMidiAsCliResult`, `#rejectsMusicXmlToMidiWhenNoPlayableEventsExist`, `#rejectsInvalidMusicXmlWithThePinnedCliApiDiagnosticForEveryExportFacade` | done facade evidence |
| `importMuseScoreToMusicXml` | `CoreApi.importMuseScoreToMusicXml`; `CoreApiTest#importsMuseScoreToMusicXmlAsCliResult` | done facade evidence |
| `exportMusicXmlToMuseScore` | `CoreApi.exportMusicXmlToMuseScore`; `CoreApiTest#exportsMusicXmlToMuseScoreAsCliResult`, `#rejectsInvalidMusicXmlWithThePinnedCliApiDiagnosticForEveryExportFacade` | done facade evidence |
| `importMeiToMusicXml` | `CoreApi.importMeiToMusicXml`; `CoreApiTest#importsMeiToMusicXmlAsCliResult` | done facade evidence |
| `exportMusicXmlToMei` | `CoreApi.exportMusicXmlToMei`; `CoreApiTest#exportsMusicXmlToMeiAsCliResult`, `#rejectsInvalidMusicXmlWithThePinnedCliApiDiagnosticForEveryExportFacade` | done facade evidence |
| `importLilyPondToMusicXml` | `CoreApi.importLilyPondToMusicXml`; `CoreApiTest#importsLilyPondToMusicXmlAsCliResult` | done facade evidence |
| `exportMusicXmlToLilyPond` | `CoreApi.exportMusicXmlToLilyPond`; `CffpSeriesTest#roundtripsEveryPinnedCffpCaseThroughEachIncludedBridge`, `CoreApiTest#rejectsInvalidMusicXmlWithThePinnedCliApiDiagnosticForEveryExportFacade` | done facade evidence |
| `renderMusicXmlToSvg` | Verovio/browser runtime | excluded |
| `summarizeMusicXmlState` | `CoreApi.summarizeMusicXmlState`; `CoreApiTest#summarizesMusicXmlStateAsCliResult` | done evidence |
| `validateMusicXmlCommand` | `CoreApi.validateMusicXmlCommand`; `CoreApiTest#validatesAndInspectsMusicXmlStateThroughTheCliApiFacade`, `#reportsPinnedCliApiSelectorResolutionDiagnostics`, `#resolvesPinnedCliSelectorsWithEmptyPartMeasureAndVoiceValues` | done evidence |
| `applyMusicXmlCommand` | `CoreApi.applyMusicXmlCommand`; `CoreApiTest#appliesMusicXmlCommandAndRetainsSuccessfulTimingWarnings` | done evidence |
| `inspectMusicXmlMeasure` | `CoreApi.inspectMusicXmlMeasure`; `CoreApiTest#validatesAndInspectsMusicXmlStateThroughTheCliApiFacade`, `MusicXmlStateTest#inspectionRetainsPinnedCliApiEmptySelectorFieldsAndFractionalDuration`, `#inspectionRetainsPinnedCliApiPitchNumberSemantics` | done evidence |
| `diffMusicXmlState` | `CoreApi.diffMusicXmlState`; `CoreApiTest#diffsMusicXmlStateThroughTheCliApiFacade` | done evidence |
| `cliApi` aggregate | Java exposes the same operations as `CoreApi` static methods; the excluded renderer is deliberately absent | done evidence |

`resolveLoadFileToMusicXml` and `create*DownloadPayload` are Java additions
that own the separately tracked pure load/download flows; they are not
`cli-api.ts` exports.
