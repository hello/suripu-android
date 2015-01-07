package is.hello.sense.ui.widget.graphing.drawables;

import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapterCache;

public abstract class AdapterGraphDrawable extends Drawable implements GraphAdapter.ChangeObserver {
    protected final GraphAdapterCache adapterCache = new GraphAdapterCache(GraphAdapterCache.Type.PLAIN);


    //region Overrides

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    //endregion


    //region Adapter Support

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

    @Override
    public void onGraphAdapterChanged() {
        adapterCache.rebuild();
        invalidateSelf();
    }

    //endregion
}
