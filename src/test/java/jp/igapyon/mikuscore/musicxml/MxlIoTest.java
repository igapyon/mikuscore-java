/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    public void supportsStoredZipModeForContainerFixtures() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.musicxml", "<score-partwise/>".getBytes(StandardCharsets.UTF_8));

        byte[] archive = MxlIo.makeZipBytes(entries, false);
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8);
        ZipEntry entry = zip.getNextEntry();
        try {
            assertEquals(ZipEntry.STORED, entry.getMethod());
            assertEquals("<score-partwise/>", MxlIo.extractMusicXmlTextFromMxl(archive));
        } finally {
            zip.close();
        }
    }

    @Test
    public void preferredCompressionUsesDeflateOnlyWhenItReducesTheEntryPayload() throws Exception {
        byte[] incompressible = new byte[1024];
        new Random(123456789L).nextBytes(incompressible);
        byte[] repetitive = new byte[1024];
        Arrays.fill(repetitive, (byte) 'A');
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("random.bin", incompressible);
        entries.put("repeated.txt", repetitive);

        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(MxlIo.makeZipBytes(entries, true)),
                StandardCharsets.UTF_8);
        Map<String, Integer> methods = new HashMap<String, Integer>();
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                methods.put(entry.getName(), Integer.valueOf(entry.getMethod()));
                zip.closeEntry();
            }
        } finally {
            zip.close();
        }

        assertEquals(Integer.valueOf(ZipEntry.STORED), methods.get("random.bin"));
        assertEquals(Integer.valueOf(ZipEntry.DEFLATED), methods.get("repeated.txt"));
    }

    @Test
    public void writesUpstreamZipHeaderLayoutWithoutExtraFieldsOrDataDescriptors() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.txt", "Zip layout".getBytes(StandardCharsets.UTF_8));
        byte[] archive = MxlIo.makeZipBytes(entries, false);
        int localNameLength = readU16(archive, 26);
        int centralOffset = 30 + localNameLength + "Zip layout".getBytes(StandardCharsets.UTF_8).length;
        int eocdOffset = centralOffset + 46 + localNameLength;

        assertEquals(0x04034b50L, readU32(archive, 0));
        assertEquals(20, readU16(archive, 4));
        assertEquals(0x0800, readU16(archive, 6));
        assertEquals(ZipEntry.STORED, readU16(archive, 8));
        assertEquals(0, readU16(archive, 28));
        assertEquals(0x02014b50L, readU32(archive, centralOffset));
        assertEquals(20, readU16(archive, centralOffset + 4));
        assertEquals(20, readU16(archive, centralOffset + 6));
        assertEquals(0x0800, readU16(archive, centralOffset + 8));
        assertEquals(0, readU16(archive, centralOffset + 30));
        assertEquals(0, readU16(archive, centralOffset + 32));
        assertEquals(0x06054b50L, readU32(archive, eocdOffset));
        assertEquals(0, readU16(archive, eocdOffset + 20));
    }

    @Test
    public void readsAndRepackagesAllPinnedUpstreamMxlAndMsczSamples() {
        for (int index = 1; index <= 4; index++) {
            String mxlName = "upstream-zip/sample" + index + ".mxl";
            String musicXml = MxlIo.extractMusicXmlTextFromMxl(readTestResource(mxlName));
            assertTrue(musicXml.contains("<score-partwise"), mxlName);
            assertEquals(musicXml, MxlIo.extractMusicXmlTextFromMxl(MxlIo.makeMxlBytes(musicXml)), mxlName);

            String msczName = "upstream-zip/sample" + index + ".mscz";
            String mscx = MxlIo.extractTextFromZipByExtensions(readTestResource(msczName), new String[] { ".mscx" });
            assertTrue(mscx.contains("<museScore"), msczName);
            assertEquals(mscx, MxlIo.extractTextFromZipByExtensions(MxlIo.makeMsczBytes(mscx),
                    new String[] { ".mscx" }), msczName);
        }
    }

    @Test
    public void matchesPinnedNodeRawDeflateZipBytesAfterTimestampNormalization() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("repeated.txt", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                .getBytes(StandardCharsets.UTF_8));
        byte[] expectedFromPinnedNode = Base64.getDecoder().decode(
                "UEsDBBQAAAgIAIpqCV08YkxBBgAAAEAAAAAMAAAAcmVwZWF0ZWQudHh0c3SkDAAAUEsBAhQAFAAACAgAimoJXTxiTEEGAAAAQAAAAAwAAAAAAAAAAAAAAAAAAAAAAHJlcGVhdGVkLnR4dFBLBQYAAAAAAQABADoAAAAwAAAAAAA=");

        assertArrayEquals(normalizeZipTimestamps(expectedFromPinnedNode),
                normalizeZipTimestamps(MxlIo.makeZipBytes(entries, true)));
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
    public void fallsBackToXmlExtensionWhenMxlHasNoContainerRootFile() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.xml", sampleMusicXml("XML extension fallback").getBytes(StandardCharsets.UTF_8));

        String extracted = MxlIo.extractMusicXmlTextFromMxl(MxlIo.makeZipBytes(entries));

        assertTrue(extracted.contains("<work-title>XML extension fallback</work-title>"));
    }

    @Test
    public void rejectsMxlWhenContainerRootFileIsMissing() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("META-INF/container.xml", ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<container><rootfiles><rootfile full-path=\"missing.musicxml\"/></rootfiles></container>")
                .getBytes(StandardCharsets.UTF_8));
        entries.put("score.musicxml", sampleMusicXml("Fallback").getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractMusicXmlTextFromMxl(MxlIo.makeZipBytes(entries)));

        assertEquals("MusicXML root file was not found in archive: missing.musicxml", failure.getMessage());
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
    public void rejectsZipExtensionExtractionWhenNoEntryMatches() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.musicxml", "<score-partwise/>".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractTextFromZipByExtensions(MxlIo.makeZipBytes(entries), new String[] { ".mscx" }));

        assertEquals("No matching entry was found for extensions: .mscx", failure.getMessage());
    }

    @Test
    public void listsRootEntryPathsByExtensionsOnlyAtRoot() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.mscx", "<museScore/>".getBytes(StandardCharsets.UTF_8));
        entries.put("nested/ignored.mscx", "<museScore/>".getBytes(StandardCharsets.UTF_8));
        entries.put("nested/", new byte[0]);

        List<String> paths = MxlIo.listZipRootEntryPathsByExtensions(MxlIo.makeZipBytes(entries),
                new String[] { ".mscx" });

        assertEquals(1, paths.size());
        assertEquals("score.mscx", paths.get(0));
    }

    @Test
    public void extractsExactZipEntryBytesAndRejectsMissingPath() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.musicxml", "<score-partwise/>".getBytes(StandardCharsets.UTF_8));
        byte[] archive = MxlIo.makeZipBytes(entries);

        assertArrayEquals("<score-partwise/>".getBytes(StandardCharsets.UTF_8),
                MxlIo.extractZipEntryBytesByPath(archive, "./score.musicxml"));
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractZipEntryBytesByPath(archive, "missing.musicxml"));
        assertEquals("ZIP entry not found: missing.musicxml", missing.getMessage());
    }

    @Test
    public void distinguishesInvalidZipWithoutCentralDirectoryFromEmptyZip() {
        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractTextFromZipByExtensions(new byte[] { 0x50, 0x4b, 0x03, 0x04 },
                        new String[] { ".xml" }));
        assertEquals("Invalid ZIP: end of central directory was not found.", invalid.getMessage());

        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractTextFromZipByExtensions(MxlIo.makeZipBytes(new LinkedHashMap<String, byte[]>()),
                        new String[] { ".xml" }));
        assertEquals("The ZIP archive is empty.", empty.getMessage());
    }

    @Test
    public void reportsStableCentralDirectoryMetadataFailures() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.musicxml", "<score-partwise/>".getBytes(StandardCharsets.UTF_8));

        byte[] malformedEntry = MxlIo.makeZipBytes(entries, false);
        int centralOffset = findSignature(malformedEntry, new byte[] { 0x50, 0x4b, 0x01, 0x02 });
        malformedEntry[centralOffset] = 0;
        IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractMusicXmlTextFromMxl(malformedEntry));
        assertEquals("Invalid ZIP: central directory entry is malformed.", malformed.getMessage());

        byte[] outOfRange = MxlIo.makeZipBytes(entries, false);
        int eocdOffset = findSignature(outOfRange, new byte[] { 0x50, 0x4b, 0x05, 0x06 });
        writeU32(outOfRange, eocdOffset + 16, 0x7fffffffL);
        IllegalArgumentException range = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractMusicXmlTextFromMxl(outOfRange));
        assertEquals("Invalid ZIP: central directory is out of range.", range.getMessage());

        byte[] missingLocalHeader = MxlIo.makeZipBytes(entries, false);
        int missingLocalCentralOffset = findSignature(missingLocalHeader, new byte[] { 0x50, 0x4b, 0x01, 0x02 });
        writeU32(missingLocalHeader, missingLocalCentralOffset + 42, 1);
        IllegalArgumentException local = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractMusicXmlTextFromMxl(missingLocalHeader));
        assertEquals("Invalid ZIP: local header is missing for \"score.musicxml\".", local.getMessage());

        byte[] dataOutOfRange = MxlIo.makeZipBytes(entries, false);
        int dataCentralOffset = findSignature(dataOutOfRange, new byte[] { 0x50, 0x4b, 0x01, 0x02 });
        writeU32(dataOutOfRange, dataCentralOffset + 20, 0x7fffffffL);
        IllegalArgumentException data = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractMusicXmlTextFromMxl(dataOutOfRange));
        assertEquals("Invalid ZIP: data is out of range for \"score.musicxml\".", data.getMessage());

        byte[] filenameOutOfRange = MxlIo.makeZipBytes(entries, false);
        int filenameCentralOffset = findSignature(filenameOutOfRange, new byte[] { 0x50, 0x4b, 0x01, 0x02 });
        filenameOutOfRange[filenameCentralOffset + 28] = (byte) 0xff;
        filenameOutOfRange[filenameCentralOffset + 29] = (byte) 0xff;
        IllegalArgumentException filename = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractMusicXmlTextFromMxl(filenameOutOfRange));
        assertEquals("Invalid ZIP: entry filename is out of range.", filename.getMessage());
    }

    @Test
    public void defersUnsupportedCompressionFailureUntilItsEntryIsSelected() {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("unreadable.bin", new byte[] { 1, 2, 3 });
        entries.put("readable.txt", "Readable".getBytes(StandardCharsets.UTF_8));
        byte[] archive = MxlIo.makeZipBytes(entries, false);

        int firstLocalOffset = findSignature(archive, new byte[] { 0x50, 0x4b, 0x03, 0x04 });
        int firstCentralOffset = findSignature(archive, new byte[] { 0x50, 0x4b, 0x01, 0x02 });
        writeU16(archive, firstLocalOffset + 8, 12);
        writeU16(archive, firstCentralOffset + 10, 12);

        assertEquals("Readable", MxlIo.extractTextFromZipByExtensions(archive, new String[] { ".txt" }));
        assertEquals(Arrays.asList("unreadable.bin", "readable.txt"),
                MxlIo.listZipRootEntryPathsByExtensions(archive, new String[] { ".bin", ".txt" }));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> MxlIo.extractZipEntryBytesByPath(archive, "unreadable.bin"));
        assertEquals("Unsupported ZIP compression method: 12.", failure.getMessage());
    }

    private static int findSignature(byte[] bytes, byte[] signature) {
        for (int index = 0; index <= bytes.length - signature.length; index++) {
            boolean matches = true;
            for (int offset = 0; offset < signature.length; offset++) {
                if (bytes[index + offset] != signature[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return index;
            }
        }
        throw new AssertionError("ZIP signature was not found.");
    }

    private static void writeU32(byte[] bytes, int offset, long value) {
        bytes[offset] = (byte) (value & 0xff);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xff);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xff);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xff);
    }

    private static void writeU16(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xff);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xff);
    }

    private static int readU16(byte[] bytes, int offset) {
        return ((int) bytes[offset] & 0xff) | (((int) bytes[offset + 1] & 0xff) << 8);
    }

    private static long readU32(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xffL) | (((long) bytes[offset + 1] & 0xffL) << 8)
                | (((long) bytes[offset + 2] & 0xffL) << 16) | (((long) bytes[offset + 3] & 0xffL) << 24);
    }

    private static byte[] readTestResource(String name) {
        InputStream input = MxlIoTest.class.getClassLoader().getResourceAsStream(name);
        if (input == null) {
            throw new AssertionError("Test resource was not found: " + name);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            input.close();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new AssertionError("Failed to read test resource: " + name, ex);
        }
    }

    private static byte[] normalizeZipTimestamps(byte[] archive) {
        byte[] normalized = archive.clone();
        for (int index = 0; index <= normalized.length - 4; index++) {
            long signature = readU32(normalized, index);
            if (signature == 0x04034b50L) {
                writeU16(normalized, index + 10, 0);
                writeU16(normalized, index + 12, 0);
            } else if (signature == 0x02014b50L) {
                writeU16(normalized, index + 12, 0);
                writeU16(normalized, index + 14, 0);
            }
        }
        return normalized;
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
