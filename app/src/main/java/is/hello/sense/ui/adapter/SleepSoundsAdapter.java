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

import java.util.Arrays;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.util.Constants;

public class SleepSoundsAdapter extends RecyclerView.Adapter<SleepSoundsAdapter.BaseViewHolder> {
    static final int VIEW_TITLE = 0;
    static final int VIEW_VOLUME = 1;
    static final int VIEW_SOUNDS = 2;
    static final int VIEW_DURATIONS = 3;
    private final static SleepSoundStatus.Volume[] volumes = new SleepSoundStatus.Volume[]{
            SleepSoundStatus.Volume.Low,
            SleepSoundStatus.Volume.Medium,
            SleepSoundStatus.Volume.High};
    private final LayoutInflater inflater;
    private final SharedPreferences preferences;
    private final InteractionListener interactionListener;

    private SleepSoundStatus sleepSoundStatus;
    private SleepSounds sleepSounds;
    private SleepDurations sleepDurations;
    private int itemCount = 0;

    public SleepSoundsAdapter(final @NonNull Context context,
                              final @NonNull SharedPreferences preferences,
                              final @NonNull InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
        this.inflater = LayoutInflater.from(context);
        this.preferences = preferences;
    }

    public void bind(final @NonNull SleepSoundStatus status,
                     final @NonNull SleepSounds sleepSounds,
                     final @NonNull SleepDurations sleepDurations) {
        this.sleepSoundStatus = status;
        this.sleepSounds = sleepSounds;
        this.sleepDurations = sleepDurations;
        this.itemCount = 4;
        notifyDataSetChanged();
    }

    private String getSavedSound() {
        return preferences.getString(Constants.SLEEP_SOUNDS_SOUND_NAME, null);
    }

    private String getSavedDuration() {
        return preferences.getString(Constants.SLEEP_SOUNDS_DURATION_NAME, null);
    }

    @Override
    public int getItemViewType(final int position) {
        if (position == 0) {
            return VIEW_TITLE;
        } else if (position == 1) {
            return VIEW_SOUNDS;
        } else if (position == 2) {
            return VIEW_DURATIONS;
        }
        return VIEW_VOLUME;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == VIEW_TITLE) {
            return new TitleViewHolder(inflater.inflate(R.layout.item_centered_title, parent, false));
        } else if (viewType == VIEW_SOUNDS) {
            return new SleepSoundsViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
        } else if (viewType == VIEW_DURATIONS) {
            return new SleepDurationsViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
        }
        return new SleepVolumeViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);

    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    abstract static class BaseViewHolder extends RecyclerView.ViewHolder {


        BaseViewHolder(final @NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(final int position);
    }

    public class TitleViewHolder extends BaseViewHolder {

        TitleViewHolder(final @NonNull View itemView) {
            super(itemView);
            ((TextView) itemView.findViewById(R.id.item_centered_title_text)).setText(R.string.sleep_sounds_title);
        }

        @Override
        void bind(final int position) {

        }
    }

    public abstract class SleepViewHolder extends BaseViewHolder {
        protected final ImageView image;
        protected final TextView label;
        protected final TextView value;
        protected final ImageView chevron;
        protected final View holder;

        SleepViewHolder(final @NonNull View itemView) {
            super(itemView);
            this.holder = itemView;
            this.image = (ImageView) itemView.findViewById(R.id.item_sleep_sounds_image);
            this.chevron = (ImageView) itemView.findViewById(R.id.item_sleep_sounds_chevron);
            this.label = (TextView) itemView.findViewById(R.id.item_sleep_sounds_label);
            this.value = (TextView) itemView.findViewById(R.id.item_sleep_sounds_value);
        }
    }

    class SleepSoundsViewHolder extends SleepViewHolder {

        SleepSoundsViewHolder(final @NonNull View itemView) {
            super(itemView);
            image.setImageResource(R.drawable.sounds_sound_icon);
            label.setText(R.string.sleep_sounds_sound_label);
        }

        @Override
        void bind(final int position) {

            if (sleepSounds == null) {
                return;
            }

            final List<Sound> sounds = sleepSounds.getSounds();

            if (sounds.isEmpty()) {
                value.setText(null);
                return;
            }

            holder.setOnClickListener(v -> {
                interactionListener.onSoundClick(value.getText().toString(), sounds);
            });

            final Sound currentSounds = sleepSoundStatus.getSound();
            if (currentSounds != null && sleepSounds.hasSound(currentSounds.getName())) {
                value.setText(currentSounds.getName());
            } else {
                final String savedSound = getSavedSound();
                if (sleepSounds.hasSound(savedSound)) {
                    value.setText(savedSound);
                } else {
                    value.setText(sounds.get(0).getName());
                }
            }
        }
    }

    class SleepDurationsViewHolder extends SleepViewHolder {

        SleepDurationsViewHolder(final @NonNull View itemView) {
            super(itemView);
            image.setImageResource(R.drawable.sounds_duration_icon);
            label.setText(R.string.sleep_sounds_duration_label);

        }

        @Override
        void bind(final int position) {
            if (sleepDurations == null) {
                return;
            }

            final List<Duration> durations = sleepDurations.getDurations();

            if (durations.isEmpty()) {
                value.setText(null);
                return;
            }

            holder.setOnClickListener(v -> {
                interactionListener.onDurationClick(value.getText().toString(), durations);
            });


            final Duration currentDuration = sleepSoundStatus.getDuration();
            if (currentDuration != null && sleepDurations.hasDuration(currentDuration.getName())) {
                value.setText(currentDuration.getName());
            } else {
                final String savedDuration = getSavedDuration();
                if (sleepDurations.hasDuration(savedDuration)) {
                    value.setText(savedDuration);
                } else {
                    value.setText(durations.get(0).getName());
                }
            }
        }
    }

    class SleepVolumeViewHolder extends SleepViewHolder {

        SleepVolumeViewHolder(final @NonNull View itemView) {
            super(itemView);
            image.setImageResource(R.drawable.sounds_volume_icon);
            label.setText(R.string.sleep_sounds_volume_label);
            chevron.setVisibility(View.INVISIBLE);

        }

        @Override
        void bind(final int position) {
            if (sleepSoundStatus == null) {
                return;
            }

            final List<SleepSoundStatus.Volume> volumes = Arrays.asList(SleepSoundsAdapter.volumes);

            if (volumes.isEmpty()) {
                value.setText(null);
                return;
            }

            if (sleepSoundStatus.getVolume() != SleepSoundStatus.Volume.None) {
                value.setText(sleepSoundStatus.getVolume().toString());
            } else {
                value.setText(volumes.get(0).toString());
            }

        }
    }

    public interface InteractionListener {
        void onSoundClick(final @NonNull String currentSound, final @NonNull List<?> sounds);

        void onDurationClick(final @NonNull String currentDuration, final @NonNull List<?> durations);

    }

}
