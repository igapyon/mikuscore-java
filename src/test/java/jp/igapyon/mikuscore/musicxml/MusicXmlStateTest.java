package jp.igapyon.mikuscore.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MusicXmlStateTest {
    @Test
    public void summarizesCanonicalMusicXmlState() {
        MusicXmlStateSummary summary = MusicXmlState.summarizeMusicXmlState(sampleMusicXml("State summary"));

        assertEquals("State summary", summary.getTitle());
        assertEquals(1, summary.getPartCount());
        assertEquals(2, summary.getMeasureCount());
        assertEquals("1", summary.getMeasureNumbers().get(0));
        assertEquals("2", summary.getMeasureNumbers().get(1));
        assertEquals("1", summary.getVoices().get(0));
        assertEquals("2", summary.getVoices().get(1));
    }

    @Test
    public void emitsUpstreamShapedJson() {
        String json = MusicXmlState.summarizeMusicXmlState(sampleMusicXml("State summary")).toJson();

        assertTrue(json.contains("\"kind\": \"musicxml_state_summary\""));
        assertTrue(json.contains("\"title\": \"State summary\""));
        assertTrue(json.contains("\"part_count\": 1"));
        assertTrue(json.contains("\"measure_count\": 2"));
        assertTrue(json.contains("\"measure_numbers\": [\"1\", \"2\"]"));
        assertTrue(json.contains("\"voices\": [\"1\", \"2\"]"));
    }

    @Test
    public void inspectsOneMeasureForEditTargeting() {
        MusicXmlMeasureInspection inspected = MusicXmlState.inspectMusicXmlMeasure(sampleMusicXml("Inspect measure"),
                "1");

        assertEquals("1", inspected.getMeasureNumber());
        assertEquals(1, inspected.getMeasures().size());
        assertEquals("P1", inspected.getMeasures().get(0).getPartId());
        assertEquals(2, inspected.getMeasures().get(0).getNoteCount());
        MusicXmlMeasureInspection.Note first = inspected.getMeasures().get(0).getNotes().get(0);
        assertEquals("n1", first.getNodeId());
        assertEquals("P1", first.getSelector().getPartId());
        assertEquals("1", first.getSelector().getMeasureNumber());
        assertEquals(1, first.getSelector().getMeasureNoteIndex());
        assertEquals("1", first.getSelector().getVoice());
        assertEquals(Integer.valueOf(1), first.getSelector().getVoiceNoteIndex());
        assertEquals("C", first.getPitch().getStep());
        assertEquals(Integer.valueOf(4), first.getPitch().getOctave());
    }

    @Test
    public void emitsMeasureInspectionJson() {
        String json = MusicXmlState.inspectMusicXmlMeasure(sampleMusicXml("Inspect measure"), "1").toJson();

        assertTrue(json.contains("\"kind\": \"musicxml_measure_inspection\""));
        assertTrue(json.contains("\"measure_number\": \"1\""));
        assertTrue(json.contains("\"part_id\": \"P1\""));
        assertTrue(json.contains("\"node_id\": \"n1\""));
        assertTrue(json.contains("\"measure_note_index\": 1"));
        assertTrue(json.contains("\"voice_note_index\": 1"));
        assertTrue(json.contains("\"step\": \"C\""));
    }

    @Test
    public void diffsTwoCanonicalMusicXmlStates() {
        String before = sampleMusicXml("Before title");
        String after = sampleMusicXml("After title").replace("<step>C</step>", "<step>G</step>");

        MusicXmlStateDiff diff = MusicXmlState.diffMusicXmlState(before, after);

        assertTrue(diff.isChanged());
        assertTrue(diff.getChangedFields().contains("title"));
        assertEquals("1", diff.getChangedMeasureNumbers().get(0));
        assertEquals("P1", diff.getChangedMeasures().get(0).getPartId());
        assertEquals("1", diff.getChangedMeasures().get(0).getMeasureNumber());
        assertEquals(2, diff.getChangedMeasures().get(0).getBeforeNoteCount());
        assertEquals(2, diff.getChangedMeasures().get(0).getAfterNoteCount());
        assertEquals("Before title", diff.getBefore().getTitle());
        assertEquals("After title", diff.getAfter().getTitle());
    }

    @Test
    public void emitsDiffJson() {
        String before = sampleMusicXml("Before title");
        String after = sampleMusicXml("After title").replace("<step>C</step>", "<step>G</step>");
        String json = MusicXmlState.diffMusicXmlState(before, after).toJson();

        assertTrue(json.contains("\"kind\": \"musicxml_state_diff\""));
        assertTrue(json.contains("\"changed\": true"));
        assertTrue(json.contains("\"changed_fields\": [\"title\"]"));
        assertTrue(json.contains("\"changed_measure_numbers\": [\"1\"]"));
        assertTrue(json.contains("\"before_note_count\": 2"));
        assertTrue(json.contains("\"after_note_count\": 2"));
        assertTrue(json.contains("\"title\": \"Before title\""));
        assertTrue(json.contains("\"title\": \"After title\""));
    }

    @Test
    public void validatesChangeToPitchCommand() {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"G\",\"octave\":4}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(sampleMusicXml("Validate command"),
                command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
        assertEquals(0, validation.getDiagnostics().size());
    }

    @Test
    public void validatesChangeToPitchCommandViaSelector() {
        String command = "{\"type\":\"change_to_pitch\",\"selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"},\"pitch\":{\"step\":\"G\",\"octave\":4}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXml("Validate selector command"), command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
    }

    @Test
    public void emitsCommandValidationJson() {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"G\",\"octave\":4}}";
        String json = MusicXmlState.validateMusicXmlCommand(sampleMusicXml("Validate command"), command).toJson();

        assertTrue(json.contains("\"kind\": \"musicxml_command_validation\""));
        assertTrue(json.contains("\"ok\": true"));
        assertTrue(json.contains("\"dirty_changed\": true"));
        assertTrue(json.contains("\"changed_node_ids\": [\"n1\"]"));
        assertTrue(json.contains("\"affected_measure_numbers\": [\"1\"]"));
    }

    @Test
    public void appliesChangeToPitchCommand() {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"G\",\"octave\":5}}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleMusicXml("Apply command"), command);

        assertTrue(xml.contains("<step>G</step>"));
        assertTrue(xml.contains("<octave>5</octave>"));
        assertTrue(xml.contains("<step>D</step>"));
    }

    @Test
    public void appliesChangeToPitchCommandViaSelector() {
        String command = "{\"type\":\"change_to_pitch\",\"selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"},\"pitch\":{\"step\":\"A\",\"octave\":4}}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleMusicXml("Apply selector command"), command);

        assertTrue(xml.contains("<step>A</step>"));
        assertTrue(xml.contains("<octave>4</octave>"));
    }

    @Test
    public void applyCommandEmitsFailureJsonWhenValidationFails() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":0}";

        String json = MusicXmlState.applyMusicXmlCommand(sampleMusicXml("Apply invalid command"), command);

        assertTrue(json.contains("\"kind\": \"musicxml_command_apply\""));
        assertTrue(json.contains("\"ok\": false"));
        assertTrue(json.contains("\"code\": \"MVP_INVALID_COMMAND_PAYLOAD\""));
    }

    @Test
    public void validatesChangeDurationCommand() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":2}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Validate duration"), command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
    }

    @Test
    public void rejectsInvalidChangeDurationPayload() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":0}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Reject duration"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void appliesChangeDurationCommandAndUpdatesSimpleNotation() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":2}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleMusicXmlWithDivisions("Apply duration"), command);

        assertTrue(xml.contains("<duration>2</duration>"));
        assertTrue(xml.contains("<type>half</type>"));
    }

    @Test
    public void appliesDottedChangeDurationNotation() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":3}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleMusicXmlWithDivisions("Apply dotted duration"), command);

        assertTrue(xml.contains("<duration>3</duration>"));
        assertTrue(xml.contains("<type>half</type>"));
        assertTrue(xml.contains("<dot/>") || xml.contains("<dot></dot>"));
    }

    @Test
    public void rejectsTripletChangeDurationWithoutTupletContext() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":2}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleTripletDurationWithoutContextMusicXml("Reject triplet duration"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", validation.getDiagnostics().get(0).getCode());
        assertEquals("Tuplet durations are not allowed because this measure/voice has no tuplet context.",
                validation.getDiagnostics().get(0).getMessage());
    }

    @Test
    public void rejectsChangeDurationForRestTarget() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n3\",\"voice\":\"1\",\"duration\":2}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Reject rest duration"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NOTE_KIND", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void validatesDeleteNoteCommand() {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Validate delete"), command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
    }

    @Test
    public void appliesDeleteNoteCommandAsSameDurationRest() {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleMusicXmlWithDivisions("Apply delete"), command);

        assertTrue(xml.contains("<rest/>") || xml.contains("<rest></rest>"));
        assertTrue(xml.contains("<duration>1</duration>"));
        assertTrue(xml.contains("<voice>1</voice>"));
        assertTrue(!xml.contains("<step>C</step>"));
        assertTrue(xml.contains("<step>D</step>"));
    }

    @Test
    public void rejectsDeleteNoteForRestTarget() {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n3\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Reject rest delete"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NOTE_KIND", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void appliesDeleteChordHeadByPromotingNextChordTone() {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleChordTimingMusicXml("Delete chord head"), command);

        assertTrue(!xml.contains("<step>C</step>"));
        assertTrue(xml.contains("<step>E</step>"));
        assertTrue(!xml.contains("<chord/>"));
        assertTrue(!xml.contains("<chord></chord>"));
        assertEquals(3, countOccurrences(xml, "<note>"));
    }

    @Test
    public void rejectsDeleteNoteForChordToneTarget() {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n2\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleChordTimingMusicXml("Reject chord tone delete"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NOTE_KIND", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void validatesSplitNoteCommand() {
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleSplitMusicXml("Validate split"), command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
        assertEquals("n3", validation.getChangedNodeIds().get(1));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
    }

    @Test
    public void appliesSplitNoteCommand() {
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleSplitMusicXml("Apply split"), command);

        assertEquals(2, countOccurrences(xml, "<step>C</step>"));
        assertEquals(3, countOccurrences(xml, "<duration>1</duration>"));
        assertTrue(xml.contains("<type>quarter</type>"));
        assertTrue(xml.contains("<step>D</step>"));
    }

    @Test
    public void rejectsSplitNoteForOddDuration() {
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Reject odd split").replace("<duration>1</duration>", "<duration>3</duration>"),
                command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void rejectsSplitNoteWhenMeasureLaneIsOverfull() {
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n2\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleOverfullSplitMusicXml("Reject overfull split"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MEASURE_OVERFULL", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void validatesInsertNoteAfterCommand() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Validate insert"), command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
        assertEquals("n4", validation.getChangedNodeIds().get(1));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
    }

    @Test
    public void appliesInsertNoteAfterCommand() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"alter\":1,\"octave\":4}}}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleMusicXmlWithDivisions("Apply insert"), command);

        assertTrue(xml.contains("<step>A</step>"));
        assertTrue(xml.contains("<alter>1</alter>"));
        assertTrue(xml.contains("<accidental>sharp</accidental>"));
        assertEquals(4, countOccurrences(xml, "<note>"));
    }

    @Test
    public void validatesInsertNoteAfterCommandViaAnchorSelector() {
        String command = "{\"type\":\"insert_note_after\",\"anchor_selector\":{\"part_id\":\"P1\",\"measure_number\":\"1\",\"measure_note_index\":1,\"voice\":\"1\"},\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"B\",\"octave\":4}}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Validate insert selector"), command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
    }

    @Test
    public void rejectsInvalidInsertNoteAfterPayload() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":0,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Reject insert"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void rejectsOverfullChangeDuration() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":2}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleFullMeasureMusicXml("Reject overfull duration"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MEASURE_OVERFULL", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void extendingDurationConsumesFollowingRestInSameVoice() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n2\",\"voice\":\"1\",\"duration\":2}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleFollowingRestMusicXml("Consume following rest"), command);
        String xml = MusicXmlState.applyMusicXmlCommand(sampleFollowingRestMusicXml("Consume following rest"), command);

        assertTrue(validation.isOk());
        assertEquals(3, countOccurrences(xml, "<note>"));
        assertTrue(xml.contains("<step>C</step>"));
        assertTrue(xml.contains("<step>D</step>"));
        assertTrue(xml.contains("<duration>2</duration>"));
        assertTrue(xml.contains("<step>E</step>"));
        assertTrue(!xml.contains("<rest/>"));
        assertTrue(!xml.contains("<rest></rest>"));
    }

    @Test
    public void shorteningDurationAutoFillsTrailingRest() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":1}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleFullWithHalfMusicXml("Fill underfull rest"), command);

        assertEquals(4, countOccurrences(xml, "<note>"));
        assertTrue(xml.contains("<rest/>") || xml.contains("<rest></rest>"));
        assertTrue(xml.contains("<duration>1</duration>"));
        assertTrue(xml.contains("<voice>1</voice>"));
        assertTrue(xml.contains("<type>quarter</type>"));
        assertTrue(xml.contains("<step>D</step>"));
        assertTrue(xml.contains("<step>E</step>"));
    }

    @Test
    public void rejectsOverfullInsertNoteAfter() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        String json = MusicXmlState.applyMusicXmlCommand(sampleFullMeasureMusicXml("Reject overfull insert"), command);

        assertTrue(json.contains("\"kind\": \"musicxml_command_apply\""));
        assertTrue(json.contains("\"ok\": false"));
        assertTrue(json.contains("\"code\": \"MEASURE_OVERFULL\""));
    }

    @Test
    public void rejectsInsertAcrossBackupForwardBoundary() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleBoundaryMusicXml("Reject insert boundary"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void rejectsDeleteAcrossBackupForwardBoundary() {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleBoundaryMusicXml("Reject delete boundary"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void rejectsSplitAcrossForwardBoundary() {
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleForwardBoundaryMusicXml("Reject split boundary"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void rejectsInsertAcrossLocalVoiceLane() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMixedVoiceLaneMusicXml("Reject insert lane"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void validatesUiNoopWithoutDirtyChange() {
        String command = "{\"type\":\"ui_noop\",\"reason\":\"cursor_move\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXmlWithDivisions("Validate noop"), command);

        assertTrue(validation.isOk());
        assertEquals(0, validation.getChangedNodeIds().size());
        assertEquals(0, validation.getAffectedMeasureNumbers().size());
        assertTrue(validation.toJson().contains("\"dirty_changed\": false"));
    }

    @Test
    public void appliesUiNoopWithoutMutation() {
        String xml = sampleMusicXmlWithDivisions("Apply noop");
        String command = "{\"type\":\"ui_noop\",\"reason\":\"selection_change\"}";

        String after = MusicXmlState.applyMusicXmlCommand(xml, command);

        assertEquals(xml, after);
    }

    public static String sampleMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>2</voice></note>\n"
                + "    </measure>\n"
                + "    <measure number=\"2\">\n"
                + "      <note><rest/><duration>1</duration><voice>1</voice></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleSplitMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice><type>half</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleFullMeasureMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleTripletDurationWithoutContextMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>3</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>3</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><rest/><duration>9</duration><voice>1</voice><type>half</type><dot/></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleFollowingRestMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><rest/><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleFullWithHalfMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice><type>half</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleOverfullSplitMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice><type>whole</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice><type>half</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleBoundaryMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice><type>half</type></note>\n"
                + "      <backup><duration>2</duration></backup>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>2</duration><voice>2</voice><type>half</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleForwardBoundaryMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice><type>half</type></note>\n"
                + "      <forward><duration>1</duration></forward>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleMixedVoiceLaneMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>G</step><octave>3</octave></pitch><duration>1</duration><voice>2</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    private static String sampleChordTimingMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>8</divisions><time><beats>3</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>8</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>8</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>G</step><octave>4</octave></pitch><duration>8</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>B</step><octave>4</octave></pitch><duration>8</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
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

    public static String sampleMusicXmlWithDivisions(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "    <measure number=\"2\">\n"
                + "      <note><rest/><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }
}
