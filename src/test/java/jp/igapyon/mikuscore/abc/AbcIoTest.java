/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class AbcIoTest {
    @Test
    public void reducesAndCombinesFractions() {
        assertFraction(3, 4, AbcIo.reduceFraction(6, 8));
        assertFraction(-3, 4, AbcIo.reduceFraction(6, -8));
        assertFraction(2, 3, AbcIo.multiplyFractions(new AbcIo.Fraction(1, 2), new AbcIo.Fraction(4, 3)));
        assertFraction(3, 8, AbcIo.divideFractions(new AbcIo.Fraction(3, 4), new AbcIo.Fraction(2, 1)));
    }

    @Test
    public void parsesFractionTextWithFallback() {
        assertFraction(3, 8, AbcIo.parseFractionText("6/16"));
        assertFraction(1, 8, AbcIo.parseFractionText("bad"));
        assertFraction(1, 8, AbcIo.parseFractionText("1/0"));
    }

    @Test
    public void parsesAbcLengthTokens() {
        assertFraction(1, 1, AbcIo.parseAbcLengthToken("", 1));
        assertFraction(1, 2, AbcIo.parseAbcLengthToken("/", 1));
        assertFraction(1, 4, AbcIo.parseAbcLengthToken("//", 1));
        assertFraction(2, 1, AbcIo.parseAbcLengthToken("2", 1));
        assertFraction(3, 2, AbcIo.parseAbcLengthToken("3/", 1));
        assertFraction(1, 3, AbcIo.parseAbcLengthToken("/3", 1));
        assertFraction(3, 4, AbcIo.parseAbcLengthToken("6/8", 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AbcIo.parseAbcLengthToken("bad", 12));
        assertEquals("line 12: Could not parse length token: bad", ex.getMessage());
    }

    @Test
    public void formatsAbcLengthTokens() {
        assertEquals("", AbcIo.abcLengthTokenFromFraction(new AbcIo.Fraction(1, 1)));
        assertEquals("2", AbcIo.abcLengthTokenFromFraction(new AbcIo.Fraction(2, 1)));
        assertEquals("/", AbcIo.abcLengthTokenFromFraction(new AbcIo.Fraction(1, 2)));
        assertEquals("/4", AbcIo.abcLengthTokenFromFraction(new AbcIo.Fraction(1, 4)));
        assertEquals("3/4", AbcIo.abcLengthTokenFromFraction(new AbcIo.Fraction(6, 8)));
    }

    @Test
    public void formatsPitchAccidentalKeyAndTempo() {
        assertEquals("C", AbcIo.abcPitchFromStepOctave("C", 4));
        assertEquals("C,", AbcIo.abcPitchFromStepOctave("C", 3));
        assertEquals("c", AbcIo.abcPitchFromStepOctave("C", 5));
        assertEquals("c'", AbcIo.abcPitchFromStepOctave("C", 6));
        assertEquals("C", AbcIo.abcPitchFromStepOctave("bad", 4));

        assertEquals("", AbcIo.accidentalFromAlter(0));
        assertEquals("^", AbcIo.accidentalFromAlter(1));
        assertEquals("^^", AbcIo.accidentalFromAlter(3));
        assertEquals("_", AbcIo.accidentalFromAlter(-1));
        assertEquals("__", AbcIo.accidentalFromAlter(-3));

        assertEquals("C", AbcIo.keyFromFifthsMode(0, "major"));
        assertEquals("Am", AbcIo.keyFromFifthsMode(0, "minor"));
        assertEquals("G", AbcIo.keyFromFifthsMode(1, ""));
        assertEquals("C", AbcIo.keyFromFifthsMode(99, "major"));
        assertEquals("1/4", AbcIo.fractionToAbcTempoUnit(new AbcIo.Fraction(2, 8)));
    }

    @Test
    public void detectsAbcjsWrapperLines() {
        assertEquals(true, AbcIo.isAbcjsWrapperLine("[abcjs]"));
        assertEquals(true, AbcIo.isAbcjsWrapperLine("[/abcjs]"));
        assertEquals(true, AbcIo.isAbcjsWrapperLine("[abcjs-audio foo=bar]"));
        assertEquals(false, AbcIo.isAbcjsWrapperLine("[K:C]"));
        assertEquals(false, AbcIo.isAbcjsWrapperLine("abcjs"));
    }

    @Test
    public void estimatesMeasureContentDurationByVoice() {
        assertEquals(0, AbcIo.estimateAbcMeasureContentDiv(null));
        assertEquals(960, AbcIo.estimateAbcMeasureContentDiv(Arrays.asList(
                new AbcIo.AbcMeasureNote("1", 480, false, false),
                new AbcIo.AbcMeasureNote("1", 480, false, false))));
        assertEquals(480, AbcIo.estimateAbcMeasureContentDiv(Arrays.asList(
                new AbcIo.AbcMeasureNote("1", 480, false, false),
                new AbcIo.AbcMeasureNote("1", 480, true, false))));
        assertEquals(960, AbcIo.estimateAbcMeasureContentDiv(Arrays.asList(
                new AbcIo.AbcMeasureNote("1", 480, false, false),
                new AbcIo.AbcMeasureNote("2", 960, false, false),
                new AbcIo.AbcMeasureNote("1", 120, false, true))));
    }

    @Test
    public void mapsAbcKeysToFifths() {
        assertEquals(Integer.valueOf(0), AbcIo.fifthsFromAbcKey("C"));
        assertEquals(Integer.valueOf(1), AbcIo.fifthsFromAbcKey("G"));
        assertEquals(Integer.valueOf(-1), AbcIo.fifthsFromAbcKey("F"));
        assertEquals(Integer.valueOf(0), AbcIo.fifthsFromAbcKey("A m"));
        assertEquals(Integer.valueOf(6), AbcIo.fifthsFromAbcKey("F#"));
        assertEquals(Integer.valueOf(-7), AbcIo.fifthsFromAbcKey("Cb"));
        assertNull(AbcIo.fifthsFromAbcKey("H"));
    }

    @Test
    public void parsesAndAppliesAbcMetaParams() {
        Map<String, String> params = AbcIo.parseAbcMetaParams("voice=V1 measure=2 event=3 upper=D# ignored");
        assertEquals("V1", params.get("voice"));
        assertEquals("2", params.get("measure"));
        assertEquals("D#", params.get("upper"));

        Map<String, String> trill = new LinkedHashMap<String, String>();
        assertEquals(true, AbcIo.applyAbcTrillMeta(params, trill));
        assertEquals("D#", trill.get("V1#2#3"));
    }

    @Test
    public void handlesAbcMetaDirectiveLines() {
        Map<String, String> trill = new LinkedHashMap<String, String>();
        Map<String, Integer> keys = new LinkedHashMap<String, Integer>();
        Map<String, AbcIo.AbcMeasureMeta> measures = new LinkedHashMap<String, AbcIo.AbcMeasureMeta>();
        Map<String, AbcIo.AbcTransposeMeta> transposes = new LinkedHashMap<String, AbcIo.AbcTransposeMeta>();

        assertEquals(true, AbcIo.handleAbcMetaDirectiveLine("%@mks trill voice=V1 measure=2 event=3 upper=D",
                trill, keys, measures, transposes));
        assertEquals("D", trill.get("V1#2#3"));

        assertEquals(true, AbcIo.handleAbcMetaDirectiveLine("%@mks key voice=V1 measure=2 fifths=99",
                trill, keys, measures, transposes));
        assertEquals(Integer.valueOf(7), keys.get("V1#2"));

        assertEquals(true, AbcIo.handleAbcMetaDirectiveLine(
                "%@mks measure voice=V1 measure=4 number=pickup implicit=yes left-repeat=true right-repeat=true times=3 ending-start=1 ending-stop=1 ending-type=discontinue",
                trill, keys, measures, transposes));
        AbcIo.AbcMeasureMeta measure = measures.get("V1#4");
        assertEquals("pickup", measure.getNumber());
        assertEquals(true, measure.isImplicit());
        assertEquals(true, measure.isRepeatStart());
        assertEquals(true, measure.isRepeatEnd());
        assertEquals(Integer.valueOf(3), measure.getRepeatTimes());
        assertEquals("1", measure.getEndingStart());
        assertEquals("1", measure.getEndingStop());
        assertEquals("discontinue", measure.getEndingStopType());

        assertEquals(true, AbcIo.handleAbcMetaDirectiveLine("%@mks transpose voice=V2 chromatic=-2 diatonic=-1",
                trill, keys, measures, transposes));
        assertEquals(Integer.valueOf(-2), transposes.get("V2").getChromatic());
        assertEquals(Integer.valueOf(-1), transposes.get("V2").getDiatonic());

        assertEquals(false, AbcIo.handleAbcMetaDirectiveLine("%not-mks key voice=V1", trill, keys, measures,
                transposes));
    }

    @Test
    public void detectsStructuredDirectiveLines() {
        assertEquals(true, AbcIo.isAbcStructuredDirectiveLine("%@mks key voice=V1 measure=1 fifths=0"));
        assertEquals(true, AbcIo.isAbcStructuredDirectiveLine("%%score { 1 2 }"));
        assertEquals(true, AbcIo.isAbcStructuredDirectiveLine("K:C"));
        assertEquals(false, AbcIo.isAbcStructuredDirectiveLine("C D E F"));
    }

    @Test
    public void handlesUnsupportedContinuedFieldLines() {
        AbcIo.AbcImportLineState state = new AbcIo.AbcImportLineState();
        List<String> warnings = new ArrayList<String>();
        state.setPendingUnsupportedContinuedFieldName("T:");

        assertEquals(true, AbcIo.handleAbcUnsupportedContinuedFieldLine("continued \\", "continued", 2, state,
                warnings));
        assertEquals("T:", state.getPendingUnsupportedContinuedFieldName());
        assertEquals(true, AbcIo.handleAbcUnsupportedContinuedFieldLine("last", "last", 3, state, warnings));
        assertEquals("", state.getPendingUnsupportedContinuedFieldName());
        assertEquals("line 2: Skipped unsupported continued field text for T:: continued", warnings.get(0));
        assertEquals("line 3: Skipped unsupported continued field text for T:: last", warnings.get(1));

        state.setPendingUnsupportedContinuedFieldName("T:");
        AbcIo.clearAbcPendingUnsupportedContinuedFieldOnStructuredLine("K:C", state);
        assertEquals("", state.getPendingUnsupportedContinuedFieldName());
    }

    @Test
    public void parsesAndExpandsUserDefinedDecorations() {
        AbcIo.AbcUserDefinedDecoration decoration = AbcIo.parseUserDefinedDecoration("H = !trill!");
        assertEquals("H", decoration.getSymbol());
        assertEquals("trill", decoration.getDecoration());

        Map<String, String> symbols = new LinkedHashMap<String, String>();
        symbols.put("H", "trill");
        assertEquals("C !trill!D \"H\" !H!", AbcIo.expandUserDefinedDecorationSymbols("C HD \"H\" !H!", symbols));
    }

    @Test
    public void processesAbcImportLinesIntoHeadersMetaAndBody() {
        AbcIo.AbcImportLineState state = new AbcIo.AbcImportLineState();
        List<String> warnings = new ArrayList<String>();
        Map<String, String> headers = new LinkedHashMap<String, String>();
        Map<String, List<AbcIo.AbcLyricEntry>> lyrics = new LinkedHashMap<String, List<AbcIo.AbcLyricEntry>>();
        AbcIo.AbcImportVoiceRegistry voiceRegistry = new AbcIo.AbcImportVoiceRegistry();
        Map<String, String> symbols = new LinkedHashMap<String, String>();
        Map<String, String> trill = new LinkedHashMap<String, String>();
        Map<String, Integer> keys = new LinkedHashMap<String, Integer>();
        Map<String, AbcIo.AbcMeasureMeta> measures = new LinkedHashMap<String, AbcIo.AbcMeasureMeta>();
        Map<String, AbcIo.AbcTransposeMeta> transposes = new LinkedHashMap<String, AbcIo.AbcTransposeMeta>();
        List<String> body = new ArrayList<String>();
        AbcIo.AbcImportLineProcessorContext context = new AbcIo.AbcImportLineProcessorContext(state, warnings,
                headers, lyrics, new HashSet<String>(Arrays.asList("K", "L", "M", "Q")), voiceRegistry, symbols,
                trill, keys, measures, transposes, (rawBodyText, lineNo, voiceId) -> {
                    body.add(voiceId + ":" + rawBodyText.trim());
                    state.setBodyStarted(true);
                    state.setCurrentVoiceId(voiceId);
                }, raw -> new AbcIo.AbcParsedVoiceDirectiveTail("Lead", "treble",
                        new AbcIo.AbcTransposeMeta(Integer.valueOf(1), null), raw.contains("body") ? "C D" : "",
                        "skipme", Arrays.asList("foo")),
                AbcIo::parseUserDefinedDecoration, AbcIo::expandUserDefinedDecorationSymbols);

        AbcIo.processAbcImportLine("T: Tune", 1, context);
        AbcIo.processAbcImportLine("U: H = !trill!", 2, context);
        AbcIo.processAbcImportLine("%@mks key voice=V1 measure=2 fifths=-9", 3, context);
        AbcIo.processAbcImportLine("%%score { V1 }", 4, context);
        AbcIo.processAbcImportLine("V: V1 body", 5, context);
        AbcIo.processAbcImportLine("w: la la", 6, context);
        AbcIo.processAbcImportLine("K:G", 7, context);
        AbcIo.processAbcImportLine("H A B % comment", 8, context);
        AbcIo.processAbcImportLine("[abcjs]", 9, context);

        assertEquals("Tune", headers.get("T"));
        assertEquals(Integer.valueOf(-7), keys.get("V1#2"));
        assertEquals("{ V1 }", state.getScoreDirective());
        assertEquals(Arrays.asList("V1:C D", "V1:[K:G]", "V1:!trill! A B"), body);
        assertEquals("Lead", voiceRegistry.getVoiceNameById().get("V1"));
        assertEquals("treble", voiceRegistry.getVoiceClefById().get("V1"));
        assertEquals(Integer.valueOf(1), voiceRegistry.getVoiceTransposeById().get("V1").getChromatic());
        assertEquals("la la", lyrics.get("V1").get(0).getText());
        assertEquals("line 5: Skipped unsupported V: directive tail token: skipme", warnings.get(0));
        assertEquals("line 5: Skipped unsupported V: property: foo", warnings.get(1));
        assertEquals("line 9: Skipped unsupported abcjs wrapper line: [abcjs]", warnings.get(2));
    }

    @Test
    public void splitsBodyTextByInlineVoice() {
        AbcIo.AbcInlineVoiceSplitResult result = AbcIo.splitBodyTextByInlineVoice("A B [V:V2] C D [K:G]", "V1");

        assertEquals("V2", result.getFinalVoiceId());
        assertEquals(2, result.getSegments().size());
        assertEquals("V1", result.getSegments().get(0).getVoiceId());
        assertEquals("A B ", result.getSegments().get(0).getText());
        assertEquals("V2", result.getSegments().get(1).getVoiceId());
        assertEquals(" C D [K:G]", result.getSegments().get(1).getText());
    }

    @Test
    public void splitsBodyTextByOverlay() {
        List<AbcIo.AbcOverlaySegment> segments = AbcIo.splitBodyTextByOverlay("A B | C & D | \"E&\" !trill! F",
                "V1");

        assertEquals(2, segments.size());
        assertEquals("V1", segments.get(0).getVoiceId());
        assertEquals("A B | C | \"E&\" !trill! F", segments.get(0).getText());
        assertEquals(0, segments.get(0).getOverlayIndex());
        assertEquals("V1_ov2", segments.get(1).getVoiceId());
        assertEquals("| D |", segments.get(1).getText());
        assertEquals(1, segments.get(1).getOverlayIndex());
    }

    @Test
    public void appendsAbcBodyTextEntriesWithOverlayVoiceMetadata() {
        AbcIo.AbcImportVoiceRegistry registry = new AbcIo.AbcImportVoiceRegistry();
        registry.getVoiceNameById().put("V1", "Lead");
        registry.getVoiceClefById().put("V1", "treble");
        registry.getVoiceTransposeById().put("V1", new AbcIo.AbcTransposeMeta(Integer.valueOf(2), null));
        List<AbcIo.AbcImportBodyEntry> bodyEntries = new ArrayList<AbcIo.AbcImportBodyEntry>();

        AbcIo.AbcAppendBodyTextResult result = AbcIo.appendAbcBodyTextEntries("A B | C & D | [V:V2] E F \\", 12,
                "V1", registry, bodyEntries);

        assertEquals(true, result.isAppended());
        assertEquals("V2", result.getFinalVoiceId());
        assertEquals(3, bodyEntries.size());
        assertEquals("V1", bodyEntries.get(0).getVoiceId());
        assertEquals("A B | C | ", bodyEntries.get(0).getText());
        assertEquals("V1_ov2", bodyEntries.get(1).getVoiceId());
        assertEquals("| D |", bodyEntries.get(1).getText());
        assertEquals("V2", bodyEntries.get(2).getVoiceId());
        assertEquals(" E F ", bodyEntries.get(2).getText());
        assertEquals(Arrays.asList("V1", "V1_ov2", "V2"), registry.getDeclaredVoiceIds());
        assertEquals("Lead overlay 2", registry.getVoiceNameById().get("V1_ov2"));
        assertEquals("treble", registry.getVoiceClefById().get("V1_ov2"));
        assertEquals(Integer.valueOf(2), registry.getVoiceTransposeById().get("V1_ov2").getChromatic());
    }

    @Test
    public void parsesVoiceDirectiveTail() {
        AbcIo.AbcParsedVoiceDirectiveTail parsed = AbcIo
                .parseVoiceDirectiveTail("name=\"Clarinet in A\" clef=treble transpose=-3 C D");

        assertEquals("Clarinet in A", parsed.getName());
        assertEquals("treble", parsed.getClef());
        assertEquals(Integer.valueOf(-3), parsed.getTranspose().getChromatic());
        assertEquals("C D", parsed.getBodyText());
        assertEquals("", parsed.getSkippedText());
        assertEquals(0, parsed.getUnsupportedKeys().size());

        AbcIo.AbcParsedVoiceDirectiveTail bareClef = AbcIo.parseVoiceDirectiveTail("bass z2");
        assertEquals("bass", bareClef.getClef());
        assertEquals("z2", bareClef.getSkippedText());
        assertEquals("", bareClef.getBodyText());

        AbcIo.AbcParsedVoiceDirectiveTail unsupported = AbcIo
                .parseVoiceDirectiveTail("name=Lead unknown=1 transpose=99 body C");
        assertEquals("Lead", unsupported.getName());
        assertNull(unsupported.getTranspose());
        assertEquals("body", unsupported.getSkippedText());
        assertEquals("C", unsupported.getBodyText());
        assertEquals(Arrays.asList("unknown"), unsupported.getUnsupportedKeys());
    }

    @Test
    public void parsesAbcHeaderTempoMeterFractionAndKey() {
        List<String> warnings = new ArrayList<String>();

        assertEquals(Integer.valueOf(120), AbcIo.parseTempoFromQ("1/4=120", warnings));
        assertEquals(Integer.valueOf(240), AbcIo.parseTempoFromQ("\"Allegro\" 1/2 = 120", warnings));
        assertEquals(Integer.valueOf(88), AbcIo.parseTempoFromQ("Q:=88", warnings));
        assertEquals(Integer.valueOf(20), AbcIo.parseTempoFromQ("1", warnings));
        assertNull(AbcIo.parseTempoFromQ("", warnings));
        assertNull(AbcIo.parseTempoFromQ("fast", warnings));
        assertEquals("Q: unsupported tempo format; ignored: fast", warnings.get(0));

        AbcIo.AbcMeter common = AbcIo.parseMeter("C", warnings);
        assertEquals(4, common.getBeats());
        assertEquals(4, common.getBeatType());
        AbcIo.AbcMeter cut = AbcIo.parseMeter("C|", warnings);
        assertEquals(2, cut.getBeats());
        assertEquals(2, cut.getBeatType());
        AbcIo.AbcMeter threeFour = AbcIo.parseMeter("3/4", warnings);
        assertEquals(3, threeFour.getBeats());
        assertEquals(4, threeFour.getBeatType());
        AbcIo.AbcMeter fallbackMeter = AbcIo.parseMeter("free", warnings);
        assertEquals(4, fallbackMeter.getBeats());
        assertEquals(4, fallbackMeter.getBeatType());
        assertEquals("Invalid meter M: format; defaulted to 4/4: free", warnings.get(1));

        assertFraction(1, 16, AbcIo.parseFraction("1/16", "L", warnings));
        assertFraction(1, 8, AbcIo.parseFraction("bad", "L", warnings));
        assertEquals("L has invalid format; defaulted to 1/8: bad", warnings.get(2));
        assertFraction(1, 8, AbcIo.parseFraction("1/0", "L", warnings));
        assertEquals("L has invalid value; defaulted to 1/8: 1/0", warnings.get(3));

        assertEquals(2, AbcIo.parseKey("D", warnings).getFifths());
        assertEquals(0, AbcIo.parseKey("H", warnings).getFifths());
        assertEquals("K: unsupported key; defaulted to C: H", warnings.get(4));
    }

    @Test
    public void buildsAbcVoiceMeasureMetaByIndex() {
        Map<String, Integer> keyHints = new LinkedHashMap<String, Integer>();
        keyHints.put("V1#2", Integer.valueOf(3));

        Map<String, Map<Integer, AbcIo.AbcMeasureMeta>> notationMeta = new LinkedHashMap<String, Map<Integer, AbcIo.AbcMeasureMeta>>();
        Map<Integer, AbcIo.AbcMeasureMeta> notationMetaByMeasure = new LinkedHashMap<Integer, AbcIo.AbcMeasureMeta>();
        notationMetaByMeasure.put(Integer.valueOf(2),
                new AbcIo.AbcMeasureMeta("N2", true, true, false, Integer.valueOf(2), "1", "old-stop",
                        "discontinue"));
        notationMeta.put("V1", notationMetaByMeasure);

        Map<String, AbcIo.AbcMeasureMeta> hintedMeta = new LinkedHashMap<String, AbcIo.AbcMeasureMeta>();
        hintedMeta.put("V1#2",
                new AbcIo.AbcMeasureMeta("H2", false, false, true, Integer.valueOf(3), "", "2", "stop"));

        Map<String, Map<Integer, AbcIo.AbcMeter>> meterHints = new LinkedHashMap<String, Map<Integer, AbcIo.AbcMeter>>();
        Map<Integer, AbcIo.AbcMeter> meterByMeasure = new LinkedHashMap<Integer, AbcIo.AbcMeter>();
        meterByMeasure.put(Integer.valueOf(2), new AbcIo.AbcMeter(6, 8));
        meterHints.put("V1", meterByMeasure);

        Map<String, Map<Integer, Integer>> tempoHints = new LinkedHashMap<String, Map<Integer, Integer>>();
        Map<Integer, Integer> tempoByMeasure = new LinkedHashMap<Integer, Integer>();
        tempoByMeasure.put(Integer.valueOf(2), Integer.valueOf(999));
        tempoHints.put("V1", tempoByMeasure);

        AbcIo.AbcVoiceMeasureMetaByIndex result = AbcIo.buildAbcVoiceMeasureMetaByIndex("V1",
                Arrays.asList("m1", "m2", "m3"), keyHints, notationMeta, hintedMeta, meterHints, tempoHints);

        assertEquals(Integer.valueOf(3), result.getKeyByMeasure().get(Integer.valueOf(2)));
        assertEquals(6, result.getMeterByMeasure().get(Integer.valueOf(2)).getBeats());
        assertEquals(8, result.getMeterByMeasure().get(Integer.valueOf(2)).getBeatType());
        assertEquals(Integer.valueOf(300), result.getTempoByMeasure().get(Integer.valueOf(2)));

        AbcIo.AbcMeasureMeta measure = result.getMeasureMetaByIndex().get(Integer.valueOf(2));
        assertEquals("H2", measure.getNumber());
        assertEquals(false, measure.isImplicit());
        assertEquals(true, measure.isRepeatStart());
        assertEquals(true, measure.isRepeatEnd());
        assertEquals(Integer.valueOf(3), measure.getRepeatTimes());
        assertEquals("1", measure.getEndingStart());
        assertEquals("old-stop", measure.getEndingStop());
        assertEquals("stop", measure.getEndingStopType());
    }

    @Test
    public void buildsAbcMusicXmlExportHelperXml() {
        assertEquals("&amp;&lt;&gt;&quot;&apos;", AbcIo.xmlEscape("&<>\"'"));
        assertEquals("<clef><sign>G</sign><line>2</line></clef>", AbcIo.clefXmlFromAbcClef(""));
        assertEquals("<clef><sign>F</sign><line>4</line></clef>", AbcIo.clefXmlFromAbcClef("bass"));
        assertEquals("<clef><sign>C</sign><line>3</line></clef>", AbcIo.clefXmlFromAbcClef("alto"));
        assertEquals("<clef><sign>C</sign><line>4</line></clef>", AbcIo.clefXmlFromAbcClef("tenor"));

        assertEquals("<transpose><diatonic>-1</diatonic><chromatic>-2</chromatic></transpose>",
                AbcIo.buildAbcPartTransposeXml(new AbcIo.AbcTransposeMeta(Integer.valueOf(-2),
                        Integer.valueOf(-1))));
        assertEquals("", AbcIo.buildAbcPartTransposeXml(null));
        assertEquals("", AbcIo.buildAbcTempoDirectionXml(Integer.valueOf(120), false));
        assertEquals(
                "<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>120</per-minute></metronome></direction-type><sound tempo=\"120\"/></direction>",
                AbcIo.buildAbcTempoDirectionXml(Integer.valueOf(120), true));

        List<AbcIo.AbcParsedStaffVoice> staffVoices = Arrays.asList(new AbcIo.AbcParsedStaffVoice(1, "treble"),
                new AbcIo.AbcParsedStaffVoice(2, "bass"));
        assertEquals(
                "<clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign><line>4</line></clef>",
                AbcIo.buildAbcGroupedStaffClefXml(staffVoices));

        AbcIo.AbcParsedPartHeader part = new AbcIo.AbcParsedPartHeader("alto",
                new AbcIo.AbcTransposeMeta(Integer.valueOf(-3), null), staffVoices);
        AbcIo.AbcMeasureHeaderXml firstHeader = AbcIo.buildAbcMeasureHeaderXml(part, 0, 0, -1,
                new AbcIo.AbcMeter(3, 4), Integer.valueOf(96), null, null);
        assertEquals(
                "<attributes><divisions>960</divisions><key><fifths>-1</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time><staves>2</staves><transpose><chromatic>-3</chromatic></transpose><clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes><direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>96</per-minute></metronome></direction-type><sound tempo=\"96\"/></direction>",
                firstHeader.getHeaderXml());
        assertEquals("", firstHeader.getTempoDirectionXml());

        AbcIo.AbcMeasureHeaderXml laterHeader = AbcIo.buildAbcMeasureHeaderXml(part, 1, 2, 2,
                new AbcIo.AbcMeter(6, 8), Integer.valueOf(72), Integer.valueOf(2), new AbcIo.AbcMeter(6, 8));
        assertEquals(
                "<attributes><key><fifths>2</fifths></key><time><beats>6</beats><beat-type>8</beat-type></time></attributes>",
                laterHeader.getHeaderXml());

        AbcIo.AbcMeasureMeta meta = new AbcIo.AbcMeasureMeta("A&B", true, true, true, Integer.valueOf(3), "1<",
                "2>", "discontinue");
        assertEquals(
                "<barline location=\"left\"><ending number=\"1&lt;\" type=\"start\"/><repeat direction=\"forward\" winged=\"none\"/></barline>",
                AbcIo.buildAbcMeasureRepeatStartXml(meta));
        assertEquals(
                "<barline location=\"right\"><ending number=\"2&gt;\" type=\"discontinue\"/><repeat direction=\"backward\" winged=\"none\" times=\"3\"/></barline>",
                AbcIo.buildAbcMeasureRepeatEndXml(meta));
        assertEquals("<measure number=\"A&amp;B\" implicit=\"yes\"><left/><attr/><tempo/><debug/><diag/><src/><note/><right/></measure>",
                AbcIo.buildAbcMeasureXml(4, "A&B", true, "<left/>", "<attr/>", "<tempo/>", "<debug/>",
                        "<diag/>", "<src/>", "<note/>", "<right/>"));
    }

    @Test
    public void buildsAbcPartMeasureRenderContext() {
        AbcIo.AbcPartRenderState state = AbcIo.createInitialAbcPartRenderState(9, 4, 4, Integer.valueOf(88));
        assertEquals(7, state.getCurrentPartFifths());
        assertEquals(4, state.getCurrentPartMeter().getBeats());
        assertEquals(4, state.getCurrentPartMeter().getBeatType());
        assertEquals(Integer.valueOf(88), state.getCurrentPartTempo());

        List<List<AbcIo.AbcMeasureNote>> measures = Arrays.asList(
                Arrays.asList(new AbcIo.AbcMeasureNote("V1", 480, false, false)),
                Arrays.asList(new AbcIo.AbcMeasureNote("V1", 720, false, false),
                        new AbcIo.AbcMeasureNote("V1", 720, false, false)));
        Map<Integer, Integer> keyByMeasure = new LinkedHashMap<Integer, Integer>();
        keyByMeasure.put(Integer.valueOf(2), Integer.valueOf(-9));
        Map<Integer, AbcIo.AbcMeter> meterByMeasure = new LinkedHashMap<Integer, AbcIo.AbcMeter>();
        meterByMeasure.put(Integer.valueOf(2), new AbcIo.AbcMeter(6, 8));
        Map<Integer, Integer> tempoByMeasure = new LinkedHashMap<Integer, Integer>();
        tempoByMeasure.put(Integer.valueOf(2), Integer.valueOf(999));
        Map<Integer, AbcIo.AbcMeasureMeta> measureMetaByIndex = new LinkedHashMap<Integer, AbcIo.AbcMeasureMeta>();
        measureMetaByIndex.put(Integer.valueOf(2),
                new AbcIo.AbcMeasureMeta("2", true, false, false, null, "", "", ""));
        AbcIo.AbcParsedPartRenderData part = new AbcIo.AbcParsedPartRenderData(measures, keyByMeasure, meterByMeasure,
                tempoByMeasure, measureMetaByIndex);

        AbcIo.AbcPartMeasureRenderContext first = AbcIo.buildAbcPartMeasureRenderContext(part, 0, state, 4, 4);
        assertEquals(1, first.getNotes().size());
        assertNull(first.getMeasureMeta());
        assertNull(first.getHintedFifths());
        assertNull(first.getHintedMeter());
        assertNull(first.getHintedTempo());
        assertEquals(3840, first.getCurrentMeasureDurationDiv());
        assertEquals(true, first.isInferredImplicitPickup());
        assertEquals(7, first.getNextState().getCurrentPartFifths());
        assertEquals(Integer.valueOf(88), first.getNextState().getCurrentPartTempo());

        AbcIo.AbcPartMeasureRenderContext second = AbcIo.buildAbcPartMeasureRenderContext(part, 1,
                first.getNextState(), 4, 4);
        assertEquals(Integer.valueOf(-7), second.getHintedFifths());
        assertEquals(6, second.getHintedMeter().getBeats());
        assertEquals(8, second.getHintedMeter().getBeatType());
        assertEquals(Integer.valueOf(300), second.getHintedTempo());
        assertEquals(2880, second.getCurrentMeasureDurationDiv());
        assertEquals(false, second.isInferredImplicitPickup());
        assertEquals(-7, second.getNextState().getCurrentPartFifths());
        assertEquals(6, second.getNextState().getCurrentPartMeter().getBeats());
        assertEquals(8, second.getNextState().getCurrentPartMeter().getBeatType());
        assertEquals(Integer.valueOf(300), second.getNextState().getCurrentPartTempo());
        assertEquals("2", second.getMeasureMeta().getNumber());
    }

    @Test
    public void buildsAbcRenderedMeasureMiscXml() {
        List<AbcIo.AbcMeasureNote> notes = Arrays.asList(new AbcIo.AbcMeasureNote("V12", 480, false, false, false,
                "D", Integer.valueOf(5), Integer.valueOf(1), "eighth"));

        String debug = AbcIo.buildAbcMeasureDebugMiscXml(notes, 3);
        assertEquals(
                "<attributes><miscellaneous><miscellaneous-field name=\"mks:dbg:abc:meta:count\">0x0001</miscellaneous-field><miscellaneous-field name=\"mks:dbg:abc:meta:0001\">idx=0x0000;m=0x0003;v=12;r=0;g=0;ch=0;st=D;al=1;oc=0x05;dd=0x01E0;tp=eighth</miscellaneous-field></miscellaneous></attributes>",
                debug);

        String source = AbcIo.buildAbcSourceMiscXml("A\\B\nC");
        assertEquals(
                "<attributes><miscellaneous><miscellaneous-field name=\"mks:src:abc:raw-encoding\">escape-v1</miscellaneous-field><miscellaneous-field name=\"mks:src:abc:raw-length\">5</miscellaneous-field><miscellaneous-field name=\"mks:src:abc:raw-encoded-length\">7</miscellaneous-field><miscellaneous-field name=\"mks:src:abc:raw-chunks\">1</miscellaneous-field><miscellaneous-field name=\"mks:src:abc:raw-truncated\">0</miscellaneous-field><miscellaneous-field name=\"mks:src:abc:raw-0001\">A\\\\B\\nC</miscellaneous-field></miscellaneous></attributes>",
                source);

        List<AbcIo.AbcImportDiagnostic> diagnostics = Arrays.asList(
                new AbcIo.AbcImportDiagnostic("warn", "OVERFULL", "abc", "moved <x>", "V1", Integer.valueOf(2),
                        "reflowed", Integer.valueOf(3)),
                new AbcIo.AbcImportDiagnostic("warn", "GLOBAL", "abc", "", "", null, "", null));
        String diag = AbcIo.buildAbcDiagMiscXml(diagnostics);
        assertEquals(
                "<attributes><miscellaneous><miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field><miscellaneous-field name=\"mks:diag:0001\">level=warn;code=OVERFULL;fmt=abc;measure=2;voice=V1;action=reflowed;message=moved &lt;x&gt;;movedEvents=3</miscellaneous-field><miscellaneous-field name=\"mks:diag:0002\">level=warn;code=GLOBAL;fmt=abc</miscellaneous-field></miscellaneous></attributes>",
                diag);

        AbcIo.AbcParsedPartRenderData part = new AbcIo.AbcParsedPartRenderData("V1",
                Arrays.asList(notes), new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcIo.AbcMeter>(),
                new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcIo.AbcMeasureMeta>());
        AbcIo.AbcRenderedMeasureMiscXml rendered = AbcIo.buildAbcRenderedMeasureMiscXml(
                new AbcIo.AbcRenderedMeasureMiscContext(part, 0, 1, notes, true, true,
                        Arrays.asList(diagnostics.get(0),
                                new AbcIo.AbcImportDiagnostic("warn", "OTHER", "abc", "", "V2", null, "", null)),
                        "X:1"));
        assertEquals(AbcIo.buildAbcMeasureDebugMiscXml(notes, 1), rendered.getDebugMiscXml());
        assertEquals(true, rendered.getDiagMiscXml().contains("code=OVERFULL"));
        assertEquals(false, rendered.getDiagMiscXml().contains("code=OTHER"));
        assertEquals(true, rendered.getSourceMiscXml().contains("mks:src:abc:raw-encoding"));
    }

    @Test
    public void buildsAbcRenderedPartMeasureXml() {
        List<AbcIo.AbcMeasureNote> upperNotes = Arrays.asList(new AbcIo.AbcMeasureNote("V1", 960, false, false));
        List<AbcIo.AbcMeasureNote> lowerNotes = Arrays.asList(new AbcIo.AbcMeasureNote("V2", 960, false, false));
        List<AbcIo.AbcParsedStaffVoice> staffVoices = Arrays.asList(
                new AbcIo.AbcParsedStaffVoice("V1", 1, "treble", Arrays.asList(upperNotes)),
                new AbcIo.AbcParsedStaffVoice("V2", 2, "bass", Arrays.asList(lowerNotes)));
        AbcIo.AbcParsedPartHeader partHeader = new AbcIo.AbcParsedPartHeader("treble", null, staffVoices);
        assertEquals(true, AbcIo.hasAbcGroupedStaffVoices(partHeader));
        assertEquals(false, AbcIo.hasAbcGroupedStaffVoices(new AbcIo.AbcParsedPartHeader("treble", null,
                Arrays.asList(new AbcIo.AbcParsedStaffVoice(1, "treble")))));

        AbcIo.AbcMeasureNotesXmlBuilder noteBuilder = new AbcIo.AbcMeasureNotesXmlBuilder() {
            public String build(List<AbcIo.AbcMeasureNote> notes, Integer staffNumber) {
                return "<note><voice>" + notes.get(0).getVoice() + "</voice>"
                        + (staffNumber == null ? "" : "<staff>" + staffNumber + "</staff>") + "</note>";
            }
        };
        assertEquals(
                "<note><voice>V1</voice><staff>1</staff></note><backup><duration>3840</duration></backup><note><voice>V2</voice><staff>2</staff></note>",
                AbcIo.buildAbcGroupedStaffMeasureNotesXml(staffVoices, 0, 3840, noteBuilder));

        AbcIo.AbcMeasureMeta meta = new AbcIo.AbcMeasureMeta("P1", false, true, true, Integer.valueOf(2), "", "",
                "");
        AbcIo.AbcParsedPartRenderData part = new AbcIo.AbcParsedPartRenderData("V1", Arrays.asList(upperNotes),
                new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcIo.AbcMeter>(),
                new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcIo.AbcMeasureMeta>());
        String xml = AbcIo.buildAbcRenderedPartMeasureXml(new AbcIo.AbcRenderedPartMeasureContext(partHeader, part, 0,
                0, 1, upperNotes, meta, null, null, null, 0, new AbcIo.AbcMeter(4, 4), Integer.valueOf(120),
                3840, false, true, true,
                Arrays.asList(new AbcIo.AbcImportDiagnostic("warn", "ABC", "abc", "message", "V1",
                        Integer.valueOf(1), "", null)),
                "X:1", noteBuilder));
        assertEquals(true, xml.startsWith("<measure number=\"P1\">"));
        assertEquals(true, xml.contains("<staves>2</staves>"));
        assertEquals(true, xml.contains("<repeat direction=\"forward\" winged=\"none\"/>"));
        assertEquals(true, xml.contains("<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>120</per-minute></metronome></direction-type><sound tempo=\"120\"/></direction>"));
        assertEquals(true, xml.contains("<backup><duration>3840</duration></backup>"));
        assertEquals(true, xml.contains("mks:dbg:abc:meta:count"));
        assertEquals(true, xml.contains("code=ABC"));
        assertEquals(true, xml.contains("mks:src:abc:raw-encoding"));
        assertEquals(true, xml.contains("<repeat direction=\"backward\" winged=\"none\" times=\"2\"/>"));
    }

    @Test
    public void buildsAbcPartListAndBodyXml() {
        List<AbcIo.AbcMeasureNote> firstMeasure = Arrays.asList(new AbcIo.AbcMeasureNote("V1", 960, false, false));
        List<AbcIo.AbcMeasureNote> secondMeasure = Arrays.asList(new AbcIo.AbcMeasureNote("V1", 1920, false, false));
        Map<Integer, AbcIo.AbcMeasureMeta> measureMetaByIndex = new LinkedHashMap<Integer, AbcIo.AbcMeasureMeta>();
        measureMetaByIndex.put(Integer.valueOf(2),
                new AbcIo.AbcMeasureMeta("B", false, false, false, null, "", "", ""));
        AbcIo.AbcParsedPart part = new AbcIo.AbcParsedPart("P&1", "Piano <One>", "V1", "treble", null,
                new ArrayList<AbcIo.AbcParsedStaffVoice>(), Arrays.asList(firstMeasure, secondMeasure),
                new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcIo.AbcMeter>(),
                new LinkedHashMap<Integer, Integer>(), measureMetaByIndex);
        AbcIo.AbcMeasureNotesXmlBuilder noteBuilder = new AbcIo.AbcMeasureNotesXmlBuilder() {
            public String build(List<AbcIo.AbcMeasureNote> notes, Integer staffNumber) {
                if (notes.isEmpty()) {
                    return "<note><rest/><duration>3840</duration></note>";
                }
                return "<note><voice>" + notes.get(0).getVoice() + "</voice><duration>"
                        + notes.get(0).getDuration() + "</duration></note>";
            }
        };

        String partListXml = AbcIo.buildAbcPartListXml(Arrays.asList(part, new AbcIo.AbcParsedPart("P10", "Drums")));
        assertEquals(true, partListXml.contains("<score-part id=\"P&amp;1\">"));
        assertEquals(true, partListXml.contains("<part-name>Piano &lt;One&gt;</part-name>"));
        assertEquals(true, partListXml.contains("<midi-channel>1</midi-channel>"));
        assertEquals(true, partListXml.contains("<midi-channel>2</midi-channel>"));

        String bodyXml = AbcIo.buildAbcPartBodyXml(Arrays.asList(part), 2, 0, 4, 4, Integer.valueOf(90), false,
                false, new ArrayList<AbcIo.AbcImportDiagnostic>(), "", noteBuilder);
        assertEquals(true, bodyXml.startsWith("<part id=\"P&amp;1\">"));
        assertEquals(true, bodyXml.contains("<measure number=\"1\" implicit=\"yes\">"));
        assertEquals(true, bodyXml.contains("<measure number=\"B\">"));
        assertEquals(true, bodyXml.contains("<duration>960</duration>"));
        assertEquals(true, bodyXml.contains("<duration>1920</duration>"));
        assertEquals(true, bodyXml.endsWith("</part>"));

        String document = AbcIo.buildAbcScorePartwiseXmlDocument("T&A", "C<1>", partListXml, bodyXml);
        assertEquals(true, document.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertEquals(true, document.contains("<score-partwise version=\"4.0\">"));
        assertEquals(true, document.contains("<work-title>T&amp;A</work-title>"));
        assertEquals(true, document.contains("<creator type=\"composer\">C&lt;1&gt;</creator>"));
        assertEquals(true, document.contains("<part-list>" + partListXml + "</part-list>"));
        assertEquals(true, document.endsWith("</score-partwise>"));
    }

    @Test
    public void buildsMusicXmlExportContextAndDocumentFromParsedAbc() {
        List<AbcIo.AbcMeasureNote> lowNotes = Arrays.asList(new AbcIo.AbcMeasureNote("V1", 3840, false, false,
                false, "C", Integer.valueOf(3), Integer.valueOf(0), "whole"));
        AbcIo.AbcParsedPart part = new AbcIo.AbcParsedPart("P1", "Bass Voice", "V1", "", null,
                new ArrayList<AbcIo.AbcParsedStaffVoice>(), Arrays.asList(lowNotes),
                new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcIo.AbcMeter>(),
                new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcIo.AbcMeasureMeta>());
        AbcIo.AbcParsedResult parsed = new AbcIo.AbcParsedResult(
                new AbcIo.AbcParsedMeta("ABC Title", "ABC Composer", new AbcIo.AbcMeter(3, 8),
                        new AbcIo.AbcKeyInfo(-2), Integer.valueOf(400)),
                Arrays.asList(part), new ArrayList<String>(),
                Arrays.asList(new AbcIo.AbcImportDiagnostic("warn", "D1", "abc", "diag", "V1", Integer.valueOf(1),
                        "", null)));

        AbcIo.AbcMusicXmlExportContext context = AbcIo.buildAbcMusicXmlExportContext(parsed);
        assertEquals(1, context.getMeasureCount());
        assertEquals("ABC Title", context.getTitle());
        assertEquals("ABC Composer", context.getComposer());
        assertEquals(3, context.getBeats());
        assertEquals(8, context.getBeatType());
        assertEquals(-2, context.getDefaultFifths());
        assertEquals(960, context.getDivisions());
        assertEquals(480, context.getBeatDiv());
        assertEquals(1440, context.getMeasureDurationDiv());
        assertEquals("quarter", context.getEmptyMeasureRestType());
        assertEquals(Integer.valueOf(300), context.getTempoBpm());
        assertEquals("bass", context.getResolvedParts().get(0).getClef());

        AbcIo.AbcMeasureNotesXmlBuilder noteBuilder = new AbcIo.AbcMeasureNotesXmlBuilder() {
            public String build(List<AbcIo.AbcMeasureNote> notes, Integer staffNumber) {
                return "<note><voice>" + notes.get(0).getVoice() + "</voice><duration>"
                        + notes.get(0).getDuration() + "</duration></note>";
            }
        };
        String xml = AbcIo.buildMusicXmlFromAbcParsed(parsed, "X:1",
                new AbcIo.AbcImportOptions(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, null), noteBuilder);
        assertEquals(true, xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertEquals(true, xml.contains("<work-title>ABC Title</work-title>"));
        assertEquals(true, xml.contains("<part-name>Bass Voice</part-name>"));
        assertEquals(true, xml.contains("<clef><sign>F</sign><line>4</line></clef>"));
        assertEquals(true, xml.contains("<sound tempo=\"300\"/>"));
        assertEquals(true, xml.contains("mks:src:abc:raw-encoding"));
    }

    @Test
    public void buildsAbcMeasureNotesXmlCoreSubset() {
        assertEquals("<note><rest/><duration>1440</duration><voice>1</voice><type>quarter</type><staff>2</staff></note>",
                AbcIo.buildAbcEmptyMeasureNotesXml(1440, "quarter", Integer.valueOf(2)));

        AbcIo.AbcMeasureNote pitched = new AbcIo.AbcMeasureNote("V12", 480, false, false, false, "D",
                Integer.valueOf(5), Integer.valueOf(1), "eighth", Integer.valueOf(1), "sharp", true, true, true,
                false, false, "", "la & le", "begin", true, Integer.valueOf(3), Integer.valueOf(2),
                Arrays.asList("say <go>"), true, true, "A&1", true, true, true, true, true, true, true, true,
                "mf", true);
        assertEquals("<pitch><step>D</step><alter>1</alter><octave>5</octave></pitch>",
                AbcIo.buildAbcNotePitchOrRestXml(pitched));
        assertEquals("<accidental editorial=\"yes\" cautionary=\"yes\">sharp</accidental>",
                AbcIo.buildAbcNoteAccidentalXml(pitched));
        assertEquals("<lyric><syllabic>begin</syllabic><text>la &amp; le</text><extend/></lyric>",
                AbcIo.buildAbcNoteLyricXml(pitched));
        assertEquals("<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>",
                AbcIo.buildAbcNoteTimeModificationXml(pitched));
        assertEquals(
                "<direction><direction-type><words>say &lt;go&gt;</words></direction-type></direction>",
                AbcIo.buildAbcNoteHarmonyAndWordsDirectionXml(pitched));
        assertEquals(
                "<direction><direction-type><segno/></direction-type></direction><direction><direction-type><coda/></direction-type></direction><direction><direction-type><rehearsal>A&amp;1</rehearsal></direction-type></direction><direction><sound fine=\"yes\"/></direction><direction><sound dacapo=\"yes\"/></direction><direction><sound dalsegno=\"segno\"/></direction><direction><sound tocoda=\"coda\"/></direction><direction><direction-type><wedge type=\"crescendo\"/></direction-type></direction><direction><direction-type><wedge type=\"diminuendo\"/></direction-type></direction><direction><direction-type><wedge type=\"stop\"/></direction-type></direction><direction><direction-type><dynamics><mf/></dynamics></direction-type></direction><direction><direction-type><dynamics><sfz/></dynamics></direction-type></direction>",
                AbcIo.buildAbcNoteControlDirectionXml(pitched));
        assertEquals(
                "<direction><direction-type><words>say &lt;go&gt;</words></direction-type></direction><direction><direction-type><segno/></direction-type></direction><direction><direction-type><coda/></direction-type></direction><direction><direction-type><rehearsal>A&amp;1</rehearsal></direction-type></direction><direction><sound fine=\"yes\"/></direction><direction><sound dacapo=\"yes\"/></direction><direction><sound dalsegno=\"segno\"/></direction><direction><sound tocoda=\"coda\"/></direction><direction><direction-type><wedge type=\"crescendo\"/></direction-type></direction><direction><direction-type><wedge type=\"diminuendo\"/></direction-type></direction><direction><direction-type><wedge type=\"stop\"/></direction-type></direction><direction><direction-type><dynamics><mf/></dynamics></direction-type></direction><direction><direction-type><dynamics><sfz/></dynamics></direction-type></direction><note><pitch><step>D</step><alter>1</alter><octave>5</octave></pitch><duration>480</duration><voice>12</voice><staff>1</staff><lyric><syllabic>begin</syllabic><text>la &amp; le</text><extend/></lyric><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><accidental editorial=\"yes\" cautionary=\"yes\">sharp</accidental><tie type=\"start\"/><notations><tied type=\"start\"/></notations></note>",
                AbcIo.buildAbcNoteXml(pitched, 0, null, new LinkedHashMap<Integer, String>()));

        AbcIo.AbcMeasureNote rest = new AbcIo.AbcMeasureNote("V2", 960, false, false, true, "C",
                Integer.valueOf(4), Integer.valueOf(0), "quarter");
        assertEquals(
                "<direction><direction-type><words>say &lt;go&gt;</words></direction-type></direction><direction><direction-type><segno/></direction-type></direction><direction><direction-type><coda/></direction-type></direction><direction><direction-type><rehearsal>A&amp;1</rehearsal></direction-type></direction><direction><sound fine=\"yes\"/></direction><direction><sound dacapo=\"yes\"/></direction><direction><sound dalsegno=\"segno\"/></direction><direction><sound tocoda=\"coda\"/></direction><direction><direction-type><wedge type=\"crescendo\"/></direction-type></direction><direction><direction-type><wedge type=\"diminuendo\"/></direction-type></direction><direction><direction-type><wedge type=\"stop\"/></direction-type></direction><direction><direction-type><dynamics><mf/></dynamics></direction-type></direction><direction><direction-type><dynamics><sfz/></dynamics></direction-type></direction><note><pitch><step>D</step><alter>1</alter><octave>5</octave></pitch><duration>480</duration><voice>12</voice><staff>2</staff><lyric><syllabic>begin</syllabic><text>la &amp; le</text><extend/></lyric><type>eighth</type><time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification><accidental editorial=\"yes\" cautionary=\"yes\">sharp</accidental><tie type=\"start\"/><notations><tied type=\"start\"/></notations></note><note><rest/><duration>960</duration><voice>2</voice><staff>2</staff><type>quarter</type></note>",
                AbcIo.buildAbcMeasureNotesXml(Arrays.asList(pitched, rest), 3840, "whole", 960, Integer.valueOf(2)));
    }

    @Test
    public void buildsAbcBeamXmlByNoteIndex() {
        List<AbcIo.AbcMeasureNote> notes = Arrays.asList(
                new AbcIo.AbcMeasureNote("V1", 480, false, false, false, "C", Integer.valueOf(4),
                        Integer.valueOf(0), "eighth"),
                new AbcIo.AbcMeasureNote("V1", 480, false, false, false, "D", Integer.valueOf(4),
                        Integer.valueOf(0), "eighth"),
                new AbcIo.AbcMeasureNote("V1", 480, false, false, false, "E", Integer.valueOf(4),
                        Integer.valueOf(0), "eighth"),
                new AbcIo.AbcMeasureNote("V1", 480, false, false, false, "F", Integer.valueOf(4),
                        Integer.valueOf(0), "eighth"));

        Map<Integer, String> beams = AbcIo.buildAbcBeamXmlByNoteIndex(notes, 960);
        assertEquals("<beam number=\"1\">begin</beam>", beams.get(Integer.valueOf(0)));
        assertEquals("<beam number=\"1\">end</beam>", beams.get(Integer.valueOf(1)));
        assertEquals("<beam number=\"1\">begin</beam>", beams.get(Integer.valueOf(2)));
        assertEquals("<beam number=\"1\">end</beam>", beams.get(Integer.valueOf(3)));
        assertEquals(true, AbcIo.buildAbcMeasureNotesXml(notes, 3840, "whole", 960, null)
                .contains("<beam number=\"1\">begin</beam>"));

        List<AbcIo.AbcMeasureNote> explicit = Arrays.asList(
                new AbcIo.AbcMeasureNote("V2", 240, false, false, false, "C", Integer.valueOf(4),
                        Integer.valueOf(0), "16th", null, "", false, false, false, false, false, "begin"),
                new AbcIo.AbcMeasureNote("V2", 240, false, false, false, "D", Integer.valueOf(4),
                        Integer.valueOf(0), "16th", null, "", false, false, false, false, false, "mid"));
        Map<Integer, String> explicitBeams = AbcIo.buildAbcBeamXmlByNoteIndex(explicit, 960);
        assertEquals("<beam number=\"1\">begin</beam><beam number=\"2\">begin</beam>",
                explicitBeams.get(Integer.valueOf(0)));
        assertEquals("<beam number=\"1\">end</beam><beam number=\"2\">end</beam>",
                explicitBeams.get(Integer.valueOf(1)));
    }

    private static void assertFraction(int num, int den, AbcIo.Fraction actual) {
        assertEquals(num, actual.getNum());
        assertEquals(den, actual.getDen());
    }
}
