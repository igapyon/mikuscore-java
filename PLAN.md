---
title: miku-score Java upstream parity execution plan
description: Ordered implementation and verification plan for completing the included upstream parity scope.
status: active
updated: 2026-08-10
---

# PLAN

## Objective

Complete Java parity with the renamed Node upstream `../miku-score` at
`v0.6.1` revision `a8adc1998237f7b371cae75728afec7dd1795977`, except for the
explicit exclusions below. `TODO.md` is the checklist; this file defines the
execution order, phase boundaries, and acceptance conditions.

The checked reference also has a local upstream worktree delta. Its included
source/test additions (`load-input`, `measure-operations`, `musicxml-output`,
`new-score`, `output-encoding`, and their unit cases, plus the load/download
flow updates) are part of this parity target and are individually mapped in
`docs/`. The bundle, HTML, browser main, and UI changes remain outside scope.

Current verified Java baseline: 1,302 tests, 0 failures, 0 errors, with
`mvn -q package` and `git diff --check` passing.

## Scope boundary

Included work covers Core state/commands/validators/XML, score features,
shared beams, MusicXML/MXL/ZIP, ABC, MIDI, MEI, LilyPond, MuseScore,
Core API/CLI, and runtime-independent load/download/preview/playback behavior.

The following remain `excluded` and must not be counted as unfinished parity:

- VSQX import/export and fixtures;
- Verovio-backed SVG rendering;
- browser DOM/UI, file picker, object URL, download triggering, page flow, and
  WebAudio execution;
- npm/Vite/TypeScript bundles, declarations, web deployment, and other
  Node/Web distribution work.

## Execution phases

### Phase 0: Finish the atomic evidence map

Status: complete.

Work:

- Decompose every included upstream test file to `describe`/`it` case names.
- All 93 included MuseScore I/O cases and both public source exports are done
  in [`docs/upstream-musescore-io-case-map.md`](docs/upstream-musescore-io-case-map.md)
  and [`docs/upstream-musescore-source-map.md`](docs/upstream-musescore-source-map.md).
  Its local-only semantic predicates are complete through versioned compact
  behavior-equivalent fixtures in
  [`docs/upstream-musescore-nonunit-case-map.md`](docs/upstream-musescore-nonunit-case-map.md).
- Map each case to a named Java test, a phase item below, or an agreed
  exclusion.
- Record fixture provenance and comparison rules for corpus, golden, binary,
  generated, integration, property, spot, and slow cases.
- Split broad `partial` mapping rows when they hide more than one independent
  acceptance condition.

Acceptance:

- No included source export, option, diagnostic, case, or fixture is unmapped.
- Mapping documents and `docs/upstream-parity-gap-matrix.md` identify one
  implementation owner and one verification target for every remaining item.

### Phase 1: Close Core, XML, and shared-beam foundations

Status: complete (source audit; final corpus audit remains).

Work:

- `CORE-01`, `CORE-02`, `CORE-03`, and `CORE-05`: done. Core
  lifecycle, contracts, validators, and XML utility behavior are source-audited
  in [`docs/upstream-core-contract-source-map.md`](docs/upstream-core-contract-source-map.md).
- `XML-01`: done. Public MusicXML parsing/serialization, normalization,
  effective attributes, editor-document, and measure-replacement behavior are
  source-audited in
  [`docs/upstream-musicxml-io-source-map.md`](docs/upstream-musicxml-io-source-map.md).
- `SHARED-01`: done. `MusicXmlIo.computeBeamAssignments` now implements the
  pinned `beam-common.ts` algorithm and is used by the MusicXML, ABC, MIDI,
  and MuseScore converter paths; direct regressions cover implicit/explicit
  groups, rests, grace notes, beat boundaries, and multiple beam levels.

Acceptance:

- Every remaining included `core.spec.ts` and `musicxml-io.spec.ts` case has a
  named Java regression.
- Converter-local beam behavior agrees with the shared helper for equivalent
  MusicXML lanes.
- Focused Core/MusicXML tests and the full Maven package pass.

### Phase 2: Complete MuseScore public and corpus parity

Status: complete.

Work:

- All public source/export, 93 unit-case, public sample, integration, and
  round-trip evidence is complete in the MuseScore source/case maps.
