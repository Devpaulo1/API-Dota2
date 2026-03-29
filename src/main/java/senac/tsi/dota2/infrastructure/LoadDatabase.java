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
import java.util.List;

@Configuration
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(HeroRepository heroRepository, TeamRepository teamRepository, PlayerRepository playerRepository) {
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


            List<Team> topTeams = null; // Declaramos aqui fora para poder usar no Passo 3
            try {
                HttpRequest teamRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.opendota.com/api/teams"))
                        .GET()
                        .build();

                HttpResponse<String> teamResponse = client.send(teamRequest, HttpResponse.BodyHandlers.ofString());
                List<Team> teams = mapper.readValue(teamResponse.body(), new TypeReference<List<Team>>() {});

                teams.removeIf(t -> t.getId() == null || t.getName() == null || t.getName().trim().isEmpty());
                topTeams = teams.size() > 50 ? teams.subList(0, 50) : teams;

                teamRepository.saveAll(topTeams);
                log.info(topTeams.size() + " Times salvos do OpenDota.");
            } catch (Exception e) {
                log.error("Erro ao buscar Times: " + e.getMessage());
            }


            // 3. CARREGANDO JOGADORES DE TESTE (Mock)
            if (topTeams != null && !topTeams.isEmpty()) {
                log.info("Iniciando vínculo de Jogadores aos Times baixados...");

                // Pega os dois primeiros times que vieram da API para não dar erro
                Team time1 = topTeams.get(0);
                Team time2 = topTeams.size() > 1 ? topTeams.get(1) : time1;

                Player p1 = new Player();
                p1.setNickname("KillRockts");
                p1.setRealName("Paulo");
                p1.setTeam(time1); // Vincula ao primeiro time real da API!
                playerRepository.save(p1);

                Player p2 = new Player();
                p2.setNickname("GamerX");
                p2.setRealName("Carlos Eduardo");
                p2.setTeam(time2); // Vincula ao segundo time real da API!
                playerRepository.save(p2);

                log.info("2 Jogadores criados e vinculados aos times reais com sucesso!");
            }
        };
    }
}