// src/main/java/th/go/dxc/core/search/config/security/DevPermitAllSecurityConfig.java
package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.ServerHttpSecurity;

@Configuration
@Profile("dev" // NOSONAR: for development only
    // + " | sit" // NOSONAR: for test only
    + " | !spring.profiles.active") // NOSONAR: default profile is "dev"
@EnableWebFluxSecurity
public class DevPermitAllSecurityConfig {

  @Bean
  SecurityWebFilterChain devWebFilterChain(ServerHttpSecurity http) {
    return http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
      .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
      .logout(ServerHttpSecurity.LogoutSpec::disable)
      .authorizeExchange(ex -> ex.anyExchange().permitAll())
      .build();
  }
}
