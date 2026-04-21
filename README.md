Now implement the first batch of stages incrementally, using the workbook in the current project directory as the source of truth, without rewriting unrelated code.

Implement these sheets/stages:
1. Input Data
2. BOM Requirements integration / shared field catalog
3. Global Calcs
4. Knockout Calcs & Policy
5. Error Scenarios
6. Initialize & Impute

Implementation instructions per sheet:

1. Input Data
- Implement as Java after extraction.
- Parse the sheet into structured field metadata and use it to build request/domain mapping support.
- Support nested structures like application, applicant, loan, merchant, etc.
- Reuse this to create or validate the request contract, domain mapping, and shared field/path catalog.

2. BOM Requirements
- Implement as Java metadata/config integration only.
- Use it to standardize shared field catalog paths, output/BOM locations, and naming alignment.
- Do not treat this as a runtime business-rule stage.

3. Global Calcs
- Implement as deterministic Java stage after extraction/normalization.
- Use the workbook rows to build normalized calc definitions and evaluate them in Java.
- Keep it expandable for future same-structure global calc rows.

4. Knockout Calcs & Policy
- Implement using the Tachyon authoring pipeline:
  extraction -> normalization -> Tachyon request -> DRL generation -> DRL validation -> artifact persistence -> Drools runtime execution.
- Support the FICO instructions:
  - execute Knockout Calcs first
  - then execute Knockout Policy
  - if any application policy exists after knockout execution, stop processing and mark the application declined
  - create policy via a helper/action function that can add policies into the decision details / policies structure
  - support only primary applicant for MVP
- Preserve traceability and gating.

5. Error Scenarios
- Split implementation:
  a. business-authored business errors from the sheet should use the Tachyon authoring pipeline and Drools runtime
  b. technical/system/API error mapping should remain Java/config-based and separate from authored DRL
- Use the sheet rows for missing data/business error rules such as missing applicant income, residence status, monthly installment, bureau error, etc.
- Keep generic system error codes and external API retry/non-retry handling separate and deterministic.

6. Initialize & Impute
- Implement as deterministic Java stage after extraction/normalization.
- Do not use Tachyon for this sheet.
- Use the sheet to build normalized imputation/derived-attribute definitions.
- Support repeated patterns such as:
  - for primary applicant only
  - null/unavailable -> sentinel/default assignment
  - threshold transform
  - copy-through
  - reason-code appends
  - score/model extraction
- Write results into stable BTS / imputed / decisionDetails locations so later stages can consume them.

Critical requirement: visibility / debugging
For now, add strong developer-facing visibility so I can inspect whether generation and execution are behaving correctly.

Please add:
- structured debug logging or a debug report object per stage
- for Java stages, print/log:
  - extracted rows/definitions used
  - inputs read
  - outputs written
- for Tachyon stages, print/log:
  - normalized rules
  - request payload shape/content sent to Tachyon
  - response content received
  - DRL generated
  - DRL validation result
  - artifact persistence location/version
- for Drools runtime, print/log:
  - facts inserted
  - helper/actions inserted
  - rules fired
  - outputs/policies/errors written
  - whether the stage stopped or continued execution
- for the overall engine, print/log:
  - stage execution order
  - stage result summary
  - final state summary

Also add a developer-friendly way to run and inspect the current flow, such as:
- a simulation/debug runner
- or a test harness
- or a simple endpoint/service method
so I can see the current generation/execution behavior before moving further.

Implementation constraints:
- keep extraction, normalization, validation, generation, persistence, runtime execution, and tracing separate
- preserve extensibility and avoid hardcoding row-by-row business logic
- use the workbook in the local project directory
- only implement the first batch up to Initialize & Impute for now
- keep the code clean and production-oriented
- at the end, clearly list:
  - files added
  - files updated
  - assumptions made
  - any ambiguous workbook rows interpreted heuristically
  - how to run the debug flow and inspect stage outputs
