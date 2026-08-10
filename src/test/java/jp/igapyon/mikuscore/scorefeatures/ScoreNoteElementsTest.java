/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuscore.scorefeatures.ScoreNoteElements.AccidentalFeature;
import jp.igapyon.mikuscore.scorefeatures.ScoreNoteElements.GraceFeature;
import jp.igapyon.mikuscore.scorefeatures.ScoreNoteElements.LyricFeature;

public class ScoreNoteElementsTest {
    @Test
    public void normalizesAccidentalTextAndFlags() {
        AccidentalFeature normalized = ScoreNoteElements
                .normalizeAccidentalFeature(new AccidentalFeature(" sharp ", true, false));

        assertNotNull(normalized);
        assertEquals("sharp", normalized.getText());
        assertEquals(true, normalized.isEditorial());
        assertNull(ScoreNoteElements.normalizeAccidentalFeature(new AccidentalFeature("")));
    }

    @Test
    public void buildsMusicXmlAccidentals() {
        assertEquals("<accidental cautionary=\"yes\">flat</accidental>",
                ScoreNoteElements.buildMusicXmlAccidentalXml(new AccidentalFeature("flat", false, true)));
        assertEquals("<accidental>sharp &amp; flat</accidental>",
                ScoreNoteElements.buildMusicXmlAccidentalXml(new AccidentalFeature("sharp & flat")));
    }

    @Test
    public void buildsMusicXmlGraceItems() {
        assertEquals("<grace/>", ScoreNoteElements.buildMusicXmlGraceXml());
        assertEquals("<grace/>", ScoreNoteElements.buildMusicXmlGraceXml(null));
        assertEquals("<grace slash=\"yes\"/>", ScoreNoteElements.buildMusicXmlGraceXml(new GraceFeature(true)));
    }

    @Test
    public void buildsMusicXmlStemItems() {
        assertEquals("<stem>up</stem>", ScoreNoteElements.buildMusicXmlStemXml("up"));
        assertEquals("", ScoreNoteElements.buildMusicXmlStemXml("sideways"));
    }

    @Test
    public void normalizesAndBuildsMusicXmlLyricItems() {
        LyricFeature normalized = ScoreNoteElements.normalizeLyricFeature(new LyricFeature(" la ", "single", true));

        assertNotNull(normalized);
        assertEquals("la", normalized.getText());
        assertEquals("single", normalized.getSyllabic());
        assertEquals(true, normalized.isExtend());
        assertEquals("<lyric><syllabic>begin</syllabic><text>a &amp; b</text></lyric>",
                ScoreNoteElements.buildMusicXmlLyricXml(new LyricFeature("a & b", "begin", false)));
    }

    @Test
    public void buildsMusicXmlTechnicalItems() {
        assertEquals("<fingering>2</fingering>", ScoreNoteElements.buildMusicXmlFingeringXml(" 2 "));
        assertEquals("<string>3.4</string>", ScoreNoteElements.buildMusicXmlStringNumberXml(Double.valueOf(3.4)));
        assertEquals("<string>3</string>",
                ScoreNoteElements.buildMusicXmlStringNumberXml(Double.valueOf(3.4), true));
        assertEquals("<string>1</string>", ScoreNoteElements.buildMusicXmlStringNumberXml(Boolean.TRUE, true));
        assertEquals("<string>16</string>", ScoreNoteElements.buildMusicXmlStringNumberXml("0x10", true));
        assertEquals("<technical><fingering>2</fingering></technical>",
                ScoreNoteElements.buildMusicXmlTechnicalXml(Arrays.asList("<fingering>2</fingering>", "")));
    }
}
