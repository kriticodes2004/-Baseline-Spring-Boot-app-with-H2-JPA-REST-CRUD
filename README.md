Implement the next stage: Global Calcs.

Important:
- Build on top of the existing Input Data + Initialize & Impute implementation.
- Do NOT implement knockout logic.
- Do NOT implement error scenarios.
- Do NOT implement risk tier tables.
- Do NOT implement Drools.
- Do NOT implement workbook parsing.
- Do NOT redesign the architecture.

This stage is small and should only add the visible Global Calcs fields from the sheet.

==================================================
GOAL
==================================================

Implement Global Calcs so that after Initialize & Impute, the system sets:

1. creditDecisionDate
2. rulesVersion

Both belong under:
application.decisionDetails

Do not add anything else from later stages.

==================================================
FILES TO ADD
==================================================

Create:

src/main/java/com/example/creditdecision/service/GlobalCalcsService.java

==================================================
FILES TO UPDATE
==================================================

Update:

src/main/java/com/example/creditdecision/model/DecisionDetails.java
src/main/java/com/example/creditdecision/api/DecisionController.java
src/main/java/com/example/creditdecision/service/InputNormalizationService.java

Do not modify other files unless required for compilation.

==================================================
1) UPDATE DecisionDetails.java
==================================================

Add these fields to DecisionDetails:

- String creditDecisionDate
- String rulesVersion

Keep existing fields already implemented:
- primFico
- primCustomScore
- ficoReasonCodes
- customReasonCodes
- policies

Do not remove anything existing.

==================================================
2) CREATE GlobalCalcsService.java
==================================================

Create a Spring @Service in:
com.example.creditdecision.service

Add a public method:

- public void applyGlobalCalcs(ExecutionContext context)

This method must set:

A. creditDecisionDate
Rule:
- set from the current system timestamp when the calculation runs
- use UTC / Zulu time
- output as ISO-8601 string
- store in context.getDecisionDetails().setCreditDecisionDate(...)

Use Java time cleanly, for example Instant.now().toString() is acceptable.

B. rulesVersion
Rule:
- set a user-defined constant for now
- format required by sheet: POC_YYMM.XX
- use a simple hardcoded placeholder for now, for example: "POC_2504.01"
- store in context.getDecisionDetails().setRulesVersion(...)

Do not overcomplicate version generation yet.

==================================================
3) UPDATE DecisionController.java
==================================================

Inject GlobalCalcsService into the controller.

In /decision/evaluate flow:
1. call initializeImputeService.initialize(request)
2. call globalCalcsService.applyGlobalCalcs(context)
3. return a richer summary using InputNormalizationService

Do not remove validation.

==================================================
4) UPDATE InputNormalizationService.java
==================================================

Update buildInitializationSummary(...) or rename only if necessary.

It should now include:
- message -> "Input accepted, initialized, and global calcs applied"
- primaryApplicantFound
- primFico
- primCustomScore
- ficoReasonCodeCount
- customReasonCodeCount
- businessTermSetInitialized
- applicationPoliciesInitialized
- errorDetailsInitialized
- creditDecisionDate
- rulesVersion

Keep LinkedHashMap response.

==================================================
5) IMPLEMENTATION RULES
==================================================

- Keep code minimal and clean
- No business rule engine
- No later-stage logic
- No database
- No TODOs
- No placeholder stubs
- Do not create extra architecture
- Use current internal ExecutionContext and DecisionDetails

==================================================
6) EXPECTED RESPONSE AFTER IMPLEMENTATION
==================================================

After POST /decision/evaluate, the response should look like:

{
  "message": "Input accepted, initialized, and global calcs applied",
  "primaryApplicantFound": true,
  "primFico": 720,
  "primCustomScore": 154.0,
  "ficoReasonCodeCount": 3,
  "customReasonCodeCount": 2,
  "businessTermSetInitialized": true,
  "applicationPoliciesInitialized": true,
  "errorDetailsInitialized": true,
  "creditDecisionDate": "2026-04-23T12:34:56.789Z",
  "rulesVersion": "POC_2504.01"
}

Values depend on payload and runtime.

==================================================
7) AFTER CODING
==================================================

After changes:
1. ensure project compiles
2. list files changed
3. mention assumptions
4. stop there, do not continue to knockout automatically

Now implement exactly this Global Calcs stage.
