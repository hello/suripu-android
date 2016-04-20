package is.hello.sense.ui.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.util.Constants;

public class SleepSoundsAdapter extends RecyclerView.Adapter<SleepSoundsAdapter.BaseViewHolder> {
    private static final int TEMP_PROBLEM_ITEM_COUNT = 1;
    private static final int DESIRED_ITEM_COUNT = 4;
    private static final int VIEW_TITLE = 0;
    private static final int VIEW_VOLUME = 1;
    private static final int VIEW_SOUNDS = 2;
    private static final int VIEW_DURATIONS = 3;
    private static final int VIEW_SENSE_FIRMWARE_UPDATE = 4;
    private static final int VIEW_SENSE_SOUNDS_DOWNLOAD = 5;
    private static final int VIEW_ERROR = 6;

    private final LayoutInflater inflater;
    private final SharedPreferences preferences;
    private final InteractionListener interactionListener;
    private final AnimatorContext animatorContext;

    private SleepSoundStatus sleepSoundStatus;
    private SleepSounds sleepSounds;
    private SleepDurations sleepDurations;
    private int itemCount = 0;
    private final float minFadeFactor = .2f;
    private final float maxFadeFactor = 1f;
    private float fadeFactor = 1f;

    private Sound displayedSound;
    private Duration displayedDuration;
    private SleepSoundStatus.Volume displayedVolume;
    private Retry retry;

    public SleepSoundsAdapter(final @NonNull Context context,
                              final @NonNull SharedPreferences preferences,
                              final @NonNull InteractionListener interactionListener,
                              final @NonNull AnimatorContext animatorContext,
                              final @NonNull Retry retry) {
        this.interactionListener = interactionListener;
        this.inflater = LayoutInflater.from(context);
        this.preferences = preferences;
        this.animatorContext = animatorContext;
        this.retry = retry;
    }

    public void bind(final @NonNull SleepSoundStatus status,
                     final @NonNull SleepSounds sleepSounds,
                     final @NonNull SleepDurations sleepDurations) {
        this.sleepSoundStatus = status;
        this.sleepSounds = sleepSounds;
        this.sleepDurations = sleepDurations;

        if (sleepSounds.getState() != SleepSounds.State.OK) {
            this.itemCount = TEMP_PROBLEM_ITEM_COUNT;
        } else {
            this.itemCount = DESIRED_ITEM_COUNT;
        }

        if (sleepSoundStatus.isPlaying()) {
            fadeFactor = minFadeFactor;
        }
        notifyDataSetChanged();
    }

    public void bind(final @NonNull SleepSoundStatus status) {
        if (!hasDesiredItemCount() || this.sleepSoundStatus == null) {
            return; // todo determine how to recover when SleepSoundState fails.
        }

        if (this.sleepSoundStatus.isPlaying() != status.isPlaying()) {
            this.sleepSoundStatus = status;
            final ValueAnimator animator;
            if (status.isPlaying()) {
                animator = ValueAnimator.ofFloat(maxFadeFactor, minFadeFactor);
            } else {
                animator = ValueAnimator.ofFloat(minFadeFactor, maxFadeFactor);
            }
            animator.setDuration(Anime.DURATION_SLOW);
            animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
            animator.addUpdateListener(a -> {
                fadeFactor = (float) a.getAnimatedValue();
                notifyDataSetChanged();
            });
            animatorContext.startWhenIdle(animator);
        }

    }

    public Sound getDisplayedSound() {
        return displayedSound;
    }

    public Duration getDisplayedDuration() {
        return displayedDuration;
    }

    public SleepSoundStatus.Volume getDisplayedVolume() {
        return displayedVolume;
    }

    public void setErrorState() {
        itemCount = 1;
        sleepSounds = null;
        sleepSoundStatus = null;
        sleepDurations = null;
        notifyDataSetChanged();
    }

    private Sound getSavedSound() {
        return sleepSounds.getSoundWithId(preferences.getInt(Constants.SLEEP_SOUNDS_SOUND_ID, -1));
    }

    private Duration getSavedDuration() {
        return sleepDurations.getDurationWithId(preferences.getInt(Constants.SLEEP_SOUNDS_DURATION_ID, -1));
    }

    private SleepSoundStatus.Volume getSavedVolume() {
        return sleepSoundStatus.getVolumeWithValue(preferences.getInt(Constants.SLEEP_SOUNDS_VOLUME_ID, -1));
    }


