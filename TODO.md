---
title: miku-score-java maintenance and migration TODO
description: Current repository-specific work, decisions, and verification for the Java runtime.
topics: [miku-score, java, migration, maintenance]
category: worklog
status: active
audience: [maintainer, developer, agent]
updated: 2026-08-10
sources:
  - type: local-file
    role: primary
    path: docs/remaining-migration-items.md
    checked: 2026-08-09
  - type: local-file
    role: supporting
    path: docs/miku-soft-reference.md
    checked: 2026-08-06
---

# TODO

## Current role

This file is the execution checklist for completing the Java port of
`miku-score`. Detailed findings and evidence belong in
[`docs/remaining-migration-items.md`](docs/remaining-migration-items.md) and
the class, test, CLI, and follow-up mapping documents under `docs/`.

The completed 2026-05 straight-conversion history was retained without
rewriting it in
[`docs/worklog/2026-05-legacy-straight-conversion-log.md`](docs/worklog/2026-05-legacy-straight-conversion-log.md).

## Current project direction

- Preserve `miku-score` upstream semantics in Java; do not begin with a
  Java-first redesign.
- Use the renamed upstream Node repository at `../miku-score` as the reference.
  Do not use the former upstream repository or package name when comparing
  source, tests, commands, or fixtures.
- Keep Java 8, Maven, JUnit Jupiter, and the executable runtime jar as the Java
  runtime and build boundaries.
- Treat MusicXML as the semantic anchor.
- Use `mvn test` as the primary verification command. Run focused JUnit tests
  for the affected Java classes and CLI contract.

## Upstream parity baseline

| Item | Baseline |
| --- | --- |
| Upstream repository | `../miku-score` |
| Upstream package | `miku-score` |
| Upstream version | `v0.6.1` / package version `0.6.1` |
| Upstream revision | `a8adc1998237f7b371cae75728afec7dd1795977` |
| Upstream worktree delta | Included source/test delta: `load-input`, `measure-operations`, `musicxml-output`, `new-score`, `output-encoding`, and load/download-flow updates; browser bundle/UI delta remains excluded |
| Upstream unit-test observation | 37 files, 1,064 passed, 12 skipped; this raw count includes tests that still require in-scope/excluded classification |
| Java test observation | 1,298 passed, 0 failures, 0 errors |
| Baseline date | 2026-08-09 |

Recheck the revision before every parity batch. If upstream has advanced,
record the new revision and first audit the delta from the revision above;
do not silently change the comparison target during a batch.

## Scope boundary

Full parity means parity for every runtime-independent behavior in the
following included scope:

- `core/` state lifecycle, commands, validators, time index, XML utilities,
  accidental spelling, and staff/clef policy;
- `score-features/` and shared beam behavior;
- MusicXML, MXL/ZIP, ABC, MIDI, MEI, LilyPond, and MuseScore import, export,
  conversion, diagnostics, options, and fixtures;
- Core API and CLI contracts for those formats;
- runtime-independent load, download-payload, preview-map, and dense playback
  scheduling helpers when they define score or conversion semantics;
- upstream unit, property, integration, slow, corpus, and golden behavior that
  exercises an included item.

The following items are intentionally excluded and must be recorded as
`excluded`, not left as `partial`, `pending`, or `not started`:

- VSQX import/export and its tests or fixtures;
- Verovio-backed SVG rendering, including `render svg` and rendered preview
  output;
- browser UI and browser-only plumbing: DOM interaction, `File`/file picker,
  object URLs, browser download triggering, WebAudio playback, and page event
  flows;
- Node/Web distribution concerns: npm packaging, TypeScript declaration and
  JavaScript bundles, Vite/browser build output, and web deployment.

## Definition of complete parity

- [x] Inventory every included upstream source export, CLI/API operation,
  option, diagnostic, test case, and fixture against the pinned revision.
- [x] Replace every completed included `partial`, `pending`, and `not started` mapping
  with `done`, with a Java implementation and focused test evidence.
- [x] Mark only the scope-boundary items above as `excluded`; do not use
  `excluded` for difficult runtime-independent behavior.
- [x] Match observable behavior: result fields, score semantics, serialized
  output, defaults, warnings/errors, and CLI exit/stdout/stderr contracts.
  Document only unavoidable Java/Node representation differences.
- [x] Port every active included upstream test case or provide an equivalent
  Java assertion identified by upstream file and case name. Do not use total
  test counts as a substitute for case mapping.
- [x] Mirror every included corpus and golden fixture, or document and test a
  behavior-equivalent Java fixture.
- [x] Pass all non-excluded CFFP cross-format feature-parity cases and round-trip
  checks without losing included score semantics.
- [x] Pass upstream reference tests at the pinned revision and all Java tests
  and packaging checks listed in the closeout phase below.
