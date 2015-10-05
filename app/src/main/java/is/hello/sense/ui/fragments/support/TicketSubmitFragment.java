package is.hello.sense.ui.fragments.support;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.zendesk.sdk.attachment.AttachmentHelper;
import com.zendesk.sdk.attachment.ImageUploadHelper;
import com.zendesk.sdk.feedback.ui.AttachmentContainerHost;
import com.zendesk.sdk.model.CreateRequest;
import com.zendesk.sdk.model.network.UploadResponse;
import com.zendesk.service.ErrorResponse;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.zendesk.AttachmentPicker;
import is.hello.sense.zendesk.TicketsPresenter;
import rx.Observable;

public class TicketSubmitFragment extends InjectionFragment implements TextWatcher, ImageUploadHelper.ImageUploadProgressListener {
    private static final String ARG_SUPPORT_TOPIC = TicketSubmitFragment.class.getName() + ".ARG_SUPPORT_TOPIC";

    @Inject TicketsPresenter ticketsPresenter;

    private AttachmentPicker attachmentPicker;
    private SupportTopic supportTopic;
    private ImageUploadHelper imageUploadHelper;

    private EditText text;
    private AttachmentContainerHost attachmentHost;

    private MenuItem addAttachmentItem;
    private MenuItem sendItem;


    //region Lifecycle

    public static TicketSubmitFragment newInstance(@NonNull SupportTopic topic) {
        TicketSubmitFragment fragment = new TicketSubmitFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SUPPORT_TOPIC, topic);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        this.attachmentPicker = new AttachmentPicker(this, savedInstanceState);
        this.supportTopic = (SupportTopic) getArguments().getSerializable(ARG_SUPPORT_TOPIC);
        this.imageUploadHelper = new ImageUploadHelper(this);

        addPresenter(ticketsPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_us, container, false);

        this.text = (EditText) view.findViewById(R.id.fragment_contact_us_text);
        text.addTextChangedListener(this);

        LinearLayout textContainer = (LinearLayout) view.findViewById(R.id.fragment_contact_us_container);
        textContainer.setOnClickListener(ignored -> {
            text.requestFocus();

            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(text, 0);
        });

        this.attachmentHost = (AttachmentContainerHost) view.findViewById(R.id.fragment_contact_us_attachment_host);
        attachmentHost.setState(imageUploadHelper);
        attachmentHost.setAttachmentsDeletable(true);
        // A retrolambda bug prevents a method reference from working here.
        //noinspection Convert2MethodRef
        attachmentHost.setAttachmentContainerListener(f -> imageUploadHelper.removeImage(f));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(text.getWindowToken(), 0);

        text.removeTextChangedListener(this);

        this.text = null;
        this.attachmentHost = null;
        this.addAttachmentItem = null;
        this.sendItem = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        attachmentPicker.saveInstanceState(outState);
    }

    //endregion


    //region Menu

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contact_us, menu);

        this.addAttachmentItem = menu.findItem(R.id.action_add_attachment);
        this.sendItem = menu.findItem(R.id.action_send);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send: {
                send();
                return true;
            }

            case R.id.action_add_attachment: {
                showAttachOptions();
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean hasText = !TextUtils.isEmpty(text.getText());
        boolean uploadsInactive = imageUploadHelper.isImageUploadCompleted();
        addAttachmentItem.setEnabled(uploadsInactive);
        addAttachmentItem.getIcon().setAlpha(uploadsInactive ? 0xFF : 0x77);

        sendItem.setEnabled(hasText && uploadsInactive);
        sendItem.getIcon().setAlpha(hasText && uploadsInactive ? 0xFF : 0x77);
    }

    //endregion


    //region Attachments

    private void showAttachOptions() {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(ticketsPresenter.initializeIfNeeded(),
                config -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    attachmentPicker.showOptions();
                }, e -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    ErrorDialogFragment.presentError(getActivity(), e);
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent response) {
        super.onActivityResult(requestCode, resultCode, response);

        List<File> files = attachmentPicker.getFilesFromResult(requestCode, resultCode, response);
        AttachmentHelper.processAndUploadSelectedFiles(files,
                imageUploadHelper, getActivity(), attachmentHost);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void allImagesUploaded(Map<File, UploadResponse> map) {
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void imageUploaded(UploadResponse uploadResponse, File file) {
        attachmentHost.setAttachmentUploaded(file);
    }

    @Override
    public void imageUploadError(ErrorResponse errorResponse, File file) {
        Analytics.trackError(errorResponse.getReason(), ErrorResponse.class.getCanonicalName(),
                errorResponse.getResponseBody(), "Zendesk Attachment Upload");

        AttachmentHelper.showAttachmentTryAgainDialog(getActivity(), file,
                errorResponse, imageUploadHelper, attachmentHost);
    }

    //endregion


    //region Submissions

    private void send() {
        LoadingDialogFragment.show(getFragmentManager());

        Observable<CreateRequest> openTicket = ticketsPresenter.createTicket(supportTopic,
                text.getText().toString(), imageUploadHelper.getUploadTokens());
        bindAndSubscribe(openTicket,
                ignored -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    getFragmentNavigation().popFragment(this, false);
                }, e -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    ErrorDialogFragment.presentError(getActivity(), e);
                });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        getActivity().invalidateOptionsMenu();
    }

    //endregion
}