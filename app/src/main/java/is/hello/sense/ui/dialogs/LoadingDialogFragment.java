package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.SenseDialogFragment;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class LoadingDialogFragment extends SenseDialogFragment {
    public static final String TAG = LoadingDialogFragment.class.getSimpleName();

    protected static final long DURATION_DONE_MESSAGE = 2 * 1000;

    protected static final String ARG_TITLE = LoadingDialogFragment.class.getName() + ".ARG_TITLE";
    protected static final String ARG_WANTS_OPAQUE_BACKGROUND = LoadingDialogFragment.class.getName() + ".ARG_WANTS_OPAQUE_BACKGROUND";
    protected static final String ARG_HEIGHT = LoadingDialogFragment.class.getName() + ".ARG_HEIGHT";
    protected static final String ARG_WINDOW_GRAVITY = LoadingDialogFragment.class.getName() + ".ARG_WINDOW_GRAVITY";
    protected static final String ARG_WINDOW_DIMMED = LoadingDialogFragment.class.getName() + ".ARG_WINDOW_DIMMED";

    private TextView titleText;
    private ProgressBar activityIndicator;
    private ImageView checkMark;

    public static @NonNull LoadingDialogFragment show(@NonNull FragmentManager fm,
                                                      @Nullable String title,
                                                      boolean wantsOpaqueBackground) {
        LoadingDialogFragment preexistingDialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (preexistingDialog != null) {
            preexistingDialog.dismiss();
        }

        LoadingDialogFragment dialog = LoadingDialogFragment.newInstance(title, wantsOpaqueBackground);
        dialog.show(fm, TAG);

        return dialog;
    }

    public static @NonNull LoadingDialogFragment show(@NonNull FragmentManager fm) {
        return show(fm, null, false);
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


    public static LoadingDialogFragment newInstance(@Nullable String title, boolean wantsOpaqueBackground) {
        LoadingDialogFragment fragment = new LoadingDialogFragment();

        Bundle arguments = new Bundle();
        if (!TextUtils.isEmpty(title)) {
            arguments.putString(ARG_TITLE, title);
        }
        arguments.putBoolean(ARG_WANTS_OPAQUE_BACKGROUND, wantsOpaqueBackground);
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

            if (arguments.getBoolean(ARG_WANTS_OPAQUE_BACKGROUND, false)) {
                root.setBackgroundColor(getResources().getColor(R.color.background));
            }

            titleText.setText(arguments.getString(ARG_TITLE));

            Window window = dialog.getWindow();
            WindowManager.LayoutParams windowAttributes = window.getAttributes();

            if (arguments.getBoolean(ARG_WINDOW_DIMMED)) {
                window.setDimAmount(0.5f);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            if (arguments.containsKey(ARG_HEIGHT)) {
                root.getLayoutParams().height = arguments.getInt(ARG_HEIGHT);
                root.requestLayout();
                window.setLayout(windowAttributes.width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            window.setGravity(arguments.getInt(ARG_WINDOW_GRAVITY, windowAttributes.gravity));
        }

        return dialog;
    }

    public void setTitle(@Nullable String title) {
        getArguments().putString(ARG_TITLE, title);
        if (titleText != null) {
            titleText.setText(title);
        }
    }

    public void setHeight(int height) {
        getArguments().putInt(ARG_HEIGHT, height);
    }

    public void setWindowGravity(int gravity) {
        getArguments().putInt(ARG_WINDOW_GRAVITY, gravity);
    }

    public void setWindowDimmed(boolean dimmed) {
        getArguments().putBoolean(ARG_WINDOW_DIMMED, dimmed);
    }

    public void dismissWithDoneTransition(@Nullable Runnable onCompletion) {
        if (titleText != null) {
            animate(titleText)
                    .setDuration(Animation.DURATION_FAST)
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        if (!finished)
                            return;

                        animate(activityIndicator)
                                .fadeOut(View.INVISIBLE)
                                .start();

                        animate(checkMark)
                                .zoomInFrom(0f)
                                .start();

                        if (!TextUtils.isEmpty(titleText.getText())) {
                            titleText.setText(R.string.action_done);
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
}
