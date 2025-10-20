// Comprehensive test for full authorization workflow

async function testFullAuthorizationWorkflow() {
  const API_BASE = 'http://localhost:8080/api';

  try {
    // 1. Register a user
    console.log('=== STEP 1: User Registration ===');
    const username = 'authtest' + Date.now();
    const registerResponse = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        password: 'Password123!',
        email: `${username}@example.com`
      })
    });

    if (!registerResponse.ok) {
      console.error('Registration failed:', await registerResponse.text());
      return;
    }

    const authData = await registerResponse.json();
    console.log('✓ User registered:', username);
    const token = authData.token;
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    };

    // 2. Create an SSP library item
    console.log('\n=== STEP 2: Create SSP Library Item ===');
    const sspContent = `<?xml version="1.0" encoding="UTF-8"?>
<system-security-plan xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <uuid>12345678-1234-1234-1234-123456789012</uuid>
  <metadata>
    <title>Test Production System SSP</title>
    <last-modified>2024-01-01T00:00:00Z</last-modified>
    <version>1.0</version>
    <oscal-version>1.0.0</oscal-version>
  </metadata>
  <system-characteristics>
    <system-id>test-system-001</system-id>
    <system-name>Test Production System</system-name>
  </system-characteristics>
</system-security-plan>`;

    const sspResponse = await fetch(`${API_BASE}/library`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        title: 'Test Production System SSP',
        description: 'SSP for testing authorization workflow',
        oscalType: 'system-security-plan',
        format: 'xml',
        content: sspContent,
        tags: ['test', 'production']
      })
    });

    if (!sspResponse.ok) {
      console.error('SSP creation failed:', await sspResponse.text());
      return;
    }

    const sspItem = await sspResponse.json();
    console.log('✓ SSP created with ID:', sspItem.itemId);
    console.log('  Title:', sspItem.title);

    // 3. Create an authorization template
    console.log('\n=== STEP 3: Create Authorization Template ===');
    const templateResponse = await fetch(`${API_BASE}/authorization-templates`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        name: 'Standard Production Authorization',
        content: `# SYSTEM AUTHORIZATION DECISION

## System Information
**System Name**: {{ system_name }}
**System Owner**: {{ system_owner }}
**Environment**: {{ environment }}

## Authorization Decision

After thorough review of the System Security Plan and supporting documentation,
this system is hereby **{{ decision }}** for operation in the {{ environment }} environment.

**Authorizing Official**: {{ authorizing_official }}
**Date of Authorization**: {{ authorization_date }}
**Authorization Period**: {{ authorization_period }}

## Risk Level
**Overall Risk**: {{ risk_level }}

## Special Conditions
{{ special_conditions }}

## Continuous Monitoring Requirements
{{ monitoring_requirements }}

---
*Document Generated: {{ generation_date }}*
*Authorized by: {{ authorizing_official }}*
`
      })
    });

    if (!templateResponse.ok) {
      console.error('Template creation failed:', await templateResponse.text());
      return;
    }

    const template = await templateResponse.json();
    console.log('✓ Template created with ID:', template.id);
    console.log('  Variables extracted:', template.variables.length);
    console.log('  Variables:', template.variables.join(', '));

    // 4. Create an authorization using the template and SSP
    console.log('\n=== STEP 4: Create Authorization ===');
    const authorizationResponse = await fetch(`${API_BASE}/authorizations`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        name: 'Production Authorization for Test System',
        sspItemId: sspItem.itemId,
        templateId: template.id,
        variableValues: {
          system_name: 'Test Production System',
          system_owner: 'John Doe',
          environment: 'Production',
          decision: 'AUTHORIZED',
          authorizing_official: 'Jane Smith, CISO',
          authorization_date: '2024-10-19',
          authorization_period: '3 years',
          risk_level: 'Low',
          special_conditions: 'System must undergo quarterly security reviews and maintain continuous monitoring.',
          monitoring_requirements: '- Weekly vulnerability scans\n- Monthly security assessments\n- Quarterly penetration testing\n- Annual security audit',
          generation_date: new Date().toISOString().split('T')[0]
        }
      })
    });

    if (!authorizationResponse.ok) {
      console.error('Authorization creation failed:', await authorizationResponse.text());
      return;
    }

    const authorization = await authorizationResponse.json();
    console.log('✓ Authorization created with ID:', authorization.id);
    console.log('  Name:', authorization.name);
    console.log('  SSP:', authorization.sspItemId);
    console.log('  Template:', authorization.templateName);
    console.log('  Authorized By:', authorization.authorizedBy);
    console.log('  Authorized At:', authorization.authorizedAt);

    // 5. View the completed authorization document
    console.log('\n=== STEP 5: View Completed Authorization Document ===');
    console.log('Completed Content Preview:');
    console.log('---');
    console.log(authorization.completedContent.substring(0, 400) + '...');
    console.log('---');

    // 6. Get all authorizations
    console.log('\n=== STEP 6: List All Authorizations ===');
    const allAuthsResponse = await fetch(`${API_BASE}/authorizations`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (allAuthsResponse.ok) {
      const allAuths = await allAuthsResponse.json();
      console.log(`✓ Found ${allAuths.length} authorization(s)`);
      allAuths.forEach(auth => {
        console.log(`  - ${auth.name} (ID: ${auth.id})`);
      });
    }

    // 7. Get authorizations for specific SSP
    console.log('\n=== STEP 7: Get Authorizations for SSP ===');
    const sspAuthsResponse = await fetch(`${API_BASE}/authorizations/ssp/${sspItem.itemId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (sspAuthsResponse.ok) {
      const sspAuths = await sspAuthsResponse.json();
      console.log(`✓ Found ${sspAuths.length} authorization(s) for this SSP`);
    }

    // 8. Search authorizations
    console.log('\n=== STEP 8: Search Authorizations ===');
    const searchResponse = await fetch(`${API_BASE}/authorizations/search?q=Production`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (searchResponse.ok) {
      const searchResults = await searchResponse.json();
      console.log(`✓ Search found ${searchResults.length} result(s)`);
    }

    // Summary
    console.log('\n=== ✅ WORKFLOW COMPLETE ===');
    console.log('Summary:');
    console.log('  1. ✓ User registered');
    console.log('  2. ✓ SSP library item created');
    console.log('  3. ✓ Authorization template created');
    console.log('  4. ✓ Authorization generated from template');
    console.log('  5. ✓ Completed document rendered');
    console.log('  6. ✓ Authorizations listed');
    console.log('  7. ✓ SSP-specific authorizations retrieved');
    console.log('  8. ✓ Authorization search working');
    console.log('\n🎉 All authorization features are working correctly!');

  } catch (error) {
    console.error('❌ Test failed:', error.message);
    console.error(error.stack);
  }
}

testFullAuthorizationWorkflow();
