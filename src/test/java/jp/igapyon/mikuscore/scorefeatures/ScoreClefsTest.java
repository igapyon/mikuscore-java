/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.scorefeatures.ScoreClefs.ClefFeature;

public class ScoreClefsTest {
    @Test
    public void normalizesClefSignLineAndOptionalNumber() {
        ClefFeature normalized = ScoreClefs
                .normalizeClefFeature(new ClefFeature(" G ", Double.valueOf(2.2), " 1 "));

        assertNotNull(normalized);
        assertEquals("G", normalized.getSign());
        assertEquals(Integer.valueOf(2), normalized.getLine());
        assertEquals("1", normalized.getNumber());
        assertNull(ScoreClefs.normalizeClefFeature(new ClefFeature("", Integer.valueOf(2))));
    }

    @Test
    public void buildsMusicXmlClefFeatures() {
        assertEquals("<clef number=\"2\"><sign>F</sign><line>4</line></clef>",
                ScoreClefs.buildMusicXmlClefXml(new ClefFeature("F", Integer.valueOf(4), Integer.valueOf(2))));
    }

    @Test
    public void extractsMusicXmlClefFeatures() {
        Element clef = parseClef("<clef number=\"1\"><sign>C</sign><line>3</line></clef>");

        ClefFeature feature = ScoreClefs.extractMusicXmlClefFeature(clef);
        assertNotNull(feature);
        assertEquals("C", feature.getSign());
        assertEquals(Integer.valueOf(3), feature.getLine());
        assertEquals("1", feature.getNumber());
    }

    private static Element parseClef(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element clef = (Element) doc.getElementsByTagName("clef").item(0);
        assertNotNull(clef);
        return clef;
    }
}
