# Upstream CLI Mapping

This document maps the upstream `mikuscore` CLI contract to the Java CLI.

## Current Java CLI

| Java command | Status | Notes |
| --- | --- | --- |
| `--help` | implemented | Documents the implemented `convert` / `state` command split and the Java runtime I/O, exit-code, and overwrite contract |
| `--version` | implemented | Returns Java package version fallback |
| `convert --from musicxml --to musicxml [--in <file>|-] [--out <file>|-]` | implemented | First Java convert slice; supports text MusicXML and `.mxl` decode / encode by file extension |
| `convert --from abc --to musicxml [--in <file>|-] [--out <file>|-]` | partial | First ABC import/export bridge; supports basic ABC text headers, notes, rests, chords, tuplets, grace groups, basic decorations, repeat / ending metadata, tie handoff, and barline-separated measures |
| `convert --from abc --to midi [--in <file>|-] [--out <file>|-]` | partial | First ABC-to-MIDI bridge; imports ABC to MusicXML, then delegates to `CoreApi.exportMusicXmlToMidi` |
| `convert --from musicxml --to abc [--in <file>|-] [--out <file>|-]` | partial | First ABC export CLI bridge; supports MusicXML text and `.mxl` input through the migrated `musicXmlToAbc` path |
| `convert --from mei --to musicxml [--in <file>|-] [--out <file>|-]` | partial | First MEI import bridge through `CoreApi.importMeiToMusicXml` |
| `convert --from musicxml --to mei [--in <file>|-] [--out <file>|-]` | partial | First MEI export bridge through `CoreApi.exportMusicXmlToMei` |
| `convert --from lilypond --to musicxml [--in <file>|-] [--out <file>|-]` | partial | First LilyPond import CLI bridge; supports UTF-8 LilyPond text input and MusicXML/MXL output through `CoreApi.importLilyPondToMusicXml` |
| `convert --from musicxml --to lilypond [--in <file>|-] [--out <file>|-]` | partial | First LilyPond export bridge through `CoreApi.exportMusicXmlToLilyPond` |
| `convert --from midi --to musicxml [--in <file>|-] [--out <file>|-]` | partial | First MIDI import CLI bridge; supports MIDI byte input and MusicXML/MXL output through `CoreApi.importMidiToMusicXml` |
| `convert --from musicxml --to midi [--in <file>|-] [--out <file>|-]` | partial | First MIDI export CLI bridge; supports MusicXML/MXL input and MIDI byte output through `CoreApi.exportMusicXmlToMidi` |
| `convert --from musescore --to musicxml [--in <file>|-] [--out <file>|-]` | partial | First MuseScore/MSCZ import bridge through `CoreApi.importMuseScoreToMusicXml` |
| `convert --from musicxml --to musescore [--in <file>|-] [--out <file>|-]` | partial | First MuseScore/MSCZ export bridge through `CoreApi.exportMusicXmlToMuseScore` |
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
| `convert --from abc --to midi` | `convert --from abc --to midi` | partial | First Java CLI bridge imports ABC to MusicXML and exports MIDI bytes through the migrated MIDI facade; broader ABC/MIDI parity remains pending |
| `convert --from musicxml --to abc` | `convert --from musicxml --to abc` | partial | Java CLI bridge covers stdin/file input and `.mxl` decode, then delegates to the migrated `AbcIo.musicXmlToAbc` exporter |
| `convert --from midi --to musicxml` | `convert --from midi --to musicxml` | partial | First Java CLI bridge reads MIDI bytes from stdin/file and delegates to `CoreApi.importMidiToMusicXml` / `MidiIo.convertMidiToMusicXml`; broader MIDI import parity remains pending |
| `convert --from musicxml --to midi` | `convert --from musicxml --to midi` | partial | First Java CLI bridge reads MusicXML text/MXL, delegates to `CoreApi.exportMusicXmlToMidi`, and writes SMF bytes; broader MIDI export parity remains pending |
| `convert --from mei --to musicxml` | `convert --from mei --to musicxml` | partial | First Java CLI bridge covers stdin/file MEI text input and delegates to the migrated MEI importer through `CoreApi.importMeiToMusicXml`; broader MEI import parity remains pending |
| `convert --from musicxml --to mei` | `convert --from musicxml --to mei` | partial | First Java CLI bridge covers stdin/file MusicXML input and delegates to the migrated MEI exporter through `CoreApi.exportMusicXmlToMei`; broader MEI export parity remains pending |
| `convert --from lilypond --to musicxml` | `convert --from lilypond --to musicxml` | partial | First Java CLI bridge delegates to `CoreApi.importLilyPondToMusicXml`; broader LilyPond import parity remains pending |
| `convert --from musicxml --to lilypond` | `convert --from musicxml --to lilypond` | partial | First Java export bridge uses `CoreApi.exportMusicXmlToLilyPond`; broader LilyPond export parity remains pending |
| `convert --from musescore --to musicxml` | `convert --from musescore --to musicxml` | partial | First Java import bridge accepts `.mscx` text and `.mscz` containers through `CoreApi.importMuseScoreToMusicXml` |
| `convert --from musicxml --to musescore` | `convert --from musicxml --to musescore` | partial | First Java export bridge emits MuseScore text or MSCZ bytes through `CoreApi.exportMusicXmlToMuseScore` |
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
| diagnostics mode | Upstream supports `--diagnostics text|json`; Java has no equivalent option yet, so it must not be passed as if supported |
| unsupported pair | Exit status `2`, message `Unsupported conversion pair: --from ... --to ...` |
