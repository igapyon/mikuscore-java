# Pinned upstream MuseScore public source map

This map audits the public exports of `src/ts/musescore-io.ts` in the
renamed upstream repository `../miku-score` at `v0.6.1`
(`a8adc1998237f7b371cae75728afec7dd1795977`). The 93 public unit cases
are individually mapped in
[`upstream-musescore-io-case-map.md`](upstream-musescore-io-case-map.md).

| Upstream public export | Java owner | Focused Java evidence | Status |
| --- | --- | --- | --- |
| `convertMuseScoreToMusicXml` | `MuseScoreIo.museScoreToMusicXml`, `CoreApi` facade | all import rows in `upstream-musescore-io-case-map.md`; `MuseScoreIoTest#routesPublicMuseScoreConversionOptionsToCutTimeAndImplicitBeamBehavior`, `#routesPublicMuseScoreSourceAndDebugMetadataOptions`, and pinned MSCZ/sample regressions | done |
| `exportMusicXmlDomToMuseScore` | `MuseScoreIo.musicXmlToMuseScore`, `CoreApi` facade | all export rows in `upstream-musescore-io-case-map.md`; `MuseScoreIoTest#routesPublicMuseScoreExportCutTimeOption`, `#routesMusicXmlDirectionsThroughPublicMuseScoreExport`, and pinned MXL/sample regressions | done |

The source's public facade and all public unit evidence are complete. The
separate non-unit map completes the local-only predicates with versioned,
behavior-equivalent Java fixtures; the unavailable original assets do not
create a reproducibility dependency.
