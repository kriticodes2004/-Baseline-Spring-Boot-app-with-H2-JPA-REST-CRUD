Implement the Final AA Mapping sheet as a sheet-driven Java stage.

Do not use Tachyon.  
Do not use Drools.  
Do not hardcode the mapping rows in Java.

## Goal

This stage determines the final decisionDetails.adverseActions output based on:

- highest priority / winning policyRsnCode
- custxFicoRsnCd
- converted FICO ACAPS reason codes
- converted Custom ACAPS reason codes

This stage must run after:
1. Credit Policy Table enrichment
2. Decision
3. FICO reason conversion
4. Custom reason conversion

Then this stage builds the final adverse action codes list.

## Create

- FinalAdverseActionMappingStage
- FinalAdverseActionMappingService
- FinalAdverseActionMappingExtractor
- FinalAdverseActionMappingRow

Add minimal output model if needed for:
- AdverseAction
or whatever the current response model already uses.

## Excel extraction

Read the Final AA Mapping sheet.

Expected columns include:
- policyRsnCode
- custxFicoRsnCd
- AACodeType1
- AACodeType2
- AACodeType3
- AACodeType4

Build lookup rows keyed by:
- policyRsnCode
- custxFicoRsnCd

Treat custxFicoRsnCd values as strings like:
- C
- F
- N/A

Validation:
- skip blank rows
- trim all strings
- allow rows where later AA columns are blank
- fail fast only if the same (policyRsnCode, custxFicoRsnCd) appears twice with conflicting content

## Runtime input assumptions

At runtime, assume these are already available:
- application-level policies enriched with policyRsnCode and policyRank
- final winning / highest-ranked application policy
- custxFicoRsnCd
- converted FICO ACAPS reason code list
- converted Custom ACAPS reason code list

## Matching logic

1. Determine the winning application-level policyRsnCode
2. Read custxFicoRsnCd
3. Find exact row by:
   - (policyRsnCode, custxFicoRsnCd)
4. If no exact row exists, try fallback:
   - (policyRsnCode, "N/A")

If still not found:
- do not crash
- log warning
- leave adverse actions empty or use minimal fallback according to existing response model

## Supported AA expression patterns

Each AA output cell can contain one of these patterns:

1. policyRsnCode
   - return the winning policy reason code

2. customRsnCodes[n].customAcapsRsnCode
   - return nth converted custom ACAPS reason code

3. ficoRsnCode[n].ficoAcapsRsnCode
   - return nth converted FICO ACAPS reason code

You do not need a general expression engine.
Implement a small parser/evaluator that supports only these exact patterns.

## Evaluation behavior

- evaluate AA columns in order: 1 to 4
- if referenced nth reason does not exist, skip it
- if expression is blank, skip it
- deduplicate final adverse action codes while preserving order
- create final adverseActions output list

## Output population

Populate:
- application.decisionDetails.adverseActions

If the output model expects objects, create objects with the resolved adverse action code.
If the output model expects strings, populate strings.
Keep it aligned with the current project response contract.

## Tests

Add tests for:
- exact row match on (policyRsnCode, custxFicoRsnCd)
- fallback to (policyRsnCode, N/A)
- policyRsnCode expression resolution
- custom reason lookup resolution
- fico reason lookup resolution
- missing indexed reason safely skipped
- duplicate final AA codes removed while preserving order
- final adverseActions populated correctly for examples like:
  - direct policyRsnCode
  - C-based custom reason supplementation
  - F-based fico reason supplementation

## Pipeline placement

Insert this stage after:
- DecisionStage
- FicoReasonConversionStage
- CustomReasonConversionStage

and before final response serialization/output mapping.

## Constraints

- Java only
- sheet-driven
- no Tachyon
- no DRL
- no hardcoded row mappings
- compact implementation
- build on top of the current working codebase

At the end, provide:
- files created/updated
- exact pipeline order near final output
- assumptions made about adverseActions output structure
