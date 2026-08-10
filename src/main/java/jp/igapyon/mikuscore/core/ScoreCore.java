package jp.igapyon.mikuscore.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.musicxml.MusicXmlCommandValidation;
import jp.igapyon.mikuscore.musicxml.MusicXmlState;
import jp.igapyon.mikuscore.musicxml.MusicXmlIo;

/**
 * Stateful facade corresponding to the upstream core/ScoreCore lifecycle.
 *
 * <p>Command validation and mutation stay centralised in {@link MusicXmlState}
 * so the CLI and this in-memory API have identical MusicXML semantics.</p>
 */
public final class ScoreCore {
    // Keep this distinct from the historic generic prefix so a vendor attribute
    // named data-mikuscore-java-node-id is never mistaken for Java state.
    private static final String STABLE_NODE_ID_ATTRIBUTE_PREFIX = "data-mikuscore-java-internal-node-id";
    private static final String STABLE_NODE_ID_VALUE_PREFIX = "mksj:";

    private final String editableVoice;
    private String originalXml = "";
    private String currentXml;
    private boolean dirty;
    private String stableNodeIdAttribute;
    private int stableNodeIdCounter;

    /** Creates a core with no voice restriction. */
    public ScoreCore() {
        this(new Options());
    }

    /**
     * Creates a core using the upstream-shaped options boundary.
     *
     * @param options optional construction options; a blank editable voice
     *        permits all voices
     */
    public ScoreCore(Options options) {
        String rawEditableVoice = options == null ? null : options.getEditableVoice();
        this.editableVoice = trimToNull(rawEditableVoice);
    }

    /** Loads a score and establishes its original, clean save state. */
    public void load(String xmlText) {
        // ScoreCore.ts assigns originalXml before parseXml().  If a later load
        // fails, its existing document/IDs/dirty state remain intact but a
        // clean subsequent save returns this newly supplied source text.
        this.originalXml = xmlText == null ? "" : xmlText;
        Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
        if (doc == null) {
            throw new IllegalArgumentException("Invalid XML input.");
        }
        Element root = doc.getDocumentElement();
        if (root == null || !"score-partwise".equals(root.getTagName())) {
            throw new IllegalArgumentException("MusicXML root must be <score-partwise>.");
        }
        this.stableNodeIdAttribute = chooseStableNodeIdAttribute(doc);
        // Node's nodeCounter belongs to the ScoreCore instance, not to a
        // loaded document.  Preserve its monotonic sequence across reloads.
        for (Element note : noteElements(doc)) {
            note.setAttribute(stableNodeIdAttribute, encodeStableNodeId(nextStableNodeId()));
        }
        this.currentXml = MusicXmlIo.serializeMusicXmlDocument(doc);
        this.dirty = false;
    }

