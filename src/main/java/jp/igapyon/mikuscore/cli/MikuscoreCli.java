package jp.igapyon.mikuscore.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.igapyon.mikuscore.coreapi.CoreApi;
import jp.igapyon.mikuscore.musicxml.MusicXmlState;

/**
 * Minimal CLI entrypoint. Product commands are added through straight conversion.
 */
public final class MikuscoreCli {
    private MikuscoreCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.in, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, System.in, out, err);
    }

    public static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        if (isHelpOrVersionRequest(args)) {
            return runWithoutDiagnostics(args, in, out, err);
        }
        String diagnosticsFormat = optionValue(args, "--diagnostics");
        if ("json".equals(diagnosticsFormat)) {
            ByteArrayOutputStream capturedDiagnostics = new ByteArrayOutputStream();
            PrintStream diagnosticStream = new PrintStream(capturedDiagnostics, true);
            int exitCode = runWithoutDiagnostics(args, in, out, diagnosticStream);
            writeJsonDiagnostics(err, args, exitCode,
                    new String(capturedDiagnostics.toByteArray(), StandardCharsets.UTF_8));
            return exitCode;
        }
        return runWithoutDiagnostics(args, in, out, err);
    }

    private static int runWithoutDiagnostics(String[] args, InputStream in, PrintStream out, PrintStream err) {
        if (args == null || args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printHelp(out);
            return 0;
        }
        if ("--version".equals(args[0])) {
            out.println(CoreApi.version());
            return 0;
        }
        if (hasOption(args, "--diagnostics")) {
            String diagnosticsFormat = optionValue(args, "--diagnostics");
            if (diagnosticsFormat == null || diagnosticsFormat.trim().length() == 0) {
                err.println("Option --diagnostics requires a value.");
                return 2;
            }
            if (!"text".equals(diagnosticsFormat) && !"json".equals(diagnosticsFormat)) {
                err.println("--diagnostics must be either text or json.");
                return 2;
            }
        }
        if ("convert".equals(args[0])) {
            return runConvert(args, in, out, err);
        }
        if ("render".equals(args[0])) {
            return runRender(args, in, out, err);
        }
        if ("state".equals(args[0])) {
            return runState(args, in, out, err);
        }
        err.println("Unsupported command: " + args[0]);
        err.println("Product commands will be added through straight conversion from upstream miku-score.");
        return 2;
    }

    private static int runConvert(String[] args, InputStream in, PrintStream out, PrintStream err) {
        if (args.length < 2 || "--help".equals(args[1]) || "-h".equals(args[1])) {
            printConvertHelp(out);
            return 0;
        }
        String from = lowerOptionValue(args, "--from");
        String to = lowerOptionValue(args, "--to");
        if (from == null || from.length() == 0 || to == null || to.length() == 0) {
            err.println("convert requires both --from <format> and --to <format>.");
            return 2;
        }
        if ("musicxml".equals(from) && "musicxml".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                writeMusicXmlOutput(outputPath, xmlText, out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MusicXML to MusicXML conversion failed");
            }
        }
        if ("abc".equals(from) && "musicxml".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String abcText = readInputText(inputPath, in);
                CoreApi.CliResult result = CoreApi.importAbcToMusicXml(abcText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeMusicXmlOutput(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "ABC to MusicXML conversion failed");
            }
        }
        if ("abc".equals(from) && "midi".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String abcText = readInputText(inputPath, in);
                CoreApi.CliResult imported = CoreApi.importAbcToMusicXml(abcText);
                if (!imported.isOk()) {
                    err.println(imported.getDiagnostic());
                    return 1;
                }
                CoreApi.CliResult exported = CoreApi.exportMusicXmlToMidi(imported.getOutput());
                if (!exported.isOk()) {
                    err.println(exported.getDiagnostic());
                    return 1;
                }
                writeOutputBytes(outputPath, exported.getOutputBytes(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "ABC to MIDI conversion failed");
            }
        }
        if ("mei".equals(from) && "musicxml".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String meiText = readInputText(inputPath, in);
                CoreApi.CliResult result = CoreApi.importMeiToMusicXml(meiText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeMusicXmlOutput(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MEI to MusicXML conversion failed");
            }
        }
        if ("lilypond".equals(from) && "musicxml".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String lilyPondText = readInputText(inputPath, in);
                CoreApi.CliResult result = CoreApi.importLilyPondToMusicXml(lilyPondText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeMusicXmlOutput(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "LilyPond to MusicXML conversion failed");
            }
        }
        if ("midi".equals(from) && "musicxml".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                CoreApi.CliResult result = CoreApi.importMidiToMusicXml(readInputBytes(inputPath, in));
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeMusicXmlOutput(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MIDI to MusicXML conversion failed");
            }
        }
        if ("musescore".equals(from) && "musicxml".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String museScoreText = readMuseScoreInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.importMuseScoreToMusicXml(museScoreText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeMusicXmlOutput(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MuseScore to MusicXML conversion failed");
            }
        }
        if ("musicxml".equals(from) && "abc".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.exportMusicXmlToAbc(xmlText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeOutputText(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MusicXML to ABC conversion failed");
            }
        }
        if ("musicxml".equals(from) && "mei".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.exportMusicXmlToMei(xmlText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeOutputText(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MusicXML to MEI conversion failed");
            }
        }
        if ("musicxml".equals(from) && "lilypond".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.exportMusicXmlToLilyPond(xmlText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeOutputText(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MusicXML to LilyPond conversion failed");
            }
        }
        if ("musicxml".equals(from) && "musescore".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.exportMusicXmlToMuseScore(xmlText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeMuseScoreOutput(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MusicXML to MuseScore conversion failed");
            }
        }
        if ("musicxml".equals(from) && "midi".equals(to)) {
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.exportMusicXmlToMidi(xmlText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeOutputBytes(outputPath, result.getOutputBytes(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "MusicXML to MIDI conversion failed");
            }
        }
        err.println("Unsupported conversion pair: --from " + from + " --to " + to);
        return 2;
    }

    private static int runRender(String[] args, InputStream in, PrintStream out, PrintStream err) {
        if (args.length < 2 || "--help".equals(args[1]) || "-h".equals(args[1])) {
            printRenderHelp(out);
            return 0;
        }
        if ("svg".equals(args[1])) {
            String from = lowerOptionValue(args, "--from");
            if (from == null || from.length() == 0) {
                from = "musicxml";
            }
            if (!"musicxml".equals(from) && !"abc".equals(from)) {
                err.println("Unsupported render source: --from " + from);
                return 2;
            }
            err.println("SVG render is unsupported in the current Java slice: upstream depends on verovio.js/browser runtime.");
            return 2;
        }
        err.println("Unsupported render command: " + args[1]);
        return 2;
    }

    private static int runState(String[] args, InputStream in, PrintStream out, PrintStream err) {
        if (args.length < 2 || "--help".equals(args[1]) || "-h".equals(args[1])) {
            printStateHelp(out);
            return 0;
        }
        if ("summarize".equals(args[1])) {
            String inputPath = optionValue(args, "--in");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.summarizeMusicXmlState(xmlText);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                out.print(result.getOutput());
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "Failed to summarize MusicXML state");
            }
        }
        if ("inspect-measure".equals(args[1])) {
            String measureNumber = optionValue(args, "--measure");
            if (measureNumber == null || measureNumber.trim().length() == 0) {
                err.println("state inspect-measure requires --measure <number>.");
                return 2;
            }
            String inputPath = optionValue(args, "--in");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.inspectMusicXmlMeasure(xmlText, measureNumber);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                out.print(result.getOutput());
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "Failed to inspect MusicXML measure");
            }
        }
        if ("diff".equals(args[1])) {
            String beforePath = optionValue(args, "--before");
            String afterPath = optionValue(args, "--after");
            if (beforePath == null || beforePath.trim().length() == 0 || afterPath == null
                    || afterPath.trim().length() == 0) {
                err.println("state diff requires both --before <file> and --after <file>.");
                return 2;
            }
            try {
                String beforeXml = readMusicXmlInput(beforePath, in);
                String afterXml = readMusicXmlInput(afterPath, in);
                CoreApi.CliResult result = CoreApi.diffMusicXmlState(beforeXml, afterXml);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                out.print(result.getOutput());
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "Failed to diff MusicXML state");
            }
        }
        if ("validate-command".equals(args[1])) {
            String commandPayload;
            try {
                commandPayload = readCommandPayload(args, in, err);
            } catch (IOException ex) {
                err.println("Failed to read command payload: " + ex.getMessage());
                return 1;
            }
            if (commandPayload == null) {
                return 2;
            }
            String inputPath = optionValue(args, "--in");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.validateMusicXmlCommand(xmlText, commandPayload);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                out.print(result.getOutput());
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "Failed to validate MusicXML command");
            }
        }
        if ("apply-command".equals(args[1])) {
            String commandPayload;
            try {
                commandPayload = readCommandPayload(args, in, err);
            } catch (IOException ex) {
                err.println("Failed to read command payload: " + ex.getMessage());
                return 1;
            }
            if (commandPayload == null) {
                return 2;
            }
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readMusicXmlInput(inputPath, in);
                CoreApi.CliResult result = CoreApi.applyMusicXmlCommand(xmlText, commandPayload);
                if (!result.isOk()) {
                    err.println(result.getDiagnostic());
                    return 1;
                }
                writeWarnings(err, result);
                writeOutputText(outputPath, result.getOutput(), out);
                return 0;
            } catch (Exception ex) {
                return reportCommandFailure(err, ex, "Failed to apply MusicXML command");
            }
        }
        err.println("Unsupported state command: " + args[1]);
        return 2;
    }

    private static void printHelp(PrintStream out) {
        out.println("miku-score-java");
        out.println();
        out.println("Usage:");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from abc --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from abc --to midi [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to abc [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from mei --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from lilypond --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to lilypond [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from midi --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musescore --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to musescore [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to midi [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar render svg [--from musicxml|abc] [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar state summarize [--in <file>|-]");
        out.println("  java -jar target/miku-score.jar state inspect-measure --measure <number> [--in <file>|-]");
        out.println("  java -jar target/miku-score.jar state validate-command [--command <json>|--command-file <file>|-] [--in <file>|-]");
        out.println("  java -jar target/miku-score.jar state apply-command [--command <json>|--command-file <file>|-] [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar state diff --before <file> --after <file>");
        out.println("  java -jar target/miku-score.jar convert --help");
        out.println("  java -jar target/miku-score.jar state --help");
        out.println("  java -jar target/miku-score.jar --help");
        out.println();
        out.println("Commands:");
        out.println("  convert   Convert score text between supported formats");
        out.println("  render    Render derived outputs such as SVG");
        out.println("  state     Inspect canonical MusicXML state");
        out.println();
        out.println("Options:");
        out.println("  --from <format>  Source format");
        out.println("  --to <format>    Target format");
        out.println("  --in <file>|-    Read input from file or stdin");
        out.println("  --out <file>|-   Write output to file or stdout");
        out.println("  --diagnostics text|json  Select diagnostics format");
        out.println("  --version        Show version");
        out.println();
        out.println("Diagnostics:");
        out.println("  --diagnostics text is the default. --diagnostics json writes version 1 diagnostics JSON to stderr.");
    }

    private static void printConvertHelp(PrintStream out) {
        out.println("miku-score-java convert");
        out.println();
        out.println("Usage:");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from abc --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from abc --to midi [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to abc [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from mei --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to mei [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from lilypond --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to lilypond [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from midi --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musescore --to musicxml [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to musescore [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --from musicxml --to midi [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar convert --help");
        out.println();
        out.println("Description:");
        out.println("  Convert score text between supported formats.");
        out.println();
        out.println("Supported pairs:");
        out.println("  --from musicxml --to musicxml");
        out.println("  --from abc --to musicxml");
        out.println("  --from abc --to midi");
        out.println("  --from musicxml --to abc");
        out.println("  --from mei --to musicxml");
        out.println("  --from musicxml --to mei");
        out.println("  --from lilypond --to musicxml");
        out.println("  --from musicxml --to lilypond");
        out.println("  --from midi --to musicxml");
        out.println("  --from musescore --to musicxml");
        out.println("  --from musicxml --to musescore");
        out.println("  --from musicxml --to midi");
        out.println();
        out.println("Input:");
        out.println("  --in <file>|-  Read MusicXML, MXL, ABC, MEI, LilyPond, MIDI, MuseScore MSCX, or MSCZ from file or stdin");
        out.println("  stdin          Used when --in is omitted or is -");
        out.println("  file paths     musicxml accepts .musicxml / .xml / .mxl; musescore accepts .mscx / .mscz; midi accepts bytes");
        out.println();
        out.println("Output:");
        out.println("  --out <file>|-  Write MusicXML text, MXL bytes, MIDI bytes, MuseScore MSCX text, or MSCZ bytes");
        out.println("  stdout          Used when --out is omitted or is -");
        out.println("  file paths      musicxml writes .mxl when --out ends with .mxl; musescore writes .mscz when requested");
        out.println("  overwrite       --out <file> replaces an existing file");
        out.println();
        out.println("Runtime contract:");
        out.println("  primary output  stdout; usage and processing failures use stderr");
        out.println("  exit codes      0 success, 1 processing failure, 2 invalid usage or unsupported pair");
        out.println("  diagnostics     --diagnostics text is the default; --diagnostics json writes version 1 diagnostics JSON to stderr");
    }

    private static void printRenderHelp(PrintStream out) {
        out.println("miku-score-java render");
        out.println();
        out.println("Usage:");
        out.println("  java -jar target/miku-score.jar render svg [--from musicxml|abc] [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar render --help");
        out.println();
        out.println("Description:");
        out.println("  Render derived outputs.");
        out.println();
        out.println("Supported outputs:");
        out.println("  svg   Recognized but unsupported in the current Java slice");
        out.println();
        out.println("Constraint:");
        out.println("  SVG rendering depends on upstream verovio.js/browser runtime.");
    }

    private static void printStateHelp(PrintStream out) {
        out.println("miku-score-java state");
        out.println();
        out.println("Usage:");
        out.println("  java -jar target/miku-score.jar state summarize [--in <file>|-]");
        out.println("  java -jar target/miku-score.jar state inspect-measure --measure <number> [--in <file>|-]");
        out.println("  java -jar target/miku-score.jar state validate-command [--command <json>|--command-file <file>|-] [--in <file>|-]");
        out.println("  java -jar target/miku-score.jar state apply-command [--command <json>|--command-file <file>|-] [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/miku-score.jar state diff --before <file> --after <file>");
        out.println();
        out.println("Description:");
        out.println("  Inspect canonical MusicXML state.");
        out.println();
        out.println("Commands:");
        out.println("  summarize   Emit a compact JSON summary of canonical MusicXML state");
        out.println("  inspect-measure   Emit note selectors for one MusicXML measure");
        out.println("  validate-command   Validate one bounded MusicXML command");
        out.println("  apply-command   Apply one bounded command and emit the next canonical MusicXML state");
        out.println("  diff   Emit a compact JSON diff between two MusicXML states");
        out.println();
        out.println("Command payload note:");
        out.println("  Targeting may use targetNodeId/anchorNodeId directly or selector/anchor_selector from inspect-measure output.");
        out.println();
        out.println("Runtime contract:");
        out.println("  input/output    UTF-8 MusicXML; stdin is used by commands with omitted --in, and results use stdout");
        out.println("  primary output  stdout; usage and processing failures use stderr");
        out.println("  exit codes      0 success, 1 processing failure, 2 invalid usage");
        out.println("  diagnostics     --diagnostics text is the default; --diagnostics json writes version 1 diagnostics JSON to stderr");
    }

    private static boolean isHelpOrVersionRequest(String[] args) {
        return args == null || args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])
                || "--version".equals(args[0]) || hasOption(args, "--help") || hasOption(args, "-h");
    }

    private static String readCommandPayload(String[] args, InputStream in, PrintStream err) throws IOException {
        String inlinePayload = optionValue(args, "--command");
        String commandFile = optionValue(args, "--command-file");
        boolean hasInlinePayload = inlinePayload != null && inlinePayload.trim().length() > 0;
        boolean hasCommandFile = commandFile != null && commandFile.trim().length() > 0;
        if (hasInlinePayload == hasCommandFile) {
            err.println("state validate-command requires exactly one of --command <json> or --command-file <file>.");
            return null;
        }
        String payload = hasInlinePayload ? inlinePayload : readInputText(commandFile, in);
        try {
            MusicXmlState.requireMusicXmlCommandJsonObject(payload);
            return payload;
        } catch (IllegalArgumentException ex) {
            err.println("Command payload must be valid JSON: " + ex.getMessage());
            return null;
        }
    }

    private static void writeJsonDiagnostics(PrintStream err, String[] args, int exitCode, String capturedText) {
        List<String> errors = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();
        String message = capturedText == null ? "" : capturedText.trim();
        if (message.length() > 0) {
            String[] lines = message.split("\\r?\\n");
            for (String line : lines) {
                if (line.startsWith("[warning] ")) {
                    warnings.add(line.substring("[warning] ".length()));
                } else if (line.length() > 0) {
                    errors.add(line);
                }
            }
        }
        String command = commandForDiagnostics(args);
        String inputPath = optionValue(args, "--in");
        String outputPath = optionValue(args, "--out");
        boolean ok = exitCode == 0 && errors.isEmpty();
        String status = ok ? (warnings.isEmpty() ? "success" : "warning") : "error";
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"ok\": ").append(ok).append(",\n");
        json.append("  \"diagnostics_version\": 1,\n");
        json.append("  \"command\": \"").append(jsonEscape(command)).append("\",\n");
        json.append("  \"context\": \"").append(jsonEscape(command)).append("\",\n");
        json.append("  \"status\": \"").append(status).append("\",\n");
        json.append("  \"exit_code\": ").append(exitCode).append(",\n");
        json.append("  \"warning_count\": ").append(warnings.size()).append(",\n");
        json.append("  \"error_count\": ").append(errors.size()).append(",\n");
        json.append("  \"io\": {\n");
        json.append("    \"inputs\": [").append(jsonInputDescriptor(inputPath)).append("],\n");
        json.append("    \"output\": ").append(jsonOutputDescriptor(outputPath)).append("\n");
        json.append("  },\n");
        json.append("  \"warnings\": ").append(jsonStringArray(warnings)).append(",\n");
        json.append("  \"errors\": ").append(jsonStringArray(errors));
        if (!ok) {
            json.append(",\n  \"error_type\": \"")
                    .append(exitCode == 2 ? "usage_error" : "processing_error").append("\",");
            json.append("\n  \"error_code\": \"")
                    .append(jsonEscape(diagnosticErrorCode(message, exitCode))).append("\"");
        }
        json.append("\n}\n");
        err.print(json.toString());
    }

    private static void writeWarnings(PrintStream err, CoreApi.CliResult result) {
        for (String warning : result.getWarnings()) {
            err.println("[warning] " + warning);
        }
    }

    private static String commandForDiagnostics(String[] args) {
        if (args == null || args.length == 0) {
            return "cli";
        }
        List<String> command = new ArrayList<String>();
        for (int index = 0; index < args.length; index++) {
            String token = args[index];
            if (token.startsWith("--")) {
                if (!"--help".equals(token) && index + 1 < args.length) {
                    index++;
                }
                continue;
            }
            command.add(token);
        }
        return command.isEmpty() ? "cli" : join(command, " ");
    }

    private static String diagnosticErrorCode(String message, int exitCode) {
        if (exitCode != 2) {
            return "processing_error";
        }
        if (message.contains("requires both --from")) {
            return "missing_from_to";
        }
        if (message.contains("Unsupported conversion pair")) {
            return "unsupported_conversion_pair";
        }
        if (message.contains("Unsupported render source")) {
            return "unsupported_render_source";
        }
        if (message.contains("requires --measure")) {
            return "missing_measure_option";
        }
        if (message.contains("exactly one of --command")) {
            return "missing_command_payload";
        }
        if (message.contains("Command payload must be valid JSON")) {
            return "invalid_command_json";
        }
        if (message.contains("requires a value")) {
            return "missing_option_value";
        }
        if (message.contains("--diagnostics must")) {
            return "invalid_diagnostics_option";
        }
        return "usage_error";
    }

    private static String jsonInputDescriptor(String inputPath) {
        if (inputPath == null || "-".equals(inputPath)) {
            return "{\"option\":\"--in\",\"mode\":\"stdin\"}";
        }
        return "{\"option\":\"--in\",\"mode\":\"file\",\"path\":\"" + jsonEscape(inputPath) + "\"}";
    }

    private static String jsonOutputDescriptor(String outputPath) {
        if (outputPath == null || "-".equals(outputPath)) {
            return "{\"mode\":\"stdout\"}";
        }
        return "{\"mode\":\"file\",\"path\":\"" + jsonEscape(outputPath) + "\"}";
    }

    private static String jsonStringArray(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append("\"").append(jsonEscape(values.get(index))).append("\"");
        }
        return json.append("]").toString();
    }

    private static String jsonEscape(String text) {
        String value = text == null ? "" : text;
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\\' || ch == '\"') {
                escaped.append('\\').append(ch);
            } else if (ch == '\n') {
                escaped.append("\\n");
            } else if (ch == '\r') {
                escaped.append("\\r");
            } else if (ch == '\t') {
                escaped.append("\\t");
            } else if (ch < 0x20) {
                escaped.append(String.format("\\u%04x", Integer.valueOf(ch)));
            } else {
                escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                text.append(delimiter);
            }
            text.append(values.get(index));
        }
        return text.toString();
    }

    private static String optionValue(String[] args, String name) {
        for (int index = 0; index < args.length - 1; index++) {
            if (name.equals(args[index])) {
                return args[index + 1];
            }
        }
        return null;
    }

    private static boolean hasOption(String[] args, String name) {
        for (String arg : args) {
            if (name.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String lowerOptionValue(String[] args, String name) {
        String value = optionValue(args, name);
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String readMusicXmlInput(String inputPath, InputStream in) throws IOException {
        CoreApi.CliResult result = CoreApi.decodeCliMusicXmlInput(readInputBytes(inputPath, in), inputPath);
        if (!result.isOk()) {
            throw new IOException(result.getDiagnostic());
        }
        return result.getOutput();
    }

    private static String readMuseScoreInput(String inputPath, InputStream in) throws IOException {
        CoreApi.CliResult result = CoreApi.decodeCliMuseScoreInput(readInputBytes(inputPath, in), inputPath);
        if (!result.isOk()) {
            throw new IOException(result.getDiagnostic());
        }
        return result.getOutput();
    }

    private static String readInputText(String inputPath, InputStream in) throws IOException {
        return new String(readInputBytes(inputPath, in), StandardCharsets.UTF_8);
    }

    private static byte[] readInputBytes(String inputPath, InputStream in) throws IOException {
        if (inputPath != null && !"-".equals(inputPath)) {
            return Files.readAllBytes(Paths.get(inputPath));
        }
        byte[] bytes = readAllBytes(in);
        if (bytes.length == 0) {
            throw new MissingInputException();
        }
        return bytes;
    }

    private static int reportCommandFailure(PrintStream err, Exception ex, String fallback) {
        if (ex instanceof MissingInputException) {
            err.println(ex.getMessage());
            return 2;
        }
        err.println(fallback + ": " + ex.getMessage());
        return 1;
    }

    private static final class MissingInputException extends IOException {
        private static final long serialVersionUID = 1L;

        private MissingInputException() {
            super("Input is required. Use --in <file> or pipe text via stdin.");
        }
    }

    private static void writeMusicXmlOutput(String outputPath, String text, PrintStream out) throws IOException {
        CoreApi.CliResult result = CoreApi.encodeCliMusicXmlOutput(text, outputPath);
        if (!result.isOk()) {
            throw new IOException(result.getDiagnostic());
        }
        writeOutputBytes(outputPath, result.getOutputBytes(), out);
    }

    private static void writeMuseScoreOutput(String outputPath, String text, PrintStream out) throws IOException {
        CoreApi.CliResult result = CoreApi.encodeCliMuseScoreOutput(text, outputPath);
        if (!result.isOk()) {
            throw new IOException(result.getDiagnostic());
        }
        writeOutputBytes(outputPath, result.getOutputBytes(), out);
    }

    private static void writeOutputText(String outputPath, String text, PrintStream out) throws IOException {
        writeOutputBytes(outputPath, text.getBytes(StandardCharsets.UTF_8), out);
    }

    private static void writeOutputBytes(String outputPath, byte[] bytes, PrintStream out) throws IOException {
        if (outputPath != null && !"-".equals(outputPath)) {
            Path path = Paths.get(outputPath);
            Files.write(path, bytes);
            return;
        }
        out.write(bytes);
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) >= 0) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
