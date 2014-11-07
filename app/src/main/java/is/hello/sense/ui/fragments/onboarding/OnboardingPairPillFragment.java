package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;

public class OnboardingPairPillFragment extends InjectionFragment {
    private static final String ARG_COLOR_INDEX = OnboardingPairPillFragment.class.getName() + ".ARG_COLOR_INDEX";

    @Inject HardwarePresenter hardwarePresenter;

    public static OnboardingPairPillFragment newInstance(int colorIndex) {
        OnboardingPairPillFragment fragment = new OnboardingPairPillFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_COLOR_INDEX, colorIndex);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.event(Analytics.EVENT_ONBOARDING_PAIR_PILL, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        nextButton.setOnClickListener(this::next);

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(ignored -> finishedPairing());

        return view;
    }


    private void beginPairing() {
        LoadingDialogFragment.show(getFragmentManager(), getString(R.string.title_pairing), true);
    }

    private void finishedPairing() {
        hardwarePresenter.clearDevice();

        LoadingDialogFragment.close(getFragmentManager());

        OnboardingActivity activity = (OnboardingActivity) getActivity();
        activity.showWelcome();
    }


    public void next(@NonNull View sender) {
        beginPairing();

        if (hardwarePresenter.getDevice() == null) {
            bindAndSubscribe(hardwarePresenter.rediscoverDevice(), device -> next(sender), this::presentError);
            return;
        }

        if (!hardwarePresenter.getDevice().isConnected()) {
            bindAndSubscribe(hardwarePresenter.connectToDevice(hardwarePresenter.getDevice()), ignored -> next(sender), this::presentError);
            return;
        }

        bindAndSubscribe(hardwarePresenter.linkPill(), ignored -> finishedPairing(), this::presentError);
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        if (hardwarePresenter.isErrorFatal(e)) {
            ErrorDialogFragment.presentFatalBluetoothError(getFragmentManager(), getActivity());
        } else {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
    }
}
