package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationDocument;
import gov.nist.oscal.tools.api.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizationDocumentRepository extends JpaRepository<AuthorizationDocument, Long> {

    List<AuthorizationDocument> findByAuthorizationOrderByUploadedAtDesc(Authorization authorization);

    Optional<AuthorizationDocument> findByIdAndAuthorization(Long id, Authorization authorization);

    @Query("SELECT d FROM AuthorizationDocument d " +
           "WHERE d.authorization = :authorization AND d.documentType = :type " +
           "ORDER BY d.uploadedAt DESC")
    List<AuthorizationDocument> findByAuthorizationAndType(
            @Param("authorization") Authorization authorization,
            @Param("type") DocumentType type);

    @Query("SELECT d FROM AuthorizationDocument d WHERE d.authorization = :authorization AND (" +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(d.tags, '')) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<AuthorizationDocument> searchInAuthorization(
            @Param("authorization") Authorization authorization,
            @Param("q") String q);
}
