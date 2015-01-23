package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.InsightCategory;
import is.hello.sense.api.model.InsightInfo;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.InsightInfoPresenter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.ImageLoader;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.android.schedulers.AndroidSchedulers;

public class InsightInfoDialogFragment extends InjectionDialogFragment {
    public static final String TAG = InsightInfoDialogFragment.class.getSimpleName();

    private static final String ARG_INSIGHT_CATEGORY = InsightInfoDialogFragment.class.getName() + ".ARG_INSIGHT_CATEGORY";

    @Inject InsightInfoPresenter presenter;
    @Inject Markdown markdown;

    private LinearLayout contentContainer;
    private ProgressBar loadingIndicator;
    private ImageView illustration;
    private TextView title;
    private TextView message;

    public static InsightInfoDialogFragment newInstance(@NonNull InsightCategory category) {
        InsightInfoDialogFragment dialogFragment = new InsightInfoDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_INSIGHT_CATEGORY, category.toString());
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InsightCategory category = InsightCategory.fromString(getArguments().getString(ARG_INSIGHT_CATEGORY));
        presenter.setInsightCategory(category);
        addPresenter(presenter);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_dialog_insight_info);

        this.contentContainer = (LinearLayout) dialog.findViewById(R.id.fragment_dialog_insight_info_content);
        this.loadingIndicator = (ProgressBar) contentContainer.findViewById(R.id.fragment_dialog_insight_info_loading);

        this.illustration = (ImageView) contentContainer.findViewById(R.id.fragment_dialog_insight_info_illustration);
        this.title = (TextView) contentContainer.findViewById(R.id.fragment_dialog_insight_info_title);
        this.message = (TextView) contentContainer.findViewById(R.id.fragment_dialog_insight_info_message);

        ImageButton shareButton = (ImageButton) dialog.findViewById(R.id.fragment_dialog_insight_info_share);
        Views.setSafeOnClickListener(shareButton, this::share);

        Button doneButton = (Button) dialog.findViewById(R.id.fragment_dialog_insight_info_done);
        Views.setSafeOnClickListener(doneButton, this::done);

        bindAndSubscribe(presenter.insightInfo, this::bindInsightInfo, this::insightInfoUnavailable);

        return dialog;
    }


    public void showContent() {
        loadingIndicator.setVisibility(View.GONE);
        illustration.setVisibility(View.VISIBLE);
        title.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
    }

    public void bindInsightInfo(@NonNull InsightInfo insightInfo) {
        title.setText(insightInfo.getTitle());
        message.setText(insightInfo.getText());
        markdown.render(insightInfo.getText())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(message::setText, Functions.LOG_ERROR);

        illustration.setImageDrawable(null);
        String imageUrl = insightInfo.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            bindAndSubscribe(ImageLoader.withUrl(imageUrl),
                             image -> {
                                 illustration.setImageBitmap(image);
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
        illustration.setImageDrawable(null);
        title.setText(R.string.dialog_error_title);
        message.setText(e.getMessage());

        showContent();
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