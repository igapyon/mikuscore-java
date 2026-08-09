package jp.igapyon.mikuscore.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuscore.coreapi.CoreApi;
import jp.igapyon.mikuscore.musicxml.MusicXmlStateTest;
import jp.igapyon.mikuscore.musicxml.MxlIo;

public class MikuscoreCliTest {
    @Test
    public void helpReturnsZeroAndMentionsPlannedCommands() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "--help" },
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        String err = errBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("miku-score-java"));
        assertTrue(out.contains("target/miku-score.jar"));
        assertTrue(out.contains("convert --from musicxml --to musicxml"));
        assertTrue(out.contains("state summarize"));
        assertTrue(out.contains("Commands:"));
        assertTrue(out.contains("--diagnostics text|json is not yet implemented"));
        assertEquals("", err);
    }

    @Test
    public void convertHelpReturnsZeroAndMentionsMusicXmlPair() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--help" },
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("Convert score text between supported formats"));
        assertTrue(out.contains("--from musicxml --to musicxml"));
        assertTrue(out.contains("--out <file> replaces an existing file"));
        assertTrue(out.contains("0 success, 1 processing failure, 2 invalid usage or unsupported pair"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertRejectsUnsupportedDiagnosticsOption() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musicxml", "--to", "abc", "--diagnostics", "json" },
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(2, exitCode);
        assertEquals("", outBytes.toString("UTF-8"));
        assertTrue(errBytes.toString("UTF-8").contains("Unsupported option: --diagnostics"));
    }

    @Test
    public void unsupportedCommandReturnsUsageError() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "unknown" },
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        String err = errBytes.toString("UTF-8");
        assertEquals(2, exitCode);
        assertEquals("", out);
        assertTrue(err.contains("Unsupported command: unknown"));
    }

    @Test
    public void renderHelpReturnsZeroAndMentionsSvgConstraint() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "render", "--help" },
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("render svg"));
        assertTrue(out.contains("verovio.js/browser runtime"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void renderSvgReturnsUnsupportedRuntimeConstraint() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "render", "svg", "--from", "musicxml" },
                new ByteArrayInputStream(MusicXmlStateTest.sampleMusicXml("CLI render").getBytes(StandardCharsets.UTF_8)),
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(2, exitCode);
        assertEquals("", outBytes.toString("UTF-8"));
        assertTrue(errBytes.toString("UTF-8").contains("SVG render is unsupported"));
    }

    @Test
    public void renderSvgRejectsUnsupportedSource() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "render", "svg", "--from", "midi" },
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(2, exitCode);
        assertEquals("", outBytes.toString("UTF-8"));
        assertTrue(errBytes.toString("UTF-8").contains("Unsupported render source: --from midi"));
    }

    @Test
    public void convertMusicXmlToMusicXmlReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        String xml = MusicXmlStateTest.sampleMusicXml("CLI convert stdin");
        ByteArrayInputStream inBytes = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musicxml", "--to", "musicxml" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(0, exitCode);
        assertEquals(xml, outBytes.toString("UTF-8"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertMusicXmlToMusicXmlReadsTextFileAndWritesTextFile() throws Exception {
        Path input = Files.createTempFile("mikuscore-convert-in", ".musicxml");
        Path output = Files.createTempFile("mikuscore-convert-out", ".musicxml");
        try {
            String xml = MusicXmlStateTest.sampleMusicXml("CLI convert file");
            Files.write(input, xml.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

            int exitCode = MikuscoreCli.run(
                    new String[] { "convert", "--from", "musicxml", "--to", "musicxml", "--in", input.toString(),
                            "--out", output.toString() },
                    new ByteArrayInputStream(new byte[0]),
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            assertEquals(0, exitCode);
            assertEquals("", outBytes.toString("UTF-8"));
            assertEquals(xml, new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

    @Test
    public void convertMusicXmlToMusicXmlReadsMxlInputFile() throws Exception {
        Path input = Files.createTempFile("mikuscore-convert-in", ".mxl");
        try {
            String xml = MusicXmlStateTest.sampleMusicXml("CLI convert mxl in");
            Files.write(input, MxlIo.makeMxlBytes(xml));
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

            int exitCode = MikuscoreCli.run(
                    new String[] { "convert", "--from", "musicxml", "--to", "musicxml", "--in", input.toString() },
                    new ByteArrayInputStream(new byte[0]),
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            assertEquals(0, exitCode);
            assertEquals(xml, outBytes.toString("UTF-8"));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void convertMusicXmlToMusicXmlWritesMxlOutputFile() throws Exception {
        Path output = Files.createTempFile("mikuscore-convert-out", ".mxl");
        try {
            String xml = MusicXmlStateTest.sampleMusicXml("CLI convert mxl out");
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            int exitCode = MikuscoreCli.run(
                    new String[] { "convert", "--from", "musicxml", "--to", "musicxml", "--out", output.toString() },
                    inBytes,
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            assertEquals(0, exitCode);
            assertEquals("", outBytes.toString("UTF-8"));
            assertEquals(xml, MxlIo.extractMusicXmlTextFromMxl(Files.readAllBytes(output)));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    public void convertAbcToMusicXmlReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                "X:1\nT:CLI ABC\nM:4/4\nL:1/4\nK:C\nC D E F|]\n".getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "abc", "--to", "musicxml" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(0, exitCode);
        assertTrue(outBytes.toString("UTF-8").contains("<work-title>CLI ABC</work-title>"));
        assertTrue(outBytes.toString("UTF-8").contains("<step>C</step>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertAbcToMidiWritesMidiFile() throws Exception {
        Path output = Files.createTempFile("mikuscore-convert-abc-midi-out", ".mid");
        try {
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(
                    "X:1\nT:CLI ABC to MIDI\nM:4/4\nL:1/4\nK:C\nC D E F|]\n".getBytes(StandardCharsets.UTF_8));

            int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "abc", "--to", "midi",
                    "--out", output.toString() }, inBytes,
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            byte[] midi = Files.readAllBytes(output);
            assertEquals(0, exitCode);
            assertEquals("", outBytes.toString("UTF-8"));
            assertEquals('M', midi[0]);
            assertEquals('T', midi[1]);
            assertEquals('h', midi[2]);
            assertEquals('d', midi[3]);
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    public void convertMeiToMusicXmlReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        String mei = "<mei><music><body><mdiv><score><title>CLI MEI</title>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffDef n=\"1\" label=\"Voice\"/></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";
        ByteArrayInputStream inBytes = new ByteArrayInputStream(mei.getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "mei", "--to", "musicxml" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(0, exitCode);
        assertTrue(outBytes.toString("UTF-8").contains("<work-title>CLI MEI</work-title>"));
        assertTrue(outBytes.toString("UTF-8").contains("<part-name>Voice</part-name>"));
        assertTrue(outBytes.toString("UTF-8").contains("<step>F</step>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertLilyPondToMusicXmlReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        String lily = "\\version \"2.24.0\"\n"
                + "\\header { title = \"CLI LilyPond\" }\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score { \\new Staff = \"P1\" { c'4 d'4 e'4 f'4 } }";
        ByteArrayInputStream inBytes = new ByteArrayInputStream(lily.getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "lilypond", "--to", "musicxml" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(0, exitCode);
        assertTrue(outBytes.toString("UTF-8").contains("<work-title>CLI LilyPond</work-title>"));
        assertTrue(outBytes.toString("UTF-8").contains("<step>C</step>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertMusicXmlToAbcReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXml("CLI MusicXML to ABC").getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musicxml", "--to", "abc" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("X:1"));
        assertTrue(out.contains("T:CLI MusicXML to ABC"));
        assertTrue(out.contains("K:C"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertMusicXmlToAbcReadsMxlFileAndWritesAbcFile() throws Exception {
        Path input = Files.createTempFile("mikuscore-convert-abc-in", ".mxl");
        Path output = Files.createTempFile("mikuscore-convert-abc-out", ".abc");
        try {
            Files.write(input, MxlIo.makeMxlBytes(MusicXmlStateTest.sampleMusicXml("CLI MXL to ABC")));
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

            int exitCode = MikuscoreCli.run(
                    new String[] { "convert", "--from", "musicxml", "--to", "abc", "--in", input.toString(),
                            "--out", output.toString() },
                    new ByteArrayInputStream(new byte[0]),
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            String abc = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
            assertEquals(0, exitCode);
            assertEquals("", outBytes.toString("UTF-8"));
            assertTrue(abc.contains("T:CLI MXL to ABC"));
            assertTrue(abc.contains("K:C"));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

    @Test
    public void convertMusicXmlToMeiReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXml("CLI MusicXML to MEI").getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musicxml", "--to", "mei" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("<mei"));
        assertTrue(out.contains("<title>CLI MusicXML to MEI</title>"));
        assertTrue(out.contains("<scoreDef"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertMusicXmlToLilyPondReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleFullMeasureMusicXml("CLI MusicXML to LilyPond")
                        .getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musicxml", "--to", "lilypond" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\\version"));
        assertTrue(out.contains("\\score"));
        assertTrue(out.contains("title = \"CLI MusicXML to LilyPond\""));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertMusicXmlToMidiWritesMidiFile() throws Exception {
        Path output = Files.createTempFile("mikuscore-convert-midi-out", ".mid");
        try {
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(
                    MusicXmlStateTest.sampleMusicXml("CLI MusicXML to MIDI").getBytes(StandardCharsets.UTF_8));

            int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musicxml", "--to", "midi",
                    "--out", output.toString() }, inBytes,
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            byte[] midi = Files.readAllBytes(output);
            assertEquals(0, exitCode);
            assertEquals("", outBytes.toString("UTF-8"));
            assertEquals('M', midi[0]);
            assertEquals('T', midi[1]);
            assertEquals('h', midi[2]);
            assertEquals('d', midi[3]);
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    public void convertMidiToMusicXmlReadsMidiFileAndWritesStdout() throws Exception {
        Path input = Files.createTempFile("mikuscore-convert-midi-in", ".mid");
        try {
            CoreApi.CliResult midi = CoreApi.exportMusicXmlToMidi(
                    MusicXmlStateTest.sampleMusicXml("CLI MIDI to MusicXML"));
            Files.write(input, midi.getOutputBytes());
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

            int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "midi", "--to", "musicxml",
                    "--in", input.toString() }, new ByteArrayInputStream(new byte[0]),
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            String out = outBytes.toString("UTF-8");
            assertEquals(0, exitCode);
            assertTrue(out.contains("<score-partwise"));
            assertTrue(out.contains("<pitch>"));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void convertMuseScoreToMusicXmlReadsStdinAndWritesStdout() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        String mscx = "<museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">CLI MuseScore</metaTag>"
                + "<Division>480</Division><Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";
        ByteArrayInputStream inBytes = new ByteArrayInputStream(mscx.getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musescore", "--to", "musicxml" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("<score-partwise"));
        assertTrue(out.contains("<work-title>CLI MuseScore</work-title>"));
        assertTrue(out.contains("<step>C</step>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void convertMusicXmlToMuseScoreWritesMsczOutputFile() throws Exception {
        Path output = Files.createTempFile("mikuscore-convert-musescore-out", ".mscz");
        try {
            Files.write(output, "previous output".getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(
                    MusicXmlStateTest.sampleMusicXml("CLI MusicXML to MuseScore").getBytes(StandardCharsets.UTF_8));

            int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musicxml", "--to", "musescore",
                    "--out", output.toString() }, inBytes,
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            String mscx = MxlIo.extractTextFromZipByExtensions(Files.readAllBytes(output), new String[] { ".mscx" });
            assertEquals(0, exitCode);
            assertEquals("", outBytes.toString("UTF-8"));
            assertTrue(mscx.contains("<museScore version=\"4.0\">"));
            assertTrue(mscx.contains("<metaTag name=\"workTitle\">CLI MusicXML to MuseScore</metaTag>"));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    public void convertMuseScoreMsczToMusicXmlReadsFileAndWritesStdout() throws Exception {
        Path input = Files.createTempFile("mikuscore-convert-musescore-in", ".mscz");
        try {
            String mscx = "<museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">CLI MSCZ</metaTag>"
                    + "<Division>480</Division><Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                    + "<Note><pitch>64</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";
            Files.write(input, MxlIo.makeMsczBytes(mscx));
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

            int exitCode = MikuscoreCli.run(new String[] { "convert", "--from", "musescore", "--to", "musicxml",
                    "--in", input.toString() }, new ByteArrayInputStream(new byte[0]),
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            String out = outBytes.toString("UTF-8");
            assertEquals(0, exitCode);
            assertTrue(out.contains("<work-title>CLI MSCZ</work-title>"));
            assertTrue(out.contains("<step>E</step>"));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void stateSummarizeReadsStdinAndWritesJson() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXml("CLI summary").getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "state", "summarize" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        String err = errBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_state_summary\""));
        assertTrue(out.contains("\"title\": \"CLI summary\""));
        assertTrue(out.contains("\"part_count\": 1"));
        assertEquals("", err);
    }

    @Test
    public void stateInspectMeasureReadsStdinAndWritesJson() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXml("CLI inspect").getBytes(StandardCharsets.UTF_8));

        int exitCode = MikuscoreCli.run(new String[] { "state", "inspect-measure", "--measure", "1" },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        String err = errBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_measure_inspection\""));
        assertTrue(out.contains("\"measure_number\": \"1\""));
        assertTrue(out.contains("\"node_id\": \"n1\""));
        assertTrue(out.contains("\"part_id\": \"P1\""));
        assertEquals("", err);
    }

    @Test
    public void stateInspectMeasureRequiresMeasureOption() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exitCode = MikuscoreCli.run(new String[] { "state", "inspect-measure" },
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(2, exitCode);
        assertEquals("", outBytes.toString("UTF-8"));
        assertTrue(errBytes.toString("UTF-8").contains("Missing required option: --measure"));
    }

    @Test
    public void stateDiffReadsFilesAndWritesJson() throws Exception {
        Path before = Files.createTempFile("mikuscore-before", ".musicxml");
        Path after = Files.createTempFile("mikuscore-after", ".musicxml");
        try {
            Files.write(before, MusicXmlStateTest.sampleMusicXml("Before title").getBytes(StandardCharsets.UTF_8));
            Files.write(after, MusicXmlStateTest.sampleMusicXml("After title").replace("<step>C</step>", "<step>G</step>")
                    .getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

            int exitCode = MikuscoreCli.run(
                    new String[] { "state", "diff", "--before", before.toString(), "--after", after.toString() },
                    new ByteArrayInputStream(new byte[0]),
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            String out = outBytes.toString("UTF-8");
            assertEquals(0, exitCode);
            assertTrue(out.contains("\"kind\": \"musicxml_state_diff\""));
            assertTrue(out.contains("\"changed\": true"));
            assertTrue(out.contains("\"changed_fields\": [\"title\"]"));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(before);
            Files.deleteIfExists(after);
        }
    }

    @Test
    public void stateValidateCommandReadsStdinAndWritesJson() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXml("CLI validate").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"change_to_pitch\",\"selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"},\"pitch\":{\"step\":\"G\",\"octave\":4}}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "validate-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(out.contains("\"ok\": true"));
        assertTrue(out.contains("\"changed_node_ids\": [\"n1\"]"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateApplyCommandReadsStdinAndWritesMusicXml() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXml("CLI apply").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"change_to_pitch\",\"selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"},\"pitch\":{\"step\":\"A\",\"octave\":4}}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "apply-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("<step>A</step>"));
        assertTrue(out.contains("<octave>4</octave>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateApplyCommandWritesOutFile() throws Exception {
        Path output = Files.createTempFile("mikuscore-apply", ".musicxml");
        try {
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(
                    MusicXmlStateTest.sampleMusicXml("CLI apply out").getBytes(StandardCharsets.UTF_8));
            String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"B\",\"octave\":4}}";

            int exitCode = MikuscoreCli.run(
                    new String[] { "state", "apply-command", "--command", command, "--out", output.toString() },
                    inBytes,
                    new PrintStream(outBytes, true, "UTF-8"),
                    new PrintStream(errBytes, true, "UTF-8"));

            String fileText = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
            assertEquals(0, exitCode);
            assertEquals("", outBytes.toString("UTF-8"));
            assertTrue(fileText.contains("<step>B</step>"));
            assertTrue(fileText.contains("<octave>4</octave>"));
            assertEquals("", errBytes.toString("UTF-8"));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    public void stateValidateCommandAcceptsChangeDuration() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI validate duration").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"change_duration\",\"selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"},\"duration\":2}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "validate-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(out.contains("\"ok\": true"));
        assertTrue(out.contains("\"changed_node_ids\": [\"n1\"]"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateApplyCommandAcceptsChangeDuration() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI apply duration").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":2}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "apply-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("<duration>2</duration>"));
        assertTrue(out.contains("<type>half</type>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateValidateCommandAcceptsDeleteNote() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI validate delete").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"delete_note\",\"selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"}}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "validate-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(out.contains("\"ok\": true"));
        assertTrue(out.contains("\"changed_node_ids\": [\"n1\"]"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateApplyCommandAcceptsDeleteNote() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI apply delete").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "apply-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("<rest/>") || out.contains("<rest></rest>"));
        assertTrue(out.contains("<duration>1</duration>"));
        assertTrue(!out.contains("<step>C</step>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateValidateCommandAcceptsSplitNote() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleSplitMusicXml("CLI validate split").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"split_note\",\"selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"}}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "validate-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(out.contains("\"ok\": true"));
        assertTrue(out.contains("\"changed_node_ids\": [\"n1\", \"n3\"]"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateApplyCommandAcceptsSplitNote() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleSplitMusicXml("CLI apply split").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "apply-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("<step>C</step>"));
        assertTrue(out.contains("<duration>1</duration>"));
        assertTrue(out.contains("<step>D</step>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateValidateCommandAcceptsInsertNoteAfter() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI validate insert").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"insert_note_after\",\"anchor_selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"},\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "validate-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(out.contains("\"ok\": true"));
        assertTrue(out.contains("\"changed_node_ids\": [\"n1\", \"n4\"]"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateApplyCommandAcceptsInsertNoteAfter() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI apply insert").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "apply-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("<step>A</step>"));
        assertTrue(out.contains("<duration>1</duration>"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateValidateCommandAcceptsUiNoop() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(
                MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI validate noop").getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"ui_noop\",\"reason\":\"cursor_move\"}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "validate-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        String out = outBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(out.contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(out.contains("\"ok\": true"));
        assertTrue(out.contains("\"dirty_changed\": false"));
        assertTrue(out.contains("\"changed_node_ids\": []"));
        assertEquals("", errBytes.toString("UTF-8"));
    }

    @Test
    public void stateApplyCommandAcceptsUiNoop() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        String xml = MusicXmlStateTest.sampleMusicXmlWithDivisions("CLI apply noop");
        ByteArrayInputStream inBytes = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        String command = "{\"type\":\"ui_noop\",\"reason\":\"viewport_change\"}";

        int exitCode = MikuscoreCli.run(new String[] { "state", "apply-command", "--command", command },
                inBytes,
                new PrintStream(outBytes, true, "UTF-8"),
                new PrintStream(errBytes, true, "UTF-8"));

        assertEquals(0, exitCode);
        assertEquals(xml, outBytes.toString("UTF-8"));
        assertEquals("", errBytes.toString("UTF-8"));
    }
}
