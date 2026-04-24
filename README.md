
Implement the next 2 stages in the existing Spring Boot credit decision project:

1. ATP Calcs
2. Plan Offer Policy

Build on top of the current integrated pipeline. Do NOT create disconnected demo modules. These must plug into the existing `/decision/evaluate` application flow.

==================================================
HIGH LEVEL DECISION
==================================================

Implement these 2 pages using this approach:

- ATP Calcs = pure Java stage
- Plan Offer Policy = Tachyon-authored policy stage

Reason:
- ATP Calcs is deterministic, arithmetic, and per-plan derived field generation.
- Plan Offer Policy is policy-style and should remain extensible using the Tachyon pipeline already used for rule-authoring stages.

==================================================
RUNTIME POSITION IN PIPELINE
==================================================

These stages happen AFTER:
- Initialize & Impute
- Risk Tier Tables
- Policy Tables
- Application Policy Rules
- Pre-Offer Calcs
- Loan Assignment Strategy

Required runtime sequence:

1. Read `application.merchant.planDetails`
2. Sort plans by `monthlyInstallment` ascending
3. For each plan:
   - run ATP Calcs
   - then run Plan Offer Policy
4. Write outputs under that plan’s `planDecisionDetails`
5. Continue pipeline

==================================================
ATP CALCS - WHAT TO IMPLEMENT
==================================================

The ATP sheet is per-plan and uses the sorted `merchant.planDetails[i]` list.

Implement these rows exactly as Java logic:

----------------------------------
ATP 3 - counterOfferBuffer
----------------------------------
Inputs:
- application.loan.loanAmount

Logic:
- initialize counterOfferBuffer = 0
- counterOfferBuffer = min(500, 1000, 10% of loanAmount)

Interpretation for MVP:
- implement as the minimum of:
  - 500
  - 1000
  - 0.10 * loanAmount

Since 500 is always <= 1000, the practical result may often be min(500, 0.10 * loanAmount).
Still preserve formula intent cleanly in code.

Output location:
- merchant.planDetails[i].planDecisionDetails.counterOfferBuffer

----------------------------------
ATP 4 - maxLoanAmt
----------------------------------
Inputs:
- merchant.planDetails[i].dsrMaxLoanAmt
- application.decisionDetails.maxLoanAmtInit
- merchant.planDetails[i].planDecisionDetails.counterOfferBuffer

Logic:
- initialize maxLoanAmt = 0
- maxLoanAmt = min(dsrMaxLoanAmt, maxLoanAmtInit + counterOfferBuffer)

Output:
- merchant.planDetails[i].planDecisionDetails.maxLoanAmt

----------------------------------
ATP 5 - residualIncomeGrossNew
----------------------------------
Inputs:
- application.decisionDetails.monthlyIncome
- application.decisionDetails.finalBureauTotMonthlyPmt
- merchant.planDetails[i].monthlyInstallment

Logic:
- initialize residualIncomeGrossNew = 0
- residualIncomeGrossNew =
    monthlyIncome - (finalBureauTotMonthlyPmt + monthlyInstallment)

Output:
- merchant.planDetails[i].planDecisionDetails.residualIncomeGrossNew

----------------------------------
ATP 6 - dsr
----------------------------------
Inputs:
- application.decisionDetails.monthlyIncome
- application.decisionDetails.finalBureauTotMonthlyPmt
- merchant.planDetails[i].monthlyInstallment

Logic:
- initialize dsr = 0
- if monthlyIncome == 0 then dsr = 999
- else
    dsr = ((finalBureauTotMonthlyPmt + monthlyInstallment) / monthlyIncome) * 100
- round dsr to 2 decimal places

Output:
- merchant.planDetails[i].planDecisionDetails.dsr

==================================================
PLAN OUTPUT MODEL REQUIREMENTS
==================================================

Ensure the domain model supports plan-level decision outputs.

If not already present, create/update:

