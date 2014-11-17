package is.hello.sense.ui.common;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ListView;

public final class ListViewUtil {
    /**
     * Returns the scroll Y position of a given list view.
     */
    public static int getScrollY(@NonNull ListView listView) {
        if (listView.getCount() == 0) {
            return 0;
        } else {
            View rowView = listView.getChildAt(0);
            return -rowView.getTop() + listView.getFirstVisiblePosition() * rowView.getHeight();
        }
    }
}
