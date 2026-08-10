# Pinned upstream MidiWriter byte source map

This map closes the included non-raw branch of `src/ts/midi-io.ts` at renamed
Node upstream revision `a8adc1998237f7b371cae75728afec7dd1795977`. That branch
builds `MidiWriter.Track` instances and calls the bundled
`src/js/midi-writer.js` (MidiWriterJS 2.1.4).

| Node behavior | Java implementation | Evidence |
| --- | --- | --- |
| `rawWriter: false` builds Track/Writer SMF bytes | `MidiIo#buildMidiWriterCompatibleBytes` | `MidiIoTest#serializesWriterTrackPlanLikeBundledMidiWriterJs` |
| track and instrument names (`FF 03`, `FF 04`) on meta, note, and controller tracks | Writer-plan track serializers | same exact four-track byte fixture |
| stable explicit-tick note merge plus program changes | Writer playback-track serializer | same exact four-track byte fixture (two note tracks, different starts/channels/programs) |
| channel-patched controller changes | Writer control-track serializer | same exact four-track byte fixture (CC64) |
| Writer header division clamps at `0x7fff` while event ticks retain source TPQ | Writer-file serializer | `MidiIoTest#clampsOnlyMidiWriterHeaderDivisionToBundledRuntimeLimit` |
| `options.rawWriter ?? runtime.rawWriter` | `CoreApi#encodeMidiOutput` + nullable `MidiOutputOptions#getRawWriter` | `CoreApiTest#routesMidiDownloadRuntimeOptionsIntoTheSmfEncoding` |

The exact fixture contains a tempo/meta track, two note tracks, and one
controller track. It was generated with the bundled Node `midi-writer.js` from
the same plan and with its header division set to 480, then compared byte for
byte in Java. The existing raw-writer regressions remain the evidence for the
separate `rawWriter: true` branch.
