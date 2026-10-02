package com.clausify.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Security filters are off: this test is about error formatting, not authentication.
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundIs404() throws Exception {
        expectProblem(mockMvc.perform(get("/test/not-found")), 404, "Contract 42 not found.");
    }

    @Test
    void conflictIs409() throws Exception {
        expectProblem(mockMvc.perform(get("/test/conflict")), 409, "An account with this email already exists.");
    }

    @Test
    void unsupportedFileTypeIs415() throws Exception {
        expectProblem(mockMvc.perform(get("/test/unsupported-file")), 415, "Only PDF files are supported.");
    }

    @Test
    void unreadablePdfIs422() throws Exception {
        expectProblem(mockMvc.perform(get("/test/unreadable-pdf")), 422,
                "We couldn't read text from this PDF. Scanned documents aren't supported yet.");
    }

    @Test
    void uploadTooLargeIs413() throws Exception {
        expectProblem(mockMvc.perform(get("/test/too-large")), 413, "Files must be 10 MB or smaller.");
    }

    @Test
    void validationErrorIs400WithOneMessagePerField() throws Exception {
        expectProblem(mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"not-an-email\"}")),
                400, "One or more fields are invalid.")
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email must be valid"));
    }

    @Test
    void unsupportedContentTypeIs415() throws Exception {
        expectProblem(mockMvc.perform(post("/test/validated").contentType(MediaType.TEXT_PLAIN).content("hi")),
                415, "Unsupported content type.");
    }

    @Test
    void unexpectedErrorIs500WithoutInternalDetails() throws Exception {
        expectProblem(mockMvc.perform(get("/test/boom")), 500, "An unexpected error occurred.")
                .andExpect(content().string(not(containsString("secret-internal-detail"))));
    }

    private ResultActions expectProblem(ResultActions result, int status, String detail) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.title").exists());
    }

    record ValidatedRequest(@NotBlank(message = "Name is required") String name,
                            @Email(message = "Email must be valid") String email) {
    }

    /** Test-only endpoints that throw each exception. Nested in a test, so app scanning skips it. */
    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("Contract 42 not found.");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("An account with this email already exists.");
        }

        @GetMapping("/test/unsupported-file")
        void unsupportedFile() {
            throw new UnsupportedFileTypeException("Only PDF files are supported.");
        }

        @GetMapping("/test/unreadable-pdf")
        void unreadablePdf() {
            throw new UnreadablePdfException("We couldn't read text from this PDF. Scanned documents aren't supported yet.");
        }

        @GetMapping("/test/too-large")
        void tooLarge() {
            throw new MaxUploadSizeExceededException(10 * 1024 * 1024);
        }

        @PostMapping(path = "/test/validated", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validated(@Valid @RequestBody ValidatedRequest request) {
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("secret-internal-detail");
        }
    }
}
