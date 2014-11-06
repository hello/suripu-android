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
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Markdown;

public class InsightDialogFragment extends DialogFragment {
    public static final String TAG = InsightDialogFragment.class.getSimpleName();

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

        setCancelable(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        dialog.setTitle(insight.getTitle());
        markdown.render(insight.getMessage())
                .subscribe(dialog::setMessage, ignored -> dialog.setMessage(R.string.missing_data_placeholder));
        dialog.setPositiveButton(android.R.string.ok, null);

        return dialog;
    }
}
