 Continue from the existing codebase and implement the Initialize & Impute sheet incrementally without rewriting what is already generated.

Implement this stage using the same authoring-to-execution pattern already used for Knockout and Error Scenarios:
1. extract and parse the Initialize & Impute sheet into raw row models,
2. normalize rows into strict imputation / derived-attribute rule definitions,
3. validate source and target paths using the shared field catalog,
4. build a structured Tachyon request from normalized Initialize/Impute definitions,
5. generate DRL for deterministic BTS / imputed attribute assignment behavior,
6. validate and persist the generated DRL artifact and metadata,
7. execute the persisted/generated DRL through Drools at runtime,
8. write derived BTS / imputed attributes into a stable domain/context location for later stages to consume.

Important modeling rules:
- treat derived attribute names like BTS.all0000 as output targets
- treat input parameters like all0000 as validated source field paths
- treat formula/expression text as authoring syntax that must be normalized into explicit scope / filter / condition / transformation / action structures before Tachyon generates DRL
- do not execute raw sheet formula text directly at runtime
- preserve traceability to sheet row / rule id
- use stable helper/action methods for assigning derived values rather than embedding too much Java logic directly in DRL

Support repeated patterns seen in the sheet such as:
- for each applicant where applicant[i].primaryInd = 1
- if input is null or unavailable then assign -1
- else if input > threshold then assign transformed negative value
- else assign original value

Implementation requirements:
- keep extraction, normalization, Tachyon request construction, DRL validation, artifact persistence, runtime Drools evaluation, and stage integration as separate concerns
- preserve existing Global Calcs, Knockout, and Error Scenarios behavior
- integrate Initialize & Impute into the current fixed engine in the intended stage order
- extend the domain/BOM model and shared field catalog as needed for BTS / derived attributes so later stages like Risk Tier Tables and Policy Tables can consume them cleanly
- add focused tests for extraction, normalization, representative imputation rows, DRL generation, runtime assignment behavior, and stage integration
- at the end, clearly list files added/updated, assumptions made, and any ambiguous rows interpreted heuristically
- For this stage, generate DRL that performs derived attribute assignment through a stable InitializeImputeActions / BtsAttributeActions helper API, not CreateError and not knockout decision actions.
