I am building a Spring Boot Java backend for a credit decision stand-in service.

I need you to generate ONLY the request-side DTO classes from the attached Input Data workbook sheet.

Important rules:
1. The top-level incoming request JSON has this shape:
   {
     "application": { ... }
   }
   So create a top-level class `CreditDecisionRequest` with a single field:
   - application : ApplicationRequest
2. `application` is the top-level business object. All other fields belong under `application` or its nested objects.
3. Use the `JSON Section` column from the sheet to determine nesting.
   Examples:
   - TOP LEVEL -> CreditDecisionRequest
   - application -> ApplicationRequest
   - application.applicant -> ApplicantRequest
   - application.applicant.income -> IncomeRequest
   - application.applicant.name -> ApplicantNameRequest
   - application.applicant.contacts -> ContactRequest
   - application.applicant.addresses -> AddressRequest
   - application.applicant.employment -> EmploymentRequest
   - application.applicant.bureau -> BureauRequest
   - application.applicant.models -> ModelScoreRequest
   - application.loan -> LoanRequest
   - application.merchant -> MerchantRequest
   - application.merchant.planDetails -> PlanDetailRequest
4. Generate ONLY request DTOs from the Input Data sheet. Do not generate response DTOs yet.
5. Use Java wrapper types only:
   - String
   - Integer
   - Double
   - Boolean
   - List<...>
   Never use primitives like int, double, boolean.
6. Add `@JsonIgnoreProperties(ignoreUnknown = true)` to every DTO.
7. Add `@Valid` to nested object and list fields.
8. Add validation only where clearly required from the sheet:
   - If `Required MVP (*used in logic)` is Y or Y*, add validation.
   - If optional or marked N/N*, keep optional and do not add required validation.
9. Validation rules:
   - For required object fields: use `@NotNull`
   - For required string fields: use `@NotBlank`
   - For max string length based on Length column: use `@Size(max = X)` only where X is clearly numeric and reliable
   - For lists/arrays that are required: use `@NotEmpty`
   - Do NOT add regex or enum validation yet unless extremely obvious
   - Do NOT add business-rule validation yet
10. If the sheet mentions null, unavailable, missing, or notes that logic handles such values later, do NOT overvalidate at DTO layer.
11. Preserve workbook naming in camelCase exactly as shown in the sheet.
12. Create getters/setters for all fields.
13. Put classes in package:
   `com.wellsfargo.creditdecision.dto.request`
14. Create these classes if applicable from the sheet:
   - CreditDecisionRequest
   - ApplicationRequest
   - ApplicantRequest
   - IncomeRequest
   - ApplicantNameRequest
   - ContactRequest
   - AddressRequest
   - EmploymentRequest
   - BureauRequest
   - ModelScoreRequest
   - LoanRequest
   - MerchantRequest
   - PlanDetailRequest
15. Wherever the sheet shows OBJECT ARRAY, map to `List<...>`.
16. Wherever the sheet shows OBJECT, map to a nested DTO.
17. Wherever length is ambiguous like `320(64@255)?` or notes say "Need UI max", do not add size validation automatically. Leave as String with no size validation and add a comment.
18. Generate the code class-by-class.
19. Also generate one mapping summary after the classes that shows:
   - field name
   - JSON section
   - target DTO class
   - Java field type
   - whether required or optional
20. Do not generate controller, service, entity, mapper, response DTO, or rule engine code yet.

Before generating code, first infer the DTO tree from the sheet and list it briefly.

Example expected structure:
- CreditDecisionRequest
  - application : ApplicationRequest
- ApplicationRequest
  - applicationId
  - applicationDate
  - jointAppInd
  - marketSourceCode
  - applicant : List<ApplicantRequest>
  - loan : LoanRequest
  - merchant : MerchantRequest
- ApplicantRequest
  - primaryInd
  - birthDate
  - residenceStatus
  - countryOfCitizenship
  - emailAddress
  - income : IncomeRequest
  - name : ApplicantNameRequest
  - contacts : List<ContactRequest>
  - addresses : List<AddressRequest>
  - employment : EmploymentRequest
  - bureau : BureauRequest
  - models : List<ModelScoreRequest>

If there are fields in later rows not visible in one screenshot, infer them from the attached sheet and include them in the proper DTO.

Return:
1. inferred DTO tree
2. Java code for each request DTO
3. mapping summary table
