package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.api.model.v2.Volume;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.util.IListObject;

@SuppressLint("ViewConstructor")
public class SleepSoundsPlayerView extends RelativeLayout
        implements SleepSoundsAdapter.IDisplayedValues {
    private static final float minFadeFactor = .2f;
    private static final float maxFadeFactor = 1f;

    private final AnimatorContext animatorContext;
    private final TitleRow titleRow;
    private final SleepSoundsPlayerRow<Sound> soundRow;
    private final SleepSoundsPlayerRow<Duration> durationRow;
    private final VolumePlayerRow volumeRow;
    private SleepSoundStatus currentStatus;
    private float fadeFactor = 1f;

    public SleepSoundsPlayerView(final @NonNull Context context,
                                 final @NonNull AnimatorContext animatorContext,
                                 final @NonNull SleepSoundsState state,
                                 final @NonNull SleepSoundsAdapter.InteractionListener interactionListener) {
        super(context);
        LayoutInflater.from(getContext()).inflate(R.layout.item_rounded_linearlayout, this);
        final RoundedLinearLayout view = (RoundedLinearLayout) findViewById(R.id.item_rounded_linearlayout);
        view.setOrientation(LinearLayout.VERTICAL);
        this.animatorContext = animatorContext;
        this.currentStatus = state.getStatus();
        this.titleRow = new TitleRow(context);
        this.soundRow = new SleepSoundsPlayerRow<>(context, state.getSounds());
        this.durationRow = new SleepSoundsPlayerRow<>(context, state.getDurations());
        this.volumeRow = new VolumePlayerRow(context, state.getStatus());
        view.addView(titleRow);
        view.addView(soundRow);
        view.addView(durationRow);
        view.addView(volumeRow);
        soundRow.setHolderClickListener(v -> {
            if (!currentStatus.isPlaying() && displayedSound() != null) {
                interactionListener.onSoundClick(displayedSound().getId(), state.getSounds());
            }
        });
        durationRow.setHolderClickListener(v -> {
            if (!currentStatus.isPlaying() && displayedDuration() != null) {
                interactionListener.onDurationClick(displayedDuration().getId(), state.getDurations());
            }
        });
        volumeRow.setHolderClickListener(v -> {
            if (!currentStatus.isPlaying() && displayedVolume() != null) {
                interactionListener.onVolumeClick(displayedVolume().getId(), state.getStatus());
            }
        });
    }

    public void bindStatus(final @NonNull SleepSoundStatus status,
                           final @Nullable Sound savedSound,
                           final @Nullable Duration savedDuration,
                           final @Nullable Volume savedVolume) {
        this.currentStatus = status;
        final ValueAnimator animator;
        if (status.isPlaying()) {
            animator = ValueAnimator.ofFloat(minFadeFactor, maxFadeFactor);
        } else {
            animator = ValueAnimator.ofFloat(maxFadeFactor, minFadeFactor);
        }
        animator.setDuration(Anime.DURATION_SLOW);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            fadeFactor = (float) a.getAnimatedValue();
            invalidate();
            requestLayout();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                titleRow.update();
            }
        });
        animatorContext.startWhenIdle(animator);

        soundRow.bind(status.getSound(), savedSound, null);
        durationRow.bind(status.getDuration(), savedDuration, null);
        volumeRow.bind(status.getVolume(), savedVolume, Volume.Medium);
    }

    @Override
    public Sound displayedSound() {
        return soundRow.displayedItem();
    }

    @Override
    public Duration displayedDuration() {
        return durationRow.displayedItem();
    }

    @Override
    public Volume displayedVolume() {
        return volumeRow.displayedItem();
    }

    private static abstract class PlayerRow extends LinearLayout {

        public PlayerRow(final @NonNull Context context) {
            super(context);
        }

        protected View inflateView(@LayoutRes final int view) {
            return ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(view, this);
        }
    }

    private class TitleRow extends PlayerRow {
        private final TextView title;
        private boolean isPlaying = false;
        private final ImageView leftMusic;
        private final ImageView rightMusic;

        public TitleRow(final @NonNull Context context) {
            super(context);
            final View view = inflateView(R.layout.item_player_title);
            title = ((TextView) view.findViewById(R.id.item_centered_title_text));
            leftMusic = ((ImageView) view.findViewById(R.id.item_centered_image_left));
            rightMusic = ((ImageView) view.findViewById(R.id.item_centered_image_right));
            title.setText(R.string.sleep_sounds_title);
        }

        public void update() {
            if (currentStatus.isPlaying() && !isPlaying) {
                isPlaying = true;
                cycle(R.string.sleep_sounds_title_playing, true);
                animateMusic(leftMusic, .3f, 1f);
                animateMusic(rightMusic, 1f, .3f);
                leftMusic.setVisibility(View.VISIBLE);
                rightMusic.setVisibility(View.VISIBLE);
            } else if (!currentStatus.isPlaying() && isPlaying) {
                isPlaying = false;
                leftMusic.setVisibility(View.INVISIBLE);
                rightMusic.setVisibility(View.INVISIBLE);
                cycle(R.string.sleep_sounds_title, false);
            }
        }

        private void cycle(final @StringRes int textRes, final boolean moveDown) {
            final float initialPosition = 0;
            final float endPosition;
            if (moveDown) {
                endPosition = title.getBottom();
            } else {
                endPosition = -title.getBottom();
            }
            final ValueAnimator animator = ValueAnimator.ofFloat(initialPosition, endPosition);
            animator.setDuration(Anime.DURATION_SLOW);
            animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
            animator.addUpdateListener(a -> {
                title.setY((float) a.getAnimatedValue());
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    title.setText(textRes);
                    final ValueAnimator animator2 = ValueAnimator.ofFloat(-endPosition, initialPosition);
                    animator2.setDuration(Anime.DURATION_SLOW);
                    animator2.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
                    animator2.addUpdateListener(a -> {
                        title.setY((float) a.getAnimatedValue());
                    });
                    animatorContext.startWhenIdle(animator2);
                }
            });
            animatorContext.startWhenIdle(animator);
        }

        private void animateMusic(final @NonNull ImageView view, final float start, final float end) {
            final ValueAnimator animator = ValueAnimator.ofFloat(start, end);
            animator.setDuration(500);
            animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
            animator.addUpdateListener(a -> {
                view.setScaleY((float) a.getAnimatedValue());
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (isPlaying) {
                        animateMusic(view, end, start);
                    }
                }
            });
            animatorContext.startWhenIdle(animator);
        }

    }

    private class SleepSoundsPlayerRow<itemType extends IListObject.IListItem>
            extends PlayerRow implements IDisplayedValue<itemType> {
        protected final ISleepSoundsPlayerRowItem<itemType> rowItem;
        protected final View holder;
        protected final TextView label;
        protected final TextView value;
        protected final ImageView image;
        protected itemType listItem;

        public SleepSoundsPlayerRow(final @NonNull Context context, final @NonNull ISleepSoundsPlayerRowItem<itemType> item) {
            super(context);
            this.holder = inflateView(R.layout.item_sleep_sounds);  // todo change view name
            this.image = (ImageView) holder.findViewById(R.id.item_sleep_sounds_image);
            this.label = (TextView) holder.findViewById(R.id.item_sleep_sounds_label);
            this.value = (TextView) holder.findViewById(R.id.item_sleep_sounds_value);
            this.rowItem = item;
            this.image.setImageResource(item.getImageRes());
            this.label.setText(item.getLabelRes());
        }

        public void bind(final @Nullable itemType currentItem,
                         final @Nullable itemType savedItem,
                         final @Nullable itemType defaultItem) {
            final List<itemType> items = rowItem.getListObject()
                                                .getListItems();

            if (currentItem != null && itemIsContained(items, currentItem)) {
                listItem = currentItem;
            } else if (savedItem != null) {
                listItem = savedItem;
            } else if (defaultItem != null) {
                listItem = defaultItem;
            } else if (items.isEmpty()){
                listItem = null;
            } else {
                listItem = items.get(0);
            }
            value.setText(listItem == null ? null : listItem.toString());
            label.setAlpha(fadeFactor);
            image.setAlpha(fadeFactor);
            value.setAlpha(fadeFactor);
            invalidate();
            requestLayout();
        }

        public void setHolderClickListener(final OnClickListener onClickListener) {
            holder.setOnClickListener(onClickListener);
        }

        @Nullable
        @Override
        public itemType displayedItem() {
            return listItem;
        }

        private boolean itemIsContained(@NonNull final List<itemType> items,
                                        @NonNull final itemType currentItem) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId() == currentItem.getId()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class VolumePlayerRow extends SleepSoundsPlayerRow<Volume> {

        public VolumePlayerRow(@NonNull final Context context,
                               @NonNull final SleepSoundStatus item) {
            super(context, item);
        }

        @Override
        public void bind(@Nullable final Volume currentItem,
                         @Nullable final Volume savedItem,
                         @Nullable final Volume defaultItem) {
            if (savedItem == Volume.None) {
                super.bind(currentItem, null, defaultItem);
            } else{
                super.bind(currentItem, savedItem, defaultItem);
            }

        }
    }

    public interface ISleepSoundsPlayerRowItem<itemType extends IListObject.IListItem> {
        @StringRes
        int getLabelRes();

        @DrawableRes
        int getImageRes();

        IListObject<itemType> getListObject();
    }

    public interface IDisplayedValue<itemType extends IListObject.IListItem> {
        itemType displayedItem();
    }
}
