---
title: LilyPond I/O public-source parity map
status: complete
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# LilyPond I/O public-source parity map

This map covers the two public exports in the pinned Node LilyPond I/O source.
All 68 pinned LilyPond-specific cases are mapped in
[upstream-lilypond-io-case-map.md](upstream-lilypond-io-case-map.md).

| Upstream public behavior | Java owner | Java evidence | Status |
| --- | --- | --- | --- |
| LilyPond-to-MusicXML import options | LilyPondIo.convertLilyPondToMusicXml / LilyPondImportOptions | honorsPublicLilyPondSourceMetadataAndPrettyPrintOptions | done |
| Default source metadata writes escaped raw source under mks:src:lilypond; false suppresses it | finalizeLilyPondImportedMusicXml / appendLilyPondSourceMetadata | honorsPublicLilyPondSourceMetadataAndPrettyPrintOptions | done |
| debugPrettyPrint false returns compact XML; omitted value is pretty printed | finalizeLilyPondImportedMusicXml | honorsPublicLilyPondSourceMetadataAndPrettyPrintOptions | done |
| Direct-import overfull warning uses mks:diag with LILYPOND_IMPORT_WARNING | addLilyOverfullCarryDiagnostics | writesLilyPondImportWarningsUsingThePublicDiagnosticCode | done |
| No playable source throws the public No parseable notes/rests error | noParseableLilyPondEvents | rejectsLilyPondWithoutParseableNotesUsingPublicFacadeMessage; rejectsMultipleEmptyStaffBodiesUsingTheSamePublicMessage | done |
| Clef-omitted explicit wide-range new Staff uses StaffClefPolicy to make treble/bass parts; bare block stays single staff | autoSplitLilyPondWideRangeStaff | autoSplitsWideRangeExplicitStaffWithoutClefUsingSharedPolicy; importsBareTopLevelMusicBlockWithoutScoreOrStaff | done |
| MusicXML-to-LilyPond export | LilyPondIo.exportMusicXmlDomToLilyPond | All exporter/round-trip cases in upstream-lilypond-io-case-map.md | done |

## Completion boundary

All direct LilyPond unit evidence is complete. The repository-wide
`cffp-series.spec.ts` proof is also complete in
[upstream-cffp-case-map.md](upstream-cffp-case-map.md); it is not a separate
LilyPond-specific suite. No browser or WebAudio behavior is involved.
