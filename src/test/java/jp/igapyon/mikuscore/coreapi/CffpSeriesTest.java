/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.midi.MidiIo;

/**
 * Cross-format semantic anchor for the pinned Node CFFP source cases.
 */
public class CffpSeriesTest {
    private static final Pattern CFFP_CASE_PATTERN = Pattern.compile(
            "(?s)id:\\s*\\\"(CFFP-[^\\\"]+)\\\",\\s*xml:\\s*`(.*?)`,(.*?)(?=\\n\\s*\\{\\s*\\n\\s*id:|\\n\\];)");
    private static final Pattern BOOLEAN_MAP_PATTERN = Pattern.compile("([a-zA-Z]+):\\s*(true|false)");
    private static final Pattern FEATURE_SELECTOR_PATTERN = Pattern.compile(
            "querySelector(?:All)?\\(\\s*[\\\"']([^\\\"']+)");
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9-]*");

    @Test
    public void roundtripsEveryPinnedCffpCaseThroughEachIncludedBridge() {
        List<CffpCase> cases = loadPinnedCffpCases();
        assertEquals(86, cases.size());

        for (CffpCase cffpCase : cases) {
            Document sourceDocument = MusicXmlIo.parseMusicXmlDocument(cffpCase.xml);
            assertNotNull(sourceDocument, cffpCase.id);
            PitchFact sourceFact = cffpCase.requirePitchedFact ? firstPitchedFact(sourceDocument) : null;
            assertRoundtrip(cffpCase, sourceFact, "abc", roundtripAbc(cffpCase.xml));
            assertRoundtrip(cffpCase, sourceFact, "mei", roundtripMei(cffpCase.xml));
            assertRoundtrip(cffpCase, sourceFact, "lilypond", roundtripLilyPond(cffpCase.xml));
            assertRoundtrip(cffpCase, sourceFact, "musescore", roundtripMuseScore(cffpCase.xml));
            assertRoundtrip(cffpCase, sourceFact, "midi", roundtripMidi(cffpCase.xml));
        }
    }

    private static String roundtripAbc(String xml) {
        CoreApi.CliResult exported = CoreApi.exportMusicXmlToAbc(xml);
        assertTrue(exported.isOk(), exported.getDiagnostic());
        CoreApi.CliResult imported = CoreApi.importAbcToMusicXml(exported.getOutput());
        assertTrue(imported.isOk(), imported.getDiagnostic());
        return imported.getOutput();
    }

    private static String roundtripMei(String xml) {
        CoreApi.CliResult exported = CoreApi.exportMusicXmlToMei(xml);
        assertTrue(exported.isOk(), exported.getDiagnostic());
        CoreApi.CliResult imported = CoreApi.importMeiToMusicXml(exported.getOutput());
        assertTrue(imported.isOk(), imported.getDiagnostic());
        return imported.getOutput();
    }

    private static String roundtripLilyPond(String xml) {
        CoreApi.CliResult exported = CoreApi.exportMusicXmlToLilyPond(xml);
        assertTrue(exported.isOk(), exported.getDiagnostic());
        CoreApi.CliResult imported = CoreApi.importLilyPondToMusicXml(exported.getOutput());
        assertTrue(imported.isOk(), imported.getDiagnostic());
        return imported.getOutput();
    }

    private static String roundtripMuseScore(String xml) {
        CoreApi.CliResult exported = CoreApi.exportMusicXmlToMuseScore(xml);
        assertTrue(exported.isOk(), exported.getDiagnostic());
        CoreApi.CliResult imported = CoreApi.importMuseScoreToMusicXml(exported.getOutput());
        assertTrue(imported.isOk(), imported.getDiagnostic());
        return imported.getOutput();
    }

    private static String roundtripMidi(String xml) {
        Document source = MusicXmlIo.parseMusicXmlDocument(xml);
        assertNotNull(source, "MIDI source document");
        int playbackTicksPerQuarter = 128;
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromMusicXmlDoc(source,
                playbackTicksPerQuarter, new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiExportPlaybackBuildResult exported = MidiIo.buildMidiPlaybackExport(playback.getEvents(),
                playback.getTempo(), "electric_piano_2", MidiIo.collectMidiProgramOverridesFromMusicXmlDoc(source),
                MidiIo.collectMidiControlEventsFromMusicXmlDoc(source, playbackTicksPerQuarter),
                MidiIo.collectMidiTempoEventsFromMusicXmlDoc(source, playbackTicksPerQuarter),
                MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(source, playbackTicksPerQuarter),
                MidiIo.collectMidiKeySignatureEventsFromMusicXmlDoc(source, playbackTicksPerQuarter),
                false, true, true, playbackTicksPerQuarter, Collections.<String>emptyList(), false,
                "off_before_on", "", "", "", 0);
        assertFalse(exported.isRawWriter(), "CFFP uses the upstream default MidiWriterJS path");
        MidiIo.MidiImportResult imported = MidiIo.convertMidiToMusicXml(
                MidiIo.buildMidiWriterCompatibleBytes(exported.getWriterTrackPlan(), playbackTicksPerQuarter),
                new MidiIo.MidiImportOptions("1/16", null, null, null, null));
        assertTrue(imported.isOk(), imported.getDiagnostics().toString());
        return imported.getXml();
    }

    private static void assertRoundtrip(CffpCase cffpCase, PitchFact source, String format, String outputXml) {
        Document outputDocument = MusicXmlIo.parseMusicXmlDocument(outputXml);
        assertNotNull(outputDocument, cffpCase.id + ":" + format);
        if (source != null) {
            PitchFact actual = firstPitchedFact(outputDocument);
            if (valueForFormat(cffpCase.preservePitchByFormat, format, true)) {
                assertEquals(source.step, actual.step, cffpCase.id + ":" + format + ":step");
                assertEquals(source.octave, actual.octave, cffpCase.id + ":" + format + ":octave");
            }
            assertEquals(0, actual.startDiv, cffpCase.id + ":" + format + ":startDiv");
            boolean defaultDuration = !"midi".equals(format);
            if (valueForFormat(cffpCase.preserveDurationByFormat, format, defaultDuration)) {
                assertEquals(source.quarterLength, actual.quarterLength, 0.1d,
                        cffpCase.id + ":" + format + ":duration");
            }
        }
        if (valueForFormat(cffpCase.preserveByFormat, format, false)) {
            assertFalse(cffpCase.featureTagNames.isEmpty(), cffpCase.id + ": missing feature tag extraction");
            for (String featureTag : cffpCase.featureTagNames) {
                assertTrue(outputDocument.getElementsByTagName(featureTag).getLength() > 0,
                        cffpCase.id + ":" + format + ":feature=" + featureTag);
            }
            assertPinnedFeaturePredicate(cffpCase.id, outputDocument, format);
        }
    }

    /**
     * Semantic equivalents of the pinned Node {@code hasFeature} predicates for
     * the cases that declare support in {@code preserveByFormat}.  Keeping this
     * finite switch deliberate makes an upstream predicate addition fail loudly
     * instead of silently degrading to the tag-presence smoke check above.
     */
    private static void assertPinnedFeaturePredicate(String id, Document document, String format) {
        boolean preserved;
        if ("CFFP-TRILL".equals(id)) {
            preserved = document.getElementsByTagName("trill-mark").getLength() > 0;
        } else if ("CFFP-SEGNO-CODA".equals(id)) {
            preserved = document.getElementsByTagName("segno").getLength() > 0
                    && document.getElementsByTagName("coda").getLength() > 0;
        } else if ("CFFP-ACCIDENTAL".equals(id)) {
            List<Element> measures = directChildren(firstElement(document, "part"), "measure");
            List<Element> firstMeasureNotes = measures.isEmpty()
                    ? Collections.<Element>emptyList() : directChildren(measures.get(0), "note");
            List<Element> secondMeasureNotes = measures.size() < 2
                    ? Collections.<Element>emptyList() : directChildren(measures.get(1), "note");
            preserved = firstMeasureNotes.size() >= 2 && !secondMeasureNotes.isEmpty()
                    && "natural".equalsIgnoreCase(directChildText(firstMeasureNotes.get(0), "accidental"))
                    && pitchAlter(firstMeasureNotes.get(1)) == 1 && pitchAlter(secondMeasureNotes.get(0)) == 1;
        } else if ("CFFP-ACCIDENTAL-RESET".equals(id)) {
            List<Element> measures = directChildren(firstElement(document, "part"), "measure");
            List<Element> firstMeasureNotes = measures.isEmpty()
                    ? Collections.<Element>emptyList() : directChildren(measures.get(0), "note");
            List<Element> secondMeasureNotes = measures.size() < 2
                    ? Collections.<Element>emptyList() : directChildren(measures.get(1), "note");
            String resetAlter = secondMeasureNotes.isEmpty() ? ""
                    : directGrandchildText(secondMeasureNotes.get(0), "pitch", "alter");
            preserved = !firstMeasureNotes.isEmpty() && !secondMeasureNotes.isEmpty()
                    && pitchAlter(firstMeasureNotes.get(0)) == 1
                    && (resetAlter.length() == 0 || parseInteger(resetAlter, 0) == 0);
        } else if ("CFFP-MULTIVOICE-BACKUP".equals(id) || "CFFP-PERCUSSION-VOICE-LAYER".equals(id)) {
            List<String> voices = noteChildTexts(document, "voice");
            preserved = voices.contains("1") && voices.contains("2");
        } else if ("CFFP-PICKUP-IMPLICIT".equals(id)) {
            Element measure = directChildren(firstElement(document, "part"), "measure").get(0);
            preserved = "0".equals(measure.getAttribute("number"))
                    && "yes".equalsIgnoreCase(measure.getAttribute("implicit"));
        } else if ("CFFP-GRANDSTAFF-MAPPING".equals(id)) {
            List<String> staffs = noteChildTexts(document, "staff");
            preserved = staffs.contains("1") && staffs.contains("2");
        } else if ("CFFP-TIME-CHANGE".equals(id)) {
            List<Element> measures = directChildren(firstElement(document, "part"), "measure");
            preserved = hasTimeSignature(measures.get(0), 4, 4) && hasTimeSignature(measures.get(1), 3, 4);
        } else if ("CFFP-REPEAT-ENDING".equals(id)) {
            List<Element> measures = directChildren(firstElement(document, "part"), "measure");
            preserved = hasBarlineRepeat(measures.get(0), "left", "forward")
                    && hasBarlineRepeat(measures.get(1), "right", "backward");
        } else if ("CFFP-TEMPO-MAP".equals(id)) {
            List<String> tempos = descendantAttributes(document, "sound", "tempo");
            preserved = tempos.contains("120") && tempos.contains("90");
        } else if ("CFFP-OCTSHIFT".equals(id)) {
            preserved = hasDescendantAttribute(document, "octave-shift", "type", "up");
        } else if ("CFFP-SLUR".equals(id)) {
            preserved = hasDescendantAttribute(document, "slur", "type", "start")
                    && hasDescendantAttribute(document, "slur", "type", "stop");
        } else if ("CFFP-TIE".equals(id)) {
            preserved = hasDescendantAttribute(document, "tie", "type", "start")
                    && hasDescendantAttribute(document, "tie", "type", "stop");
        } else if ("CFFP-NOTE-TIES-CROSS-MEASURE".equals(id)) {
            List<Element> measures = directChildren(firstElement(document, "part"), "measure");
            preserved = noteChildrenHaveAttribute(measures.get(0), "tie", "type", "start")
                    && noteChildrenHaveAttribute(measures.get(1), "tie", "type", "stop");
        } else if ("CFFP-STACCATO".equals(id)) {
            preserved = document.getElementsByTagName("staccato").getLength() > 0;
        } else if ("CFFP-ACCENT".equals(id)) {
            preserved = document.getElementsByTagName("accent").getLength() > 0;
        } else if ("CFFP-GRACE".equals(id)) {
            preserved = document.getElementsByTagName("grace").getLength() > 0;
        } else if ("CFFP-TUPLET".equals(id)) {
            preserved = document.getElementsByTagName("actual-notes").getLength() > 0;
        } else {
            throw new AssertionError("Missing exact CFFP predicate mapping: " + id);
        }
        assertTrue(preserved, id + ":" + format + ": exact pinned feature predicate");
    }

    private static boolean valueForFormat(Map<String, Boolean> values, String format, boolean fallback) {
        Boolean value = values.get(format);
        return value == null ? fallback : value.booleanValue();
    }

    private static List<CffpCase> loadPinnedCffpCases() {
        String source = loadResourceText("upstream-cffp-series.spec.ts");
        Matcher matcher = CFFP_CASE_PATTERN.matcher(source);
        List<CffpCase> cases = new ArrayList<CffpCase>();
        while (matcher.find()) {
            String id = matcher.group(1);
            String xml = matcher.group(2);
            String config = matcher.group(3);
            cases.add(new CffpCase(id, xml, !config.contains("requirePitchedFact: false"),
                    parseFormatBooleanMap(config, "preserveByFormat"),
                    parseFormatBooleanMap(config, "preservePitchByFormat"),
                    parseFormatBooleanMap(config, "preserveDurationByFormat"),
                    extractFeatureTagNames(config)));
        }
        return cases;
    }

    private static Map<String, Boolean> parseFormatBooleanMap(String text, String property) {
        Pattern propertyPattern = Pattern.compile(property + "\\s*:\\s*\\{([^}]*)\\}");
        Matcher propertyMatcher = propertyPattern.matcher(text);
        Map<String, Boolean> values = new LinkedHashMap<String, Boolean>();
        if (!propertyMatcher.find()) {
            return values;
        }
        Matcher valueMatcher = BOOLEAN_MAP_PATTERN.matcher(propertyMatcher.group(1));
        while (valueMatcher.find()) {
            values.put(valueMatcher.group(1), Boolean.valueOf(valueMatcher.group(2)));
        }
        return values;
    }

    private static List<String> extractFeatureTagNames(String config) {
        List<String> tags = new ArrayList<String>();
        Matcher selectorMatcher = FEATURE_SELECTOR_PATTERN.matcher(config);
        while (selectorMatcher.find()) {
            String selector = selectorMatcher.group(1);
            String lastSegment = selector.substring(selector.lastIndexOf('>') + 1).trim();
            Matcher tagMatcher = TAG_NAME_PATTERN.matcher(lastSegment);
            if (tagMatcher.find()) {
                String tag = tagMatcher.group();
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private static String loadResourceText(String name) {
        try {
            InputStream in = CffpSeriesTest.class.getClassLoader().getResourceAsStream(name);
            assertNotNull(in, name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            in.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load fixture: " + name, ex);
        }
    }

    private static PitchFact firstPitchedFact(Document document) {
        Element part = firstElement(document, "part");
        assertNotNull(part, "missing part");
        int divisions = 1;
        int cursorDiv = 0;
        for (Node measureNode = part.getFirstChild(); measureNode != null; measureNode = measureNode.getNextSibling()) {
            if (!(measureNode instanceof Element) || !"measure".equals(((Element) measureNode).getTagName())) {
                continue;
            }
            Element measure = (Element) measureNode;
            String divisionsText = directGrandchildText(measure, "attributes", "divisions");
            if (divisionsText.length() > 0) {
                divisions = Math.max(1, parseInteger(divisionsText, divisions));
            }
            for (Node childNode = measure.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (!(childNode instanceof Element)) {
                    continue;
                }
                Element child = (Element) childNode;
                if ("backup".equals(child.getTagName())) {
                    cursorDiv = Math.max(0, cursorDiv - parseInteger(directChildText(child, "duration"), 0));
                    continue;
                }
                if (!"note".equals(child.getTagName())) {
                    continue;
                }
                boolean chord = directChild(child, "chord") != null;
                boolean grace = directChild(child, "grace") != null;
                int duration = parseInteger(directChildText(child, "duration"), 0);
                Element pitch = directChild(child, "pitch");
                if (pitch != null) {
                    return new PitchFact(directChildText(pitch, "step"),
                            parseInteger(directChildText(pitch, "octave"), 0),
                            duration / (double) divisions, cursorDiv);
                }
                if (!chord && !grace && duration > 0) {
                    cursorDiv += duration;
                }
            }
        }
        throw new AssertionError("no pitched note found");
    }

    private static Element firstElement(Document document, String tagName) {
        return document == null || document.getElementsByTagName(tagName).getLength() == 0
                ? null
                : (Element) document.getElementsByTagName(tagName).item(0);
    }

    private static Element directChild(Element parent, String tagName) {
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
        List<Element> children = new ArrayList<Element>();
        if (parent == null) {
            return children;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                children.add((Element) child);
            }
        }
        return children;
    }

    private static int pitchAlter(Element note) {
        return parseInteger(directGrandchildText(note, "pitch", "alter"), 0);
    }

    private static List<String> noteChildTexts(Document document, String tagName) {
        List<String> values = new ArrayList<String>();
        for (int index = 0; index < document.getElementsByTagName("note").getLength(); index++) {
            values.add(directChildText((Element) document.getElementsByTagName("note").item(index), tagName));
        }
        return values;
    }

    private static List<String> descendantAttributes(Document document, String tagName, String attributeName) {
        List<String> values = new ArrayList<String>();
        for (int index = 0; index < document.getElementsByTagName(tagName).getLength(); index++) {
            Element element = (Element) document.getElementsByTagName(tagName).item(index);
            if (element.hasAttribute(attributeName)) {
                values.add(element.getAttribute(attributeName));
            }
        }
        return values;
    }

    private static boolean hasDescendantAttribute(Document document, String tagName, String attributeName,
            String expectedValue) {
        return descendantAttributes(document, tagName, attributeName).contains(expectedValue);
    }

    private static boolean hasTimeSignature(Element measure, int beats, int beatType) {
        Element time = directChild(directChild(measure, "attributes"), "time");
        return parseInteger(directChildText(time, "beats"), -1) == beats
                && parseInteger(directChildText(time, "beat-type"), -1) == beatType;
    }

    private static boolean hasBarlineRepeat(Element measure, String location, String direction) {
        for (Element barline : directChildren(measure, "barline")) {
            if (location.equals(barline.getAttribute("location"))) {
                Element repeat = directChild(barline, "repeat");
                if (repeat != null && direction.equals(repeat.getAttribute("direction"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean noteChildrenHaveAttribute(Element measure, String tagName, String attributeName,
            String expectedValue) {
        for (Element note : directChildren(measure, "note")) {
            Element child = directChild(note, tagName);
            if (child != null && expectedValue.equals(child.getAttribute(attributeName))) {
                return true;
            }
        }
        return false;
    }

    private static String directChildText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        return child == null || child.getTextContent() == null ? "" : child.getTextContent().trim();
    }

    private static String directGrandchildText(Element parent, String childName, String grandchildName) {
        return directChildText(directChild(parent, childName), grandchildName);
    }

    private static int parseInteger(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static final class CffpCase {
        private final String id;
        private final String xml;
        private final boolean requirePitchedFact;
        private final Map<String, Boolean> preserveByFormat;
        private final Map<String, Boolean> preservePitchByFormat;
        private final Map<String, Boolean> preserveDurationByFormat;
        private final List<String> featureTagNames;

        private CffpCase(String id, String xml, boolean requirePitchedFact,
                Map<String, Boolean> preserveByFormat, Map<String, Boolean> preservePitchByFormat,
                Map<String, Boolean> preserveDurationByFormat, List<String> featureTagNames) {
            this.id = id;
            this.xml = xml;
            this.requirePitchedFact = requirePitchedFact;
            this.preserveByFormat = preserveByFormat;
            this.preservePitchByFormat = preservePitchByFormat;
            this.preserveDurationByFormat = preserveDurationByFormat;
            this.featureTagNames = featureTagNames;
        }
    }

    private static final class PitchFact {
        private final String step;
        private final int octave;
        private final double quarterLength;
        private final int startDiv;

        private PitchFact(String step, int octave, double quarterLength, int startDiv) {
            this.step = step;
            this.octave = octave;
            this.quarterLength = quarterLength;
            this.startDiv = startDiv;
        }
    }
}
