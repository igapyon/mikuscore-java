/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.midi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public class MidiIoTest {
    @Test
    public void clampsTempoAndVelocityLikeUpstreamHelpers() {
        assertEquals(120, MidiIo.clampTempo(Double.NaN));
        assertEquals(20, MidiIo.clampTempo(10));
        assertEquals(121, MidiIo.clampTempo(120.6));
        assertEquals(300, MidiIo.clampTempo(400));

        assertEquals(80, MidiIo.clampVelocity(Double.POSITIVE_INFINITY));
        assertEquals(1, MidiIo.clampVelocity(-2));
        assertEquals(65, MidiIo.clampVelocity(64.6));
        assertEquals(127, MidiIo.clampVelocity(200));
    }

    @Test
    public void mapsMidiProgramPresetNumbers() {
        assertEquals(5, MidiIo.instrumentByPreset("electric_piano_2"));
        assertEquals(1, MidiIo.instrumentByPreset("acoustic_grand_piano"));
        assertEquals(24, MidiIo.instrumentByPreset("acoustic_guitar_nylon"));
        assertEquals(62, MidiIo.instrumentByPreset("synth_brass_1"));
        assertEquals(5, MidiIo.instrumentByPreset("unknown"));
    }

    @Test
    public void normalizesLeadingPickupTimeSignaturePrelude() {
        List<MidiIo.MidiTickTimeSignatureEvent> events = Arrays.asList(
                new MidiIo.MidiTickTimeSignatureEvent(480, 4, 4),
                new MidiIo.MidiTickTimeSignatureEvent(0, 1, 4),
                new MidiIo.MidiTickTimeSignatureEvent(2400, 3, 4));

        MidiIo.LeadingPickupTimeSignatureNormalization result = MidiIo
                .normalizeLeadingPickupTimeSignatureEvents(events, 480);

        assertEquals(true, result.isNormalized());
        assertEquals(480, result.getPickupTicks());
        assertEquals(2, result.getEvents().size());
        assertEquals(0, result.getEvents().get(0).getTick());
        assertEquals(4, result.getEvents().get(0).getBeats());
        assertEquals(2400, result.getEvents().get(1).getTick());
    }

    @Test
    public void keepsNonPickupTimeSignatureEventsSortedOnly() {
        List<MidiIo.MidiTickTimeSignatureEvent> events = Arrays.asList(
                new MidiIo.MidiTickTimeSignatureEvent(960, 3, 4),
                new MidiIo.MidiTickTimeSignatureEvent(0, 2, 4));

        MidiIo.LeadingPickupTimeSignatureNormalization result = MidiIo
                .normalizeLeadingPickupTimeSignatureEvents(events, 480);

        assertEquals(false, result.isNormalized());
        assertEquals(0, result.getPickupTicks());
        assertEquals(0, result.getEvents().get(0).getTick());
        assertEquals(960, result.getEvents().get(1).getTick());
    }

    @Test
    public void buildsMuseScoreStylePickupTimeSignaturePrelude() {
        List<MidiIo.MidiTimeSignatureEvent> events = Arrays.asList(
                new MidiIo.MidiTimeSignatureEvent(0, 4, 4),
                new MidiIo.MidiTimeSignatureEvent(2400, 3, 4));

        List<MidiIo.MidiTimeSignatureEvent> remapped = MidiIo
                .buildMuseScoreStylePickupTimeSignaturePrelude(events, 480, 480);

        assertEquals(3, remapped.size());
        assertEquals(0, remapped.get(0).getStartTicks());
        assertEquals(1, remapped.get(0).getBeats());
        assertEquals(480, remapped.get(1).getStartTicks());
        assertEquals(4, remapped.get(1).getBeats());
        assertEquals(2400, remapped.get(2).getStartTicks());
    }

    @Test
    public void keepsPickupPreludeWhenItCannotMapToWholeBeats() {
        List<MidiIo.MidiTimeSignatureEvent> events = Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4));

        List<MidiIo.MidiTimeSignatureEvent> remapped = MidiIo
                .buildMuseScoreStylePickupTimeSignaturePrelude(events, 480, 240);

        assertSame(events, remapped);
    }

    @Test
    public void resolvesKeyPitchClasses() {
        assertEquals(0, MidiIo.mod12(-12));
        assertEquals(7, MidiIo.keyTonicPitchClassFromFifths(1, "major"));
        assertEquals(4, MidiIo.keyTonicPitchClassFromFifths(1, "minor"));
        assertEquals(Arrays.asList(Integer.valueOf(7), Integer.valueOf(9), Integer.valueOf(11), Integer.valueOf(0),
                Integer.valueOf(2), Integer.valueOf(4), Integer.valueOf(6)),
                Arrays.asList(MidiIo.keyScalePitchClasses(1, "major").toArray(new Integer[0])));
    }

    @Test
    public void infersKeySignatureFromImportedNotes() {
        List<MidiIo.ImportedQuantizedNote> gMajorNotes = Arrays.asList(
                new MidiIo.ImportedQuantizedNote(67, 0, 480),
                new MidiIo.ImportedQuantizedNote(69, 480, 960),
                new MidiIo.ImportedQuantizedNote(71, 960, 1440),
                new MidiIo.ImportedQuantizedNote(74, 1440, 1920),
                new MidiIo.ImportedQuantizedNote(67, 1920, 2400));

        MidiIo.MidiKeySignature inferred = MidiIo.inferKeySignatureFromImportedNotes(gMajorNotes);

        assertEquals(1, inferred.getFifths());
        assertEquals("major", inferred.getMode());
        assertEquals(null, MidiIo.inferKeySignatureFromImportedNotes(Arrays.asList(
                new MidiIo.ImportedQuantizedNote(60, 0, 120),
                new MidiIo.ImportedQuantizedNote(62, 120, 240))));
    }

    @Test
    public void mapsDrumNameHintsAndDynamicsVelocity() {
        assertEquals(Integer.valueOf(36), MidiIo.drumNameHintToGmNote("Kick Drum"));
        assertEquals(Integer.valueOf(42), MidiIo.drumNameHintToGmNote("closed hi-hat"));
        assertEquals(Integer.valueOf(81), MidiIo.drumNameHintToGmNote("Triangle"));
        assertEquals(null, MidiIo.drumNameHintToGmNote("Piano"));

        assertEquals(Integer.valueOf(20), MidiIo.dynamicsToVelocity("pppp"));
        assertEquals(Integer.valueOf(80), MidiIo.dynamicsToVelocity("mf"));
        assertEquals(Integer.valueOf(110), MidiIo.dynamicsToVelocity("sfz"));
        assertEquals(null, MidiIo.dynamicsToVelocity("unknown"));
    }

    @Test
    public void parsesStandardMidiMetaTextHelpers() {
        assertEquals(true, MidiIo.isGenericMidiTrackName(""));
        assertEquals(true, MidiIo.isGenericMidiTrackName("Track 12 ch 3"));
        assertEquals(false, MidiIo.isGenericMidiTrackName("Lead Piano"));

        assertEquals("Prelude", MidiIo.parseStandardTitleFromMetaText("Title: Prelude"));
        assertEquals("Allegro", MidiIo.parseStandardTitleFromMetaText("movement = Allegro"));
        assertEquals("", MidiIo.parseStandardTitleFromMetaText("subtitle: nope"));
        assertEquals("Toshiki", MidiIo.parseStandardComposerFromMetaText("Composer: Toshiki"));
        assertEquals("Iga", MidiIo.parseStandardComposerFromMetaText("comp=Iga"));
        assertEquals("", MidiIo.parseStandardComposerFromMetaText("arranger: someone"));
    }

    @Test
    public void buildsMetricAccentVelocityDeltas() {
        assertEquals("subtle", MidiIo.normalizeMetricAccentProfile(null));
        assertEquals("balanced", MidiIo.normalizeMetricAccentProfile("balanced"));
        assertEquals(Arrays.asList(Integer.valueOf(2), Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(0)),
                MidiIo.buildMetricAccentPattern(4, 4, "subtle"));
        assertEquals(Arrays.asList(Integer.valueOf(6), Integer.valueOf(0), Integer.valueOf(0),
                Integer.valueOf(3), Integer.valueOf(0), Integer.valueOf(0)),
                MidiIo.buildMetricAccentPattern(6, 8, "strong"));
        assertEquals(2, MidiIo.getMetricAccentVelocityDelta(0, 480, 4, 4, "subtle"));
        assertEquals(1, MidiIo.getMetricAccentVelocityDelta(960, 480, 4, 4, "subtle"));
        assertEquals(0, MidiIo.getMetricAccentVelocityDelta(Double.NaN, 480, 4, 4, "subtle"));
    }

    @Test
    public void splitsTicksAcrossPartsWithRemainderFirst() {
        assertEquals(Arrays.asList(Integer.valueOf(4), Integer.valueOf(3), Integer.valueOf(3)),
                MidiIo.splitTicks(10, 3));
        assertEquals(Arrays.asList(Integer.valueOf(10)), MidiIo.splitTicks(10, 0));
    }

    @Test
    public void splitsTicksWeightedWithStableRemainderDistribution() {
        assertEquals(Arrays.asList(Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(5)),
                MidiIo.splitTicksWeighted(10, Arrays.asList(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))));
        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(1), Integer.valueOf(1)),
                MidiIo.splitTicksWeighted(0, Arrays.asList(Double.valueOf(-1), Double.NaN, Double.valueOf(0))));
        assertEquals(Arrays.<Integer>asList(), MidiIo.splitTicksWeighted(10, Arrays.<Double>asList()));
    }

    @Test
    public void mapsPitchAndMidiUtilityValues() {
        assertEquals(Integer.valueOf(60), MidiIo.pitchToMidi("C", 0, 4));
        assertEquals(Integer.valueOf(61), MidiIo.pitchToMidi("C", 1, 4));
        assertEquals(null, MidiIo.pitchToMidi("H", 0, 4));
        assertEquals(1, MidiIo.keySignatureAlterByStep(2, "F"));
        assertEquals(-1, MidiIo.keySignatureAlterByStep(-2, "E"));
        assertEquals(0, MidiIo.keySignatureAlterByStep(0, "B"));
        assertEquals(Integer.valueOf(2), MidiIo.accidentalTextToAlter("double-sharp"));
        assertEquals(Integer.valueOf(-2), MidiIo.accidentalTextToAlter("flat-flat"));
        assertEquals(null, MidiIo.accidentalTextToAlter(""));
        assertEquals("C4", MidiIo.midiToPitchText(60));
        assertEquals("G9", MidiIo.midiToPitchText(200));
    }

    @Test
    public void normalizesMidiImportNumbersAndQuantizeGrid() {
        assertEquals(480, MidiIo.normalizeTicksPerQuarter(Double.NaN));
        assertEquals(1, MidiIo.normalizeTicksPerQuarter(0));
        assertEquals(961, MidiIo.normalizeTicksPerQuarter(960.6));
        assertEquals(Integer.valueOf(1), MidiIo.normalizeMidiProgramNumber(1.2));
        assertEquals(Integer.valueOf(128), MidiIo.normalizeMidiProgramNumber(128));
        assertEquals(null, MidiIo.normalizeMidiProgramNumber(0));
        assertEquals("auto", MidiIo.normalizeMidiImportQuantizeGridOption("auto"));
        assertEquals("1/16", MidiIo.normalizeMidiImportQuantizeGridOption("1/16"));
        assertEquals("1/64", MidiIo.normalizeMidiImportQuantizeGridOption("bad"));
        assertEquals(2, MidiIo.quantizeGridToDivisions("1/8"));
        assertEquals(4, MidiIo.quantizeGridToDivisions("1/16"));
        assertEquals(8, MidiIo.quantizeGridToDivisions("1/32"));
        assertEquals(16, MidiIo.quantizeGridToDivisions("1/64"));
    }

    @Test
    public void detectsTripletLikeTiming() {
        assertEquals(6, MidiIo.gcdInt(54, 24));
        assertEquals(24, MidiIo.gcdInt(0, 24));
        assertEquals(true, MidiIo.isNearMultiple(321, 160, 2));
        assertEquals(false, MidiIo.isNearMultiple(330, 160, 2));
        List<MidiIo.SmfImportedNote> triplets = Arrays.asList(
                new MidiIo.SmfImportedNote(0, 160),
                new MidiIo.SmfImportedNote(160, 320),
                new MidiIo.SmfImportedNote(320, 480));
        assertEquals(true, MidiIo.hasTripletLikeTiming(triplets, 480));
        assertEquals(false, MidiIo.hasTripletLikeTiming(Arrays.asList(new MidiIo.SmfImportedNote(0, 120)), 480));
    }

    @Test
    public void resolvesAndScoresImportQuantizeGrid() {
        List<MidiIo.SmfImportedNote> triplets = Arrays.asList(
                new MidiIo.SmfImportedNote(0, 160),
                new MidiIo.SmfImportedNote(160, 320),
                new MidiIo.SmfImportedNote(320, 480));

        MidiIo.ImportQuantizeResolution tripletAware = MidiIo.resolveImportQuantizeTick(triplets, 480, "1/16",
                true);
        assertEquals(40, tripletAware.getQTick());
        assertEquals(12, tripletAware.getDivisions());

        MidiIo.ImportQuantizeResolution straight = MidiIo.resolveImportQuantizeTick(triplets, 480, "1/16", false);
        assertEquals(120, straight.getQTick());
        assertEquals(4, straight.getDivisions());

        assertEquals(2, MidiIo.scoreImportQuantization(Arrays.asList(new MidiIo.SmfImportedNote(0, 121)), 120));
        assertEquals("1/16", MidiIo.chooseBestImportQuantizeGrid(
                Arrays.asList(new MidiIo.SmfImportedNote(0, 120), new MidiIo.SmfImportedNote(120, 240)), 480,
                false));
    }

    @Test
    public void readsSmfByteValuesAndMetaText() {
        assertEquals("MThd", MidiIo.readAscii(new byte[] { 'M', 'T', 'h', 'd' }, 0, 4));
        assertEquals("", MidiIo.readAscii(new byte[] { 'M', 'T' }, 0, 4));
        assertEquals(Long.valueOf(256), MidiIo.readUint32Be(new byte[] { 0, 0, 1, 0 }, 0));
        assertEquals(Integer.valueOf(256), MidiIo.readUint16Be(new byte[] { 1, 0 }, 0));

        MidiIo.VariableLengthValue variableLength = MidiIo.readVariableLengthAt(new byte[] { (byte) 0x81, 0 }, 0);
        assertEquals(128, variableLength.getValue());
        assertEquals(2, variableLength.getNext());
        assertEquals(null, MidiIo.readVariableLengthAt(new byte[] { (byte) 0x81 }, 0));

        assertEquals("AB", MidiIo.asciiBytesToString(new byte[] { 'A', 'B' }));
        assertEquals("é", MidiIo.decodeMetaTextBytes("é".getBytes(StandardCharsets.UTF_8)));
        assertEquals("A B+plus", MidiIo.safeDecodeURIComponent("A%20B+plus"));
        assertEquals("%ZZ", MidiIo.safeDecodeURIComponent("%ZZ"));
    }

    @Test
    public void buildsSmfVariableLengthAndMetaEventBytes() {
        assertArrayEquals(new byte[] { 0 }, MidiIo.numberToVariableLength(-1));
        assertArrayEquals(new byte[] { 0 }, MidiIo.numberToVariableLength(0));
        assertArrayEquals(new byte[] { 0x7f }, MidiIo.numberToVariableLength(127));
        assertArrayEquals(new byte[] { (byte) 0x81, 0 }, MidiIo.numberToVariableLength(128));
        assertArrayEquals(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x7f },
                MidiIo.numberToVariableLength(0x0fffffff));
        assertArrayEquals(new byte[] { (byte) 0x81, 0x70, (byte) 0xf0, 0x04, 'A', 'B', 0x29, (byte) 0xf7 },
                MidiIo.buildMksSysexEventData(240, "AB\u00a9"));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x03, 0x02, 'A', (byte) 0xe9 },
                MidiIo.buildTextMetaEventData(0, "A\u00e9", 0x03));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x01, 0 }, MidiIo.buildTextMetaEventData(0, null));
    }

    @Test
    public void buildsMksSysexChunkTexts() {
        assertEquals("811C9DC5", MidiIo.fnv1a32Hex(""));
        assertEquals("4F9F2CAB", MidiIo.fnv1a32Hex("hello"));
        assertEquals(Arrays.asList("ab", "cd", "e"), MidiIo.chunkString("abcde", 2));
        assertEquals(Arrays.asList(""), MidiIo.chunkString("", 2));

        List<String> chunks = MidiIo.buildMksSysexChunkTexts(new MidiIo.MksSysexChunkTextParams(480, 3, 2, 1,
                1, 1, 0, 2, Arrays.asList("", " warn & note ")));

        assertEquals(2, chunks.size());
        assertEquals(true, chunks.get(0).startsWith("mks|v=1|m=0001|i=0001|n=0002|d="));
        assertEquals(true, chunks.get(1).startsWith("mks|v=1|m=0001|i=0002|n=0002|d="));
        StringBuilder encoded = new StringBuilder();
        for (String chunk : chunks) {
            encoded.append(chunk.substring(chunk.indexOf("|d=") + 3));
        }
        String decoded = MidiIo.safeDecodeURIComponent(encoded.toString());
        assertEquals(true, decoded.contains("schema=mks-sysex-v1\nnamespace=mks\napp=mikuscore"));
        assertEquals(true, decoded.contains("tpq=480\ntrack-count=2\nevent-count=3"));
        assertEquals(true, decoded.contains("diag-count=1\ndiag-0001= warn & note "));
        assertEquals(true, decoded.contains("fingerprint-fnv1a32="));
    }

    @Test
    public void buildsTempoTimeAndKeySignatureMetaEventBytes() {
        assertArrayEquals(new byte[] { (byte) 0x81, 0x70, (byte) 0xff, 0x51, 0x03, 0x07, (byte) 0xa1, 0x20 },
                MidiIo.buildTempoMetaEventData(240, 120));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x51, 0x03, 0x2d, (byte) 0xc6, (byte) 0xc0 },
                MidiIo.buildTempoMetaEventData(0, 1));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x58, 0x04, 0x06, 0x03, 24, 8 },
                MidiIo.buildTimeSignatureMetaEventData(0, 6, 8));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x58, 0x04, (byte) 0xff, 0x00, 24, 8 },
                MidiIo.buildTimeSignatureMetaEventData(0, 999, 1));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x59, 0x02, (byte) 0xfe, 0x01 },
                MidiIo.buildKeySignatureMetaEventData(0, -2, "minor"));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x59, 0x02, 0x07, 0x00 },
                MidiIo.buildKeySignatureMetaEventData(0, 99, "major"));
    }

    @Test
    public void buildsMidiExportMetaTimelineEventData() {
        List<byte[]> data = MidiIo.buildMidiExportMetaTimelineEventData(
                Arrays.asList(new MidiIo.MidiTempoEvent(0, 100), new MidiIo.MidiTempoEvent(240, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, -1, "minor")));

        assertEquals(4, data.size());
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x58, 0x04, 0x04, 0x02, 24, 8 },
                data.get(0));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x59, 0x02, (byte) 0xff, 0x01 },
                data.get(1));
        assertArrayEquals(new byte[] { 0, (byte) 0xff, 0x51, 0x03, 0x09, 0x27, (byte) 0xc0 },
                data.get(2));
        assertArrayEquals(new byte[] { (byte) 0x81, 0x70, (byte) 0xff, 0x51, 0x03, 0x07, (byte) 0xa1, 0x20 },
                data.get(3));
    }

    @Test
    public void buildsUnsignedBigEndianAndRawTrackChunkBytes() {
        assertArrayEquals(new byte[] { 0, 0 }, MidiIo.toU16BeBytes(-1));
        assertArrayEquals(new byte[] { (byte) 0xff, (byte) 0xff }, MidiIo.toU16BeBytes(70000));
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, MidiIo.toU32BeBytes(-1));
        assertArrayEquals(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff },
                MidiIo.toU32BeBytes(4294967296.0d));
        assertEquals(1, MidiIo.toMidiWriterVelocityByte(0));
        assertEquals(64, MidiIo.toMidiWriterVelocityByte(50));
        assertEquals(127, MidiIo.toMidiWriterVelocityByte(999));

        assertArrayEquals(new byte[] { 'M', 'T', 'r', 'k', 0, 0, 0, 12, 0, 1, 0, 2, 0x78, 3, 0x78, 4, 0,
                (byte) 0xff, 0x2f, 0 },
                MidiIo.encodeRawTrackChunk(Arrays.asList(new MidiIo.RawTrackEvent(240, 2, new byte[] { 4 }),
                        new MidiIo.RawTrackEvent(0, 1, 2, new byte[] { 2 }),
                        new MidiIo.RawTrackEvent(120, 1, new byte[] { 3 }),
                        new MidiIo.RawTrackEvent(0, 1, 1, new byte[] { 1 }))));
    }

    @Test
    public void buildsRawMidiTempoTrackChunk() {
        assertArrayEquals(new byte[] { 'M', 'T', 'r', 'k', 0, 0, 0, 46,
                0, (byte) 0xff, 0x03, 0x04, 'M', 'e', 't', 'a',
                0, (byte) 0xff, 0x58, 0x04, 0x03, 0x02, 24, 8,
                0, (byte) 0xff, 0x59, 0x02, (byte) 0xff, 0x01,
                0, (byte) 0xf0, 0x02, 'A', (byte) 0xf7,
                0, (byte) 0xff, 0x01, 0x04, 'n', 'o', 't', 'e',
                0x78, (byte) 0xff, 0x51, 0x03, 0x07, (byte) 0xa1, 0x20,
                0, (byte) 0xff, 0x2f, 0 },
                MidiIo.buildRawMidiTempoTrackChunk(
                        Arrays.asList(new MidiIo.MidiTempoEvent(120, 120)),
                        Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 3, 4)),
                        Arrays.asList(new MidiIo.MidiTickKeySignatureEvent(0, -1, "minor")),
                        new MidiIo.RawMidiTempoTrackOptions(true, Arrays.asList("A"),
                                Arrays.asList("note"), "Meta")));
    }

    @Test
    public void buildsRawMidiNoteTrackChunks() {
        Map<String, Integer> overrides = new LinkedHashMap<String, Integer>();
        overrides.put("p1", Integer.valueOf(10));

        List<byte[]> chunks = MidiIo.buildRawMidiNoteTrackChunks(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 80, "p1", "Piano")),
                overrides, "electric_piano_2", "off_before_on");

        assertEquals(1, chunks.size());
        assertArrayEquals(new byte[] { 'M', 'T', 'r', 'k', 0, 0, 0, 24,
                0, (byte) 0xff, 0x03, 0x05, 'P', 'i', 'a', 'n', 'o',
                0, (byte) 0xc0, 10,
                0, (byte) 0x90, 60, 102,
                0x78, (byte) 0x80, 60, 102,
                0, (byte) 0xff, 0x2f, 0 },
                chunks.get(0));

        List<byte[]> drumChunks = MidiIo.buildRawMidiNoteTrackChunks(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(38, 0, 1, 10, 100, "", "")),
                null, "electric_piano_2", "bad");
        assertArrayEquals(new byte[] { 'M', 'T', 'r', 'k', 0, 0, 0, 27,
                0, (byte) 0xff, 0x03, 0x0b, '_', '_', 'd', 'e', 'f', 'a', 'u', 'l', 't', '_', '_',
                0, (byte) 0x99, 38, 127,
                1, (byte) 0x89, 38, 127,
                0, (byte) 0xff, 0x2f, 0 },
                drumChunks.get(0));
    }

    @Test
    public void buildsRawMidiControlTrackChunksAndFileHeader() {
        List<byte[]> chunks = MidiIo.buildRawMidiControlTrackChunks(Arrays.asList(
                new MidiIo.RawMidiControlEvent("p1", "Piano", 120, 1, 64, 0),
                new MidiIo.RawMidiControlEvent("p1", "Piano", 0, 1, 64, 127)));

        assertEquals(1, chunks.size());
        assertArrayEquals(new byte[] { 'M', 'T', 'r', 'k', 0, 0, 0, 27,
                0, (byte) 0xff, 0x03, 0x0b, 'P', 'i', 'a', 'n', 'o', ' ', 'P', 'e', 'd', 'a', 'l',
                0, (byte) 0xb0, 64, 127,
                0x78, (byte) 0xb0, 64, 0,
                0, (byte) 0xff, 0x2f, 0 },
                chunks.get(0));

        assertArrayEquals(new byte[] { 'M', 'T', 'h', 'd', 0, 0, 0, 6, 0, 1, 0, 1, 1, (byte) 0xe0,
                'M', 'T', 'r', 'k', 0, 0, 0, 27,
                0, (byte) 0xff, 0x03, 0x0b, 'P', 'i', 'a', 'n', 'o', ' ', 'P', 'e', 'd', 'a', 'l',
                0, (byte) 0xb0, 64, 127,
                0x78, (byte) 0xb0, 64, 0,
                0, (byte) 0xff, 0x2f, 0 },
                MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480));
    }

    @Test
    public void normalizesPlaybackEventsForParity() {
        List<MidiIo.RawMidiPlaybackEvent> normalized = MidiIo.normalizePlaybackEventsForParity(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, -10, 0, 99, 40, "p1", "First"),
                new MidiIo.RawMidiPlaybackEvent(60, 0, 1, 16, 80, "p2", "Second"),
                new MidiIo.RawMidiPlaybackEvent(61, 0, 1, 16, 70, "p3", "Third")));

        assertEquals(2, normalized.size());
        assertEquals(16, normalized.get(0).getChannel());
        assertEquals(0, normalized.get(0).getStartTicks());
        assertEquals(1, normalized.get(0).getDurTicks());
        assertEquals(60, normalized.get(0).getMidiNumber());
        assertEquals(80, normalized.get(0).getVelocity());
        assertEquals("p1", normalized.get(0).getTrackId());
        assertEquals("First", normalized.get(0).getTrackName());
        assertEquals(61, normalized.get(1).getMidiNumber());
    }

    @Test
    public void compactsDensePlaybackSchedules() {
        List<MidiIo.RawMidiPlaybackEvent> events = new ArrayList<MidiIo.RawMidiPlaybackEvent>();
        for (int onset = 0; onset < 120; onset++) {
            for (int pitch = 0; pitch < 60; pitch++) {
                events.add(new MidiIo.RawMidiPlaybackEvent(30 + (pitch % 60), onset * 10,
                        pitch < 10 ? 1 : 64 + (pitch % 3), 1, 80, "t" + pitch, ""));
            }
        }

        MidiIo.PlaybackScheduleCompactionResult compacted =
                MidiIo.compactPlaybackScheduleForDensePlayback(events, 128);
        MidiIo.PlaybackScheduleCompactionSummary summary = compacted.getSummary();

        assertTrue(summary.isApplied());
        assertEquals(7200, summary.getOriginalEventCount());
        assertTrue(summary.getFinalEventCount() <= 4096);
        assertEquals(1200, summary.getDroppedUltraShortCount());
        assertEquals(240, summary.getDroppedDenseOnsetCount());
        assertTrue(summary.getDroppedBudgetCount() > 0);

        Map<Integer, Integer> countsByOnset = new LinkedHashMap<Integer, Integer>();
        for (MidiIo.RawMidiPlaybackEvent event : compacted.getEvents()) {
            Integer start = Integer.valueOf(event.getStartTicks());
            Integer count = countsByOnset.get(start);
            countsByOnset.put(start, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            assertTrue(event.getDurTicks() >= 2);
        }
        int maxOnsetCount = 0;
        for (Integer count : countsByOnset.values()) {
            maxOnsetCount = Math.max(maxOnsetCount, count.intValue());
        }
        assertTrue(maxOnsetCount <= 48);
    }

    @Test
    public void keepsProtectedSmallPlaybackOnsetsDuringCompaction() {
        List<MidiIo.RawMidiPlaybackEvent> events = new ArrayList<MidiIo.RawMidiPlaybackEvent>();
        for (int onset = 0; onset < 90; onset++) {
            for (int pitch = 0; pitch < 48; pitch++) {
                events.add(new MidiIo.RawMidiPlaybackEvent(20 + pitch, onset * 12, 48, 1, 80,
                        "dense-" + onset + "-" + pitch, ""));
            }
        }
        int[] smallChordStarts = new int[] { 9999, 10011, 10023 };
        for (int i = 0; i < smallChordStarts.length; i++) {
            events.add(new MidiIo.RawMidiPlaybackEvent(72 + i, smallChordStarts[i], 48, 1, 80,
                    "small-" + i, ""));
        }

        MidiIo.PlaybackScheduleCompactionResult compacted =
                MidiIo.compactPlaybackScheduleForDensePlayback(events, 128);

        assertTrue(compacted.getSummary().isApplied());
        for (int start : smallChordStarts) {
            int count = 0;
            for (MidiIo.RawMidiPlaybackEvent event : compacted.getEvents()) {
                if (event.getStartTicks() == start) {
                    count++;
                }
            }
            assertEquals(1, count);
        }
    }

    @Test
    public void prioritizesOuterVoicesAndUniquePitchesInDensePlaybackOnsets() {
        List<MidiIo.RawMidiPlaybackEvent> events = new ArrayList<MidiIo.RawMidiPlaybackEvent>();
        for (int octave = 0; octave < 10; octave++) {
            events.add(new MidiIo.RawMidiPlaybackEvent(24 + octave * 12, 0, 48, 1, 80,
                    "oct-" + octave, ""));
        }
        for (int i = 0; i < 40; i++) {
            events.add(new MidiIo.RawMidiPlaybackEvent(61 + i, 0, 48, 1, 80, "uniq-" + i, ""));
        }
        for (int i = 0; i < 2100; i++) {
            events.add(new MidiIo.RawMidiPlaybackEvent(30 + (i % 24), 100 + i * 2, 48, 1, 80,
                    "fill-" + i, ""));
        }

        MidiIo.PlaybackScheduleCompactionResult compacted =
                MidiIo.compactPlaybackScheduleForDensePlayback(events, 128);
        List<Integer> keptMidis = new ArrayList<Integer>();
        for (MidiIo.RawMidiPlaybackEvent event : compacted.getEvents()) {
            if (event.getStartTicks() == 0) {
                keptMidis.add(Integer.valueOf(event.getMidiNumber()));
            }
        }

        assertEquals(48, keptMidis.size());
        assertTrue(keptMidis.contains(Integer.valueOf(24)));
        assertTrue(keptMidis.contains(Integer.valueOf(132)));
        assertTrue(keptMidis.contains(Integer.valueOf(61)));
        assertTrue(keptMidis.contains(Integer.valueOf(100)));
        assertFalse(keptMidis.contains(Integer.valueOf(36)));
        assertFalse(keptMidis.contains(Integer.valueOf(48)));
    }

    @Test
    public void buildsDrumPartMapByPartId() {
        assertEquals(Integer.valueOf(42), MidiIo.parseMidiNoteNumber(" 42.5 "));
        assertEquals(Integer.valueOf(0), MidiIo.parseMidiNoteNumber("0x10"));
        assertNull(MidiIo.parseMidiNoteNumber("128"));
        assertNull(MidiIo.parseMidiNoteNumber("abc"));
        assertEquals(Integer.valueOf(38), MidiIo.resolveDrumMidiFromInstrumentName(" Acoustic Snare "));
        assertNull(MidiIo.resolveDrumMidiFromInstrumentName("   "));

        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part-list>"
                        + "<score-part id=\"P1\">"
                        + "<part-name>Drums</part-name>"
                        + "<score-instrument id=\"P1-I1\"><instrument-name>Acoustic Snare</instrument-name></score-instrument>"
                        + "<score-instrument id=\"P1-I2\"><instrument-name>Open Hi-Hat</instrument-name></score-instrument>"
                        + "<score-instrument><instrument-name>Ignored</instrument-name></score-instrument>"
                        + "<midi-instrument id=\"P1-I1\"><midi-unpitched>38</midi-unpitched></midi-instrument>"
                        + "<midi-instrument id=\"P1-I2\"><midi-unpitched>999</midi-unpitched></midi-instrument>"
                        + "<midi-instrument><midi-unpitched>42</midi-unpitched></midi-instrument>"
                        + "</score-part>"
                        + "<score-part id=\"P2\"><part-name>Empty</part-name>"
                        + "<score-instrument id=\"P2-I1\"><instrument-name>Triangle</instrument-name></score-instrument>"
                        + "</score-part>"
                        + "<score-part><part-name>No Id</part-name></score-part>"
                        + "</part-list><part id=\"P1\"/></score-partwise>");

        Map<String, MidiIo.DrumPartMap> byPartId = MidiIo.buildDrumPartMapByPartId(doc);

        assertEquals(2, byPartId.size());
        MidiIo.DrumPartMap p1 = byPartId.get("P1");
        assertEquals("Acoustic Snare", p1.getInstrumentNameById().get("P1-I1"));
        assertEquals("Open Hi-Hat", p1.getInstrumentNameById().get("P1-I2"));
        assertEquals(Integer.valueOf(38), p1.getMidiUnpitchedByInstrumentId().get("P1-I1"));
        assertNull(p1.getMidiUnpitchedByInstrumentId().get("P1-I2"));
        assertEquals(Integer.valueOf(38), p1.getDefaultMidiUnpitched());

        MidiIo.DrumPartMap p2 = byPartId.get("P2");
        assertEquals("Triangle", p2.getInstrumentNameById().get("P2-I1"));
        assertEquals(0, p2.getMidiUnpitchedByInstrumentId().size());
        assertNull(p2.getDefaultMidiUnpitched());
    }

    @Test
    public void collectsMidiProgramOverridesFromMusicXmlDoc() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part-list>"
                        + "<score-part id=\"P1\"><part-name>Piano</part-name>"
                        + "<midi-instrument id=\"P1-I1\"><midi-program></midi-program></midi-instrument>"
                        + "<midi-instrument id=\"P1-I2\"><midi-program>4.8</midi-program></midi-instrument>"
                        + "<midi-instrument id=\"P1-I3\"><midi-program>7</midi-program></midi-instrument>"
                        + "</score-part>"
                        + "<score-part id=\"P2\"><part-name>Invalid</part-name>"
                        + "<midi-instrument id=\"P2-I1\"><midi-program>0</midi-program></midi-instrument>"
                        + "<midi-instrument id=\"P2-I2\"><midi-program>128</midi-program></midi-instrument>"
                        + "</score-part>"
                        + "<score-part id=\"P3\"><part-name>Hexish</part-name>"
                        + "<midi-instrument id=\"P3-I1\"><midi-program>0x10</midi-program></midi-instrument>"
                        + "</score-part>"
                        + "<score-part><midi-instrument><midi-program>5</midi-program></midi-instrument></score-part>"
                        + "</part-list><part id=\"P1\"/></score-partwise>");

        Map<String, Integer> overrides = MidiIo.collectMidiProgramOverridesFromMusicXmlDoc(doc);

        assertEquals(2, overrides.size());
        assertEquals(Integer.valueOf(4), overrides.get("P1"));
        assertEquals(Integer.valueOf(128), overrides.get("P2"));
        assertNull(overrides.get("P3"));
    }

    @Test
    public void resolvesMeasureAdvanceDiv() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\">"
                        + "<measure number=\"0\" implicit=\"yes\"/>"
                        + "<measure number=\"1\"/>"
                        + "<measure number=\"2\" implicit=\"true\"/>"
                        + "</part></score-partwise>");
        Element implicitPickup = (Element) doc.getElementsByTagName("measure").item(0);
        Element firstRegular = (Element) doc.getElementsByTagName("measure").item(1);
        Element nextImplicit = (Element) doc.getElementsByTagName("measure").item(2);

        assertEquals(8, MidiIo.measureCapacityDivFromContext(2, 4, 4));
        assertEquals(4, MidiIo.measureCapacityDivFromContext(0, 0, 0));
        assertEquals(3, MidiIo.resolveMeasureAdvanceDiv(implicitPickup, 3, 2, 4, 4));
        assertEquals(8, MidiIo.resolveMeasureAdvanceDiv(implicitPickup, 0, 2, 4, 4));
        assertEquals(8, MidiIo.resolveMeasureAdvanceDiv(firstRegular, 3, 2, 4, 4));
        assertEquals(3, MidiIo.resolveMeasureAdvanceDiv(firstRegular, 3, 2, 4, 4, true, false));
        assertEquals(3, MidiIo.resolveMeasureAdvanceDiv(implicitPickup, 3, 2, 4, 4, false, true));
        assertEquals(10, MidiIo.resolveMeasureAdvanceDiv(nextImplicit, 10, 2, 4, 4));
    }

    @Test
    public void estimatesMeasureContentSpanDiv() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\"><measure number=\"1\">"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice></note>"
                        + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>6</duration><voice>1</voice></note>"
                        + "<backup><duration>4</duration></backup>"
                        + "<note><pitch><step>G</step><octave>3</octave></pitch><duration>2</duration><voice>2</voice></note>"
                        + "<forward><duration>3</duration></forward>"
                        + "<note><rest/><duration>0</duration><voice>1</voice></note>"
                        + "</measure></part></score-partwise>");
        Element measure = (Element) doc.getElementsByTagName("measure").item(0);

        assertEquals(6, MidiIo.estimateMeasureContentSpanDiv(measure));
    }

    @Test
    public void detectsFirstUnderfullPickupAndImplicitMeasure() {
        Document pickupDoc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise>"
                        + "<part id=\"P1\"><measure number=\"0\"><attributes><divisions>2</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes><note><duration>2</duration></note></measure></part>"
                        + "<part id=\"P2\"><measure number=\"0\"><attributes><divisions>2</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes><note><duration>2</duration></note></measure></part>"
                        + "</score-partwise>");
        Document singlePartDoc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\"><measure number=\"1\"><note><duration>2</duration></note></measure></part></score-partwise>");
        Document fullDoc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise>"
                        + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes><note><duration>4</duration></note></measure></part>"
                        + "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes><note><duration>2</duration></note></measure></part>"
                        + "</score-partwise>");

        assertEquals(true, MidiIo.shouldTreatFirstUnderfullAsPickup(pickupDoc));
        assertEquals(false, MidiIo.shouldTreatFirstUnderfullAsPickup(singlePartDoc));
        assertEquals(false, MidiIo.shouldTreatFirstUnderfullAsPickup(fullDoc));

        Element implicit = (Element) MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\"><measure implicit=\"1\"/></part></score-partwise>")
                .getElementsByTagName("measure").item(0);
        Element regular = (Element) singlePartDoc.getElementsByTagName("measure").item(0);
        assertEquals(true, MidiIo.isImplicitMeasure(implicit));
        assertEquals(false, MidiIo.isImplicitMeasure(regular));
        assertEquals(false, MidiIo.isImplicitMeasure(null));
    }

    @Test
    public void collectsMidiControlEventsFromMusicXmlDoc() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part-list>"
                        + "<score-part id=\"P1\"><part-name>Piano</part-name><midi-instrument><midi-channel>3</midi-channel></midi-instrument></score-part>"
                        + "<score-part id=\"P2\"><part-name>Strings</part-name></score-part>"
                        + "</part-list>"
                        + "<part id=\"P1\">"
                        + "<measure number=\"1\"><attributes><divisions>2</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<direction><direction-type><pedal type=\"start\"/></direction-type></direction>"
                        + "<forward><duration>2</duration></forward>"
                        + "<direction><direction-type><pedal type=\"continue\"/></direction-type></direction>"
                        + "</measure>"
                        + "<measure number=\"2\" implicit=\"yes\"><direction><direction-type><pedal type=\"change\"/></direction-type></direction></measure>"
                        + "<measure number=\"3\"><direction><direction-type><pedal type=\"stop\"/></direction-type></direction></measure>"
                        + "</part>"
                        + "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>2</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<direction><direction-type><pedal type=\"resume\"/></direction-type></direction>"
                        + "</measure></part>"
                        + "</score-partwise>");

        List<MidiIo.RawMidiControlEvent> events = MidiIo.collectMidiControlEventsFromMusicXmlDoc(doc, 480);

        assertEquals(5, events.size());
        assertEquals("P1", events.get(0).getTrackId());
        assertEquals("Piano", events.get(0).getTrackName());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(3, events.get(0).getChannel());
        assertEquals(64, events.get(0).getControllerNumber());
        assertEquals(127, events.get(0).getControllerValue());
        assertEquals(480, events.get(1).getStartTicks());
        assertEquals(0, events.get(1).getControllerValue());
        assertEquals(480, events.get(2).getStartTicks());
        assertEquals(127, events.get(2).getControllerValue());
        assertEquals(2400, events.get(3).getStartTicks());
        assertEquals(0, events.get(3).getControllerValue());
        assertEquals("P2", events.get(4).getTrackId());
        assertEquals("Strings", events.get(4).getTrackName());
        assertEquals(2, events.get(4).getChannel());
        assertEquals(127, events.get(4).getControllerValue());
    }

    @Test
    public void collectsMidiTempoEventsFromMusicXmlDoc() {
        Document emptyDoc = MusicXmlIo.parseMusicXmlDocument("<score-partwise/>");
        List<MidiIo.MidiTempoEvent> emptyEvents = MidiIo.collectMidiTempoEventsFromMusicXmlDoc(emptyDoc, 480);
        assertEquals(1, emptyEvents.size());
        assertEquals(0, emptyEvents.get(0).getTick());
        assertEquals(120, emptyEvents.get(0).getBpm());

        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><sound tempo=\"90\"/>"
                        + "<part id=\"P1\">"
                        + "<measure number=\"1\"><attributes><divisions>2</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<direction><offset>1</offset><sound tempo=\"100\"/></direction>"
                        + "<note><duration>2</duration><voice>1</voice></note>"
                        + "<direction><direction-type><metronome><per-minute>140</per-minute></metronome></direction-type></direction>"
                        + "<sound tempo=\"160\"/>"
                        + "</measure>"
                        + "<measure number=\"2\"><sound tempo=\"160\"/><direction><sound tempo=\"200\"/></direction></measure>"
                        + "</part></score-partwise>");

        List<MidiIo.MidiTempoEvent> events = MidiIo.collectMidiTempoEventsFromMusicXmlDoc(doc, 480);

        assertEquals(4, events.size());
        assertEquals(0, events.get(0).getTick());
        assertEquals(120, events.get(0).getBpm());
        assertEquals(240, events.get(1).getTick());
        assertEquals(100, events.get(1).getBpm());
        assertEquals(480, events.get(2).getTick());
        assertEquals(160, events.get(2).getBpm());
        assertEquals(1920, events.get(3).getTick());
        assertEquals(200, events.get(3).getBpm());
    }

    @Test
    public void collectsLeadingPickupTicksFromMusicXmlDoc() {
        Document noPartDoc = MusicXmlIo.parseMusicXmlDocument("<score-partwise/>");
        assertEquals(0, MidiIo.collectLeadingPickupTicksFromMusicXmlDoc(noPartDoc, 480));

        Document pickupDoc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\">"
                        + "<measure number=\"0\"><attributes><divisions>2</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<note><duration>2</duration><voice>1</voice></note></measure>"
                        + "<measure number=\"1\" implicit=\"yes\"><note><duration>8</duration></note></measure>"
                        + "</part></score-partwise>");
        assertEquals(480, MidiIo.collectLeadingPickupTicksFromMusicXmlDoc(pickupDoc, 480));

        Document fullDoc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\">"
                        + "<measure number=\"1\"><attributes><divisions>2</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<note><duration>8</duration></note></measure>"
                        + "</part></score-partwise>");
        assertEquals(0, MidiIo.collectLeadingPickupTicksFromMusicXmlDoc(fullDoc, 480));
    }

    @Test
    public void collectsMidiTimeSignatureEventsFromMusicXmlDoc() {
        Document emptyDoc = MusicXmlIo.parseMusicXmlDocument("<score-partwise/>");
        List<MidiIo.MidiTimeSignatureEvent> emptyEvents = MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(
                emptyDoc, 480);
        assertEquals(1, emptyEvents.size());
        assertEquals(0, emptyEvents.get(0).getStartTicks());
        assertEquals(4, emptyEvents.get(0).getBeats());
        assertEquals(4, emptyEvents.get(0).getBeatType());

        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\">"
                        + "<measure number=\"1\"><attributes><divisions>2</divisions><time><beats>3</beats><beat-type>4</beat-type></time></attributes><note><duration>6</duration></note></measure>"
                        + "<measure number=\"2\"><attributes><time><beats>4</beats><beat-type>4</beat-type></time></attributes><note><duration>8</duration></note></measure>"
                        + "<measure number=\"3\"><attributes><time><beats>5</beats><beat-type>8</beat-type></time></attributes><note><duration>5</duration></note></measure>"
                        + "</part></score-partwise>");

        List<MidiIo.MidiTimeSignatureEvent> events = MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(doc,
                480);

        assertEquals(3, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(3, events.get(0).getBeats());
        assertEquals(4, events.get(0).getBeatType());
        assertEquals(1440, events.get(1).getStartTicks());
        assertEquals(4, events.get(1).getBeats());
        assertEquals(4, events.get(1).getBeatType());
        assertEquals(3360, events.get(2).getStartTicks());
        assertEquals(5, events.get(2).getBeats());
        assertEquals(8, events.get(2).getBeatType());
    }

    @Test
    public void collectsMidiTimeSignatureEventsForFf58ExportLikeUpstreamRegression() {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>3</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1440</duration>"
                + "<voice>1</voice><type>half</type><dot/></note></measure>"
                + "<measure number=\"2\"><attributes><time><beats>6</beats><beat-type>8</beat-type></time>"
                + "</attributes><note><pitch><step>D</step><octave>4</octave></pitch><duration>1440</duration>"
                + "<voice>1</voice><type>half</type><dot/></note></measure>"
                + "</part></score-partwise>");

        List<MidiIo.MidiTimeSignatureEvent> events = MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(doc, 128);

        assertEquals(2, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(3, events.get(0).getBeats());
        assertEquals(4, events.get(0).getBeatType());
        assertEquals(384, events.get(1).getStartTicks());
        assertEquals(6, events.get(1).getBeats());
        assertEquals(8, events.get(1).getBeatType());
    }

    @Test
    public void collectsMidiKeySignatureEventsFromMusicXmlDoc() {
        Document emptyDoc = MusicXmlIo.parseMusicXmlDocument("<score-partwise/>");
        List<MidiIo.MidiKeySignatureEvent> emptyEvents = MidiIo.collectMidiKeySignatureEventsFromMusicXmlDoc(emptyDoc,
                480);
        assertEquals(1, emptyEvents.size());
        assertEquals(0, emptyEvents.get(0).getStartTicks());
        assertEquals(0, emptyEvents.get(0).getFifths());
        assertEquals("major", emptyEvents.get(0).getMode());

        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><part id=\"P1\">"
                        + "<measure number=\"1\"><attributes><divisions>2</divisions><key><fifths>2</fifths><mode>major</mode></key><time><beats>3</beats><beat-type>4</beat-type></time></attributes><note><duration>6</duration></note></measure>"
                        + "<measure number=\"2\"><attributes><key><fifths>-3</fifths><mode>minor</mode></key></attributes><note><duration>8</duration></note></measure>"
                        + "<measure number=\"3\"><attributes><key><fifths>12</fifths><mode>dorian</mode></key></attributes><note><duration>8</duration></note></measure>"
                        + "</part></score-partwise>");

        List<MidiIo.MidiKeySignatureEvent> events = MidiIo.collectMidiKeySignatureEventsFromMusicXmlDoc(doc, 480);

        assertEquals(3, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(2, events.get(0).getFifths());
        assertEquals("major", events.get(0).getMode());
        assertEquals(1440, events.get(1).getStartTicks());
        assertEquals(-3, events.get(1).getFifths());
        assertEquals("minor", events.get(1).getMode());
        assertEquals(3360, events.get(2).getStartTicks());
        assertEquals(7, events.get(2).getFifths());
        assertEquals("major", events.get(2).getMode());
    }

    @Test
    public void collectsMidiKeySignatureEventsForFf59ExportLikeUpstreamRegression() {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<key><fifths>4</fifths><mode>major</mode></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>E</step><alter>1</alter><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>whole</type></note></measure>"
                + "<measure number=\"2\"><attributes><key><fifths>-1</fifths><mode>minor</mode></key>"
                + "</attributes><note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>whole</type></note></measure>"
                + "</part></score-partwise>");

        List<MidiIo.MidiKeySignatureEvent> events = MidiIo.collectMidiKeySignatureEventsFromMusicXmlDoc(doc, 128);

        assertEquals(2, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(4, events.get(0).getFifths());
        assertEquals("major", events.get(0).getMode());
        assertEquals(512, events.get(1).getStartTicks());
        assertEquals(-1, events.get(1).getFifths());
        assertEquals("minor", events.get(1).getMode());
    }

    @Test
    public void normalizesMidiExportKeySignatureEvents() {
        List<MidiIo.MidiKeySignatureEvent> defaultEvents = MidiIo.normalizeMidiExportKeySignatureEvents(
                Collections.<MidiIo.MidiKeySignatureEvent>emptyList());
        assertEquals(1, defaultEvents.size());
        assertEquals(0, defaultEvents.get(0).getStartTicks());
        assertEquals(0, defaultEvents.get(0).getFifths());
        assertEquals("major", defaultEvents.get(0).getMode());

        List<MidiIo.MidiKeySignatureEvent> events = MidiIo.normalizeMidiExportKeySignatureEvents(Arrays.asList(
                new MidiIo.MidiKeySignatureEvent(480, -9, "minor"),
                new MidiIo.MidiKeySignatureEvent(0, 2, "major"),
                new MidiIo.MidiKeySignatureEvent(480, 9, "dorian")));

        assertEquals(2, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(2, events.get(0).getFifths());
        assertEquals("major", events.get(0).getMode());
        assertEquals(480, events.get(1).getStartTicks());
        assertEquals(7, events.get(1).getFifths());
        assertEquals("major", events.get(1).getMode());

        List<MidiIo.MidiKeySignatureEvent> insertedZero = MidiIo.normalizeMidiExportKeySignatureEvents(Arrays.asList(
                new MidiIo.MidiKeySignatureEvent(960, -3, "minor")));
        assertEquals(2, insertedZero.size());
        assertEquals(0, insertedZero.get(0).getStartTicks());
        assertEquals(0, insertedZero.get(0).getFifths());
        assertEquals("major", insertedZero.get(0).getMode());
        assertEquals(960, insertedZero.get(1).getStartTicks());
        assertEquals(-3, insertedZero.get(1).getFifths());
        assertEquals("minor", insertedZero.get(1).getMode());
    }

    @Test
    public void normalizesMidiExportTempoAndTimeSignatureEvents() {
        List<MidiIo.MidiTempoEvent> defaultTempo = MidiIo.normalizeMidiExportTempoEvents(
                Collections.<MidiIo.MidiTempoEvent>emptyList(), 90);
        assertEquals(1, defaultTempo.size());
        assertEquals(0, defaultTempo.get(0).getTick());
        assertEquals(90, defaultTempo.get(0).getBpm());

        List<MidiIo.MidiTempoEvent> tempoEvents = MidiIo.normalizeMidiExportTempoEvents(Arrays.asList(
                new MidiIo.MidiTempoEvent(480, 10),
                new MidiIo.MidiTempoEvent(0, 301),
                new MidiIo.MidiTempoEvent(480, 121)), 100);
        assertEquals(2, tempoEvents.size());
        assertEquals(0, tempoEvents.get(0).getTick());
        assertEquals(300, tempoEvents.get(0).getBpm());
        assertEquals(480, tempoEvents.get(1).getTick());
        assertEquals(121, tempoEvents.get(1).getBpm());

        List<MidiIo.MidiTempoEvent> insertedTempoZero = MidiIo.normalizeMidiExportTempoEvents(Arrays.asList(
                new MidiIo.MidiTempoEvent(960, 80)), 400);
        assertEquals(2, insertedTempoZero.size());
        assertEquals(0, insertedTempoZero.get(0).getTick());
        assertEquals(300, insertedTempoZero.get(0).getBpm());
        assertEquals(960, insertedTempoZero.get(1).getTick());
        assertEquals(80, insertedTempoZero.get(1).getBpm());

        List<MidiIo.MidiTimeSignatureEvent> defaultTime = MidiIo.normalizeMidiExportTimeSignatureEvents(
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(), 480, 0);
        assertEquals(1, defaultTime.size());
        assertEquals(0, defaultTime.get(0).getStartTicks());
        assertEquals(4, defaultTime.get(0).getBeats());
        assertEquals(4, defaultTime.get(0).getBeatType());

        List<MidiIo.MidiTimeSignatureEvent> timeEvents = MidiIo.normalizeMidiExportTimeSignatureEvents(Arrays.asList(
                new MidiIo.MidiTimeSignatureEvent(480, 3, 4),
                new MidiIo.MidiTimeSignatureEvent(0, 4, 4),
                new MidiIo.MidiTimeSignatureEvent(480, 5, 8)), 480, 0);
        assertEquals(2, timeEvents.size());
        assertEquals(0, timeEvents.get(0).getStartTicks());
        assertEquals(4, timeEvents.get(0).getBeats());
        assertEquals(4, timeEvents.get(0).getBeatType());
        assertEquals(480, timeEvents.get(1).getStartTicks());
        assertEquals(5, timeEvents.get(1).getBeats());
        assertEquals(8, timeEvents.get(1).getBeatType());

        List<MidiIo.MidiTimeSignatureEvent> pickupPrelude = MidiIo.normalizeMidiExportTimeSignatureEvents(
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4)), 480, 480);
        assertEquals(2, pickupPrelude.size());
        assertEquals(0, pickupPrelude.get(0).getStartTicks());
        assertEquals(1, pickupPrelude.get(0).getBeats());
        assertEquals(4, pickupPrelude.get(0).getBeatType());
        assertEquals(480, pickupPrelude.get(1).getStartTicks());
        assertEquals(4, pickupPrelude.get(1).getBeats());
        assertEquals(4, pickupPrelude.get(1).getBeatType());

        List<MidiIo.MidiTimeSignatureEvent> sixEightPickupPrelude = MidiIo.normalizeMidiExportTimeSignatureEvents(
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 6, 8)), 480, 240);
        assertEquals(2, sixEightPickupPrelude.size());
        assertEquals(0, sixEightPickupPrelude.get(0).getStartTicks());
        assertEquals(1, sixEightPickupPrelude.get(0).getBeats());
        assertEquals(8, sixEightPickupPrelude.get(0).getBeatType());
        assertEquals(240, sixEightPickupPrelude.get(1).getStartTicks());
        assertEquals(6, sixEightPickupPrelude.get(1).getBeats());
        assertEquals(8, sixEightPickupPrelude.get(1).getBeatType());
    }

    @Test
    public void buildsMidiExportDiagnostics() {
        List<String> emptyDiagnostics = MidiIo.buildMidiExportDiagnostics(Arrays.asList(" keep ", "", null),
                Collections.<MidiIo.MidiTempoEvent>emptyList(),
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Collections.<MidiIo.MidiKeySignatureEvent>emptyList());
        assertEquals(4, emptyDiagnostics.size());
        assertEquals("keep", emptyDiagnostics.get(0));
        assertEquals("level=info;code=MIDI_EXPORT_DEFAULT_TEMPO_INSERTED;fmt=midi;startTick=0;bpm=120",
                emptyDiagnostics.get(1));
        assertEquals(
                "level=info;code=MIDI_EXPORT_DEFAULT_TIMESIG_INSERTED;fmt=midi;startTick=0;beats=4;beatType=4",
                emptyDiagnostics.get(2));
        assertEquals("level=info;code=MIDI_EXPORT_DEFAULT_KEYSIG_INSERTED;fmt=midi;startTick=0;fifths=0;mode=major",
                emptyDiagnostics.get(3));

        List<String> insertedAtZeroDiagnostics = MidiIo.buildMidiExportDiagnostics(null,
                Arrays.asList(new MidiIo.MidiTempoEvent(480, 100)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(480, 3, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(480, -3, "minor")));
        assertEquals(3, insertedAtZeroDiagnostics.size());
        assertEquals("level=info;code=MIDI_EXPORT_DEFAULT_TEMPO_AT_ZERO_INSERTED;fmt=midi;startTick=0;bpm=120",
                insertedAtZeroDiagnostics.get(0));
        assertEquals(
                "level=info;code=MIDI_EXPORT_DEFAULT_TIMESIG_AT_ZERO_INSERTED;fmt=midi;startTick=0;beats=4;beatType=4",
                insertedAtZeroDiagnostics.get(1));
        assertEquals(
                "level=info;code=MIDI_EXPORT_DEFAULT_KEYSIG_AT_ZERO_INSERTED;fmt=midi;startTick=0;fifths=0;mode=major",
                insertedAtZeroDiagnostics.get(2));

        List<String> noDefaultDiagnostics = MidiIo.buildMidiExportDiagnostics(null,
                Arrays.asList(new MidiIo.MidiTempoEvent(0, 100)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 3, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, 2, "major")));
        assertEquals(0, noDefaultDiagnostics.size());
    }

    @Test
    public void buildsMidiExportTextMetaLines() {
        Map<String, String> trackNameById = new LinkedHashMap<String, String>();
        trackNameById.put("p1", " Lead Piano ");
        trackNameById.put("p2", "");
        trackNameById.put("p3", "Drums & FX");

        MidiIo.MidiExportTextMetaLines lines = MidiIo.buildMidiExportTextMetaLines("  Main   Title  ",
                " Movement  1 ", "A+B", 240, Arrays.asList("p1", "p2", "p3"), trackNameById, true);

        assertEquals("Main Title", lines.getMetaTrackTitle());
        assertEquals(Arrays.asList("title:Main Title"), lines.getStandardTextMetaLines());
        assertEquals(Arrays.asList("mks:meta-version:1", "mks:title:Main%20%20%20Title",
                "mks:movement-title:Movement%20%201", "mks:composer:A%2BB", "mks:pickup-ticks:240",
                "mks:part-name-track:1:Lead%20Piano", "mks:part-name-track:3:Drums%20%26%20FX"),
                lines.getMksTextMetaLines());
        assertEquals(240, lines.getPickupTicks());

        MidiIo.MidiExportTextMetaLines movementFallback = MidiIo.buildMidiExportTextMetaLines("",
                " Movement   Only ", "", -1, Arrays.asList("p1"), trackNameById, false);
        assertEquals("Movement Only", movementFallback.getMetaTrackTitle());
        assertEquals(Arrays.asList("title:Movement Only"), movementFallback.getStandardTextMetaLines());
        assertEquals(Collections.<String>emptyList(), movementFallback.getMksTextMetaLines());
        assertEquals(0, movementFallback.getPickupTicks());

        MidiIo.MidiExportTextMetaLines untitled = MidiIo.buildMidiExportTextMetaLines(null, null, null, 0, null,
                null, true);
        assertEquals("Untitled", untitled.getMetaTrackTitle());
        assertEquals(Arrays.asList("title:Untitled"), untitled.getStandardTextMetaLines());
        assertEquals(Arrays.asList("mks:meta-version:1"), untitled.getMksTextMetaLines());
    }

    @Test
    public void buildsMidiPlaybackTracksById() {
        MidiIo.MidiPlaybackTrackGrouping grouping = MidiIo.buildMidiPlaybackTracksById(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 80, "p2", " Second "),
                new MidiIo.RawMidiPlaybackEvent(61, 120, 120, 1, 80, "", " Default "),
                new MidiIo.RawMidiPlaybackEvent(62, 240, 120, 1, 80, "p1", "First"),
                new MidiIo.RawMidiPlaybackEvent(63, 360, 120, 1, 80, "p2", "Ignored")));

        assertEquals(Arrays.asList("__default__", "p1", "p2"), grouping.getSortedTrackIds());
        assertEquals(3, grouping.getTracksById().size());
        assertEquals(1, grouping.getTracksById().get("__default__").size());
        assertEquals(1, grouping.getTracksById().get("p1").size());
        assertEquals(2, grouping.getTracksById().get("p2").size());
        assertEquals(60, grouping.getTracksById().get("p2").get(0).getMidiNumber());
        assertEquals(63, grouping.getTracksById().get("p2").get(1).getMidiNumber());
        assertEquals("Default", grouping.getTrackNameById().get("__default__"));
        assertEquals("First", grouping.getTrackNameById().get("p1"));
        assertEquals("Second", grouping.getTrackNameById().get("p2"));

        MidiIo.MidiExportTextMetaLines lines = MidiIo.buildMidiExportTextMetaLines("", "", "", 0,
                grouping.getSortedTrackIds(), grouping.getTrackNameById(), true);
        assertEquals(Arrays.asList("mks:meta-version:1",
                "mks:part-name-track:1:Default",
                "mks:part-name-track:2:First",
                "mks:part-name-track:3:Second"), lines.getMksTextMetaLines());
    }

    @Test
    public void buildsMidiExportMksSysexChunkTexts() {
        List<MidiIo.RawMidiPlaybackEvent> events = Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 80, "p1", "Piano"),
                new MidiIo.RawMidiPlaybackEvent(64, 120, 120, 20, 80, "p2", "Lead"));
        MidiIo.MidiPlaybackTrackGrouping grouping = MidiIo.buildMidiPlaybackTracksById(events);

        assertEquals("violin", MidiIo.normalizeMidiProgramPreset("violin"));
        assertEquals("electric_piano_2", MidiIo.normalizeMidiProgramPreset("bad"));
        assertEquals(2, MidiIo.countMidiExportChannels(events));

        List<String> chunks = MidiIo.buildMidiExportMksSysexChunkTexts(480, events, grouping,
                Arrays.asList(new MidiIo.RawMidiControlEvent("p1", "Piano", 0, 1, 64, 127)),
                Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, 0, "major")),
                Arrays.asList("diag"));

        StringBuilder payload = new StringBuilder();
        for (String chunk : chunks) {
            payload.append(chunk.substring(chunk.indexOf("|d=") + 3));
        }
        String decoded = MidiIo.safeDecodeURIComponent(payload.toString());
        assertEquals(true, decoded.contains("tpq=480"));
        assertEquals(true, decoded.contains("event-count=2"));
        assertEquals(true, decoded.contains("track-count=2"));
        assertEquals(true, decoded.contains("tempo-event-count=1"));
        assertEquals(true, decoded.contains("timesig-event-count=1"));
        assertEquals(true, decoded.contains("keysig-event-count=1"));
        assertEquals(true, decoded.contains("control-event-count=1"));
        assertEquals(true, decoded.contains("channel-count=2"));
        assertEquals(true, decoded.contains("diag-0001=diag"));
    }

    @Test
    public void preparesMidiExportPlayback() {
        List<MidiIo.RawMidiPlaybackEvent> events = Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 60, "p1", "Piano"),
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 100, "p1", "Piano"),
                new MidiIo.RawMidiPlaybackEvent(64, 120, 120, 20, 80, "p2", "Lead"));
        MidiIo.MidiExportPlaybackPreparation preparation = MidiIo.prepareMidiExportPlayback(events,
                90, "bad", Arrays.asList(new MidiIo.RawMidiControlEvent("p1", "Piano", 0, 1, 64, 127)),
                Collections.<MidiIo.MidiTempoEvent>emptyList(),
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Collections.<MidiIo.MidiKeySignatureEvent>emptyList(),
                false, true, -1, Arrays.asList(" keep ", "", null), true,
                "  Main   Title ", "Movement", "A+B", 0);

        assertEquals(1, preparation.getTicksPerQuarter());
        assertEquals(false, preparation.isEmbedMksSysEx());
        assertEquals("electric_piano_2", preparation.getProgramPreset());
        assertEquals(2, preparation.getSourceEvents().size());
        assertEquals(100, preparation.getSourceEvents().get(0).getVelocity());
        assertEquals(Arrays.asList("p1", "p2"), preparation.getTrackGrouping().getSortedTrackIds());
        assertEquals("Main Title", preparation.getTextMetaLines().getMetaTrackTitle());
        assertEquals(Arrays.asList("title:Main Title", "mks:meta-version:1",
                "mks:title:Main%20%20%20Title", "mks:movement-title:Movement", "mks:composer:A%2BB",
                "mks:part-name-track:1:Piano", "mks:part-name-track:2:Lead"),
                preparation.getCombinedTextMetaLines());
        assertEquals(1, preparation.getTempoEvents().size());
        assertEquals(0, preparation.getTempoEvents().get(0).getTick());
        assertEquals(90, preparation.getTempoEvents().get(0).getBpm());
        assertEquals(1, preparation.getTimeSignatureEvents().size());
        assertEquals(4, preparation.getTimeSignatureEvents().get(0).getBeats());
        assertEquals(1, preparation.getKeySignatureEvents().size());
        assertEquals("keep", preparation.getDiagnostics().get(0));
        assertEquals(4, preparation.getDiagnostics().size());

        StringBuilder payload = new StringBuilder();
        for (String chunk : preparation.getSysexChunks()) {
            payload.append(chunk.substring(chunk.indexOf("|d=") + 3));
        }
        String decoded = MidiIo.safeDecodeURIComponent(payload.toString());
        assertEquals(true, decoded.contains("event-count=2"));
        assertEquals(true, decoded.contains("track-count=2"));
        assertEquals(true, decoded.contains("control-event-count=1"));
        assertEquals(true, decoded.contains("channel-count=2"));
        assertEquals(true, decoded.contains("diag-0001=keep"));
    }

    @Test
    public void buildsRawMidiBytesForPlaybackFromPreparation() {
        List<MidiIo.RawMidiPlaybackEvent> events = Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 80, "p1", "Piano"));
        List<MidiIo.RawMidiControlEvent> controls = Arrays.asList(
                new MidiIo.RawMidiControlEvent("p1", "Piano", 0, 1, 64, 127));
        MidiIo.MidiExportPlaybackPreparation preparation = MidiIo.prepareMidiExportPlayback(events,
                120, "violin", controls,
                Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, 0, "major")),
                true, true, 480, Collections.<String>emptyList(), false,
                "Song", "", "", 0);

        Map<String, Integer> overrides = new LinkedHashMap<String, Integer>();
        overrides.put("p1", Integer.valueOf(40));
        byte[] bytes = MidiIo.buildRawMidiBytesForPlayback(preparation, overrides, controls, "on_before_off");

        List<MidiIo.MidiTickKeySignatureEvent> rawKeys = Arrays.asList(
                new MidiIo.MidiTickKeySignatureEvent(0, 0, "major"));
        List<byte[]> chunks = new ArrayList<byte[]>();
        chunks.add(MidiIo.buildRawMidiTempoTrackChunk(preparation.getTempoEvents(),
                preparation.getTimeSignatureEvents(), rawKeys,
                new MidiIo.RawMidiTempoTrackOptions(true, preparation.getSysexChunks(),
                        preparation.getCombinedTextMetaLines(), preparation.getTextMetaLines().getMetaTrackTitle())));
        chunks.addAll(MidiIo.buildRawMidiNoteTrackChunks(preparation.getSourceEvents(), overrides,
                preparation.getProgramPreset(), "on_before_off"));
        chunks.addAll(MidiIo.buildRawMidiControlTrackChunks(controls));

        assertArrayEquals(MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480), bytes);
        assertArrayEquals(new byte[] { 'M', 'T', 'h', 'd', 0, 0, 0, 6, 0, 1, 0, 3, 1, (byte) 0xe0 },
                Arrays.copyOf(bytes, 14));
    }

    @Test
    public void buildsMidiPlaybackExportRawAndWriterResults() {
        List<MidiIo.RawMidiPlaybackEvent> events = Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 80, "p1", "Piano"));
        List<MidiIo.RawMidiControlEvent> controls = Arrays.asList(
                new MidiIo.RawMidiControlEvent("p1", "Piano", 0, 1, 64, 127));
        Map<String, Integer> overrides = new LinkedHashMap<String, Integer>();
        overrides.put("p1", Integer.valueOf(40));

        MidiIo.MidiExportPlaybackBuildResult rawResult = MidiIo.buildMidiPlaybackExport(events, 120,
                "violin", overrides, controls, Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, 0, "major")),
                true, true, true, 480, Collections.<String>emptyList(), false, "pitch_order",
                "Song", "", "", 0);

        assertEquals(true, rawResult.isRawWriter());
        assertEquals(null, rawResult.getWriterTrackPlan());
        assertEquals(480, rawResult.getPreparation().getTicksPerQuarter());
        assertArrayEquals(MidiIo.buildRawMidiBytesForPlayback(rawResult.getPreparation(), overrides,
                controls, "pitch_order"), rawResult.getRawBytes());

        MidiIo.MidiExportPlaybackBuildResult writerResult = MidiIo.buildMidiPlaybackExport(events, 120,
                "bad", overrides, controls, Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, 0, "major")),
                false, false, false, 480, Collections.<String>emptyList(), false, null,
                "", "Movement", "", 0);

        assertEquals(false, writerResult.isRawWriter());
        assertEquals(null, writerResult.getRawBytes());
        assertEquals("electric_piano_2", writerResult.getPreparation().getProgramPreset());
        assertEquals("Movement", writerResult.getWriterTrackPlan().getMetaTrackName());
        assertEquals(3, writerResult.getWriterTrackPlan().getTrackCount());
        assertEquals(4, writerResult.getWriterTrackPlan().getMetaEventData().size());
        assertEquals(1, writerResult.getWriterTrackPlan().getPlaybackTrackPlans().size());
        assertEquals(1, writerResult.getWriterTrackPlan().getControlTrackPlans().size());
    }

    @Test
    public void doesNotEmitMksTextMetadataWhenDisabledInRawMidiExport() {
        byte[] midi = exportRawMidiForTextMetaRegression(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(69, 0, 240, 1, 90, "P1", "P1")),
                false, "Title", "", "Composer", 240);
        List<String> texts = collectTextMetaFromMidi(midi);

        assertEquals(false, anyStartsWith(texts, "mks:"));
        assertEquals(true, texts.contains("title:Title"));
    }

    @Test
    public void emitsStandardTitleTextMetaEvenWhenMksTextMetadataIsDisabled() {
        byte[] midi = exportRawMidiForTextMetaRegression(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(69, 0, 240, 1, 90, "P1", "P1")),
                false, "Sample Title", "", "", 0);
        List<String> texts = collectTextMetaFromMidi(midi);

        assertEquals(true, texts.contains("title:Sample Title"));
        assertEquals(false, anyStartsWith(texts, "mks:"));
    }

    @Test
    public void emitsRawWriterTrackNameMetaForNoteTracks() {
        byte[] midi = exportRawMidiForTextMetaRegression(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(69, 0, 240, 1, 90, "P1", "Violin 1"),
                new MidiIo.RawMidiPlaybackEvent(67, 0, 240, 1, 90, "P2", "Violin 2")),
                false, "Sample Title", "", "", 0);
        List<String> texts = collectTextMetaFromMidi(midi);

        assertEquals(true, texts.contains("Violin 1"));
        assertEquals(true, texts.contains("Violin 2"));
    }

    @Test
    public void roundtripsGoldenFixturesThroughMidiKeepingKeyMeterTempoBaseline() {
        for (String fixture : Arrays.asList("base.musicxml", "interleaved_voices.musicxml",
                "roundtrip_piano_tempo.musicxml")) {
            Document source = parseMusicXmlFixture("abc-roundtrip/" + fixture);
            Document roundtripped = roundtripMusicXmlFixtureThroughMidi(source);

            assertEquals(firstMeter(source), firstMeter(roundtripped), fixture);
            Integer sourceFifths = firstKeyFifths(source);
            if (sourceFifths != null) {
                assertEquals(sourceFifths, firstKeyFifths(roundtripped), fixture);
            }
            Integer sourceTempo = firstTempo(source);
            if (sourceTempo != null) {
                assertEquals(sourceTempo, firstTempo(roundtripped), fixture);
            }
        }
    }

    @Test
    public void buildsMidiExportPlaybackTrackPlan() {
        Map<String, Integer> overrides = new LinkedHashMap<String, Integer>();
        overrides.put("p1", Integer.valueOf(40));
        MidiIo.MidiExportPlaybackTrackPlan plan = MidiIo.buildMidiExportPlaybackTrackPlan("p1", 0, Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(64, 120, 120, 20, 80, "p1", "Ignored"),
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 10, 80, "p1", " Lead "),
                new MidiIo.RawMidiPlaybackEvent(55, 0, 120, -4, 80, "p1", "Also ignored")),
                overrides, "violin");

        assertEquals("Also ignored", plan.getTrackName());
        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(10), Integer.valueOf(16)), plan.getChannels());
        assertEquals(40, plan.getSelectedInstrumentProgram());
        assertEquals(55, plan.getTrackEvents().get(0).getMidiNumber());
        assertEquals(60, plan.getTrackEvents().get(1).getMidiNumber());
        assertEquals(64, plan.getTrackEvents().get(2).getMidiNumber());

        List<MidiIo.MidiExportProgramChangeEventFields> programFields =
                MidiIo.buildMidiExportProgramChangeEventFields(plan);
        assertEquals(2, programFields.size());
        assertEquals(40, programFields.get(0).getInstrument());
        assertEquals(1, programFields.get(0).getChannel());
        assertEquals(0, programFields.get(0).getDelta());
        assertEquals(16, programFields.get(1).getChannel());

        List<MidiIo.MidiWriterNoteEventFields> noteFields = MidiIo.buildMidiExportNoteEventFields(plan);
        assertEquals(Arrays.asList("G3"), noteFields.get(0).getPitch());
        assertEquals("T120", noteFields.get(0).getDuration());
        assertEquals(Integer.valueOf(0), noteFields.get(0).getStartTick());
        assertEquals(80, noteFields.get(0).getVelocity());
        assertEquals(1, noteFields.get(0).getChannel());
        assertEquals(Arrays.asList("E4"), noteFields.get(2).getPitch());
        assertEquals(Integer.valueOf(120), noteFields.get(2).getStartTick());
        assertEquals(16, noteFields.get(2).getChannel());

        MidiIo.MidiExportPlaybackTrackPlan fallback = MidiIo.buildMidiExportPlaybackTrackPlan("", 2, Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 80, "", "")), null, "bad");
        assertEquals("Track 3", fallback.getTrackName());
        assertEquals(5, fallback.getSelectedInstrumentProgram());
    }

    @Test
    public void buildsMidiExportControlTrackPlans() {
        List<MidiIo.MidiExportControlTrackPlan> plans = MidiIo.buildMidiExportControlTrackPlans(Arrays.asList(
                new MidiIo.RawMidiControlEvent("p2", "Lead", 120, 20, 64, 0),
                new MidiIo.RawMidiControlEvent("p1", "Piano", 120, 1, 10, 200),
                new MidiIo.RawMidiControlEvent("p1", "Piano", 0, 1, 64, 127),
                new MidiIo.RawMidiControlEvent("p1", "Piano", 120, 1, 64, 0),
                new MidiIo.RawMidiControlEvent("p1", "Piano", 120, 1, 10, 5)));

        assertEquals(2, plans.size());
        assertEquals("p1::1", plans.get(0).getControlKey());
        assertEquals("Piano Pedal", plans.get(0).getTrackName());
        assertEquals(64, plans.get(0).getControlEvents().get(0).getControllerNumber());
        assertEquals(10, plans.get(0).getControlEvents().get(1).getControllerNumber());
        assertEquals(5, plans.get(0).getControlEvents().get(1).getControllerValue());
        assertEquals(10, plans.get(0).getControlEvents().get(2).getControllerNumber());
        assertEquals(200, plans.get(0).getControlEvents().get(2).getControllerValue());

        List<MidiIo.MidiWriterControllerChangeEventFields> fields =
                plans.get(0).getControllerChangeFields();
        assertEquals(4, fields.size());
        assertEquals(1, fields.get(0).getChannel());
        assertEquals(0xb0, fields.get(0).getStatusByte());
        assertEquals(64, fields.get(0).getControllerNumber());
        assertEquals(127, fields.get(0).getControllerValue());
        assertEquals(0, fields.get(0).getDelta());
        assertEquals(10, fields.get(1).getControllerNumber());
        assertEquals(5, fields.get(1).getControllerValue());
        assertEquals(120, fields.get(1).getDelta());
        assertEquals(10, fields.get(2).getControllerNumber());
        assertEquals(127, fields.get(2).getControllerValue());
        assertEquals(0, fields.get(2).getDelta());

        assertEquals("p2::16", plans.get(1).getControlKey());
        assertEquals(16, plans.get(1).getControllerChangeFields().get(0).getChannel());
        assertEquals(0xbf, plans.get(1).getControllerChangeFields().get(0).getStatusByte());
    }

    @Test
    public void buildsMidiExportWriterTrackPlan() {
        List<MidiIo.RawMidiPlaybackEvent> events = Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(64, 120, 120, 2, 70, "p2", "Lead"),
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 80, "p1", "Piano"));
        MidiIo.MidiPlaybackTrackGrouping grouping = MidiIo.buildMidiPlaybackTracksById(events);
        Map<String, Integer> overrides = new LinkedHashMap<String, Integer>();
        overrides.put("p1", Integer.valueOf(40));
        byte[] timeline = MidiIo.buildTempoMetaEventData(0, 120);

        MidiIo.MidiExportWriterTrackPlan plan = MidiIo.buildMidiExportWriterTrackPlan("Meta",
                Arrays.asList(timeline), true, Arrays.asList("A"), Arrays.asList("title:Meta"),
                grouping, overrides, "violin",
                Arrays.asList(new MidiIo.RawMidiControlEvent("p1", "Piano", 0, 1, 64, 127)));

        assertEquals("Meta", plan.getMetaTrackName());
        assertEquals(4, plan.getTrackCount());
        assertEquals(3, plan.getMetaEventData().size());
        assertArrayEquals(timeline, plan.getMetaEventData().get(0));
        assertArrayEquals(MidiIo.buildMksSysexEventData(0, "A"), plan.getMetaEventData().get(1));
        assertArrayEquals(MidiIo.buildTextMetaEventData(0, "title:Meta", 0x01), plan.getMetaEventData().get(2));
        assertEquals(2, plan.getPlaybackTrackPlans().size());
        assertEquals("Piano", plan.getPlaybackTrackPlans().get(0).getTrackName());
        assertEquals(40, plan.getPlaybackTrackPlans().get(0).getSelectedInstrumentProgram());
        assertEquals("Lead", plan.getPlaybackTrackPlans().get(1).getTrackName());
        assertEquals(1, plan.getControlTrackPlans().size());
        assertEquals("p1::1", plan.getControlTrackPlans().get(0).getControlKey());
    }

    @Test
    public void parsesMksMidiTextMetadata() {
        MidiIo.MksMidiTextMetadata metadata = MidiIo.parseMksMidiTextMetadata(Arrays.asList(
                "mks:title:Hello%20World",
                "mks:title:Ignored",
                "mks:movement-title:Move%201",
                "mks:composer:A%2BB",
                "mks:pickup-ticks:240",
                "mks:part-name-track:2:Lead%20Piano",
                "mks:part-name-track:2:Ignored"));

        assertEquals("Hello World", metadata.getTitle());
        assertEquals("Move 1", metadata.getMovementTitle());
        assertEquals("A+B", metadata.getComposer());
        assertEquals(Integer.valueOf(240), metadata.getPickupTicks());
        assertEquals("Lead Piano", metadata.getPartNameByTrackIndex().get(Integer.valueOf(2)));
    }

    @Test
    public void parsesAndAssemblesMksSysExPayloads() {
        MidiIo.MksSysExChunk first = MidiIo
                .parseMksSysExChunk("mks|v=1|m=7|i=1|n=2|d=Hel%20".getBytes(StandardCharsets.US_ASCII));
        MidiIo.MksSysExChunk second = MidiIo
                .parseMksSysExChunk("mks|v=1|m=7|i=2|n=2|d=lo".getBytes(StandardCharsets.US_ASCII));

        assertEquals(7, first.getMessageId());
        assertEquals(1, first.getChunkIndex());
        assertEquals(2, first.getTotalChunks());
        assertEquals("Hel ", first.getData());
        assertEquals(Arrays.asList("Hel lo"), MidiIo.assembleMksSysExPayloads(Arrays.asList(second, first)));
        assertEquals(null, MidiIo.parseMksSysExChunk("bad".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Arrays.<String>asList(), MidiIo.assembleMksSysExPayloads(Arrays.asList(first)));
    }

    @Test
    public void parsesValidSmfHeader() {
        byte[] midi = new byte[] {
                'M', 'T', 'h', 'd',
                0, 0, 0, 6,
                0, 1,
                0, 2,
                1, (byte) 0xe0 };

        MidiIo.SmfHeaderParseResult result = MidiIo.parseSmfHeader(midi);

        assertEquals(0, result.getDiagnostics().size());
        assertEquals(1, result.getHeader().getFormat());
        assertEquals(2, result.getHeader().getTrackCount());
        assertEquals(480, result.getHeader().getTicksPerQuarter());
        assertEquals(14, result.getHeader().getNextOffset());
    }

    @Test
    public void rejectsMalformedSmfHeadersWithUpstreamDiagnostics() {
        MidiIo.SmfHeaderParseResult tooShort = MidiIo.parseSmfHeader(new byte[] { 'M', 'T', 'h', 'd' });
        assertEquals(null, tooShort.getHeader());
        assertEquals("MIDI_INVALID_FILE", tooShort.getDiagnostics().get(0).getCode());
        assertEquals("SMF header is too short.", tooShort.getDiagnostics().get(0).getMessage());

        byte[] missingSignature = new byte[] {
                'B', 'A', 'D', '!',
                0, 0, 0, 6,
                0, 1,
                0, 1,
                1, (byte) 0xe0 };
        MidiIo.SmfHeaderParseResult missing = MidiIo.parseSmfHeader(missingSignature);
        assertEquals(null, missing.getHeader());
        assertEquals("Missing MThd header chunk.", missing.getDiagnostics().get(0).getMessage());

        byte[] shortChunk = new byte[] {
                'M', 'T', 'h', 'd',
                0, 0, 0, 5,
                0, 1,
                0, 1,
                1, (byte) 0xe0 };
        MidiIo.SmfHeaderParseResult invalidFields = MidiIo.parseSmfHeader(shortChunk);
        assertEquals(null, invalidFields.getHeader());
        assertEquals("Invalid SMF header fields.", invalidFields.getDiagnostics().get(0).getMessage());
    }

    @Test
    public void rejectsUnsupportedSmfHeadersWithUpstreamDiagnostics() {
        byte[] unsupportedFormat = new byte[] {
                'M', 'T', 'h', 'd',
                0, 0, 0, 6,
                0, 2,
                0, 1,
                1, (byte) 0xe0 };
        MidiIo.SmfHeaderParseResult format = MidiIo.parseSmfHeader(unsupportedFormat);
        assertEquals(null, format.getHeader());
        assertEquals("MIDI_UNSUPPORTED_FORMAT", format.getDiagnostics().get(0).getCode());
        assertEquals("Unsupported SMF format 2. Supported formats are 0 and 1.",
                format.getDiagnostics().get(0).getMessage());

        byte[] smpteDivision = new byte[] {
                'M', 'T', 'h', 'd',
                0, 0, 0, 6,
                0, 1,
                0, 1,
                (byte) 0xe7, 0x28 };
        MidiIo.SmfHeaderParseResult division = MidiIo.parseSmfHeader(smpteDivision);
        assertEquals(null, division.getHeader());
        assertEquals("MIDI_UNSUPPORTED_DIVISION", division.getDiagnostics().get(0).getCode());
        assertEquals("SMPTE time division is unsupported. Use PPQ-based MIDI files.",
                division.getDiagnostics().get(0).getMessage());

        byte[] zeroPpq = new byte[] {
                'M', 'T', 'h', 'd',
                0, 0, 0, 6,
                0, 1,
                0, 1,
                0, 0 };
        MidiIo.SmfHeaderParseResult ppq = MidiIo.parseSmfHeader(zeroPpq);
        assertEquals(null, ppq.getHeader());
        assertEquals("PPQ must be a positive integer.", ppq.getDiagnostics().get(0).getMessage());
    }

    @Test
    public void parsesTrackSummaryMetaEventsAndRunningStatusNotes() {
        byte[] track = new byte[] {
                0, (byte) 0xff, 0x03, 5, 'P', 'i', 'a', 'n', 'o',
                0, (byte) 0xff, 0x01, 11, 'T', 'i', 't', 'l', 'e', ':', ' ', 'S', 'o', 'n', 'g',
                0, (byte) 0xff, 0x02, 13, 'C', 'o', 'm', 'p', 'o', 's', 'e', 'r', ':', ' ', 'I', 'g', 'a',
                0, (byte) 0xff, 0x01, 14, 'm', 'k', 's', ':', 't', 'i', 't', 'l', 'e', ':', 'M', 'e', 't', 'a',
                0, (byte) 0xff, 0x58, 4, 3, 2, 24, 8,
                0, (byte) 0xff, 0x59, 2, 1, 0,
                0, (byte) 0xff, 0x51, 3, 0x07, (byte) 0xa1, 0x20,
                0, (byte) 0xc0, 4,
                0, (byte) 0xb0, 7, 100,
                0, (byte) 0x90, 60, 64,
                (byte) 0x81, 0x70, 60, 0 };

        MidiIo.SmfParseSummary summary = MidiIo.parseTrackSummary(track, 3);

        assertEquals("Piano", summary.getTrackName());
        assertEquals(Arrays.asList("Song"), summary.getStandardTitleCandidates());
        assertEquals(Arrays.asList("Iga"), summary.getStandardComposerCandidates());
        assertEquals(Arrays.asList("mks:title:Meta"), summary.getMksTextMetaLines());
        assertEquals(true, summary.getChannels().contains(Integer.valueOf(1)));
        assertEquals(Integer.valueOf(5), summary.getProgramByTrackChannel().get("3:1"));

        assertEquals(1, summary.getControllerEvents().size());
        assertEquals(7, summary.getControllerEvents().get(0).getControllerNumber());
        assertEquals(100, summary.getControllerEvents().get(0).getControllerValue());

        assertEquals(1, summary.getTimeSignatureEvents().size());
        assertEquals(3, summary.getTimeSignatureEvents().get(0).getBeats());
        assertEquals(4, summary.getTimeSignatureEvents().get(0).getBeatType());
        assertEquals(1, summary.getKeySignatureEvents().get(0).getFifths());
        assertEquals("major", summary.getKeySignatureEvents().get(0).getMode());
        assertEquals(120, summary.getTempoEvents().get(0).getBpm());

        assertEquals(1, summary.getNotes().size());
        assertEquals(3, summary.getNotes().get(0).getTrackIndex());
        assertEquals(1, summary.getNotes().get(0).getChannel());
        assertEquals(60, summary.getNotes().get(0).getMidi());
        assertEquals(0, summary.getNotes().get(0).getStartTick());
        assertEquals(240, summary.getNotes().get(0).getEndTick());
        assertEquals(64, summary.getNotes().get(0).getVelocity());
        assertEquals(0, summary.getParseWarnings().size());
    }

    @Test
    public void parsesTrackSummaryWarningsForMalformedEvents() {
        MidiIo.SmfParseSummary runningStatus = MidiIo.parseTrackSummary(new byte[] { 0, 60, 0 }, 0);
        assertEquals(1, runningStatus.getParseWarnings().size());
        assertEquals("Running status without previous status; event dropped.",
                runningStatus.getParseWarnings().get(0).getMessage());

        MidiIo.SmfParseSummary unmatchedOff = MidiIo.parseTrackSummary(new byte[] { 0, (byte) 0x80, 60, 0 }, 0);
        assertEquals(1, unmatchedOff.getParseWarnings().size());
        assertEquals("Note off without matching note on (ch 1, note 60).",
                unmatchedOff.getParseWarnings().get(0).getMessage());

        MidiIo.SmfParseSummary unmatchedOn = MidiIo.parseTrackSummary(new byte[] { 0, (byte) 0x90, 62, 80 }, 0);
        assertEquals(1, unmatchedOn.getParseWarnings().size());
        assertEquals("Note on without matching note off (ch 1, note 62, start 0).",
                unmatchedOn.getParseWarnings().get(0).getMessage());
    }

    @Test
    public void quantizesImportedNotesWithResolvedGrid() {
        List<MidiIo.SmfImportedNote> notes = Arrays.asList(
                new MidiIo.SmfImportedNote(2, 3, 64, 10, 121, 70),
                new MidiIo.SmfImportedNote(2, 3, 67, 121, 239, 90));

        MidiIo.QuantizedImportedNotesResult quantized = MidiIo.quantizeImportedNotes(notes, 480, "1/16", false);

        assertEquals(120, quantized.getQTick());
        assertEquals(4, quantized.getDivisions());
        assertEquals(0, quantized.getWarnings().size());
        assertEquals(2, quantized.getNotes().size());
        assertEquals(2, quantized.getNotes().get(0).getTrackIndex());
        assertEquals(3, quantized.getNotes().get(0).getChannel());
        assertEquals(64, quantized.getNotes().get(0).getMidi());
        assertEquals(0, quantized.getNotes().get(0).getStartTick());
        assertEquals(120, quantized.getNotes().get(0).getEndTick());
        assertEquals(70, quantized.getNotes().get(0).getVelocity());
        assertEquals(120, quantized.getNotes().get(1).getStartTick());
        assertEquals(240, quantized.getNotes().get(1).getEndTick());
    }

    @Test
    public void appliesImportedControllerVelocityScaleByTrackChannelAndTick() {
        List<MidiIo.ImportedQuantizedNote> notes = Arrays.asList(
                new MidiIo.ImportedQuantizedNote(2, 1, 60, 0, 120, 100),
                new MidiIo.ImportedQuantizedNote(2, 1, 62, 240, 360, 100),
                new MidiIo.ImportedQuantizedNote(3, 1, 64, 240, 360, 100));
        List<MidiIo.MidiControllerEvent> controllers = Arrays.asList(
                new MidiIo.MidiControllerEvent(2, 0, 1, 7, 64),
                new MidiIo.MidiControllerEvent(2, 200, 1, 11, 64),
                new MidiIo.MidiControllerEvent(2, 300, 1, 7, 127));

        List<MidiIo.ImportedQuantizedNote> scaled = MidiIo.applyImportedControllerVelocityScale(notes, controllers);

        assertEquals(50, scaled.get(0).getVelocity());
        assertEquals(25, scaled.get(1).getVelocity());
        assertEquals(100, scaled.get(2).getVelocity());
        assertEquals(60, scaled.get(0).getMidi());
        assertEquals(240, scaled.get(1).getStartTick());
    }

    @Test
    public void allocatesAutoVoicesByStartClusterAndOverlap() {
        List<MidiIo.ImportedQuantizedNote> notes = Arrays.asList(
                new MidiIo.ImportedQuantizedNote(0, 1, 64, 0, 480, 80),
                new MidiIo.ImportedQuantizedNote(0, 1, 60, 0, 480, 80),
                new MidiIo.ImportedQuantizedNote(0, 1, 67, 240, 720, 80),
                new MidiIo.ImportedQuantizedNote(0, 1, 65, 480, 600, 80));
        List<MidiIo.MidiImportDiagnostic> warnings = new java.util.ArrayList<MidiIo.MidiImportDiagnostic>();

        List<MidiIo.ImportedVoiceCluster> clusters = MidiIo.allocateAutoVoices(notes, warnings);

        assertEquals(3, clusters.size());
        assertEquals(1, clusters.get(0).getVoice());
        assertEquals(0, clusters.get(0).getStartTick());
        assertEquals(480, clusters.get(0).getEndTick());
        assertEquals(60, clusters.get(0).getNotes().get(0).getMidi());
        assertEquals(64, clusters.get(0).getNotes().get(1).getMidi());
        assertEquals(2, clusters.get(1).getVoice());
        assertEquals(240, clusters.get(1).getStartTick());
        assertEquals(1, clusters.get(2).getVoice());
        assertEquals(480, clusters.get(2).getStartTick());
        assertEquals(1, warnings.size());
        assertEquals("MIDI_POLYPHONY_VOICE_ASSIGNED", warnings.get(0).getCode());
        assertEquals("Auto voice split assigned 2 voices.", warnings.get(0).getMessage());
    }

    @Test
    public void splitsClustersToMeasureSegmentsWithPickupAndGrandStaff() {
        MidiIo.ImportedVoiceCluster lowPickupCluster = new MidiIo.ImportedVoiceCluster(1, 360, 600,
                Arrays.asList(new MidiIo.ImportedQuantizedNote(0, 1, 48, 360, 600, 88)));
        MidiIo.ImportedVoiceCluster highCluster = new MidiIo.ImportedVoiceCluster(1, 600, 960,
                Arrays.asList(new MidiIo.ImportedQuantizedNote(0, 1, 72, 600, 960, 90)));

        List<MidiIo.ImportedVoiceNoteSegment> segments = MidiIo.splitClustersToMeasureSegments(
                new MidiIo.SplitClustersToMeasureSegmentsParams(Arrays.asList(lowPickupCluster, highCluster), 480, 4,
                        1920, 480, false, true));

        assertEquals(3, segments.size());
        assertEquals(0, segments.get(0).getMeasureIndex());
        assertEquals(2, segments.get(0).getStaff());
        assertEquals(3, segments.get(0).getStartDiv());
        assertEquals(1, segments.get(0).getDurDiv());
        assertEquals(360, segments.get(0).getStartTick());
        assertEquals(480, segments.get(0).getEndTick());

        assertEquals(1, segments.get(1).getMeasureIndex());
        assertEquals(2, segments.get(1).getStaff());
        assertEquals(0, segments.get(1).getStartDiv());
        assertEquals(1, segments.get(1).getDurDiv());
        assertEquals(480, segments.get(1).getStartTick());
        assertEquals(600, segments.get(1).getEndTick());

        assertEquals(1, segments.get(2).getMeasureIndex());
        assertEquals(1, segments.get(2).getStaff());
        assertEquals(1, segments.get(2).getStartDiv());
        assertEquals(3, segments.get(2).getDurDiv());
        assertEquals(72, segments.get(2).getMidi());
        assertEquals(90, segments.get(2).getVelocity());
    }

    @Test
    public void resolvesAndSplitsDurationNotationHelpers() {
        List<MidiIo.DurationNotation> candidates = MidiIo.durationNotationCandidates(4);
        assertEquals(21, candidates.size());
        assertEquals("whole", candidates.get(0).getType());
        assertEquals(16.0d, candidates.get(0).getDurDiv());

        MidiIo.DurationNotation dottedQuarter = MidiIo.resolveDurationNotation(6, 4);
        assertEquals("quarter", dottedQuarter.getType());
        assertEquals(1, dottedQuarter.getDots());
        assertEquals(6.0d, dottedQuarter.getDurDiv());

        List<MidiIo.DurationNotation> split = MidiIo.splitDurationNotations(5, 4);
        assertEquals(2, split.size());
        assertEquals("16th", split.get(0).getType());
        assertEquals("quarter", split.get(1).getType());
        assertEquals(Arrays.<MidiIo.DurationNotation>asList(), MidiIo.splitDurationNotations(2.5, 4));
    }

    @Test
    public void buildsDurationTieAndRestXmlHelpers() {
        MidiIo.DurationNotation dottedQuarter = MidiIo.resolveDurationNotation(6, 4);
        assertEquals("<type>quarter</type><dot/>", MidiIo.buildTypeXmlFromNotation(dottedQuarter));
        assertEquals(1, MidiIo.beamLevelFromNotationType("eighth"));
        assertEquals(4, MidiIo.beamLevelFromNotationType("64th"));
        assertEquals(0, MidiIo.beamLevelFromNotationType("quarter"));

        assertEquals("", MidiIo.buildTieXml(false, false, false));
        assertEquals("<tie type=\"stop\"/><tie type=\"start\"/><notations><articulations><staccato/></articulations><tied type=\"stop\"/><tied type=\"start\"/></notations>",
                MidiIo.buildTieXml(true, true, true));
        assertEquals("<note><rest/><duration>6</duration><type>quarter</type><dot/><voice>2</voice><staff>1</staff></note>",
                MidiIo.buildRestXml(6, 2, 1, 4));
        assertEquals("<note><rest/><duration>5.5</duration><voice>1</voice><staff>2</staff></note>",
                MidiIo.buildRestXml(5.5, 1, 2, 4));
    }

    @Test
    public void prettyPrintsXmlAndFormatsMidiHex() {
        assertEquals("0x000F", MidiIo.toHex(15, 4));
        assertEquals("0x00", MidiIo.toHex(-2, 2));
        assertEquals("<root>\n  <child>text</child>\n  <empty/>\n</root>",
                MidiIo.prettyPrintXml("<root><child>text</child><empty/></root>"));
    }

    @Test
    public void buildsMeasureMidiMetaMiscXmlSortedByStartMidiAndVoice() {
        List<MidiIo.ImportedVoiceNoteSegment> segments = Arrays.asList(
                new MidiIo.ImportedVoiceNoteSegment(0, 2, 1, 4, 2, 65, 90, 1, 3, 480, 720),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 2, 0, 4, 60, 80, 0, 1, 0, 480));

        assertEquals("<attributes><miscellaneous>"
                + "<miscellaneous-field name=\"mks:dbg:midi:meta:count\">0x0002</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:dbg:midi:meta:0001\">idx=0x0000;tr=0x00;ch=0x01;v=0x01;stf=0x02;key=0x3C;vel=0x50;sd=0x0000;dd=0x0004;tk0=0x000000;tk1=0x0001E0</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:dbg:midi:meta:0002\">idx=0x0001;tr=0x01;ch=0x03;v=0x02;stf=0x01;key=0x41;vel=0x5A;sd=0x0004;dd=0x0002;tk0=0x0001E0;tk1=0x0002D0</miscellaneous-field>"
                + "</miscellaneous></attributes>", MidiIo.buildMeasureMidiMetaMiscXml(segments));
        assertEquals("", MidiIo.buildMeasureMidiMetaMiscXml(Arrays.<MidiIo.ImportedVoiceNoteSegment>asList()));
    }

    @Test
    public void buildsMidiSourceAndSysExMiscXml() {
        assertEquals("<attributes><miscellaneous>"
                + "<miscellaneous-field name=\"mks:src:midi:raw-encoding\">hex-v1</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:src:midi:raw-bytes\">4</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:src:midi:raw-hex-length\">8</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:src:midi:raw-chunks\">1</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:src:midi:raw-truncated\">0</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:src:midi:raw-0001\">0041FF7F</miscellaneous-field>"
                + "</miscellaneous></attributes>",
                MidiIo.buildMidiSourceMiscXml(new byte[] { 0, 0x41, (byte) 0xff, 0x7f }));
        assertEquals("", MidiIo.buildMidiSourceMiscXml(new byte[0]));

        assertEquals("<attributes><miscellaneous>"
                + "<miscellaneous-field name=\"mks:meta:midi:sysex:count\">0x0003</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:meta:midi:sysex:0001\">schema=mks&lt;1&gt;</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:meta:midi:sysex:0002\">tpq=480</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:meta:midi:sysex:0003\">note without equals</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:meta:midi:sysex:schema\">mks&lt;1&gt;</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:meta:midi:sysex:tpq\">480</miscellaneous-field>"
                + "</miscellaneous></attributes>",
                MidiIo.buildMidiSysExMiscXml(Arrays.asList(" schema=mks<1>\n tpq=480 ", "note without equals")));
        assertEquals("", MidiIo.buildMidiSysExMiscXml(Arrays.asList("  ")));
    }

    @Test
    public void buildsMidiDiagnosticMiscXml() {
        assertEquals("<attributes><miscellaneous>"
                + "<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0001\">level=warn;code=MIDI&lt;WARN&gt;;fmt=midi;message=Use &amp; escape</miscellaneous-field>"
                + "</miscellaneous></attributes>",
                MidiIo.buildMidiDiagMiscXml(Arrays.asList(
                        new MidiIo.MidiImportDiagnostic("MIDI<WARN>", "Use & escape"))));
        assertEquals("", MidiIo.buildMidiDiagMiscXml(Arrays.<MidiIo.MidiImportDiagnostic>asList()));
    }

    @Test
    public void buildsPitchedMeasureVoiceXmlWithRestsBeamsAndAccidentals() {
        List<MidiIo.ImportedVoiceNoteSegment> segments = Arrays.asList(
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 2, 2, 64, 90, 0, 1, 240, 480),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 4, 4, 61, 90, 0, 1, 480, 960));

        assertEquals("<note><rest/><duration>2</duration><type>eighth</type><voice>1</voice><staff>1</staff></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>2</duration><type>eighth</type><voice>1</voice><staff>1</staff></note>"
                + "<note><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><accidental>sharp</accidental><duration>4</duration><type>quarter</type><voice>1</voice><staff>1</staff></note>",
                MidiIo.buildMeasureVoiceXml(segments, 1, 1, 1, 8, 4, false, 4, 0));

        assertEquals("<note><rest/><duration>8</duration><type>half</type><voice>2</voice><staff>1</staff></note>",
                MidiIo.buildMeasureVoiceXml(segments, 2, 1, 1, 8, 4, false, 4, 0));
    }

    @Test
    public void buildsDrumMeasureVoiceXmlWithChordNoteheads() {
        List<MidiIo.ImportedVoiceNoteSegment> segments = Arrays.asList(
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 0, 2, 35, 100, 0, 10, 0, 240),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 0, 2, 38, 100, 0, 10, 0, 240),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 2, 2, 42, 90, 0, 10, 240, 480));

        assertEquals("<note><unpitched><display-step>B</display-step><display-octave>1</display-octave></unpitched><duration>2</duration><type>eighth</type><voice>1</voice><beam number=\"1\">begin</beam><staff>1</staff><notehead>x</notehead></note>"
                + "<note><chord/><unpitched><display-step>D</display-step><display-octave>2</display-octave></unpitched><duration>2</duration><type>eighth</type><voice>1</voice><staff>1</staff><notehead>x</notehead></note>"
                + "<note><unpitched><display-step>F</display-step><display-octave>2</display-octave></unpitched><duration>2</duration><type>eighth</type><voice>1</voice><beam number=\"1\">end</beam><staff>1</staff><notehead>x</notehead></note>"
                + "<note><rest/><duration>4</duration><type>quarter</type><voice>1</voice><staff>1</staff></note>",
                MidiIo.buildMeasureVoiceXml(segments, 1, 1, 1, 8, 4, true, 4, 0));
    }

    @Test
    public void buildsMidiPartLaneDefsForDrumSingleAndGrandStaff() {
        List<MidiIo.ImportedVoiceNoteSegment> segments = Arrays.asList(
                new MidiIo.ImportedVoiceNoteSegment(0, 2, 1, 0, 2, 72, 90, 0, 1, 0, 240),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 2, 0, 2, 48, 80, 0, 1, 0, 240),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 2, 2, 76, 88, 0, 1, 240, 480));

        List<MidiIo.MidiPartLaneDef> grand = MidiIo.buildMidiPartLaneDefs(segments, false, true);
        assertEquals(3, grand.size());
        assertEquals(1, grand.get(0).getSourceStaff());
        assertEquals(1, grand.get(0).getVoice());
        assertEquals(1, grand.get(0).getOutputStaff());
        assertEquals(1, grand.get(1).getSourceStaff());
        assertEquals(2, grand.get(1).getVoice());
        assertEquals(2, grand.get(1).getOutputStaff());
        assertEquals(2, grand.get(2).getSourceStaff());
        assertEquals(1, grand.get(2).getVoice());
        assertEquals(3, grand.get(2).getOutputStaff());

        List<MidiIo.MidiPartLaneDef> single = MidiIo.buildMidiPartLaneDefs(segments, false, false);
        assertEquals(2, single.size());
        assertEquals(1, single.get(0).getSourceStaff());
        assertEquals(1, single.get(0).getVoice());
        assertEquals(1, single.get(0).getOutputStaff());
        assertEquals(1, single.get(1).getSourceStaff());
        assertEquals(2, single.get(1).getVoice());
        assertEquals(1, single.get(1).getOutputStaff());

        List<MidiIo.MidiPartLaneDef> drum = MidiIo.buildMidiPartLaneDefs(Arrays.asList(
                new MidiIo.ImportedVoiceNoteSegment(0, 3, 1, 0, 2, 42, 90, 0, 10, 0, 240),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 2, 2, 38, 90, 0, 10, 240, 480)), true,
                false);
        assertEquals(2, drum.size());
        assertEquals(1, drum.get(0).getVoice());
        assertEquals(1, drum.get(0).getOutputStaff());
        assertEquals(3, drum.get(1).getVoice());
        assertEquals(2, drum.get(1).getOutputStaff());

        List<MidiIo.MidiPartLaneDef> fallback = MidiIo.buildMidiPartLaneDefs(Arrays.<MidiIo.ImportedVoiceNoteSegment>asList(),
                false, true);
        assertEquals(2, fallback.size());
        assertEquals(1, fallback.get(0).getSourceStaff());
        assertEquals(2, fallback.get(1).getSourceStaff());
    }

    @Test
    public void buildsMidiPartDefsAndPartListXml() {
        List<MidiIo.MidiTrackChannelGroup> groups = Arrays.asList(
                new MidiIo.MidiTrackChannelGroup(0, 1),
                new MidiIo.MidiTrackChannelGroup(0, 2),
                new MidiIo.MidiTrackChannelGroup(1, 10));
        Map<Integer, String> trackNameByIndex = new LinkedHashMap<Integer, String>();
        trackNameByIndex.put(Integer.valueOf(0), "Piano");
        trackNameByIndex.put(Integer.valueOf(1), "Track 2");
        MidiIo.MksMidiTextMetadata metadata = MidiIo
                .parseMksMidiTextMetadata(Arrays.asList("mks:part-name-track:1:Kit%20%26%20Perc"));
        Map<String, Integer> programByTrackChannel = new LinkedHashMap<String, Integer>();
        programByTrackChannel.put("0:1", Integer.valueOf(5));
        programByTrackChannel.put("0:2", Integer.valueOf(999));
        programByTrackChannel.put("1:10", Integer.valueOf(33));

        List<MidiIo.MidiPartDef> partDefs = MidiIo.buildMidiPartDefs(groups, trackNameByIndex, metadata,
                programByTrackChannel);

        assertEquals(3, partDefs.size());
        assertEquals("P1", partDefs.get(0).getPartId());
        assertEquals("Piano Ch 1", partDefs.get(0).getName());
        assertEquals(1, partDefs.get(0).getChannel());
        assertEquals(5, partDefs.get(0).getProgram());
        assertEquals("0:1", partDefs.get(0).getKey());
        assertEquals("Piano Ch 2", partDefs.get(1).getName());
        assertEquals(1, partDefs.get(1).getProgram());
        assertEquals("Kit & Perc", partDefs.get(2).getName());
        assertEquals(10, partDefs.get(2).getChannel());
        assertEquals(33, partDefs.get(2).getProgram());

        assertEquals("<score-part id=\"P1\"><part-name>Piano Ch 1</part-name><midi-instrument id=\"P1-I1\"><midi-channel>1</midi-channel><midi-program>5</midi-program></midi-instrument></score-part>"
                + "<score-part id=\"P2\"><part-name>Piano Ch 2</part-name><midi-instrument id=\"P2-I1\"><midi-channel>2</midi-channel><midi-program>1</midi-program></midi-instrument></score-part>"
                + "<score-part id=\"P3\"><part-name>Kit &amp; Perc</part-name><midi-instrument id=\"P3-I1\"><midi-channel>10</midi-channel><midi-program>33</midi-program></midi-instrument></score-part>",
                MidiIo.buildMidiPartListXml(partDefs));
    }

    @Test
    public void buildsFallbackMidiPartDefForEmptyInput() {
        List<MidiIo.MidiPartDef> partDefs = MidiIo.buildMidiPartDefs(Arrays.<MidiIo.MidiTrackChannelGroup>asList(),
                null, null, null);

        assertEquals(1, partDefs.size());
        assertEquals("P1", partDefs.get(0).getPartId());
        assertEquals("Part 1", partDefs.get(0).getName());
        assertEquals(1, partDefs.get(0).getChannel());
        assertEquals(1, partDefs.get(0).getProgram());
        assertEquals("0:1", partDefs.get(0).getKey());
        assertEquals("<score-part id=\"P1\"><part-name>Part 1</part-name><midi-instrument id=\"P1-I1\"><midi-channel>1</midi-channel><midi-program>1</midi-program></midi-instrument></score-part>",
                MidiIo.buildMidiPartListXml(partDefs));
    }

    @Test
    public void buildsMidiImportSkeletonDocumentXml() {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>Title &amp; &lt;One&gt;</work-title></work>"
                + "<movement-title> II &lt;fast&gt; </movement-title>"
                + "<identification><creator type=\"composer\"> A &amp; B </creator></identification>"
                + "<part-list><score-part id=\"P1\"/></part-list><part id=\"P1\"/></score-partwise>",
                MidiIo.buildMidiImportSkeletonDocumentXml("Title & <One>", " II <fast> ", " A & B ",
                        "<score-part id=\"P1\"/>", "<part id=\"P1\"/>"));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title></work-title></work><part-list></part-list></score-partwise>",
                MidiIo.buildMidiImportSkeletonDocumentXml(null, "  ", "", null, null));
    }

    @Test
    public void buildsMidiImportedPartXmlWithMeasureContent() {
        List<MidiIo.ImportedQuantizedNote> notes = Arrays.asList(
                new MidiIo.ImportedQuantizedNote(0, 1, 60, 0, 240, 84),
                new MidiIo.ImportedQuantizedNote(0, 1, 64, 240, 480, 96));
        Map<Integer, List<MidiIo.MidiTempoMeasureEvent>> tempoEventsByMeasure =
                new LinkedHashMap<Integer, List<MidiIo.MidiTempoMeasureEvent>>();
        tempoEventsByMeasure.put(Integer.valueOf(0),
                Arrays.asList(new MidiIo.MidiTempoMeasureEvent(0, 132)));

        String xml = MidiIo.buildMidiImportedPartXml("P1", "Piano", 4, 4, 4, 0, "major", false, notes,
                tempoEventsByMeasure, true, 480, 0, new ArrayList<MidiIo.MidiImportDiagnostic>(), true, "", "");

        assertEquals(true, xml.startsWith("<part id=\"P1\"><measure number=\"1\">"));
        assertEquals(true, xml.contains("<divisions>4</divisions>"));
        assertEquals(true, xml.contains("<sound tempo=\"132\"/>"));
        assertEquals(true, xml.contains("<pitch><step>C</step><octave>4</octave></pitch>"));
        assertEquals(true, xml.contains("<pitch><step>E</step><octave>4</octave></pitch>"));
        assertEquals(true, xml.endsWith("</part>"));
    }

    @Test
    public void buildsImportSkeletonMusicXmlFromMidiImportPieces() {
        Map<String, List<MidiIo.ImportedQuantizedNote>> notesByTrackChannel =
                new LinkedHashMap<String, List<MidiIo.ImportedQuantizedNote>>();
        notesByTrackChannel.put("0:1", Arrays.asList(
                new MidiIo.ImportedQuantizedNote(0, 1, 60, 0, 240, 84)));
        Map<String, Integer> programByTrackChannel = new LinkedHashMap<String, Integer>();
        programByTrackChannel.put("0:1", Integer.valueOf(5));
        Map<Integer, String> trackNameByIndex = new LinkedHashMap<Integer, String>();
        trackNameByIndex.put(Integer.valueOf(0), "Lead & Keys");

        String xml = MidiIo.buildImportSkeletonMusicXml("Song", "Move", "Composer", "1/16", null, 480, 4, 4,
                0, "major", Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)), 0,
                Arrays.asList(new MidiIo.MidiTrackChannelGroup(0, 1)), notesByTrackChannel,
                programByTrackChannel, new ArrayList<MidiIo.MidiImportDiagnostic>(), true, "", "",
                trackNameByIndex, null);

        assertEquals(true, xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertEquals(true, xml.contains("<movement-title>Move</movement-title>"));
        assertEquals(true, xml.contains("<creator type=\"composer\">Composer</creator>"));
        assertEquals(true, xml.contains("<part-name>Lead &amp; Keys</part-name>"));
        assertEquals(true, xml.contains("<midi-program>5</midi-program>"));
        assertEquals(true, xml.contains("<part id=\"P1\"><measure number=\"1\">"));
        assertEquals(true, xml.contains("<pitch><step>C</step><octave>4</octave></pitch>"));
    }

    @Test
    public void convertsMidiToMusicXmlUsingImportedSkeletonFacade() {
        List<MidiIo.RawMidiPlaybackEvent> events = Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 240, 1, 84, "P1", "Lead"));
        MidiIo.MidiExportPlaybackPreparation preparation = MidiIo.prepareMidiExportPlayback(events, 120,
                "electric_piano_2", Collections.<MidiIo.RawMidiControlEvent>emptyList(),
                Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 4, 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, 0, "major")),
                false, false, 480, Collections.<String>emptyList(), false, "Imported Song", "", "", 0);
        byte[] midi = MidiIo.buildRawMidiBytesForPlayback(preparation, Collections.<String, Integer>emptyMap(),
                Collections.<MidiIo.RawMidiControlEvent>emptyList(), "off_before_on");

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("1/16", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, "Fallback"));

        assertEquals(true, result.isOk());
        assertEquals(0, result.getDiagnostics().size());
        assertEquals(true, result.getXml().contains("<work-title>Imported Song</work-title>"));
        assertEquals(true, result.getXml().contains("<part-name>Lead</part-name>"));
        assertEquals(true, result.getXml().contains("<pitch>"));
        assertEquals(true, result.getXml().contains("<step>C</step>"));
        assertEquals(true, result.getXml().contains("<octave>4</octave>"));
    }

    @Test
    public void restoresTitleComposerAndPartNameFromMksTextMetaOnMidiImport() {
        byte[] midi = buildMidiImportFixture(Arrays.asList(
                "mks:meta-version:1",
                "mks:title:Roundtrip%20Title",
                "mks:composer:Roundtrip%20Composer",
                "mks:part-name-track:1:Violin%20Solo"), "Track 1", 60, 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("Roundtrip Title", doc.getElementsByTagName("work-title").item(0).getTextContent().trim());
        assertEquals("Roundtrip Composer", doc.getElementsByTagName("creator").item(0).getTextContent().trim());
        assertEquals("Violin Solo", doc.getElementsByTagName("part-name").item(0).getTextContent().trim());
    }

    @Test
    public void prefersStandardMidiMetaTitleAndComposerOverMksTextMetaOnMidiImport() {
        byte[] midi = buildMidiImportFixture(Arrays.asList(
                "title:Concert Overture",
                "composer:Standard Composer",
                "mks:meta-version:1",
                "mks:title:Roundtrip%20Title",
                "mks:composer:Roundtrip%20Composer"), "Track 1", 60, 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("Concert Overture", doc.getElementsByTagName("work-title").item(0).getTextContent().trim());
        assertEquals("Standard Composer", doc.getElementsByTagName("creator").item(0).getTextContent().trim());
    }

    @Test
    public void prefersExplicitTrackNameOverMksPartNameTrackOnMidiImport() {
        byte[] midi = buildMidiImportFixture(Arrays.asList(
                "mks:meta-version:1",
                "mks:part-name-track:1:Viola"), "Solo Violin", 60, 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("Solo Violin", doc.getElementsByTagName("part-name").item(0).getTextContent().trim());
    }

    @Test
    public void usesAltoClefWhenImportedMidiPartNameIncludesViola() {
        byte[] midi = buildMidiImportFixture(Arrays.asList(
                "mks:meta-version:1",
                "mks:part-name-track:1:Viola"), "Track 1", 60, 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("C", doc.getElementsByTagName("sign").item(0).getTextContent().trim());
        assertEquals("3", doc.getElementsByTagName("line").item(0).getTextContent().trim());
    }

    @Test
    public void keepsSingleStaffAltoClefForViolaEvenWithWideMidiPitchRange() {
        List<byte[]> chunks = new ArrayList<byte[]>();
        chunks.add(MidiIo.buildRawMidiTempoTrackChunk(Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                new MidiIo.RawMidiTempoTrackOptions(false, Collections.<String>emptyList(),
                        Arrays.asList("mks:meta-version:1", "mks:part-name-track:1:Viola"), "Meta")));
        chunks.addAll(MidiIo.buildRawMidiNoteTrackChunks(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(48, 0, 480, 1, 100, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(76, 480, 480, 1, 100, "P1", "Track 1")),
                Collections.<String, Integer>emptyMap(), "acoustic_grand_piano", "off_before_on"));
        byte[] midi = MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(0, doc.getElementsByTagName("staves").getLength());
        assertEquals("C", doc.getElementsByTagName("sign").item(0).getTextContent().trim());
        assertEquals("3", doc.getElementsByTagName("line").item(0).getTextContent().trim());
    }

    @Test
    public void doesNotInferStaccatoFromDetachedMidiImportNotes() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(62, 240, 120, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 480, 120, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("1/16", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        List<Element> pitchedNotes = pitchedNoteElements(MusicXmlIo.parseMusicXmlDocument(result.getXml()));

        assertEquals(true, result.isOk());
        assertEquals(3, pitchedNotes.size());
        assertEquals(0, pitchedNotes.get(0).getElementsByTagName("staccato").getLength());
        assertEquals(0, pitchedNotes.get(1).getElementsByTagName("staccato").getLength());
        assertEquals(0, pitchedNotes.get(2).getElementsByTagName("staccato").getLength());
    }

    @Test
    public void appliesBeamTagsToGroupedShortMidiImportNotesAndBreaksAcrossRests() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 240, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(62, 240, 240, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 720, 240, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("1/8", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        List<Element> pitchedNotes = pitchedNoteElements(MusicXmlIo.parseMusicXmlDocument(result.getXml()));

        assertEquals(true, result.isOk());
        assertEquals(3, pitchedNotes.size());
        assertEquals("begin", pitchedNotes.get(0).getElementsByTagName("beam").item(0).getTextContent().trim());
        assertEquals("end", pitchedNotes.get(1).getElementsByTagName("beam").item(0).getTextContent().trim());
        assertEquals(0, pitchedNotes.get(2).getElementsByTagName("beam").getLength());
    }

    @Test
    public void splitsImplicitBeamsAtBeatBoundariesOnMidiImport() {
        List<byte[]> chunks = new ArrayList<byte[]>();
        chunks.add(MidiIo.buildRawMidiTempoTrackChunk(Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 2, 4)),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                new MidiIo.RawMidiTempoTrackOptions(false, Collections.<String>emptyList(),
                        Collections.<String>emptyList(), "Meta")));
        chunks.addAll(MidiIo.buildRawMidiNoteTrackChunks(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 240, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(62, 240, 240, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 480, 240, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(65, 720, 240, 1, 96, "P1", "Track 1")),
                Collections.<String, Integer>emptyMap(), "acoustic_grand_piano", "off_before_on"));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(
                MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480),
                new MidiIo.MidiImportOptions("1/8", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        List<Element> pitchedNotes = pitchedNoteElements(MusicXmlIo.parseMusicXmlDocument(result.getXml()));

        assertEquals(true, result.isOk());
        assertEquals(4, pitchedNotes.size());
        assertEquals("begin", pitchedNotes.get(0).getElementsByTagName("beam").item(0).getTextContent().trim());
        assertEquals("end", pitchedNotes.get(1).getElementsByTagName("beam").item(0).getTextContent().trim());
        assertEquals("begin", pitchedNotes.get(2).getElementsByTagName("beam").item(0).getTextContent().trim());
        assertEquals("end", pitchedNotes.get(3).getElementsByTagName("beam").item(0).getTextContent().trim());
    }

    @Test
    public void keepsSamePitchRetriggerStableWhenNoteOnAppearsBeforeNoteOffAtSameTick() {
        List<MidiIo.RawMidiPlaybackEvent> events = Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 120, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(60, 120, 120, 1, 96, "P1", "Track 1"));
        byte[] offThenOn = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", events,
                "off_before_on");
        byte[] onThenOff = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", events,
                "on_before_off");

        MidiIo.MidiImportResult offThenOnResult = MidiIo.convertMidiToMusicXml(offThenOn,
                new MidiIo.MidiImportOptions("1/16", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        MidiIo.MidiImportResult onThenOffResult = MidiIo.convertMidiToMusicXml(onThenOff,
                new MidiIo.MidiImportOptions("1/16", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));

        assertEquals(true, offThenOnResult.isOk());
        assertEquals(true, onThenOffResult.isOk());
        assertEquals(pitchedDurations(offThenOnResult.getXml()), pitchedDurations(onThenOffResult.getXml()));
        assertEquals(false, hasWarningCode(onThenOffResult, "MIDI_NOTE_PAIR_BROKEN"));
    }

    @Test
    public void autoSplitsOverlappingMidiImportNotesIntoMultipleVoices() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 480, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 120, 480, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("1/16", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(true, distinctVoiceCount(doc) >= 2);
        assertEquals(true, hasWarningCode(result, "MIDI_POLYPHONY_VOICE_ASSIGNED"));
    }

    @Test
    public void separatesSameMidiChannelAcrossDifferentTracksIntoSeparateParts() {
        List<byte[]> chunks = new ArrayList<byte[]>();
        chunks.add(MidiIo.buildRawMidiTempoTrackChunk(Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                new MidiIo.RawMidiTempoTrackOptions(false, Collections.<String>emptyList(),
                        Collections.<String>emptyList(), "Meta")));
        chunks.addAll(MidiIo.buildRawMidiNoteTrackChunks(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 480, 1, 96, "P1", ""),
                new MidiIo.RawMidiPlaybackEvent(64, 0, 480, 1, 96, "P2", "")),
                Collections.<String, Integer>emptyMap(), "acoustic_grand_piano", "off_before_on"));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(
                MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480), null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(2, doc.getElementsByTagName("part").getLength());
        assertEquals(2, doc.getElementsByTagName("score-part").getLength());
    }

    @Test
    public void separatesChannelTenIntoDedicatedDrumPartOnMidiImport() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(36, 0, 240, 10, 100, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(true, firstPartName(doc).startsWith("Drums"));
        assertEquals(true, hasWarningCode(result, "MIDI_DRUM_CHANNEL_SEPARATED"));
        assertEquals(1, doc.getElementsByTagName("unpitched").getLength());
    }

    @Test
    public void doesNotCreateEmptyPartsFromMidiChannelsWithoutNotes() {
        byte[] midi = MidiIo.buildRawMidiBytesFromTrackChunks(Arrays.asList(rawMidiTrackChunk(
                0x00, 0xc0, 0x00,
                0x00, 0xc1, 0x28,
                0x00, 0x90, 0x3c, 0x60,
                0x83, 0x60, 0x80, 0x3c, 0x00,
                0x00, 0xff, 0x2f, 0x00)), 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(1, doc.getElementsByTagName("part").getLength());
        assertEquals(1, doc.getElementsByTagName("score-part").getLength());
    }

    @Test
    public void reflectsCc11ExpressionInImportedDynamicsEstimation() {
        byte[] midi = MidiIo.buildRawMidiBytesFromTrackChunks(Arrays.asList(rawMidiTrackChunk(
                0x00, 0x90, 0x3c, 0x64,
                0x83, 0x60, 0x80, 0x3c, 0x00,
                0x00, 0xb0, 0x0b, 0x14,
                0x00, 0x90, 0x3e, 0x64,
                0x83, 0x60, 0x80, 0x3e, 0x00,
                0x00, 0xff, 0x2f, 0x00)), 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        List<String> dynamics = dynamicTagNames(MusicXmlIo.parseMusicXmlDocument(result.getXml()));

        assertEquals(true, result.isOk());
        assertTrue(dynamics.contains("ff"));
        assertTrue(dynamics.contains("pp"));
    }

    @Test
    public void prettyPrintsImportedMusicXmlWhenDebugMetadataIsEnabled() {
        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(buildFormat0SingleNoteMidi(96),
                new MidiIo.MidiImportOptions(null, Boolean.TRUE, null, null, null));

        assertEquals(true, result.isOk());
        assertTrue(result.getXml().contains("\n"));
        assertTrue(result.getXml().contains("  <part-list>"));
    }

    @Test
    public void readsMidiKeySignatureMetaEventIntoMusicXmlKey() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1",
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Arrays.asList(new MidiIo.MidiTickKeySignatureEvent(0, -3, "minor")),
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(69, 0, 480, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("-3", doc.getElementsByTagName("fifths").item(0).getTextContent().trim());
        assertEquals("minor", doc.getElementsByTagName("mode").item(0).getTextContent().trim());
    }

    @Test
    public void normalizesLeadingPickupTimeSignatureMetaEventsOnMidiImport() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1",
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 1, 8),
                        new MidiIo.MidiTimeSignatureEvent(240, 3, 8)),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(69, 0, 240, 1, 96, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(71, 240, 480, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("3", doc.getElementsByTagName("beats").item(0).getTextContent().trim());
        assertEquals("8", doc.getElementsByTagName("beat-type").item(0).getTextContent().trim());
        assertEquals(true, hasWarningCode(result, "MIDI_TIME_SIGNATURE_PICKUP_NORMALIZED"));
    }

    @Test
    public void usesTripletAwareDivisionsForTripletLikeMidiImportTiming() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 160, 1, 100, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(62, 160, 160, 1, 100, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 320, 160, 1, 100, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("1/16", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("12", doc.getElementsByTagName("divisions").item(0).getTextContent().trim());
        assertEquals(true, elementTextValues(doc, "duration").contains("4"));
    }

    @Test
    public void choosesEighthGridOnAutoModeForStraightEighthMidiImportTiming() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 240, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(62, 240, 240, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 480, 240, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("auto", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("2", doc.getElementsByTagName("divisions").item(0).getTextContent().trim());
    }

    @Test
    public void keepsTripletAwareGridOnAutoModeWhenTripletLikeTimingDominates() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 160, 1, 100, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(62, 160, 160, 1, 100, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 320, 160, 1, 100, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("auto", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ""));
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("12", doc.getElementsByTagName("divisions").item(0).getTextContent().trim());
    }

    @Test
    public void restoresPickupMeasureFromMksPickupTicksWhenFf58PreludeIsAbsent() {
        byte[] midi = buildMidiImportFixture(Arrays.asList("mks:pickup-ticks:240"), "Track 1",
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 6, 8)),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(76, 0, 120, 1, 96, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(75, 120, 120, 1, 96, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(76, 240, 120, 1, 96, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(71, 360, 120, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());
        Element firstMeasure = (Element) doc.getElementsByTagName("measure").item(0);

        assertEquals(true, result.isOk());
        assertEquals("yes", firstMeasure.getAttribute("implicit"));
        assertEquals("6", doc.getElementsByTagName("beats").item(0).getTextContent().trim());
        assertEquals("8", doc.getElementsByTagName("beat-type").item(0).getTextContent().trim());
    }

    @Test
    public void infersMusicXmlKeyWhenMidiKeySignatureMetaEventIsMissing() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(62, 0, 480, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(66, 480, 480, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(69, 960, 480, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(74, 1440, 480, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("2", doc.getElementsByTagName("fifths").item(0).getTextContent().trim());
        assertEquals("major", doc.getElementsByTagName("mode").item(0).getTextContent().trim());
        assertEquals(true, hasWarningCode(result, "MIDI_KEY_SIGNATURE_INFERRED"));
    }

    @Test
    public void emitsNaturalAccidentalWhenMidiNoteContradictsKeySignature() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1",
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Arrays.asList(new MidiIo.MidiTickKeySignatureEvent(0, 1, "major")),
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(65, 0, 480, 1, 100, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("natural", doc.getElementsByTagName("accidental").item(0).getTextContent().trim());
    }

    @Test
    public void prefersSharpLowerChromaticNeighborBetweenRepeatedMidiDNotes() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1",
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Arrays.asList(new MidiIo.MidiTickKeySignatureEvent(0, -1, "major")),
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(62, 0, 480, 1, 90, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(61, 480, 480, 1, 90, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(62, 960, 480, 1, 90, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        List<Element> notes = pitchedNoteElements(MusicXmlIo.parseMusicXmlDocument(result.getXml()));
        Element middlePitch = (Element) notes.get(1).getElementsByTagName("pitch").item(0);

        assertEquals(true, result.isOk());
        assertTrue(notes.size() >= 3);
        assertEquals("C", middlePitch.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("1", middlePitch.getElementsByTagName("alter").item(0).getTextContent().trim());
    }

    @Test
    public void keepsUpperStaffHysteresisAroundGrandStaffSplitBoundaryOnMidiImport() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(64, 0, 480, 1, 100, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(59, 480, 480, 1, 100, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(54, 960, 480, 1, 100, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());
        Element b3Note = firstPitchedNote(doc, "B", "", "3");
        Element fs3Note = firstPitchedNote(doc, "F", "1", "3");

        assertEquals(true, result.isOk());
        assertEquals("2", doc.getElementsByTagName("staves").item(0).getTextContent().trim());
        assertEquals("G", clefSign(doc, "1"));
        assertEquals("F", clefSign(doc, "2"));
        assertEquals("1", childText(b3Note, "staff"));
        assertEquals("2", childText(fs3Note, "staff"));
    }

    @Test
    public void doesNotEmitPhantomEmptyStaffWhenMidiMelodyStaysOnOneSide() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(72, 0, 480, 1, 100, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(0, doc.getElementsByTagName("staves").getLength());
        assertEquals("G", doc.getElementsByTagName("sign").item(0).getTextContent().trim());
        for (Element note : pitchedNoteElements(doc)) {
            assertEquals(false, "2".equals(childText(note, "staff")));
        }
    }

    @Test
    public void doesNotEmitFullRestOnlyInactiveVoiceInMeasureThatAlreadyHasNotesOnMidiImport() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1",
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, 1, 4)),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(60, 0, 480, 1, 100, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(64, 120, 240, 1, 100, "P1", "Track 1"),
                        new MidiIo.RawMidiPlaybackEvent(67, 480, 240, 1, 100, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());
        Element measure2 = measureByNumber(doc, "2");

        assertEquals(true, result.isOk());
        assertEquals(0, voiceNoteCount(measure2, "2"));
    }

    @Test
    public void readsMidiTempoMetaEventIntoMusicXmlDirectionSoundTempo() {
        List<byte[]> chunks = new ArrayList<byte[]>();
        chunks.add(MidiIo.buildRawMidiTempoTrackChunk(Arrays.asList(new MidiIo.MidiTempoEvent(0, 100)),
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                new MidiIo.RawMidiTempoTrackOptions(false, Collections.<String>emptyList(),
                        Collections.<String>emptyList(), "Meta")));
        chunks.addAll(MidiIo.buildRawMidiNoteTrackChunks(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 480, 1, 96, "P1", "Track 1")),
                Collections.<String, Integer>emptyMap(), "acoustic_grand_piano", "off_before_on"));
        byte[] midi = MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480);

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());
        List<MidiIo.MidiTempoEvent> tempoEvents = MidiIo.collectMidiTempoEventsFromMusicXmlDoc(doc, 128);

        assertEquals(true, result.isOk());
        assertEquals("100", doc.getElementsByTagName("sound").item(0).getAttributes()
                .getNamedItem("tempo").getTextContent().trim());
        assertEquals("100", doc.getElementsByTagName("per-minute").item(0).getTextContent().trim());
        assertEquals(100, tempoEvents.get(0).getBpm());
    }

    @Test
    public void mapsMidiImportVelocityToDynamicsAndSuppressesRepeatedDynamics() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 480, 1, 16, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(62, 480, 480, 1, 16, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 960, 480, 1, 79, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        List<String> dynamics = dynamicTagNames(MusicXmlIo.parseMusicXmlDocument(result.getXml()));

        assertEquals(true, result.isOk());
        assertTrue(dynamics.contains("pp"));
        assertTrue(dynamics.contains("ff"));
        assertEquals(1, countString(dynamics, "pp"));
    }

    @Test
    public void splitsNonNotatableMidiImportDurationsIntoTiedTypedNotes() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1",
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(60, 0, 1200, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi, null);
        List<Element> pitchedNotes = pitchedNoteElements(MusicXmlIo.parseMusicXmlDocument(result.getXml()));

        assertEquals(true, result.isOk());
        assertTrue(pitchedNotes.size() > 1);
        assertEquals(true, allPitchedNotesHaveChild(pitchedNotes, "type"));
        assertEquals(true, hasTieType(pitchedNotes, "start"));
        assertEquals(true, hasTieType(pitchedNotes, "stop"));
        assertEquals(false, elementTextValues(MusicXmlIo.parseMusicXmlDocument(result.getXml()), "duration")
                .contains("10"));
    }

    @Test
    public void writesMidiMetaMetadataIntoMiscellaneousFieldByDefault() {
        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(buildFormat0SingleNoteMidi(96), null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());
        List<Element> metaFields = miscFieldsWithNamePrefix(doc, "mks:dbg:midi:meta");
        String firstPayload = miscFieldTextByName(doc, "mks:dbg:midi:meta:0001");

        assertEquals(true, result.isOk());
        assertTrue(metaFields.size() > 0);
        assertEquals(true, firstPayload.contains("key=0x3C"));
        assertEquals(true, firstPayload.contains("vel=0x60"));
    }

    @Test
    public void writesMidiRawSourceMetadataByDefault() {
        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(buildFormat0SingleNoteMidi(96), null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("hex-v1", miscFieldTextByName(doc, "mks:src:midi:raw-encoding"));
        assertEquals(true, miscFieldTextByName(doc, "mks:src:midi:raw-0001").matches("^[0-9A-F]+$"));
    }

    @Test
    public void readsMikuscoreSysExMetadataIntoMiscellaneousFieldsOnMidiImport() {
        String payloadText = "mks|v=1|m=0001|i=0001|n=0001|d="
                + "schema%3Dmks-sysex-v1%0Aapp%3Dmikuscore%0Asource%3Dmusicxml";
        List<byte[]> chunks = new ArrayList<byte[]>();
        chunks.add(MidiIo.buildRawMidiTempoTrackChunk(Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(),
                new MidiIo.RawMidiTempoTrackOptions(true, Arrays.asList(payloadText),
                        Collections.<String>emptyList(), "Meta")));
        chunks.addAll(MidiIo.buildRawMidiNoteTrackChunks(Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 480, 1, 76, "P1", "Track 1")),
                Collections.<String, Integer>emptyMap(), "acoustic_grand_piano", "off_before_on"));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(
                MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480), null);
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("mks-sysex-v1", miscFieldTextByName(doc, "mks:meta:midi:sysex:schema"));
        assertEquals("mikuscore", miscFieldTextByName(doc, "mks:meta:midi:sysex:app"));
    }

    @Test
    public void canDisableMidiDebugMetadataOutputOnMidiImport() {
        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(buildFormat0SingleNoteMidi(96),
                new MidiIo.MidiImportOptions(null, Boolean.FALSE, null, null, null));
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(0, miscFieldsWithNamePrefix(doc, "mks:dbg:midi:meta").size());
    }

    @Test
    public void writesMidiImportWarningsIntoDiagnosticMiscellaneousFields() {
        byte[] midi = buildMidiImportFixture(Collections.<String>emptyList(), "Track 1", Arrays.asList(
                new MidiIo.RawMidiPlaybackEvent(60, 0, 480, 1, 96, "P1", "Track 1"),
                new MidiIo.RawMidiPlaybackEvent(64, 120, 480, 1, 96, "P1", "Track 1")));

        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(midi,
                new MidiIo.MidiImportOptions("1/16", Boolean.TRUE, null, Boolean.TRUE, ""));
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals(true, hasWarningCode(result, "MIDI_POLYPHONY_VOICE_ASSIGNED"));
        assertEquals("1", miscFieldTextByName(doc, "mks:diag:count"));
        assertEquals(true, miscFieldTextByName(doc, "mks:diag:0001")
                .contains("code=MIDI_POLYPHONY_VOICE_ASSIGNED"));
    }

    @Test
    public void canDisableMidiRawSourceMetadataOutputOnMidiImport() {
        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(buildFormat0SingleNoteMidi(96),
                new MidiIo.MidiImportOptions(null, null, Boolean.FALSE, null, null));
        Document doc = MusicXmlIo.parseMusicXmlDocument(result.getXml());

        assertEquals(true, result.isOk());
        assertEquals("", miscFieldTextByName(doc, "mks:src:midi:raw-encoding"));
        assertEquals(0, miscFieldsWithNamePrefix(doc, "mks:src:midi:raw").size());
    }

    @Test
    public void rejectsEmptyMidiImportWithUpstreamDiagnostic() {
        MidiIo.MidiImportResult result = MidiIo.convertMidiToMusicXml(new byte[0], null);

        assertEquals(false, result.isOk());
        assertEquals("", result.getXml());
        assertEquals(1, result.getDiagnostics().size());
        assertEquals("MIDI_INVALID_FILE", result.getDiagnostics().get(0).getCode());
        assertEquals("MIDI input is empty.", result.getDiagnostics().get(0).getMessage());
    }

    @Test
    public void buildsPlaybackEventsFromMusicXmlBasicTimeline() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "<midi-instrument id=\"P1-I1\"><midi-channel>2</midi-channel></midi-instrument>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>2</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<direction><sound tempo=\"132\"/></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice></note>"
                + "<note><rest/><duration>2</duration><voice>1</voice></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 480);

        assertEquals(120, result.getTempo());
        assertEquals(2, result.getEvents().size());
        assertEquals(60, result.getEvents().get(0).getMidiNumber());
        assertEquals(0, result.getEvents().get(0).getStartTicks());
        assertEquals(480, result.getEvents().get(0).getDurTicks());
        assertEquals(2, result.getEvents().get(0).getChannel());
        assertEquals("P1", result.getEvents().get(0).getTrackId());
        assertEquals("Lead", result.getEvents().get(0).getTrackName());
        assertEquals(62, result.getEvents().get(1).getMidiNumber());
        assertEquals(960, result.getEvents().get(1).getStartTicks());
        assertEquals(960, result.getEvents().get(1).getDurTicks());
    }

    @Test
    public void returnsEmptyPlaybackEventsForInvalidMusicXml() {
        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml("<score-partwise>", 480);

        assertEquals(120, result.getTempo());
        assertEquals(0, result.getEvents().size());
    }

    @Test
    public void keepsFullNonImplicitMeasureLengthForPlaybackTimeline() {
        String xml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>3</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure>"
                + "<measure number=\"2\">"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure>"
                + "</part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("playback"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsSortedByStart(result);

        assertTrue(events.size() >= 2);
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(384, events.get(1).getStartTicks());
    }

    @Test
    public void doesNotDoubleCountUnderfullBarBeforeImplicitPickupInPlaybackTimeline() {
        String xml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>3</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>half</type></note></measure>"
                + "<measure number=\"X1\" implicit=\"yes\">"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure>"
                + "<measure number=\"2\">"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure>"
                + "</part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("playback"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsSortedByStart(result);

        assertTrue(events.size() >= 3);
        assertEquals(384, events.get(2).getStartTicks());
    }

    @Test
    public void keepsTimelineStableForUnderfullImplicitRegularUnderfullSequence() {
        String xml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>half</type></note></measure>"
                + "<measure number=\"X1\" implicit=\"yes\">"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure>"
                + "<measure number=\"2\">"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure>"
                + "<measure number=\"3\">"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure>"
                + "</part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("playback"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsSortedByStart(result);

        assertTrue(events.size() >= 4);
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(256, events.get(1).getStartTicks());
        assertEquals(384, events.get(2).getStartTicks());
        assertEquals(896, events.get(3).getStartTicks());
    }

    @Test
    public void keepsStableTripletEighthTimingInMusicXmlPlaybackExtraction() {
        String xml = "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>5</octave></pitch><duration>160</duration><voice>1</voice>"
                + "<type>eighth</type><time-modification><actual-notes>3</actual-notes>"
                + "<normal-notes>2</normal-notes></time-modification></note>"
                + "<note><pitch><step>D</step><octave>5</octave></pitch><duration>160</duration><voice>1</voice>"
                + "<type>eighth</type><time-modification><actual-notes>3</actual-notes>"
                + "<normal-notes>2</normal-notes></time-modification></note>"
                + "<note><pitch><step>E</step><octave>5</octave></pitch><duration>160</duration><voice>1</voice>"
                + "<type>eighth</type><time-modification><actual-notes>3</actual-notes>"
                + "<normal-notes>2</normal-notes></time-modification></note>"
                + "<note><rest/><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsSortedByStart(result);

        assertEquals(3, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        int d1 = events.get(1).getStartTicks() - events.get(0).getStartTicks();
        int d2 = events.get(2).getStartTicks() - events.get(1).getStartTicks();
        assertTrue(d1 == 42 || d1 == 43);
        assertTrue(d2 == 42 || d2 == 43);
        assertEquals(85, d1 + d2);
        assertTrue(events.get(0).getDurTicks() > 0);
        assertTrue(events.get(1).getDurTicks() > 0);
        assertTrue(events.get(2).getDurTicks() > 0);
    }

    @Test
    public void keepsNoteTimingExtractionStableWithStaccatoAccentNotations() {
        String xml = "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><staccato/></articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><accent/></articulations></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsSortedByStart(result);

        assertEquals(2, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(128, events.get(1).getStartTicks());
        assertTrue(events.get(0).getDurTicks() > 0);
        assertTrue(events.get(0).getDurTicks() <= 128);
        assertTrue(events.get(1).getDurTicks() > 0);
        assertTrue(events.get(1).getDurTicks() <= 128);
    }

    @Test
    public void doesNotDuplicateTimeSignatureEventsOnExplicitSameMeterRedeclaration() {
        Document doc = MusicXmlIo.parseMusicXmlDocument("<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"24\"><attributes><divisions>480</divisions>"
                + "<time><beats>2</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<barline location=\"right\"><bar-style>light-light</bar-style></barline></measure>"
                + "<measure number=\"25\"><attributes><time><beats>2</beats><beat-type>4</beat-type></time>"
                + "</attributes><note><pitch><step>B</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>");

        List<MidiIo.MidiTimeSignatureEvent> events = MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(doc, 128);

        assertEquals(1, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertEquals(2, events.get(0).getBeats());
        assertEquals(4, events.get(0).getBeatType());
    }

    @Test
    public void appliesDirectionDynamicsInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<direction><direction-type><dynamics><ff/></dynamics></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<direction><sound dynamics=\"50\"/></direction>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(112, midi.getEvents().get(0).getVelocity());
        assertEquals(64, midi.getEvents().get(1).getVelocity());
        assertEquals(80, playback.getEvents().get(0).getVelocity());
        assertEquals(80, playback.getEvents().get(1).getVelocity());
    }

    @Test
    public void expandsGraceNotesBeforePrincipalNotesInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><rest/><duration>1</duration><voice>1</voice></note>"
                + "<note><grace slash=\"yes\"/><pitch><step>G</step><octave>4</octave></pitch><voice>1</voice></note>"
                + "<note><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 128);

        assertEquals(2, midi.getEvents().size());
        assertEquals(67, midi.getEvents().get(0).getMidiNumber());
        assertEquals(72, midi.getEvents().get(1).getMidiNumber());
        assertTrue(midi.getEvents().get(0).getStartTicks() < midi.getEvents().get(1).getStartTicks());
        assertEquals(1, playback.getEvents().size());
        assertEquals(72, playback.getEvents().get(0).getMidiNumber());
    }

    @Test
    public void supportsOnBeatGraceTimingInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><rest/><duration>1</duration><voice>1</voice></note>"
                + "<note><grace slash=\"yes\"/><pitch><step>G</step><octave>4</octave></pitch><voice>1</voice></note>"
                + "<note><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "on_beat"));

        assertEquals(2, result.getEvents().size());
        assertEquals(128, result.getEvents().get(0).getStartTicks());
        assertTrue(result.getEvents().get(1).getStartTicks() > result.getEvents().get(0).getStartTicks());
        assertTrue(result.getEvents().get(1).getDurTicks() < 128);
    }

    @Test
    public void supportsClassicalEqualGraceTimingInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<note><grace/><pitch><step>G</step><octave>5</octave></pitch>"
                + "<voice>1</voice><type>16th</type></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "classical_equal"));
        MidiIo.RawMidiPlaybackEvent grace = playbackEventByMidiNumber(result, 79);
        MidiIo.RawMidiPlaybackEvent principal = playbackEventByMidiNumber(result, 62);

        assertTrue(grace != null);
        assertTrue(principal != null);
        assertEquals(128, grace.getStartTicks());
        assertEquals(grace.getStartTicks() + grace.getDurTicks(), principal.getStartTicks());
        assertTrue(Math.abs(grace.getDurTicks() - principal.getDurTicks()) <= 1);
    }

    @Test
    public void mergesTiedNotesIntoOneSustainedPlaybackEventInMidiMode() {
        String xml = "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type><tie type=\"start\"/>"
                + "<notations><tied type=\"start\"/></notations></note>"
                + "<note><rest/><duration>1440</duration><voice>1</voice><type>half</type><dot/></note>"
                + "</measure>"
                + "<measure number=\"2\">"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type><tie type=\"stop\"/>"
                + "<notations><tied type=\"stop\"/></notations></note>"
                + "<note><rest/><duration>1440</duration><voice>1</voice><type>half</type><dot/></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsByMidiNumberSorted(result, 60);

        assertEquals(1, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertTrue(events.get(0).getDurTicks() >= 256);
    }

    @Test
    public void mergesTiedNotesWhenContinuationOmitsVoiceByChannelPitchFallback() {
        String xml = "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>2</voice><type>quarter</type><tie type=\"start\"/>"
                + "<notations><tied type=\"start\"/></notations></note>"
                + "<backup><duration>480</duration></backup>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "</measure>"
                + "<measure number=\"2\">"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<type>quarter</type><tie type=\"stop\"/>"
                + "<notations><tied type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsByMidiNumberSorted(result, 60);

        assertEquals(1, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertTrue(events.get(0).getDurTicks() >= 256);
    }

    @Test
    public void appliesArticulationVelocityAndDurationInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><articulations><accent/><staccato/></articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><articulations><strong-accent/><staccatissimo/></articulations></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(94, midi.getEvents().get(0).getVelocity());
        assertEquals(66, midi.getEvents().get(0).getDurTicks());
        assertEquals(104, midi.getEvents().get(1).getVelocity());
        assertEquals(42, midi.getEvents().get(1).getDurTicks());
        assertEquals(80, playback.getEvents().get(0).getVelocity());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
    }

    @Test
    public void appliesTemporalExpressionAdjustmentsInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><fermata/></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(154, midi.getEvents().get(0).getDurTicks());
        assertEquals(140, midi.getEvents().get(1).getStartTicks());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
        assertEquals(120, playback.getEvents().get(1).getStartTicks());
    }

    @Test
    public void appliesMetricAccentVelocityInMidiPlaybackExtractionModeWhenEnabled() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult enabled = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "strong"));
        MidiIo.MidiPlaybackEventsResult disabled = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));

        assertEquals(86, enabled.getEvents().get(0).getVelocity());
        assertEquals(80, enabled.getEvents().get(1).getVelocity());
        assertEquals(83, enabled.getEvents().get(2).getVelocity());
        assertEquals(80, enabled.getEvents().get(3).getVelocity());
        assertEquals(80, disabled.getEvents().get(0).getVelocity());
        assertEquals(80, disabled.getEvents().get(2).getVelocity());
    }

    @Test
    public void appliesMetricAccentVelocityForSixEightAndFiveFourSignatures() {
        String sixEightXml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>6</beats><beat-type>8</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "</measure></part></score-partwise>";
        String fiveFourXml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>5</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult sixEight = MidiIo.buildPlaybackEventsFromXml(sixEightXml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "subtle"));
        MidiIo.MidiPlaybackEventsResult fiveFour = MidiIo.buildPlaybackEventsFromXml(fiveFourXml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "subtle"));

        assertEquals(Arrays.asList(Integer.valueOf(82), Integer.valueOf(80), Integer.valueOf(80),
                Integer.valueOf(81), Integer.valueOf(80), Integer.valueOf(80)),
                playbackVelocities(sixEight));
        assertEquals(Arrays.asList(Integer.valueOf(82), Integer.valueOf(80), Integer.valueOf(81),
                Integer.valueOf(80), Integer.valueOf(80)), playbackVelocities(fiveFour));
    }

    @Test
    public void appliesThreeBeatAndFallbackMetricAccentPatterns() {
        String threeThreeXml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>3</beats><beat-type>3</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>640</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>640</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>640</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";
        String sevenEightXml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>7</beats><beat-type>8</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>B</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult threeThree = MidiIo.buildPlaybackEventsFromXml(threeThreeXml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "subtle"));
        MidiIo.MidiPlaybackEventsResult sevenEight = MidiIo.buildPlaybackEventsFromXml(sevenEightXml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "subtle"));

        assertEquals(Arrays.asList(Integer.valueOf(82), Integer.valueOf(80), Integer.valueOf(80)),
                playbackVelocities(threeThree));
        assertEquals(Arrays.asList(Integer.valueOf(82), Integer.valueOf(80), Integer.valueOf(80),
                Integer.valueOf(80), Integer.valueOf(80), Integer.valueOf(80), Integer.valueOf(80)),
                playbackVelocities(sevenEight));
    }

    @Test
    public void supportsConfigurableMetricAccentAmountProfilesInPlaybackExtraction() {
        String xml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult subtle = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "subtle"));
        MidiIo.MidiPlaybackEventsResult balanced = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "balanced"));
        MidiIo.MidiPlaybackEventsResult strong = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", true, "strong"));

        assertEquals(Arrays.asList(Integer.valueOf(82), Integer.valueOf(80), Integer.valueOf(81),
                Integer.valueOf(80)), playbackVelocities(subtle));
        assertEquals(Arrays.asList(Integer.valueOf(84), Integer.valueOf(80), Integer.valueOf(82),
                Integer.valueOf(80)), playbackVelocities(balanced));
        assertEquals(Arrays.asList(Integer.valueOf(86), Integer.valueOf(80), Integer.valueOf(83),
                Integer.valueOf(80)), playbackVelocities(strong));
    }

    @Test
    public void collectsInScoreTempoChangesWithTickPositions() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise version=\"3.1\">"
                        + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                        + "<part id=\"P1\"><measure number=\"1\">"
                        + "<attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                        + "<direction><sound tempo=\"90\"/></direction>"
                        + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                        + "<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>60</per-minute></metronome></direction-type></direction>"
                        + "</measure></part></score-partwise>");

        List<MidiIo.MidiTempoEvent> tempos = MidiIo.collectMidiTempoEventsFromMusicXmlDoc(doc, 128);

        assertEquals(0, tempos.get(0).getTick());
        assertEquals(120, tempos.get(0).getBpm());
        boolean hasNinetyAfterStart = false;
        boolean hasSixtyAfterStart = false;
        for (MidiIo.MidiTempoEvent tempo : tempos) {
            hasNinetyAfterStart = hasNinetyAfterStart || tempo.getBpm() == 90 && tempo.getTick() > 0;
            hasSixtyAfterStart = hasSixtyAfterStart || tempo.getBpm() == 60 && tempo.getTick() > 0;
        }
        assertTrue(hasNinetyAfterStart);
        assertTrue(hasSixtyAfterStart);
    }

    @Test
    public void collectsPedalMarkingsAsCc64Events() {
        Document doc = MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise version=\"3.1\">"
                        + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                        + "<part id=\"P1\"><measure number=\"1\">"
                        + "<attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<direction><direction-type><pedal type=\"start\"/></direction-type></direction>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                        + "<direction><direction-type><pedal type=\"change\"/></direction-type></direction>"
                        + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                        + "<direction><direction-type><pedal type=\"stop\"/></direction-type></direction>"
                        + "</measure></part></score-partwise>");

        List<MidiIo.RawMidiControlEvent> ccEvents = MidiIo.collectMidiControlEventsFromMusicXmlDoc(doc, 128);

        assertEquals(4, ccEvents.size());
        List<Integer> values = new ArrayList<Integer>();
        for (MidiIo.RawMidiControlEvent event : ccEvents) {
            assertEquals(64, event.getControllerNumber());
            values.add(Integer.valueOf(event.getControllerValue()));
        }
        assertEquals(Arrays.asList(Integer.valueOf(127), Integer.valueOf(0), Integer.valueOf(127),
                Integer.valueOf(0)), values);
    }

    @Test
    public void mapsDrumNotesViaMidiUnpitchedAndInstrumentNameHintsInPlaybackExtraction() {
        String xml = "<score-partwise version=\"3.1\">"
                + "<part-list><score-part id=\"P1\">"
                + "<part-name>Drums</part-name>"
                + "<score-instrument id=\"P1-I-Kick\"><instrument-name>Bass Drum</instrument-name></score-instrument>"
                + "<score-instrument id=\"P1-I-Snare\"><instrument-name>Snare Drum</instrument-name></score-instrument>"
                + "<midi-instrument id=\"P1-I-Kick\"><midi-channel>10</midi-channel><midi-unpitched>36</midi-unpitched></midi-instrument>"
                + "</score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><instrument id=\"P1-I-Kick\"/><unpitched><display-step>D</display-step><display-octave>4</display-octave></unpitched>"
                + "<duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><instrument id=\"P1-I-Snare\"/><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));

        assertTrue(result.getEvents().size() >= 2);
        assertEquals(10, result.getEvents().get(0).getChannel());
        assertEquals(36, result.getEvents().get(0).getMidiNumber());
        assertEquals(38, result.getEvents().get(1).getMidiNumber());
    }

    @Test
    public void mergesTiedNotesInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>2</voice>"
                + "<tie type=\"start\"/><notations><tied type=\"start\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration>"
                + "<tie type=\"stop\"/><notations><tied type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(1, midi.getEvents().size());
        assertEquals(240, midi.getEvents().get(0).getDurTicks());
        assertEquals(2, playback.getEvents().size());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
    }

    @Test
    public void appliesTenutoLegatoOverlapInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><articulations><tenuto/></articulations></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(124, midi.getEvents().get(0).getDurTicks());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
    }

    @Test
    public void appliesDefaultDetacheInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult disabled = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi", "before_beat", false, "subtle", Boolean.FALSE));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(112, midi.getEvents().get(0).getDurTicks());
        assertEquals(120, disabled.getEvents().get(0).getDurTicks());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
    }

    @Test
    public void appliesSlurLegatoAndSuppressesDefaultDetacheInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><slur type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><slur type=\"stop\" number=\"1\"/></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(124, midi.getEvents().get(0).getDurTicks());
        assertEquals(120, midi.getEvents().get(1).getDurTicks());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
        assertEquals(120, playback.getEvents().get(1).getDurTicks());
    }

    @Test
    public void mergesRepeatedSamePitchInsideSlurInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><slur type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><slur type=\"stop\" number=\"1\"/></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(2, midi.getEvents().size());
        assertEquals(244, midi.getEvents().get(0).getDurTicks());
        assertEquals(3, playback.getEvents().size());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
    }

    @Test
    public void mergesRepeatedSamePitchInsideSlurInPlaybackLikeModeWithTieProcessing() {
        String xml = "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>1</beats><beat-type>1</beat-type></time></attributes>"
                + "<note><pitch><step>F</step><octave>5</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><slur type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>F</step><octave>5</octave></pitch><duration>120</duration>"
                + "<voice>1</voice><type>16th</type></note>"
                + "<note><pitch><step>E</step><octave>5</octave></pitch><duration>120</duration>"
                + "<voice>1</voice><type>16th</type>"
                + "<notations><slur type=\"stop\" number=\"1\"/></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("playback", "before_beat", false, "subtle", null, false,
                        false, true));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsByMidiNumberSorted(result, 77);

        assertEquals(1, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertTrue(events.get(0).getDurTicks() > 120);
    }

    @Test
    public void keepsRetriggerWhenRepeatedSamePitchNoteIsSlurStartBoundary() {
        String xml = "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>1</beats><beat-type>1</beat-type></time></attributes>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>240</duration>"
                + "<voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>240</duration>"
                + "<voice>1</voice><type>eighth</type>"
                + "<notations><slur type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><slur type=\"stop\" number=\"1\"/></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("playback", "before_beat", false, "subtle", null, false,
                        false, true));
        List<MidiIo.RawMidiPlaybackEvent> midiD4 = playbackEventsByMidiNumberSorted(midi, 62);
        List<MidiIo.RawMidiPlaybackEvent> playbackD4 = playbackEventsByMidiNumberSorted(playback, 62);

        assertEquals(2, midiD4.size());
        assertEquals(0, midiD4.get(0).getStartTicks());
        assertTrue(midiD4.get(1).getStartTicks() > 0);
        assertEquals(2, playbackD4.size());
        assertTrue(playbackD4.get(1).getStartTicks() > 0);
    }

    @Test
    public void doesNotExtendSlurStopNoteIntoFollowingSamePitch() {
        String xml = "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>2</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><slur type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><slur type=\"stop\" number=\"1\"/></notations></note>"
                + "</measure>"
                + "<measure number=\"2\">"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsByMidiNumberSorted(result, 62);

        assertEquals(3, events.size());
        assertEquals(128, events.get(1).getStartTicks());
        assertTrue(events.get(1).getStartTicks() + events.get(1).getDurTicks() <= events.get(2).getStartTicks());
    }

    @Test
    public void keepsRetriggerForRepeatedSamePitchSlurWhenStaccatoIsPresentInPlaybackLikeMode() {
        String xml = repeatedSamePitchSlurWithMiddleArticulationXml("staccato");

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("playback", "before_beat", false, "subtle", null, false,
                        false, true));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsByMidiNumberSorted(result, 77);

        assertEquals(2, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertTrue(events.get(1).getStartTicks() > 0);
    }

    @Test
    public void keepsRetriggerForRepeatedSamePitchSlurWhenTenutoIsPresentInPlaybackLikeMode() {
        String xml = repeatedSamePitchSlurWithMiddleArticulationXml("tenuto");

        MidiIo.MidiPlaybackEventsResult result = MidiIo.buildPlaybackEventsFromXml(xml, 128,
                new MidiIo.MidiPlaybackExtractionOptions("playback", "before_beat", false, "subtle", null, false,
                        false, true));
        List<MidiIo.RawMidiPlaybackEvent> events = playbackEventsByMidiNumberSorted(result, 77);

        assertEquals(2, events.size());
        assertEquals(0, events.get(0).getStartTicks());
        assertTrue(events.get(1).getStartTicks() > 0);
    }

    @Test
    public void appliesDirectionWedgeVelocityRampInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<direction><direction-type><wedge type=\"crescendo\" number=\"1\"/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<direction><direction-type><wedge type=\"stop\" number=\"1\"/></direction-type></direction>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(80, midi.getEvents().get(0).getVelocity());
        assertEquals(84, midi.getEvents().get(1).getVelocity());
        assertEquals(88, midi.getEvents().get(2).getVelocity());
        assertEquals(88, midi.getEvents().get(3).getVelocity());
        assertEquals(80, playback.getEvents().get(1).getVelocity());
        assertEquals(80, playback.getEvents().get(3).getVelocity());
    }

    @Test
    public void expandsOrnamentsInMidiPlaybackExtractionMode() {
        String xml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><ornaments><turn/></ornaments></notations></note>"
                + "</measure></part></score-partwise>";

        MidiIo.MidiPlaybackEventsResult midi = MidiIo.buildPlaybackEventsFromXml(xml, 120,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromXml(xml, 120);

        assertEquals(4, midi.getEvents().size());
        assertEquals(62, midi.getEvents().get(0).getMidiNumber());
        assertEquals(60, midi.getEvents().get(1).getMidiNumber());
        assertEquals(59, midi.getEvents().get(2).getMidiNumber());
        assertEquals(60, midi.getEvents().get(3).getMidiNumber());
        assertEquals(28, midi.getEvents().get(0).getDurTicks());
        assertEquals(28, midi.getEvents().get(1).getStartTicks());
        assertEquals(1, playback.getEvents().size());
        assertEquals(120, playback.getEvents().get(0).getDurTicks());
    }

    @Test
    public void enablesPlaybackLikeGraceOrnamentAndTieProcessingByOption() {
        String graceXml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><grace slash=\"yes\"/><pitch><step>G</step><octave>4</octave></pitch><voice>1</voice></note>"
                + "<note><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";
        String ornamentXml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><ornaments><turn/></ornaments></notations></note>"
                + "</measure></part></score-partwise>";
        String tieXml = "<score-partwise><part-list><score-part id=\"P1\"><part-name>Lead</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<tie type=\"start\"/><notations><tied type=\"start\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<tie type=\"stop\"/><notations><tied type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";
        MidiIo.MidiPlaybackExtractionOptions options = new MidiIo.MidiPlaybackExtractionOptions("playback",
                "before_beat", false, "subtle", null, true, true, true);

        MidiIo.MidiPlaybackEventsResult grace = MidiIo.buildPlaybackEventsFromXml(graceXml, 120, options);
        MidiIo.MidiPlaybackEventsResult ornament = MidiIo.buildPlaybackEventsFromXml(ornamentXml, 120, options);
        MidiIo.MidiPlaybackEventsResult tie = MidiIo.buildPlaybackEventsFromXml(tieXml, 120, options);

        assertEquals(2, grace.getEvents().size());
        assertEquals(67, grace.getEvents().get(0).getMidiNumber());
        assertEquals(72, grace.getEvents().get(1).getMidiNumber());
        assertEquals(4, ornament.getEvents().size());
        assertEquals(30, ornament.getEvents().get(0).getDurTicks());
        assertEquals(1, tie.getEvents().size());
        assertEquals(240, tie.getEvents().get(0).getDurTicks());
    }

    @Test
    public void selectsActiveMidiLanesForMeasureWithFallback() {
        List<MidiIo.MidiPartLaneDef> lanes = Arrays.asList(new MidiIo.MidiPartLaneDef(1, 1, 1),
                new MidiIo.MidiPartLaneDef(2, 1, 2), new MidiIo.MidiPartLaneDef(1, 2, 3));
        List<MidiIo.ImportedVoiceNoteSegment> measureSegments = Arrays.asList(
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 2, 0, 2, 48, 80, 0, 1, 0, 240),
                new MidiIo.ImportedVoiceNoteSegment(0, 2, 1, 2, 2, 76, 90, 0, 1, 240, 480));

        List<MidiIo.MidiPartLaneDef> active = MidiIo.buildMidiLanesForMeasure(lanes, measureSegments);
        assertEquals(2, active.size());
        assertEquals(2, active.get(0).getSourceStaff());
        assertEquals(1, active.get(0).getVoice());
        assertEquals(1, active.get(1).getSourceStaff());
        assertEquals(2, active.get(1).getVoice());

        List<MidiIo.MidiPartLaneDef> fallback = MidiIo.buildMidiLanesForMeasure(lanes,
                Arrays.<MidiIo.ImportedVoiceNoteSegment>asList());
        assertEquals(1, fallback.size());
        assertEquals(1, fallback.get(0).getSourceStaff());
        assertEquals(1, fallback.get(0).getVoice());
    }

    @Test
    public void buildsMidiTempoAndDynamicsDirectionXml() {
        assertEquals("ppp", MidiIo.velocityToDynamicMark(1));
        assertEquals("mp", MidiIo.velocityToDynamicMark(63));
        assertEquals("mf", MidiIo.velocityToDynamicMark(79));
        assertEquals("f", MidiIo.velocityToDynamicMark(95));
        assertEquals("fff", MidiIo.velocityToDynamicMark(127));

        assertEquals("<direction><direction-type><dynamics><mf/></dynamics></direction-type><staff>2</staff></direction>",
                MidiIo.buildDynamicsDirectionXml("mf", 0, 2));
        assertEquals("<direction><direction-type><dynamics><ff/></dynamics></direction-type><offset>3</offset><staff>1</staff></direction>",
                MidiIo.buildDynamicsDirectionXml("ff", 3, 1));
        assertEquals("<direction><direction-type><dynamics><mf/></dynamics></direction-type><staff>1</staff></direction>",
                MidiIo.buildDynamicsDirectionXml("unknown", 0, 0));

        assertEquals("<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>20</per-minute></metronome></direction-type><sound tempo=\"20\"/></direction>",
                MidiIo.buildTempoDirectionXml(4, 0));
        assertEquals("<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>180</per-minute></metronome></direction-type><offset>6</offset><sound tempo=\"180\"/></direction>",
                MidiIo.buildTempoDirectionXml(180, 6));
    }

    @Test
    public void buildsMeasureDynamicDirectionsXmlSkippingRepeatedMark() {
        List<MidiIo.ImportedVoiceNoteSegment> segments = Arrays.asList(
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 4, 2, 64, 40, 0, 1, 480, 720),
                new MidiIo.ImportedVoiceNoteSegment(0, 2, 1, 4, 2, 67, 96, 0, 1, 480, 720),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 8, 2, 72, 100, 0, 1, 960, 1200),
                new MidiIo.ImportedVoiceNoteSegment(0, 1, 1, 12, 2, 76, 20, 0, 1, 1440, 1680));

        MidiIo.MidiDynamicDirectionsResult result = MidiIo.buildMeasureDynamicDirectionsXml(segments, "mf");

        assertEquals("<direction><direction-type><dynamics><ff/></dynamics></direction-type><offset>4</offset><staff>1</staff></direction>"
                + "<direction><direction-type><dynamics><pp/></dynamics></direction-type><offset>12</offset><staff>1</staff></direction>",
                result.getXml());
        assertEquals("pp", result.getPreviousDynamicMark());

        MidiIo.MidiDynamicDirectionsResult empty = MidiIo.buildMeasureDynamicDirectionsXml(
                Arrays.<MidiIo.ImportedVoiceNoteSegment>asList(), "ff");
        assertEquals("", empty.getXml());
        assertEquals("ff", empty.getPreviousDynamicMark());
    }

    @Test
    public void buildsMidiInitialMeasureAttributesXmlForDrumAndSingleClef() {
        assertEquals("<attributes><divisions>4</divisions><key><fifths>-2</fifths><mode>minor</mode></key>"
                + "<time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>percussion</sign><line>2</line></clef></attributes>",
                MidiIo.buildMidiInitialMeasureAttributesXml(4, -2, "minor", 3, 4, true, false,
                        Arrays.<MidiIo.MidiPartLaneDef>asList(), "G", false));

        assertEquals("<attributes><divisions>8</divisions><key><fifths>7</fifths><mode>major</mode></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>F</sign><line>4</line></clef></attributes>",
                MidiIo.buildMidiInitialMeasureAttributesXml(8, 12, "dorian", 4, 4, false, false,
                        Arrays.<MidiIo.MidiPartLaneDef>asList(), "F", false));

        assertEquals("<attributes><divisions>1</divisions><key><fifths>-7</fifths><mode>major</mode></key>"
                + "<time><beats>1</beats><beat-type>1</beat-type></time>"
                + "<clef><sign>C</sign><line>3</line></clef></attributes>",
                MidiIo.buildMidiInitialMeasureAttributesXml(0, -12, "major", 0, 0, false, false,
                        Arrays.<MidiIo.MidiPartLaneDef>asList(), "G", true));
    }

    @Test
    public void buildsMidiInitialMeasureAttributesXmlForGrandStaffLanes() {
        List<MidiIo.MidiPartLaneDef> lanes = Arrays.asList(new MidiIo.MidiPartLaneDef(1, 1, 1),
                new MidiIo.MidiPartLaneDef(1, 2, 2), new MidiIo.MidiPartLaneDef(2, 1, 3));

        assertEquals("<attributes><divisions>4</divisions><key><fifths>1</fifths><mode>major</mode></key>"
                + "<time><beats>6</beats><beat-type>8</beat-type></time><staves>3</staves>"
                + "<clef number=\"1\"><sign>G</sign><line>2</line></clef>"
                + "<clef number=\"2\"><sign>G</sign><line>2</line></clef>"
                + "<clef number=\"3\"><sign>F</sign><line>4</line></clef></attributes>",
                MidiIo.buildMidiInitialMeasureAttributesXml(4, 1, "major", 6, 8, false, true, lanes, "G",
                        false));
    }

    @Test
    public void resolvesMidiPartMeasureLayoutWithoutPickup() {
        MidiIo.MidiPartMeasureLayout layout = MidiIo.resolveMidiPartMeasureLayout(Arrays.asList(
                new MidiIo.ImportedQuantizedNote(0, 1, 60, 0, 960, 80),
                new MidiIo.ImportedQuantizedNote(0, 1, 64, 1920, 3840, 90)), 480, 4, 4, 4, 0);

        assertEquals(1920, layout.getMeasureTicks());
        assertEquals(16, layout.getMeasureDiv());
        assertEquals(0, layout.getPickupMeasureTicks());
        assertEquals(0, layout.getPickupMeasureDiv());
        assertEquals(2, layout.getMeasureCount());
        assertEquals(16, MidiIo.measureDivForMidiPartMeasureIndex(layout, 0));
        assertEquals(1, MidiIo.midiPartMeasureNumberForIndex(layout, 0));
        assertEquals("", MidiIo.midiPartMeasureImplicitAttribute(layout, 0));
        assertEquals("<measure number=\"2\">", MidiIo.buildMidiPartMeasureStartXml(layout, 1));
    }

    @Test
    public void resolvesMidiPartMeasureLayoutWithPickupPrelude() {
        MidiIo.MidiPartMeasureLayout layout = MidiIo.resolveMidiPartMeasureLayout(Arrays.asList(
                new MidiIo.ImportedQuantizedNote(0, 1, 60, 0, 480, 80),
                new MidiIo.ImportedQuantizedNote(0, 1, 64, 480, 2400, 90)), 480, 4, 4, 4, 480);

        assertEquals(1920, layout.getMeasureTicks());
        assertEquals(16, layout.getMeasureDiv());
        assertEquals(480, layout.getPickupMeasureTicks());
        assertEquals(4, layout.getPickupMeasureDiv());
        assertEquals(2, layout.getMeasureCount());
        assertEquals(4, MidiIo.measureDivForMidiPartMeasureIndex(layout, 0));
        assertEquals(16, MidiIo.measureDivForMidiPartMeasureIndex(layout, 1));
        assertEquals(0, MidiIo.midiPartMeasureNumberForIndex(layout, 0));
        assertEquals(1, MidiIo.midiPartMeasureNumberForIndex(layout, 1));
        assertEquals(" implicit=\"yes\"", MidiIo.midiPartMeasureImplicitAttribute(layout, 0));
        assertEquals("<measure number=\"0\" implicit=\"yes\">", MidiIo.buildMidiPartMeasureStartXml(layout, 0));
    }

    @Test
    public void resolvesMidiPartSegmentLayoutAndFallsBackFromEmptyGrandStaffSide() {
        MidiIo.MidiPartMeasureLayout layout = new MidiIo.MidiPartMeasureLayout(1920, 16, 0, 0, 1);
        List<MidiIo.ImportedVoiceCluster> clusters = Arrays.asList(
                new MidiIo.ImportedVoiceCluster(1, 0, 480,
                        Arrays.asList(new MidiIo.ImportedQuantizedNote(0, 1, 72, 0, 480, 90))),
                new MidiIo.ImportedVoiceCluster(1, 480, 960,
                        Arrays.asList(new MidiIo.ImportedQuantizedNote(0, 1, 76, 480, 960, 88))));

        MidiIo.MidiPartSegmentLayout segmentLayout = MidiIo.resolveMidiPartSegmentLayout(clusters, 480, 4, layout,
                false, true);

        assertEquals(false, segmentLayout.isUseGrandStaff());
        assertEquals(2, segmentLayout.getSplitSegments().size());
        assertEquals(1, segmentLayout.getSplitSegments().get(0).getStaff());
        assertEquals(1, segmentLayout.getSplitSegments().get(1).getStaff());
        assertEquals(1, segmentLayout.getVoiceSegmentsByMeasure().size());
        assertEquals(2, segmentLayout.getVoiceSegmentsByMeasure().get(Integer.valueOf(0)).size());
    }

    @Test
    public void resolvesMidiPartSegmentLayoutKeepingGrandStaffAndGroupingMeasures() {
        MidiIo.MidiPartMeasureLayout layout = new MidiIo.MidiPartMeasureLayout(1920, 16, 480, 4, 2);
        List<MidiIo.ImportedVoiceCluster> clusters = Arrays.asList(
                new MidiIo.ImportedVoiceCluster(1, 0, 480,
                        Arrays.asList(new MidiIo.ImportedQuantizedNote(0, 1, 48, 0, 480, 80))),
                new MidiIo.ImportedVoiceCluster(1, 480, 960,
                        Arrays.asList(new MidiIo.ImportedQuantizedNote(0, 1, 76, 480, 960, 90))));

        MidiIo.MidiPartSegmentLayout segmentLayout = MidiIo.resolveMidiPartSegmentLayout(clusters, 480, 4, layout,
                false, true);

        assertEquals(true, segmentLayout.isUseGrandStaff());
        assertEquals(2, segmentLayout.getSplitSegments().size());
        assertEquals(2, segmentLayout.getSplitSegments().get(0).getStaff());
        assertEquals(1, segmentLayout.getSplitSegments().get(1).getStaff());
        assertEquals(2, segmentLayout.getVoiceSegmentsByMeasure().size());
        assertEquals(1, segmentLayout.getVoiceSegmentsByMeasure().get(Integer.valueOf(0)).size());
        assertEquals(1, segmentLayout.getVoiceSegmentsByMeasure().get(Integer.valueOf(1)).size());
    }

    @Test
    public void mapsMidiTicksToMeasureOffsetDivWithPickup() {
        MidiIo.MidiPartMeasureLayout layout = new MidiIo.MidiPartMeasureLayout(1920, 16, 480, 4, 3);

        MidiIo.MidiMeasureOffsetDiv pickup = MidiIo.mapMidiTickToMeasureOffsetDiv(240, layout, 480, 4);
        assertEquals(0, pickup.getMeasureIndex());
        assertEquals(2, pickup.getOffsetDiv());

        MidiIo.MidiMeasureOffsetDiv downbeat = MidiIo.mapMidiTickToMeasureOffsetDiv(480, layout, 480, 4);
        assertEquals(1, downbeat.getMeasureIndex());
        assertEquals(0, downbeat.getOffsetDiv());

        MidiIo.MidiMeasureOffsetDiv later = MidiIo.mapMidiTickToMeasureOffsetDiv(2400, layout, 480, 4);
        assertEquals(2, later.getMeasureIndex());
        assertEquals(0, later.getOffsetDiv());
    }

    @Test
    public void groupsMidiTempoEventsByMeasureSortedAndDeduped() {
        MidiIo.MidiPartMeasureLayout layout = new MidiIo.MidiPartMeasureLayout(1920, 16, 480, 4, 3);
        Map<Integer, List<MidiIo.MidiTempoMeasureEvent>> grouped = MidiIo.groupMidiTempoEventsByMeasure(Arrays.asList(
                new MidiIo.MidiTempoEvent(2400, 180),
                new MidiIo.MidiTempoEvent(0, 10),
                new MidiIo.MidiTempoEvent(480, 120),
                new MidiIo.MidiTempoEvent(480, 140),
                new MidiIo.MidiTempoEvent(960, 90)), layout, 480, 4);

        assertEquals(3, grouped.size());
        assertEquals(20, grouped.get(Integer.valueOf(0)).get(0).getBpm());
        assertEquals(0, grouped.get(Integer.valueOf(0)).get(0).getOffsetDiv());

        assertEquals(2, grouped.get(Integer.valueOf(1)).size());
        assertEquals(0, grouped.get(Integer.valueOf(1)).get(0).getOffsetDiv());
        assertEquals(140, grouped.get(Integer.valueOf(1)).get(0).getBpm());
        assertEquals(4, grouped.get(Integer.valueOf(1)).get(1).getOffsetDiv());
        assertEquals(90, grouped.get(Integer.valueOf(1)).get(1).getBpm());

        assertEquals(1, grouped.get(Integer.valueOf(2)).size());
        assertEquals(180, grouped.get(Integer.valueOf(2)).get(0).getBpm());
    }

    private static byte[] buildMidiImportFixture(List<String> tempoTrackTextLines, String noteTrackName,
            int midiNumber, int durationTicks) {
        return buildMidiImportFixture(tempoTrackTextLines, noteTrackName,
                Arrays.asList(new MidiIo.RawMidiPlaybackEvent(midiNumber, 0, durationTicks, 1, 100, "P1",
                        noteTrackName)));
    }

    private static byte[] buildMidiImportFixture(List<String> tempoTrackTextLines, String noteTrackName,
            List<MidiIo.RawMidiPlaybackEvent> events) {
        return buildMidiImportFixture(tempoTrackTextLines, noteTrackName, events, "off_before_on");
    }

    private static byte[] buildMidiImportFixture(List<String> tempoTrackTextLines, String noteTrackName,
            List<MidiIo.RawMidiPlaybackEvent> events, String retriggerPolicy) {
        return buildMidiImportFixture(tempoTrackTextLines, noteTrackName,
                Collections.<MidiIo.MidiTimeSignatureEvent>emptyList(),
                Collections.<MidiIo.MidiTickKeySignatureEvent>emptyList(), events, retriggerPolicy);
    }

    private static byte[] buildMidiImportFixture(List<String> tempoTrackTextLines, String noteTrackName,
            List<MidiIo.MidiTimeSignatureEvent> timeSignatureEvents,
            List<MidiIo.MidiTickKeySignatureEvent> keySignatureEvents,
            List<MidiIo.RawMidiPlaybackEvent> events) {
        return buildMidiImportFixture(tempoTrackTextLines, noteTrackName, timeSignatureEvents, keySignatureEvents,
                events, "off_before_on");
    }

    private static byte[] buildMidiImportFixture(List<String> tempoTrackTextLines, String noteTrackName,
            List<MidiIo.MidiTimeSignatureEvent> timeSignatureEvents,
            List<MidiIo.MidiTickKeySignatureEvent> keySignatureEvents,
            List<MidiIo.RawMidiPlaybackEvent> events, String retriggerPolicy) {
        List<byte[]> chunks = new ArrayList<byte[]>();
        chunks.add(MidiIo.buildRawMidiTempoTrackChunk(Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                timeSignatureEvents,
                keySignatureEvents,
                new MidiIo.RawMidiTempoTrackOptions(false, Collections.<String>emptyList(), tempoTrackTextLines,
                        "Meta")));
        chunks.addAll(MidiIo.buildRawMidiNoteTrackChunks(events,
                Collections.<String, Integer>emptyMap(), "acoustic_grand_piano", retriggerPolicy));
        return MidiIo.buildRawMidiBytesFromTrackChunks(chunks, 480);
    }

    private static byte[] rawMidiTrackChunk(int... data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('M');
        out.write('T');
        out.write('r');
        out.write('k');
        int length = data == null ? 0 : data.length;
        out.write((length >>> 24) & 0xff);
        out.write((length >>> 16) & 0xff);
        out.write((length >>> 8) & 0xff);
        out.write(length & 0xff);
        if (data != null) {
            for (int value : data) {
                out.write(value & 0xff);
            }
        }
        return out.toByteArray();
    }

    private static byte[] buildFormat0SingleNoteMidi(int velocity) {
        int safeVelocity = Math.max(1, Math.min(127, velocity));
        return new byte[] {
                'M', 'T', 'h', 'd',
                0, 0, 0, 6,
                0, 0,
                0, 1,
                1, (byte) 0xe0,
                'M', 'T', 'r', 'k',
                0, 0, 0, 13,
                0, (byte) 0x90, 60, (byte) safeVelocity,
                (byte) 0x83, 0x60, (byte) 0x80, 60, 0,
                0, (byte) 0xff, 0x2f, 0 };
    }

    private static byte[] exportRawMidiForTextMetaRegression(List<MidiIo.RawMidiPlaybackEvent> events,
            boolean emitMksTextMeta, String title, String movementTitle, String composer, int pickupTicks) {
        MidiIo.MidiExportPlaybackBuildResult result = MidiIo.buildMidiPlaybackExport(events, 120,
                "electric_piano_2", Collections.<String, Integer>emptyMap(),
                Collections.<MidiIo.RawMidiControlEvent>emptyList(),
                Arrays.asList(new MidiIo.MidiTempoEvent(0, 120)),
                Arrays.asList(new MidiIo.MidiTimeSignatureEvent(0, pickupTicks > 0 ? 6 : 4, pickupTicks > 0 ? 8 : 4)),
                Arrays.asList(new MidiIo.MidiKeySignatureEvent(0, pickupTicks > 0 ? -1 : 0, "major")),
                true, false, emitMksTextMeta, 480, Collections.<String>emptyList(), false,
                "off_before_on", title, movementTitle, composer, pickupTicks);
        return result.getRawBytes();
    }

    private static Document roundtripMusicXmlFixtureThroughMidi(Document source) {
        MidiIo.MidiPlaybackEventsResult playback = MidiIo.buildPlaybackEventsFromMusicXmlDoc(source, 128,
                new MidiIo.MidiPlaybackExtractionOptions("midi"));
        assertEquals(true, playback.getEvents().size() > 0);

        MidiIo.MidiExportPlaybackBuildResult exported = MidiIo.buildMidiPlaybackExport(playback.getEvents(),
                playback.getTempo(), "electric_piano_2", MidiIo.collectMidiProgramOverridesFromMusicXmlDoc(source),
                MidiIo.collectMidiControlEventsFromMusicXmlDoc(source, 128),
                MidiIo.collectMidiTempoEventsFromMusicXmlDoc(source, 128),
                MidiIo.collectMidiTimeSignatureEventsFromMusicXmlDoc(source, 128),
                MidiIo.collectMidiKeySignatureEventsFromMusicXmlDoc(source, 128),
                true, true, true, 128, Collections.<String>emptyList(), false, "off_before_on",
                "", "", "", 0);
        MidiIo.MidiImportResult imported = MidiIo.convertMidiToMusicXml(exported.getRawBytes(),
                new MidiIo.MidiImportOptions("1/16", null, null, null, null));
        assertEquals(true, imported.isOk(), imported.getDiagnostics().toString());
        Document roundtripped = MusicXmlIo.parseMusicXmlDocument(imported.getXml());
        assertTrue(roundtripped != null);
        return roundtripped;
    }

    private static Document parseMusicXmlFixture(String name) {
        String xml = loadResourceText(name);
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        assertTrue(doc != null, name);
        return doc;
    }

    private static String loadResourceText(String name) {
        try {
            InputStream in = MidiIoTest.class.getClassLoader().getResourceAsStream(name);
            if (in == null) {
                throw new IllegalArgumentException("Missing fixture: " + name);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            in.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load fixture: " + name, ex);
        }
    }

    private static String firstMeter(Document doc) {
        Element time = firstElement(doc, "time");
        if (time == null) {
            return "";
        }
        String beats = firstDirectChildText(time, "beats");
        String beatType = firstDirectChildText(time, "beat-type");
        return beats.length() == 0 || beatType.length() == 0 ? "" : beats + "/" + beatType;
    }

    private static Integer firstKeyFifths(Document doc) {
        Element key = firstElement(doc, "key");
        if (key == null) {
            return null;
        }
        try {
            return Integer.valueOf(firstDirectChildText(key, "fifths"));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer firstTempo(Document doc) {
        for (int index = 0; index < doc.getElementsByTagName("sound").getLength(); index++) {
            Element sound = (Element) doc.getElementsByTagName("sound").item(index);
            Integer tempo = roundedPositiveInteger(sound.getAttribute("tempo"));
            if (tempo != null) {
                return tempo;
            }
        }
        for (int index = 0; index < doc.getElementsByTagName("per-minute").getLength(); index++) {
            Integer tempo = roundedPositiveInteger(doc.getElementsByTagName("per-minute").item(index).getTextContent());
            if (tempo != null) {
                return tempo;
            }
        }
        return null;
    }

    private static Element firstElement(Document doc, String name) {
        return doc.getElementsByTagName(name).getLength() == 0 ? null : (Element) doc.getElementsByTagName(name).item(0);
    }

    private static String firstDirectChildText(Element parent, String name) {
        for (int index = 0; index < parent.getChildNodes().getLength(); index++) {
            if (parent.getChildNodes().item(index) instanceof Element
                    && name.equals(((Element) parent.getChildNodes().item(index)).getTagName())) {
                return parent.getChildNodes().item(index).getTextContent().trim();
            }
        }
        return "";
    }

    private static Integer roundedPositiveInteger(String text) {
        try {
            double value = Double.parseDouble(text == null ? "" : text.trim());
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
                return null;
            }
            return Integer.valueOf((int) Math.round(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<String> collectTextMetaFromMidi(byte[] midi) {
        List<String> texts = new ArrayList<String>();
        byte[] bytes = midi == null ? new byte[0] : midi;
        if (bytes.length < 14) {
            return texts;
        }
        int trackCount = ((bytes[10] & 0xff) << 8) | (bytes[11] & 0xff);
        int offset = 14;
        for (int track = 0; track < trackCount && offset + 8 <= bytes.length; track++) {
            if (bytes[offset] != 'M' || bytes[offset + 1] != 'T' || bytes[offset + 2] != 'r'
                    || bytes[offset + 3] != 'k') {
                break;
            }
            int length = ((bytes[offset + 4] & 0xff) << 24) | ((bytes[offset + 5] & 0xff) << 16)
                    | ((bytes[offset + 6] & 0xff) << 8) | (bytes[offset + 7] & 0xff);
            int pos = offset + 8;
            int end = Math.min(bytes.length, pos + Math.max(0, length));
            int runningStatus = -1;
            while (pos < end) {
                int[] delta = readTestVlq(bytes, pos, end);
                pos = delta[1];
                if (pos >= end) {
                    break;
                }
                int status = bytes[pos] & 0xff;
                if (status < 0x80) {
                    if (runningStatus < 0) {
                        break;
                    }
                    status = runningStatus;
                } else {
                    pos++;
                    if (status < 0xf0) {
                        runningStatus = status;
                    }
                }
                if (status == 0xff) {
                    if (pos >= end) {
                        break;
                    }
                    int type = bytes[pos++] & 0xff;
                    int[] len = readTestVlq(bytes, pos, end);
                    int metaLen = len[0];
                    pos = len[1];
                    int payloadEnd = Math.min(end, pos + Math.max(0, metaLen));
                    if (type == 0x01 || type == 0x03) {
                        texts.add(new String(Arrays.copyOfRange(bytes, pos, payloadEnd), StandardCharsets.UTF_8));
                    }
                    pos = payloadEnd;
                    continue;
                }
                if (status == 0xf0 || status == 0xf7) {
                    int[] len = readTestVlq(bytes, pos, end);
                    pos = Math.min(end, len[1] + Math.max(0, len[0]));
                    continue;
                }
                int eventType = status & 0xf0;
                int dataLength = (eventType == 0xc0 || eventType == 0xd0) ? 1 : 2;
                pos = Math.min(end, pos + dataLength);
            }
            offset = end;
        }
        return texts;
    }

    private static int[] readTestVlq(byte[] bytes, int offset, int end) {
        int value = 0;
        int pos = offset;
        while (pos < end) {
            int b = bytes[pos++] & 0xff;
            value = (value << 7) | (b & 0x7f);
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return new int[] { value, pos };
    }

    private static boolean anyStartsWith(List<String> values, String prefix) {
        for (String value : values) {
            if (value != null && value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static List<Element> pitchedNoteElements(Document doc) {
        List<Element> out = new ArrayList<Element>();
        for (int index = 0; index < doc.getElementsByTagName("note").getLength(); index++) {
            Element note = (Element) doc.getElementsByTagName("note").item(index);
            if (note.getElementsByTagName("pitch").getLength() > 0) {
                out.add(note);
            }
        }
        return out;
    }

    private static List<String> pitchedDurations(String xml) {
        List<String> out = new ArrayList<String>();
        for (Element note : pitchedNoteElements(MusicXmlIo.parseMusicXmlDocument(xml))) {
            out.add(note.getElementsByTagName("duration").item(0).getTextContent().trim());
        }
        return out;
    }

    private static boolean hasWarningCode(MidiIo.MidiImportResult result, String code) {
        for (MidiIo.MidiImportDiagnostic warning : result.getWarnings()) {
            if (code.equals(warning.getCode())) {
                return true;
            }
        }
        return false;
    }

    private static int distinctVoiceCount(Document doc) {
        List<String> voices = new ArrayList<String>();
        for (int index = 0; index < doc.getElementsByTagName("voice").getLength(); index++) {
            String voice = doc.getElementsByTagName("voice").item(index).getTextContent().trim();
            if (!voices.contains(voice)) {
                voices.add(voice);
            }
        }
        return voices.size();
    }

    private static String firstPartName(Document doc) {
        return doc.getElementsByTagName("part-name").item(0).getTextContent().trim();
    }

    private static Element firstPitchedNote(Document doc, String step, String alter, String octave) {
        for (Element note : pitchedNoteElements(doc)) {
            Element pitch = (Element) note.getElementsByTagName("pitch").item(0);
            if (step.equals(childText(pitch, "step")) && alter.equals(childText(pitch, "alter"))
                    && octave.equals(childText(pitch, "octave"))) {
                return note;
            }
        }
        return null;
    }

    private static String clefSign(Document doc, String number) {
        for (int index = 0; index < doc.getElementsByTagName("clef").getLength(); index++) {
            Element clef = (Element) doc.getElementsByTagName("clef").item(index);
            if (number.equals(clef.getAttribute("number"))) {
                return childText(clef, "sign");
            }
        }
        return "";
    }

    private static Element measureByNumber(Document doc, String number) {
        for (int index = 0; index < doc.getElementsByTagName("measure").getLength(); index++) {
            Element measure = (Element) doc.getElementsByTagName("measure").item(index);
            if (number.equals(measure.getAttribute("number"))) {
                return measure;
            }
        }
        return null;
    }

    private static int voiceNoteCount(Element measure, String voice) {
        int count = 0;
        if (measure == null) {
            return count;
        }
        for (int index = 0; index < measure.getElementsByTagName("note").getLength(); index++) {
            Element note = (Element) measure.getElementsByTagName("note").item(index);
            if (voice.equals(childText(note, "voice"))) {
                count++;
            }
        }
        return count;
    }

    private static String childText(Element element, String name) {
        if (element == null || element.getElementsByTagName(name).getLength() == 0) {
            return "";
        }
        return element.getElementsByTagName(name).item(0).getTextContent().trim();
    }

    private static List<String> dynamicTagNames(Document doc) {
        List<String> out = new ArrayList<String>();
        for (int index = 0; index < doc.getElementsByTagName("dynamics").getLength(); index++) {
            Element dynamics = (Element) doc.getElementsByTagName("dynamics").item(index);
            for (int childIndex = 0; childIndex < dynamics.getChildNodes().getLength(); childIndex++) {
                if (dynamics.getChildNodes().item(childIndex) instanceof Element) {
                    out.add(((Element) dynamics.getChildNodes().item(childIndex)).getTagName());
                }
            }
        }
        return out;
    }

    private static int countString(List<String> values, String expected) {
        int count = 0;
        for (String value : values) {
            if (expected.equals(value)) {
                count++;
            }
        }
        return count;
    }

    private static boolean allPitchedNotesHaveChild(List<Element> notes, String name) {
        for (Element note : notes) {
            if (note.getElementsByTagName(name).getLength() == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasTieType(List<Element> notes, String type) {
        for (Element note : notes) {
            for (int index = 0; index < note.getElementsByTagName("tie").getLength(); index++) {
                Element tie = (Element) note.getElementsByTagName("tie").item(index);
                if (type.equals(tie.getAttribute("type"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Element> miscFieldsWithNamePrefix(Document doc, String prefix) {
        List<Element> out = new ArrayList<Element>();
        for (int index = 0; index < doc.getElementsByTagName("miscellaneous-field").getLength(); index++) {
            Element field = (Element) doc.getElementsByTagName("miscellaneous-field").item(index);
            if (field.getAttribute("name").startsWith(prefix)) {
                out.add(field);
            }
        }
        return out;
    }

    private static String miscFieldTextByName(Document doc, String name) {
        for (int index = 0; index < doc.getElementsByTagName("miscellaneous-field").getLength(); index++) {
            Element field = (Element) doc.getElementsByTagName("miscellaneous-field").item(index);
            if (name.equals(field.getAttribute("name"))) {
                return field.getTextContent().trim();
            }
        }
        return "";
    }

    private static List<String> elementTextValues(Document doc, String name) {
        List<String> out = new ArrayList<String>();
        for (int index = 0; index < doc.getElementsByTagName(name).getLength(); index++) {
            out.add(doc.getElementsByTagName(name).item(index).getTextContent().trim());
        }
        return out;
    }

    private static List<MidiIo.RawMidiPlaybackEvent> playbackEventsSortedByStart(
            MidiIo.MidiPlaybackEventsResult result) {
        List<MidiIo.RawMidiPlaybackEvent> events = new ArrayList<MidiIo.RawMidiPlaybackEvent>(
                result == null ? Collections.<MidiIo.RawMidiPlaybackEvent>emptyList() : result.getEvents());
        Collections.sort(events, new Comparator<MidiIo.RawMidiPlaybackEvent>() {
            @Override
            public int compare(MidiIo.RawMidiPlaybackEvent left, MidiIo.RawMidiPlaybackEvent right) {
                int byStart = Integer.compare(left.getStartTicks(), right.getStartTicks());
                if (byStart != 0) {
                    return byStart;
                }
                return Integer.compare(left.getMidiNumber(), right.getMidiNumber());
            }
        });
        return events;
    }

    private static MidiIo.RawMidiPlaybackEvent playbackEventByMidiNumber(MidiIo.MidiPlaybackEventsResult result,
            int midiNumber) {
        if (result == null) {
            return null;
        }
        for (MidiIo.RawMidiPlaybackEvent event : result.getEvents()) {
            if (event.getMidiNumber() == midiNumber) {
                return event;
            }
        }
        return null;
    }

    private static List<MidiIo.RawMidiPlaybackEvent> playbackEventsByMidiNumberSorted(
            MidiIo.MidiPlaybackEventsResult result, int midiNumber) {
        List<MidiIo.RawMidiPlaybackEvent> out = new ArrayList<MidiIo.RawMidiPlaybackEvent>();
        for (MidiIo.RawMidiPlaybackEvent event : playbackEventsSortedByStart(result)) {
            if (event.getMidiNumber() == midiNumber) {
                out.add(event);
            }
        }
        return out;
    }

    private static List<Integer> playbackVelocities(MidiIo.MidiPlaybackEventsResult result) {
        List<Integer> out = new ArrayList<Integer>();
        for (MidiIo.RawMidiPlaybackEvent event : playbackEventsSortedByStart(result)) {
            out.add(Integer.valueOf(event.getVelocity()));
        }
        return out;
    }

    private static String repeatedSamePitchSlurWithMiddleArticulationXml(String articulationName) {
        return "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>1</beats><beat-type>1</beat-type></time></attributes>"
                + "<note><pitch><step>F</step><octave>5</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><slur type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>F</step><octave>5</octave></pitch><duration>120</duration>"
                + "<voice>1</voice><type>16th</type>"
                + "<notations><articulations><" + articulationName + "/></articulations></notations></note>"
                + "<note><pitch><step>E</step><octave>5</octave></pitch><duration>120</duration>"
                + "<voice>1</voice><type>16th</type>"
                + "<notations><slur type=\"stop\" number=\"1\"/></notations></note>"
                + "</measure></part></score-partwise>";
    }
}
