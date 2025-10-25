# Digital Signature User Guide

**Last Updated**: October 25, 2025
**Version**: 1.0
**Audience**: System Authorizers, Security Managers, IT Administrators

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Getting Started](#getting-started)
4. [Signing an Authorization](#signing-an-authorization)
5. [Viewing Signed Authorizations](#viewing-signed-authorizations)
6. [Verifying Signatures](#verifying-signatures)
7. [Troubleshooting](#troubleshooting)
8. [Administrator Configuration](#administrator-configuration)
9. [Frequently Asked Questions](#frequently-asked-questions)

---

## Overview

### What are Digital Signatures?

Digital signatures provide **cryptographic proof** that a specific person authorized a system at a specific time. Unlike a simple username, a digital signature:

✅ **Proves Identity** - Uses your CAC/PIV card certificate
✅ **Prevents Tampering** - Any changes to the document invalidate the signature
✅ **Provides Non-Repudiation** - You cannot deny signing the document
✅ **Meets Compliance** - Satisfies FISMA, NIST SP 800-53, DoD PKI requirements

### Why Digital Signatures?

When you sign a system authorization digitally:
- Your **name, EDIPI, and certificate** are permanently recorded
- The **document content** is cryptographically linked to your signature
- **Auditors can verify** the signature was valid at signing time
- **Legally binding** under ESIGN Act and UETA

### When to Use Digital Signatures

✅ **Required for**:
- Final system authorization decisions
- Authority to Operate (ATO) approvals
- Conditional authorization approvals
- Interim Authority to Test (IATT) approvals

⚠️ **Optional for**:
- Draft authorizations
- Working copies
- Review versions

---

## Prerequisites

### What You Need

Before you can digitally sign authorizations, you must have:

#### 1. CAC or PIV Card

- **DoD users**: Common Access Card (CAC)
- **Federal civilian users**: Personal Identity Verification (PIV) card
- **Contractor users**: CAC or PIV depending on sponsor

Your card must:
- ✅ Be **current and valid** (not expired)
- ✅ Have a **signature certificate** (standard on CAC/PIV)
- ✅ Have a **working PIN** (not locked)

#### 2. Smart Card Reader

- **USB card reader** connected to your computer
- **Reader drivers** installed (usually automatic on Windows)
- **Card properly inserted** (chip side up, facing forward)

#### 3. Middleware Software

**DoD/Military users**: ActivClient (by HID Global)
- Pre-installed on most DoD computers
- Available from your IT department
- Look for ActivClient icon in system tray (Windows taskbar)

**Federal civilian users**: ActivClient or OpenSC
- Check with your IT department for approved software
- OpenSC is common for macOS/Linux users

#### 4. Supported Browser

**Fully Supported**:
- ✅ **Google Chrome** (Windows) - Recommended for most users
- ✅ **Microsoft Edge** (Windows) - Recommended for most users
- ✅ **Mozilla Firefox** (Windows/macOS/Linux) - Best for advanced users

**Limited Support**:
- ⚠️ Safari (macOS) - May require additional configuration

#### 5. HTTPS Configuration

Your administrator must configure:
- HTTPS/TLS enabled on the server
- Client certificate authentication enabled
- Proper certificate trust store configuration

---

## Getting Started

### Step 1: Verify Your Setup

Before attempting to sign an authorization, verify your setup:

#### Check Your Card

1. **Insert your CAC/PIV card** into the card reader
2. **Wait for the reader light** to indicate card is detected
3. **Check card expiration date** (printed on card)

#### Check Middleware

**Windows (ActivClient)**:
1. Look for **ActivClient icon** in system tray (bottom-right corner)
2. Right-click icon → **Status**
3. Should show "**Card Present**" and "**Middleware Ready**"

**If ActivClient is not running**:
- Start → All Programs → ActivIdentity → ActivClient
- Or search for "ActivClient" in Windows search

**macOS/Linux (OpenSC)**:
```bash
# Check if OpenSC is installed
pkcs11-tool --version

# List certificates on card
pkcs11-tool --list-objects --type cert
```

#### Check Browser Configuration

**Chrome/Edge**:
- No configuration needed if ActivClient is running
- Certificates automatically available from Windows Certificate Store

**Firefox**:
1. Open Firefox
2. Menu → **Settings** → **Privacy & Security**
3. Scroll to **Security Devices**
4. Should see "**ActivClient**" or "**OpenSC**" in the list

**If not configured**:
1. Click **Load**
2. Module Name: `ActivClient`
3. Module filename:
   - Windows: `C:\Program Files\ActivIdentity\ActivClient\acpkcs211.dll`
   - macOS: `/Library/OpenSC/lib/opensc-pkcs11.so`
   - Linux: `/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so`
4. Click **OK**

### Step 2: Test Certificate Access

Before signing a real authorization, test that your browser can access certificates:

1. **Visit the application**: `https://localhost:8443` (or your configured URL)
2. **Log in** with your username and password
3. **Navigate to**: Profile or Settings
4. Look for "**Test CAC/PIV Access**" button (if available)
5. Click the button
6. **Browser should prompt** for certificate selection
7. **Select your certificate** (look for your name + "Signature")
8. **Enter your PIN** when ActivClient prompts
9. Should see "**Success**" message

If this works, you're ready to sign authorizations!

---

## Signing an Authorization

### Complete Authorization Workflow

Signing is the **final step** in creating a system authorization. Follow this complete workflow:

### Step 1: Navigate to Authorizations

1. Log in to the OSCAL Tools application
2. Click **"Authorizations"** tile on the dashboard
3. Click **"Create New Authorization"** button

### Step 2: Complete Authorization Wizard

Follow the 8-step wizard to build your authorization:

#### Step 1: Select System Security Plan (SSP)
- Choose the SSP for the system being authorized
- SSPs must be uploaded first via the Validate page
- Click on the SSP card to select it
- Click **"Next"**

#### Step 2: Select Security Assessment Report (SAR) - Optional
- Choose the SAR if available
- SARs provide assessment results for the system
- Can skip this step if no SAR available
- Click **"Next"**

#### Step 3: Authorization Details
- **Authorization Title**: Descriptive name (e.g., "Payroll System Production ATO")
- **Date Authorized**: Date of authorization decision (usually today)
- **Date Expired**: Expiration date (typically 3 years from authorization)
- **System Owner**: Name of the Information System Owner
- **Security Manager**: Name of the Information System Security Manager (ISSM)
- **Authorizing Official**: Name of the person who will sign (usually you)
- Click **"Next"**

#### Step 4: Visualize System
- Review SSP and SAR visualizations
- Check system information, controls, boundaries
- Use this to inform your authorization decision
- Click **"Next"**

#### Step 5: Select Template
- Choose an authorization template
- Templates contain standard authorization language with variables
- Preview shows what variables need to be filled
- Click **"Next"**

#### Step 6: Fill Variables
- The template editor shows on the left
- You can edit the template content directly
- Fill in all required variables (shown in the form on the right)
- Live preview shows the completed document
- Variables turn green when filled
- Progress bar shows completion percentage
- Click **"Next"** when all variables are filled

#### Step 7: Conditions of Approval
- Add any mandatory or recommended conditions
- **Mandatory conditions** require due dates
- **Recommended conditions** are optional
- Common conditions:
  - "Complete quarterly security reviews"
  - "Implement continuous monitoring"
  - "Remediate high/critical vulnerabilities within 30 days"
- Click **"Add Condition"** for each condition
- Click **"Next"**

#### Step 8: Review
- Review all authorization details
- Check dates, stakeholders, conditions
- Preview the completed authorization document
- Click **"Next"** to proceed to signing

### Step 9: Digital Signature ⭐ **NEW**

This is where you digitally sign the authorization with your CAC/PIV card.

#### Before Signing - Checklist

✅ **Card inserted** in reader
✅ **ActivClient running** (system tray icon visible)
✅ **PIN ready** (know your card PIN)
✅ **Authorization reviewed** (content is final)

#### Signing Process

1. **Review the instructions** on the signing page

2. **Click "Sign with CAC/PIV"** button

3. **Certificate Selection Dialog Appears**
   - This is your browser's native dialog
   - Shows list of certificates from your card
   - Look for certificate with:
     - Your name (e.g., "DOE.JOHN.M.1234567890")
     - Type: "Digital Signature" or "Signature"

   ![Certificate Selection](./images/cert-selection.png)

4. **Select Your Signature Certificate**
   - Click on the certificate with your name
   - Make sure it's the "Signature" certificate (not "Authentication" or "Encryption")
   - Click **"OK"** or **"Select"**

5. **PIN Prompt Appears**
   - ActivClient will show a PIN dialog
   - This is a **secure prompt** - your PIN never goes to the browser

   ![PIN Prompt](./images/pin-prompt.png)

6. **Enter Your PIN**
   - Type your CAC/PIV PIN carefully
   - Check **CAPS LOCK** is off
   - Click **"OK"**

7. **Signing in Progress**
   - Browser communicates with ActivClient
   - ActivClient uses your card to create the signature
   - Takes 5-10 seconds

8. **Success!**
   - See confirmation message: "Authorization signed by [Your Name]"
   - Your signature details are displayed:
     - Signer name
     - EDIPI
     - Timestamp
   - Click **"Complete Authorization"**

#### If You Want to Skip Signing

You can click **"Skip Signature"** to create the authorization without a digital signature:

⚠️ **Note**: Skipping creates an unsigned authorization:
- No cryptographic proof of authorization
- May not meet compliance requirements
- Can potentially add signature later (future feature)

**When to skip**:
- Creating a draft for review
- Technical issues with card/middleware
- Will sign later with proper card

---

## Viewing Signed Authorizations

### Authorization List

1. Navigate to **Authorizations** page
2. Signed authorizations show a **green badge**: "Digitally Signed"
3. Unsigned authorizations show no badge or "Unsigned" badge

### Authorization Detail Page

Click on any authorization to view details:

#### Signature Information Section

For digitally signed authorizations, you'll see:

**Digital Signature Badge** (Green)
- ✅ **Digitally Signed**
- ✅ **Verified** (if certificate validation passed)

**Signer Information**:
- **Name**: Common Name from certificate (e.g., "DOE.JOHN.M.1234567890")
- **Email**: Email from certificate (if present)
- **EDIPI**: 10-digit DoD ID number
- **Signed**: Date and time of signature

**Certificate Details** (Expandable):
- **Issuer**: Certificate authority that issued the certificate
- **Serial Number**: Unique certificate identifier
- **Valid From**: Certificate start date
- **Valid Until**: Certificate expiration date
- **Last Verified**: Last time signature was verified
- **Verification Notes**: Any warnings or validation messages

**Actions**:
- **Re-verify Signature** button: Checks if certificate is still valid

---

## Verifying Signatures

### Automatic Verification

When an authorization is signed:
- Certificate is **automatically validated** at signing time
- Checks include:
  - ✅ Certificate not expired
  - ✅ Certificate has digitalSignature key usage
  - ✅ Certificate issuer recognized (DoD/Federal PKI)
  - ✅ Certificate format valid (CAC/PIV format)

### Manual Re-Verification

You can re-verify any signature at any time:

1. **Open the authorization** detail page
2. Scroll to **"Digital Signature"** section
3. Click **"Re-verify Signature"** button
4. System performs validation again
5. See result:
   - ✅ **"Signature verified successfully"** - Certificate still valid
   - ❌ **"Signature verification failed"** - Certificate expired or revoked

### Why Re-Verify?

Re-verification is useful for:
- **Audit compliance**: Verify signature before presenting to auditors
- **Certificate expiration**: Check if signer's certificate expired after signing
- **Policy changes**: Verify against updated certificate policies
- **Due diligence**: Periodic verification for active authorizations

### What Verification Checks

**Certificate Validity**:
- Not expired (current date between notBefore and notAfter)
- Has digitalSignature key usage extension
- Issued by recognized authority

**Note**: Optional advanced checks (if configured):
- Certificate revocation status (OCSP/CRL)
- Certificate chain validation to trusted root
- Certificate policy compliance

---

## Troubleshooting

### Problem: "No certificate selected" Error

**Symptom**: Error message after clicking "Sign with CAC/PIV"

**Possible Causes**:
- Certificate selection dialog cancelled
- No certificate selected in dialog

**Solutions**:
1. Try again - Click "Sign with CAC/PIV" again
2. When dialog appears, make sure to select a certificate
3. Look for certificate with your name and "Signature" type
4. Click OK/Select button in dialog

---

### Problem: Certificate Selection Dialog Doesn't Appear

**Symptom**: Nothing happens when clicking "Sign with CAC/PIV"

**Possible Causes**:
1. Browser cache issue
2. HTTPS not configured
3. Popup blocker

**Solutions**:

**Try a hard refresh**:
- **Windows**: Ctrl + Shift + R
- **macOS**: Cmd + Shift + R

**Clear browser cache**:
1. Chrome/Edge: Settings → Privacy → Clear browsing data
2. Firefox: Settings → Privacy & Security → Clear Data

**Check popup blocker**:
- Make sure browser isn't blocking the certificate dialog
- Check for blocked popup icon in address bar

**Try incognito/private mode**:
- This bypasses cache and extensions
- If it works, issue is with browser cache/extensions

**Check console for errors**:
1. Press F12 to open Developer Tools
2. Go to Console tab
3. Look for errors when clicking "Sign" button
4. Share errors with IT support if present

---

### Problem: "No certificates found" in Selection Dialog

**Symptom**: Certificate dialog appears but is empty or shows no certificates

**Possible Causes**:
1. CAC/PIV card not inserted
2. ActivClient not running
3. Card reader not connected
4. Browser not configured

**Solutions**:

**Check card**:
1. Remove and re-insert card
2. Wait for reader light to indicate card detected
3. Check card is inserted fully (chip side up, forward)

**Check ActivClient** (Windows):
1. Look for ActivClient icon in system tray
2. If not visible:
   - Start → Search → "ActivClient"
   - Launch ActivClient
   - Wait for it to detect card
3. Right-click icon → Status → Should show "Card Present"

**Restart card services** (Windows):
1. Right-click ActivClient icon → Exit
2. Remove card from reader
3. Open Task Manager (Ctrl+Shift+Esc)
4. End any "ActivClient" processes
5. Start ActivClient again
6. Insert card

**Check Firefox configuration**:
1. Firefox → Settings → Privacy & Security
2. Scroll to Security Devices
3. Verify "ActivClient" module is loaded
4. If not, follow [Firefox configuration steps](#check-browser-configuration)

**Check Chrome/Edge** (Windows):
1. Open Windows Certificate Manager:
   - Win + R
   - Type: `certmgr.msc`
   - Press Enter
2. Navigate: Personal → Certificates
3. Should see your CAC/PIV certificates
4. If not present, ActivClient may need reinstall

---

### Problem: "Incorrect PIN" Error

**Symptom**: ActivClient shows "Incorrect PIN" message

**Possible Causes**:
1. Wrong PIN entered
2. CAPS LOCK on
3. NumLock off (if using keypad)
4. Too many failed attempts

**Solutions**:

**Try again carefully**:
1. Check CAPS LOCK is **OFF**
2. Check NumLock is **ON** (if using numeric keypad)
3. Type PIN slowly and carefully
4. Click OK

**Check PIN attempts remaining**:
- ActivClient usually shows attempts remaining
- Typically: 3 attempts for PIN, then card locks

**If card is locked**:
- ❌ **Do not try more PINs** - you'll permanently lock the card
- Contact your security office or IT help desk
- May need:
  - PUK (PIN Unlock Key) to unlock
  - New card if PUK attempts exhausted
  - Visit RAPIDS site for new card (DoD)

**Forgot your PIN**:
- Contact your security office immediately
- Will need to reset PIN or get new card
- Process varies by organization

---

### Problem: "Certificate has expired" Error

**Symptom**: Error message that certificate is expired

**Possible Causes**:
1. Certificate validity period ended
2. Card expired
3. Certificate not renewed

**Solutions**:

**Check certificate expiration**:
1. Physical card has expiration date printed
2. Or check in ActivClient:
   - Right-click icon → Advanced Diagnostics
   - Certificates tab → View certificate
   - Check "Valid to" date

**If expired**:
- **DoD/Military**:
  - Visit nearest RAPIDS site for new CAC
  - Bring 2 forms of ID
  - Usually same-day service
- **Federal civilian**:
  - Contact your PIV issuing office
  - May be able to renew online depending on agency
- **Contractors**:
  - Contact your sponsoring organization
  - May need sponsor approval for renewal

**If not expired but still showing error**:
- System clock may be wrong
- Check computer date/time is correct
- Sync with time server

---

### Problem: Signature Verification Fails Later

**Symptom**: Authorization was signed successfully, but later verification fails

**Possible Causes**:
1. Certificate expired after signing (normal)
2. Certificate revoked
3. Document was modified
4. System issue

**Solutions**:

**Check verification notes**:
- Verification notes field shows specific reason
- Common reasons:
  - "Certificate has expired" - Normal if signed long ago
  - "Certificate revoked" - Contact PKI administrator
  - "Hash mismatch" - Document may be corrupted

**Certificate expired after signing**:
- ✅ **This is normal and expected**
- The signature is still valid
- What matters: Certificate was valid **at time of signing**
- Signature timestamp proves when it was signed

**Contact administrator if**:
- Document appears corrupted
- Suspicious verification failures
- Need audit proof of signature validity

---

### Problem: Browser Compatibility Issues

**Chrome/Edge Issues**:
- Usually works reliably on Windows with ActivClient
- If not: Check Windows Certificate Store (certmgr.msc)
- Update Chrome/Edge to latest version

**Firefox Issues**:
- Check PKCS#11 module loaded (Security Devices)
- Try removing and re-adding ActivClient module
- Update Firefox to latest version

**Safari Issues** (macOS):
- Limited support for smart cards
- Recommend using Firefox on macOS instead
- May need additional Keychain configuration

---

## Administrator Configuration

### Server Requirements

Administrators must configure the server for TLS client certificate authentication:

#### 1. HTTPS/TLS Configuration

**application.yml** (Spring Boot):
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    client-auth: want  # Request but don't require client cert
```

#### 2. Certificate Trust Store (Optional)

To validate against specific trusted CAs:

**Configure trust store**:
```yaml
server:
  ssl:
    trust-store: classpath:truststore.jks
    trust-store-password: ${TRUSTSTORE_PASSWORD}
```

**Import DoD/Federal PKI roots**:
```bash
# Import DoD Root CA 3
keytool -import -trustcacerts -alias dodroot3 \
  -file DOD_ROOT_CA_3.cer \
  -keystore truststore.jks

# Import other intermediate CAs as needed
```

#### 3. CORS Configuration

If frontend is on different domain:

```java
@Configuration
public class SecurityConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("https://your-frontend-domain.mil");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
```

#### 4. Certificate Validation Options

Configure certificate validation strictness:

**Strict validation** (Production):
```java
@Configuration
public class DigitalSignatureConfig {
    @Value("${signature.validate.expiry:true}")
    private boolean validateExpiry;

    @Value("${signature.validate.keyusage:true}")
    private boolean validateKeyUsage;

    @Value("${signature.validate.issuer:true}")
    private boolean validateIssuer;

    @Value("${signature.validate.revocation:false}")
    private boolean checkRevocation;
}
```

**application.yml**:
```yaml
signature:
  validate:
    expiry: true
    keyusage: true
    issuer: true
    revocation: false  # Enable if OCSP/CRL configured

  trusted-issuers:
    - "CN=DoD Root CA 3"
    - "CN=Federal Common Policy CA"
```

### Deployment Considerations

#### Network Requirements

- **Port 8443** (or configured port) must be accessible
- **HTTPS** required for client certificate authentication
- **Firewall rules** allow TLS with client certificates

#### Performance

- Certificate validation: ~100ms per signature
- No impact on normal operations
- Minimal storage: ~5KB per signature (certificate + metadata)

#### Backup and Recovery

**Database includes signature data**:
- Regular database backups include signatures
- Restore process preserves signature integrity
- Document hashes remain valid after restore

#### Monitoring

**Key metrics to monitor**:
- Signature success rate (target: >95%)
- Certificate validation failures
- Certificate expiration warnings
- Signature endpoint response time

**Example monitoring query**:
```sql
-- Signatures created in last 24 hours
SELECT COUNT(*)
FROM authorization
WHERE signature_timestamp > NOW() - INTERVAL '24 hours';

-- Failed validations
SELECT COUNT(*)
FROM authorization
WHERE certificate_verified = false
  AND signer_certificate IS NOT NULL;
```

---

## Frequently Asked Questions

### General Questions

**Q: Do I need special software to sign authorizations?**

A: No custom software needed! If you can already use your CAC/PIV for email or login, you can sign authorizations. You just need:
- Your CAC/PIV card and reader
- ActivClient (already on DoD computers) or OpenSC
- Supported web browser

**Q: Is digital signature optional or required?**

A: Currently **optional**, but highly recommended for:
- Production system authorizations
- Authority to Operate (ATO) decisions
- Compliance requirements
- Audit purposes

Check with your organization's policy on requirements.

**Q: Can I sign from a home computer?**

A: Yes, if:
- You have your CAC/PIV card and reader at home
- You have ActivClient or OpenSC installed
- You can access the application (VPN if required)
- Your card is current and not expired

**Q: What if I'm not the Authorizing Official?**

A: The person listed as "Authorizing Official" in Step 3 should be the one signing. If that's not you:
- Don't sign the authorization
- Forward it to the correct Authorizing Official
- They will sign using their own CAC/PIV card

---

### Security Questions

**Q: Is my PIN safe when I enter it?**

A: ✅ **Yes, completely safe**:
- PIN prompt comes from ActivClient, not the web browser
- PIN never transmitted over network
- PIN never stored anywhere
- PIN goes directly to your smart card
- Private key never leaves the card

**Q: What if someone steals my card?**

A: They still need your PIN to sign. Without the PIN:
- Cannot access the card
- Cannot create signatures
- Card locks after 3 wrong PIN attempts

**If your card is lost/stolen**:
1. Report it immediately to your security office
2. Card will be revoked
3. Get a new card issued

**Q: Can someone fake my signature?**

A: ❌ **No, not possible**:
- Private key is on your card only
- Cannot be extracted or copied
- Certificate is unique to you
- Cryptographically impossible to forge

---

### Technical Questions

**Q: What browsers are supported?**

A:
- ✅ **Chrome** (Windows) - Fully supported
- ✅ **Microsoft Edge** (Windows) - Fully supported
- ✅ **Firefox** (Windows/macOS/Linux) - Fully supported, best for advanced users
- ⚠️ **Safari** (macOS) - Limited support, use Firefox instead

**Q: Does this work on Mac or Linux?**

A: Yes, with **Firefox + OpenSC**:
- Install OpenSC smart card middleware
- Configure Firefox PKCS#11 module
- Works same as Windows once configured

**Q: What if my certificate expires?**

A: **After signing**: Signatures remain valid even after certificate expires. What matters is the certificate was valid at time of signing.

**Before signing**: Get a new certificate/card before attempting to sign.

**Q: Can I sign offline?**

A: No, must be online:
- Browser needs to communicate with server
- Server validates and stores signature
- Network connection required

---

### Troubleshooting Questions

**Q: What if ActivClient isn't working?**

A: Try this sequence:
1. Right-click ActivClient icon → Exit
2. Remove CAC/PIV card
3. Close browser
4. Start ActivClient again
5. Insert card
6. Open browser
7. Try signing again

If still not working: Contact IT help desk.

**Q: Certificate dialog is empty, what do I do?**

A: Checklist:
1. ✅ Card inserted and detected?
2. ✅ ActivClient running? (system tray icon)
3. ✅ ActivClient shows "Card Present"?
4. ✅ Browser configured for smart cards?

Try:
- Restart ActivClient
- Restart browser
- Check [Browser Configuration](#check-browser-configuration) section

**Q: I entered wrong PIN, now what?**

A: You have **3 attempts total**. After 3 wrong attempts, card locks.

**If locked**: Don't try more attempts. Contact security office for:
- PUK (PIN Unlock Key) to unlock card
- Or new card if PUK exhausted

**Q: Error says "Certificate validation failed", why?**

A: Check the error message for details:
- **"Certificate has expired"** - Need new card
- **"No digitalSignature key usage"** - Wrong certificate type selected
- **"Issuer not recognized"** - May need administrator configuration

Contact administrator if unsure.

---

### Process Questions

**Q: Can I edit an authorization after signing?**

A: **Current behavior**: Editing changes are allowed, but signature remains from original version.

**Best practice**:
- Don't edit after signing
- Create new authorization if changes needed
- Original signed version preserved for audit

**Q: Can I remove a signature?**

A: No, signatures are permanent. This ensures:
- Audit trail integrity
- Non-repudiation
- Compliance with regulations

**Q: What if I need to "un-sign" something?**

A: You cannot un-sign. Instead:
- Create a **new authorization** with corrections
- Original signed authorization remains in history
- Can add notes explaining why new version needed

**Q: Can someone else add their signature too?**

A: **Future feature**: Multiple signatures (endorsements)

**Current**: One signature per authorization (the Authorizing Official)

---

## Additional Resources

### Getting Help

**IT Support**:
- For card reader, ActivClient, or browser issues
- PIN resets or card replacements
- Software installation problems

**Application Support**:
- For questions about the authorization workflow
- Signing process issues
- Verification questions

**Security Office**:
- For card issuance and renewal
- Certificate problems
- Policy questions about signature requirements

### Related Documentation

- [Authorization Feature Guide](./AUTHORIZATION-FEATURE-SUMMARY.md)
- [CAC/PIV Technical Design](./CAC-PIV-SIGNATURE-DESIGN.md)
- [OSCAL Tools User Guide](../USER_GUIDE.md)

### External Resources

- [ActivClient Documentation](https://www.hidglobal.com/support/technical-support)
- [DoD PKI Resources](https://public.cyber.mil/pki-pke/)
- [NIST PIV Resources](https://csrc.nist.gov/projects/piv)
- [OpenSC Wiki](https://github.com/OpenSC/OpenSC/wiki)

---

## Appendix: Certificate Examples

### What a CAC Certificate Looks Like

When you select a certificate, you'll see something like:

```
Subject: CN=DOE.JOHN.MIDDLE.1234567890, OU=CONTRACTOR, OU=PKI, OU=DOD, O=U.S. Government, C=US
Issuer: CN=DOD ID CA-59, OU=PKI, OU=DoD, O=U.S. Government, C=US
Serial Number: 1234567890ABCDEF
Valid from: 01/15/2024
Valid to: 01/15/2027
Purpose: Digital Signature
```

**What to look for**:
- ✅ **Your name** in the CN (Common Name) field
- ✅ **10-digit EDIPI** at end of CN
- ✅ **"Digital Signature"** in purpose/key usage
- ✅ **Valid dates** include today's date

### PIV Certificate Example

```
Subject: CN=Last.First.Middle.1234567890, OU=People, OU=Department of Example, O=U.S. Government, C=US
Issuer: CN=Federal Common Policy CA
Serial Number: 0987654321FEDCBA
Valid from: 06/01/2024
Valid to: 06/01/2027
Purpose: Digital Signature
```

---

**End of User Guide**

**Questions?** Contact your IT support or security office.

**Document Version**: 1.0
**Last Updated**: October 25, 2025
**Maintained by**: OSCAL Tools Team
