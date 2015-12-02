package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.markup.text.MarkupString;

public class InsightInfoDialogFragment extends InjectionDialogFragment {
    public static final String TAG = InsightInfoDialogFragment.class.getSimpleName();

    private static final String ARG_TITLE = InsightInfoDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_MESSAGE = InsightInfoDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_IMAGE_URL = InsightInfoDialogFragment.class.getName() + ".ARG_IMAGE_URL";
    private static final String ARG_INFO = InsightInfoDialogFragment.class.getName() + ".ARG_INFO";

    @Inject Picasso picasso;

    private String title;
    private MarkupString message;
    private String imageUrl;
    private MarkupString info;

    public static InsightInfoDialogFragment newInstance(@NonNull String title,
                                                        @NonNull MarkupString message,
                                                        @Nullable String imageUrl,
                                                        @Nullable MarkupString info) {
        final InsightInfoDialogFragment fragment = new InsightInfoDialogFragment();

        final Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putParcelable(ARG_MESSAGE, message);
        arguments.putString(ARG_IMAGE_URL, imageUrl);
        arguments.putParcelable(ARG_INFO, info);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        this.title = arguments.getString(ARG_TITLE);
        this.message = arguments.getParcelable(ARG_MESSAGE);
        this.imageUrl = arguments.getString(ARG_IMAGE_URL);
        this.info = arguments.getParcelable(ARG_INFO);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_dialog_insight_info);

        final ImageView illustrationImage =
                (ImageView) dialog.findViewById(R.id.fragment_dialog_insight_info_illustration);
        if (imageUrl != null) {

        } else {
            illustrationImage.setVisibility(View.GONE);
        }

        final TextView titleText =
                (TextView) dialog.findViewById(R.id.fragment_dialog_insight_info_title);
        titleText.setText(title);

        final TextView messageText =
                (TextView) dialog.findViewById(R.id.fragment_dialog_insight_info_message);
        if (TextUtils.isEmpty(info)) {
            messageText.setText(message);
        } else {
            messageText.setText(info);
        }

        final Button doneButton =
                (Button) dialog.findViewById(R.id.fragment_dialog_insight_info_done);
        Views.setSafeOnClickListener(doneButton, this::done);

        return dialog;
    }

    public void done(@NonNull View sender) {
        dismissAllowingStateLoss();
    }
}