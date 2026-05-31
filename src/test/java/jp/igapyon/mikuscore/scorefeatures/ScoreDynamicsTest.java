/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.scorefeatures.ScoreDynamics.DynamicFeature;

public class ScoreDynamicsTest {
    @Test
    public void normalizesMarksAndMapsVelocity() {
        assertEquals("mf", ScoreDynamics.normalizeDynamicMark(" MF "));
        assertEquals("sffz", ScoreDynamics.normalizeDynamicMark("SFFZ"));
        assertNull(ScoreDynamics.normalizeDynamicMark("forte"));

        assertEquals("ppp", ScoreDynamics.velocityToDynamicMark(Integer.valueOf(1)));
        assertEquals("mp", ScoreDynamics.velocityToDynamicMark(Integer.valueOf(63)));
        assertEquals("mf", ScoreDynamics.velocityToDynamicMark("64"));
        assertEquals("fff", ScoreDynamics.velocityToDynamicMark(Integer.valueOf(127)));
    }

    @Test
    public void buildsMusicXmlDirectionFeatureXml() {
        assertEquals(
                "<direction placement=\"below\"><direction-type><dynamics><mf/></dynamics></direction-type><offset>3</offset><voice>1</voice><staff>2</staff></direction>",
                ScoreDynamics.buildMusicXmlDirectionFeatureXml(
                        DynamicFeature.dynamic("mf", Double.valueOf(2.6), "1", Integer.valueOf(2), "below")));
        assertEquals(
                "<direction placement=\"above\"><direction-type><wedge type=\"crescendo\" number=\"hairpin &amp; 1\"/></direction-type></direction>",
                ScoreDynamics.buildMusicXmlDirectionFeatureXml(
                        DynamicFeature.wedge("crescendo", "hairpin & 1", null, null, null, "above")));
    }

    @Test
    public void extractsMusicXmlDirectionFeatures() {
        Element direction = parseDirection(
                "<direction placement=\"above\"><direction-type><dynamics><mf/><unknown/></dynamics></direction-type><direction-type><wedge type=\"diminuendo\" number=\"2\"/></direction-type><offset>4</offset><voice>1</voice><staff>2</staff></direction>");

        List<DynamicFeature> features = ScoreDynamics.extractMusicXmlDirectionFeatures(direction);
        assertEquals(2, features.size());
        assertEquals("dynamic", features.get(0).getKind());
        assertEquals("mf", features.get(0).getMark());
        assertEquals(Integer.valueOf(4), features.get(0).getOffsetDiv());
        assertEquals("1", features.get(0).getVoice());
        assertEquals("2", features.get(0).getStaff());
        assertEquals("above", features.get(0).getPlacement());
        assertEquals("wedge", features.get(1).getKind());
        assertEquals("diminuendo", features.get(1).getWedgeType());
        assertEquals("2", features.get(1).getNumber());
    }

    private static Element parseDirection(String directionXml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(directionXml);
        assertNotNull(doc);
        Element direction = doc.getDocumentElement();
        assertNotNull(direction);
        return direction;
    }
}