- [x] Leave no open included item in
  [`docs/upstream-followup-log.md`](docs/upstream-followup-log.md), and make the
  remaining-item and mapping documents agree with this TODO.

## Current remaining work

The ordered execution plan and phase acceptance conditions are maintained in
[`PLAN.md`](PLAN.md). The remaining work at the 2026-08-09 baseline is:

- [x] Finish the atomic upstream case/fixture inventory: map every included
  `describe`/`it`, corpus, golden, integration, property, and slow case to a
  named Java test or an explicit remaining gap. The full 16-case CLI API unit
  suite is now mapped in
  [`docs/upstream-cli-api-case-map.md`](docs/upstream-cli-api-case-map.md), and
  every `cli-api.ts` export now has an owner/evidence row in
  [`docs/upstream-cli-api-source-map.md`](docs/upstream-cli-api-source-map.md).
  The non-excluded `load-flow.spec.ts` cases are likewise mapped in
  [`docs/upstream-load-flow-case-map.md`](docs/upstream-load-flow-case-map.md).
- [x] Close `CORE-01`, `CORE-02`, `CORE-03`, and `CORE-05`:
  `ScoreCore` result/save edges, command optional-field and target-resolution
  behavior, validator diagnostic ordering, XML namespaces, child ordering,
  and unknown-node/attribute preservation are source-audited in
  [`docs/upstream-core-contract-source-map.md`](docs/upstream-core-contract-source-map.md).
  Temporary Java stable-node IDs now
  use a private name plus value marker, select a collision-free name across
  the whole source document, and pass only that exact name through the command
  backend. Generic MusicXML commands treat all source attributes as vendor
  markup, so historic or private-looking attributes survive a dirty save.
  The 54 pinned `core.spec.ts` cases are now individually mapped in
  [`docs/upstream-core-case-map.md`](docs/upstream-core-case-map.md).
  Every exported Core helper is also mapped in
  [`docs/upstream-core-source-map.md`](docs/upstream-core-source-map.md). At
  the stateful boundary, temporary node IDs now resolve every document-order
  `note`, including notes nested in preserved vendor markup, while the CLI's
  explicit `part > measure > note` selector model remains unchanged.
  At
  the unchecked JSON boundary, Java now also retains
  JavaScript numeric `voice` stringification for diagnostics and XML text while
  preserving original-type comparison during validation. Truthy non-string
  target values now remain non-string `Map` lookups (not-found), while falsey
  values retain Node's missing-target result. Existing empty or whitespace-only
  `<voice>` text now follows `xmlUtils.getVoiceText`: a trimmed empty value is
  falsey for validation/save and normalized by a mutating command's fallback,
  while lane matching retains it as distinct from an absent value.
  `insert_note_after` now also retains Node's strict next-lane comparison when
  the anchor has no voice: an omitted, null, or non-string command voice does
  not silently match a following voiced note.
  Repeated `ScoreCore.load` calls now retain the instance node-ID sequence;
  a failed reload keeps the previous document/state but, as in Node, has
  already replaced the clean-save original text.
  Timing validation failures that upstream restores from a serialized snapshot
  now rebuild the same fresh WeakMap-style node-ID sequence without changing
  the clean save result.
  Failures that occur after Node's `ensureVoiceValue` now retain only that
  debug-visible in-memory voice normalization without setting dirty, while
  `save()` follows the original clean-state validation branch.
  XML `Number()` parsing also accepts finite hexadecimal, binary, and octal
  values beyond signed 64-bit range rather than treating them as missing.
  Effective divisions and projected measure capacities now retain that same
  finite JavaScript number range instead of narrowing at Java `int`.
  Blank timing fields now defer to inherited values rather than stopping
  `timeIndex` resolution with a fabricated zero.
  `split_note` accepts the same finite source duration range instead of
  rejecting valid values solely because they exceed Java `int`.
- [x] Close `SHARED-01`: centralize the pinned `beam-common.ts` algorithm in
  `MusicXmlIo` and route MusicXML, ABC, MIDI, and MuseScore conversion through
  it. Direct regressions cover implicit/explicit groups, rests, grace notes,
  beat boundaries, and multi-level beams.
