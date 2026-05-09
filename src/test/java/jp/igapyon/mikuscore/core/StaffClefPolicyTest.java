package jp.igapyon.mikuscore.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class StaffClefPolicyTest {
    @Test
    public void detectsGrandStaffRange() {
        assertEquals(false, StaffClefPolicy.shouldUseGrandStaffByRange(Collections.<Integer>emptyList()));
        assertEquals(false,
                StaffClefPolicy.shouldUseGrandStaffByRange(Arrays.asList(Integer.valueOf(60), Integer.valueOf(67))));
        assertEquals(true,
                StaffClefPolicy.shouldUseGrandStaffByRange(Arrays.asList(Integer.valueOf(55), Integer.valueOf(64))));
        assertEquals(true,
                StaffClefPolicy.shouldUseGrandStaffByRange(Arrays.asList(Integer.valueOf(48), Integer.valueOf(72))));
    }

    @Test
    public void choosesSingleClefByMinimumAndMedianKeys() {
        assertEquals("G", StaffClefPolicy.chooseSingleClefByKeys(Collections.<Integer>emptyList()));
        assertEquals("G", StaffClefPolicy.chooseSingleClefByKeys(Arrays.asList(Integer.valueOf(60), Integer.valueOf(64))));
        assertEquals("F", StaffClefPolicy.chooseSingleClefByKeys(Arrays.asList(Integer.valueOf(48), Integer.valueOf(52))));
        assertEquals("G",
                StaffClefPolicy.chooseSingleClefByKeys(Arrays.asList(Integer.valueOf(48), Integer.valueOf(60),
                        Integer.valueOf(72))));
    }

    @Test
    public void picksStaffByPitchWithHysteresis() {
        assertEquals(1, StaffClefPolicy.pickStaffByPitchWithHysteresis(60, null));
        assertEquals(2, StaffClefPolicy.pickStaffByPitchWithHysteresis(59, null));
        assertEquals(1, StaffClefPolicy.pickStaffByPitchWithHysteresis(55, Integer.valueOf(1)));
        assertEquals(2, StaffClefPolicy.pickStaffByPitchWithHysteresis(54, Integer.valueOf(1)));
        assertEquals(2, StaffClefPolicy.pickStaffByPitchWithHysteresis(64, Integer.valueOf(2)));
        assertEquals(1, StaffClefPolicy.pickStaffByPitchWithHysteresis(65, Integer.valueOf(2)));
    }

    @Test
    public void picksStaffForClusterWithHysteresis() {
        assertEquals(1, StaffClefPolicy.pickStaffForClusterWithHysteresis(48, 60, null));
        assertEquals(2, StaffClefPolicy.pickStaffForClusterWithHysteresis(48, 59, null));
        assertEquals(1, StaffClefPolicy.pickStaffForClusterWithHysteresis(48, 55, Integer.valueOf(1)));
        assertEquals(2, StaffClefPolicy.pickStaffForClusterWithHysteresis(48, 54, Integer.valueOf(1)));
        assertEquals(2, StaffClefPolicy.pickStaffForClusterWithHysteresis(64, 72, Integer.valueOf(2)));
        assertEquals(1, StaffClefPolicy.pickStaffForClusterWithHysteresis(65, 72, Integer.valueOf(2)));
    }
}
