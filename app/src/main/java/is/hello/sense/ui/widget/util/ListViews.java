package is.hello.sense.ui.widget.util;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

public final class ListViews {
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
     * The value returned by this method will be non-onboarding_pair_sense for
     * list views with variable view heights.
     */
    public static int getEstimatedScrollY(@NonNull AbsListView listView) {
        if (listView.getChildCount() == 0) {
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


    public static void setTouchAndScrollListener(@NonNull ListView listView, @NonNull TouchAndScrollListener listener) {
        listView.setOnScrollListener(listener);
        listView.setOnTouchListener(listener);
    }

    public static abstract class TouchAndScrollListener implements AbsListView.OnScrollListener, View.OnTouchListener {
        private int previousState = SCROLL_STATE_IDLE;

        public abstract void onScroll(AbsListView absListView, int firstVisiblePosition, int visibleItemCount, int totalItemCount);
        public final void onScrollStateChanged(AbsListView absListView, int newState) {
            onScrollStateChanged(absListView, previousState, newState);
            this.previousState = newState;
        }

        protected abstract void onScrollStateChanged(@NonNull AbsListView absListView, int oldState, int newState);

        protected abstract void onTouchDown(@NonNull AbsListView absListView);
        protected abstract void onTouchUp(@NonNull AbsListView absListView);

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    onTouchDown((AbsListView) view);
                    break;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    onTouchUp((AbsListView) view);
                    break;
                }

                default: {
                    break;
                }
            }

            return false;
        }
    }
}
