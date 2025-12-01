package com.example.eventlotteryapp.Admin;

import static org.junit.Assert.*;

import com.example.eventlotteryapp.Admin.AdminProfileAdapter.UserProfile;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AdminProfileAdapterTest {

    private AdminProfileAdapter.OnDeleteClickListener dummyDelete;
    private AdminProfileAdapter.OnBanClickListener dummyBan;

    /** A test-safe adapter that disables notifyDataSetChanged(). */
    private static class TestAdapter extends AdminProfileAdapter {
        TestAdapter(List<UserProfile> profiles,
                    OnDeleteClickListener deleteListener,
                    OnBanClickListener banListener) {
            super(profiles, deleteListener, banListener);
        }

        @Override
        protected void safeNotifyDataSetChanged() {
            // Overrides adapter notify so unit tests don't crash
        }
    }

    @Before
    public void setup() {
        dummyDelete = profile -> {};
        dummyBan = profile -> {};
    }

    @Test
    public void testInitialCount() {
        List<UserProfile> list = new ArrayList<>();
        list.add(new UserProfile("1", "Alice", "a@mail.com", "entrant", "123", false));

        TestAdapter adapter = new TestAdapter(list, dummyDelete, dummyBan);

        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void testUpdateProfilesReplacesList() {
        TestAdapter adapter = new TestAdapter(new ArrayList<>(), dummyDelete, dummyBan);

        List<UserProfile> newList = new ArrayList<>();
        newList.add(new UserProfile("5", "Bob", "b@mail.com", "entrant", "", false));
        newList.add(new UserProfile("6", "Sara", "s@mail.com", "organizer", "", true));

        adapter.updateProfiles(newList);

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void testUpdateProfilesHandlesNullInput() {
        TestAdapter adapter = new TestAdapter(new ArrayList<>(), dummyDelete, dummyBan);

        adapter.updateProfiles(null);

        assertEquals(0, adapter.getItemCount());
    }
}

