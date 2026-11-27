package com.clausify.contract;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-3 example 3 and #111. MockMvc skips the servlet container's multipart limits, so these send real uploads to a
 * running server, the way a browser would. The limit is 10 MB in Spring's binary units (10 × 1024 × 1024 bytes),
 * the same number the frontend checks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ContractUploadSizeLimitTest {

    private static final int LIMIT = 10 * 1024 * 1024;

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ContractRepository contracts;

    /** A valid PDF right at the limit is accepted, even though multipart headers push the request over 10 MB. */
    @ParameterizedTest
    @ValueSource(ints = {LIMIT - 1, LIMIT})
    void validPdfUpToTheLimitIs202(int size) throws Exception {
        User owner = newUser();

        HttpResponse<String> response = upload(owner, paddedPdf(size));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(contracts.countByUserId(owner.getId())).isEqualTo(1);
    }

    @Test
    void oneByteOverTheLimitIs413AndSavesNothing() throws Exception {
        User owner = newUser();

        HttpResponse<String> response = upload(owner, paddedPdf(LIMIT + 1));

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("Files must be 10 MB or smaller.");
        assertThat(contracts.countByUserId(owner.getId())).isZero();
    }

    @Test
    void pdfFarOver10MbIs413() throws Exception {
        User owner = newUser();
        byte[] bigPdf = Arrays.copyOf("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII), 14 * 1024 * 1024);

        HttpResponse<String> response = upload(owner, bigPdf);

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(contracts.countByUserId(owner.getId())).isZero();
    }

    /** The real fair-nda fixture with comment lines after %%EOF, which PDF readers ignore. */
    private static byte[] paddedPdf(int size) throws Exception {
        byte[] pdf = new ClassPathResource("contracts/fair-nda.pdf").getContentAsByteArray();
        byte[] padded = Arrays.copyOf(pdf, size);
        Arrays.fill(padded, pdf.length, size, (byte) ' ');
        for (int i = pdf.length; i < size; i += 80) {
            padded[i] = '\n';
            if (i + 1 < size) padded[i + 1] = '%';
        }
        return padded;
    }

    private HttpResponse<String> upload(User owner, byte[] pdf) throws Exception {
        String boundary = "clausify-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"contract.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        body.write(pdf);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));

        return HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/contracts"))
                        .header("Authorization", "Bearer " + jwtService.issueToken(owner))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private User newUser() {
        return users.save(User.builder().name("Upload").email("upload." + UUID.randomUUID() + "@example.com")
                .password("hash").build());
    }
}
