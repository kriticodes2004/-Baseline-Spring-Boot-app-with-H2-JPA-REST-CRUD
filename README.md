You are modifying an existing Spring Boot Java project.

Base package:
com.wellsfargo.creditdecision

Current project already has:
- request DTOs
- request-to-domain mapper
- GlobalCalcsStage
- DecisionContext
- domain Application / Applicant / Bureau / DecisionDetails
- Policy domain class may already exist; if not, create/update as needed
- knockout Excel authoring pipeline already exists:
  - KnockoutSheetMetadata
  - KnockoutPolicySheetRow
  - KnockoutSheetMetadataExtractor
  - KnockoutPolicySheetExtractor
  - KnockoutFieldDictionary
  - KnockoutRowNormalizer
- canonical knockout model already exists:
  - CanonicalKnockoutRule
  - CanonicalCondition
  - CanonicalClause
  - CanonicalAction
- KnockoutPromptBuilder already exists but may need updating
- TachyonKnockoutTranslationService skeleton/mock already exists
- smoke test for knockout extraction already passes

GOAL:
Complete the Knockout component end-to-end with this architecture:

Excel workbook
-> metadata extractor
-> row extractor
-> row normalizer
-> Tachyon translation
-> translated artifact containing:
   - canonical knockout rule JSON
   - drlPreview
-> save translated artifacts to JSON file
-> runtime loads canonical rules from saved JSON file
-> generic evaluator evaluates rules
-> KnockoutStage creates Policy objects in application.decisionDetails.policies
-> if any knockout policy created, context.stopProcessing = true and stopReason = "KNOCKOUT"

IMPORTANT ARCHITECTURE RULES:
1. Do NOT hardcode runtime policy logic by policy code in KnockoutStage.
2. Do NOT create too many tiny files.
3. Keep runtime generic and compact.
4. Tachyon should translate workbook rule rows into:
   - canonical structured JSON
   - DRL preview string
5. Runtime should still use canonical JSON + generic evaluator for MVP.
6. DRL preview is saved and visible for demo/judging, but runtime does not need to compile/use DRL yet.
7. No DB yet for MVP; use JSON file persistence.
8. Keep fallback mock translation behind property:
   credit.tachyon.mock-enabled=true

==================================================
PACKAGE / FILE PLAN
==================================================

CREATE OR UPDATE ONLY THESE FILES:

1) CREATE
src/main/java/com/wellsfargo/creditdecision/rules/canonical/KnockoutTranslatedArtifact.java

Purpose:
Wrapper object for one translated knockout rule artifact.

Fields:
- String sourceSheetName
- String sourceRuleId
- Integer sourceRowNumber
- CanonicalKnockoutRule canonicalArtifact
- String drlPreview

Plain POJO with getters/setters.

2) CREATE
src/main/java/com/wellsfargo/creditdecision/tachyon/TachyonClient.java

Purpose:
Minimal reusable client for Tachyon.
Read credentials from:
- property or env: TACHYON_API_KEY
- property or env: TACHYON_API_SECRET

Required behavior:
- Spring @Component
- method:
  String translateRule(String prompt)
- support property:
  credit.tachyon.mock-enabled=true/false
- if mock-enabled is true, caller can still use mock path without calling remote
- include placeholder real HTTP call structure using Spring RestClient or RestTemplate/WebClient, whichever is simplest and already available
- keep endpoint configurable with property:
  credit.tachyon.base-url
- do not log secrets
- add TODO comments where real Tachyon response parsing may need adjustment

3) UPDATE
src/main/java/com/wellsfargo/creditdecision/tachyon/KnockoutPromptBuilder.java

Make it build a strong prompt for ONE knockout row.

Public method suggestion:
String buildPrompt(KnockoutPolicySheetRow row, KnockoutSheetMetadata metadata)

Prompt must instruct Tachyon to return STRICT JSON with this wrapper shape:

