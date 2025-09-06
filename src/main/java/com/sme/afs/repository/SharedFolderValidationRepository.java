package com.sme.afs.repository;

import com.sme.afs.model.SharedFolderValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedFolderValidationRepository extends JpaRepository<SharedFolderValidation, Long> {
    Optional<SharedFolderValidation> findByConfigId(Long configId);
}
