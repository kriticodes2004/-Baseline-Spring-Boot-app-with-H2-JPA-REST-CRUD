You are working in an existing Spring Boot Maven project for a Credit Decision Stand-in Service.

Assume you have NO prior context other than this prompt.
Implement exactly what is described here.

==================================================
HIGH-LEVEL GOAL
==================================================

Implement the Tachyon-based Knockout Authoring pipeline.

This stage is NOT the runtime knockout execution engine.
This stage is the AI authoring pipeline that does:

1. accept knockout sheet text
2. build a sheet-specific Tachyon prompt
3. call Tachyon through one reusable client
4. parse normalized JSON output
5. validate the normalized artifact
6. render Drools DRL from the normalized artifact
7. preserve authoring traceability

This stage must support the idea that:
- one common Tachyon caller will be reused for all future Tachyon-authored sheets
- each sheet will provide its own specialized prompt/context builder
- the knockout sheet is the first such implementation

==================================================
IMPORTANT RESTRICTIONS
==================================================

Do NOT do any of the following:
- Do NOT implement runtime knockout evaluation in this stage
- Do NOT implement Drools runtime execution in this stage
- Do NOT implement workbook parsing from Excel files
- Do NOT implement Input Data, Initialize & Impute, Global Calcs, Error Scenarios, or final decision logic
- Do NOT redesign the current architecture
- Do NOT add database/persistence/repositories
- Do NOT add security
- Do NOT hardcode secrets in source code
- Do NOT invent unrelated files
- Do NOT add TODO placeholders

Use environment-variable-backed Spring properties for Tachyon credentials/config.

==================================================
BASE PACKAGE
==================================================

Use this exact base package:

com.example.creditdecision

==================================================
WHAT THIS STAGE SHOULD DELIVER
==================================================

After implementation, the project should have:
- a reusable Tachyon HTTP caller
- knockout-specific prompt builder
- knockout artifact models
- knockout normalization validator
- knockout DRL renderer
- knockout authoring service
- a REST endpoint to submit sheet text for authoring

The endpoint should accept raw sheet text and return:
- normalized artifact
- validation result
- rendered DRL
- authoring trace

==================================================
FILES TO ADD
==================================================

Create these files exactly:

src/main/java/com/example/creditdecision/tachyon/config/TachyonProperties.java
src/main/java/com/example/creditdecision/tachyon/model/TachyonChatMessage.java
src/main/java/com/example/creditdecision/tachyon/model/TachyonChatRequest.java
src/main/java/com/example/creditdecision/tachyon/model/TachyonChatResponse.java
src/main/java/com/example/creditdecision/tachyon/model/TachyonChoice.java
src/main/java/com/example/creditdecision/tachyon/model/TachyonResponseMessage.java

src/main/java/com/example/creditdecision/tachyon/client/TachyonClient.java

src/main/java/com/example/creditdecision/authoring/knockout/model/KnockoutSheetArtifact.java
src/main/java/com/example/creditdecision/authoring/knockout/model/KnockoutRuleArtifact.java
src/main/java/com/example/creditdecision/authoring/knockout/model/ConditionNode.java
src/main/java/com/example/creditdecision/authoring/knockout/model/RuleAction.java
src/main/java/com/example/creditdecision/authoring/knockout/model/RuleScope.java
src/main/java/com/example/creditdecision/authoring/knockout/model/ValidationResult.java
src/main/java/com/example/creditdecision/authoring/knockout/model/AuthoringTrace.java

src/main/java/com/example/creditdecision/authoring/knockout/prompt/KnockoutPromptBuilder.java
src/main/java/com/example/creditdecision/authoring/knockout/service/KnockoutNormalizationValidator.java
src/main/java/com/example/creditdecision/authoring/knockout/service/KnockoutDroolsRenderer.java
src/main/java/com/example/creditdecision/authoring/knockout/service/KnockoutAuthoringService.java

src/main/java/com/example/creditdecision/api/KnockoutAuthoringController.java

==================================================
FILES TO UPDATE
==================================================

Update if needed:
- src/main/resources/application.properties
- pom.xml only if required for compilation

Do NOT modify unrelated business classes.

==================================================
APPLICATION.PROPERTIES REQUIREMENTS
==================================================

Add these properties using env variable placeholders only:

credit.rules.version=${CREDIT_RULES_VERSION:POC_2504.01}

tachyon.chat.url=${TACHYON_CHAT_URL}
tachyon.api-key=${TACHYON_API_KEY}
tachyon.usecase-id=${TACHYON_USECASE_ID}
tachyon.apigee.access-token=${TACHYON_ACCESS_TOKEN}
tachyon.client-id=${TACHYON_CLIENT_ID}
tachyon.api-version=${TACHYON_API_VERSION:0.01}
tachyon.orig-client-id=${TACHYON_ORIG_CLIENT_ID}
tachyon.cmp-id=${TACHYON_CMP_ID}
tachyon.chat.model=${TACHYON_MODEL:gpt4.1}
tachyon.chat.max-tokens=${TACHYON_MAX_TOKENS:8000}

Do not hardcode tokens or secret values.

==================================================
1) CREATE TachyonProperties.java
==================================================

Package:
com.example.creditdecision.tachyon.config

