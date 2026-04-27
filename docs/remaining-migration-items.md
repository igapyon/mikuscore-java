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
- CLI: foundation entrypoint plus `state summarize`, `state inspect-measure`, `state validate-command`, `state apply-command`, and `state diff`
- Core conversion: partial basic command catalog and state inspection migration exists
- Format I/O: not yet migrated
- Render output: not yet migrated

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
- [ ] Decide whether SVG render can be implemented directly in Java or must be recorded as constrained by upstream runtime dependencies

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
