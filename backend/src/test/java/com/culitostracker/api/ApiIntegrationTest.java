package com.culitostracker.api;

import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.AccessStatus;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.PartnerAccess;
import com.culitostracker.repository.PartnerAccessRepository;
import com.culitostracker.repository.PartnerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests against a real PostgreSQL (Testcontainers) covering the
 * privacy and authorization guarantees:
 * - IDOR: user B can never read user A's partner by guessing the ID
 * - check-ins are invisible without an explicit consent grant
 * - the leaderboard only shows opted-in users
 * - domain rules (monogamy) are enforced by the API, not the UI
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PartnerRepository partnerRepository;
    @Autowired
    PartnerAccessRepository partnerAccessRepository;
    @Autowired
    com.culitostracker.application.PasswordResetService passwordResetService;

    String tokenA;
    String tokenB;
    String partnerIdOfA;

    private String register(String username, String password) throws Exception {
        String body = """
                {"username":"%s","email":"%s@test.local","password":"%s","displayName":"%s"}
                """.formatted(username, username, password, username);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    @Order(1)
    void registerAndLogin() throws Exception {
        tokenA = register("alice", "password-a1");
        tokenB = register("bob", "password-b1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"alice\",\"password\":\"password-a1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("alice"));

        // Wrong password → 422 with a generic code, no account enumeration.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("auth.invalidCredentials"));
    }

    @Test
    @Order(2)
    void protectedEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/partners")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void createPartnerAndReadIt() throws Exception {
        String body = """
                {"name":"Laura Martínez","relationshipType":"SERIOUS_RELATIONSHIP",
                 "relationshipStartDate":"2024-02-14","consentConfirmed":true}
                """;
        MvcResult result = mockMvc.perform(post("/api/partners")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationship.type").value("SERIOUS_RELATIONSHIP"))
                .andReturn();
        partnerIdOfA = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/api/partners/" + partnerIdOfA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laura Martínez"));
    }

    @Test
    @Order(4)
    void idorIsRejectedWith404() throws Exception {
        // User B guesses user A's partner ID: must look nonexistent, not forbidden.
        mockMvc.perform(get("/api/partners/" + partnerIdOfA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/partners/" + partnerIdOfA + "/cycle")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/partners/" + partnerIdOfA + "/check-ins")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/partners/" + partnerIdOfA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON).content("{\"name\":\"hacked\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void monogamousRuleEnforcedByBackend() throws Exception {
        String monogamous = """
                {"name":"Sofía Rojas","relationshipType":"MONOGAMOUS",
                 "relationshipStartDate":"2025-01-01","consentConfirmed":true}
                """;
        // Alice already has an active SERIOUS_RELATIONSHIP → declaring a
        // monogamous one must be rejected with 422.
        mockMvc.perform(post("/api/partners")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content(monogamous))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("relationship.monogamousConflict"));
    }

    @Test
    @Order(6)
    void duplicatePartnerNameRejected() throws Exception {
        String duplicate = """
                {"name":"laura  martínez","relationshipType":"CASUAL","consentConfirmed":true}
                """;
        mockMvc.perform(post("/api/partners")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content(duplicate))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("partner.duplicate"));
    }

    @Test
    @Order(7)
    void periodsAndPredictions() throws Exception {
        mockMvc.perform(post("/api/partners/" + partnerIdOfA + "/periods")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/partners/" + partnerIdOfA + "/cycle/predictions")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insufficientData").value(false))
                .andExpect(jsonPath("$.currentPhase").value("MENSTRUATION"))
                .andExpect(jsonPath("$.disclaimers").isArray())
                .andExpect(jsonPath("$.nextPeriodStart").isNotEmpty());
    }

    @Test
    @Order(8)
    void checkInsPrivateUntilGranted() throws Exception {
        // Alice checks in about her relationship.
        String checkIn = """
                {"partnerId":"%s","mood":"HAPPY","energyLevel":4,"stressLevel":2,
                 "note":"solo para mí"}
                """.formatted(partnerIdOfA);
        mockMvc.perform(post("/api/check-ins")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content(checkIn))
                .andExpect(status().isCreated());

        // Bob (unrelated) cannot even see the resource exists.
        mockMvc.perform(get("/api/partners/" + partnerIdOfA + "/check-ins")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Now link Bob as the partner's account (simulates an accepted invite).
        UUID bobId = registerIdOf("bob");
        Partner partner = partnerRepository.findById(UUID.fromString(partnerIdOfA)).orElseThrow();
        partner.setLinkedUserId(bobId);
        partnerRepository.save(partner);

        // Linked but WITHOUT a grant: Bob sees only his own (zero) check-ins.
        MvcResult withoutGrant = mockMvc.perform(get("/api/partners/" + partnerIdOfA + "/check-ins")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(withoutGrant.getResponse().getContentAsString()))
                .isEmpty();

        // Alice grants CHECK_INS to Bob → Bob now sees her mood but not her note.
        PartnerAccess grant = new PartnerAccess();
        grant.setPartnerId(UUID.fromString(partnerIdOfA));
        grant.setGrantedByUserId(registerIdOf("alice"));
        grant.setGrantedToUserId(bobId);
        grant.setScope(AccessScope.CHECK_INS);
        grant.setStatus(AccessStatus.ACTIVE);
        partnerAccessRepository.save(grant);

        MvcResult withGrant = mockMvc.perform(get("/api/partners/" + partnerIdOfA + "/check-ins")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode checkIns = objectMapper.readTree(withGrant.getResponse().getContentAsString());
        assertThat(checkIns).hasSize(1);
        assertThat(checkIns.get(0).get("mood").asText()).isEqualTo("HAPPY");
        assertThat(checkIns.get(0).get("note").isNull()).isTrue();

        // Revocation cuts access again.
        grant.setStatus(AccessStatus.REVOKED);
        partnerAccessRepository.save(grant);
        MvcResult revoked = mockMvc.perform(get("/api/partners/" + partnerIdOfA + "/check-ins")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(revoked.getResponse().getContentAsString())).isEmpty();
    }

    @Test
    @Order(9)
    void leaderboardOnlyShowsOptedInUsers() throws Exception {
        // Nobody opted in yet → empty ranking even though partners exist.
        MvcResult empty = mockMvc.perform(get("/api/leaderboard")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(empty.getResponse().getContentAsString())
                .get("entries")).isEmpty();

        // Enabling without an alias is rejected.
        mockMvc.perform(patch("/api/leaderboard/me/settings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("leaderboard.aliasRequired"));

        // Alias + enable → Alice appears, with only alias and score.
        mockMvc.perform(patch("/api/leaderboard/me/settings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"enabled\":true,\"publicAlias\":\"AliceWonder\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        MvcResult ranking = mockMvc.perform(get("/api/leaderboard")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode entries = objectMapper.readTree(ranking.getResponse().getContentAsString())
                .get("entries");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).get("alias").asText()).isEqualTo("AliceWonder");
        assertThat(entries.get(0).get("score").asLong()).isEqualTo(1);
        // The payload never contains partner names or dates.
        assertThat(entries.get(0).has("partners")).isFalse();

        // Opting out removes her immediately.
        mockMvc.perform(patch("/api/leaderboard/me/settings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        MvcResult after = mockMvc.perform(get("/api/leaderboard")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(after.getResponse().getContentAsString())
                .get("entries")).isEmpty();
    }

    @Test
    @Order(10)
    void dashboardAndStatsRespond() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partners[0].name").value("Laura Martínez"))
                .andExpect(jsonPath("$.partners[0].duration").isNotEmpty());

        mockMvc.perform(get("/api/stats/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnersRegistered").value(1));
    }

    @Test
    @Order(11)
    void avatarUploadValidateFetchDelete() throws Exception {
        // A real tiny PNG generated in-memory.
        var image = new java.awt.image.BufferedImage(64, 48, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);

        // Garbage with a .png name is rejected: validation is by magic bytes.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(org.springframework.http.HttpMethod.PUT, "/api/users/me/avatar")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "fake.png", "image/png", "not an image".getBytes()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("avatar.invalidImage"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(org.springframework.http.HttpMethod.PUT, "/api/users/me/avatar")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "me.png", "image/png", baos.toByteArray()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // Owner gets it back, re-encoded as square JPEG; profile reports hasAvatar.
        mockMvc.perform(get("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo("image/jpeg"));
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.hasAvatar").value(true));

        // Another user has no avatar of their own: 404, and there is no
        // endpoint at all to fetch someone else's photo.
        mockMvc.perform(get("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(12)
    void agendaDatePlansAreOwnerScoped() throws Exception {
        String plan = """
                {"partnerId":"%s","title":"Cena en el centro","date":"2026-12-24","startTime":"20:30"}
                """.formatted(partnerIdOfA);
        MvcResult created = mockMvc.perform(post("/api/date-plans")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content(plan))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partnerName").value("Laura Martínez"))
                .andReturn();
        String planId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/api/date-plans?from=2026-12-01&to=2026-12-31")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Cena en el centro"));

        // Bob can neither see nor edit Alice's plan; nor schedule with her partner.
        mockMvc.perform(patch("/api/date-plans/" + planId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON).content("{\"title\":\"hacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/date-plans")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON).content(plan))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(13)
    void encountersFeedPrivateXp() throws Exception {
        String encounter = """
                {"partnerId":"%s","date":"%s"}
                """.formatted(partnerIdOfA, java.time.LocalDate.now());
        mockMvc.perform(post("/api/encounters")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content(encounter))
                .andExpect(status().isCreated());

        // The future is off-limits; other users' partners look nonexistent.
        mockMvc.perform(post("/api/encounters")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"partnerId":"%s","date":"%s"}
                                """.formatted(partnerIdOfA, java.time.LocalDate.now().plusDays(1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("encounter.dateInFuture"));
        mockMvc.perform(post("/api/encounters")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON).content(encounter))
                .andExpect(status().isNotFound());

        // XP is personal: Alice's encounter counts for her and only her.
        mockMvc.perform(get("/api/xp/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encountersCount").value(1))
                .andExpect(jsonPath("$.totalXp").value(10))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.breakdown[0].partnerName").value("Laura Martínez"));
        mockMvc.perform(get("/api/xp/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encountersCount").value(0))
                .andExpect(jsonPath("$.totalXp").value(0));
    }

    @Test
    @Order(14)
    void partnerAttributesRequireAdultAndFeedXp() throws Exception {
        // A birth date implying a minor is rejected outright.
        mockMvc.perform(patch("/api/partners/" + partnerIdOfA)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"birthDate\":\"%s\"}".formatted(
                                java.time.LocalDate.now().minusYears(17))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("partner.mustBeAdult"));

        // Adult birth date + weight are stored and reflected in XP (10+12+15).
        mockMvc.perform(patch("/api/partners/" + partnerIdOfA)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"birthDate\":\"%s\",\"weightKg\":80}".formatted(
                                java.time.LocalDate.now().minusYears(30).minusDays(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.weightKg").value(80));

        mockMvc.perform(get("/api/xp/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(37));
    }

    @Test
    @Order(15)
    void passwordResetFlow() throws Exception {
        // Unknown email gets the same 204: the endpoint can't enumerate accounts.
        mockMvc.perform(post("/api/auth/forgot")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.local\"}"))
                .andExpect(status().isNoContent());

        // Obtain the raw token via the service (in production it travels by
        // email, or in the server log when no SMTP is configured).
        String token = passwordResetService.requestReset("alice@test.local").orElseThrow();

        // Garbage tokens are rejected with a generic code.
        mockMvc.perform(post("/api/auth/reset")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"not-a-token\",\"newPassword\":\"whatever-123\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("auth.invalidResetToken"));

        mockMvc.perform(post("/api/auth/reset")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"new-password-a1\"}".formatted(token)))
                .andExpect(status().isNoContent());

        // Old password no longer works; the new one does.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"alice\",\"password\":\"password-a1\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"alice\",\"password\":\"new-password-a1\"}"))
                .andExpect(status().isOk());

        // The token is single use.
        mockMvc.perform(post("/api/auth/reset")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"another-pass-1\"}".formatted(token)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("auth.invalidResetToken"));
    }

    private UUID registerIdOf(String username) throws Exception {
        String token = username.equals("alice") ? tokenA : tokenB;
        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText());
    }
}
