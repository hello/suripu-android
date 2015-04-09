package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.InsightCategory;
import is.hello.sense.api.model.InsightInfo;
import is.hello.sense.graph.presenters.InsightInfoPresenter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.ImageLoader;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;

public class InsightInfoDialogFragment extends InjectionDialogFragment {
    public static final String TAG = InsightInfoDialogFragment.class.getSimpleName();

    private static final String ARG_INSIGHT_CATEGORY = InsightInfoDialogFragment.class.getName() + ".ARG_INSIGHT_CATEGORY";
    private static final String ARG_INSIGHT_TITLE = InsightInfoDialogFragment.class.getName() + ".ARG_INSIGHT_TITLE";
    private static final String ARG_INSIGHT_MESSAGE = InsightInfoDialogFragment.class.getName() + ".ARG_INSIGHT_MESSAGE";

    @Inject InsightInfoPresenter presenter;
    @Inject Markdown markdown;

    private boolean hasCategory;

    private LinearLayout contentContainer;
    private ProgressBar loadingIndicator;
    private ImageView illustrationImage;
    private TextView titleText;
    private TextView messageText;

    public static InsightInfoDialogFragment newInstance(@NonNull Insight insight) {
        InsightInfoDialogFragment dialogFragment = new InsightInfoDialogFragment();

        Bundle arguments = new Bundle();
        if (insight.getCategory() != InsightCategory.GENERIC) {
            arguments.putString(ARG_INSIGHT_CATEGORY, insight.getCategory().toString());
        } else {
            arguments.putString(ARG_INSIGHT_TITLE, insight.getTitle());
            arguments.putString(ARG_INSIGHT_MESSAGE, insight.getMessage());
        }
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hasCategory = getArguments().containsKey(ARG_INSIGHT_CATEGORY);
        if (hasCategory) {
            InsightCategory category = InsightCategory.fromString(getArguments().getString(ARG_INSIGHT_CATEGORY));
            presenter.setInsightCategory(category);
            addPresenter(presenter);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_dialog_insight_info);

        this.contentContainer = (LinearLayout) dialog.findViewById(R.id.fragment_dialog_insight_info_content);
        this.loadingIndicator = (ProgressBar) contentContainer.findViewById(R.id.fragment_dialog_insight_info_loading);

        this.illustrationImage = (ImageView) contentContainer.findViewById(R.id.fragment_dialog_insight_info_illustration);
        this.titleText = (TextView) contentContainer.findViewById(R.id.fragment_dialog_insight_info_title);
        this.messageText = (TextView) contentContainer.findViewById(R.id.fragment_dialog_insight_info_message);

        Button doneButton = (Button) dialog.findViewById(R.id.fragment_dialog_insight_info_done);
        Views.setSafeOnClickListener(doneButton, this::done);

        if (hasCategory) {
            bindAndSubscribe(presenter.insightInfo, this::bindInsightInfo, this::insightInfoUnavailable);
        } else {
            String title = getArguments().getString(ARG_INSIGHT_TITLE);
            String message = getArguments().getString(ARG_INSIGHT_MESSAGE);

            titleText.setText(title);
            markdown.renderInto(messageText, message);

            showContent();
        }

        return dialog;
    }


    public void showContent() {
        loadingIndicator.setVisibility(View.GONE);
        illustrationImage.setVisibility(View.VISIBLE);
        titleText.setVisibility(View.VISIBLE);
        messageText.setVisibility(View.VISIBLE);
    }

    public void bindInsightInfo(@NonNull InsightInfo insightInfo) {
        titleText.setText(insightInfo.getTitle());
        markdown.renderInto(messageText, insightInfo.getText());

        illustrationImage.setImageDrawable(null);
        String imageUrl = insightInfo.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            bindAndSubscribe(ImageLoader.withUrl(imageUrl),
                             image -> {
                                 illustrationImage.setImageBitmap(image);
                                 contentContainer.setGravity(Gravity.TOP);
                                 showContent();
                             },
                             e -> {
                                 Logger.error(getClass().getSimpleName(), "Could not load image", e);
                                 showContent();
                             });
        } else {
            showContent();
        }
    }

    public void insightInfoUnavailable(Throwable e) {
        illustrationImage.setImageDrawable(null);
        titleText.setText(R.string.dialog_error_title);
        messageText.setText(e.getMessage());

        showContent();
    }


    public void done(@NonNull View sender) {
        dismiss();
    }
}