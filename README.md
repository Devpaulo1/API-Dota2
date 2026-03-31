# Dota 2 REST API - Senac TSI

Projeto acadêmico desenvolvido para a disciplina de backend no curso de TSI do Senac. Esta aplicação é uma API RESTful Nível 3 construída em Java com **Spring Boot 3+**, seguindo o padrão de arquitetura em camadas (Controller, Repository, Entity, Exception). 

A aplicação consome dados em tempo real da API pública oficial do jogo Dota 2 (OpenDota API) para alimentar o banco de dados inicialmente, e disponibiliza um ecossistema completo para gerenciar Heróis, Times, Jogadores, Perfis e Itens, utilizando relacionamentos complexos, paginação e links de navegabilidade (HATEOAS).

## Funcionalidades

- **Carga Inicial Automática (Mock & Fetch):** Ao iniciar, o servidor faz requisições HTTP (`HttpClient`) para a OpenDota API, baixando Heróis e Times reais. Também gera dados "mockados" de Jogadores e Perfis para testes imediatos.
- **Mapeamento JSON:** Utiliza a biblioteca Jackson (`@JsonProperty`) para desserializar os dados em inglês para objetos Java de forma automática.
- **Relacionamentos de Banco de Dados:** Implementação rigorosa de mapeamento ORM:
  - **One-to-One:** Jogador (Player) e Perfil (PlayerProfile).
  - **One-to-Many:** Time (Team) e Jogadores (Players).
  - **Many-to-Many:** Heróis (Heroes) e Itens (Items).
- **HATEOAS (Maturidade Nível 3):** Respostas enriquecidas com links dinâmicos (`_links`), garantindo navegabilidade e descobrimento da API.
- **Paginação de Dados:** Todas as rotas de listagem (`GET` gerais e filtros) implementam o `Pageable` nativo do Spring, evitando sobrecarga de dados.
- **Validação de Dados (Bean Validation):** Tratamento rigoroso de entradas com anotações como `@NotBlank`, `@NotNull` e `@Size`.
- **Tratamento Global de Exceções:** Retornos padronizados de status HTTP (`404 Not Found`, `201 Created`, `204 No Content`) isolados na camada de exceptions.
- **Documentação Automática:** Interface gráfica interativa (Swagger UI) para testes rápidos de todas as rotas.

## Tecnologias Utilizadas

O projeto foi construído com as seguintes ferramentas e tecnologias:

- **Java 17+** (Linguagem principal)
- **Spring Boot 3.x** (Framework base)
- **Spring Web** (Criação da API REST)
- **Spring Data JPA** (Comunicação, ORM e mapeamento do banco de dados)
- **Spring HATEOAS** (Implementação de links REST Nível 3)
- **Springdoc OpenAPI (Swagger)** (Geração de documentação automatizada)
- **Jakarta Validation** (Validação de entidades e regras de negócio)
- **H2 Database** (Banco de dados SQL em memória)
- **Lombok** (Geração automática de código boilerplate)
- **Jackson** (Manipulação e conversão de dados JSON)

## Como Executar Localmente

Siga os passos abaixo para rodar o projeto na sua máquina:

1. Clone este repositório.
2. Abra a pasta do projeto na sua IDE (recomendado: **IntelliJ IDEA**).
3. Aguarde o Maven baixar todas as dependências automaticamente.
4. Certifique-se de que o **Lombok** está ativado (habilite o *Annotation Processing* na sua IDE).
5. Navegue até `src/main/java/senac/tsi/dota2/` e execute o arquivo `DotaApplication.java` (botão de Play).
6. O servidor iniciará na porta `8080`.

*(Nota: Se a porta 8080 estiver ocupada, altere no arquivo `application.properties` usando `server.port=8081`)*

## Documentação e Endpoints (Swagger)

Como a API possui dezenas de rotas e relacionamentos complexos, toda a documentação de uso, parâmetros necessários e exemplos de respostas (JSON) estão disponíveis interativamente via Swagger.

Com o servidor rodando, acesse no seu navegador:
**[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Resumo das Rotas Principais

A API fornece operações **CRUD Completas** (GET, POST, PUT, DELETE) e consultas personalizadas paginadas para as 5 entidades base:

- `GET /api/heroes` - Gerenciamento de Heróis e filtro por Tipo de Ataque.
- `GET /api/teams` - Gerenciamento de Times e filtro por Nome.
- `GET /api/players` - Gerenciamento de Jogadores e filtro por Nickname.
- `GET /api/player-profiles` - Gerenciamento de Perfis de Jogadores (Estatísticas e Biografia) e filtro por Twitter.
- `GET /api/items` - Gerenciamento de Itens in-game e filtro por Nome do Item.

## Arquitetura do Projeto

O projeto segue a arquitetura em camadas recomendada pela documentação oficial do Spring Boot, garantindo separação de responsabilidades:

- **Entities:** O molde do domínio. Define a estrutura das tabelas no banco de dados, regras de validação (`@Valid`) e os mapeamentos de relacionamento (`@OneToMany`, `@ManyToMany`, etc).
- **Repositories:** A interface do Spring Data JPA que gerencia as operações de banco de dados no H2, incluindo métodos de consultas personalizadas paginadas (ex: `findByAttackType`).
- **Controllers:** O ponto de entrada da API. Define as rotas HTTP, recebe requisições, aplica a paginação (`PagedResourcesAssembler`), constrói os links HATEOAS e retorna o JSON com os códigos de Status HTTP corretos.
- **Exceptions:** Camada dedicada a interceptar erros de negócio (ex: buscar um ID que não existe) e devolver respostas limpas, evitando exposição de erros internos do servidor.
- **Infrastructure (LoadDatabase):** Classe executada no Startup da aplicação. Utiliza o `HttpClient` nativo do Java para consumir a API externa e semear o banco de dados com informações reais para facilitar os testes.
