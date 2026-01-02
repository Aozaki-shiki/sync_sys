package com.sss.sync.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  public static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
      .info(new Info()
        .title("sss-sync-system API")
        .version("v1"))
      // Tell Swagger UI to send Authorization: Bearer <token>
      .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
      .schemaRequirement(BEARER_AUTH, new SecurityScheme()
        .name(BEARER_AUTH)
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT"));
  }
}