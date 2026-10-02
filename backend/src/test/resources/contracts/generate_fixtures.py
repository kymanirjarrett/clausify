#!/usr/bin/env python3
"""Generate the synthetic contract PDFs used by the PDF extraction tests.

Every party, name, and clause here is invented. Never add real client contracts to this folder.
Standard library only, so it runs anywhere:  python3 generate_fixtures.py
"""
import textwrap
import zlib
from pathlib import Path

HERE = Path(__file__).parent
PAGE_W, PAGE_H = 612, 792  # US Letter, in points
MARGIN, FONT_SIZE, LEADING, WRAP = 72, 11, 15, 88
LINES_PER_PAGE = (PAGE_H - 2 * MARGIN) // LEADING

RISKY_CONSULTING = """\
CONSULTING SERVICES AGREEMENT

This Consulting Services Agreement (the "Agreement") is entered into as of March 3, 2026 between Northwind Ventures LLC (the "Client") and Jordan Rivera, an independent contractor (the "Contractor").

1. SERVICES
1.1 Contractor shall provide software development and related services as requested by Client from time to time, including any additional services Client reasonably determines are necessary to complete the project.

2. COMPENSATION AND PAYMENT
2.1 Client shall pay Contractor a fee of $85 per hour for services performed.
2.2 Contractor shall submit invoices monthly. Client shall pay undisputed invoices within a reasonable time after receipt. Client may dispute any invoice in its sole discretion, and disputed amounts are not payable until the dispute is resolved to Client's satisfaction.
2.3 No late fees or interest shall accrue on unpaid amounts.

3. INTELLECTUAL PROPERTY
3.1 All work product, inventions, and materials created by Contractor during the term of this Agreement, whether or not related to the services and whether or not created using Client resources, shall be the sole and exclusive property of Client.
3.2 Contractor hereby assigns to Client all right, title, and interest in any pre-existing tools, libraries, or know-how used in the course of performing the services.

4. CONFIDENTIALITY
4.1 Contractor shall keep confidential all information disclosed by Client, in perpetuity.

5. TERM AND TERMINATION
5.1 Client may terminate this Agreement at any time, for any reason or no reason, without notice. Upon termination Client shall have no obligation to pay for work in progress.
5.2 Contractor may terminate this Agreement only upon one hundred twenty (120) days' written notice.

6. NON-COMPETITION
6.1 During the term of this Agreement and for a period of three (3) years thereafter, Contractor shall not provide software development services to any business anywhere in the world, whether or not such business competes with Client.

7. LIABILITY AND INDEMNIFICATION
7.1 Contractor shall indemnify, defend, and hold harmless Client and its affiliates from any and all claims, losses, and damages arising out of or relating to this Agreement, regardless of fault.
7.2 Contractor shall be liable to Client without limitation as to amount or type, including for indirect, incidental, consequential, and punitive damages.
7.3 Client's total liability under this Agreement shall not exceed one hundred dollars ($100).

8. GOVERNING LAW
8.1 This Agreement is governed by the laws of the Client's choosing, and any dispute shall be resolved exclusively in a forum selected by Client.

IN WITNESS WHEREOF, the parties have executed this Agreement as of the date first written above.

Northwind Ventures LLC                    Jordan Rivera
By: ______________________                ______________________
"""

FAIR_NDA = """\
MUTUAL NON-DISCLOSURE AGREEMENT

This Mutual Non-Disclosure Agreement (the "Agreement") is made on April 14, 2026 between Bluebird Design Studio LLC and Casey Morgan, a freelance product designer (each a "Party" and together the "Parties").

1. PURPOSE
The Parties wish to share confidential information to evaluate a possible design engagement (the "Purpose").

2. CONFIDENTIAL INFORMATION
"Confidential Information" means non-public information that a Party marks as confidential or that a reasonable person would understand to be confidential. It does not include information that (a) is or becomes public through no fault of the receiving Party, (b) was known to the receiving Party before disclosure, (c) is independently developed without use of the disclosing Party's information, or (d) is received from a third party without a duty of confidentiality.

3. OBLIGATIONS
Each Party shall use the other Party's Confidential Information only for the Purpose, protect it with at least reasonable care, and share it only with people who need it for the Purpose and are bound by similar obligations.

4. TERM
This Agreement lasts two (2) years from the date above. Confidentiality obligations survive for two (2) years after it ends, except that trade secrets remain protected for as long as they qualify as trade secrets under applicable law.

5. RETURN OF INFORMATION
On written request, each Party shall return or destroy the other Party's Confidential Information within fifteen (15) days, except copies kept in routine backups or required by law.

6. NO LICENSE
Nothing in this Agreement grants either Party any license or ownership interest in the other Party's intellectual property. Each Party keeps ownership of its own pre-existing materials.

7. NO OBLIGATION
Neither Party is obligated to enter into any further agreement. Either Party may end discussions at any time by written notice.

8. REMEDIES AND LIABILITY
Each Party may seek injunctive relief for a breach. Neither Party is liable for indirect or consequential damages, and each Party's total liability is limited to direct damages actually proven.

9. GOVERNING LAW
This Agreement is governed by the laws of the State of Ohio. The Parties will first try to resolve any dispute through good-faith negotiation for thirty (30) days.

Bluebird Design Studio LLC                Casey Morgan
By: ______________________                ______________________
"""


