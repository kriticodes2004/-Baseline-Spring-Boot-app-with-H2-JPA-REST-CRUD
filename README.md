You are working in an existing Spring Boot Maven project for a Credit Decision Stand-in Service.

Implement only the **Knockout-sheet Excel extractor + compact Tachyon authoring + normalized artifact validation + Drools DRL rendering** pipeline.

Assume I will provide:
- the local Excel workbook path
- the knockout sheet name if needed
- the Tachyon calling details/config already required in the project

Do not assume anything outside this prompt.

==================================================
HIGH-LEVEL GOAL
==================================================

Build a sheet-specific pipeline for the Excel sheet **Knockout Calcs & Policy** that does this:

1. read the actual Excel workbook from a local file path
2. find and extract only the knockout sheet
3. detect:
   - instruction text
   - Knockout Calcs section
   - Knockout Policy section
   - section headers
   - all data rows under each section
4. normalize extracted policy rows into structured internal row objects
5. validate rows before Tachyon
6. exclude invalid rows from Tachyon input
7. build a **small compact Tachyon prompt** using only valid normalized knockout rows
8. call Tachyon
9. parse Tachyon response into normalized knockout rule artifacts
10. validate returned artifacts
11. render valid artifacts into Drools DRL
12. preserve traceability from Excel row -> normalized row -> Tachyon artifact -> DRL rule
13. expose the full result from an API endpoint for testing

This is NOT runtime rule execution yet.
This is NOT the full workbook pipeline.
This is ONLY the authoring pipeline for the knockout sheet.

==================================================
IMPORTANT DESIGN CONSTRAINTS
==================================================

1. Keep the Tachyon prompt compact because large prompts can be slow and may cause timeout/504.
2. Do NOT send the whole workbook to Tachyon.
3. Do NOT send unrelated sheets to Tachyon.
4. Do NOT send invalid rows to Tachyon.
5. Extraction must be dynamic:
   - if new rows are added under the knockout tables and the pipeline is rerun, they should also be extracted automatically
   - do not hardcode fixed row numbers
6. Preserve source row numbers for traceability.
7. Keep this sheet-specific for now. Do NOT build a giant generic all-sheet framework yet.
8. Do NOT implement runtime Drools execution in this stage.
9. Do NOT implement the full decision engine in this stage.
10. Do NOT hardcode secrets.

==================================================
BASE PACKAGE
==================================================

Use this exact base package:

com.example.creditdecision

==================================================
REUSE EXISTING COMPONENTS IF THEY ALREADY EXIST
==================================================

Reuse if already present:
- TachyonClient
- TachyonProperties
- knockout artifact model classes
- KnockoutNormalizationValidator
- KnockoutDroolsRenderer

If they do not exist yet, create the minimum needed versions for compilation and this pipeline.

==================================================
DEPENDENCIES
==================================================

If needed, add only:
- org.apache.poi:poi-ooxml

Do not add unnecessary dependencies.

==================================================
FILES TO ADD
==================================================

Create these files exactly:

src/main/java/com/example/creditdecision/authoring/knockout/excel/model/ExtractedKnockoutRow.java
src/main/java/com/example/creditdecision/authoring/knockout/excel/model/RejectedKnockoutRow.java
src/main/java/com/example/creditdecision/authoring/knockout/excel/model/KnockoutExtractionResult.java

src/main/java/com/example/creditdecision/authoring/knockout/excel/service/KnockoutExcelExtractor.java
src/main/java/com/example/creditdecision/authoring/knockout/excel/service/KnockoutRowNormalizer.java
src/main/java/com/example/creditdecision/authoring/knockout/excel/service/KnockoutRowPreValidator.java

src/main/java/com/example/creditdecision/authoring/knockout/model/KnockoutAuthoringPipelineResult.java

src/main/java/com/example/creditdecision/authoring/knockout/service/KnockoutCompactPromptService.java
src/main/java/com/example/creditdecision/authoring/knockout/service/KnockoutExcelAuthoringPipelineService.java

src/main/java/com/example/creditdecision/api/KnockoutExcelAuthoringController.java

==================================================
1) CREATE ExtractedKnockoutRow.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.excel.model

Use Lombok @Data.

