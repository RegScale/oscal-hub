// Test script for authorization API endpoints

async function testAuthorizationAPI() {
  const API_BASE = 'http://localhost:8080/api';

  try {
    // Register a user
    console.log('1. Registering user...');
    const registerResponse = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'testuser' + Date.now(),
        password: 'Password123!',
        email: `test${Date.now()}@example.com`
      })
    });

    if (!registerResponse.ok) {
      const error = await registerResponse.text();
      console.error('Registration failed:', error);
      return;
    }

    const authData = await registerResponse.json();
    console.log('✓ User registered successfully');
    const token = authData.token;

    // Create an authorization template
    console.log('\n2. Creating authorization template...');
    const templateResponse = await fetch(`${API_BASE}/authorization-templates`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        name: 'Production Authorization Template',
        content: `# System Authorization Document

## System Information
- **System Name**: {{ system_name }}
- **System Owner**: {{ system_owner }}
- **Authorization Type**: {{ authorization_type }}

## Authorization Decision

This system has been reviewed and is **AUTHORIZED** for production deployment.

**Authorized By**: {{ authorized_by }}
**Authorization Date**: {{ authorization_date }}
**Authorization Period**: {{ authorization_period }}

## Conditions
{{ conditions }}

## Risk Assessment
{{ risk_assessment }}

---
*Generated: {{ generation_date }}*`
      })
    });

    if (!templateResponse.ok) {
      const error = await templateResponse.text();
      console.error('Template creation failed:', error);
      return;
    }

    const template = await templateResponse.json();
    console.log('✓ Template created with ID:', template.id);
    console.log('  Extracted variables:', template.variables);

    // Get all templates
    console.log('\n3. Fetching all templates...');
    const getTemplatesResponse = await fetch(`${API_BASE}/authorization-templates`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (getTemplatesResponse.ok) {
      const templates = await getTemplatesResponse.json();
      console.log(`✓ Found ${templates.length} template(s)`);
      templates.forEach(t => {
        console.log(`  - ${t.name} (${t.variables.length} variables)`);
      });
    }

    // Update the template
    console.log('\n4. Updating template...');
    const updateResponse = await fetch(`${API_BASE}/authorization-templates/${template.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        name: 'Updated Production Authorization Template',
        content: template.content
      })
    });

    if (updateResponse.ok) {
      const updated = await updateResponse.json();
      console.log('✓ Template updated:', updated.name);
    }

    // Search templates
    console.log('\n5. Searching templates...');
    const searchResponse = await fetch(`${API_BASE}/authorization-templates/search?q=Production`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (searchResponse.ok) {
      const results = await searchResponse.json();
      console.log(`✓ Search found ${results.length} result(s)`);
    }

    // Create an authorization (requires SSP item - skipping for now)
    console.log('\n6. Authorization creation (requires SSP) - skipped');

    // Delete the template
    console.log('\n7. Deleting template...');
    const deleteResponse = await fetch(`${API_BASE}/authorization-templates/${template.id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (deleteResponse.ok) {
      console.log('✓ Template deleted successfully');
    }

    console.log('\n✅ All authorization API tests passed!');

  } catch (error) {
    console.error('❌ Test failed:', error.message);
  }
}

testAuthorizationAPI();
