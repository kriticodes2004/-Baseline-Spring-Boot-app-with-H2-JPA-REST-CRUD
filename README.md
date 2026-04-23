Now that knockout authoring + DRL rendering + Drools runtime is working and tests for Q18, D22, and no-knockout pass, do the next two tasks only:

1. Integrate knockout runtime into the main /decision/evaluate pipeline.
   - After initialize/impute and global calcs, execute the generated knockout DRL against the current ExecutionContext.
   - If any knockout policy is created, update application.decisionDetails.policies and stop further decision flow for now.
   - Keep the existing standalone authoring and runtime knockout endpoints unchanged.

2. Implement the next stage using the same pattern for the Error Scenarios sheet only.
   - Build Excel extraction for the Error Scenarios sheet
   - Normalize and prevalidate rows
   - Build a compact Tachyon prompt for that sheet only
   - Parse and validate returned artifacts
   - Render DRL
   - Execute via Drools runtime
   - Add unit tests for:
     - one error scenario triggers
     - one no-error scenario
     - one invalid Excel row rejected before Tachyon

Do not start risk tier tables yet.
Do not refactor the whole architecture.
Do not touch unrelated sheets.
After implementation, provide:
- files changed
- runtime order in /decision/evaluate
- sample payloads/tests added
- any assumptions made
