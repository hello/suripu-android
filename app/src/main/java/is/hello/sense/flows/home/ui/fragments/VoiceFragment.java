package is.hello.sense.flows.home.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;


import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.flows.home.ui.views.VoiceView;
import is.hello.sense.flows.voicecommands.ui.activities.VoiceCommandActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountPreferencesInteractor;
import is.hello.sense.mvp.presenters.SubPresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;

public class VoiceFragment extends SubPresenterFragment<VoiceView> implements ArrayRecyclerAdapter.OnItemClickedListener<VoiceCommandsAdapter.VoiceCommand> {

    private VoiceCommandsAdapter adapter;
    AccountPreferencesInteractor sharedPreferences;

    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.adapter = new VoiceCommandsAdapter(getActivity().getLayoutInflater());
            this.adapter.setOnItemClickedListener(this);
            this.presenterView = new VoiceView(getActivity(),
                                               this.adapter);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = AccountPreferencesInteractor.newInstance(getActivity());
        addInteractor(sharedPreferences);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sharedPreferences.observableBoolean(AccountPreferencesInteractor.VOICE_WELCOME_CARD, false),
                         this::showWelcomeCard,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onUserInvisible() {

    }

    @Override
    public void onUserVisible() {

    }

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
        showWelcomeCard(true);
    }


    @Override
    public void onItemClicked(final int position,
                              @NonNull final VoiceCommandsAdapter.VoiceCommand item) {
        final Intent intent = new Intent(getActivity(), VoiceCommandActivity.class);
        intent.putExtra(VoiceCommandActivity.ITEM_KEY, item.name());
        startActivity(intent); //todo undo this one day
    }
}
