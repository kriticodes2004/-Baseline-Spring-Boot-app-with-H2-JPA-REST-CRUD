
Implement the Credit Policy Table stage now, and insert it before the already-implemented Decision stage in the main /decision/evaluate pipeline.

Do not change the existing Determine Offer Approval or Decision logic unless required for wiring.
Do not use Tachyon.
Do not use Drools.
Do not hardcode policy metadata in Java.

## Why this stage must run before Decision

The Decision sheet already uses:
- ranking application-level policies by policies.policyRank
- determining adverse action from the highest-ranking policy

So the Credit Policy Table must enrich policies before Decision executes.

## Goal

Read the Credit Policy Table sheet from Excel and use it to enrich already-triggered policy objects using policyCode as the lookup key.

For each triggered policy, populate:
- policyRank
- policyDescription
- policyCategory
- policyType
- policyRsnCode

This applies to:
- application-level policies in application.decisionDetails.policies
- plan-level policies if they exist under plan decision details

## Create / update

Create:
- CreditPolicyTableStage
- CreditPolicyEnrichmentService
- CreditPolicyTableExtractor
- CreditPolicyTableProvider
- CreditPolicyMetadata
- CreditPolicyTableRow if needed

Update:
- Policy model if required, to include:
  - Integer policyRank
  - String policyDescription
  - String policyCategory
  - String policyType
  - String policyRsnCode

Keep model changes minimal.

## Extraction behavior

Use Apache POI to read the Credit Policy Table sheet.

Expected columns:
- Rule ID
- policyCode
- policyRank
- policyDescription
- policyCategory
- policyType
- policyRsnCode

Ignore notes for runtime logic.

Validation:
- skip blank rows
- skip rows with blank policyCode
- trim all strings
- parse policyRank as integer
- detect duplicate policyCode rows and fail fast with a clear startup/runtime refresh error
- build a map: Map<String, CreditPolicyMetadata>

## Runtime enrichment behavior

For every triggered policy:
- find metadata by policyCode
- enrich the policy with all mapped fields

If metadata is missing for a triggered code:
- do not fail request
- leave fields null
- log warning clearly

## Pipeline wiring

Insert this stage into the integrated pipeline:
- after all policy-triggering stages
- after Determine Offer Approval if that is how current pipeline is structured
- definitely before Decision

Required final order for this region should be effectively:

1. policy-triggering stages
2. Determine Offer Approval
3. Credit Policy Table enrichment
4. Decision

Do not move Decision earlier.

## Tests

Add tests for:
- extractor reads table correctly
- duplicate policyCode detection
- application-level policy enrichment
- plan-level policy enrichment
- missing metadata warning without crash
- Decision receives populated policyRank values after this stage runs

## Constraints

- Java only
- sheet-driven
- no hardcoded switch/case for policy code mappings
- no Tachyon
- no DRL
- build on top of current working codebase

At the end, give a short summary with:
- files created/updated
- exact place inserted in pipeline
- any assumptions made
