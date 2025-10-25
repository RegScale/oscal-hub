package gov.nist.oscal.tools.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.model.CertificateInfo;
import gov.nist.oscal.tools.api.model.CertificateValidationResult;
import gov.nist.oscal.tools.api.model.SignatureResult;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for handling CAC/PIV digital signatures on authorizations
 */
@Service
public class DigitalSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(DigitalSignatureService.class);

    @Autowired
    private AuthorizationRepository authorizationRepository;

    /**
     * Sign authorization with client certificate
     *
     * @param authorizationId ID of authorization to sign
     * @param certificate     X.509 certificate from TLS connection
     * @return SignatureResult with signer information
     * @throws Exception if signing fails
     */
    public SignatureResult signAuthorization(Long authorizationId, X509Certificate certificate)
            throws Exception {

        logger.info("Signing authorization {} with certificate", authorizationId);

        Authorization auth = authorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new EntityNotFoundException("Authorization not found: " + authorizationId));

        // Extract certificate information
        CertificateInfo certInfo = extractCertificateInfo(certificate);
        logger.info("Extracted certificate info for: {}", certInfo.getCommonName());

        // Generate document hash
        String documentHash = generateDocumentHash(auth);
        logger.debug("Generated document hash: {}", documentHash);

        // Store certificate and signature information
        auth.setDigitalSignatureMethod("TLS_CLIENT_CERT");
        auth.setSignerCertificate(encodeCertificate(certificate));
        auth.setSignerCommonName(certInfo.getCommonName());
        auth.setSignerEmail(certInfo.getEmail());
        auth.setSignerEdipi(certInfo.getEdipi());
        auth.setCertificateIssuer(certificate.getIssuerDN().getName());
        auth.setCertificateSerial(certificate.getSerialNumber().toString(16).toUpperCase());
        auth.setCertificateNotBefore(
                certificate.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );
        auth.setCertificateNotAfter(
                certificate.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );
        auth.setSignatureTimestamp(LocalDateTime.now());
        auth.setDocumentHash(documentHash);

        // Validate certificate
        CertificateValidationResult validation = validateCertificate(certificate);
        auth.setCertificateVerified(validation.isValid());
        auth.setCertificateVerificationDate(LocalDateTime.now());
        auth.setCertificateVerificationNotes(validation.getNotes());

        authorizationRepository.save(auth);

        logger.info("Authorization {} successfully signed by {}", authorizationId, certInfo.getCommonName());

        return SignatureResult.builder()
                .success(true)
                .signerName(certInfo.getCommonName())
                .signerEmail(certInfo.getEmail())
                .signerEdipi(certInfo.getEdipi())
                .signatureTimestamp(auth.getSignatureTimestamp())
                .message("Authorization successfully signed")
                .build();
    }

    /**
     * Generate canonical document hash for authorization
     * Uses deterministic JSON serialization to ensure consistent hashing
     *
     * @param authorization Authorization to hash
     * @return SHA-256 hash as hex string
     * @throws Exception if hashing fails
     */
    public String generateDocumentHash(Authorization authorization) throws Exception {
        // Create canonical representation of authorization
        // Only include fields that shouldn't change after signing
        Map<String, Object> canonicalData = new LinkedHashMap<>();
        canonicalData.put("id", authorization.getId());
        canonicalData.put("name", authorization.getName());
        canonicalData.put("sspItemId", authorization.getSspItemId());
        canonicalData.put("sarItemId", authorization.getSarItemId());
        canonicalData.put("completedContent", authorization.getCompletedContent());
        canonicalData.put("dateAuthorized", authorization.getDateAuthorized() != null ?
                authorization.getDateAuthorized().toString() : null);
        canonicalData.put("dateExpired", authorization.getDateExpired() != null ?
                authorization.getDateExpired().toString() : null);
        canonicalData.put("systemOwner", authorization.getSystemOwner());
        canonicalData.put("securityManager", authorization.getSecurityManager());
        canonicalData.put("authorizingOfficial", authorization.getAuthorizingOfficial());

        // Serialize to JSON with deterministic key ordering
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String canonicalJson = mapper.writeValueAsString(canonicalData);

        logger.debug("Canonical JSON length: {} bytes", canonicalJson.length());

        // Compute SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));

        // Return hex string
        return bytesToHex(hashBytes);
    }

    /**
     * Extract certificate information from X.509 certificate
     *
     * @param certificate X.509 certificate
     * @return Parsed certificate information
     */
    public CertificateInfo extractCertificateInfo(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectDN().getName();

        // Parse DN
        String cn = extractDNField(subjectDN, "CN");
        String email = extractDNField(subjectDN, "E");
        if (email == null || email.isEmpty()) {
            email = extractDNField(subjectDN, "EMAILADDRESS");
        }

        // Extract EDIPI (last 10 digits of CN for CAC/PIV)
        String edipi = null;
        if (cn != null) {
            Pattern pattern = Pattern.compile("(\\d{10})$");
            Matcher matcher = pattern.matcher(cn);
            if (matcher.find()) {
                edipi = matcher.group(1);
            }
        }

        return CertificateInfo.builder()
                .commonName(cn)
                .email(email)
                .edipi(edipi)
                .subjectDN(subjectDN)
                .issuerDN(certificate.getIssuerDN().getName())
                .serialNumber(certificate.getSerialNumber().toString(16).toUpperCase())
                .notBefore(certificate.getNotBefore())
                .notAfter(certificate.getNotAfter())
                .build();
    }

    /**
     * Validate X.509 certificate
     *
     * @param certificate Certificate to validate
     * @return Validation result with notes
     */
    public CertificateValidationResult validateCertificate(X509Certificate certificate) {
        List<String> notes = new ArrayList<>();
        boolean valid = true;

        try {
            // 1. Check validity period
            certificate.checkValidity();
            notes.add("Certificate is within validity period");

        } catch (CertificateExpiredException e) {
            valid = false;
            notes.add("ERROR: Certificate has expired");
            logger.warn("Certificate has expired: {}", certificate.getSubjectDN());
        } catch (CertificateNotYetValidException e) {
            valid = false;
            notes.add("ERROR: Certificate is not yet valid");
            logger.warn("Certificate not yet valid: {}", certificate.getSubjectDN());
        }

        // 2. Check key usage
        boolean[] keyUsage = certificate.getKeyUsage();
        if (keyUsage != null && keyUsage.length > 0) {
            // Bit 0 = digitalSignature
            if (keyUsage[0]) {
                notes.add("Certificate has digitalSignature key usage");
            } else {
                valid = false;
                notes.add("ERROR: Certificate does not have digitalSignature key usage");
                logger.warn("Certificate missing digitalSignature key usage: {}", certificate.getSubjectDN());
            }
        } else {
            notes.add("WARNING: No key usage extension found");
        }

        // 3. Check issuer (optional - recognizes common DoD/Federal PKI issuers)
        String issuer = certificate.getIssuerDN().getName();
        if (issuer.contains("DOD") || issuer.contains("DoD") ||
                issuer.contains("U.S. Government") || issuer.contains("Federal")) {
            notes.add("Certificate issued by recognized authority: " + extractDNField(issuer, "CN"));
        } else {
            notes.add("WARNING: Certificate issuer not recognized: " + extractDNField(issuer, "CN"));
        }

        // 4. Check certificate type (CAC/PIV format)
        String cn = extractDNField(certificate.getSubjectDN().getName(), "CN");
        if (cn != null && cn.matches(".*\\d{10}$")) {
            notes.add("Certificate appears to be CAC/PIV format (EDIPI detected)");
        }

        // Log validation result
        if (valid) {
            logger.info("Certificate validation passed for: {}", cn);
        } else {
            logger.warn("Certificate validation failed for: {}", cn);
        }

        return CertificateValidationResult.builder()
                .valid(valid)
                .notes(String.join("; ", notes))
                .build();
    }

    /**
     * Verify existing certificate (for re-verification)
     *
     * @param certificatePem Base64-encoded certificate
     * @return Validation result
     * @throws Exception if verification fails
     */
    public CertificateValidationResult verifyCertificate(String certificatePem) throws Exception {
        X509Certificate certificate = decodeCertificate(certificatePem);
        return validateCertificate(certificate);
    }

    /**
     * Encode certificate to Base64 string
     *
     * @param certificate X.509 certificate
     * @return Base64-encoded certificate
     * @throws Exception if encoding fails
     */
    private String encodeCertificate(X509Certificate certificate) throws Exception {
        byte[] encoded = certificate.getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }

    /**
     * Decode Base64 certificate string to X.509 certificate
     *
     * @param certificatePem Base64-encoded certificate
     * @return X.509 certificate
     * @throws Exception if decoding fails
     */
    private X509Certificate decodeCertificate(String certificatePem) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(certificatePem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
    }

    /**
     * Extract field from Distinguished Name
     *
     * @param dn    Distinguished Name string
     * @param field Field to extract (e.g., "CN", "E", "O")
     * @return Field value or null if not found
     */
    private String extractDNField(String dn, String field) {
        Pattern pattern = Pattern.compile(field + "=([^,]+)");
        Matcher matcher = pattern.matcher(dn);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Convert byte array to hex string
     *
     * @param bytes Byte array
     * @return Hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
