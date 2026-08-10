/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.lilypond;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.abc.AbcIo;
import jp.igapyon.mikuscore.core.StaffClefPolicy;
import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public final class LilyPondIo {
    private LilyPondIo() {
    }

    public static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static int gcd(double a, double b) {
        int x = Math.abs((int) Math.round(Double.isFinite(a) ? a : 0));
        int y = Math.abs((int) Math.round(Double.isFinite(b) ? b : 0));
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return x == 0 ? 1 : x;
    }

    public static Fraction reduceFraction(double num, double den) {
        if (!Double.isFinite(num) || !Double.isFinite(den) || den == 0.0d) {
            return new Fraction(1, 1);
        }
        int sign = den < 0.0d ? -1 : 1;
        int n = (int) Math.round(num * sign);
        int d = (int) Math.round(den * sign);
        int g = gcd(n, d);
        return new Fraction(n / g, d / g);
    }

    public static String lilyDurationToAbcLen(double duration, double dotCount) {
        return lilyDurationToAbcLen(duration, dotCount, 1);
    }

    private static String lilyDurationToAbcLen(double duration, double dotCount, int multiplier) {
        int safeDuration = Double.isFinite(duration) && duration > 0.0d ? (int) Math.round(duration) : 4;
        Fraction ratio = reduceFraction(8, safeDuration);
        int safeMultiplier = Math.max(1, multiplier);
        ratio = reduceFraction(ratio.getNum() * safeMultiplier, ratio.getDen());
        int safeDots = Math.max(0, Math.min(3, (int) Math.round(Double.isFinite(dotCount) ? dotCount : 0)));
        if (safeDots > 0) {
            Fraction dotMul = reduceFraction(Math.pow(2, safeDots + 1) - 1, Math.pow(2, safeDots));
            ratio = reduceFraction(ratio.getNum() * dotMul.getNum(), ratio.getDen() * dotMul.getDen());
        }
        if (ratio.getNum() == ratio.getDen()) {
            return "";
        }
        if (ratio.getDen() == 1) {
            return Integer.toString(ratio.getNum());
        }
        if (ratio.getNum() == 1 && ratio.getDen() == 2) {
            return "/";
        }
        if (ratio.getNum() == 1) {
            return "/" + ratio.getDen();
        }
        return ratio.getNum() + "/" + ratio.getDen();
    }

    public static LilyDuration abcLenToLilyDuration(String token) {
        String raw = token == null ? "" : token.trim();
        if (raw.isEmpty()) {
            return new LilyDuration(8, 0);
        }
        Fraction ratio = new Fraction(1, 1);
        if (raw.matches("^\\d+$")) {
            ratio = reduceFraction(Integer.parseInt(raw), 1);
        } else if ("/".equals(raw)) {
            ratio = new Fraction(1, 2);
        } else if (raw.matches("^/\\d+$")) {
            ratio = reduceFraction(1, Integer.parseInt(raw.substring(1)));
        } else {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)/(\\d+)$").matcher(raw);
            if (matcher.matches()) {
                ratio = reduceFraction(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
            }
        }

        Candidate[] candidates = new Candidate[] {
                new Candidate(reduceFraction(8 * ratio.getDen(), ratio.getNum()), 0),
                new Candidate(reduceFraction(16 * ratio.getDen(), 3 * ratio.getNum()), 1),
                new Candidate(reduceFraction(32 * ratio.getDen(), 7 * ratio.getNum()), 2) };
        for (Candidate candidate : candidates) {
            if (candidate.getFraction().getDen() == 1 && isPowerDuration(candidate.getFraction().getNum())) {
                return new LilyDuration(candidate.getFraction().getNum(), candidate.getDots());
            }
        }
        return new LilyDuration(8, 0);
    }

    public static String abcPitchFromStepOctave(String step, int octave) {
        String upperStep = step == null ? "" : step.trim().toUpperCase();
        if (!upperStep.matches("^[A-G]$")) {
            return "C";
        }
        if (octave >= 5) {
            return upperStep.toLowerCase() + repeat("'", octave - 5);
        }
        return upperStep + repeat(",", Math.max(0, 4 - octave));
    }

    public static String lilyPitchFromStepAlterOctave(String step, double alter, double octave) {
        String base = step == null ? "" : step.trim().toLowerCase();
        if (!base.matches("^[a-g]$")) {
            return "c'";
        }
        int safeAlter = Double.isFinite(alter) ? (int) Math.round(alter) : 0;
        String acc = "";
        if (safeAlter > 0) {
            acc = repeat("is", Math.min(2, safeAlter));
        }
        if (safeAlter < 0) {
            acc = repeat("es", Math.min(2, Math.abs(safeAlter)));
        }
        int octaveShift = (int) Math.round(Double.isFinite(octave) ? octave : 4) - 3;
        String octaveMarks = octaveShift >= 0 ? repeat("'", octaveShift) : repeat(",", Math.abs(octaveShift));
        return base + acc + octaveMarks;
    }

    public static Integer pitchToMidiKey(String step, double alter, double octave) {
        String upper = step == null ? "" : step.trim().toUpperCase();
        if (!upper.matches("^[A-G]$") || !Double.isFinite(octave)) {
            return null;
        }
        return Integer.valueOf(((int) Math.round(octave) + 1) * 12 + lilyPitchClassToSemitone(upper, alter));
    }

    public static int lilyPitchClassToSemitone(String step, double alter) {
        String safeStep = step == null ? "" : step.trim().toUpperCase();
        int base;
        if ("C".equals(safeStep)) {
            base = 0;
        } else if ("D".equals(safeStep)) {
            base = 2;
        } else if ("E".equals(safeStep)) {
            base = 4;
        } else if ("F".equals(safeStep)) {
            base = 5;
        } else if ("G".equals(safeStep)) {
            base = 7;
        } else if ("A".equals(safeStep)) {
            base = 9;
        } else if ("B".equals(safeStep)) {
            base = 11;
        } else {
            base = 0;
        }
        int alt = Double.isFinite(alter) ? (int) Math.round(alter) : 0;
        return base + alt;
    }

    public static String lilyDurationToMusicXmlType(double duration) {
        int d = Double.isFinite(duration) && duration > 0.0d ? (int) Math.round(duration) : 4;
        switch (d) {
        case 1:
            return "whole";
        case 2:
            return "half";
        case 4:
            return "quarter";
        case 8:
            return "eighth";
        case 16:
            return "16th";
        case 32:
            return "32nd";
        case 64:
            return "64th";
        default:
            return "quarter";
        }
    }

    public static String noteTypeToLilyDuration(String typeText) {
        String normalized = typeText == null ? "" : typeText.trim().toLowerCase();
        if ("whole".equals(normalized)) {
            return "1";
        }
        if ("half".equals(normalized)) {
            return "2";
        }
        if ("quarter".equals(normalized)) {
            return "4";
        }
        if ("eighth".equals(normalized)) {
            return "8";
        }
        if ("16th".equals(normalized)) {
            return "16";
        }
        if ("32nd".equals(normalized)) {
            return "32";
        }
        if ("64th".equals(normalized)) {
            return "64";
        }
        return "4";
    }

    public static String convertLilyPondToMusicXml(String lilySource) {
        return convertLilyPondToMusicXml(lilySource, new LilyPondImportOptions());
    }

    /**
     * Imports LilyPond using the same public metadata and serialization switches
     * as the Node facade. {@code debugMetadata} is retained for API compatibility;
     * the pinned Node implementation does not currently use it to change output.
     */
    public static String convertLilyPondToMusicXml(String lilySource, LilyPondImportOptions options) {
        String rawSource = String.valueOf(lilySource == null ? "" : lilySource);
        LilyPondImportOptions safeOptions = options == null ? new LilyPondImportOptions() : options;
        String laneRoundtripXml = extractMksLanesRoundtripXml(rawSource);
        if (!laneRoundtripXml.isEmpty()) {
            return finalizeLilyPondImportedMusicXml(laneRoundtripXml, rawSource, safeOptions);
        }
        String source = expandLilyVariables(rawSource);
        String title = parseHeaderField(source, "title");
        if (title.isEmpty()) {
            title = "Imported LilyPond";
        }
        String composer = parseHeaderField(source, "composer");
        String meter = parseTimeSignatureText(source);
        String key = parseKeySignatureText(source);
        List<LilyStaffBlock> staffBlocks = extractStaffBlocks(source);
        LilyStaffBlock staffBlock = staffBlocks.isEmpty() ? null : staffBlocks.get(0);
        String body = staffBlock == null ? extractFirstMusicBody(source) : staffBlock.getContent();
        if (body.isEmpty()) {
            throw noParseableLilyPondEvents();
        }
        if (staffBlocks.size() > 1) {
            String xml = convertLilyStaffBlocksToMusicXml(source, title, composer, meter, key, staffBlocks);
            xml = addNativeLyricsToMusicXml(xml, source);
            xml = composer.isEmpty() ? xml : addComposerToMusicXml(xml, composer);
            return finalizeLilyPondImportedMusicXml(xml, rawSource, safeOptions);
        }
        String partName = staffBlock == null ? parseFirstStaffName(source) : staffBlock.getPartName();
        String explicitClef = parseClefText(body);
        String clef = explicitClef;
        if (clef.isEmpty()) {
            clef = inferClefText(body);
        }
        String abcBody = parseLilyBodyToAbc(body);
        if (abcBody.trim().isEmpty()) {
            throw noParseableLilyPondEvents();
        }
        StringBuilder abc = new StringBuilder();
        abc.append("X:1\n");
        abc.append("T:").append(title).append('\n');
        abc.append("M:").append(meter).append('\n');
        abc.append("L:1/8\n");
        if (!partName.isEmpty() || !clef.isEmpty()) {
            abc.append("V:1");
            if (!partName.isEmpty()) {
                abc.append(" name=\"").append(abcQuoted(partName)).append("\"");
            }
            if (!clef.isEmpty()) {
                abc.append(" clef=").append(clef);
            }
            abc.append('\n');
        }
        LilyTransposeHint transpose = staffBlock == null ? parseBodyTransposition(body) : staffBlock.getTranspose();
        if (transpose != null) {
            abc.append("%@mks transpose voice=1");
            if (transpose.getChromatic() != null) {
                abc.append(" chromatic=").append(transpose.getChromatic().intValue());
            }
            if (transpose.getDiatonic() != null) {
                abc.append(" diatonic=").append(transpose.getDiatonic().intValue());
            }
            abc.append('\n');
        }
        abc.append("K:").append(key).append('\n');
        abc.append(abcBody).append('\n');
        String xml = MusicXmlIo.normalizeImportedMusicXmlText(
                AbcIo.musicXmlFromAbc(abc.toString(), new AbcIo.AbcImportOptions(Boolean.FALSE, Boolean.FALSE,
                        Boolean.FALSE, null)));
        xml = addLilyOverfullCarryDiagnostics(xml);
        xml = addNativeRepeatVoltaToMusicXml(xml, body);
        xml = addNativeAlternativeEndingsToMusicXml(xml, body);
        xml = addNativePedalCommandsToMusicXml(xml, body);
        xml = addNativeLyricsToMusicXml(xml, source);
        if (staffBlock != null && explicitClef.isEmpty()) {
            xml = autoSplitLilyPondWideRangeStaff(xml);
        }
        xml = composer.isEmpty() ? xml : addComposerToMusicXml(xml, composer);
        return finalizeLilyPondImportedMusicXml(xml, rawSource, safeOptions);
    }

    public static String exportMusicXmlDomToLilyPond(Document doc) {
        String partName = firstMusicXmlPartName(doc);
        String firstPartId = firstMusicXmlPartId(doc);
        String staffName = firstPartId.isEmpty() ? "P1" : firstPartId;
        StringBuilder out = new StringBuilder();
        out.append("\\version \"2.24.0\"\n");
        out.append("\\header {\n");
        out.append("  title = \"").append(lilyQuoted(firstMusicXmlTitle(doc))).append("\"\n");
        out.append("}\n");
        String lanesPayload = encodeMksLanesRoundtripXml(doc);
        if (!lanesPayload.isEmpty()) {
            out.append("%@mks lanes voice=").append(firstPartId.isEmpty() ? "P1" : firstPartId)
                    .append(" measure=1 data=").append(lanesPayload).append('\n');
        }
        appendMksMeasureMetadata(out, doc, firstPartId.isEmpty() ? "P1" : firstPartId);
        appendMksEventMetadata(out, doc, firstPartId.isEmpty() ? "P1" : firstPartId);
        appendMksSlurMetadata(out, doc, firstPartId.isEmpty() ? "P1" : firstPartId);
        String transposeMetadata = firstMusicXmlTransposeMetadata(doc, firstPartId.isEmpty() ? "P1" : firstPartId);
        if (!transposeMetadata.isEmpty()) {
            out.append("% ").append(transposeMetadata).append('\n');
        }
        Element firstPart = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        List<String> activeStaffNumbers = musicXmlActivePitchedStaffNumbers(firstPart);
        out.append("\\score {\n");
        String timeSignature = firstMusicXmlTimeSignature(doc);
        if (activeStaffNumbers.size() > 1) {
            String partId = firstPartId.isEmpty() ? "P1" : firstPartId;
            out.append("  \\new PianoStaff <<\n");
            for (String staffNumber : activeStaffNumbers) {
                out.append("    \\new Staff = \"").append(lilyQuoted(partId + "_s" + staffNumber)).append("\"");
                if (!partName.isEmpty()) {
                    out.append(" \\with { instrumentName = \"").append(lilyQuoted(partName)).append("\" }");
                }
                out.append(" {");
                appendMusicXmlStaffPrelude(out, timeSignature, firstMusicXmlClefForStaff(doc, staffNumber));
                appendMusicXmlStaffNotes(out, firstMusicXmlPartLilyNotes(doc, staffNumber));
                out.append(" }\n");
            }
            out.append("  >>\n");
        } else {
            String staffNumber = activeStaffNumbers.isEmpty() ? "" : activeStaffNumbers.get(0);
            out.append("  \\new Staff = \"").append(lilyQuoted(staffName)).append("\"");
            if (!partName.isEmpty()) {
                out.append(" \\with { instrumentName = \"").append(lilyQuoted(partName)).append("\" }");
            }
            out.append(" {");
            appendMusicXmlStaffPrelude(out, timeSignature, firstMusicXmlClefForStaff(doc, staffNumber));
            appendMusicXmlStaffNotes(out, firstMusicXmlPartLilyNotes(doc, staffNumber));
            out.append(" }\n");
        }
        out.append("}\n");
        return out.toString();
    }

    private static void appendMusicXmlStaffPrelude(StringBuilder out, String timeSignature, String clef) {
        if (clef != null && !clef.isEmpty()) {
            out.append(" \\clef ").append(clef);
        }
        if (timeSignature != null && !timeSignature.isEmpty()) {
            out.append(" \\time ").append(timeSignature);
        }
    }

    private static void appendMusicXmlStaffNotes(StringBuilder out, List<String> notes) {
        if (notes == null || notes.isEmpty()) {
            out.append(" r4");
            return;
        }
        for (String note : notes) {
            out.append(' ').append(note);
        }
    }

    private static String firstMusicXmlTimeSignature(Document doc) {
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        Element measure = firstDirectChild(part, "measure");
        Element attributes = firstDirectChild(measure, "attributes");
        Element time = firstDirectChild(attributes, "time");
        String beats = directChildText(time, "beats");
        String beatType = directChildText(time, "beat-type");
        if (beats.isEmpty() || beatType.isEmpty()) {
            return "";
        }
        return beats + "/" + beatType;
    }

    private static String firstMusicXmlTitle(Document doc) {
        Element root = doc == null ? null : doc.getDocumentElement();
        Element work = firstDirectChild(root, "work");
        String workTitle = directChildText(work, "work-title");
        if (!workTitle.isEmpty()) {
            return workTitle;
        }
        String movementTitle = directChildText(root, "movement-title");
        return movementTitle.isEmpty() ? "miku-score export" : movementTitle;
    }

    private static String firstMusicXmlTransposeMetadata(Document doc, String voiceId) {
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        if (part == null) {
            return "";
        }
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            Element attributes = firstDirectChild((Element) measureNode, "attributes");
            Element transpose = firstDirectChild(attributes, "transpose");
            if (transpose == null) {
                continue;
            }
            Integer chromatic = parseOptionalIntegerText(directChildText(transpose, "chromatic"));
            Integer diatonic = parseOptionalIntegerText(directChildText(transpose, "diatonic"));
            if (chromatic == null && diatonic == null) {
                continue;
            }
            StringBuilder meta = new StringBuilder("%@mks transpose voice=").append(voiceId);
            if (chromatic != null) {
                meta.append(" chromatic=").append(chromatic.intValue());
            }
            if (diatonic != null) {
                meta.append(" diatonic=").append(diatonic.intValue());
            }
            return meta.toString();
        }
        return "";
    }

    private static String inferMusicXmlClefForStaff(Element part, String staffNumber) {
        int pitchCount = 0;
        int octaveSum = 0;
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            for (Element note : directChildren((Element) measureNode, "note")) {
                String noteStaffNumber = directChildText(note, "staff");
                if (noteStaffNumber.isEmpty()) {
                    noteStaffNumber = "1";
                }
                if (!staffNumber.equals(noteStaffNumber)) {
                    continue;
                }
                Element pitch = firstDirectChild(note, "pitch");
                if (pitch == null) {
                    continue;
                }
                octaveSum += parseIntegerText(directChildText(pitch, "octave"), 4);
                pitchCount++;
            }
        }
        if (pitchCount == 0) {
            return "";
        }
        return octaveSum / pitchCount <= 3 ? "bass" : "";
    }

    private static void appendMksMeasureMetadata(StringBuilder out, Document doc, String voiceId) {
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        if (part == null) {
            return;
        }
        int measureIndex = 0;
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            measureIndex++;
            Element measure = (Element) measureNode;
            StringBuilder meta = new StringBuilder("%@mks measure voice=").append(voiceId)
                    .append(" measure=").append(measureIndex);
            String number = String.valueOf(measure.getAttribute("number") == null ? "" : measure.getAttribute("number"))
                    .trim();
            if (!number.isEmpty()) {
                meta.append(" number=").append(number);
            }
            String implicit = String.valueOf(measure.getAttribute("implicit") == null ? "" : measure.getAttribute("implicit"))
                    .trim().toLowerCase();
            boolean isImplicit = "yes".equals(implicit) || "true".equals(implicit) || "1".equals(implicit);
            meta.append(" implicit=").append(isImplicit ? "1" : "0");

            Element leftBarline = directChildWithAttribute(measure, "barline", "location", "left");
            Element rightBarline = directChildWithAttribute(measure, "barline", "location", "right");
            if (directChildWithAttribute(leftBarline, "repeat", "direction", "forward") != null) {
                meta.append(" repeat=forward");
            } else if (directChildWithAttribute(rightBarline, "repeat", "direction", "backward") != null) {
                meta.append(" repeat=backward");
                Element ending = directChildWithAttribute(rightBarline, "ending", "type", "stop");
                Integer times = parseOptionalIntegerText(ending == null ? "" : ending.getAttribute("number"));
                if (times != null && times.intValue() > 1) {
                    meta.append(" times=").append(times.intValue());
                }
            }

            Element attributes = firstDirectChild(measure, "attributes");
            Element time = firstDirectChild(attributes, "time");
            if (time != null) {
                meta.append(" explicitTime=1");
                Integer beats = parseOptionalIntegerText(directChildText(time, "beats"));
                Integer beatType = parseOptionalIntegerText(directChildText(time, "beat-type"));
                if (beats != null && beats.intValue() > 0) {
                    meta.append(" beats=").append(beats.intValue());
                }
                if (beatType != null && beatType.intValue() > 0) {
                    meta.append(" beatType=").append(beatType.intValue());
                }
            }

            boolean hasLeftDouble = "light-light".equals(directChildText(leftBarline, "bar-style").toLowerCase());
            boolean hasRightDouble = "light-light".equals(directChildText(rightBarline, "bar-style").toLowerCase());
            if (hasLeftDouble && hasRightDouble) {
                meta.append(" doubleBar=both");
            } else if (hasLeftDouble) {
                meta.append(" doubleBar=left");
            } else if (hasRightDouble) {
                meta.append(" doubleBar=right");
            }
            out.append("% ").append(meta).append('\n');
            appendMksOctaveShiftMetadata(out, measure, voiceId, measureIndex);
        }
    }

    private static void appendMksOctaveShiftMetadata(StringBuilder out, Element measure, String voiceId,
            int measureIndex) {
        if (measure == null) {
            return;
        }
        for (Node directionNode = measure.getFirstChild(); directionNode != null; directionNode = directionNode
                .getNextSibling()) {
            if (!(directionNode instanceof Element) || !"direction".equals(((Element) directionNode).getTagName())) {
                continue;
            }
            Element directionType = firstDirectChild((Element) directionNode, "direction-type");
            Element octaveShift = firstDirectChild(directionType, "octave-shift");
            if (octaveShift == null) {
                continue;
            }
            String type = octaveShift.getAttribute("type").trim().toLowerCase();
            if (!"up".equals(type) && !"down".equals(type) && !"stop".equals(type)) {
                continue;
            }
            out.append("% %@mks octshift voice=").append(voiceId)
                    .append(" measure=").append(measureIndex)
                    .append(" type=").append(type);
            Integer size = parseOptionalIntegerText(octaveShift.getAttribute("size"));
            if (size != null && size.intValue() > 0) {
                out.append(" size=").append(size.intValue());
            }
            Integer number = parseOptionalIntegerText(octaveShift.getAttribute("number"));
            if (number != null && number.intValue() > 0) {
                out.append(" number=").append(number.intValue());
            }
            out.append('\n');
        }
    }

    private static void appendMksEventMetadata(StringBuilder out, Document doc, String voiceId) {
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        if (part == null) {
            return;
        }
        int measureIndex = 0;
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            measureIndex++;
            int eventIndex = 0;
            Element measure = (Element) measureNode;
            for (Node noteNode = measure.getFirstChild(); noteNode != null; noteNode = noteNode.getNextSibling()) {
                if (!(noteNode instanceof Element) || !"note".equals(((Element) noteNode).getTagName())) {
                    continue;
                }
                Element note = (Element) noteNode;
                if (firstDirectChild(note, "chord") != null || firstDirectChild(note, "rest") != null) {
                    continue;
                }
                eventIndex++;
                List<String> articulations = new ArrayList<String>();
                Element notations = firstDirectChild(note, "notations");
                Element articulationNode = firstDirectChild(notations, "articulations");
                if (firstDirectChild(articulationNode, "staccato") != null) {
                    articulations.add("staccato");
                }
                if (firstDirectChild(articulationNode, "accent") != null) {
                    articulations.add("accent");
                }
                if (!articulations.isEmpty()) {
                    out.append("% %@mks articul voice=").append(voiceId)
                            .append(" measure=").append(measureIndex)
                            .append(" event=").append(eventIndex)
                            .append(" kind=").append(joinComma(articulations))
                            .append('\n');
                }
                String accidental = directChildText(note, "accidental").toLowerCase();
                if (!accidental.isEmpty()) {
                    out.append("% %@mks accidental voice=").append(voiceId)
                            .append(" measure=").append(measureIndex)
                            .append(" event=").append(eventIndex)
                            .append(" value=").append(accidental)
                            .append('\n');
                }
                Element grace = firstDirectChild(note, "grace");
                if (grace != null) {
                    out.append("% %@mks grace voice=").append(voiceId)
                            .append(" measure=").append(measureIndex)
                            .append(" event=").append(eventIndex)
                            .append(" slash=").append("yes".equals(grace.getAttribute("slash")) ? "1" : "0")
                            .append('\n');
                }
                String tupletMetadata = musicXmlTupletMetadata(note, voiceId, measureIndex, eventIndex);
                if (!tupletMetadata.isEmpty()) {
                    out.append("% ").append(tupletMetadata).append('\n');
                }
                appendMksTrillMetadata(out, note, voiceId, measureIndex, eventIndex);
            }
        }
    }

    private static void appendMksTrillMetadata(StringBuilder out, Element note, String voiceId, int measureIndex,
            int eventIndex) {
        Element notations = firstDirectChild(note, "notations");
        Element ornaments = firstDirectChild(notations, "ornaments");
        if (firstDirectChild(ornaments, "trill-mark") != null) {
            out.append("% %@mks trill voice=").append(voiceId)
                    .append(" measure=").append(measureIndex)
                    .append(" event=").append(eventIndex)
                    .append(" mark=1\n");
        }
        if (ornaments == null) {
            return;
        }
        for (Node child = ornaments.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element) || !"wavy-line".equals(((Element) child).getTagName())) {
                continue;
            }
            Element wavy = (Element) child;
            String type = wavy.getAttribute("type").trim().toLowerCase();
            if (!"start".equals(type) && !"stop".equals(type)) {
                continue;
            }
            out.append("% %@mks trill voice=").append(voiceId)
                    .append(" measure=").append(measureIndex)
                    .append(" event=").append(eventIndex)
                    .append(" wavy=").append(type);
            Integer number = parseOptionalIntegerText(wavy.getAttribute("number"));
            if (number != null && number.intValue() > 0) {
                out.append(" number=").append(number.intValue());
            }
            out.append('\n');
        }
    }

    private static String musicXmlTupletMetadata(Element note, String voiceId, int measureIndex, int eventIndex) {
        Element timeModification = firstDirectChild(note, "time-modification");
        Element notations = firstDirectChild(note, "notations");
        Element tuplet = firstDirectChild(notations, "tuplet");
        Integer actual = parseOptionalIntegerText(directChildText(timeModification, "actual-notes"));
        Integer normal = parseOptionalIntegerText(directChildText(timeModification, "normal-notes"));
        String tupletType = tuplet == null ? "" : tuplet.getAttribute("type").trim().toLowerCase();
        Integer number = parseOptionalIntegerText(tuplet == null ? "" : tuplet.getAttribute("number"));
        if (actual == null && normal == null && !"start".equals(tupletType) && !"stop".equals(tupletType)
                && number == null) {
            return "";
        }
        StringBuilder meta = new StringBuilder("%@mks tuplet voice=").append(voiceId)
                .append(" measure=").append(measureIndex)
                .append(" event=").append(eventIndex);
        if (actual != null && actual.intValue() > 0) {
            meta.append(" actual=").append(actual.intValue());
        }
        if (normal != null && normal.intValue() > 0) {
            meta.append(" normal=").append(normal.intValue());
        }
        if ("start".equals(tupletType)) {
            meta.append(" start=1");
        }
        if ("stop".equals(tupletType)) {
            meta.append(" stop=1");
        }
        if (number != null && number.intValue() > 0) {
            meta.append(" number=").append(number.intValue());
        }
        return meta.toString();
    }

    private static void appendMksSlurMetadata(StringBuilder out, Document doc, String voiceId) {
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        if (part == null) {
            return;
        }
        int measureIndex = 0;
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            measureIndex++;
            int eventIndex = 0;
            Element measure = (Element) measureNode;
            for (Node noteNode = measure.getFirstChild(); noteNode != null; noteNode = noteNode.getNextSibling()) {
                if (!(noteNode instanceof Element) || !"note".equals(((Element) noteNode).getTagName())) {
                    continue;
                }
                eventIndex++;
                Element slur = firstSlur((Element) noteNode);
                if (slur == null || slur.getAttribute("type").isEmpty()) {
                    continue;
                }
                out.append("%@mks slur voice=").append(voiceId)
                        .append(" measure=").append(measureIndex)
                        .append(" event=").append(eventIndex)
                        .append(" type=").append(slur.getAttribute("type"))
                        .append('\n');
            }
        }
    }

    private static Element firstSlur(Element note) {
        Element notations = firstDirectChild(note, "notations");
        if (notations == null) {
            return null;
        }
        return firstDirectChild(notations, "slur");
    }

    private static String extractMksLanesRoundtripXml(String source) {
        Matcher matcher = Pattern.compile("(?m)^%+@mks\\s+lanes\\b[^\\n]*\\bdata=([^\\s]+)")
                .matcher(String.valueOf(source == null ? "" : source));
        if (!matcher.find()) {
            return "";
        }
        try {
            String xml = new String(Base64.getDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8);
            Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
            return doc == null ? "" : MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
        } catch (Exception ex) {
            return "";
        }
    }

    private static String encodeMksLanesRoundtripXml(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return "";
        }
        String xml = MusicXmlIo.serializeMusicXmlDocument(doc);
        return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static String firstMusicXmlPartId(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return "";
        }
        Element part = firstDirectChild(doc.getDocumentElement(), "part");
        return part == null ? "" : part.getAttribute("id");
    }

    private static String firstMusicXmlPartName(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return "";
        }
        Element partList = firstDirectChild(doc.getDocumentElement(), "part-list");
        Element scorePart = firstDirectChild(partList, "score-part");
        return directChildText(scorePart, "part-name");
    }

    private static List<String> firstMusicXmlPartLilyNotes(Document doc, String staffNumber) {
        List<String> notes = new ArrayList<String>();
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        if (part == null) {
            return notes;
        }
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            Element measure = (Element) measureNode;
            notes.addAll(musicXmlMeasureLilyNotes(measure, staffNumber));
        }
        return notes;
    }

    private static List<String> musicXmlMeasureLilyNotes(Element measure, String staffNumber) {
        List<Element> allNotes = directChildren(measure, "note");
        List<Element> staffNotes = filterMusicXmlNotesForStaff(allNotes, staffNumber);
        String selectedVoice = selectedMusicXmlVoiceForLilyExport(measure, staffNotes);
        List<String> out = new ArrayList<String>();
        List<Element> chordGroup = new ArrayList<Element>();
        for (Element note : staffNotes) {
            if (!selectedVoice.isEmpty() && !selectedVoice.equals(directChildText(note, "voice"))) {
                continue;
            }
            if (firstDirectChild(note, "chord") != null) {
                if (!chordGroup.isEmpty()) {
                    chordGroup.add(note);
                }
                continue;
            }
            appendMusicXmlChordGroupToken(out, chordGroup);
            chordGroup = new ArrayList<Element>();
            chordGroup.add(note);
        }
        appendMusicXmlChordGroupToken(out, chordGroup);
        return out;
    }

    private static List<Element> filterMusicXmlNotesForStaff(List<Element> notes, String staffNumber) {
        List<Element> out = new ArrayList<Element>();
        if (staffNumber == null || staffNumber.isEmpty()) {
            out.addAll(notes);
            return out;
        }
        for (Element note : notes) {
            String noteStaffNumber = directChildText(note, "staff");
            if (noteStaffNumber.isEmpty()) {
                noteStaffNumber = "1";
            }
            if (staffNumber.equals(noteStaffNumber)) {
                out.add(note);
            }
        }
        return out;
    }

    private static List<String> musicXmlActivePitchedStaffNumbers(Element part) {
        List<String> out = new ArrayList<String>();
        if (part == null) {
            return out;
        }
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            for (Element note : directChildren((Element) measureNode, "note")) {
                if (firstDirectChild(note, "pitch") == null) {
                    continue;
                }
                String staffNumber = directChildText(note, "staff");
                if (staffNumber.isEmpty()) {
                    staffNumber = "1";
                }
                if (!out.contains(staffNumber)) {
                    out.add(staffNumber);
                }
            }
        }
        return out;
    }

    private static String firstMusicXmlClefForStaff(Document doc, String staffNumber) {
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        if (part == null) {
            return "";
        }
        String targetStaff = staffNumber == null || staffNumber.isEmpty() ? "1" : staffNumber;
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            Element attributes = firstDirectChild((Element) measureNode, "attributes");
            for (Element clef : directChildren(attributes, "clef")) {
                String number = clef.getAttribute("number");
                if (number == null || number.isEmpty()) {
                    number = "1";
                }
                if (targetStaff.equals(number)) {
                    String sign = directChildText(clef, "sign");
                    String line = directChildText(clef, "line");
                    if ("F".equalsIgnoreCase(sign) && "4".equals(line)) {
                        return "bass";
                    }
                    if ("G".equalsIgnoreCase(sign) && "2".equals(line)) {
                        return "treble";
                    }
                    return "";
                }
            }
        }
        return inferMusicXmlClefForStaff(part, targetStaff);
    }

    private static String selectedMusicXmlVoiceForLilyExport(Element measure, List<Element> notes) {
        if (directChildren(measure, "backup").isEmpty()) {
            return "";
        }
        Map<String, Integer> countByVoice = new LinkedHashMap<String, Integer>();
        for (Element note : notes) {
            if (firstDirectChild(note, "chord") != null) {
                continue;
            }
            String voice = directChildText(note, "voice");
            if (voice.isEmpty()) {
                continue;
            }
            Integer count = countByVoice.get(voice);
            countByVoice.put(voice, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        String selected = "";
        int selectedCount = 0;
        for (Map.Entry<String, Integer> entry : countByVoice.entrySet()) {
            if (entry.getValue().intValue() > selectedCount) {
                selected = entry.getKey();
                selectedCount = entry.getValue().intValue();
            }
        }
        return selected;
    }

    private static void appendMusicXmlChordGroupToken(List<String> out, List<Element> chordGroup) {
        if (chordGroup == null || chordGroup.isEmpty()) {
            return;
        }
        if (chordGroup.size() == 1) {
            String note = musicXmlNoteToLilyToken(chordGroup.get(0));
            if (!note.isEmpty()) {
                out.add(note);
            }
            return;
        }
        StringBuilder chord = new StringBuilder("<");
        for (int index = 0; index < chordGroup.size(); index++) {
            if (index > 0) {
                chord.append(' ');
            }
            chord.append(musicXmlNoteToLilyPitchToken(chordGroup.get(index)));
        }
        chord.append(">").append(noteTypeToLilyDuration(directChildText(chordGroup.get(0), "type")));
        out.add(chord.toString());
    }

    private static String musicXmlNoteToLilyToken(Element note) {
        Element pitch = firstDirectChild(note, "pitch");
        String type = directChildText(note, "type");
        String duration = noteTypeToLilyDuration(type);
        if (pitch == null) {
            return "r" + duration;
        }
        return musicXmlNoteToLilyPitchToken(note) + duration;
    }

    private static String musicXmlNoteToLilyPitchToken(Element note) {
        Element pitch = firstDirectChild(note, "pitch");
        if (pitch == null) {
            return "";
        }
        String step = directChildText(pitch, "step");
        int alter = parseIntegerText(directChildText(pitch, "alter"), 0);
        int octave = parseIntegerText(directChildText(pitch, "octave"), 4);
        return lilyPitchFromStepAlterOctave(step, alter, octave);
    }

    private static int parseIntegerText(String text, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(text == null ? "" : text).trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static Integer parseOptionalIntegerText(String text) {
        try {
            return Integer.valueOf(Integer.parseInt(String.valueOf(text == null ? "" : text).trim()));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String joinComma(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(value);
        }
        return out.toString();
    }

    private static String lilyQuoted(String text) {
        return String.valueOf(text == null ? "" : text).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String convertLilyStaffBlocksToMusicXml(String source, String title, String composer, String meter,
            String key, List<LilyStaffBlock> staffBlocks) {
        StringBuilder abc = new StringBuilder();
        abc.append("X:1\n");
        abc.append("T:").append(title).append('\n');
        abc.append("M:").append(meter).append('\n');
        abc.append("L:1/8\n");
        for (int index = 0; index < staffBlocks.size(); index++) {
            LilyStaffBlock staff = staffBlocks.get(index);
            String clef = parseClefText(staff.getContent());
            if (clef.isEmpty()) {
                clef = inferClefText(staff.getContent());
            }
            appendLilyVoiceHeader(abc, index + 1, staff.getPartName(), clef);
        }
        for (int index = 0; index < staffBlocks.size(); index++) {
            LilyTransposeHint transpose = staffBlocks.get(index).getTranspose();
            if (transpose != null) {
                appendLilyTransposeMeta(abc, index + 1, transpose);
            }
        }
        abc.append("K:").append(key).append('\n');
        boolean hasPlayableBody = false;
        for (int index = 0; index < staffBlocks.size(); index++) {
            String abcBody = parseLilyBodyToAbc(staffBlocks.get(index).getContent());
            if (abcBody.trim().isEmpty()) {
                continue;
            }
            hasPlayableBody = true;
            abc.append("V:").append(index + 1).append('\n');
            abc.append(abcBody).append('\n');
        }
        if (!hasPlayableBody) {
            throw noParseableLilyPondEvents();
        }
        String xml = MusicXmlIo.normalizeImportedMusicXmlText(
                AbcIo.musicXmlFromAbc(abc.toString(), new AbcIo.AbcImportOptions(Boolean.FALSE, Boolean.FALSE,
                        Boolean.FALSE, null)));
        xml = addLilyOverfullCarryDiagnostics(xml);
        xml = addNativeRepeatVoltaToMusicXml(xml, staffBlocks.get(0).getContent());
        xml = addNativeAlternativeEndingsToMusicXml(xml, staffBlocks.get(0).getContent());
        xml = addNativePedalCommandsToMusicXml(xml, staffBlocks.get(0).getContent());
        return composer == null || composer.isEmpty() ? xml : addComposerToMusicXml(xml, composer);
    }

    private static String addLilyOverfullCarryDiagnostics(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        boolean changed = false;
        for (int index = 0; index < doc.getElementsByTagName("miscellaneous-field").getLength(); index++) {
            Element field = (Element) doc.getElementsByTagName("miscellaneous-field").item(index);
            String text = field.getTextContent() == null ? "" : field.getTextContent();
            if (text.indexOf("code=OVERFULL_REFLOWED") < 0) {
                continue;
            }
            field.setTextContent("level=warn;code=LILYPOND_IMPORT_WARNING;fmt=lilypond"
                    + ";message=staff 1: overfull measure; carried event to next measure.");
            changed = true;
        }
        return changed ? MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc)) : xml;
    }

    private static IllegalArgumentException noParseableLilyPondEvents() {
        return new IllegalArgumentException("No parseable notes/rests were found in LilyPond source.");
    }


    /**
     * Mirrors the direct Node importer policy for an explicit {@code \new Staff}
     * with no clef: a genuinely wide pitch range becomes two score parts, with
     * events classified by the shared hysteresis policy. Bare Lily blocks never
     * reach this method, matching the upstream conservative single-staff rule.
     */
    private static String autoSplitLilyPondWideRangeStaff(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        Element root = doc.getDocumentElement();
        List<Element> parts = directChildren(root, "part");
        if (parts.size() != 1) {
            return xml;
        }
        Element upperPart = parts.get(0);
        List<Integer> allKeys = collectLilyPondPartKeys(upperPart);
        if (!StaffClefPolicy.shouldUseGrandStaffByRange(allKeys)) {
            return xml;
        }

        List<List<Integer>> assignmentsByMeasure = new ArrayList<List<Integer>>();
        Integer previousStaff = null;
        boolean upperHasPitch = false;
        boolean lowerHasPitch = false;
        for (Element measure : directChildren(upperPart, "measure")) {
            List<Integer> assignments = new ArrayList<Integer>();
            for (List<Element> group : collectLilyPondMeasureNoteGroups(measure)) {
                List<Integer> groupKeys = collectLilyPondNoteGroupKeys(group);
                if (groupKeys.isEmpty()) {
                    assignments.add(Integer.valueOf(1));
                    continue;
                }
                int minKey = groupKeys.get(0).intValue();
                int maxKey = minKey;
                for (Integer key : groupKeys) {
                    minKey = Math.min(minKey, key.intValue());
                    maxKey = Math.max(maxKey, key.intValue());
                }
                int staff = StaffClefPolicy.pickStaffForClusterWithHysteresis(minKey, maxKey, previousStaff);
                previousStaff = Integer.valueOf(staff);
                assignments.add(Integer.valueOf(staff));
                if (staff == 1) {
                    upperHasPitch = true;
                } else {
                    lowerHasPitch = true;
                }
            }
            assignmentsByMeasure.add(assignments);
        }
        if (!upperHasPitch || !lowerHasPitch) {
            return xml;
        }

        Element lowerPart = (Element) upperPart.cloneNode(true);
        List<Element> upperMeasures = directChildren(upperPart, "measure");
        List<Element> lowerMeasures = directChildren(lowerPart, "measure");
        for (int measureIndex = 0; measureIndex < upperMeasures.size(); measureIndex++) {
            List<List<Element>> upperGroups = collectLilyPondMeasureNoteGroups(upperMeasures.get(measureIndex));
            List<List<Element>> lowerGroups = collectLilyPondMeasureNoteGroups(lowerMeasures.get(measureIndex));
            List<Integer> assignments = assignmentsByMeasure.get(measureIndex);
            for (int groupIndex = 0; groupIndex < assignments.size(); groupIndex++) {
                if (assignments.get(groupIndex).intValue() == 1) {
                    removeLilyPondNoteGroup(lowerGroups.get(groupIndex));
                } else {
                    removeLilyPondNoteGroup(upperGroups.get(groupIndex));
                }
            }
            appendLilyPondEmptyMeasureRestIfNeeded(doc, lowerMeasures.get(measureIndex));
            appendLilyPondEmptyMeasureRestIfNeeded(doc, upperMeasures.get(measureIndex));
        }

        String originalPartId = upperPart.getAttribute("id").trim();
        if (originalPartId.isEmpty()) {
            originalPartId = "P1";
        }
        String upperPartId = originalPartId + "_s1";
        String lowerPartId = originalPartId + "_s2";
        upperPart.setAttribute("id", upperPartId);
        lowerPart.setAttribute("id", lowerPartId);
        setLilyPondPartInitialClef(upperPart, "G", "2");
        setLilyPondPartInitialClef(lowerPart, "F", "4");

        Element partList = firstDirectChild(root, "part-list");
        Element scorePart = findLilyPondScorePart(partList, originalPartId);
        if (partList == null || scorePart == null) {
            return xml;
        }
        Element lowerScorePart = (Element) scorePart.cloneNode(true);
        scorePart.setAttribute("id", upperPartId);
        lowerScorePart.setAttribute("id", lowerPartId);
        partList.insertBefore(lowerScorePart, scorePart.getNextSibling());
        root.insertBefore(lowerPart, upperPart.getNextSibling());
        return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
    }

    private static List<Integer> collectLilyPondPartKeys(Element part) {
        List<Integer> keys = new ArrayList<Integer>();
        for (Element measure : directChildren(part, "measure")) {
            for (List<Element> group : collectLilyPondMeasureNoteGroups(measure)) {
                keys.addAll(collectLilyPondNoteGroupKeys(group));
            }
        }
        return keys;
    }

    private static List<List<Element>> collectLilyPondMeasureNoteGroups(Element measure) {
        List<List<Element>> groups = new ArrayList<List<Element>>();
        List<Element> current = null;
        for (Element note : directChildren(measure, "note")) {
            boolean chordContinuation = firstDirectChild(note, "chord") != null;
            if (!chordContinuation || current == null) {
                current = new ArrayList<Element>();
                groups.add(current);
            }
            current.add(note);
        }
        return groups;
    }

    private static List<Integer> collectLilyPondNoteGroupKeys(List<Element> group) {
        List<Integer> keys = new ArrayList<Integer>();
        if (group == null) {
            return keys;
        }
        for (Element note : group) {
            Element pitch = firstDirectChild(note, "pitch");
            if (pitch == null) {
                continue;
            }
            Integer key = pitchToMidiKey(directChildText(pitch, "step"),
                    parseInteger(directChildText(pitch, "alter"), 0),
                    parseInteger(directChildText(pitch, "octave"), 3));
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    private static void removeLilyPondNoteGroup(List<Element> group) {
        if (group == null) {
            return;
        }
        for (Element note : group) {
            if (note != null && note.getParentNode() != null) {
                note.getParentNode().removeChild(note);
            }
        }
    }

    private static void appendLilyPondEmptyMeasureRestIfNeeded(Document doc, Element measure) {
        if (measure == null || !directChildren(measure, "note").isEmpty()) {
            return;
        }
        Element note = doc.createElement("note");
        note.appendChild(doc.createElement("rest"));
        Element duration = doc.createElement("duration");
        duration.setTextContent(Integer.toString(resolveLilyPondMeasureCapacity(measure)));
        note.appendChild(duration);
        Element voice = doc.createElement("voice");
        voice.setTextContent("1");
        note.appendChild(voice);
        Element type = doc.createElement("type");
        type.setTextContent("whole");
        note.appendChild(type);
        measure.appendChild(note);
    }

    private static int resolveLilyPondMeasureCapacity(Element measure) {
        Element attributes = firstDirectChild(measure, "attributes");
        int divisions = parseInteger(directChildText(attributes, "divisions"), 960);
        Element time = firstDirectChild(attributes, "time");
        int beats = parseInteger(directChildText(time, "beats"), 4);
        int beatType = parseInteger(directChildText(time, "beat-type"), 4);
        return Math.max(1, Math.round((divisions * 4.0f * Math.max(1, beats)) / Math.max(1, beatType)));
    }

    private static void setLilyPondPartInitialClef(Element part, String sign, String line) {
        Element firstMeasure = firstDirectChild(part, "measure");
        if (firstMeasure == null) {
            return;
        }
        Element attributes = firstDirectChild(firstMeasure, "attributes");
        if (attributes == null) {
            return;
        }
        Element clef = firstDirectChild(attributes, "clef");
        if (clef == null) {
            clef = part.getOwnerDocument().createElement("clef");
            attributes.appendChild(clef);
        }
        Element signNode = firstDirectChild(clef, "sign");
        if (signNode == null) {
            signNode = part.getOwnerDocument().createElement("sign");
            clef.appendChild(signNode);
        }
        signNode.setTextContent(sign);
        Element lineNode = firstDirectChild(clef, "line");
        if (lineNode == null) {
            lineNode = part.getOwnerDocument().createElement("line");
            clef.appendChild(lineNode);
        }
        lineNode.setTextContent(line);
    }

    private static Element findLilyPondScorePart(Element partList, String partId) {
        for (Element scorePart : directChildren(partList, "score-part")) {
            if (partId.equals(scorePart.getAttribute("id").trim())) {
                return scorePart;
            }
        }
        return null;
    }

    private static String finalizeLilyPondImportedMusicXml(String xml, String rawSource,
            LilyPondImportOptions options) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        LilyPondImportOptions safeOptions = options == null ? new LilyPondImportOptions() : options;
        if (!Boolean.FALSE.equals(safeOptions.getSourceMetadata())) {
            appendLilyPondSourceMetadata(doc, rawSource);
        }
        String normalized = MusicXmlIo.applyImplicitBeamsToMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
        if (!Boolean.FALSE.equals(safeOptions.getDebugPrettyPrint())) {
            return MusicXmlIo.prettyPrintMusicXmlText(normalized);
        }
        Document compactDoc = MusicXmlIo.parseMusicXmlDocument(normalized);
        if (compactDoc == null) {
            return normalized;
        }
        removeLilyPondInsignificantWhitespace(compactDoc);
        return MusicXmlIo.serializeMusicXmlDocument(compactDoc);
    }

    private static void removeLilyPondInsignificantWhitespace(Node node) {
        if (node == null) {
            return;
        }
        List<Node> removable = new ArrayList<Node>();
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty()) {
                removable.add(child);
            } else {
                removeLilyPondInsignificantWhitespace(child);
            }
        }
        for (Node child : removable) {
            node.removeChild(child);
        }
    }

    private static void appendLilyPondSourceMetadata(Document doc, String source) {
        String raw = source == null ? "" : source;
        if (raw.length() == 0 || doc == null || doc.getDocumentElement() == null) {
            return;
        }
        Element part = firstDirectChild(doc.getDocumentElement(), "part");
        Element measure = firstDirectChild(part, "measure");
        if (measure == null) {
            return;
        }
        Element attributes = firstDirectChild(measure, "attributes");
        if (attributes == null) {
            attributes = doc.createElement("attributes");
            measure.insertBefore(attributes, measure.getFirstChild());
        }
        Element miscellaneous = firstDirectChild(attributes, "miscellaneous");
        if (miscellaneous == null) {
            miscellaneous = doc.createElement("miscellaneous");
            attributes.appendChild(miscellaneous);
        }
        String encoded = raw.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
        int chunkSize = 240;
        int maxChunks = 512;
        List<String> chunks = new ArrayList<String>();
        for (int index = 0; index < encoded.length() && chunks.size() < maxChunks; index += chunkSize) {
            chunks.add(encoded.substring(index, Math.min(encoded.length(), index + chunkSize)));
        }
        int capturedLength = 0;
        for (String chunk : chunks) {
            capturedLength += chunk.length();
        }
        boolean truncated = capturedLength < encoded.length();
        appendLilyPondMiscField(doc, miscellaneous, "mks:src:lilypond:raw-encoding", "escape-v1");
        appendLilyPondMiscField(doc, miscellaneous, "mks:src:lilypond:raw-length", Integer.toString(raw.length()));
        appendLilyPondMiscField(doc, miscellaneous, "mks:src:lilypond:raw-encoded-length",
                Integer.toString(encoded.length()));
        appendLilyPondMiscField(doc, miscellaneous, "mks:src:lilypond:raw-chunks",
                Integer.toString(chunks.size()));
        appendLilyPondMiscField(doc, miscellaneous, "mks:src:lilypond:raw-truncated", truncated ? "1" : "0");
        for (int index = 0; index < chunks.size(); index++) {
            appendLilyPondMiscField(doc, miscellaneous, "mks:src:lilypond:raw-" + lilyPondZeroPad(index + 1, 4),
                    chunks.get(index));
        }
    }

    private static String lilyPondZeroPad(int value, int width) {
        String text = Integer.toString(Math.max(0, value));
        StringBuilder out = new StringBuilder();
        for (int index = text.length(); index < Math.max(1, width); index++) {
            out.append('0');
        }
        return out.append(text).toString();
    }

    private static void appendLilyPondMiscField(Document doc, Element miscellaneous, String name, String value) {
        Element field = doc.createElement("miscellaneous-field");
        field.setAttribute("name", name);
        field.setTextContent(value == null ? "" : value);
        miscellaneous.appendChild(field);
    }

    private static void appendLilyVoiceHeader(StringBuilder abc, int voiceNumber, String partName, String clef) {
        if ((partName == null || partName.isEmpty()) && (clef == null || clef.isEmpty())) {
            return;
        }
        abc.append("V:").append(voiceNumber);
        if (partName != null && !partName.isEmpty()) {
            abc.append(" name=\"").append(abcQuoted(partName)).append("\"");
        }
        if (clef != null && !clef.isEmpty()) {
            abc.append(" clef=").append(clef);
        }
        abc.append('\n');
    }

    private static void appendLilyTransposeMeta(StringBuilder abc, int voiceNumber, LilyTransposeHint transpose) {
        abc.append("%@mks transpose voice=").append(voiceNumber);
        if (transpose.getChromatic() != null) {
            abc.append(" chromatic=").append(transpose.getChromatic().intValue());
        }
        if (transpose.getDiatonic() != null) {
            abc.append(" diatonic=").append(transpose.getDiatonic().intValue());
        }
        abc.append('\n');
    }

    private static String addNativeLyricsToMusicXml(String xml, String source) {
        LilyLyricBlock lyricBlock = parseNativeLyricsBlock(source);
        if (lyricBlock.getWords().isEmpty()) {
            return xml;
        }
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        List<Element> notes = musicXmlPitchedNotes(doc, lyricBlock.getTargetPartName());
        int count = Math.min(lyricBlock.getWords().size(), notes.size());
        for (int index = 0; index < count; index++) {
            Element note = notes.get(index);
            if (firstDirectChild(note, "lyric") != null) {
                continue;
            }
            Element lyric = doc.createElement("lyric");
            Element syllabic = doc.createElement("syllabic");
            syllabic.setTextContent("single");
            Element text = doc.createElement("text");
            text.setTextContent(lyricBlock.getWords().get(index));
            lyric.appendChild(syllabic);
            lyric.appendChild(text);
            note.appendChild(lyric);
        }
        return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
    }

    private static LilyLyricBlock parseNativeLyricsBlock(String source) {
        List<String> lyrics = parseLilyCommandWords(source, "\\\\addlyrics");
        if (!lyrics.isEmpty()) {
            return new LilyLyricBlock("", lyrics);
        }
        lyrics = parseLilyCommandWords(source, "\\\\lyricmode");
        if (!lyrics.isEmpty()) {
            return new LilyLyricBlock("", lyrics);
        }
        String target = parseLyricstoTarget(source);
        return new LilyLyricBlock(target, parseLilyCommandWords(source, "\\\\lyricsto\\s+\"[^\"]*\""));
    }

    private static String parseLyricstoTarget(String source) {
        Matcher matcher = Pattern.compile("\\\\lyricsto\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
                .matcher(String.valueOf(source == null ? "" : source));
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static List<String> parseLilyCommandWords(String source, String commandPattern) {
        List<String> lyrics = new ArrayList<String>();
        BalancedBlock block = findCommandBlock(String.valueOf(source == null ? "" : source), commandPattern);
        if (block == null) {
            return lyrics;
        }
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|\\S+").matcher(stripLilyComments(block.getContent()));
        while (matcher.find()) {
            String token = matcher.group(1) == null ? matcher.group() : matcher.group(1);
            token = token == null ? "" : token.trim();
            if (!token.isEmpty() && !token.startsWith("\\")) {
                lyrics.add(token);
            }
        }
        return lyrics;
    }

    private static List<Element> musicXmlPitchedNotes(Document doc, String targetPartName) {
        List<Element> notes = new ArrayList<Element>();
        Element part = targetPartName == null || targetPartName.isEmpty() ? firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part")
                : musicXmlPartByName(doc, targetPartName);
        if (part == null) {
            return notes;
        }
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            Element measure = (Element) measureNode;
            for (Node noteNode = measure.getFirstChild(); noteNode != null; noteNode = noteNode.getNextSibling()) {
                if (noteNode instanceof Element && "note".equals(((Element) noteNode).getTagName())
                        && firstDirectChild((Element) noteNode, "pitch") != null) {
                    notes.add((Element) noteNode);
                }
            }
        }
        return notes;
    }

    private static Element musicXmlPartByName(Document doc, String partName) {
        if (doc == null || doc.getDocumentElement() == null) {
            return null;
        }
        String targetId = "";
        Element partList = firstDirectChild(doc.getDocumentElement(), "part-list");
        if (partList != null) {
            for (Node child = partList.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof Element && "score-part".equals(((Element) child).getTagName())
                        && partName.equals(directChildText((Element) child, "part-name"))) {
                    targetId = ((Element) child).getAttribute("id");
                    break;
                }
            }
        }
        if (targetId.isEmpty()) {
            return null;
        }
        for (Node child = doc.getDocumentElement().getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && "part".equals(((Element) child).getTagName())
                    && targetId.equals(((Element) child).getAttribute("id"))) {
                return (Element) child;
            }
        }
        return null;
    }

    private static String addNativeRepeatVoltaToMusicXml(String xml, String body) {
        Integer repeatTimes = parseNativeRepeatVoltaTimes(body);
        if (repeatTimes == null) {
            return xml;
        }
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        Element firstMeasure = firstMusicXmlMeasure(doc);
        if (firstMeasure == null) {
            return xml;
        }
        Element leftBarline = directChildWithAttribute(firstMeasure, "barline", "location", "left");
        if (leftBarline == null) {
            leftBarline = doc.createElement("barline");
            leftBarline.setAttribute("location", "left");
            Node insertBefore = firstDirectChild(firstMeasure, "note");
            if (insertBefore == null) {
                firstMeasure.appendChild(leftBarline);
            } else {
                firstMeasure.insertBefore(leftBarline, insertBefore);
            }
        }
        if (directChildWithAttribute(leftBarline, "repeat", "direction", "forward") == null) {
            Element repeat = doc.createElement("repeat");
            repeat.setAttribute("direction", "forward");
            leftBarline.appendChild(repeat);
        }

        Element rightBarline = directChildWithAttribute(firstMeasure, "barline", "location", "right");
        if (rightBarline == null) {
            rightBarline = doc.createElement("barline");
            rightBarline.setAttribute("location", "right");
            firstMeasure.appendChild(rightBarline);
        }
        if (directChildWithAttribute(rightBarline, "repeat", "direction", "backward") == null) {
            Element repeat = doc.createElement("repeat");
            repeat.setAttribute("direction", "backward");
            repeat.setAttribute("times", String.valueOf(repeatTimes.intValue()));
            rightBarline.appendChild(repeat);
        }
        if (directChildWithAttribute(rightBarline, "ending", "type", "stop") == null) {
            Element ending = doc.createElement("ending");
            ending.setAttribute("number", String.valueOf(repeatTimes.intValue()));
            ending.setAttribute("type", "stop");
            rightBarline.appendChild(ending);
        }
        return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
    }

    private static Integer parseNativeRepeatVoltaTimes(String body) {
        Matcher matcher = Pattern.compile("\\\\repeat\\s+volta\\s+(\\d+)\\s*\\{", Pattern.CASE_INSENSITIVE)
                .matcher(stripLilyComments(String.valueOf(body == null ? "" : body)));
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(parsePositiveInt(matcher.group(1), 2));
    }

    private static String addNativeAlternativeEndingsToMusicXml(String xml, String body) {
        int endingCount = countNativeAlternativeEndings(body);
        if (endingCount <= 0) {
            return xml;
        }
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        List<Element> measures = firstMusicXmlPartMeasures(doc);
        if (measures.isEmpty()) {
            return xml;
        }
        for (int index = 0; index < endingCount; index++) {
            String number = String.valueOf(index + 1);
            Element measure = measures.get(Math.min(index, measures.size() - 1));
            Element leftBarline = directChildWithAttribute(measure, "barline", "location", "left");
            if (leftBarline == null) {
                leftBarline = doc.createElement("barline");
                leftBarline.setAttribute("location", "left");
                Node insertBefore = firstDirectChild(measure, "note");
                if (insertBefore == null) {
                    measure.appendChild(leftBarline);
                } else {
                    measure.insertBefore(leftBarline, insertBefore);
                }
            }
            if (!hasDirectEnding(leftBarline, number, "start")) {
                Element ending = doc.createElement("ending");
                ending.setAttribute("number", number);
                ending.setAttribute("type", "start");
                leftBarline.appendChild(ending);
            }
            Element rightBarline = directChildWithAttribute(measure, "barline", "location", "right");
            if (rightBarline == null) {
                rightBarline = doc.createElement("barline");
                rightBarline.setAttribute("location", "right");
                measure.appendChild(rightBarline);
            }
            if (!hasDirectEnding(rightBarline, number, "stop")) {
                Element ending = doc.createElement("ending");
                ending.setAttribute("number", number);
                ending.setAttribute("type", "stop");
                rightBarline.appendChild(ending);
            }
        }
        return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
    }

    private static int countNativeAlternativeEndings(String body) {
        BalancedBlock block = findCommandBlock(stripLilyComments(String.valueOf(body == null ? "" : body)),
                "\\\\alternative");
        return block == null ? 0 : directBalancedChildBlocks(block.getContent()).size();
    }

    private static boolean hasDirectEnding(Element barline, String number, String type) {
        if (barline == null) {
            return false;
        }
        for (Node child = barline.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && "ending".equals(((Element) child).getTagName())
                    && number.equals(((Element) child).getAttribute("number"))
                    && type.equals(((Element) child).getAttribute("type"))) {
                return true;
            }
        }
        return false;
    }

    private static String addNativePedalCommandsToMusicXml(String xml, String body) {
        Matcher matcher = Pattern.compile("\\\\(?:sustainOn|sustainOff|sostenutoOn|sostenutoOff|unaCorda|treCorde)")
                .matcher(stripLilyComments(String.valueOf(body == null ? "" : body)));
        if (!matcher.find()) {
            return xml;
        }
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        Element firstMeasure = firstMusicXmlMeasure(doc);
        if (firstMeasure == null) {
            return xml;
        }
        Node insertBefore = firstDirectChild(firstMeasure, "note");
        matcher.reset();
        while (matcher.find()) {
            Element direction = buildLilyPedalDirection(doc, matcher.group().substring(1));
            if (direction == null) {
                continue;
            }
            if (insertBefore == null) {
                firstMeasure.appendChild(direction);
            } else {
                firstMeasure.insertBefore(direction, insertBefore);
            }
        }
        return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
    }

    private static Element firstMusicXmlMeasure(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return null;
        }
        Element part = firstDirectChild(doc.getDocumentElement(), "part");
        return firstDirectChild(part, "measure");
    }

    private static List<Element> firstMusicXmlPartMeasures(Document doc) {
        List<Element> measures = new ArrayList<Element>();
        Element part = firstDirectChild(doc == null ? null : doc.getDocumentElement(), "part");
        if (part == null) {
            return measures;
        }
        for (Node child = part.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && "measure".equals(((Element) child).getTagName())) {
                measures.add((Element) child);
            }
        }
        return measures;
    }

    private static Element buildLilyPedalDirection(Document doc, String command) {
        String type = "";
        String number = "";
        String words = "";
        if ("sustainOn".equals(command)) {
            type = "start";
            number = "1";
        } else if ("sustainOff".equals(command)) {
            type = "stop";
            number = "1";
        } else if ("sostenutoOn".equals(command)) {
            type = "start";
            number = "2";
            words = "sost. ped.";
        } else if ("sostenutoOff".equals(command)) {
            type = "stop";
            number = "2";
        } else if ("unaCorda".equals(command)) {
            type = "start";
            number = "3";
            words = "una corda";
        } else if ("treCorde".equals(command)) {
            type = "stop";
            number = "3";
            words = "tre corde";
        } else {
            return null;
        }
        Element direction = doc.createElement("direction");
        Element directionType = doc.createElement("direction-type");
        Element pedal = doc.createElement("pedal");
        pedal.setAttribute("type", type);
        pedal.setAttribute("number", number);
        directionType.appendChild(pedal);
        direction.appendChild(directionType);
        if (!words.isEmpty()) {
            Element wordsType = doc.createElement("direction-type");
            Element wordsElement = doc.createElement("words");
            wordsElement.setTextContent(words);
            wordsType.appendChild(wordsElement);
            direction.appendChild(wordsType);
        }
        return direction;
    }

    private static String parseHeaderField(String source, String field) {
        BalancedBlock header = findCommandBlock(source, "\\\\header");
        if (header == null) {
            return "";
        }
        Pattern pattern = Pattern.compile(Pattern.quote(field) + "\\s*=\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(header.getContent());
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String parseFirstStaffName(String source) {
        Matcher matcher = Pattern.compile("\\\\new\\s+Staff\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
                .matcher(String.valueOf(source == null ? "" : source));
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String abcQuoted(String text) {
        return String.valueOf(text == null ? "" : text).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String addComposerToMusicXml(String xml, String composer) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return xml;
        }
        Element root = doc.getDocumentElement();
        Element identification = firstDirectChild(root, "identification");
        if (identification == null) {
            identification = doc.createElement("identification");
            Element partList = firstDirectChild(root, "part-list");
            if (partList != null) {
                root.insertBefore(identification, partList);
            } else {
                root.appendChild(identification);
            }
        }
        for (Node child = identification.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && "creator".equals(((Element) child).getTagName())
                    && "composer".equals(((Element) child).getAttribute("type"))) {
                child.setTextContent(composer);
                return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
            }
        }
        Element creator = doc.createElement("creator");
        creator.setAttribute("type", "composer");
        creator.setTextContent(composer);
        identification.appendChild(creator);
        return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.serializeMusicXmlDocument(doc));
    }

    private static Element firstDirectChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                out.add((Element) child);
            }
        }
        return out;
    }

    private static String directChildText(Element parent, String tagName) {
        Element child = firstDirectChild(parent, tagName);
        return child == null ? "" : child.getTextContent().trim();
    }

    private static Element directChildWithAttribute(Element parent, String tagName, String attributeName,
            String attributeValue) {
        if (parent == null) {
            return null;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())
                    && attributeValue.equals(((Element) child).getAttribute(attributeName))) {
                return (Element) child;
            }
        }
        return null;
    }

    private static String parseTimeSignatureText(String source) {
        Matcher matcher = Pattern.compile("\\\\time\\s+(\\d+)\\s*/\\s*(\\d+)").matcher(source);
        if (!matcher.find()) {
            return "4/4";
        }
        int beats = parsePositiveInt(matcher.group(1), 4);
        int beatType = parsePositiveInt(matcher.group(2), 4);
        return beats + "/" + beatType;
    }

    private static String parseKeySignatureText(String source) {
        Matcher matcher = Pattern.compile("\\\\key\\s+([a-g](?:is|es)?)\\s+\\\\(major|minor)", Pattern.CASE_INSENSITIVE)
                .matcher(source);
        if (!matcher.find()) {
            return "C";
        }
        String tonic = matcher.group(1).toLowerCase();
        String note;
        if ("cis".equals(tonic)) {
            note = "C#";
        } else if ("fis".equals(tonic)) {
            note = "F#";
        } else if ("bes".equals(tonic)) {
            note = "Bb";
        } else if ("ees".equals(tonic)) {
            note = "Eb";
        } else if (tonic.endsWith("is")) {
            note = tonic.substring(0, 1).toUpperCase() + "#";
        } else if (tonic.endsWith("es")) {
            note = tonic.substring(0, 1).toUpperCase() + "b";
        } else {
            note = tonic.toUpperCase();
        }
        if ("minor".equalsIgnoreCase(matcher.group(2))) {
            note += "m";
        }
        return note;
    }

    private static String parseClefText(String body) {
        Matcher matcher = Pattern.compile("\\\\clef\\s+\"?([A-Za-z0-9]+)\"?", Pattern.CASE_INSENSITIVE)
                .matcher(String.valueOf(body == null ? "" : body));
        if (!matcher.find()) {
            return "";
        }
        String clef = matcher.group(1) == null ? "" : matcher.group(1).trim().toLowerCase();
        if ("bass".equals(clef) || "alto".equals(clef) || "tenor".equals(clef) || "treble".equals(clef)) {
            return clef;
        }
        return "";
    }

    private static String inferClefText(String body) {
        Matcher matcher = Pattern.compile("[a-g](?:isis|eses|is|es)?[,']*")
                .matcher(stripUnsupportedLilyCommands(stripLilyComments(body)));
        int count = 0;
        int highest = Integer.MIN_VALUE;
        while (matcher.find()) {
            Integer midiKey = lilyPitchTokenToMidiKey(matcher.group());
            if (midiKey != null) {
                count++;
                highest = Math.max(highest, midiKey.intValue());
            }
        }
        return count > 0 ? (highest < 60 ? "bass" : "treble") : "";
    }

    private static Integer lilyPitchTokenToMidiKey(String token) {
        Matcher matcher = Pattern.compile("^([a-g])(isis|eses|is|es)?([,']*)$").matcher(token);
        if (!matcher.matches()) {
            return null;
        }
        String accidentalText = matcher.group(2) == null ? "" : matcher.group(2);
        String octaveMarks = matcher.group(3) == null ? "" : matcher.group(3);
        int octave = 3 + countChar(octaveMarks, '\'') - countChar(octaveMarks, ',');
        int alter = 0;
        if ("is".equals(accidentalText)) {
            alter = 1;
        } else if ("isis".equals(accidentalText)) {
            alter = 2;
        } else if ("es".equals(accidentalText)) {
            alter = -1;
        } else if ("eses".equals(accidentalText)) {
            alter = -2;
        }
        return pitchToMidiKey(matcher.group(1).toUpperCase(), alter, octave);
    }

    private static String extractFirstMusicBody(String source) {
        LilyStaffBlock staff = extractFirstStaffBlock(source);
        if (staff != null) {
            return staff.getContent();
        }
        String stripped = removeCommandBlock(source, "\\\\header");
        stripped = removeCommandBlock(stripped, "\\\\paper");
        stripped = removeCommandBlock(stripped, "\\\\layout");
        BalancedBlock first = findFirstBlock(stripped);
        return first == null ? "" : first.getContent();
    }

    private static LilyStaffBlock extractFirstStaffBlock(String source) {
        List<LilyStaffBlock> staffBlocks = extractStaffBlocks(source);
        return staffBlocks.isEmpty() ? null : staffBlocks.get(0);
    }

    private static List<LilyStaffBlock> extractStaffBlocks(String source) {
        List<LilyStaffBlock> staffBlocks = new ArrayList<LilyStaffBlock>();
        String text = String.valueOf(source == null ? "" : source);
        Matcher matcher = Pattern.compile("\\\\new\\s+Staff(?:\\s*=\\s*\"([^\"]*)\")?", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        while (matcher.find()) {
            String staffName = matcher.group(1) == null ? "" : matcher.group(1).trim();
            String withPartName = "";
            LilyTransposeHint withTranspose = null;
            int cursor = skipWhitespace(text, matcher.end());
            if (text.regionMatches(true, cursor, "\\with", 0, "\\with".length())) {
                cursor = skipWhitespace(text, cursor + "\\with".length());
                if (cursor < text.length() && text.charAt(cursor) == '{') {
                    BalancedBlock withBlock = findBalancedBlock(text, cursor);
                    if (withBlock != null) {
                        Matcher instrumentMatcher = Pattern.compile("(?:^|[\\s;])instrumentName\\s*=\\s*\"([^\"]*)\"",
                                Pattern.CASE_INSENSITIVE).matcher(withBlock.getContent());
                        if (instrumentMatcher.find()) {
                            withPartName = instrumentMatcher.group(1).trim();
                        }
                        withTranspose = parseBodyTransposition(withBlock.getContent());
                        cursor = skipWhitespace(text, withBlock.getEndPos() + 1);
                    }
                }
            }
            BalancedBlock bodyBlock;
            cursor = skipWhitespace(text, cursor);
            if (text.regionMatches(cursor, "<<", 0, 2)) {
                bodyBlock = findBalancedSimultaneousBlock(text, cursor);
            } else {
                int bodyStart = text.indexOf('{', cursor);
                if (bodyStart < 0) {
                    continue;
                }
                bodyBlock = findBalancedBlock(text, bodyStart);
            }
            if (bodyBlock == null) {
                continue;
            }
            String bodyPartName = parseBodyInstrumentName(bodyBlock.getContent());
            String partName = !withPartName.isEmpty() ? withPartName : (!bodyPartName.isEmpty() ? bodyPartName : staffName);
            LilyTransposeHint bodyTranspose = parseBodyTransposition(bodyBlock.getContent());
            staffBlocks.add(new LilyStaffBlock(partName, bodyBlock.getContent(),
                    withTranspose == null ? bodyTranspose : withTranspose));
        }
        return staffBlocks;
    }

    private static String expandLilyVariables(String source) {
        String text = String.valueOf(source == null ? "" : source);
        Map<String, String> variables = collectLilyVariables(text);
        if (variables.isEmpty()) {
            return text;
        }
        String expanded = text;
        for (int pass = 0; pass < 4; pass++) {
            Matcher matcher = Pattern.compile("\\\\([A-Za-z][A-Za-z0-9]*)\\b").matcher(expanded);
            StringBuffer out = new StringBuffer();
            boolean changed = false;
            while (matcher.find()) {
                String replacement = variables.get(matcher.group(1));
                if (replacement == null) {
                    matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group()));
                    continue;
                }
                matcher.appendReplacement(out, Matcher.quoteReplacement(" " + replacement + " "));
                changed = true;
            }
            matcher.appendTail(out);
            expanded = out.toString();
            if (!changed) {
                break;
            }
        }
        return expanded;
    }

    private static Map<String, String> collectLilyVariables(String source) {
        Map<String, String> variables = new LinkedHashMap<String, String>();
        String text = String.valueOf(source == null ? "" : source);
        Matcher matcher = Pattern.compile("(?m)(?:^|\\s)([A-Za-z][A-Za-z0-9]*)\\s*=\\s*(\\\\relative(?:\\s+[a-g](?:isis|eses|is|es)?[,']*)?\\s*)?\\{",
                Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            int brace = text.indexOf('{', matcher.start());
            if (brace < 0) {
                continue;
            }
            BalancedBlock block = findBalancedBlock(text, brace);
            if (block == null) {
                continue;
            }
            String prefix = matcher.group(2) == null ? "" : matcher.group(2).trim();
            String value = prefix.isEmpty() ? block.getContent() : prefix + " { " + block.getContent() + " }";
            variables.put(matcher.group(1), value);
        }
        return variables;
    }

    private static String parseBodyInstrumentName(String body) {
        Matcher matcher = Pattern.compile("\\\\set\\s+Staff\\.instrumentName\\s*=\\s*\"([^\"]*)\"",
                Pattern.CASE_INSENSITIVE).matcher(String.valueOf(body == null ? "" : body));
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static LilyTransposeHint parseBodyTransposition(String body) {
        Matcher matcher = Pattern.compile("\\\\transposition\\s+([a-g](?:isis|eses|is|es)?[,']*)",
                Pattern.CASE_INSENSITIVE).matcher(String.valueOf(body == null ? "" : body));
        return matcher.find() ? lilyTranspositionTokenToHint(matcher.group(1)) : null;
    }

    private static LilyTransposeHint lilyTranspositionTokenToHint(String token) {
        Matcher matcher = Pattern.compile("^([a-g])(isis|eses|is|es)?([,']*)$", Pattern.CASE_INSENSITIVE)
                .matcher(String.valueOf(token == null ? "" : token).trim());
        if (!matcher.matches()) {
            return null;
        }
        String step = matcher.group(1).toUpperCase();
        String accidentalText = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase();
        String octaveMarks = matcher.group(3) == null ? "" : matcher.group(3);
        int alter = 0;
        if ("is".equals(accidentalText)) {
            alter = 1;
        } else if ("isis".equals(accidentalText)) {
            alter = 2;
        } else if ("es".equals(accidentalText)) {
            alter = -1;
        } else if ("eses".equals(accidentalText)) {
            alter = -2;
        }
        int octaveShift = countChar(octaveMarks, '\'') - countChar(octaveMarks, ',');
        int chromatic = lilyPitchClassToSemitone(step, alter) + octaveShift * 12;
        while (chromatic > 6) {
            chromatic -= 12;
        }
        while (chromatic < -6) {
            chromatic += 12;
        }
        int diatonic = lilyStepToDiatonic(step) + octaveShift * 7;
        while (diatonic > 3) {
            diatonic -= 7;
        }
        while (diatonic < -3) {
            diatonic += 7;
        }
        return new LilyTransposeHint(Integer.valueOf(chromatic), Integer.valueOf(diatonic));
    }

    private static int lilyStepToDiatonic(String step) {
        if ("D".equals(step)) {
            return 1;
        }
        if ("E".equals(step)) {
            return 2;
        }
        if ("F".equals(step)) {
            return 3;
        }
        if ("G".equals(step)) {
            return 4;
        }
        if ("A".equals(step)) {
            return 5;
        }
        if ("B".equals(step)) {
            return 6;
        }
        return 0;
    }

    private static String parseLilyBodyToAbc(String body) {
        LilyRelativeBlock relative = unwrapRelativeBlock(body);
        String playableBody = unwrapTupletBlocks(unwrapAlternativeBlocks(unwrapRepeatVoltaBlocks(relative.getBody())));
        Matcher matcher = Pattern.compile(
                "\\\\(?:startTrillSpan|stopTrillSpan|snappizzicato|sostenutoOff|sostenutoOn|sustainOff|sustainOn|glissando|flageolet|harmonic|treCorde|unaCorda|downbow|upbow|trill|[()<>!]|pppp|ffff|ppp|fff|sfp|sfz|rfz|pp|mp|mf|ff|fp|fz|sf|p|f)|\\(\\d+(?::\\d+)?(?::\\d+)?|[()]|<[^>]+>\\d*\\.{0,3}(?:\\*\\d+)?~?|[a-grs](?:isis|eses|is|es)?[,']*\\d*\\.{0,3}(?:\\*\\d+)?~?|\\d+\\.{0,3}(?:\\*\\d+)?~?|[|:]+")
                .matcher(stripUnsupportedLilyCommands(stripLilyComments(playableBody)));
        StringBuilder out = new StringBuilder();
        int currentDuration = 4;
        LilyRelativeState relativeState = relative.isRelativeMode() ? new LilyRelativeState(relative.getRelativeRoot())
                : null;
        LilyAbcTokenState tokenState = new LilyAbcTokenState();
        while (matcher.find()) {
            String token = matcher.group();
            if (token == null || token.isEmpty()) {
                continue;
            }
            if (token.matches("\\(\\d+(?::\\d+)?(?::\\d+)?")) {
                appendToken(out, token);
                continue;
            }
            if (token.startsWith("|") || token.indexOf(':') >= 0) {
                appendToken(out, "|");
                continue;
            }
            if (token.startsWith("\\") || "(".equals(token) || ")".equals(token)) {
                String decoration = lilyCommandToAbcDecoration(token);
                if (isBetweenLilyNotesCommand(token)) {
                    tokenState.insertPrefixBeforePreviousAbcToken(out, decoration);
                    tokenState.appendPendingAbcPrefix(lilyCommandToNextAbcDecoration(token));
                } else if (isPreviousLilyNoteCommand(token)) {
                    tokenState.insertPrefixBeforePreviousAbcToken(out, decoration);
                } else if (")".equals(decoration)) {
                    appendToken(out, decoration);
                } else if (!decoration.isEmpty()) {
                    tokenState.appendPendingAbcPrefix(decoration);
                }
                continue;
            }
            String abcToken = lilyNoteTokenToAbc(token, currentDuration, relativeState, tokenState);
            Integer duration = lilyTokenDuration(token);
            if (duration != null && duration.intValue() > 0) {
                currentDuration = duration.intValue();
            }
            if (!abcToken.isEmpty()) {
                appendPlayableToken(out, abcToken, tokenState);
            }
        }
        return out.toString().replace(" | |", " |").trim();
    }

    private static String unwrapRepeatVoltaBlocks(String sourceBody) {
        String body = String.valueOf(sourceBody == null ? "" : sourceBody);
        Matcher matcher = Pattern.compile("\\\\repeat\\s+volta\\s+\\d+\\s*\\{", Pattern.CASE_INSENSITIVE).matcher(body);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (matcher.find(cursor)) {
            int bracePos = body.indexOf('{', matcher.start());
            if (bracePos < 0) {
                break;
            }
            BalancedBlock block = findBalancedBlock(body, bracePos);
            if (block == null) {
                break;
            }
            out.append(body.substring(cursor, matcher.start()));
            out.append(' ').append(block.getContent()).append(' ');
            cursor = block.getEndPos() + 1;
        }
        out.append(body.substring(cursor));
        return out.toString();
    }

    private static String unwrapTupletBlocks(String sourceBody) {
        String body = String.valueOf(sourceBody == null ? "" : sourceBody);
        Matcher matcher = Pattern.compile("\\\\tuplet\\s+(\\d+)\\s*/\\s*(\\d+)\\s*\\{", Pattern.CASE_INSENSITIVE)
                .matcher(body);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (matcher.find(cursor)) {
            int bracePos = body.indexOf('{', matcher.start());
            if (bracePos < 0) {
                break;
            }
            BalancedBlock block = findBalancedBlock(body, bracePos);
            if (block == null) {
                break;
            }
            String actual = matcher.group(1);
            String normal = matcher.group(2);
            out.append(body.substring(cursor, matcher.start()));
            out.append(" (").append(actual).append(':').append(normal).append(':').append(actual)
                    .append(' ').append(block.getContent()).append(' ');
            cursor = block.getEndPos() + 1;
        }
        out.append(body.substring(cursor));
        return out.toString();
    }

    private static String unwrapAlternativeBlocks(String sourceBody) {
        String body = String.valueOf(sourceBody == null ? "" : sourceBody);
        Matcher matcher = Pattern.compile("\\\\alternative\\s*\\{", Pattern.CASE_INSENSITIVE).matcher(body);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (matcher.find(cursor)) {
            int bracePos = body.indexOf('{', matcher.start());
            if (bracePos < 0) {
                break;
            }
            BalancedBlock block = findBalancedBlock(body, bracePos);
            if (block == null) {
                break;
            }
            out.append(body.substring(cursor, matcher.start()));
            for (BalancedBlock childBlock : directBalancedChildBlocks(block.getContent())) {
                out.append(' ').append(childBlock.getContent()).append(' ');
            }
            cursor = block.getEndPos() + 1;
        }
        out.append(body.substring(cursor));
        return out.toString();
    }

    private static List<BalancedBlock> directBalancedChildBlocks(String sourceBody) {
        List<BalancedBlock> blocks = new ArrayList<BalancedBlock>();
        String body = String.valueOf(sourceBody == null ? "" : sourceBody);
        int cursor = 0;
        while (cursor < body.length()) {
            int bracePos = body.indexOf('{', cursor);
            if (bracePos < 0) {
                break;
            }
            BalancedBlock block = findBalancedBlock(body, bracePos);
            if (block == null) {
                break;
            }
            blocks.add(block);
            cursor = block.getEndPos() + 1;
        }
        return blocks;
    }

    private static String lilyCommandToAbcDecoration(String token) {
        String command = String.valueOf(token == null ? "" : token).trim();
        if (command.startsWith("\\")) {
            command = command.substring(1);
        }
        if ("(".equals(command)) {
            return "(";
        }
        if (")".equals(command)) {
            return ")";
        }
        if ("<".equals(command)) {
            return "!crescendo(!";
        }
        if (">".equals(command)) {
            return "!diminuendo(!";
        }
        if ("!".equals(command)) {
            return "!diminuendo)!";
        }
        if ("trill".equals(command)) {
            return "!trill!";
        }
        if ("startTrillSpan".equals(command)) {
            return "!trill(!";
        }
        if ("stopTrillSpan".equals(command)) {
            return "!trill)!";
        }
        if ("glissando".equals(command)) {
            return "!gliss-start!";
        }
        if ("upbow".equals(command)) {
            return "!upbow!";
        }
        if ("downbow".equals(command)) {
            return "!downbow!";
        }
        if ("snappizzicato".equals(command)) {
            return "!snap!";
        }
        if ("flageolet".equals(command) || "harmonic".equals(command)) {
            return "!harmonic!";
        }
        if (isSupportedLilyDynamic(command)) {
            return "!" + command + "!";
        }
        return "";
    }

    private static String lilyCommandToNextAbcDecoration(String token) {
        String command = String.valueOf(token == null ? "" : token).trim();
        if (command.startsWith("\\")) {
            command = command.substring(1);
        }
        if ("glissando".equals(command)) {
            return "!gliss-stop!";
        }
        return "";
    }

    private static boolean isBetweenLilyNotesCommand(String token) {
        String command = String.valueOf(token == null ? "" : token).trim();
        if (command.startsWith("\\")) {
            command = command.substring(1);
        }
        return "glissando".equals(command);
    }

    private static boolean isPreviousLilyNoteCommand(String token) {
        String command = String.valueOf(token == null ? "" : token).trim();
        if (command.startsWith("\\")) {
            command = command.substring(1);
        }
        return "trill".equals(command) || "startTrillSpan".equals(command) || "stopTrillSpan".equals(command)
                || "downbow".equals(command);
    }

    private static boolean isSupportedLilyDynamic(String value) {
        String dynamic = value == null ? "" : value.trim().toLowerCase();
        return "pppp".equals(dynamic) || "ppp".equals(dynamic) || "pp".equals(dynamic) || "p".equals(dynamic)
                || "mp".equals(dynamic) || "mf".equals(dynamic) || "f".equals(dynamic) || "ff".equals(dynamic)
                || "fff".equals(dynamic) || "ffff".equals(dynamic) || "fp".equals(dynamic) || "fz".equals(dynamic)
                || "rfz".equals(dynamic) || "sf".equals(dynamic) || "sfp".equals(dynamic)
                || "sfz".equals(dynamic);
    }

    private static String lilyNoteTokenToAbc(String token, int currentDuration) {
        return lilyNoteTokenToAbc(token, currentDuration, null);
    }

    private static String lilyNoteTokenToAbc(String token, int currentDuration, LilyRelativeState relativeState) {
        return lilyNoteTokenToAbc(token, currentDuration, relativeState, new LilyAbcTokenState());
    }

    private static String lilyNoteTokenToAbc(String token, int currentDuration, LilyRelativeState relativeState,
            LilyAbcTokenState tokenState) {
        if (token.startsWith("<") && token.indexOf('>') > 0) {
            return lilyChordTokenToAbc(token, currentDuration, relativeState, tokenState);
        }
        String durationOnly = lilyDurationOnlyTokenToAbc(token, currentDuration, tokenState);
        if (!durationOnly.isEmpty()) {
            return durationOnly;
        }
        Matcher matcher = Pattern.compile("^([a-grs])(isis|eses|is|es)?([,']*)(\\d+)?(\\.*)(?:\\*(\\d+))?(~?)$")
                .matcher(token);
        if (!matcher.matches()) {
            return "";
        }
        String letter = matcher.group(1);
        boolean rest = "r".equals(letter) || "s".equals(letter);
        String accidentalText = matcher.group(2) == null ? "" : matcher.group(2);
        String octaveMarks = matcher.group(3) == null ? "" : matcher.group(3);
        String durationText = matcher.group(4) == null ? "" : matcher.group(4);
        String dotsText = matcher.group(5) == null ? "" : matcher.group(5);
        int multiplier = parsePositiveInt(matcher.group(6), 1);
        boolean tieStart = "~".equals(matcher.group(7));
        int duration = durationText.isEmpty() ? currentDuration : parsePositiveInt(durationText, currentDuration);
        String len = lilyDurationToAbcLen(duration, dotsText.length(), multiplier);
        if (rest) {
            return "z" + len;
        }
        int alter = lilyAccidentalAlter(accidentalText);
        int octave;
        if (relativeState != null) {
            LilyRelativeAnchor resolved = resolveRelativePitch(letter.toUpperCase(), alter,
                    relativeState.getPreviousAnchor());
            resolved = applyLilyOctaveMarks(resolved, octaveMarks, letter.toUpperCase(), alter);
            relativeState.setPreviousAnchor(resolved);
            octave = resolved.getOctave();
        } else {
            octave = 3 + countChar(octaveMarks, '\'') - countChar(octaveMarks, ',');
        }
        String abcPitch = lilyAccidentalToAbc(alter) + abcPitchFromStepOctave(letter.toUpperCase(), octave);
        tokenState.setPreviousAbcPitch(abcPitch);
        return tokenState.consumePendingAbcPrefix() + abcPitch + len
                + (tieStart ? "-" : "");
    }

    private static String lilyDurationOnlyTokenToAbc(String token, int currentDuration, LilyAbcTokenState tokenState) {
        Matcher matcher = Pattern.compile("^(\\d+)(\\.*)(?:\\*(\\d+))?(~?)$").matcher(token);
        if (!matcher.matches() || tokenState == null || tokenState.getPreviousAbcPitch().isEmpty()) {
            return "";
        }
        int duration = parsePositiveInt(matcher.group(1), currentDuration);
        String dotsText = matcher.group(2) == null ? "" : matcher.group(2);
        int multiplier = parsePositiveInt(matcher.group(3), 1);
        boolean tieStart = "~".equals(matcher.group(4));
        return tokenState.consumePendingAbcPrefix() + tokenState.getPreviousAbcPitch()
                + lilyDurationToAbcLen(duration, dotsText.length(), multiplier)
                + (tieStart ? "-" : "");
    }

    private static String lilyChordTokenToAbc(String token, int currentDuration) {
        return lilyChordTokenToAbc(token, currentDuration, null);
    }

    private static String lilyChordTokenToAbc(String token, int currentDuration, LilyRelativeState relativeState) {
        return lilyChordTokenToAbc(token, currentDuration, relativeState, new LilyAbcTokenState());
    }

    private static String lilyChordTokenToAbc(String token, int currentDuration, LilyRelativeState relativeState,
            LilyAbcTokenState tokenState) {
        Matcher matcher = Pattern.compile("^<([^>]+)>(\\d+)?(\\.*)(?:\\*(\\d+))?(~?)$").matcher(token);
        if (!matcher.matches()) {
            return "";
        }
        String body = matcher.group(1) == null ? "" : matcher.group(1).trim();
        String durationText = matcher.group(2) == null ? "" : matcher.group(2);
        String dotsText = matcher.group(3) == null ? "" : matcher.group(3);
        int multiplier = parsePositiveInt(matcher.group(4), 1);
        boolean tieStart = "~".equals(matcher.group(5));
        int duration = durationText.isEmpty() ? currentDuration : parsePositiveInt(durationText, currentDuration);
        StringBuilder members = new StringBuilder();
        LilyRelativeAnchor firstAnchor = null;
        for (String member : body.split("\\s+")) {
            LilyRelativeAnchor[] resolvedAnchor = new LilyRelativeAnchor[1];
            String abcPitch = lilyChordMemberToAbc(member, relativeState, resolvedAnchor);
            if (!abcPitch.isEmpty()) {
                members.append(abcPitch);
                if (firstAnchor == null) {
                    firstAnchor = resolvedAnchor[0];
                }
            }
        }
        if (members.length() == 0) {
            return "";
        }
        if (relativeState != null && firstAnchor != null) {
            relativeState.setPreviousAnchor(firstAnchor);
        }
        String abcChord = "[" + members + "]";
        tokenState.setPreviousAbcPitch(abcChord);
        return tokenState.consumePendingAbcPrefix() + abcChord + lilyDurationToAbcLen(duration, dotsText.length(), multiplier)
                + (tieStart ? "-" : "");
    }

    private static String lilyChordMemberToAbc(String token) {
        return lilyChordMemberToAbc(token, null, null);
    }

    private static String lilyChordMemberToAbc(String token, LilyRelativeState relativeState,
            LilyRelativeAnchor[] resolvedAnchor) {
        Matcher matcher = Pattern.compile("^([a-g])(isis|eses|is|es)?([,']*)$").matcher(token);
        if (!matcher.matches()) {
            return "";
        }
        String accidentalText = matcher.group(2) == null ? "" : matcher.group(2);
        String octaveMarks = matcher.group(3) == null ? "" : matcher.group(3);
        int alter = lilyAccidentalAlter(accidentalText);
        String step = matcher.group(1).toUpperCase();
        int octave;
        if (relativeState != null) {
            LilyRelativeAnchor resolved = resolveRelativePitch(step, alter, relativeState.getPreviousAnchor());
            resolved = applyLilyOctaveMarks(resolved, octaveMarks, step, alter);
            relativeState.setPreviousAnchor(resolved);
            if (resolvedAnchor != null && resolvedAnchor.length > 0) {
                resolvedAnchor[0] = resolved;
            }
            octave = resolved.getOctave();
        } else {
            octave = 3 + countChar(octaveMarks, '\'') - countChar(octaveMarks, ',');
        }
        return lilyAccidentalToAbc(alter) + abcPitchFromStepOctave(step, octave);
    }

    private static Integer lilyTokenDuration(String token) {
        Matcher matcher = Pattern.compile("^(?:<[^>]+>|[a-grs](?:isis|eses|is|es)?[,']*|)(\\d+)?\\.*(?:\\*\\d+)?~?$")
                .matcher(token);
        if (!matcher.matches() || matcher.group(1) == null || matcher.group(1).isEmpty()) {
            return null;
        }
        return Integer.valueOf(parsePositiveInt(matcher.group(1), 4));
    }

    private static LilyRelativeBlock unwrapRelativeBlock(String sourceBody) {
        String body = String.valueOf(sourceBody == null ? "" : sourceBody);
        Matcher matcher = Pattern.compile("\\\\relative(?:\\s+([a-g](?:isis|eses|is|es)?[,']*))?\\s*\\{",
                Pattern.CASE_INSENSITIVE).matcher(body);
        if (!matcher.find()) {
            return new LilyRelativeBlock(body, false, null);
        }
        int bracePos = body.indexOf('{', matcher.start());
        if (bracePos < 0) {
            return new LilyRelativeBlock(body, false, null);
        }
        BalancedBlock block = findBalancedBlock(body, bracePos);
        if (block == null) {
            return new LilyRelativeBlock(body, false, null);
        }
        LilyRelativeAnchor root = parseRelativeRoot(matcher.group(1));
        return new LilyRelativeBlock(block.getContent(), true, root);
    }

    private static LilyRelativeAnchor parseRelativeRoot(String token) {
        LilyPitchToken pitch = parseLilyPitchToken(token);
        if (pitch == null) {
            return null;
        }
        int octave = 3 + countChar(pitch.getOctaveMarks(), '\'') - countChar(pitch.getOctaveMarks(), ',');
        return new LilyRelativeAnchor(pitch.getStep(), octave,
                octave * 12 + lilyPitchClassToSemitone(pitch.getStep(), pitch.getAlter()));
    }

    private static LilyPitchToken parseLilyPitchToken(String token) {
        Matcher matcher = Pattern.compile("^([a-g])(isis|eses|is|es)?([,']*)$", Pattern.CASE_INSENSITIVE)
                .matcher(String.valueOf(token == null ? "" : token).trim());
        if (!matcher.matches()) {
            return null;
        }
        String step = matcher.group(1).toUpperCase();
        String accidentalText = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase();
        String octaveMarks = matcher.group(3) == null ? "" : matcher.group(3);
        return new LilyPitchToken(step, lilyAccidentalAlter(accidentalText), octaveMarks);
    }

    private static LilyRelativeAnchor resolveRelativePitch(String step, int alter, LilyRelativeAnchor previousAnchor) {
        String safeStep = step == null ? "C" : step.trim().toUpperCase();
        if (previousAnchor == null) {
            int fallbackOctave = 3;
            return new LilyRelativeAnchor(safeStep, fallbackOctave,
                    fallbackOctave * 12 + lilyPitchClassToSemitone(safeStep, alter));
        }
        int previousIndex = previousAnchor.getOctave() * 7 + lilyStepToDiatonic(previousAnchor.getStep());
        int targetStepIndex = lilyStepToDiatonic(safeStep);
        int bestOctave = 4;
        int bestIndex = bestOctave * 7 + targetStepIndex;
        int bestMidi = bestOctave * 12 + lilyPitchClassToSemitone(safeStep, alter);
        int bestDistance = Integer.MAX_VALUE;
        for (int octave = 0; octave <= 9; octave++) {
            int index = octave * 7 + targetStepIndex;
            int distance = Math.abs(index - previousIndex);
            int midi = octave * 12 + lilyPitchClassToSemitone(safeStep, alter);
            if (distance < bestDistance || (distance == bestDistance && index > bestIndex)) {
                bestDistance = distance;
                bestOctave = octave;
                bestIndex = index;
                bestMidi = midi;
            }
        }
        return new LilyRelativeAnchor(safeStep, bestOctave, bestMidi);
    }

    private static LilyRelativeAnchor applyLilyOctaveMarks(LilyRelativeAnchor resolved, String octaveMarks, String step,
            int alter) {
        if (resolved == null) {
            return resolveRelativePitch(step, alter, null);
        }
        String marks = octaveMarks == null ? "" : octaveMarks;
        if (marks.isEmpty()) {
            return resolved;
        }
        int octave = resolved.getOctave() + countChar(marks, '\'') - countChar(marks, ',');
        String safeStep = step == null ? resolved.getStep() : step.trim().toUpperCase();
        return new LilyRelativeAnchor(safeStep, octave, octave * 12 + lilyPitchClassToSemitone(safeStep, alter));
    }

    private static int lilyAccidentalAlter(String accidentalText) {
        String text = accidentalText == null ? "" : accidentalText.toLowerCase();
        if ("is".equals(text)) {
            return 1;
        }
        if ("isis".equals(text)) {
            return 2;
        }
        if ("es".equals(text)) {
            return -1;
        }
        if ("eses".equals(text)) {
            return -2;
        }
        return 0;
    }

    private static String lilyAccidentalToAbc(int alter) {
        if (alter > 0) {
            return repeat("^", Math.min(2, alter));
        }
        if (alter < 0) {
            return repeat("_", Math.min(2, Math.abs(alter)));
        }
        return "";
    }

    private static boolean isPowerDuration(int value) {
        return value == 1 || value == 2 || value == 4 || value == 8 || value == 16 || value == 32 || value == 64
                || value == 128;
    }

    private static String repeat(String text, int count) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < count; index++) {
            out.append(text);
        }
        return out.toString();
    }

    private static void appendToken(StringBuilder out, String token) {
        if (out.length() > 0) {
            out.append(' ');
        }
        out.append(token);
    }

    private static void appendPlayableToken(StringBuilder out, String token, LilyAbcTokenState tokenState) {
        if (out.length() > 0) {
            out.append(' ');
        }
        int tokenStart = out.length();
        out.append(token);
        if (tokenState != null) {
            tokenState.setPreviousAbcTokenStart(tokenStart);
        }
    }

    private static int countChar(String text, char ch) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == ch) {
                count++;
            }
        }
        return count;
    }

    private static int parsePositiveInt(String text, int fallback) {
        try {
            int parsed = Integer.parseInt(text);
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static int parseInteger(String text, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(text == null ? "" : text).trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static int skipWhitespace(String text, int start) {
        int index = Math.max(0, start);
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static String stripLilyComments(String text) {
        return String.valueOf(text == null ? "" : text).replaceAll("(?m)%.*$", "");
    }

    private static String stripUnsupportedLilyCommands(String text) {
        return String.valueOf(text == null ? "" : text)
                .replaceAll("\\\\set\\s+Staff\\.instrumentName\\s*=\\s*\"[^\"]*\"", " ")
                .replaceAll("\\\\transposition\\s+[a-g](?:isis|eses|is|es)?[,']*", " ")
                .replaceAll("\\\\key\\s+[a-g](?:isis|eses|is|es)?\\s+\\\\(?:major|minor)", " ")
                .replaceAll("\\\\time\\s+\\d+\\s*/\\s*\\d+", " ")
                .replaceAll("\\\\clef\\s+\"?[A-Za-z0-9]+\"?", " ");
    }

    private static String removeCommandBlock(String source, String commandPattern) {
        BalancedBlock block = findCommandBlock(source, commandPattern);
        if (block == null) {
            return source;
        }
        return source.substring(0, block.getCommandStart()) + source.substring(block.getEndPos() + 1);
    }

    private static BalancedBlock findCommandBlock(String source, String commandPattern) {
        Matcher matcher = Pattern.compile(commandPattern, Pattern.CASE_INSENSITIVE).matcher(source);
        while (matcher.find()) {
            int brace = source.indexOf('{', matcher.end());
            if (brace < 0) {
                return null;
            }
            BalancedBlock block = findBalancedBlock(source, brace);
            if (block != null) {
                return new BalancedBlock(matcher.start(), block.getStartPos(), block.getEndPos(), block.getContent());
            }
        }
        return null;
    }

    private static BalancedBlock findFirstBlock(String source) {
        int brace = source.indexOf('{');
        if (brace < 0) {
            return null;
        }
        return findBalancedBlock(source, brace);
    }

    private static BalancedBlock findBalancedBlock(String source, int startBracePos) {
        int depth = 0;
        for (int index = startBracePos; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return new BalancedBlock(startBracePos, startBracePos, index,
                            source.substring(startBracePos + 1, index));
                }
            }
        }
        return null;
    }

    private static BalancedBlock findBalancedSimultaneousBlock(String source, int startPos) {
        int depth = 0;
        for (int index = startPos; index < source.length() - 1; index++) {
            if (source.regionMatches(index, "<<", 0, 2)) {
                depth++;
                index++;
            } else if (source.regionMatches(index, ">>", 0, 2)) {
                depth--;
                if (depth == 0) {
                    return new BalancedBlock(startPos, startPos, index + 1, source.substring(startPos + 2, index));
                }
                index++;
            }
        }
        return null;
    }

    public static final class LilyPondImportOptions {
        private final Boolean debugMetadata;
        private final Boolean debugPrettyPrint;
        private final Boolean sourceMetadata;

        public LilyPondImportOptions() {
            this(null, null, null);
        }

        public LilyPondImportOptions(Boolean debugMetadata, Boolean debugPrettyPrint, Boolean sourceMetadata) {
            this.debugMetadata = debugMetadata;
            this.debugPrettyPrint = debugPrettyPrint;
            this.sourceMetadata = sourceMetadata;
        }

        public Boolean getDebugMetadata() {
            return debugMetadata;
        }

        public Boolean getDebugPrettyPrint() {
            return debugPrettyPrint;
        }

        public Boolean getSourceMetadata() {
            return sourceMetadata;
        }
    }

    public static final class Fraction {
        private final int num;
        private final int den;

        public Fraction(int num, int den) {
            this.num = num;
            this.den = den == 0 ? 1 : den;
        }

        public int getNum() {
            return num;
        }

        public int getDen() {
            return den;
        }
    }

    public static final class LilyDuration {
        private final int duration;
        private final int dots;

        public LilyDuration(int duration, int dots) {
            this.duration = duration;
            this.dots = dots;
        }

        public int getDuration() {
            return duration;
        }

        public int getDots() {
            return dots;
        }
    }

    private static final class Candidate {
        private final Fraction fraction;
        private final int dots;

        private Candidate(Fraction fraction, int dots) {
            this.fraction = fraction;
            this.dots = dots;
        }

        private Fraction getFraction() {
            return fraction;
        }

        private int getDots() {
            return dots;
        }
    }

    private static final class BalancedBlock {
        private final int commandStart;
        private final int startPos;
        private final int endPos;
        private final String content;

        private BalancedBlock(int commandStart, int startPos, int endPos, String content) {
            this.commandStart = commandStart;
            this.startPos = startPos;
            this.endPos = endPos;
            this.content = content == null ? "" : content;
        }

        private int getCommandStart() {
            return commandStart;
        }

        private int getStartPos() {
            return startPos;
        }

        private int getEndPos() {
            return endPos;
        }

        private String getContent() {
            return content;
        }
    }

    private static final class LilyStaffBlock {
        private final String partName;
        private final String content;
        private final LilyTransposeHint transpose;

        private LilyStaffBlock(String partName, String content, LilyTransposeHint transpose) {
            this.partName = partName == null ? "" : partName;
            this.content = content == null ? "" : content;
            this.transpose = transpose;
        }

        private String getPartName() {
            return partName;
        }

        private String getContent() {
            return content;
        }

        private LilyTransposeHint getTranspose() {
            return transpose;
        }
    }

    private static final class LilyLyricBlock {
        private final String targetPartName;
        private final List<String> words;

        private LilyLyricBlock(String targetPartName, List<String> words) {
            this.targetPartName = targetPartName == null ? "" : targetPartName;
            this.words = words == null ? new ArrayList<String>() : words;
        }

        private String getTargetPartName() {
            return targetPartName;
        }

        private List<String> getWords() {
            return words;
        }
    }

    private static final class LilyTransposeHint {
        private final Integer chromatic;
        private final Integer diatonic;

        private LilyTransposeHint(Integer chromatic, Integer diatonic) {
            this.chromatic = chromatic;
            this.diatonic = diatonic;
        }

        private Integer getChromatic() {
            return chromatic;
        }

        private Integer getDiatonic() {
            return diatonic;
        }
    }

    private static final class LilyRelativeBlock {
        private final String body;
        private final boolean relativeMode;
        private final LilyRelativeAnchor relativeRoot;

        private LilyRelativeBlock(String body, boolean relativeMode, LilyRelativeAnchor relativeRoot) {
            this.body = body == null ? "" : body;
            this.relativeMode = relativeMode;
            this.relativeRoot = relativeRoot;
        }

        private String getBody() {
            return body;
        }

        private boolean isRelativeMode() {
            return relativeMode;
        }

        private LilyRelativeAnchor getRelativeRoot() {
            return relativeRoot;
        }
    }

    private static final class LilyRelativeState {
        private LilyRelativeAnchor previousAnchor;

        private LilyRelativeState(LilyRelativeAnchor previousAnchor) {
            this.previousAnchor = previousAnchor;
        }

        private LilyRelativeAnchor getPreviousAnchor() {
            return previousAnchor;
        }

        private void setPreviousAnchor(LilyRelativeAnchor previousAnchor) {
            this.previousAnchor = previousAnchor;
        }
    }

    private static final class LilyAbcTokenState {
        private String previousAbcPitch = "";
        private String pendingAbcPrefix = "";
        private int previousAbcTokenStart = -1;

        private String getPreviousAbcPitch() {
            return previousAbcPitch;
        }

        private void setPreviousAbcPitch(String previousAbcPitch) {
            this.previousAbcPitch = previousAbcPitch == null ? "" : previousAbcPitch;
        }

        private void appendPendingAbcPrefix(String prefix) {
            pendingAbcPrefix += prefix == null ? "" : prefix;
        }

        private String consumePendingAbcPrefix() {
            String consumed = pendingAbcPrefix;
            pendingAbcPrefix = "";
            return consumed;
        }

        private void setPreviousAbcTokenStart(int previousAbcTokenStart) {
            this.previousAbcTokenStart = previousAbcTokenStart;
        }

        private void insertPrefixBeforePreviousAbcToken(StringBuilder out, String prefix) {
            String actualPrefix = prefix == null ? "" : prefix;
            if (out == null || actualPrefix.isEmpty() || previousAbcTokenStart < 0
                    || previousAbcTokenStart > out.length()) {
                return;
            }
            out.insert(previousAbcTokenStart, actualPrefix);
            previousAbcTokenStart += actualPrefix.length();
        }
    }

    private static final class LilyRelativeAnchor {
        private final String step;
        private final int octave;
        private final int midi;

        private LilyRelativeAnchor(String step, int octave, int midi) {
            this.step = step == null ? "C" : step;
            this.octave = octave;
            this.midi = midi;
        }

        private String getStep() {
            return step;
        }

        private int getOctave() {
            return octave;
        }

        private int getMidi() {
            return midi;
        }
    }

    private static final class LilyPitchToken {
        private final String step;
        private final int alter;
        private final String octaveMarks;

        private LilyPitchToken(String step, int alter, String octaveMarks) {
            this.step = step == null ? "C" : step;
            this.alter = alter;
            this.octaveMarks = octaveMarks == null ? "" : octaveMarks;
        }

        private String getStep() {
            return step;
        }

        private int getAlter() {
            return alter;
        }

        private String getOctaveMarks() {
            return octaveMarks;
        }
    }
}
