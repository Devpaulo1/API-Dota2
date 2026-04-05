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
                version = "1.0.0",
                description = "API desenvolvida para a disciplina de backend no Senac TSI. Fornece dados atualizados sobre Heróis, Itens, Times, Jogadores e Ligas do universo de Dota 2.",
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
                @Server(url = "http://localhost:8080", description = "Servidor Local (Desenvolvimento)"),
                @Server(url = "https://api-dota2.onrender.com", description = "Servidor de Produção (Render)")

        }
)
public class OpenApiConfig {

}
