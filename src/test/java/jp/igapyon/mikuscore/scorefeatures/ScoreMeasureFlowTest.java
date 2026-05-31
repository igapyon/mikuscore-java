/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuscore.scorefeatures.ScoreMeasureFlow.FlowInput;

public class ScoreMeasureFlowTest {
    @Test
    public void buildsMusicXmlBackupControls() {
        assertEquals("<backup><duration>960</duration></backup>",
                ScoreMeasureFlow.buildMusicXmlBackupXml(new FlowInput(Double.valueOf(960.2))));
        assertEquals("", ScoreMeasureFlow.buildMusicXmlBackupXml(new FlowInput(Integer.valueOf(0))));
    }

    @Test
    public void buildsMusicXmlForwardControls() {
        assertEquals("<forward><duration>120</duration><voice>2</voice><staff>1</staff></forward>",
                ScoreMeasureFlow
                        .buildMusicXmlForwardXml(new FlowInput(Integer.valueOf(120), Integer.valueOf(2), Integer.valueOf(1))));
    }
}