- [x] Close `XML-01`: all MusicXML DOM/text/editor/effective-attribute
  helpers are source-audited. All 10 pinned `musicxml-io.spec.ts` cases and
  every public `musicxml-io.ts` export are mapped in
  [`docs/upstream-musicxml-io-case-map.md`](docs/upstream-musicxml-io-case-map.md)
  and
  [`docs/upstream-musicxml-io-source-map.md`](docs/upstream-musicxml-io-source-map.md).
  Render-document node-ID maps now retain Node `Map` insertion order and its
  unchecked `String(null)` prefix behavior. Measure-editor lookup and copied
  version attributes now retain raw attribute text; empty lane values remain
  distinct from absent voice/staff elements during beam and tuplet grouping.
  Tuplet signatures now use Node `Number()` plus `Math.round()` semantics;
  part-list normalization only supplements the first duplicate ID and checks
  right barlines by their raw `location` attribute. Existing tuplet notation
  selection and attribute supplementation also use raw values, and the
  score-partwise root follows Node's document query rather than assuming it is
  the XML document element. Measure replacement now takes the first
  document-order `part > measure` from an editor wrapper just as the Node
  selector does, and implicit-beam numeric fields retain Node
  `Number.parseInt(value, 10)` decimal-prefix behavior. Pretty-print tag
  compaction and trimming now also use Node's ECMAScript whitespace set,
  including NBSP and BOM; the same trim semantics apply to implicit-beam
  numeric/type text, lane values, tuplet numbers, and normalized part IDs.
  Tuplet enrichment follows Node's document-wide `part > measure` selector
  even if a standalone part is outside `score-partwise`; part-list and final
  barline normalization remain score-partwise-scoped as upstream.
- [x] Complete the public `MUSE-01` MSCX/MSCZ source, metadata, staff/voice,
  direction/spanner, tuplet/grace, notation, option, corpus, and round-trip
  evidence is complete. The 93 pinned public I/O cases are individually
  inventoried in
  [`docs/upstream-musescore-io-case-map.md`](docs/upstream-musescore-io-case-map.md);
  their non-unit/corpus companions are inventoried in
  [`docs/upstream-musescore-nonunit-case-map.md`](docs/upstream-musescore-nonunit-case-map.md).
  All public I/O cases and both public `musescore-io.ts` exports now have
  named Java evidence in
  [`docs/upstream-musescore-source-map.md`](docs/upstream-musescore-source-map.md);
  The local-only semantic predicates are complete through the versioned
  compact fixture recorded in
  [`docs/upstream-musescore-nonunit-case-map.md`](docs/upstream-musescore-nonunit-case-map.md).
- [x] Close `ABC-01`: all lexer exports and all 15 pinned parser unit cases
  have direct Java evidence in
  [`docs/upstream-abc-lexer-source-map.md`](docs/upstream-abc-lexer-source-map.md)
  and [`docs/upstream-abc-parser-case-map.md`](docs/upstream-abc-parser-case-map.md).
- [x] Close `ABC-02`: `abc-layout.ts`, public I/O options, all 408 pinned
  `abc-io.spec.ts` cases, the inline-voice case, and all four pinned goldens
  are complete in
  [`docs/upstream-abc-io-case-map.md`](docs/upstream-abc-io-case-map.md).
- [x] Complete the public `MIDI-01` runtime-facing exports, 70 pinned unit cases, and
  five round-trip runtime goldens are complete in
  [`docs/upstream-midi-io-source-map.md`](docs/upstream-midi-io-source-map.md)
  and [`docs/upstream-midi-io-case-map.md`](docs/upstream-midi-io-case-map.md).
  The local-only Moonlight predicates are complete through the versioned
  compact equivalent in
  [`docs/upstream-midi-nonunit-case-map.md`](docs/upstream-midi-nonunit-case-map.md).
- [x] Complete the public `MEI-01` 108 pinned unit cases and four public samples.
  complete in [`docs/upstream-mei-io-case-map.md`](docs/upstream-mei-io-case-map.md).
  The local-only Paganini checkpoints are complete through the versioned
  compact equivalent in
  [`docs/upstream-mei-nonunit-case-map.md`](docs/upstream-mei-nonunit-case-map.md).
- [x] Close `LILY-01`: all 68 pinned LilyPond cases plus public options and
  diagnostics are complete in
  [`docs/upstream-lilypond-io-case-map.md`](docs/upstream-lilypond-io-case-map.md).
- [x] Close `API-01` and `CLI-01` for the runtime-independent scope. Every
  public `cli-api.ts` export and every non-Verovio CLI unit case is complete in
  [`docs/upstream-cli-api-source-map.md`](docs/upstream-cli-api-source-map.md)
  and [`docs/upstream-cli-case-map.md`](docs/upstream-cli-case-map.md).
  `FLOW-01` through `FLOW-05` are completed separately; browser boundaries
  remain excluded.
- [x] Complete the upstream and Java closeout commands. The full CFFP matrix,
  versioned semantic/text/byte goldens, compact local-only equivalents, and
  final audit are complete.
- [x] Keep VSQX, Verovio/SVG, browser UI/WebAudio, and Node/Web packaging
  recorded as `excluded`; they are not parity blockers.

## Full parity work plan

### P0: Produce an atomic gap matrix

