# Pinned upstream CLI API case map

This is the atomic mapping for all 16 included cases in
`tests/unit/cli-api.spec.ts` at renamed Node upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`.  SVG rendering is not present in
this upstream unit suite; the separately exported Verovio renderer remains an
agreed exclusion.

| Upstream case | Java regression | Status |
| --- | --- | --- |
| imports ABC to MusicXML | `CoreApiTest#importsAbcToMusicXmlAsCliResult` | done evidence |
| fails on invalid ABC | `CoreApiTest#reportsThePinnedCliApiInvalidAbcDiagnostic` | done evidence |
| exports MusicXML to ABC | `CoreApiTest#exportsMusicXmlToAbcAsCliResult` | done evidence |
| fails on invalid MusicXML | `CoreApiTest#exportsMusicXmlToAbcAsCliResult` | done evidence |
| imports MIDI to MusicXML | `CoreApiTest#importsMidiToMusicXmlAsCliResult` | done evidence |
| exports MusicXML to MIDI bytes | `CoreApiTest#exportsMusicXmlToMidiAsCliResult` | done evidence |
| imports MuseScore to MusicXML | `CoreApiTest#importsMuseScoreToMusicXmlAsCliResult` | done evidence |
| exports MusicXML to MuseScore text | `CoreApiTest#exportsMusicXmlToMuseScoreAsCliResult` | done evidence |
| decodes `.mxl` input for CLI file reads | `CoreApiTest#decodesMxlInputAsCliResult` | done evidence |
| encodes `.mxl` output for CLI file writes | `CoreApiTest#encodesMxlOutputAsCliResult` | done evidence |
| decodes `.mscz` input for CLI file reads | `CoreApiTest#decodesMuseScoreZipInputAsCliResult` | done evidence |
| encodes `.mscz` output for CLI file writes | `CoreApiTest#encodesMuseScoreZipOutputAsCliResult` | done evidence |
| roundtrips MusicXML through `.mxl` helper I/O | `CoreApiTest#encodesMxlOutputAsCliResult`, `#decodesMxlInputAsCliResult` | done evidence |
| roundtrips MusicXML through `.mscz` helper I/O via MuseScore facade | `CoreApiTest#roundTripsMusicXmlThroughPinnedCliApiMuseScoreZipHelpers` | done evidence |

The companion [`upstream-cli-api-source-map.md`](upstream-cli-api-source-map.md)
inventories every public source export and its Java owner. Pure load/download,
CLI option, and converter behavior is case-mapped separately; local format
fixtures remain explicit format-level blockers where their data is absent.
