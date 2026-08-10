package jp.igapyon.mikuscore.musescore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;
import jp.igapyon.mikuscore.musicxml.MxlIo;

public class MuseScoreIoTest {
    @Test
    public void mapsDurationTypesToDivisions() {
        assertEquals(Integer.valueOf(1920), MuseScoreIo.durationTypeToDivisions("whole", 480));
        assertEquals(Integer.valueOf(960), MuseScoreIo.durationTypeToDivisions("half", 480));
        assertEquals(Integer.valueOf(480), MuseScoreIo.durationTypeToDivisions("quarter", 480));
        assertEquals(Integer.valueOf(240), MuseScoreIo.durationTypeToDivisions("eighth", 480));
        assertEquals(Integer.valueOf(120), MuseScoreIo.durationTypeToDivisions("16th", 480));
        assertNull(MuseScoreIo.durationTypeToDivisions("bad", 480));
    }

    @Test
    public void appliesDotsToDurationDivisions() {
        assertEquals(480, MuseScoreIo.durationWithDots(480, 0));
        assertEquals(720, MuseScoreIo.durationWithDots(480, 1));
        assertEquals(840, MuseScoreIo.durationWithDots(480, 2));
    }

    @Test
    public void mapsDivisionsBackToTypeAndDots() {
        MuseScoreIo.TypeAndDots dottedQuarter = MuseScoreIo.divisionToTypeAndDots(480, 720);
        assertEquals("quarter", dottedQuarter.getType());
        assertEquals(1, dottedQuarter.getDots());

        MuseScoreIo.TypeAndDots nearest = MuseScoreIo.divisionToTypeAndDots(480, 470);
        assertEquals("quarter", nearest.getType());
        assertEquals(0, nearest.getDots());
    }

    @Test
    public void detectsMuseScoreDefaultTitleAndComposerPlaceholders() {
        assertEquals(true, MuseScoreIo.isMuseDefaultWorkTitle("Untitled Score"));
        assertEquals(true, MuseScoreIo.isMuseDefaultWorkTitle("無題のスコア"));
        assertEquals(false, MuseScoreIo.isMuseDefaultWorkTitle("String Quartet"));

        assertEquals(true, MuseScoreIo.isMuseDefaultComposer("Composer / Arranger"));
        assertEquals(true, MuseScoreIo.isMuseDefaultComposer("作曲者 / 編曲者"));
        assertEquals(false, MuseScoreIo.isMuseDefaultComposer("W.A.Mozart"));
    }

    @Test
    public void mapsMuseScoreAccidentalSubtypeToMusicXmlText() {
        assertEquals("sharp", MuseScoreIo.museAccidentalSubtypeToMusicXml("accidentalSharp"));
        assertEquals("flat", MuseScoreIo.museAccidentalSubtypeToMusicXml("accidentalFlat"));
        assertEquals("natural", MuseScoreIo.museAccidentalSubtypeToMusicXml("accidentalNatural"));
        assertEquals("double-sharp", MuseScoreIo.museAccidentalSubtypeToMusicXml("accidentalDoubleSharp"));
        assertEquals("flat-flat", MuseScoreIo.museAccidentalSubtypeToMusicXml("accidentalDoubleFlat"));
        assertNull(MuseScoreIo.museAccidentalSubtypeToMusicXml("other"));

        assertEquals("accidentalSharp", MuseScoreIo.parseMusicXmlAccidentalSubtype(" sharp "));
        assertEquals("accidentalFlat", MuseScoreIo.parseMusicXmlAccidentalSubtype("flat"));
        assertEquals("accidentalNatural", MuseScoreIo.parseMusicXmlAccidentalSubtype("natural"));
        assertEquals("accidentalDoubleSharp", MuseScoreIo.parseMusicXmlAccidentalSubtype("double-sharp"));
        assertEquals("accidentalDoubleFlat", MuseScoreIo.parseMusicXmlAccidentalSubtype("flat-flat"));
        assertNull(MuseScoreIo.parseMusicXmlAccidentalSubtype("quarter-sharp"));
    }

    @Test
    public void parsesMusicXmlPitchStaffHelpers() {
        assertEquals(1, MuseScoreIo.getNoteStaffNo(null));
        assertEquals(2, MuseScoreIo.getNoteStaffNo(" 2.4 "));
        assertEquals(1, MuseScoreIo.getNoteStaffNo("bad"));
        assertEquals(Integer.valueOf(60), MuseScoreIo.parseMusicXmlPitchToMidi("C", "4", "0"));
        assertEquals(Integer.valueOf(61), MuseScoreIo.parseMusicXmlPitchToMidi("C", "4", "1"));
        assertEquals(Integer.valueOf(58), MuseScoreIo.parseMusicXmlPitchToMidi("B", "3", "-1"));
        assertEquals(Integer.valueOf(127), MuseScoreIo.parseMusicXmlPitchToMidi("G", "10", "0"));
        assertEquals(Integer.valueOf(0), MuseScoreIo.parseMusicXmlPitchToMidi("C", "-5", "0"));
        assertNull(MuseScoreIo.parseMusicXmlPitchToMidi("H", "4", "0"));
        assertNull(MuseScoreIo.parseMusicXmlPitchToMidi("C", "", "0"));

        Map<Integer, List<Integer>> byStaff = MuseScoreIo.collectMusicXmlPitchesByStaff(Arrays.asList(
                new MuseScoreIo.MusicXmlPitchStaff("C", "4", "0", "1", false),
                new MuseScoreIo.MusicXmlPitchStaff("E", "4", "0", "1", false),
                new MuseScoreIo.MusicXmlPitchStaff("C", "3", "0", "2", false),
                new MuseScoreIo.MusicXmlPitchStaff("D", "4", "0", "2", true),
                new MuseScoreIo.MusicXmlPitchStaff("H", "4", "0", "3", false)));
        assertEquals(Arrays.asList(Integer.valueOf(60), Integer.valueOf(64)), byStaff.get(Integer.valueOf(1)));
        assertEquals(Arrays.asList(Integer.valueOf(48)), byStaff.get(Integer.valueOf(2)));
        assertEquals(false, byStaff.containsKey(Integer.valueOf(3)));
    }

    @Test
    public void mapsMuseScoreTpcToAccidentalText() {
        assertEquals("flat", MuseScoreIo.museTpcToAccidentalText("12"));
        assertEquals("sharp", MuseScoreIo.museTpcToAccidentalText("24"));
        assertEquals("double-sharp", MuseScoreIo.museTpcToAccidentalText("31"));
        assertEquals("flat-flat", MuseScoreIo.museTpcToAccidentalText("-1"));
        assertNull(MuseScoreIo.museTpcToAccidentalText("14"));
        assertNull(MuseScoreIo.museTpcToAccidentalText("bad"));
    }

    @Test
    public void parsesBooleanAndRepeatHelpers() {
        assertEquals(true, MuseScoreIo.parseTruthyFlag("YES"));
        assertEquals(false, MuseScoreIo.parseTruthyFlag("0"));

        MuseScoreIo.RepeatFlags flags = MuseScoreIo
                .parseMeasureRepeatFlagsFromBarlineSubtypes(Arrays.asList("start repeat", "repeat_end"));
        assertEquals(true, flags.isRepeatForward());
        assertEquals(true, flags.isRepeatBackward());

        assertEquals("<barline location=\"middle\"><bar-style>heavy-light</bar-style><repeat direction=\"forward\"/></barline>",
                MuseScoreIo.buildMidMeasureRepeatBarlineXml("forward"));

        MuseScoreIo.MusePendingDirectionMarks middle = MuseScoreIo.parseMusicXmlMidBarlineRepeatMarks(" middle ",
                Arrays.asList("forward", "backward", "forward", "other"));
        assertEquals(2, middle.getRepeatForwardCount());
        assertEquals(1, middle.getRepeatBackwardCount());
        assertNull(MuseScoreIo.parseMusicXmlMidBarlineRepeatMarks("right", Arrays.asList("forward")));
    }

