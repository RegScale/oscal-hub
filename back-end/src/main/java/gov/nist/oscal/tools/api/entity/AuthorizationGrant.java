package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "authorization_grants",
       uniqueConstraints = @UniqueConstraint(name = "uq_authorization_grants_user",
                                             columnNames = {"authorization_id", "user_id"}))
public class AuthorizationGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id", nullable = false)
    private Authorization authorization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthorizationRole role;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt = LocalDateTime.now();

    public AuthorizationGrant() {
    }

    public AuthorizationGrant(Authorization authorization, User user, AuthorizationRole role, User grantedBy) {
        this.authorization = authorization;
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
        this.grantedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Authorization getAuthorization() { return authorization; }
    public void setAuthorization(Authorization authorization) { this.authorization = authorization; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }

    public User getGrantedBy() { return grantedBy; }
    public void setGrantedBy(User grantedBy) { this.grantedBy = grantedBy; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
}
