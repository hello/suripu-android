package is.hello.sense.flows.nightmode.ui.views;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.RadioGroup;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.databinding.ViewNightModeBinding;
import is.hello.sense.mvp.view.BindedPresenterView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class NightModeView extends BindedPresenterView<ViewNightModeBinding>
implements RadioGroup.OnCheckedChangeListener{

    @Nullable
    private Listener listener;

    public NightModeView(@NonNull final Activity activity) {
        super(activity);
        this.binding.viewNightModeRadioGroup.setOnCheckedChangeListener(this);
        setLocationPermissionClickListener(activity,
                                           this.binding.viewNightModeLocationPermission);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_night_mode;
    }

    @Override
    public void releaseViews() {
        this.binding.viewNightModeRadioGroup.setOnCheckedChangeListener(null);
        this.listener = null;
    }

    @Override
    public void onCheckedChanged(@NonNull final RadioGroup group,
                                 @IdRes final int checkedId) {
        if (listener == null) {
            return;
        }
        switch (checkedId) {
            case R.id.view_night_mode_off_rb:
                listener.offModeSelected();
                break;
            case R.id.view_night_mode_always_on_rb:
                listener.onModeSelected();
                break;
            case R.id.view_night_mode_scheduled_rb:
                listener.scheduledModeSelected();
                break;
            default:
                throw new IllegalStateException("unsupported radio button checked id" + checkedId);
        }
    }

    public void setRadioGroupListener(@Nullable
                                      final Listener listener) {
        this.listener = listener;
    }

    public void setScheduledModeEnabled(final boolean enabled) {
        this.binding.viewNightModeOffRb.setChecked(!enabled && !binding.viewNightModeAlwaysOnRb.isChecked());
        this.binding.viewNightModeScheduledRb.setEnabled(enabled);
        this.binding.viewNightModeScheduledInfo.setEnabled(enabled);
        this.binding.viewNightModeLocationPermission.setVisibility(enabled ? GONE : VISIBLE);
    }

    @VisibleForTesting
    protected void setLocationPermissionClickListener(@NonNull final Activity activity,
                                                      @NonNull final TextView permissionTextView) {
        permissionTextView.setText(Styles.resolveSupportLinks(activity, permissionTextView.getText()));
        permissionTextView.setOnTouchListener( (v, event) -> {
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
