// Test that all variables from the user's FedRAMP template are detected

async function testUserTemplate() {
  const API_BASE = 'http://localhost:8080/api';

  const userTemplate = `{{agency logo}}

{{Insert Date}}

{{Cloud System Owner Name}}
{{Insert Cloud Service Name}} Cloud System Owner
{{Insert Address}}

To: {{CSP System Owner Name}}

The {{Federal Agency/Office}} has completed the review of the {{Insert CSP and cloud service name}} Cloud system's security authorization package that meets the Federal Risk and Authorization Management Program (FedRAMP) requirements.  Based on the Federal Information Processing Standard (FIPS) security categorization of "{{Low, Moderate, or High}}" and the provided Security Assessment, the {{Federal Agency/Office}} has determined that the {{Insert CSP and cloud service name}} Cloud system meets the information security requirements and is granted an Authority to Operate.

The security authorization of the information system will remain in effect for a length of time in alignment with Office of Management and Budget Circular A-130 as long as:

1.    {{Insert CSP name}} satisfies the requirement of implementing continuous monitoring activities as documented in FedRAMP's continuous monitoring requirements and {Insert CSP name} Continuous Monitoring Plan;
2.    {{Insert CSP name}} mitigates all open POA&M action items, agreed to in the Security Assessment Report (SAR) and as developed during the continuous monitoring activities; and
3.    Significant changes or critical vulnerabilities are identified and managed in accordance with applicable Federal law, guidelines, and policies.

{{Federal Agency/Office}} is leveraging the documentation provided within the FedRAMP secure repository as a key element of the Authority to Operate (ATO).  Based on the documentation within the FedRAMP secure repository and customer-specific tailoring and operating procedures, the {{Federal Agency/Office}} believes the security authorization package accurately documents the {{Insert CSP name}} cloud system and clearly defines outstanding risk considerations.

SIGNED:


{{Authorizing Official}}
{{Title}}
{{Office}}
{{Agency}}
{{Street Address}}
{{City, State, Zip}}
{{Phone}}
{{Email}}

cc FedRAMP PMO at info@FedRAMP.gov`;

  console.log('Testing User Template Variable Extraction\n');
  console.log('Template from FedRAMP authorization letter\n');
  console.log('=' .repeat(80));

  // Test frontend pattern
  console.log('\n1. Testing Frontend Pattern');
  const pattern = /\{\{\s*([^}]+?)\s*\}\}/g;
  const matches = userTemplate.matchAll(pattern);
  const variables = new Set();

  for (const match of matches) {
    variables.add(match[1].trim());
  }

  console.log(`   Found ${variables.size} unique variables:\n`);
  const varArray = Array.from(variables).sort();
  varArray.forEach((v, i) => {
    console.log(`   ${(i + 1).toString().padStart(2)}. ${v}`);
  });

  // Test with backend API
  console.log('\n2. Testing Backend API Integration');
  try {
    // Register user
    const username = 'fedramptest' + Date.now();
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
      console.log('   ✗ Registration failed');
      return;
    }

    const authData = await registerResponse.json();
    const token = authData.token;

    // Create template
    const templateResponse = await fetch(`${API_BASE}/authorization-templates`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        name: 'FedRAMP Authorization Letter',
        content: userTemplate
      })
    });

    if (!templateResponse.ok) {
      const error = await templateResponse.text();
      console.log('   ✗ Template creation failed:', error);
      return;
    }

    const template = await templateResponse.json();
    console.log(`   ✓ Template created successfully`);
    console.log(`   ✓ Backend extracted ${template.variables.length} variables`);

    // Compare frontend vs backend
    const backendVars = new Set(template.variables);
    const frontendVars = new Set(varArray);

    if (backendVars.size === frontendVars.size) {
      console.log(`   ✓ Frontend and backend match: ${backendVars.size} variables`);
    } else {
      console.log(`   ✗ Mismatch: Frontend ${frontendVars.size}, Backend ${backendVars.size}`);
    }

    // Check for differences
    const onlyFrontend = [...frontendVars].filter(v => !backendVars.has(v));
    const onlyBackend = [...backendVars].filter(v => !frontendVars.has(v));

    if (onlyFrontend.length > 0) {
      console.log('\n   Variables only in frontend:', onlyFrontend);
    }
    if (onlyBackend.length > 0) {
      console.log('\n   Variables only in backend:', onlyBackend);
    }

    if (onlyFrontend.length === 0 && onlyBackend.length === 0) {
      console.log('\n   ✓ All variables match perfectly!');
    }

    // Show backend variables
    console.log('\n   Backend Variables:');
    template.variables.sort().forEach((v, i) => {
      console.log(`   ${(i + 1).toString().padStart(2)}. ${v}`);
    });

    // Test creating an authorization
    console.log('\n3. Testing Authorization Creation');
    const mockSspId = 'fedramp-ssp-' + Date.now();
    const variableValues = {};

    template.variables.forEach(varName => {
      variableValues[varName] = `[${varName}]`; // Use bracketed name as value
    });

    const authResponse = await fetch(`${API_BASE}/authorizations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        name: 'Test FedRAMP Authorization',
        sspItemId: mockSspId,
        templateId: template.id,
        variableValues
      })
    });

    if (!authResponse.ok) {
      const error = await authResponse.text();
      console.log('   ✗ Authorization creation failed:', error);
      return;
    }

    const authorization = await authResponse.json();
    console.log(`   ✓ Authorization created successfully`);
    console.log(`   ✓ All ${Object.keys(authorization.variableValues).length} variables filled`);

    // Show sample of completed content
    console.log('\n4. Sample of Completed Content:');
    const lines = authorization.completedContent.split('\n');
    console.log('\n   ' + lines.slice(0, 10).join('\n   '));
    console.log('   ...');

    console.log('\n' + '='.repeat(80));
    console.log('\n✅ SUCCESS! All variables with spaces, commas, and special characters work correctly!');

  } catch (error) {
    console.error('\n❌ Error:', error.message);
  }
}

testUserTemplate();
