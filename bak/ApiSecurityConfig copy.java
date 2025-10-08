package th.go.dxc.platform.search.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class ApiSecurityConfig {

  private static final String[] SWAGGER = {
      "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
  };

  private static final String[] PUBLIC = {
      "/actuator/health", "/actuator/info"
  };

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                   CorsConfigurationSource corsSource) {
    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(c -> c.configurationSource(corsSource))
        .authorizeExchange(auth -> auth
            .pathMatchers(PUBLIC).permitAll()
            .pathMatchers(SWAGGER).permitAll()
            .pathMatchers(HttpMethod.OPTIONS).permitAll()
            .anyExchange().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
        );

    return http.build();
  }

  /**
   * Reactive JWT -> Authentication converter expected by WebFlux:
   * Converter<Jwt, Mono<AbstractAuthenticationToken>>
   */
  private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthConverter() {
    // Delegate that knows how to build a JwtAuthenticationToken
    JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    // Add authorities from OAuth scopes (supports "scope" or "scp" by default)
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    // scopes.setAuthorityPrefix("SCOPE_"); // default already "SCOPE_"
    // scopes.setAuthoritiesClaimName("scope"); // default handles scope/scp

    // Merge scopes + Keycloak roles into one concrete Collection<GrantedAuthority>
    delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
      Collection<GrantedAuthority> out = new ArrayList<>(scopes.convert(jwt));
      out.addAll(extractKeycloakRoleAuthorities(jwt));
      return out; // IMPORTANT: exact type Collection<GrantedAuthority>
    });

    // Adapt servlet-style converter to reactive type
    return new ReactiveJwtAuthenticationConverterAdapter(delegate);
  }

  /**
   * Extract ROLE_* from Keycloak/Red Hat SSO:
   * - realm_access.roles[]
   * - resource_access.{client}.roles[]
   */
  // @SuppressWarnings("unchecked")
  private Collection<GrantedAuthority> extractKeycloakRoleAuthorities(Jwt jwt) {
    List<GrantedAuthority> out = new ArrayList<>();

    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof List<?> rs) {
      rs.stream().map(Object::toString)
        .forEach(r -> out.add(new SimpleGrantedAuthority("ROLE_" + r)));
    }

    Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
    if (resourceAccess instanceof Map<?, ?> ra) {
      for (Map.Entry<?, ?> e : ((Map<?, ?>) ra).entrySet()) {
        Object val = e.getValue();
        if (val instanceof Map<?, ?> m && m.get("roles") instanceof List<?> rs) {
          rs.stream().map(Object::toString)
            .forEach(r -> out.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
      }
    }
    return out;
    }
}
