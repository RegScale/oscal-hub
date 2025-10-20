// Test variable pattern matching to show valid vs invalid variables

function testVariablePatterns() {
  // This matches the backend pattern exactly
  const validPattern = /\{\{\s*([\w\-]+)\s*\}\}/g;

  // This finds all variable-like patterns
  const allPattern = /\{\{\s*([^}]+)\s*\}\}/g;

  const testCases = [
    // Valid cases
    { text: '{{ variable }}', expected: 'valid' },
    { text: '{{ variable_name }}', expected: 'valid' },
    { text: '{{ variable-name }}', expected: 'valid' },
    { text: '{{ Variable123 }}', expected: 'valid' },
    { text: '{{no_spaces}}', expected: 'valid' },
    { text: '{{  lots_of_spaces  }}', expected: 'valid' },
    { text: '{{ _underscore_start }}', expected: 'valid' },
    { text: '{{ name123 }}', expected: 'valid' },

    // Invalid cases (will be detected but rejected by backend)
    { text: '{{ with spaces }}', expected: 'invalid' },
    { text: '{{ special@char }}', expected: 'invalid' },
    { text: '{{ dot.separated }}', expected: 'invalid' },
    { text: '{{ plus+sign }}', expected: 'invalid' },
    { text: '{{ dollar$sign }}', expected: 'invalid' },
    { text: '{{ colon:name }}', expected: 'invalid' },
  ];

  console.log('Variable Pattern Validation Tests\n');
  console.log('Valid Pattern (Backend):   \\{\\{\\s*([\\w\\-]+)\\s*\\}\\}');
  console.log('Allows: letters, digits, underscore (_), hyphen (-)\n');
  console.log('='.repeat(80));

  let passCount = 0;
  let failCount = 0;

  testCases.forEach((testCase, index) => {
    const validMatches = [...testCase.text.matchAll(validPattern)];
    const allMatches = [...testCase.text.matchAll(allPattern)];

    const hasValidMatch = validMatches.length > 0;
    const hasAnyMatch = allMatches.length > 0;

    let actual;
    if (hasValidMatch) {
      actual = 'valid';
    } else if (hasAnyMatch) {
      actual = 'invalid';
    } else {
      actual = 'no match';
    }

    const passed = actual === testCase.expected;
    const status = passed ? '✓ PASS' : '✗ FAIL';

    if (passed) passCount++;
    else failCount++;

    console.log(`\nTest ${index + 1}: ${status}`);
    console.log(`  Input:    "${testCase.text}"`);
    console.log(`  Expected: ${testCase.expected}`);
    console.log(`  Actual:   ${actual}`);

    if (validMatches.length > 0) {
      console.log(`  Extracted: "${validMatches[0][1].trim()}"`);
    } else if (allMatches.length > 0) {
      console.log(`  Found:     "${allMatches[0][1].trim()}" (INVALID - will be rejected by backend)`);
    }
  });

  console.log('\n' + '='.repeat(80));
  console.log(`\nResults: ${passCount} passed, ${failCount} failed out of ${testCases.length} tests\n`);

  // Real-world example
  console.log('Real-World Template Example:\n');
  const template = `
# Authorization for {{ system_name }}

System: {{ system-id }}
Owner: {{ system_owner }}

INVALID EXAMPLES (will be highlighted as errors):
- With spaces: {{ system name }}
- With dots: {{ system.id }}
- With special chars: {{ date@time }}

VALID EXAMPLES:
- Underscores: {{ system_name }}
- Hyphens: {{ system-id }}
- Mixed: {{ SystemName_123 }}
`;

  const validVars = new Set();
  const invalidVars = new Set();

  // Find valid variables
  const validMatches = template.matchAll(validPattern);
  for (const match of validMatches) {
    validVars.add(match[1].trim());
  }

  // Find all patterns
  const allMatches = template.matchAll(allPattern);
  for (const match of allMatches) {
    const varName = match[1].trim();
    if (!validVars.has(varName)) {
      invalidVars.add(varName);
    }
  }

  console.log('Valid Variables (will be accepted):');
  validVars.forEach(v => console.log(`  ✓ ${v}`));

  console.log('\nInvalid Variables (will be rejected):');
  invalidVars.forEach(v => console.log(`  ✗ ${v}`));
}

testVariablePatterns();
