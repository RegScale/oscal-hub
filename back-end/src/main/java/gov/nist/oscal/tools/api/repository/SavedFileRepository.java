package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.SavedFileEntity;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedFileRepository extends JpaRepository<SavedFileEntity, Long> {

    Optional<SavedFileEntity> findByFileId(String fileId);

    List<SavedFileEntity> findByUserOrderByUploadedAtDesc(User user);

    List<SavedFileEntity> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<SavedFileEntity> findByFileIdAndUserId(String fileId, Long userId);

    void deleteByFileIdAndUserId(String fileId, Long userId);
}
