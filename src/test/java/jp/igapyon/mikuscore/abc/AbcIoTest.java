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

    private static void assertFraction(int num, int den, AbcIo.Fraction actual) {
        assertEquals(num, actual.getNum());
        assertEquals(den, actual.getDen());
    }
}
