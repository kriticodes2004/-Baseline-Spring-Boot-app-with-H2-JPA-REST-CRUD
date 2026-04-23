Implement the full “Loan Assignment Strategy” sheet end-to-end in the existing Spring Boot credit decision project.

You have NO prior context. Follow these instructions exactly and build on top of the current codebase without breaking existing stages.

==================================================
GOAL
==================================================

Implement the “Loan Assignment Strategy” stage as a Java-driven, Excel-extracted, matrix/table-based runtime stage.

This stage must:

1. Extract and normalize all loan assignment strategy tables from the Excel workbook.
2. Determine the final loan strategy for the application.
3. Use the final loan strategy to select the correct max-loan-init matrix.
4. Evaluate the chosen matrix using:
   - riskTier
   - residualIncomeNet
5. Write outputs into the application decision details.
6. Integrate this stage into the already-working pipeline after PreOfferCalcs.
7. Support startup refresh so sheet artifacts are loaded when app starts.
8. Be extensible so future strategy tables or new tblMaxLoanInit_* tables can be added in Excel without Java code changes.

DO NOT use Tachyon here.
DO NOT use Drools here.
This stage must be pure Java, similar in style to Risk Tier / Policy Tables matrix evaluation.

==================================================
SHEET UNDERSTANDING
==================================================

The sheet has 3 logical parts:

----------------------------------
A. Program-level loan strategy
----------------------------------
Table: tblLoanStrategyProgram

Pre-execution action:
- set loanStrategy = H20

Columns:
- programName (input)
- loanStrategy (output)
- ruleName (output)

Example:
- Home Projects -> H00
- Outdoor Solutions -> H00

Meaning:
- Start with default loanStrategy = H20
- If merchant programName matches a row, override the default.

----------------------------------
B. Deviation-level loan strategy
----------------------------------
Table: tblLoanStrategyDeviation

Columns:
- masterId (input)
- merchantLocationId (input)
- loanStrategy (output)
- ruleName (output)
- devProgramNameLS (transient/informational)

Meaning:
- This table overrides the program-level loan strategy for specific merchant or location cases.
- Deviation match should override program match.
- If no deviation match, keep program/default result.

Override precedence must be:
1. deviation table match
2. program table match
3. default pre-exec value

For MVP:
- Support masterId and merchantLocationId matching.
- If both are present in a row, require both to match.
- If only one is populated, match on that one.
- Blank fields in a row should be treated as “not part of match”.

----------------------------------
C. Max loan init matrices
----------------------------------
Tables:
- tblMaxLoanInit_H00
- tblMaxLoanInit_H02
- tblMaxLoanInit_H20
- potentially more later

Meaning:
- Each matrix is tied to one loanStrategy.
- Once final loanStrategy is known, select the matching matrix.
- Evaluate it by:
  - row dimension = riskTier
  - column dimension = residualIncomeNet
- Output = maxLoanAmtInit

These matrices are FIRST-HIT lookup matrices.
Do not hardcode only H00/H02/H20 in runtime logic.
Instead:
- auto-discover all tables whose names start with tblMaxLoanInit_
- extract strategy suffix from table name
- keep them in a map: strategyCode -> matrixDefinition

So future tables like tblMaxLoanInit_H50 should work automatically after Excel refresh.

==================================================
RUNTIME INPUTS / OUTPUTS
==================================================

This stage runs AFTER PreOfferCalcs, so it should read:

Inputs:
- application.merchant.programName
- application.merchant.masterId
- application.merchant.merchantLocationId
- application.decisionDetails.riskTier
- application.decisionDetails.residualIncomeNet

Outputs to write:
- application.decisionDetails.loanStrategy
- application.decisionDetails.maxLoanAmtInit

If these exact domain paths differ in the current codebase, adapt to the existing domain model cleanly.

==================================================
EXPECTED RUNTIME FLOW
==================================================

Implement runtime logic exactly like this:

1. Initialize:
   - loanStrategy = default from pre-exec of tblLoanStrategyProgram (H20)
   - maxLoanAmtInit = 0

2. Evaluate tblLoanStrategyProgram:
   - first matching programName row wins
   - if matched, update loanStrategy

3. Evaluate tblLoanStrategyDeviation:
   - first matching deviation row wins
   - if matched, override loanStrategy

4. Select matrix by final loanStrategy:
   - lookup from extracted map of strategy -> matrixDefinition

