package is.hello.sense.flows.accountsettings.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.mvp.view.SenseView;

@SuppressLint("ViewConstructor")
public class AccountSettingsView extends SenseView {
    public AccountSettingsView(@NonNull final Activity activity) {
        super(activity);
        //todo support old functions
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.static_recycler;
    }

    @Override
    public void releaseViews() {

    }
}
