# Upstream Parity Gap Matrix

This is the working inventory for Java parity with the renamed upstream
repository `../miku-score` at `v0.6.1`
(`a8adc1998237f7b371cae75728afec7dd1795977`). It turns broad mapping rows
into implementation-sized work packages. Detailed evidence stays in the class,
test, and CLI mappings; this file owns priority and acceptance conditions.

## Status vocabulary

| Status | Meaning |
| --- | --- |
| `done` | The included responsibility has an implementation and focused Java evidence. It still participates in final cross-format audit. |
| `excluded` | Explicitly outside the agreed Java scope; it is not migration debt. |

## Included source inventory

Incremental `MUSE-01` evidence: public MSCX import preserves staff-specific
numbered clefs and applies Part `Staff/defaultClef` / `Instrument/clef`
staff-ID precedence before source-measure clefs. Measure-direct TimeSig/KeySig
values also take priority over voice values, with retained cut-time state.
Grace and acciaccatura chords preserve MusicXML grace markers without consuming time.
Note `Tie`/`endSpanner` markers emit MusicXML tie/tied start/stop items, and
known chord articulation/technical subtypes and note fingering/string values
emit leading-note notations. Chord Slur transitions retain staff/voice-local
IDs across measures. Chord-local trill ornaments emit trill and accidental-mark
notations. Event `track`/`move` values route notes and directions to their
MusicXML voice/staff. Key context, explicit accidentals, and `tpc` values
select MusicXML pitch spelling and accidental state. `durationType=measure`
rests span the current measure capacity. ID-referenced tuplets retain written
type, scaled duration, time-modification, and contiguous start/stop notation.
Standalone and Chord-local Ottava spanners retain start/stop numbers across
measures, emit octave-shift directions, and apply their active display pitch
shift during MSCX import.
Standalone and Chord-local Trill spanners retain their shared number across
measures and emit MusicXML trill-mark/wavy-line start and stop notation.
Absolute MuseScore `Tick` events become measure-relative MusicXML voice
`forward` gaps; reverse and cross-lane cursor reconstruction is covered by
the case-mapped Java regressions.
Voice-level repeat `BarLine` events emit a source-positioned middle MusicXML
repeat barline, including `end-start-repeat`.
ID-less inline MuseScore tuplets retain their duration scale, start display
attributes, and `endTuplet` stop notation for chord/rest and nested events;
mixed ID-reference cases are covered by the case-mapped Java regressions.
Lowercase MuseScore `tuplet` ID definitions and chord references follow the
same imported duration/time-modification path as `Tuplet`.
Public MSCX import assigns MusicXML voice numbers part-wide by sorted
staff/local-lane pairs, including `track`/`move` destinations.
Pinned MusicXML `.mxl` samples with a standard public `DOCTYPE` now parse by
retaining the declaration while preventing external-entity resolution, matching
the upstream non-fetching conversion path. `sample2.mxl` has direct MSCX
export regressions for the viola/cello default clefs and the Violin 1 measure-2
slur stop span.
MusicXML unpitched display-step/octave notes round trip through MSCX as timed
chord events without becoming rests.
MusicXML octave-shift directions reach public MSCX export as pending Ottava
spanners and return as MusicXML start/stop directions; middle-repeat marks use
the same pending export path and round trip as a source-positioned
`end-start-repeat` voice `BarLine`.
MusicXML trill wavy-lines round trip through public MSCX Trill spanners with
their start/stop number preserved.
Chord-local `Spanner[type=Slur]`, including lowercase `spanner`, shares the
same cross-measure MusicXML slur-number state as legacy `Slur` markers.
Dropped public MSCX chord/rest events retain detailed `mks:diag` context for
unknown duration or missing pitch when `debugMetadata` is enabled.
Unsupported public MSCX voice elements retain one sorted `unsupported-elements`
`mks:diag` warning, while upstream-ignored layout and signature tags stay silent.
MusicXML `trill-mark` without a wavy-line round trips through chord-local MSCX
`ornamentTrill`, not a Trill spanner.
Public MusicXML export has direct regressions for staccato, accent, and tenuto
MSCX articulation subtypes.
An already-dirty `ScoreCore` has direct result-shape evidence for a second
successful dispatch: `dirtyChanged=false` with changed ID, affected measure,
and empty warning/diagnostic lists retained.
Overfull public MSCX voice lanes omit the overflowing tail event and retain a
source-style `mks:diag` `clamped/overfull` payload with occupied and capacity
divisions.
Public MusicXML export has direct regressions for technical stopped/up-bow/
open-string/harmonic plus fingering/string values, and for multi-staff Part
clef scaffolding with an instrument shortName.
Public MusicXML tie/slur export has a direct regression for MSCX Tie/endSpanner
note markers and start/stop Slur span fractions.
Public MSCX import has a direct regression for Dynamic placement in its source
multi-voice MusicXML lane.
Unsupported public MSCX Tuplet definitions retain a source-style
`skipped/unsupported` `mks:diag` entry with measure/staff/voice/tick context.
Unsupported visible Dynamic, Expression, Marker, and Jump events retain the
same source-style `skipped/unsupported` diagnostic context.
The pinned public `sample7` has direct roundtrip evidence for the upstream
measure-3/4 pitch-event sequence (onset, duration, staff, spelling, alter,
and accidental) and for its measure-7/staff-4 B3 natural accidental.
MusicXML Instrument transpose (including diatonic/chromatic fields and the
written-key scaffold) also has a direct public MuseScore roundtrip regression.

