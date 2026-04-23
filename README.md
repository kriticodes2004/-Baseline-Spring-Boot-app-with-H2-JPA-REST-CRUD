Implement the next stage of the credit decision pipeline: PRE-OFFER CALCS.

Assume the project already has:
- a working Spring Boot app
- request/domain models for application, applicant, bureau, merchant, loan, decisionDetails
- integrated stage orchestration through /decision/evaluate
- previous stages already wired: input normalization, global calcs, knockout, error scenarios, initialize & impute, risk tier, policy tables, application policy rules
- primary-applicant-only MVP
- application.decisionDetails is the destination for derived outputs unless explicitly stated otherwise

IMPORTANT IMPLEMENTATION DECISION:
Pre-Offer Calcs must be implemented as JAVA-FIRST, NOT Tachyon.
Reason:
- this sheet is deterministic calculation logic, not natural-language business-policy authoring
- it contains formulas, defaults, accumulation, rounding, caps/floors, and table lookup
- we want stable runtime behavior and easy debugging
- it should still be EXTENSIBLE and SHEET-DRIVEN where practical

GOAL:
Build a sheet-driven Pre-Offer Calcs stage that:
1. extracts/normalizes pre-offer rows and the tblMaxDSR table from the Excel workbook
2. evaluates the pre-offer calculations in execution order
3. writes outputs into application.decisionDetails
4. is integrated into the main pipeline after application policy rules and before loan assignment / later offer stages
5. supports refresh on app startup the same way other extracted stages do
6. is designed so adding more rows of the same pattern later requires minimal or no code changes

==================================================
BUSINESS CONTEXT / WHAT THIS SHEET DOES
==================================================

This sheet computes application-level pre-offer financial fields before plan/offer level stages.

Current visible rules from the sheet include at least:

PreOffer 1a: tmpAdtnIncome
- initialize tmpAdtnIncome = 0
- if primary applicant exists
- and primaryApplicant.income.additionalIncomes is not null
- and > 0
- then tmpAdtnIncome = primaryApplicant.income.additionalIncomes
- output location: application.decisionDetails
- note: if additionalIncomes missing/null, proxy/default remains 0

PreOffer 1b: adjustedIncome
- initialize adjustedIncome = 0
- if primary applicant exists
- and primaryApplicant.income.totalAnnualIncome is not null
- and not unavailable
- then adjustedIncome =
  primaryApplicant.income.totalAnnualIncome
  - tmpAdtnIncome
  + (tmpAdtnIncome * 1.25)
- round adjustedIncome to nearest hundredths (2 decimals)
- output location: application.decisionDetails

PreOffer 2: monthlyIncome
- initialize monthlyIncome = -1
- monthlyIncome = adjustedIncome / 12
- round monthlyIncome to nearest hundredths (2 decimals)
- output location: application.decisionDetails

PreOffer 3: housingProxy
- initialize housingProxy = 0
- initialize tempProxyApplies = 0
- for primary applicant only:
  if residenceStatus == "RENT"
  OR (residenceStatus == "OTHER" AND actMtgTradeInd == false)
  then:
    tempProxyApplies = 1
    housingProxy = housingProxy + (0.15 * monthlyIncome)
- round housingProxy to nearest hundredths

PreOffer 4: housingProxy floor/cap
- if tempProxyApplies == 1 and housingProxy < 250 then housingProxy = 250
- if tempProxyApplies == 1 and housingProxy > 4000 then housingProxy = 4000

PreOffer 5: finalBureauTotMonthlyPmt
- initialize application.finalBureauTotMonthlyPmt = 0 or application.decisionDetails.finalBureauTotMonthlyPmt depending on current model convention; prefer decisionDetails unless existing domain already has this at application level
- sum applicant[i].bureau.bureauTotMonthlyPmt for applicants where value is not null and not unavailable
- then if housingProxy is not null, add housingProxy
- round to nearest hundredths if needed
- output should be placed consistently in the project’s chosen BOM/domain location

PreOffer 6: residualIncomeGross
- initialize residualIncomeGross = 0
- if monthlyIncome and finalBureauTotMonthlyPmt are not null
- residualIncomeGross = monthlyIncome - finalBureauTotMonthlyPmt
- round to nearest hundredths
- output location: application.decisionDetails

PreOffer 7: residualIncomeNet
- initialize residualIncomeNet = 0
- if monthlyIncome and finalBureauTotMonthlyPmt are not null
- residualIncomeNet = (0.80 * monthlyIncome) - finalBureauTotMonthlyPmt
- round to nearest hundredths
- output location: application.decisionDetails

