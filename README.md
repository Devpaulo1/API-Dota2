
# Dota 2 REST API - Senac TSIS

Projeto acadêmico desenvolvido para a disciplina de backend no curso de TSI do Senac. Esta aplicação é uma API RESTful construída em Java com **Spring Boot 4**, seguindo o padrão de arquitetura em camadas (Controller, Repository, Entity). 

A aplicação consome dados em tempo real da API pública oficial do jogo Dota 2 (OpenDota API), processa essas informações e as disponibiliza através de endpoints próprios, utilizando um banco de dados em memória para armazenamento rápido.


##  Funcionalidades

- **Carga Inicial Automática (Preloading):** Ao iniciar, o servidor faz uma requisição HTTP para a OpenDota API e faz o download da lista atualizada com todos os heróis do jogo.
- **Mapeamento JSON:** Utiliza a biblioteca Jackson para desserializar os dados em inglês (`localized_name`, `attack_type`) para objetos Java de forma automática.
- **Persistência em Memória:** Salva os heróis capturados em um banco de dados relacional (H2 Database) sem necessidade de instalação local de SGBDs.
- **Endpoint REST:** Disponibiliza os dados processados em formato JSON puro para consumo de aplicações frontend (Web ou Mobile).


## Tecnologias Utilizadas

O projeto foi construído com as seguintes ferramentas e tecnologias:

- **Java 25** (Linguagem principal)
- **Spring Boot 4.0.3** (Framework base)
- **Spring Web** (Criação da API REST e Controllers)
- **Spring Data JPA** (Comunicação e mapeamento do banco de dados)
- **H2 Database** (Banco de dados SQL em memória para desenvolvimento)
- **Lombok** (Geração automática de Getters, Setters e Construtores)
- **Jackson (tools.jackson)** (Manipulação e conversão de dados JSON)

## Como Executar Localmente

Siga os passos abaixo para rodar o projeto na sua máquina:

- Clone este repositório.
- Abra a pasta do projeto na sua IDE (recomendado: **IntelliJ IDEA**).
- Aguarde o Maven baixar todas as dependências automaticamente.
- Caso o Lombok não esteja ativado, habilite o *Annotation Processing* na sua IDE.
- Navegue até `src/main/java/senac/tsi/dota2/` e execute o arquivo `DotaApplication.java` (botão de Play).
- O servidor iniciará na porta `8080`.

*(Nota: Se a porta 8080 estiver ocupada, altere no arquivo `application.properties` usando `server.port=8081`)*
## API Reference

#### Retorna todos os heróis cadastrados

```http
  GET /api/heroes
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `Nenhum` | `N/A` | Retorna a lista completa de heróis |

#### Get item

```http
  GET /api/heroes/{id}
```

| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `id`      | `Long` | **Obrigatório**. O ID oficial do herói no jogo |




## Arquitetura do Projeto

[Documentação OpenDota](https://docs.opendota.com/) | [Tutorial Spring Boot](https://spring.io/guides/tutorials/rest)

- O projeto segue a arquitetura em camadas recomendada pela documentação oficial do Spring Boot (Building a RESTful Web Service). Ele está estruturado nos seguintes pilares:

- **Entities** (Hero.java): O molde do domínio. Define a estrutura da tabela no banco de dados e utiliza as anotações do Jackson (@JsonProperty) para traduzir o JSON original da OpenDota API para o padrão Java.

- **Repositories** (HeroRepository.java): A interface do Spring Data JPA que gerencia as operações de banco de dados no H2 (salvar, buscar, deletar) de forma automática, sem a necessidade de escrever scripts SQL manuais.

- **Infrastructure** (LoadDatabase.java): A classe de configuração executada no momento de inicialização (Startup) do servidor. Ela utiliza o HttpClient nativo do Java para fazer o consumo da API externa e salvar os dados no banco local.

- **Controllers** (HeroController.java): O ponto de entrada da nossa API (a camada Web). É responsável por definir as rotas (@RequestMapping), receber as requisições HTTP do cliente e devolver as respostas serializadas em JSON.
