package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Extends the {@code RecyclerView.ViewHolder} to provide support for parallax effects.
 */
public abstract class ParallaxRecyclerViewHolder extends RecyclerView.ViewHolder {
    protected ParallaxRecyclerViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    /**
     * Sets the parallax offset for the view holder. This method will be called many times
     * when the user scrolls the containing recycler view, so it should be as fast as possible.
     *
     * @param percent The offset percentage, a value between {@code -1.0} and {@code 1.0} inclusive.
     */
    public abstract void setParallaxPercent(float percent);
}
