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

- [x] Java source / binary compatibility を `1.8` に固定する
- [x] build tool を Maven に固定する
- [x] test framework を JUnit Jupiter に固定する
- [x] primary verification command を `mvn test` に固定する
- [x] runtime packaging を single executable fat jar に固定する
- [x] distribution zip を作るか判断する
- [x] `workplace/` を repository root に作る
- [x] `workplace/.gitkeep` だけを Git 管理対象にする
- [x] upstream `https://github.com/igapyon/mikuscore` を `workplace/mikuscore` に clone する
- [ ] STOP: upstream `mikuscore` 側のリファクタリング完了を確認するまで、straight conversion を開始しない
- [x] 姉妹アプリ `workplace/mikuproject-java-devel` を参照可能にする

### 直近の限定作業

- 警告: straight conversion 実装に入ろうとしたら、まず upstream `mikuscore` のリファクタリング完了確認へ戻る
- [x] Maven project skeleton を作る
  - `pom.xml`
  - `src/main/java`
  - `src/test/java`
  - base package `jp.igapyon.mikuscore`
- [x] README に Java 版の目的、upstream 参照、straight conversion 方針、基本コマンドを書く
- [x] docs 配下に repository-specific mapping / follow-up 文書を作る
  - `docs/upstream-class-mapping.md`
  - `docs/upstream-test-mapping.md`
  - `docs/upstream-cli-mapping.md`
  - `docs/upstream-followup-log.md`
  - `docs/remaining-migration-items.md`
- [ ] upstream source / test / fixture / artifact の棚卸しを mapping 文書へ反映する
  - core / CLI / state-command 周辺は partial mapping 済み
  - format I/O / render / fixtures は未棚卸し領域が残る
- [x] 最初の Java core slice を MusicXML-oriented core から開始する
- [x] 最初の CLI slice は upstream の `convert` / `render` / `state` contract を崩さず、実装可能な最小範囲を決める

### 次回再開メモ

- 最終確認: `mvn package` 成功
  - 73 tests, failures 0, errors 0
  - `target/mikuscore.jar`
  - `target/mikuscore-dist.zip`
- 直近で入った主な Java slice:
  - `MusicXmlState`: state command basic catalog、overfull subset、structural boundary subset、chord-head delete promotion
  - `MusicXmlIo`: MusicXML parse / serialize / pretty-print、imported-text normalization、tuplet notation enrichment、explicit implicit beam pass
  - `MxlIo`: MXL extraction / encoding、ZIP extension text extraction、root entry listing、deterministic MXL bytes
- 直近で更新済みの追跡文書:
  - `README.md`
  - `docs/remaining-migration-items.md`
  - `docs/upstream-class-mapping.md`
  - `docs/upstream-test-mapping.md`
  - `docs/upstream-followup-log.md`
- 次に進めやすい候補:
  - 警告: upstream `mikuscore` の最新 TODO では、次のリファクタリング優先範囲は `src/ts/abc-io.ts`、次点で `src/ts/musescore-io.ts`、`src/ts/musicxml-io.ts` は watch only とされている
  - 警告: `convert` command 実装で ABC / MuseScore 変換に踏み込む場合は、upstream 側リファクタリング完了確認が先
  - 確認: 既移植済みの中心は `state` family / core basic command subset / MusicXML・MXL・ZIP subset であり、ABC / MuseScore I/O 本体は未移植
  - `convert` command の first cut を Java CLI に追加する
    - 最小候補: `--from musicxml --to musicxml` normalize / pass-through と `.mxl` input / output の橋渡し
    - 参照: `workplace/mikuscore/scripts/mikuscore-cli.mjs`
    - Java 側候補: `MikuscoreCli`, `MusicXmlIo`, `MxlIo`
  - format I/O を続ける場合は `src/ts/abc-lexer.ts` / `src/ts/abc-parser.ts` / `src/ts/abc-io.ts` の棚卸しから開始する
  - core を続ける場合は `core/timeIndex.ts` の underfull / rest consume-fill parity または `core/accidentalSpelling.ts` を小スライス化する
  - render を続ける場合は `render svg` の Java direct parity 可否を調べ、難しければ `docs/upstream-followup-log.md` に制約として閉じる
