package com.sme.afs.service;

import com.sme.afs.dto.*;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Group;
import com.sme.afs.model.GroupPermission;
import com.sme.afs.model.User;
import com.sme.afs.repository.GroupRepository;
import com.sme.afs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupDTO createGroup(CreateGroupRequest request) {
        // Check if group name already exists
        if (groupRepository.findByName(request.getName()).isPresent()) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "Group name already exists");
        }

        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setBasePath(request.getBasePath());

        Group savedGroup = groupRepository.save(group);
        return GroupDTO.fromGroup(savedGroup);
    }

    @Transactional
    public GroupDTO updateGroupPermissions(Long groupId, UpdateGroupPermissionsRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "Group not found"));

        if (group.getPermissions() == null) {
            GroupPermission permissions = new GroupPermission();
            permissions.setGroup(group);
            group.setPermissions(permissions);
        }

        GroupPermission permissions = group.getPermissions();
        permissions.setCanRead(request.getCanRead());
        permissions.setCanWrite(request.getCanWrite());
        permissions.setCanDelete(request.getCanDelete());
        permissions.setCanShare(request.getCanShare());
        permissions.setCanUpload(request.getCanUpload());

        Group savedGroup = groupRepository.save(group);
        return GroupDTO.fromGroup(savedGroup);
    }

    @Transactional
    public GroupDTO updateGroupBasePath(Long groupId, UpdateGroupRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "Group not found"));

        group.setBasePath(request.getBasePath());
        Group savedGroup = groupRepository.save(group);
        return GroupDTO.fromGroup(savedGroup);
    }

    @Transactional
    public GroupDTO addUserToGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "Group not found"));
            
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "User not found"));

        if (group.getUsers().contains(user)) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "User is already a member of this group");
        }

        group.getUsers().add(user);
        user.getGroups().add(group);
        
        Group savedGroup = groupRepository.save(group);
        return GroupDTO.fromGroup(savedGroup);
    }

    @Transactional
    public GroupDTO removeUserFromGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "Group not found"));
            
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "User not found"));

        if (!group.getUsers().contains(user)) {
            throw new AfsException(ErrorCode.NOT_FOUND, "User is not a member of this group");
        }

        group.getUsers().remove(user);
        user.getGroups().remove(group);
        
        Group savedGroup = groupRepository.save(group);
        return GroupDTO.fromGroup(savedGroup);
    }

    public List<UserDTO> getGroupMembers(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "Group not found"));
            
        return group.getUsers().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
    }
}
