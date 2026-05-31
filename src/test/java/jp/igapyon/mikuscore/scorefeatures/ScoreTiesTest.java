/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.scorefeatures.ScoreTies.TieState;

public class ScoreTiesTest {
    @Test
    public void buildsMusicXmlTieAndTiedItems() {
        assertEquals("<tie type=\"stop\"/><tie type=\"start\"/>",
                ScoreTies.buildMusicXmlTieItemsXml(new TieState(true, true, false, false)));
        assertEquals("<tied type=\"start\"/>",
                ScoreTies.buildMusicXmlTiedItemsXml(new TieState(false, false, true, false)));
    }

    @Test
    public void extractsSoundTieAndNotationTiedStateSeparately() {
        Element note = parseNote("<tie type=\"start\"/><notations><tied type=\"stop\"/></notations>");

        TieState state = ScoreTies.extractMusicXmlTieState(note);
        assertEquals(true, state.isTieStart());
        assertEquals(false, state.isTieStop());
        assertEquals(false, state.isTiedStart());
        assertEquals(true, state.isTiedStop());
    }

    private static Element parseNote(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<note>" + xml + "</note>");
        assertNotNull(doc);
        Element note = doc.getDocumentElement();
        assertNotNull(note);
        return note;
    }
}
