package com.sme.afs.dto;

import com.sme.afs.model.Group;
import com.sme.afs.model.Role;
import com.sme.afs.model.User;
import com.sme.afs.model.UserType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private boolean enabled;
    private UserType userType;
    private Set<String> roles;
    private Set<String> groups;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public static UserDTO fromUser(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        dto.setEnabled(user.isEnabled());
        dto.setUserType(user.getUserType());
        dto.setRoles(user.getRoles().stream()
            .map(Role::name)
            .collect(Collectors.toSet()));
        dto.setGroups(user.getGroups().stream()
            .map(Group::getName)
            .collect(Collectors.toSet()));
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }
}
