package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    // Define a simple "paste your JWT" scheme for Swagger's Authorize button
    final String SCHEME_NAME = "bearer-jwt";

    return new OpenAPI()
        .info(new Info()
            .title("DXC Search Platform API")
            .version("v1"))
        .components(new Components()
            .addSecuritySchemes(SCHEME_NAME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
        // Apply globally so endpoints show the lock icon
        .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
  }
}
