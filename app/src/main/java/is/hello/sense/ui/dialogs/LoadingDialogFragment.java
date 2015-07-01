package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.SenseDialogFragment;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public final class LoadingDialogFragment extends SenseDialogFragment {
    public static final String TAG = LoadingDialogFragment.class.getSimpleName();

    private static final long DURATION_DONE_MESSAGE = 2 * 1000;

    private static final String ARG_TITLE = LoadingDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_FLAGS = LoadingDialogFragment.class.getName() + ".ARG_FLAGS";
    private static final String ARG_HEIGHT = LoadingDialogFragment.class.getName() + ".ARG_HEIGHT";
    private static final String ARG_GRAVITY = LoadingDialogFragment.class.getName() + ".ARG_GRAVITY";
    private static final String ARG_DISMISS_MSG = LoadingDialogFragment.class.getName() + ".ARG_DISMISS_MSG";


    //region Config

    public static final int OPAQUE_BACKGROUND = (1 << 1);
    public static final int DIM_ENABLED = (1 << 2);
    public static final int DEFAULTS = 0;

    @IntDef(flag = true, value = {DEFAULTS, OPAQUE_BACKGROUND, DIM_ENABLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Config {}

    //endregion


    private TextView titleText;
    private ProgressBar activityIndicator;
    private ImageView checkMark;


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

            Window window = dialog.getWindow();
            WindowManager.LayoutParams windowAttributes = window.getAttributes();

            if ((flags & DIM_ENABLED) == DIM_ENABLED) {
                window.setDimAmount(0.5f);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            if (arguments.containsKey(ARG_HEIGHT)) {
                root.getLayoutParams().height = arguments.getInt(ARG_HEIGHT);
                root.requestLayout();
                window.setLayout(windowAttributes.width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            window.setGravity(arguments.getInt(ARG_GRAVITY, windowAttributes.gravity));
        }

        return dialog;
    }

    //endregion


    //region Attributes

    public void setTitle(@Nullable String title) {
        getArguments().putString(ARG_TITLE, title);
        if (titleText != null) {
            titleText.setText(title);
        }
    }

    public void setHeight(int height) {
        getArguments().putInt(ARG_HEIGHT, height);
    }

    public void setGravity(int gravity) {
        getArguments().putInt(ARG_GRAVITY, gravity);
    }

    public void setDismissMessage(@StringRes int messageRes) {
        getArguments().putInt(ARG_DISMISS_MSG, messageRes);
    }

    //endregion


    //region Dismissing

    public void dismissWithDoneTransition(@Nullable Runnable onCompletion) {
        if (titleText != null) {
            animate(activityIndicator)
                    .setDuration(Animation.DURATION_FAST)
                    .fadeOut(View.INVISIBLE)
                    .start();

            animate(titleText)
                    .setDuration(Animation.DURATION_FAST)
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        if (!finished)
                            return;

                        checkMark.setRotation(45f);
                        animate(checkMark)
                                .zoomInFrom(0f)
                                .rotation(0f)
                                .start();


                        int messageRes = getArguments().getInt(ARG_DISMISS_MSG, R.string.action_done);
                        if (messageRes != 0) {
                            titleText.setText(messageRes);
                        } else {
                            titleText.setText(null);
                        }

                        animate(titleText)
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
