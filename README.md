We are building a Spring Boot Java project for a credit decision stand-in service. You have NO prior context, so follow this exactly.

GOAL
Implement the "Policy Tables" sheet as a Java-driven, sheet-extracted, normalized, first-hit evaluator stage. This stage does NOT create policies directly. It computes intermediate derived outputs that are later consumed by Application Policy Rules.

CURRENT PROJECT ASSUMPTIONS
- Spring Boot Java project already exists.
- There is already an execution pipeline with stages before this:
  1. input mapping
  2. global calcs
  3. knockout
  4. error scenarios
  5. initialize & impute
  6. risk tier
- ExecutionContext already exists or can be extended.
- We want this stage integrated after risk tier and before application policy rules.
- Use Apache POI for Excel extraction.
- Keep implementation compact and sheet-driven.
- DO NOT create a separate service for every table.
- Build one generic policy-table evaluator.

SHEET BEHAVIOR TO IMPLEMENT
The Policy Tables sheet contains multiple mini-tables in one sheet.
Each table has:
- Table Name
- Pre-Execution Action
- input columns
- output columns
- ruleName output
- rows
- first-hit behavior
- some empty tables
- some default values
- some application-level tables
- some plan/offer-level tables

IMPORTANT RULES
1. Application-level tables should execute now.
2. Plan/offer-level tables should be extracted and normalized now, but may be skipped at runtime for MVP.
3. Every table is FIRST-HIT. As soon as one matching row is found, outputs are applied and that table stops.
4. Empty tables must not fail. Pre-exec defaults should still apply.
5. “N/A” in an input cell means wildcard / ANY.
6. Parse operators like:
   - <660
   - <=165
   - >=13
   - 0 <= ... <= 12
   - 2 <= ... <= 3
   - not one of (02, 11, 12, 13)
   - literal strings like Home Projects
7. Store outputs in ExecutionContext.derivedValues map.
8. Keep ruleName too if easy, but main output is derived values.
9. Use only primary applicant/application-level data for these current application-level tables.

TABLES TO SUPPORT NOW
Application-level tables to evaluate in order:
1. tblT50MinFICO
2. tblT51Policy
3. tblT94Program
4. tblT94Policy
5. tblT95Program
6. tblT95Policy
7. tblT96Program
8. tblT96Policy
9. tblT97Program
10. tblT97Policy
11. tblT98Program
12. tblT98AltMinFico
13. tblDecisionStrategyProgram
14. tblDecisionStrategyDeviation
15. tblSCDPolicy

Extract but skip runtime execution for now:
- tblT81Policy
- tblT89Program
- tblT89Policy
because those are plan/offer-level tables.

EXPECTED DERIVED OUTPUTS
The stage should be able to write values like:
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
- devProgNameDS if needed
- scdInd

IMPLEMENTATION STYLE
Create the following compact architecture.

1. Extend ExecutionContext
Add:
- Map<String, Object> derivedValues = new HashMap<>();
- helper getters and setters:
  - putDerivedValue(String key, Object value)
  - getDerivedValue(String key)
  - getDerivedInt(String key)
  - getDerivedString(String key)
  - getDerivedDouble(String key)
  - getDerivedBoolean(String key)

2. Model classes
Create under a clean package like:
com.wellsfargo.creditdecision.policytables.model

Files:
- PolicyTablesSheetDefinition.java
- PolicyTableDefinition.java
- PolicyTableRow.java
- PolicyTableCondition.java
- PolicyTableAssignment.java
- PreExecutionAssignment.java
- PolicyTableScope.java
- PolicyTableOperator.java

Suggested behavior:
PolicyTableDefinition:
- tableName
- scope
- tableOrder
- notes
- boolean firstHit
- List<PreExecutionAssignment> preExecutionAssignments
- List<String> inputColumnNames
- List<String> outputColumnNames
- List<PolicyTableRow> rows

PolicyTableRow:
- rowNumber
- List<PolicyTableCondition> conditions
- List<PolicyTableAssignment> assignments
- String ruleName

PolicyTableCondition:
- fieldName
- operator
- rawValue
- primaryValue
- secondaryValue
- List<String> multiValues

3. Excel extractor
Create:
com.wellsfargo.creditdecision.policytables.excel.PolicyTablesExcelExtractor