    @Override
    public int getItemViewType(final int position) {
        if (hasDesiredItemCount()) {
            if (position == 0) {
                return VIEW_TITLE;
            } else if (position == 1) {
                return VIEW_SOUNDS;
            } else if (position == 2) {
                return VIEW_DURATIONS;
            } else {
                return VIEW_VOLUME;
            }
        } else if (this.sleepSounds != null) {
            final SleepSounds.State state = this.sleepSounds.getState();
            if (state == SleepSounds.State.SENSE_UPDATE_REQUIRED) {
                return VIEW_SENSE_FIRMWARE_UPDATE;
            } else if (state == SleepSounds.State.SOUNDS_NOT_DOWNLOADED) {
                return VIEW_SENSE_SOUNDS_DOWNLOAD;
            }
        }
        return VIEW_ERROR;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == VIEW_TITLE) {
            return new TitleViewHolder(inflater.inflate(R.layout.item_centered_title, parent, false));
        } else if (viewType == VIEW_SOUNDS) {
            return new SleepSoundsViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
        } else if (viewType == VIEW_DURATIONS) {
            return new SleepDurationsViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
        } else if (viewType == VIEW_VOLUME) {
            return new SleepVolumeViewHolder(inflater.inflate(R.layout.item_sleep_sounds, parent, false));
        } else if (viewType == VIEW_SENSE_FIRMWARE_UPDATE) {
            return new FwUpdateStateViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
        } else if (viewType == VIEW_SENSE_SOUNDS_DOWNLOAD) {
            return new NoSoundsStateViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
        }
        return new ErrorViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public boolean hasDesiredItemCount() {
        return itemCount == DESIRED_ITEM_COUNT;
    }

    abstract static class BaseViewHolder extends RecyclerView.ViewHolder {

        BaseViewHolder(final @NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(final int position);
    }

    public class TitleViewHolder extends BaseViewHolder {
        private final TextView title;

        TitleViewHolder(final @NonNull View itemView) {
            super(itemView);
            title = ((TextView) itemView.findViewById(R.id.item_centered_title_text));
        }

        @Override
        void bind(final int position) {
            if (sleepSoundStatus.isPlaying()) {
                title.setText(R.string.sleep_sounds_title_playing);
            } else {
                title.setText(R.string.sleep_sounds_title);
            }
        }
    }

    //region sleep state view holders

    public abstract class SleepStateViewHolder extends BaseViewHolder {
        protected final ImageView image;
        protected final TextView title;
        protected final TextView message;

        SleepStateViewHolder(final @NonNull View view) {
            super(view);

            this.image = (ImageView) view.findViewById(R.id.item_message_card_image);
            this.title = (TextView) view.findViewById(R.id.item_message_card_title);
            this.message = (TextView) view.findViewById(R.id.item_message_card_message);

            final Button action = (Button) view.findViewById(R.id.item_message_card_action);
            action.setVisibility(View.GONE);

            title.setGravity(Gravity.CENTER_HORIZONTAL);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //noinspection deprecation
                message.setTextAppearance(view.getContext(), R.style.AppTheme_Text_Body_Small_New);
            } else {
                message.setTextAppearance(R.style.AppTheme_Text_Body_Small_New);
            }
        }
    }

    class NoSoundsStateViewHolder extends SleepStateViewHolder {

        NoSoundsStateViewHolder(final @NonNull View view) {
            super(view);
        }

        @Override
        void bind(final int ignored) {
            this.title.setText(R.string.sleep_sounds_state_no_sounds_title);
            this.message.setText(R.string.sleep_sounds_state_no_sounds_message);
            this.image.setImageResource(R.drawable.illustration_sense_download);
        }
    }

    class FwUpdateStateViewHolder extends SleepStateViewHolder {

        FwUpdateStateViewHolder(final @NonNull View view) {
            super(view);
        }

        @Override
        void bind(final int ignored) {
            this.title.setText(R.string.sleep_sounds_state_fw_update_title);
            this.message.setText(R.string.sleep_sounds_state_fw_update_message);
            this.image.setImageResource(R.drawable.illustration_sense_update);
        }
    }

    //endregion

    //region error

    public class ErrorViewHolder extends BaseViewHolder implements View.OnClickListener {
        private final TextView message;
        private final Button action;

        ErrorViewHolder(final @NonNull View view) {
            super(view);

            final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
            title.setVisibility(View.GONE);

            this.message = (TextView) view.findViewById(R.id.item_message_card_message);
            this.action = (Button) view.findViewById(R.id.item_message_card_action);
            action.setOnClickListener(this);
        }

        @Override
        void bind(final int ignored) {
            action.setText(R.string.action_retry);
            message.setText(R.string.sleep_sounds_error_generic);
        }

        @Override
        public void onClick(final View view) {
            retry.retry();
        }

    }

