package jp.igapyon.mikuscore.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
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
    public void createsMusicXmlDownloadPayloads() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API download MusicXML");
        CoreApi.DownloadPayload musicxml = CoreApi.createMusicXmlDownloadPayload(xml, false, false);

        assertTrue(musicxml.getFileName().endsWith(".musicxml"));
        assertEquals("application/xml;charset=utf-8", musicxml.getContentType());
        assertTrue(musicxml.getText().contains("<score-partwise"));

        CoreApi.DownloadPayload xmlPayload = CoreApi.createMusicXmlDownloadPayload(xml, false, true);
        assertTrue(xmlPayload.getFileName().endsWith(".xml"));

        CoreApi.DownloadPayload mxl = CoreApi.createMusicXmlDownloadPayload(xml, true, false);
        assertTrue(mxl.getFileName().endsWith(".mxl"));
        assertEquals("application/vnd.recordare.musicxml", mxl.getContentType());
        assertTrue(MxlIo.extractMusicXmlTextFromMxl(mxl.getBytes()).contains("<score-partwise"));
    }

    @Test
    public void createsDirectTextDownloadPayloadsWithMimeTypes() {
        CoreApi.DownloadPayload svg = CoreApi.createSvgDownloadPayload("<svg/>");
        CoreApi.DownloadPayload json = CoreApi.createJsonDownloadPayload("{\"ok\":true}", "measure-detail");
        CoreApi.DownloadPayload vsqx = CoreApi.createVsqxDownloadPayload(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><vsq4><vVoiceTable><vVoice/></vVoiceTable></vsq4>");

        assertTrue(svg.getFileName().endsWith(".svg"));
        assertEquals("image/svg+xml;charset=utf-8", svg.getContentType());
        assertTrue(json.getFileName().endsWith(".json"));
        assertEquals("application/json;charset=utf-8", json.getContentType());
        assertTrue(vsqx.getFileName().endsWith(".vsqx"));
        assertTrue(vsqx.getText().contains("\n  <vVoiceTable>"));
        assertTrue(vsqx.getText().contains("\n    <vVoice/>"));
    }

    @Test
    public void createsFormatDownloadPayloadsFromMusicXml() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API download formats");

        CoreApi.DownloadPayload abc = CoreApi.createAbcDownloadPayload(xml);
        assertTrue(abc.getFileName().endsWith(".abc"));
        assertEquals("text/plain;charset=utf-8", abc.getContentType());
        assertTrue(abc.getText().contains("T:Core API download formats"));

        CoreApi.DownloadPayload mei = CoreApi.createMeiDownloadPayload(xml);
        assertTrue(mei.getFileName().endsWith(".mei"));
        assertEquals("application/mei+xml;charset=utf-8", mei.getContentType());
        assertTrue(mei.getText().contains("<mei"));

        CoreApi.DownloadPayload lily = CoreApi.createLilyPondDownloadPayload(xml);
        assertTrue(lily.getFileName().endsWith(".ly"));
        assertEquals("text/plain;charset=utf-8", lily.getContentType());
        assertTrue(lily.getText().contains("\\score"));

        CoreApi.DownloadPayload midi = CoreApi.createMidiDownloadPayload(xml);
        assertTrue(midi.getFileName().endsWith(".mid"));
        assertEquals("audio/midi", midi.getContentType());
        assertEquals('M', midi.getBytes()[0]);
    }

    @Test
    public void createsMuseScoreDownloadPayloads() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API download MuseScore");
        CoreApi.DownloadPayload mscx = CoreApi.createMuseScoreDownloadPayload(xml, false);

        assertTrue(mscx.getFileName().endsWith(".mscx"));
        assertEquals("application/xml;charset=utf-8", mscx.getContentType());
        assertTrue(mscx.getText().contains("\n  <Score>"));

        CoreApi.DownloadPayload mscz = CoreApi.createMuseScoreDownloadPayload(xml, true);
        assertTrue(mscz.getFileName().endsWith(".mscz"));
        assertEquals("application/zip", mscz.getContentType());
        assertTrue(MxlIo.extractTextFromZipByExtensions(mscz.getBytes(), new String[] { ".mscx" })
                .contains("<museScore"));
    }

    @Test
    public void returnsNullForInvalidDownloadConversions() {
        assertNull(CoreApi.createAbcDownloadPayload("<not-musicxml/>"));
        assertNull(CoreApi.createLilyPondDownloadPayload("<not-musicxml/>"));
        assertNull(CoreApi.createMuseScoreDownloadPayload("<not-musicxml/>", false));
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
    public void roundtripsMinimalMusicXmlAcrossCoreFormatsForCffpBaseline() {
        String xml = MusicXmlStateTest.sampleFullMeasureMusicXml("Core API CFFP baseline");
        PitchFact source = firstPitchedFact(xml);

        CoreApi.CliResult abc = CoreApi.exportMusicXmlToAbc(xml);
        assertEquals(true, abc.isOk());
        assertPitchFact(source, firstPitchedFact(CoreApi.importAbcToMusicXml(abc.getOutput()).getOutput()), true);

        CoreApi.CliResult mei = CoreApi.exportMusicXmlToMei(xml);
        assertEquals(true, mei.isOk());
        assertPitchFact(source, firstPitchedFact(CoreApi.importMeiToMusicXml(mei.getOutput()).getOutput()), true);

        CoreApi.CliResult lily = CoreApi.exportMusicXmlToLilyPond(xml);
        assertEquals(true, lily.isOk());
        assertPitchFact(source, firstPitchedFact(CoreApi.importLilyPondToMusicXml(lily.getOutput()).getOutput()), true);

        CoreApi.CliResult midi = CoreApi.exportMusicXmlToMidi(xml);
        assertEquals(true, midi.isOk());
        assertPitchFact(source, firstPitchedFact(CoreApi.importMidiToMusicXml(midi.getOutputBytes()).getOutput()), false);
    }

    @Test
    public void importsMuseScoreToMusicXmlAsCliResult() {
        CoreApi.CliResult result = CoreApi.importMuseScoreToMusicXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Core API Muse import</metaTag>"
                + "<Division>480</Division><Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord><Rest><durationType>quarter</durationType></Rest>"
                + "</voice></Measure></Staff></Score></museScore>");

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<score-partwise"));
        assertTrue(result.getOutput().contains("<work-title>Core API Muse import</work-title>"));
        assertTrue(result.getOutput().contains("<step>C</step>"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void resolvesLoadFileMusicXmlAndMidiRoutes() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API load MusicXML");
        CoreApi.CliResult xmlResult = CoreApi.resolveLoadFileToMusicXml(
                xml.getBytes(java.nio.charset.StandardCharsets.UTF_8), "score.musicxml");

        assertEquals(true, xmlResult.isOk());
        assertTrue(xmlResult.getOutput().contains("<score-partwise"));
        assertTrue(xmlResult.getOutput().contains("<work-title>Core API load MusicXML</work-title>"));

        CoreApi.CliResult midi = CoreApi.exportMusicXmlToMidi(MusicXmlStateTest.sampleMusicXml("Core API load MIDI"));
        CoreApi.CliResult midiResult = CoreApi.resolveLoadFileToMusicXml(midi.getOutputBytes(), "score.mid");

        assertEquals(true, midiResult.isOk());
        assertTrue(midiResult.getOutput().contains("<score-partwise"));
        assertTrue(midiResult.getOutput().contains("<pitch>"));
    }

    @Test
    public void resolvesLoadFileMeiLilyPondAndMuseScoreRoutes() {
        String mei = "<mei><music><body><mdiv><score><title>Core API load MEI</title>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffDef n=\"1\" label=\"Voice\"/></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";
        CoreApi.CliResult meiResult = CoreApi.resolveLoadFileToMusicXml(
                mei.getBytes(java.nio.charset.StandardCharsets.UTF_8), "score.mei");

        assertEquals(true, meiResult.isOk());
        assertTrue(meiResult.getOutput().contains("<work-title>Core API load MEI</work-title>"));

        String lily = "\\version \"2.24.0\"\n\\header { title = \"Core API load Lily\" }\n"
                + "\\score { \\new Staff = \"P1\" { c'4 d'4 e'4 f'4 } }";
        CoreApi.CliResult lilyResult = CoreApi.resolveLoadFileToMusicXml(
                lily.getBytes(java.nio.charset.StandardCharsets.UTF_8), "score.ly");

        assertEquals(true, lilyResult.isOk());
        assertTrue(lilyResult.getOutput().contains("<work-title>Core API load Lily</work-title>"));

        String mscx = "<museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Core API load Muse</metaTag>"
                + "<Division>480</Division><Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";
        CoreApi.CliResult museResult = CoreApi.resolveLoadFileToMusicXml(
                mscx.getBytes(java.nio.charset.StandardCharsets.UTF_8), "score.mscx");

        assertEquals(true, museResult.isOk());
        assertTrue(museResult.getOutput().contains("<work-title>Core API load Muse</work-title>"));
        assertTrue(museResult.getOutput().contains("<step>C</step>"));
    }

    @Test
    public void resolvesLoadFileMuseScoreZipAndMusicXmlFallback() {
        String mscx = "<museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Core API load MSCZ</metaTag>"
                + "<Division>480</Division><Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>64</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";
        CoreApi.CliResult msczResult =
                CoreApi.resolveLoadFileToMusicXml(MxlIo.makeMsczBytes(mscx), "score.mscz");

        assertEquals(true, msczResult.isOk());
        assertTrue(msczResult.getOutput().contains("<work-title>Core API load MSCZ</work-title>"));
        assertTrue(msczResult.getOutput().contains("<step>E</step>"));

        String xml = MusicXmlStateTest.sampleMusicXml("Core API load fallback MusicXML");
        CoreApi.CliResult fallbackResult =
                CoreApi.resolveLoadFileToMusicXml(MxlIo.makeMxlBytes(xml), "score.mscz");

        assertEquals(true, fallbackResult.isOk());
        assertTrue(fallbackResult.getOutput().contains("<work-title>Core API load fallback MusicXML</work-title>"));
    }

    @Test
    public void rejectsLoadFileUnsupportedAndUnavailableRoutes() {
        CoreApi.CliResult missing = CoreApi.resolveLoadFileToMusicXml(new byte[0], "");
        assertEquals(false, missing.isOk());
        assertTrue(missing.getDiagnostic().contains("Please select a file"));

        CoreApi.CliResult unsupported = CoreApi.resolveLoadFileToMusicXml(new byte[0], "score.txt");
        assertEquals(false, unsupported.isOk());
        assertTrue(unsupported.getDiagnostic().contains("Unsupported file extension"));

        CoreApi.CliResult vsqx = CoreApi.resolveLoadFileToMusicXml("<vsq3/>".getBytes(
                java.nio.charset.StandardCharsets.UTF_8), "score.vsqx");
        assertEquals(false, vsqx.isOk());
        assertTrue(vsqx.getDiagnostic().contains("VSQX import is not available"));
    }

    @Test
    public void exportsMusicXmlToMuseScoreAsCliResult() {
        CoreApi.CliResult result = CoreApi.exportMusicXmlToMuseScore(
                MusicXmlStateTest.sampleMusicXml("Core API Muse export"));

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<museScore version=\"4.0\">"));
        assertTrue(result.getOutput().contains("<metaTag name=\"workTitle\">Core API Muse export</metaTag>"));
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

    private static void assertPitchFact(PitchFact expected, PitchFact actual, boolean preserveDuration) {
        assertEquals(expected.step, actual.step);
        assertEquals(expected.octave, actual.octave);
        assertEquals(0, actual.startDiv);
        if (preserveDuration) {
            assertEquals(expected.quarterLength, actual.quarterLength, 0.1d);
        }
    }

    private static PitchFact firstPitchedFact(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        Element part = firstElement(doc, "part");
        int divisions = 1;
        int cursorDiv = 0;
        Node child = part == null ? null : part.getFirstChild();
        while (child != null) {
            if (child instanceof Element && "measure".equals(((Element) child).getTagName())) {
                Element measure = (Element) child;
                String parsedDivisions = directGrandchildText(measure, "attributes", "divisions");
                if (parsedDivisions.length() > 0) {
                    divisions = Math.max(1, parseInt(parsedDivisions, divisions));
                }
                Node measureChild = measure.getFirstChild();
                while (measureChild != null) {
                    if (measureChild instanceof Element) {
                        Element element = (Element) measureChild;
                        if ("backup".equals(element.getTagName())) {
                            cursorDiv = Math.max(0, cursorDiv - parseInt(directChildText(element, "duration"), 0));
                        } else if ("note".equals(element.getTagName())) {
                            boolean chord = directChild(element, "chord") != null;
                            boolean grace = directChild(element, "grace") != null;
                            int duration = parseInt(directChildText(element, "duration"), 0);
                            Element pitch = directChild(element, "pitch");
                            if (pitch != null) {
                                String step = directChildText(pitch, "step");
                                int octave = parseInt(directChildText(pitch, "octave"), 0);
                                return new PitchFact(step, octave, duration / (double) divisions, cursorDiv);
                            }
                            if (!chord && !grace && duration > 0) {
                                cursorDiv += duration;
                            }
                        }
                    }
                    measureChild = measureChild.getNextSibling();
                }
            }
            child = child.getNextSibling();
        }
        throw new AssertionError("no pitched note found");
    }

    private static Element firstElement(Document doc, String tagName) {
        if (doc == null || doc.getElementsByTagName(tagName).getLength() == 0) {
            return null;
        }
        return (Element) doc.getElementsByTagName(tagName).item(0);
    }

    private static Element directChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private static String directChildText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        return child == null || child.getTextContent() == null ? "" : child.getTextContent().trim();
    }

    private static String directGrandchildText(Element parent, String childName, String grandchildName) {
        return directChildText(directChild(parent, childName), grandchildName);
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static final class PitchFact {
        private final String step;
        private final int octave;
        private final double quarterLength;
        private final int startDiv;

        private PitchFact(String step, int octave, double quarterLength, int startDiv) {
            this.step = step;
            this.octave = octave;
            this.quarterLength = quarterLength;
            this.startDiv = startDiv;
        }
    }
}
