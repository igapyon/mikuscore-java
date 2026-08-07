package jp.igapyon.mikuscore.coreapi;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Document;

import jp.igapyon.mikuscore.abc.AbcIo;
import jp.igapyon.mikuscore.lilypond.LilyPondIo;
import jp.igapyon.mikuscore.mei.MeiIo;
import jp.igapyon.mikuscore.midi.MidiIo;
import jp.igapyon.mikuscore.musescore.MuseScoreIo;
import jp.igapyon.mikuscore.musicxml.AccidentalSpelling;
import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.musicxml.MusicXmlState;
import jp.igapyon.mikuscore.musicxml.MxlIo;

/**
 * Minimal public core API placeholder for the straight-conversion foundation.
 */
public final class CoreApi {
    private static final DateTimeFormatter DOWNLOAD_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String MIME_MXL = "application/vnd.recordare.musicxml";
    private static final String MIME_SVG = "image/svg+xml;charset=utf-8";
    private static final String MIME_JSON = "application/json;charset=utf-8";
    private static final String MIME_XML = "application/xml;charset=utf-8";
    private static final String MIME_MIDI = "audio/midi";
    private static final String MIME_TEXT = "text/plain;charset=utf-8";
    private static final String MIME_MEI = "application/mei+xml;charset=utf-8";
    private static final String MIME_ZIP = "application/zip";

    private CoreApi() {
    }

    public static String version() {
        Package pkg = CoreApi.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return version == null ? "0.5.1" : version;
    }

    public static CliResult importAbcToMusicXml(String abcText) {
        try {
            return textResult(AbcIo.musicXmlFromAbc(abcText, new AbcIo.AbcImportOptions()));
        } catch (Exception ex) {
            return failureResult("Failed to parse ABC: " + ex.getMessage());
        }
    }

    public static CliResult importMeiToMusicXml(String meiText) {
        try {
            return textResult(MusicXmlIo.normalizeImportedMusicXmlText(MeiIo.convertMeiToMusicXml(meiText)));
        } catch (Exception ex) {
            return failureResult("Failed to parse MEI: " + ex.getMessage());
        }
    }

    public static CliResult importLilyPondToMusicXml(String lilyPondText) {
        try {
            return textResult(LilyPondIo.convertLilyPondToMusicXml(lilyPondText));
        } catch (Exception ex) {
            return failureResult("Failed to parse LilyPond: " + ex.getMessage());
        }
    }