PreOffer 8: maxDsr
- execute table tblMaxDSR
- set maxDsr from that table
- output location: application.decisionDetails.maxDsr

PreOffer 9: maxLoanAmtInit and loanStrategy
- this is delegated to Loan Assignment Strategy tab
- do not fully implement the next tab here
- only create a hook/interface for later delegation so PreOffer stage can call a loan assignment service once that tab is implemented

Use only PRIMARY applicant logic where sheet implies primary applicant for MVP.
Do not add secondary-applicant strategy now.

==================================================
ARCHITECTURE TO BUILD
==================================================

Create the following package structure under the existing base package, adapting package names to the project:

src/main/java/.../preoffer/model
src/main/java/.../preoffer/extractor
src/main/java/.../preoffer/service
src/main/java/.../preoffer/controller (only if there is an authoring/debug endpoint pattern already in the project)
src/test/java/.../preoffer

Implement these classes/interfaces:

1. PreOfferCalcRow.java
- normalized representation of a calc row
- fields:
  - ruleId
  - attributeName
  - inputAttributes (List<String>)
  - rawLogic
  - bomLocation
  - notes
  - executionOrder
  - valid
  - rejectionReason
- keep immutable or use Lombok if project already uses it

2. MaxDsrTableDefinition.java
- representation of tblMaxDSR
- fields:
  - tableName
  - preExecutionDefault
  - outputBomLocation
  - list of row definitions

3. MaxDsrTableRow.java
- fields:
  - rowLabel / sourceLabel
  - riskTierRange
  - outputValue
  - ruleName

4. NumericRange.java
- generic helper for parsing labels like:
  - 1 <= .. <= 7
  - 8
  - >= 9
  - <= 0
  - etc
- support inclusive min/max boundaries
- support exact-value rows

5. PreOfferExtractionResult.java
- workbookPath
- sheetName
- instructions
- calcRows
- maxDsrTable
- rejectedRows

6. PreOfferExcelExtractor.java
- use Apache POI
- read the Pre-Offer Calcs sheet directly from the workbook path
- dynamically find the calc grid and tblMaxDSR table; do not hardcode row numbers if avoidable
- extraction should survive inserted rows as long as headers/titles remain recognizable
- detect:
  - rule id
  - attribute name
  - input attributes
  - logic
  - BOM location
  - notes
- also extract tblMaxDSR and its ranges/outputs
- validate required columns
- reject malformed rows rather than crashing
- empty rows should be skipped
- return PreOfferExtractionResult

7. PreOfferNormalizationService.java
- normalize extracted rows into canonical internal form
- trim whitespace
- split inputAttributes into clean list
- standardize BOM paths
- standardize logic text formatting
- parse/validate tblMaxDSR labels into NumericRange
- mark invalid rows with rejectionReason
- do not use OCR
- this is workbook-based extraction, not screenshot parsing

8. MaxDsrTableEvaluator.java
- given riskTier, return maxDsr from tblMaxDSR
- first-hit semantics
- if no row matches, leave default from pre-exec
- support exact values and ranges

9. PreOfferCalculationSupport.java
- helper math methods:
  - safeBigDecimal
  - isUnavailable
  - round2
  - percentage multiplication
  - null-safe subtraction/addition
- centralize numeric handling with BigDecimal where appropriate

10. PreOfferLoanAssignmentGateway.java
- interface only for now
- method like:
  LoanAssignmentResult execute(ExecutionContext context);
- create a no-op placeholder implementation for now returning nulls/defaults so pre-offer can compile and integrate without implementing the next tab yet

11. PreOfferCalcsService.java
- main runtime evaluator
- takes ExecutionContext
- reads primary applicant from context/application
- executes rules in order:
  a. tmpAdtnIncome
  b. adjustedIncome
  c. monthlyIncome
  d. housingProxy / tempProxyApplies
  e. floor/cap
  f. finalBureauTotMonthlyPmt
  g. residualIncomeGross
  h. residualIncomeNet
  i. maxDsr via table evaluator
  j. delegate hook to loan assignment gateway
- writes outputs into decisionDetails and/or agreed domain locations
- do not compute by fragile string-eval
- implement as typed Java methods, but driven by extracted metadata for traceability
- retain execution trace / audit entries where project already supports traceability

