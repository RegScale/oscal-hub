package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.util.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate password complexity
        PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validate(request.getPassword());
        if (!passwordValidation.isValid()) {
            throw new RuntimeException(passwordValidation.getErrorMessage());
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        // Save user
        user = userRepository.save(user);

        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getId());
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Load user details
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Update last login timestamp
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate token
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getId());
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String generateToken(UserDetails userDetails) {
        return jwtUtil.generateToken(userDetails);
    }

    @Transactional
    public User updateProfile(String username, java.util.Map<String, String> updates) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update email if provided
        if (updates.containsKey("email") && updates.get("email") != null && !updates.get("email").isEmpty()) {
            String newEmail = updates.get("email");
            // Check if email is already taken by another user
            userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("Email already in use");
                }
            });
            user.setEmail(newEmail);
        }

        // Update password if provided
        if (updates.containsKey("password") && updates.get("password") != null && !updates.get("password").isEmpty()) {
            String newPassword = updates.get("password");
            // Validate password complexity
            PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validate(newPassword);
            if (!passwordValidation.isValid()) {
                throw new RuntimeException(passwordValidation.getErrorMessage());
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // Update profile metadata fields if provided
        if (updates.containsKey("street")) {
            user.setStreet(updates.get("street"));
        }
        if (updates.containsKey("city")) {
            user.setCity(updates.get("city"));
        }
        if (updates.containsKey("state")) {
            user.setState(updates.get("state"));
        }
        if (updates.containsKey("zip")) {
            user.setZip(updates.get("zip"));
        }
        if (updates.containsKey("title")) {
            user.setTitle(updates.get("title"));
        }
        if (updates.containsKey("organization")) {
            user.setOrganization(updates.get("organization"));
        }
        if (updates.containsKey("phoneNumber")) {
            user.setPhoneNumber(updates.get("phoneNumber"));
        }

        return userRepository.save(user);
    }

    /**
     * Generate a service account token for the current user
     * @param username The username to generate the token for
     * @param tokenName The name/description for the service account token
     * @param expirationDays Number of days until the token expires
     * @return Date when the token expires
     */
    public java.util.Date generateServiceAccountToken(String username, String tokenName, int expirationDays) {
        // Validate user exists
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Calculate expiration date
        java.util.Date now = new java.util.Date();
        long expirationMillis = (long) expirationDays * 24 * 60 * 60 * 1000;
        java.util.Date expirationDate = new java.util.Date(now.getTime() + expirationMillis);

        return expirationDate;
    }
}
