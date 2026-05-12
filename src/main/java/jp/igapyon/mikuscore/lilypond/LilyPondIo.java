/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.lilypond;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.abc.AbcIo;
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
        String source = String.valueOf(lilySource == null ? "" : lilySource);
        String title = parseHeaderField(source, "title");
        if (title.isEmpty()) {
            title = "Imported LilyPond";
        }
        String composer = parseHeaderField(source, "composer");
        String meter = parseTimeSignatureText(source);
        String key = parseKeySignatureText(source);
        LilyStaffBlock staffBlock = extractFirstStaffBlock(source);
        String body = staffBlock == null ? extractFirstMusicBody(source) : staffBlock.getContent();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("LilyPond music block not found.");
        }
        String partName = staffBlock == null ? parseFirstStaffName(source) : staffBlock.getPartName();
        String clef = parseClefText(body);
        if (clef.isEmpty()) {
            clef = inferClefText(body);
        }
        String abcBody = parseLilyBodyToAbc(body);
        if (abcBody.trim().isEmpty()) {
            throw new IllegalArgumentException("LilyPond playable body not found.");
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
                AbcIo.musicXmlFromAbc(abc.toString(), new AbcIo.AbcImportOptions()));
        return composer.isEmpty() ? xml : addComposerToMusicXml(xml, composer);
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
        return count > 0 && highest < 60 ? "bass" : "";
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
        String text = String.valueOf(source == null ? "" : source);
        Matcher matcher = Pattern.compile("\\\\new\\s+Staff(?:\\s*=\\s*\"([^\"]*)\")?", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (!matcher.find()) {
            return null;
        }
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
        int bodyStart = text.indexOf('{', cursor);
        if (bodyStart < 0) {
            return null;
        }
        BalancedBlock bodyBlock = findBalancedBlock(text, bodyStart);
        if (bodyBlock == null) {
            return null;
        }
        String bodyPartName = parseBodyInstrumentName(bodyBlock.getContent());
        String partName = !withPartName.isEmpty() ? withPartName : (!bodyPartName.isEmpty() ? bodyPartName : staffName);
        LilyTransposeHint bodyTranspose = parseBodyTransposition(bodyBlock.getContent());
        return new LilyStaffBlock(partName, bodyBlock.getContent(), withTranspose == null ? bodyTranspose : withTranspose);
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
        Matcher matcher = Pattern.compile(
                "<[^>]+>\\d*\\.{0,3}(?:\\*\\d+)?~?|[a-grs](?:isis|eses|is|es)?[,']*\\d*\\.{0,3}(?:\\*\\d+)?~?|[|:]+")
                .matcher(stripUnsupportedLilyCommands(stripLilyComments(relative.getBody())));
        StringBuilder out = new StringBuilder();
        int currentDuration = 4;
        LilyRelativeState relativeState = relative.isRelativeMode() ? new LilyRelativeState(relative.getRelativeRoot())
                : null;
        while (matcher.find()) {
            String token = matcher.group();
            if (token == null || token.isEmpty()) {
                continue;
            }
            if (token.startsWith("|") || token.indexOf(':') >= 0) {
                appendToken(out, "|");
                continue;
            }
            String abcToken = lilyNoteTokenToAbc(token, currentDuration, relativeState);
            Integer duration = lilyTokenDuration(token);
            if (duration != null && duration.intValue() > 0) {
                currentDuration = duration.intValue();
            }
            if (!abcToken.isEmpty()) {
                appendToken(out, abcToken);
            }
        }
        return out.toString().replace(" | |", " |").trim();
    }

    private static String lilyNoteTokenToAbc(String token, int currentDuration) {
        return lilyNoteTokenToAbc(token, currentDuration, null);
    }

    private static String lilyNoteTokenToAbc(String token, int currentDuration, LilyRelativeState relativeState) {
        if (token.startsWith("<") && token.indexOf('>') > 0) {
            return lilyChordTokenToAbc(token, currentDuration, relativeState);
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
        return lilyAccidentalToAbc(alter) + abcPitchFromStepOctave(letter.toUpperCase(), octave) + len
                + (tieStart ? "-" : "");
    }

    private static String lilyChordTokenToAbc(String token, int currentDuration) {
        return lilyChordTokenToAbc(token, currentDuration, null);
    }

    private static String lilyChordTokenToAbc(String token, int currentDuration, LilyRelativeState relativeState) {
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
        return "[" + members + "]" + lilyDurationToAbcLen(duration, dotsText.length(), multiplier)
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
        Matcher matcher = Pattern.compile("^(?:<[^>]+>|[a-grs](?:isis|eses|is|es)?[,']*)(\\d+)?\\.*(?:\\*\\d+)?~?$")
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
