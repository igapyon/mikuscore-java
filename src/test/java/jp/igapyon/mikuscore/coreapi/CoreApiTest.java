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
