Now implement only the next 3 parts using the same workbook and existing architecture, without rewriting the earlier completed parts:

4. Knockout Calcs & Policy
5. Error Scenarios
6. Initialize & Impute

Requirements:

4. Knockout Calcs & Policy
- implement using the Tachyon authoring pipeline:
  extraction -> normalization -> Tachyon request -> DRL generation -> DRL validation -> artifact persistence -> Drools runtime execution
- execute Knockout Calcs first, then Knockout Policy
- if any application policy exists after knockout execution, stop processing and mark the application declined
- use a helper/action function to create policies in decision details / policies
- support only primary applicant for MVP
- preserve traceability and gating

5. Error Scenarios
- business-authored business-error rows should use the Tachyon authoring pipeline and Drools runtime
- technical/system/API error mapping should remain Java/config-based and separate
- support missing data/business error rows from the sheet
- keep generic system error codes and external retry/non-retry handling deterministic and separate

6. Initialize & Impute
- implement as deterministic Java stage after extraction/normalization
- do not use Tachyon for this sheet
- support repeated patterns such as:
  - primary applicant only
  - null/unavailable -> sentinel/default assignment
  - threshold transform
  - copy-through
  - reason-code appends
  - score/model extraction
- write outputs into stable BTS / imputed / decisionDetails locations for later stages

Critical debug/visibility requirement:
For Knockout and Error Scenarios, print/log:
- normalized rules
- Tachyon request payload
- Tachyon response
- generated DRL
- DRL validation result
- artifact persistence details
- Drools facts inserted
- rules fired
- outputs/policies/errors written
- stop/continue behavior

For Initialize & Impute, print/log:
- extracted rows
- normalized definitions
- inputs read
- outputs written

For overall execution, print/log:
- stage execution order
- stage result summary
- final state summary

At the end, list:
- files added
- files updated
- assumptions made
- ambiguous rows interpreted heuristically
- how to run and inspect the debug flow
