# OSCAL CLI User Guide

A comprehensive guide for using the OSCAL Command Line Interface tool.

> **Installation:** For installation instructions, see [installer/README.md](installer/README.md)

## Table of Contents

- [Getting Started](#getting-started)
- [Command Reference](#command-reference)
- [Common Operations](#common-operations)
  - [Validating OSCAL Documents](#validating-oscal-documents)
  - [Converting Between Formats](#converting-between-formats)
  - [Resolving Profiles](#resolving-profiles)
  - [Creating System Authorizations](#creating-system-authorizations)
- [Working with OSCAL Models](#working-with-oscal-models)
  - [Catalogs](#catalogs)
  - [Profiles](#profiles)
  - [Component Definitions](#component-definitions)
  - [System Security Plans](#system-security-plans)
  - [Assessment Plans](#assessment-plans)
  - [Assessment Results](#assessment-results)
  - [Plan of Actions and Milestones](#plan-of-actions-and-milestones)
- [Advanced Usage](#advanced-usage)
- [Troubleshooting](#troubleshooting)

## Getting Started

This guide assumes you have already installed OSCAL CLI. If not, see [installer/README.md](installer/README.md).

Verify OSCAL CLI is working:

```bash
oscal-cli --version
```

Display general help:

```bash
oscal-cli --help
```

Display help for a specific command:

```bash
oscal-cli catalog --help
oscal-cli profile validate --help
```

## Command Reference

OSCAL CLI uses a hierarchical command structure:

```
oscal-cli <model> <operation> [options] <file>
```

### Available Models

- `catalog` - Security control catalogs (e.g., NIST SP 800-53)
- `profile` - Baselines that tailor catalogs
- `component-definition` - Component definitions
- `ssp` - System Security Plans
- `ap` - Assessment Plans (short form)
- `assessment-plan` - Assessment Plans (long form)
- `ar` - Assessment Results (short form)
- `assessment-results` - Assessment Results (long form)
- `poam` - Plan of Actions and Milestones
- `metaschema` - Metaschema definitions

### Common Operations

- `validate` - Validate an OSCAL document
- `convert` - Convert between XML, JSON, and YAML formats
- `resolve` - Resolve a profile into a catalog (profiles only)

### General Options

- `--help`, `-h` - Display help information
- `--version` - Display version information
- `--overwrite` - Overwrite output files if they exist
- `--as <FORMAT>` - Specify input format (xml, json, yaml)
- `--to <FORMAT>` - Specify output format (xml, json, yaml)

## Common Operations

### Validating OSCAL Documents

Validation checks that your OSCAL document is well-formed and complies with the OSCAL schema and constraints.

**Validate a catalog:**
```bash
oscal-cli catalog validate my-catalog.xml
```

**Validate a profile:**
```bash
oscal-cli profile validate my-profile.json
```

**Validate a System Security Plan:**
```bash
oscal-cli ssp validate my-ssp.yaml
```

**Expected output on success:**
```
The file 'my-catalog.xml' is valid.
```

**Expected output on validation failure:**
```
Validation errors found in 'my-catalog.xml':
  - Line 45: Missing required element 'title'
  - Line 120: Invalid UUID format
```

### Converting Between Formats

OSCAL CLI can convert between XML, JSON, and YAML formats.

**XML to JSON:**
```bash
oscal-cli catalog convert --to json catalog.xml catalog.json
```

**JSON to YAML:**
```bash
oscal-cli profile convert --to yaml profile.json profile.yaml
```

**YAML to XML:**
```bash
oscal-cli ssp convert --to xml ssp.yaml ssp.xml
```

**Auto-detect input format:**
```bash
# OSCAL CLI auto-detects the input format from file extension
oscal-cli catalog convert --to json my-catalog.xml my-catalog.json
```

**Specify input format explicitly:**
```bash
# Useful if file has wrong extension or no extension
oscal-cli catalog convert --as xml --to json catalog.txt catalog.json
```

**Overwrite existing output:**
```bash
oscal-cli catalog convert --to json catalog.xml catalog.json --overwrite
```

**Convert and output to stdout:**
```bash
# Omit output filename to print to console
oscal-cli profile convert --to json profile.xml
```

### Resolving Profiles

Profile resolution is a key OSCAL operation that transforms a Profile (which references and tailors a Catalog) into a resolved Catalog with only the selected and modified controls.

**Resolve a profile to XML catalog:**
```bash
oscal-cli profile resolve --to xml my-profile.json resolved-catalog.xml
```

**Resolve a profile to JSON catalog:**
```bash
oscal-cli profile resolve --to json my-profile.xml resolved-catalog.json
```

**Resolve and output to stdout:**
```bash
oscal-cli profile resolve --to json my-profile.xml
```

**What resolution does:**
- Follows profile imports to load referenced catalogs
- Selects controls based on include/exclude rules
- Applies parameter modifications
- Applies control modifications (add, set-parameter)
- Produces a stand-alone catalog with resolved controls

### Creating System Authorizations

> **Note:** System authorization creation is available through the OSCAL Hub web interface. This feature is not available in the CLI.

The web interface provides a comprehensive system for creating and managing system authorization documents:

#### Authorization Templates

Create reusable markdown templates with variable placeholders for authorization documents:

1. **Navigate to Authorizations** in the web interface
2. **Go to Templates tab**
3. **Create a new template** with markdown content
4. **Use variables** in your template with the syntax `{{ variable_name }}`

**Example Template:**
```markdown
# System Authorization for {{ system_name }}

**System Owner:** {{ system_owner }}
**Environment:** {{ environment }}

## Authorization Decision

This system is **{{ decision }}** for {{ environment }} operations.

**Authorizing Official:** {{ authorizing_official }}
**Date:** {{ authorization_date }}
**Period:** {{ authorization_period }}

## Risk Level
{{ risk_level }}

## Special Conditions
{{ conditions }}
```

**Variable Naming:**
- Variables can contain any text including spaces, commas, and special characters
- Examples: `{{ agency logo }}`, `{{ Low, Moderate, or High }}`, `{{ Federal Agency/Office }}`
- Wrap variable names in double curly braces: `{{ }}`

#### Creating Authorizations

Generate professional authorization documents from templates:

1. **Go to Authorizations tab** and click "Create New Authorization"
2. **Select an SSP** from your library
3. **Choose a template** to use
4. **Fill in all variables** - see live preview as you type
5. **Review and name** your authorization
6. **Create** - the system generates the completed document

**Benefits:**
- **Consistency** - Use the same template for multiple systems
- **Efficiency** - Fill variables instead of writing from scratch
- **Traceability** - Link authorizations to specific SSPs
- **Audit Trail** - Track who authorized what and when
- **Flexibility** - Templates support any variable naming convention

#### Managing Authorizations

- **View** completed authorization documents
- **Search** by name or SSP
- **Filter** authorizations by system
- **Track** authorization history and metadata
- **Delete** outdated authorizations

#### Common Use Cases

**FedRAMP Authorization:**
```markdown
{{agency logo}}

{{Insert Date}}

To: {{CSP System Owner Name}}

The {{Federal Agency/Office}} has completed review of the
{{Insert CSP and cloud service name}} system and grants
Authority to Operate based on categorization of
"{{Low, Moderate, or High}}".

SIGNED:
{{Authorizing Official}}
{{Title}}
```

**Internal System Authorization:**
```markdown
# Authorization: {{ system_name }}

Authorized by: {{ authorizing_official }}
Date: {{ authorization_date }}
Period: {{ authorization_period }}

Risk Level: {{ risk_level }}
Conditions: {{ conditions }}
```

For detailed documentation on the authorization system, see [docs/AUTHORIZATION-FEATURE-SUMMARY.md](docs/AUTHORIZATION-FEATURE-SUMMARY.md).

## Working with OSCAL Models

### Catalogs

Catalogs contain security controls, such as NIST SP 800-53 or ISO 27001.

**Validate a catalog:**
```bash
oscal-cli catalog validate nist-800-53-rev5-catalog.xml
```

**Convert catalog to JSON:**
```bash
oscal-cli catalog convert --to json catalog.xml catalog.json
```

**Example catalog operations:**
```bash
# Validate multiple formats
oscal-cli catalog validate catalog.xml
oscal-cli catalog validate catalog.json
oscal-cli catalog validate catalog.yaml

# Convert between all formats
oscal-cli catalog convert --to json catalog.xml catalog.json
oscal-cli catalog convert --to yaml catalog.json catalog.yaml
oscal-cli catalog convert --to xml catalog.yaml catalog.xml
```

### Profiles

Profiles define baselines by selecting and tailoring controls from catalogs.

**Validate a profile:**
```bash
oscal-cli profile validate my-baseline.json
```

**Convert profile format:**
```bash
oscal-cli profile convert --to yaml profile.xml profile.yaml
```

**Resolve profile to catalog:**
```bash
oscal-cli profile resolve --to json my-baseline.xml resolved-baseline.json
```

**Common profile workflow:**
```bash
# 1. Validate your profile
oscal-cli profile validate my-baseline.xml

# 2. Resolve it to see the resulting catalog
oscal-cli profile resolve --to json my-baseline.xml resolved.json

# 3. Validate the resolved catalog
oscal-cli catalog validate resolved.json
```

### Component Definitions

Component definitions describe implementation details for system components.

**Validate a component definition:**
```bash
oscal-cli component-definition validate components.json
```

**Convert format:**
```bash
oscal-cli component-definition convert --to yaml components.json components.yaml
```

### System Security Plans

System Security Plans (SSPs) document how a system meets security requirements.

**Validate an SSP:**
```bash
oscal-cli ssp validate system-security-plan.xml
```

**Convert SSP format:**
```bash
oscal-cli ssp convert --to json ssp.xml ssp.json
```

**Common SSP operations:**
```bash
# Validate SSP in different formats
oscal-cli ssp validate my-ssp.json

# Convert for sharing
oscal-cli ssp convert --to xml my-ssp.json my-ssp.xml --overwrite
```

### Assessment Plans

Assessment Plans define how security controls will be assessed.

**Using short form (ap):**
```bash
oscal-cli ap validate assessment-plan.json
oscal-cli ap convert --to yaml assessment-plan.json assessment-plan.yaml
```

**Using long form:**
```bash
oscal-cli assessment-plan validate assessment-plan.json
oscal-cli assessment-plan convert --to xml assessment-plan.json assessment-plan.xml
```

### Assessment Results

Assessment Results capture findings from security assessments.

**Using short form (ar):**
```bash
oscal-cli ar validate assessment-results.json
oscal-cli ar convert --to yaml assessment-results.json assessment-results.yaml
```

**Using long form:**
```bash
oscal-cli assessment-results validate assessment-results.json
```

### Plan of Actions and Milestones

POA&Ms track remediation activities for identified risks.

**Validate a POA&M:**
```bash
oscal-cli poam validate poam.json
```

**Convert POA&M format:**
```bash
oscal-cli poam convert --to xml poam.json poam.xml
```

## Advanced Usage

### Batch Processing

Process multiple files using shell loops:

**Validate all OSCAL files in a directory (Bash):**
```bash
for file in *.xml; do
    echo "Validating $file..."
    oscal-cli catalog validate "$file"
done
```

**Convert all XML files to JSON (Bash):**
```bash
for file in *.xml; do
    output="${file%.xml}.json"
    oscal-cli catalog convert --to json "$file" "$output" --overwrite
    echo "Converted $file to $output"
done
```

**Batch validate on Windows (PowerShell):**
```powershell
Get-ChildItem *.xml | ForEach-Object {
    Write-Host "Validating $_..."
    oscal-cli catalog validate $_.Name
}
```

### Pipeline Usage

Use OSCAL CLI in automation pipelines:

**CI/CD validation example:**
```bash
#!/bin/bash
# validate-oscal.sh - CI/CD script to validate all OSCAL files

EXIT_CODE=0

for file in oscal/*.xml oscal/*.json; do
    if [ -f "$file" ]; then
        echo "Validating $file..."
        if ! oscal-cli catalog validate "$file"; then
            echo "ERROR: Validation failed for $file"
            EXIT_CODE=1
        fi
    fi
done

exit $EXIT_CODE
```

### Working with HTTP URLs

OSCAL CLI can resolve profiles that import catalogs from URLs:

```bash
# Profile imports catalog from https://...
oscal-cli profile resolve --to json my-profile.xml resolved.json
```

The resolver will automatically fetch remote catalogs referenced in profile imports.

### Format Auto-Detection

OSCAL CLI automatically detects format based on file extension:
- `.xml` → XML format
- `.json` → JSON format
- `.yaml` or `.yml` → YAML format

If auto-detection fails or file has wrong extension, use `--as`:

```bash
oscal-cli catalog validate --as xml catalog.txt
oscal-cli profile convert --as json --to yaml profile.dat profile.yaml
```

## Troubleshooting

### Installation Issues

For installation-related problems (Java version, download failures, PATH configuration, etc.), see the [Troubleshooting section in installer/README.md](installer/README.md#troubleshooting).

### Validation Errors

**Error:**
```
Validation failed: Missing required element 'title'
```

**Solution:**
Review the validation error messages - they indicate specific OSCAL schema violations. Common issues:
- Missing required fields
- Invalid UUID formats
- Incorrect element nesting
- Type mismatches

Refer to [OSCAL documentation](https://pages.nist.gov/OSCAL/) for model requirements.

### Profile Resolution Fails

**Error:**
```
Unable to resolve profile: Failed to load imported catalog
```

**Possible causes and solutions:**

1. **Network issues** - Imported catalog URL is unreachable
   - Check internet connection
   - Verify the URL is accessible

2. **Invalid catalog reference** - Import href is incorrect
   - Verify the href attribute in your profile's import statement
   - Ensure the catalog file exists at the specified path

3. **Malformed catalog** - Referenced catalog has validation errors
   - Validate the imported catalog separately:
     ```bash
     oscal-cli catalog validate imported-catalog.xml
     ```

### File Permission Errors

**Error:**
```
Error: Permission denied writing to output file
```

**Solution:**
1. Check file permissions: `ls -l outputfile.xml`
2. Use `--overwrite` flag if file already exists
3. Ensure you have write permissions to the output directory

### Large File Performance

If processing large OSCAL files is slow:

1. Increase Java heap size:
   ```bash
   export JAVA_OPTS="-Xmx2g"
   oscal-cli catalog validate large-catalog.xml
   ```

2. For very large files, consider splitting into smaller components

### Getting Help

If you encounter issues not covered here:

1. Check the [OSCAL CLI GitHub Issues](https://github.com/RegScale/oscal-hub/issues)
2. Review [OSCAL documentation](https://pages.nist.gov/OSCAL/)
3. Ask questions on [OSCAL Gitter](https://gitter.im/usnistgov-OSCAL/Lobby)
4. Email: [oscal@nist.gov](mailto:oscal@nist.gov)

### Debug Mode

For detailed error information, check the console output. OSCAL CLI provides detailed stack traces for errors.

## Additional Resources

- **OSCAL Website:** https://pages.nist.gov/OSCAL/
- **OSCAL GitHub:** https://github.com/usnistgov/OSCAL
- **OSCAL CLI GitHub:** https://github.com/RegScale/oscal-hub
- **Metaschema:** https://github.com/usnistgov/metaschema
- **NIST SP 800-53:** https://csrc.nist.gov/publications/detail/sp/800-53/rev-5/final

## Examples Repository

Sample OSCAL files for testing:
- [OSCAL Content Repository](https://github.com/usnistgov/oscal-content)
- Example catalogs, profiles, and SSPs
- Test your CLI installation with these examples

## Contributing

To contribute to OSCAL CLI development, see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is in the worldwide public domain. See [LICENSE.md](LICENSE.md) for details.
