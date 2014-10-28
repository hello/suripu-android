package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.Insight;
import is.hello.sense.util.Markdown;

public class InsightDialogFragment extends DialogFragment {
    private static final String ARG_INSIGHT = InsightDialogFragment.class.getName() + ".ARG_INSIGHT";

    @Inject Markdown markdown;
    private Insight insight;

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
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Details);

        dialog.setContentView(R.layout.fragment_dialog_insight);
        dialog.setCanceledOnTouchOutside(true);

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
}
