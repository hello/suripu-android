package is.hello.sense.mvp.util;

import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Interface that determines ways to manipulate a {@link android.support.design.widget.FloatingActionButton}
 * that belongs in a parent container
 */

public interface FabPresenter {

    void setFabVisible(final boolean visible);

    void updateFab(@DrawableRes final int iconRes,
                   @Nullable final View.OnClickListener listener);

    void setFabLoading(final boolean loading);
}
