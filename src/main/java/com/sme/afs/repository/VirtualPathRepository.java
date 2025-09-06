package com.sme.afs.repository;

import com.sme.afs.model.VirtualPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VirtualPathRepository extends JpaRepository<VirtualPath, Long> {
    Optional<VirtualPath> findByVirtualPathAndIsDeletedFalse(String virtualPath);
    
    @Query("SELECT v FROM VirtualPath v WHERE v.virtualPath LIKE :parentPath || '/%' AND v.isDeleted = false")
    List<VirtualPath> findChildrenByPath(String parentPath);
    
    List<VirtualPath> findByParentIdAndIsDeletedFalse(Long parentId);
    
    boolean existsByVirtualPathAndIsDeletedFalse(String virtualPath);
    
    Optional<VirtualPath> findByPhysicalPathAndIsDeletedFalse(String physicalPath);
    
    @Query("SELECT v FROM VirtualPath v WHERE v.virtualPath LIKE CONCAT(:basePath, '/%') AND v.isDeleted = false")
    List<VirtualPath> findAllByBasePathAndIsDeletedFalse(String basePath);
    
    @Query("SELECT COUNT(v) > 0 FROM VirtualPath v WHERE v.parent.id = :parentId AND v.name = :name AND v.isDeleted = false")
    boolean existsByParentIdAndNameAndIsDeletedFalse(Long parentId, String name);
}
