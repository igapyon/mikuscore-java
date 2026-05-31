package jp.igapyon.mikuscore.mei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MeiIoTest {
    @Test
    public void mapsMusicXmlDurationsAndAccidentalsToMeiValues() {
        assertEquals("maxima", MeiIo.noteTypeToDur("maxima"));
        assertEquals("1", MeiIo.noteTypeToDur("whole"));
        assertEquals("4", MeiIo.noteTypeToDur("quarter"));
        assertEquals("16", MeiIo.noteTypeToDur("16th"));
        assertEquals("4", MeiIo.noteTypeToDur("bad"));

        assertEquals("ff", MeiIo.alterToAccid("-2"));
        assertEquals("f", MeiIo.alterToAccid("-1"));
        assertEquals("n", MeiIo.alterToAccid("0"));
        assertEquals("s", MeiIo.alterToAccid("1"));
        assertEquals("ss", MeiIo.alterToAccid("2"));
        assertNull(MeiIo.alterToAccid("bad"));

        assertEquals("s", MeiIo.musicXmlAccidentalToAccid(" sharp "));
        assertEquals("f", MeiIo.musicXmlAccidentalToAccid("flat"));
        assertEquals("n", MeiIo.musicXmlAccidentalToAccid("natural"));
        assertEquals("ss", MeiIo.musicXmlAccidentalToAccid("sharp-sharp"));
        assertEquals("ff", MeiIo.musicXmlAccidentalToAccid("double-flat"));
        assertNull(MeiIo.musicXmlAccidentalToAccid("quarter-sharp"));
    }

    @Test
    public void mapsMeiKeyPitchLyricAndTimingHelpers() {
        assertEquals("0", MeiIo.fifthsToMeiKeySig(0));
        assertEquals("7s", MeiIo.fifthsToMeiKeySig(9));
        assertEquals("3f", MeiIo.fifthsToMeiKeySig(-3));

        assertEquals("c", MeiIo.toPname("C"));
        assertEquals("g", MeiIo.toPname(" g "));
        assertEquals("c", MeiIo.toPname("H"));

        assertEquals("i", MeiIo.lyricWordposFromSyllabic("begin"));
        assertEquals("m", MeiIo.lyricWordposFromSyllabic("middle"));
        assertEquals("t", MeiIo.lyricWordposFromSyllabic("end"));
        assertEquals("", MeiIo.lyricWordposFromSyllabic("single"));

        assertEquals("begin", MeiIo.lyricSyllabicFromWordpos("i"));
        assertEquals("middle", MeiIo.lyricSyllabicFromWordpos("m"));
        assertEquals("end", MeiIo.lyricSyllabicFromWordpos("t"));
        assertEquals("single", MeiIo.lyricSyllabicFromWordpos(""));

        assertEquals(480, MeiIo.toMksDur480(240, 240));
        assertEquals(1, MeiIo.toMksDur480(0, 480));
    }

    @Test
    public void mapsMeiTieAndArticulationHelpers() {
        assertEquals("m", MeiIo.extractMeiTieFromMusicXmlTieTypes(Arrays.asList("start", "stop")));
        assertEquals("i", MeiIo.extractMeiTieFromMusicXmlTieTypes(Arrays.asList(" start ")));
        assertEquals("t", MeiIo.extractMeiTieFromMusicXmlTieTypes(Arrays.asList("stop")));
        assertEquals("", MeiIo.extractMeiTieFromMusicXmlTieTypes(Arrays.asList("continue")));

        assertEquals(Arrays.asList("stacc", "spicc", "acc", "ten", "marc"),
                MeiIo.extractMeiArticulationTokensFromMusicXmlTags(Arrays.asList("staccato", "staccatissimo",
                        "accent", "tenuto", "strong-accent", "marcato", "staccato")));
        assertEquals("<artic artic=\"stacc\"/><artic artic=\"a&amp;b\"/>",
                MeiIo.buildMeiArticulationChildren(Arrays.asList("stacc", "a&b")));
    }

    @Test
    public void buildsSimpleMeiNoteAndRestFromMusicXmlNote() {
        org.w3c.dom.Document noteDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note><grace slash=\"yes\"/><pitch><step>F</step><alter>1</alter><octave>5</octave></pitch>"
                        + "<duration>240</duration><tie type=\"start\"/><voice>1</voice><type>eighth</type><dot/>"
                        + "<accidental>sharp</accidental><time-modification><actual-notes>3</actual-notes>"
                        + "<normal-notes>2</normal-notes></time-modification><notations><tuplet type=\"start\"/>"
                        + "<articulations><staccato/><accent/><strong-accent/></articulations></notations>"
                        + "<lyric><syllabic>begin</syllabic><text>La &amp; la</text></lyric></note>");
        org.w3c.dom.Element note = noteDoc.getDocumentElement();

        assertEquals(Arrays.asList("stacc", "acc", "marc"),
                MeiIo.extractMeiArticulationTokensFromMusicXmlNote(note));
        MeiIo.MeiLyric lyric = MeiIo.extractMusicXmlLyric(note);
        assertEquals("La & la", lyric.getText());
        assertEquals("begin", lyric.getSyllabic());
        assertEquals("<note pname=\"f\" oct=\"5\" dur=\"8\" xml:id=\"n1\" mks-dur-480=\"480\" mks-dur-div=\"240\" mks-dur-ticks=\"240\" dots=\"1\" accid=\"s\" num=\"3\" numbase=\"2\" mks-tuplet-start=\"1\" grace=\"acc\" tie=\"i\"><verse n=\"1\"><syl wordpos=\"i\">La &amp; la</syl></verse><artic artic=\"stacc\"/><artic artic=\"acc\"/><artic artic=\"marc\"/></note>",
                MeiIo.buildSimpleMeiPitchNote(note, 240, true, "n1"));

        org.w3c.dom.Document restDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note print-object=\"no\"><rest/><duration>360</duration><type>quarter</type><dot/></note>");
        assertEquals("<space dur=\"4\" mks-dur-480=\"360\" mks-dur-div=\"480\" mks-dur-ticks=\"360\" dots=\"1\"/>",
                MeiIo.buildSimpleMeiRest(restDoc.getDocumentElement(), 480));
    }

    @Test
    public void buildsSimpleMeiChordFromMusicXmlNotes() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<root><note><pitch><step>C</step><octave>4</octave></pitch><duration>240</duration>"
                        + "<tie type=\"start\"/><type>eighth</type><dot/><notations><articulations><tenuto/>"
                        + "</articulations></notations><lyric><syllabic>end</syllabic><text>Hi</text></lyric></note>"
                        + "<note><chord/><pitch><step>E</step><alter>-1</alter><octave>4</octave></pitch>"
                        + "<duration>240</duration><tie type=\"stop\"/><type>eighth</type><accidental>flat</accidental>"
                        + "</note></root>");
        java.util.List<org.w3c.dom.Element> notes = new java.util.ArrayList<org.w3c.dom.Element>();
        notes.add((org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("note").item(0));
        notes.add((org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("note").item(1));

        assertEquals("<chord dur=\"8\" mks-dur-480=\"480\" mks-dur-div=\"240\" mks-dur-ticks=\"240\" dots=\"1\">"
                + "<note xml:id=\"n1\" pname=\"c\" oct=\"4\" tie=\"i\"><verse n=\"1\"><syl wordpos=\"t\">Hi</syl></verse><artic artic=\"ten\"/></note>"
                + "<note xml:id=\"n2\" pname=\"e\" oct=\"4\" accid=\"f\" tie=\"t\"/></chord>",
                MeiIo.buildSimpleMeiChord(notes, 240, Arrays.asList("n1", "n2")));
    }

    @Test
    public void buildsMeiLayerContentFromMusicXmlNotes() {
        org.w3c.dom.Document restDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<root><note><rest/><duration>960</duration><type>half</type></note></root>");
        java.util.List<org.w3c.dom.Element> restOnly = new java.util.ArrayList<org.w3c.dom.Element>();
        restOnly.add((org.w3c.dom.Element) restDoc.getDocumentElement().getElementsByTagName("note").item(0));
        assertEquals("<mRest dur=\"2\" mks-dur-480=\"960\" mks-dur-div=\"480\" mks-dur-ticks=\"960\"/>",
                MeiIo.buildMeiLayerContentFromMusicXmlNotes(restOnly, 480, 960));

        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<root><note><grace slash=\"yes\"/><pitch><step>C</step><octave>5</octave></pitch><type>eighth</type></note>"
                        + "<note><grace/><pitch><step>D</step><octave>5</octave></pitch><type>eighth</type></note>"
                        + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration><type>quarter</type></note>"
                        + "<note><chord/><pitch><step>G</step><octave>4</octave></pitch><duration>480</duration><type>quarter</type></note>"
                        + "<note print-object=\"no\"><rest/><duration>240</duration><type>eighth</type></note></root>");
        java.util.List<org.w3c.dom.Element> notes = new java.util.ArrayList<org.w3c.dom.Element>();
        org.w3c.dom.NodeList nodeList = doc.getDocumentElement().getElementsByTagName("note");
        for (int index = 0; index < nodeList.getLength(); index++) {
            notes.add((org.w3c.dom.Element) nodeList.item(index));
        }

        assertEquals("<graceGrp slash=\"yes\"><note pname=\"c\" oct=\"5\" dur=\"8\" xml:id=\"mkN1\"/><note pname=\"d\" oct=\"5\" dur=\"8\" xml:id=\"mkN2\"/></graceGrp>"
                + "<chord dur=\"4\" mks-dur-480=\"480\" mks-dur-div=\"480\" mks-dur-ticks=\"480\"><note xml:id=\"mkN3\" pname=\"e\" oct=\"4\"/><note xml:id=\"mkN4\" pname=\"g\" oct=\"4\"/></chord>"
                + "<space dur=\"8\" mks-dur-480=\"240\" mks-dur-div=\"480\" mks-dur-ticks=\"240\"/>",
                MeiIo.buildMeiLayerContentFromMusicXmlNotes(notes, 480, 1920));
    }

    @Test
    public void buildsMusicXmlNoteFromMeiNote() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note pname=\"f\" oct=\"4\" accid=\"s\" tie=\"m\" slur=\"i2 t3\" artic=\"stacc ten\""
                        + " num=\"3\" numbase=\"2\" mks-tuplet-start=\"1\" stem.dir=\"up\">"
                        + "<artic>acc</artic><verse><syl wordpos=\"i\">La</syl></verse></note>");
        org.w3c.dom.Element note = doc.getDocumentElement();
        MeiIo.MeiLyric lyric = MeiIo.extractMeiLyric(note);
        assertEquals("La", lyric.getText());
        assertEquals("begin", lyric.getSyllabic());
        assertEquals(Arrays.asList("stacc", "ten", "acc"), MeiIo.readMeiArticulationTokens(note));
        MeiIo.MeiSoundingAccid accid = MeiIo.readMeiSoundingAccid(note);
        assertEquals("s", accid.getVisualAccid());
        assertEquals("s", accid.getSoundingAccid());
        assertEquals(Arrays.asList("<staccato/>", "<accent/>", "<tenuto/>"),
                MeiIo.buildMusicXmlArticulationsFromMeiTokens(Arrays.asList("stacc", "acc", "ten")));
        assertEquals(
                "<note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch><tie type=\"start\"/><tie type=\"stop\"/><duration>320</duration><voice>2</voice><type>eighth</type><dot/><stem>up</stem><accidental>sharp</accidental><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><articulations><staccato/><accent/><tenuto/></articulations><tuplet type=\"start\"/><tied type=\"start\"/><tied type=\"stop\"/><slur type=\"start\" number=\"2\"/><slur type=\"stop\" number=\"3\"/></notations><lyric><syllabic>begin</syllabic><text>La</text></lyric></note>",
                MeiIo.buildMusicXmlNoteFromMeiNote(note, 320, "eighth", 1, "2", 0));

        org.w3c.dom.Document graceDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note pname=\"b\" oct=\"5\" accid.ges=\"f\" grace=\"unacc\" stem.mod=\"slash\"/>");
        assertEquals(
                "<note><grace slash=\"yes\"/><pitch><step>B</step><alter>-1</alter><octave>5</octave></pitch><voice>1</voice><type>quarter</type></note>",
                MeiIo.buildMusicXmlNoteFromMeiNote(graceDoc.getDocumentElement(), 120, "quarter", 0, "1", 0));
    }

    @Test
    public void buildsParsedMeiRestEvent() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<rest dur=\"8\" dots=\"1\" num=\"3\" numbase=\"2\"/>");
        MeiIo.ParsedMeiXmlEvent rest = MeiIo.buildParsedMeiRestEvent(doc.getDocumentElement(), 480, "2");
        assertEquals("rest", rest.getKind());
        assertEquals(240, rest.getDurationTicks());
        assertEquals(Integer.valueOf(1), rest.getBeamDepth());
        assertNull(rest.getBreaksecAfter());
        assertEquals("<note><rest/><duration>240</duration><voice>2</voice><type>eighth</type><dot/></note>",
                rest.getXml());

        org.w3c.dom.Document metadataDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<rest dur=\"4\" mks-dur-ticks=\"120\" mks-dur-div=\"240\"/>");
        MeiIo.ParsedMeiXmlEvent metadataRest = MeiIo.buildParsedMeiRestEvent(metadataDoc.getDocumentElement(), 480,
                "1");
        assertEquals(240, metadataRest.getDurationTicks());
        assertEquals("<note><rest/><duration>240</duration><voice>1</voice><type>quarter</type></note>",
                metadataRest.getXml());

        org.w3c.dom.Document graceDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<rest grace=\"acc\" dur=\"8\"/>");
        assertNull(MeiIo.buildParsedMeiRestEvent(graceDoc.getDocumentElement(), 480, "1"));
    }

    @Test
    public void buildsParsedMeiNoteEvent() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note dur=\"8\" dots=\"1\" num=\"3\" numbase=\"2\" pname=\"f\" oct=\"4\" accid=\"s\" tie=\"i\""
                        + " slur=\"i2\" artic=\"ten\" mks-tuplet-start=\"1\" breaksec=\"3\" stem.dir=\"down\">"
                        + "<verse><syl wordpos=\"m\">La</syl></verse></note>");
        Map<String, Integer> tieCarry = new HashMap<String, Integer>();
        Map<String, Integer> measureAccidental = new HashMap<String, Integer>();
        MeiIo.ParsedMeiXmlEvent note = MeiIo.buildParsedMeiNoteEvent(doc.getDocumentElement(), 480, "4", 0, tieCarry,
                measureAccidental);
        assertEquals("note", note.getKind());
        assertEquals(240, note.getDurationTicks());
        assertEquals(Integer.valueOf(1), note.getBeamDepth());
        assertEquals(Integer.valueOf(3), note.getBreaksecAfter());
        assertEquals(Integer.valueOf(1), tieCarry.get("F:4"));
        assertEquals(Integer.valueOf(1), measureAccidental.get("F:4"));
        assertEquals(
                "<note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch><tie type=\"start\"/><duration>240</duration><voice>4</voice><type>eighth</type><dot/><stem>down</stem><accidental>sharp</accidental><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><articulations><tenuto/></articulations><tuplet type=\"start\"/><tied type=\"start\"/><slur type=\"start\" number=\"2\"/></notations><lyric><syllabic>middle</syllabic><text>La</text></lyric></note>",
                note.getXml());

        org.w3c.dom.Document carriedDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note dur=\"4\" pname=\"f\" oct=\"4\" tie=\"t\"/>");
        MeiIo.ParsedMeiXmlEvent carried = MeiIo.buildParsedMeiNoteEvent(carriedDoc.getDocumentElement(), 480, "4",
                0, tieCarry, measureAccidental);
        assertEquals(
                "<note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch><tie type=\"stop\"/><duration>480</duration><voice>4</voice><type>quarter</type><notations><tied type=\"stop\"/></notations></note>",
                carried.getXml());
        assertFalse(tieCarry.containsKey("F:4"));

        org.w3c.dom.Document graceDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note dur=\"4\" pname=\"g\" oct=\"5\" grace=\"acc\" stem.mod=\"slash\"/>");
        MeiIo.ParsedMeiXmlEvent grace = MeiIo.buildParsedMeiNoteEvent(graceDoc.getDocumentElement(), 480, "1", 0,
                null, null);
        assertEquals(0, grace.getDurationTicks());
        assertEquals(
                "<note><grace slash=\"yes\"/><pitch><step>G</step><octave>5</octave></pitch><voice>1</voice><type>quarter</type></note>",
                grace.getXml());
    }

    @Test
    public void buildsParsedMeiChordEvent() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<chord dur=\"8\" dots=\"1\" num=\"3\" numbase=\"2\" mks-tuplet-start=\"1\" breaksec=\"2\" artic=\"ten\">"
                        + "<note pname=\"c\" oct=\"4\" accid=\"s\" tie=\"i\" artic=\"stacc\"><verse><syl wordpos=\"t\">Hi</syl></verse></note>"
                        + "<note pname=\"e\" oct=\"4\" slur=\"i2\"/></chord>");
        Map<String, Integer> tieCarry = new HashMap<String, Integer>();
        Map<String, Integer> measureAccidental = new HashMap<String, Integer>();
        MeiIo.ParsedMeiXmlEvent chord = MeiIo.buildParsedMeiChordEvent(doc.getDocumentElement(), 480, "3", 0,
                tieCarry, measureAccidental);
        assertEquals("chord", chord.getKind());
        assertEquals(240, chord.getDurationTicks());
        assertEquals(Integer.valueOf(1), chord.getBeamDepth());
        assertEquals(Integer.valueOf(2), chord.getBreaksecAfter());
        assertEquals(Integer.valueOf(1), tieCarry.get("C:4"));
        assertEquals(Integer.valueOf(1), measureAccidental.get("C:4"));
        assertEquals(
                "<note><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><tie type=\"start\"/><duration>240</duration><voice>3</voice><type>eighth</type><dot/><accidental>sharp</accidental><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><articulations><staccato/><tenuto/></articulations><tuplet type=\"start\"/><tied type=\"start\"/></notations><lyric><syllabic>end</syllabic><text>Hi</text></lyric></note>"
                        + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>240</duration><voice>3</voice><type>eighth</type><dot/><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><notations><slur type=\"start\" number=\"2\"/></notations></note>",
                chord.getXml());

        org.w3c.dom.Document graceDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<chord dur=\"4\" grace=\"acc\"><note pname=\"g\" oct=\"5\"/></chord>");
        MeiIo.ParsedMeiXmlEvent graceChord = MeiIo.buildParsedMeiChordEvent(graceDoc.getDocumentElement(), 480, "1",
                0, null, null);
        assertEquals(0, graceChord.getDurationTicks());
        assertTrue(graceChord.getXml().contains("<grace slash=\"yes\"/>"));
        assertFalse(graceChord.getXml().contains("<duration>"));

        org.w3c.dom.Document emptyDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<chord dur=\"4\"/>");
        assertNull(MeiIo.buildParsedMeiChordEvent(emptyDoc.getDocumentElement(), 480, "1", 0, null, null));
    }

    @Test
    public void appliesMeiBeamContainerToParsedEvents() {
        java.util.List<MeiIo.ParsedMeiXmlEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>", Integer.valueOf(2),
                        Integer.valueOf(1)),
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>", Integer.valueOf(2), null),
                new MeiIo.ParsedMeiXmlEvent("rest", 120, "<note><rest/></note>", Integer.valueOf(1), null),
                new MeiIo.ParsedMeiXmlEvent("chord", 120, "<note><pitch/></note><note><chord/><pitch/></note>",
                        Integer.valueOf(2), null));
        java.util.List<MeiIo.ParsedMeiXmlEvent> beamed = MeiIo.applyMeiBeamContainerToEvents(events);

        assertEquals("<note><pitch/><beam number=\"1\">begin</beam></note>", beamed.get(0).getXml());
        assertEquals("<note><pitch/><beam number=\"1\">continue</beam><beam number=\"2\">begin</beam></note>",
                beamed.get(1).getXml());
        assertEquals("<note><rest/></note>", beamed.get(2).getXml());
        assertEquals("<note><pitch/><beam number=\"1\">end</beam><beam number=\"2\">end</beam></note><note><chord/><pitch/></note>",
                beamed.get(3).getXml());
    }

    @Test
    public void resolvesMeiForcedTupletAndGraceContext() {
        org.w3c.dom.Document tupletDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<tuplet num=\"3\" numbase=\"2\"><rest dur=\"4\"/></tuplet>");
        MeiIo.MeiForcedTuplet tuplet = MeiIo.resolveMeiTupletContext(tupletDoc.getDocumentElement(), null);
        assertEquals(3, tuplet.getNum());
        assertEquals(2, tuplet.getNumbase());

        org.w3c.dom.Document fallbackDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<tuplet num=\"bad\"/>");
        MeiIo.MeiForcedTuplet fallback = new MeiIo.MeiForcedTuplet(5, 4);
        assertEquals(5, MeiIo.resolveMeiTupletContext(fallbackDoc.getDocumentElement(), fallback).getNum());
        assertEquals(4, MeiIo.resolveMeiTupletContext(fallbackDoc.getDocumentElement(), fallback).getNumbase());

        org.w3c.dom.Document graceDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<graceGrp slash=\"yes\"><note pname=\"c\"/></graceGrp>");
        assertEquals("acc", MeiIo.resolveMeiGraceGroupValue(graceDoc.getDocumentElement()));

        org.w3c.dom.Document restDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<rest dur=\"4\"/>");
        org.w3c.dom.Element forcedRest = MeiIo.cloneMeiEventElementWithForcedContext(restDoc.getDocumentElement(),
                null, tuplet);
        assertEquals("3", forcedRest.getAttribute("num"));
        assertEquals("2", forcedRest.getAttribute("numbase"));
        MeiIo.ParsedMeiXmlEvent rest = MeiIo.buildParsedMeiRestEvent(restDoc.getDocumentElement(), 480, "1", tuplet);
        assertEquals(320, rest.getDurationTicks());
        assertEquals("<note><rest/><duration>320</duration><voice>1</voice><type>quarter</type></note>",
                rest.getXml());

        org.w3c.dom.Element graceRest = MeiIo.cloneMeiEventElementWithForcedContext(restDoc.getDocumentElement(),
                "acc", null);
        assertEquals("acc", graceRest.getAttribute("grace"));
        assertNull(MeiIo.buildParsedMeiRestEvent(graceRest, 480, "1"));
    }

    @Test
    public void expandsMeiStemSlashNodes() {
        org.w3c.dom.Document noteDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note xml:id=\"n1\" dur=\"4\" stem.mod=\"2slash\" tie=\"i\" slur=\"i2\""
                        + " mks-tuplet-start=\"1\" mks-tuplet-stop=\"1\"/>");
        java.util.List<org.w3c.dom.Element> notes = MeiIo.expandMeiStemSlashNodes(noteDoc.getDocumentElement(),
                480);
        assertEquals(4, notes.size());
        assertEquals(2, MeiIo.parseMeiStemSlashCount(noteDoc.getDocumentElement()));
        assertEquals("16", notes.get(0).getAttribute("dur"));
        assertEquals("120", notes.get(0).getAttribute("mks-dur-ticks"));
        assertEquals("120", notes.get(0).getAttribute("mks-dur-480"));
        assertEquals("1", notes.get(0).getAttribute("mks-tuplet-start"));
        assertEquals("", notes.get(0).getAttribute("mks-tuplet-stop"));
        assertEquals("", notes.get(1).getAttribute("xml:id"));
        assertEquals("", notes.get(1).getAttribute("tie"));
        assertEquals("", notes.get(1).getAttribute("slur"));
        assertEquals("", notes.get(1).getAttribute("mks-tuplet-start"));
        assertEquals("1", notes.get(3).getAttribute("mks-tuplet-stop"));

        org.w3c.dom.Document chordDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<chord dur=\"4\" stem.mod=\"slash\"><note xml:id=\"c1\" tie=\"i\" slur=\"i3\"/></chord>");
        java.util.List<org.w3c.dom.Element> chords = MeiIo.expandMeiStemSlashNodes(chordDoc.getDocumentElement(),
                480);
        assertEquals(2, chords.size());
        org.w3c.dom.Element secondChordNote = (org.w3c.dom.Element) chords.get(1).getElementsByTagName("note")
                .item(0);
        assertEquals("", secondChordNote.getAttribute("xml:id"));
        assertEquals("", secondChordNote.getAttribute("tie"));
        assertEquals("", secondChordNote.getAttribute("slur"));

        org.w3c.dom.Document graceDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<note dur=\"4\" stem.mod=\"slash\" grace=\"acc\"/>");
        assertNull(MeiIo.expandMeiStemSlashNodes(graceDoc.getDocumentElement(), 480));
    }

    @Test
    public void appliesMeiMeasureRestDurationMetadata() {
        org.w3c.dom.Document restDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument("<mRest/>");
        org.w3c.dom.Element prepared = MeiIo.applyMeiMeasureRestDurationMetadata(restDoc.getDocumentElement(), 1440,
                480);
        assertEquals("2", prepared.getAttribute("dur"));
        assertEquals("1", prepared.getAttribute("dots"));
        assertEquals("480", prepared.getAttribute("mks-dur-div"));
        assertEquals("1440", prepared.getAttribute("mks-dur-480"));
        assertEquals("1440", prepared.getAttribute("mks-dur-ticks"));

        MeiIo.ParsedMeiXmlEvent rest = MeiIo.buildParsedMeiRestEvent(prepared, 480, "1");
        assertEquals(1440, rest.getDurationTicks());
        assertEquals("<note><rest/><duration>1440</duration><voice>1</voice><type>half</type><dot/></note>",
                rest.getXml());

        org.w3c.dom.Document metadataDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<mSpace mks-dur-ticks=\"960\"/>");
        org.w3c.dom.Element unchanged = MeiIo.applyMeiMeasureRestDurationMetadata(metadataDoc.getDocumentElement(),
                1440, 480);
        assertSame(metadataDoc.getDocumentElement(), unchanged);

        org.w3c.dom.Document noteDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument("<note/>");
        assertSame(noteDoc.getDocumentElement(),
                MeiIo.applyMeiMeasureRestDurationMetadata(noteDoc.getDocumentElement(), 1440, 480));
    }

    @Test
    public void parsesMeiLayerEventsWithForcedContainersAndIds() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<layer>"
                        + "<beam><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"8\"/><rest dur=\"8\"/><note id=\"n2\" pname=\"d\" oct=\"4\" dur=\"8\"/></beam>"
                        + "<tuplet num=\"3\" numbase=\"2\"><note xml:id=\"n3\" pname=\"e\" oct=\"4\" dur=\"4\"/></tuplet>"
                        + "<graceGrp slash=\"yes\"><note xml:id=\"g1\" pname=\"f\" oct=\"4\" dur=\"8\"/></graceGrp>"
                        + "<note xml:id=\"s1\" pname=\"g\" oct=\"4\" dur=\"4\" stem.mod=\"slash\"/>"
                        + "<mRest xml:id=\"mr1\"/>"
                        + "<chord xml:id=\"c1\" dur=\"4\"><note xml:id=\"cn1\" pname=\"a\" oct=\"4\"/><note xml:id=\"cn2\" pname=\"c\" oct=\"5\"/></chord>"
                        + "</layer>");

        MeiIo.ParsedMeiLayer layer = MeiIo.parseMeiLayerEvents(doc.getDocumentElement(), 480, "2", 1920, 0);

        assertEquals(9, layer.getEvents().size());
        assertEquals(Integer.valueOf(0), layer.getIdToEventIndex().get("n1"));
        assertEquals(Integer.valueOf(2), layer.getIdToEventIndex().get("n2"));
        assertEquals(Integer.valueOf(3), layer.getIdToEventIndex().get("n3"));
        assertEquals(Integer.valueOf(7), layer.getIdToEventIndex().get("mr1"));
        assertEquals(Integer.valueOf(8), layer.getIdToEventIndex().get("c1"));
        assertEquals(Integer.valueOf(8), layer.getIdToEventIndex().get("cn1"));
        assertEquals(Integer.valueOf(8), layer.getIdToEventIndex().get("cn2"));

        assertTrue(layer.getEvents().get(0).getXml().contains("<beam number=\"1\">begin</beam>"));
        assertEquals("rest", layer.getEvents().get(1).getKind());
        assertTrue(layer.getEvents().get(2).getXml().contains("<beam number=\"1\">end</beam>"));
        assertTrue(layer.getEvents().get(3).getXml()
                .contains("<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"));
        assertTrue(layer.getEvents().get(4).getXml().contains("<grace slash=\"yes\"/>"));
        assertEquals(240, layer.getEvents().get(5).getDurationTicks());
        assertEquals(1920, layer.getEvents().get(7).getDurationTicks());
        assertTrue(layer.getEvents().get(8).getXml().contains("<chord/>"));
    }

    @Test
    public void buildsMeiStaffIdToEventTickFromParsedLayers() {
        org.w3c.dom.Document firstDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<layer><rest xml:id=\"r1\" dur=\"8\"/><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"8\"/>"
                        + "<note xml:id=\"n2\" pname=\"d\" oct=\"4\" dur=\"4\"/></layer>");
        org.w3c.dom.Document secondDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<layer><note xml:id=\"v2n1\" pname=\"e\" oct=\"4\" dur=\"2\"/></layer>");

        MeiIo.ParsedMeiLayer firstLayer = MeiIo.parseMeiLayerEvents(firstDoc.getDocumentElement(), 480, "1", 1920,
                0);
        MeiIo.ParsedMeiLayer secondLayer = MeiIo.parseMeiLayerEvents(secondDoc.getDocumentElement(), 480, "2", 1920,
                0);
        Map<String, Integer> idToTick = MeiIo.buildMeiStaffIdToEventTick(Arrays.asList(firstLayer, secondLayer));

        assertEquals(Integer.valueOf(0), idToTick.get("r1"));
        assertEquals(Integer.valueOf(240), idToTick.get("n1"));
        assertEquals(Integer.valueOf(480), idToTick.get("n2"));
        assertEquals(Integer.valueOf(0), idToTick.get("v2n1"));
        assertEquals(Integer.valueOf(480), MeiIo.resolveParsedMeiXmlEventStartTickByIndex(firstLayer.getEvents(), 2));
        assertNull(MeiIo.resolveParsedMeiXmlEventStartTickByIndex(firstLayer.getEvents(), 3));
    }

    @Test
    public void buildsProcessedMeiLayerXml() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><staff n=\"1\"><layer n=\"1\">"
                        + "<note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"8\"/>"
                        + "<note xml:id=\"n2\" pname=\"d\" oct=\"4\" dur=\"8\"/>"
                        + "<slur startid=\"#n1\" endid=\"#n2\"/>"
                        + "<dynam tstamp=\"1.5\">mf</dynam><harm tstamp=\"1.5\">Dm</harm>"
                        + "</layer></staff></measure>");
        org.w3c.dom.Element staff = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("staff")
                .item(0);
        org.w3c.dom.Element layer = (org.w3c.dom.Element) staff.getElementsByTagName("layer").item(0);
        MeiIo.ParsedMeiLayer parsed = MeiIo.parseMeiLayerEvents(layer, 480, "1", 480, 0);
        Map<String, Integer> idToTick = MeiIo.buildMeiStaffIdToEventTick(Arrays.asList(parsed));

        MeiIo.MeiProcessedLayerXml processed = MeiIo.buildMeiProcessedLayerXml(staff, layer, "1", parsed, null,
                idToTick, 480, 4, 480);

        assertEquals("1", processed.getVoice());
        assertEquals(480, processed.getTotalTicks());
        assertEquals(480, processed.getSourceTotalTicks());
        assertEquals(0, processed.getDroppedCount());
        assertEquals(0, processed.getTrimmedCount());
        assertTrue(processed.getXml().startsWith(
                "<harmony><root><root-step>D</root-step></root><kind>minor</kind><offset>240</offset><staff>1</staff></harmony>"));
        assertTrue(processed.getXml().contains(
                "<direction><direction-type><dynamics><mf/></dynamics></direction-type><offset>240</offset><voice>1</voice><staff>1</staff></direction>"));
        assertTrue(processed.getXml().contains("<slur type=\"start\" number=\"1\"/>"));
        assertTrue(processed.getXml().contains("<slur type=\"stop\" number=\"1\"/>"));
        assertTrue(processed.getTieCarryOut().isEmpty());
    }

    @Test
    public void buildsProcessedMeiStaffLayers() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><staff n=\"1\">"
                        + "<layer n=\"1\"><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\" tie=\"i\"/>"
                        + "<dynam tstamp=\"1\">mf</dynam></layer>"
                        + "<layer n=\"2\"><rest xml:id=\"r2\" dur=\"4\"/></layer>"
                        + "</staff></measure>");
        org.w3c.dom.Element staff = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("staff")
                .item(0);

        MeiIo.MeiProcessedStaffLayers processed = MeiIo.buildMeiProcessedStaffLayers(staff, 480, 4, 480, 0,
                null);

        assertEquals(2, processed.getLayers().size());
        assertEquals(480, processed.getMaxLayerTicks());
        assertEquals(Integer.valueOf(0), processed.getStaffIdToEventTick().get("n1"));
        assertEquals(Integer.valueOf(0), processed.getStaffIdToEventTick().get("r2"));
        assertTrue(processed.getBodyXml().contains("<backup><duration>480</duration></backup>"));
        assertTrue(processed.getBodyXml().contains(
                "<direction><direction-type><dynamics><mf/></dynamics></direction-type><voice>1</voice><staff>1</staff></direction>"));
        assertEquals(Integer.valueOf(0), processed.getTieCarryByVoice().get("1").get("C:4"));
        assertTrue(processed.getTieCarryByVoice().get("2").isEmpty());
    }

    @Test
    public void buildsImportedMeiMeasureFromProcessedStaff() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure n=\"1\"><staff n=\"1\"><annot type=\"musicxml-misc-field\" label=\"custom\">value</annot>"
                        + "<layer n=\"1\"><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                        + "<note xml:id=\"n2\" pname=\"d\" oct=\"4\" dur=\"4\"/></layer></staff></measure>");
        org.w3c.dom.Element staff = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("staff")
                .item(0);
        MeiIo.MeiPartImportState previous = new MeiIo.MeiPartImportState("1", "P1", "Piano", 4, 4, null, 0, "G",
                2, null, false);
        MeiIo.MeiMeasureImportState state = new MeiIo.MeiMeasureImportState(previous, "1", staff, true, null, null,
                null, "1", false, 1, 4, null, 0, "G", 2, null, 480, true, true, false, true);
        MeiIo.MeiProcessedStaffLayers processed = MeiIo.buildMeiProcessedStaffLayers(staff, 480, 4, 480, 0, null);

        String xml = MeiIo.buildMeiImportedMeasureXmlFromProcessedStaff(state, processed,
                new MeiIo.ResolvedMeiImportOptions(true, true, false, null),
                Arrays.asList(new MeiIo.MiscField("mks:src:mei", "raw")), false, 0, 480);

        assertTrue(xml.startsWith("<measure number=\"1\"><attributes><divisions>480</divisions>"));
        assertTrue(xml.contains("<miscellaneous-field name=\"mks:src:mei\">raw</miscellaneous-field>"));
        assertTrue(xml.contains("<miscellaneous-field name=\"mks:src:mei:custom\">value</miscellaneous-field>"));
        assertTrue(xml.contains("<miscellaneous-field name=\"mks:dbg:mei:notes:count\">0x0002</miscellaneous-field>"));
        assertTrue(xml.contains("code=OVERFULL_CLAMPED"));
        assertTrue(xml.contains("droppedEvents=1"));
        assertTrue(xml.contains("<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"));
        assertFalse(xml.contains("<step>D</step>"));

        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                MeiIo.buildMeiImportedMeasureXmlFromProcessedStaff(state, processed,
                        new MeiIo.ResolvedMeiImportOptions(false, false, true, null), null, true, 0, 480);
            }
        });
    }

    @Test
    public void buildsImportedMeiPartFromContext() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<mei><music><body><mdiv><score><title>Part Import</title>"
                        + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"1s\"><staffDef n=\"1\" label=\"Piano\" clef.shape=\"G\" clef.line=\"2\"/></scoreDef>"
                        + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\" tie=\"i\"/></layer></staff></measure>"
                        + "<measure n=\"2\"><staff n=\"1\"><layer n=\"1\"><note xml:id=\"n2\" pname=\"c\" oct=\"4\" dur=\"4\" tie=\"t\"/></layer></staff></measure>"
                        + "</section></score></mdiv></body></music></mei>");
        MeiIo.MeiInitialImportContext context = MeiIo.buildMeiInitialImportContext(doc.getDocumentElement());

        String partXml = MeiIo.buildMeiImportedPartXmlFromContext(context, "1", 0,
                new MeiIo.ResolvedMeiImportOptions(false, false, false, null),
                Arrays.asList(new MeiIo.MiscField("mks:src:mei", "raw")));

        assertTrue(partXml.startsWith("<part id=\"P1\"><measure number=\"1\" implicit=\"yes\"><attributes>"));
        assertTrue(partXml.contains("<miscellaneous-field name=\"mks:src:mei\">raw</miscellaneous-field>"));
        assertEquals(1, countOccurrences(partXml, "<divisions>480</divisions>"));
        assertTrue(partXml.contains("<measure number=\"2\">"));
        assertTrue(partXml.contains("<tie type=\"start\"/>"));
        assertTrue(partXml.contains("<tie type=\"stop\"/>"));
        assertEquals(2, countOccurrences(partXml, "<measure number=\""));
    }

    @Test
    public void buildsScorePartwiseXmlFromMeiContext() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<mei><music><body><mdiv><score><title>Doc Import</title>"
                        + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\">"
                        + "<staffDef n=\"1\" label=\"Piano RH\" clef.shape=\"G\" clef.line=\"2\"/>"
                        + "<staffDef n=\"2\" label=\"Piano LH\" clef.shape=\"F\" clef.line=\"4\"/>"
                        + "</scoreDef><section><measure n=\"1\">"
                        + "<staff n=\"1\"><layer n=\"1\"><note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff>"
                        + "<staff n=\"2\"><layer n=\"1\"><rest dur=\"4\"/></layer></staff>"
                        + "</measure></section></score></mdiv></body></music></mei>");
        MeiIo.MeiInitialImportContext context = MeiIo.buildMeiInitialImportContext(doc.getDocumentElement());

        String xml = MeiIo.buildMeiScorePartwiseXmlFromContext(context,
                new MeiIo.ResolvedMeiImportOptions(false, false, false, null),
                Collections.<MeiIo.MiscField>emptyList());

        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        assertEquals("score-partwise", musicXml.getDocumentElement().getNodeName());
        assertEquals("4.0", musicXml.getDocumentElement().getAttribute("version"));
        assertEquals("Doc Import", musicXml.getElementsByTagName("work-title").item(0).getTextContent());
        assertEquals(2, musicXml.getElementsByTagName("score-part").getLength());
        assertEquals(2, musicXml.getElementsByTagName("part").getLength());
        assertEquals("Piano RH", musicXml.getElementsByTagName("part-name").item(0).getTextContent());
        assertEquals("Piano LH", musicXml.getElementsByTagName("part-name").item(1).getTextContent());
        assertEquals("P1", ((org.w3c.dom.Element) musicXml.getElementsByTagName("part").item(0)).getAttribute("id"));
        assertEquals("P2", ((org.w3c.dom.Element) musicXml.getElementsByTagName("part").item(1)).getAttribute("id"));
        assertEquals("F", musicXml.getElementsByTagName("sign").item(1).getTextContent());
        assertEquals("4", musicXml.getElementsByTagName("line").item(1).getTextContent());
        assertTrue(xml.contains("\n"));
    }

    @Test
    public void convertsMeiTextToMusicXmlDocument() {
        String mei = "<mei><music><body><mdiv><score><title>Text Import</title>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffDef n=\"1\" label=\"Voice\"/></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"d\" oct=\"5\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, null);

        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        assertEquals("score-partwise", musicXml.getDocumentElement().getNodeName());
        assertEquals("Text Import", musicXml.getElementsByTagName("work-title").item(0).getTextContent());
        assertEquals("Voice", musicXml.getElementsByTagName("part-name").item(0).getTextContent());
        assertEquals("D", musicXml.getElementsByTagName("step").item(0).getTextContent());
        assertEquals("5", musicXml.getElementsByTagName("octave").item(0).getTextContent());
        assertTrue(xml.contains("mks:src:mei:raw-encoding"));

        String corpus = "<meiCorpus><mei><music><body><mdiv><score><title>First</title>"
                + "<scoreDef><staffDef n=\"1\"/></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer>"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff></measure></section></score></mdiv></body>"
                + "</music></mei><mei><music><body><mdiv><score><title>Second</title>"
                + "<scoreDef><staffDef n=\"1\"/></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer>"
                + "<rest dur=\"4\"/></layer></staff></measure></section></score></mdiv></body></music></mei></meiCorpus>";
        String selected = MeiIo.convertMeiToMusicXml(corpus, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE,
                Integer.valueOf(1));
        org.w3c.dom.Document selectedDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(selected);
        assertEquals("Second", selectedDoc.getElementsByTagName("work-title").item(0).getTextContent());
        assertTrue(selected.contains("mks:src:mei:raw-encoding"));

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                MeiIo.convertMeiToMusicXml("<mei>");
            }
        });
    }

    @Test
    public void importsFirstScoreBearingMeiFromMeiCorpusByDefault() {
        String meiCorpus = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<meiCorpus xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<meiHead><fileDesc><titleStmt><title>Corpus</title></titleStmt><pubStmt><p>test</p></pubStmt></fileDesc></meiHead>"
                + "<mei><meiHead><fileDesc><titleStmt><title>First score</title></titleStmt><pubStmt><p>test</p></pubStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"S1\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>"
                + "<mei><meiHead><fileDesc><titleStmt><title>Second score</title></titleStmt><pubStmt><p>test</p></pubStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"S2\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"d\" oct=\"5\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>"
                + "</meiCorpus>";

        String xml = MeiIo.convertMeiToMusicXml(meiCorpus, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);

        assertEquals("First score", musicXml.getElementsByTagName("work-title").item(0).getTextContent());
        assertEquals("C", musicXml.getElementsByTagName("step").item(0).getTextContent());
        assertEquals("4", musicXml.getElementsByTagName("octave").item(0).getTextContent());
    }

    @Test
    public void skipsEmptyFirstMeiCorpusChildAndImportsNextScoreBearingMei() {
        String meiCorpus = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<meiCorpus xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<mei><meiHead><fileDesc><titleStmt><title>Header only</title></titleStmt><pubStmt><p>test</p></pubStmt></fileDesc></meiHead></mei>"
                + "<mei><meiHead><fileDesc><titleStmt><title>Playable score</title></titleStmt><pubStmt><p>test</p></pubStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"S\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"e\" oct=\"5\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>"
                + "</meiCorpus>";

        String xml = MeiIo.convertMeiToMusicXml(meiCorpus, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);

        assertEquals("Playable score", musicXml.getElementsByTagName("work-title").item(0).getTextContent());
        assertEquals("E", musicXml.getElementsByTagName("step").item(0).getTextContent());
        assertEquals("5", musicXml.getElementsByTagName("octave").item(0).getTextContent());
    }

    @Test
    public void importsSelectedMeiCorpusChildByIndex() {
        String meiCorpus = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<meiCorpus xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<mei><meiHead><fileDesc><titleStmt><title>First score</title></titleStmt><pubStmt><p>test</p></pubStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"S1\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>"
                + "<mei><meiHead><fileDesc><titleStmt><title>Second score</title></titleStmt><pubStmt><p>test</p></pubStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"S2\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"d\" oct=\"5\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>"
                + "</meiCorpus>";

        String xml = MeiIo.convertMeiToMusicXml(meiCorpus, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE,
                Integer.valueOf(1));
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);

        assertEquals("Second score", musicXml.getElementsByTagName("work-title").item(0).getTextContent());
        assertEquals("D", musicXml.getElementsByTagName("step").item(0).getTextContent());
        assertEquals("5", musicXml.getElementsByTagName("octave").item(0).getTextContent());
    }

    @Test
    public void rejectsOutOfRangeMeiCorpusIndex() {
        final String meiCorpus = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<meiCorpus xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<mei><meiHead><fileDesc><titleStmt><title>A</title></titleStmt><pubStmt><p>x</p></pubStmt></fileDesc></meiHead></mei>"
                + "</meiCorpus>";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        MeiIo.convertMeiToMusicXml(meiCorpus, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE,
                                Integer.valueOf(3));
                    }
                });

        assertTrue(ex.getMessage().toLowerCase().contains("index out of range"));
    }

    @Test
    public void importsPartNameFromMeiStaffDefChildLabel() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\">"
                + "<staffGrp><staffDef n=\"1\" clef.shape=\"G\" clef.line=\"2\"><label>Violin 1</label></staffDef></staffGrp>"
                + "</scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);

        assertEquals("Violin 1", musicXml.getElementsByTagName("part-name").item(0).getTextContent());
        assertEquals("C", musicXml.getElementsByTagName("step").item(0).getTextContent());
        assertEquals("4", musicXml.getElementsByTagName("octave").item(0).getTextContent());
    }

    @Test
    public void importsMeterSymbolFromMeiStaffDefAsMusicXmlTimeSymbol() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score><scoreDef>"
                + "<staffGrp><staffDef n=\"1\" meter.sym=\"common\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp>"
                + "</scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element time = (org.w3c.dom.Element) musicXml.getElementsByTagName("time").item(0);

        assertEquals("common", time.getAttribute("symbol"));
        assertEquals("4", directChildText(time, "beats"));
        assertEquals("4", directChildText(time, "beat-type"));
    }

    @Test
    public void importsMidScoreScoreDefChangesIntoSubsequentMeasureAttributes() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\">"
                + "<staffGrp><staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp>"
                + "</scoreDef><section>"
                + "<measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff></measure>"
                + "<scoreDef meter.count=\"3\" meter.unit=\"4\" key.sig=\"2s\">"
                + "<staffGrp><staffDef n=\"1\" clef.shape=\"F\" clef.line=\"4\"/></staffGrp></scoreDef>"
                + "<measure n=\"2\"><staff n=\"1\"><layer n=\"1\"><note pname=\"d\" oct=\"3\" dur=\"4\"/></layer></staff></measure>"
                + "</section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure1 = findMeasureByNumber(musicXml, "1");
        org.w3c.dom.Element measure2 = findMeasureByNumber(musicXml, "2");
        org.w3c.dom.Element attributes1 = (org.w3c.dom.Element) measure1.getElementsByTagName("attributes").item(0);
        org.w3c.dom.Element attributes2 = (org.w3c.dom.Element) measure2.getElementsByTagName("attributes").item(0);

        assertEquals("4", nestedText(attributes1, "beats"));
        assertEquals("0", nestedText(attributes1, "fifths"));
        assertEquals("G", nestedText(attributes1, "sign"));
        assertEquals("3", nestedText(attributes2, "beats"));
        assertEquals("2", nestedText(attributes2, "fifths"));
        assertEquals("F", nestedText(attributes2, "sign"));
        assertEquals("4", nestedText(attributes2, "line"));
    }

    @Test
    public void importsMidScoreStaffDefChangesForTargetStaff() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\">"
                + "<staffGrp><staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp>"
                + "</scoreDef><section>"
                + "<measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff></measure>"
                + "<measure n=\"2\"><staffDef n=\"1\" key.sig=\"2\" clef.shape=\"F\" clef.line=\"4\" trans.diat=\"1\" trans.semi=\"2\"/>"
                + "<staff n=\"1\"><layer n=\"1\"><note pname=\"d\" oct=\"4\" dur=\"4\"/></layer></staff></measure>"
                + "</section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure2 = findMeasureByNumber(musicXml, "2");
        org.w3c.dom.Element attributes2 = (org.w3c.dom.Element) measure2.getElementsByTagName("attributes").item(0);

        assertEquals("2", nestedText(attributes2, "fifths"));
        assertEquals("F", nestedText(attributes2, "sign"));
        assertEquals("4", nestedText(attributes2, "line"));
        assertEquals("1", nestedText(attributes2, "diatonic"));
        assertEquals("2", nestedText(attributes2, "chromatic"));
    }

    @Test
    public void importsTranspositionFromScoreDefWhenStaffDefOmitsTransposition() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\" trans.diat=\"-2\" trans.semi=\"-3\">"
                + "<staffGrp><staffDef n=\"1\" label=\"Clarinet in A\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp>"
                + "</scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"5\" dur=\"4\"/></layer></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element attributes = (org.w3c.dom.Element) findMeasureByNumber(musicXml, "1")
                .getElementsByTagName("attributes").item(0);

        assertEquals("-2", nestedText(attributes, "diatonic"));
        assertEquals("-3", nestedText(attributes, "chromatic"));
    }

    @Test
    public void prefersStaffDefTranspositionOverScoreDefTranspositionForTargetStaff() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\" trans.diat=\"-2\" trans.semi=\"-3\">"
                + "<staffGrp><staffDef n=\"1\" label=\"Eb Clarinet\" clef.shape=\"G\" clef.line=\"2\" "
                + "trans.diat=\"2\" trans.semi=\"3\"/></staffGrp>"
                + "</scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"g\" oct=\"4\" dur=\"4\"/></layer></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element attributes = (org.w3c.dom.Element) findMeasureByNumber(musicXml, "1")
                .getElementsByTagName("attributes").item(0);

        assertEquals("2", nestedText(attributes, "diatonic"));
        assertEquals("3", nestedText(attributes, "chromatic"));
    }

    @Test
    public void importsAltoClefFromMeiStaffDefChildClef() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Viola\" lines=\"5\"><clef shape=\"C\" line=\"3\"/></staffDef>"
                + "</staffGrp></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element attributes = (org.w3c.dom.Element) findMeasureByNumber(musicXml, "1")
                .getElementsByTagName("attributes").item(0);

        assertEquals("C", nestedText(attributes, "sign"));
        assertEquals("3", nestedText(attributes, "line"));
    }

    @Test
    public void keepsPriorAltoClefWhenLaterStaffDefOmitsClef() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Viola\" lines=\"5\" clef.shape=\"C\" clef.line=\"3\"/>"
                + "</staffGrp></scoreDef><section><scoreDef><staffGrp>"
                + "<staffDef n=\"1\" label=\"Viola\"/>"
                + "</staffGrp></scoreDef><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/></layer></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element attributes = (org.w3c.dom.Element) findMeasureByNumber(musicXml, "1")
                .getElementsByTagName("attributes").item(0);

        assertEquals("C", nestedText(attributes, "sign"));
        assertEquals("3", nestedText(attributes, "line"));
    }

    @Test
    public void appliesMeasureLocalScoreDefBeforeStaffContent() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><scoreDef key.sig=\"2s\"><staffGrp>"
                + "<staffDef n=\"1\" clef.shape=\"F\" clef.line=\"4\"/></staffGrp></scoreDef>"
                + "<staff n=\"1\"><layer n=\"1\"><note pname=\"d\" oct=\"3\" dur=\"4\"/></layer></staff>"
                + "</measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element attributes = (org.w3c.dom.Element) findMeasureByNumber(musicXml, "1")
                .getElementsByTagName("attributes").item(0);

        assertEquals("2", nestedText(attributes, "fifths"));
        assertEquals("F", nestedText(attributes, "sign"));
        assertEquals("4", nestedText(attributes, "line"));
    }

    @Test
    public void emitsInitialAttributesOnFirstMeasureContainingTargetStaff() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"3\" meter.unit=\"4\" key.sig=\"2f\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Main\" clef.shape=\"G\" clef.line=\"2\"/>"
                + "<staffDef n=\"2\" label=\"Other\" clef.shape=\"F\" clef.line=\"4\"/>"
                + "</staffGrp></scoreDef><section>"
                + "<measure n=\"1\"><staff n=\"2\"><layer n=\"1\"><rest dur=\"4\"/></layer></staff></measure>"
                + "<measure n=\"2\"><staff n=\"1\"><layer n=\"1\"><note pname=\"b\" oct=\"4\" dur=\"4\"/></layer></staff></measure>"
                + "</section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure2 = findMeasureByNumber(musicXml, "2");
        org.w3c.dom.Element attributes = (org.w3c.dom.Element) measure2.getElementsByTagName("attributes").item(0);

        assertEquals("P1", ((org.w3c.dom.Element) measure2.getParentNode()).getAttribute("id"));
        assertEquals("480", directChildText(attributes, "divisions"));
        assertEquals("-2", nestedText(attributes, "fifths"));
        assertEquals("3", nestedText(attributes, "beats"));
        assertEquals("G", nestedText(attributes, "sign"));
    }

    @Test
    public void appliesKeySignatureImpliedAccidentalWhenAccidIsOmitted() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"1s\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"g\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(musicXml, "1").getElementsByTagName("note");
        org.w3c.dom.Element note1 = (org.w3c.dom.Element) notes.item(0);
        org.w3c.dom.Element note2 = (org.w3c.dom.Element) notes.item(1);
        org.w3c.dom.Element note3 = (org.w3c.dom.Element) notes.item(2);

        assertEquals("1", nestedText(note1, "alter"));
        assertEquals("", nestedText(note2, "alter"));
        assertEquals("", nestedText(note3, "alter"));
        assertEquals(0, note1.getElementsByTagName("accidental").getLength());
    }

    @Test
    public void carriesExplicitAccidentalWithinSameMeasureWhenFollowingNoteOmitsAccid() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\" accid=\"s\"/>"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(musicXml, "1").getElementsByTagName("note");

        assertEquals(2, notes.getLength());
        assertEquals("1", nestedText((org.w3c.dom.Element) notes.item(0), "alter"));
        assertEquals("1", nestedText((org.w3c.dom.Element) notes.item(1), "alter"));
    }

    @Test
    public void prefersStaffDefKeySigOverScoreDefKeySigForTargetStaff() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" key.sig=\"1s\" clef.shape=\"G\" clef.line=\"2\"/>"
                + "</staffGrp></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure = findMeasureByNumber(musicXml, "1");

        assertEquals("1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("attributes").item(0),
                "fifths"));
        assertEquals("1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("note").item(0), "alter"));
    }

    @Test
    public void acceptsMeiKeysigAliasOnScoreDefAndStaffDef() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" keysig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" keysig=\"1s\" clef.shape=\"G\" clef.line=\"2\"/>"
                + "</staffGrp></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure = findMeasureByNumber(musicXml, "1");

        assertEquals("1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("attributes").item(0),
                "fifths"));
        assertEquals("1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("note").item(0), "alter"));
    }

    @Test
    public void infersMajorKeyFifthsFromMeiKeyPnameAndModeWhenKeysigIsAbsent() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.pname=\"g\" key.mode=\"major\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/>"
                + "</staffGrp></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure = findMeasureByNumber(musicXml, "1");

        assertEquals("1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("attributes").item(0),
                "fifths"));
        assertEquals("1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("note").item(0), "alter"));
    }

    @Test
    public void infersMinorKeyFifthsFromMeiKeyPnameAndModeWhenKeysigIsAbsent() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.pname=\"d\" key.mode=\"minor\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/>"
                + "</staffGrp></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"b\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure = findMeasureByNumber(musicXml, "1");

        assertEquals("-1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("attributes").item(0),
                "fifths"));
        assertEquals("-1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("note").item(0), "alter"));
    }

    @Test
    public void infersKeyFifthsFromMeiKeyPnameAccidAndModeWhenKeysigIsAbsent() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.pname=\"f\" key.accid=\"s\" key.mode=\"major\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/>"
                + "</staffGrp></scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure = findMeasureByNumber(musicXml, "1");

        assertEquals("6", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("attributes").item(0),
                "fifths"));
        assertEquals("1", nestedText((org.w3c.dom.Element) measure.getElementsByTagName("note").item(0), "alter"));
    }

    @Test
    public void marksFirstShortMeasureAsImplicitPickupWhenDurationIsShorterThanMeter() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\">"
                + "<music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"8\"/>"
                + "</layer></staff></measure><measure n=\"2\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"d\" oct=\"4\" dur=\"1\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
        org.w3c.dom.Document musicXml = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure1 = findMeasureByNumber(musicXml, "1");

        assertEquals("yes", measure1.getAttribute("implicit"));
        assertEquals("240", directChildText((org.w3c.dom.Element) measure1.getElementsByTagName("note").item(0),
                "duration"));
    }

    @Test
    public void importsMeiMeasureRestsAndSpacesAsTimingPreservingRests() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"3\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><mRest/></layer></staff></measure>"
                + "<measure n=\"2\"><staff n=\"1\"><layer n=\"1\"><mSpace/></layer></staff></measure>"
                + "<measure n=\"3\"><staff n=\"1\"><layer n=\"1\"><space dur=\"4\"/><space dur=\"2\"/></layer></staff></measure>"
                + "</section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure1Note = firstNoteOfMeasure(doc, "1");
        org.w3c.dom.Element measure2Note = firstNoteOfMeasure(doc, "2");
        org.w3c.dom.NodeList measure3Notes = findMeasureByNumber(doc, "3").getElementsByTagName("note");

        assertTrue(measure1Note.getElementsByTagName("rest").getLength() > 0);
        assertTrue(measure2Note.getElementsByTagName("rest").getLength() > 0);
        assertEquals("1440", directChildText(measure1Note, "duration"));
        assertEquals("1440", directChildText(measure2Note, "duration"));
        assertEquals("half", directChildText(measure1Note, "type"));
        assertEquals(1, measure1Note.getElementsByTagName("dot").getLength());
        assertEquals("half", directChildText(measure2Note, "type"));
        assertEquals(1, measure2Note.getElementsByTagName("dot").getLength());
        assertEquals("480", directChildText((org.w3c.dom.Element) measure3Notes.item(0), "duration"));
        assertEquals("960", directChildText((org.w3c.dom.Element) measure3Notes.item(1), "duration"));
    }

    @Test
    public void importsMeiBeamBreaksecAsSecondaryBeamSplit() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><beam>"
                + "<note pname=\"c\" oct=\"5\" dur=\"8\"/>"
                + "<note pname=\"d\" oct=\"5\" dur=\"16\"/>"
                + "<note pname=\"e\" oct=\"5\" dur=\"32\"/>"
                + "<note pname=\"f\" oct=\"5\" dur=\"32\"/>"
                + "<note pname=\"g\" oct=\"5\" dur=\"16\" breaksec=\"1\"/>"
                + "<note pname=\"a\" oct=\"5\" dur=\"32\"/>"
                + "<note pname=\"b\" oct=\"5\" dur=\"32\"/>"
                + "<note pname=\"c\" oct=\"6\" dur=\"32\"/>"
                + "<note pname=\"d\" oct=\"6\" dur=\"32\"/>"
                + "</beam></layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(9, notes.getLength());
        assertEquals("eighth", directChildText((org.w3c.dom.Element) notes.item(0), "type"));
        assertEquals("16th", directChildText((org.w3c.dom.Element) notes.item(1), "type"));
        assertEquals("32nd", directChildText((org.w3c.dom.Element) notes.item(2), "type"));
        assertEquals("32nd", directChildText((org.w3c.dom.Element) notes.item(8), "type"));
        assertEquals("begin", beamText((org.w3c.dom.Element) notes.item(0), "1"));
        assertEquals("end", beamText((org.w3c.dom.Element) notes.item(8), "1"));
        assertEquals("begin", beamText((org.w3c.dom.Element) notes.item(1), "2"));
        assertEquals("end", beamText((org.w3c.dom.Element) notes.item(4), "2"));
        assertEquals("begin", beamText((org.w3c.dom.Element) notes.item(5), "2"));
        assertEquals("end", beamText((org.w3c.dom.Element) notes.item(8), "2"));
        assertEquals("begin", beamText((org.w3c.dom.Element) notes.item(2), "3"));
        assertEquals("end", beamText((org.w3c.dom.Element) notes.item(3), "3"));
        assertEquals("begin", beamText((org.w3c.dom.Element) notes.item(5), "3"));
        assertEquals("end", beamText((org.w3c.dom.Element) notes.item(8), "3"));
    }

    @Test
    public void importsMeiBeamGraceGroupWithPitchTimingAndBeamContinuity() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><beam>"
                + "<note pname=\"d\" oct=\"5\" dur=\"8\" stem.dir=\"down\"/>"
                + "<graceGrp slash=\"yes\"><note pname=\"e\" oct=\"5\" dur=\"8\" stem.dir=\"up\"/></graceGrp>"
                + "<note pname=\"d\" oct=\"5\" dur=\"8\"/>"
                + "<graceGrp slash=\"yes\"><note pname=\"c\" oct=\"5\" accid=\"s\" dur=\"8\" stem.dir=\"up\"/></graceGrp>"
                + "<note pname=\"d\" oct=\"5\" dur=\"8\"/>"
                + "<note pname=\"b\" oct=\"4\" dur=\"8\"/>"
                + "</beam></layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(6, notes.getLength());
        assertEquals("D5", pitchToken((org.w3c.dom.Element) notes.item(0)));
        assertEquals("E5", pitchToken((org.w3c.dom.Element) notes.item(1)));
        assertEquals("D5", pitchToken((org.w3c.dom.Element) notes.item(2)));
        assertEquals("C5", pitchToken((org.w3c.dom.Element) notes.item(3)));
        assertEquals("D5", pitchToken((org.w3c.dom.Element) notes.item(4)));
        assertEquals("B4", pitchToken((org.w3c.dom.Element) notes.item(5)));
        assertEquals("240", directChildText((org.w3c.dom.Element) notes.item(0), "duration"));
        assertEquals("", directChildText((org.w3c.dom.Element) notes.item(1), "duration"));
        assertEquals("", directChildText((org.w3c.dom.Element) notes.item(3), "duration"));
        assertEquals("1", nestedText((org.w3c.dom.Element) notes.item(3), "alter"));
        assertEquals("down", directChildText((org.w3c.dom.Element) notes.item(0), "stem"));
        assertEquals("up", directChildText((org.w3c.dom.Element) notes.item(1), "stem"));
        assertEquals("yes", ((org.w3c.dom.Element) notes.item(1)).getElementsByTagName("grace").item(0)
                .getAttributes().getNamedItem("slash").getNodeValue());
        assertEquals("yes", ((org.w3c.dom.Element) notes.item(3)).getElementsByTagName("grace").item(0)
                .getAttributes().getNamedItem("slash").getNodeValue());
        assertEquals("begin", beamText((org.w3c.dom.Element) notes.item(0), "1"));
        assertEquals("continue", beamText((org.w3c.dom.Element) notes.item(1), "1"));
        assertEquals("continue", beamText((org.w3c.dom.Element) notes.item(4), "1"));
        assertEquals("end", beamText((org.w3c.dom.Element) notes.item(5), "1"));
    }

    @Test
    public void importsMeiBeamSpanAsBeamContinuityOnListedNotes() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"n1\" pname=\"c\" oct=\"5\" dur=\"8\"/>"
                + "<note xml:id=\"n2\" pname=\"d\" oct=\"5\" dur=\"8\"/>"
                + "<note xml:id=\"n3\" pname=\"e\" oct=\"5\" dur=\"8\"/>"
                + "<note xml:id=\"n4\" pname=\"f\" oct=\"5\" dur=\"8\"/>"
                + "</layer><beamSpan plist=\"#n1 #n2 #n3 #n4\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(4, notes.getLength());
        assertEquals("begin", beamText((org.w3c.dom.Element) notes.item(0), "1"));
        assertEquals("continue", beamText((org.w3c.dom.Element) notes.item(1), "1"));
        assertEquals("continue", beamText((org.w3c.dom.Element) notes.item(2), "1"));
        assertEquals("end", beamText((org.w3c.dom.Element) notes.item(3), "1"));
    }

    @Test
    public void importsMeiTieAttributesAcrossMeasuresAsTieStartStop() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"1\" tie=\"i\"/>"
                + "</layer></staff></measure><measure n=\"2\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"n2\" pname=\"c\" oct=\"4\" dur=\"1\" tie=\"t\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element first = firstNoteOfMeasure(doc, "1");
        org.w3c.dom.Element second = firstNoteOfMeasure(doc, "2");

        assertTrue(hasDirectChildWithAttribute(first, "tie", "type", "start"));
        assertTrue(hasNestedChildWithAttribute(first, "tied", "type", "start"));
        assertTrue(hasDirectChildWithAttribute(second, "tie", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute(second, "tied", "type", "stop"));
    }

    @Test
    public void importsMeiSlurAttributesAsStartMiddleStopNotations() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\" slur=\"i1\"/>"
                + "<note pname=\"d\" oct=\"4\" dur=\"4\" slur=\"m1\"/>"
                + "<note pname=\"e\" oct=\"4\" dur=\"4\" slur=\"t1\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(0), "slur", "type", "start"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(0), "slur", "number", "1"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(1), "slur", "type", "start"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(1), "slur", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(2), "slur", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(2), "slur", "number", "1"));
    }

    @Test
    public void importsNoteLevelMeiTieAndSlurAttributesTogether() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\" tie=\"i\" slur=\"i1\"/>"
                + "<note pname=\"d\" oct=\"4\" dur=\"4\" tie=\"t\" slur=\"t1\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");
        org.w3c.dom.Element first = (org.w3c.dom.Element) notes.item(0);
        org.w3c.dom.Element second = (org.w3c.dom.Element) notes.item(1);

        assertEquals(2, notes.getLength());
        assertTrue(hasDirectChildWithAttribute(first, "tie", "type", "start"));
        assertTrue(hasNestedChildWithAttribute(first, "tied", "type", "start"));
        assertTrue(hasNestedChildWithAttribute(first, "slur", "type", "start"));
        assertTrue(hasNestedChildWithAttribute(first, "slur", "number", "1"));
        assertTrue(hasDirectChildWithAttribute(second, "tie", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute(second, "tied", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute(second, "slur", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute(second, "slur", "number", "1"));
    }

    @Test
    public void importsStaffLevelMeiSlurControlEventsById() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n2\" pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "</layer><slur startid=\"#n1\" endid=\"#n2\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(2, notes.getLength());
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(0), "slur", "type", "start"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(1), "slur", "type", "stop"));
    }

    @Test
    public void importsStaffLevelMeiTieControlEventsById() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n2\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "</layer><tie startid=\"#n1\" endid=\"#n2\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");
        org.w3c.dom.Element first = (org.w3c.dom.Element) notes.item(0);
        org.w3c.dom.Element second = (org.w3c.dom.Element) notes.item(1);

        assertEquals(2, notes.getLength());
        assertTrue(hasDirectChildWithAttribute(first, "tie", "type", "start"));
        assertTrue(hasNestedChildWithAttribute(first, "tied", "type", "start"));
        assertTrue(hasDirectChildWithAttribute(second, "tie", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute(second, "tied", "type", "stop"));
    }

    @Test
    public void resolvesStaffLevelMeiTieStartIdFromChordChildNoteId() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<chord dur=\"4\"><note xml:id=\"cn1\" pname=\"c\" oct=\"4\"/>"
                + "<note pname=\"e\" oct=\"4\"/></chord>"
                + "<note xml:id=\"n2\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "</layer><tie startid=\"#cn1\" endid=\"#n2\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");
        org.w3c.dom.Element first = (org.w3c.dom.Element) notes.item(0);
        org.w3c.dom.Element third = (org.w3c.dom.Element) notes.item(2);

        assertEquals(3, notes.getLength());
        assertTrue(hasDirectChildWithAttribute(first, "tie", "type", "start"));
        assertTrue(hasDirectChildWithAttribute(third, "tie", "type", "stop"));
    }

    @Test
    public void appliesStaffLevelMeiTieControlAccidentalCarry() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"n1\" pname=\"f\" oct=\"4\" dur=\"4\" accid=\"s\"/>"
                + "<note xml:id=\"n2\" pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "</layer><tie startid=\"#n1\" endid=\"#n2\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");
        org.w3c.dom.Element first = (org.w3c.dom.Element) notes.item(0);
        org.w3c.dom.Element second = (org.w3c.dom.Element) notes.item(1);

        assertEquals("1", nestedText(first, "alter"));
        assertEquals("1", nestedText(second, "alter"));
        assertTrue(hasDirectChildWithAttribute(first, "tie", "type", "start"));
        assertTrue(hasDirectChildWithAttribute(second, "tie", "type", "stop"));
    }

    @Test
    public void importsStaffLevelMeiSlurControlEventsByTstamp() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "</layer><slur tstamp=\"1\" tstamp2=\"2\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(2, notes.getLength());
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(0), "slur", "type", "start"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(1), "slur", "type", "stop"));
    }

    @Test
    public void importsStaffLevelMeiTieControlEventsByTstamp() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "</layer><tie tstamp=\"1\" tstamp2=\"2\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");
        org.w3c.dom.Element first = (org.w3c.dom.Element) notes.item(0);
        org.w3c.dom.Element second = (org.w3c.dom.Element) notes.item(1);

        assertEquals(2, notes.getLength());
        assertTrue(hasDirectChildWithAttribute(first, "tie", "type", "start"));
        assertTrue(hasNestedChildWithAttribute(first, "tied", "type", "start"));
        assertTrue(hasDirectChildWithAttribute(second, "tie", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute(second, "tied", "type", "stop"));
    }

    @Test
    public void importsLayerLevelMeiSlurControlEventsById() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"ln1\" pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"ln2\" pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "<slur startid=\"#ln1\" endid=\"#ln2\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(2, notes.getLength());
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(0), "slur", "type", "start"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(1), "slur", "type", "stop"));
    }

    @Test
    public void importsLayerLevelMeiTieControlEventsById() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"tn1\" pname=\"g\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"tn2\" pname=\"g\" oct=\"4\" dur=\"4\"/>"
                + "<tie startid=\"#tn1\" endid=\"#tn2\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");
        org.w3c.dom.Element first = (org.w3c.dom.Element) notes.item(0);
        org.w3c.dom.Element second = (org.w3c.dom.Element) notes.item(1);

        assertEquals(2, notes.getLength());
        assertTrue(hasDirectChildWithAttribute(first, "tie", "type", "start"));
        assertTrue(hasNestedChildWithAttribute(first, "tied", "type", "start"));
        assertTrue(hasDirectChildWithAttribute(second, "tie", "type", "stop"));
        assertTrue(hasNestedChildWithAttribute(second, "tied", "type", "stop"));
    }

    @Test
    public void importsMeiControlEventUsingPlistWhenStartIdIsAbsent() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"p1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"p2\" pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "<trill plist=\"#p1 #p2\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(2, notes.getLength());
        assertTrue(((org.w3c.dom.Element) notes.item(0)).getElementsByTagName("trill-mark").getLength() > 0);
        assertEquals(0, ((org.w3c.dom.Element) notes.item(1)).getElementsByTagName("trill-mark").getLength());
    }

    @Test
    public void importsMeiSlurSpanUsingPlistAndTstamp2() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"sp1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"sp2\" pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "<slur plist=\"#sp1\" tstamp2=\"2\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(2, notes.getLength());
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(0), "slur", "type", "start"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(1), "slur", "type", "stop"));
    }

    @Test
    public void importsMeiAccidGesAsSoundingAlterWithoutVisualAccidental() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\" accid.ges=\"s\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element note = firstNoteOfMeasure(doc, "1");

        assertEquals("1", nestedText(note, "alter"));
        assertEquals(0, note.getElementsByTagName("accidental").getLength());
    }

    @Test
    public void importsChordNoteMeiAccidGesAsSoundingAlterForEachMember() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\"><chord dur=\"4\">"
                + "<note pname=\"f\" oct=\"4\" accid.ges=\"s\"/>"
                + "<note pname=\"b\" oct=\"4\" accid.ges=\"f\"/>"
                + "</chord></layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(2, notes.getLength());
        assertEquals("1", nestedText((org.w3c.dom.Element) notes.item(0), "alter"));
        assertEquals("-1", nestedText((org.w3c.dom.Element) notes.item(1), "alter"));
    }

    @Test
    public void importsMeiFermataControlEventByTstampAsInvertedNotation() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "<fermata tstamp=\"2\" place=\"below\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element second = (org.w3c.dom.Element) findMeasureByNumber(doc, "1").getElementsByTagName("note")
                .item(1);

        assertTrue(hasNestedChildWithAttribute(second, "fermata", "type", "inverted"));
    }

    @Test
    public void importsMeiTurnAndMordentControlEventsAsOrnaments() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"orn1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"orn2\" pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "<turn startid=\"#orn1\" type=\"inverted\"/>"
                + "<mordent startid=\"#orn2\" type=\"upper\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertTrue(((org.w3c.dom.Element) notes.item(0)).getElementsByTagName("inverted-turn").getLength() > 0);
        assertTrue(((org.w3c.dom.Element) notes.item(1)).getElementsByTagName("mordent").getLength() > 0);
    }

    @Test
    public void importsMeiBreathAndCaesuraControlEventsAsArticulations() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" label=\"Lead\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"f\" oct=\"4\" dur=\"4\"/>"
                + "<breath tstamp=\"1\"/>"
                + "<caesura tstamp=\"2\"/>"
                + "</layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertTrue(((org.w3c.dom.Element) notes.item(0)).getElementsByTagName("breath-mark").getLength() > 0);
        assertTrue(((org.w3c.dom.Element) notes.item(1)).getElementsByTagName("caesura").getLength() > 0);
    }

    @Test
    public void importsMeiDynamAsMusicXmlDynamicsMark() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\">"
                + "<layer n=\"1\"><note pname=\"c\" oct=\"4\" dur=\"4\"/></layer>"
                + "<dynam tstamp=\"1\">mf</dynam></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element measure = findMeasureByNumber(doc, "1");

        assertEquals(1, measure.getElementsByTagName("direction").getLength());
        assertEquals(1, measure.getElementsByTagName("dynamics").getLength());
        assertEquals(1, measure.getElementsByTagName("mf").getLength());
    }

    @Test
    public void importsMeiTupletAsThreeToTwoTimeModification() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<tuplet num=\"3\" numbase=\"2\">"
                + "<note pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "<note pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "</tuplet></layer></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertEquals(3, notes.getLength());
        for (int i = 0; i < notes.getLength(); i++) {
            org.w3c.dom.Element note = (org.w3c.dom.Element) notes.item(i);
            assertEquals("3", nestedText(note, "actual-notes"));
            assertEquals("2", nestedText(note, "normal-notes"));
        }
    }

    @Test
    public void importsMeiFermataAsMusicXmlFermataNotation() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\">"
                + "<layer n=\"1\"><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n2\" pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n3\" pname=\"e\" oct=\"4\" dur=\"4\"/></layer>"
                + "<fermata startid=\"#n3\"/></staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element third = (org.w3c.dom.Element) findMeasureByNumber(doc, "1").getElementsByTagName("note")
                .item(2);

        assertTrue(hasNestedChildWithAttribute(third, "fermata", "type", ""));
        assertEquals(1, third.getElementsByTagName("fermata").getLength());
    }

    @Test
    public void importsMeiGlissAsMusicXmlGlissandoStartStop() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\">"
                + "<layer n=\"1\"><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n2\" pname=\"d\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n3\" pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n4\" pname=\"f\" oct=\"4\" dur=\"4\"/></layer>"
                + "<gliss startid=\"#n1\" endid=\"#n4\"/></staff></measure></section>"
                + "</score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.NodeList notes = findMeasureByNumber(doc, "1").getElementsByTagName("note");

        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(0), "glissando", "type", "start"));
        assertTrue(hasNestedChildWithAttribute((org.w3c.dom.Element) notes.item(3), "glissando", "type", "stop"));
    }

    @Test
    public void buildsMeiExportScoreDefScaffoldFromMusicXml() {
        assertEquals("5.1+basic", MeiIo.normalizeMeiVersion(""));
        assertEquals("5.1+basic", MeiIo.normalizeMeiVersion("latest"));
        assertEquals("5.1+custom", MeiIo.normalizeMeiVersion("5.1+custom"));
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise version=\"4.0\"><work><work-title>Export</work-title></work><part-list>"
                        + "<score-part id=\"P1\"><part-name>Piano &amp; Keys</part-name></score-part>"
                        + "<score-part id=\"P2\"><part-abbreviation>Fl.</part-abbreviation></score-part></part-list>"
                        + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                        + "<key><fifths>-1</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                        + "<staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef>"
                        + "<clef number=\"2\"><sign>F</sign><line>4</line></clef>"
                        + "<transpose><diatonic>1</diatonic><chromatic>2</chromatic></transpose></attributes>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                        + "<voice>1</voice><staff>2</staff></note></measure></part>"
                        + "<part id=\"P2\"><measure number=\"1\"><attributes><clef><sign>C</sign><line>3</line></clef>"
                        + "</attributes><note><rest/><duration>480</duration><voice>1</voice></note></measure></part>"
                        + "</score-partwise>");

        java.util.Map<String, String> partNames = MeiIo.readMusicXmlPartNameMap(doc);
        assertEquals("Piano & Keys", partNames.get("P1"));
        assertEquals("Fl.", partNames.get("P2"));
        java.util.List<MeiIo.MusicXmlStaffSlot> slots = MeiIo.collectMusicXmlStaffSlots(doc);
        assertEquals(3, slots.size());
        assertEquals("Piano & Keys (1)", slots.get(0).getLabel());
        assertEquals("Piano & Keys (2)", slots.get(1).getLabel());
        assertEquals("Fl.", slots.get(2).getLabel());

        org.w3c.dom.Element firstPart = (org.w3c.dom.Element) doc.getElementsByTagName("part").item(0);
        assertEquals(2, MeiIo.detectMusicXmlStaffCountForPart(firstPart));
        MeiIo.MeiClef bass = MeiIo.resolveClefForMusicXmlSlot(firstPart, 2);
        assertEquals("F", bass.getClefSign());
        assertEquals(4, bass.getClefLine());
        MeiIo.MeiTranspose transpose = MeiIo.resolveTransposeForMusicXmlSlot(firstPart, 1);
        assertEquals(Integer.valueOf(2), transpose.getChromatic());
        assertEquals(Integer.valueOf(1), transpose.getDiatonic());

        String scoreDef = MeiIo.buildMeiExportScoreDefXml(doc);
        assertTrue(scoreDef.startsWith("<scoreDef meter.count=\"3\" meter.unit=\"4\" key.sig=\"1f\"><staffGrp>"));
        assertTrue(scoreDef.contains("<staffDef n=\"1\" label=\"Piano &amp; Keys (1)\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\" trans.diat=\"1\" trans.semi=\"2\">"));
        assertTrue(scoreDef.contains("<staffDef n=\"2\" label=\"Piano &amp; Keys (2)\" lines=\"5\" clef.shape=\"F\" clef.line=\"4\" trans.diat=\"1\" trans.semi=\"2\">"));
        assertTrue(scoreDef.contains("<staffDef n=\"3\" label=\"Fl.\" lines=\"5\" clef.shape=\"C\" clef.line=\"3\">"));
    }

    @Test
    public void buildsMeiExportMeasureNumberAndDocumentScaffold() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise version=\"4.0\"><movement-title>Movement Title</movement-title><part-list>"
                        + "<score-part id=\"P1\"><part-name>One</part-name></score-part>"
                        + "<score-part id=\"P2\"><part-name>Two</part-name></score-part></part-list>"
                        + "<part id=\"P1\"><measure number=\"A\"/><measure number=\"B\"/></part>"
                        + "<part id=\"P2\"><measure number=\"A\"/><measure/></part></score-partwise>");

        assertEquals(Arrays.asList("A", "B", "3"), MeiIo.gatherMusicXmlMeasureNumbers(doc));
        assertEquals("Movement Title", MeiIo.resolveMusicXmlTitleForMeiExport(doc));

        org.w3c.dom.Document titled = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><work><work-title>Work &amp; Title</work-title></work><part id=\"P1\"/></score-partwise>");
        assertEquals("Work & Title", MeiIo.resolveMusicXmlTitleForMeiExport(titled));

        String mei = MeiIo.buildMeiExportDocumentXml("Work & Title", "bad", "<scoreDef/>", "<measure n=\"A\"/>");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1+basic\">"
                + "<meiHead><fileDesc><titleStmt><title>Work &amp; Title</title></titleStmt><pubStmt><p>Generated by mikuscore</p></pubStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score><scoreDef/><section><measure n=\"A\"/></section></score></mdiv></body></music></mei>",
                mei);
    }

    @Test
    public void extractsMusicXmlMeasureMiscAndEncodesMeiMeasureMeta() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure number=\"5\" implicit=\"yes\"><attributes><time><beats>6</beats><beat-type>8</beat-type></time>"
                        + "<miscellaneous><miscellaneous-field name=\"mks:src:abc\"> raw </miscellaneous-field>"
                        + "<miscellaneous-field>ignored</miscellaneous-field>"
                        + "<miscellaneous-field name=\"custom\">value</miscellaneous-field></miscellaneous></attributes>"
                        + "<barline location=\"left\"><bar-style>light-light</bar-style><repeat direction=\"forward\"/></barline>"
                        + "<barline location=\"right\"><bar-style>light-light</bar-style><repeat direction=\"backward\"/>"
                        + "<ending number=\"3\" type=\"stop\"/></barline></measure>");
        org.w3c.dom.Element measure = doc.getDocumentElement();

        java.util.List<MeiIo.MiscField> fields = MeiIo.extractMusicXmlMiscellaneousFieldsFromMeasure(measure);
        assertEquals(2, fields.size());
        assertEquals("mks:src:abc", fields.get(0).getName());
        assertEquals("raw", fields.get(0).getValue());
        assertEquals("custom", fields.get(1).getName());
        assertEquals("value", fields.get(1).getValue());
        assertEquals("number=5;implicit=1;repeat=backward;times=3;explicitTime=1;beats=6;beatType=8;doubleBar=both",
                MeiIo.encodeMusicXmlMeasureMetaForMei(measure));

        org.w3c.dom.Document empty = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument("<measure/>");
        assertTrue(MeiIo.extractMusicXmlMiscellaneousFieldsFromMeasure(empty.getDocumentElement()).isEmpty());
        assertNull(MeiIo.encodeMusicXmlMeasureMetaForMei(empty.getDocumentElement()));
    }

    @Test
    public void escapesXmlAndParsesIntegersSafely() {
        assertEquals("A&amp;B&lt;C&gt;&quot;", MeiIo.xmlEscape("A&B<C>\""));
        assertEquals(12, MeiIo.parseIntSafe(" 12 ", -1));
        assertEquals(-1, MeiIo.parseIntSafe("bad", -1));
    }

    @Test
    public void mapsHarmonyKindDegreeAndTstampHelpers() {
        assertEquals("#", MeiIo.accidentalTextFromAlter(1));
        assertEquals("bb", MeiIo.accidentalTextFromAlter(-2));
        assertEquals("", MeiIo.accidentalTextFromAlter(3));

        MeiIo.HarmonyKindSuffix text = MeiIo.suffixFromHarmonyKind("major", " add9 ");
        assertEquals("add9", text.getSuffix());
        assertEquals(true, text.isFromText());
        assertEquals("m7", MeiIo.suffixFromHarmonyKind("minor-seventh", "").getSuffix());
        assertEquals("", MeiIo.suffixFromHarmonyKind("other", "").getSuffix());

        assertEquals("#5b9", MeiIo.degreeSuffixFromHarmony(Arrays.asList(new MeiIo.HarmonyDegree(5, 1),
                new MeiIo.HarmonyDegree(7, 0), new MeiIo.HarmonyDegree(9, -1))));
        assertEquals("1.5", MeiIo.offsetTicksToTstamp(240, 480, 4));
        assertEquals("2.333", MeiIo.offsetTicksToTstamp(640, 480, 4));
    }

    @Test
    public void buildsMeiHarmonyXmlFromMusicXmlHarmonyValues() {
        String xml = MeiIo.buildMeiHarmFromMusicXmlHarmonyValues(" C ", Integer.valueOf(1), "minor", "",
                "G", Integer.valueOf(-1), Arrays.asList(new MeiIo.HarmonyDegree(9, 1)), 240, 480, 4);

        assertEquals("<harm tstamp=\"1.5\">C#m#9/Gb</harm>", xml);
        assertNull(MeiIo.buildMeiHarmFromMusicXmlHarmonyValues("H", null, "major", "", null, null, null, 0, 480, 4));

        assertEquals(Arrays.asList("<harm tstamp=\"1\">F7</harm>"),
                MeiIo.collectMeiHarmsForStaff(Arrays.asList(
                        new MeiIo.MeiHarmonySource(2, "C", null, "major", "", null, null, null, 0),
                        new MeiIo.MeiHarmonySource(1, "F", null, "dominant", "", null, null, null, 0)),
                        1, 480, 4));
    }

    @Test
    public void buildsMeiHarmonyXmlFromMusicXmlHarmonyDom() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><harmony><root><root-step>C</root-step><root-alter>1</root-alter></root>"
                        + "<kind>minor</kind><degree><degree-value>9</degree-value><degree-alter>1</degree-alter></degree>"
                        + "<bass><bass-step>G</bass-step><bass-alter>-1</bass-alter></bass><offset>240</offset><staff>2</staff></harmony>"
                        + "<harmony><root><root-step>F</root-step></root><kind>dominant</kind></harmony>"
                        + "<harmony><root><root-step>H</root-step></root><kind>major</kind></harmony></measure>");

        assertEquals("<harm tstamp=\"1.5\">C#m#9/Gb</harm>",
                MeiIo.buildMeiHarmFromMusicXmlHarmony(
                        (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("harmony").item(0),
                        480, 4));
        assertEquals(Arrays.asList("<harm tstamp=\"1.5\">C#m#9/Gb</harm>"),
                MeiIo.collectMeiHarmsForStaff(doc.getDocumentElement(), 2, 480, 4));
        assertEquals(Arrays.asList("<harm tstamp=\"1\">F7</harm>"),
                MeiIo.collectMeiHarmsForStaff(doc.getDocumentElement(), 1, 480, 4));
        assertEquals("<harm staff=\"3\" tstamp=\"1\">F7</harm>",
                MeiIo.withStaffAttr("<harm tstamp=\"1\">F7</harm>", 3));
        assertEquals("<harm staff=\"2\" tstamp=\"1\">F7</harm>",
                MeiIo.withStaffAttr("<harm staff=\"2\" tstamp=\"1\">F7</harm>", 3));
    }

    @Test
    public void collectsMeiDirectionControlsForStaffFromMusicXml() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure>"
                        + "<direction placement=\"above\"><direction-type><dynamics><mf/></dynamics></direction-type><staff>1</staff></direction>"
                        + "<note><duration>480</duration><staff>1</staff></note>"
                        + "<direction placement=\"below\"><direction-type><words>Allegro</words></direction-type><sound tempo=\"126.5\"/><staff>1</staff></direction>"
                        + "<direction><direction-type><words>ignored</words></direction-type><staff>2</staff></direction>"
                        + "<sound tempo=\"88.50\" offset=\"240\"/>"
                        + "<backup><duration>480</duration></backup>"
                        + "<direction placement=\"above\"><direction-type><wedge type=\"crescendo\" number=\"1\"/></direction-type><staff>1</staff></direction>"
                        + "<forward><duration>480</duration></forward>"
                        + "<direction placement=\"above\"><direction-type><wedge type=\"stop\" number=\"1\"/></direction-type><staff>1</staff></direction>"
                        + "<backup><duration>480</duration></backup>"
                        + "<direction placement=\"below\"><direction-type><pedal type=\"start\" number=\"2\"/></direction-type><staff>1</staff></direction>"
                        + "<forward><duration>240</duration></forward>"
                        + "<direction placement=\"below\"><direction-type><pedal type=\"stop\" number=\"2\"/></direction-type><staff>1</staff></direction>"
                        + "<backup><duration>240</duration></backup>"
                        + "<direction><direction-type><octave-shift type=\"down\" size=\"15\" number=\"1\"/></direction-type><staff>1</staff></direction>"
                        + "<forward><duration>240</duration></forward>"
                        + "<direction><direction-type><octave-shift type=\"stop\" size=\"15\" number=\"1\"/></direction-type><staff>1</staff></direction>"
                        + "<direction placement=\"above\"><direction-type><segno/></direction-type><staff>1</staff></direction>"
                        + "<direction><direction-type><words>Fine</words></direction-type><staff>1</staff></direction>"
                        + "</measure>");

        assertEquals(Arrays.asList(
                "<dynam tstamp=\"1\" place=\"above\">mf</dynam>",
                "<tempo tstamp=\"2\" midi.bpm=\"126.5\" place=\"below\">Allegro</tempo>",
                "<dynam tstamp=\"1.5\">Fine</dynam>",
                "<tempo type=\"mscore-infer-from-text\" tstamp=\"1.5\" midi.bpm=\"88.5\">\u2669 = 88.5</tempo>",
                "<hairpin form=\"cres\" tstamp=\"1\" tstamp2=\"2\" place=\"above\"/>",
                "<pedal tstamp=\"1\" tstamp2=\"1.5\" place=\"below\"/>",
                "<octave dis=\"15\" dis.place=\"below\" tstamp=\"1\" tstamp2=\"1.5\"/>",
                "<repeatMark tstamp=\"1.5\" place=\"above\">segno</repeatMark>",
                "<repeatMark tstamp=\"1.5\">Fine</repeatMark>"),
                MeiIo.collectMeiDirectionControlsForStaff(doc.getDocumentElement(), 1, 480, 4));
        assertEquals(Collections.<String>emptyList(),
                MeiIo.collectMeiDirectionControlsForStaff(doc.getDocumentElement(), 3, 480, 4));
    }

    @Test
    public void collectsMeiGlissSlideControlsForStaffFromMusicXmlTimeline() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure>"
                        + "<note><pitch/><duration>480</duration><staff>1</staff><notations>"
                        + "<glissando type=\"start\" number=\"2\"/><slide type=\"stop\" number=\"1\"/></notations></note>"
                        + "<backup><duration>240</duration></backup>"
                        + "<note><pitch/><duration>240</duration><staff>2</staff><notations><glissando type=\"start\" number=\"2\"/></notations></note>"
                        + "<forward><duration>240</duration></forward>"
                        + "<note><pitch/><duration>240</duration><staff>1</staff><notations>"
                        + "<glissando type=\"stop\" number=\"2\"/><slide type=\"start\" number=\"1\"/></notations></note>"
                        + "</measure>");
        java.util.List<MeiIo.MusicXmlStaffTimelineEntry> staff1 = MeiIo.collectStaffTimelineForMeiExport(
                doc.getDocumentElement(), 1, 480);
        java.util.List<MeiIo.MusicXmlStaffTimelineEntry> staff2 = MeiIo.collectStaffTimelineForMeiExport(
                doc.getDocumentElement(), 2, 480);

        assertEquals(2, staff1.size());
        assertEquals(0, staff1.get(0).getOnset());
        assertEquals(720, staff1.get(1).getOnset());
        assertEquals(1, staff2.size());
        assertEquals(240, staff2.get(0).getOnset());
        assertEquals(Arrays.asList("<slide tstamp=\"1\"/>", "<gliss tstamp=\"1\" tstamp2=\"2.5\"/>",
                "<slide tstamp=\"2.5\"/>"),
                MeiIo.collectMeiGlissSlideControlsForStaff(doc.getDocumentElement(), 1, 480, 4));
    }

    @Test
    public void collectsMeiTieSlurControlsForStaffFromMusicXmlTimeline() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><staff>1</staff>"
                        + "<tie type=\"start\"/><notations><slur type=\"start\" number=\"2\"/></notations></note>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><staff>1</staff>"
                        + "<tie type=\"stop\"/><notations><slur type=\"stop\" number=\"2\"/></notations></note>"
                        + "</measure>");
        org.w3c.dom.Element first = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("note").item(0);
        org.w3c.dom.Element second = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("note").item(1);
        Map<org.w3c.dom.Element, String> noteIds = new HashMap<org.w3c.dom.Element, String>();
        noteIds.put(first, "n1");
        noteIds.put(second, "n2");

        assertEquals("C:0:4:v1", MeiIo.tiePitchKeyFromMusicXmlNote(first));
        assertEquals(Arrays.asList("<slur startid=\"#n1\" endid=\"#n2\"/>",
                "<tie startid=\"#n1\" endid=\"#n2\"/>"),
                MeiIo.collectMeiTieSlurControlsForStaff(doc.getDocumentElement(), 1, 480, 4, noteIds, null));

        org.w3c.dom.Document startDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><note><pitch><step>D</step><alter>1</alter><octave>5</octave></pitch>"
                        + "<duration>480</duration><voice>2</voice><staff>1</staff><tie type=\"start\"/>"
                        + "<notations><slur type=\"start\"/></notations></note></measure>");
        org.w3c.dom.Document stopDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><note><pitch><step>D</step><alter>1</alter><octave>5</octave></pitch>"
                        + "<duration>480</duration><voice>2</voice><staff>1</staff><tie type=\"stop\"/>"
                        + "<notations><slur type=\"stop\"/></notations></note></measure>");
        MeiIo.MeiExportTieSlurCarryState carry = new MeiIo.MeiExportTieSlurCarryState();
        assertEquals(Collections.<String>emptyList(),
                MeiIo.collectMeiTieSlurControlsForStaff(startDoc.getDocumentElement(), 1, 480, 4, null, carry));
        assertEquals(Arrays.asList("<slur tstamp=\"1\" tstamp2=\"1\"/>", "<tie tstamp=\"1\" tstamp2=\"1\"/>"),
                MeiIo.collectMeiTieSlurControlsForStaff(stopDoc.getDocumentElement(), 1, 480, 4, null, carry));
        assertTrue(carry.getPendingSlurByNumber().isEmpty());
        assertTrue(carry.getPendingTieByPitch().isEmpty());
    }

    @Test
    public void collectsMeiOrnamentAndBreathControlsForStaffFromMusicXmlTimeline() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure>"
                        + "<note><pitch/><duration>480</duration><staff>1</staff><notations>"
                        + "<ornaments><trill-mark/><turn/><inverted-turn/><mordent/><inverted-mordent/></ornaments>"
                        + "<fermata type=\"inverted\"/><articulations><breath-mark/><caesura/></articulations>"
                        + "</notations></note>"
                        + "<note><rest/><duration>480</duration><staff>1</staff><notations><fermata/></notations></note>"
                        + "<note><pitch/><duration>480</duration><staff>2</staff><notations><ornaments><trill-mark/></ornaments></notations></note>"
                        + "</measure>");

        assertEquals(Arrays.asList("<trill tstamp=\"1\"/>", "<turn tstamp=\"1\" type=\"upper\"/>",
                "<turn tstamp=\"1\" type=\"inverted\"/>", "<mordent tstamp=\"1\" type=\"upper\"/>",
                "<mordent tstamp=\"1\" type=\"inverted\"/>", "<fermata tstamp=\"1\" place=\"below\"/>",
                "<breath tstamp=\"1\"/>", "<caesura tstamp=\"1\"/>"),
                MeiIo.collectMeiOrnamentAndBreathControlsForStaff(doc.getDocumentElement(), 1, 480, 4));
        assertEquals(Arrays.asList("<trill tstamp=\"3\"/>"),
                MeiIo.collectMeiOrnamentAndBreathControlsForStaff(doc.getDocumentElement(), 2, 480, 4));
    }

    @Test
    public void exportsMusicXmlDomToMeiScaffoldWithLayersAndControls() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise><work><work-title>Export Title</work-title></work>"
                        + "<part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>"
                        + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                        + "<key><fifths>1</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                        + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                        + "<direction placement=\"above\"><direction-type><dynamics><mf/></dynamics></direction-type><staff>1</staff></direction>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><type>quarter</type>"
                        + "<voice>1</voice><staff>1</staff><notations><slur type=\"start\" number=\"1\"/><ornaments><trill-mark/></ornaments></notations></note>"
                        + "<harmony><root><root-step>F</root-step></root><kind>dominant</kind><staff>1</staff></harmony>"
                        + "</measure><measure number=\"2\"><note><pitch><step>D</step><octave>4</octave></pitch>"
                        + "<duration>480</duration><type>quarter</type><voice>1</voice><staff>1</staff>"
                        + "<notations><slur type=\"stop\" number=\"1\"/></notations></note></measure></part></score-partwise>");

        String mei = MeiIo.exportMusicXmlDomToMei(doc, "5.1+test");

        assertTrue(mei.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><mei"));
        assertTrue(mei.contains("meiversion=\"5.1+test\""));
        assertTrue(mei.contains("<title>Export Title</title>"));
        assertTrue(mei.contains("<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"1s\">"));
        assertTrue(mei.contains("<measure n=\"1\"><staff n=\"1\">"));
        assertTrue(mei.contains("<layer n=\"1\"><note pname=\"c\" oct=\"4\" dur=\"4\" xml:id=\"mkN1\""));
        assertTrue(mei.contains("<dynam tstamp=\"1\" place=\"above\">mf</dynam>"));
        assertTrue(mei.contains("<trill tstamp=\"1\"/>"));
        assertTrue(mei.contains("<harm tstamp=\"1\">F7</harm>"));
        assertTrue(mei.contains("<slur startid=\"#mkN1\" endid=\"#mkN2\"/>"));
        assertTrue(mei.contains("<dynam staff=\"1\" tstamp=\"1\" place=\"above\">mf</dynam>"));
    }

    @Test
    public void exportsMusicXmlDomToMeiVersionAndTransposeParity() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise version=\"3.1\">"
                        + "<part-list><score-part id=\"P1\"><part-name>Clarinet in A</part-name></score-part></part-list>"
                        + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                        + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                        + "<transpose><diatonic>-2</diatonic><chromatic>-3</chromatic></transpose>"
                        + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                        + "<voice>1</voice><type>quarter</type></note></measure></part></score-partwise>");

        String mei = MeiIo.exportMusicXmlDomToMei(doc);
        String custom = MeiIo.exportMusicXmlDomToMei(doc, "4.0.1");

        assertTrue(mei.contains("meiversion=\"5.1+basic\""));
        assertTrue(custom.contains("meiversion=\"4.0.1\""));
        assertTrue(mei.contains("trans.diat=\"-2\""));
        assertTrue(mei.contains("trans.semi=\"-3\""));
    }

    @Test
    public void exportsMusicXmlDomToMeiTempoAndDynamicsParity() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<score-partwise version=\"3.1\">"
                        + "<part-list><score-part id=\"P1\"><part-name>P1</part-name></score-part></part-list>"
                        + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                        + "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"
                        + "<direction placement=\"above\"><direction-type><words>Allegretto moderato</words></direction-type>"
                        + "<sound tempo=\"116\"/></direction>"
                        + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                        + "<voice>1</voice><type>quarter</type></note>"
                        + "<direction placement=\"below\"><direction-type><dynamics><p/></dynamics></direction-type></direction>"
                        + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                        + "<voice>1</voice><type>quarter</type></note></measure></part></score-partwise>");

        String mei = MeiIo.exportMusicXmlDomToMei(doc);

        assertTrue(mei.contains(
                "<tempo staff=\"1\" tstamp=\"1\" midi.bpm=\"116\" place=\"above\">Allegretto moderato</tempo>"));
        assertFalse(mei.contains("<dynam tstamp=\"1\" place=\"above\">Allegretto moderato</dynam>"));
        assertTrue(mei.contains("<dynam staff=\"1\" tstamp=\"2\" place=\"below\">p</dynam>"));
    }

    @Test
    public void mapsMeiHarmonyTextAndDirectionHelpersForMusicXmlImport() {
        assertEquals(1, MeiIo.parseHarmonyAlter("#"));
        assertEquals(2, MeiIo.parseHarmonyAlter("x"));
        assertEquals(-1, MeiIo.parseHarmonyAlter("b"));
        assertEquals(0, MeiIo.parseHarmonyAlter(""));

        MeiIo.ParsedMeiHarmonyText parsed = MeiIo.parseMeiHarmText("Bbmaj7/D");
        assertEquals("B", parsed.getRootStep());
        assertEquals(-1, parsed.getRootAlter());
        assertEquals("major-seventh", parsed.getKind());
        assertEquals("", parsed.getKindText());
        assertEquals("D", parsed.getBassStep());
        assertNull(parsed.getBassAlter());
        assertTrue(parsed.getDegrees().isEmpty());

        MeiIo.ParsedMeiHarmonyText altered = MeiIo.parseMeiHarmText("Cadd#11");
        assertEquals("other", altered.getKind());
        assertEquals("add#11", altered.getKindText());
        assertEquals(1, altered.getDegrees().size());
        assertEquals(11, altered.getDegrees().get(0).getValue());
        assertEquals(1, altered.getDegrees().get(0).getAlter());
        assertNull(MeiIo.parseMeiHarmText("H7"));

        assertEquals("<transpose><diatonic>-1</diatonic><chromatic>2</chromatic></transpose>",
                MeiIo.buildTransposeXml(Integer.valueOf(2), Integer.valueOf(-1)));
        assertEquals("", MeiIo.buildTransposeXml(null, null));
        assertEquals("<time symbol=\"common\"><beats>4</beats><beat-type>4</beat-type></time>",
                MeiIo.buildTimeXml(4, 4, "common"));
        assertEquals("<time><beats>1</beats><beat-type>1</beat-type></time>",
                MeiIo.buildTimeXml(0, 0, "bad"));

        assertTrue(MeiIo.isDynamicsTag("mf"));
        assertFalse(MeiIo.isDynamicsTag("dolce"));
        assertEquals("<direction placement=\"below\"><direction-type><dynamics><mf/></dynamics></direction-type><offset>240</offset><voice>2</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiDynamValues(" mf ", "below", "1.5", 480, 4, "2", "1"));
        assertEquals("<direction><direction-type><words>dolce</words></direction-type><voice>1</voice><staff>2</staff></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiDynamValues("dolce", "", "1", 480, 4, "1", "2"));
        assertNull(MeiIo.buildMusicXmlDirectionFromMeiDynamValues(" ", "above", "1", 480, 4, "1", "1"));

        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<root><dynam place=\"below\" tstamp=\"1.5\">ff</dynam><dynam placement=\"left\">dolce</dynam></root>");
        assertEquals("<direction placement=\"below\"><direction-type><dynamics><ff/></dynamics></direction-type><offset>240</offset><voice>2</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiDynam(
                        (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("dynam").item(0),
                        480, 4, "2", "1"));
        assertEquals("<direction><direction-type><words>dolce</words></direction-type><voice>1</voice><staff>2</staff></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiDynam(
                        (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("dynam").item(1),
                        480, 4, "1", "2"));
    }

    @Test
    public void buildsMusicXmlControlDirectionsFromMeiValues() {
        java.util.List<MeiIo.ParsedMeiEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiEvent("note", 120),
                new MeiIo.ParsedMeiEvent("note", 360),
                new MeiIo.ParsedMeiEvent("note", 480));
        Map<String, Integer> idToIndex = new HashMap<String, Integer>();
        idToIndex.put("n1", Integer.valueOf(0));
        idToIndex.put("n3", Integer.valueOf(2));

        assertEquals("<direction placement=\"below\"><direction-type><wedge type=\"diminuendo\"/></direction-type><voice>1</voice><staff>1</staff></direction>"
                + "<direction placement=\"below\"><direction-type><wedge type=\"stop\"/></direction-type><offset>480</offset><voice>1</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionsFromMeiHairpinValues("dim", "below", "#n1", null, null, "#n3",
                        null, 480, 4, "1", "1", events, idToIndex, null));

        assertEquals("<direction placement=\"above\"><direction-type><pedal type=\"start\" number=\"1\" line=\"yes\"/></direction-type><voice>1</voice><staff>2</staff></direction>"
                + "<direction placement=\"above\"><direction-type><pedal type=\"stop\" number=\"1\" line=\"yes\"/></direction-type><offset>480</offset><voice>1</voice><staff>2</staff></direction>",
                MeiIo.buildMusicXmlDirectionsFromMeiPedalValues("", "above", "#n1", null, null, "#n3",
                        null, 480, 4, "1", "2", events, idToIndex, null));
        assertEquals("<direction><direction-type><pedal type=\"stop\" number=\"1\" line=\"yes\"/></direction-type><offset>120</offset><voice>1</voice><staff>2</staff></direction>",
                MeiIo.buildMusicXmlDirectionsFromMeiPedalValues("release", "", "#n2", null, "#n2", "",
                        null, 480, 4, "1", "2", events, Collections.<String, Integer>emptyMap(),
                        Collections.singletonMap("n2", Integer.valueOf(120))));

        assertEquals("<direction placement=\"above\"><direction-type><octave-shift type=\"down\" size=\"15\" number=\"1\"/></direction-type><voice>1</voice><staff>1</staff></direction>"
                + "<direction placement=\"above\"><direction-type><octave-shift type=\"stop\" size=\"15\" number=\"1\"/></direction-type><offset>480</offset><voice>1</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionsFromMeiOctaveValues("", "15", "below", "above", "#n1", null,
                        null, "#n3", null, 480, 4, "1", "1", events, idToIndex, null));

        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<root><hairpin form=\"dim\" place=\"below\" startid=\"#n1\" endid=\"#n3\"/>"
                        + "<pedal place=\"above\" startid=\"#n1\" endid=\"#n3\"/>"
                        + "<octave place=\"above\" dis=\"15\" dis.place=\"below\" startid=\"#n1\" endid=\"#n3\"/></root>");
        org.w3c.dom.Element root = doc.getDocumentElement();
        assertEquals("<direction placement=\"below\"><direction-type><wedge type=\"diminuendo\"/></direction-type><voice>1</voice><staff>1</staff></direction>"
                + "<direction placement=\"below\"><direction-type><wedge type=\"stop\"/></direction-type><offset>480</offset><voice>1</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionsFromMeiHairpin(
                        (org.w3c.dom.Element) root.getElementsByTagName("hairpin").item(0), 480, 4, "1", "1",
                        events, idToIndex, null));
        assertEquals("<direction placement=\"above\"><direction-type><pedal type=\"start\" number=\"1\" line=\"yes\"/></direction-type><voice>1</voice><staff>2</staff></direction>"
                + "<direction placement=\"above\"><direction-type><pedal type=\"stop\" number=\"1\" line=\"yes\"/></direction-type><offset>480</offset><voice>1</voice><staff>2</staff></direction>",
                MeiIo.buildMusicXmlDirectionsFromMeiPedal(
                        (org.w3c.dom.Element) root.getElementsByTagName("pedal").item(0), 480, 4, "1", "2",
                        events, idToIndex, null));
        assertEquals("<direction placement=\"above\"><direction-type><octave-shift type=\"down\" size=\"15\" number=\"1\"/></direction-type><voice>1</voice><staff>1</staff></direction>"
                + "<direction placement=\"above\"><direction-type><octave-shift type=\"stop\" size=\"15\" number=\"1\"/></direction-type><offset>480</offset><voice>1</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionsFromMeiOctave(
                        (org.w3c.dom.Element) root.getElementsByTagName("octave").item(0), 480, 4, "1", "1",
                        events, idToIndex, null));
    }

    @Test
    public void buildsMusicXmlRepeatTempoAndHarmonyFromMeiValues() {
        java.util.List<MeiIo.ParsedMeiEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiEvent("rest", 240),
                new MeiIo.ParsedMeiEvent("note", 240),
                new MeiIo.ParsedMeiEvent("note", 480));

        assertEquals("<direction placement=\"above\"><direction-type><segno/></direction-type><offset>240</offset><voice>1</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiRepeatMarkValues("segno", "above", "", "1.5", "",
                        480, 4, "1", "1", events, null, null));
        assertEquals("<direction><direction-type><words>D.C.</words></direction-type><offset>240</offset><voice>1</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiRepeatMarkValues("da capo", "", "", "1", "", 480,
                        4, "1", "1", events, null, null));
        assertNull(MeiIo.buildMusicXmlDirectionFromMeiRepeatMarkValues(" ", "above", "", "1", "", 480,
                4, "1", "1", events, null, null));

        assertEquals("<direction placement=\"above\"><direction-type><words>Allegro</words></direction-type><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>127</per-minute></metronome></direction-type><offset>240</offset><voice>1</voice><staff>1</staff><sound tempo=\"126.50\"/></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiTempoValues("Allegro", "126.5", "", false, "above",
                        "", "1.5", "", 480, 4, "1", "1", events, null, null));
        assertNull(MeiIo.buildMusicXmlDirectionFromMeiTempoValues("helper", "120", "infer-from-text", false,
                "above", "", "1", "", 480, 4, "1", "1", events, null, null));
        assertEquals("<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>120</per-minute></metronome></direction-type><offset>240</offset><voice>1</voice><staff>1</staff><sound tempo=\"120\"/></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiTempoValues("helper", "120", "infer-from-text", true,
                        "", "", "1", "", 480, 4, "1", "1", events, null, null));

        assertEquals("<harmony><root><root-step>C</root-step><root-alter>1</root-alter></root><kind text=\"add#11\">other</kind><degree><degree-value>11</degree-value><degree-alter>1</degree-alter><degree-type>add</degree-type></degree><offset>240</offset><staff>2</staff></harmony>",
                MeiIo.buildMusicXmlHarmonyFromMeiHarmValues("C#add#11", "", "", "", "1.5", "", 480,
                        4, "2", events, null, null));
        assertEquals("<harmony><kind text=\"invalid chord\">other</kind><staff>2</staff></harmony>",
                MeiIo.buildMusicXmlHarmonyFromMeiHarmValues("invalid chord", "", "", "", "1", "", 480, 4,
                        "2", events, null, null));

        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<root><repeatMark place=\"above\" tstamp=\"1.5\">segno</repeatMark>"
                        + "<tempo place=\"above\" tstamp=\"1.5\" midi.bpm=\"126.5\">Allegro</tempo>"
                        + "<harm tstamp=\"1.5\">C#add#11</harm><harm type=\"C7\"/></root>");
        org.w3c.dom.Element root = doc.getDocumentElement();
        assertEquals("<direction placement=\"above\"><direction-type><segno/></direction-type><offset>240</offset><voice>1</voice><staff>1</staff></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiRepeatMark(
                        (org.w3c.dom.Element) root.getElementsByTagName("repeatMark").item(0), 480, 4,
                        "1", "1", events, null, null));
        assertEquals("<direction placement=\"above\"><direction-type><words>Allegro</words></direction-type><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>127</per-minute></metronome></direction-type><offset>240</offset><voice>1</voice><staff>1</staff><sound tempo=\"126.50\"/></direction>",
                MeiIo.buildMusicXmlDirectionFromMeiTempo(
                        (org.w3c.dom.Element) root.getElementsByTagName("tempo").item(0), 480, 4, "1",
                        "1", events, null, null, false));
        assertEquals("<harmony><root><root-step>C</root-step><root-alter>1</root-alter></root><kind text=\"add#11\">other</kind><degree><degree-value>11</degree-value><degree-alter>1</degree-alter><degree-type>add</degree-type></degree><offset>240</offset><staff>2</staff></harmony>",
                MeiIo.buildMusicXmlHarmonyFromMeiHarm(
                        (org.w3c.dom.Element) root.getElementsByTagName("harm").item(0), 480, 4, "2",
                        events, null, null));
        assertEquals("<harmony><root><root-step>C</root-step></root><kind>dominant</kind><staff>2</staff></harmony>",
                MeiIo.buildMusicXmlHarmonyFromMeiHarm(
                        (org.w3c.dom.Element) root.getElementsByTagName("harm").item(1), 480, 4, "2",
                        events, null, null));
    }

    @Test
    public void collectsMeiLayerHarmonyAndDirectionXml() {
        java.util.List<MeiIo.ParsedMeiEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiEvent("note", 240),
                new MeiIo.ParsedMeiEvent("note", 240));
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><staff n=\"2\"><layer n=\"1\"><tempo tstamp=\"1\" midi.bpm=\"120\" type=\"infer-from-text\"/>"
                        + "<dynam tstamp=\"1.5\">mf</dynam><harm tstamp=\"1.5\">Dm</harm></layer>"
                        + "<layer n=\"2\"><dynam>pp</dynam></layer><harm>C7</harm></staff>"
                        + "<repeatMark staff=\"2\">fine</repeatMark><tempo staff=\"2\">Andante</tempo></measure>");
        org.w3c.dom.Element staff = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("staff").item(0);
        org.w3c.dom.Element firstLayer = (org.w3c.dom.Element) staff.getElementsByTagName("layer").item(0);
        org.w3c.dom.Element secondLayer = (org.w3c.dom.Element) staff.getElementsByTagName("layer").item(1);

        assertEquals("<harmony><root><root-step>D</root-step></root><kind>minor</kind><offset>240</offset><staff>2</staff></harmony>"
                + "<harmony><root><root-step>C</root-step></root><kind>dominant</kind><staff>2</staff></harmony>",
                MeiIo.collectLayerHarmonyXml(staff, firstLayer, 480, 4, events, null, null));

        assertEquals(1, MeiIo.collectControlEventsForLayer("repeatMark", staff, firstLayer, "2", "1", "1").size());
        assertEquals(0, MeiIo.collectControlEventsForLayer("repeatMark", staff, secondLayer, "2", "2", "1").size());

        assertEquals("<direction><direction-type><words>Andante</words></direction-type><voice>1</voice><staff>2</staff></direction>"
                + "<direction><direction-type><dynamics><mf/></dynamics></direction-type><offset>240</offset><voice>1</voice><staff>2</staff></direction>"
                + "<direction><direction-type><words>Fine</words></direction-type><voice>1</voice><staff>2</staff></direction>",
                MeiIo.collectLayerDirectionXml(staff, firstLayer, 480, 4, "1", events, null, null));
        assertEquals("<direction><direction-type><dynamics><pp/></dynamics></direction-type><voice>2</voice><staff>2</staff></direction>",
                MeiIo.collectLayerDirectionXml(staff, secondLayer, 480, 4, "2", events, null, null));
    }

    @Test
    public void appliesMeiControlNotationEventsToMusicXmlEventXml() {
        assertEquals(Arrays.asList("1", "2", "3"), MeiIo.parseMeiTargetList(" 1, 2  3 "));
        assertTrue(MeiIo.controlEventAppliesToLayerValues("1,2", "", "layer", "2", "1", "1"));
        assertFalse(MeiIo.controlEventAppliesToLayerValues("3", "", "layer", "2", "1", "1"));
        assertTrue(MeiIo.controlEventAppliesToLayerValues("", "2", "layer", "1", "2", "1"));
        assertFalse(MeiIo.controlEventAppliesToLayerValues("", "", "staff", "1", "2", "1"));
        assertTrue(MeiIo.controlEventAppliesToLayerValues("", "", "measure", "1", "1", "1"));

        java.util.List<MeiIo.ParsedMeiXmlEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>"),
                new MeiIo.ParsedMeiXmlEvent("rest", 120, "<note><rest/></note>"),
                new MeiIo.ParsedMeiXmlEvent("note", 240, "<note><pitch/></note>"));
        Map<String, Integer> idToIndex = new HashMap<String, Integer>();
        idToIndex.put("n1", Integer.valueOf(0));
        idToIndex.put("n3", Integer.valueOf(2));

        java.util.List<MeiIo.ParsedMeiXmlEvent> slurred = MeiIo.applyMeiSlurControlEvent(events, "#n1",
                null, null, "#n3", null, idToIndex, null, 480, 4, 2);
        assertEquals("<note><pitch/><notations><slur type=\"start\" number=\"2\"/></notations></note>",
                slurred.get(0).getXml());
        assertEquals("<note><pitch/><notations><slur type=\"stop\" number=\"2\"/></notations></note>",
                slurred.get(2).getXml());

        java.util.List<MeiIo.ParsedMeiXmlEvent> tied = MeiIo.applyMeiTieControlEvent(events, "#n1", null,
                null, "#n3", null, idToIndex, null, 480, 4);
        assertEquals("<note><pitch/><tie type=\"start\"/><notations><tied type=\"start\"/></notations></note>",
                tied.get(0).getXml());
        assertEquals("<note><pitch/><tie type=\"stop\"/><notations><tied type=\"stop\"/></notations></note>",
                tied.get(2).getXml());

        java.util.List<MeiIo.ParsedMeiXmlEvent> trilled = MeiIo.applyMeiSingleNotationControlEvent(events,
                "trill", false, "#n1", null, null, idToIndex, null, 480, 4);
        assertEquals("<note><pitch/><notations><ornaments><trill-mark/></ornaments></notations></note>",
                trilled.get(0).getXml());

        java.util.List<MeiIo.ParsedMeiXmlEvent> untouchedRest = MeiIo.applyMeiSingleNotationControlEvent(events,
                "fermata", true, "", "1.25", "", Collections.<String, Integer>emptyMap(), null, 480, 4);
        assertEquals("<note><rest/></note>", untouchedRest.get(1).getXml());

        java.util.List<MeiIo.ParsedMeiXmlEvent> breathed = MeiIo.applyMeiSingleNotationControlEvent(events,
                "breath", false, "#n3", null, null, idToIndex, null, 480, 4);
        assertEquals("<note><pitch/><notations><articulations><breath-mark/></articulations></notations></note>",
                breathed.get(2).getXml());
    }

    @Test
    public void appliesStaffMeiNotationControlsFromDom() {
        java.util.List<MeiIo.ParsedMeiXmlEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>"),
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>"),
                new MeiIo.ParsedMeiXmlEvent("note", 240, "<note><pitch/></note>"));
        Map<String, Integer> idToIndex = new HashMap<String, Integer>();
        idToIndex.put("n1", Integer.valueOf(0));
        idToIndex.put("n2", Integer.valueOf(1));
        idToIndex.put("n3", Integer.valueOf(2));
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><staff n=\"1\"><layer n=\"1\"><slur startid=\"#n1\" endid=\"#n3\"/>"
                        + "<fermata startid=\"#n1\" place=\"below\"/><turn startid=\"#n2\" form=\"inverted\"/>"
                        + "<mordent startid=\"#n2\" form=\"lower\"/><breath startid=\"#n3\"/></layer>"
                        + "<tie startid=\"#n1\" endid=\"#n3\"/><caesura startid=\"#n3\"/></staff>"
                        + "<trill startid=\"#n2\"/></measure>");
        org.w3c.dom.Element staff = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("staff")
                .item(0);
        org.w3c.dom.Element layer = (org.w3c.dom.Element) staff.getElementsByTagName("layer").item(0);

        java.util.List<MeiIo.ParsedMeiXmlEvent> applied = MeiIo.applyStaffSlurControlEvents(staff, layer, events,
                idToIndex, null, 480, 4);
        assertTrue(applied.get(0).getXml().contains("<slur type=\"start\" number=\"1\"/>"));
        assertTrue(applied.get(0).getXml().contains("<tie type=\"start\"/>"));
        assertTrue(applied.get(0).getXml().contains("<fermata type=\"inverted\"/>"));
        assertTrue(applied.get(1).getXml().contains("<trill-mark/>"));
        assertTrue(applied.get(1).getXml().contains("<inverted-turn/>"));
        assertTrue(applied.get(1).getXml().contains("<inverted-mordent/>"));
        assertTrue(applied.get(2).getXml().contains("<slur type=\"stop\" number=\"1\"/>"));
        assertTrue(applied.get(2).getXml().contains("<tie type=\"stop\"/>"));
        assertTrue(applied.get(2).getXml().contains("<breath-mark/>"));
        assertTrue(applied.get(2).getXml().contains("<caesura/>"));
    }

    @Test
    public void appliesMeiSpanControlEventsToMusicXmlEventXml() {
        java.util.List<MeiIo.ParsedMeiXmlEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>"),
                new MeiIo.ParsedMeiXmlEvent("rest", 120, "<note><rest/></note>"),
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>"),
                new MeiIo.ParsedMeiXmlEvent("note", 120, "<note><pitch/></note>"));
        Map<String, Integer> idToIndex = new HashMap<String, Integer>();
        idToIndex.put("n1", Integer.valueOf(0));
        idToIndex.put("r2", Integer.valueOf(1));
        idToIndex.put("n3", Integer.valueOf(2));
        idToIndex.put("n4", Integer.valueOf(3));

        java.util.List<MeiIo.ParsedMeiXmlEvent> beamed = MeiIo.applyMeiBeamSpanControlEvent(events, "#n1",
                null, "#n1 #r2 #n4", "#n4", null, idToIndex, null, 480, 4);
        assertEquals("<note><pitch/><beam number=\"1\">begin</beam></note>", beamed.get(0).getXml());
        assertEquals("<note><rest/></note>", beamed.get(1).getXml());
        assertEquals("<note><pitch/></note>", beamed.get(2).getXml());
        assertEquals("<note><pitch/><beam number=\"1\">end</beam></note>", beamed.get(3).getXml());

        java.util.List<MeiIo.ParsedMeiXmlEvent> tupleted = MeiIo.applyMeiTupletSpanControlEvent(events,
                "#n1", null, null, "#n4", null, idToIndex, null, 480, 4, 3);
        assertEquals("<note><pitch/><notations><tuplet type=\"start\" number=\"3\"/></notations></note>",
                tupleted.get(0).getXml());
        assertEquals("<note><pitch/><notations><tuplet type=\"stop\" number=\"3\"/></notations></note>",
                tupleted.get(3).getXml());

        java.util.List<MeiIo.ParsedMeiXmlEvent> glissed = MeiIo.applyMeiGlissControlEvent(events, "#n1",
                null, null, "#n4", null, idToIndex, null, 480, 4, 2);
        assertEquals("<note><pitch/><notations><glissando type=\"start\" number=\"2\"/></notations></note>",
                glissed.get(0).getXml());
        assertEquals("<note><pitch/><notations><glissando type=\"stop\" number=\"2\"/></notations></note>",
                glissed.get(3).getXml());

        java.util.List<MeiIo.ParsedMeiXmlEvent> slid = MeiIo.applyMeiSlideControlEvent(events, "#n1", null,
                null, "#n4", null, idToIndex, null, 480, 4, 4);
        assertEquals("<note><pitch/><notations><slide type=\"start\" number=\"4\"/></notations></note>",
                slid.get(0).getXml());
        assertEquals("<note><pitch/><notations><slide type=\"stop\" number=\"4\"/></notations></note>",
                slid.get(3).getXml());

        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<measure><staff n=\"1\"><layer n=\"1\"><beamSpan startid=\"#n1\" endid=\"#n4\" plist=\"#n1 #r2 #n4\"/>"
                        + "<tupletSpan startid=\"#n1\" endid=\"#n4\"/></layer><gliss startid=\"#n1\" endid=\"#n4\"/>"
                        + "<slide startid=\"#n1\" endid=\"#n4\"/></staff></measure>");
        org.w3c.dom.Element staff = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("staff")
                .item(0);
        org.w3c.dom.Element layer = (org.w3c.dom.Element) staff.getElementsByTagName("layer").item(0);
        java.util.List<MeiIo.ParsedMeiXmlEvent> applied = MeiIo.applyStaffSpanControlEvents(staff, layer, events,
                idToIndex, null, 480, 4);
        assertTrue(applied.get(0).getXml().contains("<beam number=\"1\">begin</beam>"));
        assertTrue(applied.get(0).getXml().contains("<tuplet type=\"start\" number=\"1\"/>"));
        assertTrue(applied.get(0).getXml().contains("<glissando type=\"start\" number=\"1\"/>"));
        assertTrue(applied.get(0).getXml().contains("<slide type=\"start\" number=\"1\"/>"));
        assertTrue(applied.get(3).getXml().contains("<beam number=\"1\">end</beam>"));
        assertTrue(applied.get(3).getXml().contains("<tuplet type=\"stop\" number=\"1\"/>"));
        assertTrue(applied.get(3).getXml().contains("<glissando type=\"stop\" number=\"1\"/>"));
        assertTrue(applied.get(3).getXml().contains("<slide type=\"stop\" number=\"1\"/>"));
    }

    @Test
    public void trimsMeiLayerEventsToMeasureCapacity() {
        java.util.List<MeiIo.ParsedMeiXmlEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiXmlEvent("note", 100, "a"),
                new MeiIo.ParsedMeiXmlEvent("note", 100, "b"),
                new MeiIo.ParsedMeiXmlEvent("note", 11, "minor-overflow"),
                new MeiIo.ParsedMeiXmlEvent("note", 100, "drop"));

        MeiIo.MeiLayerTrimResult result = MeiIo.trimLayerEventsToMeasureCapacity(events, 200);
        assertEquals(3, result.getEvents().size());
        assertEquals("minor-overflow", result.getEvents().get(2).getXml());
        assertEquals(211, result.getTotalTicks());
        assertEquals(1, result.getTrimmedCount());
        assertEquals(11, result.getTrimmedTicks());
        assertEquals(1, result.getDroppedCount());
        assertEquals(100, result.getDroppedTicks());

        MeiIo.MeiLayerTrimResult exact = MeiIo.trimLayerEventsToMeasureCapacity(events, 400);
        assertEquals(4, exact.getEvents().size());
        assertEquals(311, exact.getTotalTicks());
        assertEquals(0, exact.getDroppedCount());
        assertEquals(0, exact.getTrimmedCount());
    }

    @Test
    public void buildsMeiRawMiscFieldsAndParsesMeasureMeta() {
        java.util.List<MeiIo.MiscField> rawFields = MeiIo.buildMeiSourceRawMiscFields("a\\b\nc");
        assertEquals("mks:src:mei:raw-encoding", rawFields.get(0).getName());
        assertEquals("escape-v1", rawFields.get(0).getValue());
        assertEquals("5", rawFields.get(1).getValue());
        assertEquals("7", rawFields.get(2).getValue());
        assertEquals("1", rawFields.get(3).getValue());
        assertEquals("0", rawFields.get(4).getValue());
        assertEquals("mks:src:mei:raw-0001", rawFields.get(5).getName());
        assertEquals("a\\\\b\\nc", rawFields.get(5).getValue());
        assertTrue(MeiIo.buildMeiSourceRawMiscFields("").isEmpty());

        assertEquals("<miscellaneous><miscellaneous-field name=\"a&amp;b\">x&lt;y</miscellaneous-field></miscellaneous>",
                MeiIo.buildMusicXmlMiscellaneousXml(Arrays.asList(new MeiIo.MiscField("a&b", "x<y"))));
        assertEquals("", MeiIo.buildMusicXmlMiscellaneousXml(Collections.<MeiIo.MiscField>emptyList()));

        MeiIo.MeiMeasureMeta meta = MeiIo.parseMeiMeasureMetaText(
                "number=7;implicit=yes;repeat=backward;times=3;explicitTime=true;beats=6;beatType=8;doubleBar=both");
        assertEquals("7", meta.getNumber());
        assertEquals(Boolean.TRUE, meta.getImplicit());
        assertEquals("backward", meta.getRepeat());
        assertEquals(Integer.valueOf(3), meta.getTimes());
        assertEquals(Boolean.TRUE, meta.getExplicitTime());
        assertEquals(Integer.valueOf(6), meta.getBeats());
        assertEquals(Integer.valueOf(8), meta.getBeatType());
        assertEquals("both", meta.getDoubleBar());
        assertNull(MeiIo.parseMeiMeasureMetaText("bad;times=1;repeat=sideways"));

        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<staff n=\"2\"><annot type=\"musicxml-misc-field\" label=\"src:abc\">raw</annot>"
                        + "<annot type=\"musicxml-misc-field\" label=\"custom\">value</annot>"
                        + "<annot type=\"musicxml-measure-meta\">number=8;implicit=1;beats=3;beatType=4;doubleBar=right</annot>"
                        + "<layer n=\"1\"><note dur=\"4\" pname=\"c\" oct=\"4\" accid=\"s\"/>"
                        + "<rest dur=\"8\"/><chord dur=\"2\"><note/><note/></chord></layer></staff>");
        org.w3c.dom.Element staff = doc.getDocumentElement();
        java.util.List<MeiIo.MiscField> extracted = MeiIo.extractMiscFieldsFromMeiStaff(staff);
        assertEquals(2, extracted.size());
        assertEquals("mks:src:abc", extracted.get(0).getName());
        assertEquals("raw", extracted.get(0).getValue());
        assertEquals("mks:src:mei:custom", extracted.get(1).getName());
        assertEquals("value", extracted.get(1).getValue());

        MeiIo.MeiMeasureMeta staffMeta = MeiIo.parseMeasureMetaFromMeiStaff(staff);
        assertEquals("8", staffMeta.getNumber());
        assertEquals(Boolean.TRUE, staffMeta.getImplicit());
        assertEquals(Integer.valueOf(3), staffMeta.getBeats());
        assertEquals(Integer.valueOf(4), staffMeta.getBeatType());
        assertEquals("right", staffMeta.getDoubleBar());

        java.util.List<MeiIo.MiscField> debug = MeiIo.buildMeiDebugFieldsFromStaff(staff, "8", 480);
        assertEquals("mks:dbg:mei:notes:count", debug.get(0).getName());
        assertEquals("0x0003", debug.get(0).getValue());
        assertTrue(debug.get(1).getValue().contains("k=note"));
        assertTrue(debug.get(1).getValue().contains("pn=C"));
        assertTrue(debug.get(1).getValue().contains("ac=s"));
        assertTrue(debug.get(2).getValue().contains("k=rest"));
        assertTrue(debug.get(3).getValue().contains("k=chord"));
        assertTrue(debug.get(3).getValue().contains("cn=0x02"));
    }

    @Test
    public void buildsMeiMeasureDiagnosticsBodyAndBarlines() {
        java.util.List<MeiIo.MiscField> diag = MeiIo.buildMeiOverfullDiagnosticFields("12", "2", 720,
                480, 1, 240, 1, 12);
        assertEquals(2, diag.size());
        assertEquals("mks:diag:count", diag.get(0).getName());
        assertEquals("1", diag.get(0).getValue());
        assertEquals("mks:diag:0001", diag.get(1).getName());
        assertEquals("level=warn;code=OVERFULL_CLAMPED;fmt=mei;measure=12;staff=2;action=clamped;sourceTicks=720;capacityTicks=480;droppedEvents=1;droppedTicks=240;trimmedEvents=1;trimmedTicks=12",
                diag.get(1).getValue());
        assertTrue(MeiIo.buildMeiOverfullDiagnosticFields("1", "1", 480, 480, 0, 0, 0, 0).isEmpty());

        assertTrue(MeiIo.isLikelyPickupMeasure(false, 0, 240, 480));
        assertFalse(MeiIo.isLikelyPickupMeasure(true, 0, 240, 480));
        assertEquals(" implicit=\"yes\"", MeiIo.buildMeasureImplicitAttribute(false, 0, 240, 480));
        assertEquals("", MeiIo.buildMeasureImplicitAttribute(false, 1, 240, 480));

        assertEquals("v1<backup><duration>480</duration></backup>v2",
                MeiIo.buildMeiMeasureBodyXml(Arrays.asList(new MeiIo.MeiLayerXml("v1", 360),
                        new MeiIo.MeiLayerXml("v2", 120)), 480));
        assertEquals("long<backup><duration>600</duration></backup>second",
                MeiIo.buildMeiMeasureBodyXml(Arrays.asList(new MeiIo.MeiLayerXml("long", 600),
                        new MeiIo.MeiLayerXml("second", 120)), 480));
        assertEquals("", MeiIo.buildMeiMeasureBodyXml(Collections.<MeiIo.MeiLayerXml>emptyList(), 480));

        MeiIo.MeiMeasureMeta forward = MeiIo.parseMeiMeasureMetaText("repeat=forward;doubleBar=left");
        assertEquals("<barline location=\"left\"><bar-style>light-light</bar-style></barline><barline location=\"left\"><repeat direction=\"forward\"/></barline>",
                MeiIo.buildMeiMeasureLeftBarlineXml(forward));
        assertEquals("", MeiIo.buildMeiMeasureRightBarlineXml(forward));

        MeiIo.MeiMeasureMeta backward = MeiIo.parseMeiMeasureMetaText("repeat=backward;times=3;doubleBar=right");
        assertEquals("", MeiIo.buildMeiMeasureLeftBarlineXml(backward));
        assertEquals("<barline location=\"right\"><bar-style>light-heavy</bar-style><repeat direction=\"backward\"/><ending number=\"3\" type=\"stop\"/></barline><barline location=\"right\"><bar-style>light-light</bar-style></barline>",
                MeiIo.buildMeiMeasureRightBarlineXml(backward));
    }

    @Test
    public void buildsMeiMeasureAttributesXml() {
        String misc = "<miscellaneous><miscellaneous-field name=\"m\">v</miscellaneous-field></miscellaneous>";
        assertEquals("<attributes><divisions>480</divisions><key><fifths>-2</fifths></key><time symbol=\"cut\"><beats>2</beats><beat-type>2</beat-type></time><transpose><diatonic>-1</diatonic><chromatic>2</chromatic></transpose><clef><sign>F</sign><line>4</line></clef>"
                + misc + "</attributes>",
                MeiIo.buildMeiMeasureAttributesXml(false, false, false, false, false, 480, -2, 2, 2,
                        "cut", Integer.valueOf(2), Integer.valueOf(-1), "F", 4, misc));

        assertEquals("<attributes><key><fifths>3</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>",
                MeiIo.buildMeiMeasureAttributesXml(true, true, true, false, true, 480, 3, 3, 4, "",
                        null, null, "G", 2, ""));
        assertEquals("<attributes>" + misc + "</attributes>",
                MeiIo.buildMeiMeasureAttributesXml(true, false, false, false, false, 480, 0, 4, 4, "",
                        null, null, "G", 2, misc));
        assertEquals("", MeiIo.buildMeiMeasureAttributesXml(true, false, false, false, false, 480, 0, 4,
                4, "", null, null, "G", 2, ""));
    }

    @Test
    public void buildsMeiImportedMusicXmlWrappers() {
        assertEquals("<measure number=\"1&amp;a\" implicit=\"yes\"><attributes/><barline/>body<right/></measure>",
                MeiIo.buildMeiImportedMeasureXml("1&a", " implicit=\"yes\"", "<attributes/>",
                        "<barline/>", "body", "<right/>"));
        assertEquals("<measure number=\"2\"></measure>", MeiIo.buildMeiEmptyImportedMeasureXml("2"));
        assertEquals("<part id=\"P&amp;1\"><measure/></part>", MeiIo.buildMeiImportedPartXml("P&1",
                "<measure/>"));
        assertEquals("<score-part id=\"P1\"><part-name>A&amp;B</part-name></score-part>",
                MeiIo.buildMeiScorePartXml("P1", "A&B"));

        String partList = MeiIo.buildMeiScorePartXml("P1", "Piano");
        String part = MeiIo.buildMeiImportedPartXml("P1", "<measure number=\"1\"></measure>");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>T&lt;1</work-title></work><part-list>"
                + partList + "</part-list>" + part + "</score-partwise>",
                MeiIo.buildMeiScorePartwiseXmlDocument("T<1", partList, part));
    }

    @Test
    public void carriesTieAccidentalsAcrossMeiLayerEvents() {
        java.util.List<MeiIo.ParsedMeiXmlEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiXmlEvent("note", 120,
                        "<note><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><tie type=\"start\"/></note>"),
                new MeiIo.ParsedMeiXmlEvent("note", 120,
                        "<note><pitch><step>C</step><octave>4</octave></pitch><tie type=\"stop\"/></note>"),
                new MeiIo.ParsedMeiXmlEvent("note", 120,
                        "<note><pitch><step>C</step><octave>4</octave></pitch><tie type=\"stop\"/></note>"));

        MeiIo.MeiTieCarryResult result = MeiIo.applyTieCarryAccidentalsForLayerEvents(events,
                Collections.<String, Integer>emptyMap());
        assertEquals("<note><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><tie type=\"stop\"/></note>",
                result.getEvents().get(1).getXml());
        assertEquals("<note><pitch><step>C</step><octave>4</octave></pitch><tie type=\"stop\"/></note>",
                result.getEvents().get(2).getXml());
        assertTrue(result.getTieCarryOut().isEmpty());

        java.util.List<MeiIo.ParsedMeiXmlEvent> chordEvents = Arrays.asList(new MeiIo.ParsedMeiXmlEvent("chord",
                120,
                "<note><pitch><step>D</step><octave>5</octave></pitch><tie type=\"stop\"/></note><note><pitch><step>F</step><octave>5</octave></pitch></note>"));
        Map<String, Integer> carryIn = new HashMap<String, Integer>();
        carryIn.put("D:5", Integer.valueOf(-1));
        MeiIo.MeiTieCarryResult chordResult = MeiIo.applyTieCarryAccidentalsForLayerEvents(chordEvents, carryIn);
        assertEquals("<note><pitch><step>D</step><alter>-1</alter><octave>5</octave></pitch><tie type=\"stop\"/></note><note><pitch><step>F</step><octave>5</octave></pitch></note>",
                chordResult.getEvents().get(0).getXml());
        assertTrue(chordResult.getTieCarryOut().isEmpty());
    }

    @Test
    public void buildsMeiDebugFieldsFromEventValues() {
        assertEquals("idx=0x0001;m=1&amp;a;stf=2;ly=3;li=0x0004;k=note;du=8;dt=0x00F0;pn=C;oc=5;ac=s",
                MeiIo.buildMeiDebugEntryValue(1, "1&a", "2", "3", 4, "note", "8", 240, "c", "5",
                        "s", 0));
        assertEquals("idx=0x0002;m=1;stf=1;ly=1;li=0x0000;k=chord;du=4;dt=0x01E0;cn=0x03",
                MeiIo.buildMeiDebugEntryValue(2, "1", "", "", 0, "chord", "", 480, "", "", "", 3));

        java.util.List<MeiIo.MiscField> fields = MeiIo.buildMeiDebugFieldsFromEventValues(Arrays.asList(
                new MeiIo.MeiDebugEventValue("1", "1", "1", 0, "note", "4", 480, "d", "4", "", 0),
                new MeiIo.MeiDebugEventValue("1", "1", "1", 1, "chord", "8", 240, "", "", "", 2)));
        assertEquals(3, fields.size());
        assertEquals("mks:dbg:mei:notes:count", fields.get(0).getName());
        assertEquals("0x0002", fields.get(0).getValue());
        assertEquals("mks:dbg:mei:notes:0001", fields.get(1).getName());
        assertEquals("idx=0x0000;m=1;stf=1;ly=1;li=0x0000;k=note;du=4;dt=0x01E0;pn=D;oc=4",
                fields.get(1).getValue());
        assertEquals("mks:dbg:mei:notes:0002", fields.get(2).getName());
        assertEquals("idx=0x0001;m=1;stf=1;ly=1;li=0x0001;k=chord;du=8;dt=0x00F0;cn=0x02",
                fields.get(2).getValue());
        assertTrue(MeiIo.buildMeiDebugFieldsFromEntryValues(Collections.<String>emptyList()).isEmpty());
    }

    @Test
    public void selectsMeiImportRootAndBuildsPartList() {
        org.w3c.dom.Document corpus = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<meiCorpus><mei><music/></mei><mei><music><body><mdiv><score><section><measure><staff n=\"10\"/><staff n=\"2\"/></measure></section></score></mdiv></body></music><title>Main</title></mei></meiCorpus>");
        org.w3c.dom.Element selected = MeiIo.selectMeiImportRoot(corpus, null);
        assertEquals("Main", MeiIo.firstDescendantText(selected, "title"));
        assertEquals(Arrays.asList("2", "10"), MeiIo.collectSortedStaffNumbersFromMei(selected));

        MeiIo.ResolvedMeiImportOptions defaultOptions = MeiIo.resolveMeiImportOptions(null, null, null, null);
        assertTrue(defaultOptions.isDebugMetadata());
        assertTrue(defaultOptions.isSourceMetadata());
        assertFalse(defaultOptions.isFailOnOverfullDrop());
        assertNull(defaultOptions.getMeiCorpusIndex());

        MeiIo.ResolvedMeiImportOptions explicitOptions = MeiIo.resolveMeiImportOptions(Boolean.FALSE, Boolean.FALSE,
                Boolean.TRUE, Integer.valueOf(-2));
        assertFalse(explicitOptions.isDebugMetadata());
        assertFalse(explicitOptions.isSourceMetadata());
        assertTrue(explicitOptions.isFailOnOverfullDrop());
        assertEquals(Integer.valueOf(0), explicitOptions.getMeiCorpusIndex());

        org.w3c.dom.Element first = MeiIo.selectMeiImportRoot(corpus, Integer.valueOf(0));
        assertEquals("", MeiIo.firstDescendantText(first, "title"));

        org.w3c.dom.Document contextDoc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<mei><music><body><mdiv><score><title>Ctx</title>"
                        + "<scoreDef meter.count=\"3\" meter.unit=\"8\" meter.sym=\"common\" key.sig=\"2s\" trans.semi=\"1\">"
                        + "<staffDef n=\"1\" label=\"Flute\" clef.shape=\"G\" clef.line=\"2\" meter.count=\"6\" meter.unit=\"8\" meter.sym=\"cut\" trans.semi=\"-2\"/>"
                        + "</scoreDef><section><measure n=\"1\"><staff n=\"1\"><layer/></staff></measure></section>"
                        + "</score></mdiv></body></music></mei>");
        MeiIo.MeiInitialImportContext context = MeiIo.buildMeiInitialImportContext(contextDoc.getDocumentElement());
        assertEquals("Ctx", context.getTitle());
        assertEquals(1, context.getScoreDefs().size());
        assertEquals(1, context.getStaffDefs().size());
        assertEquals(3, context.getMeterCount());
        assertEquals(8, context.getMeterUnit());
        assertEquals(2, context.getFifths());
        assertEquals(480, context.getDivisions());
        assertEquals(1, context.getMeasureNodes().size());
        assertEquals(Arrays.asList("1"), context.getStaffNumbers());
        assertEquals("Flute", context.getStaffMeta().get("1").getLabel());
        assertEquals("G", context.getStaffMeta().get("1").getClefSign());
        assertEquals(2, context.getStaffMeta().get("1").getClefLine());

        MeiIo.MeiPartImportState partState = MeiIo.buildMeiInitialPartImportState(context, "1", 0);
        assertEquals("1", partState.getStaffNo());
        assertEquals("P1", partState.getPartId());
        assertEquals("Flute", partState.getLabel());
        assertEquals(6, partState.getCurrentBeats());
        assertEquals(8, partState.getCurrentBeatType());
        assertEquals("cut", partState.getCurrentTimeSymbol());
        assertEquals(2, partState.getCurrentFifths());
        assertEquals("G", partState.getCurrentClefSign());
        assertEquals(2, partState.getCurrentClefLine());
        assertEquals(Integer.valueOf(-2), partState.getCurrentTranspose().getChromatic());
        assertNull(partState.getCurrentTranspose().getDiatonic());
        assertFalse(partState.hasEmittedInitialAttributes());

        MeiIo.MeiPartImportState fallbackPartState = MeiIo.buildMeiInitialPartImportState(context, "9", 2);
        assertEquals("9", fallbackPartState.getStaffNo());
        assertEquals("P3", fallbackPartState.getPartId());
        assertEquals("Staff 9", fallbackPartState.getLabel());
        assertEquals("G", fallbackPartState.getCurrentClefSign());
        assertEquals(2, fallbackPartState.getCurrentClefLine());

        Map<String, String> labels = new HashMap<String, String>();
        labels.put("2", "Violin & Viola");
        assertEquals("<score-part id=\"P1\"><part-name>Violin &amp; Viola</part-name></score-part><score-part id=\"P2\"><part-name>Staff 10</part-name></score-part>",
                MeiIo.buildMeiPartListXml(Arrays.asList("2", "10"), labels));

        org.w3c.dom.Document badRoot = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument("<score/>");
        try {
            MeiIo.selectMeiImportRoot(badRoot, null);
            org.junit.jupiter.api.Assertions.fail("expected invalid MEI root");
        } catch (IllegalArgumentException ex) {
            assertEquals("MEI root must be <mei> or <meiCorpus>.", ex.getMessage());
        }

        org.w3c.dom.Document noStaff = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<mei><music><measure/></music></mei>");
        try {
            MeiIo.collectSortedStaffNumbersFromMei(noStaff.getDocumentElement());
            org.junit.jupiter.api.Assertions.fail("expected missing staff");
        } catch (IllegalArgumentException ex) {
            assertEquals("MEI has no <staff> content.", ex.getMessage());
        }
    }

    @Test
    public void parsesMeiStaffDefMetadata() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<root><staffDef n=\"1\" label=\"Piano\" clef.shape=\"F\" clef.line=\"4\"/>"
                        + "<staffDef n=\"2\"><label>Violin</label><clef shape=\"G\" line=\"2\"/></staffDef>"
                        + "<staffDef n=\"3\"><labelAbbr>Vc.</labelAbbr></staffDef>"
                        + "<staffDef n=\"1\"><clef shape=\"C\" line=\"3\"/></staffDef></root>");
        org.w3c.dom.NodeList nodes = doc.getDocumentElement().getElementsByTagName("staffDef");

        MeiIo.MeiClef firstClef = MeiIo.parseClefFromStaffDefElement((org.w3c.dom.Element) nodes.item(0));
        assertEquals("F", firstClef.getClefSign());
        assertEquals(4, firstClef.getClefLine());
        assertEquals("Piano", MeiIo.parseStaffLabelFromStaffDefElement((org.w3c.dom.Element) nodes.item(0)));

        MeiIo.MeiClef childClef = MeiIo.parseClefFromStaffDefElement((org.w3c.dom.Element) nodes.item(1));
        assertEquals("G", childClef.getClefSign());
        assertEquals(2, childClef.getClefLine());
        assertEquals("Violin", MeiIo.parseStaffLabelFromStaffDefElement((org.w3c.dom.Element) nodes.item(1)));
        assertEquals("Vc.", MeiIo.parseStaffLabelFromStaffDefElement((org.w3c.dom.Element) nodes.item(2)));

        java.util.List<org.w3c.dom.Element> staffDefs = new java.util.ArrayList<org.w3c.dom.Element>();
        for (int index = 0; index < nodes.getLength(); index++) {
            staffDefs.add((org.w3c.dom.Element) nodes.item(index));
        }
        Map<String, MeiIo.MeiStaffMeta> meta = MeiIo.collectStaffMetaFromStaffDefs(staffDefs);
        assertEquals("Piano", meta.get("1").getLabel());
        assertEquals("C", meta.get("1").getClefSign());
        assertEquals(3, meta.get("1").getClefLine());
        assertEquals("Violin", meta.get("2").getLabel());
        assertEquals("G", meta.get("2").getClefSign());
        assertEquals("Vc.", meta.get("3").getLabel());
        assertEquals("G", meta.get("3").getClefSign());
        assertEquals(2, meta.get("3").getClefLine());
    }

    @Test
    public void mapsMeiImportDurationAccidentalAndKeyHelpers() {
        assertEquals("whole", MeiIo.meiDurToMusicXmlType("1"));
        assertEquals("16th", MeiIo.meiDurToMusicXmlType("16"));
        assertEquals("quarter", MeiIo.meiDurToMusicXmlType("bad"));
        assertEquals(Double.valueOf(8.0d), Double.valueOf(MeiIo.meiDurToQuarterLength("breve")));
        assertEquals(Double.valueOf(0.5d), Double.valueOf(MeiIo.meiDurToQuarterLength("8")));
        assertEquals(2, MeiIo.meiDurToBeamDepth("16"));
        assertEquals(0, MeiIo.meiDurToBeamDepth("4"));
        assertEquals(Double.valueOf(1.75d), Double.valueOf(MeiIo.dotsMultiplier(2)));

        MeiIo.MeiDurDots dotted = MeiIo.inferMeiDurAndDotsFromTicks(720, 480);
        assertEquals("4", dotted.getDur());
        assertEquals(1, dotted.getDots());

        assertEquals(Integer.valueOf(1), MeiIo.accidToAlter("s"));
        assertEquals(Integer.valueOf(2), MeiIo.accidToAlter("x"));
        assertEquals(Integer.valueOf(-1), MeiIo.accidToAlter("b"));
        assertNull(MeiIo.accidToAlter(""));
        assertEquals("double-sharp", MeiIo.accidToMusicXmlAccidental("ss"));
        assertEquals("flat-flat", MeiIo.accidToMusicXmlAccidental("bb"));
        assertEquals("<alter>-1</alter>", MeiIo.accidToPitchAlterXml("f"));
        assertEquals("", MeiIo.accidToPitchAlterXml("n"));

        assertEquals(1, MeiIo.impliedAlterFromFifths("F", 1));
        assertEquals(-1, MeiIo.impliedAlterFromFifths("B", -2));
        assertEquals(0, MeiIo.impliedAlterFromFifths("H", 7));
        assertEquals(3, MeiIo.parseMeiKeySigToFifths("3s"));
        assertEquals(-4, MeiIo.parseMeiKeySigToFifths("4f"));
        assertEquals(7, MeiIo.parseMeiKeySigToFifths("12s"));
        assertEquals(-2, MeiIo.parseMeiKeyAccidToAlter("bb"));
        assertEquals(1, MeiIo.parseMeiKeyAccidToAlter("#"));
    }

    @Test
    public void mapsMeiKeyInferenceDurationMetadataAndSpanFlags() {
        assertEquals(Integer.valueOf(7), MeiIo.tonicToFifths("C", "#", "major"));
        assertEquals(Integer.valueOf(-5), MeiIo.tonicToFifths("B", "b", "minor"));
        assertNull(MeiIo.tonicToFifths("H", "", "major"));
        assertEquals(Integer.valueOf(-3), MeiIo.parseMeiKeyFifthsFromValues("3f", "C", "", "major"));
        assertEquals(Integer.valueOf(4), MeiIo.parseMeiKeyFifthsFromValues("", "C", "#", "minor"));

        assertEquals("0x0A", MeiIo.toHex(10));
        assertEquals("0x000F", MeiIo.toHex(15, 4));
        assertEquals(360, MeiIo.resolveDurTicksFromMetadata(Integer.valueOf(360), Integer.valueOf(10),
                Integer.valueOf(20), 480, 480));
        assertEquals(960, MeiIo.resolveDurTicksFromMetadata(null, Integer.valueOf(480), Integer.valueOf(240),
                120, 480));
        assertEquals(120, MeiIo.resolveDurTicksFromMetadata(null, Integer.valueOf(480), null, 120, 480));

        MeiIo.TieFlags middle = MeiIo.parseMeiTieFlags("m");
        assertTrue(middle.isStart());
        assertTrue(middle.isStop());
        MeiIo.TieFlags start = MeiIo.parseMeiTieFlags("i");
        assertTrue(start.isStart());
        assertFalse(start.isStop());

        java.util.List<MeiIo.MeiSlurNotation> slurs = MeiIo.parseMeiSlurNotations("i2 3t m");
        assertEquals(4, slurs.size());
        assertEquals("start", slurs.get(0).getType());
        assertEquals(2, slurs.get(0).getNumber());
        assertEquals("stop", slurs.get(1).getType());
        assertEquals(3, slurs.get(1).getNumber());
        assertEquals("start", slurs.get(2).getType());
        assertEquals("stop", slurs.get(3).getType());
    }

    @Test
    public void addsMusicXmlNotationFragmentsToFirstNote() {
        String note = "<note><pitch/><duration>480</duration></note>";
        assertEquals("<note><pitch/><notations><slur type=\"start\" number=\"2\"/></notations></note>",
                MeiIo.addSlurNotationToSingleNoteXml("<note><pitch/></note>", "start", 2));
        assertEquals("<note><pitch/><tie type=\"start\"/><duration>480</duration><notations><tied type=\"start\"/></notations></note>",
                MeiIo.addTieToSingleNoteXml(note, "start"));
        assertEquals("<note><pitch/><notations><ornaments><trill-mark/></ornaments></notations></note>",
                MeiIo.addOrnamentXmlToSingleNoteXml("<note><pitch/></note>", "<trill-mark/>"));
        assertEquals("<note><pitch/><notations><articulations><breath-mark/></articulations></notations></note>",
                MeiIo.addArticulationXmlToSingleNoteXml("<note><pitch/></note>", "<breath-mark/>"));
        assertEquals("<note><pitch/><beam number=\"1\">begin</beam></note>",
                MeiIo.addBeamToSingleNoteXml("<note><pitch/></note>", "begin", 1));
        assertEquals("<note><pitch/><beam number=\"1\">begin</beam></note>",
                MeiIo.addBeamToSingleNoteXml("<note><pitch/><beam number=\"1\">begin</beam></note>", "end", 1));
    }

    @Test
    public void addsMusicXmlNotationFragmentsToFirstEventNote() {
        String event = "<backup/><note><pitch/></note><note><pitch/></note>";
        assertEquals("<backup/><note><pitch/><notations><ornaments><trill-mark/></ornaments></notations></note><note><pitch/></note>",
                MeiIo.addTrillNotationToEventXml(event));
        assertEquals("<backup/><note><pitch/><notations><fermata type=\"inverted\"/></notations></note><note><pitch/></note>",
                MeiIo.addFermataNotationToEventXml(event, true));
        assertEquals("<backup/><note><pitch/><notations><glissando type=\"start\" number=\"3\"/></notations></note><note><pitch/></note>",
                MeiIo.addGlissNotationToEventXml(event, "start", 3));
        assertEquals("<backup/><note><pitch/><notations><slide type=\"stop\" number=\"1\"/></notations></note><note><pitch/></note>",
                MeiIo.addSlideNotationToEventXml(event, "stop", 1));
        assertEquals("<backup/><note><pitch/><notations><ornaments><inverted-turn/></ornaments></notations></note><note><pitch/></note>",
                MeiIo.addTurnNotationToEventXml(event, true));
        assertEquals("<backup/><note><pitch/><notations><ornaments><mordent/></ornaments></notations></note><note><pitch/></note>",
                MeiIo.addMordentNotationToEventXml(event, false));
        assertEquals("<backup/><note><pitch/><notations><articulations><caesura/></articulations></notations></note><note><pitch/></note>",
                MeiIo.addCaesuraNotationToEventXml(event));
        assertEquals("<backup/><note><pitch/><notations><tuplet type=\"stop\" number=\"4\"/></notations></note><note><pitch/></note>",
                MeiIo.addTupletNotationToEventXml(event, "stop", 4));
        assertEquals("no-note", MeiIo.addBreathNotationToEventXml("no-note"));
    }

    @Test
    public void resolvesMeiControlEndpointHelpers() {
        assertEquals(Integer.valueOf(240), MeiIo.parseMeiTstampToTicks("1.5", 480, 4));
        assertEquals(Integer.valueOf(480), MeiIo.parseMeiTstampToTicks("2", 480, 4));
        assertNull(MeiIo.parseMeiTstampToTicks("0.5", 480, 4));
        assertNull(MeiIo.parseMeiTstampToTicks("bad", 480, 4));

        java.util.List<MeiIo.ParsedMeiEvent> events = Arrays.asList(
                new MeiIo.ParsedMeiEvent("rest", 120),
                new MeiIo.ParsedMeiEvent("note", 120),
                new MeiIo.ParsedMeiEvent("chord", 240));
        assertEquals(Integer.valueOf(1), MeiIo.resolveEventIndexByTstamp(events, 0));
        assertEquals(Integer.valueOf(2), MeiIo.resolveEventIndexByTstamp(events, 240));
        assertEquals(Integer.valueOf(2), MeiIo.resolveEventIndexByTstamp(events, 999));
        assertEquals(Integer.valueOf(240), MeiIo.resolveEventStartTickByIndex(events, 2));
        assertNull(MeiIo.resolveEventStartTickByIndex(events, 3));

        Map<String, Integer> idToIndex = new HashMap<String, Integer>();
        idToIndex.put("n1", Integer.valueOf(1));
        assertEquals(Integer.valueOf(1), MeiIo.resolveControlEventEndpointIndex("#n1", null, idToIndex, events,
                480, 4, null, null));

        Map<String, Integer> idToTick = new HashMap<String, Integer>();
        idToTick.put("late", Integer.valueOf(240));
        assertEquals(Integer.valueOf(2), MeiIo.resolveControlEventEndpointIndex("#missing", null,
                Collections.<String, Integer>emptyMap(), events, 480, 4, "#late", idToTick));
        assertEquals(Integer.valueOf(2), MeiIo.resolveControlEventEndpointIndex("", "1.5",
                Collections.<String, Integer>emptyMap(), events, 480, 4, "", null));
    }

    @Test
    public void resolvesMeiScoreDefStaffDefAndMeterHelpers() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<mei><music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\" meter.sym=\"common\" key.sig=\"2s\" trans.diat=\"1\" trans.semi=\"2\">"
                        + "<staffDef n=\"1\" meter.count=\"3\" meter.unit=\"8\" clef.shape=\"F\" clef.line=\"4\" trans.semi=\"-2\" key.pname=\"F\" key.mode=\"major\"/>"
                        + "<staffDef meter.sym=\"cut\"><clef shape=\"G\" line=\"2\"/></staffDef></scoreDef>"
                        + "<section><measure n=\"1\"><staff n=\"1\"/></measure>"
                        + "<scoreDef meter.count=\"6\" meter.unit=\"8\" keysig=\"3f\"><staffDef n=\"2\" meter.count=\"2\" meter.unit=\"2\" clef.shape=\"C\" clef.line=\"3\" trans.diat=\"-1\" trans.semi=\"-2\"/></scoreDef>"
                        + "<measure n=\"2\"><staff n=\"2\"/></measure></section></score></mdiv></body></music></mei>");
        org.w3c.dom.Element root = doc.getDocumentElement();
        java.util.List<org.w3c.dom.Element> scoreDefs = MeiIo.collectScoreDefsInDocOrder(root);
        java.util.List<org.w3c.dom.Element> staffDefs = MeiIo.collectStaffDefsInDocOrder(root);
        assertEquals(2, scoreDefs.size());
        assertEquals(3, staffDefs.size());

        org.w3c.dom.Element firstMeasure = (org.w3c.dom.Element) root.getElementsByTagName("measure").item(0);
        org.w3c.dom.Element secondMeasure = (org.w3c.dom.Element) root.getElementsByTagName("measure").item(1);
        assertEquals(scoreDefs.get(0), MeiIo.findEffectiveScoreDefForNode(firstMeasure, scoreDefs));
        assertEquals(scoreDefs.get(1), MeiIo.findEffectiveScoreDefForNode(secondMeasure, scoreDefs));
        assertEquals(staffDefs.get(1), MeiIo.findEffectiveStaffDefForNode(firstMeasure, "1", staffDefs));
        assertEquals(staffDefs.get(2), MeiIo.findEffectiveStaffDefForNode(secondMeasure, "2", staffDefs));

        MeiIo.MeiTranspose transpose = MeiIo.parseTransposeFromStaffDefElement(staffDefs.get(0));
        assertEquals(Integer.valueOf(-2), transpose.getChromatic());
        assertNull(transpose.getDiatonic());
        assertEquals("2s", MeiIo.readMeiKeySigAttr(scoreDefs.get(0)));
        assertEquals(Integer.valueOf(-1), MeiIo.parseMeiKeyFifthsFromElement(staffDefs.get(0)));
        assertEquals(2, MeiIo.parseKeySigFromScoreDefForStaff(scoreDefs.get(0), "9", 5));
        MeiIo.MeiTranspose scoreTranspose = MeiIo.parseTransposeFromScoreDefForStaff(scoreDefs.get(0), "9");
        assertEquals(Integer.valueOf(2), scoreTranspose.getChromatic());
        assertEquals(Integer.valueOf(1), scoreTranspose.getDiatonic());
        MeiIo.MeiTranspose staffTranspose = MeiIo.parseTransposeFromScoreDefForStaff(scoreDefs.get(1), "2");
        assertEquals(Integer.valueOf(-2), staffTranspose.getChromatic());
        assertEquals(Integer.valueOf(-1), staffTranspose.getDiatonic());
        assertEquals(-3, MeiIo.parseKeySigFromScoreDefForStaff(scoreDefs.get(1), "2", 5));

        assertEquals("cut", MeiIo.parseTimeSymbolFromScoreDefForStaff(scoreDefs.get(0), "9"));
        assertEquals("common", MeiIo.parseTimeSymbolFromScoreDefForStaff(scoreDefs.get(0), "1"));
        MeiIo.MeiMeter staffMeter = MeiIo.parseMeterFromScoreDefForStaff(scoreDefs.get(0), "1", 4, 4);
        assertEquals(3, staffMeter.getBeats());
        assertEquals(8, staffMeter.getBeatType());
        MeiIo.MeiMeter scoreMeter = MeiIo.parseMeterFromScoreDefForStaff(scoreDefs.get(1), "9", 4, 4);
        assertEquals(6, scoreMeter.getBeats());
        assertEquals(8, scoreMeter.getBeatType());

        MeiIo.MeiClef clef = MeiIo.parseClefFromScoreDefForStaff(scoreDefs.get(1), "2");
        assertEquals("C", clef.getClefSign());
        assertEquals(3, clef.getClefLine());
    }

    @Test
    public void resolvesMeiMeasureImportState() {
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(
                "<mei><music><body><mdiv><score><title>Measure State</title>"
                        + "<scoreDef meter.count=\"3\" meter.unit=\"8\" meter.sym=\"common\" key.sig=\"2s\" trans.semi=\"1\">"
                        + "<staffDef n=\"1\" clef.shape=\"G\" clef.line=\"2\"/></scoreDef>"
                        + "<section><measure n=\"1\"><staff n=\"1\"><annot type=\"musicxml-measure-meta\">"
                        + "number=A;implicit=true;beats=2;beatType=4;explicitTime=true</annot></staff></measure>"
                        + "<scoreDef meter.count=\"6\" meter.unit=\"8\" key.sig=\"1f\">"
                        + "<staffDef n=\"1\" meter.sym=\"cut\" clef.shape=\"F\" clef.line=\"4\" trans.semi=\"-2\"/>"
                        + "</scoreDef><measure n=\"2\"><staff n=\"1\"/></measure>"
                        + "<measure n=\"3\"><staff n=\"2\"/></measure></section></score></mdiv></body></music></mei>");
        MeiIo.MeiInitialImportContext context = MeiIo.buildMeiInitialImportContext(doc.getDocumentElement());
        MeiIo.MeiPartImportState initial = MeiIo.buildMeiInitialPartImportState(context, "1", 0);

        MeiIo.MeiMeasureImportState first = MeiIo.buildMeiMeasureImportState(context, initial,
                context.getMeasureNodes().get(0), 0);
        assertTrue(first.hasTargetStaff());
        assertEquals("1", first.getSourceMeasureNo());
        assertEquals("A", first.getMeasureNo());
        assertTrue(first.isImplicitFromMeta());
        assertEquals(2, first.getMeasureBeats());
        assertEquals(4, first.getMeasureBeatType());
        assertEquals("common", first.getMeasureTimeSymbol());
        assertEquals(2, first.getMeasureFifths());
        assertEquals("G", first.getMeasureClefSign());
        assertEquals(2, first.getMeasureClefLine());
        assertEquals(Integer.valueOf(1), first.getMeasureTranspose().getChromatic());
        assertEquals(960, first.getMeasureTicks());
        assertTrue(first.shouldEmitTime());
        assertTrue(first.shouldEmitKey());
        assertTrue(first.shouldEmitTranspose());
        assertTrue(first.shouldEmitClef());

        MeiIo.MeiMeasureImportState second = MeiIo.buildMeiMeasureImportState(context, first.toNextPartImportState(),
                context.getMeasureNodes().get(1), 1);
        assertTrue(second.hasTargetStaff());
        assertEquals("2", second.getMeasureNo());
        assertFalse(second.isImplicitFromMeta());
        assertEquals(6, second.getMeasureBeats());
        assertEquals(8, second.getMeasureBeatType());
        assertEquals("cut", second.getMeasureTimeSymbol());
        assertEquals(-1, second.getMeasureFifths());
        assertEquals("F", second.getMeasureClefSign());
        assertEquals(4, second.getMeasureClefLine());
        assertEquals(Integer.valueOf(-2), second.getMeasureTranspose().getChromatic());
        assertEquals(1440, second.getMeasureTicks());
        assertTrue(second.shouldEmitTime());
        assertTrue(second.shouldEmitKey());
        assertTrue(second.shouldEmitTranspose());
        assertTrue(second.shouldEmitClef());
        assertTrue(second.toNextPartImportState().hasEmittedInitialAttributes());

        MeiIo.MeiMeasureImportState missing = MeiIo.buildMeiMeasureImportState(context,
                second.toNextPartImportState(), context.getMeasureNodes().get(2), 2);
        assertFalse(missing.hasTargetStaff());
        assertEquals("3", missing.getMeasureNo());
    }

    @Test
    public void resolvesStaffLevelHairpinIdsAcrossLayersByTickOnImport() {
        String mei = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.1\"><music><body><mdiv><score>"
                + "<scoreDef meter.count=\"4\" meter.unit=\"4\" key.sig=\"0\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\">"
                + "<hairpin form=\"cres\" startid=\"#n1\" endid=\"#n4\"/>"
                + "<layer n=\"1\"><note xml:id=\"n1\" pname=\"c\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n2\" pname=\"d\" oct=\"4\" dur=\"4\"/></layer>"
                + "<layer n=\"2\"><note xml:id=\"n3\" pname=\"e\" oct=\"4\" dur=\"4\"/>"
                + "<note xml:id=\"n4\" pname=\"f\" oct=\"4\" dur=\"4\"/></layer>"
                + "</staff></measure></section></score></mdiv></body></music></mei>";

        String xml = MeiIo.convertMeiToMusicXml(mei);
        org.w3c.dom.Document doc = jp.igapyon.mikuscore.musicxml.MusicXmlIo.parseMusicXmlDocument(xml);
        org.w3c.dom.Element start = null;
        org.w3c.dom.Element stop = null;
        org.w3c.dom.NodeList directions = doc.getElementsByTagName("direction");
        for (int i = 0; i < directions.getLength(); i++) {
            org.w3c.dom.Element direction = (org.w3c.dom.Element) directions.item(i);
            org.w3c.dom.NodeList wedges = direction.getElementsByTagName("wedge");
            for (int j = 0; j < wedges.getLength(); j++) {
                org.w3c.dom.Element wedge = (org.w3c.dom.Element) wedges.item(j);
                if ("crescendo".equals(wedge.getAttribute("type"))) {
                    start = direction;
                }
                if ("stop".equals(wedge.getAttribute("type"))) {
                    stop = direction;
                }
            }
        }

        assertTrue(start != null);
        assertTrue(stop != null);
        assertEquals("480", stop.getElementsByTagName("offset").item(0).getTextContent().trim());
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while (text != null && pattern != null && pattern.length() > 0) {
            int found = text.indexOf(pattern, index);
            if (found < 0) {
                return count;
            }
            count++;
            index = found + pattern.length();
        }
        return count;
    }

    private static org.w3c.dom.Element firstNoteOfMeasure(org.w3c.dom.Document doc, String measureNumber) {
        return (org.w3c.dom.Element) findMeasureByNumber(doc, measureNumber).getElementsByTagName("note").item(0);
    }

    private static org.w3c.dom.Element findMeasureByNumber(org.w3c.dom.Document doc, String measureNumber) {
        org.w3c.dom.NodeList measures = doc.getElementsByTagName("measure");
        for (int i = 0; i < measures.getLength(); i++) {
            org.w3c.dom.Element measure = (org.w3c.dom.Element) measures.item(i);
            if (measureNumber.equals(measure.getAttribute("number"))) {
                return measure;
            }
        }
        throw new AssertionError("measure not found: " + measureNumber);
    }

    private static String directChildText(org.w3c.dom.Element element, String childName) {
        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child instanceof org.w3c.dom.Element && childName.equals(child.getNodeName())) {
                return child.getTextContent() == null ? "" : child.getTextContent().trim();
            }
        }
        return "";
    }

    private static String nestedText(org.w3c.dom.Element element, String childName) {
        org.w3c.dom.NodeList nodes = element.getElementsByTagName(childName);
        if (nodes.getLength() == 0) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    private static String pitchToken(org.w3c.dom.Element note) {
        return nestedText(note, "step") + nestedText(note, "octave");
    }

    private static String beamText(org.w3c.dom.Element note, String number) {
        org.w3c.dom.NodeList beams = note.getElementsByTagName("beam");
        for (int i = 0; i < beams.getLength(); i++) {
            org.w3c.dom.Element beam = (org.w3c.dom.Element) beams.item(i);
            if (number.equals(beam.getAttribute("number"))) {
                return beam.getTextContent() == null ? "" : beam.getTextContent().trim();
            }
        }
        return "";
    }

    private static boolean hasDirectChildWithAttribute(org.w3c.dom.Element element, String childName, String attrName,
            String attrValue) {
        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child instanceof org.w3c.dom.Element && childName.equals(child.getNodeName())) {
                org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
                if (attrValue.equals(childElement.getAttribute(attrName))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNestedChildWithAttribute(org.w3c.dom.Element element, String childName, String attrName,
            String attrValue) {
        org.w3c.dom.NodeList nodes = element.getElementsByTagName(childName);
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Element child = (org.w3c.dom.Element) nodes.item(i);
            if (attrValue.equals(child.getAttribute(attrName))) {
                return true;
            }
        }
        return false;
    }
}
