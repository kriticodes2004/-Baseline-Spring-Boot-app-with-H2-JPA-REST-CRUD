Continue from the existing codebase and implement the Application Policy Rules sheet incrementally without rewriting what is already generated.

Implement this sheet as a deterministic Java stage, not a Tachyon/DRL authoring pipeline.

Requirements:
1. parse the Application Policy Rules sheet into structured raw row models capturing ruleId, ruleSet, ruleName, input parameters, formula/expression text, policyCode, BOM location, and special execution-condition/helper rows,
2. normalize the sheet into strict Java rule definitions with explicit conditions, actions, execution steps, ruleset grouping, and trace metadata,
3. support expression patterns such as:
   - direct numeric/string/boolean comparisons
   - compound AND / OR logic
   - bounded comparisons like 0 < x < 170
   - IN / NOT IN style checks where needed
   - references to intermediate variables and prior stage outputs
   - execution of referenced tables such as tblT50MinFICO, tblT51Policy, t94Program, t94Policy, etc.
   - CreatePolicy actions writing to decisionDetails.policies
4. evaluate rules using applicant/application level data for the primary applicant only where specified by the sheet,
5. reuse outputs from Risk Tier Tables, Policy Tables, Initialize & Impute, Global Calcs, and prior calculated/intermediate values instead of duplicating their logic,
6. support execution-condition rows and ruleset activation/switching cleanly,
7. support helper/calc rows such as ratio or intermediate-variable setup using deterministic Java logic,
8. preserve row/ruleset order and do not assume global first-hit behavior unless explicitly stated by the sheet,
9. create structured policy objects in the domain/BOM with policyCode, ruleId, ruleName, ruleSet, source stage, and traceability,
10. integrate the stage cleanly into the fixed engine without changing existing completed stages,
11. add focused tests for extraction, normalization, referenced-table usage, representative rules like T50/T51/T94/T98 and bureau-fraud style rows, multi-policy creation behavior, execution-condition handling, and stage integration.

Implementation constraints:
- deterministic Java only
- no Tachyon and no DRL for this sheet
- keep extraction, normalization, condition parsing, table invocation/reuse, policy creation, and tests separate
- extend shared context/domain only as needed for intermediate values and policy storage
- at the end clearly list files added/updated, assumptions made, and ambiguous expressions interpreted heuristically
- For Application Policy Rules, keep all runtime behavior in Java so rule evaluation, table reuse, intermediate calculations, and policy creation are straightforward to trace and debug during payload-based testing.
