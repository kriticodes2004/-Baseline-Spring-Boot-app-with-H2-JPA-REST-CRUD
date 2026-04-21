 Continue from the existing codebase and revert the Initialize & Impute stage away from Tachyon/DRL authoring. Do not rewrite unrelated completed stages.

Implement the Initialize & Impute sheet as a deterministic Java stage, not as a Tachyon generation pipeline.

Required changes:
1. remove or stop using any Initialize & Impute Tachyon request builder / DRL generation / DRL persistence / Drools runtime pieces that were introduced for this stage,
2. keep Knockout and Error Scenarios unchanged in their current authoring-to-execution pattern,
3. re-implement Initialize & Impute as:
   - sheet extraction/parsing,
   - row normalization into strict Java models,
   - deterministic Java evaluation/execution,
   - writing BTS / imputed / derived attributes into a stable domain/context location,
   - fixed-stage engine integration,
   - focused unit tests.

Modeling rules:
- treat derived attribute names like BTS.all0000 as output targets
- treat input parameters like all0000 as validated source field paths
- treat formula/expression text as authoring syntax that must be normalized into explicit Java-executable structures
- do not execute raw sheet formula text directly
- do not use Tachyon for this stage
- do not generate DRL for this stage
- preserve traceability to row/rule id and raw formula text

Support repeated patterns seen in the sheet such as:
- for each applicant where applicant[i].primaryInd = 1
- if input is null or unavailable then assign -1
- else if input > threshold then assign transformed negative value
- else assign original value

Implement clean separation of concerns:
- InitializeImputeRow
- InitializeImputeSheetExtractor
- InitializeImputeDefinition
- InitializeImputeRowNormalizer
- InitializeImputeEvaluator
- InitializeImputeStage
- helper/writer class for BTS/derived attribute assignment if useful

Validation requirements:
- validate source and target paths using the shared field catalog
- preserve compatibility with Application, DecisionContext, and later stages
- extend the domain/BOM model only as needed for stable BTS/derived-attribute storage

Stage requirements:
- integrate Initialize & Impute into the intended fixed engine order
- preserve existing Global Calcs, Knockout, and Error Scenarios behavior
- do not change the Knockout/Error Scenarios Tachyon + DRL flow

Testing requirements:
- add focused tests for extraction
- add normalization tests for representative rows
- add evaluator tests for:
  - null/unavailable -> -1
  - threshold transform
  - copy-through case
  - primary-applicant-only filtering
- add stage integration tests
- verify full suite still passes

At the end provide:
- files added
- files updated
- files reverted or no longer used for Initialize & Impute
- assumptions made
- any ambiguous sheet rows interpreted
-  heuristically
- For Initialize & Impute, keep all runtime behavior in Java evaluator/service logic so later debugging and payload-based validation are straightforward.
