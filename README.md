 Do not modify code yet. I want a current-state execution audit of the existing pipeline exactly as implemented today.

Explain the current end-to-end flow class-by-class and stage-by-stage, including:
1. request mapping from DecisionRequest into domain objects,
2. current fixed stage order and gating behavior,
3. what Global Calcs does today and what outputs it writes,
4. how Knockout is authored, normalized, sent to Tachyon, persisted, and executed in Drools,
5. how Error Scenarios is authored, normalized, persisted, and executed,
6. how Initialize/Impute is authored, normalized, persisted, and executed,
7. what facts each Drools stage inserts at runtime,
8. what helper/action classes each DRL stage uses,
9. where each stage writes outputs in Application / DecisionContext / BOM paths,
10. which rules/rows are executable vs metadata-only,
11. what is validated vs what is still assumed,
12. what parts are proven by tests versus not yet business-validated.

For each stage, clearly show:
- input data read,
- normalized model produced,
- Tachyon request payload shape used,
- DRL artifact generated/loaded,
- Drools facts inserted,
- outputs written,
- trace/gating behavior.
  Now inspect the existing runtime execution for each authored Drools stage and confirm whether rules are actually being executed, not just generated and persisted.

For Knockout, Error Scenarios, and Initialize/Impute, explain:
- where the runtime DRL artifact is loaded from,
- how the KieBase/session is created or reused,
- which facts are inserted,
- which helper/action objects are inserted,
- how rule firing is triggered,
- how outputs are written back,
- how we know from current tests that rules fired successfully.
