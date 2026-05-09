package jp.igapyon.mikuscore.mei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        org.w3c.dom.Element first = MeiIo.selectMeiImportRoot(corpus, Integer.valueOf(0));
        assertEquals("", MeiIo.firstDescendantText(first, "title"));

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
                "<mei><music><body><mdiv><score><scoreDef meter.count=\"4\" meter.unit=\"4\" meter.sym=\"common\">"
                        + "<staffDef n=\"1\" meter.count=\"3\" meter.unit=\"8\" clef.shape=\"F\" clef.line=\"4\" trans.semi=\"-2\"/>"
                        + "<staffDef meter.sym=\"cut\"><clef shape=\"G\" line=\"2\"/></staffDef></scoreDef>"
                        + "<section><measure n=\"1\"><staff n=\"1\"/></measure>"
                        + "<scoreDef meter.count=\"6\" meter.unit=\"8\"><staffDef n=\"2\" meter.count=\"2\" meter.unit=\"2\" clef.shape=\"C\" clef.line=\"3\"/></scoreDef>"
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
}
