// src/main/java/th/go/dxc/platform/search/config/ApiSecurityConfig.java
package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@Order(1) // keep API first; it's restricted to /api/**
public class ApiSecurityConfig {

  @Bean
  public SecurityWebFilterChain apiChain(ServerHttpSecurity http) {
    return http
      .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**"))
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .authorizeExchange(ex -> ex.anyExchange().authenticated())
      .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())))
      .build();
  }

  @Bean
  public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
    var delegate = new JwtAuthenticationConverter();
    delegate.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    return new ReactiveJwtAuthenticationConverterAdapter(delegate);
  }

  @SuppressWarnings("unchecked")
  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    // realm_access.roles
    var realmAccess = (Map<String, Object>) jwt.getClaim("realm_access");
    var realmRoles = realmAccess == null
        ? List.<String>of()
        : (List<String>) realmAccess.getOrDefault("roles", List.of());

    // resource_access.*.roles (all clients)
    var resourceAccess = (Map<String, Object>) jwt.getClaim("resource_access");
    var clientRoles = resourceAccess == null
        ? List.<String>of()
        : resourceAccess.values().stream()
            .map(m -> (Map<String, Object>) m)
            .flatMap(m -> ((List<String>) m.getOrDefault("roles", List.of())).stream())
            .toList();

    return Stream.concat(realmRoles.stream(), clientRoles.stream())
        .distinct()
        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
        .collect(Collectors.toList());
  }
}