- The untracked Mozart/Paganini/Moonlight source assets are represented by a
  versioned, compact behavior-equivalent fixture with focused pickup,
  pitch-event, control-event, and MSCX-import evidence.

Acceptance:

- All public `MUSE-01` evidence is done, with direct facade, Core API, CLI,
  MSCX, and MSCZ coverage.
- `MUSE-01` is fully done, including its local-only semantic predicates.

### Phase 3: Complete ABC parity

Status: complete.

Completion evidence:

- All lexer/parser exports and cases are mapped.
- `abc-layout.ts`, every public I/O option/export, the 408 pinned I/O cases,
  inline-voice case, and four pinned goldens are covered by
  [`docs/upstream-abc-io-case-map.md`](docs/upstream-abc-io-case-map.md).

Acceptance:

- `ABC-01` and `ABC-02` are `done`.
- Every active upstream ABC case and golden has case-mapped Java evidence.

### Phase 4: Complete MIDI and pure playback parity

Status: complete.

Work:

- All runtime-facing `midi-io.ts` exports, the 70 pinned unit cases, and five
  round-trip runtime goldens are complete in
  [`docs/upstream-midi-io-source-map.md`](docs/upstream-midi-io-source-map.md)
  and [`docs/upstream-midi-io-case-map.md`](docs/upstream-midi-io-case-map.md).
- The local-only Moonlight spot predicates are complete through the versioned
  compact Moonlight-equivalent fixture in
  [`docs/upstream-midi-nonunit-case-map.md`](docs/upstream-midi-nonunit-case-map.md).
- The runtime-independent playback timeline and dense-schedule helpers are
  complete; WebAudio remains excluded.

Acceptance:

- `MIDI-02` and the pure portion of `FLOW-04` are `done`; all public
  `MIDI-01` evidence is done.
- `MIDI-01` is done, including the local-only practical-diff predicates.

### Phase 5: Complete MEI and LilyPond parity

Status: complete.

Work:

- Complete MEI metadata, score/staff definitions, layers, controls/spans,
  notation, source/debug data, options, warnings/errors, and corpus cases.
- Complete LilyPond parsing/export contexts, variables/relative pitch,
  simultaneous voices/staffs, notation, lyrics, repeats, deterministic syntax,
  diagnostics, round trips, and slow cases.

Acceptance:

- `MEI-01` and `LILY-01` are `done`.
- All included public, corpus, integration, round-trip, slow, and local-only
  semantic-equivalent cases pass.

### Phase 6: Close pure flows, Core API, and CLI contracts

Status: complete (runtime-independent scope).

Work:

- The Node non-raw MIDI-writer byte backend is complete through a Java-native
  MidiWriterJS 2.1.4-compatible plan serializer, including exact-byte evidence.
  Load/load-input source/result fields, unit cases, payload container behavior, MEI-version
  routing, ZIP bundles, ZIP duplicate-entry order, and MIDI download runtime
  options are complete.
- Value-based output encoding is complete for all included unit cases:
  MusicXML/MXL, text converters, both MIDI writer modes, MSCX/MSCZ, and ZIP
  text/byte entries. VSQX is excluded.
- Pure preview ID mapping is done; SVG rendering remains excluded.
- Match every included Core API operation, default, option, result field,
  warning/diagnostic, and text/byte contract.
- Match CLI discovery/help/version, commands/aliases, stdin/file/output paths,
  MXL/MSCX/MSCZ, overwrite behavior, diagnostics modes, stdout/stderr, negative
  paths, and exit codes.

Acceptance:

- `FLOW-01`, `FLOW-02`, `FLOW-03`, `API-01`, and `CLI-01` are `done`.
- All included `cli-api`, CLI, load, download, and preview-flow cases pass.

### Phase 7: Cross-format proof and closeout

Status: complete.

Work:

- Completed: `CffpSeriesTest` executes all 86 pinned CFFP cases through ABC,
  MEI, LilyPond, MuseScore, and MIDI; it verifies the first-fact semantic
  policies and all 44 upstream-declared feature predicates. VSQX remains
  excluded. See [`docs/upstream-cffp-case-map.md`](docs/upstream-cffp-case-map.md).
