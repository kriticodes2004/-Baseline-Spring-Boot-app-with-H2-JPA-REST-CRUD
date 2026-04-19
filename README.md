You are helping me build a Wells Fargo hackathon project called:

CREDIT DECISION STAND-IN SERVICE

I want you to act like the lead backend/platform engineer and generate code, structure, and implementation guidance according to the architecture below.

Do NOT redesign this architecture unless there is a compile/runtime blocker.
Do NOT replace Drools with a custom rule engine.
Do NOT make AI the runtime decision-maker.
Do NOT hardcode business rule logic in Java when the rule should come from an Excel sheet.
Do NOT overengineer with too many tiny files.
Build incrementally sheet by sheet.

==================================================
1. PROBLEM STATEMENT
==================================================

We are building a fallback credit decisioning service that can stand in when the primary/vendor credit decision platform is unavailable, slow, constrained, or too costly.

The service must:
- accept a structured application request payload
- evaluate decision logic defined in business Excel sheets
- return deterministic output
- support policies, errors, calculations, offer-level evaluation, adverse action logic
- support explanation of how and why the decision was taken
- use AI to convert spreadsheet or natural-language rule definitions into executable rule artifacts

This is a GenAI-assisted decisioning platform.
AI is used for authoring / translation / explanation.
Runtime execution must remain deterministic.

==================================================
2. FROZEN ARCHITECTURE
==================================================

The architecture is frozen as follows:

AUTHORING / IMPORT SIDE
Excel workbook / natural language
-> Java workbook parser
-> sheet-specific row extractors
-> row normalizers
-> Tachyon AI translation
-> generated artifacts
-> save artifacts by type

RUNTIME SIDE
REST request
-> request DTO validation
-> request-to-domain mapping
-> DecisionContext
-> fixed stage engine
-> final enriched response
-> explanation/trace service

AI DOES:
- understand workbook rows
- classify row type
- convert rule rows into DRL previews / rule artifacts
- convert calc rows into structured calc definitions
- convert matrix rows into structured table configs
- convert mapping rows into mapping configs
- generate explanations from deterministic trace

AI DOES NOT:
- directly decide live credit outcomes
- replace Drools runtime
- bypass validation/governance

==================================================
3. TECH STACK
==================================================

Use this stack:

- Java 17
- Spring Boot
- Drools (DRL runtime for rule sheets)
- PostgreSQL (or H2 for local dev initially if easier, but architecture must assume PostgreSQL)
- Tachyon as AI agent / LLM integration layer
- Apache POI for Excel parsing
- Jackson for JSON
- JUnit 5 for tests
- Gradle build

Use constructor injection.
Do not use Lombok unless I explicitly ask.
Prefer clean, readable POJOs.

==================================================
4. RUNTIME EXECUTION MODEL
==================================================

Use a fixed stage execution model.

Decision request flow:
1. validate request DTO
2. map request DTO to domain Application
3. create DecisionContext
4. execute stages in fixed order
5. return enriched Application response
6. support explanation from stored trace

Fixed stage order:

1. GlobalCalcsStage                    -> Java
2. KnockoutStage                       -> Drools
3. ErrorScenarioStage                  -> Drools
4. InitializeAndImputeStage            -> Java
5. RiskTierStage                       -> Matrix/Table evaluator
6. PolicyTableStage                    -> Table evaluator
7. ApplicationPolicyStage              -> Drools
8. PreOfferStage                       -> Java
9. LoanAssignmentStrategyStage         -> Matrix/Table evaluator
10. AtpCalcStage                       -> Java
11. PlanOfferPolicyStage               -> Drools
12. OfferApprovalStage                 -> Java
13. PolicyEnrichmentStage              -> Mapping evaluator
14. ReasonCodeConversionStage          -> Mapping evaluator
15. AdverseActionStage                 -> Mapping evaluator
16. FinalDecisionStage                 -> Java

Do not keep changing this sequence unless a sheet clearly proves otherwise.

==================================================
5. SHEET CATEGORIES AND HOW TO HANDLE THEM
==================================================