5. Evaluate matrix using:
   - riskTier
   - residualIncomeNet

6. Set:
   - decisionDetails.loanStrategy
   - decisionDetails.maxLoanAmtInit

7. If no matrix exists for chosen strategy:
   - log warning
   - leave maxLoanAmtInit = 0
   - do not crash entire pipeline

==================================================
IMPLEMENTATION REQUIREMENTS
==================================================

Build this in a clean architecture matching the rest of project style.

Create / update the following kinds of classes.

----------------------------------
1. MODEL CLASSES
----------------------------------

Create model classes under appropriate package, for example:
com.wellsfargo.creditdecision.loanassignment.model
or current package structure equivalent.

Required models:

- LoanStrategyProgramRow
  Fields:
  - sourceRowNumber
  - programName
  - loanStrategy
  - ruleName

- LoanStrategyDeviationRow
  Fields:
  - sourceRowNumber
  - masterId
  - merchantLocationId
  - loanStrategy
  - ruleName
  - devProgramNameLS

- MaxLoanInitMatrixDefinition
  Fields:
  - tableName
  - strategyCode
  - sourceRowNumber
  - rowBuckets (riskTier buckets)
  - columnBuckets (residualIncomeNet buckets)
  - matrixValues
  - ruleNames if available

- LoanAssignmentArtifacts
  Fields:
  - workbookPath
  - sheetName
  - programRows
  - deviationRows
  - matricesByStrategy
  - refreshTimestamp
  - validationErrors / warnings if you already use similar pattern elsewhere

Reuse existing numeric range / matrix bucket abstractions if already present from Risk Tier Tables.
Do NOT duplicate range parsing logic if generic classes already exist.

----------------------------------
2. EXCEL EXTRACTION
----------------------------------

Create:
- LoanAssignmentExcelExtractor

Responsibilities:
- Read the workbook using Apache POI
- Locate the “Loan Assignment Strategy” sheet
- Extract:
  - tblLoanStrategyProgram
  - tblLoanStrategyDeviation
  - all tables starting with tblMaxLoanInit_
- Detect table blocks by the “Table Name” rows and related layout
- Read pre-exec values where applicable
- Capture source row numbers for traceability
- Ignore fully blank rows
- Skip invalid rows safely but record warnings/errors

Important:
- Make extractor resilient to row insertions/deletions in Excel
- Do not rely on fixed row numbers
- Identify tables by labels and headers
- If new rows are added into a table, extractor must pick them up on next refresh

----------------------------------
3. NORMALIZATION / VALIDATION
----------------------------------

Create:
- LoanAssignmentNormalizationService

Responsibilities:
- Normalize raw extracted values
- Trim strings
- Convert blank-like cells to null
- Normalize strategy names like H00/H02/H20
- Validate required fields

Validation rules:

Program table row valid if:
- programName present
- loanStrategy present

Deviation row valid if:
- loanStrategy present
- and at least one of masterId or merchantLocationId is present

Matrix valid if:
- tableName starts with tblMaxLoanInit_
- strategy suffix is present
- row buckets parsed
- column buckets parsed
- matrix numeric outputs parsed correctly

If invalid:
- skip that row/table from runtime usage
- preserve warning in artifacts/logging
- do not crash application startup unless absolutely nothing can be loaded and fail-fast is already project convention

----------------------------------
4. RANGE / BUCKET PARSING
----------------------------------

Reuse the existing Risk Tier range parsing utility if possible.

Need support for:
- exact numeric bucket rows for riskTier
- numeric range bucket columns for residualIncomeNet, such as:
  - <=2500
  - 2500 < .. <= 4000
  - 4000 < .. <= 5500
  - 5500 < .. <= 10000
  - >10000

If an existing parser already handles this style, reuse it.
Otherwise implement a focused reusable parser.

The evaluator must correctly match:
- exact riskTier integer bucket
- residualIncomeNet numeric range bucket

Matrix behavior must be FIRST-HIT.

----------------------------------
5. RESOLVER / EVALUATOR SERVICES
----------------------------------

Create:

- LoanStrategyResolver
  Methods:
  - resolveDefaultStrategy(...)
  - resolveProgramStrategy(...)
  - resolveDeviationStrategy(...)
  - resolveFinalStrategy(...)

Behavior:
- default H20 from pre-exec
- program match by merchant.programName
- deviation overrides program/default
- first-hit row semantics

