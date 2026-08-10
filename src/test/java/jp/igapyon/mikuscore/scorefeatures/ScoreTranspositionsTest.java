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
import jp.igapyon.mikuscore.scorefeatures.ScoreTranspositions.TranspositionFeature;

public class ScoreTranspositionsTest {
    @Test
    public void normalizesFiniteDiatonicAndChromaticValues() {
        TranspositionFeature normalized = ScoreTranspositions
                .normalizeTranspositionFeature(new TranspositionFeature(Double.valueOf(1.4), "-2"));

        assertNotNull(normalized);
        assertEquals(Integer.valueOf(1), normalized.getDiatonic());
        assertEquals(Integer.valueOf(-2), normalized.getChromatic());
        assertNull(ScoreTranspositions
                .normalizeTranspositionFeature(new TranspositionFeature(Double.valueOf(Double.NaN), null)));
    }

    @Test
    public void normalizesJavaScriptBooleanAndRadixNumberInputs() {
        TranspositionFeature normalized = ScoreTranspositions
                .normalizeTranspositionFeature(new TranspositionFeature(Boolean.TRUE, "0x10"));

        assertNotNull(normalized);
        assertEquals(Integer.valueOf(1), normalized.getDiatonic());
        assertEquals(Integer.valueOf(16), normalized.getChromatic());
    }

    @Test
    public void buildsMusicXmlTransposeFeatures() {
        assertEquals("<transpose><diatonic>1</diatonic><chromatic>2</chromatic></transpose>",
                ScoreTranspositions.buildMusicXmlTransposeXml(
                        new TranspositionFeature(Integer.valueOf(1), Integer.valueOf(2))));
        assertEquals("<transpose><chromatic>-1</chromatic></transpose>",
                ScoreTranspositions.buildMusicXmlTransposeXml(new TranspositionFeature(null, Integer.valueOf(-1))));
    }

    @Test
    public void extractsMusicXmlTransposeFeatures() {
        Element transpose = parseTranspose("<transpose><diatonic>-1</diatonic><chromatic>-2</chromatic></transpose>");

        TranspositionFeature feature = ScoreTranspositions.extractMusicXmlTranspositionFeature(transpose);
        assertNotNull(feature);
        assertEquals(Integer.valueOf(-1), feature.getDiatonic());
        assertEquals(Integer.valueOf(-2), feature.getChromatic());
    }

    @Test
    public void extractionUsesZeroForMissingDirectChildrenLikeTheUpstreamEmptyStringFallback() {
        TranspositionFeature feature = ScoreTranspositions
                .extractMusicXmlTranspositionFeature(parseTranspose("<transpose><diatonic>-1</diatonic></transpose>"));

        assertNotNull(feature);
        assertEquals(Integer.valueOf(-1), feature.getDiatonic());
        assertEquals(Integer.valueOf(0), feature.getChromatic());
    }

    private static Element parseTranspose(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element transpose = (Element) doc.getElementsByTagName("transpose").item(0);
        assertNotNull(transpose);
        return transpose;
    }
}
