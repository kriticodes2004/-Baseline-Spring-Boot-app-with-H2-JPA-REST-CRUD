Proceed, but follow these constraints exactly:

1. Implement Risk Tier Tables as deterministic Java matrix evaluation only.
   - Do not use Tachyon for this sheet.
   - Do not render DRL for this sheet.
   - Do not create a generic overengineered rules framework.

2. Build only these pieces:
   - ScoreRange.java
   - MatrixTableDefinition.java
   - RiskTierTablesDefinition.java
   - RiskTierExcelExtractor.java
   - RiskTierTableNormalizationService.java
   - RiskTierMatrixEvaluator.java
   - RiskTierTablesService.java
   - tests for matrix lookup behavior
   - wire into main evaluation flow after initialize/impute

3. Parsing rules must be explicit and tested:
   - "invalid" / "missing" special bucket
   - "-1" exact bucket
   - "<= 0"
   - "0 <= .. <= 544"
   - ">= 810"
   - "0, missing"
   - first-hit behavior must be preserved

4. Do not guess matrix values manually.
   - Extract from the actual Excel workbook using Apache POI.

5. Before wiring runtime integration, first finish:
   - extractor
   - normalized model
   - evaluator
   - unit tests

6. After coding, provide:
   - files created
   - assumptions made
   - 3 to 5 passing test cases
   - where it was wired in the pipeline
