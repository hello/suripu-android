package is.hello.sense.mvp.presenters.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.CurrentConditionsInteractor;
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
    CurrentConditionsInteractor currentConditionsInteractor;
    @Inject
    UnitFormatter unitFormatter;


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
        addInteractor(currentConditionsInteractor);
        updateTimer.setOnUpdate(currentConditionsInteractor::update);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setOnAdapterItemClickListener(this);
        bindAndSubscribe(unitFormatter.unitPreferenceChanges(),
                         ignored -> presenterView.notifyDataSetChanged(),
                         Functions.LOG_ERROR);
        bindAndSubscribe(currentConditionsInteractor.sensors,
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
        currentConditionsInteractor.update();
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
    public  final void onUpdate() {
        currentConditionsInteractor.update();
    }

    //endregion


    //region Displaying Data


    public final void bindConditions(@NonNull final SensorResponse currentConditions) {
        presenterView.replaceAllSensors(currentConditions.getSensors());
    }

    public final void conditionsUnavailable(@NonNull final Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);
        if (ApiException.isNetworkError(e)) {
            presenterView.displayMessage(false, 0, getString(R.string.error_room_conditions_unavailable),
                                         R.string.action_retry,
                                         ignored -> currentConditionsInteractor.update());
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
                                         ignored -> currentConditionsInteractor.update());
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
