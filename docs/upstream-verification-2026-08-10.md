# Pinned upstream verification — 2026-08-10

Reference checkout: `/private/tmp/miku-score-upstream` at
`a8adc1998237f7b371cae75728afec7dd1795977` (`miku-score` 0.6.1).

Environment: Node.js 26.5.0, Vitest 3.2.4. Dependencies were installed with
`npm ci` in that disposable upstream checkout.

| Command | Result |
| --- | --- |
| `npm run typecheck` | passed (`tsc --noEmit`) |
| `npm run test:unit` | 37 files passed; 1,064 passed, 12 skipped (1,076 total) |
| `npm run test:property` | 1 file passed; 2 passed |
| `npm run test:integration` | 3 files passed; 287 passed, 12 skipped (299 total) |
| `npm run test:slow` | 3 files passed; 84 passed |
| `./node_modules/.bin/vitest run tests/unit/cffp-series.spec.ts` | 1 file passed; 86 passed |

The unit script includes VSQX and Verovio/browser-render assertions. They pass
in the Node reference run but remain explicit exclusions from the Java target;
their upstream success does not expand the Java scope.

The 12 skipped tests are the MEI unit suite's expected skips. Separately,
spot/local suites guard unavailable `tests/local-data` assets with `skip` or a
no-op pass; the checkout contains only `tests/local-data/.gitkeep`. Their
observable predicates are covered by versioned compact Java equivalents in
the MIDI, MEI, and MuseScore non-unit maps.

The shell emitted a non-fatal `pyenv: cannot rehash ... isn't writable`
message before some npm commands; every test command exited 0.
