# Pinned upstream midi-musescore-io source map

This map covers the three runtime-independent exports in
`src/ts/midi-musescore-io.ts` at renamed Node upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`. The source file has no separate
upstream unit spec; direct Java regressions provide the evidence.

| Upstream export | Java implementation and regression | Status |
| --- | --- | --- |
| `normalizeMidiExportProfile` | `MidiIo#normalizeMidiExportProfile`; `MidiIoTest#resolvesPinnedMidiExportProfilesAndPlaybackBuildModes` | done evidence |
| `resolveMidiExportRuntimeOptions` | `MidiIo#resolveMidiExportRuntimeOptions`; `MidiIoTest#resolvesPinnedMidiExportProfilesAndPlaybackBuildModes` | done evidence |
| `resolvePlaybackBuildModeForMidiExport` | `MidiIo#resolvePlaybackBuildModeForMidiExport`; `MidiIoTest#resolvesPinnedMidiExportProfilesAndPlaybackBuildModes` | done evidence |

The resolver is used by `CoreApi.encodeMidiOutput`, keeping safe/parity TPQ,
event-build policy, grace/ornament/tie inclusion, raw-writer preference, and
retrigger policy together. The non-raw Writer byte path is now implemented by
the Java MidiWriterJS-compatible serializer; see
[`upstream-midi-writer-byte-source-map.md`](upstream-midi-writer-byte-source-map.md).
