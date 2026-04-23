Do not add more broad tests yet.

I want verification against the actual workbook truth for the Risk Tier Tables sheet.

Please do these 4 things only:

1. Print the exact extracted structure for both tables:
   - tblRiskTier
   - tblDeclineReasonCode
   Include:
   - table name
   - pre-exec action/default
   - output field
   - BOM location
   - ordered column labels
   - ordered row labels
   - matrix size

2. Show exact normalization results for these bucket labels:
   - "invalid"
   - "missing"
   - "-1"
   - "<= 0"
   - "0 <= .. <= 544"
   - "1 <= .. <= 112"
   - ">= 810"
   - "0, missing"

3. Add 5 workbook-truth tests, not synthetic tests.
   Each test must:
   - use a value pair that maps to a real visible matrix cell from the workbook
   - state the expected row bucket
   - state the expected column bucket
   - state the exact expected table output
   Do this for both:
   - riskTier
   - custXficoRsnCd

4. Add one explicit first-hit / no-double-match assertion.
   The evaluator must return exactly one row and one column match.

Do not wire new runtime code yet.
Do not refactor.
After this, summarize:
- exact workbook points tested
- expected outputs
- whether any bucket labels were ambiguous