- [x] Fix the initial comparison baseline to upstream `v0.6.1` revision
  `a8adc1998237f7b371cae75728afec7dd1795977`.
- [x] Enumerate upstream `core/`, `src/ts/score-features/`, and included
  `src/ts/*.ts` responsibilities in
  [`docs/upstream-parity-gap-matrix.md`](docs/upstream-parity-gap-matrix.md).
- [x] Enumerate upstream tests down to `describe`/`it` case names and associate
  every case with a Java test, an explicit gap, or an agreed exclusion.
- [x] Inventory corpus/golden inputs and expected outputs, including generated
  or binary fixture provenance and comparison rules.
- [x] Split broad legacy rows in the class, test, and CLI mapping documents
  into atomic rows with an owner module, acceptance condition, and evidence.
- [x] Classify the pure and browser-bound portions of `load-flow`,
  `download-flow`, `preview-flow`, and `playback-flow`; include pure semantic
  helpers and exclude browser plumbing only.

### P1: Complete Core behavior

- [x] Implement `ScoreCore` `editableVoice` normalization, dispatch rejection,
  and save-time overfull validation boundary, with focused regressions.
- [x] Match `ScoreCore` public state access, save, dispatch, and command-result
  details. The source/contract audit is complete in
  [`docs/upstream-core-source-map.md`](docs/upstream-core-source-map.md) and
  [`docs/upstream-core-contract-source-map.md`](docs/upstream-core-contract-source-map.md).
- [x] Retain `ScoreCore` node IDs and changed-ID reporting through pitch and
  duration edits, insert, split, delete-to-rest, chord-head deletion, and
  failed commands. Internal identity markers are never exposed in saved or
  debug XML.
- [x] Finish command preconditions, normalization, dirty-state behavior,
  rollback, and exact warning/error behavior through the complete Core
  contract audit.
- [x] Finish validator parity for inherited attributes, multi-staff/multi-voice
  measures, `backup`/`forward`, chords, grace notes, tuplets, and numeric
  tolerances through the complete Core contract audit.
- [x] Finish time-index parity for all supported structural edit sequences and
  retained anchors. The previously open complex voice-lane timing calculation
  now has a direct regression for the global `backup`/`forward` cursor,
  interleaved voices, and chord-onset exclusion; retain it while auditing
  adjacent identity and result-shape behavior.
- [x] Finish `xmlUtils` parse/serialize, namespace and unknown-node retention,
  duration/notation construction, rest/note construction, and replacement
  semantics through the source audit.
- [x] Verify accidental-spelling and staff/clef-policy behavior across all
  upstream edge cases.
- [x] Port `tests/property/core.property.spec.ts` with its deterministic LCG
  seeds and iteration counts. Java assertion failures include the reproducing
  seed, step, and command text.

### P2: Complete shared score-feature behavior

- [x] Complete `durations`, `articulations`, and `measure-flow` against their
  upstream unit suites, including JavaScript boolean coercion and
  locale-independent normalization behavior.
- [x] Complete `barlines` build/extract/direct-child selection and
  locale-independent normalization behavior against its upstream unit suite.
- [x] Complete `clefs` normalization/build/extract behavior against its
  upstream unit suite, including JavaScript boolean and radix-number coercion.
- [x] Complete `time-signatures` normalization/build/extract behavior against
  its upstream unit suite, including JavaScript boolean and radix-number
  coercion.
- [x] Complete `key-signatures` normalization/build/extract behavior against
  its upstream unit suite, including JavaScript boolean and radix-number
  coercion and locale-independent mode normalization.
- [x] Complete `transposition` normalization/build/extract behavior against
  its upstream unit suite, including JavaScript boolean/radix coercion and
  empty-string defaults during DOM extraction.
- [x] Complete `pitches` normalization/build/extract behavior against its
  upstream unit suite, including JavaScript boolean/radix coercion and
  empty-string defaults during DOM extraction.
- [x] Complete `note-elements` normalization/build/default behavior against
  its upstream unit suite, including no-argument grace construction and
  JavaScript boolean/radix numeric rounding.
- [x] Complete `ties` build/extract state behavior against its upstream unit
  suite, including locale-independent type normalization.
- [x] Complete `slurs` build/extract state behavior against its upstream unit
  suite, including raw build-time type/placement and JavaScript numeric
  coercion semantics.
- [x] Complete `tuplets` time-modification normalization/build/extract behavior
  against its upstream unit suite, including JavaScript boolean/radix coercion.
- [x] Complete `ornaments` normalization/build/extract behavior against its
  upstream unit suite, including raw build-time kind/tremolo type and
  JavaScript numeric coercion semantics.
- [x] Complete `dynamics` normalization/build/extract behavior against its
  upstream unit suite, including raw build-time values, JavaScript numeric
  coercion, first-child common fields, and dynamic-before-wedge extraction.
