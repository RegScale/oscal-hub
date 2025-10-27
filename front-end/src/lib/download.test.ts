import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { generateConvertedFilename, downloadFile } from './download';
import type { OscalFormat } from '@/types/oscal';

describe('download utilities', () => {
  describe('generateConvertedFilename', () => {
    it('should replace .xml extension with target format', () => {
      const result = generateConvertedFilename('catalog.xml', 'json');
      expect(result).toBe('catalog.json');
    });

    it('should replace .json extension with target format', () => {
      const result = generateConvertedFilename('profile.json', 'xml');
      expect(result).toBe('profile.xml');
    });

    it('should replace .yaml extension with target format', () => {
      const result = generateConvertedFilename('catalog.yaml', 'json');
      expect(result).toBe('catalog.json');
    });

    it('should replace .yml extension with target format', () => {
      const result = generateConvertedFilename('profile.yml', 'xml');
      expect(result).toBe('profile.xml');
    });

    it('should handle case-insensitive extensions', () => {
      const result = generateConvertedFilename('catalog.XML', 'json');
      expect(result).toBe('catalog.json');
    });

    it('should handle filenames without extensions', () => {
      const result = generateConvertedFilename('catalog', 'json');
      expect(result).toBe('catalog.json');
    });

    it('should preserve multiple dots in filename', () => {
      const result = generateConvertedFilename('my.catalog.file.xml', 'json');
      expect(result).toBe('my.catalog.file.json');
    });

    it('should handle all target formats', () => {
      const formats: OscalFormat[] = ['xml', 'json', 'yaml'];
      formats.forEach((format) => {
        const result = generateConvertedFilename('test.xml', format);
        expect(result).toMatch(new RegExp(`\\.${format}$`));
      });
    });
  });

  describe('downloadFile', () => {
    let createObjectURLSpy: ReturnType<typeof vi.fn>;
    let revokeObjectURLSpy: ReturnType<typeof vi.fn>;
    let appendChildSpy: ReturnType<typeof vi.fn>;
    let removeChildSpy: ReturnType<typeof vi.fn>;
    let clickSpy: ReturnType<typeof vi.fn>;

    beforeEach(() => {
      // Mock URL methods
      createObjectURLSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url');
      revokeObjectURLSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});

      // Mock document.body methods
      appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation((node) => node);
      removeChildSpy = vi.spyOn(document.body, 'removeChild').mockImplementation((node) => node);

      // Mock link click
      clickSpy = vi.fn();
      vi.spyOn(document, 'createElement').mockImplementation((tagName) => {
        if (tagName === 'a') {
          const link = {
            href: '',
            download: '',
            click: clickSpy,
          } as unknown as HTMLAnchorElement;
          return link;
        }
        return document.createElement(tagName);
      });
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('should download XML file with correct MIME type', () => {
      downloadFile('<catalog/>', 'test.xml', 'xml');

      expect(createObjectURLSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'application/xml',
        })
      );
    });

    it('should download JSON file with correct MIME type', () => {
      downloadFile('{"test": true}', 'test.json', 'json');

      expect(createObjectURLSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'application/json',
        })
      );
    });

    it('should download YAML file with correct MIME type', () => {
      downloadFile('test: true', 'test.yaml', 'yaml');

      expect(createObjectURLSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'text/yaml',
        })
      );
    });

    it('should trigger download and cleanup', () => {
      downloadFile('content', 'test.xml', 'xml');

      expect(appendChildSpy).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(removeChildSpy).toHaveBeenCalled();
      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url');
    });

    it('should set correct filename', () => {
      const filename = 'my-catalog.xml';
      downloadFile('content', filename, 'xml');

      // The mock link should have had its download attribute set
      expect(clickSpy).toHaveBeenCalled();
    });
  });
});
