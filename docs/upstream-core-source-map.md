# Pinned upstream Core source map

This map audits the runtime-independent exports from `core/` at renamed Node
upstream revision `a8adc1998237f7b371cae75728afec7dd1795977`. The 54 atomic
unit cases remain in [upstream-core-case-map.md](upstream-core-case-map.md);
this document records source exports and their Java implementation owners so
that source-only behavior is not hidden behind those cases.

| Upstream export | Java owner | Evidence | Status |
| --- | --- | --- | --- |
| `ScoreCore` constructor, `load`, `isDirty`, `listNoteNodeIds`, `debugSerializeCurrentXml` | `core.ScoreCore` | `ScoreCoreTest#preservesLoadDispatchAndSaveLifecycle`, `#reloadKeepsTheNodeIdCounterAndFailedReloadUpdatesOnlyOriginalText`, `#structuralEditsRetainExistingNodeIdsAndDoNotExposeInternalMarkers` | done |
| `ScoreCore.dispatch` result shape and failure path | `core.ScoreCore`, `MusicXmlState` | all `core.spec.ts` cases in `upstream-core-case-map.md`; `ScoreCorePropertyTest#rejectedCommandsKeepStateAndChangedIdsUnchanged`; `ScoreCoreTest#postEnsureFailuresRetainOnlyTheUpstreamDebugVoiceNormalization`, `#timingRollbackReindexesNodeIdsLikeTheUpstreamWeakMapRestore` | done; post-`ensureVoiceValue` failures retain their source-visible debug-only mutation, while snapshot-restored timing failures rebuild WeakMap-style node IDs |
| `ScoreCore.save` integrity modes and diagnostics | `core.ScoreCore`, `MusicXmlState` | `ScoreCoreTest#cleanSaveIntegrityFailureUsesTheUpstreamSerializedDirtyModeAndContext`, `MusicXmlStateTest#validatesSaveIntegrityForInvalidUpstreamFixtures` | done |
| `isUiOnlyCommand`, `getCommandNodeId` | `MusicXmlState.validateMusicXmlCommand` | `ScoreCoreTest#uiNoopKeepsACleanCoreInOriginalNoopMode`, `#unknownRuntimeCommandUsesTheUpstreamMissingTargetContract` | covered |
| `validateVoice` | `MusicXmlState.validateMusicXmlCommandEditableVoice` | `ScoreCoreTest#editableVoiceRejectsOtherVoiceAndAllowsTheConfiguredVoice`, `#editableVoiceMissingCommandFieldUsesUndefinedDiagnostic` | covered |
| `validateCommandPayload` | `MusicXmlState.validateMusicXmlCommand` | `MusicXmlStateTest#invalidDurationPayloadIsRejectedWithoutChangedTargets`, `#invalidPitchPayloadIsRejectedWithoutChangedTargets` | done |
| `validateSupportedNoteKind`, `validateTargetVoiceMatch` | `MusicXmlState.validateMusicXmlCommand` | `MusicXmlStateTest#deleteRestTargetReportsNoChangedTargets`, `#insertAnchorVoiceMismatchReportsNoChangedTargets`, `ScoreCoreTest#nodeIdCommandWithoutVoiceIsRejectedAgainstTheTargetVoice` | done |
| `validateInsertLaneBoundary`, `validateBackupForwardBoundaryForStructuralEdit` | `MusicXmlState` structural validators | `MusicXmlStateTest#insertCrossingInterleavedVoiceLaneReportsNoChangedTargets`, `#rejectsInsertAcrossBackupForwardBoundary`, `#splitImmediatelyBeforeBackupBoundaryIsAllowed`, `#rejectsSplitAcrossForwardBoundary` | done |
| `validateProjectedMeasureTiming` | `MusicXmlState` timing validation and warning builder | `MusicXmlStateTest#rejectsOverfullChangeDuration`, `#rejectsOverfullInsertNoteAfter`, `#shorteningDurationAutoFillsTrailingRest` | done |
| `parseXml`, `serializeXml`, `reindexNodeIds` | `ScoreCore`, `MusicXmlIo` | `ScoreCoreTest#rejectsInvalidXmlAndNonScorePartwiseRootOnLoad`, `#editsNamespacedMusicXmlWithoutDroppingUnknownMarkup`, `#statefulCoreResolvesDocumentOrderNoteIdsInsideVendorMarkup` | done; stateful node-ID targeting follows the source document-order `querySelectorAll("note")` scope |
| `getVoiceText`, `ensureVoiceValue` | `MusicXmlState` direct-child helpers | `ScoreCoreTest#nodeIdCommandWithoutVoiceIsRejectedAgainstTheTargetVoice`, `#usesJavaScriptNumberStringificationWhenFillingAMissingTargetVoice` | covered |
| `getDurationValue`, `setDurationValue`, `getDurationNotationHint` | `MusicXmlState` number/notation helpers | `MusicXmlStateTest#appliesChangeDurationCommandAndUpdatesSimpleNotation`, `#appliesDottedChangeDurationNotation`, `#durationNotationAndTimingRetainFiniteValuesBeyondTheJavaIntRange`, `ScoreCoreTest#cleanSaveAcceptsFiniteJavaScriptRadixDurationsBeyondLongRange`, `#splitAcceptsFiniteSourceDurationsBeyondTheJavaIntRange`, `#commandDurationsUseTheFiniteJavaScriptIntegerRange` | covered; accepts finite `Number()` hexadecimal, binary, and octal values beyond signed 64-bit range, including source and command durations and effective divisions outside Java `int` |
| `setPitch`, `isUnsupportedNoteKind` | `MusicXmlState` mutation/validation helpers | `MusicXmlStateTest#appliesChangeToPitchCommand`, `#changeToLowPitchAutoAssignsGrandStaffTwo`, `#deleteRestTargetReportsNoChangedTargets`, `ScoreCoreTest#commandPitchOctaveUsesTheFiniteJavaScriptIntegerRangeAndStaffThreshold` | covered; finite command octave values outside Java `int` retain Node's XML stringification and grand-staff threshold comparison |
| `createNoteElement`, `createRestElement`, `replaceWithRestNote` | `MusicXmlState` mutation helpers | `MusicXmlStateTest#appliesInsertNoteAfterCommand`, `#appliesDeleteNoteCommandAsSameDurationRest`, `#deleteReplacesOnlyTargetNoteAndKeepsMeasureAttributes` | done |
| `findAncestorMeasure`, `measureHasBackupOrForward` | `MusicXmlState` DOM/timing helpers | `MusicXmlStateTest#voiceLaneTimingUsesGlobalBackupForwardCursorAndSkipsChordTones`, `#rejectsInsertAcrossBackupForwardBoundary` | covered |

All public Core source exports are complete. The atomic command/result audit
is recorded in [upstream-core-contract-source-map.md](upstream-core-contract-source-map.md);
the final cross-format corpus audit remains a repository-wide closeout task.
