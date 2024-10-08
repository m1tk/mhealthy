package fr.android.mhealthy.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.storage.PatientDAO;

public class MedicineRecycler extends RecyclerView.Adapter<MedicineHolder> {
    private List<Medicine> meds;
    private OnItemClickListener mListener;
    PatientDAO access;
    public boolean show_hidden;

    public interface OnItemClickListener {
        void onItemClick(Medicine m);
    }

    public MedicineRecycler(PatientDAO access, Integer patient, OnItemClickListener listener) {
        this.meds = List.of();
        this.mListener = listener;
        this.access = access;
        show_hidden = false;
    }

    public void load_data(Integer patient) {
        this.meds = access.get_all_meds(patient);
    }

    @NonNull
    @Override
    public MedicineHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.medicine_element, parent, false);
        return new MedicineHolder(view);
    }

    @Override
    public void onBindViewHolder(MedicineHolder holder, int position) {
        Medicine p = meds.get(position);
        holder.name.setText(p.name);
        holder.dose.setText(p.dose);
        Context ctx = holder.itemView.getContext();
        if (show_hidden && !p.active) {
            holder.time.setText(ctx.getString(R.string.not_assigned));
            holder.time.setBackground(ContextCompat.getDrawable(ctx, R.drawable.status_background_deleted));
            holder.time.setTextColor(ContextCompat.getColor(ctx, R.color.delete_color));
        } else {
            holder.time.setText(p.time);
            holder.time.setBackground(ContextCompat.getDrawable(ctx, R.drawable.status_background));
            holder.time.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active));
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
        return meds.size();
    }

    public void insert(Medicine p) {
        // If element is activated again we just show it
        int pos = find_pos(p.name);
        if (pos != -1) {
            meds.set(pos, p);
            notifyItemChanged(pos);
            return;
        }
        int position = 0;
        while (position < meds.size() && meds.get(position).created_at > p.created_at) {
            position += 1;
        }
        meds.add(position, p);
        notifyItemInserted(position);
    }

    public void edit(Medicine.EditMedicineNotification edit) {
        int count = 0;
        for (Medicine p : meds) {
            if (p.name.equals(edit.name)) {
                p.dose = edit.dose;
                p.time = edit.time;
                p.updated_at = edit.updated_at;
                notifyItemChanged(count);
                break;
            }
            count += 1;
        }
    }

    public int find_pos(String name) {
        int count = 0;
        for (Medicine p : meds) {
            if (p.name.equals(name)) {
                return count;
            }
            count += 1;
        }
        return -1;
    }

    public void remove(RecyclerView recyclerView, Medicine.RemoveMedicineNotification p) {
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
