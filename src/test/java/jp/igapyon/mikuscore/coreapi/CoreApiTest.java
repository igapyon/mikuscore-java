package jp.igapyon.mikuscore.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuscore.musicxml.MusicXmlStateTest;
import jp.igapyon.mikuscore.musicxml.MxlIo;

public class CoreApiTest {
    @Test
    public void importsAbcToMusicXmlAsCliResult() {
        CoreApi.CliResult result = CoreApi.importAbcToMusicXml("X:1\nT:Core API ABC\nM:4/4\nL:1/4\nK:C\nC D E F|]\n");

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<work-title>Core API ABC</work-title>"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void importsMeiToMusicXmlAsCliResult() {
        String mei = "<mei><music><body><mdiv><score><title>Core API MEI</title>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffDef n=\"1\" label=\"Voice\"/></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        CoreApi.CliResult result = CoreApi.importMeiToMusicXml(mei);

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<work-title>Core API MEI</work-title>"));
        assertTrue(result.getOutput().contains("<part-name>Voice</part-name>"));
        assertTrue(result.getOutput().contains("<step>E</step>"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void importsLilyPondToMusicXmlAsCliResult() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\header { title = \"Core API LilyPond\" }\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score { \\new Staff = \"P1\" { c'4 d'4 e'4 f'4 } }";

        CoreApi.CliResult result = CoreApi.importLilyPondToMusicXml(lily);

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<work-title>Core API LilyPond</work-title>"));
        assertTrue(result.getOutput().contains("<step>C</step>"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void decodesMusicXmlTextInputAsCliResult() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API decode text");
        CoreApi.CliResult result = CoreApi.decodeCliMusicXmlInput(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "score.musicxml");

        assertEquals(true, result.isOk());
        assertEquals(xml, result.getOutput());
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void decodesMxlInputAsCliResult() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API decode mxl");
        CoreApi.CliResult result = CoreApi.decodeCliMusicXmlInput(MxlIo.makeMxlBytes(xml), "score.mxl");

        assertEquals(true, result.isOk());
        assertEquals(xml, result.getOutput());
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void encodesMxlOutputAsCliResult() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API encode mxl");
        CoreApi.CliResult result = CoreApi.encodeCliMusicXmlOutput(xml, "score.mxl");

        assertEquals(true, result.isOk());
        assertEquals(xml, MxlIo.extractMusicXmlTextFromMxl(result.getOutputBytes()));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void decodesMuseScoreZipInputAsCliResult() {
        String mscx = "<museScore version=\"4.0\"><Score/></museScore>";
        CoreApi.CliResult result = CoreApi.decodeCliMuseScoreInput(MxlIo.makeMsczBytes(mscx), "score.mscz");

        assertEquals(true, result.isOk());
        assertEquals(mscx, result.getOutput());
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void encodesMuseScoreZipOutputAsCliResult() {
        String mscx = "<museScore version=\"4.0\"><Score/></museScore>";
        CoreApi.CliResult result = CoreApi.encodeCliMuseScoreOutput(mscx, "score.mscz");

        assertEquals(true, result.isOk());
        assertEquals(mscx, MxlIo.extractTextFromZipByExtensions(result.getOutputBytes(), new String[] { ".mscx" }));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void exportsMusicXmlToAbcAsCliResult() {
        CoreApi.CliResult result = CoreApi.exportMusicXmlToAbc(MusicXmlStateTest.sampleMusicXml("Core API ABC export"));

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("T:Core API ABC export"));
        assertTrue(result.getOutput().contains("K:C"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void exportsMusicXmlToMeiAsCliResult() {
        CoreApi.CliResult result = CoreApi.exportMusicXmlToMei(MusicXmlStateTest.sampleMusicXml("Core API MEI export"));

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<mei"));
        assertTrue(result.getOutput().contains("<title>Core API MEI export</title>"));
        assertTrue(result.getOutput().contains("<scoreDef"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void exportsMusicXmlToMidiAsCliResult() {
        CoreApi.CliResult result =
                CoreApi.exportMusicXmlToMidi(MusicXmlStateTest.sampleMusicXml("Core API MIDI export"));

        assertEquals(true, result.isOk());
        byte[] bytes = result.getOutputBytes();
        assertEquals('M', bytes[0]);
        assertEquals('T', bytes[1]);
        assertEquals('h', bytes[2]);
        assertEquals('d', bytes[3]);
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void rejectsMusicXmlToMidiWhenNoPlayableEventsExist() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Empty</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><rest/><duration>4</duration><voice>1</voice></note></measure></part></score-partwise>";

        CoreApi.CliResult result = CoreApi.exportMusicXmlToMidi(xml);

        assertEquals(false, result.isOk());
        assertTrue(result.getDiagnostic().contains("no playable note events found"));
    }

    @Test
    public void importsMidiToMusicXmlAsCliResult() {
        CoreApi.CliResult midi = CoreApi.exportMusicXmlToMidi(MusicXmlStateTest.sampleMusicXml("Core API MIDI import"));

        CoreApi.CliResult result = CoreApi.importMidiToMusicXml(midi.getOutputBytes());

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<score-partwise"));
        assertTrue(result.getOutput().contains("<pitch>"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void summarizesMusicXmlStateAsCliResult() {
        CoreApi.CliResult result = CoreApi.summarizeMusicXmlState(MusicXmlStateTest.sampleMusicXml("Core API summary"));

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("\"kind\": \"musicxml_state_summary\""));
        assertTrue(result.getOutput().contains("\"title\": \"Core API summary\""));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void returnsDiagnosticForInvalidMusicXmlState() {
        CoreApi.CliResult result = CoreApi.summarizeMusicXmlState("<not-score/>");

        assertEquals(false, result.isOk());
        assertEquals("", result.getOutput());
        assertTrue(result.getDiagnostic().contains("Failed to summarize MusicXML state"));
    }
}
