package is.hello.sense.ui.common;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.Account;

public class AccountEditingFragment extends Fragment {
    protected static final String ARG_WANTS_SKIP_BUTTON = AccountEditingFragment.class.getSimpleName() + ".ARG_WANTS_SKIP_BUTTON";

    public void setWantsSkipButton(boolean wantsSkipButton) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            arguments = new Bundle();
            setArguments(arguments);
        }

        arguments.putBoolean(ARG_WANTS_SKIP_BUTTON, wantsSkipButton);
    }

    public boolean getWantsSkipButton() {
        return (getArguments() == null || getArguments().getBoolean(ARG_WANTS_SKIP_BUTTON, true));
    }

    protected Container getContainer() {
        if (getTargetFragment() != null) {
            return (Container) getTargetFragment();
        } else {
            return (Container) getActivity();
        }
    }

    public static interface Container {
        @NonNull Account getAccount();
        void onAccountUpdated(@NonNull AccountEditingFragment updatedBy);
    }
}
