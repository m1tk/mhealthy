package fr.android.mhealthy.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import fr.android.mhealthy.R;
import io.getstream.avatarview.AvatarView;

public class PatientHolder extends RecyclerView.ViewHolder {
    public TextView name;
    public TextView id;
    public AvatarView avatar;
    public TextView status;

    public PatientHolder(View item) {
        super(item);
        name = item.findViewById(R.id.patient_name);
        id = item.findViewById(R.id.patient_id);
        avatar = item.findViewById(R.id.patient_avatar);
        status = item.findViewById(R.id.patient_status);
    }
}
