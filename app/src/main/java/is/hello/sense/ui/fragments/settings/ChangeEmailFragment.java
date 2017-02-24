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

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ChangeEmailFragment extends InjectionFragment implements Analytics.OnEventListener{
    @Inject
    AccountInteractor accountPresenter;

    private LabelEditText emailLET;
    private Button submitButton;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_change_email, container, false);

        this.emailLET = (LabelEditText) view.findViewById(R.id.fragment_change_email_let);
        emailLET.setOnEditorActionListener(new EditorActionHandler(this::save));

        this.submitButton = (Button) view.findViewById(R.id.fragment_change_email_submit);
        Views.setSafeOnClickListener(submitButton, ignored -> save());

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailLET.setEnabled(false);
        submitButton.setEnabled(false);

        bindAndSubscribe(accountPresenter.subscriptionSubject, this::bindAccount, this::presentError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.emailLET = null;
        this.submitButton = null;
    }

    public void save() {
        final String newEmail = AccountInteractor.normalizeInput(emailLET.getInputText());
        emailLET.setInputText(newEmail);
        if (!AccountInteractor.validateEmail(newEmail)) {
            emailLET.setError(R.string.invalid_email);
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
        emailLET.removeError();

        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.DEFAULTS);
        bindAndSubscribe(accountPresenter.updateEmail(newEmail),
                         ignored -> {
                             onSuccess();
                             // After hibernation, finish appears to be synchronous and
                             // as such the fragment manager is immediately nulled out.
                             final FragmentManager fragmentManager = getFragmentManager();
                             finishWithResult(Activity.RESULT_OK, null);
                             LoadingDialogFragment.close(fragmentManager);
                         },
                         this::presentError);
    }


    public void bindAccount(@NonNull final Account account) {
        emailLET.setInputText(account.getEmail());

        emailLET.setEnabled(true);
        submitButton.setEnabled(true);
        emailLET.requestFocus();

        LoadingDialogFragment.close(getFragmentManager());
    }

    public void presentError(@Nullable final Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity());

        if (ApiException.statusEquals(e, 409)) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_account_email_taken, emailLET.getInputText()));
        }

        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    //region Analytics.OnEvent Listener Methods
    @Override
    public void onSuccess() {
        Analytics.trackEvent(Analytics.Account.EVENT_CHANGE_EMAIL, null);
    }
    //endregion
}
