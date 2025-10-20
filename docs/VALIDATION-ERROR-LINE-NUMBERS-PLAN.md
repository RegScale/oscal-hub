# Validation Error Line Numbers Enhancement Plan

**Date**: 2025-10-20
**Status**: Planning
**Complexity**: Moderate to High
**Estimated Time**: 14-23 hours

## Problem Statement

The OSCAL CLI validator currently returns validation errors with error messages only, making it difficult for users to troubleshoot issues. Users need to manually search through their OSCAL documents to find the location of errors.

**Current behavior**:
```
Validation failed: required property 'title' missing
```

**Desired behavior**:
```
catalog.json:23:12: error: required property 'title' missing
     21 |     "groups": [
     22 |       {
  -> 23 |         "id": "bg1"
     24 |       }
     25 |     ]
```

## Current Architecture Analysis

### Existing Infrastructure

1. **ValidationError Model** (back-end/src/main/java/gov/nist/oscal/tools/api/model/ValidationError.java:4-8)
   - Already has `line`, `column`, and `path` fields
   - Fields exist but are not currently populated
   - No changes needed to data model

2. **Validation Flow**
   - **CLI**: `AbstractOscalValidationSubcommand` → Metaschema framework deserializer
   - **Back-end**: `ValidationService.validate()` → Metaschema framework deserializer
   - Both use `OscalBindingContext.newDeserializer()` which throws exceptions on validation failure

3. **Key Dependencies**
   - **metaschema-framework** (v0.12.2) - provides CLI framework, binding, and validation
   - **liboscal-java** (v3.0.3) - OSCAL model classes
   - **everit-json-schema** (v1.14.4) - JSON schema validation
   - **Saxon-HE** (v12.5) - XML processing with location tracking
   - **Jackson** - JSON parsing (likely used by Metaschema)
   - **SnakeYAML** - YAML parsing with Mark objects for location

### Current Limitations

1. **Exception-based validation**: Validation failures throw exceptions that may or may not contain location information
2. **Format-specific handling needed**: XML, JSON, and YAML each track location information differently
3. **Multiple validation layers**: Schema validation, Metaschema constraints, and deserialization errors all report differently
4. **No source context**: Even if line numbers were available, source code snippets are not shown

## Implementation Complexity Assessment

### Challenges

1. **Format-specific parsing requirements**:
   - **XML**: SAX/StAX parsers provide line numbers through `Locator` objects and `SAXParseException`
   - **JSON**: Jackson parser requires `JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION` configuration
   - **YAML**: SnakeYAML tracks positions with `Mark` objects containing line/column

2. **Metaschema framework abstraction**:
   - Validation happens inside the framework's deserializer
   - Need to investigate if the framework exposes location information
   - May need to customize error handlers or parse exception messages

3. **Multiple validation error sources**:
   - **Schema validation errors** (XSD for XML, JSON Schema for JSON) - usually have good location info
   - **Metaschema constraint errors** (business rules) - may need object model traversal to find location
   - **Parse errors** (malformed documents) - typically have excellent location info
   - **Binding errors** (type mismatches, missing required fields) - location depends on parser

4. **Error location mapping**:
   - Schema paths (XPath, JSON Pointer) need to map to actual file locations
   - Complex nested structures may make pinpointing difficult
   - Need to handle errors in imported/referenced documents

### Advantages

1. **Infrastructure already exists**: ValidationError model supports line/column/path fields
2. **Centralized validation logic**: Both CLI and back-end use similar code paths
3. **Extensive test resources**: Many invalid test files available in cli/src/test/resources/cli/
4. **Mature parser libraries**: All dependent parsers support location tracking

## Implementation Plan

### Phase 1: Investigation & Research (2-4 hours)

**Objective**: Understand what location information is available from existing libraries

1. **Examine Metaschema framework source code**
   - Clone metaschema-java repository
   - Review `IDeserializer` interface and implementations
   - Check if exceptions contain location information
   - Investigate `IConstraintValidator` and validation result reporting
   - Look for existing location tracking mechanisms

2. **Test parser capabilities**
   - Create standalone test program to parse OSCAL files with each format
   - Configure parsers for maximum location tracking
   - Deliberately introduce errors and capture exception details
   - Document what information is available from each exception type

3. **Document findings**
   - Create inventory of exception types and their location data
   - Identify gaps where location info is missing
   - Determine if custom parsing layer is needed

**Success Criteria**: Clear understanding of what's available and what needs to be built

### Phase 2: Back-end Enhanced Validation Service (4-6 hours)

**Objective**: Capture and report detailed error locations from validation

1. **Create error parsing utilities**
   - `LocationExtractor` interface with format-specific implementations
   - `XmlLocationExtractor` - extract from SAXParseException, XMLParseException
   - `JsonLocationExtractor` - extract from JsonParseException, JsonMappingException
   - `YamlLocationExtractor` - extract from MarkedYAMLException, ScannerException

2. **Implement source context reader**
   - `SourceContextReader` utility class
   - Given a file path and line number, extract surrounding lines
   - Configurable context size (default ±2 lines)
   - Handle edge cases (first/last lines, very short files)

