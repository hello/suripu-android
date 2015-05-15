package is.hello.sense.ui.widget.timeline;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.ui.adapter.TimelineBaseViewHolder;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;

public class TimelineEntranceItemAnimator extends AbstractTimelineItemAnimator {
    private final List<TimelineBaseViewHolder> pending = new ArrayList<>();
    private final List<TimelineBaseViewHolder> running = new ArrayList<>();

    private final AnimatorConfig animatorConfig = new AnimatorConfig(500);

    public TimelineEntranceItemAnimator(@NonNull AnimatorContext animatorContext, @NonNull Listener listener) {
        super(animatorContext, listener);
    }


    @Override
    public void runPendingAnimations() {
        sortByPosition(pending);
        dispatchAnimationWillStart(animatorConfig);
        getAnimatorContext().transaction(animatorConfig, f -> {
            for (TimelineBaseViewHolder holder : pending) {
                dispatchAddStarting(holder);

                holder.provideRenderAnimation(f);
            }
            running.addAll(pending);
            pending.clear();
        }, finished -> {
            for (TimelineBaseViewHolder item : running) {
                if (!finished) {
                    item.cancelRenderAnimation();
                }
                item.cleanUpAfterRenderAnimation();

                dispatchAddFinished(item);
            }

            running.clear();

            dispatchAnimationDidEnd(finished);
        });
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        TimelineBaseViewHolder timelineHolder = (TimelineBaseViewHolder) item;
        timelineHolder.cancelRenderAnimation();
    }

    @Override
    public void endAnimations() {
        for (TimelineBaseViewHolder holder : running) {
            holder.cancelRenderAnimation();
        }
    }

    @Override
    public boolean isRunning() {
        return !running.isEmpty();
    }


    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (!isViewHolderAnimated(holder)) {
            dispatchAddFinished(holder);
            return false;
        }

        TimelineBaseViewHolder timelineHolder = (TimelineBaseViewHolder) holder;
        timelineHolder.prepareForRenderAnimation();
        pending.add(timelineHolder);
        return true;
    }
}
