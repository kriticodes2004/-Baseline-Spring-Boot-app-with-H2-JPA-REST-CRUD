Now implement the "Application Policy Rules" sheet on top of the Policy Tables stage that already exists.

GOAL
Implement Application Policy Rules as a Tachyon-authored, canonical-artifact, DRL-rendered, Drools-executed stage.

This stage SHOULD create actual Policy objects and append them to application.decisionDetails.policies.

DESIGN DECISION
- Policy Tables = Java evaluator
- Application Policy Rules = Tachyon pipeline + DRL runtime
Do not change that design.

CURRENT CONTEXT
The project now already has:
- integrated evaluation pipeline
- ExecutionContext
- current working stages before this:
  1. input
  2. global calcs
  3. knockout
  4. error scenarios
  5. initialize & impute
  6. risk tier
  7. policy tables
- drools support already exists or is already used by knockout
- Tachyon caller support already exists or can be reused
- We want one more stage after Policy Tables called ApplicationPolicyRulesStage

WHAT THIS SHEET DOES
This sheet contains actual application-level policy rules for the PRIMARY applicant only.
It references:
- raw applicant/application fields
- risk tier outputs
- policy table outputs
- helper calculations
- grouped rulesets

Examples from the sheet:
GLOBAL:
- T73
- T75
- T76
- T77
- T78
- T50
- T51

INDUSTRY-HI/PS gated rules:
- execute only when industryCode in ('12','13')
- then evaluate T56, T57, T64 etc

SCD:
- execute policy tables tblDecisionStrategyProgram, tblDecisionStrategyDeviation, tblSCDPolicy
- if scdInd = 1 then create policy SCD

RECESSION:
- execute tables tblT94Program + tblT94Policy
- execute tables tblT95Program + tblT95Policy
- execute tables tblT96Program + tblT96Policy
- execute tables tblT97Program + tblT97Policy
- execute tables tblT98Program + tblT98AltMinFico
- then create policies T94/T95/T96/T97/T98 depending on indicators

BUREAU FRAUD:
- helper calculation: ratio_use0300all0300
- then policies FP1, FP2, FP3, FP4

IMPORTANT RULES
1. PRIMARY applicant only for MVP.
2. This stage runs AFTER Policy Tables.
3. It consumes derived values already produced earlier:
   - riskTier
   - custXficoRsnCd
   - t50MinFico
   - t51Ind
   - t94Program
   - t94Ind
   - t95Program
   - t95Ind
   - t96Program
   - t96Ind
   - t97Program
   - t97Ind
   - t98Program
   - t98MinFico
   - decisionStrategy
   - scdInd
4. This stage creates actual Policy objects.
5. Keep rule-authoring sheet-driven and extensible.

IMPLEMENTATION TO BUILD

1. Excel extractor
Create:
com.wellsfargo.creditdecision.apppolicy.excel.ApplicationPolicyRulesExcelExtractor

Responsibilities:
- read workbook path
- open sheet "Application Policy Rules"
- extract rows with:
  - ruleId
  - ruleSet
  - ruleName
  - inputParameters
  - formulaOrExpression
  - policyCode
  - locationInBom
  - notes
- also detect execution condition markers like:
  - if industryCode IN ('12','13') then execute the INDUSTRY-HI/PS ruleset
  - end execution condition
- detect helper calculation rows like:
  - Calc_use0300_all0300_Ratio

2. Raw row model
Create:
com.wellsfargo.creditdecision.apppolicy.model.ApplicationPolicyRawRow

Fields:
- rowNumber
- ruleId
- ruleSet
- ruleName
- inputParametersRaw
- formulaRaw
- policyCode
- locationInBom
- notes
- rowType (POLICY_RULE / HELPER_CALC / EXECUTION_CONDITION_START / EXECUTION_CONDITION_END)

3. Normalization service
Create:
com.wellsfargo.creditdecision.apppolicy.excel.ApplicationPolicyRuleNormalizationService

Responsibilities:
- normalize extracted rows
- split input parameters into list
- attach rules to their group/ruleset
- identify execution-gated rule groups
- mark helper calculations separately
- validate rows before Tachyon
- reject blank or unusable rows cleanly
- preserve traceability to source row number and raw formula

4. Canonical artifact models
Create:
com.wellsfargo.creditdecision.apppolicy.model.canonical

Files:
- CanonicalApplicationPolicyArtifact.java
- CanonicalHelperCalculation.java
- CanonicalRuleGroup.java
- CanonicalExecutionGate.java
- CanonicalApplicationPolicyRule.java
- CanonicalConditionNode.java
- CanonicalPolicyAction.java

Suggested structure:
CanonicalApplicationPolicyArtifact:
- sheetType
- sheetName
- List<CanonicalHelperCalculation> helperCalculations
- List<CanonicalRuleGroup> ruleGroups
- List<String> warnings

CanonicalRuleGroup:
- groupName
- executionGate
- List<CanonicalApplicationPolicyRule> rules

CanonicalHelperCalculation:
- calcId
- outputName
- inputs
- expression
- scope

CanonicalApplicationPolicyRule:
- ruleId
- ruleName
- policyCode
- inputs
- conditionTree
- action
- notes
- sourceRowNumber
- ruleGroupName

CanonicalPolicyAction:
- type = CREATE_POLICY
- target = application.decisionDetails.policies
- policyCode
- policyCategory inferred or passed
- ruleName

5. Prompt builder
Create:
com.wellsfargo.creditdecision.apppolicy.tachyon.ApplicationPolicyPromptBuilder

