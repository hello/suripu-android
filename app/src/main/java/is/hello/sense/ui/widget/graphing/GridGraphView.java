package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import is.hello.sense.R;

public class GridGraphView extends LinearLayout {
    private final LayoutParams rowLayoutParams;
    private final LayoutParams cellLayoutParams;
    private final int interRowPadding;

    private @Nullable Adapter adapter;

    public GridGraphView(@NonNull Context context) {
        this(context, null);
    }

    public GridGraphView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridGraphView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = getResources();

        setOrientation(VERTICAL);
        setShowDividers(SHOW_DIVIDER_MIDDLE);

        this.rowLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.cellLayoutParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        this.interRowPadding = resources.getDimensionPixelSize(R.dimen.gap_small);
    }

    private LinearLayout createRow(@NonNull Context context) {
        final LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(HORIZONTAL);
        rowLayout.setGravity(Gravity.CENTER);
        return rowLayout;
    }

    private void updateViews() {
        removeAllViews();

        final Context context = getContext();
        final int sectionCount = adapter.getSectionCount();
        final int lastSection = sectionCount - 1;
        for (int section = 0; section < sectionCount; section++) {
            final LinearLayout row = createRow(context);
            final int itemCount = adapter.getSectionItemCount(section);
            for (int item = 0; item < itemCount; item++) {
                final GridDataPointView pointView = new GridDataPointView(context);
                pointView.setValue(adapter.getValueReading(section, item));
                pointView.setFillColor(adapter.getValueColor(section, item));
                row.addView(pointView, cellLayoutParams);
            }

            if (section != lastSection) {
                row.setPadding(0, 0, 0, interRowPadding);
            } else {
                row.setPadding(0, 0, 0, 0);
            }

            addView(row, rowLayoutParams);
        }
    }

    public void setAdapter(@Nullable Adapter adapter) {
        this.adapter = adapter;
        updateViews();
    }

    public interface Adapter {
        int getSectionCount();
        int getSectionItemCount(int section);
        String getValueReading(int section, int item);
        @ColorInt int getValueColor(int section, int item);
        boolean isValueHighlighted(int section, int item);
    }
}
