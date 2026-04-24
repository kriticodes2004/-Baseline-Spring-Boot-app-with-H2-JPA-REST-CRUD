
Refactor ATP Calcs so it is NOT hardcoded row-by-row.

Goal:
Make ATP Calcs sheet-driven in Java, not Tachyon, not DRL, not separate if/else methods per ATP rule.

What to build:
1. ATP Excel extractor for sheet "ATP Calcs"
   - dynamically read valid rows
   - capture sourceRowNumber, ruleId, attributeName, inputAttributesRaw, logicRaw, bomLocation, notes
   - skip blank rows
   - reject malformed rows safely

2. ATP normalization service
   - convert extracted rows into canonical calculation definitions
   - support:
     - initialize variable
     - arithmetic expressions
     - min()
     - round to 2 decimals
     - conditional logic
     - null checks
     - references to application fields, current plan fields, temp variables, prior outputs
   - preserve raw logic + row number for traceability

3. Generic ATP runtime evaluator
   - sort application.merchant.planDetails by monthlyInstallment ascending
   - loop through each plan
   - evaluate canonical calc definitions generically
   - write outputs into merchant.planDetails.planDecisionDetails
   - no hardcoding like if ATP3 / ATP4 / ATP5 / ATP6

4. Field resolver
   - support paths like:
     - application.loan.loanAmount
     - application.decisionDetails.monthlyIncome
     - application.decisionDetails.finalBureauTotMonthlyPmt
     - merchant.planDetails.monthlyInstallment
     - temp.someValue
   - null safe

5. Traceability
   - for each ATP row capture:
     - sourceRowNumber
     - ruleId
     - raw logic
     - resolved input values
     - computed output
     - warnings/errors

6. ATP service integration
   - add AtpCalcsService into main /decision/evaluate pipeline
   - run ATP after loan assignment strategy
   - skip safely if no plan details exist

7. Tests
   - extraction test
   - normalization test
   - runtime evaluator test
   - integration test

Important:
- Do NOT use Tachyon
- Do NOT use Drools
- Do NOT hardcode rule ids
- Do NOT create separate Java methods per ATP row
- Keep it extensible for future rows added to the Excel sheet

Current ATP patterns to support from the sheet:
- counterOfferBuffer = min(500, 0.10 * loanAmount)
- maxLoanAmt = min(dsrMaxLoanAmt, maxLoanAmtInit + counterOfferBuffer)
- residualIncomeGrossNew = monthlyIncome - (finalBureauTotMonthlyPmt + monthlyInstallment)
- if monthlyIncome = 0 then dsr = 999 else dsr = ((finalBureauTotMonthlyPmt + monthlyInstallment) / monthlyIncome) * 100, rounded to 2 decimals

After coding, also tell me:
- files created/updated
- assumptions made
- what is still fragile if anything
