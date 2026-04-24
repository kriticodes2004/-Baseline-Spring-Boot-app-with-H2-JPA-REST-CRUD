
Now review the ATP implementation strictly.

Check:
- no hardcoded ATP rule ids
- dynamic extraction
- plan sorting by monthlyInstallment ascending
- outputs go to merchant.planDetails.planDecisionDetails
- null-safe execution
- 2-decimal rounding
- traceability exists
- future Excel row additions need no Java rule method changes
Implement the “Plan Offer Policy” sheet using a direct Tachyon-to-DRL pipeline.

IMPORTANT:
Do NOT introduce canonical artifact classes for this stage.
Do NOT add an intermediate canonical JSON model layer.
This stage should be:

Excel extraction -> normalization -> compact Tachyon prompt -> DRL text -> validation -> Drools runtime execution

This is a PLAN-level policy stage.

It runs AFTER ATP Calcs.
It writes policies to:
- merchant.planDetails[i].planDecisionDetails.policies

Build on the current codebase without breaking existing stages.

==================================================
GOAL
==================================================

Implement the Plan Offer Policy sheet so that:
1. the sheet is extracted from Excel
2. valid rows are normalized
3. a compact plan-offer-specific Tachyon prompt is built
4. Tachyon returns DRL directly
5. DRL is validated and cached on startup
6. runtime executes the DRL one plan at a time
7. policies are added only to the current plan’s planDecisionDetails.policies

No canonical artifact classes.
No canonical JSON persistence layer.
Only extraction -> prompt -> DRL -> Drools runtime.

==================================================
RULES TO SUPPORT
==================================================

Implement visible rules from the sheet:

1. OFRPOL T80
- if currentPlan.planDecisionDetails.residualIncomeGrossNew < 750
  then Create Policy T80

2. OFRPOL T81
- execute support table tblT81Policy
- if t81Ind = 1
  then Create Policy T81

Inputs:
- currentPlan.planDecisionDetails.residualIncomeGrossNew
- currentPlan.planDecisionDetails.dsr
- application.decisionDetails.riskTier
- t81Ind from support table

3. OFRPOL T89
- execute support tables tblT89Program and tblT89Policy
- if t89Program = 1 and t89Ind = 1
  then Create Policy T89

4. OFRPOL T99
- if application.loan.loanAmount > currentPlan.planDecisionDetails.maxLoanAmt
  then Create Policy T99

==================================================
SUPPORT TABLES
==================================================

Implement these support tables in Java, not Tachyon:
- tblT81Policy
- tblT89Program
- tblT89Policy

These run inside the per-plan loop before Plan Offer Policy DRL executes.

Outputs:
- t81Ind
- t89Program
- t89Ind

Store these as transient per-plan derived values in the current plan runtime context.

==================================================
IMPLEMENTATION REQUIRED
==================================================

1. Excel extraction
Create:
- PlanOfferPolicyExcelExtractor

Responsibilities:
- read the “Plan Offer Policy” sheet using Apache POI
- extract valid rule rows dynamically
- do not hardcode row numbers
- capture:
  - sourceRowNumber
  - ruleId
  - ruleName
  - inputParametersRaw
  - formulaRaw
  - policyCode
  - bomLocation
  - notes
- reject malformed rows safely

Also extract support tables:
- tblT81Policy
- tblT89Program
- tblT89Policy

2. Normalization
Create:
- PlanOfferPolicyNormalizationService

Responsibilities:
- trim and clean extracted rows
- keep only valid rows for Tachyon prompt
- preserve source row number and raw formula for traceability
- normalize sheet wording into compact prompt-ready row summaries
- do not create canonical artifact classes

3. Tachyon prompt builder
Create:
- PlanOfferPolicyPromptBuilder

Prompt must be compact.
Do NOT send whole workbook.
Do NOT send unrelated sheets.

Prompt must clearly tell Tachyon:
- this is PLAN-level policy logic
- evaluate one current plan at a time
- return DRL only, no prose, no markdown fences
- available fields include:
  - application.loan.loanAmount
  - application.decisionDetails.riskTier
  - currentPlan.monthlyInstallment
  - currentPlan.planDecisionDetails.residualIncomeGrossNew
  - currentPlan.planDecisionDetails.dsr
  - currentPlan.planDecisionDetails.maxLoanAmt
  - currentPlanDerived.t81Ind
  - currentPlanDerived.t89Program
  - currentPlanDerived.t89Ind
