package gov.nist.oscal.tools.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Swagger/OpenAPI configuration with environment-based security
 *
 * Swagger UI provides interactive API documentation but should be:
 * - Freely accessible in development (for ease of testing)
 * - Require authentication in staging (for controlled access)
 * - Disabled or heavily restricted in production (to prevent API enumeration)
 *
 * This configuration:
 * - Adds JWT authentication to Swagger UI
 * - Provides security warnings for production
 * - Documents all API endpoints with examples
 */
@Configuration
public class SwaggerConfig {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerConfig.class);

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerEnabled;

    @Value("${spring.application.name:oscal-cli-api}")
    private String applicationName;

    @PostConstruct
    public void validateSwaggerConfiguration() {
        if (swaggerEnabled) {
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                logger.warn("WARNING: Swagger UI is ENABLED in PRODUCTION environment.");
                logger.warn("This exposes your API structure and endpoints to potential attackers.");
                logger.warn("Recommendation: Set springdoc.swagger-ui.enabled=false in production");
                logger.warn("If Swagger is required in production, ensure it requires authentication");
                logger.warn("Swagger UI available at: /swagger-ui.html");
            } else if ("staging".equalsIgnoreCase(activeProfile)) {
                logger.info("Swagger UI enabled in STAGING environment");
                logger.info("Swagger UI available at: /swagger-ui.html (authentication recommended)");
            } else {
                logger.info("Swagger UI enabled for development at: /swagger-ui.html");
            }
        } else {
            logger.info("Swagger UI disabled");
        }
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OSCAL CLI API")
                        .version("1.0.0")
                        .description("""
                                REST API for OSCAL (Open Security Controls Assessment Language) operations.

                                This API provides endpoints for:
                                - Validating OSCAL documents (catalogs, profiles, SSPs, etc.)
                                - Converting between formats (JSON, XML, YAML)
                                - Resolving profile imports
                                - Managing component definitions and libraries
                                - Visualizing OSCAL content

                                ## Authentication
                                Most endpoints require JWT authentication. To authenticate:
                                1. Register a user account via POST /api/auth/register
                                2. Login via POST /api/auth/login to receive a JWT token
                                3. Click the 'Authorize' button below
                                4. Enter your token in the format: Bearer <your-token>
                                5. Click 'Authorize' to apply the token to all requests

                                ## Rate Limiting
                                API requests are rate limited to prevent abuse:
                                - Login: 5 attempts per minute per IP
                                - Registration: 3 attempts per hour per IP
                                - API: 100 requests per minute per authenticated user

                                Rate limit headers are included in responses:
                                - X-RateLimit-Limit: Total requests allowed
                                - X-RateLimit-Remaining: Requests remaining
                                - X-RateLimit-Reset: Time when limit resets

                                ## Security Headers
                                All responses include security headers:
                                - Strict-Transport-Security (HSTS)
                                - Content-Security-Policy (CSP)
                                - X-Frame-Options
                                - X-Content-Type-Options
                                - And more...
                                """)
                        .contact(new Contact()
                                .name("NIST OSCAL Team")
                                .url("https://pages.nist.gov/OSCAL/")
                                .email("oscal@nist.gov"))
                        .license(new License()
                                .name("NIST Public Domain")
                                .url("https://www.nist.gov/open/copyright-fair-use-and-licensing-statements-srd-data-software-and-technical-series-publications")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://localhost:8443")
                                .description("Local HTTPS server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT authentication. Format: Bearer <your-token>")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
