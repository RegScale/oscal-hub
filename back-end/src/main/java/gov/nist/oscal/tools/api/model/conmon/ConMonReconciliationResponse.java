package gov.nist.oscal.tools.api.model.conmon;

import java.util.List;

public class ConMonReconciliationResponse {
    private Long snapshotId;
    private Long previousSnapshotId;
    private int newCount;
    private int closedCount;
    private int reopenedCount;
    private int stillOpenCount;
    private int removedCount;
    private int changedCount;
    private List<ConMonPoamItemResponse> newItems;
    private List<ConMonPoamItemResponse> newlyClosedItems;
    private List<ConMonPoamItemResponse> reopenedItems;
    private List<ConMonPoamItemResponse> removedItems;
    private List<ChangedItem> changedItems;

    public static class ChangedItem {
        private ConMonPoamItemResponse current;
        private ConMonPoamItemResponse previous;
        private List<String> fieldsChanged;
        public ChangedItem() {}
        public ChangedItem(ConMonPoamItemResponse curr, ConMonPoamItemResponse prev, List<String> fields) {
            this.current = curr; this.previous = prev; this.fieldsChanged = fields;
        }
        public ConMonPoamItemResponse getCurrent() { return current; }
        public void setCurrent(ConMonPoamItemResponse c) { this.current = c; }
        public ConMonPoamItemResponse getPrevious() { return previous; }
        public void setPrevious(ConMonPoamItemResponse p) { this.previous = p; }
        public List<String> getFieldsChanged() { return fieldsChanged; }
        public void setFieldsChanged(List<String> f) { this.fieldsChanged = f; }
    }

    public ConMonReconciliationResponse() {}

    public Long getSnapshotId() { return snapshotId; } public void setSnapshotId(Long id) { this.snapshotId = id; }
    public Long getPreviousSnapshotId() { return previousSnapshotId; } public void setPreviousSnapshotId(Long id) { this.previousSnapshotId = id; }
    public int getNewCount() { return newCount; } public void setNewCount(int n) { this.newCount = n; }
    public int getClosedCount() { return closedCount; } public void setClosedCount(int n) { this.closedCount = n; }
    public int getReopenedCount() { return reopenedCount; } public void setReopenedCount(int n) { this.reopenedCount = n; }
    public int getStillOpenCount() { return stillOpenCount; } public void setStillOpenCount(int n) { this.stillOpenCount = n; }
    public int getRemovedCount() { return removedCount; } public void setRemovedCount(int n) { this.removedCount = n; }
    public int getChangedCount() { return changedCount; } public void setChangedCount(int n) { this.changedCount = n; }
    public List<ConMonPoamItemResponse> getNewItems() { return newItems; } public void setNewItems(List<ConMonPoamItemResponse> l) { this.newItems = l; }
    public List<ConMonPoamItemResponse> getNewlyClosedItems() { return newlyClosedItems; } public void setNewlyClosedItems(List<ConMonPoamItemResponse> l) { this.newlyClosedItems = l; }
    public List<ConMonPoamItemResponse> getReopenedItems() { return reopenedItems; } public void setReopenedItems(List<ConMonPoamItemResponse> l) { this.reopenedItems = l; }
    public List<ConMonPoamItemResponse> getRemovedItems() { return removedItems; } public void setRemovedItems(List<ConMonPoamItemResponse> l) { this.removedItems = l; }
    public List<ChangedItem> getChangedItems() { return changedItems; } public void setChangedItems(List<ChangedItem> l) { this.changedItems = l; }
}
