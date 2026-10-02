package com.clausify.contract;

/** What PdfTextExtractor pulls out of an uploaded PDF. */
public record ExtractedDocument(String text, int pageCount) {
}