- action must create PLAN-level policy in current plan decision details
- generated DRL must use existing ExecutionContext / current plan context and PolicyCreationService

4. Tachyon DRL generation service
Create:
- PlanOfferPolicyAuthoringService

Responsibilities:
- build prompt
- call shared Tachyon client
- receive DRL text directly
- clean fenced output if returned
- preserve raw prompt and raw response for traceability
- do not map to canonical artifact classes

5. DRL validation
Create:
- PlanOfferPolicyDrlValidationService

Responsibilities:
- validate returned DRL is not empty
- ensure expected policy codes / rule names appear
- ensure it references current plan context, not application-level policy target
- ensure it compiles with Drools before caching
- reject invalid DRL safely

6. Support table evaluator
Create:
- PlanOfferSupportTableEvaluator

Responsibilities:
- evaluate tblT81Policy, tblT89Program, tblT89Policy for the current plan
- write outputs into transient current-plan derived values:
  - t81Ind
  - t89Program
  - t89Ind

7. Runtime service
Create:
- PlanOfferPolicyRuntimeService

Responsibilities:
- loop through application.merchant.planDetails
- for each plan:
  a. initialize current plan runtime context
  b. run support table evaluator
  c. execute cached Plan Offer Policy DRL for that current plan
  d. attach created policies only to current plan’s planDecisionDetails.policies

8. Startup refresh
Create:
- PlanOfferPolicyRefreshService

Responsibilities:
- on app startup, load sheet from workbook
- extract + normalize
- generate DRL using Tachyon
- validate DRL
- cache DRL for runtime use

9. Stage integration
Create:
- PlanOfferPolicyStage

Integrate into main pipeline AFTER ATP Calcs.

==================================================
PLAN-LEVEL POLICY CREATION
==================================================

Reuse or extend PolicyCreationService so it can create PLAN-level policies.

Need support for:
- policyCode
- policyDescription / ruleName
- policyLevel = PLAN
- planIndex or plan identifier
- sourceRuleId

Policies must be added only to:
- merchant.planDetails[i].planDecisionDetails.policies

Not to application.decisionDetails.policies.

==================================================
CURRENT PLAN CONTEXT
==================================================

If current runtime model does not already support a current plan context, add a clean per-plan context object, for example:
- CurrentPlanExecutionContext
or equivalent

It should expose:
- current plan
- current plan index
- current plan derived values (t81Ind, t89Program, t89Ind)
- reference to parent application / shared execution context

Use this current-plan context during Plan Offer Policy Drools execution.

==================================================
TESTS
==================================================

Add tests for:

1. T80 triggers when residualIncomeGrossNew < 750
2. T99 triggers when loanAmount > maxLoanAmt
3. T81 support table runs first and T81 triggers only if t81Ind = 1
4. T89 support tables run first and T89 triggers only if t89Program = 1 and t89Ind = 1
5. policies attach only to the correct plan
6. two-plan request yields different plan-level outputs
7. returned DRL compiles before runtime use
8. malformed rows are rejected before Tachyon
9. startup refresh caches DRL successfully

==================================================
PIPELINE POSITION
==================================================

Current runtime order should include:
- ... previous stages ...
- Loan Assignment Strategy
- ATP Calcs
- Plan Offer Policy
- later offer approval stages

==================================================
IMPORTANT CONSTRAINTS
==================================================

- No canonical artifact classes
- No canonical JSON layer
- No hardcoded Java rules for T80/T81/T89/T99
- Use Tachyon directly to generate DRL
- Keep prompt compact
- Keep support tables in Java
- Keep runtime per-plan
- Keep traceability via sourceRowNumber, raw formula, raw prompt, raw response, cached DRL

==================================================
DELIVERABLES
==================================================

After implementation, provide:
1. files created/updated
2. startup refresh summary
3. runtime order
4. sample request payload with 2 plans
5. sample response snippet showing each plan’s planDecisionDetails and plan-level policies

Implement cleanly on top of current codebase.
