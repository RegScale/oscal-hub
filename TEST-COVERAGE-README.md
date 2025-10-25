# Test Coverage System

## Quick Start

### View Current Coverage

Run this single command to generate and open all coverage reports:

```bash
./coverage.sh --open
```

This will:
1. Run all tests across CLI, back-end, and front-end modules
2. Generate coverage reports with JaCoCo (Java) and Vitest (JavaScript)
3. Open HTML reports in your browser

### Just Generate Reports (Don't Open)

```bash
./coverage.sh
```

## Coverage Reports

After running the script, coverage reports are available at:

- **CLI**: `cli/target/site/jacoco/index.html`
- **Back-End**: `back-end/target/site/jacoco/index.html`
- **Front-End**: `front-end/coverage/index.html`

## Understanding Coverage Reports

### JaCoCo Reports (CLI & Back-End)

The JaCoCo HTML reports provide:
- **Overall Coverage**: Percentage of instructions, branches, lines, methods, and classes covered
- **Package View**: Drill down into packages to see which classes need tests
- **Class View**: See exactly which lines are covered (green), partially covered (yellow), or not covered (red)
- **Method View**: Individual method coverage stats

**Coverage Metrics**:
- **Instruction Coverage**: Individual bytecode instructions executed
- **Branch Coverage**: If/else, switch, loops decision points
- **Line Coverage**: Source code lines executed
- **Method Coverage**: Methods invoked during tests
- **Class Coverage**: Classes loaded during tests

### Vitest Reports (Front-End)

The Vitest coverage reports provide:
- **Statement Coverage**: Individual statements executed
- **Branch Coverage**: Conditional branches taken
- **Function Coverage**: Functions called
- **Line Coverage**: Lines of code executed

**Color Coding**:
- 🟢 **Green**: Well covered (>80%)
- 🟡 **Yellow**: Partial coverage (50-80%)
- 🔴 **Red**: Poor coverage (<50%)

## Current Status

### CLI Module
- **Tests**: 154 ✅
- **Status**: All passing
- **Coverage**: Good coverage of core CLI commands

### Back-End Module
- **Tests**: 43 total
  - 34 passing ✅
  - 9 failing ⚠️ (AuthController security config needed)
- **Coverage**: Moderate
  - ✅ ValidationService (8 tests)
  - ✅ ConversionService (12 tests)
  - ✅ ProfileResolutionService (7 tests)
  - ⚠️ AuthController (16 tests, 9 need security config)
  - ❌ Many services untested

### Front-End Module
- **Tests**: 20 ✅
- **Status**: All passing
- **Coverage**: Very low (0.18%)
  - ✅ utils.ts (100%)
  - ✅ download.ts (100%)
  - ❌ api-client.ts (0% - 2,715 lines)
  - ❌ Most components (0%)

## Running Tests Individually

### CLI Tests
```bash
cd cli
mvn test                    # Run tests
mvn jacoco:report          # Generate coverage report
```

### Back-End Tests
```bash
cd back-end
mvn test                    # Run tests (will fail with 9 failures)
mvn test -Dmaven.test.failure.ignore=true  # Run all tests, ignore failures
mvn jacoco:report          # Generate coverage report
```

### Front-End Tests
```bash
cd front-end
npm test                    # Run tests
npm run test:coverage      # Run tests with coverage
```

## Test Configuration

### JaCoCo Configuration (Java Modules)

JaCoCo is configured in `pom.xml` for both CLI and back-end modules:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Vitest Configuration (Front-End)

Vitest coverage is configured in `front-end/vitest.config.ts`:

```typescript
export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/test/',
        'e2e/',
        '**/*.d.ts',
        '**/*.config.*',
        '**/mockData',
        '**/.{next,turbo}',
      ],
    },
  },
});
```

## Next Steps

### Immediate Priorities

1. **Fix AuthController Tests** (Back-End)
   - Configure Spring Security for testing
   - Update `AuthControllerTest.java` security configuration
   - Goal: All 43 back-end tests passing

2. **Add Back-End Service Tests**
   - FileStorageService (HIGH - file operations)
   - AuthService (HIGH - authentication)
   - ValidationController (HIGH - core API)
   - LibraryService (MEDIUM)
   - VisualizationService (MEDIUM)

3. **Add Front-End Critical Tests**
   - api-client.ts (HIGH - 2,715 lines, 0% coverage)
   - AuthContext.tsx (HIGH - authentication state)
   - oscal-parser.ts (HIGH - OSCAL parsing)

### Coverage Goals

**Short-term (1-2 weeks)**:
- Back-End: 60%+ coverage
- Front-End: 40%+ coverage
- All tests passing

**Long-term (1-2 months)**:
- Back-End: 80%+ coverage
- Front-End: 70%+ coverage
- Integration and E2E tests

## Troubleshooting

### Coverage Report Not Generated

If JaCoCo reports aren't generated:
```bash
cd cli  # or back-end
mvn clean test jacoco:report
```

### Front-End Coverage Missing

If front-end coverage fails:
```bash
cd front-end
npm install --save-dev @vitest/coverage-v8@3.2.4
npm run test:coverage
```

### Browser Won't Open Reports

Manually open reports:
```bash
# Mac
open cli/target/site/jacoco/index.html
open back-end/target/site/jacoco/index.html
open front-end/coverage/index.html

# Linux
xdg-open cli/target/site/jacoco/index.html
xdg-open back-end/target/site/jacoco/index.html
xdg-open front-end/coverage/index.html

# Windows
start cli\target\site\jacoco\index.html
start back-end\target\site\jacoco\index.html
start front-end\coverage\index.html
```

## Contributing

When adding new functionality:
1. Write tests first (TDD approach recommended)
2. Run `./coverage.sh` to verify coverage
3. Aim for >80% coverage on new code
4. Update tests if modifying existing functionality

## References

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Vitest Coverage](https://vitest.dev/guide/coverage.html)
- [Test Coverage Improvements Doc](docs/TEST-COVERAGE-IMPROVEMENTS.md)

---

**Last Updated**: October 25, 2025
