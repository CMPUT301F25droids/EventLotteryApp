package com.example.eventlotteryapp.models;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the User class.
 * Tests all getters, setters, and constructors.
 * Related to user stories: US 01.02.01, US 01.02.02, US 01.07.01
 */
public class UserTest {
    private User user;

    @Before
    public void setUp() {
        user = new User();
    }

    @Test
    public void testDefaultConstructor() {
        User defaultUser = new User();
        assertNotNull(defaultUser);
        assertNull(defaultUser.getUid());
        assertNull(defaultUser.getName());
        assertNull(defaultUser.getEmail());
        assertNull(defaultUser.getPhone());
    }

    @Test
    public void testParameterizedConstructor() {
        String uid = "user123";
        String name = "John Doe";
        String email = "john@example.com";
        String phone = "123-456-7890";
        
        User newUser = new User(uid, name, email, phone);
        assertEquals(uid, newUser.getUid());
        assertEquals(name, newUser.getName());
        assertEquals(email, newUser.getEmail());
        assertEquals(phone, newUser.getPhone());
    }

    @Test
    public void testUidGetterAndSetter() {
        String uid = "user456";
        user.setUid(uid);
        assertEquals(uid, user.getUid());
        
        user.setUid(null);
        assertNull(user.getUid());
    }

    @Test
    public void testNameGetterAndSetter() {
        String name = "Jane Smith";
        user.setName(name);
        assertEquals(name, user.getName());
        
        user.setName("");
        assertEquals("", user.getName());
    }

    @Test
    public void testEmailGetterAndSetter() {
        String email = "jane@example.com";
        user.setEmail(email);
        assertEquals(email, user.getEmail());
        
        user.setEmail(null);
        assertNull(user.getEmail());
    }

    @Test
    public void testPhoneGetterAndSetter() {
        String phone = "555-1234";
        user.setPhone(phone);
        assertEquals(phone, user.getPhone());
        
        user.setPhone(null);
        assertNull(user.getPhone());
        
        // Test optional phone number (can be empty)
        user.setPhone("");
        assertEquals("", user.getPhone());
    }

    @Test
    public void testSerializable() {
        // Verify User implements Serializable (required for intent extras)
        assertTrue(user instanceof java.io.Serializable);
    }

    @Test
    public void testCompleteUserProfile() {
        String uid = "testUser123";
        String name = "Test User";
        String email = "test@example.com";
        String phone = "123-456-7890";
        
        user.setUid(uid);
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        
        assertEquals(uid, user.getUid());
        assertEquals(name, user.getName());
        assertEquals(email, user.getEmail());
        assertEquals(phone, user.getPhone());
    }

    @Test
    public void testUpdateProfile() {
        // Initial profile
        user.setUid("user123");
        user.setName("Original Name");
        user.setEmail("original@example.com");
        user.setPhone("111-111-1111");
        
        // Update profile (US 01.02.02)
        user.setName("Updated Name");
        user.setEmail("updated@example.com");
        user.setPhone("222-222-2222");
        
        assertEquals("user123", user.getUid()); // UID should not change
        assertEquals("Updated Name", user.getName());
        assertEquals("updated@example.com", user.getEmail());
        assertEquals("222-222-2222", user.getPhone());
    }
}
