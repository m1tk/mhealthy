package fr.android.mhealthy.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.storage.CaregiverDAO;

public class PatientRecycler extends RecyclerView.Adapter<PatientHolder> {
    private final CaregiverDAO access;
    private final List<Patient> patients;

    public PatientRecycler(CaregiverDAO access) {
        this.access = access;
        this.patients = access.get_all_patients();
        Log.d("Ins", String.valueOf(patients.size()));
    }

    @NonNull
    @Override
    public PatientHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.patient_element, parent, false);
        return new PatientHolder(view);
    }

    @Override
    public void onBindViewHolder(PatientHolder holder, int position) {
        Patient p = patients.get(position);
        holder.name.setText(p.name);
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public void insert(Patient p) {
        // Skipping if patient already in list
        if (patients.stream().anyMatch(e -> e.id == p.id)) {
            return;
        }
        patients.add(0, p);
        notifyItemInserted(0);
    }
}
