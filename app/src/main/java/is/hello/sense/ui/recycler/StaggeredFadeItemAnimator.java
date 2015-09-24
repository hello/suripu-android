package is.hello.sense.ui.recycler;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;

/**
 * A simple staggered fade-in animation.
 * <p />
 * Each item faded-in has the delay <code>{@link #DELAY} * index</code>.
 */
public class StaggeredFadeItemAnimator extends ExtendedItemAnimator {
    public static final long DELAY = 20;

    private final List<Transaction> pending = new ArrayList<>();
    private final List<Transaction> running = new ArrayList<>();

    private AnimatorTemplate template = AnimatorTemplate.DEFAULT;
    private boolean delayEnabled = true;

    public StaggeredFadeItemAnimator(@NonNull AnimatorContext animatorContext) {
        super(animatorContext);

        setEnabled(Action.ADD, true);
    }

    public void setDelayEnabled(boolean delayEnabled) {
        this.delayEnabled = delayEnabled;
    }

    public void setTemplate(@NonNull AnimatorTemplate template) {
        this.template = template;
    }

    public @NonNull AnimatorTemplate getTemplate() {
        return template;
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
        getAnimatorContext().transaction(template, AnimatorContext.OPTIONS_DEFAULT, t -> {
            dispatchAnimationWillStart(t);

            final long delayAmount = getDelayAmount();
            long transactionDelay = 0;
            for (Transaction transaction : pending) {
                RecyclerView.ViewHolder target = transaction.target;

                switch (transaction.action) {
                    case ADD: {
                        dispatchAddStarting(target);
                        t.animatorFor(target.itemView)
                         .withStartDelay(transactionDelay)
                         .fadeIn();

                        break;
                    }
                    case REMOVE: {
                        dispatchRemoveStarting(target);
                        t.animatorFor(target.itemView)
                         .withStartDelay(transactionDelay)
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
        Anime.cancelAll(item.itemView);
    }

    @Override
    public void endAnimations() {
        pending.clear();
        for (Transaction transaction : running) {
            Anime.cancelAll(transaction.target.itemView);
        }
    }

    @Override
    public boolean isRunning() {
        return !running.isEmpty();
    }
}
