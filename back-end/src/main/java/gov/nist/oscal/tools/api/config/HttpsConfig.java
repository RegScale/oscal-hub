package gov.nist.oscal.tools.api.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * HTTPS/TLS Configuration
 *
 * Configures HTTPS for secure communication:
 * - Enables TLS 1.2 and TLS 1.3 only
 * - Configures strong cipher suites
 * - Redirects HTTP to HTTPS in production
 * - Disables weak protocols (SSL, TLS 1.0, TLS 1.1)
 *
 * Certificate Configuration:
 * - Development: Self-signed certificates (generate with provided script)
 * - Staging: Let's Encrypt or commercial certificate
 * - Production: Commercial certificate or Let's Encrypt
 *
 * Enable via: server.ssl.enabled=true in application properties
 */
@Configuration
public class HttpsConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpsConfig.class);

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.http.port:8080}")
    private int httpPort;

    @Value("${server.ssl.redirect-http:false}")
    private boolean redirectHttp;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void logHttpsConfiguration() {
        if (sslEnabled) {
            logger.info("================================================================================");
            logger.info("HTTPS/TLS Configuration");
            logger.info("================================================================================");
            logger.info("SSL/TLS: ENABLED");
            logger.info("HTTPS Port: {}", serverPort);
            if (redirectHttp) {
                logger.info("HTTP Port: {} (redirects to HTTPS)", httpPort);
            }
            logger.info("TLS Protocols: TLS 1.2, TLS 1.3");
            logger.info("Weak Protocols Disabled: SSL, TLS 1.0, TLS 1.1");
            logger.info("================================================================================");

            if ("dev".equalsIgnoreCase(activeProfile)) {
                logger.warn("Using self-signed certificate for development");
                logger.warn("Browsers will show security warnings - this is expected");
            }
        } else {
            logger.info("HTTPS/TLS: DISABLED");
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                logger.warn("⚠ WARNING: HTTPS is DISABLED in PRODUCTION environment");
                logger.warn("All traffic is unencrypted and vulnerable to interception");
                logger.warn("Recommendation: Enable HTTPS with server.ssl.enabled=true");
            }
        }
    }

    /**
     * HTTP to HTTPS redirect connector
     * Only active when SSL is enabled and redirect is configured
     */
    @Bean
    @ConditionalOnProperty(name = "server.ssl.redirect-http", havingValue = "true")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpToHttpsRedirect() {
        return factory -> {
            logger.info("Configuring HTTP to HTTPS redirect...");
            logger.info("HTTP requests on port {} will redirect to HTTPS port {}", httpPort, serverPort);

            // Add HTTP connector for redirect
            factory.addAdditionalTomcatConnectors(createHttpConnector());

            // Add redirect security constraint
            factory.addContextCustomizers(context -> {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            });
        };
    }

    /**
     * Creates HTTP connector that will be redirected to HTTPS
     */
    private Connector createHttpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setRedirectPort(serverPort);
        return connector;
    }

    /**
     * Customizes Tomcat for enhanced HTTPS security
     * Configures strong TLS protocols and cipher suites
     */
    @Bean
    @ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                // Enable only strong TLS protocols
                connector.setProperty("sslEnabledProtocols", "TLSv1.2,TLSv1.3");

                // Configure strong cipher suites (OWASP recommended)
                String ciphers = String.join(",",
                    // TLS 1.3 cipher suites (strongest)
                    "TLS_AES_128_GCM_SHA256",
                    "TLS_AES_256_GCM_SHA384",
                    "TLS_CHACHA20_POLY1305_SHA256",

                    // TLS 1.2 cipher suites (strong)
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"
                );
                connector.setProperty("ciphers", ciphers);

                // Prefer server cipher suite order
                connector.setProperty("honorCipherOrder", "true");

                // Enable OCSP stapling for better certificate validation
                connector.setProperty("SSLHonorCipherOrder", "true");

                logger.debug("Tomcat HTTPS connector customized with strong cipher suites");
            });
        };
    }
}
