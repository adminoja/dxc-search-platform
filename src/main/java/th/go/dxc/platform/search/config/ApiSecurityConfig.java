// src/main/java/th/go/dxc/platform/search/config/ApiSecurityConfig.java
package th.go.dxc.platform.search.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.server.SecurityWebFilterChain;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.adapter.in.security.mapper.UserContextMapper;
import th.go.dxc.platform.search.application.common.security.HmacScopeHasher;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class ApiSecurityConfig {

  @Bean
  ScopeHasher scopeHasher(SecurityProperties props) {
    return new HmacScopeHasher(props.hmacSecret(), props.scopeVersion());
  }

  @Bean
  SecurityWebFilterChain springSecurity(
      ServerHttpSecurity http,
      Converter<Jwt, Mono<AbstractAuthenticationToken>> userContextJwtConverter
  ) {
    return http
        // -----------------------------------------------------------------------------------------
        // ❗ Your CSRF setting — this is CORRECT for a stateless OAuth2 Resource Server.
        // Snyk will flag it, but it is safe because no cookies or sessions are used.
        // We’ll document this in security justification.
        // CSRF is intentionally disabled for DXCSP because the service is a 
        // stateless OAuth2 Resource Server, using only Bearer tokens in the Authorization header. 
        // No cookie-based authentication or HTTP sessions are used. 
        // Therefore CSRF attacks are not applicable.
        // -----------------------------------------------------------------------------------------
        .csrf(ServerHttpSecurity.CsrfSpec::disable)

        // -----------------------------------------------------------------------------------------
        // 🔐 RECOMMENDED UPDATE: explicitly disable other auth mechanisms.
        // Helps scanners see that only Bearer tokens are accepted.
        // -----------------------------------------------------------------------------------------
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)

        .authorizeExchange(ex -> ex
            // CORS preflight
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // Static / error
            .pathMatchers(
                "/",
                "/index.html",
                "/favicon.ico",
                "/error"
            ).permitAll()

            // Swagger/OpenAPI
            .pathMatchers(
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**"
            ).permitAll()

            // Actuator health/info
            .pathMatchers(
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info"
            ).permitAll()

            // everything else must have Authorization: Bearer <token>
            .anyExchange().authenticated()
        )


        // .authorizeExchange(ex -> ex
        //     // ===== KEEP YOUR EXISTING MATCHERS HERE (add/adjust as needed) =====
        //     .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        //     .pathMatchers(
        //         "/",
        //         "/index.html",
        //         "/favicon.ico",
        //         "/error"
        //     ).permitAll()
        //     // Swagger/OpenAPI
        //     .pathMatchers(
        //         "/v3/api-docs/**",
        //         "/swagger-ui.html",
        //         "/swagger-ui/**"
        //     ).permitAll()
        //     // Actuator health probes (tighten if you expose more)
        //     .pathMatchers(
        //         "/actuator/health",
        //         "/actuator/health/**",
        //         "/actuator/info"
        //     ).permitAll()
        //     // Example: allow a dev-only debug endpoint (remove on prod)
        //     // .pathMatchers("/api/debug/**").hasRole("DEV")
        //     // ==================================================================
        //     .anyExchange().authenticated()
        // )
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(userContextJwtConverter)))
        .build();
  }

  /**
   * Convert a Jwt into an Authentication whose principal is your domain UserContext.
   * This enables `@AuthenticationPrincipal UserContext user` in controllers.
   */
  @Bean
  Converter<Jwt, Mono<AbstractAuthenticationToken>> userContextJwtConverter() {
    return jwt -> {
      UserContext uc = UserContextMapper.fromJwt(jwt); // your mapper builds the domain principal
      // map authorities from your UserContext
      Set<SimpleGrantedAuthority> authorities = uc.authorities().stream()
          .map(SimpleGrantedAuthority::new)
          .collect(Collectors.toUnmodifiableSet());
      // principal = UserContext, credentials = Jwt (handy if you need claims later)
      return Mono.just(new UsernamePasswordAuthenticationToken(uc, jwt, authorities));
    };
  }
}
