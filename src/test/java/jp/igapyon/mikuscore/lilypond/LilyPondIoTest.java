/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.lilypond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public class LilyPondIoTest {
    @Test
    public void escapesXmlForLilyPondImportExportHelpers() {
        assertEquals("&amp;&lt;&gt;&quot;", LilyPondIo.xmlEscape("&<>\""));
        assertEquals("", LilyPondIo.xmlEscape(null));
    }

    @Test
    public void reducesFractionsLikeUpstreamHelper() {
        LilyPondIo.Fraction fraction = LilyPondIo.reduceFraction(6, -8);

        assertEquals(-3, fraction.getNum());
        assertEquals(4, fraction.getDen());
        assertEquals(1, LilyPondIo.gcd(0, 0));
    }

    @Test
    public void convertsLilyDurationToAbcLengthToken() {
        assertEquals("", LilyPondIo.lilyDurationToAbcLen(8, 0));
        assertEquals("2", LilyPondIo.lilyDurationToAbcLen(4, 0));
        assertEquals("/", LilyPondIo.lilyDurationToAbcLen(16, 0));
        assertEquals("3", LilyPondIo.lilyDurationToAbcLen(4, 1));
        assertEquals("7/2", LilyPondIo.lilyDurationToAbcLen(4, 2));
    }

    @Test
    public void convertsAbcLengthTokenToLilyDuration() {
        assertLilyDuration(8, 0, LilyPondIo.abcLenToLilyDuration(""));
        assertLilyDuration(4, 0, LilyPondIo.abcLenToLilyDuration("2"));
        assertLilyDuration(16, 0, LilyPondIo.abcLenToLilyDuration("/"));
        assertLilyDuration(16, 0, LilyPondIo.abcLenToLilyDuration("/2"));
        assertLilyDuration(8, 0, LilyPondIo.abcLenToLilyDuration("3"));
        assertLilyDuration(8, 0, LilyPondIo.abcLenToLilyDuration("7/2"));
    }

    @Test
    public void convertsPitchHelpers() {
        assertEquals("c'", LilyPondIo.lilyPitchFromStepAlterOctave("C", 0, 4));
        assertEquals("fis''", LilyPondIo.lilyPitchFromStepAlterOctave("F", 1, 5));
        assertEquals("bes,", LilyPondIo.lilyPitchFromStepAlterOctave("B", -1, 2));
        assertEquals("C,", LilyPondIo.abcPitchFromStepOctave("C", 3));
        assertEquals("c'", LilyPondIo.abcPitchFromStepOctave("C", 6));
        assertEquals(Integer.valueOf(60), LilyPondIo.pitchToMidiKey("C", 0, 4));
        assertEquals(Integer.valueOf(66), LilyPondIo.pitchToMidiKey("F", 1, 4));
        assertNull(LilyPondIo.pitchToMidiKey("H", 0, 4));
    }

    @Test
    public void mapsLilyAndMusicXmlDurationNames() {
        assertEquals("whole", LilyPondIo.lilyDurationToMusicXmlType(1));
        assertEquals("16th", LilyPondIo.lilyDurationToMusicXmlType(16));
        assertEquals("quarter", LilyPondIo.lilyDurationToMusicXmlType(3));
        assertEquals("1", LilyPondIo.noteTypeToLilyDuration("whole"));
        assertEquals("64", LilyPondIo.noteTypeToLilyDuration("64th"));
        assertEquals("4", LilyPondIo.noteTypeToLilyDuration("bad"));
    }

    @Test
    public void convertsBasicLilyPondSourceIntoMusicXml() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\header {\n"
                + "  title = \"Lily import test\"\n"
                + "}\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 d'4 e'4 f'4 | g'4 a'4 b'4 c''4 }\n"
                + "}";

        String xml = LilyPondIo.convertLilyPondToMusicXml(lily);
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);

        assertEquals("score-partwise", doc.getDocumentElement().getTagName());
        assertEquals(8, doc.getElementsByTagName("note").getLength());
        assertEquals(true, xml.contains("<work-title>Lily import test</work-title>"));
    }

    @Test
    public void importsHeaderComposerIntoMusicXmlIdentification() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\header {\n"
                + "  title = \"Composer import test\"\n"
                + "  composer = \"J S Bach\"\n"
                + "}\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 d'4 e'4 f'4 }\n"
                + "}";

        String xml = LilyPondIo.convertLilyPondToMusicXml(lily);
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);

        assertEquals("J S Bach", doc.getElementsByTagName("creator").item(0).getTextContent().trim());
        assertEquals("composer", ((org.w3c.dom.Element) doc.getElementsByTagName("creator").item(0))
                .getAttribute("type"));
    }

    @Test
    public void importsBareTopLevelMusicBlockWithoutScoreOrStaff() {
        String lily = "\\version \"2.24.4\"\n"
                + "{\n"
                + "  c' e' g' e'\n"
                + "}";

        String xml = LilyPondIo.convertLilyPondToMusicXml(lily);

        assertEquals(true, xml.contains("<step>C</step>"));
        assertEquals(true, xml.contains("<step>E</step>"));
        assertEquals(true, xml.contains("<step>G</step>"));
    }

    @Test
    public void importsAbsoluteOctaveMarkedNoteAsMiddleC() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("octave").item(0).getTextContent().trim());
    }

    @Test
    public void importsAbsoluteUnmarkedNotesAtBaseOctave() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c4 d4 e4 f4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("3", doc.getElementsByTagName("octave").item(0).getTextContent().trim());
        assertEquals("3", doc.getElementsByTagName("octave").item(1).getTextContent().trim());
        assertEquals("3", doc.getElementsByTagName("octave").item(2).getTextContent().trim());
        assertEquals("3", doc.getElementsByTagName("octave").item(3).getTextContent().trim());
    }

    @Test
    public void importsBasicChordTokens() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { <c' e' g'>4 r4 <d' f' a'>2 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(4, doc.getElementsByTagName("chord").getLength());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("E", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("G", doc.getElementsByTagName("step").item(2).getTextContent().trim());
    }

    @Test
    public void importsBasicRelativeNotation() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\relative c' { c4 d e f | g a b c } }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(8, doc.getElementsByTagName("note").getLength());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("E", doc.getElementsByTagName("step").item(2).getTextContent().trim());
        assertEquals("F", doc.getElementsByTagName("step").item(3).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("octave").item(0).getTextContent().trim());
    }

    @Test
    public void honorsExplicitOctaveMarksInsideRelativeNotation() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\relative c' { d'4 d4 } }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("D", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("5", doc.getElementsByTagName("octave").item(0).getTextContent().trim());
        assertEquals("5", doc.getElementsByTagName("octave").item(1).getTextContent().trim());
    }

    @Test
    public void resolvesRelativeOctaveByLetterNameDistance() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\relative c' { f4 bis4 } }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("B", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("1", doc.getElementsByTagName("alter").item(0).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("octave").item(1).getTextContent().trim());
    }

    @Test
    public void usesFirstChordToneAsPostChordRelativeAnchor() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\relative c' { <c e g>4 b4 } }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("B", doc.getElementsByTagName("step").item(3).getTextContent().trim());
        assertEquals("3", doc.getElementsByTagName("octave").item(3).getTextContent().trim());
    }

    @Test
    public void importsNativeTieMarkerAsMusicXmlTie() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'2~ c'2 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("start", ((org.w3c.dom.Element) doc.getElementsByTagName("tie").item(0)).getAttribute("type"));
        assertEquals("stop", ((org.w3c.dom.Element) doc.getElementsByTagName("tie").item(1)).getAttribute("type"));
        assertEquals("start", ((org.w3c.dom.Element) doc.getElementsByTagName("tied").item(0)).getAttribute("type"));
        assertEquals("stop", ((org.w3c.dom.Element) doc.getElementsByTagName("tied").item(1)).getAttribute("type"));
    }

    @Test
    public void importsIntegerDurationMultiplier() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 3/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { r4*3 | c'4 d'4 e'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("2880", doc.getElementsByTagName("duration").item(0).getTextContent().trim());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
    }

    @Test
    public void addsImplicitBeamsForShortNoteGroups() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 2/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'8 d'8 e'8 f'8 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("begin", doc.getElementsByTagName("beam").item(0).getTextContent().trim());
        assertEquals("end", doc.getElementsByTagName("beam").item(1).getTextContent().trim());
        assertEquals("begin", doc.getElementsByTagName("beam").item(2).getTextContent().trim());
        assertEquals("end", doc.getElementsByTagName("beam").item(3).getTextContent().trim());
    }

    @Test
    public void importsStaffClefFromLilyPond() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"Bass\" { \\clef bass c,4 d,4 e,4 f,4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("F", doc.getElementsByTagName("sign").item(0).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("line").item(0).getTextContent().trim());
        assertEquals("Bass", doc.getElementsByTagName("part-name").item(0).getTextContent().trim());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
    }

    @Test
    public void importsSingleStaffWithInstrumentNameMetadata() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"Flute\" \\with { instrumentName = \"Fl.\" } { c'4 d'4 e'4 f'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("Fl.", doc.getElementsByTagName("part-name").item(0).getTextContent().trim());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("F", doc.getElementsByTagName("step").item(3).getTextContent().trim());
    }

    @Test
    public void importsSingleStaffBodyInstrumentNameMetadata() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\set Staff.instrumentName = \"Cello\" c,4 d,4 e,4 f,4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("Cello", doc.getElementsByTagName("part-name").item(0).getTextContent().trim());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
    }

    @Test
    public void importsStaffTranspositionIntoMusicXmlAttributes() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"ClarinetInA\" { \\transposition a c'4 d'4 e'4 f'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("-3", doc.getElementsByTagName("chromatic").item(0).getTextContent().trim());
        assertEquals("-2", doc.getElementsByTagName("diatonic").item(0).getTextContent().trim());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
    }

    @Test
    public void infersBassClefForLowRangeStaffWhenClefIsOmitted() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c,4 d,4 e,4 f,4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("F", doc.getElementsByTagName("sign").item(0).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("line").item(0).getTextContent().trim());
    }

    private static void assertLilyDuration(int duration, int dots, LilyPondIo.LilyDuration actual) {
        assertEquals(duration, actual.getDuration());
        assertEquals(dots, actual.getDots());
    }
}
