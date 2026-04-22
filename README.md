You are working inside a brand new Spring Boot Maven project.

Your task is to generate ONLY the initial Input Data layer for a Credit Decision Stand-in Service.

Important constraints:
1. Do NOT implement Drools.
2. Do NOT implement Tachyon.
3. Do NOT implement workbook parsing.
4. Do NOT implement business rules beyond input validation and primary applicant identification.
5. Do NOT add extra architecture beyond what is asked.
6. Keep the implementation clean, compilable, and minimal.
7. Use Java 17, Spring Boot 3, Maven, Lombok, Jackson, and Bean Validation.
8. Generate the exact package structure and classes listed below.
9. Follow the folder structure exactly.
10. If a field is not fully modeled yet, keep the class simple rather than inventing logic.

Goal of this phase:
Take request JSON -> map into DTOs -> validate basic types/required fields/enums -> identify primary applicant -> return a normalized response.

==================================================
PROJECT PACKAGE
==================================================

Base package:
com.example.creditdecision

Create this exact structure:

src/main/java/com/example/creditdecision
  CreditDecisionApplication.java

src/main/java/com/example/creditdecision/api
  DecisionController.java

src/main/java/com/example/creditdecision/dto
  DecisionRequest.java
  ApplicationDto.java
  ApplicantDto.java
  LoanDto.java
  MerchantDto.java
  PlanDetailsDto.java
  IncomeDto.java
  NameDto.java
  ContactDto.java
  AddressDto.java
  EmploymentDto.java
  BureauDto.java
  ModelDto.java

src/main/java/com/example/creditdecision/enums
  MarketSourceCode.java
  ResidenceStatus.java
  PhoneType.java
  AddressType.java
  EmploymentStatus.java
  BureauCode.java

src/main/java/com/example/creditdecision/service
  InputNormalizationService.java

src/main/java/com/example/creditdecision/exception
  GlobalExceptionHandler.java

src/main/java/com/example/creditdecision/util
  PrimaryApplicantUtil.java

Also update pom.xml correctly.

==================================================
POM.XML REQUIREMENTS
==================================================

Use:
- spring-boot-starter-web
- spring-boot-starter-validation
- jackson-datatype-jsr310
- lombok
- spring-boot-starter-test

Parent:
org.springframework.boot:spring-boot-starter-parent:3.3.5

Java version:
17

Enable Lombok annotation processing in maven-compiler-plugin.

==================================================
MAIN APPLICATION
==================================================

Create:
com.example.creditdecision.CreditDecisionApplication

Standard Spring Boot main class with @SpringBootApplication.

==================================================
ENUMS
==================================================

1. MarketSourceCode
Values:
- 01 = SCAN_TO_APPLY
- 02 = EMAIL_TO_APPLY
- 03 = INTERNET_MERCHANT_PORTAL
- 04 = PHONE_APPLICATION
- 05 = APPLY_ON_MERCHANT_DEVICE

Requirements:
- store code as string
- support Jackson serialization/deserialization
- @JsonValue getter
- @JsonCreator static fromCode(String value)
- throw IllegalArgumentException for invalid code

2. ResidenceStatus
Values:
- RENT
- OWN
- OTHER

3. PhoneType
Values:
- HOME
- MOBILE
- WORK
- OTHER

4. AddressType
Values:
- CURRENT
- MAILING
- OFFICE
- HOME_PRESENT
- HOME_PREVIOUS

5. EmploymentStatus
Values:
- EMPLOYED
- SELF_EMPLOYED
- RETIRED
- STUDENT
- HOMEMAKER
- UNEMPLOYED_WITHOUT_INCOME

6. BureauCode
Values:
- EXP
- EFX
- TU

==================================================
DTO REQUIREMENTS
==================================================

Use Lombok @Data for DTOs.
Use jakarta.validation annotations.

1. DecisionRequest
Fields:
- @NotNull @Valid ApplicationDto application

2. ApplicationDto
Fields:
- @NotBlank @Size(max = 15) String applicationId
- @NotBlank String applicationDate
- @NotNull Boolean jointAppInd
- @NotNull MarketSourceCode marketSourceCode
- @NotEmpty @Valid List<ApplicantDto> applicant
- @NotNull @Valid LoanDto loan
- @NotNull @Valid MerchantDto merchant

3. ApplicantDto
Fields:
- @NotNull @Min(0) @Max(1) Integer primaryInd
- @NotBlank @Pattern("^\\d{4}-\\d{2}-\\d{2}$") String birthDate
- @NotNull ResidenceStatus residenceStatus
- @Size(min = 2, max = 2) String countryOfCitizenship
- @Size(max = 320) String emailAddress
- @Valid IncomeDto income
- @Valid NameDto name
- @Valid List<ContactDto> contacts
- @Valid List<AddressDto> addresses
- @Valid EmploymentDto employment
- @Valid BureauDto bureau
- @Valid List<ModelDto> models

4. IncomeDto
Fields:
- @DecimalMin("0.0") BigDecimal totalAnnualIncome
- @DecimalMin("0.0") BigDecimal additionalIncomes

