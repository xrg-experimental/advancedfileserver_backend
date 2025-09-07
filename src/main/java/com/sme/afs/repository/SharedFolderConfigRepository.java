package com.sme.afs.repository;

import com.sme.afs.model.SharedFolderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedFolderConfigRepository extends JpaRepository<SharedFolderConfig, Long> {
    Optional<SharedFolderConfig> findByPath(String path);
}
