package is.hello.sense.ui.recycler;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;

public class StaggeredSlideItemAnimator extends ExtendedItemAnimator {
    private static final long DELAY = 20;

    private final List<Transaction> pending = new ArrayList<>();
    private final List<Transaction> running = new ArrayList<>();

    public StaggeredSlideItemAnimator(@NonNull AnimatorContext animatorContext) {
        super(animatorContext);

        setEnabled(Action.ADD, true);
        setEnabled(Action.REMOVE, true);
    }

    @Override
    public void runPendingAnimations() {
        Collections.sort(pending);
        getAnimatorContext().transaction(null, 0, t -> {
            dispatchAnimationWillStart(t);

            long delay = 0;
            for (Transaction transaction : pending) {
                RecyclerView.ViewHolder target = transaction.target;

                switch (transaction.action) {
                    case ADD: {
                        dispatchAddStarting(target);
                        t.animatorFor(target.itemView)
                         .withStartDelay(delay)
                         .translationX(0f);

                        break;
                    }
                    case REMOVE: {
                        dispatchRemoveStarting(target);
                        t.animatorFor(target.itemView)
                         .withStartDelay(delay)
                         .translationX(-target.itemView.getMeasuredWidth());

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
                switch (transaction.action) {
                    case ADD: {
                        target.itemView.setTranslationX(0f);
                        dispatchAddFinished(target);
                        break;
                    }
                    case REMOVE: {
                        target.itemView.setTranslationX(-target.itemView.getMeasuredWidth());
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

        holder.itemView.setTranslationX(holder.itemView.getMeasuredWidth());
        pending.add(new Transaction(Action.ADD, holder));
        return true;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        if (!isAnimatable(Action.REMOVE, holder)) {
            dispatchRemoveFinished(holder);
            return false;
        }

        holder.itemView.setTranslationX(0f);
        pending.add(new Transaction(Action.REMOVE, holder));
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        Anime.cancelAll(item.itemView);
    }

    @Override
    public void endAnimations() {
        for (int i = running.size() - 1; i >= 0; i--) {
            Anime.cancelAll(running.get(i).target.itemView);
        }
        running.clear();
        pending.clear();
    }

    @Override
    public boolean isRunning() {
        return (running.size() > 0);
    }
}
