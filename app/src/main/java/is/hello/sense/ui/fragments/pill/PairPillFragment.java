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

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.presenters.pairpill.BasePairPillPresenter;
import is.hello.sense.ui.common.OnboardingToolbar;
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

    @Override
    protected BasePairPillPresenter getPresenter() {
        return presenter;
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
        OnboardingToolbar.of(this, view)
                         .setWantsBackButton(presenter.wantsBackButton())
                         .setOnHelpClickListener(presenter::onHelpClick);

        if (BuildConfig.DEBUG) {
            diagram.setOnLongClickListener(ignored -> {
                presenter.skipPairingPill(getActivity());
                return true;
            });
            diagram.setBackgroundResource(R.drawable.selectable_dark);
        }
        return view;
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

    @Override
    public void finishedPairing(final boolean success) {
        presenter.finishedPairingAction(getActivity(),
                                        success);
    }

    @Override
    public void animateDiagram(final boolean animate) {
        if (animate) {
            diagram.startPlayback();
        } else {
            diagram.startPlayback();
        }
    }


    @Override
    public void showPillPairing() {
        activityIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);

        skipButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    @Override
    public void showError() {
        diagram.suspendPlayback(true);
        activityIndicator.setVisibility(View.GONE);
        activityStatus.setVisibility(View.GONE);

        if (presenter.showSkipButtonOnError()) {
            skipButton.setVisibility(View.VISIBLE);
        }
        retryButton.setVisibility(View.VISIBLE);

    }

}
