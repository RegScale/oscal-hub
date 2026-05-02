package gov.nist.oscal.tools.api.test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.SpringBootMockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * Re-applies SecurityMockMvcConfigurers#springSecurity() to MockMvc and
 * enables method security for tests.
 *
 * Spring Boot 3 auto-applied {@code springSecurity()} via
 * MockMvcSecurityConfiguration. Spring Boot 4 dropped that integration when
 * the webmvc-test slice was extracted, so {@code @WithMockUser} no longer
 * populates request.getUserPrincipal() and {@code @PreAuthorize} on
 * controllers is silently ignored. This autoconfig restores both behaviors
 * for every {@code @WebMvcTest} in this module.
 *
 * Honors {@code @AutoConfigureMockMvc(addFilters = false)} by skipping the
 * springSecurity() filter chain when filters are disabled, so tests that
 * deliberately bypass security still work.
 */
@AutoConfiguration
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnClass(SecurityMockMvcConfigurers.class)
public class MockMvcSecurityTestAutoConfiguration {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public MockMvcBuilderCustomizer springSecurityMockMvcBuilderCustomizer(
            ObjectProvider<SpringBootMockMvcBuilderCustomizer> bootCustomizer) {
        return builder -> {
            // @SpringBootTest (no @AutoConfigureMockMvc) loads this autoconfig
            // but does not provide a SpringBootMockMvcBuilderCustomizer bean,
            // so guard against its absence.
            SpringBootMockMvcBuilderCustomizer customizer = bootCustomizer.getIfAvailable();
            if (customizer != null && !customizer.isAddFilters()) {
                return;
            }
            if (builder instanceof ConfigurableMockMvcBuilder<?> configurable) {
                configurable.apply(SecurityMockMvcConfigurers.springSecurity());
            }
        };
    }
}
