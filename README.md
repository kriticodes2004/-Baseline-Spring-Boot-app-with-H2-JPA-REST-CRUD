 Fix compile/runtime integration for KnockoutStage in my Spring Boot project.

1. Replace the current broken/generated KnockoutStage.java completely.
2. Create a clean Spring @Component class at:
   src/main/java/com/wellsfargo/creditdecision/engine/stage/KnockoutStage.java
3. Use constructor injection for:
   - KnockoutRuleSetProvider
   - CanonicalRuleEvaluator
   - PolicyFactory
4. Implement execute(DecisionContext context) to:
   - return if context/application null
   - ensure application.decisionDetails exists
   - ensure decisionDetails.policies exists
   - load canonical rules from provider
   - sort by executionOrder ascending
   - iterate all applicants
   - evaluate rule generically with evaluator.matches(rule, context, applicant, applicantIndex)
   - create Policy using policyFactory.create(rule, applicantIndex)
   - avoid duplicates by policyCode + applicantIndex
   - if any policy added:
       context.setStopProcessing(true)
       context.setStopReason("KNOCKOUT")
5. Do not hardcode Q18/D22 in KnockoutStage.
6. Also verify/update:
   - DecisionContext has application, stopProcessing, stopReason with getters/setters
   - DecisionDetails has List<Policy> policies = new ArrayList<>()
   - Policy has sourceRuleId, ruleName, policyCode, policyCategory, policyApclntIndex, executionOrder
7. Ensure imports are correct including:
   import org.springframework.stereotype.Component;
8. Do not create extra files.
