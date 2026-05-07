package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConMonReconciliationServiceTest {

    private final ConMonReconciliationService service = new ConMonReconciliationService();

    @Test
    void detectsAllSixCategories() {
        ConMonSnapshot prev = snap();
        ConMonSnapshot curr = snap();

        // P-1: still_open  (open in both)
        addItem(prev, "P-1", "Still open", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-1", "Still open", ConMonItemStatus.OPEN, "HIGH", null);

        // P-2: closed (open → closed)
        addItem(prev, "P-2", "Will close", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-2", "Will close", ConMonItemStatus.CLOSED, "HIGH", null);

        // P-3: reopened (closed → open)
        addItem(prev, "P-3", "Came back", ConMonItemStatus.CLOSED, "MODERATE", null);
        addItem(curr, "P-3", "Came back", ConMonItemStatus.OPEN, "MODERATE", null);

        // P-4: changed (severity bumped)
        addItem(prev, "P-4", "Bumped", ConMonItemStatus.OPEN, "MODERATE", null);
        addItem(curr, "P-4", "Bumped", ConMonItemStatus.OPEN, "HIGH", null);

        // P-5: new (only in curr)
        addItem(curr, "P-5", "Brand new", ConMonItemStatus.OPEN, "LOW", null);

        // P-6: removed (only in prev)
        addItem(prev, "P-6", "Vanished", ConMonItemStatus.OPEN, "LOW", null);

        ConMonReconciliation rec = service.compute(curr, prev);

        assertThat(rec.getNewCount()).isEqualTo(1);
        assertThat(rec.getClosedCount()).isEqualTo(1);
        assertThat(rec.getReopenedCount()).isEqualTo(1);
        assertThat(rec.getStillOpenCount()).isEqualTo(1);
        assertThat(rec.getRemovedCount()).isEqualTo(1);
        assertThat(rec.getChangedCount()).isEqualTo(1);
        assertThat(rec.getSnapshot()).isSameAs(curr);
        assertThat(rec.getPreviousSnapshot()).isSameAs(prev);
    }

    @Test
    void titleChangeIsAlsoChanged() {
        ConMonSnapshot prev = snap();
        ConMonSnapshot curr = snap();
        addItem(prev, "P-1", "Old title", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-1", "New title", ConMonItemStatus.OPEN, "HIGH", null);

        ConMonReconciliation rec = service.compute(curr, prev);
        assertThat(rec.getChangedCount()).isEqualTo(1);
        assertThat(rec.getStillOpenCount()).isEqualTo(0);
    }

    @Test
    void emptyPrev_allItemsAreNew() {
        ConMonSnapshot prev = snap();
        ConMonSnapshot curr = snap();
        addItem(curr, "P-1", "x", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-2", "y", ConMonItemStatus.CLOSED, null, null);

        ConMonReconciliation rec = service.compute(curr, prev);
        assertThat(rec.getNewCount()).isEqualTo(2);
    }

    private ConMonSnapshot snap() {
        ConMonSnapshot s = new ConMonSnapshot();
        s.setItems(new ArrayList<>());
        return s;
    }

    private void addItem(ConMonSnapshot s, String extId, String title,
                         ConMonItemStatus status, String severity, java.time.LocalDate sched) {
        ConMonPoamItem i = new ConMonPoamItem();
        i.setSnapshot(s);
        i.setExternalId(extId);
        i.setTitle(title);
        i.setStatus(status);
        i.setSeverity(severity);
        i.setScheduledCompletionDate(sched);
        s.getItems().add(i);
    }
}
