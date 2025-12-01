package com.example.eventlotteryapp.models;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Entrant class.
 * Tests all getters, setters, and constructors.
 * Related to user stories: US 01.01.01, US 01.05.02, US 01.05.03
 */
public class EntrantTest {
    private Entrant entrant;

    @Before
    public void setUp() {
        entrant = new Entrant();
    }

    @Test
    public void testDefaultConstructor() {
        Entrant defaultEntrant = new Entrant();
        assertNotNull(defaultEntrant);
        assertNull(defaultEntrant.getId());
        assertNull(defaultEntrant.getName());
        assertNull(defaultEntrant.getEmail());
    }

    @Test
    public void testParameterizedConstructor() {
        String id = "entrant123";
        String name = "Alice Johnson";
        String email = "alice@example.com";
        
        Entrant newEntrant = new Entrant(id, name, email);
        assertEquals(id, newEntrant.getId());
        assertEquals(name, newEntrant.getName());
        assertEquals(email, newEntrant.getEmail());
    }

    @Test
    public void testIdGetterAndSetter() {
        String id = "entrant456";
        entrant.setId(id);
        assertEquals(id, entrant.getId());
        
        entrant.setId(null);
        assertNull(entrant.getId());
    }

    @Test
    public void testNameGetterAndSetter() {
        String name = "Bob Williams";
        entrant.setName(name);
        assertEquals(name, entrant.getName());
        
        entrant.setName("");
        assertEquals("", entrant.getName());
    }

    @Test
    public void testEmailGetterAndSetter() {
        String email = "bob@example.com";
        entrant.setEmail(email);
        assertEquals(email, entrant.getEmail());
        
        entrant.setEmail(null);
        assertNull(entrant.getEmail());
    }

    @Test
    public void testCompleteEntrant() {
        String id = "entrant789";
        String name = "Charlie Brown";
        String email = "charlie@example.com";
        
        entrant.setId(id);
        entrant.setName(name);
        entrant.setEmail(email);
        
        assertEquals(id, entrant.getId());
        assertEquals(name, entrant.getName());
        assertEquals(email, entrant.getEmail());
    }

    @Test
    public void testEntrantEquality() {
        Entrant entrant1 = new Entrant("id1", "Name", "email@example.com");
        Entrant entrant2 = new Entrant("id1", "Name", "email@example.com");
        Entrant entrant3 = new Entrant("id2", "Name", "email@example.com");
        
        // Same ID
        assertEquals(entrant1.getId(), entrant2.getId());
        // Different IDs
        assertNotEquals(entrant1.getId(), entrant3.getId());
    }
}
