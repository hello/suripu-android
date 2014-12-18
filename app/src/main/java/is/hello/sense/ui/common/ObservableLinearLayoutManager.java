package is.hello.sense.ui.common;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class ObservableLinearLayoutManager extends LinearLayoutManager {
    private @Nullable Runnable onPostLayout;

    public ObservableLinearLayoutManager(Context context) {
        super(context);
    }

    public ObservableLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }


    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);

        if (onPostLayout != null) {
            onPostLayout.run();
        }
    }


    public void setOnPostLayout(@Nullable Runnable onPostLayout) {
        this.onPostLayout = onPostLayout;
    }
}
