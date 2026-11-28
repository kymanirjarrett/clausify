package com.clausify.analysis;

import com.clausify.TestcontainersConfiguration;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-11: GET /api/contracts/{id}/report. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReportIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private AnalysisRepository analyses;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    private User maria;

    @BeforeEach
    void createUser() {
        maria = newUser();
    }

    /** FR-11 example 1: the score, the categories, every finding with its revision, and the disclaimer. */
    @Test
    void reportContainsScoreCategoriesFindingsAndDisclaimer() throws Exception {
        Contract contract = analyzed(maria, "Consulting Agreement v2.pdf",
                finding(RiskCategory.LIABILITY, 85, false, "Unlimited liability", "Fairer cap wording."),
                finding(RiskCategory.NON_COMPETE, 90, false, "Broad worldwide non‑competition", "Six months, US only."),
                finding(RiskCategory.TERMINATION, 20, true, "Paid for work done", null));

        byte[] pdf = as(maria, get("/api/contracts/" + contract.getId() + "/report"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Clausify_Report_Consulting_Agreement_v2.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        String text = textOf(pdf);
        assertThat(text)
                .contains("Contract risk report", "Consulting Agreement v2.pdf", "80 / 100", "High risk")
                .contains("Risk by category", "Non-compete: 90 (High), 1 finding", "Liability: 85 (High), 1 finding")
                .contains("Unlimited liability", "Fairer cap wording.", "Suggested revision")
                // U+2011 from real model output is not in the PDF font; it is drawn as a plain hyphen.
                .contains("Broad worldwide non-competition")
                .contains("What's in your favor", "Paid for work done")
                .contains(ReportService.DISCLAIMER.substring(0, 40));
        assertThat(text.indexOf("Broad worldwide")).isLessThan(text.indexOf("Unlimited liability"));
    }

    @Test
    void longReportsContinueOnNewPages() throws Exception {
        ClauseFinding[] many = new ClauseFinding[30];
        for (int i = 0; i < many.length; i++) {
            many[i] = finding(RiskCategory.PAYMENT_TERMS, 50 + i, false, "Clause " + i, "Revision " + i);
        }
        Contract contract = analyzed(maria, "long.pdf", many);

        byte[] pdf = as(maria, get("/api/contracts/" + contract.getId() + "/report"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(document)).contains("Clause 0", "Clause 29", "Revision 29");
        }
    }

    /** FR-11 example 2 at the API level; errors stay JSON even when the client asked for a PDF. */
    @Test
    void notAnalyzedYetIs409() throws Exception {
        Contract pending = contracts.save(Contract.uploaded(maria, "nda.pdf", 1000, 1, "text", T0));

        as(maria, get("/api/contracts/" + pending.getId() + "/report").accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.contractStatus").value("UPLOADED"));
    }

    @Test
    void anotherUsersReportIs404() throws Exception {
        Contract bobs = analyzed(newUser(), "secret.pdf", finding(RiskCategory.LIABILITY, 85, false, "Secret", "x"));

        as(maria, get("/api/contracts/" + bobs.getId() + "/report").accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/contracts/" + bobs.getId() + "/report")).andExpect(status().isUnauthorized());
    }

    @Test
    void filenameKeepsOnlyHeaderSafeCharacters() {
        assertThat(ReportService.filename("NDA ClientCo v1.PDF")).isEqualTo("Clausify_Report_NDA_ClientCo_v1.pdf");
        assertThat(ReportService.filename("Résumé \"draft\";.pdf")).isEqualTo("Clausify_Report_Resume_draft.pdf");
        assertThat(ReportService.filename("....pdf")).isEqualTo("Clausify_Report_....pdf");
        assertThat(ReportService.filename("**.pdf")).isEqualTo("Clausify_Report_contract.pdf");
    }

    private static String textOf(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private Contract analyzed(User owner, String filename, ClauseFinding... findings) {
        Contract contract = Contract.uploaded(owner, filename, 4096, 2, "Full contract text.", T0);
        contract.markAnalyzing();
        contract.markAnalyzed(T0.plusSeconds(30));
        contracts.save(contract);
        Analysis analysis = new Analysis(null, contract, 80, RiskLevel.HIGH, "Consulting Agreement",
                "Risk concentrated in liability.", "test-model", 19400L, new ArrayList<>(), null);
        for (ClauseFinding finding : findings) {
            analysis.addFinding(finding);
        }
        analyses.save(analysis);
        return contract;
    }

    private static ClauseFinding finding(RiskCategory category, int score, boolean favorable, String title, String revision) {
        return new ClauseFinding(null, null, title, "7.2", category, score, RiskLevel.fromScore(score), favorable,
                "Original clause → text.", "Why it matters.", revision, null);
    }

    private ResultActions as(User user, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser() {
        return users.save(User.builder().name("Report").email("rep." + UUID.randomUUID() + "@example.com")
                .password("hash").build());
    }
}
