package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CertificateInfoTest {

    @Test
    void testNoArgsConstructor() {
        CertificateInfo info = new CertificateInfo();

        assertNotNull(info);
        assertNull(info.getCommonName());
        assertNull(info.getEmail());
        assertNull(info.getEdipi());
        assertNull(info.getSubjectDN());
        assertNull(info.getIssuerDN());
        assertNull(info.getSerialNumber());
        assertNull(info.getNotBefore());
        assertNull(info.getNotAfter());
    }

    @Test
    void testBuilderWithAllFields() {
        String commonName = "John Doe";
        String email = "john.doe@example.com";
        String edipi = "1234567890";
        String subjectDN = "CN=John Doe,OU=Engineering,O=Example Corp,C=US";
        String issuerDN = "CN=Example CA,O=Example Corp,C=US";
        String serialNumber = "1A:2B:3C:4D:5E:6F";
        Date notBefore = new Date(System.currentTimeMillis() - 86400000); // Yesterday
        Date notAfter = new Date(System.currentTimeMillis() + 31536000000L); // +1 year

        CertificateInfo info = CertificateInfo.builder()
                .commonName(commonName)
                .email(email)
                .edipi(edipi)
                .subjectDN(subjectDN)
                .issuerDN(issuerDN)
                .serialNumber(serialNumber)
                .notBefore(notBefore)
                .notAfter(notAfter)
                .build();

        assertNotNull(info);
        assertEquals(commonName, info.getCommonName());
        assertEquals(email, info.getEmail());
        assertEquals(edipi, info.getEdipi());
        assertEquals(subjectDN, info.getSubjectDN());
        assertEquals(issuerDN, info.getIssuerDN());
        assertEquals(serialNumber, info.getSerialNumber());
        assertEquals(notBefore, info.getNotBefore());
        assertEquals(notAfter, info.getNotAfter());
    }

    @Test
    void testBuilderWithMinimalFields() {
        String commonName = "Jane Smith";
        String subjectDN = "CN=Jane Smith,O=Example,C=US";

        CertificateInfo info = CertificateInfo.builder()
                .commonName(commonName)
                .subjectDN(subjectDN)
                .build();

        assertNotNull(info);
        assertEquals(commonName, info.getCommonName());
        assertEquals(subjectDN, info.getSubjectDN());
        assertNull(info.getEmail());
        assertNull(info.getEdipi());
        assertNull(info.getIssuerDN());
        assertNull(info.getSerialNumber());
        assertNull(info.getNotBefore());
        assertNull(info.getNotAfter());
    }

    @Test
    void testSetCommonName() {
        CertificateInfo info = new CertificateInfo();
        String commonName = "Test User";

        info.setCommonName(commonName);

        assertEquals(commonName, info.getCommonName());
    }

    @Test
    void testSetEmail() {
        CertificateInfo info = new CertificateInfo();
        String email = "test@example.com";

        info.setEmail(email);

        assertEquals(email, info.getEmail());
    }

    @Test
    void testSetEdipi() {
        CertificateInfo info = new CertificateInfo();
        String edipi = "9876543210";

        info.setEdipi(edipi);

        assertEquals(edipi, info.getEdipi());
    }

    @Test
    void testSetSubjectDN() {
        CertificateInfo info = new CertificateInfo();
        String subjectDN = "CN=Test,OU=Unit,O=Org,C=US";

        info.setSubjectDN(subjectDN);

        assertEquals(subjectDN, info.getSubjectDN());
    }

    @Test
    void testSetIssuerDN() {
        CertificateInfo info = new CertificateInfo();
        String issuerDN = "CN=CA,O=Trust,C=US";

        info.setIssuerDN(issuerDN);

        assertEquals(issuerDN, info.getIssuerDN());
    }

    @Test
    void testSetSerialNumber() {
        CertificateInfo info = new CertificateInfo();
        String serialNumber = "FF:EE:DD:CC:BB:AA";

        info.setSerialNumber(serialNumber);

        assertEquals(serialNumber, info.getSerialNumber());
    }

    @Test
    void testSetNotBefore() {
        CertificateInfo info = new CertificateInfo();
        Date notBefore = new Date();

        info.setNotBefore(notBefore);

        assertEquals(notBefore, info.getNotBefore());
    }

    @Test
    void testSetNotAfter() {
        CertificateInfo info = new CertificateInfo();
        Date notAfter = new Date();

        info.setNotAfter(notAfter);

        assertEquals(notAfter, info.getNotAfter());
    }

    @Test
    void testSetAllFieldsToNull() {
        CertificateInfo info = CertificateInfo.builder()
                .commonName("Test")
                .email("test@example.com")
                .edipi("123")
                .subjectDN("CN=Test")
                .issuerDN("CN=CA")
                .serialNumber("ABC")
                .notBefore(new Date())
                .notAfter(new Date())
                .build();

        info.setCommonName(null);
        info.setEmail(null);
        info.setEdipi(null);
        info.setSubjectDN(null);
        info.setIssuerDN(null);
        info.setSerialNumber(null);
        info.setNotBefore(null);
        info.setNotAfter(null);

        assertNull(info.getCommonName());
        assertNull(info.getEmail());
        assertNull(info.getEdipi());
        assertNull(info.getSubjectDN());
        assertNull(info.getIssuerDN());
        assertNull(info.getSerialNumber());
        assertNull(info.getNotBefore());
        assertNull(info.getNotAfter());
    }

    @Test
    void testBuilderWithNullValues() {
        CertificateInfo info = CertificateInfo.builder()
                .commonName(null)
                .email(null)
                .edipi(null)
                .subjectDN(null)
                .issuerDN(null)
                .serialNumber(null)
                .notBefore(null)
                .notAfter(null)
                .build();

        assertNotNull(info);
        assertNull(info.getCommonName());
        assertNull(info.getEmail());
        assertNull(info.getEdipi());
        assertNull(info.getSubjectDN());
        assertNull(info.getIssuerDN());
        assertNull(info.getSerialNumber());
        assertNull(info.getNotBefore());
        assertNull(info.getNotAfter());
    }

    @Test
    void testBuilderWithEmptyStrings() {
        CertificateInfo info = CertificateInfo.builder()
                .commonName("")
                .email("")
                .edipi("")
                .subjectDN("")
                .issuerDN("")
                .serialNumber("")
                .build();

        assertNotNull(info);
        assertEquals("", info.getCommonName());
        assertEquals("", info.getEmail());
        assertEquals("", info.getEdipi());
        assertEquals("", info.getSubjectDN());
        assertEquals("", info.getIssuerDN());
        assertEquals("", info.getSerialNumber());
    }

    @Test
    void testDateRangeValid() {
        Date now = new Date();
        Date past = new Date(now.getTime() - 86400000); // 1 day ago
        Date future = new Date(now.getTime() + 86400000); // 1 day from now

        CertificateInfo info = CertificateInfo.builder()
                .notBefore(past)
                .notAfter(future)
                .build();

        assertNotNull(info);
        assertTrue(info.getNotBefore().before(info.getNotAfter()));
    }

    @Test
    void testDateRangeInverted() {
        Date now = new Date();
        Date past = new Date(now.getTime() - 86400000); // 1 day ago
        Date future = new Date(now.getTime() + 86400000); // 1 day from now

        // Builder doesn't validate, so we can set inverted dates
        CertificateInfo info = CertificateInfo.builder()
                .notBefore(future)
                .notAfter(past)
                .build();

        assertNotNull(info);
        assertTrue(info.getNotBefore().after(info.getNotAfter()));
    }

    @Test
    void testSameDatesForNotBeforeAndNotAfter() {
        Date sameDate = new Date();

        CertificateInfo info = CertificateInfo.builder()
                .notBefore(sameDate)
                .notAfter(sameDate)
                .build();

        assertNotNull(info);
        assertEquals(info.getNotBefore(), info.getNotAfter());
    }

    @Test
    void testMultipleEmailFormats() {
        String[] emails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "user_name@sub.example.com",
                "123@example.com"
        };

        for (String email : emails) {
            CertificateInfo info = CertificateInfo.builder()
                    .email(email)
                    .build();
            assertEquals(email, info.getEmail());
        }
    }

    @Test
    void testMultipleSerialNumberFormats() {
        String[] serialNumbers = {
                "1A:2B:3C:4D:5E:6F",
                "1a2b3c4d5e6f",
                "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF",
                "123456789"
        };

        for (String serialNumber : serialNumbers) {
            CertificateInfo info = CertificateInfo.builder()
                    .serialNumber(serialNumber)
                    .build();
            assertEquals(serialNumber, info.getSerialNumber());
        }
    }

    @Test
    void testComplexDistinguishedNames() {
        String complexSubjectDN = "CN=John Doe+SERIALNUMBER=123,OU=Engineering+OU=Development,O=Example Corp,L=Springfield,ST=Illinois,C=US";
        String complexIssuerDN = "CN=Example Root CA,OU=PKI,O=Example Trust Network,C=US";

        CertificateInfo info = CertificateInfo.builder()
                .subjectDN(complexSubjectDN)
                .issuerDN(complexIssuerDN)
                .build();

        assertEquals(complexSubjectDN, info.getSubjectDN());
        assertEquals(complexIssuerDN, info.getIssuerDN());
    }

    @Test
    void testEdipiVariousFormats() {
        String[] edipis = {
                "1234567890",
                "0000000001",
                "9999999999"
        };

        for (String edipi : edipis) {
            CertificateInfo info = CertificateInfo.builder()
                    .edipi(edipi)
                    .build();
            assertEquals(edipi, info.getEdipi());
        }
    }

    @Test
    void testBuilderChaining() {
        CertificateInfo info = CertificateInfo.builder()
                .commonName("Test User")
                .email("test@example.com")
                .edipi("1234567890")
                .subjectDN("CN=Test User")
                .issuerDN("CN=CA")
                .serialNumber("ABC123")
                .notBefore(new Date())
                .notAfter(new Date())
                .build();

        assertNotNull(info);
        assertNotNull(info.getCommonName());
        assertNotNull(info.getEmail());
        assertNotNull(info.getEdipi());
        assertNotNull(info.getSubjectDN());
        assertNotNull(info.getIssuerDN());
        assertNotNull(info.getSerialNumber());
        assertNotNull(info.getNotBefore());
        assertNotNull(info.getNotAfter());
    }

    @Test
    void testUpdateExistingCertificateInfo() {
        CertificateInfo info = CertificateInfo.builder()
                .commonName("Old Name")
                .email("old@example.com")
                .build();

        // Update fields
        info.setCommonName("New Name");
        info.setEmail("new@example.com");
        info.setEdipi("9876543210");
        info.setSerialNumber("XYZ789");

        assertEquals("New Name", info.getCommonName());
        assertEquals("new@example.com", info.getEmail());
        assertEquals("9876543210", info.getEdipi());
        assertEquals("XYZ789", info.getSerialNumber());
    }
}
