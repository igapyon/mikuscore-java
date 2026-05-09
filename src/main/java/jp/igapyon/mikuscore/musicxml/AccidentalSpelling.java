package jp.igapyon.mikuscore.musicxml;

import java.util.Map;

public final class AccidentalSpelling {
    private AccidentalSpelling() {
    }

    public static SpelledPitch midiToPitch(int midiNumber, Integer keyFifths, String preferAccidental) {
        int n = Math.max(0, Math.min(127, Math.round(midiNumber)));
        int octave = n / 12 - 1;
        int semitone = n % 12;
        String pref = preferAccidental == null ? "" : preferAccidental.trim().toLowerCase();
        boolean preferFlatByAccidental = "flat".equals(pref) || "flat-flat".equals(pref);
        boolean preferSharpByAccidental = "sharp".equals(pref) || "double-sharp".equals(pref);
        boolean preferFlatByKey = keyFifths != null && keyFifths.intValue() < 0;
        boolean preferFlat = preferFlatByAccidental || (!preferSharpByAccidental && preferFlatByKey);
        String[] sharpSteps = { "C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B" };
        int[] sharpAlters = { 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0 };
        String[] flatSteps = { "C", "D", "D", "E", "E", "F", "G", "G", "A", "A", "B", "B" };
        int[] flatAlters = { 0, -1, 0, -1, 0, 0, -1, 0, -1, 0, -1, 0 };
        return preferFlat ? new SpelledPitch(flatSteps[semitone], flatAlters[semitone], octave)
                : new SpelledPitch(sharpSteps[semitone], sharpAlters[semitone], octave);
    }

    public static int keySignatureAlterForStep(int fifths, String step) {
        String[] sharps = { "F", "C", "G", "D", "A", "E", "B" };
        String[] flats = { "B", "E", "A", "D", "G", "C", "F" };
        String s = step == null ? "" : step.trim().toUpperCase();
        if (s.length() == 0) {
            return 0;
        }
        int n = Math.max(-7, Math.min(7, Math.round(fifths)));
        if (n > 0) {
            for (int index = 0; index < n; index++) {
                if (sharps[index].equals(s)) {
                    return 1;
                }
            }
        }
        if (n < 0) {
            for (int index = 0; index < Math.abs(n); index++) {
                if (flats[index].equals(s)) {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static String accidentalTextFromAlter(int alter) {
        switch (Math.round(alter)) {
        case -2:
            return "flat-flat";
        case -1:
            return "flat";
        case 0:
            return "natural";
        case 1:
            return "sharp";
        case 2:
            return "double-sharp";
        default:
            return null;
        }
    }

    public static String resolveAccidentalTextForPitch(SpelledPitch pitch, int keyFifths,
            Map<String, Integer> previousAlterByPitchKey, String pitchKey, String preferredAccidentalText) {
        int alter = Math.round(pitch.getAlter());
        int keyAlter = keySignatureAlterForStep(keyFifths, pitch.getStep());
        Map<String, Integer> previous = previousAlterByPitchKey;
        Integer active = previous == null ? null : previous.get(pitchKey);
        int activeAlter = active == null ? keyAlter : active.intValue();
        String accidentalText = preferredAccidentalText == null ? "" : preferredAccidentalText.trim();
        if (accidentalText.length() == 0 && alter != activeAlter) {
            String resolved = accidentalTextFromAlter(alter);
            accidentalText = resolved == null ? "" : resolved;
        }
        if (previous != null) {
            previous.put(pitchKey, Integer.valueOf(alter));
        }
        return accidentalText;
    }

    public static final class SpelledPitch {
        private final String step;
        private final int alter;
        private final int octave;

        public SpelledPitch(String step, int alter, int octave) {
            this.step = step;
            this.alter = alter;
            this.octave = octave;
        }

        public String getStep() {
            return step;
        }

        public int getAlter() {
            return alter;
        }

        public int getOctave() {
            return octave;
        }
    }
}
