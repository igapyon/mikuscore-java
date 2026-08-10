/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class MxlIo {
    private static final String CONTAINER_PATH = "META-INF/container.xml";

    private static final class ZipArchiveEntry {
        private final String path;
        private final int compressionMethod;
        private final long compressedSize;
        private final long dataOffset;

        private ZipArchiveEntry(String path, int compressionMethod, long compressedSize, long dataOffset) {
            this.path = path;
            this.compressionMethod = compressionMethod;
            this.compressedSize = compressedSize;
            this.dataOffset = dataOffset;
        }
    }

    private static final class EncodedZipEntry {
        private final byte[] pathBytes;
        private final byte[] data;
        private final long crc;
        private final int compressionMethod;
        private final int uncompressedSize;

        private EncodedZipEntry(byte[] pathBytes, byte[] data, long crc, int compressionMethod,
                int uncompressedSize) {
            this.pathBytes = pathBytes;
            this.data = data;
            this.crc = crc;
            this.compressionMethod = compressionMethod;
            this.uncompressedSize = uncompressedSize;
        }
    }

    /**
     * One ZIP entry to encode. A list of these entries preserves the source
     * order, including deliberately duplicated paths, just as the Node ZIP
     * payload helper does.
     */
    public static final class ZipEntryPayload {
        private final String path;
        private final byte[] bytes;

        public ZipEntryPayload(String path, byte[] bytes) {
            this.path = path;
            this.bytes = bytes == null ? new byte[0] : bytes.clone();
        }

        public String getPath() {
            return path;
        }

        public byte[] getBytes() {
            return bytes.clone();
        }
    }

    private MxlIo() {
    }

    public static String extractMusicXmlTextFromMxl(byte[] archiveBytes) {
        List<ZipArchiveEntry> entries = readZipEntries(archiveBytes);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The MXL archive is empty.");
        }

        ZipArchiveEntry containerEntry = findEntryByPath(entries, CONTAINER_PATH);
        if (containerEntry != null) {
            String rootPath = parseContainerRootFilePath(extractEntryText(archiveBytes, containerEntry));
            if (rootPath != null) {
                ZipArchiveEntry rootEntry = findEntryByPath(entries, rootPath);
                if (rootEntry == null) {
                    throw new IllegalArgumentException("MusicXML root file was not found in archive: " + rootPath);
                }
                return extractEntryText(archiveBytes, rootEntry);
            }
        }

        for (ZipArchiveEntry entry : entries) {
            String path = entry.path.toLowerCase(Locale.ROOT);
            if (path.endsWith(".musicxml")) {
                return extractEntryText(archiveBytes, entry);
            }
        }
        for (ZipArchiveEntry entry : entries) {
            String path = entry.path.toLowerCase(Locale.ROOT);
            if (path.endsWith(".xml") && !path.equals("meta-inf/container.xml")) {
                return extractEntryText(archiveBytes, entry);
            }
        }
        throw new IllegalArgumentException("No MusicXML file (.musicxml or .xml) was found in the MXL archive.");
    }

    public static String extractTextFromZipByExtensions(byte[] archiveBytes, String[] extensions) {
        List<ZipArchiveEntry> entries = readZipEntries(archiveBytes);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The ZIP archive is empty.");
        }
        for (ZipArchiveEntry entry : entries) {
            if (hasAnyExtension(entry.path, extensions)) {
                return extractEntryText(archiveBytes, entry);
            }
        }
        throw new IllegalArgumentException("No matching entry was found for extensions: " + joinExtensions(extensions));
    }

    public static List<String> listZipRootEntryPathsByExtensions(byte[] archiveBytes, String[] extensions) {
        List<ZipArchiveEntry> entries = readZipEntries(archiveBytes);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The ZIP archive is empty.");
        }
        List<String> result = new ArrayList<String>();
        for (ZipArchiveEntry entry : entries) {
            if (entry.path.indexOf('/') < 0 && hasAnyExtension(entry.path, extensions)) {
                result.add(entry.path);
            }
        }
        return result;
    }

    /**
     * Extracts one exact ZIP entry as bytes, using the same normalized lookup
     * path as the upstream ZIP helper.
     */
    public static byte[] extractZipEntryBytesByPath(byte[] archiveBytes, String entryPath) {
        List<ZipArchiveEntry> entries = readZipEntries(archiveBytes);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The ZIP archive is empty.");
        }
        ZipArchiveEntry entry = findEntryByPath(entries, entryPath);
        if (entry == null) {
            throw new IllegalArgumentException("ZIP entry not found: " + entryPath);
        }
        return extractEntryBytes(archiveBytes, entry);
    }

    public static byte[] makeMxlBytes(String formattedXml) {
        String containerXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                + "<rootfiles><rootfile full-path=\"score.musicxml\" media-type=\"application/vnd.recordare.musicxml+xml\"/></rootfiles>"
                + "</container>";
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put(CONTAINER_PATH, containerXml.getBytes(StandardCharsets.UTF_8));
        entries.put("score.musicxml", formattedXml.getBytes(StandardCharsets.UTF_8));
        return makeZipBytes(entries);
    }

    public static byte[] makeMsczBytes(String mscxText) {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("score.mscx", mscxText.getBytes(StandardCharsets.UTF_8));
        return makeZipBytes(entries);
    }

    public static byte[] makeZipBytes(Map<String, byte[]> entries) {
        return makeZipBytes(entries, true);
    }

    /**
     * Encodes ZIP entries using the upstream selection rule: when compression
     * is preferred, choose DEFLATE only if a raw-DEFLATE trial is smaller than
     * the original payload; otherwise write a stored entry. Explicitly stored
     * mode remains available for deterministic fixture construction.
     */
    public static byte[] makeZipBytes(Map<String, byte[]> entries, boolean preferCompression) {
        List<ZipEntryPayload> payloads = new ArrayList<ZipEntryPayload>();
        if (entries != null) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                payloads.add(new ZipEntryPayload(entry.getKey(), entry.getValue()));
            }
        }
        return makeZipBytes(payloads, preferCompression);
    }

    /**
     * Encodes ZIP payload entries in their supplied order. Unlike the legacy
     * map overload, this form intentionally retains duplicate paths.
     */
    public static byte[] makeZipBytes(List<ZipEntryPayload> entries, boolean preferCompression) {
        List<EncodedZipEntry> encodedEntries = new ArrayList<EncodedZipEntry>();
        List<ZipEntryPayload> safeEntries = entries == null ? Collections.<ZipEntryPayload>emptyList() : entries;
        for (ZipEntryPayload entry : safeEntries) {
            String path = entry == null ? null : entry.getPath();
            byte[] uncompressed = entry == null ? new byte[0] : entry.getBytes();
            byte[] data = uncompressed;
            int compressionMethod = ZipEntry.STORED;
            if (preferCompression) {
                byte[] compressed = deflateRaw(uncompressed);
                if (compressed.length < uncompressed.length) {
                    data = compressed;
                    compressionMethod = ZipEntry.DEFLATED;
                }
            }
            CRC32 crc = new CRC32();
            crc.update(uncompressed);
            encodedEntries.add(new EncodedZipEntry(
                    normalizeZipEntryPathForWrite(path).getBytes(StandardCharsets.UTF_8),
                    data, crc.getValue(), compressionMethod, uncompressed.length));
        }

        Calendar calendar = Calendar.getInstance();
        int dosTime = (calendar.get(Calendar.HOUR_OF_DAY) << 11) | (calendar.get(Calendar.MINUTE) << 5)
                | (calendar.get(Calendar.SECOND) / 2);
        int dosDate = ((Math.max(1980, Math.min(2107, calendar.get(Calendar.YEAR))) - 1980) << 9)
                | ((calendar.get(Calendar.MONTH) + 1) << 5) | calendar.get(Calendar.DAY_OF_MONTH);

        ByteArrayOutputStream localChunks = new ByteArrayOutputStream();
        ByteArrayOutputStream centralChunks = new ByteArrayOutputStream();
        int localOffset = 0;
        for (EncodedZipEntry entry : encodedEntries) {
            writeZipLocalHeader(localChunks, entry, dosTime, dosDate);
            localChunks.write(entry.data, 0, entry.data.length);
            writeZipCentralHeader(centralChunks, entry, dosTime, dosDate, localOffset);
            localOffset += 30 + entry.pathBytes.length + entry.data.length;
        }

        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        byte[] localBytes = localChunks.toByteArray();
        byte[] centralBytes = centralChunks.toByteArray();
        archive.write(localBytes, 0, localBytes.length);
        archive.write(centralBytes, 0, centralBytes.length);
        writeU32(archive, 0x06054b50L);
        writeU16(archive, 0);
        writeU16(archive, 0);
        writeU16(archive, encodedEntries.size());
        writeU16(archive, encodedEntries.size());
        writeU32(archive, centralBytes.length);
        writeU32(archive, localBytes.length);
        writeU16(archive, 0);
        return archive.toByteArray();
    }

    private static byte[] deflateRaw(byte[] bytes) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try {
            deflater.setInput(bytes);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                if (count <= 0) {
                    throw new IllegalArgumentException("Failed to encode ZIP archive.");
                }
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static void writeZipLocalHeader(ByteArrayOutputStream out, EncodedZipEntry entry, int dosTime,
            int dosDate) {
        writeU32(out, 0x04034b50L);
        writeU16(out, 20);
        writeU16(out, 0x0800);
        writeU16(out, entry.compressionMethod);
        writeU16(out, dosTime);
        writeU16(out, dosDate);
        writeU32(out, entry.crc);
        writeU32(out, entry.data.length);
        writeU32(out, entry.uncompressedSize);
        writeU16(out, entry.pathBytes.length);
        writeU16(out, 0);
        out.write(entry.pathBytes, 0, entry.pathBytes.length);
    }

    private static void writeZipCentralHeader(ByteArrayOutputStream out, EncodedZipEntry entry, int dosTime,
            int dosDate, int localOffset) {
        writeU32(out, 0x02014b50L);
        writeU16(out, 20);
        writeU16(out, 20);
        writeU16(out, 0x0800);
        writeU16(out, entry.compressionMethod);
        writeU16(out, dosTime);
        writeU16(out, dosDate);
        writeU32(out, entry.crc);
        writeU32(out, entry.data.length);
        writeU32(out, entry.uncompressedSize);
        writeU16(out, entry.pathBytes.length);
        writeU16(out, 0);
        writeU16(out, 0);
        writeU16(out, 0);
        writeU16(out, 0);
        writeU32(out, 0);
        writeU32(out, localOffset);
        out.write(entry.pathBytes, 0, entry.pathBytes.length);
    }

    private static void writeU16(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >>> 8) & 0xff);
    }

    private static void writeU32(ByteArrayOutputStream out, long value) {
        out.write((int) (value & 0xff));
        out.write((int) ((value >>> 8) & 0xff));
        out.write((int) ((value >>> 16) & 0xff));
        out.write((int) ((value >>> 24) & 0xff));
    }

    /**
     * Reads the central directory as the upstream ZIP helper does. Entry
     * payloads are expanded only when a public operation actually selects one,
     * so an unrelated entry using an unknown compression method remains inert.
     */
    private static List<ZipArchiveEntry> readZipEntries(byte[] archiveBytes) {
        int eocdOffset = findEndOfCentralDirectoryOffset(archiveBytes);
        if (eocdOffset < 0 || eocdOffset + 22 > archiveBytes.length) {
            throw new IllegalArgumentException("Invalid ZIP: end of central directory was not found.");
        }
        long centralSize = readLittleEndianU32(archiveBytes, eocdOffset + 12);
        long centralOffset = readLittleEndianU32(archiveBytes, eocdOffset + 16);
        long centralEnd = centralOffset + centralSize;
        if (centralOffset < 0 || centralEnd > archiveBytes.length) {
            throw new IllegalArgumentException("Invalid ZIP: central directory is out of range.");
        }

        List<ZipArchiveEntry> entries = new ArrayList<ZipArchiveEntry>();
        long offset = centralOffset;
        while (offset < centralEnd) {
            if (offset + 46 > archiveBytes.length || readLittleEndianU32(archiveBytes, (int) offset) != 0x02014b50L) {
                throw new IllegalArgumentException("Invalid ZIP: central directory entry is malformed.");
            }
            int base = (int) offset;
            int flags = readLittleEndianU16(archiveBytes, base + 8);
            int compressionMethod = readLittleEndianU16(archiveBytes, base + 10);
            long compressedSize = readLittleEndianU32(archiveBytes, base + 20);
            int fileNameLength = readLittleEndianU16(archiveBytes, base + 28);
            int extraLength = readLittleEndianU16(archiveBytes, base + 30);
            int commentLength = readLittleEndianU16(archiveBytes, base + 32);
            long localHeaderOffset = readLittleEndianU32(archiveBytes, base + 42);
            long fileNameStart = offset + 46;
            long fileNameEnd = fileNameStart + fileNameLength;
            if (fileNameEnd > archiveBytes.length) {
                throw new IllegalArgumentException("Invalid ZIP: entry filename is out of range.");
            }
            String fileName = new String(archiveBytes, (int) fileNameStart, fileNameLength,
                    (flags & 0x0800) != 0 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1);
            String normalizedPath = normalizeZipPath(fileName);

            if (localHeaderOffset + 30 > archiveBytes.length
                    || readLittleEndianU32(archiveBytes, (int) localHeaderOffset) != 0x04034b50L) {
                throw new IllegalArgumentException("Invalid ZIP: local header is missing for \"" + normalizedPath
                        + "\".");
            }
            int localBase = (int) localHeaderOffset;
            long dataOffset = localHeaderOffset + 30 + readLittleEndianU16(archiveBytes, localBase + 26)
                    + readLittleEndianU16(archiveBytes, localBase + 28);
            if (dataOffset + compressedSize > archiveBytes.length) {
                throw new IllegalArgumentException("Invalid ZIP: data is out of range for \"" + normalizedPath
                        + "\".");
            }
            if (!normalizedPath.isEmpty() && !normalizedPath.endsWith("/")) {
                entries.add(new ZipArchiveEntry(normalizedPath, compressionMethod, compressedSize, dataOffset));
            }
            offset = fileNameEnd + extraLength + commentLength;
        }
        return entries;
    }

    private static int findEndOfCentralDirectoryOffset(byte[] archiveBytes) {
        if (archiveBytes == null) {
            return -1;
        }
        int minOffset = Math.max(0, archiveBytes.length - 65557);
        for (int offset = archiveBytes.length - 22; offset >= minOffset; offset--) {
            if (readLittleEndianU32(archiveBytes, offset) == 0x06054b50L) {
                return offset;
            }
        }
        return -1;
    }

    private static int readLittleEndianU16(byte[] bytes, int offset) {
        if (offset < 0 || offset + 2 > bytes.length) {
            return -1;
        }
        return ((int) bytes[offset] & 0xff) | (((int) bytes[offset + 1] & 0xff) << 8);
    }

    private static long readLittleEndianU32(byte[] bytes, int offset) {
        if (offset < 0 || offset + 4 > bytes.length) {
            return -1L;
        }
        return ((long) bytes[offset] & 0xffL) | (((long) bytes[offset + 1] & 0xffL) << 8)
                | (((long) bytes[offset + 2] & 0xffL) << 16) | (((long) bytes[offset + 3] & 0xffL) << 24);
    }

    private static ZipArchiveEntry findEntryByPath(List<ZipArchiveEntry> entries, String path) {
        String normalizedPath = normalizeZipPath(path);
        for (ZipArchiveEntry entry : entries) {
            if (entry.path.equals(normalizedPath)) {
                return entry;
            }
        }
        return null;
    }

    private static String extractEntryText(byte[] archiveBytes, ZipArchiveEntry entry) {
        return new String(extractEntryBytes(archiveBytes, entry), StandardCharsets.UTF_8);
    }

    private static byte[] extractEntryBytes(byte[] archiveBytes, ZipArchiveEntry entry) {
        int offset = (int) entry.dataOffset;
        int length = (int) entry.compressedSize;
        byte[] compressed = new byte[length];
        System.arraycopy(archiveBytes, offset, compressed, 0, length);
        if (entry.compressionMethod == ZipEntry.STORED) {
            return compressed;
        }
        if (entry.compressionMethod != ZipEntry.DEFLATED) {
            throw new IllegalArgumentException("Unsupported ZIP compression method: " + entry.compressionMethod + ".");
        }
        return inflateRawDeflate(compressed);
    }

    private static byte[] inflateRawDeflate(byte[] compressed) {
        Inflater inflater = new Inflater(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed), inflater);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            input.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid ZIP archive.", ex);
        } finally {
            inflater.end();
        }
    }

    private static String parseContainerRootFilePath(String containerXmlText) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(containerXmlText);
        if (doc == null) {
            return null;
        }
        NodeList rootFiles = doc.getElementsByTagName("rootfile");
        for (int index = 0; index < rootFiles.getLength(); index++) {
            Element rootFile = (Element) rootFiles.item(index);
            String fullPath = trimToEmpty(rootFile.getAttribute("full-path"));
            if (!fullPath.isEmpty()) {
                return fullPath;
            }
        }
        return null;
    }

    private static boolean hasAnyExtension(String path, String[] extensions) {
        String lowerPath = path.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            String normalized = trimToEmpty(extension).toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && lowerPath.endsWith(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeZipPath(String value) {
        String normalized = String.valueOf(value == null ? "" : value).replace('\\', '/');
        if (normalized.startsWith("./")) {
            return normalized.substring(2);
        }
        if (normalized.startsWith("/")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    private static String normalizeZipEntryPathForWrite(String value) {
        String normalized = String.valueOf(value == null ? "" : value).replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String joinExtensions(String[] extensions) {
        StringBuilder out = new StringBuilder();
        for (String extension : extensions) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(extension);
        }
        return out.toString();
    }

    private static String trimToEmpty(String text) {
        return text == null ? "" : text.trim();
    }
}
