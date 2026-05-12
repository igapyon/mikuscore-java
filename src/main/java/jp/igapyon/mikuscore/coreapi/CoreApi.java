package jp.igapyon.mikuscore.coreapi;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Document;

import jp.igapyon.mikuscore.abc.AbcIo;
import jp.igapyon.mikuscore.lilypond.LilyPondIo;
import jp.igapyon.mikuscore.mei.MeiIo;
import jp.igapyon.mikuscore.midi.MidiIo;
import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.musicxml.MusicXmlState;
import jp.igapyon.mikuscore.musicxml.MxlIo;

/**
 * Minimal public core API placeholder for the straight-conversion foundation.
 */
public final class CoreApi {
    private CoreApi() {
    }

    public static String version() {
        Package pkg = CoreApi.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return version == null ? "0.5.0" : version;
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
}