- `PlanDetail`
- `PlanDecisionDetails`

`PlanDecisionDetails` should include at least:
- counterOfferBuffer
- maxLoanAmt
- residualIncomeGrossNew
- dsr
- policies (plan-level policies list)

If there is already a plan decision object, extend it instead of duplicating.

==================================================
ATP CALCS IMPLEMENTATION CLASSES
==================================================

Create or update the following:

1. `AtpCalcsService`
   Main Java service to evaluate ATP fields for one plan

2. `PlanSortingService`
   Sort `merchant.planDetails` by `monthlyInstallment` ascending

3. `AtpStage`
   Pipeline stage that:
   - sorts plans
   - loops through plans
   - initializes planDecisionDetails if missing
   - calls `AtpCalcsService` for each plan

4. Add trace logging:
   - plan index
   - monthlyInstallment
   - counterOfferBuffer
   - maxLoanAmt
   - residualIncomeGrossNew
   - dsr

Use BigDecimal where appropriate for money and percentage arithmetic.

==================================================
PLAN OFFER POLICY - WHAT TO IMPLEMENT
==================================================

This page is policy logic executed for each plan.

Rows visible in sheet:

----------------------------------
OFRPOL T80
Gross Residual Income (T80)
----------------------------------
Input:
- merchant.planDetails[i].planDecisionDetails.residualIncomeGrossNew

Logic:
- if residualIncomeGrossNew < 750
  then Create Policy T80

Policy level:
- PLAN

Output location:
- merchant.planDetails[i].planDecisionDetails.policies

----------------------------------
OFRPOL T81
Existing ATP Rule (T81)
----------------------------------
Inputs:
- merchant.planDetails[i].planDecisionDetails.residualIncomeGrossNew
- merchant.planDetails[i].planDecisionDetails.dsr
- application.decisionDetails.riskTier
- t81Ind

Logic:
- if residualIncomeGrossNew is not null
  and dsr is not null
  and riskTier is not null
  then execute table `tblT81Policy`
- if t81Ind = 1
  then Create Policy T81

Policy level:
- PLAN

----------------------------------
OFRPOL T89
Recession Rule - Capacity (T89)
----------------------------------
Inputs:
- t89Program
- t89Ind

Logic:
- execute tables `tblT89Program` and `tblT89Policy`
- if t89Program = 1 and t89Ind = 1
  then Create Policy T89

Policy level:
- PLAN

----------------------------------
OFRPOL T99
Requested Loan Amount > MaxLoan (T99)
----------------------------------
Inputs:
- application.loan.loanAmount
- merchant.planDetails[i].planDecisionDetails.maxLoanAmt

Logic:
- if loanAmount > maxLoanAmt
  then Create Policy T99

Policy level:
- PLAN

==================================================
PLAN OFFER POLICY IMPLEMENTATION STRATEGY
==================================================

Implement this page using Tachyon authoring pipeline, similar to prior extensible policy stages.

Need:

1. Excel extraction for Plan Offer Policy sheet
2. Row normalization and validation
3. Compact Tachyon context specific only to this sheet
4. Tachyon output in canonical normalized JSON
5. Runtime evaluator that executes against ONE plan at a time
6. Policy creation into `merchant.planDetails[i].planDecisionDetails.policies`

Important:
- This stage is per-plan
- Runtime context must include:
  - application-level fields
  - plan-level fields for current plan
  - any transient table outputs such as t81Ind, t89Program, t89Ind

==================================================
PLAN OFFER POLICY SUPPORT TABLES
==================================================

Also implement support for the referenced plan-level tables:

- `tblT81Policy`
- `tblT89Program`
- `tblT89Policy`

Implement them in Java matrix/table style like Policy Tables, unless your current project already has a reusable table-evaluator for these mini policy tables.

Behavior:
- these tables run inside the per-plan loop
- their outputs are transient plan-level indicators:
  - t81Ind
  - t89Program
  - t89Ind

