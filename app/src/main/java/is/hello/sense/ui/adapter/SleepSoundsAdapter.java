package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.util.Constants;

public class SleepSoundsAdapter extends RecyclerView.Adapter<SleepSoundsAdapter.BaseViewHolder> {
    static final int VIEW_VOLUME = 0;
    static final int VIEW_SOUNDS = 1;
    static final int VIEW_DURATIONS = 2;
    private final LayoutInflater inflater;
    private final SharedPreferences preferences;
    private final InteractionListener interactionListener;

    private final static int SOUNDS_REQUEST_CODE = 1234;
    private final static int DURATION_REQUEST_CODE = 4321;

    private SleepSoundStatus sleepSoundStatus;
    private SleepSounds sleepSounds;
    private SleepDurations sleepDurations;
    private int itemCount = 0;

    private SleepSoundsAdapter(@NonNull Context context,
                               @NonNull SharedPreferences preferences,
                               @NonNull InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
        this.inflater = LayoutInflater.from(context);
        this.preferences = preferences;
    }

    public void bind(@NonNull SleepSoundStatus status, @NonNull SleepSounds sleepSounds, @NonNull SleepDurations sleepDurations) {
        this.sleepSoundStatus = status;
        this.sleepSounds = sleepSounds;
        this.sleepDurations = sleepDurations;
        this.itemCount = 3;
        notifyDataSetChanged();
    }

    private String getSavedSound() {
        return preferences.getString(Constants.SLEEP_SOUNDS_SOUND_NAME, null);
    }

    private String getSavedDuration() {
        return preferences.getString(Constants.SLEEP_SOUNDS_DURATION_NAME, null);
    }


    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_SOUNDS;
        } else if (position == 1) {
            return VIEW_DURATIONS;
        }
        return VIEW_VOLUME;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_SOUNDS) {
            return new SleepSoundsViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
        } else if (viewType == VIEW_DURATIONS) {
            return new SleepDurationsViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
        }
        return new SleepVolumeViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        protected ImageView image;
        protected TextView label;
        protected TextView value;
        protected ImageView chevron;
        protected View holder;

        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
            this.holder = itemView;
            this.image = (ImageView) itemView.findViewById(R.id.item_sleep_sounds_image);
            this.chevron = (ImageView) itemView.findViewById(R.id.item_sleep_sounds_chevron);
            this.label = (TextView) itemView.findViewById(R.id.item_sleep_sounds_label);
            this.value = (TextView) itemView.findViewById(R.id.item_sleep_sounds_value);
        }

        abstract void bind(int position);
    }

    class SleepSoundsViewHolder extends BaseViewHolder {

        SleepSoundsViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        void bind(int position) {

            if (sleepSounds == null) {
                return;
            }

            List<Sound> sounds = sleepSounds.getSounds();

            if (sounds.isEmpty()) {
                value.setText(null);
                return;
            }

            holder.setOnClickListener(v -> {
                interactionListener.onSoundClick(value.getText().toString(), sounds);
            });

            String savedSound = getSavedSound();
            if (sleepSounds.hasSound(savedSound)) {
                value.setText(savedSound);
            } else if (sleepSounds.hasSound(sleepSoundStatus.getSound().getName())) {
                value.setText(sleepSoundStatus.getSound().getName());
            } else {
                value.setText(sounds.get(0).getName());
            }
        }
    }

    class SleepDurationsViewHolder extends BaseViewHolder {

        SleepDurationsViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        void bind(int position) {
            if (sleepDurations == null) {
                return;
            }

            List<Duration> durations = sleepDurations.getDurations();

            if (durations.isEmpty()) {
                value.setText(null);
                return;
            }

            holder.setOnClickListener(v -> {
                interactionListener.onDurationClick(value.getText().toString(), durations);

            });

            String savedDuration = getSavedDuration();
            if (sleepDurations.hasDuration(savedDuration)) {
                value.setText(savedDuration);
            } else if (sleepDurations.hasDuration(sleepSoundStatus.getDuration().getName())) {
                value.setText(sleepSoundStatus.getDuration().getName());
            } else {
                value.setText(durations.get(0).getName());
            }


        }
    }

    class SleepVolumeViewHolder extends BaseViewHolder {

        SleepVolumeViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        void bind(int position) {

        }
    }

    public interface InteractionListener {
        void onSoundClick(@NonNull String currentValue, @NonNull List<?> sounds);

        void onDurationClick(@NonNull String currentDuration, @NonNull List<?> durations);
    }

}