    /** Applies one upstream-shaped JSON command to the loaded score. */
    public DispatchResult dispatch(String commandJson) {
        if (currentXml == null) {
            return DispatchResult.failure("MVP_SCORE_NOT_LOADED", "Score is not loaded.");
        }
        MusicXmlCommandValidation validation;
        try {
            MusicXmlCommandValidation.Diagnostic editableVoiceDiagnostic = MusicXmlState
                    .validateMusicXmlCommandEditableVoice(commandJson, editableVoice);
            if (editableVoiceDiagnostic != null) {
                return DispatchResult.failure(editableVoiceDiagnostic.getCode(), editableVoiceDiagnostic.getMessage());
            }
            validation = MusicXmlState.validateMusicXmlCommand(currentXml, commandJson, stableNodeIdAttribute);
        } catch (Exception ex) {
            return DispatchResult.failure("MVP_INVALID_COMMAND_PAYLOAD", ex.getMessage());
        }
        if (!validation.isOk()) {
            if (MusicXmlState.scoreCoreRestoresSnapshotAfterValidationFailure(commandJson, validation)) {
                try {
                    currentXml = restoreStableNodeIdsAfterSourceRollback(currentXml);
                } catch (Exception ignored) {
                    // The already determined validation diagnostic is authoritative.
                }
                return DispatchResult.fromValidation(validation, false);
            }
            // The Node implementation calls ensureVoiceValue() immediately
            // before a few command-local checks.  When one of those checks
            // rejects, the core remains clean and save() returns originalXml,
            // but debugSerializeCurrentXml() still exposes that voice-only
            // in-memory normalization.  Preserve this otherwise subtle public
            // lifecycle behavior without applying any failed score edit.
            try {
                currentXml = MusicXmlState.applyScoreCoreFailureVoiceNormalization(currentXml, commandJson,
                        validation.getDiagnostics(), stableNodeIdAttribute);
            } catch (Exception ignored) {
                // The already determined validation diagnostic is authoritative.
            }
            return DispatchResult.fromValidation(validation, false);
        }

        if (!validation.isDirtyChanged()) {
            return DispatchResult.success(false, Collections.<String>emptyList(), Collections.<String>emptyList(),
                    Collections.<MusicXmlCommandValidation.Warning>emptyList());
        }

        MusicXmlState.MusicXmlCommandApplyResult applied;
        try {
            applied = MusicXmlState.applyMusicXmlCommandWithWarnings(currentXml, commandJson, stableNodeIdAttribute);
        } catch (Exception ex) {
            return DispatchResult.failure("MVP_COMMAND_EXECUTION_FAILED", "Command failed unexpectedly.");
        }
        StableNodeIdNormalization normalization;
        try {
            normalization = normalizeStableNodeIds(applied.getOutput());
        } catch (Exception ex) {
            return DispatchResult.failure("MVP_COMMAND_EXECUTION_FAILED", "Command failed unexpectedly.");
        }
        boolean dirtyChanged = !dirty;
        currentXml = normalization.getXml();
        dirty = true;
        return DispatchResult.success(dirtyChanged,
                changedNodeIdsAfterNormalization(validation.getChangedNodeIds(), normalization.getGeneratedNodeIds()),
                validation.getAffectedMeasureNumbers(), applied.getWarnings());
    }

    /** Validates and serializes the current score, preserving clean input verbatim. */
    public SaveResult save() {
        if (currentXml == null) {
            return SaveResult.failure("original_noop", "MVP_SCORE_NOT_LOADED", "Score is not loaded.");
        }
        MusicXmlCommandValidation validation;
        try {
            validation = MusicXmlState.validateMusicXmlForSave(currentXml, dirty, editableVoice, stableNodeIdAttribute);
        } catch (Exception ex) {
            return SaveResult.failure(dirty ? "serialized_dirty" : "original_noop", "MVP_COMMAND_EXECUTION_FAILED",
                    ex.getMessage());
        }
        if (!validation.isOk()) {
            // Upstream reports a failed integrity check as serialized_dirty even
            // when the document itself has not been edited yet.
            return new SaveResult(false, "serialized_dirty", "",
                    validation.getDiagnostics());
        }
        return new SaveResult(true, dirty ? "serialized_dirty" : "original_noop",
                dirty ? withoutStableNodeIdAttributes(currentXml) : originalXml,
                Collections.<MusicXmlCommandValidation.Diagnostic>emptyList());
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Returns the deterministic node IDs currently addressable by JSON commands. */
    public List<String> listNoteNodeIds() {
        if (currentXml == null) {
            return Collections.emptyList();
        }
        Document doc = MusicXmlIo.parseMusicXmlDocument(currentXml);
        Set<String> ids = new LinkedHashSet<String>();
        for (Element note : noteElements(doc)) {
            String nodeId = decodeStableNodeId(note.getAttribute(stableNodeIdAttribute));
            if (nodeId != null) {
                ids.add(nodeId);
            }
        }
        return Collections.unmodifiableList(new ArrayList<String>(ids));
    }

    /** Returns the in-memory XML regardless of save state, or {@code null} before load. */
    public String debugSerializeCurrentXml() {
        return currentXml == null ? null : withoutStableNodeIdAttributes(currentXml);
    }

    private static String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.length() == 0 ? null : normalized;
    }

    private String nextStableNodeId() {
        stableNodeIdCounter++;
        return "n" + stableNodeIdCounter;
    }

