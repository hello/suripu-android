package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;

@SuppressLint("ViewConstructor")
public class VoiceView extends PresenterView {

    public VoiceView(@NonNull final Activity activity) {
        super(activity);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_voice;
    }

    @Override
    public void releaseViews() {

    }
}
