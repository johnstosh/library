/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthoritySerializationTest {

    @Test
    void testAuthoritySerialization() throws Exception {
        // Create an Authority
        Authority authority = new Authority();
        authority.setName("LIBRARIAN");

        // Serialize it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(authority);
        oos.close();

        // Deserialize it
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Authority deserializedAuthority = (Authority) ois.readObject();
        ois.close();

        // Verify
        assertEquals("LIBRARIAN", deserializedAuthority.getName());
    }

    @Test
    void testUserSerialization() throws Exception {
        // Create a User with Authority
        User user = new User();
        user.setUsername("testuser");

        Authority authority = new Authority();
        authority.setName("USER");
        user.setAuthorities(java.util.Set.of(authority));

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
        assertEquals(1, deserializedUser.getAuthorities().size());
    }
}
