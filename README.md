
Build on top of the current codebase and implement the next stage: ApplicationPolicyRulesStage.

Use Java only for this stage.
Do not use Tachyon.
Do not use DRL.
Do not use Drools.
Do not modify previous stage behavior unless required for integration.

Goal:
Implement the “Application Policy Rules” sheet as an extensible Excel-driven Java stage.

Required behavior from this sheet:
- apply rules only for the primary applicant
- support Rule ID, RuleSet, Rule Name, Formula/Expression, Policy Code, Location In BOM, Notes
- support grouped rulesets such as GLOBAL, INDUSTRY-*, RECESSION, BUREAU FRAUD, SCD
- support “Execution Condition” rows that activate/deactivate a ruleset
- support rows that say “Execute tables 'X' and 'Y'”
- support rows with direct conditional checks and then Create Policy
- support temporary/intermediate calculated rows such as Calc_use0300_all0300_Ratio
- support policy creation using the policyCode column and configured BOM path
- preserve deterministic ordered evaluation
- keep the stage extensible so new rows of the same sheet structure can be added without code changes

Implement these pieces:
1. ApplicationPolicyRulesSheetExtractor
2. normalized row/model classes for:
   - executable policy rows
   - execution-condition rows
   - table-invocation rows
   - temporary-calculation rows
3. ApplicationPolicyRowNormalizer
4. ApplicationPolicyRulesEvaluator
5. ApplicationPolicyRulesStage
6. any helper classes needed for:
   - named table invocation
   - temporary value storage
   - policy creation / dedupe
   - trace logging

Important execution semantics:
- this stage must build on current outputs already available from prior stages, especially:
  - GlobalCalcs
  - Initialize & Impute
  - RiskTier
  - PolicyTables
- rows must be processed in sheet order
- execution-condition rows must control whether a ruleset is evaluated
- table-invocation rows must call existing table/program evaluators by table name, not by hardcoded row-specific logic
- direct condition rows must evaluate against application/applicant fields and prior derived values
- temporary calculated rows must write stage-local or context-local values that later rows can reference
- Create Policy rows must add policy objects to application.decisionDetails.policies
- keep primary-applicant-only behavior for MVP

Extensibility requirements:
- do not hardcode current rule IDs like T73, T75, FP1, etc. into evaluator branches
- do not hardcode current ruleset names into if/else logic
- do not hardcode current table names into evaluator logic beyond generic named dispatch
- new rows of the same structure should work without code changes
- new rules within an existing ruleset should work without code changes
- new execution-condition rows of the same pattern should work without code changes

Supported expression features to implement now:
- equality and inequality
- numeric comparisons: <, <=, >, >=
- range-style comparisons already represented in normalized form
- IN / NOT IN style list checks
- null / blank / unavailable handling
- boolean true/false checks
- “then Create Policy”
- “Execute tables 'X' and 'Y'”
- “initialize tempValue = ...”
- simple arithmetic expression for temp calculated fields like ratio = toFloat(a) / toFloat(b) with zero-check guard handled by normalized logic

Do not over-engineer.
Implement directly.
Show files added/updated.
Add focused tests for:
- extraction
- normalization
- execution condition gating
- table invocation rows
- temp calculation rows
- policy creation rows
- policy dedupe behavior if duplicate policy codes occur
- integration with current codebase

Also add developer-visible trace/debug logging for:
- extracted rows
- normalized row type
- activated/skipped rulesets
- invoked tables
- temp values created
- matched policy rows
- created policies
- final stage summary
