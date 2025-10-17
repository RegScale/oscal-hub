package gov.nist.oscal.tools.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI oscalCliOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OSCAL CLI API")
                        .description("REST API for OSCAL (Open Security Controls Assessment Language) operations including validation, format conversion, and profile resolution")
                        .version("1.0.0")
                        .license(new License()
                                .name("Dual License: Public Domain (NIST) / Commercial (RegScale)")
                                .url("https://github.com/usnistgov/oscal-cli/blob/main/front-end/LICENSE.md")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ));
    }
}
