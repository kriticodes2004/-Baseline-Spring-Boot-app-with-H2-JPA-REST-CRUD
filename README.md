You have already generated the initial Input Data implementation for my Spring Boot project.

Now refine and update the existing code by comparing it against the attached Input Data sheet screenshots.

Important:
- Do NOT rewrite the architecture.
- Do NOT introduce Drools, Tachyon, workflow engine logic, database, persistence, or business rules.
- Stay within the existing Input Data scope only.
- Use the current codebase as the base and only ADD / UPDATE what is needed.
- Preserve package names and folder structure unless a small addition is necessary.
- Do not rename existing classes unless absolutely necessary.
- Do not remove working validation already present unless it clearly conflicts with the sheet.

Goal:
Make the DTO layer, enums, and input validation align more closely with the Input Data sheet and BOM mapping notes.

==================================================
CONTEXT
==================================================

This phase is only for the Input Data sheet.

The Input Data sheet defines:
- request field names
- object nesting
- types
- lengths
- allowed values
- required MVP flags
- JSON section mapping
- upstream source notes
- some field-level notes

The project already has a first pass implementation with:
- DecisionRequest
- ApplicationDto
- ApplicantDto
- nested DTOs
- enums
- validation
- normalization service
- primary applicant utility
- controller
- exception handler

Now I want you to compare the existing implementation with the attached Input Data screenshots and refine it carefully.

==================================================
DO NOT CHANGE THESE HIGH-LEVEL RULES
==================================================

1. Keep Java 17 + Spring Boot + Lombok + Bean Validation.
2. Keep the same package structure.
3. Keep `/decision/evaluate`.
4. Keep this phase limited to input mapping and validation.
5. Do not add business calculations.
6. Do not add stage pipeline logic.
7. Do not add rules engine logic.
8. Do not parse Excel files.
9. Do not create output DTOs yet.
10. Do not hardcode decision logic.

==================================================
WHAT TO REVIEW AND UPDATE
==================================================

Review the existing DTOs against the Input Data sheet and do the following:

### A. Verify top-level request mapping
Ensure request structure remains:

- DecisionRequest
  - application

And under application:
- applicationId
- applicationDate
- jointAppInd
- marketSourceCode
- applicant
- loan
- merchant

Keep JSON naming in camelCase.

### B. Verify applicant-level nested structure
Under applicant, ensure these object/nested groupings exist correctly:

- income
- name
- contacts
- addresses
- employment
- bureau
- models

If any of these classes or fields are missing, add them.

### C. Add any clearly missing fields from the screenshots
Based on the Input Data screenshots, refine DTOs to include missing fields that are visible and clearly part of input scope.

Examples of visible fields that should exist if not already present:
- totalAnnualIncome
- additionalIncomes
- firstName
- middleInitial
- lastName
- suffix
- phoneType
- phoneNumber
- type (address type)
- addressLine1
- addressLine2
- addressLine3
- city
- state
- zip
- employmentStatus
- bureauCode
- bureauErrorIndicator
- frozenFileInd
- lockedFileOrWithheldIndicator
- noHitInd
- noTradeInd
- actMtgTradeInd
- minorIndicator
- fico9Score
- fico9AARC1
- fico9AARC2
- fico9AARC3
- fico9AARC4
- fico9AARC5
- bureauTotMonthlyPmt
- all0000
- all0100
- all0136
- all2327
- all8220
- all8222
- all8321
- all9220
- brc7140
- iqt9416
- iqt9425
- iqt9426
- pil0438
- pil8120
- reh5030

If some of these are already present, keep them and do not duplicate.

### D. Refine validation annotations
Use the sheet to refine validation where obvious.

Examples:
- applicationId max length 15
- birthDate format YYYY-MM-DD
- marketSourceCode enum codes 01 to 05
- primaryInd should be 0 or 1
- countryOfCitizenship length 2
- phoneNumber should be 10 digits
- state length 2
- zip should support 5 or 9 digits
- emailAddress max length should stay reasonable based on current implementation unless a better clear mapping is needed
- numeric fields should use suitable numeric types like BigDecimal or Integer

Do not invent overly strict validation where the sheet is unclear.

### E. Required MVP fields
Use required flags conservatively.

Only keep `@NotNull`, `@NotBlank`, `@NotEmpty` on fields that are clearly required for this MVP based on the existing implementation and visible sheet indicators.

Do not suddenly mark everything required.

### F. Enum refinement
Verify enums against visible allowed values from the sheet and update if needed:

- MarketSourceCode:
  01, 02, 03, 04, 05
- ResidenceStatus:
  RENT, OWN, OTHER
- PhoneType:
  HOME, MOBILE, WORK, OTHER
- AddressType:
  CURRENT, MAILING, OFFICE, HOME_PRESENT, HOME_PREVIOUS
- EmploymentStatus:
  EMPLOYED, SELF_EMPLOYED, RETIRED, STUDENT, HOMEMAKER, UNEMPLOYED_WITHOUT_INCOME
- BureauCode:
  EXP, EFX, TU

Keep Jackson-friendly enum handling for MarketSourceCode.

### G. BOM / JSON naming note
Important note from BOM Requirements:
- Use built-in BOM-style naming aligned with JSON schema.
- Output tabs were standardized to camelCase.
- Keep field names in camelCase matching the JSON/input sheet naming.
- Do not introduce snake_case or custom alternate naming.

### H. Application date note
From the notes:
- applicationDate may need timezone standardization to Zulu/UTC later.
For now:
- keep it as String in DTO
- do not implement full date conversion yet
- optionally add a comment in code or normalization service noting that UTC normalization will happen in a later phase

### I. Normalization service
Keep InputNormalizationService simple.
Only update it if needed to reflect any added fields that are useful for input acceptance summary.

Do not add business logic.
Do not compute derived fields.
Do not transform deeply yet.

### J. Primary applicant utility
Keep primary applicant detection logic as:
- first applicant where primaryInd == 1

Do not overcomplicate it.

==================================================
DELIVERABLE
==================================================

Please do the following in the existing project:

1. Review all current files created for Input Data.
2. Update DTOs/enums/validation to align better with the attached Input Data sheet.
3. Add only missing fields/classes that are clearly supported by the screenshots.
4. Keep the code compiling.
5. Show me:
   - which files were updated
   - what fields were added/changed
   - any validation changes made
   - any assumptions kept intentionally loose

==================================================
VERY IMPORTANT RESTRICTIONS
==================================================

- Do not generate unrelated files.
- Do not replace the architecture.
- Do not add service layers other than minimal input-layer refinements.
- Do not add rule processing.
- Do not add output schema classes.
- Do not add database entities.
- Do not add repository layer.
- Do not add configuration unless required for compilation.
- Do not break existing endpoint behavior.

Work as a refinement pass over the current Input Data implementation only.
