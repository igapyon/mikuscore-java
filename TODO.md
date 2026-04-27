# TODO

## 使い分け

- この `TODO.md` は `mikuscore-java` の straight conversion 進行管理を扱う
- `docs/miku-soft-*.md` は shared design / policy の READONLY 参照文書として扱い、このファイルでは repository-specific な判断、作業順、完了条件を管理する
- upstream Node.js / TypeScript 版は `workplace/mikuscore` を一時参照として使う
- 姉妹 Java アプリの実装例は `workplace/mikuproject-java-devel` を参照する
- `workplace/` 配下は作業用であり、Git 管理対象は `workplace/.gitkeep` のみにする

## 最重要

### 現在フェーズの運用方針

- 新規機能追加ではなく、Node.js / TypeScript upstream の既存 semantics を Java へ追跡可能に移す
- Java-first な再設計を先に行わず、upstream の file boundary / vocabulary / CLI / tests / artifacts を辿れる形を優先する
- Web UI は Java 版の対象外とし、CLI runtime / core API / local artifact generation を中心にする
- MusicXML を semantic anchor として扱い、変換 loss / diagnostics / unsupported area を見える形で保持する
- upstream に bug や制約が見つかった場合、まず follow-up item として記録し、straight conversion 自体は upstream behavior を基準に進める

### 固定前提

- [ ] Java source / binary compatibility を `1.8` に固定する
- [ ] build tool を Maven に固定する
- [ ] test framework を JUnit Jupiter に固定する
- [ ] primary verification command を `mvn test` に固定する
- [ ] runtime packaging を single executable fat jar に固定する
- [ ] distribution zip を作るか判断する
- [x] `workplace/` を repository root に作る
- [x] `workplace/.gitkeep` だけを Git 管理対象にする
- [x] upstream `https://github.com/igapyon/mikuscore` を `workplace/mikuscore` に clone する
- [x] 姉妹アプリ `workplace/mikuproject-java-devel` を参照可能にする

### 直近の限定作業

- [ ] Maven project skeleton を作る
  - `pom.xml`
  - `src/main/java`
  - `src/test/java`
  - base package `jp.igapyon.mikuscore`
- [ ] README に Java 版の目的、upstream 参照、straight conversion 方針、基本コマンドを書く
- [ ] docs 配下に repository-specific mapping / follow-up 文書を作る
  - `docs/upstream-class-mapping.md`
  - `docs/upstream-test-mapping.md`
  - `docs/upstream-cli-mapping.md`
  - `docs/upstream-followup-log.md`
  - `docs/remaining-migration-items.md`
- [ ] upstream source / test / fixture / artifact の棚卸しを mapping 文書へ反映する
- [ ] 最初の Java core slice を MusicXML-oriented core から開始する
- [ ] 最初の CLI slice は upstream の `convert` / `render` / `state` contract を崩さず、実装可能な最小範囲を決める

## Upstream 棚卸し

### source files

- [ ] `core/ScoreCore.ts`
- [ ] `core/commands.ts`
- [ ] `core/interfaces.ts`
- [ ] `core/validators.ts`
- [ ] `core/timeIndex.ts`
- [ ] `core/accidentalSpelling.ts`
- [ ] `core/staffClefPolicy.ts`
- [ ] `core/xmlUtils.ts`
- [ ] `src/ts/musicxml-io.ts`
- [ ] `src/ts/abc-io.ts`
- [ ] `src/ts/abc-lexer.ts`
- [ ] `src/ts/abc-parser.ts`
- [ ] `src/ts/mxl-io.ts`
- [ ] `src/ts/musescore-io.ts`
- [ ] `src/ts/midi-io.ts`
- [ ] `src/ts/mei-io.ts`
- [ ] `src/ts/lilypond-io.ts`
- [ ] `src/ts/vsqx-io.ts`
- [ ] `src/ts/cli-api.ts`
- [ ] `scripts/mikuscore-cli.mjs`
- [ ] Web UI-only files are out of Java conversion scope, but their core-facing behavior should be checked where they reveal product semantics

### test files and fixtures

- [ ] `tests/unit/core.spec.ts`
- [ ] `tests/unit/musicxml-io.spec.ts`
- [ ] `tests/unit/abc-io.spec.ts`
- [ ] `tests/unit/abc-parser.spec.ts`
- [ ] `tests/unit/abc-roundtrip-golden.spec.ts`
- [ ] `tests/unit/cli-api.spec.ts`
- [ ] `tests/unit/mikuscore-cli.spec.ts`
- [ ] `tests/unit/musescore-io.spec.ts`
- [ ] `tests/unit/midi-io.spec.ts`
- [ ] `tests/unit/mei-io.spec.ts`
- [ ] `tests/unit/lilypond-io.spec.ts`
- [ ] `tests/property/core.property.spec.ts`
- [ ] `tests/fixtures/*.musicxml`
- [ ] `src/samples/**`
- [ ] local / spot tests that depend on external tools or large references should be classified separately from primary `mvn test`

