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
import jp.igapyon.mikuscore.scorefeatures.ScoreDirectionText.DirectionTempoFeature;
import jp.igapyon.mikuscore.scorefeatures.ScoreDirectionText.DirectionWord;
import jp.igapyon.mikuscore.scorefeatures.ScoreDirectionText.DirectionWordsFeature;

public class ScoreDirectionTextTest {
    @Test
    public void normalizesAndFormatsTempo() {
        assertEquals(Double.valueOf(120.5), ScoreDirectionText.normalizeTempoBpm("120.5"));
        assertNull(ScoreDirectionText.normalizeTempoBpm("0"));
        assertEquals("120", ScoreDirectionText.formatTempoBpm(Integer.valueOf(120)));
        assertEquals("92.5", ScoreDirectionText.formatTempoBpm(Double.valueOf(92.5)));
        assertEquals("92.35", ScoreDirectionText.formatTempoBpm(Double.valueOf(92.345)));
    }

    @Test
    public void buildsWordsDirectionXml() {
        assertEquals(
                "<direction placement=\"above\"><direction-type><words font-style=\"italic\">Allegro &amp; bright</words></direction-type><offset>3</offset><voice>1</voice><staff>2</staff><sound tempo=\"120.5\"/></direction>",
                ScoreDirectionText.buildMusicXmlWordsDirectionXml(new DirectionWordsFeature(" Allegro & bright ",
                        "above", "italic", Double.valueOf(120.5), Double.valueOf(2.6), "1", Integer.valueOf(2))));
        assertEquals("", ScoreDirectionText.buildMusicXmlWordsDirectionXml(new DirectionWordsFeature(" ")));
    }

    @Test
    public void buildsTempoDirectionXml() {
        assertEquals(
                "<direction placement=\"below\"><direction-type><words>Andante</words></direction-type><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>76</per-minute></metronome></direction-type><offset>2</offset><voice>2</voice><staff>1</staff><sound tempo=\"76\"/></direction>",
                ScoreDirectionText.buildMusicXmlTempoDirectionXml(new DirectionTempoFeature(Integer.valueOf(76),
                        "Andante", "below", Integer.valueOf(2), "2", "1", true)));
    }

    @Test
    public void extractsWordsTempoAndPlacement() {
        Element direction = parseElement(
                "<direction placement=\"above\"><direction-type><words font-style=\"normal\">dolce</words></direction-type><direction-type><words font-style=\"unsupported\">ignored style</words></direction-type><sound tempo=\"88.5\"/></direction>");

        List<DirectionWord> words = ScoreDirectionText.extractMusicXmlDirectionWords(direction);
        assertEquals(2, words.size());
        assertEquals("dolce", words.get(0).getText());
        assertEquals("normal", words.get(0).getFontStyle());
        assertEquals("ignored style", words.get(1).getText());
        assertNull(words.get(1).getFontStyle());
        assertEquals(Double.valueOf(88.5), ScoreDirectionText.extractMusicXmlSoundTempoBpm(direction));
        assertEquals("above", ScoreDirectionText.extractMusicXmlDirectionPlacement(direction));
        assertEquals(Double.valueOf(72.0), ScoreDirectionText.extractMusicXmlSoundTempoBpm(parseElement("<sound tempo=\"72\"/>")));
    }

    private static Element parseElement(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        assertNotNull(doc);
        Element element = doc.getDocumentElement();
        assertNotNull(element);
        return element;
    }
}
