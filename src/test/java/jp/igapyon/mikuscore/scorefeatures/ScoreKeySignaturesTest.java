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
import jp.igapyon.mikuscore.scorefeatures.ScoreKeySignatures.KeySignatureFeature;

public class ScoreKeySignaturesTest {
    @Test
    public void normalizesFifthsAndOptionalMode() {
        KeySignatureFeature normalized = ScoreKeySignatures
                .normalizeKeySignatureFeature(new KeySignatureFeature(Double.valueOf(-2.4), " Minor "));

        assertNotNull(normalized);
        assertEquals(Integer.valueOf(-2), normalized.getFifths());
        assertEquals("minor", normalized.getMode());
        assertNull(ScoreKeySignatures.normalizeKeySignatureFeature(new KeySignatureFeature(Double.valueOf(Double.NaN))));
    }

    @Test
    public void buildsMusicXmlKeySignatureFeatures() {
        assertEquals("<key><fifths>3</fifths><mode>major</mode></key>",
                ScoreKeySignatures
                        .buildMusicXmlKeySignatureXml(new KeySignatureFeature(Integer.valueOf(3), "major")));
        assertEquals("<key><fifths>-1</fifths></key>",
                ScoreKeySignatures.buildMusicXmlKeySignatureXml(new KeySignatureFeature(Integer.valueOf(-1))));
    }

    @Test
    public void extractsMusicXmlKeySignatureFeatures() {
        Element key = parseKey("<key><fifths>1</fifths><mode>minor</mode></key>");

        KeySignatureFeature feature = ScoreKeySignatures.extractMusicXmlKeySignatureFeature(key);
        assertNotNull(feature);
        assertEquals(Integer.valueOf(1), feature.getFifths());
        assertEquals("minor", feature.getMode());
    }

    private static Element parseKey(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element key = (Element) doc.getElementsByTagName("key").item(0);
        assertNotNull(key);
        return key;
    }
}
