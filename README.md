Continue the same implementation of the Tachyon-based Knockout Authoring pipeline.

==================================================
6) COMPLETE KnockoutPromptBuilder.java
==================================================

Implement method:
- public String buildUserPrompt(String knockoutSheetText)

This user prompt must include all of the following:

1. sheet name = Knockout Calcs & Policy
2. sheet purpose
3. execution semantics:
   - execute knockout calcs first
   - then knockout policy rows
   - if any knockout policy triggers, create policy under application.decisionDetails.policies
   - for MVP evaluate only primary applicant where primaryInd = 1
4. interpretation rules:
   - preserve condition grouping exactly
   - preserve raw vs BTS fields
   - represent primary applicant wording as PRIMARY_ONLY scope
   - Create Policy = action type CREATE_POLICY
   - use policyCode and policyCategory exactly
   - use BOM location exactly
   - preserve null checks explicitly
   - if calcs section empty, return empty calcDefinitions
5. field path conventions:
   - application.applicant[].bureau.<field>
   - bts.<field>
   - application.decisionDetails.policies
6. required output schema example
7. few-shot example 1 for Q18
8. few-shot example 2 for D22 preserving grouped logic
9. append the passed knockoutSheetText at the end

The prompt must be detailed and optimized for extraction quality.

==================================================
7) CREATE KnockoutNormalizationValidator.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.service

Requirements:
- Use @Service

Implement validation for the normalized artifact.

Allowed fields set:
- application.applicant[].bureau.frozenFileInd
- application.applicant[].bureau.lockedFileOrWithheldIndicator
- application.applicant[].bureau.bureauErrorIndicator
- application.applicant[].bureau.noTradeInd
- application.applicant[].bureau.noHitInd
- application.applicant[].bureau.minorIndicator
- bts.all0300

Allowed operators set:
- EQ
- IN
- NE
- GT
- GTE
- LT
- LTE

Implement method:
- public ValidationResult validate(KnockoutSheetArtifact artifact)

Validation behavior:
- if artifact null -> invalid
- if policyDefinitions empty -> warning
- validate each rule:
  - ruleId present
  - ruleName present
  - policyCode present
  - policyCategory present
  - locationInBom present
  - if locationInBom is not application.decisionDetails.policies -> warning
  - duplicate policy codes -> warning
  - missing explicit scope -> warning
  - missing action -> error
  - if action type != CREATE_POLICY -> warning
  - validate conditionTree recursively

Implement recursive helper:
- validateConditionNode(...)

Rules:
- node must be either a group node (logic + children) or leaf node (field/operator/value)
- unknown field -> warning
- unsupported operator -> error

If errors exist, set valid = false.
Otherwise valid = true.

==================================================
8) CREATE KnockoutDroolsRenderer.java
==================================================

Package:
com.example.creditdecision.authoring.knockout.service

Requirements:
- Use @Service

Implement method:
- public String render(KnockoutSheetArtifact artifact)

Behavior:
- generate DRL text
- add package line: com.example.creditdecision.rules.knockout
- import ExecutionContext
- import PolicyCreationService (even if not implemented yet; this is authoring output)

For each rule, render one DRL rule.

Implement helper:
- private String renderRule(KnockoutRuleArtifact rule)

Template:
rule "<RULE_ID>_<POLICY_CODE>"
when
    $ctx : ExecutionContext()
    eval(<generated_boolean_expression>)
then
    PolicyCreationService.addPolicy($ctx, "<ruleId>", "<policyCode>", "<policyCategory>", "<ruleName>");
end

Implement recursive helper:
- private String toDroolsExpression(ConditionNode node)

Support:
- grouped nodes with AND / OR
- leaf operators:
  - EQ
  - NE
  - IN

Implement helper:
- private String renderInExpression(String accessor, Object value)

Implement helper:
- private String toAccessor(String field)

Map these fields:
- application.applicant[].bureau.frozenFileInd -> $ctx.getPrimaryApplicant().getBureau().getFrozenFileInd()
- application.applicant[].bureau.lockedFileOrWithheldIndicator -> $ctx.getPrimaryApplicant().getBureau().getLockedFileOrWithheldIndicator()
- application.applicant[].bureau.bureauErrorIndicator -> $ctx.getPrimaryApplicant().getBureau().getBureauErrorIndicator()
- application.applicant[].bureau.noTradeInd -> $ctx.getPrimaryApplicant().getBureau().getNoTradeInd()
- application.applicant[].bureau.noHitInd -> $ctx.getPrimaryApplicant().getBureau().getNoHitInd()
- application.applicant[].bureau.minorIndicator -> $ctx.getPrimaryApplicant().getBureau().getMinorIndicator()
- bts.all0300 -> $ctx.getBusinessTermSet().getAll0300()

Throw IllegalArgumentException for unsupported fields/operators.

Implement helper:
- private String renderValue(Object value)
- private String escape(String input)
