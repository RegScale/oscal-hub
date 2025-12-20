# Maven Toolchains Setup

This project uses Maven Toolchains to ensure consistent Java 21 compilation across all developer environments, regardless of the system's default Java version.

## Why Toolchains?

Maven Toolchains allow you to:
- Use a specific JDK version for compilation independent of the JDK running Maven
- Maintain consistent builds across different developer machines
- Support multiple Java versions without changing system environment variables

## Prerequisites

You need Java 21 installed on your system. We recommend Eclipse Temurin (formerly AdoptOpenJDK).

### Installing Java 21

**Windows (winget):**
```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

**macOS (Homebrew):**
```bash
brew install --cask temurin@21
```

**Linux (apt):**
```bash
sudo apt install temurin-21-jdk
```

**Using SDKMAN (all platforms):**
```bash
sdk install java 21.0.4-tem
```

## Toolchains Configuration

Create or update `~/.m2/toolchains.xml` with the following content:

### Windows

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 http://maven.apache.org/xsd/toolchains-1.1.0.xsd">
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>21</version>
            <vendor>temurin</vendor>
        </provides>
        <configuration>
            <jdkHome>C:\Program Files\Eclipse Adoptium\jdk-21.0.4+7</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

### macOS

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 http://maven.apache.org/xsd/toolchains-1.1.0.xsd">
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>21</version>
            <vendor>temurin</vendor>
        </provides>
        <configuration>
            <jdkHome>/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

### Linux

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 http://maven.apache.org/xsd/toolchains-1.1.0.xsd">
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>21</version>
            <vendor>temurin</vendor>
        </provides>
        <configuration>
            <jdkHome>/usr/lib/jvm/temurin-21-jdk-amd64</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

## Finding Your Java Installation Path

To find the correct `jdkHome` path for your system:

**Windows:**
```powershell
# Check common installation location
dir "C:\Program Files\Eclipse Adoptium"

# Or find via registry
reg query "HKLM\SOFTWARE\Eclipse Adoptium" /s | findstr Path
```

**macOS:**
```bash
/usr/libexec/java_home -V
```

**Linux:**
```bash
update-java-alternatives --list
# or
ls -la /usr/lib/jvm/
```

## Verifying Your Setup

After configuring toolchains, verify the setup:

```bash
cd back-end
mvn help:effective-pom | grep -A5 maven-toolchains-plugin
```

Run a test build to confirm Java 21 is being used:

```bash
mvn clean compile -X 2>&1 | grep "Toolchain"
```

You should see output like:
```
[DEBUG] Toolchain in maven-toolchains-plugin: JDK[/path/to/jdk-21]
```

## Troubleshooting

### "No toolchain found for type jdk"

This error means Maven can't find a matching toolchain. Check:

1. **File location**: Ensure `toolchains.xml` is in `~/.m2/` (your Maven home)
2. **Path correctness**: Verify the `jdkHome` path exists and contains a valid JDK
3. **Version/vendor match**: The `<version>` and `<vendor>` must match exactly what's in `pom.xml`

### "release version 21 not supported"

This error occurs when:
1. Toolchains aren't configured - follow the setup steps above
2. The JDK path is incorrect - verify the path in toolchains.xml
3. Maven can't read toolchains.xml - check file permissions

### Multiple Java Versions

You can configure multiple JDK versions in toolchains.xml:

```xml
<toolchains>
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>21</version>
            <vendor>temurin</vendor>
        </provides>
        <configuration>
            <jdkHome>/path/to/jdk-21</jdkHome>
        </configuration>
    </toolchain>
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>17</version>
            <vendor>temurin</vendor>
        </provides>
        <configuration>
            <jdkHome>/path/to/jdk-17</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

## CI/CD Considerations

In CI/CD environments (GitHub Actions, Jenkins, etc.), toolchains are typically not needed because:
- The CI environment controls which Java version is available
- GitHub Actions uses `setup-java` action to configure the correct JDK

The toolchains plugin gracefully handles missing toolchains.xml when the system Java matches the required version.

## Related Documentation

- [Maven Toolchains Plugin](https://maven.apache.org/plugins/maven-toolchains-plugin/)
- [Eclipse Temurin Downloads](https://adoptium.net/temurin/releases/)
- [SDKMAN](https://sdkman.io/) - Tool for managing multiple Java versions
