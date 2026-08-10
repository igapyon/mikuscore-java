# Pinned upstream new-score case map

This map covers `tests/unit/new-score.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. `new-score.ts` is a pure
MusicXML template generator, so it is included independently of browser UI.

| Upstream case | Java regression | Status |
| --- | --- | --- |
| creates eight-measure multi-part score shape | `NewScoreTest#createsPinnedEightMeasureMultiPartScoreShape` | done evidence |
| creates single-part piano grand-staff template | `NewScoreTest#createsPinnedPianoGrandStaffTemplate` | done evidence |
| normalizes public options without Web form controls | `NewScoreTest#normalizesPinnedPublicNewScoreOptions` | done evidence |

`NewScore.Options` accepts the public numerical values as `Object` so the
same Boolean, radix-string, and numeric normalization boundary can be tested
without a browser form layer.
