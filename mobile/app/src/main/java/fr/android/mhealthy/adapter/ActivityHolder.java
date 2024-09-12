package fr.android.mhealthy.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import fr.android.mhealthy.R;

public class ActivityHolder extends RecyclerView.ViewHolder {
    public TextView name;
    public TextView time;
    public TextView goal;

    public ActivityHolder(View item) {
        super(item);
        name = item.findViewById(R.id.activity_name);
        time = item.findViewById(R.id.activity_time);
        goal = item.findViewById(R.id.activity_goal);
    }
}
