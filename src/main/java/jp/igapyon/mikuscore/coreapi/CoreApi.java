package jp.igapyon.mikuscore.coreapi;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import jp.igapyon.mikuscore.abc.AbcIo;
import jp.igapyon.mikuscore.mei.MeiIo;
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

    private static boolean hasExtension(String path, String extension) {
        if (path == null) {
            return false;
        }
        return path.trim().toLowerCase(Locale.ROOT).endsWith(extension);
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
