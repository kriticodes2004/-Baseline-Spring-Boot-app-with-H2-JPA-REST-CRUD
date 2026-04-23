Proceed in order, but implement only steps 1 and 2 first:

1. Implement RiskTierExcelExtractor extraction logic
2. Implement RiskTierTableNormalizationService normalization logic

Constraints:
- Read the actual workbook with Apache POI
- Extract both tables from the Risk Tier Tables sheet:
  - tblRiskTier
  - tblDeclineReasonCode
- Do not guess matrix values manually
- Preserve:
  - pre-exec default values
  - output field names
  - BOM locations
  - row bucket labels
  - column bucket labels
  - matrix values
- Normalization must explicitly support:
  - invalid
  - missing
  - -1
  - <= 0
  - >= 810
  - 0 <= .. <= 544
  - 1 <= .. <= 112
  - 0, missing
- Keep first-hit semantics intact
- Do not wire into main pipeline yet
- Do not refactor other stages

After implementing, provide:
- exact extracted raw structure for both tables
- exact normalized bucket interpretation
- any parsing assumptions that were required
- whether any ambiguous labels remain