- 作業開始時に見る場所:
  - 方針: `docs/miku-soft-20-javaapp-design-v20260426.md`, `docs/miku-soft-30-straight-conversion-v20260425.md`
  - 進行: `docs/remaining-migration-items.md`
  - mapping: `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md`, `docs/upstream-cli-mapping.md`
  - 上流: `workplace/mikuscore`
- 注意:
  - `workplace/` 配下は Git 管理しない
  - Web UI は対象外
  - VSQX は初期 Java 移植対象外として固定済み
  - upstream に追随する straight conversion を優先し、Java-first redesign はまだ行わない

## Upstream 棚卸し

### source files

- [ ] `core/ScoreCore.ts`
  - basic command catalog は Java `MusicXmlState` に partial 移植済み
  - timing parity は follow-up に残る
- [ ] `core/commands.ts`
  - command node id / `ui_noop` behavior は partial 移植済み
- [ ] `core/interfaces.ts`
  - command validation result / diagnostics subset は partial 移植済み
- [ ] `core/validators.ts`
  - basic command payload / target validation、overfull、structural boundary subset は partial 移植済み
- [ ] `core/timeIndex.ts`
  - measure capacity / occupied-time subset は overfull validation 用に partial 移植済み
- [ ] `src/ts/beam-common.ts`
  - implicit beam assignment subset は `MusicXmlIo.applyImplicitBeamsToMusicXmlText` 用に partial 移植済み
- [ ] `core/accidentalSpelling.ts`
- [ ] `core/staffClefPolicy.ts`
- [ ] `core/xmlUtils.ts`
  - pitch / duration / insert / delete / chord-head promotion / split helper subset は partial 移植済み
- [ ] `src/ts/musicxml-io.ts`
  - state command 用 DOM parse / serialize subset は partial 移植済み
  - parse / serialize / pretty-print、part-list / part id / tuplet notation / final barline normalization、explicit implicit beam pass subset は `MusicXmlIo` に partial 移植済み
- [ ] `src/ts/abc-io.ts`
- [ ] `src/ts/abc-lexer.ts`
- [ ] `src/ts/abc-parser.ts`
- [ ] `src/ts/mxl-io.ts`
  - MXL container extraction / encoding subset は `MxlIo` に partial 移植済み
- [ ] `src/ts/zip-io.ts`
  - ZIP text extraction by extension / root entry listing / MXL zip encoding subset は `MxlIo` に partial 移植済み
- [ ] `src/ts/musescore-io.ts`
- [ ] `src/ts/midi-io.ts`
- [ ] `src/ts/mei-io.ts`
- [ ] `src/ts/lilypond-io.ts`
- [x] `src/ts/vsqx-io.ts`
  - 初期 Java 移植対象外として固定する
- [ ] `src/ts/cli-api.ts`
  - `state` family core API subset は partial 移植済み
- [ ] `scripts/mikuscore-cli.mjs`
  - Java CLI の `state` family subset は partial 移植済み
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
  - [x] `state summarize`
  - [x] `state inspect-measure`
  - [x] `state validate-command`
  - [x] `state apply-command`
  - [x] `state diff`
- [ ] supported input / output formats
  - MusicXML `.musicxml`, `.xml`, `.mxl`
  - MuseScore `.mscx`, `.mscz`
  - MIDI `.mid`, `.midi`
  - VSQX `.vsqx` は初期 Java 移植対象外
  - ABC `.abc`
  - MEI `.mei`
  - LilyPond `.ly`
- [ ] artifact roles
  - canonical score data: MusicXML
  - exchange inputs / outputs: MusicXML, MuseScore, MIDI, ABC, MEI, LilyPond
  - VSQX は upstream dependency / bridge constraint のため初期 Java 移植対象外
  - derived render output: SVG
  - diagnostics / summaries / state inspection JSON or text
  - command / patch-like bounded mutation payloads

## 実装順

### 1. Build foundation

- [x] `pom.xml` に Java 1.8, JUnit Jupiter, fat jar packaging を設定する
- [ ] Maven wrapper を置くか判断する
- [x] package base を `jp.igapyon.mikuscore` に固定する
- [x] CLI entrypoint は `System.exit` と処理本体を分けてテスト可能にする
- [ ] UTF-8, line ending, timezone, locale 依存の扱いを固定する

### 2. Core

