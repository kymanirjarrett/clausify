package com.clausify.contract;

import com.clausify.common.UnreadablePdfException;

/**
 * Run in a separate JVM with a small heap by PdfTextExtractorIsolationTest, so a regression that runs out of
 * memory kills only this process. Prints the outcome on one line.
 */
public final class HostilePdfProbe {

    private HostilePdfProbe() {
    }

    public static void main(String[] args) throws Exception {
        long rawMegabytes = Long.parseLong(args[0]);
        byte[] pdf = HostilePdfs.textBomb(rawMegabytes * 1024 * 1024);
        try {
            new PdfTextExtractor().extract(pdf);
            System.out.println("ACCEPTED");
        } catch (UnreadablePdfException ex) {
            System.out.println("REJECTED " + ex.getMessage());
        }
        // An ordinary contract must still work in the same JVM right after the rejection.
        try (var in = HostilePdfProbe.class.getResourceAsStream("/contracts/fair-nda.pdf")) {
            System.out.println("NEXT_PAGES " + new PdfTextExtractor().extract(in.readAllBytes()).pageCount());
        }
    }
}
