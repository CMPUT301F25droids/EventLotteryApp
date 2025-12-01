package com.example.eventlotteryapp.Admin;

import static org.junit.Assert.*;

import com.example.eventlotteryapp.Admin.AdminImageAdapter.ImageItem;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AdminImageAdapterTest {

    private AdminImageAdapter.OnImageClick dummyListener;

    /**
     * A test-safe subclass that prevents the real RecyclerView
     * notifyDataSetChanged() from running.
     */
    private static class TestAdapter extends AdminImageAdapter {

        TestAdapter(List<ImageItem> images, OnImageClick listener) {
            super(images, listener);
        }

        @Override
        protected void safeNotifyDataSetChanged() {
            // Disable RecyclerView behavior for unit tests
        }
    }

    @Before
    public void setup() {
        dummyListener = (eventId, base64) -> {
            // no-op for tests
        };
    }

    @Test
    public void testInitialCount() {
        List<ImageItem> list = new ArrayList<>();
        list.add(new ImageItem("1", "abc123"));

        TestAdapter adapter = new TestAdapter(list, dummyListener);

        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void testUpdateImagesReplacesList() {
        TestAdapter adapter = new TestAdapter(new ArrayList<>(), dummyListener);

        List<ImageItem> newList = new ArrayList<>();
        newList.add(new ImageItem("10", "imageA"));
        newList.add(new ImageItem("11", "imageB"));

        adapter.updateImages(newList);

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void testUpdateImagesHandlesEmpty() {
        TestAdapter adapter = new TestAdapter(new ArrayList<>(), dummyListener);

        adapter.updateImages(new ArrayList<>());

        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void testUpdateImagesClearsOldList() {
        List<ImageItem> initial = new ArrayList<>();
        initial.add(new ImageItem("id1", "data1"));

        TestAdapter adapter = new TestAdapter(initial, dummyListener);

        adapter.updateImages(new ArrayList<>());

        assertEquals(0, adapter.getItemCount());
    }
}