Requirements:
- Use @Configuration
- Use @ConfigurationProperties(prefix = "tachyon")
- Use Lombok @Data
- Include fields:
  - String chatUrl
  - String apiKey
  - String usecaseId
  - String apigeeAccessToken
  - String clientId
  - String apiVersion
  - String origClientId
  - String cmpId
  - Chat chat = new Chat()

Nested static class Chat:
- String model
- Integer maxTokens

==================================================
2) CREATE Tachyon model classes
==================================================

Package:
com.example.creditdecision.tachyon.model

Create these exact classes:

A. TachyonChatMessage
- fields:
  - String role
  - String content
- annotations:
  - @Data
  - @AllArgsConstructor
  - @NoArgsConstructor

B. TachyonChatRequest
- fields:
  - String model
  - List<TachyonChatMessage> messages
  - Double temperature
  - Integer maxTokens
- annotate maxTokens with @JsonProperty("max_tokens")
- use Lombok @Data

C. TachyonResponseMessage
- fields:
  - String role
  - String content
- use Lombok @Data

D. TachyonChoice
- fields:
  - Integer index
  - TachyonResponseMessage message
- use Lombok @Data

E. TachyonChatResponse
- fields:
  - String id
  - List<TachyonChoice> choices
- use Lombok @Data

==================================================
3) CREATE TachyonClient.java
==================================================

Package:
com.example.creditdecision.tachyon.client

Requirements:
- Use @Component
- Use constructor injection via Lombok @RequiredArgsConstructor
- Dependencies:
  - TachyonProperties
  - ObjectMapper

Implement method:
- public TachyonChatResponse chat(TachyonChatRequest requestBody)

Behavior:
1. generate requestId using UUID.randomUUID().toString()
2. serialize requestBody to JSON using ObjectMapper
3. send HTTP POST using java.net.http.HttpClient
4. use URL from properties.getChatUrl()
5. set headers exactly:
   - Authorization: Bearer <apigeeAccessToken>
   - Content-Type: application/json
   - Accept: */*
   - X-REQUEST-ID
   - X-CORRELATION-ID
   - X-WF-REQUEST-DATE = Instant.now().toString()
   - X-WF-CLIENT-ID
   - X-WF-ORIG-CLIENT-ID
   - X-WF-CMP-ID
   - X-WF-API-VERSION
   - X-WF-API-KEY
   - X-WF-USECASE-ID
6. if HTTP status is not 2xx, throw IllegalStateException with status/body
7. parse response JSON into TachyonChatResponse

Implement helper method:
- public String extractFirstContent(TachyonChatResponse response)

Behavior:
- read first choice message content
- if missing, throw IllegalStateException("No message content returned from Tachyon")

Important:
- handle IOException and InterruptedException
- if interrupted, re-set interrupt flag and throw IllegalStateException
- do not print secrets
- do not log payloads here

==================================================
4) CREATE knockout artifact model classes
==================================================

Package:
com.example.creditdecision.authoring.knockout.model

A. RuleScope
- fields:
  - String applicantFilter
  - String applicantIndexSource
- use Lombok @Data

B. RuleAction
- fields:
  - String type
  - String target
  - String policyCode
  - String policyCategory
- use Lombok @Data

C. ConditionNode
- fields:
  - String logic
  - List<ConditionNode> children
  - String field
  - String operator
  - Object value
  - String nullTreatment
- use Lombok @Data
- use @JsonInclude(JsonInclude.Include.NON_NULL)

D. KnockoutRuleArtifact
- fields:
  - String ruleId
  - String ruleName
  - String policyCode
  - String policyCategory
  - String locationInBom
  - RuleScope scope
  - List<String> inputs = new ArrayList<>()
  - ConditionNode conditionTree
  - RuleAction action
  - List<String> notes = new ArrayList<>()
- use Lombok @Data

E. KnockoutSheetArtifact
- fields:
  - String sheetType
  - String sheetName
  - Map<String, Object> instructions
  - List<Map<String, Object>> calcDefinitions = new ArrayList<>()
  - List<KnockoutRuleArtifact> policyDefinitions = new ArrayList<>()
- use Lombok @Data

F. ValidationResult
- fields:
  - boolean valid
  - List<String> errors = new ArrayList<>()
  - List<String> warnings = new ArrayList<>()
- use Lombok @Data

G. AuthoringTrace
- fields:
  - Instant createdAt = Instant.now()
  - String sheetName
  - String rawPrompt
  - String rawResponse
  - ValidationResult validationResult
  - List<String> generatedFiles = new ArrayList<>()
- use Lombok @Data

==================================================
5) CREATE KnockoutPromptBuilder.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.prompt

Requirements:
- Use @Component
- Use @RequiredArgsConstructor
- dependency:
  - TachyonProperties

Implement method:
- public TachyonChatRequest buildRequest(String knockoutSheetText)

Behavior:
- model = properties.getChat().getModel()
- temperature = 0.1
- maxTokens = properties.getChat().getMaxTokens()
- messages = system + user prompt

Implement method:
- public String buildSystemPrompt()

System prompt must instruct Tachyon to:
- act as a business-rule authoring assistant
- convert spreadsheet knockout rules into normalized JSON
- preserve rule meaning exactly
- not generate Java
- not generate Drools yet
- not flatten grouped boolean logic incorrectly
- not drop null handling
- not invent missing fields
- mark ambiguity in notes instead of guessing
- return only valid JSON
