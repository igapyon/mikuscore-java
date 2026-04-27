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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class MxlIo {
    private static final String CONTAINER_PATH = "META-INF/container.xml";

    private MxlIo() {
    }

    public static String extractMusicXmlTextFromMxl(byte[] archiveBytes) {
        Map<String, byte[]> entries = readZipEntries(archiveBytes);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The MXL archive is empty.");
        }

        byte[] containerBytes = entries.get(normalizeZipPath(CONTAINER_PATH));
        if (containerBytes != null) {
            String rootPath = parseContainerRootFilePath(new String(containerBytes, StandardCharsets.UTF_8));
            if (rootPath != null) {
                byte[] rootBytes = entries.get(normalizeZipPath(rootPath));
                if (rootBytes == null) {
                    throw new IllegalArgumentException("MusicXML root file was not found in archive: " + rootPath);
                }
                return new String(rootBytes, StandardCharsets.UTF_8);
            }
        }

        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String path = entry.getKey().toLowerCase(Locale.ROOT);
            if (path.endsWith(".musicxml")) {
                return new String(entry.getValue(), StandardCharsets.UTF_8);
            }
        }
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String path = entry.getKey().toLowerCase(Locale.ROOT);
            if (path.endsWith(".xml") && !path.equals("meta-inf/container.xml")) {
                return new String(entry.getValue(), StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("No MusicXML file (.musicxml or .xml) was found in the MXL archive.");
    }

    public static String extractTextFromZipByExtensions(byte[] archiveBytes, String[] extensions) {
        Map<String, byte[]> entries = readZipEntries(archiveBytes);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The ZIP archive is empty.");
        }
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (hasAnyExtension(entry.getKey(), extensions)) {
                return new String(entry.getValue(), StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("No matching entry was found for extensions: " + joinExtensions(extensions));
    }

    public static List<String> listZipRootEntryPathsByExtensions(byte[] archiveBytes, String[] extensions) {
        Map<String, byte[]> entries = readZipEntries(archiveBytes);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The ZIP archive is empty.");
        }
        List<String> result = new ArrayList<String>();
        for (String path : entries.keySet()) {
            if (path.indexOf('/') < 0 && hasAnyExtension(path, extensions)) {
                result.add(path);
            }
        }
        return result;
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

    public static byte[] makeZipBytes(Map<String, byte[]> entries) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(normalizeZipPath(entry.getKey()));
                zipEntry.setTime(0L);
                zip.putNextEntry(zipEntry);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
            zip.finish();
            zip.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to encode ZIP archive.", ex);
        }
    }

    private static Map<String, byte[]> readZipEntries(byte[] archiveBytes) {
        try {
            Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
            ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archiveBytes), StandardCharsets.UTF_8);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String path = normalizeZipPath(entry.getName());
                if (!path.isEmpty() && !path.endsWith("/")) {
                    entries.put(path, readAll(zip));
                }
                zip.closeEntry();
            }
            zip.close();
            return entries;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid ZIP archive.", ex);
        }
    }

    private static byte[] readAll(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = zip.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
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
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
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
