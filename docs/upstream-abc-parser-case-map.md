# Pinned upstream ABC parser case map

This map covers all 15 cases in `tests/unit/abc-parser.spec.ts` at renamed
Node upstream revision `a8adc1998237f7b371cae75728afec7dd1795977`.
`abc-parser.ts` is runtime independent and is included in the Java parity
scope.

| Upstream case | Java regression | Status |
| --- | --- | --- |
| parses inline body fields with normalized field names | `AbcParserTest#parsesFieldAndBarlineHelpers` | done evidence |
| parses repeat ending markers in bracketed and bare forms | `AbcParserTest#parsesFieldAndBarlineHelpers` | done evidence |
| parses barline tokens with repeat metadata | `AbcParserTest#parsesFieldAndBarlineHelpers` | done evidence |
| returns null when field and barline helpers do not match | `AbcParserTest#parsesFieldAndBarlineHelpers` | done evidence |
| parses standalone body fields and unsupported fallback tokens | `AbcParserTest#parsesStandaloneFieldsAndUnsupportedFallbackTokens` | done evidence |
| parses delimited quoted and decoration spans | `AbcParserTest#parsesSpanDecorationAndBodyTokenAtoms` | done evidence |
| parses quoted strings and decorations with termination state | `AbcParserTest#parsesSpanDecorationAndBodyTokenAtoms` | done evidence |
| parses broken-rhythm shorthand | `AbcParserTest#parsesSpanDecorationAndBodyTokenAtoms` | done evidence |
| parses single-character body shorthand tokens | `AbcParserTest#parsesSpanDecorationAndBodyTokenAtoms` | done evidence |
| parses tie and slur-stop tokens | `AbcParserTest#parsesSpanDecorationAndBodyTokenAtoms` | done evidence |
| parses parenthesis tokens as tuplet or slur-start | `AbcParserTest#parsesStructuralDispatcherTokens` | done evidence |
| parses bracket tokens as inline-field, repeat-ending, or chord-start | `AbcParserTest#parsesStructuralDispatcherTokens` | done evidence |
| dispatches common body tokens through a single parser entrypoint | `AbcParserTest#dispatchesBodyTokens` | done evidence |
| parses playable note and chord events through a shared parser entrypoint | `AbcParserTest#parsesPlayableEvents`, `AbcParserTest#retainsPinnedPlayableFallbackAndUnterminatedDecorationDispatch` | done evidence |
| dispatches body entries through a higher-level parser entrypoint | `AbcParserTest#dispatchesBodyEntries`, `AbcParserTest#retainsPinnedPlayableFallbackAndUnterminatedDecorationDispatch` | done evidence |

The Java port also directly tests `parseAbcGraceGroupAt`, which is exported by
the pinned source but not exercised by this upstream unit file, through
`AbcParserTest#parsesGraceGroups` and
`AbcParserTest#reportsMalformedGraceAccidentals`.
