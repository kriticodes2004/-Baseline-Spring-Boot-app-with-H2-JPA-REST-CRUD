
We are starting this project completely from scratch.

Build ONLY the Input Data foundation first. Do not implement any later sheets yet.

Important clarification:
The Input Data sheet is NOT a runtime rule stage.
Do NOT build workbook extraction at runtime.
Do NOT build workbook extraction during build.
Do NOT add Apache POI generators.
Do NOT use Tachyon, DRL, or Drools for the Input Data sheet.

Treat the Input Data sheet only as a business specification/reference for manually creating the Java foundation.

What to implement from the Input Data sheet:
1. Request DTO/contract classes matching the sheet structure
2. Nested domain model classes for the runtime object graph (application, applicant, loan, merchant, bureau, models, contacts, etc.)
3. DTO-to-domain mapper
4. Shared field/path catalog for later stages
5. Basic validation foundation from the sheet metadata:
   - required/MVP fields where clearly indicated
   - type validation
   - allowed values/enums where defined
6. Clean package structure and focused tests

Architecture rules for this task:
- deterministic Java only
- no runtime extraction
- no build-time code generation
- no Tachyon
- no DRL
- no Drools
- keep DTOs, domain models, mapper, field catalog, and validation separate
- use the workbook/Input Data sheet only as a reference/spec to decide what Java classes and fields to create

What I want right now:
- manually implemented Java foundation based on the Input Data sheet
- clean, extensible classes
- summary of files added/updated
- assumptions made
- focused tests

Again: do NOT create Excel readers/generators/extractors for Input Data. Just implement the Java foundation from the sheet spec.
