---
title: miku-score-java rename migration
description: Canonical-name migration record and retained old-name classification.
topics: [miku-score, migration, compatibility, release]
category: migration
status: implemented
created: 2026-08-09
updated: 2026-08-09
audience: [maintainer, developer, agent]
sources:
  - type: github-issue
    role: primary
    url: https://github.com/igapyon/miku-score-java/issues/31
    checked: 2026-08-09
---

# miku-score-java rename migration

GitHub repository rename and the tracked repository content now use the
canonical `miku-score` name. The rename was performed by a human; the local
`origin` remote points to `git@github.com:igapyon/miku-score-java.git`.

## Current canonical names

| Surface | Canonical value |
| --- | --- |
| GitHub repository | `igapyon/miku-score-java` |
| Maven artifactId | `miku-score` |
| Maven project name | `miku-score-java` |
| Runtime jar | `target/miku-score.jar` |
| Sources jar | `target/miku-score-sources.jar` |
| Distribution ZIP | `target/miku-score-dist.zip` |
| Future Release assets | `miku-score-<version>.jar`, `miku-score-sources-<version>.jar` |

## Old-name classification

| Classification | Retained old name | Reason |
| --- | --- | --- |
| Compatibility | Java package `jp.igapyon.mikuscore` and public class names such as `MikuscoreCli` | Changing Java package or public class names would break Java callers; this migration intentionally preserves them. |
| Historical reference | `docs/worklog/2026-05-legacy-straight-conversion-log.md`, past Releases, and past Issue/PR references | These records describe pre-rename work and published artifacts. They are not rewritten. |
| Historical local workspace | `workplace/mikuscore` when it already exists | `workplace/` is ignored local scratch space. Its directory name does not define the current repository or public Maven/CLI contract. |
| Compatibility identifier | MIDI `app=mikuscore` metadata | This is a retained wire-format identifier. It remains unchanged for compatibility with existing MIDI metadata consumers. |
| Unmigrated current public surface | None | Current repository URL, Maven metadata, CLI output, generated fallback titles, download filename stems, documented artifact names, and workflow input paths use `miku-score`. |

## 2026-08-09 follow-up

Upstream `miku-score` fallback titles and generated download filename stems were
checked against the Java implementation. Java now emits `miku-score` for these
user-facing values in the ABC, MEI, LilyPond, and MuseScore conversion paths
and in `CoreApi` download payloads. The MIDI `app=mikuscore` metadata key is
intentionally retained as the documented compatibility identifier.

## Follow-up boundary

Do not create a tag or GitHub Release as part of this migration. A human
reviews the next release's generated assets and publishes it through the
Release workflow after this change is merged.
