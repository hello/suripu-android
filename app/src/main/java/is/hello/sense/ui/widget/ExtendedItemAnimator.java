package is.hello.sense.ui.widget;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.functional.Functions;
import rx.functions.Func1;

public abstract class ExtendedItemAnimator extends RecyclerView.ItemAnimator {
    private final AnimatorContext animatorContext;
    private final List<Listener> listeners = new ArrayList<>();
    private final SparseBooleanArray enabledAnimations = new SparseBooleanArray(2);
    private @Nullable Func1<RecyclerView.ViewHolder, Boolean> filter;

    protected ExtendedItemAnimator(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
        setSupportsChangeAnimations(false);
    }


    //region Attributes

    public void setFilter(@Nullable Func1<RecyclerView.ViewHolder, Boolean> filter) {
        this.filter = filter;
    }

    public void setEnabled(@NonNull Action action, boolean enabled) {
        enabledAnimations.put(action.ordinal(), enabled);
    }

    public AnimatorContext getAnimatorContext() {
        return animatorContext;
    }

    //endregion


    //region Animations

    protected boolean isAnimatable(@NonNull Action action, @NonNull RecyclerView.ViewHolder holder) {
        return (enabledAnimations.get(action.ordinal()) &&
                (filter == null || filter.call(holder)));
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        dispatchAddFinished(holder);
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
                                 RecyclerView.ViewHolder newHolder,
                                 int fromLeft,
                                 int fromTop,
                                 int toLeft,
                                 int toTop) {
        dispatchChangeFinished(oldHolder, true);
        return false;
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        dispatchMoveFinished(holder);
        return false;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        dispatchRemoveFinished(holder);
        return false;
    }

    //endregion


    //region Listener

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    protected void dispatchAnimationWillStart(@NonNull AnimatorContext.Transaction transaction) {
        for (Listener listener : listeners) {
            listener.onItemAnimatorWillStart(transaction);
        }
    }

    protected void dispatchAnimationDidEnd(boolean finished) {
        dispatchAnimationsFinished();
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).onItemAnimatorDidStop(finished);
        }
    }

    public interface Listener {
        void onItemAnimatorWillStart(@NonNull AnimatorContext.Transaction transaction);
        void onItemAnimatorDidStop(boolean finished);
    }

    //endregion


    //region Transactions

    protected static final class Transaction implements Comparable<Transaction> {
        public final Action action;
        public final RecyclerView.ViewHolder target;

        public Transaction(@NonNull Action action,
                           @NonNull RecyclerView.ViewHolder target) {
            this.action = action;
            this.target = target;
        }

        @Override
        public int compareTo(@NonNull Transaction another) {
            return Functions.compareInts(target.getAdapterPosition(),
                    another.target.getAdapterPosition());
        }
    }

    public enum Action {
        ADD,
        REMOVE,
        CHANGE,
        MOVE,
    }

    //endregion
}
