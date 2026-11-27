package com.clausify.contract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/** Synthetic hostile PDFs for #110. Nothing here comes from a real document. */
final class HostilePdfs {

    private HostilePdfs() {
    }

    /**
     * One page whose compressed content stream draws {@code rawBytes} of text. The file stays small (about 0.3% of
     * rawBytes) while extraction would produce text roughly as large as rawBytes.
     */
    static byte[] textBomb(long rawBytes) throws IOException {
        byte[] line = "(Contractor shall pay all damages and costs arising) Tj T* ".getBytes(StandardCharsets.US_ASCII);
        byte[] chunk = new byte[line.length * 1000];
        for (int i = 0; i < 1000; i++) {
            System.arraycopy(line, 0, chunk, i * line.length, line.length);
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflate = new DeflaterOutputStream(compressed)) {
            deflate.write("BT /F1 10 Tf 12 TL 72 720 Td ".getBytes(StandardCharsets.US_ASCII));
            for (long written = 0; written < rawBytes; written += chunk.length) {
                deflate.write(chunk, 0, (int) Math.min(chunk.length, rawBytes - written));
            }
        }
        byte[] stream = compressed.toByteArray();

        // Written by hand: PDFBox's own writer would buffer the whole uncompressed stream first.
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(pdf, "%PDF-1.7\n");
        offsets.add(pdf.size());
        write(pdf, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        offsets.add(pdf.size());
        write(pdf, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        offsets.add(pdf.size());
        write(pdf, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
                + "/Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> >>\nendobj\n");
        offsets.add(pdf.size());
        write(pdf, "4 0 obj\n<< /Length " + stream.length + " /Filter /FlateDecode >>\nstream\n");
        pdf.write(stream);
        write(pdf, "\nendstream\nendobj\n");
        int xref = pdf.size();
        write(pdf, "xref\n0 5\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(pdf, String.format("%010d 00000 n \n", offset));
        }
        write(pdf, "trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return pdf.toByteArray();
    }

    /** {@code pages} blank pages: cheap to read, but over the page cap. */
    static byte[] manyPages(int pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static void write(OutputStream out, String ascii) throws IOException {
        out.write(ascii.getBytes(StandardCharsets.US_ASCII));
    }
}
