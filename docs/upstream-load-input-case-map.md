# Pinned upstream load-input case map

This map covers `tests/unit/load-input.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. Java's
`CoreApi.convertLoadInputToMusicXml` is deliberately value based: it receives
a format plus `String` or `byte[]`, rather than browser file objects, while
retaining the source result fields and structured MIDI diagnostics.

| Upstream case | Java regression / owner | Status |
| --- | --- | --- |
| normalizes declared MusicXML text without browser file objects | `CoreApiTest#convertsPinnedValueBasedLoadInputsWithStructuredDiagnostics` | done evidence |
| delegates declared ABC text | `CoreApiTest#resolvesPinnedDirectLoadFlowAndNewScoreResultShapes` | done evidence |
| preserves structured MIDI diagnostics and warnings on failure | `CoreApiTest#convertsPinnedValueBasedLoadInputsWithStructuredDiagnostics` | done evidence |
| decodes MXL bytes before normalizing MusicXML | `CoreApiTest#convertsPinnedValueBasedLoadInputsWithStructuredDiagnostics` | done evidence |
| prefers MSCX entry when decoding MSCZ | `CoreApiTest#resolvesLoadFileMuseScoreZipAndMusicXmlFallback` | done evidence |
| accepts MXL-compatible archive through MSCZ path | `CoreApiTest#resolvesLoadFileMuseScoreZipAndMusicXmlFallback` | done evidence |
| rejects a payload kind mismatching declared format | `CoreApiTest#convertsPinnedValueBasedLoadInputsWithStructuredDiagnostics` | done evidence |

VSQX conversion is excluded separately. The facade returns the source
`MVP_INVALID_COMMAND_PAYLOAD` result code for a failed conversion, including
when a MIDI diagnostic keeps its more specific code in the diagnostics array.
