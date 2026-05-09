/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MusicXmlIoTest {
    @Test
    public void parsesValidMusicXmlDocument() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(minimalMusicXmlWithoutPartList("Parse"));

        assertNotNull(doc);
        assertEquals("score-partwise", doc.getDocumentElement().getTagName());
    }

    @Test
    public void returnsNullForInvalidMusicXmlDocument() {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<score-partwise");

        assertNull(doc);
    }

    @Test
    public void normalizesMissingPartListAndPartId() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(minimalMusicXmlWithoutPartList("Normalize"));

        assertTrue(normalized.contains("<part-list>"));
        assertTrue(normalized.contains("<score-part id=\"P1\">"));
        assertTrue(normalized.contains("<part-name>Music</part-name>"));
        assertTrue(normalized.contains("<part id=\"P1\">"));
    }

    @Test
    public void addsTupletStartAndStopNotationsFromTimeModification() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(tupletMusicXml(false));
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        Element firstTuplet = firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet");
        Element thirdTuplet = firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet");
        assertEquals("start", firstTuplet.getAttribute("type"));
        assertEquals("1", firstTuplet.getAttribute("number"));
        assertEquals("yes", firstTuplet.getAttribute("bracket"));
        assertEquals("actual", firstTuplet.getAttribute("show-number"));
        assertEquals("stop", thirdTuplet.getAttribute("type"));
        assertEquals("1", thirdTuplet.getAttribute("number"));
    }

    @Test
    public void keepsExistingTupletNumbersAndAddsMissingDisplayAttrs() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(tupletMusicXml(true));
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        Element firstTuplet = firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet");
        Element thirdTuplet = firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet");
        assertEquals("7", firstTuplet.getAttribute("number"));
        assertEquals("yes", firstTuplet.getAttribute("bracket"));
        assertEquals("actual", firstTuplet.getAttribute("show-number"));
        assertEquals("7", thirdTuplet.getAttribute("number"));
    }

    @Test
    public void fillsMissingTupletGroupAfterExistingExplicitTupletInSameLane() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(twoTupletGroupsMusicXml());
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        Element firstGroupStart = firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet");
        Element firstGroupStop = firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet");
        Element secondGroupStart = firstDirectChild(directChild(noteAt(doc, 4), "notations"), "tuplet");
        Element secondGroupStop = firstDirectChild(directChild(noteAt(doc, 6), "notations"), "tuplet");
        assertEquals("7", firstGroupStart.getAttribute("number"));
        assertEquals("7", firstGroupStop.getAttribute("number"));
        assertEquals("start", secondGroupStart.getAttribute("type"));
        assertEquals("stop", secondGroupStop.getAttribute("type"));
        assertEquals("yes", secondGroupStart.getAttribute("bracket"));
        assertEquals("actual", secondGroupStart.getAttribute("show-number"));
    }

    @Test
    public void doesNotAddImplicitBeamsDuringImportedTextNormalization() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(beamMusicXml(false));
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        assertNull(directChild(noteAt(doc, 0), "beam"));
        assertNull(directChild(noteAt(doc, 1), "beam"));
        assertNull(directChild(noteAt(doc, 2), "beam"));
        assertNull(directChild(noteAt(doc, 3), "beam"));
    }

    @Test
    public void addsImplicitBeamsOnlyWhenRequestedExplicitly() {
        String withBeams = MusicXmlIo.applyImplicitBeamsToMusicXmlText(beamMusicXml(false));
        Document doc = MusicXmlIo.parseMusicXmlDocument(withBeams);

        assertEquals("begin", directChild(noteAt(doc, 0), "beam").getTextContent());
        assertEquals("end", directChild(noteAt(doc, 1), "beam").getTextContent());
        assertEquals("begin", directChild(noteAt(doc, 2), "beam").getTextContent());
        assertEquals("end", directChild(noteAt(doc, 3), "beam").getTextContent());
    }

    @Test
    public void keepsLaneBeamsUntouchedWhenImplicitBeamPassRunsOverExistingBeams() {
        String withBeams = MusicXmlIo.applyImplicitBeamsToMusicXmlText(beamMusicXml(true));
        Document doc = MusicXmlIo.parseMusicXmlDocument(withBeams);

        assertEquals(1, countDirectChildren(noteAt(doc, 0), "beam"));
        assertEquals(1, countDirectChildren(noteAt(doc, 1), "beam"));
        assertEquals(0, countDirectChildren(noteAt(doc, 2), "beam"));
        assertEquals(0, countDirectChildren(noteAt(doc, 3), "beam"));
    }

    @Test
    public void addsFinalRightBarlineWhenMissing() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(minimalMusicXmlWithoutPartList("Final"));

        assertTrue(normalized.contains("<barline location=\"right\">"));
        assertTrue(normalized.contains("<bar-style>light-heavy</bar-style>"));
    }

    @Test
    public void keepsExistingFinalRightBarline() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(musicXmlWithExistingFinalBarline());

        assertEquals(1, countOccurrences(normalized, "<barline location=\"right\">"));
        assertTrue(normalized.contains("<bar-style>heavy-light</bar-style>"));
        assertTrue(!normalized.contains("<bar-style>light-heavy</bar-style>"));
    }

    @Test
    public void returnsOriginalTextWhenNormalizationCannotParse() {
        String invalid = "<score-partwise";

        assertEquals(invalid, MusicXmlIo.normalizeImportedMusicXmlText(invalid));
    }

    private static String minimalMusicXmlWithoutPartList(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part>\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><rest/><duration>1920</duration><voice>1</voice><type>whole</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String beamMusicXml(boolean existingBeams) {
        String firstBeam = existingBeams ? "<beam number=\"1\">begin</beam>" : "";
        String secondBeam = existingBeams ? "<beam number=\"1\">end</beam>" : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type>" + firstBeam + "</note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type>" + secondBeam + "</note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String tupletMusicXml(boolean existingTuplets) {
        String firstNotations = existingTuplets ? "<notations><tuplet type=\"start\" number=\"7\"/></notations>" : "";
        String thirdNotations = existingTuplets ? "<notations><tuplet type=\"stop\" number=\"7\"/></notations>" : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>" + firstNotations + "</note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>" + thirdNotations + "</note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String twoTupletGroupsMusicXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><tuplet type=\"start\" number=\"7\"/></notations></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><tuplet type=\"stop\" number=\"7\"/></notations></note>\n"
                + "      <note><rest/></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>G</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>A</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String musicXmlWithExistingFinalBarline() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><rest/><duration>1920</duration><voice>1</voice><type>whole</type></note>\n"
                + "      <barline location=\"right\"><bar-style>heavy-light</bar-style></barline>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while (true) {
            index = text.indexOf(pattern, index);
            if (index < 0) {
                return count;
            }
            count++;
            index += pattern.length();
        }
    }

    private static Element noteAt(Document doc, int index) {
        NodeList notes = doc.getElementsByTagName("note");
        return (Element) notes.item(index);
    }

    private static Element directChild(Element parent, String tagName) {
        return firstDirectChild(parent, tagName);
    }

    private static Element firstDirectChild(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element && tagName.equals(((Element) children.item(index)).getTagName())) {
                return (Element) children.item(index);
            }
        }
        return null;
    }

    private static int countDirectChildren(Element parent, String tagName) {
        int count = 0;
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element && tagName.equals(((Element) children.item(index)).getTagName())) {
                count++;
            }
        }
        return count;
    }
}
