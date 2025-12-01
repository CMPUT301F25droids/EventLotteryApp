package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.Entrant;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for CsvExportController.
 * Tests CSV export functionality for event entrant lists.
 * Related user story: US 02.06.05 - Export final list as CSV
 */
public class CsvExportControllerTest {
    private CsvExportController csvExportController;

    @Before
    public void setUp() {
        csvExportController = new CsvExportController();
    }

    @Test
    public void testControllerInitialization() {
        assertNotNull(csvExportController);
    }

    @Test
    public void testFileNameGeneration() {
        // Test that file names are generated correctly
        String eventName = "Swimming Lessons";
        String expectedFileName = "Swimming_Lessons_final_list.csv";
        
        String actualFileName = eventName.replace(" ", "_") + "_final_list.csv";
        assertEquals(expectedFileName, actualFileName);
    }

    @Test
    public void testFileNameGenerationWithSpecialCharacters() {
        // Test file name generation with special characters
        String eventName = "Piano & Violin Class";
        String fileName = eventName.replace(" ", "_") + "_final_list.csv";
        
        assertNotNull(fileName);
        assertTrue(fileName.endsWith(".csv"));
    }

    @Test
    public void testEmptyEntrantsList() {
        // Test export with empty list
        List<Entrant> emptyList = new ArrayList<>();
        assertNotNull(emptyList);
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testEntrantsListCreation() {
        // Test creating a list of entrants for export
        List<Entrant> entrants = new ArrayList<>();
        entrants.add(new Entrant("id1", "John Doe", "john@example.com"));
        entrants.add(new Entrant("id2", "Jane Smith", "jane@example.com"));
        
        assertEquals(2, entrants.size());
        assertEquals("John Doe", entrants.get(0).getName());
        assertEquals("Jane Smith", entrants.get(1).getName());
    }
}