3. **Update ValidationService** (back-end/src/main/java/gov/nist/oscal/tools/api/service/ValidationService.java)
   ```java
   // Current code (line 74):
   catch (Exception e) {
       result.setValid(false);
       ValidationError error = new ValidationError();
       error.setMessage(e.getMessage());
       error.setSeverity("error");
       result.addError(error);
   }

   // Enhanced code:
   catch (Exception e) {
       result.setValid(false);
       List<ValidationError> errors = parseValidationException(e, tempFile, format);
       errors.forEach(result::addError);
   }
   ```

4. **Implement parseValidationException method**
   - Use appropriate LocationExtractor based on format
   - Extract line, column, path from exception
   - Read source context if line number available
   - Handle multiple errors from schema validation
   - Gracefully fall back to message-only for unparseable exceptions

**Files to modify**:
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/ValidationService.java`

**Files to create**:
- `back-end/src/main/java/gov/nist/oscal/tools/api/util/LocationExtractor.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/util/XmlLocationExtractor.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/util/JsonLocationExtractor.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/util/YamlLocationExtractor.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/util/SourceContextReader.java`

**Success Criteria**: Back-end API returns ValidationError objects with line numbers and context

### Phase 3: CLI Error Output Enhancement (3-5 hours)

**Objective**: Display enhanced error information in CLI output

1. **Locate CLI error reporting code**
   - Find where `AbstractOscalValidationSubcommand` reports validation results
   - Likely in Metaschema framework's `AbstractValidateContentCommand` or `AbstractValidationCommandExecutor`
   - Determine if we can override error output formatting

2. **Create enhanced error formatter**
   - `ValidationErrorFormatter` class for CLI
   - Format similar to compiler errors: `file:line:column: severity: message`
   - Include source context with line numbers
   - Add visual indicator (arrow or caret) pointing to error location
   - Support color output using existing JANSI dependency

3. **Implement formatting options**
   ```
   Basic format:
   catalog.json:23:12: error: required property 'title' missing

   Verbose format (with --verbose flag):
   catalog.json:23:12: error: required property 'title' missing
        21 |     "groups": [
        22 |       {
     -> 23 |         "id": "bg1"
        24 |       }
        25 |     ]
   ```

4. **Handle multiple errors**
   - Group errors by file (for batch operations)
   - Sort errors by line number
   - Limit output if too many errors (e.g., "...and 50 more errors")

**Files to modify**:
- `cli/src/main/java/gov/nist/secauto/oscal/tools/cli/core/commands/oscal/AbstractOscalValidationSubcommand.java`

**Files to create**:
- `cli/src/main/java/gov/nist/secauto/oscal/tools/cli/core/util/ValidationErrorFormatter.java`

**Success Criteria**: CLI displays errors with line numbers and helpful context

### Phase 4: Handle Multiple Error Types (2-4 hours)

**Objective**: Ensure all types of validation errors report location information

1. **Schema validation errors** (XSD, JSON Schema)
   - These typically include location via exception messages
   - Parse XSD validation messages to extract line/column
   - Parse JSON Schema validation to extract JSON pointer path
   - Map JSON pointers to line numbers (may require re-parsing)

2. **Metaschema constraint validation errors**
   - These may only provide object path (e.g., /catalog/groups[0]/id)
   - Investigate if Metaschema framework provides location tracking
   - If not, may need to implement custom constraint validator
   - Consider using object path as fallback when line number unavailable

3. **Parse errors** (malformed documents)
   - Usually have excellent location information
   - Extract from parser exceptions directly
   - These should be the easiest to handle

4. **Binding/deserialization errors**
   - Type conversion errors
   - Missing required fields
   - Location info depends on how deeply parser is integrated
   - May need to enhance Metaschema framework integration

**Testing approach**:
- Create test files with each type of error
- Verify location information is captured correctly
- Document any error types that can't provide locations

**Success Criteria**: All common error types report locations when available

### Phase 5: Testing & Documentation (3-4 hours)

**Objective**: Validate the implementation and document the feature

1. **Update existing tests**
   - Modify `CLITest.java` to verify line numbers in error output
   - Test against all invalid files in cli/src/test/resources/cli/
   - Verify each format (XML, JSON, YAML)
   - Test each model type (catalog, profile, SSP, etc.)

2. **Create new unit tests**
   - `LocationExtractorTest` - test error parsing for each format
   - `SourceContextReaderTest` - test context extraction edge cases
   - `ValidationErrorFormatterTest` - test CLI output formatting

3. **Integration testing**
   - Test CLI with various invalid documents
   - Test back-end API endpoints with invalid payloads
   - Verify front-end displays enhanced errors correctly
   - Test edge cases: very large files, files with many errors

4. **Create documentation**
   - Update USER_GUIDE.md with examples of enhanced error output
   - Create VALIDATION-ERROR-REPORTING.md in docs/ explaining:
     - How error location tracking works
     - Examples for each format and error type
     - Known limitations (e.g., constraint errors may not have line numbers)
     - Troubleshooting tips for users

5. **Performance testing**
   - Measure impact of reading source files for context
   - Verify validation is not significantly slower
   - Test with large files (MB+ size)

**Files to create**:
- `cli/src/test/java/gov/nist/secauto/oscal/tools/cli/core/util/LocationExtractorTest.java`
- `cli/src/test/java/gov/nist/secauto/oscal/tools/cli/core/util/SourceContextReaderTest.java`
- `cli/src/test/java/gov/nist/secauto/oscal/tools/cli/core/util/ValidationErrorFormatterTest.java`
- `docs/VALIDATION-ERROR-REPORTING.md`

**Files to modify**:
- `cli/src/test/java/gov/nist/secauto/oscal/tools/cli/core/CLITest.java`
- `USER_GUIDE.md` (if exists, or installer/README.md)

**Success Criteria**: All tests pass, documentation is complete

## Time Estimates

| Phase | Task | Estimated Hours |
|-------|------|----------------|
| 1 | Investigation & Research | 2-4 hours |
| 2 | Back-end Enhanced Validation Service | 4-6 hours |
| 3 | CLI Error Output Enhancement | 3-5 hours |
| 4 | Handle Multiple Error Types | 2-4 hours |
| 5 | Testing & Documentation | 3-4 hours |
| **Total** | | **14-23 hours** |

## Risks & Mitigations

### Risk 1: Metaschema framework doesn't expose location information

**Impact**: High
**Probability**: Medium

**Mitigation strategies**:
- Parse exception messages using regex to extract location hints
- Implement custom validation layer that wraps Metaschema validation
- Contribute enhancement back to metaschema-java project
- As last resort, re-parse document with location-tracking parser on error

### Risk 2: Performance impact from reading source files

**Impact**: Medium
**Probability**: Low

**Mitigation strategies**:
- Only read source on error (success path unchanged)
- Cache file contents in memory during validation
- Make source context optional via configuration flag
- Limit context size to prevent excessive I/O

### Risk 3: Complex error types may not have location info

**Impact**: Medium
**Probability**: High (for constraint errors)

**Mitigation strategies**:
- Gracefully fall back to message-only errors
- Use JSON Pointer or XPath as "location" even without line numbers
- Clearly document which error types support line numbers
- Consider this MVP without constraint location tracking

### Risk 4: Different error formats from different validators

**Impact**: Medium
**Probability**: High

**Mitigation strategies**:
- Create unified error parsing layer with format-specific handlers
- Extract common patterns from exception messages
- Build comprehensive test suite covering all error types
- Document known variations and limitations

### Risk 5: Front-end display of enhanced errors

**Impact**: Low
**Probability**: Low

**Mitigation strategies**:
- ValidationError model already supports all fields
- Front-end can display what's available, ignore nulls
- Consider syntax-highlighted error display in future enhancement

## Success Metrics

1. **Coverage**: At least 80% of validation errors include line numbers
2. **Accuracy**: Line numbers are accurate within ±1 line
3. **Performance**: Validation with errors is not more than 20% slower
4. **Usability**: User feedback indicates errors are easier to troubleshoot
5. **Testing**: All existing tests pass, new tests cover error parsing

## Future Enhancements

### Phase 6 (Optional): Front-end Error Display

- Syntax-highlighted error display in web UI
- Click to jump to error line in embedded editor
- Side-by-side view of error and source

### Phase 7 (Optional): IDE Integration

- Language Server Protocol (LSP) support
- Real-time validation in VS Code, IntelliJ
- Quick fixes for common errors

### Phase 8 (Optional): Error Recovery Suggestions

- Suggest corrections for common mistakes
- "Did you mean...?" for typos
- Link to relevant OSCAL documentation

## Recommendation

This is a **worthwhile enhancement** that would significantly improve user experience. The moderate complexity is justified by:

- **High user value**: Drastically easier error troubleshooting
- **Existing infrastructure**: Models and validation flow already in place
- **Reusable across components**: Benefits CLI, back-end API, and front-end
- **Professional quality**: Matches error reporting in modern compilers and validators

**Recommended approach**:

1. **Start with Phase 1** (Investigation) to validate assumptions about available information
2. **Make go/no-go decision** based on what's feasible with Metaschema framework
3. **If feasible, proceed with Phases 2-3** for MVP (basic line numbers and context)
4. **Phase 4-5** can be done iteratively as error types are discovered
5. **Consider contributing** enhancements back to metaschema-java project

## References

- Metaschema Framework: https://github.com/usnistgov/metaschema-java
- OSCAL Java Library: https://github.com/usnistgov/liboscal-java
- Jackson location tracking: https://github.com/FasterXML/jackson-core
- Saxon error reporting: https://www.saxonica.com/documentation11/index.html
- JSON Schema validation: https://github.com/erosb/everit-json-schema

## Related Documentation

- CLAUDE.md - Project overview and development guidelines
- USER_GUIDE.md - End user documentation (will be updated with error examples)
- cli/src/test/resources/cli/ - Test files with various validation errors