    public static CliResult importMidiToMusicXml(byte[] midiBytes) {
        try {
            MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midiBytes, new MidiIo.MidiImportOptions());
            if (!result.isOk()) {
                return failureResult("Failed to parse MIDI: " + midiDiagnosticsText(result));
            }
            return textResult(MusicXmlIo.normalizeImportedMusicXmlText(result.getXml()));
        } catch (Exception ex) {
            return failureResult("Failed to parse MIDI: " + ex.getMessage());
        }
    }

    public static CliResult importMuseScoreToMusicXml(String musescoreText) {
        try {
            return textResult(convertSimpleMuseScoreToMusicXml(musescoreText));
        } catch (Exception ex) {
            return failureResult("Failed to parse MuseScore: " + ex.getMessage());
        }
    }

    public static CliResult resolveLoadFileToMusicXml(byte[] inputBytes, String inputPath) {
        byte[] bytes = inputBytes == null ? new byte[0] : inputBytes;
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (inputPath == null || inputPath.trim().length() == 0) {
            return failureResult("Please select a file.");
        }
        try {
            if (hasExtension(inputPath, ".mxl")) {
                return textResult(MusicXmlIo.normalizeImportedMusicXmlText(MxlIo.extractMusicXmlTextFromMxl(bytes)));
            }
            if (hasExtension(inputPath, ".musicxml") || hasExtension(inputPath, ".xml")) {
                return textResult(MusicXmlIo.normalizeImportedMusicXmlText(text));
            }
            if (hasExtension(inputPath, ".abc")) {
                return importAbcToMusicXml(text);
            }
            if (hasExtension(inputPath, ".mid") || hasExtension(inputPath, ".midi")) {
                return importMidiToMusicXml(bytes);
            }
            if (hasExtension(inputPath, ".vsqx")) {
                return failureResult("Failed to parse VSQX: VSQX import is not available in this Java core API.");
            }
            if (hasExtension(inputPath, ".mei")) {
                return importMeiToMusicXml(text);
            }
            if (hasExtension(inputPath, ".ly")) {
                return importLilyPondToMusicXml(text);
            }
            if (hasExtension(inputPath, ".mscx")) {
                return importMuseScoreToMusicXml(text);
            }
            if (hasExtension(inputPath, ".mscz")) {
                return resolveMuseScoreZipLoad(bytes);
            }
            return failureResult("Unsupported file extension. Use .musicxml, .xml, .mxl, .abc, .mid, .midi, "
                    + ".vsqx, .mei, .ly, .mscx, or .mscz.");
        } catch (Exception ex) {
            return failureResult("Failed to load file: " + ex.getMessage());
        }
    }

    public static CliResult decodeCliMusicXmlInput(byte[] inputBytes, String inputPath) {
        try {
            if (hasExtension(inputPath, ".mxl")) {
                return textResult(MxlIo.extractMusicXmlTextFromMxl(inputBytes));
            }
            return textResult(new String(inputBytes == null ? new byte[0] : inputBytes, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return failureResult("Failed to read MusicXML input: " + ex.getMessage());
        }
    }

    public static CliResult encodeCliMusicXmlOutput(String xmlText, String outputPath) {
        try {
            if (hasExtension(outputPath, ".mxl")) {
                return bytesResult(MxlIo.makeMxlBytes(xmlText));
            }
            return textResult(xmlText);
        } catch (Exception ex) {
            return failureResult("Failed to encode MusicXML output: " + ex.getMessage());
        }
    }

    public static CliResult decodeCliMuseScoreInput(byte[] inputBytes, String inputPath) {
        try {
            if (hasExtension(inputPath, ".mscz")) {
                return textResult(MxlIo.extractTextFromZipByExtensions(inputBytes, new String[] { ".mscx" }));
            }
            return textResult(new String(inputBytes == null ? new byte[0] : inputBytes, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return failureResult("Failed to read MuseScore input: " + ex.getMessage());
        }
    }

    public static CliResult encodeCliMuseScoreOutput(String musescoreText, String outputPath) {
        try {
            if (hasExtension(outputPath, ".mscz")) {
                return bytesResult(MxlIo.makeMsczBytes(musescoreText));
            }
            return textResult(musescoreText);
        } catch (Exception ex) {
            return failureResult("Failed to encode MuseScore output: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToAbc(String xmlText) {
        try {
            return textResult(AbcIo.musicXmlToAbc(xmlText));
        } catch (Exception ex) {
            return failureResult("Failed to export ABC: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToMei(String xmlText) {
        try {
            return textResult(MeiIo.exportMusicXmlDomToMei(MusicXmlIo.parseMusicXmlDocument(xmlText)));
        } catch (Exception ex) {
            return failureResult("Failed to export MEI: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToMidi(String xmlText) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (doc == null || doc.getDocumentElement() == null) {
                return failureResult("Failed to export MIDI: invalid MusicXML input.");
            }
            int ticksPerQuarter = 480;
            MidiIo.MidiPlaybackExtractionOptions options = new MidiIo.MidiPlaybackExtractionOptions("midi");
            MidiIo.MidiPlaybackEventsResult playback =
                    MidiIo.buildPlaybackEventsFromMusicXmlDoc(doc, ticksPerQuarter, options);
            if (playback.getEvents().isEmpty()) {
                return failureResult("Failed to export MIDI: no playable note events found.");
            }
            Map<String, Integer> programOverrides = MidiIo.collectMidiProgramOverridesFromMusicXmlDoc(doc);
            MidiIo.MidiExportPlaybackBuildResult result = MidiIo.buildMidiPlaybackExport(playback.getEvents(),
                    playback.getTempo(), "electric_piano_2", programOverrides,
                    MidiIo.collectMidiControlEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    MidiIo.collectMidiTempoEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    MidiIo.collectMidiKeySignatureEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    true, true, true, ticksPerQuarter, Collections.<String>emptyList(), false,
                    "off_before_on", musicXmlTitle(doc), musicXmlMovementTitle(doc), musicXmlComposer(doc),
                    MidiIo.collectLeadingPickupTicksFromMusicXmlDoc(doc, ticksPerQuarter));
            return bytesResult(result.getRawBytes());
        } catch (Exception ex) {
            return failureResult("Failed to export MIDI: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToMuseScore(String xmlText) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (!isScorePartwiseDocument(doc)) {
                return failureResult("Failed to export MuseScore: invalid MusicXML input.");
            }
            return textResult(MuseScoreIo.buildEmptyMuseScoreExportXml(480, musicXmlTitle(doc)));
        } catch (Exception ex) {
            return failureResult("Failed to export MuseScore: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToLilyPond(String xmlText) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (!isScorePartwiseDocument(doc)) {
                return failureResult("Failed to export LilyPond: invalid MusicXML input.");
            }
            return textResult(LilyPondIo.exportMusicXmlDomToLilyPond(doc));
        } catch (Exception ex) {
            return failureResult("Failed to export LilyPond: " + ex.getMessage());
        }
    }

    public static DownloadPayload createMusicXmlDownloadPayload(String xmlText, boolean compressed,
            boolean useXmlExtension) {
        String timestamp = buildFileTimestamp();
        String formattedXml = MusicXmlIo.prettyPrintMusicXmlText(xmlText);
        if (compressed) {
            return bytesDownloadPayload("mikuscore-" + timestamp + ".mxl", MxlIo.makeMxlBytes(formattedXml),
                    MIME_MXL);
        }
        String extension = useXmlExtension ? "xml" : "musicxml";
        return textDownloadPayload("mikuscore-" + timestamp + "." + extension, formattedXml, MIME_XML);
    }

    public static DownloadPayload createSvgDownloadPayload(String svgText) {
        return textTimestampedDownloadPayload("svg", svgText, MIME_SVG, "mikuscore");
    }

    public static DownloadPayload createJsonDownloadPayload(String jsonText, String stem) {
        String safeStem = stem == null || stem.trim().length() == 0 ? "measure-detail" : stem.trim();
        return textTimestampedDownloadPayload("json", jsonText, MIME_JSON, "mikuscore-" + safeStem);
    }

    public static DownloadPayload createVsqxDownloadPayload(String vsqxText) {
        return textTimestampedDownloadPayload("vsqx", formatXmlWithTwoSpaceIndent(vsqxText), MIME_XML, "mikuscore");
    }

    public static DownloadPayload createMidiDownloadPayload(String xmlText) {
        CliResult result = exportMusicXmlToMidi(xmlText);
        if (!result.isOk()) {
            return null;
        }
        return bytesTimestampedDownloadPayload("mid", result.getOutputBytes(), MIME_MIDI, "mikuscore");
    }

    public static DownloadPayload createAbcDownloadPayload(String xmlText) {
        CliResult result = exportMusicXmlToAbc(xmlText);
        if (!result.isOk()) {
            return null;
        }
        return textTimestampedDownloadPayload("abc", result.getOutput(), MIME_TEXT, "mikuscore");
    }

    public static DownloadPayload createMeiDownloadPayload(String xmlText) {
        CliResult result = exportMusicXmlToMei(xmlText);
        if (!result.isOk()) {
            return null;
        }
        return textTimestampedDownloadPayload("mei", MusicXmlIo.prettyPrintMusicXmlText(result.getOutput()),
                MIME_MEI, "mikuscore");
    }

    public static DownloadPayload createLilyPondDownloadPayload(String xmlText) {
        CliResult result = exportMusicXmlToLilyPond(xmlText);
        if (!result.isOk()) {
            return null;
        }
        return textTimestampedDownloadPayload("ly", result.getOutput(), MIME_TEXT, "mikuscore");
    }

    public static DownloadPayload createMuseScoreDownloadPayload(String xmlText, boolean compressed) {
        CliResult result = exportMusicXmlToMuseScore(xmlText);
        if (!result.isOk()) {
            return null;
        }
        String timestamp = buildFileTimestamp();
        String formattedMscx = formatXmlWithTwoSpaceIndent(result.getOutput());
        if (compressed) {
            return bytesDownloadPayload("mikuscore-" + timestamp + ".mscz", MxlIo.makeMsczBytes(formattedMscx),
                    MIME_ZIP);
        }
        return textDownloadPayload("mikuscore-" + timestamp + ".mscx", formattedMscx, MIME_XML);
    }

    public static CliResult summarizeMusicXmlState(String xmlText) {
        try {
            return textResult(MusicXmlState.summarizeMusicXmlState(xmlText).toJson());
        } catch (Exception ex) {
            return failureResult("Failed to summarize MusicXML state: " + ex.getMessage());
        }
    }

    public static CliResult inspectMusicXmlMeasure(String xmlText, String measureNumber) {
        try {
            return textResult(MusicXmlState.inspectMusicXmlMeasure(xmlText, measureNumber).toJson());
        } catch (Exception ex) {
            return failureResult("Failed to inspect MusicXML measure: " + ex.getMessage());
        }
    }

    public static CliResult validateMusicXmlCommand(String xmlText, String commandJson) {
        try {
            return textResult(MusicXmlState.validateMusicXmlCommand(xmlText, commandJson).toJson());
        } catch (Exception ex) {
            return failureResult("Failed to validate MusicXML command: " + ex.getMessage());
        }
    }

    public static CliResult applyMusicXmlCommand(String xmlText, String commandJson) {
        try {
            return textResult(MusicXmlState.applyMusicXmlCommand(xmlText, commandJson));
        } catch (Exception ex) {
            return failureResult("Failed to apply MusicXML command: " + ex.getMessage());
        }
    }

    public static CliResult diffMusicXmlState(String beforeXml, String afterXml) {
        try {
            return textResult(MusicXmlState.diffMusicXmlState(beforeXml, afterXml).toJson());
        } catch (Exception ex) {
            return failureResult("Failed to diff MusicXML state: " + ex.getMessage());
        }
    }

    private static CliResult textResult(String output) {
        return new CliResult(true, output, null, "");
    }

    private static CliResult bytesResult(byte[] outputBytes) {
        return new CliResult(true, "", outputBytes, "");
    }

    private static CliResult failureResult(String diagnostic) {
        return new CliResult(false, "", null, diagnostic);
    }

    private static DownloadPayload textTimestampedDownloadPayload(String extension, String content, String contentType,
            String stem) {
        return textDownloadPayload(stem + "-" + buildFileTimestamp() + "." + extension, content, contentType);
    }

    private static DownloadPayload bytesTimestampedDownloadPayload(String extension, byte[] content, String contentType,
            String stem) {
        return bytesDownloadPayload(stem + "-" + buildFileTimestamp() + "." + extension, content, contentType);
    }

    private static DownloadPayload textDownloadPayload(String fileName, String content, String contentType) {
        return new DownloadPayload(fileName, contentType, content == null ? "" : content, null);
    }

    private static DownloadPayload bytesDownloadPayload(String fileName, byte[] content, String contentType) {
        return new DownloadPayload(fileName, contentType, "", content == null ? new byte[0] : content);
    }

    private static String buildFileTimestamp() {
        return LocalDateTime.now().format(DOWNLOAD_TIMESTAMP_FORMAT);
    }

    private static String midiDiagnosticsText(MidiIo.MidiImportResult result) {
        if (result == null || result.getDiagnostics().isEmpty()) {
            return "unknown MIDI import failure.";
        }
        StringBuilder out = new StringBuilder();
        for (MidiIo.MidiImportDiagnostic diagnostic : result.getDiagnostics()) {
            if (out.length() > 0) {
                out.append("; ");
            }
            out.append(diagnostic.getCode()).append(": ").append(diagnostic.getMessage());
        }
        return out.toString();
    }

    private static boolean hasExtension(String path, String extension) {
        if (path == null) {
            return false;
        }
        return path.trim().toLowerCase(Locale.ROOT).endsWith(extension);
    }

    private static CliResult resolveMuseScoreZipLoad(byte[] bytes) {
        try {
            String mscxText = MxlIo.extractTextFromZipByExtensions(bytes, new String[] { ".mscx" });
            return importMuseScoreToMusicXml(mscxText);
        } catch (Exception ignored) {
            try {
                String extractedXml = MxlIo.extractMusicXmlTextFromMxl(bytes);
                if (looksLikeScorePartwise(extractedXml)) {
                    return textResult(MusicXmlIo.normalizeImportedMusicXmlText(extractedXml));
                }
                return importMuseScoreToMusicXml(extractedXml);
            } catch (Exception ex) {
                return failureResult("Failed to parse MuseScore: " + ex.getMessage());
            }
        }
    }

    private static boolean looksLikeScorePartwise(String xmlText) {
        return xmlText != null && xmlText.matches("(?is).*<\\s*score-partwise(?:\\s|>).*");
    }

    private static boolean isScorePartwiseDocument(Document doc) {
        return doc != null && doc.getDocumentElement() != null
                && "score-partwise".equals(doc.getDocumentElement().getTagName());
    }

    private static String formatXmlWithTwoSpaceIndent(String xml) {
        String compact = String.valueOf(xml == null ? "" : xml).replaceAll(">\\s+<", "><").trim();
        if (compact.length() == 0) {
            return "";
        }
        String[] split = compact.replaceAll("(>)(<)(/?)", "$1\n$2$3").split("\n");
        int indent = 0;
        StringBuilder out = new StringBuilder();
        for (String rawToken : split) {
            String token = rawToken.trim();
            if (token.length() == 0) {
                continue;
            }
            if (token.startsWith("</")) {
                indent = Math.max(0, indent - 1);
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            for (int index = 0; index < indent; index++) {
                out.append("  ");
            }
            out.append(token);
            boolean opening = token.matches("^<[^!?/][^>]*>$");
            boolean selfClosing = token.endsWith("/>");
            if (opening && !selfClosing) {
                indent++;
            }
        }
        return out.toString();
    }

    private static String convertSimpleMuseScoreToMusicXml(String musescoreText) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(musescoreText);
        if (doc == null || doc.getDocumentElement() == null) {
            throw new IllegalArgumentException("invalid MuseScore XML input.");
        }
        int divisions = positiveInt(firstElementText(doc, "Division"), 480);
        String title = museScoreMetaTag(doc, "workTitle");
        if (title.length() == 0) {
            title = "Untitled";
        }
        StringBuilder body = new StringBuilder();
        org.w3c.dom.NodeList voices = doc.getElementsByTagName("voice");
        org.w3c.dom.Element voice = voices.getLength() == 0 ? null : (org.w3c.dom.Element) voices.item(0);
        if (voice != null) {
            org.w3c.dom.Node child = voice.getFirstChild();
            while (child != null) {
                if (child instanceof org.w3c.dom.Element) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) child;
                    if ("Chord".equals(element.getTagName())) {
                        body.append(simpleMuseChordToMusicXmlNote(element, divisions));
                    } else if ("Rest".equals(element.getTagName())) {
                        body.append(simpleMuseRestToMusicXmlNote(element, divisions));
                    }
                }
                child = child.getNextSibling();
            }
        }
        if (body.length() == 0) {
            body.append("<note><rest/><duration>").append(divisions * 4)
                    .append("</duration><voice>1</voice><type>whole</type></note>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>"
                + xmlEscape(title)
                + "</work-title></work><part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>" + divisions
                + "</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>" + body.toString()
                + "</measure></part></score-partwise>";
    }

    private static String simpleMuseChordToMusicXmlNote(org.w3c.dom.Element chord, int divisions) {
        int duration = museDurationDiv(chord, divisions);
        int midi = positiveInt(firstElementText(chord, "pitch"), 60);
        AccidentalSpelling.SpelledPitch pitch = AccidentalSpelling.midiToPitch(midi, Integer.valueOf(0), "");
        String alterXml = pitch.getAlter() == 0 ? "" : "<alter>" + pitch.getAlter() + "</alter>";
        return "<note><pitch><step>" + pitch.getStep() + "</step>" + alterXml + "<octave>" + pitch.getOctave()
                + "</octave></pitch><duration>" + duration + "</duration><voice>1</voice><type>"
                + museDurationType(chord) + "</type></note>";
    }

    private static String simpleMuseRestToMusicXmlNote(org.w3c.dom.Element rest, int divisions) {
        int duration = museDurationDiv(rest, divisions);
        return "<note><rest/><duration>" + duration + "</duration><voice>1</voice><type>" + museDurationType(rest)
                + "</type></note>";
    }

    private static int museDurationDiv(org.w3c.dom.Element element, int divisions) {
        Integer duration = MuseScoreIo.durationTypeToDivisions(museDurationType(element), divisions);
        return duration == null ? divisions : duration.intValue();
    }

    private static String museDurationType(org.w3c.dom.Element element) {
        String type = firstElementText(element, "durationType");
        return type.length() == 0 ? "quarter" : type;
    }

    private static String museScoreMetaTag(Document doc, String name) {
        org.w3c.dom.NodeList nodes = doc.getElementsByTagName("metaTag");
        for (int index = 0; index < nodes.getLength(); index++) {
            org.w3c.dom.Node node = nodes.item(index);
            if (node instanceof org.w3c.dom.Element && name.equals(((org.w3c.dom.Element) node).getAttribute("name"))) {
                String text = node.getTextContent();
                return text == null ? "" : text.trim();
            }
        }
        return "";
    }

    private static int positiveInt(String text, int fallback) {
        try {
            int value = Integer.parseInt(text == null ? "" : text.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String musicXmlTitle(Document doc) {
        String workTitle = firstElementText(doc, "work-title");
        if (!workTitle.isEmpty()) {
            return workTitle;
        }
        return musicXmlMovementTitle(doc);
    }

    private static String musicXmlMovementTitle(Document doc) {
        return firstElementText(doc, "movement-title");
    }

    private static String musicXmlComposer(Document doc) {
        if (doc == null) {
            return "";
        }
        org.w3c.dom.NodeList creators = doc.getElementsByTagName("creator");
        for (int index = 0; index < creators.getLength(); index++) {
            org.w3c.dom.Node node = creators.item(index);
            if (node instanceof org.w3c.dom.Element) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                if ("composer".equals(element.getAttribute("type"))) {
                    return element.getTextContent() == null ? "" : element.getTextContent().trim();
                }
            }
        }
        return firstElementText(doc, "creator");
    }

    private static String firstElementText(Document doc, String tagName) {
        if (doc == null || tagName == null) {
            return "";
        }
        org.w3c.dom.NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    private static String firstElementText(org.w3c.dom.Element parent, String tagName) {
        if (parent == null || tagName == null) {
            return "";
        }
        org.w3c.dom.NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static final class CliResult {
        private final boolean ok;
        private final String output;
        private final byte[] outputBytes;
        private final String diagnostic;

        private CliResult(boolean ok, String output, byte[] outputBytes, String diagnostic) {
            this.ok = ok;
            this.output = output == null ? "" : output;
            this.outputBytes = outputBytes == null ? null : outputBytes.clone();
            this.diagnostic = diagnostic == null ? "" : diagnostic;
        }

        public boolean isOk() {
            return ok;
        }

        public String getOutput() {
            return output;
        }

        public byte[] getOutputBytes() {
            return outputBytes == null ? output.getBytes(StandardCharsets.UTF_8) : outputBytes.clone();
        }

        public String getDiagnostic() {
            return diagnostic;
        }
    }

    public static final class DownloadPayload {
        private final String fileName;
        private final String contentType;
        private final String text;
        private final byte[] bytes;

        private DownloadPayload(String fileName, String contentType, String text, byte[] bytes) {
            this.fileName = fileName == null ? "" : fileName;
            this.contentType = contentType == null ? "" : contentType;
            this.text = text == null ? "" : text;
            this.bytes = bytes == null ? null : bytes.clone();
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public String getText() {
            return text;
        }

        public byte[] getBytes() {
            return bytes == null ? text.getBytes(StandardCharsets.UTF_8) : bytes.clone();
        }
    }
}
