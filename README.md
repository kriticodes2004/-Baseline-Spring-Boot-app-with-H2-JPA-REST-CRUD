Create the runtime knockout execution stage for the existing Spring Boot project.

Context:
- Package root: com.wellsfargo.creditdecision
- Existing domain classes:
  - com.wellsfargo.creditdecision.domain.Application
  - com.wellsfargo.creditdecision.domain.Applicant
  - com.wellsfargo.creditdecision.domain.Bureau
  - com.wellsfargo.creditdecision.domain.DecisionDetails
  - com.wellsfargo.creditdecision.domain.Policy
- Existing context class:
  - com.wellsfargo.creditdecision.engine.context.DecisionContext
- Existing canonical rule classes:
  - com.wellsfargo.creditdecision.rules.canonical.CanonicalKnockoutRule
  - com.wellsfargo.creditdecision.rules.canonical.CanonicalCondition
  - com.wellsfargo.creditdecision.rules.canonical.CanonicalClause
  - com.wellsfargo.creditdecision.rules.canonical.CanonicalAction

I want MVP runtime execution for knockout rules only.

Create file:
src/main/java/com/wellsfargo/creditdecision/engine/stage/KnockoutStage.java

Requirements for KnockoutStage:
1. Annotate with @Component.
2. Public method:
   public boolean execute(DecisionContext context, List<CanonicalKnockoutRule> rules)
3. If context, application, applicant list, or rules are null/empty, return false.
4. Ensure application.decisionDetails exists.
5. Ensure decisionDetails.policies exists.
6. Process rules ordered by executionOrder ascending, nulls last.
7. MVP rule scope:
   - only evaluate primary applicant
   - treat primary applicant as applicant.primaryInd == 1
   - if no applicant has primaryInd == 1, use the first applicant only as fallback
8. Support only these policy codes for now:
   - Q18
   - D22
9. Rule evaluation logic:
   Q18 triggers when:
   - bureau.frozenFileInd == true
     OR
   - bureau.lockedFileOrWithheldIndicator == true

   D22 triggers when ALL of the below are true:
   - bureau.frozenFileInd == false
   - bureau.lockedFileOrWithheldIndicator == false
   - and at least one of:
       - bureau.all0300 is not null and (bureau.all0300 == 0 or bureau.all0300 == 99)
       - bureau.noTradeInd == true
       - bureau.noHitInd == true
       - bureau.minorIndicator == true

10. When a rule triggers, create a Policy object and append it to decisionDetails.policies.
11. Avoid duplicate policy insertion for same policyCode + applicant index.
12. For created Policy set:
   - policyCode = rule.getPolicyCode()
   - policyCategory = rule.getPolicyCategory()
   - policyDescription = rule.getRuleName()
   - policyType = rule.getScope()
   - policyAplcntIndex = 1 for primary applicant
   - leave policyRsnCode, policyRank, policyOfferIndex as null for now
13. Return true if at least one policy was added, else false.
14. Keep implementation simple, readable, and Java 17 compatible.
15. Do not use Lombok.

Also create:
src/test/java/com/wellsfargo/creditdecision/engine/stage/KnockoutStageTest.java

Test requirements:
1. Use JUnit 5.
2. Create unit tests for:
   - q18_should_add_policy_when_frozen_file_is_true
   - q18_should_add_policy_when_locked_indicator_is_true
   - d22_should_add_policy_when_not_frozen_not_locked_and_all0300_is_0
   - d22_should_add_policy_when_not_frozen_not_locked_and_no_trade_is_true
   - should_not_add_policy_when_q18_and_d22_conditions_are_not_met
   - should_not_duplicate_same_policy_for_same_applicant
3. Build domain objects manually in test.
4. Build minimal CanonicalKnockoutRule manually in test with:
   - policyCode
   - policyCategory
   - ruleName
   - scope = APPLICANT
   - executionOrder
5. Assert on:
   - returned boolean
   - size of decisionDetails.policies
   - inserted policyCode
6. Keep tests independent and readable.

Important:
- Use the actual domain getter/setter names already present in the project.
- If Bureau field names differ slightly from the canonical path text, use the real Java field names from the domain model.
- Keep this as an MVP runtime executor, not a generic expression engine yet.
