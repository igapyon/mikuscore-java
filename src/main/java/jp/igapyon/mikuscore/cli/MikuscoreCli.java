package jp.igapyon.mikuscore.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        if (args == null || args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printHelp(out);
            return 0;
        }
        if ("--version".equals(args[0])) {
            out.println(CoreApi.version());
            return 0;
        }
        if ("state".equals(args[0])) {
            return runState(args, in, out, err);
        }
        err.println("Unsupported command: " + args[0]);
        err.println("Product commands will be added through straight conversion from upstream mikuscore.");
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
                String xmlText = readInputText(inputPath, in);
                out.print(MusicXmlState.summarizeMusicXmlState(xmlText).toJson());
                return 0;
            } catch (Exception ex) {
                err.println("Failed to summarize MusicXML state: " + ex.getMessage());
                return 1;
            }
        }
        if ("inspect-measure".equals(args[1])) {
            String measureNumber = optionValue(args, "--measure");
            if (measureNumber == null || measureNumber.trim().length() == 0) {
                err.println("Missing required option: --measure");
                return 2;
            }
            String inputPath = optionValue(args, "--in");
            try {
                String xmlText = readInputText(inputPath, in);
                out.print(MusicXmlState.inspectMusicXmlMeasure(xmlText, measureNumber).toJson());
                return 0;
            } catch (Exception ex) {
                err.println("Failed to inspect MusicXML measure: " + ex.getMessage());
                return 1;
            }
        }
        if ("diff".equals(args[1])) {
            String beforePath = optionValue(args, "--before");
            String afterPath = optionValue(args, "--after");
            if (beforePath == null || beforePath.trim().length() == 0) {
                err.println("Missing required option: --before");
                return 2;
            }
            if (afterPath == null || afterPath.trim().length() == 0) {
                err.println("Missing required option: --after");
                return 2;
            }
            try {
                String beforeXml = readInputText(beforePath, in);
                String afterXml = readInputText(afterPath, in);
                out.print(MusicXmlState.diffMusicXmlState(beforeXml, afterXml).toJson());
                return 0;
            } catch (Exception ex) {
                err.println("Failed to diff MusicXML state: " + ex.getMessage());
                return 1;
            }
        }
        if ("validate-command".equals(args[1])) {
            String commandPayload = optionValue(args, "--command");
            if (commandPayload == null || commandPayload.trim().length() == 0) {
                err.println("Missing required option: --command");
                return 2;
            }
            String inputPath = optionValue(args, "--in");
            try {
                String xmlText = readInputText(inputPath, in);
                out.print(MusicXmlState.validateMusicXmlCommand(xmlText, commandPayload).toJson());
                return 0;
            } catch (Exception ex) {
                err.println("Failed to validate MusicXML command: " + ex.getMessage());
                return 1;
            }
        }
        if ("apply-command".equals(args[1])) {
            String commandPayload = optionValue(args, "--command");
            if (commandPayload == null || commandPayload.trim().length() == 0) {
                err.println("Missing required option: --command");
                return 2;
            }
            String inputPath = optionValue(args, "--in");
            String outputPath = optionValue(args, "--out");
            try {
                String xmlText = readInputText(inputPath, in);
                String output = MusicXmlState.applyMusicXmlCommand(xmlText, commandPayload);
                writeOutputText(outputPath, output, out);
                return 0;
            } catch (Exception ex) {
                err.println("Failed to apply MusicXML command: " + ex.getMessage());
                return 1;
            }
        }
        err.println("Unsupported state command: " + args[1]);
        return 2;
    }

    private static void printHelp(PrintStream out) {
        out.println("mikuscore-java");
        out.println();
        out.println("Usage:");
        out.println("  java -jar target/mikuscore.jar --help");
        out.println("  java -jar target/mikuscore.jar --version");
        out.println();
        out.println("Planned upstream command families:");
        out.println("  convert --from <format> --to <format>");
        out.println("  render svg");
        out.println("  state <command>");
    }

    private static void printStateHelp(PrintStream out) {
        out.println("mikuscore-java state");
        out.println();
        out.println("Usage:");
        out.println("  java -jar target/mikuscore.jar state summarize [--in <file>|-]");
        out.println("  java -jar target/mikuscore.jar state inspect-measure --measure <number> [--in <file>|-]");
        out.println("  java -jar target/mikuscore.jar state validate-command --command <json> [--in <file>|-]");
        out.println("  java -jar target/mikuscore.jar state apply-command --command <json> [--in <file>|-] [--out <file>|-]");
        out.println("  java -jar target/mikuscore.jar state diff --before <file> --after <file>");
        out.println();
        out.println("Commands:");
        out.println("  summarize   Emit a compact JSON summary of canonical MusicXML state");
        out.println("  inspect-measure   Emit note selectors for one MusicXML measure");
        out.println("  validate-command   Validate one bounded MusicXML command");
        out.println("  apply-command   Apply one bounded command and emit the next canonical MusicXML state");
        out.println("  diff   Emit a compact JSON diff between two MusicXML states");
    }

    private static String optionValue(String[] args, String name) {
        for (int index = 0; index < args.length - 1; index++) {
            if (name.equals(args[index])) {
                return args[index + 1];
            }
        }
        return null;
    }

    private static String readInputText(String inputPath, InputStream in) throws IOException {
        if (inputPath != null && !"-".equals(inputPath)) {
            return new String(Files.readAllBytes(Paths.get(inputPath)), StandardCharsets.UTF_8);
        }
        return new String(readAllBytes(in), StandardCharsets.UTF_8);
    }

    private static void writeOutputText(String outputPath, String text, PrintStream out) throws IOException {
        if (outputPath != null && !"-".equals(outputPath)) {
            Files.write(Paths.get(outputPath), text.getBytes(StandardCharsets.UTF_8));
            return;
        }
        out.print(text);
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
