/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
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
    public void buildsHarmonyXmlFromAbcChordSymbols() {
        assertEquals("C7/G", AbcIo.normalizeChordToken(" C 7 / G "));
        assertEquals("A'b", AbcIo.abcQuotedTextEscape(" A\"b "));
        assertEquals(true, AbcIo.isLikelyAbcChordSymbol("F#m7b5/C#"));
        assertEquals(false, AbcIo.isLikelyAbcChordSymbol("not a chord"));
        assertEquals("half-diminished", AbcIo.xmlHarmonyKindFromChordSuffix("m7b5"));
        assertEquals("",
                AbcIo.buildHarmonyXmlFromChordSymbol("Cunknown"));
        assertEquals(
                "<harmony><root><root-step>F</root-step><root-alter>1</root-alter></root>"
                        + "<bass><bass-step>C</bass-step><bass-alter>1</bass-alter></bass>"
                        + "<kind text=\"F#m7b5/C#\">half-diminished</kind></harmony>",
                AbcIo.buildHarmonyXmlFromChordSymbol("F#m7b5/C#"));

        AbcIo.AbcMeasureNote note = new AbcIo.AbcMeasureNote("V1", 480, false, false);
        note.getChordSymbols().add("Bbmaj7/F");
        assertEquals(
                "<harmony><root><root-step>B</root-step><root-alter>-1</root-alter></root>"
                        + "<bass><bass-step>F</bass-step></bass>"
                        + "<kind text=\"Bbmaj7/F\">major-seventh</kind></harmony>",
                AbcIo.buildAbcNoteHarmonyAndWordsDirectionXml(note));

        AbcIo.AbcMeasureNote chordTone = new AbcIo.AbcMeasureNote("V1", 480, true, false);
        chordTone.getChordSymbols().add("C");
        assertEquals("", AbcIo.buildAbcNoteHarmonyAndWordsDirectionXml(chordTone));
    }

    @Test
    public void readsAbcTokensFromMusicXmlHarmonyAndLyrics() throws Exception {
        assertEquals("C'7 / G", AbcIo.abcChordSymbolFromHarmony(parseElement(
                "<harmony><root><root-step>C</root-step></root><kind text=\" C&quot;7 / G \">dominant</kind></harmony>")));
        assertEquals("F#m7b5/C#", AbcIo.abcChordSymbolFromHarmony(parseElement(
                "<harmony><root><root-step>F</root-step><root-alter>1</root-alter></root>"
                        + "<kind>half-diminished</kind><bass><bass-step>C</bass-step><bass-alter>1</bass-alter></bass></harmony>")));
        assertEquals("Bbdim", AbcIo.abcChordSymbolFromHarmony(parseElement(
                "<harmony><root><root-step>B</root-step><root-alter>-1</root-alter></root><kind>diminished</kind></harmony>")));
        assertEquals("", AbcIo.abcChordSymbolFromHarmony(parseElement(
                "<harmony><root><root-step>H</root-step></root><kind>major</kind></harmony>")));

        assertEquals("hello~world-", AbcIo.abcLyricTokenFromMusicXml("hello world", "begin"));
        assertEquals("middle-", AbcIo.abcLyricTokenFromMusicXml("middle", "middle"));
        assertEquals("end", AbcIo.abcLyricTokenFromMusicXml("end", "end"));
        assertEquals("*", AbcIo.abcLyricTokenFromMusicXml("   ", "single"));
    }

    @Test
    public void readsMusicXmlToAbcDomUtilityValues() throws Exception {
        assertEquals("bass", AbcIo.abcClefFromMusicXmlPart(parseElement(
                "<part><measure><attributes><clef><sign>F</sign><line>4</line></clef></attributes></measure></part>")));
        assertEquals("treble", AbcIo.abcClefFromMusicXmlPart(parseElement(
                "<part><measure><attributes><clef><sign>G</sign><line>2</line></clef></attributes></measure></part>")));
        assertEquals("alto", AbcIo.abcClefFromMusicXmlPart(parseElement(
                "<part><measure><attributes><clef><sign>C</sign><line>3</line></clef></attributes></measure></part>")));
        assertEquals("tenor", AbcIo.abcClefFromMusicXmlPart(parseElement(
                "<part><measure><attributes><clef><sign>C</sign><line>4</line></clef></attributes></measure></part>")));
        assertEquals("", AbcIo.abcClefFromMusicXmlPart(parseElement("<part><measure/></part>")));

        assertEquals(Integer.valueOf(1), AbcIo.accidentalTextToAlter("sharp"));
        assertEquals(Integer.valueOf(-1), AbcIo.accidentalTextToAlter("flat"));
        assertEquals(Integer.valueOf(0), AbcIo.accidentalTextToAlter("natural"));
        assertEquals(Integer.valueOf(2), AbcIo.accidentalTextToAlter("double-sharp"));
        assertEquals(Integer.valueOf(-2), AbcIo.accidentalTextToAlter("flat-flat"));
        assertNull(AbcIo.accidentalTextToAlter("quarter-flat"));

        assertEquals(Double.valueOf(12.5), AbcIo.parseOptionalNumber("12.5"));
        assertNull(AbcIo.parseOptionalNumber(""));
        assertNull(AbcIo.parseOptionalNumber("bad"));
    }

    @Test
    public void collectsMusicXmlPartLaneDefinitions() throws Exception {
        Element part = parseElement("<part id=\"P&amp;bad\">"
                + "<measure><attributes>"
                + "<clef number=\"2\"><sign>F</sign><line>4</line></clef>"
                + "<clef number=\"1\"><sign>G</sign><line>2</line></clef>"
                + "</attributes>"
                + "<note><voice>2</voice><staff>1</staff></note>"
                + "<note><voice>1</voice><staff>1</staff></note>"
                + "<note><voice>1</voice><staff>2</staff></note>"
                + "</measure></part>");

        List<AbcIo.AbcMusicXmlLaneDef> lanes = AbcIo.collectMusicXmlPartLaneDefs(part, "P&bad", "Piano");
        assertEquals(3, lanes.size());
        assertEquals("1", lanes.get(0).getStaff());
        assertEquals("1", lanes.get(0).getVoice());
        assertEquals("P&bad_s1_v1", lanes.get(0).getVoiceId());
        assertEquals("P_bad_s1_v1", lanes.get(0).getNormalizedVoiceId());
        assertEquals("Piano (Staff 1 Voice 1)", lanes.get(0).getLaneName());
        assertEquals("treble", lanes.get(0).getClef());
        assertEquals("2", lanes.get(1).getVoice());
        assertEquals("P_bad_s1_v2", lanes.get(1).getNormalizedVoiceId());
        assertEquals("2", lanes.get(2).getStaff());
        assertEquals("bass", lanes.get(2).getClef());

        List<AbcIo.AbcMusicXmlLaneDef> fallback = AbcIo.collectMusicXmlPartLaneDefs(
                parseElement("<part><measure/></part>"), "P1", "Solo");
        assertEquals(1, fallback.size());
        assertNull(fallback.get(0).getStaff());
        assertNull(fallback.get(0).getVoice());
        assertEquals("P1", fallback.get(0).getVoiceId());
        assertEquals("Solo", fallback.get(0).getLaneName());
    }

    @Test
    public void buildsMusicXmlToAbcMetaLines() throws Exception {
        Element part = parseElement("<part><measure><attributes><transpose>"
                + "<chromatic>-2.4</chromatic><diatonic>-1.2</diatonic>"
                + "</transpose></attributes></measure>"
                + "<measure><attributes><transpose><chromatic>7</chromatic></transpose></attributes></measure></part>");
        assertEquals(Arrays.asList("%@mks transpose voice=V1 chromatic=-2 diatonic=-1"),
                AbcIo.buildMusicXmlPartTransposeMetaLines(part, "V1"));

        Element implicitRepeatMeasure = parseElement("<measure number=\"pickup\" implicit=\"yes\">"
                + "<barline location=\"right\"><ending number=\"2\" type=\"discontinue\"/>"
                + "<repeat direction=\"backward\" times=\"4\"/></barline></measure>");
        assertEquals(Arrays.asList(
                "%@mks measure voice=V1 measure=1 number=pickup implicit=1 times=4 ending-stop=2 ending-type=discontinue"),
                AbcIo.buildMusicXmlMeasureMetaLines("V1", implicitRepeatMeasure, 1));
        assertEquals(new ArrayList<String>(), AbcIo.buildMusicXmlMeasureMetaLines("V1",
                parseElement("<measure number=\"2\"><barline location=\"right\"><repeat direction=\"backward\" times=\"2\"/></barline></measure>"),
                2));

        Element measure = parseElement("<measure><attributes><miscellaneous>"
                + "<miscellaneous-field name=\"mks:diag:0002\">second value</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:src:abc:raw-length\">ignored</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0001\">warn &amp; move</miscellaneous-field>"
                + "</miscellaneous></attributes></measure>");
        assertEquals(Arrays.asList(
                "%@mks diag voice=V1 measure=3 name=mks:diag:count enc=uri-v1 value=2",
                "%@mks diag voice=V1 measure=3 name=mks:diag:0001 enc=uri-v1 value=warn%20%26%20move",
                "%@mks diag voice=V1 measure=3 name=mks:diag:0002 enc=uri-v1 value=second%20value"),
                AbcIo.buildMusicXmlMeasureDiagMetaLines("V1", measure, 3));
    }

    @Test
    public void updatesMusicXmlToAbcMeasureState() throws Exception {
        AbcIo.AbcMusicXmlMeasureState state = AbcIo.updateMusicXmlMeasureState(parseElement(
                "<measure><attributes><divisions>960</divisions><key><fifths>-2.4</fifths></key>"
                        + "<time><beats>6</beats><beat-type>8</beat-type></time></attributes></measure>"),
                480, 0, 4, 4, Integer.valueOf(0));

        assertEquals(Double.valueOf(960), Double.valueOf(state.getDivisions()));
        assertEquals(-2, state.getFifths());
        assertEquals(Double.valueOf(6), Double.valueOf(state.getBeats()));
        assertEquals(Double.valueOf(8), Double.valueOf(state.getBeatType()));
        assertEquals(true, state.isNeedsInlineKeyChange());
        assertEquals(Integer.valueOf(-1), state.getKeyAlterByStep().get("B"));
        assertEquals(Integer.valueOf(-1), state.getKeyAlterByStep().get("E"));
        assertEquals(Integer.valueOf(0), state.getKeyAlterByStep().get("A"));
        assertEquals(true, state.getMeasureAccidentalByStepOctave().isEmpty());

        AbcIo.AbcMusicXmlMeasureState unchanged = AbcIo.updateMusicXmlMeasureState(
                parseElement("<measure><attributes><divisions>0</divisions><time><beats>bad</beats></time></attributes></measure>"),
                480, 1, 3, 4, Integer.valueOf(1));
        assertEquals(Double.valueOf(480), Double.valueOf(unchanged.getDivisions()));
        assertEquals(1, unchanged.getFifths());
        assertEquals(Double.valueOf(3), Double.valueOf(unchanged.getBeats()));
        assertEquals(Double.valueOf(4), Double.valueOf(unchanged.getBeatType()));
        assertEquals(false, unchanged.isNeedsInlineKeyChange());
    }

    @Test
    public void collectsMusicXmlToAbcDirectionTokens() throws Exception {
        Element direction = parseElement("<direction>"
                + "<direction-type><rehearsal>A &amp; B</rehearsal><words>go now</words><segno/><coda/>"
                + "<wedge type=\"diminuendo\"/><dynamics><mf/><sfz/></dynamics></direction-type>"
                + "<sound fine=\"yes\" dacapo=\"yes\" tocoda=\"coda\" dalsegno=\"segno\"/>"
                + "</direction>");
        AbcIo.AbcMusicXmlDirectionTokens tokens = AbcIo.collectMusicXmlDirectionTokens(direction, "");

        assertEquals(Arrays.asList("go now"), tokens.getWords());
        assertEquals(Arrays.asList("!rehearsal:A & B!", "!segno!", "!coda!", "!diminuendo(!", "!mf!", "!sfz!",
                "!fine!", "!dacoda!", "!dalsegno!"), tokens.getDecorations());
        assertEquals("diminuendo", tokens.getActiveWedgeType());

        AbcIo.AbcMusicXmlDirectionTokens stop = AbcIo.collectMusicXmlDirectionTokens(parseElement(
                "<direction><direction-type><wedge type=\"stop\"/></direction-type><sound tocoda=\"coda\"/></direction>"),
                "diminuendo");
        assertEquals(Arrays.asList("!diminuendo)!", "!tocoda!"), stop.getDecorations());
        assertEquals("", stop.getActiveWedgeType());
    }

    @Test
    public void resolvesMusicXmlToAbcNoteLaneAndTiming() throws Exception {
        AbcIo.AbcMusicXmlLaneDef lane = new AbcIo.AbcMusicXmlLaneDef("2", "1", "P1_s2_v1", "P1_s2_v1",
                "Piano (Staff 2 Voice 1)", "bass");
        Element matchingNote = parseElement(
                "<note><voice>1</voice><staff>2</staff><duration>240</duration><pitch><step>C</step></pitch></note>");
        Element defaultVoiceNote = parseElement(
                "<note><staff>2</staff><duration>240</duration><pitch><step>C</step></pitch></note>");
        Element otherStaffNote = parseElement(
                "<note><voice>1</voice><staff>1</staff><duration>240</duration><pitch><step>C</step></pitch></note>");

        assertEquals(true, AbcIo.isMusicXmlNoteInLane(matchingNote, lane));
        assertEquals(true, AbcIo.isMusicXmlNoteInLane(defaultVoiceNote, lane));
        assertEquals(false, AbcIo.isMusicXmlNoteInLane(otherStaffNote, lane));

        AbcIo.AbcMusicXmlNoteTiming timing = AbcIo.resolveMusicXmlNoteTiming(matchingNote, 480);
        assertEquals(false, timing.isChord());
        assertEquals(false, timing.isGrace());
        assertEquals(240, timing.getDuration());
        assertEquals(true, timing.isPlayable());

        AbcIo.AbcMusicXmlNoteTiming graceFallback = AbcIo.resolveMusicXmlNoteTiming(
                parseElement("<note><grace/><pitch><step>D</step></pitch></note>"), 480);
        assertEquals(true, graceFallback.isGrace());
        assertEquals(240, graceFallback.getDuration());
        assertEquals(true, graceFallback.isPlayable());

        AbcIo.AbcMusicXmlNoteTiming skipped = AbcIo.resolveMusicXmlNoteTiming(
                parseElement("<note><pitch><step>E</step></pitch></note>"), 480);
        assertEquals(false, skipped.isPlayable());
    }

    @Test
    public void collectsMusicXmlToAbcNoteOrnaments() throws Exception {
        Element note = parseElement("<note><notations>"
                + "<ornaments><wavy-line type=\"start\"/><wavy-line type=\"stop\"/>"
                + "<inverted-turn slash=\"yes\"/><delayed-turn/><mordent/>"
                + "<tremolo type=\"start\">12</tremolo><accidental-mark>sharp</accidental-mark>"
                + "<schleifer/><shake/></ornaments>"
                + "<glissando type=\"start\"/><glissando type=\"stop\"/>"
                + "<slide type=\"start\"/><slide type=\"stop\"/><arpeggiate/>"
                + "</notations></note>");
        AbcIo.AbcMusicXmlNoteOrnaments ornaments = AbcIo.collectMusicXmlNoteOrnaments(note);

        assertEquals(true, ornaments.isTrill());
        assertEquals(true, ornaments.isWavyLineStart());
        assertEquals(true, ornaments.isWavyLineStop());
        assertEquals("sharp", ornaments.getTrillAccidentalText());
        assertEquals("inverted-turn", ornaments.getTurnType());
        assertEquals(true, ornaments.isTurnSlash());
        assertEquals(true, ornaments.isDelayedTurn());
        assertEquals("mordent", ornaments.getMordentType());
        assertEquals("start", ornaments.getTremoloType());
        assertEquals(Integer.valueOf(8), ornaments.getTremoloMarks());
        assertEquals(true, ornaments.isGlissandoStart());
        assertEquals(true, ornaments.isGlissandoStop());
        assertEquals(true, ornaments.isSlideStart());
        assertEquals(true, ornaments.isSlideStop());
        assertEquals(true, ornaments.isSchleifer());
        assertEquals(true, ornaments.isShake());
        assertEquals(true, ornaments.isArpeggiate());
        assertEquals("!trill)!!delayedinvertedturn!!mordent!!tremolo-start-8!!gliss-start!!slide!!schleifer!!shake!!arpeggio!",
                AbcIo.buildMusicXmlNoteOrnamentPrefix(ornaments));

        AbcIo.AbcMusicXmlNoteOrnaments none = AbcIo.collectMusicXmlNoteOrnaments(parseElement("<note/>"));
        assertEquals(false, none.isTrill());
        assertEquals("", none.getTurnType());
        assertEquals("", none.getTremoloType());
        assertEquals(Integer.valueOf(1), none.getTremoloMarks());
        assertEquals("", AbcIo.buildMusicXmlNoteOrnamentPrefix(none));

        assertEquals("!trill(!", AbcIo.buildMusicXmlNoteOrnamentPrefix(AbcIo.collectMusicXmlNoteOrnaments(
                parseElement("<note><notations><ornaments><trill-mark/><wavy-line type=\"start\"/></ornaments></notations></note>"))));
        assertEquals("!delayedturn!!pralltriller!!gliss-stop!!slide-stop!", AbcIo.buildMusicXmlNoteOrnamentPrefix(
                AbcIo.collectMusicXmlNoteOrnaments(parseElement("<note><notations><ornaments><turn/><delayed-turn/>"
                        + "<inverted-mordent/></ornaments><glissando type=\"stop\"/><slide type=\"stop\"/>"
                        + "</notations></note>"))));
    }

    @Test
    public void resolvesMusicXmlToAbcPitchTokens() throws Exception {
        Map<String, Integer> keyAlterByStep = AbcIo.keySignatureAlterByStep(1);
        Map<String, Integer> measureAccidentals = new LinkedHashMap<String, Integer>();

        AbcIo.AbcMusicXmlPitchToken sharp = AbcIo.resolveMusicXmlNotePitchToken(parseElement(
                "<note><pitch><step>C</step><octave>4</octave><alter>1</alter></pitch>"
                        + "<accidental editorial=\"yes\">sharp</accidental></note>"),
                keyAlterByStep, measureAccidentals);
        assertEquals("!editorial!^C", sharp.getToken());
        assertEquals("C4", sharp.getStepOctaveKey());
        assertEquals(1, sharp.getTargetAlter());
        assertEquals(Integer.valueOf(1), measureAccidentals.get("C4"));
        assertEquals(true, sharp.isAccidentalEditorial());

        AbcIo.AbcMusicXmlPitchToken natural = AbcIo.resolveMusicXmlNotePitchToken(parseElement(
                "<note><pitch><step>F</step><octave>4</octave></pitch></note>"),
                keyAlterByStep, measureAccidentals);
        assertEquals("=F", natural.getToken());
        assertEquals(Integer.valueOf(0), measureAccidentals.get("F4"));

        AbcIo.AbcMusicXmlPitchToken courtesy = AbcIo.resolveMusicXmlNotePitchToken(parseElement(
                "<note><pitch><step>G</step><octave>5</octave></pitch><accidental cautionary=\"yes\">sharp</accidental></note>"),
                keyAlterByStep, measureAccidentals);
        assertEquals("!courtesy!^g", courtesy.getToken());
        assertEquals(true, courtesy.isAccidentalCautionary());

        AbcIo.AbcMusicXmlPitchToken rest = AbcIo.resolveMusicXmlNotePitchToken(parseElement("<note><rest/></note>"),
                keyAlterByStep, measureAccidentals);
        assertEquals("z", rest.getToken());
    }

    @Test
    public void collectsMusicXmlToAbcNoteArticulations() throws Exception {
        Element note = parseElement("<note><notations><articulations>"
                + "<staccato/><staccatissimo/><accent/><tenuto/><stress/><unstress/>"
                + "<strong-accent/><breath-mark/><caesura/>"
                + "<other-articulation>ignored</other-articulation>"
                + "<other-articulation>mediumphrase</other-articulation>"
                + "</articulations></notations></note>");
        AbcIo.AbcMusicXmlNoteArticulations articulations = AbcIo.collectMusicXmlNoteArticulations(note);

        assertEquals(true, articulations.isStaccato());
        assertEquals(true, articulations.isStaccatissimo());
        assertEquals(true, articulations.isAccent());
        assertEquals(true, articulations.isTenuto());
        assertEquals(true, articulations.isStress());
        assertEquals(true, articulations.isUnstress());
        assertEquals(true, articulations.isStrongAccent());
        assertEquals(true, articulations.isBreathMark());
        assertEquals(true, articulations.isCaesura());
        assertEquals("mediumphrase", articulations.getPhraseMarkText());
        assertEquals("!wedge!!accent!!tenuto!!stress!!unstress!!marcato!!breath!!caesura!!mediumphrase!",
                AbcIo.buildMusicXmlNoteArticulationPrefix(articulations));

        AbcIo.AbcMusicXmlNoteArticulations staccatoOnly = AbcIo.collectMusicXmlNoteArticulations(
                parseElement("<note><notations><articulations><staccato/></articulations></notations></note>"));
        assertEquals("!staccato!", AbcIo.buildMusicXmlNoteArticulationPrefix(staccatoOnly));
        assertEquals("", AbcIo.buildMusicXmlNoteArticulationPrefix(
                AbcIo.collectMusicXmlNoteArticulations(parseElement("<note/>"))));
    }

    @Test
    public void collectsMusicXmlToAbcNoteTechnical() throws Exception {
        Element note = parseElement("<note><notations><technical>"
                + "<up-bow/><down-bow/><double-tongue/><triple-tongue/><heel/><toe/>"
                + "<fingering>2</fingering><fingering>x</fingering><string>3</string><pluck>pizz</pluck>"
                + "<open-string/><snap-pizzicato/><harmonic/><stopped/><thumb-position/>"
                + "</technical></notations></note>");
        AbcIo.AbcMusicXmlNoteTechnical technical = AbcIo.collectMusicXmlNoteTechnical(note);

        assertEquals(true, technical.isUpBow());
        assertEquals(true, technical.isDownBow());
        assertEquals(true, technical.isDoubleTongue());
        assertEquals(true, technical.isTripleTongue());
        assertEquals(true, technical.isHeel());
        assertEquals(true, technical.isToe());
        assertEquals(Arrays.asList("2", "x"), technical.getFingerings());
        assertEquals(Arrays.asList("3"), technical.getStrings());
        assertEquals(Arrays.asList("pizz"), technical.getPlucks());
        assertEquals(true, technical.isOpenString());
        assertEquals(true, technical.isSnapPizzicato());
        assertEquals(true, technical.isHarmonic());
        assertEquals(true, technical.isStopped());
        assertEquals(true, technical.isThumbPosition());
        assertEquals("!upbow!!downbow!!doubletongue!!tripletongue!!heel!!toe!!2!!fingering:x!"
                + "!string:3!!pluck:pizz!!open!!snap!!harmonic!!stopped!!thumb!",
                AbcIo.buildMusicXmlNoteTechnicalPrefix(technical));

        assertEquals("", AbcIo.buildMusicXmlNoteTechnicalPrefix(
                AbcIo.collectMusicXmlNoteTechnical(parseElement("<note/>"))));
    }

    @Test
    public void buildsAbcNoteNotationDecorationSubset() {
        AbcIo.AbcMeasureNote decorated = new AbcIo.AbcMeasureNote("V1", 240, false, false, false, "E",
                Integer.valueOf(5), Integer.valueOf(0), "16th", null, "", false, false, true, false, false,
                "", "", "single", false, null, null, Arrays.asList("ornamented"), false, false, "", false, false,
                false, false, false, false, false, false, "", false, true, true, true, false, true, true, false,
                "sharp", "inverted-turn", true, true, "inverted-mordent", "start", Integer.valueOf(9), true,
                true, true, true, true, true, true, true, true, true, true, true, true, "inverted", true, true,
                true, "short <phrase>", true, true, true, true, true, true, Arrays.asList("1", "2&"),
                Arrays.asList("3"), Arrays.asList("pizz <x>"), true, true, true, true, true);

        assertEquals(
                "<ornaments><trill-mark/><wavy-line type=\"start\"/><accidental-mark>sharp</accidental-mark></ornaments>"
                        + "<ornaments><inverted-turn slash=\"yes\"/><delayed-turn/></ornaments>"
                        + "<ornaments><inverted-mordent/></ornaments>"
                        + "<ornaments><tremolo type=\"start\">8</tremolo></ornaments>"
                        + "<glissando type=\"start\" number=\"1\">wavy</glissando>"
                        + "<glissando type=\"stop\" number=\"1\">wavy</glissando>"
                        + "<slide type=\"start\" number=\"1\"/><slide type=\"stop\" number=\"1\"/>"
                        + "<ornaments><schleifer/></ornaments><ornaments><shake/></ornaments><arpeggiate/>",
                AbcIo.buildAbcNoteOrnamentsXml(decorated));
        assertEquals(
                "<articulations><staccato/><staccatissimo/><accent/><tenuto/><stress/><unstress/>"
                        + "<strong-accent/><breath-mark/><caesura/>"
                        + "<other-articulation>short &lt;phrase&gt;</other-articulation></articulations>",
                AbcIo.buildAbcNoteArticulationsXml(decorated));
        assertEquals(
                "<technical><up-bow/><down-bow/><double-tongue/><triple-tongue/><heel/><toe/>"
                        + "<fingering>1</fingering><fingering>2&amp;</fingering><string>3</string>"
                        + "<pluck>pizz &lt;x&gt;</pluck><open-string/><snap-pizzicato/><harmonic/>"
                        + "<stopped/><thumb-position/></technical>",
                AbcIo.buildAbcNoteTechnicalXml(decorated));
        assertEquals(true, AbcIo.buildAbcNoteNotationsXml(decorated)
                .contains("<slur type=\"start\"/><slur type=\"stop\"/><tuplet type=\"start\"/>"));
        assertEquals(true, AbcIo.buildAbcNoteXml(decorated, 0, null, new LinkedHashMap<Integer, String>())
                .contains("<fermata>inverted</fermata></notations></note>"));
    }

    @Test
    public void managesAbcVoiceStoresForBodyImport() {
        AbcIo.AbcVoiceStores stores = AbcIo.createAbcVoiceStores();
        assertEquals(true, stores.getMeasuresByVoice().isEmpty());
        assertEquals(true, stores.getNotationMeasureMetaByVoice().isEmpty());

        List<List<AbcIo.AbcMeasureNote>> measures = AbcIo.ensureAbcVoiceMeasures(stores, "V1");
        assertEquals(1, measures.size());
        assertEquals(true, measures.get(0).isEmpty());
        measures.get(0).add(new AbcIo.AbcMeasureNote("V1", 480, false, false));
        measures.add(new ArrayList<AbcIo.AbcMeasureNote>());
        measures.add(new ArrayList<AbcIo.AbcMeasureNote>());

        AbcIo.AbcMeasureMeta firstMeta = AbcIo.ensureAbcNotationMeasureMeta(stores, "V1", 1);
        assertEquals("1", firstMeta.getNumber());
        assertEquals(false, firstMeta.isRepeatStart());

        Map<Integer, AbcIo.AbcMeter> meterByMeasure = AbcIo.ensureAbcMeterByMeasure(stores, "V1");
        meterByMeasure.put(Integer.valueOf(2), new AbcIo.AbcMeter(3, 4));
        assertEquals(3, stores.getMeterByMeasureByVoice().get("V1").get(Integer.valueOf(2)).getBeats());

        Map<Integer, Integer> tempoByMeasure = AbcIo.ensureAbcTempoByMeasure(stores, "V1");
        tempoByMeasure.put(Integer.valueOf(2), Integer.valueOf(132));
        assertEquals(Integer.valueOf(132), stores.getTempoByMeasureByVoice().get("V1").get(Integer.valueOf(2)));

        stores.getCurrentKeyFifthsByVoice().put("V1", Integer.valueOf(2));
        stores.getActiveEndingByVoice().put("V1", "1");
        AbcIo.finalizeAbcActiveEndings(stores);
        assertEquals(1, stores.getMeasuresByVoice().get("V1").size());
        assertEquals("1", stores.getNotationMeasureMetaByVoice().get("V1").get(Integer.valueOf(1)).getEndingStop());
        assertEquals("stop", stores.getNotationMeasureMetaByVoice().get("V1").get(Integer.valueOf(1)).getEndingStopType());
        assertEquals(Integer.valueOf(2), stores.getCurrentKeyFifthsByVoice().get("V1"));
    }

    @Test
    public void appliesAbcLyricsToMeasures() {
        List<AbcIo.AbcLyricToken> tokens = AbcIo.tokenizeAbcLyricLine("hel-lo mid- dle _ * tail|end");
        assertEquals("text", tokens.get(0).getType());
        assertEquals("hel", tokens.get(0).getText());
        assertEquals("begin", tokens.get(0).getSyllabic());
        assertEquals("extend", tokens.get(4).getType());
        assertEquals("skip", tokens.get(5).getType());

        AbcIo.AbcMeasureNote first = new AbcIo.AbcMeasureNote("V1", 480, false, false);
        AbcIo.AbcMeasureNote rest = new AbcIo.AbcMeasureNote("V1", 480, false, false, true, "C",
                Integer.valueOf(4), Integer.valueOf(0), "quarter");
        AbcIo.AbcMeasureNote chord = new AbcIo.AbcMeasureNote("V1", 480, true, false);
        AbcIo.AbcMeasureNote second = new AbcIo.AbcMeasureNote("V1", 480, false, false);
        Map<String, List<List<AbcIo.AbcMeasureNote>>> measuresByVoice =
                new LinkedHashMap<String, List<List<AbcIo.AbcMeasureNote>>>();
        measuresByVoice.put("V1", Arrays.asList(Arrays.asList(first, rest, chord, second)));
        Map<String, List<AbcIo.AbcLyricEntry>> lyricsByVoice =
                new LinkedHashMap<String, List<AbcIo.AbcLyricEntry>>();
        lyricsByVoice.put("V1", Arrays.asList(new AbcIo.AbcLyricEntry("hel- _ lo", 3)));

        AbcIo.applyAbcLyricsToMeasures(lyricsByVoice, measuresByVoice);

        assertEquals("hel", first.getLyricText());
        assertEquals("begin", first.getLyricSyllabic());
        assertEquals(true, first.isLyricExtend());
        assertEquals("lo", second.getLyricText());
        assertEquals("end", second.getLyricSyllabic());
        assertEquals(false, second.isLyricExtend());
        assertEquals("", rest.getLyricText());
    }

    @Test
    public void appliesAbcBodyFieldsForBodyImportState() {
        AbcIo.AbcVoiceStores stores = AbcIo.createAbcVoiceStores();
        Map<String, Integer> keyHints = new LinkedHashMap<String, Integer>();
        Map<String, Integer> accidentals = new LinkedHashMap<String, Integer>();
        accidentals.put("F", Integer.valueOf(1));
        List<String> warnings = new ArrayList<String>();
        AbcIo.AbcBodyFieldContext context = new AbcIo.AbcBodyFieldContext(0, new AbcIo.Fraction(1, 8),
                new AbcIo.AbcMeter(4, 4), null, accidentals, stores, "V1", 2, keyHints, warnings);

        AbcIo.AbcBodyFieldResult keyResult = AbcIo.applyAbcBodyField("K", "D", context);
        assertEquals(true, keyResult.isHandled());
        assertEquals(2, keyResult.getActiveKeyFifths());
        assertEquals(Integer.valueOf(1), keyResult.getActiveKeySignatureAccidentals().get("F"));
        assertEquals(Integer.valueOf(1), keyResult.getActiveKeySignatureAccidentals().get("C"));
        assertEquals(true, keyResult.getMeasureAccidentals().isEmpty());
        assertEquals(Integer.valueOf(2), stores.getCurrentKeyFifthsByVoice().get("V1"));
        assertEquals(Integer.valueOf(2), keyHints.get("V1#2"));

        AbcIo.AbcBodyFieldResult lengthResult = AbcIo.applyAbcBodyField("L", "1/16", context);
        assertFraction(1, 16, lengthResult.getActiveUnitLength());

        AbcIo.AbcBodyFieldResult meterResult = AbcIo.applyAbcBodyField("M", "3/8", context);
        assertEquals(3, meterResult.getActiveMeter().getBeats());
        assertEquals(8, stores.getMeterByMeasureByVoice().get("V1").get(Integer.valueOf(2)).getBeatType());

        AbcIo.AbcBodyFieldResult tempoResult = AbcIo.applyAbcBodyField("Q", "1/8=900", context);
        assertEquals(Integer.valueOf(300), tempoResult.getActiveTempoBpm());
        assertEquals(Integer.valueOf(300), stores.getTempoByMeasureByVoice().get("V1").get(Integer.valueOf(2)));

        AbcIo.AbcBodyFieldResult unsupported = AbcIo.applyAbcBodyField("X", "1", context);
        assertEquals(false, unsupported.isHandled());
    }

    @Test
    public void processesAbcBarlineEntriesForBodyImportState() {
        Map<String, Integer> accidentals = new LinkedHashMap<String, Integer>();
        accidentals.put("F", Integer.valueOf(1));
        AbcParser.AbcParsedBarlineToken repeatEnd = AbcParser.parseAbcBarlineTokenAt(":|1 A", 0);
        AbcIo.AbcBarlineEntryContext context = new AbcIo.AbcBarlineEntryContext(":|1 A", 0, "old", 1, 480, 1,
                accidentals);

        assertEquals(true, AbcIo.processAbcBarlineEntry(repeatEnd, context));

        assertEquals(true, context.isRepeatEndMarked());
        assertEquals(false, context.isRepeatStartMarked());
        assertEquals(true, context.isActiveEndingStopped());
        assertEquals(1, context.getStoppedEndingMeasureNo());
        assertEquals(true, context.isAdvancedToNextMeasure());
        assertEquals(2, context.getCurrentMeasureNo());
        assertEquals(true, context.isMeasureAccidentalsCleared());
        assertEquals(true, context.getMeasureAccidentals().isEmpty());
        assertEquals(true, context.isLastNoteCleared());
        assertEquals(true, context.isBeamContextReset());
        assertEquals("1", context.getStartedEndingMarker());
        assertEquals(3, context.getIdx());

        AbcIo.AbcBarlineEntryContext repeatStartContext = new AbcIo.AbcBarlineEntryContext("|: C", 0, "", 2, 0, 1,
                new LinkedHashMap<String, Integer>());
        assertEquals(true, AbcIo.processAbcBarlineEntry(AbcParser.parseAbcBarlineTokenAt("|: C", 0),
                repeatStartContext));
        assertEquals(true, repeatStartContext.isRepeatStartMarked());
        assertEquals(false, repeatStartContext.isRepeatEndMarked());
        assertEquals(false, repeatStartContext.isAdvancedToNextMeasure());
        assertEquals(2, repeatStartContext.getIdx());
    }

    @Test
    public void processesAbcNonPlayableBodyEntriesForBodyImportState() {
        List<String> warnings = new ArrayList<String>();
        final List<String> handled = new ArrayList<String>();
        AbcIo.AbcNonPlayableBodyEntryContext context = new AbcIo.AbcNonPlayableBodyEntryContext(0, warnings,
                barlineToken -> {
                    handled.add("barline:" + barlineToken.getNextIdx());
                    return true;
                },
                (fieldName, fieldValue) -> {
                    handled.add(fieldName + "=" + fieldValue);
                    return "M".equals(fieldName);
                });

        assertEquals(true, AbcIo.processAbcNonPlayableBodyEntry(AbcParser.parseAbcBodyEntryAt("| C", 0), context));
        assertEquals("barline:1", handled.get(0));

        assertEquals(true, AbcIo.processAbcNonPlayableBodyEntry(AbcParser.parseAbcBodyEntryAt("M:3/4 C", 0),
                context));
        assertEquals("M=3/4", handled.get(1));
        assertEquals(5, context.getIdx());

        assertEquals(true, AbcIo.processAbcNonPlayableBodyEntry(AbcParser.parseAbcBodyEntryAt("Z:ignored C", 0),
                context));
        assertEquals("Skipped unsupported standalone body field token: Z:ignored", warnings.get(0));
        assertEquals(9, context.getIdx());

        assertEquals(true, AbcIo.processAbcNonPlayableBodyEntry(AbcParser.parseAbcBodyEntryAt("yabc C", 0),
                context));
        assertEquals("Skipped unsupported body token: yabc", warnings.get(1));
        assertEquals(4, context.getIdx());

        assertEquals(true, AbcIo.processAbcNonPlayableBodyEntry(AbcParser.parseAbcBodyEntryAt("123 C", 0),
                context));
        assertEquals("Skipped unsupported body number token: 123", warnings.get(2));
        assertEquals(3, context.getIdx());

        assertEquals(false, AbcIo.processAbcNonPlayableBodyEntry(AbcParser.parseAbcBodyEntryAt("!trill!C", 0),
                context));
        assertEquals(false, AbcIo.processAbcNonPlayableBodyEntry(null, context));
    }

    @Test
    public void processesAbcSimpleBodyTokensForBodyImportState() {
        final List<String> calls = new ArrayList<String>();
        AbcIo.AbcSimpleBodyTokenHandlerContext context = new AbcIo.AbcSimpleBodyTokenHandlerContext("!",
                bodyToken -> {
                    calls.add("broken:" + bodyToken.getBrokenRhythm().getSymbol());
                    return true;
                },
                (bodyToken, ch) -> {
                    calls.add("decoration:" + bodyToken.getDecoration().getDecoration() + ":" + ch);
                    return true;
                },
                bodyToken -> {
                    calls.add("paren:" + bodyToken.getParenToken().getKind());
                    return true;
                },
                bodyToken -> {
                    calls.add("quoted:" + bodyToken.getQuotedString().getNormalizedText());
                    return true;
                },
                (bodyToken, ch) -> {
                    calls.add("shorthand:" + bodyToken.getShorthand().getKind() + ":" + ch);
                    return true;
                },
                bodyToken -> {
                    calls.add("slur-stop:" + bodyToken.getSlurStop().getNextIdx());
                    return true;
                },
                bodyToken -> {
                    calls.add("tie:" + bodyToken.getTie().getNextIdx());
                    return true;
                });

        assertEquals(true, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt(">A", 0), context));
        assertEquals(true, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt("!trill!C", 0), context));
        assertEquals(true, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt("(C", 0), context));
        assertEquals(true, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt("\"txt\"C", 0), context));
        assertEquals(true, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt(".C", 0), context));
        assertEquals(true, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt(")C", 0), context));
        assertEquals(true, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt("-C", 0), context));
        assertEquals(false, AbcIo.processAbcSimpleBodyToken(AbcParser.parseAbcBodyTokenAt("[K:C]", 0), context));
        assertEquals(false, AbcIo.processAbcSimpleBodyToken(null, context));

        assertEquals("broken:>", calls.get(0));
        assertEquals("decoration:trill:!", calls.get(1));
        assertEquals("paren:slur-start", calls.get(2));
        assertEquals("quoted:txt", calls.get(3));
        assertEquals("shorthand:staccato:!", calls.get(4));
        assertEquals("slur-stop:1", calls.get(5));
        assertEquals("tie:1", calls.get(6));
    }

    @Test
    public void processesAbcBracketGraceAndFallbackBodyTokens() {
        final List<String> bracketCalls = new ArrayList<String>();
        AbcIo.AbcBracketBodyTokenContext bracketContext = new AbcIo.AbcBracketBodyTokenContext("[K:G] [1 [CE]",
                8,
                bracketToken -> {
                    bracketCalls.add("inline:" + bracketToken.getInlineField().getFieldName());
                    return true;
                },
                bracketToken -> {
                    bracketCalls.add("ending:" + bracketToken.getRepeatEndingMarker().getMarker());
                    return true;
                },
                (playableEvent, fallbackToNextChar) -> {
                    bracketCalls.add("playable:" + playableEvent.getKind() + ":" + fallbackToNextChar);
                    return true;
                });
        assertEquals(true, AbcIo.processAbcBracketBodyToken(AbcParser.parseAbcBodyTokenAt("[K:G]", 0),
                bracketContext));
        assertEquals(true, AbcIo.processAbcBracketBodyToken(AbcParser.parseAbcBodyTokenAt("[1 A", 0),
                bracketContext));
        AbcIo.AbcBracketBodyTokenContext playableBracketContext = new AbcIo.AbcBracketBodyTokenContext("[CE]",
                0, bracketContext::handleInlineFieldBracketToken, bracketContext::handleRepeatEndingBracketToken,
                (playableEvent, fallbackToNextChar) -> {
                    bracketCalls.add("playable:" + playableEvent.getKind() + ":" + fallbackToNextChar);
                    return true;
                });
        assertEquals(true, AbcIo.processAbcBracketBodyToken(AbcParser.parseAbcBodyTokenAt("[CE]", 0),
                playableBracketContext));
        assertEquals(false, AbcIo.processAbcBracketBodyToken(AbcParser.parseAbcBodyTokenAt("A", 0),
                bracketContext));
        assertEquals("inline:K", bracketCalls.get(0));
        assertEquals("ending:1", bracketCalls.get(1));
        assertEquals("playable:playable:true", bracketCalls.get(2));

        final List<AbcParser.AbcParsedGraceNote> graceNotes = new ArrayList<AbcParser.AbcParsedGraceNote>();
        List<String> warnings = new ArrayList<String>();
        AbcIo.AbcGraceGroupProcessResult graceResult = AbcIo.processAbcGraceGroup(
                new AbcIo.AbcGraceGroupContext("{/c}A", 0, "{", 9, warnings, notes -> graceNotes.addAll(notes)));
        assertEquals(true, graceResult.isHandled());
        assertEquals(4, graceResult.getNextIdx());
        assertEquals(1, graceNotes.size());
        assertEquals(true, graceNotes.get(0).isGraceSlash());

        AbcIo.AbcGraceGroupProcessResult ignoredGrace = AbcIo.processAbcGraceGroup(
                new AbcIo.AbcGraceGroupContext("A", 0, "A", 9, warnings, notes -> graceNotes.addAll(notes)));
        assertEquals(false, ignoredGrace.isHandled());
        assertEquals(0, ignoredGrace.getNextIdx());

        AbcIo.AbcGraceGroupProcessResult failedGrace = AbcIo.processAbcGraceGroup(
                new AbcIo.AbcGraceGroupContext("{c", 0, "{", 9, warnings, notes -> graceNotes.addAll(notes)));
        assertEquals(true, failedGrace.isHandled());
        assertEquals(1, failedGrace.getNextIdx());
        assertEquals(true, warnings.get(warnings.size() - 1).contains("Failed to parse grace group"));

        final List<String> fallbackCalls = new ArrayList<String>();
        assertEquals(true, AbcIo.processAbcBodyFallback(new AbcIo.AbcBodyFallbackContext(")", null,
                ch -> {
                    fallbackCalls.add("closing:" + ch);
                    return true;
                },
                ch -> false,
                () -> {
                    throw new IllegalStateException("unexpected");
                })));
        assertEquals(true, AbcIo.processAbcBodyFallback(new AbcIo.AbcBodyFallbackContext("?", null,
                ch -> false,
                ch -> {
                    fallbackCalls.add("punct:" + ch);
                    return true;
                },
                () -> {
                    throw new IllegalStateException("unexpected");
                })));
        assertThrows(IllegalStateException.class, () -> AbcIo.processAbcBodyFallback(
                new AbcIo.AbcBodyFallbackContext("x", null, ch -> false, ch -> false, () -> {
                    throw new IllegalStateException("parse");
                })));
        assertEquals(false, AbcIo.processAbcBodyFallback(new AbcIo.AbcBodyFallbackContext("x",
                AbcParser.parseAbcBodyEntryAt("!trill!C", 0), ch -> false, ch -> false, null)));
        assertEquals("closing:)", fallbackCalls.get(0));
        assertEquals("punct:?", fallbackCalls.get(1));
    }

    @Test
    public void appliesAbcPendingNoteStateHelpers() {
        AbcIo.AbcMeasureNote note = new AbcIo.AbcMeasureNote("V1", 480, false, false, false, "C",
                Integer.valueOf(4), Integer.valueOf(0), "quarter");
        final List<String> calls = new ArrayList<String>();
        final boolean[] pendingTie = new boolean[] { true };
        AbcIo.applyAbcPendingStateToPlayableNote(new AbcIo.AbcPendingPlayableNoteContext(note,
                new AbcIo.AbcPendingPlayableNoteOptions(Boolean.FALSE, null, "wide"),
                (target, applySlurStart, trillHint) -> calls.add("ornament:" + applySlurStart + ":" + trillHint),
                target -> calls.add("articulation"), target -> calls.add("direction"),
                target -> calls.add("technical"), () -> pendingTie[0], () -> pendingTie[0] = false,
                message -> calls.add("warn:" + message)));
        assertEquals(true, note.isTieStop());
        assertEquals(false, pendingTie[0]);
        assertEquals("ornament:false:wide", calls.get(0));
        assertEquals("articulation", calls.get(1));
        assertEquals("direction", calls.get(2));
        assertEquals("technical", calls.get(3));

        AbcIo.AbcMeasureNote rest = new AbcIo.AbcMeasureNote("V1", 480, false, false, true, "C",
                Integer.valueOf(4), Integer.valueOf(0), "quarter");
        final boolean[] restPendingTie = new boolean[] { true };
        AbcIo.applyAbcPendingStateToPlayableNote(new AbcIo.AbcPendingPlayableNoteContext(rest,
                new AbcIo.AbcPendingPlayableNoteOptions(null, null, ""), null, null, null, null,
                () -> restPendingTie[0], () -> restPendingTie[0] = false, message -> calls.add("warn:" + message)));
        assertEquals(false, rest.isTieStop());
        assertEquals(false, restPendingTie[0]);
        assertEquals(true, calls.get(calls.size() - 1).contains("tie(-) was followed by a rest"));

        final int[] applied = new int[] { 0 };
        final int[] cleared = new int[] { 0 };
        AbcIo.applyAbcPendingNoteValue(new AbcIo.AbcPendingNoteValueContext(note, true,
                () -> applied[0] += 1, () -> cleared[0] += 1));
        AbcIo.applyAbcPendingNoteValue(new AbcIo.AbcPendingNoteValueContext(rest, true,
                () -> applied[0] += 10, () -> cleared[0] += 10));
        assertEquals(1, applied[0]);
        assertEquals(1, cleared[0]);

        final List<String> optionalValues = new ArrayList<String>();
        AbcIo.applyAbcPendingNoteOptionalValue(new AbcIo.AbcPendingNoteOptionalValueContext(note, "fermata",
                value -> value == null || value.toString().length() == 0,
                value -> optionalValues.add(value.toString()), () -> optionalValues.add("cleared")));
        AbcIo.applyAbcPendingNoteOptionalValue(new AbcIo.AbcPendingNoteOptionalValueContext(note, "",
                value -> value == null || value.toString().length() == 0,
                value -> optionalValues.add("bad"), () -> optionalValues.add("bad-clear")));
        assertEquals(Arrays.asList("fermata", "cleared"), optionalValues);

        final List<String> arrayValues = new ArrayList<String>();
        AbcIo.applyAbcPendingNoteArray(new AbcIo.AbcPendingNoteArrayContext(note, Arrays.asList("1", "2"),
                values -> {
                    for (Object value : values) {
                        arrayValues.add(String.valueOf(value));
                    }
                },
                () -> arrayValues.add("cleared")));
        AbcIo.applyAbcPendingNoteArray(new AbcIo.AbcPendingNoteArrayContext(note, new ArrayList<String>(),
                values -> arrayValues.add("bad"), () -> arrayValues.add("bad-clear")));
        assertEquals(Arrays.asList("1", "2", "cleared"), arrayValues);
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

    @Test
    public void parsesBasicAbcBodyAndBuildsMusicXml() {
        String abc = "X:1\nT:Basic ABC\nM:4/4\nL:1/4\nK:C\nC D E F|z2 [CE]2|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        assertEquals("Basic ABC", parsed.getMeta().getTitle());
        assertEquals(1, parsed.getParts().size());
        assertEquals(2, parsed.getParts().get(0).getMeasures().size());
        assertEquals(4, parsed.getParts().get(0).getMeasures().get(0).size());
        assertEquals(true, parsed.getParts().get(0).getMeasures().get(1).get(0).isRest());
        assertEquals(true, parsed.getParts().get(0).getMeasures().get(1).get(2).isChord());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<work-title>Basic ABC</work-title>"));
        assertEquals(true, xml.contains("<part-name>Voice 1</part-name>"));
        assertEquals(true, xml.contains("<step>C</step>"));
        assertEquals(true, xml.contains("<rest/>"));
        assertEquals(true, xml.contains("<chord/>"));
    }

    @Test
    public void parsesAbcTupletBodyIntoMusicXmlTiming() {
        String abc = "X:1\nT:Tuplet ABC\nM:4/4\nL:1/4\nK:C\n(3CDE F|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(4, notes.size());
        assertEquals(640, notes.get(0).getDuration());
        assertEquals(Integer.valueOf(3), notes.get(0).getTimeModificationActual());
        assertEquals(Integer.valueOf(2), notes.get(0).getTimeModificationNormal());
        assertEquals(true, notes.get(0).isTupletStart());
        assertEquals(true, notes.get(2).isTupletStop());
        assertEquals(960, notes.get(3).getDuration());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<actual-notes>3</actual-notes>"));
        assertEquals(true, xml.contains("<normal-notes>2</normal-notes>"));
        assertEquals(true, xml.contains("<tuplet type=\"start\"/>"));
        assertEquals(true, xml.contains("<tuplet type=\"stop\"/>"));
    }

    @Test
    public void parsesAbcGraceGroupIntoMusicXmlGraceNotes() {
        String abc = "X:1\nT:Grace ABC\nM:4/4\nL:1/4\nK:C\n{/c}D E F G|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(5, notes.size());
        assertEquals(true, notes.get(0).isGrace());
        assertEquals(true, notes.get(0).isGraceSlash());
        assertEquals("C", notes.get(0).getStep());
        assertEquals(false, notes.get(1).isGrace());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<grace slash=\"yes\"/>"));
        assertEquals(false, xml.contains("<grace slash=\"yes\"/><pitch><step>C</step><octave>5</octave></pitch><duration>"));
    }

    @Test
    public void parsesBasicAbcDecorationsIntoMusicXmlNotations() {
        String abc = "X:1\nT:Decorated ABC\nM:4/4\nL:1/4\nK:C\n!trill!C .D !accent!E !fermata!F|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(true, notes.get(0).isTrill());
        assertEquals(true, notes.get(1).isStaccato());
        assertEquals(true, notes.get(2).isAccent());
        assertEquals("normal", notes.get(3).getFermataType());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<trill-mark/>"));
        assertEquals(true, xml.contains("<staccato/>"));
        assertEquals(true, xml.contains("<accent/>"));
        assertEquals(true, xml.contains("<fermata>normal</fermata>"));
    }

    @Test
    public void parsesAbcStandardShorthandDecorationsIntoMusicXml() {
        String abc = "X:1\nT:Standard Shorthand ABC\nM:4/4\nL:1/8\nK:C\n~C H D L E M F O G P A S B T c u d v e|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(true, notes.get(0).isArpeggiate());
        assertEquals("normal", notes.get(1).getFermataType());
        assertEquals(true, notes.get(2).isAccent());
        assertEquals("mordent", notes.get(3).getMordentType());
        assertEquals(true, notes.get(4).isCoda());
        assertEquals("inverted-mordent", notes.get(5).getMordentType());
        assertEquals(true, notes.get(6).isSegno());
        assertEquals(true, notes.get(7).isTrill());
        assertEquals(true, notes.get(8).isUpBow());
        assertEquals(true, notes.get(9).isDownBow());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<arpeggiate/>"));
        assertEquals(true, xml.contains("<fermata>normal</fermata>"));
        assertEquals(true, xml.contains("<accent/>"));
        assertEquals(true, xml.contains("<mordent/>"));
        assertEquals(true, xml.contains("<coda/>"));
        assertEquals(true, xml.contains("<inverted-mordent/>"));
        assertEquals(true, xml.contains("<segno/>"));
        assertEquals(true, xml.contains("<trill-mark/>"));
        assertEquals(true, xml.contains("<up-bow/>"));
        assertEquals(true, xml.contains("<down-bow/>"));
    }

    @Test
    public void parsesAbcRepeatAndEndingMetadataIntoMusicXmlBarlines() {
        String abc = "X:1\nT:Repeat ABC\nM:4/4\nL:1/4\nK:C\n|: C D :|] [2 E F |]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        Map<Integer, AbcIo.AbcMeasureMeta> metaByMeasure = parsed.getParts().get(0).getMeasureMetaByIndex();
        assertEquals(true, metaByMeasure.get(Integer.valueOf(1)).isRepeatStart());
        assertEquals(true, metaByMeasure.get(Integer.valueOf(1)).isRepeatEnd());
        assertEquals("2", metaByMeasure.get(Integer.valueOf(2)).getEndingStart());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<repeat direction=\"forward\" winged=\"none\"/>"));
        assertEquals(true, xml.contains("<repeat direction=\"backward\" winged=\"none\"/>"));
        assertEquals(true, xml.contains("<ending number=\"2\" type=\"start\"/>"));
    }

    @Test
    public void parsesAbcTieBodyTokenIntoMusicXmlTieAndTied() {
        String abc = "X:1\nT:Tie ABC\nM:4/4\nL:1/4\nK:C\nC-C D E|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(true, notes.get(0).isTieStart());
        assertEquals(true, notes.get(1).isTieStop());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<tie type=\"start\"/>"));
        assertEquals(true, xml.contains("<tie type=\"stop\"/>"));
        assertEquals(true, xml.contains("<tied type=\"start\"/>"));
        assertEquals(true, xml.contains("<tied type=\"stop\"/>"));
    }

    @Test
    public void parsesAbcBrokenRhythmAndSlurBodyIntoMusicXml() {
        String abc = "X:1\nT:Broken Rhythm Slur ABC\nM:4/4\nL:1/4\nK:C\n(C>D) E F|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(4, notes.size());
        assertEquals(1440, notes.get(0).getDuration());
        assertEquals(480, notes.get(1).getDuration());
        assertEquals(true, notes.get(0).isSlurStart());
        assertEquals(true, notes.get(1).isSlurStop());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<duration>1440</duration>"));
        assertEquals(true, xml.contains("<duration>480</duration>"));
        assertEquals(true, xml.contains("<slur type=\"start\"/>"));
        assertEquals(true, xml.contains("<slur type=\"stop\"/>"));
    }

    private static void assertFraction(int num, int den, AbcIo.Fraction actual) {
        assertEquals(num, actual.getNum());
        assertEquals(den, actual.getDen());
    }

    private static Element parseElement(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))
                .getDocumentElement();
    }
}
