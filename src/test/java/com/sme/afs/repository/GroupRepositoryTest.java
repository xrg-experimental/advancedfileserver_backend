package com.sme.afs.repository;

import com.sme.afs.model.Group;
import com.sme.afs.model.User;
import com.sme.afs.util.TestDataUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class GroupRepositoryTest {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveGroup() {
        // Given
        Group group = TestDataUtil.createTestGroup("testgroup");

        // When
        Group savedGroup = groupRepository.save(group);
        Group retrievedGroup = groupRepository.findById(savedGroup.getId()).orElse(null);

        // Then
        assertThat(retrievedGroup).isNotNull();
        assertThat(retrievedGroup.getName()).isEqualTo("testgroup");
        assertThat(retrievedGroup.getBasePath()).isEqualTo("/test/path/testgroup");
    }

    @Test
    void shouldFindGroupByName() {
        // Given
        Group group = TestDataUtil.createTestGroup("findme");
        groupRepository.save(group);

        // When
        Group found = groupRepository.findByName("findme").orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("findme");
    }

    @Test
    void shouldManageUserGroupRelationship() {
        // Given
        User user = TestDataUtil.createTestUser("groupuser");
        Group group = TestDataUtil.createTestGroup("usergroup");
        
        user = userRepository.save(user);
        group = groupRepository.save(group);

        // When
        user.getGroups().add(group);
        group.getUsers().add(user);
        userRepository.save(user);
        groupRepository.save(group);

        // Then
        Group retrievedGroup = groupRepository.findById(group.getId()).orElseThrow();
        assertThat(retrievedGroup.getUsers()).hasSize(1);
        assertThat(retrievedGroup.getUsers().iterator().next().getUsername())
            .isEqualTo("groupuser");
    }
}
