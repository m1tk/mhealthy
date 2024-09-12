package fr.android.mhealthy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.storage.PatientDAO;

public class MedicineRecycler extends RecyclerView.Adapter<MedicineHolder> {
    private final List<Medicine> meds;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(Medicine m);
    }

    public MedicineRecycler(PatientDAO access, Integer patient, OnItemClickListener listener) {
        this.meds = access.get_all_meds(patient);
        this.mListener = listener;
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
        return meds.size();
    }

    public void insert(Medicine p) {
        // Skipping if patient already in list
        if (meds.stream().anyMatch(e -> e.name.equals(p.name))) {
            return;
        }
        int position = 0;
        while (position < meds.size() && meds.get(position).created_at > p.created_at) {
            position += 1;
        }
        meds.add(position, p);
        notifyItemInserted(position);
    }
}
