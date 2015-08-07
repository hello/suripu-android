package is.hello.sense.ui.widget.timeline;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.ExtendedItemAnimator;

/**
 * A simple staggered fade-in animation.
 * <p />
 * Each item faded-in has the delay <code>{@link #DELAY} * index</code>.
 */
public class TimelineFadeItemAnimator extends ExtendedItemAnimator {
    public static final long DELAY = 20;

    private final AnimatorConfig config = AnimatorConfig.DEFAULT;

    private final List<Transaction> pending = new ArrayList<>();
    private final List<Transaction> running = new ArrayList<>();

    private boolean delayEnabled = true;

    public TimelineFadeItemAnimator(@NonNull AnimatorContext animatorContext) {
        super(animatorContext);

        setEnabled(Action.ADD, true);
    }

    public void setDelayEnabled(boolean delayEnabled) {
        this.delayEnabled = delayEnabled;
    }

    private long getDelayAmount() {
        if (delayEnabled) {
            return DELAY;
        } else {
            return 0;
        }
    }

    @Override
    public void runPendingAnimations() {
        Collections.sort(pending);
        getAnimatorContext().transaction(config, AnimatorContext.OPTIONS_DEFAULT, f -> {
            dispatchAnimationWillStart(config, f);

            final long delayAmount = getDelayAmount();
            long transactionDelay = delayAmount;
            for (Transaction transaction : pending) {
                RecyclerView.ViewHolder target = transaction.target;

                switch (transaction.action) {
                    case ADD: {
                        dispatchAddStarting(target);
                        f.animate(target.itemView)
                         .setStartDelay(transactionDelay)
                         .fadeIn();

                        break;
                    }
                    case REMOVE: {
                        dispatchRemoveStarting(target);
                        f.animate(target.itemView)
                         .setStartDelay(transactionDelay)
                         .fadeOut(View.VISIBLE);

                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("Transaction type " +
                                transaction.action + " currently unsupported.");
                    }
                }

                transactionDelay += delayAmount;
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
                            dispatchAddFinished(target);
                            break;
                        }
                        case REMOVE: {
                            target.itemView.setAlpha(0f);
                            dispatchRemoveFinished(target);
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("Transaction type " +
                                    transaction.action + " currently unsupported.");
                        }
                    }
                }
            }

            running.clear();

            dispatchAnimationDidEnd(finished);
        });
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (!isAnimatable(Action.ADD, holder)) {
            dispatchAddFinished(holder);
            return false;
        }

        holder.itemView.setAlpha(0f);
        pending.add(new Transaction(Action.ADD, holder));
        return true;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        if (!isAnimatable(Action.REMOVE, holder)) {
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
