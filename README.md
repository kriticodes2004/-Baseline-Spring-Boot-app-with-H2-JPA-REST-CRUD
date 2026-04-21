Continue from the existing codebase and implement both the Risk Tier Tables sheet and the Policy Tables sheet incrementally without rewriting what is already generated.

Implement both sheets as deterministic Java table-evaluation stages, not Tachyon.

For both sheets:
1. parse each sheet into one or more named table blocks with metadata, pre-execution actions, declared outputs/BOM locations, input columns, output columns, and ordered data rows,
2. normalize the extracted tables into strict Java models for table definition, row conditions, output assignments, defaults, and evaluation mode,
3. support cell condition patterns such as:
   - N/A as wildcard
   - exact value match
   - numeric >= / <= comparisons
   - numeric ranges / score bands
   - one-of / not-one-of lists
   - invalid / missing buckets where present
4. apply declared pre-execution defaults before row evaluation,
5. evaluate using primary-applicant data where the sheet says primary applicant only,
6. enforce FIRST-HIT behavior exactly as stated in the sheets,
7. write outputs to the exact BOM/domain/context locations,
8. preserve traceability to sheet name, table name, matched row, matched row/column band where relevant, defaults applied, inputs used, and outputs written,
9. add focused tests for extraction, normalization, condition parsing, first-hit evaluation, default handling, output writing, and stage integration.

Risk Tier Tables specifics:
- treat the sheet as matrix/band lookup logic
- parse row bands and column bands correctly
- support matrix lookup using the configured applicant-level scores
- write outputs such as riskTier and related reason-code outputs to the configured BOM locations
- preserve the exact first qualifying matrix result

Policy Tables specifics:
- treat the sheet as ordered row-based policy lookup tables
- parse multiple named table blocks such as min-fico and policy-indicator tables
- support mixed condition columns and one or more output columns
- apply pre-exec actions like initializing default outputs before first-hit row evaluation
- allow intermediate policy-table outputs to be consumed by later tables if needed

Implementation constraints:
- keep extraction, normalization, condition parsing, matrix/table evaluation, BOM writing, and tests separate
- preserve existing Global Calcs, Knockout, Error Scenarios, and Initialize & Impute behavior
- do not use Tachyon or DRL for these two sheets
- keep runtime behavior deterministic and easy to debug
- extend the shared field catalog and domain/context only as needed for clean output storage and later-stage consumption
- at the end, clearly list files added/updated, assumptions made, and any ambiguous table cells or ranges interpreted heuristically
