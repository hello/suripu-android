package is.hello.sense.ui.fragments.pill;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.bluetooth.errors.SensePeripheralError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.presenters.BasePairPillPresenter;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.util.Views;

public class PairPillFragment extends BasePresenterFragment
        implements BasePairPillPresenter.Output {

    @Inject
    BasePairPillPresenter presenter;
    protected ProgressBar activityIndicator;
    protected TextView activityStatus;

    protected DiagramVideoView diagram;
    protected Button skipButton;
    protected Button retryButton;
    protected boolean isPairing = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addScopedPresenter(presenter);
        presenter.trackOnCreate();
    }

    @NonNull
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pair_pill, container, false);
        ((TextView) view.findViewById(R.id.fragment_pair_pill_subhead)).setText(presenter.getSubTitleRes());
        ((TextView) view.findViewById(R.id.fragment_pair_pill_title)).setText(presenter.getTitleRes());
        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_pair_pill_status);

        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_pair_pill_diagram);

        this.skipButton = (Button) view.findViewById(R.id.fragment_pair_pill_skip);
        Views.setSafeOnClickListener(skipButton, ignored -> presenter.skipPairingPill(getActivity()));

        this.retryButton = (Button) view.findViewById(R.id.fragment_pair_pill_retry);
        Views.setSafeOnClickListener(retryButton, ignored -> presenter.pairPill());
        initialize(view);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        if (!isPairing) {
            presenter.pairPill();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diagram != null) {
            diagram.destroy();
        }

        this.activityIndicator = null;
        this.activityStatus = null;

        this.diagram = null;

        this.skipButton = null;
        this.retryButton = null;
    }

    protected void initialize(@NonNull final View view) {
        OnboardingToolbar.of(this, view)
                         .setWantsBackButton(presenter.wantsBackButton())
                         .setOnHelpClickListener(this::help);

        if (BuildConfig.DEBUG) {
            diagram.setOnLongClickListener(ignored -> {
                presenter.skipPairingPill(getActivity());
                return true;
            });
            diagram.setBackgroundResource(R.drawable.selectable_dark);
        }

    }

    @Override
    public void finishedPairing(final boolean success) {
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> presenter.execute(() -> {
            presenter.finishedPairingAction(getActivity(), success);

        }));
    }

    @Override
    public void animateDiagram(final boolean animate) {
        if (animate) {
            diagram.startPlayback();
        } else {
            diagram.startPlayback();
        }
    }

    protected void help(@NonNull final View sender) {
        UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PILL_PAIRING);
    }

    @Override
    public void showPillPairing() {
        this.isPairing = true;

        activityIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);

        skipButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    @Override
    public void presentError(final Throwable e) {
        this.isPairing = false;

        diagram.suspendPlayback(true);
        presenter.hideAllActivityForFailure(() -> {
            activityIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);

            if (presenter.showSkipButtonOnError()) {
                skipButton.setVisibility(View.VISIBLE);
            }
            retryButton.setVisibility(View.VISIBLE);

            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(e, getActivity());
            errorDialogBuilder.withOperation("Pair Pill");
            if (e instanceof OperationTimeoutException ||
                    SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_message_sleep_pill_scan_timeout));
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.NETWORK_ERROR)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_network_failure_pair_pill));
                errorDialogBuilder.withSupportLink();
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_pill_already_paired));
                errorDialogBuilder.withSupportLink();
            } else {
                errorDialogBuilder.withUnstableBluetoothHelp(getActivity());
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

}
