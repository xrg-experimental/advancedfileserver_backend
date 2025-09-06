package com.sme.afs.controller;

import com.sme.afs.dto.GroupDTO;
import com.sme.afs.dto.UserDTO;
import com.sme.afs.security.annotation.IsAdmin;
import com.sme.afs.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupMembershipController {
    private final GroupService groupService;

    @GetMapping("/{groupId}/members")
    @IsAdmin
    public List<UserDTO> getGroupMembers(@PathVariable Long groupId) {
        return groupService.getGroupMembers(groupId);
    }

    @PutMapping("/{groupId}/members/{userId}")
    @IsAdmin
    public GroupDTO addUserToGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        return groupService.addUserToGroup(groupId, userId);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @IsAdmin
    public GroupDTO removeUserFromGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        return groupService.removeUserFromGroup(groupId, userId);
    }
}