- The untracked local corpus predicates have versioned compact semantic
  equivalents; no fixture supply is required for parity acceptance.
- Use semantic goldens where byte equality is inappropriate and deterministic
  text/byte goldens where upstream promises stable output.
- Run upstream unit/property/integration/slow suites at the pinned revision and
  record commands, versions, totals, skips, and external prerequisites.
  Completed on 2026-08-10; see
  [`docs/upstream-verification-2026-08-10.md`](docs/upstream-verification-2026-08-10.md).
- Run Java focused tests, full tests/package, executable-jar smoke tests, all
  included conversions/state commands, diagnostics, stdin/stdout, and
  container paths.
- Reconcile `TODO.md`, all mapping documents, the gap matrix, remaining-items,
  and follow-up log.

Acceptance:

- Every included mapping is `done`; only agreed scope boundaries are
  `excluded`.
- No included follow-up item remains open.
- All upstream reference checks and Java closeout checks pass before declaring
  complete parity.

## Closeout evidence

The original local-only source assets remain absent from the pinned upstream
checkout, but their observable predicates are covered by versioned compact
Java fixtures. See the MIDI, MEI, and MuseScore non-unit maps.

All versioned Core and MusicXML work is complete in
[`docs/upstream-core-case-map.md`](docs/upstream-core-case-map.md) and
[`docs/upstream-musicxml-io-case-map.md`](docs/upstream-musicxml-io-case-map.md).
The shared beam foundation is also complete: the pinned `beam-common.ts`
algorithm is centralized in `MusicXmlIo` and its MusicXML, ABC, MIDI, and
MuseScore users now call that implementation rather than converter-local
grouping code. `XML-01` is now source-audited; its public exports and
all pinned cases are mapped in
[`docs/upstream-musicxml-io-source-map.md`](docs/upstream-musicxml-io-source-map.md).
No Core/XML implementation batch remains; the cross-format matrix and all
local-only semantic-equivalent corpus evidence are complete.
The completed `XML-01` work includes aligned render-map insertion order and
`String(null)` prefixes, raw measure-editor attribute lookup/version
retention, and the distinction between empty and absent beam/tuplet lane
fields. The source audit also covers wrapper editor replacement selection and the
decimal-prefix numeric parsing used by the implicit-beam timeline.
Pretty-print compaction and trimming use the same ECMAScript whitespace set
as the pinned Node implementation.
Tuplet enrichment now follows the source's document-wide `part > measure`
selector rather than the narrower score-partwise-only traversal.
The Core command pass now also uses Node's trimmed `getVoiceText` semantics:
an empty value is falsey for target validation/save and normalized by a
mutating command, while lane comparisons retain it as distinct from a missing
voice.
Stateful Core node-ID resolution now follows the source document-order
`querySelectorAll("note")` scope, including preserved vendor markup; this
does not broaden the Java CLI selector facade.
Timing failures that upstream restores from a serialized snapshot now also
rebuild the Core node-ID sequence while preserving the clean save result.
The state lifecycle now retains node-ID counters across reloads and preserves
the upstream's original-text assignment ordering on a failed reload.
The first Phase-2 public MuseScore warning path is also connected: importing
an MSCX with no readable staff emits the source placeholder diagnostic when
`debugMetadata` is enabled.
The same facade now reads trimmed MuseScore `metaTag` values, uses VBox title
and composer text when the source carries MuseScore's default placeholders,
and emits the resulting work/movement/subtitle/identification metadata through
both direct conversion and `CoreApi`.
Readable MuseScore staffs now follow the source grouping contract as well:
empty declared parts are skipped, each declared staff is assigned once, and
unclaimed staffs become separately named fallback parts.
Part-level MuseScore transposition now reaches MusicXML `<transpose>` and
selects the source's written-key (`transposeKey`) priority for such parts.
The direct importer now also emits MusicXML key mode from `KeySig`/`keysig`,
with the source's title, movement-title, and VBox text inference as its
initial fallback.
MuseScore measure `len` fractions now set the source capacity and mark a
short first measure as the implicit pickup (`number="0"`).
MuseScore repeat markers and `BarLine`/`barline` subtypes now produce the
source-compatible left double/forward and right backward/final barlines.
Grand-staff imports now preserve each Staff's clef sign and line in numbered
MusicXML `<clef>` elements.
Part `Staff/defaultClef` and `Instrument/clef` defaults now follow the Node
staff-ID precedence before source-measure clefs are considered.
Measure-direct signatures now take priority over voice signatures, and a
cut-time symbol remains active across later explicit time signatures.
Grace and acciaccatura chords now preserve their MusicXML grace markers and
do not consume measure time.
Note-level `Tie`/`endSpanner` markers now produce matching MusicXML `<tie>` and
`<tied>` start/stop items.
Chord-level known articulation and technical subtypes, plus note fingering and
string values, now emit MusicXML notations on the leading note.
Chord `Slur type=start|stop` transitions now retain their IDs across measures
in staff/voice-local MusicXML slur notation.
Chord-local trill ornaments now emit MusicXML trill and accidental-mark
notation.
Per-event MuseScore `track` and `move` values now route notes and directions to
their MusicXML voice and staff.
MuseScore key context, explicit accidentals, and `tpc` values now select the
MusicXML pitch spelling and accidental state.
`durationType=measure` rests now span the current MuseScore measure capacity.
ID-referenced MuseScore tuplets now retain written type, scaled duration,
time-modification, and contiguous start/stop notation.
Standalone and Chord-local Ottava spanners now retain start/stop numbers across
measures, emit MusicXML octave-shift directions, and apply the active 8va/8vb/
15ma/15mb display pitch shift.
Standalone and Chord-local Trill spanners now retain their shared number across
measures and emit MusicXML trill-mark/wavy-line start and stop notation.
Absolute MuseScore `Tick` events now become measure-relative MusicXML voice
`forward` gaps; the public case map covers reverse and cross-lane cursor
reconstruction as part of the completed import path.
Voice-level repeat `BarLine` events now emit a source-positioned middle
MusicXML repeat barline, including `end-start-repeat`.
ID-less inline MuseScore tuplets now retain their duration scale, start display
attributes, and `endTuplet` stop notation for chord/rest and nested events;
mixed ID-reference cases use the same completed public import path.
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
Chord-local `Spanner[type=Slur]`, including lowercase `spanner`, now shares the
same cross-measure MusicXML slur-number state as legacy `Slur` markers.
Dropped public MSCX chord/rest events now retain `mks:diag` context for unknown
duration or missing pitch when `debugMetadata` is enabled.
Unsupported public MSCX voice elements now produce one sorted
`unsupported-elements` `mks:diag` warning, while upstream-ignored layout and
signature tags remain silent.
MusicXML `trill-mark` without a wavy-line now round trips through the
chord-local MSCX `ornamentTrill` subtype rather than a Trill spanner.
Public MusicXML export now has direct regressions for staccato, accent, and
tenuto MSCX articulation subtypes.
An already-dirty `ScoreCore` now has direct result-shape evidence for a second
successful dispatch: `dirtyChanged=false` with its changed ID, affected
measure, and empty warning/diagnostic lists retained.
Overfull public MSCX voice lanes now omit the overflowing tail event and retain
the source-style `mks:diag` `clamped/overfull` payload with occupied and
capacity divisions.
Public MusicXML export has direct regressions for technical stopped/up-bow/
open-string/harmonic plus fingering/string values, and for multi-staff Part
clef scaffolding with an instrument shortName.
Public MusicXML tie/slur export now has a direct regression for MSCX
Tie/endSpanner note markers and start/stop Slur span fractions.
Public MSCX import now has a direct regression for Dynamic placement in its
source multi-voice MusicXML lane.
Unsupported public MSCX Tuplet definitions now retain a source-style
`skipped/unsupported` `mks:diag` entry with measure/staff/voice/tick context.
Unsupported visible Dynamic, Expression, Marker, and Jump events now retain
the same source-style `skipped/unsupported` diagnostic context.
For each batch:

1. Record the exact pinned upstream cases being followed.
2. Add or update focused Java tests before marking the mapped item complete.
3. Implement the smallest coherent behavior group.
4. Run focused tests, `mvn -q package`, and `git diff --check`.
5. Update `TODO.md`, this PLAN, the gap matrix, and affected mapping documents.
