package jp.igapyon.mikuscore.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuscore.musicxml.MusicXmlStateTest;

public class ScoreCoreTest {
    @Test
    public void rejectsInvalidXmlAndNonScorePartwiseRootOnLoad() {
        ScoreCore core = new ScoreCore();

        IllegalArgumentException invalidXml = assertThrows(IllegalArgumentException.class,
                () -> core.load("<score-partwise"));
        assertEquals("Invalid XML input.", invalidXml.getMessage());

        IllegalArgumentException invalidRoot = assertThrows(IllegalArgumentException.class,
                () -> core.load("<score-timewise version=\"4.0\"/>"));
        assertEquals("MusicXML root must be <score-partwise>.", invalidRoot.getMessage());
        assertFalse(core.save().isOk());
        assertEquals("MVP_SCORE_NOT_LOADED", core.save().getDiagnostics().get(0).getCode());
    }

    @Test
    public void reloadKeepsTheNodeIdCounterAndFailedReloadUpdatesOnlyOriginalText() {
        ScoreCore core = new ScoreCore();
        String source = MusicXmlStateTest.sampleMusicXml("Reload lifecycle");
        core.load(source);
        assertEquals(Arrays.asList("n1", "n2", "n3"), core.listNoteNodeIds());

        core.load(source);
        assertEquals(Arrays.asList("n4", "n5", "n6"), core.listNoteNodeIds());

        assertThrows(IllegalArgumentException.class, () -> core.load("<score-partwise"));
        assertEquals(Arrays.asList("n4", "n5", "n6"), core.listNoteNodeIds());
        assertFalse(core.isDirty());
        assertEquals("<score-partwise", core.save().getXml());
    }

    @Test
    public void preservesLoadDispatchAndSaveLifecycle() {
        ScoreCore core = new ScoreCore();

        ScoreCore.DispatchResult unloaded = core.dispatch("{\"type\":\"ui_noop\",\"reason\":\"cursor_move\"}");
        assertFalse(unloaded.isOk());
        assertEquals("MVP_SCORE_NOT_LOADED", unloaded.getDiagnostics().get(0).getCode());

        String source = MusicXmlStateTest.sampleMusicXml("Stateful Core");
        core.load(source);
        assertFalse(core.isDirty());
        assertEquals(source, core.save().getXml());
        assertEquals("original_noop", core.save().getMode());
        assertTrue(core.listNoteNodeIds().contains("n1"));

        ScoreCore.DispatchResult changed = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");
        assertTrue(changed.isOk());
        assertTrue(changed.isDirtyChanged());
        assertEquals("1", changed.getAffectedMeasureNumbers().get(0));
        assertTrue(core.isDirty());
        assertTrue(core.debugSerializeCurrentXml().contains("<step>A</step>"));

        ScoreCore.SaveResult saved = core.save();
        assertTrue(saved.isOk());
        assertEquals("serialized_dirty", saved.getMode());
        assertTrue(saved.getXml().contains("<step>A</step>"));

        ScoreCore.DispatchResult noop = core.dispatch("{\"type\":\"ui_noop\",\"reason\":\"cursor_move\"}");
        assertTrue(noop.isOk());
        assertFalse(noop.isDirtyChanged());
        assertNotNull(core.debugSerializeCurrentXml());
    }

    @Test
    public void successfulDispatchOnAnAlreadyDirtyCoreKeepsTheFullResultShape() {
        ScoreCore core = new ScoreCore();
        core.load(MusicXmlStateTest.sampleMusicXml("Dirty result shape"));

        ScoreCore.DispatchResult first = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");
        ScoreCore.DispatchResult second = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n2\","
                + "\"voice\":\"2\",\"pitch\":{\"step\":\"B\",\"octave\":4}}");

