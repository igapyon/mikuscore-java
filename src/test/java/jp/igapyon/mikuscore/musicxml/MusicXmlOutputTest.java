/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MusicXmlOutputTest {
    @Test
    public void retainsOriginalTextWhenAllMetadataFamiliesAreKept() {
        assertEquals(metadataFixture(), MusicXmlOutput.stripMetadataFromMusicXml(metadataFixture(),
                new MusicXmlOutput.MksMetadataOutputSettings(true, true, true)));
    }

    @Test
    public void removesSelectedMetadataFamiliesAndPrunesEmptyContainers() {
        String output = MusicXmlOutput.stripMetadataFromMusicXml(metadataFixture(),
                new MusicXmlOutput.MksMetadataOutputSettings(false, true, false));
        Document doc = MusicXmlIo.parseMusicXmlDocument(output);

        assertNull(findField(doc, "mks:meta:test"));
        assertEquals("source", findField(doc, "mks:src:test").getTextContent());
        assertNull(findField(doc, "mks:dbg:test"));
        assertEquals("code=OTHER_WARNING", findField(doc, "mks:diag:0001").getTextContent());
        assertEquals("other", findField(doc, "other:test").getTextContent());
        assertNull(findMeasure(doc, "2").getElementsByTagName("attributes").item(0));
    }

    @Test
    public void retainsInvalidMetadataInputUnchanged() {
        assertEquals("<not-musicxml", MusicXmlOutput.stripMetadataFromMusicXml("<not-musicxml",
                new MusicXmlOutput.MksMetadataOutputSettings(false, false, false)));
    }

    @Test
    public void summarizesExistingAbcDiagnosticWarningCategories() {
        String xml = "<score-partwise>"
                + "<miscellaneous-field name=\"mks:diag:count\">4</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0001\">code=OVERFULL_REFLOWED;message=one</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0002\">message=two;code=overfull_reflowed</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0003\">code=ABC_IMPORT_WARNING</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0004\">code=OTHER_WARNING</miscellaneous-field>"
                + "</score-partwise>";

        assertEquals("ABC overfull auto-reflow: 2 / ABC parser warnings: 1",
                MusicXmlOutput.summarizeImportedDiagWarnings(xml));
    }

    @Test
    public void returnsEmptyDiagnosticSummaryForInvalidMusicXml() {
        assertEquals("", MusicXmlOutput.summarizeImportedDiagWarnings("<score-partwise"));
    }

    private static Element findField(Document doc, String name) {
        if (doc == null) {
            return null;
        }
        org.w3c.dom.NodeList fields = doc.getElementsByTagName("miscellaneous-field");
        for (int index = 0; index < fields.getLength(); index++) {
            Element field = (Element) fields.item(index);
            if (name.equals(field.getAttribute("name"))) {
                return field;
            }
        }
        return null;
    }

    private static Element findMeasure(Document doc, String number) {
        org.w3c.dom.NodeList measures = doc.getElementsByTagName("measure");
        for (int index = 0; index < measures.getLength(); index++) {
            Element measure = (Element) measures.item(index);
            if (number.equals(measure.getAttribute("number"))) {
                return measure;
            }
        }
        return null;
    }

    private static String metadataFixture() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>Part 1</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes>"
                + "<divisions>480</divisions><miscellaneous>"
                + "<miscellaneous-field name=\"mks:meta:test\">meta</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:src:test\">source</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:dbg:test\">debug</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0001\">code=OTHER_WARNING</miscellaneous-field>"
                + "<miscellaneous-field name=\"other:test\">other</miscellaneous-field>"
                + "</miscellaneous></attributes></measure><measure number=\"2\"><attributes><miscellaneous>"
                + "<miscellaneous-field name=\"mks:dbg:only\">debug only</miscellaneous-field>"
                + "</miscellaneous></attributes></measure></part></score-partwise>";
    }
}
