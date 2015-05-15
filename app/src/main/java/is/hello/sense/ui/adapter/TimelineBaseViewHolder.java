package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.ui.animation.AnimatorContext;

public abstract class TimelineBaseViewHolder extends RecyclerView.ViewHolder {
    protected TimelineBaseViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    //region Binding

    public abstract void bind(int position);

    public void unbind() {
        cancelRenderAnimation();
    }

    //endregion


    //region Animations

    public void prepareForRenderAnimation() {
        // Do nothing.
    }

    public void provideRenderAnimation(@NonNull AnimatorContext.TransactionFacade transactionFacade) {
        // Do nothing.
    }

    public void cancelRenderAnimation() {
        // Do nothing.
    }

    public void cleanUpAfterRenderAnimation() {
        // Do nothing.
    }

    //endregion
}
