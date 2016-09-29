package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
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
    }

    public final void renderScales(@NonNull final List<Scale> scales, @NonNull final String measuredIn) {
        removeAllViews();
        addHeaderRow(measuredIn);
        for (final Scale scale : scales) {
            inflate(getContext(), R.layout.item_scale_view, this);
            final View row = getChildAt(getChildCount() - 1);
            ((TextView) row.findViewById(R.id.item_scale_view_name)).setText(scale.getName());
            ((TextView) row.findViewById(R.id.item_scale_view_value)).setText(scale.getScaleViewValueText(getResources()));
            row.findViewById(R.id.item_scale_view_circle).setBackground(new CircleDrawable(ContextCompat.getColor(getContext(), scale.getCondition().colorRes)));
        }
    }

    private void addHeaderRow(@NonNull final String measuredIn) {
        inflate(getContext(), R.layout.item_scale_view, this);
        findViewById(R.id.item_scale_view_name).setVisibility(GONE);
        findViewById(R.id.item_scale_view_title).setVisibility(VISIBLE);
        findViewById(R.id.item_scale_view_divider).setVisibility(GONE);
        final TextView value = ((TextView) findViewById(R.id.item_scale_view_value));
        value.setText(getContext().getString(R.string.sensor_scale_list_measured_in, measuredIn));
        value.setTextSize(Styles.pxToDp(getContext().getResources().getDimensionPixelSize(R.dimen.text_h7)));
        value.setTextColor(ContextCompat.getColor(getContext(), R.color.gray3));
        ((RelativeLayout.LayoutParams) value.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        // return row;
    }

    private class CircleDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CircleDrawable(final int color) {
            this.paint.setColor(color);
        }

        @Override
        public void draw(final Canvas canvas) {
            canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, canvas.getHeight() / 2, paint);
        }

        @Override
        public void setAlpha(final int alpha) {

        }

        @Override
        public void setColorFilter(final ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return 0;
        }
    }

}
