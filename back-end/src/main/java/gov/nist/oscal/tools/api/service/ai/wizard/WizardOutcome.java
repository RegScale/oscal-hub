package gov.nist.oscal.tools.api.service.ai.wizard;

public record WizardOutcome(boolean success, int tokensIn, int tokensOut, String errorCode, String errorMessage) {
    public static WizardOutcome ok(int in, int out) { return new WizardOutcome(true, in, out, null, null); }
    public static WizardOutcome failed(String code, String msg) { return new WizardOutcome(false, 0, 0, code, msg); }
}
