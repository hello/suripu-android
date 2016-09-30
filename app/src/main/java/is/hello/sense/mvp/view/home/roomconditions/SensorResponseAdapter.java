package is.hello.sense.mvp.view.home.roomconditions;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;

public class SensorResponseAdapter extends ArrayRecyclerAdapter<Sensor, SensorResponseAdapter.BaseViewHolder> {
    private static final int VIEW_SENSOR = 0;
    private static final int VIEW_ID_MESSAGE = 1;
    private static final int VIEW_AIR_QUALITY = 2;


    private final LayoutInflater inflater;
    private final int graphHeight;
    private boolean messageWantsSenseIcon;
    /**
     * Store air quality sensors within here
     */
    private final List<Sensor> airQualitySensors = new ArrayList<>();
    /**
     * Avoids having to check if an air quality sensor exists when getting the item count.
     */
    private boolean hasAirQuality = false;

    @StringRes
    private int messageTitle;
    @Nullable
    private CharSequence messageBody;
    @StringRes
    private int messageActionTitle;
    @Nullable
    private View.OnClickListener messageActionOnClick;

    private final UnitFormatter unitFormatter;

    public SensorResponseAdapter(@NonNull final LayoutInflater inflater,
                                 @NonNull final UnitFormatter unitFormatter) {
        super(new ArrayList<>());
        this.unitFormatter = unitFormatter;
        this.inflater = inflater;
        this.graphHeight = this.inflater.getContext().getResources().getDimensionPixelSize(R.dimen.sensor_graph_height);
    }

    //region adapter overrides
    @Override
    public int getItemViewType(final int position) {
        if (airQualitySensors.isEmpty()) {
            return VIEW_SENSOR;
        }
        if (position == getItemCount() - 1) {
            return VIEW_AIR_QUALITY;
        }
        return VIEW_SENSOR;
    }

    @Override
    public int getItemCount() {
        if (hasAirQuality && !airQualitySensors.isEmpty()) {
            return super.getItemCount() + 1;
        }
        return super.getItemCount();
    }

