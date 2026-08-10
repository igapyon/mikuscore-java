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
import jp.igapyon.mikuscore.scorefeatures.ScorePitches.PitchFeature;

public class ScorePitchesTest {
    @Test
    public void normalizesMusicXmlPitchValues() {
        PitchFeature normalized = ScorePitches.normalizePitchFeature(new PitchFeature(" f ", "-1", Double.valueOf(4.4)));

        assertEquals("F", normalized.getStep());
        assertEquals(Integer.valueOf(-1), normalized.getAlter());
        assertEquals(Integer.valueOf(4), normalized.getOctave());

        PitchFeature fallback = ScorePitches.normalizePitchFeature(new PitchFeature("x", null, Integer.valueOf(20)));
        assertEquals("C", fallback.getStep());
        assertNull(fallback.getAlter());
        assertEquals(Integer.valueOf(9), fallback.getOctave());
    }

    @Test
    public void buildsMusicXmlPitchFeatures() {
        assertEquals("<pitch><step>C</step><octave>4</octave></pitch>",
                ScorePitches.buildMusicXmlPitchXml(new PitchFeature("C", null, Integer.valueOf(4))));
        assertEquals("<pitch><step>F</step><alter>1</alter><octave>5</octave></pitch>",
                ScorePitches.buildMusicXmlPitchXml(new PitchFeature("F", Integer.valueOf(1), Integer.valueOf(5))));
    }

    @Test
    public void normalizesJavaScriptBooleanAndRadixNumberInputs() {
        PitchFeature normalized = ScorePitches
                .normalizePitchFeature(new PitchFeature("g", Boolean.TRUE, "0x5"));

        assertEquals("G", normalized.getStep());
        assertEquals(Integer.valueOf(1), normalized.getAlter());
        assertEquals(Integer.valueOf(5), normalized.getOctave());
    }

    @Test
    public void extractsMusicXmlPitchFeatures() {
        Element pitch = parsePitch("<pitch><step>B</step><alter>-1</alter><octave>3</octave></pitch>");

        PitchFeature feature = ScorePitches.extractMusicXmlPitchFeature(pitch);
        assertNotNull(feature);
        assertEquals("B", feature.getStep());
        assertEquals(Integer.valueOf(-1), feature.getAlter());
        assertEquals(Integer.valueOf(3), feature.getOctave());
    }

    @Test
    public void extractionUsesEmptyStringNumberCoercionForMissingDirectChildren() {
        PitchFeature feature = ScorePitches.extractMusicXmlPitchFeature(parsePitch("<pitch><step>D</step></pitch>"));

        assertEquals("D", feature.getStep());
        assertNull(feature.getAlter());
        assertEquals(Integer.valueOf(0), feature.getOctave());
    }

    private static Element parsePitch(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element pitch = (Element) doc.getElementsByTagName("pitch").item(0);
        assertNotNull(pitch);
        return pitch;
    }
}
