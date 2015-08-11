package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.MultiAnimator;
import is.hello.sense.ui.common.SenseDialogFragment;

import static is.hello.sense.ui.animation.MultiAnimator.animatorFor;

public final class LoadingDialogFragment extends SenseDialogFragment {
    public static final String TAG = LoadingDialogFragment.class.getSimpleName();

    private static final long DURATION_DONE_MESSAGE = 2 * 1000;

    private static final String ARG_TITLE = LoadingDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_FLAGS = LoadingDialogFragment.class.getName() + ".ARG_FLAGS";
    private static final String ARG_DISMISS_MSG = LoadingDialogFragment.class.getName() + ".ARG_DISMISS_MSG";
    private static final String ARG_LOCK_ORIENTATION = LoadingDialogFragment.class.getName() + ".ARG_LOCK_ORIENTATION";


    //region Config

    public static final int OPAQUE_BACKGROUND = (1 << 1);
    public static final int DEFAULTS = 0;

    @IntDef(flag = true, value = {DEFAULTS, OPAQUE_BACKGROUND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Config {}

    //endregion


    private TextView titleText;
    private ProgressBar activityIndicator;
    private ImageView checkMark;

    private @Nullable Integer oldOrientation;


    //region Shortcuts

    public static @NonNull LoadingDialogFragment show(@NonNull FragmentManager fm,
                                                      @Nullable String title,
                                                      int flags) {
        LoadingDialogFragment preexistingDialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (preexistingDialog != null) {
            preexistingDialog.dismiss();
        }

        LoadingDialogFragment dialog = LoadingDialogFragment.newInstance(title, flags);
        dialog.show(fm, TAG);

        return dialog;
    }

    public static @NonNull LoadingDialogFragment show(@NonNull FragmentManager fm) {
        return show(fm, null, DEFAULTS);
    }

    public static void close(@NonNull FragmentManager fm) {
        LoadingDialogFragment dialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (dialog != null) {
            dialog.dismissSafely();
        }
    }

    public static void closeWithDoneTransition(@NonNull FragmentManager fm, @Nullable Runnable onCompletion) {
        LoadingDialogFragment dialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (dialog != null) {
            dialog.dismissWithDoneTransition(onCompletion);
        } else if (onCompletion != null) {
            onCompletion.run();
        }
    }

    //endregion


    //region Lifecycle

    public static LoadingDialogFragment newInstance(@Nullable String title, @Config int flags) {
        LoadingDialogFragment fragment = new LoadingDialogFragment();

        Bundle arguments = new Bundle();
        if (!TextUtils.isEmpty(title)) {
            arguments.putString(ARG_TITLE, title);
        }
        arguments.putInt(ARG_FLAGS, flags);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Loading);

        dialog.setContentView(R.layout.fragment_dialog_loading);
        dialog.setCanceledOnTouchOutside(false);

        this.titleText = (TextView) dialog.findViewById(R.id.fragment_dialog_loading_title);
        this.activityIndicator = (ProgressBar) dialog.findViewById(R.id.fragment_dialog_loading_bar);
        this.checkMark = (ImageView) dialog.findViewById(R.id.fragment_dialog_loading_check_mark);

        if (getArguments() != null) {
            Bundle arguments = getArguments();
            View root = dialog.findViewById(R.id.fragment_dialog_loading_container);

            @Config int flags = arguments.getInt(ARG_FLAGS, DEFAULTS);

            if ((flags & OPAQUE_BACKGROUND) == OPAQUE_BACKGROUND) {
                root.setBackgroundColor(getResources().getColor(R.color.background));
            }

            titleText.setText(arguments.getString(ARG_TITLE));

            boolean lockOrientation = getArguments().getBoolean(ARG_LOCK_ORIENTATION, false);
            if (lockOrientation) {
                Activity activity = getActivity();
                oldOrientation = activity.getRequestedOrientation();
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        }

        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (oldOrientation != null) {
            getActivity().setRequestedOrientation(oldOrientation);
        }
    }

    //endregion


    //region Attributes

    public void setTitle(@Nullable String title) {
        getArguments().putString(ARG_TITLE, title);
        if (titleText != null) {
            titleText.setText(title);
        }
    }

    public void setDismissMessage(@StringRes int messageRes) {
        getArguments().putInt(ARG_DISMISS_MSG, messageRes);
    }

    /**
     * Causes orientation changes to be blocked for the
     * duration of the loading dialog being visible.
     * <p />
     * Probably not something we want to support long-term.
     */
    public void setLockOrientation() {
        getArguments().putBoolean(ARG_LOCK_ORIENTATION, true);
    }

    //endregion


    //region Dismissing

    public void dismissWithDoneTransition(@Nullable Runnable onCompletion) {
        if (titleText != null) {
            animatorFor(activityIndicator)
                    .withDuration(Animation.DURATION_FAST)
                    .fadeOut(View.INVISIBLE)
                    .start();

            animatorFor(titleText)
                    .withDuration(Animation.DURATION_FAST)
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        if (!finished)
                            return;

                        animatorFor(checkMark)
                                .addOnAnimationWillStart(() -> {
                                    checkMark.setAlpha(0f);
                                    checkMark.setScaleX(0f);
                                    checkMark.setScaleY(0f);
                                    checkMark.setVisibility(View.VISIBLE);
                                })
                                .alpha(1f)
                                .scale(1f)
                                .start();


                        int messageRes = getArguments().getInt(ARG_DISMISS_MSG, R.string.action_done);
                        if (messageRes != 0) {
                            titleText.setText(messageRes);
                        } else {
                            titleText.setText(null);
                        }

                        animatorFor(titleText)
                                .fadeIn()
                                .addOnAnimationCompleted(finished1 -> {
                                    if (!finished1)
                                        return;

                                    titleText.postDelayed(() -> {
                                        if (onCompletion != null) {
                                            onCompletion.run();
                                        }
                                        dismissSafely();
                                    }, DURATION_DONE_MESSAGE);
                                })
                                .start();
                    })
                    .start();
        } else if (isAdded()) {
            dismiss();
        }
    }

    //endregion
}
