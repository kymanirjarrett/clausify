package com.clausify.analysis;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FR-11: the analysis as a downloadable PDF, drawn with PDFBox in memory (nothing is written to disk). Reads through
 * {@link AnalysisResultService}, so ownership (404) and "not analyzed yet" (409) behave exactly like the results page.
 */
@Service
public class ReportService {

    static final String DISCLAIMER = "Clausify provides information, not legal advice. "
            + "Review important contracts with a qualified lawyer.";

    private static final float MARGIN = 56;
    private static final float WIDTH = PDRectangle.LETTER.getWidth() - 2 * MARGIN;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US).withZone(ZoneOffset.UTC);
    private static final Map<String, String> LABELS = Map.of(
            "PAYMENT_TERMS", "Payment terms", "LIABILITY", "Liability", "INDEMNIFICATION", "Indemnification",
            "IP_RIGHTS", "IP rights", "TERMINATION", "Termination", "NON_COMPETE", "Non-compete",
            "CONFIDENTIALITY", "Confidentiality", "JURISDICTION", "Jurisdiction");

    private final AnalysisResultService analyses;

    public ReportService(AnalysisResultService analyses) {
        this.analyses = analyses;
    }

    /** A finished report and the file name to download it as. */
    public record Report(String filename, byte[] pdf) {
    }

    public Report render(Long contractId, Long userId) {
        AnalysisResponse a = analyses.get(contractId, userId);
        return new Report(filename(a.document().filename()), draw(a));
    }

    /** {@code Clausify_Report_<contract name>.pdf} (FR-11), keeping only characters safe in a download header. */
    static String filename(String original) {
        String name = original.replaceFirst("(?i)\\.pdf$", "");
        name = Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("_+", "_").replaceAll("^_|_$", "");
        return "Clausify_Report_" + (name.isEmpty() ? "contract" : name) + ".pdf";
    }

    private byte[] draw(AnalysisResponse a) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle("Clausify report: " + a.document().filename());
            info.setCreator("Clausify");
            Writer w = new Writer(document);

            w.text(Font.BOLD, 20, "Contract risk report");
            w.text(Font.REGULAR, 11, a.document().filename());
            w.text(Font.REGULAR, 9, "Uploaded " + DATE.format(a.document().uploadedAt())
                    + (a.document().analyzedAt() == null ? "" : "  ·  Analyzed " + DATE.format(a.document().analyzedAt()))
                    + (a.contractType() == null ? "" : "  ·  " + a.contractType())
                    + "  ·  " + a.document().pageCount() + (a.document().pageCount() == 1 ? " page" : " pages"));
            // On page 1 of every report, so it can't be separated from the findings it qualifies.
            w.paragraph(Font.ITALIC, 9, DISCLAIMER);
            w.gap(14);

            w.text(Font.BOLD, 28, a.overallScore() + " / 100");
            w.text(Font.BOLD, 12, level(a.riskLevel()) + " risk");
            if (a.summary() != null) {
                w.paragraph(Font.REGULAR, 10, a.summary());
            }
            w.gap(10);

            w.heading("Risk by category");
            if (a.categories().isEmpty()) {
                w.paragraph(Font.REGULAR, 10, "No risky clauses were found in any category.");
            }
            for (AnalysisResponse.CategoryRisk c : a.categories()) {
                w.paragraph(Font.REGULAR, 10, LABELS.get(c.category().name()) + ": " + c.highestScore() + " ("
                        + level(c.riskLevel()) + "), " + c.findingCount() + (c.findingCount() == 1 ? " finding" : " findings"));
            }

            w.heading("Findings");
            if (a.findings().isEmpty()) {
                w.paragraph(Font.REGULAR, 10, "No clauses outside tolerance were found.");
            }
            for (AnalysisResponse.Finding f : a.findings()) {
                finding(w, f, true);
            }
            if (!a.favorable().isEmpty()) {
                w.heading("What's in your favor");
                for (AnalysisResponse.Finding f : a.favorable()) {
                    finding(w, f, false);
                }
            }

            w.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render the report", e);
        }
    }

    private static void finding(Writer w, AnalysisResponse.Finding f, boolean risky) throws IOException {
        w.gap(6);
        w.paragraph(Font.BOLD, 11, f.title());
        w.paragraph(Font.REGULAR, 9, (risky ? level(f.riskLevel()) + " · " + f.riskScore() : "Favorable") + "  ·  "
                + LABELS.get(f.category().name()) + (f.sectionRef() == null ? "" : "  ·  " + f.sectionRef()));
        w.paragraph(Font.ITALIC, 9.5f, "“" + f.originalText() + "”");
        w.paragraph(Font.BOLD, 9.5f, risky ? "Why this matters" : "Why it helps");
        w.paragraph(Font.REGULAR, 10, f.explanation());
        if (f.suggestedRevision() != null) {
            w.paragraph(Font.BOLD, 9.5f, "Suggested revision");
            w.paragraph(Font.REGULAR, 10, f.suggestedRevision());
        }
    }

    private static String level(RiskLevel level) {
        return switch (level) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
        };
    }

    private enum Font {
        REGULAR(Standard14Fonts.FontName.HELVETICA),
        BOLD(Standard14Fonts.FontName.HELVETICA_BOLD),
        ITALIC(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        final PDType1Font pdf;

        Font(Standard14Fonts.FontName name) {
            this.pdf = new PDType1Font(name);
        }
    }

    /** Writes wrapped lines top to bottom, starting a new page when one fills up. */
    private static final class Writer {

        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        Writer(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void heading(String text) throws IOException {
            gap(12);
            text(Font.BOLD, 13, text);
            gap(2);
        }

        void text(Font font, float size, String text) throws IOException {
            paragraph(font, size, text);
        }

        void paragraph(Font font, float size, String text) throws IOException {
            for (String line : wrap(font, size, Glyphs.safe(font.pdf, text))) {
                if (y - size * 1.4f < MARGIN) {
                    newPage();
                }
                y -= size * 1.4f;
                stream.beginText();
                stream.setFont(font.pdf, size);
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(line);
                stream.endText();
            }
        }

        void gap(float points) {
            y -= points;
        }

        void close() throws IOException {
            stream.close();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private static List<String> wrap(Font font, float size, String text) throws IOException {
            List<String> lines = new ArrayList<>();
            for (String paragraph : text.split("\n")) {
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split(" ")) {
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    if (font.pdf.getStringWidth(candidate) / 1000 * size <= WIDTH || line.isEmpty()) {
                        line.setLength(0);
                        line.append(candidate);
                    } else {
                        lines.add(line.toString());
                        line.setLength(0);
                        line.append(word);
                    }
                }
                lines.add(line.toString());
            }
            return lines;
        }
    }

    /**
     * The standard PDF fonts only cover WinAnsi (roughly Latin-1). Model output can contain anything (production
     * replies used U+2011, a non-breaking hyphen), and PDFBox throws on a character it cannot encode. Each such
     * character becomes its closest ASCII form, or "?" as a last resort.
     */
    static final class Glyphs {

        private static final Map<Integer, String> REPLACEMENTS = Map.of(
                0x2010, "-", 0x2011, "-", 0x2012, "-", 0x2192, "->", 0x2190, "<-", 0x2264, "<=", 0x2265, ">=",
                0x00A0, " ", 0x2009, " ", 0x202F, " ");
        private static final Map<String, Boolean> ENCODABLE = new java.util.concurrent.ConcurrentHashMap<>();

        private Glyphs() {
        }

        static String safe(PDType1Font font, String text) {
            StringBuilder out = new StringBuilder(text.length());
            text.replace("\r", "").replace("\t", " ").codePoints().forEach(cp -> {
                String ch = new String(Character.toChars(cp));
                if (ch.equals("\n") || encodable(font, ch)) {
                    out.append(ch);
                } else if (REPLACEMENTS.containsKey(cp)) {
                    out.append(REPLACEMENTS.get(cp));
                } else {
                    String ascii = Normalizer.normalize(ch, Normalizer.Form.NFKD).replaceAll("[^\\x20-\\x7E]", "");
                    out.append(ascii.isEmpty() ? "?" : ascii);
                }
            });
            return out.toString();
        }

        private static boolean encodable(PDType1Font font, String ch) {
            return ENCODABLE.computeIfAbsent(font.getName() + ch, key -> {
                try {
                    font.encode(ch);
                    return true;
                } catch (IOException | IllegalArgumentException e) {
                    return false;
                }
            });
        }
    }
}
