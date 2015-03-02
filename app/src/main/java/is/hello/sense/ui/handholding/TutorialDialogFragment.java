package is.hello.sense.ui.handholding;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.util.Views;

import static android.widget.RelativeLayout.ALIGN_PARENT_LEFT;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.LayoutParams;

public class TutorialDialogFragment extends SenseDialogFragment {
    public static final String TAG = TutorialDialogFragment.class.getSimpleName();

    private static final String ARG_TUTORIAL = TutorialDialogFragment.class.getName() + ".ARG_TUTORIAL";

    private Tutorial tutorial;
    private RelativeLayout contentLayout;

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
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);

        this.contentLayout = new RelativeLayout(getActivity());
        dialog.setContentView(contentLayout);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        TextView descriptionText = (TextView) inflater.inflate(R.layout.sub_fragment_tutorial_description, contentLayout, false);
        descriptionText.setText(tutorial.description);
        descriptionText.setOnClickListener(ignored -> dismissSafely());
        contentLayout.addView(descriptionText, tutorial.generateDescriptionLayoutParams());

        Interaction interaction = tutorial.interaction;
        View anchorView = getActivity().findViewById(interaction.anchorViewRes);
        if (anchorView != null) {
            if (anchorView.isLayoutRequested()) {
                Views.observeNextLayout(anchorView)
                     .subscribe(this::addInteractionIndicator);
            } else {
                addInteractionIndicator(anchorView);
            }
        }

        return dialog;
    }


    private void addInteractionIndicator(@NonNull View anchorView) {
        InteractionView interactionView = new InteractionView(getActivity());

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

        interactionView.playInteraction(tutorial.interaction);
    }
}
