/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import jp.igapyon.mikuscore.musicxml.MusicXmlState;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
    public void convertsMusicXmlToAbcHarmonyDirectionAndLyrics() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Lead</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<harmony><root><root-step>G</root-step></root><kind>dominant</kind></harmony>"
                + "<direction><direction-type><words>dolce</words><dynamics><mf/></dynamics></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration>"
                + "<voice>1</voice><lyric><syllabic>single</syllabic><text>hello</text></lyric></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration>"
                + "<voice>1</voice><lyric><syllabic>single</syllabic><text>world</text></lyric></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("\"G7\"\"dolce\"!mf!C2"));
        assertEquals(true, abc.contains("\nw: hello world"));

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<harmony>"));
        assertEquals(true, roundtripped.contains("<root-step>G</root-step>"));
        assertEquals(true, roundtripped.contains("<kind text=\"G7\">dominant</kind>"));
        assertEquals(true, roundtripped.contains("<words>dolce</words>"));
        assertEquals(true, roundtripped.contains("<dynamics>"));
        assertEquals(true, roundtripped.contains("<mf/>"));
        assertEquals(true, roundtripped.contains("<text>hello</text>"));
        assertEquals(true, roundtripped.contains("<text>world</text>"));
    }

    @Test
    public void abcImportMapsWLyricsOntoSubsequentNotes() throws Exception {
        String abc = "X:1\nT:Lyrics\nM:4/4\nL:1/8\nK:C\nC D E F |\nw: la la la la\n";

        Element measure = directChildren(directChild(parseElement(
                AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions())), "part"), "measure").get(0);
        List<Element> notes = directChildren(measure, "note");

        assertEquals(4, notes.size());
        assertEquals("la", directChildText(directChild(notes.get(0), "lyric"), "text"));
        assertEquals("single", directChildText(directChild(notes.get(0), "lyric"), "syllabic"));
        assertEquals("la", directChildText(directChild(notes.get(1), "lyric"), "text"));
        assertEquals("single", directChildText(directChild(notes.get(1), "lyric"), "syllabic"));
        assertEquals("la", directChildText(directChild(notes.get(2), "lyric"), "text"));
        assertEquals("single", directChildText(directChild(notes.get(2), "lyric"), "syllabic"));
        assertEquals("la", directChildText(directChild(notes.get(3), "lyric"), "text"));
        assertEquals("single", directChildText(directChild(notes.get(3), "lyric"), "syllabic"));
    }

    @Test
    public void musicXmlToAbcExportsLyricsAsWLines() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Voice</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>eighth</type><lyric><syllabic>begin</syllabic><text>hal</text></lyric></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>eighth</type><lyric><syllabic>end</syllabic><text>lo</text></lyric></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>eighth</type><lyric><syllabic>single</syllabic><text>world</text></lyric></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);

        assertEquals(true, abc.contains("\nw: hal- lo world"), abc);
    }

    @Test
    public void abcImportSupportsHyphenatedWLyricsWithSyllabicMarkers() throws Exception {
        String abc = "X:1\nT:Lyrics hyphen\nM:4/4\nL:1/8\nK:C\nC D E |\nw: hal-le-lu\n";

        Element measure = directChildren(directChild(parseElement(
                AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions())), "part"), "measure").get(0);
        List<Element> notes = directChildren(measure, "note");

        assertEquals("hal", directChildText(directChild(notes.get(0), "lyric"), "text"));
        assertEquals("begin", directChildText(directChild(notes.get(0), "lyric"), "syllabic"));
        assertEquals("le", directChildText(directChild(notes.get(1), "lyric"), "text"));
        assertEquals("middle", directChildText(directChild(notes.get(1), "lyric"), "syllabic"));
        assertEquals("lu", directChildText(directChild(notes.get(2), "lyric"), "text"));
        assertEquals("end", directChildText(directChild(notes.get(2), "lyric"), "syllabic"));
    }

    @Test
    public void musicXmlToAbcRoundtripsCommonHyphenatedLyrics() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Voice</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>480</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>eighth</type><lyric><syllabic>begin</syllabic><text>hal</text></lyric></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>eighth</type><lyric><syllabic>middle</syllabic><text>le</text></lyric></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration>"
                + "<voice>1</voice><type>eighth</type><lyric><syllabic>end</syllabic><text>lu</text></lyric></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(roundtripped), "part"), "measure").get(0);
        List<Element> notes = directChildren(measure, "note");

        assertEquals(true, abc.contains("\nw: hal- le- lu"), abc);
        assertEquals("hal", directChildText(directChild(notes.get(0), "lyric"), "text"));
        assertEquals("begin", directChildText(directChild(notes.get(0), "lyric"), "syllabic"));
        assertEquals("le", directChildText(directChild(notes.get(1), "lyric"), "text"));
        assertEquals("middle", directChildText(directChild(notes.get(1), "lyric"), "syllabic"));
        assertEquals("lu", directChildText(directChild(notes.get(2), "lyric"), "text"));
        assertEquals("end", directChildText(directChild(notes.get(2), "lyric"), "syllabic"));
    }

    @Test
    public void abcImportSupportsInlineVoiceSwitchesInBodyText() throws Exception {
        String abc = "X:1\nT:Inline voice switch\nM:4/4\nL:1/8\nK:C\n"
                + "V:1 name=\"Upper\"\n"
                + "V:2 name=\"Lower\"\n"
                + "[V:1] C D | [V:2] E F |\n"
                + "[V:1] G A | [V:2] B c |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> parts = directChildren(root, "part");
        List<Element> scoreParts = directChildren(directChild(root, "part-list"), "score-part");

        assertEquals(2, parts.size());
        assertEquals("Upper", directChildText(scoreParts.get(0), "part-name"));
        assertEquals("Lower", directChildText(scoreParts.get(1), "part-name"));
        assertEquals(Arrays.asList("C", "D", "G", "A"), pitchSteps(parts.get(0)));
        assertEquals(Arrays.asList("E", "F", "B", "C"), pitchSteps(parts.get(1)));
        assertEquals(false, AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()).contains("mks:diag:count"));
    }

    @Test
    public void abcImportMapsScoreGroupedVoicesIntoOneMultiStaffPart() throws Exception {
        String abc = "X:1\nT:Grand staff from %%score\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 2)\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "[V:1] C D E F |\n"
                + "[V:2] C, D, E, F, |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> parts = directChildren(root, "part");
        Element measure = directChildren(parts.get(0), "measure").get(0);
        Element attributes = directChild(measure, "attributes");

        assertEquals(1, parts.size(), xml);
        assertEquals("Upper / Lower", directChildText(
                directChildren(directChild(root, "part-list"), "score-part").get(0), "part-name"));
        assertEquals("2", directChildText(attributes, "staves"));
        assertEquals("G", directChildText(directChildren(attributes, "clef").get(0), "sign"));
        assertEquals("1", directChildren(attributes, "clef").get(0).getAttribute("number"));
        assertEquals("F", directChildText(directChildren(attributes, "clef").get(1), "sign"));
        assertEquals("2", directChildren(attributes, "clef").get(1).getAttribute("number"));
        assertEquals("3840", directChildText(directChild(measure, "backup"), "duration"));
        assertEquals(Arrays.asList("1", "1", "1", "1", "2", "2", "2", "2"), staffNumbers(measure));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportKeepsScoreGroupedVoicesAlignedAcrossMultipleMeasures() throws Exception {
        String abc = "X:1\nT:Grand staff multi-measure from %%score\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 2)\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "[V:1] C D E F | G A B c |\n"
                + "[V:2] C, D, E, F, | G, A, B, C |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> parts = directChildren(root, "part");
        List<Element> measures = directChildren(parts.get(0), "measure");

        assertEquals(1, parts.size(), xml);
        assertEquals(2, measures.size(), xml);
        assertEquals(Arrays.asList("1", "1", "1", "1", "2", "2", "2", "2"), staffNumbers(measures.get(0)));
        assertEquals(Arrays.asList("1", "1", "1", "1", "2", "2", "2", "2"), staffNumbers(measures.get(1)));
        assertEquals("3840", directChildText(directChild(measures.get(0), "backup"), "duration"));
        assertEquals("3840", directChildText(directChild(measures.get(1), "backup"), "duration"));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportRestoresRepeatAndEndingMarkersOnScoreGroupedVoices() throws Exception {
        String abc = "X:1\nT:Grand staff alternate endings\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 2)\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "[V:1] |: C D E F |1 G A B c :|2 c B A G |\n"
                + "[V:2] |: C, D, E, F, |1 G, A, B, C :|2 C B, A, G, |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> parts = directChildren(root, "part");
        List<Element> measures = directChildren(parts.get(0), "measure");

        assertEquals(1, parts.size(), xml);
        assertEquals("2", directChildText(directChild(measures.get(0), "attributes"), "staves"));
        assertEquals(3, directChildren(measures.get(0), "backup").size()
                + directChildren(measures.get(1), "backup").size()
                + directChildren(measures.get(2), "backup").size());
        assertEquals("forward", directChild(measureBarline(measures.get(0), "left"), "repeat")
                .getAttribute("direction"));
        assertEquals("1", directChild(measureBarline(measures.get(1), "left"), "ending").getAttribute("number"));
        assertEquals("backward", directChild(measureBarline(measures.get(1), "right"), "repeat")
                .getAttribute("direction"));
        assertEquals("1", directChild(measureBarline(measures.get(1), "right"), "ending").getAttribute("number"));
        assertEquals("2", directChild(measureBarline(measures.get(2), "left"), "ending").getAttribute("number"));
        assertEquals("2", directChild(measureBarline(measures.get(2), "right"), "ending").getAttribute("number"));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportKeepsLyricsAttachedToScoreGroupedStaves() throws Exception {
        String abc = "X:1\nT:Grand staff lyrics\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 2)\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "V:1\n"
                + "C D E F |\n"
                + "w: up one two three\n"
                + "V:2\n"
                + "C, D, E, F, |\n"
                + "w: low one two three\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element measure = directChildren(directChildren(root, "part").get(0), "measure").get(0);
        List<Element> notes = directChildren(measure, "note");

        assertEquals(Arrays.asList("1", "1", "1", "1", "2", "2", "2", "2"), staffNumbers(measure));
        assertEquals(Arrays.asList("up", "one", "two", "three", "low", "one", "two", "three"),
                noteLyricTexts(notes));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportKeepsScoreGroupedAttributeChangesAtMeasureBoundary() throws Exception {
        String abc = "X:1\nT:Grand staff attributes\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 2)\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "[V:1] C D E F | [K:G][M:3/4][Q:1/4=132] G A B |\n"
                + "[V:2] C, D, E, F, | [K:G][M:3/4][Q:1/4=132] G, A, B, |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element measure2 = directChildren(directChildren(root, "part").get(0), "measure").get(1);
        Element attributes = directChild(measure2, "attributes");
        Element time = directChild(attributes, "time");
        Element direction = directChild(measure2, "direction");
        Element sound = directChild(direction, "sound");

        assertEquals("1", directChildText(directChild(attributes, "key"), "fifths"));
        assertEquals("3", directChildText(time, "beats"));
        assertEquals("4", directChildText(time, "beat-type"));
        assertEquals("132", directChildText(directChild(directChild(direction, "direction-type"), "metronome"),
                "per-minute"));
        assertEquals("132", sound.getAttribute("tempo"));
        assertEquals("2880", directChildText(directChild(measure2, "backup"), "duration"));
        assertEquals(Arrays.asList("1", "1", "1", "2", "2", "2"), staffNumbers(measure2));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportMapsMultipleScoreGroupedBlocksIntoMultipleMultiStaffParts() throws Exception {
        String abc = "X:1\nT:Two grouped systems\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 2) (3 4)\n"
                + "V:1 name=\"Upper A\" clef=treble\n"
                + "V:2 name=\"Lower A\" clef=bass\n"
                + "V:3 name=\"Upper B\" clef=treble\n"
                + "V:4 name=\"Lower B\" clef=bass\n"
                + "[V:1] C D E F |\n"
                + "[V:2] C, D, E, F, |\n"
                + "[V:3] G A B c |\n"
                + "[V:4] G, A, B, C |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> parts = directChildren(root, "part");
        List<Element> scoreParts = directChildren(directChild(root, "part-list"), "score-part");

        assertEquals(2, parts.size(), xml);
        assertEquals("Upper A / Lower A", directChildText(scoreParts.get(0), "part-name"));
        assertEquals("Upper B / Lower B", directChildText(scoreParts.get(1), "part-name"));
        assertEquals("2", directChildText(directChild(directChildren(parts.get(0), "measure").get(0), "attributes"),
                "staves"));
        assertEquals("2", directChildText(directChild(directChildren(parts.get(1), "measure").get(0), "attributes"),
                "staves"));
        assertEquals(1, directChildren(directChildren(parts.get(0), "measure").get(0), "backup").size());
        assertEquals(1, directChildren(directChildren(parts.get(1), "measure").get(0), "backup").size());
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportPreservesMixedScoreGroupedAndUngroupedOrdering() throws Exception {
        String abc = "X:1\nT:Grouped and single\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 2) 3\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "V:3 name=\"Solo\" clef=treble\n"
                + "[V:1] C D E F |\n"
                + "[V:2] C, D, E, F, |\n"
                + "[V:3] G A B c |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> parts = directChildren(root, "part");
        List<Element> scoreParts = directChildren(directChild(root, "part-list"), "score-part");
        Element groupedMeasure = directChildren(parts.get(0), "measure").get(0);
        Element soloMeasure = directChildren(parts.get(1), "measure").get(0);

        assertEquals(2, parts.size(), xml);
        assertEquals("Upper / Lower", directChildText(scoreParts.get(0), "part-name"));
        assertEquals("Solo", directChildText(scoreParts.get(1), "part-name"));
        assertEquals("2", directChildText(directChild(groupedMeasure, "attributes"), "staves"));
        assertNull(directChild(directChild(soloMeasure, "attributes"), "staves"));
        assertEquals(1, directChildren(groupedMeasure, "backup").size());
        assertEquals(0, directChildren(soloMeasure, "backup").size());
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportDeDuplicatesRepeatedIdsInsideScoreGroups() throws Exception {
        String abc = "X:1\nT:Repeated score ids\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (1 1 2) 2\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "[V:1] C D E F |\n"
                + "[V:2] C, D, E, F, |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> parts = directChildren(root, "part");
        Element measure = directChildren(parts.get(0), "measure").get(0);
        List<Element> scoreParts = directChildren(directChild(root, "part-list"), "score-part");

        assertEquals(1, parts.size(), xml);
        assertEquals("Upper / Lower", directChildText(scoreParts.get(0), "part-name"));
        assertEquals("2", directChildText(directChild(measure, "attributes"), "staves"));
        assertEquals(Arrays.asList("1", "1", "1", "1", "2", "2", "2", "2"), staffNumbers(measure));
        assertEquals(1, directChildren(measure, "backup").size());
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportIgnoresMalformedScoreIdsAndAppendsDeclaredVoicesInFallbackOrder() throws Exception {
        String abc = "X:1\nT:Malformed score ids\nM:4/4\nL:1/4\nK:C\n"
                + "%%score (!)\n"
                + "V:1 name=\"Upper\" clef=treble\n"
                + "V:2 name=\"Lower\" clef=bass\n"
                + "[V:1] C D E F |\n"
                + "[V:2] C, D, E, F, |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> parts = directChildren(root, "part");
        List<Element> scoreParts = directChildren(directChild(root, "part-list"), "score-part");
        Element upperMeasure = directChildren(parts.get(0), "measure").get(0);
        Element lowerMeasure = directChildren(parts.get(1), "measure").get(0);

        assertEquals(2, parts.size(), xml);
        assertEquals("Upper", directChildText(scoreParts.get(0), "part-name"));
        assertEquals("Lower", directChildText(scoreParts.get(1), "part-name"));
        assertNull(directChild(directChild(upperMeasure, "attributes"), "staves"));
        assertNull(directChild(directChild(lowerMeasure, "attributes"), "staves"));
        assertEquals(0, directChildren(upperMeasure, "backup").size());
        assertEquals(0, directChildren(lowerMeasure, "backup").size());
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportAppliesEditorialToNextExplicitAccidental() throws Exception {
        String abc = "X:1\nT:Editorial accidental\nM:4/4\nL:1/4\nK:C\n"
                + "!editorial!^C z |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element measure = directChildren(directChildren(root, "part").get(0), "measure").get(0);
        Element accidental = directChild(directChildren(measure, "note").get(0), "accidental");

        assertEquals("sharp", accidental.getTextContent().trim());
        assertEquals("yes", accidental.getAttribute("editorial"));
        assertEquals("", accidental.getAttribute("cautionary"));
    }

    @Test
    public void abcImportAppliesCourtesyToNextExplicitAccidental() throws Exception {
        String abc = "X:1\nT:Courtesy accidental\nM:4/4\nL:1/4\nK:G\n"
                + "!courtesy!=F z |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element measure = directChildren(directChildren(root, "part").get(0), "measure").get(0);
        Element accidental = directChild(directChildren(measure, "note").get(0), "accidental");

        assertEquals("natural", accidental.getTextContent().trim());
        assertEquals("yes", accidental.getAttribute("cautionary"));
        assertEquals("", accidental.getAttribute("editorial"));
    }

    @Test
    public void musicXmlToAbcExportsEditorialAccidentalAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<accidental editorial=\"yes\">sharp</accidental></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!editorial!^C"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element accidental = directChild(directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0), "accidental");
        assertEquals("sharp", accidental.getTextContent().trim());
        assertEquals("yes", accidental.getAttribute("editorial"));
        assertEquals("", accidental.getAttribute("cautionary"));
    }

    @Test
    public void musicXmlToAbcExportsCourtesyAccidentalAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>1</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>F</step><alter>0</alter><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<accidental cautionary=\"yes\">natural</accidental></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!courtesy!=F"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element accidental = directChild(directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0), "accidental");
        assertEquals("natural", accidental.getTextContent().trim());
        assertEquals("yes", accidental.getAttribute("cautionary"));
        assertEquals("", accidental.getAttribute("editorial"));
    }

    @Test
    public void musicXmlToAbcKeepsExplicitAccidentalWhenLaneKeyIsUnknown() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<accidental>sharp</accidental></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);

        assertEquals(true, abc.contains("^F"), abc);
    }

    @Test
    public void musicXmlToAbcDoesNotEmitRedundantNaturalInCMajor() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<accidental>natural</accidental></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);

        assertEquals(false, abc.contains("=D"), abc);
        assertEquals(true, abc.contains("D2"), abc);
    }

    @Test
    public void abcImportSupportsUserDefinedDecorationSymbolsWithPunctuation() throws Exception {
        String abc = "X:1\nT:User defined decoration\nU:~=!trill!\nM:4/4\nL:1/8\nK:C\n"
                + "~C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element firstNote = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note").get(0);
        Element ornaments = directChild(directChild(firstNote, "notations"), "ornaments");

        assertNotNull(directChild(ornaments, "trill-mark"));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportSupportsUserDefinedDecorationSymbolsWithLettersOutsideNoteNames() throws Exception {
        String abc = "X:1\nT:User defined fermata\nU:H=!fermata!\nM:4/4\nL:1/8\nK:C\n"
                + "HC D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element firstNote = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(firstNote, "notations"), "fermata"));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportSupportsUserDefinedDecorationSymbolsDeclaredWithPlusWrappers() throws Exception {
        String abc = "X:1\nT:User defined accent\nU:Z=+accent+\nM:4/4\nL:1/8\nK:C\n"
                + "ZC D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element firstNote = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note").get(0);
        Element articulations = directChild(directChild(firstNote, "notations"), "articulations");

        assertNotNull(directChild(articulations, "accent"));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportIgnoresMalformedUserDefinedSymbolSyntaxAndContinues() throws Exception {
        String abc = "X:1\nT:Broken user defined decoration\nU:~\nM:4/4\nL:1/8\nK:C\n"
                + "C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(notes));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportSkipsUnsupportedInlineBodyFieldsWithWarning() throws Exception {
        String abc = "X:1\nT:Inline unsupported field\nM:4/4\nL:1/8\nK:C\n"
                + "[P:A] C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported inline field: [P:A]"), xml);
    }

    @Test
    public void abcImportSkipsAbcjsWrapperLinesWithWarning() throws Exception {
        String abc = "[abcjs-audio engraver=\"{responsive:'resize'}\"]\n"
                + "X:1\nT:Kaeru\nM:4/4\nL:1/4\nQ:1/4=100\nK:C\n"
                + "|CDEF | EDCz| EFGA | GFEz |\n"
                + "| CzCz | CzCz | C/2C/2D/2D/2 E/2E/2F/2F/2 | EDCz||\n"
                + "[/abcjs-audio]\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> measures = directChildren(directChildren(root, "part").get(0), "measure");

        assertEquals(true, measures.size() > 0, xml);
        assertEquals(true, xml.contains("mks:diag:count"), xml);
        assertEquals(true, xml.contains("Skipped unsupported abcjs wrapper line"), xml);
    }

    @Test
    public void abcImportWarnsOnUnsupportedStandaloneBodyFields() throws Exception {
        String abc = "X:1\nT:Standalone unsupported body field\nM:4/4\nL:1/8\nK:C\n"
                + "C D E F |\n"
                + "P:A\n"
                + "G A B c |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> measures = directChildren(directChildren(root, "part").get(0), "measure");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported standalone body field: P:A"), xml);
        assertEquals(2, measures.size(), xml);
    }

    @Test
    public void abcImportAcceptsSameLineStandaloneBodyKeyTokensAsInlineFieldCompatibility() throws Exception {
        String abc = "X:1\nT:Same-line body key token\nM:4/4\nL:1/8\nK:C\n"
                + "C D E F | K:G G A B c |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        Element measure2 = directChildren(directChildren(root, "part").get(0), "measure").get(1);

        assertEquals("1", directChildText(directChild(directChild(measure2, "attributes"), "key"), "fifths"));
    }

    @Test
    public void abcImportWarnsOnUnsupportedSameLineStandaloneBodyFieldTokens() throws Exception {
        String abc = "X:1\nT:Same-line unsupported body token\nM:4/4\nL:1/8\nK:C\n"
                + "C D E F | P:A G A B c |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported standalone body field token: P:A"), xml);
    }

    @Test
    public void abcImportWarnsOnUnsupportedAbcDirectives() throws Exception {
        String abc = "X:1\nT:Unsupported directive\nM:4/4\nL:1/8\nK:C\n"
                + "%%text ignored\n"
                + "C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported ABC directive: %%text ignored"), xml);
    }

    @Test
    public void abcImportWarnsOnStrayBodyContinuationMarkers() throws Exception {
        String abc = "X:1\nT:Stray body continuation\nM:4/4\nL:1/8\nK:C\n"
                + "C D \\ E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped stray body continuation marker"), xml);
        assertEquals(4, notes.size(), xml);
    }

    @Test
    public void abcImportWarnsOnUnsupportedBodyWordTokens() throws Exception {
        String abc = "X:1\nT:Unsupported body word\nM:4/4\nL:1/8\nK:C\n"
                + "C D ignored E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported body token: ignored"), xml);
        assertEquals(4, notes.size(), xml);
    }

    @Test
    public void abcImportWarnsOnLowerCaseUnsupportedBodyWordLeftovers() throws Exception {
        String abc = "X:1\nT:Lower-case unsupported body word\nM:4/4\nL:1/8\nK:C\n"
                + "C D still E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported body token: still"), xml);
        assertEquals(4, notes.size(), xml);
    }

    @Test
    public void abcImportWarnsOnUnsupportedOctaveRangeInSingleNote() throws Exception {
        String abc = "X:1\nT:Unsupported octave single\nM:4/4\nL:1/8\nK:C\n"
                + "C'''''''''' D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped note with unsupported octave range."), xml);
        assertEquals(3, notes.size(), xml);
        assertEquals(Arrays.asList("D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnUnsupportedOctaveRangeInChordNote() throws Exception {
        String abc = "X:1\nT:Unsupported octave chord\nM:4/4\nL:1/8\nK:C\n"
                + "[C''''''''''E] D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped chord note with unsupported octave range."), xml);
        assertEquals(3, notes.size(), xml);
        assertEquals(Arrays.asList("D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnInvalidSingleNoteLength() throws Exception {
        String abc = "X:1\nT:Invalid single-note length\nM:4/4\nL:1/8\nK:C\n"
                + "C0 D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped note with invalid length."), xml);
        assertEquals(3, notes.size(), xml);
        assertEquals(Arrays.asList("D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnInvalidChordLength() throws Exception {
        String abc = "X:1\nT:Invalid chord length\nM:4/4\nL:1/8\nK:C\n"
                + "[CE]0 D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped chord with invalid length."), xml);
        assertEquals(3, notes.size(), xml);
        assertEquals(Arrays.asList("D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnInvalidGraceNoteLength() throws Exception {
        String abc = "X:1\nT:Invalid grace-note length\nM:4/4\nL:1/8\nK:C\n"
                + "{C0} D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped grace note with invalid length."), xml);
        assertEquals(3, notes.size(), xml);
        assertEquals(Arrays.asList("D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnMalformedAccidentalLeftovers() throws Exception {
        String abc = "X:1\nT:Malformed accidental leftover\nM:4/4\nL:1/8\nK:C\n"
                + "C ^; D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped malformed accidental token: ^"), xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: ;"), xml);
        assertEquals(4, notes.size(), xml);
        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnMalformedGraceAccidentalLeftovers() throws Exception {
        String abc = "X:1\nT:Malformed grace accidental leftover\nM:4/4\nL:1/8\nK:C\n"
                + "{^;} D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped malformed grace accidental token: ^"), xml);
        assertEquals(3, notes.size(), xml);
        assertEquals(Arrays.asList("D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnStrayBodyPunctuation() throws Exception {
        String abc = "X:1\nT:Stray body punctuation\nM:4/4\nL:1/8\nK:C\n"
                + "C ; D ` E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: ;"), xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: `"), xml);
        assertEquals(4, notes.size(), xml);
        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnAdditionalBoundedStrayBodyPunctuation() throws Exception {
        String abc = "X:1\nT:Additional stray body punctuation\nM:4/4\nL:1/8\nK:C\n"
                + "C ? D @ E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: ?"), xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: @"), xml);
        assertEquals(4, notes.size(), xml);
        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnStraySharpAndDollarSignPunctuation() throws Exception {
        String abc = "X:1\nT:Stray sharp and dollar signs\nM:4/4\nL:1/8\nK:C\n"
                + "C # D $ E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: #"), xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: $"), xml);
        assertEquals(4, notes.size(), xml);
        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnStrayAsteriskPunctuation() throws Exception {
        String abc = "X:1\nT:Stray asterisk\nM:4/4\nL:1/8\nK:C\n"
                + "C * D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported body punctuation: *"), xml);
        assertEquals(4, notes.size(), xml);
        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void abcImportWarnsOnStrayBodyNumberTokens() throws Exception {
        String abc = "X:1\nT:Stray body number\nM:4/4\nL:1/8\nK:C\n"
                + "C 123 D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);
        List<Element> notes = directChildren(directChildren(directChildren(root, "part").get(0), "measure").get(0),
                "note");

        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"),
                xml);
        assertEquals(true, xml.contains("Skipped unsupported body number token: 123"), xml);
        assertEquals(4, notes.size(), xml);
        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(notes));
    }

    @Test
    public void convertsMusicXmlToAbcGraceTieAndSlur() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Solo</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><grace slash=\"yes\"/><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>1</duration><voice>1</voice></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<tie type=\"start\"/><notations><tied type=\"start\"/><slur type=\"start\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<tie type=\"stop\"/><notations><tied type=\"stop\"/><slur type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("({/D2}C2- C2)"));

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<grace slash=\"yes\"/>"));
        assertEquals(true, roundtripped.contains("<tie type=\"start\"/>"));
        assertEquals(true, roundtripped.contains("<tie type=\"stop\"/>"));
        assertEquals(true, roundtripped.contains("<slur type=\"start\"/>"));
        assertEquals(true, roundtripped.contains("<slur type=\"stop\"/>"));
    }

    @Test
    public void musicXmlToAbcPreservesGraceNotesWithoutOrnaments() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><grace/><pitch><step>G</step><octave>4</octave></pitch>"
                + "<voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("{"), abc);
        assertEquals(true, abc.contains("}"), abc);
        assertEquals(false, abc.contains("!trill!"), abc);
        assertEquals(false, abc.contains("!turn!"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<grace"), roundtripped);
        assertEquals(false, roundtripped.contains("<trill-mark/>"), roundtripped);
        assertEquals(false, roundtripped.contains("<turn/>"), roundtripped);
    }

    @Test
    public void musicXmlToAbcExportsTrillDecorationAndGraceNotes() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><grace/><pitch><step>G</step><octave>4</octave></pitch>"
                + "<voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><trill-mark/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!trill!"), abc);
        assertEquals(true, abc.contains("{"), abc);
        assertEquals(true, abc.contains("}"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(roundtripped);
        assertNotNull(root.getElementsByTagName("grace").item(0));
        assertNotNull(root.getElementsByTagName("trill-mark").item(0));
        assertEquals(0, root.getElementsByTagName("wavy-line").getLength());
    }

    @Test
    public void musicXmlToAbcExportsTrillDecorationWithoutGraceNotes() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><trill-mark/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!trill!"), abc);
        assertEquals(false, abc.contains("{"), abc);
        assertEquals(false, abc.contains("}"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(roundtripped);
        assertEquals(0, root.getElementsByTagName("grace").getLength());
        assertNotNull(root.getElementsByTagName("trill-mark").item(0));
        assertEquals(0, root.getElementsByTagName("wavy-line").getLength());
    }

    @Test
    public void musicXmlToAbcPreservesSlashGraceNotesWithoutOrnaments() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><grace slash=\"yes\"/><pitch><step>G</step><octave>4</octave></pitch>"
                + "<voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("{/"), abc);
        assertEquals(false, abc.contains("!trill!"), abc);
        assertEquals(false, abc.contains("!turn!"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<grace slash=\"yes\"/>"), roundtripped);
        assertEquals(false, roundtripped.contains("<trill-mark/>"), roundtripped);
        assertEquals(false, roundtripped.contains("<turn/>"), roundtripped);
    }

    @Test
    public void musicXmlToAbcPreservesTurnTogetherWithSlashGraceNotes() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><grace slash=\"yes\"/><pitch><step>G</step><octave>4</octave></pitch>"
                + "<voice>1</voice><type>eighth</type></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><turn/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("{/"), abc);
        assertEquals(true, abc.contains("!turn!"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<grace slash=\"yes\"/>"), roundtripped);
        assertEquals(true, roundtripped.contains("<turn/>"), roundtripped);
    }

    @Test
    public void musicXmlToAbcExportsTurnDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><turn/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!turn!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note")
                .get(0);
        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "turn"));
    }

    @Test
    public void musicXmlToAbcExportsInvertedTurnDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><inverted-turn/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!invertedturn!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note")
                .get(0);
        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"),
                "inverted-turn"));
    }

    @Test
    public void musicXmlToAbcExportsTurnxVariantsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><turn slash=\"yes\"/></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><inverted-turn slash=\"yes\"/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!turnx!"), abc);
        assertEquals(true, abc.contains("!invertedturnx!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals("yes", directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "turn")
                .getAttribute("slash"));
        assertEquals("yes", directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"),
                "inverted-turn").getAttribute("slash"));
    }

    @Test
    public void musicXmlToAbcExportsDelayedTurnVariantsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><turn/><delayed-turn/></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><inverted-turn/><delayed-turn/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!delayedturn!"), abc);
        assertEquals(true, abc.contains("!delayedinvertedturn!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element firstOrnaments = directChild(directChild(notes.get(0), "notations"), "ornaments");
        Element secondOrnaments = directChild(directChild(notes.get(1), "notations"), "ornaments");
        assertNotNull(directChild(firstOrnaments, "turn"));
        assertNotNull(directChild(firstOrnaments, "delayed-turn"));
        assertNotNull(directChild(secondOrnaments, "inverted-turn"));
        assertNotNull(directChild(secondOrnaments, "delayed-turn"));
    }

    @Test
    public void musicXmlToAbcExportsDelayedTurnAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><turn/><delayed-turn/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!delayedturn!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element ornaments = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "ornaments");
        assertNotNull(directChild(ornaments, "turn"));
        assertNotNull(directChild(ornaments, "delayed-turn"));
    }

    @Test
    public void musicXmlToAbcExportsDelayedInvertedTurnAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><inverted-turn/><delayed-turn/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!delayedinvertedturn!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element ornaments = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "ornaments");
        assertNotNull(directChild(ornaments, "inverted-turn"));
        assertNotNull(directChild(ornaments, "delayed-turn"));
    }

    @Test
    public void convertsMusicXmlToAbcTupletTimeModification() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Triplet</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>6</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice>"
                + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"
                + "<notations><tuplet type=\"start\"/></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice>"
                + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice>"
                + "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"
                + "<notations><tuplet type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("(3:2:3C D E"));

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<actual-notes>3</actual-notes>"));
        assertEquals(true, roundtripped.contains("<normal-notes>2</normal-notes>"));
        assertEquals(true, roundtripped.contains("<tuplet type=\"start\"/>"));
        assertEquals(true, roundtripped.contains("<tuplet type=\"stop\"/>"));
    }

    @Test
    public void convertsMusicXmlToAbcNoteNotationPrefixes() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Marked</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice>"
                + "<notations><ornaments><trill-mark/></ornaments>"
                + "<articulations><staccato/><accent/></articulations>"
                + "<technical><up-bow/><fingering>1</fingering></technical><fermata>inverted</fermata></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!trill!!staccato!!accent!!upbow!!1!!invertedfermata!C2"));

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<trill-mark/>"));
        assertEquals(true, roundtripped.contains("<staccato/>"));
        assertEquals(true, roundtripped.contains("<accent/>"));
        assertEquals(true, roundtripped.contains("<up-bow/>"));
        assertEquals(true, roundtripped.contains("<fingering>1</fingering>"));
        assertEquals(true, roundtripped.contains("<fermata>inverted</fermata>"));
    }

    @Test
    public void convertsMusicXmlToAbcTrillAccidentalMeta() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Marked</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice>"
                + "<notations><ornaments><trill-mark/><accidental-mark>sharp</accidental-mark></ornaments>"
                + "</notations></note>"
                + "<note><rest/><duration>2</duration><voice>1</voice></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!trill!C4"));
        assertEquals(true, abc.contains("%@mks trill voice=P1 measure=1 event=1 upper=sharp"));

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<accidental-mark>sharp</accidental-mark>"));
    }

    @Test
    public void convertsMusicXmlToAbcRepeatAndEndingBarlines() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<part-list><score-part id=\"P1\"><part-name>Repeat</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>1</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<barline location=\"left\"><ending number=\"1\" type=\"start\"/>"
                + "<repeat direction=\"forward\"/></barline>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<barline location=\"right\"><ending number=\"1\" type=\"stop\"/>"
                + "<repeat direction=\"backward\"/></barline>"
                + "</measure><measure number=\"2\">"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>"
                + "<barline location=\"right\"><ending number=\"2\" type=\"stop\"/></barline>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("|:[1 C2 :|]"));
        assertEquals(true, abc.contains("D2 ]|"));

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<repeat direction=\"forward\" winged=\"none\"/>"));
        assertEquals(true, roundtripped.contains("<repeat direction=\"backward\" winged=\"none\"/>"));
        assertEquals(true, roundtripped.contains("<ending number=\"1\" type=\"start\"/>"));
        assertEquals(true, roundtripped.contains("<ending number=\"1\" type=\"stop\"/>")
                || roundtripped.contains("<ending number=\"1\" type=\"discontinue\"/>"));
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
    public void parsesSlashLengthShorthandIncludingDoubleSlashIntoMusicXml() throws Exception {
        String abc = "X:1\nT:Slash shorthand\nM:4/4\nL:1/8\nK:C\n"
                + "C/D/E/F/ G/F/E/D/ C//D//E//F// G//F//E//D// C2 |]\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(xml), "part"), "measure").get(0);
        List<Element> notes = directChildren(measure, "note");

        assertEquals(17, notes.size());
        assertEquals("240", directChildText(notes.get(0), "duration"));
        assertEquals("120", directChildText(notes.get(8), "duration"));
        assertEquals("960", directChildText(notes.get(16), "duration"));
    }

    @Test
    public void parsesNumeratorSlashShorthandInNotesChordsAndGraceGroups() throws Exception {
        String abc = "X:1\nT:Numerator slash shorthand\nM:4/4\nL:1/8\nK:C\nV:1\n"
                + "C3/ D | [CE]3/ G | {/g3/}a2 z2 |]\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element part = directChild(parseElement(xml), "part");
        List<Element> measures = directChildren(part, "measure");
        List<Element> firstMeasureNotes = directChildren(measures.get(0), "note");
        List<Element> secondMeasureNotes = directChildren(measures.get(1), "note");
        List<Element> thirdMeasureNotes = directChildren(measures.get(2), "note");

        assertEquals("720", directChildText(firstMeasureNotes.get(0), "duration"));
        assertEquals("720", directChildText(secondMeasureNotes.get(0), "duration"));
        assertEquals("chord", directChild(secondMeasureNotes.get(1), "chord").getTagName());
        assertEquals("grace", directChild(thirdMeasureNotes.get(0), "grace").getTagName());
    }

    @Test
    public void roundtripPreservesTempoViaAbcQHeader() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>\n"
                + "      <direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>220</per-minute></metronome></direction-type><sound tempo=\"220\"/></direction>\n"
                + "      <note><rest/><duration>1920</duration><voice>1</voice><type>whole</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";

        String abc = AbcIo.musicXmlToAbc(xml);
        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(roundtripped);
        Element part = directChild(root, "part");
        Element measure = directChildren(part, "measure").get(0);
        Element direction = directChild(measure, "direction");

        assertEquals(true, abc.contains("Q:1/4=220"), abc);
        assertEquals("220", directChild(direction, "sound").getAttribute("tempo"));
    }

    @Test
    public void musicXmlToAbcExportsMetronomeBeatUnitIntoQHeader() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>\n"
                + "      <direction><direction-type><metronome><beat-unit>half</beat-unit><per-minute>72</per-minute></metronome></direction-type><sound tempo=\"144\"/></direction>\n"
                + "      <note><rest/><duration>1920</duration><voice>1</voice><type>whole</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";

        String abc = AbcIo.musicXmlToAbc(xml);
        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(roundtripped);
        Element direction = directChild(directChildren(directChild(root, "part"), "measure").get(0), "direction");

        assertEquals(true, abc.contains("Q:1/2=72"), abc);
        assertEquals("144", directChild(direction, "sound").getAttribute("tempo"));
    }

    @Test
    public void musicXmlToAbcPrefersLastLeadingTempoInFirstMeasure() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>480</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>\n"
                + "      <direction><direction-type><words>Allegretto moderato</words></direction-type><sound tempo=\"116\"/></direction>\n"
                + "      <direction><sound tempo=\"90\"/></direction>\n"
                + "      <note><rest/><duration>1920</duration><voice>1</voice><type>whole</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";

        String abc = AbcIo.musicXmlToAbc(xml);

        assertEquals(true, abc.contains("Q:1/4=90"), abc);
        assertEquals(false, abc.contains("Q:1/4=116"), abc);
    }

    @Test
    public void roundtripSameStaffMultiVoiceScoreAvoidsMeasureOverfull() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"3.1\">\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Piano RH</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\">\n"
                + "    <measure number=\"1\">\n"
                + "      <attributes><divisions>960</divisions><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>\n"
                + "      <note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <note><pitch><step>F</step><octave>4</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type></note>\n"
                + "      <backup><duration>3840</duration></backup>\n"
                + "      <note><pitch><step>G</step><octave>3</octave></pitch><duration>1920</duration><voice>2</voice><type>half</type></note>\n"
                + "      <note><pitch><step>A</step><octave>3</octave></pitch><duration>1920</duration><voice>2</voice><type>half</type></note>\n"
                + "    </measure>\n"
                + "  </part>\n"
                + "</score-partwise>\n";

        String abc = AbcIo.musicXmlToAbc(xml);
        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(roundtripped);
        List<Element> parts = directChildren(root, "part");

        assertEquals(2, parts.size(), roundtripped);
        assertEquals(4, directChildren(directChildren(parts.get(0), "measure").get(0), "note").size());
        assertEquals(2, directChildren(directChildren(parts.get(1), "measure").get(0), "note").size());
        assertNoOverfullMeasures(root);
        assertEquals(true, MusicXmlState.validateMusicXmlForSave(roundtripped, true).isOk(), roundtripped);
    }

    @Test
    public void abcImportInfersBassClefFromLowNotesWhenClefIsOmitted() throws Exception {
        String abc = "X:1\nT:Clef inference\nM:4/4\nL:1/4\nK:C\nV:1\nC,, D,, E,, F,, |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element clef = directChild(directChild(directChildren(directChild(root, "part"), "measure").get(0),
                "attributes"), "clef");

        assertEquals("F", directChildText(clef, "sign"));
        assertEquals("4", directChildText(clef, "line"));
    }

    @Test
    public void abcImportAcceptsBareVoiceClefNamesAndAliases() throws Exception {
        String abc = "X:1\nT:Voice clef shorthand\nM:4/4\nL:1/4\nK:C\n"
                + "V:1 treble\nC D E F |\n"
                + "V:2 bass\nC,, D,, E,, F,, |\n"
                + "V:3 c3\nC D E F |\n"
                + "V:4 c4\nE F G A |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> parts = directChildren(root, "part");

        assertEquals(4, parts.size());
        assertClef(parts.get(0), "G", "2");
        assertClef(parts.get(1), "F", "4");
        assertClef(parts.get(2), "C", "3");
        assertClef(parts.get(3), "C", "4");
        assertEquals(false, AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()).contains("mks:diag:count"));
    }

    @Test
    public void abcImportKeepsSameLineBodyAfterBareVoiceClefName() throws Exception {
        String abc = "X:1\nT:Voice clef shorthand inline body\nM:2/4\nL:1/4\nK:C\nV:1 bass C,, D,, |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element part = directChild(root, "part");
        Element measure = directChildren(part, "measure").get(0);

        assertClef(part, "F", "4");
        assertEquals(2, directChildren(measure, "note").size());
    }

    @Test
    public void abcImportWarnsOnUnsupportedBareVoiceTailToken() throws Exception {
        String abc = "X:1\nT:Voice tail warning\nM:4/4\nL:1/4\nK:C\nV:1 bassoon\nC D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);

        assertEquals(4, directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note").size());
        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"), xml);
        assertEquals(true, xml.contains("Skipped unsupported V: directive tail token: bassoon"), xml);
    }

    @Test
    public void abcImportAppliesVoiceTransposePropertyAsChromaticTranspose() throws Exception {
        String abc = "X:1\nT:Voice transpose\nM:4/4\nL:1/4\nK:C\n"
                + "V:1 name=\"Clarinet in A\" clef=treble transpose=-3\nC D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element attributes = directChild(directChildren(directChild(parseElement(xml), "part"), "measure").get(0),
                "attributes");

        assertEquals("-3", directChildText(directChild(attributes, "transpose"), "chromatic"));
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportWarnsOnUnsupportedStandardVoiceProperties() throws Exception {
        String abc = "X:1\nT:Unsupported V property warning\nM:4/4\nL:1/4\nK:C\n"
                + "V:1 name=\"Upper\" staves=2 middle=c\nC D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);

        assertEquals(4, directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note").size());
        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field>"), xml);
        assertEquals(true, xml.contains("Skipped unsupported V: property: staves"), xml);
        assertEquals(true, xml.contains("Skipped unsupported V: property: middle"), xml);
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
    public void abcImportParsesTrillDecorationAfterGraceNotes() throws Exception {
        String abc = "X:1\nT:Ornament test\nM:4/4\nL:1/8\nK:C\nV:1\n"
                + "{g}!trill!a2 b2 c2 d2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element principal = null;
        for (Element note : notes) {
            if (directChild(note, "grace") == null) {
                principal = note;
                break;
            }
        }

        assertEquals(true, notes.size() >= 5);
        assertNotNull(directChild(notes.get(0), "grace"));
        assertNotNull(principal);
        assertNotNull(directChild(directChild(directChild(principal, "notations"), "ornaments"), "trill-mark"));
    }

    @Test
    public void abcImportAcceptsTrAsTrillAlias() throws Exception {
        String abc = "X:1\nT:Trill alias\nM:4/4\nL:1/8\nK:C\n"
                + "!tr!C D E F |\n";

        Element firstNote = directChildren(directChildren(directChild(
                parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions())), "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "trill-mark"));
    }

    @Test
    public void abcImportAcceptsTrillerAsTrillAlias() throws Exception {
        String abc = "X:1\nT:Triller alias\nM:4/4\nL:1/8\nK:C\n"
                + "!triller!C D E F |\n";

        Element firstNote = directChildren(directChildren(directChild(
                parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions())), "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "trill-mark"));
    }

    @Test
    public void abcImportParsesTurnDecorationAndGraceSlashVariant() throws Exception {
        String abc = "X:1\nT:Turn test\nM:4/4\nL:1/8\nK:C\nV:1\n"
                + "{/g}!turn!a2 b2 c2 d2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element principal = null;
        for (Element note : notes) {
            if (directChild(note, "grace") == null) {
                principal = note;
                break;
            }
        }

        assertEquals("yes", directChild(notes.get(0), "grace").getAttribute("slash"));
        assertNotNull(principal);
        assertNotNull(directChild(directChild(directChild(principal, "notations"), "ornaments"), "turn"));
    }

    @Test
    public void abcImportAcceptsLowerturnAsInvertedTurnAlias() throws Exception {
        String abc = "X:1\nT:Lower turn alias\nM:4/4\nL:1/8\nK:C\n"
                + "!lowerturn!C D E F |\n";

        Element firstNote = directChildren(directChildren(directChild(
                parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions())), "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "inverted-turn"));
    }

    @Test
    public void abcImportParsesSlashedTurnVariants() throws Exception {
        String abc = "X:1\nT:Turn slash variants\nM:4/4\nL:1/4\nK:C\n"
                + "!turnx!C !invertedturnx!D |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element firstOrnaments = directChild(directChild(notes.get(0), "notations"), "ornaments");
        Element secondOrnaments = directChild(directChild(notes.get(1), "notations"), "ornaments");

        assertEquals("yes", directChild(firstOrnaments, "turn").getAttribute("slash"));
        assertEquals("yes", directChild(secondOrnaments, "inverted-turn").getAttribute("slash"));
    }

    @Test
    public void abcImportParsesDelayedTurnVariants() throws Exception {
        String abc = "X:1\nT:Delayed turn\nM:4/4\nL:1/4\nK:C\n"
                + "!delayedturn!C !delayedinvertedturn!D |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element firstOrnaments = directChild(directChild(notes.get(0), "notations"), "ornaments");
        Element secondOrnaments = directChild(directChild(notes.get(1), "notations"), "ornaments");

        assertNotNull(directChild(firstOrnaments, "turn"));
        assertNotNull(directChild(firstOrnaments, "delayed-turn"));
        assertNotNull(directChild(secondOrnaments, "inverted-turn"));
        assertNotNull(directChild(secondOrnaments, "delayed-turn"));
    }

    @Test
    public void abcImportRoundtripsTrillAliasesToCanonicalTrill() throws Exception {
        for (String alias : Arrays.asList("tr", "triller")) {
            String abc = "X:1\nT:Trill alias canonical\nM:4/4\nL:1/4\nK:C\n"
                    + "!" + alias + "!C z |\n";

            String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
            assertEquals(true, xml.contains("<trill-mark/>"), xml);

            String exportedAbc = AbcIo.musicXmlToAbc(xml);
            assertEquals(true, exportedAbc.contains("!trill!"), exportedAbc);
            assertEquals(false, exportedAbc.contains("!" + alias + "!"), exportedAbc);
        }
    }

    @Test
    public void abcImportRoundtripsLowerturnAliasToCanonicalInvertedTurn() throws Exception {
        String abc = "X:1\nT:Lower turn canonical\nM:4/4\nL:1/4\nK:C\n"
                + "!lowerturn!C z |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<inverted-turn/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, exportedAbc.contains("!invertedturn!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!lowerturn!"), exportedAbc);
    }

    @Test
    public void musicXmlToAbcExportsTurnAndInvertedTurnAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><turn/></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><inverted-turn/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!turn!"), abc);
        assertEquals(true, abc.contains("!invertedturn!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "turn"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"),
                "inverted-turn"));
    }

    @Test
    public void abcImportParsesLongTrillStartAndStopDecorations() throws Exception {
        String abc = "X:1\nT:Long trill\nM:4/4\nL:1/4\nK:C\n"
                + "!trill(!C D !trill)!E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element firstOrnaments = directChild(directChild(notes.get(0), "notations"), "ornaments");
        Element thirdOrnaments = directChild(directChild(notes.get(2), "notations"), "ornaments");

        assertNotNull(directChild(firstOrnaments, "trill-mark"));
        assertEquals("start", directChild(firstOrnaments, "wavy-line").getAttribute("type"));
        assertEquals("stop", directChild(thirdOrnaments, "wavy-line").getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsWavyLineStartAsTrillToken() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><ornaments><wavy-line type=\"start\"/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);

        assertEquals(true, abc.contains("!trill!"), abc);
    }

    @Test
    public void abcImportParsesMikuscoreTremoloDecorations() throws Exception {
        String abc = "X:1\nT:Tremolo\nM:4/4\nL:1/4\nK:C\n"
                + "!tremolo-single-3!C !tremolo-start-2!D !tremolo-stop-2!E |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element firstTremolo = directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"),
                "tremolo");
        Element secondTremolo = directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"),
                "tremolo");
        Element thirdTremolo = directChild(directChild(directChild(notes.get(2), "notations"), "ornaments"),
                "tremolo");

        assertEquals("single", firstTremolo.getAttribute("type"));
        assertEquals("3", firstTremolo.getTextContent().trim());
        assertEquals("start", secondTremolo.getAttribute("type"));
        assertEquals("2", secondTremolo.getTextContent().trim());
        assertEquals("stop", thirdTremolo.getAttribute("type"));
        assertEquals("2", thirdTremolo.getTextContent().trim());
    }

    @Test
    public void abcImportParsesMikuscoreGlissandoAndSlideDecorations() throws Exception {
        String abc = "X:1\nT:Spanners\nM:4/4\nL:1/4\nK:C\n"
                + "!gliss-start!C !gliss-stop!D !slide-start!E !slide-stop!F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertEquals("start", directChild(directChild(notes.get(0), "notations"), "glissando")
                .getAttribute("type"));
        assertEquals("stop", directChild(directChild(notes.get(1), "notations"), "glissando")
                .getAttribute("type"));
        assertEquals("start", directChild(directChild(notes.get(2), "notations"), "slide").getAttribute("type"));
        assertEquals("stop", directChild(directChild(notes.get(3), "notations"), "slide").getAttribute("type"));
    }

    @Test
    public void abcImportAcceptsStandardSlideDecorationAsSlideStart() throws Exception {
        String abc = "X:1\nT:Standard slide\nM:4/4\nL:1/4\nK:C\n"
                + "!slide!C D |\n";

        Element firstNote = directChildren(directChildren(directChild(
                parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions())), "part"), "measure").get(0),
                "note").get(0);

        assertEquals("start", directChild(directChild(firstNote, "notations"), "slide").getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsTurnSlashAndDelayedVariantsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><turn slash=\"yes\"/></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><inverted-turn slash=\"yes\"/></ornaments></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><turn/><delayed-turn/></ornaments></notations></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><inverted-turn/><delayed-turn/></ornaments></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!turnx!"), abc);
        assertEquals(true, abc.contains("!invertedturnx!"), abc);
        assertEquals(true, abc.contains("!delayedturn!"), abc);
        assertEquals(true, abc.contains("!delayedinvertedturn!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals("yes", directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "turn")
                .getAttribute("slash"));
        assertEquals("yes", directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"),
                "inverted-turn").getAttribute("slash"));
        assertNotNull(directChild(directChild(directChild(notes.get(2), "notations"), "ornaments"), "delayed-turn"));
        assertNotNull(directChild(directChild(directChild(notes.get(3), "notations"), "ornaments"), "delayed-turn"));
    }

    @Test
    public void musicXmlToAbcExportsTremoloGlissandoAndSlideVariantsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><ornaments><tremolo type=\"single\">3</tremolo></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><ornaments><tremolo type=\"start\">2</tremolo></ornaments></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><ornaments><tremolo type=\"stop\">2</tremolo></ornaments></notations></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><glissando type=\"start\" number=\"1\">wavy</glissando></notations></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><glissando type=\"stop\" number=\"1\">wavy</glissando></notations></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><slide type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>B</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><slide type=\"stop\" number=\"1\"/></notations></note>"
                + "<note><rest/><duration>480</duration><voice>1</voice><type>eighth</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!tremolo-single-3!"), abc);
        assertEquals(true, abc.contains("!tremolo-start-2!"), abc);
        assertEquals(true, abc.contains("!tremolo-stop-2!"), abc);
        assertEquals(true, abc.contains("!gliss-start!"), abc);
        assertEquals(true, abc.contains("!gliss-stop!"), abc);
        assertEquals(true, abc.contains("!slide!"), abc);
        assertEquals(true, abc.contains("!slide-stop!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals("single", directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"),
                "tremolo").getAttribute("type"));
        assertEquals("start", directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"),
                "tremolo").getAttribute("type"));
        assertEquals("stop", directChild(directChild(directChild(notes.get(2), "notations"), "ornaments"),
                "tremolo").getAttribute("type"));
        assertEquals("start", directChild(directChild(notes.get(3), "notations"), "glissando")
                .getAttribute("type"));
        assertEquals("stop", directChild(directChild(notes.get(4), "notations"), "glissando").getAttribute("type"));
        assertEquals("start", directChild(directChild(notes.get(5), "notations"), "slide").getAttribute("type"));
        assertEquals("stop", directChild(directChild(notes.get(6), "notations"), "slide").getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsTremoloDecorationsAndRoundtripsMarks() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><tremolo type=\"single\">3</tremolo></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><tremolo type=\"start\">2</tremolo></ornaments></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><tremolo type=\"stop\">2</tremolo></ornaments></notations></note>"
                + "<note><rest/><duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!tremolo-single-3!"), abc);
        assertEquals(true, abc.contains("!tremolo-start-2!"), abc);
        assertEquals(true, abc.contains("!tremolo-stop-2!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element firstTremolo = directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"),
                "tremolo");
        Element secondTremolo = directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"),
                "tremolo");
        Element thirdTremolo = directChild(directChild(directChild(notes.get(2), "notations"), "ornaments"),
                "tremolo");

        assertEquals("single", firstTremolo.getAttribute("type"));
        assertEquals("3", firstTremolo.getTextContent().trim());
        assertEquals("start", secondTremolo.getAttribute("type"));
        assertEquals("2", secondTremolo.getTextContent().trim());
        assertEquals("stop", thirdTremolo.getAttribute("type"));
        assertEquals("2", thirdTremolo.getTextContent().trim());
    }

    @Test
    public void musicXmlToAbcExportsSingleTremoloAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><tremolo type=\"single\">3</tremolo></ornaments></notations></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type><dot/></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!tremolo-single-3!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element tremolo = directChild(directChild(directChild(directChildren(directChildren(directChild(root,
                "part"), "measure").get(0), "note").get(0), "notations"), "ornaments"), "tremolo");
        assertEquals("single", tremolo.getAttribute("type"));
        assertEquals("3", tremolo.getTextContent().trim());
    }

    @Test
    public void musicXmlToAbcExportsTremoloStartAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><tremolo type=\"start\">2</tremolo></ornaments></notations></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type><dot/></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!tremolo-start-2!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element tremolo = directChild(directChild(directChild(directChildren(directChildren(directChild(root,
                "part"), "measure").get(0), "note").get(0), "notations"), "ornaments"), "tremolo");
        assertEquals("start", tremolo.getAttribute("type"));
        assertEquals("2", tremolo.getTextContent().trim());
    }

    @Test
    public void musicXmlToAbcExportsTremoloStopAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><tremolo type=\"stop\">2</tremolo></ornaments></notations></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type><dot/></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!tremolo-stop-2!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element tremolo = directChild(directChild(directChild(directChildren(directChildren(directChild(root,
                "part"), "measure").get(0), "note").get(0), "notations"), "ornaments"), "tremolo");
        assertEquals("stop", tremolo.getAttribute("type"));
        assertEquals("2", tremolo.getTextContent().trim());
    }

    @Test
    public void musicXmlToAbcExportsGlissandoAndSlideDecorationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><glissando type=\"start\" number=\"1\">wavy</glissando></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><glissando type=\"stop\" number=\"1\">wavy</glissando></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><slide type=\"start\" number=\"1\"/></notations></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><slide type=\"stop\" number=\"1\"/></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!gliss-start!"), abc);
        assertEquals(true, abc.contains("!gliss-stop!"), abc);
        assertEquals(true, abc.contains("!slide!"), abc);
        assertEquals(true, abc.contains("!slide-stop!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals("start", directChild(directChild(notes.get(0), "notations"), "glissando")
                .getAttribute("type"));
        assertEquals("stop", directChild(directChild(notes.get(1), "notations"), "glissando").getAttribute("type"));
        assertEquals("start", directChild(directChild(notes.get(2), "notations"), "slide").getAttribute("type"));
        assertEquals("stop", directChild(directChild(notes.get(3), "notations"), "slide").getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsGlissandoStartAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><glissando type=\"start\" number=\"1\">wavy</glissando></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!gliss-start!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element glissando = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "glissando");
        assertEquals("start", glissando.getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsGlissandoStopAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><glissando type=\"stop\" number=\"1\">wavy</glissando></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!gliss-stop!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element glissando = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "glissando");
        assertEquals("stop", glissando.getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsSlideStartAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><slide type=\"start\" number=\"1\"/></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!slide!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element slide = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "slide");
        assertEquals("start", slide.getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsSlideStopAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><slide type=\"stop\" number=\"1\"/></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!slide-stop!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element slide = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "slide");
        assertEquals("stop", slide.getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsLongTrillStartAndStopDecorations() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><trill-mark/><wavy-line type=\"start\"/></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><wavy-line type=\"stop\"/></ornaments></notations></note>"
                + "<note><rest/><duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!trill(!"), abc);
        assertEquals(true, abc.contains("!trill)!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        Element firstOrnaments = directChild(directChild(notes.get(0), "notations"), "ornaments");
        Element thirdOrnaments = directChild(directChild(notes.get(2), "notations"), "ornaments");
        assertNotNull(directChild(firstOrnaments, "trill-mark"));
        assertEquals("start", directChild(firstOrnaments, "wavy-line").getAttribute("type"));
        assertEquals("stop", directChild(thirdOrnaments, "wavy-line").getAttribute("type"));
    }

    @Test
    public void abcImportParsesStandardPhraseMarkDecorations() throws Exception {
        String abc = "X:1\nT:Phrase marks\nM:4/4\nL:1/4\nK:C\n"
                + "!shortphrase!C !mediumphrase!D !longphrase!E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertEquals("shortphrase", directChildText(directChild(directChild(notes.get(0), "notations"),
                "articulations"), "other-articulation"));
        assertEquals("mediumphrase", directChildText(directChild(directChild(notes.get(1), "notations"),
                "articulations"), "other-articulation"));
        assertEquals("longphrase", directChildText(directChild(directChild(notes.get(2), "notations"),
                "articulations"), "other-articulation"));
    }

    @Test
    public void musicXmlToAbcExportsPhraseMarkDecorationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><articulations>"
                + "<other-articulation>shortphrase</other-articulation>"
                + "</articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><articulations>"
                + "<other-articulation>mediumphrase</other-articulation>"
                + "</articulations></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><articulations>"
                + "<other-articulation>longphrase</other-articulation>"
                + "</articulations></notations></note>"
                + "<note><rest/><duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!shortphrase!"), abc);
        assertEquals(true, abc.contains("!mediumphrase!"), abc);
        assertEquals(true, abc.contains("!longphrase!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals("shortphrase", directChildText(directChild(directChild(notes.get(0), "notations"),
                "articulations"), "other-articulation"));
        assertEquals("mediumphrase", directChildText(directChild(directChild(notes.get(1), "notations"),
                "articulations"), "other-articulation"));
        assertEquals("longphrase", directChildText(directChild(directChild(notes.get(2), "notations"),
                "articulations"), "other-articulation"));
    }

    @Test
    public void abcImportAcceptsStaccatoAliases() throws Exception {
        String abc = "X:1\nT:Staccato aliases\nM:4/4\nL:1/4\nK:C\n"
                + "!staccato!C !stacc!D !stac!E |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "articulations"),
                "staccato"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "articulations"),
                "staccato"));
        assertNotNull(directChild(directChild(directChild(notes.get(2), "notations"), "articulations"),
                "staccato"));
    }

    @Test
    public void abcImportRoundtripsStaccatoAliasesToCanonicalStaccato() throws Exception {
        for (String alias : Arrays.asList("stacc", "stac")) {
            String abc = "X:1\nT:Staccato alias canonical\nM:4/4\nL:1/4\nK:C\n"
                    + "!" + alias + "!C z |\n";

            String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
            assertEquals(true, xml.contains("<staccato/>"), xml);

            String exportedAbc = AbcIo.musicXmlToAbc(xml);
            assertEquals(true, exportedAbc.contains("!staccato!"), exportedAbc);
            assertEquals(false, exportedAbc.contains("!" + alias + "!"), exportedAbc);
        }
    }

    @Test
    public void musicXmlToAbcExportsStaccatoDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><staccato/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!staccato!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "staccato"));
    }

    @Test
    public void abcImportParsesAccentTenutoStressAndFermataDecorations() throws Exception {
        String abc = "X:1\nT:Core articulations\nM:5/4\nL:1/4\nK:C\n"
                + "!accent!C !tenuto!D !stress!E !unstress!F !fermata!G |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "articulations"), "accent"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "articulations"), "tenuto"));
        assertNotNull(directChild(directChild(directChild(notes.get(2), "notations"), "articulations"), "stress"));
        assertNotNull(directChild(directChild(directChild(notes.get(3), "notations"), "articulations"), "unstress"));
        assertEquals("normal", directChild(directChild(notes.get(4), "notations"), "fermata").getTextContent().trim());
    }

    @Test
    public void musicXmlToAbcExportsAccentTenutoAndFermataDecorationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><articulations><accent/></articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><articulations><tenuto/></articulations></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><fermata>normal</fermata></notations></note>"
                + "<note><rest/><duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!accent!"), abc);
        assertEquals(true, abc.contains("!tenuto!"), abc);
        assertEquals(true, abc.contains("!fermata!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "articulations"), "accent"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "articulations"), "tenuto"));
        assertNotNull(directChild(directChild(notes.get(2), "notations"), "fermata"));
    }

    @Test
    public void musicXmlToAbcExportsAccentDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><accent/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!accent!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "accent"));
    }

    @Test
    public void musicXmlToAbcExportsTenutoDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><tenuto/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!tenuto!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "tenuto"));
    }

    @Test
    public void musicXmlToAbcExportsFermataDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><fermata>normal</fermata></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!fermata!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        assertNotNull(directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "fermata"));
    }

    @Test
    public void musicXmlToAbcExportsStressAndUnstressDecorationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><articulations><stress/></articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><articulations><unstress/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!stress!"), abc);
        assertEquals(true, abc.contains("!unstress!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "articulations"), "stress"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "articulations"), "unstress"));
    }

    @Test
    public void musicXmlToAbcExportsStressDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><stress/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!stress!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "stress"));
    }

    @Test
    public void musicXmlToAbcExportsUnstressDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><unstress/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!unstress!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "unstress"));
    }

    @Test
    public void musicXmlToAbcExportsInvertedFermataDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><fermata type=\"inverted\">inverted</fermata></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!invertedfermata!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element fermata = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "fermata");
        assertEquals("inverted", fermata.getTextContent().trim());
    }

    @Test
    public void abcImportAcceptsAccentAndInvertedFermataAliases() throws Exception {
        String abc = "X:1\nT:Accent and fermata aliases\nM:4/4\nL:1/4\nK:C\n"
                + "!>!C !emphasis!D !invertedfermata!E !inverted fermata!F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "articulations"), "accent"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "articulations"), "accent"));
        assertEquals("inverted", directChild(directChild(notes.get(2), "notations"), "fermata")
                .getTextContent().trim());
        assertEquals("inverted", directChild(directChild(notes.get(3), "notations"), "fermata")
                .getTextContent().trim());
    }

    @Test
    public void abcImportRoundtripsAccentAliasesToCanonicalAccent() throws Exception {
        for (String alias : Arrays.asList(">", "emphasis")) {
            String abc = "X:1\nT:Accent alias canonical\nM:4/4\nL:1/4\nK:C\n"
                    + "!" + alias + "!C z |\n";

            String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
            assertEquals(true, xml.contains("<accent/>"), xml);

            String exportedAbc = AbcIo.musicXmlToAbc(xml);
            assertEquals(true, exportedAbc.contains("!accent!"), exportedAbc);
            assertEquals(false, exportedAbc.contains("!" + alias + "!"), exportedAbc);
        }
    }

    @Test
    public void abcImportRoundtripsInvertedFermataAliasToCanonicalInvertedFermata() throws Exception {
        String abc = "X:1\nT:Inverted fermata alias canonical\nM:4/4\nL:1/4\nK:C\n"
                + "!inverted fermata!C z |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<fermata>inverted</fermata>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, exportedAbc.contains("!invertedfermata!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!inverted fermata!"), exportedAbc);
    }

    @Test
    public void musicXmlToAbcExportsStressMarcatoBreathAndCaesuraAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><articulations><stress/></articulations></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><articulations><unstress/></articulations></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><articulations><strong-accent/></articulations></notations></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><articulations><breath-mark/></articulations></notations></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<notations><articulations><caesura/></articulations></notations></note>"
                + "<note><rest/><duration>1440</duration><voice>1</voice><type>quarter</type>"
                + "<dot/></note></measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!stress!"), abc);
        assertEquals(true, abc.contains("!unstress!"), abc);
        assertEquals(true, abc.contains("!marcato!"), abc);
        assertEquals(true, abc.contains("!breath!"), abc);
        assertEquals(true, abc.contains("!caesura!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "articulations"), "stress"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "articulations"), "unstress"));
        assertNotNull(directChild(directChild(directChild(notes.get(2), "notations"), "articulations"),
                "strong-accent"));
        assertNotNull(directChild(directChild(directChild(notes.get(3), "notations"), "articulations"),
                "breath-mark"));
        assertNotNull(directChild(directChild(directChild(notes.get(4), "notations"), "articulations"), "caesura"));
    }

    @Test
    public void musicXmlToAbcExportsMarcatoDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><strong-accent/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!marcato!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "strong-accent"));
    }

    @Test
    public void musicXmlToAbcExportsBreathDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><breath-mark/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!breath!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "breath-mark"));
    }

    @Test
    public void musicXmlToAbcExportsCaesuraDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type>"
                + "<notations><articulations><caesura/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!caesura!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(directChildren(directChild(root, "part"),
                "measure").get(0), "note").get(0), "notations"), "articulations");
        assertNotNull(directChild(articulations, "caesura"));
    }

    @Test
    public void abcImportAcceptsMarcatoAndBreathAliases() throws Exception {
        String abc = "X:1\nT:Marcato and breath aliases\nM:9/4\nL:1/4\nK:C\n"
                + "!marcato!C !strong accent!D !strongaccent!E !strong-accent!F "
                + "!breath!G !breathmark!A !breath mark!B !breath-mark!c !caesura!d |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        for (int i = 0; i < 4; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "articulations"),
                    "strong-accent"));
        }
        for (int i = 4; i < 8; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "articulations"),
                    "breath-mark"));
        }
        assertNotNull(directChild(directChild(directChild(notes.get(8), "notations"), "articulations"), "caesura"));
    }

    @Test
    public void abcImportRoundtripsMarcatoAliasesToCanonicalMarcato() throws Exception {
        for (String alias : Arrays.asList("strong accent", "strongaccent", "strong-accent")) {
            String abc = "X:1\nT:Marcato alias canonical\nM:4/4\nL:1/4\nK:C\n"
                    + "!" + alias + "!C z |\n";

            String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
            assertEquals(true, xml.contains("<strong-accent/>"), xml);

            String exportedAbc = AbcIo.musicXmlToAbc(xml);
            assertEquals(true, exportedAbc.contains("!marcato!"), exportedAbc);
            assertEquals(false, exportedAbc.contains("!" + alias + "!"), exportedAbc);
        }
    }

    @Test
    public void abcImportRoundtripsBreathAliasesToCanonicalBreath() throws Exception {
        for (String alias : Arrays.asList("breathmark", "breath mark", "breath-mark")) {
            String abc = "X:1\nT:Breath alias canonical\nM:4/4\nL:1/4\nK:C\n"
                    + "!" + alias + "!C z |\n";

            String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
            assertEquals(true, xml.contains("<breath-mark/>"), xml);

            String exportedAbc = AbcIo.musicXmlToAbc(xml);
            assertEquals(true, exportedAbc.contains("!breath!"), exportedAbc);
            assertEquals(false, exportedAbc.contains("!" + alias + "!"), exportedAbc);
        }
    }

    @Test
    public void abcImportParsesStaccatissimoDistinctlyFromStaccato() throws Exception {
        String abc = "X:1\nT:Staccatissimo aliases\nM:4/4\nL:1/4\nK:C\n"
                + "!wedge!C !spiccato!D |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        for (Element note : notes) {
            Element articulations = directChild(directChild(note, "notations"), "articulations");
            assertNotNull(directChild(articulations, "staccatissimo"));
            assertNull(directChild(articulations, "staccato"));
        }
    }

    @Test
    public void abcImportRoundtripsSpiccatoAliasToCanonicalWedge() throws Exception {
        String abc = "X:1\nT:Spiccato alias canonical\nM:4/4\nL:1/4\nK:C\n"
                + "!spiccato!C z |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<staccatissimo/>"), xml);
        assertEquals(false, xml.contains("<staccato/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, exportedAbc.contains("!wedge!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!spiccato!"), exportedAbc);
    }

    @Test
    public void musicXmlToAbcExportsStaccatissimoAsWedgeAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><articulations><staccatissimo/></articulations></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!wedge!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element articulations = directChild(directChild(directChildren(
                directChildren(directChild(root, "part"), "measure").get(0), "note").get(0),
                "notations"), "articulations");
        assertNotNull(directChild(articulations, "staccatissimo"));
        assertNull(directChild(articulations, "staccato"));
    }

    @Test
    public void abcImportAcceptsBowingAliases() throws Exception {
        String abc = "X:1\nT:Bowing aliases\nM:4/4\nL:1/4\nK:C\n"
                + "!upbow!C !downbow!D !up bow!E !down-bow!F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "technical"), "up-bow"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "technical"), "down-bow"));
        assertNotNull(directChild(directChild(directChild(notes.get(2), "notations"), "technical"), "up-bow"));
        assertNotNull(directChild(directChild(directChild(notes.get(3), "notations"), "technical"), "down-bow"));
    }

    @Test
    public void musicXmlToAbcExportsBowingTongueHeelAndToeAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>6</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><up-bow/></technical></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><down-bow/></technical></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><double-tongue/></technical></notations></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><triple-tongue/></technical></notations></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><heel/></technical></notations></note>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><toe/></technical></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!upbow!"), abc);
        assertEquals(true, abc.contains("!downbow!"), abc);
        assertEquals(true, abc.contains("!doubletongue!"), abc);
        assertEquals(true, abc.contains("!tripletongue!"), abc);
        assertEquals(true, abc.contains("!heel!"), abc);
        assertEquals(true, abc.contains("!toe!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "technical"), "up-bow"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "technical"), "down-bow"));
        assertNotNull(directChild(directChild(directChild(notes.get(2), "notations"), "technical"),
                "double-tongue"));
        assertNotNull(directChild(directChild(directChild(notes.get(3), "notations"), "technical"),
                "triple-tongue"));
        assertNotNull(directChild(directChild(directChild(notes.get(4), "notations"), "technical"), "heel"));
        assertNotNull(directChild(directChild(directChild(notes.get(5), "notations"), "technical"), "toe"));
    }

    @Test
    public void abcImportRoundtripsBowingAliasesToCanonicalBowing() throws Exception {
        String abc = "X:1\nT:Bowing alias canonical\nM:4/4\nL:1/4\nK:C\n"
                + "!up bow!C !up-bow!D !down bow!E !down-bow!F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(2, countOccurrences(xml, "<up-bow/>"), xml);
        assertEquals(2, countOccurrences(xml, "<down-bow/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(2, countOccurrences(exportedAbc, "!upbow!"), exportedAbc);
        assertEquals(2, countOccurrences(exportedAbc, "!downbow!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!up bow!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!up-bow!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!down bow!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!down-bow!"), exportedAbc);
    }

    @Test
    public void abcImportAcceptsTongueHeelAndToeAliases() throws Exception {
        String abc = "X:1\nT:Tongue heel toe aliases\nM:10/4\nL:1/4\nK:C\n"
                + "!doubletongue!C !double tongue!D !double-tongue!E "
                + "!tripletongue!F !triple tongue!G !triple-tongue!A "
                + "!heel!B !heel mark!c !toe!d !toe mark!e |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        for (int i = 0; i < 3; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "technical"),
                    "double-tongue"));
        }
        for (int i = 3; i < 6; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "technical"),
                    "triple-tongue"));
        }
        assertNotNull(directChild(directChild(directChild(notes.get(6), "notations"), "technical"), "heel"));
        assertNotNull(directChild(directChild(directChild(notes.get(7), "notations"), "technical"), "heel"));
        assertNotNull(directChild(directChild(directChild(notes.get(8), "notations"), "technical"), "toe"));
        assertNotNull(directChild(directChild(directChild(notes.get(9), "notations"), "technical"), "toe"));
    }

    @Test
    public void abcImportRoundtripsTongueHeelAndToeAliasesToCanonicalDecorations() throws Exception {
        String abc = "X:1\nT:Tongue heel toe canonical\nM:6/4\nL:1/4\nK:C\n"
                + "!double tongue!C !double-tongue!D !triple tongue!E !triple-tongue!F "
                + "!heel mark!G !toe mark!A |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(2, countOccurrences(xml, "<double-tongue/>"), xml);
        assertEquals(2, countOccurrences(xml, "<triple-tongue/>"), xml);
        assertEquals(1, countOccurrences(xml, "<heel/>"), xml);
        assertEquals(1, countOccurrences(xml, "<toe/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(2, countOccurrences(exportedAbc, "!doubletongue!"), exportedAbc);
        assertEquals(2, countOccurrences(exportedAbc, "!tripletongue!"), exportedAbc);
        assertEquals(true, exportedAbc.contains("!heel!"), exportedAbc);
        assertEquals(true, exportedAbc.contains("!toe!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!double tongue!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!double-tongue!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!triple tongue!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!triple-tongue!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!heel mark!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!toe mark!"), exportedAbc);
    }

    @Test
    public void abcImportParsesFingeringStringAndPluckDecorations() throws Exception {
        String abc = "X:1\nT:Fingering string pluck\nM:6/4\nL:1/4\nK:C\n"
                + "!fingering:1!!fingering:4!C !string:1!D !string:4!E "
                + "!0!F !5!G !pluck:p!!pluck:i!!pluck:m!!pluck:a!A |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertEquals(Arrays.asList("1", "4"), directChildTextList(directChild(directChild(notes.get(0), "notations"),
                "technical"), "fingering"));
        assertEquals("1", directChildText(directChild(directChild(notes.get(1), "notations"), "technical"),
                "string"));
        assertEquals("4", directChildText(directChild(directChild(notes.get(2), "notations"), "technical"),
                "string"));
        assertEquals("0", directChildText(directChild(directChild(notes.get(3), "notations"), "technical"),
                "fingering"));
        assertEquals("5", directChildText(directChild(directChild(notes.get(4), "notations"), "technical"),
                "fingering"));
        assertEquals(Arrays.asList("p", "i", "m", "a"), directChildTextList(directChild(directChild(notes.get(5),
                "notations"), "technical"), "pluck"));
    }

    @Test
    public void abcImportParsesStandardDigitFingeringDecorations() throws Exception {
        String abc = "X:1\nT:Standard fingering decorations\nM:6/4\nL:1/4\nK:C\n"
                + "!0!C !1!D !2!E !3!F !4!G !5!A |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<String> fingerings = new ArrayList<String>();
        for (Element note : directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note")) {
            fingerings.add(directChildText(directChild(directChild(note, "notations"), "technical"),
                    "fingering"));
        }

        assertEquals(Arrays.asList("0", "1", "2", "3", "4", "5"), fingerings);
    }

    @Test
    public void musicXmlToAbcExportsFingeringStringAndPluckDecorationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>6</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type><notations><technical>"
                + "<fingering>1</fingering><fingering>4</fingering></technical></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><string>1</string></technical></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><string>4</string></technical></notations></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><string>3</string></technical></notations></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><pluck>p</pluck><pluck>i</pluck></technical></notations></note>"
                + "<note><rest/><duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!1!"), abc);
        assertEquals(true, abc.contains("!4!"), abc);
        assertEquals(true, abc.contains("!string:1!"), abc);
        assertEquals(true, abc.contains("!string:4!"), abc);
        assertEquals(true, abc.contains("!string:3!"), abc);
        assertEquals(true, abc.contains("!pluck:p!"), abc);
        assertEquals(true, abc.contains("!pluck:i!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals(Arrays.asList("1", "4"), directChildTextList(directChild(directChild(notes.get(0), "notations"),
                "technical"), "fingering"));
        assertEquals("1", directChildText(directChild(directChild(notes.get(1), "notations"), "technical"),
                "string"));
        assertEquals("4", directChildText(directChild(directChild(notes.get(2), "notations"), "technical"),
                "string"));
        assertEquals("3", directChildText(directChild(directChild(notes.get(3), "notations"), "technical"),
                "string"));
        assertEquals(Arrays.asList("p", "i"), directChildTextList(directChild(directChild(notes.get(4), "notations"),
                "technical"), "pluck"));
    }

    @Test
    public void musicXmlToAbcExportsDigitFingeringsAsStandardDecorationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><technical><fingering>0</fingering></technical></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type>"
                + "<notations><technical><fingering>5</fingering></technical></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!0!C"), abc);
        assertEquals(true, abc.contains("!5!D"), abc);
        assertEquals(false, abc.contains("!fingering:0!"), abc);
        assertEquals(false, abc.contains("!fingering:5!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals("0", directChildText(directChild(directChild(notes.get(0), "notations"), "technical"),
                "fingering"));
        assertEquals("5", directChildText(directChild(directChild(notes.get(1), "notations"), "technical"),
                "fingering"));
    }

    @Test
    public void musicXmlToAbcExportsAllPluckDecorationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>3840</duration><voice>1</voice><type>whole</type>"
                + "<notations><technical><pluck>p</pluck><pluck>i</pluck>"
                + "<pluck>m</pluck><pluck>a</pluck></technical></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!pluck:p!"), abc);
        assertEquals(true, abc.contains("!pluck:i!"), abc);
        assertEquals(true, abc.contains("!pluck:m!"), abc);
        assertEquals(true, abc.contains("!pluck:a!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals(Arrays.asList("p", "i", "m", "a"), directChildTextList(directChild(directChild(notes.get(0),
                "notations"), "technical"), "pluck"));
    }

    @Test
    public void musicXmlToAbcExportsSingleStringDecorationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><technical><string>2</string></technical></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!string:2!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertEquals("2", directChildText(directChild(directChild(notes.get(0), "notations"), "technical"),
                "string"));
    }

    @Test
    public void abcImportAcceptsOpenStringAndSnapPizzicatoAliases() throws Exception {
        String abc = "X:1\nT:Open and snap aliases\nM:8/4\nL:1/4\nK:C\n"
                + "!open!C !openstring!D !open string!E !open-string!F "
                + "!snap!G !snappizzicato!A !snap pizzicato!B !snap-pizzicato!c |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        for (int i = 0; i < 4; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "technical"),
                    "open-string"));
        }
        for (int i = 4; i < 8; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "technical"),
                    "snap-pizzicato"));
        }
    }

    @Test
    public void musicXmlToAbcExportsOpenStringAndSnapPizzicatoAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>2</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><open-string/></technical></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><snap-pizzicato/></technical></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!open!"), abc);
        assertEquals(true, abc.contains("!snap!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "technical"),
                "open-string"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "technical"),
                "snap-pizzicato"));
    }

    @Test
    public void abcImportRoundtripsOpenStringAndSnapPizzicatoAliasesToCanonicalDecorations() throws Exception {
        String abc = "X:1\nT:Open snap canonical\nM:6/4\nL:1/4\nK:C\n"
                + "!openstring!C !open string!D !open-string!E "
                + "!snappizzicato!F !snap pizzicato!G !snap-pizzicato!A |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(3, countOccurrences(xml, "<open-string/>"), xml);
        assertEquals(3, countOccurrences(xml, "<snap-pizzicato/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(3, countOccurrences(exportedAbc, "!open!"), exportedAbc);
        assertEquals(3, countOccurrences(exportedAbc, "!snap!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!openstring!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!open string!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!open-string!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!snappizzicato!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!snap pizzicato!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!snap-pizzicato!"), exportedAbc);
    }

    @Test
    public void abcImportAcceptsStoppedAndThumbPositionAliases() throws Exception {
        String abc = "X:1\nT:Stopped and thumb aliases\nM:10/4\nL:1/4\nK:C\n"
                + "!stopped!C !plus!D !stopped horn!E !stopped-horn!F "
                + "!thumb!G !thumbpos!A !thumb pos!B !thumb position!c !thumbposition!d !thumb-position!e |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        for (int i = 0; i < 4; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "technical"),
                    "stopped"));
        }
        for (int i = 4; i < 10; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "technical"),
                    "thumb-position"));
        }
    }

    @Test
    public void abcImportParsesStoppedDecoration() throws Exception {
        String abc = "X:1\nT:Stopped\nM:4/4\nL:1/4\nK:C\n"
                + "!stopped!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"), "stopped"));
    }

    @Test
    public void abcImportTreatsPlusAsStoppedDecorationAlias() throws Exception {
        String abc = "X:1\nT:Stopped alias\nM:4/4\nL:1/4\nK:C\n"
                + "!plus!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"), "stopped"));
    }

    @Test
    public void abcImportAcceptsStoppedHornAsStoppedAlias() throws Exception {
        String abc = "X:1\nT:Stopped horn alias\nM:4/4\nL:1/4\nK:C\n"
                + "!stopped horn!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"), "stopped"));
    }

    @Test
    public void abcImportAcceptsStoppedHornHyphenAsStoppedAlias() throws Exception {
        String abc = "X:1\nT:Stopped horn hyphen alias\nM:4/4\nL:1/4\nK:C\n"
                + "!stopped-horn!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"), "stopped"));
    }

    @Test
    public void abcImportParsesHarmonicDecoration() throws Exception {
        String abc = "X:1\nT:Harmonic\nM:4/4\nL:1/4\nK:C\n"
                + "!harmonic!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"), "harmonic"));
    }

    @Test
    public void abcImportParsesThumbDecoration() throws Exception {
        String abc = "X:1\nT:Thumb position\nM:4/4\nL:1/4\nK:C\n"
                + "!thumb!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void abcImportAcceptsThumbposAlias() throws Exception {
        String abc = "X:1\nT:Thumb alias\nM:4/4\nL:1/4\nK:C\n"
                + "!thumbpos!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void abcImportAcceptsThumbPosAlias() throws Exception {
        String abc = "X:1\nT:Thumb spaced alias\nM:4/4\nL:1/4\nK:C\n"
                + "!thumb pos!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void abcImportAcceptsThumbPositionAlias() throws Exception {
        String abc = "X:1\nT:Thumb position alias\nM:4/4\nL:1/4\nK:C\n"
                + "!thumb position!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void abcImportAcceptsThumbpositionAlias() throws Exception {
        String abc = "X:1\nT:Thumbposition alias\nM:4/4\nL:1/4\nK:C\n"
                + "!thumbposition!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void abcImportAcceptsThumbHyphenPositionAlias() throws Exception {
        String abc = "X:1\nT:Thumb-position alias\nM:4/4\nL:1/4\nK:C\n"
                + "!thumb-position!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void musicXmlToAbcExportsHarmonicStoppedAndThumbPositionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><harmonic/></technical></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><stopped/></technical></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><technical><thumb-position/></technical></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!harmonic!"), abc);
        assertEquals(true, abc.contains("!stopped!"), abc);
        assertEquals(true, abc.contains("!thumb!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "technical"), "harmonic"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "technical"), "stopped"));
        assertNotNull(directChild(directChild(directChild(notes.get(2), "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void musicXmlToAbcExportsHarmonicAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><technical><harmonic/></technical></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!harmonic!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "technical"), "harmonic"));
    }

    @Test
    public void musicXmlToAbcExportsThumbPositionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><technical><thumb-position/></technical></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!thumb!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "technical"),
                "thumb-position"));
    }

    @Test
    public void abcImportRoundtripsStoppedAndThumbPositionAliasesToCanonicalDecorations() throws Exception {
        String abc = "X:1\nT:Stopped thumb canonical\nM:10/4\nL:1/4\nK:C\n"
                + "!plus!C !+!D !stopped horn!E !stopped-horn!F "
                + "!thumbpos!G !thumb pos!A !thumb position!B !thumbposition!c !thumb-position!d |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(4, countOccurrences(xml, "<stopped/>"), xml);
        assertEquals(5, countOccurrences(xml, "<thumb-position/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(4, countOccurrences(exportedAbc, "!stopped!"), exportedAbc);
        assertEquals(5, countOccurrences(exportedAbc, "!thumb!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!plus!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!+!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!stopped horn!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!stopped-horn!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!thumbpos!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!thumb pos!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!thumb position!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!thumbposition!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!thumb-position!"), exportedAbc);
    }

    @Test
    public void abcImportParsesMordentDecoration() throws Exception {
        String abc = "X:1\nT:Mordent\nM:4/4\nL:1/4\nK:C\n"
                + "!mordent!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "mordent"));
    }

    @Test
    public void abcImportParsesPralltrillerDecoration() throws Exception {
        String abc = "X:1\nT:Pralltriller\nM:4/4\nL:1/4\nK:C\n"
                + "!pralltriller!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"),
                "inverted-mordent"));
    }

    @Test
    public void abcImportAcceptsMordentAliases() throws Exception {
        String abc = "X:1\nT:Mordent aliases\nM:7/4\nL:1/4\nK:C\n"
                + "!mordent!C !lowermordent!D !pralltriller!E !pralltrill!F "
                + "!prall!G !uppermordent!A !invertedmordent!B |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "mordent"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"), "mordent"));
        for (int i = 2; i < 7; i++) {
            assertNotNull(directChild(directChild(directChild(notes.get(i), "notations"), "ornaments"),
                    "inverted-mordent"));
        }
    }

    @Test
    public void abcImportAcceptsLowermordentAsMordentAlias() throws Exception {
        String abc = "X:1\nT:Lowermordent alias\nM:4/4\nL:1/4\nK:C\n"
                + "!lowermordent!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "mordent"));
    }

    @Test
    public void abcImportAcceptsUppermordentAsInvertedMordentAlias() throws Exception {
        String abc = "X:1\nT:Uppermordent alias\nM:4/4\nL:1/4\nK:C\n"
                + "!uppermordent!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"),
                "inverted-mordent"));
    }

    @Test
    public void abcImportAcceptsPralltrillAsInvertedMordentAlias() throws Exception {
        String abc = "X:1\nT:Pralltrill alias\nM:4/4\nL:1/4\nK:C\n"
                + "!pralltrill!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"),
                "inverted-mordent"));
    }

    @Test
    public void abcImportAcceptsInvertedmordentAsInvertedMordentAlias() throws Exception {
        String abc = "X:1\nT:Inverted mordent alias\nM:4/4\nL:1/4\nK:C\n"
                + "!invertedmordent!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"),
                "inverted-mordent"));
    }

    @Test
    public void abcImportAcceptsInvertedMordentAsInvertedMordentAlias() throws Exception {
        String abc = "X:1\nT:Inverted-mordent alias\nM:4/4\nL:1/4\nK:C\n"
                + "!inverted-mordent!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"),
                "inverted-mordent"));
    }

    @Test
    public void abcImportParsesRollDecorationAsArpeggiate() throws Exception {
        String abc = "X:1\nT:Arpeggiate\nM:4/4\nL:1/4\nK:C\n"
                + "!roll![CEG]2 z2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(firstNote, "notations"), "arpeggiate"));
    }

    @Test
    public void abcImportAcceptsArpeggioAlias() throws Exception {
        String abc = "X:1\nT:Arpeggiate alias\nM:4/4\nL:1/4\nK:C\n"
                + "!arpeggio![CEG]2 z2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(firstNote, "notations"), "arpeggiate"));
    }

    @Test
    public void abcImportAcceptsArpeggiateAlias() throws Exception {
        String abc = "X:1\nT:Arpeggiate alias\nM:4/4\nL:1/4\nK:C\n"
                + "!arpeggiate![CEG]2 z2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(firstNote, "notations"), "arpeggiate"));
    }

    @Test
    public void abcImportParsesSchleiferDecoration() throws Exception {
        String abc = "X:1\nT:Schleifer\nM:4/4\nL:1/4\nK:C\n"
                + "!schleifer!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "schleifer"));
    }

    @Test
    public void abcImportParsesShakeDecoration() throws Exception {
        String abc = "X:1\nT:Shake\nM:4/4\nL:1/4\nK:C\n"
                + "!shake!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element firstNote = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "note").get(0);

        assertNotNull(directChild(directChild(directChild(firstNote, "notations"), "ornaments"), "shake"));
    }

    @Test
    public void abcImportAcceptsArpeggiateAliasesAndMoreOrnaments() throws Exception {
        String abc = "X:1\nT:Arpeggiate aliases\nM:8/4\nL:1/4\nK:C\n"
                + "!roll![CEG]2 !arpeggio![DFA]2 !arpeggiate![EGB]2 !schleifer!F !shake!G |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertNotNull(directChild(directChild(notes.get(0), "notations"), "arpeggiate"));
        assertNotNull(directChild(directChild(notes.get(3), "notations"), "arpeggiate"));
        assertNotNull(directChild(directChild(notes.get(6), "notations"), "arpeggiate"));
        assertNotNull(directChild(directChild(directChild(notes.get(9), "notations"), "ornaments"), "schleifer"));
        assertNotNull(directChild(directChild(directChild(notes.get(10), "notations"), "ornaments"), "shake"));
    }

    @Test
    public void musicXmlToAbcExportsMordentArpeggiateSchleiferAndShakeAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>6</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><mordent/></ornaments></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><inverted-mordent/></ornaments></notations></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><arpeggiate/></notations></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><schleifer/></ornaments></notations></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type>"
                + "<notations><ornaments><shake/></ornaments></notations></note>"
                + "<note><rest/><duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!mordent!"), abc);
        assertEquals(true, abc.contains("!pralltriller!"), abc);
        assertEquals(true, abc.contains("!arpeggio!"), abc);
        assertEquals(true, abc.contains("!schleifer!"), abc);
        assertEquals(true, abc.contains("!shake!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "mordent"));
        assertNotNull(directChild(directChild(directChild(notes.get(1), "notations"), "ornaments"),
                "inverted-mordent"));
        assertNotNull(directChild(directChild(notes.get(2), "notations"), "arpeggiate"));
        assertNotNull(directChild(directChild(directChild(notes.get(3), "notations"), "ornaments"), "schleifer"));
        assertNotNull(directChild(directChild(directChild(notes.get(4), "notations"), "ornaments"), "shake"));
    }

    @Test
    public void musicXmlToAbcExportsMordentAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><ornaments><mordent/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!mordent!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "mordent"));
    }

    @Test
    public void musicXmlToAbcExportsInvertedMordentAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><ornaments><inverted-mordent/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!pralltriller!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"),
                "inverted-mordent"));
    }

    @Test
    public void musicXmlToAbcExportsArpeggiateAsCanonicalArpeggioAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><arpeggiate/></notations></note>"
                + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!arpeggio!["), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(notes.get(0), "notations"), "arpeggiate"));
    }

    @Test
    public void musicXmlToAbcExportsSchleiferAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><ornaments><schleifer/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!schleifer!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "schleifer"));
    }

    @Test
    public void musicXmlToAbcExportsShakeAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type>"
                + "<notations><ornaments><shake/></ornaments></notations></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!shake!"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        assertNotNull(directChild(directChild(directChild(notes.get(0), "notations"), "ornaments"), "shake"));
    }

    @Test
    public void abcImportRoundtripsMordentAndArpeggiateAliasesToCanonicalDecorations() throws Exception {
        String abc = "X:1\nT:Mordent arpeggiate canonical\nM:8/4\nL:1/4\nK:C\n"
                + "!lowermordent!C !uppermordent!D !prall!E !pralltrill!F "
                + "!invertedmordent!G !inverted-mordent!A !roll!B !arpeggiate!c |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(1, countOccurrences(xml, "<mordent/>"), xml);
        assertEquals(5, countOccurrences(xml, "<inverted-mordent/>"), xml);
        assertEquals(2, countOccurrences(xml, "<arpeggiate/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, exportedAbc.contains("!mordent!"), exportedAbc);
        assertEquals(5, countOccurrences(exportedAbc, "!pralltriller!"), exportedAbc);
        assertEquals(2, countOccurrences(exportedAbc, "!arpeggio!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!lowermordent!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!uppermordent!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!prall!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!pralltrill!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!invertedmordent!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!inverted-mordent!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!roll!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!arpeggiate!"), exportedAbc);
    }

    @Test
    public void abcImportParsesSegnoDecorationAsDirection() throws Exception {
        String abc = "X:1\nT:Segno\nM:4/4\nL:1/4\nK:C\n"
                + "!segno!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element directionType = directChild(directChild(measure, "direction"), "direction-type");

        assertNotNull(directChild(directionType, "segno"));
    }

    @Test
    public void abcImportParsesCodaDecorationAsDirection() throws Exception {
        String abc = "X:1\nT:Coda\nM:4/4\nL:1/4\nK:C\n"
                + "!coda!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element directionType = directChild(directChild(measure, "direction"), "direction-type");

        assertNotNull(directChild(directionType, "coda"));
    }

    @Test
    public void abcImportParsesFineDecorationAsSoundDirection() throws Exception {
        String abc = "X:1\nT:Fine\nM:4/4\nL:1/4\nK:C\n"
                + "!fine!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("yes", sound.getAttribute("fine"));
    }

    @Test
    public void abcImportParsesDacapoDecorationAsSoundDirection() throws Exception {
        String abc = "X:1\nT:Da Capo\nM:4/4\nL:1/4\nK:C\n"
                + "!dacapo!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("yes", sound.getAttribute("dacapo"));
    }

    @Test
    public void abcImportAcceptsDaCapoAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:Da Capo alias\nM:4/4\nL:1/4\nK:C\n"
                + "!da capo!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("yes", sound.getAttribute("dacapo"));
    }

    @Test
    public void abcImportAcceptsDaCapoHyphenAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:Da Capo hyphen alias\nM:4/4\nL:1/4\nK:C\n"
                + "!da-capo!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("yes", sound.getAttribute("dacapo"));
    }

    @Test
    public void abcImportAcceptsDcAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:D.C. alias\nM:4/4\nL:1/4\nK:C\n"
                + "!D.C.!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("yes", sound.getAttribute("dacapo"));
    }

    @Test
    public void abcImportParsesDalsegnoDecorationAsSoundDirection() throws Exception {
        String abc = "X:1\nT:Dal Segno\nM:4/4\nL:1/4\nK:C\n"
                + "!dalsegno!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("segno", sound.getAttribute("dalsegno"));
    }

    @Test
    public void abcImportAcceptsDalSegnoAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:Dal Segno alias\nM:4/4\nL:1/4\nK:C\n"
                + "!dal segno!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("segno", sound.getAttribute("dalsegno"));
    }

    @Test
    public void abcImportAcceptsDalSegnoHyphenAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:Dal Segno hyphen alias\nM:4/4\nL:1/4\nK:C\n"
                + "!dal-segno!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("segno", sound.getAttribute("dalsegno"));
    }

    @Test
    public void abcImportAcceptsDsAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:D.S. alias\nM:4/4\nL:1/4\nK:C\n"
                + "!D.S.!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("segno", sound.getAttribute("dalsegno"));
    }

    @Test
    public void abcImportParsesTocodaDecorationAsSoundDirection() throws Exception {
        String abc = "X:1\nT:To Coda\nM:4/4\nL:1/4\nK:C\n"
                + "!tocoda!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("coda", sound.getAttribute("tocoda"));
    }

    @Test
    public void abcImportAcceptsToCodaAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:To Coda alias\nM:4/4\nL:1/4\nK:C\n"
                + "!to coda!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("coda", sound.getAttribute("tocoda"));
    }

    @Test
    public void abcImportAcceptsToCodaHyphenAliasAsSoundDirection() throws Exception {
        String abc = "X:1\nT:To Coda hyphen alias\nM:4/4\nL:1/4\nK:C\n"
                + "!to-coda!C D E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure = directChildren(directChild(root, "part"), "measure").get(0);
        Element sound = directChild(directChild(measure, "direction"), "sound");

        assertEquals("coda", sound.getAttribute("tocoda"));
    }

    @Test
    public void abcImportParsesDacodaDecorationAsDacapoPlusTocodaSoundDirections() throws Exception {
        String abc = "X:1\nT:Da Coda\nM:4/4\nL:1/4\nK:C\n"
                + "!dacoda!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<sound dacapo=\"yes\"/>"));
        assertEquals(true, xml.contains("<sound tocoda=\"coda\"/>"));
    }

    @Test
    public void abcImportParsesCrescendoAndDiminuendoWedgeDecorationsAsDirections() throws Exception {
        String abc = "X:1\nT:Wedges\nM:4/4\nL:1/4\nK:C\n"
                + "!crescendo(!C D !crescendo)!E !diminuendo(!F | !diminuendo)!G A B c |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(1, countOccurrences(xml, "<wedge type=\"crescendo\"/>"));
        assertEquals(1, countOccurrences(xml, "<wedge type=\"diminuendo\"/>"));
        assertEquals(2, countOccurrences(xml, "<wedge type=\"stop\"/>"));
    }

    @Test
    public void abcImportAcceptsCrescDimAndDecrescWedgeAliases() throws Exception {
        String abc = "X:1\nT:Wedge aliases\nM:4/4\nL:1/4\nK:C\n"
                + "!cresc(!C !cresc)!D !dim(!E !decresc)!F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(1, countOccurrences(xml, "<wedge type=\"crescendo\"/>"));
        assertEquals(1, countOccurrences(xml, "<wedge type=\"diminuendo\"/>"));
        assertEquals(2, countOccurrences(xml, "<wedge type=\"stop\"/>"));
    }

    @Test
    public void abcImportAcceptsSymbolicWedgeAliases() throws Exception {
        String abc = "X:1\nT:Symbolic wedge aliases\nM:4/4\nL:1/4\nK:C\n"
                + "!<(!C !<)!D !>(!E !>)!F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(1, countOccurrences(xml, "<wedge type=\"crescendo\"/>"));
        assertEquals(1, countOccurrences(xml, "<wedge type=\"diminuendo\"/>"));
        assertEquals(2, countOccurrences(xml, "<wedge type=\"stop\"/>"));
    }

    @Test
    public void abcImportAcceptsDecrescendoWedgeAlias() throws Exception {
        String abc = "X:1\nT:Decrescendo alias\nM:4/4\nL:1/4\nK:C\n"
                + "!decrescendo(!C D !decrescendo)!E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(1, countOccurrences(xml, "<wedge type=\"diminuendo\"/>"));
        assertEquals(1, countOccurrences(xml, "<wedge type=\"stop\"/>"));
    }

    @Test
    public void abcImportAcceptsDecrescStartWedgeAlias() throws Exception {
        String abc = "X:1\nT:Decresc start alias\nM:4/4\nL:1/4\nK:C\n"
                + "!decresc(!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(1, countOccurrences(xml, "<wedge type=\"diminuendo\"/>"));
    }

    @Test
    public void abcImportAcceptsDimStopWedgeAlias() throws Exception {
        String abc = "X:1\nT:Dim stop alias\nM:4/4\nL:1/4\nK:C\n"
                + "C !dim)!D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(1, countOccurrences(xml, "<wedge type=\"stop\"/>"));
    }

    @Test
    public void abcImportParsesSfzDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Sforzato\nM:4/4\nL:1/4\nK:C\n"
                + "!sfz!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<sfz/>"));
    }

    @Test
    public void abcImportParsesSfDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Sforzando\nM:4/4\nL:1/4\nK:C\n"
                + "!sf!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<sf/>"));
    }

    @Test
    public void abcImportParsesSfpDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Sforzando piano\nM:4/4\nL:1/4\nK:C\n"
                + "!sfp!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<sfp/>"));
    }

    @Test
    public void abcImportParsesRfzDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Rinforzando\nM:4/4\nL:1/4\nK:C\n"
                + "!rfz!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<rfz/>"));
    }

    @Test
    public void abcImportParsesPDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Piano\nM:4/4\nL:1/4\nK:C\n"
                + "!p!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<p/>"));
    }

    @Test
    public void abcImportParsesPpDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Pianissimo\nM:4/4\nL:1/4\nK:C\n"
                + "!pp!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<pp/>"));
    }

    @Test
    public void abcImportParsesFfDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Fortissimo\nM:4/4\nL:1/4\nK:C\n"
                + "!ff!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<ff/>"));
    }

    @Test
    public void abcImportParsesFDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Forte\nM:4/4\nL:1/4\nK:C\n"
                + "!f!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<f/>"));
    }

    @Test
    public void abcImportParsesFffDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Fortississimo\nM:4/4\nL:1/4\nK:C\n"
                + "!fff!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<fff/>"));
    }

    @Test
    public void abcImportParsesFpDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Fortepiano\nM:4/4\nL:1/4\nK:C\n"
                + "!fp!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<fp/>"));
    }

    @Test
    public void abcImportParsesPppDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Pianississimo\nM:4/4\nL:1/4\nK:C\n"
                + "!ppp!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<ppp/>"));
    }

    @Test
    public void abcImportParsesMpDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Mezzo piano\nM:4/4\nL:1/4\nK:C\n"
                + "!mp!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<mp/>"));
    }

    @Test
    public void abcImportParsesMfDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Mezzo forte\nM:4/4\nL:1/4\nK:C\n"
                + "!mf!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<mf/>"));
    }

    @Test
    public void abcImportParsesFzDecorationAsDynamicsDirection() throws Exception {
        String abc = "X:1\nT:Forzando\nM:4/4\nL:1/4\nK:C\n"
                + "!fz!C D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<fz/>"));
    }

    @Test
    public void abcImportParsesCommonDynamicDecorationsAsDynamicsDirections() throws Exception {
        String abc = "X:1\nT:Common dynamics\nM:2/1\nL:1/4\nK:C\n"
                + "!ppp!C !mp!D !mf!E !ff!F | !fp!G !fz!A !rfz!B !sfp!c |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        for (String dynamic : Arrays.asList("ppp", "mp", "mf", "ff", "fp", "fz", "rfz", "sfp")) {
            assertEquals(true, xml.contains("<" + dynamic + "/>"), dynamic + " missing:\n" + xml);
        }
    }

    @Test
    public void abcImportParsesPpppAndFfffDecorationsAsDynamicsDirections() throws Exception {
        String abc = "X:1\nT:Extreme dynamics\nM:2/4\nL:1/4\nK:C\n"
                + "!pppp!C !ffff!D |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<pppp/>"));
        assertEquals(true, xml.contains("<ffff/>"));
    }

    @Test
    public void abcImportParsesSegnoCodaFineAndDaCodaDirections() throws Exception {
        String abc = "X:1\nT:Navigation directions\nM:4/4\nL:1/4\nK:C\n"
                + "!segno!C !coda!D !fine!E !dacoda!F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("<segno/>"));
        assertEquals(true, xml.contains("<coda/>"));
        assertEquals(true, xml.contains("<sound fine=\"yes\"/>"));
        assertEquals(true, xml.contains("<sound dacapo=\"yes\"/>"));
        assertEquals(true, xml.contains("<sound tocoda=\"coda\"/>"));
    }

    @Test
    public void abcImportAcceptsDaCapoDalSegnoAndToCodaAliases() throws Exception {
        String abc = "X:1\nT:Navigation aliases\nM:9/4\nL:1/4\nK:C\n"
                + "!dacapo!C !da capo!D !da-capo!E !D.C.!F "
                + "!dalsegno!G !dal segno!A !dal-segno!B !D.S.!c "
                + "!tocoda!d !to coda!e !to-coda!f |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(4, countOccurrences(xml, "<sound dacapo=\"yes\"/>"));
        assertEquals(4, countOccurrences(xml, "<sound dalsegno=\"segno\"/>"));
        assertEquals(3, countOccurrences(xml, "<sound tocoda=\"coda\"/>"));
    }

    @Test
    public void musicXmlToAbcExportsNavigationDirectionsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>8</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><segno/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<direction><direction-type><coda/></direction-type></direction>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<direction><sound fine=\"yes\"/></direction>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<direction><sound dacapo=\"yes\"/></direction>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<direction><sound dalsegno=\"segno\"/></direction>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<direction><sound tocoda=\"coda\"/></direction>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<direction><sound dacapo=\"yes\" tocoda=\"coda\"/></direction>"
                + "<note><pitch><step>B</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!segno!C"), abc);
        assertEquals(true, abc.contains("!coda!D"), abc);
        assertEquals(true, abc.contains("!fine!E"), abc);
        assertEquals(true, abc.contains("!dacapo!F"), abc);
        assertEquals(true, abc.contains("!dalsegno!G"), abc);
        assertEquals(true, abc.contains("!tocoda!A"), abc);
        assertEquals(true, abc.contains("!dacoda!B"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<segno/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<coda/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<sound fine=\"yes\"/>"), roundtripXml);
        assertEquals(2, countOccurrences(roundtripXml, "<sound dacapo=\"yes\"/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<sound dalsegno=\"segno\"/>"), roundtripXml);
        assertEquals(2, countOccurrences(roundtripXml, "<sound tocoda=\"coda\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsSegnoDirectionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><segno/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!segno!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<segno/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsCodaDirectionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><coda/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!coda!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<coda/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsFineDirectionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><sound fine=\"yes\"/></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!fine!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<sound fine=\"yes\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsDaCapoDirectionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><sound dacapo=\"yes\"/></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!dacapo!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<sound dacapo=\"yes\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsDalSegnoDirectionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><sound dalsegno=\"segno\"/></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!dalsegno!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<sound dalsegno=\"segno\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsToCodaDirectionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><sound tocoda=\"coda\"/></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!tocoda!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<sound tocoda=\"coda\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsDaCodaDirectionAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "</part-list><part id=\"P1\"><measure number=\"1\">"
                + "<attributes><divisions>960</divisions><key><fifths>0</fifths></key>"
                + "<time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><sound dacapo=\"yes\" tocoda=\"coda\"/></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!dacoda!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<sound dacapo=\"yes\"/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<sound tocoda=\"coda\"/>"), roundtripXml);
    }

    @Test
    public void abcImportRoundtripsNavigationAliasesToCanonicalDecorations() throws Exception {
        String abc = "X:1\nT:Navigation canonical\nM:9/4\nL:1/4\nK:C\n"
                + "!da capo!C !da-capo!D !D.C.!E "
                + "!dal segno!F !dal-segno!G !D.S.!A "
                + "!to coda!B !to-coda!c !dacoda!d |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(4, countOccurrences(xml, "dacapo=\"yes\""), xml);
        assertEquals(3, countOccurrences(xml, "dalsegno=\"segno\""), xml);
        assertEquals(3, countOccurrences(xml, "tocoda=\"coda\""), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(4, countOccurrences(exportedAbc, "!dacapo!"), exportedAbc);
        assertEquals(3, countOccurrences(exportedAbc, "!dalsegno!"), exportedAbc);
        assertEquals(3, countOccurrences(exportedAbc, "!tocoda!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!dacoda!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!da capo!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!da-capo!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!D.C.!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!dal segno!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!dal-segno!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!D.S.!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!to coda!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!to-coda!"), exportedAbc);
    }

    @Test
    public void abcImportAcceptsWedgeDecorationAliases() throws Exception {
        String abc = "X:1\nT:Wedge aliases\nM:8/4\nL:1/4\nK:C\n"
                + "!crescendo(!C !crescendo)!D !cresc(!E !cresc)!F "
                + "!<(!G !<)!A !diminuendo(!B !diminuendo)!c "
                + "!decrescendo(!d !decrescendo)!e !dim(!f !dim)!g "
                + "!decresc(!a !decresc)!b !>(!c' !>)!d' |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(3, countOccurrences(xml, "<wedge type=\"crescendo\"/>"));
        assertEquals(5, countOccurrences(xml, "<wedge type=\"diminuendo\"/>"));
        assertEquals(8, countOccurrences(xml, "<wedge type=\"stop\"/>"));
    }

    @Test
    public void musicXmlToAbcExportsWedgeDirectionsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><wedge type=\"crescendo\"/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<direction><direction-type><wedge type=\"stop\"/></direction-type></direction>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<direction><direction-type><wedge type=\"diminuendo\"/></direction-type></direction>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<direction><direction-type><wedge type=\"stop\"/></direction-type></direction>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!crescendo(!C"), abc);
        assertEquals(true, abc.contains("!crescendo)!D"), abc);
        assertEquals(true, abc.contains("!diminuendo(!E"), abc);
        assertEquals(true, abc.contains("!diminuendo)!F"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(1, countOccurrences(roundtripXml, "<wedge type=\"crescendo\"/>"), roundtripXml);
        assertEquals(1, countOccurrences(roundtripXml, "<wedge type=\"diminuendo\"/>"), roundtripXml);
        assertEquals(2, countOccurrences(roundtripXml, "<wedge type=\"stop\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsCrescendoWedgeStartAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><wedge type=\"crescendo\"/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>3840</duration><voice>1</voice><type>whole</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!crescendo(!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(1, countOccurrences(roundtripXml, "<wedge type=\"crescendo\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsWedgeStopAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><wedge type=\"stop\"/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>3840</duration><voice>1</voice><type>whole</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains(")!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(1, countOccurrences(roundtripXml, "<wedge type=\"stop\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsDiminuendoWedgeStartAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><wedge type=\"diminuendo\"/></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>3840</duration><voice>1</voice><type>whole</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("!diminuendo(!C"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(1, countOccurrences(roundtripXml, "<wedge type=\"diminuendo\"/>"), roundtripXml);
    }

    @Test
    public void abcImportRoundtripsWedgeAliasesToCanonicalDecorations() throws Exception {
        String abc = "X:1\nT:Wedge alias canonical\nM:4/4\nL:1/4\nK:C\n"
                + "!<(!C !<)!D !>(!E !>)!F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(1, countOccurrences(xml, "<wedge type=\"crescendo\"/>"), xml);
        assertEquals(1, countOccurrences(xml, "<wedge type=\"diminuendo\"/>"), xml);
        assertEquals(2, countOccurrences(xml, "<wedge type=\"stop\"/>"), xml);

        String exportedAbc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, exportedAbc.contains("!crescendo(!"), exportedAbc);
        assertEquals(true, exportedAbc.contains("!crescendo)!"), exportedAbc);
        assertEquals(true, exportedAbc.contains("!diminuendo(!"), exportedAbc);
        assertEquals(true, exportedAbc.contains("!diminuendo)!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!<(!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!<)!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!>(!"), exportedAbc);
        assertEquals(false, exportedAbc.contains("!>)!"), exportedAbc);
    }

    @Test
    public void abcImportParsesCommonAndExtremeDynamics() throws Exception {
        String abc = "X:1\nT:Dynamics aliases\nM:16/4\nL:1/4\nK:C\n"
                + "!pppp!C !ppp!D !pp!E !p!F !mp!G !mf!A !f!B !ff!c "
                + "!fff!d !ffff!e !fp!f !fz!g !rfz!a !sf!b !sfp!c' !sfz!d' |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        for (String dynamic : Arrays.asList("pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff", "ffff",
                "fp", "fz", "rfz", "sf", "sfp", "sfz")) {
            assertEquals(true, xml.contains("<" + dynamic + "/>"), dynamic + " missing:\n" + xml);
        }
    }

    @Test
    public void musicXmlToAbcExportsDynamicsAndRoundtrips() throws Exception {
        List<String> dynamics = Arrays.asList("pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff", "ffff",
                "fp", "fz", "rfz", "sf", "sfp", "sfz");
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>16</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>");
        String[] steps = { "C", "D", "E", "F", "G", "A", "B" };
        for (int i = 0; i < dynamics.size(); i++) {
            String dynamic = dynamics.get(i);
            xml.append("<direction><direction-type><dynamics><").append(dynamic)
                    .append("/></dynamics></direction-type></direction>")
                    .append("<note><pitch><step>").append(steps[i % steps.length]).append("</step><octave>")
                    .append(i < steps.length ? "4" : "5")
                    .append("</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type></note>");
        }
        xml.append("</measure></part></score-partwise>");

        String abc = AbcIo.musicXmlToAbc(xml.toString());
        for (String dynamic : dynamics) {
            assertEquals(true, abc.contains("!" + dynamic + "!"), dynamic + " missing:\n" + abc);
        }

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        for (String dynamic : dynamics) {
            assertEquals(true, roundtripXml.contains("<" + dynamic + "/>"), dynamic + " missing:\n"
                    + roundtripXml);
        }
    }

    @Test
    public void abcImportSplitsBeamsAtBeatBoundaries() throws Exception {
        String abc = "X:1\nT:Beam test\nM:2/4\nL:1/8\nK:C\nV:1\n"
                + "CDEF |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertEquals(Arrays.asList("begin", "end", "begin", "end"), beamTexts(notes));
    }

    @Test
    public void abcImportTreatsWhitespaceAsExplicitBeamBreakHint() throws Exception {
        String abc = "X:1\nT:Whitespace beam test\nM:2/4\nL:1/16\nK:C\nV:1\n"
                + "CD EF GA Bc |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertEquals(Arrays.asList("begin", "end", "begin", "end", "begin", "end", "begin", "end"),
                beamTexts(notes));
    }

    @Test
    public void musicXmlToAbcDoesNotPreserveBeamGroupingThroughExactSpacing() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>2</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<beam number=\"1\">begin</beam></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<beam number=\"1\">end</beam></note>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<beam number=\"1\">begin</beam></note>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch>"
                + "<duration>480</duration><voice>1</voice><type>eighth</type>"
                + "<beam number=\"1\">end</beam></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);

        assertEquals(true, abc.contains("V:P1"), abc);
        assertEquals(true, abc.contains("C D E F |"), abc);
        assertEquals(false, abc.contains("CD EF"), abc);
    }

    @Test
    public void parsesAbcStandardShorthandDecorationsIntoMusicXml() {
        String abc = "X:1\nT:Standard Shorthand ABC\nM:5/4\nL:1/8\nK:C\n~C H D L E M F O G P A S B T c u d v e|]\n";

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
    public void parsesAbcPrefixedDecorationsAndAccidentalAnnotationsIntoMusicXml() {
        String abc = "X:1\nT:Prefixed Decoration ABC\nM:4/4\nL:1/4\nK:C\n"
                + "!rehearsal:A1!!fingering:2!!string:3!!pluck:pizz!C !editorial!^D !courtesy!=F G|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals("A1", notes.get(0).getRehearsalMark());
        assertEquals(Arrays.asList("2"), notes.get(0).getFingerings());
        assertEquals(Arrays.asList("3"), notes.get(0).getStrings());
        assertEquals(Arrays.asList("pizz"), notes.get(0).getPlucks());
        assertEquals(true, notes.get(1).isAccidentalEditorial());
        assertEquals(false, notes.get(1).isAccidentalCautionary());
        assertEquals(false, notes.get(2).isAccidentalEditorial());
        assertEquals(true, notes.get(2).isAccidentalCautionary());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<rehearsal>A1</rehearsal>"));
        assertEquals(true, xml.contains("<fingering>2</fingering>"));
        assertEquals(true, xml.contains("<string>3</string>"));
        assertEquals(true, xml.contains("<pluck>pizz</pluck>"));
        assertEquals(true, xml.contains("<accidental editorial=\"yes\">sharp</accidental>"));
        assertEquals(true, xml.contains("<accidental cautionary=\"yes\">natural</accidental>"));
    }

    @Test
    public void parsesAbcNavigationWedgeAndDynamicDecorationsIntoMusicXml() {
        String abc = "X:1\nT:Direction Decoration ABC\nM:5/4\nL:1/8\nK:C\n"
                + "!fine!C !dacapo!D !dalsegno!E !tocoda!F !crescendo(!G !crescendo)!A !diminuendo(!B !diminuendo)!c !mf!d !sfz!e|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(true, notes.get(0).isFine());
        assertEquals(true, notes.get(1).isDaCapo());
        assertEquals(true, notes.get(2).isDalSegno());
        assertEquals(true, notes.get(3).isToCoda());
        assertEquals(true, notes.get(4).isCrescendoStart());
        assertEquals(true, notes.get(5).isCrescendoStop());
        assertEquals(true, notes.get(6).isDiminuendoStart());
        assertEquals(true, notes.get(7).isDiminuendoStop());
        assertEquals("mf", notes.get(8).getDynamicMark());
        assertEquals(true, notes.get(9).isSfz());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<sound fine=\"yes\"/>"));
        assertEquals(true, xml.contains("<sound dacapo=\"yes\"/>"));
        assertEquals(true, xml.contains("<sound dalsegno=\"segno\"/>"));
        assertEquals(true, xml.contains("<sound tocoda=\"coda\"/>"));
        assertEquals(true, xml.contains("<wedge type=\"crescendo\"/>"));
        assertEquals(true, xml.contains("<wedge type=\"diminuendo\"/>"));
        assertEquals(true, xml.contains("<wedge type=\"stop\"/>"));
        assertEquals(true, xml.contains("<mf/>"));
        assertEquals(true, xml.contains("<sfz/>"));
    }

    @Test
    public void parsesAbcOrnamentArticulationAndTechnicalDecorationAliasesIntoMusicXml() {
        String abc = "X:1\nT:Decoration Alias ABC\nM:15/4\nL:1/8\nK:C\n"
                + "!turn!C !turnx!D !invertedturn!E !invertedturnx!F !delayedturn!G !delayedinvertedturn!A "
                + "!tremolo-single-3!B !gliss-start!c !gliss-stop!d !slide!e !slide-stop!f !schleifer!g "
                + "!shake!a !staccatissimo!b !tenuto!c' !stress!d' !unstress!e' !marcato!f' !breath!g' "
                + "!caesura!a' !shortphrase!b' !doubletongue!c'' !tripletongue!d'' !heel!e'' !toe!f'' "
                + "!open!g'' !snap!a'' !harmonic!b'' !stopped!c''' !thumb!d'''|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals("turn", notes.get(0).getTurnType());
        assertEquals(true, notes.get(1).isTurnSlash());
        assertEquals("inverted-turn", notes.get(2).getTurnType());
        assertEquals(true, notes.get(3).isTurnSlash());
        assertEquals(true, notes.get(4).isDelayedTurn());
        assertEquals("inverted-turn", notes.get(5).getTurnType());
        assertEquals("single", notes.get(6).getTremoloType());
        assertEquals(Integer.valueOf(3), notes.get(6).getTremoloMarks());
        assertEquals(true, notes.get(7).isGlissandoStart());
        assertEquals(true, notes.get(8).isGlissandoStop());
        assertEquals(true, notes.get(9).isSlideStart());
        assertEquals(true, notes.get(10).isSlideStop());
        assertEquals(true, notes.get(11).isSchleifer());
        assertEquals(true, notes.get(12).isShake());
        assertEquals(true, notes.get(13).isStaccatissimo());
        assertEquals(true, notes.get(14).isTenuto());
        assertEquals(true, notes.get(15).isStress());
        assertEquals(true, notes.get(16).isUnstress());
        assertEquals(true, notes.get(17).isStrongAccent());
        assertEquals(true, notes.get(18).isBreathMark());
        assertEquals(true, notes.get(19).isCaesura());
        assertEquals("shortphrase", notes.get(20).getPhraseMark());
        assertEquals(true, notes.get(21).isDoubleTongue());
        assertEquals(true, notes.get(22).isTripleTongue());
        assertEquals(true, notes.get(23).isHeel());
        assertEquals(true, notes.get(24).isToe());
        assertEquals(true, notes.get(25).isOpenString());
        assertEquals(true, notes.get(26).isSnapPizzicato());
        assertEquals(true, notes.get(27).isHarmonic());
        assertEquals(true, notes.get(28).isStopped());
        assertEquals(true, notes.get(29).isThumbPosition());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<turn/>"));
        assertEquals(true, xml.contains("<turn slash=\"yes\"/>"));
        assertEquals(true, xml.contains("<inverted-turn/>"));
        assertEquals(true, xml.contains("<delayed-turn/>"));
        assertEquals(true, xml.contains("<tremolo type=\"single\">3</tremolo>"));
        assertEquals(true, xml.contains("<glissando type=\"start\" number=\"1\">wavy</glissando>"));
        assertEquals(true, xml.contains("<slide type=\"stop\" number=\"1\"/>"));
        assertEquals(true, xml.contains("<schleifer/>"));
        assertEquals(true, xml.contains("<shake/>"));
        assertEquals(true, xml.contains("<staccatissimo/>"));
        assertEquals(true, xml.contains("<tenuto/>"));
        assertEquals(true, xml.contains("<stress/>"));
        assertEquals(true, xml.contains("<unstress/>"));
        assertEquals(true, xml.contains("<strong-accent/>"));
        assertEquals(true, xml.contains("<breath-mark/>"));
        assertEquals(true, xml.contains("<caesura/>"));
        assertEquals(true, xml.contains("<other-articulation>shortphrase</other-articulation>"));
        assertEquals(true, xml.contains("<double-tongue/>"));
        assertEquals(true, xml.contains("<triple-tongue/>"));
        assertEquals(true, xml.contains("<heel/>"));
        assertEquals(true, xml.contains("<toe/>"));
        assertEquals(true, xml.contains("<open-string/>"));
        assertEquals(true, xml.contains("<snap-pizzicato/>"));
        assertEquals(true, xml.contains("<harmonic/>"));
        assertEquals(true, xml.contains("<stopped/>"));
        assertEquals(true, xml.contains("<thumb-position/>"));
    }

    @Test
    public void parsesAbcStoppedHornAliasAsStoppedTechnicalMark() {
        String abc = "X:1\nT:Stopped Horn Alias ABC\nM:4/4\nL:1/4\nK:C\n!stopped-horn!C D E F|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals(true, notes.get(0).isStopped());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<stopped/>"));
    }

    @Test
    public void parsesAbcPrallDecorationAliasesAsInvertedMordent() {
        String abc = "X:1\nT:Prall Alias ABC\nM:3/4\nL:1/4\nK:C\n!prall!C !pralltrill!D !pralltriller!E|]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<AbcIo.AbcMeasureNote> notes = parsed.getParts().get(0).getMeasures().get(0);
        assertEquals("inverted-mordent", notes.get(0).getMordentType());
        assertEquals("inverted-mordent", notes.get(1).getMordentType());
        assertEquals("inverted-mordent", notes.get(2).getMordentType());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<inverted-mordent/>"));
    }

    @Test
    public void reflowsOverfullAbcMeasuresByDefaultWithDiagnostics() {
        String abc = "X:1\nT:Overfull ABC\nM:4/4\nL:1/4\nK:C\nC D E F G | A B c d |]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        List<List<AbcIo.AbcMeasureNote>> measures = parsed.getParts().get(0).getMeasures();
        assertEquals(3, measures.size());
        assertEquals(4, measures.get(0).size());
        assertEquals(4, measures.get(1).size());
        assertEquals(1, measures.get(2).size());
        assertEquals("G", measures.get(1).get(0).getStep());
        assertEquals("D", measures.get(2).get(0).getStep());

        List<AbcIo.AbcImportDiagnostic> diagnostics = parsed.getDiagnostics();
        assertEquals(2, diagnostics.size());
        assertEquals("OVERFULL_REFLOWED", diagnostics.get(0).getCode());
        assertEquals(Integer.valueOf(1), diagnostics.get(0).getMeasure());
        assertEquals("reflowed", diagnostics.get(0).getAction());
        assertEquals(Integer.valueOf(1), diagnostics.get(0).getMovedEvents());
        assertEquals("OVERFULL_REFLOWED", diagnostics.get(1).getCode());
        assertEquals(Integer.valueOf(2), diagnostics.get(1).getMeasure());
    }

    @Test
    public void preservesOverfullAbcMeasuresWhenCompatibilityModeIsDisabled() {
        String abc = "X:1\nT:Overfull ABC\nM:4/4\nL:1/4\nK:C\nC D E F G | A B c d |]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc,
                new AbcIo.AbcImportOptions(null, null, null, Boolean.FALSE));
        List<List<AbcIo.AbcMeasureNote>> measures = parsed.getParts().get(0).getMeasures();
        assertEquals(2, measures.size());
        assertEquals(5, measures.get(0).size());
        assertEquals(4, measures.get(1).size());
        assertEquals(true, parsed.getDiagnostics().isEmpty());
    }

    @Test
    public void abcImportReflowsOverfullMeasureContentForSaveCompatibility() throws Exception {
        String abc = "X:1\nT:Overfull\nM:4/4\nL:1/8\nK:C\nV:1\nV:1\nC D E F G A B c d |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);

        assertEquals(true, MusicXmlState.validateMusicXmlForSave(xml, true).isOk(), xml);
        assertEquals(true, directChildren(directChild(root, "part"), "measure").size() >= 2);
        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"), xml);
        assertEquals(true, xml.contains("code=OVERFULL_REFLOWED"), xml);
    }

    @Test
    public void abcImportCanDisableOverfullCompatibilityReflow() {
        String abc = "X:1\nT:Overfull strict\nM:4/4\nL:1/8\nK:C\nV:1\nC D E F G A B c d |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions(null, null, null, Boolean.FALSE));

        assertEquals(false, xml.contains("mks:diag:count"), xml);
        assertEquals(false, MusicXmlState.validateMusicXmlForSave(xml, true).isOk(), xml);
        assertEquals("MEASURE_OVERFULL",
                MusicXmlState.validateMusicXmlForSave(xml, true).getDiagnostics().get(0).getCode());
    }

    @Test
    public void abcImportRecordsParserFallbackWarningsIntoDiagnostics() {
        String abc = "X:1\nT:Bad header\nM:not-a-meter\nL:1/8\nK:C\nV:1\nC D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, xml.contains("mks:diag:count"), xml);
        assertEquals(true, xml.contains("code=ABC_IMPORT_WARNING"), xml);
    }

    @Test
    public void abcImportSupportsInlineKeyMeterLengthAndTempoFields() throws Exception {
        String abc = "X:1\nT:Inline fields\nM:4/4\nL:1/8\nK:C\n"
                + "C D E F | [K:G] [M:3/4] [L:1/4] [Q:1/4=132] G A B |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        Element measure2 = directChildren(directChild(root, "part"), "measure").get(1);
        Element attributes = directChild(measure2, "attributes");
        Element time = directChild(attributes, "time");
        List<Element> notes = directChildren(measure2, "note");

        assertEquals("1", directChildText(directChild(attributes, "key"), "fifths"));
        assertEquals("3", directChildText(time, "beats"));
        assertEquals("4", directChildText(time, "beat-type"));
        assertEquals("960", directChildText(notes.get(0), "duration"));
        assertEquals("132", directChildText(directChild(directChild(directChild(measure2, "direction"),
                "direction-type"), "metronome"), "per-minute"));
        assertEquals("132", directChild(directChild(measure2, "direction"), "sound").getAttribute("tempo"));
    }

    @Test
    public void abcImportAcceptsContinuedBodyLinesWithStandaloneKeyFields() throws Exception {
        String abc = "X:1\nT:Keys and modes\nM:4/4\nL:1/8\nK:C\n"
                + "CDEF GABc |\\\n"
                + "K:CMAJOR\n"
                + "CDEF GABc |\\\n"
                + "K:Cmajor\n"
                + "CDEF GABc |]\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> measures = directChildren(directChild(root, "part"), "measure");

        assertEquals("0", directChildText(directChild(directChild(measures.get(1), "attributes"), "key"), "fifths"));
        assertEquals("0", directChildText(directChild(directChild(measures.get(2), "attributes"), "key"), "fifths"));
    }

    @Test
    public void abcImportWarnsAndSkipsUnsupportedContinuedHeaderFieldText() throws Exception {
        String abc = "X:1\nT:Continued title\\\nstill title text\nM:4/4\nL:1/8\nK:C\nC D E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(xml);

        assertEquals(4, directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note").size());
        assertEquals(true, xml.contains("<miscellaneous-field name=\"mks:diag:count\">2</miscellaneous-field>"), xml);
        assertEquals(true, xml.contains("Unsupported continued field after T:"), xml);
        assertEquals(true, xml.contains("Skipped unsupported continued field text for T:"), xml);
    }

    @Test
    public void abcImportMapsQuotedChordSymbolsToHarmonyAndAnnotationsToWords() throws Exception {
        String abc = "X:1\nT:Quoted annotation\nM:4/4\nL:1/8\nK:C\n\"Am\"C D \"rit.\"E F |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(xml), "part"), "measure").get(0);
        Element harmony = directChild(measure, "harmony");

        assertEquals("A", directChildText(directChild(harmony, "root"), "root-step"));
        assertEquals("minor", directChildText(harmony, "kind"));
        assertEquals("Am", directChild(harmony, "kind").getAttribute("text"));
        assertEquals(true, xml.contains("<words>rit.</words>"), xml);
        assertEquals(false, xml.contains("<words>Am</words>"), xml);
        assertEquals(false, xml.contains("mks:diag:count"), xml);
    }

    @Test
    public void abcImportMapsExtendedAndSlashQuotedChordSymbolsToHarmony() throws Exception {
        String abc = "X:1\nT:Richer chord symbols\nM:4/4\nL:1/8\nK:C\n"
                + "\"C6\"C \"Dm6\"D \"G9\"E \"Fmaj9\"F |\n"
                + "\"Em9\"C \"G11/B\"D \"A13\"E \"D7sus4/F#\"F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> measure1Harmonies = directChildren(directChildren(directChild(root, "part"), "measure").get(0),
                "harmony");
        List<Element> measure2Harmonies = directChildren(directChildren(directChild(root, "part"), "measure").get(1),
                "harmony");

        assertEquals("major-sixth", directChildText(measure1Harmonies.get(0), "kind"));
        assertEquals("minor-sixth", directChildText(measure1Harmonies.get(1), "kind"));
        assertEquals("dominant-ninth", directChildText(measure1Harmonies.get(2), "kind"));
        assertEquals("major-ninth", directChildText(measure1Harmonies.get(3), "kind"));
        assertEquals("minor-ninth", directChildText(measure2Harmonies.get(0), "kind"));
        assertEquals("dominant-11th", directChildText(measure2Harmonies.get(1), "kind"));
        assertEquals("dominant-13th", directChildText(measure2Harmonies.get(2), "kind"));
        assertEquals("suspended-fourth", directChildText(measure2Harmonies.get(3), "kind"));
        assertEquals("B", directChildText(directChild(measure2Harmonies.get(1), "bass"), "bass-step"));
        assertEquals("F", directChildText(directChild(measure2Harmonies.get(3), "bass"), "bass-step"));
        assertEquals("1", directChildText(directChild(measure2Harmonies.get(3), "bass"), "bass-alter"));
    }

    @Test
    public void abcImportKeepsUnsupportedQuotedChordLikeTextAsAnnotation() throws Exception {
        String abc = "X:1\nT:Unsupported chord inventory\nM:4/4\nL:1/8\nK:C\n\"Cadd9\"C \"Fmaj13\"D |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(xml), "part"), "measure").get(0);

        assertEquals(0, directChildren(measure, "harmony").size());
        assertEquals(true, xml.contains("<words>Cadd9</words>"), xml);
        assertEquals(true, xml.contains("<words>Fmaj13</words>"), xml);
    }

    @Test
    public void musicXmlToAbcExportsUnsupportedChordLikeWordsAsQuotedAnnotationsAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><words>Cadd9</words></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>eighth</type></note>"
                + "<direction><direction-type><words>Fmaj13</words></direction-type></direction>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>eighth</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(roundtripped), "part"), "measure").get(0);

        assertEquals(true, abc.contains("\"Cadd9\"C"), abc);
        assertEquals(true, abc.contains("\"Fmaj13\"D"), abc);
        assertEquals(0, directChildren(measure, "harmony").size());
        assertEquals(true, roundtripped.contains("<words>Cadd9</words>"), roundtripped);
        assertEquals(true, roundtripped.contains("<words>Fmaj13</words>"), roundtripped);
    }

    @Test
    public void musicXmlToAbcExportsWordsDirectionAsQuotedAnnotationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><words>rit.</words></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("\"rit.\"C"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripped.contains("<words>rit.</words>"), roundtripped);
    }

    @Test
    public void musicXmlToAbcExportsBasicHarmonyAsQuotedChordSymbolAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<harmony><root><root-step>C</root-step></root><kind>dominant</kind></harmony>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("\"C7\"C"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(roundtripped), "part"), "measure").get(0);
        Element harmony = directChild(measure, "harmony");
        assertEquals("C", directChildText(directChild(harmony, "root"), "root-step"));
        assertEquals("dominant", directChildText(harmony, "kind"));
        assertEquals("C7", directChild(harmony, "kind").getAttribute("text"));
    }

    @Test
    public void musicXmlToAbcExportsSlashBassHarmonyAsQuotedChordSymbolAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<harmony><root><root-step>F</root-step><root-alter>1</root-alter></root>"
                + "<kind>half-diminished</kind><bass><bass-step>C</bass-step>"
                + "<bass-alter>1</bass-alter></bass></harmony>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("\"F#m7b5/C#\"C"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(roundtripped), "part"), "measure").get(0);
        Element harmony = directChild(measure, "harmony");
        assertEquals("F", directChildText(directChild(harmony, "root"), "root-step"));
        assertEquals("1", directChildText(directChild(harmony, "root"), "root-alter"));
        assertEquals("half-diminished", directChildText(harmony, "kind"));
        assertEquals("C", directChildText(directChild(harmony, "bass"), "bass-step"));
        assertEquals("1", directChildText(directChild(harmony, "bass"), "bass-alter"));
    }

    @Test
    public void musicXmlToAbcExportsMajorSixthHarmonyAsQuotedChordSymbolAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<harmony><root><root-step>C</root-step></root><kind>major-sixth</kind></harmony>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch>"
                + "<duration>960</duration><voice>1</voice><type>quarter</type></note>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("\"C6\"C"), abc);

        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element measure = directChildren(directChild(parseElement(roundtripped), "part"), "measure").get(0);
        Element harmony = directChild(measure, "harmony");
        assertEquals("C", directChildText(directChild(harmony, "root"), "root-step"));
        assertEquals("major-sixth", directChildText(harmony, "kind"));
        assertEquals("C6", directChild(harmony, "kind").getAttribute("text"));
    }

    @Test
    public void musicXmlToAbcExportsExtendedHarmonyKindsAsQuotedChordSymbols() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<harmony><root><root-step>C</root-step></root><kind>major-sixth</kind></harmony>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>eighth</type></note>"
                + "<harmony><root><root-step>D</root-step></root><kind>minor-sixth</kind></harmony>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>eighth</type></note>"
                + "<harmony><root><root-step>G</root-step></root><kind>dominant-ninth</kind></harmony>"
                + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>eighth</type></note>"
                + "<harmony><root><root-step>D</root-step></root><bass><bass-step>F</bass-step><bass-alter>1</bass-alter></bass><kind>suspended-fourth</kind></harmony>"
                + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>480</duration><voice>1</voice><type>eighth</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);

        assertEquals(true, abc.contains("\"C6\"C"), abc);
        assertEquals(true, abc.contains("\"Dm6\"D"), abc);
        assertEquals(true, abc.contains("\"G9\"E"), abc);
        assertEquals(true, abc.contains("\"Dsus4/F#\"F"), abc);
    }

    @Test
    public void musicXmlToAbcExportsRehearsalDirectionAsDecorationAndRoundtrips() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>480</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<direction><direction-type><rehearsal>A1</rehearsal></direction-type></direction>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>whole</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        String roundtripped = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());

        assertEquals(true, abc.contains("!rehearsal:A1!"), abc);
        assertEquals(true, roundtripped.contains("<rehearsal>A1</rehearsal>"), roundtripped);
    }

    @Test
    public void importsAbcOverlayBodyAsSeparateVoicesWithMetadata() {
        String abc = "X:1\nT:Overlay ABC\nM:4/4\nL:1/4\nK:C\nV:Lead name=\"Lead\" clef=treble\n"
                + "C E G c & D F A d |]\n";

        AbcIo.AbcParsedResult parsed = AbcIo.parseForMusicXml(abc, new AbcIo.AbcImportOptions());
        assertEquals(2, parsed.getParts().size());
        assertEquals("Lead", parsed.getParts().get(0).getPartName());
        assertEquals("Lead overlay 2", parsed.getParts().get(1).getPartName());
        assertEquals("Lead", parsed.getParts().get(0).getVoiceId());
        assertEquals("Lead_ov2", parsed.getParts().get(1).getVoiceId());
        assertEquals("treble", parsed.getParts().get(1).getClef());
        assertEquals(4, parsed.getParts().get(0).getMeasures().get(0).size());
        assertEquals(4, parsed.getParts().get(1).getMeasures().get(0).size());
        assertEquals("C", parsed.getParts().get(0).getMeasures().get(0).get(0).getStep());
        assertEquals("D", parsed.getParts().get(1).getMeasures().get(0).get(0).getStep());

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<score-part id=\"P2\">"));
        assertEquals(true, xml.contains("<part-name>Lead overlay 2</part-name>"));
        assertEquals(true, xml.contains("<part id=\"P2\">"));
    }

    @Test
    public void abcImportMapsOverlaySyntaxIntoSyntheticOverlayVoices() throws Exception {
        String abc = "X:1\nT:Overlay mapped\nM:4/4\nL:1/8\nK:C\n"
                + "C D & E F |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> scoreParts = directChildren(directChild(root, "part-list"), "score-part");
        List<Element> parts = directChildren(root, "part");

        assertEquals(2, scoreParts.size());
        assertEquals("Voice 1", directChildText(scoreParts.get(0), "part-name"));
        assertEquals("Voice 1 overlay 2", directChildText(scoreParts.get(1), "part-name"));
        assertEquals(Arrays.asList("C", "D"), pitchSteps(parts.get(0)));
        assertEquals(Arrays.asList("E", "F"), pitchSteps(parts.get(1)));
        assertEquals(false, AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()).contains("mks:diag:count"));
    }

    @Test
    public void abcImportKeepsLaterMeasureOverlayNotesAfterPlainMeasures() throws Exception {
        String abc = "X:1\nT:Overlay later measure\nM:4/4\nL:1/8\nK:C\n"
                + "C D E F | G A & B c |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> parts = directChildren(root, "part");
        List<Element> part1Measures = directChildren(parts.get(0), "measure");

        assertEquals(Arrays.asList("C", "D", "E", "F"), notePitchSteps(directChildren(part1Measures.get(0), "note")));
        assertEquals(Arrays.asList("G", "A"), notePitchSteps(directChildren(part1Measures.get(1), "note")));
        assertEquals(Arrays.asList("B", "C"), pitchSteps(parts.get(1)));
        assertEquals(false, AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()).contains("mks:diag:count"));
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
    public void abcImportRestoresStandardRepeatBarlines() throws Exception {
        String abc = "X:1\nT:Repeat bars\nM:4/4\nL:1/8\nK:C\n"
                + "|: C D E F | G A B c :|\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> measures = directChildren(directChild(root, "part"), "measure");

        assertEquals("forward", directChild(measureBarline(measures.get(0), "left"), "repeat")
                .getAttribute("direction"));
        assertEquals("backward", directChild(measureBarline(measures.get(1), "right"), "repeat")
                .getAttribute("direction"));
    }

    @Test
    public void abcImportRestoresAlternateEndingsFromStandardMarkers() throws Exception {
        String abc = "X:1\nT:Alternate endings\nM:4/4\nL:1/8\nK:C\n"
                + "|: C D E F |\n"
                + "[1 G A B c :|]\n"
                + "[2 c B A G ||\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> measures = directChildren(directChild(root, "part"), "measure");

        assertEquals("forward", directChild(measureBarline(measures.get(0), "left"), "repeat")
                .getAttribute("direction"));
        assertEquals("1", directChild(measureBarline(measures.get(1), "left"), "ending").getAttribute("number"));
        assertEquals("start", directChild(measureBarline(measures.get(1), "left"), "ending").getAttribute("type"));
        assertEquals("backward", directChild(measureBarline(measures.get(1), "right"), "repeat")
                .getAttribute("direction"));
        assertEquals("1", directChild(measureBarline(measures.get(1), "right"), "ending").getAttribute("number"));
        assertEquals("stop", directChild(measureBarline(measures.get(1), "right"), "ending").getAttribute("type"));
        assertEquals("2", directChild(measureBarline(measures.get(2), "left"), "ending").getAttribute("number"));
        assertEquals("2", directChild(measureBarline(measures.get(2), "right"), "ending").getAttribute("number"));
        assertEquals(true, pitchSteps(directChild(root, "part")).size() >= 12);
    }

    @Test
    public void abcImportRestoresAlternateEndingsFromBarlineStyleMarkers() throws Exception {
        String abc = "X:1\nT:Alternate endings barline style\nM:4/4\nL:1/8\nK:C\n"
                + "|: C D E F |1 G A B c :|2 c B A G ||\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> measures = directChildren(directChild(root, "part"), "measure");

        assertEquals("forward", directChild(measureBarline(measures.get(0), "left"), "repeat")
                .getAttribute("direction"));
        assertEquals("1", directChild(measureBarline(measures.get(1), "left"), "ending").getAttribute("number"));
        assertEquals("backward", directChild(measureBarline(measures.get(1), "right"), "repeat")
                .getAttribute("direction"));
        assertEquals("1", directChild(measureBarline(measures.get(1), "right"), "ending").getAttribute("number"));
        assertEquals("2", directChild(measureBarline(measures.get(2), "left"), "ending").getAttribute("number"));
        assertEquals("2", directChild(measureBarline(measures.get(2), "right"), "ending").getAttribute("number"));
        assertEquals(true, pitchSteps(directChild(root, "part")).size() >= 12);
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

    @Test
    public void abcImportParsesSlurNotation() throws Exception {
        String abc = "X:1\nT:Slur test\nM:4/4\nL:1/8\nK:C\nV:1\n"
                + "(c2 d2) e2 f2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");

        assertNotNull(directChild(directChild(notes.get(0), "notations"), "slur"));
        assertEquals("start", directChild(directChild(notes.get(0), "notations"), "slur").getAttribute("type"));
        assertNotNull(directChild(directChild(notes.get(1), "notations"), "slur"));
        assertEquals("stop", directChild(directChild(notes.get(1), "notations"), "slur").getAttribute("type"));
    }

    @Test
    public void musicXmlToAbcExportsSlurNotationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><notations><slur type=\"start\"/></notations></note>"
                + "<note><pitch><step>D</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><notations><slur type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("(C4"), abc);
        assertEquals(true, abc.contains("D4)"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<slur type=\"start\"/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<slur type=\"stop\"/>"), roundtripXml);
    }

    @Test
    public void musicXmlToAbcExportsTieNotationAndRoundtrips() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><tie type=\"start\"/>"
                + "<notations><tied type=\"start\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><tie type=\"stop\"/>"
                + "<notations><tied type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("C4- C4"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, roundtripXml.contains("<tie type=\"start\"/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<tie type=\"stop\"/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<tied type=\"start\"/>"), roundtripXml);
        assertEquals(true, roundtripXml.contains("<tied type=\"stop\"/>"), roundtripXml);
    }

    @Test
    public void warnsWhenAbcSlurStopHasNoPrecedingNonRestNote() {
        String abc = "X:1\nT:Slur Stop After Rest ABC\nM:4/4\nL:1/8\nK:C\n(c2 z2) e2 f2|]\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("mks:diag:count"));
        assertEquals(true, xml.contains("slur stop()) has no preceding note; skipped."));
    }

    @Test
    public void abcImportReportsSlurStopWarningDiagnosticFields() throws Exception {
        String abc = "X:1\nT:Slur stop after rest\nM:4/4\nL:1/8\nK:C\nV:1\n"
                + "(c2 z2) e2 f2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        NodeList nodes = root.getElementsByTagName("miscellaneous-field");
        List<Element> fields = new ArrayList<Element>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element field = (Element) node;
                if (field.getAttribute("name").startsWith("mks:diag:")) {
                    fields.add(field);
                }
            }
        }

        assertEquals("mks:diag:count", fields.get(0).getAttribute("name"));
        assertEquals("1", fields.get(0).getTextContent());
        assertEquals("mks:diag:0001", fields.get(1).getAttribute("name"));
        assertEquals(true, fields.get(1).getTextContent().contains(
                "slur stop()) has no preceding note; skipped."));
    }

    @Test
    public void appliesAbcChordTiesToAllChordNotes() {
        String abc = "X:1\nT:Chord Tie ABC\nM:4/4\nL:1/4\nK:C\n[CE]-[CE] z2|]\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(2, countOccurrences(xml, "<tie type=\"start\"/>"));
        assertEquals(2, countOccurrences(xml, "<tie type=\"stop\"/>"));
        assertEquals(2, countOccurrences(xml, "<tied type=\"start\"/>"));
        assertEquals(2, countOccurrences(xml, "<tied type=\"stop\"/>"));
    }

    @Test
    public void abcImportAppliesChordTiesToEachChordNote() throws Exception {
        String abc = "X:1\nT:Chord tie test\nM:4/4\nL:1/4\nK:C\n"
                + "[CE]-[CE] z2 |\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> notes = directChildren(directChildren(directChild(root, "part"), "measure").get(0), "note");
        List<Element> firstChord = notes.subList(0, 2);
        List<Element> secondChord = notes.subList(2, 4);

        assertEquals(2, firstChord.size());
        assertEquals(2, secondChord.size());
        for (Element note : firstChord) {
            assertEquals("start", directChild(note, "tie").getAttribute("type"));
            assertEquals("start", directChild(directChild(note, "notations"), "tied").getAttribute("type"));
        }
        for (Element note : secondChord) {
            assertEquals("stop", directChild(note, "tie").getAttribute("type"));
            assertEquals("stop", directChild(directChild(note, "notations"), "tied").getAttribute("type"));
        }
    }

    @Test
    public void musicXmlToAbcRoundtripsChordTiesAcrossAllChordNotes() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><tie type=\"start\"/>"
                + "<notations><tied type=\"start\"/></notations></note>"
                + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><tie type=\"start\"/>"
                + "<notations><tied type=\"start\"/></notations></note>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><tie type=\"stop\"/>"
                + "<notations><tied type=\"stop\"/></notations></note>"
                + "<note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type><tie type=\"stop\"/>"
                + "<notations><tied type=\"stop\"/></notations></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("[CE]4- [CE]4"), abc);

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(2, countOccurrences(roundtripXml, "<tie type=\"start\"/>"), roundtripXml);
        assertEquals(2, countOccurrences(roundtripXml, "<tie type=\"stop\"/>"), roundtripXml);
        assertEquals(2, countOccurrences(roundtripXml, "<tied type=\"start\"/>"), roundtripXml);
        assertEquals(2, countOccurrences(roundtripXml, "<tied type=\"stop\"/>"), roundtripXml);
    }

    @Test
    public void usesMeterSizedEmptyRestsForMissingAbcVoiceMeasures() {
        String abc = "X:1\nT:Missing Measure ABC\nM:2/4\nL:1/8\nK:C\nV:1\nC D | E F |\nV:2\nG A |]\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<part id=\"P2\">"));
        assertEquals(true, xml.contains("<measure number=\"2\">"));
        assertEquals(true, xml.contains("<duration>1920</duration>"));
    }

    @Test
    public void doesNotTreatAbcGraceNotesAsMeasureOccupancy() {
        String abc = "X:1\nT:Grace Occupancy ABC\nM:2/4\nL:1/8\nK:C\n{a}c {b}d {c}e {d}f|]\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(false, xml.contains("code=OVERFULL_REFLOWED"));
        assertEquals(4, countOccurrences(xml, "<grace"));
    }

    @Test
    public void exportsMusicXmlDiagnosticMiscFieldsAsAbcMksDiagLines() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Lead</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef><miscellaneous>"
                + "<miscellaneous-field name=\"mks:diag:count\">1</miscellaneous-field>"
                + "<miscellaneous-field name=\"mks:diag:0001\">level=warn;code=OVERFULL_CLAMPED;fmt=mei;measure=1</miscellaneous-field>"
                + "</miscellaneous></attributes>"
                + "<note><rest/><duration>3840</duration><voice>1</voice><type>whole</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("%@mks diag"));
        assertEquals(true, abc.contains("name=mks:diag:count"));
        assertEquals(true, abc.contains("name=mks:diag:0001"));
        assertEquals(true, abc.contains("enc=uri-v1"));
    }

    @Test
    public void doesNotSplitSeparateAbcLaneForGraceNotesMissingVoice() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><grace/><pitch><step>D</step><octave>5</octave></pitch><type>eighth</type></note>"
                + "<note><pitch><step>C</step><octave>5</octave></pitch><duration>1920</duration>"
                + "<voice>1</voice><type>half</type></note>"
                + "<note><rest/><duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("{d}"));
        assertEquals(false, abc.contains("V:P1_v2"));

        String roundtripXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        Element root = parseElement(roundtripXml);
        assertEquals(1, directChildren(root, "part").size());
        assertEquals(true, roundtripXml.contains("<grace"));
    }

    @Test
    public void exportsAbcRepeatTimesMetadataOnlyWhenStandardRepeatIsInsufficient() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<barline location=\"left\"><repeat direction=\"forward\"/></barline>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>3840</duration>"
                + "<voice>1</voice><type>whole</type></note></measure>"
                + "<measure number=\"2\"><note><pitch><step>D</step><octave>4</octave></pitch>"
                + "<duration>3840</duration><voice>1</voice><type>whole</type></note>"
                + "<barline location=\"right\"><repeat direction=\"backward\" times=\"3\"/></barline>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("|:"));
        assertEquals(true, abc.contains(":|"));
        assertEquals(true, abc.contains("%@mks measure voice=P1 measure=2 number=2 implicit=0 times=3"));
    }

    @Test
    public void restoresAbcRepeatTimesFromMeasureMetadataWhenStandardRepeatIsInsufficient() {
        String abc = "X:1\nT:Repeat times restore\nM:4/4\nL:1/4\nK:C\nV:P1\n"
                + "|: C D E F | G A B c :|\n"
                + "%@mks measure voice=P1 measure=1 number=1 implicit=0\n"
                + "%@mks measure voice=P1 measure=2 number=2 implicit=0 times=3\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<repeat direction=\"backward\" winged=\"none\" times=\"3\"/>"));
    }

    @Test
    public void exportsAbcDiscontinueEndingMetadataWhenStandardSurfaceIsInsufficient() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<barline location=\"left\"><ending number=\"1\" type=\"start\"/></barline>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>3840</duration>"
                + "<voice>1</voice><type>whole</type></note>"
                + "<barline location=\"right\"><ending number=\"1\" type=\"discontinue\"/></barline>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("[1"));
        assertEquals(true,
                abc.contains("%@mks measure voice=P1 measure=1 number=1 implicit=0 ending-stop=1 ending-type=discontinue"));
    }

    @Test
    public void restoresAbcDiscontinueEndingTypeFromMeasureMetadata() {
        String abc = "X:1\nT:Ending discontinue restore\nM:4/4\nL:1/4\nK:C\nV:P1\n"
                + "[1 C D E F |\n"
                + "%@mks measure voice=P1 measure=1 number=1 implicit=0 ending-stop=1 ending-type=discontinue\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<ending number=\"1\" type=\"start\"/>"), xml);
        assertEquals(true, xml.contains("<ending number=\"1\" type=\"discontinue\"/>"), xml);
    }

    @Test
    public void importsAbcCommonTupletShorthandAsMusicXmlTuplet() {
        String abc = "X:1\nT:Tuplet shorthand\nM:3/4\nL:1/8\nK:C\nV:1\n(3 DEF z2 |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<actual-notes>3</actual-notes>"));
        assertEquals(true, xml.contains("<normal-notes>2</normal-notes>"));
        assertEquals(true, xml.contains("<tuplet type=\"start\"/>"));
        assertEquals(true, xml.contains("<tuplet type=\"stop\"/>"));
    }

    @Test
    public void importsAbcExplicitTupletRatioAsMusicXmlTuplet() {
        String abc = "X:1\nT:Tuplet explicit ratio\nM:4/4\nL:1/8\nK:C\nV:1\n(5:4:5 C D E F G z2 |\n";

        String xml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
        assertEquals(true, xml.contains("<actual-notes>5</actual-notes>"));
        assertEquals(true, xml.contains("<normal-notes>4</normal-notes>"));
        assertEquals(true, xml.contains("<tuplet type=\"start\"/>"));
        assertEquals(true, xml.contains("<tuplet type=\"stop\"/>"));
    }

    @Test
    public void roundtripsPerPartKeySignaturesViaStandardAbcKeyFields() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Top</part-name></score-part>"
                + "<score-part id=\"P2\"><part-name>Bottom</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>3</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>A</step><octave>4</octave></pitch><duration>2880</duration>"
                + "<voice>1</voice><type>half</type></note></measure>"
                + "<measure number=\"2\"><attributes><key><fifths>0</fifths></key></attributes>"
                + "<note><pitch><step>C</step><octave>5</octave></pitch><duration>2880</duration>"
                + "<voice>1</voice><type>half</type></note></measure></part>"
                + "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>F</sign><line>4</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>3</octave></pitch><duration>2880</duration>"
                + "<voice>1</voice><type>half</type></note></measure>"
                + "<measure number=\"2\"><attributes><key><fifths>3</fifths></key></attributes>"
                + "<note><pitch><step>A</step><octave>2</octave></pitch><duration>2880</duration>"
                + "<voice>1</voice><type>half</type></note></measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(false, abc.contains("%@mks key"), abc);
        assertEquals(true, abc.contains("K:A"), abc);
        assertEquals(true, abc.contains("V:P1"), abc);
        assertEquals(true, abc.contains("V:P2"), abc);
        assertEquals(true, abc.contains("[K:C]"), abc);
        assertEquals(true, abc.contains("[K:A]"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> parts = directChildren(root, "part");
        assertEquals(2, parts.size());
        List<Element> part1Measures = directChildren(parts.get(0), "measure");
        List<Element> part2Measures = directChildren(parts.get(1), "measure");
        assertEquals("3", directChildText(directChild(directChild(part1Measures.get(0), "attributes"), "key"), "fifths"));
        assertEquals("0", directChildText(directChild(directChild(part1Measures.get(1), "attributes"), "key"), "fifths"));
        assertEquals("0", directChildText(directChild(directChild(part2Measures.get(0), "attributes"), "key"), "fifths"));
        assertEquals("3", directChildText(directChild(directChild(part2Measures.get(1), "attributes"), "key"), "fifths"));
    }

    @Test
    public void keepsFirstAbcKeyHintWhenDuplicateHintsExistForSameVoiceAndMeasure() throws Exception {
        String abc = "X:1\nT:Duplicate key hint\nM:3/4\nL:1/8\nK:C\n"
                + "V:P1 name=\"clarinet in A\" clef=treble\n"
                + "V:P2 name=\"violino I\" clef=treble\n"
                + "V:P1\nc2 d2 e2 |\n"
                + "V:P2\nz6 |\n"
                + "%@mks key voice=P1 measure=1 fifths=0\n"
                + "%@mks key voice=P2 measure=1 fifths=3\n"
                + "%@mks key voice=P2 measure=1 fifths=0\n";

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> parts = directChildren(root, "part");
        assertEquals(2, parts.size());
        Element part1Measure1 = directChildren(parts.get(0), "measure").get(0);
        Element part2Measure1 = directChildren(parts.get(1), "measure").get(0);
        assertEquals("0", directChildText(directChild(directChild(part1Measure1, "attributes"), "key"), "fifths"));
        assertEquals("3", directChildText(directChild(directChild(part2Measure1, "attributes"), "key"), "fifths"));
    }

    @Test
    public void exportsSharedHeaderKeyWhenAllLanesStartWithSameKey() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Upper</part-name></score-part>"
                + "<score-part id=\"P2\"><part-name>Lower</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>1</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>3840</duration>"
                + "<voice>1</voice><type>whole</type></note></measure></part>"
                + "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>1</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>F</sign><line>4</line></clef></attributes>"
                + "<note><pitch><step>G</step><octave>2</octave></pitch><duration>3840</duration>"
                + "<voice>1</voice><type>whole</type></note></measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("K:G"), abc);
        assertEquals(false, abc.contains("%@mks key"), abc);
        assertEquals(false, abc.contains("[K:G]"), abc);
    }

    @Test
    public void exportsNaturalAgainstLaneKeySignature() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>3</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>2880</duration>"
                + "<voice>1</voice><type>half</type></note></measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("=G"), abc);
    }

    @Test
    public void usesPerPartInitialKeyForAccidentalEmission() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"3.1\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Part 1</part-name></score-part>"
                + "<score-part id=\"P2\"><part-name>Part 2</part-name></score-part>"
                + "<score-part id=\"P3\"><part-name>Part 3</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note></measure></part>"
                + "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>3</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><rest/><duration>2880</duration><voice>1</voice><type>half</type></note></measure></part>"
                + "<part id=\"P3\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>3</fifths></key><time><beats>3</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>G</sign><line>2</line></clef></attributes>"
                + "<note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch>"
                + "<duration>1920</duration><voice>1</voice><type>half</type></note>"
                + "<note><pitch><step>G</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type><accidental>natural</accidental></note>"
                + "</measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        String[] lines = abc.split("\\R");
        int p3Index = -1;
        for (int index = 0; index < lines.length; index++) {
            if ("V:P3".equals(lines[index].trim())) {
                p3Index = index;
                break;
            }
        }
        assertEquals(true, p3Index >= 0, abc);
        StringBuilder p3Block = new StringBuilder();
        for (int index = p3Index; index < lines.length && index < p3Index + 3; index++) {
            p3Block.append(lines[index]).append('\n');
        }
        assertEquals(true, p3Block.toString().contains("=G"), abc);
    }

    @Test
    public void exportsCommonCClefHeadersAndRoundtripsThem() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\"><part-list>"
                + "<score-part id=\"P1\"><part-name>Alto staff</part-name></score-part>"
                + "<score-part id=\"P2\"><part-name>Tenor staff</part-name></score-part></part-list>"
                + "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>C</sign><line>3</line></clef></attributes>"
                + "<note><pitch><step>C</step><octave>4</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure></part>"
                + "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>960</divisions>"
                + "<key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time>"
                + "<clef><sign>C</sign><line>4</line></clef></attributes>"
                + "<note><pitch><step>D</step><octave>3</octave></pitch><duration>960</duration>"
                + "<voice>1</voice><type>quarter</type></note></measure></part></score-partwise>";

        String abc = AbcIo.musicXmlToAbc(xml);
        assertEquals(true, abc.contains("V:P1 name=\"Alto staff\" clef=alto"), abc);
        assertEquals(true, abc.contains("V:P2 name=\"Tenor staff\" clef=tenor"), abc);

        Element root = parseElement(AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions()));
        List<Element> parts = directChildren(root, "part");
        assertEquals(2, parts.size());
        Element part1Attributes = directChild(directChildren(parts.get(0), "measure").get(0), "attributes");
        Element part2Attributes = directChild(directChildren(parts.get(1), "measure").get(0), "attributes");
        assertEquals("C", directChildText(directChild(part1Attributes, "clef"), "sign"));
        assertEquals("3", directChildText(directChild(part1Attributes, "clef"), "line"));
        assertEquals("C", directChildText(directChild(part2Attributes, "clef"), "sign"));
        assertEquals("4", directChildText(directChild(part2Attributes, "clef"), "line"));
    }

    @Test
    public void roundtripsBundledAbcGoldenFixturesThroughMusicXmlToAbc() throws Exception {
        for (String fixture : Arrays.asList("base.musicxml", "with_rest.musicxml", "interleaved_voices.musicxml",
                "roundtrip_piano_tempo.musicxml", "with_backup_safe.musicxml", "with_beam.musicxml",
                "with_chord_timing.musicxml", "with_following_rest.musicxml", "with_rest_tail.musicxml",
                "full_with_half.musicxml", "inherited_time_changed.musicxml",
                "inherited_divisions_changed.musicxml", "inherited_attributes.musicxml", "mixed_voices.musicxml",
                "with_backup.musicxml", "with_unknown.musicxml", "underfull.musicxml", "overfull.musicxml",
                "roundtrip_moonlight_m13_m16_like.musicxml", "roundtrip_triplet_m1_m4_like.musicxml",
                "roundtrip_sample6_m1_m2.musicxml")) {
            String srcXml = loadFixtureText("abc-roundtrip/" + fixture);
            String abc = AbcIo.musicXmlToAbc(srcXml);
            assertEquals(true, abc.contains("V:"));
            assertEquals(true, abc.contains("K:"));

            String dstXml = AbcIo.musicXmlFromAbc(abc, new AbcIo.AbcImportOptions());
            AbcRoundtripStats srcStats = countRoundtripStats(parseElement(srcXml));
            AbcRoundtripStats dstStats = countRoundtripStats(parseElement(dstXml));
            assertEquals(srcStats.noteCount, dstStats.noteCount);
            assertEquals(srcStats.restCount, dstStats.restCount);
            assertEquals(srcStats.pitchedCount, dstStats.pitchedCount);
            assertEquals(srcStats.firstMeter, dstStats.firstMeter);
            assertEquals(srcStats.nonChordQuarterSum, dstStats.nonChordQuarterSum, 0.000001);
            if (srcStats.firstTempo != null) {
                assertEquals(srcStats.firstTempo, dstStats.firstTempo);
            }
            assertNoOverfullMeasures(parseElement(dstXml));
        }
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while (text != null && needle != null && needle.length() > 0) {
            index = text.indexOf(needle, index);
            if (index < 0) {
                break;
            }
            count++;
            index += needle.length();
        }
        return count;
    }

    private static void assertFraction(int num, int den, AbcIo.Fraction actual) {
        assertEquals(num, actual.getNum());
        assertEquals(den, actual.getDen());
    }

    private static String loadFixtureText(String name) throws Exception {
        InputStream in = AbcIoTest.class.getClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new IllegalArgumentException("Missing fixture: " + name);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    private static AbcRoundtripStats countRoundtripStats(Element root) {
        int noteCount = 0;
        int restCount = 0;
        int pitchedCount = 0;
        double nonChordQuarterSum = 0.0;
        String firstMeter = "";
        Integer firstTempo = null;
        for (Element part : directChildren(root, "part")) {
            int currentDivisions = 1;
            for (Element measure : directChildren(part, "measure")) {
                Element attributes = directChild(measure, "attributes");
                Element divisions = directChild(attributes, "divisions");
                if (divisions != null) {
                    currentDivisions = Math.max(1, parseInt(divisions.getTextContent(), currentDivisions));
                }
                if (firstMeter.length() == 0) {
                    Element time = directChild(attributes, "time");
                    String beats = directChildText(time, "beats");
                    String beatType = directChildText(time, "beat-type");
                    if (beats.length() > 0 && beatType.length() > 0) {
                        firstMeter = beats + "/" + beatType;
                    }
                }
                if (firstTempo == null) {
                    firstTempo = findFirstTempoInMeasure(measure);
                }
                for (Element note : directChildren(measure, "note")) {
                    noteCount++;
                    boolean rest = directChild(note, "rest") != null;
                    if (rest) {
                        restCount++;
                    } else {
                        pitchedCount++;
                    }
                    if (directChild(note, "chord") == null) {
                        int duration = parseInt(directChildText(note, "duration"), 0);
                        if (duration > 0) {
                            nonChordQuarterSum += ((double) duration) / currentDivisions;
                        }
                    }
                }
            }
        }
        return new AbcRoundtripStats(noteCount, restCount, pitchedCount, nonChordQuarterSum, firstMeter, firstTempo);
    }

    private static Integer findFirstTempoInMeasure(Element measure) {
        for (Element direction : directChildren(measure, "direction")) {
            for (Element sound : directChildren(direction, "sound")) {
                int tempo = parseInt(sound.getAttribute("tempo"), -1);
                if (tempo > 0) {
                    return Integer.valueOf(tempo);
                }
            }
            for (Element directionType : directChildren(direction, "direction-type")) {
                Element metronome = directChild(directionType, "metronome");
                int tempo = parseInt(directChildText(metronome, "per-minute"), -1);
                if (tempo > 0) {
                    return Integer.valueOf(tempo);
                }
            }
        }
        return null;
    }

    private static void assertNoOverfullMeasures(Element root) {
        for (Element part : directChildren(root, "part")) {
            int currentDivisions = 1;
            int beats = 4;
            int beatType = 4;
            for (Element measure : directChildren(part, "measure")) {
                Element attributes = directChild(measure, "attributes");
                currentDivisions = Math.max(1, parseInt(directChildText(attributes, "divisions"), currentDivisions));
                Element time = directChild(attributes, "time");
                beats = Math.max(1, parseInt(directChildText(time, "beats"), beats));
                beatType = Math.max(1, parseInt(directChildText(time, "beat-type"), beatType));
                int capacity = (int) Math.round(currentDivisions * beats * (4.0 / beatType));
                Map<String, Integer> occupiedByVoice = new LinkedHashMap<String, Integer>();
                for (Element note : directChildren(measure, "note")) {
                    if (directChild(note, "chord") != null || directChild(note, "grace") != null) {
                        continue;
                    }
                    String voice = directChildText(note, "voice");
                    if (voice.length() == 0) {
                        voice = "1";
                    }
                    int duration = parseInt(directChildText(note, "duration"), 0);
                    Integer occupied = occupiedByVoice.get(voice);
                    occupiedByVoice.put(voice, Integer.valueOf((occupied == null ? 0 : occupied.intValue())
                            + Math.max(0, duration)));
                }
                for (Integer occupied : occupiedByVoice.values()) {
                    assertEquals(true, occupied.intValue() <= capacity);
                }
            }
        }
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

    private static Element directChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<Element>();
        if (parent == null) {
            return result;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                result.add((Element) child);
            }
            child = child.getNextSibling();
        }
        return result;
    }

    private static String directChildText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        return child == null ? "" : child.getTextContent().trim();
    }

    private static List<String> directChildTextList(Element parent, String tagName) {
        List<String> result = new ArrayList<String>();
        for (Element child : directChildren(parent, tagName)) {
            result.add(child.getTextContent().trim());
        }
        return result;
    }

    private static List<String> pitchSteps(Element part) {
        List<String> result = new ArrayList<String>();
        for (Element measure : directChildren(part, "measure")) {
            for (Element note : directChildren(measure, "note")) {
                Element pitch = directChild(note, "pitch");
                if (pitch != null) {
                    result.add(directChildText(pitch, "step"));
                }
            }
        }
        return result;
    }

    private static List<String> notePitchSteps(List<Element> notes) {
        List<String> result = new ArrayList<String>();
        for (Element note : notes) {
            Element pitch = directChild(note, "pitch");
            if (pitch != null) {
                result.add(directChildText(pitch, "step"));
            }
        }
        return result;
    }

    private static List<String> staffNumbers(Element measure) {
        List<String> result = new ArrayList<String>();
        for (Element note : directChildren(measure, "note")) {
            result.add(directChildText(note, "staff"));
        }
        return result;
    }

    private static List<String> noteLyricTexts(List<Element> notes) {
        List<String> result = new ArrayList<String>();
        for (Element note : notes) {
            result.add(directChildText(directChild(note, "lyric"), "text"));
        }
        return result;
    }

    private static List<String> beamTexts(List<Element> notes) {
        List<String> result = new ArrayList<String>();
        for (Element note : notes) {
            result.add(directChildText(note, "beam"));
        }
        return result;
    }

    private static Element measureBarline(Element measure, String location) {
        for (Element barline : directChildren(measure, "barline")) {
            if (location.equals(barline.getAttribute("location"))) {
                return barline;
            }
        }
        return null;
    }

    private static void assertClef(Element part, String sign, String line) {
        Element clef = directChild(directChild(directChildren(part, "measure").get(0), "attributes"), "clef");
        assertEquals(sign, directChildText(clef, "sign"));
        assertEquals(line, directChildText(clef, "line"));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static final class AbcRoundtripStats {
        private final int noteCount;
        private final int restCount;
        private final int pitchedCount;
        private final double nonChordQuarterSum;
        private final String firstMeter;
        private final Integer firstTempo;

        private AbcRoundtripStats(int noteCount, int restCount, int pitchedCount, double nonChordQuarterSum,
                String firstMeter, Integer firstTempo) {
            this.noteCount = noteCount;
            this.restCount = restCount;
            this.pitchedCount = pitchedCount;
            this.nonChordQuarterSum = nonChordQuarterSum;
            this.firstMeter = firstMeter;
            this.firstTempo = firstTempo;
        }
    }
}
