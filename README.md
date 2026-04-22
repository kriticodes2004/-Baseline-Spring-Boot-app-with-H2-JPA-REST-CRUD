You are working in an existing Spring Boot Maven project for a Credit Decision Stand-in Service.

The Input Data layer is already implemented and the `/decision/evaluate` endpoint is already accepting Postman requests successfully.

Now implement the next stage: **Initialize & Impute**.

Important:
- Generate only what is needed for Initialize & Impute.
- Do NOT implement Drools.
- Do NOT implement Tachyon.
- Do NOT implement workbook parsing.
- Do NOT implement knockout logic.
- Do NOT implement global calcs yet.
- Do NOT implement error scenarios yet.
- Do NOT implement risk tier tables yet.
- Do NOT implement final decision logic yet.
- Do NOT add database/persistence/repositories.
- Do NOT add trace logging yet.
- Do NOT redesign the existing architecture.
- Update and extend the current codebase in place.

The goal of this stage is:
1. create an internal execution context
2. initialize internal decision containers
3. identify the primary applicant
4. create and populate a Business Term Set (BTS)
5. impute BTS values for the primary applicant
6. derive early decision fields:
   - primFico
   - ficoReasonCodes
   - primCustomScore
   - customReasonCodes
7. return an initialization summary from `/decision/evaluate`

==================================================
EXISTING PROJECT CONTEXT
==================================================

Base package:
com.example.creditdecision

The project already has:
- DTO request classes
- enums
- DecisionController
- InputNormalizationService
- GlobalExceptionHandler
- PrimaryApplicantUtil
- /decision/evaluate endpoint

Do NOT remove these.
Only update what is needed.

==================================================
FILES TO ADD
==================================================

Create these files exactly:

src/main/java/com/example/creditdecision/model/ExecutionContext.java
src/main/java/com/example/creditdecision/model/BusinessTermSet.java
src/main/java/com/example/creditdecision/model/DecisionDetails.java
src/main/java/com/example/creditdecision/model/Policy.java
src/main/java/com/example/creditdecision/model/ErrorDetail.java
src/main/java/com/example/creditdecision/model/ModelReasonCode.java

src/main/java/com/example/creditdecision/service/InitializeImputeService.java

==================================================
FILES TO UPDATE
==================================================

Update these existing files:

src/main/java/com/example/creditdecision/dto/ModelDto.java
src/main/java/com/example/creditdecision/dto/BureauDto.java
src/main/java/com/example/creditdecision/service/InputNormalizationService.java
src/main/java/com/example/creditdecision/api/DecisionController.java

Do not modify other files unless absolutely necessary for compilation.

==================================================
WHAT INITIALIZE & IMPUTE MEANS HERE
==================================================

This stage is NOT just null initialization.

It must do 3 things:

A. Initialize internal runtime context
- create ExecutionContext
- store request
- store primary applicant
- initialize decisionDetails
- initialize applicationPolicies as empty list
- initialize errorDetails as empty list
- initialize businessTermSet

B. Impute Business Term Set (BTS) values for the primary applicant
These fields are sanitized versions of upstream bureau values and will be used by later rules.

C. Derive early score-related fields
- primFico
- ficoReasonCodes
- primCustomScore
- customReasonCodes

==================================================
1) UPDATE ModelDto.java
==================================================

ModelDto must no longer be empty.

Update it to contain exactly these fields:

- String name
- String score
- Boolean errorIndicator
- String aarc1
- String aarc2
- String aarc3
- String aarc4

Use Lombok @Data.

==================================================
2) UPDATE BureauDto.java
==================================================

Keep existing fields and ADD any missing fields needed by Initialize & Impute.

BureauDto must contain these Integer fields if not already present:
- reh7120
- rev7140
- use0300
- wfccbkcy
- wfccchof
- wfccfore
- wfccrepo
- wfccstld

Do not remove existing bureau fields.
Only add missing ones.

==================================================
3) CREATE model/ExecutionContext.java
==================================================

Create an internal runtime context class with Lombok @Data.

Fields:
- DecisionRequest request
- ApplicantDto primaryApplicant
- BusinessTermSet businessTermSet
- DecisionDetails decisionDetails
- List<Policy> applicationPolicies = new ArrayList<>()
- List<ErrorDetail> errorDetails = new ArrayList<>()

Package:
com.example.creditdecision.model

Imports must use existing DTO classes.

==================================================
4) CREATE model/BusinessTermSet.java
==================================================

Create a Lombok @Data class with these Integer fields:

