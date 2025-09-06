package com.sme.afs.controller;

import com.sme.afs.dto.GroupDTO;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Group;
import com.sme.afs.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.sme.afs.dto.UserDTO;
import com.sme.afs.model.User;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMembershipControllerTest {
    @Mock
    private GroupService groupService;
    
    @InjectMocks
    private GroupMembershipController controller;

    private GroupDTO groupDTO;
    private static final Long GROUP_ID = 1L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        groupDTO = new GroupDTO();
        groupDTO.setId(GROUP_ID);
        groupDTO.setName("Test Group");
    }

    @Test
    void addUserToGroup_NotAdmin_ThrowsException() {
        when(groupService.addUserToGroup(GROUP_ID, USER_ID))
            .thenThrow(new AfsException(HttpStatus.FORBIDDEN, "Admin access required"));

        AfsException exception = assertThrows(AfsException.class,
            () -> controller.addUserToGroup(GROUP_ID, USER_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Admin access required", exception.getMessage());
    }

    @Test
    void removeUserFromGroup_NotAdmin_ThrowsException() {
        when(groupService.removeUserFromGroup(GROUP_ID, USER_ID))
            .thenThrow(new AfsException(HttpStatus.FORBIDDEN, "Admin access required"));

        AfsException exception = assertThrows(AfsException.class,
            () -> controller.removeUserFromGroup(GROUP_ID, USER_ID));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Admin access required", exception.getMessage());
    }

    @Test
    void addUserToGroup_Success() {
        when(groupService.addUserToGroup(GROUP_ID, USER_ID)).thenReturn(groupDTO);
        
        GroupDTO result = controller.addUserToGroup(GROUP_ID, USER_ID);
        
        assertNotNull(result);
        assertEquals(GROUP_ID, result.getId());
        verify(groupService).addUserToGroup(GROUP_ID, USER_ID);
    }

    @Test
    void removeUserFromGroup_Success() {
        when(groupService.removeUserFromGroup(GROUP_ID, USER_ID)).thenReturn(groupDTO);
        
        GroupDTO result = controller.removeUserFromGroup(GROUP_ID, USER_ID);
        
        assertNotNull(result);
        assertEquals(GROUP_ID, result.getId());
        verify(groupService).removeUserFromGroup(GROUP_ID, USER_ID);
    }

    @Test
    void getGroupMembers_Success() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testUser");
        
        Group group = new Group();
        group.setId(GROUP_ID);
        group.setUsers(new HashSet<>(Collections.singletonList(user)));
        
        UserDTO userDTO = UserDTO.fromUser(user);
        List<UserDTO> expectedMembers = Collections.singletonList(userDTO);
        
        when(groupService.getGroupMembers(GROUP_ID)).thenReturn(expectedMembers);
        
        List<UserDTO> result = controller.getGroupMembers(GROUP_ID);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getId());
        verify(groupService).getGroupMembers(GROUP_ID);
    }

    @Test
    void getGroupMembers_GroupNotFound() {
        when(groupService.getGroupMembers(GROUP_ID))
            .thenThrow(new AfsException(HttpStatus.NOT_FOUND, "Group not found"));

        AfsException exception = assertThrows(AfsException.class,
            () -> controller.getGroupMembers(GROUP_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Group not found", exception.getMessage());
    }
}
