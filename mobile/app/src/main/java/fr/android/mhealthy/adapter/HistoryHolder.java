package fr.android.mhealthy.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import fr.android.mhealthy.R;

public class HistoryHolder extends RecyclerView.ViewHolder {
    public TextView text;
    public TextView time;

    public HistoryHolder(View item) {
        super(item);
        text = item.findViewById(R.id.history_text);
        time = item.findViewById(R.id.history_time);
    }
}