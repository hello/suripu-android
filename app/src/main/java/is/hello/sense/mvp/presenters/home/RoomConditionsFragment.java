package is.hello.sense.mvp.presenters.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.RoomConditionsInteractor;
import is.hello.sense.mvp.view.home.RoomConditionsView;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

import static is.hello.sense.ui.adapter.SensorHistoryAdapter.Update;

public class RoomConditionsFragment extends BacksideTabFragment<RoomConditionsView> implements
        ArrayRecyclerAdapter.OnItemClickedListener<SensorState> {
    private final UpdateTimer updateTimer = new UpdateTimer(1, TimeUnit.MINUTES);

    @Inject
    RoomConditionsInteractor roomConditionsInteractor;
    @Inject
    UnitFormatter unitFormatter;


    //region Lifecycle

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.Backside.EVENT_CURRENT_CONDITIONS, null);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateTimer.setOnUpdate(roomConditionsInteractor::update);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setOnAdapterItemClickListener(this);
        bindAndSubscribe(unitFormatter.unitPreferenceChanges(),
                         ignored -> presenterView.notifyDataSetChanged(),
                         Functions.LOG_ERROR);
        bindAndSubscribe(roomConditionsInteractor.currentConditions,
                         this::bindConditions,
                         this::conditionsUnavailable);
    }

    @Override
    public RoomConditionsView getPresenterView() {
        if (presenterView == null) {
            return new RoomConditionsView(getActivity(), unitFormatter);
        }
        return presenterView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTimer.schedule();
    }

    @Override
    public void onPause() {
        super.onPause();
        updateTimer.unschedule();
    }

    @Override
    public void onSwipeInteractionDidFinish() {
        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_current_conditions, true);
    }

    @Override
    public void onUpdate() {
        roomConditionsInteractor.update();
    }

    //endregion


    //region Displaying Data


    public void bindConditions(@NonNull final RoomConditionsInteractor.Result result) {
        final RoomSensorHistory roomSensorHistory = result.roomSensorHistory;
        final List<SensorState> sensors = result.conditions.toList();

        for (final SensorState sensor : sensors) {
            final String sensorName = sensor.getName();
            final ArrayList<SensorGraphSample> samplesForSensor =
                    roomSensorHistory.getSamplesForSensor(sensorName);
            final SensorHistoryAdapter sensorGraphAdapter = presenterView.getSensorGraphAdapter(sensorName);
            bindAndSubscribe(Update.forHistorySeries(samplesForSensor, true),
                             sensorGraphAdapter::update,
                             e -> {
                                 Logger.error(getClass().getSimpleName(),
                                              "Could not update graph.", e);
                                 sensorGraphAdapter.clear();
                             });
        }
        presenterView.replaceAllSensors(sensors);
    }

    public void conditionsUnavailable(@NonNull final Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);
        if (ApiException.isNetworkError(e)) {
            presenterView.displayMessage(false, 0, getString(R.string.error_room_conditions_unavailable),
                                         R.string.action_retry,
                                         ignored -> roomConditionsInteractor.update());
        } else if (ApiException.statusEquals(e, 404)) {
            presenterView.displayMessage(true,
                                         0,
                                         getString(R.string.error_room_conditions_no_sense),
                                         R.string.action_pair_new_sense,
                                         ignored -> {
                                             final Intent intent = new Intent(getActivity(), OnboardingActivity.class);
                                             intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
                                             intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
                                             startActivity(intent);
                                         });
        } else {
            final StringRef messageRef = Errors.getDisplayMessage(e);
            final String message = messageRef != null
                    ? messageRef.resolve(getActivity())
                    : e.getMessage();
            presenterView.displayMessage(false, 0, message,
                                         R.string.action_retry,
                                         ignored -> roomConditionsInteractor.update());
        }
    }

    //endregion


    @Override
    public void onItemClicked(final int position, final SensorState sensorState) {
        final Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensorState.getName());
        startActivity(intent);
    }

}
