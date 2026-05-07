# Upstream Test Mapping

This document maps upstream `mikuscore` test intent to Java tests.

## Mapping Status

| Upstream test | Java test | Status | Notes |
| --- | --- | --- | --- |
| `tests/unit/core.spec.ts` | `MusicXmlStateTest` | partial | Basic command catalog validation/apply subset, `ui_noop` no-dirty behavior, overfull validation subset, structural boundary subset, and chord-head delete promotion |
| `tests/unit/musicxml-io.spec.ts` | `MusicXmlIoTest` | partial | Parse failure shape, part-list / part id normalization, tuplet notation enrichment, explicit implicit beam pass, final right barline normalization, and invalid-input passthrough |
| `tests/unit/mikuscore-cli.spec.ts` MXL / ZIP fixture intent | `MxlIoTest`, `MikuscoreCliTest` | partial | MXL make/extract roundtrip, container root-file extraction, fallback MusicXML extraction, extension-based text extraction, root-entry listing, and first CLI `.mxl` input/output coverage |
| `tests/unit/abc-io.spec.ts` utility intent | `AbcIoTest` | partial | Fraction helpers, ABC length token parse/format, pitch, accidental, key, tempo unit, abcjs wrapper, measure duration estimate, ABC key fifths, `%@mks` meta directive helpers, import line processor helpers, body text entry helpers, voice directive tail helpers, header parsing helpers, voice measure meta helpers, MusicXML export XML helpers, part measure render context helpers, rendered measure misc XML helpers, rendered part measure XML helper composition, part list/body XML assembly, score-partwise document wrapper, export context, parsed-to-document assembly, measure note XML core subset, note lyric/time-modification XML subset, note leading direction XML subset, measure beam XML subset, note notations decoration XML subset, body import voice stores helper subset, body lyric application subset, body field state update subset, body barline processing subset, non-playable body entry dispatch subset, simple body token dispatch subset, bracket / grace / fallback body dispatch subset, pending note state helper subset, ABC chord harmony XML helper subset, MusicXML to ABC harmony / lyric helper subset, MusicXML to ABC DOM utility helper subset, MusicXML to ABC lane definition helper subset, MusicXML to ABC meta line helper subset, MusicXML to ABC measure meta helper subset, MusicXML to ABC measure state helper subset, MusicXML to ABC direction token helper subset, MusicXML to ABC note lane / timing helper subset, and MusicXML to ABC note ornament helper subset covered |
| `tests/unit/abc-io.spec.ts` conversion intent | pending | not started | ABC body import and MusicXML export integration |
| `tests/unit/abc-parser.spec.ts` ABC lexer-backed note intent | `AbcLexerTest` | partial | Low-level length token, accidental, and note lexing behavior covered before higher-level parser migration |
| `tests/unit/abc-parser.spec.ts` playable-event intent | `AbcParserTest` | partial | `parseAbcNoteAt`, `parseAbcChordAt`, and `parseAbcPlayableEventAt` behavior covered |
| `tests/unit/abc-parser.spec.ts` structural/body token intent | `AbcParserTest` | partial | Field, repeat, barline, span, decoration, broken-rhythm, shorthand, tie/slur, paren/bracket, body-token, and body-entry helpers covered |
| `tests/unit/abc-parser.spec.ts` grace group intent | `AbcParserTest` | partial | Grace note groups and malformed grace accidental warning behavior covered |
| `tests/unit/abc-roundtrip-golden.spec.ts` | pending | not started | ABC golden roundtrip |
| `tests/unit/cli-api.spec.ts` | `MusicXmlStateTest` | partial | State summary/inspection/diff plus basic command validation/apply JSON/output shape |
| `tests/unit/mikuscore-cli.spec.ts` | `MikuscoreCliTest` | partial | `--help`, `convert --from musicxml --to musicxml`, unsupported-command / unsupported-pair behavior, state commands, and basic command catalog through CLI |
| `tests/unit/musescore-io.spec.ts` | pending | not started | MuseScore I/O |
| `tests/unit/midi-io.spec.ts` | pending | not started | MIDI I/O |
| `tests/unit/mei-io.spec.ts` | pending | not started | MEI I/O |
| `tests/unit/lilypond-io.spec.ts` | pending | not started | LilyPond I/O |
| `tests/property/core.property.spec.ts` | pending | not started | Property tests require Java-side strategy |

## Fixture Policy

- Use upstream fixtures as comparison material where practical
- Decide per fixture whether to copy into Java test resources or refer to a generated/local comparison path
- Keep local / spot tests that depend on external tools separate from primary `mvn test`