{
  "sourceSheetName": "...",
  "sourceRuleId": "...",
  "sourceRowNumber": 0,
  "canonicalArtifact": {
    "artifactId": "...",
    "stage": "KNOCKOUT",
    "artifactType": "POLICY_RULE",
    "scope": "APPLICANT",
    "applicantFilter": "primaryInd = 1",
    "ruleName": "...",
    "policyCode": "...",
    "policyCategory": "KNOCKOUT",
    "inputs": [],
    "condition": {
      "operator": "AND or OR",
      "clauses": [
        {
          "field": "...",
          "op": "EQ | IN | NOT_NULL | RAW_EXPR",
          "value": "...",
          "nullSafe": true
        }
      ]
    },
    "action": {
      "type": "CREATE_POLICY",
      "policyCode": "...",
      "applicantIndexSource": "PRIMARY_APPLICANT"
    },
    "outputPath": "application.decisionDetails.policies",
    "executionOrder": 0,
    "notes": []
  },
  "drlPreview": "..."
}

Prompt requirements:
- stage fixed as KNOCKOUT
- artifactType fixed as POLICY_RULE
- outputPath fixed as application.decisionDetails.policies
- only primary applicant for MVP
- preserve notes
- normalized input parameters must be included
- DRL preview must use fixed helper action call style, not arbitrary Java logic
- DRL preview should be text only
- return strict JSON only, no markdown
- explain valid field paths:
  application.applicant.bureau.frozenFileInd
  application.applicant.bureau.lockedFileOrWithheldIndicator
  application.applicant.bureau.noTradeInd
  application.applicant.bureau.noHitInd
  application.applicant.bureau.minorIndicator
  application.applicant.bureau.all0300
  application.applicant.bureau.bureauErrorIndicator

4) UPDATE
src/main/java/com/wellsfargo/creditdecision/tachyon/TachyonKnockoutTranslationService.java

Change interface method to:
KnockoutTranslatedArtifact translate(KnockoutPolicySheetRow row, KnockoutSheetMetadata metadata);

5) UPDATE
src/main/java/com/wellsfargo/creditdecision/tachyon/TachyonKnockoutTranslationServiceImpl.java

Requirements:
- use KnockoutRowNormalizer
- use KnockoutPromptBuilder
- use TachyonClient
- use Jackson ObjectMapper

Flow:
a) normalize row
b) build prompt
c) if credit.tachyon.mock-enabled=true:
      return mock translated artifact for Q18 and D22
   else:
      call tachyonClient.translateRule(prompt)
      parse JSON into KnockoutTranslatedArtifact

Mock mode requirements:
- keep only as safe fallback
- build KnockoutTranslatedArtifact with:
  sourceSheetName
  sourceRuleId
  sourceRowNumber
  canonicalArtifact
  drlPreview

Q18 mock canonicalArtifact:
- stage KNOCKOUT
- artifactType POLICY_RULE
- scope APPLICANT
- applicantFilter primaryInd = 1
- ruleName "Credit Bureau Frozen or Locked (Q18)"
- policyCode Q18
- policyCategory KNOCKOUT
- outputPath application.decisionDetails.policies
- executionOrder 10
- condition OR:
  - frozenFileInd EQ true
  - lockedFileOrWithheldIndicator EQ true
- action CREATE_POLICY Q18

Q18 mock drlPreview should be something like:
rule "KNOCK_1_Q18"
agenda-group "knockout"
salience 10
when
    // primary applicant knockout condition
then
    // create policy Q18 through helper
end

D22 mock canonicalArtifact:
- stage KNOCKOUT
- artifactType POLICY_RULE
- scope APPLICANT
- applicantFilter primaryInd = 1
- ruleName "No Trade No Hit (D22)"
- policyCode D22
- policyCategory KNOCKOUT
- outputPath application.decisionDetails.policies
- executionOrder 20
- condition AND:
  - frozenFileInd EQ false
  - lockedFileOrWithheldIndicator EQ false
  - all0300 RAW_EXPR "(all0300 != null && (all0300 == 0 || all0300 == 99)) || noTradeInd == true || noHitInd == true || minorIndicator == true"
- action CREATE_POLICY D22
- preserve workbook note:
  knockout runs before imputations; use raw all0300 and check 0/99, not imputed -99

D22 mock drlPreview should be text only.

If unsupported policy code in mock mode, throw UnsupportedOperationException with clear message.

IMPORTANT:
- outputPath must always be application.decisionDetails.policies
- no runtime hardcoded logic here except mock fallback path
- default path should be real Tachyon translation

6) CREATE
src/main/java/com/wellsfargo/creditdecision/authoring/service/KnockoutRulePersistenceService.java

Purpose:
Save/load translated knockout artifacts as JSON file for MVP.

