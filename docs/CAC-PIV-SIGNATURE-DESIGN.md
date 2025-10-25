# CAC/PIV Digital Signature Integration Design

**Date**: October 25, 2025
**Status**: 📋 Design Phase
**Author**: System Architect
**Version**: 2.0

## Executive Summary

This document outlines the design and implementation plan for integrating CAC/PIV (Common Access Card/Personal Identity Verification) digital signature capabilities into the OSCAL Tools authorization workflow using **browser-native certificate access** with ActivClient middleware. This approach leverages existing middleware that government users already have installed, requiring no additional software deployment.

## Table of Contents

1. [Background](#background)
2. [Current State Analysis](#current-state-analysis)
3. [Recommended Architecture](#recommended-architecture)
4. [Technical Implementation](#technical-implementation)
5. [Integration Points](#integration-points)
6. [Security Considerations](#security-considerations)
7. [Implementation Plan](#implementation-plan)
8. [User Experience Design](#user-experience-design)
9. [Testing Strategy](#testing-strategy)
10. [Compliance Requirements](#compliance-requirements)
11. [Browser Compatibility](#browser-compatibility)

---

## Background

### What are CAC/PIV Cards?

**CAC (Common Access Card)**: Smart card issued to US military personnel, DoD civilians, and contractors. Contains:
- PKI certificates for authentication and digital signatures
- Biometric data (fingerprints, photo)
- Magnetic stripe for physical access

**PIV (Personal Identity Verification)**: Smart card issued to federal employees and contractors under HSPD-12. Similar functionality to CAC.

Both cards contain:
- **Authentication Certificate**: For user authentication (login)
- **Signature Certificate**: For digital signatures (non-repudiation)
- **Encryption Certificate**: For email encryption
- Private keys stored securely on the chip (never extracted)

### ActivClient Middleware

**ActivClient** (by HID Global) is the standard DoD middleware for CAC access:
- Installed on virtually all DoD and many federal government computers
- Provides PKCS#11 interface to smart cards
- Makes certificates available to browsers and applications
- Handles PIN prompts and card operations
- Supports all major browsers (Chrome, Firefox, Edge)

**Key Point**: Users already have this installed - no additional software needed!

### Digital Signature Use Case

In the authorization workflow, the **Authorizing Official** needs to digitally sign the authorization document to:
- **Authenticate** their identity using their PIV/CAC card
- **Provide non-repudiation** - cryptographic proof they approved the authorization
- **Comply with federal regulations** (FISMA, NIST SP 800-53, DoD directives)
- **Create legally binding** electronic signatures under ESIGN Act and UETA

---

## Current State Analysis

### Authorization Workflow (8 Steps)

Based on `authorization-wizard.tsx` and `authorizations/page.tsx`:

1. **Select SSP** - Choose System Security Plan
2. **Select SAR** - Choose Security Assessment Report (optional)
3. **Stakeholder Information** - Enter dates and stakeholder names
   - Date Authorized
   - Date Expired
   - System Owner
   - Security Manager
   - **Authorizing Official** ← This person should sign
4. **Visualize** - Review SSP and SAR data
5. **Select Template** - Choose authorization template
6. **Fill Variables** - Complete template variables
7. **Conditions of Approval** - Add mandatory/recommended conditions
8. **Review** - Final review before creation

### Current Authentication

- **JWT-based authentication** (username/password)
- **Username stored** in authorization record (`authorizedBy` field)
- **No digital signature** - only username string
- **No cryptographic proof** of identity

### Database Schema

From `Authorization.java` entity:
```java
- id (Long)
- name (String)
- sspItemId (String)
- sarItemId (String)
- template (AuthorizationTemplate)
- variableValues (Map<String, String>)
- completedContent (TEXT)
- authorizedBy (User) // Currently just username
- authorizedAt (LocalDateTime)
- dateAuthorized (String)
- dateExpired (String)
- systemOwner (String)
- securityManager (String)
- authorizingOfficial (String)
- conditions (List<Condition>)
```

**Gap**: No fields for digital signature data.

---

## Recommended Architecture

### Browser-Native Certificate Access with ActivClient

**Overview**: Use browser's built-in certificate handling with ActivClient middleware that users already have installed.

**Why This Approach?**

✅ **No Additional Software** - Users already have ActivClient
✅ **Simple Deployment** - Just frontend and backend changes
✅ **Native Browser UI** - Familiar certificate selection dialog
✅ **Works Today** - Proven technology stack
✅ **Secure** - Private keys never leave the card
✅ **Cross-Browser** - Works on Chrome, Firefox, Edge

**How It Works**:

1. User completes authorization wizard
2. Frontend generates document hash
3. Frontend triggers browser certificate selection
4. ActivClient middleware provides CAC/PIV certificates
5. Browser shows native certificate selection dialog
6. User selects certificate and enters PIN (via ActivClient)
7. Browser signs the hash using the card's private key
8. Frontend sends signature + certificate to backend
9. Backend verifies signature and stores it

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Web Browser                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         Next.js Frontend (localhost:3000)             │  │
│  │                                                       │  │
│  │  Authorization Wizard → Digital Signature Step       │  │
│  │                                                       │  │
│  │  JavaScript Crypto API:                               │  │
│  │  - crypto.subtle.sign() OR                            │  │
│  │  - <keygen> element OR                                │  │
│  │  - XMLHttpRequest with client certificates            │  │
│  └───────────┬───────────────────────────────────────────┘  │
│              │                                              │
│              │ Certificate Selection Dialog                 │
│              │ (Native Browser UI)                          │
│              │                                              │
└──────────────┼──────────────────────────────────────────────┘
               │
               ▼
    ┌──────────────────────┐
    │   ActivClient        │
    │   Middleware         │
    │                      │
    │  - Enumerates certs  │
    │  - Prompts for PIN   │
    │  - Performs signing  │
    └──────────┬───────────┘
               │ PKCS#11
               ▼
    ┌──────────────────────┐
    │  Smart Card Reader   │
    │   + CAC/PIV Card     │
    └──────────────────────┘

               │
               │ HTTP POST (signature + cert)
               ▼
    ┌────────────────────────────┐
    │   Spring Boot Backend      │
    │   (localhost:8080)         │
    │                            │
    │  - Verify signature        │
    │  - Validate certificate    │
    │  - Store signature data    │
    └────────────┬───────────────┘
                 │
                 ▼
    ┌────────────────────────────┐
    │   H2 Database              │
    │                            │
    │  - Authorization record    │
    │  - Digital signature       │
    │  - Certificate             │
    │  - Timestamp               │
    └────────────────────────────┘
```

---

## Technical Implementation

### Approach 1: TLS Client Certificate Authentication ✅ **CHOSEN APPROACH**

**Best option for CAC/PIV**: Use TLS client certificate authentication during the signature submission.

**Status**: ✅ **APPROVED FOR IMPLEMENTATION**

**How it works**:
1. Browser prompts for certificate selection when accessing a protected endpoint
2. User selects CAC/PIV certificate
3. TLS handshake includes client certificate
4. Backend extracts certificate from TLS connection
5. Backend uses certificate to verify user identity

**Frontend Code**:
```javascript
// Create authorization and get document hash
const authResponse = await apiClient.createAuthorization(data);
const authorizationId = authResponse.id;
const documentHash = authResponse.documentHash;

// Sign using TLS client certificate
// The browser will prompt for certificate selection
const signatureResponse = await fetch('/api/authorizations/sign-with-tls', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    authorizationId,
    documentHash
  }),
  // Browser will prompt for client certificate
  credentials: 'include'
});

if (signatureResponse.ok) {
  // Signature completed
  const result = await signatureResponse.json();
  console.log('Signed by:', result.signerName);
}
```

**Backend Configuration (Spring Boot)**:
```java
@Configuration
public class TlsClientCertConfig {

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

        tomcat.addConnectorCustomizers(connector -> {
            // Enable client certificate authentication
            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            protocol.setSSLEnabled(true);
            protocol.setClientAuth("want"); // Request client cert but don't require
        });

        return tomcat;
    }
}

@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    @PostMapping("/sign-with-tls")
    public ResponseEntity<SignatureResult> signWithTls(
        @RequestBody SignRequest request,
        HttpServletRequest httpRequest
    ) {
        // Extract client certificate from TLS connection
        X509Certificate[] certs = (X509Certificate[])
            httpRequest.getAttribute("javax.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            return ResponseEntity.status(401).build(); // No certificate provided
        }

        X509Certificate clientCert = certs[0];

        // Verify and store signature
        signatureService.signWithCertificate(
            request.getAuthorizationId(),
            request.getDocumentHash(),
            clientCert
        );

        return ResponseEntity.ok(new SignatureResult(
            getCN(clientCert),
            LocalDateTime.now()
        ));
    }
}
```

**Pros**:
- ✅ Standard TLS mechanism
- ✅ Browser handles certificate selection
- ✅ Secure (private key never exposed)
- ✅ ActivClient handles PIN prompts

**Cons**:
- ⚠️ Requires HTTPS with client cert authentication configured
- ⚠️ More complex server configuration

---

### Approach 2: Web Crypto API with SubtleCrypto

**Modern browser API** for cryptographic operations.

**Note**: This approach has **limited smart card support** in most browsers. The Web Crypto API was designed primarily for software-based keys, not hardware tokens. However, some browsers can access smart cards if properly configured.

**Frontend Code**:
```javascript
async function signWithWebCrypto(documentHash) {
  try {
    // Request access to certificates
    // Note: This may not work with smart cards in all browsers
    const keys = await window.crypto.subtle.exportKey('pkcs8', privateKey);

    // Convert document hash to ArrayBuffer
    const hashBuffer = Uint8Array.from(atob(documentHash), c => c.charCodeAt(0));

    // Sign the hash
    const signature = await window.crypto.subtle.sign(
      {
        name: "RSASSA-PKCS1-v1_5",
      },
      privateKey,
      hashBuffer
    );

    // Convert signature to Base64
    const signatureBase64 = btoa(
      String.fromCharCode.apply(null, new Uint8Array(signature))
    );

    return signatureBase64;
  } catch (error) {
    console.error('Signing failed:', error);
    throw error;
  }
}
```

**Limitation**: Web Crypto API doesn't provide a standard way to enumerate certificates from smart cards or prompt for certificate selection. This works better with software keys or certificates already imported into the browser.

---

### Approach 3: XMLHttpRequest with Certificate Selection (Legacy but Reliable)

**Traditional approach** that works well with smart cards.

**Frontend Code**:
```javascript
function signWithXHR(authorizationId, documentHash) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    // Open connection to signing endpoint
    xhr.open('POST', '/api/authorizations/sign-with-cert', true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    // This will trigger browser's certificate selection dialog
    xhr.withCredentials = true;

    xhr.onload = function() {
      if (xhr.status === 200) {
        resolve(JSON.parse(xhr.responseText));
      } else {
        reject(new Error('Signing failed'));
      }
    };

    xhr.onerror = function() {
      reject(new Error('Network error'));
    };

    xhr.send(JSON.stringify({
      authorizationId,
      documentHash
    }));
  });
}
```

**Backend**: Same as TLS approach - extract certificate from request.

---

### Approach 4: HTML5 Keygen Element (Deprecated)

**Note**: The `<keygen>` element is **deprecated** and no longer supported in modern browsers. Not recommended.

---

### Recommended Implementation: TLS Client Certificate

**Why**: Most reliable approach that works with ActivClient and CAC/PIV cards.

**User Experience**:
1. User clicks "Sign with CAC/PIV"
2. Browser shows certificate selection dialog (native UI)
3. User selects their CAC/PIV signature certificate
4. ActivClient prompts for PIN (secure dialog)
5. User enters PIN
6. Browser completes TLS handshake with client certificate
7. Backend receives certificate and creates signature record
8. User sees "Successfully signed!" confirmation

---

## Integration Points

### 1. Database Schema Changes

**Add to `Authorization` entity**:

```java
@Entity
public class Authorization {
    // ... existing fields ...

    // Digital Signature Fields
    @Column(name = "digital_signature_method")
    private String digitalSignatureMethod; // "TLS_CLIENT_CERT" or "WEB_CRYPTO"

    @Column(name = "signer_certificate", columnDefinition = "TEXT")
    private String signerCertificate; // Base64-encoded X.509 cert

    @Column(name = "signer_common_name")
    private String signerCommonName; // CN from certificate (e.g., "DOE.JOHN.M.1234567890")

    @Column(name = "signer_email")
    private String signerEmail; // Email from certificate

    @Column(name = "signer_edipi")
    private String signerEdipi; // EDIPI from certificate (last 10 digits of CN)

    @Column(name = "certificate_issuer")
    private String certificateIssuer; // Certificate issuer DN

    @Column(name = "certificate_serial")
    private String certificateSerial; // Certificate serial number

    @Column(name = "certificate_not_before")
    private LocalDateTime certificateNotBefore;

    @Column(name = "certificate_not_after")
    private LocalDateTime certificateNotAfter;

    @Column(name = "signature_timestamp")
    private LocalDateTime signatureTimestamp;

    @Column(name = "document_hash")
    private String documentHash; // SHA-256 hash of signed content

    @Column(name = "certificate_verified")
    private Boolean certificateVerified; // Certificate validation result

    @Column(name = "certificate_verification_date")
    private LocalDateTime certificateVerificationDate;

    @Column(name = "certificate_verification_notes")
    private String certificateVerificationNotes; // Any validation warnings
}
```

**Database Migration** (Flyway):
```sql
-- V1.5__add_digital_signature.sql

ALTER TABLE authorization ADD COLUMN digital_signature_method VARCHAR(50);
ALTER TABLE authorization ADD COLUMN signer_certificate TEXT;
ALTER TABLE authorization ADD COLUMN signer_common_name VARCHAR(255);
ALTER TABLE authorization ADD COLUMN signer_email VARCHAR(255);
ALTER TABLE authorization ADD COLUMN signer_edipi VARCHAR(10);
ALTER TABLE authorization ADD COLUMN certificate_issuer VARCHAR(500);
ALTER TABLE authorization ADD COLUMN certificate_serial VARCHAR(100);
ALTER TABLE authorization ADD COLUMN certificate_not_before TIMESTAMP;
ALTER TABLE authorization ADD COLUMN certificate_not_after TIMESTAMP;
ALTER TABLE authorization ADD COLUMN signature_timestamp TIMESTAMP;
ALTER TABLE authorization ADD COLUMN document_hash VARCHAR(64);
ALTER TABLE authorization ADD COLUMN certificate_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE authorization ADD COLUMN certificate_verification_date TIMESTAMP;
ALTER TABLE authorization ADD COLUMN certificate_verification_notes TEXT;
```

### 2. Backend API Endpoints

**New endpoints in `AuthorizationController`**:

```java
@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    /**
     * Sign authorization with TLS client certificate
     * Browser will prompt for certificate selection
     */
    @PostMapping("/sign-with-cert")
    public ResponseEntity<SignatureResult> signWithClientCertificate(
        @RequestBody SignRequest request,
        HttpServletRequest httpRequest
    ) {
        // Extract client certificate from TLS connection
        X509Certificate[] certs = (X509Certificate[])
            httpRequest.getAttribute("javax.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            return ResponseEntity.status(401)
                .body(new SignatureResult(false, "No client certificate provided"));
        }

        X509Certificate clientCert = certs[0];

        try {
            // Validate certificate
            CertificateValidationResult validation =
                digitalSignatureService.validateCertificate(clientCert);

            if (!validation.isValid()) {
                return ResponseEntity.status(400)
                    .body(new SignatureResult(false,
                        "Certificate validation failed: " + validation.getError()));
            }

            // Sign the authorization
            SignatureResult result = digitalSignatureService.signAuthorization(
                request.getAuthorizationId(),
                clientCert
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Signing failed", e);
            return ResponseEntity.status(500)
                .body(new SignatureResult(false, "Signing failed: " + e.getMessage()));
        }
    }

    /**
     * Get signature details for an authorization
     */
    @GetMapping("/{id}/signature")
    public ResponseEntity<SignatureDetailsResponse> getSignatureDetails(
        @PathVariable Long id
    ) {
        Authorization auth = authorizationService.getAuthorization(id);

        if (auth.getSignerCertificate() == null) {
            return ResponseEntity.status(404)
                .body(new SignatureDetailsResponse("No signature found"));
        }

        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
            .signed(true)
            .signerName(auth.getSignerCommonName())
            .signerEmail(auth.getSignerEmail())
            .signerEdipi(auth.getSignerEdipi())
            .signatureTimestamp(auth.getSignatureTimestamp())
            .certificateIssuer(auth.getCertificateIssuer())
            .certificateSerial(auth.getCertificateSerial())
            .certificateNotBefore(auth.getCertificateNotBefore())
            .certificateNotAfter(auth.getCertificateNotAfter())
            .certificateVerified(auth.getCertificateVerified())
            .verificationDate(auth.getCertificateVerificationDate())
            .verificationNotes(auth.getCertificateVerificationNotes())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Re-verify signature (for auditing)
     */
    @PostMapping("/{id}/verify-signature")
    public ResponseEntity<SignatureVerificationResponse> verifySignature(
        @PathVariable Long id
    ) {
        Authorization auth = authorizationService.getAuthorization(id);

        if (auth.getSignerCertificate() == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            CertificateValidationResult result =
                digitalSignatureService.verifyCertificate(auth.getSignerCertificate());

            // Update verification status
            auth.setCertificateVerified(result.isValid());
            auth.setCertificateVerificationDate(LocalDateTime.now());
            auth.setCertificateVerificationNotes(result.getNotes());
            authorizationRepository.save(auth);

            return ResponseEntity.ok(SignatureVerificationResponse.builder()
                .valid(result.isValid())
                .verificationDate(LocalDateTime.now())
                .notes(result.getNotes())
                .build());

        } catch (Exception e) {
            logger.error("Verification failed", e);
            return ResponseEntity.status(500).build();
        }
    }
}
```

### 3. Backend Service Layer

**New service: `DigitalSignatureService`**:

```java
@Service
public class DigitalSignatureService {

    @Autowired
    private AuthorizationRepository authorizationRepository;

    /**
     * Sign authorization with client certificate
     */
    public SignatureResult signAuthorization(Long authorizationId, X509Certificate certificate)
            throws Exception {

        Authorization auth = authorizationRepository.findById(authorizationId)
            .orElseThrow(() -> new EntityNotFoundException("Authorization not found"));

        // Extract certificate information
        CertificateInfo certInfo = extractCertificateInfo(certificate);

        // Generate document hash
        String documentHash = generateDocumentHash(auth);

        // Store certificate and signature information
        auth.setDigitalSignatureMethod("TLS_CLIENT_CERT");
        auth.setSignerCertificate(encodeCertificate(certificate));
        auth.setSignerCommonName(certInfo.getCommonName());
        auth.setSignerEmail(certInfo.getEmail());
        auth.setSignerEdipi(certInfo.getEdipi());
        auth.setCertificateIssuer(certificate.getIssuerDN().getName());
        auth.setCertificateSerial(certificate.getSerialNumber().toString(16));
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

        return SignatureResult.builder()
            .success(true)
            .signerName(certInfo.getCommonName())
            .signerEmail(certInfo.getEmail())
            .signatureTimestamp(auth.getSignatureTimestamp())
            .build();
    }

    /**
     * Generate canonical document hash
     */
    public String generateDocumentHash(Authorization authorization) throws Exception {
        // Create canonical representation of authorization
        Map<String, Object> canonicalData = new LinkedHashMap<>();
        canonicalData.put("id", authorization.getId());
        canonicalData.put("name", authorization.getName());
        canonicalData.put("sspItemId", authorization.getSspItemId());
        canonicalData.put("sarItemId", authorization.getSarItemId());
        canonicalData.put("completedContent", authorization.getCompletedContent());
        canonicalData.put("dateAuthorized", authorization.getDateAuthorized());
        canonicalData.put("dateExpired", authorization.getDateExpired());
        canonicalData.put("systemOwner", authorization.getSystemOwner());
        canonicalData.put("securityManager", authorization.getSecurityManager());
        canonicalData.put("authorizingOfficial", authorization.getAuthorizingOfficial());

        // Serialize to JSON (deterministic order)
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String canonicalJson = mapper.writeValueAsString(canonicalData);

        // Compute SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));

        // Return hex string
        return bytesToHex(hashBytes);
    }

    /**
     * Extract certificate information
     */
    public CertificateInfo extractCertificateInfo(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectDN().getName();

        // Parse DN
        String cn = extractDNField(subjectDN, "CN");
        String email = extractDNField(subjectDN, "E");

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
            .serialNumber(certificate.getSerialNumber().toString(16))
            .notBefore(certificate.getNotBefore())
            .notAfter(certificate.getNotAfter())
            .build();
    }

    /**
     * Validate certificate
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
        } catch (CertificateNotYetValidException e) {
            valid = false;
            notes.add("ERROR: Certificate is not yet valid");
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
            }
        }

        // 3. Check issuer (optional - if you want to validate against specific CAs)
        String issuer = certificate.getIssuerDN().getName();
        if (issuer.contains("DOD") || issuer.contains("U.S. Government")) {
            notes.add("Certificate issued by recognized authority: " + issuer);
        } else {
            notes.add("WARNING: Certificate issuer not recognized: " + issuer);
        }

        // 4. Check certificate type (optional)
        String cn = extractDNField(certificate.getSubjectDN().getName(), "CN");
        if (cn != null && cn.matches(".*\\d{10}$")) {
            notes.add("Certificate appears to be CAC/PIV format");
        }

        // TODO: Add OCSP/CRL checking for revocation (optional)
        // This requires network access and additional configuration

        return CertificateValidationResult.builder()
            .valid(valid)
            .notes(String.join("; ", notes))
            .build();
    }

    /**
     * Verify existing certificate
     */
    public CertificateValidationResult verifyCertificate(String certificatePem) throws Exception {
        X509Certificate certificate = decodeCertificate(certificatePem);
        return validateCertificate(certificate);
    }

    // Helper methods

    private String encodeCertificate(X509Certificate certificate) throws Exception {
        byte[] encoded = certificate.getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }

    private X509Certificate decodeCertificate(String certificatePem) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(certificatePem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
    }

    private String extractDNField(String dn, String field) {
        Pattern pattern = Pattern.compile(field + "=([^,]+)");
        Matcher matcher = pattern.matcher(dn);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
```

### 4. Frontend Changes

**New component: `DigitalSignatureStep.tsx`**:

```typescript
'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { ShieldCheck, AlertTriangle, CheckCircle, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface DigitalSignatureStepProps {
  authorizationId: number;
  authorizationName: string;
  onSignatureComplete: (result: SignatureResult) => void;
  onSkip: () => void;
}

interface SignatureResult {
  success: boolean;
  signerName?: string;
  signerEmail?: string;
  signatureTimestamp?: string;
  error?: string;
}

export function DigitalSignatureStep({
  authorizationId,
  authorizationName,
  onSignatureComplete,
  onSkip
}: DigitalSignatureStepProps) {
  const [signing, setSigning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSign = async () => {
    setSigning(true);
    setError(null);

    try {
      // Call the signing endpoint
      // This will trigger browser's certificate selection dialog
      const response = await fetch('/api/authorizations/sign-with-cert', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // Important: include credentials for client cert
        body: JSON.stringify({
          authorizationId
        })
      });

      if (response.ok) {
        const result: SignatureResult = await response.json();

        if (result.success) {
          onSignatureComplete(result);
        } else {
          setError(result.error || 'Signing failed');
        }
      } else {
        const errorData = await response.json();
        setError(errorData.message || `Signing failed with status ${response.status}`);
      }
    } catch (err) {
      console.error('Signing error:', err);
      setError('Failed to sign authorization. Please ensure your CAC/PIV card is inserted and ActivClient is running.');
    } finally {
      setSigning(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card className="p-6">
        <div className="flex items-start gap-4 mb-6">
          <ShieldCheck className="h-8 w-8 text-blue-500 flex-shrink-0" />
          <div>
            <h3 className="text-xl font-semibold mb-2">Digital Signature with CAC/PIV</h3>
            <p className="text-gray-600">
              Sign the authorization "{authorizationName}" using your CAC or PIV card to provide
              cryptographic proof of authorization.
            </p>
          </div>
        </div>

        {/* Instructions */}
        <Alert className="mb-6">
          <AlertDescription>
            <div className="space-y-2">
              <p className="font-semibold">Before signing:</p>
              <ul className="list-disc ml-6 space-y-1">
                <li>Ensure your CAC/PIV card is inserted into the card reader</li>
                <li>Verify ActivClient is running (look for system tray icon)</li>
                <li>Have your PIN ready</li>
              </ul>
            </div>
          </AlertDescription>
        </Alert>

        {/* What happens when you sign */}
        <div className="mb-6 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
          <h4 className="font-semibold mb-2 text-blue-900 dark:text-blue-100">What happens when you click "Sign":</h4>
          <ol className="list-decimal ml-6 space-y-1 text-sm text-blue-800 dark:text-blue-200">
            <li>Your browser will show a certificate selection dialog</li>
            <li>Select your CAC/PIV signature certificate</li>
            <li>ActivClient will prompt for your PIN</li>
            <li>Enter your PIN to complete the signature</li>
            <li>The authorization will be digitally signed</li>
          </ol>
        </div>

        {/* Error display */}
        {error && (
          <Alert className="mb-6" variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              <p className="font-semibold">Signing Failed</p>
              <p className="text-sm mt-1">{error}</p>
              <p className="text-sm mt-2">
                Common solutions:
              </p>
              <ul className="list-disc ml-6 text-sm mt-1">
                <li>Ensure your card is fully inserted</li>
                <li>Restart ActivClient</li>
                <li>Check that your certificate is not expired</li>
                <li>Verify your PIN is correct</li>
              </ul>
            </AlertDescription>
          </Alert>
        )}

        {/* Action buttons */}
        <div className="flex gap-3">
          <Button
            onClick={handleSign}
            disabled={signing}
            className="flex-1"
            size="lg"
          >
            {signing ? (
              <>
                <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                Waiting for signature...
              </>
            ) : (
              <>
                <ShieldCheck className="h-5 w-5 mr-2" />
                Sign with CAC/PIV
              </>
            )}
          </Button>

          <Button
            variant="outline"
            onClick={onSkip}
            disabled={signing}
            size="lg"
          >
            Skip Signature
          </Button>
        </div>

        {/* Info about skipping */}
        <p className="text-xs text-gray-500 mt-3 text-center">
          Skipping the digital signature will create the authorization without cryptographic proof.
          You can add a signature later if needed.
        </p>
      </Card>

      {/* Technical details (collapsible) */}
      <details className="text-sm">
        <summary className="cursor-pointer text-gray-600 hover:text-gray-900">
          Technical Details
        </summary>
        <div className="mt-2 p-4 bg-gray-50 dark:bg-gray-900 rounded border text-xs space-y-2">
          <p><strong>Signature Method:</strong> TLS Client Certificate Authentication</p>
          <p><strong>Certificate Type:</strong> CAC/PIV Signature Certificate</p>
          <p><strong>Middleware:</strong> ActivClient (HID Global)</p>
          <p><strong>Compliance:</strong> FIPS 201, NIST SP 800-53, DoD PKI</p>
          <p><strong>Authorization ID:</strong> {authorizationId}</p>
        </div>
      </details>
    </div>
  );
}
```

**Update `authorization-wizard.tsx`**:

```typescript
// Add import
import { DigitalSignatureStep } from './digital-signature-step';

// Add new step type
type Step = 'select-ssp' | 'select-sar' | 'stakeholder-info' | 'visualize' |
            'select-template' | 'fill-variables' | 'conditions' | 'review' |
            'sign'; // New step

// Add state for draft authorization
const [draftAuthorizationId, setDraftAuthorizationId] = useState<number | null>(null);

// Update handleNext to create draft before signing
const handleNext = async () => {
  // ... existing code ...
  else if (step === 'review') {
    // Create draft authorization before showing sign step
    const draft = await createDraftAuthorization();
    setDraftAuthorizationId(draft.id);
    setStep('sign');
  }
};

// Add createDraftAuthorization function
const createDraftAuthorization = async () => {
  if (!selectedSsp || !selectedTemplate || !authorizationName) {
    throw new Error('Missing required data');
  }

  const authData = {
    name: authorizationName,
    sspItemId: selectedSsp.itemId,
    sarItemId: selectedSar?.itemId,
    templateId: selectedTemplate.id,
    variableValues,
    dateAuthorized,
    dateExpired,
    systemOwner,
    securityManager,
    authorizingOfficial,
    editedContent,
    conditions,
  };

  return await apiClient.createAuthorization(authData);
};

// Add sign step rendering
{step === 'sign' && draftAuthorizationId && (
  <DigitalSignatureStep
    authorizationId={draftAuthorizationId}
    authorizationName={authorizationName}
    onSignatureComplete={(result) => {
      toast.success(`Authorization signed by ${result.signerName}`);
      onSave(draftAuthorizationId); // Complete the wizard
    }}
    onSkip={() => {
      onSave(draftAuthorizationId); // Complete without signature
    }}
  />
)}
```

**Add signature display to authorization detail page**:

```typescript
// In authorizations/page.tsx, add signature display

{selectedAuthorization.signerCertificate && (
  <div className="space-y-4">
    <h3 className="text-lg font-semibold border-b pb-2">Digital Signature</h3>
    <Card className="p-4 bg-green-900/10 border-green-700">
      <div className="flex items-start gap-3">
        <CheckCircle className="h-6 w-6 text-green-500 flex-shrink-0" />
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <Badge className="bg-green-600">Digitally Signed</Badge>
            {selectedAuthorization.certificateVerified && (
              <Badge variant="outline" className="border-green-500 text-green-500">
                Verified
              </Badge>
            )}
          </div>
          <p className="font-semibold">{selectedAuthorization.signerCommonName}</p>
          {selectedAuthorization.signerEmail && (
            <p className="text-sm text-gray-600">{selectedAuthorization.signerEmail}</p>
          )}
          {selectedAuthorization.signerEdipi && (
            <p className="text-xs text-gray-500">EDIPI: {selectedAuthorization.signerEdipi}</p>
          )}
          <p className="text-xs text-gray-500 mt-2">
            Signed: {new Date(selectedAuthorization.signatureTimestamp).toLocaleString()}
          </p>
        </div>
      </div>

      {/* Certificate details (expandable) */}
      <details className="mt-4">
        <summary className="cursor-pointer text-sm font-semibold text-gray-700">
          Certificate Details
        </summary>
        <div className="mt-2 p-3 bg-gray-50 rounded text-xs space-y-1">
          <p><strong>Issuer:</strong> {selectedAuthorization.certificateIssuer}</p>
          <p><strong>Serial:</strong> {selectedAuthorization.certificateSerial}</p>
          <p><strong>Valid From:</strong> {new Date(selectedAuthorization.certificateNotBefore).toLocaleDateString()}</p>
          <p><strong>Valid Until:</strong> {new Date(selectedAuthorization.certificateNotAfter).toLocaleDateString()}</p>
          {selectedAuthorization.certificateVerificationDate && (
            <p><strong>Last Verified:</strong> {new Date(selectedAuthorization.certificateVerificationDate).toLocaleDateString()}</p>
          )}
          {selectedAuthorization.certificateVerificationNotes && (
            <p className="mt-2 text-gray-600">{selectedAuthorization.certificateVerificationNotes}</p>
          )}
        </div>
      </details>

      {/* Verify button */}
      <Button
        variant="outline"
        size="sm"
        className="mt-3"
        onClick={async () => {
          const result = await apiClient.verifyAuthorizationSignature(selectedAuthorization.id);
          if (result.valid) {
            toast.success('Signature verified successfully');
          } else {
            toast.error('Signature verification failed');
          }
        }}
      >
        Re-verify Signature
      </Button>
    </Card>
  </div>
)}
```

---

## Security Considerations

### 1. TLS Configuration

**Backend must configure TLS for client certificates**:

```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    client-auth: want  # Request but don't require client cert
```

**Certificate Extraction**:
```java
X509Certificate[] certs = (X509Certificate[])
    request.getAttribute("javax.servlet.request.X509Certificate");
```

### 2. Certificate Validation

**Must validate**:
- ✅ Certificate is not expired (`checkValidity()`)
- ✅ Certificate has digitalSignature key usage
- ✅ Certificate issuer is trusted (optional but recommended)
- ✅ Certificate is not revoked (OCSP/CRL - optional)

### 3. Private Key Security

**Private keys never leave the smart card**:
- ✅ Signing occurs on the card
- ✅ PIN required for each signature operation
- ✅ ActivClient handles PIN prompts securely
- ✅ Browser never has access to private key

### 4. Document Integrity

**Store permanently**:
- Document hash (SHA-256)
- Signer certificate
- Signature timestamp
- Certificate validation results

This proves:
- **Who signed**: Certificate CN, EDIPI
- **When**: Timestamp
- **What**: Document hash
- **Valid**: Certificate verification result

### 5. Non-Repudiation

The combination of:
- Signed document hash
- Signer's certificate
- Timestamp
- Certificate validation

Provides **cryptographic proof** that the named individual authorized the system at the specified time.

### 6. Privacy

**Certificate contains PII**:
- Name
- Email
- EDIPI (DoD ID number)

**Protections**:
- Store encrypted at rest (database encryption)
- Access controls (only authorized users can view)
- Audit log access to signature data
- Implement retention policies

---

## Implementation Plan

### Phase 1: Backend Foundation (1.5 weeks)

**Days 1-3**:
- [ ] Update `Authorization` entity with signature fields
- [ ] Create database migration
- [ ] Configure TLS for client certificates
- [ ] Test certificate extraction from request

**Days 4-7**:
- [ ] Create `DigitalSignatureService`
- [ ] Implement certificate parsing
- [ ] Implement certificate validation
- [ ] Implement document hash generation
- [ ] Write unit tests

**Days 8-10**:
- [ ] Create `AuthorizationController` endpoints
  - `/sign-with-cert`
  - `/{id}/signature`
  - `/{id}/verify-signature`
- [ ] Write integration tests
- [ ] Test with test certificates

### Phase 2: Frontend Integration (1.5 weeks)

**Days 1-4**:
- [ ] Create `DigitalSignatureStep` component
- [ ] Implement signing flow
- [ ] Add error handling
- [ ] Update authorization wizard
- [ ] Test with mock backend

**Days 5-7**:
- [ ] Create signature display component
- [ ] Update authorization detail page
- [ ] Update authorization list (show signed badge)
- [ ] Add signature verification UI
- [ ] Test end-to-end flow

**Days 8-10**:
- [ ] Browser compatibility testing
- [ ] UI/UX refinements
- [ ] Documentation updates

### Phase 3: Testing & Documentation (1 week)

**Days 1-3**:
- [ ] End-to-end testing with real CAC/PIV cards
- [ ] Test on different browsers (Chrome, Firefox, Edge)
- [ ] Test error scenarios
- [ ] Security review
- [ ] Performance testing

**Days 4-5**:
- [ ] User documentation
- [ ] Administrator guide
- [ ] Troubleshooting guide
- [ ] API documentation

**Days 6-7**:
- [ ] Final testing
- [ ] Bug fixes
- [ ] Code review
- [ ] Prepare for deployment

### Phase 4: Deployment (0.5 weeks)

**Days 1-2**:
- [ ] Deploy backend changes
- [ ] Deploy frontend changes
- [ ] Update TLS configuration
- [ ] Monitor rollout
- [ ] User support

**Total Timeline**: 4.5 weeks (~1 month)

---

## User Experience Design

### Pre-requisites (User Already Has)

✅ **CAC/PIV card** issued by their organization
✅ **Card reader** connected to computer
✅ **ActivClient** installed and configured
✅ **PIN** for their card

**No additional software needed!**

### Signing Experience

1. **User completes authorization wizard**
   - Steps 1-8: Existing flow (SSP, SAR, details, template, etc.)
   - Step 9: **Digital Signature (NEW)**

2. **Digital signature step**
   - See instructions to insert card
   - Click "Sign with CAC/PIV"
   - **Browser shows native certificate selection dialog**
   - Select signature certificate (usually shows name + "Signature")
   - **ActivClient shows PIN dialog**
   - Enter PIN
   - Signature created!
   - Show success message with signer name
   - Click "Complete Authorization"

3. **Post-signature**
   - Authorization detail page shows:
     - "Digitally Signed" badge (green)
     - Signer name (from certificate)
     - Signer email
     - EDIPI
     - Signature timestamp
     - Certificate details (expandable)
     - "Re-verify Signature" button

4. **Optional: Skip signature**
   - User can click "Skip Signature" if:
     - Card not available
     - Technical issues
     - Want to sign later
   - Authorization created without signature
   - Can potentially add signature later (future enhancement)

### Error Handling

**Common errors and solutions**:

| Error | User Message | Solution |
|-------|--------------|----------|
| No certificate selected | "No certificate was selected" | Try again, ensure card is inserted |
| Wrong PIN | "Incorrect PIN" (from ActivClient) | Re-enter PIN, check for CAPS LOCK |
| Card locked | "Card is locked" (from ActivClient) | Contact security office for unlock/replacement |
| Certificate expired | "Certificate has expired" | Contact PKI administrator for new certificate |
| Certificate not found | "No certificates found" | Check ActivClient is running, card is inserted |
| Browser not configured | "TLS configuration error" | Check browser security settings |
| ActivClient not running | "Cannot access smart card" | Start ActivClient |

---

## Testing Strategy

### Unit Tests

**Backend**:
- [ ] `DigitalSignatureService.generateDocumentHash()` - consistent hashing
- [ ] `DigitalSignatureService.extractCertificateInfo()` - CN, email, EDIPI parsing
- [ ] `DigitalSignatureService.validateCertificate()` - validation logic
- [ ] Certificate encoding/decoding
- [ ] DN field extraction

### Integration Tests

- [ ] POST `/api/authorizations/sign-with-cert` with test certificate
- [ ] GET `/api/authorizations/{id}/signature` - returns signature details
- [ ] POST `/api/authorizations/{id}/verify-signature` - verification works
- [ ] Test with expired certificate - should fail
- [ ] Test without certificate - should return 401

### End-to-End Tests

1. **Happy path with real CAC/PIV**:
   - Insert card
   - Complete authorization wizard
   - Click "Sign with CAC/PIV"
   - Select certificate in browser dialog
   - Enter PIN in ActivClient
   - Verify signature appears on detail page

2. **Error scenarios**:
   - No card inserted - appropriate error message
   - Wrong PIN - ActivClient handles, can retry
   - Certificate expired - validation fails
   - Cancel certificate selection - can retry
   - ActivClient not running - clear error message

3. **Browser compatibility**:
   - Chrome on Windows + ActivClient
   - Firefox on Windows + ActivClient
   - Edge on Windows + ActivClient
   - Chrome on macOS + OpenSC
   - Firefox on macOS + OpenSC

### Security Testing

- [ ] Attempt to submit without certificate - should fail
- [ ] Attempt to use expired certificate - should be rejected
- [ ] Verify PIN is not logged
- [ ] Verify certificate data is stored securely
- [ ] Verify document hash cannot be tampered
- [ ] Test HTTPS/TLS configuration
- [ ] Verify access controls on signature endpoints

### Performance Testing

- [ ] Certificate parsing time (< 100ms)
- [ ] Hash generation time (< 500ms)
- [ ] Signature validation time (< 200ms)
- [ ] End-to-end signing time (< 10 seconds including user interaction)

---

## Compliance Requirements

### Federal PKI Requirements

1. **FIPS 201** - Personal Identity Verification (PIV) of Federal Employees and Contractors
   - ✅ Use PIV authentication certificate for signing
   - ✅ Private key remains on hardware token
   - ✅ PIN required for each signature operation

2. **NIST SP 800-63** - Digital Identity Guidelines
   - ✅ AAL3 (Authenticator Assurance Level 3) - multi-factor with hardware token
   - ✅ Cryptographic proof of identity

3. **NIST SP 800-53** - Security and Privacy Controls
   - ✅ IA-2 (Identification and Authentication)
   - ✅ IA-5 (Authenticator Management)
   - ✅ SC-17 (Public Key Infrastructure Certificates)

4. **DoD PKI** (if DoD deployment)
   - ✅ DoD certificate hierarchy
   - ✅ Compatible with CAC cards
   - ✅ DoD Root CA trust anchor (configurable)

### Electronic Signature Laws

1. **ESIGN Act** - Digital signature must be:
   - ✅ Attributable to the person signing (certificate CN)
   - ✅ Capable of verification (certificate validation)
   - ✅ Under sole control of the signer (PIN protected)
   - ✅ Linked to the document (document hash)

2. **UETA** - Uniform Electronic Transactions Act:
   - ✅ Electronic signature has same legal effect as handwritten
   - ✅ Record retention (stored in database)

### Audit Requirements

Must maintain audit trail including:
- ✅ Who signed (certificate subject)
- ✅ When signed (timestamp)
- ✅ What was signed (document hash)
- ✅ Certificate details (issuer, serial, validity)
- ✅ Verification results

---

## Browser Compatibility

### Chrome / Chromium / Edge

**Status**: ✅ **Supported**

**Requirements**:
- ActivClient installed and running
- Certificate imported to Windows Certificate Store (ActivClient does this)
- HTTPS with client certificate authentication configured

**User Experience**:
- Chrome shows native certificate selection dialog
- ActivClient PIN prompt appears
- Signature created

**Testing**: Test on Windows 10/11 with ActivClient

---

### Firefox

**Status**: ✅ **Supported** (Best support)

**Requirements**:
- ActivClient installed and running
- PKCS#11 module configured (ActivClient installer does this)

**User Experience**:
- Firefox has excellent smart card support
- Native certificate selection dialog
- ActivClient PIN prompt
- Most reliable browser for CAC/PIV

**Testing**: Test on Windows, macOS, Linux

**Manual PKCS#11 configuration** (if needed):
1. Firefox → Settings → Privacy & Security → Security Devices
2. Click "Load"
3. Module Name: ActivClient
4. Module filename: `C:\Program Files\ActivIdentity\ActivClient\acpkcs211.dll`

---

### Safari (macOS)

**Status**: ⚠️ **Limited Support**

**Requirements**:
- OpenSC or similar PKCS#11 middleware
- Certificate in macOS Keychain

**Notes**:
- Safari uses macOS Keychain for certificates
- May require additional configuration
- Less common in DoD environments

---

### Summary Table

| Browser | Platform | Status | Middleware | Notes |
|---------|----------|--------|------------|-------|
| Chrome | Windows | ✅ Supported | ActivClient | Uses Windows Cert Store |
| Edge | Windows | ✅ Supported | ActivClient | Same as Chrome |
| Firefox | Windows | ✅ Supported | ActivClient | Best support, PKCS#11 |
| Firefox | macOS | ✅ Supported | OpenSC | PKCS#11 configuration |
| Firefox | Linux | ✅ Supported | OpenSC | PKCS#11 configuration |
| Chrome | macOS | ⚠️ Limited | OpenSC | Requires configuration |
| Safari | macOS | ⚠️ Limited | OpenSC | Keychain integration |

**Recommendation**: Support Chrome/Edge/Firefox on Windows with ActivClient as primary target (covers 95%+ of DoD users).

---

## Troubleshooting Guide

### Issue: Certificate selection dialog doesn't appear

**Possible causes**:
1. Server not configured for client certificate authentication
2. HTTPS not enabled
3. Browser cache issues

**Solutions**:
- Verify server TLS configuration: `client-auth: want`
- Check HTTPS is enabled
- Clear browser cache and restart
- Try in incognito/private mode

### Issue: No certificates shown in selection dialog

**Possible causes**:
1. CAC/PIV card not inserted
2. ActivClient not running
3. Card reader not connected
4. Certificate not imported to browser

**Solutions**:
- Insert card fully into reader
- Start ActivClient (look for system tray icon)
- Check card reader is connected and powered
- **Firefox**: Check PKCS#11 module is loaded
- **Chrome**: Check Windows Certificate Store has certificates

### Issue: "Incorrect PIN" error

**Possible causes**:
1. Wrong PIN entered
2. CAPS LOCK on
3. Card locked (too many wrong attempts)

**Solutions**:
- Re-enter PIN carefully
- Check CAPS LOCK
- If locked: Contact security office for PUK unlock or new card

### Issue: "Certificate has expired"

**Possible causes**:
1. Certificate validity period has ended

**Solutions**:
- Contact PKI administrator or security office
- Request new certificate
- May need to visit RAPIDS site for new CAC/PIV

### Issue: Signature verification fails

**Possible causes**:
1. Document was modified after signing
2. Certificate has been revoked
3. System clock incorrect

**Solutions**:
- Check document hasn't been tampered with
- Verify certificate status with PKI administrator
- Check system time is correct
- Re-verify after addressing issues

---

## Recommendations

### Immediate Next Steps

1. **Validate Requirements** (2 days)
   - Confirm CAC/PIV requirement with stakeholders
   - Identify target user base (DoD, federal, contractor?)
   - Determine if signature is mandatory or optional
   - Get sample test cards for development

2. **Technical Proof of Concept** (3 days)
   - Set up TLS with client certificate authentication
   - Test certificate extraction from browser
   - Test with real CAC/PIV card
   - Demo to stakeholders

3. **Security Review** (2 days)
   - Review design with security team
   - Confirm compliance requirements
   - Get approval for TLS configuration changes
   - Review data storage and privacy

4. **Implementation** (4.5 weeks)
   - Follow implementation plan above
   - Weekly demos to stakeholders
   - Iterative testing with real users

### Success Metrics

- **Functionality**: 95%+ successful signature operations
- **Usability**: < 30 seconds to complete signature (including PIN entry)
- **Reliability**: No additional software installation required
- **Security**: 100% compliance with federal PKI requirements
- **Browser Support**: Works on Chrome, Firefox, Edge with ActivClient

### Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| TLS configuration complex | Medium | Work with DevOps team, use clear documentation |
| Browser compatibility issues | Medium | Test on all major browsers early, document requirements |
| ActivClient compatibility | Low | ActivClient is standard DoD middleware, well-tested |
| User resistance | Low | Signature is optional, clear benefits explained |
| Certificate validation complexity | Medium | Use well-tested libraries, implement comprehensive error handling |
| Users forget PIN | Low | Standard problem with existing workflows, support available |

---

## Conclusion

The **Browser-Native Certificate Access with ActivClient** approach provides:

✅ **Zero Additional Software**: Users already have ActivClient installed
✅ **Simple Deployment**: Only backend and frontend changes needed
✅ **Familiar UX**: Native browser certificate selection dialogs
✅ **Secure**: Private keys never leave the card, PIN required
✅ **Compliant**: Meets federal PKI and electronic signature requirements
✅ **Reliable**: Proven technology stack used across government

This approach is **strongly recommended** for implementation to enable CAC/PIV digital signatures in the authorization workflow.

The implementation will take approximately **4.5 weeks** (1 month) with a team of 2 developers, following the phased approach outlined in this document.

**Key Advantage**: No custom applications to install or maintain - leverages existing infrastructure that government users already have.

---

## Appendix A: Sample Certificate Information

**Example CAC Certificate Subject**:
```
CN=DOE.JOHN.MIDDLE.1234567890
OU=CONTRACTOR
OU=PKI
OU=DOD
O=U.S. Government
C=US
```

**Extracting EDIPI**: Last 10 digits of CN = `1234567890`

**Example PIV Certificate Subject**:
```
CN=Last.First.Middle.1234567890
OU=People
OU=Department of Example
O=U.S. Government
C=US
```

## Appendix B: TLS Configuration Examples

**application.yml** (Spring Boot):
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    # Request client cert but don't require for all endpoints
    client-auth: want
```

**Extracting Certificate in Controller**:
```java
@PostMapping("/sign-with-cert")
public ResponseEntity<SignatureResult> signWithCert(
    HttpServletRequest request
) {
    X509Certificate[] certs = (X509Certificate[])
        request.getAttribute("javax.servlet.request.X509Certificate");

    if (certs == null || certs.length == 0) {
        return ResponseEntity.status(401).body(
            new SignatureResult(false, "No client certificate")
        );
    }

    X509Certificate cert = certs[0];
    // Process certificate...
}
```

## Appendix C: ActivClient Configuration

**Check ActivClient Status**:
- Look for ActivClient icon in system tray (Windows)
- Right-click icon → Status → should show "Card Present"

**View Certificates**:
- ActivClient → Tools → Advanced Diagnostics
- Certificates tab → should show certificates from card

**Middleware Location** (for Firefox PKCS#11):
- Windows: `C:\Program Files\ActivIdentity\ActivClient\acpkcs211.dll`
- Windows (32-bit): `C:\Program Files (x86)\ActivIdentity\ActivClient\acpkcs211.dll`

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-25 | System Architect | Initial design with helper app approach |
| 2.0 | 2025-10-25 | System Architect | Updated to browser-native ActivClient approach |

---

**End of Document**
