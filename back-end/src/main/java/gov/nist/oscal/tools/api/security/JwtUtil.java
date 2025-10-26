package gov.nist.oscal.tools.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final int MINIMUM_SECRET_LENGTH = 32; // 256 bits for HS256

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Validates JWT secret configuration on application startup
     * Fails fast if secret is missing or insecure in production
     */
    @PostConstruct
    public void validateSecretConfiguration() {
        if (secret == null || secret.trim().isEmpty()) {
            String errorMessage = "CRITICAL SECURITY ERROR: JWT secret is not configured. " +
                    "Set the JWT_SECRET environment variable before starting the application.";
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        if (secret.length() < MINIMUM_SECRET_LENGTH) {
            String errorMessage = String.format(
                    "CRITICAL SECURITY ERROR: JWT secret is too short (%d characters). " +
                    "Minimum %d characters (256 bits) required for HS256 algorithm. " +
                    "Generate a secure secret with: openssl rand -base64 64",
                    secret.length(), MINIMUM_SECRET_LENGTH
            );
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // Warn if using development secret in non-dev environments
        if (secret.contains("development") || secret.contains("dev-secret")) {
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                String errorMessage = "CRITICAL SECURITY ERROR: Development JWT secret detected in PRODUCTION environment. " +
                        "Never use development secrets in production. Generate a new secret immediately.";
                logger.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            } else if ("staging".equalsIgnoreCase(activeProfile)) {
                logger.warn("WARNING: Development JWT secret detected in STAGING environment. " +
                        "Use a unique secret for each environment.");
            } else {
                logger.info("Using development JWT secret (acceptable for dev environment)");
            }
        } else {
            logger.info("JWT secret configured successfully (length: {} characters)", secret.length());
        }

        logger.info("JWT token expiration set to {} ms ({} hours)",
                expiration, expiration / 1000 / 60 / 60);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Generate a service account token with custom expiration and token name
     * @param username The username for the token
     * @param tokenName The name/description of the service account token
     * @param expirationDays Number of days until the token expires
     * @return JWT token string
     */
    public String generateServiceAccountToken(String username, String tokenName, int expirationDays) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenName", tokenName);
        claims.put("tokenType", "service-account");

        Date now = new Date();
        // Convert days to milliseconds: days * 24 * 60 * 60 * 1000
        long expirationMillis = (long) expirationDays * 24 * 60 * 60 * 1000;
        Date expirationDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
