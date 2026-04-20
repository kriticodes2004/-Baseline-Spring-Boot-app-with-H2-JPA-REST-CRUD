 Continue from the existing codebase and implement the complete Error Scenarios stage incrementally without rewriting what is already generated.

Important context:
- Keep the same overall architecture already established in this project:
  - request/domain mapping
  - shared field catalog validation
  - fixed stage engine / decision context / trace model
  - authoring-to-execution pipeline where sheets are extracted, normalized, converted to DRL through Tachyon authoring support, persisted, and executed through Drools at runtime
- Preserve the existing Global Calcs and Knockout behavior.
- Runtime decisioning must remain deterministic: Tachyon is used only for authoring/generation, while persisted/generated DRL is executed later by Drools.

Goal:
Implement the Error Scenarios sheet as a full authoring-to-execution component using Tachyon + DRL + Drools, while still keeping generic system exception handling and transport/integration failures separate from sheet-authored business error rules.

Build the complete pipeline with clear separation of concerns:

1. Sheet extraction/parsing
- Add extractor/model classes for the Error Scenarios sheet.
- Parse the sheet into raw row models that capture:
  - ruleId
  - inputParameter
  - formula/expression text
  - errorId
  - errorMessage
  - errorType
  - locationInBOM if present
  - notes
- Handle blank cells, merged-cell style carry-forward patterns if present, and row skipping safely.

2. Normalization
- Convert raw sheet rows into strict normalized error rule definitions.
- Do NOT execute raw natural-language formula text directly.
- Treat:
  - rule IDs as metadata identifiers,
  - input parameters as validated field paths,
  - formula/expression text as authoring syntax that must be normalized into explicit executable structures.
- Normalize repeated authoring patterns into explicit rule components such as:
  - scope
  - iteration target / for-each target
  - filters
  - allConditions / anyConditions
  - operators
  - values
  - action = CreateError
  - error metadata
- Support patterns seen in the sheet such as:
  - for each applicant where applicant[i].primaryInd = 1
  - if field is equal to null
  - if field is equal to unavailable
  - if boolean indicator = true
  - then CreateError
- Preserve the original raw formula text for traceability.

3. Field/path validation
- Validate all referenced input/global-calc/output/BOM paths using the shared field catalog already in the project.
- Reuse existing path conventions and validation mechanisms.
- Reject or flag ambiguous/unknown paths clearly.
- Explicitly validate paths such as applicant, application.loan, merchant.planDetails, decisionDetails, and any output/error paths referenced by the sheet.

4. Typed normalized model
Create strong normalized classes for error authoring, for example:
- ErrorScenarioRow
- ErrorScenarioDefinition
- ErrorScenarioCondition
- ErrorScenarioAction
- ErrorScenarioArtifactMetadata
- Any enums needed for:
  - operator type
  - scope type
  - action type
  - error severity/type
Keep naming aligned with the existing project style.

5. Tachyon request building
Implement an Error Scenario Tachyon request builder using the same style/pattern as Knockout, but tailored for CreateError-oriented DRL generation.

Requirements:
- Build the Tachyon request programmatically from normalized error rule definitions, not from raw Excel rows and not from ad hoc manual prompts.
- Use a fixed instruction template plus structured normalized rule context.
- Include few-shot prompting if the existing project pattern supports it.
- Keep the request deterministic and explicit.
- Reuse the existing Tachyon transport/client/auth pattern already added in the repo if available.
- Do not invent a new Tachyon API style if a client already exists.

The request context should include clearly separated sections for:
- normalizedErrorRules
- availableFieldCatalog
- availableGlobalCalcMetadata if applicable
- output/error object metadata
- DRL generation instructions
- expected response schema

6. DRL generation target
Generate DRL for Error Scenario rules in the project’s house style.

Requirements for generated DRL:
- bind the correct facts already used by the runtime
- work with the existing DecisionContext / Application / Applicant and related facts
- use a stable helper/action class for CreateError behavior rather than embedding too much custom Java in rule bodies
- generate one or more rules that create structured errors on match
- preserve traceability back to ruleId and errorId
- keep runtime deterministic
- do not generate prose; generate DRL-only payload content as already done for knockout

7. Stable helper/actions for runtime
Add or extend a helper/actions class for error creation from Drools, for example something like:
- ErrorScenarioActions
- DecisionErrorActions
This helper should expose stable methods for CreateError behavior.

