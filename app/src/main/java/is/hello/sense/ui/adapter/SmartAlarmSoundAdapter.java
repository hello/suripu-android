package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;

public class SmartAlarmSoundAdapter extends ArrayRecyclerAdapter<Alarm.Sound, SmartAlarmSoundAdapter.ViewHolder> {
    public static final int NONE = -1;

    private final LayoutInflater inflater;
    private final Resources resources;

    private long selectedSoundId = NONE;
    private long playingSoundId = NONE;
    private boolean playingSoundLoading = false;

    public SmartAlarmSoundAdapter(@NonNull Context context) {
        super(new ArrayList<>());

        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }

    public void setSelectedSoundId(long selectedSoundId) {
        this.selectedSoundId = selectedSoundId;
        notifyDataSetChanged();
    }

    public void setPlayingSoundId(long id, boolean loading) {
        this.playingSoundId = id;
        this.playingSoundLoading = loading;

        notifyDataSetChanged();
    }

    public long getPlayingSoundId() {
        return playingSoundId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.item_static_choice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Alarm.Sound sound = getItem(position);
        holder.name.setText(sound.name);

        if (selectedSoundId == sound.id) {
            holder.checked.setImageResource(R.drawable.radio_on);
        } else {
            holder.checked.setImageResource(R.drawable.radio_off);
        }

        if (playingSoundId == sound.id) {
            holder.name.setTextColor(resources.getColor(R.color.light_accent));
            if (playingSoundLoading) {
                holder.checked.setVisibility(View.INVISIBLE);
                holder.busy.setVisibility(View.VISIBLE);
            } else {
                holder.checked.setVisibility(View.VISIBLE);
                holder.busy.setVisibility(View.GONE);
            }
        } else {
            holder.name.setTextColor(resources.getColor(R.color.text_dark));
            holder.checked.setVisibility(View.VISIBLE);
            holder.busy.setVisibility(View.GONE);
        }
    }

    class ViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        final TextView name;
        final ImageView checked;
        final ProgressBar busy;

        ViewHolder(@NonNull View view) {
            super(view);

            this.name = (TextView) view.findViewById(R.id.item_static_choice_name);
            this.checked = (ImageView) view.findViewById(R.id.item_static_choice_checked);
            this.busy = (ProgressBar) view.findViewById(R.id.item_static_choice_progress);

            view.setOnClickListener(this);
        }
    }
}
