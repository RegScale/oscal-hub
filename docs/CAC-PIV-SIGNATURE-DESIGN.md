# CAC/PIV Digital Signature Integration Design

**Date**: October 25, 2025
**Status**: 📋 Design Phase
**Author**: System Architect
**Version**: 1.0

## Executive Summary

This document outlines the design and implementation plan for integrating CAC/PIV (Common Access Card/Personal Identity Verification) digital signature capabilities into the OSCAL Tools authorization workflow. This feature will enable government users to digitally sign system authorization documents using their smart cards, providing strong authentication, non-repudiation, and compliance with federal PKI requirements.

## Table of Contents

1. [Background](#background)
2. [Current State Analysis](#current-state-analysis)
3. [Technical Approaches](#technical-approaches)
4. [Recommended Architecture](#recommended-architecture)
5. [Integration Points](#integration-points)
6. [Security Considerations](#security-considerations)
7. [Implementation Plan](#implementation-plan)
8. [User Experience Design](#user-experience-design)
9. [Testing Strategy](#testing-strategy)
10. [Compliance Requirements](#compliance-requirements)
11. [Alternative Approaches](#alternative-approaches)

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

## Technical Approaches

### Approach 1: Web Crypto API with Smart Card Support

**Overview**: Use browser's native Web Crypto API to access smart cards.

**Pros**:
- ✅ No browser extensions required
- ✅ Standards-based (W3C)
- ✅ Works on modern browsers
- ✅ Direct JavaScript access

**Cons**:
- ❌ **Limited smart card support** - Web Crypto API was designed for software keys
- ❌ Browser support varies significantly
- ❌ No standard API for PKCS#11 access
- ❌ Cannot directly access CAC/PIV without middleware

**Verdict**: ⚠️ **Not recommended** as primary approach - Web Crypto API doesn't natively support smart card operations.

---

### Approach 2: Browser Native Smart Card Support

**Overview**: Use browser's built-in certificate selection and signing.

#### Firefox

Firefox has the best native smart card support:
- Built-in PKCS#11 module management
- Native certificate selection dialog
- Can access smart cards via PC/SC middleware

**Implementation**:
```javascript
// Firefox can access smart cards via native certificate store
// User selects certificate from browser's built-in dialog
window.crypto.subtle.sign(
  { name: "RSASSA-PKCS1-v1_5" },
  privateKey, // From certificate selected by user
  dataToSign
)
```

#### Chrome/Edge (Chromium)

Chrome requires:
- Middleware (e.g., ActivClient, OpenSC)
- Certificate imported to system keystore
- Extension or native messaging for advanced features

**Pros**:
- ✅ No custom code for basic scenarios
- ✅ Browser handles certificate selection UI
- ✅ Uses system certificate store
- ✅ Firefox has excellent PKCS#11 support

**Cons**:
- ❌ **Requires middleware** (ActivClient, OpenSC, etc.)
- ❌ Browser-specific implementations
- ❌ User must install and configure middleware
- ❌ Limited programmatic control
- ❌ Certificate selection UX varies by browser

**Verdict**: ✅ **Good option** for Firefox users with middleware already installed.

---

### Approach 3: Browser Extension

**Overview**: Create browser extension to access smart card via native messaging.

**Architecture**:
```
Web App → Browser Extension → Native Host App → PKCS#11 → Smart Card
```

**Example**: Chrome Extension with Native Messaging Host

**Extension** (JavaScript):
```javascript
// Content script in web page
chrome.runtime.sendMessage({
  action: "signData",
  data: base64Data
}, (response) => {
  if (response.signature) {
    // Upload signature to server
  }
});
```

**Native Host** (Java, C++, or Node.js):
```java
// Accesses PKCS#11 library
SunPKCS11 provider = new SunPKCS11(configFile);
Security.addProvider(provider);
KeyStore keyStore = KeyStore.getInstance("PKCS11", provider);
// Get certificate and sign
```

**Pros**:
- ✅ Full control over smart card operations
- ✅ Cross-browser support (Chrome, Firefox, Edge)
- ✅ Can provide rich UX
- ✅ Can cache certificate selection
- ✅ Works with all PKCS#11-compliant cards

**Cons**:
- ❌ **Requires installation** - extension + native host
- ❌ Complex deployment (Chrome Web Store, packaging)
- ❌ Maintenance burden (multiple browsers)
- ❌ Permissions and security reviews
- ❌ Native host must be signed/trusted

**Verdict**: ✅ **Best technical solution** but highest deployment complexity.

---

### Approach 4: Server-Side Helper Application

**Overview**: Small desktop application that web app communicates with via localhost HTTP/WebSocket.

**Architecture**:
```
Web App → HTTP/WebSocket → Desktop App (localhost:8000) → PKCS#11 → Smart Card
```

**Example**: Desktop app runs on user's machine:

```java
// Java desktop app with embedded HTTP server
HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
server.createContext("/sign", new SignHandler());
server.start();

class SignHandler implements HttpHandler {
  public void handle(HttpExchange exchange) {
    // 1. Read data from request
    // 2. Access smart card via PKCS#11
    // 3. Prompt user for PIN
    // 4. Sign data
    // 5. Return signature
  }
}
```

**Web app** (JavaScript):
```javascript
// Check if helper app is running
const isHelperRunning = await fetch('http://localhost:8000/health')
  .then(() => true)
  .catch(() => false);

if (isHelperRunning) {
  // Sign document
  const response = await fetch('http://localhost:8000/sign', {
    method: 'POST',
    body: JSON.stringify({ data: documentHash })
  });
  const { signature, certificate } = await response.json();
}
```

**Pros**:
- ✅ No browser extensions needed
- ✅ Full control over signing process
- ✅ Can provide custom UI (PIN entry, certificate selection)
- ✅ Works on all browsers
- ✅ Easier to update than browser extensions
- ✅ Can use Java PKCS#11 support

**Cons**:
- ❌ **Requires installation** of desktop app
- ❌ CORS restrictions (localhost exceptions needed)
- ❌ Port conflicts possible
- ❌ Security: local HTTP endpoint
- ❌ Auto-start configuration needed

**Verdict**: ✅ **Recommended approach** - good balance of functionality and deployment simplicity.

---

### Approach 5: Cloud HSM / Remote Signing Service

**Overview**: Signatures created by remote HSM, not local smart card.

**Examples**:
- DocuSign with AATL certificates
- Adobe Sign with digital signatures
- DigiCert Document Signing Service

**Pros**:
- ✅ No client software required
- ✅ Works on all devices (mobile, tablet)
- ✅ Easy to implement
- ✅ Managed service (reliability, uptime)

**Cons**:
- ❌ **Not CAC/PIV** - uses different certificates
- ❌ Doesn't meet "two-factor" requirement (no physical token)
- ❌ May not comply with federal PKI policies
- ❌ Cost per signature
- ❌ Privacy: document sent to third party

**Verdict**: ❌ **Not suitable** - doesn't use CAC/PIV cards.

---

## Recommended Architecture

### Primary Recommendation: Server-Side Helper Application

**Why this approach?**

1. **Federal Government Environment**:
   - Users already have ActivClient, OpenSC, or similar middleware installed
   - Desktop applications are common and acceptable
   - IT departments can deploy via SCCM, GPO, etc.

2. **Technical Advantages**:
   - Works on all browsers (Chrome, Firefox, Edge, Safari)
   - Java app can use built-in PKCS#11 support
   - No browser extension approval process
   - Easier to update and maintain

3. **User Experience**:
   - One-time installation
   - Auto-start with Windows/macOS
   - System tray icon for status
   - Clear prompts and feedback

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Web Browser                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         Next.js Frontend (localhost:3000)             │  │
│  │                                                       │  │
│  │  Authorization Wizard → Digital Signature Step       │  │
│  └─────────────┬─────────────────────────────────────────┘  │
│                │ HTTP POST                                  │
└────────────────┼────────────────────────────────────────────┘
                 │
                 ▼
    ┌────────────────────────────┐
    │   Spring Boot Backend      │
    │   (localhost:8080)         │
    │                            │
    │  - Generate document hash  │
    │  - Verify signature        │
    │  - Store signature data    │
    └────────────────────────────┘
                 │
                 │ Store signature
                 ▼
    ┌────────────────────────────┐
    │   H2 Database              │
    │                            │
    │  - Authorization record    │
    │  - Digital signature       │
    │  - Certificate             │
    │  - Timestamp               │
    └────────────────────────────┘

         ┌────────────────┐
         │  Web Browser   │
         │  JavaScript    │
         └────┬───────────┘
              │ HTTP POST to localhost:8000
              ▼
    ┌─────────────────────────────────┐
    │  CAC/PIV Helper Application     │
    │  (Java Desktop App)             │
    │                                 │
    │  Port: 8000                     │
    │  Endpoints:                     │
    │    GET  /health                 │
    │    GET  /certificates           │
    │    POST /sign                   │
    │                                 │
    │  CORS: Allow localhost:3000     │
    └────────┬────────────────────────┘
             │ PKCS#11
             ▼
    ┌─────────────────────┐
    │   Middleware        │
    │  (ActivClient,      │
    │   OpenSC, etc.)     │
    └────────┬────────────┘
             │ PC/SC
             ▼
    ┌─────────────────────┐
    │  Smart Card Reader  │
    │   + CAC/PIV Card    │
    └─────────────────────┘
```

---

## Integration Points

### 1. Database Schema Changes

**Add to `Authorization` entity**:

```java
@Entity
public class Authorization {
    // ... existing fields ...

    // Digital Signature Fields
    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature; // Base64-encoded signature

    @Column(name = "signature_algorithm")
    private String signatureAlgorithm; // e.g., "SHA256withRSA"

    @Column(name = "signer_certificate", columnDefinition = "TEXT")
    private String signerCertificate; // Base64-encoded X.509 cert

    @Column(name = "signer_common_name")
    private String signerCommonName; // CN from certificate

    @Column(name = "signer_email")
    private String signerEmail; // Email from certificate

    @Column(name = "signature_timestamp")
    private LocalDateTime signatureTimestamp;

    @Column(name = "document_hash")
    private String documentHash; // SHA-256 hash of signed content

    @Column(name = "signature_verified")
    private Boolean signatureVerified; // Verification result

    @Column(name = "signature_verification_date")
    private LocalDateTime signatureVerificationDate;
}
```

### 2. Backend API Endpoints

**New endpoints in `AuthorizationController`**:

```java
@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    /**
     * Generate hash of authorization document for signing
     */
    @PostMapping("/{id}/generate-signature-data")
    public ResponseEntity<SignatureDataResponse> generateSignatureData(
        @PathVariable Long id
    ) {
        // 1. Get authorization
        // 2. Generate canonical representation of document
        // 3. Compute SHA-256 hash
        // 4. Return hash + metadata
    }

    /**
     * Submit digital signature
     */
    @PostMapping("/{id}/submit-signature")
    public ResponseEntity<Void> submitSignature(
        @PathVariable Long id,
        @RequestBody SubmitSignatureRequest request
    ) {
        // 1. Validate signature format
        // 2. Extract certificate
        // 3. Verify signature
        // 4. Store signature data
        // 5. Update authorization status
    }

    /**
     * Verify existing signature
     */
    @GetMapping("/{id}/verify-signature")
    public ResponseEntity<SignatureVerificationResponse> verifySignature(
        @PathVariable Long id
    ) {
        // 1. Get authorization with signature
        // 2. Re-compute document hash
        // 3. Verify signature against certificate
        // 4. Check certificate validity
        // 5. Return verification result
    }

    /**
     * Get signature details
     */
    @GetMapping("/{id}/signature-details")
    public ResponseEntity<SignatureDetailsResponse> getSignatureDetails(
        @PathVariable Long id
    ) {
        // Return certificate info, signer details, timestamp
    }
}
```

### 3. Backend Service Layer

**New service: `DigitalSignatureService`**:

```java
@Service
public class DigitalSignatureService {

    /**
     * Generate document hash for signing
     */
    public String generateDocumentHash(Authorization authorization) {
        // 1. Create canonical JSON representation
        // 2. Include all fields that shouldn't change
        // 3. Compute SHA-256 hash
        // 4. Return Base64-encoded hash
    }

    /**
     * Verify digital signature
     */
    public boolean verifySignature(
        String documentHash,
        String signature,
        String certificate
    ) {
        // 1. Decode signature and certificate
        // 2. Extract public key from certificate
        // 3. Verify signature using public key
        // 4. Return verification result
    }

    /**
     * Extract certificate information
     */
    public CertificateInfo extractCertificateInfo(String certificate) {
        // 1. Parse X.509 certificate
        // 2. Extract CN, email, issuer, validity dates
        // 3. Return structured info
    }

    /**
     * Validate certificate
     */
    public CertificateValidationResult validateCertificate(String certificate) {
        // 1. Check certificate validity dates
        // 2. Verify certificate chain (if configured)
        // 3. Check revocation status (OCSP/CRL if configured)
        // 4. Return validation result
    }
}
```

### 4. Frontend Changes

**New component: `DigitalSignatureStep.tsx`**:

```typescript
interface DigitalSignatureStepProps {
  authorizationId: number;
  documentHash: string;
  onSignatureComplete: (signatureData: SignatureData) => void;
  onSkip: () => void;
}

export function DigitalSignatureStep({
  authorizationId,
  documentHash,
  onSignatureComplete,
  onSkip
}: DigitalSignatureStepProps) {
  const [helperStatus, setHelperStatus] = useState<'checking' | 'available' | 'unavailable'>('checking');
  const [certificates, setCertificates] = useState<Certificate[]>([]);
  const [selectedCertificate, setSelectedCertificate] = useState<Certificate | null>(null);
  const [signing, setSigning] = useState(false);

  useEffect(() => {
    checkHelperAvailability();
  }, []);

  const checkHelperAvailability = async () => {
    try {
      const response = await fetch('http://localhost:8000/health');
      if (response.ok) {
        setHelperStatus('available');
        loadCertificates();
      } else {
        setHelperStatus('unavailable');
      }
    } catch (error) {
      setHelperStatus('unavailable');
    }
  };

  const loadCertificates = async () => {
    const response = await fetch('http://localhost:8000/certificates');
    const certs = await response.json();
    setCertificates(certs);
  };

  const handleSign = async () => {
    if (!selectedCertificate) return;

    setSigning(true);
    try {
      // Call helper app to sign
      const response = await fetch('http://localhost:8000/sign', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          data: documentHash,
          certificateId: selectedCertificate.id
        })
      });

      const result = await response.json();

      if (result.signature) {
        // Submit to backend
        await apiClient.submitSignature(authorizationId, {
          signature: result.signature,
          certificate: result.certificate,
          algorithm: result.algorithm
        });

        onSignatureComplete(result);
      }
    } catch (error) {
      console.error('Signing failed:', error);
    } finally {
      setSigning(false);
    }
  };

  if (helperStatus === 'checking') {
    return <LoadingSpinner message="Checking for CAC/PIV helper..." />;
  }

  if (helperStatus === 'unavailable') {
    return (
      <Card className="p-6">
        <Alert>
          <AlertDescription>
            CAC/PIV Helper application is not running. You can:
            <ul className="list-disc ml-6 mt-2">
              <li>Install and start the CAC/PIV Helper application</li>
              <li>Skip digital signature and complete authorization without signature</li>
            </ul>
          </AlertDescription>
        </Alert>
        <div className="mt-4 flex gap-3">
          <Button onClick={() => window.open('/downloads/cac-piv-helper')}>
            Download Helper App
          </Button>
          <Button variant="outline" onClick={onSkip}>
            Skip Digital Signature
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <Card className="p-6">
      <h3 className="text-lg font-semibold mb-4">Sign Authorization with CAC/PIV</h3>

      {/* Certificate Selection */}
      <div className="space-y-4">
        <Label>Select Certificate</Label>
        {certificates.map((cert) => (
          <Card
            key={cert.id}
            className={`p-4 cursor-pointer ${
              selectedCertificate?.id === cert.id ? 'border-blue-500' : ''
            }`}
            onClick={() => setSelectedCertificate(cert)}
          >
            <p className="font-semibold">{cert.commonName}</p>
            <p className="text-sm text-gray-600">{cert.email}</p>
            <p className="text-xs text-gray-500">
              Valid: {new Date(cert.notBefore).toLocaleDateString()} - {new Date(cert.notAfter).toLocaleDateString()}
            </p>
          </Card>
        ))}
      </div>

      {/* Sign Button */}
      <div className="mt-6 flex gap-3">
        <Button
          onClick={handleSign}
          disabled={!selectedCertificate || signing}
        >
          {signing ? 'Signing...' : 'Sign with CAC/PIV'}
        </Button>
        <Button variant="outline" onClick={onSkip}>
          Skip Digital Signature
        </Button>
      </div>
    </Card>
  );
}
```

**Update `authorization-wizard.tsx`**:

Add a new step after "Review":

```typescript
type Step = 'select-ssp' | 'select-sar' | 'stakeholder-info' | 'visualize' |
            'select-template' | 'fill-variables' | 'conditions' | 'review' |
            'sign'; // New step

// In wizard flow:
const handleNext = async () => {
  // ... existing code ...
  else if (step === 'review') {
    // Before showing sign step, create draft authorization
    const draftId = await createDraftAuthorization();
    setDraftAuthorizationId(draftId);
    setStep('sign');
  }
};
```

### 5. CAC/PIV Helper Application

**Technology**: Java Swing/JavaFX desktop app

**Structure**:
```
cac-piv-helper/
├── src/main/java/
│   ├── gov/nist/oscal/tools/cacpiv/
│   │   ├── HelperApplication.java      # Main entry point
│   │   ├── server/
│   │   │   ├── HttpServer.java         # Embedded HTTP server
│   │   │   ├── SignHandler.java        # /sign endpoint
│   │   │   ├── CertificatesHandler.java # /certificates endpoint
│   │   │   └── HealthHandler.java      # /health endpoint
│   │   ├── pkcs11/
│   │   │   ├── SmartCardManager.java   # PKCS#11 operations
│   │   │   ├── CertificateReader.java  # Read certificates
│   │   │   └── SignatureGenerator.java # Sign data
│   │   ├── ui/
│   │   │   ├── SystemTrayIcon.java     # System tray integration
│   │   │   ├── PinDialog.java          # PIN entry dialog
│   │   │   └── CertificateDialog.java  # Certificate selection
│   │   └── config/
│   │       └── Configuration.java      # App configuration
│   └── resources/
│       ├── pkcs11-config.txt           # PKCS#11 configuration
│       └── icon.png                    # App icon
├── pom.xml
└── README.md
```

**Key code snippets**:

```java
// PKCS#11 Smart Card Access
public class SmartCardManager {
    private Provider provider;
    private KeyStore keyStore;

    public void initialize() throws Exception {
        // Configure PKCS#11 provider
        String config = "--name=SmartCard\nlibrary=/usr/lib/opensc-pkcs11.so";
        ByteArrayInputStream configStream = new ByteArrayInputStream(config.getBytes());
        provider = new SunPKCS11(configStream);
        Security.addProvider(provider);

        // Load keystore
        keyStore = KeyStore.getInstance("PKCS11", provider);
        keyStore.load(null, null); // PIN will be prompted
    }

    public List<Certificate> getCertificates() throws Exception {
        List<Certificate> certs = new ArrayList<>();
        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = keyStore.getCertificate(alias);

            if (cert instanceof X509Certificate) {
                X509Certificate x509 = (X509Certificate) cert;
                // Filter for signature certificates
                if (isSignatureCertificate(x509)) {
                    certs.add(cert);
                }
            }
        }
        return certs;
    }

    public byte[] signData(byte[] data, String certificateAlias, char[] pin) throws Exception {
        // Load private key entry
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(certificateAlias, pin);

        // Sign data
        Signature signature = Signature.getInstance("SHA256withRSA", provider);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }
}

// HTTP Server
public class HttpServer {
    private com.sun.net.httpserver.HttpServer server;
    private SmartCardManager smartCardManager;

    public void start() throws Exception {
        smartCardManager = new SmartCardManager();
        smartCardManager.initialize();

        server = com.sun.net.httpserver.HttpServer.create(
            new InetSocketAddress(8000), 0
        );

        // Enable CORS for localhost:3000
        server.createContext("/health", new HealthHandler());
        server.createContext("/certificates", new CertificatesHandler(smartCardManager));
        server.createContext("/sign", new SignHandler(smartCardManager));

        server.setExecutor(null);
        server.start();

        System.out.println("CAC/PIV Helper running on http://localhost:8000");
    }
}

// Sign Handler
public class SignHandler implements HttpHandler {
    private SmartCardManager smartCardManager;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:3000");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            // Parse request
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            JSONObject request = new JSONObject(requestBody);
            String dataToSign = request.getString("data");
            String certificateId = request.getString("certificateId");

            // Prompt for PIN
            char[] pin = showPinDialog();

            // Sign data
            byte[] signature = smartCardManager.signData(
                Base64.getDecoder().decode(dataToSign),
                certificateId,
                pin
            );

            // Get certificate
            Certificate cert = smartCardManager.getCertificate(certificateId);

            // Build response
            JSONObject response = new JSONObject();
            response.put("signature", Base64.getEncoder().encodeToString(signature));
            response.put("certificate", Base64.getEncoder().encodeToString(cert.getEncoded()));
            response.put("algorithm", "SHA256withRSA");

            // Send response
            byte[] responseBytes = response.toString().getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.getResponseBody().close();

        } catch (Exception e) {
            // Error response
            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            byte[] errorBytes = error.toString().getBytes();
            exchange.sendResponseHeaders(500, errorBytes.length);
            exchange.getResponseBody().write(errorBytes);
            exchange.getResponseBody().close();
        }
    }
}
```

---

## Security Considerations

### 1. Transport Security

**Problem**: Helper app runs on localhost HTTP (not HTTPS).

**Mitigations**:
- ✅ Only accept connections from localhost
- ✅ Validate Origin header (localhost:3000 only)
- ✅ Use CORS to restrict access
- ✅ Consider adding local TLS with self-signed cert

### 2. PIN Handling

**Requirements**:
- ❌ Never transmit PIN over network (even localhost)
- ✅ PIN entered directly in helper app UI
- ✅ PIN passed directly to PKCS#11
- ✅ PIN not logged or stored
- ✅ Clear PIN from memory after use

### 3. Certificate Validation

**Backend must validate**:
- Certificate is not expired
- Certificate chain is valid (if configured)
- Certificate is issued by trusted CA (federal PKI root)
- Certificate has not been revoked (OCSP/CRL)
- Certificate has correct key usage (digitalSignature)

### 4. Signature Verification

**Backend must**:
- Re-compute document hash
- Verify signature matches document
- Check signature was created with private key for provided certificate
- Store verification result with timestamp

### 5. Non-Repudiation

**Store permanently**:
- Original document content (or hash)
- Digital signature
- Signer certificate
- Timestamp
- Signature algorithm

This data proves:
- Who signed (certificate CN)
- When they signed (timestamp)
- What they signed (document hash)
- Signature is valid (verification result)

### 6. Privacy

**Consider**:
- Certificate contains PII (name, email)
- Store certificate securely
- Log access to signature data
- Implement retention policies

---

## Implementation Plan

### Phase 1: Backend Foundation (2 weeks)

**Week 1**:
- [ ] Update `Authorization` entity with signature fields
- [ ] Create `DigitalSignatureService`
- [ ] Implement document hash generation
- [ ] Implement signature verification logic
- [ ] Add certificate parsing utilities

**Week 2**:
- [ ] Create `AuthorizationController` endpoints
  - `/generate-signature-data`
  - `/submit-signature`
  - `/verify-signature`
  - `/signature-details`
- [ ] Write unit tests for signature service
- [ ] Write integration tests for API endpoints
- [ ] Update database schema and migrations

### Phase 2: CAC/PIV Helper Application (3 weeks)

**Week 1: Core Functionality**
- [ ] Create Java Maven project
- [ ] Implement PKCS#11 smart card access
- [ ] Implement certificate reading
- [ ] Implement data signing
- [ ] Test with CAC/PIV test cards

**Week 2: HTTP Server**
- [ ] Implement embedded HTTP server (Jetty or HttpServer)
- [ ] Create `/health` endpoint
- [ ] Create `/certificates` endpoint
- [ ] Create `/sign` endpoint
- [ ] Implement CORS handling
- [ ] Test with curl/Postman

**Week 3: User Interface**
- [ ] Create system tray icon
- [ ] Implement PIN entry dialog
- [ ] Implement certificate selection dialog
- [ ] Add auto-start capability
- [ ] Create installer (Windows .exe, macOS .dmg)
- [ ] Write user documentation

### Phase 3: Frontend Integration (2 weeks)

**Week 1**:
- [ ] Create `DigitalSignatureStep` component
- [ ] Implement helper availability check
- [ ] Implement certificate selection UI
- [ ] Implement signing flow
- [ ] Add to authorization wizard

**Week 2**:
- [ ] Create signature status display component
- [ ] Add signature details view
- [ ] Update authorization list (show signed status)
- [ ] Update authorization detail page
- [ ] Add signature verification UI

### Phase 4: Testing & Documentation (2 weeks)

**Week 1: Testing**
- [ ] End-to-end testing with real CAC/PIV cards
- [ ] Browser compatibility testing (Chrome, Firefox, Edge)
- [ ] Error handling testing
- [ ] Performance testing
- [ ] Security review

**Week 2: Documentation**
- [ ] User installation guide
- [ ] Administrator deployment guide
- [ ] Troubleshooting guide
- [ ] API documentation
- [ ] Architecture documentation

### Phase 5: Deployment (1 week)

- [ ] Package helper application
- [ ] Create download page
- [ ] Deploy backend changes
- [ ] Deploy frontend changes
- [ ] Monitor rollout
- [ ] User support

**Total Timeline**: 10 weeks (2.5 months)

---

## User Experience Design

### Installation Experience

1. **First-time user visits authorization page**
   - See message: "CAC/PIV digital signatures are available"
   - Click "Download CAC/PIV Helper" button
   - Download appropriate installer for their OS

2. **Install helper app**
   - **Windows**: Run `cac-piv-helper-setup.exe`
   - **macOS**: Open `cac-piv-helper.dmg`, drag to Applications
   - **Linux**: `sudo dpkg -i cac-piv-helper.deb`
   - Helper app auto-starts and shows system tray icon

3. **Configure auto-start (optional)**
   - System tray icon → Settings → "Start on login"

### Signing Experience

1. **User completes authorization wizard**
   - Step 1-7: Existing flow
   - Step 8: Review
   - **Step 9: Digital Signature (NEW)**

2. **Digital signature step**
   - **If helper app is running**:
     - Show "CAC/PIV Helper connected" (green checkmark)
     - Insert CAC/PIV card prompt
     - List available signature certificates
     - Select certificate
     - Click "Sign with CAC/PIV"
     - Helper app shows PIN dialog
     - Enter PIN
     - Signature created
     - Show "Successfully signed!" with certificate details
     - Click "Complete Authorization"

   - **If helper app is NOT running**:
     - Show "CAC/PIV Helper not detected" (warning)
     - Option 1: "Download and install CAC/PIV Helper"
     - Option 2: "Skip digital signature" (authorization created without signature)

3. **Post-signature**
   - Authorization detail page shows:
     - "Digitally Signed" badge
     - Signer name (from certificate)
     - Signature timestamp
     - Certificate details (expandable)
     - "Verify Signature" button → shows verification result

### Error Handling

**Common errors and solutions**:

| Error | User Message | Solution |
|-------|--------------|----------|
| Helper not running | "CAC/PIV Helper application is not running" | "Start the helper app from Applications or download it" |
| No card inserted | "Please insert your CAC/PIV card" | Insert card, click "Retry" |
| Wrong PIN | "Incorrect PIN. X attempts remaining." | Re-enter PIN |
| Card locked | "Card is locked. Contact your security office." | Unlock card with PUK or request new card |
| Certificate expired | "Your certificate has expired" | Contact PKI administrator for new certificate |
| Signature verification failed | "Signature verification failed" | Show details, contact administrator |

---

## Testing Strategy

### Unit Tests

**Backend**:
- [ ] `DigitalSignatureService.generateDocumentHash()` - test hash consistency
- [ ] `DigitalSignatureService.verifySignature()` - test with known signatures
- [ ] `DigitalSignatureService.extractCertificateInfo()` - test cert parsing
- [ ] `DigitalSignatureService.validateCertificate()` - test validation logic

**Helper App**:
- [ ] `SmartCardManager.getCertificates()` - test with mock PKCS#11
- [ ] `SmartCardManager.signData()` - test signature generation
- [ ] `CertificateReader.parseCertificate()` - test X.509 parsing

### Integration Tests

- [ ] POST `/api/authorizations/{id}/generate-signature-data` → returns hash
- [ ] POST `/api/authorizations/{id}/submit-signature` → stores signature
- [ ] GET `/api/authorizations/{id}/verify-signature` → verifies correctly
- [ ] Helper app `/certificates` endpoint → returns certificate list
- [ ] Helper app `/sign` endpoint → returns valid signature

### End-to-End Tests

1. **Happy path**:
   - Start with CAC/PIV card inserted
   - Complete authorization wizard
   - Select certificate
   - Enter PIN
   - Sign authorization
   - Verify signature on detail page

2. **Error scenarios**:
   - No card inserted
   - Wrong PIN (1st attempt)
   - Wrong PIN (3rd attempt - card locked)
   - Certificate expired
   - Helper app not running
   - Network timeout

3. **Browser compatibility**:
   - Test on Chrome (Windows, macOS)
   - Test on Firefox (Windows, macOS)
   - Test on Edge (Windows)
   - Test on Safari (macOS)

### Security Testing

- [ ] Attempt to sign with tampered document hash
- [ ] Attempt to submit invalid signature
- [ ] Attempt to use revoked certificate
- [ ] Attempt to use expired certificate
- [ ] Attempt CSRF attack on helper app endpoints
- [ ] Attempt to connect to helper app from different origin
- [ ] Verify PIN is not logged or transmitted

### Performance Testing

- [ ] Measure signature generation time (should be < 2 seconds)
- [ ] Measure signature verification time (should be < 1 second)
- [ ] Test with large authorization documents (100+ pages)
- [ ] Test concurrent signing requests

---

## Compliance Requirements

### Federal PKI Requirements

The solution must comply with:

1. **FIPS 201** - Personal Identity Verification (PIV) of Federal Employees and Contractors
   - Use PIV authentication certificate for signing
   - Verify certificate chain to Federal PKI root

2. **NIST SP 800-63** - Digital Identity Guidelines
   - AAL3 (Authenticator Assurance Level 3) - multi-factor with hardware token

3. **NIST SP 800-53** - Security and Privacy Controls
   - IA-2 (Identification and Authentication)
   - IA-5 (Authenticator Management)
   - SC-8 (Transmission Confidentiality and Integrity)
   - SC-17 (Public Key Infrastructure Certificates)

4. **DoD PKI** (if DoD deployment)
   - DoD Root CA 3 trust anchor
   - DoD certificate policy compliance

### Electronic Signature Laws

1. **ESIGN Act** (Electronic Signatures in Global and National Commerce Act)
   - Digital signature must be:
     - Attributable to the person signing
     - Capable of verification
     - Under sole control of the signer
     - Linked to the document (detect tampering)

2. **UETA** (Uniform Electronic Transactions Act)
   - Electronic signature has same legal effect as handwritten signature
   - Record retention requirements

### Audit Requirements

Must maintain audit trail including:
- Who signed (certificate subject)
- When signed (timestamp)
- What was signed (document hash)
- Signature verification results
- Access to signed documents

### Certificate Validation

Must check:
- Certificate validity period
- Certificate revocation status (OCSP or CRL)
- Certificate chain to trusted root
- Certificate key usage (digitalSignature)
- Certificate policy (if required)

---

## Alternative Approaches

### Alternative 1: WebAuthn/FIDO2

**Description**: Use WebAuthn API with FIDO2-compliant smart cards.

**Pros**:
- ✅ Modern web standard
- ✅ No helper app needed
- ✅ Good browser support

**Cons**:
- ❌ Not all CAC/PIV cards support FIDO2
- ❌ Different from traditional PKI signatures
- ❌ May not meet compliance requirements

**Verdict**: Future consideration, not current solution.

### Alternative 2: PDF Signing Service

**Description**: Generate PDF of authorization, use Adobe Sign or similar.

**Pros**:
- ✅ Familiar PDF signing workflow
- ✅ Portable document format
- ✅ Visual signature

**Cons**:
- ❌ Requires PDF generation
- ❌ Third-party service
- ❌ Not CAC/PIV signing

**Verdict**: Could be complementary feature (export signed PDF).

### Alternative 3: Client-Side JavaScript Crypto with WebUSB

**Description**: Access smart card directly via WebUSB API.

**Pros**:
- ✅ No helper app
- ✅ Direct browser access

**Cons**:
- ❌ WebUSB doesn't support smart cards
- ❌ Requires special hardware
- ❌ Not widely supported

**Verdict**: Not viable.

### Alternative 4: Server-Side HSM

**Description**: Store user private keys in server-side HSM.

**Pros**:
- ✅ Centralized management
- ✅ No client software

**Cons**:
- ❌ **Not CAC/PIV** - defeats purpose of physical token
- ❌ Doesn't meet two-factor requirement
- ❌ High cost
- ❌ Security concerns (key escrow)

**Verdict**: Not suitable for CAC/PIV requirement.

---

## Recommendations

### Immediate Next Steps

1. **Validate Requirements** (1 week)
   - Confirm CAC/PIV requirement with stakeholders
   - Identify compliance requirements (FIPS, DoD, etc.)
   - Determine if signature is mandatory or optional
   - Identify user base (% with CAC/PIV access)

2. **Proof of Concept** (2 weeks)
   - Build simple helper app
   - Test PKCS#11 access with real cards
   - Test signing and verification
   - Validate browser integration
   - Demo to stakeholders

3. **Architecture Review** (1 week)
   - Review this design document
   - Get feedback from security team
   - Get feedback from IT deployment team
   - Finalize architecture decisions

4. **Implementation** (10 weeks)
   - Follow implementation plan above
   - Weekly demos to stakeholders
   - Iterative testing and feedback

### Success Metrics

- **Functionality**: 95%+ successful signature operations
- **Performance**: < 5 seconds total signing time
- **Usability**: < 10 minutes to install and configure helper app
- **Reliability**: 99.9% helper app uptime
- **Security**: Zero security vulnerabilities in security review
- **Compliance**: 100% compliance with federal PKI requirements

### Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Helper app installation difficult | High | Provide automated installer, clear docs, IT support |
| PKCS#11 middleware compatibility | Medium | Test with ActivClient, OpenSC, others; document supported middleware |
| User resistance to helper app | Medium | Make signature optional; provide clear value proposition |
| Certificate validation complexity | High | Use well-tested libraries; implement comprehensive error handling |
| Browser CORS restrictions | Low | Documented localhost CORS exception; provide fallback |
| Smart card reader issues | Medium | Provide troubleshooting guide; support multiple reader types |

---

## Conclusion

The **Server-Side Helper Application** approach provides the best balance of:
- **Functionality**: Full CAC/PIV smart card access via PKCS#11
- **Security**: Private keys never leave the card; strong cryptographic signatures
- **Usability**: Works on all browsers; reasonable installation burden
- **Compliance**: Meets federal PKI and electronic signature requirements
- **Maintainability**: Standard Java app; easier to update than browser extensions

This approach is **recommended for implementation** to enable CAC/PIV digital signatures in the authorization workflow.

The implementation will take approximately **10 weeks** with a team of 2-3 developers, following the phased approach outlined in this document.

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

**Example PIV Certificate Subject**:
```
CN=Last.First.Middle.1234567890
OU=People
OU=Department of Example
O=U.S. Government
C=US
```

## Appendix B: PKCS#11 Configuration

**SunPKCS11 Configuration** (for ActivClient on Windows):
```
name = ActivClient
library = C:\Program Files\ActivIdentity\ActivClient\acpkcs211.dll
```

**SunPKCS11 Configuration** (for OpenSC on macOS):
```
name = OpenSC
library = /Library/OpenSC/lib/opensc-pkcs11.so
```

**SunPKCS11 Configuration** (for OpenSC on Linux):
```
name = OpenSC
library = /usr/lib/x86_64-linux-gnu/opensc-pkcs11.so
```

## Appendix C: Signature Format

**Signed Authorization Document Structure**:

```json
{
  "authorization": {
    "id": 123,
    "name": "Production System Authorization",
    "sspItemId": "ssp-001",
    "completedContent": "...",
    "authorizedBy": "john.doe",
    "authorizedAt": "2025-10-25T14:30:00Z"
  },
  "digitalSignature": {
    "signature": "BASE64_ENCODED_SIGNATURE",
    "algorithm": "SHA256withRSA",
    "certificate": "BASE64_ENCODED_X509_CERT",
    "timestamp": "2025-10-25T14:30:05Z",
    "documentHash": "SHA256_HASH_OF_AUTHORIZATION"
  }
}
```

**Document Hash Calculation**:
```java
// Canonical representation of authorization (JSON)
String canonicalJson = createCanonicalJson(authorization);

// SHA-256 hash
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
String hashBase64 = Base64.getEncoder().encodeToString(hash);
```

**Signature Verification**:
```java
// 1. Parse certificate
X509Certificate cert = parseCertificate(signatureData.getCertificate());

// 2. Get public key
PublicKey publicKey = cert.getPublicKey();

// 3. Verify signature
Signature signature = Signature.getInstance("SHA256withRSA");
signature.initVerify(publicKey);
signature.update(documentHash.getBytes());
boolean verified = signature.verify(Base64.getDecoder().decode(signatureData.getSignature()));
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-25 | System Architect | Initial design document |

---

**End of Document**
