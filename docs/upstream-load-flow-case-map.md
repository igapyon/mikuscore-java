# Pinned upstream load-flow case map

This map covers `tests/unit/load-flow.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. Browser `File`,
`ArrayBuffer`, and input-section UI plumbing are represented by the Java
byte-array/path facade `CoreApi.resolveLoadFileToMusicXml`; only their
runtime-independent routing and diagnostic semantics are included here.

| Upstream case | Java regression / owner | Status |
| --- | --- | --- |
| accepts `.mid` and converts via `convertMidiToMusicXml` | `CoreApiTest#resolvesLoadFileMusicXmlAndMidiRoutes` | done evidence |
| returns load failure when MIDI conversion reports diagnostics | `CoreApiTest#reportsPinnedLoadFlowMidiDiagnosticWithItsCode` | done evidence |
| accepts `.mei` and converts via `convertMeiToMusicXml` | `CoreApiTest#resolvesLoadFileMeiLilyPondAndMuseScoreRoutes` | done evidence |
| normalizes direct MusicXML file input | `CoreApiTest#resolvesLoadFileMusicXmlAndMidiRoutes` | done evidence |
| accepts `.vsqx` and converts via `convertVsqxToMusicXml` | VSQX conversion | excluded |
| returns load failure when VSQX conversion reports diagnostics | VSQX conversion | excluded |
| accepts `.ly` and converts via `convertLilyPondToMusicXml` | `CoreApiTest#resolvesLoadFileMeiLilyPondAndMuseScoreRoutes` | done evidence |
| accepts `.mscx` and converts via `convertMuseScoreToMusicXml` | `CoreApiTest#resolvesLoadFileMeiLilyPondAndMuseScoreRoutes` | done evidence |
| delegates direct ABC text and preserves editor source text | `CoreApiTest#resolvesPinnedDirectLoadFlowAndNewScoreResultShapes` | done evidence |
| keeps missing-file and unsupported-extension policy in the adapter | `CoreApiTest#rejectsLoadFileUnsupportedAndUnavailableRoutes` | done evidence |

## Source-only non-file outcome branches

| Upstream branch | Java regression / owner | Status |
| --- | --- | --- |
| `isNewType` creates a successful collapsed load with next XML text | `CoreApiTest#resolvesPinnedDirectLoadFlowAndNewScoreResultShapes` | done evidence |
| direct `xml` / `abc` / `mei` / `lilypond` / `musescore` selector mapping | `CoreApi.resolveDirectLoadFlow`; `CoreApiTest#resolvesPinnedDirectLoadFlowAndNewScoreResultShapes`, `#resolvesLoadFileMeiLilyPondAndMuseScoreRoutes` | done evidence |
| file reading through `File` and `FileReader` | Browser file API | excluded |

The same Java facade also has direct coverage for MXL, MSCZ, MSCZ-to-MXL
fallback, missing/unsupported paths, and MXL/MSCZ parse diagnostics. The
value-based conversion cases are separately mapped in
[`upstream-load-input-case-map.md`](upstream-load-input-case-map.md).
