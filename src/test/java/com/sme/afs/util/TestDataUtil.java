package com.sme.afs.util;

import com.sme.afs.model.Group;
import com.sme.afs.model.User;
import com.sme.afs.model.UserType;

public class TestDataUtil {
    
    public static User createTestUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setEmail(username + "@test.com");
        user.setEnabled(true);
        user.setUserType(UserType.INTERNAL);
        return user;
    }

    public static Group createTestGroup(String name) {
        Group group = new Group();
        group.setName(name);
        group.setDescription("Test group " + name);
        group.setBasePath("/test/path/" + name);
        return group;
    }
}
