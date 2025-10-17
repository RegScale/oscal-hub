import type { OscalFormat } from '@/types/oscal';

/**
 * Triggers a download of the given content as a file
 */
export function downloadFile(
  content: string,
  filename: string,
  format: OscalFormat
): void {
  // Determine MIME type based on format
  const mimeTypes: Record<OscalFormat, string> = {
    xml: 'application/xml',
    json: 'application/json',
    yaml: 'text/yaml',
  };

  const mimeType = mimeTypes[format];

  // Create blob
  const blob = new Blob([content], { type: mimeType });

  // Create download link
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;

  // Trigger download
  document.body.appendChild(link);
  link.click();

  // Cleanup
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * Generate a filename for a converted file
 */
export function generateConvertedFilename(
  originalFilename: string,
  targetFormat: OscalFormat
): string {
  // Remove existing extension
  const nameWithoutExt = originalFilename.replace(/\.(xml|json|ya?ml)$/i, '');

  // Add new extension
  const extensions: Record<OscalFormat, string> = {
    xml: 'xml',
    json: 'json',
    yaml: 'yaml',
  };

  return `${nameWithoutExt}.${extensions[targetFormat]}`;
}
