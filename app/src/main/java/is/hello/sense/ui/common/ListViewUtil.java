package is.hello.sense.ui.common;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ListView;

public final class ListViewUtil {
    /**
     * Returns the accessible last position (index) in the list view's adapter,
     * taking into account any headers and footers contained by the list view.
     */
    public static int getLastAdapterPosition(@NonNull ListView listView) {
        return listView.getCount() - listView.getHeaderViewsCount() - listView.getFooterViewsCount() - 1;
    }

    /**
     * Adjusts a given position (index) to fit within the bounds of a list view's adapter,
     * taking into acount any headers and footers contained in the list view.
     */
    public static int getAdapterPosition(@NonNull ListView listView, int rawPosition) {
        if (rawPosition < listView.getHeaderViewsCount()) {
            return 0;
        } else if (rawPosition > getLastAdapterPosition(listView)) {
            return getLastAdapterPosition(listView);
        } else {
            return rawPosition - listView.getHeaderViewsCount();
        }
    }

    /**
     * Returns an estimate for the scroll Y position of a given list view.
     * <p/>
     * The value returned by this method will be non-sense for
     * list views with variable view heights.
     */
    public static int getEstimatedScrollY(@NonNull ListView listView) {
        if (listView.getCount() == 0) {
            return 0;
        } else {
            View rowView = listView.getChildAt(0);
            return -rowView.getTop() + listView.getFirstVisiblePosition() * rowView.getHeight();
        }
    }

    /**
     * Returns the corresponding adapter position at a given Y coordinate.
     */
    public static int getPositionForY(@NonNull ListView listView, float y) {
        View view = Views.findChildAtY(listView, y);
        int lastItem = getLastAdapterPosition(listView);
        if (view == null) {
            return lastItem;
        } else {
            int position = listView.getPositionForView(view);
            return Math.min(lastItem, position);
        }
    }
}
