package gov.nist.oscal.tools.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationRulesResponse {
    private int totalRules;
    private int builtInRules;
    private int customRules;
    private Map<String, Integer> rulesByModelType = new HashMap<>();
    private Map<String, Integer> rulesByCategory = new HashMap<>();
    private List<ValidationRuleCategory> categories = new ArrayList<>();
    private List<ValidationRule> rules = new ArrayList<>();

    // Constructors
    public ValidationRulesResponse() {
    }

    // Getters and Setters
    public int getTotalRules() {
        return totalRules;
    }

    public void setTotalRules(int totalRules) {
        this.totalRules = totalRules;
    }

    public int getBuiltInRules() {
        return builtInRules;
    }

    public void setBuiltInRules(int builtInRules) {
        this.builtInRules = builtInRules;
    }

    public int getCustomRules() {
        return customRules;
    }

    public void setCustomRules(int customRules) {
        this.customRules = customRules;
    }

    public Map<String, Integer> getRulesByModelType() {
        return rulesByModelType;
    }

    public void setRulesByModelType(Map<String, Integer> rulesByModelType) {
        this.rulesByModelType = rulesByModelType;
    }

    public Map<String, Integer> getRulesByCategory() {
        return rulesByCategory;
    }

    public void setRulesByCategory(Map<String, Integer> rulesByCategory) {
        this.rulesByCategory = rulesByCategory;
    }

    public List<ValidationRuleCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ValidationRuleCategory> categories) {
        this.categories = categories;
    }

    public List<ValidationRule> getRules() {
        return rules;
    }

    public void setRules(List<ValidationRule> rules) {
        this.rules = rules;
    }

    // Helper methods
    public void addRule(ValidationRule rule) {
        this.rules.add(rule);
    }

    public void addCategory(ValidationRuleCategory category) {
        this.categories.add(category);
    }

    public void calculateStats() {
        this.totalRules = this.rules.size();
        this.builtInRules = (int) this.rules.stream().filter(ValidationRule::isBuiltIn).count();
        this.customRules = this.totalRules - this.builtInRules;

        // Calculate rules by model type
        this.rulesByModelType.clear();
        for (ValidationRule rule : this.rules) {
            for (OscalModelType modelType : rule.getApplicableModelTypes()) {
                String key = modelType.getValue();
                this.rulesByModelType.put(key, this.rulesByModelType.getOrDefault(key, 0) + 1);
            }
        }

        // Calculate rules by category
        this.rulesByCategory.clear();
        for (ValidationRule rule : this.rules) {
            String category = rule.getCategory();
            if (category != null && !category.isEmpty()) {
                this.rulesByCategory.put(category, this.rulesByCategory.getOrDefault(category, 0) + 1);
            }
        }
    }
}
