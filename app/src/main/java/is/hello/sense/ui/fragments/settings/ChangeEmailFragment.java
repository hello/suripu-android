package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ChangeEmailFragment extends InjectionFragment {
    @Inject AccountPresenter accountPresenter;

    private EditText email;
    private Button submitButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);

        setRetainInstance(true);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_CHANGE_EMAIL, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_email, container, false);

        this.email = (EditText) view.findViewById(R.id.fragment_change_email_value);
        email.setOnEditorActionListener(new EditorActionHandler(this::save));

        this.submitButton = (Button) view.findViewById(R.id.fragment_change_email_submit);
        Views.setSafeOnClickListener(submitButton, ignored -> save());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        email.setEnabled(false);
        submitButton.setEnabled(false);

        bindAndSubscribe(accountPresenter.account, this::bindAccount, this::presentError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.email = null;
        this.submitButton = null;
    }

    public void save() {
        String newEmail = AccountPresenter.normalizeInput(email.getText());
        email.setText(newEmail);
        if (!AccountPresenter.validateEmail(newEmail)) {
            email.requestFocus();
            animatorFor(email).simplePop(1.4f).start();

            return;
        }

        LoadingDialogFragment.show(getFragmentManager(),
                null, LoadingDialogFragment.DEFAULTS);
        bindAndSubscribe(accountPresenter.updateEmail(newEmail),
                         ignored -> {
                             finishWithResult(Activity.RESULT_OK, null);
                             LoadingDialogFragment.close(getFragmentManager());
                         },
                         this::presentError);
    }


    public void bindAccount(@NonNull Account account) {
        email.setText(account.getEmail());

        email.setEnabled(true);
        submitButton.setEnabled(true);
        email.requestFocus();

        LoadingDialogFragment.close(getFragmentManager());
    }

    public void presentError(@Nullable Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getResources());

        if (ApiException.statusEquals(e, 409)) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_account_email_taken, email.getText().toString()));
        }

        ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
