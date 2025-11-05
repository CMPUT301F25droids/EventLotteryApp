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
     */
    public File exportFinalListToCSV(Context context, String eventName, List<Entrant> entrants) {
        File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();

        File file = new File(exportDir, eventName.replace(" ", "_") + "_final_list.csv");

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Entrant Name,Email\n");
            for (Entrant e : entrants) {
                writer.append(e.getName()).append(",").append(e.getEmail()).append("\n");
            }
            Toast.makeText(context, "CSV Exported: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return file;
    }
}

