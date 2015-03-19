package is.hello.sense.ui.handholding;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.handholding.util.DismissTutorialsDialog;
import is.hello.sense.ui.handholding.util.EventDelegatingDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Constants;

import static android.widget.RelativeLayout.ALIGN_PARENT_LEFT;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.LayoutParams;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class TutorialOverlayFragment extends SenseDialogFragment implements EventDelegatingDialog.EventForwarder {
    public static final String TAG = TutorialOverlayFragment.class.getSimpleName();

    private static final int REQUEST_CODE_DISMISS_ALL = 0x99;

    public static final int RESULT_COMPLETED = 0x55;
    public static final int RESULT_CANCELED = 0x54;

    private static final String ARG_TUTORIAL = TutorialOverlayFragment.class.getName() + ".ARG_TUTORIAL";

    private Tutorial tutorial;

    private RelativeLayout contentLayout;
    private TextView descriptionText;

    private @Nullable InteractionView interactionView;
    private View anchorView;
    private float interactionStartX = 0f, interactionStartY = 0f;


    //region Lifecycle

    public static boolean shouldShow(@NonNull Activity context, @NonNull Tutorial tutorial) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        return (!preferences.getBoolean(Constants.HANDHOLDING_SUPPRESSED, false) &&
                !preferences.getBoolean(tutorial.getShownKey(), false) &&
                context.getFragmentManager().findFragmentByTag(TAG) == null);
    }

    public static void markShown(@NonNull Context context, @NonNull Tutorial tutorial) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        preferences.edit()
                   .putBoolean(tutorial.getShownKey(), true)
                   .apply();
    }

    public static TutorialOverlayFragment newInstance(@NonNull Tutorial tutorial) {
        TutorialOverlayFragment dialogFragment = new TutorialOverlayFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_TUTORIAL, tutorial);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    public static void show(@NonNull FragmentManager fragmentManager, @NonNull Tutorial tutorial) {
        TutorialOverlayFragment dialogFragment = newInstance(tutorial);
        dialogFragment.show(fragmentManager, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.tutorial = (Tutorial) getArguments().getSerializable(ARG_TUTORIAL);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new EventDelegatingDialog(getActivity(), R.style.AppTheme_Dialog_FullScreen, this);
        if (tutorial.descriptionGravity == Gravity.BOTTOM) {
            dialog.getWindow().setWindowAnimations(R.style.WindowAnimations_SlideUpAndFade);
        } else {
            dialog.getWindow().setWindowAnimations(R.style.WindowAnimations_SlideDownAndFade);
        }

        this.contentLayout = new RelativeLayout(getActivity());
        dialog.setContentView(contentLayout);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        this.descriptionText = (TextView) inflater.inflate(R.layout.sub_fragment_tutorial_description, contentLayout, false);
        descriptionText.setText(tutorial.descriptionRes);
        descriptionText.setOnClickListener(ignored -> interactionCompleted());
        descriptionText.setOnLongClickListener(ignored -> {
            DismissTutorialsDialog tutorialsDialog = new DismissTutorialsDialog();
            tutorialsDialog.setTargetFragment(this, REQUEST_CODE_DISMISS_ALL);
            tutorialsDialog.show(getFragmentManager(), DismissTutorialsDialog.TAG);
            return true;
        });

        Drawable dismissIcon = descriptionText.getCompoundDrawablesRelative()[2].mutate();
        dismissIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        descriptionText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, dismissIcon, null);

        contentLayout.addView(descriptionText, tutorial.generateDescriptionLayoutParams());

        this.anchorView = getActivity().findViewById(tutorial.anchorId);
        if (anchorView != null) {
            if (anchorView.isLayoutRequested()) {
                bindAndSubscribe(Views.observeNextLayout(anchorView),
                                 ignored -> showInteractionFrom(),
                                 Functions.LOG_ERROR);
            } else {
                showInteractionFrom();
            }
        }

        return dialog;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_DISMISS_ALL && resultCode == Activity.RESULT_OK) {
            dismiss();
        }
    }

    //endregion


    //region Interactions

    private void showInteractionFrom() {
        this.interactionView = new InteractionView(getActivity());

        int interactionMidX = interactionView.getMinimumWidth() / 2;
        int interactionMidY = interactionView.getMinimumHeight() / 2;

        Rect anchorFrame = new Rect();
        Views.getFrameInWindow(anchorView, anchorFrame);

        LayoutParams layoutParams = new LayoutParams(interactionView.getMinimumWidth(), interactionView.getMinimumHeight());
        layoutParams.addRule(ALIGN_PARENT_TOP);
        layoutParams.addRule(ALIGN_PARENT_LEFT);
        layoutParams.leftMargin = anchorFrame.centerX() - interactionMidX;
        layoutParams.topMargin = anchorFrame.centerY() - interactionMidY;

        contentLayout.postDelayed(() -> {
            interactionView.setAlpha(0f);
            contentLayout.addView(interactionView, layoutParams);
            animate(interactionView)
                    .fadeIn()
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            interactionView.playTutorial(tutorial);
                        }
                    })
                    .start();
        }, 150);
    }

    private void interactionStarted() {
        if (interactionView != null) {
            interactionView.stopAnimation();

            animate(interactionView)
                    .setDuration(Animation.DURATION_VERY_FAST)
                    .fadeOut(View.GONE)
                    .start();

            animate(descriptionText)
                    .setDuration(Animation.DURATION_VERY_FAST)
                    .fadeOut(View.GONE)
                    .start();
        }
    }

    private void interactionCanceled() {
        Log.i(getClass().getSimpleName(), "interactionCanceled()");

        if (getTargetFragment() != null) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_CANCELED, null);
        }

        dismiss();
    }

    private void interactionCompleted() {
        Log.i(getClass().getSimpleName(), "interactionCompleted()");

        markShown(getActivity(), tutorial);

        if (getTargetFragment() != null) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_COMPLETED, null);
        }

        dismiss();
    }


    //region Event Forwarding

    @Override
    public boolean tryConsumeTouchEvent(@NonNull MotionEvent event) {
        if (getActivity() == null || Views.isMotionEventInside(descriptionText, event)) {
            return false;
        }

        getActivity().dispatchTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.interactionStartX = event.getRawX();
                this.interactionStartY = event.getRawY();

                interactionStarted();

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (tutorial.interaction == Interaction.TAP) {
                    if (Views.isMotionEventInside(anchorView, event)) {
                        interactionCompleted();
                    } else {
                        interactionCanceled();
                    }
                } else {
                    if (tutorial.interaction.isVertical) {
                        float deltaY = Math.abs(interactionStartY - event.getRawY());
                        if (deltaY >= getResources().getDimensionPixelSize(R.dimen.interaction_slide_positive)) {
                            interactionCompleted();
                        } else {
                            interactionCanceled();
                        }
                    } else {
                        float deltaX = Math.abs(interactionStartX - event.getRawX());
                        if (deltaX >= getResources().getDimensionPixelSize(R.dimen.interaction_slide_positive)) {
                            interactionCompleted();
                        } else {
                            interactionCanceled();
                        }
                    }
                }

                break;
            }

            default: {
                break;
            }
        }

        return true;
    }

    @Override
    public boolean tryConsumeTrackballEvent(@NonNull MotionEvent event) {
        return (getActivity() != null &&
                getActivity().dispatchTrackballEvent(event));
    }

    @Override
    public boolean tryConsumeKeyEvent(@NonNull KeyEvent event) {
        return (getActivity() != null &&
                event.getKeyCode() != KeyEvent.KEYCODE_BACK &&
                getActivity().dispatchKeyEvent(event));
    }

    @Override
    public boolean tryConsumeKeyShortcutKey(@NonNull KeyEvent event) {
        return (getActivity() != null &&
                getActivity().dispatchKeyShortcutEvent(event));
    }

    @Override
    public boolean tryConsumePopulateAccessibilityEvent(@NonNull AccessibilityEvent event) {
        return (getActivity() != null &&
                getActivity().dispatchPopulateAccessibilityEvent(event));
    }

    //endregion
}