Fields:
- Integer sourceRowNumber
- String sectionType
- String ruleId
- String ruleName
- String inputParametersRaw
- String formulaRaw
- String policyCode
- String policyCategory
- String locationInBom
- String notes
- String rawRowSnapshot
- boolean validForTachyon

Purpose:
This is the normalized pre-Tachyon row structure for knockout rows.

==================================================
2) CREATE RejectedKnockoutRow.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.excel.model

Use Lombok @Data.

Fields:
- Integer sourceRowNumber
- String sectionType
- String rawRowSnapshot
- String rejectionReason

==================================================
3) CREATE KnockoutExtractionResult.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.excel.model

Use Lombok @Data.

Fields:
- String workbookPath
- String sheetName
- List<String> instructions = new ArrayList<>()
- List<ExtractedKnockoutRow> knockoutCalcRows = new ArrayList<>()
- List<ExtractedKnockoutRow> knockoutPolicyRows = new ArrayList<>()
- List<RejectedKnockoutRow> rejectedRows = new ArrayList<>()

==================================================
4) CREATE KnockoutExcelExtractor.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.excel.service

Use @Service.

Provide method:
- public KnockoutExtractionResult extract(String workbookPath, String preferredSheetName)

Responsibilities:
- open workbook from local file path using Apache POI
- identify the knockout sheet
- extract instruction text near top
- detect section titles similar to:
  - Knockout Calcs
  - Knockout Policy
- detect header row for each section by cell labels, not fixed row numbers
- scan downward dynamically until next section or repeated blank termination
- include newly added rows on refresh automatically
- preserve source row numbers
- capture raw row snapshot text

Implementation guidance:
- if preferredSheetName is provided and found, use it
- otherwise search workbook sheets for a sheet name containing “Knockout”
- detect headers by looking for expected labels like:
  - Rule ID
  - Rule Name
  - Input Parameters
  - Formula OR Expression
  - Policy Code
  - Policy Category
  - Location In BOM
  - Notes
- for calc section allow Rule ID / Derived Attribute / Formula type patterns
- do not reject rows here unless structurally unreadable; normalization + prevalidation can handle business-field completeness

==================================================
5) CREATE KnockoutRowNormalizer.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.excel.service

Use @Service.

Provide method:
- public List<ExtractedKnockoutRow> normalizePolicyRows(KnockoutExtractionResult extractionResult)

Responsibilities:
- take raw extracted knockout policy rows from extractor
- normalize cells into explicit fields
- trim whitespace
- preserve raw formula text
- preserve raw row snapshot
- set sectionType = KNOCKOUT_POLICY
- keep sourceRowNumber
- map columns into:
  - ruleId
  - ruleName
  - inputParametersRaw
  - formulaRaw
  - policyCode
  - policyCategory
  - locationInBom
  - notes

If extractor already returns normalized rows, this class can refine/clean them rather than duplicate extraction logic.

==================================================
6) CREATE KnockoutRowPreValidator.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.excel.service

Use @Service.

Provide method:
- public KnockoutExtractionResult validateAndFilter(KnockoutExtractionResult extractionResult)

Responsibilities:
- validate knockout policy rows before Tachyon
- rows missing required fields must NOT be sent to Tachyon
- invalid rows should be moved to rejectedRows with rejectionReason
- valid rows should remain in knockoutPolicyRows and have validForTachyon = true

Required fields for a valid knockout policy row:
- ruleId
- ruleName
- formulaRaw
- policyCode
- policyCategory
- locationInBom

Warnings are fine internally, but rows missing any required field above must be excluded from Tachyon input.

==================================================
7) CREATE KnockoutAuthoringPipelineResult.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.model

Use Lombok @Data.

Fields:
- KnockoutExtractionResult extractionResult
- String compactPrompt
- String rawTachyonResponse
- KnockoutSheetArtifact artifact
- ValidationResult artifactValidationResult
- String renderedDrl

This is the final API response payload for pipeline testing.

==================================================
8) CREATE KnockoutCompactPromptService.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.service

Use @Service.

Provide method:
- public String buildCompactPrompt(KnockoutExtractionResult extractionResult)

Purpose:
Build a small Tachyon prompt only from valid knockout policy rows.

