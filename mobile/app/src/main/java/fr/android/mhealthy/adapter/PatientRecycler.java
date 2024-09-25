package fr.android.mhealthy.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.storage.CaregiverDAO;
import fr.android.mhealthy.utils.AvatarColorUtils;

public class PatientRecycler extends RecyclerView.Adapter<PatientHolder> {
    private List<Patient> patients;
    private OnItemClickListener mListener;
    private CaregiverDAO access;
    public boolean show_hidden;

    public interface OnItemClickListener {
        void onItemClick(Patient p);
    }

    public PatientRecycler(CaregiverDAO access, OnItemClickListener listener) {
        this.patients = List.of();
        this.mListener = listener;
        this.access = access;
        show_hidden = false;
    }

    public void load_data() {
        this.patients = access.get_all_patients();
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
        holder.id.setText(holder.itemView.getContext().getString(R.string.ID, String.valueOf(p.id)));
        holder.avatar.setAvatarInitials(p.name);
        holder.avatar.setAvatarInitialsBackgroundColor(AvatarColorUtils.generateColorFromString(p.name));
        if (!show_hidden) {
            holder.status.setVisibility(View.GONE);
        } else {
            Context ctx = holder.itemView.getContext();
            if (p.active) {
                holder.status.setText(ctx.getString(R.string.assigned));
                holder.status.setBackground(ContextCompat.getDrawable(ctx, R.drawable.status_background));
                holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active));
            } else {
                holder.status.setText(holder.itemView.getContext().getString(R.string.not_assigned));
                holder.status.setBackground(ContextCompat.getDrawable(ctx, R.drawable.status_background_deleted));
                holder.status.setTextColor(ContextCompat.getColor(ctx, R.color.delete_color));
            }
            holder.status.setVisibility(View.VISIBLE);
        }
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            });
            mListener.onItemClick(p);
        });
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (p.active || show_hidden) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.itemView.setLayoutParams(params);
            holder.itemView.setVisibility(View.VISIBLE);
        } else {
            params.height = 0;
            params.width = 0;
            holder.itemView.setLayoutParams(params);
            holder.itemView.setVisibility(View.GONE);
        }
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
