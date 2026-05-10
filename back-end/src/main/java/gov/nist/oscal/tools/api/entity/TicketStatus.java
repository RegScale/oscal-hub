package gov.nist.oscal.tools.api.entity;

import java.util.Set;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    WONT_FIX,
    DUPLICATE;

    private static final Set<TicketStatus> TERMINAL =
        Set.of(CLOSED, WONT_FIX, DUPLICATE);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canReopen() {
        return this == RESOLVED;
    }
}
