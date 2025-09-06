package com.sme.afs.dto;

import com.sme.afs.model.Group;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupDTO {
    private Long id;
    private String name;
    private String description;
    private String basePath;
    private LocalDateTime createdAt;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canShare;
    private boolean canUpload;

    public static GroupDTO fromGroup(Group group) {
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setBasePath(group.getBasePath());
        dto.setCreatedAt(group.getCreatedAt());
        
        if (group.getPermissions() != null) {
            dto.setCanRead(group.getPermissions().isCanRead());
            dto.setCanWrite(group.getPermissions().isCanWrite());
            dto.setCanDelete(group.getPermissions().isCanDelete());
            dto.setCanShare(group.getPermissions().isCanShare());
            dto.setCanUpload(group.getPermissions().isCanUpload());
        }
        return dto;
    }
}
