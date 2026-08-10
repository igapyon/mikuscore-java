---
title: LilyPond I/O pinned case map
status: complete
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# LilyPond I/O pinned case map

The renamed Node upstream at `v0.6.1` contains 68 `it` cases in its only
LilyPond-specific test file, `tests/unit/lilypond-io.spec.ts`. It has no
separate LilyPond corpus, integration, property, or additional slow test file;
the unit file is simply scheduled by the upstream slow command.

The line ranges cover every pinned `it` invocation in that contiguous source
range. Java has extra focused tests for public options, diagnostics, and
failure behavior added after the original Node suite.

| Pinned upstream cases | Java evidence | Status |
| --- | --- | --- |
| lines 13-103 (7 cases): basic/bare import, low-clef policy, explicit-staff wide-range split, no-split bare block, and warning diagnostics | `convertsBasicLilyPondSourceIntoMusicXml`; bare/low-clef tests; `autoSplitsWideRangeExplicitStaffWithoutClefUsingSharedPolicy`; `writesLilyPondImportWarningsUsingThePublicDiagnosticCode` | done |
| lines 120-402 (17 cases): relative/octave/chord anchors, ties, native directions/notations, and omitted-root relative pedal behavior | relative, tie, dynamic/wedge/slur/trill/gliss/pedal/bow/technical, and pedal-anchor regressions in `LilyPondIoTest` | done |
| lines 422-719 (15 cases): repeat/alternative, lyrics, part-name roundtrip, tuplets/chords/absolute pitch, multi-staff, variables, and lane metadata | repeat/alternative/lyric, `preservesPartNameAcrossMusicXmlToLilyPondToMusicXml`, tuplet/chord/octave/multi-part/variable/lane regressions in `LilyPondIoTest` | done |
| lines 776-998 (9 cases): complex tuplets, clef/transpose, capacity/reflow, duration multiplier, and implicit beams | 7:8/triplet visual, clef/transpose, capacity/reflow, integer-duration, and implicit-beam regressions in `LilyPondIoTest` | done |
| lines 1016-1656 (20 cases): exporter text/header/metadata and MusicXML-to-LilyPond-to-MusicXML roundtrips for measures, notation, lanes, chords, staves, and clefs | exporter and metadata roundtrip regression group in `LilyPondIoTest`, including backup-lane, PianoStaff, non-voice1, rest-only, and low-clef cases | done |

The public-source and option mapping is in
[upstream-lilypond-io-source-map.md](upstream-lilypond-io-source-map.md).
The repository-wide CFFP matrix is complete in
[upstream-cffp-case-map.md](upstream-cffp-case-map.md); it is separate from
the LilyPond-specific case inventory.
