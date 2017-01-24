package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.gson.reflect.TypeToken;
import com.segment.analytics.Properties;

import java.io.Serializable;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.alerts.DialogViewModel;
import is.hello.sense.databinding.DialogBottomAlertBinding;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseBottomAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

import static is.hello.go99.animators.MultiAnimator.animatorFor;
/**
 * Used specifically for displaying {@link DialogViewModel} in a bottom dialog.
 */
public abstract class BottomAlertDialogFragment<T extends DialogViewModel> extends SenseDialogFragment {

    public static final String TAG = BottomAlertDialogFragment.class.getName() + "TAG";

    protected static final String ARG_ALERT = BottomAlertDialogFragment.class.getName() + "ARG_ALERT";

    protected T alert;

    protected DialogBottomAlertBinding binding;

    abstract T getEmptyDialogViewModelInstance();

    abstract void onPositiveButtonClicked();

    public void onNeutralButtonClicked() {
        //do nothing
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setAlertFrom(getArguments());

            final Properties properties = Analytics.createProperties(
                    Analytics.Timeline.PROP_SYSTEM_ALERT_TYPE, alert.getAnalyticPropertyType());

            Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT, properties);
        } else {
            setAlertFrom(savedInstanceState);
        }
    }

    @CallSuper
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.dialog_bottom_alert, container, false);

        this.binding.dialogBottomAlert.setText(alert.getTitle());
        this.binding.dialogBottomAlertMessage.setText(alert.getBody());
        this.binding.dialogBottomAlertMessage.setMovementMethod(LinkMovementMethod.getInstance());

        this.binding.dialogBottomAlertNeutral.setText(alert.getNeutralButtonText());
        Views.setTimeOffsetOnClickListener(this.binding.dialogBottomAlertNeutral,
                                           ignore -> {
                                               onNeutralButtonClicked();
                                               dismissSafely();
                                           });

        this.binding.dialogBottomAlertPositive.setText(alert.getPositiveButtonText());
        Views.setTimeOffsetOnClickListener(this.binding.dialogBottomAlertPositive,
                                           ignore -> {
                                               onPositiveButtonClicked();
                                               dismissSafely();
                                           });
        return this.binding.getRoot();
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        return new SenseBottomAlertDialog(getActivity());
    }

    @CallSuper
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_ALERT, alert);
    }

    @CallSuper
    @Override
    public void dismissSafely() {
        if(!isAdded() || getShowsDialog()) {
            super.dismissSafely();
            return;
        }
        //fragment transaction to remove does not properly call R.animator.bottom_slide_down
        //so this is alternative
        animatorFor(this.binding.getRoot())
                .withDuration(Anime.DURATION_NORMAL)
                .withInterpolator(new AccelerateDecelerateInterpolator())
                .fadeOut(View.GONE)
                .translationY(this.binding.getRoot().getHeight())
                .addOnAnimationCompleted( isComplete -> {
            if(isComplete) {
                super.dismissSafely();
            }
        }).start();
    }

    /**
     * @param containerResId will treat as fragment and attempt to add into this container.
     *                       Dialog lifecycle methods like {@link android.app.DialogFragment#onCreateDialog(Bundle)}
     *                       are not called, so {@link android.app.Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)} must be implemented.
     */
    public void showAllowingStateLoss(@NonNull final FragmentManager fm,
                                      final int containerResId,
                                      @NonNull final String tag) {
        fm.beginTransaction()
          .setTransition(FragmentTransaction.TRANSIT_NONE)
          .setCustomAnimations(R.animator.bottom_slide_up,
                               R.animator.bottom_slide_down,
                               R.animator.bottom_slide_up,
                               R.animator.bottom_slide_down)
          .add(containerResId, this, tag)
          .commitAllowingStateLoss();
    }

    @SuppressWarnings("unchecked")
    private void setAlertFrom(@Nullable final Bundle bundle) {
        if (bundle == null) {
            setAlert(null);
            return;
        }
        final Serializable serializedAlert = bundle.getSerializable(ARG_ALERT);
        if (serializedAlert instanceof DialogViewModel) {
            try {
                setAlert((T) serializedAlert);
            } catch (final ClassCastException e) {
                Logger.error(BottomAlertDialogFragment.TAG, ARG_ALERT + " was not expecting class " + new TypeToken<T>(){}.getType(), e);
                setAlert(null);
            }
        } else {
            setAlert(null);
        }
    }

    private void setAlert(@Nullable final T alert) {
        if (alert == null) {
            this.alert = getEmptyDialogViewModelInstance();
            Logger.error(BottomAlertDialogFragment.TAG, " requires non null DialogViewModel object passed in arguments");
        } else {
                this.alert = alert;
        }
    }
}
