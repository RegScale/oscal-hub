# OSCAL CLI Installation Guide

This directory contains automated installation scripts for OSCAL CLI on Mac, Linux, and Windows platforms.

## Quick Installation

### Mac/Linux

The easiest way to install OSCAL CLI on Mac or Linux is using the provided installation script:

```bash
# Download and run the installer
curl -fsSL https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.sh | bash

# Or download and run separately
curl -O https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.sh
chmod +x install.sh
./install.sh
```

**What the installer does:**
- Checks for Java 11+ installation
- Downloads the latest OSCAL CLI release from Maven Central
- Installs to `~/.oscal-cli`
- Provides instructions for adding to PATH

**Custom installation directory:**
```bash
OSCAL_CLI_HOME=/opt/oscal-cli ./install.sh
```

**Install specific version:**
```bash
OSCAL_CLI_VERSION=1.0.2 ./install.sh
```

**Add to PATH** (add to `~/.bashrc`, `~/.zshrc`, or `~/.bash_profile`):
```bash
export PATH="$PATH:$HOME/.oscal-cli/bin"
```

Then reload your shell configuration:
```bash
source ~/.bashrc  # or ~/.zshrc, or ~/.bash_profile
```

### Windows

Use PowerShell to run the Windows installer:

1. **Open PowerShell** (Right-click Start menu → Windows PowerShell)

2. **Allow script execution** (if needed):
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

3. **Download and run the installer**:
   ```powershell
   # Download the installer
   Invoke-WebRequest -Uri https://raw.githubusercontent.com/usnistgov/oscal-cli/main/installer/install.ps1 -OutFile install.ps1

   # Run the installer
   .\install.ps1
   ```

The installer will:
- Check for Java 11+ installation
- Download the latest OSCAL CLI release from Maven Central
- Install to `%USERPROFILE%\.oscal-cli`
- Optionally add to your PATH

**Custom installation directory:**
```powershell
.\install.ps1 -InstallDir "C:\Tools\oscal-cli"
```

**Install specific version:**
```powershell
.\install.ps1 -Version "1.0.2"
```

## Prerequisites

### Java Requirement

OSCAL CLI requires **Java 11 or higher**. The installation scripts will check your Java version automatically.

**Check your Java version:**
```bash
java -version
```

