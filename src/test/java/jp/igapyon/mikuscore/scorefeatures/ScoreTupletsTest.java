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
import jp.igapyon.mikuscore.scorefeatures.ScoreTuplets.TimeModificationFeature;

public class ScoreTupletsTest {
    @Test
    public void normalizesPositiveTimeModificationValues() {
        TimeModificationFeature normalized = ScoreTuplets
                .normalizeTimeModificationFeature(new TimeModificationFeature(Double.valueOf(3.4), Double.valueOf(2.2)));

        assertNotNull(normalized);
        assertEquals(Integer.valueOf(3), normalized.getActualNotes());
        assertEquals(Integer.valueOf(2), normalized.getNormalNotes());
        assertNull(ScoreTuplets
                .normalizeTimeModificationFeature(new TimeModificationFeature(Integer.valueOf(0), Integer.valueOf(2))));
    }

    @Test
    public void buildsMusicXmlTimeModificationFeatures() {
        assertEquals(
                "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>",
                ScoreTuplets.buildMusicXmlTimeModificationXml(
                        new TimeModificationFeature(Integer.valueOf(3), Integer.valueOf(2))));
    }

    @Test
    public void extractsMusicXmlTimeModificationFeatures() {
        Element note = parseNote(
                "<note><time-modification><actual-notes>5</actual-notes><normal-notes>4</normal-notes></time-modification></note>");

        TimeModificationFeature feature = ScoreTuplets.extractMusicXmlTimeModificationFeature(note);
        assertNotNull(feature);
        assertEquals(Integer.valueOf(5), feature.getActualNotes());
        assertEquals(Integer.valueOf(4), feature.getNormalNotes());
    }

    private static Element parseNote(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element note = (Element) doc.getElementsByTagName("note").item(0);
        assertNotNull(note);
        return note;
    }
}
