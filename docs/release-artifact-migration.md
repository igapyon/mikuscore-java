---
title: Release artifact naming migration
description: Canonical Maven and Release artifact naming for miku-score-java.
topics: [miku-score, release, maven, artifacts, compatibility]
category: workflow
status: implemented
audience: [maintainer, developer]
created: 2026-08-06
updated: 2026-08-09
sources:
  - type: local-file
    role: primary
    path: pom.xml
    checked: 2026-08-06
  - type: local-file
    role: supporting
    path: src/assembly/dist.xml
    checked: 2026-08-06
  - type: local-file
    role: supporting
    path: .github/workflows/release-cli-runtime.yml
    checked: 2026-08-06
---

# Release artifact naming migration

## Purpose

Issue #31 renamed this repository to `miku-score-java`. The Maven outputs,
CLI help, distribution ZIP, and future Release assets use the same
`miku-score` canonical name.

## Current contract

`mvn package` produces these local paths:

- `target/miku-score.jar`
- `target/miku-score-sources.jar`
- `target/miku-score-dist.zip`

The distribution ZIP contains `miku-score.jar`, `miku-score-sources.jar`,
`README.md`, and `LICENSE`.

The tag workflow stages and uploads these public assets:

- `miku-score-<version>.jar`
- `miku-score-sources-<version>.jar`

It does not publish the distribution ZIP. The tag/POM version validation and
Java 8 runtime smoke test remain unchanged.

## Compatibility boundary

Existing releases and their assets are historical records and are not
rewritten. The new names apply to future release assets after the migration.
No compatibility alias is created in the repository; release publication
continues to be a human-controlled GitHub operation.

## Verification

Run `mvn test`, `mvn package`, `java -jar target/miku-score.jar --help`, and
`java -jar target/miku-score.jar --version`. Confirm the three local artifacts
are non-empty and inspect the ZIP entries before preparing a release.