A) CONTRACT / SCHEMA SHEETS
These define the API/model and are not executed as rules.

1. Input Data
Represents:
- request schema
- field names
- types
- lengths
- required flags
- JSON sections
Use for:
- request DTOs
- field catalog
- prompt grounding

2. BOM Requirements
Represents:
- path mapping between business object model and JSON
Use for:
- field normalization
- output path validation
- prompt grounding

3. Output Data (JSON)
Represents:
- output schema / enriched response model
Use for:
- domain response model
- output path validation

B) JAVA CALCULATION / ORCHESTRATION SHEETS
These are deterministic Java stages, not Drools-first.

4. Global Calcs
Use Java
Examples:
- creditDecisionDate
- rulesVersion

5. Initialize & Impute
Use Java
Examples:
- BTS fields
- primFico
- primCustomScore
- ordered reason code capture

6. Pre-Offer Calcs
Use Java
Examples:
- adjusted income
- monthly income
- housing proxy
- residual incomes
- maxDsr

7. ATP Calcs
Use Java
Examples:
- plan-level affordability
- dsr
- max loan amount
- residual income gross new

8. Determine Offer Approval
Use Java
Examples:
- set planDecisionDetails.isApproved based on policies

9. Decision
Use Java
Examples:
- final decision status
- knockout/error/approved/decline flow
- final aggregation

C) DROOLS / DRL RULE SHEETS
These must be translated into DRL and executed by Drools.

10. Knockout Calcs & Policy
Use Drools
Examples:
- Q18
- D22
- E02

11. Error Scenarios
Use Drools
Examples:
- ERR1, ERR2, etc.

12. Application Policy Rules
Use Drools
Examples:
- global policies
- industry policies
- recession policies
- bureau fraud policies

13. Plan Offer Policy
Use Drools
Examples:
- T80
- T81
- T89
- T99

D) MATRIX / TABLE SHEETS
These should become table/matrix configs, not DRL.

14. Risk Tier Tables
Use matrix evaluator
Examples:
- tblRiskTier
- tblDeclineReasonCode

15. Policy Tables
Use table evaluator
Examples:
- T50
- T51
- threshold/indicator tables

16. Loan Assignment Strategy
Use table evaluator
Examples:
- strategy routing
- max loan init table selection

E) MAPPING / ENRICHMENT SHEETS
These should become mapping configs.

17. Credit Policy Table
Use mapping evaluator
Examples:
- policy rank
- description
- category
- type
- reason code

18. FICO Rsn to ACAPS Rsn Conv
Use mapping evaluator

19. Custom Rsn to ACAPS Rsn Conv
Use mapping evaluator

20. Final AA Mapping
Use mapping evaluator

F) DOCUMENTATION / REFERENCE SHEETS
Not runtime executed.

21. Change Log
22. Flow Diagram

==================================================
6. ROLE OF TACHYON
==================================================

Tachyon is the AI authoring/translation layer.

Tachyon must be used for:
- converting worksheet rule rows into DRL previews and rule metadata
- converting calc rows into structured calc definitions
- converting matrix rows into table configs
- converting mapping rows into mapping configs
- generating explanation text from runtime trace

Tachyon is NOT the runtime executor.

Runtime executor remains:
- Drools for rule sheets
- Java for calc/orchestration
- table evaluators for matrix sheets
- mapping evaluators for mapping sheets

Implement a Tachyon client abstraction so the provider can be swapped later if needed.
Keep credentials externalized, never hardcoded.

==================================================
7. WHAT TACHYON SHOULD RECEIVE
==================================================

For each translated row, send:
- sheet name
- row number
- rule id / row id
- rule name
- input parameters
- formula / expression
- notes
- policy code if present
- normalized field paths
- stage info
- expected artifact type
- output path
- shared field catalog
- shared path catalog
- strict output schema instructions

For rule sheets, Tachyon should return:
- rule metadata
- DRL preview text

For calc sheets, return:
- structured calc definition

For table sheets, return:
- structured matrix/table config

For mapping sheets, return:
- mapping config

