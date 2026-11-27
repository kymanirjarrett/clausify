package com.clausify.contract;

import com.clausify.common.UnreadablePdfException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extracts text from an uploaded PDF with Apache PDFBox, entirely in memory: the file is never written
 * to disk (ADR 0005), including PDFBox's own scratch storage, which is forced to memory-only.
 * <p>
 * A small PDF can expand enormously once decompressed (#110: a 0.9 MB file drawing ~300 MB of text ran a 256 MB
 * heap out of memory). Three caps keep one upload from taking the server down: PDFBox's scratch memory, the page
 * count, and the characters extracted. Hitting a cap is a 422 with a plain message. There is no separate time
 * limit: work is bounded by these caps, and the request thread finishes or fails on its own.
 */
@Component
public class PdfTextExtractor {

    /** Below this many non-whitespace characters the PDF is treated as a scan (page images, no text layer). */
    static final int MIN_TEXT_CHARACTERS = 200;

    /** Well above what analysis accepts (about 26 pages), so ordinary long contracts get the analyzer's message. */
    static final int MAX_PAGES = 100;
    static final int MAX_TEXT_CHARACTERS = 200_000;
    /** PDFBox scratch memory for decoded streams such as images and fonts; exceeding it fails the load. */
    static final long MAX_SCRATCH_BYTES = 64L * 1024 * 1024;

    static final String NO_TEXT = "We couldn't read text from this PDF. Scanned documents aren't supported yet.";
    static final String ENCRYPTED = "This PDF is password-protected. Remove the password and upload it again.";
    static final String CORRUPT = "This file couldn't be opened as a PDF. It may be damaged.";
    static final String TOO_MANY_PAGES = "This PDF has more than " + MAX_PAGES + " pages. Upload a shorter contract.";
    static final String TOO_MUCH_TEXT = "This PDF contains more text than Clausify can read. Upload a shorter contract.";

    public ExtractedDocument extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf, "", null, null,
                MemoryUsageSetting.setupMainMemoryOnly(MAX_SCRATCH_BYTES).streamCache)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new UnreadablePdfException(TOO_MANY_PAGES);
            }
            String text = new CappedTextStripper().getText(document).strip();
            if (countNonWhitespace(text) < MIN_TEXT_CHARACTERS) {
                throw new UnreadablePdfException(NO_TEXT);
            }
            return new ExtractedDocument(text, document.getNumberOfPages());
        } catch (TooMuchTextException ex) {
            throw new UnreadablePdfException(TOO_MUCH_TEXT);
        } catch (InvalidPasswordException ex) {
            throw new UnreadablePdfException(ENCRYPTED);
        } catch (IOException ex) {
            // Includes running past MAX_SCRATCH_BYTES; the message stays generic on purpose.
            throw new UnreadablePdfException(CORRUPT);
        }
    }

    private static long countNonWhitespace(String text) {
        return text.codePoints().filter(c -> !Character.isWhitespace(c)).count();
    }

    /**
     * Counts characters as PDFBox reads them, before it collects a page's text in memory. Capping the output alone
     * would be too late: the stripper holds every character of a page until the page ends.
     */
    private static final class CappedTextStripper extends PDFTextStripper {

        private long characters;

        @Override
        protected void processTextPosition(TextPosition text) {
            characters += text.getUnicode() == null ? 1 : text.getUnicode().length();
            if (characters > MAX_TEXT_CHARACTERS) {
                throw new TooMuchTextException();
            }
            super.processTextPosition(text);
        }
    }

    private static final class TooMuchTextException extends RuntimeException {
        TooMuchTextException() {
            super(null, null, false, false); // control flow only: no stack trace to build
        }
    }
}
