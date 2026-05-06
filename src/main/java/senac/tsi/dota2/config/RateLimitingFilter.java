package senac.tsi.dota2.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements Filter {

    // Mapa para guardar um "balde" exclusivo para cada IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Método que cria o balde com as regras que você definiu
    private Bucket createNewBucket() {
        // Capacidade total: 20 fichas
        // Recarga: Ganha 2 fichas a cada 1 minuto
        Bandwidth limit = Bandwidth.classic(20, Refill.intervally(2, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    // Busca o balde do IP atual ou cria um novo se ele nunca acessou
    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, k -> createNewBucket());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Pega o IP de quem está fazendo a requisição
        String ip = httpRequest.getRemoteAddr();
        Bucket bucket = resolveBucket(ip);

        // 2. Tenta consumir 1 ficha e coleta o status detalhado (ConsumptionProbe)
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Envia no Header quantas requisições o cara ainda pode fazer (Bônus bem legal)
            httpResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            // 3. MÁGICA AQUI: Pega os nanossegundos exatos que faltam e converte para segundos
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

            httpResponse.setContentType("text/plain");
            httpResponse.setStatus(429);

            // Requisito oficial do professor (Cabeçalho Retry-After)
            httpResponse.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));

            // A mensagem de texto no Postman com o tempo dinâmico cravado
            httpResponse.getWriter().append("Too many requests! Wait " + waitForRefillSeconds + " seconds to try again.");
        }
    }
}