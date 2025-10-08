// src/main/java/th/go/dxc/platform/search/config/SecurityConfig.java
package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
public class SecurityConfig {

  @Bean
  ScopeHasher scopeHasher(SecurityProperties props) {
    return new HmacScopeHasher(props.hmacSecret(), props.scopeVersion());
  }

  @Bean
  SecurityWebFilterChain springSecurity(ServerHttpSecurity http,
      Converter<Jwt, Mono<AbstractAuthenticationToken>> converter) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(ex -> ex.anyExchange().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
    return http.build();
  }

  @Bean
  Converter<Jwt, Mono<AbstractAuthenticationToken>> userContextJwtConverter() {
    return jwt -> {
      UserContext uc = UserContextMapper.fromJwt(jwt); // your mapper
      var authorities = uc.authorities().stream()
          .map(SimpleGrantedAuthority::new).toList();
      var auth = new UsernamePasswordAuthenticationToken(uc, jwt, authorities);
      return Mono.just(auth);
    };
  }
}
