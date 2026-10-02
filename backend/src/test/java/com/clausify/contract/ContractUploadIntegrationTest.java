package com.clausify.contract;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-3 examples 1, 2, and 4 through the full stack (example 3, 413, is in ContractUploadSizeLimitTest). */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class ContractUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationEvents events;

    private User owner;
    private String token;

    @BeforeEach
    void signIn() {
        owner = users.findByEmail("uploader@example.com").orElseGet(() -> users.save(
                User.builder().name("Uploader").email("uploader@example.com").password("hash").build()));
        token = jwtService.issueToken(owner);
    }

    @Test
    void validPdfIsAcceptedSavedAndQueuedForAnalysis() throws Exception {
        String body = upload(pdf("Consulting_Agreement_2026.pdf", fixture("risky-consulting-agreement.pdf")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.filename").value("Consulting_Agreement_2026.pdf"))
                .andExpect(jsonPath("$.pageCount").value(2))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.uploadedAt").exists())
                .andExpect(jsonPath("$.extractedText").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        Contract saved = contracts.findByIdAndUserId(id, owner.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ContractStatus.UPLOADED);
        assertThat(saved.getExtractedText()).contains("without limitation as to amount or type");
        assertThat(saved.getFileSizeBytes()).isEqualTo(fixture("risky-consulting-agreement.pdf").length);
        assertThat(events.stream(ContractUploadedEvent.class)).containsExactly(new ContractUploadedEvent(id));
    }

    @Test
    void locationHeaderPointsAtTheNewContract() throws Exception {
        upload(pdf("NDA.pdf", fixture("fair-nda.pdf")))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.matchesPattern("/api/contracts/\\d+")));
    }

    @Test
    void wordDocumentIs415AndNothingIsSaved() throws Exception {
        long before = contracts.count();
        MockMultipartFile docx = new MockMultipartFile("file", "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "PK\u0003\u0004 not a pdf".getBytes());

        upload(docx)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Only PDF files are supported."));
        assertThat(contracts.count()).isEqualTo(before);
        assertThat(events.stream(ContractUploadedEvent.class)).isEmpty();
    }

    @Test
    void fileClaimingToBePdfWithoutPdfSignatureIs415() throws Exception {
        upload(pdf("fake.pdf", "<html>definitely not a pdf</html>".getBytes()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void scannedPdfIs422AndNothingIsSaved() throws Exception {
        long before = contracts.count();

        upload(pdf("scan.pdf", fixture("scanned-image-only.pdf")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("We couldn't read text from this PDF. Scanned documents aren't supported yet."));
        assertThat(contracts.count()).isEqualTo(before);
        assertThat(events.stream(ContractUploadedEvent.class)).isEmpty();
    }

    @Test
    void missingFilePartIs400() throws Exception {
        mockMvc.perform(multipart("/api/contracts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadWithoutTokenIs401() throws Exception {
        mockMvc.perform(multipart("/api/contracts").file(pdf("NDA.pdf", fixture("fair-nda.pdf"))))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions upload(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/contracts").file(file).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static MockMultipartFile pdf(String filename, byte[] bytes) {
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_PDF_VALUE, bytes);
    }

    static byte[] fixture(String name) throws IOException {
        try (InputStream in = ContractUploadIntegrationTest.class.getResourceAsStream("/contracts/" + name)) {
            return in.readAllBytes();
        }
    }
}
