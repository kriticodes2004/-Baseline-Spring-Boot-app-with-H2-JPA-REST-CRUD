Continue the same implementation of the Tachyon-based Knockout Authoring pipeline.

==================================================
9) CREATE KnockoutAuthoringService.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.service

Requirements:
- Use @Service
- Use @RequiredArgsConstructor

Dependencies:
- KnockoutPromptBuilder
- TachyonClient
- ObjectMapper
- KnockoutNormalizationValidator
- KnockoutDroolsRenderer

Implement public method:
- public KnockoutAuthoringResult author(String knockoutSheetText)

Behavior:
1. build Tachyon request with promptBuilder.buildRequest(knockoutSheetText)
2. also build rawPrompt = promptBuilder.buildUserPrompt(knockoutSheetText)
3. call tachyonClient.chat(...)
4. get rawContent via tachyonClient.extractFirstContent(...)
5. clean markdown code fences if Tachyon returned ```json ... ```
6. parse into KnockoutSheetArtifact using ObjectMapper
7. validate artifact
8. if valid, render DRL, otherwise renderedDrl = ""
9. create AuthoringTrace:
   - sheetName = "Knockout Calcs & Policy"
   - rawPrompt
   - rawResponse
   - validationResult
   - if valid, generatedFiles contains:
     - knockout-policy.json
     - knockout-policy.drl
10. return result object

Implement helper:
- private String cleanupJson(String raw)

Create nested static DTO:
- KnockoutAuthoringResult
fields:
- KnockoutSheetArtifact artifact
- ValidationResult validationResult
- String renderedDrl
- AuthoringTrace trace

Use Lombok @Data on nested result class.

If parsing/validation fails unexpectedly, throw IllegalStateException("Failed to parse or validate knockout Tachyon response", e)
Continue the same implementation of the Tachyon-based Knockout Authoring pipeline.

==================================================
10) CREATE KnockoutAuthoringController.java
==================================================

Package:
com.example.creditdecision.api

Requirements:
- Use @RestController
- Use @RequestMapping("/authoring/knockout")
- Use @RequiredArgsConstructor

Dependency:
- KnockoutAuthoringService

Add POST endpoint:
- @PostMapping
- accept request body class KnockoutAuthoringRequest
- return KnockoutAuthoringService.KnockoutAuthoringResult

Create nested static request DTO:
- field: String sheetText
- use Lombok @Data

Endpoint behavior:
- return ResponseEntity.ok(knockoutAuthoringService.author(request.getSheetText()))

==================================================
11) POM / COMPILATION NOTES
==================================================

If needed for compilation:
- ensure spring-boot-starter-web exists
- ensure lombok exists
- ensure jackson-databind is available
- ensure configuration properties scanning works

Do not add unnecessary dependencies.
Continue the same implementation of the Tachyon-based Knockout Authoring pipeline.

==================================================
12) SAMPLE REQUEST BODY
==================================================

After implementing, share a sample POST body for /authoring/knockout like:

{
  "sheetText": "Sheet: Knockout Calcs & Policy\nInstructions: Execute KNOCKOUT_CALCS, then execute KNOCKOUT_POLICY. If any application.POLICY exists after execution of the knockout rules, the application should be declined. For MVP only one applicant is supported and rules are filtered on the primary applicant.\n\nKnockout Policy Rows:\nKNOCK_1 | Credit Bureau Frozen or Locked (Q18) | Inputs: frozenIndicator, lockedIndicator | For each applicant[i] where applicant[i].primaryInd = 1 then if applicant[i].bureauData.frozenFileInd is not null and applicant[i].bureauData.frozenFileInd = true OR applicant[i].bureauData.lockedFileOrWithheldIndicator is not null and applicant[i].bureauData.lockedFileOrWithheldIndicator = true then Create a Policy for applicant using POLICY_APLCNT_INDEX = [i] | PolicyCode=Q18 | PolicyCategory=KNOCKOUT | LocationInBOM=application.decisionDetails.policies\n\nKNOCK_2 | No Trade No Hit (D22) | Inputs: bureauErrorIndicator, frozenFileInd, BTS.ALL0300, noTradeInd, noHitInd, minorIndicator | For each applicant[i] where applicant[i].primaryInd = 1 then if frozenFileInd = false and lockedFileOrWithheldIndicator = false and ((ALL0300 is not null and (ALL0300 = 0 or ALL0300 = 99)) or noTradeInd = true or noHitInd = true or minorIndicator = true) then Create a Policy for applicant using POLICY_APLCNT_INDEX = [i] | PolicyCode=D22 | PolicyCategory=KNOCKOUT | LocationInBOM=application.decisionDetails.policies"
}

==================================================
13) AFTER IMPLEMENTATION
==================================================

After coding:
1. ensure project compiles
2. list files created/updated
3. mention assumptions
4. stop there

Now implement exactly this stage.
