# Java & Spring Boot Upgrade Migration Plan

**Document Version:** 1.0
**Date:** October 18, 2025
**Author:** System Analysis

## Executive Summary

This document outlines a comprehensive plan to upgrade the OSCAL Tools project from:
- **Java 11** → **Java 21 LTS** (or Java 17 LTS as fallback)
- **Spring Boot 2.7.18** → **Spring Boot 3.4.10** or **3.5.5**

### Motivation

1. **Security**: Java 11 and Spring Boot 2.7.x have reached end-of-life
2. **Performance**: Java 21 offers significant performance improvements
3. **Modern Features**: Access to latest language features and APIs
4. **Long-term Support**: Java 21 LTS supported until September 2029

---

## Current State Analysis

### Current Versions

| Component | Current Version | Status |
|-----------|----------------|--------|
| Java | 11 | EOL (September 2023) |
| Spring Boot | 2.7.18 | EOL (November 2023) |
| liboscal-java | 3.0.3 | Current (Feb 2024) |
| metaschema-framework | 0.12.2 | Current (Sept 2023) |
| JWT (jjwt) | 0.11.5 | Outdated |
| springdoc-openapi | 1.7.0 | Incompatible with Spring Boot 3 |
| H2 Database | (Spring Boot managed) | Needs version check |
| Azure Storage Blob | 12.25.1 | Needs compatibility check |

### Dependency Compatibility Research

**liboscal-java v3.0.3:**
- Built with Java 11 (source/target/release all set to 11)
- **Compatible with Java 17/21**: Yes (via backward compatibility)
- Latest release: February 1, 2024
- Repository: https://github.com/usnistgov/liboscal-java

**metaschema-framework v0.12.2:**
- Built with Java 11 (source/target/release all set to 11)
- **Compatible with Java 17/21**: Yes (via backward compatibility)
- Latest release: September 21, 2023
- Repository: https://github.com/usnistgov/metaschema-java

**Spring Boot 3.x:**
- Latest stable: **3.5.5** (August 21, 2025)
- Previous stable: **3.4.10** (September 18, 2025)
- **Minimum Java version**: Java 17
- **Recommended Java version**: Java 21

---

## Target Versions

### Recommended Configuration

| Component | Target Version | Rationale |
|-----------|---------------|-----------|
| Java | **21 LTS** | Latest LTS, best performance, supported until 2029 |
| Spring Boot | **3.4.10** | Most recent stable in 3.4 series, well-tested |
| springdoc-openapi | **2.6.0** | Latest Spring Boot 3 compatible version |
| JWT (jjwt) | **0.12.6** | Latest stable with security fixes |
| H2 Database | **2.2.224** | Latest compatible with Spring Boot 3.4 |

### Alternative Conservative Configuration

| Component | Target Version | Rationale |
|-----------|---------------|-----------|
| Java | **17 LTS** | More conservative, minimum for Spring Boot 3 |
| Spring Boot | **3.2.x** | Earlier Spring Boot 3 release, more mature |

---

## Migration Strategy: Incremental Approach

We will use an **incremental, phase-by-phase** approach to minimize risk and allow for testing at each stage.

### Phase Overview

```
Phase 1: Java Upgrade (11 → 17 or 21)
   ↓ [Test thoroughly]
Phase 2: Update Minor Dependencies
   ↓ [Test thoroughly]
Phase 3: Spring Boot Upgrade (2.7.18 → 3.4.10)
   ↓ [Test thoroughly]
Phase 4: javax → jakarta Migration
   ↓ [Test thoroughly]
Phase 5: Spring Security Reconfiguration
   ↓ [Test thoroughly]
Phase 6: Update Remaining Dependencies
   ↓ [Final comprehensive testing]
Phase 7: Documentation Updates
```

---

## Phase 1: Java Version Upgrade

### 1.1 Update Parent POM (`pom.xml`)

**File**: `/pom.xml` (lines 46-48)

```xml
<!-- BEFORE -->
<maven.compiler.source>11</maven.compiler.source>
<maven.compiler.target>11</maven.compiler.target>
<maven.compiler.release>11</maven.compiler.release>

<!-- AFTER (Java 21) -->
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>
<maven.compiler.release>21</maven.compiler.release>
```

**File**: `/pom.xml` (lines 64-66)

```xml
<!-- BEFORE -->
<configuration>
    <source>11</source>
    <target>11</target>
    <release>11</release>
</configuration>

<!-- AFTER (Java 21) -->
<configuration>
    <source>21</source>
    <target>21</target>
    <release>21</release>
</configuration>
```

