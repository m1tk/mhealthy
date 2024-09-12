package fr.android.mhealthy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.storage.CaregiverDAO;

public class PatientRecycler extends RecyclerView.Adapter<PatientHolder> {
    private final CaregiverDAO access;
    private final List<Patient> patients;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(Patient p);
    }

    public PatientRecycler(CaregiverDAO access, OnItemClickListener listener) {
        this.access = access;
        this.patients = access.get_all_patients();
        this.mListener = listener;
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
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            });
            mListener.onItemClick(p);
        });
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
        int position = 0;
        while (position < patients.size() && patients.get(position).id > p.id) {
            position += 1;
        }
        patients.add(position, p);
        notifyItemInserted(position);
    }
}
