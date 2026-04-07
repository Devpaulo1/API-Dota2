package senac.tsi.dota2.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import senac.tsi.dota2.entities.Hero;
import senac.tsi.dota2.entities.Player;
import senac.tsi.dota2.entities.Team;
import senac.tsi.dota2.repositories.HeroRepository;
import senac.tsi.dota2.repositories.PlayerRepository;
import senac.tsi.dota2.repositories.TeamRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(HeroRepository heroRepository, TeamRepository teamRepository, PlayerRepository playerRepository) {
        return args -> {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            // 1. CARREGANDO OS HERÓIS
            try {
                HttpRequest heroRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.opendota.com/api/heroes"))
                        .GET()
                        .build();

                HttpResponse<String> heroResponse = client.send(heroRequest, HttpResponse.BodyHandlers.ofString());
                List<Hero> heroes = mapper.readValue(heroResponse.body(), new TypeReference<List<Hero>>() {});

                heroRepository.saveAll(heroes);
                log.info(heroes.size() + " Heroes saved from OpenDota.");
            } catch (Exception e) {
                log.error("Error fetching Heroes: " + e.getMessage());
            }

            // 2. CARREGANDO OS TIMES (Salvamento individual para evitar crash de transação)
            List<Team> validTeams = new ArrayList<>();
            try {
                HttpRequest teamRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.opendota.com/api/teams"))
                        .GET()
                        .build();

                HttpResponse<String> teamResponse = client.send(teamRequest, HttpResponse.BodyHandlers.ofString());
                List<Team> allTeams = mapper.readValue(teamResponse.body(), new TypeReference<List<Team>>() {});

                int count = 0;
                for (Team t : allTeams) {
                    if (count >= 20) break; // Reduzi para 20 times para ser mais rápido e seguro

                    // Validação manual antes de salvar para evitar o Erro 500/400
                    if (t.getId() != null && t.getName() != null && !t.getName().isBlank()) {
                        try {
                            // Se a tag ou rating vierem nulos da API, garantimos o valor padrão
                            if (t.getTag() == null) t.setTag("TBD");
                            if (t.getRating() == null) t.setRating(0.0);

                            teamRepository.save(t);
                            validTeams.add(t);
                            count++;
                        } catch (Exception ex) {
                            // Se um time específico der erro (ex: nome duplicado), ignora e pula pro próximo
                            log.warn("Skipping team " + t.getName() + " due to data error.");
                        }
                    }
                }
                log.info(validTeams.size() + " Teams successfully synced.");
            } catch (Exception e) {
                log.error("Critical error fetching Teams: " + e.getMessage());
            }

            // 3. CARREGANDO JOGADORES DE TESTE (Mock)
            if (validTeams != null && !validTeams.isEmpty()) {
                log.info("Linking test players to synced teams...");

                // Buscamos os times DIRETO do banco para garantir que o Hibernate não tente criar novos
                Team time1 = teamRepository.findById(validTeams.get(0).getId()).orElse(null);
                Team time2 = (validTeams.size() > 1) ? teamRepository.findById(validTeams.get(1).getId()).orElse(null) : time1;

                if (time1 != null && playerRepository.count() == 0) {
                    Player p1 = new Player();
                    p1.setNickname("KillRockts");
                    p1.setRealName("Paulo");
                    p1.setTeam(time1);
                    playerRepository.save(p1);

                    if (time2 != null) {
                        Player p2 = new Player();
                        p2.setNickname("GamerX");
                        p2.setRealName("Carlos Eduardo");
                        p2.setTeam(time2);
                        playerRepository.save(p2);
                    }

                    log.info("Test players created successfully!");
                }
            }
        };
    }
}