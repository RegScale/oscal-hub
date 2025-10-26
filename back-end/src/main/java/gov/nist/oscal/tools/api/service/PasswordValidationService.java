package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.config.AccountSecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Password Validation Service
 * <p>
 * Validates passwords against complexity requirements to prevent weak password attacks.
 * Based on NIST SP 800-63B and OWASP password guidelines.
 * </p>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li>Minimum and maximum length requirements</li>
 *   <li>Character complexity (uppercase, lowercase, digits, special characters)</li>
 *   <li>Common password checking (top 10,000 most common passwords)</li>
 *   <li>Username prevention in password</li>
 *   <li>Sequential character detection</li>
 *   <li>Repeated character detection</li>
 * </ul>
 *
 * <h2>Security Standards</h2>
 * <ul>
 *   <li>NIST SP 800-63B: Digital Identity Guidelines</li>
 *   <li>OWASP: Password Storage Cheat Sheet</li>
 *   <li>CWE-521: Weak Password Requirements</li>
 * </ul>
 *
 * @see AccountSecurityConfig
 */
@Service
public class PasswordValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordValidationService.class);

    private final AccountSecurityConfig config;

    // Regular expression patterns for password validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]");

    /**
     * Top 100 most common passwords (subset of the top 10,000)
     * In production, consider using a larger list or external service
     */
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>();

    static {
        // Top 100 most common passwords from various password breach databases
        String[] passwords = {
            "password", "123456", "123456789", "12345678", "12345", "1234567", "password1",
            "1234567890", "qwerty", "abc123", "111111", "123123", "admin", "letmein",
            "welcome", "monkey", "1234", "dragon", "master", "sunshine", "princess",
            "football", "qwertyuiop", "solo", "passw0rd", "starwars", "password123",
            "hello", "freedom", "whatever", "trustno1", "charlie", "shadow", "michael",
            "jennifer", "jordan", "hunter", "iloveyou", "batman", "trustno1", "thomas",
            "robert", "access", "loveme", "buster", "1qaz2wsx", "baseball", "jessica",
            "superman", "killer", "hockey", "george", "computer", "michelle", "secret",
            "summer", "test", "qazwsx", "zxcvbnm", "fuckyou", "asdfgh", "joshua",
            "andrew", "bailey", "passw0rd", "shadow", "123321", "654321", "superman",
            "qazwsx", "michael", "football", "123qwe", "password1", "666666", "987654321",
            "123", "qwe123", "1q2w3e4r", "7777777", "1q2w3e", "654321", "555555",
            "3rjs1la7qe", "google", "1q2w3e4r5t", "123qwe", "zxcvbnm", "1q2w3e",
            "default", "password!", "P@ssw0rd", "Password1", "Password123"
        };

        for (String password : passwords) {
            COMMON_PASSWORDS.add(password.toLowerCase());
        }
    }

    @Autowired
    public PasswordValidationService(AccountSecurityConfig config) {
        this.config = config;
    }

    /**
     * Validate password against all configured rules
     *
     * @param password Password to validate
     * @param username Username (to check if password contains username)
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    public void validatePassword(String password, String username) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        List<String> errors = new ArrayList<>();

        // Length validation
        if (password.length() < config.getPasswordMinLength()) {
            errors.add("Password must be at least " + config.getPasswordMinLength() + " characters long");
        }

        if (password.length() > config.getPasswordMaxLength()) {
            errors.add("Password must not exceed " + config.getPasswordMaxLength() + " characters");
        }

        // Character complexity validation
        if (config.isPasswordRequireUppercase() && !UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (config.isPasswordRequireLowercase() && !LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (config.isPasswordRequireDigit() && !DIGIT_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }

        if (config.isPasswordRequireSpecial() && !SPECIAL_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        // Common password check
        if (config.isPasswordCheckCommonPasswords() && isCommonPassword(password)) {
            errors.add("Password is too common. Please choose a more unique password");
        }

        // Username in password check
        if (config.isPasswordPreventUsernameInPassword() && username != null && !username.isEmpty()) {
            if (containsUsername(password, username)) {
                errors.add("Password must not contain your username");
            }
        }

        // Sequential character check
        if (containsSequentialCharacters(password)) {
            errors.add("Password should not contain sequential characters (e.g., abc, 123)");
        }

        // Repeated character check
        if (containsExcessiveRepeatedCharacters(password)) {
            errors.add("Password should not contain excessive repeated characters");
        }

        // If there are any errors, throw exception with all validation messages
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            logger.warn("Password validation failed for user {}: {}", username, errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        logger.debug("Password validation successful for user {}", username);
    }

    /**
     * Check if password is in the common passwords list
     *
     * @param password Password to check
     * @return true if password is common
     */
    private boolean isCommonPassword(String password) {
        // Check exact match (case-insensitive)
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            return true;
        }

        // Check with common substitutions (leet speak)
        String normalized = password.toLowerCase()
            .replace("@", "a")
            .replace("3", "e")
            .replace("1", "i")
            .replace("!", "i")
            .replace("0", "o")
            .replace("$", "s")
            .replace("7", "t");

        return COMMON_PASSWORDS.contains(normalized);
    }

    /**
     * Check if password contains username
     *
     * @param password Password to check
     * @param username Username to look for
     * @return true if password contains username
     */
    private boolean containsUsername(String password, String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        String passwordLower = password.toLowerCase();
        String usernameLower = username.toLowerCase();

        // Check if username appears in password
        return passwordLower.contains(usernameLower);
    }

    /**
     * Check if password contains sequential characters
     * Detects sequences like: abc, 123, xyz, etc.
     *
     * @param password Password to check
     * @return true if sequential characters found
     */
    private boolean containsSequentialCharacters(String password) {
        // Check for at least 3 sequential characters
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            // Check if characters are sequential (ascending)
            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }

            // Check if characters are sequential (descending)
            if (c2 == c1 - 1 && c3 == c2 - 1) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if password contains excessive repeated characters
     * Detects patterns like: aaa, 111, etc.
     *
     * @param password Password to check
     * @return true if excessive repeated characters found
     */
    private boolean containsExcessiveRepeatedCharacters(String password) {
        // Check for at least 3 repeated characters
        for (int i = 0; i < password.length() - 2; i++) {
            char c = password.charAt(i);

            // Check if next 2 characters are the same
            if (password.charAt(i + 1) == c && password.charAt(i + 2) == c) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate password requirements message for user display
     *
     * @return Human-readable password requirements
     */
    public String getPasswordRequirements() {
        List<String> requirements = new ArrayList<>();

        requirements.add("Password must be between " + config.getPasswordMinLength() +
                        " and " + config.getPasswordMaxLength() + " characters");

        if (config.isPasswordRequireUppercase()) {
            requirements.add("At least one uppercase letter");
        }

        if (config.isPasswordRequireLowercase()) {
            requirements.add("At least one lowercase letter");
        }

        if (config.isPasswordRequireDigit()) {
            requirements.add("At least one digit");
        }

        if (config.isPasswordRequireSpecial()) {
            requirements.add("At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        if (config.isPasswordCheckCommonPasswords()) {
            requirements.add("Cannot be a common password");
        }

        if (config.isPasswordPreventUsernameInPassword()) {
            requirements.add("Cannot contain your username");
        }

        requirements.add("No sequential characters (e.g., abc, 123)");
        requirements.add("No excessive repeated characters (e.g., aaa, 111)");

        return String.join("; ", requirements);
    }

    /**
     * Calculate password strength score (0-100)
     *
     * @param password Password to analyze
     * @return Strength score (0=very weak, 100=very strong)
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length contribution (max 40 points)
        score += Math.min(password.length() * 2, 40);

        // Character diversity (max 40 points)
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 10;
        if (DIGIT_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_PATTERN.matcher(password).find()) score += 10;

        // Penalty for common patterns (max -20 points)
        if (isCommonPassword(password)) score -= 20;
        if (containsSequentialCharacters(password)) score -= 10;
        if (containsExcessiveRepeatedCharacters(password)) score -= 10;

        // Additional entropy bonus (max 20 points)
        Set<Character> uniqueChars = new HashSet<>();
        for (char c : password.toCharArray()) {
            uniqueChars.add(c);
        }
        score += Math.min(uniqueChars.size(), 20);

        // Ensure score is within 0-100 range
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Get password strength label based on score
     *
     * @param score Password strength score (0-100)
     * @return Human-readable strength label
     */
    public String getPasswordStrengthLabel(int score) {
        if (score < 20) return "Very Weak";
        if (score < 40) return "Weak";
        if (score < 60) return "Fair";
        if (score < 80) return "Strong";
        return "Very Strong";
    }
}
