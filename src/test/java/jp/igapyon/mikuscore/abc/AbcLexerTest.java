/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class AbcLexerTest {
    @Test
    public void lexesLengthTokens() {
        assertLength("2", 1, AbcLexer.lexAbcLengthToken("2C", 0));
        assertLength("3/2", 3, AbcLexer.lexAbcLengthToken("3/2C", 0));
        assertLength("/", 1, AbcLexer.lexAbcLengthToken("/C", 0));
        assertLength("/2", 2, AbcLexer.lexAbcLengthToken("/2C", 0));
        assertLength("//", 2, AbcLexer.lexAbcLengthToken("//C", 0));
        assertLength("///", 3, AbcLexer.lexAbcLengthToken("///C", 0));
        assertNull(AbcLexer.lexAbcLengthToken("C2", 0));
    }

    @Test
    public void lexesAccidentals() {
        assertAccidental("^", 1, AbcLexer.lexAbcAccidental("^C", 0));
        assertAccidental("^^", 2, AbcLexer.lexAbcAccidental("^^C", 0));
        assertAccidental("_", 1, AbcLexer.lexAbcAccidental("_C", 0));
        assertAccidental("__", 2, AbcLexer.lexAbcAccidental("__C", 0));
        assertAccidental("=", 1, AbcLexer.lexAbcAccidental("=C", 0));
        assertAccidental("^_", 2, AbcLexer.lexAbcAccidental("^_C", 0));
        assertNull(AbcLexer.lexAbcAccidental("C", 0));
    }

    @Test
    public void lexesNotes() {
        assertNote("", "C", "", "2", 2, AbcLexer.lexAbcNote("C2", 0));
        assertNote("^", "C", "'", "/2", 5, AbcLexer.lexAbcNote("^C'/2", 0));
        assertNote("__", "B", ",,", "3/2", 8, AbcLexer.lexAbcNote("__B,,3/2", 0));
        assertNote("", "z", "", "", 1, AbcLexer.lexAbcNote("z", 0));
        assertNote("", "x", "", "", 1, AbcLexer.lexAbcNote("x", 0));
        assertNull(AbcLexer.lexAbcNote("!", 0));
        assertNull(AbcLexer.lexAbcNote("^", 0));
    }

    @Test
    public void clampsNegativeStartIndex() {
        assertNote("", "C", "", "2", 2, AbcLexer.lexAbcNote("C2", -1));
    }

    private static void assertLength(String token, int nextIdx, AbcLexer.AbcLengthTokenLex actual) {
        assertEquals(token, actual.getToken());
        assertEquals(nextIdx, actual.getNextIdx());
    }

    private static void assertAccidental(String accidentalText, int nextIdx, AbcLexer.AbcAccidentalLex actual) {
        assertEquals(accidentalText, actual.getAccidentalText());
        assertEquals(nextIdx, actual.getNextIdx());
    }

    private static void assertNote(String accidentalText, String pitchChar, String octaveShift, String lengthToken,
            int nextIdx, AbcLexer.AbcNoteLex actual) {
        assertEquals(accidentalText, actual.getAccidentalText());
        assertEquals(pitchChar, actual.getPitchChar());
        assertEquals(octaveShift, actual.getOctaveShift());
        assertEquals(lengthToken, actual.getLengthToken());
        assertEquals(nextIdx, actual.getNextIdx());
    }
}
