package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class TimelineBaseViewHolder extends RecyclerView.ViewHolder {
    protected TimelineBaseViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    //region Binding

    public abstract void bind(int position);

    public void unbind() {
        // Do nothing.
    }

    //endregion
}
