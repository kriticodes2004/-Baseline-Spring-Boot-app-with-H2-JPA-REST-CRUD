We are restarting this project from scratch and need a clean, extensible implementation.

Problem statement:
We are building a credit decisioning service driven by an Excel workbook that acts as the business source of truth. Business users should be able to edit/add/remove rows in the workbook, and the implementation should absorb those changes with minimal code changes as long as the sheet structure and supported rule grammar remain the same.

Workbook:
- The cleaned workbook is present in the current project directory.
- Refer to that workbook while implementing.

Architecture decisions:
- Input Data -> Java after extraction
- BOM Requirements -> Java metadata/config integration
- Global Calcs -> deterministic Java stage
- Knockout Calcs & Policy -> Tachyon authoring pipeline + DRL + Drools runtime
- Error Scenarios -> Tachyon authoring pipeline for business-authored business-error rows, but system/API/technical errors remain Java
- Initialize & Impute -> deterministic Java stage, not Tachyon

Execution order for this first phase:
1. Request mapping / Input Data
2. BOM Requirements integration / shared field catalog
3. Global Calcs
4. Knockout Calcs & Policy
5. Error Scenarios
6. Initialize & Impute

Core requirements:
- keep the design extensible
- do not hardcode row-by-row business logic
- separate extraction, normalization, validation, generation, persistence, execution, and tracing
- add strong developer-visible tracing/debugging so we can inspect what each stage extracted, normalized, generated, executed, and wrote
- later we want Tachyon explainability on top of execution traces
We are starting this project from scratch.

Project context:
We are building a credit decisioning service from an Excel workbook that acts as the business source of truth. The implementation must be extensible, traceable, and cleanly structured. We will implement the workbook sheet by sheet, in order, with a clear architecture per sheet.

Important:
For now, implement ONLY the Input Data sheet.
Do not implement any later sheets yet.
Do not assume prior generated code.
Treat this as a fresh project.

What the Input Data sheet is for:
The Input Data sheet is NOT a runtime rule-execution sheet.
It is a metadata/schema sheet.
Its job is to define:
- input field names
- hierarchy/nesting
- types
- lengths
- allowed values
- descriptions
- required/MVP flags
- upstream/source info
- JSON section / BOM alignment
- notes and constraints

So for this sheet:
- yes, read/parse/extract the sheet contents from the workbook
- no, do NOT create a Tachyon authoring pipeline for it
- no, do NOT create DRL for it
- no, do NOT treat it as a runtime executable rule stage

What to build from the Input Data sheet:
1. Request/domain contract foundation
   - request DTOs / contract classes
   - nested structures for application, applicant, loan, merchant, plan, bureau, models, etc.
   - clean package structure

2. Domain model foundation
   - Application and nested domain objects needed as the core runtime model
   - align naming with the workbook and expected JSON/BOM structure

3. Mapping layer
   - DTO-to-domain mapping
   - support nested objects and arrays
   - keep it maintainable and explicit

4. Shared field/path catalog
   - normalized field catalog for paths defined by the workbook
   - used later for validation, stage inputs/outputs, and cross-sheet references

5. Validation foundation
   - required field validation where clearly indicated
   - basic type/shape validation
   - enum/allowed-value validation where the workbook defines values
   - keep it MVP-aligned, not overbuilt

6. Workbook metadata extraction support
   - create the raw row model and extractor/parser for the Input Data sheet
   - use the sheet as the source of truth for generating/validating the metadata model
   - but do not make this a runtime stage

7. Developer visibility
   - add a simple debug/inspection way to show:
     - what rows/fields were extracted from the Input Data sheet
     - what normalized field definitions were built
     - what DTO/domain/path-catalog structures were created

Architecture rules:
- Input Data is metadata extraction, not runtime rule execution
- no Tachyon for this sheet
- no DRL for this sheet
- no Drools for this sheet
- build it in deterministic Java
- keep extraction, normalization, DTO/domain modeling, mapping, and validation separate
- design it so later sheets can reuse the shared field/path catalog and domain model cleanly

Workbook usage:
- The workbook is available in the project context / local directory
- Use the Input Data sheet from that workbook as the source of truth
- I will also share Tachyon details separately, but do not use Tachyon for this Input Data sheet implementation

What I want in this first implementation:
- clean project foundation for the Input Data sheet only
- files/classes for extraction, models, mapping, validation, and shared field catalog
- focused tests for representative extracted fields and mapping behavior
- a summary of files added/updated and assumptions made

Do not move on to Global Calcs, Knockout, Error Scenarios, or any later sheets yet.
Implement only the Input Data sheet foundation properly.
input data sheet:


Use this as the foundation for the next prompts.
