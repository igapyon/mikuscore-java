# Pinned upstream preview-flow case map

This map covers `tests/unit/preview-flow.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. Only
`preparePreviewSvgIdMap` is runtime independent. Both Verovio rendering
functions, DOM update callbacks, and rendered SVG handling are excluded as
Verovio/browser UI work.

| Upstream case | Java regression | Status |
| --- | --- | --- |
| uses the direct render map when rendered IDs include `mks-` IDs | `MusicXmlIoTest#preparesPreviewSvgIdMapWithDirectAndFallbackModes` | done evidence |
| builds a sequential fallback map when rendered IDs do not include `mks-` IDs | `MusicXmlIoTest#preparesPreviewSvgIdMapWithDirectAndFallbackModes` | done evidence |
| keeps the direct map when there are no rendered IDs | `MusicXmlIoTest#preparesPreviewSvgIdMapWithDirectAndFallbackModes` | done evidence |

The Java regression additionally locks the source predicate: any embedded
`mks-` rendered ID selects direct mode even when that individual ID is absent
from the direct map.
