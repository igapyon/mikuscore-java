/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Runtime-independent MusicXML output metadata policy helpers. */
public final class MusicXmlOutput {
    private static final Pattern DIAGNOSTIC_CODE = Pattern.compile("(?:^|;)code=([^;]+)");

    private MusicXmlOutput() {
    }

    /**
     * Removes selected {@code mks:meta:}, {@code mks:src:}, and
     * {@code mks:dbg:} miscellaneous fields. Invalid XML and the all-kept
     * policy retain the input text verbatim.
     */
    public static String stripMetadataFromMusicXml(String xml, MksMetadataOutputSettings settings) {
        MksMetadataOutputSettings safe = settings == null ? new MksMetadataOutputSettings(true, true, true) : settings;
        if (safe.isKeepMeta() && safe.isKeepSrc() && safe.isKeepDbg()) {
            return xml;
        }
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null) {
            return xml;
        }

        List<Element> fields = new ArrayList<Element>();
        List<Element> miscellaneousNodes = new ArrayList<Element>();
        for (Element part : elementsByTagName(doc, "part")) {
            for (Element measure : directChildren(part, "measure")) {
                for (Element attributes : directChildren(measure, "attributes")) {
                    for (Element miscellaneous : directChildren(attributes, "miscellaneous")) {
                        miscellaneousNodes.add(miscellaneous);
                        for (Element field : directChildren(miscellaneous, "miscellaneous-field")) {
                            String name = field.getAttribute("name");
                            if (name != null && name.startsWith("mks:")) {
                                fields.add(field);
                            }
                        }
                    }
                }
            }
        }
        for (Element field : fields) {
            if (shouldRemoveMksField(field.getAttribute("name"), safe) && field.getParentNode() != null) {
                field.getParentNode().removeChild(field);
            }
        }
        for (Element miscellaneous : miscellaneousNodes) {
            if (directChildren(miscellaneous, "miscellaneous-field").isEmpty()
                    && miscellaneous.getParentNode() != null) {
                miscellaneous.getParentNode().removeChild(miscellaneous);
            }
        }
        for (Element part : elementsByTagName(doc, "part")) {
            for (Element measure : directChildren(part, "measure")) {
                for (Element attributes : new ArrayList<Element>(directChildren(measure, "attributes"))) {
                    if (directElementChildren(attributes).isEmpty() && attributes.getParentNode() != null) {
                        attributes.getParentNode().removeChild(attributes);
                    }
                }
            }
        }
        return MusicXmlIo.serializeMusicXmlDocument(doc);
    }

    /** Summarizes the existing ABC reflow and parser-warning diagnostic fields. */
    public static String summarizeImportedDiagWarnings(String xml) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xml);
        if (doc == null) {
            return "";
        }
        int overfullReflowCount = 0;
        int parserWarningCount = 0;
        for (Element field : elementsByTagName(doc, "miscellaneous-field")) {
            String rawName = field.getAttribute("name");
            if (rawName == null || !rawName.startsWith("mks:diag:")) {
                continue;
            }
            String name = trim(rawName).toLowerCase(Locale.ROOT);
            if ("mks:diag:count".equals(name)) {
                continue;
            }
            Matcher matcher = DIAGNOSTIC_CODE.matcher(trim(field.getTextContent()));
            String code = matcher.find() ? trim(matcher.group(1)).toUpperCase(Locale.ROOT) : "";
            if ("OVERFULL_REFLOWED".equals(code)) {
                overfullReflowCount++;
            }
            if ("ABC_IMPORT_WARNING".equals(code)) {
                parserWarningCount++;
            }
        }
        List<String> summaries = new ArrayList<String>();
        if (overfullReflowCount > 0) {
            summaries.add("ABC overfull auto-reflow: " + overfullReflowCount);
        }
        if (parserWarningCount > 0) {
            summaries.add("ABC parser warnings: " + parserWarningCount);
        }
        return join(summaries, " / ");
    }

    private static boolean shouldRemoveMksField(String fieldName, MksMetadataOutputSettings settings) {
        String lowered = trim(fieldName).toLowerCase(Locale.ROOT);
        if (!lowered.startsWith("mks:")) {
            return false;
        }
        if (lowered.startsWith("mks:meta:")) {
            return !settings.isKeepMeta();
        }
        if (lowered.startsWith("mks:src:")) {
            return !settings.isKeepSrc();
        }
        if (lowered.startsWith("mks:dbg:")) {
            return !settings.isKeepDbg();
        }
        return false;
    }

    private static List<Element> elementsByTagName(Document doc, String tagName) {
        List<Element> result = new ArrayList<Element>();
        if (doc == null) {
            return result;
        }
        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int index = 0; index < nodes.getLength(); index++) {
            if (nodes.item(index) instanceof Element) {
                result.add((Element) nodes.item(index));
            }
        }
        return result;
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<Element>();
        if (parent == null) {
            return result;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                result.add((Element) child);
            }
            child = child.getNextSibling();
        }
        return result;
    }

    private static List<Element> directElementChildren(Element parent) {
        List<Element> result = new ArrayList<Element>();
        if (parent == null) {
            return result;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                result.add((Element) child);
            }
            child = child.getNextSibling();
        }
        return result;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String join(List<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) {
                out.append(separator);
            }
            out.append(value);
        }
        return out.toString();
    }

    /** Immutable output policy corresponding to {@code MksMetadataOutputSettings}. */
    public static final class MksMetadataOutputSettings {
        private final boolean keepMeta;
        private final boolean keepSrc;
        private final boolean keepDbg;

        public MksMetadataOutputSettings(boolean keepMeta, boolean keepSrc, boolean keepDbg) {
            this.keepMeta = keepMeta;
            this.keepSrc = keepSrc;
            this.keepDbg = keepDbg;
        }

        public boolean isKeepMeta() {
            return keepMeta;
        }

        public boolean isKeepSrc() {
            return keepSrc;
        }

        public boolean isKeepDbg() {
            return keepDbg;
        }
    }
}
