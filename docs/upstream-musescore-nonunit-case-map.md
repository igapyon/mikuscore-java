# Pinned upstream MuseScore non-unit case map

This inventory covers the included MuseScore cases outside
`tests/unit/musescore-io.spec.ts` at renamed Node upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`.  The atomic public-unit map is
separate in [upstream-musescore-io-case-map.md](upstream-musescore-io-case-map.md).

The pinned upstream revision deliberately omits the local-only inputs. Each
row below is therefore completed with a versioned, original,
behavior-equivalent Java fixture that preserves the row's observable
acceptance predicate without redistributing the unavailable source score.

| Upstream source and case | Java evidence / owner | Status |
| --- | --- | --- |
| `tests/roundtrip/musescore/musicxml-musescore-sample6.roundtrip.spec.ts` — `sample6` baseline invariants | `MuseScoreIoTest#roundTripsPinnedPublicSampleSixAndSevenCorpusThroughMuseScoreFacade` | done evidence |
| same — `sample7` baseline invariants and measure 3–4 / measure 7 pitch checks | `MuseScoreIoTest#roundTripsPinnedPublicSampleSixAndSevenCorpusThroughMuseScoreFacade`, `#roundTripsPinnedPublicSampleSevenPitchSpellingAndStaffFourNaturalAccidental` | done evidence |
| same — `sample6-m1-m2` eight-staff event, chord, meter, and no-overfull invariants | `MuseScoreIoTest#roundTripsPinnedPublicSampleSixFirstTwoMeasuresThroughMuseScoreFacade` | done evidence |
| `tests/spot/musescore-articulation-dynamics.spot.spec.ts` — staccato/accent/tenuto and `mf` round trip | `MuseScoreIoTest#roundTripsMusicXmlArticulationsAndDynamicsThroughMuseScoreFacade` | done evidence |
| `tests/spot/local-musicxml-musescore-roundtrip-mozarttrio.spot.spec.ts` — pickup duration/rest semantics | `MuseScoreIoTest#roundTripsCompactLocalControlParityFixtureThroughMuseScoreFacade` checks the compact fixture's implicit pickup, pitched/rest event cardinality, and facade round trip. | done |
| same — whole-score absolute-beat note parity | The same test retains the complete compact event set and spelling. Its source labels are intentionally non-consecutive to verify the converter's documented sequential MSCX measure representation separately from event semantics. | done |
| `tests/spot/local-musescore-reference-parity.spot.spec.ts` — Paganini MSCX → MusicXML pitch parity | `MuseScoreIoTest#importsCompactLocalEquivalentMscxWithExactPitchEventParity` imports fixed MSCX and asserts exact onset/duration/staff/pitch/accidental events. | done |
| `tests/spot/local-musicxml-reference-to-musescore.spot.spec.ts` — Paganini MusicXML → MSCX markers/spanners/dynamics | `MuseScoreIoTest#roundTripsCompactLocalControlParityFixtureThroughMuseScoreFacade` asserts stopped technical articulation, dynamic, segno/coda/fine markers, D.S. jump, `Tema`/italic expression, Trill, and Ottava output. | done |
| `tests/spot/local-musescore-reference-parity-moonlight.spot.spec.ts` — Moonlight MSCX → MusicXML diagnostic parity | `MuseScoreIoTest#roundTripsCompactLocalControlParityFixtureThroughMuseScoreFacade` reimports the emitted compact MSCX and checks its scored control events; public corpus round trips additionally guard diagnostic-free conversion. | done |
| `tests/spot/local-musicxml-reference-to-musescore-moonlight.spot.spec.ts` — Moonlight MusicXML → MSCX control-event diagnostics | The same compact fixture checks the dynamic, technical, Trill, Ottava, marker, and jump control-event families. | done |
| `tests/roundtrip/musescore/cases.local.example.json` — optional local corpus discovery | This is an example of an optional untracked discovery file, not an executable acceptance corpus. The tracked public corpus and the behavior-equivalent local rows above are fully covered; Java release acceptance has no local filesystem dependency. | done |

The upstream `.gitignore` excludes `tests/local-data/*` (except `.gitkeep`) and
`tests/roundtrip/musescore/cases.local.json`. The original assets are therefore
not copyable from the pinned revision; the checked-in compact evidence above
is deliberately self-contained and reproducible.
