# Remaining Migration Items

This document tracks repository-specific remaining work for the `mikuscore-java` straight conversion.

The shared policy is described in `docs/miku-soft-30-straight-conversion-v20260425.md`.
This file records the current `mikuscore-java` status and next migration items.

## Current Status

- Build foundation: initial Maven skeleton exists
- Java compatibility: configured as source / target `1.8`
- Test framework: JUnit Jupiter
- Primary verification command: `mvn test`
- Runtime packaging: single executable jar configured through Maven shade plugin
- Distribution zip: configured as an initial Maven assembly
- CLI: foundation entrypoint plus first `convert --from musicxml --to musicxml` slice, first `convert --from abc --to musicxml` slice, `state summarize`, `state inspect-measure`, `state validate-command`, `state apply-command`, and `state diff`
- Core conversion: partial basic command catalog and state inspection migration exists
- Format I/O: MusicXML / MXL / ZIP helper subset migrated; broader ABC / MuseScore / MIDI / MEI / LilyPond conversion remains pending
- ABC lexer: first low-level `abc-lexer.ts` helper slice migrated
- ABC parser: playable-event, structural token, body dispatcher, and grace group helper slices migrated
- ABC I/O: utility, meta directive, import line processor, body text entry, voice directive tail, header parsing, voice measure meta, MusicXML export XML, part measure render context, rendered measure misc XML, rendered part measure XML, part list/body XML integration, MusicXML export context, measure note XML core, note lyric/time-modification XML, note leading direction XML, measure beam XML, note notations decoration XML, body import voice stores helper, body lyric application, body field state update, body barline processing, non-playable body entry dispatch, simple body token dispatch, bracket / grace / fallback body dispatch, pending note state helper, ABC chord harmony XML helper, MusicXML to ABC harmony / lyric helper, MusicXML to ABC DOM utility helper, MusicXML to ABC lane definition helper, MusicXML to ABC meta line helper, MusicXML to ABC measure meta helper, MusicXML to ABC measure state helper, MusicXML to ABC direction token helper, MusicXML to ABC note lane / timing helper, MusicXML to ABC note ornament helper, MusicXML to ABC pitch token helper, MusicXML to ABC note ornament prefix helper, MusicXML to ABC note articulation prefix helper, MusicXML to ABC note technical prefix helper, first ABC body import to MusicXML integration, ABC body tuplet timing, ABC body grace group import, basic and standard-shorthand ABC body decoration pending state, ABC body repeat / ending metadata, ABC body tie handoff, and ABC body broken rhythm / slur handoff slices migrated
- Render output: not migrated; `render svg` is constrained by upstream `verovio.js` browser runtime dependency

## Current Scope

Initial Java conversion scope:

- MusicXML-centered core processing
- file-based CLI workflows
- upstream CLI command family preservation where practical
- deterministic local artifacts where practical
- upstream-aware JUnit tests

Out of scope for the initial Java conversion:

- browser UI
- DOM event handling
- browser download flow
- browser preview surfaces
- UI-only helpers that do not define product semantics
- VSQX conversion, because the upstream path depends on a bridge / dependency shape outside the initial Java straight-conversion target

## Immediate Items

- [ ] Fill `docs/upstream-class-mapping.md` with Java class groups as each migration slice lands
- [ ] Fill `docs/upstream-test-mapping.md` with JUnit test coverage as each upstream test intent is ported
- [ ] Fill `docs/upstream-cli-mapping.md` with option / stdout / stderr / exit-code correspondence
- [x] Decide first core-adjacent migration slice: `state summarize`
- [x] Add first MusicXML state summary implementation
- [x] Add first MusicXML measure inspection implementation for edit targeting
- [x] Add first MusicXML state diff implementation
- [x] Add first MusicXML command validation implementation for `change_to_pitch`
- [x] Add first MusicXML command apply implementation for `change_to_pitch`
- [x] Add simple `change_duration` validation/apply implementation
- [x] Add simple `insert_note_after` validation/apply implementation
- [x] Add simple `delete_note` validation/apply implementation
- [x] Add simple `split_note` validation/apply implementation
- [x] Add `ui_noop` validation/apply no-mutation behavior
- [x] Add `core/timeIndex.ts` overfull validation subset for `change_duration` and `insert_note_after`
- [x] Add structural boundary validation subset for `insert_note_after`, `delete_note`, and `split_note`
- [x] Add chord-head promotion behavior for `delete_note`
- [x] Decide next core migration slice from upstream `core/`
  - next core slice should address deeper `timeIndex` parity or a still-pending core helper such as accidental spelling / staff-clef policy
