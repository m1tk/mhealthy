package fr.android.mhealthy.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import fr.android.mhealthy.R;

public class PatientHolder extends RecyclerView.ViewHolder {
    public TextView name;

    public PatientHolder(View item) {
        super(item);
        name = item.findViewById(R.id.patient_name);
    }
}
