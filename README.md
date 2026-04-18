I am building a Spring Boot Java backend for a Wells Fargo Credit Decision Stand-in Service.

Current base package:
com.wellsfargo.creditdecision

Current runtime code already exists for:
- controller
- domain
- dto/request
- engine/context
- engine/stage
- mapper
- exception

Global calcs are already implemented.
Now I want to build the AUTHORING / IMPORT pipeline for ONE SHEET ONLY:
"Knockout Calcs & Policy"

Important:
- Only implement the "Knockout Policy" section for now.
- Ignore the "Knockout Calcs" section for now.
- This is not runtime stage code yet. This is workbook extraction + normalization + Tachyon translation preparation.
- Use plain Java classes with getters/setters, not Lombok.
- Use Jackson-compatible POJOs.
- Use Apache POI for sheet parsing.
- Keep code clean, compilable, and package-correct.

Create the following files in these exact packages:

1) src/main/java/com/wellsfargo/creditdecision/authoring/excel/model/KnockoutSheetMetadata.java
2) src/main/java/com/wellsfargo/creditdecision/authoring/excel/model/KnockoutPolicySheetRow.java
3) src/main/java/com/wellsfargo/creditdecision/authoring/excel/extractor/KnockoutSheetMetadataExtractor.java
4) src/main/java/com/wellsfargo/creditdecision/authoring/excel/extractor/KnockoutPolicySheetExtractor.java
5) src/main/java/com/wellsfargo/creditdecision/authoring/normalize/KnockoutFieldDictionary.java
6) src/main/java/com/wellsfargo/creditdecision/authoring/normalize/KnockoutRowNormalizer.java
7) src/main/java/com/wellsfargo/creditdecision/rules/canonical/CanonicalKnockoutRule.java
8) src/main/java/com/wellsfargo/creditdecision/rules/canonical/CanonicalCondition.java
9) src/main/java/com/wellsfargo/creditdecision/rules/canonical/CanonicalClause.java
10) src/main/java/com/wellsfargo/creditdecision/rules/canonical/CanonicalAction.java
11) src/main/java/com/wellsfargo/creditdecision/tachyon/KnockoutPromptBuilder.java
12) src/main/java/com/wellsfargo/creditdecision/tachyon/TachyonKnockoutTranslationService.java
13) src/main/java/com/wellsfargo/creditdecision/tachyon/TachyonKnockoutTranslationServiceImpl.java

Business context for this sheet:
- Sheet name: "Knockout Calcs & Policy"
- We are implementing only the "Knockout Policy" table.
- The sheet contains rows like:
  - KNOCK1: Credit Bureau Frozen or Locked (Q18)
  - KNOCK2: No Trade No Hit (D22)
- policyCategory is KNOCKOUT
- output location is application.decisionDetails.policies object
- for MVP only primary applicant is supported
- notes say knockout rules should run only for primary applicant for MVP
- notes for D22 say knockout runs before imputations, so ALL0300 must be checked using the raw value, not an imputed BTS value

Expected raw row fields:
- sheetName
- rowNumber
- ruleId
- ruleName
- policyOrCalculation
- inputParameters (List<String>)
- formulaExpression
- policyCode
- policyCategory
- locationInBom
- notes

Expected metadata fields:
- sheetName
- rawInstructions
- primaryApplicantOnly
- stopProcessingIfAnyPolicyTriggered
- outputPath

Field normalization rules for KnockoutFieldDictionary:
Map workbook parameter names to canonical domain field paths as follows:
- frozenIndicator -> application.applicant.bureau.frozenFileInd
- frozenFileInd -> application.applicant.bureau.frozenFileInd
- lockedIndicator -> application.applicant.bureau.lockedFileOrWithheldIndicator
- lockedFileOrWithheldIndicator -> application.applicant.bureau.lockedFileOrWithheldIndicator
- bureauErrorIndicator -> application.applicant.bureau.bureauErrorIndicator
- noTradeInd -> application.applicant.bureau.noTradeInd
- noHitInd -> application.applicant.bureau.noHitInd
- minorIndicator -> application.applicant.bureau.minorIndicator
- ALL0300 -> application.applicant.bureau.all0300
- BTS_ALL0300 -> application.applicant.bureau.all0300

