# Pinned upstream MusicXML I/O source map

This is the public-export audit for `src/ts/musicxml-io.ts` in the renamed
upstream repository `../miku-score` at `v0.6.1`
(`a8adc1998237f7b371cae75728afec7dd1795977`). The 10 pinned unit cases
are separately named in
[`upstream-musicxml-io-case-map.md`](upstream-musicxml-io-case-map.md).

| Upstream public export | Java owner | Focused Java evidence | Status |
| --- | --- | --- | --- |
| `parseMusicXmlDocument`, `serializeMusicXmlDocument` | `MusicXmlIo` parse/serialize facades | `MusicXmlIoTest#parsesValidMusicXmlDocument`, `#returnsNullForInvalidMusicXmlDocument` | done |
| `prettyPrintMusicXmlText` | `MusicXmlIo.prettyPrintMusicXmlText` | `MusicXmlIoTest#prettyPrintUsesJavaScriptWhitespaceForTagCompactionAndTrimming` | done |
| `normalizeImportedMusicXmlText` | `MusicXmlIo.normalizeImportedMusicXmlText` | `MusicXmlIoTest#normalizesMissingPartListAndPartId`, `#normalizesScorePartwiseElementsNestedUnderAnotherXmlRoot`, and the pinned unit-case map | done |
| `applyImplicitBeamsToMusicXmlText` | `MusicXmlIo.applyImplicitBeamsToMusicXmlText` and shared `computeBeamAssignments` | `MusicXmlIoTest#addsImplicitBeamsOnlyWhenRequestedExplicitly`, `#usesJavaScriptDecimalPrefixParsingForImplicitBeamTimelineValues`, `#keepsEmptyVoiceLaneDistinctFromAnAbsentVoiceWhenInferringBeams` | done |
| `buildRenderDocWithNodeIds` | `MusicXmlIo.buildRenderDocWithNodeIds` | `MusicXmlIoTest#buildsRenderDocWithSvgNodeIdsWithoutMutatingSource`, `#preservesRenderNodeMapInsertionOrderAndJavaScriptNullStringification` | done |
| `extractMeasureEditorDocument` | `MusicXmlIo.extractMeasureEditorDocument` | `MusicXmlIoTest#extractsMeasureEditorDocumentWithEffectiveAttributesAndBlankPartNames`, `#usesRawPartMeasureAndVersionAttributesForMeasureEditorLookup` | done |
| `replaceMeasureInMainDocument` | `MusicXmlIo.replaceMeasureInMainDocument` | `MusicXmlIoTest#replacesMeasureInMainDocumentAndDropsPreviewOnlyAttributes`, `#replacesFromTheFirstPartMeasureInAWrappedEditorDocument` | done |

All public source exports and all pinned unit cases now have Java owners and
focused evidence. This is a source-audit completion for `XML-01`; the
cross-format corpus audit remains part of final project closeout.
