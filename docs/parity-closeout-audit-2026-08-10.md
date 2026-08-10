# Parity closeout audit — 2026-08-10

Target: renamed Node upstream `miku-score` 0.6.1 at
`a8adc1998237f7b371cae75728afec7dd1795977`.

## Audit result

- Every versioned, runtime-independent source/export and test/corpus mapping
  is `done` in the class, test, source, case, and CLI maps.
- The full Node reference verification passed, including CFFP; details are in
  [upstream-verification-2026-08-10.md](upstream-verification-2026-08-10.md).
- Java `CffpSeriesTest`, the Maven package suite, diff check,
  and executable-JAR smoke checks passed; details are in
  [java-verification-2026-08-10.md](java-verification-2026-08-10.md).
- The CFFP matrix executes all 86 source cases through ABC, MEI, LilyPond,
  MuseScore, and MIDI, including all 44 declared semantic preservation
  predicates; see [upstream-cffp-case-map.md](upstream-cffp-case-map.md).

## Local-only evidence

The upstream checkout has only `tests/local-data/.gitkeep`; it does not contain
the original Moonlight, Paganini, Mozart, or local-corpus assets. Their
observable predicates are nonetheless `done`: the repository contains
original compact behavior-equivalent fixtures and named Java tests in the
[MIDI](upstream-midi-nonunit-case-map.md),
[MEI](upstream-mei-nonunit-case-map.md), and
[MuseScore](upstream-musescore-nonunit-case-map.md) non-unit maps.

## Scope exclusions

VSQX, Verovio/SVG rendering, browser UI/WebAudio, and Node/Web distribution
are explicitly excluded. The `sampleXml*.ts` constants are also excluded
because they are imported solely by browser `main.ts` sample controls.

No included implementation or evidence work remains.
