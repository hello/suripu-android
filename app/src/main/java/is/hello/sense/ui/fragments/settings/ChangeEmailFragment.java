package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.app.FragmentManager;
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
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ChangeEmailFragment extends InjectionFragment {
    @Inject
    AccountPresenter accountPresenter;

    private LabelEditText emailLET;
    private Button submitButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);

        setRetainInstance(true);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_CHANGE_EMAIL, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_email, container, false);

        this.emailLET = (LabelEditText) view.findViewById(R.id.fragment_change_email_let);
        emailLET.setOnEditorActionListener(new EditorActionHandler(this::save));

        this.submitButton = (Button) view.findViewById(R.id.fragment_change_email_submit);
        Views.setSafeOnClickListener(submitButton, ignored -> save());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailLET.setEnabled(false);
        submitButton.setEnabled(false);

        bindAndSubscribe(accountPresenter.account, this::bindAccount, this::presentError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.emailLET = null;
        this.submitButton = null;
    }

    public void save() {
        String newEmail = AccountPresenter.normalizeInput(emailLET.getInputText());
        emailLET.setInputText(newEmail);
        if (!AccountPresenter.validateEmail(newEmail)) {
            emailLET.requestFocus();
            animatorFor(emailLET)
                    .scale(1.4f)
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            animatorFor(emailLET)
                                    .scale(1.0f)
                                    .start();
                        }
                    })
                    .start();

            return;
        }

        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.DEFAULTS);
        bindAndSubscribe(accountPresenter.updateEmail(newEmail),
                         ignored -> {
                             // After hibernation, finish appears to be synchronous and
                             // as such the fragment manager is immediately nulled out.
                             final FragmentManager fragmentManager = getFragmentManager();
                             finishWithResult(Activity.RESULT_OK, null);
                             LoadingDialogFragment.close(fragmentManager);
                         },
                         this::presentError);
    }


    public void bindAccount(@NonNull Account account) {
        emailLET.setInputText(account.getEmail());

        emailLET.setEnabled(true);
        submitButton.setEnabled(true);
        emailLET.requestFocus();

        LoadingDialogFragment.close(getFragmentManager());
    }

    public void presentError(@Nullable Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getResources());

        if (ApiException.statusEquals(e, 409)) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_account_email_taken, emailLET.getInputText()));
        }

        ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
