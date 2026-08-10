# Pinned upstream ABC lexer source map

This source-level map covers `src/ts/abc-lexer.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`. The pinned upstream has no
separate lexer unit file: its three exports are exercised by
`abc-parser.spec.ts` through parser entrypoints and have direct Java lexical
regressions below.

| Upstream export | Java implementation | Java regression | Status |
| --- | --- | --- | --- |
| `lexAbcLengthToken` | `AbcLexer#lexAbcLengthToken` | `AbcLexerTest#lexesLengthTokens`, `AbcLexerTest#retainsSourceLexicalBoundaryBehaviorAtOffsets` | done evidence |
| `lexAbcAccidental` | `AbcLexer#lexAbcAccidental` | `AbcLexerTest#lexesAccidentals`, `AbcLexerTest#retainsSourceLexicalBoundaryBehaviorAtOffsets` | done evidence |
| `lexAbcNote` | `AbcLexer#lexAbcNote` | `AbcLexerTest#lexesNotes`, `AbcLexerTest#clampsNegativeStartIndex`, `AbcLexerTest#retainsSourceLexicalBoundaryBehaviorAtOffsets` | done evidence |

The direct cases cover slash and repeated-slash length boundaries, malformed
and mixed accidental runs, rests/skips, non-note rejection, negative-index
clamping, and nonzero source offsets. Java's public signature uses a typed
`int` start index, matching the TypeScript declaration's numeric API.
