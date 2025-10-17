package gov.nist.oscal.tools.api.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationRuleCategory {
    private String id;
    private String name;
    private String description;
    private List<ValidationRule> rules = new ArrayList<>();
    private int ruleCount;

    // Constructors
    public ValidationRuleCategory() {
    }

    public ValidationRuleCategory(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ValidationRule> getRules() {
        return rules;
    }

    public void setRules(List<ValidationRule> rules) {
        this.rules = rules;
        this.ruleCount = rules.size();
    }

    public int getRuleCount() {
        return ruleCount;
    }

    public void setRuleCount(int ruleCount) {
        this.ruleCount = ruleCount;
    }

    // Helper methods
    public void addRule(ValidationRule rule) {
        this.rules.add(rule);
        this.ruleCount = this.rules.size();
    }
}
