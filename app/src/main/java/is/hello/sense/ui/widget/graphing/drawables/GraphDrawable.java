package is.hello.sense.ui.widget.graphing.drawables;

import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapterCache;

public abstract class GraphDrawable extends Drawable implements GraphAdapter.ChangeObserver {
    protected final GraphAdapterCache adapterCache = new GraphAdapterCache(GraphAdapterCache.Type.PLAIN);
    protected final List<GraphAdapter.ChangeObserver> observers = new ArrayList<>();

    protected int topInset = 0, bottomInset = 0;

    //region Overrides

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    //endregion


    //region Adapter Support

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

    public void registerObserver(@NonNull GraphAdapter.ChangeObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(@NonNull GraphAdapter.ChangeObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void onGraphAdapterChanged() {
        adapterCache.rebuild();
        invalidateSelf();

        for (GraphAdapter.ChangeObserver observer : observers) {
            observer.onGraphAdapterChanged();
        }
    }

    //endregion
}
