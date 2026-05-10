package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "conmon_reconciliations")
public class ConMonReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false, unique = true)
    private ConMonSnapshot snapshot;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_snapshot_id", nullable = false)
    private ConMonSnapshot previousSnapshot;

    @Column(name = "new_count", nullable = false) private int newCount;
    @Column(name = "closed_count", nullable = false) private int closedCount;
    @Column(name = "reopened_count", nullable = false) private int reopenedCount;
    @Column(name = "still_open_count", nullable = false) private int stillOpenCount;
    @Column(name = "removed_count", nullable = false) private int removedCount;
    @Column(name = "changed_count", nullable = false) private int changedCount;

    public ConMonReconciliation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConMonSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(ConMonSnapshot s) { this.snapshot = s; }
    public ConMonSnapshot getPreviousSnapshot() { return previousSnapshot; }
    public void setPreviousSnapshot(ConMonSnapshot s) { this.previousSnapshot = s; }
    public int getNewCount() { return newCount; }
    public void setNewCount(int n) { this.newCount = n; }
    public int getClosedCount() { return closedCount; }
    public void setClosedCount(int n) { this.closedCount = n; }
    public int getReopenedCount() { return reopenedCount; }
    public void setReopenedCount(int n) { this.reopenedCount = n; }
    public int getStillOpenCount() { return stillOpenCount; }
    public void setStillOpenCount(int n) { this.stillOpenCount = n; }
    public int getRemovedCount() { return removedCount; }
    public void setRemovedCount(int n) { this.removedCount = n; }
    public int getChangedCount() { return changedCount; }
    public void setChangedCount(int n) { this.changedCount = n; }
}
