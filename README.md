Do not summarize architecture.

I want hard verification only.

For the current knockout implementation, show me these exact things:

1. Does the project compile right now? If not, list exact compile errors with file names and line numbers.
2. Is /authoring/knockout/excel runnable right now? If not, show the exact failing point.
3. Is /runtime/knockout/execute runnable right now? If not, show the exact failing point.
4. Give me one exact sample application payload that should trigger Q18.
5. Give me one exact sample application payload that should trigger D22.
6. Give me one exact sample application payload that should trigger no knockout.
7. For each of the three cases above, tell me the exact expected output in decisionDetails.policies.
8. If any part is unverified, explicitly say UNVERIFIED instead of assuming it works.

Do not give me a conceptual explanation.
Do not describe intended flow.
Only report verified state, concrete payloads, concrete outputs, and concrete errors.