- [x] Complete `direction-text` normalization/build/extract behavior against
  its upstream unit suite, including raw presentation values, JavaScript
  numeric coercion, and locale-independent extraction normalization.
- [x] Match `beam-common` grouping and route every included Node user through
  one Java implementation: MusicXML, ABC, MIDI, and MuseScore now share
  explicit/implicit grouping, rest/grace handling, beat boundaries, and beam
  XML construction.
- [x] Port all upstream `score-features` unit cases with matching fixture
  intent.
- [x] Port all upstream `beam-common` cases with matching fixture intent,
  including JavaScript `Math.round` level handling.

### P3: Complete MusicXML and container behavior

- [x] Finish MusicXML DOM/text parsing, canonical serialization, editor-
  document handling, effective-attribute resolution, implicit beams, and
  measure replacement behavior.
- [x] Port `new-score.ts`'s included eight-measure, piano-grand-staff, and
  public-option template cases; evidence is in
  `docs/upstream-new-score-case-map.md`.
- [x] Port all included `measure-operations.ts` text-facade cases: extraction,
  replacement, inherited full-measure rests, and G/F grand-staff backup
  generation. Evidence is in `docs/upstream-measure-operations-case-map.md`.
- [x] Port all included `musicxml-output.ts` metadata-policy and imported-ABC
  diagnostic-summary cases. Evidence is in
  `docs/upstream-musicxml-output-case-map.md`.
- [x] Port all included `output-encoding.ts` unit cases for value-based
  MusicXML/MXL, ABC, raw and Writer-compatible MIDI, MSCX/MSCZ, and ZIP
  text/byte outputs.
  VSQX is excluded; evidence is in `docs/upstream-output-encoding-case-map.md`.
- [x] Match the Node non-raw MIDI-writer backend with a Java-native
  MidiWriterJS 2.1.4-compatible plan serializer. It covers FF03/FF04 names,
  program/note/controller event ordering, SMF headers, and Node nullish
  `rawWriter` selection; a four-track exact-byte regression is in
  `docs/upstream-midi-writer-byte-source-map.md`.
- [x] Preserve supported metadata, namespaces, unknown elements/attributes,
  ordering, divisions, voices, staves, notation, directions, and layout data
  through state operations and round trips.
- [x] Finish MXL/ZIP rootfile selection, `META-INF/container.xml`, media type,
  filename, compression, UTF-8/XML declarations, malformed archive errors,
  and source-style timestamped output behavior. Preferred compression now follows the
  upstream size rule: use raw DEFLATE only when it reduces an entry payload;
  otherwise retain a stored entry. Central-directory range and malformed-entry
  diagnostics, plus filename/local-header/data range diagnostics and
  requested-entry unsupported-compression handling, now match the upstream
  preflight. The encoder now writes the source-style UTF-8 local/central/EOCD
  layout without extra fields or data descriptors. All four pinned upstream
  MXL and MSCZ sample archives are extracted and repackaged in the Java
  regression suite, and a timestamp-normalized raw-DEFLATE archive byte golden
  generated by the pinned Node source matches Java output.
- [x] Finish the included runtime-independent download-payload options. Load
  routing/value conversion is done in `docs/upstream-load-flow-case-map.md`
  and `docs/upstream-load-input-case-map.md`; download unit cases are complete
  in `docs/upstream-download-flow-case-map.md`. MIDI `ticksPerQuarter`,
  program/override, grace/accent, export-profile, and roundtrip-metadata
  parameters are now exposed; raw and non-raw Writer-compatible byte branches
  are selected with Node-equivalent option precedence.
  Browser file reading and download triggering remain excluded.
- [x] Port all included `musicxml-io`, `mxl-io`, `zip-io`, load-flow, and
  download-flow cases and fixtures.

### P4: Complete every included converter

#### ABC

- [x] Complete ABC lexer/parser, layout, public options, conversion,
  diagnostics, inline voice switching, and all pinned unit/golden cases.
  The authoritative inventory is
  [`docs/upstream-abc-io-case-map.md`](docs/upstream-abc-io-case-map.md).

#### MIDI

- [x] Match import parsing, track/channel assignment, quantization, divisions,
  tempo/meter/key changes, polyphonic voice/staff assignment, controllers,
  percussion, metadata, warnings, and malformed-file handling for every
  pinned public unit case.
- [x] Match export scheduling and raw/Writer-compatible MIDI writing for ties,
  grace notes, tuplets, ornaments, dynamics, tempo/meta events, simultaneous
  notes, retriggers, channels/programs, percussion, deterministic ordering,
  and all public round-trip goldens.
- [x] Match `midi-musescore-io` profile normalization, MuseScore-parity TPQ,
  runtime options, and playback-build-mode selection. Evidence is in
  `docs/upstream-midi-musescore-io-source-map.md`.
