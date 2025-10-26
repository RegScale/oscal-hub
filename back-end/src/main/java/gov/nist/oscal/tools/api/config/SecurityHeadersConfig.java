package gov.nist.oscal.tools.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for security headers
 *
 * Configures HTTP security headers to protect against:
 * - Cross-Site Scripting (XSS)
 * - Clickjacking
 * - MIME type sniffing
 * - Man-in-the-middle attacks
 * - Referrer leakage
 */
@Configuration
@ConfigurationProperties(prefix = "security.headers")
public class SecurityHeadersConfig {

    private boolean enabled = false;
    private boolean requireHttps = false;

    // Strict-Transport-Security (HSTS)
    private HstsConfig hsts = new HstsConfig();

    // Content-Security-Policy (CSP)
    private CspConfig csp = new CspConfig();

    // X-Frame-Options
    private FrameOptionsConfig frameOptions = new FrameOptionsConfig();

    // X-Content-Type-Options
    private boolean enableContentTypeOptions = true;

    // X-XSS-Protection
    private boolean enableXssProtection = true;

    // Referrer-Policy
    private String referrerPolicy = "strict-origin-when-cross-origin";

    // Permissions-Policy
    private PermissionsPolicyConfig permissionsPolicy = new PermissionsPolicyConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    public HstsConfig getHsts() {
        return hsts;
    }

    public void setHsts(HstsConfig hsts) {
        this.hsts = hsts;
    }

    public CspConfig getCsp() {
        return csp;
    }

    public void setCsp(CspConfig csp) {
        this.csp = csp;
    }

    public FrameOptionsConfig getFrameOptions() {
        return frameOptions;
    }

    public void setFrameOptions(FrameOptionsConfig frameOptions) {
        this.frameOptions = frameOptions;
    }

    public boolean isEnableContentTypeOptions() {
        return enableContentTypeOptions;
    }

    public void setEnableContentTypeOptions(boolean enableContentTypeOptions) {
        this.enableContentTypeOptions = enableContentTypeOptions;
    }

    public boolean isEnableXssProtection() {
        return enableXssProtection;
    }

    public void setEnableXssProtection(boolean enableXssProtection) {
        this.enableXssProtection = enableXssProtection;
    }

    public String getReferrerPolicy() {
        return referrerPolicy;
    }

    public void setReferrerPolicy(String referrerPolicy) {
        this.referrerPolicy = referrerPolicy;
    }

    public PermissionsPolicyConfig getPermissionsPolicy() {
        return permissionsPolicy;
    }

    public void setPermissionsPolicy(PermissionsPolicyConfig permissionsPolicy) {
        this.permissionsPolicy = permissionsPolicy;
    }

    /**
     * HTTP Strict Transport Security (HSTS) configuration
     * Enforces HTTPS connections
     */
    public static class HstsConfig {
        private boolean enabled = true;
        private long maxAge = 31536000; // 1 year in seconds
        private boolean includeSubDomains = true;
        private boolean preload = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }

        public boolean isIncludeSubDomains() {
            return includeSubDomains;
        }

        public void setIncludeSubDomains(boolean includeSubDomains) {
            this.includeSubDomains = includeSubDomains;
        }

        public boolean isPreload() {
            return preload;
        }

