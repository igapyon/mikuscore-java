---
title: MEI I/O public-source parity map
status: unit-and-sample-complete
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# MEI I/O public-source parity map

This map covers the two public exports in the pinned Node MEI converter. The
full pinned unit and public-sample inventory is recorded in
[upstream-mei-io-case-map.md](upstream-mei-io-case-map.md).

| Upstream public behavior | Java owner | Java evidence | Status |
| --- | --- | --- | --- |
| MEI-to-MusicXML import facade | MeiIo.convertMeiToMusicXml | convertsMeiTextToMusicXmlDocument; importsBundledMeiSampleCorpusThroughThePublicFacade; publicMeiImportClampsOverfullEventsOrFailsWhenStrict | done |
| debugMetadata, sourceMetadata, failOnOverfullDrop, and meiCorpusIndex option resolution | resolveMeiImportOptions and import builders | selectsMeiImportRootAndBuildsPartList; convertsMeiTextToMusicXmlDocument; publicMeiImportClampsOverfullEventsOrFailsWhenStrict | done |
| Raw MEI source fields are always preserved on initial attributes; sourceMetadata controls source miscellaneous annot import | buildMeiSourceRawMiscFields and buildMeiImportedMeasureXmlFromProcessedStaff | buildsMeiRawMiscFieldsAndParsesMeasureMeta; convertsMeiTextToMusicXmlDocument | done |
| Overfull default clamp diagnostic and strict drop failure | buildMeiImportedMeasureXmlFromProcessedStaff | publicMeiImportClampsOverfullEventsOrFailsWhenStrict | done |
| MusicXML-to-MEI export facade and meiVersion normalization | MeiIo.exportMusicXmlDomToMei / normalizeMeiVersion | exportsMusicXmlDomToMeiScaffoldWithLayersAndControls; exportsMusicXmlDomToMeiVersionAndTransposeParity; exportsPinnedMxlSamplesThroughThePublicMeiFacade | done |

## Non-unit evidence

All 108 pinned unit cases and the four public samples are mapped done in
[upstream-mei-io-case-map.md](upstream-mei-io-case-map.md). The local-only
Paganini roundtrip checkpoints are covered by a versioned compact equivalent
in [upstream-mei-nonunit-case-map.md](upstream-mei-nonunit-case-map.md). The
official visual comparison itself is Verovio rendering and remains excluded.
