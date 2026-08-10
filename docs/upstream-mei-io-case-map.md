---
title: MEI I/O pinned case map
status: unit-and-sample-complete
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# MEI I/O pinned case map

The renamed Node upstream at `v0.6.1` has 108 `it` cases in
`tests/unit/mei-io.spec.ts`. Its four public MEI samples are now copied
byte-for-byte into `src/test/resources/mei-samples/` and exercised through the
Java public facade. A separate map records the non-unit local-only tests.

Each line range below covers every `it` invocation in that contiguous range at
the pinned revision. One Java regression intentionally combines closely
related alias/control cases where the same public conversion path verifies all
of them.

| Pinned upstream cases | Java evidence | Status |
| --- | --- | --- |
| lines 29-365 (16 cases): basic export, MEI version, tempo/dynamics, slurs, pinned MXL sample export, and rest/space forms | `exportsMusicXmlDomToMeiScaffoldWithLayersAndControls`; `exportsMusicXmlDomToMeiVersionAndTransposeParity`; `exportsMusicXmlDomToMeiTempoAndDynamicsParity`; `exportsPinnedMxlSamplesThroughThePublicMeiFacade`; rest helpers in `MeiIoTest` | done |
| lines 387-953 (21 cases): basic import, tempo, part/time metadata, pinned MEI samples, mid-score definitions, transposition, clef, and initial attributes | `convertsMeiTextToMusicXmlDocument`; `importsBundledMeiSampleCorpusThroughThePublicFacade`; scoreDef/staffDef/transpose/clef import regressions in `MeiIoTest` | done |
| lines 992-1684 (21 cases): key inference, pickup, corpus selection, and staff/layer/note tie/slur controls | key/pickup/corpus-selection regressions; `importsStaffLevelMei*`; `importsLayerLevelMei*`; `importsNoteLevelMeiTieAndSlurAttributesTogether` | done |
| lines 1723-2664 (27 cases): grace/beam containers, dynamics/hairpins, ornament/pedal/gliss/slide/octave/repeat controls, spans, and harmony import | MEI beam/grace, dynam, control-notation, span-control, direction, and harmony regressions in `MeiIoTest` | done |
| lines 2708-3616 (22 cases): miscellaneous/harmony/direction roundtrips, raw-source namespace, overfull policy, implicit beams, metadata, articulations, ties, accidentals, tuplets, external-style timing, and rest-space import | public strict-overfull regression; source/diagnostic/metadata helpers; direction/notation/timing roundtrip regressions in `MeiIoTest` | done |
| line 3904 (1 case): staff-level cross-layer hairpin endpoint resolution by tick | `resolvesStaffLevelHairpinIdsAcrossLayersByTickOnImport` | done |
| `src/samples/mei/sample1.mei` through `sample4.mei` | `importsBundledMeiSampleCorpusThroughThePublicFacade` | done — all four parsed, have playable output, and pass Java save validation; sample1 accidental/tempo/articulation and sample4 6/8 assertions match the pinned unit cases |

The public-source option ownership is in
[upstream-mei-io-source-map.md](upstream-mei-io-source-map.md). The upstream
local Paganini test skips when its untracked source is absent; its observable
checkpoints are complete through the versioned compact equivalent in
[upstream-mei-nonunit-case-map.md](upstream-mei-nonunit-case-map.md).
