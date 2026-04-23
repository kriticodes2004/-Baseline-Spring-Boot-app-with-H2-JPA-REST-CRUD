Implement the application flow using startup artifact refresh + one runtime endpoint.

Desired behavior:

1. On application startup:
- automatically refresh/generate artifacts for implemented Tachyon-backed sheets
- for now this includes:
  - knockout
  - error scenarios
- read workbook path from configuration
- generate/validate DRL and keep it ready for runtime use
- fail clearly if startup authoring/validation fails

2. At runtime:
Create one endpoint:
POST /decision/evaluate

When this endpoint is hit with application payload:
- build ExecutionContext
- execute stages in this exact order:
  1. initialize/impute
  2. global calcs
  3. knockout runtime using pre-generated DRL
  4. error scenarios runtime using pre-generated DRL
  5. risk tier runtime using Java matrix evaluator
- return enriched response

Architecture requirements:
- do not regenerate DRL on each request
- startup refresh should happen once when app starts
- runtime should use already loaded/generated artifacts
- keep authoring/bootstrap separate from request execution
- one runtime endpoint only
- reuse existing working services where possible
- do not build a heavy workflow engine

Implement/refine these pieces as needed:
- DecisionStartupRefreshService
- artifact registry/cache for generated DRL
- DecisionOrchestrationService
- DecisionRuntimeService
- DecisionController with POST /decision/evaluate

Also:
- load workbook path from configuration
- make startup refresh logs clear
- if startup generation fails, fail loudly with clear error
- document the runtime order

After implementation, provide:
- files created/updated
- startup flow summary
- runtime flow summary
- config needed
- sample request body for /decision/evaluate
