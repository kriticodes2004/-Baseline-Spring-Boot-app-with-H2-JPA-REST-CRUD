Now do a strict implementation audit of the knockout Tachyon authoring pipeline you just created.

Check all files and verify the following one by one:

1. No Tachyon secrets or tokens are hardcoded anywhere in Java or properties.
2. application.properties uses only environment variable placeholders for Tachyon config.
3. TachyonClient is reusable and contains no knockout-specific business logic.
4. KnockoutPromptBuilder contains:
   - strong system prompt
   - strong user prompt
   - explicit execution semantics
   - explicit field-path conventions
   - explicit schema example
   - few-shot examples for Q18 and grouped D22 logic
5. cleanupJson correctly handles:
   - ```json fenced output
   - ``` fenced output
   - plain JSON
   - leading/trailing whitespace
6. KnockoutNormalizationValidator correctly validates:
   - missing ruleId/ruleName/policyCode/policyCategory
   - missing action
   - missing conditionTree
   - unsupported operators
   - unknown fields as warnings
   - unexpected BOM location as warning
7. KnockoutDroolsRenderer correctly supports nested AND/OR trees and does not produce blank expressions for malformed group nodes.
8. Controller endpoint contract is correct and compile-safe.
9. All created files are in the exact packages requested.
10. The project compiles successfully.

If anything is wrong, fix it directly.
Then give me:
- exact files changed
- exact bugs found
- exact fixes made
- final sample request body for /authoring/knockout
Do not refactor beyond what is needed for correctness.
