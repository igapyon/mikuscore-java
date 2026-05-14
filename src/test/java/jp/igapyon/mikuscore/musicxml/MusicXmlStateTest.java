package jp.igapyon.mikuscore.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
    public void changeToLowPitchAutoAssignsGrandStaffTwo() {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":2}}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleGrandStaffMusicXml("Grand staff low"), command);

        assertTrue(xml.contains("<step>A</step>"));
        assertTrue(xml.contains("<octave>2</octave>"));
        assertTrue(xml.contains("<staff>2</staff>"));
    }

    @Test
    public void changeToHighPitchAutoAssignsGrandStaffOne() {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"C\",\"octave\":5}}";

        String xml = MusicXmlState.applyMusicXmlCommand(
                sampleGrandStaffMusicXml("Grand staff high").replace("<staff>1</staff>", "<staff>2</staff>"),
                command);

        assertTrue(xml.contains("<step>C</step>"));
        assertTrue(xml.contains("<octave>5</octave>"));
        assertTrue(xml.contains("<staff>1</staff>"));
    }

    @Test
    public void editsNonPrimaryVoiceWhenCommandVoiceMatchesTarget() throws Exception {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n2\",\"voice\":\"2\",\"pitch\":{\"step\":\"A\",\"octave\":3}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                loadTestFixture("abc-roundtrip/mixed_voices.musicxml"), command);
        String xml = MusicXmlState.applyMusicXmlCommand(loadTestFixture("abc-roundtrip/mixed_voices.musicxml"),
                command);

        assertTrue(validation.isOk());
        assertEquals("n2", validation.getChangedNodeIds().get(0));
        assertEquals("1:C:4:1|2:A:3:1|1:D:4:1|1:E:4:1", noteSignatureSequence(xml));
    }

    @Test
    public void changingDurationInVoiceTwoDoesNotMutateVoiceOneNotes() throws Exception {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n2\",\"voice\":\"2\",\"duration\":3}";

        String xml = MusicXmlState.applyMusicXmlCommand(sampleMixedVoiceDurationMusicXml("Voice two duration"),
                command);

        assertEquals("1:C:4:2|2:G:3:3|rest:2:1|1:D:4:2", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
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
    public void invalidDurationPayloadIsRejectedWithoutChangedTargets() {
        String command = "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":0}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXml("Reject invalid duration payload"), command);
        String json = MusicXmlState.applyMusicXmlCommand(sampleMusicXml("Reject invalid duration payload"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", validation.getDiagnostics().get(0).getCode());
        assertEquals(0, validation.getChangedNodeIds().size());
        assertEquals(0, validation.getAffectedMeasureNumbers().size());
        assertTrue(json.contains("\"kind\": \"musicxml_command_apply\""));
        assertTrue(json.contains("\"changed_node_ids\": []"));
        assertTrue(json.contains("\"affected_measure_numbers\": []"));
    }

    @Test
    public void invalidPitchPayloadIsRejectedWithoutChangedTargets() {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"H\",\"octave\":4}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleMusicXml("Reject invalid pitch payload"), command);
        String json = MusicXmlState.applyMusicXmlCommand(sampleMusicXml("Reject invalid pitch payload"), command);

        assertEquals(false, validation.isOk());
        assertEquals("MVP_INVALID_COMMAND_PAYLOAD", validation.getDiagnostics().get(0).getCode());
        assertEquals(0, validation.getChangedNodeIds().size());
        assertEquals(0, validation.getAffectedMeasureNumbers().size());
        assertTrue(json.contains("\"kind\": \"musicxml_command_apply\""));
        assertTrue(json.contains("\"changed_node_ids\": []"));
        assertTrue(json.contains("\"affected_measure_numbers\": []"));
    }

    @Test
    public void failedCommandDoesNotMutatePreviouslySuccessfulEdit() throws Exception {
        String source = loadTestFixture("abc-roundtrip/base.musicxml");
        String successCommand = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"G\",\"octave\":5}}";
        String failCommand = "{\"type\":\"change_duration\",\"targetNodeId\":\"n2\",\"voice\":\"1\",\"duration\":2}";

        String editedXml = MusicXmlState.applyMusicXmlCommand(source, successCommand);
        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(editedXml, failCommand);
        String failedJson = MusicXmlState.applyMusicXmlCommand(editedXml, failCommand);

        assertEquals(false, validation.isOk());
        assertEquals("MEASURE_OVERFULL", validation.getDiagnostics().get(0).getCode());
        assertTrue(failedJson.contains("\"kind\": \"musicxml_command_apply\""));
        assertTrue(failedJson.contains("\"code\": \"MEASURE_OVERFULL\""));
        assertEquals("1:G:5:1|1:D:4:1|1:E:4:1|1:F:4:1", noteSignatureSequence(editedXml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(editedXml, true).isOk());
    }

    @Test
    public void validatesSaveIntegrityForInvalidUpstreamFixtures() throws Exception {
        assertSaveDiagnostic("invalid_note_duration.musicxml", true, "MVP_INVALID_NOTE_DURATION");
        assertSaveDiagnostic("invalid_note_pitch.musicxml", true, "MVP_INVALID_NOTE_PITCH");
        assertSaveDiagnostic("invalid_rest_with_pitch.musicxml", true, "MVP_INVALID_NOTE_PITCH");
        assertSaveDiagnostic("invalid_chord_without_pitch.musicxml", true, "MVP_INVALID_NOTE_PITCH");
    }

    @Test
    public void saveIntegrityAllowsMissingVoiceOnlyForNoopState() throws Exception {
        String xml = loadMusicXmlStateFixture("invalid_note_voice.musicxml");

        MusicXmlCommandValidation noopValidation = MusicXmlState.validateMusicXmlForSave(xml, false);
        MusicXmlCommandValidation dirtyValidation = MusicXmlState.validateMusicXmlForSave(xml, true);

        assertTrue(noopValidation.isOk());
        assertEquals(false, dirtyValidation.isOk());
        assertEquals("MVP_INVALID_NOTE_VOICE", dirtyValidation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void changeToPitchAddsMissingVoiceToEditedNoteOnly() throws Exception {
        String source = loadMusicXmlStateFixture("invalid_note_voice.musicxml");
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"G\",\"octave\":4}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String xml = MusicXmlState.applyMusicXmlCommand(source, command);

        assertTrue(validation.isOk());
        assertEquals("1:G:4:1|1:D:4:1|1:E:4:1|1:F:4:1", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
    }

    @Test
    public void saveIntegrityRejectsOverfullFixture() throws Exception {
        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlForSave(
                loadMusicXmlStateFixture("overfull.musicxml"), false);

        assertEquals(false, validation.isOk());
        assertEquals("MEASURE_OVERFULL", validation.getDiagnostics().get(0).getCode());
    }

    @Test
    public void saveIntegrityAllowsSameVoiceSplitByBackupForGrandStaffTimeline() throws Exception {
        String xml = sampleGrandStaffSameVoiceBackupMusicXml("Grand staff same voice backup");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlForSave(xml, true);

        assertTrue(validation.isOk());
        assertEquals("rest:1:3840|rest:1:3840", noteSignatureSequence(xml));
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
    public void deleteReplacesOnlyTargetNoteAndKeepsMeasureAttributes() throws Exception {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n2\",\"voice\":\"1\"}";

        String xml = MusicXmlState.applyMusicXmlCommand(loadTestFixture("abc-roundtrip/base.musicxml"), command);

        assertEquals("1:4/4", firstMeasureAttributesSignature(xml));
        assertEquals("1:C:4:1|rest:1:1|1:E:4:1|1:F:4:1", noteSignatureSequence(xml));
    }

    @Test
    public void deleteReplacesTargetAtSamePositionAndDuration() throws Exception {
        String source = loadTestFixture("abc-roundtrip/underfull.musicxml");
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n2\",\"voice\":\"1\"}";

        String xml = MusicXmlState.applyMusicXmlCommand(source, command);

        assertEquals(3, countOccurrences(xml, "<note>"));
        assertEquals("1:C:4:1|rest:1:1|1:E:4:1", noteSignatureSequence(xml));
    }

    @Test
    public void deleteKeepsTotalDurationForTargetMeasureVoice() throws Exception {
        String source = loadTestFixture("abc-roundtrip/base.musicxml");
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n2\",\"voice\":\"1\"}";

        int beforeTotal = sumDurationForMeasureVoice(source, "1", "1");
        String xml = MusicXmlState.applyMusicXmlCommand(source, command);
        int afterTotal = sumDurationForMeasureVoice(xml, "1", "1");

        assertEquals(beforeTotal, afterTotal);
    }

    @Test
    public void deleteToRestKeepsGeneratedNodeIdsStable() throws Exception {
        String source = loadTestFixture("abc-roundtrip/base.musicxml");
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n2\",\"voice\":\"1\"}";

        String xml = MusicXmlState.applyMusicXmlCommand(source, command);
        MusicXmlMeasureInspection.Measure measure = MusicXmlState.inspectMusicXmlMeasure(xml, "1").getMeasures().get(0);

        assertEquals(4, measure.getNoteCount());
        assertEquals("n1", measure.getNotes().get(0).getNodeId());
        assertEquals("n2", measure.getNotes().get(1).getNodeId());
        assertEquals("n3", measure.getNotes().get(2).getNodeId());
        assertEquals("n4", measure.getNotes().get(3).getNodeId());
        assertTrue(measure.getNotes().get(1).isRest());
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
    public void deleteRestTargetReportsNoChangedTargets() throws Exception {
        String source = sampleMusicXmlWithDivisions("Reject rest delete no targets");
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n3\",\"voice\":\"1\"}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String json = MusicXmlState.applyMusicXmlCommand(source, command);

        assertUnsupportedNoteKindNoChangedTargets(validation, json);
        assertEquals("1:C:4:1|1:D:4:1|rest:1:1", noteSignatureSequence(source));
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
    public void insertKeepsExistingNotesStableExceptLocalInsertion() throws Exception {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        String xml = MusicXmlState.applyMusicXmlCommand(loadTestFixture("abc-roundtrip/underfull.musicxml"), command);

        assertEquals("1:4/4", firstMeasureAttributesSignature(xml));
        assertEquals("1:C:4:1|1:A:4:1|1:D:4:1|1:E:4:1", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
    }

    @Test
    public void insertReportsAnchorAndNewNodeIds() throws Exception {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                loadTestFixture("abc-roundtrip/underfull.musicxml"), command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
        assertEquals("n4", validation.getChangedNodeIds().get(1));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
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
    public void changeToPitchPreservesUnknownElements() throws Exception {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"B\",\"octave\":4}}";

        String xml = MusicXmlState.applyMusicXmlCommand(loadTestFixture("abc-roundtrip/with_unknown.musicxml"), command);

        assertTrue(xml.contains("<unknown-tag foo=\"bar\">x</unknown-tag>"));
        assertEquals("1:B:4:1|1:D:4:1|1:E:4:1|1:F:4:1", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
    }

    @Test
    public void changeToPitchPreservesExistingBeamXml() throws Exception {
        String command = "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n3\",\"voice\":\"1\",\"pitch\":{\"step\":\"B\",\"octave\":5}}";

        String xml = MusicXmlState.applyMusicXmlCommand(loadTestFixture("abc-roundtrip/with_beam.musicxml"), command);

        assertTrue(xml.contains("<beam number=\"1\">begin</beam>"));
        assertTrue(xml.contains("<beam number=\"1\">end</beam>"));
        assertEquals("1:C:4:1|1:D:4:1|1:B:5:1|1:F:4:1", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
    }

    @Test
    public void saveIntegrityAllowsGraceNoteWithoutDuration() {
        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlForSave(
                sampleGraceNoteWithoutDurationMusicXml("Save grace without duration"), true);

        assertTrue(validation.isOk());
        assertEquals(0, validation.getDiagnostics().size());
    }

    @Test
    public void warnsUnderfullInsertNoteAfter() {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(
                sampleUnderfullInsertMusicXml("Warn underfull insert"), command);

        assertTrue(validation.isOk());
        assertEquals(1, validation.getWarnings().size());
        assertEquals("MEASURE_UNDERFULL", validation.getWarnings().get(0).getCode());
        assertTrue(validation.toJson().contains("\"code\": \"MEASURE_UNDERFULL\""));
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
    public void insertAcrossBackupBoundaryReportsNoChangedTargets() throws Exception {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";
        String source = sampleBoundaryMusicXml("Reject insert boundary no targets");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String json = MusicXmlState.applyMusicXmlCommand(source, command);

        assertUnsupportedNoChangedTargets(validation, json);
        assertEquals("1:C:4:2|2:E:4:2", noteSignatureSequence(source));
    }

    @Test
    public void splitAcrossForwardBoundaryReportsNoChangedTargets() throws Exception {
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";
        String source = sampleForwardBoundaryMusicXml("Reject split boundary no targets");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String json = MusicXmlState.applyMusicXmlCommand(source, command);

        assertUnsupportedNoChangedTargets(validation, json);
        assertEquals("1:C:4:2|1:D:4:1", noteSignatureSequence(source));
    }

    @Test
    public void insertAwayFromBackupForwardBoundaryIsAllowed() throws Exception {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n4\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"F\",\"octave\":4}}}";
        String source = loadTestFixture("abc-roundtrip/with_backup_safe.musicxml");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String xml = MusicXmlState.applyMusicXmlCommand(source, command);

        assertTrue(validation.isOk());
        assertEquals("n4", validation.getChangedNodeIds().get(0));
        assertEquals("n5", validation.getChangedNodeIds().get(1));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
        assertEquals("1:C:4:1|2:G:3:1|1:D:4:1|1:E:4:1|1:F:4:1", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
    }

    @Test
    public void deleteAwayFromBackupForwardBoundaryIsAllowed() throws Exception {
        String command = "{\"type\":\"delete_note\",\"targetNodeId\":\"n4\",\"voice\":\"1\"}";
        String source = loadTestFixture("abc-roundtrip/with_backup_safe.musicxml");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String xml = MusicXmlState.applyMusicXmlCommand(source, command);

        assertTrue(validation.isOk());
        assertEquals("n4", validation.getChangedNodeIds().get(0));
        assertEquals("1", validation.getAffectedMeasureNumbers().get(0));
        assertEquals("1:C:4:1|2:G:3:1|1:D:4:1|rest:1:1", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
    }

    @Test
    public void splitImmediatelyBeforeBackupBoundaryIsAllowed() throws Exception {
        String command = "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}";
        String source = sampleBoundaryMusicXml("Split before backup boundary");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String xml = MusicXmlState.applyMusicXmlCommand(source, command);

        assertTrue(validation.isOk());
        assertEquals("n1", validation.getChangedNodeIds().get(0));
        assertEquals("n3", validation.getChangedNodeIds().get(1));
        assertEquals("1:C:4:1|1:C:4:1|2:E:4:2", noteSignatureSequence(xml));
        assertTrue(MusicXmlState.validateMusicXmlForSave(xml, true).isOk());
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
    public void insertAnchorVoiceMismatchReportsNoChangedTargets() throws Exception {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n2\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";
        String source = loadTestFixture("abc-roundtrip/mixed_voices.musicxml");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String json = MusicXmlState.applyMusicXmlCommand(source, command);

        assertUnsupportedNoChangedTargets(validation, json);
        assertEquals("1:C:4:1|2:G:3:1|1:D:4:1|1:E:4:1", noteSignatureSequence(source));
    }

    @Test
    public void insertCrossingInterleavedVoiceLaneReportsNoChangedTargets() throws Exception {
        String command = "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}";
        String source = loadTestFixture("abc-roundtrip/interleaved_voices.musicxml");

        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlCommand(source, command);
        String json = MusicXmlState.applyMusicXmlCommand(source, command);

        assertUnsupportedNoChangedTargets(validation, json);
        assertEquals("1:C:4:1|2:G:3:1|1:D:4:1|1:E:4:1", noteSignatureSequence(source));
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

    public static String sampleGrandStaffMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef>"
                + "<clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice><staff>1</staff></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleGrandStaffSameVoiceBackupMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>960</divisions><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef>"
                + "<clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes>\n"
                + "      <note><rest measure=\"yes\"/><duration>3840</duration><voice>1</voice><staff>1</staff></note>\n"
                + "      <backup><duration>3840</duration></backup>\n"
                + "      <note><rest measure=\"yes\"/><duration>3840</duration><voice>1</voice><staff>2</staff></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";
    }

    public static String sampleUnderfullInsertMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
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

    public static String sampleMixedVoiceDurationMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice></note>\n"
                + "      <note><pitch><step>G</step><octave>3</octave></pitch><duration>1</duration><voice>2</voice></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice></note>\n"
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

    public static String sampleGraceNoteWithoutDurationMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>\n"
                + "      <note><grace/><pitch><step>G</step><octave>5</octave></pitch><voice>1</voice><type>16th</type></note>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><rest/><duration>1440</duration><voice>1</voice><type>half</type><dot/></note>\n"
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

    private static void assertSaveDiagnostic(String fixture, boolean dirty, String code) throws Exception {
        MusicXmlCommandValidation validation = MusicXmlState.validateMusicXmlForSave(
                loadMusicXmlStateFixture(fixture), dirty);

        assertEquals(false, validation.isOk());
        assertEquals(code, validation.getDiagnostics().get(0).getCode());
    }

    private static void assertUnsupportedNoChangedTargets(MusicXmlCommandValidation validation, String json) {
        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", validation.getDiagnostics().get(0).getCode());
        assertEquals(0, validation.getChangedNodeIds().size());
        assertEquals(0, validation.getAffectedMeasureNumbers().size());
        assertTrue(json.contains("\"kind\": \"musicxml_command_apply\""));
        assertTrue(json.contains("\"code\": \"MVP_UNSUPPORTED_NON_EDITABLE_VOICE\""));
        assertTrue(json.contains("\"changed_node_ids\": []"));
        assertTrue(json.contains("\"affected_measure_numbers\": []"));
    }

    private static void assertUnsupportedNoteKindNoChangedTargets(MusicXmlCommandValidation validation, String json) {
        assertEquals(false, validation.isOk());
        assertEquals("MVP_UNSUPPORTED_NOTE_KIND", validation.getDiagnostics().get(0).getCode());
        assertEquals(0, validation.getChangedNodeIds().size());
        assertEquals(0, validation.getAffectedMeasureNumbers().size());
        assertTrue(json.contains("\"kind\": \"musicxml_command_apply\""));
        assertTrue(json.contains("\"code\": \"MVP_UNSUPPORTED_NOTE_KIND\""));
        assertTrue(json.contains("\"changed_node_ids\": []"));
        assertTrue(json.contains("\"affected_measure_numbers\": []"));
    }

    private static String loadMusicXmlStateFixture(String name) throws Exception {
        return loadTestFixture("musicxml-state/" + name);
    }

    private static String loadTestFixture(String path) throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        assertTrue(stream != null, path);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }

    private static String noteSignatureSequence(String xml) throws Exception {
        Document doc = parseTestXml(xml);
        NodeList notes = doc.getElementsByTagName("note");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < notes.getLength(); index++) {
            if (index > 0) {
                builder.append("|");
            }
            Element note = (Element) notes.item(index);
            String voice = firstElementText(note, "voice");
            String duration = firstElementText(note, "duration");
            if (note.getElementsByTagName("rest").getLength() > 0) {
                builder.append("rest:").append(voice).append(":").append(duration);
            } else {
                Element pitch = (Element) note.getElementsByTagName("pitch").item(0);
                builder.append(voice).append(":").append(firstElementText(pitch, "step")).append(":")
                        .append(firstElementText(pitch, "octave")).append(":").append(duration);
            }
        }
        return builder.toString();
    }

    private static int sumDurationForMeasureVoice(String xml, String measureNumber, String voice) throws Exception {
        Document doc = parseTestXml(xml);
        NodeList measures = doc.getElementsByTagName("measure");
        for (int measureIndex = 0; measureIndex < measures.getLength(); measureIndex++) {
            Element measure = (Element) measures.item(measureIndex);
            if (!measureNumber.equals(measure.getAttribute("number"))) {
                continue;
            }
            int sum = 0;
            NodeList notes = measure.getElementsByTagName("note");
            for (int noteIndex = 0; noteIndex < notes.getLength(); noteIndex++) {
                Element note = (Element) notes.item(noteIndex);
                if (!voice.equals(firstElementText(note, "voice"))) {
                    continue;
                }
                sum += Integer.parseInt(firstElementText(note, "duration"));
            }
            return sum;
        }
        return 0;
    }

    private static String firstMeasureAttributesSignature(String xml) throws Exception {
        Document doc = parseTestXml(xml);
        Element measure = (Element) doc.getElementsByTagName("measure").item(0);
        Element attributes = (Element) measure.getElementsByTagName("attributes").item(0);
        Element time = (Element) attributes.getElementsByTagName("time").item(0);
        return firstElementText(attributes, "divisions") + ":" + firstElementText(time, "beats") + "/"
                + firstElementText(time, "beat-type");
    }

    private static Document parseTestXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String firstElementText(Element parent, String tagName) {
        NodeList elements = parent.getElementsByTagName(tagName);
        return elements.getLength() == 0 ? "" : elements.item(0).getTextContent().trim();
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
