package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.segment.analytics.Properties;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.model.v2.alerts.AlertDialogViewModel;
import is.hello.sense.databinding.DialogBottomAlertBinding;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseBottomAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

/**
 * Used specifically for displaying {@link Alert} objects in a bottom dialog.
 */
public class BottomAlertDialogFragment extends SenseDialogFragment {

    public static final String TAG = BottomAlertDialogFragment.class.getName() + "TAG";

    private static final String ARG_ALERT = BottomAlertDialogFragment.class.getName() + "ARG_ALERT";

    private AlertDialogViewModel alert;

    private DialogBottomAlertBinding binding;

    public static BottomAlertDialogFragment newInstance(@NonNull final Alert alert, @NonNull final Resources resources) {

        final Bundle args = new Bundle();
        args.putSerializable(ARG_ALERT, new AlertDialogViewModel(alert, resources));
        final BottomAlertDialogFragment fragment = new BottomAlertDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setAlert((AlertDialogViewModel) getArguments().getSerializable(ARG_ALERT));

            final Properties properties = Analytics.createProperties(
                    Analytics.Timeline.PROP_SYSTEM_ALERT_TYPE, alert.getCategory());

            Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT, properties);
        } else {
            setAlert((AlertDialogViewModel) savedInstanceState.getSerializable(ARG_ALERT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.dialog_bottom_alert, container, false);

        this.binding.dialogBottomAlert.setText(alert.getTitle());
        this.binding.dialogBottomAlertMessage.setText(alert.getBody());

        this.binding.dialogBottomAlertNeutral.setText(alert.neutralButtonText);
        this.binding.dialogBottomAlertNeutral.setOnClickListener(ignore -> dismissSafely());

        this.binding.dialogBottomAlertPositive.setText(alert.positiveButtonText);
        this.binding.dialogBottomAlertPositive.setOnClickListener(ignore -> {
            dispatchAlertAction();
            dismissSafely();
        });
        return this.binding.getRoot();
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        return new SenseBottomAlertDialog(getActivity());
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_ALERT, alert);
    }

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

    private void setAlert(@Nullable final AlertDialogViewModel alert) {
        if(alert == null){
            this.alert = AlertDialogViewModel.NewEmptyInstance(getResources());
            Logger.error(BottomAlertDialogFragment.TAG, " requires non null Alert object passed in arguments");
        } else {
            this.alert = alert;
        }
    }

    private void dispatchAlertAction(){
        switch (alert.getCategory()){
            case SENSE_MUTED:
                if(getActivity() instanceof Alert.ActionHandler){
                    ((Alert.ActionHandler) getActivity()).unMuteSense();
                }
                break;
            case EXPANSION_UNREACHABLE:
            case UNKNOWN:
            default:
                //do nothing
        }
    }
}
