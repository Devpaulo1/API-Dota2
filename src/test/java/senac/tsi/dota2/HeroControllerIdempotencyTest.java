package senac.tsi.dota2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import senac.tsi.dota2.repositories.HeroRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HeroControllerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HeroRepository heroRepository;

    @BeforeEach
    void setUp() {
        heroRepository.deleteAll();
    }

    @Test
    void shouldReturnTheOriginalResponseWhenIdempotencyKeyIsReusedWithSamePayload() throws Exception {
        // Agora com snake_case (attack_type) e Enum correto (Melee)
        String requestBody = """
                {
                  "id": 1001,
                  "name": "npc_dota_hero_axe",
                  "localized_name": "Axe",
                  "attack_type": "Melee"
                }
                """;

        MvcResult firstResponse = mockMvc.perform(post("/api/heroes")
                        .header("Idempotency-Key", "hero-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        String firstLocation = firstResponse.getResponse().getHeader("Location");
        assertThat(firstLocation).isNotBlank();

        MvcResult secondResponse = mockMvc.perform(post("/api/heroes")
                        .header("Idempotency-Key", "hero-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", firstLocation))
                .andReturn();

        assertThat(secondResponse.getResponse().getContentAsString())
                .isEqualTo(firstResponse.getResponse().getContentAsString());

        assertThat(heroRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectReusedIdempotencyKeyWithDifferentPayload() throws Exception {
        mockMvc.perform(post("/api/heroes")
                        .header("Idempotency-Key", "hero-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 1002,
                                  "name": "npc_dota_hero_pudge",
                                  "localized_name": "Pudge",
                                  "attack_type": "Melee"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/heroes")
                        .header("Idempotency-Key", "hero-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 1003,
                                  "name": "npc_dota_hero_sniper",
                                  "localized_name": "Sniper",
                                  "attack_type": "Ranged"
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(heroRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectRequestsWhenNoIdempotencyKeyIsProvided() throws Exception {
        String requestBody = """
                {
                  "id": 1004,
                  "name": "npc_dota_hero_crystal_maiden",
                  "localized_name": "Crystal Maiden",
                  "attack_type": "Ranged"
                }
                """;

        mockMvc.perform(post("/api/heroes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(heroRepository.count()).isZero();
    }

    @Test
    void shouldRejectBlankIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/heroes")
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 1005,
                                  "name": "npc_dota_hero_lina",
                                  "localized_name": "Lina",
                                  "attack_type": "Ranged"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(heroRepository.count()).isZero();
    }
}