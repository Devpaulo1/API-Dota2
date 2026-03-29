package senac.tsi.dota2;

import org.springdoc.core.configuration.SpringDocHateoasConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// O Escudo: Impede o Swagger de tentar carregar o HATEOAS antigo e quebrar o projeto
@SpringBootApplication(exclude = {SpringDocHateoasConfiguration.class})
public class Dota2Application {

	public static void main(String[] args) {
		SpringApplication.run(Dota2Application.class, args);
	}

}