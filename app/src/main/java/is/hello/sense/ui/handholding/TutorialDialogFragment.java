package is.hello.sense.ui.handholding;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.util.Views;

import static android.widget.RelativeLayout.ALIGN_PARENT_LEFT;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.LayoutParams;

public class TutorialDialogFragment extends SenseDialogFragment implements EventDelegatingDialog.EventForwarder {
    public static final String TAG = TutorialDialogFragment.class.getSimpleName();

    public static final int RESULT_COMPLETED = 0x55;
    public static final int RESULT_CANCELED = 0x54;

    private static final String ARG_TUTORIAL = TutorialDialogFragment.class.getName() + ".ARG_TUTORIAL";

    private Tutorial tutorial;

    private RelativeLayout contentLayout;
    private TextView descriptionText;

    private InteractionView interactionView;
    private View anchorView;
    private float interactionStartX = 0f, interactionStartY = 0f;


    //region Lifecycle

    public static TutorialDialogFragment newInstance(@NonNull Tutorial tutorial) {
        TutorialDialogFragment dialogFragment = new TutorialDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_TUTORIAL, tutorial);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.tutorial = (Tutorial) getArguments().getSerializable(ARG_TUTORIAL);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new EventDelegatingDialog(getActivity(), R.style.AppTheme_Dialog_FullScreen, this);

        this.contentLayout = new RelativeLayout(getActivity());
        dialog.setContentView(contentLayout);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        this.descriptionText = (TextView) inflater.inflate(R.layout.sub_fragment_tutorial_description, contentLayout, false);
        descriptionText.setText(tutorial.descriptionRes);
        descriptionText.setOnClickListener(ignored -> interactionCompleted());
        contentLayout.addView(descriptionText, tutorial.generateDescriptionLayoutParams());

        View anchorView = getActivity().findViewById(tutorial.anchorId);
        if (anchorView != null) {
            if (anchorView.isLayoutRequested()) {
                Views.observeNextLayout(anchorView)
                     .subscribe(this::showInteractionFrom);
            } else {
                showInteractionFrom(anchorView);
            }
        }

        return dialog;
    }

    //endregion


    //region Interactions

    private void showInteractionFrom(@NonNull View anchorView) {
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

        contentLayout.addView(interactionView, layoutParams);

        interactionView.playTutorial(tutorial);

        this.anchorView = anchorView;
    }

    private void interactionStarted() {
        interactionView.stopAnimation();
        interactionView.setAlpha(0f);
    }

    private void interactionCanceled() {
        Log.i(getClass().getSimpleName(), "interactionCanceled()");

        if (getTargetFragment() != null) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_CANCELED, null);
        }

        dismissSafely();
    }

    private void interactionCompleted() {
        Log.i(getClass().getSimpleName(), "interactionCompleted()");

        if (getTargetFragment() != null) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_COMPLETED, null);
        }

        dismissSafely();
    }

    @Override
    public boolean tryConsumeTouchEvent(@NonNull MotionEvent event) {
        if (Views.isMotionEventInside(descriptionText, event)) {
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

    //endregion
}
