# Upstream CLI Mapping

This document maps the upstream `mikuscore` CLI contract to the Java CLI.

## Current Java CLI

| Java command | Status | Notes |
| --- | --- | --- |
| `--help` | implemented | Foundation help only |
| `--version` | implemented | Returns Java package version fallback |
| `state summarize [--in <file>|-]` | implemented | Emits upstream-shaped JSON summary for MusicXML text input |
| `state inspect-measure --measure <number> [--in <file>|-]` | implemented | Emits upstream-shaped note selectors for one MusicXML measure |
| `state validate-command --command <json> [--in <file>|-]` | implemented | Partial: validates basic command catalog including `ui_noop` |
| `state apply-command --command <json> [--in <file>|-] [--out <file>|-]` | implemented | Partial: applies basic command catalog including no-op behavior |
| `state diff --before <file> --after <file>` | implemented | Emits upstream-shaped JSON diff for two MusicXML files |

## Planned Upstream Command Families

| Upstream command | Java command | Status | Notes |
| --- | --- | --- | --- |
| `convert --from abc --to musicxml` | pending | not started | |
| `convert --from musicxml --to abc` | pending | not started | |
| `convert --from midi --to musicxml` | pending | not started | |
| `convert --from musicxml --to midi` | pending | not started | |
| `convert --from mei --to musicxml` | pending | not started | |
| `convert --from musicxml --to mei` | pending | not started | |
| `convert --from lilypond --to musicxml` | pending | not started | |
| `convert --from musicxml --to lilypond` | pending | not started | |
| `convert --from musescore --to musicxml` | pending | not started | |
| `convert --from musicxml --to musescore` | pending | not started | |
| `convert --from vsqx --to musicxml` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints |
| `convert --from musicxml --to vsqx` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints |
| `render svg` | pending | not started | Java render feasibility needs review |
| `render svg --from abc` | pending | not started | Upstream one-shot flow is internally ABC -> MusicXML -> SVG |
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
