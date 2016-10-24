package is.hello.sense.flows.voice.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.CompoundButton;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class VoiceSettingsListView extends PresenterView {

    private final TextView volumeValueTextView;
    private final CompoundButton muteSwitch;
    private final TextView primaryUserValueTextView;

    public VoiceSettingsListView(@NonNull final Activity activity) {
        super(activity);

        this.volumeValueTextView = (TextView) findViewById(R.id.view_voice_settings_list_volume_value_tv);
        this.muteSwitch = (CompoundButton) findViewById(R.id.view_voice_settings_mute_switch);
        this.primaryUserValueTextView = (TextView) findViewById(R.id.view_voice_settings_list_primary_user_value_tv);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_voice_settings_list;
    }

    @Override
    public void releaseViews() {
        this.volumeValueTextView.setOnClickListener(null);
        this.muteSwitch.setOnCheckedChangeListener(null);
        this.primaryUserValueTextView.setOnClickListener(null);
    }

    public void setVolumeValueClickListener(@NonNull final OnClickListener listener){
        Views.setTimeOffsetOnClickListener(volumeValueTextView, listener);
    }
}
