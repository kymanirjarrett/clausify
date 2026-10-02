package com.clausify.contract;

import com.clausify.common.UnreadablePdfException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extracts text from an uploaded PDF with Apache PDFBox, entirely in memory: the file is never written
 * to disk (ADR 0005), including PDFBox's own scratch storage, which is forced to memory-only.
 */
@Component
public class PdfTextExtractor {

    /** Below this many non-whitespace characters the PDF is treated as a scan (page images, no text layer). */
    static final int MIN_TEXT_CHARACTERS = 200;

    static final String NO_TEXT = "We couldn't read text from this PDF. Scanned documents aren't supported yet.";
    static final String ENCRYPTED = "This PDF is password-protected. Remove the password and upload it again.";
    static final String CORRUPT = "This file couldn't be opened as a PDF. It may be damaged.";

    public ExtractedDocument extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf, "", null, null, IOUtils.createMemoryOnlyStreamCache())) {
            String text = new PDFTextStripper().getText(document).strip();
            if (countNonWhitespace(text) < MIN_TEXT_CHARACTERS) {
                throw new UnreadablePdfException(NO_TEXT);
            }
            return new ExtractedDocument(text, document.getNumberOfPages());
        } catch (InvalidPasswordException ex) {
            throw new UnreadablePdfException(ENCRYPTED);
        } catch (IOException ex) {
            throw new UnreadablePdfException(CORRUPT);
        }
    }

    private static long countNonWhitespace(String text) {
        return text.codePoints().filter(c -> !Character.isWhitespace(c)).count();
    }
}
