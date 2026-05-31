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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public class ScoreArticulationsTest {
    @Test
    public void normalizesSupportedArticulationKinds() {
        assertEquals("accent", ScoreArticulations.normalizeArticulationKind("Accent"));
        assertEquals("breath-mark", ScoreArticulations.normalizeArticulationKind("breath-mark"));
        assertNull(ScoreArticulations.normalizeArticulationKind("trill-mark"));
    }

    @Test
    public void buildsMusicXmlArticulationsWithStableDeduplication() {
        assertEquals("<staccato/><accent/>", ScoreArticulations
                .buildMusicXmlArticulationItemsXml(Arrays.asList("staccato", "accent", "staccato")));
        assertEquals("<articulations><staccato/><accent/></articulations>", ScoreArticulations
                .buildMusicXmlArticulationsXml(Arrays.asList("staccato", "accent", "staccato")));
    }

    @Test
    public void extractsSupportedMusicXmlArticulationKinds() {
        Element note = parseNote(
                "<articulations><staccato/><accent/><caesura/><other-articulation>x</other-articulation></articulations>");

        assertEquals(Arrays.asList("staccato", "accent", "caesura"),
                ScoreArticulations.extractMusicXmlArticulationKinds(note));
    }

    private static Element parseNote(String articulationsXml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<note><notations>" + articulationsXml + "</notations></note>");
        assertNotNull(doc);
        Element note = doc.getDocumentElement();
        assertNotNull(note);
        return note;
    }
}
