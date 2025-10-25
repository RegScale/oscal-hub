/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.ProfileResolutionRequest;
import gov.nist.oscal.tools.api.model.ProfileResolutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileResolutionServiceTest {

    private ProfileResolutionService service;

    @BeforeEach
    void setUp() {
        service = new ProfileResolutionService();
    }

    @Test
    void testResolveProfile_withValidProfileWithImports_returnsNotImplementedMessage() {
        // Given: A valid profile with imports
        String profileContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile xmlns="http://csrc.nist.gov/ns/oscal/1.0" uuid="531e37ca-9ab8-435f-915d-e96c7f4361d2">
                <metadata>
                    <title>Valid OSCAL Profile</title>
                    <last-modified>2023-10-24T00:00:00.000000-00:00</last-modified>
                    <version>1.0</version>
                    <oscal-version>1.1.1</oscal-version>
                </metadata>
                <import href="#fa585705-f386-4e3d-98d9-69a39fb26a0b">
                    <include-controls>
                        <with-id>control-1</with-id>
                    </include-controls>
                </import>
                <back-matter>
                    <resource uuid="fa585705-f386-4e3d-98d9-69a39fb26a0b">
                        <rlink href="example_catalog_valid.xml"/>
                    </resource>
                </back-matter>
            </profile>
            """;

        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent(profileContent);
        request.setFormat(OscalFormat.XML);

        // When: Resolving the profile
        ProfileResolutionResult result = service.resolveProfile(request, "testuser");

        // Then: Should return not implemented message with import count
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("not yet implemented"));
        assertTrue(result.getError().contains("1 catalog(s)"));
    }

    @Test
    void testResolveProfile_withProfileWithoutImports_returnsError() {
        // Given: A profile without imports
        String profileContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile xmlns="http://csrc.nist.gov/ns/oscal/1.0" uuid="9bf02239-f9e1-4608-b41b-af82e6b7ff1a">
                <metadata>
                    <title>Profile Without Imports</title>
                    <last-modified>2023-10-24T00:00:00.000000-00:00</last-modified>
                    <version>1.0</version>
                    <oscal-version>1.1.1</oscal-version>
                </metadata>
            </profile>
            """;

        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent(profileContent);
        request.setFormat(OscalFormat.XML);

        // When: Resolving the profile
        ProfileResolutionResult result = service.resolveProfile(request, "testuser");

        // Then: Should return error about missing imports
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("no imports"));
    }

    @Test
    void testResolveProfile_withInvalidXml_returnsError() {
        // Given: Invalid XML content
        String profileContent = "invalid xml content";

        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent(profileContent);
        request.setFormat(OscalFormat.XML);

        // When: Resolving the profile
        ProfileResolutionResult result = service.resolveProfile(request, "testuser");

        // Then: Should return error
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("failed"));
    }

    @Test
    void testResolveProfile_withJsonFormat_deserializesCorrectly() {
        // Given: A valid profile in JSON format with imports
        String profileContent = """
            {
              "profile": {
                "uuid": "531e37ca-9ab8-435f-915d-e96c7f4361d2",
                "metadata": {
                  "title": "Valid OSCAL Profile",
                  "last-modified": "2023-10-24T00:00:00.000000-00:00",
                  "version": "1.0",
                  "oscal-version": "1.1.1"
                },
                "imports": [
                  {
                    "href": "#fa585705-f386-4e3d-98d9-69a39fb26a0b",
                    "include-controls": [
                      {
                        "with-ids": ["control-1"]
                      }
                    ]
                  }
                ],
                "back-matter": {
                  "resources": [
                    {
                      "uuid": "fa585705-f386-4e3d-98d9-69a39fb26a0b",
                      "rlinks": [
                        {
                          "href": "example_catalog_valid.xml"
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """;

        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent(profileContent);
        request.setFormat(OscalFormat.JSON);

        // When: Resolving the profile
        ProfileResolutionResult result = service.resolveProfile(request, "testuser");

        // Then: Should handle JSON format correctly
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("not yet implemented"));
    }

    @Test
    void testResolveProfile_withMultipleImports_countsCorrectly() {
        // Given: A profile with multiple imports
        String profileContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile xmlns="http://csrc.nist.gov/ns/oscal/1.0" uuid="531e37ca-9ab8-435f-915d-e96c7f4361d2">
                <metadata>
                    <title>Profile with Multiple Imports</title>
                    <last-modified>2023-10-24T00:00:00.000000-00:00</last-modified>
                    <version>1.0</version>
                    <oscal-version>1.1.1</oscal-version>
                </metadata>
                <import href="#catalog1">
                    <include-controls>
                        <with-id>control-1</with-id>
                    </include-controls>
                </import>
                <import href="#catalog2">
                    <include-controls>
                        <with-id>control-2</with-id>
                    </include-controls>
                </import>
                <import href="#catalog3">
                    <include-controls>
                        <with-id>control-3</with-id>
                    </include-controls>
                </import>
            </profile>
            """;

        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent(profileContent);
        request.setFormat(OscalFormat.XML);

        // When: Resolving the profile
        ProfileResolutionResult result = service.resolveProfile(request, "testuser");

        // Then: Should count all 3 imports
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("3 catalog(s)"));
    }

    @Test
    void testResolveProfile_withYamlFormat_deserializesCorrectly() {
        // Given: A valid profile in YAML format
        String profileContent = """
            profile:
              uuid: 531e37ca-9ab8-435f-915d-e96c7f4361d2
              metadata:
                title: Valid OSCAL Profile
                last-modified: 2023-10-24T00:00:00.000000-00:00
                version: '1.0'
                oscal-version: 1.1.1
              imports:
                - href: '#fa585705-f386-4e3d-98d9-69a39fb26a0b'
                  include-controls:
                    - with-ids:
                        - control-1
            """;

        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent(profileContent);
        request.setFormat(OscalFormat.YAML);

        // When: Resolving the profile
        ProfileResolutionResult result = service.resolveProfile(request, "testuser");

        // Then: Should handle YAML format correctly
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("not yet implemented"));
    }

    @Test
    void testResolveProfile_withEmptyContent_returnsError() {
        // Given: Empty content
        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent("");
        request.setFormat(OscalFormat.XML);

        // When: Resolving the profile
        ProfileResolutionResult result = service.resolveProfile(request, "testuser");

        // Then: Should return error
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("failed"));
    }
}
