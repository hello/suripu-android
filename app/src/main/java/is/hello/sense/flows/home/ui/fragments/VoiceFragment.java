package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.voice.VoiceCommandTopic;
import is.hello.sense.flows.home.interactors.VoiceCommandResponseInteractor;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.flows.home.ui.views.VoiceView;
import is.hello.sense.flows.voicecommands.ui.activities.VoiceCommandDetailActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountPreferencesInteractor;
import is.hello.sense.mvp.presenters.ControllerPresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.NotTested;

@NotTested // enough
public class VoiceFragment extends ControllerPresenterFragment<VoiceView>
        implements
        VoiceView.Listener,
        ArrayRecyclerAdapter.ErrorHandler,
        HomeActivity.ScrollUp {

    private AccountPreferencesInteractor sharedPreferences;

    @Inject
    VoiceCommandResponseInteractor voiceCommandResponseInteractor;

    @Inject
    Picasso picasso;

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            final VoiceCommandsAdapter adapter = new VoiceCommandsAdapter(getActivity(),
                                                                          this.picasso);
            adapter.setErrorHandler(this);
            this.presenterView = new VoiceView(getActivity(),
                                               adapter,
                                               this);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sharedPreferences = AccountPreferencesInteractor.newInstance(getActivity());
        addInteractor(this.sharedPreferences);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(this.sharedPreferences.observableBoolean(AccountPreferencesInteractor.VOICE_WELCOME_CARD, false),
                         this::showWelcomeCard,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.voiceCommandResponseInteractor.commands,
                         this.presenterView::bindVoiceCommands,
                         this::presentError);
    }
    //endregion

    //region Controller
    @Override
    public void setVisibleToUser(final boolean isVisible) {
        super.setVisibleToUser(isVisible);
        if (isVisible) {
            Analytics.trackEvent(Analytics.Backside.EVENT_VOICE_TAB, null);
            if (this.voiceCommandResponseInteractor != null) {
                this.voiceCommandResponseInteractor.update();
            }
        }
    }
    //endregion

    //region  VoiceView.Listener
    @Override
    public void onTopicClicked(@NonNull final VoiceCommandTopic item) {
        Analytics.trackEvent(Analytics.Backside.EVENT_VOICE_EXAMPLES,
                             Analytics.createProperties(Analytics.Backside.PROP_VOICE_EXAMPLES, item.getTitle()));
        startActivity(VoiceCommandDetailActivity.getIntent(getActivity(), item));
    }
    //endregion

    //region ErrorHandler
    @Override
    public void retry() {
        this.presenterView.showProgress();
        this.voiceCommandResponseInteractor.update();
    }
    //endregion

    //region ScrollUp
    @Override
    public void scrollUp() {
        if (this.presenterView == null) {
            return;
        }
        this.presenterView.scrollUp();
    }
    //endregion

    //region methods
    private void showWelcomeCard(final boolean wasShown) {
        if (wasShown) {
            this.presenterView.showWelcomeCard(null);
        } else {
            this.presenterView.showWelcomeCard(this::onWelcomeCardCloseClicked);
        }
    }

    private void onWelcomeCardCloseClicked(@NonNull final View ignored) {
        this.sharedPreferences.edit()
                              .putBoolean(AccountPreferencesInteractor.VOICE_WELCOME_CARD, true)
                              .apply();
    }

    private void presentError(@NonNull final Throwable throwable) {
        this.presenterView.showError();

    }
    //endregion

}
