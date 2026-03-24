package senac.tsi.dota2.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.dota2.entities.Hero;
import senac.tsi.dota2.exceptions.HeroNotFoundException;
import senac.tsi.dota2.repositories.HeroRepository;

import java.net.URI;
import java.util.List;


/**
 * ============================================================================
 * PROJETO: Dota 2 REST API - Senac TSI
 * ============================================================================
 * * HISTÓRICO E ARQUITETURA (Até o Dia 2):
 * * 1. O Molde e o Banco de Dados (Entity e Repository):
 * - Criação da entidade Hero mapeando os dados para o padrão Java.
 * - Uso do Jackson 3 (@JsonProperty e @JsonIgnoreProperties) para traduzir
 * o JSON original em inglês da API oficial do jogo.
 * - Configuração do H2 Database (Banco em memória) gerenciado pelo Spring Data JPA.
 * * 2. Carga Inicial de Dados (Preloading):
 * - Implementação da classe LoadDatabase na camada de Infrastructure.
 * - Uso do HttpClient nativo do Java 25 para consumir a OpenDota API
 * (/api/heroes) no momento de inicialização (Startup) do servidor.
 * - Salvamento automático de mais de 120 heróis oficiais no banco local.
 * * 3. Tratamento de Erros e Segurança (Exceptions):
 * - Implementação do GlobalErrorAdvice (@RestControllerAdvice) funcionando
 * como um "para-raios" para a aplicação inteira.
 * - Criação da HeroNotFoundException para interceptar buscas inválidas e
 * garantir o retorno do Status HTTP 404 (Not Found), evitando o Erro 500.
 * * 4. Documentação Viva (Swagger / OpenAPI):
 * - Configuração do springdoc-openapi no pom.xml e criação do OpenApiConfig.
 * - Uso das anotações @Tag, @Operation e @ApiResponse no Controller para
 * gerar uma interface web interativa de testes.
 * * 5. O Controlador RESTful (Richardson Maturity Model - Nível 2):
 * - Construção do CRUD completo usando ResponseEntity para controle rigoroso
 * dos Status HTTP, padrão exigido pelo mercado:
 * -> GET    : Listar todos (200 OK)
 * -> GET    : Buscar por ID (200 OK ou 404 Not Found)
 * -> POST   : Criar novo (201 Created)
 * -> PUT    : Atualizar existente (200 OK ou 201 Created)
 * -> DELETE : Remover do banco (204 No Content ou 404 Not Found)
 * ============================================================================
 */
@Tag(name = "Heroes", description = "Rotas para gerenciar os Heróis do Dota 2")
@RestController
@RequestMapping("/api/heroes")
public class HeroController {

    private final HeroRepository repository;

    public HeroController(HeroRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Listar todos os heróis", description = "Retorna a lista completa de heróis carregados da OpenDota API.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Hero> getAllHeroes() {
        return repository.findAll();
    }

    @Operation(summary = "Buscar herói por ID", description = "Busca um herói específico. Retorna 404 Not Found se o ID não existir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Herói encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Herói não encontrado")
    })
    @GetMapping("/{id}")
    public Hero getHeroById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new HeroNotFoundException(id));
    }

    @Operation(summary = "Criar um novo herói", description = "Adiciona um herói customizado ao banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Herói criado com sucesso")
    })
    @PostMapping
    public ResponseEntity<Hero> createHero(@RequestBody Hero newHero) {
        Hero savedHero = repository.save(newHero);
        return ResponseEntity
                .created(URI.create("/api/heroes/" + savedHero.getId()))
                .body(savedHero);
    }

    @Operation(summary = "Atualizar um herói", description = "Atualiza os dados de um herói existente. Se o ID não existir, ele cria um novo.")
    @PutMapping("/{id}")
    public ResponseEntity<Hero> updateHero(@PathVariable Long id, @RequestBody Hero updatedHero) {
        return repository.findById(id)
                .map(hero -> {
                    hero.setLocalizedName(updatedHero.getLocalizedName());
                    hero.setAttackType(updatedHero.getAttackType());
                    return ResponseEntity.ok(repository.save(hero));
                })
                .orElseGet(() -> {
                    updatedHero.setId(id);
                    return ResponseEntity
                            .created(URI.create("/api/heroes/" + id))
                            .body(repository.save(updatedHero));
                });
    }

    @Operation(summary = "Deletar um herói", description = "Remove um herói do banco de dados local pelo ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Herói deletado com sucesso (Sem conteúdo no retorno)"),
            @ApiResponse(responseCode = "404", description = "Herói não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHero(@PathVariable Long id) {
        Hero hero = repository.findById(id).orElseThrow(() -> new HeroNotFoundException(id));

        repository.delete(hero);

        return ResponseEntity.noContent().build();
    }
}