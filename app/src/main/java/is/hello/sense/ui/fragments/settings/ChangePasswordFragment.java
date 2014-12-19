package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.PasswordUpdate;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.EditorActionHandler;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class ChangePasswordFragment extends InjectionFragment {
    private static final String ARG_EMAIL = ChangePasswordFragment.class.getName() + ".ARG_EMAIL";

    @Inject BuildValues buildValues;
    @Inject ApiService apiService;
    @Inject ApiSessionManager apiSessionManager;

    private String email;

    private EditText currentPassword;
    private EditText newPassword;
    private EditText confirmNewPassword;

    public static ChangePasswordFragment newInstance(@NonNull String email) {
        ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_EMAIL, email);
        changePasswordFragment.setArguments(arguments);

        return changePasswordFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.email = getArguments().getString(ARG_EMAIL);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        this.currentPassword = (EditText) view.findViewById(R.id.fragment_change_password_current);
        this.newPassword = (EditText) view.findViewById(R.id.fragment_change_password_new);
        this.confirmNewPassword = (EditText) view.findViewById(R.id.fragment_change_password_new_confirm);
        confirmNewPassword.setOnEditorActionListener(new EditorActionHandler(this::changePassword));

        Button submit = (Button) view.findViewById(R.id.fragment_change_password_submit);
        Views.setSafeOnClickListener(submit, ignored -> changePassword());

        return view;
    }


    private boolean popIfEmpty(EditText... editTexts) {
        for (EditText editText : editTexts) {
            if (TextUtils.isEmpty(editText.getText())) {
                editText.requestFocus();
                animate(editText).simplePop(1.4f).start();
                return true;
            }
        }

        return false;
    }

    public void changePassword() {
        if (popIfEmpty(currentPassword, newPassword, confirmNewPassword)) {
            return;
        }

        if (newPassword.getText().length() <= 3) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_invalid_register_password));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            newPassword.requestFocus();
            return;
        }

        if (!TextUtils.equals(newPassword.getText(), confirmNewPassword.getText())) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_mismatching_new_passwords));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            return;
        }

        LoadingDialogFragment.show(getFragmentManager(), null, true);
        PasswordUpdate passwordUpdate = new PasswordUpdate(currentPassword.getText().toString(), newPassword.getText().toString());
        bindAndSubscribe(apiService.changePassword(passwordUpdate),
                         ignored -> recreateSession(),
                         this::presentError);
    }

    public void recreateSession() {
        ApiEnvironment apiEnvironment = ApiEnvironment.fromString(buildValues.defaultApiEnvironment);
        String password = newPassword.getText().toString();
        Observable<OAuthSession> authorize = apiService.authorize(new OAuthCredentials(apiEnvironment, email, password));
        bindAndSubscribe(authorize,
                         session -> {
                             apiSessionManager.setSession(session);
                             LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {});
                             getFragmentManager().popBackStack();
                         },
                         this::presentError);
    }

    public void presentError(@Nullable Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