        public void setPreload(boolean preload) {
            this.preload = preload;
        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            sb.append("max-age=").append(maxAge);
            if (includeSubDomains) {
                sb.append("; includeSubDomains");
            }
            if (preload) {
                sb.append("; preload");
            }
            return sb.toString();
        }
    }

    /**
     * Content Security Policy (CSP) configuration
     * Controls which resources can be loaded
     */
    public static class CspConfig {
        private boolean enabled = true;
        private boolean reportOnly = false;
        private String defaultSrc = "'self'";
        private String scriptSrc = "'self'";
        private String styleSrc = "'self' 'unsafe-inline'";
        private String imgSrc = "'self' data: https:";
        private String fontSrc = "'self' data:";
        private String connectSrc = "'self'";
        private String frameSrc = "'none'";
        private String objectSrc = "'none'";
        private String baseUri = "'self'";
        private String formAction = "'self'";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isReportOnly() {
            return reportOnly;
        }

        public void setReportOnly(boolean reportOnly) {
            this.reportOnly = reportOnly;
        }

        public String getDefaultSrc() {
            return defaultSrc;
        }

        public void setDefaultSrc(String defaultSrc) {
            this.defaultSrc = defaultSrc;
        }

        public String getScriptSrc() {
            return scriptSrc;
        }

        public void setScriptSrc(String scriptSrc) {
            this.scriptSrc = scriptSrc;
        }

        public String getStyleSrc() {
            return styleSrc;
        }

        public void setStyleSrc(String styleSrc) {
            this.styleSrc = styleSrc;
        }

        public String getImgSrc() {
            return imgSrc;
        }

        public void setImgSrc(String imgSrc) {
            this.imgSrc = imgSrc;
        }

        public String getFontSrc() {
            return fontSrc;
        }

        public void setFontSrc(String fontSrc) {
            this.fontSrc = fontSrc;
        }

        public String getConnectSrc() {
            return connectSrc;
        }

        public void setConnectSrc(String connectSrc) {
            this.connectSrc = connectSrc;
        }

        public String getFrameSrc() {
            return frameSrc;
        }

        public void setFrameSrc(String frameSrc) {
            this.frameSrc = frameSrc;
        }

        public String getObjectSrc() {
            return objectSrc;
        }

        public void setObjectSrc(String objectSrc) {
            this.objectSrc = objectSrc;
        }

        public String getBaseUri() {
            return baseUri;
        }

        public void setBaseUri(String baseUri) {
            this.baseUri = baseUri;
        }

        public String getFormAction() {
            return formAction;
        }

        public void setFormAction(String formAction) {
            this.formAction = formAction;
        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            sb.append("default-src ").append(defaultSrc).append("; ");
            sb.append("script-src ").append(scriptSrc).append("; ");
            sb.append("style-src ").append(styleSrc).append("; ");
            sb.append("img-src ").append(imgSrc).append("; ");
            sb.append("font-src ").append(fontSrc).append("; ");
            sb.append("connect-src ").append(connectSrc).append("; ");
            sb.append("frame-src ").append(frameSrc).append("; ");
            sb.append("object-src ").append(objectSrc).append("; ");
            sb.append("base-uri ").append(baseUri).append("; ");
            sb.append("form-action ").append(formAction);
            return sb.toString();
        }
    }

    /**
     * X-Frame-Options configuration
     * Prevents clickjacking attacks
     */
    public static class FrameOptionsConfig {
        private String policy = "DENY"; // DENY, SAMEORIGIN, or ALLOW-FROM

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = policy;
        }
    }

    /**
     * Permissions-Policy configuration
     * Controls browser features and APIs
     */
    public static class PermissionsPolicyConfig {
        private boolean enabled = true;
        private String geolocation = "()";
        private String microphone = "()";
        private String camera = "()";
        private String payment = "()";
        private String usb = "()";
        private String magnetometer = "()";
        private String gyroscope = "()";
        private String accelerometer = "()";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getGeolocation() {
            return geolocation;
        }

        public void setGeolocation(String geolocation) {
            this.geolocation = geolocation;
        }

        public String getMicrophone() {
            return microphone;
        }

        public void setMicrophone(String microphone) {
            this.microphone = microphone;
        }

        public String getCamera() {
            return camera;
        }

        public void setCamera(String camera) {
            this.camera = camera;
        }

        public String getPayment() {
            return payment;
        }

        public void setPayment(String payment) {
            this.payment = payment;
        }

        public String getUsb() {
            return usb;
        }

        public void setUsb(String usb) {
            this.usb = usb;
        }

        public String getMagnetometer() {
            return magnetometer;
        }

        public void setMagnetometer(String magnetometer) {
            this.magnetometer = magnetometer;
        }

        public String getGyroscope() {
            return gyroscope;
        }

        public void setGyroscope(String gyroscope) {
            this.gyroscope = gyroscope;
        }

        public String getAccelerometer() {
            return accelerometer;
        }

        public void setAccelerometer(String accelerometer) {
            this.accelerometer = accelerometer;
        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            sb.append("geolocation=").append(geolocation).append(", ");
            sb.append("microphone=").append(microphone).append(", ");
            sb.append("camera=").append(camera).append(", ");
            sb.append("payment=").append(payment).append(", ");
            sb.append("usb=").append(usb).append(", ");
            sb.append("magnetometer=").append(magnetometer).append(", ");
            sb.append("gyroscope=").append(gyroscope).append(", ");
            sb.append("accelerometer=").append(accelerometer);
            return sb.toString();
        }
    }
}
