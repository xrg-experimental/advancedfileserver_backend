package com.sme.afs.repository;

import com.sme.afs.model.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    Optional<FileShare> findByAccessToken(String accessToken);
    
    List<FileShare> findByFileId(Long fileId);
    
    @Query("SELECT fs FROM FileShare fs WHERE fs.expiresAt < :now")
    List<FileShare> findExpiredShares(LocalDateTime now);
    
    List<FileShare> findBySharedWithId(Long userId);
    
    List<FileShare> findBySharedWithGroupId(Long groupId);
}
