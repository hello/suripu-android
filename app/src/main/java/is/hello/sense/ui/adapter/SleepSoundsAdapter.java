package is.hello.sense.ui.adapter;

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

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.ui.widget.SleepSoundsPlayerView;
import is.hello.sense.util.Constants;

public class SleepSoundsAdapter extends RecyclerView.Adapter<SleepSoundsAdapter.BaseViewHolder> {

    private final LayoutInflater inflater;
    private final SharedPreferences preferences;
    private final InteractionListener interactionListener;
    private final AnimatorContext animatorContext;
    private SleepSoundsState combinedSleepState;
    private SleepSoundStatus sleepSoundStatus;
    private AdapterState currentState = AdapterState.NONE;

    private Retry retry;
    private final Context context;
    private IDisplayedValues displayedValues;


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
        this.context = context;
    }

    public void bindData(final @NonNull SleepSoundsState combinedState) {
        this.combinedSleepState = combinedState;
        this.sleepSoundStatus = combinedState.getStatus();
        this.currentState = AdapterState.PLAYER;
        notifyDataSetChanged();
    }

    public void bind(final @NonNull SleepSoundStatus status) {
        if (this.combinedSleepState == null) {
            currentState = AdapterState.NONE;
            return;
        }
        this.sleepSoundStatus = status;
        if (this.combinedSleepState.getSounds() != null) {
            final SleepSounds.State state = this.combinedSleepState.getSounds().getState();
            if (state == SleepSounds.State.SENSE_UPDATE_REQUIRED) {
                currentState = AdapterState.FIRMWARE_UPDATE;
            } else if (state == SleepSounds.State.SOUNDS_NOT_DOWNLOADED) {
                currentState = AdapterState.SOUNDS_DOWNLOAD;
            }
        }
        notifyDataSetChanged();
    }

    public boolean isShowingPlayer() {
        return this.currentState == AdapterState.PLAYER;
    }

    public Sound getDisplayedSound() {
        return displayedValues.displayedSound();
    }

    public Duration getDisplayedDuration() {
        return displayedValues.displayedDuration();
    }

    public SleepSoundStatus.Volume getDisplayedVolume() {
        return displayedValues.displayedVolume();
    }

    public void setErrorState() {
        currentState = AdapterState.ERROR;
        sleepSoundStatus = null;
        notifyDataSetChanged();
    }

    public void setOfflineTooLong() {
        currentState = AdapterState.OFFLINE;
        sleepSoundStatus = null;
        notifyDataSetChanged();
    }

    public void setUpdatingState() {
        currentState = AdapterState.FIRMWARE_UPDATE;
        sleepSoundStatus = null;
        notifyDataSetChanged();
    }

    private Sound getSavedSound() {
        return combinedSleepState.getSounds().getSoundWithId(preferences.getInt(Constants.SLEEP_SOUNDS_SOUND_ID, -1));
    }

    private Duration getSavedDuration() {
        return combinedSleepState.getDurations().getDurationWithId(preferences.getInt(Constants.SLEEP_SOUNDS_DURATION_ID, -1));
    }

    private SleepSoundStatus.Volume getSavedVolume() {
        return combinedSleepState.getStatus().getVolumeWithValue(preferences.getInt(Constants.SLEEP_SOUNDS_VOLUME_ID, -1));
    }


    @Override
    public int getItemViewType(final int position) {
        return currentState.ordinal();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == AdapterState.PLAYER.ordinal()) {
            final SleepSoundsPlayerView playerView = new SleepSoundsPlayerView(context, animatorContext, combinedSleepState, interactionListener);
            displayedValues = playerView;
            return new SleepSoundsPlayerViewHolder(playerView);
        } else if (viewType == AdapterState.FIRMWARE_UPDATE.ordinal()) {
            return new FwUpdateStateViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
        } else if (viewType == AdapterState.SOUNDS_DOWNLOAD.ordinal()) {
            return new NoSoundsStateViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
        } else if (viewType == AdapterState.OFFLINE.ordinal()) {
            return new OfflineViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
        }
        return new ErrorViewHolder(inflater.inflate(R.layout.item_message_card, parent, false));
    }


    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return currentState == AdapterState.NONE ? 0 : 1;
    }

    abstract static class BaseViewHolder extends RecyclerView.ViewHolder {

        BaseViewHolder(final @NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(final int position);
    }

    public class SleepSoundsPlayerViewHolder extends BaseViewHolder {
        private final SleepSoundsPlayerView view;

        SleepSoundsPlayerViewHolder(@NonNull SleepSoundsPlayerView playerView) {
            super(playerView);
            this.view = playerView;
        }

        @Override
        void bind(int position) {
            view.bindStatus(sleepSoundStatus,
                            getSavedSound(),
                            getSavedDuration(),
                            getSavedVolume());
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

    class OfflineViewHolder extends SleepStateViewHolder {
        OfflineViewHolder(final @NonNull View view) {
            super(view);
        }

        @Override
        void bind(int position) {
            this.title.setText(R.string.sense_offline_title);
            this.image.setImageResource(R.drawable.illustration_sense_offline);
            this.message.setText(R.string.sense_offline_message);

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

    private enum AdapterState {
        NONE,
        PLAYER,
        FIRMWARE_UPDATE,
        SOUNDS_DOWNLOAD,
        ERROR,
        OFFLINE
    }

    //endregion

    public interface InteractionListener {
        void onSoundClick(final int currentSound, final @NonNull SleepSounds sleepSounds);

        void onDurationClick(final int currentDuration, final @NonNull SleepDurations sleepDurations);

        void onVolumeClick(final int currentVolume, final @NonNull SleepSoundStatus status);
    }

    public interface Retry {
        void retry();
    }

    public interface IDisplayedValues {
        Sound displayedSound();

        Duration displayedDuration();

        SleepSoundStatus.Volume displayedVolume();
    }


}
