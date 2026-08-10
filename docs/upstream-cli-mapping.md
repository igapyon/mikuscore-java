# Upstream CLI Mapping

This document maps the upstream `miku-score` CLI contract to the Java CLI.
All non-Verovio unit cases and all included `cli-api.ts` source exports are
complete in [upstream-cli-case-map.md](upstream-cli-case-map.md) and
[upstream-cli-api-source-map.md](upstream-cli-api-source-map.md). Converter
semantics are owned by their format maps; local-only corpus predicates have
versioned behavior-equivalent evidence there rather than CLI gaps.

## Current Java CLI

| Java command | Status | Notes |
| --- | --- | --- |
| `--help` | done | Documents the implemented `convert` / `state` command split and the Java runtime I/O, exit-code, and overwrite contract |
| `--version` | done | Returns Java package version fallback |
| `convert --from musicxml --to musicxml [--in <file>|-] [--out <file>|-]` | done | First Java convert slice; supports text MusicXML and `.mxl` decode / encode by file extension |
| `convert --from abc --to musicxml [--in <file>|-] [--out <file>|-]` | done | Delegates to the complete public ABC facade |
| `convert --from abc --to midi [--in <file>|-] [--out <file>|-]` | done | Delegates through the complete ABC and MIDI facades |
| `convert --from musicxml --to abc [--in <file>|-] [--out <file>|-]` | done | Delegates to the complete public ABC facade with MXL input support |
| `convert --from mei --to musicxml [--in <file>|-] [--out <file>|-]` | done | Delegates to the public MEI facade; local-only evidence is format-owned |
| `convert --from musicxml --to mei [--in <file>|-] [--out <file>|-]` | done | Delegates to the public MEI facade; local-only evidence is format-owned |
| `convert --from lilypond --to musicxml [--in <file>|-] [--out <file>|-]` | done | Delegates to the complete public LilyPond facade |
| `convert --from musicxml --to lilypond [--in <file>|-] [--out <file>|-]` | done | Delegates to the complete public LilyPond facade |
| `convert --from midi --to musicxml [--in <file>|-] [--out <file>|-]` | done | Delegates to the public MIDI facade; local-only evidence is format-owned |
| `convert --from musicxml --to midi [--in <file>|-] [--out <file>|-]` | done | Delegates to the public MIDI facade; raw and Writer-compatible bytes are covered |
| `convert --from musescore --to musicxml [--in <file>|-] [--out <file>|-]` | done | Delegates to the complete public MSCX/MSCZ facade; local-only evidence is format-owned |
| `convert --from musicxml --to musescore [--in <file>|-] [--out <file>|-]` | done | Delegates to the complete public MSCX/MSCZ facade; local-only evidence is format-owned |
| `render svg [--from musicxml\|abc] [--in <file>\|-] [--out <file>\|-]` | excluded | Command family is recognized, but Verovio/browser rendering is outside the Java parity scope |
| `state summarize [--in <file>|-]` | done | Emits upstream-shaped JSON summary for MusicXML text or `.mxl` file input |
| `state inspect-measure --measure <number> [--in <file>|-]` | done | Emits upstream-shaped note selectors for one MusicXML text or `.mxl` input |
| `state validate-command [--command <json>|--command-file <file>|-] [--in <file>|-]` | done | Validates the complete included Core command catalog; accepts MusicXML text or `.mxl` input |
| `state apply-command [--command <json>|--command-file <file>|-] [--in <file>|-] [--out <file>|-]` | done | Applies the complete included Core command catalog; accepts MusicXML text or `.mxl` input |
| `state diff --before <file> --after <file>` | done | Emits upstream-shaped JSON diff for two MusicXML text or `.mxl` files |

## Planned Upstream Command Families

