package is.hello.sense.flows.voicecommands.ui.activities;


import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.v2.voice.VoiceCommandTopic;
import is.hello.sense.flows.voicecommands.ui.fragments.VoiceCommandDetailFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;

public class VoiceCommandDetailActivity extends FragmentNavigationActivity {
    private static final String EXTRA_ITEM = VoiceCommandDetailActivity.class.getSimpleName() + ".EXTRA_ITEM";

    public static Intent getIntent(@NonNull final Context context, @NonNull final VoiceCommandTopic topic) {
        final Intent intent = new Intent(context, VoiceCommandDetailActivity.class);
        intent.putExtra(VoiceCommandDetailActivity.EXTRA_ITEM, topic);
        return intent;
    }

    @Override
    protected void onCreateAction() {
        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_ITEM)) {
            finish();
            return;
        }
        final VoiceCommandTopic topic = (VoiceCommandTopic) intent.getSerializableExtra(EXTRA_ITEM);
        pushFragment(VoiceCommandDetailFragment.newInstance(topic), null, false);
    }

}