- all0000
- all0100
- all0136
- all0300
- all2327
- all8220
- all8222
- all8321
- all9220
- brc7140
- iqt9425
- iqt9426
- pil0438
- pil8120
- reh5030
- reh7120
- rev7140
- use0300
- wfccbkcy
- wfccchof
- wfccfore
- wfccrepo
- wfccstld

Package:
com.example.creditdecision.model

==================================================
5) CREATE model/DecisionDetails.java
==================================================

Create a Lombok @Data class with:

- Integer primFico = -1
- Double primCustomScore = -1.0
- List<ModelReasonCode> ficoReasonCodes = new ArrayList<>()
- List<ModelReasonCode> customReasonCodes = new ArrayList<>()
- List<Policy> policies = new ArrayList<>()

Package:
com.example.creditdecision.model

==================================================
6) CREATE model/Policy.java
==================================================

Create a simple Lombok @Data class with:
- String policyCode
- String policyDescription

Package:
com.example.creditdecision.model

==================================================
7) CREATE model/ErrorDetail.java
==================================================

Create a simple Lombok @Data class with:
- String errorId
- String errorMessage

Package:
com.example.creditdecision.model

==================================================
8) CREATE model/ModelReasonCode.java
==================================================

Create a Lombok model with:
- Integer applicantIndex
- String code

Use:
- @Data
- @NoArgsConstructor
- @AllArgsConstructor

Package:
com.example.creditdecision.model

==================================================
9) CREATE service/InitializeImputeService.java
==================================================

Create this service in:
com.example.creditdecision.service

Use @Service.

This class must implement exactly the Initialize & Impute behavior for the current sheet.

Add this constant:
- CUSTOM_MODEL_NAME = "CORDS-retail-services-orig-risk-12855"

Public method:
- public ExecutionContext initialize(DecisionRequest request)

This method must:
1. find the primary applicant using PrimaryApplicantUtil.findPrimaryApplicant(...)
2. throw IllegalArgumentException("No primary applicant found") if not found
3. create ExecutionContext
4. set request
5. set primaryApplicant
6. initialize businessTermSet
7. initialize decisionDetails
8. call internal methods in this order:
   - imputeBusinessTermSet(context)
   - derivePrimFico(context)
   - deriveFicoReasonCodes(context)
   - derivePrimCustomScore(context)
   - deriveCustomReasonCodes(context)
9. return the context

------------------------------------------
A. BTS IMPUTATION RULES
------------------------------------------

Implement a private method:
- private void imputeBusinessTermSet(ExecutionContext context)

It must read values only from the PRIMARY applicant’s bureau object.

If bureau is null:
- set all BTS fields to -1 using a helper method
- return

Otherwise populate BTS using the following rules:

For each source field:
- if value is null -> BTS value = -1
- else if value > upperLimit -> BTS value = -1 * value
- else BTS value = value

Implement helper:
- private Integer imputeInteger(Integer value, int upperLimit)

Use these upper limits:

- all0000 -> 90
- all0100 -> 90
- all0136 -> 90
- all0300 -> 90
- all2327 -> 90
- all8220 -> 9990
- all8222 -> 9990
- all8321 -> 9990
- all9220 -> 9990
- brc7140 -> 990
- iqt9425 -> 90
- iqt9426 -> 90
- pil0438 -> 90
- pil8120 -> 9990
- reh5030 -> 999999990
- reh7120 -> 990
- rev7140 -> 990
- use0300 -> 90
- wfccbkcy -> Integer.MAX_VALUE
- wfccchof -> Integer.MAX_VALUE
- wfccfore -> Integer.MAX_VALUE
- wfccrepo -> Integer.MAX_VALUE
- wfccstld -> Integer.MAX_VALUE

Use a helper method to read integer fields from BureauDto.

You may use reflection to safely read fields if needed.
Implement:
- private Integer getIntegerField(BureauDto bureau, String fieldName)
- private Integer getIntegerFieldByReflection(Object source, String fieldName)

Also implement:
- private void setAllBtsDefaults(BusinessTermSet bts)

It must set every BTS field to -1.

------------------------------------------
B. primFico
------------------------------------------

Implement:
- private void derivePrimFico(ExecutionContext context)

Rules:
- default primFico = -1
- read primary applicant bureau fico9Score
- if null -> keep -1
- if score between 300 and 850 inclusive -> set primFico = fico9Score
- otherwise set primFico = -1

Store in:
context.getDecisionDetails().setPrimFico(...)

------------------------------------------
C. ficoReasonCodes
------------------------------------------

Implement:
- private void deriveFicoReasonCodes(ExecutionContext context)

Rules:
- from primary applicant bureau:
  - fico9AARC1
  - fico9AARC2
  - fico9AARC3
  - fico9AARC4
  - fico9AARC5
