package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Computes a six-category diff between a current snapshot and the immediate
 * prior snapshot. Items match across snapshots by external_id.
 */
@Service
public class ConMonReconciliationService {

    public ConMonReconciliation compute(ConMonSnapshot current, ConMonSnapshot previous) {
        Objects.requireNonNull(current);
        Objects.requireNonNull(previous);

        Map<String, ConMonPoamItem> prevByExt = new HashMap<>();
        for (ConMonPoamItem p : previous.getItems()) {
            if (p.getExternalId() != null) prevByExt.put(p.getExternalId(), p);
        }
        Map<String, ConMonPoamItem> currByExt = new HashMap<>();
        for (ConMonPoamItem c : current.getItems()) {
            if (c.getExternalId() != null) currByExt.put(c.getExternalId(), c);
        }

        int newCount = 0, closedCount = 0, reopenedCount = 0;
        int stillOpen = 0, removed = 0, changed = 0;

        for (var entry : currByExt.entrySet()) {
            ConMonPoamItem curr = entry.getValue();
            ConMonPoamItem prev = prevByExt.get(entry.getKey());
            if (prev == null) {
                newCount++;
                continue;
            }
            // Status transitions
            ConMonItemStatus pStatus = prev.getStatus();
            ConMonItemStatus cStatus = curr.getStatus();
            if (pStatus == ConMonItemStatus.OPEN && cStatus == ConMonItemStatus.CLOSED) {
                closedCount++;
            } else if (pStatus == ConMonItemStatus.CLOSED && cStatus == ConMonItemStatus.OPEN) {
                reopenedCount++;
            } else if (pStatus == ConMonItemStatus.OPEN && cStatus == ConMonItemStatus.OPEN
                    && fieldsEqual(prev, curr)) {
                stillOpen++;
            } else if (fieldsDiffer(prev, curr)) {
                changed++;
            }
        }
        for (String prevExt : prevByExt.keySet()) {
            if (!currByExt.containsKey(prevExt)) removed++;
        }

        ConMonReconciliation rec = new ConMonReconciliation();
        rec.setSnapshot(current);
        rec.setPreviousSnapshot(previous);
        rec.setNewCount(newCount);
        rec.setClosedCount(closedCount);
        rec.setReopenedCount(reopenedCount);
        rec.setStillOpenCount(stillOpen);
        rec.setRemovedCount(removed);
        rec.setChangedCount(changed);
        return rec;
    }

    private static boolean fieldsEqual(ConMonPoamItem a, ConMonPoamItem b) {
        return Objects.equals(a.getTitle(), b.getTitle())
                && Objects.equals(a.getSeverity(), b.getSeverity())
                && Objects.equals(a.getScheduledCompletionDate(), b.getScheduledCompletionDate())
                && Objects.equals(a.getStatus(), b.getStatus());
    }

    private static boolean fieldsDiffer(ConMonPoamItem a, ConMonPoamItem b) {
        return !fieldsEqual(a, b);
    }
}
