package is.hello.sense.flows.home.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.v2.sensors.QueryScope;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorDataRequest;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.api.model.v2.sensors.SensorsDataResponse;
import is.hello.sense.flows.home.interactors.SensorResponseInteractor;
import is.hello.sense.flows.home.ui.adapters.SensorResponseAdapter;
import is.hello.sense.flows.home.ui.views.RoomConditionsView;
import is.hello.sense.flows.sensordetails.ui.activities.SensorDetailActivity;
import is.hello.sense.flows.settings.ui.activities.AppSettingsActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.util.ViewPagerPresenterChild;
import is.hello.sense.mvp.util.ViewPagerPresenterChildDelegate;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class RoomConditionsPresenterFragment extends PresenterFragment<RoomConditionsView>
        implements ArrayRecyclerAdapter.OnItemClickedListener<Sensor>,
        SensorResponseAdapter.ErrorItemClickListener,
        ViewPagerPresenterChild{
    private final static long WELCOME_CARD_TIMES_SHOWN_LIMIT = 2;

    @Inject
    SensorResponseInteractor sensorResponseInteractor;
    @Inject
    UnitFormatter unitFormatter;
    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    ApiService apiService;

    @VisibleForTesting
    public SensorResponseAdapter adapter;
    private UpdateTimer updateTimer;
    private boolean checkRoomConditions = false;
    @NonNull
    private Subscription postSensorSubscription = Subscriptions.empty();
    private final ViewPagerPresenterChildDelegate viewPagerPresenterChildDelegate = new ViewPagerPresenterChildDelegate(this);

    @Override
    public final void initializePresenterView() {
        if (this.presenterView == null) {
            if (this.adapter == null) {
                this.adapter = new SensorResponseAdapter(getActivity().getLayoutInflater(), unitFormatter);
                this.adapter.setOnItemClickedListener(this);
                this.adapter.setErrorItemClickListener(this);
            }
            this.presenterView = new RoomConditionsView(getActivity(), this.adapter);
            this.presenterView.setSettingsButtonClickListener(this::startSettingsActivity);
            this.viewPagerPresenterChildDelegate.onViewInitialized();
        }
    }

    //region Lifecycle
    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        viewPagerPresenterChildDelegate.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(this.unitFormatter);
        addInteractor(this.preferencesInteractor);
        addInteractor(this.sensorResponseInteractor);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.updateTimer = new UpdateTimer(1, TimeUnit.MINUTES);
        this.updateTimer.setOnUpdate(this.sensorResponseInteractor::update);
        bindAndSubscribe(this.unitFormatter.unitPreferenceChanges(),
                         ignored -> this.adapter.notifyDataSetChanged(),
                         Functions.LOG_ERROR);

        bindAndSubscribe(this.sensorResponseInteractor.sensors,
                         this::bindConditions,
                         this::conditionsUnavailable);

        checkRoomConditions = true; // todo support this again.
    }

    @Override
    public final void onResume() {
        super.onResume();
        this.updateTimer.schedule();
        viewPagerPresenterChildDelegate.onResume();
    }

    @Override
    public final void onPause() {
        super.onPause();
        if (this.updateTimer != null) {
            this.updateTimer.unschedule();
            viewPagerPresenterChildDelegate.onPause();
        }
    }

    @Override
    public void onUserVisible() {
        Analytics.trackEvent(Analytics.Backside.EVENT_CURRENT_CONDITIONS, null);
        this.sensorResponseInteractor.update();

    }

    @Override
    public void onUserInvisible() {

    }

    @Override
    public void onRelease() {
        super.onRelease();
        this.postSensorSubscription.unsubscribe();
        this.postSensorSubscription = Subscriptions.empty();

        if (this.updateTimer != null) {
            this.updateTimer.unschedule();
        }

        if (this.adapter != null) {
            this.adapter.release();
            this.adapter.setOnItemClickedListener(null);
            this.adapter.setErrorItemClickListener(null);
            this.adapter.clear();
        }

        this.adapter = null;
        this.updateTimer = null;
    }

    //endregion

    private void startSettingsActivity(final View ignore) {
        startActivity(new Intent(getActivity(), AppSettingsActivity.class));
    }


    //region Displaying Data

    private void showWelcomeCardIfNeeded() {
        if (checkRoomConditions) {
            checkRoomConditions = false;
            final int timesShown = preferencesInteractor.getInt(PreferencesInteractor.ROOM_CONDITIONS_WELCOME_CARD_TIMES_SHOWN, 1);
            if (timesShown <= WELCOME_CARD_TIMES_SHOWN_LIMIT) {
                preferencesInteractor.edit().putInt(PreferencesInteractor.ROOM_CONDITIONS_WELCOME_CARD_TIMES_SHOWN,
                                                    timesShown + 1).apply();
                adapter.showWelcomeCard(true);
            } else {
                adapter.showWelcomeCard(false);
            }
        }
    }

    public final void bindConditions(@NonNull final SensorResponse currentConditions) {
        presenterView.showProgress(false);

        switch (currentConditions.getStatus()) {
            case OK:
            case WAITING_FOR_DATA:
                showWelcomeCardIfNeeded();
                final List<Sensor> sensors = currentConditions.getSensors();
                postSensorSubscription.unsubscribe();
                postSensorSubscription = bind(this.apiService.postSensors(new SensorDataRequest(QueryScope.LAST_3H_5_MINUTE, sensors)))
                        .subscribe(sensorsDataResponse -> RoomConditionsPresenterFragment.this.bindDataResponse(sensorsDataResponse, sensors),
                                   this::conditionsUnavailable);
                break;
            case NO_SENSE:
                adapter.showSenseMissingCard();
                break;
            default:
        }
    }

    public final void bindDataResponse(@NonNull final SensorsDataResponse sensorsDataResponse,
                                       @NonNull final List<Sensor> sensors) {
        for (final Sensor sensor : sensors) {
            sensor.setSensorValues(sensorsDataResponse);
        }
        this.adapter.replaceAll(sensors);
    }

    public final void conditionsUnavailable(@NonNull final Throwable e) {
        presenterView.showProgress(false);

        Logger.error(RoomConditionsPresenterFragment.class.getSimpleName(), "Could not load conditions", e);
        if (ApiException.isNetworkError(e)) {
            this.adapter.displayMessage(false, 0, getString(R.string.error_room_conditions_unavailable),
                                        R.string.action_retry,
                                        ignored -> this.sensorResponseInteractor.update());
        } else if (ApiException.statusEquals(e, 404)) {
            this.adapter.showSenseMissingCard();
        } else {
            final StringRef messageRef = Errors.getDisplayMessage(e);
            final String message = messageRef != null
                    ? messageRef.resolve(getActivity())
                    : e.getMessage();
            this.adapter.displayMessage(false, 0, message,
                                        R.string.action_retry,
                                        ignored -> this.sensorResponseInteractor.update());
        }
    }

    //endregion


    @Override
    public final void onItemClicked(final int position, final Sensor sensor) {
        SensorDetailActivity.startActivity(getActivity(), sensor);
    }

    @Override
    public void onErrorItemClicked() {
        final Intent intent = new Intent(getActivity(), OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
        intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
        this.startActivity(intent);
    }

}
