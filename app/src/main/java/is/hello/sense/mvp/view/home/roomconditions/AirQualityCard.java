package is.hello.sense.mvp.view.home.roomconditions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.ui.activities.SensorDetailActivity;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;

public class AirQualityCard extends LinearLayout {
    private final List<Sensor> sensors = new ArrayList<>();
    private UnitFormatter unitFormatter;
    private OnRowItemClickListener onRowClickListener;

    public AirQualityCard(@NonNull final Context context) {
        this(context, null);

    }

    public AirQualityCard(@NonNull final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public AirQualityCard(@NonNull final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public final void replaceAll(@NonNull final List<Sensor> sensors) {
        this.sensors.clear();
        this.sensors.addAll(sensors);
        renderSensors();
    }

    public void setUnitFormatter(@NonNull final UnitFormatter unitFormatter) {
        this.unitFormatter = unitFormatter;
    }

    public void setOnRowClickListener(@NonNull final OnRowItemClickListener listener) {
        this.onRowClickListener = listener;
    }

    public final void renderSensors() {
        removeAllViews();
        for (final Sensor sensor : sensors) {
            inflate(getContext(), R.layout.item_chevron_row, this);
            final View row = getChildAt(getChildCount() - 1);
            ((TextView) row.findViewById(R.id.item_chevron_view_name)).setText(sensor.getName());
            if (sensor.getValue() == null) {
                ((TextView) row.findViewById(R.id.item_chevron_view_value)).setText(R.string.missing_data_placeholder);
            } else {
                if (unitFormatter == null) {
                    ((TextView) row.findViewById(R.id.item_chevron_view_value)).setText(formatValue(sensor.getValue(), Constants.EMPTY_STRING));
                } else {
                    ((TextView) row.findViewById(R.id.item_chevron_view_value)).setText(formatValue(sensor.getValue(), unitFormatter.getSuffixForSensor(sensor.getType())));
                }
            }
            ((TextView) row.findViewById(R.id.item_chevron_view_value)).setTextColor(ContextCompat.getColor(getContext(), sensor.getColor()));
            row.setOnClickListener(v -> {
                if (onRowClickListener != null) {
                    onRowClickListener.onClick(sensor);
                }
            });
        }
        if (getChildCount() > 0) {
            final View row = getChildAt(getChildCount() - 1);
            row.findViewById(R.id.item_chevron_view_divider).setVisibility(GONE);
        }
    }

    private String formatValue(@NonNull final Double value, @NonNull final String suffix) {
        return String.format("%.0f %s", value, suffix);
    }

    public interface OnRowItemClickListener {
        void onClick(@NonNull final Sensor sensor);
    }

}
