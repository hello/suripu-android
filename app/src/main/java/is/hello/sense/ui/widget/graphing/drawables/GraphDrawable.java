package is.hello.sense.ui.widget.graphing.drawables;

import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapterCache;

public abstract class GraphDrawable extends Drawable implements GraphAdapter.ChangeObserver {
    protected final GraphAdapterCache adapterCache = new GraphAdapterCache();
    protected int topInset = 0, bottomInset = 0;


    //region Properties

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setTintColor(int color) {
        setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public void setTopInset(int topInset) {
        this.topInset = topInset;
        invalidateSelf();
    }

    public void setBottomInset(int bottomInset) {
        this.bottomInset = bottomInset;
        invalidateSelf();
    }

    public void setAdapter(@Nullable GraphAdapter adapter) {
        if (adapter == adapterCache.getAdapter()) {
            return;
        }

        if (adapterCache.getAdapter() != null) {
            adapterCache.getAdapter().unregisterObserver(this);
        }

        adapterCache.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerObserver(this);
        }

        invalidateSelf();
    }

    public @Nullable GraphAdapter getAdapter() {
        return adapterCache.getAdapter();
    }

    public GraphAdapterCache getAdapterCache() {
        return adapterCache;
    }

    @Override
    public void onGraphAdapterChanged() {
        adapterCache.rebuild();
        invalidateSelf();
    }

    //endregion
}