### CLI and artifacts

- [ ] upstream CLI command tree
  - `convert --from ... --to ...`
  - `render svg`
  - `state summarize`
  - `state inspect-measure`
  - `state validate-command`
  - `state apply-command`
  - `state diff`
- [ ] supported input / output formats
  - MusicXML `.musicxml`, `.xml`, `.mxl`
  - MuseScore `.mscx`, `.mscz`
  - MIDI `.mid`, `.midi`
  - VSQX `.vsqx`
  - ABC `.abc`
  - MEI `.mei`
  - LilyPond `.ly`
- [ ] artifact roles
  - canonical score data: MusicXML
  - exchange inputs / outputs: MusicXML, MuseScore, MIDI, ABC, MEI, LilyPond, VSQX
  - derived render output: SVG
  - diagnostics / summaries / state inspection JSON or text
  - command / patch-like bounded mutation payloads

## 実装順

### 1. Build foundation

- [ ] `pom.xml` に Java 1.8, JUnit Jupiter, fat jar packaging を設定する
- [ ] Maven wrapper を置くか判断する
- [ ] package base を `jp.igapyon.mikuscore` に固定する
- [ ] CLI entrypoint は `System.exit` と処理本体を分けてテスト可能にする
- [ ] UTF-8, line ending, timezone, locale 依存の扱いを固定する

### 2. Core

- [ ] MusicXML parsing / serialization の最小方針を決める
- [ ] `ScoreCore` 相当の Java class group を作る
- [ ] bounded command model を Java へ移す
- [ ] validation result / diagnostics model を Java へ移す
- [ ] measure / note inspection に必要な state view を Java へ移す
- [ ] upstream method names は Java convention に反しない範囲で camelCase を維持する

### 3. Format I/O

- [ ] MusicXML text I/O
- [ ] MXL zip container I/O
- [ ] ABC parse / export
- [ ] MuseScore MSCX text I/O
- [ ] MSCZ zip container I/O
- [ ] MIDI import / export
- [ ] MEI import / export
- [ ] LilyPond import / export
- [ ] VSQX は upstream dependency / browser bridge 由来の制約を確認して、Java 側対象範囲を決める

### 4. Render / derived outputs

- [ ] SVG render を Java 版でどう扱うか決める
  - upstream が Verovio / browser-side runtime に依存する範囲を確認する
  - Java 版で direct parity が難しい場合、制約と代替 contract を follow-up に記録する
- [ ] render output の deterministic comparison 方針を決める

### 5. CLI

- [ ] upstream CLI help / option / exit code / stdout / stderr を棚卸しする
- [ ] Java CLI の first cut 範囲を決める
- [ ] `convert` command を実装する
- [ ] `render svg` command を実装または制約付きで記録する
- [ ] `state` family を実装する
- [ ] text payload は stdout、diagnostics は stderr の分離を保つ
- [ ] JSON diagnostics mode の有無と形式を upstream と照合する

### 6. Packaging

- [ ] executable fat jar を作る
- [ ] jar manifest / main class を固定する
- [ ] distribution zip が必要なら assembly を追加する
- [ ] runtime artifact の smoke command を README に書く

## テスト

- [ ] `mvn test` が空で通る foundation を作る
- [ ] upstream fixture を Java test resources へどう持つか決める
- [ ] upstream test intent -> Java test mapping を作る
- [ ] core command / validation / state inspection の JUnit tests を追加する
- [ ] format I/O の roundtrip / golden tests を追加する
- [ ] CLI tests は exit code / stdout / stderr / output file を分けて確認する
- [ ] Node upstream と Java output の byte-level parity が必要な artifact を分類する
- [ ] parity が難しい箇所は差分理由を follow-up log に記録する

## ドキュメント

- [ ] `README.md` を作成する
- [ ] Java 版の対象範囲と out-of-scope を README に書く
- [ ] upstream 参照方法を README に書く
- [ ] build / test / run command を README に書く
- [ ] upstream class mapping を作成する
- [ ] upstream test mapping を作成する
- [ ] upstream CLI mapping を作成する
- [ ] remaining migration items を作成する
- [ ] upstream follow-up log を作成する

## 完了条件

- [ ] `mvn test` が primary verification command として成立する
- [ ] Java 1.8 source / binary compatibility が維持されている
- [ ] executable fat jar で CLI を実行できる
- [ ] upstream file -> Java class group の対応を辿れる
- [ ] upstream test intent -> Java test の対応を辿れる
- [ ] upstream CLI -> Java CLI の対応を辿れる
- [ ] Java 側独自拡張がある場合、upstream-derived behavior と分けて記録されている

