package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.Insight;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.util.Markdown;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class InsightDialogFragment extends DialogFragment {
    public static final String TAG = InsightDialogFragment.class.getSimpleName();

    private static final String ARG_INSIGHT = InsightDialogFragment.class.getName() + ".ARG_INSIGHT";

    @Inject Markdown markdown;
    private Insight insight;
    private View contentView;

    public static InsightDialogFragment newInstance(@NonNull Insight insight) {
        InsightDialogFragment fragment = new InsightDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_INSIGHT, insight);
        fragment.setArguments(arguments);

        return fragment;
    }


    public InsightDialogFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.insight = (Insight) getArguments().getSerializable(ARG_INSIGHT);

        setCancelable(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Details);

        dialog.setContentView(R.layout.fragment_dialog_insight);
        dialog.setCanceledOnTouchOutside(true);

        this.contentView = dialog.findViewById(R.id.fragment_dialog_insight_container);

        View overlay = dialog.findViewById(R.id.fragment_dialog_insight_overlay);
        overlay.setOnClickListener(ignored -> dismiss());

        TextView title = (TextView) dialog.findViewById(R.id.fragment_dialog_insight_title);
        title.setText(insight.getTitle());

        ImageView icon = (ImageView) dialog.findViewById(R.id.fragment_dialog_insight_icon);
        icon.setContentDescription(insight.getTitle());

        TextView message = (TextView) dialog.findViewById(R.id.fragment_dialog_insight_message);
        markdown.render(insight.getMessage())
                .subscribe(message::setText, ignored -> message.setText(R.string.missing_data_placeholder));

        Button okButton = (Button) dialog.findViewById(R.id.fragment_dialog_insight_ok);
        okButton.setOnClickListener(ignored -> dismiss());

        return dialog;
    }


    @Override
    public void onStart() {
        super.onStart();

        contentView.setScaleX(0.5f);
        contentView.setScaleY(0.5f);
        animate(contentView)
                .setDuration(Animation.DURATION_DEFAULT / 2)
                .setInterpolator(new AccelerateInterpolator())
                .scaleX(1.05f)
                .scaleY(1.05f)
                .andThen()
                .setApplyChangesToView(true)
                .setDuration(Animation.DURATION_DEFAULT / 2)
                .setInterpolator(new DecelerateInterpolator())
                .scaleX(1f)
                .scaleY(1f)
                .start();
    }
}