    @Test
    public void parsesMusicXmlNotationNumberHelpers() {
        MuseScoreIo.TieFlags tie = MuseScoreIo.parseMusicXmlTieFlags(false, true, true, false);
        assertEquals(true, tie.isTieStart());
        assertEquals(true, tie.isTieStop());

        MuseScoreIo.MusicXmlNumberSet slurs = MuseScoreIo.parseMusicXmlSlurNumbers(Arrays.asList(
                new MuseScoreIo.MusicXmlTypedNumber(" start ", "2"),
                new MuseScoreIo.MusicXmlTypedNumber("stop", ""),
                new MuseScoreIo.MusicXmlTypedNumber("start", "-3"),
                new MuseScoreIo.MusicXmlTypedNumber("continue", "4")));
        assertEquals(Arrays.asList(Integer.valueOf(2), Integer.valueOf(1)), slurs.getStarts());
        assertEquals(Arrays.asList(Integer.valueOf(1)), slurs.getStops());

        MuseScoreIo.MusicXmlNumberSet trills = MuseScoreIo.parseMusicXmlTrillNumbers(Arrays.asList(
                new MuseScoreIo.MusicXmlTypedNumber("start", "3"),
                new MuseScoreIo.MusicXmlTypedNumber("stop", "bad")));
        assertEquals(Arrays.asList(Integer.valueOf(3)), trills.getStarts());
        assertEquals(Arrays.asList(Integer.valueOf(1)), trills.getStops());
        assertEquals(false, MuseScoreIo.hasMusicXmlTrillMarkOnly(true, trills));
        assertEquals(true, MuseScoreIo.hasMusicXmlTrillMarkOnly(true,
                new MuseScoreIo.MusicXmlNumberSet(null, null)));

        MuseScoreIo.MusicXmlNumberSet tuplets = MuseScoreIo.parseMusicXmlTupletNumbers(Arrays.asList(
                new MuseScoreIo.MusicXmlTypedNumber("start", "5"),
                new MuseScoreIo.MusicXmlTypedNumber("stop", "6")));
        assertEquals(Arrays.asList(Integer.valueOf(5)), tuplets.getStarts());
        assertEquals(Arrays.asList(Integer.valueOf(6)), tuplets.getStops());
        MuseScoreIo.TimeModification timeModification = MuseScoreIo.parseMusicXmlTupletTimeModification("3", "2");
        assertEquals(3, timeModification.getActualNotes());
        assertEquals(2, timeModification.getNormalNotes());
        assertNull(MuseScoreIo.parseMusicXmlTupletTimeModification("0", "2"));
        assertNull(MuseScoreIo.parseMusicXmlTupletTimeModification("3", "bad"));

        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)),
                MuseScoreIo.mergeUniqueNumbers(Arrays.asList(Integer.valueOf(2), Integer.valueOf(1)),
                        Arrays.asList(Integer.valueOf(3), Integer.valueOf(2))));
        assertEquals(Arrays.asList("a", "b", "c"),
                MuseScoreIo.mergeUniqueStrings(Arrays.asList("a", "b"), Arrays.asList("b", "c")));
    }

    @Test
    public void parsesMusicXmlArticulationTechnicalAndClefHelpers() {
        assertEquals(Arrays.asList("articStaccatoAbove", "articAccentAbove", "articTenutoAbove",
                "articLhPizzicatoAbove", "snapPizzicato", "articUpBowAbove", "articDownBowAbove",
                "articOpenStringAbove", "articHarmonicAbove"),
                MuseScoreIo.parseMusicXmlArticulationSubtypes(
                        Arrays.asList("staccato", "accent", "tenuto", "other"),
                        Arrays.asList("stopped", "snap-pizzicato", "up-bow", "down-bow", "open-string",
                                "harmonic", "other")));
        assertEquals("2", MuseScoreIo.parseMusicXmlTechnicalFingering(" 2 "));
        assertNull(MuseScoreIo.parseMusicXmlTechnicalFingering("  "));
        assertEquals(Integer.valueOf(4), MuseScoreIo.parseMusicXmlTechnicalString("4"));
        assertNull(MuseScoreIo.parseMusicXmlTechnicalString("0"));
        assertNull(MuseScoreIo.parseMusicXmlTechnicalString("bad"));

        assertEquals("C", MuseScoreIo.normalizeMusicXmlClefSign(" alto C "));
        assertEquals("F", MuseScoreIo.normalizeMusicXmlClefSign("f"));
        assertEquals("G", MuseScoreIo.normalizeMusicXmlClefSign("G"));
        assertNull(MuseScoreIo.normalizeMusicXmlClefSign(""));
        assertEquals("C3", MuseScoreIo.readMusicXmlMeasureMuseConcertClefType("C", ""));
        assertEquals("C4", MuseScoreIo.readMusicXmlMeasureMuseConcertClefType("C", "4"));
        assertEquals("F", MuseScoreIo.readMusicXmlMeasureMuseConcertClefType("F", "4"));
        assertNull(MuseScoreIo.readMusicXmlMeasureMuseConcertClefType("P", "4"));
        assertEquals("F", MuseScoreIo.readMusicXmlMeasureClefSignFromValues(
                Arrays.asList(new MuseScoreIo.MusicXmlClefSource(null, "G"),
                        new MuseScoreIo.MusicXmlClefSource("1", "F")),
                1));
        assertEquals("C", MuseScoreIo.readMusicXmlMeasureClefSignFromValues(
                Arrays.asList(new MuseScoreIo.MusicXmlClefSource(null, "G"),
                        new MuseScoreIo.MusicXmlClefSource("2", "C")),
                2));
        assertNull(MuseScoreIo.readMusicXmlMeasureClefSignFromValues(
                Arrays.asList(new MuseScoreIo.MusicXmlClefSource(null, "G")), 2));
        assertEquals("C", MuseScoreIo.readFirstExplicitClefInPartValues(Arrays.asList(
                new MuseScoreIo.MusicXmlMeasureClefSet(null),
                new MuseScoreIo.MusicXmlMeasureClefSet(Arrays.asList(
                        new MuseScoreIo.MusicXmlClefSource("2", "C")))),
                2));
        assertEquals("F", MuseScoreIo.inferClefSignFromPitches(Arrays.asList(Integer.valueOf(40),
                Integer.valueOf(50), Integer.valueOf(61))));
        assertEquals("G", MuseScoreIo.inferClefSignFromPitches(Arrays.asList(Integer.valueOf(59),
                Integer.valueOf(61))));
        assertEquals("C", MuseScoreIo.inferClefSignFromPartName("Solo Viola"));
        assertEquals("F", MuseScoreIo.inferClefSignFromPartName("Double Bass"));
        assertNull(MuseScoreIo.inferClefSignFromPartName("Violin"));
        assertEquals("C3", MuseScoreIo.clefSignToMuseDefaultClef("C"));
        assertEquals("F", MuseScoreIo.clefSignToMuseConcertClefType("F"));
    }

    @Test
    public void mapsDynamicsAndVisibilityHelpers() {
        assertEquals("sfz", MuseScoreIo.parseMuseDynamicMark("SFZ"));
        assertNull(MuseScoreIo.parseMuseDynamicMark("loud"));
        assertEquals("mf", MuseScoreIo.dynamicTagToMuseSubtype(" MF "));
        assertNull(MuseScoreIo.dynamicTagToMuseSubtype("loud"));
        assertEquals(Double.valueOf(100.0d), MuseScoreIo.parseMuseDynamicSoundValue(Integer.valueOf(90)));
        assertEquals(Integer.valueOf(90), MuseScoreIo.musicXmlSoundDynamicsToMuseVelocity("100"));
        assertEquals(Integer.valueOf(127), MuseScoreIo.musicXmlSoundDynamicsToMuseVelocity("200"));
        assertNull(MuseScoreIo.musicXmlSoundDynamicsToMuseVelocity("0"));
        assertEquals(Double.valueOf(1.5d), MuseScoreIo.beatUnitToQuarterFactor(" quarter ", 1));
        assertEquals(Double.valueOf(0.875d), MuseScoreIo.beatUnitToQuarterFactor("eighth", 2));
        assertNull(MuseScoreIo.beatUnitToQuarterFactor("breve", 0));
        assertEquals(2.0d, MuseScoreIo.readDirectionTempoQps("120", "", "", 0), 0.0001d);
        assertEquals(1.5d, MuseScoreIo.readDirectionTempoQps("", "60", "quarter", 1), 0.0001d);
        assertEquals(0.0d, MuseScoreIo.readDirectionTempoQps("", "bad", "quarter", 0), 0.0001d);
        assertNull(MuseScoreIo.parseMuseDynamicSoundValue(Integer.valueOf(0)));
        assertEquals(true, MuseScoreIo.isMuseElementVisible(null));
        assertEquals(false, MuseScoreIo.isMuseElementVisible(Integer.valueOf(0)));
    }

    @Test
    public void collectsDirectionSeedsFromMusicXmlValues() {
        List<MuseScoreIo.MusicXmlDirectionSeedSource> directions = Arrays.asList(
                new MuseScoreIo.MusicXmlDirectionSeedSource("1", "120", "", "", 0, "100", "segno", "", "",
                        "coda", Arrays.asList("mf", "loud"), true, false,
                        Arrays.asList(new MuseScoreIo.MusicXmlDirectionWords("Allegro", ""))),
                new MuseScoreIo.MusicXmlDirectionSeedSource("1", "", "", "", 0, "", "", "", "", "", null,
                        false, true, Arrays.asList(new MuseScoreIo.MusicXmlDirectionWords(" Fine ", ""),
                                new MuseScoreIo.MusicXmlDirectionWords(" dolce ", "italic"))),
                new MuseScoreIo.MusicXmlDirectionSeedSource("2", "", "60", "quarter", 1, "", "", "yes", "",
                        "", null, false, false, null));

        List<MuseScoreIo.MuseDirectionSeed> staffOne = MuseScoreIo.collectDirectionSeedsFromMusicXmlMeasureValues(
                directions, Arrays.asList("90", "bad"), 1);
        assertEquals(8, staffOne.size());
        assertEquals("dynamic", staffOne.get(0).getKind());
        assertEquals("mf", staffOne.get(0).getSubtype());
        assertEquals(Integer.valueOf(90), staffOne.get(0).getVelocity());
        assertEquals("marker", staffOne.get(1).getKind());
        assertEquals("segno", staffOne.get(1).getSubtype());
        assertEquals("tempo", staffOne.get(2).getKind());
        assertEquals(2.0d, staffOne.get(2).getQps(), 0.0001d);
        assertEquals("Allegro", staffOne.get(2).getText());
        assertEquals("jump", staffOne.get(3).getKind());
        assertEquals("D.S.", staffOne.get(3).getText());
        assertEquals("segno", staffOne.get(3).getJumpTo());
        assertEquals("coda", staffOne.get(3).getPlayUntil());
        assertEquals("coda", staffOne.get(3).getContinueAt());
        assertEquals("coda", staffOne.get(4).getSubtype());
        assertEquals("fine", staffOne.get(5).getSubtype());
        assertEquals("expression", staffOne.get(6).getKind());
        assertEquals(true, staffOne.get(6).isItalic());
        assertEquals("tempo", staffOne.get(7).getKind());
        assertEquals(true, staffOne.get(7).isFollowText());
        assertEquals(Boolean.FALSE, staffOne.get(7).getVisible());

        List<MuseScoreIo.MuseDirectionSeed> staffTwo = MuseScoreIo.collectDirectionSeedsFromMusicXmlMeasureValues(
                directions, Arrays.asList("90"), 2);
        assertEquals(2, staffTwo.size());
        assertEquals("tempo", staffTwo.get(0).getKind());
        assertEquals(1.5d, staffTwo.get(0).getQps(), 0.0001d);
        assertEquals("D.C.", staffTwo.get(1).getText());
        assertEquals("start", staffTwo.get(1).getJumpTo());
    }

    @Test
    public void buildsMuseScoreDirectionSeedXml() {
        assertEquals("<Tempo><tempo>2.000000</tempo><text>Allegro &amp; fast</text></Tempo>",
                MuseScoreIo.buildMuseScoreDirectionSeedXml(MuseScoreIo.MuseDirectionSeed.tempo(2.0d,
                        "Allegro & fast", false, null)));
        assertEquals("<Tempo><tempo>1.500000</tempo><followText>1</followText><visible>0</visible><text><sym>metNoteQuarterUp</sym><font face=\"Edwin\"></font> = 90</text></Tempo>",
                MuseScoreIo.buildMuseScoreDirectionSeedXml(MuseScoreIo.MuseDirectionSeed.tempo(1.5d,
                        "<sym>metNoteQuarterUp</sym><font face=\"Edwin\"></font> = 90", true, Boolean.FALSE)));
        assertEquals("<Dynamic><subtype>mf</subtype><velocity>90</velocity></Dynamic>",
                MuseScoreIo.buildMuseScoreDirectionSeedXml(MuseScoreIo.MuseDirectionSeed.dynamic("mf",
                        Integer.valueOf(90))));
        assertEquals("<Expression><text><i></i>dolce &amp; cantabile</text></Expression>",
                MuseScoreIo.buildMuseScoreDirectionSeedXml(MuseScoreIo.MuseDirectionSeed.expression(
                        "dolce & cantabile", true)));
        assertEquals("<Marker><subtype>segno</subtype><label>segno</label></Marker>",
                MuseScoreIo.buildMuseScoreDirectionSeedXml(MuseScoreIo.MuseDirectionSeed.marker("segno", "segno")));
        assertEquals("<Jump><text>D.S.</text><jumpTo>segno</jumpTo><playUntil>coda</playUntil><continueAt>coda</continueAt></Jump>",
                MuseScoreIo.buildMuseScoreDirectionSeedXml(MuseScoreIo.MuseDirectionSeed.jump("D.S.", "segno",
                        "coda", "coda")));
    }

    @Test
    public void buildsMuseScoreMeasureHeaderXml() {
        assertEquals("<KeySig><accidental>2</accidental><concertKey>2</concertKey></KeySig>",
                MuseScoreIo.resolveMuseExportKeySigXml(2, null));
        assertEquals("<KeySig><accidental>2</accidental><concertKey>-3</concertKey><transposeKey>2</transposeKey></KeySig>",
                MuseScoreIo.resolveMuseExportKeySigXml(2, new MuseScoreIo.Transpose(null, Integer.valueOf(1))));

        MuseScoreIo.MuseScoreExportMeasureContext context = new MuseScoreIo.MuseScoreExportMeasureContext(2, 2,
                "cut", 2, "C", null, true, true, true, true,
                Arrays.asList(MuseScoreIo.MuseDirectionSeed.dynamic("mf", Integer.valueOf(90))), true);
        assertEquals("<Clef><concertClefType>C3</concertClefType></Clef><TimeSig><subtype>2</subtype><sigN>2</sigN><sigD>2</sigD></TimeSig><KeySig><accidental>2</accidental><concertKey>-3</concertKey><transposeKey>2</transposeKey></KeySig><BarLine><subtype>double</subtype></BarLine><Dynamic><subtype>mf</subtype><velocity>90</velocity></Dynamic><startRepeat/>",
                MuseScoreIo.buildMuseScoreMeasureHeaderXml(context,
                        new MuseScoreIo.Transpose(null, Integer.valueOf(1))));

        MuseScoreIo.MuseScoreExportMeasureContext explicitClef = new MuseScoreIo.MuseScoreExportMeasureContext(3, 4,
                null, 0, "G", "F", true, false, false, false, null, false);
        assertEquals("<Clef><concertClefType>F</concertClefType></Clef>",
                MuseScoreIo.buildMuseScoreMeasureHeaderXml(explicitClef, null));
    }

    @Test
    public void resolvesMuseScoreExportSlurIds() {
        MuseScoreIo.MuseScoreExportSlurState state = new MuseScoreIo.MuseScoreExportSlurState();
        assertEquals(Arrays.asList(Integer.valueOf(1)),
                MuseScoreIo.resolveMuseExportSlurIds(state, 1, 1, 1, Arrays.asList(Integer.valueOf(2)), true));
        assertEquals(Arrays.asList(Integer.valueOf(2)),
                MuseScoreIo.resolveMuseExportSlurIds(state, 1, 1, 1, Arrays.asList(Integer.valueOf(2)), true));
        assertEquals(Arrays.asList(Integer.valueOf(2)),
                MuseScoreIo.resolveMuseExportSlurIds(state, 1, 1, 1, Arrays.asList(Integer.valueOf(2)), false));
        assertEquals(Arrays.asList(Integer.valueOf(1)),
                MuseScoreIo.resolveMuseExportSlurIds(state, 1, 1, 1, Arrays.asList(Integer.valueOf(2)), false));
        assertEquals(false, state.getSlurActiveIdsBySource().containsKey("1:1:1:2"));

        assertEquals(Arrays.asList(Integer.valueOf(3)),
                MuseScoreIo.resolveMuseExportSlurIds(state, 1, 1, 1, Arrays.asList(Integer.valueOf(9)), false));
        assertEquals(4, state.getNextSlurId());
        assertEquals(Arrays.asList(Integer.valueOf(4), Integer.valueOf(5)),
                MuseScoreIo.resolveMuseExportSlurIds(state, 1, 2, 1,
                        Arrays.asList(Integer.valueOf(0), Integer.valueOf(3)), true));
        assertEquals(true, state.getSlurActiveIdsBySource().containsKey("1:2:1:1"));
        assertEquals(true, state.getSlurActiveIdsBySource().containsKey("1:2:1:3"));
    }

    @Test
    public void resolvesMuseScoreExportSlurFractions() {
        MuseScoreIo.MuseScoreExportSlurState state = new MuseScoreIo.MuseScoreExportSlurState();
        Map<Integer, String> active = new HashMap<Integer, String>();
        MuseScoreIo.MuseScoreExportSlurFractions start = MuseScoreIo.resolveMuseExportSlurFractions(state, 1, 1, 1,
                Arrays.asList(Integer.valueOf(1)), null, "1/4", active);
        assertEquals(Arrays.asList("1/4"), start.getSlurStartFractions());
        assertEquals(Arrays.<String>asList(), start.getSlurStopFractions());
        assertEquals("1/4", active.get(Integer.valueOf(1)));

        MuseScoreIo.MuseScoreExportSlurFractions stopAndStart = MuseScoreIo.resolveMuseExportSlurFractions(state, 1,
                1, 1, Arrays.asList(Integer.valueOf(2)), Arrays.asList(Integer.valueOf(1)), "3/8", active);
        assertEquals(Arrays.asList("3/8"), stopAndStart.getSlurStartFractions());
        assertEquals(Arrays.asList("1/4"), stopAndStart.getSlurStopFractions());
        assertEquals(false, active.containsKey(Integer.valueOf(1)));
        assertEquals("3/8", active.get(Integer.valueOf(2)));

        MuseScoreIo.MuseScoreExportSlurFractions unmatchedStop = MuseScoreIo.resolveMuseExportSlurFractions(state, 1,
                1, 1, null, Arrays.asList(Integer.valueOf(9)), "1/8", active);
        assertEquals(Arrays.asList("1/8"), unmatchedStop.getSlurStopFractions());
    }

    @Test
    public void resolvesMuseScoreExportTupletRefs() {
        MuseScoreIo.MuseScoreExportTupletRefState state = new MuseScoreIo.MuseScoreExportTupletRefState();
        MuseScoreIo.TimeModification triplet = new MuseScoreIo.TimeModification(3, 2);

        MuseScoreIo.MuseScoreExportTupletRef start = MuseScoreIo.resolveMuseExportTupletRef(state,
                Arrays.asList(Integer.valueOf(2)), null, triplet, 160);
        assertEquals("<Tuplet id=\"T1\"><normalNotes>2</normalNotes><actualNotes>3</actualNotes></Tuplet>",
                start.getDefinitionXml());
        assertEquals("T1", start.getTupletRefId());
        assertEquals(240, start.getDisplayDurationDiv());
        assertEquals("T1", state.getActiveTupletRefByNumber().get(Integer.valueOf(2)));

        MuseScoreIo.MuseScoreExportTupletRef implicit = MuseScoreIo.resolveMuseExportTupletRef(
                new MuseScoreIo.MuseScoreExportTupletRefState(), null, null, triplet, 160);
        assertEquals("<Tuplet id=\"T1\"><normalNotes>2</normalNotes><actualNotes>3</actualNotes></Tuplet>",
                implicit.getDefinitionXml());
        assertEquals("T1", implicit.getTupletRefId());

        MuseScoreIo.applyMuseExportTupletStops(state, Arrays.asList(Integer.valueOf(2)));
        assertEquals(false, state.getActiveTupletRefByNumber().containsKey(Integer.valueOf(2)));

        MuseScoreIo.resolveMuseExportTupletRef(state, Arrays.asList(Integer.valueOf(4)), null, triplet, 160);
        assertEquals(true, state.getActiveTupletRefByNumber().containsKey(Integer.valueOf(4)));
        MuseScoreIo.resolveMuseExportTupletRef(state, null, null, null, 240);
        assertEquals(true, state.getActiveTupletRefByNumber().isEmpty());
    }

    @Test
    public void buildsMuseScoreExportVoiceXml() {
        MuseScoreIo.MuseVoiceEvent rest = new MuseScoreIo.MuseVoiceEvent(120, 120, null, null, null, null, null,
                null, null, false, null, null, null, 0, false, false, true, false);
        MuseScoreIo.MuseVoiceEvent chord = new MuseScoreIo.MuseVoiceEvent(240, 160,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(60, true, false, "sharp", "2",
                        Integer.valueOf(3))),
                Arrays.asList(Integer.valueOf(1)), Arrays.asList(Integer.valueOf(1)),
                new MuseScoreIo.TimeModification(3, 2), Arrays.asList(Integer.valueOf(1)), null,
                Arrays.asList("articStaccato"), true, null, null, Arrays.asList("8va"), 1, false, false, false,
                true);
        Map<Integer, String> activeSlurs = new HashMap<Integer, String>();
        String xml = MuseScoreIo.buildMuseScoreExportVoiceXml(Arrays.asList(chord, rest), 480, 1, 1, 480, 1,
                new MuseScoreIo.MuseScoreExportSlurState(), activeSlurs);

        assertEquals(true, xml.startsWith("<voice><Rest><durationType>16th</durationType></Rest>"));
        assertEquals(true, xml.contains("<BarLine><subtype>start-repeat</subtype></BarLine>"));
        assertEquals(true, xml.contains("<BarLine><subtype>end-repeat</subtype></BarLine>"));
        assertEquals(true,
                xml.contains("<Tuplet id=\"T1\"><normalNotes>2</normalNotes><actualNotes>3</actualNotes></Tuplet>"));
        assertEquals(true, xml.contains("<Chord><durationType>eighth</durationType><Tuplet>T1</Tuplet>"));
        assertEquals(true, xml.contains("<Spanner type=\"Slur\"><Slur/><next><location><fractions>1/8</fractions>"));
        assertEquals(true, xml.contains("<Articulation><subtype>articStaccato</subtype></Articulation>"));
        assertEquals(true, xml.contains("<Note><pitch>60</pitch><Accidental><subtype>sharp</subtype></Accidental>"));
        assertEquals(true, xml.endsWith("<Rest><durationType>32nd</durationType></Rest></voice>"));
        assertEquals("1/8", activeSlurs.get(Integer.valueOf(1)));
    }

    @Test
    public void buildsMuseScoreExportMeasureVoiceXml() {
        MuseScoreIo.MuseScoreExportMeasureContext context = new MuseScoreIo.MuseScoreExportMeasureContext(2, 2, "cut",
                -1, "F", null, true, true, true, false, null, true, true);
        MuseScoreIo.MuseVoiceEvent chord = new MuseScoreIo.MuseVoiceEvent(0, 240,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(55, false, false, null, null, null)), null, null,
                null, Arrays.asList(Integer.valueOf(1)), null, null, false, null, null, null, 0, false, false,
                false, false);
        Map<Integer, Map<Integer, String>> activeSlursByVoice = new HashMap<Integer, Map<Integer, String>>();

        MuseScoreIo.MuseScoreExportMeasureVoiceXml first = MuseScoreIo.buildMuseScoreExportMeasureVoiceXml(context,
                Arrays.asList(chord), 240, 1, 0, 1, 480, 1, null, new MuseScoreIo.MuseScoreExportSlurState(),
                activeSlursByVoice);
        assertEquals(true, first.getXml().startsWith("<voice><Clef><concertClefType>F</concertClefType></Clef>"));
        assertEquals(true, first.getXml().contains("<TimeSig><subtype>2</subtype><sigN>2</sigN><sigD>2</sigD></TimeSig>"));
        assertEquals(true,
                first.getXml().contains("<KeySig><accidental>-1</accidental><concertKey>-1</concertKey></KeySig>"));
        assertEquals(true, first.getXml().endsWith("<endRepeat/></voice>"));
        assertEquals("F", first.getTargetClef());
        assertEquals(true, activeSlursByVoice.containsKey(Integer.valueOf(1)));

        MuseScoreIo.MuseScoreExportMeasureVoiceXml second = MuseScoreIo.buildMuseScoreExportMeasureVoiceXml(context,
                null, 120, 2, 1, 1, 480, 1, null, new MuseScoreIo.MuseScoreExportSlurState(), activeSlursByVoice);
        assertEquals("<voice><Rest><durationType>16th</durationType></Rest></voice>", second.getXml());
        assertNull(second.getTargetClef());
        assertEquals("abc", MuseScoreIo.stripMuseVoiceWrapper("<voice>abc</voice>"));
    }

    @Test
    public void appliesMuseScoreExportStaffState() {
        MuseScoreIo.MuseScoreExportStaffState initial = MuseScoreIo.createInitialMuseScoreExportStaffState(480, 0, 0,
                12, "G");
        assertEquals(480, initial.getCurrentSourceDivisions());
        assertEquals(1, initial.getCurrentBeats());
        assertEquals(1, initial.getCurrentBeatType());
        assertEquals(7, initial.getCurrentFifths());
        assertEquals("G", initial.getCurrentClef());

        MuseScoreIo.MuseScoreExportMeasureContext context = new MuseScoreIo.MuseScoreExportMeasureContext(960, 6, 8,
                null, -8, "F", null, false, false, false, false, null, false, false);
        MuseScoreIo.MuseScoreExportStaffState next = MuseScoreIo.applyMuseScoreExportMeasureState(initial, context,
                null);
        assertEquals(960, next.getCurrentSourceDivisions());
        assertEquals(6, next.getCurrentBeats());
        assertEquals(8, next.getCurrentBeatType());
        assertEquals(-7, next.getCurrentFifths());
        assertEquals("G", next.getCurrentClef());

        MuseScoreIo.MuseScoreExportMeasureContext fallbackDivisions = new MuseScoreIo.MuseScoreExportMeasureContext(3,
                4, "cut", 2, "C", null, false, false, false, false, null, false, false);
        MuseScoreIo.MuseScoreExportStaffState fallback = MuseScoreIo.applyMuseScoreExportMeasureState(next,
                fallbackDivisions, "G");
        assertEquals(960, fallback.getCurrentSourceDivisions());
        assertEquals("cut", fallback.getCurrentTimeSymbol());
        assertEquals("G", fallback.getCurrentClef());
    }

    @Test
    public void buildsMuseScoreExportMeasureContextFromValues() {
        MuseScoreIo.MuseVoiceEvent grace = new MuseScoreIo.MuseVoiceEvent(30, 500,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(72, false, false, null, null, null)), null, null,
                null, null, null, null, false, null, null, null, 0, true, false, false, false);
        MuseScoreIo.MuseVoiceEvent timed = new MuseScoreIo.MuseVoiceEvent(120, 360,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(60, false, false, null, null, null)), null, null,
                null, null, null, null, false, null, null, null, 0, false, false, false, false);
        Map<Integer, List<MuseScoreIo.MuseVoiceEvent>> byVoice = new HashMap<Integer, List<MuseScoreIo.MuseVoiceEvent>>();
        byVoice.put(Integer.valueOf(2), Arrays.asList(grace));
        byVoice.put(Integer.valueOf(1), Arrays.asList(timed));

        MuseScoreIo.MuseScoreExportMeasureContextResult result = MuseScoreIo
                .buildMuseScoreExportMeasureContextFromValues(960, byVoice, 1, 480, 4, 4, null, 0, "G", 4, 4,
                        " cut ", true, 9, "F", "F", true, true, true, false,
                        Arrays.asList(MuseScoreIo.MuseDirectionSeed.dynamic("mf", Integer.valueOf(90))), true, true);
        assertEquals(960, result.getMeasureContext().getMeasureSourceDivisions());
        assertEquals(2, result.getMeasureContext().getEffectiveMeasureBeats());
        assertEquals(2, result.getMeasureContext().getEffectiveMeasureBeatType());
        assertEquals("cut", result.getMeasureContext().getMeasureTimeSymbol());
        assertEquals(7, result.getMeasureContext().getMeasureFifths());
        assertEquals("F", result.getMeasureContext().getTargetClef());
        assertEquals(true, result.getMeasureContext().isShouldWriteClef());
        assertEquals(true, result.getMeasureContext().isShouldWriteTime());
        assertEquals(true, result.getMeasureContext().isShouldWriteKey());
        assertEquals(true, result.getMeasureContext().isNeedsDoubleBarlineAtMeasureStart());
        assertEquals(true, result.getMeasureContext().isHasStartRepeat());
        assertEquals(true, result.getMeasureContext().isHasEndRepeat());
        assertEquals(1920, result.getCapacityDiv());
        assertEquals(480, result.getUsedDiv());
        assertEquals(480, result.getRenderCapacityDiv());
        assertEquals("1/4", result.getLenAttr());
        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)), result.getVoiceNos());

        assertEquals(Arrays.asList(Integer.valueOf(1)), MuseScoreIo.resolveMuseScoreExportVoiceNos(null));
    }

    @Test
    public void computesMuseScoreExportSourceAnalysisHelpers() {
        assertEquals(480, MuseScoreIo.computeGlobalMusicXmlDivisionsFromValues(null));
        assertEquals(240, MuseScoreIo.computeGlobalMusicXmlDivisionsFromValues(
                Arrays.asList(Integer.valueOf(120), Integer.valueOf(240), Integer.valueOf(-1), null)));
        assertEquals(3840, MuseScoreIo.computeGlobalMusicXmlDivisionsFromValues(
                Arrays.asList(Integer.valueOf(3072), Integer.valueOf(3125))));
        assertEquals(120, MuseScoreIo.lcmForMuseScoreExportDivisions(24, 40));
        assertEquals(1, MuseScoreIo.getMeasureStaffCountFromMusicXmlValues(null, null));
        assertEquals(3, MuseScoreIo.getMeasureStaffCountFromMusicXmlValues(Integer.valueOf(2),
                Arrays.asList("1", " 3.2 ", "bad", null)));
        assertEquals(4, MuseScoreIo.getPartStaffCountFromMusicXmlValues(
                Arrays.asList(Integer.valueOf(1), Integer.valueOf(4), null, Integer.valueOf(-2))));
        assertEquals(1, MuseScoreIo.getPartStaffCountFromMusicXmlValues(null));

        Map<String, MuseScoreIo.MuseScoreExportPartName> map = MuseScoreIo.readPartNameMapFromMusicXmlParts(
                Arrays.asList(new MuseScoreIo.MuseScoreExportPartNameEntry(" P1 ", " Piano ", " Pno "),
                        new MuseScoreIo.MuseScoreExportPartNameEntry("", "Ignored", "I"),
                        new MuseScoreIo.MuseScoreExportPartNameEntry("P2", "", null)));
        assertEquals("Piano", map.get("P1").getName());
        assertEquals("Pno", map.get("P1").getAbbreviation());
        assertEquals("P2", map.get("P2").getName());
        assertEquals("", map.get("P2").getAbbreviation());
        assertEquals(false, map.containsKey(""));
    }

    @Test
    public void readsMuseScoreExportMetadataFromValues() {
        List<MuseScoreIo.MuseScoreExportCredit> credits = Arrays.asList(
                new MuseScoreIo.MuseScoreExportCredit("title", "Ignored"),
                new MuseScoreIo.MuseScoreExportCredit(" subtitle ", "  Sub Title  "));
        List<MuseScoreIo.MuseScoreExportCreator> creators = Arrays.asList(
                new MuseScoreIo.MuseScoreExportCreator("", "Generic"),
                new MuseScoreIo.MuseScoreExportCreator("composer", " Composer "),
                new MuseScoreIo.MuseScoreExportCreator("arranger", " Arranger "),
                new MuseScoreIo.MuseScoreExportCreator("lyricist", " Lyricist "),
                new MuseScoreIo.MuseScoreExportCreator("translator", " Translator "));
        MuseScoreIo.MuseScoreExportMetadata metadata = MuseScoreIo.readMusicXmlExportMetadataFromValues(" Work ",
                "Movement", " Op.1 ", " 1 ", credits, creators, " Rights ", " 2026-05-09 ");
        assertEquals("Work", metadata.getTitle());
        assertEquals("Sub Title", metadata.getSubtitle());
        assertEquals("Composer", metadata.getComposer());
        assertEquals("Arranger", metadata.getArranger());
        assertEquals("Lyricist", metadata.getLyricist());
        assertEquals("Translator", metadata.getTranslator());
        assertEquals("Rights", metadata.getRights());
        assertEquals("Op.1", metadata.getWorkNumber());
        assertEquals("Movement", metadata.getMovementTitle());
        assertEquals("1", metadata.getMovementNumber());
        assertEquals("2026-05-09", metadata.getCreationDate());

        MuseScoreIo.MuseScoreExportMetadata fallback = MuseScoreIo.readMusicXmlExportMetadataFromValues("", " Move ",
                null, null, null, Arrays.asList(new MuseScoreIo.MuseScoreExportCreator("", " Generic ")), null,
                null);
        assertEquals("Move", fallback.getTitle());
        assertEquals("", fallback.getSubtitle());
        assertEquals("Generic", fallback.getComposer());

        MuseScoreIo.MuseScoreExportMetadata defaultTitle = MuseScoreIo.readMusicXmlExportMetadataFromValues("", "",
                null, null, null, null, null, null);
        assertEquals("miku-score export", defaultTitle.getTitle());
    }

    @Test
    public void buildsMuseScoreExportStaffXml() {
        MuseScoreIo.MuseScoreExportMeasureContext firstContext = new MuseScoreIo.MuseScoreExportMeasureContext(480, 4,
                4, null, 0, "G", null, true, false, false, false, null, false, false);
        MuseScoreIo.MuseVoiceEvent firstChord = new MuseScoreIo.MuseVoiceEvent(0, 240,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(60, false, false, null, null, null)), null, null,
                null, Arrays.asList(Integer.valueOf(1)), null, null, false, null, null, null, 0, false, false,
                false, false);
        Map<Integer, List<MuseScoreIo.MuseVoiceEvent>> firstEvents = new HashMap<Integer, List<MuseScoreIo.MuseVoiceEvent>>();
        firstEvents.put(Integer.valueOf(1), Arrays.asList(firstChord));
        MuseScoreIo.MuseScoreExportStaffMeasure firstMeasure = new MuseScoreIo.MuseScoreExportStaffMeasure(firstContext,
                "1/4", 240, Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)), firstEvents);

        MuseScoreIo.MuseScoreExportMeasureContext secondContext = new MuseScoreIo.MuseScoreExportMeasureContext(480, 4,
                4, null, 0, "F", null, false, false, false, false, null, false, false);
        MuseScoreIo.MuseVoiceEvent stopChord = new MuseScoreIo.MuseVoiceEvent(0, 240,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(62, false, false, null, null, null)), null, null,
                null, null, Arrays.asList(Integer.valueOf(1)), null, false, null, null, null, 0, false, false,
                false, false);
        Map<Integer, List<MuseScoreIo.MuseVoiceEvent>> secondEvents = new HashMap<Integer, List<MuseScoreIo.MuseVoiceEvent>>();
        secondEvents.put(Integer.valueOf(1), Arrays.asList(stopChord));
        MuseScoreIo.MuseScoreExportStaffMeasure secondMeasure = new MuseScoreIo.MuseScoreExportStaffMeasure(
                secondContext, null, 240, Arrays.asList(Integer.valueOf(1)), secondEvents);

        String xml = MuseScoreIo.buildMuseScoreExportStaffXml(7, Arrays.asList(firstMeasure, secondMeasure), 1, 480, 1,
                null, new MuseScoreIo.MuseScoreExportSlurState(),
                MuseScoreIo.createInitialMuseScoreExportStaffState(480, 4, 4, 0, "G"));
        assertEquals(true, xml.startsWith("<Staff id=\"7\"><Measure len=\"1/4\"><voice><Clef>"));
        assertEquals(true, xml.contains("<voice><Rest><durationType>eighth</durationType></Rest></voice>"));
        assertEquals(true, xml.contains("<Spanner type=\"Slur\"><Slur/><next><location><fractions>1/8</fractions>"));
        assertEquals(true, xml.contains("<Spanner type=\"Slur\"><prev><location><fractions>-1/8</fractions>"));
        assertEquals(true, xml.endsWith("</Measure></Staff>"));
    }

    @Test
    public void buildsMuseScoreExportPartResult() {
        Map<Integer, String> clefs = new HashMap<Integer, String>();
        clefs.put(Integer.valueOf(1), "G");
        clefs.put(Integer.valueOf(2), "F");
        assertEquals(
                "<Instrument><trackName>Piano &amp; Keys</trackName><longName>Piano &amp; Keys</longName><shortName>Pno</shortName><clef>G</clef><clef staff=\"2\">F</clef><transposeDiatonic>1</transposeDiatonic><transposeChromatic>2</transposeChromatic></Instrument>",
                MuseScoreIo.buildMuseScoreExportInstrumentXml("Piano & Keys", "Pno", clefs,
                        new MuseScoreIo.Transpose(Integer.valueOf(1), Integer.valueOf(2))));
        assertEquals(
                "<Staff><defaultClef>G</defaultClef></Staff><Staff><defaultClef>F</defaultClef></Staff><trackName>Piano &amp; Keys</trackName><Instrument><trackName>Piano &amp; Keys</trackName><longName>Piano &amp; Keys</longName><shortName>Pno</shortName><clef>G</clef><clef staff=\"2\">F</clef></Instrument>",
                MuseScoreIo.buildMuseScoreExportPartDefBodyXml("Piano & Keys", "Pno",
                        Arrays.asList(Integer.valueOf(5), Integer.valueOf(6)), clefs, null));

        MuseScoreIo.Transpose bFlatInstrumentTranspose = new MuseScoreIo.Transpose(Integer.valueOf(-2),
                Integer.valueOf(-3));
        assertEquals("<Instrument><trackName>Clarinet</trackName><longName>Clarinet</longName><shortName>Cl.</shortName><clef>G</clef><transposeDiatonic>-2</transposeDiatonic><transposeChromatic>-3</transposeChromatic></Instrument>",
                MuseScoreIo.buildMuseScoreExportInstrumentXml("Clarinet", "Cl.", Collections.singletonMap(
                        Integer.valueOf(1), "G"), bFlatInstrumentTranspose));
        assertEquals("<KeySig><accidental>0</accidental><concertKey>3</concertKey><transposeKey>0</transposeKey></KeySig>",
                MuseScoreIo.resolveMuseExportKeySigXml(0, bFlatInstrumentTranspose));
        assertEquals("<transpose><diatonic>-2</diatonic><chromatic>-3</chromatic></transpose>",
                MuseScoreIo.buildTransposeXml(MuseScoreIo.readPartTransposeFromValues("-2", "-3")));

        MuseScoreIo.MuseScoreExportMeasureContext context = new MuseScoreIo.MuseScoreExportMeasureContext(480, 4, 4,
                null, 0, "G", null, false, false, false, false, null, false, false);
        MuseScoreIo.MuseScoreExportStaffMeasure measure = new MuseScoreIo.MuseScoreExportStaffMeasure(context, null,
                120, Arrays.asList(Integer.valueOf(1)), null);
        Map<Integer, Collection<MuseScoreIo.MuseScoreExportStaffMeasure>> byStaff = new HashMap<Integer, Collection<MuseScoreIo.MuseScoreExportStaffMeasure>>();
        byStaff.put(Integer.valueOf(1), Arrays.asList(measure));
        byStaff.put(Integer.valueOf(2), Arrays.asList(measure));
        MuseScoreIo.MuseScoreExportPartResult result = MuseScoreIo.buildMuseScoreExportPartResult(3, "Piano", "Pno",
                5, null, byStaff, 2, 480, clefs, null, new MuseScoreIo.MuseScoreExportSlurState());
        assertEquals(7, result.getNextStaffId());
        assertEquals(true, result.getPartDefXml().startsWith("<Part id=\"3\"><Staff><defaultClef>G</defaultClef>"));
        assertEquals(2, result.getStaffsXml().size());
        assertEquals(true, result.getStaffsXml().get(0).startsWith("<Staff id=\"5\">"));
        assertEquals(true, result.getStaffsXml().get(1).startsWith("<Staff id=\"6\">"));
    }

    @Test
    public void resolvesMuseScoreExportPartIdentityAndScaffold() {
        Map<String, MuseScoreIo.MuseScoreExportPartName> names = new HashMap<String, MuseScoreIo.MuseScoreExportPartName>();
        names.put("P1", new MuseScoreIo.MuseScoreExportPartName("  Grand Piano  ", " Pno "));
        MuseScoreIo.MuseScoreExportPartIdentity named = MuseScoreIo.resolveMuseScoreExportPartIdentity(" P1 ", 1,
                names);
        assertEquals("Grand Piano", named.getPartName());
        assertEquals("Pno", named.getPartAbbreviation());
        MuseScoreIo.MuseScoreExportPartIdentity fallback = MuseScoreIo.resolveMuseScoreExportPartIdentity("", 3,
                names);
        assertEquals("P3", fallback.getPartName());
        assertEquals("", fallback.getPartAbbreviation());

        Map<Integer, String> explicitClefs = new HashMap<Integer, String>();
        explicitClefs.put(Integer.valueOf(2), "F");
        Map<Integer, Collection<Integer>> pitchesByStaff = new HashMap<Integer, Collection<Integer>>();
        pitchesByStaff.put(Integer.valueOf(1), Arrays.asList(Integer.valueOf(45), Integer.valueOf(48)));
        MuseScoreIo.MuseScoreExportPartScaffold scaffold = MuseScoreIo.buildMuseScoreExportPartScaffold("Solo Viola",
                "Vla", 8, 3, new MuseScoreIo.Transpose(null, Integer.valueOf(-12)), explicitClefs,
                pitchesByStaff);
        assertEquals(Arrays.asList(Integer.valueOf(8), Integer.valueOf(9), Integer.valueOf(10)),
                scaffold.getStaffIds());
        assertEquals("C", scaffold.getInitialClefByStaff().get(Integer.valueOf(1)));
        assertEquals("F", scaffold.getInitialClefByStaff().get(Integer.valueOf(2)));
        assertEquals("G", scaffold.getInitialClefByStaff().get(Integer.valueOf(3)));
        assertEquals(true, scaffold.getPartDefBodyXml().contains("<Staff><defaultClef>C3</defaultClef></Staff>"));
        assertEquals(true, scaffold.getPartDefBodyXml().contains("<transposeChromatic>-12</transposeChromatic>"));
    }

    @Test
    public void buildsMuseScoreExportDocumentBodyAndEmptyXml() {
        MuseScoreIo.MuseScoreExportPartResult first = new MuseScoreIo.MuseScoreExportPartResult(2,
                "<Part id=\"1\"><trackName>P1</trackName></Part>", Arrays.asList("<Staff id=\"1\"/>"));
        MuseScoreIo.MuseScoreExportPartResult second = new MuseScoreIo.MuseScoreExportPartResult(3,
                "<Part id=\"2\"><trackName>P2</trackName></Part>",
                Arrays.asList("<Staff id=\"2\"/>", "<Staff id=\"3\"/>"));
        MuseScoreIo.MuseScoreExportDocumentBody body = MuseScoreIo
                .buildMuseScoreExportDocumentBody(Arrays.asList(first, null, second));
        assertEquals(Arrays.asList("<Part id=\"1\"><trackName>P1</trackName></Part>",
                "<Part id=\"2\"><trackName>P2</trackName></Part>"), body.getPartDefs());
        assertEquals(Arrays.asList("<Staff id=\"1\"/>", "<Staff id=\"2\"/>", "<Staff id=\"3\"/>"),
                body.getStaffsXml());

        MuseScoreIo.MuseScoreExportMetadata metadata = new MuseScoreIo.MuseScoreExportMetadata("Doc", null, null,
                null, null, null, null, null, null, null, null);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Doc</metaTag><Division>480</Division><Part id=\"1\"><trackName>P1</trackName></Part><Part id=\"2\"><trackName>P2</trackName></Part><Staff id=\"1\"/><Staff id=\"2\"/><Staff id=\"3\"/></Score></museScore>",
                MuseScoreIo.buildMuseScoreExportDocumentXml(metadata, 480, body));

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Empty &amp; Score</metaTag><Division>480</Division><Part><trackName>P1</trackName><Staff id=\"1\"/></Part><Staff id=\"1\"><Measure><voice><Rest><durationType>whole</durationType></Rest></voice></Measure></Staff></Score></museScore>",
                MuseScoreIo.buildEmptyMuseScoreExportXml(480, "Empty & Score"));
    }

    @Test
    public void buildsMuseScoreExportXmlFromPartsOrEmptyFallback() {
        MuseScoreIo.MuseScoreExportMetadata metadata = new MuseScoreIo.MuseScoreExportMetadata("Score", null, null,
                null, null, null, null, null, null, null, null);
        MuseScoreIo.MuseScoreExportPartResult part = new MuseScoreIo.MuseScoreExportPartResult(2,
                "<Part id=\"1\"><trackName>P1</trackName></Part>", Arrays.asList("<Staff id=\"1\"/>"));
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Score</metaTag><Division>480</Division><Part id=\"1\"><trackName>P1</trackName></Part><Staff id=\"1\"/></Score></museScore>",
                MuseScoreIo.buildMuseScoreExportXmlFromParts(metadata, 480, Arrays.asList(part)));
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Score</metaTag><Division>480</Division><Part><trackName>P1</trackName><Staff id=\"1\"/></Part><Staff id=\"1\"><Measure><voice><Rest><durationType>whole</durationType></Rest></voice></Measure></Staff></Score></museScore>",
                MuseScoreIo.buildMuseScoreExportXmlFromParts(metadata, 480, null));
    }

    @Test
    public void buildsMuseScoreExportMetadataXml() {
        MuseScoreIo.MuseScoreExportMetadata metadata = new MuseScoreIo.MuseScoreExportMetadata("Work & Title",
                "Sub", "Composer <A>", "Arranger", "Lyricist", "Translator", "Copyright", "Op.1", "Move",
                "I", "2026-05-09");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Work &amp; Title</metaTag><metaTag name=\"subtitle\">Sub</metaTag><metaTag name=\"composer\">Composer &lt;A&gt;</metaTag><metaTag name=\"arranger\">Arranger</metaTag><metaTag name=\"lyricist\">Lyricist</metaTag><metaTag name=\"translator\">Translator</metaTag><metaTag name=\"copyright\">Copyright</metaTag><metaTag name=\"workNumber\">Op.1</metaTag><metaTag name=\"movementTitle\">Move</metaTag><metaTag name=\"movementNumber\">I</metaTag><metaTag name=\"creationDate\">2026-05-09</metaTag><Division>480</Division>",
                MuseScoreIo.buildMuseScoreExportMetadataXml(metadata, 480));

        MuseScoreIo.MuseScoreExportMetadata minimal = new MuseScoreIo.MuseScoreExportMetadata("Untitled", "", null,
                null, null, null, null, null, null, null, null);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">Untitled</metaTag><Division>960</Division>",
                MuseScoreIo.buildMuseScoreExportMetadataXml(minimal, 960));
    }

    @Test
    public void mapsArticulationAndOrnamentSubtypes() {
        MuseScoreIo.NotationTag staccato = MuseScoreIo.museArticulationSubtypeToMusicXmlTag("articStaccato");
        assertEquals("articulations", staccato.getGroup());
        assertEquals("staccato", staccato.getTag());

        MuseScoreIo.NotationTag upBow = MuseScoreIo.museArticulationSubtypeToMusicXmlTag("stringsUpBow");
        assertEquals("technical", upBow.getGroup());
        assertEquals("up-bow", upBow.getTag());

        MuseScoreIo.NotationTag stopped = MuseScoreIo.museOrnamentSubtypeToMusicXmlTag("brassMuteClosed");
        assertEquals("technical", stopped.getGroup());
        assertEquals("stopped", stopped.getTag());
    }

    @Test
    public void parsesKeyModeHelpers() {
        assertEquals("major", MuseScoreIo.normalizeKeyMode("maj"));
        assertEquals("minor", MuseScoreIo.normalizeKeyMode("1"));
        assertNull(MuseScoreIo.normalizeKeyMode("dorian"));
        assertEquals("minor", MuseScoreIo.inferKeyModeFromText("A minor"));
        assertEquals("major", MuseScoreIo.inferKeyModeFromText("ハ長調"));
    }

    @Test
    public void buildsSmallMusicXmlDirectionHelpers() {
        assertEquals("<direction><direction-type><dynamics><mf/></dynamics></direction-type><sound dynamics=\"100.00\"/></direction>",
                MuseScoreIo.buildDynamicDirectionXml("mf", Double.valueOf(100.0d)));
        assertEquals("<direction placement=\"above\"><direction-type><words font-style=\"italic\">A&amp;B</words></direction-type><sound tempo=\"120\"/></direction>",
                MuseScoreIo.buildWordsDirectionXml("A&B", "above", Integer.valueOf(120), "italic"));
        assertEquals("<direction><direction-type><segno/></direction-type></direction>",
                MuseScoreIo.buildSegnoDirectionXml());
        assertEquals("<direction><direction-type><coda/></direction-type></direction>",
                MuseScoreIo.buildCodaDirectionXml());
    }

    @Test
    public void mapsKeyClefTransposeAndMeasureLengthHelpers() {
        assertEquals(7, MuseScoreIo.normalizeKeyFifthsToMuseRange(19));
        assertEquals(-5, MuseScoreIo.normalizeKeyFifthsToMuseRange(-17));
        assertEquals("<transpose><diatonic>-1</diatonic><chromatic>-2</chromatic></transpose>",
                MuseScoreIo.buildTransposeXml(new MuseScoreIo.Transpose(Integer.valueOf(-1), Integer.valueOf(-2))));

        MuseScoreIo.Transpose parsedTranspose = MuseScoreIo.readPartTransposeFromValues(" -2.2 ", " -3 ");
        assertEquals(Integer.valueOf(-2), parsedTranspose.getDiatonic());
        assertEquals(Integer.valueOf(-3), parsedTranspose.getChromatic());
        assertNull(MuseScoreIo.readPartTransposeFromValues("", "bad"));
        assertEquals(Integer.valueOf(0), MuseScoreIo.readMuseKeyFifthsFromValues("0", "4", "3", true));
        assertEquals(Integer.valueOf(4), MuseScoreIo.readMuseKeyFifthsFromValues("0", "4", "3", false));
        assertEquals(Integer.valueOf(7), MuseScoreIo.readMuseKeyFifthsFromValues("9", null, null, true));

        MuseScoreIo.Clef tenor = MuseScoreIo.parseMuseClefText("tenor");
        assertEquals("C", tenor.getSign());
        assertEquals(4, tenor.getLine());

        MuseScoreIo.Clef explicit = MuseScoreIo.parseMuseClefText("F3");
        assertEquals("F", explicit.getSign());
        assertEquals(3, explicit.getLine());

        assertEquals("3/4", MuseScoreIo.formatMeasureLenFromDivisions(1440, 480));
    }

    @Test
    public void addsDirectionStaffAndVoicePlacement() {
        assertEquals("<direction><direction-type><segno/></direction-type><staff>2</staff></direction>",
                MuseScoreIo.withDirectionStaff("<direction><direction-type><segno/></direction-type></direction>", 2));
        assertEquals("<direction><direction-type><segno/></direction-type><staff>2</staff><voice>3</voice></direction>",
                MuseScoreIo.withDirectionPlacement("<direction><direction-type><segno/></direction-type></direction>",
                        2, 3));
        assertEquals("<direction><direction-type><octave-shift type=\"start\"/></direction-type><staff>1</staff></direction>",
                MuseScoreIo.withDirectionPlacement(
                        "<direction><direction-type><octave-shift type=\"start\"/></direction-type></direction>", 1,
                        4));
    }

    @Test
    public void buildsTupletNotationXml() {
        MuseScoreIo.TupletMusicXml xml = MuseScoreIo.buildTupletMusicXml(
                new MuseScoreIo.TimeModification(3, 2),
                Arrays.asList(new MuseScoreIo.TupletStart(1, "actual", "yes")), Arrays.asList(Integer.valueOf(1)));
        assertEquals("<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>",
                xml.getTimeModificationXml());
        assertEquals("<tuplet type=\"start\" number=\"1\" bracket=\"yes\" show-number=\"actual\"/>",
                xml.getNotationItems().get(0));
        assertEquals("<tuplet type=\"stop\" number=\"1\"/>", xml.getNotationItems().get(1));
    }

    @Test
    public void computesTupletToleranceAndBeamLevels() {
        assertEquals(1, MuseScoreIo.tupletRoundingToleranceByTimedEvents(Arrays.asList(
                new MuseScoreIo.TimedEvent(160, false, true),
                new MuseScoreIo.TimedEvent(160, true, true),
                new MuseScoreIo.TimedEvent(160, false, true))));
        assertEquals(0, MuseScoreIo.beamLevelFromType("quarter"));
        assertEquals(1, MuseScoreIo.beamLevelFromType("eighth"));
        assertEquals(3, MuseScoreIo.beamLevelFromType("32nd"));
    }

    @Test
    public void buildsMuseBeamXmlFromExplicitBeamModeChain() {
        Map<Integer, String> beams = MuseScoreIo.buildMuseBeamXmlByEventInfo(Arrays.asList(
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, "begin"),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null)), 480, false);

        assertEquals("<beam number=\"1\">begin</beam><beam number=\"2\">begin</beam>", beams.get(Integer.valueOf(0)));
        assertEquals("<beam number=\"1\">continue</beam><beam number=\"2\">continue</beam>",
                beams.get(Integer.valueOf(1)));
        assertEquals("<beam number=\"1\">end</beam><beam number=\"2\">end</beam>", beams.get(Integer.valueOf(2)));
    }

    @Test
    public void includesPrecedingChordWhenMuseRestStartsWithBeamModeMid() {
        Map<Integer, String> beams = MuseScoreIo.buildMuseBeamXmlByEventInfo(Arrays.asList(
                new MuseScoreIo.MuseBeamEvent(true, true, false, 240, 1, null),
                new MuseScoreIo.MuseBeamEvent(true, false, false, 120, 2, "mid"),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null)), 480, false);

        assertEquals("<beam number=\"1\">begin</beam>", beams.get(Integer.valueOf(0)));
        assertNull(beams.get(Integer.valueOf(1)));
        assertEquals("<beam number=\"1\">end</beam><beam number=\"2\">end</beam>", beams.get(Integer.valueOf(2)));
        assertEquals("<beam number=\"1\">begin</beam><beam number=\"2\">begin</beam>", beams.get(Integer.valueOf(3)));
        assertEquals("<beam number=\"1\">end</beam><beam number=\"2\">end</beam>", beams.get(Integer.valueOf(4)));
    }

    @Test
    public void skipsImplicitMuseBeamInferenceWhenDisabled() {
        Map<Integer, String> beams = MuseScoreIo.buildMuseBeamXmlByEventInfo(Arrays.asList(
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 120, 2, null)), 480, false);

        assertEquals(true, beams.isEmpty());
    }

    @Test
    public void infersMuseBeamsAtBeatBoundariesWhenBeamModeIsAbsent() {
        Map<Integer, String> beams = MuseScoreIo.buildMuseBeamXmlByEventInfo(Arrays.asList(
                new MuseScoreIo.MuseBeamEvent(true, true, false, 240, 1, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 240, 1, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 240, 1, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 240, 1, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 240, 1, null),
                new MuseScoreIo.MuseBeamEvent(true, true, false, 240, 1, null)), 720, true);

        assertEquals("<beam number=\"1\">begin</beam>", beams.get(Integer.valueOf(0)));
        assertEquals("<beam number=\"1\">continue</beam>", beams.get(Integer.valueOf(1)));
        assertEquals("<beam number=\"1\">end</beam>", beams.get(Integer.valueOf(2)));
        assertEquals("<beam number=\"1\">begin</beam>", beams.get(Integer.valueOf(3)));
        assertEquals("<beam number=\"1\">continue</beam>", beams.get(Integer.valueOf(4)));
        assertEquals("<beam number=\"1\">end</beam>", beams.get(Integer.valueOf(5)));
    }

    @Test
    public void buildsMuseImportedBeamInfoAndReadsByEventIndex() {
        List<MuseScoreIo.MuseBeamEvent> events = Arrays.asList(
                MuseScoreIo.buildMuseImportedBeamEventInfo(true, true, false, 120, Integer.valueOf(120), 480,
                        "begin"),
                MuseScoreIo.buildMuseImportedBeamEventInfo(true, false, false, 120, Integer.valueOf(120), 480,
                        "mid"),
                MuseScoreIo.buildMuseImportedBeamEventInfo(true, true, false, 120, Integer.valueOf(120), 480, null),
                MuseScoreIo.buildMuseImportedBeamEventInfo(false, false, false, 0, null, 480, null));

        Map<Integer, String> beams = MuseScoreIo.buildMuseBeamXmlByEventInfo(events, 480, false);

        assertEquals("<beam number=\"1\">begin</beam><beam number=\"2\">begin</beam>",
                MuseScoreIo.readMuseImportedBeamXmlByEventIndex(beams, 0));
        assertEquals("", MuseScoreIo.readMuseImportedBeamXmlByEventIndex(beams, 1));
        assertEquals("<beam number=\"1\">end</beam><beam number=\"2\">end</beam>",
                MuseScoreIo.readMuseImportedBeamXmlByEventIndex(beams, 2));
        assertEquals("", MuseScoreIo.readMuseImportedBeamXmlByEventIndex(beams, 99));
    }

    @Test
    public void parsesOttavaHelpers() {
        MuseScoreIo.OttavaShift down = MuseScoreIo.parseOttavaSubtype("15mb");
        assertEquals(15, down.getSize());
        assertEquals("down", down.getShiftType());

        MuseScoreIo.OttavaState state = new MuseScoreIo.OttavaState(2, down.getSize(), down.getShiftType());
        assertEquals("<direction placement=\"below\"><direction-type><octave-shift type=\"start\" size=\"15\" number=\"2\"/></direction-type></direction>",
                MuseScoreIo.buildOctaveShiftDirectionXml("start", state));
        assertEquals(-24, MuseScoreIo.semitoneShiftForOttavaDisplay(state));

        assertEquals("8vb", MuseScoreIo.parseMusicXmlOctaveShiftSubtype("down", "8"));
        assertEquals("15mb", MuseScoreIo.parseMusicXmlOctaveShiftSubtype("down", "15"));
        assertEquals("8va", MuseScoreIo.parseMusicXmlOctaveShiftSubtype("up", ""));
        assertEquals("15ma", MuseScoreIo.parseMusicXmlOctaveShiftSubtype("up", "16"));
        assertNull(MuseScoreIo.parseMusicXmlOctaveShiftSubtype("stop", "8"));
        assertNull(MuseScoreIo.parseMusicXmlOctaveShiftSubtype("continue", "8"));
    }

    @Test
    public void appliesMuseOttavaSpannerStartStopWithSharedNumber() {
        List<MuseScoreIo.OttavaState> active = new ArrayList<MuseScoreIo.OttavaState>();
        MuseScoreIo.MutableInt next = new MuseScoreIo.MutableInt(1);

        List<String> start = MuseScoreIo.applyMuseOttavaSpannerTransition("8va", true, false, active, next);
        List<String> stop = MuseScoreIo.applyMuseOttavaSpannerTransition(null, false, true, active, next);

        assertEquals("<direction placement=\"above\"><direction-type><octave-shift type=\"start\" size=\"8\" number=\"1\"/></direction-type></direction>",
                start.get(0));
        assertEquals("<direction placement=\"above\"><direction-type><octave-shift type=\"stop\" size=\"8\" number=\"1\"/></direction-type></direction>",
                stop.get(0));
        assertEquals(2, next.getCurrent());
        assertEquals(0, active.size());
    }

    @Test
    public void appliesMuseOttavaDisplayShiftToMidi() {
        List<MuseScoreIo.OttavaState> active = new ArrayList<MuseScoreIo.OttavaState>();
        MuseScoreIo.applyMuseOttavaSpannerTransition("8va", true, false, active, new MuseScoreIo.MutableInt(1));

        assertEquals(93, MuseScoreIo.applyActiveOttavaDisplayShiftToMidi(81, active));
        assertEquals(12, MuseScoreIo.semitoneShiftForActiveOttavaDisplay(active));
    }

    @Test
    public void keepsMuseOttavaDisplayShiftAcrossMeasureBoundary() {
        List<MuseScoreIo.OttavaState> active = new ArrayList<MuseScoreIo.OttavaState>();
        MuseScoreIo.MutableInt next = new MuseScoreIo.MutableInt(1);

        MuseScoreIo.applyMuseOttavaSpannerTransition("8va", true, false, active, next);
        int firstMeasure = MuseScoreIo.applyActiveOttavaDisplayShiftToMidi(60, active);
        int secondMeasure = MuseScoreIo.applyActiveOttavaDisplayShiftToMidi(62, active);
        List<String> stop = MuseScoreIo.applyMuseOttavaSpannerTransition(null, false, true, active, next);

        assertEquals(72, firstMeasure);
        assertEquals(74, secondMeasure);
        assertEquals("<direction placement=\"above\"><direction-type><octave-shift type=\"stop\" size=\"8\" number=\"1\"/></direction-type></direction>",
                stop.get(0));
        assertEquals(0, active.size());
    }

    @Test
    public void resolvesMuseScoreImportOptionDefaults() {
        MuseScoreIo.ResolvedMuseScoreImportOptions defaults = MuseScoreIo.resolveMuseScoreImportOptions(null, null,
                null, null);
        assertEquals(true, defaults.isSourceMetadata());
        assertEquals(true, defaults.isDebugMetadata());
        assertEquals(false, defaults.isNormalizeCutTimeToTwoTwo());
        assertEquals(true, defaults.isApplyImplicitBeams());

        MuseScoreIo.ResolvedMuseScoreImportOptions explicit = MuseScoreIo.resolveMuseScoreImportOptions(Boolean.FALSE,
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
        assertEquals(false, explicit.isSourceMetadata());
        assertEquals(false, explicit.isDebugMetadata());
        assertEquals(true, explicit.isNormalizeCutTimeToTwoTwo());
        assertEquals(false, explicit.isApplyImplicitBeams());
    }

    @Test
    public void readsMuseTickRelativeDiv() {
        assertEquals(120, MuseScoreIo.readMuseTickRelativeDiv("600", 480));
        assertEquals(0, MuseScoreIo.readMuseTickRelativeDiv("120", 480));
        assertEquals(0, MuseScoreIo.readMuseTickRelativeDiv("bad", 480));
    }

    @Test
    public void readsMuseTupletDescriptor() {
        MuseScoreIo.TupletDescriptor descriptor = MuseScoreIo.readMuseTupletDescriptor(" t1 ", Integer.valueOf(2),
                Integer.valueOf(3), Integer.valueOf(2), Integer.valueOf(1));
        assertEquals("t1", descriptor.getId());
        assertEquals(3, descriptor.getActualNotes());
        assertEquals(2, descriptor.getNormalNotes());
        assertEquals("none", descriptor.getShowNumber());
        assertEquals("yes", descriptor.getBracket());
        assertNull(MuseScoreIo.readMuseTupletDescriptor("bad", Integer.valueOf(0), Integer.valueOf(3), null, null));
    }

    @Test
    public void appliesMuseTupletStateTransitions() {
        MuseScoreIo.TupletDescriptor descriptor = MuseScoreIo.readMuseTupletDescriptor("t1", Integer.valueOf(2),
                Integer.valueOf(3), null, Integer.valueOf(2));
        List<Double> scaleStack = new ArrayList<Double>();
        List<MuseScoreIo.TupletState> stateStack = new ArrayList<MuseScoreIo.TupletState>();

        int next = MuseScoreIo.applyMuseInlineTupletStart(descriptor, scaleStack, stateStack, 4);
        assertEquals(5, next);
        assertEquals(Double.valueOf(2.0d / 3.0d), scaleStack.get(0));
        assertEquals(4, stateStack.get(0).getNumber());
        assertEquals("no", stateStack.get(0).getBracket());
        assertEquals(true, stateStack.get(0).isStartPending());

        assertEquals(Integer.valueOf(4), MuseScoreIo.applyMuseEndTuplet(scaleStack, stateStack));
        assertEquals(0, scaleStack.size());
        assertEquals(0, stateStack.size());
    }

    @Test
    public void finalizesActiveMuseTupletRef() {
        Map<String, Integer> tupletNumberById = new HashMap<String, Integer>();
        tupletNumberById.put("t1", Integer.valueOf(7));

        MuseScoreIo.FinalizedTupletRef result = MuseScoreIo.finalizeActiveMuseTupletRef("t1", tupletNumberById);
        assertNull(result.getActiveTupletRefId());
        assertEquals(Integer.valueOf(7), result.getEndedTupletNumber());

        MuseScoreIo.FinalizedTupletRef empty = MuseScoreIo.finalizeActiveMuseTupletRef(null, tupletNumberById);
        assertNull(empty.getActiveTupletRefId());
        assertNull(empty.getEndedTupletNumber());
    }

    @Test
    public void consumesMuseTupletStartsAndScale() {
        MuseScoreIo.TupletDescriptor descriptor = MuseScoreIo.readMuseTupletDescriptor("t1", Integer.valueOf(2),
                Integer.valueOf(3), null, Integer.valueOf(1));
        List<Double> scaleStack = new ArrayList<Double>();
        List<MuseScoreIo.TupletState> stateStack = new ArrayList<MuseScoreIo.TupletState>();
        MuseScoreIo.applyMuseInlineTupletStart(descriptor, scaleStack, stateStack, 9);

        assertEquals(Double.valueOf(2.0d / 3.0d), Double.valueOf(MuseScoreIo.currentTupletScale(scaleStack)));
        List<MuseScoreIo.TupletStart> starts = MuseScoreIo.consumeTupletStarts(stateStack);
        assertEquals(1, starts.size());
        assertEquals(9, starts.get(0).getNumber());
        assertEquals("yes", starts.get(0).getBracket());
        assertEquals(0, MuseScoreIo.consumeTupletStarts(stateStack).size());
    }

    @Test
    public void appendsTupletStopToLastTimedEvent() {
        List<MuseScoreIo.TimedEvent> events = new ArrayList<MuseScoreIo.TimedEvent>();
        events.add(new MuseScoreIo.TimedEvent(120, false, false));
        events.add(new MuseScoreIo.TimedEvent(160, false, true));

        assertEquals(Integer.valueOf(1), MuseScoreIo.appendTupletStopToLastTimedEvent(events, 5));
        assertEquals(Integer.valueOf(5), events.get(1).getTupletStops().get(0));
    }

    @Test
    public void resolvesTupletNumberByIdAndConsumesTrillNumbers() {
        Map<String, Integer> tupletNumberById = new HashMap<String, Integer>();
        MuseScoreIo.MutableInt next = new MuseScoreIo.MutableInt(3);
        assertEquals(3, MuseScoreIo.resolveTupletNumberById("a", tupletNumberById, next));
        assertEquals(4, next.getCurrent());
        assertEquals(3, MuseScoreIo.resolveTupletNumberById("a", tupletNumberById, next));
        assertEquals(4, next.getCurrent());

        List<Integer> pending = new ArrayList<Integer>();
        pending.add(Integer.valueOf(1));
        pending.add(Integer.valueOf(2));
        List<Integer> consumed = MuseScoreIo.consumePendingTrillNumbers(pending);
        assertEquals(2, consumed.size());
        assertEquals(0, pending.size());
    }

    @Test
    public void resolvesIndependentMuseTupletIdReferences() {
        Map<String, Integer> tupletNumberById = new HashMap<String, Integer>();
        MuseScoreIo.MutableInt next = new MuseScoreIo.MutableInt(1);

        int first = MuseScoreIo.resolveTupletNumberById("1", tupletNumberById, next);
        int firstAgain = MuseScoreIo.resolveTupletNumberById("1", tupletNumberById, next);
        int second = MuseScoreIo.resolveTupletNumberById("2", tupletNumberById, next);
        MuseScoreIo.TimeModification triplet = MuseScoreIo.parseMusicXmlTupletTimeModification("3", "2");
        MuseScoreIo.TupletMusicXml firstStart = MuseScoreIo.buildTupletMusicXml(triplet,
                Arrays.asList(new MuseScoreIo.TupletStart(first, "actual", "yes")), Arrays.<Integer>asList());
        MuseScoreIo.TupletMusicXml firstStop = MuseScoreIo.buildTupletMusicXml(triplet,
                Arrays.<MuseScoreIo.TupletStart>asList(), Arrays.asList(Integer.valueOf(first)));
        MuseScoreIo.TupletMusicXml secondStart = MuseScoreIo.buildTupletMusicXml(triplet,
                Arrays.asList(new MuseScoreIo.TupletStart(second, "actual", "yes")), Arrays.<Integer>asList());
        MuseScoreIo.TupletMusicXml secondStop = MuseScoreIo.buildTupletMusicXml(triplet,
                Arrays.<MuseScoreIo.TupletStart>asList(), Arrays.asList(Integer.valueOf(second)));

        assertEquals(1, first);
        assertEquals(1, firstAgain);
        assertEquals(2, second);
        assertEquals(3, next.getCurrent());
        assertEquals(true, first != second);
        assertEquals("<tuplet type=\"start\" number=\"1\" bracket=\"yes\" show-number=\"actual\"/>",
                firstStart.getNotationItems().get(0));
        assertEquals("<tuplet type=\"stop\" number=\"1\"/>", firstStop.getNotationItems().get(0));
        assertEquals("<tuplet type=\"start\" number=\"2\" bracket=\"yes\" show-number=\"actual\"/>",
                secondStart.getNotationItems().get(0));
        assertEquals("<tuplet type=\"stop\" number=\"2\"/>", secondStop.getNotationItems().get(0));
    }

    @Test
    public void keepsWrittenDurationTypeSeparateFromTupletScaledDuration() {
        Integer written = MuseScoreIo.durationTypeToDivisions("16th", 480);
        MuseScoreIo.TimeModification triplet = MuseScoreIo.parseMusicXmlTupletTimeModification("3", "2");
        MuseScoreIo.TupletMusicXml tuplet = MuseScoreIo.buildTupletMusicXml(triplet,
                Arrays.asList(new MuseScoreIo.TupletStart(1, "actual", "yes")), Arrays.<Integer>asList());
        int scaledDuration = (int) Math.round(written.intValue() * (2.0d / 3.0d));

        assertEquals(Integer.valueOf(120), written);
        assertEquals(80, scaledDuration);
        assertEquals("<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>",
                tuplet.getTimeModificationXml());
    }

    @Test
    public void readsMuseImportEventRouting() {
        MuseScoreIo.MuseScoreImportEventRouting routed = MuseScoreIo.readMuseImportEventRouting("Chord",
                Integer.valueOf(5), 1, 1, Integer.valueOf(-1));
        assertEquals("chord", routed.getTag());
        assertEquals(2, routed.getVoiceNo());
        assertEquals(1, routed.getMovedStaffNo());

        MuseScoreIo.MuseScoreImportEventRouting fallback = MuseScoreIo.readMuseImportEventRouting("Rest", null, 3, 0,
                null);
        assertEquals("rest", fallback.getTag());
        assertEquals(3, fallback.getVoiceNo());
        assertEquals(1, fallback.getMovedStaffNo());
    }

    @Test
    public void parsesTieStringAndTrillTransitionHelpers() {
        MuseScoreIo.TieFlags start = MuseScoreIo.parseMuseTieFlags(true, false, false, false);
        assertEquals(true, start.isTieStart());
        assertEquals(false, start.isTieStop());

        MuseScoreIo.TieFlags stop = MuseScoreIo.parseMuseTieFlags(true, true, false, true);
        assertEquals(false, stop.isTieStart());
        assertEquals(true, stop.isTieStop());

        assertEquals("3", MuseScoreIo.parseMuseStringText(" ignored ", " 3 "));
        assertNull(MuseScoreIo.parseMuseStringText(" ", null));

        MuseScoreIo.TrillSpannerTransition trill = MuseScoreIo.parseTrillSpannerTransition("Trill", true, false);
        assertEquals(true, trill.isStart());
        assertEquals(false, trill.isStop());
        MuseScoreIo.TrillSpannerTransition other = MuseScoreIo.parseTrillSpannerTransition("slur", true, true);
        assertEquals(false, other.isStart());
        assertEquals(false, other.isStop());
    }

    @Test
    public void buildsMuseImportedTieAndTiedXml() {
        MuseScoreIo.TieFlags start = MuseScoreIo.parseMuseTieFlags(true, false, false, false);
        MuseScoreIo.TieFlags stop = MuseScoreIo.parseMuseTieFlags(false, false, false, true);

        assertEquals("<tie type=\"start\"/>", MuseScoreIo.buildMuseImportedNoteTieXml(start));
        assertEquals("<notations><tied type=\"start\"/></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, null, null, null, start, null, null));
        assertEquals("<tie type=\"stop\"/>", MuseScoreIo.buildMuseImportedNoteTieXml(stop));
        assertEquals("<notations><tied type=\"stop\"/></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, null, null, null, stop, null, null));
    }

    @Test
    public void buildsMuseImportedArticulationNotationsXml() {
        MuseScoreIo.MuseScoreChordNotationSummary summary = MuseScoreIo.summarizeMuseChordNotations(
                Arrays.asList("articStaccatoBelow", "articTenutoAbove"), null);

        assertEquals("<notations><articulations><staccato/><tenuto/></articulations></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, null, summary.getArticulationTags(),
                        null, null, null, null));
    }

    @Test
    public void buildsMuseImportedTechnicalNotationsXml() {
        MuseScoreIo.MuseScoreChordNotationSummary summary = MuseScoreIo.summarizeMuseChordNotations(
                Arrays.asList("articLhPizzicatoAbove", "articUpBowAbove", "articDownBowAbove",
                        "articOpenStringAbove", "articHarmonicAbove"),
                Arrays.asList("brassMuteClosed"));

        assertEquals(
                "<notations><technical><stopped/><up-bow/><down-bow/><open-string/><harmonic/><stopped/><fingering>1</fingering><string>3</string></technical></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, null, null,
                        summary.getTechnicalTags(), null, "1", Integer.valueOf(3)));
    }

    @Test
    public void buildsMuseImportedTrillSpannerNotationsXml() {
        List<Integer> active = new ArrayList<Integer>();
        List<Integer> pendingStarts = new ArrayList<Integer>();
        List<Integer> pendingStops = new ArrayList<Integer>();
        MuseScoreIo.MutableInt next = new MuseScoreIo.MutableInt(1);

        MuseScoreIo.applyMuseTrillSpannerTransition(MuseScoreIo.parseTrillSpannerTransition("Trill", true, false),
                active, next, pendingStarts, pendingStops);
        List<String> startItems = MuseScoreIo.buildMuseImportedTrillNotationItems(
                MuseScoreIo.consumePendingTrillNumbers(pendingStarts), null, false, null);
        MuseScoreIo.applyMuseTrillSpannerTransition(MuseScoreIo.parseTrillSpannerTransition("Trill", false, true),
                active, next, pendingStarts, pendingStops);
        List<String> stopItems = MuseScoreIo.buildMuseImportedTrillNotationItems(null,
                MuseScoreIo.consumePendingTrillNumbers(pendingStops), false, null);

        assertEquals(
                "<notations><ornaments><trill-mark/><wavy-line type=\"start\" number=\"1\"/></ornaments></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, startItems, null, null, null, null,
                        null));
        assertEquals("<notations><ornaments><wavy-line type=\"stop\" number=\"1\"/></ornaments></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, stopItems, null, null, null, null,
                        null));
        assertEquals(2, next.getCurrent());
        assertEquals(0, active.size());
    }

    @Test
    public void buildsMuseImportedChordTrillMarkNotationsXml() {
        MuseScoreIo.MuseScoreChordNotationSummary summary = MuseScoreIo.summarizeMuseChordNotations(
                null, Arrays.asList("ornamentTrill"));
        List<String> items = MuseScoreIo.buildMuseImportedTrillNotationItems(null, null,
                summary.hasChordLocalTrillMark(), null);

        assertEquals("<notations><ornaments><trill-mark/></ornaments></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, items, null, null, null, null, null));
    }

    @Test
    public void buildsMuseImportedTrillAccidentalMarkNotationsXml() {
        List<String> items = MuseScoreIo.buildMuseImportedTrillNotationItems(null, null, true, "flat");

        assertEquals("<notations><ornaments><trill-mark/><accidental-mark>flat</accidental-mark></ornaments></notations>",
                MuseScoreIo.buildMuseImportedNoteNotationsXml(null, null, null, items, null, null, null, null, null));
    }

    @Test
    public void importsMuseSlurSpannerAsMusicXmlStartStop() {
        MuseScoreIo.MuseImportSlurState state = new MuseScoreIo.MuseImportSlurState();
        MuseScoreIo.MuseSlurTransitions start = MuseScoreIo.parseMuseChordSlurTransitions(
                null, Arrays.asList(MuseScoreIo.parseMuseSlurSpannerTransition("Slur", true, false)), state);
        MuseScoreIo.MuseSlurTransitions stop = MuseScoreIo.parseMuseChordSlurTransitions(
                null, Arrays.asList(MuseScoreIo.parseMuseSlurSpannerTransition("Slur", false, true)), state);

        assertEquals(Arrays.asList(Integer.valueOf(1)), start.getStarts());
        assertEquals(Arrays.asList(Integer.valueOf(1)), stop.getStops());
        assertEquals(2, state.getNextSlurNumber());
        assertEquals(0, state.getActiveSlurNumbers().size());
    }

    @Test
    public void keepsMuseSlurSpannerNumberAcrossMeasureBoundary() {
        MuseScoreIo.MuseImportSlurState state = new MuseScoreIo.MuseImportSlurState();
        MuseScoreIo.MuseSlurTransitions firstMeasure = MuseScoreIo.parseMuseChordSlurTransitions(
                null, Arrays.asList(MuseScoreIo.parseMuseSlurSpannerTransition("Slur", true, false)), state);
        MuseScoreIo.MuseSlurTransitions secondMeasure = MuseScoreIo.parseMuseChordSlurTransitions(
                null, Arrays.asList(MuseScoreIo.parseMuseSlurSpannerTransition("Slur", false, true)), state);

        assertEquals(firstMeasure.getStarts().get(0), secondMeasure.getStops().get(0));
        assertEquals(Arrays.asList(Integer.valueOf(1)), firstMeasure.getStarts());
        assertEquals(Arrays.asList(Integer.valueOf(1)), secondMeasure.getStops());
    }

    @Test
    public void importsMuseLegacyChordLevelSlurStartStopWithId() {
        MuseScoreIo.MuseImportSlurState state = new MuseScoreIo.MuseImportSlurState();
        MuseScoreIo.MuseSlurTransitions start = MuseScoreIo.parseMuseChordSlurTransitions(
                Arrays.asList(new MuseScoreIo.MuseSlurElement("start", "2")), null, state);
        MuseScoreIo.MuseSlurTransitions stop = MuseScoreIo.parseMuseChordSlurTransitions(
                Arrays.asList(new MuseScoreIo.MuseSlurElement("stop", "2")), null, state);

        assertEquals(Arrays.asList(Integer.valueOf(2)), start.getStarts());
        assertEquals(Arrays.asList(Integer.valueOf(2)), stop.getStops());
        assertEquals(0, state.getActiveSlurNumbers().size());
    }

    @Test
    public void summarizesMuseChordNotations() {
        MuseScoreIo.MuseScoreChordNotationSummary summary = MuseScoreIo.summarizeMuseChordNotations(
                Arrays.asList("articStaccato", "stringsUpBow"),
                Arrays.asList("trill", "brassMuteClosed"));
        assertEquals(1, summary.getArticulationTags().size());
        assertEquals("staccato", summary.getArticulationTags().get(0));
        assertEquals(2, summary.getTechnicalTags().size());
        assertEquals("up-bow", summary.getTechnicalTags().get(0));
        assertEquals("stopped", summary.getTechnicalTags().get(1));
        assertEquals(true, summary.hasChordLocalTrillMark());
    }

    @Test
    public void parsesMuseChordNotes() {
        MuseScoreIo.TieFlags tie = MuseScoreIo.parseMuseTieFlags(true, false, true, false);
        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), "accidentalSharp", "12", tie, " 2 ",
                        " 4 "),
                new MuseScoreIo.MuseScoreChordNoteInput(null, "accidentalFlat", "bad", null, null, null)), 12);

        assertEquals(1, notes.size());
        MuseScoreIo.MuseScoreChordNote note = notes.get(0);
        assertEquals(72, note.getMidi());
        assertEquals("sharp", note.getAccidentalText());
        assertEquals("flat", note.getTpcAccidentalText());
        assertEquals(true, note.isTieStart());
        assertEquals(false, note.isTieStop());
        assertEquals("2", note.getFingeringText());
        assertEquals(Integer.valueOf(4), note.getStringNumber());
    }

    @Test
    public void buildsMuseImportedPitchXmlWithPreferredAccidentalSpelling() {
        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(61), "accidentalFlat", null, null, null,
                        null)), 0);
        MuseScoreIo.MuseImportedPitchXml pitch = MuseScoreIo.buildMuseImportedPitchXml(notes.get(0), 0, 1,
                new HashMap<String, Integer>());

        assertEquals("<pitch><step>D</step><alter>-1</alter><octave>4</octave></pitch>", pitch.getPitchXml());
        assertEquals("<accidental>flat</accidental>", pitch.getAccidentalXml());
    }

    @Test
    public void keepsMuseImportedNaturalAccidentalOnSeparateStaff() {
        Map<String, Integer> state = new HashMap<String, Integer>();
        state.put("3:3:B", Integer.valueOf(0));
        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(59), "accidentalNatural", null, null, null,
                        null)), 0);

        MuseScoreIo.MuseImportedPitchXml pitch = MuseScoreIo.buildMuseImportedPitchXml(notes.get(0), -1, 4, state);

        assertEquals("<pitch><step>B</step><octave>3</octave></pitch>", pitch.getPitchXml());
        assertEquals("<accidental>natural</accidental>", pitch.getAccidentalXml());
        assertEquals(Integer.valueOf(0), state.get("4:3:B"));
    }

    @Test
    public void keepsMuseImportedVoiceAccidentalStateByStaffPitchKey() {
        Map<String, Integer> state = new HashMap<String, Integer>();
        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(62), null, null, null, null, null),
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(63), null, null, null, null, null),
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(62), null, null, null, null, null),
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(59), null, null, null, null, null)), 0);

        List<MuseScoreIo.MuseImportedPitchXml> pitches = MuseScoreIo.buildMuseImportedVoicePitchXmlItems(notes, 4, 4,
                state);

        assertEquals("<pitch><step>D</step><octave>4</octave></pitch>", pitches.get(0).getPitchXml());
        assertEquals("<accidental>natural</accidental>", pitches.get(0).getAccidentalXml());
        assertEquals("<pitch><step>D</step><alter>1</alter><octave>4</octave></pitch>",
                pitches.get(1).getPitchXml());
        assertEquals("<accidental>sharp</accidental>", pitches.get(1).getAccidentalXml());
        assertEquals("<pitch><step>D</step><octave>4</octave></pitch>", pitches.get(2).getPitchXml());
        assertEquals("<accidental>natural</accidental>", pitches.get(2).getAccidentalXml());
        assertEquals("<pitch><step>B</step><octave>3</octave></pitch>", pitches.get(3).getPitchXml());
        assertEquals("", pitches.get(3).getAccidentalXml());
        assertEquals(Integer.valueOf(0), state.get("4:4:D"));
        assertEquals(Integer.valueOf(0), state.get("4:3:B"));
        assertNull(state.get("1:4:D"));
    }

    @Test
    public void buildsMuseImportedRestAndPitchedNoteXml() {
        String timeModificationXml = "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>";
        assertEquals(
                "<note><rest/><duration>160</duration><voice>2</voice><type>eighth</type><dot/>"
                        + timeModificationXml
                        + "<beam number=\"1\">begin</beam><staff>3</staff><notations><tuplet type=\"start\" number=\"1\"/></notations></note>",
                MuseScoreIo.buildMuseImportedRestNoteXml(160, 2, "eighth", 1, timeModificationXml,
                        "<beam number=\"1\">begin</beam>", 3, Arrays.asList("<tuplet type=\"start\" number=\"1\"/>")));

        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(61), "accidentalSharp", null,
                        MuseScoreIo.parseMuseTieFlags(true, false, true, false), null, null)), 0);
        MuseScoreIo.MuseImportedPitchXml pitch = MuseScoreIo.buildMuseImportedPitchXml(notes.get(0), 0, 1,
                new HashMap<String, Integer>());
        assertEquals(
                "<note><chord/><grace slash=\"yes\"/><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><tie type=\"start\"/><voice>4</voice><type>16th</type><accidental>sharp</accidental><staff>1</staff><notations><tied type=\"start\"/></notations></note>",
                MuseScoreIo.buildMuseImportedPitchedNoteXml(true, true, true, pitch,
                        MuseScoreIo.parseMuseTieFlags(true, false, true, false), 120, 4, "16th", 0, "", "", 1,
                        Arrays.asList("<tied type=\"start\"/>")));
    }

    @Test
    public void buildsMuseImportedRestAndChordEventXml() {
        MuseScoreIo.TupletMusicXml tuplet = MuseScoreIo.buildTupletMusicXml(new MuseScoreIo.TimeModification(3, 2),
                Arrays.asList(new MuseScoreIo.TupletStart(1, "actual", "yes")), null);
        assertEquals(
                "<note><rest/><duration>240</duration><voice>2</voice><type>eighth</type>"
                        + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"
                        + "<beam number=\"1\">begin</beam><staff>1</staff><notations><tuplet type=\"start\" number=\"1\" bracket=\"yes\" show-number=\"actual\"/></notations></note>",
                MuseScoreIo.buildMuseImportedRestEventXml(240, Integer.valueOf(240), 480, 2, 1, tuplet,
                        "<beam number=\"1\">begin</beam>"));

        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(61), "accidentalSharp", null,
                        MuseScoreIo.parseMuseTieFlags(true, false, true, false), "1", "2"),
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(64), null, null, null, null, null)), 0);
        String xml = MuseScoreIo.buildMuseImportedChordEventXml(notes, 0, 1, new HashMap<String, Integer>(), 240,
                Integer.valueOf(240), 480, 2, false, false, null, "<beam number=\"1\">begin</beam>",
                Arrays.asList(Integer.valueOf(2)), null, Arrays.asList("<ornaments><trill-mark/></ornaments>"),
                Arrays.asList("staccato"), Arrays.asList("up-bow"));

        assertEquals(
                "<note><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><tie type=\"start\"/><duration>240</duration><voice>2</voice><type>eighth</type><accidental>sharp</accidental><beam number=\"1\">begin</beam><staff>1</staff><notations><slur type=\"start\" number=\"2\"/><ornaments><trill-mark/></ornaments><articulations><staccato/></articulations><technical><up-bow/><fingering>1</fingering><string>2</string></technical><tied type=\"start\"/></notations></note>"
                        + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>240</duration><voice>2</voice><type>eighth</type><staff>1</staff></note>",
                xml);

        String graceXml = MuseScoreIo.buildMuseImportedChordEventXml(notes, 0, 1, new HashMap<String, Integer>(),
                240, Integer.valueOf(240), 480, 2, true, true, null, "", null, null, null, null, null);
        assertEquals(
                "<note><grace slash=\"yes\"/><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><tie type=\"start\"/><voice>2</voice><type>eighth</type><accidental>sharp</accidental><staff>1</staff><notations><technical><fingering>1</fingering><string>2</string></technical><tied type=\"start\"/></notations></note>"
                        + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch><voice>2</voice><type>eighth</type><staff>1</staff></note>",
                graceXml);
    }

    @Test
    public void buildsMuseImportedVoiceCursorXmlHelpers() {
        assertEquals("<forward><duration>120</duration><voice>2</voice><staff>3</staff></forward>",
                MuseScoreIo.buildMuseImportedForwardXml(120, 2, 3));
        assertEquals("", MuseScoreIo.buildMuseImportedForwardXml(0, 2, 3));
        assertEquals(false, MuseScoreIo.shouldClampMuseImportedTimedEvent(360, 120, 480, 0));
        assertEquals(true, MuseScoreIo.shouldClampMuseImportedTimedEvent(361, 120, 480, 0));
        assertEquals(false, MuseScoreIo.shouldClampMuseImportedTimedEvent(361, 120, 480, 1));

        assertEquals("<note><rest/><duration>120</duration><voice>2</voice><type>16th</type><staff>3</staff></note>",
                MuseScoreIo.buildMuseImportedTailRestNoteXml(360, 480, 0, 480, 2, 3));
        assertEquals("", MuseScoreIo.buildMuseImportedTailRestNoteXml(470, 480, 10, 480, 2, 3));
        assertEquals("", MuseScoreIo.buildMuseImportedTailRestNoteXml(480, 480, 0, 480, 2, 3));
    }

    @Test
    public void buildsMuseImportedPlacedUntimedEventXml() {
        assertEquals(
                "<forward><duration>120</duration><voice>2</voice><staff>3</staff></forward><direction><direction-type><dynamics><mf/></dynamics></direction-type><sound dynamics=\"90.00\"/><staff>3</staff><voice>2</voice></direction>",
                MuseScoreIo.buildMuseImportedPlacedDynamicXml(240, 120, 2, 3, "mf", Double.valueOf(90.0d)));
        assertEquals(
                "<direction><direction-type><words>dolce</words></direction-type><staff>4</staff><voice>1</voice></direction>",
                MuseScoreIo.buildMuseImportedPlacedDirectionXml(100, 120, 1, 4,
                        "<direction><direction-type><words>dolce</words></direction-type></direction>"));
        assertEquals(
                "<forward><duration>60</duration><voice>2</voice><staff>3</staff></forward><barline location=\"middle\"><bar-style>light-light</bar-style></barline>",
                MuseScoreIo.buildMuseImportedPlacedBarlineXml(180, 120, 2, 3,
                        "<barline location=\"middle\"><bar-style>light-light</bar-style></barline>"));
    }

    @Test
    public void buildsMuseImportedVoiceXmlFromTypedEvents() {
        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), null, null, null, null, null)), 0);
        List<MuseScoreIo.MuseImportedVoiceEvent> events = Arrays.asList(
                MuseScoreIo.MuseImportedVoiceEvent.dynamic(Integer.valueOf(120), Integer.valueOf(1), "mf",
                        Double.valueOf(90.0d)),
                MuseScoreIo.MuseImportedVoiceEvent.chord(Integer.valueOf(240), 120, null, Integer.valueOf(1), notes,
                        false, false, null, null, null, null, null, null, null, null, null),
                MuseScoreIo.MuseImportedVoiceEvent.rest(Integer.valueOf(360), 120, null, Integer.valueOf(1), null,
                        null, null, null));

        assertEquals(
                "<forward><duration>120</duration><voice>2</voice><staff>1</staff></forward><direction><direction-type><dynamics><mf/></dynamics></direction-type><sound dynamics=\"90.00\"/><staff>1</staff><voice>2</voice></direction>"
                        + "<forward><duration>120</duration><voice>2</voice><staff>1</staff></forward><note><pitch><step>C</step><octave>4</octave></pitch><duration>120</duration><voice>2</voice><type>16th</type><staff>1</staff></note>"
                        + "<note><rest/><duration>120</duration><voice>2</voice><type>16th</type><staff>1</staff></note>"
                        + "<note><rest/><duration>120</duration><voice>2</voice><type>16th</type><staff>1</staff></note>",
                MuseScoreIo.buildMuseImportedVoiceXml(4, 4, 0, 1, 2, 600, 480, false, events));
    }

    @Test
    public void buildsMuseImportedVoiceXmlWithImplicitBeamsTupletsAndClamp() {
        List<MuseScoreIo.MuseScoreChordNote> first = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), null, null, null, null, null)), 0);
        List<MuseScoreIo.MuseScoreChordNote> second = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(62), null, null, null, null, null)), 0);
        List<MuseScoreIo.MuseImportedVoiceEvent> beamedEvents = Arrays.asList(
                MuseScoreIo.MuseImportedVoiceEvent.chord(Integer.valueOf(0), 240, null, Integer.valueOf(1), first,
                        false, false, null, null, null, null, null, null, null, null, null),
                MuseScoreIo.MuseImportedVoiceEvent.chord(Integer.valueOf(240), 240, null, Integer.valueOf(1), second,
                        false, false, null, null, null, null, null, null, null, null, null));

        assertEquals(
                "<note><pitch><step>C</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type><beam number=\"1\">begin</beam><staff>1</staff></note>"
                        + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>240</duration><voice>1</voice><type>eighth</type><beam number=\"1\">end</beam><staff>1</staff></note>",
                MuseScoreIo.buildMuseImportedVoiceXml(4, 4, 0, 1, 1, 480, 480, true, beamedEvents));

        List<MuseScoreIo.MuseImportedVoiceEvent> tupletEvents = Arrays.asList(
                MuseScoreIo.MuseImportedVoiceEvent.rest(Integer.valueOf(0), 160, Integer.valueOf(240),
                        Integer.valueOf(1), new MuseScoreIo.TimeModification(3, 2),
                        Arrays.asList(new MuseScoreIo.TupletStart(1, "actual", "yes")), null, null),
                MuseScoreIo.MuseImportedVoiceEvent.chord(Integer.valueOf(160), 160, Integer.valueOf(240),
                        Integer.valueOf(1), first, false, false, new MuseScoreIo.TimeModification(3, 2), null,
                        Arrays.asList(Integer.valueOf(1)), null, null, null, null, null, null));

        assertEquals(
                "<note><rest/><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><staff>1</staff><notations><tuplet type=\"start\" number=\"1\" bracket=\"yes\" show-number=\"actual\"/></notations></note>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>160</duration><voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><staff>1</staff><notations><tuplet type=\"stop\" number=\"1\"/></notations></note>",
                MuseScoreIo.buildMuseImportedVoiceXml(4, 4, 0, 1, 1, 320, 480, false, tupletEvents));

        List<MuseScoreIo.MuseImportedVoiceEvent> clampedEvents = Arrays.asList(
                MuseScoreIo.MuseImportedVoiceEvent.rest(Integer.valueOf(0), 240, null, Integer.valueOf(1), null,
                        null, null, null),
                MuseScoreIo.MuseImportedVoiceEvent.rest(Integer.valueOf(240), 240, null, Integer.valueOf(1), null,
                        null, null, null));

        assertEquals("<note><rest/><duration>240</duration><voice>1</voice><type>eighth</type><staff>1</staff></note>",
                MuseScoreIo.buildMuseImportedVoiceXml(4, 4, 0, 1, 1, 240, 480, false, clampedEvents));
    }

    @Test
    public void collectsMuseImportedTypedVoiceEventsByVoice() {
        List<MuseScoreIo.MuseScoreChordNote> notes = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), null, null, null, null, null)), 0);
        List<MuseScoreIo.MuseImportedVoiceEvent> events = Arrays.asList(
                MuseScoreIo.MuseImportedVoiceEvent.restForVoice(Integer.valueOf(360), 2, 120, null, Integer.valueOf(1),
                        null, null, null, null),
                MuseScoreIo.MuseImportedVoiceEvent.chordForVoice(Integer.valueOf(120), 1, 120, null, Integer.valueOf(1),
                        notes, false, false, null, null, null, null, null, null, null, null, null),
                MuseScoreIo.MuseImportedVoiceEvent.dynamicForVoice(Integer.valueOf(240), 2, Integer.valueOf(1), "p",
                        null));

        List<Integer> voiceNos = MuseScoreIo.resolveMuseImportedTypedVoiceNos(events);
        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)), voiceNos);

        List<MuseScoreIo.MuseImportedVoiceEvent> voiceTwo = MuseScoreIo.collectMuseImportedTypedVoiceEvents(events,
                2);
        assertEquals(2, voiceTwo.size());
        assertEquals("dynamic", voiceTwo.get(0).getKind());
        assertEquals("rest", voiceTwo.get(1).getKind());
        assertEquals(2, voiceTwo.get(0).getVoiceNo());
        assertEquals(Arrays.asList(Integer.valueOf(1)), MuseScoreIo.resolveMuseImportedTypedVoiceNos(null));
    }

    @Test
    public void buildsMuseImportedStaffVoicesXmlWithBackups() {
        List<MuseScoreIo.MuseScoreChordNote> c4 = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), null, null, null, null, null)), 0);
        List<MuseScoreIo.MuseScoreChordNote> e4 = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(64), null, null, null, null, null)), 0);
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 480, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.ParsedMuseScoreMeasure measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.<MuseScoreIo.TimedEvent>asList(new MuseScoreIo.TimedEvent(120, false, false, 1),
                        new MuseScoreIo.TimedEvent(120, false, false, 2)));
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.asList(new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2, Arrays.asList(measure))));
        MuseScoreIo.MuseImportedPartVoiceIdResolver resolver = MuseScoreIo.buildMuseImportedPartVoiceIdResolver(part);
        List<MuseScoreIo.MuseImportedVoiceEvent> events = Arrays.asList(
                MuseScoreIo.MuseImportedVoiceEvent.chordForVoice(Integer.valueOf(0), 2, 120, null, Integer.valueOf(1),
                        e4, false, false, null, null, null, null, null, null, null, null, null),
                MuseScoreIo.MuseImportedVoiceEvent.chordForVoice(Integer.valueOf(0), 1, 120, null, Integer.valueOf(1),
                        c4, false, false, null, null, null, null, null, null, null, null, null));

        assertEquals("<backup><duration>480</duration></backup>", MuseScoreIo.buildMuseImportedBackupXml(480));
        assertEquals(
                "<note><pitch><step>C</step><octave>4</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type><staff>1</staff></note><note><rest/><duration>360</duration><voice>1</voice><type>eighth</type><dot/><staff>1</staff></note>"
                        + "<backup><duration>480</duration></backup>"
                        + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>120</duration><voice>2</voice><type>16th</type><staff>1</staff></note><note><rest/><duration>360</duration><voice>2</voice><type>eighth</type><dot/><staff>1</staff></note>",
                MuseScoreIo.buildMuseImportedStaffVoicesXml(4, 4, 0, 1, 480, 480, false, resolver, events));
    }

    @Test
    public void buildsMuseImportedMeasureXmlFromTypedStaffEvents() {
        List<MuseScoreIo.MuseScoreChordNote> c4 = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), null, null, null, null, null)), 0);
        List<MuseScoreIo.MuseScoreChordNote> c3 = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(48), null, null, null, null, null)), 0);
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 480, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.ParsedMuseScoreMeasure measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.<MuseScoreIo.TimedEvent>asList(new MuseScoreIo.TimedEvent(120, false, false, 1)));
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.asList(new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2, Arrays.asList(measure)),
                        new MuseScoreIo.ParsedMuseScoreStaff("2", "F", 4, Arrays.asList(measure))));
        Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>> eventsByStaff = new HashMap<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>();
        eventsByStaff.put(Integer.valueOf(1),
                Arrays.asList(MuseScoreIo.MuseImportedVoiceEvent.chordForVoice(Integer.valueOf(0), 1, 120, null,
                        Integer.valueOf(1), c4, false, false, null, null, null, null, null, null, null, null,
                        null)));
        eventsByStaff.put(Integer.valueOf(2),
                Arrays.asList(MuseScoreIo.MuseImportedVoiceEvent.chordForVoice(Integer.valueOf(0), 1, 120, null,
                        Integer.valueOf(2), c3, false, false, null, null, null, null, null, null, null, null,
                        null)));

        assertEquals(
                "<measure number=\"1\"><attributes><divisions>480</divisions><key><fifths>0</fifths><mode>major</mode></key><time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign><line>4</line></clef><miscellaneous><m/></miscellaneous></attributes>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type><staff>1</staff></note><note><rest/><duration>360</duration><voice>1</voice><type>eighth</type><dot/><staff>1</staff></note>"
                        + "<backup><duration>480</duration></backup>"
                        + "<note><pitch><step>C</step><octave>3</octave></pitch><duration>120</duration><voice>2</voice><type>16th</type><staff>2</staff></note><note><rest/><duration>360</duration><voice>2</voice><type>eighth</type><dot/><staff>2</staff></note>"
                        + "<barline location=\"right\"><bar-style>light-heavy</bar-style></barline></measure>",
                MuseScoreIo.buildMuseImportedMeasureXml(part, measure, 0, 0, 1, false, 480, "<m/>", true, false,
                        MuseScoreIo.buildMuseImportedPartVoiceIdResolver(part), eventsByStaff));
    }

    @Test
    public void buildsMuseImportedPartXmlFromTypedMeasureEvents() {
        List<MuseScoreIo.MuseScoreChordNote> c4 = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), null, null, null, null, null)), 0);
        MuseScoreIo.MuseScoreImportMeasureContext firstContext = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 480, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.MuseScoreImportMeasureContext secondContext = new MuseScoreIo.MuseScoreImportMeasureContext(3, 4,
                null, true, 360, false, 1, "minor", null, null, false, true, false);
        MuseScoreIo.ParsedMuseScoreMeasure firstMeasure = MuseScoreIo.buildParsedMuseScoreMeasure(1, firstContext,
                Arrays.<MuseScoreIo.TimedEvent>asList(new MuseScoreIo.TimedEvent(120, false, false, 1)));
        MuseScoreIo.ParsedMuseScoreMeasure secondMeasure = MuseScoreIo.buildParsedMuseScoreMeasure(2, secondContext,
                Arrays.<MuseScoreIo.TimedEvent>asList(new MuseScoreIo.TimedEvent(120, false, false, 1)));
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.asList(new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2,
                        Arrays.asList(firstMeasure, secondMeasure))));
        Map<Integer, Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>> eventsByMeasureStaff = new HashMap<Integer, Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>>();
        Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>> firstEvents = new HashMap<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>();
        firstEvents.put(Integer.valueOf(1),
                Arrays.asList(MuseScoreIo.MuseImportedVoiceEvent.chordForVoice(Integer.valueOf(0), 1, 120, null,
                        Integer.valueOf(1), c4, false, false, null, null, null, null, null, null, null, null,
                        null)));
        eventsByMeasureStaff.put(Integer.valueOf(0), firstEvents);

        assertEquals(
                "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions><key><fifths>0</fifths><mode>major</mode></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef><miscellaneous><m/></miscellaneous></attributes>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type><staff>1</staff></note><note><rest/><duration>360</duration><voice>1</voice><type>eighth</type><dot/><staff>1</staff></note></measure>"
                        + "<measure number=\"2\"><attributes><divisions>480</divisions><key><fifths>1</fifths><mode>minor</mode></key><time><beats>3</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>"
                        + "<note><rest/><duration>360</duration><voice>1</voice><type>eighth</type><dot/><staff>1</staff></note><barline location=\"right\"><bar-style>light-heavy</bar-style><repeat direction=\"backward\"/></barline></measure></part>",
                MuseScoreIo.buildMuseImportedPartXml(part, 0, 480, "<m/>", false, 4, 4, null, 0, "major",
                        eventsByMeasureStaff));
    }

    @Test
    public void bridgesTimedEventsToTypedRestImportEvents() {
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 480, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.ParsedMuseScoreMeasure staffOneMeasure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.asList(new MuseScoreIo.TimedEvent(120, false, false, 2, 240)));
        MuseScoreIo.ParsedMuseScoreMeasure staffTwoMeasure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.asList(new MuseScoreIo.TimedEvent(480, false, false, 1, 0)));
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.asList(new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2, Arrays.asList(staffOneMeasure)),
                        new MuseScoreIo.ParsedMuseScoreStaff("2", "F", 4, Arrays.asList(staffTwoMeasure))));

        List<MuseScoreIo.MuseImportedVoiceEvent> staffOneEvents = MuseScoreIo.buildMuseImportedRestVoiceEvents(
                staffOneMeasure.getEvents(), 1);
        assertEquals(1, staffOneEvents.size());
        assertEquals("rest", staffOneEvents.get(0).getKind());
        assertEquals(2, staffOneEvents.get(0).getVoiceNo());
        assertEquals(240, staffOneEvents.get(0).getEventAtDiv(0));
        assertEquals(Integer.valueOf(1), staffOneEvents.get(0).getStaffNo());

        Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>> byStaff = MuseScoreIo
                .buildMuseImportedRestStaffEventsByStaffNo(part, 0, staffOneMeasure);
        assertEquals(2, byStaff.size());
        assertEquals(1, byStaff.get(Integer.valueOf(1)).iterator().next().getStaffNo().intValue());
        assertEquals(2, byStaff.get(Integer.valueOf(2)).iterator().next().getStaffNo().intValue());

        assertEquals(
                "<measure number=\"1\"><attributes><divisions>480</divisions><key><fifths>0</fifths><mode>major</mode></key><time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes>"
                        + "<forward><duration>240</duration><voice>1</voice><staff>1</staff></forward><note><rest/><duration>120</duration><voice>1</voice><type>16th</type><staff>1</staff></note><note><rest/><duration>120</duration><voice>1</voice><type>16th</type><staff>1</staff></note>"
                        + "<backup><duration>480</duration></backup><note><rest/><duration>480</duration><voice>2</voice><type>quarter</type><staff>2</staff></note>"
                        + "<barline location=\"right\"><bar-style>light-heavy</bar-style></barline></measure>",
                MuseScoreIo.buildMuseImportedMeasureXml(part, staffOneMeasure, 0, 0, 1, false, 480, "", true, false,
                        MuseScoreIo.buildMuseImportedPartVoiceIdResolver(part), byStaff));
    }

    @Test
    public void advancesMuseImportedVoiceCursorForEventLoop() {
        MuseScoreIo.MuseImportedVoiceCursorStep lead = MuseScoreIo.advanceMuseImportedVoiceCursorForEvent(
                Integer.valueOf(240), 120, 120, 480, 0, 2, 3);

        assertEquals(240, lead.getEventAtDiv());
        assertEquals("<forward><duration>120</duration><voice>2</voice><staff>3</staff></forward>",
                lead.getForwardXml());
        assertEquals(240, lead.getOccupiedAfterLead());
        assertEquals(360, lead.getOccupiedAfterTimed());
        assertEquals(false, lead.isClamped());

        MuseScoreIo.MuseImportedVoiceCursorStep sameCursor = MuseScoreIo.advanceMuseImportedVoiceCursorForEvent(null,
                360, 0, 480, 0, 2, 3);
        assertEquals(360, sameCursor.getEventAtDiv());
        assertEquals("", sameCursor.getForwardXml());
        assertEquals(360, sameCursor.getOccupiedAfterTimed());

        MuseScoreIo.MuseImportedVoiceCursorStep clamped = MuseScoreIo.advanceMuseImportedVoiceCursorForEvent(
                Integer.valueOf(420), 360, 120, 480, 0, 2, 3);
        assertEquals(true, clamped.isClamped());
        assertEquals(420, clamped.getOccupiedAfterLead());
        assertEquals(420, clamped.getOccupiedAfterTimed());
    }

    @Test
    public void detectsIgnoredMuseImportTags() {
        assertEquals(true, MuseScoreIo.isIgnoredMuseImportTag("TimeSig"));
        assertEquals(true, MuseScoreIo.isIgnoredMuseImportTag("layoutBreak"));
        assertEquals(false, MuseScoreIo.isIgnoredMuseImportTag("Chord"));
    }

    @Test
    public void warnsOnMuseImportMeasureOverflow() {
        List<MuseScoreIo.TimedEvent> events = Arrays.asList(
                new MuseScoreIo.TimedEvent(300, false, false, 1),
                new MuseScoreIo.TimedEvent(250, false, false, 1),
                new MuseScoreIo.TimedEvent(100, false, false, 2));
        List<MuseScoreIo.MuseScoreWarning> warnings = MuseScoreIo.warnOnMuseImportMeasureOverflow(events, 480, 3, 2);
        assertEquals(1, warnings.size());
        assertEquals("MUSESCORE_IMPORT_WARNING", warnings.get(0).getCode());
        assertEquals(Integer.valueOf(3), warnings.get(0).getMeasure());
        assertEquals(Integer.valueOf(1), warnings.get(0).getVoice());
        assertEquals("clamped", warnings.get(0).getAction());
        assertEquals("overfull", warnings.get(0).getReason());
        assertEquals(Integer.valueOf(550), warnings.get(0).getOccupiedDiv());
        assertEquals(Integer.valueOf(480), warnings.get(0).getCapacityDiv());
    }

    @Test
    public void buildsParsedAndFallbackMuseScoreMeasures() {
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(3, 4,
                null, true, 1440, false, -1, "minor", Integer.valueOf(90), "Allegro", true, false, true);
        MuseScoreIo.ParsedMuseScoreMeasure measure = MuseScoreIo.buildParsedMuseScoreMeasure(5, context,
                Arrays.asList(new MuseScoreIo.TimedEvent(1440, false, false, 1)));
        assertEquals(5, measure.getIndex());
        assertEquals(3, measure.getBeats());
        assertEquals(4, measure.getBeatType());
        assertEquals(true, measure.isExplicitTimeSig());
        assertEquals(1440, measure.getCapacityDiv());
        assertEquals(-1, measure.getFifths());
        assertEquals("minor", measure.getMode());
        assertEquals(Integer.valueOf(90), measure.getTempoBpm());
        assertEquals("Allegro", measure.getTempoText());
        assertEquals(true, measure.isRepeatForward());
        assertEquals(false, measure.isRepeatBackward());
        assertEquals(true, measure.isLeftDoubleBarline());
        assertEquals(1, measure.getEvents().size());

        MuseScoreIo.ParsedMuseScoreMeasure fallback = MuseScoreIo.buildFallbackParsedMuseScoreMeasure(2, 4, 4, "cut",
                1920, false, 0, "major");
        assertEquals(2, fallback.getIndex());
        assertEquals("cut", fallback.getTimeSymbol());
        assertEquals(false, fallback.isExplicitTimeSig());
        assertEquals(1920, fallback.getEvents().get(0).getDurationDiv());
    }

    @Test
    public void buildsMuseScoreImportPartListAndIdentificationXml() {
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P&1", "Piano <Solo>", null,
                Arrays.<MuseScoreIo.ParsedMuseScoreStaff>asList());
        assertEquals("<score-part id=\"P&amp;1\"><part-name>Piano &lt;Solo&gt;</part-name></score-part>",
                MuseScoreIo.buildMuseScoreImportPartListXml(Arrays.asList(part)));

        MuseScoreIo.MuseScoreImportMetadata metadata = new MuseScoreIo.MuseScoreImportMetadata("Work", "A&B",
                "Arranger", "Lyricist", "Translator", "Copyright", "2026-05-09");
        assertEquals("<identification><creator type=\"composer\">A&amp;B</creator><creator type=\"arranger\">Arranger</creator><creator type=\"lyricist\">Lyricist</creator><creator type=\"translator\">Translator</creator><rights>Copyright</rights><encoding><encoding-date>2026-05-09</encoding-date></encoding></identification>",
                MuseScoreIo.buildMuseScoreImportIdentificationXml(metadata));
    }

    @Test
    public void collectsMuseImportedVoiceEventsSortedByAtDiv() {
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 1920, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.TimedEvent later = new MuseScoreIo.TimedEvent(120, false, false, 2, 480);
        MuseScoreIo.TimedEvent earlier = new MuseScoreIo.TimedEvent(120, false, false, 2, 120);
        MuseScoreIo.TimedEvent otherVoice = new MuseScoreIo.TimedEvent(120, false, false, 1, 0);
        MuseScoreIo.ParsedMuseScoreMeasure measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.asList(later, otherVoice, earlier));

        List<MuseScoreIo.TimedEvent> events = MuseScoreIo.collectMuseImportedVoiceEvents(measure, 2);
        assertEquals(2, events.size());
        assertEquals(120, events.get(0).getAtDiv());
        assertEquals(480, events.get(1).getAtDiv());
    }

    @Test
    public void convertsTimedEventsToTypedRestEvents() {
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 480, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.TimedEvent later = new MuseScoreIo.TimedEvent(120, false, false, 2, 240);
        MuseScoreIo.TimedEvent earlier = new MuseScoreIo.TimedEvent(120, false, false, 1, 0);
        later.getTupletStops().add(Integer.valueOf(3));
        MuseScoreIo.ParsedMuseScoreMeasure measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.asList(later, earlier));

        List<MuseScoreIo.MuseImportedVoiceEvent> events = MuseScoreIo.collectMuseImportedTypedRestEvents(measure, 4);

        assertEquals(2, events.size());
        assertEquals("rest", events.get(0).getKind());
        assertEquals(1, events.get(0).getVoiceNo());
        assertEquals(0, events.get(0).getEventAtDiv(-1));
        assertEquals(Integer.valueOf(4), events.get(0).getStaffNo());
        assertEquals(2, events.get(1).getVoiceNo());
        assertEquals(240, events.get(1).getEventAtDiv(-1));
        assertEquals(Arrays.asList(Integer.valueOf(3)), events.get(1).getTupletStops());

        assertEquals(
                "<note><rest/><duration>120</duration><voice>1</voice><type>16th</type><staff>4</staff></note><note><rest/><duration>360</duration><voice>1</voice><type>eighth</type><dot/><staff>4</staff></note>"
                        + "<backup><duration>480</duration></backup>"
                        + "<forward><duration>240</duration><voice>2</voice><staff>4</staff></forward><note><rest/><duration>120</duration><voice>2</voice><type>16th</type><staff>4</staff><notations><tuplet type=\"stop\" number=\"3\"/></notations></note><note><rest/><duration>120</duration><voice>2</voice><type>16th</type><staff>4</staff></note>",
                MuseScoreIo.buildMuseImportedStaffVoicesXml(4, 4, 0, 4, 480, 480, false, null, events));
    }

    @Test
    public void resolvesMuseImportedMeasureFallbacksAndAttributes() {
        MuseScoreIo.ParsedMuseScoreMeasure primary = MuseScoreIo.buildFallbackParsedMuseScoreMeasure(1, 3, 4, null,
                1440, false, -1, "minor");
        MuseScoreIo.ParsedMuseScoreStaff staff = new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2,
                Arrays.asList(primary));
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.asList(staff));

        assertEquals(primary, MuseScoreIo.resolveMuseImportedPrimaryMeasure(part, 0, 480, 4, 4, null, 0, "major"));
        MuseScoreIo.ParsedMuseScoreMeasure fallbackPrimary = MuseScoreIo.resolveMuseImportedPrimaryMeasure(part, 1,
                480, 4, 4, null, 0, "major");
        assertEquals(2, fallbackPrimary.getIndex());
        assertEquals(1920, fallbackPrimary.getCapacityDiv());

        MuseScoreIo.ParsedMuseScoreMeasure fallbackStaff = MuseScoreIo.resolveMuseImportedStaffMeasure(part, 1, 0,
                primary);
        assertEquals(1, fallbackStaff.getIndex());
        assertEquals(1440, fallbackStaff.getCapacityDiv());

        assertEquals(true, MuseScoreIo.needsMuseImportedMeasureAttributes(0, primary, 3, 4, null, -1, "minor"));
        assertEquals(false, MuseScoreIo.needsMuseImportedMeasureAttributes(1, primary, 3, 4, null, -1, "minor"));
        assertEquals(true, MuseScoreIo.needsMuseImportedMeasureAttributes(1, primary, 4, 4, null, -1, "minor"));
    }

    @Test
    public void finalizesMuseImportedMeasureXml() {
        MuseScoreIo.ParsedMuseScoreMeasure pickup = MuseScoreIo.buildFallbackParsedMuseScoreMeasure(1, 4, 4, null,
                480, true, 0, "major");
        assertEquals("<measure number=\"0\" implicit=\"yes\"><note/><barline location=\"right\"><bar-style>light-heavy</bar-style></barline></measure>",
                MuseScoreIo.finalizeMuseImportedMeasureXml("<note/>", pickup, 0, 1, true));

        MuseScoreIo.MuseScoreImportMeasureContext repeatContext = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 1920, false, 0, "major", null, null, false, true, false);
        MuseScoreIo.ParsedMuseScoreMeasure repeat = MuseScoreIo.buildParsedMuseScoreMeasure(2, repeatContext,
                Arrays.<MuseScoreIo.TimedEvent>asList());
        assertEquals("<measure number=\"2\"><rest/><barline location=\"right\"><repeat direction=\"backward\"/></barline></measure>",
                MuseScoreIo.finalizeMuseImportedMeasureXml("<rest/>", repeat, 1, 3, false));
    }

    @Test
    public void buildsMuseImportedMeasureHeaderXml() {
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(6, 8,
                "cut", true, 1440, false, 2, "major", Integer.valueOf(120), "Andante", true, false, true);
        MuseScoreIo.ParsedMuseScoreMeasure measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.<MuseScoreIo.TimedEvent>asList());
        MuseScoreIo.ParsedMuseScoreStaff upper = new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2,
                Arrays.asList(measure));
        MuseScoreIo.ParsedMuseScoreStaff lower = new MuseScoreIo.ParsedMuseScoreStaff("2", "F", 4,
                Arrays.asList(measure));
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano",
                new MuseScoreIo.Transpose(Integer.valueOf(-1), Integer.valueOf(-2)), Arrays.asList(upper, lower));

        assertEquals("<attributes><divisions>480</divisions><key><fifths>2</fifths><mode>major</mode></key><time symbol=\"cut\"><beats>6</beats><beat-type>8</beat-type></time><transpose><diatonic>-1</diatonic><chromatic>-2</chromatic></transpose><staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign><line>4</line></clef><miscellaneous><misc/></miscellaneous></attributes><barline location=\"left\"><bar-style>light-light</bar-style></barline><barline location=\"left\"><repeat direction=\"forward\"/></barline><direction placement=\"above\"><direction-type><words>Andante</words></direction-type><sound tempo=\"120\"/></direction>",
                MuseScoreIo.buildMuseImportedMeasureHeaderXml(measure, part, 0, 480, "<misc/>", true));
    }

    @Test
    public void buildsMuseImportedPartVoiceIdResolver() {
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4, null,
                false, 1920, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.ParsedMuseScoreMeasure staff1Measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.asList(new MuseScoreIo.TimedEvent(120, false, false, 2),
                        new MuseScoreIo.TimedEvent(120, false, false, 1)));
        MuseScoreIo.ParsedMuseScoreMeasure staff2Measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.asList(new MuseScoreIo.TimedEvent(120, false, false, 1)));
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.asList(new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2, Arrays.asList(staff1Measure)),
                        new MuseScoreIo.ParsedMuseScoreStaff("2", "F", 4, Arrays.asList(staff2Measure))));

        MuseScoreIo.MuseImportedPartVoiceIdResolver resolver = MuseScoreIo.buildMuseImportedPartVoiceIdResolver(part);
        assertEquals(1, resolver.resolve(1, 1));
        assertEquals(2, resolver.resolve(1, 2));
        assertEquals(3, resolver.resolve(2, 1));
        assertEquals(4, resolver.resolve(2, 3));
        assertEquals(Integer.valueOf(4), resolver.getVoiceIdByStaffLocal().get("2:3"));
    }

    @Test
    public void chunksStringsAndBuildsMuseScoreWarningMiscXml() {
        assertEquals(Arrays.asList("ab", "cd", "e"), MuseScoreIo.chunkString("abcde", 2));

        List<MuseScoreIo.MuseScoreWarning> warnings = MuseScoreIo.warnOnMuseImportMeasureOverflow(Arrays.asList(
                new MuseScoreIo.TimedEvent(300, false, false, 1),
                new MuseScoreIo.TimedEvent(250, false, false, 1)), 480, 3, 2);
        assertEquals("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field><miscellaneous-field name=\"mks:diag:0001\">level=warn;code=MUSESCORE_IMPORT_WARNING;fmt=mscx;message=measure 3 voice 1: overfull content (550 &gt; 480); tail events are clamped.;measure=3;staff=2;voice=1;action=clamped;reason=overfull;occupiedDiv=550;capacityDiv=480</miscellaneous-field>",
                MuseScoreIo.buildWarningMiscXml(warnings));
    }

    @Test
    public void buildsMuseScoreSourceAndImportMiscXml() {
        assertEquals("<miscellaneous-field name=\"mks:src:musescore:raw-encoding\">uri-v1</miscellaneous-field><miscellaneous-field name=\"mks:src:musescore:raw-length\">5</miscellaneous-field><miscellaneous-field name=\"mks:src:musescore:raw-encoded-length\">11</miscellaneous-field><miscellaneous-field name=\"mks:src:musescore:raw-chunks\">1</miscellaneous-field><miscellaneous-field name=\"mks:src:musescore:raw-0001\">A%20%26%20B</miscellaneous-field>",
                MuseScoreIo.buildSourceMiscXml("A & B"));

        MuseScoreIo.ResolvedMuseScoreImportOptions withAll = MuseScoreIo.resolveMuseScoreImportOptions(null, null,
                null, null);
        List<MuseScoreIo.MuseScoreWarning> warnings = MuseScoreIo.warnOnMuseImportMeasureOverflow(Arrays.asList(
                new MuseScoreIo.TimedEvent(500, false, false, 1)), 480, 1, 1);
        String misc = MuseScoreIo.buildMuseScoreImportMiscXml("A", "4.0", withAll, warnings);
        assertEquals(true, misc.contains("mks:diag:count"));
        assertEquals(true, misc.contains("mks:src:musescore:raw-encoding"));
        assertEquals(true, misc.contains("mks:src:musescore:version\">4.0</miscellaneous-field>"));

        MuseScoreIo.ResolvedMuseScoreImportOptions withoutAll = MuseScoreIo.resolveMuseScoreImportOptions(
                Boolean.FALSE, Boolean.FALSE, null, null);
        assertEquals("", MuseScoreIo.buildMuseScoreImportMiscXml("A", "4.0", withoutAll, warnings));
    }

    @Test
    public void buildsMuseScoreImportDocumentXml() {
        MuseScoreIo.ParsedMuseScorePart part = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.<MuseScoreIo.ParsedMuseScoreStaff>asList());
        MuseScoreIo.MuseScoreImportMetadata metadata = new MuseScoreIo.MuseScoreImportMetadata("Work & Title",
                "Subtitle", "Movement", "1", "Op.1", "Composer", "", "", "", "", "");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>Work &amp; Title</work-title><work-number>Op.1</work-number></work><movement-title>Movement</movement-title><movement-number>1</movement-number><credit page=\"1\"><credit-type>subtitle</credit-type><credit-words>Subtitle</credit-words></credit><identification><creator type=\"composer\">Composer</creator></identification><part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list><part id=\"P1\"></part></score-partwise>",
                MuseScoreIo.buildMuseScoreImportDocumentXml(Arrays.asList(part), metadata,
                        Arrays.asList("<part id=\"P1\"></part>")));

        List<MuseScoreIo.MuseScoreChordNote> c4 = MuseScoreIo.parseMuseChordNotes(Arrays.asList(
                new MuseScoreIo.MuseScoreChordNoteInput(Integer.valueOf(60), null, null, null, null, null)), 0);
        MuseScoreIo.MuseScoreImportMeasureContext context = new MuseScoreIo.MuseScoreImportMeasureContext(4, 4,
                null, false, 480, false, 0, "major", null, null, false, false, false);
        MuseScoreIo.ParsedMuseScoreMeasure measure = MuseScoreIo.buildParsedMuseScoreMeasure(1, context,
                Arrays.<MuseScoreIo.TimedEvent>asList(new MuseScoreIo.TimedEvent(120, false, false, 1)));
        MuseScoreIo.ParsedMuseScorePart typedPart = new MuseScoreIo.ParsedMuseScorePart("P1", "Piano", null,
                Arrays.asList(new MuseScoreIo.ParsedMuseScoreStaff("1", "G", 2, Arrays.asList(measure))));
        Map<Integer, Map<Integer, Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>>> eventsByPart = new HashMap<Integer, Map<Integer, Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>>>();
        Map<Integer, Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>> eventsByMeasure = new HashMap<Integer, Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>>();
        Map<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>> eventsByStaff = new HashMap<Integer, Collection<MuseScoreIo.MuseImportedVoiceEvent>>();
        eventsByStaff.put(Integer.valueOf(1),
                Arrays.asList(MuseScoreIo.MuseImportedVoiceEvent.chordForVoice(Integer.valueOf(0), 1, 120, null,
                        Integer.valueOf(1), c4, false, false, null, null, null, null, null, null, null, null,
                        null)));
        eventsByMeasure.put(Integer.valueOf(0), eventsByStaff);
        eventsByPart.put(Integer.valueOf(0), eventsByMeasure);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>Work &amp; Title</work-title><work-number>Op.1</work-number></work><movement-title>Movement</movement-title><movement-number>1</movement-number><credit page=\"1\"><credit-type>subtitle</credit-type><credit-words>Subtitle</credit-words></credit><identification><creator type=\"composer\">Composer</creator></identification><part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions><key><fifths>0</fifths><mode>major</mode></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type><staff>1</staff></note><note><rest/><duration>360</duration><voice>1</voice><type>eighth</type><dot/><staff>1</staff></note><barline location=\"right\"><bar-style>light-heavy</bar-style></barline></measure></part></score-partwise>",
                MuseScoreIo.buildMuseScoreImportDocumentXml(Arrays.asList(typedPart), metadata, 480, "", false, 4,
                        4, null, 0, "major", eventsByPart));
    }

    @Test
    public void buildsMuseScoreExportChordAndRestXml() {
        assertEquals("1/4", MuseScoreIo.fractionFromDivisions(480, 480));
        assertEquals("3/8", MuseScoreIo.fractionFromDivisions(720, 480));
        assertEquals("<Rest><durationType>quarter</durationType><Tuplet>t1</Tuplet></Rest>",
                MuseScoreIo.makeMuseRestXml(480, 0, 480, " t1 "));
        assertEquals("<Rest><durationType>quarter</durationType><dots>1</dots></Rest>",
                MuseScoreIo.makeMuseRestXml(720, 720, 480, null));

        MuseScoreIo.MuseScoreExportChord chord = new MuseScoreIo.MuseScoreExportChord(720, 720, 480,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(128, true, false, "accidentalSharp&",
                        "  2<  ", Integer.valueOf(3)),
                        new MuseScoreIo.MuseScoreExportNote(-3, false, true, null, "", Integer.valueOf(0))),
                Arrays.asList(""), Arrays.asList("1/8"), Arrays.asList("articStaccato<"), false,
                Arrays.asList(Integer.valueOf(1)), Arrays.asList(Integer.valueOf(2)), " tup<1 ",
                Arrays.asList("8va"), 1, true, true);

        assertEquals("<Chord><acciaccatura/><durationType>quarter</durationType><Tuplet>tup&lt;1</Tuplet><dots>1</dots><Spanner type=\"Ottava\"><Ottava><subtype>8va</subtype></Ottava><next><location><fractions>1/1</fractions></location></next></Spanner><Spanner type=\"Ottava\"><prev><location><fractions>-1/1</fractions></location></prev></Spanner><Spanner type=\"Trill\"><Trill><subtype>trill</subtype></Trill><next><location><fractions>1/1</fractions></location></next></Spanner><Spanner type=\"Trill\"><prev><location><fractions>-1/1</fractions></location></prev></Spanner><Spanner type=\"Slur\"><prev><location><fractions>-1/8</fractions></location></prev></Spanner><Spanner type=\"Slur\"><Slur/><next><location><fractions>3/8</fractions></location></next></Spanner><Articulation><subtype>articStaccato&lt;</subtype></Articulation><Note><pitch>127</pitch><Accidental><subtype>accidentalSharp&amp;</subtype></Accidental><Fingering>2&lt;</Fingering><String>3</String><Tie/></Note><Note><pitch>0</pitch><endSpanner/></Note></Chord>",
                MuseScoreIo.makeMuseChordXml(chord));

        MuseScoreIo.MuseScoreExportChord trillMarkOnly = new MuseScoreIo.MuseScoreExportChord(480, 0, 480,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(60, false, false, null, null, null)), null, null,
                null, true, null, null, null, null, 0, false, false);
        assertEquals("<Chord><durationType>quarter</durationType><Ornament><subtype>ornamentTrill</subtype></Ornament><Note><pitch>60</pitch></Note></Chord>",
                MuseScoreIo.makeMuseChordXml(trillMarkOnly));
    }

    @Test
    public void queuesAndConsumesMusePendingDirectionMarks() {
        assertEquals(240, MuseScoreIo.normalizeMuseVoiceEventDuration(120, 480, 240));
        assertEquals(0, MuseScoreIo.normalizeMuseVoiceEventDuration(-20, 480, 240));
        assertEquals(120, MuseScoreIo.processMuseVoiceEventBackupCursor(360, 120, 480, 240));
        assertEquals(0, MuseScoreIo.processMuseVoiceEventBackupCursor(120, 240, 480, 240));
        assertEquals(600, MuseScoreIo.processMuseVoiceEventForwardCursor(360, 120, 480, 240));
        assertEquals(0, MuseScoreIo.processMuseVoiceEventForwardCursor(-20, -120, 480, 240));

        MuseScoreIo.MusicXmlDirectionMarkPayload payload = MuseScoreIo.parseMusicXmlDirectionMarkPayloadValues("2",
                "3", Arrays.asList(new MuseScoreIo.MusicXmlOctaveShiftSource("up", "15"),
                        new MuseScoreIo.MusicXmlOctaveShiftSource("stop", "8"),
                        new MuseScoreIo.MusicXmlOctaveShiftSource("continue", "8")),
                "yes", "1");
        assertEquals(2, payload.getStaffNo());
        assertEquals(3, payload.getVoiceNo());
        assertEquals(Arrays.asList("15ma"), payload.getMarks().getOttavaStartSubtypes());
        assertEquals(1, payload.getMarks().getOttavaStopCount());
        assertEquals(1, payload.getMarks().getRepeatForwardCount());
        assertEquals(1, payload.getMarks().getRepeatBackwardCount());
        assertNull(MuseScoreIo.parseMusicXmlDirectionMarkPayloadValues(null, null, null, "", ""));

        List<MuseScoreIo.MusePendingDirectionMarkEntry> routed = new ArrayList<MuseScoreIo.MusePendingDirectionMarkEntry>();
        MuseScoreIo.processMuseVoiceEventDirectionMarks(routed, 96, payload);
        MuseScoreIo.processMuseVoiceEventMidBarlineMarks(routed, 144,
                MuseScoreIo.parseMusicXmlMidBarlineRepeatMarks("middle", Arrays.asList("forward")));
        MuseScoreIo.processMuseVoiceEventDirectionMarks(routed, 192, null);
        MuseScoreIo.processMuseVoiceEventMidBarlineMarks(routed, 240, null);
        assertEquals(2, routed.size());
        assertEquals(2, routed.get(0).getStaffNo());
        assertEquals(3, routed.get(0).getVoiceNo());
        assertEquals(96, routed.get(0).getAtDiv());
        assertEquals(1, routed.get(1).getStaffNo());
        assertEquals(1, routed.get(1).getVoiceNo());
        assertEquals(144, routed.get(1).getAtDiv());
        assertEquals(1, routed.get(1).getMarks().getRepeatForwardCount());

        List<MuseScoreIo.MusePendingDirectionMarkEntry> pending = new ArrayList<MuseScoreIo.MusePendingDirectionMarkEntry>();
        MuseScoreIo.queueMusePendingDirectionMarks(pending, 1, 2, 120,
                new MuseScoreIo.MusePendingDirectionMarks(Arrays.asList("8va"), 1, 0, 1));
        MuseScoreIo.queueMusePendingDirectionMarks(pending, 1, 2, 120,
                new MuseScoreIo.MusePendingDirectionMarks(Arrays.asList("15ma"), 0, 2, 0));
        MuseScoreIo.queueMusePendingDirectionMarks(pending, 1, 2, 240,
                new MuseScoreIo.MusePendingDirectionMarks(Arrays.asList("8vb"), 3, 1, 0));
        MuseScoreIo.queueMusePendingDirectionMarks(pending, 2, 1, 120,
                new MuseScoreIo.MusePendingDirectionMarks(Arrays.asList("ignored"), 1, 1, 1));

        assertEquals(3, pending.size());
        assertEquals(Arrays.asList("8va", "15ma"), pending.get(0).getMarks().getOttavaStartSubtypes());
        assertEquals(1, pending.get(0).getMarks().getOttavaStopCount());
        assertEquals(2, pending.get(0).getMarks().getRepeatForwardCount());
        assertEquals(1, pending.get(0).getMarks().getRepeatBackwardCount());

        assertNull(MuseScoreIo.consumeMusePendingDirectionMarks(pending, 1, 2, 100));
        MuseScoreIo.MusePendingDirectionMarks consumed = MuseScoreIo.consumeMusePendingDirectionMarks(pending, 1, 2,
                200);
        assertEquals(Arrays.asList("8va", "15ma"), consumed.getOttavaStartSubtypes());
        assertEquals(1, consumed.getOttavaStopCount());
        assertEquals(2, consumed.getRepeatForwardCount());
        assertEquals(1, consumed.getRepeatBackwardCount());
        assertEquals(2, pending.size());

        MuseScoreIo.MusePendingDirectionMarks later = MuseScoreIo.consumeMusePendingDirectionMarks(pending, 1, 2,
                240);
        assertEquals(Arrays.asList("8vb"), later.getOttavaStartSubtypes());
        assertEquals(3, later.getOttavaStopCount());
        assertEquals(1, later.getRepeatForwardCount());
        assertEquals(1, pending.size());
    }

    @Test
    public void mergesChordFollowMusicXmlNoteEvent() {
        Map<Integer, Map<Integer, List<MuseScoreIo.MuseVoiceEvent>>> byStaff = new HashMap<Integer, Map<Integer, List<MuseScoreIo.MuseVoiceEvent>>>();
        List<MuseScoreIo.MuseVoiceEvent> created = MuseScoreIo.pushMuseVoiceEventList(byStaff, 0, 2);
        created.add(new MuseScoreIo.MuseVoiceEvent(0, 120, null, null, 0, false, false));
        assertEquals(created, byStaff.get(Integer.valueOf(1)).get(Integer.valueOf(2)));
        assertEquals(created, MuseScoreIo.pushMuseVoiceEventList(byStaff, 1, 2));
        assertEquals(0, MuseScoreIo.pushMuseVoiceEventList(null, 1, 1).size());

        MuseScoreIo.MusePendingDirectionMarks marks = new MuseScoreIo.MusePendingDirectionMarks(
                Arrays.asList("8va"), 1, 1, 0);
        MuseScoreIo.TimeModification triplet = new MuseScoreIo.TimeModification(3, 2);
        MuseScoreIo.MuseVoiceEvent rest = MuseScoreIo.buildRestMuseVoiceEventFromMusicXmlValues(48, 120, triplet,
                Arrays.asList(Integer.valueOf(1)), Arrays.asList(Integer.valueOf(2)), marks);
        assertEquals(48, rest.getAtDiv());
        assertEquals(120, rest.getDurationDiv());
        assertNull(rest.getPitches());
        assertEquals(triplet, rest.getTupletTimeModification());
        assertEquals(Arrays.asList(Integer.valueOf(1)), rest.getTupletStarts());
        assertEquals(Arrays.asList(Integer.valueOf(2)), rest.getTupletStops());
        assertEquals(Arrays.asList("8va"), rest.getOttavaStartSubtypes());
        assertEquals(1, rest.getOttavaStopCount());
        assertEquals(true, rest.isRepeatForwardAtStart());
        assertEquals(false, rest.isRepeatBackwardAtStart());

        MuseScoreIo.MuseVoiceEvent single = MuseScoreIo.buildChordMuseVoiceEventFromMusicXmlValues(96, 240, true,
                true, triplet, Arrays.asList(Integer.valueOf(3)), Arrays.asList(Integer.valueOf(4)),
                new MuseScoreIo.MuseScoreExportNote(67, true, false, "accidentalNatural", "3", null),
                Arrays.asList(Integer.valueOf(5)), Arrays.asList(Integer.valueOf(6)),
                Arrays.asList(Integer.valueOf(7)), Arrays.asList(Integer.valueOf(8)), true,
                Arrays.asList("articTenutoAbove"), marks);
        assertEquals(1, single.getPitches().size());
        assertEquals(67, single.getPitches().get(0).getMidi());
        assertEquals(Arrays.asList(Integer.valueOf(3)), single.getTupletStarts());
        assertEquals(Arrays.asList(Integer.valueOf(4)), single.getTupletStops());
        assertEquals(Arrays.asList(Integer.valueOf(5)), single.getSlurStarts());
        assertEquals(Arrays.asList(Integer.valueOf(6)), single.getSlurStops());
        assertEquals(Arrays.asList(Integer.valueOf(7)), single.getTrillStarts());
        assertEquals(Arrays.asList(Integer.valueOf(8)), single.getTrillStops());
        assertEquals(Arrays.asList("articTenutoAbove"), single.getArticulationSubtypes());
        assertEquals(true, single.isTrillMarkOnly());
        assertEquals(true, single.isGrace());
        assertEquals(true, single.isGraceSlash());
        assertNull(MuseScoreIo.buildChordMuseVoiceEventFromMusicXmlValues(0, 120, false, false, null, null, null,
                null, null, null, null, null, false, null, null));
        assertEquals(336, MuseScoreIo.advanceMuseVoiceEventCursorAfterNote(96, false, false, 240));
        assertEquals(96, MuseScoreIo.advanceMuseVoiceEventCursorAfterNote(96, true, false, 240));
        assertEquals(96, MuseScoreIo.advanceMuseVoiceEventCursorAfterNote(96, false, true, 240));
        assertEquals(0, MuseScoreIo.advanceMuseVoiceEventCursorAfterNote(-12, false, false, -1));

        MuseScoreIo.MuseVoiceEvent previous = new MuseScoreIo.MuseVoiceEvent(0, 240,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(60, true, false, "accidentalSharp", "1",
                        Integer.valueOf(2))),
                Arrays.asList(Integer.valueOf(1)), null, null, Arrays.asList(Integer.valueOf(2)), null,
                Arrays.asList("articAccentAbove"), false, Arrays.asList(Integer.valueOf(3)), null,
                Arrays.asList("8va"), 1, false, false, true, false);

        MuseScoreIo.MuseVoiceEvent merged = MuseScoreIo.mergeChordFollowMusicXmlNoteEvent(previous, true, false, 240,
                new MuseScoreIo.MuseScoreExportNote(64, false, true, null, "2", Integer.valueOf(3)),
                Arrays.asList(Integer.valueOf(2), Integer.valueOf(4)), Arrays.asList(Integer.valueOf(5)),
                Arrays.asList(Integer.valueOf(3), Integer.valueOf(6)), Arrays.asList(Integer.valueOf(7)), true,
                Arrays.asList(Integer.valueOf(1), Integer.valueOf(8)), Arrays.asList(Integer.valueOf(9)), triplet,
                Arrays.asList("articAccentAbove", "articStaccatoAbove"), true, true);

        assertEquals(2, merged.getPitches().size());
        assertEquals(64, merged.getPitches().get(1).getMidi());
        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(8)), merged.getTupletStarts());
        assertEquals(Arrays.asList(Integer.valueOf(9)), merged.getTupletStops());
        assertEquals(triplet, merged.getTupletTimeModification());
        assertEquals(Arrays.asList(Integer.valueOf(2), Integer.valueOf(4)), merged.getSlurStarts());
        assertEquals(Arrays.asList(Integer.valueOf(5)), merged.getSlurStops());
        assertEquals(Arrays.asList(Integer.valueOf(3), Integer.valueOf(6)), merged.getTrillStarts());
        assertEquals(Arrays.asList(Integer.valueOf(7)), merged.getTrillStops());
        assertEquals(Arrays.asList("articAccentAbove", "articStaccatoAbove"), merged.getArticulationSubtypes());
        assertEquals(true, merged.isTrillMarkOnly());
        assertEquals(true, merged.isGrace());
        assertEquals(true, merged.isGraceSlash());
        assertEquals(true, merged.isRepeatForwardAtStart());
        assertEquals(false, merged.isRepeatBackwardAtStart());

        assertNull(MuseScoreIo.mergeChordFollowMusicXmlNoteEvent(previous, false, false, 240,
                new MuseScoreIo.MuseScoreExportNote(64, false, false, null, null, null), null, null, null, null,
                false, null, null, null, null, false, false));
        assertNull(MuseScoreIo.mergeChordFollowMusicXmlNoteEvent(previous, true, false, 120,
                new MuseScoreIo.MuseScoreExportNote(64, false, false, null, null, null), null, null, null, null,
                false, null, null, null, null, false, false));
    }

    @Test
    public void appliesMuseTrailingDirectionMarksToLastVoiceEvents() {
        MuseScoreIo.MuseVoiceEvent first = new MuseScoreIo.MuseVoiceEvent(0, 120,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(60, false, false, null, null, null)),
                Arrays.asList("8va"), 0, false, false);
        MuseScoreIo.MuseVoiceEvent last = new MuseScoreIo.MuseVoiceEvent(120, 120,
                Arrays.asList(new MuseScoreIo.MuseScoreExportNote(62, false, false, null, null, null)),
                Arrays.asList("8va"), 1, false, false);
        Map<Integer, List<MuseScoreIo.MuseVoiceEvent>> byVoice = new HashMap<Integer, List<MuseScoreIo.MuseVoiceEvent>>();
        byVoice.put(Integer.valueOf(1), new ArrayList<MuseScoreIo.MuseVoiceEvent>(Arrays.asList(first, last)));
        Map<Integer, Map<Integer, List<MuseScoreIo.MuseVoiceEvent>>> byStaff = new HashMap<Integer, Map<Integer, List<MuseScoreIo.MuseVoiceEvent>>>();
        byStaff.put(Integer.valueOf(1), byVoice);

        List<MuseScoreIo.MusePendingDirectionMarkEntry> pending = Arrays.asList(
                new MuseScoreIo.MusePendingDirectionMarkEntry(1, 1, 240,
                        new MuseScoreIo.MusePendingDirectionMarks(Arrays.asList("8va", "15ma"), 2, 1, 0)),
                new MuseScoreIo.MusePendingDirectionMarkEntry(1, 2, 240,
                        new MuseScoreIo.MusePendingDirectionMarks(Arrays.asList("ignored"), 1, 1, 1)),
                new MuseScoreIo.MusePendingDirectionMarkEntry(2, 1, 240,
                        new MuseScoreIo.MusePendingDirectionMarks(Arrays.asList("ignored"), 1, 1, 1)),
                new MuseScoreIo.MusePendingDirectionMarkEntry(1, 1, 360,
                        new MuseScoreIo.MusePendingDirectionMarks(null, 0, 0, 1)));

        MuseScoreIo.applyMuseTrailingDirectionMarks(byStaff, pending);

        assertEquals(Arrays.asList("8va"), first.getOttavaStartSubtypes());
        assertEquals(Arrays.asList("8va", "15ma"), last.getOttavaStartSubtypes());
        assertEquals(3, last.getOttavaStopCount());
        assertEquals(true, last.isRepeatForwardAtStart());
        assertEquals(true, last.isRepeatBackwardAtStart());
    }

    @Test
    public void routesPublicMuseScoreConversionOptionsToCutTimeAndImplicitBeamBehavior() {
        String cutMscx = "<museScore version=\"3.02\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><TimeSig><subtype>2</subtype><sigN>4</sigN><sigD>4</sigD></TimeSig>"
                + "<Chord><durationType>whole</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";
        String normalizedCut = MuseScoreIo.convertMuseScoreToMusicXml(cutMscx, true, false);
        assertEquals(true, normalizedCut.contains("<time symbol=\"cut\"><beats>2</beats><beat-type>2</beat-type>"));

        String beamMscx = "<museScore version=\"3.02\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><TimeSig><sigN>6</sigN><sigD>8</sigD></TimeSig>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>64</pitch></Note></Chord>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>65</pitch></Note></Chord>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>67</pitch></Note></Chord>"
                + "<Chord><durationType>eighth</durationType><Note><pitch>69</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";
        String defaultBeamXml = MuseScoreIo.convertMuseScoreToMusicXml(beamMscx);
        assertEquals(true, defaultBeamXml.contains("<beam number=\"1\">begin</beam>"));
        assertEquals(true, defaultBeamXml.contains("<beam number=\"1\">continue</beam>"));
        assertEquals(true, defaultBeamXml.contains("<beam number=\"1\">end</beam>"));
        assertEquals(false, MuseScoreIo.convertMuseScoreToMusicXml(beamMscx, false, false)
                .contains("<beam number=\"1\">"));
    }

    @Test
    public void routesPublicMuseScoreSourceAndDebugMetadataOptions() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch>"
                + "</Note></Chord></voice></Measure></Staff></Score></museScore>";
        String defaults = MuseScoreIo.convertMuseScoreToMusicXml(mscx);
        assertEquals(true, defaults.contains("<miscellaneous-field name=\"mks:src:musescore:raw-encoding\">uri-v1"));
        assertEquals(true, defaults.contains("name=\"mks:src:musescore:version\">4.0</miscellaneous-field>"));

        String noMetadata = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);
        assertEquals(false, noMetadata.contains("mks:src:musescore:"));
        assertEquals(false, noMetadata.contains("mks:diag:"));
    }

    @Test
    public void emitsPlaceholderWarningMetadataThroughThePublicMuseScoreImportFacade() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division></Score></museScore>";

        String defaults = MuseScoreIo.convertMuseScoreToMusicXml(mscx);
        assertEquals(true, defaults.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"));
        assertEquals(true, defaults.contains("No readable staff content found; created an empty placeholder score."));
        assertEquals(true, defaults.contains("action=placeholder-created"));

        String noDebug = MuseScoreIo.convertMuseScoreToMusicXml(mscx, true, false, false, true);
        assertEquals(false, noDebug.contains("mks:diag:"));
    }

    @Test
    public void routesPublicMuseScoreImportMetadataAndVBoxFallbacks() {
        String mscx = "<museScore version=\"4.0\"><Score>"
                + "<metaTag name=\"workTitle\"> Untitled Score </metaTag>"
                + "<metaTag name=\"subtitle\"> Sub &amp; title </metaTag>"
                + "<metaTag name=\"movementTitle\"> Movement </metaTag>"
                + "<metaTag name=\"movementNumber\"> II </metaTag>"
                + "<metaTag name=\"workNumber\"> Op. 42 </metaTag>"
                + "<metaTag name=\"composer\"> Composer / Arranger </metaTag>"
                + "<metaTag name=\"arranger\"> Arranger </metaTag>"
                + "<metaTag name=\"lyricist\"> Lyricist </metaTag>"
                + "<metaTag name=\"translator\"> Translator </metaTag>"
                + "<metaTag name=\"copyright\"> Copyright </metaTag>"
                + "<metaTag name=\"creationDate\"> 2026-08-09 </metaTag><Division>480</Division>"
                + "<Staff id=\"1\"><VBox><Text><style>title</style><text> VBox title </text></Text>"
                + "<Text><style>composer</style><text> VBox composer </text></Text></VBox>"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch>"
                + "</Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<work-title>VBox title</work-title><work-number>Op. 42</work-number>"));
        assertEquals(true, musicXml.contains("<movement-title>Movement</movement-title><movement-number>II</movement-number>"));
        assertEquals(true, musicXml.contains("<credit page=\"1\"><credit-type>subtitle</credit-type>"
                + "<credit-words>Sub &amp; title</credit-words></credit>"));
        assertEquals(true, musicXml.contains("<creator type=\"composer\">VBox composer</creator>"));
        assertEquals(true, musicXml.contains("<creator type=\"arranger\">Arranger</creator>"));
        assertEquals(true, musicXml.contains("<creator type=\"lyricist\">Lyricist</creator>"));
        assertEquals(true, musicXml.contains("<creator type=\"translator\">Translator</creator>"));
        assertEquals(true, musicXml.contains("<rights>Copyright</rights><encoding><encoding-date>2026-08-09"));
    }

    @Test
    public void groupsUnclaimedMuseScoreStaffsAsSeparateFallbackParts() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division>"
                + "<Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff>"
                + "<Staff id=\"2\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>62</pitch></Note></Chord></voice></Measure></Staff>"
                + "</Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<score-part id=\"P1\"><part-name>P1</part-name></score-part>"));
        assertEquals(true, musicXml.contains("<score-part id=\"P2\"><part-name>P2</part-name></score-part>"));
        assertEquals(2, musicXml.split("<part id=\"P", -1).length - 1);
        assertEquals(true, musicXml.contains("<part id=\"P1\"><measure"));
        assertEquals(true, musicXml.contains("<part id=\"P2\"><measure"));
    }

    @Test
    public void skipsEmptyMuseScorePartsAndUsesUniqueDeclaredStaffsBeforeFallbacks() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division>"
                + "<Part><trackName>Ignored empty part</trackName></Part>"
                + "<Part><Instrument><longName>Lead</longName></Instrument><Staff id=\"1\"/></Part>"
                + "<Part><trackName>Backup</trackName><Staff id=\"1\"/><Staff id=\"1\"/></Part>"
                + "<Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff>"
                + "<Staff id=\"2\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>62</pitch></Note></Chord></voice></Measure></Staff>"
                + "<Staff id=\"3\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>64</pitch></Note></Chord></voice></Measure></Staff>"
                + "</Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<score-part id=\"P1\"><part-name>Lead</part-name></score-part>"));
        assertEquals(true, musicXml.contains("<score-part id=\"P2\"><part-name>Backup</part-name></score-part>"));
        assertEquals(false, musicXml.contains("Ignored empty part"));
        assertEquals(2, musicXml.split("<part id=\"P", -1).length - 1);
        assertEquals(true, musicXml.contains("<part id=\"P2\"><measure number=\"1\"><attributes><divisions>480"
                + "</divisions><staves>2</staves>"));
    }

    @Test
    public void importsMuseScorePartTransposeAndUsesItsWrittenKey() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Part>"
                + "<Instrument><transposeDiatonic>-1.2</transposeDiatonic>"
                + "<transposeChromatic>-2</transposeChromatic></Instrument><Staff id=\"1\"/></Part>"
                + "<Staff id=\"1\"><Measure><voice><KeySig><transposeKey>-2</transposeKey>"
                + "<accidental>2</accidental><concertKey>3</concertKey></KeySig>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<key><fifths>-2</fifths><mode>major</mode></key>"));
        assertEquals(true, musicXml.contains("<transpose><diatonic>-1</diatonic><chromatic>-2</chromatic>"
                + "</transpose>"));
    }

    @Test
    public void importsMuseScoreKeyModesFromTitleFallbackAndMeasureKeySignature() {
        String titleFallbackMscx = "<museScore version=\"4.0\"><Score>"
                + "<metaTag name=\"workTitle\">Nocturne in minor</metaTag><Division>480</Division>"
                + "<Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";
        String explicitModeMscx = "<museScore version=\"4.0\"><Score><Division>480</Division>"
                + "<Staff id=\"1\"><Measure><voice><keysig><accidental>-3</accidental>"
                + "<mode>major</mode></keysig><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String titleFallbackXml = MuseScoreIo.convertMuseScoreToMusicXml(titleFallbackMscx, false, false, false, false);
        String explicitModeXml = MuseScoreIo.convertMuseScoreToMusicXml(explicitModeMscx, false, false, false, false);

        assertEquals(true, titleFallbackXml.contains("<key><fifths>0</fifths><mode>minor</mode></key>"));
        assertEquals(true, explicitModeXml.contains("<key><fifths>-3</fifths><mode>major</mode></key>"));
    }

    @Test
    public void importsMuseScoreMeasureLengthAsAnImplicitPickupMeasure() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure len=\"1 / 4\"><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<measure number=\"0\" implicit=\"yes\">"));
        assertEquals(true, musicXml.contains("<duration>480</duration>"));
    }

    @Test
    public void importsMuseScoreRepeatAndDoubleBarlineMarkers() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure startRepeat=\"yes\"><BarLine><subtype>double</subtype></BarLine><voice>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure><Measure><voice><BarLine><subtype>end-repeat</subtype></BarLine>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<barline location=\"left\"><bar-style>light-light</bar-style>"
                + "</barline><barline location=\"left\"><repeat direction=\"forward\"/></barline>"));
        assertEquals(true, musicXml.contains("<barline location=\"right\"><bar-style>light-heavy</bar-style>"
                + "<repeat direction=\"backward\"/></barline>"));
    }

    @Test
    public void importsStaffSpecificMuseScoreClefsForGrandStaffParts() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division>"
                + "<Part><Staff id=\"1\"/><Staff id=\"2\"/></Part>"
                + "<Staff id=\"1\"><Measure><voice><Clef><concertClefType>G2</concertClefType></Clef>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff>"
                + "<Staff id=\"2\"><Measure><voice><Clef><concertClefType>F4</concertClefType></Clef>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>48</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<staves>2</staves>"));
        assertEquals(true, musicXml.contains("<clef number=\"1\"><sign>G</sign><line>2</line></clef>"));
        assertEquals(true, musicXml.contains("<clef number=\"2\"><sign>F</sign><line>4</line></clef>"));
    }

    @Test
    public void importsMuseScorePartDefaultAndInstrumentStaffClefs() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Part>"
                + "<Staff id=\"1\"/><Staff id=\"2\"><defaultClef>F</defaultClef></Staff>"
                + "<Instrument><clef>C3</clef><clef staff=\"2\">C4</clef></Instrument></Part>"
                + "<Staff id=\"1\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff>"
                + "<Staff id=\"2\"><Measure><voice><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>48</pitch></Note></Chord></voice></Measure></Staff>"
                + "</Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<clef number=\"1\"><sign>C</sign><line>3</line></clef>"));
        assertEquals(true, musicXml.contains("<clef number=\"2\"><sign>C</sign><line>4</line></clef>"));
    }

    @Test
    public void prioritizesDirectMuseScoreSignaturesAndRetainsCutTimeSymbol() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><TimeSig><subtype>2</subtype><sigN>3</sigN><sigD>4</sigD></TimeSig>"
                + "<KeySig><accidental>-2</accidental></KeySig><voice><TimeSig><sigN>4</sigN><sigD>4</sigD>"
                + "</TimeSig><KeySig><accidental>4</accidental></KeySig><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure>"
                + "<Measure><TimeSig><sigN>2</sigN><sigD>4</sigD></TimeSig><voice>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<key><fifths>-2</fifths><mode>major</mode></key>"));
        assertEquals(true, musicXml.contains("<time symbol=\"cut\"><beats>3</beats><beat-type>4</beat-type>"));
        assertEquals(true, musicXml.contains("<measure number=\"2\"><attributes><key><fifths>-2</fifths>"
                + "<mode>major</mode></key><time symbol=\"cut\"><beats>2</beats><beat-type>4</beat-type>"));
    }

    @Test
    public void importsMuseScoreGraceAndAcciaccaturaWithoutConsumingMeasureTime() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><grace/><durationType>eighth</durationType><Note><pitch>60</pitch>"
                + "</Note></Chord><Chord><acciaccatura/><durationType>eighth</durationType><Note><pitch>62</pitch>"
                + "</Note></Chord><Chord><durationType>quarter</durationType><Note><pitch>64</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<grace/><pitch><step>C</step>"));
        assertEquals(true, musicXml.contains("<grace slash=\"yes\"/><pitch><step>D</step>"));
        assertEquals(false, musicXml.contains("<grace/><pitch><step>C</step><octave>4</octave></pitch><duration>"));
        assertEquals(true, musicXml.contains("<pitch><step>E</step><octave>4</octave></pitch><duration>480</duration>"));
    }

    @Test
    public void importsMuseScoreNoteTieAndEndSpannerMarkers() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch><Tie/>"
                + "</Note></Chord><Chord><durationType>quarter</durationType><Note><pitch>60</pitch>"
                + "<endSpanner/></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<tie type=\"start\"/><duration>480</duration>"));
        assertEquals(true, musicXml.contains("<tie type=\"stop\"/><duration>480</duration>"));
        assertEquals(true, musicXml.contains("<notations><tied type=\"start\"/></notations>"));
        assertEquals(true, musicXml.contains("<notations><tied type=\"stop\"/></notations>"));
    }

    @Test
    public void importsMuseScoreChordArticulationAndTechnicalSubtypes() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Articulation>"
                + "<subtype>articStaccatoBelow</subtype></Articulation><Note><pitch>60</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Articulation><subtype>articTenutoAbove</subtype>"
                + "</Articulation><Articulation><subtype>lhPizzicato</subtype></Articulation>"
                + "<Note><pitch>62</pitch><Fingering>1</Fingering><String>3</String></Note></Chord></voice>"
                + "</Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<notations><articulations><staccato/></articulations></notations>"));
        assertEquals(true, musicXml.contains("<articulations><tenuto/></articulations><technical><stopped/>"));
        assertEquals(true, musicXml.contains("<technical><stopped/><fingering>1</fingering><string>3</string>"
                + "</technical>"));
    }

    @Test
    public void importsMuseScoreChordSlurTransitionsAcrossMeasures() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Slur type=\"start\" id=\"4\"/>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure>"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Slur type=\"stop\" id=\"4\"/>"
                + "<Note><pitch>62</pitch></Note></Chord></voice></Measure>"
                + "</Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<slur type=\"start\" number=\"4\"/>"));
        assertEquals(true, musicXml.contains("<slur type=\"stop\" number=\"4\"/>"));
    }

    @Test
    public void importsMuseScoreChordLocalTrillOrnament() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Ornament>"
                + "<subtype>ornamentTrill</subtype></Ornament><Note><pitch>60</pitch><Accidental>"
                + "<subtype>accidentalFlat</subtype></Accidental></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<accidental>flat</accidental>"));
        assertEquals(true, musicXml.contains("<notations><ornaments><trill-mark/><accidental-mark>flat</accidental-mark>"
                + "</ornaments></notations>"));
    }

    @Test
    public void routesMuseScoreEventTrackAndMoveIntoMusicXmlVoiceAndStaff() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Part><Staff id=\"1\"/>"
                + "<Staff id=\"2\"/></Part><Staff id=\"1\"><Measure><voice>"
                + "<Chord><track>1</track><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "<Chord><move>1</move><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "<Dynamic><track>2</track><move>1</move><subtype>mf</subtype></Dynamic>"
                + "</voice></Measure></Staff><Staff id=\"2\"><Measure><voice><Rest><durationType>whole</durationType>"
                + "</Rest></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>2</voice><type>quarter</type><staff>1</staff>"));
        assertEquals(true, musicXml.contains("<pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>3</voice><type>quarter</type><staff>2</staff>"));
        assertEquals(true, musicXml.contains("<dynamics><mf/></dynamics></direction-type>"));
        assertEquals(true, musicXml.contains("<staff>2</staff><voice>4</voice></direction>"));
    }

    @Test
    public void assignsPartWideVoiceNumbersForMultipleMuseScoreVoiceLanes() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note>"
                + "</Chord></voice><voice><Chord><durationType>quarter</durationType><Note><pitch>64</pitch>"
                + "</Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type><staff>1</staff>"));
        assertEquals(true, musicXml.contains("<backup><duration>1920</duration></backup>"));
        assertEquals(true, musicXml.contains("<pitch><step>E</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>2</voice><type>quarter</type><staff>1</staff>"));
    }

    @Test
    public void roundTripsUnpitchedMusicXmlNotesAsTimedMuseScoreChordEvents() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>Drums</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type>"
                + "</time></attributes><note><unpitched><display-step>C</display-step><display-octave>5</display-octave>"
                + "</unpitched><duration>480</duration><voice>1</voice><type>quarter</type></note><note>"
                + "<unpitched><display-step>D</display-step><display-octave>5</display-octave></unpitched>"
                + "<duration>480</duration><voice>1</voice><type>quarter</type></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(2, roundTripped.split("<duration>480</duration><voice>1</voice><type>quarter</type>", -1).length
                - 1);
        assertEquals(false, roundTripped.contains("<note><rest/><duration>480</duration>"));
    }

    @Test
    public void roundTripsMusicXmlOctaveShiftAsMuseScoreOttavaSpanner() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type>"
                + "</time></attributes><direction><direction-type><octave-shift type=\"down\" size=\"8\""
                + " number=\"1\"/></direction-type></direction><note><pitch><step>A</step><octave>6</octave>"
                + "</pitch><duration>960</duration><voice>1</voice><type>half</type></note></measure>"
                + "<measure number=\"2\"><direction><direction-type><octave-shift type=\"stop\" size=\"8\""
                + " number=\"1\"/></direction-type></direction><note><pitch><step>A</step><octave>5</octave>"
                + "</pitch><duration>960</duration><voice>1</voice><type>half</type></note></measure>"
                + "</part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, mscx.contains("<Spanner type=\"Ottava\"><Ottava><subtype>8vb</subtype></Ottava>"));
        assertEquals(true, mscx.contains("<Spanner type=\"Ottava\"><prev>"));
        assertEquals(true, roundTripped.contains("<octave-shift type=\"start\" size=\"8\" number=\"1\"/>"));
        assertEquals(true, roundTripped.contains("<octave-shift type=\"stop\" size=\"8\" number=\"1\"/>"));
    }

    @Test
    public void roundTripsMusicXmlTrillWavyLineAsMuseScoreSpanner() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type>"
                + "</time></attributes><note><pitch><step>A</step><octave>3</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>half</type><notations><ornaments><trill-mark/><wavy-line type=\"start\""
                + " number=\"1\"/></ornaments></notations></note></measure><measure number=\"2\"><note>"
                + "<pitch><step>A</step><octave>3</octave></pitch><duration>960</duration><voice>1</voice>"
                + "<type>half</type><notations><ornaments><wavy-line type=\"stop\" number=\"1\"/>"
                + "</ornaments></notations></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, mscx.contains("<Spanner type=\"Trill\"><Trill><subtype>trill</subtype></Trill>"));
        assertEquals(true, mscx.contains("<Spanner type=\"Trill\"><prev>"));
        assertEquals(true, roundTripped.contains("<trill-mark/><wavy-line type=\"start\" number=\"1\"/>"));
        assertEquals(true, roundTripped.contains("<wavy-line type=\"stop\" number=\"1\"/>"));
    }

    @Test
    public void roundTripsMusicXmlTrillMarkOnlyAsMuseScoreChordOrnament() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions></attributes><note><pitch><step>C</step><octave>4</octave>"
                + "</pitch><duration>480</duration><voice>1</voice><type>quarter</type><notations><ornaments>"
                + "<trill-mark/></ornaments></notations></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, mscx.contains("<Ornament><subtype>ornamentTrill</subtype></Ornament>"));
        assertEquals(false, mscx.contains("<Spanner type=\"Trill\"><Trill><subtype>trill</subtype></Trill><next>"));
        assertEquals(true, roundTripped.contains("<trill-mark/>"));
        assertEquals(false, roundTripped.contains("<wavy-line"));
    }

    @Test
    public void exportsMusicXmlArticulationsAsMuseScoreSubtypes() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><staccato/></articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><accent/></articulations></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><tenuto/></articulations></notations></note>"
                + "</measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(true, mscx.contains("<Articulation><subtype>articStaccatoAbove</subtype></Articulation>"));
        assertEquals(true, mscx.contains("<Articulation><subtype>articAccentAbove</subtype></Articulation>"));
        assertEquals(true, mscx.contains("<Articulation><subtype>articTenutoAbove</subtype></Articulation>"));
    }

    @Test
    public void exportsMusicXmlTechnicalNotationAsMuseScoreArticulationAndNoteValues() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions></attributes><note><pitch><step>C</step><octave>4</octave>"
                + "</pitch><duration>480</duration><voice>1</voice><type>quarter</type><notations><technical>"
                + "<stopped/><up-bow/><open-string/><harmonic/><fingering>2</fingering><string>4</string>"
                + "</technical></notations></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(true, mscx.contains("<Articulation><subtype>articLhPizzicatoAbove</subtype></Articulation>"));
        assertEquals(true, mscx.contains("<Articulation><subtype>articUpBowAbove</subtype></Articulation>"));
        assertEquals(true, mscx.contains("<Articulation><subtype>articOpenStringAbove</subtype></Articulation>"));
        assertEquals(true, mscx.contains("<Articulation><subtype>articHarmonicAbove</subtype></Articulation>"));
        assertEquals(true, mscx.contains("<Fingering>2</Fingering><String>4</String>"));
    }

    @Test
    public void exportsMultiStaffPartScaffoldAndInstrumentShortNameToMuseScore() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>Piano</part-name><part-abbreviation>Pno.</part-abbreviation></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions><staves>2</staves>"
                + "<clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign>"
                + "<line>4</line></clef></attributes><note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>"
                + "<backup><duration>480</duration></backup><note><pitch><step>C</step><octave>3</octave></pitch>"
                + "<duration>480</duration><voice>2</voice><type>quarter</type><staff>2</staff></note>"
                + "</measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(true, mscx.contains("<Part id=\"1\"><Staff><defaultClef>G</defaultClef></Staff>"
                + "<Staff><defaultClef>F</defaultClef></Staff><trackName>Piano</trackName>"));
        assertEquals(true, mscx.contains("<Instrument><trackName>Piano</trackName><longName>Piano</longName>"
                + "<shortName>Pno.</shortName><clef>G</clef><clef staff=\"2\">F</clef></Instrument>"));
        assertEquals(true, mscx.contains("<Staff id=\"1\">"));
        assertEquals(true, mscx.contains("<Staff id=\"2\">"));
    }

    @Test
    public void exportsMusicXmlTieAndSlurAsMuseScoreMarkers() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions></attributes><note><pitch><step>C</step><octave>4</octave>"
                + "</pitch><duration>480</duration><voice>1</voice><type>quarter</type><tie type=\"start\"/>"
                + "<notations><tied type=\"start\"/><slur type=\"start\" number=\"3\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><tie type=\"stop\"/><notations><tied type=\"stop\"/>"
                + "<slur type=\"stop\" number=\"3\"/></notations></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(true, mscx.contains("<Spanner type=\"Slur\"><Slur/><next><location><fractions>1/4"));
        assertEquals(true, mscx.contains("<Spanner type=\"Slur\"><prev><location><fractions>-1/4"));
        assertEquals(true, mscx.contains("<Tie/>"));
        assertEquals(true, mscx.contains("<endSpanner/>"));
    }

    @Test
    public void roundTripsMusicXmlMiddleEndStartRepeatThroughMuseScoreVoiceBarline() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>2</beats><beat-type>4</beat-type>"
                + "</time></attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note><barline location=\"middle\"><repeat direction=\"backward\"/>"
                + "<repeat direction=\"forward\"/></barline><note><pitch><step>D</step><octave>4</octave>"
                + "</pitch><duration>480</duration><voice>1</voice><type>quarter</type></note></measure></part>"
                + "</score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, mscx.contains("<BarLine><subtype>end-start-repeat</subtype></BarLine>"));
        assertEquals(true, roundTripped.contains("<barline location=\"middle\"><bar-style>light-heavy</bar-style>"
                + "<repeat direction=\"backward\"/><repeat direction=\"forward\"/></barline>"));
    }

    @Test
    public void emitsDetailedDiagnosticsForDroppedMuseScoreEvents() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Rest/><Chord><durationType>quarter</durationType></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, true, false, false);

        assertEquals(true, musicXml.contains("<miscellaneous-field name=\"mks:diag:count\">2"));
        assertEquals(true, musicXml.contains("level=warn;code=MUSESCORE_IMPORT_WARNING;fmt=mscx;"));
        assertEquals(true, musicXml.contains("measure=1;staff=1;voice=1;atDiv=0;action=dropped;"
                + "reason=unknown-duration;tag=Rest"));
        assertEquals(true, musicXml.contains("measure=1;staff=1;voice=1;atDiv=0;action=dropped;"
                + "reason=missing-pitch;tag=Chord"));
    }

    @Test
    public void emitsOneSortedDiagnosticForUnsupportedMuseScoreElements() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><LayoutBreak/><Zeta/><Alpha/></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, true, false, false);

        assertEquals(true, musicXml.contains("<miscellaneous-field name=\"mks:diag:count\">1"));
        assertEquals(true, musicXml.contains("unsupported MuseScore elements skipped: alpha, zeta"));
        assertEquals(true, musicXml.contains("action=skipped;reason=unsupported-elements"));
    }

    @Test
    public void clampsOverfullMuseScoreVoiceTailAndEmitsItsDiagnostic() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><TimeSig><sigN>2</sigN><sigD>4</sigD></TimeSig><voice>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>64</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, true, false, false);

        assertEquals(2, musicXml.split("<pitch>", -1).length - 1);
        assertEquals(false, musicXml.contains("<step>E</step>"));
        assertEquals(true, musicXml.contains("action=clamped;reason=overfull;occupiedDiv=1440;capacityDiv=960"));
    }

    @Test
    public void placesMuseScoreDirectionsInTheirSourceVoiceLane() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note>"
                + "</Chord></voice><voice><Rest><durationType>quarter</durationType></Rest>"
                + "<Dynamic><subtype>mf</subtype></Dynamic><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>64</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<dynamics><mf/></dynamics></direction-type><staff>1</staff>"
                + "<voice>2</voice></direction>"));
    }

    @Test
    public void emitsDiagnosticForUnsupportedMuseScoreTuplet() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tuplet><actualNotes>x</actualNotes><normalNotes>2</normalNotes></Tuplet>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, true, false, false);

        assertEquals(true, musicXml.contains("message=measure 1: unsupported tuplet skipped.;measure=1;staff=1;"
                + "voice=1;atDiv=0;action=skipped;reason=unsupported;tag=Tuplet"));
    }

    @Test
    public void emitsDiagnosticsForUnsupportedMuseScoreDirectionLikeEvents() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Dynamic><subtype>unknown</subtype></Dynamic><Expression/>"
                + "<Marker/><Jump/><Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note>"
                + "</Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, true, false, false);

        assertEquals(true, musicXml.contains("<miscellaneous-field name=\"mks:diag:count\">4"));
        for (String tag : Arrays.asList("Dynamic", "Expression", "Marker", "Jump")) {
            assertEquals(true, musicXml.contains("action=skipped;reason=unsupported;tag=" + tag));
        }
    }

    @Test
    public void importsMuseScoreKeyAccidentalsAndTpcPitchSpelling() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><TimeSig><sigN>5</sigN><sigD>4</sigD></TimeSig><KeySig><accidental>4</accidental></KeySig><voice>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>63</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>70</pitch><tpc>12</tpc></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>70</pitch><tpc>24</tpc></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(2, musicXml.split("<accidental>natural</accidental>", -1).length - 1);
        assertEquals(true, musicXml.contains("<pitch><step>B</step><alter>-1</alter><octave>4</octave></pitch>"
                + "<accidental>flat</accidental>"));
        assertEquals(true, musicXml.contains("<pitch><step>A</step><alter>1</alter><octave>4</octave></pitch>"
                + "<accidental>sharp</accidental>"));
    }

    @Test
    public void importsMuseScoreMeasureDurationRestAtTheMeasureCapacity() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><TimeSig><sigN>3</sigN><sigD>4</sigD></TimeSig><voice><Rest>"
                + "<durationType>measure</durationType></Rest></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<note><rest/><duration>1440</duration><voice>1</voice><type>half</type>"));
    }

    @Test
    public void importsMuseScoreTupletReferencesWithWrittenTypeAndActualDuration() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tuplet id=\"T1\"><normalNotes>2</normalNotes><actualNotes>3</actualNotes>"
                + "</Tuplet><Chord><durationType>quarter</durationType><Tuplet>T1</Tuplet><Note><pitch>60</pitch>"
                + "</Note></Chord><Chord><durationType>quarter</durationType><Tuplet>T1</Tuplet><Note><pitch>62</pitch>"
                + "</Note></Chord><Chord><durationType>quarter</durationType><Tuplet>T1</Tuplet><Note><pitch>64</pitch>"
                + "</Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(3, musicXml.split("<duration>320</duration><voice>1</voice><type>quarter</type>"
                + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes>"
                + "</time-modification>", -1).length - 1);
        assertEquals(true, musicXml.contains("<notations><tuplet type=\"start\" number=\"1\"/></notations>"));
        assertEquals(true, musicXml.contains("<notations><tuplet type=\"stop\" number=\"1\"/></notations>"));
    }

    @Test
    public void importsMuseScoreStandaloneAndChordLocalOttavaSpannersWithDisplayPitchShift() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Spanner type=\"Ottava\"><Ottava><subtype>8va</subtype></Ottava><next/>"
                + "</Spanner><Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure><Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>62</pitch>"
                + "</Note></Chord><Chord><durationType>quarter</durationType><Spanner type=\"Ottava\"><prev/>"
                + "</Spanner><Note><pitch>64</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<octave-shift type=\"start\" size=\"8\" number=\"1\"/>"
                + "</direction-type><staff>1</staff></direction>"));
        assertEquals(true, musicXml.contains("<octave-shift type=\"stop\" size=\"8\" number=\"1\"/>"
                + "</direction-type><staff>1</staff></direction>"));
        assertEquals(true, musicXml.contains("<pitch><step>C</step><octave>5</octave></pitch>"));
        assertEquals(true, musicXml.contains("<pitch><step>D</step><octave>5</octave></pitch>"));
        assertEquals(true, musicXml.contains("<pitch><step>E</step><octave>4</octave></pitch>"));
    }

    @Test
    public void importsMuseScoreStandaloneAndChordLocalTrillSpannersWithSharedNumbers() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Spanner type=\"Trill\"><Trill><subtype>trill</subtype></Trill><next/>"
                + "</Spanner><Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure><Measure><voice><Chord><durationType>quarter</durationType><Spanner type=\"Trill\">"
                + "<prev/></Spanner><Note><pitch>62</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<ornaments><trill-mark/><wavy-line type=\"start\" number=\"1\"/>"
                + "</ornaments>"));
        assertEquals(true, musicXml.contains("<ornaments><wavy-line type=\"stop\" number=\"1\"/></ornaments>"));
    }

    @Test
    public void importsMuseScoreAbsoluteTicksAsMeasureRelativeVoiceForwards() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tick>480</Tick><Chord><durationType>quarter</durationType><Note><pitch>60</pitch>"
                + "</Note></Chord></voice></Measure><Measure><voice><Tick>2400</Tick><Chord>"
                + "<durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord></voice></Measure>"
                + "</Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(2, musicXml.split("<forward><duration>480</duration><voice>1</voice><staff>1</staff></forward>", -1)
                .length - 1);
        assertEquals(true, musicXml.contains("<pitch><step>C</step><octave>4</octave></pitch>"));
        assertEquals(true, musicXml.contains("<pitch><step>D</step><octave>4</octave></pitch>"));
    }

    @Test
    public void importsMuseScoreMidMeasureRepeatBarlineAtItsTickPosition() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tick>480</Tick><BarLine><subtype>end-start-repeat</subtype></BarLine>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<forward><duration>480</duration><voice>1</voice><staff>1</staff>"
                + "</forward><barline location=\"middle\"><bar-style>light-heavy</bar-style>"
                + "<repeat direction=\"backward\"/><repeat direction=\"forward\"/></barline>"));
    }

    @Test
    public void importsMuseScoreInlineTupletStartScaleAndEndNotation() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tuplet><normalNotes>2</normalNotes><actualNotes>3</actualNotes>"
                + "<numberType>1</numberType><bracketType>1</bracketType></Tuplet>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>64</pitch></Note></Chord>"
                + "<endTuplet/></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(3, musicXml.split("<duration>320</duration><voice>1</voice><type>quarter</type>"
                + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes>"
                + "</time-modification>", -1).length - 1);
        assertEquals(true, musicXml.contains("<tuplet type=\"start\" number=\"1\" bracket=\"yes\""
                + " show-number=\"actual\"/>"));
        assertEquals(true, musicXml.contains("<tuplet type=\"stop\" number=\"1\"/>"));
    }

    @Test
    public void importsMuseScoreInlineTupletRestWithTimeModificationAndStop() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tuplet><normalNotes>2</normalNotes><actualNotes>3</actualNotes></Tuplet>"
                + "<Rest><durationType>quarter</durationType></Rest><Rest><durationType>quarter</durationType></Rest>"
                + "<Rest><durationType>quarter</durationType></Rest><endTuplet/></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(3, musicXml.split("<rest/><duration>320</duration><voice>1</voice><type>quarter</type>"
                + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes>"
                + "</time-modification>", -1).length - 1);
        assertEquals(true, musicXml.contains("<tuplet type=\"start\" number=\"1\" bracket=\"yes\"/>"));
        assertEquals(true, musicXml.contains("<tuplet type=\"stop\" number=\"1\"/>"));
    }

    @Test
    public void importsNestedMuseScoreInlineTupletsWithStackedDurationAndStops() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tuplet><normalNotes>2</normalNotes><actualNotes>3</actualNotes></Tuplet>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "<Tuplet><normalNotes>4</normalNotes><actualNotes>5</actualNotes></Tuplet>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>62</pitch></Note></Chord><endTuplet/>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>64</pitch></Note></Chord><endTuplet/>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<duration>256</duration><voice>1</voice><type>quarter</type>"
                + "<time-modification><actual-notes>5</actual-notes><normal-notes>4</normal-notes>"
                + "</time-modification>"));
        assertEquals(true, musicXml.contains("<tuplet type=\"start\" number=\"1\" bracket=\"yes\"/>"));
        assertEquals(true, musicXml.contains("<tuplet type=\"start\" number=\"2\" bracket=\"yes\"/>"));
        assertEquals(true, musicXml.contains("<tuplet type=\"stop\" number=\"2\"/>"));
        assertEquals(true, musicXml.contains("<tuplet type=\"stop\" number=\"1\"/>"));
    }

    @Test
    public void importsLowercaseMuseScoreTupletDefinitionAndReference() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><tuplet id=\"T1\"><normalNotes>2</normalNotes><actualNotes>3</actualNotes>"
                + "</tuplet><Chord><durationType>quarter</durationType><tuplet>T1</tuplet><Note><pitch>60</pitch>"
                + "</Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<duration>320</duration><voice>1</voice><type>quarter</type>"
                + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes>"
                + "</time-modification>"));
    }

    @Test
    public void importsMuseScoreChordSlurSpannerAcrossMeasures() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Spanner type=\"Slur\">"
                + "<Slur/><next/></Spanner><Note><pitch>60</pitch></Note></Chord></voice></Measure>"
                + "<Measure><voice><Chord><durationType>quarter</durationType><spanner type=\"Slur\"><prev/>"
                + "</spanner><Note><pitch>62</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, musicXml.contains("<slur type=\"start\" number=\"1\"/>"));
        assertEquals(true, musicXml.contains("<slur type=\"stop\" number=\"1\"/>"));
    }

    @Test
    public void routesPublicMuseScoreExportCutTimeOption() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time symbol=\"cut\"><beats>4</beats>"
                + "<beat-type>4</beat-type></time></attributes><note><pitch><step>C</step><octave>4</octave>"
                + "</pitch><duration>1920</duration><voice>1</voice><type>whole</type></note>"
                + "</measure></part></score-partwise>";
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml), true);
        assertEquals(true, mscx.contains("<TimeSig><subtype>2</subtype><sigN>2</sigN><sigD>2</sigD></TimeSig>"));
    }

    @Test
    public void routesMusicXmlDirectionsThroughPublicMuseScoreExport() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type>"
                + "</time></attributes><direction><direction-type><dynamics><mf/></dynamics></direction-type>"
                + "<sound dynamics=\"90\"/></direction><direction><direction-type><words font-style=\"italic\">"
                + "dolce</words></direction-type></direction><note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));
        assertEquals(true, mscx.contains("<Dynamic><subtype>mf</subtype><velocity>81</velocity></Dynamic>"));
        assertEquals(true, mscx.contains("<Expression><text><i></i>dolce</text></Expression>"));
    }

    @Test
    public void routesMuseScoreDynamicThroughPublicMusicXmlImport() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tempo><tempo>2.0</tempo></Tempo><Dynamic><subtype>mf</subtype>"
                + "<velocity>90</velocity></Dynamic><Expression><text><i></i>dolce</text></Expression>"
                + "<Marker><subtype>segno</subtype><label>segno</label></Marker><Jump><text>D.S.</text>"
                + "<jumpTo>segno</jumpTo></Jump>"
                + "<Dynamic><subtype>f</subtype><visible>0</visible></Dynamic>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";
        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false);
        assertEquals(true, musicXml.contains("<direction><direction-type><dynamics><mf/></dynamics></direction-type>"
                + "<sound dynamics=\"100.00\"/><staff>1</staff><voice>1</voice></direction>"));
        assertEquals(true, musicXml.contains("<metronome><beat-unit>quarter</beat-unit><per-minute>120</per-minute>"));
        assertEquals(true, musicXml.contains("<words font-style=\"italic\">dolce</words>"));
        assertEquals(true, musicXml.contains("<direction-type><segno/></direction-type>"));
        assertEquals(true, musicXml.contains("<words>D.S.</words></direction-type><sound dalsegno=\"segno\"/>"));
        assertEquals(false, musicXml.contains("<dynamics><f/></dynamics>"));
    }

    @Test
    public void importsPinnedMsczSampleTwoWithPartNamesAndInstrumentClefs() throws Exception {
        String mscx = MxlIo.extractTextFromZipByExtensions(readTestResource("upstream-zip/sample2.mscz"),
                new String[] { ".mscx" });

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(4, musicXml.split("<part id=\\\"P", -1).length - 1);
        assertEquals(true, musicXml.contains("<part-name>Violin 1</part-name>"));
        assertEquals(true, musicXml.contains("<part-name>Violin 2</part-name>"));
        assertEquals(true, musicXml.contains("<part-name>Viola</part-name>"));
        assertEquals(true, musicXml.contains("<part-name>Violoncello</part-name>"));
        assertEquals(true, musicXml.contains("<clef><sign>C</sign><line>3</line></clef>"));
        assertEquals(true, musicXml.contains("<clef><sign>F</sign><line>4</line></clef>"));
    }

    @Test
    public void roundTripsPinnedMsczSampleFourMidMeasureEndStartRepeats() throws Exception {
        String source = MxlIo.extractTextFromZipByExtensions(readTestResource("upstream-zip/sample4.mscz"),
                new String[] { ".mscx" });
        String xml = MuseScoreIo.convertMuseScoreToMusicXml(source, false, false, false, true);
        String roundTripped = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(xml));
        assertEquals(true, countOccurrences(roundTripped, "<subtype>end-start-repeat</subtype>") >= 3);
    }

    @Test
    public void importsPinnedMsczSampleFourStringQuartetClefs() throws Exception {
        String source = MxlIo.extractTextFromZipByExtensions(readTestResource("upstream-zip/sample4.mscz"),
                new String[] { ".mscx" });
        String xml = MuseScoreIo.convertMuseScoreToMusicXml(source, false, false, false, true);
        assertEquals(true, xml.contains("<clef><sign>C</sign><line>3</line></clef>"));
        assertEquals(true, xml.contains("<clef><sign>F</sign><line>4</line></clef>"));
        assertEquals(true, countOccurrences(xml, "<clef><sign>G</sign><line>2</line></clef>") >= 2);
    }

    @Test
    public void exportsPinnedMxlSampleTwoWithViolaAndCelloClefDefaults() throws Exception {
        String musicXml = MxlIo.extractMusicXmlTextFromMxl(readTestResource("upstream-zip/sample2.mxl"));

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(true, mscx.contains("<trackName>Viola</trackName>"));
        assertEquals(true, mscx.contains("<trackName>Violoncello</trackName>"));
        assertEquals(true, mscx.contains("<defaultClef>C3</defaultClef>"));
        assertEquals(true, mscx.contains("<defaultClef>F</defaultClef>"));
        assertEquals(true, mscx.contains("<Instrument><trackName>Viola</trackName><longName>Viola</longName>"
                + "<shortName>Vla.</shortName><clef>C3</clef></Instrument>"));
        assertEquals(true, mscx.contains("<Instrument><trackName>Violoncello</trackName><longName>Violoncello</longName>"
                + "<shortName>Vc.</shortName><clef>F</clef></Instrument>"));
    }

    @Test
    public void exportsPinnedMxlSampleTwoViolinOneSlurStopWithItsStartSpan() throws Exception {
        String musicXml = MxlIo.extractMusicXmlTextFromMxl(readTestResource("upstream-zip/sample2.mxl"));
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        int dynamicPosition = mscx.indexOf("<subtype>mf</subtype>");
        assertEquals(true, dynamicPosition >= 0);
        int measureEnd = mscx.indexOf("</Measure>", dynamicPosition);
        assertEquals(true, measureEnd > dynamicPosition);
        String measureExcerpt = mscx.substring(dynamicPosition, measureEnd);

        assertEquals(true, measureExcerpt.contains("<Chord><durationType>quarter</durationType><Spanner type=\"Slur\">"
                + "<Slur/><next><location><fractions>1/4</fractions></location></next></Spanner>"
                + "<Note><pitch>65</pitch></Note></Chord>"));
        assertEquals(true, measureExcerpt.contains("<Chord><durationType>32nd</durationType><Spanner type=\"Slur\">"
                + "<prev><location><fractions>-1/4</fractions></location></prev></Spanner>"
                + "<Note><pitch>67</pitch></Note></Chord>"));
        assertEquals(false, measureExcerpt.contains("<Chord><durationType>32nd</durationType><Spanner type=\"Slur\">"
                + "<prev><location><fractions>-1/32</fractions></location></prev></Spanner>"
                + "<Note><pitch>67</pitch></Note></Chord>"));
    }

    @Test
    public void exportsBasicMusicXmlContentIntoMuseScoreLikePinnedUpstreamCase() {
        String musicXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>P1</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes>"
                + "<divisions>480</divisions><key><fifths>1</fifths><mode>major</mode></key>"
                + "<time><beats>3</beats><beat-type>4</beat-type></time></attributes>"
                + "<direction><direction-type><dynamics><mf/></dynamics></direction-type><sound tempo=\"120\"/>"
                + "</direction><note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>quarter</type></note><note><rest/><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type></note><barline location=\"right\"><repeat direction=\"backward\"/>"
                + "</barline></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(true, mscx.contains("<museScore"));
        assertEquals(true, mscx.contains("<Staff id=\"1\">"));
        assertEquals(true, mscx.contains("<TimeSig><sigN>3</sigN><sigD>4</sigD></TimeSig>"));
        assertEquals(true, mscx.contains("<KeySig><accidental>1</accidental><concertKey>1</concertKey></KeySig>"));
        assertEquals(true, mscx.contains("<Tempo><tempo>2.000000</tempo></Tempo>"));
        assertEquals(true, mscx.contains("<Dynamic><subtype>mf</subtype></Dynamic>"));
        assertEquals(true, mscx.contains("<endRepeat/>"));
    }

    @Test
    public void importsBasicMuseScoreChordAndRestContentLikePinnedUpstreamCase() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division>"
                + "<metaTag name=\"workTitle\">MS Test</metaTag><Staff id=\"1\"><Measure><voice>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "<Rest><durationType>quarter</durationType></Rest></voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, true, true, false, true);
        Document document = MusicXmlIo.parseMusicXmlDocument(musicXml);

        assertEquals("MS Test", directChildText(directChild(document.getDocumentElement(), "work"), "work-title"));
        assertEquals(true, musicXml.contains("<score-part id=\"P1\">"));
        assertEquals(true, musicXml.contains("<pitch><step>C</step><octave>4</octave></pitch>"));
        assertEquals(true, musicXml.contains("<miscellaneous-field name=\"mks:src:musescore:raw-encoding\">"));
    }

    @Test
    public void importsMuseScoreTempoSignaturesRepeatsAndDynamicsLikePinnedUpstreamCase() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure startRepeat=\"1\"><TimeSig><sigN>3</sigN><sigD>4</sigD></TimeSig>"
                + "<KeySig><accidental>-1</accidental><mode>minor</mode></KeySig><voice>"
                + "<Tempo><tempo>2.0</tempo></Tempo><Dynamic><subtype>mf</subtype><velocity>90</velocity></Dynamic>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord></voice></Measure>"
                + "<Measure endRepeat=\"1\"><TimeSig><sigN>4</sigN><sigD>4</sigD></TimeSig><voice>"
                + "<Dynamic><subtype>p</subtype><velocity>49</velocity></Dynamic>"
                + "<Rest><durationType>quarter</durationType></Rest></voice></Measure>"
                + "</Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, true, true, false, true);

        assertEquals(true, musicXml.contains("<time><beats>3</beats><beat-type>4</beat-type></time>"));
        assertEquals(true, musicXml.contains("<key><fifths>-1</fifths><mode>minor</mode></key>"));
        assertEquals(true, musicXml.contains("<sound tempo=\"120\"/>"));
        assertEquals(true, musicXml.contains("<barline location=\"left\"><repeat direction=\"forward\"/></barline>"));
        assertEquals(true, musicXml.contains("<measure number=\"2\"><attributes><key><fifths>-1</fifths>"
                + "<mode>minor</mode></key><time><beats>4</beats><beat-type>4</beat-type></time>"));
        assertEquals(true, musicXml.contains("<barline location=\"right\">"));
        assertEquals(true, musicXml.contains("<repeat direction=\"backward\"/>"));
        assertEquals(true, musicXml.contains("<dynamics><mf/></dynamics>"));
        assertEquals(true, musicXml.contains("<dynamics><p/></dynamics>"));
        assertEquals(true, musicXml.contains("<sound dynamics=\"100.00\"/>"));
        assertEquals(true, musicXml.contains("<sound dynamics=\"54.44\"/>"));
        assertEquals(true, musicXml.contains("name=\"mks:src:musescore:version\">4.0</miscellaneous-field>"));
    }

    @Test
    public void importsVisibleAndHiddenMuseScoreTempoTextLikePinnedUpstreamCases() {
        String visible = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<VBox><Text><text>Tema</text></Text></VBox><Measure><voice>"
                + "<Tempo><tempo>2.1666667</tempo><text>Quasi Presto</text></Tempo>"
                + "<Tempo><tempo>2.1666667</tempo><text>Tema</text></Tempo>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";
        String hidden = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Tempo><tempo>1.0</tempo><followText>1</followText><visible>0</visible>"
                + "<text><sym>metNoteQuarterUp</sym><sym>noSym</sym><sym>metAugmentationDot</sym> = 60</text>"
                + "</Tempo><Rest><durationType>quarter</durationType></Rest></voice></Measure></Staff></Score></museScore>";

        String visibleXml = MuseScoreIo.convertMuseScoreToMusicXml(visible, false, false, false, true);
        String hiddenXml = MuseScoreIo.convertMuseScoreToMusicXml(hidden, false, false, false, true);

        assertEquals(1, countOccurrences(visibleXml, "<words>Quasi Presto</words>"));
        assertEquals(1, countOccurrences(visibleXml, "<words>Tema</words>"));
        assertEquals(true, visibleXml.contains("<sound tempo=\"130\"/>"));
        assertEquals(false, hiddenXml.contains("<words"));
        assertEquals(true, hiddenXml.contains("<sound tempo=\"60\"/>"));
    }

    @Test
    public void roundTripsMusicXmlTupletMarkersLikePinnedUpstreamCase() {
        String noteStart = "<note><pitch><step>C</step><octave>4</octave></pitch><duration>320</duration>"
                + "<voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes>"
                + "<normal-notes>2</normal-notes></time-modification><notations><tuplet type=\"start\" number=\"1\"/>"
                + "</notations></note>";
        String noteMiddle = "<note><pitch><step>D</step><octave>4</octave></pitch><duration>320</duration>"
                + "<voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes>"
                + "<normal-notes>2</normal-notes></time-modification></note>";
        String noteStop = "<note><pitch><step>E</step><octave>4</octave></pitch><duration>320</duration>"
                + "<voice>1</voice><type>eighth</type><time-modification><actual-notes>3</actual-notes>"
                + "<normal-notes>2</normal-notes></time-modification><notations><tuplet type=\"stop\" number=\"1\"/>"
                + "</notations></note>";
        String source = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>P1</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>" + noteStart + noteMiddle + noteStop
                + "<note><rest/><duration>960</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, true);

        assertEquals(true, mscx.contains("<Tuplet id=\"T1\"><normalNotes>2</normalNotes><actualNotes>3</actualNotes></Tuplet>"));
        assertEquals(true, roundTripped.contains("<tuplet type=\"start\" number=\"1\"/>"));
        assertEquals(true, roundTripped.contains("<tuplet type=\"stop\" number=\"1\"/>"));
    }

    @Test
    public void retainsCutTimeSubtypeWithoutFollowingExplicitTimeChangeLikePinnedUpstreamCase() {
        String source = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>P1</part-name>"
                + "</score-part></part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time symbol=\"cut\"><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration><voice>1</voice><type>whole</type></note>"
                + "</measure><measure number=\"2\"><note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>whole</type></note></measure></part></score-partwise>";
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        assertEquals(1, countOccurrences(mscx, "<TimeSig>"));
        assertEquals(1, countOccurrences(mscx,
                "<TimeSig><subtype>2</subtype><sigN>4</sigN><sigD>4</sigD></TimeSig>"));
    }

    @Test
    public void importsMuseScoreConcertAndTransposeKeysLikePinnedUpstreamCases() {
        String concertKeyMscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><KeySig><concertKey>-1</concertKey><mode>minor</mode></KeySig>"
                + "<TimeSig><sigN>3</sigN><sigD>4</sigD></TimeSig><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>60</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";
        String transposeKeyMscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Part>"
                + "<trackName>Clarinet in A</trackName><Instrument><transposeDiatonic>-2</transposeDiatonic>"
                + "<transposeChromatic>-3</transposeChromatic></Instrument><Staff id=\"1\"/></Part><Staff id=\"1\">"
                + "<Measure><voice><KeySig><concertKey>3</concertKey><transposeKey>0</transposeKey><mode>major</mode>"
                + "</KeySig><TimeSig><sigN>3</sigN><sigD>4</sigD></TimeSig><Chord><durationType>quarter</durationType>"
                + "<Note><pitch>72</pitch></Note></Chord></voice></Measure></Staff></Score></museScore>";

        String concertKeyXml = MuseScoreIo.convertMuseScoreToMusicXml(concertKeyMscx, false, false, false, true);
        String transposeKeyXml = MuseScoreIo.convertMuseScoreToMusicXml(transposeKeyMscx, false, false, false, true);

        assertEquals(true, concertKeyXml.contains("<key><fifths>-1</fifths><mode>minor</mode></key>"));
        assertEquals(true, transposeKeyXml.contains("<key><fifths>0</fifths><mode>major</mode></key>"));
        assertEquals(true, transposeKeyXml.contains("<transpose><diatonic>-2</diatonic><chromatic>-3</chromatic>"
                + "</transpose>"));
    }

    @Test
    public void importsMuseScoreMeasureCThreeClefLikePinnedUpstreamCase() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Clef><concertClefType>C3</concertClefType></Clef>"
                + "<Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, true);

        assertEquals(true, musicXml.contains("<clef><sign>C</sign><line>3</line></clef>"));
    }

    @Test
    public void exportsMusicXmlAltoClefAsMuseScoreDefaultClefCThreeLikePinnedUpstreamCase() {
        String musicXml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>Viola</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><clef><sign>C</sign><line>3</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(false, mscx.contains("<Clef><concertClefType>C3</concertClefType></Clef>"));
        assertEquals(true, mscx.contains("<defaultClef>C3</defaultClef>"));
    }

    @Test
    public void addsFinalLightHeavyBarlineWithoutExplicitMuseScoreEndBarlineLikePinnedUpstreamCase() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note></Chord>"
                + "</voice></Measure></Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, true);

        assertEquals(true, musicXml.contains("<barline location=\"right\"><bar-style>light-heavy</bar-style>"
                + "</barline>"));
    }

    @Test
    public void exportsMusicXmlHeaderMetadataIntoMuseScoreMetaTagsLikePinnedUpstreamCase() {
        String musicXml = "<score-partwise version=\"4.0\"><work><work-title>String Quartet No.15</work-title>"
                + "<work-number>K.421</work-number></work><movement-title>Andante</movement-title>"
                + "<movement-number>1</movement-number><identification>"
                + "<creator type=\"composer\">Wolfgang Amadeus Mozart</creator>"
                + "<creator type=\"arranger\">Arranger Name</creator><creator type=\"lyricist\">Lyricist Name</creator>"
                + "<creator type=\"translator\">Translator Name</creator><rights>Public Domain</rights>"
                + "<encoding><encoding-date>2026-03-02</encoding-date></encoding></identification>"
                + "<credit page=\"1\"><credit-type>subtitle</credit-type><credit-words>K.421 Mvt 1</credit-words>"
                + "</credit><part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes><note><pitch><step>C</step>"
                + "<octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(musicXml));

        assertEquals(true, mscx.contains("<metaTag name=\"workTitle\">String Quartet No.15</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"workNumber\">K.421</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"movementTitle\">Andante</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"movementNumber\">1</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"subtitle\">K.421 Mvt 1</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"composer\">Wolfgang Amadeus Mozart</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"arranger\">Arranger Name</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"lyricist\">Lyricist Name</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"translator\">Translator Name</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"copyright\">Public Domain</metaTag>"));
        assertEquals(true, mscx.contains("<metaTag name=\"creationDate\">2026-03-02</metaTag>"));
    }

    @Test
    public void roundTripsPinnedPublicSampleSixFirstTwoMeasuresThroughMuseScoreFacade() throws Exception {
        String source = new String(readTestResource("abc-roundtrip/roundtrip_sample6_m1_m2.musicxml"),
                java.nio.charset.StandardCharsets.UTF_8);
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, true, false, false);

        assertEquals(1, roundTripped.split("<part id=", -1).length - 1);
        assertEquals(2, roundTripped.split("<measure number=", -1).length - 1);
        assertEquals(source.split("<note>", -1).length - 1, roundTripped.split("<note>", -1).length - 1);
        assertEquals(true, roundTripped.contains("<key><fifths>0</fifths><mode>major</mode></key>"));
        assertEquals(true, roundTripped.contains("<pitch><step>B</step><octave>4</octave></pitch>"
                + "<duration>4</duration><voice>1</voice><type>quarter</type><staff>1</staff>"));
        assertEquals(true, roundTripped.contains("<chord/><pitch><step>B</step><octave>5</octave></pitch>"
                + "<duration>4</duration><voice>1</voice><type>quarter</type><staff>1</staff>"));
        assertEquals(true, roundTripped.contains("<pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>4</duration><voice>1</voice><type>quarter</type><staff>1</staff>"));
        assertEquals(true, roundTripped.contains("<pitch><step>E</step><octave>3</octave></pitch>"
                + "<duration>4</duration><voice>6</voice><type>quarter</type><staff>6</staff>"));
        assertEquals(collectMeasurePitchEvents(source, 1, 2), collectMeasurePitchEvents(roundTripped, 1, 2));
        assertEquals(false, roundTripped.contains("action=clamped;reason=overfull"));
    }

    /** Mirrors the tracked Node articulation/dynamics MuseScore round-trip spot check. */
    @Test
    public void roundTripsMusicXmlArticulationsAndDynamicsThroughMuseScoreFacade() {
        String source = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "</attributes><direction><direction-type><dynamics><mf/></dynamics></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><staccato/></articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><accent/></articulations></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type><notations><articulations><tenuto/></articulations></notations></note>"
                + "<note><rest/><duration>480</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, roundTripped.contains("<dynamics><mf/></dynamics>"));
        assertEquals(true, roundTripped.contains("<staccato/>"));
        assertEquals(true, roundTripped.contains("<accent/>"));
        assertEquals(true, roundTripped.contains("<tenuto/>"));
        assertEquals(collectMeasurePitchEvents(source, 1, 1), collectMeasurePitchEvents(roundTripped, 1, 1));
        assertEquals(1, countOccurrences(roundTripped, "<rest/>"));
    }

    /**
     * Redistributable, behavior-equivalent evidence for the local-only
     * Mozart/Paganini/Moonlight spot fixtures in the pinned Node repository.
     */
    @Test
    public void roundTripsCompactLocalControlParityFixtureThroughMuseScoreFacade() throws Exception {
        String source = new String(readTestResource("upstream-local-equivalent/compact-control-parity.musicxml"),
                java.nio.charset.StandardCharsets.UTF_8);
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, mscx.contains("<Articulation><subtype>articLhPizzicatoAbove</subtype></Articulation>"));
        assertEquals(true, mscx.contains("<Dynamic><subtype>mf</subtype>"));
        assertEquals(true, mscx.contains("<Spanner type=\"Trill\">"));
        assertEquals(true, mscx.contains("<Spanner type=\"Ottava\">"));
        assertEquals(true, mscx.contains("<Marker><subtype>segno</subtype><label>segno</label></Marker>"));
        assertEquals(true, mscx.contains("<Marker><subtype>coda</subtype><label>coda</label></Marker>"));
        assertEquals(true, mscx.contains("<Marker><subtype>fine</subtype><label>Fine</label></Marker>"));
        assertEquals(true, mscx.contains("<Jump><text>D.S.</text><jumpTo>segno</jumpTo>"));
        assertEquals(true, mscx.contains("<Expression><text>Tema</text></Expression>"));
        assertEquals(true, mscx.contains("<Expression><text><i></i>sempre legato</text></Expression>"));

        assertEquals(true, roundTripped.contains("<measure number=\"0\" implicit=\"yes\">"));
        assertEquals(true, roundTripped.contains("<trill-mark/><wavy-line type=\"start\" number=\"1\"/>"));
        assertEquals(true, roundTripped.contains("<wavy-line type=\"stop\" number=\"1\"/>"));
        assertEquals(true, roundTripped.contains("<octave-shift type=\"start\" size=\"8\" number=\"1\"/>"));
        assertEquals(true, roundTripped.contains("<octave-shift type=\"stop\" size=\"8\" number=\"1\"/>"));
        assertEquals(true, roundTripped.contains("<sound dalsegno=\"segno\" fine=\"fine\" tocoda=\"coda\"/>"));

        // MuseScore serializes non-consecutive MusicXML labels as sequential
        // measure indices and applies an Ottava display shift. Assert the
        // upstream-observable event cardinality and pitch spelling separately
        // from those intentional representation details.
        List<String> sourceEvents = collectMeasurePitchEvents(source, 0, 153);
        List<String> roundTrippedEvents = collectMeasurePitchEvents(roundTripped, 0, 153);
        assertEquals(4, sourceEvents.size());
        assertEquals(sourceEvents.size(), roundTrippedEvents.size());
        assertEquals(true, containsPitchEvent(roundTrippedEvents, "1", "C", "4", ""));
        assertEquals(true, containsPitchEvent(roundTrippedEvents, "1", "C", "4", "sharp"));
    }

    /** Compact fixed MSCX input for the local Paganini/Moonlight import-parity path. */
    @Test
    public void importsCompactLocalEquivalentMscxWithExactPitchEventParity() {
        String mscx = "<museScore version=\"4.0\"><Score><Division>480</Division><Staff id=\"1\">"
                + "<Measure><voice><Chord><durationType>quarter</durationType><Note><pitch>60</pitch></Note>"
                + "</Chord><Chord><durationType>quarter</durationType><Note><pitch>61</pitch><Accidental>"
                + "<subtype>accidentalSharp</subtype></Accidental></Note></Chord></voice></Measure>"
                + "</Staff></Score></museScore>";

        String musicXml = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(Arrays.asList("1|0|480|1|C||4|", "1|480|480|1|C|1|4|sharp"),
                collectMeasurePitchEvents(musicXml, 1, 1));
    }

    @Test
    public void roundTripsPinnedPublicSampleSixAndSevenCorpusThroughMuseScoreFacade() throws Exception {
        assertMuseScoreRoundtripCorpusStats("upstream-musescore-roundtrip/sample6.musicxml", 4, 4, 137);
        assertMuseScoreRoundtripCorpusStats("upstream-musescore-roundtrip/sample7.musicxml", 2, 4, 201);
    }

    @Test
    public void roundTripsPinnedPublicSampleSevenPitchSpellingAndStaffFourNaturalAccidental() throws Exception {
        String source = new String(readTestResource("upstream-musescore-roundtrip/sample7.musicxml"),
                java.nio.charset.StandardCharsets.UTF_8);
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(collectMeasurePitchEvents(source, 3, 4), collectMeasurePitchEvents(roundTripped, 3, 4));
        assertEquals(true, containsPitchEvent(collectMeasurePitchEvents(source, 7, 7), "4", "B", "3", "natural"));
        assertEquals(true,
                containsPitchEvent(collectMeasurePitchEvents(roundTripped, 7, 7), "4", "B", "3", "natural"));
    }

    @Test
    public void roundTripsMusicXmlTransposeThroughMuseScoreInstrument() {
        String source = "<score-partwise version=\"3.1\"><part-list><score-part id=\"P1\">"
                + "<part-name>Clarinet in A</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><key><fifths>0</fifths><mode>major</mode></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef>"
                + "<transpose><diatonic>-2</diatonic><chromatic>-3</chromatic></transpose></attributes>"
                + "<note><pitch><step>C</step><octave>5</octave></pitch><duration>480</duration><voice>1</voice>"
                + "<type>quarter</type></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, false, false, false);

        assertEquals(true, mscx.contains("<transposeDiatonic>-2</transposeDiatonic>"));
        assertEquals(true, mscx.contains("<transposeChromatic>-3</transposeChromatic>"));
        assertEquals(true, mscx.contains("<KeySig><accidental>0</accidental><concertKey>3</concertKey><transposeKey>0</transposeKey>"));
        assertEquals(true, roundTripped.contains("<transpose><diatonic>-2</diatonic><chromatic>-3</chromatic></transpose>"));
    }

    @Test
    public void exportsZeroDurationForwardAndRoundedVoiceNumbersLikeUpstream() {
        String source = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\">"
                + "<part-name>P1</part-name></score-part></part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>4</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                + "<forward><duration>0</duration></forward>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice>"
                + "<type>quarter</type></note><backup><duration>4</duration></backup>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>4</duration><voice>2.4</voice>"
                + "<type>quarter</type></note></measure></part></score-partwise>";

        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));

        assertEquals(2, countOccurrences(mscx, "<voice>"));
        assertEquals(true, mscx.indexOf("<Chord>") >= 0 && mscx.indexOf("<Chord>") < mscx.indexOf("<Rest>"));
    }

    private static void assertMuseScoreRoundtripCorpusStats(String resourceName, int beats, int beatType, int tempo)
            throws Exception {
        String source = new String(readTestResource(resourceName), java.nio.charset.StandardCharsets.UTF_8);
        String mscx = MuseScoreIo.exportMusicXmlDomToMuseScore(MusicXmlIo.parseMusicXmlDocument(source));
        String roundTripped = MuseScoreIo.convertMuseScoreToMusicXml(mscx, false, true, false, false);

        assertEquals(countOccurrences(source, "<part id="), countOccurrences(roundTripped, "<part id="), resourceName);
        assertEquals(countOccurrences(source, "<measure number="), countOccurrences(roundTripped, "<measure number="),
                resourceName);
        assertEquals(countOccurrences(source, "<note>"), countOccurrences(roundTripped, "<note>"), resourceName);
        assertEquals(countOccurrences(source, "<rest"), countOccurrences(roundTripped, "<rest"), resourceName);
        assertEquals(firstMuseScoreRoundtripMeasureStats(source, 5),
                firstMuseScoreRoundtripMeasureStats(roundTripped, 5), resourceName);
        assertEquals(true, roundTripped.contains("<time><beats>" + beats + "</beats><beat-type>" + beatType
                + "</beat-type></time>"), resourceName);
        assertEquals(true, roundTripped.contains("<sound tempo=\"" + tempo + "\"/>"), resourceName);
        assertEquals(false, roundTripped.contains("action=clamped;reason=overfull"), resourceName);
    }

    private static int countOccurrences(String text, String token) {
        if (text == null || token == null || token.length() == 0) {
            return 0;
        }
        int count = 0;
        int start = 0;
        while (true) {
            int found = text.indexOf(token, start);
            if (found < 0) {
                return count;
            }
            count++;
            start = found + token.length();
        }
    }

    private static List<String> firstMuseScoreRoundtripMeasureStats(String musicXml, int limit) {
        List<String> out = new ArrayList<String>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("<measure\\s+number=\\\"([^\\\"]*)\\\"[^>]*>(.*?)</measure>", java.util.regex.Pattern.DOTALL)
                .matcher(musicXml == null ? "" : musicXml);
        while (matcher.find() && out.size() < limit) {
            String measureBody = matcher.group(2);
            out.add(matcher.group(1) + "|" + countOccurrences(measureBody, "<note") + "|"
                    + countOccurrences(measureBody, "<rest") + "|" + countOccurrences(measureBody, "<chord")
                    + "|" + countOccurrences(measureBody, "<tie type=\"start\"") + "|"
                    + countOccurrences(measureBody, "<tie type=\"stop\""));
        }
        return out;
    }

    /** Mirrors the pinned Node collectMeasurePitchEvents test helper. */
    private static List<String> collectMeasurePitchEvents(String musicXml, int fromMeasure, int toMeasure) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(musicXml);
        List<String> out = new ArrayList<String>();
        if (doc == null || doc.getDocumentElement() == null) {
            return out;
        }
        for (Element part : directChildren(doc.getDocumentElement(), "part")) {
            for (Element measure : directChildren(part, "measure")) {
                Integer measureNo = integerOrNull(measure.getAttribute("number"));
                if (measureNo == null || measureNo.intValue() < fromMeasure || measureNo.intValue() > toMeasure) {
                    continue;
                }
                int cursor = 0;
                for (Element child : directElementChildren(measure)) {
                    String tagName = child.getTagName();
                    if ("backup".equals(tagName) || "forward".equals(tagName)) {
                        Integer duration = roundedPositiveNumberOrNull(directChildText(child, "duration"));
                        if (duration != null) {
                            cursor = "backup".equals(tagName) ? Math.max(0, cursor - duration.intValue())
                                    : cursor + duration.intValue();
                        }
                        continue;
                    }
                    if (!"note".equals(tagName)) {
                        continue;
                    }
                    Integer duration = roundedPositiveNumberOrNull(directChildText(child, "duration"));
                    int roundedDuration = duration == null ? 0 : duration.intValue();
                    boolean chord = directChild(child, "chord") != null;
                    if (directChild(child, "rest") != null) {
                        if (!chord && roundedDuration > 0) {
                            cursor += roundedDuration;
                        }
                        continue;
                    }
                    Element pitch = directChild(child, "pitch");
                    String step = directChildText(pitch, "step");
                    String octave = directChildText(pitch, "octave");
                    if (step == null || octave == null) {
                        continue;
                    }
                    int onset = chord ? Math.max(0, cursor - roundedDuration) : cursor;
                    String staff = directChildText(child, "staff");
                    String alter = directChildText(pitch, "alter");
                    String accidental = directChildText(child, "accidental");
                    out.add(measureNo + "|" + onset + "|" + roundedDuration + "|"
                            + (staff == null ? "1" : staff) + "|" + step + "|"
                            + (alter == null ? "" : alter) + "|" + octave + "|"
                            + (accidental == null ? "" : accidental));
                    if (!chord) {
                        cursor += roundedDuration;
                    }
                }
            }
        }
        return out;
    }

    private static Integer integerOrNull(String raw) {
        try {
            return Integer.valueOf(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean containsPitchEvent(List<String> events, String staff, String step, String octave,
            String accidental) {
        String suffix = "|" + staff + "|" + step + "|";
        String expected = "|" + octave + "|" + accidental;
        for (String event : events) {
            if (event.contains(suffix) && event.endsWith(expected)) {
                return true;
            }
        }
        return false;
    }

    private static Integer roundedPositiveNumberOrNull(String raw) {
        try {
            double value = Double.parseDouble(raw == null ? "" : raw.trim());
            if (!Double.isFinite(value) || value <= 0) {
                return null;
            }
            return Integer.valueOf((int) Math.round(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> out = new ArrayList<Element>();
        for (Element child : directElementChildren(parent)) {
            if (tagName.equals(child.getTagName())) {
                out.add(child);
            }
        }
        return out;
    }

    private static List<Element> directElementChildren(Element parent) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                out.add((Element) child);
            }
            child = child.getNextSibling();
        }
        return out;
    }

    private static Element directChild(Element parent, String tagName) {
        for (Element child : directElementChildren(parent)) {
            if (tagName.equals(child.getTagName())) {
                return child;
            }
        }
        return null;
    }

    private static String directChildText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        if (child == null || child.getTextContent() == null) {
            return null;
        }
        String text = child.getTextContent().trim();
        return text.length() == 0 ? null : text;
    }

    private static byte[] readTestResource(String resourceName) throws IOException {
        InputStream stream = MuseScoreIoTest.class.getClassLoader().getResourceAsStream(resourceName);
        if (stream == null) {
            throw new IOException("Missing test resource: " + resourceName);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            stream.close();
        }
    }
}
