# AI Validation Rule Generator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a wizard at `/rules/custom/ai-generate` that turns natural-language descriptions into enforceable Metaschema constraints with synthetic test cases and a clarification loop, plus the enforcement plumbing so saved rules actually fire during OSCAL validation.

**Architecture:** AI generates Metaschema external-constraint XML via Anthropic tool-use. A new `MetapathConstraintService` builds a per-request `IBindingContext` (cached by content hash) carrying the user's enabled custom rules, which `ValidationService` uses instead of the immutable `OscalBindingContext` singleton when custom rules exist. Wizard state lives in an in-memory Caffeine cache with TTL. Synthetic test cases are evaluated by running a transient context against fragment files and observing thrown exceptions.

**Tech Stack:** Spring Boot 3.5, liboscal-java 6.0.0, metaschema-databind 3.0.0.M1, Anthropic Java SDK 2.27, Next.js 19, shadcn/ui + Tailwind v4, React Query, Sonner, Caffeine.

**Spec:** `docs/superpowers/specs/2026-05-04-ai-validation-rule-generator-design.md`

---

## File Structure

### Backend — new files

```
back-end/src/main/java/gov/nist/oscal/tools/api/
├── controller/
│   └── AiRuleGenController.java                  # REST endpoints for wizard
├── service/
│   ├── MetapathConstraintService.java            # build IBindingContext per (user, model)
│   ├── ConstraintXmlBuilder.java                 # assemble METASCHEMA-CONSTRAINTS XML
│   └── ai/rulegen/
│       ├── AiRuleGenService.java                 # orchestration: turn → tool call → branch
│       ├── AiRuleGenSession.java                 # in-memory session state record
│       ├── AiRuleGenSessionStore.java            # Caffeine TTL cache
│       ├── RuleGenPrompts.java                   # system prompts + tool definitions
│       └── RuleGenTestRunner.java                # evaluate synthetic test cases
├── model/airulegen/
│   ├── StartRuleGenRequest.java
│   ├── StartRuleGenResponse.java
│   ├── RuleGenTurnRequest.java
│   ├── RuleGenTurnResponse.java                  # tagged-union over phase
│   ├── RuleProposal.java
│   ├── TestCase.java
│   ├── TestResult.java
│   ├── EditProposalRequest.java
│   └── SaveRuleRequest.java

back-end/src/main/resources/
├── db/migration/V1.3__ai_rule_metadata.sql       # 3 new columns
└── oscal-schema-summaries/                       # static prompt assets
    ├── catalog.txt
    ├── profile.txt
    ├── system-security-plan.txt
    ├── assessment-plan.txt
    ├── assessment-results.txt
    ├── plan-of-action-and-milestones.txt
    └── component-definition.txt

back-end/src/test/java/gov/nist/oscal/tools/api/
├── service/
│   ├── MetapathConstraintServiceTest.java
│   └── ai/rulegen/
│       ├── AiRuleGenServiceTest.java             # mocked AnthropicClient
│       └── RuleGenTestRunnerTest.java
├── controller/
│   └── AiRuleGenControllerIntegrationTest.java   # full HTTP flow, mocked Claude
└── service/ai/rulegen/
    └── AiRuleGenLiveTest.java                    # gated on ANTHROPIC_API_KEY
```

### Backend — modified files

- `entity/CustomValidationRule.java` — add 3 fields
- `model/CustomRuleRequest.java` — accept new fields (optional)
- `model/CustomRuleResponse.java` — expose new fields
- `service/CustomRulesService.java` — set new fields on save; evict constraint cache on CRUD
- `service/ValidationService.java` — use per-request IBindingContext when custom rules exist
- `service/ai/AnthropicClient.java` — add `sendWithTools(...)` overload
- `service/ai/AnthropicCall.java` — add `tools(...)` and `toolChoice(...)` (verify exact location during Task 8)
- `repository/CustomValidationRuleRepository.java` — add user-scoped query

### Frontend — new files

```
front-end/src/
├── app/rules/custom/ai-generate/
│   ├── page.tsx                                  # wizard shell
│   ├── RuleGenChat.tsx                           # chat history + input
│   ├── RuleProposalView.tsx                      # rule preview pane
│   ├── TestMatrix.tsx                            # synthetic test results
│   └── useRuleGenSession.ts                      # session state hook
├── types/rule-gen.ts                             # mirror backend DTOs
└── __tests__/rules/ai-generate/
    └── RuleGenChat.test.tsx
```

### Frontend — modified files

- `lib/api-client.ts` — add `startRuleGen`, `sendRuleGenTurn`, `editRuleGenProposal`, `saveRuleGenRule`
- `app/rules/custom/page.tsx` — add "Generate with AI" button + AI badge in rule cards

---

## Phase 1 — Enforcement foundation

This phase makes manually-authored Metapath rules actually fire during validation, independent of the AI surface. Phase 2 builds AI generation on top.

### Task 1: Migration + entity fields for AI metadata

**Files:**
- Create: `back-end/src/main/resources/db/migration/V1.3__ai_rule_metadata.sql`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/CustomValidationRule.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/CustomRuleRequest.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/CustomRuleResponse.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/CustomRulesService.java`

- [ ] **Step 1: Write the migration**

Create `back-end/src/main/resources/db/migration/V1.3__ai_rule_metadata.sql`:

```sql
ALTER TABLE custom_validation_rules
    ADD COLUMN IF NOT EXISTS ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS generation_prompt TEXT,
    ADD COLUMN IF NOT EXISTS generation_model VARCHAR(64);
```

- [ ] **Step 2: Add fields to the entity**

In `CustomValidationRule.java`, add these three fields (place them after `createdBy` and before the `User user` relation):

```java
@Column(name = "ai_generated", nullable = false)
@org.hibernate.annotations.ColumnDefault("false")
private Boolean aiGenerated = false;

@Column(name = "generation_prompt", columnDefinition = "TEXT")
private String generationPrompt;

@Column(name = "generation_model", length = 64)
private String generationModel;
```

Add getters/setters for each (matching the style of the existing `createdBy` accessors in the file).

- [ ] **Step 3: Add the fields to the request/response DTOs**

In `CustomRuleRequest.java`, add (after the existing `applicableModelTypes`):

```java
private Boolean aiGenerated;
private String generationPrompt;
private String generationModel;

public Boolean getAiGenerated() { return aiGenerated; }
public void setAiGenerated(Boolean aiGenerated) { this.aiGenerated = aiGenerated; }
public String getGenerationPrompt() { return generationPrompt; }
public void setGenerationPrompt(String generationPrompt) { this.generationPrompt = generationPrompt; }
public String getGenerationModel() { return generationModel; }
public void setGenerationModel(String generationModel) { this.generationModel = generationModel; }
```

In `CustomRuleResponse.java`, add the same three fields with getters/setters.

- [ ] **Step 4: Wire fields through CustomRulesService**

Open `CustomRulesService.java`. Find the `createCustomRule(CustomRuleRequest request)` method. After the existing `rule.setApplicableModelTypes(...)` line, add:

```java
rule.setAiGenerated(Boolean.TRUE.equals(request.getAiGenerated()));
rule.setGenerationPrompt(request.getGenerationPrompt());
rule.setGenerationModel(request.getGenerationModel());
```

Find the `updateCustomRule(...)` method and the `toResponse(...)` mapper (where `CustomRuleResponse` is built from a `CustomValidationRule`). Mirror the same three lines on the response side:

```java
response.setAiGenerated(rule.getAiGenerated());
response.setGenerationPrompt(rule.getGenerationPrompt());
response.setGenerationModel(rule.getGenerationModel());
```

- [ ] **Step 5: Run backend startup to confirm Flyway + Hibernate validate**

Run: `cd back-end && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -Dspring-boot.run.fork=false 2>&1 | head -120`

Expected: log lines `Successfully applied 1 migration to schema "public", now at version v1.3` and no `SchemaManagementException`. Stop the process with Ctrl+C once Spring Boot logs `Started OscalCliApiApplication`.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.3__ai_rule_metadata.sql \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/CustomValidationRule.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/CustomRuleRequest.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/CustomRuleResponse.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/CustomRulesService.java
git commit -m "feat(rules): persist AI generation metadata on custom rules"
```

---

### Task 2: User-scoped query for enforcement filtering

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/CustomValidationRuleRepository.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/repository/CustomValidationRuleRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/repository/CustomValidationRuleRepositoryTest.java`:

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomValidationRuleRepositoryTest {

    @Autowired private CustomValidationRuleRepository repo;
    @Autowired private gov.nist.oscal.tools.api.repository.UserRepository userRepo;

    @Test
    void findEnabledRulesForModelTypeAndUser_returnsOnlyOwnedEnabledRules() {
        User alice = saveUser("alice");
        User bob = saveUser("bob");

        save("a-1", "metapath", "ssp", true, alice);
        save("a-2-disabled", "metapath", "ssp", false, alice);
        save("a-3-other-model", "metapath", "catalog", true, alice);
        save("b-1", "metapath", "ssp", true, bob);

        List<CustomValidationRule> hits =
            repo.findEnabledRulesForModelTypeAndUser("ssp", alice.getId());

        assertThat(hits).extracting(CustomValidationRule::getRuleId)
            .containsExactly("a-1");
    }

    private User saveUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPasswordHash("x");
        u.setCreatedDate(LocalDateTime.now());
        return userRepo.save(u);
    }

    private void save(String id, String type, String model, boolean enabled, User u) {
        CustomValidationRule r = new CustomValidationRule();
        r.setRuleId(id);
        r.setName(id);
        r.setRuleType(type);
        r.setSeverity("error");
        r.setApplicableModelTypes(model);
        r.setEnabled(enabled);
        r.setRuleExpression("<placeholder/>");
        r.setCreatedDate(LocalDateTime.now());
        r.setUpdatedDate(LocalDateTime.now());
        r.setUser(u);
        r.setAiGenerated(false);
        repo.save(r);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=CustomValidationRuleRepositoryTest -q`
Expected: FAIL — `findEnabledRulesForModelTypeAndUser` does not exist.

- [ ] **Step 3: Add the query method**

In `CustomValidationRuleRepository.java`, add:

```java
@Query("SELECT r FROM CustomValidationRule r " +
       "WHERE r.enabled = true " +
       "  AND r.user.id = :userId " +
       "  AND (r.applicableModelTypes LIKE CONCAT('%', :modelType, '%') " +
       "       OR r.applicableModelTypes IS NULL)")
List<CustomValidationRule> findEnabledRulesForModelTypeAndUser(
        @Param("modelType") String modelType,
        @Param("userId") Long userId);
```

