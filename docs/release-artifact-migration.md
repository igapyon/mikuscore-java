---
title: Release artifact naming migration decision
description: Compatibility gate and implementation design for aligning Maven and Release artifact names.
topics: [mikuscore, release, maven, artifacts, compatibility]
category: workflow
status: draft
audience: [maintainer, developer]
created: 2026-08-06
updated: 2026-08-06
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

# Release artifact naming migration decision

## Purpose

The current shared miku-soft Java guidance prefers Maven-coordinate-traceable
artifact names. Changing public Release asset names can break download scripts,
checksums, and user documentation, so it is intentionally not included in
ordinary repository cleanup.

## Current contract

`mvn package` currently produces these local paths:

- `target/mikuscore.jar`
- `target/mikuscore-sources.jar`
- `target/mikuscore-dist.zip`

The distribution ZIP contains `mikuscore.jar`, `mikuscore-sources.jar`,
`README.md`, and `LICENSE`.

The tag workflow renames and uploads only these public assets:

- `mikuscore-<version>.jar`
- `mikuscore-sources-<version>.jar`

It does not currently publish the distribution ZIP.

## Decision required

Before changing any filename, inspect the published Releases and known
consumers. Decide one of these contracts explicitly:

1. Retain the existing public names and record them as a compatibility
   exception. This is the default until consumer evidence is available.
2. Migrate consistently to Maven-coordinate-traceable names:
   `<artifactId>-<version>.jar`,
   `<artifactId>-<version>-sources.jar`, and
   `<artifactId>-dist-<version>.zip`.

The decision also needs to state whether the distribution ZIP becomes a public
Release asset and whether a compatibility alias or a release-note migration
period is required.

## Implementation after approval

1. Change `pom.xml` and `src/assembly/dist.xml` together so Maven outputs and
   ZIP contents use the selected final names.
2. Change `.github/workflows/release-cli-runtime.yml` to stage only the exact
   approved files. Keep the tag/POM version validation and Java 8 final-jar
   smoke test.
3. Update `README.md` and any download or checksum references in the same
   change.
4. Build from a clean staging directory; assert the exact basenames, non-empty
   files, executable jar behavior, ZIP contents, and any approved checksums.
5. Leave tagging, Release creation, asset upload, and publication to the
   human-controlled GitHub step.

## Current disposition

Deferred on 2026-08-06. No public artifact name or Release workflow behavior
was changed by the miku-soft maintenance alignment.