==================================================
8. GENERATED ARTIFACT STORAGE
==================================================

Generated artifacts should be stored by type and versioned.

For local MVP:
- save generated artifacts under resources or a local generated-rules folder

Examples:
src/main/resources/generated-rules/knockout/
src/main/resources/generated-rules/error/
src/main/resources/generated-rules/application-policy/
src/main/resources/generated-rules/plan-offer/
src/main/resources/generated-config/risk-tier/
src/main/resources/generated-config/policy-tables/
src/main/resources/generated-config/mappings/

Also keep metadata/versioning information in DB or a file-backed metadata store initially.

For local MVP, if PostgreSQL is too heavy at first, H2 is acceptable for development as long as code structure remains DB-ready for PostgreSQL.

==================================================
9. DROOLS USAGE
==================================================

Drools must be used at runtime for these rule stages:
- KnockoutStage
- ErrorScenarioStage
- ApplicationPolicyStage
- PlanOfferPolicyStage

For each Drools-backed stage:
- load generated DRL
- build / reuse KieContainer / KieBase
- create KieSession for that stage
- insert relevant facts:
  - DecisionContext
  - Application
  - Applicant
  - Bureau
  - PlanDetail
  - PlanDecisionDetails
  - BusinessTermSet
  depending on stage
- fire rules
- actions update domain objects / context

DRL should use stable facts and preferably stable helper actions, not arbitrary generated Java code in then-blocks.

==================================================
10. EXPLAINABILITY / TRACE
==================================================

The system must support:
- what decision was taken
- how it was taken
- why it was taken

So runtime should collect a deterministic trace:
- stage entered
- artifact loaded
- rule fired
- policy created
- error created
- table hit
- derived value produced

Then Tachyon can summarize that trace in natural language.

Expose:
- main decision API
- explanation API

==================================================
11. API EXPECTATIONS
==================================================

Need at least:
- POST /decision
- POST /decision/explain

Optional later:
- rule import APIs
- simulation APIs
- rule activation/update APIs

==================================================
12. PACKAGE / PROJECT STRUCTURE EXPECTATION
==================================================

Use a structure like:

com.wellsfargo.creditdecision
  controller
  service
  dto
    request
    response (optional later)
  domain
  mapper
  engine
    context
    stage
    drools
    matrix
    mapping
  authoring
    excel
      extractor
      model
    normalize
    service
  tachyon
  rules
    canonical
    generated
  repository
  config
  exception

Keep file count reasonable.
Do not generate dozens of tiny abstractions without clear value.

==================================================
13. HOW I WILL WORK WITH YOU
==================================================

I will send sheets one by one.

For each sheet, you must:
1. classify the sheet type correctly
2. explain how it fits into the frozen architecture
3. generate only the code needed for that sheet/component
4. preserve consistency with previous sheets
5. avoid redesigning everything

If a sheet is:
- rule sheet -> build extractor + Tachyon prompt/translation + DRL path + Drools runtime stage integration
- calc sheet -> build extractor if needed + Java stage
- matrix sheet -> build extractor + config + evaluator
- mapping sheet -> build extractor + config + lookup evaluator

==================================================
14. FIRST TASK WHEN I SEND A SHEET
==================================================

When I send a sheet:
- first summarize what the sheet represents
- identify whether it is:
  - schema
  - calc
  - rule
  - matrix
  - mapping
- then generate the exact files/classes needed
- prefer compile-ready code
- keep code incremental and consistent

==================================================
15. NON-NEGOTIABLES
==================================================

- Do not replace Drools with a custom generic rule engine
- Do not make AI the runtime decision-maker
- Do not hardcode business rule content in Java if it belongs in workbook-driven rules
- Do not re-architect the whole system every turn
- Do not forget Tachyon
- Do not forget explainability
- Do not forget that this is a GenAI hackathon and the AI story must be visible in authoring + explanation

==================================================
16. WHAT TO DO NOW
==================================================

Start by helping me with the first sheet/component I send next.
Before coding, always align the component with this frozen architecture.