| ID | Upstream source | Java target | Status | Remaining acceptance condition |
| --- | --- | --- | --- | --- |
| CORE-01 | `core/ScoreCore.ts` | `core.ScoreCore`, `MusicXmlState` | done | Lifecycle, load/dispatch/save modes, result ordering, post-`ensureVoiceValue` failure state, and snapshot-restored WeakMap-style node-ID regeneration are source-audited in [`upstream-core-contract-source-map.md`](upstream-core-contract-source-map.md). |
| CORE-02 | `core/commands.ts`, `core/interfaces.ts` | `ScoreCore`, `MusicXmlCommandValidation` | done | Command discriminator, target resolution, optional fields, dirty/change reporting, and all public result fields are source-audited in [`upstream-core-contract-source-map.md`](upstream-core-contract-source-map.md). |
| CORE-03 | `core/validators.ts` | `MusicXmlState` | done | Payload, voice, note-kind, lane, structural-boundary, and finite-number timing diagnostics are source-audited in [`upstream-core-contract-source-map.md`](upstream-core-contract-source-map.md). |
| CORE-04 | `core/timeIndex.ts` | `MusicXmlState` | done | Inherited timing context plus global `backup`/`forward` cursor, chord-onset exclusion, and multi-voice occupancy are ported and regression-tested. Blank timing fields defer to inherited values; finite Number()-style source durations and capacities—including values beyond Java `int`—drive command projection and underfull warnings, while integer overflows can partially consume fractional rests as upstream; re-audit only with final corpus fixtures. |
| CORE-05 | `core/xmlUtils.ts` | `MusicXmlState`, `MusicXmlIo` | done | Parse/serialize, child ordering, note/rest replacement, duration notation, namespace/unknown preservation, and document-order node targeting are source-audited in [`upstream-core-source-map.md`](upstream-core-source-map.md) and [`upstream-core-contract-source-map.md`](upstream-core-contract-source-map.md). |
| CORE-06 | `core/accidentalSpelling.ts` | `AccidentalSpelling` | done | Re-audit only against final cross-format fixtures. |
| CORE-07 | `core/staffClefPolicy.ts` | `StaffClefPolicy` | done | Re-audit only against final cross-format fixtures. |
| CORE-08 | `src/ts/new-score.ts` | `core.NewScore` | done | Eight-measure multi-part and piano grand-staff templates plus public option normalization are case-mapped in [`upstream-new-score-case-map.md`](upstream-new-score-case-map.md). |
| CORE-09 | `src/ts/measure-operations.ts` | `MusicXmlIo` | done | The runtime-independent extract, replace, and append text facades are case-mapped in [`upstream-measure-operations-case-map.md`](upstream-measure-operations-case-map.md), including inherited timing and treble/bass grand-staff backup generation. |
| CORE-10 | `src/ts/musicxml-output.ts` | `MusicXmlOutput` | done | Runtime-independent metadata stripping and imported-ABC diagnostic summaries are case-mapped in [`upstream-musicxml-output-case-map.md`](upstream-musicxml-output-case-map.md). |
| FEATURE-01 | `score-features/durations.ts` | `ScoreDurations` | done | All upstream unit cases are ported; final cross-format audit remains. |
| FEATURE-02 | `score-features/articulations.ts` | `ScoreArticulations` | done | All upstream unit cases are ported, with locale-independent normalization; final cross-format audit remains. |
| FEATURE-03 | `score-features/barlines.ts` | `ScoreBarlines` | done | All upstream unit cases are ported, including direct-child first-match behavior and locale-independent extraction normalization; final cross-format audit remains. |
| FEATURE-04 | `score-features/clefs.ts` | `ScoreClefs` | done | All upstream unit cases are ported, including JavaScript boolean and radix-number coercion in normalization; final cross-format audit remains. |
| FEATURE-05 | `score-features/time-signatures.ts` | `ScoreTimeSignatures` | done | All upstream unit cases are ported, including JavaScript boolean and radix-number coercion in normalization; final cross-format audit remains. |
| FEATURE-06 | `score-features/key-signatures.ts` | `ScoreKeySignatures` | done | All upstream unit cases are ported, including JavaScript boolean and radix-number coercion plus locale-independent mode normalization; final cross-format audit remains. |
| FEATURE-07 | `score-features/transposition.ts` | `ScoreTranspositions` | done | All upstream unit cases are ported, including JavaScript boolean/radix coercion and empty-string defaults during direct-child extraction; final cross-format audit remains. |
| FEATURE-08 | `score-features/pitches.ts` | `ScorePitches` | done | All upstream unit cases are ported, including JavaScript boolean/radix coercion and empty-string defaults during direct-child extraction; final cross-format audit remains. |
| FEATURE-09 | `score-features/note-elements.ts` | `ScoreNoteElements` | done | All upstream unit cases are ported, including default no-argument grace construction and JavaScript boolean/radix numeric rounding; final cross-format audit remains. |
| FEATURE-10 | `score-features/ties.ts` | `ScoreTies` | done | All upstream unit cases are ported; extraction keeps sound ties and notation ties separate with locale-independent type normalization. Final cross-format audit remains. |
| FEATURE-11 | `score-features/slurs.ts` | `ScoreSlurs` | done | All upstream unit cases are ported, including raw build-time type/placement semantics and JavaScript numeric coercion; final cross-format audit remains. |
| FEATURE-12 | `score-features/tuplets.ts` | `ScoreTuplets` | done | All upstream unit cases are ported, including JavaScript boolean/radix coercion for actual/normal note counts; final cross-format audit remains. |
| FEATURE-13 | `score-features/ornaments.ts` | `ScoreOrnaments` | done | All upstream unit cases are ported, including raw build-time kind/tremolo type behavior and JavaScript numeric coercion; final cross-format audit remains. |
| FEATURE-14 | `score-features/measure-flow.ts` | `ScoreMeasureFlow` | done | All upstream unit cases are ported, including JavaScript boolean-number coercion; final cross-format audit remains. |
| FEATURE-15 | `score-features/dynamics.ts` | `ScoreDynamics` | done | All upstream unit cases are ported, including raw build-time values, JavaScript numeric coercion, first-child common fields, and dynamic-before-wedge extraction order; final cross-format audit remains. |
| FEATURE-16 | `score-features/direction-text.ts` | `ScoreDirectionText` | done | All upstream unit cases are ported, including raw presentation values, JavaScript numeric coercion, and locale-independent extraction normalization; final cross-format audit remains. |
| SHARED-01 | `src/ts/beam-common.ts` | `MusicXmlIo` | done | Full implicit/explicit grouping and beam XML construction are centralized and used by MusicXML, ABC, MIDI, and MuseScore. Direct regressions cover rest/grace boundaries, multi-level beams, and explicit `mid` continuation. |
| XML-01 | `src/ts/musicxml-io.ts` | `MusicXmlIo`, `MusicXmlState` | done | All public source exports and all 10 pinned unit cases are mapped in [`upstream-musicxml-io-source-map.md`](upstream-musicxml-io-source-map.md) and [`upstream-musicxml-io-case-map.md`](upstream-musicxml-io-case-map.md). DOM/text behavior, normalization, effective attributes, editor documents, and replacement helpers retain raw selector attributes, Node map/string coercion, wrapper-measure selection, document-wide tuplet selection, decimal-prefix beam parsing, and ECMAScript whitespace pretty printing. `measure-operations.ts` and `musicxml-output.ts` are complete separately; final cross-format audit remains. |
| XML-02 | `src/ts/mxl-io.ts`, `src/ts/zip-io.ts` | `MxlIo`, `CoreApi` | done | Rootfile/container selection, fallback extensions, exact-entry bytes, stored ZIP mode, size-based stored/DEFLATE selection, source-style UTF-8 local/central/EOCD header layout, and stable empty/missing-EOCD/central-directory-range/central-directory-malformed/filename-range/local-header/data-range/missing-entry/requested-entry-unsupported-compression errors are covered. All four pinned upstream MXL and MSCZ archives are regression-tested for extraction and Java repackaging, and a timestamp-normalized raw-DEFLATE byte golden generated from the pinned Node source matches Java output. |
| FLOW-01 | `src/ts/load-flow.ts`, `src/ts/load-input.ts` | `CoreApi` | done | File extension routing, value-based text/byte conversion, direct source selection/new-score result fields, MXL/MSCZ fallback, and structured MIDI diagnostics are case-mapped in [`upstream-load-flow-case-map.md`](upstream-load-flow-case-map.md) and [`upstream-load-input-case-map.md`](upstream-load-input-case-map.md). Browser `File`/`FileReader` APIs and VSQX conversion are excluded. |
| FLOW-02 | `src/ts/download-flow.ts` | `CoreApi`, `MxlIo` | done | All included `download-flow.spec.ts` cases are case-mapped done in [`upstream-download-flow-case-map.md`](upstream-download-flow-case-map.md); ZIP bundle payloads preserve entry order and duplicates, MEI-version is routed, and the MIDI TPQ/preset/override/grace/accent/profile/roundtrip options select both raw and Writer-compatible byte backends. Browser triggering and VSQX payload are excluded. |
| FLOW-03 | `src/ts/preview-flow.ts` | `MusicXmlIo` | done | The only runtime-independent helper, `preparePreviewSvgIdMap`, is case-mapped in [`upstream-preview-flow-case-map.md`](upstream-preview-flow-case-map.md), including the source `mks-` predicate. Verovio rendering and browser UI are excluded. |
| FLOW-04 | `src/ts/playback-flow.ts`, `src/ts/playback.ts` | `MidiIo` | done | Dense schedule compaction and pickup-aware part measure timelines are case-mapped in [`upstream-playback-flow-case-map.md`](upstream-playback-flow-case-map.md). `playback.ts` only re-exports MIDI APIs; WebAudio and browser controls are excluded. |
| FLOW-05 | `src/ts/output-encoding.ts` | `CoreApi` | done | All included `output-encoding.spec.ts` cases are mapped in [`upstream-output-encoding-case-map.md`](upstream-output-encoding-case-map.md): MusicXML/MXL, ABC, both MIDI writer branches, MSCX/MSCZ, and ZIP text/byte outputs are done. VSQX is excluded. The non-raw branch has an exact MidiWriterJS 2.1.4 plan-to-SMF regression in [`upstream-midi-writer-byte-source-map.md`](upstream-midi-writer-byte-source-map.md). |
| MIDI-01 | `src/ts/midi-io.ts` | `MidiIo` | done | All runtime-facing exports, 70 pinned MIDI unit cases, three generated baseline goldens, and two semantic goldens are complete in [upstream-midi-io-source-map.md](upstream-midi-io-source-map.md) and [upstream-midi-io-case-map.md](upstream-midi-io-case-map.md). The local-only Moonlight checks have a versioned compact behavior-equivalent fixture in [upstream-midi-nonunit-case-map.md](upstream-midi-nonunit-case-map.md). |
| MIDI-02 | `src/ts/midi-musescore-io.ts` | `MidiIo` | done | All three source exports—profile normalization, profile-owned runtime options, and event-build-mode selection—are directly mapped in [`upstream-midi-musescore-io-source-map.md`](upstream-midi-musescore-io-source-map.md). |
| ABC-01 | `src/ts/abc-lexer.ts`, `abc-parser.ts` | `AbcLexer`, `AbcParser` | done | All lexer exports are mapped in [`upstream-abc-lexer-source-map.md`](upstream-abc-lexer-source-map.md), and all 15 pinned parser cases are mapped in [`upstream-abc-parser-case-map.md`](upstream-abc-parser-case-map.md). |
| ABC-02 | `src/ts/abc-layout.ts`, `abc-io.ts` | `AbcIo` | done | Public exports, layout, options, and all 408 pinned `abc-io.spec.ts` cases, one inline-voice case, and four pinned goldens are mapped in [upstream-abc-io-source-map.md](upstream-abc-io-source-map.md) and [upstream-abc-io-case-map.md](upstream-abc-io-case-map.md). |
| MEI-01 | `src/ts/mei-io.ts` | `MeiIo` | done | All public exports, 108 pinned unit cases, four public samples, and the local-only Paganini semantic checkpoints are complete in [upstream-mei-io-source-map.md](upstream-mei-io-source-map.md), [upstream-mei-io-case-map.md](upstream-mei-io-case-map.md), and [upstream-mei-nonunit-case-map.md](upstream-mei-nonunit-case-map.md). The Verovio visual test is excluded. |
| LILY-01 | `src/ts/lilypond-io.ts` | `LilyPondIo` | done | Public options/diagnostics and all 68 pinned LilyPond-specific cases are complete in [upstream-lilypond-io-source-map.md](upstream-lilypond-io-source-map.md) and [upstream-lilypond-io-case-map.md](upstream-lilypond-io-case-map.md). The cross-format CFFP suite is also complete in [upstream-cffp-case-map.md](upstream-cffp-case-map.md). |
| MUSE-01 | `src/ts/musescore-io.ts` | `MuseScoreIo`, `CoreApi` | done | All public source exports, 93 pinned unit cases, public samples, integration, round-trip evidence, and local-only semantic predicates are done in [upstream-musescore-io-source-map.md](upstream-musescore-io-source-map.md), [upstream-musescore-io-case-map.md](upstream-musescore-io-case-map.md), and [upstream-musescore-nonunit-case-map.md](upstream-musescore-nonunit-case-map.md). |
| API-01 | `src/ts/cli-api.ts` | `CoreApi` | done | All 16 pinned `cli-api.spec.ts` cases and every public source export are mapped in [upstream-cli-api-case-map.md](upstream-cli-api-case-map.md) and [upstream-cli-api-source-map.md](upstream-cli-api-source-map.md). |
| CLI-01 | `scripts/miku-score-cli.mjs` | `MikuscoreCli` | done | Every non-render CLI unit case is mapped in [upstream-cli-case-map.md](upstream-cli-case-map.md): package/version, help, stdin/stdout/file I/O, explicit `--out -`, MXL/MSCZ, state commands, diagnostics, failures, and exit codes. Render remains excluded. |

