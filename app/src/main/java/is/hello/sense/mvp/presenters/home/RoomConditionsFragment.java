package is.hello.sense.mvp.presenters.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorData;
import is.hello.sense.api.model.v2.sensors.SensorDataRequest;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.SensorResponseInteractor;
import is.hello.sense.mvp.view.home.RoomConditionsView;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public class RoomConditionsFragment extends BacksideTabFragment<RoomConditionsView> implements
        ArrayRecyclerAdapter.OnItemClickedListener<Sensor> {
    private final UpdateTimer updateTimer = new UpdateTimer(1, TimeUnit.MINUTES);

    @Inject
    SensorResponseInteractor sensorResponseInteractor;
    @Inject
    UnitFormatter unitFormatter;
    @Inject
    ApiService apiService;


    //region Lifecycle

    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.Backside.EVENT_CURRENT_CONDITIONS, null);
        }
    }


    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(unitFormatter);
        addInteractor(sensorResponseInteractor);
        updateTimer.setOnUpdate(sensorResponseInteractor::update);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setOnAdapterItemClickListener(this);
        bindAndSubscribe(unitFormatter.unitPreferenceChanges(),
                         ignored -> presenterView.notifyDataSetChanged(),
                         Functions.LOG_ERROR);
        bindAndSubscribe(sensorResponseInteractor.sensors,
                         this::bindConditions,
                         this::conditionsUnavailable);
    }

    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView= new RoomConditionsView(getActivity(), unitFormatter);
        }
    }

    @Override
    public final void onResume() {
        super.onResume();
        updateTimer.schedule();
        sensorResponseInteractor.update();
    }

    @Override
    public final void onPause() {
        super.onPause();
        updateTimer.unschedule();
    }

    @Override
    public final void onSwipeInteractionDidFinish() {
        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_current_conditions, true);
    }

    @Override
    public final void onUpdate() {
        sensorResponseInteractor.update();
    }

    //endregion


    //region Displaying Data


    public final void bindConditions(@NonNull final SensorResponse currentConditions) {
        final List<Sensor> sensors = currentConditions.getSensors();
        bindAndSubscribe(apiService.postSensors(new SensorDataRequest(sensors)),
                         sensorsDataResponse -> {
                             final SensorData sensorData = sensorsDataResponse.getSensorData();
                             for (final Sensor sensor : sensors) {
                                 final float[] values = sensorData.get(sensor.getType());
                                 if (values != null) {
                                     sensor.setSensorValues(values);
                                 }
                             }
                             presenterView.replaceAllSensors(sensors);
                         },
                         throwable -> {
                             Log.e("Sensor", "error: " + throwable);
                         });


    }

    public final void conditionsUnavailable(@NonNull final Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);
        if (ApiException.isNetworkError(e)) {
            presenterView.displayMessage(false, 0, getString(R.string.error_room_conditions_unavailable),
                                         R.string.action_retry,
                                         ignored -> sensorResponseInteractor.update());
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
                                         ignored -> sensorResponseInteractor.update());
        }
    }

    //endregion


    @Override
    public final void onItemClicked(final int position, final Sensor sensor) {
        final Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        /// intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensorState.getName()); todo update
        startActivity(intent);
    }

}
