package is.hello.sense.flows.voicecommands.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.voice.VoiceCommandTopic;
import is.hello.sense.flows.voicecommands.ui.adapters.VoiceCommandDetailAdapter;
import is.hello.sense.flows.voicecommands.ui.views.VoiceCommandDetailView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class VoiceCommandDetailFragment extends PresenterFragment<VoiceCommandDetailView> {

    private static final String ARGS_TOPIC = VoiceCommandDetailFragment.class.getSimpleName() + ".ARG_TOPIC";

    @Inject
    Picasso picasso;

    public static VoiceCommandDetailFragment newInstance(@NonNull final VoiceCommandTopic topic) {
        final VoiceCommandDetailFragment fragment = new VoiceCommandDetailFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARGS_TOPIC, topic);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new VoiceCommandDetailView(getActivity());
        }
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle args = getArguments();
        final VoiceCommandTopic commandTopic;
        if (args == null || !args.containsKey(ARGS_TOPIC)) {
            commandTopic = null;
        } else {
            commandTopic = (VoiceCommandTopic) getArguments().getSerializable(ARGS_TOPIC);
        }

        if (commandTopic == null || picasso == null) {
            return;
        }
        this.presenterView.setAdapter(new VoiceCommandDetailAdapter(getActivity(),
                                                                    commandTopic,
                                                                    picasso));
    }



}
