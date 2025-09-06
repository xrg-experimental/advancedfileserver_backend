package com.sme.afs.service;

import com.sme.afs.dto.GroupDTO;
import com.sme.afs.model.Group;
import com.sme.afs.model.User;
import com.sme.afs.repository.GroupRepository;
import com.sme.afs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceMembershipTest {
    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private Group group;
    private User user;

    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(1L);
        group.setName("Test Group");
        group.setUsers(new HashSet<>());

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setGroups(new HashSet<>());
    }

    @Test
    void addUserToGroup_Success() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        GroupDTO result = groupService.addUserToGroup(1L, 1L);

        assertNotNull(result);
        verify(groupRepository).save(group);
        assertTrue(group.getUsers().contains(user));
        assertTrue(user.getGroups().contains(group));
    }

    @Test
    void removeUserFromGroup_Success() {
        group.getUsers().add(user);
        user.getGroups().add(group);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        GroupDTO result = groupService.removeUserFromGroup(1L, 1L);

        assertNotNull(result);
        verify(groupRepository).save(group);
        assertFalse(group.getUsers().contains(user));
        assertFalse(user.getGroups().contains(group));
    }

    @Test
    void addUserToGroup_UserAlreadyInGroup() {
        group.getUsers().add(user);
        user.getGroups().add(group);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(Exception.class, () ->
            groupService.addUserToGroup(1L, 1L));
    }

    @Test
    void removeUserFromGroup_UserNotInGroup() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(Exception.class, () ->
            groupService.removeUserFromGroup(1L, 1L));
    }
}
