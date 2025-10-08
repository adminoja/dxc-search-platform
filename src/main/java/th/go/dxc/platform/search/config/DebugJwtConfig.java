package th.go.dxc.platform.search.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

import com.nimbusds.jose.util.JSONObjectUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Profile("sit") // enable only when needed: SPRING_PROFILES_ACTIVE=sit,jwtpeek
public class DebugJwtConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // run before Spring Security
    public WebFilter jwtPeek() {
        log.debug("jwtPeek");
        return (exchange, chain) -> {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                log.debug("Bearer detected");
                String t = auth.substring(7).trim();
                String[] p = t.split("\\.");
                if (p.length != 3) {
                    log.debug("Token parts != 3 (got {})", p.length);
                } else {
                    try {
                        log.debug("p.length = 3");
                        // base64url → base64 padding
                        String payload = p[1] + "=".repeat((4 - p[1].length() % 4) % 4);
                        byte[] pl = Base64.getUrlDecoder().decode(payload);
                        char first = (char) pl[0];
                        log.debug("JWT ok: parts=3, payloadLen={}, firstChar='{}' (expect '{{')", pl.length, first);

                        try {
                            String json = new String(pl, StandardCharsets.UTF_8);
                            log.debug("JSON={}",json);
                            JSONObjectUtils.parse(json); // if this passes, payload JSON is valid
                            log.debug("Nimbus JSON parse: OK");
                        } catch (Exception e) {
                            log.debug("Nimbus JSON parse: FAIL -> {}", e.toString());
                        }

                    } catch (Exception e) {
                        log.debug("JWT payload decode failed: {}", e.toString());
                    }
                }
            } else {
                log.debug("bearer not detected");
            }
            return chain.filter(exchange);
        };
    }
}