12. PreOfferRefreshService.java
- on startup refreshes extracted/normalized pre-offer artifacts from workbook path configured in properties
- store normalized artifacts in memory cache or file cache, consistent with existing project style
- if workbook missing, fail gracefully with clear logs
- if refresh fails, app should not crash unless project already intentionally fails startup

13. PreOfferArtifactsProvider.java
- gives current normalized extracted artifacts to runtime service
- analogous to provider pattern used elsewhere if present

14. PreOfferStage.java
- integrate with overall stage orchestration
- wrapper around PreOfferCalcsService
- updates context/stage trace/status

15. PreOfferDebugController.java (optional but recommended if consistent with existing project)
- endpoint to inspect extracted and normalized pre-offer artifacts
- something like POST /authoring/preoffer/excel
- request body contains workbook path + sheet name
- response contains extraction result for debugging

==================================================
DOMAIN / OUTPUT MAPPING
==================================================

Use existing domain models if already present. Add missing fields only if needed.

Ensure DecisionDetails has or can hold:
- tmpAdtnIncome
- adjustedIncome
- monthlyIncome
- housingProxy
- tempProxyApplies
- finalBureauTotMonthlyPmt
- residualIncomeGross
- residualIncomeNet
- maxDsr
- optionally maxLoanAmtInit
- optionally loanStrategy

If some of these already exist elsewhere in the project, do not duplicate them. Reuse current domain placement.
But keep BOM alignment as close as possible to the sheet and current project conventions.

==================================================
RUNTIME INTEGRATION
==================================================

Integrate this into the main /decision/evaluate pipeline:
- after Policy Tables and Application Policy Rules if that is the current stage order in the project
- before Loan Assignment Strategy / ATP / Plan Offer Policy
- do not break existing full integration endpoint

When app starts:
- refresh pre-offer artifacts from workbook
- runtime evaluation should use the refreshed artifacts

When /decision/evaluate is hit:
- request should pass through already integrated stages
- then PreOfferStage should run automatically
- response/debug state should show derived pre-offer outputs

==================================================
VALIDATION RULES
==================================================

Implement validation carefully:
- if row is blank, skip
- if rule id missing but row is clearly not a real rule, skip
- if logic missing for a rule row, reject row
- if BOM location missing, keep warning but allow if runtime mapping is explicit
- if tblMaxDSR labels are malformed, reject that row and record reason
- never crash the whole extractor because one row is bad

==================================================
TESTS TO WRITE
==================================================

Write unit tests for:

1. PreOfferExcelExtractorTest
- extracts calc rows from workbook
- extracts tblMaxDSR rows
- skips blank rows
- handles additional inserted rows

2. NumericRangeTest
- parse exact values
- parse closed range
- parse >= range
- parse <= range

3. MaxDsrTableEvaluatorTest
- riskTier 5 -> expected maxDsr from table
- riskTier 8 -> expected maxDsr
- riskTier 10 -> expected maxDsr
- unmatched -> default

4. PreOfferCalcsServiceTest
Use realistic application payloads and assert:
- tmpAdtnIncome from additional incomes
- adjustedIncome formula with 1.25 factor
- monthlyIncome /12 rounded
- housingProxy for RENT
- housingProxy for OTHER + no mortgage trade
- housingProxy floor 250
- housingProxy cap 4000
- finalBureauTotMonthlyPmt sums bureau pmt + proxy
- residualIncomeGross correct
- residualIncomeNet correct
- maxDsr assigned from riskTier table

5. PipelineIntegrationTest
- /decision/evaluate runs through integrated pipeline and includes pre-offer outputs in final response/context

==================================================
CODING RULES
==================================================

- Use existing project style and package conventions
- Prefer BigDecimal for financial calculations
- Use clear, small methods
- No giant god class
- No OCR
- No Tachyon for this sheet
- No hardcoding specific workbook row numbers unless absolutely necessary; anchor off table labels/headings
- Adding new rows to the sheet should be picked up on refresh if they match existing patterns
- Keep logging clean and useful
- Keep traces/debug output available for verification

==================================================
DELIVERABLE EXPECTATION
==================================================

Generate all code files, tests, and integration wiring needed so that:
1. Pre-Offer Calcs can be refreshed from the workbook on startup
2. /decision/evaluate runs the pre-offer stage automatically
3. derived outputs are written to decisionDetails
4. extraction + normalization + runtime evaluation are testable and debuggable
5. implementation is Java extensible and sheet driven

If some exact domain field names differ from current codebase, adapt to the existing project rather than creating conflicting duplicates.
Proceed file by file and include all required classes and tests
