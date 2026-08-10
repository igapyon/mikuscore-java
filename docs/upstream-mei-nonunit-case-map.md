---
title: MEI non-unit evidence map
status: done
upstream_revision: a8adc1998237f7b371cae75728afec7dd1795977
---

# MEI non-unit evidence map

| Upstream case | Scope decision / Java work | Status |
| --- | --- | --- |
| `tests/spot/local-mei-official-visual.spot.spec.ts` | The test renders MEI with Verovio, then compares raster output with ImageMagick and rsvg-convert. It is excluded by the agreed Verovio/SVG-rendering boundary; its structural prechecks are already covered by the MEI unit map. | excluded |
| `tests/spot/local-mei-roundtrip-parity.spot.spec.ts` | `MeiIoTest#roundTripsCompactPaganiniEquivalentMeasureCheckpointsThroughMei` uses `upstream-local-equivalent/compact-control-parity.musicxml` and preserves the exact upstream observable checkpoints: measure 138 `D7`/120, measure 140 `C7`/120, and measure 153 `C♯4`/69 through `MeiIo.exportMusicXmlDomToMei` and `MeiIo.convertMeiToMusicXml`. | done |

The original local Paganini asset is not present in the pinned revision, so it
cannot be copied into this repository. The compact, original Java fixture is a
behavior-equivalent replacement and makes the executable semantic evidence
fully reproducible. The visual test remains excluded solely because it is a
Verovio/SVG rendering comparison.
