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
import jp.igapyon.mikuscore.scorefeatures.ScoreTimeSignatures.TimeSignatureFeature;

public class ScoreTimeSignaturesTest {
    @Test
    public void normalizesPositiveBeatsAndBeatTypeValues() {
        TimeSignatureFeature normalized = ScoreTimeSignatures
                .normalizeTimeSignatureFeature(new TimeSignatureFeature(Double.valueOf(3.2), "4", "cut"));

        assertNotNull(normalized);
        assertEquals(Integer.valueOf(3), normalized.getBeats());
        assertEquals(Integer.valueOf(4), normalized.getBeatType());
        assertEquals("cut", normalized.getSymbol());
        assertNull(ScoreTimeSignatures
                .normalizeTimeSignatureFeature(new TimeSignatureFeature(Integer.valueOf(0), Integer.valueOf(4))));
    }

    @Test
    public void buildsMusicXmlTimeSignatureFeatures() {
        assertEquals("<time symbol=\"common\"><beats>6</beats><beat-type>8</beat-type></time>",
                ScoreTimeSignatures.buildMusicXmlTimeSignatureXml(
                        new TimeSignatureFeature(Integer.valueOf(6), Integer.valueOf(8), "common")));
    }

    @Test
    public void extractsMusicXmlTimeSignatureFeatures() {
        Element time = parseTime("<time symbol=\"cut\"><beats>2</beats><beat-type>2</beat-type></time>");

        TimeSignatureFeature feature = ScoreTimeSignatures.extractMusicXmlTimeSignatureFeature(time);
        assertNotNull(feature);
        assertEquals(Integer.valueOf(2), feature.getBeats());
        assertEquals(Integer.valueOf(2), feature.getBeatType());
        assertEquals("cut", feature.getSymbol());
    }

    private static Element parseTime(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element time = (Element) doc.getElementsByTagName("time").item(0);
        assertNotNull(time);
        return time;
    }
}
