package is.hello.sense.flows.voice.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitOperations;

@SuppressLint("ViewConstructor")
public class VoiceSettingsListView extends PresenterView {

    private final ViewGroup volumeViewGroup;
    private final ViewGroup primaryUserGroup;
    private final TextView volumeValueTextView;
    private final CompoundButton muteSwitch;
    private final TextView primaryUserValueTextView;

    public VoiceSettingsListView(@NonNull final Activity activity) {
        super(activity);

        this.volumeViewGroup = (ViewGroup) findViewById(R.id.view_voice_settings_list_volume_container);
        this.volumeValueTextView = (TextView) volumeViewGroup.findViewById(R.id.view_voice_settings_list_volume_value_tv);
        this.muteSwitch = (CompoundButton) findViewById(R.id.view_voice_settings_mute_switch);
        this.primaryUserGroup = (ViewGroup) findViewById(R.id.view_voice_settings_list_primary_user_container);
        this.primaryUserValueTextView = (TextView) primaryUserGroup.findViewById(R.id.view_voice_settings_list_primary_user_value_tv);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_voice_settings_list;
    }

    @Override
    public void releaseViews() {
        this.volumeViewGroup.setOnClickListener(null);
        this.muteSwitch.setOnCheckedChangeListener(null);
        this.primaryUserGroup.setOnClickListener(null);
    }

    public void setVolumeClickListener(@NonNull final OnClickListener listener) {
        Views.setTimeOffsetOnClickListener(volumeViewGroup, listener);
    }

    public void updateMuteSwitch(final boolean isChecked,
                                 @NonNull final CompoundButton.OnCheckedChangeListener listener) {
        this.muteSwitch.setOnCheckedChangeListener(null);
        this.muteSwitch.setChecked(isChecked);
        this.muteSwitch.setOnCheckedChangeListener(listener);
        this.muteSwitch.setVisibility(VISIBLE);
    }

    public void flipMuteSwitch(@NonNull final CompoundButton.OnCheckedChangeListener listener) {
        updateMuteSwitch(!this.muteSwitch.isChecked(), listener);
    }

    public void updateVolumeTextView(@NonNull final SenseVoiceSettings settings) {
        this.volumeValueTextView.setText(String.valueOf(
                UnitOperations.percentageToLevel(settings.getVolumeOrDefault(),
                                                 SenseVoiceSettings.TOTAL_VOLUME_LEVELS)));
        this.volumeValueTextView.setVisibility(VISIBLE);
    }

    public void makePrimaryUser() {
        primaryUserGroup.setOnClickListener(null);
        primaryUserGroup.setEnabled(false);
        primaryUserValueTextView.setText(R.string.voice_settings_primary_user_true);
        primaryUserValueTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        primaryUserValueTextView.setEnabled(false); // required to change text color
        this.primaryUserValueTextView.setVisibility(VISIBLE);
    }

    public void makeSecondaryUser(@NonNull final OnClickListener listener) {
        primaryUserGroup.setOnClickListener(listener);
        primaryUserGroup.setEnabled(true);
        primaryUserValueTextView.setText(R.string.voice_settings_primary_user_false);
        primaryUserValueTextView.setEnabled(true);  // required to change text color
        primaryUserValueTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.disclosure_chevron_small, 0);
        this.primaryUserValueTextView.setVisibility(VISIBLE);
    }
}