### 1.2 Update CLI Module POM (`cli/pom.xml`)

**File**: `/cli/pom.xml` (lines 103-105)

```xml
<!-- BEFORE -->
<maven.compiler.source>11</maven.compiler.source>
<maven.compiler.target>11</maven.compiler.target>
<maven.compiler.release>11</maven.compiler.release>

<!-- AFTER (Java 21) -->
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>
<maven.compiler.release>21</maven.compiler.release>
```

### 1.3 Update Back-end Module POM (`back-end/pom.xml`)

**File**: `/back-end/pom.xml` (line 22)

```xml
<!-- BEFORE -->
<java.version>11</java.version>

<!-- AFTER (Java 21) -->
<java.version>21</java.version>
```

**File**: `/back-end/pom.xml` (lines 187-189)

```xml
<!-- BEFORE -->
<configuration>
    <source>11</source>
    <target>11</target>
    <release>11</release>
</configuration>

<!-- AFTER (Java 21) -->
<configuration>
    <source>21</source>
    <target>21</target>
    <release>21</release>
</configuration>
```

### 1.4 Update Dockerfile (if exists)

Check for Java version specifications in:
- `Dockerfile`
- `docker-compose.yml`
- `.github/workflows/*.yml` (CI/CD pipelines)

### 1.5 Update Developer Environment

```bash
# Install Java 21 using SDKMAN (already used in project)
sdk install java 21-tem
sdk use java 21-tem

# Verify installation
java -version  # Should show version 21.x.x
```

### 1.6 Testing Checklist - Phase 1

- [ ] CLI module compiles: `cd cli && mvn clean compile`
- [ ] Back-end module compiles: `cd back-end && mvn clean compile`
- [ ] All tests pass: `mvn clean test`
- [ ] CLI executable works: `cli/target/appassembler/bin/oscal-cli --help`
- [ ] CLI validate command works on sample files
- [ ] CLI convert command works on sample files
- [ ] CLI profile resolve works
- [ ] Back-end starts successfully: `cd back-end && mvn spring-boot:run`
- [ ] Front-end connects to back-end API
- [ ] Manual smoke tests of key features

**STOP HERE AND TEST BEFORE PROCEEDING**

---

## Phase 2: Update Minor Dependencies

### 2.1 Update Plugin Versions (CLI module)

**File**: `/cli/pom.xml`

Update these dependency versions that may have Java 21 optimizations:

```xml
<!-- Update Log4j2 (current: 2.20.0) -->
<dependency.log4j2.version>2.24.1</dependency.log4j2.version>

<!-- Update Commons IO (current: 2.15.1) -->
<dependency.commons-io.version>2.17.0</dependency.commons-io.version>

<!-- Update Saxon HE (current: 12.4) -->
<dependency.saxon-he.version>12.5</dependency.saxon-he.version>
```

### 2.2 Update Maven Compiler Plugin

**File**: `/pom.xml` (line 62)

```xml
<!-- BEFORE -->
<version>3.11.0</version>

<!-- AFTER -->
<version>3.13.0</version>
```

### 2.3 Testing Checklist - Phase 2

- [ ] Full build completes: `mvn clean install`
- [ ] All unit tests pass: `mvn test`
- [ ] Integration tests pass
- [ ] CLI operations still work correctly
- [ ] No new warnings in build output

**STOP HERE AND TEST BEFORE PROCEEDING**

---

## Phase 3: Spring Boot Upgrade

### 3.1 Update Spring Boot Parent Version

**File**: `/back-end/pom.xml` (line 11)

```xml
<!-- BEFORE -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>

<!-- AFTER -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.10</version>
    <relativePath/>
</parent>
```

### 3.2 Testing Checklist - Phase 3

**IMPORTANT**: This change will likely cause compilation errors due to:
1. javax → jakarta package migration
2. Spring Security API changes
3. Deprecated API removals

Expected at this phase:
- [ ] Compilation errors (expected - will fix in Phase 4)
- [ ] Note all compilation error messages for Phase 4
- [ ] Document any missing dependencies

**DO NOT ATTEMPT TO RUN - PROCEED TO PHASE 4**

---

## Phase 4: javax → jakarta Package Migration

### 4.1 Package Import Changes

Spring Boot 3 uses Jakarta EE 9+ which renamed all `javax.*` packages to `jakarta.*`.

**Affected Packages:**
```
javax.servlet.* → jakarta.servlet.*
javax.persistence.* → jakarta.persistence.*
javax.validation.* → jakarta.validation.*
javax.annotation.* → jakarta.annotation.*
javax.transaction.* → jakarta.transaction.*
```