Add the imports `org.springframework.data.jpa.repository.Query` and `org.springframework.data.repository.query.Param` if not present.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=CustomValidationRuleRepositoryTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/CustomValidationRuleRepository.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/repository/CustomValidationRuleRepositoryTest.java
git commit -m "feat(rules): add user-scoped query for enforcement"
```

---

### Task 3: ConstraintXmlBuilder — wrap a Metapath rule in METASCHEMA-CONSTRAINTS

The `IConstraintLoader` API only consumes complete `METASCHEMA-CONSTRAINTS` XML documents. A custom rule's `ruleExpression` may be just the inner `<expect>` / `<allowed-values>` / etc. fragment. This builder wraps a fragment in the boilerplate. For rules whose `ruleExpression` is already a complete document (root `<METASCHEMA-CONSTRAINTS>`), it returns the input unchanged.

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ConstraintXmlBuilder.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ConstraintXmlBuilderTest.java`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/ConstraintXmlBuilderTest.java`:

```java
package gov.nist.oscal.tools.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintXmlBuilderTest {

    private final ConstraintXmlBuilder builder = new ConstraintXmlBuilder();

    @Test
    void wrapsAssemblyFragmentForCatalog() {
        String fragment = "<assembly target=\"metadata\">"
            + "<expect id=\"r1\" level=\"ERROR\" test=\"title\">"
            + "<message>need title</message></expect></assembly>";

        String xml = builder.build("rule-r1", "catalog", fragment);

        assertThat(xml)
            .contains("<METASCHEMA-CONSTRAINTS")
            .contains("xmlns=\"http://csrc.nist.gov/ns/oscal/metaschema/1.0\"")
            .contains("metaschema-short-name=\"oscal-catalog\"")
            .contains("metaschema-namespace=\"http://csrc.nist.gov/ns/oscal/1.0\"")
            .contains("<expect id=\"r1\"");
    }

    @Test
    void passesThroughCompleteDocument() {
        String complete = "<METASCHEMA-CONSTRAINTS xmlns=\"http://csrc.nist.gov/ns/oscal/metaschema/1.0\">"
            + "<name>x</name><version>1</version></METASCHEMA-CONSTRAINTS>";
        assertThat(builder.build("any", "ssp", complete)).isEqualTo(complete);
    }

    @Test
    void rejectsUnknownModelType() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> builder.build("r", "not-a-real-model", "<assembly target=\"x\"/>"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=ConstraintXmlBuilderTest -q`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Implement the builder**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/ConstraintXmlBuilder.java`:

```java
package gov.nist.oscal.tools.api.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConstraintXmlBuilder {

    private static final String NS = "http://csrc.nist.gov/ns/oscal/metaschema/1.0";
    private static final String OSCAL_NS = "http://csrc.nist.gov/ns/oscal/1.0";

    private static final Map<String, String> SHORT_NAMES = Map.of(
        "catalog",                            "oscal-catalog",
        "profile",                            "oscal-profile",
        "system-security-plan",               "oscal-ssp",
        "ssp",                                "oscal-ssp",
        "component-definition",               "oscal-component-definition",
        "assessment-plan",                    "oscal-ap",
        "assessment-results",                 "oscal-ar",
        "plan-of-action-and-milestones",      "oscal-poam",
        "poam",                               "oscal-poam"
    );

    public String build(String ruleId, String modelType, String body) {
        if (body != null && body.trim().startsWith("<METASCHEMA-CONSTRAINTS")) {
            return body;
        }
        String shortName = SHORT_NAMES.get(modelType);
        if (shortName == null) {
            throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType);
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<METASCHEMA-CONSTRAINTS xmlns=\"" + NS + "\">"
            + "<name>" + escape(ruleId) + "</name>"
            + "<version>1.0.0</version>"
            + "<scope metaschema-namespace=\"" + OSCAL_NS + "\""
            + "       metaschema-short-name=\"" + shortName + "\">"
            + body
            + "</scope>"
            + "</METASCHEMA-CONSTRAINTS>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=ConstraintXmlBuilderTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ConstraintXmlBuilder.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ConstraintXmlBuilderTest.java
git commit -m "feat(rules): wrap rule fragments in METASCHEMA-CONSTRAINTS envelope"
```

---

### Task 4: MetapathConstraintService — load rules and build IBindingContext

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/MetapathConstraintService.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/MetapathConstraintServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/MetapathConstraintServiceTest.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.Catalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetapathConstraintServiceTest {

    @Mock CustomValidationRuleRepository repo;
    ConstraintXmlBuilder builder = new ConstraintXmlBuilder();
    MetapathConstraintService svc;

    @BeforeEach
    void setUp() {
        svc = new MetapathConstraintService(repo, builder);
    }

    @Test
    void noRulesReturnsSingletonContext() {
        when(repo.findEnabledRulesForModelTypeAndUser(anyString(), anyLong()))
            .thenReturn(List.of());

        IBindingContext ctx = svc.contextFor("catalog", 1L);

        assertThat(ctx).isSameAs(OscalBindingContext.instance());
    }

    @Test
    void buildsContextThatEnforcesCustomConstraint() throws Exception {
        CustomValidationRule rule = newRule("r-title", "catalog",
            "<assembly target=\"metadata\">"
            + "<expect id=\"need-title\" level=\"ERROR\""
            + "        test=\"title and string-length(title) &gt; 0\">"
            + "<message>need title</message></expect></assembly>");
        when(repo.findEnabledRulesForModelTypeAndUser("catalog", 1L))
            .thenReturn(List.of(rule));

        IBindingContext ctx = svc.contextFor("catalog", 1L);

        // A catalog without a title in metadata must throw at deserialize time.
        String catalogNoTitle = "{\"catalog\":{\"uuid\":\"00000000-0000-0000-0000-000000000001\","
            + "\"metadata\":{\"title\":\"\",\"last-modified\":\"2026-01-01T00:00:00Z\","
            + "\"version\":\"1\",\"oscal-version\":\"1.1.2\"}}}";

        assertThatThrownBy(() -> ctx.newDeserializer(Format.JSON, Catalog.class)
            .deserialize(new ByteArrayInputStream(catalogNoTitle.getBytes(StandardCharsets.UTF_8))))
            .hasMessageContaining("need title");
    }

    @Test
    void cachesContextByContentHash() {
        CustomValidationRule rule = newRule("r-1", "catalog",
            "<assembly target=\"metadata\"><expect id=\"x\" level=\"ERROR\" test=\"true\">"
            + "<message>m</message></expect></assembly>");
        when(repo.findEnabledRulesForModelTypeAndUser("catalog", 1L))
            .thenReturn(List.of(rule));

        IBindingContext a = svc.contextFor("catalog", 1L);
        IBindingContext b = svc.contextFor("catalog", 1L);

        assertThat(a).isSameAs(b);
    }

    @Test
    void evictUserClearsCache() {
        CustomValidationRule rule = newRule("r-1", "catalog",
            "<assembly target=\"metadata\"><expect id=\"x\" level=\"ERROR\" test=\"true\">"
            + "<message>m</message></expect></assembly>");
        when(repo.findEnabledRulesForModelTypeAndUser("catalog", 1L))
            .thenReturn(List.of(rule));

        IBindingContext a = svc.contextFor("catalog", 1L);
        svc.evictForUser(1L);
        IBindingContext b = svc.contextFor("catalog", 1L);

        assertThat(a).isNotSameAs(b);
    }

    private CustomValidationRule newRule(String id, String model, String body) {
        CustomValidationRule r = new CustomValidationRule();
        r.setRuleId(id);
        r.setRuleType("metapath");
        r.setApplicableModelTypes(model);
        r.setRuleExpression(body);
        r.setEnabled(true);
        User u = new User();
        u.setId(1L);
        r.setUser(u);
        return r;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=MetapathConstraintServiceTest -q`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Implement the service**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/MetapathConstraintService.java`:

```java
package gov.nist.oscal.tools.api.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import gov.nist.secauto.metaschema.core.model.IConstraintLoader;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds an {@link IBindingContext} pre-loaded with the user's enabled custom
 * validation rules. {@code OscalBindingContext.instance()} is immutable, so we
 * must construct a fresh context whenever custom rules apply. Contexts are
 * cached by SHA-256 of the concatenated constraint XML to keep hot-path
 * validations cheap.
 */
@Service
public class MetapathConstraintService {

    private static final Logger log = LoggerFactory.getLogger(MetapathConstraintService.class);

    private final CustomValidationRuleRepository repo;
    private final ConstraintXmlBuilder builder;

    private final Cache<String, IBindingContext> contextCache = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    public MetapathConstraintService(CustomValidationRuleRepository repo, ConstraintXmlBuilder builder) {
        this.repo = repo;
        this.builder = builder;
    }

    /**
     * Returns an {@link IBindingContext} that enforces the user's enabled custom
     * rules for the given OSCAL model type. Falls back to the singleton when no
     * custom rules apply.
     */
    public IBindingContext contextFor(String modelType, Long userId) {
        List<CustomValidationRule> rules =
            repo.findEnabledRulesForModelTypeAndUser(modelType, userId);
        if (rules.isEmpty()) {
            return OscalBindingContext.instance();
        }

        List<String> xmls = new ArrayList<>(rules.size());
        for (CustomValidationRule r : rules) {
            xmls.add(builder.build(r.getRuleId(), modelType, r.getRuleExpression()));
        }
        String key = sha256(xmls);
        return contextCache.get(key, k -> {
            try {
                return buildContext(xmls);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to build constraint context", e);
            }
        });
    }

    /** Evict every cache entry that may have been built from this user's rules. */
    public void evictForUser(Long userId) {
        // Coarse: invalidate everything. The cache rebuilds lazily on next request
        // and the cost is bounded by the number of distinct (model,user) tuples.
        contextCache.invalidateAll();
        log.debug("Evicted MetapathConstraintService cache after rule change for user {}", userId);
    }

    private IBindingContext buildContext(List<String> constraintXmls) throws Exception {
        IConstraintLoader loader = IBindingContext.getConstraintLoader();
        Set<IConstraintSet> all = new LinkedHashSet<>();
        List<Path> tempFiles = new ArrayList<>();
        try {
            for (String xml : constraintXmls) {
                Path tmp = Files.createTempFile("oscal-constraints-", ".xml");
                Files.writeString(tmp, xml, StandardCharsets.UTF_8);
                tempFiles.add(tmp);
                all.addAll(loader.load(tmp));
            }
        } finally {
            for (Path p : tempFiles) {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            }
        }
        return OscalBindingContext.builder()
                .constraintSet(all)
                .build();
    }

    private static String sha256(List<String> xmls) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String xml : xmls) {
                md.update(xml.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
```

- [ ] **Step 4: Verify Caffeine is on the classpath**

Run: `cd back-end && grep -A1 'caffeine' pom.xml`

If no result, add this to `back-end/pom.xml` inside `<dependencies>`:

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=MetapathConstraintServiceTest -q`
Expected: PASS. The deserialization in `buildsContextThatEnforcesCustomConstraint` should throw with the constraint message.

If the test fails because the JSON catalog format isn't accepted, swap to a minimal XML catalog payload — the JSON envelope shape (`{"catalog":{...}}`) is what `liboscal-java` expects but verify against a valid sample under `cli/src/test/resources/cli/example_catalog_valid.json` and adapt.

- [ ] **Step 6: Commit**

```bash
git add back-end/pom.xml \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/MetapathConstraintService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/MetapathConstraintServiceTest.java
git commit -m "feat(rules): build per-user IBindingContext loaded with custom constraints"
```

---

### Task 5: Wire MetapathConstraintService into ValidationService

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ValidationService.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/CustomRulesService.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ValidationServiceCustomRuleTest.java`

- [ ] **Step 1: Write the failing integration test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/ValidationServiceCustomRuleTest.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.model.ValidationRequest;
import gov.nist.oscal.tools.api.model.ValidationResult;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ValidationServiceCustomRuleTest {

    @Autowired ValidationService validationService;
    @Autowired CustomValidationRuleRepository ruleRepo;
    @Autowired UserRepository userRepo;

    @Test
    void customRuleViolationFlagsValidationResult() {
        User u = new User();
        u.setUsername("rule-tester");
        u.setEmail("rule-tester@example.com");
        u.setPasswordHash("x");
        u.setCreatedDate(LocalDateTime.now());
        userRepo.save(u);

        CustomValidationRule r = new CustomValidationRule();
        r.setRuleId("must-have-version");
        r.setName("Catalog must declare version");
        r.setRuleType("metapath");
        r.setSeverity("error");
        r.setApplicableModelTypes("catalog");
        r.setEnabled(true);
        r.setUser(u);
        r.setRuleExpression(
            "<assembly target=\"metadata\">"
            + "<expect id=\"must-have-version\" level=\"ERROR\""
            + "        test=\"version and string-length(version) &gt; 0\">"
            + "<message>[custom: must-have-version] missing version</message></expect>"
            + "</assembly>");
        r.setCreatedDate(LocalDateTime.now());
        r.setUpdatedDate(LocalDateTime.now());
        r.setAiGenerated(false);
        ruleRepo.save(r);

        // Read a known-good sample, then strip its version to trigger the rule.
        String good = readResource("cli/example_catalog_valid.json");
        String bad = good.replaceAll("\"version\"\\s*:\\s*\"[^\"]*\"", "\"version\":\"\"");

        ValidationRequest req = new ValidationRequest(bad, OscalModelType.CATALOG, OscalFormat.JSON);
        ValidationResult result = validationService.validate(req, "rule-tester");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).extracting("message")
            .anyMatch(m -> m.toString().contains("missing version"));
    }

    private String readResource(String classpath) {
        try (var in = getClass().getClassLoader().getResourceAsStream(classpath)) {
            if (in == null) throw new IllegalStateException("missing " + classpath);
            return new String(in.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

The fixture path matches the CLI test resources. Verify with:
`find /Users/travishowerton/Documents/GitHub/oscal-cli -name 'example_catalog_valid.json'`
Copy that file into `back-end/src/test/resources/cli/example_catalog_valid.json` if not already on the test classpath.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=ValidationServiceCustomRuleTest -q`
Expected: FAIL — `result.isValid()` is `true` because custom rules aren't enforced yet.

- [ ] **Step 3: Make ValidationService user-aware**

Edit `ValidationService.java`. Replace the constructor and the `validate(...)` and `getDeserializer(...)` methods:

```java
private final OscalBindingContext defaultContext;
private final HistoryService historyService;
private final FileStorageService fileStorageService;
private final MetapathConstraintService constraintService;
private final UserRepository userRepository;

@Autowired
public ValidationService(HistoryService historyService,
                         FileStorageService fileStorageService,
                         MetapathConstraintService constraintService,
                         UserRepository userRepository) {
    this.defaultContext = OscalBindingContext.instance();
    this.historyService = historyService;
    this.fileStorageService = fileStorageService;
    this.constraintService = constraintService;
    this.userRepository = userRepository;
}
```

Update `validate(ValidationRequest request, String username)` so that the deserializer is obtained from a user-scoped context. Replace lines 47-55 (the existing `getDeserializer` call and surrounding deserialization) with:

```java
// Determine the format
Format format = getFormat(request.getFormat());

// Resolve the calling user (for per-user custom rule enforcement)
Long userId = userRepository.findByUsername(username)
        .map(u -> u.getId())
        .orElse(null);

// Build a binding context that enforces the user's custom rules
String modelKey = request.getModelType().getValue();
IBindingContext context = userId == null
        ? defaultContext
        : constraintService.contextFor(modelKey, userId);

// Get the appropriate deserializer for the model type
IDeserializer<?> deserializer = newDeserializer(context, request.getModelType(), format);
```

Rename the existing `getDeserializer(modelType, format)` method to `newDeserializer(context, modelType, format)` and change every `bindingContext.newDeserializer(...)` inside it to `context.newDeserializer(...)`. Add the import `gov.nist.secauto.metaschema.databind.IBindingContext`.

In the `catch (Exception e)` block at lines 81-91, prefix the error message with `[custom: ...]` only if the error message contains the constraint id pattern — actually the constraint authors include the tag in their `<message>` already, so no special handling is needed here. Leave the catch block as-is.

- [ ] **Step 4: Add CRUD-driven cache eviction**

In `CustomRulesService.java`, inject `MetapathConstraintService`:

```java
private final MetapathConstraintService constraintService;

@Autowired
public CustomRulesService(CustomValidationRuleRepository repository,
                          MetapathConstraintService constraintService) {
    this.repository = repository;
    this.constraintService = constraintService;
}
```

In `createCustomRule`, `updateCustomRule`, `deleteCustomRule`, and `toggleRuleEnabled`, after the repository write, add (immediately before `return`):

```java
constraintService.evictForUser(rule.getUser() == null ? null : rule.getUser().getId());
```

For `deleteCustomRule`, capture `Long uid = rule.getUser() == null ? null : rule.getUser().getId();` before the delete, then evict after.

- [ ] **Step 5: Run the integration test to verify it passes**

Run: `cd back-end && mvn test -Dtest=ValidationServiceCustomRuleTest -q`
Expected: PASS. The catalog with a stripped version is rejected, and the custom message appears in the result.

- [ ] **Step 6: Run the full backend test suite**

Run: `cd back-end && mvn test -q`
Expected: PASS — no regressions in existing validation tests.

- [ ] **Step 7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ValidationService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/CustomRulesService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ValidationServiceCustomRuleTest.java \
        back-end/src/test/resources/cli/example_catalog_valid.json
git commit -m "feat(rules): enforce custom Metapath rules during OSCAL validation"
```

---

## Phase 2 — AI rule generation backend

### Task 6: OSCAL schema summaries (prompt assets)

**Files:**
- Create: `back-end/src/main/resources/oscal-schema-summaries/catalog.txt` (and 6 siblings)

These are static text assets shipped on the classpath, embedded in the system prompt so Claude can reference paths and cardinalities. Keep each under ~3KB so prompt-cache invalidation is cheap.

- [ ] **Step 1: Author the catalog summary**

Create `back-end/src/main/resources/oscal-schema-summaries/catalog.txt`:

```
OSCAL Catalog (oscal-catalog) — Metaschema short-name "oscal-catalog", namespace http://csrc.nist.gov/ns/oscal/1.0

Top-level: catalog
Required children of catalog:
  - uuid (flag, UUID)
  - metadata (assembly)
Optional:
  - params (list of parameter assemblies)
  - controls (list of control assemblies)
  - groups (list of group assemblies, may be nested)
  - back-matter

Key Metapath addresses (from catalog/):
  - metadata/title (required, non-empty string)
  - metadata/last-modified (required, dateTime)
  - metadata/version (required, string)
  - metadata/oscal-version (required, string)
  - metadata/parties (list)
  - controls/control@id (string, unique within catalog)
  - controls/control/title (required)
  - controls/control/parts/part@name (string, e.g. "statement")
  - groups/group/controls/control@id

Common constraint patterns:
  <expect target="controls/control" test="title and string-length(title)>0">
  <has-cardinality target="controls/control" min-occurs="1"/>
  <is-unique target="controls/control" id="catalog-controls-unique"><key-field target="@id"/></is-unique>
  <allowed-values target="controls/control/parts/part/@name" allow-other="yes">
    <enum value="statement"/><enum value="guidance"/></allowed-values>
```

- [ ] **Step 2: Author the other six summaries**

Create the same shape for: `profile.txt`, `system-security-plan.txt`, `assessment-plan.txt`, `assessment-results.txt`, `plan-of-action-and-milestones.txt`, `component-definition.txt`. For each, include short-name + namespace, required/optional top-level children, and 8-15 key Metapath addresses with cardinality. Source data from the OSCAL JSON schemas in `liboscal-java`'s test resources or https://pages.nist.gov/OSCAL/concepts/layer/.

Constraints per file: top header line, "Required children" list, "Optional" list, "Key Metapath addresses" list, "Common constraint patterns" examples. Aim for 40-80 lines each.

- [ ] **Step 3: Verify they load on the classpath**

Run: `cd back-end && mvn -q -DskipTests package && jar tf target/*.jar | grep oscal-schema-summaries`
Expected: all 7 `.txt` files listed under `BOOT-INF/classes/oscal-schema-summaries/`.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/resources/oscal-schema-summaries/
git commit -m "feat(rules): add per-OSCAL-model schema summaries for AI prompts"
```

---

### Task 7: AI rule-gen DTOs

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/StartRuleGenRequest.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/StartRuleGenResponse.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/RuleGenTurnRequest.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/RuleGenTurnResponse.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/RuleProposal.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/TestCase.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/TestResult.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/EditProposalRequest.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/SaveRuleRequest.java`

These are simple record classes — Java 17 records with Jackson defaults. No tests for plain records.

- [ ] **Step 1: Create the records**

`StartRuleGenRequest.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartRuleGenRequest(
    @NotNull Long organizationId,
    @NotBlank String modelType
) {}
```

`StartRuleGenResponse.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

import java.util.UUID;

public record StartRuleGenResponse(UUID sessionId) {}
```

`RuleGenTurnRequest.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

import jakarta.validation.constraints.NotBlank;

public record RuleGenTurnRequest(@NotBlank String userMessage) {}
```

`RuleGenTurnResponse.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

import java.util.List;

/**
 * Tagged-union response. Exactly one of clarifyingQuestion / proposal is set.
 *
 * phase = "clarify" → clarifyingQuestion is non-null
 * phase = "proposal" → proposal + testResults are non-null
 * phase = "exhausted" → message describes why we couldn't reach a working rule;
 *                       lastProposal may be non-null (best attempt)
 */
public record RuleGenTurnResponse(
    String phase,
    String clarifyingQuestion,
    RuleProposal proposal,
    List<TestResult> testResults,
    RuleProposal lastProposal,
    String message,
    int iterations,
    int totalTokensIn,
    int totalTokensOut
) {}
```

`RuleProposal.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

import java.util.List;

public record RuleProposal(
    String name,
    String description,
    String severity,
    String fieldPath,
    String constraintXml,
    List<TestCase> testCases
) {}
```

`TestCase.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

public record TestCase(
    String description,
    String fragmentJson,
    String expected   // "pass" or "fail"
) {}
```

`TestResult.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

public record TestResult(
    int index,
    String description,
    String expected,
    String actual,         // "pass" or "fail"
    boolean passed,        // true when actual == expected
    String violationMessage // null when no violation
) {}
```

`EditProposalRequest.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

import jakarta.validation.constraints.NotBlank;

public record EditProposalRequest(@NotBlank String constraintXml) {}
```

`SaveRuleRequest.java`:

```java
package gov.nist.oscal.tools.api.model.airulegen;

import jakarta.validation.constraints.NotBlank;

public record SaveRuleRequest(
    @NotBlank String ruleId,
    String category,
    Boolean enabled
) {}
```

- [ ] **Step 2: Verify compile**

Run: `cd back-end && mvn -q compile`
Expected: SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/airulegen/
git commit -m "feat(rules): DTOs for AI rule generation wizard"
```

---

### Task 8: Session store (in-memory, TTL)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSession.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSessionStore.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSessionStoreTest.java`

- [ ] **Step 1: Write the failing test**

```java
// back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSessionStoreTest.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRuleGenSessionStoreTest {

    @Test
    void createGetUpdate() {
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        UUID id = store.create(7L, 99L, "catalog", "claude-opus-4-7");

        AiRuleGenSession s = store.get(id);
        assertThat(s.organizationId()).isEqualTo(7L);
        assertThat(s.userId()).isEqualTo(99L);
        assertThat(s.modelType()).isEqualTo("catalog");
        assertThat(s.transcript()).isEmpty();

        store.appendUser(id, "hello");
        store.appendAssistant(id, "hi back");
        assertThat(store.get(id).transcript()).hasSize(2);
    }

    @Test
    void unknownSessionThrows() {
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        assertThatThrownBy(() -> store.get(UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=AiRuleGenSessionStoreTest -q`
Expected: FAIL — types do not exist.

- [ ] **Step 3: Implement the session record**

```java
// back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSession.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.model.airulegen.RuleProposal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class AiRuleGenSession {

    public record TranscriptEntry(String role, String text) {}

    private final UUID id;
    private final long organizationId;
    private final long userId;
    private final String modelType;
    private final String anthropicModel;
    private final List<TranscriptEntry> transcript = new ArrayList<>();
    private final AtomicInteger tokensIn = new AtomicInteger();
    private final AtomicInteger tokensOut = new AtomicInteger();

    private RuleProposal currentProposal;

    AiRuleGenSession(UUID id, long organizationId, long userId, String modelType, String anthropicModel) {
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.modelType = modelType;
        this.anthropicModel = anthropicModel;
    }

    public UUID id() { return id; }
    public long organizationId() { return organizationId; }
    public long userId() { return userId; }
    public String modelType() { return modelType; }
    public String anthropicModel() { return anthropicModel; }
    public List<TranscriptEntry> transcript() { return transcript; }
    public RuleProposal currentProposal() { return currentProposal; }
    public void setCurrentProposal(RuleProposal p) { this.currentProposal = p; }
    public int tokensIn() { return tokensIn.get(); }
    public int tokensOut() { return tokensOut.get(); }
    public void addTokens(int in, int out) { tokensIn.addAndGet(in); tokensOut.addAndGet(out); }
}
```

- [ ] **Step 4: Implement the store**

```java
// back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSessionStore.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class AiRuleGenSessionStore {

    private final Cache<UUID, AiRuleGenSession> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(1024)
            .build();

    public UUID create(long organizationId, long userId, String modelType, String anthropicModel) {
        UUID id = UUID.randomUUID();
        cache.put(id, new AiRuleGenSession(id, organizationId, userId, modelType, anthropicModel));
        return id;
    }

    public AiRuleGenSession get(UUID id) {
        AiRuleGenSession s = cache.getIfPresent(id);
        if (s == null) {
            throw new IllegalArgumentException("Unknown or expired rule-gen session: " + id);
        }
        return s;
    }

    public void appendUser(UUID id, String text) {
        get(id).transcript().add(new AiRuleGenSession.TranscriptEntry("user", text));
    }

    public void appendAssistant(UUID id, String text) {
        get(id).transcript().add(new AiRuleGenSession.TranscriptEntry("assistant", text));
    }

    public void close(UUID id) {
        cache.invalidate(id);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=AiRuleGenSessionStoreTest -q`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSession.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSessionStore.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenSessionStoreTest.java
git commit -m "feat(rules): in-memory rule-gen session store with 30-min TTL"
```

---

### Task 9: Extend AnthropicClient with tool-use

The existing `AnthropicClient.send(...)` only returns plain text. The wizard needs structured outputs. Add a `sendWithTools(...)` overload that exposes the tool-use block.

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicClient.java`
- Modify (or create alongside): `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicCall.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicToolUseResult.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AnthropicClientToolsContractTest.java`

- [ ] **Step 1: Inspect AnthropicCall**

Run: `cat back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicCall.java`

Note its current shape (record vs class, builder vs constructor). The new `tools()` and `toolChoice()` accessors should follow the same style. The instructions below assume it's a record; if it is a class with a builder, adapt accordingly.

- [ ] **Step 2: Add tool definition types**

```java
// back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicToolUseResult.java
package gov.nist.oscal.tools.api.service.ai;

import com.fasterxml.jackson.databind.JsonNode;

public record AnthropicToolUseResult(
    String toolName,
    JsonNode input,
    int tokensIn,
    int tokensOut
) {}
```

- [ ] **Step 3: Extend AnthropicCall**

If `AnthropicCall` is a record, add two components: `java.util.List<com.anthropic.models.messages.ToolUnion> tools` and `com.anthropic.models.messages.ToolChoice toolChoice`. Default both to null.

If you cannot identify the exact SDK types from the project's `anthropic-java:2.27.0` JAR, write the call using untyped `Map<String, Object>` JSON and pass it to the SDK builder via reflection. The pragmatic path: copy `tools` as a `List<Map<String, Object>>` representing the JSON schema for each tool, and pass via `MessageCreateParams.builder().putAdditionalBodyProperty("tools", tools)`.

Concretely, add to `AnthropicCall.java`:

```java
// existing components ...
java.util.List<java.util.Map<String, Object>> tools,
String toolChoice
```

- [ ] **Step 4: Implement sendWithTools**

In `AnthropicClient.java`, add a new method:

```java
public AnthropicToolUseResult sendWithTools(String apiKey, AnthropicCall call,
                                            Consumer<String> onRetry) {
    if (apiKey == null || apiKey.isBlank()) {
        throw new IllegalArgumentException("Missing Anthropic API key");
    }
    com.anthropic.client.AnthropicClient client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .maxRetries(0)
            .timeout(Duration.ofMinutes(2))
            .build();

    List<ContentBlockParam> blocks = new ArrayList<>();
    for (String text : call.textDocuments()) {
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(text).build()));
    }
    blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(call.userMessage()).build()));

    MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
            .model(call.model())
            .maxTokens(call.maxTokens())
            .system(call.systemPrompt())
            .addMessage(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(blocks)
                    .build());

    // Tools and tool_choice: shipped as raw JSON via the SDK's
    // putAdditionalBodyProperty escape hatch — keeps us decoupled from
    // SDK-version-specific tool-builder classes.
    if (call.tools() != null && !call.tools().isEmpty()) {
        paramsBuilder = paramsBuilder.putAdditionalBodyProperty("tools",
                com.anthropic.core.JsonValue.from(call.tools()));
    }
    if (call.toolChoice() != null) {
        paramsBuilder = paramsBuilder.putAdditionalBodyProperty("tool_choice",
                com.anthropic.core.JsonValue.from(java.util.Map.of(
                    "type", call.toolChoice())));
    }

    Message message = sendWithRetry(client, paramsBuilder.build(), onRetry);

    // Find the tool_use block.
    String toolName = null;
    com.fasterxml.jackson.databind.JsonNode input = null;
    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
    for (var block : message.content()) {
        if (block.toolUse().isPresent()) {
            var tu = block.toolUse().get();
            toolName = tu.name();
            input = om.valueToTree(tu.input());
            break;
        }
    }
    if (toolName == null) {
        throw new IllegalStateException("Anthropic response contained no tool_use block");
    }
    return new AnthropicToolUseResult(toolName, input,
            (int) message.usage().inputTokens(),
            (int) message.usage().outputTokens());
}
```

If the SDK's `ContentBlock.toolUse()` accessor is named differently in 2.27, run:
`unzip -p ~/.m2/repository/com/anthropic/anthropic-java-core/*/anthropic-java-core-*.jar com/anthropic/models/messages/ContentBlock.class | strings | grep -i tool` and adapt the access pattern. The extraction goal is unchanged: locate the `tool_use` block, read `name` and `input`.

- [ ] **Step 5: Write a contract test (mocked at the HTTP layer)**

This test validates the request shape we send to Anthropic — not Claude's response logic. Use OkHttp's `MockWebServer` is overkill; instead, test the public contract by passing a recording-test stub and asserting the produced `AnthropicCall` round-trips correctly.

```java
// back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AnthropicClientToolsContractTest.java
package gov.nist.oscal.tools.api.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicClientToolsContractTest {

    @Test
    void anthropicCallCarriesToolsAndChoice() {
        // Re-create the call shape we'll send. This guards the AnthropicCall API.
        AnthropicCall call = new AnthropicCall(
            "claude-opus-4-7",
            4096,
            "system",
            "user",
            List.of(),
            List.of(),
            List.of(Map.of(
                "name", "ask_clarifying_question",
                "description", "Ask the user one short clarifying question.",
                "input_schema", Map.of(
                    "type", "object",
                    "properties", Map.of("question", Map.of("type", "string")),
                    "required", List.of("question"))
            )),
            "any"
        );

        assertThat(call.tools()).hasSize(1);
        assertThat(call.tools().get(0).get("name")).isEqualTo("ask_clarifying_question");
        assertThat(call.toolChoice()).isEqualTo("any");
    }
}
```

Adapt the constructor argument order to match whatever the existing `AnthropicCall` shape is. If `AnthropicCall` uses a builder, write the test against the builder.

- [ ] **Step 6: Run tests**

Run: `cd back-end && mvn test -Dtest='AnthropicClientToolsContractTest,AnthropicClientTest' -q`
Expected: PASS, no regressions in any pre-existing AnthropicClient tests.

- [ ] **Step 7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicClient.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicCall.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicToolUseResult.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AnthropicClientToolsContractTest.java
git commit -m "feat(ai): tool-use support in AnthropicClient"
```

---

### Task 10: RuleGenPrompts — system prompt + tool definitions

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenPrompts.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenPromptsTest.java`

- [ ] **Step 1: Write the failing test**

```java
// back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenPromptsTest.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleGenPromptsTest {

    private final RuleGenPrompts prompts = new RuleGenPrompts();

    @Test
    void systemPromptIncludesSchemaSummary() {
        String s = prompts.systemPromptFor("catalog");
        assertThat(s).contains("oscal-catalog");
        assertThat(s).contains("METASCHEMA-CONSTRAINTS");
        assertThat(s).contains("ask_clarifying_question");
        assertThat(s).contains("generate_rule");
        assertThat(s).contains("revise_rule");
    }

    @Test
    void unknownModelTypeFails() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> prompts.systemPromptFor("not-a-model"));
    }

    @Test
    void toolDefinitionsHaveAllThreeTools() {
        List<java.util.Map<String, Object>> tools = prompts.toolDefinitions();
        assertThat(tools).extracting(t -> t.get("name"))
            .containsExactlyInAnyOrder("ask_clarifying_question", "generate_rule", "revise_rule");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=RuleGenPromptsTest -q`
Expected: FAIL.

- [ ] **Step 3: Implement RuleGenPrompts**

```java
// back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenPrompts.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class RuleGenPrompts {

    public String systemPromptFor(String modelType) {
        String summary = loadSummary(modelType);
        return ""
            + "You help OSCAL Hub users author Metaschema validation rules.\n"
            + "The user describes a rule in plain English. You convert it into a\n"
            + "well-formed Metaschema external constraint XML fragment plus 4-6\n"
            + "synthetic test cases (minimal valid OSCAL stubs labeled pass/fail).\n"
            + "\n"
            + "OUTPUT CONTRACT — every turn you must call exactly ONE tool:\n"
            + "  ask_clarifying_question  — the description is ambiguous; ask\n"
            + "                            ONE short question.\n"
            + "  generate_rule            — you have enough info; produce the full\n"
            + "                            proposal with test cases.\n"
            + "  revise_rule              — same shape as generate_rule, used when\n"
            + "                            iterating from a failing test matrix.\n"
            + "\n"
            + "Constraint XML rules:\n"
            + "  * Output the inner <assembly target=...> / <field target=...>\n"
            + "    fragment ONLY — the system wraps it in METASCHEMA-CONSTRAINTS.\n"
            + "  * Use <expect>, <allowed-values>, <matches>, <has-cardinality>,\n"
            + "    <is-unique>, <index>, <index-has-key> only.\n"
            + "  * Every constraint has an id and a level (ERROR / WARNING / INFORMATIONAL).\n"
            + "  * Include a clear <message> on every constraint, prefixed with\n"
            + "    [custom: <ruleId>] so users can identify the source.\n"
            + "  * Test fragments are minimal valid JSON OSCAL stubs (just enough\n"
            + "    of the model to be parseable). Avoid full real-world docs.\n"
            + "\n"
            + "OSCAL model summary:\n"
            + "===================\n"
            + summary;
    }

    public List<Map<String, Object>> toolDefinitions() {
        return List.of(
            Map.of(
                "name", "ask_clarifying_question",
                "description", "Ask ONE short clarifying question when the user's description is ambiguous. Keep questions focused on the specific missing info.",
                "input_schema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "question", Map.of("type", "string", "description", "The question to ask the user.")),
                    "required", List.of("question"))
            ),
            Map.of(
                "name", "generate_rule",
                "description", "Produce a full rule proposal with synthetic test cases. Use this when you have enough information.",
                "input_schema", proposalSchema()
            ),
            Map.of(
                "name", "revise_rule",
                "description", "Same shape as generate_rule. Use only when revising a previous proposal that had failing test cases.",
                "input_schema", proposalSchema()
            )
        );
    }

    private static Map<String, Object> proposalSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "name", Map.of("type", "string"),
                "description", Map.of("type", "string"),
                "severity", Map.of("type", "string", "enum", List.of("error", "warning", "info")),
                "fieldPath", Map.of("type", "string"),
                "constraintXml", Map.of("type", "string", "description", "Inner <assembly>/<field> fragment, NOT a full METASCHEMA-CONSTRAINTS document."),
                "testCases", Map.of(
                    "type", "array",
                    "minItems", 4,
                    "maxItems", 6,
                    "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "description", Map.of("type", "string"),
                            "fragmentJson", Map.of("type", "string", "description", "Minimal JSON OSCAL fragment"),
                            "expected", Map.of("type", "string", "enum", List.of("pass", "fail"))),
                        "required", List.of("description", "fragmentJson", "expected")))),
            "required", List.of("name", "description", "severity", "constraintXml", "testCases"));
    }

    private String loadSummary(String modelType) {
        String resource = "oscal-schema-summaries/" + canonicalize(modelType) + ".txt";
        try {
            return new String(new ClassPathResource(resource).getInputStream().readAllBytes(),
                              StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType, e);
        }
    }

    private static String canonicalize(String modelType) {
        return switch (modelType) {
            case "ssp" -> "system-security-plan";
            case "ap" -> "assessment-plan";
            case "ar" -> "assessment-results";
            case "poam" -> "plan-of-action-and-milestones";
            default -> modelType;
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=RuleGenPromptsTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenPrompts.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenPromptsTest.java
git commit -m "feat(rules): system prompt + tool defs for AI rule wizard"
```

---

### Task 11: RuleGenTestRunner — execute synthetic test cases

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenTestRunner.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenTestRunnerTest.java`

- [ ] **Step 1: Write the failing test**

```java
// back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenTestRunnerTest.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.model.airulegen.TestCase;
import gov.nist.oscal.tools.api.model.airulegen.TestResult;
import gov.nist.oscal.tools.api.service.ConstraintXmlBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleGenTestRunnerTest {

    private final RuleGenTestRunner runner = new RuleGenTestRunner(new ConstraintXmlBuilder());

    @Test
    void detectsExpectedPassAndFail() {
        String constraint =
            "<assembly target=\"metadata\">"
            + "<expect id=\"need-version\" level=\"ERROR\""
            + "        test=\"version and string-length(version) &gt; 0\">"
            + "<message>missing version</message></expect></assembly>";

        String good = "{\"catalog\":{\"uuid\":\"00000000-0000-0000-0000-000000000001\","
            + "\"metadata\":{\"title\":\"t\",\"last-modified\":\"2026-01-01T00:00:00Z\","
            + "\"version\":\"1\",\"oscal-version\":\"1.1.2\"}}}";
        String bad = good.replace("\"version\":\"1\"", "\"version\":\"\"");

        List<TestResult> results = runner.run(
            "rule-x", "catalog", constraint,
            List.of(
                new TestCase("good has version", good, "pass"),
                new TestCase("bad has empty version", bad, "fail")));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).passed()).isTrue();
        assertThat(results.get(1).passed()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=RuleGenTestRunnerTest -q`
Expected: FAIL.

- [ ] **Step 3: Implement the runner**

```java
// back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenTestRunner.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.model.airulegen.TestCase;
import gov.nist.oscal.tools.api.model.airulegen.TestResult;
import gov.nist.oscal.tools.api.service.ConstraintXmlBuilder;
import gov.nist.secauto.metaschema.core.model.IConstraintLoader;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.AssessmentPlan;
import gov.nist.secauto.oscal.lib.model.AssessmentResults;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.ComponentDefinition;
import gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones;
import gov.nist.secauto.oscal.lib.model.Profile;
import gov.nist.secauto.oscal.lib.model.SystemSecurityPlan;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class RuleGenTestRunner {

    private final ConstraintXmlBuilder builder;

    public RuleGenTestRunner(ConstraintXmlBuilder builder) {
        this.builder = builder;
    }

    public List<TestResult> run(String ruleId, String modelType, String constraintBody, List<TestCase> cases) {
        IBindingContext context = buildContext(ruleId, modelType, constraintBody);
        Class<?> klass = modelClass(modelType);
        List<TestResult> results = new ArrayList<>(cases.size());

        for (int i = 0; i < cases.size(); i++) {
            TestCase tc = cases.get(i);
            String actual;
            String violation = null;
            try {
                context.newDeserializer(Format.JSON, klass)
                    .deserialize(new ByteArrayInputStream(
                        tc.fragmentJson().getBytes(StandardCharsets.UTF_8)));
                actual = "pass";
            } catch (Exception e) {
                actual = "fail";
                violation = e.getMessage();
            }
            boolean passed = actual.equals(tc.expected());
            results.add(new TestResult(i, tc.description(), tc.expected(), actual, passed, violation));
        }
        return results;
    }

    private IBindingContext buildContext(String ruleId, String modelType, String body) {
        try {
            String wrapped = builder.build(ruleId, modelType, body);
            Path tmp = Files.createTempFile("rulegen-constraints-", ".xml");
            try {
                Files.writeString(tmp, wrapped, StandardCharsets.UTF_8);
                IConstraintLoader loader = IBindingContext.getConstraintLoader();
                Set<IConstraintSet> set = new LinkedHashSet<>(loader.load(tmp));
                return OscalBindingContext.builder().constraintSet(set).build();
            } finally {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build context for test runner", e);
        }
    }

    private static Class<?> modelClass(String modelType) {
        return switch (modelType) {
            case "catalog" -> Catalog.class;
            case "profile" -> Profile.class;
            case "system-security-plan", "ssp" -> SystemSecurityPlan.class;
            case "component-definition" -> ComponentDefinition.class;
            case "assessment-plan", "ap" -> AssessmentPlan.class;
            case "assessment-results", "ar" -> AssessmentResults.class;
            case "plan-of-action-and-milestones", "poam" -> PlanOfActionAndMilestones.class;
            default -> throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType);
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=RuleGenTestRunnerTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenTestRunner.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/RuleGenTestRunnerTest.java
git commit -m "feat(rules): synthetic test runner for AI-generated constraints"
```

---

### Task 12: AiRuleGenService — orchestration

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenService.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
// back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenServiceTest.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnResponse;
import gov.nist.oscal.tools.api.service.ConstraintXmlBuilder;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicToolUseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRuleGenServiceTest {

    @Mock AnthropicClient anthropic;
    @Mock AiSettingsService aiSettings;

    AiRuleGenSessionStore store;
    RuleGenPrompts prompts = new RuleGenPrompts();
    RuleGenTestRunner runner = new RuleGenTestRunner(new ConstraintXmlBuilder());
    AiRuleGenService svc;
    ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        store = new AiRuleGenSessionStore();
        when(aiSettings.requireApiKey(anyLong())).thenReturn("sk-test");
        when(aiSettings.getDefaultModel(anyLong())).thenReturn("claude-opus-4-7");
        svc = new AiRuleGenService(anthropic, aiSettings, store, prompts, runner);
    }

    @Test
    void clarifyingTurnReturnsQuestionWithoutGenerating() throws Exception {
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
            .thenReturn(new AnthropicToolUseResult(
                "ask_clarifying_question",
                om.readTree("{\"question\":\"Which control field?\"}"),
                10, 5));

        UUID sid = svc.start(7L, 99L, "catalog");
        RuleGenTurnResponse res = svc.turn(sid, "rule about controls");

        assertThat(res.phase()).isEqualTo("clarify");
        assertThat(res.clarifyingQuestion()).isEqualTo("Which control field?");
        assertThat(res.proposal()).isNull();
    }

    @Test
    void cleanProposalReturnsImmediately() throws Exception {
        String good = "{\"catalog\":{\"uuid\":\"00000000-0000-0000-0000-000000000001\","
            + "\"metadata\":{\"title\":\"t\",\"last-modified\":\"2026-01-01T00:00:00Z\","
            + "\"version\":\"1\",\"oscal-version\":\"1.1.2\"}}}";
        String bad = good.replace("\"version\":\"1\"", "\"version\":\"\"");
        String constraint = "<assembly target=\\\"metadata\\\">"
            + "<expect id=\\\"need-version\\\" level=\\\"ERROR\\\""
            + " test=\\\"version and string-length(version) &gt; 0\\\">"
            + "<message>need version</message></expect></assembly>";

        String input = "{"
            + "\"name\":\"need version\","
            + "\"description\":\"version required\","
            + "\"severity\":\"error\","
            + "\"fieldPath\":\"metadata/version\","
            + "\"constraintXml\":\"" + constraint + "\","
            + "\"testCases\":["
            + "  {\"description\":\"good\",\"fragmentJson\":" + om.writeValueAsString(good) + ",\"expected\":\"pass\"},"
            + "  {\"description\":\"bad\",\"fragmentJson\":" + om.writeValueAsString(bad) + ",\"expected\":\"fail\"}"
            + "]}";

        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
            .thenReturn(new AnthropicToolUseResult("generate_rule", om.readTree(input), 100, 200));

        UUID sid = svc.start(7L, 99L, "catalog");
        RuleGenTurnResponse res = svc.turn(sid, "version must be set");

        assertThat(res.phase()).isEqualTo("proposal");
        assertThat(res.proposal()).isNotNull();
        assertThat(res.testResults()).hasSize(2);
        assertThat(res.testResults()).allMatch(tr -> tr.passed());
        assertThat(res.iterations()).isEqualTo(1);
    }

    @Test
    void dirtyMatrixTriggersUpToThreeIterations() throws Exception {
        // Anthropic returns a generate_rule whose tests are all 'pass' but its
        // constraint never fires — every "fail" expected case actually passes.
        String good = "{\"catalog\":{\"uuid\":\"00000000-0000-0000-0000-000000000001\","
            + "\"metadata\":{\"title\":\"t\",\"last-modified\":\"2026-01-01T00:00:00Z\","
            + "\"version\":\"1\",\"oscal-version\":\"1.1.2\"}}}";
        String trivialAlwaysTrue =
            "<assembly target=\\\"metadata\\\">"
            + "<expect id=\\\"x\\\" level=\\\"ERROR\\\" test=\\\"true()\\\">"
            + "<message>impossible</message></expect></assembly>";

        String dirtyInput = "{"
            + "\"name\":\"x\",\"description\":\"x\",\"severity\":\"error\","
            + "\"fieldPath\":\"metadata\",\"constraintXml\":\"" + trivialAlwaysTrue + "\","
            + "\"testCases\":["
            + "  {\"description\":\"good\",\"fragmentJson\":" + om.writeValueAsString(good) + ",\"expected\":\"fail\"}"
            + "]}";

        AnthropicToolUseResult dirty = new AnthropicToolUseResult(
            "generate_rule", om.readTree(dirtyInput), 100, 200);

        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
            .thenReturn(dirty, dirty, dirty, dirty);  // 1 initial + 3 revisions

        UUID sid = svc.start(7L, 99L, "catalog");
        RuleGenTurnResponse res = svc.turn(sid, "anything");

        assertThat(res.phase()).isEqualTo("exhausted");
        assertThat(res.iterations()).isEqualTo(4);  // 1 + 3 retries
        verify(anthropic, times(4)).sendWithTools(anyString(), any(AnthropicCall.class), any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=AiRuleGenServiceTest -q`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Implement AiRuleGenService**

```java
// back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenService.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnResponse;
import gov.nist.oscal.tools.api.model.airulegen.RuleProposal;
import gov.nist.oscal.tools.api.model.airulegen.TestCase;
import gov.nist.oscal.tools.api.model.airulegen.TestResult;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicToolUseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiRuleGenService {

    private static final Logger log = LoggerFactory.getLogger(AiRuleGenService.class);
    private static final int MAX_REVISIONS = 3;
    private static final int MAX_TOKENS = 4096;

    private final AnthropicClient anthropic;
    private final AiSettingsService aiSettings;
    private final AiRuleGenSessionStore store;
    private final RuleGenPrompts prompts;
    private final RuleGenTestRunner testRunner;
    private final ObjectMapper om = new ObjectMapper();

    public AiRuleGenService(AnthropicClient anthropic,
                            AiSettingsService aiSettings,
                            AiRuleGenSessionStore store,
                            RuleGenPrompts prompts,
                            RuleGenTestRunner testRunner) {
        this.anthropic = anthropic;
        this.aiSettings = aiSettings;
        this.store = store;
        this.prompts = prompts;
        this.testRunner = testRunner;
    }

    public UUID start(long organizationId, long userId, String modelType) {
        String model = aiSettings.getDefaultModel(organizationId);
        return store.create(organizationId, userId, modelType, model);
    }

    public RuleGenTurnResponse turn(UUID sessionId, String userMessage) {
        AiRuleGenSession session = store.get(sessionId);
        store.appendUser(sessionId, userMessage);

        AnthropicToolUseResult res = callClaude(session, userMessage);
        session.addTokens(res.tokensIn(), res.tokensOut());

        return switch (res.toolName()) {
            case "ask_clarifying_question" -> handleClarify(session, res);
            case "generate_rule", "revise_rule" -> handleProposal(session, res, 1);
            default -> throw new IllegalStateException("Unexpected tool: " + res.toolName());
        };
    }

    public RuleGenTurnResponse rerunTests(UUID sessionId, String editedConstraintXml) {
        AiRuleGenSession session = store.get(sessionId);
        RuleProposal current = session.currentProposal();
        if (current == null) {
            throw new IllegalStateException("No current proposal to edit");
        }
        RuleProposal edited = new RuleProposal(
            current.name(), current.description(), current.severity(),
            current.fieldPath(), editedConstraintXml, current.testCases());
        List<TestResult> results = testRunner.run(
            "rule-" + sessionId, session.modelType(), editedConstraintXml, current.testCases());
        session.setCurrentProposal(edited);
        boolean clean = results.stream().allMatch(TestResult::passed);
        return new RuleGenTurnResponse(
            clean ? "proposal" : "exhausted",
            null, edited, results, edited,
            clean ? null : "Edited constraint produced failing tests.",
            1, session.tokensIn(), session.tokensOut());
    }

    public AiRuleGenSession session(UUID id) { return store.get(id); }
    public void close(UUID id) { store.close(id); }

    // ---- internals ----

    private AnthropicToolUseResult callClaude(AiRuleGenSession session, String userMessage) {
        String apiKey = aiSettings.requireApiKey(session.organizationId());
        AnthropicCall call = buildCall(session, userMessage);
        return anthropic.sendWithTools(apiKey, call, msg -> log.info(
            "rule-gen session={} retry: {}", session.id(), msg));
    }

    private AnthropicCall buildCall(AiRuleGenSession session, String userMessage) {
        // History as a single recap blob keeps the prompt-cache breakpoint
        // at the system prompt level and avoids fiddling with multi-turn
        // assistant messages in this synchronous flow.
        StringBuilder ctx = new StringBuilder();
        for (var entry : session.transcript()) {
            ctx.append(entry.role().toUpperCase()).append(": ").append(entry.text()).append("\n\n");
        }
        ctx.append("USER: ").append(userMessage);
        return new AnthropicCall(
            session.anthropicModel(),
            MAX_TOKENS,
            prompts.systemPromptFor(session.modelType()),
            ctx.toString(),
            List.of(),
            List.of(),
            prompts.toolDefinitions(),
            "any"
        );
    }

    private RuleGenTurnResponse handleClarify(AiRuleGenSession session, AnthropicToolUseResult res) {
        String q = res.input().path("question").asText("");
        store.appendAssistant(session.id(), q);
        return new RuleGenTurnResponse("clarify", q, null, null, null, null, 1,
                session.tokensIn(), session.tokensOut());
    }

    private RuleGenTurnResponse handleProposal(AiRuleGenSession session,
                                               AnthropicToolUseResult res,
                                               int iteration) {
        RuleProposal proposal = parseProposal(res.input());
        store.appendAssistant(session.id(),
            "(proposed rule \"" + proposal.name() + "\" with " + proposal.testCases().size() + " test cases)");
        session.setCurrentProposal(proposal);

        List<TestResult> results = testRunner.run(
            "rule-" + session.id(), session.modelType(),
            proposal.constraintXml(), proposal.testCases());

        boolean clean = results.stream().allMatch(TestResult::passed);
        if (clean) {
            return new RuleGenTurnResponse("proposal", null, proposal, results, proposal,
                    null, iteration, session.tokensIn(), session.tokensOut());
        }

        if (iteration >= 1 + MAX_REVISIONS) {
            return new RuleGenTurnResponse(
                "exhausted", null, null, results, proposal,
                buildExhaustedMessage(results),
                iteration, session.tokensIn(), session.tokensOut());
        }

        // Auto-revise
        String reviseMsg = "Your last proposal had failing tests:\n"
            + formatFailures(results)
            + "\nFix the constraint and regenerate test cases. Call revise_rule.";
        AnthropicToolUseResult revised = anthropic.sendWithTools(
            aiSettings.requireApiKey(session.organizationId()),
            buildCall(session, reviseMsg),
            m -> log.info("rule-gen session={} retry: {}", session.id(), m));
        session.addTokens(revised.tokensIn(), revised.tokensOut());

        return switch (revised.toolName()) {
            case "generate_rule", "revise_rule" -> handleProposal(session, revised, iteration + 1);
            case "ask_clarifying_question" -> handleClarify(session, revised);
            default -> throw new IllegalStateException("Unexpected tool: " + revised.toolName());
        };
    }

    private RuleProposal parseProposal(JsonNode node) {
        try {
            String constraintXml = node.path("constraintXml").asText();
            List<TestCase> cases = new ArrayList<>();
            for (JsonNode tc : node.withArray("testCases")) {
                cases.add(new TestCase(
                    tc.path("description").asText(),
                    tc.path("fragmentJson").asText(),
                    tc.path("expected").asText()));
            }
            return new RuleProposal(
                node.path("name").asText(),
                node.path("description").asText(),
                node.path("severity").asText("error"),
                node.path("fieldPath").asText(""),
                constraintXml,
                cases);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse rule proposal: " + e.getMessage(), e);
        }
    }

    private String formatFailures(List<TestResult> results) {
        StringBuilder sb = new StringBuilder();
        for (TestResult r : results) {
            if (!r.passed()) {
                sb.append("  - case \"").append(r.description()).append("\": ")
                  .append("expected ").append(r.expected())
                  .append(", got ").append(r.actual());
                if (r.violationMessage() != null) {
                    sb.append(" (").append(r.violationMessage()).append(")");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String buildExhaustedMessage(List<TestResult> results) {
        long failed = results.stream().filter(r -> !r.passed()).count();
        return "I couldn't reach a working rule after the maximum number of revisions. "
             + failed + " test case(s) still don't match expectations. "
             + "Please clarify your description, edit the constraint manually, or abandon.";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=AiRuleGenServiceTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenServiceTest.java
git commit -m "feat(rules): orchestrate AI clarify/generate/revise loop"
```

---

### Task 13: AiRuleGenController + save endpoint

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiRuleGenController.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiRuleGenControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing integration test**

```java
// back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiRuleGenControllerIntegrationTest.java
package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicToolUseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiRuleGenControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean AnthropicClient anthropic;
    ObjectMapper om = new ObjectMapper();

    @Test
    @WithMockUser(username = "alice")
    void clarifyTurnReturnsQuestion() throws Exception {
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
            .thenReturn(new AnthropicToolUseResult(
                "ask_clarifying_question",
                om.readTree("{\"question\":\"Which field?\"}"),
                10, 5));

        // Assumes test fixture: alice belongs to org 1 with AI configured.
        // Setup of OrgAiSettings + User happens in @BeforeAll using
        // OrgAiSettingsRepository + UserRepository — adapt to project's
        // test-fixture conventions if they exist.
        String startBody = "{\"organizationId\":1,\"modelType\":\"catalog\"}";
        String startResp = mvc.perform(post("/api/rules/ai-generate/sessions")
                .contentType(MediaType.APPLICATION_JSON).content(startBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String sessionId = om.readTree(startResp).get("sessionId").asText();

        mvc.perform(post("/api/rules/ai-generate/sessions/" + sessionId + "/turn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userMessage\":\"hello\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("clarify"))
            .andExpect(jsonPath("$.clarifyingQuestion").value("Which field?"));
    }
}
```

The fixture setup (`OrgAiSettings` + `User`) follows whatever pattern existing AI integration tests use — find one in `back-end/src/test/java/.../service/ai/` and copy.

- [ ] **Step 2: Run to verify failure**

Run: `cd back-end && mvn test -Dtest=AiRuleGenControllerIntegrationTest -q`
Expected: FAIL — controller doesn't exist.

- [ ] **Step 3: Implement the controller**

```java
// back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiRuleGenController.java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.airulegen.*;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.MetapathConstraintService;
import gov.nist.oscal.tools.api.service.ai.rulegen.AiRuleGenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/rules/ai-generate")
public class AiRuleGenController {

    private final AiRuleGenService service;
    private final UserRepository userRepository;
    private final CustomValidationRuleRepository ruleRepository;
    private final MetapathConstraintService constraintService;

    public AiRuleGenController(AiRuleGenService service,
                               UserRepository userRepository,
                               CustomValidationRuleRepository ruleRepository,
                               MetapathConstraintService constraintService) {
        this.service = service;
        this.userRepository = userRepository;
        this.ruleRepository = ruleRepository;
        this.constraintService = constraintService;
    }

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StartRuleGenResponse> start(@Valid @RequestBody StartRuleGenRequest req,
                                                      Principal principal) {
        User user = requireUser(principal);
        UUID id = service.start(req.organizationId(), user.getId(), req.modelType());
        return ResponseEntity.ok(new StartRuleGenResponse(id));
    }

    @PostMapping("/sessions/{id}/turn")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RuleGenTurnResponse> turn(@PathVariable UUID id,
                                                    @Valid @RequestBody RuleGenTurnRequest req,
                                                    Principal principal) {
        requireOwnership(id, principal);
        return ResponseEntity.ok(service.turn(id, req.userMessage()));
    }

    @PostMapping("/sessions/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RuleGenTurnResponse> edit(@PathVariable UUID id,
                                                    @Valid @RequestBody EditProposalRequest req,
                                                    Principal principal) {
        requireOwnership(id, principal);
        return ResponseEntity.ok(service.rerunTests(id, req.constraintXml()));
    }

    @PostMapping("/sessions/{id}/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> save(@PathVariable UUID id,
                                     @Valid @RequestBody SaveRuleRequest req,
                                     Principal principal) {
        var session = service.session(id);
        User user = requireUser(principal);
        if (session.userId() != user.getId()) {
            return ResponseEntity.status(403).build();
        }
        var p = session.currentProposal();
        if (p == null) {
            return ResponseEntity.badRequest().build();
        }
        if (ruleRepository.existsByRuleId(req.ruleId())) {
            return ResponseEntity.status(409).build();
        }
        CustomValidationRule rule = new CustomValidationRule();
        rule.setRuleId(req.ruleId());
        rule.setName(p.name());
        rule.setDescription(p.description());
        rule.setRuleType("metapath");
        rule.setSeverity(p.severity());
        rule.setCategory(req.category());
        rule.setFieldPath(p.fieldPath());
        rule.setRuleExpression(p.constraintXml());
        rule.setApplicableModelTypes(session.modelType());
        rule.setEnabled(req.enabled() == null ? Boolean.TRUE : req.enabled());
        rule.setCreatedDate(LocalDateTime.now());
        rule.setUpdatedDate(LocalDateTime.now());
        rule.setCreatedBy(user.getUsername());
        rule.setUser(user);
        rule.setAiGenerated(true);
        rule.setGenerationModel(session.anthropicModel());
        // generation_prompt: first user transcript entry
        rule.setGenerationPrompt(session.transcript().isEmpty()
            ? null
            : session.transcript().get(0).text());

        ruleRepository.save(rule);
        constraintService.evictForUser(user.getId());
        service.close(id);
        return ResponseEntity.ok(rule.getId());
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> abandon(@PathVariable UUID id, Principal principal) {
        requireOwnership(id, principal);
        service.close(id);
        return ResponseEntity.noContent().build();
    }

    private User requireUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private void requireOwnership(UUID id, Principal principal) {
        long userId = requireUser(principal).getId();
        if (service.session(id).userId() != userId) {
            throw new org.springframework.security.access.AccessDeniedException("Not your session");
        }
    }
}
```

- [ ] **Step 4: Run integration test**

Run: `cd back-end && mvn test -Dtest=AiRuleGenControllerIntegrationTest -q`
Expected: PASS.

- [ ] **Step 5: Run full backend suite**

Run: `cd back-end && mvn test -q`
Expected: PASS — no regressions.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiRuleGenController.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiRuleGenControllerIntegrationTest.java
git commit -m "feat(rules): REST endpoints for AI rule-gen wizard"
```

---

## Phase 3 — Frontend wizard

### Task 14: TypeScript types

**Files:**
- Create: `front-end/src/types/rule-gen.ts`

- [ ] **Step 1: Create the types**

```ts
// front-end/src/types/rule-gen.ts
export type RuleGenPhase = 'clarify' | 'proposal' | 'exhausted';

export interface TestCase {
  description: string;
  fragmentJson: string;
  expected: 'pass' | 'fail';
}

export interface TestResult {
  index: number;
  description: string;
  expected: 'pass' | 'fail';
  actual: 'pass' | 'fail';
  passed: boolean;
  violationMessage: string | null;
}

export interface RuleProposal {
  name: string;
  description: string;
  severity: 'error' | 'warning' | 'info';
  fieldPath: string;
  constraintXml: string;
  testCases: TestCase[];
}

export interface RuleGenTurnResponse {
  phase: RuleGenPhase;
  clarifyingQuestion: string | null;
  proposal: RuleProposal | null;
  testResults: TestResult[] | null;
  lastProposal: RuleProposal | null;
  message: string | null;
  iterations: number;
  totalTokensIn: number;
  totalTokensOut: number;
}

export interface ChatEntry {
  role: 'user' | 'assistant';
  text: string;
}

export type OscalModelType =
  | 'catalog'
  | 'profile'
  | 'system-security-plan'
  | 'component-definition'
  | 'assessment-plan'
  | 'assessment-results'
  | 'plan-of-action-and-milestones';
```

- [ ] **Step 2: Commit**

```bash
git add front-end/src/types/rule-gen.ts
git commit -m "feat(rules): TS types for AI rule-gen wizard"
```

---

### Task 15: API client methods

**Files:**
- Modify: `front-end/src/lib/api-client.ts`

- [ ] **Step 1: Add the methods**

In `api-client.ts`, locate the existing custom-rules methods (e.g., `getAllCustomRules`) and add these alongside, following the same `fetchWithTimeout` + auth-header pattern:

```ts
import type {
  RuleGenTurnResponse,
  OscalModelType,
} from '@/types/rule-gen';

async startRuleGen(organizationId: number, modelType: OscalModelType): Promise<{ sessionId: string }> {
  const res = await this.fetchWithTimeout(`${this.base}/rules/ai-generate/sessions`, {
    method: 'POST',
    headers: this.authHeaders('json'),
    body: JSON.stringify({ organizationId, modelType }),
  });
  return res.json();
}

async sendRuleGenTurn(sessionId: string, userMessage: string): Promise<RuleGenTurnResponse> {
  const res = await this.fetchWithTimeout(
    `${this.base}/rules/ai-generate/sessions/${sessionId}/turn`,
    {
      method: 'POST',
      headers: this.authHeaders('json'),
      body: JSON.stringify({ userMessage }),
      timeoutMs: 120_000,  // up to 4 Anthropic round-trips at ~30s each
    });
  return res.json();
}

async editRuleGenProposal(sessionId: string, constraintXml: string): Promise<RuleGenTurnResponse> {
  const res = await this.fetchWithTimeout(
    `${this.base}/rules/ai-generate/sessions/${sessionId}/edit`,
    {
      method: 'POST',
      headers: this.authHeaders('json'),
      body: JSON.stringify({ constraintXml }),
    });
  return res.json();
}

async saveRuleGenRule(sessionId: string, ruleId: string, category?: string, enabled = true): Promise<number> {
  const res = await this.fetchWithTimeout(
    `${this.base}/rules/ai-generate/sessions/${sessionId}/save`,
    {
      method: 'POST',
      headers: this.authHeaders('json'),
      body: JSON.stringify({ ruleId, category, enabled }),
    });
  return res.json();
}

async abandonRuleGen(sessionId: string): Promise<void> {
  await this.fetchWithTimeout(
    `${this.base}/rules/ai-generate/sessions/${sessionId}`,
    { method: 'DELETE', headers: this.authHeaders() });
}
```

If the existing client uses `apiClient.X` (function-style) rather than class methods, adapt to that style. The existing custom-rules methods establish the pattern — match it exactly.

If `fetchWithTimeout` doesn't currently accept a `timeoutMs` override, add one (default to the existing 5000) — the rule-gen turn can take well over the default 5s, especially during auto-iterate, and we don't want to abort mid-Claude-call.

- [ ] **Step 2: Type-check**

Run: `cd front-end && npm run typecheck` (or `npx tsc --noEmit` if no script exists).
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/lib/api-client.ts
git commit -m "feat(rules): API client methods for rule-gen wizard"
```

---

### Task 16: useRuleGenSession hook

**Files:**
- Create: `front-end/src/app/rules/custom/ai-generate/useRuleGenSession.ts`

- [ ] **Step 1: Implement the hook**

```ts
'use client';

import { useCallback, useState } from 'react';
import { apiClient } from '@/lib/api-client';
import type {
  ChatEntry,
  OscalModelType,
  RuleGenTurnResponse,
} from '@/types/rule-gen';

interface RuleGenState {
  sessionId: string | null;
  modelType: OscalModelType | null;
  chat: ChatEntry[];
  latest: RuleGenTurnResponse | null;
  loading: boolean;
  error: string | null;
}

export function useRuleGenSession() {
  const [state, setState] = useState<RuleGenState>({
    sessionId: null,
    modelType: null,
    chat: [],
    latest: null,
    loading: false,
    error: null,
  });

  const start = useCallback(async (organizationId: number, modelType: OscalModelType) => {
    setState((s) => ({ ...s, loading: true, error: null, modelType }));
    try {
      const { sessionId } = await apiClient.startRuleGen(organizationId, modelType);
      setState((s) => ({ ...s, sessionId, loading: false }));
    } catch (e) {
      setState((s) => ({ ...s, error: errorMessage(e), loading: false }));
    }
  }, []);

  const send = useCallback(async (userMessage: string) => {
    if (!state.sessionId) return;
    setState((s) => ({
      ...s,
      loading: true,
      error: null,
      chat: [...s.chat, { role: 'user', text: userMessage }],
    }));
    try {
      const res = await apiClient.sendRuleGenTurn(state.sessionId, userMessage);
      const assistantText = renderAssistantBlurb(res);
      setState((s) => ({
        ...s,
        loading: false,
        latest: res,
        chat: [...s.chat, { role: 'assistant', text: assistantText }],
      }));
    } catch (e) {
      setState((s) => ({ ...s, error: errorMessage(e), loading: false }));
    }
  }, [state.sessionId]);

  const editConstraint = useCallback(async (constraintXml: string) => {
    if (!state.sessionId) return;
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const res = await apiClient.editRuleGenProposal(state.sessionId, constraintXml);
      setState((s) => ({ ...s, loading: false, latest: res }));
    } catch (e) {
      setState((s) => ({ ...s, error: errorMessage(e), loading: false }));
    }
  }, [state.sessionId]);

  const save = useCallback(async (ruleId: string, category?: string) => {
    if (!state.sessionId) return null;
    return apiClient.saveRuleGenRule(state.sessionId, ruleId, category, true);
  }, [state.sessionId]);

  const abandon = useCallback(async () => {
    if (state.sessionId) await apiClient.abandonRuleGen(state.sessionId);
    setState({ sessionId: null, modelType: null, chat: [], latest: null, loading: false, error: null });
  }, [state.sessionId]);

  return { ...state, start, send, editConstraint, save, abandon };
}

function renderAssistantBlurb(res: RuleGenTurnResponse): string {
  if (res.phase === 'clarify') return res.clarifyingQuestion ?? '';
  if (res.phase === 'proposal') {
    return `Drafted "${res.proposal?.name}" — ${res.testResults?.length ?? 0} test cases all pass.`;
  }
  return res.message ?? "I couldn't reach a working rule.";
}

function errorMessage(e: unknown): string {
  if (e instanceof Error) return e.message;
  return String(e);
}
```

- [ ] **Step 2: Commit**

```bash
git add front-end/src/app/rules/custom/ai-generate/useRuleGenSession.ts
git commit -m "feat(rules): useRuleGenSession hook"
```

---

### Task 17: Wizard sub-components

**Files:**
- Create: `front-end/src/app/rules/custom/ai-generate/RuleGenChat.tsx`
- Create: `front-end/src/app/rules/custom/ai-generate/RuleProposalView.tsx`
- Create: `front-end/src/app/rules/custom/ai-generate/TestMatrix.tsx`

- [ ] **Step 1: Create RuleGenChat**

```tsx
'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import type { ChatEntry } from '@/types/rule-gen';

interface Props {
  entries: ChatEntry[];
  loading: boolean;
  disabled: boolean;
  placeholder?: string;
  onSend: (text: string) => void;
}

export function RuleGenChat({ entries, loading, disabled, placeholder, onSend }: Props) {
  const [draft, setDraft] = useState('');
  const submit = () => {
    const t = draft.trim();
    if (!t) return;
    onSend(t);
    setDraft('');
  };
  return (
    <div className="flex flex-col h-full border rounded-md">
      <div className="flex-1 overflow-auto p-3 space-y-2">
        {entries.map((e, i) => (
          <div
            key={i}
            className={
              e.role === 'user'
                ? 'self-end max-w-[85%] ml-auto bg-primary text-primary-foreground rounded-md px-3 py-2'
                : 'self-start max-w-[85%] mr-auto bg-muted rounded-md px-3 py-2'
            }
          >
            <p className="text-sm whitespace-pre-wrap">{e.text}</p>
          </div>
        ))}
        {loading && (
          <div className="text-xs text-muted-foreground italic">Thinking…</div>
        )}
      </div>
      <div className="border-t p-2 flex gap-2">
        <Textarea
          value={draft}
          disabled={disabled || loading}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) submit();
          }}
          placeholder={placeholder ?? 'Describe the rule…'}
          className="min-h-[60px]"
        />
        <Button onClick={submit} disabled={disabled || loading || !draft.trim()}>
          Send
        </Button>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Create RuleProposalView**

```tsx
'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import type { RuleProposal } from '@/types/rule-gen';

interface Props {
  proposal: RuleProposal | null;
  onEdit: (constraintXml: string) => void;
  onSave: () => void;
  saveDisabled: boolean;
  loading: boolean;
}

export function RuleProposalView({ proposal, onEdit, onSave, saveDisabled, loading }: Props) {
  const [xml, setXml] = useState(proposal?.constraintXml ?? '');
  useEffect(() => {
    setXml(proposal?.constraintXml ?? '');
  }, [proposal?.constraintXml]);

  if (!proposal) {
    return (
      <Card className="p-4 text-sm text-muted-foreground">
        Once a rule is drafted it will appear here.
      </Card>
    );
  }
  const dirty = xml !== proposal.constraintXml;
  return (
    <Card className="p-4 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold">{proposal.name}</h3>
          <p className="text-sm text-muted-foreground">{proposal.description}</p>
        </div>
        <Badge variant={proposal.severity === 'error' ? 'destructive' : 'secondary'}>
          {proposal.severity}
        </Badge>
      </div>
      <div>
        <label className="text-xs uppercase tracking-wide text-muted-foreground">
          Metaschema constraint (Metapath)
        </label>
        <Textarea
          value={xml}
          onChange={(e) => setXml(e.target.value)}
          rows={8}
          className="font-mono text-xs"
        />
      </div>
      <div className="flex gap-2 justify-end">
        <Button
          variant="outline"
          disabled={!dirty || loading}
          onClick={() => onEdit(xml)}
        >
          Re-test edited constraint
        </Button>
        <Button disabled={saveDisabled || loading || dirty} onClick={onSave}>
          Save rule
        </Button>
      </div>
    </Card>
  );
}
```

- [ ] **Step 3: Create TestMatrix**

```tsx
'use client';

import { Card } from '@/components/ui/card';
import { Check, X } from 'lucide-react';
import type { TestResult } from '@/types/rule-gen';

interface Props {
  results: TestResult[] | null;
}

export function TestMatrix({ results }: Props) {
  if (!results) {
    return (
      <Card className="p-4 text-sm text-muted-foreground">
        Synthetic test results will appear here.
      </Card>
    );
  }
  return (
    <Card className="p-0 overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-muted">
          <tr>
            <th className="text-left px-3 py-2">#</th>
            <th className="text-left px-3 py-2">Case</th>
            <th className="text-left px-3 py-2">Expected</th>
            <th className="text-left px-3 py-2">Actual</th>
            <th className="text-center px-3 py-2 w-12">Status</th>
          </tr>
        </thead>
        <tbody>
          {results.map((r) => (
            <tr key={r.index} className="border-t">
              <td className="px-3 py-2 text-muted-foreground">{r.index + 1}</td>
              <td className="px-3 py-2">
                <div>{r.description}</div>
                {!r.passed && r.violationMessage && (
                  <div className="text-xs text-muted-foreground mt-1">
                    {r.violationMessage}
                  </div>
                )}
              </td>
              <td className="px-3 py-2">{r.expected}</td>
              <td className="px-3 py-2">{r.actual}</td>
              <td className="px-3 py-2 text-center">
                {r.passed
                  ? <Check className="inline h-4 w-4 text-emerald-600" />
                  : <X className="inline h-4 w-4 text-red-600" />}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
```

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/rules/custom/ai-generate/RuleGenChat.tsx \
        front-end/src/app/rules/custom/ai-generate/RuleProposalView.tsx \
        front-end/src/app/rules/custom/ai-generate/TestMatrix.tsx
git commit -m "feat(rules): wizard sub-components (chat, proposal, matrix)"
```

---

### Task 18: Wizard page

**Files:**
- Create: `front-end/src/app/rules/custom/ai-generate/page.tsx`

- [ ] **Step 1: Implement the page**

```tsx
'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { ProtectedRoute } from '@/components/protected-route';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useRuleGenSession } from './useRuleGenSession';
import { RuleGenChat } from './RuleGenChat';
import { RuleProposalView } from './RuleProposalView';
import { TestMatrix } from './TestMatrix';
import type { OscalModelType } from '@/types/rule-gen';

const MODEL_OPTIONS: { value: OscalModelType; label: string }[] = [
  { value: 'catalog', label: 'Catalog' },
  { value: 'profile', label: 'Profile' },
  { value: 'system-security-plan', label: 'System Security Plan' },
  { value: 'component-definition', label: 'Component Definition' },
  { value: 'assessment-plan', label: 'Assessment Plan' },
  { value: 'assessment-results', label: 'Assessment Results' },
  { value: 'plan-of-action-and-milestones', label: 'POA&M' },
];

const STARTER_PROMPTS = [
  'Every control in a catalog must have a non-empty title.',
  'All implemented requirements in an SSP must reference a control id.',
  'Profile imports must reference a catalog by UUID.',
];

export default function AiGenerateRulePage() {
  return (
    <ProtectedRoute>
      <Inner />
    </ProtectedRoute>
  );
}

function Inner() {
  const router = useRouter();
  const session = useRuleGenSession();
  const [model, setModel] = useState<OscalModelType | ''>('');
  const [orgId, setOrgId] = useState<number | null>(null);
  const [ruleId, setRuleId] = useState('');

  // Org id resolution: same pattern as the existing AI wizard pages —
  // currentOrgId() helper or auth-context hook. Replace with whatever
  // /front-end/src/app/ai/wizard/[kind]/page.tsx uses.
  useEffect(() => {
    const stored = window.localStorage.getItem('currentOrganizationId');
    if (stored) setOrgId(Number(stored));
  }, []);

  const begin = async () => {
    if (!model || !orgId) return;
    await session.start(orgId, model);
  };

  const onSave = async () => {
    if (!ruleId.trim()) {
      toast.error('Please choose a rule id');
      return;
    }
    try {
      await session.save(ruleId.trim());
      toast.success('Rule saved');
      router.push('/rules/custom');
    } catch (e) {
      toast.error('Save failed: ' + (e instanceof Error ? e.message : String(e)));
    }
  };

  const matrixClean = useMemo(
    () => (session.latest?.testResults ?? []).every((r) => r.passed),
    [session.latest?.testResults],
  );

  if (!session.sessionId) {
    return (
      <div className="container mx-auto p-6 max-w-2xl">
        <h1 className="text-2xl font-semibold mb-4">Generate a rule with AI</h1>
        <Card className="p-4 space-y-4">
          <div>
            <label className="text-sm font-medium block mb-1">OSCAL model</label>
            <Select value={model} onValueChange={(v) => setModel(v as OscalModelType)}>
              <SelectTrigger><SelectValue placeholder="Pick a model" /></SelectTrigger>
              <SelectContent>
                {MODEL_OPTIONS.map((m) => (
                  <SelectItem key={m.value} value={m.value}>{m.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="text-sm text-muted-foreground">
            <div className="font-medium mb-1">Examples to try:</div>
            <ul className="list-disc pl-5 space-y-1">
              {STARTER_PROMPTS.map((p) => <li key={p}>{p}</li>)}
            </ul>
          </div>
          <Button onClick={begin} disabled={!model || !orgId}>Start</Button>
          {session.error && <p className="text-sm text-red-600">{session.error}</p>}
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 grid grid-cols-1 lg:grid-cols-2 gap-4 h-[calc(100vh-6rem)]">
      <div className="flex flex-col h-full min-h-0">
        <h2 className="text-lg font-semibold mb-2">
          Conversation
          {session.latest && (
            <span className="ml-3 text-xs text-muted-foreground">
              tokens in: {session.latest.totalTokensIn} / out: {session.latest.totalTokensOut}
            </span>
          )}
        </h2>
        <RuleGenChat
          entries={session.chat}
          loading={session.loading}
          disabled={session.latest?.phase === 'proposal' && matrixClean}
          placeholder="Describe what the rule should enforce…"
          onSend={session.send}
        />
        {session.error && <p className="text-sm text-red-600 mt-2">{session.error}</p>}
      </div>
      <div className="flex flex-col gap-4 h-full overflow-auto">
        <RuleProposalView
          proposal={session.latest?.proposal ?? session.latest?.lastProposal ?? null}
          onEdit={session.editConstraint}
          onSave={onSave}
          saveDisabled={!matrixClean || session.latest?.phase !== 'proposal'}
          loading={session.loading}
        />
        {matrixClean && session.latest?.phase === 'proposal' && (
          <Card className="p-4 space-y-2">
            <label className="text-sm font-medium">Rule id</label>
            <Input
              value={ruleId}
              onChange={(e) => setRuleId(e.target.value)}
              placeholder="custom-r-001"
            />
            <p className="text-xs text-muted-foreground">
              Must be unique. This is the id used when this rule fires during validation.
            </p>
          </Card>
        )}
        <TestMatrix results={session.latest?.testResults ?? null} />
        {session.latest?.phase === 'exhausted' && (
          <Card className="p-4 border-amber-300 bg-amber-50 text-sm">
            {session.latest.message}
          </Card>
        )}
        <div className="flex justify-end">
          <Button variant="ghost" onClick={async () => { await session.abandon(); router.push('/rules/custom'); }}>
            Abandon
          </Button>
        </div>
      </div>
    </div>
  );
}
```

If the existing AI wizard page uses a specific organization-id resolution pattern (auth context, useOrg hook, etc.), copy that pattern instead of reading from localStorage.

- [ ] **Step 2: Type-check**

Run: `cd front-end && npm run typecheck`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/rules/custom/ai-generate/page.tsx
git commit -m "feat(rules): AI rule generator wizard page"
```

---

### Task 19: Entry point + AI badge on /rules/custom

**Files:**
- Modify: `front-end/src/app/rules/custom/page.tsx`

- [ ] **Step 1: Add the "Generate with AI" link**

In the existing custom rules page, find the page header / action toolbar (the `<div>` containing the existing "Create" button and search bar). Add a `Link` button next to the existing Create button:

```tsx
import Link from 'next/link';
import { Sparkles } from 'lucide-react';

// inside the toolbar:
<Link href="/rules/custom/ai-generate">
  <Button variant="default">
    <Sparkles className="h-4 w-4 mr-1.5" />
    Generate with AI
  </Button>
</Link>
```

- [ ] **Step 2: Add an "AI" badge to rule cards**

Find the rule card render block in the same file. Where the rule's name + severity badges are rendered, add a conditional badge:

```tsx
{rule.aiGenerated && (
  <Badge variant="outline" className="ml-1.5 gap-1">
    <Sparkles className="h-3 w-3" />
    AI
  </Badge>
)}
```

If `rule.aiGenerated` isn't already on the `CustomRule` type used by this page, add it to that type (mirroring the response field added in Task 1).

- [ ] **Step 3: Visually verify**

Start the dev environment: `./dev.sh`. Navigate to http://localhost:3010/rules/custom and confirm:
- "Generate with AI" button shows in the toolbar.
- Existing rules render unchanged.

Stop with `./stop.sh`.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/rules/custom/page.tsx
git commit -m "feat(rules): entry point + AI badge on custom rules page"
```

---

## Phase 4 — Live test, feature flag, E2E

### Task 20: Real-API live test (gated on ANTHROPIC_API_KEY)

**Files:**
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenLiveTest.java`

This test runs only when `ANTHROPIC_API_KEY` is set. It exercises an end-to-end conversation against the real Claude API and asserts that the generated constraint actually enforces correctly. Match the gating pattern of any existing `*LiveTest` or `*Live*` integration test in the repo.

- [ ] **Step 1: Find the project's gating pattern**

Run: `grep -rl 'EnabledIfEnvironmentVariable\|ANTHROPIC_API_KEY' back-end/src/test/`
Use the same annotation style.

- [ ] **Step 2: Write the live test**

```java
// back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenLiveTest.java
package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.entity.OrgAiSettings;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrgAiSettingsRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@Transactional
class AiRuleGenLiveTest {

    @Autowired AiRuleGenService service;
    @Autowired OrgAiSettingsRepository orgAiRepo;
    @Autowired UserRepository userRepo;
    @Autowired gov.nist.oscal.tools.api.service.ai.AiSettingsService aiSettings;

    @Test
    void wizardReachesCleanProposal() {
        // Set up an org with the live API key.
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(99_999L);
        s.setEnabled(true);
        s.setDefaultModel("claude-opus-4-7");
        s.setCreatedAt(LocalDateTime.now());
        // Use AiSettingsService to encrypt the key (matches prod path).
        aiSettings.setApiKey(99_999L, System.getenv("ANTHROPIC_API_KEY"), "claude-opus-4-7");

        User u = new User();
        u.setUsername("rulegen-live");
        u.setEmail("rulegen-live@example.com");
        u.setPasswordHash("x");
        u.setCreatedDate(LocalDateTime.now());
        userRepo.save(u);

        UUID sid = service.start(99_999L, u.getId(), "catalog");
        var res = service.turn(sid,
            "Every catalog must have a non-empty metadata title.");

        // Acceptable outcomes: clarify or proposal. We only assert that the
        // service did not crash and that any returned proposal has a
        // non-empty constraint and at least one passing test.
        assertThat(res.phase()).isIn("clarify", "proposal");
        if (res.phase().equals("proposal")) {
            assertThat(res.proposal().constraintXml()).isNotBlank();
            assertThat(res.testResults()).isNotEmpty();
        }
    }
}
```

- [ ] **Step 3: Run the test**

Run: `cd back-end && ANTHROPIC_API_KEY=$YOUR_KEY mvn test -Dtest=AiRuleGenLiveTest -q`
Expected: PASS. Without `ANTHROPIC_API_KEY` set, the test is skipped, not failed.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/rulegen/AiRuleGenLiveTest.java
git commit -m "test(rules): real-API live test for rule-gen wizard"
```

---

### Task 21: Feature flag wiring

**Files:**
- Modify: `back-end/src/main/resources/application.properties`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiRuleGenController.java`
- Modify: `front-end/src/app/rules/custom/page.tsx`
- Modify: `front-end/src/app/rules/custom/ai-generate/page.tsx`

- [ ] **Step 1: Backend property + conditional**

In `application.properties`, add:

```properties
app.features.ai-rule-gen.enabled=true
```

In `AiRuleGenController.java`, add the class-level annotation:

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@RequestMapping("/api/rules/ai-generate")
@ConditionalOnProperty(name = "app.features.ai-rule-gen.enabled", havingValue = "true", matchIfMissing = true)
public class AiRuleGenController { ... }
```

- [ ] **Step 2: Frontend env-var gate**

In the existing `/rules/custom/page.tsx`, gate the "Generate with AI" button:

```tsx
{process.env.NEXT_PUBLIC_ENABLE_AI_RULE_GEN !== 'false' && (
  <Link href="/rules/custom/ai-generate">
    <Button variant="default">
      <Sparkles className="h-4 w-4 mr-1.5" />
      Generate with AI
    </Button>
  </Link>
)}
```

In `/rules/custom/ai-generate/page.tsx`, at the very top of `Inner()`, redirect if disabled:

```tsx
useEffect(() => {
  if (process.env.NEXT_PUBLIC_ENABLE_AI_RULE_GEN === 'false') {
    router.replace('/rules/custom');
  }
}, [router]);
```

- [ ] **Step 3: Document the flag**

In `docs/CICD-BOOTSTRAP.md` (or wherever feature toggles are documented in this repo — search for "feature" or "FEATURE"), add a one-line entry:

```
- app.features.ai-rule-gen.enabled (default true) — gates the AI rule generation wizard.
- NEXT_PUBLIC_ENABLE_AI_RULE_GEN=false — hides the wizard entry point in the frontend.
```

If no central doc exists, skip — the property names are self-documenting.

- [ ] **Step 4: Verify the flag works**

Run: `cd back-end && SPRING_PROFILES_ACTIVE=dev APP_FEATURES_AI_RULE_GEN_ENABLED=false mvn spring-boot:run -Dspring-boot.run.fork=false 2>&1 | head -60`

Hit `curl -i -X POST http://localhost:8090/api/rules/ai-generate/sessions -H 'Content-Type: application/json' -d '{}'`
Expected: 404 (controller not loaded). Stop the server.

Restart without the env var: same curl returns 401 or 400 (controller loaded; request rejected for auth/validation).

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/resources/application.properties \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiRuleGenController.java \
        front-end/src/app/rules/custom/page.tsx \
        front-end/src/app/rules/custom/ai-generate/page.tsx
git commit -m "feat(rules): feature-flag the AI rule-gen surface"
```

---

### Task 22: Frontend Playwright E2E

**Files:**
- Create: `front-end/e2e/rules-ai-generate.spec.ts`

- [ ] **Step 1: Find the existing E2E pattern**

Run: `ls front-end/e2e/` and read one existing spec, especially how it logs in and how it mocks/intercepts API calls.

- [ ] **Step 2: Write the spec**

```ts
// front-end/e2e/rules-ai-generate.spec.ts
import { test, expect } from '@playwright/test';

test('AI rule wizard happy path with mocked API', async ({ page }) => {
  // Login flow — match existing E2E specs for cookie/token setup.
  await page.goto('/login');
  await page.fill('[name="username"]', 'alice');
  await page.fill('[name="password"]', 'alice-password');
  await page.click('button[type="submit"]');
  await page.waitForURL(/\/(dashboard|home|rules)/);

  // Mock the rule-gen API.
  await page.route('**/api/rules/ai-generate/sessions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ sessionId: '00000000-0000-0000-0000-000000000001' }),
    });
  });
  await page.route('**/api/rules/ai-generate/sessions/*/turn', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        phase: 'proposal',
        clarifyingQuestion: null,
        proposal: {
          name: 'Mock rule',
          description: 'Always passes',
          severity: 'error',
          fieldPath: 'metadata',
          constraintXml: '<assembly target="metadata"><expect id="x" level="ERROR" test="true()"><message>m</message></expect></assembly>',
          testCases: [
            { description: 'a', fragmentJson: '{}', expected: 'pass' },
          ],
        },
        testResults: [
          { index: 0, description: 'a', expected: 'pass', actual: 'pass', passed: true, violationMessage: null },
        ],
        lastProposal: null,
        message: null,
        iterations: 1,
        totalTokensIn: 100,
        totalTokensOut: 200,
      }),
    });
  });

  await page.goto('/rules/custom');
  await page.click('a[href="/rules/custom/ai-generate"]');
  await page.waitForURL('**/rules/custom/ai-generate');

  await page.click('text=Pick a model');
  await page.click('text=Catalog');
  await page.click('button:has-text("Start")');

  await page.fill('textarea', 'Catalog must have a title.');
  await page.click('button:has-text("Send")');

  await expect(page.locator('text=Mock rule')).toBeVisible();
  await expect(page.locator('text=Save rule')).toBeEnabled();
});
```

- [ ] **Step 3: Run the spec**

Run: `cd front-end && npm run test:e2e -- rules-ai-generate`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add front-end/e2e/rules-ai-generate.spec.ts
git commit -m "test(rules): E2E spec for AI rule-gen wizard"
```

---

### Task 23: Smoke and final pass

- [ ] **Step 1: Full backend test suite**

Run: `cd back-end && mvn test -q`
Expected: PASS.

- [ ] **Step 2: Full frontend test suite**

Run: `cd front-end && npm test`
Expected: PASS.

- [ ] **Step 3: Manual smoke**

Start `./dev.sh`, log in, navigate to `/rules/custom`, click "Generate with AI", pick "Catalog", describe a simple rule. Verify:
- Clarification or proposal returns within reasonable latency (under 60s).
- Test matrix renders with rows.
- Save button is disabled until matrix is fully green.
- Saving creates a rule visible on `/rules/custom` with the AI badge.
- Uploading a violating OSCAL doc to the validation endpoint produces a violation message containing `[custom: <ruleId>]`.

Stop with `./stop.sh`.

- [ ] **Step 4: Commit anything pulled up by the smoke**

If the smoke uncovered a small fix, commit it scoped to the issue. If everything's clean, no commit is needed.

---

## Done

Total commits: ~22 (one per task). Total backend code added: ~1,200 LoC. Total frontend code added: ~600 LoC. Real-API integration test gated on `ANTHROPIC_API_KEY` so CI without the secret skips it cleanly.

Risk hot-spots to monitor in code review:

1. **Per-request `IBindingContext` build** — verify Caffeine cache hit rate in production. If cold-cache builds dominate, consider warming on startup.
2. **AnthropicClient tool-use raw JSON path** — the `putAdditionalBodyProperty` escape hatch sidesteps SDK type-checking. If the SDK ships first-class tool-use types in a future bump, migrate.
3. **Session ownership check** — covered by `requireOwnership` in the controller, but worth confirming during review that users cannot send turns to another user's session id.
4. **Schema summary drift** — the static text assets need to be updated when liboscal-java upgrades introduce new fields. Treat as part of dependency-bump work.



