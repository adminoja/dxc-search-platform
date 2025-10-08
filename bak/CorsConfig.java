package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class CorsConfig {

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties p) {
    var cfg = new CorsConfiguration();

    if (p.allowedOriginPatterns() != null && !p.allowedOriginPatterns().isEmpty()) {
      cfg.setAllowedOriginPatterns(p.allowedOriginPatterns());
    } else if (p.allowedOrigins() != null && !p.allowedOrigins().isEmpty()) {
      cfg.setAllowedOrigins(p.allowedOrigins());
    }

    cfg.setAllowedMethods(
        p.allowedMethods() == null || p.allowedMethods().isEmpty()
            ? List.of("GET", "POST")
            : p.allowedMethods());

    cfg.setAllowedHeaders(
        p.allowedHeaders() == null || p.allowedHeaders().isEmpty()
            ? List.of("Authorization", "Content-Type", "X-XSRF-TOKEN")
            : p.allowedHeaders());

    if (p.exposedHeaders() != null) cfg.setExposedHeaders(p.exposedHeaders());
    cfg.setAllowCredentials(Boolean.TRUE.equals(p.allowCredentials()));
    cfg.setMaxAge(p.maxAge() == null ? Duration.ofMinutes(10) : p.maxAge());

    var source = new UrlBasedCorsConfigurationSource();
    var paths = (p.paths() == null || p.paths().isEmpty()) ? List.of("/api/**") : p.paths();
    paths.forEach(path -> source.registerCorsConfiguration(path, cfg));
    return source;
  }
}
