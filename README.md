Continue from the existing codebase and implement the Knockout Policy sheet incrementally without rewriting what is already generated.

Build this sheet as an authoring-to-execution pipeline:
1. extract and parse the Knockout Policy sheet into normalized row/models,
2. validate referenced input/global-calc/output paths using the shared field catalog,
3. generate normalized intermediate rule definitions,
4. generate DRL from those normalized rules using Tachyon-assisted authoring support,
5. persist/store the generated DRL and rule metadata for later execution,
6. wire a Drools execution stage that loads the generated DRL and executes Knockout rules first in the decision flow.

Execution requirement:
- Knockout rule execution must happen before later policy/rules stages.
- If knockout produces a fail/decline/stop decision, later policy rule execution should be skipped or gated.
- If knockout passes, the design should allow subsequent policy/rules execution stages to run later.
- Model this with clear stage ordering, gating, traceability, and decision outcomes.

Important constraints:
- keep extraction/parsing, normalization, DRL generation, persistence, and execution as separate concerns
- do not execute Tachyon at decision runtime; use it only for authoring/generation support
- runtime decisioning should execute persisted/generated DRL through Drools
- support clear rule identifiers, versioning, traceability, and validation errors
- preserve compatibility with the existing request/domain model, decision context, stage model, and field catalog
- add focused unit tests for extraction, normalization, DRL generation, knockout-first execution flow, and gating of later rules
- at the end, clearly list assumptions, files added/updated, and any ambiguous sheet rows needing clarification