KnockoutRowNormalizer requirements:
- normalize input parameter names using KnockoutFieldDictionary
- preserve raw notes
- set scope hint to APPLICANT
- set stage hint to KNOCKOUT
- set artifact type hint to POLICY_RULE
- if metadata says primary applicant only, preserve that for Tachyon prompt generation
- handle D22 note correctly: raw all0300 should be used, not imputed value

Canonical output model requirements:
CanonicalKnockoutRule should contain:
- artifactId
- stage
- artifactType
- scope
- applicantFilter
- ruleName
- policyCode
- policyCategory
- inputs
- condition
- action
- outputPath
- executionOrder
- notes

CanonicalCondition:
- operator
- clauses

CanonicalClause:
- field
- op
- value
- nullSafe

CanonicalAction:
- type
- policyCode
- applicantIndexSource

KnockoutSheetMetadataExtractor requirements:
- read top yellow instruction area
- produce KnockoutSheetMetadata
- infer:
  - primaryApplicantOnly = true if instruction/notes mention only one applicant supported or primary applicant filter
  - stopProcessingIfAnyPolicyTriggered = true if instructions say transaction should stop if any policy exists
  - outputPath = application.decisionDetails.policies object
- also preserve the raw instruction text

KnockoutPolicySheetExtractor requirements:
- use Apache POI Sheet
- locate the "Knockout Policy" header table
- detect header row that includes columns like:
  - Rule ID
  - Rule Name
  - Policy / Calculation
  - Input Parameters
  - Formula OR Expression
  - Policy Code
  - policyCategory
  - Location in BOM
- extract every valid knockout policy row below that header
- split multiline input parameter cells into List<String>
- safely handle blank cells
- preserve row number
- preserve notes if found
- do not crash on unexpected blank rows
- stop when table clearly ends

KnockoutPromptBuilder requirements:
- build a deterministic prompt for Tachyon for one row
- include:
  - stage fixed as KNOCKOUT
  - artifact type fixed as POLICY_RULE
  - output path application.policies
  - normalized input paths
  - row formula
  - notes
  - instruction that only strict JSON should be returned
- prompt should explain that runtime is deterministic and Tachyon is only translating the business rule into canonical JSON

TachyonKnockoutTranslationService:
- define interface with:
  CanonicalKnockoutRule translate(KnockoutPolicySheetRow row, KnockoutSheetMetadata metadata);

TachyonKnockoutTranslationServiceImpl requirements:
- use KnockoutRowNormalizer
- use KnockoutPromptBuilder
- add a TODO placeholder for actual Tachyon API call
- for now, if policyCode is Q18 or D22, return a mocked CanonicalKnockoutRule so the pipeline can be tested
- mocked Q18 logic:
  - if frozenFileInd = true OR lockedFileOrWithheldIndicator = true then CREATE_POLICY Q18
  - applicantFilter primaryInd = 1
  - stage KNOCKOUT
  - artifactType POLICY_RULE
  - outputPath application.policies
  - executionOrder 10
- mocked D22 logic:
  - primary only
  - use raw all0300
  - executionOrder 20
  - include notes saying knockout runs before imputations
- if unsupported policyCode, throw UnsupportedOperationException with clear message

Also:
- add useful helper methods for reading string cell values
- keep code production-style and null-safe
- no Lombok
- generate full getters/setters
- package declarations must exactly match paths above
- imports must be correct
- all code should compile cleanly

After generating the files, also generate a short example snippet showing how to use:
- KnockoutSheetMetadataExtractor
- KnockoutPolicySheetExtractor
- TachyonKnockoutTranslationServiceImpl

Use only the knockout sheet context described above.
