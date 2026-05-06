# mikuscore-java

Java straight-conversion workspace for `mikuscore`.

## Purpose

This repository is a Java 1.8 based straight conversion of `mikuscore`.

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

- Use `workplace/mikuscore` as the local temporary reference clone of the Node.js / TypeScript upstream.
- Treat `workplace/` as local working space.
- Track only `workplace/.gitkeep` in Git.
- Keep Java implementation and Java-specific specs outside `workplace/`.

The upstream source repository is:

- `https://github.com/igapyon/mikuscore`

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

- `target/mikuscore.jar`
- `target/mikuscore-dist.zip`

Expected execution path:

```sh
java -jar target/mikuscore.jar --help
```

## CLI

The current Java CLI is only a foundation entrypoint.
Product commands are added through straight conversion from upstream `mikuscore`.

Current foundation commands:

- `--help`
- `--version`
- `convert --from musicxml --to musicxml [--in <file>|-] [--out <file>|-]`
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

The first `convert` slice is intentionally narrow and follows the latest upstream CLI taxonomy while Java format conversion is still partial:

- stdin / stdout MusicXML text pass-through
- `.musicxml` / `.xml` file input and output
- `.mxl` file input decoded to MusicXML text
- `.mxl` file output encoded from MusicXML text
- unsupported conversion pairs return usage error status `2`

Planned upstream command families:

- `convert --from ... --to ...`
- `render svg` (pending; upstream currently depends on `verovio.js` browser runtime)
- `state summarize`
- `state inspect-measure`
- `state validate-command`
- `state apply-command`
- `state diff`

## Development Docs

Suggested order:

1. `docs/remaining-migration-items.md`
2. `docs/miku-soft-30-straight-conversion-v20260425.md`
3. `docs/upstream-class-mapping.md`
4. `docs/upstream-test-mapping.md`
5. `docs/upstream-cli-mapping.md`
6. `docs/upstream-followup-log.md`

Tracking flow:

1. Check current scope and status in `docs/remaining-migration-items.md`
2. Find the matching Java classes in `docs/upstream-class-mapping.md`
3. Find the matching tests in `docs/upstream-test-mapping.md`
4. Find CLI correspondence in `docs/upstream-cli-mapping.md`
5. Record unresolved upstream or parity items in `docs/upstream-followup-log.md`
