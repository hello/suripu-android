package is.hello.sense.flows.voicecommands.ui.activities;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.ui.activities.appcompat.SenseActivity;

//todo redo this activity
public class VoiceCommandActivity extends SenseActivity {
    private static final String EXTRA_ITEM = VoiceCommandActivity.class.getSimpleName() + ".EXTRA_ITEM";

    public static Intent getIntent(@NonNull final Context context, @NonNull final String itemKeyName) {
        final Intent intent = new Intent(context, VoiceCommandActivity.class);
        intent.putExtra(VoiceCommandActivity.EXTRA_ITEM, itemKeyName);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_ITEM)) {
            finish();
            return;
        }
        final VoiceCommandsAdapter.VoiceCommand voiceCommand = VoiceCommandsAdapter.VoiceCommand.fromString(intent.getStringExtra(EXTRA_ITEM));
        switch (voiceCommand) {
            case ALARM:
                setContentView(R.layout.voice_sounds);
                break;
            case SLEEP:
                setContentView(R.layout.voice_sleep);
                break;
            case ROOM:
                setContentView(R.layout.voice_room);
                break;
            case EXPANSIONS:
                setContentView(R.layout.voice_expansions);
                break;
            default:
                finish();
        }
    }
}