**Install Java** (if needed):
- **Recommended:** [Eclipse Adoptium](https://adoptium.net/) (formerly AdoptOpenJDK)
- **Alternative:** [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

### Mac/Linux Requirements

The install.sh script requires:
- `bash` shell
- `curl` or `wget` for downloading
- `unzip` for extracting archives
- Internet connection to download from Maven Central

### Windows Requirements

The install.ps1 script requires:
- **PowerShell 5.0+** (included with Windows 10/11)
- Internet connection to download from Maven Central
- Execution policy allowing scripts (script will provide instructions)

## Manual Installation

If you prefer to install manually or the automated scripts don't work for your environment:

### Step 1: Install Java

Ensure Java 11 or higher is installed:
```bash
java -version
```

If Java is not installed, download and install from [Adoptium](https://adoptium.net/).

### Step 2: Download OSCAL CLI

Download the latest release from [Maven Central](https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/):

1. Navigate to the version you want (e.g., `1.0.3/`)
2. Download the file: `cli-core-<version>-oscal-cli.zip`

**Direct download example (version 1.0.3):**
```bash
wget https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/1.0.3/cli-core-1.0.3-oscal-cli.zip
```

### Step 3: Extract the Archive

**Mac/Linux:**
```bash
mkdir -p ~/.oscal-cli
unzip cli-core-1.0.3-oscal-cli.zip -d ~/.oscal-cli
```

**Windows:**
```powershell
Expand-Archive -Path cli-core-1.0.3-oscal-cli.zip -DestinationPath $env:USERPROFILE\.oscal-cli
```

### Step 4: Add to PATH (Optional but Recommended)

**Mac/Linux:**

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, or `~/.bash_profile`):
```bash
export PATH="$PATH:$HOME/.oscal-cli/bin"
```

Then reload:
```bash
source ~/.bashrc  # or your shell profile
```

**Windows:**

1. Open **System Properties** → **Environment Variables**
2. Under **User variables**, edit the **Path** variable
3. Click **New** and add: `%USERPROFILE%\.oscal-cli\bin`
4. Click **OK** to save

### Step 5: Verify Installation

```bash
oscal-cli --version
```

If PATH is not configured, use the full path:
- **Mac/Linux:** `~/.oscal-cli/bin/oscal-cli --version`
- **Windows:** `%USERPROFILE%\.oscal-cli\bin\oscal-cli.bat --version`

## Verifying GPG Signatures (Optional)

For enhanced security, you can verify the GPG signature of the downloaded release:

```bash
# Download the signature file
wget https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/1.0.3/cli-core-1.0.3-oscal-cli.zip.asc

# Import the NIST OSCAL Release Engineering Key
gpg --keyserver hkps://pgp.mit.edu:443 --recv-keys 6387E83B4828A504

# Verify the signature
gpg --verify cli-core-1.0.3-oscal-cli.zip.asc cli-core-1.0.3-oscal-cli.zip
```

## Troubleshooting

### Java Not Found

**Error:**
```
Error: Java is not installed or not in PATH
```

**Solution:**
1. Install Java 11 or higher from [Adoptium](https://adoptium.net/)
2. Verify installation: `java -version`
3. Ensure Java is in your PATH

### Incorrect Java Version

**Error:**
```
Error: Java 11 or higher is required (found Java 8)
```

**Solution:**
Upgrade Java to version 11 or higher from [Adoptium](https://adoptium.net/).

### Download Fails

**Error:**
```
Error: Failed to download OSCAL CLI
```

**Possible solutions:**
1. Check your internet connection
2. Verify Maven Central is accessible: https://repo1.maven.org/maven2/
3. Try manual download from Maven Central
4. Check if your firewall or proxy is blocking the connection

### Permission Denied (Mac/Linux)

**Error:**
```
Permission denied
```

**Solution:**
Make the script executable:
```bash
chmod +x install.sh
```

Or run with bash explicitly:
```bash
bash install.sh
```

### PowerShell Execution Policy (Windows)

**Error:**
```
install.ps1 cannot be loaded because running scripts is disabled on this system
```

**Solution:**
Allow script execution for the current user:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Command Not Found After Installation

**Error:**
```
oscal-cli: command not found
```

**Solution:**
Either add OSCAL CLI to PATH (see instructions above) or use the full path:
- **Mac/Linux:** `~/.oscal-cli/bin/oscal-cli --help`
- **Windows:** `%USERPROFILE%\.oscal-cli\bin\oscal-cli.bat --help`

## Installation Locations

### Default Locations

- **Mac/Linux:** `~/.oscal-cli` (expands to `/Users/username/.oscal-cli` or `/home/username/.oscal-cli`)
- **Windows:** `%USERPROFILE%\.oscal-cli` (expands to `C:\Users\username\.oscal-cli`)

### Custom Locations

You can install to a custom location:

**Mac/Linux:**
```bash
OSCAL_CLI_HOME=/opt/oscal-cli ./install.sh
```

**Windows:**
```powershell
.\install.ps1 -InstallDir "C:\Tools\oscal-cli"
```

## Uninstallation

To remove OSCAL CLI:

1. **Delete the installation directory:**
   - **Mac/Linux:** `rm -rf ~/.oscal-cli`
   - **Windows:** `Remove-Item -Recurse -Force $env:USERPROFILE\.oscal-cli`

2. **Remove PATH entry** (if you added it):
   - **Mac/Linux:** Edit your shell profile and remove the export line
   - **Windows:** Remove from Environment Variables

## Next Steps

After installation, see the [USER_GUIDE.md](../USER_GUIDE.md) for:
- Getting started with OSCAL CLI
- Command reference
- Common operations and examples
- Working with different OSCAL models
- Advanced usage and troubleshooting

## Version Information

The install scripts default to the latest release. Available versions can be found at:
https://repo1.maven.org/maven2/gov/nist/secauto/oscal/tools/oscal-cli/cli-core/

Current latest version: **1.0.3**

## Support

If you encounter issues not covered here:

- **GitHub Issues:** https://github.com/usnistgov/oscal-cli/issues
- **OSCAL Documentation:** https://pages.nist.gov/OSCAL/
- **OSCAL Gitter Chat:** https://gitter.im/usnistgov-OSCAL/Lobby
- **Email:** oscal@nist.gov

## License

This project is in the worldwide public domain. See [LICENSE.md](../LICENSE.md) for details.
