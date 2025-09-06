package com.sme.afs.repository;

import com.sme.afs.model.SharedFolderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedFolderConfigRepository extends JpaRepository<SharedFolderConfig, Long> {
    List<SharedFolderConfig> findByIsBasePath(boolean isBasePath);
    Optional<SharedFolderConfig> findByIsTempPath(boolean isTempPath);
    boolean existsByPath(String path);
    Optional<SharedFolderConfig> findByPath(String path);
}