- [x] Port all MIDI unit and round-trip golden cases; the authoritative case
  map is [`docs/upstream-midi-io-case-map.md`](docs/upstream-midi-io-case-map.md).
- [x] Port the two local-only Moonlight semantic reference-MIDI spot checks
  with the versioned compact equivalent and all pinned quantization grids;
  see [`docs/upstream-midi-nonunit-case-map.md`](docs/upstream-midi-nonunit-case-map.md).

#### MEI

- [x] Complete MEI public import/export, options, all 108 pinned unit cases,
  and all four public sample fixtures. The evidence is in
  [`docs/upstream-mei-io-case-map.md`](docs/upstream-mei-io-case-map.md).
- [x] Port the local-only Paganini MEI semantic roundtrip checkpoints with the
  versioned compact equivalent; see
  [`docs/upstream-mei-nonunit-case-map.md`](docs/upstream-mei-nonunit-case-map.md).

#### LilyPond

- [x] Complete LilyPond public import/export options and all 68 pinned unit
  cases. The unit suite is the upstream slow entry and has no separate
  LilyPond corpus/integration file; the case inventory is
  [`docs/upstream-lilypond-io-case-map.md`](docs/upstream-lilypond-io-case-map.md).

#### MuseScore

- [x] Connect the advanced helpers already present in Java to the public
  MSCX/MSCZ import/export facade; do not count unreachable helpers as parity.