Prompt requirements:
- short sheet purpose
- short execution semantics:
  - evaluate only primary applicant for MVP
  - if triggered, create policy under application.decisionDetails.policies
  - preserve grouped logic exactly
  - preserve raw bureau fields vs bts fields
- short field path conventions:
  - application.applicant[].bureau.<field>
  - bts.<field>
  - application.decisionDetails.policies
- include only valid normalized knockout policy rows
- ask for structured JSON only, not prose

The prompt should be much smaller than previous large prompts.
Do NOT include unrelated workbook architecture.

==================================================
9) CREATE KnockoutExcelAuthoringPipelineService.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.service

Use @Service.
Use @RequiredArgsConstructor.

Dependencies:
- KnockoutExcelExtractor
- KnockoutRowNormalizer
- KnockoutRowPreValidator
- KnockoutCompactPromptService
- TachyonClient
- ObjectMapper
- KnockoutNormalizationValidator
- KnockoutDroolsRenderer

Provide method:
- public KnockoutAuthoringPipelineResult run(String workbookPath, String preferredSheetName)

Behavior:
1. extract knockout sheet
2. normalize policy rows
3. write normalized rows back into extractionResult
4. prevalidate and filter rows
5. build compact prompt from valid rows only
6. build a minimal Tachyon chat request
   - system message: brief instruction to convert knockout rows into normalized JSON rule artifacts
   - user message: compact prompt from service
7. call Tachyon
8. capture raw response string
9. clean fenced JSON if needed
10. parse into KnockoutSheetArtifact
11. validate artifact using KnockoutNormalizationValidator
12. if valid, render DRL using KnockoutDroolsRenderer, else renderedDrl = empty string
13. return KnockoutAuthoringPipelineResult

Also implement a private helper to strip ```json fences safely.

If no valid rows remain after prevalidation:
- do NOT call Tachyon
- return result with extractionResult populated, compactPrompt populated, rawTachyonResponse empty, artifact null, validation result showing invalid or warning, renderedDrl empty

==================================================
10) CREATE KnockoutExcelAuthoringController.java
==================================================

Package:
com.example.creditdecision.api

Use @RestController
Use @RequestMapping("/authoring/knockout/excel")
Use @RequiredArgsConstructor

Dependency:
- KnockoutExcelAuthoringPipelineService

Add POST endpoint:
- @PostMapping
- request body class KnockoutExcelAuthoringRequest
- returns KnockoutAuthoringPipelineResult

Nested request DTO fields:
- String workbookPath
- String preferredSheetName

Endpoint behavior:
- return ResponseEntity.ok(service.run(request.getWorkbookPath(), request.getPreferredSheetName()))

Use Lombok @Data on nested request DTO.

==================================================
11) TACHYON REQUEST SHAPE
==================================================

Inside the pipeline service, the system prompt should be very short, like:

You convert knockout-sheet rows into normalized rule artifacts.
Return JSON only.
Preserve grouped boolean logic exactly.
Preserve primary-applicant scope as PRIMARY_ONLY for MVP.
Do not return prose.

The user prompt should be the compact prompt built from valid rows only.

Keep this compact because Tachyon is slow.

==================================================
12) TRACEABILITY REQUIREMENTS
==================================================

Preserve at minimum:
- workbookPath
- sheetName
- sourceRowNumber
- rawRowSnapshot
- formulaRaw
- normalized row fields
- raw Tachyon response
- final rendered DRL

If possible, include sourceRowNumber in the compact prompt per row so returned artifacts can be traced back.

==================================================
13) DYNAMIC EXTRACTION REQUIREMENT
==================================================

Make sure extraction is not dependent on fixed row numbers.
If a user adds another rule row under the knockout policy table and reruns the endpoint, that new row should also be extracted automatically.

Header and section detection must drive extraction, not hardcoded row positions.

==================================================
14) WHAT NOT TO DO
==================================================

Do NOT:
- implement runtime Drools execution
- implement full workbook generic processing
- implement all sheets
- send invalid rows to Tachyon
- send giant prompts
- hardcode secrets
- mix unrelated business logic into this pipeline

==================================================
15) AFTER IMPLEMENTATION
==================================================

After coding:
1. ensure project compiles
2. list files created/updated
3. mention assumptions
4. provide sample request body for the endpoint:
   /authoring/knockout/excel
5. stop there

Now implement exactly this pipeline.
