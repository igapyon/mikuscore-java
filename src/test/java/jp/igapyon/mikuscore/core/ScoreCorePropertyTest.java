package jp.igapyon.mikuscore.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Deterministic Java port of tests/property/core.property.spec.ts.
 *
 * <p>Failures include their seed and step in the assertion message, allowing
 * the exact command sequence to be reproduced without a property-test runtime
 * dependency.</p>
 */
public class ScoreCorePropertyTest {
    private static final List<Integer> REJECT_PATH_SEEDS = Arrays.asList(Integer.valueOf(1), Integer.valueOf(7),
            Integer.valueOf(17), Integer.valueOf(31), Integer.valueOf(97));

    @Test
    public void rejectedCommandsKeepStateAndChangedIdsUnchanged() {
        String baseXml = loadFixture("abc-roundtrip/base.musicxml");
        for (Integer seedValue : REJECT_PATH_SEEDS) {
            int seed = seedValue.intValue();
            Lcg random = new Lcg(seed);
            ScoreCore core = new ScoreCore();
            core.load(baseXml);

            for (int step = 0; step < 120; step++) {
                ScoreCore.SaveResult before = core.save();
                String command = randomCommand(random, core.listNoteNodeIds());
                ScoreCore.DispatchResult result = core.dispatch(command);

                if (!result.isOk()) {
                    String context = "seed=" + seed + " step=" + step + " command=" + command;
                    assertTrue(result.getChangedNodeIds().isEmpty(), context);
                    ScoreCore.SaveResult after = core.save();
                    assertEquals(before.isOk(), after.isOk(), context);
                    assertEquals(before.getMode(), after.getMode(), context);
                    assertEquals(before.getXml(), after.getXml(), context);
                }
            }
        }
    }

    @Test
    public void nonStructuralRandomEditsPreserveUnknownBeamAndBackupMarkers() {
        assertPreservesMarker("abc-roundtrip/with_unknown.musicxml", "<unknown-tag foo=\"bar\">x</unknown-tag>");
        assertPreservesMarker("abc-roundtrip/with_beam.musicxml", "<beam number=\"1\">begin</beam>");
        assertPreservesMarker("abc-roundtrip/with_backup_safe.musicxml", "<backup><duration>1</duration></backup>");
    }

    private static void assertPreservesMarker(String fixture, String marker) {
        Lcg random = new Lcg(marker.length() * 13 + 11);
        ScoreCore core = new ScoreCore();
        core.load(loadFixture(fixture));

        for (int step = 0; step < 80; step++) {
            String command = randomNonStructuralCommand(random, core.listNoteNodeIds());
            core.dispatch(command);
            ScoreCore.SaveResult saved = core.save();
            if (saved.isOk()) {
                assertTrue(saved.getXml().contains(marker),
                        "fixture=" + fixture + " step=" + step + " command=" + command);
                assertFalse(saved.getXml().contains("data-mikuscore-java-internal-node-id"),
                        "fixture=" + fixture + " step=" + step);
            }
        }
    }

    private static String randomCommand(Lcg random, List<String> nodeIds) {
        if (nodeIds.isEmpty() || random.chance(10)) {
            return "{\"type\":\"ui_noop\",\"reason\":\"cursor_move\"}";
        }
        String target = random.pick(nodeIds);
        String type = random.pick(Arrays.asList("change_to_pitch", "change_duration", "insert_note_after", "delete_note"));
        String voice = random.chance(85) ? "1" : "2";
        if ("change_to_pitch".equals(type)) {
            return "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"" + target + "\",\"voice\":\""
                    + voice + "\",\"pitch\":" + randomPitch(random) + "}";
        }
        if ("change_duration".equals(type)) {
            int duration = random.chance(95) ? random.intValue(1, 4) : 0;
            return "{\"type\":\"change_duration\",\"targetNodeId\":\"" + target + "\",\"voice\":\""
                    + voice + "\",\"duration\":" + duration + "}";
        }
        if ("insert_note_after".equals(type)) {
            int duration = random.chance(95) ? random.intValue(1, 2) : 0;
            return "{\"type\":\"insert_note_after\",\"anchorNodeId\":\"" + target + "\",\"voice\":\""
                    + voice + "\",\"note\":{\"duration\":" + duration + ",\"pitch\":" + randomPitch(random)
                    + "}}";
        }
        return "{\"type\":\"delete_note\",\"targetNodeId\":\"" + target + "\",\"voice\":\"" + voice
                + "\"}";
    }

    private static String randomNonStructuralCommand(Lcg random, List<String> nodeIds) {
        if (nodeIds.isEmpty() || random.chance(15)) {
            return "{\"type\":\"ui_noop\",\"reason\":\"cursor_move\"}";
        }
        String target = random.pick(nodeIds);
        String voice = random.chance(90) ? "1" : "2";
        if (random.pick(Arrays.asList("change_to_pitch", "change_duration")).equals("change_to_pitch")) {
            return "{\"type\":\"change_to_pitch\",\"targetNodeId\":\"" + target + "\",\"voice\":\""
                    + voice + "\",\"pitch\":" + randomPitch(random) + "}";
        }
        int duration = random.chance(95) ? random.intValue(1, 4) : 0;
        return "{\"type\":\"change_duration\",\"targetNodeId\":\"" + target + "\",\"voice\":\""
                + voice + "\",\"duration\":" + duration + "}";
    }

    private static String randomPitch(Lcg random) {
        String step = random.pick(Arrays.asList("A", "B", "C", "D", "E", "F", "G"));
        StringBuilder pitch = new StringBuilder();
        pitch.append("{\"step\":\"").append(step).append("\",\"octave\":").append(random.intValue(2, 6));
        if (random.chance(50)) {
            pitch.append(",\"alter\":").append(random.pick(Arrays.asList(Integer.valueOf(-2), Integer.valueOf(-1),
                    Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2))).intValue());
        }
        pitch.append("}");
        return pitch.toString();
    }

    private static String loadFixture(String path) {
        InputStream stream = ScoreCorePropertyTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalArgumentException("Missing test fixture: " + path);
        }
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read test fixture: " + path, ex);
        }
    }

    private static final class Lcg {
        private long state;

        private Lcg(int seed) {
            this.state = ((long) seed) & 0xffffffffL;
        }

        private long next() {
            state = (1664525L * state + 1013904223L) & 0xffffffffL;
            return state;
        }

        private <T> T pick(List<T> values) {
            return values.get((int) (next() % values.size()));
        }

        private int intValue(int minimum, int maximum) {
            return minimum + (int) (next() % (maximum - minimum + 1));
        }

        private boolean chance(int percent) {
            return intValue(1, 100) <= percent;
        }
    }
}