    //endregion

    public abstract class SleepViewHolder extends BaseViewHolder {
        protected final ImageView image;
        protected final TextView label;
        protected final TextView value;
        protected final View holder;

        SleepViewHolder(final @NonNull View itemView) {
            super(itemView);
            this.holder = itemView;
            this.image = (ImageView) itemView.findViewById(R.id.item_sleep_sounds_image);
            this.label = (TextView) itemView.findViewById(R.id.item_sleep_sounds_label);
            this.value = (TextView) itemView.findViewById(R.id.item_sleep_sounds_value);
        }

        protected void applyFadeFactor() {
            label.setAlpha(fadeFactor);
            image.setAlpha(fadeFactor);
            value.setAlpha(fadeFactor);
            holder.setClickable(!sleepSoundStatus.isPlaying());
            holder.setEnabled(!sleepSoundStatus.isPlaying());
        }
    }

    class SleepSoundsViewHolder extends SleepViewHolder {

        SleepSoundsViewHolder(final @NonNull View itemView) {
            super(itemView);
            image.setImageResource(R.drawable.icon_alarm_tone);
            label.setText(R.string.sleep_sounds_sound_label);
        }

        @Override
        void bind(final int position) {
            applyFadeFactor();

            final List<Sound> sounds = sleepSounds.getSounds();

            if (sounds.isEmpty()) {
                value.setText(null);
                return;
            }

            final Sound currentSounds = sleepSoundStatus.getSound();
            if (currentSounds != null && sleepSounds.hasSound(currentSounds.getName())) {
                displayedSound = currentSounds;
                value.setText(currentSounds.getName());
            } else {
                final Sound savedSound = getSavedSound();
                if (savedSound != null) {
                    displayedSound = savedSound;
                    value.setText(savedSound.getName());
                } else {
                    displayedSound = sounds.get(0);
                    value.setText(sounds.get(0).getName());
                }
            }

            holder.setOnClickListener(v -> {
                String soundText = value.getText().toString();
                interactionListener.onSoundClick(displayedSound.getId(), sleepSounds);
            });
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
            applyFadeFactor();

            final List<Duration> durations = sleepDurations.getDurations();

            if (durations.isEmpty()) {
                value.setText(null);
                return;
            }

            final Duration currentDuration = sleepSoundStatus.getDuration();
            if (currentDuration != null && sleepDurations.hasDuration(currentDuration.getName())) {
                value.setText(currentDuration.getName());
                displayedDuration = currentDuration;
            } else {
                final Duration savedDuration = getSavedDuration();
                if (savedDuration != null) {
                    value.setText(savedDuration.getName());
                    displayedDuration = savedDuration;
                } else {
                    value.setText(durations.get(0).getName());
                    displayedDuration = durations.get(0);
                }
            }

            holder.setOnClickListener(v -> {
                interactionListener.onDurationClick(displayedDuration.getId(), sleepDurations);
            });

        }
    }

    class SleepVolumeViewHolder extends SleepViewHolder {

        SleepVolumeViewHolder(final @NonNull View itemView) {
            super(itemView);
            image.setImageResource(R.drawable.sounds_volume_icon);
            label.setText(R.string.sleep_sounds_volume_label);
        }

        @Override
        void bind(final int position) {
            applyFadeFactor();

            final List<SleepSoundStatus.Volume> volumes = sleepSoundStatus.getVolumes();

            if (volumes.isEmpty()) {
                value.setText(null);
                return;
            }

            if (sleepSoundStatus.getVolume() != SleepSoundStatus.Volume.None) {
                value.setText(sleepSoundStatus.getVolume().toString());
                displayedVolume = sleepSoundStatus.getVolume();
            } else {
                final SleepSoundStatus.Volume savedVolume = getSavedVolume();
                if (!savedVolume.equals(SleepSoundStatus.Volume.None)) {
                    value.setText(savedVolume.toString());
                    displayedVolume = savedVolume;
                } else {
                    value.setText(volumes.get(0).toString());
                    displayedVolume = volumes.get(0);
                }
            }

            holder.setOnClickListener(v -> {
                interactionListener.onVolumeClick(displayedVolume.getVolume(), sleepSoundStatus);
            });
        }
    }

    public interface InteractionListener {
        void onSoundClick(final int currentSound, final @NonNull SleepSounds sleepSounds);

        void onDurationClick(final int currentDuration, final @NonNull SleepDurations sleepDurations);

        void onVolumeClick(final int currentVolume, final @NonNull SleepSoundStatus status);
    }

    public interface Retry {
        void retry();
    }

}
