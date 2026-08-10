# Upstream ABC I/O public option case map

This map covers the public option slice in the pinned renamed upstream
repository `../miku-score` at `v0.6.1`
(`a8adc1998237f7b371cae75728afec7dd1795977`). It is intentionally not a
completion claim for the broader `abc-io.ts` corpus; that work remains
`ABC-02`.

| Upstream case / responsibility | Java evidence | Status |
| --- | --- | --- |
| `ABC->MusicXML writes mks:dbg:abc:meta miscellaneous fields by default` | `AbcIoTest#musicXmlFromAbcHonorsPublicMetadataAndPrettyPrintOptions` | done |
| `ABC->MusicXML can disable mks:dbg:abc:meta miscellaneous fields` | `AbcIoTest#musicXmlFromAbcHonorsPublicMetadataAndPrettyPrintOptions` | done |
| `ABC->MusicXML pretty-prints output in debug mode like MIDI import` | `AbcIoTest#musicXmlFromAbcHonorsPublicMetadataAndPrettyPrintOptions` | done |
| Public `sourceMetadata: false` suppression and the default coupling of `debugMetadata` to `debugPrettyPrint` | `AbcIoTest#musicXmlFromAbcHonorsPublicMetadataAndPrettyPrintOptions` | done |

The test uses the public `AbcIo.musicXmlFromAbc` facade, rather than export
helpers, so that parser, metadata, and serializer options are exercised
together.