File path for MVP:
src/main/resources/generated-rules/knockout/knockout-translated-artifacts.json

Methods:
- void saveArtifacts(List<KnockoutTranslatedArtifact> artifacts)
- List<KnockoutTranslatedArtifact> loadArtifacts()

Requirements:
- use Jackson ObjectMapper
- create directories if missing
- if file not found on load, return empty list
- add TODO that DB can replace file persistence later

7) CREATE
src/main/java/com/wellsfargo/creditdecision/authoring/service/KnockoutRuleAuthoringService.java

Purpose:
Orchestrate authoring/import for knockout sheet.

Method:
List<KnockoutTranslatedArtifact> importFromWorkbook(Path workbookPath)

Flow:
a) open workbook with Apache POI
b) get sheet "Knockout Calcs & Policy"
c) extract metadata using KnockoutSheetMetadataExtractor
d) extract rows using KnockoutPolicySheetExtractor
e) translate rows using TachyonKnockoutTranslationService
f) sort artifacts by canonicalArtifact.executionOrder ascending, nulls last
g) save artifacts using KnockoutRulePersistenceService
h) return artifacts

Only process actual policy rows.
Do not do runtime evaluation here.

8) CREATE
src/main/java/com/wellsfargo/creditdecision/engine/rule/KnockoutRuleSetProvider.java

Interface:
List<CanonicalKnockoutRule> getRules();

9) CREATE
src/main/java/com/wellsfargo/creditdecision/engine/rule/JsonBackedKnockoutRuleSetProvider.java

Spring @Component.

Inject:
- KnockoutRulePersistenceService

Behavior:
- load translated artifacts from JSON file
- return only artifact.getCanonicalArtifact() values
- filter nulls safely
- no hardcoded rules here
- if file missing, return empty list

10) CREATE
src/main/java/com/wellsfargo/creditdecision/engine/rule/PolicyFactory.java

Spring @Component.

Method:
Policy create(CanonicalKnockoutRule rule, int applicantIndex)

Map:
- sourceRuleId <- rule.getArtifactId()
- ruleName <- rule.getRuleName()
- policyCode <- rule.getPolicyCode()
- policyCategory <- rule.getPolicyCategory()
- policyApclntIndex <- applicantIndex
- executionOrder <- rule.getExecutionOrder()

11) CREATE
src/main/java/com/wellsfargo/creditdecision/engine/rule/CanonicalRuleEvaluator.java

Spring @Component.

This is the generic runtime evaluator for knockout canonical rules.

Method:
boolean matches(CanonicalKnockoutRule rule, DecisionContext context, Applicant applicant, int applicantIndex)

Support:
A) applicantFilter
- if "primaryInd = 1", applicant.primaryInd must be 1
- blank filter means allow

B) condition operator
- AND
- OR

C) clause operator
- EQ
- IN
- NOT_NULL
- RAW_EXPR (temporary for current translated rows only)

D) field resolution from Applicant/Bureau for these field paths:
- application.applicant.bureau.frozenFileInd
- application.applicant.bureau.lockedFileOrWithheldIndicator
- application.applicant.bureau.noTradeInd
- application.applicant.bureau.noHitInd
- application.applicant.bureau.minorIndicator
- application.applicant.bureau.all0300
- application.applicant.bureau.bureauErrorIndicator

E) RAW_EXPR support
- no scripting engine
- no SpEL
- no reflection library
- implement temporary helper for current knockout RAW_EXPR shape using applicant.getBureau() values directly
- add TODO comment that nested canonical condition structure should replace RAW_EXPR later

Keep all helper methods inside this one file:
- matchesApplicantFilter
- matchesCondition
- matchesClause
- resolveFieldValue
- evaluateRawExprForCurrentKnockoutCases
- parseBooleanIfPossible
- parseIntegerIfPossible
- isBlank

NO policyCode branching in this evaluator.

12) CREATE
src/main/java/com/wellsfargo/creditdecision/engine/stage/KnockoutStage.java

Spring @Component.

Inject:
- KnockoutRuleSetProvider
- CanonicalRuleEvaluator
- PolicyFactory

Method:
void execute(DecisionContext context)

