package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ReusableElement;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReusableElementRepository extends JpaRepository<ReusableElement, Long> {

    /**
     * Find all elements created by a specific user
     */
    List<ReusableElement> findByCreatedBy(User user);

    /**
     * Find elements by type for a specific user
     */
    List<ReusableElement> findByCreatedByAndType(User user, ReusableElement.ElementType type);

    /**
     * Find elements by type (all users, for shared elements)
     */
    List<ReusableElement> findByType(ReusableElement.ElementType type);

    /**
     * Find shared elements by type
     */
    List<ReusableElement> findByTypeAndIsShared(ReusableElement.ElementType type, boolean isShared);

    /**
     * Find elements by name (case-insensitive partial match)
     */
    List<ReusableElement> findByNameContainingIgnoreCase(String name);

    /**
     * Find elements by user and name
     */
    List<ReusableElement> findByCreatedByAndNameContainingIgnoreCase(User user, String name);

    /**
     * Find recently created elements by user
     */
    @Query("SELECT e FROM ReusableElement e WHERE e.createdBy = :user ORDER BY e.createdAt DESC")
    List<ReusableElement> findByCreatedByOrderByCreatedAtDesc(@Param("user") User user);

    /**
     * Find most used elements by user
     */
    @Query("SELECT e FROM ReusableElement e WHERE e.createdBy = :user ORDER BY e.useCount DESC")
    List<ReusableElement> findByCreatedByOrderByUseCountDesc(@Param("user") User user);

    /**
     * Find elements by user with optional type filter
     */
    @Query("SELECT e FROM ReusableElement e WHERE e.createdBy = :user " +
           "AND (:type IS NULL OR e.type = :type) " +
           "ORDER BY e.createdAt DESC")
    List<ReusableElement> findByCreatedByAndOptionalType(@Param("user") User user,
                                                          @Param("type") ReusableElement.ElementType type);

    /**
     * Search elements by user, name, and optional type
     */
    @Query("SELECT e FROM ReusableElement e WHERE e.createdBy = :user " +
           "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND (:type IS NULL OR e.type = :type) " +
           "ORDER BY e.createdAt DESC")
    List<ReusableElement> searchByUserNameAndType(@Param("user") User user,
                                                   @Param("searchTerm") String searchTerm,
                                                   @Param("type") ReusableElement.ElementType type);

    /**
     * Count elements by type for a user
     */
    @Query("SELECT COUNT(e) FROM ReusableElement e WHERE e.createdBy = :user AND e.type = :type")
    long countByCreatedByAndType(@Param("user") User user, @Param("type") ReusableElement.ElementType type);
}
