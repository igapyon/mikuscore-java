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
        String rootAlterXml = "<root-alter>" + parsed.getRootAlter() + "</root-alter>";
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

    public static List<ParsedMeiXmlEvent> applyMeiBeamSpanControlEvent(Collection<ParsedMeiXmlEvent> events,
            String startId, String tstamp, String plist, String endId, String tstamp2,
            Map<String, Integer> idToEventIndex, Map<String, Integer> idToEventTick, int divisions, int beatType) {
        List<ParsedMeiXmlEvent> out = copyXmlEvents(events);
        Integer startIndex = resolveXmlEventEndpointIndex(startId, tstamp, idToEventIndex, out, divisions, beatType,
                plist, idToEventTick);
        Integer endIndex = resolveXmlEventEndpointIndex(endId, tstamp2, idToEventIndex, out, divisions, beatType, null,
                idToEventTick);
        if (!isValidXmlEventIndex(out, startIndex) || !isValidXmlEventIndex(out, endIndex)) {
            return out;
        }
        List<Integer> spanIndexes = resolveBeamSpanIndexes(plist, idToEventIndex, startIndex.intValue(),
                endIndex.intValue());
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
        List<ParsedMeiEvent> plainEvents = new ArrayList<ParsedMeiEvent>();
        if (events != null) {
            for (ParsedMeiXmlEvent event : events) {
                plainEvents.add(new ParsedMeiEvent(event == null ? "" : event.getKind(),
                        event == null ? 0 : event.getDurationTicks()));
            }
        }
        return resolveControlEventEndpointIndex(rawId, tstamp, idToEventIndex, plainEvents, divisions, beatType,
                rawPlist, idToEventTick);
    }

    private static List<ParsedMeiXmlEvent> copyXmlEvents(Collection<ParsedMeiXmlEvent> events) {
        return events == null ? new ArrayList<ParsedMeiXmlEvent>() : new ArrayList<ParsedMeiXmlEvent>(events);
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

    private static List<Integer> resolveBeamSpanIndexes(String plist, Map<String, Integer> idToEventIndex,
            int startIndex, int endIndex) {
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
        if (!plistIndexes.isEmpty()) {
            return plistIndexes;
        }
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

    private static String offsetXml(Integer tick) {
        return tick != null && tick.intValue() > 0 ? "<offset>" + tick.intValue() + "</offset>" : "";
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

        public ParsedMeiXmlEvent(String kind, int durationTicks, String xml) {
            this.kind = kind == null ? "" : kind;
            this.durationTicks = durationTicks;
            this.xml = xml == null ? "" : xml;
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

        public ParsedMeiXmlEvent withXml(String nextXml) {
            return new ParsedMeiXmlEvent(kind, durationTicks, nextXml);
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
