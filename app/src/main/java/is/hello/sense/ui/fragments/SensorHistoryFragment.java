package is.hello.sense.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.WelcomeDialog;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import is.hello.sense.util.WelcomeDialogParser;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class SensorHistoryFragment extends InjectionFragment implements SelectorLinearLayout.OnSelectionChangedListener {
    @Inject RoomConditionsPresenter conditionsPresenter;
    @Inject SensorHistoryPresenter sensorHistoryPresenter;
    @Inject Markdown markdown;
    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;

    private TextView readingText;
    private TextView messageText;
    private SelectorLinearLayout historyModeSelector;
    private ProgressBar loadingIndicator;
    private TextView insightText;
    private String sensor;

    private GraphView graphView;
    private SensorDataSource sensorDataSource = new SensorDataSource();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.sensor = getSensorHistoryActivity().getSensor();

        sensorHistoryPresenter.setSensorName(sensor);
        addPresenter(sensorHistoryPresenter);
        addPresenter(conditionsPresenter);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_SENSOR_HISTORY, Analytics.createProperties(Analytics.TopView.PROP_SENSOR_NAME, sensor));
        }

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_history, container, false);

        this.readingText = (TextView) view.findViewById(R.id.fragment_sensor_history_reading);
        this.messageText = (TextView) view.findViewById(R.id.fragment_sensor_history_message);
        this.graphView = (GraphView) view.findViewById(R.id.fragment_sensor_history_graph);
        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_sensor_history_loading);
        this.insightText = (TextView) view.findViewById(R.id.fragment_sensor_history_insight);

        graphView.setGraphDrawable(new LineGraphDrawable(getResources()));
        graphView.setAdapter(sensorDataSource);
        graphView.setHeaderFooterProvider(sensorDataSource);
        graphView.setHighlightListener(sensorDataSource);
        graphView.setTintColor(getResources().getColor(R.color.sensor_unknown));

        this.historyModeSelector = (SelectorLinearLayout) view.findViewById(R.id.fragment_sensor_history_mode);
        historyModeSelector.setOnSelectionChangedListener(this);
        historyModeSelector.setButtonTags(SensorHistoryPresenter.Mode.DAY, SensorHistoryPresenter.Mode.WEEK);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(conditionsPresenter.currentConditions, this::bindConditions, this::conditionUnavailable);
        bindAndSubscribe(sensorHistoryPresenter.history, sensorDataSource::bindHistory, sensorDataSource::historyUnavailable);
        bindAndSubscribe(preferences.observableBoolean(PreferencesPresenter.USE_24_TIME, false), sensorDataSource::setUse24Time, Functions.LOG_ERROR);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= Presenter.BASE_TRIM_LEVEL) {
            sensorDataSource.clear();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        int welcomeDialogRes;
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_temperature;
                break;
            }
            case ApiService.SENSOR_NAME_HUMIDITY: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_humidity;
                break;
            }
            case ApiService.SENSOR_NAME_PARTICULATES: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_particulates;
                break;
            }
            case ApiService.SENSOR_NAME_SOUND: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_sound;
                break;
            }
            case ApiService.SENSOR_NAME_LIGHT: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_light;
                break;
            }
            default: {
                welcomeDialogRes = WelcomeDialogParser.MISSING_RES;
                break;
            }
        }
        WelcomeDialog.showIfNeeded(getActivity(), welcomeDialogRes);
    }

    public SensorHistoryActivity getSensorHistoryActivity() {
        return (SensorHistoryActivity) getActivity();
    }

    public void bindConditions(@Nullable RoomConditionsPresenter.Result result) {
        if (result == null) {
            readingText.setText(R.string.missing_data_placeholder);
            messageText.setText(R.string.missing_data_placeholder);
            insightText.setText(R.string.missing_data_placeholder);
        } else {
            SensorState condition = result.conditions.getSensorStateWithName(sensor);
            if (condition != null) {
                UnitFormatter.Formatter formatter = result.units.getUnitFormatterForSensor(sensor);

                CharSequence formattedValue = condition.getFormattedValue(formatter);
                if (formattedValue != null) {
                    readingText.setText(formattedValue);
                } else {
                    readingText.setText(R.string.missing_data_placeholder);
                }

                int sensorColor = getResources().getColor(condition.getCondition().colorRes);
                readingText.setTextColor(sensorColor);

                markdown.renderEmphasisInto(messageText, sensorColor, condition.getMessage());
                markdown.renderEmphasisInto(insightText, sensorColor, condition.getIdealConditions());

                graphView.setTintColor(sensorColor);
            } else {
                readingText.setText(R.string.missing_data_placeholder);
                messageText.setText(R.string.missing_data_placeholder);
                insightText.setText(R.string.missing_data_placeholder);
            }
        }
    }

    public void conditionUnavailable(@NonNull Throwable e) {
        Logger.error(SensorHistoryFragment.class.getSimpleName(), "Could not load conditions", e);
        readingText.setText(R.string.missing_data_placeholder);
        messageText.setText(e.getMessage());
        insightText.setText(R.string.missing_data_placeholder);
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        loadingIndicator.setScaleX(0f);
        loadingIndicator.setScaleY(0f);
        animate(loadingIndicator)
                .fadeIn()
                .scale(1f)
                .start();
        sensorDataSource.clear();

        SensorHistoryPresenter.Mode newMode = (SensorHistoryPresenter.Mode) historyModeSelector.getButtonTag(newSelectionIndex);
        sensorHistoryPresenter.setMode(newMode);
    }


    public class SensorDataSource extends SensorHistoryAdapter implements GraphView.HeaderFooterProvider, GraphView.HighlightListener {
        private UnitSystem unitSystem;
        private boolean use24Time = false;

        public void bindHistory(@NonNull SensorHistoryPresenter.Result historyAndUnits) {
            List<SensorGraphSample> history = historyAndUnits.data;
            if (history.isEmpty()) {
                clear();
                return;
            }

            Observable<Update> update = Update.forHistorySeries(history, sensorHistoryPresenter.getMode());
            bindAndSubscribe(update, this::update, Functions.LOG_ERROR);

            this.unitSystem = historyAndUnits.unitSystem;

            animate(loadingIndicator)
                    .fadeOut(View.GONE)
                    .scale(0f)
                    .start();
        }

        public void historyUnavailable(Throwable e) {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
            clear();

            animate(loadingIndicator)
                    .fadeOut(View.GONE)
                    .scale(0f)
                    .start();
        }


        //region Styling

        public void setUse24Time(boolean use24Time) {
            this.use24Time = use24Time;
            notifyDataChanged();
        }

        private CharSequence formatSensorValue(long value) {
            switch (sensor) {
                case ApiService.SENSOR_NAME_TEMPERATURE:
                    return unitSystem.formatTemperature(value);

                case ApiService.SENSOR_NAME_HUMIDITY:
                    return unitSystem.formatHumidity(value);

                case ApiService.SENSOR_NAME_PARTICULATES:
                    return unitSystem.formatParticulates(value);

                case ApiService.SENSOR_NAME_SOUND:
                    return unitSystem.formatDecibels(value);

                case ApiService.SENSOR_NAME_LIGHT:
                    return unitSystem.formatLight(value);

                default:
                    return Long.toString(value);
            }
        }

        @Override
        public int getSectionHeaderFooterCount() {
            return getSectionCount();
        }

        @Override
        public int getSectionHeaderTextColor(int section) {
            return Color.TRANSPARENT;
        }

        @NonNull
        @Override
        public String getSectionHeader(int section) {
            return "";
        }

        @Override
        public int getSectionFooterTextColor(int section) {
            if (section == getSectionHeaderFooterCount() - 1) {
                return Color.BLACK;
            } else {
                return Color.GRAY;
            }
        }

        @NonNull
        @Override
        public String getSectionFooter(int section) {
            long value = getSection(section).getAverage();
            if (value == ApiService.PLACEHOLDER_VALUE) {
                return getString(R.string.missing_data_placeholder);
            } else {
                return formatSensorValue(value).toString();
            }
        }

        //endregion


        //region Highlight Listener

        private CharSequence savedReading;
        private CharSequence savedMessage;

        @Override
        public void onGraphHighlightBegin() {
            this.savedReading = readingText.getText();
            this.savedMessage = messageText.getText();

            messageText.setGravity(Gravity.CENTER);
            messageText.setTextColor(getResources().getColor(R.color.text_dim));
        }

        @Override
        public void onGraphValueHighlighted(int section, int position) {
            SensorGraphSample instant = getSection(section).get(position);
            if (instant.isValuePlaceholder()) {
                readingText.setText(R.string.missing_data_placeholder);
            } else {
                readingText.setText(formatSensorValue(instant.getValue()));
            }

            if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.Mode.WEEK) {
                messageText.setText(dateFormatter.formatAsDayAndTime(instant.getShiftedTime(), use24Time));
            } else {
                messageText.setText(dateFormatter.formatAsTime(instant.getShiftedTime(), use24Time));
            }
        }

        @Override
        public void onGraphHighlightEnd() {
            coordinator.postOnResume(() -> {
                readingText.setText(savedReading);
                this.savedReading = null;

                messageText.setText(savedMessage);
                this.savedMessage = null;

                messageText.setGravity(Gravity.START);
                messageText.setTextColor(getResources().getColor(R.color.text_dark));
            });
        }

        //endregion
    }
}
