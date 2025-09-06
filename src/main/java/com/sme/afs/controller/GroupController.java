package com.sme.afs.controller;

import com.sme.afs.dto.CreateGroupRequest;
import com.sme.afs.dto.GroupDTO;
import com.sme.afs.dto.UpdateGroupPermissionsRequest;
import com.sme.afs.dto.UpdateGroupRequest;
import com.sme.afs.security.annotation.IsAdmin;
import com.sme.afs.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @PostMapping
    @IsAdmin
    public ResponseEntity<GroupDTO> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        GroupDTO createdGroup = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    @PutMapping("/{id}/base-path")
    @IsAdmin
    public ResponseEntity<GroupDTO> updateGroupBasePath(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupRequest request) {
        GroupDTO updatedGroup = groupService.updateGroupBasePath(id, request);
        return ResponseEntity.ok(updatedGroup);
    }

    @PutMapping("/{id}/permissions")
    @IsAdmin
    public ResponseEntity<GroupDTO> updateGroupPermissions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupPermissionsRequest request) {
        GroupDTO updatedGroup = groupService.updateGroupPermissions(id, request);
        return ResponseEntity.ok(updatedGroup);
    }
}
