We are restarting this project from scratch and need a clean, extensible implementation.

Project problem statement:
We are building a credit decisioning service driven by an Excel workbook that acts as the business source of truth. The system must be extensible so that when business users edit/add/remove rows in the workbook, the implementation can absorb those changes with minimal code changes, as long as the sheet structure and supported rule grammar remain the same.

Core requirements:
1. Excel workbook is the source of truth for business logic and mappings.
2. The architecture must support extensibility and traceability.
3. We need stage-by-stage visibility into what is extracted, normalized, generated, executed, and written.
4. We also want explainability later, where Tachyon can be invoked to explain why a decision was made using execution traces.
5. For now, focus on implementing only the first set of stages up to Initialize & Impute.

Workbook:
- The cleaned workbook is present in this project directory.
- Refer to the workbook from the local directory while implementing.
- Treat the workbook as the source for sheet structure, mappings, and business rules.

Architecture we are following:

A. Java deterministic stages
Use Java after extraction/normalization for:
- Input Data
- BOM Requirements integration / shared path catalog / BOM mapping metadata
- Global Calcs
- Initialize & Impute

B. Tachyon authoring pipeline stages
Use normalize -> Tachyon -> DRL -> validation -> persistence -> Drools runtime for:
- Knockout Calcs & Policy
- business-authored business-error rows in Error Scenarios

C. Java-only technical/system handling
Keep in Java:
- system/unexpected exception handling
- transport/API error mapping
- retry/non-retry mapping
- alerts/technical failure handling

Execution order for the first implementation batch:
1. Request mapping / Input Data
2. BOM Requirements integration / shared field catalog
3. Global Calcs
4. Knockout Calcs & Policy
5. Error Scenarios
6. Initialize & Impute

Expected implementation style:
- do not hardcode row-by-row business rules
- use sheet extraction + normalization + generic evaluators/pipelines
- make the design expandable so more rows of the same structure can be added later from Excel without code changes
- only require code changes if a brand-new grammar/operator/sheet structure appears

Traceability / debug requirement:
For every stage, we need developer-visible tracing so we can inspect:
- what sheet rows/tables were extracted
- what normalized models were produced
- for Tachyon stages: what request payload was built, what DRL was generated/validated/persisted
- for Drools stages: what facts were inserted, what rules fired, what outputs were written
- for Java stages: what inputs were read, what outputs were written
- whether processing continued, stopped, or was skipped

Please use this context as the foundation for the implementation. Do not start implementing yet in this response; first absorb this architecture and use it consistently for the next prompts.
