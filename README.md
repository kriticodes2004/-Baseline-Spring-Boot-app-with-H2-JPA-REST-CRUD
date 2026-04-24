
Implement Determine Offer Approval and Decision as pure Java orchestration stages in the existing Spring Boot decision pipeline.

Do not use Tachyon.  
Do not use Drools.  
Do not build a generic authoring/extraction pipeline for these two sheets.

These two pages are final workflow/orchestration logic, so implement them as deterministic Java services and stages.

## Goal

After the earlier stages have already run (global calcs, knockout, error scenarios, initialize/impute, risk tier, policy tables, app policy rules, pre-offer, loan strategy, ATP, plan offer policy), implement the final two stages:

1. Determine Offer Approval
2. Decision

Both must run inside the main /decision/evaluate integrated pipeline.

---

## 1. Determine Offer Approval stage

Create:
- DetermineOfferApprovalStage
- DetermineOfferApprovalService

### Sheet logic to implement

For all merchant.planDetails:

#### OFFRAP1
Sort merchant.planDetails ascending by monthlyInstallment.

#### OFFRAP2
For each plan, initialize:
- planDecisionDetails.isApproved = true

#### OFFRAP3
For each plan:
- if any application-level policy exists with non-null / non-blank policyCode
  OR
- if that plan’s plan-level policies exist with non-null / non-blank policyCode

then:
- set planDecisionDetails.isApproved = false

Otherwise it remains true.

### Required behavior
- null-safe handling for missing merchant, planDetails, planDecisionDetails, policies
- create planDecisionDetails if absent
- preserve sorted order
- application-level policies should reject all plans
- plan-level policies should reject only that specific plan

### Helper methods
Implement clean helper methods like:
- sortPlansByMonthlyInstallment(...)
- hasTriggeredApplicationPolicy(...)
- hasTriggeredPlanPolicy(...)
- hasNonBlankPolicyCode(...)

---

## 2. Decision stage

Create:
- DecisionStage
- DecisionService

This must be implemented as ordered Java orchestration matching the Decision sheet / flowchart.

### Important interpretation
Some Excel rows contain multiple nested instructions in one paragraph.  
Do not try to dynamically interpret them.  
Instead, manually decompose them into explicit ordered Java steps.

### Decision flow to implement

#### Branch 1: Knockout triggered
If knockout is triggered:
- set application.decisionDetails.decisionStatus = "TD"
- set all offers isApproved = false
- rank existing application-level policies by policyRank
- determine adverse action from the highest-ranking policy
- stop further decision branching

#### Branch 2: Errors triggered
Else if errors are triggered:
- set application.decisionDetails.decisionStatus = "QE"
- set all offers isApproved = false
- stop further decision branching

#### Branch 3: Normal decisioning
Else:
- initialize decisionStatus = "TD"
- if any offer has planDecisionDetails.isApproved = true
  - set decisionStatus = "RA"
- else
  - copy the policies from the top-ranked offer to application.decisionDetails.policies
  - then rank application-level policies by policyRank
  - then determine adverse action from the highest-ranking policy code

### Top-ranked offer selection
Use the sorted merchant.planDetails order from Determine Offer Approval / plan sort logic.
Top-ranked offer = first plan after ascending sort by monthlyInstallment, unless current codebase already has a clearly defined helper for “best offer”.

### Policy ranking
Sort application-level policies ascending by policyRank, with null-safe behavior.
Policies without rank should go last.

### Adverse action
Determine adverse action from the highest-ranking policy code.
Use existing mapping/service if present.
If no mapping exists yet, create a small placeholder service interface so the pipeline is clean:
- AdverseActionService
- method like determineFromPolicies(List<Policy> policies)

Do not overengineer, but wire it properly.

---

## 3. Expected model usage

Use existing domain models wherever possible:
- Application
- DecisionDetails
- Merchant
- PlanDetails
- PlanDecisionDetails
- Policy

If missing, minimally add:
- decisionStatus on application decision details
- isApproved on plan decision details
- policy rank field if already modeled in sheet/domain
- adverse action field if already expected in output

Do not redesign the whole model.

---

## 4. Pipeline integration

Wire both stages into the main integrated pipeline in this order:

1. earlier stages...
2. Plan Offer Policy
3. DetermineOfferApprovalStage
4. DecisionStage
5. final response mapping

Do not create separate endpoints for these.
They must run when /decision/evaluate is called.

---

## 5. Tests to add

Add focused unit/integration tests for both stages.

### Determine Offer Approval tests
- plans sorted ascending by monthlyInstallment
- no policies anywhere -> all plans approved
- application-level policy exists -> all plans rejected
- only one plan has plan-level policy -> only that plan rejected
- null-safe creation of missing planDecisionDetails

### Decision tests
- knockout present -> decisionStatus = TD, all offers false, policy ranking executed
- errors present -> decisionStatus = QE, all offers false
- no knockout/errors and at least one offer approved -> decisionStatus = RA
- no knockout/errors and no offers approved -> copy top offer policies to application level, then rank, then adverse action
- policy ranking sorts by policyRank
- adverse action uses highest-ranking policy

---

## 6. Code quality constraints

- clean Java only
- no Tachyon
- no DRL
- no OCR-based logic
- no giant utility class
- keep methods small and explicit
- prefer service + stage pattern matching the rest of the project
- null-safe everywhere
- build on top of the current codebase, do not rewrite working earlier stages

---

## 7. Deliverables

Implement all code required for:
- Determine Offer Approval stage
- Decision stage
- pipeline wiring
- minimal supporting services/helpers
- tests

At the end, also provide a short markdown summary listing:
- files created/updated
- exact runtime order
- what assumptions were made
- any TODOs for post-MVP enhancements