| Upstream command | Java command | Status | Notes |
| --- | --- | --- | --- |
| `convert --from musicxml --to musicxml` | `convert --from musicxml --to musicxml` | done | Text MusicXML and MXL file-path handling |
| `convert --from abc --to musicxml` | `convert --from abc --to musicxml` | done | Complete public ABC facade |
| `convert --from abc --to midi` | `convert --from abc --to midi` | done | Complete public ABC and MIDI facades |
| `convert --from musicxml --to abc` | `convert --from musicxml --to abc` | done | Complete public ABC facade |
| `convert --from midi --to musicxml` | `convert --from midi --to musicxml` | done | Complete public MIDI facade; local-only evidence is format-owned |
| `convert --from musicxml --to midi` | `convert --from musicxml --to midi` | done | Complete public MIDI facade; local-only evidence is format-owned |
| `convert --from mei --to musicxml` | `convert --from mei --to musicxml` | done | Complete public MEI facade; local-only evidence is format-owned |
| `convert --from musicxml --to mei` | `convert --from musicxml --to mei` | done | Complete public MEI facade; local-only evidence is format-owned |
| `convert --from lilypond --to musicxml` | `convert --from lilypond --to musicxml` | done | Complete public LilyPond facade |
| `convert --from musicxml --to lilypond` | `convert --from musicxml --to lilypond` | done | Complete public LilyPond facade |
| `convert --from musescore --to musicxml` | `convert --from musescore --to musicxml` | done | Complete public MuseScore facade; local-only evidence is format-owned |
| `convert --from musicxml --to musescore` | `convert --from musicxml --to musescore` | done | Complete public MuseScore facade; local-only evidence is format-owned |
| `convert --from vsqx --to musicxml` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints |
| `convert --from musicxml --to vsqx` | excluded | out of initial Java conversion scope | VSQX bridge / dependency constraints |
| `render svg` | `render svg` | excluded from Java parity scope | Java CLI recognizes the command and reports the Verovio/browser runtime constraint |
| `render svg --from abc` | `render svg --from abc` | excluded from Java parity scope | Java CLI recognizes the source option, but SVG rendering has the same Verovio/browser runtime constraint |
| `state summarize` | `state summarize [--in <file>|-]` | done | MusicXML text or `.mxl` input from stdin or file |
| `state inspect-measure` | `state inspect-measure --measure <number> [--in <file>|-]` | done | MusicXML text or `.mxl` input from stdin or file |
| `state validate-command` | `state validate-command [--command <json>|--command-file <file>|-] [--in <file>|-]` | done | Complete Core command catalog with `targetNodeId`/`selector` or `anchorNodeId`/`anchor_selector` |
| `state apply-command` | `state apply-command [--command <json>|--command-file <file>|-] [--in <file>|-] [--out <file>|-]` | done | Complete Core command catalog and upstream-shaped validation responses |
| `state diff` | `state diff --before <file> --after <file>` | done | MusicXML text or `.mxl` file inputs |

## Contract Items To Track

- stdout payload
- stderr diagnostics
- exit codes
- file input / output behavior
- stdin / stdout text behavior
- `.mxl` / `.mscz` file path behavior
- diagnostics text / JSON mode
- overwrite behavior for `--out <file>`

## Implemented Convert Slice Details

| Contract item | Java behavior |
| --- | --- |
| stdin input | Used when `--in` is omitted or `-` is supplied; text routes use UTF-8 and binary MIDI routes use bytes |
| stdout output | Used when `--out` is omitted or `-` is supplied; emits the primary text or binary result |
| `.musicxml` / `.xml` input | UTF-8 MusicXML text |
| `.mxl` input | Decode via `MxlIo.extractMusicXmlTextFromMxl` |
| `.musicxml` / `.xml` output | UTF-8 MusicXML text |
| `.mxl` output | Encode via `MxlIo.makeMxlBytes` |
| MuseScore input / output | `.mscx` UTF-8 text and `.mscz` bytes are decoded or encoded by the `CoreApi` MuseScore facade |
| explicit output file | Existing file is replaced, matching the upstream CLI `writeFileSync` behavior |
| standard error | Usage and processing failures are emitted to stderr; primary results are not mixed with those failures |
| exit codes | `0` success, `1` processing failure, `2` invalid usage or unsupported command / conversion pair |
| diagnostics mode | `--diagnostics text|json` is supported; text is the default and JSON mode emits a version 1 diagnostics envelope to stderr without mixing it into the primary result |
| unsupported pair | Exit status `2`, message `Unsupported conversion pair: --from ... --to ...` |
