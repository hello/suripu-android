package is.hello.sense.flows.nightmode.ui.views;

import android.app.Activity;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.databinding.ViewNightModeBinding;
import is.hello.sense.mvp.view.BindedPresenterView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class NightModeView extends BindedPresenterView<ViewNightModeBinding> {

    @Nullable
    private Listener listener;
    @DrawableRes
    private final int radioOn = R.drawable.radio_on;
    @DrawableRes
    private final int radioOff = R.drawable.radio_off;

    public NightModeView(@NonNull final Activity activity) {
        super(activity);
        setLocationPermissionClickListener(activity,
                                           this.binding.viewNightModeLocationPermission);
        this.binding.viewNightModeOff.setOnClickListener(this::offClickListener);
        this.binding.viewNightModeAlwaysOn.setOnClickListener(this::onClickListener);
        this.binding.viewNightModeScheduled.setOnClickListener(this::scheduleClickListener);
        this.binding.viewNightModeScheduledInfo.setOnClickListener(this::scheduleClickListener);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_night_mode;
    }

    @Override
    public void releaseViews() {
        this.binding.viewNightModeOff.setOnClickListener(null);
        this.binding.viewNightModeAlwaysOn.setOnClickListener(null);
        this.binding.viewNightModeScheduled.setOnClickListener(null);
        this.binding.viewNightModeScheduledInfo.setOnClickListener(null);
        this.listener = null;
    }


    public void offClickListener(final View ignored) {
        if (this.listener == null) {
            return;
        }
        setOffMode();
        this.listener.offModeSelected();
    }

    public void onClickListener(final View ignored) {
        if (this.listener == null) {
            return;
        }
        setAlwaysOnMode();
        this.listener.onModeSelected();
    }


    public void scheduleClickListener(final View ignored) {
        if (this.listener == null) {
            return;
        }
        setScheduledMode();
        this.listener.scheduledModeSelected();
    }

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    public void setOffMode() {
        this.binding.viewNightModeOff.setCompoundDrawablesWithIntrinsicBounds(radioOn, 0, 0, 0);
        this.binding.viewNightModeAlwaysOn.setCompoundDrawablesWithIntrinsicBounds(radioOff, 0, 0, 0);
        this.binding.viewNightModeScheduled.setCompoundDrawablesWithIntrinsicBounds(radioOff, 0, 0, 0);
    }

    public void setAlwaysOnMode() {
        this.binding.viewNightModeOff.setCompoundDrawablesWithIntrinsicBounds(radioOff, 0, 0, 0);
        this.binding.viewNightModeAlwaysOn.setCompoundDrawablesWithIntrinsicBounds(radioOn, 0, 0, 0);
        this.binding.viewNightModeScheduled.setCompoundDrawablesWithIntrinsicBounds(radioOff, 0, 0, 0);
    }

    public void setScheduledMode() {
        this.binding.viewNightModeOff.setCompoundDrawablesWithIntrinsicBounds(radioOff, 0, 0, 0);
        this.binding.viewNightModeAlwaysOn.setCompoundDrawablesWithIntrinsicBounds(radioOff, 0, 0, 0);
        this.binding.viewNightModeScheduled.setCompoundDrawablesWithIntrinsicBounds(radioOn, 0, 0, 0);
    }

    public void setScheduledModeEnabled(final boolean enabled) {
        this.binding.viewNightModeScheduled.setEnabled(enabled);
        this.binding.viewNightModeScheduledInfo.setEnabled(enabled);
        this.binding.viewNightModeLocationPermission.setVisibility(enabled ? GONE : VISIBLE);
    }

    @VisibleForTesting
    protected void setLocationPermissionClickListener(@NonNull final Activity activity,
                                                      @NonNull final TextView permissionTextView) {
        permissionTextView.setText(Styles.resolveSupportLinks(activity, permissionTextView.getText()));
        permissionTextView.setOnTouchListener((v, event) -> {
            if (listener == null) {
                return false;
            }
            final ClickableSpan link = Views.getClickableSpan(((TextView) v), event);
            if (link != null) {
                if (event.getAction() == MotionEvent.ACTION_UP
                        && !listener.onLocationPermissionLinkIntercepted()) {
                    link.onClick(v);
                }
                return true;
            }
            return false;
        });
    }

    public interface Listener {
        void offModeSelected();

        void onModeSelected();

        void scheduledModeSelected();

        boolean onLocationPermissionLinkIntercepted();
    }
}
