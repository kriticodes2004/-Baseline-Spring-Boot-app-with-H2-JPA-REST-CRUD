I have already generated these classes for the knockout authoring pipeline:
- KnockoutSheetMetadata
- KnockoutPolicySheetRow
- KnockoutSheetMetadataExtractor
- KnockoutPolicySheetExtractor
- KnockoutFieldDictionary
- KnockoutRowNormalizer
- CanonicalKnockoutRule
- CanonicalCondition
- CanonicalClause
- CanonicalAction
- KnockoutPromptBuilder
- TachyonKnockoutTranslationService
- TachyonKnockoutTranslationServiceImpl

Now fix and extend the implementation with these exact requirements:

1. In TachyonKnockoutTranslationServiceImpl, change output path from:
   application.policies
   to:
   application.decisionDetails.policies

2. Ensure every canonical knockout rule explicitly sets:
   - stage = "KNOCKOUT"
   - artifactType = "POLICY_RULE"
   - scope = "APPLICANT"
   - applicantFilter = "primaryInd = 1"

3. Fix D22 mocked logic so it matches workbook semantics:
   frozenFileInd = false
   AND lockedFileOrWithheldIndicator = false
   AND (
       (all0300 != null AND (all0300 == 0 OR all0300 == 99))
       OR noTradeInd = true
       OR noHitInd = true
       OR minorIndicator = true
   )

   For MVP, it is acceptable to represent the OR block as a RAW_EXPR clause if nested condition support is not implemented yet.

4. Preserve D22 note text:
   "Knockout runs before imputations; use raw all0300 and check for 0/99, not imputed -99."

5. Keep Q18 logic as:
   frozenFileInd = true OR lockedFileOrWithheldIndicator = true

6. Add a smoke test class at:
   src/test/java/com/wellsfargo/creditdecision/authoring/excel/KnockoutPolicyImportSmokeTest.java

7. That test should:
   - open the Excel workbook using Apache POI from a configurable local file path
   - read sheet "Knockout Calcs & Policy"
   - run KnockoutSheetMetadataExtractor
   - run KnockoutPolicySheetExtractor
   - print extracted metadata
   - print all extracted rows
   - translate only policy codes Q18 and D22 using TachyonKnockoutTranslationServiceImpl
   - serialize canonical rules using Jackson ObjectMapper pretty print
   - assert that at least 2 policy rows were extracted
   - assert that Q18 and D22 were found

8. Add any helper methods needed for safe cell reading and pretty output.

9. Do not use Lombok.

10. Keep code compilable with Gradle and JUnit 5.

Generate only the changed files and the new test file.
