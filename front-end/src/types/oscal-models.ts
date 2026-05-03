/**
 * OSCAL 1.1.x JSON model types for the in-app builder.
 * Field names use kebab-case to match the OSCAL JSON schema verbatim.
 */

export type UUID = string;

export interface Prop {
  name: string;
  uuid?: UUID;
  ns?: string;
  value: string;
  class?: string;
  group?: string;
  remarks?: string;
}

export interface Link {
  href: string;
  rel?: string;
  'media-type'?: string;
  'resource-fragment'?: string;
  text?: string;
}

export interface DocumentId {
  scheme?: string;
  identifier: string;
}

export interface Address {
  type?: string;
  'addr-lines'?: string[];
  city?: string;
  state?: string;
  'postal-code'?: string;
  country?: string;
}

export interface TelephoneNumber {
  type?: string;
  number: string;
}

export interface ExternalId {
  scheme: string;
  id: string;
}

export interface Party {
  uuid: UUID;
  type: 'person' | 'organization';
  name?: string;
  'short-name'?: string;
  'external-ids'?: ExternalId[];
  props?: Prop[];
  links?: Link[];
  'email-addresses'?: string[];
  'telephone-numbers'?: TelephoneNumber[];
  addresses?: Address[];
  'location-uuids'?: UUID[];
  'member-of-organizations'?: UUID[];
  remarks?: string;
}

export interface Location {
  uuid: UUID;
  title?: string;
  address?: Address;
  'email-addresses'?: string[];
  'telephone-numbers'?: TelephoneNumber[];
  urls?: string[];
  props?: Prop[];
  links?: Link[];
  remarks?: string;
}

export interface Role {
  id: string;
  title: string;
  'short-name'?: string;
  description?: string;
  props?: Prop[];
  links?: Link[];
  remarks?: string;
}

export interface ResponsibleParty {
  'role-id': string;
  'party-uuids': UUID[];
  props?: Prop[];
  links?: Link[];
  remarks?: string;
}

export interface Revision {
  title?: string;
  published?: string;
  'last-modified'?: string;
  version: string;
  'oscal-version'?: string;
  props?: Prop[];
  links?: Link[];
  remarks?: string;
}

export interface Metadata {
  title: string;
  published?: string;
  'last-modified': string;
  version: string;
  'oscal-version': string;
  revisions?: Revision[];
  'document-ids'?: DocumentId[];
  props?: Prop[];
  links?: Link[];
  roles?: Role[];
  locations?: Location[];
  parties?: Party[];
  'responsible-parties'?: ResponsibleParty[];
  remarks?: string;
}

export interface ParamConstraintTest {
  expression: string;
  remarks?: string;
}

export interface ParamConstraint {
  description?: string;
  tests?: ParamConstraintTest[];
}

export interface ParamGuideline {
  prose: string;
}

export interface ParamSelection {
  'how-many'?: 'one' | 'one-or-more';
  choice?: string[];
}

export interface Param {
  id: string;
  class?: string;
  'depends-on'?: string;
  props?: Prop[];
  links?: Link[];
  label?: string;
  usage?: string;
  constraints?: ParamConstraint[];
  guidelines?: ParamGuideline[];
  values?: string[];
  select?: ParamSelection;
  remarks?: string;
}

export interface Part {
  id?: string;
  name: string;
  ns?: string;
  class?: string;
  title?: string;
  props?: Prop[];
  prose?: string;
  parts?: Part[];
  links?: Link[];
}

export interface Control {
  id: string;
  class?: string;
  title: string;
  params?: Param[];
  props?: Prop[];
  links?: Link[];
  parts?: Part[];
  controls?: Control[];
}

export interface Group {
  id?: string;
  class?: string;
  title: string;
  params?: Param[];
  props?: Prop[];
  links?: Link[];
  parts?: Part[];
  groups?: Group[];
  controls?: Control[];
}

export interface Hash {
  algorithm: string;
  value: string;
}

export interface Rlink {
  href: string;
  'media-type'?: string;
  hashes?: Hash[];
}

export interface Citation {
  text: string;
  props?: Prop[];
  links?: Link[];
}

