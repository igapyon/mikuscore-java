/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public class ScoreDurationsTest {
    @Test
    public void normalizesDotCounts() {
        assertEquals(2, ScoreDurations.normalizeDotCount(Double.valueOf(2.2)));
        assertEquals(0, ScoreDurations.normalizeDotCount(Integer.valueOf(0)));
        assertEquals(0, ScoreDurations.normalizeDotCount(Double.valueOf(Double.NaN)));
    }

    @Test
    public void buildsMusicXmlDotItems() {
        assertEquals("<dot/><dot/><dot/>", ScoreDurations.buildMusicXmlDotsXml(Integer.valueOf(3)));
    }

    @Test
    public void countsMusicXmlDotItems() {
        Element note = parseNote("<note><dot/><dot/></note>");

        assertEquals(2, ScoreDurations.countMusicXmlDots(note));
    }

    private static Element parseNote(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element note = (Element) doc.getElementsByTagName("note").item(0);
        assertNotNull(note);
        return note;
    }
}
