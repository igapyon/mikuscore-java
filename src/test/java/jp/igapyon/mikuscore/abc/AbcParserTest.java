/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class AbcParserTest {
    @Test
    public void parsesNotesAndMalformedAccidentals() {
        AbcParser.AbcNoteParseResult noteResult = AbcParser.parseAbcNoteAt("^C'/2", 0);
        assertEquals("note", noteResult.getKind());
        assertEquals("^", noteResult.getNote().getAccidentalText());
        assertEquals("C", noteResult.getNote().getPitchChar());
        assertEquals("'", noteResult.getNote().getOctaveShift());
        assertEquals("/2", noteResult.getNote().getLengthToken());
        assertEquals(5, noteResult.getNote().getNextIdx());

        AbcParser.AbcNoteParseResult malformed = AbcParser.parseAbcNoteAt("^", 0);
        assertEquals("malformed-accidental", malformed.getKind());
        assertEquals("^", malformed.getAccidentalText());
        assertEquals(1, malformed.getNextIdx());

        assertNull(AbcParser.parseAbcNoteAt("!", 0));
    }

    @Test
    public void parsesChordsWithChordLength() {
        AbcParser.AbcParsedChord chord = AbcParser.parseAbcChordAt("[CEG]2", 0);
        assertEquals(3, chord.getPitchSources().size());
        assertEquals("C", chord.getPitchSources().get(0).getPitchChar());
        assertEquals("E", chord.getPitchSources().get(1).getPitchChar());
        assertEquals("G", chord.getPitchSources().get(2).getPitchChar());
        assertEquals("2", chord.getLengthToken());
        assertEquals(6, chord.getNextIdx());
    }

    @Test
    public void skipsMalformedTokensInsideChords() {
        AbcParser.AbcParsedChord chord = AbcParser.parseAbcChordAt("[C ^ E]", 0);
        assertEquals(2, chord.getPitchSources().size());
        assertEquals("C", chord.getPitchSources().get(0).getPitchChar());
        assertEquals("E", chord.getPitchSources().get(1).getPitchChar());
        assertEquals(7, chord.getNextIdx());
    }

    @Test
    public void parsesPlayableEvents() {
        AbcParser.AbcParsedPlayableEvent note = AbcParser.parseAbcPlayableEventAt("C2", 0);
        assertEquals("playable", note.getKind());
        assertEquals("note", note.getSource());
        assertEquals("2", note.getRawLengthToken());
        assertEquals(2, note.getNextIdx());
        assertEquals("C", note.getPitchSources().get(0).getPitchChar());

        AbcParser.AbcParsedPlayableEvent chord = AbcParser.parseAbcPlayableEventAt("[CEG]2", 0);
        assertEquals("playable", chord.getKind());
        assertEquals("chord", chord.getSource());
        assertEquals("2", chord.getRawLengthToken());
        assertEquals(6, chord.getNextIdx());
        assertEquals(3, chord.getPitchSources().size());

        AbcParser.AbcParsedPlayableEvent malformed = AbcParser.parseAbcPlayableEventAt("^", 0);
        assertEquals("malformed-accidental", malformed.getKind());
        assertEquals("^", malformed.getAccidentalText());
        assertEquals(1, malformed.getNextIdx());

        AbcParser.AbcParsedPlayableEvent invalid = AbcParser.parseAbcPlayableEventAt("[", 0);
        assertEquals("invalid-chord", invalid.getKind());
        assertEquals(1, invalid.getNextIdx());

        assertNull(AbcParser.parseAbcPlayableEventAt("!", 0));
    }

    @Test
    public void parsesFieldAndBarlineHelpers() {
        AbcParser.AbcParsedInlineField inlineField = AbcParser.parseAbcInlineFieldAt("[k: Cmaj ] rest", 0);
        assertEquals("K", inlineField.getFieldName());
        assertEquals("Cmaj", inlineField.getFieldValue());
        assertEquals(10, inlineField.getNextIdx());

        AbcParser.AbcParsedRepeatEndingMarker repeat = AbcParser.parseAbcRepeatEndingMarkerAt("[1,2 C", 0);
        assertEquals("1,2", repeat.getMarker());
        assertEquals(4, repeat.getNextIdx());

        AbcParser.AbcParsedRepeatEndingMarker bareRepeat = AbcParser.parseAbcBareRepeatEndingMarkerAt("2-3 z", 0);
        assertEquals("2-3", bareRepeat.getMarker());
        assertEquals(3, bareRepeat.getNextIdx());

        AbcParser.AbcParsedBarlineToken barline = AbcParser.parseAbcBarlineTokenAt(":|] next", 0);
        assertEquals(3, barline.getNextIdx());
        assertEquals(true, barline.isEndsMeasure());
        assertEquals(true, barline.isRepeatEnd());
        assertEquals(false, barline.isRepeatStart());
        assertEquals(true, barline.isEndingStop());

        AbcParser.AbcParsedBarlineToken endingStop = AbcParser.parseAbcBarlineTokenAt("]| next", 0);
        assertEquals(2, endingStop.getNextIdx());
        assertEquals(true, endingStop.isEndsMeasure());
        assertEquals(false, endingStop.isRepeatEnd());
        assertEquals(false, endingStop.isRepeatStart());
        assertEquals(true, endingStop.isEndingStop());

        AbcParser.AbcParsedBarlineToken doubleRepeat = AbcParser.parseAbcBarlineTokenAt(":: next", 0);
        assertEquals(2, doubleRepeat.getNextIdx());
        assertEquals(true, doubleRepeat.isRepeatEnd());
        assertEquals(true, doubleRepeat.isRepeatStart());

        AbcParser.AbcParsedBarlineToken orphanColon = AbcParser.parseAbcBarlineTokenAt(": orphan", 0);
        assertEquals(1, orphanColon.getNextIdx());
        assertEquals(false, orphanColon.isEndsMeasure());

        assertNull(AbcParser.parseAbcInlineFieldAt("K:C", 0));
        assertNull(AbcParser.parseAbcRepeatEndingMarkerAt("|1", 0));
        assertNull(AbcParser.parseAbcBareRepeatEndingMarkerAt("abc", 0));
        assertNull(AbcParser.parseAbcBarlineTokenAt("abc", 0));
    }

    @Test
    public void parsesStandaloneFieldsAndUnsupportedFallbackTokens() {
        AbcParser.AbcParsedStandaloneBodyField field = AbcParser.parseAbcStandaloneBodyFieldAt("q:120 rest", 0);
        assertEquals("Q", field.getFieldName());
        assertEquals("120", field.getFieldValue());
        assertEquals("q:120", field.getToken());
        assertEquals(5, field.getNextIdx());

        AbcParser.AbcParsedUnsupportedBodyToken token = AbcParser.parseAbcUnsupportedBodyTokenAt("restLike next", 0);
        assertEquals("restLike", token.getToken());
        assertEquals(8, token.getNextIdx());

        AbcParser.AbcParsedUnsupportedBodyToken number = AbcParser.parseAbcUnsupportedBodyNumberAt("123 abc", 0);
        assertEquals("123", number.getToken());
        assertEquals(3, number.getNextIdx());
    }

    @Test
    public void parsesSpanDecorationAndBodyTokenAtoms() {
        AbcParser.AbcParsedDelimitedSpan quotedSpan = AbcParser.parseAbcDelimitedSpanAt("\"text\" tail", 0, '"');
        assertEquals("\"", quotedSpan.getDelimiter());
        assertEquals("\"text\"", quotedSpan.getText());
        assertEquals(6, quotedSpan.getNextIdx());

        AbcParser.AbcParsedDelimitedSpan unterminatedSpan = AbcParser.parseAbcDelimitedSpanAt("+sym", 0, '+');
        assertEquals("+sym", unterminatedSpan.getText());
        assertEquals(4, unterminatedSpan.getNextIdx());

        AbcParser.AbcParsedQuotedString quoted = AbcParser.parseAbcQuotedStringAt("\"^Cmaj7\" tail", 0);
        assertEquals("^Cmaj7", quoted.getRawText());
        assertEquals("Cmaj7", quoted.getNormalizedText());
        assertEquals(8, quoted.getNextIdx());
        assertEquals(true, quoted.isTerminated());

        AbcParser.AbcParsedQuotedString unterminated = AbcParser.parseAbcQuotedStringAt("\"unterminated", 0);
        assertEquals("unterminated", unterminated.getRawText());
        assertEquals(false, unterminated.isTerminated());

        AbcParser.AbcParsedDecoration decoration = AbcParser.parseAbcDecorationAt("!Trill!C", 0);
        assertEquals("Trill", decoration.getRawDecoration());
        assertEquals("trill", decoration.getDecoration());
        assertEquals("!", decoration.getDelimiter());
        assertEquals(7, decoration.getNextIdx());
        assertEquals(true, decoration.isTerminated());

        AbcParser.AbcParsedDecoration unterminatedDecoration = AbcParser.parseAbcDecorationAt("+unterminated", 0);
        assertEquals("unterminated", unterminatedDecoration.getRawDecoration());
        assertEquals("unterminated", unterminatedDecoration.getDecoration());
        assertEquals("+", unterminatedDecoration.getDelimiter());
        assertEquals(13, unterminatedDecoration.getNextIdx());
        assertEquals(false, unterminatedDecoration.isTerminated());

        AbcParser.AbcParsedBrokenRhythm broken = AbcParser.parseAbcBrokenRhythmAt("> next", 0);
        assertEquals(">", broken.getSymbol());
        assertEquals(3, broken.getLeftScale().getNum());
        assertEquals(2, broken.getLeftScale().getDen());
        assertEquals(1, broken.getRightScale().getNum());
        assertEquals(2, broken.getRightScale().getDen());
        assertEquals(1, broken.getNextIdx());

        AbcParser.AbcParsedBrokenRhythm leftBroken = AbcParser.parseAbcBrokenRhythmAt("< next", 0);
        assertEquals("<", leftBroken.getSymbol());
        assertEquals(1, leftBroken.getLeftScale().getNum());
        assertEquals(2, leftBroken.getLeftScale().getDen());
        assertEquals(3, leftBroken.getRightScale().getNum());
        assertEquals(2, leftBroken.getRightScale().getDen());
        assertEquals(1, leftBroken.getNextIdx());

        AbcParser.AbcParsedSingleCharShorthand shorthand = AbcParser.parseAbcSingleCharShorthandAt("~", 0);
        assertEquals("arpeggiate", shorthand.getKind());
        assertEquals(1, shorthand.getNextIdx());
        assertEquals("inverted-mordent", AbcParser.parseAbcSingleCharShorthandAt("P", 0).getKind());
        assertEquals("staccato", AbcParser.parseAbcSingleCharShorthandAt(".", 0).getKind());
        assertNull(AbcParser.parseAbcSingleCharShorthandAt("x", 0));

        assertEquals(1, AbcParser.parseAbcTieAt("-", 0).getNextIdx());
        assertEquals(1, AbcParser.parseAbcSlurStopAt(")", 0).getNextIdx());
        assertNull(AbcParser.parseAbcTieAt("x", 0));
        assertNull(AbcParser.parseAbcSlurStopAt("x", 0));
    }

    @Test
    public void parsesTuplets() {
        AbcParser.AbcParsedTuplet simple = AbcParser.parseAbcTupletAt("(3ABC", 0);
        assertEquals(3, simple.getActual());
        assertEquals(2, simple.getNormal());
        assertEquals(3, simple.getCount());
        assertEquals(2, simple.getNextIdx());
        assertEquals("(3", simple.getRaw());

        AbcParser.AbcParsedTuplet explicit = AbcParser.parseAbcTupletAt("(5:4:6ABC", 0);
        assertEquals(5, explicit.getActual());
        assertEquals(4, explicit.getNormal());
        assertEquals(6, explicit.getCount());
        assertEquals(6, explicit.getNextIdx());

        assertNull(AbcParser.parseAbcTupletAt("C", 0));
    }

    @Test
    public void parsesStructuralDispatcherTokens() {
        AbcParser.AbcParsedParenToken tupletParen = AbcParser.parseAbcParenTokenAt("(3ABC", 0);
        assertEquals("tuplet", tupletParen.getKind());
        assertEquals(3, tupletParen.getTuplet().getActual());
        assertEquals(2, tupletParen.getTuplet().getNormal());
        assertEquals(3, tupletParen.getTuplet().getCount());

        AbcParser.AbcParsedParenToken slurStart = AbcParser.parseAbcParenTokenAt("(C", 0);
        assertEquals("slur-start", slurStart.getKind());
        assertEquals(1, slurStart.getNextIdx());
        assertNull(AbcParser.parseAbcParenTokenAt("C", 0));

        AbcParser.AbcParsedBracketToken inline = AbcParser.parseAbcBracketTokenAt("[K:C] C", 0);
        assertEquals("inline-field", inline.getKind());
        assertEquals("K", inline.getInlineField().getFieldName());
        assertEquals("C", inline.getInlineField().getFieldValue());

        AbcParser.AbcParsedBracketToken repeat = AbcParser.parseAbcBracketTokenAt("[1,2 C", 0);
        assertEquals("repeat-ending", repeat.getKind());
        assertEquals("1,2", repeat.getRepeatEndingMarker().getMarker());

        AbcParser.AbcParsedBracketToken chordStart = AbcParser.parseAbcBracketTokenAt("[CEG]2", 0);
        assertEquals("chord-start", chordStart.getKind());
        assertEquals(1, chordStart.getNextIdx());
        assertNull(AbcParser.parseAbcBracketTokenAt("C", 0));
    }

    @Test
    public void dispatchesBodyTokens() {
        AbcParser.AbcParsedBodyToken broken = AbcParser.parseAbcBodyTokenAt(">A", 0);
        assertEquals("broken-rhythm", broken.getKind());
        assertEquals(">", broken.getBrokenRhythm().getSymbol());

        AbcParser.AbcParsedBodyToken quoted = AbcParser.parseAbcBodyTokenAt("\"txt\"", 0);
        assertEquals("quoted-string", quoted.getKind());
        assertEquals("txt", quoted.getQuotedString().getRawText());

        AbcParser.AbcParsedBodyToken decoration = AbcParser.parseAbcBodyTokenAt("!trill!", 0);
        assertEquals("decoration", decoration.getKind());
        assertEquals("trill", decoration.getDecoration().getDecoration());

        AbcParser.AbcParsedBodyToken bracket = AbcParser.parseAbcBodyTokenAt("[K:C]", 0);
        assertEquals("bracket", bracket.getKind());
        assertEquals("inline-field", bracket.getBracketToken().getKind());
        assertEquals("K", bracket.getBracketToken().getInlineField().getFieldName());

        AbcParser.AbcParsedBodyToken slurStop = AbcParser.parseAbcBodyTokenAt(")", 0);
        assertEquals("slur-stop", slurStop.getKind());
        assertEquals(1, slurStop.getSlurStop().getNextIdx());

        AbcParser.AbcParsedBodyToken tie = AbcParser.parseAbcBodyTokenAt("-", 0);
        assertEquals("tie", tie.getKind());
        assertEquals(1, tie.getTie().getNextIdx());

        assertNull(AbcParser.parseAbcBodyTokenAt("C", 0));
    }

    @Test
    public void dispatchesBodyEntries() {
        AbcParser.AbcParsedBodyEntry barline = AbcParser.parseAbcBodyEntryAt("|: C", 0);
        assertEquals("barline", barline.getKind());
        assertEquals(2, barline.getBarlineToken().getNextIdx());
        assertEquals(true, barline.getBarlineToken().isRepeatStart());

        AbcParser.AbcParsedBodyEntry field = AbcParser.parseAbcBodyEntryAt("Q:120 C", 0);
        assertEquals("standalone-body-field", field.getKind());
        assertEquals("Q", field.getStandaloneBodyField().getFieldName());
        assertEquals("120", field.getStandaloneBodyField().getFieldValue());

        AbcParser.AbcParsedBodyEntry unsupportedToken = AbcParser.parseAbcBodyEntryAt("restLike", 0);
        assertEquals("unsupported-body-token", unsupportedToken.getKind());
        assertEquals("restLike", unsupportedToken.getUnsupportedBodyToken().getToken());

        AbcParser.AbcParsedBodyEntry unsupportedNumber = AbcParser.parseAbcBodyEntryAt("123abc", 0);
        assertEquals("unsupported-body-number", unsupportedNumber.getKind());
        assertEquals("123", unsupportedNumber.getUnsupportedBodyNumber().getToken());

        AbcParser.AbcParsedBodyEntry bodyToken = AbcParser.parseAbcBodyEntryAt(">A", 0);
        assertEquals("body-token", bodyToken.getKind());
        assertEquals("broken-rhythm", bodyToken.getBodyToken().getKind());

        AbcParser.AbcParsedBodyEntry playable = AbcParser.parseAbcBodyEntryAt("C2", 0);
        assertEquals("playable-event", playable.getKind());
        assertEquals("note", playable.getPlayableEvent().getSource());
        assertEquals("2", playable.getPlayableEvent().getRawLengthToken());
    }

    @Test
    public void parsesGraceGroups() {
        List<String> warnings = new ArrayList<String>();
        AbcParser.AbcParsedGraceGroup grace = AbcParser.parseAbcGraceGroupAt("{/c^d2}E", 0, 7, warnings);

        assertEquals(2, grace.getNotes().size());
        assertEquals("c", grace.getNotes().get(0).getPitchChar());
        assertEquals(true, grace.getNotes().get(0).isGraceSlash());
        assertEquals("^", grace.getNotes().get(1).getAccidentalText());
        assertEquals("d", grace.getNotes().get(1).getPitchChar());
        assertEquals("2", grace.getNotes().get(1).getLengthToken());
        assertEquals(false, grace.getNotes().get(1).isGraceSlash());
        assertEquals(7, grace.getNextIdx());
        assertEquals(0, warnings.size());
    }

    @Test
    public void reportsMalformedGraceAccidentals() {
        List<String> warnings = new ArrayList<String>();
        AbcParser.AbcParsedGraceGroup grace = AbcParser.parseAbcGraceGroupAt("{^ c}", 0, 9, warnings);

        assertEquals(1, grace.getNotes().size());
        assertEquals("c", grace.getNotes().get(0).getPitchChar());
        assertEquals(1, warnings.size());
        assertEquals("line 9: Skipped malformed grace accidental token: ^", warnings.get(0));
        assertNull(AbcParser.parseAbcGraceGroupAt("C", 0, 9, warnings));
        assertNull(AbcParser.parseAbcGraceGroupAt("{C", 0, 9, warnings));
    }
}
