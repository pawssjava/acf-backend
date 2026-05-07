package kz.cyber.acf.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Academy of Cyber Football API")
                        .description("""
                                Backend API for the **Academy of Cyber Football** platform.

                                ## Authentication
                                Most endpoints require a **Keycloak JWT** passed as a Bearer token:
                                ```
                                Authorization: Bearer <token>
                                ```
                                Click the **Authorize** button above to enter your token.

                                ### Public endpoints (no token required)
                                - `POST /api/auth/send-sms` — request SMS code
                                - `POST /api/auth/register` — complete registration
                                - `GET /api/news` / `GET /api/news/{id}` — read news

                                ## Covers
                                - **Authentication** — SMS-based registration (code `1111` for testing)
                                - **Users** — profile management
                                - **News** — articles with images (read-only is public)
                                - **Tournaments** — full CRUD with status and type join
                                - **Participants** — register / unregister users to tournaments
                                - **Results** — record final standings per tournament
                                - **Dictionaries** — tournament types and statuses
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ACF Dev Team Telegram: @gazizdev")
                                .email("gazizbakhyt@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Bearer")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your Keycloak access token here")));
    }
}
