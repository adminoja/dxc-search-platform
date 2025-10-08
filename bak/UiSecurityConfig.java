// src/main/java/th/go/dxc/platform/search/config/UiSecurityConfig.java
package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
@Order(2) // runs after the API chain
public class UiSecurityConfig {

  private final SecurityProperties props;

  public UiSecurityConfig(SecurityProperties props) {
    this.props = props;
  }

  @Bean
  public SecurityWebFilterChain uiChain(ServerHttpSecurity http,
                                        ReactiveClientRegistrationRepository clients) {

    // Swagger UI and OpenAPI JSON
    String[] swagger = {
        "/swagger-ui.html", "/swagger-ui/**",
        "/v3/api-docs", "/v3/api-docs/**"
    };
    // OAuth2 init + callback + default login/error endpoints
    String[] oauth = {
        "/oauth2/**",
        "/login/oauth2/**",
        "/login", "/login/**",
        "/error"
    };
    String[] chainPaths = Stream.concat(Stream.of(swagger), Stream.of(oauth)).toArray(String[]::new);

    // Match this chain only for Swagger + OAuth endpoints
    http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(chainPaths));

    // CSRF cookie, honor app.security.cookies
    var csrfRepo = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
    if (props.cookies() != null) {
      if (props.cookies().secure() != null) {
        csrfRepo.setSecure(props.cookies().secure());
      }
      if (props.cookies().sameSite() != null && !props.cookies().sameSite().isBlank()) {
        csrfRepo.setCookieCustomizer(b -> b.sameSite(props.cookies().sameSite()));
      }
    }

    // Build a list of paths to SKIP CSRF (swagger + oauth + configured ignores)
    String[] extraIgnores = normalizePrefixes(props.csrfIgnorePaths());
    String[] skipCsrf = Stream.concat(Stream.of(chainPaths), Stream.of(extraIgnores)).toArray(String[]::new);

    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf
          .csrfTokenRepository(csrfRepo)
          .requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(
              ServerWebExchangeMatchers.pathMatchers(skipCsrf)
          ))
      )
      .authorizeExchange(ex -> {
        // OAuth endpoints must be reachable to start/finish the flow
        ex.pathMatchers(oauth).permitAll();

        // Gate Swagger: by roles if configured, otherwise require login
        var sw = props.swagger();
        boolean swaggerEnabled = (sw == null || sw.enabled() == null) ? true : sw.enabled();
        if (swaggerEnabled) {
          if (sw != null && sw.roles() != null && !sw.roles().isEmpty()) {
            var required = sw.roles().stream()
                .filter(Objects::nonNull)
                .map(r -> "ROLE_" + r)
                .toArray(String[]::new);
            ex.pathMatchers(swagger).hasAnyAuthority(required);
          } else {
            ex.pathMatchers(swagger).authenticated();
          }
        } else {
          ex.pathMatchers(swagger).denyAll();
        }

        // Within this chain, everything else matched here should be authenticated
        ex.anyExchange().authenticated();
      })
      .oauth2Login(Customizer.withDefaults())
      .oauth2Client(Customizer.withDefaults())
      .logout(l -> l.logoutSuccessHandler(oidcLogout(clients)));

    return http.build();
  }

  private OidcClientInitiatedServerLogoutSuccessHandler oidcLogout(
      ReactiveClientRegistrationRepository clients) {
    var handler = new OidcClientInitiatedServerLogoutSuccessHandler(clients);
    handler.setPostLogoutRedirectUri("{baseUrl}/");
    return handler;
  }

  private static String[] normalizePrefixes(List<String> prefixes) {
    if (prefixes == null || prefixes.isEmpty()) return new String[0];
    return prefixes.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(p -> p.endsWith("/**") || p.endsWith("*") ? p : (p.endsWith("/") ? p + "**" : p + "/**"))
        .toArray(String[]::new);
  }
}