## Explicit exclusions

| Upstream source or responsibility | Status | Boundary |
| --- | --- | --- |
| `src/ts/vsqx-io.ts` and VSQX fixtures/payloads | excluded | VSQX is outside the Java parity target. |
| `src/ts/verovio-out.ts`, `render svg`, rendered SVG tests | excluded | Verovio rendering is outside the Java target. |
| `src/ts/render-document.ts`, `tests/unit/render-document.spec.ts` | excluded | MusicXML preparation whose sole consumer is Verovio is included in the Verovio exclusion. |
| Browser-only portions of `main.ts`, load/download/preview/playback flow | excluded | DOM, file picker, object URL, download triggering, page events, and WebAudio execution are browser UI. |
| npm/Vite/TypeScript declarations/bundles and web deployment | excluded | Node/Web distribution is not a Java runtime contract. |
| `sampleXml*.ts` source constants | excluded | Imported only by browser `main.ts` built-in sample controls; browser UI is outside scope. |

## Immediate atomic queue

The queue below is the next implementation order. An item moves to `done` only
when its listed Java test names and the mapped upstream cases pass.

| Queue ID | Work item | Upstream evidence | Java acceptance evidence |
| --- | --- | --- | --- |
| C-01 (done) | All 54 `core.spec.ts` cases have a named Java regression in [`upstream-core-case-map.md`](upstream-core-case-map.md). This batch added public `ScoreCore` regressions for inherited attributes/divisions/time capacity (TI-3 through TI-5), clean UI noop (DR-1), and target voice mismatch (BF-1). | `core.spec.ts` RT/TI/BF/SV/PL/AT/MP cases | The case map plus focused `ScoreCoreTest`, `MusicXmlStateTest`, and `ScoreCorePropertyTest`. |
| C-02 (done) | Validator diagnostic ordering, optional command fields, JavaScript numeric/whitespace behavior, target truthiness, discriminator handling, and complete dispatch result shapes are source-audited. | `validators.ts`, `core.spec.ts` payload/boundary cases | [`upstream-core-contract-source-map.md`](upstream-core-contract-source-map.md) plus focused `ScoreCore` and CLI JSON diagnostics tests. |
| C-03 (done) | `xmlUtils.ts` child ordering, namespace/unknown-node retention, exact stable-ID scoping, rest/pitch child order, and generated-rest notation synchronization are source-audited. | `xmlUtils.ts`, `core.spec.ts` preservation cases | [`upstream-core-source-map.md`](upstream-core-source-map.md) plus DOM-level `MusicXmlStateTest` and `ScoreCoreTest` regressions. |
| F-01 (done) | Score-feature and shared-beam unit series are complete. | `tests/unit/score-*.spec.ts`, beam tests | Every score-feature and beam case has named JUnit evidence. |
| X-01 (done) | MusicXML/MXL/ZIP container, payload, and pure-flow cases are complete. | `musicxml-io`, `load-flow`, `download-flow`, CLI tests | Atomic case maps plus fixture byte/text and error-contract tests. |
| A-01 (done) | ABC lexer/parser, layout, public I/O, and goldens are complete. | `abc-parser`, `abc-io`, golden tests | [ABC I/O case map](upstream-abc-io-case-map.md), focused `AbcIoTest`, and semantic round trips. |
| M-01 (done) | MIDI profile/options, all public unit/golden byte/semantic tests, and compact local-only equivalent evidence are complete. | `midi-musescore-io`, `midi-io`, golden tests | [`upstream-midi-io-case-map.md`](upstream-midi-io-case-map.md) and [`upstream-midi-nonunit-case-map.md`](upstream-midi-nonunit-case-map.md). |
| E-01 (done) | MEI, LilyPond, and the local MEI semantic checkpoint equivalent are complete. | respective unit/integration/slow suites | Case-mapped Java tests and [upstream-mei-nonunit-case-map.md](upstream-mei-nonunit-case-map.md). |
| U-01 (done) | MuseScore source, public cases, samples, round-trip corpus, and local semantic equivalents are complete. | `musescore-io` and roundtrip cases | [`upstream-musescore-io-case-map.md`](upstream-musescore-io-case-map.md) and [`upstream-musescore-nonunit-case-map.md`](upstream-musescore-nonunit-case-map.md). |
| K-01 (done) | Core API and CLI contract matrices are complete. | `cli-api`, CLI unit cases | API/CLI contract tests including negative paths. |

