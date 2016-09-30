package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Scale;
import is.hello.sense.ui.widget.graphing.drawables.CircleDrawable;
import is.hello.sense.ui.widget.util.Styles;

/**
 * View for rendering a list of {@link is.hello.sense.api.model.v2.Scale} like
 * {@link is.hello.sense.api.model.v2.sensors.Sensor#scales} from within a {@link android.widget.ScrollView}
 */
public class SensorScaleList extends LinearLayout {
    public SensorScaleList(final Context context) {
        this(context, null);
    }

    public SensorScaleList(final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SensorScaleList(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public final void renderScales(@NonNull final List<Scale> scales, @NonNull final String measuredIn) {
        removeAllViews();
        addHeaderRow(measuredIn);
        for (final Scale scale : scales) {
            inflate(getContext(), R.layout.item_circle_row, this);
            final View row = getChildAt(getChildCount() - 1);
            ((TextView) row.findViewById(R.id.item_circle_view_name)).setText(scale.getName());
            ((TextView) row.findViewById(R.id.item_circle_view_value)).setText(scale.getScaleViewValueText(getResources()));
            row.findViewById(R.id.item_circle_view_circle).setBackground(new CircleDrawable(ContextCompat.getColor(getContext(), scale.getCondition().colorRes)));
        }
    }

    private void addHeaderRow(@NonNull final String measuredIn) {
        inflate(getContext(), R.layout.item_circle_row, this);
        findViewById(R.id.item_circle_view_name).setVisibility(GONE);
        findViewById(R.id.item_circle_view_title).setVisibility(VISIBLE);
        findViewById(R.id.item_circle_view_divider).setVisibility(GONE);
        final TextView value = ((TextView) findViewById(R.id.item_circle_view_value));
        value.setText(getContext().getString(R.string.sensor_scale_list_measured_in, measuredIn));
        value.setTextSize(Styles.pxToDp(getContext().getResources().getDimensionPixelSize(R.dimen.text_h7)));
        value.setTextColor(ContextCompat.getColor(getContext(), R.color.gray3));
        ((RelativeLayout.LayoutParams) value.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    }



}
