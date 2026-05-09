# Upstream CLI Mapping

This document maps the upstream `mikuscore` CLI contract to the Java CLI.

## Current Java CLI

| Java command | Status | Notes |
| --- | --- | --- |
| `--help` | implemented | Help now follows the current upstream `convert` / `state` command split for implemented Java slices |
| `--version` | implemented | Returns Java package version fallback |
| `convert --from musicxml --to musicxml [--in <file>|-] [--out <file>|-]` | implemented | First Java convert slice; supports text MusicXML and `.mxl` decode / encode by file extension |
| `convert --from abc --to musicxml [--in <file>|-] [--out <file>|-]` | partial | First ABC import/export bridge; supports basic ABC text headers, notes, rests, chords, tuplets, grace groups, basic decorations, repeat / ending metadata, tie handoff, and barline-separated measures |
| `convert --from musicxml --to abc [--in <file>|-] [--out <file>|-]` | partial | First ABC export CLI bridge; supports MusicXML text and `.mxl` input through the migrated `musicXmlToAbc` path |
| `render svg [--from musicxml\|abc] [--in <file>\|-] [--out <file>\|-]` | unsupported | Command family is recognized, but SVG output remains blocked by the upstream Verovio/browser runtime dependency |
| `state summarize [--in <file>|-]` | implemented | Emits upstream-shaped JSON summary for MusicXML text input |
| `state inspect-measure --measure <number> [--in <file>|-]` | implemented | Emits upstream-shaped note selectors for one MusicXML measure |
| `state validate-command --command <json> [--in <file>|-]` | implemented | Partial: validates basic command catalog including `ui_noop` |
| `state apply-command --command <json> [--in <file>|-] [--out <file>|-]` | implemented | Partial: applies basic command catalog including no-op behavior |
| `state diff --before <file> --after <file>` | implemented | Emits upstream-shaped JSON diff for two MusicXML files |

## Planned Upstream Command Families

| Upstream command | Java command | Status | Notes |
| --- | --- | --- | --- |
| `convert --from musicxml --to musicxml` | `convert --from musicxml --to musicxml` | implemented | Java-side bridge command for text MusicXML and MXL file-path handling while broader conversion pairs are pending |
| `convert --from abc --to musicxml` | `convert --from abc --to musicxml` | partial | First Java slice covers basic ABC text, tuplet timing, grace groups, basic decorations, repeat / ending metadata, and tie handoff to MusicXML; broader decorations, overlays, diagnostics parity, and golden fixtures remain pending |
| `convert --from musicxml --to abc` | `convert --from musicxml --to abc` | partial | Java CLI bridge covers stdin/file input and `.mxl` decode, then delegates to the migrated `AbcIo.musicXmlToAbc` exporter |
| `convert --from midi --to musicxml` | pending | not started | |
| `convert --from musicxml --to midi` | pending | not started | |
| `convert --from mei --to musicxml` | pending | not started | |
| `convert --from musicxml --to mei` | `convert --from musicxml --to mei` | partial | First Java CLI bridge covers stdin/file MusicXML input and delegates to the migrated MEI exporter through `CoreApi.exportMusicXmlToMei`; broader MEI export parity remains pending |
| `convert --from lilypond --to musicxml` | pending | not started | |
| `convert --from musicxml --to lilypond` | pending | not started | |
| `convert --from musescore --to musicxml` | pending | not started | |
| `convert --from musicxml --to musescore` | pending | not started | |
| `convert --from vsqx --to musicxml` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints |
| `convert --from musicxml --to vsqx` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints |
| `render svg` | `render svg` | unsupported in current Java slice | Java CLI recognizes the command and reports the Verovio/browser runtime constraint |
| `render svg --from abc` | `render svg --from abc` | unsupported in current Java slice | Java CLI recognizes the source option, but SVG rendering has the same Verovio/browser runtime constraint |
| `state summarize` | `state summarize [--in <file>|-]` | partial | Supports MusicXML text input from stdin or file |
| `state inspect-measure` | `state inspect-measure --measure <number> [--in <file>|-]` | partial | Supports MusicXML text input from stdin or file |
| `state validate-command` | `state validate-command --command <json> [--in <file>|-]` | partial | Basic command catalog; supports `targetNodeId`/`selector` or `anchorNodeId`/`anchor_selector` where applicable |
| `state apply-command` | `state apply-command --command <json> [--in <file>|-] [--out <file>|-]` | partial | Basic command catalog; emits MusicXML on success and upstream-shaped apply JSON on validation failure |
| `state diff` | `state diff --before <file> --after <file>` | partial | File inputs only |

## Contract Items To Track

- stdout payload
- stderr diagnostics
- exit codes
- file input / output behavior
- stdin / stdout text behavior
- `.mxl` / `.mscz` file path behavior
- diagnostics text / JSON mode

## Implemented Convert Slice Details

| Contract item | Java behavior |
| --- | --- |
| stdin input | UTF-8 MusicXML text |
| stdout output | UTF-8 MusicXML text unless `--out` names a file |
| `.musicxml` / `.xml` input | UTF-8 MusicXML text |
| `.mxl` input | Decode via `MxlIo.extractMusicXmlTextFromMxl` |
| `.musicxml` / `.xml` output | UTF-8 MusicXML text |
| `.mxl` output | Encode via `MxlIo.makeMxlBytes` |
| unsupported pair | Exit status `2`, message `Unsupported conversion pair: --from ... --to ...` |
