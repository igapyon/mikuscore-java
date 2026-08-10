# Pinned upstream MuseScore I/O case map

This is the atomic inventory for the 93 included cases in
`tests/unit/musescore-io.spec.ts` at renamed Node upstream revision
`a8adc1998237f7b371cae75728afec7dd1795977`.

Every included entry is `done evidence`: its named Java regression passes
against the pinned source semantics. Scope exclusions are recorded separately.

| Upstream case | Java owner / evidence | Status |
| --- | --- | --- |
| converts basic mscx chord/rest content into MusicXML | `MuseScoreIoTest#importsBasicMuseScoreChordAndRestContentLikePinnedUpstreamCase` | done evidence |
| prefers VBox title/composer when MuseScore meta tags are placeholders | `MuseScoreIoTest#routesPublicMuseScoreImportMetadataAndVBoxFallbacks` | done evidence |
| maps MuseScore project metadata to standard MusicXML fields | `MuseScoreIoTest#routesPublicMuseScoreImportMetadataAndVBoxFallbacks` | done evidence |
| imports tempo/time/key changes, repeats, and dynamics | `MuseScoreIoTest#importsMuseScoreTempoSignaturesRepeatsAndDynamicsLikePinnedUpstreamCase` | done evidence |
| imports MuseScore concertKey when KeySig accidental is absent | `MuseScoreIoTest#importsMuseScoreConcertAndTransposeKeysLikePinnedUpstreamCases` | done evidence |
| prefers MuseScore transposeKey over concertKey for transposing instruments | `MuseScoreIoTest#importsMuseScoreConcertAndTransposeKeysLikePinnedUpstreamCases` | done evidence |
| imports repeats from MuseScore BarLine subtype variants | `MuseScoreIoTest#importsMuseScoreRepeatAndDoubleBarlineMarkers` | done evidence |
| roundtrips sample4 mid-measure end-start-repeat barlines | `MuseScoreIoTest#roundTripsPinnedMsczSampleFourMidMeasureEndStartRepeats` | done evidence |
| imports MuseScore cut-time symbol as MusicXML time symbol | `MuseScoreIoTest#prioritizesDirectMuseScoreSignaturesAndRetainsCutTimeSymbol` | done evidence |
| imports MuseScore Instrument transpose into MusicXML attributes transpose | `MuseScoreIoTest#importsMuseScoreConcertAndTransposeKeysLikePinnedUpstreamCases` | done evidence |
| optionally normalizes MuseScore cut-time to 2/2 in MusicXML | `MuseScoreIoTest#routesPublicMuseScoreConversionOptionsToCutTimeAndImplicitBeamBehavior` | done evidence |
| keeps cut-time symbol on following measures without explicit TimeSig | `MuseScoreIoTest#prioritizesDirectMuseScoreSignaturesAndRetainsCutTimeSymbol` | done evidence |
| imports only measure-anchored tempo text as words direction | `MuseScoreIoTest#importsVisibleAndHiddenMuseScoreTempoTextLikePinnedUpstreamCases` | done evidence |
| imports multiple Tempo events in one measure (e.g. Tema) as words directions | `MuseScoreIoTest#importsVisibleAndHiddenMuseScoreTempoTextLikePinnedUpstreamCases` | done evidence |
| does not emit words for hidden MuseScore Tempo text (visible=0) | `MuseScoreIoTest#importsVisibleAndHiddenMuseScoreTempoTextLikePinnedUpstreamCases` | done evidence |
| skips hidden MuseScore Dynamic (visible=0) | `MuseScoreIoTest#routesMuseScoreDynamicThroughPublicMusicXmlImport` | done evidence |
| infers key mode from title when MuseScore key mode is not present | `MuseScoreIoTest#importsMuseScoreKeyModesFromTitleFallbackAndMeasureKeySignature` | done evidence |
| emits natural accidental when note cancels key-signature sharp | `MuseScoreIoTest#importsMuseScoreKeyAccidentalsAndTpcPitchSpelling` | done evidence |
| imports note-level accidentals from MuseScore Accidental subtype | `MuseScoreIoTest#mapsMuseScoreAccidentalSubtypeToMusicXmlText` | done evidence |
| prefers MuseScore tpc spelling for enharmonic notes when Accidental subtype is absent | `MuseScoreIoTest#importsMuseScoreKeyAccidentalsAndTpcPitchSpelling` | done evidence |
| prefers MuseScore accidental subtype for pitch spelling even in flat key context | `MuseScoreIoTest#importsMuseScoreKeyAccidentalsAndTpcPitchSpelling` | done evidence |
| imports marker/jump as MusicXML directions and emits diag when playback mapping is incomplete | `MuseScoreIoTest#routesMuseScoreDynamicThroughPublicMusicXmlImport`, `#emitsDiagnosticsForUnsupportedMuseScoreDirectionLikeEvents` | done evidence |
| imports MuseScore Expression as MusicXML words direction | `MuseScoreIoTest#routesMuseScoreDynamicThroughPublicMusicXmlImport` | done evidence |
| exports basic MusicXML content into mscx | `MuseScoreIoTest#exportsBasicMusicXmlContentIntoMuseScoreLikePinnedUpstreamCase` | done evidence |
| exports MusicXML header metadata into MuseScore metaTag fields | `MuseScoreIoTest#exportsMusicXmlHeaderMetadataIntoMuseScoreMetaTagsLikePinnedUpstreamCase` | done evidence |
| preserves MusicXML tuplet markers through MuseScore roundtrip | `MuseScoreIoTest#roundTripsMusicXmlTupletMarkersLikePinnedUpstreamCase` | done evidence |
| roundtrip keeps octave-shift (8va/8vb) exported as chord-local Ottava spanner | `MuseScoreIoTest#roundTripsMusicXmlOctaveShiftAsMuseScoreOttavaSpanner` | done evidence |
| roundtrip keeps trill ornaments exported as chord-local Trill spanner | `MuseScoreIoTest#roundTripsMusicXmlTrillWavyLineAsMuseScoreSpanner` | done evidence |
| roundtrip keeps staccato articulation on a single-measure note | `MuseScoreIoTest#roundTripsMusicXmlArticulationsAndDynamicsThroughMuseScoreFacade` | done evidence |
| roundtrip keeps accent articulation on a single-measure note | `MuseScoreIoTest#roundTripsMusicXmlArticulationsAndDynamicsThroughMuseScoreFacade` | done evidence |
| roundtrip keeps implicit short pickup measure length | `MuseScoreIoTest#importsMuseScoreMeasureLengthAsAnImplicitPickupMeasure`, `#buildsMuseScoreExportMeasureContextFromValues` | done evidence |
| roundtrip keeps grace note marker and principal duration | `MuseScoreIoTest#importsMuseScoreGraceAndAcciaccaturaWithoutConsumingMeasureTime`, `#buildsMuseScoreExportChordAndRestXml` | done evidence |
| roundtrip keeps unpitched notes as timed note events | `MuseScoreIoTest#roundTripsUnpitchedMusicXmlNotesAsTimedMuseScoreChordEvents` | done evidence |
| roundtrip keeps section-boundary double bar + explicit same-meter time (m24/m25 minimal) | `MuseScoreIoTest#roundTripsMusicXmlMiddleEndStartRepeatThroughMuseScoreVoiceBarline`, `#buildMuseScoreExportMeasureContextFromValues` | done evidence |
| keeps written type for MusicXML triplet eighths on MusicXML->MuseScore->MusicXML | `MuseScoreIoTest#roundTripsMusicXmlTupletMarkersLikePinnedUpstreamCase`, `#keepsWrittenDurationTypeSeparateFromTupletScaledDuration` | done evidence |
| exports MusicXML cut-time symbol into MuseScore TimeSig subtype | `MuseScoreIoTest#routesPublicMuseScoreExportCutTimeOption` | done evidence |
| optionally normalizes MusicXML cut-time 4/4 into MuseScore 2/2 | `MuseScoreIoTest#routesPublicMuseScoreExportCutTimeOption` | done evidence |
| keeps cut-time subtype on following MuseScore measures without explicit time change | `MuseScoreIoTest#retainsCutTimeSubtypeWithoutFollowingExplicitTimeChangeLikePinnedUpstreamCase` | done evidence |
| exports MusicXML words+sound tempo as MuseScore Tempo text and words-only as Expression | `MuseScoreIoTest#routesMusicXmlDirectionsThroughPublicMuseScoreExport` | done evidence |
| exports metronome-only tempo direction as MuseScore Tempo | `MuseScoreIoTest#collectsDirectionSeedsFromMusicXmlValues` | done evidence |
| exports MusicXML segno/coda/fine and sound jump attrs into MuseScore Marker/Jump | `MuseScoreIoTest#collectsDirectionSeedsFromMusicXmlValues`, `#buildsMuseScoreDirectionSeedXml` | done evidence |
| exports MusicXML sound dynamics into MuseScore Dynamic velocity | `MuseScoreIoTest#routesMusicXmlDirectionsThroughPublicMuseScoreExport` | done evidence |
| exports MusicXML octave-shift direction into MuseScore Ottava spanner | `MuseScoreIoTest#roundTripsMusicXmlOctaveShiftAsMuseScoreOttavaSpanner` | done evidence |
| exports MusicXML trill ornaments into MuseScore Trill spanner | `MuseScoreIoTest#roundTripsMusicXmlTrillWavyLineAsMuseScoreSpanner` | done evidence |
| exports MusicXML trill-mark without wavy-line as MuseScore chord Ornament trill | `MuseScoreIoTest#roundTripsMusicXmlTrillMarkOnlyAsMuseScoreChordOrnament` | done evidence |
| exports MusicXML tie/slur into MuseScore note/chord markers | `MuseScoreIoTest#exportsMusicXmlTieAndSlurAsMuseScoreMarkers` | done evidence |
| assigns independent slur ids per part to avoid cross-part slur linking | `MuseScoreIoTest#resolvesMuseScoreExportSlurIds` | done evidence |
| emits slur stop before slur start when both occur on the same note | `MuseScoreIoTest#resolvesMuseScoreExportSlurFractions` | done evidence |
| reuses slur start span for slur stop when start/stop note durations differ | `MuseScoreIoTest#resolvesMuseScoreExportSlurFractions` | done evidence |
| exports MusicXML alto clef as MuseScore concertClefType C | `MuseScoreIoTest#exportsMusicXmlAltoClefAsMuseScoreDefaultClefCThreeLikePinnedUpstreamCase` | done evidence |
| exports MusicXML articulations into MuseScore Articulation subtypes | `MuseScoreIoTest#exportsMusicXmlArticulationsAsMuseScoreSubtypes` | done evidence |
| exports MusicXML technical stopped into MuseScore left-hand pizzicato articulation | `MuseScoreIoTest#exportsMusicXmlTechnicalNotationAsMuseScoreArticulationAndNoteValues` | done evidence |
| exports MusicXML technical bow/open/harmonic and fingering/string into MuseScore | `MuseScoreIoTest#exportsMusicXmlTechnicalNotationAsMuseScoreArticulationAndNoteValues` | done evidence |
| exports multi-staff MusicXML part into MuseScore Part with multiple Staff refs | `MuseScoreIoTest#exportsMultiStaffPartScaffoldAndInstrumentShortNameToMuseScore` | done evidence |
| exports MusicXML part-abbreviation into MuseScore Instrument shortName | `MuseScoreIoTest#exportsMultiStaffPartScaffoldAndInstrumentShortNameToMuseScore` | done evidence |
| exports sample2.mxl with viola/cello clef defaults (C3/F4) | `MuseScoreIoTest#exportsPinnedMxlSampleTwoWithViolaAndCelloClefDefaults` | done evidence |
| exports sample2.mxl Violin1 m2 slur stop span consistent with its start span | `MuseScoreIoTest#exportsPinnedMxlSampleTwoViolinOneSlurStopWithItsStartSpan` | done evidence |
| keeps viola/cello clefs on sample2.mscz -> MusicXML -> MuseScore path | `MuseScoreIoTest#importsPinnedMsczSampleTwoWithPartNamesAndInstrumentClefs` | done evidence |
| imports multi-staff MuseScore part into a single MusicXML part with staves | `MuseScoreIoTest#importsStaffSpecificMuseScoreClefsForGrandStaffParts` | done evidence |
| keeps voice numbers per staff when MuseScore measure has multiple voice lanes | `MuseScoreIoTest#assignsPartWideVoiceNumbersForMultipleMuseScoreVoiceLanes` | done evidence |
| places direction per voice lane with explicit voice/staff tags | `MuseScoreIoTest#placesMuseScoreDirectionsInTheirSourceVoiceLane` | done evidence |
| emits detailed diag fields for dropped events | `MuseScoreIoTest#emitsDetailedDiagnosticsForDroppedMuseScoreEvents` | done evidence |
| uses Part staff defaultClef when measure-level clef is absent | `MuseScoreIoTest#importsMuseScorePartDefaultAndInstrumentStaffClefs` | done evidence |
| imports MuseScore C clef (C3) from measure clef | `MuseScoreIoTest#importsMuseScoreMeasureCThreeClefLikePinnedUpstreamCase` | done evidence |
| uses Part staff defaultClef C3 when measure-level clef is absent | `MuseScoreIoTest#importsMuseScorePartDefaultAndInstrumentStaffClefs` | done evidence |
| imports local Mozart SQ fixture clefs from mscz (P1/P2=G2, P3=C3, P4=F4) | `MuseScoreIoTest#importsPinnedMsczSampleFourStringQuartetClefs` | done evidence |
| handles tuplet and measure-rest duration without unknown-duration diag | `MuseScoreIoTest#importsMuseScoreMeasureDurationRestAtTheMeasureCapacity`, `#importsMuseScoreTupletReferencesWithWrittenTypeAndActualDuration` | done evidence |
| handles MuseScore tuplet id references without nesting durations | `MuseScoreIoTest#importsMuseScoreTupletReferencesWithWrittenTypeAndActualDuration` | done evidence |
| imports pickup measure len as implicit short measure | `MuseScoreIoTest#importsMuseScoreMeasureLengthAsAnImplicitPickupMeasure` | done evidence |
| adds final light-heavy barline on the last measure even without explicit MuseScore end barline | `MuseScoreIoTest#addsFinalLightHeavyBarlineWithoutExplicitMuseScoreEndBarlineLikePinnedUpstreamCase` | done evidence |
| keeps written note type for tuplet notes (e.g. 16th triplet stays 16th) | `MuseScoreIoTest#importsMuseScoreTupletReferencesWithWrittenTypeAndActualDuration` | done evidence |
| maps MuseScore BeamMode begin chain to MusicXML beam begin/continue/end | `MuseScoreIoTest#buildsMuseBeamXmlFromExplicitBeamModeChain` | done evidence |
| keeps beaming across a rest when MuseScore BeamMode marks the rest lane | `MuseScoreIoTest#buildsMuseImportedBeamInfoAndReadsByEventIndex` | done evidence |
| includes the preceding chord when rest starts with BeamMode mid | `MuseScoreIoTest#includesPrecedingChordWhenMuseRestStartsWithBeamModeMid` | done evidence |
| does not infer beams when MuseScore BeamMode is absent and implicit-beam fill is disabled | `MuseScoreIoTest#skipsImplicitMuseBeamInferenceWhenDisabled` | done evidence |
| infers beams when MuseScore BeamMode is absent (default behavior) | `MuseScoreIoTest#infersMuseBeamsAtBeatBoundariesWhenBeamModeIsAbsent` | done evidence |
| imports MuseScore Slur spanner into MusicXML slur start/stop | `MuseScoreIoTest#importsMuseSlurSpannerAsMusicXmlStartStop` | done evidence |
| keeps slur matching across measure boundary for MuseScore Spanner Slur | `MuseScoreIoTest#keepsMuseSlurSpannerNumberAcrossMeasureBoundary` | done evidence |
| imports MuseScore legacy chord-level Slur type start/stop with id | `MuseScoreIoTest#importsMuseLegacyChordLevelSlurStartStopWithId` | done evidence |
| imports MuseScore note tie markers into MusicXML tie/tied | `MuseScoreIoTest#importsMuseScoreNoteTieAndEndSpannerMarkers` | done evidence |
| imports MuseScore chord articulation subtype into MusicXML articulations | `MuseScoreIoTest#importsMuseScoreChordArticulationAndTechnicalSubtypes` | done evidence |
| maps MuseScore left-hand pizzicato articulation into MusicXML technical stopped (+) | `MuseScoreIoTest#importsMuseScoreChordArticulationAndTechnicalSubtypes` | done evidence |
| maps MuseScore technical articulations and note fingering/string into MusicXML technical | `MuseScoreIoTest#importsMuseScoreChordArticulationAndTechnicalSubtypes` | done evidence |
| maps MuseScore brassMuteClosed ornament into MusicXML technical stopped | `MuseScoreIoTest#importsMuseScoreChordArticulationAndTechnicalSubtypes` | done evidence |
| imports MuseScore Trill spanner into MusicXML ornaments trill-mark/wavy-line | `MuseScoreIoTest#importsMuseScoreStandaloneAndChordLocalTrillSpannersWithSharedNumbers` | done evidence |
| imports MuseScore chord Ornament trill into MusicXML trill-mark | `MuseScoreIoTest#importsMuseScoreChordLocalTrillOrnament` | done evidence |
| maps accidental near MuseScore chord Ornament trill to MusicXML accidental-mark | `MuseScoreIoTest#importsMuseScoreChordLocalTrillOrnament` | done evidence |
| imports MuseScore Ottava spanner into MusicXML octave-shift direction | `MuseScoreIoTest#importsMuseScoreStandaloneAndChordLocalOttavaSpannersWithDisplayPitchShift` | done evidence |
| raises displayed pitch under Ottava while exporting octave-shift | `MuseScoreIoTest#importsMuseScoreStandaloneAndChordLocalOttavaSpannersWithDisplayPitchShift` | done evidence |
| keeps Ottava display shift active across measure boundaries including repeat barlines | `MuseScoreIoTest#keepsMuseOttavaDisplayShiftAcrossMeasureBoundary` | done evidence |
| keeps sample7 measure 3-4 pitch spelling and accidentals on roundtrip | `MuseScoreIoTest#roundTripsPinnedPublicSampleSevenPitchSpellingAndStaffFourNaturalAccidental` | done evidence |
| roundtrips MusicXML transpose through MuseScore Instrument transpose | `MuseScoreIoTest#roundTripsMusicXmlTransposeThroughMuseScoreInstrument` | done evidence |
| keeps sample7 measure 7 staff 4 natural accidental on roundtrip | `MuseScoreIoTest#roundTripsPinnedPublicSampleSevenPitchSpellingAndStaffFourNaturalAccidental` | done evidence |
