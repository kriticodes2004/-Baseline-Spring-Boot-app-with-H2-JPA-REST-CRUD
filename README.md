Now stop adding new architecture and make the knockout flow work end to end.

Focus only on these tasks:

1. Verify /authoring/knockout/excel returns valid artifact + valid rendered DRL.
2. Verify /runtime/knockout/execute can compile that DRL and execute it against an application payload.
3. Ensure ExecutionContext, DecisionDetails, Policy, Applicant, Bureau, and BusinessTermSet align exactly with the field paths used in generated DRL.
4. Ensure PolicyCreationService is used by DRL instead of repeated inline policy object construction wherever possible.
5. Ensure renderer supports operators EQ, NE, IN, NOT_NULL and nested AND/OR groups correctly.
6. Add or update tests / sample payloads for these 3 cases:
   - Q18 triggers
   - D22 triggers
   - no knockout triggers
7. If runtime or compilation errors exist, fix them directly.
8. After that, summarize:
   - exact files changed
   - exact bugs fixed
   - exact sample payloads used
   - actual output for each of the 3 scenarios

Do not move to other sheets yet.
Do not add new architecture beyond what is needed to make knockout work fully.
