package th.go.dxc.platform.search.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfig {
  // @Bean
  // @LoadBalanced
  // WebClient.Builder loadBalancedWebClientBuilder() {
  // return WebClient.builder();
  // }

  @Bean
  @LoadBalanced
  public WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder()
        .filter(tokenRelayFilter());
  }

  private ExchangeFilterFunction tokenRelayFilter() {
    return (request, next) ->
        ReactiveSecurityContextHolder.getContext()
            .map(sc -> sc.getAuthentication())
            .map(this::extractBearer)                 // may return null
            .flatMap(token -> {
              ClientRequest.Builder rb = ClientRequest.from(request);
              if (token != null && !token.isBlank()) {
                rb.headers(h -> h.setBearerAuth(token));
              }
              return next.exchange(rb.build());
            })
            // No security context at all → just proceed
            .switchIfEmpty(next.exchange(request));
  }

  /** Extracts bearer from various Authentication shapes you may have. */
  private String extractBearer(Authentication auth) {
    if (auth == null) return null;

    // 1) Default case: JwtAuthenticationToken
    if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getTokenValue();
    }

    // 2) Your converter: UsernamePasswordAuthenticationToken with Jwt credentials
    Object creds = auth.getCredentials();
    if (creds instanceof Jwt jwt) return jwt.getTokenValue();

    // 3) Some stacks put a String in credentials (with or without "Bearer ")
    if (creds instanceof String s) return s.startsWith("Bearer ") ? s.substring(7) : s;

    // 4) Fallback: principal might be a Jwt
    Object principal = auth.getPrincipal();
    if (principal instanceof Jwt pJwt) return pJwt.getTokenValue();

    return null;
  }
}