package fr.android.mhealthy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import fr.android.mhealthy.R;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.model.PatientInfo;
import fr.android.mhealthy.storage.PatientDAO;

public class HistoryRecycler extends RecyclerView.Adapter<HistoryHolder> {
    private List<Object> hist;
    PatientDAO access;
    private String name;
    private boolean is_med;

    public interface OnItemClickListener {
        void onItemClick(Medicine m);
    }

    public HistoryRecycler(PatientDAO access, String name, boolean is_med, Integer patient) {
        this.hist = List.of();
        this.is_med = is_med;
        this.name = name;
        this.access = access;
    }

    public void load_data(Integer patient) {
        this.hist = access.get_all_history(patient, name, is_med);
    }

    @NonNull
    @Override
    public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_element, parent, false);
        return new HistoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {
        Object p = hist.get(position);
        String text;
        long time;
        if (p instanceof PatientInfo) {
            PatientInfo i = (PatientInfo) p;
            text = i.get_action_string(holder.itemView.getContext());
            time = i.get_time();
        } else {
            Instruction i = (Instruction)p;
            text = i.get_action_string(holder.itemView.getContext());
            time = i.get_time();
        }
        holder.text.setText(text);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault());
        holder.time.setText(localDateTime.toString());
    }

    @Override
    public int getItemCount() {
        return hist.size();
    }

    public void insert(RecyclerView recyclerView, Object p) {
    }
}
