package senac.tsi.dota2.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    // Puxa a chave secreta que configuramos no application.properties
    @Value("${api.security.key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. ÁREA VIP (Libera o Swagger e a documentação para não pedir senha)
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-resources")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. EXTRAÇÃO: Pega o cabeçalho que o cliente enviou
        String reqApiKey = request.getHeader("X-API-KEY");

        // 3. VALIDAÇÃO: Bate a chave enviada com a nossa chave secreta
        if (expectedApiKey.equals(reqApiKey)) {
            // Chave correta! Pode passar para o próximo passo (Controller ou Rate Limiter)
            filterChain.doFilter(request, response);
        } else {
            // Chave errada ou ausente! Bloqueia na porta e retorna 401 Unauthorized
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or missing X-API-KEY\"}");
        }
    }
}