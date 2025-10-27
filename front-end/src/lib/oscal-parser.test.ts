import { describe, it, expect } from 'vitest';
import { analyzeCatalog, type CatalogAnalysis } from './oscal-parser';

describe('oscal-parser', () => {
  // ========== XML PARSING TESTS ==========

  describe('XML Catalog Parsing', () => {
    it('should parse a valid XML catalog with groups', () => {
      const xmlCatalog = `<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <metadata>
    <title>Test Catalog</title>
    <version>1.0</version>
    <oscal-version>1.0.0</oscal-version>
    <last-modified>2025-01-01T00:00:00Z</last-modified>
    <published>2024-12-01T00:00:00Z</published>
  </metadata>
  <group id="ac">
    <title>Access Control</title>
    <control id="ac-1">
      <title>Access Control Policy and Procedures</title>
    </control>
    <control id="ac-2">
      <title>Account Management</title>
    </control>
  </group>
  <group id="au">
    <title>Audit and Accountability</title>
    <control id="au-1">
      <title>Audit Policy</title>
    </control>
  </group>
</catalog>`;

      const result = analyzeCatalog(xmlCatalog, 'xml');

      expect(result.metadata.title).toBe('Test Catalog');
      expect(result.metadata.version).toBe('1.0');
      expect(result.metadata.oscalVersion).toBe('1.0.0');
      expect(result.metadata.lastModified).toBe('2025-01-01T00:00:00Z');
      expect(result.metadata.published).toBe('2024-12-01T00:00:00Z');
      expect(result.totalControls).toBe(3);
      expect(result.families).toHaveLength(2);

      const acFamily = result.families.find(f => f.id === 'ac');
      expect(acFamily).toBeDefined();
      expect(acFamily?.title).toBe('Access Control');
      expect(acFamily?.controlCount).toBe(2);

      const auFamily = result.families.find(f => f.id === 'au');
      expect(auFamily).toBeDefined();
      expect(auFamily?.title).toBe('Audit and Accountability');
      expect(auFamily?.controlCount).toBe(1);
    });

    it('should parse XML catalog with nested groups', () => {
      const xmlCatalog = `<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <metadata>
    <title>Nested Catalog</title>
  </metadata>
  <group id="parent">
    <title>Parent Group</title>
    <control id="parent-1">
      <title>Parent Control</title>
    </control>
    <group id="child">
      <title>Child Group</title>
      <control id="child-1">
        <title>Child Control 1</title>
      </control>
      <control id="child-2">
        <title>Child Control 2</title>
      </control>
    </group>
  </group>
</catalog>`;

      const result = analyzeCatalog(xmlCatalog, 'xml');

      expect(result.totalControls).toBe(3);
      expect(result.families).toHaveLength(1);

      const parentFamily = result.families[0];
      expect(parentFamily.id).toBe('parent');
      expect(parentFamily.controlCount).toBe(3); // 1 parent + 2 child controls
    });

    it('should parse XML catalog with top-level controls', () => {
      const xmlCatalog = `<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <metadata>
    <title>Mixed Catalog</title>
  </metadata>
  <control id="top-1">
    <title>Top Level Control 1</title>
  </control>
  <control id="top-2">
    <title>Top Level Control 2</title>
  </control>
  <group id="grouped">
    <title>Grouped Controls</title>
    <control id="grouped-1">
      <title>Grouped Control</title>
    </control>
  </group>
</catalog>`;

      const result = analyzeCatalog(xmlCatalog, 'xml');

      expect(result.totalControls).toBe(3);
      expect(result.families).toHaveLength(2);

      const ungroupedFamily = result.families.find(f => f.id === 'ungrouped');
      expect(ungroupedFamily).toBeDefined();
      expect(ungroupedFamily?.title).toBe('Ungrouped Controls');
      expect(ungroupedFamily?.controlCount).toBe(2);
    });

    it('should handle XML catalog with minimal metadata', () => {
      const xmlCatalog = `<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <metadata>
    <title>Minimal Catalog</title>
  </metadata>
  <group id="test">
    <title>Test Group</title>
    <control id="test-1">
      <title>Test Control</title>
    </control>
  </group>
</catalog>`;

      const result = analyzeCatalog(xmlCatalog, 'xml');

      expect(result.metadata.title).toBe('Minimal Catalog');
      expect(result.metadata.version).toBeUndefined();
      expect(result.metadata.oscalVersion).toBeUndefined();
      expect(result.metadata.lastModified).toBeUndefined();
      expect(result.metadata.published).toBeUndefined();
    });

    it('should handle XML catalog without metadata element', () => {
      const xmlCatalog = `<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <group id="test">
    <title>Test Group</title>
    <control id="test-1">
      <title>Test Control</title>
    </control>
  </group>
</catalog>`;

      const result = analyzeCatalog(xmlCatalog, 'xml');

      expect(result.metadata.title).toBe('Untitled Catalog');
      expect(result.totalControls).toBe(1);
    });

    it('should throw error for invalid XML catalog', () => {
      const invalidXml = `<?xml version="1.0" encoding="UTF-8"?>
<not-a-catalog>
  <metadata>
    <title>Invalid</title>
  </metadata>
</not-a-catalog>`;

      expect(() => analyzeCatalog(invalidXml, 'xml')).toThrow('Invalid catalog XML');
    });
  });

  // ========== JSON PARSING TESTS ==========

  describe('JSON Catalog Parsing', () => {
    it('should parse a valid JSON catalog with groups', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'Test JSON Catalog',
            version: '2.0',
            'oscal-version': '1.0.0',
            'last-modified': '2025-01-01T00:00:00Z',
            published: '2024-12-01T00:00:00Z',
          },
          groups: [
            {
              id: 'ac',
              title: 'Access Control',
              controls: [
                { id: 'ac-1', title: 'Policy' },
                { id: 'ac-2', title: 'Management' },
              ],
            },
            {
              id: 'au',
              title: 'Audit',
              controls: [
                { id: 'au-1', title: 'Audit Policy' },
              ],
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.metadata.title).toBe('Test JSON Catalog');
      expect(result.metadata.version).toBe('2.0');
      expect(result.metadata.oscalVersion).toBe('1.0.0');
      expect(result.totalControls).toBe(3);
      expect(result.families).toHaveLength(2);

      const acFamily = result.families.find(f => f.id === 'ac');
      expect(acFamily?.controlCount).toBe(2);
    });

    it('should parse JSON catalog with nested groups', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'Nested JSON Catalog',
          },
          groups: [
            {
              id: 'parent',
              title: 'Parent',
              controls: [
                { id: 'parent-1', title: 'Parent Control' },
              ],
              groups: [
                {
                  id: 'child',
                  title: 'Child',
                  controls: [
                    { id: 'child-1', title: 'Child Control 1' },
                    { id: 'child-2', title: 'Child Control 2' },
                  ],
                },
              ],
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.totalControls).toBe(3);
      const parentFamily = result.families[0];
      expect(parentFamily.controlCount).toBe(3); // Includes nested controls
    });

    it('should parse JSON catalog with top-level controls', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'Mixed JSON Catalog',
          },
          controls: [
            { id: 'top-1', title: 'Top Control 1' },
            { id: 'top-2', title: 'Top Control 2' },
          ],
          groups: [
            {
              id: 'grouped',
              title: 'Grouped',
              controls: [
                { id: 'grouped-1', title: 'Grouped Control' },
              ],
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.totalControls).toBe(3);
      expect(result.families).toHaveLength(2);

      const ungroupedFamily = result.families.find(f => f.id === 'ungrouped');
      expect(ungroupedFamily?.controlCount).toBe(2);
    });

    it('should handle JSON catalog with minimal metadata', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'Minimal JSON',
          },
          groups: [
            {
              id: 'test',
              title: 'Test',
              controls: [{ id: 'test-1', title: 'Test' }],
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.metadata.title).toBe('Minimal JSON');
      expect(result.metadata.version).toBeUndefined();
    });

    it('should handle JSON catalog without metadata', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          groups: [
            {
              id: 'test',
              title: 'Test',
              controls: [{ id: 'test-1', title: 'Test' }],
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.metadata.title).toBe('Untitled Catalog');
    });

    it('should throw error for JSON without catalog property', () => {
      const invalidJson = JSON.stringify({
        notACatalog: {
          metadata: { title: 'Invalid' },
        },
      });

      expect(() => analyzeCatalog(invalidJson, 'json')).toThrow('Invalid catalog JSON');
    });

    it('should throw error for invalid JSON', () => {
      const malformedJson = '{ "catalog": invalid json }';

      expect(() => analyzeCatalog(malformedJson, 'json')).toThrow('Failed to analyze catalog');
    });
  });

  // ========== YAML PARSING TESTS ==========

  describe('YAML Catalog Parsing', () => {
    it('should parse a valid YAML catalog', () => {
      const yamlCatalog = `
catalog:
  metadata:
    title: Test YAML Catalog
    version: "3.0"
    oscal-version: "1.0.0"
  groups:
    - id: ac
      title: Access Control
      controls:
        - id: ac-1
          title: Policy
        - id: ac-2
          title: Management
    - id: au
      title: Audit
      controls:
        - id: au-1
          title: Audit Policy
`;

      const result = analyzeCatalog(yamlCatalog, 'yaml');

      expect(result.metadata.title).toBe('Test YAML Catalog');
      expect(result.metadata.version).toBe('3.0');
      expect(result.totalControls).toBe(3);
      expect(result.families).toHaveLength(2);
    });

    it('should parse YAML catalog with nested groups', () => {
      const yamlCatalog = `
catalog:
  metadata:
    title: Nested YAML
  groups:
    - id: parent
      title: Parent
      controls:
        - id: parent-1
          title: Parent Control
      groups:
        - id: child
          title: Child
          controls:
            - id: child-1
              title: Child Control
`;

      const result = analyzeCatalog(yamlCatalog, 'yaml');

      expect(result.totalControls).toBe(2);
      const parentFamily = result.families[0];
      expect(parentFamily.controlCount).toBe(2);
    });

    it('should throw error for invalid YAML', () => {
      const invalidYaml = `
catalog:
  metadata:
    title: Invalid
  invalid yaml: [unclosed
`;

      expect(() => analyzeCatalog(invalidYaml, 'yaml')).toThrow('Failed to analyze catalog');
    });
  });

  // ========== EDGE CASES ==========

  describe('Edge Cases', () => {
    it('should handle catalog with no controls', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'Empty Catalog',
          },
          groups: [],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.totalControls).toBe(0);
      expect(result.families).toHaveLength(0);
    });

    it('should handle groups without controls', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'Groups Without Controls',
          },
          groups: [
            {
              id: 'empty1',
              title: 'Empty Group 1',
            },
            {
              id: 'empty2',
              title: 'Empty Group 2',
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.totalControls).toBe(0);
      expect(result.families).toHaveLength(2);
      expect(result.families[0].controlCount).toBe(0);
      expect(result.families[1].controlCount).toBe(0);
    });

    it('should handle group without id attribute', () => {
      const xmlCatalog = `<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <metadata>
    <title>No ID Group</title>
  </metadata>
  <group>
    <title>No ID</title>
    <control id="test-1">
      <title>Test</title>
    </control>
  </group>
</catalog>`;

      const result = analyzeCatalog(xmlCatalog, 'xml');

      expect(result.families[0].id).toBe('unknown');
    });

    it('should handle group without title', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'No Title Group',
          },
          groups: [
            {
              id: 'no-title',
              controls: [{ id: 'test-1', title: 'Test' }],
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.families[0].title).toBe('no-title'); // Uses ID as fallback
    });

    it('should throw error for unsupported format', () => {
      const content = 'some content';

      expect(() => analyzeCatalog(content, 'unsupported' as 'xml' | 'json' | 'yaml')).toThrow('Unsupported format');
    });
  });

  // ========== COMPLEX SCENARIOS ==========

  describe('Complex Scenarios', () => {
    it('should correctly count controls across multiple nesting levels', () => {
      const xmlCatalog = `<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://csrc.nist.gov/ns/oscal/1.0">
  <metadata>
    <title>Deep Nesting</title>
  </metadata>
  <group id="level1">
    <title>Level 1</title>
    <control id="l1-c1"><title>L1 Control 1</title></control>
    <control id="l1-c2"><title>L1 Control 2</title></control>
    <group id="level2">
      <title>Level 2</title>
      <control id="l2-c1"><title>L2 Control 1</title></control>
      <group id="level3">
        <title>Level 3</title>
        <control id="l3-c1"><title>L3 Control 1</title></control>
        <control id="l3-c2"><title>L3 Control 2</title></control>
      </group>
    </group>
  </group>
</catalog>`;

      const result = analyzeCatalog(xmlCatalog, 'xml');

      expect(result.totalControls).toBe(5);
      expect(result.families[0].controlCount).toBe(5); // All controls in nested groups
    });

    it('should handle large catalog with many families', () => {
      const groups = [];
      for (let i = 1; i <= 20; i++) {
        groups.push({
          id: `family-${i}`,
          title: `Family ${i}`,
          controls: [
            { id: `f${i}-c1`, title: `Control 1` },
            { id: `f${i}-c2`, title: `Control 2` },
            { id: `f${i}-c3`, title: `Control 3` },
          ],
        });
      }

      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: { title: 'Large Catalog' },
          groups,
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.totalControls).toBe(60); // 20 families * 3 controls
      expect(result.families).toHaveLength(20);
    });

    it('should handle mixed top-level and grouped controls correctly', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: { title: 'Mixed' },
          controls: [
            { id: 'top-1' },
            { id: 'top-2' },
            { id: 'top-3' },
          ],
          groups: [
            {
              id: 'g1',
              title: 'Group 1',
              controls: [{ id: 'g1-c1' }, { id: 'g1-c2' }],
            },
            {
              id: 'g2',
              title: 'Group 2',
              controls: [{ id: 'g2-c1' }],
            },
          ],
        },
      });

      const result = analyzeCatalog(jsonCatalog, 'json');

      expect(result.totalControls).toBe(6);
      expect(result.families).toHaveLength(3); // g1, g2, and ungrouped

      const ungrouped = result.families.find(f => f.id === 'ungrouped');
      expect(ungrouped?.controlCount).toBe(3);
    });
  });

  // ========== TYPE CHECKING ==========

  describe('Return Type Validation', () => {
    it('should return correct CatalogAnalysis structure', () => {
      const jsonCatalog = JSON.stringify({
        catalog: {
          metadata: {
            title: 'Type Test',
            version: '1.0',
          },
          groups: [
            {
              id: 'test',
              title: 'Test Group',
              controls: [{ id: 'test-1', title: 'Test' }],
            },
          ],
        },
      });

      const result: CatalogAnalysis = analyzeCatalog(jsonCatalog, 'json');

      // Check structure
      expect(result).toHaveProperty('metadata');
      expect(result).toHaveProperty('totalControls');
      expect(result).toHaveProperty('families');

      // Check metadata structure
      expect(result.metadata).toHaveProperty('title');
      expect(typeof result.metadata.title).toBe('string');

      // Check families structure
      expect(Array.isArray(result.families)).toBe(true);
      if (result.families.length > 0) {
        expect(result.families[0]).toHaveProperty('id');
        expect(result.families[0]).toHaveProperty('title');
        expect(result.families[0]).toHaveProperty('controlCount');
        expect(typeof result.families[0].controlCount).toBe('number');
      }

      // Check totalControls is a number
      expect(typeof result.totalControls).toBe('number');
    });
  });
});
