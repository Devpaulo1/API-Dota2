package senac.tsi.dota2.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Dota 2 REST API",
                version = "1.1.0",
                description = """
                API RESTful desenvolvida para o gerenciamento de dados do universo de Dota 2, consumindo informações da OpenDota API e aplicando persistência relacional customizada. O foco principal do projeto é a aplicação de conceitos avançados de arquitetura backend, atingindo o Nível 3 do Modelo de Maturidade de Richardson.
                
                            Visão Geral da Arquitetura e Tecnologias Utilizadas
                
                Esta aplicação foi desenvolvida seguindo boas práticas de backend, para garantir que o código fique organizado, os dados sejam consistentes e o sistema seja fácil de manter e evoluir.
                
                - Persistência dos Dados
                
                    Para armazenar as informações, usamos um banco de dados relacional em memória chamado **H2** — ideal para testes rápidos, pois não precisa de instalação extra. As entidades (como jogadores, times e heróis) estão conectadas entre si de formas que refletem a realidade do jogo DOTA2:
                   - **Player e PlayerProfile:** cada jogador tem um perfil exclusivo (relação um-para-um).
                   - **Team e Players:** um time pode ter vários jogadores (um-para-muitos).
                   - **Heroes e Items:** heróis podem possuir vários itens, e itens podem ser usados por vários heróis (muitos-para-muitos).

                     Essas conexões ajudam a manter os dados organizados e evitam que informações fiquem inconsistentes.
                
                - Validação dos Dados
                
                Antes de aceitar qualquer informação enviada para a API, fazemos uma checagem automática para garantir que tudo esteja correto, usando uma ferramenta chamada **Bean Validation**. Isso impede que dados errados ou incompletos causem problemas depois.
                
                - Tratamento de Erros
                
                    Para lidar com situações inesperadas, a API possui um sistema centralizado que captura erros e responde de forma clara e padronizada. Por exemplo:
                   - Se um recurso não existir, a API retorna um erro **404 (Não Encontrado)**.
                   - Se uma operação não gerar conteúdo, ela responde com **204 (Sem Conteúdo)**.
                   - Quando algo é criado com sucesso, retorna **201 (Criado)**.
                
                  Assim, quem usa a API sempre sabe o que esperar e pode tratar as respostas corretamente.
                
                            Implementação de HATEOAS (Navegabilidade):
                
                Para tornar a API mais intuitiva e fácil de consumir, foi adotado o padrão **HATEOAS**, onde cada resposta já traz os caminhos (links) necessários para navegar entre os recursos. Dessa forma, o cliente não precisa conhecer previamente todas as rotas da API — ele simplesmente segue os links retornados.
                
                - **Hero:** retorna um link **self**, que aponta para os detalhes daquele **herói**, e um link heroes, permitindo voltar para a listagem paginada.
                - **Item:** inclui o link **self** para acesso direto ao item e **items** para retornar à coleção completa.
                - **Player:** além do **self**, disponibiliza também o link para o recurso **team**, facilitando o acesso direto à equipe do jogador.
                - **PlayerProfile:** fornece o **self** para os dados detalhados do perfil e **player-profiles** para navegação na coleção.
                - **Team:** retorna o **self** com os dados da equipe e o link **teams** para acessar a lista paginada de equipes.
                
                Com essa abordagem, a API se torna mais autodescritiva, reduz o acoplamento com o cliente e melhora a experiência de integração, já que a navegação entre os recursos acontece de forma mais natural e guiada.
                
                            Deploy e Ambientes:
                
                A aplicação foi disponibilizada em um ambiente real de execução, utilizando a plataforma Render (PaaS). Com isso, é possível acessar a API publicamente, facilitando testes, validações e demonstrações do funcionamento fora do ambiente local.
                
                A utilização de uma plataforma em nuvem também contribui para simular um cenário mais próximo do mercado, garantindo maior confiabilidade, disponibilidade e facilidade no processo de deploy.
                
                Este projeto foi desenvolvido como parte dos requisitos acadêmicos do curso de Tecnologia em Sistemas para Internet (TSI) do Senac, aplicando na prática conceitos de desenvolvimento backend, arquitetura de APIs e boas práticas de engenharia de software.
                
                """,
                contact = @Contact(
                        name = "Paulo",
                        email = "dev.paulo@outlook.com"
                ),
                license = @License(
                        name = "OpenDota API",
                        url = "https://docs.opendota.com/"
                )
        ),
        servers = {
                @Server(url = "https://api-dota2.onrender.com", description = "Servidor de Produção (Render)"),
                @Server(url = "http://localhost:8080", description = "Servidor Local (Desenvolvimento)")
        }
)
public class OpenApiConfig {
}