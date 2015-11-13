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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
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
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.widget.BlockableScrollView;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitPrinter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Observable;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SensorHistoryFragment extends InjectionFragment implements SelectorView.OnSelectionChangedListener {
    @Inject RoomConditionsPresenter conditionsPresenter;
    @Inject SensorHistoryPresenter sensorHistoryPresenter;
    @Inject Markdown markdown;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;
    @Inject PreferencesPresenter preferences;

    private final UpdateTimer updateTimer = new UpdateTimer(1, TimeUnit.MINUTES);

    private BlockableScrollView scrollView;
    private TextView readingText;
    private TextView messageText;
    private TextView insightText;
    private SelectorView historyModeSelector;
    private String sensor;

    private ProgressBar loadingIndicator;
    private TextView graphPlaceholder;
    private GraphView graphView;
    private final SensorDataSource sensorDataSource = new SensorDataSource();

    private TutorialOverlayView tutorialOverlayView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.sensor = getSensorHistoryActivity().getSensor();

        sensorHistoryPresenter.setSensorName(sensor);
        addPresenter(sensorHistoryPresenter);
        addPresenter(conditionsPresenter);

        if (savedInstanceState == null) {
            final JSONObject properties = Analytics.createProperties(Analytics.TopView.PROP_SENSOR_NAME, sensor);
            Analytics.trackEvent(Analytics.TopView.EVENT_SENSOR_HISTORY, properties);
        }

        updateTimer.setOnUpdate(() -> {
            sensorHistoryPresenter.update();
            conditionsPresenter.update();
        });

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_history, container, false);

        this.scrollView = (BlockableScrollView) view.findViewById(R.id.fragment_sensor_history_scroll_view);

        this.readingText = (TextView) view.findViewById(R.id.fragment_sensor_history_reading);
        this.messageText = (TextView) view.findViewById(R.id.fragment_sensor_history_message);
        this.insightText = (TextView) view.findViewById(R.id.fragment_sensor_history_insight);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_sensor_history_loading);
        this.graphPlaceholder = (TextView) view.findViewById(R.id.fragment_sensor_history_placeholder);
        this.graphView = (GraphView) view.findViewById(R.id.fragment_sensor_history_graph);

        graphView.setGraphDrawable(new LineGraphDrawable(getResources()));
        graphView.setAdapter(sensorDataSource);
        graphView.setHeaderFooterProvider(sensorDataSource);
        graphView.setHighlightListener(sensorDataSource);
        graphView.setTintColor(getResources().getColor(R.color.sensor_unknown));

        this.historyModeSelector = (SelectorView) view.findViewById(R.id.fragment_sensor_history_mode);
        historyModeSelector.setOnSelectionChangedListener(this);
        historyModeSelector.setButtonTags(SensorHistoryPresenter.Mode.DAY, SensorHistoryPresenter.Mode.WEEK);
        historyModeSelector.setBackground(new TabsBackgroundDrawable(getResources(), TabsBackgroundDrawable.Style.INLINE));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(unitFormatter.unitPreferenceChanges(),
                         ignored -> {
                             conditionsPresenter.update();
                             sensorDataSource.notifyDataChanged();
                         },
                         Functions.LOG_ERROR);
        bindAndSubscribe(conditionsPresenter.currentConditions,
                         this::bindConditions,
                         this::conditionUnavailable);
        bindAndSubscribe(sensorHistoryPresenter.history,
                         sensorDataSource::bindHistory,
                         sensorDataSource::historyUnavailable);
        bindAndSubscribe(preferences.observableUse24Time(),
                         sensorDataSource::setUse24Time,
                         Functions.LOG_ERROR);
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

        final SensorHistoryActivity activity = (SensorHistoryActivity) getActivity();
        final boolean welcomeDialogShown = activity.showWelcomeDialog(false);
        if (!welcomeDialogShown && tutorialOverlayView == null &&
                Tutorial.SCRUB_SENSOR_HISTORY.shouldShow(getActivity())) {
            this.tutorialOverlayView = new TutorialOverlayView(activity, Tutorial.SCRUB_SENSOR_HISTORY);
            tutorialOverlayView.show(R.id.activity_sensor_history_root);
        }

        updateTimer.schedule();
    }

    @Override
    public void onPause() {
        super.onPause();

        updateTimer.unschedule();
    }

    public SensorHistoryActivity getSensorHistoryActivity() {
        return (SensorHistoryActivity) getActivity();
    }

    public void bindConditions(@Nullable RoomConditionsPresenter.Result result) {
        if (result == null) {
            readingText.setText(R.string.missing_data_placeholder);
            messageText.setText(null);
            insightText.setText(null);
        } else {
            SensorState condition = result.conditions.getSensorStateWithName(sensor);
            if (condition != null) {
                UnitPrinter printer = unitFormatter.getUnitPrinterForSensor(sensor);

                CharSequence formattedValue = condition.getFormattedValue(printer);
                if (formattedValue != null) {
                    readingText.setText(formattedValue);
                } else {
                    readingText.setText(R.string.missing_data_placeholder);
                }

                int sensorColor = getResources().getColor(condition.getCondition().colorRes);
                readingText.setTextColor(getResources().getColor(condition.getCondition().colorRes));

                markdown.renderInto(messageText, condition.getMessage());
                markdown.renderInto(insightText, condition.getIdealConditions());

                graphView.setTintColor(sensorColor);
            } else {
                readingText.setText(R.string.missing_data_placeholder);
                messageText.setText(null);
                insightText.setText(null);
            }
        }
    }

    public void conditionUnavailable(@NonNull Throwable e) {
        Logger.error(SensorHistoryFragment.class.getSimpleName(), "Could not load conditions", e);
        readingText.setText(R.string.missing_data_placeholder);
        StringRef errorMessage = Errors.getDisplayMessage(e);
        if (errorMessage != null) {
            messageText.setText(errorMessage.resolve(getActivity()));
        } else {
            messageText.setText(R.string.dialog_error_generic_message);
        }
        insightText.setText(null);
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        loadingIndicator.setScaleX(0f);
        loadingIndicator.setScaleY(0f);
        animatorFor(loadingIndicator)
                .fadeIn()
                .scale(1f)
                .start();

        graphPlaceholder.setVisibility(View.GONE);
        sensorDataSource.clear();

        SensorHistoryPresenter.Mode newMode = (SensorHistoryPresenter.Mode) historyModeSelector.getButtonTagAt(newSelectionIndex);
        sensorHistoryPresenter.setMode(newMode);
    }


    public class SensorDataSource extends SensorHistoryAdapter implements GraphView.HeaderFooterProvider, GraphView.HighlightListener {
        private boolean use24Time = false;

        public void bindHistory(@NonNull ArrayList<SensorGraphSample> history) {
            if (history.isEmpty()) {
                clear();
                graphPlaceholder.setText(R.string.message_not_enough_data);
                graphPlaceholder.setVisibility(View.VISIBLE);
                loadingIndicator.setVisibility(View.GONE);
                return;
            }

            Observable<Update> update = Update.forHistorySeries(history, false);
            bindAndSubscribe(update, this::update, this::historyUnavailable);

            animatorFor(loadingIndicator)
                    .fadeOut(View.GONE)
                    .scale(0f)
                    .start();
        }

        public void historyUnavailable(Throwable e) {
            Logger.error(getClass().getSimpleName(), "Could not render graph", e);

            clear();
            graphPlaceholder.setText(R.string.sensor_history_message_error);
            graphPlaceholder.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
        }


        //region Styling

        public void setUse24Time(boolean use24Time) {
            this.use24Time = use24Time;
            notifyDataChanged();
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
            return "";
        }

        //endregion


        //region Highlight Listener

        private CharSequence savedReading;
        private CharSequence savedMessage;
        private int savedScrollY;

        @Override
        public void onGraphHighlightBegin() {
            this.savedReading = readingText.getText();
            this.savedMessage = messageText.getText();

            messageText.setGravity(Gravity.CENTER);
            messageText.setTextColor(getResources().getColor(R.color.text_dim));

            insightText.setVisibility(View.GONE);

            scrollView.setScrollingEnabled(false);
            this.savedScrollY = scrollView.getScrollY();

            Tutorial.SCRUB_SENSOR_HISTORY.markShown(getActivity());
        }

        @Override
        public void onGraphValueHighlighted(int section, int position) {
            final SensorGraphSample instant = getSection(section).get(position);
            if (instant.isValuePlaceholder()) {
                readingText.setText(R.string.missing_data_placeholder);
            } else {
                final UnitPrinter printer = unitFormatter.getUnitPrinterForSensor(sensor);
                final CharSequence reading = printer.print(instant.getValue());
                readingText.setText(reading);
            }

            if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.Mode.WEEK) {
                messageText.setText(dateFormatter.formatAsDayAndTime(instant.getShiftedTime(), use24Time));
            } else {
                messageText.setText(dateFormatter.formatAsTime(instant.getShiftedTime(), use24Time));
            }
        }

        @Override
        public void onGraphHighlightEnd() {
            stateSafeExecutor.execute(() -> {
                readingText.setText(savedReading);
                this.savedReading = null;

                messageText.setText(savedMessage);
                this.savedMessage = null;

                messageText.setGravity(Gravity.START);
                messageText.setTextColor(getResources().getColor(R.color.text_dark));

                insightText.setVisibility(View.VISIBLE);

                scrollView.setScrollingEnabled(true);
                scrollView.post(() -> scrollView.smoothScrollTo(0, savedScrollY));
            });
        }

        //endregion
    }
}