The helper should support creation of structured decision errors including at least:
- ruleId
- errorId
- errorMessage
- errorType
- stageName
- optional field/path context
- optional applicant/plan index where relevant
- trace metadata if appropriate

8. DRL validation
Add DRL validation before persistence, similar to the existing Knockout validation approach.
- Validate generated DRL syntax/compilation
- Validate expected helper/fact references if possible
- Fail clearly on invalid DRL

9. Artifact persistence
Persist generated Error Scenario DRL and metadata separately from Knockout artifacts.
- Add a repository/storage component similar to the existing artifact persistence approach.
- Support versioning and artifact metadata.
- Keep artifact type/stage clearly identifiable as Error Scenarios.
- Preserve ability to load persisted/generated DRL later for runtime use.

10. Runtime Drools execution stage
Add the runtime stage for Error Scenarios.
- Load persisted/generated Error Scenario DRL artifact
- Compile/reuse Drools runtime objects as appropriate
- Insert the required facts
- Execute Error Scenario rules at runtime
- Record structured errors into the domain/BOM/decision context
- Capture trace events consistently

11. Stage flow integration
Integrate Error Scenarios into the fixed stage engine cleanly.

Execution expectations:
- Preserve existing Global Calcs first.
- Preserve existing Knockout-first gating if already implemented.
- Error Scenarios should be integrated in a way consistent with the current engine design.
- If there is a business-meaningful reason to run Error Scenarios before or after Knockout based on current architecture, keep that decision explicit and document it in the summary.
- Do not break the current ability to gate later stages.

12. Important separation of concerns
Keep these separate:
- sheet-authored business error rules from Error Scenarios sheet
- generic unexpected Java/system exceptions
- external API/transport/FICO response error mappings

Meaning:
- business/authored Error Scenario rows -> normalized -> Tachyon -> DRL -> Drools
- unexpected system failures -> deterministic exception mapping, not Tachyon-authored DRL
- transport/downstream API response mapping -> integration-layer handling, not mixed into the sheet-authored rules pipeline

13. Temporary / test-only scenarios
The sheet includes temporary simulation-style error rows such as end-to-end testing triggers.
Implement support for these safely:
- allow them to be modeled and generated if present in the sheet
- but protect them with configuration/environment gating where appropriate
- make it obvious in code and summary that these are not meant to run uncontrolled in production
- preserve notes like “remove before prod deployment” as warnings/flags/metadata if possible

14. Domain/BOM integration
Extend the domain/BOM/decision model as needed so structured errors can be stored consistently.
Prefer explicit structured error objects rather than loose strings.
Ensure compatibility with existing decision details / policies / status / trace metadata patterns already in the project.

15. Tests
Add focused tests covering:
- sheet extraction
- normalization of representative rows like ERR1, ERR5, ERR6, and ERRTEMP
- field/path validation
- Tachyon request building for normalized error rules
- Tachyon response parsing for DRL content
- DRL validation
- artifact persistence
- Drools execution creating structured errors
- stage-flow integration with the fixed engine
- test/config-gated temporary simulation error scenarios

16. Constraints
- Do not rewrite the existing Global Calcs, Knockout, Tachyon client, or Drools runtime flow unless needed for clean reuse.
- Reuse existing abstractions and patterns already generated where possible.
- Do not execute raw sheet formula text directly at runtime.
- Do not use Tachyon at runtime.
- Runtime must use persisted/generated DRL only.
- Keep code production-oriented, readable, and incremental.

17. Output format
At the end, provide:
- list of files added
- list of files updated
- assumptions made
- any ambiguities in the Error Scenarios sheet
- any rows that were interpreted with heuristics
- what stage ordering decision was implemented and why

Representative examples from this sheet that should be supported:
- primary applicant annual income missing/null/unavailable -> create FICO_CRDT_1001
- primary applicant residence status missing/null/unavailable -> create FICO_CRDT_1002
- plan monthly installment missing/null/unavailable -> create FICO_CRDT_1003
- application loan amount missing/null/unavailable -> create FICO_CRDT_1004
- bureau error indicator true -> create FICO_CRDT_1005
- generic unexpected processing/system error mapping like FICO_CRDT_9000 should remain separate from authored DRL where appropriate
- temporary end-to-end simulation error like FICO_CRDT_9999 should be supported safely with config gating

Build the complete component now.