- [x] MusicXML parsing / serialization の最小方針を決める
  - initial state command slice は hardened DOM parse / JAXP serialize を使う
- [x] `state summarize` 用の MusicXML DOM parse / summary 生成を追加する
- [ ] `ScoreCore` 相当の Java class group を作る
  - basic command catalog、overfull validation、structural boundary subset は `MusicXmlState` に partial 移植済み
- [ ] bounded command model を Java へ移す
  - JSON payload driven subset は移植済み
  - typed command class group は未作成
- [x] validation result / diagnostics model を Java へ移す
  - current subset: `MusicXmlCommandValidation`
- [x] measure / note inspection に必要な state view を Java へ移す
  - current subset: `MusicXmlMeasureInspection`
- [ ] upstream method names は Java convention に反しない範囲で camelCase を維持する

### 3. Format I/O

- [ ] MusicXML text I/O
  - state command 用 text input / output subset は実装済み
  - imported-text normalization subset は `MusicXmlIo` に partial 移植済み
  - explicit implicit beam pass subset は `MusicXmlIo` に partial 移植済み
- [x] MXL zip container I/O
  - `META-INF/container.xml` root-file extraction、fallback `.musicxml` / `.xml` extraction、`score.musicxml` MXL encoding subset は実装済み
- [ ] ABC parse / export
- [ ] MuseScore MSCX text I/O
- [ ] MSCZ zip container I/O
- [ ] MIDI import / export
- [ ] MEI import / export
- [ ] LilyPond import / export
- [x] VSQX は upstream dependency / browser bridge 由来の制約により、初期 Java 移植対象外に固定する

### 4. Render / derived outputs

- [ ] SVG render を Java 版でどう扱うか決める
  - upstream が Verovio / browser-side runtime に依存する範囲を確認する
  - Java 版で direct parity が難しい場合、制約と代替 contract を follow-up に記録する
- [ ] render output の deterministic comparison 方針を決める

### 5. CLI

- [ ] upstream CLI help / option / exit code / stdout / stderr を棚卸しする
- [x] Java CLI の first cut 範囲を決める
- [ ] `convert` command を実装する
- [ ] `render svg` command を実装または制約付きで記録する
- [ ] `state` family を実装する
  - [x] `state summarize`
  - [x] `state inspect-measure`
  - [x] `state validate-command`
  - [x] `state apply-command`
  - [x] `state diff`
- [x] text payload は stdout、diagnostics は stderr の分離を保つ
  - current CLI slice: success payload stdout, command failure messages stderr
- [ ] JSON diagnostics mode の有無と形式を upstream と照合する

### 6. Packaging

- [x] executable fat jar を作る
- [x] jar manifest / main class を固定する
- [x] distribution zip が必要なら assembly を追加する
- [x] runtime artifact の smoke command を README に書く

## テスト

- [x] `mvn test` が空で通る foundation を作る
- [ ] upstream fixture を Java test resources へどう持つか決める
- [ ] upstream test intent -> Java test mapping を作る
- [x] core command / validation / state inspection の JUnit tests を追加する
- [x] `state summarize` の JUnit tests を追加する
- [ ] format I/O の roundtrip / golden tests を追加する
- [x] CLI tests は exit code / stdout / stderr / output file を分けて確認する
- [ ] Node upstream と Java output の byte-level parity が必要な artifact を分類する
- [x] parity が難しい箇所は差分理由を follow-up log に記録する

## ドキュメント

- [x] `README.md` を作成する
- [x] Java 版の対象範囲と out-of-scope を README に書く
- [x] upstream 参照方法を README に書く
- [x] build / test / run command を README に書く
- [x] upstream class mapping を作成する
- [x] upstream test mapping を作成する
- [x] upstream CLI mapping を作成する
- [x] remaining migration items を作成する
- [x] upstream follow-up log を作成する

## 完了条件

- [x] `mvn test` が primary verification command として成立する
- [x] Java 1.8 source / binary compatibility が維持されている
- [x] executable fat jar で CLI を実行できる
- [x] upstream file -> Java class group の対応を辿れる
- [x] upstream test intent -> Java test の対応を辿れる
- [x] upstream CLI -> Java CLI の対応を辿れる
- [ ] Java 側独自拡張がある場合、upstream-derived behavior と分けて記録されている
