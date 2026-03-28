package senac.tsi.dota2.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import senac.tsi.dota2.entities.Hero;
import senac.tsi.dota2.entities.Team;
import senac.tsi.dota2.repositories.HeroRepository;
import senac.tsi.dota2.repositories.TeamRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Configuration
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(HeroRepository heroRepository, TeamRepository teamRepository) {
        return args -> {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            // ==========================================
            // 1. CARREGANDO OS HERÓIS
            // ==========================================
            try {
                HttpRequest heroRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.opendota.com/api/heroes"))
                        .GET()
                        .build();

                HttpResponse<String> heroResponse = client.send(heroRequest, HttpResponse.BodyHandlers.ofString());
                List<Hero> heroes = mapper.readValue(heroResponse.body(), new TypeReference<List<Hero>>() {});

                heroRepository.saveAll(heroes);
                log.info( heroes.size() + " Heróis salvos do OpenDota.");
            } catch (Exception e) {
                log.error("Erro ao buscar Heróis: " + e.getMessage());
            }

            // ==========================================
            // 2. CARREGANDO OS TIMES (O Mesmo Padrão!)
            // ==========================================
            try {
                HttpRequest teamRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.opendota.com/api/teams"))
                        .GET()
                        .build();

                HttpResponse<String> teamResponse = client.send(teamRequest, HttpResponse.BodyHandlers.ofString());
                List<Team> teams = mapper.readValue(teamResponse.body(), new TypeReference<List<Team>>() {});

                // O TRUQUE AQUI: Removemos os times que vêm em branco da API para o banco não recusar a lista
                teams.removeIf(t -> t.getId() == null || t.getName() == null || t.getName().trim().isEmpty());

                // Pegamos só os 50 primeiros para não demorar a ligar o projeto
                List<Team> topTeams = teams.size() > 50 ? teams.subList(0, 50) : teams;

                teamRepository.saveAll(topTeams);
                log.info(topTeams.size() + " Times salvos do OpenDota.");
            } catch (Exception e) {
                log.error("Erro ao buscar Times: " + e.getMessage());
            }
        };
    }
}