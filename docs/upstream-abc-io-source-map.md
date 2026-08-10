---
title: ABC I/O public-source parity map
status: active
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# ABC I/O public-source parity map

This map audits the public exports in the pinned Node ABC I/O module. It
complements the lexer/parser, public-option, layout, and complete pinned case
maps.

| Upstream public export | Java owner | Java evidence | Status |
| --- | --- | --- | --- |
| AbcCommon: fraction, length, pitch, accidental, and key helpers | AbcIo public helper methods | reducesAndCombinesFractions; parsesFractionTextWithFallback; parsesAbcLengthTokens; formatsAbcLengthTokens; formatsPitchAccidentalKeyAndTempo; mapsAbcKeysToFifths | done |
| AbcCompatParser.parseForMusicXml | AbcIo.parseForMusicXml plus AbcParser and AbcLexer | parsesBasicAbcBodyAndBuildsMusicXml; parsesAbcTupletBodyIntoMusicXmlTiming; parsesAbcGraceGroupIntoMusicXmlGraceNotes; parser and lexer source maps | done |
| exportMusicXmlDomToAbc | AbcIo.musicXmlToAbc | convertsMusicXmlToAbcHarmonyDirectionAndLyrics; musicXmlToAbcExportsLyricsAsWLines; musicXmlToAbcExportsMetronomeBeatUnitIntoQHeader | done |
| MusicXML-to-ABC public header selection | AbcIo.musicXmlToAbc | musicXmlToAbcUsesPublicHeaderFallbacksAndOmitsMissingComposer | done |
| clefXmlFromAbcClef | AbcIo.clefXmlFromAbcClef | buildsAbcMusicXmlExportHelperXml | done |
| convertAbcToMusicXml and its import options | AbcIo.musicXmlFromAbc and AbcImportOptions | musicXmlFromAbcHonorsPublicMetadataAndPrettyPrintOptions; parsesBasicAbcBodyAndBuildsMusicXml | done |
| Public export/import grand-staff roundtrip and positive MusicXML voice values | AbcIo.musicXmlToAbc / musicXmlFromAbc | abcRoundtripKeepsGrandStaffLanesValidAndVoiceNumbersPositive | done |

## Completion evidence

All 408 pinned `abc-io.spec.ts` cases, the inline-voice case, and the four
pinned ABC golden fixtures are mapped as done in
[upstream-abc-io-case-map.md](upstream-abc-io-case-map.md). Java's semantic
roundtrip loop runs the four upstream goldens plus additional fixtures for
backup, inherited timing, tuplets, multi-staff, unknown markup, underfull,
and overfull source shapes. This closes `ABC-02` for the included scope.
