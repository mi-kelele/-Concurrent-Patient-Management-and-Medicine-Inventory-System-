package Problem2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 
  Represents a registered patient in the hospital.
 
  Concurrency Safety (Task g):
  Multiple healthcare workers (doctors updating diagnoses, nurses recording vitals)
  can access the same patient at the exact same time.
  To prevent corruption, methods that read or write patient data are marked
  'synchronized', guaranteeing that only ONE worker can touch this patient at a time.
 */
public class Patient {

    // Unique ID for the patient (e.g. 1 to 100)
    private final int id; // constant. 
    private final String name;

    // Vital signs recorded by nurses
    private String bloodPressure;
    private double temperature;   // in Celsius (e.g. 37.0)
    private int heartRate;        // in beats per minute (e.g. 75)

    // Medical history & lab results added by doctors and lab technicians
    private final List<String> clinicalNotes = Collections.synchronizedList(new ArrayList<>());

    public Patient(int id, String name) {
        this.id = id;
        this.name = name;
        this.bloodPressure = "120/80";
        this.temperature = 36.8;
        this.heartRate = 72;
    }

    public int getId() {
        return id; // getter. 
    }

    public String getName() {
        return name;
    }

    
    // NURSE OPERATION: Update Vital Signs
    // 'synchronized' ensures two nurses don't write conflicting vitals at once
    
    public synchronized void updateVitals(String nurseName, String bp, double temp, int hr) {
        this.bloodPressure = bp;
        this.temperature = temp;
        this.heartRate = hr;
        clinicalNotes.add("[VITALS UPDATED by " + nurseName + "] BP: " + bp
                + ", Temp: " + temp + "°C, HR: " + hr + " bpm");
    }

   
    // DOCTOR / LAB TECH OPERATION: Add Diagnostic Notes or Lab Results
    
    public synchronized void addClinicalNote(String author, String note) { // setter. 
        clinicalNotes.add("[" + author + "] " + note);
    }

   
    // READ RECORD: Synchronized to ensure no one reads half-written data
   
    public synchronized String getRecordSummary() { // getter
        return "Patient #" + id + " (" + name + ") | Vitals: BP=" + bloodPressure
                + ", Temp=" + temperature + "°C, HR=" + heartRate + " bpm | Notes count: "
                + clinicalNotes.size();
    }

    public synchronized List<String> getClinicalNotes() { // getter. 
        return new ArrayList<>(clinicalNotes);
    }
}

