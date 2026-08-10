# Pinned upstream CLI case map

This map covers `tests/unit/miku-score-cli.spec.ts` at renamed Node upstream
revision `a8adc1998237f7b371cae75728afec7dd1795977`.

The SVG renderer and its successful render diagnostics are excluded because
they require Verovio/browser runtime.  CLI recognition of the excluded command
remains covered only as an unsupported-runtime boundary; it is not migration
debt.

| Upstream case | Java regression / owner | Status |
| --- | --- | --- |
| prints top-level and command help | `MikuscoreCliTest#helpReturnsZeroAndMentionsPlannedCommands`, `#convertHelpReturnsZeroAndMentionsMusicXmlPair`, `#stateHelpReturnsZeroAndListsPinnedStateCommands` | done evidence |
| prints the package version | `MikuscoreCliTest#printsThePinnedUpstreamPackageVersion` | done evidence |
| converts stdin to stdout for a supported pair | `MikuscoreCliTest#convertAbcToMusicXmlReadsStdinAndWritesStdout` | done evidence |
| converts ABC directly to MIDI | `MikuscoreCliTest#convertAbcToMidiWritesBytesToStdout` | done evidence |
| converts MEI directly to MusicXML | `MikuscoreCliTest#convertMeiToMusicXmlReadsStdinAndWritesStdout` | done evidence |
| converts MusicXML directly to MEI | `MikuscoreCliTest#convertMusicXmlToMeiReadsStdinAndWritesStdout` | done evidence |
| converts LilyPond directly to MusicXML | `MikuscoreCliTest#convertLilyPondToMusicXmlReadsStdinAndWritesStdout` | done evidence |
| converts MusicXML directly to LilyPond | `MikuscoreCliTest#convertMusicXmlToLilyPondReadsStdinAndWritesStdout` | done evidence |
| writes output via `--out` | `MikuscoreCliTest#convertAbcFileInputWritesMusicXmlFile` | done evidence |
| treats `--out -` as explicit stdout | `MikuscoreCliTest#convertOutDashUsesStdoutForFileBackedInput` | done evidence |
| reads `.mxl` input files for MusicXML source | `MikuscoreCliTest#convertMusicXmlToAbcReadsMxlFileAndWritesAbcFile` | done evidence |
| reads `.mscz` input files for MuseScore source | `MikuscoreCliTest#convertMuseScoreMsczToMusicXmlReadsFileAndWritesStdout` | done evidence |
| writes `.mxl` output | `MikuscoreCliTest#convertMusicXmlToMusicXmlWritesMxlOutputFile` | done evidence |
| writes `.mscz` output | `MikuscoreCliTest#convertMusicXmlToMuseScoreWritesMsczOutputFile` | done evidence |
| renders SVG from MusicXML | Verovio/browser runtime | excluded |
| renders SVG from ABC | Verovio/browser runtime | excluded |
| summarizes canonical MusicXML state | `MikuscoreCliTest#stateSummarizeReadsStdinAndWritesJson` | done evidence |
| validates a bounded command by node ID | `MikuscoreCliTest#stateValidateCommandAcceptsPinnedDirectNodeIdTargeting` | done evidence |
| validates a bounded command by selector | `MikuscoreCliTest#stateValidateCommandReadsStdinAndWritesJson` | done evidence |
| inspects one measure for edit targeting | `MikuscoreCliTest#stateInspectMeasureReadsStdinAndWritesJson` | done evidence |
| applies a bounded command by node ID | `MikuscoreCliTest#stateApplyCommandWritesOutFile` | done evidence |
| applies a bounded command by selector | `MikuscoreCliTest#stateApplyCommandReadsStdinAndWritesMusicXml` | done evidence |
| applies `insert_note_after` via `anchor_selector` | `MikuscoreCliTest#stateApplyCommandAcceptsInsertNoteAfter` | done evidence |
| diffs two canonical MusicXML states | `MikuscoreCliTest#stateDiffReadsFilesAndWritesJson` | done evidence |
| writes JSON diagnostics for successful SVG render | Verovio/browser runtime | excluded |
| writes staged JSON diagnostics for ABC-to-SVG render | Verovio/browser runtime | excluded |
| writes JSON usage diagnostics | `MikuscoreCliTest#convertWritesStructuredUsageDiagnosticsJson` | done evidence |
| reports expected CLI failures | `MikuscoreCliTest#missingStdinInputUsesThePinnedUsageErrorContract`, `#reportsPinnedConversionFailureContracts`, `#stateValidateCommandRequiresExactlyOneCommandPayload`, `#stateValidateCommandReportsPinnedSelectorResolutionFailures`, `#stateInspectMeasureRequiresMeasureOption`, `#stateDiffRequiresBothFileOptions`, `#stateValidateCommandRejectsInvalidCommandJsonAsUsageError`, `#renderSvgRejectsUnsupportedSource` | done evidence |
