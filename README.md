Now implement only the first 3 parts using the workbook in the current project directory, without rewriting unrelated code:

1. Input Data
2. BOM Requirements integration
3. Global Calcs

Requirements:

1. Input Data
- implement as Java after extraction
- parse the sheet into structured field metadata
- use it to support request contract, domain mapping, nested structures, and shared path catalog creation/validation

2. BOM Requirements
- implement as Java metadata/config integration only
- use it to standardize field catalog paths, BOM/output locations, and naming alignment
- do not treat it as a runtime business-rule stage

3. Global Calcs
- implement as deterministic Java stage after extraction/normalization
- use workbook rows to build normalized calc definitions and evaluate them in Java
- keep it expandable for future same-structure rows

Debug/visibility requirement:
Add developer-visible tracing/logging or a debug report so I can inspect:
- extracted rows/definitions
- normalized models
- inputs read
- outputs written
- stage execution result
- final state summary after these 3 parts

Also add a simple developer-friendly way to run and inspect the current flow.

At the end, list:
- files added
- files updated
- assumptions made
- ambiguous rows interpreted heuristically
- how to run the debug flow