- [x] Match metadata, parts/instruments, staff properties, measure/voice
  hierarchy, timing, layout, source/debug data, options, warnings, and errors.
  The public MSCX import/export overloads now route cut-time 4/4-to-2/2
  normalization, while public MSCX import also routes default/disabled
  implicit-beam inference (including compound 6/8 grouping) and basic
  dynamic/tempo/expression/marker/jump directions. Public export now routes
  dynamics and words as well. Raw source/version metadata is now emitted by
  default and controllable through direct/CoreApi import options; the Core API
  and CLI now also normalize imported MusicXML (including final barlines).
  Cut-time is controllable through direct/CoreApi export options. Generated
  diagnostic metadata now reaches public MSCX import for the source's
  no-readable-staff placeholder case and observes `debugMetadata`; dropped
  unknown-duration and missing-pitch public events now also retain detailed
  `mks:diag` context, and unsupported public voice elements now produce one
  sorted `unsupported-elements` warning. Broader generated-warning coverage
  and CLI option parsing still need public wiring. Public
  MSCX/CoreApi import now also emits trimmed `metaTag` metadata for work,
  movement, subtitle, creators, rights, and creation date, with VBox title and
  composer fallback for default MuseScore placeholders. Readable staffs now
  obey the Node grouping contract: empty declared parts are skipped, declared
  staff IDs are assigned only once, and unclaimed staffs become individual
  fallback parts. Part-level transpose now emits MusicXML `<transpose>` and
  switches KeySig priority to the source's `transposeKey`. MSCZ output is
  normalized with the upstream two-space XML indentation before ZIP packaging.
  Public MSCX import also emits key mode from `KeySig`/`keysig`, falling back
  to the Node title, movement-title, and VBox inference when no explicit mode
  exists. MuseScore measure `len` fractions now control capacity and emit the
  source-compatible implicit pickup marker and first measure number.
  `startRepeat`/`endRepeat`, voice markers, and `BarLine`/`barline` subtypes
  now emit the matching MusicXML repeat and double/final barlines.
  Grand-staff imports now preserve each Staff's clef sign and line in numbered
  MusicXML `<clef>` elements.
  Part `Staff/defaultClef` and `Instrument/clef` defaults now follow the Node
  staff-ID precedence before source-measure clefs are considered.
  Measure-direct signatures now take priority over voice signatures, and a
  cut-time symbol remains active across later explicit time signatures.
  Grace and acciaccatura chords now preserve their MusicXML grace markers and
  do not consume measure time.
  Note-level `Tie`/`endSpanner` markers now produce matching MusicXML `<tie>`
  and `<tied>` start/stop items.
  Chord-level known articulation and technical subtypes, plus note fingering
  and string values, now emit MusicXML notations on the leading note.
  Chord `Slur type=start|stop` transitions now retain their IDs across measures
  in staff/voice-local MusicXML slur notation.
  Chord-local trill ornaments now emit MusicXML trill and accidental-mark
  notation.
  Per-event MuseScore `track` and `move` values now route notes and directions
  to their MusicXML voice and staff.
  MuseScore key context, explicit accidentals, and `tpc` values now select the
  MusicXML pitch spelling and accidental state.
  `durationType=measure` rests now span the current MuseScore measure capacity.
  ID-referenced MuseScore tuplets now retain written type, scaled duration,
  time-modification, and contiguous start/stop notation.
  Standalone and Chord-local Ottava spanners now retain start/stop numbers
  across measures, emit MusicXML octave-shift directions, and apply the active
  8va/8vb/15ma/15mb display pitch shift.
  Standalone and Chord-local Trill spanners now retain their shared number
  across measures and emit MusicXML trill-mark/wavy-line start and stop
  notation.
  Absolute MuseScore `Tick` events now become measure-relative MusicXML voice
  `forward` gaps; reverse and cross-lane cursor reconstruction is covered by
  the completed public case map.
  Voice-level repeat `BarLine` events now emit a source-positioned middle
  MusicXML repeat barline, including `end-start-repeat`.
  ID-less inline MuseScore tuplets now retain their duration scale, start
  display attributes, and `endTuplet` stop notation for chord/rest and nested
  events; mixed ID-reference cases use the same completed public import path.
  Lowercase MuseScore `tuplet` ID definitions and chord references now follow
  the same imported duration/time-modification path as `Tuplet`.
  Public MSCX import now assigns MusicXML voice numbers part-wide by sorted
  staff/local-lane pairs, including `track`/`move` destinations, rather than
  using a staff-offset implementation detail.
  MusicXML unpitched display-step/octave notes now round trip through MSCX as
  timed chord events without becoming rests.
  MusicXML octave-shift directions now reach public MSCX export as pending
  Ottava spanners and return as MusicXML start/stop directions; middle-repeat
  marks use the same pending export path and round trip as a source-positioned
  `end-start-repeat` voice `BarLine`.
  MusicXML trill wavy-lines now round trip through public MSCX Trill spanners
  with their start/stop number preserved.
  Chord-local `Spanner[type=Slur]`, including lowercase `spanner`, now shares
  the same cross-measure MusicXML slur-number state as legacy `Slur` markers.
  Dropped public MSCX chord/rest events now retain `mks:diag` context for
  unknown duration or missing pitch when `debugMetadata` is enabled.
  Unsupported public MSCX voice elements now produce one sorted
  `unsupported-elements` `mks:diag` warning, while upstream-ignored layout and
  signature tags remain silent.
  MusicXML `trill-mark` without a wavy-line now round trips through the
  chord-local MSCX `ornamentTrill` subtype rather than a Trill spanner.
  Public MusicXML export now has direct regressions for staccato, accent, and
  tenuto MSCX articulation subtypes.
  An already-dirty `ScoreCore` now has direct result-shape evidence for a
  second successful dispatch: `dirtyChanged=false` with its changed ID,
  affected measure, and empty warning/diagnostic lists retained.
  Overfull public MSCX voice lanes now omit the overflowing tail event and
  retain the source-style `mks:diag` `clamped/overfull` payload with occupied
  and capacity divisions.
  Public MusicXML export now has direct regressions for technical stopped/
  up-bow/open-string/harmonic plus fingering/string values, and for multi-staff
  Part clef scaffolding with an instrument shortName.
  Public MusicXML tie/slur export now has a direct regression for MSCX
  Tie/endSpanner note markers and start/stop Slur span fractions.
  Public MSCX import now has a direct regression for Dynamic placement in its
  source multi-voice MusicXML lane.
  Unsupported public MSCX Tuplet definitions now retain a source-style
  `skipped/unsupported` `mks:diag` entry with measure/staff/voice/tick context.
  Unsupported visible Dynamic, Expression, Marker, and Jump events now retain
  the same source-style `skipped/unsupported` diagnostic context.
- [x] Match directions, dynamics/hairpins, repeats/endings, barlines, tempo and
  text; beams, tuplets and grace notes; ties, slurs, trills, ornaments,
  articulations and technical marks; ottava, transpose, clef, key, and meter.
- [x] Match MSCX text and MSCZ container/resource behavior, file selection,
  encoding, deterministic output, and round-trip preservation.
- [x] Port the complete public MuseScore unit, corpus, integration, and round-trip
  cases.
- [x] Port the local-only Mozart, Paganini, and Moonlight MuseScore semantic
  spot/round-trip assertions with versioned compact equivalents; see
  [`docs/upstream-musescore-nonunit-case-map.md`](docs/upstream-musescore-nonunit-case-map.md).

### P5: Complete Core API and CLI contracts

- [x] Expose every included conversion pair and upstream option through
  `CoreApi` with matching defaults, structured result fields, diagnostics, and
  byte/text behavior.
- [x] Match state create/summarize/inspect/validate/apply/diff behavior and
  response shapes for all included Core operations.
- [x] Match CLI command/alias discovery, help, version, option parsing,
  stdin/file handling, output naming, overwrite policy, MXL, MSCX/MSCZ, JSON
  and text diagnostics, stdout/stderr separation, and exit codes.
