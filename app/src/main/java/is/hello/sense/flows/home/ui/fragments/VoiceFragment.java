package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.flows.home.interactors.VoiceCommandsInteractor;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.flows.home.ui.views.VoiceView;
import is.hello.sense.flows.voicecommands.ui.activities.VoiceCommandActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountPreferencesInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.util.ViewPagerPresenterChild;
import is.hello.sense.mvp.util.ViewPagerPresenterChildDelegate;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.NotTested;

@NotTested // enough
public class VoiceFragment extends PresenterFragment<VoiceView>
        implements
        ArrayRecyclerAdapter.OnItemClickedListener<VoiceCommandsAdapter.VoiceCommand>,
        ViewPagerPresenterChild,
        HomeActivity.ScrollUp {

    private final ViewPagerPresenterChildDelegate presenterChildDelegate = new ViewPagerPresenterChildDelegate(this);
    private VoiceCommandsAdapter adapter;

    private AccountPreferencesInteractor sharedPreferences;
    @Inject
    VoiceCommandsInteractor voiceCommandsInteractor;

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.adapter = new VoiceCommandsAdapter(getActivity().getLayoutInflater());
            this.adapter.setOnItemClickedListener(this);
            this.presenterView = new VoiceView(getActivity(),
                                               this.adapter);
            this.presenterChildDelegate.onViewInitialized();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = AccountPreferencesInteractor.newInstance(getActivity());
        addInteractor(sharedPreferences);
        addInteractor(voiceCommandsInteractor);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sharedPreferences.observableBoolean(AccountPreferencesInteractor.VOICE_WELCOME_CARD, false),
                         this::showWelcomeCard,
                         Functions.LOG_ERROR);
        bindAndSubscribe(voiceCommandsInteractor.voiceCommands,
                         voiceResponse -> {
                             Log.e("VoiceFragment", voiceResponse.toString());
                             //todo finish
                         },
                         Functions.LOG_ERROR); //todo show error.
        voiceCommandsInteractor.update();
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.presenterChildDelegate.setUserVisibleHint(isVisibleToUser);
    }

    //endRegion
    //region ViewPagerPresenterChild
    @Override
    public void onUserInvisible() {

    }

    @Override
    public void onUserVisible() {
        Analytics.trackEvent(Analytics.Backside.EVENT_VOICE_TAB, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.presenterChildDelegate.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.presenterChildDelegate.onPause();
    }

    //endregion

    //region  ArrayRecyclerAdapter.OnItemClickedListener
    @Override
    public void onItemClicked(final int position,
                              @NonNull final VoiceCommandsAdapter.VoiceCommand item) {
        Analytics.trackEvent(Analytics.Backside.EVENT_VOICE_EXAMPLES,
                             Analytics.createProperties(Analytics.Backside.PROP_VOICE_EXAMPLES, item));
        startActivity(VoiceCommandActivity.getIntent(getActivity(),
                                                     item.name()));
    }
    //endregion

    //region scrollup
    @Override
    public void scrollUp() {
        if (presenterView == null) {
            return;
        }
        presenterView.scrollUp();
    }
    //endregion

    //region methods
    private void showWelcomeCard(final boolean wasShown) {
        if (wasShown) {
            this.adapter.showWelcomeCard(null);
        } else {
            this.adapter.showWelcomeCard(this::onWelcomeCardCloseClicked);
        }
        this.presenterView.setInsetForWelcomeCard(!wasShown);
    }

    private void onWelcomeCardCloseClicked(@NonNull final View ignored) {
        sharedPreferences.edit()
                         .putBoolean(AccountPreferencesInteractor.VOICE_WELCOME_CARD, true)
                         .apply();
    }

    //endregion

}
