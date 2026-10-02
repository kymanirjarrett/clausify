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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-4: listing and reading contracts, and that users can only ever see their own. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContractQueryIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    private User maria;
    private User bob;

    @BeforeEach
    void createUsers() {
        // Fresh users per test, so other tests' contracts never show up in these lists.
        maria = newUser("maria");
        bob = newUser("bob");
    }

    @Test
    void listIsNewestFirstAndPaged() throws Exception {
        for (int i = 1; i <= 3; i++) {
            contracts.save(Contract.uploaded(maria, "contract-" + i + ".pdf", 1000L * i, i, "text", T0.plusSeconds(i)));
        }

        as(maria, "/api/contracts?page=0&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].filename").value("contract-3.pdf"))
                .andExpect(jsonPath("$.content[1].filename").value("contract-2.pdf"))
                .andExpect(jsonPath("$.content[0].status").value("UPLOADED"))
                .andExpect(jsonPath("$.content[0].extractedText").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        as(maria, "/api/contracts?page=1&size=2")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].filename").value("contract-1.pdf"));
    }

    @Test
    void clientCannotChangeTheSortOrder() throws Exception {
        contracts.save(Contract.uploaded(maria, "older.pdf", 1, 1, "text", T0));
        contracts.save(Contract.uploaded(maria, "newer.pdf", 1, 1, "text", T0.plusSeconds(60)));

        as(maria, "/api/contracts?sort=uploadedAt,asc")
                .andExpect(jsonPath("$.content[0].filename").value("newer.pdf"));
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        as(maria, "/api/contracts?size=5000").andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void listOnlyContainsOwnContracts() throws Exception {
        contracts.save(Contract.uploaded(maria, "maria.pdf", 1, 1, "text", T0));
        contracts.save(Contract.uploaded(bob, "bob.pdf", 1, 1, "text", T0));

        as(bob, "/api/contracts")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].filename").value("bob.pdf"));
    }

    @Test
    void detailShowsStatusAndFailureReason() throws Exception {
        Contract contract = Contract.uploaded(maria, "Retainer_Agreement_Draft.pdf", 2048, 4, "text", T0);
        contract.markAnalyzing();
        contract.markFailed("The AI service did not respond.");
        Long id = contracts.save(contract).getId();

        as(maria, "/api/contracts/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.filename").value("Retainer_Agreement_Draft.pdf"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("The AI service did not respond."))
                .andExpect(jsonPath("$.extractedText").doesNotExist());
    }

    /** FR-4 example 3: another user's contract and a missing one get byte-for-byte the same response. */
    @Test
    void otherUsersContractIsIndistinguishableFromMissing() throws Exception {
        Long mariasContract = contracts.save(Contract.uploaded(maria, "private.pdf", 1, 1, "text", T0)).getId();
        long missing = mariasContract + 1_000_000;

        String notYours = as(bob, "/api/contracts/" + mariasContract)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Contract not found."))
                .andReturn().getResponse().getContentAsString();

        as(bob, "/api/contracts/" + missing)
                .andExpect(status().isNotFound())
                .andExpect(content().json(notYours.replace("/" + mariasContract, "/" + missing)));
    }

    @Test
    void endpointsRequireAToken() throws Exception {
        mockMvc.perform(get("/api/contracts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/contracts/1")).andExpect(status().isUnauthorized());
    }

    private ResultActions as(User user, String url) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser(String name) {
        return users.save(User.builder().name(name).email(name + "." + UUID.randomUUID() + "@example.com").password("hash").build());
    }
}