- [x] Add negative contract tests for invalid commands/options/input, missing
  tools/files, malformed containers, conflicting output modes, and partial
  writes.
- [x] Keep VSQX, `render svg`, browser preview, and browser playback absent by
  explicit exclusion rather than placeholder implementations.

### P6: Prove cross-format and behavioral parity

- [x] Run the pinned non-excluded CFFP cross-format feature-parity cases
  through all applicable bridges. [`CffpSeriesTest`](src/test/java/jp/igapyon/mikuscore/coreapi/CffpSeriesTest.java)
  executes all 86 cases and all 44 declared preservation predicates; see
  [`docs/upstream-cffp-case-map.md`](docs/upstream-cffp-case-map.md).
- [x] Add semantic golden comparisons where byte equality is inappropriate,
  and deterministic byte/text goldens where upstream promises stable output.
- [x] Add round-trip assertions for each format and cross-format assertions via
  the MusicXML semantic anchor. The CFFP matrix records the VSQX exclusion;
  local-only fixtures are represented by versioned behavior-equivalent
  semantic evidence rather than allowlisted format limitations.
- [x] Port invalid-input, warning, option-matrix, and regression fixtures; make
  diagnostics part of expected results rather than incidental log output.
- [x] Verify runtime-independent preview ID mapping and dense playback schedule
  compaction/timeline helpers. Verovio rendering and WebAudio execution remain
  excluded.

### P7: Close out the migration

- [x] Run upstream `npm run test:unit` and `npm run test:property`, plus every
  included integration and slow suite, at the pinned revision. Record commands,
  versions, pass/skip totals, and any upstream-only environmental prerequisite.
  The reference run is recorded in
  [`docs/upstream-verification-2026-08-10.md`](docs/upstream-verification-2026-08-10.md).
- [x] Run Java focused tests, `mvn test`, and `mvn package`; smoke-test the
  executable jar for help, version, all included conversions, state operations,
  diagnostics, stdin/stdout, and container formats.
  See [`docs/java-verification-2026-08-10.md`](docs/java-verification-2026-08-10.md).
- [x] Update `docs/upstream-class-mapping.md`,
  `docs/upstream-test-mapping.md`, `docs/upstream-cli-mapping.md`,
  `docs/upstream-followup-log.md`, and `docs/remaining-migration-items.md` so
  they contain only `done` or agreed `excluded` entries.
- [x] Audit the final tree against the pinned upstream source/test/fixture
  inventories; record all intentional Java runtime differences and confirm no
  included responsibility is unmapped.
  See [`docs/parity-closeout-audit-2026-08-10.md`](docs/parity-closeout-audit-2026-08-10.md).
- [x] Remove completed migration TODOs and declare parity complete after the
  definition-of-complete checklist above is fully checked.

## Repository maintenance outside parity

- [ ] Record an installed miku-soft skill commit when the deployed skill source
  exposes Git metadata. The copy inspected on 2026-08-06 was not a Git
  worktree, so its exact commit could not be recorded.
- [x] Rename the repository and adopt canonical `miku-score` Maven, CLI, and
  Release artifact names; see [`docs/rename-migration.md`](docs/rename-migration.md)
  and [`docs/release-artifact-migration.md`](docs/release-artifact-migration.md).

## 2026-08-06 miku-soft maintenance record

| Field | Record |
| --- | --- |
| Profile | Java application / straight-conversion maintenance |
| Shared reference | [`docs/miku-soft-reference.md`](docs/miku-soft-reference.md), checked 2026-08-06 |
| Applied | Replace copied shared miku-soft policy documents with a project-local reference; synchronize documented Java CLI support and make its runtime contract explicit. |
| Deferred | Public artifact-name and Release-asset changes require the dedicated compatibility decision in [`docs/release-artifact-migration.md`](docs/release-artifact-migration.md). The shared maintenance backlog is still decision-gated. |
| Verification | `mvn package`; built runtime `--version` and `--help`; distribution ZIP contents; focused CLI contract tests. |
| Next action | Continue a bounded upstream-parity slice from `docs/remaining-migration-items.md`. |

## 2026-08-09 repository rename record

| Field | Record |
| --- | --- |
| Issue | GitHub Issue #31 |
| GitHub operation | The repository was renamed by a human from `mikuscore-java` to `miku-score-java`; the old URL redirects to the new URL. |
| Applied | Updated `origin`, Maven coordinates and outputs, CLI display/help, README, release workflow input paths, and the artifact migration record. |
| Compatibility | Java package `jp.igapyon.mikuscore` and public class names remain unchanged; historical references are listed in `docs/rename-migration.md`. |
| Verification | `mvn test`, `mvn package`, CLI `--help` / `--version`, artifact and ZIP-content checks. |
