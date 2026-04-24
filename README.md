
Implement the next two Excel sheets as sheet-driven Java reason-code conversion stages:

1. FICO Rsn to ACAPS Rsn Conv
2. Custom Rsn to ACAPS Rsn Conv

Do not use Tachyon.  
Do not use Drools.  
Do not hardcode mappings in Java.

## Goal

These sheets are simple lookup tables used to translate reason codes for final output.

- FICO reason code -> ACAPS reason code
- Custom reason code -> ACAPS reason code

These stages should run after Decision and before final response/output shaping.

## Create

For FICO:
- FicoReasonConversionStage
- FicoReasonConversionService
- FicoReasonConversionExtractor

For Custom:
- CustomReasonConversionStage
- CustomReasonConversionService
- CustomReasonConversionExtractor

Add small row/model classes only if useful.

## FICO sheet behavior

Read sheet FICO Rsn to ACAPS Rsn Conv.

Expected columns:
- RULE ID
- ficoRsnCode
- ficoAcapsRsnCode
- ficoRsnTxtEn
- ficoRsnTxtSp

Only mapping columns are needed for MVP.
Text columns may remain unused for now.

Build map:
- Map<String, String> ficoToAcapsReasonMap

Rules:
- trim all strings
- preserve leading zeroes like 01
- skip blank rows
- if output is N/A, treat as unmapped and do not include it in converted output
- if duplicates exist with same output, dedupe safely
- if duplicates exist with different outputs, fail fast with clear error

## Custom sheet behavior

Read sheet Custom Rsn to ACAPS Rsn Conv.

Expected columns:
- customRsnCode
- customAcapsRsnCode
- customRsnTxtEn
- customRsnTxtSp

Build map:
- Map<String, String> customToAcapsReasonMap

Rules:
- trim all strings
- skip blank rows
- if duplicates exist with same output, allow and dedupe
- if duplicates exist with different outputs, fail fast with clear error

Important: custom codes like C00187 must be treated as strings, not numbers.

## Runtime behavior

After Decision has already run, convert reason codes present in the application output.

Implement conversion for:
- FICO reason codes list
- Custom reason codes list

If a code is missing from map:
- do not fail request
- skip it or leave unmapped according to current output contract
- log warning

If raw and converted lists both exist in output, preserve raw values and populate converted values separately.

## Pipeline placement

Insert these stages:
- after DecisionStage
- before final output mapping / response serialization

## Tests

Add tests for:
- FICO map extraction
- custom map extraction
- leading zero preservation for FICO codes like 01
- duplicate custom code with same output allowed
- duplicate code with conflicting output rejected
- FICO code 96 -> N/A is skipped
- runtime conversion of multiple reason codes works correctly

## Constraints

- Java only
- sheet-driven
- no hardcoded switch/case mappings
- no Tachyon
- no DRL
- keep implementation compact and consistent with current project style

At the end, provide:
- files created/updated
- exact place inserted in pipeline
- assumptions made about output fields
