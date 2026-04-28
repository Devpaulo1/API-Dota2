package senac.tsi.dota2.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitingFilter implements Filter {

    // Criamos um "balde" que aceita 10 moedas e se recarrega a cada 1 minuto
    private final Bucket bucket;

    public RateLimitingFilter() {
        // Capacidade total: 20 fichas (o balde)
        // Recarga: Ganha 2 fichas a cada 1 minuto (Refill.intervally)
        Bandwidth limit = Bandwidth.classic(20, Refill.intervally(2, Duration.ofMinutes(1)));

        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Tenta consumir 1 "moeda" do balde
        if (bucket.tryConsume(1)) {
            // Se tiver moeda, deixa a requisição passar para o Controller
            chain.doFilter(request, response);
        } else {
            // Se não tiver, barra com o erro 429 (Too Many Requests)
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setContentType("text/plain");
            httpResponse.setStatus(429);
            httpResponse.getWriter().append("Too many requests! Wait a minute to try again.");
        }
    }
}