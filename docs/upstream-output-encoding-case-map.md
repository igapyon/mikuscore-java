# Pinned upstream output-encoding case map

This map covers `tests/unit/output-encoding.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. The module is a pure
text/byte boundary; its Blob and filename policy is intentionally owned by
`download-flow.ts` instead.

| Upstream case | Java regression | Status |
| --- | --- | --- |
| returns plain MusicXML text without Blob or filename policy | `CoreApiTest#encodesValueBasedOutputsWithoutBrowserBoundaries` | done evidence |
| returns MXL bytes that preserve the formatted MusicXML | `CoreApiTest#encodesValueBasedOutputsWithoutBrowserBoundaries` | done evidence |
| formats VSQX and converts text formats as plain strings — VSQX formatting | — | excluded (VSQX) |
| formats VSQX and converts text formats as plain strings — ABC conversion and invalid XML | `CoreApiTest#encodesValueBasedOutputsWithoutBrowserBoundaries` | done evidence |
| builds MIDI bytes using explicit safe/raw-writer runtime options | `CoreApiTest#encodesValueBasedOutputsWithoutBrowserBoundaries`, `CoreApiTest#routesMidiDownloadRuntimeOptionsIntoTheSmfEncoding`, `MidiIoTest#serializesWriterTrackPlanLikeBundledMidiWriterJs` | done — safe default/explicit false selects the Writer-compatible backend; parity/explicit true selects raw; the four-track Writer byte fixture is exact |
| returns plain MSCX or compressed MSCZ data from the same conversion | `CoreApiTest#encodesValueBasedOutputsWithoutBrowserBoundaries` | done evidence |
| builds ZIP bytes from string and byte entries without Blob | `CoreApiTest#encodesValueBasedOutputsWithoutBrowserBoundaries` | done evidence |

`CoreApi` also exposes the source's SVG, JSON, MEI, and LilyPond pure output
facades. Both Node MIDI branches are now runtime-independent in Java:
`rawWriter: true` uses the existing raw SMF writer, while omitted/false uses
the Java serializer for the same MidiWriterJS 2.1.4 track plan. Its byte
evidence is recorded in [`upstream-midi-writer-byte-source-map.md`](upstream-midi-writer-byte-source-map.md).
VSQX stays excluded.
