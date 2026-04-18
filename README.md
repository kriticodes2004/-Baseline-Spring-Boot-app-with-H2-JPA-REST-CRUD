Create the output domain model needed for knockout policy execution.

1. Add a new class:
src/main/java/com/wellsfargo/creditdecision/domain/Policy.java

Fields:
- String policyCode
- String policyRsnCode
- Integer policyRank
- String policyDescription
- String policyCategory
- String policyType
- Integer policyAplcntIndex
- String policyOfferIndex

Generate plain getters and setters only.

2. Update:
src/main/java/com/wellsfargo/creditdecision/domain/DecisionDetails.java

Add:
- List<Policy> policies = new ArrayList<>()

Also add:
- getter and setter for policies
- helper method addPolicy(Policy policy)

Do not use Lombok.
Do not change package structure.
Keep code Java 17 / Spring Boot compatible.
