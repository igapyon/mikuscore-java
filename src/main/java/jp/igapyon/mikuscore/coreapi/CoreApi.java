package jp.igapyon.mikuscore.coreapi;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Document;

import jp.igapyon.mikuscore.abc.AbcIo;
import jp.igapyon.mikuscore.lilypond.LilyPondIo;
import jp.igapyon.mikuscore.mei.MeiIo;
import jp.igapyon.mikuscore.midi.MidiIo;
import jp.igapyon.mikuscore.musescore.MuseScoreIo;
import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.musicxml.MusicXmlCommandValidation;
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
        return version == null ? "0.6.1" : version;
    }

    public static CliResult importAbcToMusicXml(String abcText) {
        try {
            return textResult(MusicXmlIo.normalizeImportedMusicXmlText(
                    AbcIo.musicXmlFromAbc(abcText, new AbcIo.AbcImportOptions())));
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
            return textResult(MusicXmlIo.normalizeImportedMusicXmlText(
                    LilyPondIo.convertLilyPondToMusicXml(lilyPondText)));
        } catch (Exception ex) {
            return failureResult("Failed to parse LilyPond: " + ex.getMessage());
        }
    }

    public static CliResult importMidiToMusicXml(byte[] midiBytes) {
        try {
            MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midiBytes, new MidiIo.MidiImportOptions());
            List<String> warnings = midiDiagnosticMessages(result == null ? null : result.getWarnings());
            List<String> diagnostics = midiDiagnosticMessages(result == null ? null : result.getDiagnostics());
            if (!result.isOk()) {
                return failureResult(diagnostics, warnings);
            }
            return textResult(MusicXmlIo.normalizeImportedMusicXmlText(result.getXml()), warnings, diagnostics);
        } catch (Exception ex) {
            return failureResult("Failed to parse MIDI: " + ex.getMessage());
        }
    }

    public static CliResult importMuseScoreToMusicXml(String musescoreText) {
        return importMuseScoreToMusicXml(musescoreText, true, true, false, true);
    }

    /**
     * Imports MSCX with the runtime-independent MuseScore import options.
     */
    public static CliResult importMuseScoreToMusicXml(String musescoreText, boolean sourceMetadata,
            boolean debugMetadata, boolean normalizeCutTimeToTwoTwo, boolean applyImplicitBeams) {
        try {
            return textResult(MusicXmlIo.normalizeImportedMusicXmlText(
                    MuseScoreIo.convertMuseScoreToMusicXml(musescoreText, sourceMetadata, debugMetadata,
                            normalizeCutTimeToTwoTwo, applyImplicitBeams)));
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
                try {
                    return textResult(MusicXmlIo.normalizeImportedMusicXmlText(
                            MxlIo.extractMusicXmlTextFromMxl(bytes)));
                } catch (Exception ex) {
                    return failureResult("Failed to parse MXL: " + ex.getMessage());
                }
            }
            if (hasExtension(inputPath, ".musicxml") || hasExtension(inputPath, ".xml")) {
                return textResult(MusicXmlIo.normalizeImportedMusicXmlText(text));
            }
            if (hasExtension(inputPath, ".abc")) {
                return importAbcToMusicXml(text);
            }
            if (hasExtension(inputPath, ".mid") || hasExtension(inputPath, ".midi")) {
                return resolveLoadMidiToMusicXml(bytes);
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
                try {
                    return resolveMuseScoreZipLoad(bytes);
                } catch (Exception ex) {
                    return failureResult("Failed to parse MuseScore: " + ex.getMessage());
                }
            }
            return failureResult("Unsupported file extension. Use .musicxml, .xml, .mxl, .abc, .mid, .midi, "
                    + ".vsqx, .mei, .ly, .mscx, or .mscz.");
        } catch (Exception ex) {
            return failureResult("Failed to load file: " + ex.getMessage());
        }
    }

    /**
     * Value-based counterpart of the Node {@code load-input.ts} conversion
     * layer. It deliberately accepts {@code Object} so Java callers can also
     * receive the source's declared-format/payload-kind diagnostic instead of
     * relying on a Java overload mismatch.
     */
    public static LoadInputResult convertLoadInputToMusicXml(String format, Object data) {
        String requestedFormat = String.valueOf(format);
        if ("musicxml".equals(requestedFormat)) {
            String text = requireLoadText(requestedFormat, data);
            if (text == null) {
                return loadInputFailure("Expected text input for " + requestedFormat + ".");
            }
            try {
                return loadInputSuccess(MusicXmlIo.normalizeImportedMusicXmlText(text), Collections.<LoadInputDiagnostic>emptyList(),
                        Collections.<LoadInputDiagnostic>emptyList());
            } catch (Exception ex) {
                return loadInputFailure("Failed to parse MusicXML: " + ex.getMessage());
            }
        }
        if ("mxl".equals(requestedFormat)) {
            byte[] bytes = requireLoadBytes(requestedFormat, data);
            if (bytes == null) {
                return loadInputFailure("Expected binary input for " + requestedFormat + ".");
            }
            try {
                return loadInputSuccess(MusicXmlIo.normalizeImportedMusicXmlText(MxlIo.extractMusicXmlTextFromMxl(bytes)),
                        Collections.<LoadInputDiagnostic>emptyList(), Collections.<LoadInputDiagnostic>emptyList());
            } catch (Exception ex) {
                return loadInputFailure("Failed to parse MXL: " + ex.getMessage());
            }
        }
        if ("midi".equals(requestedFormat)) {
            byte[] bytes = requireLoadBytes(requestedFormat, data);
            if (bytes == null) {
                return loadInputFailure("Expected binary input for " + requestedFormat + ".");
            }
            try {
                MidiIo.MidiImportResult converted = MidiIo.convertMidiToMusicXml(bytes, new MidiIo.MidiImportOptions());
                List<LoadInputDiagnostic> diagnostics = loadInputDiagnostics(
                        converted == null ? null : converted.getDiagnostics());
                List<LoadInputDiagnostic> warnings = loadInputDiagnostics(
                        converted == null ? null : converted.getWarnings());
                if (converted == null || !converted.isOk()) {
                    LoadInputDiagnostic first = diagnostics.isEmpty() ? null : diagnostics.get(0);
                    String detail = first == null ? "Unknown parse error."
                            : first.getMessage() + " (" + first.getCode() + ")";
                    return loadInputFailure("Failed to parse MIDI: " + detail, diagnostics, warnings);
                }
                return loadInputSuccess(MusicXmlIo.normalizeImportedMusicXmlText(converted.getXml()), diagnostics, warnings);
            } catch (Exception ex) {
                return loadInputFailure("Failed to parse MIDI: " + ex.getMessage());
            }
        }
        if ("mscz".equals(requestedFormat)) {
            byte[] bytes = requireLoadBytes(requestedFormat, data);
            if (bytes == null) {
                return loadInputFailure("Expected binary input for " + requestedFormat + ".");
            }
            CliResult result = resolveMuseScoreZipLoad(bytes);
            return result.isOk()
                    ? loadInputSuccess(result.getOutput(), Collections.<LoadInputDiagnostic>emptyList(),
                            Collections.<LoadInputDiagnostic>emptyList())
                    : loadInputFailure(result.getDiagnostic());
        }
        String text = requireLoadText(requestedFormat, data);
        if (text == null) {
            return loadInputFailure("Expected text input for " + requestedFormat + ".");
        }
        CliResult result;
        if ("abc".equals(requestedFormat)) {
            result = importAbcToMusicXml(text);
        } else if ("mei".equals(requestedFormat)) {
            result = importMeiToMusicXml(text);
        } else if ("lilypond".equals(requestedFormat)) {
            result = importLilyPondToMusicXml(text);
        } else if ("musescore".equals(requestedFormat)) {
            result = importMuseScoreToMusicXml(text);
        } else if ("vsqx".equals(requestedFormat)) {
            return loadInputFailure("Failed to parse VSQX: VSQX import is not available in this Java core API.");
        } else {
            return loadInputFailure("Unsupported input format: " + requestedFormat);
        }
        return result.isOk()
                ? loadInputSuccess(result.getOutput(), Collections.<LoadInputDiagnostic>emptyList(),
                        Collections.<LoadInputDiagnostic>emptyList())
                : loadInputFailure(result.getDiagnostic());
    }

    /**
     * Resolves the non-file source selector used by {@code load-flow.ts}.
     * The caller supplies the selected source text, avoiding browser UI state
     * while retaining the source result shape.
     */
    public static LoadFlowResult resolveDirectLoadFlow(boolean isNewType, String sourceType, String sourceText,
            String newMusicXml) {
        if (isNewType) {
            String xml = newMusicXml == null ? "" : newMusicXml;
            return LoadFlowResult.success(xml, null);
        }
        String type = sourceType == null ? "" : sourceType;
        String format;
        if ("xml".equals(type)) {
            format = "musicxml";
        } else if ("abc".equals(type)) {
            format = "abc";
        } else if ("mei".equals(type)) {
            format = "mei";
        } else if ("lilypond".equals(type)) {
            format = "lilypond";
        } else if ("musescore".equals(type)) {
            format = "musescore";
        } else if ("vsqx".equals(type)) {
            format = "vsqx";
        } else {
            format = type;
        }
        String selectedText = sourceText == null ? "" : sourceText;
        LoadInputResult converted = convertLoadInputToMusicXml(format, selectedText);
        if (!converted.isOk()) {
            return LoadFlowResult.failure(converted.getDiagnosticCode(), converted.getDiagnosticMessage());
        }
        return LoadFlowResult.success(converted.getXml(), "abc".equals(type) ? selectedText : null);
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
                return bytesResult(MxlIo.makeMsczBytes(formatXmlWithTwoSpaceIndent(musescoreText)));
            }
            return textResult(musescoreText);
        } catch (Exception ex) {
            return failureResult("Failed to encode MuseScore output: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToAbc(String xmlText) {
        if (!isScorePartwiseDocument(MusicXmlIo.parseMusicXmlDocument(xmlText))) {
            return invalidMusicXmlResult();
        }
        try {
            return textResult(AbcIo.musicXmlToAbc(xmlText));
        } catch (Exception ex) {
            return failureResult("Failed to export ABC: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToMei(String xmlText) {
        return exportMusicXmlToMei(xmlText, null);
    }

    /** Exports MusicXML using the upstream MEI-version normalization option. */
    public static CliResult exportMusicXmlToMei(String xmlText, String meiVersion) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (!isScorePartwiseDocument(doc)) {
                return invalidMusicXmlResult();
            }
            return textResult(MeiIo.exportMusicXmlDomToMei(doc, meiVersion));
        } catch (Exception ex) {
            return failureResult("Failed to export MEI: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToMidi(String xmlText) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (!isScorePartwiseDocument(doc)) {
                return invalidMusicXmlResult();
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
        return exportMusicXmlToMuseScore(xmlText, false);
    }

    /**
     * Exports MusicXML to MSCX with the runtime-independent cut-time option.
     */
    public static CliResult exportMusicXmlToMuseScore(String xmlText, boolean normalizeCutTimeToTwoTwo) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (!isScorePartwiseDocument(doc)) {
                return invalidMusicXmlResult();
            }
            return textResult(MuseScoreIo.exportMusicXmlDomToMuseScore(doc, normalizeCutTimeToTwoTwo));
        } catch (Exception ex) {
            return failureResult("Failed to export MuseScore: " + ex.getMessage());
        }
    }

    public static CliResult exportMusicXmlToLilyPond(String xmlText) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (!isScorePartwiseDocument(doc)) {
                return invalidMusicXmlResult();
            }
            return textResult(LilyPondIo.exportMusicXmlDomToLilyPond(doc));
        } catch (Exception ex) {
            return failureResult("Failed to export LilyPond: " + ex.getMessage());
        }
    }

    /**
     * Value-based output encoding boundary corresponding to the Node
     * {@code output-encoding.ts} module. Filename, Blob, and download policy
     * deliberately stay in the separate download-payload facade.
     */
    public static EncodedOutput encodeMusicXmlOutput(String xmlText, boolean compressed) {
        String formattedXml = MusicXmlIo.prettyPrintMusicXmlText(xmlText);
        return compressed ? EncodedOutput.bytes(MxlIo.makeMxlBytes(formattedXml)) : EncodedOutput.text(formattedXml);
    }

    public static EncodedOutput encodeMusicXmlOutput(String xmlText) {
        return encodeMusicXmlOutput(xmlText, false);
    }

    public static String encodeSvgOutput(String svgText) {
        return svgText == null ? "" : svgText;
    }

    public static String encodeJsonOutput(String jsonText) {
        return jsonText == null ? "" : jsonText;
    }

    /** Encodes a SMF byte stream for the supported value-based MIDI output options. */
    public static byte[] encodeMidiOutput(String xmlText, MidiOutputOptions options) {
        try {
            Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
            if (!isScorePartwiseDocument(doc)) {
                return null;
            }
            MidiOutputOptions safe = options == null ? new MidiOutputOptions(480d) : options;
            MidiIo.MidiExportRuntimeOptions runtime = MidiIo.resolveMidiExportRuntimeOptions(
                    safe.getExportProfile(), safe.getTicksPerQuarter());
            int ticksPerQuarter = runtime.getTicksPerQuarter();
            MidiIo.MidiPlaybackExtractionOptions playbackOptions = new MidiIo.MidiPlaybackExtractionOptions(
                    MidiIo.resolvePlaybackBuildModeForMidiExport(runtime.getEventBuildPolicy()),
                    safe.getGraceTimingMode(), safe.isMetricAccentEnabled(), safe.getMetricAccentProfile(), null,
                    runtime.isIncludeGraceInPlaybackLikeMode(), runtime.isIncludeOrnamentInPlaybackLikeMode(),
                    runtime.isIncludeTieInPlaybackLikeMode());
            MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromMusicXmlDoc(doc,
                    ticksPerQuarter, playbackOptions);
            if (playback.getEvents().isEmpty()) {
                return null;
            }
            Map<String, Integer> programOverrides = safe.isForceProgramPreset()
                    ? new LinkedHashMap<String, Integer>() : MidiIo.collectMidiProgramOverridesFromMusicXmlDoc(doc);
            Boolean requestedRawWriter = safe.getRawWriter();
            boolean rawWriter = requestedRawWriter == null ? runtime.isRawWriter()
                    : requestedRawWriter.booleanValue();
            MidiIo.MidiExportPlaybackBuildResult result = MidiIo.buildMidiPlaybackExport(playback.getEvents(),
                    playback.getTempo(), safe.getProgramPreset(), programOverrides,
                    MidiIo.collectMidiControlEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    MidiIo.collectMidiTempoEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    MidiIo.collectMidiKeySignatureEventsFromMusicXmlDoc(doc, ticksPerQuarter),
                    rawWriter, true, safe.isKeepRoundtripMetadata(), ticksPerQuarter,
                    Collections.<String>emptyList(), runtime.isNormalizeForParity(), runtime.getRawRetriggerPolicy(),
                    musicXmlTitle(doc),
                    musicXmlMovementTitle(doc), musicXmlComposer(doc),
                    MidiIo.collectLeadingPickupTicksFromMusicXmlDoc(doc, ticksPerQuarter));
            return result.isRawWriter() ? result.getRawBytes()
                    : MidiIo.buildMidiWriterCompatibleBytes(result.getWriterTrackPlan(), ticksPerQuarter);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Converts MusicXML to ABC without a browser callback boundary. */
    public static String encodeAbcOutput(String xmlText) {
        CliResult result = exportMusicXmlToAbc(xmlText);
        return result.isOk() ? result.getOutput() : null;
    }

    /** Converts MusicXML to MEI and applies the source output pretty-print step. */
    public static String encodeMeiOutput(String xmlText, String meiVersion) {
        CliResult result = exportMusicXmlToMei(xmlText, meiVersion);
        return result.isOk() ? MusicXmlIo.prettyPrintMusicXmlText(result.getOutput()) : null;
    }

    public static String encodeMeiOutput(String xmlText) {
        return encodeMeiOutput(xmlText, null);
    }

    public static String encodeLilyPondOutput(String xmlText) {
        CliResult result = exportMusicXmlToLilyPond(xmlText);
        return result.isOk() ? result.getOutput() : null;
    }

    public static EncodedOutput encodeMuseScoreOutput(String xmlText, boolean compressed) {
        CliResult result = exportMusicXmlToMuseScore(xmlText);
        if (!result.isOk()) {
            return null;
        }
        String formattedMscx = formatXmlWithTwoSpaceIndent(result.getOutput());
        return compressed ? EncodedOutput.bytes(MxlIo.makeMsczBytes(formattedMscx))
                : EncodedOutput.text(formattedMscx);
    }

    public static EncodedOutput encodeMuseScoreOutput(String xmlText) {
        return encodeMuseScoreOutput(xmlText, false);
    }

    /** Builds a ZIP from ordered text and byte values without Blob handling. */
    public static byte[] encodeZipBundleOutput(List<OutputArchiveEntry> entries, boolean compressed) {
        List<MxlIo.ZipEntryPayload> zipEntries = new ArrayList<MxlIo.ZipEntryPayload>();
        if (entries != null) {
            for (OutputArchiveEntry entry : entries) {
                if (entry == null) {
                    continue;
                }
                String path = entry.getPath() == null ? "" : entry.getPath().trim();
                if (path.length() == 0) {
                    continue;
                }
                zipEntries.add(new MxlIo.ZipEntryPayload(path, entry.getBytes()));
            }
        }
        return MxlIo.makeZipBytes(zipEntries, compressed);
    }

    public static byte[] encodeZipBundleOutput(List<OutputArchiveEntry> entries) {
        return encodeZipBundleOutput(entries, true);
    }

    public static DownloadPayload createMusicXmlDownloadPayload(String xmlText, boolean compressed,
            boolean useXmlExtension) {
        String timestamp = buildFileTimestamp();
        String formattedXml = MusicXmlIo.prettyPrintMusicXmlText(xmlText);
        if (compressed) {
            return bytesDownloadPayload("miku-score-" + timestamp + ".mxl", MxlIo.makeMxlBytes(formattedXml),
                    MIME_MXL);
        }
        String extension = useXmlExtension ? "xml" : "musicxml";
        return textDownloadPayload("miku-score-" + timestamp + "." + extension, formattedXml, MIME_XML);
    }

    public static DownloadPayload createSvgDownloadPayload(String svgText) {
        return textTimestampedDownloadPayload("svg", svgText, MIME_SVG, "miku-score");
    }

    public static DownloadPayload createJsonDownloadPayload(String jsonText, String stem) {
        return textTimestampedDownloadPayload("json", jsonText, MIME_JSON,
                "miku-score-" + String.valueOf(stem));
    }

    /**
     * Creates the default JSON detail payload, corresponding to the optional
     * Node {@code stem = "measure-detail"} argument.
     */
    public static DownloadPayload createJsonDownloadPayload(String jsonText) {
        return createJsonDownloadPayload(jsonText, "measure-detail");
    }

    public static DownloadPayload createVsqxDownloadPayload(String vsqxText) {
        return textTimestampedDownloadPayload("vsqx", formatXmlWithTwoSpaceIndent(vsqxText), MIME_XML, "miku-score");
    }

    public static DownloadPayload createMidiDownloadPayload(String xmlText) {
        return createMidiDownloadPayload(xmlText, new MidiOutputOptions(480d));
    }

    /**
     * Creates the MIDI download payload with the source runtime options. The
     * byte encoding is shared with {@link #encodeMidiOutput(String, MidiOutputOptions)}.
     */
    public static DownloadPayload createMidiDownloadPayload(String xmlText, MidiOutputOptions options) {
        byte[] bytes = encodeMidiOutput(xmlText, options);
        if (bytes == null) {
            return null;
        }
        return bytesTimestampedDownloadPayload("mid", bytes, MIME_MIDI, "miku-score");
    }

    public static DownloadPayload createAbcDownloadPayload(String xmlText) {
        CliResult result = exportMusicXmlToAbc(xmlText);
        if (!result.isOk()) {
            return null;
        }
        return textTimestampedDownloadPayload("abc", result.getOutput(), MIME_TEXT, "miku-score");
    }

    public static DownloadPayload createMeiDownloadPayload(String xmlText) {
        return createMeiDownloadPayload(xmlText, null);
    }

    /** Creates an MEI download payload with the upstream {@code meiVersion} option. */
    public static DownloadPayload createMeiDownloadPayload(String xmlText, String meiVersion) {
        CliResult result = exportMusicXmlToMei(xmlText, meiVersion);
        if (!result.isOk()) {
            return null;
        }
        return textTimestampedDownloadPayload("mei", MusicXmlIo.prettyPrintMusicXmlText(result.getOutput()),
                MIME_MEI, "miku-score");
    }

    public static DownloadPayload createLilyPondDownloadPayload(String xmlText) {
        CliResult result = exportMusicXmlToLilyPond(xmlText);
        if (!result.isOk()) {
            return null;
        }
        return textTimestampedDownloadPayload("ly", result.getOutput(), MIME_TEXT, "miku-score");
    }

    public static DownloadPayload createMuseScoreDownloadPayload(String xmlText, boolean compressed) {
        CliResult result = exportMusicXmlToMuseScore(xmlText);
        if (!result.isOk()) {
            return null;
        }
        String timestamp = buildFileTimestamp();
        String formattedMscx = formatXmlWithTwoSpaceIndent(result.getOutput());
        if (compressed) {
            return bytesDownloadPayload("miku-score-" + timestamp + ".mscz", MxlIo.makeMsczBytes(formattedMscx),
                    MIME_ZIP);
        }
        return textDownloadPayload("miku-score-" + timestamp + ".mscx", formattedMscx, MIME_XML);
    }

    /**
     * Builds the runtime-independent ZIP bundle payload. Browser object URL
     * creation and download triggering intentionally remain outside this API.
     */
    public static DownloadPayload createZipBundleDownloadPayload(List<ZipBundleEntry> entries, String baseName,
            boolean compressed) {
        String safeBase = baseName == null || baseName.trim().length() == 0 ? "miku-score-all" : baseName.trim();
        List<MxlIo.ZipEntryPayload> zipEntries = new ArrayList<MxlIo.ZipEntryPayload>();
        if (entries != null) {
            for (ZipBundleEntry entry : entries) {
                if (entry == null) {
                    continue;
                }
                String fileName = entry.getFileName() == null ? "" : entry.getFileName().trim();
                if (fileName.length() == 0) {
                    continue;
                }
                zipEntries.add(new MxlIo.ZipEntryPayload(fileName, entry.getBytes()));
            }
        }
        return bytesDownloadPayload(safeBase + "-" + buildFileTimestamp() + ".zip",
                MxlIo.makeZipBytes(zipEntries, compressed), MIME_ZIP);
    }

    /** Creates a ZIP bundle using the upstream defaults. */
    public static DownloadPayload createZipBundleDownloadPayload(List<ZipBundleEntry> entries) {
        return createZipBundleDownloadPayload(entries, "miku-score-all", true);
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
            MusicXmlState.CliCommandNormalization normalized =
                    MusicXmlState.normalizeCliCommandSelectors(xmlText, commandJson);
            if (!normalized.isOk()) {
                return failureResult(normalized.getMessage());
            }
            return textResult(MusicXmlState.validateMusicXmlCommand(xmlText, normalized.getCommandJson()).toJson());
        } catch (Exception ex) {
            return failureResult("Failed to validate MusicXML command: " + ex.getMessage());
        }
    }

    public static CliResult applyMusicXmlCommand(String xmlText, String commandJson) {
        try {
            MusicXmlState.CliCommandNormalization normalized =
                    MusicXmlState.normalizeCliCommandSelectors(xmlText, commandJson);
            if (!normalized.isOk()) {
                return failureResult(normalized.getMessage());
            }
            MusicXmlState.MusicXmlCommandApplyResult applied =
                    MusicXmlState.applyMusicXmlCommandWithWarnings(xmlText, normalized.getCommandJson());
            List<String> warnings = new ArrayList<String>();
            for (MusicXmlCommandValidation.Warning warning : applied.getWarnings()) {
                warnings.add(warning.getMessage());
            }
            return textResult(applied.getOutput(), warnings);
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

    private static CliResult textResult(String output, List<String> warnings) {
        return new CliResult(true, output, null, "", warnings);
    }

    private static CliResult textResult(String output, List<String> warnings, List<String> diagnostics) {
        return new CliResult(true, output, null, diagnostics, warnings);
    }

    private static CliResult bytesResult(byte[] outputBytes) {
        return new CliResult(true, "", outputBytes, "");
    }

    private static CliResult failureResult(String diagnostic) {
        return new CliResult(false, "", null, diagnostic);
    }

    private static CliResult failureResult(List<String> diagnostics, List<String> warnings) {
        return new CliResult(false, "", null, diagnostics, warnings);
    }

    private static CliResult invalidMusicXmlResult() {
        return failureResult("Failed to parse MusicXML: input is not a valid MusicXML document.");
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

    private static List<String> midiDiagnosticMessages(List<MidiIo.MidiImportDiagnostic> diagnostics) {
        List<String> messages = new ArrayList<String>();
        if (diagnostics != null) {
            for (MidiIo.MidiImportDiagnostic diagnostic : diagnostics) {
                if (diagnostic != null) {
                    messages.add(diagnostic.getMessage());
                }
            }
        }
        return messages;
    }

    private static String requireLoadText(String format, Object data) {
        return data instanceof String ? (String) data : null;
    }

    private static byte[] requireLoadBytes(String format, Object data) {
        return data instanceof byte[] ? ((byte[]) data).clone() : null;
    }

    private static List<LoadInputDiagnostic> loadInputDiagnostics(List<MidiIo.MidiImportDiagnostic> values) {
        List<LoadInputDiagnostic> result = new ArrayList<LoadInputDiagnostic>();
        if (values != null) {
            for (MidiIo.MidiImportDiagnostic value : values) {
                if (value != null) {
                    result.add(new LoadInputDiagnostic(value.getCode(), value.getMessage()));
                }
            }
        }
        return result;
    }

    private static LoadInputResult loadInputSuccess(String xml, List<LoadInputDiagnostic> diagnostics,
            List<LoadInputDiagnostic> warnings) {
        return new LoadInputResult(true, xml, "", "", diagnostics, warnings);
    }

    private static LoadInputResult loadInputFailure(String message) {
        return loadInputFailure(message, Collections.<LoadInputDiagnostic>emptyList(),
                Collections.<LoadInputDiagnostic>emptyList());
    }

    private static LoadInputResult loadInputFailure(String message, List<LoadInputDiagnostic> diagnostics,
            List<LoadInputDiagnostic> warnings) {
        String safeMessage = message == null ? "" : message;
        List<LoadInputDiagnostic> safeDiagnostics = diagnostics == null
                ? Collections.<LoadInputDiagnostic>emptyList()
                : diagnostics;
        if (safeDiagnostics.isEmpty()) {
            safeDiagnostics = Collections.singletonList(new LoadInputDiagnostic("MVP_INVALID_COMMAND_PAYLOAD", safeMessage));
        }
        return new LoadInputResult(false, "", "MVP_INVALID_COMMAND_PAYLOAD", safeMessage, safeDiagnostics, warnings);
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

    /** Mirrors load-flow.ts, whose MIDI file-load diagnostic includes the first diagnostic code. */
    private static CliResult resolveLoadMidiToMusicXml(byte[] inputBytes) {
        try {
            MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(inputBytes, new MidiIo.MidiImportOptions());
            if (result == null || !result.isOk()) {
                MidiIo.MidiImportDiagnostic first = result == null || result.getDiagnostics().isEmpty() ? null
                        : result.getDiagnostics().get(0);
                String detail = first == null ? "Unknown parse error."
                        : first.getMessage() + " (" + first.getCode() + ")";
                return failureResult("Failed to parse MIDI: " + detail);
            }
            return textResult(MusicXmlIo.normalizeImportedMusicXmlText(result.getXml()),
                    midiDiagnosticMessages(result.getWarnings()), midiDiagnosticMessages(result.getDiagnostics()));
        } catch (Exception ex) {
            return failureResult("Failed to parse MIDI: " + ex.getMessage());
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
        private final List<String> diagnostics;
        private final List<String> warnings;

        private CliResult(boolean ok, String output, byte[] outputBytes, String diagnostic) {
            this(ok, output, outputBytes,
                    diagnostic == null || diagnostic.length() == 0 ? Collections.<String>emptyList()
                            : Collections.singletonList(diagnostic),
                    Collections.<String>emptyList());
        }

        private CliResult(boolean ok, String output, byte[] outputBytes, String diagnostic, List<String> warnings) {
            this(ok, output, outputBytes,
                    diagnostic == null || diagnostic.length() == 0 ? Collections.<String>emptyList()
                            : Collections.singletonList(diagnostic),
                    warnings);
        }

        private CliResult(boolean ok, String output, byte[] outputBytes, List<String> diagnostics,
                List<String> warnings) {
            this.ok = ok;
            this.output = output == null ? "" : output;
            this.outputBytes = outputBytes == null ? null : outputBytes.clone();
            this.diagnostics = diagnostics == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(diagnostics));
            this.warnings = warnings == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(warnings));
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
            return diagnostics.isEmpty() ? "" : diagnostics.get(0);
        }

        /** Messages corresponding to the upstream {@code CliResult.diagnostics} array. */
        public List<String> getDiagnostics() {
            return diagnostics;
        }

        /**
         * Non-fatal diagnostics emitted by a successful operation.
         */
        public List<String> getWarnings() {
            return warnings;
        }
    }

    /** One structured diagnostic in the value-based load-input facade. */
    public static final class LoadInputDiagnostic {
        private final String code;
        private final String message;

        public LoadInputDiagnostic(String code, String message) {
            this.code = code == null ? "" : code;
            this.message = message == null ? "" : message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    /** Result shape corresponding to {@code convertLoadInputToMusicXml}. */
    public static final class LoadInputResult {
        private final boolean ok;
        private final String xml;
        private final String diagnosticCode;
        private final String diagnosticMessage;
        private final List<LoadInputDiagnostic> diagnostics;
        private final List<LoadInputDiagnostic> warnings;

        private LoadInputResult(boolean ok, String xml, String diagnosticCode, String diagnosticMessage,
                List<LoadInputDiagnostic> diagnostics, List<LoadInputDiagnostic> warnings) {
            this.ok = ok;
            this.xml = xml == null ? "" : xml;
            this.diagnosticCode = diagnosticCode == null ? "" : diagnosticCode;
            this.diagnosticMessage = diagnosticMessage == null ? "" : diagnosticMessage;
            this.diagnostics = diagnostics == null ? Collections.<LoadInputDiagnostic>emptyList()
                    : Collections.unmodifiableList(new ArrayList<LoadInputDiagnostic>(diagnostics));
            this.warnings = warnings == null ? Collections.<LoadInputDiagnostic>emptyList()
                    : Collections.unmodifiableList(new ArrayList<LoadInputDiagnostic>(warnings));
        }

        public boolean isOk() {
            return ok;
        }

        public String getXml() {
            return xml;
        }

        public String getDiagnosticCode() {
            return diagnosticCode;
        }

        public String getDiagnosticMessage() {
            return diagnosticMessage;
        }

        public List<LoadInputDiagnostic> getDiagnostics() {
            return diagnostics;
        }

        public List<LoadInputDiagnostic> getWarnings() {
            return warnings;
        }
    }

    /** Result shape corresponding to the browser-independent load-flow branch. */
    public static final class LoadFlowResult {
        private final boolean ok;
        private final String xmlToLoad;
        private final boolean collapseInputSection;
        private final String nextXmlInputText;
        private final String nextAbcInputText;
        private final String diagnosticCode;
        private final String diagnosticMessage;

        private LoadFlowResult(boolean ok, String xmlToLoad, boolean collapseInputSection, String nextXmlInputText,
                String nextAbcInputText, String diagnosticCode, String diagnosticMessage) {
            this.ok = ok;
            this.xmlToLoad = xmlToLoad == null ? "" : xmlToLoad;
            this.collapseInputSection = collapseInputSection;
            this.nextXmlInputText = nextXmlInputText;
            this.nextAbcInputText = nextAbcInputText;
            this.diagnosticCode = diagnosticCode == null ? "" : diagnosticCode;
            this.diagnosticMessage = diagnosticMessage == null ? "" : diagnosticMessage;
        }

        private static LoadFlowResult success(String xml, String nextAbcText) {
            return new LoadFlowResult(true, xml, true, xml, nextAbcText, "", "");
        }

        private static LoadFlowResult failure(String diagnosticCode, String diagnosticMessage) {
            return new LoadFlowResult(false, "", false, null, null, diagnosticCode, diagnosticMessage);
        }

        public boolean isOk() {
            return ok;
        }

        public String getXmlToLoad() {
            return xmlToLoad;
        }

        public boolean isCollapseInputSection() {
            return collapseInputSection;
        }

        public String getNextXmlInputText() {
            return nextXmlInputText;
        }

        public String getNextAbcInputText() {
            return nextAbcInputText;
        }

        public String getDiagnosticCode() {
            return diagnosticCode;
        }

        public String getDiagnosticMessage() {
            return diagnosticMessage;
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

    /** A value result that is either UTF-8 text or a byte payload. */
    public static final class EncodedOutput {
        private final String text;
        private final byte[] bytes;

        private EncodedOutput(String text, byte[] bytes) {
            this.text = text;
            this.bytes = bytes == null ? null : bytes.clone();
        }

        private static EncodedOutput text(String value) {
            return new EncodedOutput(value == null ? "" : value, null);
        }

        private static EncodedOutput bytes(byte[] value) {
            return new EncodedOutput(null, value == null ? new byte[0] : value);
        }

        public boolean isText() {
            return bytes == null;
        }

        public String getText() {
            return text == null ? "" : text;
        }

        public byte[] getBytes() {
            return bytes == null ? getText().getBytes(StandardCharsets.UTF_8) : bytes.clone();
        }
    }

    /**
     * Runtime-independent MIDI output options corresponding to the Node
     * {@code MidiOutputOptions} object. Null Boolean values represent omitted
     * optional properties.
     */
    public static final class MidiOutputOptions {
        private final double ticksPerQuarter;
        private final String programPreset;
        private final Boolean forceProgramPreset;
        private final String graceTimingMode;
        private final Boolean metricAccentEnabled;
        private final String metricAccentProfile;
        private final String exportProfile;
        private final Boolean keepRoundtripMetadata;
        private final Boolean rawWriter;

        public MidiOutputOptions(double ticksPerQuarter) {
            this(ticksPerQuarter, null, null, null, null, null, null, null, null);
        }

        public MidiOutputOptions(double ticksPerQuarter, String exportProfile, Boolean rawWriter) {
            this(ticksPerQuarter, null, null, null, null, null, exportProfile, null, rawWriter);
        }

        public MidiOutputOptions(double ticksPerQuarter, String programPreset, Boolean forceProgramPreset,
                String graceTimingMode, Boolean metricAccentEnabled, String metricAccentProfile,
                String exportProfile, Boolean keepRoundtripMetadata, Boolean rawWriter) {
            this.ticksPerQuarter = ticksPerQuarter;
            this.programPreset = programPreset;
            this.forceProgramPreset = forceProgramPreset;
            this.graceTimingMode = graceTimingMode;
            this.metricAccentEnabled = metricAccentEnabled;
            this.metricAccentProfile = metricAccentProfile;
            this.exportProfile = exportProfile;
            this.keepRoundtripMetadata = keepRoundtripMetadata;
            this.rawWriter = rawWriter;
        }

        public double getTicksPerQuarter() {
            return ticksPerQuarter;
        }

        public String getProgramPreset() {
            return programPreset == null ? "electric_piano_2" : programPreset;
        }

        public boolean isForceProgramPreset() {
            return Boolean.TRUE.equals(forceProgramPreset);
        }

        public String getGraceTimingMode() {
            return graceTimingMode == null ? "before_beat" : graceTimingMode;
        }

        public boolean isMetricAccentEnabled() {
            return Boolean.TRUE.equals(metricAccentEnabled);
        }

        public String getMetricAccentProfile() {
            return metricAccentProfile == null ? "subtle" : metricAccentProfile;
        }

        public String getExportProfile() {
            return "musescore_parity".equals(exportProfile) ? "musescore_parity" : "safe";
        }

        public boolean isKeepRoundtripMetadata() {
            return !Boolean.FALSE.equals(keepRoundtripMetadata);
        }

        public boolean isRawWriter() {
            return Boolean.TRUE.equals(rawWriter);
        }

        /** Returns the optional override unchanged; null means use profile default. */
        public Boolean getRawWriter() {
            return rawWriter;
        }
    }

    /** One ordered string or byte entry for {@link #encodeZipBundleOutput(List, boolean)}. */
    public static final class OutputArchiveEntry {
        private final String path;
        private final byte[] bytes;

        public OutputArchiveEntry(String path, String text) {
            this(path, text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8));
        }

        public OutputArchiveEntry(String path, byte[] bytes) {
            this.path = path;
            this.bytes = bytes == null ? new byte[0] : bytes.clone();
        }

        public String getPath() {
            return path;
        }

        public byte[] getBytes() {
            return bytes.clone();
        }
    }

    /** One source entry for {@link #createZipBundleDownloadPayload(List, String, boolean)}. */
    public static final class ZipBundleEntry {
        private final String fileName;
        private final byte[] bytes;

        public ZipBundleEntry(String fileName, byte[] bytes) {
            this.fileName = fileName;
            this.bytes = bytes == null ? new byte[0] : bytes.clone();
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getBytes() {
            return bytes.clone();
        }
    }
}
