/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.lilypond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
    public void importsIsolatedDurationTokensAfterTieWithoutPitchLoss() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { a'2~ 4~ 16 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(3, doc.getElementsByTagName("note").getLength());
        assertEquals("A", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("A", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("A", doc.getElementsByTagName("step").item(2).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("octave").item(0).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("octave").item(1).getTextContent().trim());
        assertEquals("4", doc.getElementsByTagName("octave").item(2).getTextContent().trim());
        assertEquals("1920", doc.getElementsByTagName("duration").item(0).getTextContent().trim());
        assertEquals("960", doc.getElementsByTagName("duration").item(1).getTextContent().trim());
        assertEquals("240", doc.getElementsByTagName("duration").item(2).getTextContent().trim());
        assertEquals(4, doc.getElementsByTagName("tie").getLength());
        assertEquals(4, doc.getElementsByTagName("tied").getLength());
    }

    @Test
    public void importsNativeDynamicCommandsAsMusicXmlDirections() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\p c'4 \\mf d'4 \\sfz e'4 }\n"
                + "}";

        String xml = LilyPondIo.convertLilyPondToMusicXml(lily);
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);

        assertEquals(1, doc.getElementsByTagName("p").getLength());
        assertEquals(1, doc.getElementsByTagName("mf").getLength());
        assertEquals(1, doc.getElementsByTagName("sfz").getLength());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("E", doc.getElementsByTagName("step").item(2).getTextContent().trim());
    }

    @Test
    public void importsNativeWedgeCommandsAsMusicXmlDirections() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 \\< d'4 \\> e'4 \\! f'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("crescendo", ((org.w3c.dom.Element) doc.getElementsByTagName("wedge").item(0))
                .getAttribute("type"));
        assertEquals("diminuendo", ((org.w3c.dom.Element) doc.getElementsByTagName("wedge").item(1))
                .getAttribute("type"));
        assertEquals("stop", ((org.w3c.dom.Element) doc.getElementsByTagName("wedge").item(2))
                .getAttribute("type"));
        assertEquals("F", doc.getElementsByTagName("step").item(3).getTextContent().trim());
    }

    @Test
    public void importsNativeSlurMarkersAsMusicXmlSlurStartStop() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { ( c'4 d'4 ) e'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(2, doc.getElementsByTagName("slur").getLength());
        assertEquals("start", ((org.w3c.dom.Element) doc.getElementsByTagName("slur").item(0))
                .getAttribute("type"));
        assertEquals("stop", ((org.w3c.dom.Element) doc.getElementsByTagName("slur").item(1))
                .getAttribute("type"));
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
    }

    @Test
    public void importsNativeSlurCommandsAsMusicXmlSlurStartStop() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\( c'4 d'4 \\) e'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(2, doc.getElementsByTagName("slur").getLength());
        assertEquals("start", ((org.w3c.dom.Element) doc.getElementsByTagName("slur").item(0))
                .getAttribute("type"));
        assertEquals("stop", ((org.w3c.dom.Element) doc.getElementsByTagName("slur").item(1))
                .getAttribute("type"));
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
    }

    @Test
    public void importsNativeTrillCommandAsMusicXmlTrillMark() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 \\trill d'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(1, doc.getElementsByTagName("trill-mark").getLength());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("trill-mark", doc.getElementsByTagName("trill-mark").item(0).getNodeName());
    }

    @Test
    public void importsNativeTrillSpanCommandsAsMusicXmlWavyLineStartStop() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 \\startTrillSpan d'4 \\stopTrillSpan e'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(2, doc.getElementsByTagName("wavy-line").getLength());
        assertEquals("start", ((org.w3c.dom.Element) doc.getElementsByTagName("wavy-line").item(0))
                .getAttribute("type"));
        assertEquals("stop", ((org.w3c.dom.Element) doc.getElementsByTagName("wavy-line").item(1))
                .getAttribute("type"));
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("E", doc.getElementsByTagName("step").item(2).getTextContent().trim());
    }

    @Test
    public void importsNativeGlissandoAsMusicXmlStartStop() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 \\glissando d'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(2, doc.getElementsByTagName("glissando").getLength());
        assertEquals("start", ((org.w3c.dom.Element) doc.getElementsByTagName("glissando").item(0))
                .getAttribute("type"));
        assertEquals("stop", ((org.w3c.dom.Element) doc.getElementsByTagName("glissando").item(1))
                .getAttribute("type"));
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
    }

    @Test
    public void importsNativePedalCommandsAsMusicXmlDirections() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" {\n"
                + "    \\sustainOn c'4 \\sustainOff\n"
                + "    \\sostenutoOn d'4 \\sostenutoOff\n"
                + "    \\unaCorda e'4 \\treCorde\n"
                + "  }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(6, doc.getElementsByTagName("pedal").getLength());
        assertPedal(doc, 0, "start", "1");
        assertPedal(doc, 1, "stop", "1");
        assertPedal(doc, 2, "start", "2");
        assertPedal(doc, 3, "stop", "2");
        assertPedal(doc, 4, "start", "3");
        assertPedal(doc, 5, "stop", "3");
        assertEquals(true, LilyPondIoTest.musicXmlText(doc).contains("<words>sost. ped.</words>"));
        assertEquals(true, LilyPondIoTest.musicXmlText(doc).contains("<words>una corda</words>"));
        assertEquals(true, LilyPondIoTest.musicXmlText(doc).contains("<words>tre corde</words>"));
    }

    private static void assertPedal(Document doc, int index, String type, String number) {
        org.w3c.dom.Element pedal = (org.w3c.dom.Element) doc.getElementsByTagName("pedal").item(index);
        assertEquals(type, pedal.getAttribute("type"));
        assertEquals(number, pedal.getAttribute("number"));
    }

    private static String musicXmlText(Document doc) {
        return MusicXmlIo.serializeMusicXmlDocument(doc);
    }

    @Test
    public void importsNativeBowCommandsAsMusicXmlTechnicalNotations() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\upbow c'4 d'4 \\downbow }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(1, doc.getElementsByTagName("up-bow").getLength());
        assertEquals(1, doc.getElementsByTagName("down-bow").getLength());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
    }

    @Test
    public void importsNativeSnapPizzicatoAndHarmonicCommandsAsMusicXmlNotations() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\snappizzicato c'4 \\flageolet d'4 \\harmonic e'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(1, doc.getElementsByTagName("snap-pizzicato").getLength());
        assertEquals(2, doc.getElementsByTagName("harmonic").getLength());
        assertEquals("C", doc.getElementsByTagName("step").item(0).getTextContent().trim());
        assertEquals("D", doc.getElementsByTagName("step").item(1).getTextContent().trim());
        assertEquals("E", doc.getElementsByTagName("step").item(2).getTextContent().trim());
    }

    @Test
    public void keepsOmittedRootRelativePedalSampleInTrebleWithFullFirstMeasureNotes() {
        String lily = "\\relative {\n"
                + "  c''4\\sustainOn d e g\n"
                + "  <c, f a>1\\sustainOff\n"
                + "  c4\\sostenutoOn e g c,\n"
                + "  <bes d f>1\\sostenutoOff\n"
                + "  c4\\unaCorda d e g\n"
                + "  <d fis a>1\\treCorde\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element firstMeasure = measureAt(doc, 0);

        assertEquals("G", directText(directChild(directChild(directChild(firstMeasure, "attributes"), "clef"), "sign")));
        assertEquals(4, directPitchCount(firstMeasure));
        assertEquals("5", directText(directChild(directChild(directNoteAt(firstMeasure, 0), "pitch"), "octave")));
        assertEquals("flat", directText(directChild(directNoteAt(measureAt(doc, 3), 0), "accidental")));
        assertEquals("sharp", directText(directChild(directNoteAt(measureAt(doc, 5), 1), "accidental")));
    }

    @Test
    public void importsNativeRepeatVoltaIntoMusicXmlRepeatBarlines() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\repeat volta 2 { c'4 d'4 e'4 f'4 } }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element measure = measureAt(doc, 0);
        Element leftBarline = directChildWithAttribute(measure, "barline", "location", "left");
        Element rightBarline = directChildWithAttribute(measure, "barline", "location", "right");

        assertEquals("forward", directChild(leftBarline, "repeat").getAttribute("direction"));
        assertEquals("backward", directChild(rightBarline, "repeat").getAttribute("direction"));
        assertEquals("2", directChild(rightBarline, "ending").getAttribute("number"));
        assertEquals("stop", directChild(rightBarline, "ending").getAttribute("type"));
    }

    @Test
    public void importsAlternativeBlockWithMultipleEndings() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 2/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" {\n"
                + "    \\repeat volta 2 { c'4 d'4 }\n"
                + "    \\alternative {\n"
                + "      { e'4 }\n"
                + "      { f'4 }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        List<String> steps = new ArrayList<String>();
        for (int index = 0; index < doc.getElementsByTagName("step").getLength(); index++) {
            steps.add(doc.getElementsByTagName("step").item(index).getTextContent().trim());
        }

        assertEquals(true, steps.contains("E"));
        assertEquals(true, steps.contains("F"));
        assertEquals(true, hasEnding(doc, "1", "start"));
        assertEquals(true, hasEnding(doc, "1", "stop"));
        assertEquals(true, hasEnding(doc, "2", "start"));
        assertEquals(true, hasEnding(doc, "2", "stop"));
    }

    @Test
    public void importsNativeTupletRatioIntoMusicXmlTupletTimeModification() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 2/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { \\tuplet 3/2 { c'8 d'8 e'8 } }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element measure = measureAt(doc, 0);
        Element firstNote = directNoteAt(measure, 0);
        Element thirdNote = directNoteAt(measure, 2);

        assertEquals(3, directChildren(measure, "note").size());
        assertEquals("3", directText(directChild(directChild(firstNote, "time-modification"), "actual-notes")));
        assertEquals("2", directText(directChild(directChild(firstNote, "time-modification"), "normal-notes")));
        assertEquals("start", directChild(directChild(firstNote, "notations"), "tuplet").getAttribute("type"));
        assertEquals("stop", directChild(directChild(thirdNote, "notations"), "tuplet").getAttribute("type"));
    }

    @Test
    public void importsMultiPartStaffBlocksWithWithMetadata() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  <<\n"
                + "    \\new Staff = \"Flute\" \\with { instrumentName = \"Fl.\" } { c'4 d'4 e'4 f'4 }\n"
                + "    \\new Staff = \"Clarinet\" \\with { instrumentName = \"Cl.\" } { c4 d4 e4 f4 }\n"
                + "  >>\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(2, directChildren(doc.getDocumentElement(), "part").size());
        assertEquals("Fl.", doc.getElementsByTagName("part-name").item(0).getTextContent().trim());
        assertEquals("Cl.", doc.getElementsByTagName("part-name").item(1).getTextContent().trim());
    }

    @Test
    public void doesNotOvercountNotesForSimultaneousTwoStaffFragment() {
        String lily = "<<\n"
                + "  \\new Staff { \\clef \"treble\" \\key d \\major \\time 3/4 c''4 }\n"
                + "  \\new Staff { \\clef \"bass\" c4 }\n"
                + ">>";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element firstPartMeasure = measureAt(partAt(doc, 0), 0);
        Element secondPartMeasure = measureAt(partAt(doc, 1), 0);

        assertEquals(2, directChildren(doc.getDocumentElement(), "part").size());
        assertEquals(1, directChildren(firstPartMeasure, "note").size());
        assertEquals(1, directChildren(secondPartMeasure, "note").size());
        assertEquals("C", directText(directChild(directChild(directNoteAt(firstPartMeasure, 0), "pitch"), "step")));
        assertEquals("C", directText(directChild(directChild(directNoteAt(secondPartMeasure, 0), "pitch"), "step")));
    }

    @Test
    public void importsVariableBasedOrganScoreWithRelativeBlocks() {
        String lily = "\\header {\n"
                + "  title = \"Jesu, meine Freude\"\n"
                + "  composer = \"J S Bach\"\n"
                + "}\n"
                + "keyTime = { \\key c \\minor \\time 4/4 }\n"
                + "ManualOneVoiceOneMusic = \\relative {\n"
                + "  g'4 g f ees |\n"
                + "  d2 c |\n"
                + "}\n"
                + "ManualOneVoiceTwoMusic = \\relative {\n"
                + "  ees'16 d ees8~ 16 f ees d c8 d~ d c~ |\n"
                + "  8 c4 b8 c8. g16 c b c d |\n"
                + "}\n"
                + "ManualTwoMusic = \\relative {\n"
                + "  c'16 b c8~ 16 b c g a8 g~ 16 g aes ees |\n"
                + "  f16 ees f d g aes g f ees d ees8~ 16 f ees d |\n"
                + "}\n"
                + "PedalOrganMusic = \\relative {\n"
                + "  r8 c16 d ees d ees8~ 16 a, b g c b c8 |\n"
                + "  r16 g ees f g f g8 c,2 |\n"
                + "}\n"
                + "\\score {\n"
                + "  <<\n"
                + "    \\new PianoStaff <<\n"
                + "      \\new Staff = \"ManualOne\" <<\n"
                + "        \\keyTime\n"
                + "        \\clef \"treble\"\n"
                + "        \\new Voice { \\voiceOne \\ManualOneVoiceOneMusic }\n"
                + "        \\new Voice { \\voiceTwo \\ManualOneVoiceTwoMusic }\n"
                + "      >>\n"
                + "      \\new Staff = \"ManualTwo\" <<\n"
                + "        \\keyTime\n"
                + "        \\clef \"bass\"\n"
                + "        \\new Voice { \\ManualTwoMusic }\n"
                + "      >>\n"
                + "    >>\n"
                + "    \\new Staff = \"PedalOrgan\" <<\n"
                + "      \\keyTime\n"
                + "      \\clef \"bass\"\n"
                + "      \\new Voice { \\PedalOrganMusic }\n"
                + "    >>\n"
                + "  >>\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(true, directChildren(doc.getDocumentElement(), "part").size() >= 3);
        assertEquals(true, doc.getElementsByTagName("note").getLength() > 0);
        assertEquals("ManualOne", doc.getElementsByTagName("part-name").item(0).getTextContent().trim());
    }

    @Test
    public void importsBasicLyricsFromAddLyricsBlock() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 d'4 }\n"
                + "  \\addlyrics { la le }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("la", directText(directChild(directChild(directNoteAt(measureAt(doc, 0), 0), "lyric"), "text")));
        assertEquals("le", directText(directChild(directChild(directNoteAt(measureAt(doc, 0), 1), "lyric"), "text")));
    }

    @Test
    public void importsBasicLyricsFromStandaloneLyricModeBlock() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\lyricmode { do re }\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 d'4 }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("do", directText(directChild(directChild(directNoteAt(measureAt(doc, 0), 0), "lyric"), "text")));
        assertEquals("re", directText(directChild(directChild(directNoteAt(measureAt(doc, 0), 1), "lyric"), "text")));
    }

    @Test
    public void importsBasicLyricsFromLyricstoBlock() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { c'4 d'4 }\n"
                + "  \\lyricsto \"P1\" { mi fa }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals("mi", directText(directChild(directChild(directNoteAt(measureAt(doc, 0), 0), "lyric"), "text")));
        assertEquals("fa", directText(directChild(directChild(directNoteAt(measureAt(doc, 0), 1), "lyric"), "text")));
    }

    @Test
    public void appliesLyricstoTargetToMatchingStaffId() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 4/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  <<\n"
                + "    \\new Staff = \"P1\" { c'4 d'4 }\n"
                + "    \\new Staff = \"P2\" { e'4 f'4 }\n"
                + "  >>\n"
                + "  \\lyricsto \"P2\" { lo rem }\n"
                + "}";

        Document doc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element firstPartMeasure = measureAt(partAt(doc, 0), 0);
        Element secondPartMeasure = measureAt(partAt(doc, 1), 0);

        assertNull(directChild(directNoteAt(firstPartMeasure, 0), "lyric"));
        assertEquals("lo", directText(directChild(directChild(directNoteAt(secondPartMeasure, 0), "lyric"), "text")));
        assertEquals("rem", directText(directChild(directChild(directNoteAt(secondPartMeasure, 1), "lyric"), "text")));
    }

    @Test
    public void preservesPartNameAcrossMusicXmlToLilyPondToMusicXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list>\n"
                + "    <score-part id=\"P1\"><part-name>Violin</part-name></score-part>\n"
                + "  </part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes>\n"
                + "        <divisions>480</divisions>\n"
                + "        <key><fifths>0</fifths><mode>major</mode></key>\n"
                + "        <time><beats>4</beats><beat-type>4</beat-type></time>\n"
                + "        <clef><sign>G</sign><line>2</line></clef>\n"
                + "      </attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>";

        String lily = LilyPondIo.exportMusicXmlDomToLilyPond(MusicXmlIo.parseMusicXmlDocument(xml));
        Document roundtrip = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));

        assertEquals(true, lily.contains("instrumentName = \"Violin\""));
        assertEquals("Violin", roundtrip.getElementsByTagName("part-name").item(0).getTextContent().trim());
    }

    @Test
    public void roundtripsSameStaffMultiVoiceNoteViaMksLanesMetadata() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes>\n"
                + "        <divisions>480</divisions>\n"
                + "        <key><fifths>0</fifths><mode>major</mode></key>\n"
                + "        <time><beats>2</beats><beat-type>4</beat-type></time>\n"
                + "        <clef><sign>G</sign><line>2</line></clef>\n"
                + "      </attributes>\n"
                + "      <note>\n"
                + "        <pitch><step>E</step><octave>5</octave></pitch>\n"
                + "        <duration>960</duration><voice>1</voice><type>half</type>\n"
                + "      </note>\n"
                + "      <backup><duration>960</duration></backup>\n"
                + "      <note>\n"
                + "        <pitch><step>A</step><octave>4</octave></pitch>\n"
                + "        <duration>480</duration><voice>2</voice><type>quarter</type>\n"
                + "      </note>\n"
                + "      <note>\n"
                + "        <pitch><step>B</step><octave>4</octave></pitch>\n"
                + "        <duration>480</duration><voice>2</voice><type>quarter</type>\n"
                + "      </note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>";

        String lily = LilyPondIo.exportMusicXmlDomToLilyPond(MusicXmlIo.parseMusicXmlDocument(xml));
        Document outDoc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element measure = measureAt(outDoc, 0);

        assertEquals(true, lily.contains("%@mks lanes voice=P1 measure=1 data="));
        assertEquals(3, directChildren(measure, "note").size());
        assertEquals("960", directText(directChild(measure, "backup")));
        assertEquals("E", directText(directChild(directChild(directNoteAt(measure, 0), "pitch"), "step")));
        assertEquals("5", directText(directChild(directChild(directNoteAt(measure, 0), "pitch"), "octave")));
        assertEquals("960", directText(directChild(directNoteAt(measure, 0), "duration")));
    }

    @Test
    public void keepsFinal16thNoteInSevenToEightTupletAnd16thRunMeasureRoundtrip() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes>\n"
                + "        <divisions>480</divisions>\n"
                + "        <key><fifths>0</fifths><mode>major</mode></key>\n"
                + "        <time><beats>2</beats><beat-type>4</beat-type></time>\n"
                + "      </attributes>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>69</duration><voice>1</voice><type>32nd</type><time-modification><actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>69</duration><voice>1</voice><type>32nd</type><time-modification><actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>69</duration><voice>1</voice><type>32nd</type><time-modification><actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>A</step><octave>4</octave></pitch><duration>69</duration><voice>1</voice><type>32nd</type><time-modification><actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>D</step><octave>5</octave></pitch><duration>69</duration><voice>1</voice><type>32nd</type><time-modification><actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>F</step><octave>5</octave></pitch><duration>69</duration><voice>1</voice><type>32nd</type><time-modification><actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>A</step><octave>5</octave></pitch><duration>69</duration><voice>1</voice><type>32nd</type><time-modification><actual-notes>7</actual-notes><normal-notes>8</normal-notes></time-modification></note>\n"
                + "      <note><pitch><step>D</step><octave>6</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type></note>\n"
                + "      <note><pitch><step>F</step><octave>6</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type></note>\n"
                + "      <note><pitch><step>A</step><octave>6</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type></note>\n"
                + "      <note><pitch><step>D</step><octave>7</octave></pitch><duration>120</duration><voice>1</voice><type>16th</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>";

        String lily = LilyPondIo.exportMusicXmlDomToLilyPond(MusicXmlIo.parseMusicXmlDocument(xml));
        Document outDoc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element measure = measureAt(outDoc, 0);
        Element lastNote = directNoteAt(measure, 10);

        assertEquals(11, directChildren(measure, "note").size());
        assertEquals("D", directText(directChild(directChild(lastNote, "pitch"), "step")));
        assertEquals("7", directText(directChild(directChild(lastNote, "pitch"), "octave")));
    }

    @Test
    public void keepsTriplet16thVisualSemanticsTypeAndSlurOnRoundtrip() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes>\n"
                + "        <divisions>480</divisions>\n"
                + "        <key><fifths>0</fifths><mode>major</mode></key>\n"
                + "        <time><beats>2</beats><beat-type>4</beat-type></time>\n"
                + "      </attributes>\n"
                + "      <note>\n"
                + "        <pitch><step>E</step><octave>6</octave></pitch>\n"
                + "        <duration>80</duration><voice>1</voice><type>16th</type>\n"
                + "        <time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>\n"
                + "        <notations><tuplet type=\"start\"/><slur type=\"start\" number=\"1\" placement=\"above\"/></notations>\n"
                + "      </note>\n"
                + "      <note>\n"
                + "        <pitch><step>F</step><octave>6</octave></pitch>\n"
                + "        <duration>80</duration><voice>1</voice><type>16th</type>\n"
                + "        <time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>\n"
                + "      </note>\n"
                + "      <note>\n"
                + "        <pitch><step>E</step><octave>6</octave></pitch>\n"
                + "        <duration>80</duration><voice>1</voice><type>16th</type>\n"
                + "        <time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>\n"
                + "        <notations><tuplet type=\"stop\"/><slur type=\"stop\" number=\"1\"/></notations>\n"
                + "      </note>\n"
                + "      <note>\n"
                + "        <pitch><step>C</step><octave>6</octave></pitch>\n"
                + "        <duration>480</duration><voice>1</voice><type>quarter</type>\n"
                + "      </note>\n"
                + "      <note>\n"
                + "        <pitch><step>A</step><octave>5</octave></pitch>\n"
                + "        <duration>80</duration><voice>1</voice><type>16th</type>\n"
                + "        <time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>\n"
                + "        <notations><tuplet type=\"start\"/><slur type=\"start\" number=\"1\" placement=\"above\"/></notations>\n"
                + "      </note>\n"
                + "      <note>\n"
                + "        <pitch><step>B</step><octave>5</octave></pitch>\n"
                + "        <duration>80</duration><voice>1</voice><type>16th</type>\n"
                + "        <time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>\n"
                + "      </note>\n"
                + "      <note>\n"
                + "        <pitch><step>A</step><octave>5</octave></pitch>\n"
                + "        <duration>80</duration><voice>1</voice><type>16th</type>\n"
                + "        <time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>\n"
                + "        <notations><tuplet type=\"stop\"/><slur type=\"stop\" number=\"1\"/></notations>\n"
                + "      </note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>";

        String lily = LilyPondIo.exportMusicXmlDomToLilyPond(MusicXmlIo.parseMusicXmlDocument(xml));
        Document outDoc = MusicXmlIo.parseMusicXmlDocument(LilyPondIo.convertLilyPondToMusicXml(lily));
        Element measure = measureAt(outDoc, 0);

        assertEquals(true, lily.contains("%@mks slur voice=P1 measure=1 event=1 type=start"));
        assertEquals(true, lily.contains("%@mks slur voice=P1 measure=1 event=3 type=stop"));
        assertEquals(7, directChildren(measure, "note").size());
        assertEquals("16th", directText(directChild(directNoteAt(measure, 0), "type")));
        assertEquals("16th", directText(directChild(directNoteAt(measure, 1), "type")));
        assertEquals("16th", directText(directChild(directNoteAt(measure, 2), "type")));
        assertEquals("start", directChild(directChild(directNoteAt(measure, 0), "notations"), "slur").getAttribute("type"));
        assertEquals("stop", directChild(directChild(directNoteAt(measure, 2), "notations"), "slur").getAttribute("type"));
        assertEquals("start", directChild(directChild(directNoteAt(measure, 4), "notations"), "slur").getAttribute("type"));
        assertEquals("stop", directChild(directChild(directNoteAt(measure, 6), "notations"), "slur").getAttribute("type"));
    }

    @Test
    public void respectsNonFourFourMeasureCapacityOnDirectImport() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 3/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { r4 r4 d'8 a8 f8 | r4 r4 r4 }\n"
                + "}";

        String xml = LilyPondIo.convertLilyPondToMusicXml(lily);
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        Element firstMeasure = measureAt(doc, 0);
        Element secondMeasure = measureAt(doc, 1);

        Element time = directChild(directChild(firstMeasure, "attributes"), "time");
        assertEquals("3", directText(directChild(time, "beats")));
        assertEquals("4", directText(directChild(time, "beat-type")));
        assertEquals(2, directPitchCount(firstMeasure));
        assertEquals(true, doc.getElementsByTagName("miscellaneous-field").getLength() > 0);
        assertEquals(3, directChildren(secondMeasure, "note").size());
    }

    @Test
    public void carriesOverfullEventToNextMeasureInsteadOfDroppingIt() {
        String lily = "\\version \"2.24.0\"\n"
                + "\\time 3/4\n"
                + "\\key c \\major\n"
                + "\\score {\n"
                + "  \\new Staff = \"P1\" { r4 r4 d'8 a8 f8 | a8 d'8 f'8 a'8 d''8 f''8 }\n"
                + "}";

        String xml = LilyPondIo.convertLilyPondToMusicXml(lily);
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        List<String> firstMeasureSteps = pitchedSteps(measureAt(doc, 0));
        List<String> secondMeasureSteps = pitchedSteps(measureAt(doc, 1));

        assertEquals("D", firstMeasureSteps.get(0));
        assertEquals("A", firstMeasureSteps.get(1));
        assertEquals("F", secondMeasureSteps.get(0));
        assertEquals(true, xml.contains("carried event to next measure"));
    }

    @Test
    public void exportsMusicXmlToLilyPondTextWithTimeSignature() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes>\n"
                + "        <divisions>480</divisions>\n"
                + "        <key><fifths>0</fifths><mode>major</mode></key>\n"
                + "        <time><beats>4</beats><beat-type>4</beat-type></time>\n"
                + "      </attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>quarter</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>";

        String lily = LilyPondIo.exportMusicXmlDomToLilyPond(MusicXmlIo.parseMusicXmlDocument(xml));

        assertEquals(true, lily.contains("\\score"));
        assertEquals(true, lily.contains("\\new Staff"));
        assertEquals(true, lily.contains("\\time 4/4"));
    }

    private static Element measureAt(Document doc, int index) {
        Element part = directChild(doc.getDocumentElement(), "part");
        return measureAt(part, index);
    }

    private static Element partAt(Document doc, int index) {
        return directChildren(doc.getDocumentElement(), "part").get(index);
    }

    private static Element measureAt(Element part, int index) {
        return directChildren(part, "measure").get(index);
    }

    private static Element directNoteAt(Element measure, int index) {
        return directChildren(measure, "note").get(index);
    }

    private static int directPitchCount(Element measure) {
        int count = 0;
        for (Element note : directChildren(measure, "note")) {
            if (directChild(note, "pitch") != null) {
                count++;
            }
        }
        return count;
    }

    private static List<String> pitchedSteps(Element measure) {
        List<String> steps = new ArrayList<String>();
        for (Element note : directChildren(measure, "note")) {
            Element pitch = directChild(note, "pitch");
            if (pitch != null) {
                steps.add(directText(directChild(pitch, "step")));
            }
        }
        return steps;
    }

    private static Element directChild(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static boolean hasEnding(Document doc, String number, String type) {
        for (int index = 0; index < doc.getElementsByTagName("ending").getLength(); index++) {
            Element ending = (Element) doc.getElementsByTagName("ending").item(index);
            if (number.equals(ending.getAttribute("number")) && type.equals(ending.getAttribute("type"))) {
                return true;
            }
        }
        return false;
    }

    private static Element directChildWithAttribute(Element parent, String name, String attributeName,
            String attributeValue) {
        if (parent == null) {
            return null;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(((Element) child).getTagName())
                    && attributeValue.equals(((Element) child).getAttribute(attributeName))) {
                return (Element) child;
            }
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String name) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(((Element) child).getTagName())) {
                out.add((Element) child);
            }
        }
        return out;
    }

    private static String directText(Element element) {
        return element == null ? "" : element.getTextContent().trim();
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
