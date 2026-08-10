package jp.igapyon.mikuscore.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StaffClefPolicy {
    public static final int UPPER_STAFF_HOLD_MIN = 55;
    public static final int LOWER_STAFF_HOLD_MAX = 64;
    public static final int STAFF_SPLIT_C4 = 60;
    public static final int STAFF_SPLIT_B3 = 59;

    private StaffClefPolicy() {
    }

    public static boolean shouldUseGrandStaffByRange(List<Integer> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        Integer first = firstNonNull(keys);
        if (first == null) {
            return false;
        }
        int minKey = first.intValue();
        int maxKey = first.intValue();
        for (Integer key : keys) {
            if (key == null) {
                continue;
            }
            minKey = Math.min(minKey, key.intValue());
            maxKey = Math.max(maxKey, key.intValue());
        }
        return minKey <= UPPER_STAFF_HOLD_MIN && maxKey >= LOWER_STAFF_HOLD_MAX;
    }

    public static String chooseSingleClefByKeys(List<Integer> keys) {
        if (keys == null || keys.isEmpty()) {
            return "G";
        }
        List<Integer> sorted = new ArrayList<Integer>();
        for (Integer key : keys) {
            if (key != null) {
                sorted.add(key);
            }
        }
        if (sorted.isEmpty()) {
            return "G";
        }
        Collections.sort(sorted);
        int minKey = sorted.get(0).intValue();
        if (minKey >= UPPER_STAFF_HOLD_MIN) {
            return "G";
        }
        int median = sorted.get(sorted.size() / 2).intValue();
        return median < STAFF_SPLIT_C4 ? "F" : "G";
    }

    public static int pickStaffByPitchWithHysteresis(int midiKey, Integer previousStaff) {
        return pickStaffByPitchWithHysteresis((double) midiKey, previousStaff);
    }

    /** Mirrors the Node number-based staff threshold comparison without an int bound. */
    public static int pickStaffByPitchWithHysteresis(double midiKey, Integer previousStaff) {
        if (previousStaff != null && previousStaff.intValue() == 1) {
            return midiKey >= UPPER_STAFF_HOLD_MIN ? 1 : 2;
        }
        if (previousStaff != null && previousStaff.intValue() == 2) {
            return midiKey <= LOWER_STAFF_HOLD_MAX ? 2 : 1;
        }
        return midiKey >= STAFF_SPLIT_C4 ? 1 : 2;
    }

    public static int pickStaffForClusterWithHysteresis(int minClusterKey, int maxClusterKey, Integer previousStaff) {
        if (previousStaff != null && previousStaff.intValue() == 1) {
            return maxClusterKey >= UPPER_STAFF_HOLD_MIN ? 1 : 2;
        }
        if (previousStaff != null && previousStaff.intValue() == 2) {
            return minClusterKey <= LOWER_STAFF_HOLD_MAX ? 2 : 1;
        }
        return maxClusterKey >= STAFF_SPLIT_C4 ? 1 : 2;
    }

    private static Integer firstNonNull(List<Integer> keys) {
        for (Integer key : keys) {
            if (key != null) {
                return key;
            }
        }
        return null;
    }
}
