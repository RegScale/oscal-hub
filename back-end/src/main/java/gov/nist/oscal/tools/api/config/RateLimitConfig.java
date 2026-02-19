package gov.nist.oscal.tools.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for rate limiting
 *
 * Configures rate limits for different endpoint types:
 * - Login endpoints: Prevent brute force attacks
 * - Registration endpoints: Prevent spam/abuse
 * - Global API: Prevent API abuse
 */
@Configuration
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitConfig {

    private boolean enabled = false;

    // Login rate limiting (per IP address)
    private LoginRateLimit login = new LoginRateLimit();

    // Registration rate limiting (per IP address)
    private RegistrationRateLimit registration = new RegistrationRateLimit();

    // Global API rate limiting (per authenticated user)
    private ApiRateLimit api = new ApiRateLimit();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LoginRateLimit getLogin() {
        return login;
    }

    public void setLogin(LoginRateLimit login) {
        this.login = login;
    }

    public RegistrationRateLimit getRegistration() {
        return registration;
    }

    public void setRegistration(RegistrationRateLimit registration) {
        this.registration = registration;
    }

    public ApiRateLimit getApi() {
        return api;
    }

    public void setApi(ApiRateLimit api) {
        this.api = api;
    }

    /**
     * Login rate limit configuration
     * Default: 5 attempts per minute per IP
     */
    public static class LoginRateLimit {
        private int attempts = 5;
        private int duration = 60; // seconds

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    /**
     * Registration rate limit configuration
     * Default: 50 attempts per hour per IP
     */
    public static class RegistrationRateLimit {
        private int attempts = 50;
        private int duration = 3600; // seconds (1 hour)

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    /**
     * Global API rate limit configuration
     * Default: 100 requests per minute per user
     */
    public static class ApiRateLimit {
        private int requests = 100;
        private int duration = 60; // seconds

        public int getRequests() {
            return requests;
        }

        public void setRequests(int requests) {
            this.requests = requests;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }
    }
}
