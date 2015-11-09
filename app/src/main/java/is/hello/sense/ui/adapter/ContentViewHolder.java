package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class ContentViewHolder extends RecyclerView.ViewHolder {
    private int contentPosition = RecyclerView.NO_POSITION;

    protected ContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    final void setContentPosition(int contentPosition) {
        this.contentPosition = contentPosition;
    }

    public final int getContentPosition() {
        return contentPosition;
    }
}
