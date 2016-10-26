package is.hello.sense.flows.voice.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.CompoundButton;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitOperations;

@SuppressLint("ViewConstructor")
public class VoiceSettingsListView extends PresenterView {

    private final TextView volumeValueTextView;
    private final CompoundButton muteSwitch;
    private final TextView primaryUserValueTextView;
    private final Drawable chevronDrawable;
    private final int chevronPadding;

    public VoiceSettingsListView(@NonNull final Activity activity) {
        super(activity);

        this.volumeValueTextView = (TextView) findViewById(R.id.view_voice_settings_list_volume_value_tv);
        this.muteSwitch = (CompoundButton) findViewById(R.id.view_voice_settings_mute_switch);
        this.primaryUserValueTextView = (TextView) findViewById(R.id.view_voice_settings_list_primary_user_value_tv);
        this.chevronDrawable = ContextCompat.getDrawable(context, R.drawable.disclosure_chevron_small);
        this.chevronPadding = getResources().getDimensionPixelSize(R.dimen.x1);
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

    public void updateMuteSwitch(final boolean isChecked,
                                 @NonNull final CompoundButton.OnCheckedChangeListener listener){
        this.muteSwitch.setOnCheckedChangeListener(null);
        this.muteSwitch.setChecked(isChecked);
        this.muteSwitch.setOnCheckedChangeListener(listener);
        this.muteSwitch.setVisibility(VISIBLE);
    }

    public void updateVolumeTextView(@NonNull final SenseVoiceSettings settings) {
        this.volumeValueTextView.setText(String.valueOf(
                UnitOperations.percentageToLevel(settings.getVolume(),
                                                 SenseVoiceSettings.TOTAL_VOLUME_LEVELS)));
        this.volumeValueTextView.setVisibility(VISIBLE);

    }

    public void makePrimaryUser() {
        primaryUserValueTextView.setOnClickListener(null);
        primaryUserValueTextView.setText(R.string.voice_settings_primary_user_true);
        primaryUserValueTextView.setCompoundDrawables(null, null, null, null);
        primaryUserValueTextView.setEnabled(false);
        this.primaryUserValueTextView.setVisibility(VISIBLE);
    }

    public void makeSecondaryUser(@NonNull final OnClickListener listener) {
        primaryUserValueTextView.setOnClickListener(listener);
        primaryUserValueTextView.setText(R.string.voice_settings_primary_user_false);
        primaryUserValueTextView.setEnabled(true);
        primaryUserValueTextView.setCompoundDrawables(null, null, chevronDrawable, null);
        primaryUserValueTextView.setCompoundDrawablePadding(chevronPadding);
        this.primaryUserValueTextView.setVisibility(VISIBLE);
    }
}
