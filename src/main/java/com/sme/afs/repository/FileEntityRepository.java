package com.sme.afs.repository;

import com.sme.afs.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileEntityRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByVirtualPath_VirtualPathAndIsDeletedFalse(String virtualPath);
    List<FileEntity> findByGroupIdAndIsDeletedFalse(Long groupId);
    
    @Query("SELECT f FROM FileEntity f WHERE f.virtualPath.virtualPath LIKE CONCAT(:parentPath, '/%') AND f.isDeleted = false")
    List<FileEntity> findChildrenByPath(String parentPath);
    
    boolean existsByVirtualPath_VirtualPathAndIsDeletedFalse(String virtualPath);
}
