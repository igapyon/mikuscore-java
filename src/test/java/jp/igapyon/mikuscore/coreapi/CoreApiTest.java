package jp.igapyon.mikuscore.coreapi;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    public void reportsThePinnedUpstreamPackageVersion() {
        assertEquals("0.6.1", CoreApi.version());
    }

    @Test
    public void importsAbcToMusicXmlAsCliResult() {
        CoreApi.CliResult result = CoreApi.importAbcToMusicXml("X:1\nT:Core API ABC\nM:4/4\nL:1/4\nK:C\nC D E F|]\n");

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<work-title>Core API ABC</work-title>"));
        assertTrue(result.getOutput().contains("<barline location=\"right\">"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void reportsThePinnedCliApiInvalidAbcDiagnostic() {
        CoreApi.CliResult result = CoreApi.importAbcToMusicXml("");

        assertFalse(result.isOk());
        assertTrue(result.getDiagnostic().startsWith("Failed to parse ABC:"));
    }

    @Test
    public void reportsPinnedCliApiSelectorResolutionDiagnostics() {
        String source = MusicXmlStateTest.sampleMusicXml("Core API selector diagnostics");

        CoreApi.CliResult nonObject = CoreApi.validateMusicXmlCommand(source,
                "{\"type\":\"change_to_pitch\",\"selector\":\"n1\",\"pitch\":{\"step\":\"G\",\"octave\":4}}");
        CoreApi.CliResult unmatched = CoreApi.validateMusicXmlCommand(source,
                "{\"type\":\"change_to_pitch\",\"selector\":{\"part_id\":\"P9\"},\"pitch\":{\"step\":\"G\",\"octave\":4}}");
        CoreApi.CliResult ambiguous = CoreApi.validateMusicXmlCommand(source,
                "{\"type\":\"change_to_pitch\",\"selector\":{\"part_id\":\"P1\"},\"pitch\":{\"step\":\"G\",\"octave\":4}}");

        assertFalse(nonObject.isOk());
        assertEquals("Failed to resolve CLI command selector: selector must be an object when provided.",
                nonObject.getDiagnostic());
        assertFalse(unmatched.isOk());
        assertEquals("Failed to resolve CLI command selector: selector did not match any note in the current MusicXML state.",
                unmatched.getDiagnostic());
        assertFalse(ambiguous.isOk());
        assertEquals("Failed to resolve CLI command selector: selector matched multiple notes; add more selector fields to disambiguate.",
                ambiguous.getDiagnostic());
    }

    @Test
    public void validatesAndInspectsMusicXmlStateThroughTheCliApiFacade() {
        String source = MusicXmlStateTest.sampleMusicXml("Core API state facade");
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\","
                + "\"pitch\":{\"step\":\"G\",\"octave\":4}}";

        CoreApi.CliResult inspected = CoreApi.inspectMusicXmlMeasure(source, "1");
        CoreApi.CliResult validated = CoreApi.validateMusicXmlCommand(source, command);

        assertTrue(inspected.isOk());
        assertTrue(inspected.getOutput().contains("\"node_id\": \"n1\""));
        assertTrue(validated.isOk());
        assertTrue(validated.getOutput().contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(validated.getOutput().contains("\"ok\": true"));
    }

    @Test
    public void resolvesPinnedCliSelectorsWithEmptyPartMeasureAndVoiceValues() {
        String source = "<score-partwise version=\"4.0\"><part id=\"  \"><measure number=\"  \">"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration>"
                + "<voice> </voice></note></measure></part></score-partwise>";
        String command = "{\"type\":\"change_to_pitch\",\"selector\":{\"part_id\":\"\","
                + "\"measure_number\":\"\",\"measure_note_index\":1,\"voice\":\"\"},"
                + "\"voice\":\"\",\"pitch\":{\"step\":\"G\",\"octave\":4}}";

        CoreApi.CliResult result = CoreApi.validateMusicXmlCommand(source, command);

        assertTrue(result.isOk());
        assertTrue(result.getOutput().contains("\"ok\": true"));
        assertTrue(result.getOutput().contains("\"changed_node_ids\": [\"n1\"]"));
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
        assertTrue(result.getOutput().contains("<barline location=\"right\">"));
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
        CoreApi.CliResult plainResult = CoreApi.encodeCliMuseScoreOutput(mscx, "score.mscx");

        assertEquals(true, result.isOk());
        assertEquals("<museScore version=\"4.0\">\n  <Score/>\n</museScore>",
                MxlIo.extractTextFromZipByExtensions(result.getOutputBytes(), new String[] { ".mscx" }));
        assertEquals("", result.getDiagnostic());
        assertEquals(true, plainResult.isOk());
        assertEquals(mscx, plainResult.getOutput());
    }

    @Test
    public void roundTripsMusicXmlThroughPinnedCliApiMuseScoreZipHelpers() {
        String source = MusicXmlStateTest.sampleMusicXml("Core API MSCZ helper roundtrip");
        CoreApi.CliResult exported = CoreApi.exportMusicXmlToMuseScore(source);
        assertTrue(exported.isOk());

        CoreApi.CliResult encoded = CoreApi.encodeCliMuseScoreOutput(exported.getOutput(), "score.mscz");
        assertTrue(encoded.isOk());
        CoreApi.CliResult decoded = CoreApi.decodeCliMuseScoreInput(encoded.getOutputBytes(), "score.mscz");
        assertTrue(decoded.isOk());
        CoreApi.CliResult imported = CoreApi.importMuseScoreToMusicXml(decoded.getOutput());

        assertTrue(imported.isOk());
        assertTrue(imported.getOutput().contains("<score-partwise"));
        assertTrue(imported.getOutput().contains("<work-title>Core API MSCZ helper roundtrip</work-title>"));
    }

    @Test
    public void createsMusicXmlDownloadPayloads() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API download MusicXML");
        CoreApi.DownloadPayload musicxml = CoreApi.createMusicXmlDownloadPayload(xml, false, false);

        assertTrue(musicxml.getFileName().startsWith("miku-score-"));
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
    public void preservesPinnedJsonStemAndBuildsZipBundlePayloads() {
        CoreApi.DownloadPayload emptyStem = CoreApi.createJsonDownloadPayload("{}", "");
        assertTrue(emptyStem.getFileName().matches("miku-score--[0-9]{12}\\.json"));

        CoreApi.DownloadPayload bundle = CoreApi.createZipBundleDownloadPayload(Arrays.asList(
                new CoreApi.ZipBundleEntry("  first.txt  ", "first".getBytes(StandardCharsets.UTF_8)),
                new CoreApi.ZipBundleEntry("  ", "ignored".getBytes(StandardCharsets.UTF_8)),
                new CoreApi.ZipBundleEntry("nested\\second.txt", "second".getBytes(StandardCharsets.UTF_8)),
                new CoreApi.ZipBundleEntry("first.txt", "duplicate".getBytes(StandardCharsets.UTF_8))),
                "  score bundle  ", false);

        assertTrue(bundle.getFileName().matches("score bundle-[0-9]{12}\\.zip"));
        assertEquals("application/zip", bundle.getContentType());
        assertEquals("first", new String(MxlIo.extractZipEntryBytesByPath(bundle.getBytes(), "first.txt"),
                StandardCharsets.UTF_8));
        assertEquals("second", new String(MxlIo.extractZipEntryBytesByPath(bundle.getBytes(), "nested/second.txt"),
                StandardCharsets.UTF_8));
        assertEquals(Arrays.asList("first.txt", "first.txt"),
                MxlIo.listZipRootEntryPathsByExtensions(bundle.getBytes(), new String[] { ".txt" }));

        CoreApi.DownloadPayload defaultBundle = CoreApi.createZipBundleDownloadPayload(null);
        assertTrue(defaultBundle.getFileName().matches("miku-score-all-[0-9]{12}\\.zip"));
        assertEquals("application/zip", defaultBundle.getContentType());
    }

    @Test
    public void createsFormatDownloadPayloadsFromMusicXml() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API download formats");

        CoreApi.DownloadPayload abc = CoreApi.createAbcDownloadPayload(xml);
        assertTrue(abc.getFileName().startsWith("miku-score-"));
        assertTrue(abc.getFileName().endsWith(".abc"));
        assertEquals("text/plain;charset=utf-8", abc.getContentType());
        assertTrue(abc.getText().contains("T:Core API download formats"));

        CoreApi.DownloadPayload mei = CoreApi.createMeiDownloadPayload(xml);
        assertTrue(mei.getFileName().endsWith(".mei"));
        assertEquals("application/mei+xml;charset=utf-8", mei.getContentType());
        assertTrue(mei.getText().contains("<mei"));

        CoreApi.DownloadPayload customMei = CoreApi.createMeiDownloadPayload(xml, "4.0.1");
        assertTrue(customMei.getText().contains("meiversion=\"4.0.1\""));
        CoreApi.DownloadPayload normalizedMei = CoreApi.createMeiDownloadPayload(xml, "not-a-version");
        assertTrue(normalizedMei.getText().contains("meiversion=\"5.1+basic\""));

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
    public void routesMidiDownloadRuntimeOptionsIntoTheSmfEncoding() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API MIDI download options");
        CoreApi.DownloadPayload safeWriterDefault = CoreApi.createMidiDownloadPayload(xml,
                new CoreApi.MidiOutputOptions(960d, "safe", null));
        CoreApi.DownloadPayload safe = CoreApi.createMidiDownloadPayload(xml,
                new CoreApi.MidiOutputOptions(960d, "safe", Boolean.TRUE));
        CoreApi.DownloadPayload parityWriterOverride = CoreApi.createMidiDownloadPayload(xml,
                new CoreApi.MidiOutputOptions(960d, "musescore_parity", Boolean.FALSE));
        CoreApi.DownloadPayload parity = CoreApi.createMidiDownloadPayload(xml,
                new CoreApi.MidiOutputOptions(960d, "musescore_parity", Boolean.TRUE));
        CoreApi.DownloadPayload configured = CoreApi.createMidiDownloadPayload(xml,
                new CoreApi.MidiOutputOptions(480d, "violin", Boolean.TRUE, "classical_equal", Boolean.TRUE,
                        "strong", "safe", Boolean.FALSE, Boolean.TRUE));

        assertNotNull(safeWriterDefault);
        assertNotNull(safe);
        assertNotNull(parityWriterOverride);
        assertNotNull(parity);
        assertNotNull(configured);
        // Node's nullish option resolution selects safe's Writer backend by
        // default, while a profile default may still be overridden explicitly.
        assertTrue(containsByteSequence(safeWriterDefault.getBytes(), 0xff, 0x04));
        assertFalse(containsByteSequence(safe.getBytes(), 0xff, 0x04));
        assertTrue(containsByteSequence(parityWriterOverride.getBytes(), 0xff, 0x04));
        assertFalse(containsByteSequence(parity.getBytes(), 0xff, 0x04));
        assertEquals(960, smfTicksPerQuarter(safe.getBytes()));
        assertEquals(960, smfTicksPerQuarter(safeWriterDefault.getBytes()));
        assertEquals(480, smfTicksPerQuarter(parityWriterOverride.getBytes()));
        assertEquals(480, smfTicksPerQuarter(parity.getBytes()));
        assertEquals(480, smfTicksPerQuarter(configured.getBytes()));
    }

    @Test
    public void createsMuseScoreDownloadPayloads() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API download MuseScore");
        CoreApi.DownloadPayload mscx = CoreApi.createMuseScoreDownloadPayload(xml, false);

        assertTrue(mscx.getFileName().startsWith("miku-score-"));
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
    public void encodesValueBasedOutputsWithoutBrowserBoundaries() {
        String xml = MusicXmlStateTest.sampleMusicXml("Output boundary");

        CoreApi.EncodedOutput plainMusicXml = CoreApi.encodeMusicXmlOutput(xml);
        assertTrue(plainMusicXml.isText());
        assertTrue(plainMusicXml.getText().contains("\n<score-partwise"));
        CoreApi.EncodedOutput compressedMusicXml = CoreApi.encodeMusicXmlOutput(xml, true);
        assertFalse(compressedMusicXml.isText());
        assertTrue(MxlIo.extractMusicXmlTextFromMxl(compressedMusicXml.getBytes())
                .contains("<work-title>Output boundary</work-title>"));

        assertEquals("<svg/>", CoreApi.encodeSvgOutput("<svg/>"));
        assertEquals("{\"ok\":true}", CoreApi.encodeJsonOutput("{\"ok\":true}"));
        assertTrue(CoreApi.encodeAbcOutput(xml).contains("T:Output boundary"));
        assertNull(CoreApi.encodeAbcOutput("<invalid"));
        assertTrue(CoreApi.encodeMeiOutput(xml, "4.0.1").contains("meiversion=\"4.0.1\""));
        assertTrue(CoreApi.encodeLilyPondOutput(xml).contains("\\score"));

        byte[] midi = CoreApi.encodeMidiOutput(xml, new CoreApi.MidiOutputOptions(480d, "safe", Boolean.TRUE));
        assertNotNull(midi);
        assertEquals(Arrays.asList(0x4d, 0x54, 0x68, 0x64), Arrays.asList(
                Integer.valueOf(midi[0] & 0xff), Integer.valueOf(midi[1] & 0xff),
                Integer.valueOf(midi[2] & 0xff), Integer.valueOf(midi[3] & 0xff)));

        CoreApi.EncodedOutput plainMuseScore = CoreApi.encodeMuseScoreOutput(xml);
        assertTrue(plainMuseScore.isText());
        assertTrue(plainMuseScore.getText().contains("\n  <Score>"));
        CoreApi.EncodedOutput compressedMuseScore = CoreApi.encodeMuseScoreOutput(xml, true);
        assertFalse(compressedMuseScore.isText());
        assertTrue(MxlIo.extractTextFromZipByExtensions(compressedMuseScore.getBytes(), new String[] { ".mscx" })
                .contains("<museScore version=\"4.0\">"));

        byte[] zip = CoreApi.encodeZipBundleOutput(Arrays.asList(
                new CoreApi.OutputArchiveEntry("score.musicxml", xml),
                new CoreApi.OutputArchiveEntry("score.mid", new byte[] { 0x4d, 0x54, 0x68, 0x64 })), false);
        assertTrue(MxlIo.extractTextFromZipByExtensions(zip, new String[] { ".musicxml" })
                .contains("<score-partwise"));
    }

    @Test
    public void exportsMusicXmlToAbcAsCliResult() {
        CoreApi.CliResult result = CoreApi.exportMusicXmlToAbc(MusicXmlStateTest.sampleMusicXml("Core API ABC export"));

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("T:Core API ABC export"));
        assertTrue(result.getOutput().contains("K:C"));
        assertEquals("", result.getDiagnostic());

        CoreApi.CliResult invalid = CoreApi.exportMusicXmlToAbc("<not-xml");
        assertEquals(false, invalid.isOk());
        assertTrue(invalid.getDiagnostic().contains("Failed to parse MusicXML"));
    }

    @Test
    public void rejectsInvalidMusicXmlWithThePinnedCliApiDiagnosticForEveryExportFacade() {
        String invalid = "<not-score/>";
        CoreApi.CliResult[] results = new CoreApi.CliResult[] {
                CoreApi.exportMusicXmlToAbc(invalid),
                CoreApi.exportMusicXmlToMei(invalid),
                CoreApi.exportMusicXmlToLilyPond(invalid),
                CoreApi.exportMusicXmlToMidi(invalid),
                CoreApi.exportMusicXmlToMuseScore(invalid) };

        for (CoreApi.CliResult result : results) {
            assertFalse(result.isOk());
            assertEquals("Failed to parse MusicXML: input is not a valid MusicXML document.",
                    result.getDiagnostic());
            assertEquals(java.util.Collections.singletonList(result.getDiagnostic()), result.getDiagnostics());
        }
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
        assertTrue(result.getDiagnostics().isEmpty());
    }

    @Test
    public void retainsThePinnedMidiImportDiagnosticsArray() {
        CoreApi.CliResult result = CoreApi.importMidiToMusicXml(new byte[0]);

        assertFalse(result.isOk());
        assertEquals(java.util.Collections.singletonList("MIDI input is empty."), result.getDiagnostics());
        assertEquals("MIDI input is empty.", result.getDiagnostic());
        assertTrue(result.getWarnings().isEmpty());
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

        CoreApi.CliResult musescore = CoreApi.exportMusicXmlToMuseScore(xml);
        assertEquals(true, musescore.isOk());
        assertPitchFact(source, firstPitchedFact(CoreApi.importMuseScoreToMusicXml(musescore.getOutput()).getOutput()),
                true);
    }

    @Test
    public void exportsMusicXmlNotesAndPartsToMuseScoreInsteadOfAnEmptyPlaceholder() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\">"
                + "<work><work-title>Core Muse export</work-title></work><part-list>"
                + "<score-part id=\"P1\"><part-name>Piano</part-name></score-part>"
                + "<score-part id=\"P2\"><part-name>Bass</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions><key><fifths>0</fifths>"
                + "</key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line>"
                + "</clef></attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure></part>"
                + "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>480</divisions><key><fifths>0</fifths>"
                + "</key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>F</sign><line>4</line>"
                + "</clef></attributes><note><pitch><step>E</step><octave>2</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure></part></score-partwise>";

        CoreApi.CliResult result = CoreApi.exportMusicXmlToMuseScore(xml);

        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<metaTag name=\"workTitle\">Core Muse export</metaTag>"));
        assertTrue(result.getOutput().contains("<trackName>Piano</trackName>"));
        assertTrue(result.getOutput().contains("<trackName>Bass</trackName>"));
        assertTrue(result.getOutput().contains("<pitch>60</pitch>"));
        assertTrue(result.getOutput().contains("<pitch>40</pitch>"));
        assertEquals("", result.getDiagnostic());

        CoreApi.CliResult imported = CoreApi.importMuseScoreToMusicXml(result.getOutput());
        assertEquals(true, imported.isOk());
        assertTrue(imported.getOutput().contains("<part id=\"P1\">"));
        assertTrue(imported.getOutput().contains("<part id=\"P2\">"));
        assertTrue(imported.getOutput().contains("<step>C</step>"));
        assertTrue(imported.getOutput().contains("<step>E</step>"));
    }

    @Test
    public void routesMuseScoreExportCutTimeOptionThroughCoreApi() {
        String xml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time symbol=\"cut\"><beats>4</beats>"
                + "<beat-type>4</beat-type></time></attributes><note><pitch><step>C</step><octave>4</octave>"
                + "</pitch><duration>1920</duration><voice>1</voice><type>whole</type></note>"
                + "</measure></part></score-partwise>";
        CoreApi.CliResult result = CoreApi.exportMusicXmlToMuseScore(xml, true);
        assertEquals(true, result.isOk());
        assertTrue(result.getOutput().contains("<TimeSig><subtype>2</subtype><sigN>2</sigN><sigD>2</sigD></TimeSig>"));
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
        assertTrue(result.getOutput().contains("<barline location=\"right\">"));
        assertEquals("", result.getDiagnostic());
    }

    @Test
    public void routesMuseScoreImportMetadataOptionsThroughCoreApi() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch>"
                + "</Note></Chord></voice></Measure></Staff></Score></museScore>";
        CoreApi.CliResult defaults = CoreApi.importMuseScoreToMusicXml(mscx);
        assertEquals(true, defaults.isOk());
        assertTrue(defaults.getOutput().contains("mks:src:musescore:raw-encoding"));

        CoreApi.CliResult noMetadata = CoreApi.importMuseScoreToMusicXml(mscx, false, false, false, false);
        assertEquals(true, noMetadata.isOk());
        assertEquals(false, noMetadata.getOutput().contains("mks:src:musescore:"));

        String cutMscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><TimeSig><subtype>2</subtype><sigN>4</sigN><sigD>4</sigD></TimeSig>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";
        CoreApi.CliResult configured = CoreApi.importMuseScoreToMusicXml(cutMscx, false, false, true, false);
        assertEquals(true, configured.isOk());
        assertTrue(configured.getOutput().contains("<time symbol=\"cut\">"));
        assertTrue(configured.getOutput().contains("<beats>2</beats>"));
        assertTrue(configured.getOutput().contains("<beat-type>2</beat-type>"));
        assertEquals(false, configured.getOutput().contains("<beam number=\"1\">"));
        assertEquals(false, configured.getOutput().contains("mks:src:musescore:"));
    }

    @Test
    public void routesMuseScoreScoreMetadataThroughCoreApi() {
        String mscx = "<museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Core metadata</metaTag>"
                + "<metaTag name=\"movementTitle\">Movement</metaTag><metaTag name=\"composer\">Composer</metaTag>"
                + "<metaTag name=\"copyright\">Copyright</metaTag><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch>"
                + "</Note></Chord></voice></Measure></Staff></Score></museScore>";

        CoreApi.CliResult result = CoreApi.importMuseScoreToMusicXml(mscx, false, false, false, false);

        assertTrue(result.isOk());
        assertTrue(result.getOutput().contains("<work-title>Core metadata</work-title>"));
        assertTrue(result.getOutput().contains("<movement-title>Movement</movement-title>"));
        assertTrue(result.getOutput().contains("<creator type=\"composer\">Composer</creator>"));
        assertTrue(result.getOutput().contains("<rights>Copyright</rights>"));
    }

    @Test
    public void routesMuseScorePlaceholderWarningMetadataThroughCoreApi() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division></Score></museScore>";

        CoreApi.CliResult defaults = CoreApi.importMuseScoreToMusicXml(mscx);
        assertTrue(defaults.isOk());
        assertTrue(defaults.getOutput().contains("mks:diag:count"));
        assertTrue(defaults.getOutput().contains("action=placeholder-created"));

        CoreApi.CliResult noDebug = CoreApi.importMuseScoreToMusicXml(mscx, true, false, false, true);
        assertTrue(noDebug.isOk());
        assertFalse(noDebug.getOutput().contains("mks:diag:"));
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
    public void convertsPinnedValueBasedLoadInputsWithStructuredDiagnostics() {
        String xml = MusicXmlStateTest.sampleMusicXml("Core API value load");
        CoreApi.LoadInputResult musicXml = CoreApi.convertLoadInputToMusicXml("musicxml", xml);
        assertTrue(musicXml.isOk());
        assertTrue(musicXml.getXml().contains("<work-title>Core API value load</work-title>"));
        assertTrue(musicXml.getDiagnostics().isEmpty());

        CoreApi.LoadInputResult mxl = CoreApi.convertLoadInputToMusicXml("mxl", MxlIo.makeMxlBytes(xml));
        assertTrue(mxl.isOk());
        assertTrue(mxl.getXml().contains("<score-partwise"));

        CoreApi.LoadInputResult midiFailure = CoreApi.convertLoadInputToMusicXml("midi", new byte[0]);
        assertFalse(midiFailure.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", midiFailure.getDiagnosticCode());
        assertEquals("Failed to parse MIDI: MIDI input is empty. (MIDI_INVALID_FILE)",
                midiFailure.getDiagnosticMessage());
        assertEquals("MIDI_INVALID_FILE", midiFailure.getDiagnostics().get(0).getCode());
        assertEquals("MIDI input is empty.", midiFailure.getDiagnostics().get(0).getMessage());

        CoreApi.LoadInputResult textAsMidi = CoreApi.convertLoadInputToMusicXml("midi", "not bytes");
        CoreApi.LoadInputResult bytesAsAbc = CoreApi.convertLoadInputToMusicXml("abc", new byte[] { 0x41 });
        assertFalse(textAsMidi.isOk());
        assertEquals("Expected binary input for midi.", textAsMidi.getDiagnosticMessage());
        assertFalse(bytesAsAbc.isOk());
        assertEquals("Expected text input for abc.", bytesAsAbc.getDiagnosticMessage());
    }

    @Test
    public void resolvesPinnedDirectLoadFlowAndNewScoreResultShapes() {
        String abc = "X:1\nT:Core API direct ABC\nM:4/4\nL:1/4\nK:C\nC|\n";
        CoreApi.LoadFlowResult abcResult = CoreApi.resolveDirectLoadFlow(false, "abc", abc, "unused");
        assertTrue(abcResult.isOk());
        assertTrue(abcResult.isCollapseInputSection());
        assertTrue(abcResult.getXmlToLoad().contains("<score-partwise"));
        assertEquals(abcResult.getXmlToLoad(), abcResult.getNextXmlInputText());
        assertEquals(abc, abcResult.getNextAbcInputText());

        CoreApi.LoadFlowResult newResult = CoreApi.resolveDirectLoadFlow(true, "abc", "ignored",
                "<score-partwise version=\"4.0\"/>");
        assertTrue(newResult.isOk());
        assertEquals("<score-partwise version=\"4.0\"/>", newResult.getXmlToLoad());
        assertNull(newResult.getNextAbcInputText());

        CoreApi.LoadFlowResult unsupported = CoreApi.resolveDirectLoadFlow(false, "unknown", "", "");
        assertFalse(unsupported.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", unsupported.getDiagnosticCode());
        assertEquals("Unsupported input format: unknown", unsupported.getDiagnosticMessage());
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
    public void resolvesPinnedMsczCorpusThroughTheCoreApiConversionPath() throws Exception {
        java.io.InputStream stream = CoreApiTest.class.getClassLoader()
                .getResourceAsStream("upstream-zip/sample2.mscz");
        assertTrue(stream != null);
        byte[] archive;
        try {
            archive = stream.readAllBytes();
        } finally {
            stream.close();
        }

        CoreApi.CliResult result = CoreApi.resolveLoadFileToMusicXml(archive, "sample2.mscz");

        assertTrue(result.isOk());
        assertTrue(result.getOutput().contains("<part-name>Violin 1</part-name>"));
        assertTrue(result.getOutput().contains("<part-name>Viola</part-name>"));
        assertTrue(result.getOutput().contains("<clef"));
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
    public void reportsPinnedLoadFlowMxlParseDiagnostic() {
        CoreApi.CliResult result = CoreApi.resolveLoadFileToMusicXml(new byte[] { 1, 2, 3 }, "broken.mxl");

        assertFalse(result.isOk());
        assertTrue(result.getDiagnostic().startsWith("Failed to parse MXL: "));
    }

    @Test
    public void reportsPinnedLoadFlowMidiDiagnosticWithItsCode() {
        CoreApi.CliResult result = CoreApi.resolveLoadFileToMusicXml(new byte[0], "broken.mid");

        assertFalse(result.isOk());
        assertEquals("Failed to parse MIDI: MIDI input is empty. (MIDI_INVALID_FILE)", result.getDiagnostic());
    }

    @Test
    public void reportsPinnedLoadFlowMuseScoreZipParseDiagnostic() {
        CoreApi.CliResult result = CoreApi.resolveLoadFileToMusicXml(new byte[] { 1, 2, 3 }, "broken.mscz");

        assertFalse(result.isOk());
        assertTrue(result.getDiagnostic().startsWith("Failed to parse MuseScore: "));
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
    public void diffsMusicXmlStateThroughTheCliApiFacade() {
        String before = MusicXmlStateTest.sampleMusicXml("Core API before");
        String after = before.replace("<step>C</step>", "<step>G</step>");

        CoreApi.CliResult result = CoreApi.diffMusicXmlState(before, after);

        assertTrue(result.isOk());
        assertTrue(result.getOutput().contains("\"kind\": \"musicxml_state_diff\""));
        assertTrue(result.getOutput().contains("\"changed\": true"));
    }

    @Test
    public void appliesMusicXmlCommandAndRetainsSuccessfulTimingWarnings() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        CoreApi.CliResult result = CoreApi.applyMusicXmlCommand(
                MusicXmlStateTest.sampleUnderfullInsertMusicXml("Core API timing warning"), command);

        assertTrue(result.isOk());
        assertTrue(result.getOutput().contains("<step>A</step>"));
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Projected occupied time"));
    }

    @Test
    public void returnsDiagnosticForInvalidMusicXmlState() {
        CoreApi.CliResult result = CoreApi.summarizeMusicXmlState("<not-score/>");

        assertEquals(false, result.isOk());
        assertEquals("", result.getOutput());
        assertTrue(result.getDiagnostic().contains("Failed to summarize MusicXML state"));
    }

    private static int smfTicksPerQuarter(byte[] bytes) {
        assertNotNull(bytes);
        assertTrue(bytes.length >= 14);
        assertEquals('M', bytes[0]);
        assertEquals('T', bytes[1]);
        assertEquals('h', bytes[2]);
        assertEquals('d', bytes[3]);
        return ((bytes[12] & 0xff) << 8) | (bytes[13] & 0xff);
    }

    private static boolean containsByteSequence(byte[] bytes, int... sequence) {
        if (bytes == null || sequence == null || sequence.length == 0) {
            return false;
        }
        for (int start = 0; start <= bytes.length - sequence.length; start++) {
            boolean matches = true;
            for (int index = 0; index < sequence.length; index++) {
                if ((bytes[start + index] & 0xff) != (sequence[index] & 0xff)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
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
