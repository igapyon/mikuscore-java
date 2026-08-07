---
title: mikuscore-java maintenance and migration TODO
description: Current repository-specific work, decisions, and verification for the Java runtime.
topics: [mikuscore, java, migration, maintenance]
category: worklog
status: active
audience: [maintainer, developer, agent]
updated: 2026-08-06
sources:
  - type: local-file
    role: primary
    path: docs/remaining-migration-items.md
    checked: 2026-08-06
  - type: local-file
    role: supporting
    path: docs/miku-soft-reference.md
    checked: 2026-08-06
---

# TODO

## Current role

This file is the concise repository worklog for `mikuscore-java`. Detailed
upstream migration status belongs in
[`docs/remaining-migration-items.md`](docs/remaining-migration-items.md), and
class, test, CLI, and parity evidence belongs in the corresponding mapping
documents under `docs/`.

The completed 2026-05 straight-conversion history was retained without
rewriting it in
[`docs/worklog/2026-05-legacy-straight-conversion-log.md`](docs/worklog/2026-05-legacy-straight-conversion-log.md).

## Current project direction

- Preserve `mikuscore` upstream semantics in Java; do not begin with a
  Java-first redesign.
- Keep Java 8, Maven, JUnit Jupiter, the executable runtime jar, and
  `workplace/` as the established runtime and repository boundaries.
- Treat MusicXML as the semantic anchor. Keep browser/Web UI behavior and
  VSQX outside the current Java conversion scope unless a new decision changes
  that boundary.
- Use `mvn test` as the primary verification command. Run focused JUnit tests
  for the affected Java classes and CLI contract.

## Open work

- [ ] Continue the repository-specific migration priorities in
  [`docs/remaining-migration-items.md`](docs/remaining-migration-items.md).
- [ ] Keep class, test, CLI, and follow-up mappings synchronized whenever a
  Java conversion slice changes.
- [ ] Resolve the remaining timing parity for `change_duration`,
  `insert_note_after`, and `split_note`; see
  [`docs/upstream-followup-log.md`](docs/upstream-followup-log.md).
- [ ] Keep `render svg` unsupported until a Java-compatible runtime strategy
  is explicitly selected.
- [ ] Record an installed miku-soft skill commit when the deployed skill source
  exposes Git metadata. The copy inspected on 2026-08-06 was not a Git
  worktree, so its exact commit could not be recorded.
- [ ] Resolve the public artifact-name decision in
  [`docs/release-artifact-migration.md`](docs/release-artifact-migration.md)
  before changing Maven or GitHub Release filenames.

## 2026-08-06 miku-soft maintenance record

| Field | Record |
| --- | --- |
| Profile | Java application / straight-conversion maintenance |
| Shared reference | [`docs/miku-soft-reference.md`](docs/miku-soft-reference.md), checked 2026-08-06 |
| Applied | Replace copied shared miku-soft policy documents with a project-local reference; synchronize documented Java CLI support and make its runtime contract explicit. |
| Deferred | Public artifact-name and Release-asset changes require the dedicated compatibility decision in [`docs/release-artifact-migration.md`](docs/release-artifact-migration.md). The shared maintenance backlog is still decision-gated. |
| Verification | `mvn package`; built runtime `--version` and `--help`; distribution ZIP contents; focused CLI contract tests. |
| Next action | Continue a bounded upstream-parity slice from `docs/remaining-migration-items.md`. |
