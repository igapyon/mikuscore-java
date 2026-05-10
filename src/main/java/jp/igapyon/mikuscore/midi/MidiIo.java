/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.midi;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.core.StaffClefPolicy;
import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

public final class MidiIo {
    private static final double DEFAULT_DETACHE_DURATION_RATIO = 0.93d;

    private MidiIo() {
    }

    public static int clampTempo(double tempo) {
        if (Double.isNaN(tempo) || Double.isInfinite(tempo)) {
            return 120;
        }
        return Math.max(20, Math.min(300, (int) Math.round(tempo)));
    }

    public static int clampVelocity(double velocity) {
        if (Double.isNaN(velocity) || Double.isInfinite(velocity)) {
            return 80;
        }
        return Math.max(1, Math.min(127, (int) Math.round(velocity)));
    }

    public static int instrumentByPreset(String preset) {
        String value = preset == null ? "" : preset.trim();
        if ("electric_piano_2".equals(value)) {
            return 5;
        }
        if ("acoustic_grand_piano".equals(value)) {
            return 1;
        }
        if ("electric_piano_1".equals(value)) {
            return 4;
        }
        if ("honky_tonk_piano".equals(value)) {
            return 3;
        }
        if ("harpsichord".equals(value)) {
            return 6;
        }
        if ("clavinet".equals(value)) {
            return 7;
        }
        if ("drawbar_organ".equals(value)) {
            return 16;
        }
        if ("acoustic_guitar_nylon".equals(value)) {
            return 24;
        }
        if ("acoustic_bass".equals(value)) {
            return 32;
        }
        if ("violin".equals(value)) {
            return 40;
        }
        if ("string_ensemble_1".equals(value)) {
            return 48;
        }
        if ("synth_brass_1".equals(value)) {
            return 62;
        }
        return 5;
    }

    public static LeadingPickupTimeSignatureNormalization normalizeLeadingPickupTimeSignatureEvents(
            List<MidiTickTimeSignatureEvent> events, int ticksPerQuarter) {
        if (events == null || events.size() < 2) {
            return new LeadingPickupTimeSignatureNormalization(events, false, 0);
        }
        List<MidiTickTimeSignatureEvent> sorted = new ArrayList<MidiTickTimeSignatureEvent>();
        for (MidiTickTimeSignatureEvent event : events) {
            if (event != null) {
                sorted.add(new MidiTickTimeSignatureEvent(event.getTick(), event.getBeats(), event.getBeatType()));
            }
        }
        Collections.sort(sorted, new Comparator<MidiTickTimeSignatureEvent>() {
            @Override
            public int compare(MidiTickTimeSignatureEvent left, MidiTickTimeSignatureEvent right) {
                return Integer.valueOf(left.getTick()).compareTo(Integer.valueOf(right.getTick()));
            }
        });
        if (sorted.size() < 2) {
            return new LeadingPickupTimeSignatureNormalization(sorted, false, 0);
        }
        MidiTickTimeSignatureEvent first = sorted.get(0);
        MidiTickTimeSignatureEvent second = sorted.get(1);
        int firstMeasureTicks = Math.max(1,
                Math.round((Math.max(1, ticksPerQuarter) * 4.0f * first.getBeats()) / first.getBeatType()));
        boolean isPickupPrelude = first.getTick() == 0 && first.getBeats() == 1
                && second.getTick() == firstMeasureTicks && second.getBeats() > 1
                && second.getBeatType() == first.getBeatType();
        if (!isPickupPrelude) {
            return new LeadingPickupTimeSignatureNormalization(sorted, false, 0);
        }
        List<MidiTickTimeSignatureEvent> normalized = new ArrayList<MidiTickTimeSignatureEvent>();
        normalized.add(new MidiTickTimeSignatureEvent(0, second.getBeats(), second.getBeatType()));
        normalized.addAll(sorted.subList(2, sorted.size()));
        return new LeadingPickupTimeSignatureNormalization(normalized, true, firstMeasureTicks);
    }

    public static List<MidiTimeSignatureEvent> buildMuseScoreStylePickupTimeSignaturePrelude(
            List<MidiTimeSignatureEvent> events, int ticksPerQuarter, int pickupTicks) {
        int normalizedPickupTicks = Math.max(0, pickupTicks);
        if (normalizedPickupTicks <= 0 || events == null || events.isEmpty()) {
            return events == null ? Collections.<MidiTimeSignatureEvent>emptyList() : events;
        }
        MidiTimeSignatureEvent baseAtZero = null;
        for (MidiTimeSignatureEvent event : events) {
            if (event != null && event.getStartTicks() == 0) {
                baseAtZero = event;
                break;
            }
        }
        if (baseAtZero == null) {
            return events;
        }
        int baseBeatType = baseAtZero.getBeatType();
        int baseBeats = baseAtZero.getBeats();
        int fullMeasureTicks = Math.max(1,
                Math.round((Math.max(1, ticksPerQuarter) * 4.0f * baseBeats) / baseBeatType));
        if (normalizedPickupTicks >= fullMeasureTicks) {
            return events;
        }
        double pickupBeatsFloat = (normalizedPickupTicks * baseBeatType) / (Math.max(1, ticksPerQuarter) * 4.0d);
        int pickupBeats = (int) Math.round(pickupBeatsFloat);
        if (Double.isNaN(pickupBeatsFloat) || Double.isInfinite(pickupBeatsFloat)
                || Math.abs(pickupBeatsFloat - pickupBeats) > 0.000001d) {
            return events;
        }
        if (pickupBeats < 1 || pickupBeats >= baseBeats) {
            return events;
        }
        boolean hasPickupPrelude = false;
        boolean hasBaseAtPickup = false;
        for (MidiTimeSignatureEvent event : events) {
            if (event == null) {
                continue;
            }
            if (event.getStartTicks() == 0 && event.getBeats() == pickupBeats
                    && event.getBeatType() == baseBeatType) {
                hasPickupPrelude = true;
            }
            if (event.getStartTicks() == normalizedPickupTicks && event.getBeats() == baseBeats
                    && event.getBeatType() == baseBeatType) {
                hasBaseAtPickup = true;
            }
        }
        if (hasPickupPrelude && hasBaseAtPickup) {
            return events;
        }

        List<MidiTimeSignatureEvent> remapped = new ArrayList<MidiTimeSignatureEvent>();
        remapped.add(new MidiTimeSignatureEvent(0, pickupBeats, baseBeatType));
        remapped.add(new MidiTimeSignatureEvent(normalizedPickupTicks, baseBeats, baseBeatType));
        for (MidiTimeSignatureEvent event : events) {
            if (event == null) {
                continue;
            }
            int tick = event.getStartTicks();
            if (tick == 0) {
                continue;
            }
            if (tick == normalizedPickupTicks && event.getBeats() == baseBeats
                    && event.getBeatType() == baseBeatType) {
                continue;
            }
            remapped.add(new MidiTimeSignatureEvent(tick, event.getBeats(), event.getBeatType()));
        }
        Collections.sort(remapped, new Comparator<MidiTimeSignatureEvent>() {
            @Override
            public int compare(MidiTimeSignatureEvent left, MidiTimeSignatureEvent right) {
                return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
            }
        });
        return remapped;
    }

    public static int mod12(int value) {
        int rounded = Math.round(value);
        return ((rounded % 12) + 12) % 12;
    }

    public static int keyTonicPitchClassFromFifths(int fifths, String mode) {
        int boundedFifths = Math.max(-7, Math.min(7, Math.round(fifths)));
        int majorTonic = mod12(7 * boundedFifths);
        return "minor".equals(mode) ? mod12(majorTonic + 9) : majorTonic;
    }

    public static Set<Integer> keyScalePitchClasses(int fifths, String mode) {
        int tonic = keyTonicPitchClassFromFifths(fifths, mode);
        int[] intervals = "minor".equals(mode) ? new int[] { 0, 2, 3, 5, 7, 8, 10 }
                : new int[] { 0, 2, 4, 5, 7, 9, 11 };
        Set<Integer> out = new LinkedHashSet<Integer>();
        for (int interval : intervals) {
            out.add(Integer.valueOf(mod12(tonic + interval)));
        }
        return out;
    }

    public static MidiKeySignature inferKeySignatureFromImportedNotes(List<ImportedQuantizedNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return null;
        }
        int[] pitchClassWeights = new int[12];
        for (ImportedQuantizedNote note : notes) {
            if (note == null) {
                continue;
            }
            int pitchClass = mod12(note.getMidi());
            int duration = Math.max(1, note.getEndTick() - note.getStartTick());
            pitchClassWeights[pitchClass] += duration;
        }
        int totalWeight = 0;
        int uniquePitchClasses = 0;
        for (int weight : pitchClassWeights) {
            totalWeight += weight;
            if (weight > 0) {
                uniquePitchClasses++;
            }
        }
        if (totalWeight <= 0 || notes.size() < 3 || uniquePitchClasses < 3) {
            return null;
        }

        List<ImportedQuantizedNote> sortedNotes = new ArrayList<ImportedQuantizedNote>();
        for (ImportedQuantizedNote note : notes) {
            if (note != null) {
                sortedNotes.add(note);
            }
        }
        Collections.sort(sortedNotes, new Comparator<ImportedQuantizedNote>() {
            @Override
            public int compare(ImportedQuantizedNote left, ImportedQuantizedNote right) {
                if (left.getStartTick() == right.getStartTick()) {
                    return Integer.valueOf(left.getMidi()).compareTo(Integer.valueOf(right.getMidi()));
                }
                return Integer.valueOf(left.getStartTick()).compareTo(Integer.valueOf(right.getStartTick()));
            }
        });
        if (sortedNotes.size() < 3) {
            return null;
        }
        int firstPitchClass = mod12(sortedNotes.get(0).getMidi());
        int lastPitchClass = mod12(sortedNotes.get(sortedNotes.size() - 1).getMidi());
        MidiKeySignature best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int fifths = -7; fifths <= 7; fifths++) {
            for (String mode : new String[] { "major", "minor" }) {
                Set<Integer> inScale = keyScalePitchClasses(fifths, mode);
                int tonicPitchClass = keyTonicPitchClassFromFifths(fifths, mode);
                double score = 0;
                for (int pitchClass = 0; pitchClass < 12; pitchClass++) {
                    int weight = pitchClassWeights[pitchClass];
                    if (weight <= 0) {
                        continue;
                    }
                    score += inScale.contains(Integer.valueOf(pitchClass)) ? weight : -weight * 0.55d;
                }
                score += pitchClassWeights[tonicPitchClass] * 0.2d;
                if (firstPitchClass == tonicPitchClass) {
                    score += totalWeight * 0.08d;
                }
                if (lastPitchClass == tonicPitchClass) {
                    score += totalWeight * 0.12d;
                }
                if (best == null || score > bestScore || (score == bestScore
                        && Math.abs(fifths) < Math.abs(best.getFifths()))
                        || (score == bestScore && Math.abs(fifths) == Math.abs(best.getFifths())
                                && "major".equals(mode))) {
                    best = new MidiKeySignature(fifths, mode);
                    bestScore = score;
                }
            }
        }
        return best;
    }

    public static Integer drumNameHintToGmNote(String name) {
        String text = name == null ? "" : name;
        if (matchesHint(text, "kick", "bass drum", "bd")) {
            return Integer.valueOf(36);
        }
        if (matchesHint(text, "snare", "sd")) {
            return Integer.valueOf(38);
        }
        if (matchesHint(text, "rim")) {
            return Integer.valueOf(37);
        }
        if (matchesHint(text, "clap")) {
            return Integer.valueOf(39);
        }
        if (matchesHint(text, "closed hihat", "closed hi-hat", "chh", "hh closed")) {
            return Integer.valueOf(42);
        }
        if (matchesHint(text, "pedal hihat", "pedal hi-hat")) {
            return Integer.valueOf(44);
        }
        if (matchesHint(text, "open hihat", "open hi-hat", "ohh", "hh open")) {
            return Integer.valueOf(46);
        }
        if (matchesHint(text, "low tom", "floor tom")) {
            return Integer.valueOf(45);
        }
        if (matchesHint(text, "mid tom", "middle tom")) {
            return Integer.valueOf(47);
        }
        if (matchesHint(text, "high tom")) {
            return Integer.valueOf(50);
        }
        if (matchesHint(text, "crash")) {
            return Integer.valueOf(49);
        }
        if (matchesHint(text, "ride")) {
            return Integer.valueOf(51);
        }
        if (matchesHint(text, "cowbell")) {
            return Integer.valueOf(56);
        }
        if (matchesHint(text, "tambourine")) {
            return Integer.valueOf(54);
        }
        if (matchesHint(text, "shaker", "maracas")) {
            return Integer.valueOf(70);
        }
        if (matchesHint(text, "conga")) {
            return Integer.valueOf(64);
        }
        if (matchesHint(text, "bongo")) {
            return Integer.valueOf(60);
        }
        if (matchesHint(text, "timbale")) {
            return Integer.valueOf(65);
        }
        if (matchesHint(text, "agogo")) {
            return Integer.valueOf(67);
        }
        if (matchesHint(text, "triangle")) {
            return Integer.valueOf(81);
        }
        return null;
    }

    private static boolean matchesHint(String text, String... hints) {
        String lower = text == null ? "" : text.toLowerCase();
        for (String hint : hints) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    public static Integer dynamicsToVelocity(String tag) {
        String value = tag == null ? "" : tag.trim().toLowerCase();
        if ("pppp".equals(value)) {
            return Integer.valueOf(20);
        }
        if ("ppp".equals(value)) {
            return Integer.valueOf(28);
        }
        if ("pp".equals(value)) {
            return Integer.valueOf(38);
        }
        if ("p".equals(value)) {
            return Integer.valueOf(50);
        }
        if ("mp".equals(value)) {
            return Integer.valueOf(64);
        }
        if ("mf".equals(value)) {
            return Integer.valueOf(80);
        }
        if ("f".equals(value)) {
            return Integer.valueOf(96);
        }
        if ("ff".equals(value)) {
            return Integer.valueOf(112);
        }
        if ("fff".equals(value)) {
            return Integer.valueOf(120);
        }
        if ("ffff".equals(value)) {
            return Integer.valueOf(126);
        }
        if ("sfz".equals(value)) {
            return Integer.valueOf(110);
        }
        if ("sf".equals(value)) {
            return Integer.valueOf(108);
        }
        if ("rfz".equals(value)) {
            return Integer.valueOf(106);
        }
        return null;
    }

    public static boolean isGenericMidiTrackName(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return true;
        }
        return text.matches("(?i)^(track|trk)\\s*\\d+(\\s*ch(?:annel)?\\s*\\d+)?$");
    }

    public static String parseStandardTitleFromMetaText(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)^(title|piece|movement)\\s*[:=]\\s*(.+)$").matcher(text);
        return matcher.matches() ? matcher.group(2).trim() : "";
    }

    public static String parseStandardComposerFromMetaText(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)^(composer|comp)\\s*[:=]\\s*(.+)$")
                .matcher(text);
        return matcher.matches() ? matcher.group(2).trim() : "";
    }

    public static String normalizeMetricAccentProfile(String value) {
        if ("balanced".equals(value) || "strong".equals(value)) {
            return value;
        }
        return "subtle";
    }

    public static List<Integer> buildMetricAccentPattern(int beats, int beatType, String profile) {
        String normalizedProfile = normalizeMetricAccentProfile(profile);
        int strong = "strong".equals(normalizedProfile) ? 6 : ("balanced".equals(normalizedProfile) ? 4 : 2);
        int medium = "strong".equals(normalizedProfile) ? 3 : ("balanced".equals(normalizedProfile) ? 2 : 1);
        if (beats == 4 && beatType == 4) {
            return integers(strong, 0, medium, 0);
        }
        if (beats == 6 && beatType == 8) {
            return integers(strong, 0, 0, medium, 0, 0);
        }
        if (beats == 3) {
            return integers(strong, 0, 0);
        }
        if (beats == 5) {
            return integers(strong, 0, medium, 0, 0);
        }
        List<Integer> out = new ArrayList<Integer>();
        out.add(Integer.valueOf(strong));
        int rest = Math.max(0, beats - 1);
        for (int index = 0; index < rest; index++) {
            out.add(Integer.valueOf(0));
        }
        return out;
    }

    public static int getMetricAccentVelocityDelta(double startDiv, double divisions, double beats, double beatType,
            String profile) {
        if (Double.isNaN(startDiv) || Double.isInfinite(startDiv) || Double.isNaN(divisions)
                || Double.isInfinite(divisions) || Double.isNaN(beats) || Double.isInfinite(beats)
                || Double.isNaN(beatType) || Double.isInfinite(beatType)) {
            return 0;
        }
        if (divisions <= 0 || beats <= 0 || beatType <= 0) {
            return 0;
        }
        double beatUnitDiv = (divisions * 4.0d) / beatType;
        if (Double.isNaN(beatUnitDiv) || Double.isInfinite(beatUnitDiv) || beatUnitDiv <= 0) {
            return 0;
        }
        double measureDiv = beatUnitDiv * beats;
        if (Double.isNaN(measureDiv) || Double.isInfinite(measureDiv) || measureDiv <= 0) {
            return 0;
        }
        double normalizedStartDiv = ((startDiv % measureDiv) + measureDiv) % measureDiv;
        int beatIndex = Math.max(0, Math.min((int) Math.round(beats) - 1,
                (int) Math.floor(normalizedStartDiv / beatUnitDiv)));
        List<Integer> pattern = buildMetricAccentPattern((int) Math.round(beats), (int) Math.round(beatType),
                normalizeMetricAccentProfile(profile));
        if (pattern.isEmpty()) {
            return 0;
        }
        return pattern.get(beatIndex % pattern.size()).intValue();
    }

    public static List<Integer> splitTicks(int totalTicks, int parts) {
        int safeParts = Math.max(1, Math.round(parts));
        int base = (int) Math.floor(totalTicks / (double) safeParts);
        int rest = totalTicks - base * safeParts;
        List<Integer> out = new ArrayList<Integer>();
        for (int index = 0; index < safeParts; index++) {
            out.add(Integer.valueOf(base + (index < rest ? 1 : 0)));
        }
        return out;
    }

    public static List<Integer> splitTicksWeighted(int totalTicks, List<Double> rawWeights) {
        List<Double> weights = new ArrayList<Double>();
        if (rawWeights != null) {
            for (Double rawWeight : rawWeights) {
                double weight = rawWeight == null ? Double.NaN : rawWeight.doubleValue();
                weights.add(Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0 ? Double.valueOf(1)
                        : Double.valueOf(weight));
            }
        }
        int count = weights.size();
        if (count == 0) {
            return Collections.emptyList();
        }
        int safeTotal = Math.max(count, Math.round(totalTicks));
        double weightSum = 0;
        for (Double weight : weights) {
            weightSum += weight.doubleValue();
        }
        if (weightSum == 0) {
            weightSum = count;
        }
        List<Double> provisional = new ArrayList<Double>();
        List<Integer> floors = new ArrayList<Integer>();
        int assigned = 0;
        for (Double weight : weights) {
            double value = (safeTotal * weight.doubleValue()) / weightSum;
            provisional.add(Double.valueOf(value));
            int floor = Math.max(1, (int) Math.floor(value));
            floors.add(Integer.valueOf(floor));
            assigned += floor;
        }
        if (assigned > safeTotal) {
            int overflow = assigned - safeTotal;
            for (int index = count - 1; index >= 0 && overflow > 0; index--) {
                int canRemove = Math.max(0, floors.get(index).intValue() - 1);
                int take = Math.min(canRemove, overflow);
                floors.set(index, Integer.valueOf(floors.get(index).intValue() - take));
                overflow -= take;
            }
            return floors;
        }
        int remaining = safeTotal - assigned;
        List<WeightedSplitOrder> order = new ArrayList<WeightedSplitOrder>();
        for (int index = 0; index < provisional.size(); index++) {
            double value = provisional.get(index).doubleValue();
            order.add(new WeightedSplitOrder(index, value - Math.floor(value)));
        }
        Collections.sort(order, new Comparator<WeightedSplitOrder>() {
            @Override
            public int compare(WeightedSplitOrder left, WeightedSplitOrder right) {
                int fracCompare = Double.valueOf(right.getFraction()).compareTo(Double.valueOf(left.getFraction()));
                return fracCompare != 0 ? fracCompare
                        : Integer.valueOf(left.getIndex()).compareTo(Integer.valueOf(right.getIndex()));
            }
        });
        int cursor = 0;
        while (remaining > 0) {
            int target = order.get(cursor % order.size()).getIndex();
            floors.set(target, Integer.valueOf(floors.get(target).intValue() + 1));
            remaining--;
            cursor++;
        }
        return floors;
    }

    public static Integer pitchToMidi(String step, int alter, int octave) {
        String value = step == null ? "" : step.trim();
        int base;
        if ("C".equals(value)) {
            base = 0;
        } else if ("D".equals(value)) {
            base = 2;
        } else if ("E".equals(value)) {
            base = 4;
        } else if ("F".equals(value)) {
            base = 5;
        } else if ("G".equals(value)) {
            base = 7;
        } else if ("A".equals(value)) {
            base = 9;
        } else if ("B".equals(value)) {
            base = 11;
        } else {
            return null;
        }
        return Integer.valueOf((octave + 1) * 12 + base + alter);
    }

    public static int keySignatureAlterByStep(int fifths, String step) {
        String value = step == null ? "" : step.trim();
        String[] sharpOrder = new String[] { "F", "C", "G", "D", "A", "E", "B" };
        String[] flatOrder = new String[] { "B", "E", "A", "D", "G", "C", "F" };
        int safeFifths = Math.max(-7, Math.min(7, Math.round(fifths)));
        if (safeFifths > 0) {
            for (int index = 0; index < safeFifths; index++) {
                if (sharpOrder[index].equals(value)) {
                    return 1;
                }
            }
        } else if (safeFifths < 0) {
            for (int index = 0; index < Math.abs(safeFifths); index++) {
                if (flatOrder[index].equals(value)) {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static Integer accidentalTextToAlter(String text) {
        String normalized = text == null ? "" : text.trim().toLowerCase();
        if (normalized.length() == 0) {
            return null;
        }
        if ("sharp".equals(normalized)) {
            return Integer.valueOf(1);
        }
        if ("flat".equals(normalized)) {
            return Integer.valueOf(-1);
        }
        if ("natural".equals(normalized)) {
            return Integer.valueOf(0);
        }
        if ("double-sharp".equals(normalized)) {
            return Integer.valueOf(2);
        }
        if ("flat-flat".equals(normalized)) {
            return Integer.valueOf(-2);
        }
        return null;
    }

    public static String midiToPitchText(int midiNumber) {
        String[] names = new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
        int note = Math.max(0, Math.min(127, Math.round(midiNumber)));
        int octave = (int) Math.floor(note / 12.0d) - 1;
        return names[note % 12] + octave;
    }

    public static int normalizeTicksPerQuarter(double ticksPerQuarter) {
        if (Double.isNaN(ticksPerQuarter) || Double.isInfinite(ticksPerQuarter)) {
            return 480;
        }
        return Math.max(1, (int) Math.round(ticksPerQuarter));
    }

    public static Integer normalizeMidiProgramNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        int rounded = (int) Math.round(value);
        if (rounded < 1 || rounded > 128) {
            return null;
        }
        return Integer.valueOf(rounded);
    }

    public static String normalizeMidiImportQuantizeGridOption(String value) {
        if ("auto".equals(value) || "1/8".equals(value) || "1/16".equals(value) || "1/32".equals(value)
                || "1/64".equals(value)) {
            return value;
        }
        return "1/64";
    }

    public static int quantizeGridToDivisions(String grid) {
        if ("1/8".equals(grid)) {
            return 2;
        }
        if ("1/64".equals(grid)) {
            return 16;
        }
        if ("1/32".equals(grid)) {
            return 8;
        }
        return 4;
    }

    public static int gcdInt(int a, int b) {
        int x = Math.abs(Math.round(a));
        int y = Math.abs(Math.round(b));
        if (x == 0) {
            return Math.max(1, y);
        }
        if (y == 0) {
            return Math.max(1, x);
        }
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return Math.max(1, x);
    }

    public static boolean isNearMultiple(double value, double base, double tolerance) {
        if (Double.isNaN(value) || Double.isInfinite(value) || Double.isNaN(base) || Double.isInfinite(base)
                || base <= 0) {
            return false;
        }
        double nearest = Math.round(value / base) * base;
        return Math.abs(value - nearest) <= tolerance;
    }

    public static boolean hasTripletLikeTiming(List<SmfImportedNote> notes, double ticksPerQuarter) {
        if (notes == null || notes.isEmpty() || Double.isNaN(ticksPerQuarter) || Double.isInfinite(ticksPerQuarter)
                || ticksPerQuarter <= 0) {
            return false;
        }
        double tripletTick = ticksPerQuarter / 3.0d;
        int tolerance = Math.max(1, (int) Math.round(ticksPerQuarter / 96.0d));
        int evidence = 0;
        for (SmfImportedNote note : notes) {
            if (note == null) {
                continue;
            }
            int duration = Math.max(1, note.getEndTick() - note.getStartTick());
            if (isNearMultiple(note.getStartTick(), tripletTick, tolerance)) {
                evidence++;
            }
            if (isNearMultiple(duration, tripletTick, tolerance)) {
                evidence++;
            }
            if (evidence >= 4) {
                return true;
            }
        }
        return false;
    }

    public static ImportQuantizeResolution resolveImportQuantizeTick(List<SmfImportedNote> notes, int ticksPerQuarter,
            String grid, boolean tripletAwareQuantize) {
        int subdivision = quantizeGridToDivisions(grid);
        int baseQTick = Math.max(1, Math.round(ticksPerQuarter / (float) subdivision));
        boolean useTripletAwareQuantize = tripletAwareQuantize && "1/16".equals(grid)
                && hasTripletLikeTiming(notes, ticksPerQuarter);
        int tripletQTick = Math.max(1, Math.round(ticksPerQuarter / 3.0f));
        int qTick = useTripletAwareQuantize ? gcdInt(baseQTick, tripletQTick) : baseQTick;
        int divisions = Math.max(1, Math.round(ticksPerQuarter / (float) qTick));
        return new ImportQuantizeResolution(qTick, divisions);
    }

    public static int scoreImportQuantization(List<SmfImportedNote> notes, int qTick) {
        int score = 0;
        int safeQTick = Math.max(1, qTick);
        if (notes != null) {
            for (SmfImportedNote note : notes) {
                if (note == null) {
                    continue;
                }
                int start = Math.max(0, note.getStartTick());
                int end = Math.max(start + 1, note.getEndTick());
                int duration = Math.max(1, end - start);
                int quantizedStart = Math.round(start / (float) safeQTick) * safeQTick;
                int quantizedEnd = Math.round(end / (float) safeQTick) * safeQTick;
                int quantizedDuration = Math.max(safeQTick, Math.round(duration / (float) safeQTick) * safeQTick);
                int startError = Math.abs(start - quantizedStart);
                int endError = Math.abs(end - quantizedEnd);
                int durationError = Math.abs(duration - quantizedDuration);
                score += startError * 2 + endError + durationError;
            }
        }
        return score;
    }

    public static String chooseBestImportQuantizeGrid(List<SmfImportedNote> notes, int ticksPerQuarter,
            boolean tripletAwareQuantize) {
        String[] candidates = new String[] { "1/8", "1/16", "1/32", "1/64" };
        String bestGrid = null;
        int bestScore = 0;
        int bestDivisions = 0;
        for (String grid : candidates) {
            ImportQuantizeResolution resolved = resolveImportQuantizeTick(notes, ticksPerQuarter, grid,
                    tripletAwareQuantize);
            int score = scoreImportQuantization(notes, resolved.getQTick());
            if (bestGrid == null || score < bestScore
                    || (score == bestScore && resolved.getDivisions() < bestDivisions)) {
                bestGrid = grid;
                bestScore = score;
                bestDivisions = resolved.getDivisions();
            }
        }
        return bestGrid == null ? "1/64" : bestGrid;
    }

    public static String readAscii(byte[] bytes, int start, int length) {
        if (bytes == null || start < 0 || length < 0 || start + length > bytes.length) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < length; index++) {
            out.append((char) (bytes[start + index] & 0xff));
        }
        return out.toString();
    }

    public static Long readUint32Be(byte[] bytes, int start) {
        if (bytes == null || start < 0 || start + 4 > bytes.length) {
            return null;
        }
        long value = ((long) (bytes[start] & 0xff) << 24) | ((long) (bytes[start + 1] & 0xff) << 16)
                | ((long) (bytes[start + 2] & 0xff) << 8) | (long) (bytes[start + 3] & 0xff);
        return Long.valueOf(value & 0xffffffffL);
    }

    public static Integer readUint16Be(byte[] bytes, int start) {
        if (bytes == null || start < 0 || start + 2 > bytes.length) {
            return null;
        }
        return Integer.valueOf(((bytes[start] & 0xff) << 8) | (bytes[start + 1] & 0xff));
    }

    public static VariableLengthValue readVariableLengthAt(byte[] bytes, int start) {
        if (bytes == null) {
            return null;
        }
        int value = 0;
        int cursor = start;
        for (int index = 0; index < 4; index++) {
            if (cursor < 0 || cursor >= bytes.length) {
                return null;
            }
            int current = bytes[cursor] & 0xff;
            value = (value << 7) | (current & 0x7f);
            cursor++;
            if ((current & 0x80) == 0) {
                return new VariableLengthValue(value, cursor);
            }
        }
        return null;
    }

    public static byte[] numberToVariableLength(double value) {
        int buffer = (int) (Math.max(0L, Math.round(value)) & 0x0fffffffL);
        byte[] bytes = new byte[] { (byte) (buffer & 0x7f) };
        buffer >>= 7;
        while (buffer > 0) {
            byte[] next = new byte[bytes.length + 1];
            next[0] = (byte) ((buffer & 0x7f) | 0x80);
            System.arraycopy(bytes, 0, next, 1, bytes.length);
            bytes = next;
            buffer >>= 7;
        }
        return bytes;
    }

    public static byte[] buildMksSysexEventData(double deltaTicks, String payloadText) {
        String safePayloadText = payloadText == null ? "" : payloadText;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytes(out, numberToVariableLength(deltaTicks));
        out.write(0xf0);
        writeBytes(out, numberToVariableLength(safePayloadText.length() + 1));
        for (int index = 0; index < safePayloadText.length(); index++) {
            out.write(safePayloadText.charAt(index) & 0x7f);
        }
        out.write(0xf7);
        return out.toByteArray();
    }

    public static byte[] buildTextMetaEventData(double deltaTicks, String text) {
        return buildTextMetaEventData(deltaTicks, text, 0x01);
    }

    public static byte[] buildTextMetaEventData(double deltaTicks, String text, int metaType) {
        String safeText = text == null ? "" : text;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytes(out, numberToVariableLength(deltaTicks));
        out.write(0xff);
        out.write(metaType & 0xff);
        writeBytes(out, numberToVariableLength(safeText.length()));
        for (int index = 0; index < safeText.length(); index++) {
            out.write(safeText.charAt(index) & 0xff);
        }
        return out.toByteArray();
    }

    public static String fnv1a32Hex(String text) {
        String safeText = text == null ? "" : text;
        int hash = 0x811c9dc5;
        for (int index = 0; index < safeText.length(); index++) {
            hash ^= safeText.charAt(index) & 0xff;
            hash *= 0x01000193;
        }
        return padLeft(Long.toHexString(hash & 0xffffffffL).toUpperCase(), 8, '0');
    }

    public static List<String> chunkString(String text, int size) {
        String safeText = text == null ? "" : text;
        int safeSize = Math.max(1, Math.round(size));
        List<String> out = new ArrayList<String>();
        for (int index = 0; index < safeText.length(); index += safeSize) {
            out.add(safeText.substring(index, Math.min(safeText.length(), index + safeSize)));
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return Collections.unmodifiableList(out);
    }

    public static List<String> buildMksSysexChunkTexts(MksSysexChunkTextParams params) {
        MksSysexChunkTextParams safeParams = params == null ? new MksSysexChunkTextParams(0, 0, 0, 0, 0, 0, 0, 0,
                Collections.<String>emptyList()) : params;
        List<String> diagnostics = new ArrayList<String>();
        if (safeParams.getDiagnostics() != null) {
            for (String diagnostic : safeParams.getDiagnostics()) {
                if (diagnostic != null && diagnostic.trim().length() > 0) {
                    diagnostics.add(diagnostic);
                }
            }
        }
        String fingerprint = fnv1a32Hex(joinWithPipe(new int[] {
                safeParams.getTicksPerQuarter(),
                safeParams.getEventCount(),
                safeParams.getTrackCount(),
                safeParams.getTempoEventCount(),
                safeParams.getTimeSignatureEventCount(),
                safeParams.getKeySignatureEventCount(),
                safeParams.getControlEventCount(),
                safeParams.getChannelCount() }));
        StringBuilder metadata = new StringBuilder();
        appendLine(metadata, "schema=mks-sysex-v1");
        appendLine(metadata, "namespace=mks");
        appendLine(metadata, "app=mikuscore");
        appendLine(metadata, "source=musicxml");
        appendLine(metadata, "tpq=" + Math.max(1, Math.round(safeParams.getTicksPerQuarter())));
        appendLine(metadata, "track-count=" + Math.max(0, Math.round(safeParams.getTrackCount())));
        appendLine(metadata, "event-count=" + Math.max(0, Math.round(safeParams.getEventCount())));
        appendLine(metadata, "tempo-event-count=" + Math.max(0, Math.round(safeParams.getTempoEventCount())));
        appendLine(metadata, "timesig-event-count=" + Math.max(0, Math.round(safeParams.getTimeSignatureEventCount())));
        appendLine(metadata, "keysig-event-count=" + Math.max(0, Math.round(safeParams.getKeySignatureEventCount())));
        appendLine(metadata, "control-event-count=" + Math.max(0, Math.round(safeParams.getControlEventCount())));
        appendLine(metadata, "channel-count=" + Math.max(0, Math.round(safeParams.getChannelCount())));
        appendLine(metadata, "diag-count=" + diagnostics.size());
        for (int index = 0; index < diagnostics.size(); index++) {
            appendLine(metadata, "diag-" + padLeft(Integer.toString(index + 1), 4, '0') + "="
                    + diagnostics.get(index));
        }
        appendLine(metadata, "fingerprint-fnv1a32=" + fingerprint);

        List<String> payloadChunks = chunkString(encodeURIComponent(metadata.toString()), 180);
        List<String> out = new ArrayList<String>();
        int total = payloadChunks.size();
        for (int index = 0; index < payloadChunks.size(); index++) {
            out.add("mks|v=1|m=0001|i=" + padLeft(Integer.toString(index + 1), 4, '0') + "|n="
                    + padLeft(Integer.toString(total), 4, '0') + "|d=" + payloadChunks.get(index));
        }
        return Collections.unmodifiableList(out);
    }

    public static byte[] buildTempoMetaEventData(double deltaTicks, double bpm) {
        int safeBpm = clampTempo(bpm);
        int microsPerQuarter = Math.max(1, (int) Math.round(60000000.0d / safeBpm));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytes(out, numberToVariableLength(deltaTicks));
        out.write(0xff);
        out.write(0x51);
        out.write(0x03);
        out.write((microsPerQuarter >> 16) & 0xff);
        out.write((microsPerQuarter >> 8) & 0xff);
        out.write(microsPerQuarter & 0xff);
        return out.toByteArray();
    }

    public static byte[] buildTimeSignatureMetaEventData(double deltaTicks, double beats, double beatType) {
        int safeBeats = Math.max(1, Math.min(255, (int) Math.round(beats)));
        int safeBeatType = Math.max(1, (int) Math.round(beatType));
        int dd = Math.max(0, Math.min(7, (int) Math.round(Math.log(safeBeatType) / Math.log(2.0d))));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytes(out, numberToVariableLength(deltaTicks));
        out.write(0xff);
        out.write(0x58);
        out.write(0x04);
        out.write(safeBeats & 0xff);
        out.write(dd & 0xff);
        out.write(24);
        out.write(8);
        return out.toByteArray();
    }

    public static byte[] buildKeySignatureMetaEventData(double deltaTicks, double fifths, String mode) {
        int safeFifths = Math.max(-7, Math.min(7, (int) Math.round(fifths)));
        int sf = safeFifths < 0 ? safeFifths + 256 : safeFifths;
        int mi = "minor".equals(mode) ? 1 : 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytes(out, numberToVariableLength(deltaTicks));
        out.write(0xff);
        out.write(0x59);
        out.write(0x02);
        out.write(sf & 0xff);
        out.write(mi);
        return out.toByteArray();
    }

    public static byte[] toU16BeBytes(double value) {
        int normalized = Math.max(0, Math.min(0xffff, (int) Math.round(value)));
        return new byte[] { (byte) ((normalized >> 8) & 0xff), (byte) (normalized & 0xff) };
    }

    public static byte[] toU32BeBytes(double value) {
        long normalized = Math.max(0L, Math.min(0xffffffffL, Math.round(value)));
        return new byte[] { (byte) ((normalized >> 24) & 0xff), (byte) ((normalized >> 16) & 0xff),
                (byte) ((normalized >> 8) & 0xff), (byte) (normalized & 0xff) };
    }

    public static int toMidiWriterVelocityByte(double velocity) {
        int normalized = Math.max(1, Math.min(100, (int) Math.round(velocity)));
        return Math.max(0, Math.min(127, (int) Math.round((normalized / 100.0d) * 127)));
    }

    public static byte[] encodeRawTrackChunk(List<RawTrackEvent> events) {
        List<RawTrackEvent> sorted = events == null ? new ArrayList<RawTrackEvent>()
                : new ArrayList<RawTrackEvent>(events);
        Collections.sort(sorted, new Comparator<RawTrackEvent>() {
            @Override
            public int compare(RawTrackEvent left, RawTrackEvent right) {
                if (left == right) {
                    return 0;
                }
                if (left == null) {
                    return 1;
                }
                if (right == null) {
                    return -1;
                }
                if (left.getTick() == right.getTick()) {
                    if (left.getOrder() == right.getOrder()) {
                        return Integer.valueOf(left.getSortKey()).compareTo(Integer.valueOf(right.getSortKey()));
                    }
                    return Integer.valueOf(left.getOrder()).compareTo(Integer.valueOf(right.getOrder()));
                }
                return Integer.valueOf(left.getTick()).compareTo(Integer.valueOf(right.getTick()));
            }
        });
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int prevTick = 0;
        for (RawTrackEvent event : sorted) {
            if (event == null) {
                continue;
            }
            int tick = Math.max(0, Math.round(event.getTick()));
            int delta = Math.max(0, tick - prevTick);
            writeBytes(body, numberToVariableLength(delta));
            writeBytes(body, event.getBytes());
            prevTick = tick;
        }
        body.write(0x00);
        body.write(0xff);
        body.write(0x2f);
        body.write(0x00);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x4d);
        out.write(0x54);
        out.write(0x72);
        out.write(0x6b);
        writeBytes(out, toU32BeBytes(body.size()));
        writeBytes(out, body.toByteArray());
        return out.toByteArray();
    }

    public static byte[] buildRawMidiTempoTrackChunk(List<MidiTempoEvent> tempoEvents,
            List<MidiTimeSignatureEvent> timeSignatureEvents, List<MidiTickKeySignatureEvent> keySignatureEvents,
            RawMidiTempoTrackOptions options) {
        RawMidiTempoTrackOptions safeOptions = options == null
                ? new RawMidiTempoTrackOptions(false, Collections.<String>emptyList(),
                        Collections.<String>emptyList(), "")
                : options;
        List<RawTrackEvent> events = new ArrayList<RawTrackEvent>();
        events.add(new RawTrackEvent(0, -1, stripInitialDeltaZero(
                buildTextMetaEventData(0, safeOptions.getMetaTrackName(), 0x03))));

        if (tempoEvents != null) {
            for (MidiTempoEvent event : tempoEvents) {
                if (event == null) {
                    continue;
                }
                events.add(new RawTrackEvent(event.getTick(), 0,
                        stripInitialDeltaZero(buildTempoMetaEventData(0, event.getBpm()))));
            }
        }
        if (timeSignatureEvents != null) {
            for (MidiTimeSignatureEvent event : timeSignatureEvents) {
                if (event == null) {
                    continue;
                }
                events.add(new RawTrackEvent(event.getStartTicks(), 1,
                        stripInitialDeltaZero(buildTimeSignatureMetaEventData(0, event.getBeats(),
                                event.getBeatType()))));
            }
        }
        if (keySignatureEvents != null) {
            for (MidiTickKeySignatureEvent event : keySignatureEvents) {
                if (event == null) {
                    continue;
                }
                events.add(new RawTrackEvent(event.getTick(), 2,
                        stripInitialDeltaZero(buildKeySignatureMetaEventData(0, event.getFifths(),
                                event.getMode()))));
            }
        }
        if (safeOptions.isEmbedMksSysEx()) {
            for (String chunkText : safeOptions.getSysexChunkTexts()) {
                events.add(new RawTrackEvent(0, 3, stripInitialDeltaZero(buildMksSysexEventData(0, chunkText))));
            }
        }
        for (String line : safeOptions.getTextMetaLines()) {
            events.add(new RawTrackEvent(0, 4, stripInitialDeltaZero(buildTextMetaEventData(0, line, 0x01))));
        }
        return encodeRawTrackChunk(events);
    }

    public static List<byte[]> buildRawMidiNoteTrackChunks(List<RawMidiPlaybackEvent> sourceEvents,
            Map<String, Integer> trackProgramOverrides, String normalizedProgramPreset, String retriggerPolicy) {
        Map<String, List<RawMidiPlaybackEvent>> tracksById =
                new LinkedHashMap<String, List<RawMidiPlaybackEvent>>();
        if (sourceEvents != null) {
            for (RawMidiPlaybackEvent event : sourceEvents) {
                if (event == null) {
                    continue;
                }
                String key = event.getTrackId().length() > 0 ? event.getTrackId() : "__default__";
                List<RawMidiPlaybackEvent> bucket = tracksById.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<RawMidiPlaybackEvent>();
                    tracksById.put(key, bucket);
                }
                bucket.add(event);
            }
        }

        List<String> sortedTrackIds = new ArrayList<String>(tracksById.keySet());
        Collections.sort(sortedTrackIds);
        List<byte[]> trackChunks = new ArrayList<byte[]>();
        int normalizedProgram = instrumentByPreset(normalizedProgramPreset);
        String policy = normalizeRawMidiRetriggerPolicy(retriggerPolicy);
        Map<String, Integer> safeOverrides = trackProgramOverrides == null ? Collections.<String, Integer>emptyMap()
                : trackProgramOverrides;

        for (String trackId : sortedTrackIds) {
            List<RawMidiPlaybackEvent> trackEvents = new ArrayList<RawMidiPlaybackEvent>(tracksById.get(trackId));
            Collections.sort(trackEvents, new Comparator<RawMidiPlaybackEvent>() {
                @Override
                public int compare(RawMidiPlaybackEvent left, RawMidiPlaybackEvent right) {
                    if (left.getStartTicks() == right.getStartTicks()) {
                        return Integer.valueOf(left.getMidiNumber()).compareTo(Integer.valueOf(right.getMidiNumber()));
                    }
                    return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
                }
            });
            if (trackEvents.isEmpty()) {
                continue;
            }

            List<RawTrackEvent> noteEvents = new ArrayList<RawTrackEvent>();
            String rawTrackName = trimToEmpty(trackEvents.get(0).getTrackName());
            if (rawTrackName.length() == 0) {
                rawTrackName = trackId.length() > 0 ? trackId : "Track";
            }
            noteEvents.add(new RawTrackEvent(0, -1,
                    stripInitialDeltaZero(buildTextMetaEventData(0, rawTrackName, 0x03))));

            Set<Integer> channelSet = new LinkedHashSet<Integer>();
            for (RawMidiPlaybackEvent event : trackEvents) {
                channelSet.add(Integer.valueOf(normalizeMidiChannel(event.getChannel())));
            }
            List<Integer> channels = new ArrayList<Integer>(channelSet);
            Collections.sort(channels);
            Integer rawOverrideProgram = safeOverrides.get(trackId);
            Integer overrideProgram = normalizeMidiProgramNumber(
                    rawOverrideProgram == null ? Double.NaN : rawOverrideProgram.doubleValue());
            int selectedProgram = Math.max(0,
                    Math.min(127, (overrideProgram == null ? normalizedProgram : overrideProgram.intValue()) & 0xff));
            for (Integer channel : channels) {
                if (channel.intValue() == 10) {
                    continue;
                }
                noteEvents.add(new RawTrackEvent(0, 0,
                        new byte[] { (byte) (0xc0 + channel.intValue() - 1), (byte) selectedProgram }));
            }

            for (RawMidiPlaybackEvent event : trackEvents) {
                int channel = normalizeMidiChannel(event.getChannel());
                int midiNumber = Math.max(0, Math.min(127, Math.round(event.getMidiNumber())));
                int startTick = Math.max(0, Math.round(event.getStartTicks()));
                int endTick = Math.max(startTick + 1,
                        startTick + Math.max(1, Math.round(event.getDurTicks())));
                int velocity = toMidiWriterVelocityByte(clampVelocity(event.getVelocity()));
                int offOrder = "on_before_off".equals(policy) ? 2 : 1;
                int onOrder = "on_before_off".equals(policy) ? 1 : 2;
                int pitchOrderKeyOff = midiNumber * 2;
                int pitchOrderKeyOn = midiNumber * 2 + 1;
                boolean isPitchOrder = "pitch_order".equals(policy);
                noteEvents.add(new RawTrackEvent(endTick, isPitchOrder ? 1 : offOrder,
                        isPitchOrder ? pitchOrderKeyOff : 0,
                        new byte[] { (byte) (0x80 + channel - 1), (byte) midiNumber, (byte) velocity }));
                noteEvents.add(new RawTrackEvent(startTick, isPitchOrder ? 1 : onOrder,
                        isPitchOrder ? pitchOrderKeyOn : 0,
                        new byte[] { (byte) (0x90 + channel - 1), (byte) midiNumber, (byte) velocity }));
            }
            trackChunks.add(encodeRawTrackChunk(noteEvents));
        }
        return Collections.unmodifiableList(trackChunks);
    }

    public static List<byte[]> buildRawMidiControlTrackChunks(List<RawMidiControlEvent> controlEvents) {
        Map<String, List<RawMidiControlEvent>> groupedControlEvents =
                new LinkedHashMap<String, List<RawMidiControlEvent>>();
        if (controlEvents != null) {
            for (RawMidiControlEvent controlEvent : controlEvents) {
                if (controlEvent == null) {
                    continue;
                }
                String key = controlEvent.getTrackId() + "::" + normalizeMidiChannel(controlEvent.getChannel());
                List<RawMidiControlEvent> bucket = groupedControlEvents.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<RawMidiControlEvent>();
                    groupedControlEvents.put(key, bucket);
                }
                bucket.add(controlEvent);
            }
        }
        List<String> sortedControlKeys = new ArrayList<String>(groupedControlEvents.keySet());
        Collections.sort(sortedControlKeys);
        List<byte[]> trackChunks = new ArrayList<byte[]>();
        for (String controlKey : sortedControlKeys) {
            List<RawMidiControlEvent> channelEvents = new ArrayList<RawMidiControlEvent>(
                    groupedControlEvents.get(controlKey));
            Collections.sort(channelEvents, new Comparator<RawMidiControlEvent>() {
                @Override
                public int compare(RawMidiControlEvent left, RawMidiControlEvent right) {
                    if (left.getStartTicks() == right.getStartTicks()) {
                        if (left.getControllerNumber() == right.getControllerNumber()) {
                            return Integer.valueOf(left.getControllerValue())
                                    .compareTo(Integer.valueOf(right.getControllerValue()));
                        }
                        return Integer.valueOf(left.getControllerNumber())
                                .compareTo(Integer.valueOf(right.getControllerNumber()));
                    }
                    return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
                }
            });
            if (channelEvents.isEmpty()) {
                continue;
            }
            List<RawTrackEvent> ccEvents = new ArrayList<RawTrackEvent>();
            String baseName = trimToEmpty(channelEvents.get(0).getTrackName());
            if (baseName.length() == 0) {
                baseName = "Track";
            }
            ccEvents.add(new RawTrackEvent(0, -1,
                    stripInitialDeltaZero(buildTextMetaEventData(0, baseName + " Pedal", 0x03))));
            for (RawMidiControlEvent controlEvent : channelEvents) {
                int channel = normalizeMidiChannel(controlEvent.getChannel());
                int controllerNumber = Math.max(0, Math.min(127, Math.round(controlEvent.getControllerNumber())));
                int controllerValue = Math.max(0, Math.min(127, Math.round(controlEvent.getControllerValue())));
                ccEvents.add(new RawTrackEvent(Math.max(0, Math.round(controlEvent.getStartTicks())), 1,
                        new byte[] { (byte) (0xb0 + channel - 1), (byte) controllerNumber,
                                (byte) controllerValue }));
            }
            trackChunks.add(encodeRawTrackChunk(ccEvents));
        }
        return Collections.unmodifiableList(trackChunks);
    }

    public static byte[] buildRawMidiBytesFromTrackChunks(List<byte[]> trackChunks, int writerTicksPerQuarter) {
        List<byte[]> chunks = trackChunks == null ? Collections.<byte[]>emptyList() : trackChunks;
        int size = 14;
        for (byte[] chunk : chunks) {
            if (chunk != null) {
                size += chunk.length;
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        out.write(0x4d);
        out.write(0x54);
        out.write(0x68);
        out.write(0x64);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x06);
        out.write(0x00);
        out.write(0x01);
        writeBytes(out, toU16BeBytes(chunks.size()));
        writeBytes(out, toU16BeBytes(writerTicksPerQuarter));
        for (byte[] chunk : chunks) {
            writeBytes(out, chunk);
        }
        return out.toByteArray();
    }

    public static List<RawMidiPlaybackEvent> normalizePlaybackEventsForParity(List<RawMidiPlaybackEvent> events) {
        Map<String, RawMidiPlaybackEvent> deduped = new LinkedHashMap<String, RawMidiPlaybackEvent>();
        if (events != null) {
            for (RawMidiPlaybackEvent event : events) {
                if (event == null) {
                    continue;
                }
                int channel = normalizeMidiChannel(event.getChannel());
                int startTicks = Math.max(0, Math.round(event.getStartTicks()));
                int durTicks = Math.max(1, Math.round(event.getDurTicks()));
                int midiNumber = Math.round(event.getMidiNumber());
                String key = channel + "|" + midiNumber + "|" + startTicks + "|" + durTicks;
                RawMidiPlaybackEvent prev = deduped.get(key);
                if (prev == null) {
                    deduped.put(key, new RawMidiPlaybackEvent(midiNumber, startTicks, durTicks, channel,
                            event.getVelocity(), event.getTrackId(), event.getTrackName()));
                    continue;
                }
                if (event.getVelocity() > prev.getVelocity()) {
                    deduped.put(key, new RawMidiPlaybackEvent(prev.getMidiNumber(), prev.getStartTicks(),
                            prev.getDurTicks(), prev.getChannel(), event.getVelocity(), prev.getTrackId(),
                            prev.getTrackName()));
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<RawMidiPlaybackEvent>(deduped.values()));
    }

    public static Integer parseMidiNoteNumber(String value) {
        Integer parsed = parseLeadingBase10Integer(value);
        if (parsed == null) {
            return null;
        }
        if (parsed.intValue() < 0 || parsed.intValue() > 127) {
            return null;
        }
        return parsed;
    }

    public static Integer resolveDrumMidiFromInstrumentName(String name) {
        String trimmed = trimToEmpty(name);
        if (trimmed.isEmpty()) {
            return null;
        }
        return drumNameHintToGmNote(trimmed);
    }

    public static Map<String, DrumPartMap> buildDrumPartMapByPartId(Document doc) {
        Map<String, DrumPartMap> byPartId = new LinkedHashMap<String, DrumPartMap>();
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.unmodifiableMap(byPartId);
        }
        Element partList = directChildElementByName(doc.getDocumentElement(), "part-list");
        if (partList == null) {
            return Collections.unmodifiableMap(byPartId);
        }
        for (Element scorePart : directChildElementsByName(partList, "score-part")) {
            String partId = trimToEmpty(scorePart.getAttribute("id"));
            if (partId.isEmpty()) {
                continue;
            }

            Map<String, String> instrumentNameById = new LinkedHashMap<String, String>();
            for (Element scoreInstrument : directChildElementsByName(scorePart, "score-instrument")) {
                String instrumentId = trimToEmpty(scoreInstrument.getAttribute("id"));
                if (instrumentId.isEmpty()) {
                    continue;
                }
                String name = trimToEmpty(directChildText(scoreInstrument, "instrument-name"));
                if (!name.isEmpty()) {
                    instrumentNameById.put(instrumentId, name);
                }
            }

            Map<String, Integer> midiUnpitchedByInstrumentId = new LinkedHashMap<String, Integer>();
            Integer defaultMidiUnpitched = null;
            for (Element midiInstrument : directChildElementsByName(scorePart, "midi-instrument")) {
                Integer midiUnpitched = parseMidiNoteNumber(directChildText(midiInstrument, "midi-unpitched"));
                if (midiUnpitched == null) {
                    continue;
                }
                String midiInstrumentId = trimToEmpty(midiInstrument.getAttribute("id"));
                if (!midiInstrumentId.isEmpty()) {
                    midiUnpitchedByInstrumentId.put(midiInstrumentId, midiUnpitched);
                }
                if (defaultMidiUnpitched == null) {
                    defaultMidiUnpitched = midiUnpitched;
                }
            }

            byPartId.put(partId, new DrumPartMap(midiUnpitchedByInstrumentId, instrumentNameById,
                    defaultMidiUnpitched));
        }
        return Collections.unmodifiableMap(byPartId);
    }

    public static Map<String, Integer> collectMidiProgramOverridesFromMusicXmlDoc(Document doc) {
        Map<String, Integer> byPartId = new LinkedHashMap<String, Integer>();
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.unmodifiableMap(byPartId);
        }
        Element partList = directChildElementByName(doc.getDocumentElement(), "part-list");
        if (partList == null) {
            return Collections.unmodifiableMap(byPartId);
        }
        for (Element scorePart : directChildElementsByName(partList, "score-part")) {
            String partId = trimToEmpty(scorePart.getAttribute("id"));
            if (partId.isEmpty()) {
                continue;
            }
            for (Element midiInstrument : directChildElementsByName(scorePart, "midi-instrument")) {
                String midiProgramText = trimToEmpty(directChildText(midiInstrument, "midi-program"));
                if (midiProgramText.isEmpty()) {
                    continue;
                }
                Integer parsed = parseLeadingBase10Integer(midiProgramText);
                Integer normalized = parsed == null ? null : normalizeMidiProgramNumber(parsed.doubleValue());
                if (normalized == null) {
                    continue;
                }
                byPartId.put(partId, normalized);
                break;
            }
        }
        return Collections.unmodifiableMap(byPartId);
    }

    public static int resolveMeasureAdvanceDiv(Element measure, int measureMaxDiv, int currentDivisions,
            int currentBeats, int currentBeatType) {
        return resolveMeasureAdvanceDiv(measure, measureMaxDiv, currentDivisions, currentBeats, currentBeatType, false,
                false);
    }

    public static int resolveMeasureAdvanceDiv(Element measure, int measureMaxDiv, int currentDivisions,
            int currentBeats, int currentBeatType, boolean nextMeasureIsImplicit, boolean firstMeasureUnderfullAsPickup) {
        int safeDivisions = Math.max(1, Math.round(currentDivisions));
        int safeBeats = Math.max(1, Math.round(currentBeats));
        int safeBeatType = Math.max(1, Math.round(currentBeatType));
        int capacityDiv = Math.max(1, Math.round((safeDivisions * 4.0f * safeBeats) / safeBeatType));
        String implicitAttr = measure == null ? "" : trimToEmpty(measure.getAttribute("implicit")).toLowerCase();
        boolean isImplicit = "yes".equals(implicitAttr) || "true".equals(implicitAttr) || "1".equals(implicitAttr);
        if (isImplicit) {
            return measureMaxDiv > 0 ? measureMaxDiv : capacityDiv;
        }

        boolean hasPreviousMeasure = false;
        if (measure != null) {
            for (Element prev = previousElementSibling(measure); prev != null; prev = previousElementSibling(prev)) {
                String prevName = prev.getLocalName() == null ? prev.getTagName() : prev.getLocalName();
                if ("measure".equals(prevName == null ? "" : prevName.toLowerCase())) {
                    hasPreviousMeasure = true;
                    break;
                }
            }
        }
        boolean isFirstMeasureInPart = !hasPreviousMeasure;
        if (firstMeasureUnderfullAsPickup && isFirstMeasureInPart && measureMaxDiv > 0 && measureMaxDiv < capacityDiv) {
            return measureMaxDiv;
        }
        if (nextMeasureIsImplicit && measureMaxDiv > 0 && measureMaxDiv < capacityDiv) {
            return measureMaxDiv;
        }
        return Math.max(capacityDiv, measureMaxDiv);
    }

    public static int measureCapacityDivFromContext(int divisions, int beats, int beatType) {
        int safeDivisions = Math.max(1, Math.round(divisions));
        int safeBeats = Math.max(1, Math.round(beats));
        int safeBeatType = Math.max(1, Math.round(beatType));
        return Math.max(1, Math.round((safeDivisions * 4.0f * safeBeats) / safeBeatType));
    }

    public static int estimateMeasureContentSpanDiv(Element measure) {
        int cursorDiv = 0;
        int measureMaxDiv = 0;
        Map<String, Integer> lastStartByVoice = new LinkedHashMap<String, Integer>();
        for (Element child : directElementChildren(measure)) {
            String tagName = child.getTagName();
            if ("backup".equals(tagName) || "forward".equals(tagName)) {
                Integer dur = getFirstInteger(child, "duration");
                if (dur == null || dur.intValue() <= 0) {
                    continue;
                }
                if ("backup".equals(tagName)) {
                    cursorDiv = Math.max(0, cursorDiv - dur.intValue());
                } else {
                    cursorDiv += dur.intValue();
                    measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
                }
                continue;
            }
            if (!"note".equals(tagName)) {
                continue;
            }
            Integer durationDiv = getFirstInteger(child, "duration");
            if (durationDiv == null || durationDiv.intValue() <= 0) {
                continue;
            }
            String voice = trimToEmpty(firstDescendantTextByName(child, "voice"));
            if (voice.isEmpty()) {
                voice = "1";
            }
            boolean isChord = firstDescendantElementByName(child, "chord") != null;
            Integer lastStart = lastStartByVoice.get(voice);
            int startDiv = isChord ? (lastStart == null ? cursorDiv : lastStart.intValue()) : cursorDiv;
            if (!isChord) {
                lastStartByVoice.put(voice, Integer.valueOf(startDiv));
                cursorDiv += durationDiv.intValue();
            }
            measureMaxDiv = Math.max(measureMaxDiv, Math.max(cursorDiv, startDiv + durationDiv.intValue()));
        }
        return measureMaxDiv;
    }

    public static boolean shouldTreatFirstUnderfullAsPickup(Document doc) {
        if (doc == null || doc.getDocumentElement() == null) {
            return false;
        }
        Element root = doc.getDocumentElement();
        if (!"score-partwise".equals(root.getNodeName())) {
            return false;
        }
        List<Element> parts = directChildElementsByName(root, "part");
        if (parts.size() < 2) {
            return false;
        }
        for (Element part : parts) {
            Element firstMeasure = directChildElementByName(part, "measure");
            if (firstMeasure == null) {
                return false;
            }
            Integer divisions = getFirstIntegerByDirectPath(firstMeasure, "attributes", "divisions");
            Integer beats = getFirstIntegerByDirectPath(firstMeasure, "attributes", "time", "beats");
            Integer beatType = getFirstIntegerByDirectPath(firstMeasure, "attributes", "time", "beat-type");
            int capacityDiv = measureCapacityDivFromContext(divisions == null ? 1 : divisions.intValue(),
                    beats == null ? 4 : beats.intValue(), beatType == null ? 4 : beatType.intValue());
            int contentDiv = estimateMeasureContentSpanDiv(firstMeasure);
            if (!(contentDiv > 0 && contentDiv < capacityDiv)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isImplicitMeasure(Element measure) {
        if (measure == null) {
            return false;
        }
        String implicitAttr = trimToEmpty(measure.getAttribute("implicit")).toLowerCase();
        return "yes".equals(implicitAttr) || "true".equals(implicitAttr) || "1".equals(implicitAttr);
    }

    public static List<RawMidiControlEvent> collectMidiControlEventsFromMusicXmlDoc(Document doc, int ticksPerQuarter) {
        int normalizedTicksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
        boolean firstUnderfullAsPickup = shouldTreatFirstUnderfullAsPickup(doc);
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.emptyList();
        }
        Element root = doc.getDocumentElement();
        List<Element> partNodes = "score-partwise".equals(root.getNodeName()) ? directChildElementsByName(root, "part")
                : Collections.<Element>emptyList();
        if (partNodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> channelMap = new LinkedHashMap<String, Integer>();
        Map<String, String> partNameById = new LinkedHashMap<String, String>();
        Element partList = directChildElementByName(root, "part-list");
        if (partList != null) {
            for (Element scorePart : directChildElementsByName(partList, "score-part")) {
                String partId = scorePart.getAttribute("id") == null ? "" : scorePart.getAttribute("id");
                if (partId.isEmpty()) {
                    continue;
                }
                Integer midiChannel = null;
                for (Element midiInstrument : directChildElementsByName(scorePart, "midi-instrument")) {
                    midiChannel = parseLeadingBase10Integer(directChildText(midiInstrument, "midi-channel"));
                    if (midiChannel != null) {
                        break;
                    }
                }
                if (midiChannel != null && midiChannel.intValue() >= 1 && midiChannel.intValue() <= 16) {
                    channelMap.put(partId, midiChannel);
                }
                String rawName = trimToEmpty(directChildText(scorePart, "part-name"));
                partNameById.put(partId, rawName.isEmpty() ? partId : rawName);
            }
        }

        List<RawMidiControlEvent> controlEvents = new ArrayList<RawMidiControlEvent>();
        for (int partIndex = 0; partIndex < partNodes.size(); partIndex++) {
            Element part = partNodes.get(partIndex);
            String partId = part.getAttribute("id") == null ? "" : part.getAttribute("id");
            int channelCandidate = (partIndex % 16) + 1;
            int fallbackChannel = channelCandidate == 10 ? 11 : channelCandidate;
            Integer mappedChannel = channelMap.get(partId);
            int channel = mappedChannel == null ? fallbackChannel : mappedChannel.intValue();
            String trackId = partId.isEmpty() ? "part-" + (partIndex + 1) : partId;
            String mappedName = partNameById.get(partId);
            String trackName = mappedName == null ? trackId : mappedName;

            int currentDivisions = 1;
            int currentBeats = 4;
            int currentBeatType = 4;
            int timelineDiv = 0;
            Integer lastPedalValue = null;
            List<Element> measures = directChildElementsByName(part, "measure");
            for (int measureIndex = 0; measureIndex < measures.size(); measureIndex++) {
                Element measure = measures.get(measureIndex);
                Element nextMeasure = measureIndex + 1 < measures.size() ? measures.get(measureIndex + 1) : null;
                Integer divisions = getFirstIntegerByDirectPath(measure, "attributes", "divisions");
                if (divisions != null && divisions.intValue() > 0) {
                    currentDivisions = divisions.intValue();
                }
                Integer beats = getFirstIntegerByDirectPath(measure, "attributes", "time", "beats");
                Integer beatType = getFirstIntegerByDirectPath(measure, "attributes", "time", "beat-type");
                if (beats != null && beats.intValue() > 0 && beatType != null && beatType.intValue() > 0) {
                    currentBeats = beats.intValue();
                    currentBeatType = beatType.intValue();
                }

                int cursorDiv = 0;
                int measureMaxDiv = 0;
                for (Element child : directElementChildren(measure)) {
                    String tagName = child.getTagName();
                    if ("backup".equals(tagName) || "forward".equals(tagName)) {
                        Integer dur = getFirstInteger(child, "duration");
                        if (dur == null || dur.intValue() <= 0) {
                            continue;
                        }
                        if ("backup".equals(tagName)) {
                            cursorDiv = Math.max(0, cursorDiv - dur.intValue());
                        } else {
                            cursorDiv += dur.intValue();
                            measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
                        }
                        continue;
                    }
                    if (!"direction".equals(tagName)) {
                        continue;
                    }
                    List<Element> pedalNodes = directPedalElements(child);
                    if (pedalNodes.isEmpty()) {
                        continue;
                    }
                    int startTicks = Math.max(0,
                            Math.round(((timelineDiv + cursorDiv) / (float) currentDivisions)
                                    * normalizedTicksPerQuarter));
                    for (Element pedalNode : pedalNodes) {
                        String pedalType = trimToEmpty(pedalNode.getAttribute("type")).toLowerCase();
                        if (pedalType.isEmpty()) {
                            pedalType = "start";
                        }
                        if ("stop".equals(pedalType)) {
                            if (lastPedalValue == null || lastPedalValue.intValue() != 0) {
                                controlEvents.add(new RawMidiControlEvent(trackId, trackName, startTicks, channel, 64, 0));
                                lastPedalValue = Integer.valueOf(0);
                            }
                            continue;
                        }
                        if ("change".equals(pedalType)) {
                            if (lastPedalValue == null || lastPedalValue.intValue() != 0) {
                                controlEvents.add(new RawMidiControlEvent(trackId, trackName, startTicks, channel, 64, 0));
                            }
                            controlEvents
                                    .add(new RawMidiControlEvent(trackId, trackName, startTicks, channel, 64, 127));
                            lastPedalValue = Integer.valueOf(127);
                            continue;
                        }
                        if ("start".equals(pedalType) || "continue".equals(pedalType) || "resume".equals(pedalType)) {
                            if (lastPedalValue == null || lastPedalValue.intValue() != 127) {
                                controlEvents.add(
                                        new RawMidiControlEvent(trackId, trackName, startTicks, channel, 64, 127));
                                lastPedalValue = Integer.valueOf(127);
                            }
                        }
                    }
                }

                timelineDiv += resolveMeasureAdvanceDiv(measure, measureMaxDiv, currentDivisions, currentBeats,
                        currentBeatType, isImplicitMeasure(nextMeasure), firstUnderfullAsPickup);
            }
        }

        return Collections.unmodifiableList(controlEvents);
    }

    public static List<MidiTempoEvent> collectMidiTempoEventsFromMusicXmlDoc(Document doc, int ticksPerQuarter) {
        int normalizedTicksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
        boolean firstUnderfullAsPickup = shouldTreatFirstUnderfullAsPickup(doc);
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.singletonList(new MidiTempoEvent(0, 120));
        }
        Element root = doc.getDocumentElement();
        Element firstPart = "score-partwise".equals(root.getNodeName()) ? directChildElementByName(root, "part") : null;
        if (firstPart == null) {
            return Collections.singletonList(new MidiTempoEvent(0, 120));
        }

        int currentDivisions = 1;
        int currentBeats = 4;
        int currentBeatType = 4;
        int timelineDiv = 0;
        int currentTempo = clampTempo(firstSoundTempoInDocument(doc, 120));
        List<MidiTempoEvent> events = new ArrayList<MidiTempoEvent>();
        events.add(new MidiTempoEvent(0, currentTempo));

        List<Element> measures = directChildElementsByName(firstPart, "measure");
        for (int measureIndex = 0; measureIndex < measures.size(); measureIndex++) {
            Element measure = measures.get(measureIndex);
            Element nextMeasure = measureIndex + 1 < measures.size() ? measures.get(measureIndex + 1) : null;
            Integer divisions = getFirstIntegerByDirectPath(measure, "attributes", "divisions");
            if (divisions != null && divisions.intValue() > 0) {
                currentDivisions = divisions.intValue();
            }
            Integer beats = getFirstIntegerByDirectPath(measure, "attributes", "time", "beats");
            Integer beatType = getFirstIntegerByDirectPath(measure, "attributes", "time", "beat-type");
            if (beats != null && beats.intValue() > 0 && beatType != null && beatType.intValue() > 0) {
                currentBeats = beats.intValue();
                currentBeatType = beatType.intValue();
            }

            int cursorDiv = 0;
            int measureMaxDiv = 0;
            Map<String, Integer> lastStartByVoice = new LinkedHashMap<String, Integer>();
            for (Element child : directElementChildren(measure)) {
                String tagName = child.getTagName();
                if ("backup".equals(tagName) || "forward".equals(tagName)) {
                    Integer dur = getFirstInteger(child, "duration");
                    if (dur == null || dur.intValue() <= 0) {
                        continue;
                    }
                    if ("backup".equals(tagName)) {
                        cursorDiv = Math.max(0, cursorDiv - dur.intValue());
                    } else {
                        cursorDiv += dur.intValue();
                        measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
                    }
                    continue;
                }

                if ("sound".equals(tagName)) {
                    Double rawTempo = parseFiniteDouble(child.getAttribute("tempo"));
                    if (rawTempo != null && rawTempo.doubleValue() > 0) {
                        int eventDiv = Math.max(0, timelineDiv + cursorDiv);
                        int eventTick = Math.max(0,
                                Math.round((eventDiv / (float) Math.max(1, currentDivisions))
                                        * normalizedTicksPerQuarter));
                        int normalizedTempo = clampTempo(rawTempo.doubleValue());
                        if (normalizedTempo != currentTempo) {
                            events.add(new MidiTempoEvent(eventTick, normalizedTempo));
                            currentTempo = normalizedTempo;
                        }
                    }
                    continue;
                }

                if ("direction".equals(tagName)) {
                    Double soundTempo = directDirectionSoundTempo(child);
                    Double metronomeTempo = directionMetronomePerMinute(child);
                    Double rawTempo = soundTempo != null && soundTempo.doubleValue() > 0 ? soundTempo : metronomeTempo;
                    if (rawTempo != null && rawTempo.doubleValue() > 0) {
                        Integer offsetDiv = getFirstInteger(child, "offset");
                        int eventDiv = Math.max(0, timelineDiv + cursorDiv + (offsetDiv == null ? 0 : offsetDiv.intValue()));
                        int eventTick = Math.max(0,
                                Math.round((eventDiv / (float) Math.max(1, currentDivisions))
                                        * normalizedTicksPerQuarter));
                        int normalizedTempo = clampTempo(rawTempo.doubleValue());
                        if (normalizedTempo != currentTempo) {
                            events.add(new MidiTempoEvent(eventTick, normalizedTempo));
                            currentTempo = normalizedTempo;
                        }
                    }
                }

                if (!"note".equals(tagName)) {
                    continue;
                }
                Integer durationDiv = getFirstInteger(child, "duration");
                if (durationDiv == null || durationDiv.intValue() <= 0) {
                    continue;
                }
                String voice = trimToEmpty(firstDescendantTextByName(child, "voice"));
                if (voice.isEmpty()) {
                    voice = "1";
                }
                boolean isChord = firstDescendantElementByName(child, "chord") != null;
                Integer lastStart = lastStartByVoice.get(voice);
                int startDiv = isChord ? (lastStart == null ? cursorDiv : lastStart.intValue()) : cursorDiv;
                if (!isChord) {
                    lastStartByVoice.put(voice, Integer.valueOf(startDiv));
                    cursorDiv += durationDiv.intValue();
                }
                measureMaxDiv = Math.max(measureMaxDiv, Math.max(cursorDiv, startDiv + durationDiv.intValue()));
            }

            timelineDiv += resolveMeasureAdvanceDiv(measure, measureMaxDiv, currentDivisions, currentBeats,
                    currentBeatType, isImplicitMeasure(nextMeasure), firstUnderfullAsPickup);
        }

        Map<Integer, Integer> byTick = new LinkedHashMap<Integer, Integer>();
        for (MidiTempoEvent event : events) {
            byTick.put(Integer.valueOf(Math.max(0, Math.round(event.getTick()))),
                    Integer.valueOf(clampTempo(event.getBpm())));
        }
        List<Integer> sortedTicks = new ArrayList<Integer>(byTick.keySet());
        Collections.sort(sortedTicks);
        if (sortedTicks.isEmpty() || sortedTicks.get(0).intValue() != 0) {
            sortedTicks.add(0, Integer.valueOf(0));
            byTick.put(Integer.valueOf(0), Integer.valueOf(clampTempo(firstSoundTempoInDocument(doc, 120))));
        }
        List<MidiTempoEvent> out = new ArrayList<MidiTempoEvent>();
        for (Integer tick : sortedTicks) {
            Integer bpm = byTick.get(tick);
            out.add(new MidiTempoEvent(tick.intValue(), bpm == null ? 120 : bpm.intValue()));
        }
        return Collections.unmodifiableList(out);
    }

    public static int collectLeadingPickupTicksFromMusicXmlDoc(Document doc, int ticksPerQuarter) {
        int normalizedTicksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
        if (doc == null || doc.getDocumentElement() == null) {
            return 0;
        }
        Element root = doc.getDocumentElement();
        Element firstPart = "score-partwise".equals(root.getNodeName()) ? directChildElementByName(root, "part") : null;
        if (firstPart == null) {
            return 0;
        }
        List<Element> measures = directChildElementsByName(firstPart, "measure");
        if (measures.isEmpty()) {
            return 0;
        }
        Element firstMeasure = measures.get(0);
        Element secondMeasure = measures.size() > 1 ? measures.get(1) : null;
        boolean firstUnderfullAsPickup = shouldTreatFirstUnderfullAsPickup(doc);

        Integer divisions = getFirstIntegerByDirectPath(firstMeasure, "attributes", "divisions");
        Integer beats = getFirstIntegerByDirectPath(firstMeasure, "attributes", "time", "beats");
        Integer beatType = getFirstIntegerByDirectPath(firstMeasure, "attributes", "time", "beat-type");
        int currentDivisions = Math.max(1, divisions == null ? 1 : Math.round(divisions.intValue()));
        int currentBeats = Math.max(1, beats == null ? 4 : Math.round(beats.intValue()));
        int currentBeatType = Math.max(1, beatType == null ? 4 : Math.round(beatType.intValue()));

        int measureMaxDiv = estimateMeasureContentSpanDiv(firstMeasure);
        int advanceDiv = resolveMeasureAdvanceDiv(firstMeasure, measureMaxDiv, currentDivisions, currentBeats,
                currentBeatType, isImplicitMeasure(secondMeasure), firstUnderfullAsPickup);
        int fullMeasureDiv = Math.max(1,
                Math.round((currentDivisions * 4.0f * currentBeats) / currentBeatType));
        if (advanceDiv <= 0 || advanceDiv >= fullMeasureDiv) {
            return 0;
        }
        return Math.max(1, Math.round((advanceDiv / (float) currentDivisions) * normalizedTicksPerQuarter));
    }

    public static List<MidiTimeSignatureEvent> collectMidiTimeSignatureEventsFromMusicXmlDoc(Document doc,
            int ticksPerQuarter) {
        int normalizedTicksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
        boolean firstUnderfullAsPickup = shouldTreatFirstUnderfullAsPickup(doc);
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.singletonList(new MidiTimeSignatureEvent(0, 4, 4));
        }
        Element root = doc.getDocumentElement();
        Element firstPart = "score-partwise".equals(root.getNodeName()) ? directChildElementByName(root, "part") : null;
        if (firstPart == null) {
            return Collections.singletonList(new MidiTimeSignatureEvent(0, 4, 4));
        }

        int currentDivisions = 1;
        int tickCursor = 0;
        int currentBeats = 4;
        int currentBeatType = 4;
        List<MidiTimeSignatureEvent> events = new ArrayList<MidiTimeSignatureEvent>();
        events.add(new MidiTimeSignatureEvent(0, currentBeats, currentBeatType));

        List<Element> measures = directChildElementsByName(firstPart, "measure");
        for (int measureIndex = 0; measureIndex < measures.size(); measureIndex++) {
            Element measure = measures.get(measureIndex);
            Element nextMeasure = measureIndex + 1 < measures.size() ? measures.get(measureIndex + 1) : null;
            Integer divisions = getFirstIntegerByDirectPath(measure, "attributes", "divisions");
            if (divisions != null && divisions.intValue() > 0) {
                currentDivisions = divisions.intValue();
            }

            Integer beats = getFirstIntegerByDirectPath(measure, "attributes", "time", "beats");
            Integer beatType = getFirstIntegerByDirectPath(measure, "attributes", "time", "beat-type");
            if (beats != null && beatType != null
                    && (Math.round(beats.intValue()) != currentBeats
                            || Math.round(beatType.intValue()) != currentBeatType)) {
                currentBeats = Math.max(1, Math.round(beats.intValue()));
                currentBeatType = Math.max(1, Math.round(beatType.intValue()));
                events.add(new MidiTimeSignatureEvent(tickCursor, currentBeats, currentBeatType));
            }

            int cursorDiv = 0;
            int measureMaxDiv = 0;
            for (Element child : directElementChildren(measure)) {
                String tagName = child.getTagName();
                if ("backup".equals(tagName) || "forward".equals(tagName)) {
                    Integer dur = getFirstInteger(child, "duration");
                    if (dur == null || dur.intValue() <= 0) {
                        continue;
                    }
                    if ("backup".equals(tagName)) {
                        cursorDiv = Math.max(0, cursorDiv - dur.intValue());
                    } else {
                        cursorDiv += dur.intValue();
                        measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
                    }
                    continue;
                }
                if (!"note".equals(tagName)) {
                    continue;
                }
                Integer durationDiv = getFirstInteger(child, "duration");
                if (durationDiv == null || durationDiv.intValue() <= 0) {
                    continue;
                }
                if (firstDescendantElementByName(child, "chord") == null) {
                    cursorDiv += durationDiv.intValue();
                }
                measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
            }
            int advanceDiv = resolveMeasureAdvanceDiv(measure, measureMaxDiv, currentDivisions, currentBeats,
                    currentBeatType, isImplicitMeasure(nextMeasure), firstUnderfullAsPickup);
            tickCursor += Math.max(1,
                    Math.round((advanceDiv / (float) Math.max(1, currentDivisions)) * normalizedTicksPerQuarter));
        }

        Map<Integer, MidiTimeSignatureEvent> byTick = new LinkedHashMap<Integer, MidiTimeSignatureEvent>();
        for (MidiTimeSignatureEvent event : events) {
            int tick = Math.max(0, Math.round(event.getStartTicks()));
            byTick.put(Integer.valueOf(tick), new MidiTimeSignatureEvent(tick, event.getBeats(), event.getBeatType()));
        }
        List<Integer> sortedTicks = new ArrayList<Integer>(byTick.keySet());
        Collections.sort(sortedTicks);
        List<MidiTimeSignatureEvent> out = new ArrayList<MidiTimeSignatureEvent>();
        for (Integer tick : sortedTicks) {
            MidiTimeSignatureEvent event = byTick.get(tick);
            out.add(event == null ? new MidiTimeSignatureEvent(tick.intValue(), 4, 4) : event);
        }
        return Collections.unmodifiableList(out);
    }

    public static List<MidiKeySignatureEvent> collectMidiKeySignatureEventsFromMusicXmlDoc(Document doc,
            int ticksPerQuarter) {
        int normalizedTicksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
        boolean firstUnderfullAsPickup = shouldTreatFirstUnderfullAsPickup(doc);
        if (doc == null || doc.getDocumentElement() == null) {
            return Collections.singletonList(new MidiKeySignatureEvent(0, 0, "major"));
        }
        Element root = doc.getDocumentElement();
        Element firstPart = "score-partwise".equals(root.getNodeName()) ? directChildElementByName(root, "part") : null;
        if (firstPart == null) {
            return Collections.singletonList(new MidiKeySignatureEvent(0, 0, "major"));
        }

        int currentDivisions = 1;
        int tickCursor = 0;
        int currentFifths = 0;
        String currentMode = "major";
        List<MidiKeySignatureEvent> events = new ArrayList<MidiKeySignatureEvent>();
        events.add(new MidiKeySignatureEvent(0, currentFifths, currentMode));
        boolean hasInitialKey = false;

        List<Element> measures = directChildElementsByName(firstPart, "measure");
        for (int measureIndex = 0; measureIndex < measures.size(); measureIndex++) {
            Element measure = measures.get(measureIndex);
            Element nextMeasure = measureIndex + 1 < measures.size() ? measures.get(measureIndex + 1) : null;
            Integer divisions = getFirstIntegerByDirectPath(measure, "attributes", "divisions");
            if (divisions != null && divisions.intValue() > 0) {
                currentDivisions = divisions.intValue();
            }

            Integer fifths = getFirstIntegerByDirectPath(measure, "attributes", "key", "fifths");
            String modeText = trimToEmpty(getFirstTextByDirectPath(measure, "attributes", "key", "mode")).toLowerCase();
            String mode = "minor".equals(modeText) ? "minor" : "major";
            if (fifths != null) {
                int roundedFifths = Math.max(-7, Math.min(7, Math.round(fifths.intValue())));
                if (!hasInitialKey || roundedFifths != currentFifths || !mode.equals(currentMode)) {
                    if (!hasInitialKey) {
                        events.set(0, new MidiKeySignatureEvent(0, roundedFifths, mode));
                        hasInitialKey = true;
                    } else {
                        events.add(new MidiKeySignatureEvent(tickCursor, roundedFifths, mode));
                    }
                    currentFifths = roundedFifths;
                    currentMode = mode;
                }
            }

            int cursorDiv = 0;
            int measureMaxDiv = 0;
            for (Element child : directElementChildren(measure)) {
                String tagName = child.getTagName();
                if ("backup".equals(tagName) || "forward".equals(tagName)) {
                    Integer dur = getFirstInteger(child, "duration");
                    if (dur == null || dur.intValue() <= 0) {
                        continue;
                    }
                    if ("backup".equals(tagName)) {
                        cursorDiv = Math.max(0, cursorDiv - dur.intValue());
                    } else {
                        cursorDiv += dur.intValue();
                        measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
                    }
                    continue;
                }
                if (!"note".equals(tagName)) {
                    continue;
                }
                Integer durationDiv = getFirstInteger(child, "duration");
                if (durationDiv == null || durationDiv.intValue() <= 0) {
                    continue;
                }
                if (firstDescendantElementByName(child, "chord") == null) {
                    cursorDiv += durationDiv.intValue();
                }
                measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
            }
            Integer beats = getFirstIntegerByDirectPath(measure, "attributes", "time", "beats");
            Integer beatType = getFirstIntegerByDirectPath(measure, "attributes", "time", "beat-type");
            int advanceDiv = resolveMeasureAdvanceDiv(measure, measureMaxDiv, currentDivisions,
                    beats == null ? 4 : beats.intValue(), beatType == null ? 4 : beatType.intValue(),
                    isImplicitMeasure(nextMeasure), firstUnderfullAsPickup);
            tickCursor += Math.max(1,
                    Math.round((advanceDiv / (float) Math.max(1, currentDivisions)) * normalizedTicksPerQuarter));
        }

        Map<Integer, MidiKeySignatureEvent> byTick = new LinkedHashMap<Integer, MidiKeySignatureEvent>();
        for (MidiKeySignatureEvent event : events) {
            int tick = Math.max(0, Math.round(event.getStartTicks()));
            byTick.put(Integer.valueOf(tick), new MidiKeySignatureEvent(tick, event.getFifths(), event.getMode()));
        }
        List<Integer> sortedTicks = new ArrayList<Integer>(byTick.keySet());
        Collections.sort(sortedTicks);
        List<MidiKeySignatureEvent> out = new ArrayList<MidiKeySignatureEvent>();
        for (Integer tick : sortedTicks) {
            MidiKeySignatureEvent event = byTick.get(tick);
            out.add(event == null ? new MidiKeySignatureEvent(tick.intValue(), 0, "major") : event);
        }
        return Collections.unmodifiableList(out);
    }

    public static MidiPlaybackEventsResult buildPlaybackEventsFromMusicXmlDoc(Document doc, int ticksPerQuarter) {
        return buildPlaybackEventsFromMusicXmlDoc(doc, ticksPerQuarter, null);
    }

    public static MidiPlaybackEventsResult buildPlaybackEventsFromMusicXmlDoc(Document doc, int ticksPerQuarter,
            MidiPlaybackExtractionOptions options) {
        MidiPlaybackExtractionOptions safeOptions = options == null ? new MidiPlaybackExtractionOptions() : options;
        int normalizedTicksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
        if (doc == null || doc.getDocumentElement() == null) {
            return new MidiPlaybackEventsResult(120, Collections.<RawMidiPlaybackEvent>emptyList());
        }
        Element root = doc.getDocumentElement();
        List<Element> partNodes = "score-partwise".equals(root.getNodeName()) ? directChildElementsByName(root, "part")
                : Collections.<Element>emptyList();
        if (partNodes.isEmpty()) {
            return new MidiPlaybackEventsResult(120, Collections.<RawMidiPlaybackEvent>emptyList());
        }

        Map<String, Integer> channelMap = new LinkedHashMap<String, Integer>();
        Map<String, String> partNameById = new LinkedHashMap<String, String>();
        Element partList = directChildElementByName(root, "part-list");
        if (partList != null) {
            for (Element scorePart : directChildElementsByName(partList, "score-part")) {
                String partId = scorePart.getAttribute("id") == null ? "" : scorePart.getAttribute("id");
                if (partId.isEmpty()) {
                    continue;
                }
                for (Element midiInstrument : directChildElementsByName(scorePart, "midi-instrument")) {
                    Integer midiChannel = parseLeadingBase10Integer(directChildText(midiInstrument, "midi-channel"));
                    if (midiChannel != null && midiChannel.intValue() >= 1 && midiChannel.intValue() <= 16) {
                        channelMap.put(partId, midiChannel);
                        break;
                    }
                }
                String rawName = trimToEmpty(directChildText(scorePart, "part-name"));
                partNameById.put(partId, rawName.isEmpty() ? partId : rawName);
            }
        }

        int tempo = clampTempo(firstSoundTempoInDocument(doc, 120));
        boolean firstUnderfullAsPickup = shouldTreatFirstUnderfullAsPickup(doc);
        Map<String, DrumPartMap> drumPartMapByPartId = buildDrumPartMapByPartId(doc);
        List<RawMidiPlaybackEvent> events = new ArrayList<RawMidiPlaybackEvent>();
        for (int partIndex = 0; partIndex < partNodes.size(); partIndex++) {
            Element part = partNodes.get(partIndex);
            String partId = part.getAttribute("id") == null ? "" : part.getAttribute("id");
            int channelCandidate = (partIndex % 16) + 1;
            int fallbackChannel = channelCandidate == 10 ? 11 : channelCandidate;
            Integer mappedChannel = channelMap.get(partId);
            int channel = mappedChannel == null ? fallbackChannel : mappedChannel.intValue();
            String trackId = partId.isEmpty() ? "part-" + (partIndex + 1) : partId;
            String mappedName = partNameById.get(partId);
            String trackName = mappedName == null || mappedName.length() == 0 ? trackId : mappedName;
            DrumPartMap drumPartMap = drumPartMapByPartId.get(partId);

            int currentDivisions = 1;
            int currentBeats = 4;
            int currentBeatType = 4;
            int currentFifths = 0;
            int currentTransposeSemitones = 0;
            int currentVelocity = 80;
            int timelineDiv = 0;
            Map<String, Integer> tieChainIndexByKey = new LinkedHashMap<String, Integer>();
            Map<String, Integer> lastStartByVoice = new LinkedHashMap<String, Integer>();
            Map<String, Integer> voiceTimeShiftTicks = new LinkedHashMap<String, Integer>();
            Map<String, String> activeWedgeByNumber = new LinkedHashMap<String, String>();
            Map<String, List<PendingGracePlaybackNote>> pendingGraceByVoice =
                    new LinkedHashMap<String, List<PendingGracePlaybackNote>>();
            Map<String, Set<String>> activeSlurByVoice = new LinkedHashMap<String, Set<String>>();
            Map<String, Integer> lastEventIndexByVoiceChannelPitch = new LinkedHashMap<String, Integer>();
            Map<String, Boolean> lastEventAllowsRepeatedSlurMergeByVoiceChannelPitch =
                    new LinkedHashMap<String, Boolean>();
            List<Element> measures = directChildElementsByName(part, "measure");
            for (int measureIndex = 0; measureIndex < measures.size(); measureIndex++) {
                Element measure = measures.get(measureIndex);
                Element nextMeasure = measureIndex + 1 < measures.size() ? measures.get(measureIndex + 1) : null;
                Integer divisions = getFirstIntegerByDirectPath(measure, "attributes", "divisions");
                if (divisions != null && divisions.intValue() > 0) {
                    currentDivisions = divisions.intValue();
                }
                Integer beats = getFirstIntegerByDirectPath(measure, "attributes", "time", "beats");
                Integer beatType = getFirstIntegerByDirectPath(measure, "attributes", "time", "beat-type");
                if (beats != null && beats.intValue() > 0 && beatType != null && beatType.intValue() > 0) {
                    currentBeats = beats.intValue();
                    currentBeatType = beatType.intValue();
                }
                Integer fifths = getFirstIntegerByDirectPath(measure, "attributes", "key", "fifths");
                if (fifths != null) {
                    currentFifths = Math.max(-7, Math.min(7, fifths.intValue()));
                }
                Integer chromatic = getFirstIntegerByDirectPath(measure, "attributes", "transpose", "chromatic");
                Integer octaveChange = getFirstIntegerByDirectPath(measure, "attributes", "transpose",
                        "octave-change");
                if (chromatic != null || octaveChange != null) {
                    currentTransposeSemitones = (chromatic == null ? 0 : chromatic.intValue())
                            + (octaveChange == null ? 0 : octaveChange.intValue()) * 12;
                }

                int cursorDiv = 0;
                int measureMaxDiv = 0;
                Map<String, Integer> measureAccidentalByStepOctave = new LinkedHashMap<String, Integer>();
                for (Element child : directElementChildren(measure)) {
                    String tagName = child.getTagName();
                    if ("backup".equals(tagName) || "forward".equals(tagName)) {
                        Integer dur = getFirstInteger(child, "duration");
                        if (dur == null || dur.intValue() <= 0) {
                            continue;
                        }
                        if ("backup".equals(tagName)) {
                            cursorDiv = Math.max(0, cursorDiv - dur.intValue());
                        } else {
                            cursorDiv += dur.intValue();
                            measureMaxDiv = Math.max(measureMaxDiv, cursorDiv);
                        }
                        continue;
                    }
                    if (!"note".equals(tagName)) {
                        if (safeOptions.isMidiMode() && "direction".equals(tagName)) {
                            currentVelocity = readDirectionVelocity(child, currentVelocity);
                            WedgeDirective wedgeDirective = readDirectionWedgeDirective(child);
                            for (String wedgeNumber : wedgeDirective.getStops()) {
                                activeWedgeByNumber.remove(wedgeNumber);
                            }
                            for (WedgeStart start : wedgeDirective.getStarts()) {
                                activeWedgeByNumber.put(start.getNumber(), start.getKind());
                            }
                        }
                        continue;
                    }
                    boolean isGrace = firstDescendantElementByName(child, "grace") != null;
                    boolean isChord = firstDescendantElementByName(child, "chord") != null;
                    boolean isRest = firstDescendantElementByName(child, "rest") != null;
                    Integer durationDiv = getFirstInteger(child, "duration");
                    if (!isGrace && (durationDiv == null || durationDiv.intValue() <= 0)) {
                        continue;
                    }
                    String voice = trimToEmpty(firstDescendantTextByName(child, "voice"));
                    if (voice.isEmpty()) {
                        voice = "1";
                    }
                    Integer lastStart = lastStartByVoice.get(voice);
                    int startDiv = isChord ? (lastStart == null ? cursorDiv : lastStart.intValue()) : cursorDiv;
                    if (!isChord) {
                        lastStartByVoice.put(voice, Integer.valueOf(startDiv));
                    }
                    if (!isRest && (safeOptions.includeGraceProcessing() || !isGrace)) {
                        Integer soundingMidi = resolveMusicXmlNoteMidiForPlayback(child, channel, currentFifths,
                                currentTransposeSemitones, drumPartMap, measureAccidentalByStepOctave);
                        if (soundingMidi != null && soundingMidi.intValue() >= 0 && soundingMidi.intValue() <= 127) {
                            NoteArticulationAdjustments articulation = safeOptions.isMidiMode()
                                    ? getNoteArticulationAdjustments(child)
                                    : NoteArticulationAdjustments.NONE;
                            boolean hasAnyExplicitArticulation = hasExplicitArticulation(child);
                            boolean allowsRepeatedSlurMergeForCurrent = !hasAnyExplicitArticulation
                                    && articulation.getDurationRatio() >= 1.0d && !articulation.hasTenuto()
                                    && articulation.getVelocityDelta() == 0;
                            int metricAccentDelta = safeOptions.isMidiMode() && safeOptions.isMetricAccentEnabled()
                                    ? getMetricAccentVelocityDelta(startDiv, currentDivisions, currentBeats,
                                            currentBeatType, safeOptions.getMetricAccentProfile())
                                    : 0;
                            int velocity = clampVelocity(currentVelocity + articulation.getVelocityDelta()
                                    + metricAccentDelta);
                            if (safeOptions.includeGraceProcessing() && isGrace) {
                                Element graceNode = firstDescendantElementByName(child, "grace");
                                boolean hasSlash = graceNode != null
                                        && ("yes".equals(trimToEmpty(graceNode.getAttribute("slash")).toLowerCase())
                                                || directChildElementByName(graceNode, "slash") != null);
                                List<PendingGracePlaybackNote> pending = pendingGraceByVoice.get(voice);
                                if (pending == null) {
                                    pending = new ArrayList<PendingGracePlaybackNote>();
                                    pendingGraceByVoice.put(voice, pending);
                                }
                                pending.add(new PendingGracePlaybackNote(soundingMidi.intValue(), velocity,
                                        hasSlash ? 1 : 2));
                                continue;
                            }
                            int voiceShiftTicks = safeOptions.isMidiMode()
                                    ? (voiceTimeShiftTicks.containsKey(voice)
                                            ? voiceTimeShiftTicks.get(voice).intValue()
                                            : 0)
                                    : 0;
                            int startTicks = Math.max(0, Math.round(
                                    ((timelineDiv + startDiv) / (float) currentDivisions)
                                            * normalizedTicksPerQuarter)
                                    + voiceShiftTicks);
                            int durTicks = Math.max(1,
                                    Math.round((durationDiv.intValue() / (float) currentDivisions)
                                            * normalizedTicksPerQuarter));
                            SlurNumbers slurNumbers = safeOptions.includeSlurProcessing() ? getSlurNumbers(child)
                                    : SlurNumbers.NONE;
                            Set<String> activeSlurSet = activeSlurByVoice.get(voice);
                            int activeSlurCount = activeSlurSet == null ? 0 : activeSlurSet.size();
                            boolean noteUnderSlur = safeOptions.includeSlurProcessing()
                                    && (activeSlurCount > 0 || !slurNumbers.getStarts().isEmpty()
                                            || !slurNumbers.getStops().isEmpty());
                            boolean hasForwardSlurConnection = safeOptions.includeSlurProcessing()
                                    && (!slurNumbers.getStarts().isEmpty()
                                            || activeSlurCount > slurNumbers.getStops().size());
                            boolean isInsideOngoingSlurOnly = safeOptions.includeSlurProcessing() && activeSlurCount > 0
                                    && slurNumbers.getStarts().isEmpty() && slurNumbers.getStops().isEmpty();
                            TemporalExpressionAdjustments temporalAdjustments = safeOptions.isMidiMode() && !isGrace
                                    ? getTemporalExpressionAdjustments(child, durTicks, normalizedTicksPerQuarter)
                                    : TemporalExpressionAdjustments.NONE;
                            int legatoOverlapTicks = safeOptions.isMidiMode() && !isChord
                                    && (hasForwardSlurConnection || articulation.hasTenuto())
                                    ? Math.max(1, Math.round(normalizedTicksPerQuarter / 32.0f))
                                    : 0;
                            TieFlags tieFlags = safeOptions.includeTieProcessing() ? getTieFlags(child) : TieFlags.NONE;
                            boolean shouldApplyDefaultDetache = safeOptions.applyDefaultDetache() && !isGrace
                                    && !isChord && articulation.getDurationRatio() >= 1.0d && !articulation.hasTenuto()
                                    && !tieFlags.isStart() && !tieFlags.isStop() && !noteUnderSlur;
                            double effectiveDurationRatio = shouldApplyDefaultDetache ? DEFAULT_DETACHE_DURATION_RATIO
                                    : articulation.getDurationRatio();
                            int eventStartTicks = startTicks;
                            int eventDurTicks = Math.max(1,
                                    Math.round(durTicks * (float) effectiveDurationRatio)
                                            + legatoOverlapTicks
                                            + temporalAdjustments.getDurationExtraTicks());
                            List<PendingGracePlaybackNote> pendingGrace = safeOptions.includeGraceProcessing()
                                    ? pendingGraceByVoice.get(voice)
                                    : null;
                            if (pendingGrace != null && !pendingGrace.isEmpty()) {
                                int maxLeadByPrincipal = Math.max(pendingGrace.size(), Math.round(durTicks * 0.45f));
                                int maxLeadByTempo = Math.max(pendingGrace.size(),
                                        Math.round(normalizedTicksPerQuarter / 2.0f));
                                int totalGraceTicks = Math.max(pendingGrace.size(),
                                        Math.min(maxLeadByPrincipal, maxLeadByTempo));
                                List<Integer> graceDurations;
                                if ("classical_equal".equals(safeOptions.getGraceTimingMode())) {
                                    graceDurations = splitTicks(Math.max(durTicks, pendingGrace.size() + 1),
                                            pendingGrace.size() + 1).subList(0, pendingGrace.size());
                                } else {
                                    List<Double> weights = new ArrayList<Double>();
                                    for (PendingGracePlaybackNote grace : pendingGrace) {
                                        weights.add(Double.valueOf(grace.getWeight()));
                                    }
                                    graceDurations = splitTicksWeighted(totalGraceTicks, weights);
                                }
                                int graceTick = "before_beat".equals(safeOptions.getGraceTimingMode())
                                        ? Math.max(0, startTicks - totalGraceTicks)
                                        : startTicks;
                                for (int graceIndex = 0; graceIndex < pendingGrace.size(); graceIndex++) {
                                    PendingGracePlaybackNote grace = pendingGrace.get(graceIndex);
                                    int graceDur = Math.max(1, graceDurations.get(graceIndex).intValue());
                                    events.add(new RawMidiPlaybackEvent(grace.getMidiNumber(), graceTick, graceDur,
                                            channel, grace.getVelocity(), trackId, trackName));
                                    graceTick += graceDur;
                                }
                                if ("before_beat".equals(safeOptions.getGraceTimingMode())) {
                                    eventStartTicks = Math.max(eventStartTicks, graceTick);
                                } else if ("on_beat".equals(safeOptions.getGraceTimingMode())) {
                                    eventStartTicks = graceTick;
                                    eventDurTicks = Math.max(1, durTicks - (graceTick - startTicks));
                                } else {
                                    List<Integer> equalDurations = splitTicks(
                                            Math.max(durTicks, pendingGrace.size() + 1), pendingGrace.size() + 1);
                                    eventStartTicks = graceTick;
                                    eventDurTicks = Math.max(1, equalDurations.get(pendingGrace.size()).intValue());
                                }
                                pendingGraceByVoice.remove(voice);
                            }
                            boolean canExpandOrnament = safeOptions.includeOrnamentExpansion()
                                    && !isMusicXmlDrumPlaybackContext(child, channel, drumPartMap)
                                    && !tieFlags.isStart() && !tieFlags.isStop();
                            List<Integer> ornamentMidiSequence = canExpandOrnament
                                    ? buildOrnamentMidiSequence(child, soundingMidi.intValue(), eventDurTicks,
                                            normalizedTicksPerQuarter, currentFifths, measureAccidentalByStepOctave)
                                    : Collections.singletonList(Integer.valueOf(soundingMidi.intValue()));
                            List<Integer> ornamentDurations = splitTicks(eventDurTicks, ornamentMidiSequence.size());
                            List<RawMidiPlaybackEvent> generatedEvents = new ArrayList<RawMidiPlaybackEvent>();
                            int ornamentStartTicks = eventStartTicks;
                            for (int ornamentIndex = 0; ornamentIndex < ornamentMidiSequence.size(); ornamentIndex++) {
                                int ornamentDurTicks = Math.max(1, ornamentDurations.get(ornamentIndex).intValue());
                                generatedEvents.add(new RawMidiPlaybackEvent(
                                        ornamentMidiSequence.get(ornamentIndex).intValue(), ornamentStartTicks,
                                        ornamentDurTicks, channel, velocity, trackId, trackName));
                                ornamentStartTicks += ornamentDurTicks;
                            }
                            RawMidiPlaybackEvent primaryEvent = generatedEvents.isEmpty() ? null : generatedEvents.get(0);
                            if (primaryEvent == null) {
                                continue;
                            }
                            String tieKey = voice + "|" + channel + "|" + soundingMidi.intValue();
                            String voiceChannelPitchKey = tieKey;
                            Integer priorSamePitchIndex = lastEventIndexByVoiceChannelPitch.get(voiceChannelPitchKey);
                            RawMidiPlaybackEvent priorSamePitchEvent = priorSamePitchIndex == null
                                    || priorSamePitchIndex.intValue() < 0
                                    || priorSamePitchIndex.intValue() >= events.size()
                                            ? null
                                            : events.get(priorSamePitchIndex.intValue());
                            boolean shouldMergeRepeatedSlurSamePitch = safeOptions.isMidiMode() && !isChord
                                    && !isGrace && !tieFlags.isStart() && !tieFlags.isStop()
                                    && isInsideOngoingSlurOnly && priorSamePitchEvent != null
                                    && generatedEvents.size() == 1 && allowsRepeatedSlurMergeForCurrent
                                    && Boolean.TRUE.equals(
                                            lastEventAllowsRepeatedSlurMergeByVoiceChannelPitch.get(
                                                    voiceChannelPitchKey))
                                    && priorSamePitchEvent.getStartTicks() < startTicks
                                    && priorSamePitchEvent.getStartTicks() + priorSamePitchEvent.getDurTicks()
                                            >= startTicks;
                            if (safeOptions.isMidiMode()) {
                                for (String wedgeKind : activeWedgeByNumber.values()) {
                                    currentVelocity = clampVelocity(currentVelocity
                                            + ("crescendo".equals(wedgeKind) ? 4 : -4));
                                }
                            }
                            if (shouldMergeRepeatedSlurSamePitch && priorSamePitchIndex != null) {
                                int priorEndTick = priorSamePitchEvent.getStartTicks()
                                        + priorSamePitchEvent.getDurTicks();
                                int currentEndTick = primaryEvent.getStartTicks() + primaryEvent.getDurTicks();
                                RawMidiPlaybackEvent merged = new RawMidiPlaybackEvent(
                                        priorSamePitchEvent.getMidiNumber(), priorSamePitchEvent.getStartTicks(),
                                        Math.max(1, Math.max(priorEndTick, currentEndTick)
                                                - priorSamePitchEvent.getStartTicks()),
                                        priorSamePitchEvent.getChannel(),
                                        Math.max(priorSamePitchEvent.getVelocity(), velocity),
                                        priorSamePitchEvent.getTrackId(), priorSamePitchEvent.getTrackName());
                                events.set(priorSamePitchIndex.intValue(), merged);
                                lastEventIndexByVoiceChannelPitch.put(voiceChannelPitchKey, priorSamePitchIndex);
                                lastEventAllowsRepeatedSlurMergeByVoiceChannelPitch.put(voiceChannelPitchKey,
                                        Boolean.valueOf(allowsRepeatedSlurMergeForCurrent));
                            } else if (safeOptions.includeTieProcessing() && tieFlags.isStop()) {
                                String chainedKey = resolveFallbackTieChainKey(tieChainIndexByKey, voice, channel,
                                        soundingMidi.intValue());
                                Integer chainedIndex = chainedKey == null ? null : tieChainIndexByKey.get(chainedKey);
                                if (chainedIndex != null && chainedIndex.intValue() >= 0
                                        && chainedIndex.intValue() < events.size()) {
                                    RawMidiPlaybackEvent chained = events.get(chainedIndex.intValue());
                                    RawMidiPlaybackEvent extended = new RawMidiPlaybackEvent(chained.getMidiNumber(),
                                            chained.getStartTicks(), chained.getDurTicks() + primaryEvent.getDurTicks(),
                                            chained.getChannel(), Math.max(chained.getVelocity(), velocity),
                                            chained.getTrackId(), chained.getTrackName());
                                    events.set(chainedIndex.intValue(), extended);
                                    lastEventIndexByVoiceChannelPitch.put(chainedKey, chainedIndex);
                                    lastEventAllowsRepeatedSlurMergeByVoiceChannelPitch.put(chainedKey,
                                            Boolean.valueOf(allowsRepeatedSlurMergeForCurrent));
                                    if (tieFlags.isStart()) {
                                        tieChainIndexByKey.put(chainedKey, chainedIndex);
                                    } else {
                                        tieChainIndexByKey.remove(chainedKey);
                                    }
                                } else {
                                    events.add(primaryEvent);
                                    int addedIndex = events.size() - 1;
                                    lastEventIndexByVoiceChannelPitch.put(tieKey, Integer.valueOf(addedIndex));
                                    lastEventAllowsRepeatedSlurMergeByVoiceChannelPitch.put(tieKey,
                                            Boolean.valueOf(allowsRepeatedSlurMergeForCurrent));
                                    if (tieFlags.isStart()) {
                                        tieChainIndexByKey.put(tieKey, Integer.valueOf(addedIndex));
                                    }
                                }
                            } else {
                                int firstAddedIndex = events.size();
                                events.addAll(generatedEvents);
                                for (int generatedIndex = 0; generatedIndex < generatedEvents.size(); generatedIndex++) {
                                    RawMidiPlaybackEvent generated = generatedEvents.get(generatedIndex);
                                    String generatedKey = voice + "|" + channel + "|" + generated.getMidiNumber();
                                    lastEventIndexByVoiceChannelPitch.put(generatedKey,
                                            Integer.valueOf(firstAddedIndex + generatedIndex));
                                    lastEventAllowsRepeatedSlurMergeByVoiceChannelPitch.put(generatedKey,
                                            Boolean.valueOf(allowsRepeatedSlurMergeForCurrent));
                                }
                                if (safeOptions.includeTieProcessing() && tieFlags.isStart()) {
                                    tieChainIndexByKey.put(tieKey, Integer.valueOf(firstAddedIndex));
                                } else if (safeOptions.includeTieProcessing()) {
                                    tieChainIndexByKey.remove(tieKey);
                                }
                            }
                            if (safeOptions.includeSlurProcessing()) {
                                Set<String> nextSlurSet = activeSlurSet == null ? new LinkedHashSet<String>()
                                        : new LinkedHashSet<String>(activeSlurSet);
                                nextSlurSet.addAll(slurNumbers.getStarts());
                                nextSlurSet.removeAll(slurNumbers.getStops());
                                if (nextSlurSet.isEmpty()) {
                                    activeSlurByVoice.remove(voice);
                                } else {
                                    activeSlurByVoice.put(voice, nextSlurSet);
                                }
                            }
                            if (safeOptions.isMidiMode() && !isChord
                                    && temporalAdjustments.getPostPauseTicks() > 0) {
                                voiceTimeShiftTicks.put(voice, Integer.valueOf(voiceShiftTicks
                                        + temporalAdjustments.getPostPauseTicks()));
                            }
                        }
                    }
                    if (!isChord && !isGrace && durationDiv != null) {
                        cursorDiv += durationDiv.intValue();
                    }
                    if (!isGrace && durationDiv != null) {
                        measureMaxDiv = Math.max(measureMaxDiv, Math.max(cursorDiv, startDiv + durationDiv.intValue()));
                    }
                }
                timelineDiv += resolveMeasureAdvanceDiv(measure, measureMaxDiv, currentDivisions, currentBeats,
                        currentBeatType, isImplicitMeasure(nextMeasure), firstUnderfullAsPickup);
            }
        }
        return new MidiPlaybackEventsResult(tempo, events);
    }

    public static MidiPlaybackEventsResult buildPlaybackEventsFromXml(String xml, int ticksPerQuarter) {
        return buildPlaybackEventsFromXml(xml, ticksPerQuarter, null);
    }

    public static MidiPlaybackEventsResult buildPlaybackEventsFromXml(String xml, int ticksPerQuarter,
            MidiPlaybackExtractionOptions options) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml == null ? "" : xml);
        if (doc == null || doc.getDocumentElement() == null) {
            return new MidiPlaybackEventsResult(120, Collections.<RawMidiPlaybackEvent>emptyList());
        }
        return buildPlaybackEventsFromMusicXmlDoc(doc, ticksPerQuarter, options);
    }

    public static List<MidiKeySignatureEvent> normalizeMidiExportKeySignatureEvents(
            List<MidiKeySignatureEvent> keySignatureEvents) {
        List<MidiKeySignatureEvent> sorted = new ArrayList<MidiKeySignatureEvent>();
        if (keySignatureEvents != null) {
            for (MidiKeySignatureEvent event : keySignatureEvents) {
                if (event == null) {
                    continue;
                }
                sorted.add(new MidiKeySignatureEvent(Math.max(0, Math.round(event.getStartTicks())),
                        Math.max(-7, Math.min(7, Math.round(event.getFifths()))),
                        "minor".equals(event.getMode()) ? "minor" : "major"));
            }
        }
        Collections.sort(sorted, new Comparator<MidiKeySignatureEvent>() {
            @Override
            public int compare(MidiKeySignatureEvent left, MidiKeySignatureEvent right) {
                return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
            }
        });

        List<MidiKeySignatureEvent> deduped = new ArrayList<MidiKeySignatureEvent>();
        for (MidiKeySignatureEvent event : sorted) {
            if (!deduped.isEmpty() && deduped.get(deduped.size() - 1).getStartTicks() == event.getStartTicks()) {
                deduped.set(deduped.size() - 1,
                        new MidiKeySignatureEvent(event.getStartTicks(), event.getFifths(), event.getMode()));
                continue;
            }
            deduped.add(new MidiKeySignatureEvent(event.getStartTicks(), event.getFifths(), event.getMode()));
        }
        if (deduped.isEmpty() || deduped.get(0).getStartTicks() != 0) {
            deduped.add(0, new MidiKeySignatureEvent(0, 0, "major"));
        }
        return Collections.unmodifiableList(deduped);
    }

    public static List<MidiTempoEvent> normalizeMidiExportTempoEvents(List<MidiTempoEvent> tempoEvents,
            int defaultTempo) {
        List<MidiTempoEvent> source = new ArrayList<MidiTempoEvent>();
        if (tempoEvents == null || tempoEvents.isEmpty()) {
            source.add(new MidiTempoEvent(0, defaultTempo));
        } else {
            for (MidiTempoEvent event : tempoEvents) {
                if (event == null) {
                    continue;
                }
                source.add(new MidiTempoEvent(Math.max(0, Math.round(event.getTick())), clampTempo(event.getBpm())));
            }
        }
        Collections.sort(source, new Comparator<MidiTempoEvent>() {
            @Override
            public int compare(MidiTempoEvent left, MidiTempoEvent right) {
                return Integer.valueOf(left.getTick()).compareTo(Integer.valueOf(right.getTick()));
            }
        });

        List<MidiTempoEvent> deduped = new ArrayList<MidiTempoEvent>();
        for (MidiTempoEvent event : source) {
            if (!deduped.isEmpty() && deduped.get(deduped.size() - 1).getTick() == event.getTick()) {
                deduped.set(deduped.size() - 1, new MidiTempoEvent(event.getTick(), event.getBpm()));
                continue;
            }
            deduped.add(new MidiTempoEvent(event.getTick(), event.getBpm()));
        }
        if (deduped.isEmpty() || deduped.get(0).getTick() != 0) {
            deduped.add(0, new MidiTempoEvent(0, clampTempo(defaultTempo)));
        }
        return Collections.unmodifiableList(deduped);
    }

    public static List<MidiTimeSignatureEvent> normalizeMidiExportTimeSignatureEvents(
            List<MidiTimeSignatureEvent> timeSignatureEvents, int ticksPerQuarter, int pickupTicks) {
        List<MidiTimeSignatureEvent> sorted = new ArrayList<MidiTimeSignatureEvent>();
        if (timeSignatureEvents != null) {
            for (MidiTimeSignatureEvent event : timeSignatureEvents) {
                if (event == null) {
                    continue;
                }
                sorted.add(new MidiTimeSignatureEvent(Math.max(0, Math.round(event.getStartTicks())),
                        Math.max(1, Math.round(event.getBeats())),
                        Math.max(1, Math.round(event.getBeatType()))));
            }
        }
        Collections.sort(sorted, new Comparator<MidiTimeSignatureEvent>() {
            @Override
            public int compare(MidiTimeSignatureEvent left, MidiTimeSignatureEvent right) {
                return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
            }
        });

        List<MidiTimeSignatureEvent> deduped = new ArrayList<MidiTimeSignatureEvent>();
        for (MidiTimeSignatureEvent event : sorted) {
            if (!deduped.isEmpty() && deduped.get(deduped.size() - 1).getStartTicks() == event.getStartTicks()) {
                deduped.set(deduped.size() - 1,
                        new MidiTimeSignatureEvent(event.getStartTicks(), event.getBeats(), event.getBeatType()));
                continue;
            }
            deduped.add(new MidiTimeSignatureEvent(event.getStartTicks(), event.getBeats(), event.getBeatType()));
        }
        if (deduped.isEmpty() || deduped.get(0).getStartTicks() != 0) {
            deduped.add(0, new MidiTimeSignatureEvent(0, 4, 4));
        }
        return Collections.unmodifiableList(buildMuseScoreStylePickupTimeSignaturePrelude(deduped,
                normalizeTicksPerQuarter(ticksPerQuarter), Math.max(0, Math.round(pickupTicks))));
    }

    public static List<String> buildMidiExportDiagnostics(List<String> diagnostics, List<MidiTempoEvent> tempoEvents,
            List<MidiTimeSignatureEvent> timeSignatureEvents, List<MidiKeySignatureEvent> keySignatureEvents) {
        List<String> exportDiagnostics = new ArrayList<String>();
        if (diagnostics != null) {
            for (String entry : diagnostics) {
                String text = trimToEmpty(entry);
                if (text.length() > 0) {
                    exportDiagnostics.add(text);
                }
            }
        }

        if (tempoEvents == null || tempoEvents.isEmpty()) {
            exportDiagnostics.add("level=info;code=MIDI_EXPORT_DEFAULT_TEMPO_INSERTED;fmt=midi;startTick=0;bpm=120");
        } else if (!hasTempoEventAtZero(tempoEvents)) {
            exportDiagnostics
                    .add("level=info;code=MIDI_EXPORT_DEFAULT_TEMPO_AT_ZERO_INSERTED;fmt=midi;startTick=0;bpm=120");
        }
        if (timeSignatureEvents == null || timeSignatureEvents.isEmpty()) {
            exportDiagnostics.add(
                    "level=info;code=MIDI_EXPORT_DEFAULT_TIMESIG_INSERTED;fmt=midi;startTick=0;beats=4;beatType=4");
        } else if (!hasTimeSignatureEventAtZero(timeSignatureEvents)) {
            exportDiagnostics.add(
                    "level=info;code=MIDI_EXPORT_DEFAULT_TIMESIG_AT_ZERO_INSERTED;fmt=midi;startTick=0;beats=4;beatType=4");
        }
        if (keySignatureEvents == null || keySignatureEvents.isEmpty()) {
            exportDiagnostics
                    .add("level=info;code=MIDI_EXPORT_DEFAULT_KEYSIG_INSERTED;fmt=midi;startTick=0;fifths=0;mode=major");
        } else if (!hasKeySignatureEventAtZero(keySignatureEvents)) {
            exportDiagnostics.add(
                    "level=info;code=MIDI_EXPORT_DEFAULT_KEYSIG_AT_ZERO_INSERTED;fmt=midi;startTick=0;fifths=0;mode=major");
        }
        return Collections.unmodifiableList(exportDiagnostics);
    }

    public static MidiExportTextMetaLines buildMidiExportTextMetaLines(String title, String movementTitle,
            String composer, int pickupTicks, List<String> sortedTrackIds, Map<String, String> trackNameById,
            boolean emitMksTextMeta) {
        List<String> mksTextMetaLines = new ArrayList<String>();
        if (emitMksTextMeta) {
            mksTextMetaLines.add("mks:meta-version:1");
        }
        String metaTitle = trimToEmpty(title);
        String metaMovementTitle = trimToEmpty(movementTitle);
        String metaComposer = trimToEmpty(composer);
        String selectedTitle = metaTitle.length() > 0 ? metaTitle
                : (metaMovementTitle.length() > 0 ? metaMovementTitle : "Untitled");
        String metaTrackTitle = selectedTitle.replaceAll("\\s+", " ").trim();
        if (metaTrackTitle.length() == 0) {
            metaTrackTitle = "Untitled";
        }
        List<String> standardTextMetaLines = new ArrayList<String>();
        standardTextMetaLines.add("title:" + metaTrackTitle);
        int metaPickupTicks = Math.max(0, Math.round(pickupTicks));

        if (emitMksTextMeta) {
            if (metaTitle.length() > 0) {
                mksTextMetaLines.add("mks:title:" + encodeURIComponent(metaTitle));
            }
            if (metaMovementTitle.length() > 0) {
                mksTextMetaLines.add("mks:movement-title:" + encodeURIComponent(metaMovementTitle));
            }
            if (metaComposer.length() > 0) {
                mksTextMetaLines.add("mks:composer:" + encodeURIComponent(metaComposer));
            }
            if (metaPickupTicks > 0) {
                mksTextMetaLines.add("mks:pickup-ticks:" + metaPickupTicks);
            }
            if (sortedTrackIds != null) {
                for (int index = 0; index < sortedTrackIds.size(); index++) {
                    String trackId = sortedTrackIds.get(index);
                    String trackName = trackNameById == null ? "" : trimToEmpty(trackNameById.get(trackId));
                    if (trackName.length() == 0) {
                        continue;
                    }
                    mksTextMetaLines.add("mks:part-name-track:" + (index + 1) + ":"
                            + encodeURIComponent(trackName));
                }
            }
        }

        return new MidiExportTextMetaLines(metaTrackTitle, standardTextMetaLines, mksTextMetaLines, metaPickupTicks);
    }

    public static MidiPlaybackTrackGrouping buildMidiPlaybackTracksById(List<RawMidiPlaybackEvent> sourceEvents) {
        Map<String, List<RawMidiPlaybackEvent>> tracksById =
                new LinkedHashMap<String, List<RawMidiPlaybackEvent>>();
        Map<String, String> trackNameById = new LinkedHashMap<String, String>();
        if (sourceEvents != null) {
            for (RawMidiPlaybackEvent event : sourceEvents) {
                if (event == null) {
                    continue;
                }
                String key = event.getTrackId().length() == 0 ? "__default__" : event.getTrackId();
                List<RawMidiPlaybackEvent> bucket = tracksById.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<RawMidiPlaybackEvent>();
                    tracksById.put(key, bucket);
                    trackNameById.put(key, trimToEmpty(event.getTrackName()));
                }
                bucket.add(event);
            }
        }
        List<String> sortedTrackIds = new ArrayList<String>(tracksById.keySet());
        Collections.sort(sortedTrackIds);
        return new MidiPlaybackTrackGrouping(tracksById, sortedTrackIds, trackNameById);
    }

    public static String normalizeMidiProgramPreset(String programPreset) {
        String value = trimToEmpty(programPreset);
        if ("electric_piano_2".equals(value)
                || "acoustic_grand_piano".equals(value)
                || "electric_piano_1".equals(value)
                || "honky_tonk_piano".equals(value)
                || "harpsichord".equals(value)
                || "clavinet".equals(value)
                || "drawbar_organ".equals(value)
                || "acoustic_guitar_nylon".equals(value)
                || "acoustic_bass".equals(value)
                || "violin".equals(value)
                || "string_ensemble_1".equals(value)
                || "synth_brass_1".equals(value)) {
            return value;
        }
        return "electric_piano_2";
    }

    public static int countMidiExportChannels(List<RawMidiPlaybackEvent> sourceEvents) {
        Set<Integer> channels = new LinkedHashSet<Integer>();
        if (sourceEvents != null) {
            for (RawMidiPlaybackEvent event : sourceEvents) {
                if (event != null) {
                    channels.add(Integer.valueOf(normalizeMidiChannel(event.getChannel())));
                }
            }
        }
        return channels.size();
    }

    public static List<String> buildMidiExportMksSysexChunkTexts(int ticksPerQuarter,
            List<RawMidiPlaybackEvent> sourceEvents, MidiPlaybackTrackGrouping trackGrouping,
            List<RawMidiControlEvent> controlEvents, List<MidiTempoEvent> tempoEvents,
            List<MidiTimeSignatureEvent> timeSignatureEvents, List<MidiKeySignatureEvent> keySignatureEvents,
            List<String> diagnostics) {
        MidiPlaybackTrackGrouping safeGrouping = trackGrouping == null ? buildMidiPlaybackTracksById(sourceEvents)
                : trackGrouping;
        return buildMksSysexChunkTexts(new MksSysexChunkTextParams(
                normalizeTicksPerQuarter(ticksPerQuarter),
                sourceEvents == null ? 0 : sourceEvents.size(),
                safeGrouping.getTracksById().size(),
                tempoEvents == null ? 0 : tempoEvents.size(),
                timeSignatureEvents == null ? 0 : timeSignatureEvents.size(),
                keySignatureEvents == null ? 0 : keySignatureEvents.size(),
                controlEvents == null ? 0 : controlEvents.size(),
                countMidiExportChannels(sourceEvents),
                diagnostics));
    }

    public static MidiExportPlaybackPreparation prepareMidiExportPlayback(List<RawMidiPlaybackEvent> events,
            int tempo, String programPreset, List<RawMidiControlEvent> controlEvents,
            List<MidiTempoEvent> tempoEvents, List<MidiTimeSignatureEvent> timeSignatureEvents,
            List<MidiKeySignatureEvent> keySignatureEvents, boolean embedMksSysEx, boolean emitMksTextMeta,
            int ticksPerQuarter, List<String> diagnostics, boolean normalizeForParity, String title,
            String movementTitle, String composer, int pickupTicks) {
        int writerTicksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
        List<RawMidiPlaybackEvent> sourceEvents = normalizeForParity
                ? normalizePlaybackEventsForParity(events)
                : (events == null ? Collections.<RawMidiPlaybackEvent>emptyList()
                        : Collections.unmodifiableList(new ArrayList<RawMidiPlaybackEvent>(events)));
        MidiPlaybackTrackGrouping trackGrouping = buildMidiPlaybackTracksById(sourceEvents);
        MidiExportTextMetaLines textMetaLines = buildMidiExportTextMetaLines(title, movementTitle, composer,
                pickupTicks, trackGrouping.getSortedTrackIds(), trackGrouping.getTrackNameById(), emitMksTextMeta);
        List<MidiTempoEvent> normalizedTempoEvents = normalizeMidiExportTempoEvents(tempoEvents, tempo);
        List<MidiTimeSignatureEvent> normalizedTimeSignatureEvents =
                normalizeMidiExportTimeSignatureEvents(timeSignatureEvents, writerTicksPerQuarter,
                        textMetaLines.getPickupTicks());
        List<MidiKeySignatureEvent> normalizedKeySignatureEvents =
                normalizeMidiExportKeySignatureEvents(keySignatureEvents);
        List<String> exportDiagnostics = buildMidiExportDiagnostics(diagnostics, tempoEvents, timeSignatureEvents,
                keySignatureEvents);
        List<String> sysexChunks = buildMidiExportMksSysexChunkTexts(writerTicksPerQuarter, sourceEvents,
                trackGrouping, controlEvents, normalizedTempoEvents, normalizedTimeSignatureEvents,
                normalizedKeySignatureEvents, exportDiagnostics);
        String normalizedProgramPreset = normalizeMidiProgramPreset(programPreset);
        return new MidiExportPlaybackPreparation(writerTicksPerQuarter, sourceEvents, trackGrouping,
                textMetaLines, normalizedTempoEvents, normalizedTimeSignatureEvents, normalizedKeySignatureEvents,
                exportDiagnostics, sysexChunks, normalizedProgramPreset, embedMksSysEx);
    }

    public static byte[] buildRawMidiBytesForPlayback(MidiExportPlaybackPreparation preparation,
            Map<String, Integer> trackProgramOverrides, List<RawMidiControlEvent> controlEvents,
            String retriggerPolicy) {
        MidiExportPlaybackPreparation safePreparation = preparation == null
                ? prepareMidiExportPlayback(Collections.<RawMidiPlaybackEvent>emptyList(), 120,
                        "electric_piano_2", Collections.<RawMidiControlEvent>emptyList(),
                        Collections.<MidiTempoEvent>emptyList(), Collections.<MidiTimeSignatureEvent>emptyList(),
                        Collections.<MidiKeySignatureEvent>emptyList(), true, true, 480,
                        Collections.<String>emptyList(), false, null, null, null, 0)
                : preparation;
        List<byte[]> trackChunks = new ArrayList<byte[]>();
        List<MidiTickKeySignatureEvent> rawKeySignatureEvents = new ArrayList<MidiTickKeySignatureEvent>();
        for (MidiKeySignatureEvent event : safePreparation.getKeySignatureEvents()) {
            rawKeySignatureEvents.add(new MidiTickKeySignatureEvent(event.getStartTicks(), event.getFifths(),
                    event.getMode()));
        }
        trackChunks.add(buildRawMidiTempoTrackChunk(safePreparation.getTempoEvents(),
                safePreparation.getTimeSignatureEvents(), rawKeySignatureEvents,
                new RawMidiTempoTrackOptions(safePreparation.isEmbedMksSysEx(),
                        safePreparation.getSysexChunks(), safePreparation.getCombinedTextMetaLines(),
                        safePreparation.getTextMetaLines().getMetaTrackTitle())));
        trackChunks.addAll(buildRawMidiNoteTrackChunks(safePreparation.getSourceEvents(), trackProgramOverrides,
                safePreparation.getProgramPreset(), retriggerPolicy == null ? "off_before_on" : retriggerPolicy));
        trackChunks.addAll(buildRawMidiControlTrackChunks(controlEvents));
        return buildRawMidiBytesFromTrackChunks(trackChunks, safePreparation.getTicksPerQuarter());
    }

    public static MidiExportPlaybackBuildResult buildMidiPlaybackExport(List<RawMidiPlaybackEvent> events,
            int tempo, String programPreset, Map<String, Integer> trackProgramOverrides,
            List<RawMidiControlEvent> controlEvents, List<MidiTempoEvent> tempoEvents,
            List<MidiTimeSignatureEvent> timeSignatureEvents, List<MidiKeySignatureEvent> keySignatureEvents,
            boolean rawWriter, boolean embedMksSysEx, boolean emitMksTextMeta, int ticksPerQuarter,
            List<String> diagnostics, boolean normalizeForParity, String rawRetriggerPolicy, String title,
            String movementTitle, String composer, int pickupTicks) {
        MidiExportPlaybackPreparation preparation = prepareMidiExportPlayback(events, tempo, programPreset,
                controlEvents, tempoEvents, timeSignatureEvents, keySignatureEvents, embedMksSysEx,
                emitMksTextMeta, ticksPerQuarter, diagnostics, normalizeForParity, title, movementTitle,
                composer, pickupTicks);
        if (rawWriter) {
            byte[] rawBytes = buildRawMidiBytesForPlayback(preparation, trackProgramOverrides, controlEvents,
                    rawRetriggerPolicy == null ? "off_before_on" : rawRetriggerPolicy);
            return MidiExportPlaybackBuildResult.raw(preparation, rawBytes);
        }
        MidiExportWriterTrackPlan writerTrackPlan = buildMidiExportWriterTrackPlan(
                preparation.getTextMetaLines().getMetaTrackTitle(),
                buildMidiExportMetaTimelineEventData(preparation.getTempoEvents(),
                        preparation.getTimeSignatureEvents(), preparation.getKeySignatureEvents()),
                preparation.isEmbedMksSysEx(), preparation.getSysexChunks(),
                preparation.getCombinedTextMetaLines(), preparation.getTrackGrouping(), trackProgramOverrides,
                preparation.getProgramPreset(), controlEvents);
        return MidiExportPlaybackBuildResult.writer(preparation, writerTrackPlan);
    }

    public static List<byte[]> buildMidiExportMetaTimelineEventData(List<MidiTempoEvent> tempoEvents,
            List<MidiTimeSignatureEvent> timeSignatureEvents, List<MidiKeySignatureEvent> keySignatureEvents) {
        List<MidiExportMetaTimelineEvent> timeline = new ArrayList<MidiExportMetaTimelineEvent>();
        if (tempoEvents != null) {
            for (MidiTempoEvent event : tempoEvents) {
                if (event != null) {
                    timeline.add(MidiExportMetaTimelineEvent.tempo(event));
                }
            }
        }
        if (timeSignatureEvents != null) {
            for (MidiTimeSignatureEvent event : timeSignatureEvents) {
                if (event != null) {
                    timeline.add(MidiExportMetaTimelineEvent.time(event));
                }
            }
        }
        if (keySignatureEvents != null) {
            for (MidiKeySignatureEvent event : keySignatureEvents) {
                if (event != null) {
                    timeline.add(MidiExportMetaTimelineEvent.key(event));
                }
            }
        }
        Collections.sort(timeline, new Comparator<MidiExportMetaTimelineEvent>() {
            @Override
            public int compare(MidiExportMetaTimelineEvent left, MidiExportMetaTimelineEvent right) {
                if (left.getStartTicks() == right.getStartTicks()) {
                    return Integer.valueOf(left.getKindPriority()).compareTo(Integer.valueOf(right.getKindPriority()));
                }
                return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
            }
        });

        List<byte[]> out = new ArrayList<byte[]>();
        int prevTempoTick = 0;
        for (MidiExportMetaTimelineEvent event : timeline) {
            int currentTick = Math.max(0, Math.round(event.getStartTicks()));
            int deltaTicks = Math.max(0, currentTick - prevTempoTick);
            if ("tempo".equals(event.getKind())) {
                out.add(buildTempoMetaEventData(deltaTicks, event.getBpm()));
            } else if ("time".equals(event.getKind())) {
                out.add(buildTimeSignatureMetaEventData(deltaTicks, event.getBeats(), event.getBeatType()));
            } else {
                out.add(buildKeySignatureMetaEventData(deltaTicks, event.getFifths(), event.getMode()));
            }
            prevTempoTick = currentTick;
        }
        return Collections.unmodifiableList(out);
    }

    public static MidiExportPlaybackTrackPlan buildMidiExportPlaybackTrackPlan(String trackId, int trackIndex,
            List<RawMidiPlaybackEvent> trackEvents, Map<String, Integer> trackProgramOverrides,
            String programPreset) {
        List<RawMidiPlaybackEvent> sorted = trackEvents == null ? new ArrayList<RawMidiPlaybackEvent>()
                : new ArrayList<RawMidiPlaybackEvent>(trackEvents);
        Collections.sort(sorted, new Comparator<RawMidiPlaybackEvent>() {
            @Override
            public int compare(RawMidiPlaybackEvent left, RawMidiPlaybackEvent right) {
                if (left.getStartTicks() == right.getStartTicks()) {
                    return Integer.valueOf(left.getMidiNumber()).compareTo(Integer.valueOf(right.getMidiNumber()));
                }
                return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
            }
        });
        if (sorted.isEmpty()) {
            return new MidiExportPlaybackTrackPlan(Collections.<RawMidiPlaybackEvent>emptyList(),
                    trimToEmpty(trackId).length() > 0 ? trimToEmpty(trackId) : "Track " + (Math.max(0, trackIndex) + 1),
                    Collections.<Integer>emptyList(), instrumentByPreset(normalizeMidiProgramPreset(programPreset)));
        }

        String safeTrackId = trackId == null ? "" : trackId;
        String trackName = trimToEmpty(sorted.get(0).getTrackName());
        if (trackName.length() == 0) {
            trackName = safeTrackId.length() > 0 ? safeTrackId : "Track " + (Math.max(0, trackIndex) + 1);
        }

        Set<Integer> channelSet = new LinkedHashSet<Integer>();
        for (RawMidiPlaybackEvent event : sorted) {
            channelSet.add(Integer.valueOf(normalizeMidiChannel(event.getChannel())));
        }
        List<Integer> channels = new ArrayList<Integer>(channelSet);
        Collections.sort(channels);

        Map<String, Integer> safeOverrides = trackProgramOverrides == null ? Collections.<String, Integer>emptyMap()
                : trackProgramOverrides;
        Integer rawOverrideProgram = safeOverrides.get(safeTrackId);
        Integer overrideProgram = normalizeMidiProgramNumber(
                rawOverrideProgram == null ? Double.NaN : rawOverrideProgram.doubleValue());
        int selectedInstrumentProgram = overrideProgram == null
                ? instrumentByPreset(normalizeMidiProgramPreset(programPreset))
                : overrideProgram.intValue();
        return new MidiExportPlaybackTrackPlan(sorted, trackName, channels, selectedInstrumentProgram);
    }

    public static List<MidiExportProgramChangeEventFields> buildMidiExportProgramChangeEventFields(
            MidiExportPlaybackTrackPlan trackPlan) {
        if (trackPlan == null) {
            return Collections.emptyList();
        }
        List<MidiExportProgramChangeEventFields> fields = new ArrayList<MidiExportProgramChangeEventFields>();
        for (Integer channel : trackPlan.getChannels()) {
            if (channel == null || channel.intValue() == 10) {
                continue;
            }
            fields.add(new MidiExportProgramChangeEventFields(trackPlan.getSelectedInstrumentProgram(),
                    channel.intValue(), 0));
        }
        return Collections.unmodifiableList(fields);
    }

    public static List<MidiWriterNoteEventFields> buildMidiExportNoteEventFields(
            MidiExportPlaybackTrackPlan trackPlan) {
        if (trackPlan == null) {
            return Collections.emptyList();
        }
        List<MidiWriterNoteEventFields> fields = new ArrayList<MidiWriterNoteEventFields>();
        for (RawMidiPlaybackEvent event : trackPlan.getTrackEvents()) {
            fields.add(new MidiWriterNoteEventFields(Collections.singletonList(midiToPitchText(event.getMidiNumber())),
                    "T" + event.getDurTicks(), Math.max(0, Math.round(event.getStartTicks())),
                    clampVelocity(event.getVelocity()), normalizeMidiChannel(event.getChannel())));
        }
        return Collections.unmodifiableList(fields);
    }

    public static List<MidiExportControlTrackPlan> buildMidiExportControlTrackPlans(
            List<RawMidiControlEvent> controlEvents) {
        Map<String, List<RawMidiControlEvent>> groupedControlEvents =
                new LinkedHashMap<String, List<RawMidiControlEvent>>();
        if (controlEvents != null) {
            for (RawMidiControlEvent controlEvent : controlEvents) {
                if (controlEvent == null) {
                    continue;
                }
                String key = controlEvent.getTrackId() + "::" + normalizeMidiChannel(controlEvent.getChannel());
                List<RawMidiControlEvent> bucket = groupedControlEvents.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<RawMidiControlEvent>();
                    groupedControlEvents.put(key, bucket);
                }
                bucket.add(controlEvent);
            }
        }

        List<String> sortedControlKeys = new ArrayList<String>(groupedControlEvents.keySet());
        Collections.sort(sortedControlKeys);
        List<MidiExportControlTrackPlan> plans = new ArrayList<MidiExportControlTrackPlan>();
        for (String controlKey : sortedControlKeys) {
            List<RawMidiControlEvent> channelEvents = new ArrayList<RawMidiControlEvent>(
                    groupedControlEvents.get(controlKey));
            Collections.sort(channelEvents, new Comparator<RawMidiControlEvent>() {
                @Override
                public int compare(RawMidiControlEvent left, RawMidiControlEvent right) {
                    if (left.getStartTicks() == right.getStartTicks()) {
                        if (left.getControllerNumber() == right.getControllerNumber()) {
                            return Integer.valueOf(left.getControllerValue())
                                    .compareTo(Integer.valueOf(right.getControllerValue()));
                        }
                        return Integer.valueOf(left.getControllerNumber())
                                .compareTo(Integer.valueOf(right.getControllerNumber()));
                    }
                    return Integer.valueOf(left.getStartTicks()).compareTo(Integer.valueOf(right.getStartTicks()));
                }
            });
            if (channelEvents.isEmpty()) {
                continue;
            }
            RawMidiControlEvent first = channelEvents.get(0);
            String trackName = first.getTrackName() + " Pedal";
            List<MidiWriterControllerChangeEventFields> fields =
                    new ArrayList<MidiWriterControllerChangeEventFields>();
            int prevTick = 0;
            for (RawMidiControlEvent controlEvent : channelEvents) {
                int currentTick = Math.max(0, Math.round(controlEvent.getStartTicks()));
                int deltaTicks = Math.max(0, currentTick - prevTick);
                fields.add(new MidiWriterControllerChangeEventFields(controlEvent.getChannel(),
                        controlEvent.getControllerNumber(), controlEvent.getControllerValue(), deltaTicks));
                prevTick = currentTick;
            }
            plans.add(new MidiExportControlTrackPlan(controlKey, channelEvents, trackName, fields));
        }
        return Collections.unmodifiableList(plans);
    }

    public static MidiExportWriterTrackPlan buildMidiExportWriterTrackPlan(String metaTrackTitle,
            List<byte[]> metaTimelineEventData, boolean embedMksSysEx, List<String> sysexChunks,
            List<String> textMetaLines, MidiPlaybackTrackGrouping trackGrouping,
            Map<String, Integer> trackProgramOverrides, String programPreset,
            List<RawMidiControlEvent> controlEvents) {
        List<byte[]> metaEvents = new ArrayList<byte[]>();
        if (metaTimelineEventData != null) {
            for (byte[] eventData : metaTimelineEventData) {
                if (eventData != null) {
                    metaEvents.add(Arrays.copyOf(eventData, eventData.length));
                }
            }
        }
        if (embedMksSysEx && sysexChunks != null) {
            for (String chunk : sysexChunks) {
                metaEvents.add(buildMksSysexEventData(0, chunk));
            }
        }
        if (textMetaLines != null) {
            for (String line : textMetaLines) {
                metaEvents.add(buildTextMetaEventData(0, line, 0x01));
            }
        }

        List<MidiExportPlaybackTrackPlan> playbackPlans = new ArrayList<MidiExportPlaybackTrackPlan>();
        if (trackGrouping != null) {
            Map<String, List<RawMidiPlaybackEvent>> tracksById = trackGrouping.getTracksById();
            List<String> sortedTrackIds = trackGrouping.getSortedTrackIds();
            for (int index = 0; index < sortedTrackIds.size(); index++) {
                String trackId = sortedTrackIds.get(index);
                MidiExportPlaybackTrackPlan plan = buildMidiExportPlaybackTrackPlan(trackId, index,
                        tracksById.get(trackId), trackProgramOverrides, programPreset);
                if (!plan.getTrackEvents().isEmpty()) {
                    playbackPlans.add(plan);
                }
            }
        }

        List<MidiExportControlTrackPlan> controlPlans = buildMidiExportControlTrackPlans(controlEvents);
        return new MidiExportWriterTrackPlan(metaTrackTitle, metaEvents, playbackPlans, controlPlans);
    }

    public static String asciiBytesToString(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        if (bytes != null) {
            for (byte value : bytes) {
                out.append((char) (value & 0x7f));
            }
        }
        return out.toString();
    }

    public static String decodeMetaTextBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String safeDecodeURIComponent(String value) {
        String text = value == null ? "" : value;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch == '%' && index + 2 < text.length()) {
                int hi = hexValue(text.charAt(index + 1));
                int lo = hexValue(text.charAt(index + 2));
                if (hi >= 0 && lo >= 0) {
                    bytes.write((hi << 4) | lo);
                    index += 2;
                    continue;
                }
            }
            flushDecodedBytes(bytes, out);
            out.append(ch);
        }
        flushDecodedBytes(bytes, out);
        return out.toString();
    }

    private static int hexValue(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        return -1;
    }

    private static void flushDecodedBytes(ByteArrayOutputStream bytes, StringBuilder out) {
        if (bytes.size() == 0) {
            return;
        }
        out.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        bytes.reset();
    }

    public static MksMidiTextMetadata parseMksMidiTextMetadata(List<String> lines) {
        MksMidiTextMetadata metadata = new MksMidiTextMetadata();
        if (lines == null) {
            return metadata;
        }
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (!line.startsWith("mks:")) {
                continue;
            }
            if (line.startsWith("mks:title:")) {
                if (metadata.getTitle() == null) {
                    metadata.setTitle(safeDecodeURIComponent(line.substring("mks:title:".length())));
                }
                continue;
            }
            if (line.startsWith("mks:movement-title:")) {
                if (metadata.getMovementTitle() == null) {
                    metadata.setMovementTitle(safeDecodeURIComponent(line.substring("mks:movement-title:".length())));
                }
                continue;
            }
            if (line.startsWith("mks:composer:")) {
                if (metadata.getComposer() == null) {
                    metadata.setComposer(safeDecodeURIComponent(line.substring("mks:composer:".length())));
                }
                continue;
            }
            if (line.startsWith("mks:pickup-ticks:")) {
                if (metadata.getPickupTicks() == null) {
                    try {
                        int parsed = Integer.parseInt(line.substring("mks:pickup-ticks:".length()));
                        if (parsed > 0) {
                            metadata.setPickupTicks(Integer.valueOf(parsed));
                        }
                    } catch (NumberFormatException ex) {
                        // Keep upstream fallback behavior: ignore malformed metadata.
                    }
                }
                continue;
            }
            if (line.startsWith("mks:part-name-track:")) {
                String payload = line.substring("mks:part-name-track:".length());
                int sep = payload.indexOf(':');
                if (sep <= 0) {
                    continue;
                }
                try {
                    int trackIndex = Integer.parseInt(payload.substring(0, sep));
                    if (trackIndex < 0 || metadata.getPartNameByTrackIndex().containsKey(Integer.valueOf(trackIndex))) {
                        continue;
                    }
                    metadata.getPartNameByTrackIndex().put(Integer.valueOf(trackIndex),
                            safeDecodeURIComponent(payload.substring(sep + 1)));
                } catch (NumberFormatException ex) {
                    // Ignore malformed metadata.
                }
            }
        }
        return metadata;
    }

    public static MksSysExChunk parseMksSysExChunk(byte[] payloadBytes) {
        if (payloadBytes == null || payloadBytes.length == 0) {
            return null;
        }
        int length = payloadBytes[payloadBytes.length - 1] == (byte) 0xf7 ? payloadBytes.length - 1
                : payloadBytes.length;
        byte[] trimmed = new byte[length];
        System.arraycopy(payloadBytes, 0, trimmed, 0, length);
        String text = asciiBytesToString(trimmed);
        if (!text.startsWith("mks|")) {
            return null;
        }
        String[] parts = text.split("\\|");
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int index = 1; index < parts.length; index++) {
            int eq = parts[index].indexOf('=');
            if (eq <= 0) {
                continue;
            }
            map.put(parts[index].substring(0, eq), parts[index].substring(eq + 1));
        }
        if (!"1".equals(map.get("v"))) {
            return null;
        }
        try {
            int messageId = Integer.parseInt(valueOrEmpty(map.get("m")));
            int chunkIndex = Integer.parseInt(valueOrEmpty(map.get("i")));
            int totalChunks = Integer.parseInt(valueOrEmpty(map.get("n")));
            if (chunkIndex < 1 || totalChunks < 1 || chunkIndex > totalChunks) {
                return null;
            }
            return new MksSysExChunk(messageId, chunkIndex, totalChunks,
                    safeDecodeURIComponent(valueOrEmpty(map.get("d"))));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public static List<String> assembleMksSysExPayloads(List<MksSysExChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, List<MksSysExChunk>> byMessageId = new LinkedHashMap<Integer, List<MksSysExChunk>>();
        for (MksSysExChunk chunk : chunks) {
            if (chunk == null) {
                continue;
            }
            Integer key = Integer.valueOf(chunk.getMessageId());
            List<MksSysExChunk> bucket = byMessageId.get(key);
            if (bucket == null) {
                bucket = new ArrayList<MksSysExChunk>();
                byMessageId.put(key, bucket);
            }
            bucket.add(chunk);
        }
        List<Integer> messageIds = new ArrayList<Integer>(byMessageId.keySet());
        Collections.sort(messageIds);
        List<String> payloads = new ArrayList<String>();
        for (Integer messageId : messageIds) {
            List<MksSysExChunk> group = new ArrayList<MksSysExChunk>(byMessageId.get(messageId));
            Collections.sort(group, new Comparator<MksSysExChunk>() {
                @Override
                public int compare(MksSysExChunk left, MksSysExChunk right) {
                    return Integer.valueOf(left.getChunkIndex()).compareTo(Integer.valueOf(right.getChunkIndex()));
                }
            });
            int total = group.isEmpty() ? 0 : group.get(0).getTotalChunks();
            if (total <= 0 || group.size() < total) {
                continue;
            }
            Map<Integer, String> byIndex = new LinkedHashMap<Integer, String>();
            for (MksSysExChunk chunk : group) {
                if (chunk.getTotalChunks() == total) {
                    byIndex.put(Integer.valueOf(chunk.getChunkIndex()), chunk.getData());
                }
            }
            if (byIndex.size() < total) {
                continue;
            }
            StringBuilder assembled = new StringBuilder();
            boolean ok = true;
            for (int index = 1; index <= total; index++) {
                String text = byIndex.get(Integer.valueOf(index));
                if (text == null) {
                    ok = false;
                    break;
                }
                assembled.append(text);
            }
            if (ok) {
                payloads.add(assembled.toString());
            }
        }
        return payloads;
    }

    public static MidiImportResult convertMidiToMusicXml(byte[] midiBytes, MidiImportOptions options) {
        MidiImportOptions safeOptions = options == null ? new MidiImportOptions() : options;
        List<MidiImportDiagnostic> diagnostics = new ArrayList<MidiImportDiagnostic>();
        List<MidiImportDiagnostic> warnings = new ArrayList<MidiImportDiagnostic>();
        String quantizeGridOption = normalizeMidiImportQuantizeGridOption(safeOptions.getQuantizeGrid());
        boolean debugImportMetadata = safeOptions.getDebugMetadata() == null
                ? true
                : safeOptions.getDebugMetadata().booleanValue();
        boolean sourceImportMetadata = safeOptions.getSourceMetadata() == null
                ? true
                : safeOptions.getSourceMetadata().booleanValue();
        boolean tripletAwareQuantize = safeOptions.getTripletAwareQuantize() == null
                ? true
                : safeOptions.getTripletAwareQuantize().booleanValue();
        byte[] bytes = midiBytes == null ? new byte[0] : midiBytes;
        if (bytes.length == 0) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE", "MIDI input is empty."));
            return new MidiImportResult(false, "", diagnostics, warnings);
        }

        SmfHeaderParseResult headerResult = parseSmfHeader(bytes);
        diagnostics.addAll(headerResult.getDiagnostics());
        if (headerResult.getHeader() == null) {
            return new MidiImportResult(false, "", diagnostics, warnings);
        }
        ParsedSmfHeader header = headerResult.getHeader();
        int offset = header.getNextOffset();
        Set<String> trackChannelSet = new LinkedHashSet<String>();
        Map<String, Integer> programByTrackChannel = new LinkedHashMap<String, Integer>();
        List<SmfImportedNote> collectedNotes = new ArrayList<SmfImportedNote>();
        List<MidiControllerEvent> controllerEvents = new ArrayList<MidiControllerEvent>();
        List<MidiTickTimeSignatureEvent> timeSignatureEvents = new ArrayList<MidiTickTimeSignatureEvent>();
        List<MidiTickKeySignatureEvent> keySignatureEvents = new ArrayList<MidiTickKeySignatureEvent>();
        List<MidiTempoEvent> tempoMetaEvents = new ArrayList<MidiTempoEvent>();
        List<String> mksSysExPayloads = new ArrayList<String>();
        List<String> mksTextMetaLines = new ArrayList<String>();
        List<String> standardTitleCandidates = new ArrayList<String>();
        List<String> standardComposerCandidates = new ArrayList<String>();
        String singleTrackTitleCandidate = "";
        Map<Integer, String> trackNameByIndex = new LinkedHashMap<Integer, String>();

        for (int index = 0; index < header.getTrackCount(); index++) {
            if (offset + 8 > bytes.length) {
                diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE",
                        "Track chunk " + (index + 1) + " header is truncated."));
                return new MidiImportResult(false, "", diagnostics, warnings);
            }
            if (!"MTrk".equals(readAscii(bytes, offset, 4))) {
                diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE",
                        "Track chunk " + (index + 1) + " is missing MTrk signature."));
                return new MidiImportResult(false, "", diagnostics, warnings);
            }
            Long trackLength = readUint32Be(bytes, offset + 4);
            if (trackLength == null || offset + 8L + trackLength.longValue() > bytes.length) {
                diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE",
                        "Track chunk " + (index + 1) + " has invalid length."));
                return new MidiImportResult(false, "", diagnostics, warnings);
            }
            byte[] trackData = Arrays.copyOfRange(bytes, offset + 8, (int) (offset + 8L + trackLength.longValue()));
            SmfParseSummary summary = parseTrackSummary(trackData, index);
            if (header.getTrackCount() == 1 && index == 0 && singleTrackTitleCandidate.length() == 0
                    && summary.getTrackName() != null && !isGenericMidiTrackName(summary.getTrackName())) {
                singleTrackTitleCandidate = summary.getTrackName().trim();
            }
            if (summary.getTrackName() != null && summary.getTrackName().length() > 0) {
                trackNameByIndex.put(Integer.valueOf(index), summary.getTrackName());
            }
            collectedNotes.addAll(summary.getNotes());
            standardTitleCandidates.addAll(summary.getStandardTitleCandidates());
            standardComposerCandidates.addAll(summary.getStandardComposerCandidates());
            for (MidiControllerEvent event : summary.getControllerEvents()) {
                controllerEvents.add(new MidiControllerEvent(index, event.getTick(), event.getChannel(),
                        event.getControllerNumber(), event.getControllerValue()));
            }
            timeSignatureEvents.addAll(summary.getTimeSignatureEvents());
            keySignatureEvents.addAll(summary.getKeySignatureEvents());
            tempoMetaEvents.addAll(summary.getTempoEvents());
            mksSysExPayloads.addAll(summary.getMksSysExPayloads());
            mksTextMetaLines.addAll(summary.getMksTextMetaLines());
            for (SmfImportedNote note : summary.getNotes()) {
                trackChannelSet.add(index + ":" + note.getChannel());
            }
            for (Map.Entry<String, Integer> entry : summary.getProgramByTrackChannel().entrySet()) {
                if (!programByTrackChannel.containsKey(entry.getKey())) {
                    programByTrackChannel.put(entry.getKey(), entry.getValue());
                }
            }
            warnings.addAll(summary.getParseWarnings());
            offset += 8 + trackLength.intValue();
        }

        MksMidiTextMetadata parsedMksTextMetadata = parseMksMidiTextMetadata(mksTextMetaLines);
        String standardTitle = firstNonBlank(standardTitleCandidates);
        if (standardTitle.length() == 0) {
            standardTitle = singleTrackTitleCandidate;
        }
        String standardComposer = firstNonBlank(standardComposerCandidates);
        String title = standardTitle.length() > 0 ? standardTitle
                : (trimToEmpty(parsedMksTextMetadata.getTitle()).length() > 0
                        ? trimToEmpty(parsedMksTextMetadata.getTitle())
                        : (trimToEmpty(safeOptions.getTitle()).length() > 0
                                ? trimToEmpty(safeOptions.getTitle())
                                : "Imported MIDI"));
        String quantizeGrid = "auto".equals(quantizeGridOption)
                ? chooseBestImportQuantizeGrid(collectedNotes, header.getTicksPerQuarter(), tripletAwareQuantize)
                : quantizeGridOption;
        QuantizedImportedNotesResult quantized = quantizeImportedNotes(collectedNotes, header.getTicksPerQuarter(),
                quantizeGrid, tripletAwareQuantize);
        warnings.addAll(quantized.getWarnings());
        List<ImportedQuantizedNote> velocityScaledNotes =
                applyImportedControllerVelocityScale(quantized.getNotes(), controllerEvents);
        Map<String, List<ImportedQuantizedNote>> notesByTrackChannel =
                new LinkedHashMap<String, List<ImportedQuantizedNote>>();
        for (ImportedQuantizedNote note : velocityScaledNotes) {
            String key = note.getTrackIndex() + ":" + note.getChannel();
            List<ImportedQuantizedNote> bucket = notesByTrackChannel.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ImportedQuantizedNote>();
                notesByTrackChannel.put(key, bucket);
            }
            bucket.add(note);
        }

        LeadingPickupTimeSignatureNormalization normalizedTimeSignature =
                normalizeLeadingPickupTimeSignatureEvents(timeSignatureEvents, header.getTicksPerQuarter());
        if (normalizedTimeSignature.isNormalized()) {
            warnings.add(new MidiImportDiagnostic("MIDI_TIME_SIGNATURE_PICKUP_NORMALIZED",
                    "Normalized leading pickup time signature (e.g. 1/8 at tick 0 followed by full meter)."));
        }
        MidiTickTimeSignatureEvent firstTimeSignature = normalizedTimeSignature.getEvents().isEmpty()
                ? new MidiTickTimeSignatureEvent(0, 4, 4)
                : normalizedTimeSignature.getEvents().get(0);
        MidiKeySignature inferredKeySignature = keySignatureEvents.isEmpty()
                ? inferKeySignatureFromImportedNotes(velocityScaledNotes)
                : null;
        MidiTickKeySignatureEvent firstKeySignature = firstMidiKeySignatureEvent(keySignatureEvents,
                inferredKeySignature);
        int beats = Math.max(1, Math.round(firstTimeSignature.getBeats()));
        int beatType = Math.max(1, Math.round(firstTimeSignature.getBeatType()));
        int measureTicks = Math.max(1, Math.round((header.getTicksPerQuarter() * 4.0f * beats) / beatType));
        int metadataPickupTicks = Math.max(0, Math.min(measureTicks - 1,
                parsedMksTextMetadata.getPickupTicks() == null ? 0 : parsedMksTextMetadata.getPickupTicks()));
        int resolvedPickupTicks = normalizedTimeSignature.getPickupTicks() > 0
                ? normalizedTimeSignature.getPickupTicks()
                : metadataPickupTicks;
        int keyFifths = Math.max(-7, Math.min(7, Math.round(firstKeySignature.getFifths())));
        String keyMode = "minor".equals(firstKeySignature.getMode()) ? "minor" : "major";
        if (keySignatureEvents.isEmpty() && inferredKeySignature != null) {
            warnings.add(new MidiImportDiagnostic("MIDI_KEY_SIGNATURE_INFERRED",
                    "MIDI key signature meta event was missing; inferred key signature (" + keyFifths + ", "
                            + keyMode + ")."));
        }

        List<MidiTempoEvent> tempoEvents = normalizeImportedTempoEvents(tempoMetaEvents);
        List<MidiTrackChannelGroup> partGroups = buildMidiImportPartGroups(trackChannelSet);
        boolean hadDrumChannel = false;
        for (MidiTrackChannelGroup group : partGroups) {
            if (group.getChannel() == 10) {
                hadDrumChannel = true;
                break;
            }
        }
        if (hadDrumChannel) {
            warnings.add(new MidiImportDiagnostic("MIDI_DRUM_CHANNEL_SEPARATED",
                    "Channel 10 was mapped to a dedicated drum part."));
        }
        String xml = buildImportSkeletonMusicXml(title, parsedMksTextMetadata.getMovementTitle(),
                standardComposer.length() > 0 ? standardComposer : parsedMksTextMetadata.getComposer(), quantizeGrid,
                Integer.valueOf(quantized.getDivisions()), header.getTicksPerQuarter(), beats, beatType, keyFifths,
                keyMode, tempoEvents, resolvedPickupTicks, partGroups, notesByTrackChannel, programByTrackChannel,
                warnings, debugImportMetadata, debugImportMetadata ? buildMidiSysExMiscXml(mksSysExPayloads) : "",
                sourceImportMetadata ? buildMidiSourceMiscXml(bytes) : "", trackNameByIndex,
                parsedMksTextMetadata);
        return new MidiImportResult(diagnostics.isEmpty(), prettyPrintXml(xml), diagnostics, warnings);
    }

    private static String firstNonBlank(List<String> values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String trimmed = trimToEmpty(value);
            if (trimmed.length() > 0) {
                return trimmed;
            }
        }
        return "";
    }

    private static MidiTickKeySignatureEvent firstMidiKeySignatureEvent(
            List<MidiTickKeySignatureEvent> keySignatureEvents, MidiKeySignature inferredKeySignature) {
        List<MidiTickKeySignatureEvent> sorted = new ArrayList<MidiTickKeySignatureEvent>();
        if (keySignatureEvents != null) {
            for (MidiTickKeySignatureEvent event : keySignatureEvents) {
                if (event != null) {
                    sorted.add(event);
                }
            }
        }
        Collections.sort(sorted, new Comparator<MidiTickKeySignatureEvent>() {
            @Override
            public int compare(MidiTickKeySignatureEvent left, MidiTickKeySignatureEvent right) {
                return Integer.valueOf(left.getTick()).compareTo(Integer.valueOf(right.getTick()));
            }
        });
        if (!sorted.isEmpty()) {
            return sorted.get(0);
        }
        return new MidiTickKeySignatureEvent(0,
                inferredKeySignature == null ? 0 : inferredKeySignature.getFifths(),
                inferredKeySignature == null ? "major" : inferredKeySignature.getMode());
    }

    private static List<MidiTempoEvent> normalizeImportedTempoEvents(List<MidiTempoEvent> tempoMetaEvents) {
        List<MidiTempoEvent> sorted = new ArrayList<MidiTempoEvent>();
        if (tempoMetaEvents != null) {
            for (MidiTempoEvent event : tempoMetaEvents) {
                if (event != null) {
                    sorted.add(new MidiTempoEvent(Math.max(0, event.getTick()), clampTempo(event.getBpm())));
                }
            }
        }
        Collections.sort(sorted, new Comparator<MidiTempoEvent>() {
            @Override
            public int compare(MidiTempoEvent left, MidiTempoEvent right) {
                return Integer.valueOf(left.getTick()).compareTo(Integer.valueOf(right.getTick()));
            }
        });
        List<MidiTempoEvent> deduped = new ArrayList<MidiTempoEvent>();
        for (MidiTempoEvent event : sorted) {
            if (!deduped.isEmpty() && deduped.get(deduped.size() - 1).getTick() == event.getTick()) {
                deduped.set(deduped.size() - 1, new MidiTempoEvent(event.getTick(), event.getBpm()));
            } else {
                deduped.add(event);
            }
        }
        return Collections.unmodifiableList(deduped);
    }

    private static List<MidiTrackChannelGroup> buildMidiImportPartGroups(Set<String> trackChannelSet) {
        List<MidiTrackChannelGroup> groups = new ArrayList<MidiTrackChannelGroup>();
        if (trackChannelSet != null) {
            for (String entry : trackChannelSet) {
                String[] parts = (entry == null ? "" : entry).split(":");
                int trackIndex = parts.length > 0 ? parseIntOrZero(parts[0]) : 0;
                int channel = parts.length > 1 ? parseIntOrZero(parts[1]) : 1;
                groups.add(new MidiTrackChannelGroup(trackIndex, Math.max(1, Math.min(16, channel))));
            }
        }
        Collections.sort(groups, new Comparator<MidiTrackChannelGroup>() {
            @Override
            public int compare(MidiTrackChannelGroup left, MidiTrackChannelGroup right) {
                if (left.getTrackIndex() == right.getTrackIndex()) {
                    return Integer.valueOf(left.getChannel()).compareTo(Integer.valueOf(right.getChannel()));
                }
                return Integer.valueOf(left.getTrackIndex()).compareTo(Integer.valueOf(right.getTrackIndex()));
            }
        });
        return Collections.unmodifiableList(groups);
    }

    public static SmfHeaderParseResult parseSmfHeader(byte[] midiBytes) {
        List<MidiImportDiagnostic> diagnostics = new ArrayList<MidiImportDiagnostic>();
        if (midiBytes == null || midiBytes.length < 14) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE", "SMF header is too short."));
            return new SmfHeaderParseResult(null, diagnostics);
        }
        if (!"MThd".equals(readAscii(midiBytes, 0, 4))) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE", "Missing MThd header chunk."));
            return new SmfHeaderParseResult(null, diagnostics);
        }
        Long headerLength = readUint32Be(midiBytes, 4);
        Integer format = readUint16Be(midiBytes, 8);
        Integer trackCount = readUint16Be(midiBytes, 10);
        Integer division = readUint16Be(midiBytes, 12);
        if (headerLength == null || format == null || trackCount == null || division == null
                || headerLength.longValue() < 6L) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE", "Invalid SMF header fields."));
            return new SmfHeaderParseResult(null, diagnostics);
        }
        long nextOffsetLong = 8L + headerLength.longValue();
        if (nextOffsetLong > midiBytes.length) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE", "Header chunk length exceeds file size."));
            return new SmfHeaderParseResult(null, diagnostics);
        }
        if (format.intValue() != 0 && format.intValue() != 1) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_UNSUPPORTED_FORMAT",
                    "Unsupported SMF format " + format + ". Supported formats are 0 and 1."));
            return new SmfHeaderParseResult(null, diagnostics);
        }
        if ((division.intValue() & 0x8000) != 0) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_UNSUPPORTED_DIVISION",
                    "SMPTE time division is unsupported. Use PPQ-based MIDI files."));
            return new SmfHeaderParseResult(null, diagnostics);
        }
        int ticksPerQuarter = division.intValue() & 0x7fff;
        if (ticksPerQuarter <= 0) {
            diagnostics.add(new MidiImportDiagnostic("MIDI_INVALID_FILE", "PPQ must be a positive integer."));
            return new SmfHeaderParseResult(null, diagnostics);
        }
        return new SmfHeaderParseResult(
                new ParsedSmfHeader(format.intValue(), trackCount.intValue(), ticksPerQuarter, (int) nextOffsetLong),
                diagnostics);
    }

    public static SmfParseSummary parseTrackSummary(byte[] trackData, int trackIndex) {
        List<SmfImportedNote> notes = new ArrayList<SmfImportedNote>();
        Set<Integer> channels = new LinkedHashSet<Integer>();
        String trackName = null;
        List<String> standardTitleCandidates = new ArrayList<String>();
        List<String> standardComposerCandidates = new ArrayList<String>();
        Map<String, Integer> programByTrackChannel = new LinkedHashMap<String, Integer>();
        List<MidiControllerEvent> controllerEvents = new ArrayList<MidiControllerEvent>();
        List<MidiTickTimeSignatureEvent> timeSignatureEvents = new ArrayList<MidiTickTimeSignatureEvent>();
        List<MidiTickKeySignatureEvent> keySignatureEvents = new ArrayList<MidiTickKeySignatureEvent>();
        List<MidiTempoEvent> tempoEvents = new ArrayList<MidiTempoEvent>();
        List<MksSysExChunk> mksSysExChunks = new ArrayList<MksSysExChunk>();
        List<String> mksTextMetaLines = new ArrayList<String>();
        List<MidiImportDiagnostic> parseWarnings = new ArrayList<MidiImportDiagnostic>();
        Map<String, List<ActiveNoteStart>> activeNoteStartTicks = new LinkedHashMap<String, List<ActiveNoteStart>>();
        int cursor = 0;
        int absTick = 0;
        Integer runningStatus = null;
        byte[] data = trackData == null ? new byte[0] : trackData;

        while (cursor < data.length) {
            VariableLengthValue delta = readVariableLengthAt(data, cursor);
            if (delta == null) {
                parseWarnings.add(new MidiImportDiagnostic("MIDI_EVENT_DROPPED",
                        "Invalid variable-length delta time in track; remaining events were dropped."));
                break;
            }
            cursor = delta.getNext();
            absTick += Math.max(0, delta.getValue());
            if (cursor >= data.length) {
                break;
            }

            int statusByte = data[cursor] & 0xff;
            if (statusByte < 0x80) {
                if (runningStatus == null) {
                    parseWarnings.add(new MidiImportDiagnostic("MIDI_EVENT_DROPPED",
                            "Running status without previous status; event dropped."));
                    break;
                }
                statusByte = runningStatus.intValue();
            } else {
                cursor += 1;
                runningStatus = statusByte < 0xf0 ? Integer.valueOf(statusByte) : null;
            }

            if (statusByte == 0xff) {
                if (cursor >= data.length) {
                    break;
                }
                int metaType = data[cursor] & 0xff;
                cursor += 1;
                VariableLengthValue metaLen = readVariableLengthAt(data, cursor);
                if (metaLen == null) {
                    break;
                }
                int payloadStart = metaLen.getNext();
                long payloadEndLong = (long) payloadStart + metaLen.getValue();
                if (payloadEndLong > data.length) {
                    parseWarnings.add(new MidiImportDiagnostic("MIDI_EVENT_DROPPED",
                            "Meta event length overflow; remaining events were dropped."));
                    break;
                }
                int payloadEnd = (int) payloadEndLong;
                if (metaType == 0x58 && metaLen.getValue() >= 2) {
                    int beats = data[payloadStart] & 0xff;
                    int beatTypePow = data[payloadStart + 1] & 0xff;
                    double beatType = Math.pow(2.0d, beatTypePow);
                    if (beats > 0 && !Double.isNaN(beatType) && !Double.isInfinite(beatType) && beatType > 0) {
                        timeSignatureEvents.add(new MidiTickTimeSignatureEvent(absTick, beats, (int) Math.round(beatType)));
                    }
                } else if (metaType == 0x59 && metaLen.getValue() >= 2) {
                    int sfRaw = data[payloadStart] & 0xff;
                    int sf = sfRaw >= 0x80 ? sfRaw - 0x100 : sfRaw;
                    int mi = data[payloadStart + 1] & 0xff;
                    int fifths = Math.max(-7, Math.min(7, sf));
                    String mode = mi == 1 ? "minor" : "major";
                    keySignatureEvents.add(new MidiTickKeySignatureEvent(absTick, fifths, mode));
                } else if (metaType == 0x51 && metaLen.getValue() >= 3) {
                    int microsPerQuarter = ((data[payloadStart] & 0xff) << 16)
                            | ((data[payloadStart + 1] & 0xff) << 8) | (data[payloadStart + 2] & 0xff);
                    if (microsPerQuarter > 0) {
                        tempoEvents.add(new MidiTempoEvent(absTick, clampTempo(60000000.0d / microsPerQuarter)));
                    }
                } else if (metaType == 0x01 || metaType == 0x02 || metaType == 0x03) {
                    byte[] payloadBytes = Arrays.copyOfRange(data, payloadStart, payloadEnd);
                    String text = decodeMetaTextBytes(payloadBytes).trim();
                    if (metaType == 0x03 && text.length() > 0 && trackName == null) {
                        trackName = text;
                    }
                    if (text.startsWith("mks:")) {
                        mksTextMetaLines.add(text);
                    } else if (text.length() > 0) {
                        if (metaType == 0x01) {
                            String parsedTitle = parseStandardTitleFromMetaText(text);
                            if (parsedTitle.length() > 0) {
                                standardTitleCandidates.add(parsedTitle);
                            }
                            String parsedComposer = parseStandardComposerFromMetaText(text);
                            if (parsedComposer.length() > 0) {
                                standardComposerCandidates.add(parsedComposer);
                            }
                        }
                        if (metaType == 0x02) {
                            String parsedComposer = parseStandardComposerFromMetaText(text);
                            if (parsedComposer.length() > 0) {
                                standardComposerCandidates.add(parsedComposer);
                            }
                        }
                    }
                }
                cursor = payloadEnd;
                continue;
            }

            if (statusByte == 0xf0 || statusByte == 0xf7) {
                VariableLengthValue sysExLen = readVariableLengthAt(data, cursor);
                if (sysExLen == null) {
                    break;
                }
                int payloadStart = sysExLen.getNext();
                long payloadEndLong = (long) payloadStart + sysExLen.getValue();
                if (payloadEndLong > data.length) {
                    parseWarnings.add(new MidiImportDiagnostic("MIDI_EVENT_DROPPED",
                            "SysEx event length overflow; remaining events were dropped."));
                    break;
                }
                int payloadEnd = (int) payloadEndLong;
                if (statusByte == 0xf0) {
                    MksSysExChunk parsedChunk = parseMksSysExChunk(Arrays.copyOfRange(data, payloadStart, payloadEnd));
                    if (parsedChunk != null) {
                        mksSysExChunks.add(parsedChunk);
                    }
                }
                cursor = payloadEnd;
                if (cursor > data.length) {
                    parseWarnings.add(new MidiImportDiagnostic("MIDI_EVENT_DROPPED",
                            "SysEx event length overflow; remaining events were dropped."));
                    break;
                }
                continue;
            }

            int messageType = statusByte & 0xf0;
            int channel = (statusByte & 0x0f) + 1;
            channels.add(Integer.valueOf(channel));
            int dataLen = messageType == 0xc0 || messageType == 0xd0 ? 1 : 2;
            if (cursor + dataLen > data.length) {
                parseWarnings.add(new MidiImportDiagnostic("MIDI_EVENT_DROPPED",
                        "Channel event data is truncated; remaining events were dropped."));
                break;
            }
            int data1 = data[cursor] & 0xff;
            int data2 = dataLen == 2 ? data[cursor + 1] & 0xff : 0;
            cursor += dataLen;

            if (messageType == 0xc0) {
                programByTrackChannel.put(trackIndex + ":" + channel, Integer.valueOf(data1 + 1));
                continue;
            }

            if (messageType == 0xb0 && (data1 == 7 || data1 == 11)) {
                controllerEvents.add(new MidiControllerEvent(absTick, channel, data1,
                        Math.max(0, Math.min(127, Math.round(data2)))));
                continue;
            }

            if (messageType != 0x80 && messageType != 0x90) {
                continue;
            }
            String key = channel + ":" + data1;
            if (messageType == 0x90 && data2 > 0) {
                List<ActiveNoteStart> bucket = activeNoteStartTicks.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<ActiveNoteStart>();
                    activeNoteStartTicks.put(key, bucket);
                }
                bucket.add(new ActiveNoteStart(absTick, clampVelocity(data2)));
                continue;
            }
            List<ActiveNoteStart> bucket = activeNoteStartTicks.get(key);
            ActiveNoteStart started = bucket == null || bucket.isEmpty() ? null : bucket.remove(0);
            if (bucket != null && bucket.isEmpty()) {
                activeNoteStartTicks.remove(key);
            }
            if (started == null) {
                parseWarnings.add(new MidiImportDiagnostic("MIDI_NOTE_PAIR_BROKEN",
                        "Note off without matching note on (ch " + channel + ", note " + data1 + ")."));
                continue;
            }
            int startTick = started.getStartTick();
            int endTick = Math.max(startTick + 1, absTick);
            notes.add(new SmfImportedNote(trackIndex, channel, data1, startTick, endTick, started.getVelocity()));
        }

        for (Map.Entry<String, List<ActiveNoteStart>> entry : activeNoteStartTicks.entrySet()) {
            String[] keyParts = entry.getKey().split(":");
            int channel = keyParts.length > 0 ? parseIntOrZero(keyParts[0]) : 0;
            int note = keyParts.length > 1 ? parseIntOrZero(keyParts[1]) : 0;
            for (ActiveNoteStart started : entry.getValue()) {
                parseWarnings.add(new MidiImportDiagnostic("MIDI_NOTE_PAIR_BROKEN",
                        "Note on without matching note off (ch " + channel + ", note " + note + ", start "
                                + started.getStartTick() + ")."));
            }
        }

        return new SmfParseSummary(notes, channels, trackName, standardTitleCandidates, standardComposerCandidates,
                programByTrackChannel, controllerEvents, timeSignatureEvents, keySignatureEvents, tempoEvents,
                assembleMksSysExPayloads(mksSysExChunks), mksTextMetaLines, parseWarnings);
    }

    public static QuantizedImportedNotesResult quantizeImportedNotes(List<SmfImportedNote> notes, int ticksPerQuarter,
            String grid, boolean tripletAwareQuantize) {
        List<MidiImportDiagnostic> warnings = new ArrayList<MidiImportDiagnostic>();
        ImportQuantizeResolution resolved = resolveImportQuantizeTick(notes, ticksPerQuarter, grid,
                tripletAwareQuantize);
        int qTick = resolved.getQTick();
        int divisions = resolved.getDivisions();
        List<ImportedQuantizedNote> quantized = new ArrayList<ImportedQuantizedNote>();
        if (notes != null) {
            for (SmfImportedNote note : notes) {
                if (note == null) {
                    continue;
                }
                int startTick = Math.max(0, Math.round(note.getStartTick() / (float) qTick) * qTick);
                int endTick = Math.max(startTick + qTick, Math.round(note.getEndTick() / (float) qTick) * qTick);
                if (endTick <= startTick) {
                    endTick = startTick + qTick;
                    warnings.add(new MidiImportDiagnostic("MIDI_QUANTIZE_CLAMPED",
                            "Quantized note duration was clamped (ch " + note.getChannel() + ", note "
                                    + note.getMidi() + ")."));
                }
                quantized.add(new ImportedQuantizedNote(note.getTrackIndex(), note.getChannel(), note.getMidi(),
                        startTick, endTick, note.getVelocity()));
            }
        }
        return new QuantizedImportedNotesResult(quantized, warnings, qTick, divisions);
    }

    public static List<ImportedQuantizedNote> applyImportedControllerVelocityScale(List<ImportedQuantizedNote> notes,
            List<MidiControllerEvent> controllerEvents) {
        if (notes == null || notes.isEmpty() || controllerEvents == null || controllerEvents.isEmpty()) {
            return notes == null ? Collections.<ImportedQuantizedNote>emptyList() : notes;
        }
        Map<String, ControllerValueBucket> controlByTrackChannel = new LinkedHashMap<String, ControllerValueBucket>();
        for (MidiControllerEvent event : controllerEvents) {
            if (event == null) {
                continue;
            }
            String key = event.getTrackIndex() + ":" + event.getChannel();
            ControllerValueBucket bucket = controlByTrackChannel.get(key);
            if (bucket == null) {
                bucket = new ControllerValueBucket();
                controlByTrackChannel.put(key, bucket);
            }
            List<ControllerValuePoint> target = event.getControllerNumber() == 7 ? bucket.getCc7() : bucket.getCc11();
            target.add(new ControllerValuePoint(Math.max(0, event.getTick()),
                    Math.max(0, Math.min(127, event.getControllerValue()))));
        }
        for (ControllerValueBucket bucket : controlByTrackChannel.values()) {
            Collections.sort(bucket.getCc7(), new Comparator<ControllerValuePoint>() {
                @Override
                public int compare(ControllerValuePoint left, ControllerValuePoint right) {
                    return Integer.valueOf(left.getTick()).compareTo(Integer.valueOf(right.getTick()));
                }
            });
            Collections.sort(bucket.getCc11(), new Comparator<ControllerValuePoint>() {
                @Override
                public int compare(ControllerValuePoint left, ControllerValuePoint right) {
                    return Integer.valueOf(left.getTick()).compareTo(Integer.valueOf(right.getTick()));
                }
            });
        }

        List<ImportedQuantizedNote> out = new ArrayList<ImportedQuantizedNote>();
        for (ImportedQuantizedNote note : notes) {
            if (note == null) {
                continue;
            }
            String key = note.getTrackIndex() + ":" + note.getChannel();
            ControllerValueBucket bucket = controlByTrackChannel.get(key);
            if (bucket == null) {
                out.add(note);
                continue;
            }
            int cc7 = resolveCcValueAtTick(bucket.getCc7(), note.getStartTick());
            int cc11 = resolveCcValueAtTick(bucket.getCc11(), note.getStartTick());
            int scaled = (int) Math.round(note.getVelocity() * (cc7 / 127.0d) * (cc11 / 127.0d));
            out.add(new ImportedQuantizedNote(note.getTrackIndex(), note.getChannel(), note.getMidi(),
                    note.getStartTick(), note.getEndTick(), clampVelocity(Math.max(1, scaled))));
        }
        return out;
    }

    public static List<ImportedVoiceCluster> allocateAutoVoices(List<ImportedQuantizedNote> notes,
            List<MidiImportDiagnostic> warnings) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, List<ImportedQuantizedNote>> clustersByStart = new LinkedHashMap<Integer, List<ImportedQuantizedNote>>();
        for (ImportedQuantizedNote note : notes) {
            if (note == null) {
                continue;
            }
            Integer key = Integer.valueOf(note.getStartTick());
            List<ImportedQuantizedNote> bucket = clustersByStart.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ImportedQuantizedNote>();
                clustersByStart.put(key, bucket);
            }
            bucket.add(note);
        }
        List<Integer> starts = new ArrayList<Integer>(clustersByStart.keySet());
        Collections.sort(starts);
        List<AutoVoiceState> voices = new ArrayList<AutoVoiceState>();
        List<ImportedVoiceCluster> out = new ArrayList<ImportedVoiceCluster>();

        for (Integer startValue : starts) {
            int start = startValue.intValue();
            List<ImportedQuantizedNote> clusterNotes = new ArrayList<ImportedQuantizedNote>(
                    clustersByStart.get(startValue));
            Collections.sort(clusterNotes, new Comparator<ImportedQuantizedNote>() {
                @Override
                public int compare(ImportedQuantizedNote left, ImportedQuantizedNote right) {
                    return Integer.valueOf(left.getMidi()).compareTo(Integer.valueOf(right.getMidi()));
                }
            });
            if (clusterNotes.isEmpty()) {
                continue;
            }
            int clusterEnd = 0;
            for (ImportedQuantizedNote note : clusterNotes) {
                clusterEnd = Math.max(clusterEnd, note.getEndTick());
            }
            int representativePitch = clusterNotes.get(clusterNotes.size() / 2).getMidi();

            int bestVoice = -1;
            int bestGap = Integer.MAX_VALUE;
            int bestPitchJump = Integer.MAX_VALUE;
            for (int index = 0; index < voices.size(); index++) {
                AutoVoiceState voice = voices.get(index);
                if (voice.getLastEnd() > start) {
                    continue;
                }
                int gap = start - voice.getLastEnd();
                int pitchJump = Math.abs(representativePitch - voice.getLastPitch());
                if (gap < bestGap || (gap == bestGap && pitchJump < bestPitchJump)) {
                    bestVoice = index;
                    bestGap = gap;
                    bestPitchJump = pitchJump;
                }
            }
            if (bestVoice < 0) {
                bestVoice = voices.size();
                voices.add(new AutoVoiceState(clusterEnd, representativePitch));
            } else {
                voices.set(bestVoice, new AutoVoiceState(clusterEnd, representativePitch));
            }
            out.add(new ImportedVoiceCluster(bestVoice + 1, start, clusterEnd, clusterNotes));
        }

        if (voices.size() > 1 && warnings != null) {
            warnings.add(new MidiImportDiagnostic("MIDI_POLYPHONY_VOICE_ASSIGNED",
                    "Auto voice split assigned " + voices.size() + " voices."));
        }
        if (voices.size() > 8 && warnings != null) {
            warnings.add(new MidiImportDiagnostic("MIDI_POLYPHONY_VOICE_OVERFLOW",
                    "Auto voice split generated " + voices.size() + " voices (high density)."));
        }
        return out;
    }

    public static List<ImportedVoiceNoteSegment> splitClustersToMeasureSegments(
            SplitClustersToMeasureSegmentsParams params) {
        List<ImportedVoiceNoteSegment> out = new ArrayList<ImportedVoiceNoteSegment>();
        if (params == null || params.getClusters().isEmpty()) {
            return out;
        }
        int ticksPerQuarter = Math.max(1, params.getTicksPerQuarter());
        int divisions = Math.max(1, params.getDivisions());
        int measureTicks = Math.max(1, params.getMeasureTicks());
        int pickupTicks = Math.max(0, params.getPickupTicks());
        Integer previousStaff = null;

        for (ImportedVoiceCluster cluster : params.getClusters()) {
            if (cluster == null) {
                continue;
            }
            int minClusterKey = 60;
            int maxClusterKey = 60;
            boolean hasClusterKey = false;
            for (ImportedQuantizedNote note : cluster.getNotes()) {
                if (note == null) {
                    continue;
                }
                if (!hasClusterKey) {
                    minClusterKey = note.getMidi();
                    maxClusterKey = note.getMidi();
                    hasClusterKey = true;
                } else {
                    minClusterKey = Math.min(minClusterKey, note.getMidi());
                    maxClusterKey = Math.max(maxClusterKey, note.getMidi());
                }
            }
            int clusterStaff = params.isDrum() ? 1
                    : params.isUseGrandStaff()
                            ? StaffClefPolicy.pickStaffForClusterWithHysteresis(minClusterKey, maxClusterKey,
                                    previousStaff)
                            : 1;
            if (!params.isDrum()) {
                previousStaff = Integer.valueOf(clusterStaff);
            }
            for (ImportedQuantizedNote note : cluster.getNotes()) {
                if (note == null) {
                    continue;
                }
                int segmentStart = note.getStartTick();
                while (segmentStart < note.getEndTick()) {
                    int measureIndex = measureIndexAtTick(segmentStart, measureTicks, pickupTicks);
                    int measureEndTick = nextMeasureBoundaryTick(segmentStart, measureTicks, pickupTicks);
                    int segmentEnd = Math.min(note.getEndTick(), measureEndTick);
                    int startInMeasureTick = segmentStart - measureStartTick(measureIndex, measureTicks, pickupTicks);
                    int startDiv = toDiv(startInMeasureTick, divisions, ticksPerQuarter);
                    int durDiv = Math.max(1, toDiv(segmentEnd - segmentStart, divisions, ticksPerQuarter));
                    out.add(new ImportedVoiceNoteSegment(measureIndex, cluster.getVoice(), clusterStaff, startDiv,
                            durDiv, note.getMidi(), note.getVelocity(), note.getTrackIndex(), note.getChannel(),
                            segmentStart, segmentEnd));
                    segmentStart = segmentEnd;
                }
            }
        }
        return out;
    }

    public static MidiPartSegmentLayout resolveMidiPartSegmentLayout(List<ImportedVoiceCluster> clusters,
            int ticksPerQuarter, int divisions, MidiPartMeasureLayout measureLayout, boolean drum,
            boolean initialGrandStaff) {
        MidiPartMeasureLayout layout = measureLayout == null
                ? new MidiPartMeasureLayout(1, 1, 0, 0, 1)
                : measureLayout;
        boolean useGrandStaff = initialGrandStaff;
        List<ImportedVoiceNoteSegment> splitSegments = splitClustersToMeasureSegments(
                new SplitClustersToMeasureSegmentsParams(clusters, ticksPerQuarter, divisions,
                        layout.getMeasureTicks(), layout.getPickupMeasureTicks(), drum, useGrandStaff));
        if (!drum && useGrandStaff) {
            boolean hasUpper = false;
            boolean hasLower = false;
            for (ImportedVoiceNoteSegment segment : splitSegments) {
                if (segment == null) {
                    continue;
                }
                if (segment.getStaff() == 1) {
                    hasUpper = true;
                }
                if (segment.getStaff() == 2) {
                    hasLower = true;
                }
            }
            if (!hasUpper || !hasLower) {
                useGrandStaff = false;
                List<ImportedVoiceNoteSegment> remapped = new ArrayList<ImportedVoiceNoteSegment>();
                for (ImportedVoiceNoteSegment segment : splitSegments) {
                    if (segment == null) {
                        continue;
                    }
                    remapped.add(new ImportedVoiceNoteSegment(segment.getMeasureIndex(), segment.getVoice(), 1,
                            segment.getStartDiv(), segment.getDurDiv(), segment.getMidi(), segment.getVelocity(),
                            segment.getTrackIndex(), segment.getChannel(), segment.getStartTick(),
                            segment.getEndTick()));
                }
                splitSegments = remapped;
            }
        }
        return new MidiPartSegmentLayout(splitSegments, groupMidiSegmentsByMeasure(splitSegments), useGrandStaff);
    }

    public static Map<Integer, List<ImportedVoiceNoteSegment>> groupMidiSegmentsByMeasure(
            List<ImportedVoiceNoteSegment> splitSegments) {
        Map<Integer, List<ImportedVoiceNoteSegment>> byMeasure =
                new LinkedHashMap<Integer, List<ImportedVoiceNoteSegment>>();
        if (splitSegments != null) {
            for (ImportedVoiceNoteSegment segment : splitSegments) {
                if (segment == null) {
                    continue;
                }
                Integer key = Integer.valueOf(segment.getMeasureIndex());
                List<ImportedVoiceNoteSegment> bucket = byMeasure.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<ImportedVoiceNoteSegment>();
                    byMeasure.put(key, bucket);
                }
                bucket.add(segment);
            }
        }
        Map<Integer, List<ImportedVoiceNoteSegment>> out =
                new LinkedHashMap<Integer, List<ImportedVoiceNoteSegment>>();
        for (Map.Entry<Integer, List<ImportedVoiceNoteSegment>> entry : byMeasure.entrySet()) {
            out.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<ImportedVoiceNoteSegment>(
                    entry.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }

    private static int toDiv(int ticks, int divisions, int ticksPerQuarter) {
        return Math.max(0, Math.round((ticks * (float) divisions) / Math.max(1, ticksPerQuarter)));
    }

    private static int measureStartTick(int measureIndex, int measureTicks, int pickupTicks) {
        if (pickupTicks <= 0) {
            return Math.max(0, measureIndex) * measureTicks;
        }
        if (measureIndex <= 0) {
            return 0;
        }
        return pickupTicks + (measureIndex - 1) * measureTicks;
    }

    private static int measureIndexAtTick(int tick, int measureTicks, int pickupTicks) {
        int safeTick = Math.max(0, tick);
        if (pickupTicks <= 0) {
            return safeTick / measureTicks;
        }
        if (safeTick < pickupTicks) {
            return 0;
        }
        return 1 + ((safeTick - pickupTicks) / measureTicks);
    }

    private static int nextMeasureBoundaryTick(int tick, int measureTicks, int pickupTicks) {
        int safeTick = Math.max(0, tick);
        if (pickupTicks <= 0) {
            int index = safeTick / measureTicks;
            return (index + 1) * measureTicks;
        }
        if (safeTick < pickupTicks) {
            return pickupTicks;
        }
        int index = (safeTick - pickupTicks) / measureTicks;
        return pickupTicks + (index + 1) * measureTicks;
    }

    public static List<DurationNotation> durationNotationCandidates(int divisions) {
        List<DurationNotation> out = new ArrayList<DurationNotation>();
        int safeDivisions = Math.max(1, divisions);
        out.add(new DurationNotation("whole", 0, 4.0d, 4.0d * safeDivisions));
        out.add(new DurationNotation("whole", 1, 6.0d, 6.0d * safeDivisions));
        out.add(new DurationNotation("whole", 2, 7.0d, 7.0d * safeDivisions));
        out.add(new DurationNotation("half", 0, 2.0d, 2.0d * safeDivisions));
        out.add(new DurationNotation("half", 1, 3.0d, 3.0d * safeDivisions));
        out.add(new DurationNotation("half", 2, 3.5d, 3.5d * safeDivisions));
        out.add(new DurationNotation("quarter", 0, 1.0d, 1.0d * safeDivisions));
        out.add(new DurationNotation("quarter", 1, 1.5d, 1.5d * safeDivisions));
        out.add(new DurationNotation("quarter", 2, 1.75d, 1.75d * safeDivisions));
        out.add(new DurationNotation("eighth", 0, 0.5d, 0.5d * safeDivisions));
        out.add(new DurationNotation("eighth", 1, 0.75d, 0.75d * safeDivisions));
        out.add(new DurationNotation("eighth", 2, 0.875d, 0.875d * safeDivisions));
        out.add(new DurationNotation("16th", 0, 0.25d, 0.25d * safeDivisions));
        out.add(new DurationNotation("16th", 1, 0.375d, 0.375d * safeDivisions));
        out.add(new DurationNotation("16th", 2, 0.4375d, 0.4375d * safeDivisions));
        out.add(new DurationNotation("32nd", 0, 0.125d, 0.125d * safeDivisions));
        out.add(new DurationNotation("32nd", 1, 0.1875d, 0.1875d * safeDivisions));
        out.add(new DurationNotation("32nd", 2, 0.21875d, 0.21875d * safeDivisions));
        out.add(new DurationNotation("64th", 0, 0.0625d, 0.0625d * safeDivisions));
        out.add(new DurationNotation("64th", 1, 0.09375d, 0.09375d * safeDivisions));
        out.add(new DurationNotation("64th", 2, 0.109375d, 0.109375d * safeDivisions));
        return out;
    }

    public static DurationNotation resolveDurationNotation(double durDiv, double divisions) {
        if (!Double.isFinite(durDiv) || !Double.isFinite(divisions) || durDiv <= 0 || divisions <= 0) {
            return null;
        }
        double tolerance = 0.000001d;
        for (DurationNotation candidate : durationNotationCandidates((int) Math.round(divisions))) {
            if (Math.abs(candidate.getDurDiv() - durDiv) <= tolerance) {
                return candidate;
            }
        }
        return null;
    }

    public static List<DurationNotation> splitDurationNotations(double durDiv, double divisions) {
        DurationNotation single = resolveDurationNotation(durDiv, divisions);
        if (single != null) {
            return Collections.singletonList(single);
        }
        if (!Double.isFinite(durDiv) || !Double.isFinite(divisions) || durDiv <= 0 || divisions <= 0) {
            return Collections.emptyList();
        }
        int roundedDur = (int) Math.round(durDiv);
        if (Math.abs(durDiv - roundedDur) > 0.000001d) {
            return Collections.emptyList();
        }
        List<DurationNotation> candidates = new ArrayList<DurationNotation>();
        for (DurationNotation candidate : durationNotationCandidates((int) Math.round(divisions))) {
            double roundedCandidateDur = Math.round(candidate.getDurDiv());
            if (Math.abs(candidate.getDurDiv() - roundedCandidateDur) <= 0.000001d && roundedCandidateDur > 0) {
                candidates.add(new DurationNotation(candidate.getType(), candidate.getDots(), candidate.getQ(),
                        roundedCandidateDur));
            }
        }
        Collections.sort(candidates, new Comparator<DurationNotation>() {
            @Override
            public int compare(DurationNotation left, DurationNotation right) {
                return Double.valueOf(right.getDurDiv()).compareTo(Double.valueOf(left.getDurDiv()));
            }
        });
        List<List<DurationNotation>> best = new ArrayList<List<DurationNotation>>();
        for (int index = 0; index <= roundedDur; index++) {
            best.add(null);
        }
        best.set(0, new ArrayList<DurationNotation>());
        for (int target = 1; target <= roundedDur; target++) {
            for (DurationNotation candidate : candidates) {
                int candidateDur = (int) Math.round(candidate.getDurDiv());
                if (candidateDur > target) {
                    continue;
                }
                List<DurationNotation> previous = best.get(target - candidateDur);
                if (previous == null) {
                    continue;
                }
                List<DurationNotation> composed = new ArrayList<DurationNotation>(previous);
                composed.add(candidate);
                if (best.get(target) == null || composed.size() < best.get(target).size()) {
                    best.set(target, composed);
                }
            }
        }
        List<DurationNotation> result = best.get(roundedDur);
        return result == null ? Collections.<DurationNotation>emptyList() : result;
    }

    public static String buildTypeXmlFromNotation(DurationNotation notation) {
        if (notation == null) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        xml.append("<type>").append(notation.getType()).append("</type>");
        for (int index = 0; index < notation.getDots(); index++) {
            xml.append("<dot/>");
        }
        return xml.toString();
    }

    public static int beamLevelFromNotationType(String type) {
        if ("eighth".equals(type)) {
            return 1;
        }
        if ("16th".equals(type)) {
            return 2;
        }
        if ("32nd".equals(type)) {
            return 3;
        }
        if ("64th".equals(type)) {
            return 4;
        }
        return 0;
    }

    public static String buildTieXml(boolean tieStart, boolean tieStop, boolean withStaccato) {
        if (!tieStart && !tieStop && !withStaccato) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        if (tieStop) {
            xml.append("<tie type=\"stop\"/>");
        }
        if (tieStart) {
            xml.append("<tie type=\"start\"/>");
        }
        xml.append("<notations>");
        if (withStaccato) {
            xml.append("<articulations><staccato/></articulations>");
        }
        if (tieStop) {
            xml.append("<tied type=\"stop\"/>");
        }
        if (tieStart) {
            xml.append("<tied type=\"start\"/>");
        }
        xml.append("</notations>");
        return xml.toString();
    }

    public static String buildRestXml(double durDiv, int voice, int outputStaff, int divisions) {
        List<DurationNotation> chunks = splitDurationNotations(durDiv, divisions);
        if (chunks.isEmpty()) {
            return "<note><rest/><duration>" + formatMidiNumber(durDiv) + "</duration><voice>" + voice
                    + "</voice><staff>" + outputStaff + "</staff></note>";
        }
        StringBuilder xml = new StringBuilder();
        for (DurationNotation chunk : chunks) {
            xml.append("<note><rest/><duration>").append(formatMidiNumber(chunk.getDurDiv())).append("</duration>")
                    .append(buildTypeXmlFromNotation(chunk)).append("<voice>").append(voice).append("</voice><staff>")
                    .append(outputStaff).append("</staff></note>");
        }
        return xml.toString();
    }

    public static String prettyPrintXml(String xml) {
        String compact = (xml == null ? "" : xml).replaceAll(">\\s+<", "><").trim();
        if (compact.length() == 0) {
            return "";
        }
        String[] split = compact.replaceAll("(>)(<)(/*)", "$1\n$2$3").split("\n");
        int indent = 0;
        List<String> lines = new ArrayList<String>();
        for (String rawToken : split) {
            String token = rawToken == null ? "" : rawToken.trim();
            if (token.length() == 0) {
                continue;
            }
            if (token.matches("^</.*")) {
                indent = Math.max(0, indent - 1);
            }
            StringBuilder pad = new StringBuilder();
            for (int index = 0; index < indent; index++) {
                pad.append("  ");
            }
            lines.add(pad.toString() + token);
            boolean isOpening = token.matches("^<[^!?/][^>]*>$");
            boolean isSelfClosing = token.matches(".*/>$");
            if (isOpening && !isSelfClosing) {
                indent += 1;
            }
        }
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                out.append('\n');
            }
            out.append(lines.get(index));
        }
        return out.toString();
    }

    public static String toHex(double value, int width) {
        int safe = Math.max(0, (int) Math.round(value));
        String hex = Integer.toHexString(safe).toUpperCase();
        return "0x" + padLeft(hex, Math.max(0, width), '0');
    }

    public static String buildMeasureMidiMetaMiscXml(List<ImportedVoiceNoteSegment> measureSegments) {
        if (measureSegments == null || measureSegments.isEmpty()) {
            return "";
        }
        List<ImportedVoiceNoteSegment> sorted = new ArrayList<ImportedVoiceNoteSegment>();
        for (ImportedVoiceNoteSegment segment : measureSegments) {
            if (segment != null) {
                sorted.add(segment);
            }
        }
        if (sorted.isEmpty()) {
            return "";
        }
        Collections.sort(sorted, new Comparator<ImportedVoiceNoteSegment>() {
            @Override
            public int compare(ImportedVoiceNoteSegment left, ImportedVoiceNoteSegment right) {
                if (left.getStartDiv() == right.getStartDiv()) {
                    if (left.getMidi() == right.getMidi()) {
                        return Integer.valueOf(left.getVoice()).compareTo(Integer.valueOf(right.getVoice()));
                    }
                    return Integer.valueOf(left.getMidi()).compareTo(Integer.valueOf(right.getMidi()));
                }
                return Integer.valueOf(left.getStartDiv()).compareTo(Integer.valueOf(right.getStartDiv()));
            }
        });
        StringBuilder xml = new StringBuilder();
        xml.append("<attributes><miscellaneous>");
        xml.append("<miscellaneous-field name=\"mks:dbg:midi:meta:count\">").append(toHex(sorted.size(), 4))
                .append("</miscellaneous-field>");
        for (int index = 0; index < sorted.size(); index++) {
            ImportedVoiceNoteSegment segment = sorted.get(index);
            StringBuilder payload = new StringBuilder();
            payload.append("idx=").append(toHex(index, 4));
            payload.append(";tr=").append(toHex(segment.getTrackIndex(), 2));
            payload.append(";ch=").append(toHex(segment.getChannel(), 2));
            payload.append(";v=").append(toHex(segment.getVoice(), 2));
            payload.append(";stf=").append(toHex(segment.getStaff(), 2));
            payload.append(";key=").append(toHex(segment.getMidi(), 2));
            payload.append(";vel=").append(toHex(segment.getVelocity(), 2));
            payload.append(";sd=").append(toHex(segment.getStartDiv(), 4));
            payload.append(";dd=").append(toHex(segment.getDurDiv(), 4));
            payload.append(";tk0=").append(toHex(segment.getStartTick(), 6));
            payload.append(";tk1=").append(toHex(segment.getEndTick(), 6));
            xml.append("<miscellaneous-field name=\"mks:dbg:midi:meta:").append(padLeft(Integer.toString(index + 1), 4, '0'))
                    .append("\">").append(payload).append("</miscellaneous-field>");
        }
        xml.append("</miscellaneous></attributes>");
        return xml.toString();
    }

    public static String buildMidiSourceMiscXml(byte[] midiBytes) {
        byte[] bytes = midiBytes == null ? new byte[0] : midiBytes;
        if (bytes.length == 0) {
            return "";
        }
        StringBuilder hex = new StringBuilder();
        for (byte value : bytes) {
            hex.append(padLeft(Integer.toHexString(value & 0xff).toUpperCase(), 2, '0'));
        }
        int chunkSize = 240;
        int maxChunks = 512;
        List<String> chunks = new ArrayList<String>();
        for (int index = 0; index < hex.length() && chunks.size() < maxChunks; index += chunkSize) {
            chunks.add(hex.substring(index, Math.min(index + chunkSize, hex.length())));
        }
        int joinedLength = 0;
        for (String chunk : chunks) {
            joinedLength += chunk.length();
        }
        boolean truncated = joinedLength < hex.length();
        StringBuilder xml = new StringBuilder();
        xml.append("<attributes><miscellaneous>");
        xml.append("<miscellaneous-field name=\"mks:src:midi:raw-encoding\">hex-v1</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:midi:raw-bytes\">").append(bytes.length)
                .append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:midi:raw-hex-length\">").append(hex.length())
                .append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:midi:raw-chunks\">").append(chunks.size())
                .append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:midi:raw-truncated\">").append(truncated ? "1" : "0")
                .append("</miscellaneous-field>");
        for (int index = 0; index < chunks.size(); index++) {
            xml.append("<miscellaneous-field name=\"mks:src:midi:raw-")
                    .append(padLeft(Integer.toString(index + 1), 4, '0')).append("\">")
                    .append(chunks.get(index)).append("</miscellaneous-field>");
        }
        xml.append("</miscellaneous></attributes>");
        return xml.toString();
    }

    public static String buildMidiSysExMiscXml(List<String> payloads) {
        List<String> lines = new ArrayList<String>();
        if (payloads != null) {
            for (String payload : payloads) {
                String[] split = (payload == null ? "" : payload).split("\\r?\\n");
                for (String line : split) {
                    String trimmed = line.trim();
                    if (trimmed.length() > 0) {
                        lines.add(trimmed);
                    }
                }
            }
        }
        if (lines.isEmpty()) {
            return "";
        }
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim().toLowerCase();
            String value = line.substring(eq + 1).trim();
            if (key.length() == 0 || value.length() == 0) {
                continue;
            }
            map.put(key, value);
        }
        StringBuilder xml = new StringBuilder();
        xml.append("<attributes><miscellaneous>");
        xml.append("<miscellaneous-field name=\"mks:meta:midi:sysex:count\">").append(toHex(lines.size(), 4))
                .append("</miscellaneous-field>");
        for (int index = 0; index < lines.size(); index++) {
            xml.append("<miscellaneous-field name=\"mks:meta:midi:sysex:")
                    .append(padLeft(Integer.toString(index + 1), 4, '0')).append("\">")
                    .append(xmlEscape(lines.get(index))).append("</miscellaneous-field>");
        }
        String[] preferred = new String[] { "schema", "namespace", "app", "source", "tpq", "track-count",
                "event-count", "tempo-event-count", "timesig-event-count", "keysig-event-count",
                "control-event-count", "channel-count", "fingerprint-fnv1a32" };
        for (String key : preferred) {
            String value = map.get(key);
            if (value != null && value.length() > 0) {
                xml.append("<miscellaneous-field name=\"mks:meta:midi:sysex:").append(key).append("\">")
                        .append(xmlEscape(value)).append("</miscellaneous-field>");
            }
        }
        xml.append("</miscellaneous></attributes>");
        return xml.toString();
    }

    public static String buildMidiDiagMiscXml(List<MidiImportDiagnostic> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "";
        }
        int maxEntries = Math.min(256, warnings.size());
        StringBuilder xml = new StringBuilder();
        xml.append("<attributes><miscellaneous>");
        xml.append("<miscellaneous-field name=\"mks:diag:count\">").append(maxEntries)
                .append("</miscellaneous-field>");
        for (int index = 0; index < maxEntries; index++) {
            MidiImportDiagnostic warning = warnings.get(index);
            StringBuilder payload = new StringBuilder();
            payload.append("level=warn");
            payload.append(";code=").append(xmlEscape(warning == null ? "" : warning.getCode()));
            payload.append(";fmt=midi");
            payload.append(";message=").append(xmlEscape(warning == null ? "" : warning.getMessage()));
            xml.append("<miscellaneous-field name=\"mks:diag:").append(padLeft(Integer.toString(index + 1), 4, '0'))
                    .append("\">").append(payload).append("</miscellaneous-field>");
        }
        xml.append("</miscellaneous></attributes>");
        return xml.toString();
    }

    public static String buildMeasureVoiceXml(List<ImportedVoiceNoteSegment> segments, int voice, int sourceStaff,
            int outputStaff, int measureDiv, int beatDiv, boolean drum, int divisions, int keyFifths) {
        List<ImportedVoiceNoteSegment> voiceSegments = new ArrayList<ImportedVoiceNoteSegment>();
        if (segments != null) {
            for (ImportedVoiceNoteSegment segment : segments) {
                if (segment != null && segment.getVoice() == voice && segment.getStaff() == sourceStaff) {
                    voiceSegments.add(segment);
                }
            }
        }
        Collections.sort(voiceSegments, new Comparator<ImportedVoiceNoteSegment>() {
            @Override
            public int compare(ImportedVoiceNoteSegment left, ImportedVoiceNoteSegment right) {
                if (left.getStartDiv() == right.getStartDiv()) {
                    return Integer.valueOf(left.getMidi()).compareTo(Integer.valueOf(right.getMidi()));
                }
                return Integer.valueOf(left.getStartDiv()).compareTo(Integer.valueOf(right.getStartDiv()));
            }
        });

        if (voiceSegments.isEmpty()) {
            return buildRestXml(measureDiv, voice, outputStaff, divisions);
        }

        Map<Integer, List<ImportedVoiceNoteSegment>> groupsByStart = new LinkedHashMap<Integer, List<ImportedVoiceNoteSegment>>();
        for (ImportedVoiceNoteSegment segment : voiceSegments) {
            Integer key = Integer.valueOf(segment.getStartDiv());
            List<ImportedVoiceNoteSegment> bucket = groupsByStart.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ImportedVoiceNoteSegment>();
                groupsByStart.put(key, bucket);
            }
            bucket.add(segment);
        }
        List<Integer> starts = new ArrayList<Integer>(groupsByStart.keySet());
        Collections.sort(starts);
        Map<Integer, Integer> groupIndexByStart = new LinkedHashMap<Integer, Integer>();
        for (int index = 0; index < starts.size(); index++) {
            groupIndexByStart.put(starts.get(index), Integer.valueOf(index));
        }

        Map<String, Integer> keyAlterMap = keySignatureAlterMapByStep(keyFifths);
        Map<String, Integer> accidentalByStepOctave = new LinkedHashMap<String, Integer>();
        List<GroupAtStart> groups = new ArrayList<GroupAtStart>();
        Map<Integer, List<ImportedVoiceNoteSegment>> groupByStart = new LinkedHashMap<Integer, List<ImportedVoiceNoteSegment>>();
        for (Integer start : starts) {
            List<ImportedVoiceNoteSegment> group = new ArrayList<ImportedVoiceNoteSegment>(groupsByStart.get(start));
            Collections.sort(group, new Comparator<ImportedVoiceNoteSegment>() {
                @Override
                public int compare(ImportedVoiceNoteSegment left, ImportedVoiceNoteSegment right) {
                    return Integer.valueOf(left.getMidi()).compareTo(Integer.valueOf(right.getMidi()));
                }
            });
            int sourceDurDiv = 1;
            for (ImportedVoiceNoteSegment segment : group) {
                sourceDurDiv = Math.max(sourceDurDiv, segment.getDurDiv());
            }
            groups.add(new GroupAtStart(start.intValue(), sourceDurDiv, false, group));
            groupByStart.put(start, group);
        }

        Map<ImportedVoiceNoteSegment, MidiPitchComponents> pitchBySegment =
                new LinkedHashMap<ImportedVoiceNoteSegment, MidiPitchComponents>();
        for (ImportedVoiceNoteSegment segment : voiceSegments) {
            Integer groupIndexValue = groupIndexByStart.get(Integer.valueOf(segment.getStartDiv()));
            int groupIndex = groupIndexValue == null ? -1 : groupIndexValue.intValue();
            List<ImportedVoiceNoteSegment> prevGroup = groupIndex > 0 ? groupByStart.get(starts.get(groupIndex - 1))
                    : null;
            List<ImportedVoiceNoteSegment> nextGroup = groupIndex >= 0 && groupIndex < starts.size() - 1
                    ? groupByStart.get(starts.get(groupIndex + 1))
                    : null;
            pitchBySegment.put(segment, chooseMidiPitchComponentsWithContext(segment, keyFifths, prevGroup,
                    nextGroup));
        }

        List<PreparedNoteChunk> preparedNoteChunks = new ArrayList<PreparedNoteChunk>();
        Map<Integer, Integer> noteTimelineByChunkIndex = new LinkedHashMap<Integer, Integer>();
        List<BeamTimelineEvent> beamTimeline = new ArrayList<BeamTimelineEvent>();
        int cursorForTimeline = 0;
        for (GroupAtStart entry : groups) {
            int start = entry.getStartDiv();
            if (start > cursorForTimeline) {
                appendRestBeamTimeline(beamTimeline, start - cursorForTimeline, divisions);
            }
            int groupDur = Math.max(1, Math.round(entry.getSourceDurDiv()));
            List<DurationNotation> notationChunks = splitDurationNotations(groupDur, divisions);
            if (notationChunks.isEmpty()) {
                notationChunks = Collections.singletonList(new DurationNotation("quarter", 0,
                        groupDur / (double) Math.max(1, divisions), groupDur));
            }
            int chunkStartDiv = start;
            for (int chunkIndex = 0; chunkIndex < notationChunks.size(); chunkIndex++) {
                DurationNotation chunk = notationChunks.get(chunkIndex);
                boolean tieStart = notationChunks.size() > 1 && chunkIndex < notationChunks.size() - 1;
                boolean tieStop = notationChunks.size() > 1 && chunkIndex > 0;
                int preparedIndex = preparedNoteChunks.size();
                noteTimelineByChunkIndex.put(Integer.valueOf(preparedIndex), Integer.valueOf(beamTimeline.size()));
                beamTimeline.add(new BeamTimelineEvent("note", (int) Math.round(chunk.getDurDiv()),
                        beamLevelFromNotationType(chunk.getType()), Integer.valueOf(preparedIndex)));
                preparedNoteChunks.add(new PreparedNoteChunk(chunkStartDiv, (int) Math.round(chunk.getDurDiv()),
                        buildTypeXmlFromNotation(chunk), tieStart, tieStop,
                        entry.isInferredStaccato() && notationChunks.size() == 1 && chunkIndex == 0,
                        entry.getGroup()));
                chunkStartDiv += (int) Math.round(chunk.getDurDiv());
            }
            cursorForTimeline = Math.max(cursorForTimeline, start + groupDur);
        }
        if (cursorForTimeline < measureDiv) {
            appendRestBeamTimeline(beamTimeline, measureDiv - cursorForTimeline, divisions);
        }

        Map<Integer, BeamAssignment> beamAssignments = computeMidiBeamAssignments(beamTimeline, beatDiv, true);
        Map<Integer, String> beamXmlByChunkIndex = new LinkedHashMap<Integer, String>();
        for (Map.Entry<Integer, Integer> entry : noteTimelineByChunkIndex.entrySet()) {
            BeamAssignment assignment = beamAssignments.get(entry.getValue());
            if (assignment == null || assignment.getLevels() <= 0) {
                continue;
            }
            StringBuilder beamXml = new StringBuilder();
            for (int level = 1; level <= assignment.getLevels(); level++) {
                beamXml.append("<beam number=\"").append(level).append("\">").append(assignment.getState())
                        .append("</beam>");
            }
            if (beamXml.length() > 0) {
                beamXmlByChunkIndex.put(entry.getKey(), beamXml.toString());
            }
        }

        int cursor = 0;
        StringBuilder xml = new StringBuilder();
        for (int chunkIndex = 0; chunkIndex < preparedNoteChunks.size(); chunkIndex++) {
            PreparedNoteChunk prepared = preparedNoteChunks.get(chunkIndex);
            if (prepared.getStartDiv() > cursor) {
                xml.append(buildRestXml(prepared.getStartDiv() - cursor, voice, outputStaff, divisions));
            }
            String beamXml = beamXmlByChunkIndex.get(Integer.valueOf(chunkIndex));
            if (beamXml == null) {
                beamXml = "";
            }
            for (int index = 0; index < prepared.getGroup().size(); index++) {
                ImportedVoiceNoteSegment segment = prepared.getGroup().get(index);
                if (drum) {
                    MidiDrumDisplay display = midiToDrumDisplay(segment.getMidi());
                    xml.append("<note>");
                    if (index > 0) {
                        xml.append("<chord/>");
                    }
                    xml.append("<unpitched><display-step>").append(display.getStep()).append("</display-step>")
                            .append("<display-octave>").append(display.getOctave())
                            .append("</display-octave></unpitched>");
                    xml.append("<duration>").append(prepared.getDurDiv()).append("</duration>")
                            .append(prepared.getTypeXml()).append("<voice>").append(voice).append("</voice>")
                            .append(index == 0 ? beamXml : "").append("<staff>").append(outputStaff)
                            .append("</staff><notehead>x</notehead>");
                    xml.append(buildTieXml(prepared.isTieStart(), prepared.isTieStop(),
                            !drum && prepared.isInferredStaccato() && index == 0));
                    xml.append("</note>");
                } else {
                    MidiPitchComponents pitch = pitchBySegment.get(segment);
                    if (pitch == null) {
                        pitch = midiToPitchComponentsByKey(segment.getMidi(), keyFifths);
                    }
                    String stepOctaveKey = pitch.getStep() + pitch.getOctave();
                    Integer defaultAlterValue = accidentalByStepOctave.containsKey(stepOctaveKey)
                            ? accidentalByStepOctave.get(stepOctaveKey)
                            : keyAlterMap.get(pitch.getStep());
                    int defaultAlter = defaultAlterValue == null ? 0 : defaultAlterValue.intValue();
                    boolean requiresAccidental = pitch.getAlter() != defaultAlter;
                    String accidentalText = requiresAccidental ? accidentalTextFromAlter(pitch.getAlter()) : null;
                    xml.append("<note>");
                    if (index > 0) {
                        xml.append("<chord/>");
                    }
                    xml.append("<pitch><step>").append(pitch.getStep()).append("</step>");
                    if (pitch.getAlter() != 0) {
                        xml.append("<alter>").append(pitch.getAlter()).append("</alter>");
                    }
                    xml.append("<octave>").append(pitch.getOctave()).append("</octave></pitch>");
                    if (accidentalText != null) {
                        xml.append("<accidental>").append(accidentalText).append("</accidental>");
                    }
                    xml.append("<duration>").append(prepared.getDurDiv()).append("</duration>")
                            .append(prepared.getTypeXml()).append("<voice>").append(voice).append("</voice>")
                            .append(index == 0 ? beamXml : "").append("<staff>").append(outputStaff)
                            .append("</staff>");
                    xml.append(buildTieXml(prepared.isTieStart(), prepared.isTieStop(),
                            prepared.isInferredStaccato() && index == 0));
                    xml.append("</note>");
                    accidentalByStepOctave.put(stepOctaveKey, Integer.valueOf(pitch.getAlter()));
                }
            }
            cursor = Math.max(cursor, prepared.getStartDiv() + prepared.getDurDiv());
        }
        if (cursor < measureDiv) {
            xml.append(buildRestXml(measureDiv - cursor, voice, outputStaff, divisions));
        }
        return xml.toString();
    }

    public static List<MidiPartLaneDef> buildMidiPartLaneDefs(List<ImportedVoiceNoteSegment> splitSegments,
            boolean drum, boolean useGrandStaff) {
        List<ImportedVoiceNoteSegment> segments = splitSegments == null
                ? Collections.<ImportedVoiceNoteSegment>emptyList()
                : splitSegments;
        List<MidiPartLaneDef> laneDefs = new ArrayList<MidiPartLaneDef>();
        if (drum) {
            List<Integer> voices = sortedUniqueVoices(segments, null);
            if (voices.isEmpty()) {
                voices.add(Integer.valueOf(1));
            }
            for (int index = 0; index < voices.size(); index++) {
                laneDefs.add(new MidiPartLaneDef(1, voices.get(index).intValue(), index + 1));
            }
            return Collections.unmodifiableList(laneDefs);
        }

        if (useGrandStaff) {
            List<Integer> trebleVoices = sortedUniqueVoices(segments, Integer.valueOf(1));
            List<Integer> bassVoices = sortedUniqueVoices(segments, Integer.valueOf(2));
            if (trebleVoices.isEmpty()) {
                trebleVoices.add(Integer.valueOf(1));
            }
            if (bassVoices.isEmpty()) {
                bassVoices.add(Integer.valueOf(1));
            }
            int outputStaff = 1;
            for (Integer voice : trebleVoices) {
                laneDefs.add(new MidiPartLaneDef(1, voice.intValue(), outputStaff));
                outputStaff++;
            }
            for (Integer voice : bassVoices) {
                laneDefs.add(new MidiPartLaneDef(2, voice.intValue(), outputStaff));
                outputStaff++;
            }
            return Collections.unmodifiableList(laneDefs);
        }

        List<Integer> voices = sortedUniqueVoices(segments, null);
        if (voices.isEmpty()) {
            voices.add(Integer.valueOf(1));
        }
        for (Integer voice : voices) {
            laneDefs.add(new MidiPartLaneDef(1, voice.intValue(), 1));
        }
        return Collections.unmodifiableList(laneDefs);
    }

    public static List<MidiPartLaneDef> buildMidiLanesForMeasure(List<MidiPartLaneDef> laneDefs,
            List<ImportedVoiceNoteSegment> measureSegments) {
        List<MidiPartLaneDef> safeLaneDefs = laneDefs == null ? Collections.<MidiPartLaneDef>emptyList() : laneDefs;
        List<ImportedVoiceNoteSegment> safeSegments = measureSegments == null
                ? Collections.<ImportedVoiceNoteSegment>emptyList()
                : measureSegments;
        List<MidiPartLaneDef> active = new ArrayList<MidiPartLaneDef>();
        for (MidiPartLaneDef lane : safeLaneDefs) {
            if (lane == null) {
                continue;
            }
            for (ImportedVoiceNoteSegment segment : safeSegments) {
                if (segment != null && segment.getVoice() == lane.getVoice()
                        && segment.getStaff() == lane.getSourceStaff()) {
                    active.add(lane);
                    break;
                }
            }
        }
        if (!active.isEmpty()) {
            return Collections.unmodifiableList(active);
        }
        if (!safeLaneDefs.isEmpty() && safeLaneDefs.get(0) != null) {
            return Collections.unmodifiableList(
                    Collections.singletonList(safeLaneDefs.get(0)));
        }
        return Collections.emptyList();
    }

    public static List<MidiPartDef> buildMidiPartDefs(List<MidiTrackChannelGroup> partGroups,
            Map<Integer, String> trackNameByIndex, MksMidiTextMetadata mksTextMetadata,
            Map<String, Integer> programByTrackChannel) {
        List<MidiTrackChannelGroup> groups = partGroups == null
                ? Collections.<MidiTrackChannelGroup>emptyList()
                : partGroups;
        Map<Integer, String> safeTrackNameByIndex = trackNameByIndex == null
                ? Collections.<Integer, String>emptyMap()
                : trackNameByIndex;
        Map<String, Integer> safeProgramByTrackChannel = programByTrackChannel == null
                ? Collections.<String, Integer>emptyMap()
                : programByTrackChannel;
        Map<Integer, Integer> channelCountByTrackIndex = new LinkedHashMap<Integer, Integer>();
        for (MidiTrackChannelGroup group : groups) {
            if (group == null) {
                continue;
            }
            Integer trackIndex = Integer.valueOf(group.getTrackIndex());
            Integer count = channelCountByTrackIndex.get(trackIndex);
            channelCountByTrackIndex.put(trackIndex, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }

        List<MidiPartDef> partDefs = new ArrayList<MidiPartDef>();
        int index = 1;
        for (MidiTrackChannelGroup group : groups) {
            if (group == null) {
                continue;
            }
            int trackIndex = group.getTrackIndex();
            int channel = group.getChannel();
            String key = trackIndex + ":" + channel;
            boolean isDrum = channel == 10;
            String mksName = mksTextMetadata == null ? ""
                    : trimToEmpty(mksTextMetadata.getPartNameByTrackIndex().get(Integer.valueOf(trackIndex)));
            String trackName = trimToEmpty(safeTrackNameByIndex.get(Integer.valueOf(trackIndex)));
            String preferred = !isGenericMidiTrackName(trackName) ? trackName
                    : (mksName.length() > 0 ? mksName : trackName);
            String name;
            if (preferred.length() > 0) {
                Integer channelCount = channelCountByTrackIndex.get(Integer.valueOf(trackIndex));
                name = channelCount != null && channelCount.intValue() > 1 ? preferred + " Ch " + channel
                        : preferred;
            } else {
                name = isDrum ? "Drums (Track " + (trackIndex + 1) + ")"
                        : "Track " + (trackIndex + 1) + " Ch " + channel;
            }
            Integer programRaw = safeProgramByTrackChannel.get(key);
            Integer program = normalizeMidiProgramNumber(programRaw == null ? Double.NaN : programRaw.doubleValue());
            partDefs.add(new MidiPartDef("P" + index, name, channel, program == null ? 1 : program.intValue(), key));
            index++;
        }
        if (partDefs.isEmpty()) {
            partDefs.add(new MidiPartDef("P1", "Part 1", 1, 1, "0:1"));
        }
        return Collections.unmodifiableList(partDefs);
    }

    public static String buildMidiPartListXml(List<MidiPartDef> partDefs) {
        List<MidiPartDef> safePartDefs = partDefs == null ? Collections.<MidiPartDef>emptyList() : partDefs;
        StringBuilder xml = new StringBuilder();
        for (MidiPartDef part : safePartDefs) {
            if (part == null) {
                continue;
            }
            xml.append("<score-part id=\"").append(xmlEscape(part.getPartId())).append("\"><part-name>")
                    .append(xmlEscape(part.getName())).append("</part-name><midi-instrument id=\"")
                    .append(xmlEscape(part.getPartId())).append("-I1\"><midi-channel>")
                    .append(part.getChannel()).append("</midi-channel><midi-program>")
                    .append(part.getProgram()).append("</midi-program></midi-instrument></score-part>");
        }
        return xml.toString();
    }

    public static String buildMidiImportSkeletonDocumentXml(String title, String movementTitle, String composer,
            String partList, String parts) {
        String safeMovementTitle = movementTitle == null ? "" : movementTitle;
        String safeComposer = composer == null ? "" : composer;
        String movementTitleXml = safeMovementTitle.trim().length() > 0
                ? "<movement-title>" + xmlEscape(safeMovementTitle) + "</movement-title>"
                : "";
        String composerXml = safeComposer.trim().length() > 0
                ? "<identification><creator type=\"composer\">" + xmlEscape(safeComposer)
                        + "</creator></identification>"
                : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>"
                + xmlEscape(title) + "</work-title></work>" + movementTitleXml + composerXml + "<part-list>"
                + (partList == null ? "" : partList) + "</part-list>" + (parts == null ? "" : parts)
                + "</score-partwise>";
    }

    public static String buildMidiImportedPartXml(String partId, String partName, int divisions, int beats,
            int beatType, int keyFifths, String keyMode, boolean drum, List<ImportedQuantizedNote> notes,
            Map<Integer, List<MidiTempoMeasureEvent>> tempoEventsByMeasure, boolean includeTempoEvents,
            int ticksPerQuarter, int pickupTicks, List<MidiImportDiagnostic> warnings, boolean debugImportMetadata,
            String mksSysExMetadataXml, String sourceMetadataXml) {
        int safeDivisions = Math.max(1, Math.round(divisions));
        int safeBeats = Math.max(1, Math.round(beats));
        int safeBeatType = Math.max(1, Math.round(beatType));
        int safeTicksPerQuarter = Math.max(1, Math.round(ticksPerQuarter));
        List<ImportedQuantizedNote> safeNotes = notes == null ? Collections.<ImportedQuantizedNote>emptyList()
                : notes;
        List<MidiImportDiagnostic> mutableWarnings = warnings == null
                ? new ArrayList<MidiImportDiagnostic>()
                : warnings;
        MidiPartMeasureLayout measureLayout = resolveMidiPartMeasureLayout(safeNotes, safeTicksPerQuarter,
                safeDivisions, safeBeats, safeBeatType, pickupTicks);
        List<ImportedVoiceCluster> clusters = allocateAutoVoices(safeNotes, mutableWarnings);
        List<Integer> melodicKeys = new ArrayList<Integer>();
        for (ImportedQuantizedNote note : safeNotes) {
            if (note != null) {
                melodicKeys.add(Integer.valueOf(note.getMidi()));
            }
        }
        String singleClefSign = StaffClefPolicy.chooseSingleClefByKeys(melodicKeys);
        String normalizedPartName = partName == null ? "" : partName.trim().toLowerCase();
        boolean prefersAltoClef = normalizedPartName.matches(".*(^|[^a-z])(viola|vla\\.?)([^a-z]|$).*");
        boolean initialGrandStaff = !drum && !prefersAltoClef && StaffClefPolicy.shouldUseGrandStaffByRange(melodicKeys);
        MidiPartSegmentLayout segmentLayout = resolveMidiPartSegmentLayout(clusters, safeTicksPerQuarter,
                safeDivisions, measureLayout, drum, initialGrandStaff);
        List<MidiPartLaneDef> laneDefs = buildMidiPartLaneDefs(segmentLayout.getSplitSegments(), drum,
                segmentLayout.isUseGrandStaff());
        String warningMetadataXml = buildMidiDiagMiscXml(mutableWarnings);
        StringBuilder partXml = new StringBuilder();
        partXml.append("<part id=\"").append(xmlEscape(partId)).append("\">");
        String previousDynamicMark = null;
        for (int measureIndex = 0; measureIndex < measureLayout.getMeasureCount(); measureIndex++) {
            List<ImportedVoiceNoteSegment> measureSegments = segmentLayout.getVoiceSegmentsByMeasure()
                    .get(Integer.valueOf(measureIndex));
            if (measureSegments == null) {
                measureSegments = Collections.emptyList();
            }
            int currentMeasureDiv = measureDivForMidiPartMeasureIndex(measureLayout, measureIndex);
            partXml.append(buildMidiPartMeasureStartXml(measureLayout, measureIndex));
            if (measureIndex == 0) {
                partXml.append(buildMidiInitialMeasureAttributesXml(safeDivisions, keyFifths, keyMode, safeBeats,
                        safeBeatType, drum, segmentLayout.isUseGrandStaff(), laneDefs, singleClefSign,
                        prefersAltoClef));
            }
            if (includeTempoEvents && tempoEventsByMeasure != null) {
                List<MidiTempoMeasureEvent> tempoEvents = tempoEventsByMeasure.get(Integer.valueOf(measureIndex));
                if (tempoEvents != null) {
                    for (MidiTempoMeasureEvent tempoEvent : tempoEvents) {
                        if (tempoEvent != null) {
                            partXml.append(buildTempoDirectionXml(tempoEvent.getBpm(), tempoEvent.getOffsetDiv()));
                        }
                    }
                }
            }
            if (debugImportMetadata) {
                partXml.append(buildMeasureMidiMetaMiscXml(measureSegments));
            }
            if (measureIndex == 0) {
                partXml.append(mksSysExMetadataXml == null ? "" : mksSysExMetadataXml);
                partXml.append(sourceMetadataXml == null ? "" : sourceMetadataXml);
                partXml.append(warningMetadataXml);
            }
            MidiDynamicDirectionsResult dynamicDirections =
                    buildMeasureDynamicDirectionsXml(measureSegments, previousDynamicMark);
            partXml.append(dynamicDirections.getXml());
            previousDynamicMark = dynamicDirections.getPreviousDynamicMark();
            List<MidiPartLaneDef> lanesForMeasure = buildMidiLanesForMeasure(laneDefs, measureSegments);
            for (int laneIndex = 0; laneIndex < lanesForMeasure.size(); laneIndex++) {
                MidiPartLaneDef lane = lanesForMeasure.get(laneIndex);
                if (laneIndex > 0) {
                    partXml.append("<backup><duration>").append(currentMeasureDiv).append("</duration></backup>");
                }
                partXml.append(buildMeasureVoiceXml(measureSegments, lane.getVoice(), lane.getSourceStaff(),
                        lane.getOutputStaff(), currentMeasureDiv,
                        Math.max(1, Math.round(currentMeasureDiv / (float) Math.max(1, safeBeats))), drum,
                        safeDivisions, keyFifths));
            }
            partXml.append("</measure>");
        }
        partXml.append("</part>");
        return partXml.toString();
    }

    public static String buildImportSkeletonMusicXml(String title, String movementTitle, String composer,
            String quantizeGrid, Integer divisionsOverride, int ticksPerQuarter, int beats, int beatType,
            int keyFifths, String keyMode, List<MidiTempoEvent> tempoEvents, int pickupTicks,
            List<MidiTrackChannelGroup> partGroups, Map<String, List<ImportedQuantizedNote>> notesByTrackChannel,
            Map<String, Integer> programByTrackChannel, List<MidiImportDiagnostic> warnings,
            boolean debugImportMetadata, String mksSysExMetadataXml, String sourceMetadataXml,
            Map<Integer, String> trackNameByIndex, MksMidiTextMetadata mksTextMetadata) {
        int divisions = Math.max(1, divisionsOverride == null
                ? quantizeGridToDivisions(normalizeMidiImportQuantizeGridOption(quantizeGrid))
                : Math.round(divisionsOverride.intValue()));
        int safeTicksPerQuarter = Math.max(1, Math.round(ticksPerQuarter));
        int safeBeats = Math.max(1, Math.round(beats));
        int safeBeatType = Math.max(1, Math.round(beatType));
        int safeKeyFifths = Math.max(-7, Math.min(7, Math.round(keyFifths)));
        String safeKeyMode = "minor".equals(keyMode) ? "minor" : "major";
        int measureTicks = Math.max(1,
                Math.round((safeTicksPerQuarter * 4.0f * safeBeats) / Math.max(1, safeBeatType)));
        int pickupMeasureTicks = Math.max(0, Math.min(measureTicks - 1, Math.round(pickupTicks)));
        List<MidiPartDef> partDefs = buildMidiPartDefs(partGroups, trackNameByIndex, mksTextMetadata,
                programByTrackChannel);
        Map<Integer, List<MidiTempoMeasureEvent>> tempoEventsByMeasure =
                groupMidiTempoEventsByMeasure(tempoEvents, new MidiPartMeasureLayout(measureTicks,
                        Math.max(1, Math.round((divisions * 4.0f * safeBeats) / Math.max(1, safeBeatType))),
                        pickupMeasureTicks, pickupMeasureTicks > 0
                                ? Math.max(1, Math.round((pickupMeasureTicks * (float) divisions)
                                        / safeTicksPerQuarter))
                                : 0,
                        1), safeTicksPerQuarter, divisions);
        StringBuilder parts = new StringBuilder();
        Map<String, List<ImportedQuantizedNote>> safeNotesByTrackChannel = notesByTrackChannel == null
                ? Collections.<String, List<ImportedQuantizedNote>>emptyMap()
                : notesByTrackChannel;
        for (int index = 0; index < partDefs.size(); index++) {
            MidiPartDef part = partDefs.get(index);
            List<ImportedQuantizedNote> notes = safeNotesByTrackChannel.get(part.getKey());
            parts.append(buildMidiImportedPartXml(part.getPartId(), part.getName(), divisions, safeBeats,
                    safeBeatType, safeKeyFifths, safeKeyMode, part.getChannel() == 10, notes,
                    tempoEventsByMeasure, index == 0, safeTicksPerQuarter, pickupMeasureTicks, warnings,
                    debugImportMetadata, index == 0 ? mksSysExMetadataXml : "",
                    index == 0 ? sourceMetadataXml : ""));
        }
        return buildMidiImportSkeletonDocumentXml(title, movementTitle, composer, buildMidiPartListXml(partDefs),
                parts.toString());
    }

    public static String velocityToDynamicMark(int velocity) {
        int value = clampVelocity(velocity);
        if (value <= 15) {
            return "ppp";
        }
        if (value <= 31) {
            return "pp";
        }
        if (value <= 47) {
            return "p";
        }
        if (value <= 63) {
            return "mp";
        }
        if (value <= 79) {
            return "mf";
        }
        if (value <= 95) {
            return "f";
        }
        if (value <= 111) {
            return "ff";
        }
        return "fff";
    }

    public static String buildDynamicsDirectionXml(String dynamicMark, int offsetDiv, int staff) {
        String mark = normalizeDynamicMark(dynamicMark);
        StringBuilder xml = new StringBuilder();
        xml.append("<direction>");
        xml.append("<direction-type><dynamics><").append(mark).append("/></dynamics></direction-type>");
        if (offsetDiv > 0) {
            xml.append("<offset>").append(offsetDiv).append("</offset>");
        }
        xml.append("<staff>").append(Math.max(1, Math.round(staff))).append("</staff>");
        xml.append("</direction>");
        return xml.toString();
    }

    public static String buildTempoDirectionXml(int bpm, int offsetDiv) {
        int tempo = clampTempo(bpm);
        StringBuilder xml = new StringBuilder();
        xml.append("<direction>");
        xml.append("<direction-type><metronome><beat-unit>quarter</beat-unit>");
        xml.append("<per-minute>").append(tempo).append("</per-minute>");
        xml.append("</metronome></direction-type>");
        if (offsetDiv > 0) {
            xml.append("<offset>").append(offsetDiv).append("</offset>");
        }
        xml.append("<sound tempo=\"").append(tempo).append("\"/>");
        xml.append("</direction>");
        return xml.toString();
    }

    public static MidiDynamicDirectionsResult buildMeasureDynamicDirectionsXml(
            List<ImportedVoiceNoteSegment> measureSegments, String previousDynamicMark) {
        if (measureSegments == null || measureSegments.isEmpty()) {
            return new MidiDynamicDirectionsResult("", normalizeNullableDynamicMark(previousDynamicMark));
        }
        Map<Integer, Integer> dynamicVelocityByOffset = new LinkedHashMap<Integer, Integer>();
        for (ImportedVoiceNoteSegment segment : measureSegments) {
            if (segment == null) {
                continue;
            }
            Integer offset = Integer.valueOf(segment.getStartDiv());
            Integer previousVelocity = dynamicVelocityByOffset.get(offset);
            if (previousVelocity == null || segment.getVelocity() > previousVelocity.intValue()) {
                dynamicVelocityByOffset.put(offset, Integer.valueOf(segment.getVelocity()));
            }
        }
        List<Integer> offsets = new ArrayList<Integer>(dynamicVelocityByOffset.keySet());
        Collections.sort(offsets);
        String currentMark = normalizeNullableDynamicMark(previousDynamicMark);
        StringBuilder xml = new StringBuilder();
        for (Integer offset : offsets) {
            Integer velocity = dynamicVelocityByOffset.get(offset);
            String dynamicMark = velocityToDynamicMark(velocity == null ? 80 : velocity.intValue());
            if (dynamicMark.equals(currentMark)) {
                continue;
            }
            xml.append(buildDynamicsDirectionXml(dynamicMark, offset.intValue(), 1));
            currentMark = dynamicMark;
        }
        return new MidiDynamicDirectionsResult(xml.toString(), currentMark);
    }

    public static String buildMidiInitialMeasureAttributesXml(int divisions, int keyFifths, String keyMode,
            int beats, int beatType, boolean drum, boolean useGrandStaff, List<MidiPartLaneDef> laneDefs,
            String singleClefSign, boolean prefersAltoClef) {
        int safeDivisions = Math.max(1, Math.round(divisions));
        int safeFifths = Math.max(-7, Math.min(7, Math.round(keyFifths)));
        String mode = "minor".equals(keyMode) ? "minor" : "major";
        int safeBeats = Math.max(1, Math.round(beats));
        int safeBeatType = Math.max(1, Math.round(beatType));
        List<MidiPartLaneDef> safeLaneDefs = laneDefs == null ? Collections.<MidiPartLaneDef>emptyList() : laneDefs;

        StringBuilder xml = new StringBuilder();
        xml.append("<attributes>");
        xml.append("<divisions>").append(safeDivisions).append("</divisions>");
        xml.append("<key><fifths>").append(safeFifths).append("</fifths><mode>").append(mode)
                .append("</mode></key>");
        xml.append("<time><beats>").append(safeBeats).append("</beats><beat-type>").append(safeBeatType)
                .append("</beat-type></time>");
        if (drum) {
            xml.append("<clef><sign>percussion</sign><line>2</line></clef>");
        } else if (useGrandStaff) {
            int laneCount = Math.max(1, safeLaneDefs.size());
            xml.append("<staves>").append(laneCount).append("</staves>");
            if (safeLaneDefs.isEmpty()) {
                xml.append("<clef number=\"1\"><sign>G</sign><line>2</line></clef>");
            } else {
                for (MidiPartLaneDef lane : safeLaneDefs) {
                    if (lane == null) {
                        continue;
                    }
                    if (lane.getSourceStaff() == 1) {
                        xml.append("<clef number=\"").append(lane.getOutputStaff())
                                .append("\"><sign>G</sign><line>2</line></clef>");
                    } else {
                        xml.append("<clef number=\"").append(lane.getOutputStaff())
                                .append("\"><sign>F</sign><line>4</line></clef>");
                    }
                }
            }
        } else {
            String clefSign = prefersAltoClef ? "C" : normalizeMidiSingleClefSign(singleClefSign);
            int line = "F".equals(clefSign) ? 4 : ("C".equals(clefSign) ? 3 : 2);
            xml.append("<clef><sign>").append(clefSign).append("</sign><line>").append(line)
                    .append("</line></clef>");
        }
        xml.append("</attributes>");
        return xml.toString();
    }

    public static MidiPartMeasureLayout resolveMidiPartMeasureLayout(List<ImportedQuantizedNote> notes,
            int ticksPerQuarter, int divisions, int beats, int beatType, int pickupTicks) {
        int safeTicksPerQuarter = Math.max(1, Math.round(ticksPerQuarter));
        int safeDivisions = Math.max(1, Math.round(divisions));
        int safeBeats = Math.max(1, Math.round(beats));
        int safeBeatType = Math.max(1, Math.round(beatType));
        int measureTicks = Math.max(1,
                Math.round((safeTicksPerQuarter * 4.0f * safeBeats) / Math.max(1, safeBeatType)));
        int measureDiv = Math.max(1, Math.round((safeDivisions * 4.0f * safeBeats) / Math.max(1, safeBeatType)));
        int pickupMeasureTicks = Math.max(0, Math.min(measureTicks - 1, Math.round(pickupTicks)));
        int pickupMeasureDiv = pickupMeasureTicks > 0
                ? Math.max(1, Math.round((pickupMeasureTicks * (float) safeDivisions) / safeTicksPerQuarter))
                : 0;
        int maxEndTick = measureTicks;
        if (notes != null && !notes.isEmpty()) {
            for (ImportedQuantizedNote note : notes) {
                if (note != null) {
                    maxEndTick = Math.max(maxEndTick, note.getEndTick());
                }
            }
        }
        int measureCount;
        if (pickupMeasureTicks > 0) {
            measureCount = maxEndTick <= pickupMeasureTicks ? 1
                    : 1 + Math.max(1, (int) Math.ceil((maxEndTick - pickupMeasureTicks) / (double) measureTicks));
        } else {
            measureCount = Math.max(1, (int) Math.ceil(maxEndTick / (double) measureTicks));
        }
        return new MidiPartMeasureLayout(measureTicks, measureDiv, pickupMeasureTicks, pickupMeasureDiv,
                measureCount);
    }

    public static int measureDivForMidiPartMeasureIndex(MidiPartMeasureLayout layout, int measureIndex) {
        if (layout == null) {
            return 1;
        }
        return measureIndex == 0 && layout.getPickupMeasureDiv() > 0 ? layout.getPickupMeasureDiv()
                : layout.getMeasureDiv();
    }

    public static int midiPartMeasureNumberForIndex(MidiPartMeasureLayout layout, int measureIndex) {
        int index = Math.max(0, Math.round(measureIndex));
        return layout != null && layout.getPickupMeasureDiv() > 0 ? index : index + 1;
    }

    public static String midiPartMeasureImplicitAttribute(MidiPartMeasureLayout layout, int measureIndex) {
        return layout != null && measureIndex == 0 && layout.getPickupMeasureDiv() > 0 ? " implicit=\"yes\"" : "";
    }

    public static String buildMidiPartMeasureStartXml(MidiPartMeasureLayout layout, int measureIndex) {
        return "<measure number=\"" + midiPartMeasureNumberForIndex(layout, measureIndex) + "\""
                + midiPartMeasureImplicitAttribute(layout, measureIndex) + ">";
    }

    public static MidiMeasureOffsetDiv mapMidiTickToMeasureOffsetDiv(int tickRaw, MidiPartMeasureLayout layout,
            int ticksPerQuarter, int divisions) {
        MidiPartMeasureLayout safeLayout = layout == null ? new MidiPartMeasureLayout(1, 1, 0, 0, 1) : layout;
        int tick = Math.max(0, Math.round(tickRaw));
        int measureIndex = 0;
        int tickInMeasure = tick;
        int measureTicks = Math.max(1, safeLayout.getMeasureTicks());
        int pickupMeasureTicks = Math.max(0, safeLayout.getPickupMeasureTicks());
        if (pickupMeasureTicks > 0 && tick >= pickupMeasureTicks) {
            measureIndex = 1 + (int) Math.floor((tick - pickupMeasureTicks) / (double) measureTicks);
            tickInMeasure = (tick - pickupMeasureTicks) % measureTicks;
        } else if (pickupMeasureTicks <= 0) {
            measureIndex = (int) Math.floor(tick / (double) measureTicks);
            tickInMeasure = tick - measureIndex * measureTicks;
        }
        int offsetDiv = Math.max(0,
                Math.round((tickInMeasure * (float) Math.max(1, divisions)) / Math.max(1, ticksPerQuarter)));
        return new MidiMeasureOffsetDiv(measureIndex, offsetDiv);
    }

    public static Map<Integer, List<MidiTempoMeasureEvent>> groupMidiTempoEventsByMeasure(
            List<MidiTempoEvent> tempoEvents, MidiPartMeasureLayout layout, int ticksPerQuarter, int divisions) {
        Map<Integer, List<MidiTempoMeasureEvent>> byMeasure = new LinkedHashMap<Integer, List<MidiTempoMeasureEvent>>();
        if (tempoEvents != null) {
            for (MidiTempoEvent tempoEvent : tempoEvents) {
                if (tempoEvent == null) {
                    continue;
                }
                MidiMeasureOffsetDiv mapped = mapMidiTickToMeasureOffsetDiv(tempoEvent.getTick(), layout,
                        ticksPerQuarter, divisions);
                Integer key = Integer.valueOf(mapped.getMeasureIndex());
                List<MidiTempoMeasureEvent> bucket = byMeasure.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<MidiTempoMeasureEvent>();
                    byMeasure.put(key, bucket);
                }
                bucket.add(new MidiTempoMeasureEvent(mapped.getOffsetDiv(), clampTempo(tempoEvent.getBpm())));
            }
        }
        Map<Integer, List<MidiTempoMeasureEvent>> out =
                new LinkedHashMap<Integer, List<MidiTempoMeasureEvent>>();
        List<Integer> measureIndexes = new ArrayList<Integer>(byMeasure.keySet());
        Collections.sort(measureIndexes);
        for (Integer measureIndex : measureIndexes) {
            List<MidiTempoMeasureEvent> events = new ArrayList<MidiTempoMeasureEvent>(byMeasure.get(measureIndex));
            Collections.sort(events, new Comparator<MidiTempoMeasureEvent>() {
                @Override
                public int compare(MidiTempoMeasureEvent left, MidiTempoMeasureEvent right) {
                    if (left.getOffsetDiv() == right.getOffsetDiv()) {
                        return Integer.valueOf(left.getBpm()).compareTo(Integer.valueOf(right.getBpm()));
                    }
                    return Integer.valueOf(left.getOffsetDiv()).compareTo(Integer.valueOf(right.getOffsetDiv()));
                }
            });
            List<MidiTempoMeasureEvent> deduped = new ArrayList<MidiTempoMeasureEvent>();
            for (MidiTempoMeasureEvent event : events) {
                if (!deduped.isEmpty()
                        && deduped.get(deduped.size() - 1).getOffsetDiv() == event.getOffsetDiv()) {
                    deduped.set(deduped.size() - 1, new MidiTempoMeasureEvent(event.getOffsetDiv(),
                            event.getBpm()));
                } else {
                    deduped.add(new MidiTempoMeasureEvent(event.getOffsetDiv(), event.getBpm()));
                }
            }
            out.put(measureIndex, Collections.unmodifiableList(deduped));
        }
        return Collections.unmodifiableMap(out);
    }

    private static String normalizeMidiSingleClefSign(String singleClefSign) {
        String value = singleClefSign == null ? "" : singleClefSign.trim().toUpperCase();
        if ("F".equals(value) || "C".equals(value)) {
            return value;
        }
        return "G";
    }

    private static String normalizeDynamicMark(String dynamicMark) {
        String mark = normalizeNullableDynamicMark(dynamicMark);
        return mark == null ? "mf" : mark;
    }

    private static String normalizeNullableDynamicMark(String dynamicMark) {
        String value = dynamicMark == null ? "" : dynamicMark.trim().toLowerCase();
        if ("ppp".equals(value) || "pp".equals(value) || "p".equals(value) || "mp".equals(value)
                || "mf".equals(value) || "f".equals(value) || "ff".equals(value) || "fff".equals(value)) {
            return value;
        }
        return null;
    }

    private static List<Integer> sortedUniqueVoices(List<ImportedVoiceNoteSegment> segments, Integer sourceStaff) {
        Set<Integer> seen = new LinkedHashSet<Integer>();
        if (segments != null) {
            for (ImportedVoiceNoteSegment segment : segments) {
                if (segment == null) {
                    continue;
                }
                if (sourceStaff != null && segment.getStaff() != sourceStaff.intValue()) {
                    continue;
                }
                seen.add(Integer.valueOf(segment.getVoice()));
            }
        }
        List<Integer> voices = new ArrayList<Integer>(seen);
        Collections.sort(voices);
        return voices;
    }

    private static void appendRestBeamTimeline(List<BeamTimelineEvent> beamTimeline, int restDur, int divisions) {
        List<DurationNotation> restChunks = splitDurationNotations(restDur, divisions);
        if (restChunks.isEmpty()) {
            beamTimeline.add(new BeamTimelineEvent("rest", restDur, 0, null));
            return;
        }
        for (DurationNotation restChunk : restChunks) {
            beamTimeline.add(new BeamTimelineEvent("rest", (int) Math.round(restChunk.getDurDiv()),
                    beamLevelFromNotationType(restChunk.getType()), null));
        }
    }

    private static Map<String, Integer> keySignatureAlterMapByStep(int fifths) {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        String[] steps = new String[] { "C", "D", "E", "F", "G", "A", "B" };
        for (String step : steps) {
            map.put(step, Integer.valueOf(keySignatureAlterByStep(fifths, step)));
        }
        return map;
    }

    private static MidiPitchComponents midiToPitchComponents(int midiNumber) {
        int note = Math.max(0, Math.min(127, Math.round(midiNumber)));
        int octave = (int) Math.floor(note / 12.0d) - 1;
        String[] steps = new String[] { "C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B" };
        int[] alters = new int[] { 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0 };
        int semitone = note % 12;
        return new MidiPitchComponents(steps[semitone], alters[semitone], octave);
    }

    private static MidiPitchComponents midiToPitchComponentsByKey(int midiNumber, int keyFifths) {
        int note = Math.max(0, Math.min(127, Math.round(midiNumber)));
        int octave = (int) Math.floor(note / 12.0d) - 1;
        boolean useFlatSpelling = keyFifths < 0;
        String[] sharpSteps = new String[] { "C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B" };
        int[] sharpAlters = new int[] { 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0 };
        String[] flatSteps = new String[] { "C", "D", "D", "E", "E", "F", "G", "G", "A", "A", "B", "B" };
        int[] flatAlters = new int[] { 0, -1, 0, -1, 0, 0, -1, 0, -1, 0, -1, 0 };
        int semitone = note % 12;
        return new MidiPitchComponents(useFlatSpelling ? flatSteps[semitone] : sharpSteps[semitone],
                useFlatSpelling ? flatAlters[semitone] : sharpAlters[semitone], octave);
    }

    private static Integer pickClosestMidiInGroup(List<ImportedVoiceNoteSegment> group, int targetMidi) {
        if (group == null || group.isEmpty()) {
            return null;
        }
        Integer best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (ImportedVoiceNoteSegment note : group) {
            if (note == null) {
                continue;
            }
            int distance = Math.abs(note.getMidi() - targetMidi);
            if (best == null || distance < bestDistance) {
                best = Integer.valueOf(note.getMidi());
                bestDistance = distance;
            }
        }
        return best;
    }

    private static MidiPitchComponents chooseMidiPitchComponentsWithContext(ImportedVoiceNoteSegment segment,
            int keyFifths, List<ImportedVoiceNoteSegment> prevGroup, List<ImportedVoiceNoteSegment> nextGroup) {
        int semitone = mod12(segment == null ? 0 : segment.getMidi());
        Set<Integer> enharmonicSemitones = new LinkedHashSet<Integer>(
                Arrays.asList(Integer.valueOf(1), Integer.valueOf(3), Integer.valueOf(6), Integer.valueOf(8),
                        Integer.valueOf(10)));
        if (!enharmonicSemitones.contains(Integer.valueOf(semitone))) {
            return midiToPitchComponentsByKey(segment.getMidi(), keyFifths);
        }
        Integer prevMidi = pickClosestMidiInGroup(prevGroup, segment.getMidi());
        Integer nextMidi = pickClosestMidiInGroup(nextGroup, segment.getMidi());
        boolean touchesUpperSemitone = (prevMidi != null && prevMidi.intValue() == segment.getMidi() + 1)
                || (nextMidi != null && nextMidi.intValue() == segment.getMidi() + 1);
        boolean touchesLowerSemitone = (prevMidi != null && prevMidi.intValue() == segment.getMidi() - 1)
                || (nextMidi != null && nextMidi.intValue() == segment.getMidi() - 1);
        if (touchesUpperSemitone && !touchesLowerSemitone) {
            return midiToPitchComponentsByKey(segment.getMidi(), Math.max(0, keyFifths));
        }
        if (touchesLowerSemitone && !touchesUpperSemitone) {
            return midiToPitchComponentsByKey(segment.getMidi(), Math.min(0, keyFifths));
        }
        return midiToPitchComponentsByKey(segment.getMidi(), keyFifths);
    }

    private static String accidentalTextFromAlter(int alter) {
        if (alter == -2) {
            return "flat-flat";
        }
        if (alter == -1) {
            return "flat";
        }
        if (alter == 0) {
            return "natural";
        }
        if (alter == 1) {
            return "sharp";
        }
        if (alter == 2) {
            return "double-sharp";
        }
        return null;
    }

    private static MidiDrumDisplay midiToDrumDisplay(int midiNumber) {
        MidiPitchComponents pitch = midiToPitchComponents(midiNumber);
        return new MidiDrumDisplay(pitch.getStep(), pitch.getOctave());
    }

    private static Map<Integer, BeamAssignment> computeMidiBeamAssignments(List<BeamTimelineEvent> events,
            int beatDiv, boolean splitAtBeatBoundaryWhenImplicit) {
        Map<Integer, BeamAssignment> assignmentByIndex = new LinkedHashMap<Integer, BeamAssignment>();
        if (events == null) {
            return assignmentByIndex;
        }
        boolean hasExplicitBeamMode = false;
        if (!hasExplicitBeamMode) {
            List<Integer> currentGroup = new ArrayList<Integer>();
            int cursorDiv = 0;
            int resolvedBeatDiv = Math.max(1, Math.round(beatDiv));
            for (int index = 0; index < events.size(); index++) {
                BeamTimelineEvent info = events.get(index);
                if (info != null && splitAtBeatBoundaryWhenImplicit) {
                    boolean startsAtBeatBoundary = cursorDiv > 0 && cursorDiv % resolvedBeatDiv == 0;
                    if (startsAtBeatBoundary) {
                        flushMidiBeamGroup(events, currentGroup, assignmentByIndex);
                        currentGroup.clear();
                    }
                }
                if (info == null || !"note".equals(info.getKind()) || !isMidiBeamableTimedEvent(info)) {
                    flushMidiBeamGroup(events, currentGroup, assignmentByIndex);
                    currentGroup.clear();
                    if (info != null) {
                        cursorDiv += Math.max(0, info.getDurDiv());
                    }
                    continue;
                }
                currentGroup.add(Integer.valueOf(index));
                cursorDiv += Math.max(0, info.getDurDiv());
            }
            flushMidiBeamGroup(events, currentGroup, assignmentByIndex);
        }
        return assignmentByIndex;
    }

    private static boolean isMidiBeamableTimedEvent(BeamTimelineEvent info) {
        return info != null && info.getLevels() > 0;
    }

    private static void flushMidiBeamGroup(List<BeamTimelineEvent> infos, List<Integer> indices,
            Map<Integer, BeamAssignment> assignmentByIndex) {
        List<Integer> chordIndices = new ArrayList<Integer>();
        for (Integer index : indices) {
            if (index == null || index.intValue() < 0 || index.intValue() >= infos.size()) {
                continue;
            }
            BeamTimelineEvent info = infos.get(index.intValue());
            if (info != null && "note".equals(info.getKind())) {
                chordIndices.add(index);
            }
        }
        if (chordIndices.size() < 2) {
            return;
        }
        for (int groupIndex = 0; groupIndex < chordIndices.size(); groupIndex++) {
            Integer eventIndex = chordIndices.get(groupIndex);
            BeamTimelineEvent info = infos.get(eventIndex.intValue());
            if (info == null || info.getLevels() <= 0) {
                continue;
            }
            String state = groupIndex == 0 ? "begin" : (groupIndex == chordIndices.size() - 1 ? "end" : "continue");
            assignmentByIndex.put(eventIndex, new BeamAssignment(state, info.getLevels()));
        }
    }

    private static String formatMidiNumber(double value) {
        if (Math.abs(value - Math.round(value)) <= 0.000001d) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static String padLeft(String value, int width, char ch) {
        String text = value == null ? "" : value;
        StringBuilder out = new StringBuilder();
        for (int index = text.length(); index < width; index++) {
            out.append(ch);
        }
        out.append(text);
        return out.toString();
    }

    private static String xmlEscape(String raw) {
        return (raw == null ? "" : raw).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String trimToEmpty(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static Integer parseLeadingBase10Integer(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int index = 0;
        if (trimmed.charAt(index) == '+' || trimmed.charAt(index) == '-') {
            index++;
        }
        int digitStart = index;
        while (index < trimmed.length() && Character.isDigit(trimmed.charAt(index))) {
            index++;
        }
        if (index == digitStart) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(trimmed.substring(0, index)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<Element> directChildElementsByName(Element parent, String name) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(node.getNodeName())) {
                out.add((Element) node);
            }
        }
        return out;
    }

    private static Element directChildElementByName(Element parent, String name) {
        List<Element> children = directChildElementsByName(parent, name);
        return children.isEmpty() ? null : children.get(0);
    }

    private static String directChildText(Element parent, String name) {
        Element child = directChildElementByName(parent, name);
        return child == null ? "" : child.getTextContent();
    }

    private static List<Element> directElementChildren(Element parent) {
        List<Element> out = new ArrayList<Element>();
        if (parent == null) {
            return out;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                out.add((Element) node);
            }
        }
        return out;
    }

    private static Integer getFirstInteger(Element parent, String name) {
        String text = trimToEmpty(directChildText(parent, name));
        if (text.isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return null;
            }
            return Integer.valueOf((int) Math.round(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer getFirstIntegerByDirectPath(Element parent, String... names) {
        Element current = parent;
        if (names == null) {
            return null;
        }
        for (String name : names) {
            current = directChildElementByName(current, name);
            if (current == null) {
                return null;
            }
        }
        String text = trimToEmpty(current.getTextContent());
        if (text.isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return null;
            }
            return Integer.valueOf((int) Math.round(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean hasTempoEventAtZero(List<MidiTempoEvent> events) {
        for (MidiTempoEvent event : events) {
            if (event != null && Math.max(0, Math.round(event.getTick())) == 0) {
                return true;
            }
        }
        return false;
    }

    private static Integer resolveMusicXmlNoteMidiForPlayback(Element note, int channel, int currentFifths,
            int currentTransposeSemitones, DrumPartMap drumPartMap, Map<String, Integer> measureAccidentalByStepOctave) {
        Element instrument = directChildElementByName(note, "instrument");
        String noteInstrumentId = instrument == null ? "" : trimToEmpty(instrument.getAttribute("id"));
        Element unpitched = directChildElementByName(note, "unpitched");
        String pitchStep = trimToEmpty(getFirstTextByDirectPath(note, "pitch", "step"));
        Integer pitchOctave = getFirstIntegerByDirectPath(note, "pitch", "octave");
        Integer explicitAlter = getFirstIntegerByDirectPath(note, "pitch", "alter");
        Integer accidentalAlter = accidentalTextToAlter(directChildText(note, "accidental"));
        boolean hasUnpitched = unpitched != null;
        Integer drumByInstrumentId = noteInstrumentId.length() > 0 && drumPartMap != null
                ? drumPartMap.getMidiUnpitchedByInstrumentId().get(noteInstrumentId)
                : null;
        boolean drumContext = channel == 10 || hasUnpitched || drumByInstrumentId != null;
        if (drumContext) {
            if (drumByInstrumentId != null) {
                return drumByInstrumentId;
            }
            if (hasUnpitched) {
                String displayStep = trimToEmpty(getFirstTextByDirectPath(note, "unpitched", "display-step"));
                if (displayStep.length() == 0) {
                    displayStep = trimToEmpty(getFirstTextByDirectPath(note, "unpitched", "step"));
                }
                Integer displayOctave = getFirstIntegerByDirectPath(note, "unpitched", "display-octave");
                if (displayOctave == null) {
                    displayOctave = getFirstIntegerByDirectPath(note, "unpitched", "octave");
                }
                Integer displayAlter = getFirstIntegerByDirectPath(note, "unpitched", "display-alter");
                if (displayAlter == null) {
                    displayAlter = getFirstIntegerByDirectPath(note, "unpitched", "alter");
                }
                if (displayOctave != null) {
                    return pitchToMidi(displayStep, displayAlter == null ? 0 : displayAlter.intValue(),
                            displayOctave.intValue());
                }
            }
            if (noteInstrumentId.length() > 0 && drumPartMap != null) {
                String instrumentName = drumPartMap.getInstrumentNameById().get(noteInstrumentId);
                Integer resolved = resolveDrumMidiFromInstrumentName(instrumentName);
                if (resolved != null) {
                    return resolved;
                }
            }
            if (drumPartMap != null && drumPartMap.getDefaultMidiUnpitched() != null) {
                return drumPartMap.getDefaultMidiUnpitched();
            }
        }
        if (pitchOctave == null || pitchStep.length() == 0) {
            return null;
        }
        String stepOctaveKey = pitchStep + pitchOctave;
        int effectiveAlter;
        if (explicitAlter != null) {
            effectiveAlter = explicitAlter.intValue();
            measureAccidentalByStepOctave.put(stepOctaveKey, Integer.valueOf(effectiveAlter));
        } else if (accidentalAlter != null) {
            effectiveAlter = accidentalAlter.intValue();
            measureAccidentalByStepOctave.put(stepOctaveKey, Integer.valueOf(effectiveAlter));
        } else if (measureAccidentalByStepOctave.containsKey(stepOctaveKey)) {
            effectiveAlter = measureAccidentalByStepOctave.get(stepOctaveKey).intValue();
        } else {
            effectiveAlter = keySignatureAlterByStep(currentFifths, pitchStep);
        }
        Integer midi = pitchToMidi(pitchStep, effectiveAlter, pitchOctave.intValue());
        return midi == null ? null : Integer.valueOf(midi.intValue() + currentTransposeSemitones);
    }

    private static boolean isMusicXmlDrumPlaybackContext(Element note, int channel, DrumPartMap drumPartMap) {
        Element instrument = directChildElementByName(note, "instrument");
        String noteInstrumentId = instrument == null ? "" : trimToEmpty(instrument.getAttribute("id"));
        boolean hasUnpitched = directChildElementByName(note, "unpitched") != null;
        boolean hasDrumInstrument = noteInstrumentId.length() > 0 && drumPartMap != null
                && drumPartMap.getMidiUnpitchedByInstrumentId().containsKey(noteInstrumentId);
        return channel == 10 || hasUnpitched || hasDrumInstrument;
    }

    private static List<Integer> buildOrnamentMidiSequence(Element noteNode, int baseMidi, int durTicks,
            int ticksPerQuarter, int currentFifths, Map<String, Integer> measureAccidentalByStepOctave) {
        if (durTicks < 2) {
            return Collections.singletonList(Integer.valueOf(baseMidi));
        }
        Set<String> ornamentTags = new LinkedHashSet<String>();
        Element notations = directChildElementByName(noteNode, "notations");
        if (notations != null) {
            for (Element ornaments : directChildElementsByName(notations, "ornaments")) {
                for (Element ornament : directElementChildren(ornaments)) {
                    ornamentTags.add(ornament.getTagName().toLowerCase());
                }
            }
        }
        if (ornamentTags.isEmpty()) {
            return Collections.singletonList(Integer.valueOf(baseMidi));
        }

        String step = trimToEmpty(getFirstTextByDirectPath(noteNode, "pitch", "step"));
        Integer octave = getFirstIntegerByDirectPath(noteNode, "pitch", "octave");
        if (step.length() == 0 || octave == null) {
            return Collections.singletonList(Integer.valueOf(baseMidi));
        }
        NeighborPitch upperNeighbor = resolveNeighborPitch("up", step, octave.intValue(), currentFifths,
                measureAccidentalByStepOctave);
        NeighborPitch lowerNeighbor = resolveNeighborPitch("down", step, octave.intValue(), currentFifths,
                measureAccidentalByStepOctave);
        Integer upperResolved = upperNeighbor == null ? null
                : pitchToMidi(upperNeighbor.getStep(), upperNeighbor.getAlter(), upperNeighbor.getOctave());
        Integer lowerResolved = lowerNeighbor == null ? null
                : pitchToMidi(lowerNeighbor.getStep(), lowerNeighbor.getAlter(), lowerNeighbor.getOctave());
        int upperMidi = upperResolved == null ? Math.min(127, baseMidi + 2) : upperResolved.intValue();
        int lowerMidi = lowerResolved == null ? Math.max(0, baseMidi - 2) : lowerResolved.intValue();

        if (ornamentTags.contains("trill-mark") || ornamentTags.contains("shake")) {
            int segmentTicks = Math.max(1, Math.round(ticksPerQuarter / 8.0f));
            int count = Math.max(2, Math.min(16, (int) Math.floor(durTicks / (double) segmentTicks)));
            List<Integer> sequence = new ArrayList<Integer>();
            for (int index = 0; index < count; index++) {
                sequence.add(Integer.valueOf(index % 2 == 0 ? baseMidi : upperMidi));
            }
            return sequence;
        }
        if (ornamentTags.contains("turn")) {
            return Arrays.asList(Integer.valueOf(upperMidi), Integer.valueOf(baseMidi), Integer.valueOf(lowerMidi),
                    Integer.valueOf(baseMidi));
        }
        if (ornamentTags.contains("inverted-turn")) {
            return Arrays.asList(Integer.valueOf(lowerMidi), Integer.valueOf(baseMidi), Integer.valueOf(upperMidi),
                    Integer.valueOf(baseMidi));
        }
        if (ornamentTags.contains("mordent")) {
            return Arrays.asList(Integer.valueOf(baseMidi), Integer.valueOf(lowerMidi), Integer.valueOf(baseMidi));
        }
        if (ornamentTags.contains("inverted-mordent")) {
            return Arrays.asList(Integer.valueOf(baseMidi), Integer.valueOf(upperMidi), Integer.valueOf(baseMidi));
        }
        return Collections.singletonList(Integer.valueOf(baseMidi));
    }

    private static NeighborPitch resolveNeighborPitch(String direction, String step, int octave, int currentFifths,
            Map<String, Integer> measureAccidentalByStepOctave) {
        List<String> stepOrder = Arrays.asList("C", "D", "E", "F", "G", "A", "B");
        int currentIndex = stepOrder.indexOf(step);
        if (currentIndex < 0) {
            return null;
        }
        int delta = "up".equals(direction) ? 1 : -1;
        int rawIndex = currentIndex + delta;
        int wrappedIndex = (rawIndex + stepOrder.size()) % stepOrder.size();
        String neighborStep = stepOrder.get(wrappedIndex);
        int neighborOctave = octave;
        if ("up".equals(direction) && "B".equals(step)) {
            neighborOctave++;
        }
        if ("down".equals(direction) && "C".equals(step)) {
            neighborOctave--;
        }
        String stepOctaveKey = neighborStep + neighborOctave;
        Integer accidental = measureAccidentalByStepOctave == null ? null
                : measureAccidentalByStepOctave.get(stepOctaveKey);
        int alter = accidental == null ? keySignatureAlterByStep(currentFifths, neighborStep) : accidental.intValue();
        return new NeighborPitch(neighborStep, neighborOctave, alter);
    }

    private static int readDirectionVelocity(Element directionNode, int fallback) {
        Element sound = directChildElementByName(directionNode, "sound");
        if (sound != null) {
            Double parsed = parseFiniteDouble(sound.getAttribute("dynamics"));
            if (parsed != null && parsed.doubleValue() > 0) {
                return clampVelocity((parsed.doubleValue() / 100.0d) * 127.0d);
            }
        }
        for (Element directionType : directChildElementsByName(directionNode, "direction-type")) {
            for (Element dynamics : directChildElementsByName(directionType, "dynamics")) {
                for (Element child : directElementChildren(dynamics)) {
                    Integer velocity = dynamicsToVelocity(child.getTagName());
                    if (velocity != null) {
                        return velocity.intValue();
                    }
                }
            }
        }
        return clampVelocity(fallback);
    }

    private static WedgeDirective readDirectionWedgeDirective(Element directionNode) {
        List<WedgeStart> starts = new ArrayList<WedgeStart>();
        Set<String> stops = new LinkedHashSet<String>();
        for (Element directionType : directChildElementsByName(directionNode, "direction-type")) {
            for (Element wedge : directChildElementsByName(directionType, "wedge")) {
                String type = trimToEmpty(wedge.getAttribute("type")).toLowerCase();
                String number = trimToEmpty(wedge.getAttribute("number"));
                if (number.isEmpty()) {
                    number = "1";
                }
                if ("crescendo".equals(type) || "diminuendo".equals(type)) {
                    starts.add(new WedgeStart(number, type));
                }
                if ("stop".equals(type)) {
                    stops.add(number);
                }
            }
        }
        return new WedgeDirective(starts, stops);
    }

    private static NoteArticulationAdjustments getNoteArticulationAdjustments(Element noteNode) {
        int velocityDelta = 0;
        double durationRatio = 1.0d;
        boolean hasTenuto = false;
        Element notations = directChildElementByName(noteNode, "notations");
        if (notations != null) {
            for (Element articulations : directChildElementsByName(notations, "articulations")) {
                for (Element articulation : directElementChildren(articulations)) {
                    String tag = articulation.getTagName();
                    if ("strong-accent".equals(tag)) {
                        velocityDelta += 24;
                    }
                    if ("accent".equals(tag)) {
                        velocityDelta += 14;
                    }
                    if ("staccatissimo".equals(tag)) {
                        durationRatio = Math.min(durationRatio, 0.35d);
                    }
                    if ("staccato".equals(tag)) {
                        durationRatio = Math.min(durationRatio, 0.55d);
                    }
                    if ("tenuto".equals(tag)) {
                        hasTenuto = true;
                        durationRatio = Math.max(durationRatio, 1.0d);
                    }
                }
            }
        }
        return new NoteArticulationAdjustments(velocityDelta, durationRatio, hasTenuto);
    }

    private static boolean hasExplicitArticulation(Element noteNode) {
        Element notations = directChildElementByName(noteNode, "notations");
        if (notations == null) {
            return false;
        }
        for (Element articulations : directChildElementsByName(notations, "articulations")) {
            if (!directElementChildren(articulations).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static TemporalExpressionAdjustments getTemporalExpressionAdjustments(Element noteNode, int baseDurTicks,
            int ticksPerQuarter) {
        Element notations = directChildElementByName(noteNode, "notations");
        boolean hasFermata = notations != null && directChildElementByName(notations, "fermata") != null;
        boolean hasCaesura = false;
        if (notations != null) {
            if (directChildElementByName(notations, "caesura") != null) {
                hasCaesura = true;
            } else {
                for (Element articulations : directChildElementsByName(notations, "articulations")) {
                    if (directChildElementByName(articulations, "caesura") != null) {
                        hasCaesura = true;
                        break;
                    }
                }
            }
        }
        if (!hasFermata && !hasCaesura) {
            return TemporalExpressionAdjustments.NONE;
        }

        int durationExtraTicks = 0;
        int postPauseTicks = 0;
        if (hasFermata) {
            durationExtraTicks += Math.max(Math.round(baseDurTicks * 0.35f),
                    Math.max(1, Math.round(ticksPerQuarter / 8.0f)));
            postPauseTicks += Math.max(1, Math.round(ticksPerQuarter / 6.0f));
        }
        if (hasCaesura) {
            durationExtraTicks += Math.max(0, Math.round(baseDurTicks * 0.12f));
            postPauseTicks += Math.max(1, Math.round(ticksPerQuarter / 4.0f));
        }
        return new TemporalExpressionAdjustments(durationExtraTicks, postPauseTicks);
    }

    private static TieFlags getTieFlags(Element noteNode) {
        boolean start = false;
        boolean stop = false;
        for (Element child : directElementChildren(noteNode)) {
            if ("tie".equals(child.getTagName())) {
                String type = trimToEmpty(child.getAttribute("type")).toLowerCase();
                if ("start".equals(type)) {
                    start = true;
                }
                if ("stop".equals(type)) {
                    stop = true;
                }
            }
        }
        Element notations = directChildElementByName(noteNode, "notations");
        if (notations != null) {
            for (Element tied : directChildElementsByName(notations, "tied")) {
                String type = trimToEmpty(tied.getAttribute("type")).toLowerCase();
                if ("start".equals(type)) {
                    start = true;
                }
                if ("stop".equals(type)) {
                    stop = true;
                }
            }
        }
        return new TieFlags(start, stop);
    }

    private static SlurNumbers getSlurNumbers(Element noteNode) {
        List<String> starts = new ArrayList<String>();
        List<String> stops = new ArrayList<String>();
        Element notations = directChildElementByName(noteNode, "notations");
        if (notations != null) {
            for (Element slur : directChildElementsByName(notations, "slur")) {
                String type = trimToEmpty(slur.getAttribute("type")).toLowerCase();
                String number = trimToEmpty(slur.getAttribute("number"));
                if (number.isEmpty()) {
                    number = "1";
                }
                if ("start".equals(type)) {
                    starts.add(number);
                }
                if ("stop".equals(type)) {
                    stops.add(number);
                }
            }
        }
        return new SlurNumbers(starts, stops);
    }

    private static String resolveFallbackTieChainKey(Map<String, Integer> tieChainIndexByKey, String voice, int channel,
            int midiNumber) {
        String suffix = "|" + channel + "|" + midiNumber;
        String exact = (voice == null ? "" : voice) + suffix;
        if (tieChainIndexByKey.containsKey(exact)) {
            return exact;
        }
        String candidate = null;
        for (String key : tieChainIndexByKey.keySet()) {
            if (key != null && key.endsWith(suffix)) {
                if (candidate != null) {
                    return null;
                }
                candidate = key;
            }
        }
        return candidate;
    }

    private static boolean hasTimeSignatureEventAtZero(List<MidiTimeSignatureEvent> events) {
        for (MidiTimeSignatureEvent event : events) {
            if (event != null && Math.max(0, Math.round(event.getStartTicks())) == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasKeySignatureEventAtZero(List<MidiKeySignatureEvent> events) {
        for (MidiKeySignatureEvent event : events) {
            if (event != null && Math.max(0, Math.round(event.getStartTicks())) == 0) {
                return true;
            }
        }
        return false;
    }

    private static String getFirstTextByDirectPath(Element parent, String... names) {
        Element current = parent;
        if (names == null) {
            return "";
        }
        for (String name : names) {
            current = directChildElementByName(current, name);
            if (current == null) {
                return "";
            }
        }
        return current.getTextContent();
    }

    private static Double parseFiniteDouble(String value) {
        String text = trimToEmpty(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(text);
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? null : Double.valueOf(parsed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static double firstSoundTempoInDocument(Document doc, double fallback) {
        Element root = doc == null ? null : doc.getDocumentElement();
        Element sound = firstDescendantElementWithAttribute(root, "sound", "tempo");
        Double parsed = sound == null ? null : parseFiniteDouble(sound.getAttribute("tempo"));
        return parsed == null ? fallback : parsed.doubleValue();
    }

    private static Double directDirectionSoundTempo(Element direction) {
        Element sound = directChildElementByName(direction, "sound");
        return sound == null ? null : parseFiniteDouble(sound.getAttribute("tempo"));
    }

    private static Double directionMetronomePerMinute(Element direction) {
        for (Element directionType : directChildElementsByName(direction, "direction-type")) {
            for (Element metronome : directChildElementsByName(directionType, "metronome")) {
                Element perMinute = directChildElementByName(metronome, "per-minute");
                if (perMinute != null) {
                    Double parsed = parseFiniteDouble(perMinute.getTextContent());
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
        }
        return null;
    }

    private static String firstDescendantTextByName(Element parent, String name) {
        Element element = firstDescendantElementByName(parent, name);
        return element == null ? "" : element.getTextContent();
    }

    private static List<Element> directPedalElements(Element direction) {
        List<Element> out = new ArrayList<Element>();
        for (Element directionType : directChildElementsByName(direction, "direction-type")) {
            out.addAll(directChildElementsByName(directionType, "pedal"));
        }
        return out;
    }

    private static Element firstDescendantElementWithAttribute(Element parent, String name, String attributeName) {
        if (parent == null) {
            return null;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                Element element = (Element) node;
                if (name.equals(element.getNodeName()) && element.hasAttribute(attributeName)) {
                    return element;
                }
                Element descendant = firstDescendantElementWithAttribute(element, name, attributeName);
                if (descendant != null) {
                    return descendant;
                }
            }
        }
        return null;
    }

    private static Element firstDescendantElementByName(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                Element element = (Element) node;
                if (name.equals(element.getNodeName())) {
                    return element;
                }
                Element descendant = firstDescendantElementByName(element, name);
                if (descendant != null) {
                    return descendant;
                }
            }
        }
        return null;
    }

    private static Element previousElementSibling(Element element) {
        if (element == null) {
            return null;
        }
        for (Node node = element.getPreviousSibling(); node != null; node = node.getPreviousSibling()) {
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
    }

    private static int normalizeMidiChannel(double channel) {
        return Math.max(1, Math.min(16, (int) Math.round(channel)));
    }

    private static String normalizeRawMidiRetriggerPolicy(String policy) {
        if ("on_before_off".equals(policy) || "pitch_order".equals(policy)) {
            return policy;
        }
        return "off_before_on";
    }

    private static void writeBytes(ByteArrayOutputStream out, byte[] bytes) {
        if (out == null || bytes == null) {
            return;
        }
        for (byte value : bytes) {
            out.write(value & 0xff);
        }
    }

    private static byte[] stripInitialDeltaZero(byte[] bytes) {
        if (bytes == null || bytes.length <= 1) {
            return new byte[0];
        }
        return Arrays.copyOfRange(bytes, numberToVariableLength(0).length, bytes.length);
    }

    private static void appendLine(StringBuilder out, String line) {
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(line == null ? "" : line);
    }

    private static String joinWithPipe(int[] values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (int index = 0; index < values.length; index++) {
                if (index > 0) {
                    out.append('|');
                }
                out.append(values[index]);
            }
        }
        return out.toString();
    }

    private static String encodeURIComponent(String text) {
        String safeText = text == null ? "" : text;
        byte[] bytes = safeText.getBytes(StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder();
        for (byte raw : bytes) {
            int value = raw & 0xff;
            if ((value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z')
                    || (value >= '0' && value <= '9') || value == '-' || value == '_' || value == '.'
                    || value == '!' || value == '~' || value == '*' || value == '\'' || value == '('
                    || value == ')') {
                out.append((char) value);
            } else {
                out.append('%').append(padLeft(Integer.toHexString(value).toUpperCase(), 2, '0'));
            }
        }
        return out.toString();
    }

    private static int resolveCcValueAtTick(List<ControllerValuePoint> events, int tick) {
        int current = 127;
        for (ControllerValuePoint event : events) {
            if (event.getTick() > tick) {
                break;
            }
            current = event.getValue();
        }
        return current;
    }

    private static int parseIntOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static List<Integer> integers(int... values) {
        List<Integer> out = new ArrayList<Integer>();
        for (int value : values) {
            out.add(Integer.valueOf(value));
        }
        return out;
    }

    private static final class MidiExportMetaTimelineEvent {
        private final String kind;
        private final int startTicks;
        private final int bpm;
        private final int beats;
        private final int beatType;
        private final int fifths;
        private final String mode;

        private MidiExportMetaTimelineEvent(String kind, int startTicks, int bpm, int beats, int beatType,
                int fifths, String mode) {
            this.kind = kind;
            this.startTicks = Math.max(0, Math.round(startTicks));
            this.bpm = bpm;
            this.beats = beats;
            this.beatType = beatType;
            this.fifths = fifths;
            this.mode = mode;
        }

        private static MidiExportMetaTimelineEvent tempo(MidiTempoEvent event) {
            return new MidiExportMetaTimelineEvent("tempo", event.getTick(), event.getBpm(), 0, 0, 0, "major");
        }

        private static MidiExportMetaTimelineEvent time(MidiTimeSignatureEvent event) {
            return new MidiExportMetaTimelineEvent("time", event.getStartTicks(), 0, event.getBeats(),
                    event.getBeatType(), 0, "major");
        }

        private static MidiExportMetaTimelineEvent key(MidiKeySignatureEvent event) {
            return new MidiExportMetaTimelineEvent("key", event.getStartTicks(), 0, 0, 0, event.getFifths(),
                    event.getMode());
        }

        private String getKind() {
            return kind;
        }

        private int getKindPriority() {
            if ("time".equals(kind)) {
                return 0;
            }
            if ("key".equals(kind)) {
                return 1;
            }
            return 2;
        }

        private int getStartTicks() {
            return startTicks;
        }

        private int getBpm() {
            return bpm;
        }

        private int getBeats() {
            return beats;
        }

        private int getBeatType() {
            return beatType;
        }

        private int getFifths() {
            return fifths;
        }

        private String getMode() {
            return mode;
        }
    }

    public static final class MidiTickTimeSignatureEvent {
        private final int tick;
        private final int beats;
        private final int beatType;

        public MidiTickTimeSignatureEvent(int tick, int beats, int beatType) {
            this.tick = Math.max(0, Math.round(tick));
            this.beats = Math.max(1, Math.round(beats));
            this.beatType = Math.max(1, Math.round(beatType));
        }

        public int getTick() {
            return tick;
        }

        public int getBeats() {
            return beats;
        }

        public int getBeatType() {
            return beatType;
        }
    }

    public static final class MidiTickKeySignatureEvent {
        private final int tick;
        private final int fifths;
        private final String mode;

        public MidiTickKeySignatureEvent(int tick, int fifths, String mode) {
            this.tick = Math.max(0, Math.round(tick));
            this.fifths = Math.max(-7, Math.min(7, Math.round(fifths)));
            this.mode = "minor".equals(mode) ? "minor" : "major";
        }

        public int getTick() {
            return tick;
        }

        public int getFifths() {
            return fifths;
        }

        public String getMode() {
            return mode;
        }
    }

    public static final class MidiTempoEvent {
        private final int tick;
        private final int bpm;

        public MidiTempoEvent(int tick, int bpm) {
            this.tick = Math.max(0, Math.round(tick));
            this.bpm = clampTempo(bpm);
        }

        public int getTick() {
            return tick;
        }

        public int getBpm() {
            return bpm;
        }
    }

    public static final class MidiControllerEvent {
        private final int trackIndex;
        private final int tick;
        private final int channel;
        private final int controllerNumber;
        private final int controllerValue;

        public MidiControllerEvent(int tick, int channel, int controllerNumber, int controllerValue) {
            this(0, tick, channel, controllerNumber, controllerValue);
        }

        public MidiControllerEvent(int trackIndex, int tick, int channel, int controllerNumber, int controllerValue) {
            this.trackIndex = Math.max(0, Math.round(trackIndex));
            this.tick = Math.max(0, Math.round(tick));
            this.channel = Math.max(1, Math.round(channel));
            this.controllerNumber = Math.max(0, Math.min(127, Math.round(controllerNumber)));
            this.controllerValue = Math.max(0, Math.min(127, Math.round(controllerValue)));
        }

        public int getTrackIndex() {
            return trackIndex;
        }

        public int getTick() {
            return tick;
        }

        public int getChannel() {
            return channel;
        }

        public int getControllerNumber() {
            return controllerNumber;
        }

        public int getControllerValue() {
            return controllerValue;
        }
    }

    public static final class MidiTimeSignatureEvent {
        private final int startTicks;
        private final int beats;
        private final int beatType;

        public MidiTimeSignatureEvent(int startTicks, int beats, int beatType) {
            this.startTicks = Math.max(0, Math.round(startTicks));
            this.beats = Math.max(1, Math.round(beats));
            this.beatType = Math.max(1, Math.round(beatType));
        }

        public int getStartTicks() {
            return startTicks;
        }

        public int getBeats() {
            return beats;
        }

        public int getBeatType() {
            return beatType;
        }
    }

    public static final class MidiKeySignatureEvent {
        private final int startTicks;
        private final int fifths;
        private final String mode;

        public MidiKeySignatureEvent(int startTicks, int fifths, String mode) {
            this.startTicks = Math.max(0, Math.round(startTicks));
            this.fifths = Math.max(-7, Math.min(7, Math.round(fifths)));
            this.mode = "minor".equals(mode) ? "minor" : "major";
        }

        public int getStartTicks() {
            return startTicks;
        }

        public int getFifths() {
            return fifths;
        }

        public String getMode() {
            return mode;
        }
    }

    public static final class MidiExportTextMetaLines {
        private final String metaTrackTitle;
        private final List<String> standardTextMetaLines;
        private final List<String> mksTextMetaLines;
        private final int pickupTicks;

        private MidiExportTextMetaLines(String metaTrackTitle, List<String> standardTextMetaLines,
                List<String> mksTextMetaLines, int pickupTicks) {
            this.metaTrackTitle = trimToEmpty(metaTrackTitle);
            this.standardTextMetaLines = standardTextMetaLines == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(standardTextMetaLines));
            this.mksTextMetaLines = mksTextMetaLines == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(mksTextMetaLines));
            this.pickupTicks = Math.max(0, Math.round(pickupTicks));
        }

        public String getMetaTrackTitle() {
            return metaTrackTitle;
        }

        public List<String> getStandardTextMetaLines() {
            return standardTextMetaLines;
        }

        public List<String> getMksTextMetaLines() {
            return mksTextMetaLines;
        }

        public int getPickupTicks() {
            return pickupTicks;
        }
    }

    public static final class MidiExportPlaybackPreparation {
        private final int ticksPerQuarter;
        private final List<RawMidiPlaybackEvent> sourceEvents;
        private final MidiPlaybackTrackGrouping trackGrouping;
        private final MidiExportTextMetaLines textMetaLines;
        private final List<MidiTempoEvent> tempoEvents;
        private final List<MidiTimeSignatureEvent> timeSignatureEvents;
        private final List<MidiKeySignatureEvent> keySignatureEvents;
        private final List<String> diagnostics;
        private final List<String> sysexChunks;
        private final String programPreset;
        private final boolean embedMksSysEx;

        private MidiExportPlaybackPreparation(int ticksPerQuarter, List<RawMidiPlaybackEvent> sourceEvents,
                MidiPlaybackTrackGrouping trackGrouping, MidiExportTextMetaLines textMetaLines,
                List<MidiTempoEvent> tempoEvents, List<MidiTimeSignatureEvent> timeSignatureEvents,
                List<MidiKeySignatureEvent> keySignatureEvents, List<String> diagnostics,
                List<String> sysexChunks, String programPreset, boolean embedMksSysEx) {
            this.ticksPerQuarter = normalizeTicksPerQuarter(ticksPerQuarter);
            this.sourceEvents = sourceEvents == null ? Collections.<RawMidiPlaybackEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<RawMidiPlaybackEvent>(sourceEvents));
            this.trackGrouping = trackGrouping == null ? buildMidiPlaybackTracksById(this.sourceEvents)
                    : trackGrouping;
            this.textMetaLines = textMetaLines == null
                    ? buildMidiExportTextMetaLines(null, null, null, 0, null, null, true)
                    : textMetaLines;
            this.tempoEvents = tempoEvents == null ? Collections.<MidiTempoEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiTempoEvent>(tempoEvents));
            this.timeSignatureEvents = timeSignatureEvents == null
                    ? Collections.<MidiTimeSignatureEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiTimeSignatureEvent>(timeSignatureEvents));
            this.keySignatureEvents = keySignatureEvents == null
                    ? Collections.<MidiKeySignatureEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiKeySignatureEvent>(keySignatureEvents));
            this.diagnostics = diagnostics == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(diagnostics));
            this.sysexChunks = sysexChunks == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(sysexChunks));
            this.programPreset = normalizeMidiProgramPreset(programPreset);
            this.embedMksSysEx = embedMksSysEx;
        }

        public int getTicksPerQuarter() {
            return ticksPerQuarter;
        }

        public List<RawMidiPlaybackEvent> getSourceEvents() {
            return sourceEvents;
        }

        public MidiPlaybackTrackGrouping getTrackGrouping() {
            return trackGrouping;
        }

        public MidiExportTextMetaLines getTextMetaLines() {
            return textMetaLines;
        }

        public List<MidiTempoEvent> getTempoEvents() {
            return tempoEvents;
        }

        public List<MidiTimeSignatureEvent> getTimeSignatureEvents() {
            return timeSignatureEvents;
        }

        public List<MidiKeySignatureEvent> getKeySignatureEvents() {
            return keySignatureEvents;
        }

        public List<String> getDiagnostics() {
            return diagnostics;
        }

        public List<String> getSysexChunks() {
            return sysexChunks;
        }

        public String getProgramPreset() {
            return programPreset;
        }

        public boolean isEmbedMksSysEx() {
            return embedMksSysEx;
        }

        public List<String> getCombinedTextMetaLines() {
            List<String> out = new ArrayList<String>();
            out.addAll(textMetaLines.getStandardTextMetaLines());
            out.addAll(textMetaLines.getMksTextMetaLines());
            return Collections.unmodifiableList(out);
        }
    }

    public static final class MidiExportPlaybackBuildResult {
        private final MidiExportPlaybackPreparation preparation;
        private final boolean rawWriter;
        private final byte[] rawBytes;
        private final MidiExportWriterTrackPlan writerTrackPlan;

        private MidiExportPlaybackBuildResult(MidiExportPlaybackPreparation preparation, boolean rawWriter,
                byte[] rawBytes, MidiExportWriterTrackPlan writerTrackPlan) {
            this.preparation = preparation;
            this.rawWriter = rawWriter;
            this.rawBytes = rawBytes == null ? null : Arrays.copyOf(rawBytes, rawBytes.length);
            this.writerTrackPlan = writerTrackPlan;
        }

        private static MidiExportPlaybackBuildResult raw(MidiExportPlaybackPreparation preparation, byte[] rawBytes) {
            return new MidiExportPlaybackBuildResult(preparation, true, rawBytes, null);
        }

        private static MidiExportPlaybackBuildResult writer(MidiExportPlaybackPreparation preparation,
                MidiExportWriterTrackPlan writerTrackPlan) {
            return new MidiExportPlaybackBuildResult(preparation, false, null, writerTrackPlan);
        }

        public MidiExportPlaybackPreparation getPreparation() {
            return preparation;
        }

        public boolean isRawWriter() {
            return rawWriter;
        }

        public byte[] getRawBytes() {
            return rawBytes == null ? null : Arrays.copyOf(rawBytes, rawBytes.length);
        }

        public MidiExportWriterTrackPlan getWriterTrackPlan() {
            return writerTrackPlan;
        }
    }

    public static final class MidiPlaybackTrackGrouping {
        private final Map<String, List<RawMidiPlaybackEvent>> tracksById;
        private final List<String> sortedTrackIds;
        private final Map<String, String> trackNameById;

        private MidiPlaybackTrackGrouping(Map<String, List<RawMidiPlaybackEvent>> tracksById,
                List<String> sortedTrackIds, Map<String, String> trackNameById) {
            Map<String, List<RawMidiPlaybackEvent>> safeTracks =
                    new LinkedHashMap<String, List<RawMidiPlaybackEvent>>();
            if (tracksById != null) {
                for (Map.Entry<String, List<RawMidiPlaybackEvent>> entry : tracksById.entrySet()) {
                    safeTracks.put(entry.getKey(), entry.getValue() == null
                            ? Collections.<RawMidiPlaybackEvent>emptyList()
                            : Collections.unmodifiableList(new ArrayList<RawMidiPlaybackEvent>(entry.getValue())));
                }
            }
            this.tracksById = Collections.unmodifiableMap(safeTracks);
            this.sortedTrackIds = sortedTrackIds == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(sortedTrackIds));
            this.trackNameById = trackNameById == null ? Collections.<String, String>emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<String, String>(trackNameById));
        }

        public Map<String, List<RawMidiPlaybackEvent>> getTracksById() {
            return tracksById;
        }

        public List<String> getSortedTrackIds() {
            return sortedTrackIds;
        }

        public Map<String, String> getTrackNameById() {
            return trackNameById;
        }
    }

    public static final class MidiExportPlaybackTrackPlan {
        private final List<RawMidiPlaybackEvent> trackEvents;
        private final String trackName;
        private final List<Integer> channels;
        private final int selectedInstrumentProgram;

        private MidiExportPlaybackTrackPlan(List<RawMidiPlaybackEvent> trackEvents, String trackName,
                List<Integer> channels, int selectedInstrumentProgram) {
            this.trackEvents = trackEvents == null ? Collections.<RawMidiPlaybackEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<RawMidiPlaybackEvent>(trackEvents));
            this.trackName = trimToEmpty(trackName);
            this.channels = channels == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(channels));
            this.selectedInstrumentProgram = selectedInstrumentProgram;
        }

        public List<RawMidiPlaybackEvent> getTrackEvents() {
            return trackEvents;
        }

        public String getTrackName() {
            return trackName;
        }

        public List<Integer> getChannels() {
            return channels;
        }

        public int getSelectedInstrumentProgram() {
            return selectedInstrumentProgram;
        }
    }

    public static final class MidiExportProgramChangeEventFields {
        private final int instrument;
        private final int channel;
        private final int delta;

        private MidiExportProgramChangeEventFields(int instrument, int channel, int delta) {
            this.instrument = instrument;
            this.channel = normalizeMidiChannel(channel);
            this.delta = Math.max(0, delta);
        }

        public int getInstrument() {
            return instrument;
        }

        public int getChannel() {
            return channel;
        }

        public int getDelta() {
            return delta;
        }
    }

    public static final class MidiWriterNoteEventFields {
        private final List<String> pitch;
        private final String duration;
        private final Integer startTick;
        private final int velocity;
        private final int channel;

        private MidiWriterNoteEventFields(List<String> pitch, String duration, Integer startTick,
                int velocity, int channel) {
            this.pitch = pitch == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(pitch));
            this.duration = duration == null ? "" : duration;
            this.startTick = startTick;
            this.velocity = clampVelocity(velocity);
            this.channel = normalizeMidiChannel(channel);
        }

        public List<String> getPitch() {
            return pitch;
        }

        public String getDuration() {
            return duration;
        }

        public Integer getStartTick() {
            return startTick;
        }

        public int getVelocity() {
            return velocity;
        }

        public int getChannel() {
            return channel;
        }
    }

    public static final class MidiExportControlTrackPlan {
        private final String controlKey;
        private final List<RawMidiControlEvent> controlEvents;
        private final String trackName;
        private final List<MidiWriterControllerChangeEventFields> controllerChangeFields;

        private MidiExportControlTrackPlan(String controlKey, List<RawMidiControlEvent> controlEvents,
                String trackName, List<MidiWriterControllerChangeEventFields> controllerChangeFields) {
            this.controlKey = controlKey == null ? "" : controlKey;
            this.controlEvents = controlEvents == null ? Collections.<RawMidiControlEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<RawMidiControlEvent>(controlEvents));
            this.trackName = trackName == null ? "" : trackName;
            this.controllerChangeFields = controllerChangeFields == null
                    ? Collections.<MidiWriterControllerChangeEventFields>emptyList()
                    : Collections.unmodifiableList(
                            new ArrayList<MidiWriterControllerChangeEventFields>(controllerChangeFields));
        }

        public String getControlKey() {
            return controlKey;
        }

        public List<RawMidiControlEvent> getControlEvents() {
            return controlEvents;
        }

        public String getTrackName() {
            return trackName;
        }

        public List<MidiWriterControllerChangeEventFields> getControllerChangeFields() {
            return controllerChangeFields;
        }
    }

    public static final class MidiWriterControllerChangeEventFields {
        private final int channel;
        private final int statusByte;
        private final int controllerNumber;
        private final int controllerValue;
        private final int delta;

        private MidiWriterControllerChangeEventFields(int channel, int controllerNumber, int controllerValue,
                int delta) {
            this.channel = normalizeMidiChannel(channel);
            this.statusByte = 0xb0 + this.channel - 1;
            this.controllerNumber = Math.max(0, Math.min(127, Math.round(controllerNumber)));
            this.controllerValue = Math.max(0, Math.min(127, Math.round(controllerValue)));
            this.delta = Math.max(0, Math.round(delta));
        }

        public int getChannel() {
            return channel;
        }

        public int getStatusByte() {
            return statusByte;
        }

        public int getControllerNumber() {
            return controllerNumber;
        }

        public int getControllerValue() {
            return controllerValue;
        }

        public int getDelta() {
            return delta;
        }
    }

    public static final class MidiExportWriterTrackPlan {
        private final String metaTrackName;
        private final List<byte[]> metaEventData;
        private final List<MidiExportPlaybackTrackPlan> playbackTrackPlans;
        private final List<MidiExportControlTrackPlan> controlTrackPlans;

        private MidiExportWriterTrackPlan(String metaTrackName, List<byte[]> metaEventData,
                List<MidiExportPlaybackTrackPlan> playbackTrackPlans,
                List<MidiExportControlTrackPlan> controlTrackPlans) {
            this.metaTrackName = trimToEmpty(metaTrackName);
            List<byte[]> safeMetaEventData = new ArrayList<byte[]>();
            if (metaEventData != null) {
                for (byte[] eventData : metaEventData) {
                    if (eventData != null) {
                        safeMetaEventData.add(Arrays.copyOf(eventData, eventData.length));
                    }
                }
            }
            this.metaEventData = Collections.unmodifiableList(safeMetaEventData);
            this.playbackTrackPlans = playbackTrackPlans == null
                    ? Collections.<MidiExportPlaybackTrackPlan>emptyList()
                    : Collections.unmodifiableList(
                            new ArrayList<MidiExportPlaybackTrackPlan>(playbackTrackPlans));
            this.controlTrackPlans = controlTrackPlans == null
                    ? Collections.<MidiExportControlTrackPlan>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiExportControlTrackPlan>(controlTrackPlans));
        }

        public String getMetaTrackName() {
            return metaTrackName;
        }

        public List<byte[]> getMetaEventData() {
            List<byte[]> out = new ArrayList<byte[]>();
            for (byte[] eventData : metaEventData) {
                out.add(Arrays.copyOf(eventData, eventData.length));
            }
            return Collections.unmodifiableList(out);
        }

        public List<MidiExportPlaybackTrackPlan> getPlaybackTrackPlans() {
            return playbackTrackPlans;
        }

        public List<MidiExportControlTrackPlan> getControlTrackPlans() {
            return controlTrackPlans;
        }

        public int getTrackCount() {
            return 1 + playbackTrackPlans.size() + controlTrackPlans.size();
        }
    }

    public static final class LeadingPickupTimeSignatureNormalization {
        private final List<MidiTickTimeSignatureEvent> events;
        private final boolean normalized;
        private final int pickupTicks;

        private LeadingPickupTimeSignatureNormalization(List<MidiTickTimeSignatureEvent> events, boolean normalized,
                int pickupTicks) {
            this.events = events == null ? Collections.<MidiTickTimeSignatureEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiTickTimeSignatureEvent>(events));
            this.normalized = normalized;
            this.pickupTicks = Math.max(0, pickupTicks);
        }

        public List<MidiTickTimeSignatureEvent> getEvents() {
            return events;
        }

        public boolean isNormalized() {
            return normalized;
        }

        public int getPickupTicks() {
            return pickupTicks;
        }
    }

    public static final class ImportedQuantizedNote {
        private final int trackIndex;
        private final int channel;
        private final int midi;
        private final int startTick;
        private final int endTick;
        private final int velocity;

        public ImportedQuantizedNote(int midi, int startTick, int endTick) {
            this(0, 1, midi, startTick, endTick, 80);
        }

        public ImportedQuantizedNote(int trackIndex, int channel, int midi, int startTick, int endTick, int velocity) {
            this.trackIndex = Math.max(0, Math.round(trackIndex));
            this.channel = Math.max(1, Math.round(channel));
            this.midi = Math.max(0, Math.min(127, Math.round(midi)));
            this.startTick = Math.max(0, Math.round(startTick));
            this.endTick = Math.max(this.startTick, Math.round(endTick));
            this.velocity = clampVelocity(velocity);
        }

        public int getTrackIndex() {
            return trackIndex;
        }

        public int getChannel() {
            return channel;
        }

        public int getMidi() {
            return midi;
        }

        public int getStartTick() {
            return startTick;
        }

        public int getEndTick() {
            return endTick;
        }

        public int getVelocity() {
            return velocity;
        }
    }

    private static final class ControllerValuePoint {
        private final int tick;
        private final int value;

        private ControllerValuePoint(int tick, int value) {
            this.tick = Math.max(0, Math.round(tick));
            this.value = Math.max(0, Math.min(127, Math.round(value)));
        }

        public int getTick() {
            return tick;
        }

        public int getValue() {
            return value;
        }
    }

    private static final class ControllerValueBucket {
        private final List<ControllerValuePoint> cc7 = new ArrayList<ControllerValuePoint>();
        private final List<ControllerValuePoint> cc11 = new ArrayList<ControllerValuePoint>();

        public List<ControllerValuePoint> getCc7() {
            return cc7;
        }

        public List<ControllerValuePoint> getCc11() {
            return cc11;
        }
    }

    private static final class AutoVoiceState {
        private final int lastEnd;
        private final int lastPitch;

        private AutoVoiceState(int lastEnd, int lastPitch) {
            this.lastEnd = Math.max(0, Math.round(lastEnd));
            this.lastPitch = Math.max(0, Math.min(127, Math.round(lastPitch)));
        }

        public int getLastEnd() {
            return lastEnd;
        }

        public int getLastPitch() {
            return lastPitch;
        }
    }

    public static final class MidiKeySignature {
        private final int fifths;
        private final String mode;

        public MidiKeySignature(int fifths, String mode) {
            this.fifths = Math.max(-7, Math.min(7, Math.round(fifths)));
            this.mode = "minor".equals(mode) ? "minor" : "major";
        }

        public int getFifths() {
            return fifths;
        }

        public String getMode() {
            return mode;
        }
    }

    private static final class WeightedSplitOrder {
        private final int index;
        private final double fraction;

        private WeightedSplitOrder(int index, double fraction) {
            this.index = index;
            this.fraction = fraction;
        }

        public int getIndex() {
            return index;
        }

        public double getFraction() {
            return fraction;
        }
    }

    public static final class SmfImportedNote {
        private final int trackIndex;
        private final int channel;
        private final int midi;
        private final int startTick;
        private final int endTick;
        private final int velocity;

        public SmfImportedNote(int startTick, int endTick) {
            this(0, 1, 60, startTick, endTick, 80);
        }

        public SmfImportedNote(int trackIndex, int channel, int midi, int startTick, int endTick, int velocity) {
            this.trackIndex = Math.max(0, Math.round(trackIndex));
            this.channel = Math.max(1, Math.round(channel));
            this.midi = Math.max(0, Math.min(127, Math.round(midi)));
            this.startTick = Math.max(0, Math.round(startTick));
            this.endTick = Math.max(this.startTick, Math.round(endTick));
            this.velocity = clampVelocity(velocity);
        }

        public int getTrackIndex() {
            return trackIndex;
        }

        public int getChannel() {
            return channel;
        }

        public int getMidi() {
            return midi;
        }

        public int getStartTick() {
            return startTick;
        }

        public int getEndTick() {
            return endTick;
        }

        public int getVelocity() {
            return velocity;
        }
    }

    private static final class ActiveNoteStart {
        private final int startTick;
        private final int velocity;

        private ActiveNoteStart(int startTick, int velocity) {
            this.startTick = Math.max(0, Math.round(startTick));
            this.velocity = clampVelocity(velocity);
        }

        public int getStartTick() {
            return startTick;
        }

        public int getVelocity() {
            return velocity;
        }
    }

    public static final class ImportQuantizeResolution {
        private final int qTick;
        private final int divisions;

        public ImportQuantizeResolution(int qTick, int divisions) {
            this.qTick = Math.max(1, Math.round(qTick));
            this.divisions = Math.max(1, Math.round(divisions));
        }

        public int getQTick() {
            return qTick;
        }

        public int getDivisions() {
            return divisions;
        }
    }

    public static final class VariableLengthValue {
        private final int value;
        private final int next;

        public VariableLengthValue(int value, int next) {
            this.value = Math.max(0, Math.round(value));
            this.next = Math.max(0, Math.round(next));
        }

        public int getValue() {
            return value;
        }

        public int getNext() {
            return next;
        }
    }

    public static final class MksMidiTextMetadata {
        private String title;
        private String movementTitle;
        private String composer;
        private Integer pickupTicks;
        private final Map<Integer, String> partNameByTrackIndex = new LinkedHashMap<Integer, String>();

        public String getTitle() {
            return title;
        }

        private void setTitle(String title) {
            this.title = title;
        }

        public String getMovementTitle() {
            return movementTitle;
        }

        private void setMovementTitle(String movementTitle) {
            this.movementTitle = movementTitle;
        }

        public String getComposer() {
            return composer;
        }

        private void setComposer(String composer) {
            this.composer = composer;
        }

        public Integer getPickupTicks() {
            return pickupTicks;
        }

        private void setPickupTicks(Integer pickupTicks) {
            this.pickupTicks = pickupTicks;
        }

        public Map<Integer, String> getPartNameByTrackIndex() {
            return partNameByTrackIndex;
        }
    }

    public static final class MksSysExChunk {
        private final int messageId;
        private final int chunkIndex;
        private final int totalChunks;
        private final String data;

        public MksSysExChunk(int messageId, int chunkIndex, int totalChunks, String data) {
            this.messageId = Math.max(0, Math.round(messageId));
            this.chunkIndex = Math.max(1, Math.round(chunkIndex));
            this.totalChunks = Math.max(1, Math.round(totalChunks));
            this.data = data == null ? "" : data;
        }

        public int getMessageId() {
            return messageId;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public String getData() {
            return data;
        }
    }

    public static final class ParsedSmfHeader {
        private final int format;
        private final int trackCount;
        private final int ticksPerQuarter;
        private final int nextOffset;

        public ParsedSmfHeader(int format, int trackCount, int ticksPerQuarter, int nextOffset) {
            this.format = Math.max(0, Math.round(format));
            this.trackCount = Math.max(0, Math.round(trackCount));
            this.ticksPerQuarter = Math.max(1, Math.round(ticksPerQuarter));
            this.nextOffset = Math.max(0, Math.round(nextOffset));
        }

        public int getFormat() {
            return format;
        }

        public int getTrackCount() {
            return trackCount;
        }

        public int getTicksPerQuarter() {
            return ticksPerQuarter;
        }

        public int getNextOffset() {
            return nextOffset;
        }
    }

    public static final class MksSysexChunkTextParams {
        private final int ticksPerQuarter;
        private final int eventCount;
        private final int trackCount;
        private final int tempoEventCount;
        private final int timeSignatureEventCount;
        private final int keySignatureEventCount;
        private final int controlEventCount;
        private final int channelCount;
        private final List<String> diagnostics;

        public MksSysexChunkTextParams(int ticksPerQuarter, int eventCount, int trackCount, int tempoEventCount,
                int timeSignatureEventCount, int keySignatureEventCount, int controlEventCount, int channelCount,
                List<String> diagnostics) {
            this.ticksPerQuarter = Math.round(ticksPerQuarter);
            this.eventCount = Math.round(eventCount);
            this.trackCount = Math.round(trackCount);
            this.tempoEventCount = Math.round(tempoEventCount);
            this.timeSignatureEventCount = Math.round(timeSignatureEventCount);
            this.keySignatureEventCount = Math.round(keySignatureEventCount);
            this.controlEventCount = Math.round(controlEventCount);
            this.channelCount = Math.round(channelCount);
            this.diagnostics = diagnostics == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(diagnostics));
        }

        public int getTicksPerQuarter() {
            return ticksPerQuarter;
        }

        public int getEventCount() {
            return eventCount;
        }

        public int getTrackCount() {
            return trackCount;
        }

        public int getTempoEventCount() {
            return tempoEventCount;
        }

        public int getTimeSignatureEventCount() {
            return timeSignatureEventCount;
        }

        public int getKeySignatureEventCount() {
            return keySignatureEventCount;
        }

        public int getControlEventCount() {
            return controlEventCount;
        }

        public int getChannelCount() {
            return channelCount;
        }

        public List<String> getDiagnostics() {
            return diagnostics;
        }
    }

    public static final class RawTrackEvent {
        private final int tick;
        private final int order;
        private final int sortKey;
        private final byte[] bytes;

        public RawTrackEvent(int tick, int order, byte[] bytes) {
            this(tick, order, 0, bytes);
        }

        public RawTrackEvent(int tick, int order, int sortKey, byte[] bytes) {
            this.tick = Math.round(tick);
            this.order = Math.round(order);
            this.sortKey = Math.round(sortKey);
            this.bytes = bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
        }

        public int getTick() {
            return tick;
        }

        public int getOrder() {
            return order;
        }

        public int getSortKey() {
            return sortKey;
        }

        public byte[] getBytes() {
            return Arrays.copyOf(bytes, bytes.length);
        }
    }

    public static final class RawMidiTempoTrackOptions {
        private final boolean embedMksSysEx;
        private final List<String> sysexChunkTexts;
        private final List<String> textMetaLines;
        private final String metaTrackName;

        public RawMidiTempoTrackOptions(boolean embedMksSysEx, List<String> sysexChunkTexts,
                List<String> textMetaLines, String metaTrackName) {
            this.embedMksSysEx = embedMksSysEx;
            this.sysexChunkTexts = sysexChunkTexts == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(sysexChunkTexts));
            this.textMetaLines = textMetaLines == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(textMetaLines));
            this.metaTrackName = metaTrackName == null ? "" : metaTrackName;
        }

        public boolean isEmbedMksSysEx() {
            return embedMksSysEx;
        }

        public List<String> getSysexChunkTexts() {
            return sysexChunkTexts;
        }

        public List<String> getTextMetaLines() {
            return textMetaLines;
        }

        public String getMetaTrackName() {
            return metaTrackName;
        }
    }

    public static final class RawMidiPlaybackEvent {
        private final int midiNumber;
        private final int startTicks;
        private final int durTicks;
        private final int channel;
        private final int velocity;
        private final String trackId;
        private final String trackName;

        public RawMidiPlaybackEvent(int midiNumber, int startTicks, int durTicks, int channel, int velocity,
                String trackId, String trackName) {
            this.midiNumber = Math.round(midiNumber);
            this.startTicks = Math.round(startTicks);
            this.durTicks = Math.round(durTicks);
            this.channel = Math.round(channel);
            this.velocity = Math.round(velocity);
            this.trackId = trackId == null ? "" : trackId;
            this.trackName = trackName == null ? "" : trackName;
        }

        public int getMidiNumber() {
            return midiNumber;
        }

        public int getStartTicks() {
            return startTicks;
        }

        public int getDurTicks() {
            return durTicks;
        }

        public int getChannel() {
            return channel;
        }

        public int getVelocity() {
            return velocity;
        }

        public String getTrackId() {
            return trackId;
        }

        public String getTrackName() {
            return trackName;
        }
    }

    public static final class RawMidiControlEvent {
        private final String trackId;
        private final String trackName;
        private final int startTicks;
        private final int channel;
        private final int controllerNumber;
        private final int controllerValue;

        public RawMidiControlEvent(String trackId, String trackName, int startTicks, int channel,
                int controllerNumber, int controllerValue) {
            this.trackId = trackId == null ? "" : trackId;
            this.trackName = trackName == null ? "" : trackName;
            this.startTicks = Math.round(startTicks);
            this.channel = Math.round(channel);
            this.controllerNumber = Math.round(controllerNumber);
            this.controllerValue = Math.round(controllerValue);
        }

        public String getTrackId() {
            return trackId;
        }

        public String getTrackName() {
            return trackName;
        }

        public int getStartTicks() {
            return startTicks;
        }

        public int getChannel() {
            return channel;
        }

        public int getControllerNumber() {
            return controllerNumber;
        }

        public int getControllerValue() {
            return controllerValue;
        }
    }

    public static final class MidiImportDiagnostic {
        private final String code;
        private final String message;

        public MidiImportDiagnostic(String code, String message) {
            this.code = code == null ? "" : code;
            this.message = message == null ? "" : message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class MidiImportOptions {
        private final String quantizeGrid;
        private final Boolean debugMetadata;
        private final Boolean sourceMetadata;
        private final Boolean tripletAwareQuantize;
        private final String title;

        public MidiImportOptions() {
            this(null, null, null, null, null);
        }

        public MidiImportOptions(String quantizeGrid, Boolean debugMetadata, Boolean sourceMetadata,
                Boolean tripletAwareQuantize, String title) {
            this.quantizeGrid = quantizeGrid;
            this.debugMetadata = debugMetadata;
            this.sourceMetadata = sourceMetadata;
            this.tripletAwareQuantize = tripletAwareQuantize;
            this.title = title;
        }

        public String getQuantizeGrid() {
            return quantizeGrid;
        }

        public Boolean getDebugMetadata() {
            return debugMetadata;
        }

        public Boolean getSourceMetadata() {
            return sourceMetadata;
        }

        public Boolean getTripletAwareQuantize() {
            return tripletAwareQuantize;
        }

        public String getTitle() {
            return title;
        }
    }

    public static final class MidiImportResult {
        private final boolean ok;
        private final String xml;
        private final List<MidiImportDiagnostic> diagnostics;
        private final List<MidiImportDiagnostic> warnings;

        public MidiImportResult(boolean ok, String xml, List<MidiImportDiagnostic> diagnostics,
                List<MidiImportDiagnostic> warnings) {
            this.ok = ok;
            this.xml = xml == null ? "" : xml;
            this.diagnostics = diagnostics == null ? Collections.<MidiImportDiagnostic>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiImportDiagnostic>(diagnostics));
            this.warnings = warnings == null ? Collections.<MidiImportDiagnostic>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiImportDiagnostic>(warnings));
        }

        public boolean isOk() {
            return ok;
        }

        public String getXml() {
            return xml;
        }

        public List<MidiImportDiagnostic> getDiagnostics() {
            return diagnostics;
        }

        public List<MidiImportDiagnostic> getWarnings() {
            return warnings;
        }
    }

    public static final class MidiPlaybackEventsResult {
        private final int tempo;
        private final List<RawMidiPlaybackEvent> events;

        public MidiPlaybackEventsResult(int tempo, List<RawMidiPlaybackEvent> events) {
            this.tempo = clampTempo(tempo);
            this.events = events == null ? Collections.<RawMidiPlaybackEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<RawMidiPlaybackEvent>(events));
        }

        public int getTempo() {
            return tempo;
        }

        public List<RawMidiPlaybackEvent> getEvents() {
            return events;
        }
    }

    public static final class MidiPlaybackExtractionOptions {
        private final String mode;
        private final String graceTimingMode;
        private final boolean metricAccentEnabled;
        private final String metricAccentProfile;
        private final Boolean applyDefaultDetache;
        private final boolean includeGraceInPlaybackLikeMode;
        private final boolean includeOrnamentInPlaybackLikeMode;
        private final boolean includeTieInPlaybackLikeMode;

        public MidiPlaybackExtractionOptions() {
            this("playback");
        }

        public MidiPlaybackExtractionOptions(String mode) {
            this(mode, "before_beat");
        }

        public MidiPlaybackExtractionOptions(String mode, String graceTimingMode) {
            this(mode, graceTimingMode, false, "subtle");
        }

        public MidiPlaybackExtractionOptions(String mode, String graceTimingMode, boolean metricAccentEnabled,
                String metricAccentProfile) {
            this(mode, graceTimingMode, metricAccentEnabled, metricAccentProfile, null);
        }

        public MidiPlaybackExtractionOptions(String mode, String graceTimingMode, boolean metricAccentEnabled,
                String metricAccentProfile, Boolean applyDefaultDetache) {
            this(mode, graceTimingMode, metricAccentEnabled, metricAccentProfile, applyDefaultDetache, false, false,
                    false);
        }

        public MidiPlaybackExtractionOptions(String mode, String graceTimingMode, boolean metricAccentEnabled,
                String metricAccentProfile, Boolean applyDefaultDetache, boolean includeGraceInPlaybackLikeMode,
                boolean includeOrnamentInPlaybackLikeMode, boolean includeTieInPlaybackLikeMode) {
            this.mode = "midi".equals(mode) ? "midi" : "playback";
            this.graceTimingMode = normalizeGraceTimingMode(graceTimingMode);
            this.metricAccentEnabled = metricAccentEnabled;
            this.metricAccentProfile = normalizeMetricAccentProfile(metricAccentProfile);
            this.applyDefaultDetache = applyDefaultDetache;
            this.includeGraceInPlaybackLikeMode = includeGraceInPlaybackLikeMode;
            this.includeOrnamentInPlaybackLikeMode = includeOrnamentInPlaybackLikeMode;
            this.includeTieInPlaybackLikeMode = includeTieInPlaybackLikeMode;
        }

        public String getMode() {
            return mode;
        }

        public boolean isMidiMode() {
            return "midi".equals(mode);
        }

        public String getGraceTimingMode() {
            return graceTimingMode;
        }

        public boolean includeGraceProcessing() {
            return isMidiMode() || includeGraceInPlaybackLikeMode;
        }

        public boolean includeOrnamentExpansion() {
            return isMidiMode() || includeOrnamentInPlaybackLikeMode;
        }

        public boolean includeTieProcessing() {
            return isMidiMode() || includeTieInPlaybackLikeMode;
        }

        public boolean includeSlurProcessing() {
            return isMidiMode() || includeTieProcessing();
        }

        public boolean isMetricAccentEnabled() {
            return metricAccentEnabled;
        }

        public String getMetricAccentProfile() {
            return metricAccentProfile;
        }

        public boolean applyDefaultDetache() {
            return applyDefaultDetache == null ? isMidiMode() : applyDefaultDetache.booleanValue();
        }

        private static String normalizeGraceTimingMode(String value) {
            String normalized = value == null ? "" : value.trim();
            if ("on_beat".equals(normalized) || "classical_equal".equals(normalized)) {
                return normalized;
            }
            return "before_beat";
        }
    }

    private static final class PendingGracePlaybackNote {
        private final int midiNumber;
        private final int velocity;
        private final int weight;

        private PendingGracePlaybackNote(int midiNumber, int velocity, int weight) {
            this.midiNumber = midiNumber;
            this.velocity = clampVelocity(velocity);
            this.weight = Math.max(1, Math.round(weight));
        }

        private int getMidiNumber() {
            return midiNumber;
        }

        private int getVelocity() {
            return velocity;
        }

        private int getWeight() {
            return weight;
        }
    }

    private static final class NeighborPitch {
        private final String step;
        private final int octave;
        private final int alter;

        private NeighborPitch(String step, int octave, int alter) {
            this.step = step == null ? "" : step;
            this.octave = octave;
            this.alter = alter;
        }

        private String getStep() {
            return step;
        }

        private int getOctave() {
            return octave;
        }

        private int getAlter() {
            return alter;
        }
    }

    private static final class WedgeStart {
        private final String number;
        private final String kind;

        private WedgeStart(String number, String kind) {
            this.number = number == null || number.length() == 0 ? "1" : number;
            this.kind = "diminuendo".equals(kind) ? "diminuendo" : "crescendo";
        }

        private String getNumber() {
            return number;
        }

        private String getKind() {
            return kind;
        }
    }

    private static final class WedgeDirective {
        private final List<WedgeStart> starts;
        private final Set<String> stops;

        private WedgeDirective(List<WedgeStart> starts, Set<String> stops) {
            this.starts = Collections.unmodifiableList(new ArrayList<WedgeStart>(starts == null
                    ? Collections.<WedgeStart>emptyList()
                    : starts));
            this.stops = Collections.unmodifiableSet(new LinkedHashSet<String>(stops == null
                    ? Collections.<String>emptySet()
                    : stops));
        }

        private List<WedgeStart> getStarts() {
            return starts;
        }

        private Set<String> getStops() {
            return stops;
        }
    }

    private static final class NoteArticulationAdjustments {
        private static final NoteArticulationAdjustments NONE =
                new NoteArticulationAdjustments(0, 1.0d, false);

        private final int velocityDelta;
        private final double durationRatio;
        private final boolean hasTenuto;

        private NoteArticulationAdjustments(int velocityDelta, double durationRatio, boolean hasTenuto) {
            this.velocityDelta = velocityDelta;
            this.durationRatio = Double.isNaN(durationRatio) || Double.isInfinite(durationRatio) || durationRatio <= 0
                    ? 1.0d
                    : durationRatio;
            this.hasTenuto = hasTenuto;
        }

        private int getVelocityDelta() {
            return velocityDelta;
        }

        private double getDurationRatio() {
            return durationRatio;
        }

        private boolean hasTenuto() {
            return hasTenuto;
        }
    }

    private static final class TemporalExpressionAdjustments {
        private static final TemporalExpressionAdjustments NONE = new TemporalExpressionAdjustments(0, 0);

        private final int durationExtraTicks;
        private final int postPauseTicks;

        private TemporalExpressionAdjustments(int durationExtraTicks, int postPauseTicks) {
            this.durationExtraTicks = Math.max(0, Math.round(durationExtraTicks));
            this.postPauseTicks = Math.max(0, Math.round(postPauseTicks));
        }

        private int getDurationExtraTicks() {
            return durationExtraTicks;
        }

        private int getPostPauseTicks() {
            return postPauseTicks;
        }
    }

    private static final class TieFlags {
        private static final TieFlags NONE = new TieFlags(false, false);

        private final boolean start;
        private final boolean stop;

        private TieFlags(boolean start, boolean stop) {
            this.start = start;
            this.stop = stop;
        }

        private boolean isStart() {
            return start;
        }

        private boolean isStop() {
            return stop;
        }
    }

    private static final class SlurNumbers {
        private static final SlurNumbers NONE = new SlurNumbers(Collections.<String>emptyList(),
                Collections.<String>emptyList());

        private final List<String> starts;
        private final List<String> stops;

        private SlurNumbers(List<String> starts, List<String> stops) {
            this.starts = Collections.unmodifiableList(new ArrayList<String>(starts == null
                    ? Collections.<String>emptyList()
                    : starts));
            this.stops = Collections.unmodifiableList(new ArrayList<String>(stops == null
                    ? Collections.<String>emptyList()
                    : stops));
        }

        private List<String> getStarts() {
            return starts;
        }

        private List<String> getStops() {
            return stops;
        }
    }

    public static final class SmfHeaderParseResult {
        private final ParsedSmfHeader header;
        private final List<MidiImportDiagnostic> diagnostics;

        public SmfHeaderParseResult(ParsedSmfHeader header, List<MidiImportDiagnostic> diagnostics) {
            this.header = header;
            this.diagnostics = diagnostics == null ? Collections.<MidiImportDiagnostic>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiImportDiagnostic>(diagnostics));
        }

        public ParsedSmfHeader getHeader() {
            return header;
        }

        public List<MidiImportDiagnostic> getDiagnostics() {
            return diagnostics;
        }
    }

    public static final class SmfParseSummary {
        private final List<SmfImportedNote> notes;
        private final Set<Integer> channels;
        private final String trackName;
        private final List<String> standardTitleCandidates;
        private final List<String> standardComposerCandidates;
        private final Map<String, Integer> programByTrackChannel;
        private final List<MidiControllerEvent> controllerEvents;
        private final List<MidiTickTimeSignatureEvent> timeSignatureEvents;
        private final List<MidiTickKeySignatureEvent> keySignatureEvents;
        private final List<MidiTempoEvent> tempoEvents;
        private final List<String> mksSysExPayloads;
        private final List<String> mksTextMetaLines;
        private final List<MidiImportDiagnostic> parseWarnings;

        public SmfParseSummary(List<SmfImportedNote> notes, Set<Integer> channels, String trackName,
                List<String> standardTitleCandidates, List<String> standardComposerCandidates,
                Map<String, Integer> programByTrackChannel, List<MidiControllerEvent> controllerEvents,
                List<MidiTickTimeSignatureEvent> timeSignatureEvents,
                List<MidiTickKeySignatureEvent> keySignatureEvents, List<MidiTempoEvent> tempoEvents,
                List<String> mksSysExPayloads, List<String> mksTextMetaLines,
                List<MidiImportDiagnostic> parseWarnings) {
            this.notes = notes == null ? Collections.<SmfImportedNote>emptyList()
                    : Collections.unmodifiableList(new ArrayList<SmfImportedNote>(notes));
            this.channels = channels == null ? Collections.<Integer>emptySet()
                    : Collections.unmodifiableSet(new LinkedHashSet<Integer>(channels));
            this.trackName = trackName;
            this.standardTitleCandidates = standardTitleCandidates == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(standardTitleCandidates));
            this.standardComposerCandidates = standardComposerCandidates == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(standardComposerCandidates));
            this.programByTrackChannel = programByTrackChannel == null ? Collections.<String, Integer>emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(programByTrackChannel));
            this.controllerEvents = controllerEvents == null ? Collections.<MidiControllerEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiControllerEvent>(controllerEvents));
            this.timeSignatureEvents = timeSignatureEvents == null ? Collections.<MidiTickTimeSignatureEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiTickTimeSignatureEvent>(timeSignatureEvents));
            this.keySignatureEvents = keySignatureEvents == null ? Collections.<MidiTickKeySignatureEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiTickKeySignatureEvent>(keySignatureEvents));
            this.tempoEvents = tempoEvents == null ? Collections.<MidiTempoEvent>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiTempoEvent>(tempoEvents));
            this.mksSysExPayloads = mksSysExPayloads == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(mksSysExPayloads));
            this.mksTextMetaLines = mksTextMetaLines == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(mksTextMetaLines));
            this.parseWarnings = parseWarnings == null ? Collections.<MidiImportDiagnostic>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiImportDiagnostic>(parseWarnings));
        }

        public List<SmfImportedNote> getNotes() {
            return notes;
        }

        public Set<Integer> getChannels() {
            return channels;
        }

        public String getTrackName() {
            return trackName;
        }

        public List<String> getStandardTitleCandidates() {
            return standardTitleCandidates;
        }

        public List<String> getStandardComposerCandidates() {
            return standardComposerCandidates;
        }

        public Map<String, Integer> getProgramByTrackChannel() {
            return programByTrackChannel;
        }

        public List<MidiControllerEvent> getControllerEvents() {
            return controllerEvents;
        }

        public List<MidiTickTimeSignatureEvent> getTimeSignatureEvents() {
            return timeSignatureEvents;
        }

        public List<MidiTickKeySignatureEvent> getKeySignatureEvents() {
            return keySignatureEvents;
        }

        public List<MidiTempoEvent> getTempoEvents() {
            return tempoEvents;
        }

        public List<String> getMksSysExPayloads() {
            return mksSysExPayloads;
        }

        public List<String> getMksTextMetaLines() {
            return mksTextMetaLines;
        }

        public List<MidiImportDiagnostic> getParseWarnings() {
            return parseWarnings;
        }
    }

    public static final class ImportedVoiceCluster {
        private final int voice;
        private final int startTick;
        private final int endTick;
        private final List<ImportedQuantizedNote> notes;

        public ImportedVoiceCluster(int voice, int startTick, int endTick, List<ImportedQuantizedNote> notes) {
            this.voice = Math.max(1, Math.round(voice));
            this.startTick = Math.max(0, Math.round(startTick));
            this.endTick = Math.max(this.startTick, Math.round(endTick));
            this.notes = notes == null ? Collections.<ImportedQuantizedNote>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ImportedQuantizedNote>(notes));
        }

        public int getVoice() {
            return voice;
        }

        public int getStartTick() {
            return startTick;
        }

        public int getEndTick() {
            return endTick;
        }

        public List<ImportedQuantizedNote> getNotes() {
            return notes;
        }
    }

    public static final class SplitClustersToMeasureSegmentsParams {
        private final List<ImportedVoiceCluster> clusters;
        private final int ticksPerQuarter;
        private final int divisions;
        private final int measureTicks;
        private final int pickupTicks;
        private final boolean drum;
        private final boolean useGrandStaff;

        public SplitClustersToMeasureSegmentsParams(List<ImportedVoiceCluster> clusters, int ticksPerQuarter,
                int divisions, int measureTicks, int pickupTicks, boolean drum, boolean useGrandStaff) {
            this.clusters = clusters == null ? Collections.<ImportedVoiceCluster>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ImportedVoiceCluster>(clusters));
            this.ticksPerQuarter = Math.max(1, Math.round(ticksPerQuarter));
            this.divisions = Math.max(1, Math.round(divisions));
            this.measureTicks = Math.max(1, Math.round(measureTicks));
            this.pickupTicks = Math.max(0, Math.round(pickupTicks));
            this.drum = drum;
            this.useGrandStaff = useGrandStaff;
        }

        public List<ImportedVoiceCluster> getClusters() {
            return clusters;
        }

        public int getTicksPerQuarter() {
            return ticksPerQuarter;
        }

        public int getDivisions() {
            return divisions;
        }

        public int getMeasureTicks() {
            return measureTicks;
        }

        public int getPickupTicks() {
            return pickupTicks;
        }

        public boolean isDrum() {
            return drum;
        }

        public boolean isUseGrandStaff() {
            return useGrandStaff;
        }
    }

    public static final class ImportedVoiceNoteSegment {
        private final int measureIndex;
        private final int voice;
        private final int staff;
        private final int startDiv;
        private final int durDiv;
        private final int midi;
        private final int velocity;
        private final int trackIndex;
        private final int channel;
        private final int startTick;
        private final int endTick;

        public ImportedVoiceNoteSegment(int measureIndex, int voice, int staff, int startDiv, int durDiv, int midi,
                int velocity, int trackIndex, int channel, int startTick, int endTick) {
            this.measureIndex = Math.max(0, Math.round(measureIndex));
            this.voice = Math.max(1, Math.round(voice));
            this.staff = Math.max(1, Math.min(2, Math.round(staff)));
            this.startDiv = Math.max(0, Math.round(startDiv));
            this.durDiv = Math.max(1, Math.round(durDiv));
            this.midi = Math.max(0, Math.min(127, Math.round(midi)));
            this.velocity = clampVelocity(velocity);
            this.trackIndex = Math.max(0, Math.round(trackIndex));
            this.channel = Math.max(1, Math.round(channel));
            this.startTick = Math.max(0, Math.round(startTick));
            this.endTick = Math.max(this.startTick, Math.round(endTick));
        }

        public int getMeasureIndex() {
            return measureIndex;
        }

        public int getVoice() {
            return voice;
        }

        public int getStaff() {
            return staff;
        }

        public int getStartDiv() {
            return startDiv;
        }

        public int getDurDiv() {
            return durDiv;
        }

        public int getMidi() {
            return midi;
        }

        public int getVelocity() {
            return velocity;
        }

        public int getTrackIndex() {
            return trackIndex;
        }

        public int getChannel() {
            return channel;
        }

        public int getStartTick() {
            return startTick;
        }

        public int getEndTick() {
            return endTick;
        }
    }

    public static final class DurationNotation {
        private final String type;
        private final int dots;
        private final double q;
        private final double durDiv;

        public DurationNotation(String type, int dots, double q, double durDiv) {
            this.type = type == null ? "" : type;
            this.dots = Math.max(0, Math.min(2, Math.round(dots)));
            this.q = q;
            this.durDiv = durDiv;
        }

        public String getType() {
            return type;
        }

        public int getDots() {
            return dots;
        }

        public double getQ() {
            return q;
        }

        public double getDurDiv() {
            return durDiv;
        }
    }

    public static final class QuantizedImportedNotesResult {
        private final List<ImportedQuantizedNote> notes;
        private final List<MidiImportDiagnostic> warnings;
        private final int qTick;
        private final int divisions;

        public QuantizedImportedNotesResult(List<ImportedQuantizedNote> notes, List<MidiImportDiagnostic> warnings,
                int qTick, int divisions) {
            this.notes = notes == null ? Collections.<ImportedQuantizedNote>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ImportedQuantizedNote>(notes));
            this.warnings = warnings == null ? Collections.<MidiImportDiagnostic>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MidiImportDiagnostic>(warnings));
            this.qTick = Math.max(1, Math.round(qTick));
            this.divisions = Math.max(1, Math.round(divisions));
        }

        public List<ImportedQuantizedNote> getNotes() {
            return notes;
        }

        public List<MidiImportDiagnostic> getWarnings() {
            return warnings;
        }

        public int getQTick() {
            return qTick;
        }

        public int getDivisions() {
            return divisions;
        }
    }

    public static final class MidiTrackChannelGroup {
        private final int trackIndex;
        private final int channel;

        public MidiTrackChannelGroup(int trackIndex, int channel) {
            this.trackIndex = Math.max(0, Math.round(trackIndex));
            this.channel = Math.max(1, Math.round(channel));
        }

        public int getTrackIndex() {
            return trackIndex;
        }

        public int getChannel() {
            return channel;
        }
    }

    public static final class MidiPartDef {
        private final String partId;
        private final String name;
        private final int channel;
        private final int program;
        private final String key;

        public MidiPartDef(String partId, String name, int channel, int program, String key) {
            this.partId = partId == null ? "" : partId;
            this.name = name == null ? "" : name;
            this.channel = Math.max(1, Math.round(channel));
            Integer normalizedProgram = normalizeMidiProgramNumber(program);
            this.program = normalizedProgram == null ? 1 : normalizedProgram.intValue();
            this.key = key == null ? "" : key;
        }

        public String getPartId() {
            return partId;
        }

        public String getName() {
            return name;
        }

        public int getChannel() {
            return channel;
        }

        public int getProgram() {
            return program;
        }

        public String getKey() {
            return key;
        }
    }

    public static final class MidiPartLaneDef {
        private final int sourceStaff;
        private final int voice;
        private final int outputStaff;

        public MidiPartLaneDef(int sourceStaff, int voice, int outputStaff) {
            this.sourceStaff = Math.max(1, Math.min(2, Math.round(sourceStaff)));
            this.voice = Math.max(1, Math.round(voice));
            this.outputStaff = Math.max(1, Math.round(outputStaff));
        }

        public int getSourceStaff() {
            return sourceStaff;
        }

        public int getVoice() {
            return voice;
        }

        public int getOutputStaff() {
            return outputStaff;
        }
    }

    public static final class MidiDynamicDirectionsResult {
        private final String xml;
        private final String previousDynamicMark;

        public MidiDynamicDirectionsResult(String xml, String previousDynamicMark) {
            this.xml = xml == null ? "" : xml;
            this.previousDynamicMark = previousDynamicMark;
        }

        public String getXml() {
            return xml;
        }

        public String getPreviousDynamicMark() {
            return previousDynamicMark;
        }
    }

    public static final class MidiPartMeasureLayout {
        private final int measureTicks;
        private final int measureDiv;
        private final int pickupMeasureTicks;
        private final int pickupMeasureDiv;
        private final int measureCount;

        public MidiPartMeasureLayout(int measureTicks, int measureDiv, int pickupMeasureTicks, int pickupMeasureDiv,
                int measureCount) {
            this.measureTicks = Math.max(1, Math.round(measureTicks));
            this.measureDiv = Math.max(1, Math.round(measureDiv));
            this.pickupMeasureTicks = Math.max(0, Math.round(pickupMeasureTicks));
            this.pickupMeasureDiv = Math.max(0, Math.round(pickupMeasureDiv));
            this.measureCount = Math.max(1, Math.round(measureCount));
        }

        public int getMeasureTicks() {
            return measureTicks;
        }

        public int getMeasureDiv() {
            return measureDiv;
        }

        public int getPickupMeasureTicks() {
            return pickupMeasureTicks;
        }

        public int getPickupMeasureDiv() {
            return pickupMeasureDiv;
        }

        public int getMeasureCount() {
            return measureCount;
        }
    }

    public static final class MidiPartSegmentLayout {
        private final List<ImportedVoiceNoteSegment> splitSegments;
        private final Map<Integer, List<ImportedVoiceNoteSegment>> voiceSegmentsByMeasure;
        private final boolean useGrandStaff;

        public MidiPartSegmentLayout(List<ImportedVoiceNoteSegment> splitSegments,
                Map<Integer, List<ImportedVoiceNoteSegment>> voiceSegmentsByMeasure, boolean useGrandStaff) {
            this.splitSegments = splitSegments == null ? Collections.<ImportedVoiceNoteSegment>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ImportedVoiceNoteSegment>(splitSegments));
            Map<Integer, List<ImportedVoiceNoteSegment>> byMeasure =
                    new LinkedHashMap<Integer, List<ImportedVoiceNoteSegment>>();
            if (voiceSegmentsByMeasure != null) {
                for (Map.Entry<Integer, List<ImportedVoiceNoteSegment>> entry : voiceSegmentsByMeasure.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    byMeasure.put(entry.getKey(), entry.getValue() == null
                            ? Collections.<ImportedVoiceNoteSegment>emptyList()
                            : Collections.unmodifiableList(new ArrayList<ImportedVoiceNoteSegment>(
                                    entry.getValue())));
                }
            }
            this.voiceSegmentsByMeasure = Collections.unmodifiableMap(byMeasure);
            this.useGrandStaff = useGrandStaff;
        }

        public List<ImportedVoiceNoteSegment> getSplitSegments() {
            return splitSegments;
        }

        public Map<Integer, List<ImportedVoiceNoteSegment>> getVoiceSegmentsByMeasure() {
            return voiceSegmentsByMeasure;
        }

        public boolean isUseGrandStaff() {
            return useGrandStaff;
        }
    }

    public static final class MidiMeasureOffsetDiv {
        private final int measureIndex;
        private final int offsetDiv;

        public MidiMeasureOffsetDiv(int measureIndex, int offsetDiv) {
            this.measureIndex = Math.max(0, Math.round(measureIndex));
            this.offsetDiv = Math.max(0, Math.round(offsetDiv));
        }

        public int getMeasureIndex() {
            return measureIndex;
        }

        public int getOffsetDiv() {
            return offsetDiv;
        }
    }

    public static final class MidiTempoMeasureEvent {
        private final int offsetDiv;
        private final int bpm;

        public MidiTempoMeasureEvent(int offsetDiv, int bpm) {
            this.offsetDiv = Math.max(0, Math.round(offsetDiv));
            this.bpm = clampTempo(bpm);
        }

        public int getOffsetDiv() {
            return offsetDiv;
        }

        public int getBpm() {
            return bpm;
        }
    }

    public static final class DrumPartMap {
        private final Map<String, Integer> midiUnpitchedByInstrumentId;
        private final Map<String, String> instrumentNameById;
        private final Integer defaultMidiUnpitched;

        public DrumPartMap(Map<String, Integer> midiUnpitchedByInstrumentId, Map<String, String> instrumentNameById,
                Integer defaultMidiUnpitched) {
            this.midiUnpitchedByInstrumentId = midiUnpitchedByInstrumentId == null
                    ? Collections.<String, Integer>emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(midiUnpitchedByInstrumentId));
            this.instrumentNameById = instrumentNameById == null ? Collections.<String, String>emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<String, String>(instrumentNameById));
            this.defaultMidiUnpitched = defaultMidiUnpitched;
        }

        public Map<String, Integer> getMidiUnpitchedByInstrumentId() {
            return midiUnpitchedByInstrumentId;
        }

        public Map<String, String> getInstrumentNameById() {
            return instrumentNameById;
        }

        public Integer getDefaultMidiUnpitched() {
            return defaultMidiUnpitched;
        }
    }

    private static final class GroupAtStart {
        private final int startDiv;
        private final int sourceDurDiv;
        private final boolean inferredStaccato;
        private final List<ImportedVoiceNoteSegment> group;

        private GroupAtStart(int startDiv, int sourceDurDiv, boolean inferredStaccato,
                List<ImportedVoiceNoteSegment> group) {
            this.startDiv = Math.max(0, startDiv);
            this.sourceDurDiv = Math.max(1, sourceDurDiv);
            this.inferredStaccato = inferredStaccato;
            this.group = group == null ? Collections.<ImportedVoiceNoteSegment>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ImportedVoiceNoteSegment>(group));
        }

        private int getStartDiv() {
            return startDiv;
        }

        private int getSourceDurDiv() {
            return sourceDurDiv;
        }

        private boolean isInferredStaccato() {
            return inferredStaccato;
        }

        private List<ImportedVoiceNoteSegment> getGroup() {
            return group;
        }
    }

    private static final class PreparedNoteChunk {
        private final int startDiv;
        private final int durDiv;
        private final String typeXml;
        private final boolean tieStart;
        private final boolean tieStop;
        private final boolean inferredStaccato;
        private final List<ImportedVoiceNoteSegment> group;

        private PreparedNoteChunk(int startDiv, int durDiv, String typeXml, boolean tieStart, boolean tieStop,
                boolean inferredStaccato, List<ImportedVoiceNoteSegment> group) {
            this.startDiv = Math.max(0, startDiv);
            this.durDiv = Math.max(1, durDiv);
            this.typeXml = typeXml == null ? "" : typeXml;
            this.tieStart = tieStart;
            this.tieStop = tieStop;
            this.inferredStaccato = inferredStaccato;
            this.group = group == null ? Collections.<ImportedVoiceNoteSegment>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ImportedVoiceNoteSegment>(group));
        }

        private int getStartDiv() {
            return startDiv;
        }

        private int getDurDiv() {
            return durDiv;
        }

        private String getTypeXml() {
            return typeXml;
        }

        private boolean isTieStart() {
            return tieStart;
        }

        private boolean isTieStop() {
            return tieStop;
        }

        private boolean isInferredStaccato() {
            return inferredStaccato;
        }

        private List<ImportedVoiceNoteSegment> getGroup() {
            return group;
        }
    }

    private static final class BeamTimelineEvent {
        private final String kind;
        private final int durDiv;
        private final int levels;
        @SuppressWarnings("unused")
        private final Integer chunkIndex;

        private BeamTimelineEvent(String kind, int durDiv, int levels, Integer chunkIndex) {
            this.kind = kind == null ? "" : kind;
            this.durDiv = Math.max(0, durDiv);
            this.levels = Math.max(0, levels);
            this.chunkIndex = chunkIndex;
        }

        private String getKind() {
            return kind;
        }

        private int getDurDiv() {
            return durDiv;
        }

        private int getLevels() {
            return levels;
        }
    }

    private static final class BeamAssignment {
        private final String state;
        private final int levels;

        private BeamAssignment(String state, int levels) {
            this.state = state == null ? "" : state;
            this.levels = Math.max(0, levels);
        }

        private String getState() {
            return state;
        }

        private int getLevels() {
            return levels;
        }
    }

    private static final class MidiPitchComponents {
        private final String step;
        private final int alter;
        private final int octave;

        private MidiPitchComponents(String step, int alter, int octave) {
            this.step = step == null ? "C" : step;
            this.alter = alter;
            this.octave = octave;
        }

        private String getStep() {
            return step;
        }

        private int getAlter() {
            return alter;
        }

        private int getOctave() {
            return octave;
        }
    }

    private static final class MidiDrumDisplay {
        private final String step;
        private final int octave;

        private MidiDrumDisplay(String step, int octave) {
            this.step = step == null ? "C" : step;
            this.octave = octave;
        }

        private String getStep() {
            return step;
        }

        private int getOctave() {
            return octave;
        }
    }
}
