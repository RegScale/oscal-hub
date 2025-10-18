import type { OscalFormat } from '@/types/oscal';
import { parse as parseYaml } from 'yaml';

export interface CatalogMetadata {
  title: string;
  version?: string;
  lastModified?: string;
  oscalVersion?: string;
  published?: string;
}

export interface ControlFamily {
  id: string;
  title: string;
  controlCount: number;
}

export interface CatalogAnalysis {
  metadata: CatalogMetadata;
  totalControls: number;
  families: ControlFamily[];
}

function parseXmlString(xmlString: string): Document {
  const parser = new DOMParser();
  return parser.parseFromString(xmlString, 'application/xml');
}

function countControlsInGroup(groupElement: Element): number {
  const controls = groupElement.querySelectorAll(':scope > control');
  let count = controls.length;

  // Recursively count controls in nested groups
  const subGroups = groupElement.querySelectorAll(':scope > group');
  subGroups.forEach(subGroup => {
    count += countControlsInGroup(subGroup);
  });

  return count;
}

function extractCatalogFromXml(doc: Document): CatalogAnalysis {
  const catalog = doc.querySelector('catalog');
  if (!catalog) {
    throw new Error('Invalid catalog XML: No catalog element found');
  }

  const metadata = catalog.querySelector('metadata');
  const title = metadata?.querySelector('title')?.textContent || 'Untitled Catalog';
  const version = metadata?.querySelector('version')?.textContent;
  const lastModified = metadata?.querySelector('last-modified')?.textContent;
  const oscalVersion = metadata?.querySelector('oscal-version')?.textContent;
  const published = metadata?.querySelector('published')?.textContent;

  const catalogMetadata: CatalogMetadata = {
    title,
    version,
    lastModified,
    oscalVersion,
    published,
  };

  // Count controls in groups (families)
  const families: ControlFamily[] = [];
  const groups = catalog.querySelectorAll(':scope > group');

  groups.forEach(group => {
    const id = group.getAttribute('id') || 'unknown';
    const titleElement = group.querySelector(':scope > title');
    const familyTitle = titleElement?.textContent || id;
    const controlCount = countControlsInGroup(group);

    families.push({
      id,
      title: familyTitle,
      controlCount,
    });
  });

  // Count top-level controls (not in groups)
  const topLevelControls = catalog.querySelectorAll(':scope > control');
  const topLevelCount = topLevelControls.length;

  const totalControls = families.reduce((sum, f) => sum + f.controlCount, 0) + topLevelCount;

  // If there are top-level controls, add them as a separate "family"
  if (topLevelCount > 0) {
    families.push({
      id: 'ungrouped',
      title: 'Ungrouped Controls',
      controlCount: topLevelCount,
    });
  }

  return {
    metadata: catalogMetadata,
    totalControls,
    families,
  };
}

function countControlsInJsonGroup(group: any): number {
  let count = 0;

  if (group.controls && Array.isArray(group.controls)) {
    count = group.controls.length;
  }

  if (group.groups && Array.isArray(group.groups)) {
    group.groups.forEach((subGroup: any) => {
      count += countControlsInJsonGroup(subGroup);
    });
  }

  return count;
}

function extractCatalogFromJson(obj: any): CatalogAnalysis {
  const catalog = obj.catalog;
  if (!catalog) {
    throw new Error('Invalid catalog JSON: No catalog property found');
  }

  const metadata = catalog.metadata || {};
  const catalogMetadata: CatalogMetadata = {
    title: metadata.title || 'Untitled Catalog',
    version: metadata.version,
    lastModified: metadata['last-modified'],
    oscalVersion: metadata['oscal-version'],
    published: metadata.published,
  };

  const families: ControlFamily[] = [];

  if (catalog.groups && Array.isArray(catalog.groups)) {
    catalog.groups.forEach((group: any) => {
      const id = group.id || 'unknown';
      const title = group.title || id;
      const controlCount = countControlsInJsonGroup(group);

      families.push({
        id,
        title,
        controlCount,
      });
    });
  }

  // Count top-level controls
  let topLevelCount = 0;
  if (catalog.controls && Array.isArray(catalog.controls)) {
    topLevelCount = catalog.controls.length;
  }

  const totalControls = families.reduce((sum, f) => sum + f.controlCount, 0) + topLevelCount;

  // If there are top-level controls, add them as a separate "family"
  if (topLevelCount > 0) {
    families.push({
      id: 'ungrouped',
      title: 'Ungrouped Controls',
      controlCount: topLevelCount,
    });
  }

  return {
    metadata: catalogMetadata,
    totalControls,
    families,
  };
}

export function analyzeCatalog(content: string, format: OscalFormat): CatalogAnalysis {
  try {
    if (format === 'xml') {
      const doc = parseXmlString(content);
      return extractCatalogFromXml(doc);
    } else if (format === 'json') {
      const obj = JSON.parse(content);
      return extractCatalogFromJson(obj);
    } else if (format === 'yaml') {
      const obj = parseYaml(content);
      return extractCatalogFromJson(obj);
    } else {
      throw new Error(`Unsupported format: ${format}`);
    }
  } catch (error) {
    console.error('Error analyzing catalog:', error);
    throw new Error(`Failed to analyze catalog: ${error instanceof Error ? error.message : 'Unknown error'}`);
  }
}
