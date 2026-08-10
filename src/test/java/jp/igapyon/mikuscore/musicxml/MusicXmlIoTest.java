/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MusicXmlIoTest {
    @Test
    public void parsesValidMusicXmlDocument() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(minimalMusicXmlWithoutPartList("Parse"));

        assertNotNull(doc);
        assertEquals("score-partwise", doc.getDocumentElement().getTagName());
    }

    @Test
    public void returnsNullForInvalidMusicXmlDocument() {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<score-partwise");

        assertNull(doc);
    }

    @Test
    public void normalizesMissingPartListAndPartId() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(minimalMusicXmlWithoutPartList("Normalize"));

        assertTrue(normalized.contains("<part-list>"));
        assertTrue(normalized.contains("<score-part id=\"P1\">"));
        assertTrue(normalized.contains("<part-name>Music</part-name>"));
        assertTrue(normalized.contains("<part id=\"P1\">"));
    }

    @Test
    public void normalizesScorePartwiseElementsNestedUnderAnotherXmlRoot() {
        String bareMusicXml = minimalMusicXmlWithoutPartList("Nested");
        String source = "<wrapper>" + bareMusicXml.substring(bareMusicXml.indexOf("<score-partwise")) + "</wrapper>";

        Document doc = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.normalizeImportedMusicXmlText(source));

        assertEquals("wrapper", doc.getDocumentElement().getTagName());
        assertEquals(1, doc.getElementsByTagName("part-list").getLength());
    }

    @Test
    public void addsTupletStartAndStopNotationsFromTimeModification() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(tupletMusicXml(false));
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        Element firstTuplet = firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet");
        Element thirdTuplet = firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet");
        assertEquals("start", firstTuplet.getAttribute("type"));
        assertEquals("1", firstTuplet.getAttribute("number"));
        assertEquals("yes", firstTuplet.getAttribute("bracket"));
        assertEquals("actual", firstTuplet.getAttribute("show-number"));
        assertEquals("stop", thirdTuplet.getAttribute("type"));
        assertEquals("1", thirdTuplet.getAttribute("number"));
    }

    @Test
    public void enrichesTupletsForStandalonePartsOutsideScorePartwise() {
        String fullScore = tupletMusicXml(false);
        int partStart = fullScore.indexOf("<part id=\"P1\">");
        int partEnd = fullScore.indexOf("</part>", partStart) + "</part>".length();
        String source = "<wrapper>" + fullScore.substring(partStart, partEnd) + "</wrapper>";

        Document doc = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.normalizeImportedMusicXmlText(source));

        assertEquals("start", firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet")
                .getAttribute("type"));
        assertEquals("stop", firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet")
                .getAttribute("type"));
    }

    @Test
    public void roundsJavaScriptNumericTupletSignaturesBeforeGrouping() {
        String source = tupletMusicXml(false).replace("<actual-notes>3</actual-notes><normal-notes>2</normal-notes>",
                "<actual-notes>0x3</actual-notes><normal-notes>2.4</normal-notes>");
        Document doc = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.normalizeImportedMusicXmlText(source));

        assertEquals("start", firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet")
                .getAttribute("type"));
        assertEquals("stop", firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet")
                .getAttribute("type"));
    }

    @Test
    public void usesRawTupletAttributesForExistingNotationSelection() {
        String source = tupletMusicXml(true).replace("type=\"start\" number=\"7\"",
                "type=\" start \" number=\" \"")
                .replace("type=\"stop\" number=\"7\"", "type=\"stop\" number=\" \"");
        Document doc = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.normalizeImportedMusicXmlText(source));
        Element firstNotations = directChild(noteAt(doc, 0), "notations");
        Element lastTuplet = firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet");

        assertEquals(2, countDirectChildren(firstNotations, "tuplet"));
        assertEquals(" start ", firstDirectChild(firstNotations, "tuplet").getAttribute("type"));
        assertEquals(" ", firstDirectChild(firstNotations, "tuplet").getAttribute("number"));
        assertEquals(" ", lastTuplet.getAttribute("number"));
    }

    @Test
    public void keepsExistingTupletNumbersAndAddsMissingDisplayAttrs() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(tupletMusicXml(true));
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        Element firstTuplet = firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet");
        Element thirdTuplet = firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet");
        assertEquals("7", firstTuplet.getAttribute("number"));
        assertEquals("yes", firstTuplet.getAttribute("bracket"));
        assertEquals("actual", firstTuplet.getAttribute("show-number"));
        assertEquals("7", thirdTuplet.getAttribute("number"));
    }

    @Test
    public void fillsMissingTupletGroupAfterExistingExplicitTupletInSameLane() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(twoTupletGroupsMusicXml());
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        Element firstGroupStart = firstDirectChild(directChild(noteAt(doc, 0), "notations"), "tuplet");
        Element firstGroupStop = firstDirectChild(directChild(noteAt(doc, 2), "notations"), "tuplet");
        Element secondGroupStart = firstDirectChild(directChild(noteAt(doc, 4), "notations"), "tuplet");
        Element secondGroupStop = firstDirectChild(directChild(noteAt(doc, 6), "notations"), "tuplet");
        assertEquals("7", firstGroupStart.getAttribute("number"));
        assertEquals("7", firstGroupStop.getAttribute("number"));
        assertEquals("start", secondGroupStart.getAttribute("type"));
        assertEquals("stop", secondGroupStop.getAttribute("type"));
        assertEquals("yes", secondGroupStart.getAttribute("bracket"));
        assertEquals("actual", secondGroupStart.getAttribute("show-number"));
    }

    @Test
    public void doesNotAddImplicitBeamsDuringImportedTextNormalization() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(beamMusicXml(false));
        Document doc = MusicXmlIo.parseMusicXmlDocument(normalized);

        assertNull(directChild(noteAt(doc, 0), "beam"));
        assertNull(directChild(noteAt(doc, 1), "beam"));
        assertNull(directChild(noteAt(doc, 2), "beam"));
        assertNull(directChild(noteAt(doc, 3), "beam"));
    }

    @Test
    public void addsImplicitBeamsOnlyWhenRequestedExplicitly() {
        String withBeams = MusicXmlIo.applyImplicitBeamsToMusicXmlText(beamMusicXml(false));
        Document doc = MusicXmlIo.parseMusicXmlDocument(withBeams);

        assertEquals("begin", directChild(noteAt(doc, 0), "beam").getTextContent());
        assertEquals("end", directChild(noteAt(doc, 1), "beam").getTextContent());
        assertEquals("begin", directChild(noteAt(doc, 2), "beam").getTextContent());
        assertEquals("end", directChild(noteAt(doc, 3), "beam").getTextContent());
    }

    @Test
    public void usesJavaScriptDecimalPrefixParsingForImplicitBeamTimelineValues() {
        String source = beamMusicXml(false)
                .replace("<divisions>480</divisions>", "<divisions>\u00A0480suffix\u00A0</divisions>")
                .replace("<duration>240</duration>", "<duration>\u00A0240tail\u00A0</duration>")
                .replace("<type>eighth</type>", "<type>\u00A0eighth\u00A0</type>");

        Document doc = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.applyImplicitBeamsToMusicXmlText(source));

        assertEquals("begin", directChild(noteAt(doc, 0), "beam").getTextContent());
        assertEquals("end", directChild(noteAt(doc, 1), "beam").getTextContent());
        assertEquals("begin", directChild(noteAt(doc, 2), "beam").getTextContent());
        assertEquals("end", directChild(noteAt(doc, 3), "beam").getTextContent());
    }

    @Test
    public void keepsEmptyVoiceLaneDistinctFromAnAbsentVoiceWhenInferringBeams() {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type></time>"
                + "</attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>240</duration>"
                + "<type>eighth</type></note><note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>240</duration><voice/></note></measure></part></score-partwise>";

        Document doc = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.applyImplicitBeamsToMusicXmlText(source));

        assertNull(directChild(noteAt(doc, 0), "beam"));
        assertNull(directChild(noteAt(doc, 1), "beam"));
    }

    @Test
    public void keepsLaneBeamsUntouchedWhenImplicitBeamPassRunsOverExistingBeams() {
        String withBeams = MusicXmlIo.applyImplicitBeamsToMusicXmlText(beamMusicXml(true));
        Document doc = MusicXmlIo.parseMusicXmlDocument(withBeams);

        assertEquals(1, countDirectChildren(noteAt(doc, 0), "beam"));
        assertEquals(1, countDirectChildren(noteAt(doc, 1), "beam"));
        assertEquals(0, countDirectChildren(noteAt(doc, 2), "beam"));
        assertEquals(0, countDirectChildren(noteAt(doc, 3), "beam"));
    }

    @Test
    public void buildsMusicXmlBeamItemsXmlFromAssignmentParts() {
        assertEquals("<beam number=\"1\">begin</beam><beam number=\"2\">begin</beam>",
                MusicXmlIo.buildMusicXmlBeamItemsXml("begin", Integer.valueOf(2)));
        assertEquals("<beam number=\"1\">continue</beam><beam number=\"2\">continue</beam>"
                + "<beam number=\"3\">continue</beam>",
                MusicXmlIo.buildMusicXmlBeamItemsXml("continue", Double.valueOf(2.5d)));
        assertEquals("", MusicXmlIo.buildMusicXmlBeamItemsXml("end", Integer.valueOf(0)));
        assertEquals("<beam number=\"1\">end</beam><beam number=\"2\">end</beam>",
                MusicXmlIo.buildMusicXmlBeamItemsXml(new MusicXmlIo.BeamAssignment("end", 2)));
    }

    @Test
    public void preservesRenderNodeMapInsertionOrderAndJavaScriptNullStringification() {
        Document source = MusicXmlIo.parseMusicXmlDocument(beamMusicXml(false));

        MusicXmlIo.RenderDocBundle bundle = MusicXmlIo.buildRenderDocWithNodeIds(source,
                Arrays.asList("n2", "n1"), null);

        assertEquals(Arrays.asList("null-n2", "null-n1"),
                new ArrayList<String>(bundle.getSvgIdToNodeId().keySet()));
        assertEquals("n2", bundle.getSvgIdToNodeId().get("null-n2"));
        assertEquals("null-n2", noteAt(bundle.getRenderDoc(), 0).getAttribute("xml:id"));
    }

    @Test
    public void computesSharedBeamAssignmentsForImplicitAndExplicitModes() {
        Map<Integer, MusicXmlIo.BeamAssignment> implicit = MusicXmlIo.computeBeamAssignments(Arrays.asList(
                new MusicXmlIo.BeamEventInfo(true, true, false, 120, 1, ""),
                new MusicXmlIo.BeamEventInfo(true, true, false, 120, 1, ""),
                new MusicXmlIo.BeamEventInfo(true, false, false, 240, 0, ""),
                new MusicXmlIo.BeamEventInfo(true, true, true, 0, 1, ""),
                new MusicXmlIo.BeamEventInfo(true, true, false, 120, 2, ""),
                new MusicXmlIo.BeamEventInfo(true, true, false, 120, 2, "")), 480, true);

        assertEquals("begin", implicit.get(Integer.valueOf(0)).getState());
        assertEquals("end", implicit.get(Integer.valueOf(1)).getState());
        assertNull(implicit.get(Integer.valueOf(2)));
        assertNull(implicit.get(Integer.valueOf(3)));
        assertEquals(2, implicit.get(Integer.valueOf(4)).getLevels());
        assertEquals("end", implicit.get(Integer.valueOf(5)).getState());

        Map<Integer, MusicXmlIo.BeamAssignment> explicit = MusicXmlIo.computeBeamAssignments(Arrays.asList(
                new MusicXmlIo.BeamEventInfo(true, true, false, 120, 1, ""),
                new MusicXmlIo.BeamEventInfo(true, true, false, 120, 2, "mid"),
                new MusicXmlIo.BeamEventInfo(true, true, false, 120, 2, "")), 960, true);

        assertEquals("begin", explicit.get(Integer.valueOf(0)).getState());
        assertEquals("continue", explicit.get(Integer.valueOf(1)).getState());
        assertEquals(2, explicit.get(Integer.valueOf(1)).getLevels());
        assertEquals("end", explicit.get(Integer.valueOf(2)).getState());
    }

    @Test
    public void addsFinalRightBarlineWhenMissing() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(minimalMusicXmlWithoutPartList("Final"));

        assertTrue(normalized.contains("<barline location=\"right\">"));
        assertTrue(normalized.contains("<bar-style>light-heavy</bar-style>"));
    }

    @Test
    public void keepsExistingFinalRightBarline() {
        String normalized = MusicXmlIo.normalizeImportedMusicXmlText(musicXmlWithExistingFinalBarline());

        assertEquals(1, countOccurrences(normalized, "<barline location=\"right\">"));
        assertTrue(normalized.contains("<bar-style>heavy-light</bar-style>"));
        assertTrue(!normalized.contains("<bar-style>light-heavy</bar-style>"));
    }

    @Test
    public void normalizesOnlyExactRightBarlineLocationsAndTheFirstDuplicateScorePart() {
        String source = musicXmlWithExistingFinalBarline().replace("location=\"right\"", "location=\" right \"")
                .replace("<part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>",
                        "<part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part>"
                                + "<score-part id=\"P1\"/></part-list>");

        Document doc = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.normalizeImportedMusicXmlText(source));
        Element partList = directChild(doc.getDocumentElement(), "part-list");
        Element secondScorePart = (Element) partList.getElementsByTagName("score-part").item(1);
        Element lastMeasure = directMeasureAt(firstDirectChild(doc.getDocumentElement(), "part"), 0);

        assertNull(directChild(secondScorePart, "part-name"));
        assertEquals(2, countDirectChildren(lastMeasure, "barline"));
    }

    @Test
    public void returnsOriginalTextWhenNormalizationCannotParse() {
        String invalid = "<score-partwise";

        assertEquals(invalid, MusicXmlIo.normalizeImportedMusicXmlText(invalid));
    }

    @Test
    public void prettyPrintUsesJavaScriptWhitespaceForTagCompactionAndTrimming() {
        String input = "\uFEFF\u00A0<root>\u00A0<child/>\u3000</root>\u00A0";

        assertEquals("<root>\n <child/>\n</root>", MusicXmlIo.prettyPrintMusicXmlText(input));
    }

    @Test
    public void buildsRenderDocWithSvgNodeIdsWithoutMutatingSource() {
        Document source = MusicXmlIo.parseMusicXmlDocument(twoMeasureMusicXml());

        MusicXmlIo.RenderDocBundle bundle = MusicXmlIo.buildRenderDocWithNodeIds(source,
                Arrays.asList("n1", "n2"), "mks");

        assertEquals(2, bundle.getNoteCount());
        assertEquals("n1", bundle.getSvgIdToNodeId().get("mks-n1"));
        assertEquals("mks-n1", noteAt(bundle.getRenderDoc(), 0).getAttribute("xml:id"));
        assertEquals("mks-n2", noteAt(bundle.getRenderDoc(), 1).getAttribute("id"));
        assertEquals("", noteAt(source, 0).getAttribute("xml:id"));
    }

    @Test
    public void preparesPreviewSvgIdMapWithDirectAndFallbackModes() {
        Document source = MusicXmlIo.parseMusicXmlDocument(twoMeasureMusicXml());
        MusicXmlIo.RenderDocBundle bundle = MusicXmlIo.buildRenderDocWithNodeIds(source,
                Arrays.asList("n1", "n2"), "mks");

        MusicXmlIo.PreviewSvgIdMap direct = MusicXmlIo.preparePreviewSvgIdMap(bundle,
                Arrays.asList("n1", "n2"), Arrays.asList("mks-n1"));
        MusicXmlIo.PreviewSvgIdMap fallback = MusicXmlIo.preparePreviewSvgIdMap(bundle,
                Arrays.asList("n1", "n2"), Arrays.asList("vrv-note-1", "vrv-note-2"));
        MusicXmlIo.PreviewSvgIdMap noRendered = MusicXmlIo.preparePreviewSvgIdMap(bundle,
                Arrays.asList("n1", "n2"), Arrays.<String>asList());
        MusicXmlIo.PreviewSvgIdMap unmappedEmbedded = MusicXmlIo.preparePreviewSvgIdMap(bundle,
                Arrays.asList("n1", "n2"), Arrays.asList("mks-unmapped", "vrv-note-1"));

        assertEquals("direct", direct.getMapMode());
        assertEquals("n1", direct.getMap().get("mks-n1"));
        assertEquals("fallback-seq", fallback.getMapMode());
        assertEquals("n1", fallback.getMap().get("vrv-note-1"));
        assertEquals("n2", fallback.getMap().get("vrv-note-2"));
        assertEquals("direct", noRendered.getMapMode());
        assertEquals("direct", unmappedEmbedded.getMapMode());
        assertEquals("n1", unmappedEmbedded.getMap().get("mks-n1"));
    }

    @Test
    public void extractsMeasureEditorDocumentWithEffectiveAttributesAndBlankPartNames() {
        Document source = MusicXmlIo.parseMusicXmlDocument(twoMeasureMusicXml());

        Document editor = MusicXmlIo.extractMeasureEditorDocument(source, "P1", "2");

        assertNotNull(editor);
        Element root = editor.getDocumentElement();
        assertEquals("score-partwise", root.getTagName());
        assertEquals("4.0", root.getAttribute("version"));
        Element scorePart = firstDirectChild(firstDirectChild(root, "part-list"), "score-part");
        assertEquals("", firstDirectChild(scorePart, "part-name").getTextContent());
        assertEquals("", firstDirectChild(scorePart, "part-abbreviation").getTextContent());
        Element measure = firstDirectChild(firstDirectChild(root, "part"), "measure");
        Element attrs = firstDirectChild(measure, "attributes");
        assertEquals("480", firstDirectChild(attrs, "divisions").getTextContent());
        assertEquals("0", firstDirectChild(firstDirectChild(attrs, "key"), "fifths").getTextContent());
        assertEquals("4", firstDirectChild(firstDirectChild(attrs, "time"), "beats").getTextContent());
        assertEquals("G", firstDirectChild(firstDirectChild(attrs, "clef"), "sign").getTextContent());
    }

    @Test
    public void usesRawPartMeasureAndVersionAttributesForMeasureEditorLookup() {
        String sourceText = twoMeasureMusicXml().replace("version=\"4.0\"", "version=\" 4.0 \"")
                .replace("id=\"P1\"", "id=\" P1 \"").replace("number=\"2\"", "number=\" 2 \"");
        Document source = MusicXmlIo.parseMusicXmlDocument(sourceText);

        assertNull(MusicXmlIo.extractMeasureEditorDocument(source, "P1", "2"));
        Document editor = MusicXmlIo.extractMeasureEditorDocument(source, " P1 ", " 2 ");

        assertNotNull(editor);
        assertEquals(" 4.0 ", editor.getDocumentElement().getAttribute("version"));
    }

    @Test
    public void replacesMeasureInMainDocumentAndDropsPreviewOnlyAttributes() {
        Document main = MusicXmlIo.parseMusicXmlDocument(twoMeasureMusicXml());
        Document editor = MusicXmlIo.extractMeasureEditorDocument(main, "P1", "2");
        Element editorMeasure = firstDirectChild(firstDirectChild(editor.getDocumentElement(), "part"), "measure");
        Element editorNote = firstDirectChild(editorMeasure, "note");
        firstDirectChild(firstDirectChild(editorNote, "pitch"), "step").setTextContent("A");

        Document replaced = MusicXmlIo.replaceMeasureInMainDocument(main, "P1", "2", editor);

        assertNotNull(replaced);
        Element replacedPart = firstDirectChild(replaced.getDocumentElement(), "part");
        Element replacedMeasure = directMeasureAt(replacedPart, 1);
        assertNull(firstDirectChild(replacedMeasure, "attributes"));
        assertEquals("A", firstDirectChild(firstDirectChild(firstDirectChild(replacedMeasure, "note"), "pitch"), "step")
                .getTextContent());
        assertEquals("D", firstDirectChild(firstDirectChild(noteAt(main, 1), "pitch"), "step").getTextContent());
    }

    @Test
    public void replacesFromTheFirstPartMeasureInAWrappedEditorDocument() {
        Document main = MusicXmlIo.parseMusicXmlDocument(twoMeasureMusicXml());
        Document wrappedEditor = MusicXmlIo.parseMusicXmlDocument("<editor><part><measure number=\"ignored\">"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure></part></editor>");

        Document replaced = MusicXmlIo.replaceMeasureInMainDocument(main, "P1", "2", wrappedEditor);

        assertNotNull(replaced);
        Element replacedMeasure = directMeasureAt(firstDirectChild(replaced.getDocumentElement(), "part"), 1);
        assertEquals("A", firstDirectChild(firstDirectChild(firstDirectChild(replacedMeasure, "note"), "pitch"), "step")
                .getTextContent());
    }

    @Test
    public void exposesStringMeasureOperationFacadesWithEditorOnlyAttributes() {
        String source = measureOperationsSingleStaffMusicXml();
        String extracted = MusicXmlIo.extractMeasureEditorMusicXml(source, "P1", "2");
        Document extractedDoc = MusicXmlIo.parseMusicXmlDocument(extracted);

        assertNotNull(extractedDoc);
        Element extractedMeasure = directMeasureAt(firstDirectChild(extractedDoc.getDocumentElement(), "part"), 0);
        assertEquals("", firstDirectChild(firstDirectChild(extractedDoc.getDocumentElement(), "part-list"), "score-part")
                .getElementsByTagName("part-name").item(0).getTextContent());
        assertEquals("480", firstDirectChild(firstDirectChild(extractedMeasure, "attributes"), "divisions").getTextContent());
        assertEquals("3", firstDirectChild(firstDirectChild(firstDirectChild(extractedMeasure, "attributes"), "time"), "beats")
                .getTextContent());

        firstDirectChild(firstDirectChild(extractedMeasure, "note"), "duration").setTextContent("960");
        String merged = MusicXmlIo.replaceMeasureInMusicXml(source, "P1", "2",
                MusicXmlIo.serializeMusicXmlDocument(extractedDoc));
        Document mergedDoc = MusicXmlIo.parseMusicXmlDocument(merged);
        Element replaced = directMeasureAt(firstDirectChild(mergedDoc.getDocumentElement(), "part"), 1);
        assertNull(firstDirectChild(replaced, "attributes"));
        assertEquals("960", firstDirectChild(firstDirectChild(replaced, "note"), "duration").getTextContent());
    }

    @Test
    public void appendsFullMeasureRestWithInheritedTiming() {
        Document appended = MusicXmlIo.parseMusicXmlDocument(
                MusicXmlIo.appendMeasureToMusicXml(measureOperationsSingleStaffMusicXml()));

        assertNotNull(appended);
        Element measure = directMeasureAt(firstDirectChild(appended.getDocumentElement(), "part"), 2);
        assertEquals("3", measure.getAttribute("number"));
        assertEquals("yes", firstDirectChild(firstDirectChild(measure, "note"), "rest").getAttribute("measure"));
        assertEquals("1440", firstDirectChild(firstDirectChild(measure, "note"), "duration").getTextContent());
        assertNull(firstDirectChild(firstDirectChild(measure, "note"), "staff"));
    }

    @Test
    public void appendsSynchronizedRestsAndBackupForTrebleBassGrandStaff() {
        String source = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Piano</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"intro\"><attributes>"
                + "<divisions>240</divisions><time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves>"
                + "<clef number=\"1\"><sign>G</sign><line>2</line></clef>"
                + "<clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes>"
                + "<note><rest/><duration>960</duration><voice>1</voice><staff>1</staff></note>"
                + "<backup><duration>960</duration></backup>"
                + "<note><rest/><duration>960</duration><voice>1</voice><staff>2</staff></note>"
                + "</measure></part></score-partwise>";
        Document appended = MusicXmlIo.parseMusicXmlDocument(MusicXmlIo.appendMeasureToMusicXml(source));

        assertNotNull(appended);
        Element measure = directMeasureAt(firstDirectChild(appended.getDocumentElement(), "part"), 1);
        assertEquals("2", measure.getAttribute("number"));
        assertEquals("1", firstDirectChild(firstDirectChild(measure, "note"), "staff").getTextContent());
        Element backup = firstDirectChild(measure, "backup");
        assertEquals("960", firstDirectChild(backup, "duration").getTextContent());
        Element secondNote = (Element) measure.getElementsByTagName("note").item(1);
        assertEquals("2", firstDirectChild(secondNote, "staff").getTextContent());
    }

    @Test
    public void returnsNullForInvalidOrMissingStringMeasureOperationTargets() {
        String source = measureOperationsSingleStaffMusicXml();

        assertNull(MusicXmlIo.extractMeasureEditorMusicXml("<score-partwise", "P1", "1"));
        assertNull(MusicXmlIo.extractMeasureEditorMusicXml(source, "missing", "2"));
        assertNull(MusicXmlIo.replaceMeasureInMusicXml(source, "P1", "missing", source));
        assertNull(MusicXmlIo.appendMeasureToMusicXml("<score-partwise"));
        assertNull(MusicXmlIo.appendMeasureToMusicXml("<score-partwise><part-list/></score-partwise>"));
    }

    private static String minimalMusicXmlWithoutPartList(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part>\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><rest/><duration>1920</duration><voice>1</voice><type>whole</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String beamMusicXml(boolean existingBeams) {
        String firstBeam = existingBeams ? "<beam number=\"1\">begin</beam>" : "";
        String secondBeam = existingBeams ? "<beam number=\"1\">end</beam>" : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type>" + firstBeam + "</note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type>" + secondBeam + "</note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String tupletMusicXml(boolean existingTuplets) {
        String firstNotations = existingTuplets ? "<notations><tuplet type=\"start\" number=\"7\"/></notations>" : "";
        String thirdNotations = existingTuplets ? "<notations><tuplet type=\"stop\" number=\"7\"/></notations>" : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>" + firstNotations + "</note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>" + thirdNotations + "</note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String twoTupletGroupsMusicXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><tuplet type=\"start\" number=\"7\"/></notations></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><tuplet type=\"stop\" number=\"7\"/></notations></note>\n"
                + "      <note><rest/></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>G</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>A</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String musicXmlWithExistingFinalBarline() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><rest/><duration>1920</duration><voice>1</voice><type>whole</type></note>\n"
                + "      <barline location=\"right\"><bar-style>heavy-light</bar-style></barline>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String twoMeasureMusicXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Piano</part-name><part-abbreviation>Pno.</part-abbreviation></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "    <measure number=\"2\">\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String measureOperationsSingleStaffMusicXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Flute</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><rest/><duration>1440</duration><voice>1</voice></note></measure>\n"
                + "    <measure number=\"2\"><note><rest/><duration>1440</duration><voice>1</voice>"
                + "</note></measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while (true) {
            index = text.indexOf(pattern, index);
            if (index < 0) {
                return count;
            }
            count++;
            index += pattern.length();
        }
    }

    private static Element noteAt(Document doc, int index) {
        NodeList notes = doc.getElementsByTagName("note");
        return (Element) notes.item(index);
    }

    private static Element directChild(Element parent, String tagName) {
        return firstDirectChild(parent, tagName);
    }

    private static Element firstDirectChild(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element && tagName.equals(((Element) children.item(index)).getTagName())) {
                return (Element) children.item(index);
            }
        }
        return null;
    }

    private static int countDirectChildren(Element parent, String tagName) {
        int count = 0;
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element && tagName.equals(((Element) children.item(index)).getTagName())) {
                count++;
            }
        }
        return count;
    }

    private static Element directMeasureAt(Element part, int index) {
        int measureIndex = 0;
        NodeList children = part.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            if (children.item(childIndex) instanceof Element
                    && "measure".equals(((Element) children.item(childIndex)).getTagName())) {
                if (measureIndex == index) {
                    return (Element) children.item(childIndex);
                }
                measureIndex++;
            }
        }
        return null;
    }
}
