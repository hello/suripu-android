package is.hello.sense.ui.widget.timeline;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;

/**
 * A simple staggered fade-in animation.
 * <p />
 * Each item faded-in has the delay <code>{@link #DELAY} * index</code>.
 */
public class TimelineFadeItemAnimator extends AbstractTimelineItemAnimator {
    public static final long DELAY = 20;

    private final AnimatorConfig config = AnimatorConfig.DEFAULT;

    private final List<Transaction> pending = new ArrayList<>();
    private final List<Transaction> running = new ArrayList<>();

    private boolean removeAnimationEnabled = false;

    public TimelineFadeItemAnimator(@NonNull AnimatorContext animatorContext, int headerCount) {
        super(animatorContext, headerCount);
    }

    public void setRemoveAnimationEnabled(boolean removeAnimationEnabled) {
        this.removeAnimationEnabled = removeAnimationEnabled;
    }

    @Override
    public void runPendingAnimations() {
        sortByPosition(pending);
        getAnimatorContext().transaction(config, AnimatorContext.OPTIONS_DEFAULT, f -> {
            dispatchAnimationWillStart(f);

            long delay = DELAY;
            for (Transaction transaction : pending) {
                RecyclerView.ViewHolder target = transaction.target;
                dispatchAddStarting(target);

                switch (transaction.action) {
                    case ADD: {
                        f.animate(target.itemView)
                         .setStartDelay(delay)
                         .fadeIn();

                        break;
                    }
                    case REMOVE: {
                        f.animate(target.itemView)
                         .setStartDelay(delay)
                         .fadeOut(View.VISIBLE);

                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("Transaction type " +
                                transaction.action + " currently unsupported.");
                    }
                }

                delay += DELAY;
            }

            running.addAll(pending);
            pending.clear();
        }, finished -> {
            for (Transaction transaction : running) {
                RecyclerView.ViewHolder target = transaction.target;
                if (!finished) {
                    switch (transaction.action) {
                        case ADD: {
                            target.itemView.setAlpha(1f);
                            break;
                        }
                        case REMOVE: {
                            target.itemView.setAlpha(0f);
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("Transaction type " +
                                    transaction.action + " currently unsupported.");
                        }
                    }
                }

                dispatchAddFinished(target);
            }

            running.clear();

            dispatchAnimationDidEnd(finished);
        });
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (!isViewHolderAnimated(holder)) {
            dispatchAddFinished(holder);
            return false;
        }

        holder.itemView.setAlpha(0f);
        pending.add(new Transaction(Action.ADD, holder));
        return true;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        if (!removeAnimationEnabled || !isViewHolderAnimated(holder)) {
            dispatchRemoveFinished(holder);
            return false;
        }

        holder.itemView.setAlpha(1f);
        pending.add(new Transaction(Action.REMOVE, holder));
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        PropertyAnimatorProxy.stop(item.itemView);
    }

    @Override
    public void endAnimations() {
        for (Transaction transaction : running) {
            PropertyAnimatorProxy.stop(transaction.target.itemView);
        }
    }

    @Override
    public boolean isRunning() {
        return !running.isEmpty();
    }
}
