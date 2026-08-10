# Pinned upstream measure-operations case map

This map covers `tests/unit/measure-operations.spec.ts` at renamed Node
upstream revision `a8adc1998237f7b371cae75728afec7dd1795977`.
`measure-operations.ts` is a runtime-independent MusicXML text facade, so all
five unit cases are included.

| Upstream case | Java regression | Status |
| --- | --- | --- |
| extracts a self-contained measure with inherited rendering attributes | `MusicXmlIoTest#exposesStringMeasureOperationFacadesWithEditorOnlyAttributes` | done evidence |
| replaces a measure without persisting attributes injected only for the editor | `MusicXmlIoTest#exposesStringMeasureOperationFacadesWithEditorOnlyAttributes` | done evidence |
| appends a full-measure rest using inherited time and divisions | `MusicXmlIoTest#appendsFullMeasureRestWithInheritedTiming` | done evidence |
| appends synchronized rests and a backup for a treble-bass grand staff | `MusicXmlIoTest#appendsSynchronizedRestsAndBackupForTrebleBassGrandStaff` | done evidence |
| returns null for invalid documents or missing operation targets | `MusicXmlIoTest#returnsNullForInvalidOrMissingStringMeasureOperationTargets` | done evidence |

The Java text facade delegates extraction and replacement to the existing DOM
operations, then adds source-equivalent appended-rest generation: inherited
timing is resolved from the final measure, and an explicit G/F two-staff
layout receives lane-one rest, `backup`, and lane-two rest.
