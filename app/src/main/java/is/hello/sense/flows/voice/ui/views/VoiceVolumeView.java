package is.hello.sense.flows.voice.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.flows.voice.ui.widgets.VolumePickerView;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class VoiceVolumeView extends PresenterView{

    private final TextView scaleValue;
    private final VolumePickerView scale;
    private final Button doneButton;

    public VoiceVolumeView(@NonNull final Activity activity) {
        super(activity);
        this.scaleValue = (TextView) findViewById(R.id.view_voice_volume_selected_value);
        this.scale = (VolumePickerView) findViewById(R.id.view_voice_volume_scale);
        this.doneButton = (Button) findViewById(R.id.view_voice_volume_done_button);
        this.scale.setOnValueChangedListener(this::updateDisplayValue);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_voice_volume;
    }

    @Override
    public void releaseViews() {
        scale.onDestroyView();
    }

    public void setDoneButtonClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(doneButton, listener);
    }

    public int getVolume(){
        return scale.convertSelectedValueToPercentageValue();
    }

    private void updateDisplayValue(final int value) {
        this.scaleValue.setText(String.valueOf(value));
    }

    public void setVolume(final int volume) {
        final int converted = this.scale.convertFromPercentageValue(volume);
        this.scale.setValue(converted, true);
    }
}
