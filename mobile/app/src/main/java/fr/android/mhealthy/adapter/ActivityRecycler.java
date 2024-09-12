package fr.android.mhealthy.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Activity;
import fr.android.mhealthy.storage.PatientDAO;

public class ActivityRecycler extends RecyclerView.Adapter<ActivityHolder> {
    private final List<Activity> acts;
    private OnItemClickListener mListener;
    private final Context ctx;

    public interface OnItemClickListener {
        void onItemClick(Activity m);
    }

    public ActivityRecycler(Context ctx, PatientDAO access, Integer patient, OnItemClickListener listener) {
        this.ctx = ctx;
        this.acts = access.get_all_activities(patient);
        Log.d("Instruction", String.valueOf(this.acts.size()));
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ActivityHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_element, parent, false);
        return new ActivityHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityHolder holder, int position) {
        Activity p = acts.get(position);
        try {
            p.name = ctx.getResources().getStringArray(R.array.activities_options)[Integer.parseInt(p.name)];
        } catch (NumberFormatException e) {}
        holder.name.setText(p.name);
        holder.goal.setText(p.goal);
        holder.time.setText(p.time);
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            });
            mListener.onItemClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return acts.size();
    }

    public void insert(Activity p) {
        // Skipping if patient already in list
        if (acts.stream().anyMatch(e -> e.name.equals(p.name))) {
            return;
        }
        int position = 0;
        while (position < acts.size() && acts.get(position).created_at > p.created_at) {
            position += 1;
        }
        acts.add(position, p);
        notifyItemInserted(position);
    }
}
