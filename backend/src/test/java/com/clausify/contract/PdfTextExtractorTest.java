package com.clausify.contract;

import com.clausify.common.UnreadablePdfException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses the synthetic fixtures in src/test/resources/contracts (see the README there). */
class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extractsTextAndPageCountFromRiskyContract() throws IOException {
        ExtractedDocument document = extractor.extract(fixture("risky-consulting-agreement.pdf"));

        assertThat(document.pageCount()).isEqualTo(2);
        assertThat(document.text())
                .startsWith("CONSULTING SERVICES AGREEMENT")
                .contains("within a reasonable time")
                .contains("without limitation as to amount or type")
                .contains("for a period of three (3) years");
    }

    @Test
    void extractsFairNda() throws IOException {
        ExtractedDocument document = extractor.extract(fixture("fair-nda.pdf"));

        assertThat(document.pageCount()).isEqualTo(2);
        assertThat(document.text()).contains("MUTUAL NON-DISCLOSURE AGREEMENT", "laws of the State of Ohio");
    }

    @Test
    void scannedPdfWithoutTextLayerIsUnreadable() throws IOException {
        assertUnreadable(fixture("scanned-image-only.pdf"), PdfTextExtractor.NO_TEXT);
    }

    @Test
    void pdfWithTooLittleTextIsUnreadable() throws IOException {
        assertUnreadable(pdfWithText("Page 1 of 1", null), PdfTextExtractor.NO_TEXT);
    }

    @Test
    void passwordProtectedPdfIsUnreadable() throws IOException {
        assertUnreadable(pdfWithText("Confidential terms ".repeat(30), "open-sesame"), PdfTextExtractor.ENCRYPTED);
    }

    @Test
    void corruptFileIsUnreadable() throws IOException {
        byte[] truncated = Arrays.copyOf(fixture("fair-nda.pdf"), 300);

        assertUnreadable(truncated, PdfTextExtractor.CORRUPT);
        assertUnreadable("%PDF-1.4 this is not really a PDF".getBytes(), PdfTextExtractor.CORRUPT);
    }

    private void assertUnreadable(byte[] pdf, String message) {
        assertThatThrownBy(() -> extractor.extract(pdf))
                .isInstanceOf(UnreadablePdfException.class)
                .hasMessage(message);
    }

    private static byte[] fixture(String name) throws IOException {
        try (InputStream in = PdfTextExtractorTest.class.getResourceAsStream("/contracts/" + name)) {
            assertThat(in).as("fixture %s", name).isNotNull();
            return in.readAllBytes();
        }
    }

    private static byte[] pdfWithText(String text, String userPassword) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.newLineAtOffset(40, 700);
                content.showText(text);
                content.endText();
            }
            if (userPassword != null) {
                document.protect(new StandardProtectionPolicy("owner-secret", userPassword, new AccessPermission()));
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
