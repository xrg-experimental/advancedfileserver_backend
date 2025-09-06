package com.sme.afs.repository;

import com.sme.afs.model.FileTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileTagRepository extends JpaRepository<FileTag, Long> {
    Optional<FileTag> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