These indicators are then used by the Tachyon-authored plan-offer rules.

Do NOT hardcode policy outcome directly if sheet says to execute a table first.
Implement the table execution as part of plan runtime.

==================================================
PLAN OFFER POLICY CLASSES
==================================================

Create or update classes like:

- `PlanOfferPolicyExcelExtractor`
- `PlanOfferPolicyNormalizationService`
- `PlanOfferPolicyPromptBuilder`
- `PlanOfferPolicyAuthoringService`
- `PlanOfferPolicyArtifact`
- `PlanOfferPolicyRuntimeService`
- `PlanOfferPolicyStage`

Also add support table services:

- `PlanOfferSupportTableExtractor`
- `PlanOfferSupportTableEvaluator`
or integrate into an existing table evaluator framework if already present.

==================================================
TACTYON PROMPT REQUIREMENTS
==================================================

Prompt must be compact and sheet-specific to avoid HTTP 504 / slow response.

Tell Tachyon:
- this is PLAN-level policy logic
- execution is only for one current plan at a time
- available fields include:
  - application.loan.loanAmount
  - application.decisionDetails.riskTier
  - currentPlan.monthlyInstallment
  - currentPlan.planDecisionDetails.residualIncomeGrossNew
  - currentPlan.planDecisionDetails.dsr
  - currentPlan.planDecisionDetails.maxLoanAmt
  - transient table outputs like t81Ind, t89Program, t89Ind
- action is CREATE_POLICY with policy code from sheet
- output location is current plan’s decision details policies list
- preserve ruleId, ruleName, policyCode, policy level PLAN, BOM path

Make sure no huge workbook context is sent.
Only send normalized valid rows for this page.

==================================================
POLICY CREATION
==================================================

Plan-level policies must be distinguishable from application-level policies.

Update policy model or factory as needed so policies can carry:
- policyCode
- policyDescription / ruleName
- policyCategory if needed
- policyLevel = PLAN
- planIndex or plan identifier
- sourceRuleId

If current policy model already supports this, reuse it.

==================================================
PIPELINE INTEGRATION
==================================================

Integrate these stages in the main evaluation pipeline:

... existing stages ...
-> LoanAssignmentStrategy
-> AtpStage
-> PlanOfferPolicyStage
-> next stages

`AtpStage` must run first because Plan Offer Policy depends on ATP outputs.

==================================================
STARTUP REFRESH
==================================================

On app startup:
- refresh / extract / normalize Plan Offer Policy artifacts
- refresh support tables used by T81/T89 if they come from this page or nearby page structure
- cache artifacts for runtime use

==================================================
TESTS TO ADD
==================================================

Create tests for ATP Calcs:

1. sort plans ascending by monthlyInstallment
2. counterOfferBuffer calculation
3. maxLoanAmt calculation
4. residualIncomeGrossNew calculation
5. dsr calculation with monthlyIncome zero -> 999
6. per-plan outputs written correctly

Create tests for Plan Offer Policy:

1. T80 triggers when residualIncomeGrossNew < 750
2. T99 triggers when loanAmount > maxLoanAmt
3. T81 executes table first, then policy if t81Ind = 1
4. T89 executes both referenced tables, then policy if both indicators = 1
5. plan policies are attached to correct plan only
6. multiple plans can produce different outcomes

Create one integration test where:
- application has multiple planDetails
- plans are sorted
- ATP values computed
- plan offer policies evaluated
- final planDecisionDetails contain expected fields and policies

==================================================
DELIVERABLES
==================================================

After implementation, provide:

1. List of files created/updated
2. Exact runtime order of ATP + Plan Offer Policy
3. Any assumptions made for plan model paths
4. Example request payload with 2 plans
5. Example response snippet showing:
   - sorted plans
   - each plan’s planDecisionDetails
   - ATP outputs
   - plan-level policies

Now implement the full feature cleanly on top of the current codebase.