    private String chooseStableNodeIdAttribute(Document doc) {
        String candidate = STABLE_NODE_ID_ATTRIBUTE_PREFIX;
        int suffix = 1;
        // The Node implementation keeps IDs outside the XML tree.  Java uses a
        // temporary attribute only while the score is in memory, so the name
        // must not collide with user/vendor markup anywhere in the document.
        while (hasElementAttribute(doc, candidate)) {
            candidate = STABLE_NODE_ID_ATTRIBUTE_PREFIX + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static boolean hasElementAttribute(Document doc, String attributeName) {
        return doc != null && hasElementAttribute(doc.getDocumentElement(), attributeName);
    }

    private static boolean hasElementAttribute(Node node, String attributeName) {
        if (node instanceof Element && ((Element) node).hasAttribute(attributeName)) {
            return true;
        }
        Node child = node == null ? null : node.getFirstChild();
        while (child != null) {
            if (hasElementAttribute(child, attributeName)) {
                return true;
            }
            child = child.getNextSibling();
        }
        return false;
    }

    private StableNodeIdNormalization normalizeStableNodeIds(String xmlText) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
        if (doc == null) {
            throw new IllegalArgumentException("command did not produce parseable MusicXML.");
        }
        Set<String> retainedIds = new LinkedHashSet<String>();
        List<String> generatedIds = new ArrayList<String>();
        for (Element note : noteElements(doc)) {
            String nodeId = decodeStableNodeId(note.getAttribute(stableNodeIdAttribute));
            if (nodeId == null || retainedIds.contains(nodeId)) {
                nodeId = nextStableNodeId();
                note.setAttribute(stableNodeIdAttribute, encodeStableNodeId(nodeId));
                generatedIds.add(nodeId);
            }
            retainedIds.add(nodeId);
        }
        return new StableNodeIdNormalization(MusicXmlIo.serializeMusicXmlDocument(doc), generatedIds);
    }

    /**
     * ScoreCore.ts restores a serialized snapshot after selected timing failures.
     * Its WeakMap identity is therefore rebuilt and every note receives a fresh
     * monotonically allocated node ID. Java stores those IDs temporarily in XML,
     * so remove them before rebuilding the equivalent restored state.
     */
    private String restoreStableNodeIdsAfterSourceRollback(String xmlText) {
        Document doc = MusicXmlIo.parseMusicXmlDocument(xmlText);
        if (doc == null) {
            throw new IllegalArgumentException("command did not produce parseable MusicXML.");
        }
        for (Element note : noteElements(doc)) {
            note.removeAttribute(stableNodeIdAttribute);
            note.setAttribute(stableNodeIdAttribute, encodeStableNodeId(nextStableNodeId()));
        }
        return MusicXmlIo.serializeMusicXmlDocument(doc);
    }

    private static String encodeStableNodeId(String nodeId) {
        return STABLE_NODE_ID_VALUE_PREFIX + String.valueOf(nodeId == null ? "" : nodeId);
    }

    private static String decodeStableNodeId(String storedValue) {
        String value = trimToNull(storedValue);
        if (value == null || !value.startsWith(STABLE_NODE_ID_VALUE_PREFIX)) {
            return null;
        }
        return trimToNull(value.substring(STABLE_NODE_ID_VALUE_PREFIX.length()));
    }

    private List<String> changedNodeIdsAfterNormalization(List<String> validatedNodeIds,
            List<String> generatedNodeIds) {
        if (validatedNodeIds == null || validatedNodeIds.size() < 2 || generatedNodeIds == null
                || generatedNodeIds.isEmpty()) {
            return validatedNodeIds;
        }
        List<String> changed = new ArrayList<String>();
        changed.add(validatedNodeIds.get(0));
        changed.add(generatedNodeIds.get(0));
        return changed;
    }

    private String withoutStableNodeIdAttributes(String xmlText) {
        if (stableNodeIdAttribute == null || stableNodeIdAttribute.length() == 0) {
            return xmlText;
        }
        return String.valueOf(xmlText).replaceAll("\\s+" + Pattern.quote(stableNodeIdAttribute) + "=\"[^\"]*\"", "");
    }

    private static List<Element> noteElements(Document doc) {
        List<Element> notes = new ArrayList<Element>();
        if (doc != null && doc.getDocumentElement() != null) {
            collectNoteElements(doc.getDocumentElement(), notes);
        }
        return notes;
    }

    private static void collectNoteElements(Node node, List<Element> notes) {
        if (node instanceof Element && "note".equals(((Element) node).getTagName())) {
            notes.add((Element) node);
        }
        Node child = node == null ? null : node.getFirstChild();
        while (child != null) {
            collectNoteElements(child, notes);
            child = child.getNextSibling();
        }
    }

    private static final class StableNodeIdNormalization {
        private final String xml;
        private final List<String> generatedNodeIds;

        private StableNodeIdNormalization(String xml, List<String> generatedNodeIds) {
            this.xml = xml;
            this.generatedNodeIds = Collections.unmodifiableList(new ArrayList<String>(generatedNodeIds));
        }

        private String getXml() {
            return xml;
        }

        private List<String> getGeneratedNodeIds() {
            return generatedNodeIds;
        }
    }

    public static final class DispatchResult {
        private final boolean ok;
        private final boolean dirtyChanged;
        private final List<String> changedNodeIds;
        private final List<String> affectedMeasureNumbers;
        private final List<MusicXmlCommandValidation.Warning> warnings;
        private final List<MusicXmlCommandValidation.Diagnostic> diagnostics;

        private DispatchResult(boolean ok, boolean dirtyChanged, List<String> changedNodeIds,
                List<String> affectedMeasureNumbers, List<MusicXmlCommandValidation.Warning> warnings,
                List<MusicXmlCommandValidation.Diagnostic> diagnostics) {
            this.ok = ok;
            this.dirtyChanged = dirtyChanged;
            this.changedNodeIds = Collections.unmodifiableList(new ArrayList<String>(changedNodeIds));
            this.affectedMeasureNumbers = Collections.unmodifiableList(new ArrayList<String>(affectedMeasureNumbers));
            this.warnings = Collections.unmodifiableList(new ArrayList<MusicXmlCommandValidation.Warning>(warnings));
            this.diagnostics = Collections.unmodifiableList(
                    new ArrayList<MusicXmlCommandValidation.Diagnostic>(diagnostics));
        }

        private static DispatchResult fromValidation(MusicXmlCommandValidation validation, boolean dirtyChanged) {
            return new DispatchResult(validation.isOk(), dirtyChanged, validation.getChangedNodeIds(),
                    validation.getAffectedMeasureNumbers(), validation.getWarnings(), validation.getDiagnostics());
        }

        private static DispatchResult success(boolean dirtyChanged, List<String> changedNodeIds,
                List<String> affectedMeasureNumbers, List<MusicXmlCommandValidation.Warning> warnings) {
            return new DispatchResult(true, dirtyChanged, changedNodeIds, affectedMeasureNumbers, warnings,
                    Collections.<MusicXmlCommandValidation.Diagnostic>emptyList());
        }

        private static DispatchResult failure(String code, String message) {
            return new DispatchResult(false, false, Collections.<String>emptyList(), Collections.<String>emptyList(),
                    Collections.<MusicXmlCommandValidation.Warning>emptyList(), Collections.singletonList(
                            new MusicXmlCommandValidation.Diagnostic(code, message == null ? "" : message)));
        }

        public boolean isOk() {
            return ok;
        }

        public boolean isDirtyChanged() {
            return dirtyChanged;
        }

        public List<String> getChangedNodeIds() {
            return changedNodeIds;
        }

        public List<String> getAffectedMeasureNumbers() {
            return affectedMeasureNumbers;
        }

        public List<MusicXmlCommandValidation.Warning> getWarnings() {
            return warnings;
        }

        public List<MusicXmlCommandValidation.Diagnostic> getDiagnostics() {
            return diagnostics;
        }
    }

    public static final class SaveResult {
        private final boolean ok;
        private final String mode;
        private final String xml;
        private final List<MusicXmlCommandValidation.Diagnostic> diagnostics;

        private SaveResult(boolean ok, String mode, String xml,
                List<MusicXmlCommandValidation.Diagnostic> diagnostics) {
            this.ok = ok;
            this.mode = mode;
            this.xml = xml;
            this.diagnostics = Collections.unmodifiableList(
                    new ArrayList<MusicXmlCommandValidation.Diagnostic>(diagnostics));
        }

        private static SaveResult failure(String mode, String code, String message) {
            return new SaveResult(false, mode, "", Collections.singletonList(
                    new MusicXmlCommandValidation.Diagnostic(code, message == null ? "" : message)));
        }

        public boolean isOk() {
            return ok;
        }

        public String getMode() {
            return mode;
        }

        public String getXml() {
            return xml;
        }

        public List<MusicXmlCommandValidation.Diagnostic> getDiagnostics() {
            return diagnostics;
        }
    }

    /** Immutable construction options corresponding to upstream ScoreCoreOptions. */
    public static final class Options {
        private final String editableVoice;

        public Options() {
            this(null);
        }

        public Options(String editableVoice) {
            this.editableVoice = editableVoice;
        }

        public String getEditableVoice() {
            return editableVoice;
        }
    }
}