Build a compact prompt, not a huge one.
The prompt must clearly instruct Tachyon:
- Sheet type = APPLICATION_POLICY_RULES
- Scope = PRIMARY_ONLY
- Output = valid canonical JSON only
- Do not wrap output in markdown
- Do not invent fields
- Preserve source row numbers
- helper calculations must be emitted separately
- execution conditions must become rule-group gates
- table references like tblT50MinFICO / tblT51Policy / tblT94Program etc are NOT to be redefined; they are already resolved by prior Java policy-table stage, and should be referenced as derived inputs
- output must be DRL-renderable
- formulas should use application/applicant/derivedValues naming consistently

Also include compact context for known field mapping:
- primFico
- primCustomScore
- riskTier
- industryCode
- use0300
- all0300
- all8220
- all8321
- all2327
- pil8120
- pil0438
- iqt9416
- iqt9425
- iqt9426
- reh5030
- wfccbkcy
- wfccstld
- wfccfore
- wfccrepo
- wfccchof
- noTradeInd etc if used

6. Tachyon translation service
Create:
com.wellsfargo.creditdecision.apppolicy.tachyon.ApplicationPolicyTranslationService

Responsibilities:
- accept normalized rows
- build prompt
- call shared Tachyon client
- parse JSON response
- map to CanonicalApplicationPolicyArtifact
- keep raw prompt + raw response for traceability
- fail safely if response malformed

7. Artifact validation service
Create:
com.wellsfargo.creditdecision.apppolicy.validation.ApplicationPolicyArtifactValidationService

Validate:
- policy rules have policyCode when action is CREATE_POLICY
- helper calculations have outputName
- groups are not null
- condition trees are not empty
- execution gates are structurally valid
- reject bad artifacts before DRL render

8. DRL renderer
Create:
com.wellsfargo.creditdecision.apppolicy.drools.ApplicationPolicyDroolsRenderer

Responsibilities:
- render valid DRL from canonical artifact
- helper calculations must be rendered first
- group execution gates must be honored
- individual rules create Policy using PolicyCreationService
- append policies to application.decisionDetails.policies
- use ExecutionContext as inserted fact
- use primary applicant only
- read policy-table derived values from context.getDerivedValue(...)
- renderer must support nested AND/OR condition tree
- renderer must support operators:
  EQ, NE, LT, LTE, GT, GTE, IN, NOT_IN, NOT_NULL, BETWEEN
- render clean readable DRL

9. Persistence / startup refresh
Create:
com.wellsfargo.creditdecision.apppolicy.bootstrap.ApplicationPolicyBootstrapService

Behavior:
- on app startup, refresh Application Policy Rules sheet from Excel
- extract -> normalize -> Tachyon translate -> validate -> render DRL
- cache:
  - canonical artifact
  - raw prompt
  - raw response
  - rendered DRL
- property examples:
  - credit.app-policy.sheet-name=Application Policy Rules

10. Runtime stage
Create:
com.wellsfargo.creditdecision.apppolicy.runtime.ApplicationPolicyRulesStage

Behavior when /decision/evaluate is called:
- stage runs AFTER PolicyTablesStage
- use cached DRL artifact from startup refresh
- execute DRL against current ExecutionContext
- policies created by DRL must be appended to application.decisionDetails.policies
- no separate endpoint needed for main pipeline, but optional debug endpoint is okay

11. Policy creation service alignment
Ensure Application Policy DRL uses a proper service for policy construction instead of manually new-ing half-baked policy objects.

If a PolicyCreationService already exists, reuse it.
Otherwise create or extend:
com.wellsfargo.creditdecision.policy.PolicyCreationService

Need method like:
createApplicationPolicy(
    String policyCode,
    String ruleName,
    String policyCategory,
    Integer applicantIndex,
    String sourceRuleId
)

Policy should populate fields needed downstream:
- policyCode
- policyCategory
- policyDescription if available
- policyApclntIndex
- maybe policyRank later if known
- source rule metadata if useful

12. Integration into main pipeline
Wire stage order like this:
1. input
2. global calcs
3. knockout
4. error scenarios
5. initialize & impute
6. risk tier
7. policy tables
8. application policy rules
9. later stages continue

13. Optional debug endpoints
If easy, create:
- /authoring/application-policy/excel
returns extraction + canonical artifact + DRL preview
- /runtime/application-policy/execute
executes just this stage for debugging

But main requirement is integration into /decision/evaluate.

14. Tests
Create tests for:
- helper calc ratio_use0300all0300
- T50 using t50MinFico derived value
- T51 using t51Ind
- industry gate for HI/PS rules
- SCD using scdInd
- recession rules using t94/t95/t96/t97/t98 values
- bureau fraud rules FP1/FP2/FP3/FP4
- no-policy scenario
- DRL render validity test if possible

15. Keep MVP scope sane
For MVP:
- primary applicant only
- application-level rules only
- do not implement offer/plan-level rules from later sheets here
- do not over-engineer dynamic registries
- but keep artifact format extensible

IMPORTANT CONSTRAINTS
- Do not rewrite or break current knockout/error/risk tier stages
- Do not convert Policy Tables to Tachyon
- Do not hardcode each application policy as separate Java service
- Use Tachyon -> canonical JSON -> DRL -> Drools runtime
- Keep traceability:
  - workbook path
  - sheet name
  - source row number
  - raw prompt
  - raw response
  - rendered DRL

EXPECTED OUTCOME
After startup refresh, hitting /decision/evaluate should:
- execute earlier stages
- execute Policy Tables Java stage
- execute Application Policy Rules DRL stage
- produce final policies in application.decisionDetails.policies

Now implement all required files, startup refresh, validation, DRL rendering, stage integration, and tests for Application Policy Rules.
