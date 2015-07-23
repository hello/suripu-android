package is.hello.sense.ui.fragments.support;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.zendesk.sdk.feedback.ZendeskFeedbackConfiguration;
import com.zendesk.sdk.model.CreateRequest;

import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.graph.presenters.ZendeskPresenter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.util.Views;
import rx.Observable;

public class ContactUsFragment extends InjectionFragment implements TextWatcher {
    private static final String ARG_SUPPORT_TOPIC = ContactUsFragment.class.getName() + ".ARG_SUPPORT_TOPIC";

    private static final int REQUEST_CODE_TAKE_IMAGE = 0x01;
    private static final int REQUEST_CODE_PICK_IMAGE = 0x02;

    @Inject ZendeskPresenter zendeskPresenter;

    private SupportTopic supportTopic;

    private Button attachFile;
    private EditText text;

    private MenuItem sendItem;


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

        setHasOptionsMenu(true);

        this.supportTopic = (SupportTopic) getArguments().getSerializable(ARG_SUPPORT_TOPIC);

        addPresenter(zendeskPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_us, container, false);

        this.text = (EditText) view.findViewById(R.id.fragment_contact_us_text);
        text.addTextChangedListener(this);

        this.attachFile = (Button) view.findViewById(R.id.fragment_contact_us_attach);
        Views.setSafeOnClickListener(attachFile, this::attach);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        text.removeTextChangedListener(this);

        this.text = null;
        this.attachFile = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contact_us, menu);

        this.sendItem = menu.findItem(R.id.action_send);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send: {
                send();
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        sendItem.setEnabled(!TextUtils.isEmpty(text.getText()));
    }

    //endregion


    public void attach(@NonNull View sender) {
        SenseBottomSheet chooseSource = new SenseBottomSheet(getActivity());

        chooseSource.addOption(new SenseBottomSheet.Option(0)
                .setTitle("Take Photo"));
        chooseSource.addOption(new SenseBottomSheet.Option(1)
                .setTitle("Choose Image"));

        chooseSource.setOnOptionSelectedListener(option -> {
            if (option.getOptionId() == 0) {
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(captureIntent, REQUEST_CODE_TAKE_IMAGE);
            } else if (option.getOptionId() == 1) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");
                startActivityForResult(pickIntent, REQUEST_CODE_PICK_IMAGE);
            }

            return true;
        });
        chooseSource.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(getClass().getSimpleName(), "resultCode: " + resultCode + "; data: " + data);
    }

    private void send() {
        LoadingDialogFragment.show(getFragmentManager());
        Observable<ZendeskFeedbackConfiguration> prepare = zendeskPresenter.prepareForFeedback(supportTopic);
        Observable<CreateRequest> submit = prepare.flatMap(config -> zendeskPresenter.submitFeedback(config,
                text.getText().toString(), Collections.<String>emptyList()));
        bindAndSubscribe(submit,
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
}
