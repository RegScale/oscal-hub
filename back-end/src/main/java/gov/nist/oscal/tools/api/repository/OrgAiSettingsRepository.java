package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.OrgAiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgAiSettingsRepository extends JpaRepository<OrgAiSettings, Long> {
    Optional<OrgAiSettings> findByOrganizationId(Long organizationId);
}
