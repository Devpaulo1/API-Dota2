package senac.tsi.dota2.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(2, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    // --- NOVO MÉTODO: Pega o IP real por trás do Load Balancer da Nuvem ---
    private String getClientIP(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null || xForwardedForHeader.isEmpty() || "unknown".equalsIgnoreCase(xForwardedForHeader)) {
            // Se for localhost (não tem proxy), cai aqui
            return request.getRemoteAddr();
        }
        // Na nuvem, o cabeçalho pode trazer vários IPs separados por vírgula. O primeiro é sempre o do cliente.
        return xForwardedForHeader.split(",")[0].trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // --- AQUI ESTÁ A MÁGICA: Substituímos o getRemoteAddr() pelo nosso novo método ---
        String ip = getClientIP(request);

        Bucket bucket = cache.computeIfAbsent(ip, k -> createNewBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

            response.setStatus(429);
            response.setContentType("text/plain");
            response.addHeader("Retry-After", String.valueOf(waitForRefillSeconds));
            response.getWriter().write("Too many requests! Wait " + waitForRefillSeconds + " seconds to try again.");
        }
    }
}