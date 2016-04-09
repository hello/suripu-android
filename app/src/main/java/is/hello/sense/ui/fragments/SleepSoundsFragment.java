package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.presenters.SleepDurationsPresenter;
import is.hello.sense.graph.presenters.SleepSoundsPresenter;
import is.hello.sense.ui.common.InjectionFragment;

public class SleepSoundsFragment extends InjectionFragment {

    @Inject
    SleepSoundsPresenter sleepSoundsPresenter;

    @Inject
    SleepDurationsPresenter sleepDurationsPresenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sleep_sounds, container, false);
        //todo Sound stuff.

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sleepSoundsPresenter.sounds, this::bindSounds, this::presentError);
        bindAndSubscribe(sleepDurationsPresenter.durations, this::bindDurations, this::presentError);
        sleepDurationsPresenter.update();
        sleepSoundsPresenter.update();

    }

    private void bindSounds(@NonNull VoidResponse sounds) {

    }

    private void bindDurations(@NonNull VoidResponse durations) {

    }

    private void presentError(@NonNull Throwable error) {

    }


}
