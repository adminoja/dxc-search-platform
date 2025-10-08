package th.go.dxc.platform.search.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import lombok.extern.slf4j.Slf4j;

// ResourceServerJwtConfig.java
@Slf4j
@Configuration
public class ResourceServerJwtConfig {
  @Bean
  ReactiveJwtDecoder reactiveJwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwk) {
    log.info("Using JWKS: {}", jwk);
    NimbusReactiveJwtDecoder dec = NimbusReactiveJwtDecoder.withJwkSetUri(jwk).build();
    dec.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefault(),
        new JwtTimestampValidator(Duration.ofMinutes(5))));
    return dec;
  }
}
