---
title: MIDI I/O source map
status: public-source-complete
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# MIDI I/O source map

This map covers every runtime-facing export in the renamed upstream
`src/ts/midi-io.ts`. Types are represented by the corresponding immutable
`MidiIo` value classes and `MidiImportOptions` / `MidiImportResult`.

| Upstream export | Java implementation and evidence | Status |
| --- | --- | --- |
| `collectMidiProgramOverridesFromMusicXmlDoc` | `MidiIo#collectMidiProgramOverridesFromMusicXmlDoc`; `MidiIoTest#collectsMidiProgramOverridesFromMusicXmlDoc` | done |
| `collectMidiControlEventsFromMusicXmlDoc` | `MidiIo#collectMidiControlEventsFromMusicXmlDoc`; `MidiIoTest#collectsMidiControlEventsFromMusicXmlDoc` | done |
| `collectMidiTempoEventsFromMusicXmlDoc` | `MidiIo#collectMidiTempoEventsFromMusicXmlDoc`; `MidiIoTest#collectsMidiTempoEventsFromMusicXmlDoc` | done |
| `collectLeadingPickupTicksFromMusicXmlDoc` | `MidiIo#collectLeadingPickupTicksFromMusicXmlDoc`; `MidiIoTest#collectsLeadingPickupTicksFromMusicXmlDoc` | done |
| `collectMidiTimeSignatureEventsFromMusicXmlDoc` | `MidiIo#collectMidiTimeSignatureEventsFromMusicXmlDoc`; FF58 collection and pickup-prelude regressions in `MidiIoTest` | done |
| `collectMidiKeySignatureEventsFromMusicXmlDoc` | `MidiIo#collectMidiKeySignatureEventsFromMusicXmlDoc`; FF59 collection regression in `MidiIoTest` | done |
| `buildMidiBytesForPlayback` | `MidiIo#buildMidiPlaybackExport`, raw writer, and `buildMidiWriterCompatibleBytes`; raw and exact Writer-byte regressions in `MidiIoTest` | done |
| `convertMidiToMusicXml` | `MidiIo#convertMidiToMusicXml`; all import-option, metadata, warning, quantization, lane, and malformed-input cases in `MidiIoTest` | done |
| `buildPlaybackEventsFromMusicXmlDoc` | `MidiIo#buildPlaybackEventsFromMusicXmlDoc`; timeline, grace, tie, slur, articulation, expression, ornament, metric, and drum regressions in `MidiIoTest` | done |
| `buildPlaybackEventsFromXml` | `MidiIo#buildPlaybackEventsFromXml`; invalid-input and basic extraction regressions in `MidiIoTest` | done |

`midi-musescore-io.ts` profile ownership is mapped separately in
[upstream-midi-musescore-io-source-map.md](upstream-midi-musescore-io-source-map.md),
and exact non-raw MidiWriterJS serialization is documented in
[upstream-midi-writer-byte-source-map.md](upstream-midi-writer-byte-source-map.md).
