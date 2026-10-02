# Test fixture contracts

Synthetic PDFs for the PDF extraction and upload tests (FR-3). Every party, name, and clause is invented. **Never add real client contracts here.**

| File | Pages | What it exercises |
|---|---|---|
| `risky-consulting-agreement.pdf` | 2 | Text PDF full of freelancer risks: payment "within a reasonable time", unlimited liability and one-sided indemnity, three-year worldwide non-compete, broad IP assignment, termination without pay |
| `fair-nda.pdf` | 2 | Text PDF with balanced mutual NDA terms (useful as a low-risk baseline) |
| `scanned-image-only.pdf` | 2 | Page images with no text layer, like a scan: extraction finds no text, so upload must return 422 |

Regenerate with `python3 generate_fixtures.py` (standard library only). Edit the contract text in that script, not the PDFs.
