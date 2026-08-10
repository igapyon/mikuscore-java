# Pinned upstream MusicXML I/O case map

This is the atomic mapping for all 10 included cases in
`tests/unit/musicxml-io.spec.ts` at renamed Node upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`.

| Upstream case | Java regression |
| --- | --- |
| adds tuplet start/stop notations when only time-modification exists | `MusicXmlIoTest#addsTupletStartAndStopNotationsFromTimeModification` |
| keeps existing tuplet notations untouched | `MusicXmlIoTest#keepsExistingTupletNumbersAndAddsMissingDisplayAttrs` |
| adds display attrs to existing tuplet start when missing | `MusicXmlIoTest#keepsExistingTupletNumbersAndAddsMissingDisplayAttrs` |
| fills missing tuplet groups after explicit tags in the same lane | `MusicXmlIoTest#fillsMissingTupletGroupAfterExistingExplicitTupletInSameLane` |
| adds missing part-list and part ids for minimal imports | `MusicXmlIoTest#normalizesMissingPartListAndPartId` |
| adds final right barline when missing | `MusicXmlIoTest#addsFinalRightBarlineWhenMissing` |
| generic normalization does not add implicit beams | `MusicXmlIoTest#doesNotAddImplicitBeamsDuringImportedTextNormalization` |
| explicit beam pass adds implicit beams | `MusicXmlIoTest#addsImplicitBeamsOnlyWhenRequestedExplicitly` |
| explicit beam pass preserves existing lane beams | `MusicXmlIoTest#keepsLaneBeamsUntouchedWhenImplicitBeamPassRunsOverExistingBeams` |
| preserves existing final right barline | `MusicXmlIoTest#keepsExistingFinalRightBarline` |
