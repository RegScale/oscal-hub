package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void nullUserReturnsSingletonContext() {
        // No DB hit when userId is null.
        IBindingContext ctx = svc.contextFor("catalog", null);
        assertThat(ctx).isSameAs(OscalBindingContext.instance());
    }

    @Test
    void noRulesReturnsSingletonContext() {
        when(repo.findEnabledRulesForModelTypeAndUser(anyString(), anyLong()))
            .thenReturn(List.of());

        IBindingContext ctx = svc.contextFor("catalog", 1L);

        assertThat(ctx).isSameAs(OscalBindingContext.instance());
    }

    @Test
    void cachesContextByContentHash() {
        CustomValidationRule rule = newRule("r-1", "catalog",
            "<assembly target=\"metadata\"><expect id=\"x\" level=\"ERROR\" test=\"true()\">"
            + "<message>m</message></expect></assembly>");
        when(repo.findEnabledRulesForModelTypeAndUser("catalog", 1L))
            .thenReturn(List.of(rule));

        IBindingContext a = svc.contextFor("catalog", 1L);
        IBindingContext b = svc.contextFor("catalog", 1L);

        assertThat(a).isSameAs(b);
        assertThat(a).isNotSameAs(OscalBindingContext.instance());
    }

    @Test
    void evictUserClearsCache() {
        CustomValidationRule rule = newRule("r-1", "catalog",
            "<assembly target=\"metadata\"><expect id=\"x\" level=\"ERROR\" test=\"true()\">"
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
