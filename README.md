# miku-score-java

Java straight-conversion workspace for `miku-score`.

## Purpose

This repository is a Java 1.8 based straight conversion of `miku-score`.

The Java version does not try to redesign the upstream project into a Java-first architecture at the initial stage.
The primary goal is to preserve the upstream Node.js / TypeScript structure, naming, CLI intent, tests, and artifact roles closely enough that upstream changes remain traceable.

The Java version targets the CLI-oriented feature set that can run in the Java runtime.
Browser/Web UI behavior is out of scope for this repository unless a later explicit decision changes that boundary.

Current initial scope is focused on:

- MusicXML-centered score processing
- CLI runtime foundation
- upstream-aware Java tests using JUnit Jupiter
- mapping documents for upstream source, tests, CLI, and follow-up items

VSQX conversion is intentionally out of scope for the initial Java conversion because the upstream path depends on a bridge / dependency shape that is not a direct Java straight-conversion target.

## Upstream Policy

- Use `workplace/miku-score` as the local temporary reference clone of the Node.js / TypeScript upstream. An existing pre-rename `workplace/mikuscore` checkout is an ignored local historical path, not the canonical name.
- Treat `workplace/` as local working space.
- Track only `workplace/.gitkeep` in Git.
- Keep Java implementation and Java-specific specs outside `workplace/`.

The upstream source repository is:

- `https://github.com/igapyon/miku-score`

## Porting Policy

- Keep Java package names under `jp.igapyon.mikuscore`.
- Respect upstream file boundaries and responsibility splits as much as practical.
- Prefer names that are easy to map back to upstream files and `camelCase` methods.
- Do not over-optimize for modern Java style when it harms migration traceability.
- Keep upstream-derived behavior separate from Java-side original extensions.
- Keep MusicXML as the semantic anchor for score data.
- Treat VSQX as an explicit initial-scope exclusion, not as an untracked missing feature.

## Build

Primary verification command:

```sh
mvn test
```

Package command:

```sh
mvn package
```

Runtime artifact:

- `target/miku-score.jar`
- `target/miku-score-sources.jar`
- `target/miku-score-dist.zip`

Expected execution path:

```sh
java -jar target/miku-score.jar --help
```

Release asset workflow:

- pushing a `v*` tag, for example `v0.5.1`, builds the CLI runtime and attaches release assets to the matching GitHub Release
- attached files are `miku-score-<version>.jar` and `miku-score-sources-<version>.jar`
- the Release tag version must equal the Maven version or use its documented dot suffix

The current local Maven output and public Release asset names use the
`miku-score` canonical name. The compatibility boundary, including the Java
package and historical references that intentionally retain `mikuscore`, is
recorded in [`docs/rename-migration.md`](docs/rename-migration.md).

## CLI

The Java CLI is a testable adapter over `CoreApi`. Product commands are added
through straight conversion from upstream `miku-score`; supported conversion
pairs remain partial unless their mapping documents say otherwise.

Current foundation commands:

- `--help`
- `--version`
- `convert --from musicxml --to musicxml [--in <file>|-] [--out <file>|-]`
- `convert --from abc --to musicxml [--in <file>|-] [--out <file>|-]`
- `convert --from abc --to midi [--in <file>|-] [--out <file>|-]`
- `convert --from musicxml --to abc [--in <file>|-] [--out <file>|-]`
- `convert --from mei --to musicxml [--in <file>|-] [--out <file>|-]`
- `convert --from musicxml --to mei [--in <file>|-] [--out <file>|-]`
- `convert --from lilypond --to musicxml [--in <file>|-] [--out <file>|-]`
- `convert --from musicxml --to lilypond [--in <file>|-] [--out <file>|-]`
- `convert --from midi --to musicxml [--in <file>|-] [--out <file>|-]`
- `convert --from musescore --to musicxml [--in <file>|-] [--out <file>|-]`
- `convert --from musicxml --to musescore [--in <file>|-] [--out <file>|-]`
- `convert --from musicxml --to midi [--in <file>|-] [--out <file>|-]`
- `render svg [--from musicxml|abc] [--in <file>|-] [--out <file>|-]` (recognized but unsupported)
- `state summarize [--in <file>|-]`
- `state inspect-measure --measure <number> [--in <file>|-]`
- `state validate-command --command <json> [--in <file>|-]`
- `state apply-command --command <json> [--in <file>|-] [--out <file>|-]`
- `state diff --before <file> --after <file>`

The current `state validate-command` / `state apply-command` slice supports the upstream basic command catalog for MusicXML text input:

- `change_to_pitch`
- `change_duration`
- `insert_note_after`
- `delete_note`
- `split_note`
- `ui_noop`

This is still a partial core migration. Timing-sensitive parity such as underfull validation and rest consume / fill behavior is tracked in `docs/upstream-followup-log.md`.

Current MusicXML I/O support also includes a Java `MusicXmlIo` normalization subset for imported MusicXML text: parse / serialize / pretty-print, part-list / part id repair, tuplet notation enrichment, final right barline repair, and explicit implicit-beam generation.

MXL container support is available through the Java `MxlIo` slice for `META-INF/container.xml` based MusicXML extraction, fallback `.musicxml` / `.xml` extraction, and `score.musicxml` MXL encoding.

The conversion surface follows the upstream CLI taxonomy while Java format
conversion remains partial. It covers MusicXML/MXL, ABC, MIDI, MEI, LilyPond,
and MuseScore/MSCZ routes through the migrated Java facades. The exact current
coverage and remaining parity work are maintained in
[`docs/upstream-cli-mapping.md`](docs/upstream-cli-mapping.md).

### CLI runtime contract

- Omit `--in` or pass `-` to read from standard input. Omit `--out` or pass
  `-` to write the primary result to standard output.
- Text input and output use UTF-8. MusicXML accepts `.musicxml`, `.xml`, and
  `.mxl`; MuseScore accepts `.mscx` and `.mscz`; MIDI is handled as bytes.
- Primary results use standard output; usage and processing failures use
  standard error. Exit code `0` means success, `1` a processing failure, and
  `2` an invalid or unsupported invocation.
- An explicit `--out <file>` replaces an existing file. This matches the
  upstream CLI behavior. Use an explicit output path for binary results in
  automation.
- The upstream `--diagnostics text|json` option is not yet implemented by the
  Java CLI. This is a documented parity follow-up, rather than an implied
  supported option.
- `render svg` is recognized but unsupported because the upstream path depends
  on the browser-oriented Verovio runtime.

## Development Docs

Suggested order:

1. `docs/remaining-migration-items.md`
2. `docs/miku-soft-reference.md`
3. `docs/upstream-class-mapping.md`
4. `docs/upstream-test-mapping.md`
5. `docs/upstream-cli-mapping.md`
6. `docs/upstream-followup-log.md`
7. `docs/rename-migration.md`

Tracking flow:

1. Check current scope and status in `docs/remaining-migration-items.md`
2. Find the matching Java classes in `docs/upstream-class-mapping.md`
3. Find the matching tests in `docs/upstream-test-mapping.md`
4. Find CLI correspondence in `docs/upstream-cli-mapping.md`
5. Record unresolved upstream or parity items in `docs/upstream-followup-log.md`
