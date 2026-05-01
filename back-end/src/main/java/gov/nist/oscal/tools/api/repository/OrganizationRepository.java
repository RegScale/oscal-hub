package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Organization;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByName(String name);

    List<Organization> findByActiveTrue();

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    /**
     * Find newest organizations (ordered by creation date desc)
     */
    List<Organization> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
