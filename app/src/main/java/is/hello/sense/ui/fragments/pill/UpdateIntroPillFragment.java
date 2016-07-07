package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.concurrent.TimeUnit;

import is.hello.sense.R;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class UpdateIntroPillFragment extends PillHardwareFragment {
    private static final String ARG_NEXT_SCREEN_ID = UpdateIntroPillFragment.class.getName() + ".ARG_NEXT_SCREEN_ID";

    private Button primaryButton;

    public static UpdateIntroPillFragment newInstance(final int nextScreenId) {
        final UpdateIntroPillFragment fragment = new UpdateIntroPillFragment();

        final Bundle arguments = new Bundle();
        arguments.putInt(ARG_NEXT_SCREEN_ID, nextScreenId);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Todo replace Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_BLE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        final View view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_update_sleep_pill)
                .setSubheadingText(R.string.info_update_sleep_pill)
                .setDiagramImage(R.drawable.sleep_pill_ota)
                .setPrimaryOnClickListener(this::onPrimaryButtonClick)
                .setSecondaryOnClickListener(this::onCancel)
                .setSecondaryButtonText(R.string.action_cancel)
                .setWantsSecondaryButton(true)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATE_PILL));

        this.primaryButton = (Button) view.findViewById(R.id.view_onboarding_simple_step_primary);
        this.primaryButton.setEnabled(false);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribeDevice();
        checkPillBattery();
    }

    public void onCancel(final View ignored) {
        finishWithResult(Activity.RESULT_CANCELED, null);
    }

    public void onPrimaryButtonClick(@NonNull final View ignored){
        done();
    }

    private void bindAndSubscribeDevice() {
        //Todo locate or use default delays
        bindAndSubscribe(
                devicesPresenter.devices.delay(1200, TimeUnit.MILLISECONDS)
                                        .map(devices -> {
                                            final SleepPillDevice sleepPillDevice = devices.getSleepPill();
                                            return sleepPillDevice != null && !sleepPillDevice.hasLowBattery();
                                        })
                , this::onNext
                , this::presentError);
    }

    private void done() {
        final int nextScreenId = getArguments().getInt(ARG_NEXT_SCREEN_ID,-1);
        if(nextScreenId != -1 && getActivity() instanceof FragmentNavigation){
            ((FragmentNavigation) getActivity()).flowFinished(this, nextScreenId, null);
        } else{
            onCancel(null);
        }
    }

    private void checkPillBattery() {
        showBlockingActivity(R.string.title_checking_pill_battery);
        devicesPresenter.update();
    }

    private void onNext(final boolean hasEnoughBattery){
        hideBlockingActivity(false, () -> {
            updateButtonUI();
            if(!hasEnoughBattery){
                presentPillBatteryError();
            }
        });
    }

    private void updateButtonUI(){
        primaryButton.setEnabled(true);
    }

    private void presentError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            updateButtonUI();
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withSupportLink()
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }
}
