package jp.igapyon.mikuscore.mei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public final class MeiIo {
    public static final int MEI_IMPORT_DIVISIONS = 480;

    private MeiIo() {
    }

    public static String xmlEscape(String value) {
        String text = value == null ? "" : value;
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static String noteTypeToDur(String typeText) {
        String normalized = typeText == null ? "" : typeText.trim().toLowerCase();
        if ("maxima".equals(normalized)) {
            return "maxima";
        }
        if ("long".equals(normalized)) {
            return "long";
        }
        if ("breve".equals(normalized)) {
            return "breve";
        }
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
        if ("128th".equals(normalized)) {
            return "128";
        }
        return "4";
    }

    public static String alterToAccid(String alterText) {
        int alter;
        try {
            alter = Integer.parseInt(alterText == null ? "" : alterText.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
        if (alter <= -2) {
            return "ff";
        }
        if (alter == -1) {
            return "f";
        }
        if (alter == 0) {
            return "n";
        }
        if (alter == 1) {
            return "s";
        }
        if (alter >= 2) {
            return "ss";
        }
        return null;
    }

    public static String musicXmlAccidentalToAccid(String accidentalText) {
        String normalized = accidentalText == null ? "" : accidentalText.trim().toLowerCase();
        if (normalized.length() == 0) {
            return null;
        }
        if ("sharp".equals(normalized)) {
            return "s";
        }
        if ("flat".equals(normalized)) {
            return "f";
        }
        if ("natural".equals(normalized)) {
            return "n";
        }
        if ("double-sharp".equals(normalized) || "sharp-sharp".equals(normalized)) {
            return "ss";
        }
        if ("flat-flat".equals(normalized) || "double-flat".equals(normalized)) {
            return "ff";
        }
        return null;
    }

    public static String fifthsToMeiKeySig(int fifths) {
        if (fifths == 0) {
            return "0";
        }
        if (fifths > 0) {
            return Math.min(7, Math.round(fifths)) + "s";
        }
        return Math.min(7, Math.abs(Math.round(fifths))) + "f";
    }

    public static String toPname(String stepText) {
        String step = stepText == null ? "" : stepText.trim().toLowerCase();
        if (step.length() == 1 && step.charAt(0) >= 'a' && step.charAt(0) <= 'g') {
            return step;
        }
        return "c";
    }

    public static String lyricWordposFromSyllabic(String syllabicText) {
        String value = syllabicText == null ? "" : syllabicText.trim().toLowerCase();
        if ("begin".equals(value)) {
            return "i";
        }
        if ("middle".equals(value)) {
            return "m";
        }
        if ("end".equals(value)) {
            return "t";
        }
        return "";
    }

    public static String lyricSyllabicFromWordpos(String wordposText) {
        String value = wordposText == null ? "" : wordposText.trim().toLowerCase();
        if ("i".equals(value)) {
            return "begin";
        }
        if ("m".equals(value)) {
            return "middle";
        }
        if ("t".equals(value)) {
            return "end";
        }
        return "single";
    }

    public static int toMksDur480(int durationTicks, int sourceDivisions) {
        int base = Math.max(1, Math.round(sourceDivisions));
        return Math.max(1, Math.round((durationTicks * 480.0f) / base));
    }

    public static String extractMeiTieFromMusicXmlTieTypes(Collection<String> tieTypes) {
        boolean hasStart = false;
        boolean hasStop = false;
        if (tieTypes != null) {
            for (String type : tieTypes) {
                String normalized = type == null ? "" : type.trim().toLowerCase();
                if ("start".equals(normalized)) {
                    hasStart = true;
                }
                if ("stop".equals(normalized)) {
                    hasStop = true;
                }
            }
        }
        if (hasStart && hasStop) {
            return "m";
        }
        if (hasStart) {
            return "i";
        }
        if (hasStop) {
            return "t";
        }
        return "";
    }

    public static List<String> extractMeiArticulationTokensFromMusicXmlTags(Collection<String> tags) {
        Set<String> out = new LinkedHashSet<String>();
        if (tags != null) {
            for (String tag : tags) {
                String normalized = tag == null ? "" : tag.trim().toLowerCase();
                if ("staccato".equals(normalized)) {
                    out.add("stacc");
                } else if ("staccatissimo".equals(normalized)) {
                    out.add("spicc");
                } else if ("accent".equals(normalized)) {
                    out.add("acc");
                } else if ("tenuto".equals(normalized)) {
                    out.add("ten");
                } else if ("strong-accent".equals(normalized) || "marcato".equals(normalized)) {
                    out.add("marc");
                }
            }
        }
        return new ArrayList<String>(out);
    }

    public static List<String> extractMeiArticulationTokensFromMusicXmlNote(Element note) {
        List<String> tags = new ArrayList<String>();
        for (Element notations : directChildElementsByLocalName(note, "notations")) {
            for (Element articulations : directChildElementsByLocalName(notations, "articulations")) {
                for (Element child : directChildElements(articulations)) {
                    tags.add(localNameOf(child));
                }
            }
        }
        return extractMeiArticulationTokensFromMusicXmlTags(tags);
    }

    public static MeiLyric extractMeiLyric(Element meiNote) {
        if (meiNote == null) {
            return null;
        }
        for (Element verse : directChildElementsByLocalName(meiNote, "verse")) {
            for (Element syl : directChildElementsByLocalName(verse, "syl")) {
                String text = textOf(syl).trim();
                if (text.length() > 0) {
                    return new MeiLyric(text, lyricSyllabicFromWordpos(syl.getAttribute("wordpos")));
                }
            }
        }
        return null;
    }

    public static MeiSoundingAccid readMeiSoundingAccid(Element node) {
        if (node == null) {
            return new MeiSoundingAccid("", "");
        }
        Element childAccid = null;
        List<Element> accids = directChildElementsByLocalName(node, "accid");
        if (!accids.isEmpty()) {
            childAccid = accids.get(0);
        }
        String visualAccid = firstNonEmpty(node.getAttribute("accid"),
                childAccid == null ? "" : childAccid.getAttribute("accid"), "").trim();
        String gesturalAccid = firstNonEmpty(node.getAttribute("accid.ges"), node.getAttribute("accid-ges"),
                childAccid == null ? "" : childAccid.getAttribute("accid.ges"),
                childAccid == null ? "" : childAccid.getAttribute("accid-ges"), "").trim();
        return new MeiSoundingAccid(visualAccid, visualAccid.length() > 0 ? visualAccid : gesturalAccid);
    }

    public static List<String> readMeiArticulationTokens(Element node) {
        if (node == null) {
            return Collections.emptyList();
        }
        Set<String> tokens = new LinkedHashSet<String>();
        addMeiArticulationTokens(tokens, node.getAttribute("artic"));
        for (Element articNode : directChildElementsByLocalName(node, "artic")) {
            addMeiArticulationTokens(tokens, articNode.getAttribute("artic"));
            addMeiArticulationTokens(tokens, textOf(articNode));
        }
        return new ArrayList<String>(tokens);
    }

    public static List<String> buildMusicXmlArticulationsFromMeiTokens(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<String>();
        for (String token : tokens) {
            String value = token == null ? "" : token.trim().toLowerCase();
            if (value.length() > 0) {
                normalized.add(value);
            }
        }
        List<String> out = new ArrayList<String>();
        if (normalized.contains("stacc")) {
            out.add("<staccato/>");
        }
        if (normalized.contains("spicc") || normalized.contains("stacciss")) {
            out.add("<staccatissimo/>");
        }
        if (normalized.contains("acc")) {
            out.add("<accent/>");
        }
        if (normalized.contains("ten") || normalized.contains("tenuto")) {
            out.add("<tenuto/>");
        }
        if (normalized.contains("marc") || normalized.contains("marcato")) {
            out.add("<strong-accent/>");
        }
        return out;
    }

    public static String buildMusicXmlNoteFromMeiNote(Element meiNote, int durationTicks, String typeText, int dots,
            String voice, int measureFifths) {
        String pname = meiNote == null || meiNote.getAttribute("pname").trim().length() == 0 ? "C"
                : meiNote.getAttribute("pname").trim().toUpperCase();
        int octave = parseIntSafe(meiNote == null ? null : meiNote.getAttribute("oct"), 4);
        MeiSoundingAccid accid = readMeiSoundingAccid(meiNote);
        String explicitAlterXml = accidToPitchAlterXml(accid.getSoundingAccid());
        int impliedAlter = impliedAlterFromFifths(pname, measureFifths);
        String alterXml = explicitAlterXml.length() > 0 ? explicitAlterXml
                : impliedAlter == 0 ? "" : "<alter>" + impliedAlter + "</alter>";
        String accidentalText = accidToMusicXmlAccidental(accid.getVisualAccid());
        String accidentalXml = accidentalText == null ? ""
                : "<accidental>" + xmlEscape(accidentalText) + "</accidental>";
        StringBuilder dotXml = new StringBuilder();
        for (int index = 0; index < Math.max(0, dots); index++) {
            dotXml.append("<dot/>");
        }
        int actual = parseIntSafe(meiNote == null ? null : meiNote.getAttribute("num"), Integer.MIN_VALUE);
        int normal = parseIntSafe(meiNote == null ? null : meiNote.getAttribute("numbase"), Integer.MIN_VALUE);
        String timeModificationXml = actual > 0 && normal > 0
                ? "<time-modification><actual-notes>" + actual + "</actual-notes><normal-notes>" + normal
                        + "</normal-notes></time-modification>"
                : "";
        boolean hasTupletStart = meiNote != null && "1".equals(meiNote.getAttribute("mks-tuplet-start").trim());
        boolean hasTupletStop = meiNote != null && "1".equals(meiNote.getAttribute("mks-tuplet-stop").trim());
        List<String> arts = buildMusicXmlArticulationsFromMeiTokens(readMeiArticulationTokens(meiNote));
        TieFlags tieFlags = parseMeiTieFlags(meiNote == null ? "" : meiNote.getAttribute("tie"));
        String tieXml = (tieFlags.isStart() ? "<tie type=\"start\"/>" : "")
                + (tieFlags.isStop() ? "<tie type=\"stop\"/>" : "");
        String tiedXml = (tieFlags.isStart() ? "<tied type=\"start\"/>" : "")
                + (tieFlags.isStop() ? "<tied type=\"stop\"/>" : "");
        StringBuilder slurXml = new StringBuilder();
        for (MeiSlurNotation entry : parseMeiSlurNotations(meiNote == null ? "" : meiNote.getAttribute("slur"))) {
            slurXml.append("<slur type=\"").append(entry.getType()).append("\" number=\"").append(entry.getNumber())
                    .append("\"/>");
        }
        String tupletXml = (hasTupletStart ? "<tuplet type=\"start\"/>" : "")
                + (hasTupletStop ? "<tuplet type=\"stop\"/>" : "");
        StringBuilder notationsXml = new StringBuilder();
        if (!arts.isEmpty() || tupletXml.length() > 0 || tiedXml.length() > 0 || slurXml.length() > 0) {
            notationsXml.append("<notations>");
            if (!arts.isEmpty()) {
                notationsXml.append("<articulations>");
                for (String art : arts) {
                    notationsXml.append(art);
                }
                notationsXml.append("</articulations>");
            }
            notationsXml.append(tupletXml).append(tiedXml).append(slurXml).append("</notations>");
        }
        String graceAttr = meiNote == null ? "" : meiNote.getAttribute("grace").trim().toLowerCase();
        boolean isGrace = "acc".equals(graceAttr) || "unacc".equals(graceAttr);
        String stemMod = meiNote == null ? "" : meiNote.getAttribute("stem.mod").trim().toLowerCase();
        String graceXml = isGrace ? "<grace"
                + ("acc".equals(graceAttr) || stemMod.indexOf("slash") >= 0 ? " slash=\"yes\"" : "") + "/>" : "";
        String durationXml = isGrace ? "" : "<duration>" + Math.max(0, durationTicks) + "</duration>";
        String stemDir = meiNote == null ? "" : meiNote.getAttribute("stem.dir").trim().toLowerCase();
        String stemXml = "up".equals(stemDir) || "down".equals(stemDir) ? "<stem>" + xmlEscape(stemDir) + "</stem>" : "";
        MeiLyric lyric = extractMeiLyric(meiNote);
        String lyricXml = lyric == null ? ""
                : "<lyric>"
                        + (lyric.getSyllabic().length() > 0
                                ? "<syllabic>" + xmlEscape(lyric.getSyllabic()) + "</syllabic>" : "")
                        + "<text>" + xmlEscape(lyric.getText()) + "</text></lyric>";
        return "<note>" + graceXml + "<pitch><step>" + xmlEscape(pname) + "</step>" + alterXml + "<octave>"
                + octave + "</octave></pitch>" + tieXml + durationXml + "<voice>" + xmlEscape(voice)
                + "</voice><type>" + xmlEscape(typeText) + "</type>" + dotXml.toString() + stemXml + accidentalXml
                + timeModificationXml + notationsXml.toString() + lyricXml + "</note>";
    }

    public static ParsedMeiXmlEvent buildParsedMeiRestEvent(Element rest, int divisions, String voice) {
        if (rest == null) {
            return null;
        }
        String graceAttr = rest.getAttribute("grace") == null ? "" : rest.getAttribute("grace").trim().toLowerCase();
        if ("acc".equals(graceAttr) || "unacc".equals(graceAttr)) {
            return null;
        }
        String durAttr = rest.getAttribute("dur") == null || rest.getAttribute("dur").trim().length() == 0 ? "4"
                : rest.getAttribute("dur").trim();
        int dots = parseIntSafe(rest.getAttribute("dots"), 0);
        int actual = parseIntSafe(rest.getAttribute("num"), Integer.MIN_VALUE);
        int normal = parseIntSafe(rest.getAttribute("numbase"), Integer.MIN_VALUE);
        double tupletRatio = actual > 0 && normal > 0 ? Math.max(0.0001d, Math.round(normal) / (double) Math.round(actual))
                : 1.0d;
        String typeText = meiDurToMusicXmlType(durAttr);
        int safeDivisions = Math.max(1, divisions);
        int ticks = Math.max(1,
                (int) Math.round(meiDurToQuarterLength(durAttr) * dotsMultiplier(dots) * safeDivisions * tupletRatio));
        int resolvedTicks = resolveDurTicksFromMetadata(rest, ticks, safeDivisions);
        StringBuilder dotXml = new StringBuilder();
        for (int index = 0; index < Math.max(0, dots); index++) {
            dotXml.append("<dot/>");
        }
        String xml = "<note><rest/><duration>" + resolvedTicks + "</duration><voice>" + xmlEscape(voice)
                + "</voice><type>" + xmlEscape(typeText) + "</type>" + dotXml.toString() + "</note>";
        return new ParsedMeiXmlEvent("rest", resolvedTicks, xml, meiDurToBeamDepth(durAttr), null);
    }

    public static ParsedMeiXmlEvent buildParsedMeiRestEvent(Element rest, int divisions, String voice,
            MeiForcedTuplet forcedTuplet) {
        return buildParsedMeiRestEvent(cloneMeiEventElementWithForcedContext(rest, null, forcedTuplet), divisions,
                voice);
    }

    public static ParsedMeiXmlEvent buildParsedMeiNoteEvent(Element note, int divisions, String voice,
            int measureFifths, Map<String, Integer> tieCarryByPitch, Map<String, Integer> measureAccidentalByPitch) {
        if (note == null) {
            return null;
        }
        String durAttr = note.getAttribute("dur") == null || note.getAttribute("dur").trim().length() == 0 ? "4"
                : note.getAttribute("dur").trim();
        int dots = parseIntSafe(note.getAttribute("dots"), 0);
        String typeText = meiDurToMusicXmlType(durAttr);
        String graceAttr = note.getAttribute("grace") == null ? "" : note.getAttribute("grace").trim().toLowerCase();
        boolean isGrace = "acc".equals(graceAttr) || "unacc".equals(graceAttr);
        int actual = parseIntSafe(note.getAttribute("num"), Integer.MIN_VALUE);
        int normal = parseIntSafe(note.getAttribute("numbase"), Integer.MIN_VALUE);
        double tupletRatio = actual > 0 && normal > 0 ? Math.max(0.0001d, Math.round(normal) / (double) Math.round(actual))
                : 1.0d;
        int safeDivisions = Math.max(1, divisions);
        int ticks = isGrace ? 0
                : Math.max(1, (int) Math.round(
                        meiDurToQuarterLength(durAttr) * dotsMultiplier(dots) * safeDivisions * tupletRatio));
        int resolvedTicks = isGrace ? 0 : resolveDurTicksFromMetadata(note, ticks, safeDivisions);
        String pname = note.getAttribute("pname") == null || note.getAttribute("pname").trim().length() == 0 ? "C"
                : note.getAttribute("pname").trim().toUpperCase();
        int octave = parseIntSafe(note.getAttribute("oct"), 4);
        TieFlags tieFlags = parseMeiTieFlags(note.getAttribute("tie"));
        MeiSoundingAccid accid = readMeiSoundingAccid(note);
        Integer explicitAlter = accidToAlter(accid.getSoundingAccid());
        int impliedAlter = impliedAlterFromFifths(pname, measureFifths);
        String pitchKey = pname + ":" + octave;
        Map<String, Integer> tieCarry = tieCarryByPitch == null ? new HashMap<String, Integer>() : tieCarryByPitch;
        Map<String, Integer> measureAccidental = measureAccidentalByPitch == null ? new HashMap<String, Integer>()
                : measureAccidentalByPitch;
        Integer carriedAlter = tieFlags.isStop() && explicitAlter == null ? tieCarry.get(pitchKey) : null;
        Integer measureCarriedAlter = explicitAlter == null ? measureAccidental.get(pitchKey) : null;
        int resolvedAlter = explicitAlter != null ? explicitAlter.intValue()
                : carriedAlter != null ? carriedAlter.intValue()
                        : measureCarriedAlter != null ? measureCarriedAlter.intValue() : impliedAlter;
        String alterXml = resolvedAlter == 0 ? "" : "<alter>" + resolvedAlter + "</alter>";
        String accidentalText = accidToMusicXmlAccidental(accid.getVisualAccid());
        String accidentalXml = accidentalText == null ? "" : "<accidental>" + xmlEscape(accidentalText) + "</accidental>";
        String tieXml = (tieFlags.isStart() ? "<tie type=\"start\"/>" : "")
                + (tieFlags.isStop() ? "<tie type=\"stop\"/>" : "");
        String tiedXml = (tieFlags.isStart() ? "<tied type=\"start\"/>" : "")
                + (tieFlags.isStop() ? "<tied type=\"stop\"/>" : "");
        StringBuilder slurXml = new StringBuilder();
        for (MeiSlurNotation entry : parseMeiSlurNotations(note.getAttribute("slur"))) {
            slurXml.append("<slur type=\"").append(entry.getType()).append("\" number=\"").append(entry.getNumber())
                    .append("\"/>");
        }
        String stemMod = note.getAttribute("stem.mod") == null ? "" : note.getAttribute("stem.mod").trim().toLowerCase();
        String graceXml = isGrace ? "<grace"
                + ("acc".equals(graceAttr) || stemMod.indexOf("slash") >= 0 ? " slash=\"yes\"" : "") + "/>" : "";
        String durationXml = isGrace ? "" : "<duration>" + resolvedTicks + "</duration>";
        StringBuilder dotXml = new StringBuilder();
        for (int index = 0; index < Math.max(0, dots); index++) {
            dotXml.append("<dot/>");
        }
        String stemDir = note.getAttribute("stem.dir") == null ? "" : note.getAttribute("stem.dir").trim().toLowerCase();
        String stemXml = "up".equals(stemDir) || "down".equals(stemDir) ? "<stem>" + xmlEscape(stemDir) + "</stem>" : "";
        String timeModificationXml = actual > 0 && normal > 0
                ? "<time-modification><actual-notes>" + actual + "</actual-notes><normal-notes>" + normal
                        + "</normal-notes></time-modification>"
                : "";
        List<String> arts = buildMusicXmlArticulationsFromMeiTokens(readMeiArticulationTokens(note));
        boolean tupletStart = "1".equals(note.getAttribute("mks-tuplet-start").trim());
        boolean tupletStop = "1".equals(note.getAttribute("mks-tuplet-stop").trim());
        String tupletXml = (tupletStart ? "<tuplet type=\"start\"/>" : "")
                + (tupletStop ? "<tuplet type=\"stop\"/>" : "");
        StringBuilder notationsXml = new StringBuilder();
        if (!arts.isEmpty() || tupletXml.length() > 0 || tiedXml.length() > 0 || slurXml.length() > 0) {
            notationsXml.append("<notations>");
            if (!arts.isEmpty()) {
                notationsXml.append("<articulations>");
                for (String art : arts) {
                    notationsXml.append(art);
                }
                notationsXml.append("</articulations>");
            }
            notationsXml.append(tupletXml).append(tiedXml).append(slurXml).append("</notations>");
        }
        MeiLyric lyric = extractMeiLyric(note);
        String lyricXml = lyric == null ? ""
                : "<lyric>"
                        + (lyric.getSyllabic().length() > 0
                                ? "<syllabic>" + xmlEscape(lyric.getSyllabic()) + "</syllabic>" : "")
                        + "<text>" + xmlEscape(lyric.getText()) + "</text></lyric>";
        if (tieFlags.isStart()) {
            tieCarry.put(pitchKey, Integer.valueOf(resolvedAlter));
        } else if (tieFlags.isStop()) {
            tieCarry.remove(pitchKey);
        }
        if (explicitAlter != null) {
            measureAccidental.put(pitchKey, Integer.valueOf(resolvedAlter));
        }
        String xml = "<note>" + graceXml + "<pitch><step>" + xmlEscape(pname) + "</step>" + alterXml
                + "<octave>" + octave + "</octave></pitch>" + tieXml + durationXml + "<voice>" + xmlEscape(voice)
                + "</voice><type>" + xmlEscape(typeText) + "</type>" + dotXml.toString() + stemXml + accidentalXml
                + timeModificationXml + notationsXml.toString() + lyricXml + "</note>";
        return new ParsedMeiXmlEvent("note", resolvedTicks, xml, Integer.valueOf(meiDurToBeamDepth(durAttr)),
                parseBreaksecFromMeiNode(note));
    }

    public static ParsedMeiXmlEvent buildParsedMeiChordEvent(Element chord, int divisions, String voice,
            int measureFifths, Map<String, Integer> tieCarryByPitch, Map<String, Integer> measureAccidentalByPitch) {
        if (chord == null) {
            return null;
        }
        String durAttr = chord.getAttribute("dur") == null || chord.getAttribute("dur").trim().length() == 0 ? "4"
                : chord.getAttribute("dur").trim();
        int dots = parseIntSafe(chord.getAttribute("dots"), 0);
        String typeText = meiDurToMusicXmlType(durAttr);
        String graceAttr = chord.getAttribute("grace") == null ? "" : chord.getAttribute("grace").trim().toLowerCase();
        boolean isGrace = "acc".equals(graceAttr) || "unacc".equals(graceAttr);
        int actual = parseIntSafe(chord.getAttribute("num"), Integer.MIN_VALUE);
        int normal = parseIntSafe(chord.getAttribute("numbase"), Integer.MIN_VALUE);
        double tupletRatio = actual > 0 && normal > 0 ? Math.max(0.0001d, Math.round(normal) / (double) Math.round(actual))
                : 1.0d;
        int safeDivisions = Math.max(1, divisions);
        int ticks = isGrace ? 0
                : Math.max(1, (int) Math.round(
                        meiDurToQuarterLength(durAttr) * dotsMultiplier(dots) * safeDivisions * tupletRatio));
        int resolvedTicks = isGrace ? 0 : resolveDurTicksFromMetadata(chord, ticks, safeDivisions);
        List<Element> noteChildren = directChildElementsByLocalName(chord, "note");
        if (noteChildren.isEmpty()) {
            return null;
        }
        StringBuilder dotXml = new StringBuilder();
        for (int index = 0; index < Math.max(0, dots); index++) {
            dotXml.append("<dot/>");
        }
        boolean chordTupletStart = "1".equals(chord.getAttribute("mks-tuplet-start").trim());
        boolean chordTupletStop = "1".equals(chord.getAttribute("mks-tuplet-stop").trim());
        String timeModificationXml = actual > 0 && normal > 0
                ? "<time-modification><actual-notes>" + actual + "</actual-notes><normal-notes>" + normal
                        + "</normal-notes></time-modification>"
                : "";
        List<String> chordArticTokens = readMeiArticulationTokens(chord);
        String graceXml = isGrace ? "<grace" + ("acc".equals(graceAttr) ? " slash=\"yes\"" : "") + "/>" : "";
        String durationXml = isGrace ? "" : "<duration>" + resolvedTicks + "</duration>";
        Map<String, Integer> tieCarry = tieCarryByPitch == null ? new HashMap<String, Integer>() : tieCarryByPitch;
        Map<String, Integer> measureAccidental = measureAccidentalByPitch == null ? new HashMap<String, Integer>()
                : measureAccidentalByPitch;
        StringBuilder noteXml = new StringBuilder();
        for (int index = 0; index < noteChildren.size(); index++) {
            Element note = noteChildren.get(index);
            String pname = note.getAttribute("pname") == null || note.getAttribute("pname").trim().length() == 0 ? "C"
                    : note.getAttribute("pname").trim().toUpperCase();
            int octave = parseIntSafe(note.getAttribute("oct"), 4);
            MeiSoundingAccid accid = readMeiSoundingAccid(note);
            Integer explicitAlter = accidToAlter(accid.getSoundingAccid());
            int impliedAlter = impliedAlterFromFifths(pname, measureFifths);
            TieFlags tieFlags = parseMeiTieFlags(note.getAttribute("tie"));
            String pitchKey = pname + ":" + octave;
            Integer carriedAlter = tieFlags.isStop() && explicitAlter == null ? tieCarry.get(pitchKey) : null;
            Integer measureCarriedAlter = explicitAlter == null ? measureAccidental.get(pitchKey) : null;
            int resolvedAlter = explicitAlter != null ? explicitAlter.intValue()
                    : carriedAlter != null ? carriedAlter.intValue()
                            : measureCarriedAlter != null ? measureCarriedAlter.intValue() : impliedAlter;
            String alterXml = resolvedAlter == 0 ? "" : "<alter>" + resolvedAlter + "</alter>";
            String accidentalText = accidToMusicXmlAccidental(accid.getVisualAccid());
            String accidentalXml = accidentalText == null ? ""
                    : "<accidental>" + xmlEscape(accidentalText) + "</accidental>";
            Set<String> articTokens = new LinkedHashSet<String>(chordArticTokens);
            articTokens.addAll(readMeiArticulationTokens(note));
            List<String> arts = buildMusicXmlArticulationsFromMeiTokens(articTokens);
            String tieXml = (tieFlags.isStart() ? "<tie type=\"start\"/>" : "")
                    + (tieFlags.isStop() ? "<tie type=\"stop\"/>" : "");
            String tiedXml = (tieFlags.isStart() ? "<tied type=\"start\"/>" : "")
                    + (tieFlags.isStop() ? "<tied type=\"stop\"/>" : "");
            StringBuilder slurXml = new StringBuilder();
            for (MeiSlurNotation entry : parseMeiSlurNotations(note.getAttribute("slur"))) {
                slurXml.append("<slur type=\"").append(entry.getType()).append("\" number=\"")
                        .append(entry.getNumber()).append("\"/>");
            }
            String tupletXml = index == 0
                    ? (chordTupletStart ? "<tuplet type=\"start\"/>" : "")
                            + (chordTupletStop ? "<tuplet type=\"stop\"/>" : "")
                    : "";
            StringBuilder notationsXml = new StringBuilder();
            if (index == 0 && (!arts.isEmpty() || tupletXml.length() > 0) || tiedXml.length() > 0
                    || slurXml.length() > 0) {
                notationsXml.append("<notations>");
                if (index == 0 && !arts.isEmpty()) {
                    notationsXml.append("<articulations>");
                    for (String art : arts) {
                        notationsXml.append(art);
                    }
                    notationsXml.append("</articulations>");
                }
                if (index == 0) {
                    notationsXml.append(tupletXml);
                }
                notationsXml.append(tiedXml).append(slurXml).append("</notations>");
            }
            MeiLyric lyric = extractMeiLyric(note);
            String lyricXml = lyric == null ? ""
                    : "<lyric>"
                            + (lyric.getSyllabic().length() > 0
                                    ? "<syllabic>" + xmlEscape(lyric.getSyllabic()) + "</syllabic>" : "")
                            + "<text>" + xmlEscape(lyric.getText()) + "</text></lyric>";
            if (tieFlags.isStart()) {
                tieCarry.put(pitchKey, Integer.valueOf(resolvedAlter));
            } else if (tieFlags.isStop()) {
                tieCarry.remove(pitchKey);
            }
            if (explicitAlter != null) {
                measureAccidental.put(pitchKey, Integer.valueOf(resolvedAlter));
            }
            noteXml.append("<note>").append(index > 0 ? "<chord/>" : "").append(graceXml).append("<pitch><step>")
                    .append(xmlEscape(pname)).append("</step>").append(alterXml).append("<octave>").append(octave)
                    .append("</octave></pitch>").append(tieXml).append(durationXml).append("<voice>")
                    .append(xmlEscape(voice)).append("</voice><type>").append(xmlEscape(typeText)).append("</type>")
                    .append(dotXml).append(accidentalXml).append(timeModificationXml).append(notationsXml)
                    .append(lyricXml).append("</note>");
        }
        return new ParsedMeiXmlEvent("chord", resolvedTicks, noteXml.toString(),
                Integer.valueOf(meiDurToBeamDepth(durAttr)), parseBreaksecFromMeiNode(chord));
    }

    public static List<ParsedMeiXmlEvent> applyMeiBeamContainerToEvents(Collection<ParsedMeiXmlEvent> events) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        List<Integer> pitchedIndexes = new ArrayList<Integer>();
        for (int index = 0; index < out.size(); index++) {
            ParsedMeiXmlEvent event = out.get(index);
            if (event != null && !"rest".equals(event.getKind())) {
                pitchedIndexes.add(Integer.valueOf(index));
            }
        }
        if (pitchedIndexes.size() < 2) {
            return out;
        }
        List<Integer> depths = new ArrayList<Integer>();
        int maxDepth = 1;
        for (Integer index : pitchedIndexes) {
            Integer depth = out.get(index.intValue()).getBeamDepth();
            int safeDepth = Math.max(1, depth == null ? 1 : depth.intValue());
            depths.add(Integer.valueOf(safeDepth));
            maxDepth = Math.max(maxDepth, safeDepth);
        }
        for (int level = 1; level <= maxDepth; level++) {
            for (int pitchedIndex = 0; pitchedIndex < pitchedIndexes.size(); pitchedIndex++) {
                if (depths.get(pitchedIndex).intValue() < level) {
                    continue;
                }
                boolean prev = canConnectMeiBeam(pitchedIndex - 1, level, pitchedIndexes, depths, out);
                boolean next = canConnectMeiBeam(pitchedIndex, level, pitchedIndexes, depths, out);
                if (!prev && !next) {
                    continue;
                }
                String value = !prev && next ? "begin" : prev && !next ? "end" : "continue";
                int eventIndex = pitchedIndexes.get(pitchedIndex).intValue();
                ParsedMeiXmlEvent event = out.get(eventIndex);
                out.set(eventIndex, event.withXml(addBeamToEventXml(event.getXml(), value, level)));
            }
        }
        return out;
    }

    public static MeiForcedTuplet resolveMeiTupletContext(Element tuplet, MeiForcedTuplet forcedTuplet) {
        int num = parseIntSafe(tuplet == null ? null : tuplet.getAttribute("num"), Integer.MIN_VALUE);
        int numbase = parseIntSafe(tuplet == null ? null : tuplet.getAttribute("numbase"), Integer.MIN_VALUE);
        if (num > 0 && numbase > 0) {
            return new MeiForcedTuplet(num, numbase);
        }
        return forcedTuplet;
    }

    public static String resolveMeiGraceGroupValue(Element graceGrp) {
        String raw = graceGrp == null ? "" : graceGrp.getAttribute("grace").trim().toLowerCase();
        if ("acc".equals(raw) || "unacc".equals(raw)) {
            return raw;
        }
        String slash = graceGrp == null ? "" : graceGrp.getAttribute("slash").trim().toLowerCase();
        return "yes".equals(slash) ? "acc" : "unacc";
    }

    public static Element cloneMeiEventElementWithForcedContext(Element node, String forcedGrace,
            MeiForcedTuplet forcedTuplet) {
        if (node == null) {
            return null;
        }
        String name = localNameOf(node);
        boolean acceptsGrace = "note".equals(name) || "chord".equals(name) || "rest".equals(name);
        String safeGrace = forcedGrace == null ? "" : forcedGrace.trim().toLowerCase();
        boolean applyGrace = acceptsGrace && ("acc".equals(safeGrace) || "unacc".equals(safeGrace))
                && node.getAttribute("grace").trim().length() == 0;
        boolean applyTuplet = forcedTuplet != null && node.getAttribute("num").trim().length() == 0
                && node.getAttribute("numbase").trim().length() == 0;
        if (!applyGrace && !applyTuplet) {
            return node;
        }
        Element clone = (Element) node.cloneNode(true);
        if (applyGrace) {
            clone.setAttribute("grace", safeGrace);
        }
        if (applyTuplet) {
            clone.setAttribute("num", Integer.toString(forcedTuplet.getNum()));
            clone.setAttribute("numbase", Integer.toString(forcedTuplet.getNumbase()));
        }
        return clone;
    }

    public static int parseMeiStemSlashCount(Element node) {
        String stemMod = node == null ? "" : node.getAttribute("stem.mod").trim().toLowerCase();
        if (!stemMod.contains("slash")) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(\\d+)\\s*slash").matcher(stemMod);
        int count = matcher.find() ? parseIntSafe(matcher.group(1), 1) : 1;
        return Math.max(1, Math.min(4, Math.round(count)));
    }

    public static List<Element> expandMeiStemSlashNodes(Element node, int divisions) {
        if (node == null) {
            return null;
        }
        String name = localNameOf(node);
        if (!"note".equals(name) && !"chord".equals(name)) {
            return null;
        }
        String graceAttr = node.getAttribute("grace").trim().toLowerCase();
        if ("acc".equals(graceAttr) || "unacc".equals(graceAttr)) {
            return null;
        }
        int slashCount = parseMeiStemSlashCount(node);
        if (slashCount <= 0) {
            return null;
        }

        String durAttr = node.getAttribute("dur").length() > 0 ? node.getAttribute("dur") : "4";
        int dots = parseIntSafe(node.getAttribute("dots"), 0);
        int actual = parseIntSafe(node.getAttribute("num"), Integer.MIN_VALUE);
        int normal = parseIntSafe(node.getAttribute("numbase"), Integer.MIN_VALUE);
        double tupletRatio = actual > 0 && normal > 0
                ? Math.max(0.0001d, Math.round(normal) / (double) Math.round(actual))
                : 1.0d;
        int safeDivisions = Math.max(1, Math.round(divisions));
        int baseTicks = Math.max(1,
                (int) Math.round(meiDurToQuarterLength(durAttr) * dotsMultiplier(dots) * safeDivisions * tupletRatio));
        int totalTicks = resolveDurTicksFromMetadata(node, baseTicks, safeDivisions);
        int unitTicks = Math.max(1, (int) Math.round(safeDivisions / Math.pow(2.0d, slashCount)));
        if (totalTicks < unitTicks * 2 || totalTicks % unitTicks != 0) {
            return null;
        }

        int repeatCount = Math.max(2, Math.round(totalTicks / (float) unitTicks));
        List<Element> expanded = new ArrayList<Element>();
        for (int index = 0; index < repeatCount; index++) {
            Element clone = (Element) node.cloneNode(true);
            MeiDurDots inferred = inferMeiDurAndDotsFromTicks(unitTicks, safeDivisions);
            clone.setAttribute("dur", inferred.getDur());
            if (inferred.getDots() > 0) {
                clone.setAttribute("dots", Integer.toString(inferred.getDots()));
            } else {
                clone.removeAttribute("dots");
            }
            clone.removeAttribute("stem.mod");
            clone.setAttribute("mks-dur-div", Integer.toString(safeDivisions));
            clone.setAttribute("mks-dur-480", Integer.toString(toMksDur480(unitTicks, safeDivisions)));
            clone.setAttribute("mks-dur-ticks", Integer.toString(unitTicks));

            if (index > 0) {
                clone.removeAttribute("xml:id");
                clone.removeAttribute("id");
                clone.removeAttribute("tie");
                clone.removeAttribute("slur");
                clone.removeAttribute("mks-tuplet-start");
                for (Element child : directChildElementsByLocalName(clone, "note")) {
                    child.removeAttribute("xml:id");
                    child.removeAttribute("id");
                    child.removeAttribute("tie");
                    child.removeAttribute("slur");
                }
            }
            if (index < repeatCount - 1) {
                clone.removeAttribute("mks-tuplet-stop");
            }
            expanded.add(clone);
        }
        return expanded;
    }

    public static Element applyMeiMeasureRestDurationMetadata(Element node, int measureTicks, int divisions) {
        if (node == null) {
            return null;
        }
        String name = localNameOf(node);
        if (!"mSpace".equals(name) && !"mRest".equals(name)) {
            return node;
        }
        if (node.getAttribute("mks-dur-ticks").trim().length() > 0) {
            return node;
        }
        int safeMeasureTicks = Math.max(1, Math.round(measureTicks));
        int safeDivisions = Math.max(1, Math.round(divisions));
        MeiDurDots inferred = inferMeiDurAndDotsFromTicks(safeMeasureTicks, safeDivisions);
        Element clone = (Element) node.cloneNode(true);
        clone.setAttribute("dur", inferred.getDur());
        if (inferred.getDots() > 0) {
            clone.setAttribute("dots", Integer.toString(inferred.getDots()));
        }
        clone.setAttribute("mks-dur-div", Integer.toString(safeDivisions));
        clone.setAttribute("mks-dur-480", Integer.toString(toMksDur480(safeMeasureTicks, safeDivisions)));
        clone.setAttribute("mks-dur-ticks", Integer.toString(safeMeasureTicks));
        return clone;
    }

    public static ParsedMeiLayer parseMeiLayerEvents(Element layer, int divisions, String voice, int measureTicks,
            int measureFifths, Map<String, Integer> tieCarryIn) {
        List<ParsedMeiXmlEvent> events = new ArrayList<ParsedMeiXmlEvent>();
        Map<String, Integer> idToEventIndex = new HashMap<String, Integer>();
        Map<String, Integer> tieCarryByPitch = tieCarryIn == null ? new HashMap<String, Integer>()
                : new HashMap<String, Integer>(tieCarryIn);
        Map<String, Integer> measureAccidentalByPitch = new HashMap<String, Integer>();
        if (layer != null) {
            for (Element child : directChildElements(layer)) {
                parseMeiLayerEventElement(child, divisions, voice, measureTicks, measureFifths, tieCarryByPitch,
                        measureAccidentalByPitch, null, null, events, idToEventIndex);
            }
        }
        return new ParsedMeiLayer(events, idToEventIndex, tieCarryByPitch);
    }

    public static ParsedMeiLayer parseMeiLayerEvents(Element layer, int divisions, String voice, int measureTicks,
            int measureFifths) {
        return parseMeiLayerEvents(layer, divisions, voice, measureTicks, measureFifths, null);
    }

    public static Integer resolveParsedMeiXmlEventStartTickByIndex(Collection<ParsedMeiXmlEvent> events,
            int eventIndex) {
        if (events == null || eventIndex < 0 || eventIndex >= events.size()) {
            return null;
        }
        int cursor = 0;
        int index = 0;
        for (ParsedMeiXmlEvent event : events) {
            if (index == eventIndex) {
                return Integer.valueOf(cursor);
            }
            cursor += Math.max(0, event == null ? 0 : event.getDurationTicks());
            index++;
        }
        return null;
    }

    public static Map<String, Integer> buildMeiStaffIdToEventTick(Collection<ParsedMeiLayer> parsedLayers) {
        Map<String, Integer> staffIdToEventTick = new HashMap<String, Integer>();
        if (parsedLayers == null) {
            return staffIdToEventTick;
        }
        for (ParsedMeiLayer parsedLayer : parsedLayers) {
            if (parsedLayer == null) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : parsedLayer.getIdToEventIndex().entrySet()) {
                Integer index = entry.getValue();
                if (index == null) {
                    continue;
                }
                Integer tick = resolveParsedMeiXmlEventStartTickByIndex(parsedLayer.getEvents(), index.intValue());
                if (tick != null) {
                    staffIdToEventTick.put(entry.getKey(), Integer.valueOf(Math.max(0, tick.intValue())));
                }
            }
        }
        return staffIdToEventTick;
    }

    public static MeiProcessedLayerXml buildMeiProcessedLayerXml(Element staff, Element layer, String voice,
            ParsedMeiLayer parsedLayer, Map<String, Integer> tieCarryIn, Map<String, Integer> idToEventTick,
            int divisions, int beatType, int measureTicks) {
        ParsedMeiLayer safeLayer = parsedLayer == null
                ? new ParsedMeiLayer(Collections.<ParsedMeiXmlEvent>emptyList(), null, null)
                : parsedLayer;
        String safeVoice = voice == null ? "" : voice;
        List<ParsedMeiXmlEvent> slurAppliedEvents = applyStaffSlurControlEvents(staff, layer, safeLayer.getEvents(),
                safeLayer.getIdToEventIndex(), idToEventTick, divisions, beatType);
        List<ParsedMeiXmlEvent> spanAppliedEvents = applyStaffSpanControlEvents(staff, layer, slurAppliedEvents,
                safeLayer.getIdToEventIndex(), idToEventTick, divisions, beatType);
        MeiTieCarryResult tieApplied = applyTieCarryAccidentalsForLayerEvents(spanAppliedEvents, tieCarryIn);
        List<ParsedMeiEvent> plainEvents = toParsedMeiEvents(tieApplied.getEvents());
        String directionXml = collectLayerDirectionXml(staff, layer, divisions, beatType, safeVoice, plainEvents,
                safeLayer.getIdToEventIndex(), idToEventTick);
        String harmonyXml = collectLayerHarmonyXml(staff, layer, divisions, beatType, plainEvents,
                safeLayer.getIdToEventIndex(), idToEventTick);
        int sourceTotalTicks = 0;
        for (ParsedMeiXmlEvent event : tieApplied.getEvents()) {
            sourceTotalTicks += Math.max(0, event == null ? 0 : event.getDurationTicks());
        }
        MeiLayerTrimResult trimmed = trimLayerEventsToMeasureCapacity(tieApplied.getEvents(), measureTicks);
        StringBuilder xml = new StringBuilder();
        xml.append(harmonyXml).append(directionXml);
        for (ParsedMeiXmlEvent event : trimmed.getEvents()) {
            xml.append(event == null ? "" : event.getXml());
        }
        return new MeiProcessedLayerXml(safeVoice, xml.toString(), trimmed.getTotalTicks(), sourceTotalTicks,
                trimmed.getDroppedCount(), trimmed.getDroppedTicks(), trimmed.getTrimmedCount(),
                trimmed.getTrimmedTicks(), tieApplied.getTieCarryOut());
    }

    public static MeiProcessedStaffLayers buildMeiProcessedStaffLayers(Element staff, int divisions, int beatType,
            int measureTicks, int measureFifths, Map<String, Map<String, Integer>> tieCarryByVoice) {
        List<Element> layerNodes = directChildElementsByLocalName(staff, "layer");
        List<ParsedMeiLayerEntry> parsedEntries = new ArrayList<ParsedMeiLayerEntry>();
        for (int index = 0; index < layerNodes.size(); index++) {
            Element layer = layerNodes.get(index);
            String voice = layer.getAttribute("n").trim().length() > 0 ? layer.getAttribute("n").trim()
                    : Integer.toString(index + 1);
            Map<String, Integer> tieCarryIn = tieCarryByVoice == null ? null : tieCarryByVoice.get(voice);
            ParsedMeiLayer parsedLayer = parseMeiLayerEvents(layer, divisions, voice, measureTicks, measureFifths,
                    tieCarryIn);
            parsedEntries.add(new ParsedMeiLayerEntry(layer, voice, parsedLayer, tieCarryIn));
        }

        List<ParsedMeiLayer> parsedLayers = new ArrayList<ParsedMeiLayer>();
        for (ParsedMeiLayerEntry entry : parsedEntries) {
            parsedLayers.add(entry.parsedLayer);
        }
        Map<String, Integer> staffIdToEventTick = buildMeiStaffIdToEventTick(parsedLayers);
        List<MeiProcessedLayerXml> layers = new ArrayList<MeiProcessedLayerXml>();
        Map<String, Map<String, Integer>> tieCarryOutByVoice = new HashMap<String, Map<String, Integer>>();
        for (ParsedMeiLayerEntry entry : parsedEntries) {
            MeiProcessedLayerXml processed = buildMeiProcessedLayerXml(staff, entry.layer, entry.voice,
                    entry.parsedLayer, entry.tieCarryIn, staffIdToEventTick, divisions, beatType, measureTicks);
            tieCarryOutByVoice.put(entry.voice, processed.getTieCarryOut());
            if (processed.getXml().length() > 0) {
                layers.add(processed);
            }
        }
        int maxLayerTicks = 0;
        List<MeiLayerXml> layerXml = new ArrayList<MeiLayerXml>();
        for (MeiProcessedLayerXml layer : layers) {
            maxLayerTicks = Math.max(maxLayerTicks, layer.getTotalTicks());
            layerXml.add(new MeiLayerXml(layer.getXml(), layer.getTotalTicks()));
        }
        return new MeiProcessedStaffLayers(layers, buildMeiMeasureBodyXml(layerXml, measureTicks), maxLayerTicks,
                staffIdToEventTick, tieCarryOutByVoice);
    }

    public static String buildMeiImportedMeasureXmlFromProcessedStaff(MeiMeasureImportState state,
            MeiProcessedStaffLayers processedStaff, ResolvedMeiImportOptions options,
            Collection<MiscField> rawSourceFields, boolean hasEmittedInitialAttributes, int measureIndex) {
        return buildMeiImportedMeasureXmlFromProcessedStaff(state, processedStaff, options, rawSourceFields,
                hasEmittedInitialAttributes, measureIndex, MEI_IMPORT_DIVISIONS);
    }

    public static String buildMeiImportedMeasureXmlFromProcessedStaff(MeiMeasureImportState state,
            MeiProcessedStaffLayers processedStaff, ResolvedMeiImportOptions options,
            Collection<MiscField> rawSourceFields, boolean hasEmittedInitialAttributes, int measureIndex,
            int divisions) {
        if (state == null) {
            throw new IllegalArgumentException("MEI measure import state is missing.");
        }
        if (!state.hasTargetStaff()) {
            return buildMeiEmptyImportedMeasureXml(state.getMeasureNo());
        }
        MeiProcessedStaffLayers processed = processedStaff == null
                ? new MeiProcessedStaffLayers(Collections.<MeiProcessedLayerXml>emptyList(), "", 0, null, null)
                : processedStaff;
        ResolvedMeiImportOptions safeOptions = options == null ? resolveMeiImportOptions(null, null, null, null)
                : options;
        String staffNo = state.getPreviousPartState() == null ? "" : state.getPreviousPartState().getStaffNo();
        int droppedEvents = 0;
        int droppedTicks = 0;
        int trimmedEvents = 0;
        int trimmedTicks = 0;
        int sourceTotalTicks = 0;
        boolean overfullDetected = false;
        for (MeiProcessedLayerXml layer : processed.getLayers()) {
            droppedEvents += layer.getDroppedCount();
            droppedTicks += layer.getDroppedTicks();
            trimmedEvents += layer.getTrimmedCount();
            trimmedTicks += layer.getTrimmedTicks();
            sourceTotalTicks += layer.getSourceTotalTicks();
            if (layer.getSourceTotalTicks() > state.getMeasureTicks()) {
                overfullDetected = true;
            }
        }
        if (safeOptions.isFailOnOverfullDrop() && droppedEvents > 0) {
            throw new IllegalStateException("MEI overfull would drop events (measure=" + state.getMeasureNo()
                    + ", staff=" + staffNo + ", droppedEvents=" + droppedEvents + ", droppedTicks=" + droppedTicks
                    + ").");
        }

        List<MiscField> allFields = new ArrayList<MiscField>();
        if (!hasEmittedInitialAttributes && rawSourceFields != null) {
            allFields.addAll(rawSourceFields);
        }
        if (safeOptions.isSourceMetadata()) {
            allFields.addAll(extractMiscFieldsFromMeiStaff(state.getTargetStaff()));
        }
        if (safeOptions.isDebugMetadata()) {
            allFields.addAll(buildMeiDebugFieldsFromStaff(state.getTargetStaff(), state.getMeasureNo(),
                    Math.max(1, divisions)));
        }
        if (overfullDetected) {
            allFields.addAll(buildMeiOverfullDiagnosticFields(state.getMeasureNo(), staffNo, sourceTotalTicks,
                    state.getMeasureTicks(), droppedEvents, droppedTicks, trimmedEvents, trimmedTicks));
        }
        String miscellaneousXml = allFields.isEmpty() ? "" : buildMusicXmlMiscellaneousXml(allFields);
        String attributesXml = buildMeiMeasureAttributesXml(hasEmittedInitialAttributes, state.shouldEmitTime(),
                state.shouldEmitKey(), state.shouldEmitTranspose(), state.shouldEmitClef(), Math.max(1, divisions),
                state.getMeasureFifths(), state.getMeasureBeats(), state.getMeasureBeatType(),
                state.getMeasureTimeSymbol(), transposeChromatic(state.getMeasureTranspose()),
                transposeDiatonic(state.getMeasureTranspose()), state.getMeasureClefSign(), state.getMeasureClefLine(),
                miscellaneousXml);
        String implicitAttr = buildMeasureImplicitAttribute(state.isImplicitFromMeta(), measureIndex,
                processed.getMaxLayerTicks(), state.getMeasureTicks());
        return buildMeiImportedMeasureXml(state.getMeasureNo(), implicitAttr, attributesXml,
                buildMeiMeasureLeftBarlineXml(state.getMeasureMeta()), processed.getBodyXml(),
                buildMeiMeasureRightBarlineXml(state.getMeasureMeta()));
    }

    public static String buildMeiImportedPartXmlFromContext(MeiInitialImportContext context, String staffNo,
            int partIndex, ResolvedMeiImportOptions options, Collection<MiscField> rawSourceFields) {
        if (context == null) {
            throw new IllegalArgumentException("MEI import context is missing.");
        }
        MeiPartImportState currentState = buildMeiInitialPartImportState(context, staffNo, partIndex);
        Map<String, Map<String, Integer>> tieCarryByVoice = new HashMap<String, Map<String, Integer>>();
        StringBuilder measuresXml = new StringBuilder();
        for (int measureIndex = 0; measureIndex < context.getMeasureNodes().size(); measureIndex++) {
            Element measureNode = context.getMeasureNodes().get(measureIndex);
            MeiMeasureImportState measureState = buildMeiMeasureImportState(context, currentState, measureNode,
                    measureIndex);
            MeiProcessedStaffLayers processedStaff = measureState.hasTargetStaff()
                    ? buildMeiProcessedStaffLayers(measureState.getTargetStaff(), context.getDivisions(),
                            measureState.getMeasureBeatType(), measureState.getMeasureTicks(),
                            measureState.getMeasureFifths(), tieCarryByVoice)
                    : null;
            measuresXml.append(buildMeiImportedMeasureXmlFromProcessedStaff(measureState, processedStaff, options,
                    rawSourceFields, currentState.hasEmittedInitialAttributes(), measureIndex,
                    context.getDivisions()));
            if (processedStaff != null) {
                tieCarryByVoice = new HashMap<String, Map<String, Integer>>(processedStaff.getTieCarryByVoice());
            }
            currentState = measureState.toNextPartImportState();
        }
        return buildMeiImportedPartXml(currentState.getPartId(), measuresXml.toString());
    }

    public static String buildMeiScorePartwiseXmlFromContext(MeiInitialImportContext context,
            ResolvedMeiImportOptions options, Collection<MiscField> rawSourceFields) {
        if (context == null) {
            throw new IllegalArgumentException("MEI import context is missing.");
        }
        Map<String, String> staffLabels = new HashMap<String, String>();
        for (String staffNo : context.getStaffNumbers()) {
            String safeStaffNo = staffNo == null ? "" : staffNo.trim();
            MeiStaffMeta meta = context.getStaffMeta().get(safeStaffNo);
            if (meta != null) {
                staffLabels.put(safeStaffNo, meta.getLabel());
            }
        }
        StringBuilder partsXml = new StringBuilder();
        for (int partIndex = 0; partIndex < context.getStaffNumbers().size(); partIndex++) {
            String staffNo = context.getStaffNumbers().get(partIndex);
            partsXml.append(buildMeiImportedPartXmlFromContext(context, staffNo, partIndex, options, rawSourceFields));
        }
        String xml = buildMeiScorePartwiseXmlDocument(context.getTitle(),
                buildMeiPartListXml(context.getStaffNumbers(), staffLabels), partsXml.toString());
        return MusicXmlIo.prettyPrintMusicXmlText(MusicXmlIo.applyImplicitBeamsToMusicXmlText(xml));
    }

    public static String convertMeiToMusicXml(String meiSource) {
        return convertMeiToMusicXml(meiSource, null, null, null, null);
    }

    public static String convertMeiToMusicXml(String meiSource, Boolean debugMetadata, Boolean sourceMetadata,
            Boolean failOnOverfullDrop, Integer meiCorpusIndex) {
        ResolvedMeiImportOptions options = resolveMeiImportOptions(debugMetadata, sourceMetadata, failOnOverfullDrop,
                meiCorpusIndex);
        Document doc = MusicXmlIo.parseMusicXmlDocument(String.valueOf(meiSource == null ? "" : meiSource));
        if (doc == null) {
            throw new IllegalArgumentException("Invalid MEI XML.");
        }
        Element meiImportRoot = selectMeiImportRoot(doc, options.getMeiCorpusIndex());
        return buildMeiScorePartwiseXmlFromContext(buildMeiInitialImportContext(meiImportRoot), options,
                buildMeiSourceRawMiscFields(meiSource));
    }

    public static String normalizeMeiVersion(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.matches("^\\d+\\.\\d+(\\.\\d+)?(\\+[A-Za-z0-9._-]+)?$")) {
            return value;
        }
        return "5.1+basic";
    }

    public static Map<String, String> readMusicXmlPartNameMap(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<String, String>();
        for (Element partList : directChildElementsByLocalName(doc.getDocumentElement(), "part-list")) {
            for (Element scorePart : directChildElementsByLocalName(partList, "score-part")) {
                String id = scorePart.getAttribute("id") == null ? "" : scorePart.getAttribute("id").trim();
                if (id.length() == 0) {
                    continue;
                }
                String name = firstNonEmpty(firstDirectChildText(scorePart, "part-name"),
                        firstDirectChildText(scorePart, "part-abbreviation"), id).trim();
                map.put(id, name);
            }
        }
        return map;
    }

    public static int detectMusicXmlStaffCountForPart(Element part) {
        int maxStaff = 1;
        if (part == null) {
            return maxStaff;
        }
        for (Element measure : directChildElementsByLocalName(part, "measure")) {
            for (Element attributes : directChildElementsByLocalName(measure, "attributes")) {
                for (Element staves : directChildElementsByLocalName(attributes, "staves")) {
                    maxStaff = Math.max(maxStaff, parseIntSafe(textOf(staves), 1));
                }
            }
            for (Element note : directChildElementsByLocalName(measure, "note")) {
                Element staff = firstDirectChildLocal(note, "staff");
                maxStaff = Math.max(maxStaff, parseIntSafe(textOf(staff), 1));
            }
        }
        return Math.max(1, maxStaff);
    }

    public static List<MusicXmlStaffSlot> collectMusicXmlStaffSlots(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.emptyList();
        }
        Map<String, String> partNameMap = readMusicXmlPartNameMap(doc);
        List<MusicXmlStaffSlot> slots = new ArrayList<MusicXmlStaffSlot>();
        int globalStaff = 1;
        for (Element part : directChildElementsByLocalName(doc.getDocumentElement(), "part")) {
            String partId = part.getAttribute("id") == null ? "" : part.getAttribute("id").trim();
            if (partId.length() == 0) {
                continue;
            }
            String partName = partNameMap.containsKey(partId) ? partNameMap.get(partId) : partId;
            int count = detectMusicXmlStaffCountForPart(part);
            for (int staffNo = 1; staffNo <= count; staffNo++) {
                slots.add(new MusicXmlStaffSlot(partId, staffNo, globalStaff,
                        count > 1 ? partName + " (" + staffNo + ")" : partName));
                globalStaff++;
            }
        }
        return slots;
    }

    public static MeiClef resolveClefForMusicXmlSlot(Element part, int localStaff) {
        if (part == null) {
            return new MeiClef("G", 2);
        }
        int safeStaff = Math.max(1, localStaff);
        for (Element measure : directChildElementsByLocalName(part, "measure")) {
            for (Element attributes : directChildElementsByLocalName(measure, "attributes")) {
                for (Element clef : directChildElementsByLocalName(attributes, "clef")) {
                    String numberText = clef.getAttribute("number");
                    boolean applies = numberText == null || numberText.trim().length() == 0 ? safeStaff == 1
                            : parseIntSafe(numberText, 1) == safeStaff;
                    if (!applies) {
                        continue;
                    }
                    String sign = firstNonEmpty(firstDirectChildText(clef, "sign"), "G").trim().toUpperCase();
                    int line = parseIntSafe(firstDirectChildText(clef, "line"), 2);
                    return new MeiClef(sign, line);
                }
            }
        }
        return new MeiClef("G", 2);
    }

    public static MeiTranspose resolveTransposeForMusicXmlSlot(Element part, int localStaff) {
        if (part == null) {
            return null;
        }
        int safeStaff = Math.max(1, localStaff);
        for (Element measure : directChildElementsByLocalName(part, "measure")) {
            for (Element attributes : directChildElementsByLocalName(measure, "attributes")) {
                Element transpose = firstDirectChildLocal(attributes, "transpose");
                if (transpose == null) {
                    continue;
                }
                Integer chromatic = parseOptionalInt(firstDirectChildText(transpose, "chromatic"));
                Integer diatonic = parseOptionalInt(firstDirectChildText(transpose, "diatonic"));
                if (chromatic != null || diatonic != null) {
                    return new MeiTranspose(chromatic, diatonic);
                }
            }
            if (safeStaff == 1) {
                break;
            }
        }
        return null;
    }

    public static String buildMeiExportScoreDefXml(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            throw new IllegalArgumentException("MusicXML document is missing.");
        }
        List<Element> parts = directChildElementsByLocalName(doc.getDocumentElement(), "part");
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("MusicXML part is missing.");
        }
        Element scoreDefSource = null;
        for (Element measure : directChildElementsByLocalName(parts.get(0), "measure")) {
            scoreDefSource = firstDirectChildLocal(measure, "attributes");
            if (scoreDefSource != null) {
                break;
            }
        }
        int meterCount = parseIntSafe(firstNestedDirectChildText(scoreDefSource, "time", "beats"), 4);
        int meterUnit = parseIntSafe(firstNestedDirectChildText(scoreDefSource, "time", "beat-type"), 4);
        int fifths = parseIntSafe(firstNestedDirectChildText(scoreDefSource, "key", "fifths"), 0);
        List<MusicXmlStaffSlot> slots = collectMusicXmlStaffSlots(doc);
        StringBuilder xml = new StringBuilder();
        xml.append("<scoreDef meter.count=\"").append(meterCount).append("\" meter.unit=\"").append(meterUnit)
                .append("\" key.sig=\"").append(xmlEscape(fifthsToMeiKeySig(fifths))).append("\">");
        xml.append("<staffGrp>");
        for (MusicXmlStaffSlot slot : slots) {
            Element part = findDirectMusicXmlPartById(doc, slot.getPartId());
            MeiClef clef = resolveClefForMusicXmlSlot(part, slot.getLocalStaff());
            MeiTranspose transpose = resolveTransposeForMusicXmlSlot(part, slot.getLocalStaff());
            xml.append("<staffDef n=\"").append(slot.getGlobalStaff()).append("\" label=\"")
                    .append(xmlEscape(slot.getLabel())).append("\" lines=\"5\" clef.shape=\"")
                    .append(xmlEscape(clef.getClefSign())).append("\" clef.line=\"").append(clef.getClefLine())
                    .append("\"");
            if (transpose != null && transpose.getDiatonic() != null) {
                xml.append(" trans.diat=\"").append(transpose.getDiatonic().intValue()).append("\"");
            }
            if (transpose != null && transpose.getChromatic() != null) {
                xml.append(" trans.semi=\"").append(transpose.getChromatic().intValue()).append("\"");
            }
            xml.append("><label>").append(xmlEscape(slot.getLabel())).append("</label><clef shape=\"")
                    .append(xmlEscape(clef.getClefSign())).append("\" line=\"").append(clef.getClefLine())
                    .append("\"/></staffDef>");
        }
        xml.append("</staffGrp></scoreDef>");
        return xml.toString();
    }

    public static List<String> gatherMusicXmlMeasureNumbers(Collection<Element> parts) {
        List<String> out = new ArrayList<String>();
        if (parts == null) {
            return out;
        }
        for (Element part : parts) {
            for (Element measure : directChildElementsByLocalName(part, "measure")) {
                String number = measure.getAttribute("number") == null ? "" : measure.getAttribute("number").trim();
                if (number.length() == 0) {
                    number = Integer.toString(out.size() + 1);
                }
                if (!out.contains(number)) {
                    out.add(number);
                }
            }
        }
        return out;
    }

    public static List<String> gatherMusicXmlMeasureNumbers(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.emptyList();
        }
        return gatherMusicXmlMeasureNumbers(directChildElementsByLocalName(doc.getDocumentElement(), "part"));
    }

    public static String resolveMusicXmlTitleForMeiExport(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return "mikuscore";
        }
        Element root = doc.getDocumentElement();
        for (Element work : directChildElementsByLocalName(root, "work")) {
            String title = firstDirectChildText(work, "work-title").trim();
            if (title.length() > 0) {
                return title;
            }
        }
        String movementTitle = firstDirectChildText(root, "movement-title").trim();
        return movementTitle.length() > 0 ? movementTitle : "mikuscore";
    }

    public static String buildMeiExportDocumentXml(String title, String meiVersion, String scoreDefXml,
            String measuresXml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\""
                + xmlEscape(normalizeMeiVersion(meiVersion))
                + "\"><meiHead><fileDesc><titleStmt><title>" + xmlEscape(title == null || title.trim().length() == 0
                        ? "mikuscore" : title.trim())
                + "</title></titleStmt><pubStmt><p>Generated by mikuscore</p></pubStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score>" + (scoreDefXml == null ? "" : scoreDefXml) + "<section>"
                + (measuresXml == null ? "" : measuresXml) + "</section></score></mdiv></body></music></mei>";
    }

    public static String exportMusicXmlDomToMei(Document doc) {
        return exportMusicXmlDomToMei(doc, null);
    }

    public static String exportMusicXmlDomToMei(Document doc, String meiVersion) {
        if (doc == null || doc.getDocumentElement() == null) {
            throw new IllegalArgumentException("MusicXML document is missing.");
        }
        List<Element> parts = directChildElementsByLocalName(doc.getDocumentElement(), "part");
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("MusicXML part is missing.");
        }
        List<MusicXmlStaffSlot> slots = collectMusicXmlStaffSlots(doc);
        List<String> measureNumbers = gatherMusicXmlMeasureNumbers(parts);
        Map<String, Integer> currentDivisionsByPart = new HashMap<String, Integer>();
        Map<String, MeiExportTieSlurCarryState> tieSlurStateByStaffKey = new HashMap<String, MeiExportTieSlurCarryState>();
        for (Element part : parts) {
            String partId = part.getAttribute("id") == null ? "" : part.getAttribute("id").trim();
            if (partId.length() == 0) {
                continue;
            }
            int firstDivisions = parseIntSafe(firstNestedDirectChildText(firstDirectChildLocal(part, "measure"),
                    "attributes", "divisions"), 1);
            currentDivisionsByPart.put(partId, Integer.valueOf(Math.max(1, firstDivisions)));
        }
        Element scoreDefSource = null;
        for (Element measure : directChildElementsByLocalName(parts.get(0), "measure")) {
            scoreDefSource = firstDirectChildLocal(measure, "attributes");
            if (scoreDefSource != null) {
                break;
            }
        }
        int meterCount = parseIntSafe(firstNestedDirectChildText(scoreDefSource, "time", "beats"), 4);
        int meterUnit = parseIntSafe(firstNestedDirectChildText(scoreDefSource, "time", "beat-type"), 4);
        int[] nextGeneratedNoteId = new int[] { 1 };
        StringBuilder measuresXml = new StringBuilder();
        for (String number : measureNumbers) {
            List<String> measureLines = new ArrayList<String>();
            List<String> measureControlNodes = new ArrayList<String>();
            measureLines.add("<measure n=\"" + xmlEscape(number) + "\">");
            for (MusicXmlStaffSlot slot : slots) {
                Element part = findDirectMusicXmlPartById(doc, slot.getPartId());
                if (part == null) {
                    continue;
                }
                Element measure = findMusicXmlMeasureByNumber(part, number);
                if (measure == null) {
                    continue;
                }
                String partId = part.getAttribute("id") == null ? "" : part.getAttribute("id").trim();
                int measureDivisions = parseIntSafe(firstNestedDirectChildText(measure, "attributes", "divisions"),
                        Integer.MIN_VALUE);
                if (measureDivisions > 0 && partId.length() > 0) {
                    currentDivisionsByPart.put(partId, Integer.valueOf(measureDivisions));
                }
                int sourceDivisions = Math.max(1,
                        currentDivisionsByPart.containsKey(partId) ? currentDivisionsByPart.get(partId).intValue() : 1);
                int beatType = parseIntSafe(firstNestedDirectChildText(firstDirectChildLocal(measure, "attributes"),
                        "time", "beat-type"), meterUnit);
                Map<String, List<Element>> voiceMap = collectMusicXmlNotesByVoiceForStaff(measure,
                        slot.getLocalStaff());
                if (voiceMap.isEmpty()) {
                    continue;
                }
                measureLines.add("<staff n=\"" + slot.getGlobalStaff() + "\">");
                Map<Element, String> noteIdBySource = new java.util.IdentityHashMap<Element, String>();
                for (MiscField field : extractMusicXmlMiscellaneousFieldsFromMeasure(measure)) {
                    measureLines.add("<annot type=\"musicxml-misc-field\" label=\"" + xmlEscape(field.getName())
                            + "\">" + xmlEscape(field.getValue()) + "</annot>");
                }
                String measureMeta = encodeMusicXmlMeasureMetaForMei(measure);
                if (measureMeta != null && measureMeta.length() > 0) {
                    measureLines.add("<annot type=\"musicxml-measure-meta\" label=\"mks:measure-meta\">"
                            + xmlEscape(measureMeta) + "</annot>");
                }
                List<String> voices = new ArrayList<String>(voiceMap.keySet());
                Collections.sort(voices, new java.util.Comparator<String>() {
                    public int compare(String left, String right) {
                        return compareMusicXmlVoice(left, right);
                    }
                });
                for (String voice : voices) {
                    int measureBeats = parseIntSafe(firstNestedDirectChildText(firstDirectChildLocal(measure,
                            "attributes"), "time", "beats"), meterCount);
                    int measureTicks = Math.max(1, Math.round((measureBeats * 4.0f * sourceDivisions)
                            / Math.max(1, beatType)));
                    String layer = buildMeiLayerContentFromMusicXmlNotes(voiceMap.get(voice), sourceDivisions,
                            measureTicks, noteIdBySource, nextGeneratedNoteId);
                    measureLines.add("<layer n=\"" + xmlEscape(voice) + "\">" + layer + "</layer>");
                }
                appendMeiExportControlNodes(measureLines, measureControlNodes,
                        collectMeiDirectionControlsForStaff(measure, slot.getLocalStaff(), sourceDivisions, beatType),
                        slot.getGlobalStaff());
                appendMeiExportControlNodes(measureLines, measureControlNodes,
                        collectMeiGlissSlideControlsForStaff(measure, slot.getLocalStaff(), sourceDivisions, beatType),
                        slot.getGlobalStaff());
                String tieSlurKey = slot.getPartId() + ":" + slot.getLocalStaff();
                if (!tieSlurStateByStaffKey.containsKey(tieSlurKey)) {
                    tieSlurStateByStaffKey.put(tieSlurKey, new MeiExportTieSlurCarryState());
                }
                appendMeiExportControlNodes(measureLines, measureControlNodes,
                        collectMeiTieSlurControlsForStaff(measure, slot.getLocalStaff(), sourceDivisions, beatType,
                                noteIdBySource, tieSlurStateByStaffKey.get(tieSlurKey)),
                        slot.getGlobalStaff());
                appendMeiExportControlNodes(measureLines, measureControlNodes,
                        collectMeiOrnamentAndBreathControlsForStaff(measure, slot.getLocalStaff(), sourceDivisions,
                                beatType),
                        slot.getGlobalStaff());
                appendMeiExportControlNodes(measureLines, measureControlNodes,
                        collectMeiHarmsForStaff(measure, slot.getLocalStaff(), sourceDivisions, beatType),
                        slot.getGlobalStaff());
                measureLines.add("</staff>");
            }
            measureLines.addAll(measureControlNodes);
            measureLines.add("</measure>");
            measuresXml.append(joinStrings(measureLines));
        }
        return buildMeiExportDocumentXml(resolveMusicXmlTitleForMeiExport(doc), meiVersion, buildMeiExportScoreDefXml(doc),
                measuresXml.toString());
    }

    public static List<MiscField> extractMusicXmlMiscellaneousFieldsFromMeasure(Element measure) {
        List<MiscField> out = new ArrayList<MiscField>();
        if (measure == null) {
            return out;
        }
        for (Element attributes : directChildElementsByLocalName(measure, "attributes")) {
            for (Element miscellaneous : directChildElementsByLocalName(attributes, "miscellaneous")) {
                for (Element field : directChildElementsByLocalName(miscellaneous, "miscellaneous-field")) {
                    String name = field.getAttribute("name") == null ? "" : field.getAttribute("name").trim();
                    if (name.length() > 0) {
                        out.add(new MiscField(name, textOf(field).trim()));
                    }
                }
            }
        }
        return out;
    }

    public static String encodeMusicXmlMeasureMetaForMei(Element measure) {
        if (measure == null) {
            return null;
        }
        List<String> parts = new ArrayList<String>();
        String rawNo = measure.getAttribute("number") == null ? "" : measure.getAttribute("number").trim();
        if (rawNo.length() > 0) {
            parts.add("number=" + rawNo);
        }
        String implicitRaw = measure.getAttribute("implicit") == null ? ""
                : measure.getAttribute("implicit").trim().toLowerCase();
        if ("yes".equals(implicitRaw) || "true".equals(implicitRaw) || "1".equals(implicitRaw)) {
            parts.add("implicit=1");
        }
        boolean leftRepeat = hasDirectBarlineChild(measure, "left", "repeat", "direction", "forward");
        boolean rightRepeat = hasDirectBarlineChild(measure, "right", "repeat", "direction", "backward");
        if (rightRepeat) {
            parts.add("repeat=backward");
        } else if (leftRepeat) {
            parts.add("repeat=forward");
        }
        Integer times = parsePositiveEndingStopNumber(measure);
        if (times != null && times.intValue() > 1) {
            parts.add("times=" + times.intValue());
        }
        Element time = firstNestedDirectChild(firstDirectChildLocal(measure, "attributes"), "time");
        if (time != null) {
            parts.add("explicitTime=1");
            int beats = parseIntSafe(firstDirectChildText(time, "beats"), Integer.MIN_VALUE);
            int beatType = parseIntSafe(firstDirectChildText(time, "beat-type"), Integer.MIN_VALUE);
            if (beats > 0) {
                parts.add("beats=" + beats);
            }
            if (beatType > 0) {
                parts.add("beatType=" + beatType);
            }
        }
        boolean leftDouble = hasDirectBarlineText(measure, "left", "bar-style", "light-light");
        boolean rightDouble = hasDirectBarlineText(measure, "right", "bar-style", "light-light");
        if (leftDouble && rightDouble) {
            parts.add("doubleBar=both");
        } else if (leftDouble) {
            parts.add("doubleBar=left");
        } else if (rightDouble) {
            parts.add("doubleBar=right");
        }
        return parts.isEmpty() ? null : joinSemicolon(parts);
    }

    public static String buildMeiArticulationChildren(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        for (String token : tokens) {
            xml.append("<artic artic=\"").append(xmlEscape(token)).append("\"/>");
        }
        return xml.toString();
    }

    public static MeiLyric extractMusicXmlLyric(Element note) {
        Element lyric = firstDirectChildLocal(note, "lyric");
        if (lyric == null) {
            return null;
        }
        String text = firstDirectChildText(lyric, "text").trim();
        if (text.length() == 0) {
            return null;
        }
        return new MeiLyric(text, firstDirectChildText(lyric, "syllabic").trim());
    }

    public static String buildSimpleMeiPitchNote(Element note, int sourceDivisions) {
        return buildSimpleMeiPitchNote(note, sourceDivisions, true, null);
    }

    public static String buildSimpleMeiPitchNote(Element note, int sourceDivisions, boolean includeGraceAttr,
            String xmlId) {
        String typeText = firstDirectChildText(note, "type").trim();
        if (typeText.length() == 0) {
            typeText = "quarter";
        }
        String dur = noteTypeToDur(typeText);
        int dots = directChildElementsByLocalName(note, "dot").size();
        Element pitch = firstDirectChildLocal(note, "pitch");
        String step = firstDirectChildText(pitch, "step").trim();
        if (step.length() == 0) {
            step = "C";
        }
        String octaveText = firstDirectChildText(pitch, "octave").trim();
        if (octaveText.length() == 0) {
            octaveText = "4";
        }
        String explicitAccid = musicXmlAccidentalToAccid(firstDirectChildText(note, "accidental"));
        String accid = explicitAccid == null ? alterToAccid(firstDirectChildText(pitch, "alter")) : explicitAccid;
        List<String> attrs = new ArrayList<String>();
        attrs.add("pname=\"" + xmlEscape(toPname(step)) + "\"");
        attrs.add("oct=\"" + xmlEscape(octaveText) + "\"");
        attrs.add("dur=\"" + xmlEscape(dur) + "\"");
        if (xmlId != null && xmlId.trim().length() > 0) {
            attrs.add("xml:id=\"" + xmlEscape(xmlId.trim()) + "\"");
        }
        int durationTicks = parseIntSafe(firstDirectChildText(note, "duration"), Integer.MIN_VALUE);
        if (durationTicks > 0) {
            attrs.add("mks-dur-480=\"" + toMksDur480(durationTicks, sourceDivisions) + "\"");
            attrs.add("mks-dur-div=\"" + Math.max(1, sourceDivisions) + "\"");
            attrs.add("mks-dur-ticks=\"" + durationTicks + "\"");
        }
        Element timeModification = firstDirectChildLocal(note, "time-modification");
        int actual = parseIntSafe(firstDirectChildText(timeModification, "actual-notes"), Integer.MIN_VALUE);
        int normal = parseIntSafe(firstDirectChildText(timeModification, "normal-notes"), Integer.MIN_VALUE);
        boolean tupletStart = hasDirectTupletType(note, "start");
        boolean tupletStop = hasDirectTupletType(note, "stop");
        List<String> arts = extractMeiArticulationTokensFromMusicXmlNote(note);
        if (dots > 0) {
            attrs.add("dots=\"" + dots + "\"");
        }
        if (accid != null && accid.length() > 0) {
            attrs.add("accid=\"" + xmlEscape(accid) + "\"");
        }
        if (actual > 0 && normal > 0) {
            attrs.add("num=\"" + actual + "\"");
            attrs.add("numbase=\"" + normal + "\"");
        }
        if (tupletStart) {
            attrs.add("mks-tuplet-start=\"1\"");
        }
        if (tupletStop) {
            attrs.add("mks-tuplet-stop=\"1\"");
        }
        Element grace = firstDirectChildLocal(note, "grace");
        if (includeGraceAttr && grace != null) {
            String slash = grace.getAttribute("slash") == null ? "" : grace.getAttribute("slash").trim().toLowerCase();
            attrs.add("grace=\"" + ("yes".equals(slash) ? "acc" : "unacc") + "\"");
        }
        String tieAttr = extractMeiTieFromMusicXmlTieTypes(directTieTypes(note));
        if (tieAttr.length() > 0) {
            attrs.add("tie=\"" + xmlEscape(tieAttr) + "\"");
        }
        String articulationXml = buildMeiArticulationChildren(arts);
        MeiLyric lyric = extractMusicXmlLyric(note);
        StringBuilder body = new StringBuilder();
        if (lyric != null) {
            String wordpos = lyricWordposFromSyllabic(lyric.getSyllabic());
            body.append("<verse n=\"1\"><syl");
            if (wordpos.length() > 0) {
                body.append(" wordpos=\"").append(xmlEscape(wordpos)).append("\"");
            }
            body.append(">").append(xmlEscape(lyric.getText())).append("</syl></verse>");
        }
        body.append(articulationXml);
        return "<note " + joinSpace(attrs) + (body.length() == 0 ? "/>" : ">" + body.toString() + "</note>");
    }

    public static String buildSimpleMeiRest(Element note, int sourceDivisions) {
        String typeText = firstDirectChildText(note, "type").trim();
        if (typeText.length() == 0) {
            typeText = "quarter";
        }
        List<String> attrs = new ArrayList<String>();
        attrs.add("dur=\"" + xmlEscape(noteTypeToDur(typeText)) + "\"");
        int durationTicks = parseIntSafe(firstDirectChildText(note, "duration"), Integer.MIN_VALUE);
        if (durationTicks > 0) {
            attrs.add("mks-dur-480=\"" + toMksDur480(durationTicks, sourceDivisions) + "\"");
            attrs.add("mks-dur-div=\"" + Math.max(1, sourceDivisions) + "\"");
            attrs.add("mks-dur-ticks=\"" + durationTicks + "\"");
        }
        int dots = directChildElementsByLocalName(note, "dot").size();
        if (dots > 0) {
            attrs.add("dots=\"" + dots + "\"");
        }
        String printObject = note == null || note.getAttribute("print-object") == null ? ""
                : note.getAttribute("print-object").trim().toLowerCase();
        return "<" + ("no".equals(printObject) ? "space" : "rest") + " " + joinSpace(attrs) + "/>";
    }

    public static String buildSimpleMeiChord(List<Element> chordNotes, int sourceDivisions, List<String> xmlIds) {
        if (chordNotes == null || chordNotes.isEmpty()) {
            return "";
        }
        Element first = chordNotes.get(0);
        String typeText = firstDirectChildText(first, "type").trim();
        if (typeText.length() == 0) {
            typeText = "quarter";
        }
        List<String> chordAttrs = new ArrayList<String>();
        chordAttrs.add("dur=\"" + xmlEscape(noteTypeToDur(typeText)) + "\"");
        int durationTicks = parseIntSafe(firstDirectChildText(first, "duration"), Integer.MIN_VALUE);
        if (durationTicks > 0) {
            chordAttrs.add("mks-dur-480=\"" + toMksDur480(durationTicks, sourceDivisions) + "\"");
            chordAttrs.add("mks-dur-div=\"" + Math.max(1, sourceDivisions) + "\"");
            chordAttrs.add("mks-dur-ticks=\"" + durationTicks + "\"");
        }
        int dots = directChildElementsByLocalName(first, "dot").size();
        if (dots > 0) {
            chordAttrs.add("dots=\"" + dots + "\"");
        }
        StringBuilder members = new StringBuilder();
        for (int index = 0; index < chordNotes.size(); index++) {
            String xmlId = xmlIds != null && index < xmlIds.size() ? xmlIds.get(index) : null;
            members.append(buildSimpleMeiChordMember(chordNotes.get(index), xmlId));
        }
        return "<chord " + joinSpace(chordAttrs) + ">" + members.toString() + "</chord>";
    }

    public static String buildMeiLayerContentFromMusicXmlNotes(List<Element> notes, int sourceDivisions,
            int measureTicks) {
        return buildMeiLayerContentFromMusicXmlNotes(notes, sourceDivisions, measureTicks,
                new java.util.IdentityHashMap<Element, String>(), new int[] { 1 });
    }

    public static String buildMeiLayerContentFromMusicXmlNotes(List<Element> notes, int sourceDivisions,
            int measureTicks, Map<Element, String> noteIdBySource, int[] nextGeneratedNoteId) {
        List<Element> safeNotes = notes == null ? Collections.<Element>emptyList() : notes;
        List<Element> pitchNotes = new ArrayList<Element>();
        List<Element> simpleRests = new ArrayList<Element>();
        for (Element note : safeNotes) {
            if (hasDirectChildLocal(note, "rest")) {
                simpleRests.add(note);
            } else {
                pitchNotes.add(note);
            }
        }
        if (pitchNotes.isEmpty() && simpleRests.size() == 1 && safeNotes.size() == 1) {
            Element only = simpleRests.get(0);
            boolean isGrace = hasDirectChildLocal(only, "grace");
            int durationTicks = parseIntSafe(firstDirectChildText(only, "duration"), 0);
            if (!isGrace && durationTicks == measureTicks && measureTicks > 0) {
                String printObject = only.getAttribute("print-object") == null ? ""
                        : only.getAttribute("print-object").trim().toLowerCase();
                String tagName = "no".equals(printObject) ? "mSpace" : "mRest";
                MeiDurDots inferred = inferMeiDurAndDotsFromTicks(measureTicks, sourceDivisions);
                String dotsAttr = inferred.getDots() > 0 ? " dots=\"" + inferred.getDots() + "\"" : "";
                return "<" + tagName + " dur=\"" + xmlEscape(inferred.getDur()) + "\"" + dotsAttr
                        + " mks-dur-480=\"" + toMksDur480(measureTicks, sourceDivisions)
                        + "\" mks-dur-div=\"" + Math.max(1, sourceDivisions) + "\" mks-dur-ticks=\""
                        + measureTicks + "\"/>";
            }
        }
        Map<Element, String> ids = noteIdBySource == null ? new java.util.IdentityHashMap<Element, String>()
                : noteIdBySource;
        int[] nextId = nextGeneratedNoteId == null || nextGeneratedNoteId.length == 0 ? new int[] { 1 }
                : nextGeneratedNoteId;
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < safeNotes.size(); index++) {
            Element note = safeNotes.get(index);
            boolean isRest = hasDirectChildLocal(note, "rest");
            boolean hasChordFlag = hasDirectChildLocal(note, "chord");
            boolean isGracePitchNote = !isRest && !hasChordFlag && hasDirectChildLocal(note, "grace");
            if (isGracePitchNote) {
                ensureGeneratedNoteId(ids, note, nextId);
                List<Element> graceGroup = new ArrayList<Element>();
                graceGroup.add(note);
                boolean hasSlash = isGraceSlash(note);
                for (int nextIndex = index + 1; nextIndex < safeNotes.size(); nextIndex++) {
                    Element next = safeNotes.get(nextIndex);
                    boolean nextIsRest = hasDirectChildLocal(next, "rest");
                    boolean nextHasChordFlag = hasDirectChildLocal(next, "chord");
                    boolean nextIsGracePitch = !nextIsRest && !nextHasChordFlag && hasDirectChildLocal(next, "grace");
                    if (!nextIsGracePitch) {
                        break;
                    }
                    ensureGeneratedNoteId(ids, next, nextId);
                    graceGroup.add(next);
                    hasSlash = hasSlash || isGraceSlash(next);
                    index = nextIndex;
                }
                out.append("<graceGrp slash=\"").append(hasSlash ? "yes" : "no").append("\">");
                for (Element graceNote : graceGroup) {
                    out.append(buildSimpleMeiPitchNote(graceNote, sourceDivisions, false,
                            ids.get(graceNote)));
                }
                out.append("</graceGrp>");
                continue;
            }
            if (isRest || hasChordFlag) {
                if (isRest) {
                    out.append(buildSimpleMeiRest(note, sourceDivisions));
                } else {
                    out.append(buildSimpleMeiPitchNote(note, sourceDivisions, true,
                            ensureGeneratedNoteId(ids, note, nextId)));
                }
                continue;
            }
            List<Element> chordNotes = new ArrayList<Element>();
            chordNotes.add(note);
            for (int nextIndex = index + 1; nextIndex < safeNotes.size(); nextIndex++) {
                Element next = safeNotes.get(nextIndex);
                if (!hasDirectChildLocal(next, "chord")) {
                    break;
                }
                chordNotes.add(next);
                index = nextIndex;
            }
            if (chordNotes.size() == 1) {
                out.append(buildSimpleMeiPitchNote(note, sourceDivisions, true,
                        ensureGeneratedNoteId(ids, note, nextId)));
                continue;
            }
            List<String> xmlIds = new ArrayList<String>();
            for (Element chordNote : chordNotes) {
                xmlIds.add(ensureGeneratedNoteId(ids, chordNote, nextId));
            }
            out.append(buildSimpleMeiChord(chordNotes, sourceDivisions, xmlIds));
        }
        return out.toString();
    }

    public static String accidentalTextFromAlter(int alter) {
        if (alter == 0) {
            return "";
        }
        if (alter == 1) {
            return "#";
        }
        if (alter == -1) {
            return "b";
        }
        if (alter == 2) {
            return "##";
        }
        if (alter == -2) {
            return "bb";
        }
        return "";
    }

    public static int parseHarmonyAlter(String token) {
        String normalized = token == null ? "" : token.trim();
        if ("#".equals(normalized) || "\u266F".equals(normalized)) {
            return 1;
        }
        if ("b".equals(normalized) || "\u266D".equals(normalized)) {
            return -1;
        }
        if ("x".equals(normalized) || "##".equals(normalized)) {
            return 2;
        }
        return 0;
    }

    public static HarmonyKindSuffix suffixFromHarmonyKind(String kindText, String textAttribute) {
        String text = textAttribute == null ? "" : textAttribute.trim();
        if (text.length() > 0) {
            return new HarmonyKindSuffix(text, true);
        }
        String kind = kindText == null ? "" : kindText.trim().toLowerCase();
        if ("major".equals(kind)) {
            return new HarmonyKindSuffix("", false);
        }
        if ("minor".equals(kind)) {
            return new HarmonyKindSuffix("m", false);
        }
        if ("dominant".equals(kind)) {
            return new HarmonyKindSuffix("7", false);
        }
        if ("major-seventh".equals(kind)) {
            return new HarmonyKindSuffix("maj7", false);
        }
        if ("minor-seventh".equals(kind)) {
            return new HarmonyKindSuffix("m7", false);
        }
        if ("diminished".equals(kind)) {
            return new HarmonyKindSuffix("dim", false);
        }
        if ("augmented".equals(kind)) {
            return new HarmonyKindSuffix("aug", false);
        }
        return new HarmonyKindSuffix(kind.length() > 0 && !"other".equals(kind) ? kind : "", false);
    }

    public static ParsedMeiHarmonyText parseMeiHarmText(String text) {
        String raw = text == null ? "" : text.trim();
        Matcher match = Pattern.compile("^([A-Ga-g])([#bx\\u266D\\u266F]?)([^/]*)(?:/([A-Ga-g])([#bx\\u266D\\u266F]?))?$")
                .matcher(raw);
        if (!match.matches()) {
            return null;
        }
        String suffix = match.group(3) == null ? "" : match.group(3);
        String suffixLower = suffix.toLowerCase();
        String kind = "other";
        String kindText = suffix.length() == 0 ? "" : suffix;
        if (suffix.length() == 0) {
            kind = "major";
            kindText = "";
        } else if ("m".equals(suffixLower) || "min".equals(suffixLower)) {
            kind = "minor";
            kindText = "";
        } else if ("7".equals(suffixLower)) {
            kind = "dominant";
            kindText = "";
        } else if ("maj7".equals(suffixLower)) {
            kind = "major-seventh";
            kindText = "";
        } else if ("m7".equals(suffixLower) || "min7".equals(suffixLower)) {
            kind = "minor-seventh";
            kindText = "";
        } else if ("dim".equals(suffixLower)) {
            kind = "diminished";
            kindText = "";
        } else if ("aug".equals(suffixLower) || "+".equals(suffixLower)) {
            kind = "augmented";
            kindText = "";
        }

        List<HarmonyDegree> degrees = new ArrayList<HarmonyDegree>();
        Matcher degreeMatcher = Pattern.compile("(##|[#b\\u266D\\u266Fx])\\s*(\\d{1,2})").matcher(suffix);
        while (degreeMatcher.find()) {
            degrees.add(new HarmonyDegree(parseIntSafe(degreeMatcher.group(2), 0),
                    parseHarmonyAlter(degreeMatcher.group(1))));
        }

        String bassStep = match.group(4) == null ? null : match.group(4).toUpperCase();
        Integer bassAlter = match.group(5) == null || match.group(5).length() == 0 ? null
                : Integer.valueOf(parseHarmonyAlter(match.group(5)));
        return new ParsedMeiHarmonyText(match.group(1).toUpperCase(), parseHarmonyAlter(match.group(2)), kind,
                kindText, bassStep, bassAlter, degrees);
    }

    public static String degreeSuffixFromHarmony(Collection<HarmonyDegree> degrees) {
        if (degrees == null || degrees.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (HarmonyDegree degree : degrees) {
            if (degree == null || degree.getAlter() == 0) {
                continue;
            }
            out.append(accidentalTextFromAlter(degree.getAlter())).append(degree.getValue());
        }
        return out.toString();
    }

    public static String offsetTicksToTstamp(int offsetTicks, int divisions, int beatType) {
        double ticksPerBeat = Math.max(1.0d, (4.0d * Math.max(1, divisions)) / Math.max(1, beatType));
        double beatPos = 1.0d + (Math.max(0, offsetTicks) / ticksPerBeat);
        double rounded = Math.round(beatPos * 1000.0d) / 1000.0d;
        String text = Double.toString(rounded);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        while (text.indexOf('.') >= 0 && text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    public static String buildMeiHarmFromMusicXmlHarmonyValues(String rootStep, Integer rootAlter, String kindText,
            String kindTextAttribute, String bassStep, Integer bassAlter, Collection<HarmonyDegree> degrees,
            int offsetTicks, int sourceDivisions, int beatType) {
        String root = rootStep == null ? "" : rootStep.trim().toUpperCase();
        if (!isPitchStep(root)) {
            return null;
        }
        HarmonyKindSuffix kindSuffix = suffixFromHarmonyKind(kindText, kindTextAttribute);
        String suffix = kindSuffix.getSuffix()
                + (kindSuffix.isFromText() ? "" : degreeSuffixFromHarmony(degrees));
        String bass = bassStep == null ? "" : bassStep.trim().toUpperCase();
        StringBuilder chordText = new StringBuilder();
        chordText.append(root).append(accidentalTextFromAlter(rootAlter == null ? 0 : rootAlter.intValue()))
                .append(suffix);
        if (isPitchStep(bass)) {
            chordText.append("/").append(bass)
                    .append(accidentalTextFromAlter(bassAlter == null ? 0 : bassAlter.intValue()));
        }
        String tstamp = offsetTicks > 0 ? offsetTicksToTstamp(offsetTicks, sourceDivisions, beatType) : "1";
        return "<harm tstamp=\"" + xmlEscape(tstamp) + "\">" + xmlEscape(chordText.toString()) + "</harm>";
    }

    public static String buildMeiHarmFromMusicXmlHarmony(Element harmony, int sourceDivisions, int beatType) {
        if (harmony == null) {
            return null;
        }
        Element kind = firstDirectChildLocal(harmony, "kind");
        String bassStep = firstNestedDirectChildText(harmony, "bass", "bass-step").trim();
        List<HarmonyDegree> degrees = new ArrayList<HarmonyDegree>();
        for (Element degree : directChildElementsByLocalName(harmony, "degree")) {
            int value = parseIntSafe(firstDirectChildText(degree, "degree-value"), Integer.MIN_VALUE);
            int alter = parseIntSafe(firstDirectChildText(degree, "degree-alter"), Integer.MIN_VALUE);
            if (value != Integer.MIN_VALUE && alter != Integer.MIN_VALUE) {
                degrees.add(new HarmonyDegree(value, alter));
            }
        }
        return buildMeiHarmFromMusicXmlHarmonyValues(
                firstNestedDirectChildText(harmony, "root", "root-step"),
                Integer.valueOf(parseIntSafe(firstNestedDirectChildText(harmony, "root", "root-alter"), 0)),
                textOf(kind), kind == null ? "" : kind.getAttribute("text"), bassStep.length() == 0 ? null : bassStep,
                bassStep.length() == 0 ? null
                        : Integer.valueOf(parseIntSafe(firstNestedDirectChildText(harmony, "bass", "bass-alter"), 0)),
                degrees, parseIntSafe(firstDirectChildText(harmony, "offset"), 0), sourceDivisions, beatType);
    }

    public static List<String> collectMeiHarmsForStaff(Element measure, int localStaff, int sourceDivisions,
            int beatType) {
        if (measure == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        for (Element harmony : directChildElementsByLocalName(measure, "harmony")) {
            int staffNo = parseIntSafe(firstDirectChildText(harmony, "staff"), 1);
            if (staffNo != localStaff) {
                continue;
            }
            String xml = buildMeiHarmFromMusicXmlHarmony(harmony, sourceDivisions, beatType);
            if (xml != null) {
                out.add(xml);
            }
        }
        return out;
    }

    public static String withStaffAttr(String nodeXml, int staffNo) {
        String xml = nodeXml == null ? "" : nodeXml;
        if (Pattern.compile("\\bstaff=\"[^\"]+\"").matcher(xml).find()) {
            return xml;
        }
        Matcher matcher = Pattern.compile("^<([A-Za-z][\\w.-]*)(\\s|>)").matcher(xml);
        if (!matcher.find()) {
            return xml;
        }
        String replacement = "<" + matcher.group(1) + " staff=\"" + Math.max(1, staffNo) + "\"" + matcher.group(2);
        return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    public static Map<Element, Integer> collectDirectionOnsetTicksInMeasure(Element measure) {
        Map<Element, Integer> out = new java.util.IdentityHashMap<Element, Integer>();
        int cursor = 0;
        if (measure == null) {
            return out;
        }
        NodeList children = measure.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (!(childNode instanceof Element)) {
                continue;
            }
            Element child = (Element) childNode;
            String kind = localNameOf(child);
            if ("backup".equals(kind)) {
                int duration = Math.max(0, parseIntSafe(firstDirectChildText(child, "duration"), 0));
                cursor = Math.max(0, cursor - duration);
                continue;
            }
            if ("forward".equals(kind)) {
                cursor += Math.max(0, parseIntSafe(firstDirectChildText(child, "duration"), 0));
                continue;
            }
            if ("note".equals(kind)) {
                boolean isChord = hasDirectChildLocal(child, "chord");
                boolean isGrace = hasDirectChildLocal(child, "grace");
                if (!isChord && !isGrace) {
                    cursor += Math.max(0, parseIntSafe(firstDirectChildText(child, "duration"), 0));
                }
                continue;
            }
            if ("direction".equals(kind)) {
                Element offsetNode = firstDirectChildLocal(child, "offset");
                int onset = cursor;
                if (offsetNode != null && textOf(offsetNode).trim().length() > 0) {
                    onset = Math.max(0, cursor + parseIntSafe(textOf(offsetNode), 0));
                }
                out.put(child, Integer.valueOf(onset));
            }
        }
        return out;
    }

    public static int directionOffsetTicks(Element direction, Map<Element, Integer> onsetTicksByDirection) {
        Integer mapped = onsetTicksByDirection == null ? null : onsetTicksByDirection.get(direction);
        if (mapped != null) {
            return Math.max(0, mapped.intValue());
        }
        return Math.max(0, parseIntSafe(firstDirectChildText(direction, "offset"), 0));
    }

    public static String directionTstamp(Element direction, int divisions, int beatType,
            Map<Element, Integer> onsetTicksByDirection) {
        return offsetTicksToTstamp(directionOffsetTicks(direction, onsetTicksByDirection), divisions, beatType);
    }

    public static boolean directionStaffMatches(Element direction, int localStaff) {
        return parseIntSafe(firstDirectChildText(direction, "staff"), 1) == localStaff;
    }

    public static List<String> collectMeiDirectionControlsForStaff(Element measure, int localStaff, int sourceDivisions,
            int beatType) {
        if (measure == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        Map<Element, Integer> onsetTicksByDirection = collectDirectionOnsetTicksInMeasure(measure);
        List<Element> directions = new ArrayList<Element>();
        for (Element direction : directChildElementsByLocalName(measure, "direction")) {
            if (directionStaffMatches(direction, localStaff)) {
                directions.add(direction);
            }
        }
        for (Element direction : directions) {
            String placeAttr = meiPlaceAttribute(direction.getAttribute("placement"));
            String tstamp = directionTstamp(direction, sourceDivisions, beatType, onsetTicksByDirection);
            Element dynamics = firstDirectionTypeChild(direction, "dynamics");
            if (dynamics != null) {
                String symbol = firstElementChildLocalName(dynamics);
                if (symbol.length() > 0) {
                    out.add("<dynam tstamp=\"" + xmlEscape(tstamp) + "\"" + placeAttr + ">" + xmlEscape(symbol)
                            + "</dynam>");
                    continue;
                }
            }
            Element sound = firstDirectChildLocal(direction, "sound");
            double tempo = parsePositiveDouble(sound == null ? "" : sound.getAttribute("tempo"));
            String words = firstDirectionTypeText(direction, "words").trim();
            if (words.length() > 0 && tempo > 0.0d) {
                out.add("<tempo tstamp=\"" + xmlEscape(tstamp) + "\" midi.bpm=\"" + xmlEscape(formatInferredMeiTempo(tempo))
                        + "\"" + placeAttr + ">" + xmlEscape(words) + "</tempo>");
                continue;
            }
            if (words.length() > 0) {
                out.add("<dynam tstamp=\"" + xmlEscape(tstamp) + "\"" + placeAttr + ">" + xmlEscape(words)
                        + "</dynam>");
            }
        }
        if (localStaff == 1) {
            for (Element sound : directChildElementsByLocalName(measure, "sound")) {
                double tempo = parsePositiveDouble(sound.getAttribute("tempo"));
                if (tempo <= 0.0d) {
                    continue;
                }
                int offsetTicks = Math.max(0, parseIntSafe(sound.getAttribute("offset"), 0));
                String bpm = formatInferredMeiTempo(tempo);
                out.add("<tempo type=\"mscore-infer-from-text\" tstamp=\""
                        + xmlEscape(offsetTicksToTstamp(offsetTicks, sourceDivisions, beatType)) + "\" midi.bpm=\""
                        + xmlEscape(bpm) + "\">\u2669 = " + xmlEscape(bpm) + "</tempo>");
            }
        }
        appendMeiHairpinControls(out, directions, sourceDivisions, beatType, onsetTicksByDirection);
        appendMeiPedalControls(out, directions, sourceDivisions, beatType, onsetTicksByDirection);
        appendMeiOctaveControls(out, directions, sourceDivisions, beatType, onsetTicksByDirection);
        appendMeiRepeatMarkControls(out, directions, sourceDivisions, beatType, onsetTicksByDirection);
        return out;
    }

    public static List<MusicXmlStaffTimelineEntry> collectStaffTimelineForMeiExport(Element measure, int localStaff,
            int divisions) {
        return collectStaffTimelineForMeiExport(measure, localStaff, divisions, null);
    }

    public static List<MusicXmlStaffTimelineEntry> collectStaffTimelineForMeiExport(Element measure, int localStaff,
            int divisions, Map<Element, String> noteIdBySource) {
        List<MusicXmlStaffTimelineEntry> out = new ArrayList<MusicXmlStaffTimelineEntry>();
        int cursor = 0;
        if (measure == null) {
            return out;
        }
        NodeList children = measure.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (!(childNode instanceof Element)) {
                continue;
            }
            Element child = (Element) childNode;
            String name = localNameOf(child);
            if ("backup".equals(name)) {
                cursor = Math.max(0, cursor - Math.max(0, parseIntSafe(firstDirectChildText(child, "duration"), 0)));
                continue;
            }
            if ("forward".equals(name)) {
                cursor += Math.max(0, parseIntSafe(firstDirectChildText(child, "duration"), 0));
                continue;
            }
            if (!"note".equals(name)) {
                continue;
            }
            int staffNo = parseIntSafe(firstDirectChildText(child, "staff"), 1);
            boolean isChordContinuation = hasDirectChildLocal(child, "chord");
            int duration = parseIntSafe(firstDirectChildText(child, "duration"), 0);
            if (staffNo == localStaff) {
                out.add(new MusicXmlStaffTimelineEntry(child, cursor,
                        noteIdBySource == null ? null : noteIdBySource.get(child)));
            }
            if (!isChordContinuation) {
                cursor += Math.max(0, duration);
            }
        }
        return out;
    }

    public static List<String> collectMeiGlissSlideControlsForStaff(Element measure, int localStaff, int divisions,
            int beatType) {
        List<String> out = new ArrayList<String>();
        List<MusicXmlStaffTimelineEntry> timeline = collectStaffTimelineForMeiExport(measure, localStaff, divisions);
        Map<String, String[]> pendingByKey = new HashMap<String, String[]>();
        for (MusicXmlStaffTimelineEntry item : timeline) {
            Element note = item.getNote();
            String tstamp = offsetTicksToTstamp(item.getOnset(), divisions, beatType);
            for (Element notation : directNotationChildren(note, "glissando", "slide")) {
                String kind = "slide".equals(localNameOf(notation)) ? "slide" : "gliss";
                String type = notation.getAttribute("type") == null ? ""
                        : notation.getAttribute("type").trim().toLowerCase();
                String number = notation.getAttribute("number") == null
                        || notation.getAttribute("number").trim().length() == 0 ? "1"
                                : notation.getAttribute("number").trim();
                String key = kind + ":" + number;
                if ("start".equals(type)) {
                    pendingByKey.put(key, new String[] { kind, tstamp });
                    continue;
                }
                if ("stop".equals(type)) {
                    String[] pending = pendingByKey.get(key);
                    if (pending != null) {
                        out.add("<" + pending[0] + " tstamp=\"" + xmlEscape(pending[1]) + "\" tstamp2=\""
                                + xmlEscape(tstamp) + "\"/>");
                        pendingByKey.remove(key);
                    } else {
                        out.add("<" + kind + " tstamp=\"" + xmlEscape(tstamp) + "\"/>");
                    }
                }
            }
        }
        for (String[] pending : pendingByKey.values()) {
            out.add("<" + pending[0] + " tstamp=\"" + xmlEscape(pending[1]) + "\"/>");
        }
        return out;
    }

    public static String tiePitchKeyFromMusicXmlNote(Element note) {
        Element pitch = firstDirectChildLocal(note, "pitch");
        if (pitch == null) {
            return null;
        }
        String step = firstDirectChildText(pitch, "step").trim().toUpperCase();
        String octave = firstDirectChildText(pitch, "octave").trim();
        String alter = firstDirectChildText(pitch, "alter").trim();
        if (alter.length() == 0) {
            alter = "0";
        }
        if (!isPitchStep(step) || !Pattern.compile("^-?\\d+$").matcher(octave).matches()) {
            return null;
        }
        String voice = firstDirectChildText(note, "voice").trim();
        if (voice.length() == 0) {
            voice = "1";
        }
        return step + ":" + alter + ":" + octave + ":v" + voice;
    }

    public static List<String> collectMeiTieSlurControlsForStaff(Element measure, int localStaff, int divisions,
            int beatType) {
        return collectMeiTieSlurControlsForStaff(measure, localStaff, divisions, beatType, null, null);
    }

    public static List<String> collectMeiTieSlurControlsForStaff(Element measure, int localStaff, int divisions,
            int beatType, Map<Element, String> noteIdBySource, MeiExportTieSlurCarryState carryState) {
        List<String> out = new ArrayList<String>();
        List<MusicXmlStaffTimelineEntry> timeline = collectStaffTimelineForMeiExport(measure, localStaff, divisions,
                noteIdBySource);
        Map<String, String[]> pendingSlurByNumber = carryState == null ? new HashMap<String, String[]>()
                : carryState.pendingSlurByNumber;
        Map<String, String[]> pendingTieByPitch = carryState == null ? new HashMap<String, String[]>()
                : carryState.pendingTieByPitch;
        for (MusicXmlStaffTimelineEntry item : timeline) {
            Element note = item.getNote();
            String tstamp = offsetTicksToTstamp(item.getOnset(), divisions, beatType);
            for (Element slur : directNotationChildren(note, "slur", "slur")) {
                String type = slur.getAttribute("type") == null ? "" : slur.getAttribute("type").trim().toLowerCase();
                String number = Integer.toString(Math.max(1, parseIntSafe(slur.getAttribute("number"), 1)));
                if ("start".equals(type)) {
                    pendingSlurByNumber.put(number, new String[] { tstamp, item.getNoteId() });
                    continue;
                }
                if ("stop".equals(type)) {
                    String[] pending = pendingSlurByNumber.get(number);
                    if (pending != null) {
                        out.add(buildMeiSpanControlXml("slur", pending[0], tstamp, pending[1], item.getNoteId()));
                        pendingSlurByNumber.remove(number);
                    }
                }
            }
            List<String> tieTypes = new ArrayList<String>();
            for (Element tie : directChildElementsByLocalName(note, "tie")) {
                String type = tie.getAttribute("type") == null ? "" : tie.getAttribute("type").trim().toLowerCase();
                if (type.length() > 0) {
                    tieTypes.add(type);
                }
            }
            if (tieTypes.isEmpty()) {
                continue;
            }
            String pitchKey = tiePitchKeyFromMusicXmlNote(note);
            if (pitchKey == null) {
                continue;
            }
            boolean hasStop = tieTypes.contains("stop");
            boolean hasStart = tieTypes.contains("start");
            if (hasStop) {
                String[] pending = pendingTieByPitch.get(pitchKey);
                if (pending != null) {
                    out.add(buildMeiSpanControlXml("tie", pending[0], tstamp, pending[1], item.getNoteId()));
                    pendingTieByPitch.remove(pitchKey);
                }
            }
            if (hasStart) {
                pendingTieByPitch.put(pitchKey, new String[] { tstamp, item.getNoteId() });
            }
        }
        return out;
    }

    public static List<String> collectMeiOrnamentAndBreathControlsForStaff(Element measure, int localStaff,
            int divisions, int beatType) {
        List<String> out = new ArrayList<String>();
        List<MusicXmlStaffTimelineEntry> timeline = collectStaffTimelineForMeiExport(measure, localStaff, divisions);
        for (MusicXmlStaffTimelineEntry item : timeline) {
            Element note = item.getNote();
            if (firstDirectChildLocal(note, "pitch") == null) {
                continue;
            }
            String tstamp = offsetTicksToTstamp(item.getOnset(), divisions, beatType);
            for (Element notations : directChildElementsByLocalName(note, "notations")) {
                Element ornaments = firstDirectChildLocal(notations, "ornaments");
                if (ornaments != null) {
                    if (hasDirectChildLocal(ornaments, "trill-mark")) {
                        out.add("<trill tstamp=\"" + xmlEscape(tstamp) + "\"/>");
                    }
                    if (hasDirectChildLocal(ornaments, "turn")) {
                        out.add("<turn tstamp=\"" + xmlEscape(tstamp) + "\" type=\"upper\"/>");
                    }
                    if (hasDirectChildLocal(ornaments, "inverted-turn")) {
                        out.add("<turn tstamp=\"" + xmlEscape(tstamp) + "\" type=\"inverted\"/>");
                    }
                    if (hasDirectChildLocal(ornaments, "mordent")) {
                        out.add("<mordent tstamp=\"" + xmlEscape(tstamp) + "\" type=\"upper\"/>");
                    }
                    if (hasDirectChildLocal(ornaments, "inverted-mordent")) {
                        out.add("<mordent tstamp=\"" + xmlEscape(tstamp) + "\" type=\"inverted\"/>");
                    }
                }
                Element fermata = firstDirectChildLocal(notations, "fermata");
                if (fermata != null) {
                    String type = fermata.getAttribute("type") == null ? ""
                            : fermata.getAttribute("type").trim().toLowerCase();
                    String placeAttr = "inverted".equals(type) ? " place=\"below\"" : "";
                    out.add("<fermata tstamp=\"" + xmlEscape(tstamp) + "\"" + placeAttr + "/>");
                }
                Element articulations = firstDirectChildLocal(notations, "articulations");
                if (articulations != null) {
                    if (hasDirectChildLocal(articulations, "breath-mark")) {
                        out.add("<breath tstamp=\"" + xmlEscape(tstamp) + "\"/>");
                    }
                    if (hasDirectChildLocal(articulations, "caesura")) {
                        out.add("<caesura tstamp=\"" + xmlEscape(tstamp) + "\"/>");
                    }
                }
            }
        }
        return out;
    }

    public static String buildTransposeXml(Integer chromatic, Integer diatonic) {
        if (chromatic == null && diatonic == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        out.append("<transpose>");
        if (diatonic != null) {
            out.append("<diatonic>").append(diatonic.intValue()).append("</diatonic>");
        }
        if (chromatic != null) {
            out.append("<chromatic>").append(chromatic.intValue()).append("</chromatic>");
        }
        out.append("</transpose>");
        return out.toString();
    }

    public static String buildTimeXml(int beats, int beatType, String symbol) {
        String normalizedSymbol = symbol == null ? "" : symbol.trim().toLowerCase();
        String symbolAttr = "common".equals(normalizedSymbol) || "cut".equals(normalizedSymbol)
                ? " symbol=\"" + normalizedSymbol + "\"" : "";
        return "<time" + symbolAttr + "><beats>" + Math.max(1, beats) + "</beats><beat-type>"
                + Math.max(1, beatType) + "</beat-type></time>";
    }

    public static boolean isDynamicsTag(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "pppp".equals(normalized) || "ppp".equals(normalized) || "pp".equals(normalized)
                || "p".equals(normalized) || "mp".equals(normalized) || "mf".equals(normalized)
                || "f".equals(normalized) || "ff".equals(normalized) || "fff".equals(normalized)
                || "ffff".equals(normalized) || "fp".equals(normalized) || "sf".equals(normalized)
                || "sfz".equals(normalized) || "sffz".equals(normalized) || "rfz".equals(normalized)
                || "rf".equals(normalized) || "fz".equals(normalized);
    }

    public static String buildMusicXmlDirectionFromMeiDynamValues(String rawText, String placement, String tstamp,
            int divisions, int beatType, String voice, String staffNo) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.length() == 0) {
            return null;
        }
        String normalizedPlacement = placement == null ? "" : placement.trim();
        String placementAttr = normalizedPlacement.length() > 0 ? " placement=\"" + xmlEscape(normalizedPlacement) + "\""
                : "";
        String normalizedDynamics = text.toLowerCase();
        String directionType = isDynamicsTag(normalizedDynamics)
                ? "<dynamics><" + normalizedDynamics + "/></dynamics>"
                : "<words>" + xmlEscape(text) + "</words>";
        Integer offset = parseMeiTstampToTicks(tstamp, divisions, beatType);
        String offsetXml = offset != null && offset.intValue() > 0 ? "<offset>" + offset.intValue() + "</offset>" : "";
        return "<direction" + placementAttr + "><direction-type>" + directionType + "</direction-type>"
                + offsetXml + "<voice>" + xmlEscape(voice) + "</voice><staff>" + xmlEscape(staffNo)
                + "</staff></direction>";
    }

    public static String buildMusicXmlDirectionFromMeiDynam(Element dynam, int divisions, int beatType, String voice,
            String staffNo) {
        if (dynam == null) {
            return null;
        }
        return buildMusicXmlDirectionFromMeiDynamValues(textOf(dynam), meiPlacement(dynam), dynam.getAttribute("tstamp"),
                divisions, beatType, voice, staffNo);
    }

    public static String buildMusicXmlDirectionsFromMeiHairpinValues(String form, String placement, String startId,
            String tstamp, String plist, String endId, String tstamp2, int divisions, int beatType, String voice,
            String staffNo, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        String normalizedForm = form == null ? "" : form.trim().toLowerCase();
        String wedgeType = normalizedForm.indexOf("dim") >= 0 || normalizedForm.indexOf("decresc") >= 0
                ? "diminuendo" : "crescendo";
        String placementAttr = placementAttributeXml(placement);
        Integer startTick = resolveControlEventStartTick(startId, tstamp, idToEventIndex, events, divisions, beatType,
                plist, idToEventTick);
        Integer endTick = resolveControlEventStartTick(endId, tstamp2, idToEventIndex, events, divisions, beatType,
                null, idToEventTick);
        String startOffsetXml = offsetXml(startTick);
        String endOffsetXml = offsetXml(endTick);
        String startDir = "<direction" + placementAttr + "><direction-type><wedge type=\"" + wedgeType
                + "\"/></direction-type>" + startOffsetXml + "<voice>" + xmlEscape(voice) + "</voice><staff>"
                + xmlEscape(staffNo) + "</staff></direction>";
        String stopDir = "<direction" + placementAttr
                + "><direction-type><wedge type=\"stop\"/></direction-type>" + endOffsetXml + "<voice>"
                + xmlEscape(voice) + "</voice><staff>" + xmlEscape(staffNo) + "</staff></direction>";
        return startDir + stopDir;
    }

    public static String buildMusicXmlDirectionsFromMeiHairpin(Element hairpin, int divisions, int beatType,
            String voice, String staffNo, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        if (hairpin == null) {
            return "";
        }
        return buildMusicXmlDirectionsFromMeiHairpinValues(
                firstNonEmpty(hairpin.getAttribute("form"), hairpin.getAttribute("type"), ""),
                meiPlacement(hairpin), hairpin.getAttribute("startid"), hairpin.getAttribute("tstamp"),
                hairpin.getAttribute("plist"), hairpin.getAttribute("endid"), hairpin.getAttribute("tstamp2"),
                divisions, beatType, voice, staffNo, events, idToEventIndex, idToEventTick);
    }

    public static String buildMusicXmlDirectionsFromMeiPedalValues(String semantic, String placement, String startId,
            String tstamp, String plist, String endId, String tstamp2, int divisions, int beatType, String voice,
            String staffNo, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        String placementAttr = placementAttributeXml(placement);
        String normalized = semantic == null ? "" : semantic.trim().toLowerCase();
        boolean explicitStop = normalized.indexOf("stop") >= 0 || normalized.indexOf("end") >= 0
                || normalized.indexOf("off") >= 0 || normalized.indexOf("up") >= 0
                || normalized.indexOf("release") >= 0;
        Integer startTick = resolveControlEventStartTick(startId, tstamp, idToEventIndex, events, divisions, beatType,
                plist, idToEventTick);
        Integer endIndex = resolveControlEventEndpointIndex(endId, tstamp2, idToEventIndex, events, divisions,
                beatType, null, idToEventTick);
        Integer endTick = endIndex == null ? null : resolveEventStartTickByIndex(events, endIndex.intValue());
        String startOffsetXml = offsetXml(startTick);
        String endOffsetXml = offsetXml(endTick);
        if (endIndex != null) {
            return buildMusicXmlPedalDirectionXml("start", placementAttr, startOffsetXml, voice, staffNo)
                    + buildMusicXmlPedalDirectionXml("stop", placementAttr, endOffsetXml, voice, staffNo);
        }
        return buildMusicXmlPedalDirectionXml(explicitStop ? "stop" : "start", placementAttr, startOffsetXml, voice,
                staffNo);
    }

    public static String buildMusicXmlDirectionsFromMeiPedal(Element pedal, int divisions, int beatType, String voice,
            String staffNo, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        if (pedal == null) {
            return "";
        }
        return buildMusicXmlDirectionsFromMeiPedalValues(meiSemantic(pedal), meiPlacement(pedal),
                pedal.getAttribute("startid"), pedal.getAttribute("tstamp"), pedal.getAttribute("plist"),
                pedal.getAttribute("endid"), pedal.getAttribute("tstamp2"), divisions, beatType, voice, staffNo,
                events, idToEventIndex, idToEventTick);
    }

    public static String buildMusicXmlDirectionsFromMeiOctaveValues(String semantic, String dis, String disPlace,
            String placement, String startId, String tstamp, String plist, String endId, String tstamp2, int divisions,
            int beatType, String voice, String staffNo, Collection<ParsedMeiEvent> events,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick) {
        String placementAttr = placementAttributeXml(placement);
        String normalized = semantic == null ? "" : semantic.trim().toLowerCase();
        boolean explicitStop = normalized.indexOf("stop") >= 0 || normalized.indexOf("end") >= 0
                || normalized.indexOf("off") >= 0;
        int size = Math.max(1, parseIntSafe(dis, 8));
        String place = disPlace == null || disPlace.trim().length() == 0 ? placement : disPlace;
        String shiftType = "below".equals((place == null ? "" : place.trim().toLowerCase())) ? "down" : "up";
        Integer startTick = resolveControlEventStartTick(startId, tstamp, idToEventIndex, events, divisions, beatType,
                plist, idToEventTick);
        Integer endIndex = resolveControlEventEndpointIndex(endId, tstamp2, idToEventIndex, events, divisions,
                beatType, null, idToEventTick);
        Integer endTick = endIndex == null ? null : resolveEventStartTickByIndex(events, endIndex.intValue());
        String startOffsetXml = offsetXml(startTick);
        String endOffsetXml = offsetXml(endTick);
        if (endIndex != null) {
            return buildMusicXmlOctaveDirectionXml(shiftType, size, placementAttr, startOffsetXml, voice, staffNo)
                    + buildMusicXmlOctaveDirectionXml("stop", size, placementAttr, endOffsetXml, voice, staffNo);
        }
        return buildMusicXmlOctaveDirectionXml(explicitStop ? "stop" : shiftType, size, placementAttr, startOffsetXml,
                voice, staffNo);
    }

    public static String buildMusicXmlDirectionsFromMeiOctave(Element octave, int divisions, int beatType, String voice,
            String staffNo, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        if (octave == null) {
            return "";
        }
        String dis = firstNonEmpty(octave.getAttribute("dis"), octave.getAttribute("size"), "");
        return buildMusicXmlDirectionsFromMeiOctaveValues(meiSemantic(octave), dis,
                octave.getAttribute("dis.place"), meiPlacement(octave), octave.getAttribute("startid"),
                octave.getAttribute("tstamp"), octave.getAttribute("plist"), octave.getAttribute("endid"),
                octave.getAttribute("tstamp2"), divisions, beatType, voice, staffNo, events, idToEventIndex,
                idToEventTick);
    }

    public static String buildMusicXmlDirectionFromMeiRepeatMarkValues(String text, String placement, String startId,
            String tstamp, String plist, int divisions, int beatType, String voice, String staffNo,
            Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        String raw = text == null ? "" : text.trim();
        if (raw.length() == 0) {
            return null;
        }
        String normalized = raw.toLowerCase();
        String directionType = "<words>" + xmlEscape(raw) + "</words>";
        if (normalized.indexOf("segno") >= 0) {
            directionType = "<segno/>";
        } else if (normalized.indexOf("coda") >= 0) {
            directionType = "<coda/>";
        } else if (normalized.indexOf("fine") >= 0) {
            directionType = "<words>Fine</words>";
        } else if (normalized.indexOf("dacapo") >= 0 || normalized.indexOf("da capo") >= 0
                || normalized.indexOf("d.c.") >= 0) {
            directionType = "<words>D.C.</words>";
        } else if (normalized.indexOf("dalsegno") >= 0 || normalized.indexOf("dal segno") >= 0
                || normalized.indexOf("d.s.") >= 0) {
            directionType = "<words>D.S.</words>";
        }
        String offset = offsetXml(resolveControlEventStartTick(startId, tstamp, idToEventIndex, events, divisions,
                beatType, plist, idToEventTick));
        return "<direction" + placementAttributeXml(placement) + "><direction-type>" + directionType
                + "</direction-type>" + offset + "<voice>" + xmlEscape(voice) + "</voice><staff>"
                + xmlEscape(staffNo) + "</staff></direction>";
    }

    public static String buildMusicXmlDirectionFromMeiRepeatMark(Element repeatMark, int divisions, int beatType,
            String voice, String staffNo, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        if (repeatMark == null) {
            return null;
        }
        String text = firstNonEmpty(repeatMark.getAttribute("func"), repeatMark.getAttribute("type"),
                firstNonEmpty(repeatMark.getAttribute("label"), textOf(repeatMark), ""));
        return buildMusicXmlDirectionFromMeiRepeatMarkValues(text, meiPlacement(repeatMark),
                repeatMark.getAttribute("startid"), repeatMark.getAttribute("tstamp"),
                repeatMark.getAttribute("plist"), divisions, beatType, voice, staffNo, events, idToEventIndex,
                idToEventTick);
    }

    public static String buildMusicXmlDirectionFromMeiTempoValues(String text, String bpmText, String tempoType,
            boolean allowInferFromTextFallback, String placement, String startId, String tstamp, String plist,
            int divisions, int beatType, String voice, String staffNo, Collection<ParsedMeiEvent> events,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick) {
        String type = tempoType == null ? "" : tempoType.trim().toLowerCase();
        boolean inferFromText = type.indexOf("infer-from-text") >= 0;
        double bpm = parsePositiveDouble(bpmText);
        boolean hasBpm = bpm > 0.0d;
        if (inferFromText && !allowInferFromTextFallback) {
            return null;
        }
        if (inferFromText && !hasBpm) {
            return null;
        }
        String effectiveText = inferFromText ? "" : (text == null ? "" : text.trim());
        if (effectiveText.length() == 0 && !hasBpm) {
            return null;
        }
        StringBuilder directionTypes = new StringBuilder();
        if (effectiveText.length() > 0) {
            directionTypes.append("<direction-type><words>").append(xmlEscape(effectiveText))
                    .append("</words></direction-type>");
        }
        if (hasBpm) {
            directionTypes.append("<direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>")
                    .append(Math.round(bpm)).append("</per-minute></metronome></direction-type>");
        }
        String offset = offsetXml(resolveControlEventStartTick(startId, tstamp, idToEventIndex, events, divisions,
                beatType, plist, idToEventTick));
        String soundXml = hasBpm ? "<sound tempo=\"" + formatTempo(bpm) + "\"/>" : "";
        return "<direction" + placementAttributeXml(placement) + ">" + directionTypes.toString() + offset
                + "<voice>" + xmlEscape(voice) + "</voice><staff>" + xmlEscape(staffNo) + "</staff>"
                + soundXml + "</direction>";
    }

    public static String buildMusicXmlDirectionFromMeiTempo(Element tempo, int divisions, int beatType, String voice,
            String staffNo, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick, boolean allowInferFromTextFallback) {
        if (tempo == null) {
            return null;
        }
        return buildMusicXmlDirectionFromMeiTempoValues(textOf(tempo),
                firstNonEmpty(tempo.getAttribute("midi.bpm"), tempo.getAttribute("bpm"), ""),
                tempo.getAttribute("type"), allowInferFromTextFallback, meiPlacement(tempo),
                tempo.getAttribute("startid"), tempo.getAttribute("tstamp"), tempo.getAttribute("plist"), divisions,
                beatType, voice, staffNo, events, idToEventIndex, idToEventTick);
    }

    public static String buildMusicXmlHarmonyFromMeiHarmValues(String rawText, String fallbackType, String fallbackLabel,
            String startId, String tstamp, String plist, int divisions, int beatType, String staffNo,
            Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        String raw = firstNonEmpty(rawText, fallbackType, fallbackLabel);
        if (raw.length() == 0) {
            return null;
        }
        ParsedMeiHarmonyText parsed = parseMeiHarmText(raw);
        if (parsed == null) {
            return "<harmony><kind text=\"" + xmlEscape(raw) + "\">other</kind><staff>" + xmlEscape(staffNo)
                    + "</staff></harmony>";
        }
        String rootAlterXml = parsed.getRootAlter() == 0 ? "" : "<root-alter>" + parsed.getRootAlter()
                + "</root-alter>";
        String rootXml = "<root><root-step>" + xmlEscape(parsed.getRootStep()) + "</root-step>" + rootAlterXml
                + "</root>";
        String kindXml = parsed.getKindText().length() > 0
                ? "<kind text=\"" + xmlEscape(parsed.getKindText()) + "\">" + xmlEscape(parsed.getKind()) + "</kind>"
                : "<kind>" + xmlEscape(parsed.getKind()) + "</kind>";
        String bassXml = "";
        if (parsed.getBassStep() != null) {
            String bassAlterXml = parsed.getBassAlter() == null ? ""
                    : "<bass-alter>" + parsed.getBassAlter().intValue() + "</bass-alter>";
            bassXml = "<bass><bass-step>" + xmlEscape(parsed.getBassStep()) + "</bass-step>" + bassAlterXml
                    + "</bass>";
        }
        StringBuilder degreeXml = new StringBuilder();
        for (HarmonyDegree degree : parsed.getDegrees()) {
            degreeXml.append("<degree><degree-value>").append(degree.getValue()).append("</degree-value>")
                    .append("<degree-alter>").append(degree.getAlter()).append("</degree-alter>")
                    .append("<degree-type>add</degree-type></degree>");
        }
        String offset = offsetXml(resolveControlEventStartTick(startId, tstamp, idToEventIndex, events, divisions,
                beatType, plist, idToEventTick));
        return "<harmony>" + rootXml + kindXml + bassXml + degreeXml.toString() + offset + "<staff>"
                + xmlEscape(staffNo) + "</staff></harmony>";
    }

    public static String buildMusicXmlHarmonyFromMeiHarm(Element harm, int divisions, int beatType, String staffNo,
            Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        if (harm == null) {
            return null;
        }
        return buildMusicXmlHarmonyFromMeiHarmValues(textOf(harm), harm.getAttribute("type"),
                harm.getAttribute("label"), harm.getAttribute("startid"), harm.getAttribute("tstamp"),
                harm.getAttribute("plist"), divisions, beatType, staffNo, events, idToEventIndex, idToEventTick);
    }

    public static String collectLayerHarmonyXml(Element staff, Element layer, int divisions, int beatType,
            Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        String staffNo = staff == null || staff.getAttribute("n").trim().length() == 0 ? "1" : staff.getAttribute("n")
                .trim();
        StringBuilder out = new StringBuilder();
        List<Element> harms = new ArrayList<Element>();
        harms.addAll(directChildElementsByLocalName(layer, "harm"));
        harms.addAll(directChildElementsByLocalName(staff, "harm"));
        for (Element harm : harms) {
            String xml = buildMusicXmlHarmonyFromMeiHarm(harm, divisions, beatType, staffNo, events, idToEventIndex,
                    idToEventTick);
            if (xml != null && xml.length() > 0) {
                out.append(xml);
            }
        }
        return out.toString();
    }

    public static List<Element> collectControlEventsForLayer(String name, Element staff, Element layer, String staffNo,
            String layerNo, String primaryLayerNo) {
        List<Element> controls = new ArrayList<Element>();
        controls.addAll(directChildElementsByLocalName(layer, name));
        controls.addAll(directChildElementsByLocalName(staff, name));
        Element measure = parentElementByLocalName(staff, "measure");
        controls.addAll(directChildElementsByLocalName(measure, name));
        List<Element> out = new ArrayList<Element>();
        for (Element control : controls) {
            String parentName = control == null ? "" : localNameOf(control.getParentNode());
            if (controlEventAppliesToLayerValues(control == null ? "" : control.getAttribute("staff"),
                    control == null ? "" : control.getAttribute("layer"), parentName, staffNo, layerNo,
                    primaryLayerNo)) {
                out.add(control);
            }
        }
        return out;
    }

    public static String collectLayerDirectionXml(Element staff, Element layer, int divisions, int beatType,
            String voice, Collection<ParsedMeiEvent> events, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick) {
        String staffNo = staff == null || staff.getAttribute("n").trim().length() == 0 ? "1" : staff.getAttribute("n")
                .trim();
        String layerNo = layer == null || layer.getAttribute("n").trim().length() == 0 ? "1" : layer.getAttribute("n")
                .trim();
        List<Element> staffLayers = directChildElementsByLocalName(staff, "layer");
        String primaryLayerNo = staffLayers.isEmpty() || staffLayers.get(0).getAttribute("n").trim().length() == 0
                ? "1"
                : staffLayers.get(0).getAttribute("n").trim();
        List<Element> dyns = collectControlEventsForLayer("dynam", staff, layer, staffNo, layerNo, primaryLayerNo);
        List<Element> tempos = collectControlEventsForLayer("tempo", staff, layer, staffNo, layerNo, primaryLayerNo);
        List<Element> hairpins = collectControlEventsForLayer("hairpin", staff, layer, staffNo, layerNo,
                primaryLayerNo);
        List<Element> pedals = collectControlEventsForLayer("pedal", staff, layer, staffNo, layerNo, primaryLayerNo);
        List<Element> octaves = collectControlEventsForLayer("octave", staff, layer, staffNo, layerNo, primaryLayerNo);
        List<Element> repeatMarks = collectControlEventsForLayer("repeatMark", staff, layer, staffNo, layerNo,
                primaryLayerNo);
        boolean hasVisibleTempo = false;
        for (Element tempo : tempos) {
            String type = tempo == null ? "" : tempo.getAttribute("type").trim().toLowerCase();
            if (type.indexOf("infer-from-text") < 0) {
                hasVisibleTempo = true;
                break;
            }
        }
        StringBuilder out = new StringBuilder();
        for (Element tempo : tempos) {
            String type = tempo == null ? "" : tempo.getAttribute("type").trim().toLowerCase();
            boolean allowInfer = type.indexOf("infer-from-text") >= 0 && !hasVisibleTempo;
            appendIfPresent(out, buildMusicXmlDirectionFromMeiTempo(tempo, divisions, beatType, voice, staffNo, events,
                    idToEventIndex, idToEventTick, allowInfer));
        }
        for (Element dynam : dyns) {
            appendIfPresent(out, buildMusicXmlDirectionFromMeiDynam(dynam, divisions, beatType, voice, staffNo));
        }
        for (Element hairpin : hairpins) {
            appendIfPresent(out, buildMusicXmlDirectionsFromMeiHairpin(hairpin, divisions, beatType, voice, staffNo,
                    events, idToEventIndex, idToEventTick));
        }
        for (Element pedal : pedals) {
            appendIfPresent(out, buildMusicXmlDirectionsFromMeiPedal(pedal, divisions, beatType, voice, staffNo, events,
                    idToEventIndex, idToEventTick));
        }
        for (Element octave : octaves) {
            appendIfPresent(out, buildMusicXmlDirectionsFromMeiOctave(octave, divisions, beatType, voice, staffNo,
                    events, idToEventIndex, idToEventTick));
        }
        for (Element repeatMark : repeatMarks) {
            appendIfPresent(out, buildMusicXmlDirectionFromMeiRepeatMark(repeatMark, divisions, beatType, voice,
                    staffNo, events, idToEventIndex, idToEventTick));
        }
        return out.toString();
    }

    public static List<String> collectMeiHarmsForStaff(Collection<MeiHarmonySource> harmonies, int localStaff,
            int sourceDivisions, int beatType) {
        if (harmonies == null || harmonies.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        for (MeiHarmonySource harmony : harmonies) {
            if (harmony == null || harmony.getStaffNo() != localStaff) {
                continue;
            }
            String xml = buildMeiHarmFromMusicXmlHarmonyValues(harmony.getRootStep(), harmony.getRootAlter(),
                    harmony.getKindText(), harmony.getKindTextAttribute(), harmony.getBassStep(),
                    harmony.getBassAlter(), harmony.getDegrees(), harmony.getOffsetTicks(), sourceDivisions, beatType);
            if (xml != null) {
                out.add(xml);
            }
        }
        return out;
    }

    public static String meiDurToMusicXmlType(String dur) {
        String normalized = dur == null ? "" : dur.trim().toLowerCase();
        if ("maxima".equals(normalized)) {
            return "maxima";
        }
        if ("long".equals(normalized)) {
            return "long";
        }
        if ("breve".equals(normalized)) {
            return "breve";
        }
        if ("1".equals(normalized)) {
            return "whole";
        }
        if ("2".equals(normalized)) {
            return "half";
        }
        if ("4".equals(normalized)) {
            return "quarter";
        }
        if ("8".equals(normalized)) {
            return "eighth";
        }
        if ("16".equals(normalized)) {
            return "16th";
        }
        if ("32".equals(normalized)) {
            return "32nd";
        }
        if ("64".equals(normalized)) {
            return "64th";
        }
        if ("128".equals(normalized)) {
            return "128th";
        }
        return "quarter";
    }

    public static double meiDurToQuarterLength(String dur) {
        String normalized = dur == null ? "" : dur.trim().toLowerCase();
        if ("maxima".equals(normalized)) {
            return 32.0d;
        }
        if ("long".equals(normalized)) {
            return 16.0d;
        }
        if ("breve".equals(normalized)) {
            return 8.0d;
        }
        int denominator;
        try {
            denominator = Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return 1.0d;
        }
        if (denominator <= 0) {
            return 1.0d;
        }
        return 4.0d / denominator;
    }

    public static int meiDurToBeamDepth(String dur) {
        int denominator;
        try {
            denominator = Integer.parseInt(dur == null ? "" : dur.trim().toLowerCase());
        } catch (NumberFormatException ex) {
            return 0;
        }
        if (denominator < 8) {
            return 0;
        }
        int depth = 0;
        int value = denominator;
        while (value >= 8) {
            depth++;
            value /= 2;
            if (value <= 0) {
                break;
            }
        }
        return Math.max(0, depth);
    }

    public static double dotsMultiplier(int dots) {
        int safeDots = Math.max(0, Math.min(4, dots));
        double sum = 1.0d;
        double add = 0.5d;
        for (int index = 0; index < safeDots; index++) {
            sum += add;
            add /= 2.0d;
        }
        return sum;
    }

    public static MeiDurDots inferMeiDurAndDotsFromTicks(int ticks, int divisions) {
        int safeTicks = Math.max(1, Math.round(ticks));
        int safeDivisions = Math.max(1, Math.round(divisions));
        String[] candidates = { "1", "2", "4", "8", "16", "32", "64", "128" };
        String bestDur = "4";
        int bestDots = 0;
        int bestDiff = Integer.MAX_VALUE;
        for (String dur : candidates) {
            double base = meiDurToQuarterLength(dur) * safeDivisions;
            for (int dots = 0; dots <= 3; dots++) {
                int candidate = Math.max(1, Math.round((float) (base * dotsMultiplier(dots))));
                int diff = Math.abs(candidate - safeTicks);
                if (diff < bestDiff) {
                    bestDur = dur;
                    bestDots = dots;
                    bestDiff = diff;
                }
                if (diff == 0) {
                    return new MeiDurDots(dur, dots);
                }
            }
        }
        return new MeiDurDots(bestDur, bestDots);
    }

    public static Integer accidToAlter(String accid) {
        String normalized = accid == null ? "" : accid.trim().toLowerCase();
        if (normalized.length() == 0) {
            return null;
        }
        if ("s".equals(normalized) || "#".equals(normalized)) {
            return Integer.valueOf(1);
        }
        if ("ss".equals(normalized) || "x".equals(normalized)) {
            return Integer.valueOf(2);
        }
        if ("f".equals(normalized) || "b".equals(normalized)) {
            return Integer.valueOf(-1);
        }
        if ("ff".equals(normalized) || "bb".equals(normalized)) {
            return Integer.valueOf(-2);
        }
        if ("n".equals(normalized)) {
            return Integer.valueOf(0);
        }
        return null;
    }

    public static String accidToMusicXmlAccidental(String accid) {
        String normalized = accid == null ? "" : accid.trim().toLowerCase();
        if (normalized.length() == 0) {
            return null;
        }
        if ("s".equals(normalized) || "#".equals(normalized)) {
            return "sharp";
        }
        if ("f".equals(normalized) || "b".equals(normalized)) {
            return "flat";
        }
        if ("n".equals(normalized)) {
            return "natural";
        }
        if ("ss".equals(normalized) || "x".equals(normalized)) {
            return "double-sharp";
        }
        if ("ff".equals(normalized) || "bb".equals(normalized)) {
            return "flat-flat";
        }
        return null;
    }

    public static String accidToPitchAlterXml(String accid) {
        Integer alter = accidToAlter(accid);
        if (alter == null || alter.intValue() == 0) {
            return "";
        }
        return "<alter>" + alter.intValue() + "</alter>";
    }

    public static int impliedAlterFromFifths(String step, int fifths) {
        String normalizedStep = step == null ? "" : step.trim().toUpperCase();
        if (!isPitchStep(normalizedStep)) {
            return 0;
        }
        int n = Math.max(-7, Math.min(7, Math.round(fifths)));
        if (n == 0) {
            return 0;
        }
        String[] sharpOrder = { "F", "C", "G", "D", "A", "E", "B" };
        String[] flatOrder = { "B", "E", "A", "D", "G", "C", "F" };
        if (n > 0) {
            for (int index = 0; index < n; index++) {
                if (sharpOrder[index].equals(normalizedStep)) {
                    return 1;
                }
            }
            return 0;
        }
        for (int index = 0; index < Math.abs(n); index++) {
            if (flatOrder[index].equals(normalizedStep)) {
                return -1;
            }
        }
        return 0;
    }

    public static int parseMeiKeySigToFifths(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.length() == 0 || "0".equals(normalized)) {
            return 0;
        }
        int number;
        try {
            number = Integer.parseInt(normalized.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
        if (normalized.endsWith("s")) {
            return Math.max(-7, Math.min(7, number));
        }
        if (normalized.endsWith("f")) {
            return Math.max(-7, Math.min(7, -Math.abs(number)));
        }
        return Math.max(-7, Math.min(7, number));
    }

    public static String readMeiKeySigAttr(Element element) {
        if (element == null) {
            return "";
        }
        String keySig = element.getAttribute("key.sig") == null ? "" : element.getAttribute("key.sig").trim();
        if (keySig.length() > 0) {
            return keySig;
        }
        return element.getAttribute("keysig") == null ? "" : element.getAttribute("keysig").trim();
    }

    public static int parseMeiKeyAccidToAlter(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.length() == 0 || "n".equals(normalized)) {
            return 0;
        }
        if ("s".equals(normalized) || "#".equals(normalized)) {
            return 1;
        }
        if ("ss".equals(normalized) || "x".equals(normalized) || "##".equals(normalized)) {
            return 2;
        }
        if ("f".equals(normalized) || "b".equals(normalized) || "♭".equals(normalized)) {
            return -1;
        }
        if ("ff".equals(normalized) || "bb".equals(normalized)) {
            return -2;
        }
        return 0;
    }

    public static Integer tonicToFifths(String pname, String accid, String mode) {
        String step = pname == null ? "" : pname.trim().toUpperCase();
        if (!isPitchStep(step)) {
            return null;
        }
        int alter = parseMeiKeyAccidToAlter(accid);
        String tonic = step + repeat(alter > 0 ? "#" : "b", Math.abs(alter));
        String normalizedMode = mode == null ? "" : mode.trim().toLowerCase();
        String[] majorKeys = { "C", "G", "D", "A", "E", "B", "F#", "C#", "F", "Bb", "Eb", "Ab", "Db", "Gb",
                "Cb" };
        int[] majorValues = { 0, 1, 2, 3, 4, 5, 6, 7, -1, -2, -3, -4, -5, -6, -7 };
        String[] minorKeys = { "A", "E", "B", "F#", "C#", "G#", "D#", "A#", "D", "G", "C", "F", "Bb", "Eb",
                "Ab" };
        int[] minorValues = { 0, 1, 2, 3, 4, 5, 6, 7, -1, -2, -3, -4, -5, -6, -7 };
        String[] keys = "minor".equals(normalizedMode) ? minorKeys : majorKeys;
        int[] values = "minor".equals(normalizedMode) ? minorValues : majorValues;
        for (int index = 0; index < keys.length; index++) {
            if (keys[index].equals(tonic)) {
                return Integer.valueOf(values[index]);
            }
        }
        return null;
    }

    public static Integer parseMeiKeyFifthsFromValues(String keySig, String keyPname, String keyAccid, String keyMode) {
        String sig = keySig == null ? "" : keySig.trim();
        if (sig.length() > 0) {
            return Integer.valueOf(parseMeiKeySigToFifths(sig));
        }
        return tonicToFifths(keyPname, keyAccid, keyMode == null || keyMode.trim().length() == 0 ? "major" : keyMode);
    }

    public static Integer parseMeiKeyFifthsFromElement(Element element) {
        if (element == null) {
            return null;
        }
        String keySig = readMeiKeySigAttr(element);
        if (keySig.length() > 0) {
            return Integer.valueOf(parseMeiKeySigToFifths(keySig));
        }
        return tonicToFifths(element.getAttribute("key.pname"), element.getAttribute("key.accid"),
                element.getAttribute("key.mode") == null || element.getAttribute("key.mode").trim().length() == 0
                        ? "major"
                        : element.getAttribute("key.mode"));
    }

    public static String toHex(int value) {
        return toHex(value, 2);
    }

    public static String toHex(int value, int width) {
        int safe = Math.max(0, Math.round(value));
        String hex = Integer.toHexString(safe).toUpperCase();
        int safeWidth = Math.max(0, width);
        while (hex.length() < safeWidth) {
            hex = "0" + hex;
        }
        return "0x" + hex;
    }

    public static int resolveDurTicksFromMetadata(Integer dur480, Integer legacyTicks, Integer legacyDivisions,
            int fallbackTicks, int targetDivisions) {
        if (dur480 != null && dur480.intValue() > 0) {
            return Math.round(dur480.intValue());
        }
        if (legacyTicks == null || legacyTicks.intValue() <= 0) {
            return fallbackTicks;
        }
        if (legacyDivisions != null && legacyDivisions.intValue() > 0) {
            return Math.max(1,
                    Math.round((legacyTicks.intValue() * (float) targetDivisions) / legacyDivisions.intValue()));
        }
        return fallbackTicks;
    }

    public static int resolveDurTicksFromMetadata(Element source, int fallbackTicks, int targetDivisions) {
        if (source == null) {
            return fallbackTicks;
        }
        int dur480 = parseIntSafe(source.getAttribute("mks-dur-480"), Integer.MIN_VALUE);
        Integer dur480Value = dur480 == Integer.MIN_VALUE ? null : Integer.valueOf(dur480);
        int legacyTicks = parseIntSafe(source.getAttribute("mks-dur-ticks"), Integer.MIN_VALUE);
        Integer legacyTicksValue = legacyTicks == Integer.MIN_VALUE ? null : Integer.valueOf(legacyTicks);
        int legacyDivisions = parseIntSafe(source.getAttribute("mks-dur-div"), Integer.MIN_VALUE);
        Integer legacyDivisionsValue = legacyDivisions == Integer.MIN_VALUE ? null : Integer.valueOf(legacyDivisions);
        return resolveDurTicksFromMetadata(dur480Value, legacyTicksValue, legacyDivisionsValue, fallbackTicks,
                targetDivisions);
    }

    public static TieFlags parseMeiTieFlags(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        if (normalized.length() == 0) {
            return new TieFlags(false, false);
        }
        boolean hasMiddle = normalized.indexOf('m') >= 0;
        boolean start = hasMiddle || normalized.indexOf('i') >= 0;
        boolean stop = hasMiddle || normalized.indexOf('t') >= 0;
        return new TieFlags(start, stop);
    }

    public static List<MeiSlurNotation> parseMeiSlurNotations(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        if (normalized.length() == 0) {
            return Collections.emptyList();
        }
        List<MeiSlurNotation> out = new ArrayList<MeiSlurNotation>();
        Matcher matcher = Pattern.compile("([imt])\\s*(\\d+)?|(\\d+)\\s*([imt])").matcher(normalized);
        while (matcher.find()) {
            String kind = matcher.group(1) != null ? matcher.group(1) : (matcher.group(4) == null ? "" : matcher.group(4));
            String numberRaw = matcher.group(2) != null ? matcher.group(2)
                    : (matcher.group(3) == null ? "1" : matcher.group(3));
            int number = Math.max(1, parseIntSafe(numberRaw, 1));
            if ("i".equals(kind) || "m".equals(kind)) {
                out.add(new MeiSlurNotation("start", number));
            }
            if ("t".equals(kind) || "m".equals(kind)) {
                out.add(new MeiSlurNotation("stop", number));
            }
        }
        return out;
    }

    public static String addSlurNotationToSingleNoteXml(String noteXml, String type, int number) {
        String slurXml = "<slur type=\"" + xmlEscape(type) + "\" number=\"" + Math.max(1, number) + "\"/>";
        return addNotationXmlToSingleNoteXml(noteXml, slurXml);
    }

    public static String addTieToSingleNoteXml(String noteXml, String type) {
        String safeType = xmlEscape(type);
        String tieXml = "<tie type=\"" + safeType + "\"/>";
        String tiedXml = "<tied type=\"" + safeType + "\"/>";
        String source = noteXml == null ? "" : noteXml;
        String withTie = source.contains("<duration>") ? source.replace("<duration>", tieXml + "<duration>")
                : source.replace("</note>", tieXml + "</note>");
        return addNotationXmlToSingleNoteXml(withTie, tiedXml);
    }

    public static String addNotationXmlToSingleNoteXml(String noteXml, String notationXml) {
        String source = noteXml == null ? "" : noteXml;
        String notation = notationXml == null ? "" : notationXml;
        if (source.contains("<notations>")) {
            return source.replace("</notations>", notation + "</notations>");
        }
        return source.replace("</note>", "<notations>" + notation + "</notations></note>");
    }

    public static String addOrnamentXmlToSingleNoteXml(String noteXml, String ornamentXml) {
        String source = noteXml == null ? "" : noteXml;
        String ornament = ornamentXml == null ? "" : ornamentXml;
        if (source.contains("<ornaments>")) {
            return source.replace("</ornaments>", ornament + "</ornaments>");
        }
        return addNotationXmlToSingleNoteXml(source, "<ornaments>" + ornament + "</ornaments>");
    }

    public static String addArticulationXmlToSingleNoteXml(String noteXml, String articulationXml) {
        String source = noteXml == null ? "" : noteXml;
        String articulation = articulationXml == null ? "" : articulationXml;
        if (source.contains("<articulations>")) {
            return source.replace("</articulations>", articulation + "</articulations>");
        }
        return addNotationXmlToSingleNoteXml(source, "<articulations>" + articulation + "</articulations>");
    }

    public static String addSlurNotationToEventXml(String eventXml, String type, int number) {
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addSlurNotationToSingleNoteXml(noteXml, type, number);
            }
        });
    }

    public static String addTieNotationToEventXml(String eventXml, String type) {
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addTieToSingleNoteXml(noteXml, type);
            }
        });
    }

    public static String addTrillNotationToEventXml(String eventXml) {
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addOrnamentXmlToSingleNoteXml(noteXml, "<trill-mark/>");
            }
        });
    }

    public static String addFermataNotationToEventXml(String eventXml, boolean isBelow) {
        final String typeAttr = isBelow ? " type=\"inverted\"" : "";
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addNotationXmlToSingleNoteXml(noteXml, "<fermata" + typeAttr + "/>");
            }
        });
    }

    public static String addGlissNotationToEventXml(String eventXml, String type, int number) {
        return addSimpleTypedNumberNotationToEventXml(eventXml, "glissando", type, number);
    }

    public static String addSlideNotationToEventXml(String eventXml, String type, int number) {
        return addSimpleTypedNumberNotationToEventXml(eventXml, "slide", type, number);
    }

    public static String addTurnNotationToEventXml(String eventXml, boolean isInverted) {
        final String ornament = isInverted ? "<inverted-turn/>" : "<turn/>";
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addOrnamentXmlToSingleNoteXml(noteXml, ornament);
            }
        });
    }

    public static String addMordentNotationToEventXml(String eventXml, boolean isInverted) {
        final String ornament = isInverted ? "<inverted-mordent/>" : "<mordent/>";
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addOrnamentXmlToSingleNoteXml(noteXml, ornament);
            }
        });
    }

    public static String addBreathNotationToEventXml(String eventXml) {
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addArticulationXmlToSingleNoteXml(noteXml, "<breath-mark/>");
            }
        });
    }

    public static String addCaesuraNotationToEventXml(String eventXml) {
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addArticulationXmlToSingleNoteXml(noteXml, "<caesura/>");
            }
        });
    }

    public static String addTupletNotationToEventXml(String eventXml, String type, int number) {
        return addSimpleTypedNumberNotationToEventXml(eventXml, "tuplet", type, number);
    }

    public static String addBeamToSingleNoteXml(String noteXml, String value, int number) {
        String source = noteXml == null ? "" : noteXml;
        int safeNumber = Math.max(1, number);
        String marker = "<beam number=\"" + safeNumber + "\">";
        if (source.contains(marker)) {
            return source;
        }
        return source.replace("</note>", marker + xmlEscape(value) + "</beam></note>");
    }

    public static String addBeamToEventXml(String eventXml, String value, int number) {
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                return addBeamToSingleNoteXml(noteXml, value, number);
            }
        });
    }

    public static Integer parseMeiTstampToTicks(String tstamp, int divisions, int beatType) {
        String raw = tstamp == null ? "" : tstamp.trim();
        if (!raw.matches("\\d+(\\.\\d+)?")) {
            return null;
        }
        double beatPos;
        try {
            beatPos = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (!Double.isFinite(beatPos) || beatPos < 1.0d) {
            return null;
        }
        double ticksPerBeat = Math.max(1.0d, (4.0d * divisions) / Math.max(1, beatType));
        return Integer.valueOf(Math.max(0, Math.round((float) ((beatPos - 1.0d) * ticksPerBeat))));
    }

    public static Integer resolveEventIndexByTstamp(Collection<ParsedMeiEvent> events, int targetTick) {
        if (events == null || targetTick < 0) {
            return null;
        }
        int cursor = 0;
        Integer lastPitchedIndex = null;
        int index = 0;
        for (ParsedMeiEvent event : events) {
            if (event != null && !"rest".equals(event.getKind())) {
                lastPitchedIndex = Integer.valueOf(index);
                if (cursor >= targetTick) {
                    return Integer.valueOf(index);
                }
            }
            cursor += Math.max(0, event == null ? 0 : event.getDurationTicks());
            index++;
        }
        return lastPitchedIndex;
    }

    public static Integer resolveEventStartTickByIndex(Collection<ParsedMeiEvent> events, int eventIndex) {
        if (events == null || eventIndex < 0 || eventIndex >= events.size()) {
            return null;
        }
        int cursor = 0;
        int index = 0;
        for (ParsedMeiEvent event : events) {
            if (index == eventIndex) {
                return Integer.valueOf(cursor);
            }
            cursor += Math.max(0, event == null ? 0 : event.getDurationTicks());
            index++;
        }
        return null;
    }

    public static Integer resolveControlEventEndpointIndex(String rawId, String tstamp, Map<String, Integer> idToEventIndex,
            Collection<ParsedMeiEvent> events, int divisions, int beatType, String rawPlist,
            Map<String, Integer> idToEventTick) {
        List<ParsedMeiEvent> eventList = events == null ? Collections.<ParsedMeiEvent>emptyList()
                : new ArrayList<ParsedMeiEvent>(events);
        Map<String, Integer> indexMap = idToEventIndex == null ? Collections.<String, Integer>emptyMap()
                : idToEventIndex;
        Map<String, Integer> tickMap = idToEventTick == null ? Collections.<String, Integer>emptyMap() : idToEventTick;
        String id = rawId != null && rawId.startsWith("#") ? rawId.substring(1) : "";
        Integer byId = resolveEndpointById(id, indexMap, tickMap, eventList);
        if (byId != null) {
            return byId;
        }
        String plist = rawPlist == null ? "" : rawPlist.trim();
        if (plist.length() > 0) {
            String[] candidates = plist.split("\\s+");
            for (String candidate : candidates) {
                String normalized = candidate.startsWith("#") ? candidate.substring(1) : candidate;
                Integer byCandidate = resolveEndpointById(normalized, indexMap, tickMap, eventList);
                if (byCandidate != null) {
                    return byCandidate;
                }
            }
        }
        Integer ticks = parseMeiTstampToTicks(tstamp, divisions, beatType);
        return ticks == null ? null : resolveEventIndexByTstamp(eventList, ticks.intValue());
    }

    public static List<String> parseMeiTargetList(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        for (String token : value.split("[\\s,]+")) {
            String normalized = token == null ? "" : token.trim();
            if (normalized.length() > 0) {
                out.add(normalized);
            }
        }
        return out;
    }

    public static boolean controlEventAppliesToLayerValues(String staffTargetsRaw, String layerTargetsRaw,
            String parentName, String staffNo, String layerNo, String primaryLayerNo) {
        List<String> staffTargets = parseMeiTargetList(staffTargetsRaw);
        if (!staffTargets.isEmpty() && !staffTargets.contains(staffNo)) {
            return false;
        }
        List<String> layerTargets = parseMeiTargetList(layerTargetsRaw);
        if (!layerTargets.isEmpty()) {
            return layerTargets.contains(layerNo);
        }
        String parent = parentName == null ? "" : parentName.trim();
        if ("staff".equals(parent) || "measure".equals(parent)) {
            return (primaryLayerNo == null ? "" : primaryLayerNo).equals(layerNo);
        }
        return true;
    }

    public static List<ParsedMeiXmlEvent> applyMeiSlurControlEvent(Collection<ParsedMeiXmlEvent> events, String startId,
            String tstamp, String plist, String endId, String tstamp2, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick, int divisions, int beatType, int slurNumber) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        Integer startIndex = resolveXmlEventEndpointIndex(startId, tstamp, idToEventIndex, out, divisions, beatType,
                plist, idToEventTick);
        Integer endIndex = resolveXmlEventEndpointIndex(endId, tstamp2, idToEventIndex, out, divisions, beatType, null,
                idToEventTick);
        if (!isValidXmlEventIndex(out, startIndex) || !isValidXmlEventIndex(out, endIndex)) {
            return out;
        }
        ParsedMeiXmlEvent startEvent = out.get(startIndex.intValue());
        ParsedMeiXmlEvent endEvent = out.get(endIndex.intValue());
        if (!"rest".equals(startEvent.getKind())) {
            out.set(startIndex.intValue(), startEvent.withXml(addSlurNotationToEventXml(startEvent.getXml(), "start",
                    slurNumber)));
        }
        if (!"rest".equals(endEvent.getKind())) {
            out.set(endIndex.intValue(), endEvent.withXml(addSlurNotationToEventXml(endEvent.getXml(), "stop",
                    slurNumber)));
        }
        return out;
    }

    public static List<ParsedMeiXmlEvent> applyMeiTieControlEvent(Collection<ParsedMeiXmlEvent> events, String startId,
            String tstamp, String plist, String endId, String tstamp2, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick, int divisions, int beatType) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        Integer startIndex = resolveXmlEventEndpointIndex(startId, tstamp, idToEventIndex, out, divisions, beatType,
                plist, idToEventTick);
        Integer endIndex = resolveXmlEventEndpointIndex(endId, tstamp2, idToEventIndex, out, divisions, beatType, null,
                idToEventTick);
        if (!isValidXmlEventIndex(out, startIndex) || !isValidXmlEventIndex(out, endIndex)) {
            return out;
        }
        ParsedMeiXmlEvent startEvent = out.get(startIndex.intValue());
        ParsedMeiXmlEvent endEvent = out.get(endIndex.intValue());
        if (!"rest".equals(startEvent.getKind())) {
            out.set(startIndex.intValue(), startEvent.withXml(addTieNotationToEventXml(startEvent.getXml(), "start")));
        }
        if (!"rest".equals(endEvent.getKind())) {
            out.set(endIndex.intValue(), endEvent.withXml(addTieNotationToEventXml(endEvent.getXml(), "stop")));
        }
        return out;
    }

    public static List<ParsedMeiXmlEvent> applyMeiSingleNotationControlEvent(Collection<ParsedMeiXmlEvent> events,
            String controlName, boolean inverted, String startId, String tstamp, String plist,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick, int divisions, int beatType) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        Integer startIndex = resolveXmlEventEndpointIndex(startId, tstamp, idToEventIndex, out, divisions, beatType,
                plist, idToEventTick);
        if (!isValidXmlEventIndex(out, startIndex)) {
            return out;
        }
        ParsedMeiXmlEvent startEvent = out.get(startIndex.intValue());
        if ("rest".equals(startEvent.getKind())) {
            return out;
        }
        String normalized = controlName == null ? "" : controlName.trim().toLowerCase();
        String nextXml = startEvent.getXml();
        if ("trill".equals(normalized)) {
            nextXml = addTrillNotationToEventXml(nextXml);
        } else if ("fermata".equals(normalized)) {
            nextXml = addFermataNotationToEventXml(nextXml, inverted);
        } else if ("turn".equals(normalized)) {
            nextXml = addTurnNotationToEventXml(nextXml, inverted);
        } else if ("mordent".equals(normalized)) {
            nextXml = addMordentNotationToEventXml(nextXml, inverted);
        } else if ("breath".equals(normalized)) {
            nextXml = addBreathNotationToEventXml(nextXml);
        } else if ("caesura".equals(normalized)) {
            nextXml = addCaesuraNotationToEventXml(nextXml);
        }
        out.set(startIndex.intValue(), startEvent.withXml(nextXml));
        return out;
    }

    public static List<ParsedMeiXmlEvent> applyStaffSlurControlEvents(Element staff, Element layer,
            Collection<ParsedMeiXmlEvent> layerEvents, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick, int divisions, int beatType) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(layerEvents);
        if (out.isEmpty()) {
            return out;
        }
        int slurNumber = 1;
        for (Element slur : collectStaffLayerMeasureControls("slur", staff, layer, true)) {
            out = applyMeiSlurControlEvent(out, slur.getAttribute("startid"), slur.getAttribute("tstamp"),
                    slur.getAttribute("plist"), slur.getAttribute("endid"), slur.getAttribute("tstamp2"),
                    idToEventIndex, idToEventTick, divisions, beatType, slurNumber);
            slurNumber++;
        }
        for (Element tie : collectStaffLayerMeasureControls("tie", staff, layer, true)) {
            out = applyMeiTieControlEvent(out, tie.getAttribute("startid"), tie.getAttribute("tstamp"),
                    tie.getAttribute("plist"), tie.getAttribute("endid"), tie.getAttribute("tstamp2"),
                    idToEventIndex, idToEventTick, divisions, beatType);
        }
        for (Element trill : collectStaffLayerMeasureControls("trill", staff, layer, true)) {
            out = applyMeiSingleNotationControlEvent(out, "trill", false, trill.getAttribute("startid"),
                    trill.getAttribute("tstamp"), trill.getAttribute("plist"), idToEventIndex, idToEventTick,
                    divisions, beatType);
        }
        out = applyStaffSingleNotationControls(out, "fermata", collectStaffLayerMeasureControls("fermata", staff,
                layer, false), idToEventIndex, idToEventTick, divisions, beatType);
        out = applyStaffSingleNotationControls(out, "turn", collectStaffLayerMeasureControls("turn", staff, layer,
                false), idToEventIndex, idToEventTick, divisions, beatType);
        out = applyStaffSingleNotationControls(out, "mordent", collectStaffLayerMeasureControls("mordent", staff,
                layer, false), idToEventIndex, idToEventTick, divisions, beatType);
        out = applyStaffSingleNotationControls(out, "breath", collectStaffLayerMeasureControls("breath", staff, layer,
                false), idToEventIndex, idToEventTick, divisions, beatType);
        return applyStaffSingleNotationControls(out, "caesura", collectStaffLayerMeasureControls("caesura", staff,
                layer, false), idToEventIndex, idToEventTick, divisions, beatType);
    }

    public static List<ParsedMeiXmlEvent> applyMeiBeamSpanControlEvent(Collection<ParsedMeiXmlEvent> events,
            String startId, String tstamp, String plist, String endId, String tstamp2,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick, int divisions, int beatType) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        Integer startIndex = resolveXmlEventEndpointIndex(startId, tstamp, idToEventIndex, out, divisions, beatType,
                plist, idToEventTick);
        Integer endIndex = resolveXmlEventEndpointIndex(endId, tstamp2, idToEventIndex, out, divisions, beatType, null,
                idToEventTick);
        List<Integer> spanIndexes = resolveBeamSpanPlistIndexes(plist, idToEventIndex);
        if (spanIndexes.isEmpty()) {
            if (!isValidXmlEventIndex(out, startIndex) || !isValidXmlEventIndex(out, endIndex)) {
                return out;
            }
            spanIndexes = resolveBeamSpanIndexes(startIndex.intValue(), endIndex.intValue());
        }
        List<Integer> pitchedIndexes = new ArrayList<Integer>();
        for (Integer index : spanIndexes) {
            if (isValidXmlEventIndex(out, index) && !"rest".equals(out.get(index.intValue()).getKind())) {
                pitchedIndexes.add(index);
            }
        }
        if (pitchedIndexes.size() < 2) {
            return out;
        }
        for (int cursor = 0; cursor < pitchedIndexes.size(); cursor++) {
            int index = pitchedIndexes.get(cursor).intValue();
            String value = cursor == 0 ? "begin" : cursor == pitchedIndexes.size() - 1 ? "end" : "continue";
            ParsedMeiXmlEvent event = out.get(index);
            out.set(index, event.withXml(addBeamToEventXml(event.getXml(), value, 1)));
        }
        return out;
    }

    public static List<ParsedMeiXmlEvent> applyMeiTupletSpanControlEvent(Collection<ParsedMeiXmlEvent> events,
            String startId, String tstamp, String plist, String endId, String tstamp2,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick, int divisions, int beatType,
            int tupletNumber) {
        return applyMeiTypedSpanNotationEvent(events, "tuplet", startId, tstamp, plist, endId, tstamp2,
                idToEventIndex, idToEventTick, divisions, beatType, tupletNumber);
    }

    public static List<ParsedMeiXmlEvent> applyMeiGlissControlEvent(Collection<ParsedMeiXmlEvent> events,
            String startId, String tstamp, String plist, String endId, String tstamp2,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick, int divisions, int beatType,
            int glissNumber) {
        return applyMeiTypedSpanNotationEvent(events, "glissando", startId, tstamp, plist, endId, tstamp2,
                idToEventIndex, idToEventTick, divisions, beatType, glissNumber);
    }

    public static List<ParsedMeiXmlEvent> applyMeiSlideControlEvent(Collection<ParsedMeiXmlEvent> events,
            String startId, String tstamp, String plist, String endId, String tstamp2,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick, int divisions, int beatType,
            int slideNumber) {
        return applyMeiTypedSpanNotationEvent(events, "slide", startId, tstamp, plist, endId, tstamp2,
                idToEventIndex, idToEventTick, divisions, beatType, slideNumber);
    }

    public static List<ParsedMeiXmlEvent> applyStaffSpanControlEvents(Element staff, Element layer,
            Collection<ParsedMeiXmlEvent> layerEvents, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick, int divisions, int beatType) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(layerEvents);
        for (Element beamSpan : collectStaffLayerMeasureControls("beamSpan", staff, layer, false)) {
            out = applyMeiBeamSpanControlEvent(out, beamSpan.getAttribute("startid"), beamSpan.getAttribute("tstamp"),
                    beamSpan.getAttribute("plist"), beamSpan.getAttribute("endid"), beamSpan.getAttribute("tstamp2"),
                    idToEventIndex, idToEventTick, divisions, beatType);
        }
        int tupletNumber = 1;
        for (Element tupletSpan : collectStaffLayerMeasureControls("tupletSpan", staff, layer, false)) {
            out = applyMeiTupletSpanControlEvent(out, tupletSpan.getAttribute("startid"),
                    tupletSpan.getAttribute("tstamp"), tupletSpan.getAttribute("plist"),
                    tupletSpan.getAttribute("endid"), tupletSpan.getAttribute("tstamp2"), idToEventIndex,
                    idToEventTick, divisions, beatType, tupletNumber);
            tupletNumber++;
        }
        int glissNumber = 1;
        for (Element gliss : collectStaffLayerMeasureControls("gliss", staff, layer, false)) {
            out = applyMeiGlissControlEvent(out, gliss.getAttribute("startid"), gliss.getAttribute("tstamp"),
                    gliss.getAttribute("plist"), gliss.getAttribute("endid"), gliss.getAttribute("tstamp2"),
                    idToEventIndex, idToEventTick, divisions, beatType, glissNumber);
            glissNumber++;
        }
        int slideNumber = 1;
        for (Element slide : collectStaffLayerMeasureControls("slide", staff, layer, false)) {
            out = applyMeiSlideControlEvent(out, slide.getAttribute("startid"), slide.getAttribute("tstamp"),
                    slide.getAttribute("plist"), slide.getAttribute("endid"), slide.getAttribute("tstamp2"),
                    idToEventIndex, idToEventTick, divisions, beatType, slideNumber);
            slideNumber++;
        }
        return out;
    }

    public static MeiLayerTrimResult trimLayerEventsToMeasureCapacity(Collection<ParsedMeiXmlEvent> events,
            int measureTicks) {
        List<ParsedMeiXmlEvent> kept = new ArrayList<ParsedMeiXmlEvent>();
        int capacity = Math.max(1, measureTicks);
        int totalTicks = 0;
        int droppedCount = 0;
        int droppedTicks = 0;
        int trimmedCount = 0;
        int trimmedTicks = 0;
        if (events != null) {
            for (ParsedMeiXmlEvent event : events) {
                int durationTicks = Math.max(0, event == null ? 0 : event.getDurationTicks());
                int nextTotal = totalTicks + durationTicks;
                if (nextTotal <= capacity) {
                    kept.add(event);
                    totalTicks += durationTicks;
                    continue;
                }
                int overflow = nextTotal - capacity;
                boolean minorOverflow = overflow > 0 && overflow <= Math.max(12, Math.round(durationTicks * 0.1f));
                if (minorOverflow) {
                    kept.add(event);
                    totalTicks = nextTotal;
                    trimmedCount++;
                    trimmedTicks += overflow;
                    continue;
                }
                droppedCount++;
                droppedTicks += durationTicks;
            }
        }
        return new MeiLayerTrimResult(kept, totalTicks, droppedCount, droppedTicks, trimmedCount, trimmedTicks);
    }

    public static List<MiscField> buildMeiSourceRawMiscFields(String source) {
        String raw = source == null ? "" : source;
        if (raw.length() == 0) {
            return Collections.emptyList();
        }
        String encoded = raw.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
        int chunkSize = 240;
        int maxChunks = 16384;
        List<String> chunks = new ArrayList<String>();
        for (int index = 0; index < encoded.length() && chunks.size() < maxChunks; index += chunkSize) {
            chunks.add(encoded.substring(index, Math.min(encoded.length(), index + chunkSize)));
        }
        boolean truncated = joinLength(chunks) < encoded.length();
        List<MiscField> fields = new ArrayList<MiscField>();
        fields.add(new MiscField("mks:src:mei:raw-encoding", "escape-v1"));
        fields.add(new MiscField("mks:src:mei:raw-length", Integer.toString(raw.length())));
        fields.add(new MiscField("mks:src:mei:raw-encoded-length", Integer.toString(encoded.length())));
        fields.add(new MiscField("mks:src:mei:raw-chunks", Integer.toString(chunks.size())));
        fields.add(new MiscField("mks:src:mei:raw-truncated", truncated ? "1" : "0"));
        for (int index = 0; index < chunks.size(); index++) {
            fields.add(new MiscField("mks:src:mei:raw-" + zeroPad(index + 1, 4), chunks.get(index)));
        }
        return fields;
    }

    public static String buildMusicXmlMiscellaneousXml(Collection<MiscField> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        xml.append("<miscellaneous>");
        for (MiscField field : fields) {
            if (field == null) {
                continue;
            }
            xml.append("<miscellaneous-field name=\"").append(xmlEscape(field.getName())).append("\">")
                    .append(xmlEscape(field.getValue())).append("</miscellaneous-field>");
        }
        xml.append("</miscellaneous>");
        return xml.toString();
    }

    public static List<MiscField> extractMiscFieldsFromMeiStaff(Element staff) {
        List<MiscField> out = new ArrayList<MiscField>();
        for (Element annot : directChildElementsByLocalName(staff, "annot")) {
            String type = annot.getAttribute("type") == null ? "" : annot.getAttribute("type").trim();
            if (!"musicxml-misc-field".equals(type)) {
                continue;
            }
            String name = normalizeMeiMiscFieldName(annot.getAttribute("label"));
            if (name.length() == 0) {
                continue;
            }
            out.add(new MiscField(name, textOf(annot).trim()));
        }
        return out;
    }

    public static MeiMeasureMeta parseMeiMeasureMetaText(String text) {
        String raw = text == null ? "" : text.trim();
        if (raw.length() == 0) {
            return null;
        }
        MeiMeasureMeta.Builder builder = new MeiMeasureMeta.Builder();
        boolean hasValue = false;
        for (String token : raw.split(";")) {
            String trimmed = token == null ? "" : token.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (key.length() == 0) {
                continue;
            }
            if ("number".equals(key) && value.length() > 0) {
                builder.number(value);
                hasValue = true;
            } else if ("implicit".equals(key)) {
                builder.implicit(isTruthyText(value));
                hasValue = true;
            } else if ("repeat".equals(key) && ("forward".equals(value) || "backward".equals(value))) {
                builder.repeat(value);
                hasValue = true;
            } else if ("times".equals(key)) {
                int times = parseIntSafe(value, 0);
                if (times > 1) {
                    builder.times(times);
                    hasValue = true;
                }
            } else if ("explicittime".equals(key)) {
                builder.explicitTime(isTruthyText(value));
                hasValue = true;
            } else if ("beats".equals(key)) {
                int beats = parseIntSafe(value, 0);
                if (beats > 0) {
                    builder.beats(beats);
                    hasValue = true;
                }
            } else if ("beattype".equals(key)) {
                int beatType = parseIntSafe(value, 0);
                if (beatType > 0) {
                    builder.beatType(beatType);
                    hasValue = true;
                }
            } else if ("doublebar".equals(key)
                    && ("left".equals(value) || "right".equals(value) || "both".equals(value))) {
                builder.doubleBar(value);
                hasValue = true;
            }
        }
        return hasValue ? builder.build() : null;
    }

    public static MeiMeasureMeta parseMeasureMetaFromMeiStaff(Element staff) {
        for (Element annot : directChildElementsByLocalName(staff, "annot")) {
            String type = annot.getAttribute("type") == null ? "" : annot.getAttribute("type").trim().toLowerCase();
            String label = annot.getAttribute("label") == null ? "" : annot.getAttribute("label").trim()
                    .toLowerCase();
            if ("musicxml-measure-meta".equals(type) || "mks:measure-meta".equals(label)) {
                return parseMeiMeasureMetaText(textOf(annot));
            }
        }
        return null;
    }

    public static List<MiscField> buildMeiOverfullDiagnosticFields(String measureNo, String staffNo,
            int sourceTotalTicks, int measureTicks, int droppedEvents, int droppedTicks, int trimmedEvents,
            int trimmedTicks) {
        if (sourceTotalTicks <= measureTicks) {
            return Collections.emptyList();
        }
        List<MiscField> fields = new ArrayList<MiscField>();
        fields.add(new MiscField("mks:diag:count", "1"));
        fields.add(new MiscField("mks:diag:0001",
                "level=warn;code=OVERFULL_CLAMPED;fmt=mei;measure=" + (measureNo == null ? "" : measureNo)
                        + ";staff=" + (staffNo == null ? "" : staffNo)
                        + ";action=clamped;sourceTicks=" + sourceTotalTicks + ";capacityTicks="
                        + Math.max(1, measureTicks) + ";droppedEvents=" + Math.max(0, droppedEvents)
                        + ";droppedTicks=" + Math.max(0, droppedTicks) + ";trimmedEvents="
                        + Math.max(0, trimmedEvents) + ";trimmedTicks=" + Math.max(0, trimmedTicks)));
        return fields;
    }

    public static boolean isLikelyPickupMeasure(boolean implicitFromMeta, int measureIndex, int maxLayerTicks,
            int measureTicks) {
        return !implicitFromMeta && measureIndex == 0 && maxLayerTicks > 0 && maxLayerTicks < measureTicks;
    }

    public static String buildMeasureImplicitAttribute(boolean implicitFromMeta, int measureIndex, int maxLayerTicks,
            int measureTicks) {
        return implicitFromMeta || isLikelyPickupMeasure(implicitFromMeta, measureIndex, maxLayerTicks, measureTicks)
                ? " implicit=\"yes\"" : "";
    }

    public static String buildMeiMeasureBodyXml(Collection<MeiLayerXml> layers, int measureTicks) {
        if (layers == null || layers.isEmpty()) {
            return "";
        }
        List<MeiLayerXml> layerList = new ArrayList<MeiLayerXml>(layers);
        StringBuilder body = new StringBuilder();
        body.append(layerList.get(0).getXml());
        int backupTicks = Math.max(Math.max(1, measureTicks), layerList.get(0).getTotalTicks());
        for (int index = 1; index < layerList.size(); index++) {
            body.append("<backup><duration>").append(backupTicks).append("</duration></backup>");
            body.append(layerList.get(index).getXml());
        }
        return body.toString();
    }

    public static String buildMeiMeasureLeftBarlineXml(MeiMeasureMeta meta) {
        if (meta == null) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        if ("left".equals(meta.getDoubleBar()) || "both".equals(meta.getDoubleBar())) {
            xml.append("<barline location=\"left\"><bar-style>light-light</bar-style></barline>");
        }
        if ("forward".equals(meta.getRepeat())) {
            xml.append("<barline location=\"left\"><repeat direction=\"forward\"/></barline>");
        }
        return xml.toString();
    }

    public static String buildMeiMeasureRightBarlineXml(MeiMeasureMeta meta) {
        if (meta == null) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        if ("backward".equals(meta.getRepeat())) {
            String repeatInner = meta.getTimes() != null && meta.getTimes().intValue() > 1
                    ? "<bar-style>light-heavy</bar-style><repeat direction=\"backward\"/><ending number=\""
                            + meta.getTimes().intValue() + "\" type=\"stop\"/>"
                    : "<repeat direction=\"backward\"/>";
            xml.append("<barline location=\"right\">").append(repeatInner).append("</barline>");
        }
        if ("right".equals(meta.getDoubleBar()) || "both".equals(meta.getDoubleBar())) {
            xml.append("<barline location=\"right\"><bar-style>light-light</bar-style></barline>");
        }
        return xml.toString();
    }

    public static String buildMeiMeasureAttributesXml(boolean hasEmittedInitialAttributes, boolean shouldEmitTime,
            boolean shouldEmitKey, boolean shouldEmitTranspose, boolean shouldEmitClef, int divisions, int fifths,
            int beats, int beatType, String timeSymbol, Integer transposeChromatic, Integer transposeDiatonic,
            String clefSign, int clefLine, String miscellaneousXml) {
        String misc = miscellaneousXml == null ? "" : miscellaneousXml;
        if (!hasEmittedInitialAttributes) {
            return "<attributes><divisions>" + Math.max(1, divisions) + "</divisions><key><fifths>" + fifths
                    + "</fifths></key>" + buildTimeXml(beats, beatType, timeSymbol)
                    + buildTransposeXml(transposeChromatic, transposeDiatonic) + "<clef><sign>"
                    + xmlEscape(clefSign) + "</sign><line>" + Math.max(1, clefLine) + "</line></clef>" + misc
                    + "</attributes>";
        }
        if (!shouldEmitTime && !shouldEmitKey && !shouldEmitTranspose && !shouldEmitClef && misc.length() == 0) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        xml.append("<attributes>");
        if (shouldEmitKey) {
            xml.append("<key><fifths>").append(fifths).append("</fifths></key>");
        }
        if (shouldEmitTime) {
            xml.append(buildTimeXml(beats, beatType, timeSymbol));
        }
        if (shouldEmitTranspose) {
            xml.append(buildTransposeXml(transposeChromatic, transposeDiatonic));
        }
        if (shouldEmitClef) {
            xml.append("<clef><sign>").append(xmlEscape(clefSign)).append("</sign><line>")
                    .append(Math.max(1, clefLine)).append("</line></clef>");
        }
        xml.append(misc);
        xml.append("</attributes>");
        return xml.toString();
    }

    public static String buildMeiImportedMeasureXml(String measureNo, String implicitAttr, String attributesXml,
            String leftBarlineXml, String bodyXml, String rightBarlineXml) {
        return "<measure number=\"" + xmlEscape(measureNo) + "\"" + (implicitAttr == null ? "" : implicitAttr) + ">"
                + (attributesXml == null ? "" : attributesXml) + (leftBarlineXml == null ? "" : leftBarlineXml)
                + (bodyXml == null ? "" : bodyXml) + (rightBarlineXml == null ? "" : rightBarlineXml)
                + "</measure>";
    }

    public static String buildMeiEmptyImportedMeasureXml(String measureNo) {
        return "<measure number=\"" + xmlEscape(measureNo) + "\"></measure>";
    }

    public static String buildMeiImportedPartXml(String partId, String measuresXml) {
        return "<part id=\"" + xmlEscape(partId) + "\">" + (measuresXml == null ? "" : measuresXml) + "</part>";
    }

    public static String buildMeiScorePartXml(String partId, String partName) {
        return "<score-part id=\"" + xmlEscape(partId) + "\"><part-name>" + xmlEscape(partName)
                + "</part-name></score-part>";
    }

    public static String buildMeiScorePartwiseXmlDocument(String title, String partListXml, String partsXml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>"
                + xmlEscape(title) + "</work-title></work><part-list>" + (partListXml == null ? "" : partListXml)
                + "</part-list>" + (partsXml == null ? "" : partsXml) + "</score-partwise>";
    }

    public static MeiTieCarryResult applyTieCarryAccidentalsForLayerEvents(Collection<ParsedMeiXmlEvent> events,
            Map<String, Integer> tieCarryIn) {
        List<ParsedMeiXmlEvent> out = new ArrayList<ParsedMeiXmlEvent>();
        Map<String, Integer> tieCarryByPitch = tieCarryIn == null ? new HashMap<String, Integer>()
                : new HashMap<String, Integer>(tieCarryIn);
        if (events == null || events.isEmpty()) {
            return new MeiTieCarryResult(out, tieCarryByPitch);
        }
        for (ParsedMeiXmlEvent event : events) {
            if (event == null) {
                continue;
            }
            if ("rest".equals(event.getKind()) || event.getXml().indexOf("<note") < 0) {
                out.add(event);
                continue;
            }
            ParsedMeiXmlEvent next = applyTieCarryAccidentalsForEvent(event, tieCarryByPitch);
            out.add(next);
        }
        return new MeiTieCarryResult(out, tieCarryByPitch);
    }

    public static String buildMeiDebugEntryValue(int globalIndex, String measureNo, String staffNo, String layerNo,
            int layerEntryIndex, String kind, String dur, int durationTicks, String pname, String octave,
            String accid, int chordNoteCount) {
        String normalizedKind = kind == null ? "" : kind.trim();
        StringBuilder entry = new StringBuilder();
        entry.append("idx=").append(toHex(globalIndex, 4));
        entry.append(";m=").append(xmlEscape(measureNo));
        entry.append(";stf=").append(xmlEscape(staffNo == null || staffNo.trim().length() == 0 ? "1" : staffNo));
        entry.append(";ly=").append(xmlEscape(layerNo == null || layerNo.trim().length() == 0 ? "1" : layerNo));
        entry.append(";li=").append(toHex(layerEntryIndex, 4));
        entry.append(";k=").append(xmlEscape(normalizedKind));
        entry.append(";du=").append(xmlEscape(dur == null || dur.length() == 0 ? "4" : dur));
        entry.append(";dt=").append(toHex(Math.max(1, durationTicks), 4));
        if ("note".equals(normalizedKind)) {
            entry.append(";pn=").append(xmlEscape((pname == null || pname.trim().length() == 0 ? "c" : pname)
                    .trim().toUpperCase()));
            entry.append(";oc=").append(xmlEscape(octave == null || octave.trim().length() == 0 ? "4" : octave));
            if (accid != null && accid.trim().length() > 0) {
                entry.append(";ac=").append(xmlEscape(accid));
            }
        } else if ("chord".equals(normalizedKind)) {
            entry.append(";cn=").append(toHex(Math.max(0, chordNoteCount), 2));
        }
        return entry.toString();
    }

    public static List<MiscField> buildMeiDebugFieldsFromEntryValues(Collection<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> entryList = new ArrayList<String>(entries);
        List<MiscField> fields = new ArrayList<MiscField>();
        fields.add(new MiscField("mks:dbg:mei:notes:count", toHex(entryList.size(), 4)));
        for (int index = 0; index < entryList.size(); index++) {
            fields.add(new MiscField("mks:dbg:mei:notes:" + zeroPad(index + 1, 4), entryList.get(index)));
        }
        return fields;
    }

    public static List<MiscField> buildMeiDebugFieldsFromEventValues(Collection<MeiDebugEventValue> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> entries = new ArrayList<String>();
        int globalIndex = 0;
        for (MeiDebugEventValue event : events) {
            if (event == null) {
                continue;
            }
            entries.add(buildMeiDebugEntryValue(globalIndex, event.getMeasureNo(), event.getStaffNo(),
                    event.getLayerNo(), event.getLayerEntryIndex(), event.getKind(), event.getDur(),
                    event.getDurationTicks(), event.getPname(), event.getOctave(), event.getAccid(),
                    event.getChordNoteCount()));
            globalIndex++;
        }
        return buildMeiDebugFieldsFromEntryValues(entries);
    }

    public static List<MiscField> buildMeiDebugFieldsFromStaff(Element staff, String measureNo, int divisions) {
        if (staff == null) {
            return Collections.emptyList();
        }
        List<MeiDebugEventValue> events = new ArrayList<MeiDebugEventValue>();
        String staffNo = staff.getAttribute("n") == null || staff.getAttribute("n").trim().length() == 0 ? "1"
                : staff.getAttribute("n").trim();
        List<Element> layers = directChildElementsByLocalName(staff, "layer");
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            Element layer = layers.get(layerIndex);
            String layerNo = layer.getAttribute("n") == null || layer.getAttribute("n").trim().length() == 0
                    ? Integer.toString(layerIndex + 1)
                    : layer.getAttribute("n").trim();
            int layerEntryIndex = 0;
            for (Element child : directChildElements(layer)) {
                String kind = localNameOf(child);
                if (!"note".equals(kind) && !"rest".equals(kind) && !"chord".equals(kind)) {
                    continue;
                }
                String dur = child.getAttribute("dur") == null || child.getAttribute("dur").trim().length() == 0 ? "4"
                        : child.getAttribute("dur").trim();
                int dots = parseIntSafe(child.getAttribute("dots"), 0);
                int ticks = Math.max(1,
                        (int) Math.round(meiDurToQuarterLength(dur) * dotsMultiplier(dots) * Math.max(1, divisions)));
                String pname = "note".equals(kind) ? child.getAttribute("pname") : "";
                String octave = "note".equals(kind) ? child.getAttribute("oct") : "";
                String accid = "note".equals(kind) ? child.getAttribute("accid") : "";
                int chordNoteCount = "chord".equals(kind) ? directChildElementsByLocalName(child, "note").size() : 0;
                events.add(new MeiDebugEventValue(measureNo, staffNo, layerNo, layerEntryIndex, kind, dur, ticks,
                        pname, octave, accid, chordNoteCount));
                layerEntryIndex++;
            }
        }
        return buildMeiDebugFieldsFromEventValues(events);
    }

    public static ResolvedMeiImportOptions resolveMeiImportOptions(Boolean debugMetadata, Boolean sourceMetadata,
            Boolean failOnOverfullDrop, Integer meiCorpusIndex) {
        Integer safeCorpusIndex = meiCorpusIndex == null ? null : Integer.valueOf(Math.max(0, meiCorpusIndex.intValue()));
        return new ResolvedMeiImportOptions(debugMetadata == null || debugMetadata.booleanValue(),
                sourceMetadata == null || sourceMetadata.booleanValue(),
                failOnOverfullDrop != null && failOnOverfullDrop.booleanValue(), safeCorpusIndex);
    }

    public static Element selectMeiImportRoot(Document doc, Integer meiCorpusIndex) {
        Element meiRoot = doc == null ? null : doc.getDocumentElement();
        if (meiRoot == null) {
            throw new IllegalArgumentException("MEI root is missing.");
        }
        String rootName = localNameOf(meiRoot);
        if ("mei".equals(rootName)) {
            return meiRoot;
        }
        if (!"meiCorpus".equals(rootName)) {
            throw new IllegalArgumentException("MEI root must be <mei> or <meiCorpus>.");
        }
        List<Element> meiNodes = directChildElementsByLocalName(meiRoot, "mei");
        Element selected = null;
        if (meiCorpusIndex != null) {
            int index = Math.max(0, meiCorpusIndex.intValue());
            if (index < meiNodes.size()) {
                selected = meiNodes.get(index);
            } else {
                throw new IllegalArgumentException("MEI corpus index out of range: " + index + " (size="
                        + meiNodes.size() + ").");
            }
        } else {
            for (Element candidate : meiNodes) {
                if (!descendantElementsByLocalName(candidate, "measure").isEmpty()) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null && !meiNodes.isEmpty()) {
                selected = meiNodes.get(0);
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("MEI corpus has no child <mei>.");
        }
        return selected;
    }

    public static String firstDescendantText(Element root, String name) {
        List<Element> elements = descendantElementsByLocalName(root, name);
        if (elements.isEmpty()) {
            return "";
        }
        return textOf(elements.get(0)).trim();
    }

    public static List<String> collectSortedStaffNumbersFromMei(Element meiImportRoot) {
        List<Element> measures = descendantElementsByLocalName(meiImportRoot, "measure");
        if (measures.isEmpty()) {
            throw new IllegalArgumentException("MEI has no <measure>.");
        }
        Set<String> staffNumbers = new LinkedHashSet<String>();
        for (Element measure : measures) {
            for (Element staff : directChildElementsByLocalName(measure, "staff")) {
                String n = staff.getAttribute("n") == null ? "" : staff.getAttribute("n").trim();
                if (n.length() > 0) {
                    staffNumbers.add(n);
                }
            }
        }
        if (staffNumbers.isEmpty()) {
            throw new IllegalArgumentException("MEI has no <staff> content.");
        }
        List<String> sorted = new ArrayList<String>(staffNumbers);
        Collections.sort(sorted, new java.util.Comparator<String>() {
            public int compare(String left, String right) {
                return parseIntSafe(left, 0) - parseIntSafe(right, 0);
            }
        });
        return sorted;
    }

    public static MeiInitialImportContext buildMeiInitialImportContext(Element meiImportRoot) {
        if (meiImportRoot == null) {
            throw new IllegalArgumentException("MEI root is missing.");
        }
        String title = firstDescendantText(meiImportRoot, "title");
        if (title.length() == 0) {
            title = "mikuscore";
        }
        List<Element> scoreDefs = collectScoreDefsInDocOrder(meiImportRoot);
        Element scoreDef = scoreDefs.isEmpty() ? null : scoreDefs.get(0);
        int meterCount = parseIntSafe(scoreDef == null ? null : scoreDef.getAttribute("meter.count"), 4);
        int meterUnit = parseIntSafe(scoreDef == null ? null : scoreDef.getAttribute("meter.unit"), 4);
        Integer parsedFifths = parseMeiKeyFifthsFromElement(scoreDef);
        int fifths = parsedFifths == null ? 0 : parsedFifths.intValue();
        List<Element> staffDefs = collectStaffDefsInDocOrder(meiImportRoot);
        Map<String, MeiStaffMeta> staffMeta = collectStaffMetaFromStaffDefs(staffDefs);
        List<Element> measureNodes = descendantElementsByLocalName(meiImportRoot, "measure");
        if (measureNodes.isEmpty()) {
            throw new IllegalArgumentException("MEI has no <measure>.");
        }
        List<String> staffNumbers = collectSortedStaffNumbersFromMei(meiImportRoot);
        return new MeiInitialImportContext(title, scoreDefs, staffDefs, meterCount, meterUnit, fifths,
                MEI_IMPORT_DIVISIONS, staffMeta, measureNodes, staffNumbers);
    }

    public static MeiPartImportState buildMeiInitialPartImportState(MeiInitialImportContext context, String staffNo,
            int partIndex) {
        if (context == null) {
            throw new IllegalArgumentException("MEI import context is missing.");
        }
        int safePartIndex = Math.max(0, partIndex);
        String safeStaffNo = staffNo == null || staffNo.trim().length() == 0 ? Integer.toString(safePartIndex + 1)
                : staffNo.trim();
        String partId = "P" + (safePartIndex + 1);
        MeiStaffMeta clef = context.getStaffMeta().get(safeStaffNo);
        if (clef == null) {
            clef = new MeiStaffMeta("Staff " + safeStaffNo, "G", 2);
        }
        Element scoreDef = context.getScoreDefs().isEmpty() ? null : context.getScoreDefs().get(0);
        MeiMeter initialMeter = parseMeterFromScoreDefForStaff(scoreDef, safeStaffNo, context.getMeterCount(),
                context.getMeterUnit());
        String currentTimeSymbol = parseTimeSymbolFromScoreDefForStaff(scoreDef, safeStaffNo);
        MeiTranspose currentTranspose = parseTransposeFromScoreDefForStaff(scoreDef, safeStaffNo);
        return new MeiPartImportState(safeStaffNo, partId, clef.getLabel(), initialMeter.getBeats(),
                initialMeter.getBeatType(), currentTimeSymbol, context.getFifths(), clef.getClefSign(),
                clef.getClefLine(), currentTranspose, false);
    }

    public static MeiMeasureImportState buildMeiMeasureImportState(MeiInitialImportContext context,
            MeiPartImportState currentState, Element measureNode, int measureIndex) {
        if (context == null) {
            throw new IllegalArgumentException("MEI import context is missing.");
        }
        if (currentState == null) {
            throw new IllegalArgumentException("MEI part import state is missing.");
        }
        if (measureNode == null) {
            throw new IllegalArgumentException("MEI measure is missing.");
        }
        String sourceMeasureNo = measureNode.getAttribute("n") == null || measureNode.getAttribute("n").trim().length() == 0
                ? Integer.toString(Math.max(0, measureIndex) + 1)
                : measureNode.getAttribute("n").trim();
        Element targetStaff = null;
        for (Element staff : directChildElementsByLocalName(measureNode, "staff")) {
            String n = staff.getAttribute("n") == null ? "" : staff.getAttribute("n").trim();
            if (n.equals(currentState.getStaffNo())) {
                targetStaff = staff;
                break;
            }
        }
        if (targetStaff == null) {
            return MeiMeasureImportState.missingStaff(currentState, sourceMeasureNo, measureIndex);
        }
        Element effectiveScoreDef = findEffectiveScoreDefForNode(targetStaff, context.getScoreDefs());
        Element effectiveStaffDef = findEffectiveStaffDefForNode(targetStaff, currentState.getStaffNo(),
                context.getStaffDefs());
        MeiMeasureMeta measureMeta = parseMeasureMetaFromMeiStaff(targetStaff);
        String measureNo = measureMeta == null || measureMeta.getNumber() == null ? sourceMeasureNo
                : measureMeta.getNumber().trim();
        if (measureNo.length() == 0) {
            measureNo = sourceMeasureNo;
        }
        boolean implicitFromMeta = measureMeta != null && Boolean.TRUE.equals(measureMeta.getImplicit());
        MeiMeter scoreDefMeter = parseMeterFromScoreDefForStaff(effectiveScoreDef, currentState.getStaffNo(),
                currentState.getCurrentBeats(), currentState.getCurrentBeatType());
        String scoreDefTimeSymbol = parseTimeSymbolFromScoreDefForStaff(effectiveScoreDef, currentState.getStaffNo());
        int scoreDefFifths = parseKeySigFromScoreDefForStaff(effectiveScoreDef, currentState.getStaffNo(),
                currentState.getCurrentFifths());
        MeiClef scoreDefClef = parseClefFromScoreDefForStaff(effectiveScoreDef, currentState.getStaffNo());
        MeiTranspose scoreDefTranspose = parseTransposeFromScoreDefForStaff(effectiveScoreDef, currentState.getStaffNo());
        String staffDefTimeSymbol = parseTimeSymbolFromMeiElement(effectiveStaffDef);
        Integer staffDefFifths = parseMeiKeyFifthsFromElement(effectiveStaffDef);
        MeiClef staffDefClef = parseClefFromStaffDefElement(effectiveStaffDef);
        MeiTranspose staffDefTranspose = parseTransposeFromStaffDefElement(effectiveStaffDef);
        int measureBeats = Math.max(1,
                measureMeta != null && measureMeta.getBeats() != null ? measureMeta.getBeats().intValue()
                        : scoreDefMeter.getBeats());
        int measureBeatType = Math.max(1,
                measureMeta != null && measureMeta.getBeatType() != null ? measureMeta.getBeatType().intValue()
                        : scoreDefMeter.getBeatType());
        String measureTimeSymbol = staffDefTimeSymbol != null ? staffDefTimeSymbol
                : scoreDefTimeSymbol != null ? scoreDefTimeSymbol : currentState.getCurrentTimeSymbol();
        int measureFifths = staffDefFifths != null ? staffDefFifths.intValue() : scoreDefFifths;
        String measureClefSign = staffDefClef != null ? staffDefClef.getClefSign()
                : scoreDefClef != null ? scoreDefClef.getClefSign() : currentState.getCurrentClefSign();
        int measureClefLine = staffDefClef != null ? staffDefClef.getClefLine()
                : scoreDefClef != null ? scoreDefClef.getClefLine() : currentState.getCurrentClefLine();
        MeiTranspose measureTranspose = staffDefTranspose != null ? staffDefTranspose
                : scoreDefTranspose != null ? scoreDefTranspose : currentState.getCurrentTranspose();
        int measureTicks = Math.max(1,
                Math.round((measureBeats * 4.0f * context.getDivisions()) / Math.max(1, measureBeatType)));
        boolean shouldEmitTime = measureIndex == 0
                || measureMeta != null && Boolean.TRUE.equals(measureMeta.getExplicitTime())
                || measureBeats != currentState.getCurrentBeats()
                || measureBeatType != currentState.getCurrentBeatType()
                || !sameNullableString(measureTimeSymbol, currentState.getCurrentTimeSymbol());
        boolean shouldEmitKey = measureIndex == 0 || measureFifths != currentState.getCurrentFifths();
        boolean shouldEmitClef = measureIndex == 0 || !measureClefSign.equals(currentState.getCurrentClefSign())
                || measureClefLine != currentState.getCurrentClefLine();
        boolean shouldEmitTranspose = measureIndex == 0
                || !sameNullableInteger(transposeChromatic(measureTranspose),
                        transposeChromatic(currentState.getCurrentTranspose()))
                || !sameNullableInteger(transposeDiatonic(measureTranspose),
                        transposeDiatonic(currentState.getCurrentTranspose()));
        return new MeiMeasureImportState(currentState, sourceMeasureNo, targetStaff, true, effectiveScoreDef,
                effectiveStaffDef, measureMeta, measureNo, implicitFromMeta, measureBeats, measureBeatType,
                measureTimeSymbol, measureFifths, measureClefSign, measureClefLine, measureTranspose, measureTicks,
                shouldEmitTime, shouldEmitKey, shouldEmitTranspose, shouldEmitClef);
    }

    private static boolean sameNullableString(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static boolean sameNullableInteger(Integer left, Integer right) {
        return left == null ? right == null : left.equals(right);
    }

    private static Integer transposeChromatic(MeiTranspose transpose) {
        return transpose == null ? null : transpose.getChromatic();
    }

    private static Integer transposeDiatonic(MeiTranspose transpose) {
        return transpose == null ? null : transpose.getDiatonic();
    }

    public static String buildMeiPartListXml(Collection<String> staffNumbers, Map<String, String> staffLabels) {
        if (staffNumbers == null || staffNumbers.isEmpty()) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        int index = 0;
        Map<String, String> labels = staffLabels == null ? Collections.<String, String>emptyMap() : staffLabels;
        for (String staffNo : staffNumbers) {
            String safeStaffNo = staffNo == null || staffNo.trim().length() == 0 ? Integer.toString(index + 1)
                    : staffNo.trim();
            String partId = "P" + (index + 1);
            String partName = labels.containsKey(safeStaffNo) ? labels.get(safeStaffNo) : "Staff " + safeStaffNo;
            xml.append(buildMeiScorePartXml(partId, partName));
            index++;
        }
        return xml.toString();
    }

    public static MeiClef parseClefFromStaffDefElement(Element staffDef) {
        if (staffDef == null) {
            return null;
        }
        String attrSign = staffDef.getAttribute("clef.shape") == null ? ""
                : staffDef.getAttribute("clef.shape").trim().toUpperCase();
        int attrLine = parseIntSafe(staffDef.getAttribute("clef.line"), Integer.MIN_VALUE);
        if (attrSign.length() > 0 && attrLine != Integer.MIN_VALUE) {
            return new MeiClef(attrSign, Math.max(1, attrLine));
        }
        List<Element> clefs = directChildElementsByLocalName(staffDef, "clef");
        if (clefs.isEmpty()) {
            return null;
        }
        Element clef = clefs.get(0);
        String childSign = firstNonEmpty(clef.getAttribute("shape"), clef.getAttribute("clef.shape"), "")
                .trim().toUpperCase();
        int childLine = parseIntSafe(firstNonEmpty(clef.getAttribute("line"), clef.getAttribute("clef.line"), ""),
                Integer.MIN_VALUE);
        if (childSign.length() == 0 || childLine == Integer.MIN_VALUE) {
            return null;
        }
        return new MeiClef(childSign, Math.max(1, childLine));
    }

    public static String parseStaffLabelFromStaffDefElement(Element staffDef) {
        if (staffDef == null) {
            return "";
        }
        String attrLabel = staffDef.getAttribute("label") == null ? "" : staffDef.getAttribute("label").trim();
        if (attrLabel.length() > 0) {
            return attrLabel;
        }
        List<Element> labels = directChildElementsByLocalName(staffDef, "label");
        if (!labels.isEmpty()) {
            String labelText = textOf(labels.get(0)).trim();
            if (labelText.length() > 0) {
                return labelText;
            }
        }
        List<Element> abbrs = directChildElementsByLocalName(staffDef, "labelAbbr");
        return abbrs.isEmpty() ? "" : textOf(abbrs.get(0)).trim();
    }

    public static Map<String, MeiStaffMeta> collectStaffMetaFromStaffDefs(Collection<Element> staffDefs) {
        Map<String, MeiStaffMeta> out = new HashMap<String, MeiStaffMeta>();
        if (staffDefs == null) {
            return out;
        }
        for (Element staffDef : staffDefs) {
            String n = staffDef == null ? "" : staffDef.getAttribute("n").trim();
            if (n.length() == 0) {
                continue;
            }
            MeiStaffMeta previous = out.get(n);
            MeiClef parsedClef = parseClefFromStaffDefElement(staffDef);
            String label = parseStaffLabelFromStaffDefElement(staffDef);
            String resolvedLabel = label.length() > 0 ? label : previous == null ? "Staff " + n : previous.getLabel();
            String clefSign = parsedClef != null ? parsedClef.getClefSign()
                    : previous == null ? "G" : previous.getClefSign();
            int clefLine = parsedClef != null ? parsedClef.getClefLine() : previous == null ? 2 : previous.getClefLine();
            out.put(n, new MeiStaffMeta(resolvedLabel, clefSign, clefLine));
        }
        return out;
    }

    public static List<Element> collectScoreDefsInDocOrder(Element root) {
        return descendantElementsByLocalName(root, "scoreDef");
    }

    public static List<Element> collectStaffDefsInDocOrder(Element root) {
        return descendantElementsByLocalName(root, "staffDef");
    }

    public static Element findEffectiveScoreDefForNode(Element node, Collection<Element> scoreDefs) {
        if (node == null || scoreDefs == null) {
            return null;
        }
        Element out = null;
        for (Element scoreDef : scoreDefs) {
            if (scoreDef == null) {
                continue;
            }
            short relation = scoreDef.compareDocumentPosition(node);
            if ((relation & Node.DOCUMENT_POSITION_FOLLOWING) != 0 || scoreDef == node) {
                out = scoreDef;
                continue;
            }
            if ((relation & Node.DOCUMENT_POSITION_PRECEDING) != 0) {
                break;
            }
        }
        return out;
    }

    public static Element findEffectiveStaffDefForNode(Element node, String staffNo, Collection<Element> staffDefs) {
        if (node == null || staffDefs == null) {
            return null;
        }
        String normalizedStaffNo = staffNo == null ? "" : staffNo.trim();
        Element out = null;
        for (Element staffDef : staffDefs) {
            if (staffDef == null) {
                continue;
            }
            short relation = staffDef.compareDocumentPosition(node);
            if ((relation & Node.DOCUMENT_POSITION_FOLLOWING) != 0 || staffDef == node) {
                String n = staffDef.getAttribute("n") == null ? "" : staffDef.getAttribute("n").trim();
                if (n.length() == 0 || n.equals(normalizedStaffNo)) {
                    out = staffDef;
                }
                continue;
            }
            if ((relation & Node.DOCUMENT_POSITION_PRECEDING) != 0) {
                break;
            }
        }
        return out;
    }

    public static MeiTranspose parseTransposeFromStaffDefElement(Element staffDef) {
        if (staffDef == null) {
            return null;
        }
        int diatonic = parseIntSafe(staffDef.getAttribute("trans.diat"), Integer.MIN_VALUE);
        int chromatic = parseIntSafe(staffDef.getAttribute("trans.semi"), Integer.MIN_VALUE);
        Integer resolvedDiatonic = diatonic == Integer.MIN_VALUE ? null : Integer.valueOf(diatonic);
        Integer resolvedChromatic = chromatic == Integer.MIN_VALUE ? null : Integer.valueOf(chromatic);
        return resolvedDiatonic == null && resolvedChromatic == null ? null
                : new MeiTranspose(resolvedChromatic, resolvedDiatonic);
    }

    public static int parseKeySigFromScoreDefForStaff(Element scoreDef, String staffNo, int fallbackFifths) {
        if (scoreDef == null) {
            return fallbackFifths;
        }
        Element matched = findScoreDefStaffDef(scoreDef, staffNo);
        Integer staffFifths = parseMeiKeyFifthsFromElement(matched);
        if (staffFifths != null) {
            return staffFifths.intValue();
        }
        Integer scoreFifths = parseMeiKeyFifthsFromElement(scoreDef);
        return scoreFifths == null ? fallbackFifths : scoreFifths.intValue();
    }

    public static MeiTranspose parseTransposeFromScoreDefForStaff(Element scoreDef, String staffNo) {
        if (scoreDef == null) {
            return null;
        }
        MeiTranspose matchedTranspose = parseTransposeFromStaffDefElement(findScoreDefStaffDef(scoreDef, staffNo));
        if (matchedTranspose != null) {
            return matchedTranspose;
        }
        return parseTransposeFromStaffDefElement(scoreDef);
    }

    public static String parseTimeSymbolFromMeiElement(Element element) {
        if (element == null) {
            return null;
        }
        String raw = element.getAttribute("meter.sym") == null ? "" : element.getAttribute("meter.sym").trim()
                .toLowerCase();
        if ("common".equals(raw) || "c".equals(raw)) {
            return "common";
        }
        if ("cut".equals(raw) || "c|".equals(raw)) {
            return "cut";
        }
        return null;
    }

    public static String parseTimeSymbolFromScoreDefForStaff(Element scoreDef, String staffNo) {
        if (scoreDef == null) {
            return null;
        }
        Element matched = findScoreDefStaffDef(scoreDef, staffNo);
        String staffSymbol = parseTimeSymbolFromMeiElement(matched);
        return staffSymbol != null ? staffSymbol : parseTimeSymbolFromMeiElement(scoreDef);
    }

    public static MeiMeter parseMeterFromScoreDefForStaff(Element scoreDef, String staffNo, int fallbackBeats,
            int fallbackBeatType) {
        int safeFallbackBeats = Math.max(1, fallbackBeats);
        int safeFallbackBeatType = Math.max(1, fallbackBeatType);
        if (scoreDef == null) {
            return new MeiMeter(safeFallbackBeats, safeFallbackBeatType);
        }
        Element matched = findScoreDefStaffDef(scoreDef, staffNo);
        String beatsText = attributeOrFallback(matched, scoreDef, "meter.count");
        String beatTypeText = attributeOrFallback(matched, scoreDef, "meter.unit");
        int beats = parseIntSafe(beatsText, safeFallbackBeats);
        int beatType = parseIntSafe(beatTypeText, safeFallbackBeatType);
        return new MeiMeter(Math.max(1, beats), Math.max(1, beatType));
    }

    public static MeiClef parseClefFromScoreDefForStaff(Element scoreDef, String staffNo) {
        if (scoreDef == null) {
            return null;
        }
        Element matched = findScoreDefStaffDef(scoreDef, staffNo);
        return parseClefFromStaffDefElement(matched);
    }

    private static Integer resolveEndpointById(String id, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick, List<ParsedMeiEvent> events) {
        if (id == null || id.length() == 0) {
            return null;
        }
        Integer index = idToEventIndex.get(id);
        if (index != null) {
            return index;
        }
        Integer tick = idToEventTick.get(id);
        if (tick != null) {
            return resolveEventIndexByTstamp(events, Math.max(0, tick.intValue()));
        }
        return null;
    }

    private static Integer resolveControlEventStartTick(String rawId, String tstamp, Map<String, Integer> idToEventIndex,
            Collection<ParsedMeiEvent> events, int divisions, int beatType, String rawPlist,
            Map<String, Integer> idToEventTick) {
        Integer index = resolveControlEventEndpointIndex(rawId, tstamp, idToEventIndex, events, divisions, beatType,
                rawPlist, idToEventTick);
        return index == null ? null : resolveEventStartTickByIndex(events, index.intValue());
    }

    private static Integer resolveXmlEventEndpointIndex(String rawId, String tstamp, Map<String, Integer> idToEventIndex,
            Collection<ParsedMeiXmlEvent> events, int divisions, int beatType, String rawPlist,
            Map<String, Integer> idToEventTick) {
        List<ParsedMeiEvent> plainEvents = toParsedMeiEvents(events);
        return resolveControlEventEndpointIndex(rawId, tstamp, idToEventIndex, plainEvents, divisions, beatType,
                rawPlist, idToEventTick);
    }

    private static List<ParsedMeiXmlEvent> copyXmlEvents(Collection<ParsedMeiXmlEvent> events) {
        return events == null ? new ArrayList<ParsedMeiXmlEvent>() : new ArrayList<ParsedMeiXmlEvent>(events);
    }

    private static List<ParsedMeiEvent> toParsedMeiEvents(Collection<ParsedMeiXmlEvent> events) {
        List<ParsedMeiEvent> plainEvents = new ArrayList<ParsedMeiEvent>();
        if (events != null) {
            for (ParsedMeiXmlEvent event : events) {
                plainEvents.add(new ParsedMeiEvent(event == null ? "" : event.getKind(),
                        event == null ? 0 : event.getDurationTicks()));
            }
        }
        return plainEvents;
    }

    private static void parseMeiLayerEventElement(Element node, int divisions, String voice, int measureTicks,
            int measureFifths, Map<String, Integer> tieCarryByPitch, Map<String, Integer> measureAccidentalByPitch,
            String forcedGrace, MeiForcedTuplet forcedTuplet, List<ParsedMeiXmlEvent> events,
            Map<String, Integer> idToEventIndex) {
        if (node == null) {
            return;
        }
        String name = localNameOf(node);
        if ("beam".equals(name)) {
            int startIndex = events.size();
            for (Element child : directChildElements(node)) {
                parseMeiLayerEventElement(child, divisions, voice, measureTicks, measureFifths, tieCarryByPitch,
                        measureAccidentalByPitch, forcedGrace, forcedTuplet, events, idToEventIndex);
            }
            int endIndex = events.size();
            if (endIndex > startIndex) {
                List<ParsedMeiXmlEvent> beamed = applyMeiBeamContainerToEvents(events.subList(startIndex, endIndex));
                for (int index = 0; index < beamed.size(); index++) {
                    events.set(startIndex + index, beamed.get(index));
                }
            }
            return;
        }
        if ("tuplet".equals(name)) {
            MeiForcedTuplet nextTuplet = resolveMeiTupletContext(node, forcedTuplet);
            for (Element child : directChildElements(node)) {
                parseMeiLayerEventElement(child, divisions, voice, measureTicks, measureFifths, tieCarryByPitch,
                        measureAccidentalByPitch, forcedGrace, nextTuplet, events, idToEventIndex);
            }
            return;
        }
        if ("graceGrp".equals(name)) {
            String groupGrace = resolveMeiGraceGroupValue(node);
            String nextGrace = forcedGrace == null ? groupGrace : forcedGrace;
            for (Element child : directChildElements(node)) {
                parseMeiLayerEventElement(child, divisions, voice, measureTicks, measureFifths, tieCarryByPitch,
                        measureAccidentalByPitch, nextGrace, forcedTuplet, events, idToEventIndex);
            }
            return;
        }

        Element effectiveNode = cloneMeiEventElementWithForcedContext(node, forcedGrace, forcedTuplet);
        if ("note".equals(name) || "chord".equals(name)) {
            List<Element> slashExpanded = expandMeiStemSlashNodes(effectiveNode, divisions);
            if (slashExpanded != null && slashExpanded.size() > 1) {
                for (Element expandedNode : slashExpanded) {
                    ParsedMeiXmlEvent event = "note".equals(name)
                            ? buildParsedMeiNoteEvent(expandedNode, divisions, voice, measureFifths, tieCarryByPitch,
                                    measureAccidentalByPitch)
                            : buildParsedMeiChordEvent(expandedNode, divisions, voice, measureFifths, tieCarryByPitch,
                                    measureAccidentalByPitch);
                    addParsedMeiLayerEvent(event, expandedNode, events, idToEventIndex);
                }
                return;
            }
        }
        if ("note".equals(name)) {
            addParsedMeiLayerEvent(
                    buildParsedMeiNoteEvent(effectiveNode, divisions, voice, measureFifths, tieCarryByPitch,
                            measureAccidentalByPitch),
                    effectiveNode, events, idToEventIndex);
            return;
        }
        if ("rest".equals(name) || "space".equals(name) || "mSpace".equals(name) || "mRest".equals(name)) {
            if ("mSpace".equals(name) || "mRest".equals(name)) {
                effectiveNode = applyMeiMeasureRestDurationMetadata(effectiveNode, measureTicks, divisions);
            }
            addParsedMeiLayerEvent(buildParsedMeiRestEvent(effectiveNode, divisions, voice, forcedTuplet),
                    effectiveNode, events, idToEventIndex);
            return;
        }
        if ("chord".equals(name)) {
            addParsedMeiLayerEvent(
                    buildParsedMeiChordEvent(effectiveNode, divisions, voice, measureFifths, tieCarryByPitch,
                            measureAccidentalByPitch),
                    effectiveNode, events, idToEventIndex);
        }
    }

    private static void addParsedMeiLayerEvent(ParsedMeiXmlEvent event, Element source, List<ParsedMeiXmlEvent> events,
            Map<String, Integer> idToEventIndex) {
        if (event == null) {
            return;
        }
        int eventIndex = events.size();
        events.add(event);
        addMeiEventId(xmlIdAttribute(source), eventIndex, idToEventIndex, false);
        addMeiEventId(source == null ? "" : source.getAttribute("id"), eventIndex, idToEventIndex, false);
        if ("chord".equals(event.getKind()) && source != null) {
            for (Element chordNote : directChildElementsByLocalName(source, "note")) {
                addMeiEventId(xmlIdAttribute(chordNote), eventIndex, idToEventIndex, true);
                addMeiEventId(chordNote.getAttribute("id"), eventIndex, idToEventIndex, true);
            }
        }
    }

    private static String xmlIdAttribute(Element element) {
        if (element == null) {
            return "";
        }
        String namespaced = element.getAttributeNS("http://www.w3.org/XML/1998/namespace", "id");
        return namespaced == null || namespaced.trim().length() == 0 ? element.getAttribute("xml:id") : namespaced;
    }

    private static void addMeiEventId(String rawId, int eventIndex, Map<String, Integer> idToEventIndex,
            boolean onlyIfAbsent) {
        String id = rawId == null ? "" : rawId.trim();
        if (id.length() == 0) {
            return;
        }
        if (onlyIfAbsent && idToEventIndex.containsKey(id)) {
            return;
        }
        idToEventIndex.put(id, Integer.valueOf(eventIndex));
    }

    private static final class ParsedMeiLayerEntry {
        private final Element layer;
        private final String voice;
        private final ParsedMeiLayer parsedLayer;
        private final Map<String, Integer> tieCarryIn;

        private ParsedMeiLayerEntry(Element layer, String voice, ParsedMeiLayer parsedLayer,
                Map<String, Integer> tieCarryIn) {
            this.layer = layer;
            this.voice = voice == null ? "" : voice;
            this.parsedLayer = parsedLayer;
            this.tieCarryIn = tieCarryIn;
        }
    }

    private static List<Element> collectStaffLayerMeasureControls(String name, Element staff, Element layer,
            boolean includeMeasure) {
        List<Element> controls = new ArrayList<Element>();
        controls.addAll(directChildElementsByLocalName(layer, name));
        controls.addAll(directChildElementsByLocalName(staff, name));
        if (includeMeasure) {
            controls.addAll(directChildElementsByLocalName(parentElementByLocalName(staff, "measure"), name));
        }
        return controls;
    }

    private static List<ParsedMeiXmlEvent> applyStaffSingleNotationControls(Collection<ParsedMeiXmlEvent> events,
            String controlName, Collection<Element> controls, Map<String, Integer> idToEventIndex,
            Map<String, Integer> idToEventTick, int divisions, int beatType) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        if (controls == null) {
            return out;
        }
        for (Element control : controls) {
            String type = firstNonEmpty(control.getAttribute("type"), control.getAttribute("form"), "");
            String normalized = type.trim().toLowerCase();
            boolean inverted = "fermata".equals(controlName)
                    ? "below".equals(meiPlacement(control))
                    : normalized.indexOf("inv") >= 0 || normalized.indexOf("lower") >= 0
                            || normalized.indexOf("down") >= 0;
            out = applyMeiSingleNotationControlEvent(out, controlName, inverted, control.getAttribute("startid"),
                    control.getAttribute("tstamp"), control.getAttribute("plist"), idToEventIndex, idToEventTick,
                    divisions, beatType);
        }
        return out;
    }

    private static boolean isValidXmlEventIndex(List<ParsedMeiXmlEvent> events, Integer index) {
        return index != null && index.intValue() >= 0 && index.intValue() < events.size();
    }

    private static List<ParsedMeiXmlEvent> applyMeiTypedSpanNotationEvent(Collection<ParsedMeiXmlEvent> events,
            String notationName, String startId, String tstamp, String plist, String endId, String tstamp2,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick, int divisions, int beatType,
            int number) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        Integer startIndex = resolveXmlEventEndpointIndex(startId, tstamp, idToEventIndex, out, divisions, beatType,
                plist, idToEventTick);
        Integer endIndex = resolveXmlEventEndpointIndex(endId, tstamp2, idToEventIndex, out, divisions, beatType, null,
                idToEventTick);
        if (!isValidXmlEventIndex(out, startIndex) || !isValidXmlEventIndex(out, endIndex)) {
            return out;
        }
        ParsedMeiXmlEvent startEvent = out.get(startIndex.intValue());
        ParsedMeiXmlEvent endEvent = out.get(endIndex.intValue());
        if (!"rest".equals(startEvent.getKind())) {
            out.set(startIndex.intValue(),
                    startEvent.withXml(addTypedSpanNotationToEventXml(startEvent.getXml(), notationName, "start",
                            number)));
        }
        if (!"rest".equals(endEvent.getKind())) {
            out.set(endIndex.intValue(),
                    endEvent.withXml(addTypedSpanNotationToEventXml(endEvent.getXml(), notationName, "stop",
                            number)));
        }
        return out;
    }

    private static String addTypedSpanNotationToEventXml(String eventXml, String notationName, String type, int number) {
        if ("tuplet".equals(notationName)) {
            return addTupletNotationToEventXml(eventXml, type, number);
        }
        if ("glissando".equals(notationName)) {
            return addGlissNotationToEventXml(eventXml, type, number);
        }
        if ("slide".equals(notationName)) {
            return addSlideNotationToEventXml(eventXml, type, number);
        }
        return eventXml == null ? "" : eventXml;
    }

    private static List<Integer> resolveBeamSpanPlistIndexes(String plist, Map<String, Integer> idToEventIndex) {
        List<Integer> plistIndexes = new ArrayList<Integer>();
        String raw = plist == null ? "" : plist.trim();
        Map<String, Integer> indexMap = idToEventIndex == null ? Collections.<String, Integer>emptyMap()
                : idToEventIndex;
        if (raw.length() > 0) {
            for (String token : raw.split("\\s+")) {
                String id = token.startsWith("#") ? token.substring(1) : token;
                Integer index = indexMap.get(id);
                if (index != null) {
                    plistIndexes.add(index);
                }
            }
        }
        return plistIndexes;
    }

    private static List<Integer> resolveBeamSpanIndexes(int startIndex, int endIndex) {
        int from = Math.min(startIndex, endIndex);
        int to = Math.max(startIndex, endIndex);
        List<Integer> span = new ArrayList<Integer>();
        for (int index = from; index <= to; index++) {
            span.add(Integer.valueOf(index));
        }
        return span;
    }

    private static ParsedMeiXmlEvent applyTieCarryAccidentalsForEvent(ParsedMeiXmlEvent event,
            Map<String, Integer> tieCarryByPitch) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + event.getXml() + "</root>");
        if (doc == null || doc.getDocumentElement() == null) {
            return event;
        }
        Element root = doc.getDocumentElement();
        List<Element> notes = directChildElementsByName(root, "note");
        if (notes.isEmpty()) {
            return event;
        }
        boolean changed = false;
        for (Element note : notes) {
            Element pitch = firstDirectChild(note, "pitch");
            if (pitch == null) {
                continue;
            }
            Element stepNode = firstDirectChild(pitch, "step");
            Element octaveNode = firstDirectChild(pitch, "octave");
            String step = textOf(stepNode).trim().toUpperCase();
            int octave = parseIntSafe(textOf(octaveNode), Integer.MIN_VALUE);
            if (!isPitchStep(step) || octave == Integer.MIN_VALUE) {
                continue;
            }
            String pitchKey = step + ":" + octave;
            List<String> tieTypes = directTieTypes(note);
            boolean hasStart = tieTypes.contains("start");
            boolean hasStop = tieTypes.contains("stop");
            Element alterNode = firstDirectChild(pitch, "alter");
            if (hasStop && alterNode == null) {
                Integer carryAlter = tieCarryByPitch.get(pitchKey);
                if (carryAlter != null) {
                    Element newAlter = doc.createElement("alter");
                    newAlter.setTextContent(Integer.toString(carryAlter.intValue()));
                    if (octaveNode != null) {
                        pitch.insertBefore(newAlter, octaveNode);
                    } else {
                        pitch.appendChild(newAlter);
                    }
                    alterNode = newAlter;
                    changed = true;
                }
            }
            int resolvedAlter = parseIntSafe(textOf(alterNode), 0);
            if (hasStart) {
                tieCarryByPitch.put(pitchKey, Integer.valueOf(resolvedAlter));
            } else if (hasStop) {
                tieCarryByPitch.remove(pitchKey);
            }
        }
        return changed ? event.withXml(stripRootElement(MusicXmlIo.serializeMusicXmlDocument(doc))) : event;
    }

    private static List<String> directTieTypes(Element note) {
        List<String> out = new ArrayList<String>();
        NodeList children = note.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && "tie".equals(node.getNodeName())) {
                out.add((((Element) node).getAttribute("type") == null ? "" : ((Element) node).getAttribute("type"))
                        .trim().toLowerCase());
            }
        }
        return out;
    }

    private static List<Element> directChildElementsByName(Element parent, String name) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && name.equals(node.getNodeName())) {
                out.add((Element) node);
            }
        }
        return out;
    }

    private static List<Element> directChildElementsByLocalName(Element parent, String name) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && name.equals(localNameOf(node))) {
                out.add((Element) node);
            }
        }
        return out;
    }

    private static List<Element> directChildElements(Element parent) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                out.add((Element) node);
            }
        }
        return out;
    }

    private static List<Element> descendantElementsByLocalName(Element parent, String name) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (name.equals(localNameOf(element))) {
                    out.add(element);
                }
                out.addAll(descendantElementsByLocalName(element, name));
            }
        }
        return out;
    }

    private static Element findScoreDefStaffDef(Element scoreDef, String staffNo) {
        if (scoreDef == null) {
            return null;
        }
        String normalizedStaffNo = staffNo == null ? "" : staffNo.trim();
        List<Element> staffDefs = descendantElementsByLocalName(scoreDef, "staffDef");
        for (Element staffDef : staffDefs) {
            String n = staffDef.getAttribute("n") == null ? "" : staffDef.getAttribute("n").trim();
            if (n.equals(normalizedStaffNo)) {
                return staffDef;
            }
        }
        for (Element staffDef : staffDefs) {
            String n = staffDef.getAttribute("n") == null ? "" : staffDef.getAttribute("n").trim();
            if (n.length() == 0) {
                return staffDef;
            }
        }
        return null;
    }

    private static String attributeOrFallback(Element preferred, Element fallback, String name) {
        if (preferred != null && preferred.hasAttribute(name)) {
            return preferred.getAttribute(name);
        }
        return fallback == null ? "" : fallback.getAttribute(name);
    }

    private static String normalizeMeiMiscFieldName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() == 0) {
            return "";
        }
        if (name.startsWith("mks:")) {
            return name;
        }
        if (name.startsWith("src:") || name.startsWith("diag:")) {
            return "mks:" + name;
        }
        return "mks:src:mei:" + name;
    }

    private static Element parentElementByLocalName(Element element, String name) {
        if (element == null) {
            return null;
        }
        Node parent = element.getParentNode();
        return parent instanceof Element && name.equals(localNameOf(parent)) ? (Element) parent : null;
    }

    private static String localNameOf(Node node) {
        if (node == null) {
            return "";
        }
        String local = node.getLocalName();
        if (local != null && local.length() > 0) {
            return local;
        }
        String name = node.getNodeName() == null ? "" : node.getNodeName();
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private static Element firstDirectChild(Element parent, String name) {
        List<Element> children = directChildElementsByName(parent, name);
        return children.isEmpty() ? null : children.get(0);
    }

    private static Element firstDirectChildLocal(Element parent, String name) {
        List<Element> children = directChildElementsByLocalName(parent, name);
        return children.isEmpty() ? null : children.get(0);
    }

    private static String firstDirectChildText(Element parent, String name) {
        return textOf(firstDirectChildLocal(parent, name));
    }

    private static String firstNestedDirectChildText(Element parent, String childName, String grandchildName) {
        return firstDirectChildText(firstDirectChildLocal(parent, childName), grandchildName);
    }

    private static Element firstNestedDirectChild(Element parent, String childName) {
        return firstDirectChildLocal(parent, childName);
    }

    private static List<Element> directNotationChildren(Element note, String firstName, String secondName) {
        List<Element> out = new ArrayList<Element>();
        for (Element notations : directChildElementsByLocalName(note, "notations")) {
            for (Element child : directChildElements(notations)) {
                String name = localNameOf(child);
                if (name.equals(firstName) || name.equals(secondName)) {
                    out.add(child);
                }
            }
        }
        return out;
    }

    private static String buildMeiSpanControlXml(String tagName, String startTstamp, String endTstamp, String startId,
            String endId) {
        if (startId != null && startId.length() > 0 && endId != null && endId.length() > 0) {
            return "<" + tagName + " startid=\"#" + xmlEscape(startId) + "\" endid=\"#" + xmlEscape(endId)
                    + "\"/>";
        }
        return "<" + tagName + " tstamp=\"" + xmlEscape(startTstamp) + "\" tstamp2=\"" + xmlEscape(endTstamp)
                + "\"/>";
    }

    private static Element findMusicXmlMeasureByNumber(Element part, String number) {
        for (Element measure : directChildElementsByLocalName(part, "measure")) {
            String candidate = measure.getAttribute("number") == null ? "" : measure.getAttribute("number").trim();
            if (candidate.length() == 0) {
                continue;
            }
            if (candidate.equals(number)) {
                return measure;
            }
        }
        return null;
    }

    private static Map<String, List<Element>> collectMusicXmlNotesByVoiceForStaff(Element measure, int localStaff) {
        Map<String, List<Element>> voiceMap = new HashMap<String, List<Element>>();
        for (Element note : directChildElementsByLocalName(measure, "note")) {
            int staffNo = parseIntSafe(firstDirectChildText(note, "staff"), 1);
            if (staffNo != localStaff) {
                continue;
            }
            String voice = firstDirectChildText(note, "voice").trim();
            if (voice.length() == 0) {
                voice = "1";
            }
            if (!voiceMap.containsKey(voice)) {
                voiceMap.put(voice, new ArrayList<Element>());
            }
            voiceMap.get(voice).add(note);
        }
        return voiceMap;
    }

    private static void appendMeiExportControlNodes(List<String> measureLines, List<String> measureControlNodes,
            Collection<String> nodes, int globalStaff) {
        if (nodes == null) {
            return;
        }
        for (String node : nodes) {
            if (node == null || node.length() == 0) {
                continue;
            }
            measureLines.add(node);
            measureControlNodes.add(withStaffAttr(node, globalStaff));
        }
    }

    private static int compareMusicXmlVoice(String left, String right) {
        int leftInt = parseIntSafe(left, Integer.MIN_VALUE);
        int rightInt = parseIntSafe(right, Integer.MIN_VALUE);
        if (leftInt != Integer.MIN_VALUE && rightInt != Integer.MIN_VALUE) {
            return leftInt < rightInt ? -1 : (leftInt == rightInt ? 0 : 1);
        }
        String safeLeft = left == null ? "" : left;
        String safeRight = right == null ? "" : right;
        return safeLeft.compareTo(safeRight);
    }

    private static String joinStrings(Collection<String> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                out.append(value == null ? "" : value);
            }
        }
        return out.toString();
    }

    private static Integer parseOptionalInt(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Element findDirectMusicXmlPartById(Document doc, String partId) {
        if (doc == null || doc.getDocumentElement() == null) {
            return null;
        }
        String safePartId = partId == null ? "" : partId.trim();
        for (Element part : directChildElementsByLocalName(doc.getDocumentElement(), "part")) {
            String id = part.getAttribute("id") == null ? "" : part.getAttribute("id").trim();
            if (id.equals(safePartId)) {
                return part;
            }
        }
        return null;
    }

    private static boolean hasDirectBarlineChild(Element measure, String location, String childName, String attrName,
            String attrValue) {
        for (Element barline : directChildElementsByLocalName(measure, "barline")) {
            String loc = barline.getAttribute("location") == null ? "" : barline.getAttribute("location").trim();
            if (!location.equals(loc)) {
                continue;
            }
            for (Element child : directChildElementsByLocalName(barline, childName)) {
                String value = child.getAttribute(attrName) == null ? "" : child.getAttribute(attrName).trim();
                if (attrValue.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasDirectBarlineText(Element measure, String location, String childName, String text) {
        for (Element barline : directChildElementsByLocalName(measure, "barline")) {
            String loc = barline.getAttribute("location") == null ? "" : barline.getAttribute("location").trim();
            if (!location.equals(loc)) {
                continue;
            }
            for (Element child : directChildElementsByLocalName(barline, childName)) {
                if (text.equals(textOf(child).trim().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Integer parsePositiveEndingStopNumber(Element measure) {
        for (Element barline : directChildElementsByLocalName(measure, "barline")) {
            String loc = barline.getAttribute("location") == null ? "" : barline.getAttribute("location").trim();
            if (!"right".equals(loc)) {
                continue;
            }
            for (Element ending : directChildElementsByLocalName(barline, "ending")) {
                String type = ending.getAttribute("type") == null ? "" : ending.getAttribute("type").trim();
                if (!"stop".equals(type)) {
                    continue;
                }
                int number = parseIntSafe(ending.getAttribute("number"), Integer.MIN_VALUE);
                return number == Integer.MIN_VALUE ? null : Integer.valueOf(number);
            }
        }
        return null;
    }

    private static boolean hasDirectTupletType(Element note, String type) {
        for (Element notations : directChildElementsByLocalName(note, "notations")) {
            for (Element tuplet : directChildElementsByLocalName(notations, "tuplet")) {
                String value = tuplet.getAttribute("type") == null ? "" : tuplet.getAttribute("type").trim();
                if (type.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasDirectChildLocal(Element parent, String name) {
        return firstDirectChildLocal(parent, name) != null;
    }

    private static boolean isGraceSlash(Element note) {
        Element grace = firstDirectChildLocal(note, "grace");
        String slash = grace == null || grace.getAttribute("slash") == null ? ""
                : grace.getAttribute("slash").trim().toLowerCase();
        return "yes".equals(slash);
    }

    private static String ensureGeneratedNoteId(Map<Element, String> noteIdBySource, Element note, int[] nextGeneratedNoteId) {
        String existing = noteIdBySource.get(note);
        if (existing != null) {
            return existing;
        }
        String next = "mkN" + nextGeneratedNoteId[0];
        nextGeneratedNoteId[0]++;
        noteIdBySource.put(note, next);
        return next;
    }

    private static String buildSimpleMeiChordMember(Element note, String xmlId) {
        Element pitch = firstDirectChildLocal(note, "pitch");
        String step = firstDirectChildText(pitch, "step").trim();
        if (step.length() == 0) {
            step = "C";
        }
        String octaveText = firstDirectChildText(pitch, "octave").trim();
        if (octaveText.length() == 0) {
            octaveText = "4";
        }
        String explicitAccid = musicXmlAccidentalToAccid(firstDirectChildText(note, "accidental"));
        String accid = explicitAccid == null ? alterToAccid(firstDirectChildText(pitch, "alter")) : explicitAccid;
        List<String> noteAttrs = new ArrayList<String>();
        if (xmlId != null && xmlId.trim().length() > 0) {
            noteAttrs.add("xml:id=\"" + xmlEscape(xmlId.trim()) + "\"");
        }
        noteAttrs.add("pname=\"" + xmlEscape(toPname(step)) + "\"");
        noteAttrs.add("oct=\"" + xmlEscape(octaveText) + "\"");
        if (accid != null && accid.length() > 0) {
            noteAttrs.add("accid=\"" + xmlEscape(accid) + "\"");
        }
        String tieAttr = extractMeiTieFromMusicXmlTieTypes(directTieTypes(note));
        if (tieAttr.length() > 0) {
            noteAttrs.add("tie=\"" + xmlEscape(tieAttr) + "\"");
        }
        String articulationXml = buildMeiArticulationChildren(extractMeiArticulationTokensFromMusicXmlNote(note));
        MeiLyric lyric = extractMusicXmlLyric(note);
        StringBuilder body = new StringBuilder();
        if (lyric != null) {
            String wordpos = lyricWordposFromSyllabic(lyric.getSyllabic());
            body.append("<verse n=\"1\"><syl");
            if (wordpos.length() > 0) {
                body.append(" wordpos=\"").append(xmlEscape(wordpos)).append("\"");
            }
            body.append(">").append(xmlEscape(lyric.getText())).append("</syl></verse>");
        }
        body.append(articulationXml);
        return "<note " + joinSpace(noteAttrs) + (body.length() == 0 ? "/>" : ">" + body.toString() + "</note>");
    }

    private static String joinSemicolon(Collection<String> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (out.length() > 0) {
                    out.append(";");
                }
                out.append(value == null ? "" : value);
            }
        }
        return out.toString();
    }

    private static String joinSpace(Collection<String> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (out.length() > 0) {
                    out.append(" ");
                }
                out.append(value == null ? "" : value);
            }
        }
        return out.toString();
    }

    private static String textOf(Element element) {
        return element == null || element.getTextContent() == null ? "" : element.getTextContent();
    }

    private static String stripRootElement(String serialized) {
        String xml = serialized == null ? "" : serialized;
        if (xml.startsWith("<root>") && xml.endsWith("</root>")) {
            return xml.substring("<root>".length(), xml.length() - "</root>".length());
        }
        return xml;
    }

    private static int joinLength(Collection<String> values) {
        int length = 0;
        if (values != null) {
            for (String value : values) {
                length += value == null ? 0 : value.length();
            }
        }
        return length;
    }

    private static String zeroPad(int value, int width) {
        String text = Integer.toString(Math.max(0, value));
        while (text.length() < width) {
            text = "0" + text;
        }
        return text;
    }

    private static boolean isTruthyText(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }

    private static String placementAttributeXml(String placement) {
        String normalized = placement == null ? "" : placement.trim().toLowerCase();
        if ("above".equals(normalized) || "below".equals(normalized)) {
            return " placement=\"" + xmlEscape(normalized) + "\"";
        }
        return "";
    }

    private static String meiPlaceAttribute(String placement) {
        String normalized = placement == null ? "" : placement.trim().toLowerCase();
        return "above".equals(normalized) || "below".equals(normalized)
                ? " place=\"" + xmlEscape(normalized) + "\"" : "";
    }

    private static Element firstDirectionTypeChild(Element direction, String name) {
        for (Element directionType : directChildElementsByLocalName(direction, "direction-type")) {
            Element child = firstDirectChildLocal(directionType, name);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private static String firstDirectionTypeText(Element direction, String name) {
        Element child = firstDirectionTypeChild(direction, name);
        return child == null ? "" : textOf(child);
    }

    private static String firstElementChildLocalName(Element parent) {
        if (parent == null) {
            return "";
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                return localNameOf(child);
            }
        }
        return "";
    }

    private static void appendMeiHairpinControls(List<String> out, List<Element> directions, int sourceDivisions,
            int beatType, Map<Element, Integer> onsetTicksByDirection) {
        Map<String, String[]> pendingByNumber = new HashMap<String, String[]>();
        for (Element direction : directions) {
            Element wedge = firstDirectionTypeChild(direction, "wedge");
            if (wedge == null) {
                continue;
            }
            String type = wedge.getAttribute("type") == null ? "" : wedge.getAttribute("type").trim().toLowerCase();
            String number = wedge.getAttribute("number") == null || wedge.getAttribute("number").trim().length() == 0
                    ? "1" : wedge.getAttribute("number").trim();
            String tstamp = directionTstamp(direction, sourceDivisions, beatType, onsetTicksByDirection);
            String placeAttr = meiPlaceAttribute(direction.getAttribute("placement"));
            if ("crescendo".equals(type) || "diminuendo".equals(type)) {
                pendingByNumber.put(number, new String[] { tstamp, "diminuendo".equals(type) ? "dim" : "cres",
                        placeAttr });
                continue;
            }
            if ("stop".equals(type)) {
                String[] pending = pendingByNumber.get(number);
                if (pending == null) {
                    continue;
                }
                out.add("<hairpin form=\"" + pending[1] + "\" tstamp=\"" + xmlEscape(pending[0])
                        + "\" tstamp2=\"" + xmlEscape(tstamp) + "\"" + pending[2] + "/>");
                pendingByNumber.remove(number);
            }
        }
        for (String[] pending : pendingByNumber.values()) {
            out.add("<hairpin form=\"" + pending[1] + "\" tstamp=\"" + xmlEscape(pending[0]) + "\""
                    + pending[2] + "/>");
        }
    }

    private static void appendMeiPedalControls(List<String> out, List<Element> directions, int sourceDivisions,
            int beatType, Map<Element, Integer> onsetTicksByDirection) {
        Map<String, String[]> pendingByNumber = new HashMap<String, String[]>();
        for (Element direction : directions) {
            Element pedal = firstDirectionTypeChild(direction, "pedal");
            if (pedal == null) {
                continue;
            }
            String type = pedal.getAttribute("type") == null ? "" : pedal.getAttribute("type").trim().toLowerCase();
            String number = pedal.getAttribute("number") == null || pedal.getAttribute("number").trim().length() == 0
                    ? "1" : pedal.getAttribute("number").trim();
            String tstamp = directionTstamp(direction, sourceDivisions, beatType, onsetTicksByDirection);
            String placeAttr = meiPlaceAttribute(direction.getAttribute("placement"));
            if ("start".equals(type) || "resume".equals(type) || "change".equals(type)) {
                pendingByNumber.put(number, new String[] { tstamp, placeAttr });
                continue;
            }
            if ("stop".equals(type) || "discontinue".equals(type)) {
                String[] pending = pendingByNumber.get(number);
                if (pending != null) {
                    out.add("<pedal tstamp=\"" + xmlEscape(pending[0]) + "\" tstamp2=\"" + xmlEscape(tstamp)
                            + "\"" + pending[1] + "/>");
                    pendingByNumber.remove(number);
                } else {
                    out.add("<pedal tstamp=\"" + xmlEscape(tstamp) + "\" type=\"stop\"" + placeAttr + "/>");
                }
            }
        }
        for (String[] pending : pendingByNumber.values()) {
            out.add("<pedal tstamp=\"" + xmlEscape(pending[0]) + "\"" + pending[1] + "/>");
        }
    }

    private static void appendMeiOctaveControls(List<String> out, List<Element> directions, int sourceDivisions,
            int beatType, Map<Element, Integer> onsetTicksByDirection) {
        Map<String, String[]> pendingByNumber = new HashMap<String, String[]>();
        for (Element direction : directions) {
            Element octave = firstDirectionTypeChild(direction, "octave-shift");
            if (octave == null) {
                continue;
            }
            String type = octave.getAttribute("type") == null ? "" : octave.getAttribute("type").trim().toLowerCase();
            String number = octave.getAttribute("number") == null || octave.getAttribute("number").trim().length() == 0
                    ? "1" : octave.getAttribute("number").trim();
            int dis = Math.max(1, parseIntSafe(octave.getAttribute("size"), 8));
            String disPlace = "down".equals(type) ? "below" : "above";
            String tstamp = directionTstamp(direction, sourceDivisions, beatType, onsetTicksByDirection);
            String placeAttr = meiPlaceAttribute(direction.getAttribute("placement"));
            if ("up".equals(type) || "down".equals(type)) {
                pendingByNumber.put(number, new String[] { tstamp, Integer.toString(dis), disPlace, placeAttr });
                continue;
            }
            if ("stop".equals(type) || "continue".equals(type)) {
                String[] pending = pendingByNumber.get(number);
                if (pending != null) {
                    out.add("<octave dis=\"" + pending[1] + "\" dis.place=\"" + pending[2] + "\" tstamp=\""
                            + xmlEscape(pending[0]) + "\" tstamp2=\"" + xmlEscape(tstamp) + "\"" + pending[3]
                            + "/>");
                    pendingByNumber.remove(number);
                } else {
                    out.add("<octave dis=\"" + dis + "\" tstamp=\"" + xmlEscape(tstamp) + "\" type=\"stop\""
                            + placeAttr + "/>");
                }
            }
        }
        for (String[] pending : pendingByNumber.values()) {
            out.add("<octave dis=\"" + pending[1] + "\" dis.place=\"" + pending[2] + "\" tstamp=\""
                    + xmlEscape(pending[0]) + "\"" + pending[3] + "/>");
        }
    }

    private static void appendMeiRepeatMarkControls(List<String> out, List<Element> directions, int sourceDivisions,
            int beatType, Map<Element, Integer> onsetTicksByDirection) {
        for (Element direction : directions) {
            String tstamp = directionTstamp(direction, sourceDivisions, beatType, onsetTicksByDirection);
            String placeAttr = meiPlaceAttribute(direction.getAttribute("placement"));
            if (firstDirectionTypeChild(direction, "segno") != null) {
                out.add("<repeatMark tstamp=\"" + xmlEscape(tstamp) + "\"" + placeAttr + ">segno</repeatMark>");
                continue;
            }
            if (firstDirectionTypeChild(direction, "coda") != null) {
                out.add("<repeatMark tstamp=\"" + xmlEscape(tstamp) + "\"" + placeAttr + ">coda</repeatMark>");
                continue;
            }
            String words = firstDirectionTypeText(direction, "words").trim();
            String lowered = words.toLowerCase();
            if ("fine".equals(lowered) || "d.c.".equals(lowered) || "da capo".equals(lowered)
                    || "d.s.".equals(lowered) || "dal segno".equals(lowered)) {
                out.add("<repeatMark tstamp=\"" + xmlEscape(tstamp) + "\"" + placeAttr + ">" + xmlEscape(words)
                        + "</repeatMark>");
            }
        }
    }

    private static String meiPlacement(Element element) {
        if (element == null) {
            return "";
        }
        String normalized = firstNonEmpty(element.getAttribute("place"), element.getAttribute("placement"), "")
                .trim().toLowerCase();
        return "above".equals(normalized) || "below".equals(normalized) ? normalized : "";
    }

    private static String meiSemantic(Element element) {
        if (element == null) {
            return "";
        }
        return firstNonEmpty(element.getAttribute("type"), element.getAttribute("state"),
                firstNonEmpty(element.getAttribute("func"), element.getAttribute("val"), ""));
    }

    private static String offsetXml(Integer tick) {
        return tick != null && tick.intValue() > 0 ? "<offset>" + tick.intValue() + "</offset>" : "";
    }

    private static void appendIfPresent(StringBuilder out, String value) {
        if (value != null && value.length() > 0) {
            out.append(value);
        }
    }

    private static String buildMusicXmlPedalDirectionXml(String type, String placementAttr, String offsetXml,
            String voice, String staffNo) {
        return "<direction" + placementAttr + "><direction-type><pedal type=\"" + xmlEscape(type)
                + "\" number=\"1\" line=\"yes\"/></direction-type>" + offsetXml + "<voice>" + xmlEscape(voice)
                + "</voice><staff>" + xmlEscape(staffNo) + "</staff></direction>";
    }

    private static String buildMusicXmlOctaveDirectionXml(String type, int size, String placementAttr, String offsetXml,
            String voice, String staffNo) {
        return "<direction" + placementAttr + "><direction-type><octave-shift type=\"" + xmlEscape(type)
                + "\" size=\"" + Math.max(1, size) + "\" number=\"1\"/></direction-type>" + offsetXml
                + "<voice>" + xmlEscape(voice) + "</voice><staff>" + xmlEscape(staffNo) + "</staff></direction>";
    }

    private static double parsePositiveDouble(String value) {
        try {
            double parsed = Double.parseDouble(value == null ? "" : value.trim());
            return Double.isFinite(parsed) && parsed > 0.0d ? parsed : 0.0d;
        } catch (NumberFormatException ex) {
            return 0.0d;
        }
    }

    private static String formatTempo(double bpm) {
        String formatted = String.format(java.util.Locale.ROOT, "%.2f", bpm);
        return formatted.endsWith(".00") ? formatted.substring(0, formatted.length() - 3) : formatted;
    }

    private static String formatInferredMeiTempo(double bpm) {
        String formatted = formatTempo(bpm);
        return formatted.indexOf('.') >= 0 && formatted.endsWith("0")
                ? formatted.substring(0, formatted.length() - 1) : formatted;
    }

    private static String firstNonEmpty(String first, String second, String third) {
        String one = first == null ? "" : first.trim();
        if (one.length() > 0) {
            return one;
        }
        String two = second == null ? "" : second.trim();
        if (two.length() > 0) {
            return two;
        }
        return third == null ? "" : third.trim();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.length() > 0) {
                return normalized;
            }
        }
        return "";
    }

    private static void addMeiArticulationTokens(Set<String> tokens, String raw) {
        if (tokens == null) {
            return;
        }
        String value = raw == null ? "" : raw.trim().toLowerCase();
        if (value.length() == 0) {
            return;
        }
        for (String token : value.split("\\s+")) {
            if (token != null && token.trim().length() > 0) {
                tokens.add(token.trim());
            }
        }
    }

    private static Integer parseBreaksecFromMeiNode(Element node) {
        int breaksec = parseIntSafe(node == null ? null : node.getAttribute("breaksec"), Integer.MIN_VALUE);
        return breaksec > 0 ? Integer.valueOf(breaksec) : null;
    }

    private static boolean canConnectMeiBeam(int leftPitchedIndex, int level, List<Integer> pitchedIndexes,
            List<Integer> depths, List<ParsedMeiXmlEvent> events) {
        if (leftPitchedIndex < 0 || leftPitchedIndex >= pitchedIndexes.size() - 1) {
            return false;
        }
        if (depths.get(leftPitchedIndex).intValue() < level || depths.get(leftPitchedIndex + 1).intValue() < level) {
            return false;
        }
        ParsedMeiXmlEvent leftEvent = events.get(pitchedIndexes.get(leftPitchedIndex).intValue());
        Integer keep = leftEvent == null ? null : leftEvent.getBreaksecAfter();
        return keep == null || keep.intValue() >= level;
    }

    private static String addSimpleTypedNumberNotationToEventXml(String eventXml, final String tagName,
            final String type, final int number) {
        return rewriteFirstNoteInEventXml(eventXml, new NoteXmlRewriter() {
            public String rewrite(String noteXml) {
                String xml = "<" + tagName + " type=\"" + xmlEscape(type) + "\" number=\"" + Math.max(1, number)
                        + "\"/>";
                return addNotationXmlToSingleNoteXml(noteXml, xml);
            }
        });
    }

    private static String rewriteFirstNoteInEventXml(String eventXml, NoteXmlRewriter rewriter) {
        String source = eventXml == null ? "" : eventXml;
        int firstNoteStart = source.indexOf("<note>");
        if (firstNoteStart < 0) {
            return source;
        }
        int firstNoteEnd = source.indexOf("</note>", firstNoteStart);
        if (firstNoteEnd < 0) {
            return source;
        }
        int end = firstNoteEnd + "</note>".length();
        String before = source.substring(0, firstNoteStart);
        String noteBlock = source.substring(firstNoteStart, end);
        String after = source.substring(end);
        return before + rewriter.rewrite(noteBlock) + after;
    }

    private static String repeat(String token, int count) {
        if (token == null || token.length() == 0 || count <= 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < count; index++) {
            out.append(token);
        }
        return out.toString();
    }

    private static boolean isPitchStep(String step) {
        return step != null && step.length() == 1 && step.charAt(0) >= 'A' && step.charAt(0) <= 'G';
    }

    private interface NoteXmlRewriter {
        String rewrite(String noteXml);
    }

    public static final class HarmonyKindSuffix {
        private final String suffix;
        private final boolean fromText;

        public HarmonyKindSuffix(String suffix, boolean fromText) {
            this.suffix = suffix == null ? "" : suffix;
            this.fromText = fromText;
        }

        public String getSuffix() {
            return suffix;
        }

        public boolean isFromText() {
            return fromText;
        }
    }

    public static final class HarmonyDegree {
        private final int value;
        private final int alter;

        public HarmonyDegree(int value, int alter) {
            this.value = value;
            this.alter = alter;
        }

        public int getValue() {
            return value;
        }

        public int getAlter() {
            return alter;
        }
    }

    public static final class ParsedMeiHarmonyText {
        private final String rootStep;
        private final int rootAlter;
        private final String kind;
        private final String kindText;
        private final String bassStep;
        private final Integer bassAlter;
        private final List<HarmonyDegree> degrees;

        public ParsedMeiHarmonyText(String rootStep, int rootAlter, String kind, String kindText, String bassStep,
                Integer bassAlter, Collection<HarmonyDegree> degrees) {
            this.rootStep = rootStep == null ? "" : rootStep;
            this.rootAlter = rootAlter;
            this.kind = kind == null ? "other" : kind;
            this.kindText = kindText == null ? "" : kindText;
            this.bassStep = bassStep;
            this.bassAlter = bassAlter;
            this.degrees = degrees == null ? Collections.<HarmonyDegree>emptyList()
                    : Collections.unmodifiableList(new ArrayList<HarmonyDegree>(degrees));
        }

        public String getRootStep() {
            return rootStep;
        }

        public int getRootAlter() {
            return rootAlter;
        }

        public String getKind() {
            return kind;
        }

        public String getKindText() {
            return kindText;
        }

        public String getBassStep() {
            return bassStep;
        }

        public Integer getBassAlter() {
            return bassAlter;
        }

        public List<HarmonyDegree> getDegrees() {
            return degrees;
        }
    }

    public static final class MeiHarmonySource {
        private final int staffNo;
        private final String rootStep;
        private final Integer rootAlter;
        private final String kindText;
        private final String kindTextAttribute;
        private final String bassStep;
        private final Integer bassAlter;
        private final List<HarmonyDegree> degrees;
        private final int offsetTicks;

        public MeiHarmonySource(int staffNo, String rootStep, Integer rootAlter, String kindText,
                String kindTextAttribute, String bassStep, Integer bassAlter, Collection<HarmonyDegree> degrees,
                int offsetTicks) {
            this.staffNo = staffNo;
            this.rootStep = rootStep;
            this.rootAlter = rootAlter;
            this.kindText = kindText;
            this.kindTextAttribute = kindTextAttribute;
            this.bassStep = bassStep;
            this.bassAlter = bassAlter;
            this.degrees = degrees == null ? Collections.<HarmonyDegree>emptyList()
                    : Collections.unmodifiableList(new ArrayList<HarmonyDegree>(degrees));
            this.offsetTicks = offsetTicks;
        }

        public int getStaffNo() {
            return staffNo;
        }

        public String getRootStep() {
            return rootStep;
        }

        public Integer getRootAlter() {
            return rootAlter;
        }

        public String getKindText() {
            return kindText;
        }

        public String getKindTextAttribute() {
            return kindTextAttribute;
        }

        public String getBassStep() {
            return bassStep;
        }

        public Integer getBassAlter() {
            return bassAlter;
        }

        public List<HarmonyDegree> getDegrees() {
            return degrees;
        }

        public int getOffsetTicks() {
            return offsetTicks;
        }
    }

    public static final class MusicXmlStaffSlot {
        private final String partId;
        private final int localStaff;
        private final int globalStaff;
        private final String label;

        public MusicXmlStaffSlot(String partId, int localStaff, int globalStaff, String label) {
            this.partId = partId == null ? "" : partId;
            this.localStaff = Math.max(1, localStaff);
            this.globalStaff = Math.max(1, globalStaff);
            this.label = label == null ? "" : label;
        }

        public String getPartId() {
            return partId;
        }

        public int getLocalStaff() {
            return localStaff;
        }

        public int getGlobalStaff() {
            return globalStaff;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class MusicXmlStaffTimelineEntry {
        private final Element note;
        private final int onset;
        private final String noteId;

        public MusicXmlStaffTimelineEntry(Element note, int onset, String noteId) {
            this.note = note;
            this.onset = Math.max(0, onset);
            this.noteId = noteId;
        }

        public Element getNote() {
            return note;
        }

        public int getOnset() {
            return onset;
        }

        public String getNoteId() {
            return noteId;
        }
    }

    public static final class MeiExportTieSlurCarryState {
        private final Map<String, String[]> pendingSlurByNumber = new HashMap<String, String[]>();
        private final Map<String, String[]> pendingTieByPitch = new HashMap<String, String[]>();

        public Map<String, String[]> getPendingSlurByNumber() {
            return pendingSlurByNumber;
        }

        public Map<String, String[]> getPendingTieByPitch() {
            return pendingTieByPitch;
        }
    }

    public static final class MeiDurDots {
        private final String dur;
        private final int dots;

        public MeiDurDots(String dur, int dots) {
            this.dur = dur == null ? "4" : dur;
            this.dots = Math.max(0, dots);
        }

        public String getDur() {
            return dur;
        }

        public int getDots() {
            return dots;
        }
    }

    public static final class TieFlags {
        private final boolean start;
        private final boolean stop;

        public TieFlags(boolean start, boolean stop) {
            this.start = start;
            this.stop = stop;
        }

        public boolean isStart() {
            return start;
        }

        public boolean isStop() {
            return stop;
        }
    }

    public static final class MeiLyric {
        private final String text;
        private final String syllabic;

        public MeiLyric(String text, String syllabic) {
            this.text = text == null ? "" : text;
            this.syllabic = syllabic == null ? "" : syllabic;
        }

        public String getText() {
            return text;
        }

        public String getSyllabic() {
            return syllabic;
        }
    }

    public static final class MeiSoundingAccid {
        private final String visualAccid;
        private final String soundingAccid;

        public MeiSoundingAccid(String visualAccid, String soundingAccid) {
            this.visualAccid = visualAccid == null ? "" : visualAccid;
            this.soundingAccid = soundingAccid == null ? "" : soundingAccid;
        }

        public String getVisualAccid() {
            return visualAccid;
        }

        public String getSoundingAccid() {
            return soundingAccid;
        }
    }

    public static final class MeiForcedTuplet {
        private final int num;
        private final int numbase;

        public MeiForcedTuplet(int num, int numbase) {
            this.num = Math.max(1, num);
            this.numbase = Math.max(1, numbase);
        }

        public int getNum() {
            return num;
        }

        public int getNumbase() {
            return numbase;
        }
    }

    public static final class MeiSlurNotation {
        private final String type;
        private final int number;

        public MeiSlurNotation(String type, int number) {
            this.type = type == null ? "" : type;
            this.number = Math.max(1, number);
        }

        public String getType() {
            return type;
        }

        public int getNumber() {
            return number;
        }
    }

    public static final class ParsedMeiEvent {
        private final String kind;
        private final int durationTicks;

        public ParsedMeiEvent(String kind, int durationTicks) {
            this.kind = kind == null ? "" : kind;
            this.durationTicks = durationTicks;
        }

        public String getKind() {
            return kind;
        }

        public int getDurationTicks() {
            return durationTicks;
        }
    }

    public static final class ParsedMeiXmlEvent {
        private final String kind;
        private final int durationTicks;
        private final String xml;
        private final Integer beamDepth;
        private final Integer breaksecAfter;

        public ParsedMeiXmlEvent(String kind, int durationTicks, String xml) {
            this(kind, durationTicks, xml, null, null);
        }

        public ParsedMeiXmlEvent(String kind, int durationTicks, String xml, Integer beamDepth,
                Integer breaksecAfter) {
            this.kind = kind == null ? "" : kind;
            this.durationTicks = durationTicks;
            this.xml = xml == null ? "" : xml;
            this.beamDepth = beamDepth;
            this.breaksecAfter = breaksecAfter;
        }

        public String getKind() {
            return kind;
        }

        public int getDurationTicks() {
            return durationTicks;
        }

        public String getXml() {
            return xml;
        }

        public Integer getBeamDepth() {
            return beamDepth;
        }

        public Integer getBreaksecAfter() {
            return breaksecAfter;
        }

        public ParsedMeiXmlEvent withXml(String nextXml) {
            return new ParsedMeiXmlEvent(kind, durationTicks, nextXml, beamDepth, breaksecAfter);
        }
    }

    public static final class ParsedMeiLayer {
        private final List<ParsedMeiXmlEvent> events;
        private final Map<String, Integer> idToEventIndex;
        private final Map<String, Integer> tieCarryOut;

        public ParsedMeiLayer(Collection<ParsedMeiXmlEvent> events, Map<String, Integer> idToEventIndex,
                Map<String, Integer> tieCarryOut) {
            this.events = events == null ? Collections.<ParsedMeiXmlEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ParsedMeiXmlEvent>(events));
            this.idToEventIndex = idToEventIndex == null ? Collections.<String, Integer>emptyMap()
                    : Collections.unmodifiableMap(new HashMap<String, Integer>(idToEventIndex));
            this.tieCarryOut = tieCarryOut == null ? Collections.<String, Integer>emptyMap()
                    : Collections.unmodifiableMap(new HashMap<String, Integer>(tieCarryOut));
        }

        public List<ParsedMeiXmlEvent> getEvents() {
            return events;
        }

        public Map<String, Integer> getIdToEventIndex() {
            return idToEventIndex;
        }

        public Map<String, Integer> getTieCarryOut() {
            return tieCarryOut;
        }
    }

    public static final class MeiLayerTrimResult {
        private final List<ParsedMeiXmlEvent> events;
        private final int totalTicks;
        private final int droppedCount;
        private final int droppedTicks;
        private final int trimmedCount;
        private final int trimmedTicks;

        public MeiLayerTrimResult(Collection<ParsedMeiXmlEvent> events, int totalTicks, int droppedCount,
                int droppedTicks, int trimmedCount, int trimmedTicks) {
            this.events = events == null ? Collections.<ParsedMeiXmlEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ParsedMeiXmlEvent>(events));
            this.totalTicks = totalTicks;
            this.droppedCount = droppedCount;
            this.droppedTicks = droppedTicks;
            this.trimmedCount = trimmedCount;
            this.trimmedTicks = trimmedTicks;
        }

        public List<ParsedMeiXmlEvent> getEvents() {
            return events;
        }

        public int getTotalTicks() {
            return totalTicks;
        }

        public int getDroppedCount() {
            return droppedCount;
        }

        public int getDroppedTicks() {
            return droppedTicks;
        }

        public int getTrimmedCount() {
            return trimmedCount;
        }

        public int getTrimmedTicks() {
            return trimmedTicks;
        }
    }

    public static final class MiscField {
        private final String name;
        private final String value;

        public MiscField(String name, String value) {
            this.name = name == null ? "" : name;
            this.value = value == null ? "" : value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public static final class MeiLayerXml {
        private final String xml;
        private final int totalTicks;

        public MeiLayerXml(String xml, int totalTicks) {
            this.xml = xml == null ? "" : xml;
            this.totalTicks = Math.max(0, totalTicks);
        }

        public String getXml() {
            return xml;
        }

        public int getTotalTicks() {
            return totalTicks;
        }
    }

    public static final class MeiProcessedLayerXml {
        private final String voice;
        private final String xml;
        private final int totalTicks;
        private final int sourceTotalTicks;
        private final int droppedCount;
        private final int droppedTicks;
        private final int trimmedCount;
        private final int trimmedTicks;
        private final Map<String, Integer> tieCarryOut;

        public MeiProcessedLayerXml(String voice, String xml, int totalTicks, int sourceTotalTicks, int droppedCount,
                int droppedTicks, int trimmedCount, int trimmedTicks, Map<String, Integer> tieCarryOut) {
            this.voice = voice == null ? "" : voice;
            this.xml = xml == null ? "" : xml;
            this.totalTicks = Math.max(0, totalTicks);
            this.sourceTotalTicks = Math.max(0, sourceTotalTicks);
            this.droppedCount = Math.max(0, droppedCount);
            this.droppedTicks = Math.max(0, droppedTicks);
            this.trimmedCount = Math.max(0, trimmedCount);
            this.trimmedTicks = Math.max(0, trimmedTicks);
            this.tieCarryOut = tieCarryOut == null ? Collections.<String, Integer>emptyMap()
                    : Collections.unmodifiableMap(new HashMap<String, Integer>(tieCarryOut));
        }

        public String getVoice() {
            return voice;
        }

        public String getXml() {
            return xml;
        }

        public int getTotalTicks() {
            return totalTicks;
        }

        public int getSourceTotalTicks() {
            return sourceTotalTicks;
        }

        public int getDroppedCount() {
            return droppedCount;
        }

        public int getDroppedTicks() {
            return droppedTicks;
        }

        public int getTrimmedCount() {
            return trimmedCount;
        }

        public int getTrimmedTicks() {
            return trimmedTicks;
        }

        public Map<String, Integer> getTieCarryOut() {
            return tieCarryOut;
        }
    }

    public static final class MeiProcessedStaffLayers {
        private final List<MeiProcessedLayerXml> layers;
        private final String bodyXml;
        private final int maxLayerTicks;
        private final Map<String, Integer> staffIdToEventTick;
        private final Map<String, Map<String, Integer>> tieCarryByVoice;

        public MeiProcessedStaffLayers(Collection<MeiProcessedLayerXml> layers, String bodyXml, int maxLayerTicks,
                Map<String, Integer> staffIdToEventTick, Map<String, Map<String, Integer>> tieCarryByVoice) {
            this.layers = layers == null ? Collections.<MeiProcessedLayerXml>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MeiProcessedLayerXml>(layers));
            this.bodyXml = bodyXml == null ? "" : bodyXml;
            this.maxLayerTicks = Math.max(0, maxLayerTicks);
            this.staffIdToEventTick = staffIdToEventTick == null ? Collections.<String, Integer>emptyMap()
                    : Collections.unmodifiableMap(new HashMap<String, Integer>(staffIdToEventTick));
            Map<String, Map<String, Integer>> carry = new HashMap<String, Map<String, Integer>>();
            if (tieCarryByVoice != null) {
                for (Map.Entry<String, Map<String, Integer>> entry : tieCarryByVoice.entrySet()) {
                    carry.put(entry.getKey(), entry.getValue() == null ? Collections.<String, Integer>emptyMap()
                            : Collections.unmodifiableMap(new HashMap<String, Integer>(entry.getValue())));
                }
            }
            this.tieCarryByVoice = Collections.unmodifiableMap(carry);
        }

        public List<MeiProcessedLayerXml> getLayers() {
            return layers;
        }

        public String getBodyXml() {
            return bodyXml;
        }

        public int getMaxLayerTicks() {
            return maxLayerTicks;
        }

        public Map<String, Integer> getStaffIdToEventTick() {
            return staffIdToEventTick;
        }

        public Map<String, Map<String, Integer>> getTieCarryByVoice() {
            return tieCarryByVoice;
        }
    }

    public static final class MeiTieCarryResult {
        private final List<ParsedMeiXmlEvent> events;
        private final Map<String, Integer> tieCarryOut;

        public MeiTieCarryResult(Collection<ParsedMeiXmlEvent> events, Map<String, Integer> tieCarryOut) {
            this.events = events == null ? Collections.<ParsedMeiXmlEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ParsedMeiXmlEvent>(events));
            this.tieCarryOut = tieCarryOut == null ? Collections.<String, Integer>emptyMap()
                    : Collections.unmodifiableMap(new HashMap<String, Integer>(tieCarryOut));
        }

        public List<ParsedMeiXmlEvent> getEvents() {
            return events;
        }

        public Map<String, Integer> getTieCarryOut() {
            return tieCarryOut;
        }
    }

    public static final class ResolvedMeiImportOptions {
        private final boolean debugMetadata;
        private final boolean sourceMetadata;
        private final boolean failOnOverfullDrop;
        private final Integer meiCorpusIndex;

        public ResolvedMeiImportOptions(boolean debugMetadata, boolean sourceMetadata, boolean failOnOverfullDrop,
                Integer meiCorpusIndex) {
            this.debugMetadata = debugMetadata;
            this.sourceMetadata = sourceMetadata;
            this.failOnOverfullDrop = failOnOverfullDrop;
            this.meiCorpusIndex = meiCorpusIndex;
        }

        public boolean isDebugMetadata() {
            return debugMetadata;
        }

        public boolean isSourceMetadata() {
            return sourceMetadata;
        }

        public boolean isFailOnOverfullDrop() {
            return failOnOverfullDrop;
        }

        public Integer getMeiCorpusIndex() {
            return meiCorpusIndex;
        }
    }

    public static final class MeiInitialImportContext {
        private final String title;
        private final List<Element> scoreDefs;
        private final List<Element> staffDefs;
        private final int meterCount;
        private final int meterUnit;
        private final int fifths;
        private final int divisions;
        private final Map<String, MeiStaffMeta> staffMeta;
        private final List<Element> measureNodes;
        private final List<String> staffNumbers;

        public MeiInitialImportContext(String title, Collection<Element> scoreDefs, Collection<Element> staffDefs,
                int meterCount, int meterUnit, int fifths, int divisions, Map<String, MeiStaffMeta> staffMeta,
                Collection<Element> measureNodes, Collection<String> staffNumbers) {
            this.title = title == null ? "" : title;
            this.scoreDefs = scoreDefs == null ? Collections.<Element>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Element>(scoreDefs));
            this.staffDefs = staffDefs == null ? Collections.<Element>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Element>(staffDefs));
            this.meterCount = Math.max(1, meterCount);
            this.meterUnit = Math.max(1, meterUnit);
            this.fifths = fifths;
            this.divisions = Math.max(1, divisions);
            this.staffMeta = staffMeta == null ? Collections.<String, MeiStaffMeta>emptyMap()
                    : Collections.unmodifiableMap(new HashMap<String, MeiStaffMeta>(staffMeta));
            this.measureNodes = measureNodes == null ? Collections.<Element>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Element>(measureNodes));
            this.staffNumbers = staffNumbers == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(staffNumbers));
        }

        public String getTitle() {
            return title;
        }

        public List<Element> getScoreDefs() {
            return scoreDefs;
        }

        public List<Element> getStaffDefs() {
            return staffDefs;
        }

        public int getMeterCount() {
            return meterCount;
        }

        public int getMeterUnit() {
            return meterUnit;
        }

        public int getFifths() {
            return fifths;
        }

        public int getDivisions() {
            return divisions;
        }

        public Map<String, MeiStaffMeta> getStaffMeta() {
            return staffMeta;
        }

        public List<Element> getMeasureNodes() {
            return measureNodes;
        }

        public List<String> getStaffNumbers() {
            return staffNumbers;
        }
    }

    public static final class MeiPartImportState {
        private final String staffNo;
        private final String partId;
        private final String label;
        private final int currentBeats;
        private final int currentBeatType;
        private final String currentTimeSymbol;
        private final int currentFifths;
        private final String currentClefSign;
        private final int currentClefLine;
        private final MeiTranspose currentTranspose;
        private final boolean hasEmittedInitialAttributes;

        public MeiPartImportState(String staffNo, String partId, String label, int currentBeats, int currentBeatType,
                String currentTimeSymbol, int currentFifths, String currentClefSign, int currentClefLine,
                MeiTranspose currentTranspose, boolean hasEmittedInitialAttributes) {
            this.staffNo = staffNo == null ? "" : staffNo;
            this.partId = partId == null ? "" : partId;
            this.label = label == null ? "" : label;
            this.currentBeats = Math.max(1, currentBeats);
            this.currentBeatType = Math.max(1, currentBeatType);
            this.currentTimeSymbol = currentTimeSymbol;
            this.currentFifths = currentFifths;
            this.currentClefSign = currentClefSign == null || currentClefSign.length() == 0 ? "G" : currentClefSign;
            this.currentClefLine = Math.max(1, currentClefLine);
            this.currentTranspose = currentTranspose;
            this.hasEmittedInitialAttributes = hasEmittedInitialAttributes;
        }

        public String getStaffNo() {
            return staffNo;
        }

        public String getPartId() {
            return partId;
        }

        public String getLabel() {
            return label;
        }

        public int getCurrentBeats() {
            return currentBeats;
        }

        public int getCurrentBeatType() {
            return currentBeatType;
        }

        public String getCurrentTimeSymbol() {
            return currentTimeSymbol;
        }

        public int getCurrentFifths() {
            return currentFifths;
        }

        public String getCurrentClefSign() {
            return currentClefSign;
        }

        public int getCurrentClefLine() {
            return currentClefLine;
        }

        public MeiTranspose getCurrentTranspose() {
            return currentTranspose;
        }

        public boolean hasEmittedInitialAttributes() {
            return hasEmittedInitialAttributes;
        }
    }

    public static final class MeiMeasureImportState {
        private final MeiPartImportState previousPartState;
        private final String sourceMeasureNo;
        private final Element targetStaff;
        private final boolean hasTargetStaff;
        private final Element effectiveScoreDef;
        private final Element effectiveStaffDef;
        private final MeiMeasureMeta measureMeta;
        private final String measureNo;
        private final boolean implicitFromMeta;
        private final int measureBeats;
        private final int measureBeatType;
        private final String measureTimeSymbol;
        private final int measureFifths;
        private final String measureClefSign;
        private final int measureClefLine;
        private final MeiTranspose measureTranspose;
        private final int measureTicks;
        private final boolean shouldEmitTime;
        private final boolean shouldEmitKey;
        private final boolean shouldEmitTranspose;
        private final boolean shouldEmitClef;

        public MeiMeasureImportState(MeiPartImportState previousPartState, String sourceMeasureNo, Element targetStaff,
                boolean hasTargetStaff, Element effectiveScoreDef, Element effectiveStaffDef, MeiMeasureMeta measureMeta,
                String measureNo, boolean implicitFromMeta, int measureBeats, int measureBeatType,
                String measureTimeSymbol, int measureFifths, String measureClefSign, int measureClefLine,
                MeiTranspose measureTranspose, int measureTicks, boolean shouldEmitTime, boolean shouldEmitKey,
                boolean shouldEmitTranspose, boolean shouldEmitClef) {
            this.previousPartState = previousPartState;
            this.sourceMeasureNo = sourceMeasureNo == null ? "" : sourceMeasureNo;
            this.targetStaff = targetStaff;
            this.hasTargetStaff = hasTargetStaff;
            this.effectiveScoreDef = effectiveScoreDef;
            this.effectiveStaffDef = effectiveStaffDef;
            this.measureMeta = measureMeta;
            this.measureNo = measureNo == null || measureNo.length() == 0 ? this.sourceMeasureNo : measureNo;
            this.implicitFromMeta = implicitFromMeta;
            this.measureBeats = Math.max(1, measureBeats);
            this.measureBeatType = Math.max(1, measureBeatType);
            this.measureTimeSymbol = measureTimeSymbol;
            this.measureFifths = measureFifths;
            this.measureClefSign = measureClefSign == null || measureClefSign.length() == 0 ? "G" : measureClefSign;
            this.measureClefLine = Math.max(1, measureClefLine);
            this.measureTranspose = measureTranspose;
            this.measureTicks = Math.max(1, measureTicks);
            this.shouldEmitTime = shouldEmitTime;
            this.shouldEmitKey = shouldEmitKey;
            this.shouldEmitTranspose = shouldEmitTranspose;
            this.shouldEmitClef = shouldEmitClef;
        }

        public static MeiMeasureImportState missingStaff(MeiPartImportState previousPartState, String sourceMeasureNo,
                int measureIndex) {
            int beats = previousPartState == null ? 4 : previousPartState.getCurrentBeats();
            int beatType = previousPartState == null ? 4 : previousPartState.getCurrentBeatType();
            int fifths = previousPartState == null ? 0 : previousPartState.getCurrentFifths();
            String clefSign = previousPartState == null ? "G" : previousPartState.getCurrentClefSign();
            int clefLine = previousPartState == null ? 2 : previousPartState.getCurrentClefLine();
            MeiTranspose transpose = previousPartState == null ? null : previousPartState.getCurrentTranspose();
            return new MeiMeasureImportState(previousPartState, sourceMeasureNo, null, false, null, null, null,
                    sourceMeasureNo, false, beats, beatType,
                    previousPartState == null ? null : previousPartState.getCurrentTimeSymbol(), fifths, clefSign,
                    clefLine, transpose, Math.max(1, Math.round((beats * 4.0f * MEI_IMPORT_DIVISIONS) / beatType)),
                    measureIndex == 0, measureIndex == 0, measureIndex == 0, measureIndex == 0);
        }

        public MeiPartImportState toNextPartImportState() {
            boolean nextHasEmittedInitialAttributes = (previousPartState != null
                    && previousPartState.hasEmittedInitialAttributes()) || hasTargetStaff;
            if (previousPartState == null) {
                return new MeiPartImportState("", "", "", measureBeats, measureBeatType, measureTimeSymbol,
                        measureFifths, measureClefSign, measureClefLine, measureTranspose,
                        nextHasEmittedInitialAttributes);
            }
            return new MeiPartImportState(previousPartState.getStaffNo(), previousPartState.getPartId(),
                    previousPartState.getLabel(), measureBeats, measureBeatType, measureTimeSymbol, measureFifths,
                    measureClefSign, measureClefLine, measureTranspose, nextHasEmittedInitialAttributes);
        }

        public MeiPartImportState getPreviousPartState() {
            return previousPartState;
        }

        public String getSourceMeasureNo() {
            return sourceMeasureNo;
        }

        public Element getTargetStaff() {
            return targetStaff;
        }

        public boolean hasTargetStaff() {
            return hasTargetStaff;
        }

        public Element getEffectiveScoreDef() {
            return effectiveScoreDef;
        }

        public Element getEffectiveStaffDef() {
            return effectiveStaffDef;
        }

        public MeiMeasureMeta getMeasureMeta() {
            return measureMeta;
        }

        public String getMeasureNo() {
            return measureNo;
        }

        public boolean isImplicitFromMeta() {
            return implicitFromMeta;
        }

        public int getMeasureBeats() {
            return measureBeats;
        }

        public int getMeasureBeatType() {
            return measureBeatType;
        }

        public String getMeasureTimeSymbol() {
            return measureTimeSymbol;
        }

        public int getMeasureFifths() {
            return measureFifths;
        }

        public String getMeasureClefSign() {
            return measureClefSign;
        }

        public int getMeasureClefLine() {
            return measureClefLine;
        }

        public MeiTranspose getMeasureTranspose() {
            return measureTranspose;
        }

        public int getMeasureTicks() {
            return measureTicks;
        }

        public boolean shouldEmitTime() {
            return shouldEmitTime;
        }

        public boolean shouldEmitKey() {
            return shouldEmitKey;
        }

        public boolean shouldEmitTranspose() {
            return shouldEmitTranspose;
        }

        public boolean shouldEmitClef() {
            return shouldEmitClef;
        }
    }

    public static final class MeiDebugEventValue {
        private final String measureNo;
        private final String staffNo;
        private final String layerNo;
        private final int layerEntryIndex;
        private final String kind;
        private final String dur;
        private final int durationTicks;
        private final String pname;
        private final String octave;
        private final String accid;
        private final int chordNoteCount;

        public MeiDebugEventValue(String measureNo, String staffNo, String layerNo, int layerEntryIndex, String kind,
                String dur, int durationTicks, String pname, String octave, String accid, int chordNoteCount) {
            this.measureNo = measureNo;
            this.staffNo = staffNo;
            this.layerNo = layerNo;
            this.layerEntryIndex = layerEntryIndex;
            this.kind = kind;
            this.dur = dur;
            this.durationTicks = durationTicks;
            this.pname = pname;
            this.octave = octave;
            this.accid = accid;
            this.chordNoteCount = chordNoteCount;
        }

        public String getMeasureNo() {
            return measureNo;
        }

        public String getStaffNo() {
            return staffNo;
        }

        public String getLayerNo() {
            return layerNo;
        }

        public int getLayerEntryIndex() {
            return layerEntryIndex;
        }

        public String getKind() {
            return kind;
        }

        public String getDur() {
            return dur;
        }

        public int getDurationTicks() {
            return durationTicks;
        }

        public String getPname() {
            return pname;
        }

        public String getOctave() {
            return octave;
        }

        public String getAccid() {
            return accid;
        }

        public int getChordNoteCount() {
            return chordNoteCount;
        }
    }

    public static final class MeiClef {
        private final String clefSign;
        private final int clefLine;

        public MeiClef(String clefSign, int clefLine) {
            this.clefSign = clefSign == null || clefSign.trim().length() == 0 ? "G" : clefSign;
            this.clefLine = Math.max(1, clefLine);
        }

        public String getClefSign() {
            return clefSign;
        }

        public int getClefLine() {
            return clefLine;
        }
    }

    public static final class MeiTranspose {
        private final Integer chromatic;
        private final Integer diatonic;

        public MeiTranspose(Integer chromatic, Integer diatonic) {
            this.chromatic = chromatic;
            this.diatonic = diatonic;
        }

        public Integer getChromatic() {
            return chromatic;
        }

        public Integer getDiatonic() {
            return diatonic;
        }
    }

    public static final class MeiMeter {
        private final int beats;
        private final int beatType;

        public MeiMeter(int beats, int beatType) {
            this.beats = Math.max(1, beats);
            this.beatType = Math.max(1, beatType);
        }

        public int getBeats() {
            return beats;
        }

        public int getBeatType() {
            return beatType;
        }
    }

    public static final class MeiStaffMeta {
        private final String label;
        private final String clefSign;
        private final int clefLine;

        public MeiStaffMeta(String label, String clefSign, int clefLine) {
            this.label = label == null ? "" : label;
            this.clefSign = clefSign == null || clefSign.trim().length() == 0 ? "G" : clefSign;
            this.clefLine = Math.max(1, clefLine);
        }

        public String getLabel() {
            return label;
        }

        public String getClefSign() {
            return clefSign;
        }

        public int getClefLine() {
            return clefLine;
        }
    }

    public static final class MeiMeasureMeta {
        private final String number;
        private final Boolean implicit;
        private final String repeat;
        private final Integer times;
        private final Boolean explicitTime;
        private final Integer beats;
        private final Integer beatType;
        private final String doubleBar;

        private MeiMeasureMeta(Builder builder) {
            this.number = builder.number;
            this.implicit = builder.implicit;
            this.repeat = builder.repeat;
            this.times = builder.times;
            this.explicitTime = builder.explicitTime;
            this.beats = builder.beats;
            this.beatType = builder.beatType;
            this.doubleBar = builder.doubleBar;
        }

        public String getNumber() {
            return number;
        }

        public Boolean getImplicit() {
            return implicit;
        }

        public String getRepeat() {
            return repeat;
        }

        public Integer getTimes() {
            return times;
        }

        public Boolean getExplicitTime() {
            return explicitTime;
        }

        public Integer getBeats() {
            return beats;
        }

        public Integer getBeatType() {
            return beatType;
        }

        public String getDoubleBar() {
            return doubleBar;
        }

        public static final class Builder {
            private String number;
            private Boolean implicit;
            private String repeat;
            private Integer times;
            private Boolean explicitTime;
            private Integer beats;
            private Integer beatType;
            private String doubleBar;

            public Builder number(String value) {
                this.number = value;
                return this;
            }

            public Builder implicit(boolean value) {
                this.implicit = Boolean.valueOf(value);
                return this;
            }

            public Builder repeat(String value) {
                this.repeat = value;
                return this;
            }

            public Builder times(int value) {
                this.times = Integer.valueOf(value);
                return this;
            }

            public Builder explicitTime(boolean value) {
                this.explicitTime = Boolean.valueOf(value);
                return this;
            }

            public Builder beats(int value) {
                this.beats = Integer.valueOf(Math.max(1, value));
                return this;
            }

            public Builder beatType(int value) {
                this.beatType = Integer.valueOf(Math.max(1, value));
                return this;
            }

            public Builder doubleBar(String value) {
                this.doubleBar = value;
                return this;
            }

            public MeiMeasureMeta build() {
                return new MeiMeasureMeta(this);
            }
        }
    }
}
