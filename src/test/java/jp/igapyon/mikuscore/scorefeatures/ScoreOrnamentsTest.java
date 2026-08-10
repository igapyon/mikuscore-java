/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.scorefeatures.ScoreOrnaments.OrnamentFeature;

public class ScoreOrnamentsTest {
    @Test
    public void normalizesSupportedOrnamentKinds() {
        assertEquals("trill-mark", ScoreOrnaments.normalizeOrnamentKind("Trill-Mark"));
        assertEquals("inverted-mordent", ScoreOrnaments.normalizeOrnamentKind("inverted-mordent"));
        assertNull(ScoreOrnaments.normalizeOrnamentKind("wavy-line"));
    }

    @Test
    public void buildsMusicXmlOrnamentItemsWithStableDeduplication() {
        assertEquals("<trill-mark/><turn slash=\"yes\"/><tremolo type=\"single\">3</tremolo>",
                ScoreOrnaments.buildMusicXmlOrnamentItemsXml(Arrays.asList(new OrnamentFeature("trill-mark"),
                        new OrnamentFeature("turn", true), new OrnamentFeature("trill-mark"),
                        OrnamentFeature.tremolo("single", Integer.valueOf(3)))));
        assertEquals("<ornaments><mordent/></ornaments>",
                ScoreOrnaments.buildMusicXmlOrnamentsXml(Arrays.asList(new OrnamentFeature("mordent"))));
    }

    @Test
    public void builderRetainsUpstreamRuntimeKindAndTremoloTypeValues() {
        assertEquals("<Trill-Mark/><tremolo type=\"START\">2</tremolo>",
                ScoreOrnaments.buildMusicXmlOrnamentItemsXml(Arrays.asList(new OrnamentFeature("Trill-Mark"),
                        OrnamentFeature.tremolo("START", "0x2"))));
    }

    @Test
    public void extractsSupportedMusicXmlOrnamentFeatures() {
        Element note = parseNote(
                "<ornaments><trill-mark/><turn slash=\"yes\"/><tremolo type=\"start\">2</tremolo><wavy-line type=\"start\"/></ornaments>");

        List<OrnamentFeature> features = ScoreOrnaments.extractMusicXmlOrnamentFeatures(note);
        assertEquals(3, features.size());
        assertEquals("trill-mark", features.get(0).getKind());
        assertEquals("turn", features.get(1).getKind());
        assertEquals(true, features.get(1).isSlash());
        assertEquals("tremolo", features.get(2).getKind());
        assertEquals("start", features.get(2).getTremoloType());
        assertEquals(Integer.valueOf(2), features.get(2).getMarks());
    }

    private static Element parseNote(String ornamentsXml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<note><notations>" + ornamentsXml + "</notations></note>");
        assertNotNull(doc);
        Element note = doc.getDocumentElement();
        assertNotNull(note);
        return note;
    }
}
