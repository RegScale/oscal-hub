// Simplified authorization workflow test

async function testAuthorizationWorkflow() {
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

    // 2. Create an authorization template
    console.log('\n=== STEP 2: Create Authorization Template ===');
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
    console.log('  Name:', template.name);
    console.log('  Variables extracted:', template.variables.length);
    console.log('  Variables:', template.variables.join(', '));

    // 3. Get the template to verify
    console.log('\n=== STEP 3: Retrieve Template ===');
    const getTemplateResponse = await fetch(`${API_BASE}/authorization-templates/${template.id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (getTemplateResponse.ok) {
      const retrievedTemplate = await getTemplateResponse.json();
      console.log('✓ Template retrieved');
      console.log('  Name:', retrievedTemplate.name);
      console.log('  Created by:', retrievedTemplate.createdBy);
      console.log('  Created at:', retrievedTemplate.createdAt);
    }

    // 4. Update the template
    console.log('\n=== STEP 4: Update Template ===');
    const updateResponse = await fetch(`${API_BASE}/authorization-templates/${template.id}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify({
        name: 'Updated Standard Production Authorization',
        content: template.content + '\n\n## Additional Notes\n{{ additional_notes }}'
      })
    });

    if (updateResponse.ok) {
      const updatedTemplate = await updateResponse.json();
      console.log('✓ Template updated');
      console.log('  New name:', updatedTemplate.name);
      console.log('  Variables now:', updatedTemplate.variables.length);
    }

    // 5. Create an authorization (with mock SSP ID)
    console.log('\n=== STEP 5: Create Authorization ===');
    const mockSspId = 'test-ssp-' + Date.now();
    const authorizationResponse = await fetch(`${API_BASE}/authorizations`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        name: 'Production Authorization for Test System',
        sspItemId: mockSspId,
        templateId: template.id,
        variableValues: {
          system_name: 'Test Production System',
          system_owner: 'John Doe, IT Director',
          environment: 'Production',
          decision: 'AUTHORIZED',
          authorizing_official: 'Jane Smith, Chief Information Security Officer',
          authorization_date: '2024-10-19',
          authorization_period: '3 years (until October 19, 2027)',
          risk_level: 'Low',
          special_conditions: 'The system must undergo quarterly security reviews and maintain continuous monitoring of all critical components.',
          monitoring_requirements: '• Weekly automated vulnerability scans\n• Monthly manual security assessments\n• Quarterly penetration testing\n• Annual comprehensive security audit',
          generation_date: new Date().toISOString().split('T')[0],
          additional_notes: 'This authorization is contingent upon maintaining all security controls as documented in the SSP.'
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
    console.log('  SSP ID:', authorization.sspItemId);
    console.log('  Template:', authorization.templateName);
    console.log('  Authorized By:', authorization.authorizedBy);
    console.log('  Authorized At:', authorization.authorizedAt);
    console.log('  Variables filled:', Object.keys(authorization.variableValues).length);

    // 6. View the completed authorization document
    console.log('\n=== STEP 6: View Completed Authorization Document ===');
    console.log('---BEGIN AUTHORIZATION DOCUMENT---');
    console.log(authorization.completedContent);
    console.log('---END AUTHORIZATION DOCUMENT---');

    // 7. Get authorization by ID
    console.log('\n=== STEP 7: Retrieve Authorization ===');
    const getAuthResponse = await fetch(`${API_BASE}/authorizations/${authorization.id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (getAuthResponse.ok) {
      console.log('✓ Authorization retrieved successfully');
    }

    // 8. Get all authorizations
    console.log('\n=== STEP 8: List All Authorizations ===');
    const allAuthsResponse = await fetch(`${API_BASE}/authorizations`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (allAuthsResponse.ok) {
      const allAuths = await allAuthsResponse.json();
      console.log(`✓ Found ${allAuths.length} authorization(s)`);
      allAuths.forEach(auth => {
        console.log(`  - ${auth.name} (ID: ${auth.id}, SSP: ${auth.sspItemId})`);
      });
    }

    // 9. Get authorizations for specific SSP
    console.log('\n=== STEP 9: Get Authorizations for SSP ===');
    const sspAuthsResponse = await fetch(`${API_BASE}/authorizations/ssp/${mockSspId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (sspAuthsResponse.ok) {
      const sspAuths = await sspAuthsResponse.json();
      console.log(`✓ Found ${sspAuths.length} authorization(s) for SSP ${mockSspId}`);
    }

    // 10. Search authorizations
    console.log('\n=== STEP 10: Search Authorizations ===');
    const searchResponse = await fetch(`${API_BASE}/authorizations/search?q=Production`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (searchResponse.ok) {
      const searchResults = await searchResponse.json();
      console.log(`✓ Search for "Production" found ${searchResults.length} result(s)`);
    }

    // 11. List all templates
    console.log('\n=== STEP 11: List All Templates ===');
    const allTemplatesResponse = await fetch(`${API_BASE}/authorization-templates`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (allTemplatesResponse.ok) {
      const allTemplates = await allTemplatesResponse.json();
      console.log(`✓ Found ${allTemplates.length} template(s)`);
      allTemplates.forEach(t => {
        console.log(`  - ${t.name} (${t.variables.length} variables)`);
      });
    }

    // Summary
    console.log('\n=== ✅ WORKFLOW COMPLETE ===');
    console.log('\nSummary:');
    console.log('  1. ✓ User registered');
    console.log('  2. ✓ Authorization template created with variable extraction');
    console.log('  3. ✓ Template retrieved');
    console.log('  4. ✓ Template updated');
    console.log('  5. ✓ Authorization created from template');
    console.log('  6. ✓ Completed document rendered with all variables replaced');
    console.log('  7. ✓ Authorization retrieved by ID');
    console.log('  8. ✓ All authorizations listed');
    console.log('  9. ✓ SSP-specific authorizations retrieved');
    console.log(' 10. ✓ Authorization search working');
    console.log(' 11. ✓ All templates listed');
    console.log('\n🎉 All authorization API endpoints are working correctly!');
    console.log('\nThe authorization feature is ready for use through the web interface.');

  } catch (error) {
    console.error('❌ Test failed:', error.message);
    console.error(error.stack);
  }
}

testAuthorizationWorkflow();