Responsibilities:
- open workbook by path
- open sheet "Policy Tables"
- scan entire sheet
- identify each mini-table block
- extract:
  - table name
  - pre-execution action
  - input/output columns
  - rows
  - ruleName column
  - notes if visible in table block
- detect if table is application-level or plan-level:
  - if fields use plan/offer-specific attributes like residualIncomeGrossNew / dsr at offer level, mark as PLAN
  - otherwise APPLICATION
- gracefully skip blank filler rows
- return PolicyTablesSheetDefinition

4. Normalization service
Create:
com.wellsfargo.creditdecision.policytables.excel.PolicyTablesNormalizationService

Responsibilities:
- convert raw extracted table strings into normalized operators
- parse:
  - N/A => ANY
  - <660 => LT 660
  - <=165 => LTE 165
  - >=13 => GTE 13
  - literal value => EQ
  - “0 <= ... <= 12” => BETWEEN 0 and 12
  - “2 <= ... <= 3” => BETWEEN 2 and 3
  - “not one of (02, 11, 12, 13)” => NOT_IN
- parse pre-exec actions like:
  - set t50MinFico = 660
  - set t51Ind = 0
  - set t94Program = 1
  - set decisionStrategy = STANDARD
- normalize empty tables into valid definitions with no rows

5. Field resolver
Create:
com.wellsfargo.creditdecision.policytables.runtime.PolicyTableFieldResolver

Responsibilities:
Resolve runtime values from ExecutionContext and Application for names like:
- programName
- riskTier
- marketSourceCode
- industryCode
- primFico
- primCustomScore
- pil8120
- pil0438
- all0136
- reh5030
- merchantLocationId
- masterId
- decisionStrategy if already derived
- anything already in derivedValues

This should first check derivedValues, then application/applicant fields.

6. Generic condition evaluator
Create:
com.wellsfargo.creditdecision.policytables.runtime.PolicyTableConditionEvaluator

Supports:
- ANY
- EQ
- NE
- LT
- LTE
- GT
- GTE
- BETWEEN
- IN
- NOT_IN

Handle strings and numbers safely.

7. Generic table evaluator
Create:
com.wellsfargo.creditdecision.policytables.runtime.PolicyTableEvaluator

Behavior for one table:
- apply pre-exec assignments first
- scan rows top-to-bottom
- all conditions in a row must match
- if row matches:
  - apply output assignments into ExecutionContext.derivedValues
  - apply ruleName too if useful
  - stop because first-hit
- if no row matches, keep pre-exec defaults

8. Stage service
Create:
com.wellsfargo.creditdecision.policytables.runtime.PolicyTablesStage

Responsibilities:
- accept ExecutionContext
- load extracted+normalized policy tables
- execute only APPLICATION scope tables in the exact order listed above
- skip PLAN tables for MVP
- write derived values into context

9. Loader / bootstrap
Create one service that can refresh this sheet from Excel at app startup.
For example:
com.wellsfargo.creditdecision.policytables.bootstrap.PolicyTablesBootstrapService

Behavior:
- read workbook path from application.properties
- extract + normalize Policy Tables sheet on startup
- cache normalized definitions in memory
- expose getDefinitions()

Property examples:
credit.workbook.path=...
credit.policy-tables.sheet-name=Policy Tables

10. Integration
Wire PolicyTablesStage into the existing /decision/evaluate pipeline AFTER risk tier and BEFORE application policy rules.
Do not break current working pipeline.

11. Tests
Create targeted unit tests for:
- operator parsing
- range parsing
- not-one-of parsing
- pre-exec assignment parsing
- first-hit table behavior
- empty table behavior
- tblT50MinFICO default/override behavior
- tblDecisionStrategyProgram default/override behavior
- tblSCDPolicy indicator setting

12. Keep code clean
- No massive God class
- No per-table hardcoded services
- Reuse generic evaluator
- Use enums where appropriate
- Use comments sparingly but clearly
- Prefer compact but production-shaped code

EXPECTED END RESULT
When the pipeline runs, Policy Tables stage should produce derived values in context, ready for Application Policy Rules stage. No policy objects are created here.

IMPORTANT
After generating code:
- also update controller/service wiring if needed
- ensure project compiles
- ensure stage order is correct
- do not remove existing knockout/error/risk tier logic

Now implement all files and wiring for this Policy Tables stage.