export interface Base64Resource {
  filename?: string;
  'media-type'?: string;
  value: string;
}

export interface Resource {
  uuid: UUID;
  title?: string;
  description?: string;
  props?: Prop[];
  'document-ids'?: DocumentId[];
  citation?: Citation;
  rlinks?: Rlink[];
  base64?: Base64Resource;
  remarks?: string;
}

export interface BackMatter {
  resources?: Resource[];
}

// ===================================================================
// Catalog
// ===================================================================
export interface Catalog {
  uuid: UUID;
  metadata: Metadata;
  params?: Param[];
  controls?: Control[];
  groups?: Group[];
  'back-matter'?: BackMatter;
}

export interface CatalogDocument {
  catalog: Catalog;
}

// ===================================================================
// Profile
// ===================================================================
export interface SelectControlMatching {
  pattern: string;
}

export interface SelectControl {
  'with-child-controls'?: 'yes' | 'no';
  'with-ids'?: string[];
  matching?: SelectControlMatching[];
}

export interface ProfileImport {
  href: string;
  'include-all'?: Record<string, never>;
  'include-controls'?: SelectControl[];
  'exclude-controls'?: SelectControl[];
}

export interface MergeCombine {
  method: 'use-first' | 'merge' | 'keep';
}

export interface InsertControls {
  order?: 'keep' | 'ascending' | 'descending';
  'include-all'?: Record<string, never>;
  'include-controls'?: SelectControl[];
  'exclude-controls'?: SelectControl[];
}

export interface CustomGroup {
  id?: string;
  class?: string;
  title?: string;
  params?: Param[];
  props?: Prop[];
  links?: Link[];
  parts?: Part[];
  groups?: CustomGroup[];
  'insert-controls'?: InsertControls[];
}

export interface MergeCustom {
  groups?: CustomGroup[];
  'insert-controls'?: InsertControls[];
}

export interface ProfileMerge {
  combine?: MergeCombine;
  flat?: Record<string, never>;
  'as-is'?: boolean;
  custom?: MergeCustom;
}

export interface SetParameter {
  'param-id': string;
  class?: string;
  'depends-on'?: string;
  props?: Prop[];
  links?: Link[];
  label?: string;
  usage?: string;
  constraints?: ParamConstraint[];
  guidelines?: ParamGuideline[];
  values?: string[];
  select?: ParamSelection;
}

export interface AlterRemove {
  'by-name'?: string;
  'by-class'?: string;
  'by-id'?: string;
  'by-item-name'?: string;
  'by-ns'?: string;
}

export interface AlterAdd {
  position?: 'before' | 'after' | 'starting' | 'ending';
  'by-id'?: string;
  title?: string;
  params?: Param[];
  props?: Prop[];
  links?: Link[];
  parts?: Part[];
}

export interface Alter {
  'control-id': string;
  removes?: AlterRemove[];
  adds?: AlterAdd[];
}

export interface ProfileModify {
  'set-parameters'?: SetParameter[];
  alters?: Alter[];
}

export interface Profile {
  uuid: UUID;
  metadata: Metadata;
  imports: ProfileImport[];
  merge?: ProfileMerge;
  modify?: ProfileModify;
  'back-matter'?: BackMatter;
}

export interface ProfileDocument {
  profile: Profile;
}

// ===================================================================
// API request/response shapes (mirror backend DTOs)
// ===================================================================
export interface CatalogRequest {
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  filename: string;
  jsonContent: string;
  oscalUuid?: string;
  groupCount?: number;
  controlCount?: number;
  paramCount?: number;
  draft?: boolean;
}

export interface CatalogResponse {
  id: number;
  oscalUuid: string;
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  storagePath: string;
  filename: string;
  fileSize: number;
  groupCount?: number;
  controlCount?: number;
  paramCount?: number;
  draft: boolean;
  createdBy: string;
  createdAt: string;
  lastUpdatedBy?: string;
  updatedAt: string;
}

export interface ProfileBuildRequest {
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  filename: string;
  jsonContent: string;
  oscalUuid?: string;
  importCount?: number;
  controlCount?: number;
  alterCount?: number;
  draft?: boolean;
}

