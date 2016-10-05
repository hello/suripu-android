package is.hello.sense.mvp.view.home.roomconditions;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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
    private static final int VIEW_WELCOME_CARD = 3;
    private static final int VIEW_SENSE_MISSING = 4;


    private final LayoutInflater inflater;
    private final int graphHeight;
    private boolean messageWantsSenseIcon;
    private boolean showWelcomeCard = false;
    private boolean showSenseMissingCard = false;
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
    private ErrorItemClickListener errorItemClickListener;

    public interface ErrorItemClickListener {
        void onErrorItemClicked();
    }

    public void setErrorItemClickListener(@Nullable final ErrorItemClickListener listener) {
        this.errorItemClickListener = listener;
    }

    public SensorResponseAdapter(@NonNull final LayoutInflater inflater,
                                 @NonNull final UnitFormatter unitFormatter) {
        super(new ArrayList<>());
        this.unitFormatter = unitFormatter;
        this.inflater = inflater;
        this.graphHeight = this.inflater.getContext().getResources().getDimensionPixelSize(R.dimen.sensor_graph_height);
    }

    //region adapter overrides
    @Override
    public Sensor getItem(final int position) {
        if (this.messageBody != null || this.showSenseMissingCard) {
            return super.getItem(position);
        }
        return super.getItem(position - (this.showWelcomeCard ? 1 : 0));
    }

    @Override
    public int getItemViewType(final int position) {
        if (this.messageBody != null) {
            return VIEW_ID_MESSAGE;
        } else if (this.showSenseMissingCard) {
            return VIEW_SENSE_MISSING;
        } else if (this.showWelcomeCard && position == 0) {
            return VIEW_WELCOME_CARD;
        }
        if (this.airQualitySensors.isEmpty()) {
            return VIEW_SENSOR;
        }
        if (position == getItemCount() - 1) {
            return VIEW_AIR_QUALITY;
        }
        return VIEW_SENSOR;
    }

    @Override
    public int getItemCount() {
        if (this.messageBody != null || this.showSenseMissingCard) {
            return 1;
        }
        final int welcomeCardCount = this.showWelcomeCard ? 1 : 0;
        if (this.hasAirQuality && !this.airQualitySensors.isEmpty()) {
            return super.getItemCount() + 1 + welcomeCardCount;
        }
        return super.getItemCount() + welcomeCardCount;
    }

    public void replaceAll(@NonNull final List<Sensor> sensors) {
        dismissMessage();
        this.airQualitySensors.clear();
        this.hasAirQuality = false;
        final ArrayList<Sensor> normalSensors = new ArrayList<>();
        for (final Sensor sensor : sensors) {
            switch (sensor.getType()) {
                case PARTICULATES:
                case CO2:
                case TVOC:
                    this.hasAirQuality = true;
                    this.airQualitySensors.add(sensor);
                    break;
                default:
                    normalSensors.add(sensor);
            }
        }

        // If there is only one AirQuality item we will display it with a graph.
        if (this.airQualitySensors.size() == 1) {
            normalSensors.add(this.airQualitySensors.get(0));
            this.airQualitySensors.clear();
        }
        super.replaceAll(normalSensors);
        notifyDataSetChanged();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case VIEW_ID_MESSAGE:
            case VIEW_SENSE_MISSING:
                return new ErrorViewHolder(this.inflater.inflate(R.layout.item_message_card, parent, false));
            case VIEW_SENSOR:
                return new SensorViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_sensor_response, parent, false));
            case VIEW_AIR_QUALITY:
                return new AirQualityViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_sensor_response, parent, false));
            case VIEW_WELCOME_CARD:
                return new BaseViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_room_conditions_welcome, parent, false));
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
    public void showSenseMissingCard() {
        clear();
        dismissMessage();
        this.showWelcomeCard = false;
        this.showSenseMissingCard = true;
        notifyDataSetChanged();
    }

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
        this.showSenseMissingCard = false;
        this.messageTitle = 0;
        this.messageBody = null;
        this.messageActionTitle = 0;
        this.messageActionOnClick = null;
    }

    public void showWelcomeCard(final boolean show) {
        this.showWelcomeCard = show;
        notifyDataSetChanged();
    }

    public void release() {
        replaceAll(new ArrayList<>());
        notifyDataSetChanged();
    }

    private void dispatchErrorItemClicked() {
        if (this.errorItemClickListener != null) {
            this.errorItemClickListener.onErrorItemClicked();
        }
    }

    //endregion


    public class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    private class ErrorViewHolder extends BaseViewHolder {
        final View view;
        final TextView titleTextView;
        final TextView messageTextView;
        final ImageView imageView;
        final Button button;

        public ErrorViewHolder(@NonNull final View view) {
            super(view);
            this.titleTextView = (TextView) view.findViewById(R.id.item_message_card_title);
            this.messageTextView = (TextView) view.findViewById(R.id.item_message_card_message);
            this.view = view;
            this.imageView = (ImageView) view.findViewById(R.id.item_message_card_image);
            this.button = (Button) view.findViewById(R.id.item_message_card_action);
            if (SensorResponseAdapter.this.showSenseMissingCard) {
                this.imageView.setImageResource(R.drawable.illustration_no_sense);
                this.messageTextView.setText(R.string.error_room_conditions_no_sense);
                this.button.setText(R.string.action_pair_new_sense);
                this.button.setOnClickListener(v -> SensorResponseAdapter.this.dispatchErrorItemClicked());

            }
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            if (!SensorResponseAdapter.this.showSenseMissingCard) {
                if (SensorResponseAdapter.this.messageTitle != 0) {
                    this.titleTextView.setText(SensorResponseAdapter.this.messageTitle);
                    this.titleTextView.setVisibility(View.VISIBLE);
                } else {
                    this.titleTextView.setVisibility(View.GONE);
                }

                final ImageView image = (ImageView) this.view.findViewById(R.id.item_message_card_image);
                if (SensorResponseAdapter.this.messageWantsSenseIcon) {
                    image.setImageResource(R.drawable.illustration_no_sense);
                    image.setVisibility(View.VISIBLE);
                } else {
                    image.setVisibility(View.GONE);
                }

                final TextView messageText = (TextView) this.view.findViewById(R.id.item_message_card_message);
                messageText.setText(SensorResponseAdapter.this.messageBody);

                final Button action = (Button) this.view.findViewById(R.id.item_message_card_action);
                action.setText(SensorResponseAdapter.this.messageActionTitle);
                if (SensorResponseAdapter.this.messageActionOnClick != null) {
                    Views.setSafeOnClickListener(action, SensorResponseAdapter.this.messageActionOnClick);
                }
            }
        }
    }

    private class SensorBaseViewHolder extends BaseViewHolder {

        protected final TextView title;
        protected final TextView body;
        protected final TextView value;
        protected final TextView descriptor;
        protected final View view;
        protected final SensorGraphView graphView;

        public SensorBaseViewHolder(@NonNull final View itemView) {
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

    private class AirQualityViewHolder extends SensorBaseViewHolder {
        private final LinearLayout root;
        final AirQualityCard airQualityCard = new AirQualityCard(view.getContext());

        public AirQualityViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.root = (LinearLayout) itemView.findViewById(R.id.item_server_response_root);
            this.root.setClickable(false);
            this.title.setText(itemView.getContext().getString(R.string.air_quality));
            this.graphView.setVisibility(View.GONE);
            this.value.setVisibility(View.GONE);
            this.descriptor.setVisibility(View.GONE);
            this.airQualityCard.setClickable(true);
            this.airQualityCard.setOnRowClickListener(sensor -> SensorResponseAdapter.this.dispatchItemClicked(0, sensor));
            this.airQualityCard.setUnitFormatter(SensorResponseAdapter.this.unitFormatter);

            this.airQualityCard.replaceAll(SensorResponseAdapter.this.airQualitySensors);
            this.body.setText(getWorstMessage(SensorResponseAdapter.this.airQualitySensors));
            if (this.root.getChildCount() < 3) {
                this.root.addView(this.airQualityCard);
            }

        }

        @Override
        public void bind(final int position) {

        }

        @Nullable
        public String getWorstMessage(@NonNull final List<Sensor> sensors) {
            if (sensors.isEmpty()) {
                return null;
            }
            Sensor worstCondition = sensors.get(0);
            for (int i = 1; i < sensors.size(); i++) {
                if (worstCondition.hasBetterConditionThan(sensors.get(i))) {
                    worstCondition = sensors.get(i);
                }
            }
            return worstCondition.getMessage();
        }
    }

    private class SensorViewHolder extends SensorBaseViewHolder implements Drawable.Callback {

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
