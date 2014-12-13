package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.util.Markdown;

public class InsightDetailsDialogFragment extends InjectionDialogFragment {
    public static final String TAG = InsightDetailsDialogFragment.class.getSimpleName();

    private static final String ARG_INSIGHT = InsightDetailsDialogFragment.class.getName() + ".ARG_INSIGHT";

    @Inject Markdown markdown;

    private Insight insight;

    private TextView title;
    private TextView message;

    public static InsightDetailsDialogFragment newInstance(@NonNull Insight insight) {
        InsightDetailsDialogFragment dialogFragment = new InsightDetailsDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_INSIGHT, insight);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.insight = (Insight) getArguments().getSerializable(ARG_INSIGHT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_dialog_insight_details);

        this.title = (TextView) dialog.findViewById(R.id.fragment_dialog_insight_details_title);
        this.message = (TextView) dialog.findViewById(R.id.fragment_dialog_insight_details_message);

        ImageButton shareButton = (ImageButton) dialog.findViewById(R.id.fragment_dialog_insight_details_share);
        shareButton.setOnClickListener(this::share);

        Button doneButton = (Button) dialog.findViewById(R.id.fragment_dialog_insight_details_done);
        doneButton.setOnClickListener(this::done);

        title.setText(insight.getTitle());
        message.setText(insight.getMessage());
        markdown.render(insight.getMessage())
                .subscribe(message::setText, Functions.LOG_ERROR);

        return dialog;
    }


    public void share(@NonNull View sender) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, title.getText() + "\n\n" + message.getText());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
    }

    public void done(@NonNull View sender) {
        dismiss();
    }
}