Behavior:
- if context or application null, return
- ensure application.decisionDetails exists
- ensure decisionDetails.policies exists
- load rules from provider
- sort by executionOrder ascending
- iterate applicants in application.getApplicant()
- for each rule evaluate generically with CanonicalRuleEvaluator
- if matched:
    create Policy
    add to decisionDetails.policies
- avoid duplicate same policyCode + applicantIndex
- if any policy added:
    context.setStopProcessing(true)
    context.setStopReason("KNOCKOUT")

No hardcoded Q18/D22 logic in stage.

13) UPDATE
src/main/java/com/wellsfargo/creditdecision/engine/context/DecisionContext.java

Ensure fields exist:
- Application application
- boolean stopProcessing
- String stopReason
with getters/setters

14) UPDATE
src/main/java/com/wellsfargo/creditdecision/domain/DecisionDetails.java

Ensure field exists:
- List<Policy> policies = new ArrayList<>();

15) UPDATE if needed
src/main/java/com/wellsfargo/creditdecision/domain/Policy.java

Ensure fields exist:
- String sourceRuleId
- String ruleName
- String policyCode
- String policyCategory
- Integer policyApclntIndex
- Integer executionOrder

Keep class compact.

16) CREATE
src/test/java/com/wellsfargo/creditdecision/authoring/service/KnockoutRuleAuthoringServiceTest.java

Test should:
- use workbook from src/test/resources
- call importFromWorkbook(...)
- assert artifacts are returned
- assert Q18 and D22 are present
- assert drlPreview is present for Q18 and D22
- assert generated JSON file exists and is non-empty

17) CREATE
src/test/java/com/wellsfargo/creditdecision/engine/stage/KnockoutStageTest.java

Test should:
- first call KnockoutRuleAuthoringService.importFromWorkbook(...) in mock-enabled mode
- then build Application + Applicant + Bureau manually
- then run KnockoutStage.execute(context)

Tests:
a) frozenFileInd=true -> Q18 policy created, stopProcessing true
b) not frozen/not locked and all0300=99 -> D22 created
c) non-primary applicant with primary-only filter -> no policy
d) duplicates should not be added twice
e) provider returns empty -> no crash, no policies

18) CREATE optional local helper
src/main/java/com/wellsfargo/creditdecision/authoring/service/KnockoutImportRunner.java

Simple method:
void runImport(Path workbookPath)

No CommandLineRunner wiring required.

==================================================
CONFIGURATION
==================================================

If needed, add/update properties usage for:
- credit.tachyon.mock-enabled=true
- credit.tachyon.base-url=...
- credit.generated-rules.knockout.path=src/main/resources/generated-rules/knockout/knockout-translated-artifacts.json

Use @Value or @ConfigurationProperties, whichever is simpler and compact.
Keep it minimal.

==================================================
IMPORTANT PROMPT CONTENT FOR TACHYON
==================================================

In KnockoutPromptBuilder, the prompt must clearly say:

- You are translating one row from the "Knockout Policy" worksheet of a credit decision workbook.
- Return strict JSON only.
- Return an object with:
  - sourceSheetName
  - sourceRuleId
  - sourceRowNumber
  - canonicalArtifact
  - drlPreview
- canonicalArtifact.outputPath must be application.decisionDetails.policies
- stage must be KNOCKOUT
- artifactType must be POLICY_RULE
- scope must be APPLICANT
- applicantFilter should be primaryInd = 1 for MVP
- preserve notes
- use only allowed field paths from the field catalog
- DRL preview must be text only and use fixed helper-action style
- no markdown
- no explanations
- JSON only

Also include the normalized row data in the prompt:
- sheetName
- rowId
- rowNumber
- ruleName
- policyCode
- policyCategory
- normalized input parameters
- formulaExpression
- notes
- metadata instructions

==================================================
CODING STYLE
==================================================
- No Lombok
- Java 17 compatible
- Use constructor injection
- Keep code compact
- No overengineering
- Do not create extra helper classes beyond files listed
- Keep imports compile-clean
- Use Jackson ObjectMapper
- Use Apache POI
- Use JUnit 5

==================================================
AFTER IMPLEMENTATION
==================================================
At the end, provide:
1. list of created/updated files
2. short explanation of what gets saved and where
3. short explanation of authoring flow vs runtime flow
4. assumptions about Tachyon API integration
5. note that runtime currently uses canonical JSON + generic evaluator while drlPreview is produced for visibility/demo and future Drools integration