### 4.2 Automated Migration Using Find/Replace

Search and replace across the entire `back-end/src` directory:

```bash
# In back-end/src directory, replace:
import javax.servlet. → import jakarta.servlet.
import javax.persistence. → import jakarta.persistence.
import javax.validation. → import jakarta.validation.
import javax.annotation. → import jakarta.annotation.
```

### 4.3 Files Likely Affected

Based on common Spring Boot patterns, check these areas:

**Controllers** (`back-end/src/main/java/.../controller/`):
- `@Valid` annotations
- `HttpServletRequest`, `HttpServletResponse`
- `@PostMapping`, `@GetMapping` parameter types

**Models** (`back-end/src/main/java/.../model/`):
- `@Entity` annotations (JPA)
- `@Valid`, `@NotNull`, `@NotBlank` validation annotations

**Configuration** (`back-end/src/main/java/.../config/`):
- `@Configuration` classes
- Security configuration
- CORS configuration

**Services** (`back-end/src/main/java/.../service/`):
- `@Transactional` annotations
- `@Inject` or `@Resource` if used

### 4.4 Manual Review Required

After automated replacement:
1. Review all changed files
2. Check for any `javax.*` imports that shouldn't be changed (e.g., `javax.xml.*` stays)
3. Verify no runtime `javax.*` references in strings or annotations

### 4.5 Testing Checklist - Phase 4

- [ ] All back-end code compiles: `cd back-end && mvn clean compile`
- [ ] No `javax.*` imports remain (except XML-related)
- [ ] All tests compile (may not pass yet)

**STOP HERE AND VERIFY BEFORE PROCEEDING**

---

## Phase 5: Spring Security Configuration Updates

### 5.1 Major Breaking Changes in Spring Security 6

Spring Boot 3 includes Spring Security 6, which has significant API changes:

1. **`WebSecurityConfigurerAdapter` removed** - Use `SecurityFilterChain` beans instead
2. **Lambda DSL required** - Old chaining methods deprecated
3. **`authorizeRequests()` deprecated** - Use `authorizeHttpRequests()`
4. **CSRF changes** - Different configuration API

### 5.2 Expected Security Configuration File

**File**: `back-end/src/main/java/.../config/SecurityConfig.java`

**BEFORE (Spring Boot 2.7 style):**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
```

**AFTER (Spring Boot 3.4 style):**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
```

### 5.3 JWT Filter Updates

**Check for:** `back-end/src/main/java/.../security/JwtAuthenticationFilter.java`

Update to extend `OncePerRequestFilter` and use Jakarta servlet imports:

```java
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Implementation
}
```

### 5.4 Testing Checklist - Phase 5

- [ ] Security configuration compiles
- [ ] Application starts without security errors
- [ ] Public endpoints accessible without authentication
- [ ] Protected endpoints require authentication
- [ ] JWT authentication flow works
- [ ] CORS still configured correctly

**STOP HERE AND TEST SECURITY THOROUGHLY**

---

## Phase 6: Update Remaining Dependencies

### 6.1 Update springdoc-openapi

**File**: `/back-end/pom.xml` (lines 148-152)

```xml
<!-- BEFORE -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>

<!-- AFTER -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

**Note**: The artifact ID changes from `springdoc-openapi-ui` to `springdoc-openapi-starter-webmvc-ui`.

### 6.2 Update JWT Dependencies

**File**: `/back-end/pom.xml` (lines 127-145)

```xml
<!-- BEFORE -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- AFTER -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### 6.3 Update Azure Storage Blob (if needed)

**File**: `/back-end/pom.xml` (line 158)

```xml
<!-- Check compatibility first, current version may work -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.28.1</version>  <!-- Latest as of Oct 2025 -->
</dependency>
```

### 6.4 Update application.properties / application.yml

**Potential Changes:**

1. **Spring JPA/Hibernate properties:**
```properties
# BEFORE
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# AFTER (if needed)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
# May need to update to newer dialect class
```

2. **Logging patterns** (if customized)
3. **Server configuration keys** (most should be compatible)

### 6.5 Update Swagger/OpenAPI Configuration

**File**: `back-end/src/main/java/.../config/OpenApiConfig.java` (if exists)

Update package imports:
```java
// Check for and update springdoc imports if custom configuration exists
import org.springdoc.core.models.GroupedOpenApi;
// Instead of old package structure
```

### 6.6 Testing Checklist - Phase 6

