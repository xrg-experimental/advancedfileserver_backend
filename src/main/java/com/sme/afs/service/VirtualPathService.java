package com.sme.afs.service;

import com.sme.afs.exception.AfsException;
import com.sme.afs.model.User;
import com.sme.afs.model.VirtualPath;
import com.sme.afs.repository.VirtualPathRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualPathService {
    
    private final VirtualPathRepository virtualPathRepository;
    private final SharedFolderValidator sharedFolderValidator;

    @Transactional
    public VirtualPath createDirectory(String virtualPath, String physicalPath, User user) {
        validatePaths(virtualPath, physicalPath);

        if (virtualPathRepository.existsByVirtualPathAndIsDeletedFalse(virtualPath)) {
            throw new AfsException(HttpStatus.CONFLICT, "Virtual path already exists: " + virtualPath);
        }

        String parentVirtualPath = getParentPath(virtualPath);
        VirtualPath parent = null;
        if (!parentVirtualPath.equals("/")) {
            parent = virtualPathRepository.findByVirtualPathAndIsDeletedFalse(parentVirtualPath)
                .orElseThrow(() -> new AfsException(HttpStatus.NOT_FOUND, 
                    "Parent path not found: " + parentVirtualPath));
        }

        VirtualPath vPath = new VirtualPath();
        vPath.setVirtualPath(virtualPath);
        vPath.setPhysicalPath(physicalPath);
        vPath.setName(getNameFromPath(virtualPath));
        vPath.setDirectory(true);
        vPath.setParent(parent);
        vPath.setCreatedAt(LocalDateTime.now());
        vPath.setCreatedBy(user);
        vPath.setModifiedAt(LocalDateTime.now());
        vPath.setModifiedBy(user);

        return virtualPathRepository.save(vPath);
    }

    @Transactional
    public void deleteDirectory(String virtualPath, User user) {
        VirtualPath vPath = virtualPathRepository.findByVirtualPathAndIsDeletedFalse(virtualPath)
            .orElseThrow(() -> new AfsException(HttpStatus.NOT_FOUND, 
                "Virtual path not found: " + virtualPath));

        if (!vPath.isDirectory()) {
            throw new AfsException(HttpStatus.BAD_REQUEST, "Path is not a directory: " + virtualPath);
        }

        markDeleted(vPath, user);
    }

    @Transactional
    public VirtualPath moveDirectory(String sourceVirtualPath, String targetVirtualPath, User user) {
        VirtualPath source = virtualPathRepository.findByVirtualPathAndIsDeletedFalse(sourceVirtualPath)
            .orElseThrow(() -> new AfsException(HttpStatus.NOT_FOUND, 
                "Source path not found: " + sourceVirtualPath));

        if (!source.isDirectory()) {
            throw new AfsException(HttpStatus.BAD_REQUEST, "Source is not a directory: " + sourceVirtualPath);
        }

        String targetParentPath = getParentPath(targetVirtualPath);
        VirtualPath targetParent = virtualPathRepository.findByVirtualPathAndIsDeletedFalse(targetParentPath)
            .orElseThrow(() -> new AfsException(HttpStatus.NOT_FOUND, 
                "Target parent path not found: " + targetParentPath));

        if (source.isAncestorOf(targetParent)) {
            throw new AfsException(HttpStatus.BAD_REQUEST, 
                "Cannot move directory to its own subdirectory");
        }

        source.setVirtualPath(targetVirtualPath);
        source.setParent(targetParent);
        source.setModifiedAt(LocalDateTime.now());
        source.setModifiedBy(user);

        return virtualPathRepository.save(source);
    }

    private void validatePaths(String virtualPath, String physicalPath) {
        if (!virtualPath.startsWith("/")) {
            throw new AfsException(HttpStatus.BAD_REQUEST, "Virtual path must be absolute");
        }

        try {
            Path normalized = sharedFolderValidator.validateAndNormalizePath(physicalPath);
            if (!normalized.toString().equals(physicalPath)) {
                throw new AfsException(HttpStatus.BAD_REQUEST, "Invalid physical path");
            }
        } catch (Exception e) {
            throw new AfsException(HttpStatus.BAD_REQUEST, "Invalid physical path: " + e.getMessage());
        }
    }

    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "/";
    }

    private String getNameFromPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 && lastSlash < path.length() - 1 ? 
            path.substring(lastSlash + 1) : path;
    }

    private void markDeleted(VirtualPath vPath, User user) {
        vPath.setDeleted(true);
        vPath.setDeletedAt(LocalDateTime.now());
        vPath.setDeletedBy(user);
        virtualPathRepository.save(vPath);

        // Recursively mark children as deleted
        List<VirtualPath> children = virtualPathRepository.findByParentIdAndIsDeletedFalse(vPath.getId());
        for (VirtualPath child : children) {
            markDeleted(child, user);
        }
    }
}