- [x] Decide next format I/O migration slice from upstream MusicXML / MXL code
  - first slice is `musicxml-io.ts` imported-text normalization subset
- [x] Add `musicxml-io.ts` parse / serialize / pretty-print and basic normalization subset
- [x] Add `musicxml-io.ts` tuplet notation enrichment subset
- [x] Add `musicxml-io.ts` explicit implicit beam pass subset
- [x] Add `mxl-io.ts` / `zip-io.ts` container extraction and encoding subset
- [x] Add first latest-upstream CLI taxonomy slice: `convert --from musicxml --to musicxml`
  - supports stdin / stdout MusicXML text
  - supports `.musicxml` / `.xml` text files
  - supports `.mxl` input decode and `.mxl` output encode
- [x] Add first ABC low-level lexer slice from `src/ts/abc-lexer.ts`
  - `lexAbcLengthToken`
  - `lexAbcAccidental`
  - `lexAbcNote`
- [x] Add first ABC parser playable-event slice from `src/ts/abc-parser.ts`
  - `parseAbcNoteAt`
  - `parseAbcChordAt`
  - `parseAbcPlayableEventAt`
- [x] Add ABC parser field / token / dispatcher slice from `src/ts/abc-parser.ts`
  - field / repeat / barline helpers
  - span / quoted string / decoration helpers
  - broken rhythm / shorthand / tie / slur helpers
  - paren / bracket / body-token / body-entry dispatchers
- [x] Add ABC parser grace group slice from `src/ts/abc-parser.ts`
  - `parseAbcGraceGroupAt`
  - malformed grace accidental warning behavior
- [x] Add first ABC I/O utility slice from `src/ts/abc-io.ts`
  - fraction helpers
  - ABC length token parse / format helpers
  - pitch / accidental / key / tempo unit helpers
- [x] Add second ABC I/O utility slice from `src/ts/abc-io.ts`
  - abcjs wrapper line detection
  - measure content duration estimate
  - ABC key fifths helper
- [x] Add ABC I/O meta directive slice from `src/ts/abc-io.ts`
  - `%@mks` params parsing
  - trill/key/measure/transpose meta application
  - structured directive detection
- [x] Add ABC I/O import line processor slice from `src/ts/abc-io.ts`
  - import line state / voice registry carrier classes
  - unsupported continued field handling
  - header field / voice directive handling
  - user-defined decoration parse / expansion helpers
  - `processAbcImportLine`
- [x] Add ABC I/O body text entry slice from `src/ts/abc-io.ts`
  - body entry carrier class
  - inline voice split
  - overlay split
  - overlay voice metadata propagation
- [x] Add ABC I/O voice directive tail slice from `src/ts/abc-io.ts`
  - quoted / bare V: attribute parsing
  - name / clef / transpose handling
  - unsupported key and skipped first-token handling
- [x] Add ABC I/O header parsing helper slice from `src/ts/abc-io.ts`
  - tempo parsing from `Q:`
  - meter parsing from `M:`
  - unit length fraction parsing from `L:`
  - key parsing from `K:`
  - warning / fallback behavior
- [x] Add ABC I/O voice measure meta helper slice from `src/ts/abc-io.ts`
  - key / meter / tempo hint collection
  - notation meta and hinted meta merge behavior
  - tempo clamp behavior
