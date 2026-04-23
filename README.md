Implement the Risk Tier Tables sheet as a deterministic Java matrix evaluator, not as a Tachyon-to-DRL sheet.

Context:
- We already have working pipelines for knockout and error scenarios.
- Risk Tier Tables is different: it contains two first-hit matrix tables based on primFico and primCustomScore for the primary applicant only.
- We should implement this sheet using direct Excel extraction + normalization + Java execution.

Tables from the sheet:
1. tblRiskTier
   - pre-exec: initialize riskTier = 99
   - output: riskTier
   - BOM location: application.decisionDetails.riskTier

2. tblDeclineReasonCode
   - pre-exec: initialize custXficoRsnCd = ""
   - output: custXficoRsnCd
   - BOM location: application.decisionDetails.custXficoRsnCd

Both tables:
- use primFico as column buckets
- use primCustomScore as row buckets
- are FIRST-HIT policy tables
- evaluate only for the primary applicant
- should be executed after initialize/impute, because primFico and primCustomScore are derived earlier

Build this in these layers:

1. Excel extraction
- Use Apache POI to read the Risk Tier Tables sheet directly
- Extract both tables from the workbook
- Extract:
  - table name
  - pre-exec default
  - output field
  - BOM location
  - primFico column bucket labels
  - primCustomScore row bucket labels
  - matrix cell values

2. Normalization
- Create a reusable range bucket model for numeric ranges
- Support labels like:
  - invalid / missing
  - -1
  - <= 0
  - 0 <= .. <= 544
  - 545 <= .. <= 580
- Normalize both row and column buckets into typed range objects

3. Execution
- Create a deterministic Java evaluator that:
  - reads primFico and primCustomScore from ExecutionContext / decision details
  - applies first-hit row matching
  - applies first-hit column matching
  - returns the intersecting cell value
- For tblRiskTier, write result to application.decisionDetails.riskTier
- For tblDeclineReasonCode, write result to application.decisionDetails.custXficoRsnCd

Implementation requirements:
- Do not use Tachyon for this sheet
- Do not render DRL for this sheet
- Keep it modular and reusable
- Reuse ExecutionContext already present in project
- Assume primary applicant only for MVP

Suggested files to create:
- model/ScoreRange.java
- model/MatrixTableDefinition.java
- model/RiskTierTablesDefinition.java
- service/RiskTierExcelExtractor.java
- service/RiskTierTableNormalizationService.java
- service/RiskTierMatrixEvaluator.java
- service/RiskTierTablesService.java
- controller/RiskTierTablesController.java (optional test/debug endpoint)

Tests to add:
1. primFico + primCustomScore returns expected riskTier
2. primFico + primCustomScore returns expected custXficoRsnCd
3. invalid/missing fico bucket test
4. no matching bucket uses default output
5. first-hit behavior test

Also wire the service into the main evaluate pipeline after initialize/impute and before downstream policy tables.

After coding, summarize:
- files created
- assumptions made for parsing bucket labels
- runtime order
- sample test cases added
