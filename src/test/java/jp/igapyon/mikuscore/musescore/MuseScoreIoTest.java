package jp.igapyon.mikuscore.musescore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
        assertEquals("mikuscore export", defaultTitle.getTitle());
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
}
