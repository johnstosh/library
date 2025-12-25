/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class RoleSerializationTest {

    @Test
    void testRoleSerialization() throws Exception {
        // Create a Role
        Role role = new Role();
        role.setName("LIBRARIAN");

        // Serialize it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(role);
        oos.close();

        // Deserialize it
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Role deserializedRole = (Role) ois.readObject();
        ois.close();

        // Verify
        assertEquals("LIBRARIAN", deserializedRole.getName());
    }

    @Test
    void testUserSerialization() throws Exception {
        // Create a User with Role
        User user = new User();
        user.setUsername("testuser");

        Role role = new Role();
        role.setName("USER");
        user.setRoles(java.util.Set.of(role));

        // Serialize it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(user);
        oos.close();

        // Deserialize it
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        User deserializedUser = (User) ois.readObject();
        ois.close();

        // Verify
        assertEquals("testuser", deserializedUser.getUsername());
        assertEquals(1, deserializedUser.getRoles().size());
    }
}
