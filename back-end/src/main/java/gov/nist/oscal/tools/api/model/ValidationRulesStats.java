package gov.nist.oscal.tools.api.model;

import java.util.HashMap;
import java.util.Map;

public class ValidationRulesStats {
    private int totalRules;
    private int builtInRules;
    private int customRules;
    private Map<String, Integer> rulesByModelType = new HashMap<>();
    private Map<String, Integer> rulesByCategory = new HashMap<>();

    // Constructors
    public ValidationRulesStats() {
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
}
