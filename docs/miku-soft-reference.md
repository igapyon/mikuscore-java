---
title: mikuscore-java miku-soft reference
description: Project-local entry point to the shared miku-soft standards used for maintenance.
topics: [miku-soft, java, maintenance, straight-conversion]
category: reference
status: stable
audience: [maintainer, developer, agent]
created: 2026-08-06
updated: 2026-08-06
sources:
  - type: local-runtime
    role: primary
    label: installed igapyon-miku-soft-developer skill
    path: /Users/igapyon/.codex/skills/igapyon-miku-soft-developer
    checked: 2026-08-06
  - type: upstream-doc
    role: supporting
    label: igapyon-agent-skills miku-soft developer skill
    url: https://github.com/igapyon/igapyon-agent-skills/tree/devel/skills/igapyon-miku-soft-developer
    checked: 2026-08-06
---

# miku-soft reference

This repository follows the shared
[`igapyon-miku-soft-developer`](https://github.com/igapyon/igapyon-agent-skills/tree/devel/skills/igapyon-miku-soft-developer)
standards. The shared standards are not copied into this repository; this file
is the project-local entry point.

## Checked reference

- Checked date: 2026-08-06
- Main workflow: existing miku-soft project maintenance
- Applicable layer: Java application / straight conversion
- Installed source identity: the deployed skill directory had no Git metadata,
  so an exact skill commit was unavailable at the time of this check.

## Project-specific application

- Keep Java 8, Maven, JUnit Jupiter, a single executable runtime jar, and
  deterministic local verification as the Java runtime baseline.
- Keep product semantics in `CoreApi` and format/core packages; keep
  `MikuscoreCli` as a testable entrypoint adapter.
- Keep upstream class, test, CLI, and follow-up mappings under `docs/`.
- Keep `workplace/` for local upstream and sister-project references only;
  track only `workplace/.gitkeep`.
- Record deviations from upstream and Java-side extensions in the repository
  mapping or follow-up documents instead of in shared-policy copies.

## Related project documents

- [`remaining-migration-items.md`](remaining-migration-items.md)
- [`upstream-class-mapping.md`](upstream-class-mapping.md)
- [`upstream-test-mapping.md`](upstream-test-mapping.md)
- [`upstream-cli-mapping.md`](upstream-cli-mapping.md)
- [`upstream-followup-log.md`](upstream-followup-log.md)