- [x] Add ABC I/O MusicXML export XML helper slice from `src/ts/abc-io.ts`
  - XML escaping
  - clef / grouped-staff clef XML
  - part transpose XML
  - tempo direction XML
  - measure header / repeat / measure wrapper XML
- [x] Add ABC I/O part measure render context slice from `src/ts/abc-io.ts`
  - initial part render state
  - key / meter / tempo hint state update
  - measure duration calculation
  - implicit pickup inference
- [x] Add ABC I/O rendered measure misc XML helper slice from `src/ts/abc-io.ts`
  - debug miscellaneous metadata XML
  - source miscellaneous metadata XML
  - diagnostic miscellaneous metadata XML
  - diagnostic voice filter behavior
- [x] Add ABC I/O rendered part measure XML helper slice from `src/ts/abc-io.ts` and `src/ts/abc-layout.ts`
  - grouped staff detection and per-staff note XML assembly
  - backup insertion before later staves
  - header / tempo / repeat / misc XML composition
  - measure-number and implicit pickup handoff
- [x] Add ABC I/O part list / part body XML integration slice from `src/ts/abc-io.ts`
  - score-part list XML
  - per-part render state loop
  - part body XML assembly through rendered measure helper
  - score-partwise document wrapper
- [x] Add ABC I/O MusicXML export context / parsed document integration slice from `src/ts/abc-io.ts`
  - resolved parts fallback
  - meta-derived measure count, meter, key, tempo, and duration context
  - parsed result to score-partwise document assembly
  - single-clef inference subset from `core/staffClefPolicy.ts`
- [x] Add ABC I/O measure note XML core slice from `src/ts/abc-io.ts`
  - empty measure rest XML
  - pitch / rest / duration / voice / staff / type XML
  - lyric XML
  - time-modification XML
  - leading direction annotations / navigation / wedge / dynamics XML subset
  - accidental XML
  - tie / tied XML subset
- [x] Add ABC I/O measure beam XML slice from `src/ts/abc-io.ts` and `src/ts/beam-common.ts`
  - implicit beam grouping
  - beat boundary split behavior
  - explicit `begin` / `mid` beam mode subset
- [x] Add ABC I/O note notations decoration XML slice from `src/ts/abc-io.ts`
  - ornament XML helper
  - articulation XML helper
  - technical XML helper
  - slur / tuplet / fermata / decoration integration in notations XML
- [x] Add ABC I/O body import voice stores helper slice from `src/ts/abc-io.ts`
  - voice measure store initialization
  - notation measure meta initialization
  - meter / tempo by-measure store initialization
  - active ending finalization
- [x] Add ABC I/O body field state update slice from `src/ts/abc-io.ts`
  - lyric tokenization and measure lyric application
  - key signature accidental map helper
  - K / L / M / Q inline body field handling
  - key / meter / tempo store update
  - measure accidental reset for inline key changes
- [x] Add ABC I/O body barline processing slice from `src/ts/abc-io.ts`
  - repeat start / repeat end handoff
  - active ending stop and bare repeat ending start handoff
  - measure advance, measure accidental clear, last-note clear, and beam reset handoff
- [x] Add ABC I/O non-playable body entry dispatch slice from `src/ts/abc-io.ts`
  - barline dispatch
  - standalone body field dispatch and unsupported warning
  - unsupported token / number warning and index handoff
- [x] Add ABC I/O simple body token dispatch slice from `src/ts/abc-io.ts`
  - broken-rhythm / decoration / paren / quoted-string dispatch
  - single-char-shorthand / slur-stop / tie dispatch
- [x] Add ABC I/O bracket / grace / fallback body dispatch slice from `src/ts/abc-io.ts`
  - inline-field / repeat-ending / playable bracket dispatch
  - grace group append and parse-failure warning
  - closing notation / unsupported punctuation / parse-error fallback dispatch