## Inventory completion record

- 2026-08-09: source-level included/excluded inventory completed.
- 2026-08-09: test/corpus inventory completed against the pinned revision.
  The 26 tracked upstream MusicXML fixture basenames exactly match the 26
  Java test-resource basenames; the source sample inventory contains 18
  format samples. `local-*` spot suites remain included evidence but require
  their non-versioned local reference data/tooling at final audit time.

## Case and corpus inventory record

Every versioned family below has named Java evidence. The three local fixture
families remain visible so they cannot silently be mistaken for exclusions.

| Upstream evidence family | Scope | Current Java evidence / next queue |
| --- | --- | --- |
| `core.spec.ts`, `property/core.property.spec.ts`, `accidental-spelling.spec.ts` | included | All Core source/contract exports and cases are complete in [upstream-core-case-map.md](upstream-core-case-map.md), [upstream-core-source-map.md](upstream-core-source-map.md), and [upstream-core-contract-source-map.md](upstream-core-contract-source-map.md). |
| `score-*.spec.ts`, `beam-common.spec.ts` | included | All 16 score-feature suites and the beam helper unit suite are done. The shared helper now also supplies every included converter path. |
| `musicxml-io.spec.ts`, `load-flow.spec.ts`, `download-flow.spec.ts`, `preview-flow.spec.ts`, `playback-flow.spec.ts` | included only for runtime-independent semantics | `MusicXmlIoTest`, `MxlIoTest`, `CoreApiTest`, and `MidiIoTest`; X-01 is done for container/error and pure-flow cases. Browser DOM/WebAudio actions are excluded. |
| `cli-api.spec.ts`, `miku-score-cli.spec.ts` | included | `CoreApiTest` and `MikuscoreCliTest`; K-01 is done for full option/error and byte/text contract parity. |
| `abc-inline-voice-switch.spec.ts`, `abc-io.spec.ts`, `abc-parser.spec.ts`, `abc-roundtrip-golden.spec.ts` | included | `AbcLexerTest`, `AbcParserTest`, and `AbcIoTest`; the complete pinned case inventory is [upstream-abc-io-case-map.md](upstream-abc-io-case-map.md). |
| `midi-io.spec.ts`, `midi-roundtrip-golden.spec.ts`, `src/samples/midi/*` | included | All 70 unit and five runtime golden cases are complete in [upstream-midi-io-case-map.md](upstream-midi-io-case-map.md), including the compact local Moonlight equivalent in [upstream-midi-nonunit-case-map.md](upstream-midi-nonunit-case-map.md). The four source samples are not referenced by a pinned executable MIDI case. |
| `mei-io.spec.ts`, `src/samples/mei/*` | included | All 108 unit cases, four public samples, and the local Paganini semantic equivalent are complete in [upstream-mei-io-case-map.md](upstream-mei-io-case-map.md) and [upstream-mei-nonunit-case-map.md](upstream-mei-nonunit-case-map.md). |
| `lilypond-io.spec.ts` | included | All 68 cases are complete in [upstream-lilypond-io-case-map.md](upstream-lilypond-io-case-map.md). |
| `musescore-io.spec.ts`, `tests/roundtrip/musescore/cases.public.json`, `musicxml-musescore-sample6.roundtrip.spec.ts`, `src/samples/musescore/*` | included | All public source, unit, sample, integration, round-trip corpus, and local-only semantic-equivalent evidence is complete in the MuseScore maps. |
| `tests/spot/*.spec.ts`, `tests/roundtrip/musescore/cases.local.example.json` | included at final audit when prerequisites are available | P6/P7 must record the required local reference data and external conversion tooling; no browser UI behavior is implied. |
| `cffp-series.spec.ts`, 26 `tests/fixtures/*.musicxml`, 18 `src/samples/*` inputs | included | The CFFP semantic anchor is complete: all 86 pinned cases execute through the five included bridges, with 44 exact feature predicates in [upstream-cffp-case-map.md](upstream-cffp-case-map.md). All 26 tracked fixture basenames have Java resource counterparts. |
| `vsqx-io.spec.ts` and VSQX payload/fixtures | excluded | Explicit agreed exclusion; never count as partial migration debt. |
| `domAssertions.ts`, `fixtureLoader.ts`, `fixtures/README.md` | test infrastructure | Audit provenance only; they do not define a separate runtime responsibility. |
