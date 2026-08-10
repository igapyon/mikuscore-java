/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public final class MusicXmlIo {
    private MusicXmlIo() {
    }

    public static final class RenderDocBundle {
        private final Document renderDoc;
        private final Map<String, String> svgIdToNodeId;
        private final int noteCount;

        private RenderDocBundle(Document renderDoc, Map<String, String> svgIdToNodeId, int noteCount) {
            this.renderDoc = renderDoc;
            this.svgIdToNodeId = svgIdToNodeId;
            this.noteCount = noteCount;
        }

        public Document getRenderDoc() {
            return renderDoc;
        }

        public Map<String, String> getSvgIdToNodeId() {
            return svgIdToNodeId;
        }

        public int getNoteCount() {
            return noteCount;
        }
    }

    public static final class PreviewSvgIdMap {
        private final Map<String, String> map;
        private final String mapMode;

        private PreviewSvgIdMap(Map<String, String> map, String mapMode) {
            this.map = map == null ? Collections.<String, String>emptyMap() : map;
            this.mapMode = mapMode == null ? "" : mapMode;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public String getMapMode() {
            return mapMode;
        }
    }

    /**
     * Runtime-independent input used by the shared MusicXML/ABC beam calculator.
     */
    public static final class BeamEventInfo {
        private final boolean timed;
        private final boolean chord;
        private final boolean grace;
        private final int durationDiv;
        private final int levels;
        private final String explicitMode;

        public BeamEventInfo(boolean timed, boolean chord, boolean grace, int durationDiv, int levels,
                String explicitMode) {
            this.timed = timed;
            this.chord = chord;
            this.grace = grace;
            this.durationDiv = durationDiv;
            this.levels = levels;
            this.explicitMode = trimToEmpty(explicitMode);
        }

        public boolean isTimed() {
            return timed;
        }

        public boolean isChord() {
            return chord;
        }

        public boolean isGrace() {
            return grace;
        }

        public int getDurationDiv() {
            return durationDiv;
        }

        public int getLevels() {
            return levels;
        }

        public String getExplicitMode() {
            return explicitMode;
        }
    }

    /**
     * A calculated MusicXML beam state and level count.
     */
    public static final class BeamAssignment {
        private final String state;
        private final int levels;

        public BeamAssignment(String state, int levels) {
            this.state = trimToEmpty(state);
            this.levels = levels;
        }

        public String getState() {
            return state;
        }

        public int getLevels() {
            return levels;
        }
    }

    public static Document parseMusicXmlDocument(String xmlText) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            setFeatureIfAvailable(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setFeatureIfAvailable(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureIfAvailable(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            setFeatureIfAvailable(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            // MusicXML samples commonly declare the public Recordare DTD. Node's
            // XML parser accepts that declaration without fetching it, so retain
            // the declaration while explicitly making every external entity empty.
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            builder.setErrorHandler(new QuietErrorHandler());
            return builder.parse(new InputSource(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception ex) {
            return null;
        }
    }

    public static String serializeMusicXmlDocument(Document doc) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize MusicXML.", ex);
        }
    }

    public static String prettyPrintMusicXmlText(String xml) {
        String compact = trimJavaScriptWhitespace(compactXmlTagWhitespace(String.valueOf(xml == null ? "" : xml)));
        if (compact.isEmpty()) {
            return "";
        }
        String[] split = compact.replaceAll("(>)(<)(/?)", "$1\n$2$3").split("\n");
        int indent = 0;
        StringBuilder out = new StringBuilder();
        for (String rawToken : split) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.startsWith("</")) {
                indent = Math.max(0, indent - 1);
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            for (int index = 0; index < indent; index++) {
                out.append(' ');
            }
            out.append(token);
            boolean opening = token.matches("^<[^!?/][^>]*>$");
            boolean selfClosing = token.endsWith("/>");
            if (opening && !selfClosing) {
                indent++;
            }
        }
        return out.toString();
    }

    /** Mirrors the {@code />\s+</} compaction in musicxml-io.ts. */
    private static String compactXmlTagWhitespace(String xml) {
        StringBuilder compact = new StringBuilder();
        int index = 0;
        while (index < xml.length()) {
            char current = xml.charAt(index);
            compact.append(current);
            index++;
            if (current != '>') {
                continue;
            }
            int cursor = index;
            while (cursor < xml.length()) {
                int codePoint = xml.codePointAt(cursor);
                if (!isJavaScriptWhitespace(codePoint)) {
                    break;
                }
                cursor += Character.charCount(codePoint);
            }
            if (cursor > index && cursor < xml.length() && xml.charAt(cursor) == '<') {
                index = cursor;
            }
        }
        return compact.toString();
    }

    /** ECMAScript String#trim and {@code \s} whitespace used by the Node source. */
    private static String trimJavaScriptWhitespace(String text) {
        int start = 0;
        int end = text.length();
        while (start < end) {
            int codePoint = text.codePointAt(start);
            if (!isJavaScriptWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (start < end) {
            int codePoint = text.codePointBefore(end);
            if (!isJavaScriptWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return text.substring(start, end);
    }

    private static boolean isJavaScriptWhitespace(int codePoint) {
        return (codePoint >= 0x0009 && codePoint <= 0x000D) || codePoint == 0x0020 || codePoint == 0x00A0
                || codePoint == 0x1680 || (codePoint >= 0x2000 && codePoint <= 0x200A) || codePoint == 0x2028
                || codePoint == 0x2029 || codePoint == 0x202F || codePoint == 0x205F || codePoint == 0x3000
                || codePoint == 0xFEFF;
    }

    public static String normalizeImportedMusicXmlText(String xml) {
        Document doc = parseMusicXmlDocument(xml);
        if (doc == null) {
            return xml;
        }
        normalizeImportedMusicXmlDocument(doc);
        return prettyPrintMusicXmlText(serializeMusicXmlDocument(doc));
    }

    public static String applyImplicitBeamsToMusicXmlText(String xml) {
        Document doc = parseMusicXmlDocument(xml);
        if (doc == null) {
            return xml;
        }
        applyImplicitBeamsToMusicXmlDocument(doc);
        return serializeMusicXmlDocument(doc);
    }

    public static Document normalizeImportedMusicXmlDocument(Document doc) {
        normalizePartListAndPartIds(doc);
        enrichTupletNotationsInDocument(doc);
        ensureFinalBarlineInEachPart(doc);
        return doc;
    }

    public static Document applyImplicitBeamsToMusicXmlDocument(Document doc) {
        enrichImplicitBeamsInDocument(doc);
        return doc;
    }

    public static RenderDocBundle buildRenderDocWithNodeIds(Document sourceDoc, List<String> nodeIds, String idPrefix) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (nodeIds == null || nodeIds.isEmpty()) {
            return new RenderDocBundle(sourceDoc, map, 0);
        }
        Document doc = cloneXmlDocument(sourceDoc);
        List<Element> notes = elementsByTagName(doc, "note");
        int count = Math.min(notes.size(), nodeIds.size());
        for (int index = 0; index < count; index++) {
            String nodeId = nodeIds.get(index);
            String svgId = String.valueOf(idPrefix) + "-" + nodeId;
            Element note = notes.get(index);
            note.setAttribute("xml:id", svgId);
            note.setAttribute("id", svgId);
            map.put(svgId, nodeId);
        }
        return new RenderDocBundle(doc, map, count);
    }

    public static PreviewSvgIdMap preparePreviewSvgIdMap(RenderDocBundle renderBundle, List<String> noteNodeIds,
            List<String> renderedNoteIds) {
        Map<String, String> directMap = renderBundle == null ? Collections.<String, String>emptyMap()
                : renderBundle.getSvgIdToNodeId();
        List<String> rendered = renderedNoteIds == null ? Collections.<String>emptyList() : renderedNoteIds;
        boolean hasEmbeddedMikuScoreNoteId = false;
        for (String renderedId : rendered) {
            if (renderedId != null && renderedId.startsWith("mks-")) {
                hasEmbeddedMikuScoreNoteId = true;
                break;
            }
        }
        if (rendered.isEmpty() || hasEmbeddedMikuScoreNoteId) {
            return new PreviewSvgIdMap(directMap, "direct");
        }
        List<String> nodes = noteNodeIds == null ? Collections.<String>emptyList() : noteNodeIds;
        Map<String, String> fallback = new LinkedHashMap<String, String>();
        int count = Math.min(nodes.size(), rendered.size());
        for (int index = 0; index < count; index++) {
            fallback.put(rendered.get(index), nodes.get(index));
        }
        return new PreviewSvgIdMap(fallback, "fallback-seq");
    }

    public static Document extractMeasureEditorDocument(Document sourceDoc, String partId, String measureNumber) {
        Element srcRoot = scorePartwiseRoot(sourceDoc);
        Element srcPart = findPartById(sourceDoc, partId);
        if (srcRoot == null || srcPart == null) {
            return null;
        }
        Element srcMeasure = findMeasureByNumber(srcPart, measureNumber);
        if (srcMeasure == null) {
            return null;
        }

        Element patchedMeasure = (Element) srcMeasure.cloneNode(true);
        Element effectiveAttrs = collectEffectiveMeasureAttributes(srcPart, srcMeasure);
        if (effectiveAttrs != null) {
            Element existing = directChild(patchedMeasure, "attributes");
            if (existing == null) {
                patchedMeasure.insertBefore(effectiveAttrs, patchedMeasure.getFirstChild());
            } else {
                mergeMissingEffectiveAttributes(existing, effectiveAttrs);
            }
        }

        Document dst = createDocumentWithRoot("score-partwise");
        Element dstRoot = dst.getDocumentElement();
        String version = srcRoot.getAttribute("version");
        if (version != null && !version.isEmpty()) {
            dstRoot.setAttribute("version", version);
        }

        Element srcPartList = directChild(srcRoot, "part-list");
        Element srcScorePart = findScorePartById(sourceDoc, partId);
        if (srcPartList != null && srcScorePart != null) {
            Element dstPartList = (Element) dst.importNode(srcPartList, false);
            Element dstScorePart = (Element) dst.importNode(srcScorePart, true);
            Element dstPartName = directChild(dstScorePart, "part-name");
            if (dstPartName != null) {
                dstPartName.setTextContent("");
            }
            Element dstPartAbbreviation = directChild(dstScorePart, "part-abbreviation");
            if (dstPartAbbreviation != null) {
                dstPartAbbreviation.setTextContent("");
            }
            dstPartList.appendChild(dstScorePart);
            dstRoot.appendChild(dstPartList);
        }

        Element dstPart = (Element) dst.importNode(srcPart, false);
        dstPart.appendChild(dst.importNode(patchedMeasure, true));
        dstRoot.appendChild(dstPart);
        return dst;
    }

    public static Document replaceMeasureInMainDocument(Document mainDoc, String partId, String measureNumber,
            Document measureDoc) {
        Element replacementMeasure = firstPartMeasure(measureDoc);
        if (replacementMeasure == null) {
            return null;
        }
        Element targetPart = findPartById(mainDoc, partId);
        if (targetPart == null) {
            return null;
        }
        Element targetMeasure = findMeasureByNumber(targetPart, measureNumber);
        if (targetMeasure == null) {
            return null;
        }

        Element replacementForMain = (Element) replacementMeasure.cloneNode(true);
        Element replacementAttrs = directChild(replacementForMain, "attributes");
        Element targetAttrs = directChild(targetMeasure, "attributes");
        if (replacementAttrs != null && targetAttrs == null) {
            replacementForMain.removeChild(replacementAttrs);
        }

        Document next = cloneXmlDocument(mainDoc);
        Element nextPart = findPartById(next, partId);
        if (nextPart == null) {
            return null;
        }
        Element nextTargetMeasure = findMeasureByNumber(nextPart, measureNumber);
        if (nextTargetMeasure == null || nextTargetMeasure.getParentNode() == null) {
            return null;
        }
        nextTargetMeasure.getParentNode().replaceChild(next.importNode(replacementForMain, true), nextTargetMeasure);
        return next;
    }

    /**
     * String facade for extracting a self-contained MusicXML measure editor
     * document. Mirrors {@code measure-operations.extractMeasureEditorMusicXml}
     * in the Node implementation.
     */
    public static String extractMeasureEditorMusicXml(String sourceXml, String partId, String measureNumber) {
        Document sourceDoc = parseMusicXmlDocument(sourceXml);
        if (sourceDoc == null) {
            return null;
        }
        Document extracted = extractMeasureEditorDocument(sourceDoc, partId, measureNumber);
        return extracted == null ? null : serializeMusicXmlDocument(extracted);
    }

    /**
     * String facade for replacing one MusicXML measure. Editor-only inherited
     * attributes are removed by {@link #replaceMeasureInMainDocument} when the
     * target measure did not originally declare attributes.
     */
    public static String replaceMeasureInMusicXml(String sourceXml, String partId, String measureNumber,
            String measureXml) {
        Document mainDoc = parseMusicXmlDocument(sourceXml);
        Document measureDoc = parseMusicXmlDocument(measureXml);
        if (mainDoc == null || measureDoc == null) {
            return null;
        }
        Document merged = replaceMeasureInMainDocument(mainDoc, partId, measureNumber, measureDoc);
        return merged == null ? null : serializeMusicXmlDocument(merged);
    }

    /**
     * Append one full-measure rest to every part in a score. The rest duration
     * inherits the final measure's effective time/divisions context. A treble
     * and bass grand staff gets synchronized rests separated by a backup.
     */
    public static String appendMeasureToMusicXml(String sourceXml) {
        Document doc = parseMusicXmlDocument(sourceXml);
        Element root = scorePartwiseRoot(doc);
        if (root == null) {
            return null;
        }
        List<Element> parts = directChildren(root, "part");
        if (parts.isEmpty()) {
            return null;
        }

        for (Element part : parts) {
            List<Element> measures = directChildren(part, "measure");
            Element lastMeasure = measures.isEmpty() ? null : measures.get(measures.size() - 1);
            if (lastMeasure == null) {
                continue;
            }

            long capacity = resolveAppendMeasureCapacity(lastMeasure);
            Element measure = doc.createElement("measure");
            measure.setAttribute("number", deriveNextMeasureNumber(part));
            int staves = resolveEffectiveStavesAtEnd(part);
            boolean grandStaff = staves >= 2 && resolveHasTrebleBassGrandStaffAtEnd(part);
            if (grandStaff) {
                measure.appendChild(createMeasureRestNote(doc, capacity, "1", "1"));
                Element backup = doc.createElement("backup");
                Element backupDuration = doc.createElement("duration");
                backupDuration.setTextContent(Long.toString(capacity));
                backup.appendChild(backupDuration);
                measure.appendChild(backup);
                measure.appendChild(createMeasureRestNote(doc, capacity, "1", "2"));
            } else {
                measure.appendChild(createMeasureRestNote(doc, capacity, "1", null));
            }
            part.appendChild(measure);
        }
        return serializeMusicXmlDocument(doc);
    }

    private static Element createMeasureRestNote(Document doc, long duration, String voice, String staff) {
        Element note = doc.createElement("note");
        Element rest = doc.createElement("rest");
        rest.setAttribute("measure", "yes");
        note.appendChild(rest);
        Element durationElement = doc.createElement("duration");
        durationElement.setTextContent(Long.toString(duration));
        note.appendChild(durationElement);
        Element voiceElement = doc.createElement("voice");
        voiceElement.setTextContent(voice);
        note.appendChild(voiceElement);
        if (staff != null) {
            Element staffElement = doc.createElement("staff");
            staffElement.setTextContent(staff);
            note.appendChild(staffElement);
        }
        return note;
    }

    private static long resolveAppendMeasureCapacity(Element measure) {
        if (measure == null || !(measure.getParentNode() instanceof Element)) {
            return 3840L;
        }
        Element part = (Element) measure.getParentNode();
        if (!"part".equals(part.getTagName())) {
            return 3840L;
        }
        List<Element> measures = directChildren(part, "measure");
        int measureIndex = measures.indexOf(measure);
        if (measureIndex < 0) {
            return 3840L;
        }
        Double beats = null;
        Double beatType = null;
        Double divisions = null;
        for (int index = measureIndex; index >= 0; index--) {
            Element attributes = directChild(measures.get(index), "attributes");
            if (attributes == null) {
                continue;
            }
            String divisionsText = directChildText(attributes, "divisions");
            if (divisions == null && !trimToEmpty(divisionsText).isEmpty()) {
                divisions = Double.valueOf(toNumber(divisionsText));
            }
            Element time = directChild(attributes, "time");
            String beatsText = time == null ? null : directChildText(time, "beats");
            if (beats == null && !trimToEmpty(beatsText).isEmpty()) {
                beats = Double.valueOf(toNumber(beatsText));
            }
            String beatTypeText = time == null ? null : directChildText(time, "beat-type");
            if (beatType == null && !trimToEmpty(beatTypeText).isEmpty()) {
                beatType = Double.valueOf(toNumber(beatTypeText));
            }
            if (beats != null && beatType != null && divisions != null) {
                break;
            }
        }
        if (beats == null || beatType == null || divisions == null || !Double.isFinite(beats.doubleValue())
                || !Double.isFinite(beatType.doubleValue()) || !Double.isFinite(divisions.doubleValue())
                || beatType.doubleValue() <= 0d) {
            return 3840L;
        }
        double capacity = beats.doubleValue() * ((4d / beatType.doubleValue()) * divisions.doubleValue());
        if (!Double.isFinite(capacity)) {
            return 3840L;
        }
        long rounded = Math.round(capacity);
        return rounded > 0L ? rounded : 3840L;
    }

    private static int resolveEffectiveStavesAtEnd(Element part) {
        int staves = 1;
        for (Element measure : directChildren(part, "measure")) {
            Element attributes = directChild(measure, "attributes");
            Element stavesElement = directChild(attributes, "staves");
            double parsed = toNumber(stavesElement == null ? null : stavesElement.getTextContent());
            if (Double.isFinite(parsed) && parsed == Math.rint(parsed) && parsed > 0d
                    && parsed <= Integer.MAX_VALUE) {
                staves = (int) parsed;
            }
        }
        return staves;
    }

    private static boolean resolveHasTrebleBassGrandStaffAtEnd(Element part) {
        String clef1 = "";
        String clef2 = "";
        for (Element measure : directChildren(part, "measure")) {
            Element attributes = directChild(measure, "attributes");
            if (attributes == null) {
                continue;
            }
            for (Element clef : directChildren(attributes, "clef")) {
                String number = clef.getAttribute("number");
                Element sign = directChild(clef, "sign");
                String signText = sign == null ? "" : trimToEmpty(sign.getTextContent());
                if ("1".equals(number) && !signText.isEmpty()) {
                    clef1 = signText;
                }
                if ("2".equals(number) && !signText.isEmpty()) {
                    clef2 = signText;
                }
            }
        }
        return "G".equals(clef1) && "F".equals(clef2);
    }

    private static String deriveNextMeasureNumber(Element part) {
        List<Element> measures = directChildren(part, "measure");
        Element lastMeasure = measures.isEmpty() ? null : measures.get(measures.size() - 1);
        if (lastMeasure == null) {
            return "1";
        }
        double parsed = toNumber(lastMeasure.getAttribute("number"));
        if (Double.isFinite(parsed) && parsed == Math.rint(parsed) && parsed >= 0d
                && parsed < Long.MAX_VALUE) {
            return Long.toString((long) parsed + 1L);
        }
        return Integer.toString(measures.size() + 1);
    }

    private static void enrichImplicitBeamsInDocument(Document doc) {
        Element root = scorePartwiseRoot(doc);
        if (root == null) {
            return;
        }
        for (Element part : directChildren(root, "part")) {
            enrichImplicitBeamsInPart(part);
        }
    }

    private static void enrichImplicitBeamsInPart(Element part) {
        BeamMeasureState state = new BeamMeasureState(480, 4, 4);
        for (Element measure : directChildren(part, "measure")) {
            state = enrichImplicitBeamsInMeasure(measure, state);
        }
    }

    private static BeamMeasureState enrichImplicitBeamsInMeasure(Element measure, BeamMeasureState state) {
        BeamMeasureState nextState = updateBeamMeasureState(measure, state);
        int beatDiv = Math.max(1, Math.round((nextState.divisions * 4.0f) / Math.max(1, nextState.beatType)));
        Set<String> lanes = new HashSet<String>();
        Set<String> laneHasExistingBeam = new HashSet<String>();
        for (Element note : directChildren(measure, "note")) {
            if (directChild(note, "chord") != null) {
                continue;
            }
            String lane = noteLaneKey(note);
            lanes.add(lane);
            if (directChild(note, "beam") != null) {
                laneHasExistingBeam.add(lane);
            }
        }
        for (String lane : lanes) {
            if (laneHasExistingBeam.contains(lane)) {
                continue;
            }
            applyImplicitBeamsToLaneTimeline(buildBeamTimelineForLane(measure, lane), beatDiv);
        }
        return nextState;
    }

    private static BeamMeasureState updateBeamMeasureState(Element measure, BeamMeasureState current) {
        int divisions = current.divisions;
        int beats = current.beats;
        int beatType = current.beatType;
        Element attributes = directChild(measure, "attributes");
        if (attributes != null) {
            Integer parsedDivisions = parsePositiveInteger(directChildText(attributes, "divisions"));
            if (parsedDivisions != null) {
                divisions = parsedDivisions.intValue();
            }
            Element time = directChild(attributes, "time");
            if (time != null) {
                Integer parsedBeats = parsePositiveInteger(directChildText(time, "beats"));
                Integer parsedBeatType = parsePositiveInteger(directChildText(time, "beat-type"));
                if (parsedBeats != null) {
                    beats = parsedBeats.intValue();
                }
                if (parsedBeatType != null) {
                    beatType = parsedBeatType.intValue();
                }
            }
        }
        return new BeamMeasureState(divisions, beats, beatType);
    }

    private static List<BeamTimelineEntry> buildBeamTimelineForLane(Element measure, String lane) {
        List<BeamTimelineEntry> timeline = new ArrayList<BeamTimelineEntry>();
        for (Element child : directElementChildren(measure)) {
            if ("backup".equals(child.getTagName())) {
                timeline.add(new BeamTimelineEntry(null, false, false, false, 0, 0));
                continue;
            }
            if ("forward".equals(child.getTagName())) {
                Integer duration = parseNonNegativeInteger(directChildText(child, "duration"));
                timeline.add(new BeamTimelineEntry(null, true, false, false,
                        duration == null ? 0 : duration.intValue(), 0));
                continue;
            }
            if (!"note".equals(child.getTagName())) {
                continue;
            }
            if (directChild(child, "chord") != null) {
                continue;
            }
            if (!lane.equals(noteLaneKey(child))) {
                continue;
            }
            Integer duration = parseNonNegativeInteger(directChildText(child, "duration"));
            int levels = beamLevelsFromType(directChildText(child, "type"));
            timeline.add(new BeamTimelineEntry(child, true, directChild(child, "rest") == null,
                    directChild(child, "grace") != null, duration == null ? 0 : duration.intValue(), levels));
        }
        return timeline;
    }

    private static void applyImplicitBeamsToLaneTimeline(List<BeamTimelineEntry> timeline, int beatDiv) {
        List<BeamEventInfo> events = new ArrayList<BeamEventInfo>();
        for (BeamTimelineEntry entry : timeline) {
            events.add(new BeamEventInfo(entry.timed, entry.chord, entry.grace, entry.durationDiv, entry.levels, ""));
        }
        Map<Integer, BeamAssignment> assignments = computeBeamAssignments(events, beatDiv, true);
        for (Map.Entry<Integer, BeamAssignment> assignmentEntry : assignments.entrySet()) {
            BeamTimelineEntry entry = timeline.get(assignmentEntry.getKey().intValue());
            if (entry.note == null || directChild(entry.note, "beam") != null) {
                continue;
            }
            BeamAssignment assignment = assignmentEntry.getValue();
            for (int level = 1; level <= assignment.getLevels(); level++) {
                appendBeamElement(entry.note, level, assignment.getState());
            }
        }
    }

    private static int beamLevelsFromType(String typeText) {
        String type = trimToEmpty(typeText).toLowerCase(Locale.ROOT);
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
        if ("128th".equals(type)) {
            return 5;
        }
        if ("256th".equals(type)) {
            return 6;
        }
        return 0;
    }

    public static String buildMusicXmlBeamItemsXml(String state, Object levels) {
        return buildMusicXmlBeamItemsXml(new BeamAssignment(state, positiveRoundedInt(levels)));
    }

    public static String buildMusicXmlBeamItemsXml(BeamAssignment assignment) {
        if (assignment == null) {
            return "";
        }
        int count = assignment.getLevels();
        if (count <= 0) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        for (int level = 1; level <= count; level++) {
            xml.append("<beam number=\"").append(level).append("\">").append(xmlEscape(assignment.getState()))
                    .append("</beam>");
        }
        return xml.toString();
    }

    /**
     * Java counterpart of beam-common.ts computeBeamAssignments.  MusicXML and
     * ABC both call this routine so their implicit and explicit beam boundaries
     * stay aligned.
     */
    public static Map<Integer, BeamAssignment> computeBeamAssignments(List<BeamEventInfo> events, double beatDiv,
            boolean splitAtBeatBoundaryWhenImplicit) {
        Map<Integer, BeamAssignment> assignmentByIndex = new LinkedHashMap<Integer, BeamAssignment>();
        if (events == null) {
            return assignmentByIndex;
        }
        boolean hasExplicitBeamMode = false;
        for (BeamEventInfo info : events) {
            if (info != null && info.isTimed()
                    && ("begin".equals(info.getExplicitMode()) || "mid".equals(info.getExplicitMode()))) {
                hasExplicitBeamMode = true;
                break;
            }
        }
        int resolvedBeatDiv = Math.max(1, (int) Math.round(beatDiv));
        if (!hasExplicitBeamMode) {
            List<Integer> currentGroup = new ArrayList<Integer>();
            int cursorDiv = 0;
            for (int index = 0; index < events.size(); index++) {
                BeamEventInfo info = events.get(index);
                if (splitAtBeatBoundaryWhenImplicit && info != null && info.isTimed()
                        && cursorDiv > 0 && cursorDiv % resolvedBeatDiv == 0) {
                    flushBeamAssignments(events, currentGroup, assignmentByIndex);
                    currentGroup.clear();
                }
                if (info == null || !info.isChord() || !isBeamableTimedEvent(info)) {
                    flushBeamAssignments(events, currentGroup, assignmentByIndex);
                    currentGroup.clear();
                    if (info != null && info.isTimed()) {
                        cursorDiv += Math.max(0, info.getDurationDiv());
                    }
                    continue;
                }
                currentGroup.add(Integer.valueOf(index));
                cursorDiv += Math.max(0, info.getDurationDiv());
            }
            flushBeamAssignments(events, currentGroup, assignmentByIndex);
            return assignmentByIndex;
        }

        List<Integer> activeGroup = new ArrayList<Integer>();
        int cursorDiv = 0;
        for (int index = 0; index < events.size(); index++) {
            BeamEventInfo info = events.get(index);
            if (info == null || !info.isTimed()) {
                flushBeamAssignments(events, activeGroup, assignmentByIndex);
                activeGroup.clear();
                continue;
            }
            if (cursorDiv > 0 && cursorDiv % resolvedBeatDiv == 0) {
                flushBeamAssignments(events, activeGroup, assignmentByIndex);
                activeGroup.clear();
            }
            if (!isBeamableTimedEvent(info)) {
                flushBeamAssignments(events, activeGroup, assignmentByIndex);
                activeGroup.clear();
                continue;
            }
            if ("begin".equals(info.getExplicitMode())) {
                flushBeamAssignments(events, activeGroup, assignmentByIndex);
                activeGroup.clear();
                activeGroup.add(Integer.valueOf(index));
            } else if ("mid".equals(info.getExplicitMode())) {
                if (activeGroup.isEmpty()) {
                    BeamEventInfo previous = index > 0 ? events.get(index - 1) : null;
                    if (isBeamableTimedEvent(previous)) {
                        activeGroup.add(Integer.valueOf(index - 1));
                    }
                }
                activeGroup.add(Integer.valueOf(index));
            } else {
                activeGroup.add(Integer.valueOf(index));
            }
            cursorDiv += Math.max(0, info.getDurationDiv());
        }
        flushBeamAssignments(events, activeGroup, assignmentByIndex);
        return assignmentByIndex;
    }

    private static boolean isBeamableTimedEvent(BeamEventInfo info) {
        return info != null && info.isTimed() && !info.isGrace() && info.getLevels() > 0;
    }

    private static void flushBeamAssignments(List<BeamEventInfo> infos, List<Integer> indices,
            Map<Integer, BeamAssignment> assignmentByIndex) {
        List<Integer> chordIndices = new ArrayList<Integer>();
        for (Integer index : indices) {
            BeamEventInfo info = infos.get(index.intValue());
            if (info != null && info.isChord() && !info.isGrace()) {
                chordIndices.add(index);
            }
        }
        if (chordIndices.size() < 2) {
            return;
        }
        for (int groupIndex = 0; groupIndex < chordIndices.size(); groupIndex++) {
            int index = chordIndices.get(groupIndex).intValue();
            BeamEventInfo info = infos.get(index);
            if (info == null || info.getLevels() <= 0) {
                continue;
            }
            String state = groupIndex == 0 ? "begin" : (groupIndex == chordIndices.size() - 1 ? "end" : "continue");
            assignmentByIndex.put(Integer.valueOf(index), new BeamAssignment(state, info.getLevels()));
        }
    }

    private static void appendBeamElement(Element note, int number, String state) {
        Element beam = note.getOwnerDocument().createElement("beam");
        beam.setAttribute("number", Integer.toString(number));
        beam.setTextContent(state);
        Element before = firstDirectChildNamedAny(note, new String[] { "notations", "lyric", "play", "listen", "sound" });
        if (before != null) {
            note.insertBefore(beam, before);
        } else {
            note.appendChild(beam);
        }
    }

    private static void enrichTupletNotationsInDocument(Document doc) {
        if (doc == null) {
            return;
        }
        // musicxml-io.ts uses document.querySelectorAll("part > measure")
        // here (unlike its score-partwise-scoped part-list and barline
        // helpers), so intentionally include wrapped standalone parts too.
        org.w3c.dom.NodeList parts = doc.getElementsByTagName("part");
        for (int index = 0; index < parts.getLength(); index++) {
            if (!(parts.item(index) instanceof Element)) {
                continue;
            }
            Element part = (Element) parts.item(index);
            for (Element measure : directChildren(part, "measure")) {
                enrichTupletNotationsInMeasure(measure);
            }
        }
    }

    private static void enrichTupletNotationsInMeasure(Element measure) {
        Map<String, TupletGroup> activeByLane = new HashMap<String, TupletGroup>();
        Map<String, Integer> nextTupletNoByLane = new HashMap<String, Integer>();
        for (Element child : directElementChildren(measure)) {
            String tagName = child.getTagName();
            if ("backup".equals(tagName) || "forward".equals(tagName)) {
                flushAllTupletLanes(activeByLane, nextTupletNoByLane);
                continue;
            }
            if (!"note".equals(tagName)) {
                continue;
            }
            if (directChild(child, "chord") != null) {
                continue;
            }
            String lane = noteLaneKey(child);
            String signature = tupletSignatureForNote(child);
            TupletGroup current = activeByLane.get(lane);
            if (signature == null) {
                flushTupletLane(lane, activeByLane, nextTupletNoByLane);
                continue;
            }
            if (current == null || !current.signature.equals(signature)) {
                flushTupletLane(lane, activeByLane, nextTupletNoByLane);
                current = new TupletGroup(signature);
                activeByLane.put(lane, current);
            }
            current.notes.add(child);
        }
        flushAllTupletLanes(activeByLane, nextTupletNoByLane);
    }

    private static void flushAllTupletLanes(Map<String, TupletGroup> activeByLane,
            Map<String, Integer> nextTupletNoByLane) {
        List<String> lanes = new ArrayList<String>(activeByLane.keySet());
        for (String lane : lanes) {
            flushTupletLane(lane, activeByLane, nextTupletNoByLane);
        }
    }

    private static void flushTupletLane(String lane, Map<String, TupletGroup> activeByLane,
            Map<String, Integer> nextTupletNoByLane) {
        TupletGroup group = activeByLane.remove(lane);
        if (group == null || group.notes.size() < 2) {
            return;
        }
        Integer current = nextTupletNoByLane.get(lane);
        int number = current == null ? 1 : current.intValue();
        nextTupletNoByLane.put(lane, Integer.valueOf(number + 1));
        ensureTupletNotation(group.notes.get(0), "start", number, true);
        ensureTupletNotation(group.notes.get(group.notes.size() - 1), "stop", number, false);
    }

    private static void ensureTupletNotation(Element note, String type, int number, boolean withDisplayAttrs) {
        Element notations = directChild(note, "notations");
        if (notations == null) {
            notations = note.getOwnerDocument().createElement("notations");
            note.appendChild(notations);
        }
        Element existing = directTupletByType(notations, type);
        if (existing != null) {
            if (existing.getAttribute("number").isEmpty()) {
                existing.setAttribute("number", Integer.toString(number));
            }
            if ("start".equals(type) && withDisplayAttrs) {
                if (existing.getAttribute("bracket").isEmpty()) {
                    existing.setAttribute("bracket", "yes");
                }
                if (existing.getAttribute("show-number").isEmpty()) {
                    existing.setAttribute("show-number", "actual");
                }
            }
            return;
        }
        Element tuplet = note.getOwnerDocument().createElement("tuplet");
        tuplet.setAttribute("type", type);
        tuplet.setAttribute("number", Integer.toString(number));
        if ("start".equals(type) && withDisplayAttrs) {
            tuplet.setAttribute("bracket", "yes");
            tuplet.setAttribute("show-number", "actual");
        }
        notations.appendChild(tuplet);
    }

    private static Element directTupletByType(Element notations, String type) {
        for (Element tuplet : directChildren(notations, "tuplet")) {
            if (type.equals(tuplet.getAttribute("type"))) {
                return tuplet;
            }
        }
        return null;
    }

    private static String noteLaneKey(Element note) {
        Element voiceElement = directChild(note, "voice");
        Element staffElement = directChild(note, "staff");
        // Optional chaining plus nullish coalescing in musicxml-io.ts uses the
        // default only when the element is absent.  An existing empty element
        // remains an empty lane token.
        String voice = voiceElement == null ? "1" : trimToEmpty(voiceElement.getTextContent());
        String staff = staffElement == null ? "1" : trimToEmpty(staffElement.getTextContent());
        return voice + "::" + staff;
    }

    private static String tupletSignatureForNote(Element note) {
        Element timeModification = directChild(note, "time-modification");
        if (timeModification == null) {
            return null;
        }
        Long actual = positiveRoundedJavaScriptNumber(directChildText(timeModification, "actual-notes"));
        Long normal = positiveRoundedJavaScriptNumber(directChildText(timeModification, "normal-notes"));
        if (actual == null || normal == null) {
            return null;
        }
        return actual.toString() + "/" + normal.toString();
    }

    private static void normalizePartListAndPartIds(Document doc) {
        Element root = scorePartwiseRoot(doc);
        if (root == null) {
            return;
        }
        List<Element> parts = directChildren(root, "part");
        if (parts.isEmpty()) {
            return;
        }
        normalizeTopLevelPartIds(parts);
        Element partList = ensurePartListElement(root, parts.get(0));
        if (partList == null) {
            return;
        }
        ensureScorePartEntriesForParts(partList, parts);
    }

    private static void normalizeTopLevelPartIds(List<Element> parts) {
        Set<String> usedIds = new HashSet<String>();
        int seq = 1;
        for (Element part : parts) {
            String current = trimToEmpty(part.getAttribute("id"));
            if (!current.isEmpty() && !usedIds.contains(current)) {
                usedIds.add(current);
                continue;
            }
            while (usedIds.contains("P" + seq)) {
                seq++;
            }
            String assigned = "P" + seq;
            seq++;
            part.setAttribute("id", assigned);
            usedIds.add(assigned);
        }
    }

    private static Element ensurePartListElement(Element root, Element firstPart) {
        Element partList = directChild(root, "part-list");
        if (partList != null) {
            return partList;
        }
        partList = root.getOwnerDocument().createElement("part-list");
        root.insertBefore(partList, firstPart);
        return partList;
    }

    private static void ensureScorePartEntriesForParts(Element partList, List<Element> parts) {
        Set<String> scorePartIds = new HashSet<String>();
        for (Element scorePart : directChildren(partList, "score-part")) {
            String id = trimToEmpty(scorePart.getAttribute("id"));
            if (id.isEmpty() || scorePartIds.contains(id)) {
                continue;
            }
            scorePartIds.add(id);
            ensurePartNameElement(scorePart);
        }
        for (Element part : parts) {
            String id = trimToEmpty(part.getAttribute("id"));
            if (id.isEmpty() || scorePartIds.contains(id)) {
                continue;
            }
            Element scorePart = partList.getOwnerDocument().createElement("score-part");
            scorePart.setAttribute("id", id);
            ensurePartNameElement(scorePart);
            partList.appendChild(scorePart);
            scorePartIds.add(id);
        }
    }

    private static void ensurePartNameElement(Element scorePart) {
        if (directChild(scorePart, "part-name") != null) {
            return;
        }
        Element partName = scorePart.getOwnerDocument().createElement("part-name");
        partName.setTextContent("Music");
        scorePart.appendChild(partName);
    }

    private static void ensureFinalBarlineInEachPart(Document doc) {
        Element root = scorePartwiseRoot(doc);
        if (root == null) {
            return;
        }
        for (Element part : directChildren(root, "part")) {
            ensureFinalBarlineInPart(part);
        }
    }

    private static void ensureFinalBarlineInPart(Element part) {
        List<Element> measures = directChildren(part, "measure");
        if (measures.isEmpty()) {
            return;
        }
        Element lastMeasure = measures.get(measures.size() - 1);
        for (Element barline : directChildren(lastMeasure, "barline")) {
            if ("right".equals(barline.getAttribute("location"))) {
                return;
            }
        }
        Element barline = part.getOwnerDocument().createElement("barline");
        barline.setAttribute("location", "right");
        Element barStyle = part.getOwnerDocument().createElement("bar-style");
        barStyle.setTextContent("light-heavy");
        barline.appendChild(barStyle);
        lastMeasure.appendChild(barline);
    }

    private static Element scorePartwiseRoot(Document doc) {
        if (doc == null) {
            return null;
        }
        Element root = doc.getDocumentElement();
        if (root != null && "score-partwise".equals(root.getTagName())) {
            return root;
        }
        org.w3c.dom.NodeList candidates = doc.getElementsByTagName("score-partwise");
        for (int index = 0; index < candidates.getLength(); index++) {
            if (candidates.item(index) instanceof Element) {
                return (Element) candidates.item(index);
            }
        }
        return null;
    }

    private static Document cloneXmlDocument(Document doc) {
        Document cloned = createEmptyDocument();
        if (doc != null && doc.getDocumentElement() != null) {
            cloned.appendChild(cloned.importNode(doc.getDocumentElement(), true));
        }
        return cloned;
    }

    private static Document createEmptyDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to create MusicXML document.", ex);
        }
    }

    private static Document createDocumentWithRoot(String rootName) {
        Document doc = createEmptyDocument();
        doc.appendChild(doc.createElement(rootName));
        return doc;
    }

    private static Element findPartById(Document doc, String partId) {
        Element root = scorePartwiseRoot(doc);
        if (root == null) {
            return null;
        }
        for (Element part : directChildren(root, "part")) {
            if (part.getAttribute("id").equals(partId)) {
                return part;
            }
        }
        return null;
    }

    private static Element findScorePartById(Document doc, String partId) {
        Element root = scorePartwiseRoot(doc);
        Element partList = directChild(root, "part-list");
        if (partList == null) {
            return null;
        }
        for (Element scorePart : directChildren(partList, "score-part")) {
            if (scorePart.getAttribute("id").equals(partId)) {
                return scorePart;
            }
        }
        return null;
    }

    private static Element findMeasureByNumber(Element part, String measureNumber) {
        for (Element measure : directChildren(part, "measure")) {
            if (measure.getAttribute("number").equals(measureNumber)) {
                return measure;
            }
        }
        return null;
    }

    private static Element firstPartMeasure(Document doc) {
        if (doc == null) {
            return null;
        }
        // This mirrors document.querySelector("part > measure") rather than
        // assuming that the editor document has a score-partwise root.  The
        // replacement helper intentionally consumes the first such measure
        // even when it is supplied inside a wrapper document.
        org.w3c.dom.NodeList parts = doc.getElementsByTagName("part");
        for (int index = 0; index < parts.getLength(); index++) {
            if (!(parts.item(index) instanceof Element)) {
                continue;
            }
            Element part = (Element) parts.item(index);
            Element measure = directChild(part, "measure");
            if (measure != null) {
                return measure;
            }
        }
        return null;
    }

    private static Element collectEffectiveMeasureAttributes(Element part, Element targetMeasure) {
        Element divisions = null;
        Element key = null;
        Element time = null;
        Element staves = null;
        Map<String, Element> clefByNo = new HashMap<String, Element>();

        for (Element measure : directChildren(part, "measure")) {
            Element attrs = directChild(measure, "attributes");
            if (attrs != null) {
                Element nextDivisions = directChild(attrs, "divisions");
                if (nextDivisions != null) {
                    divisions = (Element) nextDivisions.cloneNode(true);
                }
                Element nextKey = directChild(attrs, "key");
                if (nextKey != null) {
                    key = (Element) nextKey.cloneNode(true);
                }
                Element nextTime = directChild(attrs, "time");
                if (nextTime != null) {
                    time = (Element) nextTime.cloneNode(true);
                }
                Element nextStaves = directChild(attrs, "staves");
                if (nextStaves != null) {
                    staves = (Element) nextStaves.cloneNode(true);
                }
                for (Element clef : directChildren(attrs, "clef")) {
                    String no = clef.hasAttribute("number") ? clef.getAttribute("number") : "1";
                    clefByNo.put(no, (Element) clef.cloneNode(true));
                }
            }
            if (measure == targetMeasure) {
                break;
            }
        }

        Document doc = targetMeasure.getOwnerDocument();
        Element effective = doc.createElement("attributes");
        if (divisions != null) {
            effective.appendChild(divisions);
        }
        if (key != null) {
            effective.appendChild(key);
        }
        if (time != null) {
            effective.appendChild(time);
        }
        if (staves != null) {
            effective.appendChild(staves);
        }
        List<String> clefNos = new ArrayList<String>(clefByNo.keySet());
        Collections.sort(clefNos);
        for (String no : clefNos) {
            effective.appendChild(clefByNo.get(no));
        }
        return directElementChildren(effective).isEmpty() ? null : effective;
    }

    private static void mergeMissingEffectiveAttributes(Element targetAttributes, Element effectiveAttributes) {
        ensureSingleEffectiveAttribute(targetAttributes, effectiveAttributes, "divisions");
        ensureSingleEffectiveAttribute(targetAttributes, effectiveAttributes, "key");
        ensureSingleEffectiveAttribute(targetAttributes, effectiveAttributes, "time");
        ensureSingleEffectiveAttribute(targetAttributes, effectiveAttributes, "staves");

        Set<String> existingClefNos = new HashSet<String>();
        for (Element clef : directChildren(targetAttributes, "clef")) {
            String no = clef.hasAttribute("number") ? clef.getAttribute("number") : "1";
            existingClefNos.add(no);
        }
        for (Element clef : directChildren(effectiveAttributes, "clef")) {
            String no = clef.hasAttribute("number") ? clef.getAttribute("number") : "1";
            if (existingClefNos.contains(no)) {
                continue;
            }
            targetAttributes.appendChild(clef.cloneNode(true));
            existingClefNos.add(no);
        }
    }

    private static void ensureSingleEffectiveAttribute(Element targetAttributes, Element effectiveAttributes,
            String tagName) {
        if (directChild(targetAttributes, tagName) != null) {
            return;
        }
        Element src = directChild(effectiveAttributes, tagName);
        if (src != null) {
            targetAttributes.appendChild(src.cloneNode(true));
        }
    }

    private static List<Element> elementsByTagName(Document doc, String tagName) {
        List<Element> result = new ArrayList<Element>();
        if (doc == null) {
            return result;
        }
        org.w3c.dom.NodeList nodes = doc.getElementsByTagName(tagName);
        for (int index = 0; index < nodes.getLength(); index++) {
            if (nodes.item(index) instanceof Element) {
                result.add((Element) nodes.item(index));
            }
        }
        return result;
    }

    private static Element directChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private static Element firstDirectChildNamedAny(Element parent, String[] tagNames) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                String tagName = ((Element) child).getTagName();
                for (String candidate : tagNames) {
                    if (candidate.equals(tagName)) {
                        return (Element) child;
                    }
                }
            }
            child = child.getNextSibling();
        }
        return null;
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

    private static String directChildText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        return child == null ? null : child.getTextContent();
    }

    private static Integer parsePositiveInteger(String text) {
        Integer value = parseJavaScriptDecimalInteger(text);
        if (value == null || value.intValue() <= 0) {
            return null;
        }
        return value;
    }

    private static Integer parseNonNegativeInteger(String text) {
        Integer value = parseJavaScriptDecimalInteger(text);
        if (value == null || value.intValue() < 0) {
            return null;
        }
        return value;
    }

    /**
     * JavaScript {@code Number.parseInt(text, 10)} for the finite int-sized
     * values accepted by the Java beam timeline.  In particular, parsing
     * stops at the first non-decimal character instead of requiring the full
     * XML text node to be an integer.
     */
    private static Integer parseJavaScriptDecimalInteger(String text) {
        String value = trimToEmpty(text);
        if (value.isEmpty()) {
            return null;
        }
        int index = 0;
        boolean negative = false;
        char first = value.charAt(0);
        if (first == '+' || first == '-') {
            negative = first == '-';
            index++;
        }
        int digitStart = index;
        while (index < value.length() && value.charAt(index) >= '0' && value.charAt(index) <= '9') {
            index++;
        }
        if (index == digitStart) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.substring(digitStart, index));
            if (negative) {
                parsed = -parsed;
            }
            if (parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
                return null;
            }
            return Integer.valueOf((int) parsed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int positiveRoundedInt(Object value) {
        double parsed = toNumber(value);
        if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed <= 0) {
            return 0;
        }
        return (int) Math.round(parsed);
    }

    private static Long positiveRoundedJavaScriptNumber(String text) {
        double parsed = toNumber(text);
        if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed <= 0) {
            return null;
        }
        return Long.valueOf(Math.round(parsed));
    }

    private static double toNumber(Object value) {
        if (value == null) {
            return 0d;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? 1d : 0d;
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) {
            return 0d;
        }
        try {
            if (text.startsWith("0x") || text.startsWith("0X")) {
                return Long.parseLong(text.substring(2), 16);
            }
            if (text.startsWith("0b") || text.startsWith("0B")) {
                return Long.parseLong(text.substring(2), 2);
            }
            if (text.startsWith("0o") || text.startsWith("0O")) {
                return Long.parseLong(text.substring(2), 8);
            }
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String trimToEmpty(String text) {
        return text == null ? "" : trimJavaScriptWhitespace(text);
    }

    private static void setFeatureIfAvailable(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Optional parser hardening varies by JAXP implementation.
        }
    }

    private static final class QuietErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXParseException {
            throw exception;
        }
    }

    private static final class TupletGroup {
        private final String signature;
        private final List<Element> notes = new ArrayList<Element>();

        private TupletGroup(String signature) {
            this.signature = signature;
        }
    }

    private static final class BeamMeasureState {
        private final int divisions;
        private final int beats;
        private final int beatType;

        private BeamMeasureState(int divisions, int beats, int beatType) {
            this.divisions = divisions;
            this.beats = beats;
            this.beatType = beatType;
        }
    }

    private static final class BeamTimelineEntry {
        private final Element note;
        private final boolean timed;
        private final boolean chord;
        private final boolean grace;
        private final int durationDiv;
        private final int levels;

        private BeamTimelineEntry(Element note, boolean timed, boolean chord, boolean grace, int durationDiv, int levels) {
            this.note = note;
            this.timed = timed;
            this.chord = chord;
            this.grace = grace;
            this.durationDiv = durationDiv;
            this.levels = levels;
        }
    }
}