- [x] Add ABC I/O pending note state helper slice from `src/ts/abc-io.ts`
  - pending ornament / articulation / direction / technical state handoff
  - pending value / optional value / array helper behavior
  - tie-stop handoff and rest warning behavior
- [x] Add ABC I/O chord harmony XML helper slice from `src/ts/abc-io.ts`
  - quoted text / chord token normalization
  - chord symbol detection and harmony kind mapping
  - chord symbol to MusicXML harmony XML
  - note harmony XML integration
- [x] Add ABC I/O MusicXML to ABC harmony / lyric helper slice from `src/ts/abc-io.ts`
  - MusicXML harmony element to ABC chord symbol
  - MusicXML lyric text / syllabic to ABC lyric token
  - direct child DOM helper subset
- [x] Add ABC I/O MusicXML to ABC DOM utility helper slice from `src/ts/abc-io.ts`
  - MusicXML part clef inference helper
  - accidental text to alter helper
  - optional number parser
  - nested direct-child DOM path helper subset
- [x] Add first ABC body import and MusicXML export integration slice from `src/ts/abc-io.ts`
  - basic ABC headers, body line collection, notes, rests, chords, and barline-separated measures
  - `parseForMusicXml` / `musicXmlFromAbc`
  - first CLI bridge for `convert --from abc --to musicxml`
  - focused JUnit tests
- [x] Add ABC body tuplet timing slice from `src/ts/abc-io.ts`
  - paren body token `(n` active tuplet state
  - duration scale handoff to playable events
  - MusicXML `time-modification` and tuplet start / stop notation on event-start notes
  - focused JUnit tests
- [x] Add ABC body grace group import slice from `src/ts/abc-io.ts`
  - `{...}` grace group handoff in body import loop
  - grace note pitch / accidental / octave / length conversion
  - MusicXML grace slash emission
  - focused JUnit tests
- [x] Add basic ABC body decoration pending state slice from `src/ts/abc-io.ts`
  - `!trill!` / `!staccato!` / `!accent!` / `!fermata!`
  - single-char shorthand `.`, `T`, `L`, `H`
  - pending decoration handoff to the first playable note
  - focused JUnit tests
- [x] Add standard shorthand ABC body decoration state slice from `src/ts/abc-io.ts`
  - `~`, `M`, `O`, `P`, `S`, `u`, and `v`
  - arpeggiate / mordent / inverted-mordent / coda / segno / up-bow / down-bow XML handoff
  - focused JUnit tests
- [x] Add ABC body repeat / ending metadata slice from `src/ts/abc-io.ts`
  - repeat-start / repeat-end barline handoff
  - bracket / bare repeat ending start handoff
  - MusicXML repeat / ending barline XML integration
  - focused JUnit tests
- [x] Add ABC body tie handoff slice from `src/ts/abc-io.ts`
  - tie body token `-`
  - previous event tie-start and next event tie-stop handoff
  - MusicXML `<tie>` / `<tied>` XML integration
  - focused JUnit tests
- [x] Add ABC body broken rhythm / slur handoff slice from `src/ts/abc-io.ts`
  - broken rhythm body token `>` / `<`
  - previous / next playable event duration scale handoff
  - slur-start `(` and slur-stop `)` handoff
  - MusicXML `<slur>` XML integration
  - focused JUnit tests
- [ ] Continue ABC migration with broader `src/ts/abc-io.ts` body import parity
  - prefixed / richer decorations, overlays, overfull compatibility diagnostics, and golden fixtures
- [x] Decide whether SVG render can be implemented directly in Java or must be recorded as constrained by upstream runtime dependencies
  - current decision: keep `render svg` unsupported in the Java initial slice until a Java-compatible renderer runtime or explicit external-runtime strategy is chosen

## Verification Commands

Primary:

```sh
mvn test
```

Packaging:

```sh
mvn package
```

## Notes

- `workplace/mikuscore` is a local upstream reference clone and is not tracked in Git
- `workplace/mikuproject-java-devel` is used only as a sister Java application reference
