Build the next two final stages on top of the current working codebase:

1. Final AA Mapping
2. Output mapping / final response shaping

Important: the following is already implemented and working:
- DecisionStage
- FicoReasonConversionStage
- CustomReasonConversionStage

Current pipeline near the end is:
- Stage 13: Decision
- Stage 14: FicoReasonConversionStage
- Stage 15: CustomReasonConversionStage

Now insert the next stages after those.

Do not use Tachyon.  
Do not use Drools.  
Do not hardcode Excel row mappings in Java.

---

## A. Final AA Mapping stage

Implement the Excel sheet Final AA Mapping as a sheet-driven Java mapping stage.

### Goal
Populate final:
- application.decisionDetails.adverseActions

using:
- winning / highest-priority application-level policyRsnCode
- custxFicoRsnCd
- converted FICO ACAPS reason codes
- converted Custom ACAPS reason codes

### Create
- FinalAdverseActionMappingStage
- FinalAdverseActionMappingService
- FinalAdverseActionMappingExtractor
- FinalAdverseActionMappingRow

### Extraction
Read sheet:
- Final AA Mapping

Expected columns:
- policyRsnCode
- custxFicoRsnCd
- AACodeType1
- AACodeType2
- AACodeType3
- AACodeType4

Build lookup by:
- (policyRsnCode, custxFicoRsnCd)

Validation:
- skip blank rows
- trim all strings
- allow partial AA output columns
- fail fast only if same key appears twice with conflicting content

### Runtime matching
At runtime:
1. determine winning / highest-ranked application-level policy
2. read its policyRsnCode
3. read custxFicoRsnCd
4. first try exact row match (policyRsnCode, custxFicoRsnCd)
5. if missing, fallback to (policyRsnCode, "N/A")

If no row found:
- log warning
- do not fail request
- leave adverseActions empty or minimal fallback per existing model

### Supported AA output expressions
Support only these patterns:
- policyRsnCode
- customRsnCodes[n].customAcapsRsnCode
- ficoRsnCode[n].ficoAcapsRsnCode

Do not build a generic expression engine.

### Evaluation rules
- evaluate AA columns in order 1 to 4
- blank expression -> skip
- missing indexed reason -> skip
- deduplicate final AA codes while preserving order

Populate:
- application.decisionDetails.adverseActions

If current model needs objects, create objects with resolved code.
If it uses strings, keep it aligned.

---

## B. Output mapping / final response shaping

Implement final output alignment with the Output Data sheet.

### Create
- FinalOutputMappingStage
- FinalResponseAssembler or FinalResponseBuilder

### Goal
Ensure final response JSON structure matches workbook JSON locations, especially:

#### 1. application.decisionDetails.customReasonCodes
Each item should support:
- customRsnCode
- customAcapsRsnCode
- customRsnTxtEn
- customRsnTxtSp
- customAplcntIndex

#### 2. application.decisionDetails.ficoReasonCodes
Each item should support:
- ficoRsnCode
- ficoAcapsRsnCode
- ficoRsnTxtEn
- ficoRsnTxtSp
- applicant index if model supports it

#### 3. application.decisionDetails.adverseActions
Populate from Final AA Mapping stage.

#### 4. application.trails
Support:
- entity
- description

Use trails only for lightweight processing messages. Keep simple.

### Model updates
Update models minimally only if required.
Reuse existing DTO/domain objects where possible.

Suggested minimal models if missing:
- AdverseAction
- Trail

Do not redesign the whole response contract.

---

## Pipeline order
Update final pipeline tail to:

- Stage 13: Decision
- Stage 14: FicoReasonConversionStage
- Stage 15: CustomReasonConversionStage
- Stage 16: FinalAdverseActionMappingStage
- Stage 17: FinalOutputMappingStage
- final response serialization

---

## Tests
Add tests for:

### Final AA Mapping
- exact (policyRsnCode, custxFicoRsnCd) row match
- fallback to (policyRsnCode, N/A)
- policyRsnCode expression resolution
- custom indexed reason resolution
- fico indexed reason resolution
- missing indexed reason skipped
- duplicate final AA codes removed while preserving order

### Output mapping
- final response contains:
  - application.decisionDetails.customReasonCodes
  - application.decisionDetails.ficoReasonCodes
  - application.decisionDetails.adverseActions
  - application.trails

- JSON locations align with workbook expectations

---

## Constraints
- Java only
- sheet-driven
- no Tachyon
- no DRL
- no hardcoded table row mappings
- keep implementation compact
- build on top of current working code

At the end, provide:
1. files created/updated
2. final end-of-pipeline stage order
3. assumptions made about adverseActions / trails models
4. any remaining output JSON mismatches with the workbook
