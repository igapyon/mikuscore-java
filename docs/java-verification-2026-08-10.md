# Java verification — 2026-08-10

| Check | Result |
| --- | --- |
| `mvn -q -Dtest=CffpSeriesTest test` | passed |
| `mvn -q -Dtest=MeiIoTest,MidiIoTest,MuseScoreIoTest test` | passed: compact local-only semantic-equivalent evidence |
| `mvn -q package` | passed: 1,302 tests, 0 failures, 0 errors |
| `git diff --check` | passed |
| `java -jar target/miku-score.jar --help` | passed; included conversion and state command families advertised |
| `java -jar target/miku-score.jar --version` | passed: `0.6.1` |
| JAR `convert --from musicxml --to abc` on `abc-roundtrip/base.musicxml` | passed; emitted `C2 D2 E2 F2 |` |
| JAR `state summarize` on the same MusicXML | passed; emitted one-part/one-measure/voice-1 summary |

The package test suite includes the focused CLI conversion/state and
diagnostic contracts. The JAR smoke check confirms the packaged entry point;
Verovio rendering remains an intentionally unsupported/excluded runtime path.
