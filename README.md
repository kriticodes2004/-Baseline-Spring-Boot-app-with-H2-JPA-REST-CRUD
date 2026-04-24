
Now review the Plan Offer Policy implementation strictly.

Check:
- no canonical artifact layer was introduced
- Tachyon returns DRL directly
- support tables run in Java before DRL execution
- policies attach only to the correct plan
- runtime executes one current plan at a time
- startup refresh caches DRL
- malformed rows are rejected safely
- prompt stays compact

If anything violates this design, refactor it now.
if everything is good, start with sample tests once your tests pass give me postman bodies for manual testing along with expected behaviour
