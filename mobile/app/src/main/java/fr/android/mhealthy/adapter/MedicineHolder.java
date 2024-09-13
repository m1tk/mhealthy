package fr.android.mhealthy.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import fr.android.mhealthy.R;

public class MedicineHolder extends RecyclerView.ViewHolder {
    public TextView name;
    public TextView time;
    public TextView dose;

    public MedicineHolder(View item) {
        super(item);
        name = item.findViewById(R.id.medicine_name);
        time = item.findViewById(R.id.medicine_time);
        dose = item.findViewById(R.id.medicine_dose);
    }
}
