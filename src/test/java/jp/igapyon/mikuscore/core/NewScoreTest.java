/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public class NewScoreTest {
    @Test
    public void createsPinnedEightMeasureMultiPartScoreShape() {
        Document doc = parse(NewScore.createNewScoreMusicXml(new NewScore.Options(false, Integer.valueOf(2),
                Integer.valueOf(-3), Integer.valueOf(6), Integer.valueOf(8), Arrays.asList("treble", "bass"))));

        assertEquals("3.1", doc.getDocumentElement().getAttribute("version"));
        assertEquals(2, doc.getElementsByTagName("score-part").getLength());
        assertEquals(16, doc.getElementsByTagName("measure").getLength());
        assertEquals("Part 2", directChildText((Element) doc.getElementsByTagName("score-part").item(1), "part-name"));
        assertEquals("F", descendantText((Element) doc.getElementsByTagName("part").item(1), "sign"));
        assertEquals("-3", descendantText((Element) doc.getElementsByTagName("part").item(0), "fifths"));
        assertEquals("1440", descendantText((Element) doc.getElementsByTagName("part").item(0), "duration"));
    }

    @Test
    public void createsPinnedPianoGrandStaffTemplate() {
        Document doc = parse(NewScore.createNewScoreMusicXml(new NewScore.Options(true, Integer.valueOf(8), null,
                Integer.valueOf(4), Integer.valueOf(4), Arrays.asList("alto"))));
        Element part = (Element) doc.getElementsByTagName("part").item(0);
        Element firstMeasure = (Element) part.getElementsByTagName("measure").item(0);

        assertEquals(1, doc.getElementsByTagName("score-part").getLength());
        assertEquals("Piano", directChildText((Element) doc.getElementsByTagName("score-part").item(0), "part-name"));
        assertEquals("2", directChildText((Element) firstMeasure.getElementsByTagName("attributes").item(0), "staves"));
        assertEquals(2, directChildElements(firstMeasure, "note").size());
        assertEquals("1920", directChildText((Element) firstMeasure.getElementsByTagName("backup").item(0), "duration"));
        assertEquals("1", directChildText((Element) directChildElements(firstMeasure, "note").get(0), "staff"));
        assertEquals("2", directChildText((Element) directChildElements(firstMeasure, "note").get(1), "staff"));
    }

    @Test
    public void normalizesPinnedPublicNewScoreOptions() {
        Document doc = parse(NewScore.createNewScoreMusicXml(new NewScore.Options(false, Integer.valueOf(99),
                "0x63", Integer.valueOf(0), Integer.valueOf(3), Arrays.asList("unsupported"))));
        Element firstPart = (Element) doc.getElementsByTagName("part").item(0);

        assertEquals(16, doc.getElementsByTagName("score-part").getLength());
        assertEquals("7", descendantText(firstPart, "fifths"));
        assertEquals("1", descendantText(firstPart, "beats"));
        assertEquals("4", descendantText(firstPart, "beat-type"));
        assertEquals("G", directChildText((Element) firstPart.getElementsByTagName("clef").item(0), "sign"));
    }

    private static Document parse(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        assertNotNull(doc);
        return doc;
    }

    private static String descendantText(Element parent, String name) {
        if (parent == null || parent.getElementsByTagName(name).getLength() == 0) {
            return "";
        }
        return parent.getElementsByTagName(name).item(0).getTextContent();
    }

    private static String directChildText(Element parent, String name) {
        for (Element child : directChildElements(parent, name)) {
            return child.getTextContent();
        }
        return "";
    }

    private static java.util.List<Element> directChildElements(Element parent, String name) {
        java.util.List<Element> out = new java.util.ArrayList<Element>();
        for (org.w3c.dom.Node child = parent == null ? null : parent.getFirstChild(); child != null;
                child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(((Element) child).getTagName())) {
                out.add((Element) child);
            }
        }
        return out;
    }
}
