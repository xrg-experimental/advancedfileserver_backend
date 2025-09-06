package com.sme.afs.service;

import com.sme.afs.dto.CreateGroupRequest;
import com.sme.afs.dto.GroupDTO;
import com.sme.afs.dto.UpdateGroupPermissionsRequest;
import com.sme.afs.dto.UpdateGroupRequest;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Group;
import com.sme.afs.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private GroupService groupService;

    private Group testGroup;

    @BeforeEach
    void setUp() {
        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.setName("test-group");
        testGroup.setDescription("Test Group");
        testGroup.setBasePath("/test/path");
    }

    @Test
    void createGroup_Success() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("test-group");
        request.setDescription("Test Group");
        request.setBasePath("/test/path");

        when(groupRepository.findByName("test-group")).thenReturn(Optional.empty());
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        GroupDTO result = groupService.createGroup(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-group");
        assertThat(result.getBasePath()).isEqualTo("/test/path");
    }

    @Test
    void updateGroupBasePath_Success() {
        UpdateGroupRequest request = new UpdateGroupRequest();
        request.setBasePath("/new/path");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        GroupDTO result = groupService.updateGroupBasePath(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getBasePath()).isEqualTo("/new/path");
    }

    @Test
    void updateGroupBasePath_GroupNotFound() {
        UpdateGroupRequest request = new UpdateGroupRequest();
        request.setBasePath("/new/path");

        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        AfsException exception = assertThrows(AfsException.class,
                () -> groupService.updateGroupBasePath(1L, request));

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Group not found");
    }

    @Test
    void updateGroupPermissions_Success() {
        // Arrange
        Group group = new Group();
        group.setId(1L);
        group.setName("test-group");
        
        UpdateGroupPermissionsRequest request = new UpdateGroupPermissionsRequest();
        request.setCanRead(true);
        request.setCanWrite(true);
        request.setCanDelete(false);
        request.setCanShare(true);
        request.setCanUpload(true);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        // Act
        GroupDTO result = groupService.updateGroupPermissions(1L, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isCanRead()).isTrue();
        assertThat(result.isCanWrite()).isTrue();
        assertThat(result.isCanDelete()).isFalse();
        assertThat(result.isCanShare()).isTrue();
        assertThat(result.isCanUpload()).isTrue();
    }

    @Test
    void updateGroupPermissions_GroupNotFound() {
        // Arrange
        UpdateGroupPermissionsRequest request = new UpdateGroupPermissionsRequest();
        request.setCanRead(true);
        request.setCanWrite(true);
        request.setCanDelete(false);
        request.setCanShare(true);
        request.setCanUpload(true);

        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        AfsException exception = assertThrows(AfsException.class,
                () -> groupService.updateGroupPermissions(1L, request));

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Group not found");
    }

    @Test
    void updateGroupPermissions_CreateNewPermissions() {
        // Arrange
        Group group = new Group();
        group.setId(1L);
        group.setName("test-group");
        group.setPermissions(null); // Explicitly set null permissions
        
        UpdateGroupPermissionsRequest request = new UpdateGroupPermissionsRequest();
        request.setCanRead(true);
        request.setCanWrite(true);
        request.setCanDelete(false);
        request.setCanShare(true);
        request.setCanUpload(true);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group savedGroup = invocation.getArgument(0);
            assertThat(savedGroup.getPermissions()).isNotNull();
            return savedGroup;
        });

        // Act
        GroupDTO result = groupService.updateGroupPermissions(1L, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isCanRead()).isTrue();
        assertThat(result.isCanWrite()).isTrue();
        assertThat(result.isCanDelete()).isFalse();
        assertThat(result.isCanShare()).isTrue();
        assertThat(result.isCanUpload()).isTrue();
    }
}
