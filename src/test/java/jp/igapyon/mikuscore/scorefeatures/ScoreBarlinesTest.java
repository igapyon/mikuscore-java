/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.scorefeatures.ScoreBarlines.BarlineFeature;
import jp.igapyon.mikuscore.scorefeatures.ScoreBarlines.EndingFeature;

public class ScoreBarlinesTest {
    @Test
    public void buildsMusicXmlBarlineFeatures() {
        BarlineFeature feature = new BarlineFeature().setLocation("right").setBarStyle("light-heavy")
                .addRepeat("backward").setEnding(new EndingFeature(Integer.valueOf(2), "stop"));

        assertEquals(
                "<barline location=\"right\"><bar-style>light-heavy</bar-style><repeat direction=\"backward\"/><ending number=\"2\" type=\"stop\"/></barline>",
                ScoreBarlines.buildMusicXmlBarlineXml(feature));
    }

    @Test
    public void extractsMusicXmlBarlineFeatures() {
        Element barline = parseBarline(
                "<barline location=\"middle\"><bar-style>light-heavy</bar-style><repeat direction=\"backward\"/><repeat direction=\"forward\"/></barline>");

        BarlineFeature feature = ScoreBarlines.extractMusicXmlBarlineFeature(barline);
        assertEquals("middle", feature.getLocation());
        assertEquals("light-heavy", feature.getBarStyle());
        assertEquals(Arrays.asList("backward", "forward"), feature.getRepeats());
    }

    @Test
    public void buildPreservesTypeCorrectFeatureValuesWithoutExtraNormalization() {
        BarlineFeature feature = new BarlineFeature().setLocation("RIGHT").setBarStyle("<&")
                .addRepeat("BACKWARD").setEnding(new EndingFeature(" 2 ", "STOP"));

        assertEquals(
                "<barline location=\"RIGHT\"><bar-style>&lt;&amp;</bar-style><repeat direction=\"BACKWARD\"/><ending number=\" 2 \" type=\"STOP\"/></barline>",
                ScoreBarlines.buildMusicXmlBarlineXml(feature));
    }

    @Test
    public void extractionUsesFirstDirectOptionalNodesAndJavaScriptCompatibleNormalization() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            Element barline = parseBarline(
                    "<barline location=\"MIDDLE\"><bar-style> </bar-style><bar-style>light-heavy</bar-style><repeat direction=\"BACKWARD\"/><repeat direction=\"sideways\"/><ending type=\"unknown\" number=\"1\"/><ending type=\"stop\" number=\"2\"/></barline>");

            BarlineFeature feature = ScoreBarlines.extractMusicXmlBarlineFeature(barline);
            assertEquals("middle", feature.getLocation());
            assertEquals(null, feature.getBarStyle());
            assertEquals(Arrays.asList("backward"), feature.getRepeats());
            assertEquals(null, feature.getEnding());
        } finally {
            Locale.setDefault(original);
        }
    }

    private static Element parseBarline(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<root>" + xml + "</root>");
        assertNotNull(doc);
        Element barline = (Element) doc.getElementsByTagName("barline").item(0);
        assertNotNull(barline);
        return barline;
    }
}
