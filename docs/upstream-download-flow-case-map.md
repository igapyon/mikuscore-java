# Pinned upstream download-flow case and source map

This map covers `src/ts/download-flow.ts` and its unit suite
`tests/unit/download-flow.spec.ts` at renamed Node upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`.

Java represents a browser `Blob` as the immutable `CoreApi.DownloadPayload`
text/byte facade. Object URL creation and click-triggered download are browser
UI and therefore excluded. VSQX payload generation is excluded with the VSQX
format itself. The direct SVG payload is retained because its filename/MIME
contract is independent of the excluded Verovio renderer.

## Unit cases

| Upstream case | Java regression / owner | Status |
| --- | --- | --- |
| uses `.musicxml` for plain MusicXML export by default | `CoreApiTest#createsMusicXmlDownloadPayloads` | done evidence |
| uses `.xml` when the MusicXML extension option is enabled | `CoreApiTest#createsMusicXmlDownloadPayloads` | done evidence |
| creates an `.mxl` payload when MusicXML compression is enabled | `CoreApiTest#createsMusicXmlDownloadPayloads` | done evidence |
| creates an `.mscz` payload when MuseScore compression is enabled | `CoreApiTest#createsMuseScoreDownloadPayloads` | done evidence |
| formats plain `.mscx` output with two-space indentation | `CoreApiTest#createsMuseScoreDownloadPayloads` | done evidence |
| formats `.vsqx` output with two-space indentation | VSQX import/export | excluded |
| sets stable MIME types for direct SVG and JSON downloads | `CoreApiTest#createsDirectTextDownloadPayloadsWithMimeTypes` | done evidence |
| lists only ZIP root entries by extension | `MxlIoTest#listsRootEntryPathsByExtensionsOnlyAtRoot` | done evidence |
| extracts ZIP entry bytes by exact path | `MxlIoTest#extractsExactZipEntryBytesAndRejectsMissingPath` | done evidence |
| rejects ZIP archives without EOCD | `MxlIoTest#distinguishesInvalidZipWithoutCentralDirectoryFromEmptyZip` | done evidence |
| rejects empty ZIP archives for extension extraction | `MxlIoTest#distinguishesInvalidZipWithoutCentralDirectoryFromEmptyZip` | done evidence |
| ignores directory entries when listing ZIP root entries | `MxlIoTest#listsRootEntryPathsByExtensionsOnlyAtRoot` | done evidence |
| falls back to likely MusicXML when MXL has no container rootfile | `MxlIoTest#extractsFallbackMusicXmlWhenContainerIsMissing`, `#fallsBackToXmlExtensionWhenMxlHasNoContainerRootFile` | done evidence |
| rejects exact ZIP extraction for a missing path | `MxlIoTest#extractsExactZipEntryBytesAndRejectsMissingPath` | done evidence |
| rejects extension extraction with no matching entry | `MxlIoTest#rejectsZipExtensionExtractionWhenNoEntryMatches` | done evidence |
| rejects MXL when container rootfile is missing | `MxlIoTest#rejectsMxlWhenContainerRootFileIsMissing` | done evidence |
| returns `null` when text-format conversion throws | `CoreApiTest#returnsNullForInvalidDownloadConversions` | done evidence |
| returns `null` when MuseScore receives invalid MusicXML | `CoreApiTest#returnsNullForInvalidDownloadConversions` | done evidence |

## Source exports not exercised by that suite

| Upstream export | Java owner and evidence | Status |
| --- | --- | --- |
| `triggerFileDownload` | Browser DOM/object URL/click action | excluded |
| `createZipBundleDownloadPayload` | `CoreApi.createZipBundleDownloadPayload`; `CoreApiTest#preservesPinnedJsonStemAndBuildsZipBundlePayloads`; list-based `MxlIo.makeZipBytes` preserves ordered duplicate paths | done evidence |
| `createJsonDownloadPayload` optional-stem runtime semantics | `CoreApi.createJsonDownloadPayload`; `CoreApiTest#preservesPinnedJsonStemAndBuildsZipBundlePayloads` covers empty stem and the no-argument overload models the Node default | done evidence |
| `createMidiDownloadPayload` runtime options | `CoreApi.createMidiDownloadPayload(xml, MidiOutputOptions)` routes TPQ, preset/override, grace/accent, profile, and roundtrip-metadata values through the shared value encoder; `CoreApiTest#routesMidiDownloadRuntimeOptionsIntoTheSmfEncoding` locks safe TPQ and parity-forced TPQ in SMF headers | done evidence |
| `createMeiDownloadPayload` `meiVersion` option | `CoreApi.createMeiDownloadPayload`; `CoreApiTest#createsFormatDownloadPayloadsFromMusicXml` covers valid and normalized version values | done evidence |

The unit suite is complete within the agreed scope (17 done, 1 VSQX
exclusion). `FLOW-02` is complete: Java selects raw or non-raw output with
the same option precedence as Node, and the non-raw MidiWriterJS-compatible
serializer has direct byte evidence in
[`upstream-midi-writer-byte-source-map.md`](upstream-midi-writer-byte-source-map.md).
