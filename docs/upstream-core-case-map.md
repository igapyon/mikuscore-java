# Pinned upstream Core case map

This is the atomic mapping for the 54 included cases in
`tests/unit/core.spec.ts` from the renamed Node upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`. Each entry names at least one
focused Java regression. It records semantic equivalence; it does not require
Java XML serialization to reproduce browser DOM formatting byte-for-byte.

| Upstream case | Java regression |
| --- | --- |
| RT-0 no-op save returns original text | `ScoreCoreTest#preservesLoadDispatchAndSaveLifecycle` |
| RT-1 pitch change returns serialized output | `ScoreCoreTest#preservesLoadDispatchAndSaveLifecycle` |
| RT-1a pitch change converts rest note into pitched note | `MusicXmlStateTest#appliesChangeToPitchCommand` |
| RT-1a2 pitch-down assigns staff 2 | `MusicXmlStateTest#changeToLowPitchAutoAssignsGrandStaffTwo` |
| RT-1a3 pitch-up assigns staff 1 | `MusicXmlStateTest#changeToHighPitchAutoAssignsGrandStaffOne` |
| RT-1b duration updates simple note type | `MusicXmlStateTest#appliesChangeDurationCommandAndUpdatesSimpleNotation` |
| RT-1c duration updates dotted/triplet metadata | `MusicXmlStateTest#appliesDottedChangeDurationNotation` |
| RT-1d triplet duration without context is rejected | `MusicXmlStateTest#rejectsTripletChangeDurationWithoutTupletContext` |
| RT-1e split divides a note into two equal durations | `MusicXmlStateTest#appliesSplitNoteCommand` |
| RT-1f split rejects odd duration | `MusicXmlStateTest#rejectsSplitNoteForOddDuration` |
| DR-1 UI-only command does not set dirty | `ScoreCoreTest#uiNoopKeepsACleanCoreInOriginalNoopMode` |
| TI-1 overfull duration change is rejected | `MusicXmlStateTest#rejectsOverfullChangeDuration` |
| TI-2 delete replaces with same-duration rest | `MusicXmlStateTest#appliesDeleteNoteCommandAsSameDurationRest` |
| TI-3 inherited attributes enforce capacity | `ScoreCoreTest#rejectsOverfullDurationUsingInheritedAttributesFromThePreviousMeasure` |
| TI-4 updated divisions with inherited time enforce capacity | `ScoreCoreTest#rejectsOverfullDurationUsingUpdatedDivisionsAndInheritedTime` |
| TI-5 updated time with inherited divisions enforces capacity | `ScoreCoreTest#rejectsOverfullDurationUsingUpdatedTimeAndInheritedDivisions` |
| TI-6 chord tones do not advance occupied time | `MusicXmlStateTest#voiceLaneTimingUsesGlobalBackupForwardCursorAndSkipsChordTones` |
| TI-8 deleting chord head promotes next chord tone | `MusicXmlStateTest#appliesDeleteChordHeadByPromotingNextChordTone` |
| TI-9 expanding duration consumes following rest | `MusicXmlStateTest#extendingDurationConsumesFollowingRestInSameVoice` |
| TI-10 shortening duration fills a trailing rest | `MusicXmlStateTest#shorteningDurationAutoFillsTrailingRest` |
| IN-2 overfull insertion is rejected | `MusicXmlStateTest#rejectsOverfullInsertNoteAfter` |
| IN-1 insertion succeeds on matching voice | `MusicXmlStateTest#appliesInsertNoteAfterCommand` |
| ID-1 existing IDs remain stable after insertion | `ScoreCoreTest#structuralEditsRetainExistingNodeIdsAndDoNotExposeInternalMarkers` |
| MP-1 insertion only changes the local position | `MusicXmlStateTest#insertKeepsExistingNotesStableExceptLocalInsertion` |
| BF-1 change command rejects target voice mismatch | `ScoreCoreTest#rejectsChangeCommandWhenTargetVoiceDoesNotMatchCommandVoice` |
| BF-1a matching non-primary voice is editable | `MusicXmlStateTest#editsNonPrimaryVoiceWhenCommandVoiceMatchesTarget` |
| BF-1b voice-2 duration edit leaves voice 1 intact | `MusicXmlStateTest#changingDurationInVoiceTwoDoesNotMutateVoiceOneNotes` |
| BF-3 insertion rejects anchor voice mismatch | `MusicXmlStateTest#insertAnchorVoiceMismatchReportsNoChangedTargets` |
| BF-4 insertion rejects an interleaved voice lane | `MusicXmlStateTest#insertCrossingInterleavedVoiceLaneReportsNoChangedTargets` |
| BF-2 structural insertion rejects backup/forward boundary | `MusicXmlStateTest#rejectsInsertAcrossBackupForwardBoundary` |
| BF-5 insertion away from a boundary is allowed | `MusicXmlStateTest#insertAwayFromBackupForwardBoundaryIsAllowed` |
| BF-6 deletion away from a boundary is allowed | `MusicXmlStateTest#deleteAwayFromBackupForwardBoundaryIsAllowed` |
| BF-7 split immediately before backup is allowed | `MusicXmlStateTest#splitImmediatelyBeforeBackupBoundaryIsAllowed` |
| BF-8 split immediately before forward is rejected | `MusicXmlStateTest#rejectsSplitAcrossForwardBoundary` |
| ID-2 IDs remain stable after delete-to-rest | `MusicXmlStateTest#deleteToRestKeepsGeneratedNodeIdsStable` |
| NK-1 unsupported note kind is rejected | `MusicXmlStateTest#deleteRestTargetReportsNoChangedTargets` |
| PT-1 unknown elements are preserved | `MusicXmlStateTest#changeToPitchPreservesUnknownElements` |
| BM-1 existing beams stay unchanged | `MusicXmlStateTest#changeToPitchPreservesExistingBeamXml` |
| SV-2 save rejects an overfull score | `MusicXmlStateTest#saveIntegrityRejectsOverfullFixture` |
| SV-3 save rejects invalid duration | `MusicXmlStateTest#validatesSaveIntegrityForInvalidUpstreamFixtures` |
| SV-3a save allows a grace note without duration | `MusicXmlStateTest#saveIntegrityAllowsGraceNoteWithoutDuration` |
| SV-3b save allows tuplet integer-rounding tolerance | `ScoreCoreTest#cleanSaveAllowsTheUpstreamTupletRoundingTolerance` |
| SV-4 clean save allows missing voice and returns source | `MusicXmlStateTest#saveIntegrityAllowsMissingVoiceOnlyForNoopState` |
| SV-4b editing missing voice normalizes only the target | `MusicXmlStateTest#changeToPitchAddsMissingVoiceToEditedNoteOnly` |
| SV-8 same voice split by backup remains valid | `MusicXmlStateTest#saveIntegrityAllowsSameVoiceSplitByBackupForGrandStaffTimeline` |
| SV-5 invalid pitch is rejected | `MusicXmlStateTest#validatesSaveIntegrityForInvalidUpstreamFixtures` |
| SV-6 rest with pitch is rejected | `MusicXmlStateTest#validatesSaveIntegrityForInvalidUpstreamFixtures` |
| SV-7 chord without pitch is rejected | `MusicXmlStateTest#validatesSaveIntegrityForInvalidUpstreamFixtures` |
| PL-1 invalid duration payload is atomic | `MusicXmlStateTest#invalidDurationPayloadIsRejectedWithoutChangedTargets` |
| PL-2 invalid pitch payload is atomic | `MusicXmlStateTest#invalidPitchPayloadIsRejectedWithoutChangedTargets` |
| AT-1 failed command does not mutate prior success | `MusicXmlStateTest#failedCommandDoesNotMutatePreviouslySuccessfulEdit` |
| MP-2 deletion leaves non-target notes and attributes stable | `MusicXmlStateTest#deleteReplacesOnlyTargetNoteAndKeepsMeasureAttributes` |
| MP-3 deletion retains target position and duration | `MusicXmlStateTest#deleteReplacesTargetAtSamePositionAndDuration` |
| TI-7 deletion preserves target voice total duration | `MusicXmlStateTest#deleteKeepsTotalDurationForTargetMeasureVoice` |

`tests/property/core.property.spec.ts` is separately mapped to
`ScoreCorePropertyTest`; it remains an included deterministic property
regression rather than a case in this unit-case table.
