Inspect the current Tachyon pipeline and show the exact request/response contract being used for knockout-to-DRL generation.

Specifically list:
1. the exact input fields sent to Tachyon,
2. which of those fields come from normalized knockout rules,
3. which fields come from calculated/global-calc/output metadata,
4. the fixed instruction/prompt template being sent,
5. the exact expected Tachyon response JSON shape,
6. where the DRL is extracted from in the response,
7. what assumptions were made versus what is actually configurable.

Do not modify code yet. First explain the current contract class-by-class using the existing implementation.
Refine the Tachyon request builder so the request is built from a strictly typed normalized-rule payload. Clearly separate:
- normalized knockout rule fields,
- available input/global-calc/output field catalog,
- DRL generation instructions,
- expected response schema.

Minimize assumptions and mark any undocumented Tachyon contract fields as TODO/configurable instead of hardcoding them.
