package senac.tsi.dota2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a liberação para TODAS as rotas da sua API
                .allowedOriginPatterns("*") // Permite que QUALQUER site/aplicativo consuma sua API
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Métodos liberados
                .allowedHeaders("*") // Libera nossos cabeçalhos customizados (X-API-KEY, X-API-VERSION, Idempotency-Key)
                .exposedHeaders("X-Rate-Limit-Remaining", "Retry-After") // Deixa o front-end ler os cabeçalhos de erro do Rate Limiter
                .maxAge(3600); // Faz o navegador fazer "cache" daquela requisição OPTIONS por 1 hora (deixa a API mais rápida)
    }
}