- add only codes that are:
  - not null
  - not blank
  - not equal to "unavailable" ignoring case

Store as:
List<ModelReasonCode>

Each reason code should carry:
- applicantIndex = index of the primary applicant in request.application.applicant
- code = trimmed reason code

Implement helper:
- private void addReasonCodeIfValid(List<ModelReasonCode> target, int applicantIndex, String code)

Also implement:
- private int getPrimaryApplicantIndex(DecisionRequest request)

This must return the index of the first applicant with primaryInd == 1, or -1 if not found.

------------------------------------------
D. primCustomScore
------------------------------------------

Implement:
- private void derivePrimCustomScore(ExecutionContext context)

Rules:
- default primCustomScore = -1.0
- inspect primary applicant’s models list
- only consider models where:
  - model.name equals CUSTOM_MODEL_NAME
  - model.errorIndicator is not true
  - model.score is not null/blank
- parse model.score as double
- if score >= 0, set primCustomScore to that value and stop
- if parsing fails, ignore that model
- if none match, keep -1.0

Store in:
context.getDecisionDetails().setPrimCustomScore(...)

------------------------------------------
E. customReasonCodes
------------------------------------------

Implement:
- private void deriveCustomReasonCodes(ExecutionContext context)

Rules:
- from primary applicant models
- only consider model with:
  - name == CUSTOM_MODEL_NAME
  - errorIndicator != true
- collect:
  - aarc1
  - aarc2
  - aarc3
  - aarc4
- same filtering rules as FICO reason codes:
  - not null
  - not blank
  - not "unavailable"

Store as List<ModelReasonCode> in:
context.getDecisionDetails().setCustomReasonCodes(...)

==================================================
10) UPDATE InputNormalizationService.java
==================================================

Keep the existing normalize(...) method unless changes are needed for compilation.

Add a new method:

- public Map<String, Object> buildInitializationSummary(ExecutionContext context)

This method must return a LinkedHashMap containing:

- "message" -> "Input accepted and initialized"
- "primaryApplicantFound" -> whether context.getPrimaryApplicant() != null
- "primFico" -> context.getDecisionDetails().getPrimFico()
- "primCustomScore" -> context.getDecisionDetails().getPrimCustomScore()
- "ficoReasonCodeCount" -> context.getDecisionDetails().getFicoReasonCodes().size()
- "customReasonCodeCount" -> context.getDecisionDetails().getCustomReasonCodes().size()
- "businessTermSetInitialized" -> context.getBusinessTermSet() != null
- "applicationPoliciesInitialized" -> context.getApplicationPolicies() != null
- "errorDetailsInitialized" -> context.getErrorDetails() != null

Keep imports clean.

==================================================
11) UPDATE DecisionController.java
==================================================

Update the controller so `/decision/evaluate` now does Initialize & Impute.

Requirements:
- inject InitializeImputeService
- keep InputNormalizationService
- in evaluate(...):
  1. call initializeImputeService.initialize(request)
  2. return ResponseEntity.ok(inputNormalizationService.buildInitializationSummary(context))

Do not remove @Valid request handling.

==================================================
12) WHAT NOT TO IMPLEMENT
==================================================

Do NOT implement any of the following now:
- ACAPS reason conversion tables
- tblAAConversionFico
- tblAAConversionCustom
- rulesVersion
- creditDecisionDate
- knockout logic
- error scenario logic
- risk tier logic
- policy triggering
- trace events
- plan decision logic
- final decision logic

This stage is only about Initialize & Impute.

==================================================
13) CODING REQUIREMENTS
==================================================

- Use Java 17
- Use Spring Boot style
- Use Lombok
- Keep code compilable
- Use clean imports
- No TODO comments
- No placeholder methods
- Do not generate dead code
- If reflection is used, keep it contained and safe
- Do not over-engineer

==================================================
14) EXPECTED BEHAVIOR AFTER IMPLEMENTATION
==================================================

After coding, POST /decision/evaluate should still work and now return a summary like:

{
  "message": "Input accepted and initialized",
  "primaryApplicantFound": true,
  "primFico": 720,
  "primCustomScore": 154.0,
  "ficoReasonCodeCount": 3,
  "customReasonCodeCount": 2,
  "businessTermSetInitialized": true,
  "applicationPoliciesInitialized": true,
  "errorDetailsInitialized": true
}

Values will depend on request payload.

==================================================
15) AFTER CODING
==================================================

After making the changes:
1. ensure project compiles
2. list the files created/updated
3. briefly mention any assumptions made
4. do not continue to next stage automatically

Now implement exactly this.
