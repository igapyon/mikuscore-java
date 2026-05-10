/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.musicxml;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    public static Document parseMusicXmlDocument(String xmlText) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            setFeatureIfAvailable(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setFeatureIfAvailable(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureIfAvailable(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureIfAvailable(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            setFeatureIfAvailable(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
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
        String compact = String.valueOf(xml == null ? "" : xml).replaceAll(">\\s+<", "><").trim();
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
        Map<String, String> map = new HashMap<String, String>();
        if (nodeIds == null || nodeIds.isEmpty()) {
            return new RenderDocBundle(sourceDoc, map, 0);
        }
        Document doc = cloneXmlDocument(sourceDoc);
        List<Element> notes = elementsByTagName(doc, "note");
        int count = Math.min(notes.size(), nodeIds.size());
        for (int index = 0; index < count; index++) {
            String nodeId = nodeIds.get(index);
            String svgId = String.valueOf(idPrefix == null ? "" : idPrefix) + "-" + nodeId;
            Element note = notes.get(index);
            note.setAttribute("xml:id", svgId);
            note.setAttribute("id", svgId);
            map.put(svgId, nodeId);
        }
        return new RenderDocBundle(doc, map, count);
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
        String version = trimToEmpty(srcRoot.getAttribute("version"));
        if (!version.isEmpty()) {
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
        if (timeline.size() < 2) {
            return;
        }
        List<Integer> currentGroup = new ArrayList<Integer>();
        int cursorDiv = 0;
        for (int index = 0; index < timeline.size(); index++) {
            BeamTimelineEntry entry = timeline.get(index);
            if (entry.timed && cursorDiv > 0 && cursorDiv % Math.max(1, beatDiv) == 0) {
                flushBeamGroup(timeline, currentGroup);
                currentGroup.clear();
            }
            if (!entry.chord || !isBeamableTimedEvent(entry)) {
                flushBeamGroup(timeline, currentGroup);
                currentGroup.clear();
                if (entry.timed) {
                    cursorDiv += Math.max(0, entry.durationDiv);
                }
                continue;
            }
            currentGroup.add(Integer.valueOf(index));
            if (entry.timed) {
                cursorDiv += Math.max(0, entry.durationDiv);
            }
        }
        flushBeamGroup(timeline, currentGroup);
    }

    private static boolean isBeamableTimedEvent(BeamTimelineEntry entry) {
        return entry != null && entry.timed && !entry.grace && entry.levels > 0;
    }

    private static void flushBeamGroup(List<BeamTimelineEntry> timeline, List<Integer> indices) {
        List<Integer> chordIndices = new ArrayList<Integer>();
        for (Integer index : indices) {
            BeamTimelineEntry entry = timeline.get(index.intValue());
            if (entry.chord && !entry.grace) {
                chordIndices.add(index);
            }
        }
        if (chordIndices.size() < 2) {
            return;
        }
        for (int groupIndex = 0; groupIndex < chordIndices.size(); groupIndex++) {
            BeamTimelineEntry entry = timeline.get(chordIndices.get(groupIndex).intValue());
            if (entry.note == null || entry.levels <= 0 || directChild(entry.note, "beam") != null) {
                continue;
            }
            String state = groupIndex == 0 ? "begin" : (groupIndex == chordIndices.size() - 1 ? "end" : "continue");
            for (int level = 1; level <= entry.levels; level++) {
                appendBeamElement(entry.note, level, state);
            }
        }
    }

    private static int beamLevelsFromType(String typeText) {
        String type = trimToEmpty(typeText).toLowerCase();
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
        Element root = scorePartwiseRoot(doc);
        if (root == null) {
            return;
        }
        for (Element part : directChildren(root, "part")) {
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
            if (trimToEmpty(existing.getAttribute("number")).isEmpty()) {
                existing.setAttribute("number", Integer.toString(number));
            }
            if ("start".equals(type) && withDisplayAttrs) {
                if (trimToEmpty(existing.getAttribute("bracket")).isEmpty()) {
                    existing.setAttribute("bracket", "yes");
                }
                if (trimToEmpty(existing.getAttribute("show-number")).isEmpty()) {
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
            if (type.equals(trimToEmpty(tuplet.getAttribute("type")))) {
                return tuplet;
            }
        }
        return null;
    }

    private static String noteLaneKey(Element note) {
        String voice = trimToEmpty(directChildText(note, "voice"));
        String staff = trimToEmpty(directChildText(note, "staff"));
        if (voice.isEmpty()) {
            voice = "1";
        }
        if (staff.isEmpty()) {
            staff = "1";
        }
        return voice + "::" + staff;
    }

    private static String tupletSignatureForNote(Element note) {
        Element timeModification = directChild(note, "time-modification");
        if (timeModification == null) {
            return null;
        }
        Integer actual = parsePositiveInteger(directChildText(timeModification, "actual-notes"));
        Integer normal = parsePositiveInteger(directChildText(timeModification, "normal-notes"));
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
            if (!id.isEmpty()) {
                scorePartIds.add(id);
                ensurePartNameElement(scorePart);
            }
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
            if ("right".equals(trimToEmpty(barline.getAttribute("location")))) {
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
        Element root = doc == null ? null : doc.getDocumentElement();
        if (root == null || !"score-partwise".equals(root.getTagName())) {
            return null;
        }
        return root;
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
            if (trimToEmpty(part.getAttribute("id")).equals(String.valueOf(partId == null ? "" : partId))) {
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
            if (trimToEmpty(scorePart.getAttribute("id")).equals(String.valueOf(partId == null ? "" : partId))) {
                return scorePart;
            }
        }
        return null;
    }

    private static Element findMeasureByNumber(Element part, String measureNumber) {
        for (Element measure : directChildren(part, "measure")) {
            if (trimToEmpty(measure.getAttribute("number")).equals(String.valueOf(measureNumber == null ? "" : measureNumber))) {
                return measure;
            }
        }
        return null;
    }

    private static Element firstPartMeasure(Document doc) {
        Element root = scorePartwiseRoot(doc);
        if (root == null) {
            return null;
        }
        for (Element part : directChildren(root, "part")) {
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
                    String no = trimToEmpty(clef.getAttribute("number"));
                    clefByNo.put(no.isEmpty() ? "1" : no, (Element) clef.cloneNode(true));
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
            String no = trimToEmpty(clef.getAttribute("number"));
            existingClefNos.add(no.isEmpty() ? "1" : no);
        }
        for (Element clef : directChildren(effectiveAttributes, "clef")) {
            String no = trimToEmpty(clef.getAttribute("number"));
            no = no.isEmpty() ? "1" : no;
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
        try {
            int value = Integer.parseInt(trimToEmpty(text));
            if (value <= 0) {
                return null;
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer parseNonNegativeInteger(String text) {
        try {
            int value = Integer.parseInt(trimToEmpty(text));
            if (value < 0) {
                return null;
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String trimToEmpty(String text) {
        return text == null ? "" : text.trim();
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
