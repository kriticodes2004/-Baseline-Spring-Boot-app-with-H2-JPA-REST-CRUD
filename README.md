
Refactor only PolicyTableStage to make it generic and extensible from the Policy Tables sheet. Do not modify RiskTierStage behavior or logic.

Important:
- leave RiskTierStage exactly as it is
- do not refactor or change Risk Tier evaluation
- do not change unrelated stages
- do not use Tachyon
- do not use DRL or Drools
- keep this deterministic Java only

Goal:
Make PolicyTableStage extensible so workbook updates of the same Policy Tables sheet structure can be applied without code changes.

What to build for PolicyTableStage:
1. A generic extractor for the Policy Tables sheet that can read multiple table blocks
2. A normalized table definition model for each table block
3. A generic ordered FIRST-HIT evaluator for populated policy tables
4. Safe handling of empty table blocks:
   - empty table sections are valid placeholders
   - skip them safely without errors
   - preserve them as future-expandable sheet structure
5. Support table metadata from sheet:
   - table name
   - pre-execution action/default
   - output columns
   - BOM/output path if present
6. Support generic row conditions from the sheet instead of hardcoding current tables
7. Support common condition styles found in the current sheet, such as:
   - equality
   - IN / NOT IN list checks
   - <=, <, >=, >
   - N/A / blank / missing handling
8. Write outputs using normalized table definitions, not table-specific evaluator branches

Extensibility requirements:
- if new rows are added to an existing policy table, no code change should be needed
- if a new policy table block of the same structure is added, no code change should be needed
- if currently empty table blocks are filled later, no evaluator code change should be needed
- evaluator logic must not be hardcoded to specific current table names like tblT50MinFICO or tblT51Policy

Debug visibility:
Add developer-visible logging/debug output for PolicyTableStage showing:
- extracted table blocks
- normalized table definitions
- skipped empty blocks
- pre-exec defaults applied
- first-hit row selected
- outputs written
- stage summary

Compatibility requirements:
- preserve current integration with FixedStageDecisionEngine
- do not change RiskTierStage
- do not change current stage order
- keep downstream consumers working

Tests:
Add focused tests for:
- extraction of multiple policy table blocks
- safe skipping of empty table blocks
- first-hit evaluation
- applying pre-exec defaults
- adding new same-structure rows without evaluator code changes
- filling a previously empty table block without evaluator code changes

At the end, report:
- files added
- files updated
- assumptions made from current Policy Tables sheet format
- any unsupported condition grammar still remaining
- exactly what kinds of future Excel changes will now work without code changes

Again: do not modify RiskTierStage. Only make PolicyTableStage extensible.