export interface ProfileBuildResponse {
  id: number;
  oscalUuid: string;
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  storagePath: string;
  filename: string;
  fileSize: number;
  importCount?: number;
  controlCount?: number;
  alterCount?: number;
  draft: boolean;
  createdBy: string;
  createdAt: string;
  lastUpdatedBy?: string;
  updatedAt: string;
}

// ===================================================================
// Generic OSCAL document types (SSP, AP, AR, POAM)
// ===================================================================

export type GenericOscalModelSlug =
  | 'system-security-plan'
  | 'assessment-plan'
  | 'assessment-results'
  | 'plan-of-action-and-milestones';

export interface OscalDocumentRequest {
  modelType: GenericOscalModelSlug;
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  filename: string;
  jsonContent: string;
  oscalUuid?: string;
  statsJson?: string;
  draft?: boolean;
}

export interface OscalDocumentResponse {
  id: number;
  oscalUuid: string;
  modelType: GenericOscalModelSlug;
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  storagePath: string;
  filename: string;
  fileSize: number;
  statsJson?: string;
  draft: boolean;
  createdBy: string;
  createdAt: string;
  lastUpdatedBy?: string;
  updatedAt: string;
}

// ===================================================================
// Helpers
// ===================================================================
export const CURRENT_OSCAL_VERSION = '1.1.3';