5. NameDto
Fields:
- @Size(max = 15) String firstName
- @Size(max = 1) String middleInitial
- @Size(max = 25) String lastName
- @Size(max = 2) String suffix

6. ContactDto
Fields:
- PhoneType phoneType
- @Pattern("^\\d{10}$") String phoneNumber

7. AddressDto
Fields:
- AddressType type
- @Size(max = 40) String addressLine1
- @Size(max = 40) String addressLine2
- @Size(max = 40) String addressLine3
- @Size(max = 25) String city
- @Size(min = 2, max = 2) String state
- @Pattern("^\\d{5}(\\d{4})?$") String zip

8. EmploymentDto
Fields:
- EmploymentStatus employmentStatus

9. BureauDto
Fields:
- BureauCode bureauCode
- Boolean bureauErrorIndicator
- Boolean frozenFileInd
- Boolean lockedFileOrWithheldIndicator
- Boolean noHitInd
- Boolean noTradeInd
- Boolean actMtgTradeInd
- Boolean minorIndicator

- Integer fico9Score
- String fico9AARC1
- String fico9AARC2
- String fico9AARC3
- String fico9AARC4
- String fico9AARC5

- BigDecimal bureauTotMonthlyPmt

- Integer all0000
- Integer all0100
- Integer all0136
- Integer all2327
- Integer all8220
- Integer all8222
- Integer all8321
- Integer all9220
- Integer brc7140
- Integer iqt9416
- Integer iqt9425
- Integer iqt9426
- Integer pil0438
- Integer pil8120
- Integer reh5030

10. ModelDto
Empty class with Lombok @Data only.

11. LoanDto
Fields:
- BigDecimal loanAmount

12. MerchantDto
Fields:
- List<PlanDetailsDto> planDetails

13. PlanDetailsDto
Fields:
- BigDecimal monthlyInstallment

==================================================
UTILITY
==================================================

Create PrimaryApplicantUtil.

Requirements:
- final utility class
- private constructor
- static method:
  Optional<ApplicantDto> findPrimaryApplicant(List<ApplicantDto> applicants)

Logic:
- if list is null, return Optional.empty()
- return first applicant where primaryInd == 1

==================================================
SERVICE
==================================================

Create InputNormalizationService.

Method:
public Map<String, Object> normalize(DecisionRequest request)

Logic:
- find primary applicant using PrimaryApplicantUtil
- if none found, throw IllegalArgumentException("No primary applicant found")
- return a LinkedHashMap with:
  - "message" -> "Input accepted"
  - "applicationId"
  - "applicationDate"
  - "jointAppInd"
  - "marketSourceCode" -> enum code string
  - "primaryApplicantBirthDate"
  - "primaryApplicantResidenceStatus"
  - "primaryApplicantCountry"
  - if income exists:
      "primaryApplicantTotalAnnualIncome"
      "primaryApplicantAdditionalIncomes"

Do not add business calculations.

==================================================
CONTROLLER
==================================================

Create DecisionController.

Requirements:
- @RestController
- @RequestMapping("/decision")
- constructor injection with Lombok @RequiredArgsConstructor
- endpoint:
  @PostMapping("/evaluate")
  public ResponseEntity<Map<String, Object>> evaluate(@Valid @RequestBody DecisionRequest request)

Logic:
- call InputNormalizationService.normalize(request)
- return ResponseEntity.ok(result)

==================================================
EXCEPTION HANDLER
==================================================

Create GlobalExceptionHandler with @RestControllerAdvice.

1. Handle MethodArgumentNotValidException
Return HTTP 400 with body:
- errorInd = true
- errorId = "VALIDATION_ERROR"
- errorMessage = joined field validation messages

2. Handle IllegalArgumentException
Return HTTP 400 with body:
- errorInd = true
- errorId = "INPUT_ERROR"
- errorMessage = exception message

Use LinkedHashMap for response bodies.

==================================================
CODING STYLE
==================================================

- Use clean imports
- Use jakarta.validation package, not javax
- Use Lombok annotations
- Code must compile
- No placeholder TODOs
- No extra files beyond what is requested unless absolutely needed for compilation
- No business-rule engine code
- No persistence layer
- No database
- No Swagger
- No security
- No tests yet

==================================================
AFTER GENERATING FILES
==================================================

Also generate a sample valid request JSON in a comment block or separate snippet for easy POST testing:

{
  "application": {
    "applicationId": "APP123456789012",
    "applicationDate": "2026-04-16T10:30:00.000Z",
    "jointAppInd": false,
    "marketSourceCode": "03",
    "applicant": [
      {
        "primaryInd": 1,
        "birthDate": "1995-02-10",
        "residenceStatus": "RENT",
        "countryOfCitizenship": "US",
        "emailAddress": "kriti@test.com",
        "income": {
          "totalAnnualIncome": 85000,
          "additionalIncomes": 5000
        },
        "name": {
          "firstName": "Kriti",
          "lastName": "Khurana"
        }
      }
    ],
    "loan": {
      "loanAmount": 12000
    },
    "merchant": {
      "planDetails": [
        {
          "monthlyInstallment": 400
        }
      ]
    }
  }
}

Now create all files exactly as instructed.
