package com.example.eventlotteryapp.Admin;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

public class AdminEventAdapterTest {

    @Test
    public void testItemCount() {
        List<AdminEvent> events = new ArrayList<>();
        events.add(new AdminEvent("1", "Title A", "USA"));
        events.add(new AdminEvent("2", "Title B", "Canada"));

        AdminEventAdapter adapter = new AdminEventAdapter(events);

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void testEmptyList() {
        List<AdminEvent> events = new ArrayList<>();

        AdminEventAdapter adapter = new AdminEventAdapter(events);

        assertEquals(0, adapter.getItemCount());
    }
}