export function generateUuid(): UUID {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  // Fallback: RFC 4122 v4 generator
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function nowIsoUtc(): string {
  return new Date().toISOString();
}

export function emptyMetadata(title = ''): Metadata {
  return {
    title,
    'last-modified': nowIsoUtc(),
    version: '1.0.0',
    'oscal-version': CURRENT_OSCAL_VERSION,
  };
}

export function emptyCatalog(title = 'New Catalog'): Catalog {
  return {
    uuid: generateUuid(),
    metadata: emptyMetadata(title),
  };
}

export function emptyProfile(title = 'New Profile'): Profile {
  return {
    uuid: generateUuid(),
    metadata: emptyMetadata(title),
    imports: [],
  };
}

/** Recursively count controls inside a group/control tree. */
export function countControls(items: { controls?: Control[]; groups?: Group[] }[]): number {
  let total = 0;
  for (const item of items) {
    if (item.controls) {
      for (const c of item.controls) {
        total += 1;
        if (c.controls && c.controls.length > 0) {
          total += countControls([{ controls: c.controls }]);
        }
      }
    }
    if ('groups' in item && item.groups) {
      total += countControls(item.groups);
    }
  }
  return total;
}

export function countCatalogControls(catalog: Catalog): number {
  return countControls([catalog]);
}

export function countCatalogGroups(catalog: Catalog): number {
  let total = 0;
  function walk(groups?: Group[]) {
    if (!groups) return;
    for (const g of groups) {
      total += 1;
      walk(g.groups);
    }
  }
  walk(catalog.groups);
  return total;
}

export function countCatalogParams(catalog: Catalog): number {
  let total = catalog.params?.length ?? 0;
  function walkControl(controls?: Control[]) {
    if (!controls) return;
    for (const c of controls) {
      total += c.params?.length ?? 0;
      walkControl(c.controls);
    }
  }
  function walkGroup(groups?: Group[]) {
    if (!groups) return;
    for (const g of groups) {
      total += g.params?.length ?? 0;
      walkControl(g.controls);
      walkGroup(g.groups);
    }
  }
  walkControl(catalog.controls);
  walkGroup(catalog.groups);
  return total;
}

export function countProfileControls(profile: Profile): number {
  let total = 0;
  for (const imp of profile.imports) {
    total += imp['include-controls']?.reduce((sum, sc) => sum + (sc['with-ids']?.length ?? 0), 0) ?? 0;
  }
  return total;
}

// ===================================================================
// Parsing helpers (for importing existing OSCAL JSON)
// ===================================================================

function isObject(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

/**
 * Parse a value into a Catalog. Accepts either a wrapped `{catalog: {...}}`
 * document or a bare catalog object. Throws with a descriptive message if the
 * required fields are missing.
 */
export function parseCatalog(input: unknown): Catalog {
  if (!isObject(input)) throw new Error('Expected a JSON object.');
  const root: unknown = isObject(input.catalog) ? input.catalog : input;
  if (!isObject(root)) throw new Error('Could not find a catalog object.');
  if (typeof root.uuid !== 'string') throw new Error('Catalog is missing required "uuid".');
  if (!isObject(root.metadata)) throw new Error('Catalog is missing required "metadata".');
  const metadata = root.metadata as Partial<Metadata>;
  if (typeof metadata.title !== 'string') throw new Error('metadata.title is required.');
  if (typeof metadata['oscal-version'] !== 'string') {
    throw new Error('metadata.oscal-version is required.');
  }
  return {
    uuid: root.uuid,
    metadata: {
      title: metadata.title,
      'last-modified': metadata['last-modified'] ?? nowIsoUtc(),
      version: metadata.version ?? '1.0.0',
      'oscal-version': metadata['oscal-version'],
      published: metadata.published,
      revisions: metadata.revisions,
      'document-ids': metadata['document-ids'],
      props: metadata.props,
      links: metadata.links,
      roles: metadata.roles,
      locations: metadata.locations,
      parties: metadata.parties,
      'responsible-parties': metadata['responsible-parties'],
      remarks: metadata.remarks,
    },
    params: Array.isArray(root.params) ? (root.params as Param[]) : undefined,
    controls: Array.isArray(root.controls) ? (root.controls as Control[]) : undefined,
    groups: Array.isArray(root.groups) ? (root.groups as Group[]) : undefined,
    'back-matter': isObject(root['back-matter']) ? (root['back-matter'] as BackMatter) : undefined,
  };
}

// ===================================================================
// Generic-model helpers (SSP, AP, AR, POAM)
// ===================================================================

const MODEL_DISPLAY: Record<GenericOscalModelSlug, { label: string; rootKey: string; importKey?: string }> = {
  'system-security-plan': { label: 'System Security Plan', rootKey: 'system-security-plan', importKey: 'import-profile' },
  'assessment-plan': { label: 'Assessment Plan', rootKey: 'assessment-plan', importKey: 'import-ssp' },
  'assessment-results': { label: 'Assessment Results', rootKey: 'assessment-results', importKey: 'import-ap' },
  'plan-of-action-and-milestones': { label: 'Plan of Action and Milestones', rootKey: 'plan-of-action-and-milestones', importKey: 'import-ssp' },
};

export function modelLabel(slug: GenericOscalModelSlug): string {
  return MODEL_DISPLAY[slug].label;
}

export function modelRootKey(slug: GenericOscalModelSlug): string {
  return MODEL_DISPLAY[slug].rootKey;
}

export function modelImportKey(slug: GenericOscalModelSlug): string | undefined {
  return MODEL_DISPLAY[slug].importKey;
}

/**
 * Returns a starter JSON skeleton for the given model — wrapped in the model
 * root key (e.g. `{ "system-security-plan": { ... } }`) — that an OSCAL
 * resolver can chew on as a draft.
 */
export function emptyOscalDocument(slug: GenericOscalModelSlug, title = 'New document'): Record<string, unknown> {
  const meta = emptyMetadata(title);
  const uuid = generateUuid();

  switch (slug) {
    case 'system-security-plan':
      return {
        'system-security-plan': {
          uuid,
          metadata: meta,
          'import-profile': { href: '' },
          'system-characteristics': {
            'system-name': title,
            description: '',
            'system-ids': [{ id: '' }],
            'security-sensitivity-level': 'moderate',
            'system-information': {
              'information-types': [],
            },
            'security-impact-level': {
              'security-objective-confidentiality': 'moderate',
              'security-objective-integrity': 'moderate',
              'security-objective-availability': 'moderate',
            },
            status: { state: 'operational' },
            'authorization-boundary': { description: '' },
          },
          'system-implementation': {
            users: [],
            components: [],
          },
          'control-implementation': {
            description: '',
            'implemented-requirements': [],
          },
        },
      };
    case 'assessment-plan':
      return {
        'assessment-plan': {
          uuid,
          metadata: meta,
          'import-ssp': { href: '' },
          'reviewed-controls': {
            'control-selections': [{ 'include-all': {} }],
          },
          tasks: [],
        },
      };
    case 'assessment-results':
      return {
        'assessment-results': {
          uuid,
          metadata: meta,
          'import-ap': { href: '' },
          results: [],
        },
      };
    case 'plan-of-action-and-milestones':
      return {
        'plan-of-action-and-milestones': {
          uuid,
          metadata: meta,
          'import-ssp': { href: '' },
          observations: [],
          risks: [],
          findings: [],
          'poam-items': [],
        },
      };
  }
}

/** Counts that the list view shows as badges on each card. */
export function summarizeOscalDocument(slug: GenericOscalModelSlug, content: unknown): Array<{ label: string; value: number }> {
  const root = isObject(content) ? (content as Record<string, unknown>)[modelRootKey(slug)] : undefined;
  const body = isObject(root) ? root : isObject(content) ? content : {};
  const arrLen = (k: string): number => Array.isArray((body as Record<string, unknown>)[k])
    ? ((body as Record<string, unknown>)[k] as unknown[]).length
    : 0;

  switch (slug) {
    case 'system-security-plan':
      return [
        {
          label: 'components',
          value: Array.isArray(((body as Record<string, unknown>)['system-implementation'] as Record<string, unknown> | undefined)?.components)
            ? (((body as Record<string, unknown>)['system-implementation'] as { components: unknown[] }).components.length)
            : 0,
        },
        {
          label: 'requirements',
          value: Array.isArray(((body as Record<string, unknown>)['control-implementation'] as Record<string, unknown> | undefined)?.['implemented-requirements'])
            ? (((body as Record<string, unknown>)['control-implementation'] as { 'implemented-requirements': unknown[] })['implemented-requirements'].length)
            : 0,
        },
      ];
    case 'assessment-plan':
      return [
        { label: 'tasks', value: arrLen('tasks') },
      ];
    case 'assessment-results':
      return [
        { label: 'results', value: arrLen('results') },
      ];
    case 'plan-of-action-and-milestones':
      return [
        { label: 'items', value: arrLen('poam-items') },
        { label: 'risks', value: arrLen('risks') },
      ];
  }
}

/**
 * Parse a value into a Profile. Accepts either a wrapped `{profile: {...}}`
 * document or a bare profile object.
 */
export function parseProfile(input: unknown): Profile {
  if (!isObject(input)) throw new Error('Expected a JSON object.');
  const root: unknown = isObject(input.profile) ? input.profile : input;
  if (!isObject(root)) throw new Error('Could not find a profile object.');
  if (typeof root.uuid !== 'string') throw new Error('Profile is missing required "uuid".');
  if (!isObject(root.metadata)) throw new Error('Profile is missing required "metadata".');
  if (!Array.isArray(root.imports) || root.imports.length === 0) {
    throw new Error('Profile must have at least one import.');
  }
  const metadata = root.metadata as Partial<Metadata>;
  if (typeof metadata.title !== 'string') throw new Error('metadata.title is required.');
  if (typeof metadata['oscal-version'] !== 'string') {
    throw new Error('metadata.oscal-version is required.');
  }
  return {
    uuid: root.uuid,
    metadata: {
      title: metadata.title,
      'last-modified': metadata['last-modified'] ?? nowIsoUtc(),
      version: metadata.version ?? '1.0.0',
      'oscal-version': metadata['oscal-version'],
      published: metadata.published,
      revisions: metadata.revisions,
      'document-ids': metadata['document-ids'],
      props: metadata.props,
      links: metadata.links,
      roles: metadata.roles,
      locations: metadata.locations,
      parties: metadata.parties,
      'responsible-parties': metadata['responsible-parties'],
      remarks: metadata.remarks,
    },
    imports: root.imports as ProfileImport[],
    merge: isObject(root.merge) ? (root.merge as ProfileMerge) : undefined,
    modify: isObject(root.modify) ? (root.modify as ProfileModify) : undefined,
    'back-matter': isObject(root['back-matter']) ? (root['back-matter'] as BackMatter) : undefined,
  };
}
