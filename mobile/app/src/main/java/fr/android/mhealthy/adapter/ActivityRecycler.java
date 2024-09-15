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
import fr.android.mhealthy.ui.ActivityActionActivity;

public class ActivityRecycler extends RecyclerView.Adapter<ActivityHolder> {
    private List<Activity> acts;
    private OnItemClickListener mListener;
    PatientDAO access;

    public interface OnItemClickListener {
        void onItemClick(Activity m);
    }

    public ActivityRecycler(PatientDAO access, Integer patient, OnItemClickListener listener) {
        this.acts = List.of();
        this.mListener = listener;
        this.access = access;
    }

    public void load_data(Integer patient) {
        this.acts = access.get_all_activities(patient);
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
        String name;
        try {
            name = ActivityActionActivity.get_options(holder.name.getContext())[Integer.parseInt(p.name)];
        } catch (NumberFormatException e) {
            name = p.name;
        }
        holder.name.setText(name);
        holder.goal.setText(p.goal.isEmpty() ? holder.name.getContext().getString(R.string.no_goal) : p.goal);
        holder.time.setText(p.time);
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            });
            mListener.onItemClick(p);
        });
        if (!p.active) {
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = 0;
            params.width = 0;
            holder.itemView.setLayoutParams(params);
            holder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return acts.size();
    }

    public void insert(RecyclerView recyclerView, Activity p) {
        // If element is activated again we just show it
        int pos = find_pos(p.name);
        RecyclerView.ViewHolder view;
        if (pos != -1 && (view = recyclerView.findViewHolderForAdapterPosition(pos)) != null) {
            acts.set(pos, p);
            notifyItemChanged(pos);
            ViewGroup.LayoutParams params = view.itemView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            view.itemView.setLayoutParams(params);
            view.itemView.setVisibility(View.VISIBLE);
            return;
        }
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

    public void edit(Activity.EditActivityNotification p) {
        int count = 0;
        for (Activity act : acts) {
            if (act.name.equals(p.name)) {
                act.goal = p.goal;
                act.time = p.time;
                act.updated_at = p.updated_at;
                notifyItemChanged(count);
                break;
            }
            count += 1;
        }
    }

    public int find_pos(String name) {
        int count = 0;
        for (Activity p : acts) {
            if (p.name.equals(name)) {
                return count;
            }
            count += 1;
        }
        return -1;
    }

    public void remove(RecyclerView recyclerView, Activity.RemoveActivityNotification p) {
        int pos = find_pos(p.name);
        RecyclerView.ViewHolder view;
        if (pos != -1 && (view = recyclerView.findViewHolderForAdapterPosition(pos)) != null) {
            ViewGroup.LayoutParams params = view.itemView.getLayoutParams();
            params.height = 0;
            params.width = 0;
            view.itemView.setLayoutParams(params);
            view.itemView.setVisibility(View.GONE);
        }
    }
}
