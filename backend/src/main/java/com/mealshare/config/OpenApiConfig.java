package com.mealshare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mealShareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MealShare API")
                        .description("MealShare Surplus-Food Redistribution Platform API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MealShare Engineering Team")))
                .addSecurityItem(new SecurityRequirement().addList("Firebase"))
                .components(new Components()
                        .addSecuritySchemes("Firebase", new SecurityScheme()
                                .name("Firebase")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Firebase ID Token")));
    }
}
