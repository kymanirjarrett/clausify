package com.clausify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation served by springdoc: Swagger UI at /swagger-ui.html, the OpenAPI spec at /v3/api-docs.
 * Every endpoint requires the bearer JWT unless it opts out (register, login), so Swagger UI's
 * Authorize button attaches the token to all protected calls.
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI clausifyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clausify API")
                        .version("v1")
                        .description("""
                                AI contract analysis for freelancers (IT4045C group project).

                                To call protected endpoints: register or log in, copy the `token` from the response, \
                                click **Authorize**, and paste it."""))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from POST /api/auth/register or /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
