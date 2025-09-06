package com.sme.afs.repository;

import com.sme.afs.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileIdOrderByVersionNumberDesc(Long fileId);
    FileVersion findTopByFileIdOrderByVersionNumberDesc(Long fileId);
}
