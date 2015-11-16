package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.view.View;

public abstract class TimelineBaseViewHolder extends ContentViewHolder {
    protected TimelineBaseViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    //region Binding

    public abstract void bind(int position);

    //endregion
}
