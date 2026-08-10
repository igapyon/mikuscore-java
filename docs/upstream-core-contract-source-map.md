# Pinned upstream Core contract source map

This map completes the command/result contract audit for the renamed upstream
`../miku-score` at `v0.6.1`
(`a8adc1998237f7b371cae75728afec7dd1795977`). The 54 pinned unit
cases are named individually in
[`upstream-core-case-map.md`](upstream-core-case-map.md), and helper
owners are listed in
[`upstream-core-source-map.md`](upstream-core-source-map.md).

| Upstream contract | Java owner | Focused evidence | Status |
| --- | --- | --- | --- |
| `ScoreCoreOptions.editableVoice` normalization and editable-voice diagnostics | `ScoreCore.Options`, `MusicXmlState.validateMusicXmlCommandEditableVoice` | `ScoreCoreTest#editableVoiceRejectsOtherVoiceAndAllowsTheConfiguredVoice`, `#editableVoiceMissingCommandFieldUsesTheUpstreamUndefinedDiagnostic` | done |
| `CoreCommand` discriminator, target/anchor selection, and UI no-op result | `MusicXmlState` command validation | `ScoreCoreTest#uiNoopKeepsACleanCoreInOriginalNoopMode`, `#dispatchDoesNotTreatANestedUiNoopDiscriminatorAsAUiCommand`, `#unknownRuntimeCommandUsesTheUpstreamMissingTargetContract` | done |
| Unchecked JSON command values: target truthiness, non-string lookup, voice DOM conversion, and optional fields | `MusicXmlCommandJson`, `MusicXmlState` | `ScoreCoreTest#nonStringCommandTargetsUseNodeTruthinessWithoutStringKeyCoercion`, `#usesJavaScriptNumberStringificationWhenFillingAMissingTargetVoice`, `MusicXmlStateTest#missingCommandVoiceKeepsTheRawUndefinedValueForDurationTiming` | done |
| `DispatchResult`: success/failure fields, dirty transition, changed IDs, affected measure order, diagnostics, and warnings | `ScoreCore.DispatchResult` | `ScoreCoreTest#successfulDispatchOnAnAlreadyDirtyCoreKeepsTheFullResultShape`, all rows in `upstream-core-case-map.md` | done |
| `SaveResult`: unloaded, clean original, dirty serialized, and integrity-failure modes | `ScoreCore.SaveResult` | `ScoreCoreTest#preservesLoadDispatchAndSaveLifecycle`, `#cleanSaveIntegrityFailureUsesTheUpstreamSerializedDirtyModeAndContext`, `MusicXmlStateTest#validatesSaveIntegrityForInvalidUpstreamFixtures` | done |
| Failure lifecycle: no-op validation, post-`ensureVoiceValue` normalization, snapshot rollback, and node-ID sequencing | `ScoreCore`, `MusicXmlState` | `ScoreCoreTest#postEnsureFailuresRetainOnlyTheUpstreamDebugVoiceNormalization`, `#timingRollbackReindexesNodeIdsLikeTheUpstreamWeakMapRestore`, `#reloadKeepsTheNodeIdCounterAndFailedReloadUpdatesOnlyOriginalText` | done |
| Validator diagnostics for payload, note kind, voice/lane, structural boundaries, and projected timing | `MusicXmlState` | all validator rows in `upstream-core-case-map.md`; `MusicXmlStateTest#timingContextInheritsPastBlankFieldsInsteadOfTreatingThemAsZero` | done |

All public Core source exports, interface fields, command variants, result
fields, and pinned unit/property evidence now have named Java owners. The
source-audit rows `CORE-01`, `CORE-02`, `CORE-03`, and
`CORE-05` are complete; final cross-format corpus audit still applies to
all done rows.
