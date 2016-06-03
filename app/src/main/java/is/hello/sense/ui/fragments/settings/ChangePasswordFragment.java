package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.PasswordUpdate;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import rx.Observable;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ChangePasswordFragment extends InjectionFragment {
    private static final String ARG_EMAIL = ChangePasswordFragment.class.getName() + ".ARG_EMAIL";

    @Inject
    ApiEndpoint apiEndpoint;
    @Inject ApiService apiService;
    @Inject ApiSessionManager apiSessionManager;

    private String email;

    private LabelEditText currentPasswordLET;
    private LabelEditText newPasswordLET;
    private LabelEditText confirmNewPasswordLET;

    public static ChangePasswordFragment newInstance(@NonNull final String email) {
        final ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();

        final Bundle arguments = new Bundle();
        arguments.putString(ARG_EMAIL, email);
        changePasswordFragment.setArguments(arguments);

        return changePasswordFragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.email = getArguments().getString(ARG_EMAIL);

        setRetainInstance(true);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_CHANGE_PASSWORD, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        this.currentPasswordLET = (LabelEditText) view.findViewById(R.id.fragment_change_password_current_let);
        this.newPasswordLET = (LabelEditText) view.findViewById(R.id.fragment_change_password_new_let);
        this.confirmNewPasswordLET = (LabelEditText) view.findViewById(R.id.fragment_change_password_new_confirm_let);
        confirmNewPasswordLET.setOnEditorActionListener(new EditorActionHandler(this::changePassword));

        final Button submit = (Button) view.findViewById(R.id.fragment_change_password_submit);
        Views.setSafeOnClickListener(submit, ignored -> changePassword());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.currentPasswordLET = null;
        this.newPasswordLET = null;
        this.confirmNewPasswordLET = null;
    }

    private boolean popIfEmpty(final LabelEditText... labelEditTexts) {
        currentPasswordLET.removeError();
        newPasswordLET.removeError();
        confirmNewPasswordLET.removeError();
        for (final LabelEditText editText : labelEditTexts) {
            if (TextUtils.isEmpty(editText.getInputText())) {
                editText.setError(R.string.invalid_password);
                editText.requestFocus();
                animatorFor(editText)
                        .translationY(20)
                        .addOnAnimationCompleted(finished -> {
                            if (finished) {
                                animatorFor(editText)
                                        .translationY(0)
                                        .withInterpolator(new OvershootInterpolator())
                                        .start();
                            }
                        })
                        .start();
                return true;
            }
        }

        return false;
    }

    public void changePassword() {
        if (popIfEmpty(currentPasswordLET, newPasswordLET, confirmNewPasswordLET)) {
            return;
        }

        if (!AccountPresenter.validatePassword(newPasswordLET.getInputText())) {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(R.string.error_account_password_too_short))
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            newPasswordLET.requestFocus();
            return;
        }

        if (!TextUtils.equals(newPasswordLET.getInputText(), confirmNewPasswordLET.getInputText())) {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(R.string.error_mismatching_new_passwords))
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            return;
        }

        LoadingDialogFragment.show(getFragmentManager(),
                null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        final PasswordUpdate passwordUpdate = new PasswordUpdate(currentPasswordLET.getInputText(), newPasswordLET.getInputText());
        bindAndSubscribe(apiService.changePassword(passwordUpdate),
                         ignored -> recreateSession(),
                         this::presentError);
    }

    public void recreateSession() {
        final String password = newPasswordLET.getInputText();
        final Observable<OAuthSession> authorize = apiService.authorize(new OAuthCredentials(apiEndpoint, email, password));
        bindAndSubscribe(authorize,
                         session -> {
                             apiSessionManager.setSession(session);
                             getFragmentManager().popBackStack();
                             LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);
                         },
                         this::presentError);
    }

    public void presentError(@Nullable final Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getResources());
        if (ApiException.statusEquals(e, 409)) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_message_current_pw_wrong));
        }

        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