- [ ] Full build completes: `cd back-end && mvn clean install`
- [ ] All tests pass: `mvn test`
- [ ] Application starts successfully
- [ ] Swagger UI accessible (typically at http://localhost:8080/swagger-ui.html)
- [ ] API documentation renders correctly
- [ ] All REST endpoints functional
- [ ] File upload/download works
- [ ] Azure Blob Storage operations work (if used)
- [ ] Database operations work correctly
- [ ] JWT token generation and validation work
- [ ] Front-end successfully communicates with back-end

**COMPREHENSIVE TESTING REQUIRED**

---

## Phase 7: Documentation and Cleanup

### 7.1 Update CLAUDE.md

Update the "Java Requirements" section:

```markdown
## Java Requirements

- Java 21 or higher (source/target compatibility set to Java 21)
- Uses `@NonNull` annotations from SpotBugs for null safety
```

### 7.2 Update README Files

Update installation instructions and prerequisites in:
- `/README.md`
- `/installer/README.md`
- `/USER_GUIDE.md`

### 7.3 Update Docker Configuration

If Dockerfile exists, update base image:

```dockerfile
# BEFORE
FROM eclipse-temurin:11-jdk-alpine

# AFTER
FROM eclipse-temurin:21-jdk-alpine
```

### 7.4 Update CI/CD Pipelines

Update GitHub Actions workflows (if they exist):

```yaml
# .github/workflows/*.yml
- uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
```

### 7.5 Create Migration Notes

Create `/docs/MIGRATION_NOTES.md` documenting:
- Changes made
- Deprecated features removed
- New features available
- Known issues
- Rollback procedures

---

## Testing Strategy

### Comprehensive Test Matrix

After each phase, execute this test matrix:

#### CLI Module Tests
```bash
cd cli

# Unit tests
mvn test

# Integration tests
mvn verify

# Manual CLI tests
./target/appassembler/bin/oscal-cli --help
./target/appassembler/bin/oscal-cli catalog validate src/test/resources/cli/example_catalog_valid.xml
./target/appassembler/bin/oscal-cli catalog convert src/test/resources/cli/example_catalog_valid.xml --to json
./target/appassembler/bin/oscal-cli profile resolve src/test/resources/cli/example_profile_valid.xml
```

#### Back-end Module Tests
```bash
cd back-end

# Unit tests
mvn test

# Start application
mvn spring-boot:run

# In another terminal, test endpoints
curl http://localhost:8080/swagger-ui.html
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d @test-request.json
```

#### Front-end Integration Tests
```bash
cd front-end

# Start development server
npm run dev

# Manual testing:
# 1. Load http://localhost:3000
# 2. Upload OSCAL file
# 3. Validate file
# 4. Convert file format
# 5. Resolve profile
# 6. Check history
```

#### Full System Tests
- [ ] End-to-end workflow: Upload → Validate → Convert → Download
- [ ] Authentication and authorization flows
- [ ] File upload with various formats (XML, JSON, YAML)
- [ ] Large file handling
- [ ] Error handling and validation messages
- [ ] API response times (performance regression check)

### Performance Testing

Compare before and after metrics:

```bash
# Measure startup time
time mvn spring-boot:run

# Measure API response time
time curl -X POST http://localhost:8080/api/validate -d @large-file.json

# Memory usage
jstat -gc <pid>
```

---

## Rollback Plan

If issues arise at any phase:

### Immediate Rollback Steps

1. **Revert Git changes:**
```bash
git checkout -- .
git clean -fd
```

2. **Verify Java version:**
```bash
sdk use java 11-tem
java -version
```

3. **Clean rebuild:**
```bash
mvn clean install -DskipTests
```

### Partial Rollback

If only specific components have issues:
- Each phase is independent
- Can roll back to previous phase
- Keep Java 21 but revert Spring Boot (for example)

### Version Control Strategy

Before each phase:
```bash
git checkout -b upgrade-phase-X
# Make changes
git commit -m "Phase X: [description]"
# Test thoroughly
# If successful, merge to main branch
```

---

## Known Issues and Gotchas

### 1. Module System Enforcement

Java 21 has stricter module system enforcement:
- May need to add `--add-opens` JVM arguments
- Check for split packages

### 2. Deprecated API Removals

APIs removed from Java 17+ to 21:
- Nashorn JavaScript engine (removed in Java 15)
- Some security algorithms deprecated

### 3. Spring Boot 3 Breaking Changes

- `spring.jpa.hibernate.use-new-id-generator-mappings` default changed
- Some Actuator endpoint paths changed
- Different default serialization behavior

### 4. H2 Database Console

Spring Boot 3 changed H2 console configuration:
```properties
# May need to explicitly enable
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### 5. Jackson Serialization

Minor differences in JSON serialization defaults - review API responses

---

## Timeline Estimate

| Phase | Estimated Time | Testing Time | Total |
|-------|---------------|--------------|-------|
| Phase 1: Java Upgrade | 1 hour | 2 hours | 3 hours |
| Phase 2: Minor Dependencies | 30 minutes | 1 hour | 1.5 hours |
| Phase 3: Spring Boot Version | 15 minutes | 30 minutes | 45 minutes |
| Phase 4: javax → jakarta | 2 hours | 2 hours | 4 hours |
| Phase 5: Security Config | 3 hours | 2 hours | 5 hours |
| Phase 6: Remaining Dependencies | 2 hours | 3 hours | 5 hours |
| Phase 7: Documentation | 1 hour | 1 hour | 2 hours |
| **Total** | **~10 hours** | **~11.5 hours** | **~21.5 hours** |

This assumes familiarity with the codebase. Add 50% buffer for unexpected issues.

---

## Success Criteria

The upgrade is complete and successful when:

- [ ] All Maven modules build without errors
- [ ] All existing tests pass
- [ ] CLI functionality verified manually
- [ ] Back-end API starts without errors
- [ ] All REST endpoints return correct responses
- [ ] Swagger documentation accessible and accurate
- [ ] Front-end successfully integrates with back-end
- [ ] Authentication and security work correctly
- [ ] File upload/download operations work
- [ ] No performance regressions observed
- [ ] Documentation updated
- [ ] Docker containers build and run (if applicable)
- [ ] CI/CD pipelines pass

---

## Post-Upgrade Monitoring

After deployment, monitor:

1. **Application logs** for new warnings or errors
2. **Performance metrics** (response times, memory usage)
3. **Error rates** in production
4. **User-reported issues**

Set up alerts for:
- Increased error rates
- Memory leaks (heap usage climbing)
- Slow API responses
- Authentication failures

---

## Resources and References

### Official Documentation
- [Java 21 Release Notes](https://www.oracle.com/java/technologies/javase/21-relnotes.html)
- [Spring Boot 3.4 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.4-Release-Notes)
- [Spring Security 6 Migration](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Jakarta EE 9 Migration](https://eclipse-ee4j.github.io/jakartaee-platform/jakartaee9/JakartaEE9ReleasePlan)

### Dependency Documentation
- [liboscal-java](https://github.com/usnistgov/liboscal-java)
- [metaschema-java](https://github.com/usnistgov/metaschema-java)
- [springdoc-openapi v2](https://springdoc.org/)
- [JJWT](https://github.com/jwtk/jjwt)

### Migration Guides
- [Migrating to Spring Boot 3](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Java 17 Migration Guide](https://docs.oracle.com/en/java/javase/17/migrate/getting-started.html)
- [Java 21 Migration Guide](https://docs.oracle.com/en/java/javase/21/migrate/getting-started.html)

---

## Appendix A: Quick Command Reference

```bash
# Switch Java versions
sdk list java
sdk install java 21-tem
sdk use java 21-tem

# Build specific module
cd cli && mvn clean install
cd back-end && mvn clean install

# Run tests only
mvn test

# Skip tests
mvn clean install -DskipTests

# Start back-end in debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Check for dependency updates
mvn versions:display-dependency-updates

# Generate dependency tree
mvn dependency:tree > dependencies.txt

# Find javax imports
grep -r "import javax\." back-end/src/
```

---

## Appendix B: Configuration File Checklist

Files to review and potentially update:

- [ ] `/pom.xml` (parent)
- [ ] `/cli/pom.xml`
- [ ] `/back-end/pom.xml`
- [ ] `/back-end/src/main/resources/application.properties`
- [ ] `/back-end/src/main/resources/application.yml` (if exists)
- [ ] `/back-end/src/main/resources/application-dev.properties`
- [ ] `/back-end/src/main/resources/application-prod.properties`
- [ ] `/Dockerfile`
- [ ] `/docker-compose.yml`
- [ ] `/.github/workflows/*.yml`
- [ ] `/installer/install.sh`
- [ ] `/installer/install.ps1`
- [ ] `/README.md`
- [ ] `/CLAUDE.md`
- [ ] `/USER_GUIDE.md`

---

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-10-18 | Initial migration plan created | System Analysis |

---

**Next Steps**: Begin with Phase 1 (Java Upgrade) after reviewing and approving this plan.
