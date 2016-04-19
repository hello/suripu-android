package is.hello.sense.ui.adapter;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.util.Player;

public abstract class SleepSoundsListAdapter extends RecyclerView.Adapter<SleepSoundsListAdapter.BaseViewHolder> {
    private static final int NONE = -1;

    protected int selectedId = NONE;
    protected final LayoutInflater inflater;

    public SleepSoundsListAdapter(final int selectedId, final @NonNull LayoutInflater layoutInflater) {
        this.selectedId = selectedId;
        this.inflater = layoutInflater;
    }

    public abstract void onBindViewHolder(final BaseViewHolder holder, final int position);

    protected abstract Object getItem(final int position);

    protected void setSelectedId(final int selectedId) {
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    public int getSelectedId() {
        return selectedId;
    }

    public void finish() {

    }

    public static abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        protected final TextView title;
        protected final ImageView image;
        protected final View view;

        BaseViewHolder(final @NonNull View view) {
            super(view);
            this.view = view;
            this.title = (TextView) view.findViewById(R.id.item_list_name);
            this.image = (ImageView) view.findViewById(R.id.item_list_play_image);
        }

        protected void selectedState() {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_on, 0, 0, 0);
        }

        public void unselectedState() {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_off, 0, 0, 0);
        }

        public String getTitle() {
            return title.getText().toString();
        }
    }

    public static class SoundsListAdapter extends SleepSoundsListAdapter implements Player.OnEventListener {
        private final SleepSounds sleepSounds;
        private final Player player;
        private PlayerStatus playerStatus = PlayerStatus.Idle;
        private int requestedSoundId = NONE;

        public SoundsListAdapter(final @NonNull Activity activity, final int selectedId, final @NonNull SleepSounds sleepSounds) {
            super(selectedId, activity.getLayoutInflater());
            this.sleepSounds = sleepSounds;
            this.player = new Player(activity, this, null);

        }

        // <-- SleepSoundsListAdapter
        @Override
        public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            return new SoundsBaseViewHolder(inflater.inflate(R.layout.item_list, null));
        }

        @Override
        public int getItemCount() {
            return sleepSounds.getSounds().size();
        }

        @Override
        protected Sound getItem(final int position) {
            return sleepSounds.getSounds().get(position);
        }

        @Override
        public void finish() {
            player.stopPlayback();
            player.recycle();
        }

        @Override
        public void onBindViewHolder(final BaseViewHolder holder, final int position) {
            final Sound sound = getItem(position);

            holder.title.setText(sound.toString());
            holder.title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedId == sound.getId()) {
                        return;
                    }
                    setSelectedId(sound.getId());
                    player.stopPlayback();
                }
            });
            if (sound.getId() == selectedId) {
                holder.selectedState();
                switch (playerStatus) {
                    case Idle:
                        ((SoundsBaseViewHolder) holder).enterIdleState(() -> {
                            requestedSoundId = sound.getId();
                            playerStatus = PlayerStatus.Loading;
                            player.setDataSource(Uri.parse(sound.getPreviewUrl()), true);
                        });
                        break;
                    case Playing:
                        ((SoundsBaseViewHolder) holder).enterPlayingState(() -> {
                            player.stopPlayback();
                            playerStatus = PlayerStatus.Idle;
                            notifyDataSetChanged();
                        });
                        break;
                }
            } else {
                holder.unselectedState();
            }
        }

        // SleepSoundsListAdapter -->

        // <-- OnEventListener
        @Override
        public void onPlaybackReady(final @NonNull Player player) {
        }

        @Override
        public void onPlaybackStarted(final @NonNull Player player) {
            if (requestedSoundId == selectedId) {
                playerStatus = PlayerStatus.Playing;
                notifyDataSetChanged();
            } else {
                player.stopPlayback();
            }
        }

        @Override
        public void onPlaybackStopped(final @NonNull Player player, final boolean finished) {
            playerStatus = PlayerStatus.Idle;
            notifyDataSetChanged();

        }

        @Override
        public void onPlaybackError(final @NonNull Player player, final @NonNull Throwable error) {
            //todo display error?
            playerStatus = PlayerStatus.Idle;
            notifyDataSetChanged();
        }
        // OnEventListener -->


        private class SoundsBaseViewHolder extends BaseViewHolder {
            private final static int deltaRotation = 5; // degrees
            private final static int spinnerInterval = 1; // ms

            @DrawableRes
            private final static int playIcon = R.drawable.sound_preview_play;

            @DrawableRes
            private final static int loadingIcon = R.drawable.sound_preview_loading;

            @DrawableRes
            private final static int stopIcon = R.drawable.sound_preview_stop;

            // todo create a custom component that has this logic
            final Runnable spinningRunnable = new Runnable() {
                @Override
                public void run() {
                    if (image != null) {
                        image.setRotation(image.getRotation() + deltaRotation);
                        image.postDelayed(this, spinnerInterval);
                    }
                }
            };

            SoundsBaseViewHolder(final @NonNull View view) {
                super(view);
            }

            @Override
            protected void selectedState() {
                super.selectedState();
                image.setVisibility(View.VISIBLE);
            }

            @Override
            public void unselectedState() {
                image.setVisibility(View.INVISIBLE);
                image.setOnClickListener(null);
                super.unselectedState();
            }

            private void enterIdleState(final @NonNull PlayerCallback callback) {
                image.setOnClickListener(v -> {
                    callback.userClickedImage();
                    enterLoadingState();
                });
                image.setImageResource(playIcon);
                image.removeCallbacks(spinningRunnable);
                image.setRotation(0);
            }

            private void enterPlayingState(final @NonNull PlayerCallback callback) {
                image.setOnClickListener(v -> {
                    callback.userClickedImage();
                    image.setOnClickListener(null);

                });
                image.setImageResource(stopIcon);
                image.removeCallbacks(spinningRunnable);
                image.setRotation(0);
            }

            private void enterLoadingState() {
                image.setOnClickListener(null);
                image.setImageResource(loadingIcon);
                image.post(spinningRunnable);
            }

        }

        private enum PlayerStatus {
            Idle, Loading, Playing
        }

        private interface PlayerCallback {
            void userClickedImage();
        }
    }

    public static class DurationsListAdapter extends SleepSoundsListAdapter {
        private final SleepDurations sleepDurations;
        private final Callback callback;

        public DurationsListAdapter(final @NonNull Activity activity,
                                    final @NonNull Callback callback,
                                    final int selectedId,
                                    final @NonNull SleepDurations sleepDurations) {
            super(selectedId, activity.getLayoutInflater());
            this.sleepDurations = sleepDurations;
            this.callback = callback;
        }

        @Override
        public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            return new DurationBaseViewHolder(inflater.inflate(R.layout.item_list, null));
        }

        @Override
        public void onBindViewHolder(final BaseViewHolder holder, final int position) {
            final Duration duration = getItem(position);
            holder.title.setText(duration.getName());
            if (selectedId == duration.getId()) {
                holder.selectedState();
            } else {
                holder.unselectedState();
            }
            holder.title.setOnClickListener(v -> {
                selectedId = duration.getId();
                notifyDataSetChanged();
                callback.setResultAndFinish();
            });
        }

        @Override
        public int getItemCount() {
            return sleepDurations.getDurations().size();
        }

        @Override
        protected Duration getItem(final int position) {
            return sleepDurations.getDurations().get(position);
        }

        private class DurationBaseViewHolder extends BaseViewHolder {

            DurationBaseViewHolder(final @NonNull View view) {
                super(view);
            }
        }
    }

    public static class VolumeListAdapter extends SleepSoundsListAdapter {
        private final SleepSoundStatus sleepSoundStatus;
        private final Callback callback;

        public VolumeListAdapter(final @NonNull Activity activity,
                                 final @NonNull Callback callback,
                                 final int selectedId,
                                 final @NonNull SleepSoundStatus sleepSoundStatus) {
            super(selectedId, activity.getLayoutInflater());
            this.sleepSoundStatus = sleepSoundStatus;
            this.callback = callback;
        }

        @Override
        public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            return new VolumeBaseViewHolder(inflater.inflate(R.layout.item_list, null));
        }

        @Override
        public void onBindViewHolder(final BaseViewHolder holder, final int position) {
            SleepSoundStatus.Volume volume = getItem(position);
            holder.title.setText(volume.toString());
            if (selectedId == volume.getVolume()) {
                holder.selectedState();
            } else {
                holder.unselectedState();
            }
            holder.title.setOnClickListener(v -> {
                selectedId = volume.getVolume();
                notifyDataSetChanged();
                callback.setResultAndFinish();
            });
        }

        @Override
        public int getItemCount() {
            return sleepSoundStatus.getVolumes().size();
        }

        @Override
        protected SleepSoundStatus.Volume getItem(final int position) {
            return sleepSoundStatus.getVolumes().get(position);
        }

        private class VolumeBaseViewHolder extends BaseViewHolder {

            VolumeBaseViewHolder(final @NonNull View view) {
                super(view);
            }
        }
    }

    public interface Callback {
        void setResultAndFinish();
    }

}
