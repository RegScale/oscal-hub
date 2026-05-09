package gov.nist.oscal.tools.api.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicketStatusTest {

    @Test
    void terminalStates() {
        assertTrue(TicketStatus.CLOSED.isTerminal());
        assertTrue(TicketStatus.WONT_FIX.isTerminal());
        assertTrue(TicketStatus.DUPLICATE.isTerminal());
        assertFalse(TicketStatus.OPEN.isTerminal());
        assertFalse(TicketStatus.IN_PROGRESS.isTerminal());
        assertFalse(TicketStatus.RESOLVED.isTerminal());
    }

    @Test
    void canReopen() {
        assertTrue(TicketStatus.RESOLVED.canReopen());
        assertFalse(TicketStatus.CLOSED.canReopen());
        assertFalse(TicketStatus.OPEN.canReopen());
    }
}
