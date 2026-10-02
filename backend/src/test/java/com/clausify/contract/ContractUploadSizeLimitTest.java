package com.clausify.contract;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-3 example 3. MockMvc skips the servlet container's multipart limits, so this sends a real
 * 14 MB upload to a running server, the way a browser would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ContractUploadSizeLimitTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ContractRepository contracts;

    @Test
    void pdfOver10MbIs413() throws Exception {
        User owner = users.save(User.builder().name("Big").email("big.upload@example.com").password("hash").build());
        byte[] bigPdf = Arrays.copyOf("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII), 14 * 1024 * 1024);
        long before = contracts.count();

        String boundary = "clausify-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"huge.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        body.write(bigPdf);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));

        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/contracts"))
                        .header("Authorization", "Bearer " + jwtService.issueToken(owner))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("Files must be 10 MB or smaller.");
        assertThat(contracts.count()).isEqualTo(before);
    }
}