        assertTrue(first.isOk());
        assertTrue(first.isDirtyChanged());
        assertTrue(second.isOk());
        assertFalse(second.isDirtyChanged());
        assertEquals(Arrays.asList("n2"), second.getChangedNodeIds());
        assertEquals(Arrays.asList("1"), second.getAffectedMeasureNumbers());
        assertTrue(second.getWarnings().isEmpty());
        assertTrue(second.getDiagnostics().isEmpty());
        assertTrue(core.isDirty());
    }

    @Test
    public void cleanSaveIntegrityFailureUsesTheUpstreamSerializedDirtyModeAndContext() {
        ScoreCore core = new ScoreCore();
        String invalid = MusicXmlStateTest.sampleMusicXml("Invalid duration")
                .replace("<duration>1</duration><voice>1</voice>", "<duration>0</duration><voice>1</voice>");
        core.load(invalid);

        ScoreCore.SaveResult saved = core.save();

        assertFalse(saved.isOk());
        assertEquals("serialized_dirty", saved.getMode());
        assertEquals("MVP_INVALID_NOTE_DURATION", saved.getDiagnostics().get(0).getCode());
        assertEquals("Note is missing a valid positive <duration> value. part=P1 measure=1 voice=1 nodeId=n1 "
                + "grace=false cue=false rest=false chord=false", saved.getDiagnostics().get(0).getMessage());
    }

    @Test
    public void deleteWithAnInvalidDurationUsesTheUpstreamDispatchDiagnosticWithoutMutation() {
        ScoreCore core = new ScoreCore();
        String source = MusicXmlStateTest.sampleMusicXml("Invalid delete duration")
                .replace("<duration>1</duration><voice>1</voice>", "<duration>0</duration><voice>1</voice>");
        core.load(source);

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"delete_note\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\"}");

        assertFalse(result.isOk());
        assertEquals("MVP_INVALID_NOTE_DURATION", result.getDiagnostics().get(0).getCode());
        assertEquals("Target note has invalid duration.", result.getDiagnostics().get(0).getMessage());
        assertFalse(core.isDirty());
        assertTrue(core.debugSerializeCurrentXml().contains("<duration>0</duration>"));
    }

    @Test
    public void grandStaffPitchAssignmentUsesNumberCoercionAndExactClefNumberSelectors() {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><staves>0x2</staves><clef number=\"1\"><sign>G</sign></clef>"
                + "<clef number=\"2\"><sign>F</sign></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";
        ScoreCore hexadecimalStaves = new ScoreCore();
        hexadecimalStaves.load(source);

        ScoreCore.DispatchResult changed = hexadecimalStaves.dispatch("{\"type\":\"change_to_pitch\","
                + "\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"C\",\"octave\":6}}");

        assertTrue(changed.isOk());
        assertTrue(hexadecimalStaves.debugSerializeCurrentXml().contains("<staff>1</staff>"));

        ScoreCore whitespaceClefNumber = new ScoreCore();
        whitespaceClefNumber.load(source.replace("number=\"1\"", "number=\" 1 \""));
        ScoreCore.DispatchResult whitespaceChanged = whitespaceClefNumber.dispatch("{\"type\":\"change_to_pitch\","
                + "\"targetNodeId\":\"n1\",\"voice\":\"1\",\"pitch\":{\"step\":\"C\",\"octave\":6}}");

        assertTrue(whitespaceChanged.isOk());
        assertFalse(whitespaceClefNumber.debugSerializeCurrentXml().contains("<staff>1</staff>"));
    }

    @Test
    public void editableVoiceMissingCommandFieldUsesTheUpstreamUndefinedDiagnostic() {
        ScoreCore core = new ScoreCore(new ScoreCore.Options("1"));
        core.load(MusicXmlStateTest.sampleMusicXml("Editable voice missing field"));

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"pitch\":{\"step\":\"A\",\"octave\":4}}");

        assertFalse(result.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", result.getDiagnostics().get(0).getCode());
        assertEquals("Voice undefined is not editable in MVP.", result.getDiagnostics().get(0).getMessage());
        assertFalse(core.isDirty());
    }

    @Test
    public void uiNoopKeepsACleanCoreInOriginalNoopMode() {
        ScoreCore core = new ScoreCore();
        String source = MusicXmlStateTest.sampleMusicXml("Clean UI noop");
        core.load(source);

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"ui_noop\",\"reason\":\"cursor_move\"}");

        assertTrue(result.isOk());
        assertFalse(result.isDirtyChanged());
        assertTrue(result.getChangedNodeIds().isEmpty());
        assertTrue(result.getAffectedMeasureNumbers().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        assertFalse(core.isDirty());
        assertEquals(source, core.save().getXml());
        assertEquals("original_noop", core.save().getMode());
    }

    @Test
    public void rejectsChangeCommandWhenTargetVoiceDoesNotMatchCommandVoice() {
        ScoreCore core = new ScoreCore();
        core.load(MusicXmlStateTest.sampleMusicXml("Target voice mismatch"));

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"2\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");

        assertFalse(result.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", result.getDiagnostics().get(0).getCode());
        assertTrue(result.getChangedNodeIds().isEmpty());
        assertTrue(result.getAffectedMeasureNumbers().isEmpty());
        assertFalse(core.isDirty());
        assertEquals("original_noop", core.save().getMode());
    }

    @Test
    public void cleanSaveAllowsTheUpstreamTupletRoundingTolerance() {
        StringBuilder notes = new StringBuilder();
        String[] tupletSteps = new String[] { "C", "D", "E", "F", "G", "A", "B" };
        for (String step : tupletSteps) {
            notes.append("<note><pitch><step>").append(step).append("</step><octave>4</octave></pitch>")
                    .append("<duration>69</duration><voice>1</voice><type>32nd</type><time-modification>")
                    .append("<actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>");
        }
        for (String step : new String[] { "C", "D", "E", "F" }) {
            notes.append("<note><pitch><step>").append(step).append("</step><octave>5</octave></pitch>")
                    .append("<duration>120</duration><voice>1</voice><type>16th</type></note>");
        }
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type></time></attributes>"
                + notes + "</measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.SaveResult saved = core.save();

        assertTrue(saved.isOk());
        assertEquals("original_noop", saved.getMode());
    }

    @Test
    public void rejectsOverfullDurationUsingInheritedAttributesFromThePreviousMeasure() {
        ScoreCore core = loadFixtureCore("abc-roundtrip/inherited_attributes.musicxml");

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_duration\",\"targetNodeId\":\"n5\","
                + "\"voice\":\"1\",\"duration\":2}");

        assertFalse(result.isOk());
        assertEquals("MEASURE_OVERFULL", result.getDiagnostics().get(0).getCode());
        assertFalse(core.isDirty());
    }

    @Test
    public void timingRollbackReindexesNodeIdsLikeTheUpstreamWeakMapRestore() {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "</attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration>"
                + "<voice>1</voice></note><note><pitch><step>D</step><octave>4</octave></pitch><duration>2</duration>"
                + "<voice>1</voice></note></measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_duration\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"duration\":3}");

        assertFalse(result.isOk());
        assertEquals("MEASURE_OVERFULL", result.getDiagnostics().get(0).getCode());
        assertFalse(core.isDirty());
        assertEquals(Arrays.asList("n3", "n4"), core.listNoteNodeIds());
        assertEquals(source, core.save().getXml());
    }

    @Test
    public void rejectsOverfullDurationUsingUpdatedDivisionsAndInheritedTime() {
        ScoreCore core = loadFixtureCore("abc-roundtrip/inherited_divisions_changed.musicxml");

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_duration\",\"targetNodeId\":\"n5\","
                + "\"voice\":\"1\",\"duration\":3}");

        assertFalse(result.isOk());
        assertEquals("MEASURE_OVERFULL", result.getDiagnostics().get(0).getCode());
        assertFalse(core.isDirty());
    }

    @Test
    public void rejectsOverfullDurationUsingUpdatedTimeAndInheritedDivisions() {
        ScoreCore core = loadFixtureCore("abc-roundtrip/inherited_time_changed.musicxml");

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_duration\",\"targetNodeId\":\"n5\","
                + "\"voice\":\"1\",\"duration\":2}");

        assertFalse(result.isOk());
        assertEquals("MEASURE_OVERFULL", result.getDiagnostics().get(0).getCode());
        assertFalse(core.isDirty());
    }

    @Test
    public void dispatchDoesNotTreatANestedUiNoopDiscriminatorAsAUiCommand() {
        ScoreCore core = new ScoreCore();
        core.load(MusicXmlStateTest.sampleMusicXml("Nested discriminator"));

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4,\"metadata\":{\"type\":\"ui_noop\"}}}");

        assertTrue(result.isOk());
        assertTrue(result.isDirtyChanged());
        assertTrue(core.isDirty());
        assertTrue(core.save().getXml().contains("<step>A</step>"));
    }

    @Test
    public void nodeIdCommandWithoutVoiceIsRejectedAgainstTheTargetVoice() {
        ScoreCore core = new ScoreCore();
        core.load(MusicXmlStateTest.sampleMusicXml("Missing command voice"));

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"pitch\":{\"step\":\"A\",\"octave\":4}}");

        assertFalse(result.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", result.getDiagnostics().get(0).getCode());
        assertEquals("Target note voice (1) does not match command voice (undefined).",
                result.getDiagnostics().get(0).getMessage());
        assertFalse(core.isDirty());
    }

    @Test
    public void insertWithAMissingAnchorVoiceStillChecksTheFollowingVoiceLane() {
        ScoreCore core = new ScoreCore();
        core.load("<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>2</voice></note>"
                + "</measure></part></score-partwise>");

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"insert_note_after\","
                + "\"anchorNodeId\":\"n1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"E\",\"octave\":4}}}");

        assertFalse(result.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", result.getDiagnostics().get(0).getCode());
        assertEquals("Insert is restricted to a continuous local voice lane in MVP.",
                result.getDiagnostics().get(0).getMessage());
        assertFalse(core.isDirty());
    }

    @Test
    public void usesJavaScriptNumberStringificationWhenFillingAMissingTargetVoice() {
        ScoreCore core = new ScoreCore();
        core.load("<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration></note>"
                + "</measure></part></score-partwise>");

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":1e0,\"pitch\":{\"step\":\"D\",\"octave\":4}}");

        assertTrue(result.isOk());
        assertTrue(core.save().getXml().contains("<voice>1</voice>"));
        assertFalse(core.save().getXml().contains("<voice>1.0</voice>"));
    }

    @Test
    public void unknownRuntimeCommandUsesTheUpstreamMissingTargetContract() {
        ScoreCore core = new ScoreCore();
        core.load(MusicXmlStateTest.sampleMusicXml("Unknown command"));

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"unknown_command\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}");

        assertFalse(result.isOk());
        assertEquals("MVP_COMMAND_TARGET_MISSING", result.getDiagnostics().get(0).getCode());
        assertEquals("Command target is missing.", result.getDiagnostics().get(0).getMessage());
        assertFalse(core.isDirty());
    }

    @Test
    public void nonStringCommandTargetsUseNodeTruthinessWithoutStringKeyCoercion() {
        ScoreCore core = new ScoreCore();
        core.load(MusicXmlStateTest.sampleMusicXml("Unchecked target values"));

        ScoreCore.DispatchResult numeric = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":1,"
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");
        assertFalse(numeric.isOk());
        assertEquals("MVP_TARGET_NOT_FOUND", numeric.getDiagnostics().get(0).getCode());
        assertEquals("Unknown nodeId: 1", numeric.getDiagnostics().get(0).getMessage());

        ScoreCore.DispatchResult array = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":[1],"
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");
        assertFalse(array.isOk());
        assertEquals("MVP_TARGET_NOT_FOUND", array.getDiagnostics().get(0).getCode());
        assertEquals("Unknown nodeId: 1", array.getDiagnostics().get(0).getMessage());

        ScoreCore.DispatchResult object = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":{},"
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");
        assertFalse(object.isOk());
        assertEquals("MVP_TARGET_NOT_FOUND", object.getDiagnostics().get(0).getCode());
        assertEquals("Unknown nodeId: [object Object]", object.getDiagnostics().get(0).getMessage());

        ScoreCore.DispatchResult falseTarget = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":false,"
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");
        assertFalse(falseTarget.isOk());
        assertEquals("MVP_COMMAND_TARGET_MISSING", falseTarget.getDiagnostics().get(0).getCode());
        assertFalse(core.isDirty());
    }

    @Test
    public void editsNamespacedMusicXmlWithoutDroppingUnknownMarkup() {
        String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise xmlns=\"https://www.musicxml.org/ns/musicxml\" version=\"4.0\" data-source=\"kept\""
                + " data-mikuscore-java-node-id=\"vendor-kept\""
                + " data-mikuscore-java-internal-node-id=\"vendor-internal-root-kept\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note custom-note=\"kept\" data-mikuscore-java-node-id=\"vendor-note-kept\""
                + " data-mikuscore-java-internal-node-id=\"vendor-internal-note-kept\""
                + " data-mikuscore-java-internal-node-id-1=\"mksj:n999\">"
                + "<pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>4</duration><voice>1</voice><vendor-data code=\"x\">keep</vendor-data></note>"
                + "</measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.DispatchResult changed = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"D\",\"octave\":4}}");
        ScoreCore.SaveResult saved = core.save();

        assertTrue(changed.isOk());
        assertTrue(saved.isOk());
        assertTrue(saved.getXml().contains("xmlns=\"https://www.musicxml.org/ns/musicxml\""));
        assertTrue(saved.getXml().contains("data-source=\"kept\""));
        assertTrue(saved.getXml().contains("data-mikuscore-java-node-id=\"vendor-kept\""));
        assertTrue(saved.getXml().contains("data-mikuscore-java-internal-node-id=\"vendor-internal-root-kept\""));
        assertTrue(saved.getXml().contains("custom-note=\"kept\""));
        assertTrue(saved.getXml().contains("data-mikuscore-java-node-id=\"vendor-note-kept\""));
        assertTrue(saved.getXml().contains("data-mikuscore-java-internal-node-id=\"vendor-internal-note-kept\""));
        assertTrue(saved.getXml().contains("data-mikuscore-java-internal-node-id-1=\"mksj:n999\""));
        assertTrue(saved.getXml().contains("<vendor-data code=\"x\">keep</vendor-data>"));
        assertFalse(saved.getXml().contains("mksj:n1"));
    }

    @Test
    public void statefulCoreResolvesDocumentOrderNoteIdsInsideVendorMarkup() {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "</attributes><vendor-wrapper><note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>4</duration><voice>1</voice></note></vendor-wrapper></measure></part>"
                + "</score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        assertEquals(Arrays.asList("n1"), core.listNoteNodeIds());
        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"D\",\"octave\":4}}");

        assertTrue(result.isOk());
        assertEquals(Arrays.asList("n1"), result.getChangedNodeIds());
        assertEquals(Arrays.asList("1"), result.getAffectedMeasureNumbers());
        assertTrue(core.save().getXml().contains("<step>D</step>"));
    }

    @Test
    public void editableVoiceRejectsOtherVoiceAndAllowsTheConfiguredVoice() {
        String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>Music</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice></note>"
                + "<note><pitch><step>G</step><octave>3</octave></pitch><duration>4</duration><voice>2</voice></note>"
                + "</measure></part></score-partwise>";
        ScoreCore core = new ScoreCore(new ScoreCore.Options(" 2 "));
        core.load(source);

        ScoreCore.DispatchResult rejected = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"A\",\"octave\":4}}");
        assertFalse(rejected.isOk());
        assertEquals("MVP_UNSUPPORTED_NON_EDITABLE_VOICE", rejected.getDiagnostics().get(0).getCode());
        assertEquals("Voice 1 is not editable in MVP.", rejected.getDiagnostics().get(0).getMessage());
        assertFalse(core.isDirty());

        ScoreCore.DispatchResult accepted = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n2\","
                + "\"voice\":\"2\",\"pitch\":{\"step\":\"A\",\"octave\":3}}");
        assertTrue(accepted.isOk());
        assertTrue(core.isDirty());
        assertTrue(core.save().getXml().contains("<step>A</step>"));
    }

    @Test
    public void editableVoiceLimitsSaveOverfullValidationToThatVoice() {
        String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>Music</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>5</duration><voice>1</voice></note>"
                + "<note><pitch><step>G</step><octave>3</octave></pitch><duration>4</duration><voice>2</voice></note>"
                + "</measure></part></score-partwise>";

        ScoreCore unrestricted = new ScoreCore();
        unrestricted.load(source);
        assertFalse(unrestricted.save().isOk());
        assertEquals("MEASURE_OVERFULL", unrestricted.save().getDiagnostics().get(0).getCode());

        ScoreCore voiceTwoOnly = new ScoreCore(new ScoreCore.Options("2"));
        voiceTwoOnly.load(source);
        assertTrue(voiceTwoOnly.save().isOk());
        assertEquals("original_noop", voiceTwoOnly.save().getMode());
    }

    @Test
    public void structuralEditsRetainExistingNodeIdsAndDoNotExposeInternalMarkers() {
        String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>Music</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);
        assertEquals(Arrays.asList("n1", "n2", "n3"), core.listNoteNodeIds());

        ScoreCore.DispatchResult inserted = core.dispatch("{\"type\":\"insert_note_after\",\"anchorNodeId\":\"n1\","
                + "\"voice\":\"1\",\"note\":{\"duration\":1,\"pitch\":{\"step\":\"A\",\"octave\":4}}}");
        assertTrue(inserted.isOk());
        assertEquals(Arrays.asList("n1", "n4"), inserted.getChangedNodeIds());
        assertEquals(Arrays.asList("n1", "n4", "n2", "n3"), core.listNoteNodeIds());

        ScoreCore.DispatchResult originalThird = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n3\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"F\",\"octave\":4}}");
        assertTrue(originalThird.isOk());
        assertEquals(Arrays.asList("n3"), originalThird.getChangedNodeIds());

        ScoreCore.DispatchResult deletedToRest = core.dispatch("{\"type\":\"delete_note\",\"targetNodeId\":\"n2\","
                + "\"voice\":\"1\"}");
        assertTrue(deletedToRest.isOk());
        assertEquals(Arrays.asList("n2"), deletedToRest.getChangedNodeIds());
        assertEquals(Arrays.asList("n1", "n4", "n2", "n3"), core.listNoteNodeIds());
        assertFalse(core.debugSerializeCurrentXml().contains("data-mikuscore-java-node-id"));
        assertFalse(core.save().getXml().contains("data-mikuscore-java-node-id"));
    }

    @Test
    public void structuralDeleteDoesNotReuseAFormerNodeId() {
        String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>Music</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice></note>"
                + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice></note>"
                + "<note><pitch><step>G</step><octave>3</octave></pitch><duration>2</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.DispatchResult deletedChordHead = core.dispatch("{\"type\":\"delete_note\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\"}");
        assertTrue(deletedChordHead.isOk());
        assertEquals(Arrays.asList("n1"), deletedChordHead.getChangedNodeIds());
        assertEquals(Arrays.asList("n2", "n3"), core.listNoteNodeIds());

        ScoreCore.DispatchResult split = core.dispatch("{\"type\":\"split_note\",\"targetNodeId\":\"n3\","
                + "\"voice\":\"1\"}");
        assertTrue(split.isOk());
        assertEquals(Arrays.asList("n3", "n4"), split.getChangedNodeIds());
        assertEquals(Arrays.asList("n2", "n3", "n4"), core.listNoteNodeIds());
    }

    @Test
    public void durationChangeKeepsExistingIdsWhenItAddsAnUnderfullRest() {
        String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>Music</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_duration\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"duration\":1}");
        assertTrue(result.isOk());
        assertEquals(Arrays.asList("n1"), result.getChangedNodeIds());
        assertEquals(Arrays.asList("n1", "n3", "n2"), core.listNoteNodeIds());
        assertTrue(core.save().isOk());
    }

    @Test
    public void cleanSaveAcceptsFiniteJavaScriptRadixDurationsBeyondLongRange() {
        String[] radixDurations = new String[] { "0x10000000000000000", "0b10000000000000000000000000000000000000000000000000000000000000000",
                "0o2000000000000000000000" };
        for (String duration : radixDurations) {
            String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                    + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>" + duration
                    + "</duration><voice>1</voice></note></measure></part></score-partwise>";
            ScoreCore core = new ScoreCore();
            core.load(source);

            ScoreCore.SaveResult saved = core.save();

            assertTrue(saved.isOk(), duration);
            assertEquals("original_noop", saved.getMode(), duration);
            assertEquals(source, saved.getXml(), duration);
        }
    }

    @Test
    public void postEnsureFailuresRetainOnlyTheUpstreamDebugVoiceNormalization() {
        assertPostEnsureFailureRetainsMissingVoice("<duration>1</duration>",
                "{\"type\":\"change_duration\",\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":2}",
                "MVP_INVALID_COMMAND_PAYLOAD", true);
        assertPostEnsureFailureRetainsMissingVoice("<duration>1</duration>",
                "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}",
                "MVP_INVALID_COMMAND_PAYLOAD", true);
        assertPostEnsureFailureRetainsMissingVoice("<duration>0</duration>",
                "{\"type\":\"delete_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}",
                "MVP_INVALID_NOTE_DURATION", false);
    }

    private static void assertPostEnsureFailureRetainsMissingVoice(String durationXml, String command,
            String expectedDiagnostic, boolean cleanSaveIsValid) {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>3</divisions><time><beats>1</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>" + durationXml
                + "</note></measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.DispatchResult result = core.dispatch(command);

        assertFalse(result.isOk());
        assertEquals(expectedDiagnostic, result.getDiagnostics().get(0).getCode());
        assertFalse(core.isDirty());
        assertTrue(core.debugSerializeCurrentXml().contains("<voice>1</voice>"));
        ScoreCore.SaveResult saved = core.save();
        assertEquals(Boolean.valueOf(cleanSaveIsValid), Boolean.valueOf(saved.isOk()));
        if (cleanSaveIsValid) {
            assertEquals("original_noop", saved.getMode());
            assertEquals(source, saved.getXml());
        } else {
            assertEquals("serialized_dirty", saved.getMode());
            assertEquals("", saved.getXml());
            assertEquals("MVP_INVALID_NOTE_DURATION", saved.getDiagnostics().get(0).getCode());
        }
    }

    @Test
    public void splitAcceptsFiniteSourceDurationsBeyondTheJavaIntRange() {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>0x100000000</duration>"
                + "<voice>1</voice></note></measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.DispatchResult result = core.dispatch(
                "{\"type\":\"split_note\",\"targetNodeId\":\"n1\",\"voice\":\"1\"}");

        assertTrue(result.isOk());
        assertEquals(Arrays.asList("n1", "n2"), result.getChangedNodeIds());
        String saved = core.save().getXml();
        assertTrue(saved.contains("<duration>2147483648</duration><voice>1</voice></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>2147483648</duration>"));
    }

    @Test
    public void commandDurationsUseTheFiniteJavaScriptIntegerRange() {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";
        String duration = "4294967296";

        ScoreCore durationCore = new ScoreCore();
        durationCore.load(source);
        ScoreCore.DispatchResult durationResult = durationCore.dispatch("{\"type\":\"change_duration\","
                + "\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":" + duration + "}");
        assertTrue(durationResult.isOk());
        assertTrue(durationCore.save().getXml().contains("<duration>" + duration + "</duration>"));

        ScoreCore insertCore = new ScoreCore();
        insertCore.load(source);
        ScoreCore.DispatchResult insertResult = insertCore.dispatch("{\"type\":\"insert_note_after\","
                + "\"anchorNodeId\":\"n1\",\"voice\":\"1\",\"note\":{\"duration\":" + duration
                + ",\"pitch\":{\"step\":\"D\",\"octave\":4}}}");
        assertTrue(insertResult.isOk());
        assertTrue(insertCore.save().getXml().contains("<duration>" + duration + "</duration>"));

        String metered = source.replace("<measure number=\"1\">",
                "<measure number=\"1\"><attributes><divisions>1</divisions><time><beats>4</beats>"
                        + "<beat-type>4</beat-type></time></attributes>");
        ScoreCore overfullCore = new ScoreCore();
        overfullCore.load(metered);
        ScoreCore.DispatchResult overfull = overfullCore.dispatch("{\"type\":\"change_duration\","
                + "\"targetNodeId\":\"n1\",\"voice\":\"1\",\"duration\":" + duration + "}");
        assertFalse(overfull.isOk());
        assertEquals("MEASURE_OVERFULL", overfull.getDiagnostics().get(0).getCode());
        assertEquals("Projected occupied time " + duration + " exceeds capacity 4.",
                overfull.getDiagnostics().get(0).getMessage());
        assertFalse(overfullCore.isDirty());
    }

    @Test
    public void commandPitchOctaveUsesTheFiniteJavaScriptIntegerRangeAndStaffThreshold() {
        String source = "<score-partwise version=\"4.0\"><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><staves>2</staves><clef number=\"1\"><sign>G</sign></clef>"
                + "<clef number=\"2\"><sign>F</sign></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";
        ScoreCore core = new ScoreCore();
        core.load(source);

        ScoreCore.DispatchResult result = core.dispatch("{\"type\":\"change_to_pitch\",\"targetNodeId\":\"n1\","
                + "\"voice\":\"1\",\"pitch\":{\"step\":\"C\",\"octave\":4294967296}}");

        assertTrue(result.isOk());
        String saved = core.save().getXml();
        assertTrue(saved.contains("<octave>4294967296</octave>"));
        assertTrue(saved.contains("<staff>1</staff>"));
    }

    private static ScoreCore loadFixtureCore(String fixture) {
        ScoreCore core = new ScoreCore();
        core.load(loadFixture(fixture));
        return core;
    }

    private static String loadFixture(String fixture) {
        try (InputStream stream = ScoreCoreTest.class.getClassLoader().getResourceAsStream(fixture)) {
            assertNotNull(stream, "Missing test fixture: " + fixture);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read test fixture: " + fixture, ex);
        }
    }
}
