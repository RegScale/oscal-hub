package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a user's membership in an organization.
 * A user can belong to multiple organizations with different roles in each.
 */
@Entity
@Table(name = "organization_memberships",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}))
public class OrganizationMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationRole role = OrganizationRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public OrganizationMembership() {
        this.joinedAt = LocalDateTime.now();
        this.status = MembershipStatus.ACTIVE;
        this.role = OrganizationRole.USER;
    }

    public OrganizationMembership(User user, Organization organization, OrganizationRole role) {
        this();
        this.user = user;
        this.organization = organization;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public OrganizationRole getRole() {
        return role;
    }

    public void setRole(OrganizationRole role) {
        this.role = role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Organization-level roles
     */
    public enum OrganizationRole {
        ORG_ADMIN,  // Can manage users and access requests within the organization
        USER        // Standard user with access to organization's resources
    }

    /**
     * Membership status
     */
    public enum MembershipStatus {
        ACTIVE,       // Active member with full access
        LOCKED,       // Temporarily locked by admin
        DEACTIVATED   // Deactivated by admin
    }
}
