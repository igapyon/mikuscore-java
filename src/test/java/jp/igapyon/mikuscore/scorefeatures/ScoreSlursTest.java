/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.scorefeatures.ScoreSlurs.SlurFeature;

public class ScoreSlursTest {
    @Test
    public void buildsMusicXmlSlurItems() {
        assertEquals("<slur type=\"start\" number=\"2\" placement=\"above\"/>",
                ScoreSlurs.buildMusicXmlSlurXml(new SlurFeature("start", Integer.valueOf(2), "above")));
        assertEquals("<slur type=\"start\"/><slur type=\"stop\" number=\"1\"/>",
                ScoreSlurs.buildMusicXmlSlursXml(
                        Arrays.asList(new SlurFeature("start"), new SlurFeature("stop", Integer.valueOf(1)))));
    }

    @Test
    public void extractsSupportedMusicXmlSlurFeatures() {
        Element note = parseNote(
                "<slur type=\"start\" number=\"2\" placement=\"below\"/><slur type=\"stop\" number=\"2\"/><slur type=\"continue\"/>");

        List<SlurFeature> features = ScoreSlurs.extractMusicXmlSlurFeatures(note);
        assertEquals(2, features.size());
        assertEquals("start", features.get(0).getType());
        assertEquals(Integer.valueOf(2), features.get(0).getNumber());
        assertEquals("below", features.get(0).getPlacement());
        assertEquals("stop", features.get(1).getType());
        assertEquals(Integer.valueOf(2), features.get(1).getNumber());
        assertEquals(null, features.get(1).getPlacement());
    }

    private static Element parseNote(String slursXml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<note><notations>" + slursXml + "</notations></note>");
        assertNotNull(doc);
        Element note = doc.getDocumentElement();
        assertNotNull(note);
        return note;
    }
}
