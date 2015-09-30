package is.hello.sense.ui.recycler;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class InsetItemDecoration extends RecyclerView.ItemDecoration {
    private final SimpleArrayMap<Integer, Rect> insets = new SimpleArrayMap<>();

    public void addItemInset(int position, @NonNull Rect inset) {
        insets.put(position, inset);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        final int position = parent.getChildAdapterPosition(view);
        final Rect insets = this.insets.get(position);
        if (insets != null) {
            outRect.top += insets.top;
            outRect.left += insets.left;
            outRect.right += insets.right;
            outRect.bottom += insets.bottom;
        }
    }
}
