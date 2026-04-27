# Upstream Test Mapping

This document maps upstream `mikuscore` test intent to Java tests.

## Mapping Status

| Upstream test | Java test | Status | Notes |
| --- | --- | --- | --- |
| `tests/unit/core.spec.ts` | `MusicXmlStateTest` | partial | Basic command catalog validation/apply subset, `ui_noop` no-dirty behavior, overfull validation subset, structural boundary subset, and chord-head delete promotion |
| `tests/unit/musicxml-io.spec.ts` | `MusicXmlIoTest` | partial | Parse failure shape, part-list / part id normalization, tuplet notation enrichment, explicit implicit beam pass, final right barline normalization, and invalid-input passthrough |
| `tests/unit/mikuscore-cli.spec.ts` MXL / ZIP fixture intent | `MxlIoTest` | partial | MXL make/extract roundtrip, container root-file extraction, fallback MusicXML extraction, extension-based text extraction, and root-entry listing |
| `tests/unit/abc-io.spec.ts` | pending | not started | ABC I/O |
| `tests/unit/abc-parser.spec.ts` | pending | not started | ABC parser |
| `tests/unit/abc-roundtrip-golden.spec.ts` | pending | not started | ABC golden roundtrip |
| `tests/unit/cli-api.spec.ts` | `MusicXmlStateTest` | partial | State summary/inspection/diff plus basic command validation/apply JSON/output shape |
| `tests/unit/mikuscore-cli.spec.ts` | `MikuscoreCliTest` | partial | `--help`, unsupported-command behavior, state commands, and basic command catalog through CLI |
| `tests/unit/musescore-io.spec.ts` | pending | not started | MuseScore I/O |
| `tests/unit/midi-io.spec.ts` | pending | not started | MIDI I/O |
| `tests/unit/mei-io.spec.ts` | pending | not started | MEI I/O |
| `tests/unit/lilypond-io.spec.ts` | pending | not started | LilyPond I/O |
| `tests/property/core.property.spec.ts` | pending | not started | Property tests require Java-side strategy |

## Fixture Policy

- Use upstream fixtures as comparison material where practical
- Decide per fixture whether to copy into Java test resources or refer to a generated/local comparison path
- Keep local / spot tests that depend on external tools separate from primary `mvn test`