def write_pdf(path, pages):
    """pages: list of (content_stream_bytes, image_xobject_or_None). Writes a minimal valid PDF 1.4."""
    objects = []  # index i holds the body of object i+1

    def add(body):
        objects.append(body)
        return len(objects)

    font = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
    pages_id = add(None)  # filled in once the page ids are known
    page_ids = []
    for content, image in pages:
        content_id = add(stream(content))
        resources = b"<< /Font << /F1 %d 0 R >>" % font
        if image is not None:
            resources += b" /XObject << /Im1 %d 0 R >>" % add(image)
        resources += b" >>"
        page_ids.append(add(b"<< /Type /Page /Parent %d 0 R /MediaBox [0 0 %d %d] /Resources %s /Contents %d 0 R >>"
                            % (pages_id, PAGE_W, PAGE_H, resources, content_id)))
    objects[pages_id - 1] = (b"<< /Type /Pages /Kids [%s] /Count %d >>"
                             % (b" ".join(b"%d 0 R" % p for p in page_ids), len(page_ids)))
    catalog = add(b"<< /Type /Catalog /Pages %d 0 R >>" % pages_id)
    info = add(b"<< /Producer (Clausify synthetic test fixture) /Title (%s) >>" % path.stem.encode())

    out = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = []
    for number, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += b"%d 0 obj\n" % number + body + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 %d\n0000000000 65535 f \n" % (len(objects) + 1)
    out += b"".join(b"%010d 00000 n \n" % o for o in offsets)
    out += (b"trailer\n<< /Size %d /Root %d 0 R /Info %d 0 R >>\nstartxref\n%d\n%%%%EOF\n"
            % (len(objects) + 1, catalog, info, xref))
    path.write_bytes(bytes(out))


def stream(data, extra=b""):
    data = zlib.compress(data)
    return b"<< /Length %d /Filter /FlateDecode%s >>\nstream\n" % (len(data), extra) + data + b"\nendstream"


def pdf_string(text):
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").encode("latin-1")


def text_pages(document):
    lines = []
    for paragraph in document.splitlines():
        lines.extend(textwrap.wrap(paragraph, WRAP) or [""])
    pages = []
    for start in range(0, len(lines), LINES_PER_PAGE):
        ops = [b"BT /F1 %d Tf %d TL %d %d Td" % (FONT_SIZE, LEADING, MARGIN, PAGE_H - MARGIN)]
        ops += [b"(%s) '" % pdf_string(line) for line in lines[start:start + LINES_PER_PAGE]]
        ops.append(b"ET")
        pages.append((b"\n".join(ops), None))
    return pages


def scanned_pages(count=2):
    """Grayscale page images with dark bars where lines of text would be: what a scan looks like, with no text layer."""
    w, h = PAGE_W, PAGE_H
    pages = []
    for page in range(count):
        rows = []
        for y in range(h):
            line_index, offset = divmod(y - MARGIN, LEADING)
            in_text = MARGIN <= y < h - MARGIN and offset < 8
            # Ragged right edge, like real paragraphs; some lines left blank between paragraphs.
            line_end = w - MARGIN - (line_index * 37 + page * 53) % 140
            blank = line_index % 9 == 8
            row = bytes(40 if in_text and not blank and MARGIN <= x < line_end else 245 for x in range(w))
            rows.append(row)
        image = stream(b"".join(rows), b" /Type /XObject /Subtype /Image /Width %d /Height %d"
                                         b" /ColorSpace /DeviceGray /BitsPerComponent 8" % (w, h))
        pages.append((b"q %d 0 0 %d 0 0 cm /Im1 Do Q" % (w, h), image))
    return pages


if __name__ == "__main__":
    write_pdf(HERE / "risky-consulting-agreement.pdf", text_pages(RISKY_CONSULTING))
    write_pdf(HERE / "fair-nda.pdf", text_pages(FAIR_NDA))
    write_pdf(HERE / "scanned-image-only.pdf", scanned_pages())
    print("wrote 3 fixtures to", HERE)
