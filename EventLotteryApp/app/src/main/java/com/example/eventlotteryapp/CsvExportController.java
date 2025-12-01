package com.example.eventlotteryapp;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.example.eventlotteryapp.models.Entrant;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExportController {

    /**
     * Exports final enrolled entrants to CSV format.
     * Saves the file to the Downloads folder.
     */
    public File exportFinalListToCSV(Context context, String eventName, List<Entrant> entrants) {
        // Get the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        
        // Check if external storage is available and writable
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, "External storage not available", Toast.LENGTH_LONG).show();
            return null;
        }
        
        // Create Downloads directory if it doesn't exist
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        // Create the CSV file in Downloads folder
        String fileName = eventName.replace(" ", "_") + "_final_list.csv";
        File file = new File(downloadsDir, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Entrant Name,Email\n");
            for (Entrant e : entrants) {
                writer.append(e.getName()).append(",").append(e.getEmail()).append("\n");
            }
            writer.flush();
            Toast.makeText(context, "CSV exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
        return file;
    }
}

