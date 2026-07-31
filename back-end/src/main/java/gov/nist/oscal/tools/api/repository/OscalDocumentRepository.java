package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OscalDocumentRepository extends JpaRepository<OscalDocument, Long> {

    Optional<OscalDocument> findByOscalUuid(String oscalUuid);

    /**
     * Leaderboard: documents created per user since the cutoff.
     * Rows are [userId, count]. Pass epoch for all-time.
     */
    @Query("SELECT d.createdBy.id, COUNT(d) FROM OscalDocument d "
            + "WHERE d.createdAt >= :cutoff GROUP BY d.createdBy.id")
    List<Object[]> countCreatedPerUserSince(@Param("cutoff") java.time.LocalDateTime cutoff);

    @Query("SELECT d FROM OscalDocument d WHERE d.createdBy = :user AND d.modelType = :modelType ORDER BY d.createdAt DESC")
    List<OscalDocument> findByUserAndType(@Param("user") User user, @Param("modelType") OscalModelType modelType);

    @Query("SELECT d FROM OscalDocument d WHERE d.createdBy = :user AND d.modelType = :modelType AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :term, '%'))) " +
           "ORDER BY d.createdAt DESC")
    List<OscalDocument> searchByUserAndType(
            @Param("user") User user,
            @Param("modelType") OscalModelType modelType,
            @Param("term") String term);
}
