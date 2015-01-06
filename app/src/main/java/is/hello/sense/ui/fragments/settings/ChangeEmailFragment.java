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
import is.hello.sense.api.model.Account;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class ChangeEmailFragment extends InjectionFragment {
    @Inject AccountPresenter accountPresenter;

    private EditText email;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_email, container, false);

        this.email = (EditText) view.findViewById(R.id.fragment_change_email_value);

        Button submit = (Button) view.findViewById(R.id.fragment_change_email_submit);
        Views.setSafeOnClickListener(submit, this::save);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LoadingDialogFragment.show(getFragmentManager(), null, false);
        bindAndSubscribe(accountPresenter.account, this::bindAccount, this::presentError);
    }

    public void save(@NonNull View sender) {
        String newEmail = email.getText().toString();
        if (TextUtils.isEmpty(newEmail)) {
            email.requestFocus();
            animate(email).simplePop(1.4f).start();

            return;
        }

        LoadingDialogFragment.show(getFragmentManager(), null, false);
        bindAndSubscribe(accountPresenter.updateEmail(newEmail),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             getFragmentManager().popBackStack();
                         },
                         this::presentError);
    }


    public void bindAccount(@NonNull Account account) {
        email.setText(account.getEmail());

        LoadingDialogFragment.close(getFragmentManager());
    }

    public void presentError(@Nullable Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