- MaxLoanInitMatrixEvaluator
  Method:
  - evaluate(strategy, riskTier, residualIncomeNet, artifacts)

Behavior:
- find matrix by strategy
- match row by riskTier
- match column by residualIncomeNet
- return maxLoanAmtInit as BigDecimal or appropriate numeric type
- if no match, return zero or empty optional depending on current project style

- LoanAssignmentStrategyService
  Main orchestrator for this stage.
  Method example:
  - apply(ExecutionContext context)

Responsibilities:
- read required inputs from context/application/decisionDetails
- resolve loanStrategy
- evaluate maxLoanAmtInit
- write outputs back to decisionDetails
- log trace/debug information

----------------------------------
6. REFRESH / PROVIDER
----------------------------------

Create:
- LoanAssignmentArtifactsProvider
- LoanAssignmentRefreshService

Behavior:
- On application startup, load this sheet from configured workbook path
- parse and normalize artifacts
- cache latest artifacts in memory
- expose read access to runtime stage

Use similar pattern already used in other sheet modules if present.

Add config properties as needed, for example:
- credit.excel.workbook-path
- credit.excel.loan-assignment.sheet-name=Loan Assignment Strategy
- credit.loan-assignment.refresh-on-startup=true

Do not invent a completely different config style if one already exists.

----------------------------------
7. RUNTIME STAGE
----------------------------------

Create:
- LoanAssignmentStage

Responsibilities:
- obtain artifacts from provider
- call LoanAssignmentStrategyService
- update execution context
- integrate into the main application pipeline

Order:
This stage must run AFTER PreOfferCalcs.

If you already have a central orchestrator for stages, wire it there.
If current project uses a pipeline service, add this stage into that sequence.

----------------------------------
8. TRACEABILITY
----------------------------------

Preserve useful trace/debug info:
- chosen default strategy
- matched program row (if any)
- matched deviation row (if any)
- final strategy
- selected matrix name
- matched riskTier bucket
- matched residualIncomeNet bucket
- final maxLoanAmtInit

Do this via logs or a debug object if project already has a trace model.
Do not overengineer.

----------------------------------
9. TESTS
----------------------------------

Create tests for:

- LoanStrategyResolverTest
  Cases:
  - no program/deviation match -> default H20
  - program match -> H00
  - deviation overrides program
  - partial deviation matching behavior

- MaxLoanInitMatrixEvaluatorTest
  Cases:
  - H00 matrix lookup works
  - H02 matrix lookup works
  - H20 matrix lookup works
  - no matrix for strategy -> safe fallback
  - range boundary correctness

- LoanAssignmentExcelExtractorTest
  Cases:
  - program table extracted
  - deviation table extracted
  - tblMaxLoanInit_* tables auto-discovered
  - inserted extra rows still extracted

- LoanAssignmentStageIntegrationTest
  Build an execution context with:
  - merchant program
  - masterId / merchantLocationId
  - riskTier
  - residualIncomeNet
  Assert:
  - loanStrategy correctly set
  - maxLoanAmtInit correctly set

Use realistic sample values from sheet screenshots.

----------------------------------
10. INTEGRATION INTO CURRENT PROJECT
----------------------------------

Update the existing main decision pipeline so that when the normal application endpoint is hit, this stage also runs automatically after PreOfferCalcs.

Do not create a disconnected module.
This must be part of the integrated application flow.

If there is already an endpoint like:
- POST /decision/evaluate
keep using it.

The application should, on a normal request:
- run prior stages
- run PreOfferCalcs
- run LoanAssignmentStage
- continue downstream

----------------------------------
11. CODE QUALITY RULES
----------------------------------

- Build on existing packages and naming style
- Reuse risk-tier table parsing utilities if possible
- No hardcoded row numbers
- No hardcoded only-H00/H02/H20 evaluator logic
- No Tachyon
- No Drools
- No duplicate generic range/matrix code if already available
- Keep code compile-safe and minimal
- Prefer plain Java and Spring services
- Add comments only where genuinely useful

==================================================
DELIVERABLES
==================================================

After implementation, provide:

1. List of files created/updated
2. Short explanation of runtime flow
3. Any assumptions about merchant fields or decisionDetails fields
4. Exact sample request payload additions needed to test this stage
5. One example expected output showing:
   - loanStrategy
   - maxLoanAmtInit

Now implement the full feature.
