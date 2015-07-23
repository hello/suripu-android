package is.hello.sense.ui.fragments.support;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import com.zendesk.sdk.attachment.UriToFileUtil;
import com.zendesk.sdk.feedback.ZendeskFeedbackConfiguration;
import com.zendesk.sdk.feedback.ui.AttachmentContainerHost;
import com.zendesk.sdk.model.CreateRequest;
import com.zendesk.sdk.model.network.UploadResponse;
import com.zendesk.service.ErrorResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.ZendeskPresenter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

public class ContactUsFragment extends InjectionFragment implements TextWatcher, ImageUploadHelper.ImageUploadProgressListener {
    private static final String ARG_SUPPORT_TOPIC = ContactUsFragment.class.getName() + ".ARG_SUPPORT_TOPIC";

    private static final int REQUEST_CODE_TAKE_IMAGE = 0x01;
    private static final int REQUEST_CODE_PICK_IMAGE = 0x02;

    @Inject ZendeskPresenter zendeskPresenter;

    private SupportTopic supportTopic;

    private EditText text;
    private AttachmentContainerHost attachmentHost;

    private MenuItem addAttachmentItem;
    private MenuItem sendItem;

    private ImageUploadHelper imageUploadHelper;
    private String pendingCapturePath;


    //region Lifecycle

    public static ContactUsFragment newInstance(@NonNull SupportTopic topic) {
        ContactUsFragment fragment = new ContactUsFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SUPPORT_TOPIC, topic);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.pendingCapturePath = savedInstanceState.getString("pendingCapturePath");
        }
        setHasOptionsMenu(true);

        this.supportTopic = (SupportTopic) getArguments().getSerializable(ARG_SUPPORT_TOPIC);
        this.imageUploadHelper = new ImageUploadHelper(this);

        addPresenter(zendeskPresenter);

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

        outState.putString("pendingCapturePath", pendingCapturePath);
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
        SenseBottomSheet chooseSource = new SenseBottomSheet(getActivity());
        chooseSource.setTitle(R.string.action_add_attachment);

        chooseSource.addOption(new SenseBottomSheet.Option(0)
                .setTitle(R.string.action_take_photo));
        chooseSource.addOption(new SenseBottomSheet.Option(1)
                .setTitle(R.string.action_pick_image));

        chooseSource.setOnOptionSelectedListener(option -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(zendeskPresenter.initializeIfNeeded(),
                    config -> {
                        LoadingDialogFragment.close(getFragmentManager());
                        switch (option.getOptionId()) {
                            case 0:
                                captureImage();
                                break;
                            case 1:
                                pickImage();
                                break;
                            default:
                                new IllegalArgumentException();
                        }
                    }, e -> {
                        LoadingDialogFragment.close(getFragmentManager());
                        ErrorDialogFragment.presentError(getFragmentManager(), e);
                    });

            return true;
        });
        chooseSource.show();

    }

    private void captureImage() {
        String filename = "JPEG_" + DateFormatter.nowDateTime().toString();
        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        try {
            File imageLocation = File.createTempFile(filename, ".jpg", pictures);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageLocation));
            startActivityForResult(intent, REQUEST_CODE_TAKE_IMAGE);

            this.pendingCapturePath = imageLocation.getAbsolutePath();
        } catch (IOException | ActivityNotFoundException e) {
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e)
                    .withMessage(StringRef.from(R.string.error_image_capture_failed))
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        }
    }

    private void pickImage() {
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("image/*");
        getContentIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        getContentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        Intent pickerIntent = Intent.createChooser(getContentIntent, getString(R.string.action_pick_image));
        startActivityForResult(pickerIntent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent response) {
        super.onActivityResult(requestCode, resultCode, response);

        if (requestCode == REQUEST_CODE_TAKE_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri imageLocation = response.getData();
                String path = imageLocation != null
                        ? UriToFileUtil.getPath(getActivity(), imageLocation)
                        : pendingCapturePath;
                File imageFile = new File(path);
                AttachmentHelper.processAndUploadSelectedFiles(Lists.newArrayList(imageFile),
                        imageUploadHelper, getActivity(), attachmentHost);

                getActivity().invalidateOptionsMenu();
            }

            this.pendingCapturePath = null;
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            List<File> files = new ArrayList<>();
            if (response.getClipData() != null) {
                ClipData clipData = response.getClipData();
                for (int i = 0, count = clipData.getItemCount(); i < count; i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    Uri uri = item.getUri();
                    if (uri != null) {
                        files.add(UriToFileUtil.getFile(getActivity(), uri));
                    }
                }
            } else {
                Uri uri = response.getData();
                files.add(UriToFileUtil.getFile(getActivity(), uri));
            }

            AttachmentHelper.processAndUploadSelectedFiles(files,
                    imageUploadHelper, getActivity(), attachmentHost);

            getActivity().invalidateOptionsMenu();
        }
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
        Observable<ZendeskFeedbackConfiguration> prepare = zendeskPresenter.prepareForFeedback(supportTopic);
        Observable<CreateRequest> send = prepare.flatMap(config -> zendeskPresenter.sendFeedback(config,
                text.getText().toString(), imageUploadHelper.getUploadTokens()));
        bindAndSubscribe(send,
                ignored -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    ((FragmentNavigation) getActivity()).popFragment(this, false);
                }, e -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    ErrorDialogFragment.presentError(getFragmentManager(), e);
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