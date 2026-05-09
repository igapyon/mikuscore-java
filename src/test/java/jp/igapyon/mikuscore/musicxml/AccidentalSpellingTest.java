package jp.igapyon.mikuscore.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class AccidentalSpellingTest {
    @Test
    public void mapsKeySignatureSharpsAndFlatsToDefaultAlters() {
        assertEquals(1, AccidentalSpelling.keySignatureAlterForStep(4, "F"));
        assertEquals(1, AccidentalSpelling.keySignatureAlterForStep(4, "D"));
        assertEquals(0, AccidentalSpelling.keySignatureAlterForStep(4, "B"));
        assertEquals(-1, AccidentalSpelling.keySignatureAlterForStep(-3, "B"));
        assertEquals(-1, AccidentalSpelling.keySignatureAlterForStep(-3, "A"));
        assertEquals(0, AccidentalSpelling.keySignatureAlterForStep(-3, "F"));
    }

    @Test
    public void choosesPitchSpellingFromMidiWithKeyPreference() {
        AccidentalSpelling.SpelledPitch sharp = AccidentalSpelling.midiToPitch(61, Integer.valueOf(4), null);
        AccidentalSpelling.SpelledPitch flat = AccidentalSpelling.midiToPitch(61, Integer.valueOf(-4), null);

        assertEquals("C1", sharp.getStep() + sharp.getAlter());
        assertEquals("D-1", flat.getStep() + flat.getAlter());
    }

    @Test
    public void resolvesNaturalAccidentalWhenCancelingKeySignature() {
        Map<String, Integer> state = new LinkedHashMap<String, Integer>();
        AccidentalSpelling.SpelledPitch pitch = AccidentalSpelling.midiToPitch(62, Integer.valueOf(4), null);

        String text = AccidentalSpelling.resolveAccidentalTextForPitch(pitch, 4, state, "1:4:D", null);

        assertEquals("natural", text);
    }

    @Test
    public void mapsAlterValueToAccidentalText() {
        assertEquals("flat-flat", AccidentalSpelling.accidentalTextFromAlter(-2));
        assertEquals("flat", AccidentalSpelling.accidentalTextFromAlter(-1));
        assertEquals("natural", AccidentalSpelling.accidentalTextFromAlter(0));
        assertEquals("sharp", AccidentalSpelling.accidentalTextFromAlter(1));
        assertEquals("double-sharp", AccidentalSpelling.accidentalTextFromAlter(2));
    }
}
