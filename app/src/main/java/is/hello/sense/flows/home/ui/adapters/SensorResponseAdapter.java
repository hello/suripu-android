package is.hello.sense.flows.home.ui.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.databinding.ItemSensorGroupBinding;
import is.hello.sense.databinding.ItemWelcomeCardBinding;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.ImageTextView;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;

public class SensorResponseAdapter extends ArrayRecyclerAdapter<Sensor, SensorResponseAdapter.BaseViewHolder> {
    public static final int VIEW_SENSOR = 0;
    public static final int VIEW_ID_MESSAGE = 1;
    public static final int VIEW_SENSOR_GROUP = 2;
    public static final int VIEW_WELCOME_CARD = 3;


    private final LayoutInflater inflater;
    private final int graphHeight;
    private boolean messageWantsSenseIcon;
    private boolean showWelcomeCard = false;
    /**
     * Groups lists of sensors mapped to the index of first sensor in normal sensor list
     */
    private final SparseArray<List<Sensor>> sensorGroupArray = new SparseArray<>();

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
        if (this.messageBody != null) {
            return super.getItem(position);
        }
        return super.getItem(position - (this.showWelcomeCard ? 1 : 0));
    }

    @Override
    public int getItemViewType(final int position) {
        if (this.messageBody != null) {
            return VIEW_ID_MESSAGE;
        } else if (this.showWelcomeCard && position == 0) {
            return VIEW_WELCOME_CARD;
        } else if (this.sensorGroupArray.get(position) != null) {
            return VIEW_SENSOR_GROUP;
        }
        return VIEW_SENSOR;
    }

    @Override
    public int getItemCount() {
        if (this.messageBody != null) {
            return 1;
        }
        final int welcomeCardCount = this.showWelcomeCard ? 1 : 0;

        return super.getItemCount() + welcomeCardCount;
    }

    public void replaceAll(@NonNull final List<Sensor> sensors) {
        dismissMessage();
        int firstAirQualitySensorPosition = RecyclerView.NO_POSITION;
        this.sensorGroupArray.clear();
        final ArrayList<Sensor> normalSensors = new ArrayList<>();
        for (final Sensor sensor : sensors) {
            switch (sensor.getType()) {
                case PARTICULATES:
                case CO2:
                case TVOC:
                    if (firstAirQualitySensorPosition == RecyclerView.NO_POSITION) {
                        firstAirQualitySensorPosition = normalSensors.size();
                        normalSensors.add(sensor);
                    }
                    addSensorToGroupAt(firstAirQualitySensorPosition, sensor);
                    break;
                default:
                    normalSensors.add(sensor);
            }
        }
        this.removeSingleSensorGroupsAt(firstAirQualitySensorPosition);
        super.replaceAll(normalSensors);
    }

    private void addSensorToGroupAt(final int key,
                                    @NonNull final Sensor sensor) {
        final List<Sensor> group = this.sensorGroupArray.get(key, new ArrayList<>());
        group.add(sensor);
        this.sensorGroupArray.put(key, group);
    }

    private void removeSingleSensorGroupsAt(@NonNull final int... keys) {
        for (final int key : keys) {
            final List<Sensor> sensors = this.sensorGroupArray.get(key);
            if (sensors != null && sensors.size() <= 1) {
                this.sensorGroupArray.remove(key);
            }
        }
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case VIEW_ID_MESSAGE:
                return new ErrorViewHolder(this.inflater.inflate(R.layout.item_message_card, parent, false));
            case VIEW_SENSOR:
                return new SensorViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_sensor_response, parent, false));
            case VIEW_SENSOR_GROUP:
                return new SensorGroupViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_sensor_group, parent, false));
            case VIEW_WELCOME_CARD:
                return new WelcomeCardViewHolder(SensorResponseAdapter.this.inflater.inflate(R.layout.item_welcome_card, parent, false));
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
    public void showSenseMissingCard(@NonNull final Context context) {
        this.showWelcomeCard = false;
        displayMessage(true,
                       0,
                       context.getString(R.string.error_room_conditions_no_sense),
                       R.string.action_pair_sense,
                       v -> SensorResponseAdapter.this.dispatchErrorItemClicked()
                      );
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

    private class WelcomeCardViewHolder extends BaseViewHolder {

        public WelcomeCardViewHolder(@NonNull final View itemView) {
            super(itemView);
            final ItemWelcomeCardBinding binding = DataBindingUtil.bind(itemView);
            binding.itemWelcomeImage.setImageResource(R.drawable.feature_sensors);
            binding.itemWelcomeTitle.setText(R.string.room_conditions_welcome_title);
            binding.itemWelcomeMessage.setText(R.string.room_conditions_welcome_message);
        }
    }

    private class ErrorViewHolder extends BaseViewHolder {
        final View view;
        final ImageTextView titleImageTextView;
        final TextView messageTextView;
        final ImageView imageView;
        final Button button;

        public ErrorViewHolder(@NonNull final View view) {
            super(view);
            this.titleImageTextView = (ImageTextView) view.findViewById(R.id.item_message_card_image_text);
            this.messageTextView = (TextView) view.findViewById(R.id.item_message_card_message);
            this.view = view;
            this.imageView = (ImageView) view.findViewById(R.id.item_message_card_image);
            this.button = (Button) view.findViewById(R.id.item_message_card_action);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);

            if (SensorResponseAdapter.this.messageTitle != 0) {
                this.titleImageTextView.setText(SensorResponseAdapter.this.messageTitle);
                this.titleImageTextView.setVisibility(View.VISIBLE);
            } else {
                this.titleImageTextView.setVisibility(View.GONE);
            }

            final ImageView image = (ImageView) this.view.findViewById(R.id.item_message_card_image);
            if (SensorResponseAdapter.this.messageWantsSenseIcon) {
                image.setImageResource(R.drawable.empty_no_sense_paired);
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

    private class SensorBaseViewHolder extends BaseViewHolder {

        protected final TextView title;
        protected final TextView body;
        @VisibleForTesting
        public final TextView value;
        @VisibleForTesting
        public final TextView descriptor;
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
            this.descriptor.setAllCaps(false);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final Sensor sensor = getItem(position);
            this.title.setText(sensor.getName());
            this.body.setText(sensor.getMessage());
            if (sensor.getType() == SensorType.TEMPERATURE || sensor.getType() == SensorType.HUMIDITY) {
                this.value.setText(unitFormatter.createUnitBuilder(sensor)
                                                .buildWithStyle());
                this.descriptor.setText(null);
            } else {
                this.value.setText(unitFormatter.createUnitBuilder(sensor)
                                                .hideSuffix()
                                                .build());
                this.descriptor.setText(SensorResponseAdapter.this.unitFormatter
                                                .createUnitBuilder(sensor)
                                                .hideValue()
                                                .build());
            }
            this.value.setTextColor(ContextCompat.getColor(inflater.getContext(), sensor.getColor()));
        }
    }

    public class SensorGroupViewHolder extends BaseViewHolder {
        final ItemSensorGroupBinding sensorGroupBinding;

        public SensorGroupViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.sensorGroupBinding = DataBindingUtil.bind(itemView);
            //todo also update title text on bind
            sensorGroupBinding.itemSensorGroupTitle.setText(itemView.getContext().getString(R.string.air_quality));
            sensorGroupBinding.itemSensorGroupContent.setOnRowClickListener(sensor -> SensorResponseAdapter.this.dispatchItemClicked(0, sensor));
            sensorGroupBinding.itemSensorGroupContent.setUnitFormatter(SensorResponseAdapter.this.unitFormatter);

        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final List<Sensor> sensors = SensorResponseAdapter.this.sensorGroupArray.get(position, new ArrayList<>());
            sensorGroupBinding.itemSensorGroupBody.setText(Sensor.getWorstMessage(sensors));
            sensorGroupBinding.itemSensorGroupContent.replaceAll(sensors);
        }
    }

    public class SensorViewHolder extends SensorBaseViewHolder implements Drawable.Callback {

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
