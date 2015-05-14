package is.hello.sense.ui.widget.timeline;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.sense.functional.Functions;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;

/**
 * A simple staggered fade-in animation.
 * <p />
 * Each item faded-in has the delay <code>{@link #DELAY} * index</code>.
 */
public class TimelineSimpleItemAnimator extends AbstractTimelineItemAnimator {
    public static final long DELAY = 20;

    private final AnimatorConfig config = new AnimatorConfig(Animation.DURATION_FAST);

    private final List<RecyclerView.ViewHolder> pending = new ArrayList<>();
    private final List<RecyclerView.ViewHolder> running = new ArrayList<>();

    public TimelineSimpleItemAnimator(@NonNull AnimatorContext animatorContext, @NonNull Listener listener) {
        super(animatorContext, listener);
    }

    @Override
    public void runPendingAnimations() {
        Collections.sort(pending, (l, r) -> Functions.compareInts(l.getLayoutPosition(), r.getLayoutPosition()));

        dispatchAnimationWillStart(config);
        getAnimatorContext().transaction(config, f -> {
            long delay = DELAY;
            for (RecyclerView.ViewHolder item : pending) {
                dispatchAddStarting(item);

                f.animate(item.itemView)
                 .setStartDelay(delay)
                 .fadeIn();

                delay += DELAY;
            }

            running.addAll(pending);
            pending.clear();
        }, finished -> {
            for (RecyclerView.ViewHolder item : running) {
                if (!finished) {
                    item.itemView.setAlpha(1f);
                }

                dispatchAddFinished(item);
            }

            running.clear();

            dispatchAnimationDidEnd(finished);
        });
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha(0f);
        pending.add(holder);
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        PropertyAnimatorProxy.stop(item.itemView);
    }

    @Override
    public void endAnimations() {
        for (RecyclerView.ViewHolder item : running) {
            PropertyAnimatorProxy.stop(item.itemView);
        }
    }

    @Override
    public boolean isRunning() {
        return !running.isEmpty();
    }
}
