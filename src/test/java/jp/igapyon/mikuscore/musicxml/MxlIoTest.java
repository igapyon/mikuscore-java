/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MxlIoTest {
    @Test
    public void makesAndExtractsMxlBytes() {
        String xml = sampleMusicXml("MXL Roundtrip");

        byte[] archive = MxlIo.makeMxlBytes(xml);
        String extracted = MxlIo.extractMusicXmlTextFromMxl(archive);

        assertEquals(xml, extracted);
    }

    @Test
    public void makesDeterministicMxlBytes() {
        String xml = sampleMusicXml("Deterministic");

        assertArrayEquals(MxlIo.makeMxlBytes(xml), MxlIo.makeMxlBytes(xml));
    }

    @Test
    public void makesAndExtractsMsczBytes() {
        String mscx = "<museScore version=\"4.0\"><Score/></museScore>";

        byte[] archive = MxlIo.makeMsczBytes(mscx);
        String extracted = MxlIo.extractTextFromZipByExtensions(archive, new String[] { ".mscx" });

        assertEquals(mscx, extracted);
    }

    @Test
    public void extractsMusicXmlFromContainerRootFilePath() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("META-INF/container.xml", ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<container><rootfiles><rootfile full-path=\"scores/main.xml\"/></rootfiles></container>")
                .getBytes(StandardCharsets.UTF_8));
        entries.put("fallback.musicxml", sampleMusicXml("Fallback").getBytes(StandardCharsets.UTF_8));
        entries.put("scores/main.xml", sampleMusicXml("Main").getBytes(StandardCharsets.UTF_8));

        String extracted = MxlIo.extractMusicXmlTextFromMxl(MxlIo.makeZipBytes(entries));

        assertTrue(extracted.contains("<work-title>Main</work-title>"));
    }

    @Test
    public void extractsFallbackMusicXmlWhenContainerIsMissing() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.musicxml", sampleMusicXml("Fallback MusicXML").getBytes(StandardCharsets.UTF_8));

        String extracted = MxlIo.extractMusicXmlTextFromMxl(MxlIo.makeZipBytes(entries));

        assertTrue(extracted.contains("<work-title>Fallback MusicXML</work-title>"));
    }

    @Test
    public void extractsTextFromZipByExtensions() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("readme.txt", "Readme".getBytes(StandardCharsets.UTF_8));
        entries.put("score.abc", "X:1\nT:ABC\nK:C\nC|\n".getBytes(StandardCharsets.UTF_8));

        String extracted = MxlIo.extractTextFromZipByExtensions(MxlIo.makeZipBytes(entries), new String[] { ".abc" });

        assertTrue(extracted.contains("T:ABC"));
    }

    @Test
    public void listsRootEntryPathsByExtensionsOnlyAtRoot() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.mscx", "<museScore/>".getBytes(StandardCharsets.UTF_8));
        entries.put("nested/ignored.mscx", "<museScore/>".getBytes(StandardCharsets.UTF_8));

        List<String> paths = MxlIo.listZipRootEntryPathsByExtensions(MxlIo.makeZipBytes(entries),
                new String[] { ".mscx" });

        assertEquals(1, paths.size());
        assertEquals("score.mscx", paths.get(0));
    }

    private static String sampleMusicXml(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"4.0\">\n"
                + "  <work><work-title>" + title + "</work-title></work>\n"
                + "  <part-list><score-part id=\"P1\"><part-name>Music</part-name></score-part></part-list>\n"
                + "  <part id=\"P1\"><measure number=\"1\"><note><rest/><duration>1</duration></note></measure></part>\n"
                + "</score-partwise>\n";
    }
}
