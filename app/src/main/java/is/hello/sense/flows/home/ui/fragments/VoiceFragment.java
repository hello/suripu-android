package is.hello.sense.flows.home.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;


import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.flows.home.ui.views.VoiceView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.util.InternalPrefManager;

public class VoiceFragment extends PresenterFragment<VoiceView> {

    //todo redesign how shared preferences work.
    private static final String PREF_NAME = "ACCOUNT_SHARED_PREF";
    private static final String PREF_KEY = "VOICE_WELCOME_CARD";

    private VoiceCommandsAdapter adapter;
    private SharedPreferences sharedPreferences;

    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.adapter = new VoiceCommandsAdapter(getActivity().getLayoutInflater());
            this.presenterView = new VoiceView(getActivity(),
                                               this.adapter);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showWelcomeCard(sharedPreferences.getBoolean(getAccountPrefKey(), false));
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
                         .putBoolean(getAccountPrefKey(), true)
                         .apply();
        showWelcomeCard(true);
    }

    private String getAccountPrefKey() {
        return InternalPrefManager.getAccountId(getActivity()) + PREF_KEY;
    }

}