    public void replaceAll(@NonNull final List<Sensor> sensors) {
        airQualitySensors.clear();
        hasAirQuality = false;
        final ArrayList<Sensor> normalSensors = new ArrayList<>();
        for (final Sensor sensor : sensors) {
            switch (sensor.getType()) {
                case PARTICULATES:
                case CO2:
                case TVOC:
                    hasAirQuality = true;
                    airQualitySensors.add(sensor);
                    break;
                default:
                    normalSensors.add(sensor);
            }
        }

        // If there is only one AirQuality item we will display it with a graph.
        if (airQualitySensors.size() == 1) {
            normalSensors.add(airQualitySensors.get(0));
            airQualitySensors.clear();
        }
        super.replaceAll(normalSensors);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case VIEW_ID_MESSAGE:
                final View view = this.inflater.inflate(R.layout.item_message_card, parent, false);

                final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
                if (this.messageTitle != 0) {
                    title.setText(this.messageTitle);
                    title.setVisibility(View.VISIBLE);
                } else {
                    title.setVisibility(View.GONE);
                }

                final ImageView image = (ImageView) view.findViewById(R.id.item_message_card_image);
                if (this.messageWantsSenseIcon) {
                    image.setImageResource(R.drawable.illustration_no_sense);
                    image.setVisibility(View.VISIBLE);
                } else {
                    image.setVisibility(View.GONE);
                }

                final TextView messageText = (TextView) view.findViewById(R.id.item_message_card_message);
                messageText.setText(this.messageBody);

                final Button action = (Button) view.findViewById(R.id.item_message_card_action);
                action.setText(this.messageActionTitle);
                if (this.messageActionOnClick != null) {
                    Views.setSafeOnClickListener(action, this.messageActionOnClick);
                }
                return new BaseViewHolder(view);
            case VIEW_SENSOR:
                return new SensorViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_sensor_response, parent, false));
            case VIEW_AIR_QUALITY:
                return new AirQualityViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_sensor_response, parent, false));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    //endregion

    //region adapter helpers

    public void displayMessage(final boolean messageWantsSenseIcon,
                               @StringRes final int title,
                               @NonNull final CharSequence message,
                               @StringRes final int actionTitle,
                               @NonNull final View.OnClickListener actionOnClick) {
        clear();
        this.messageWantsSenseIcon = messageWantsSenseIcon;
        this.messageTitle = title;
        this.messageBody = message;
        this.messageActionTitle = actionTitle;
        this.messageActionOnClick = actionOnClick;

        notifyDataSetChanged();
    }

    public void dismissMessage() {
        this.messageTitle = 0;
        this.messageBody = null;
        this.messageActionTitle = 0;
        this.messageActionOnClick = null;
    }

    public void release() {
        replaceAll(new ArrayList<>());
        notifyDataSetChanged();
    }

    //endregion


    public class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        protected final TextView title;
        protected final TextView body;
        protected final TextView value;
        protected final TextView descriptor;
        protected final View view;
        protected final SensorGraphView graphView;

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.view = itemView;
            this.title = (TextView) itemView.findViewById(R.id.item_server_response_title);
            this.body = (TextView) itemView.findViewById(R.id.item_server_response_body);
            this.value = (TextView) itemView.findViewById(R.id.item_server_response_value);
            this.descriptor = (TextView) itemView.findViewById(R.id.item_server_response_descriptor);
            this.graphView = (SensorGraphView) itemView.findViewById(R.id.item_server_response_graph);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final Sensor sensor = getItem(position);
            this.title.setText(sensor.getName());
            this.body.setText(sensor.getMessage());
            this.value.setText(SensorResponseAdapter.this.unitFormatter.getUnitPrinterForSensorAverageValue(sensor.getType()).print(sensor.getValue()));
            if (sensor.getType() == SensorType.TEMPERATURE || sensor.getType() == SensorType.HUMIDITY) {
                this.descriptor.setText(null);
            } else {
                this.descriptor.setText(SensorResponseAdapter.this.unitFormatter.getSuffixForSensor(sensor.getType()));
            }
            this.value.setTextColor(ContextCompat.getColor(inflater.getContext(), sensor.getColor()));
        }
    }

    private class AirQualityViewHolder extends BaseViewHolder {
        private final LinearLayout root;

        public AirQualityViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.root = (LinearLayout) itemView.findViewById(R.id.item_server_response_root);
            this.title.setText(itemView.getContext().getString(R.string.air_quality));
            graphView.setVisibility(View.GONE);
            value.setVisibility(View.GONE);
            descriptor.setVisibility(View.GONE);
        }

        @Override
        public void bind(final int position) {
            final AirQualityCard airQualityCard = new AirQualityCard(view.getContext());
            airQualityCard.setUnitFormatter(unitFormatter);
            airQualityCard.replaceAll(airQualitySensors);
            body.setText(airQualityCard.getWorstMessage());
            if (root.getChildCount() < 3) {
                root.addView(airQualityCard);
            }
        }
    }


    private class SensorViewHolder extends BaseViewHolder implements Drawable.Callback {

        public SensorViewHolder(@NonNull final View itemView) {
            super(itemView);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final Sensor sensor = getItem(position);
            this.graphView.setSensorGraphDrawable(new SensorGraphDrawable(SensorResponseAdapter.this.inflater.getContext(),
                                                                          sensor,
                                                                          unitFormatter,
                                                                          SensorResponseAdapter.this.graphHeight));
            this.view.setOnClickListener(v -> SensorResponseAdapter.this.dispatchItemClicked(position));
        }

        @Override
        public void invalidateDrawable(final Drawable who) {
            this.view.invalidate();
            this.view.requestLayout();
        }

        @Override
        public void scheduleDrawable(final Drawable who, final Runnable what, final long when) {
            invalidateDrawable(who);
        }

        @Override
        public void unscheduleDrawable(final Drawable who, final Runnable what) {

        }
    }

}
